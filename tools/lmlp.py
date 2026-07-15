#!/usr/bin/env python3
"""Central build, deployment, verification and release tooling for LMLP."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import platform
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError as exc:  # pragma: no cover - Python < 3.11
    raise SystemExit("LMLP 管理工具需要 Python 3.11 或更高版本。") from exc


ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "versions.toml"


class LmlpError(RuntimeError):
    pass


def run(command: list[str], cwd: Path | None = None, *, capture: bool = False,
        check: bool = True, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    if not capture:
        print("+", " ".join(command))
    return subprocess.run(
        command,
        cwd=cwd,
        env=env,
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
    )


def git(args: list[str], cwd: Path = ROOT, *, capture: bool = True,
        check: bool = True) -> subprocess.CompletedProcess[str]:
    return run(["git", *args], cwd, capture=capture, check=check)


def load_config() -> dict:
    if not CONFIG_PATH.is_file():
        raise LmlpError(f"找不到版本清单：{CONFIG_PATH}")
    with CONFIG_PATH.open("rb") as handle:
        return tomllib.load(handle)


def normalize_version(value: str) -> str:
    value = value.strip()
    return value[2:] if value.startswith("mc") else value


def version_entry(config: dict, value: str) -> tuple[str, dict]:
    key = normalize_version(value)
    entry = config["versions"].get(key)
    if entry is None:
        raise LmlpError(f"未知 Minecraft 版本：{value}")
    if not entry.get("supported", False):
        reason = entry.get("reason", "该版本不再受支持")
        raise LmlpError(f"Minecraft {key} 不参与当前构建：{reason}")
    return key, entry


def selected_versions(config: dict, values: list[str] | None = None) -> list[tuple[str, dict]]:
    if values:
        return [version_entry(config, value) for value in values]
    return [(key, value) for key, value in config["versions"].items()
            if value.get("supported", False)]


def worktree_map() -> dict[str, Path]:
    output = git(["worktree", "list", "--porcelain"]).stdout
    result: dict[str, Path] = {}
    current: Path | None = None
    for line in output.splitlines():
        if line.startswith("worktree "):
            current = Path(line.removeprefix("worktree "))
        elif line.startswith("branch refs/heads/") and current is not None:
            result[line.removeprefix("branch refs/heads/")] = current
    return result


def ensure_worktree(entry: dict, *, create: bool = True) -> Path:
    branch = entry["branch"]
    existing = worktree_map().get(branch)
    if existing:
        return existing
    target = ROOT.parent / f"{ROOT.name.removesuffix('-main')}-{branch}"
    if not create:
        return target
    local = git(["show-ref", "--verify", f"refs/heads/{branch}"], check=False)
    if local.returncode == 0:
        git(["worktree", "add", str(target), branch], capture=False)
    else:
        git(["worktree", "add", "-b", branch, str(target), f"origin/{branch}"], capture=False)
    return target


def git_state(worktree: Path) -> dict[str, object]:
    branch = git(["branch", "--show-current"], worktree).stdout.strip()
    commit = git(["rev-parse", "--short=8", "HEAD"], worktree).stdout.strip()
    dirty = bool(git(["status", "--porcelain"], worktree).stdout.strip())
    upstream_result = git(["rev-parse", "--abbrev-ref", "@{upstream}"], worktree, check=False)
    ahead = behind = None
    if upstream_result.returncode == 0:
        counts = git(["rev-list", "--left-right", "--count", "@{upstream}...HEAD"], worktree).stdout.split()
        behind, ahead = (int(value) for value in counts)
    return {
        "branch": branch,
        "commit": commit,
        "dirty": dirty,
        "ahead": ahead,
        "behind": behind,
    }


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def java_major(java_home: Path) -> int | None:
    executable = java_home / "bin" / "java"
    if not executable.is_file():
        return None
    result = run([str(executable), "-version"], capture=True, check=False)
    text = result.stderr or result.stdout
    match = re.search(r'version "(?:1\.)?(\d+)', text)
    return int(match.group(1)) if match else None


def java_home(required: int) -> Path:
    env_names = [f"LMLP_JAVA_{required}_HOME", f"JAVA_HOME_{required}"]
    candidates: list[Path] = []
    for name in env_names:
        if os.environ.get(name):
            candidates.append(Path(os.environ[name]).expanduser())
    if os.environ.get("JAVA_HOME"):
        candidates.append(Path(os.environ["JAVA_HOME"]).expanduser())
    if platform.system() == "Darwin":
        candidates.extend([
            Path(f"/opt/homebrew/opt/openjdk@{required}/libexec/openjdk.jdk/Contents/Home"),
            Path("/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"),
        ])
        result = run(["/usr/libexec/java_home", "-v", str(required)], capture=True, check=False)
        if result.returncode == 0 and result.stdout.strip():
            candidates.append(Path(result.stdout.strip()))
    java_on_path = shutil.which("java")
    if java_on_path:
        candidates.append(Path(java_on_path).resolve().parent.parent)
    seen: set[Path] = set()
    fallback: tuple[Path, int] | None = None
    for candidate in candidates:
        candidate = candidate.resolve() if candidate.exists() else candidate
        if candidate in seen:
            continue
        seen.add(candidate)
        major = java_major(candidate)
        if major == required:
            return candidate
        if major and major > required and fallback is None:
            fallback = (candidate, major)
    if fallback:
        return fallback[0]
    raise LmlpError(
        f"找不到 Java {required}。请安装它，或设置 LMLP_JAVA_{required}_HOME。"
    )


def parse_gradle_properties(worktree: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    path = worktree / "gradle.properties"
    if not path.is_file():
        raise LmlpError(f"{worktree} 缺少已纳入 Git 的 gradle.properties")
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def artifact_for(worktree: Path, expected_version: str | None = None) -> Path:
    candidates = [path for path in (worktree / "build" / "libs").glob("*.jar")
                  if not any(marker in path.name for marker in ("-sources", "-dev", "-javadoc"))]
    if expected_version:
        exact = [path for path in candidates if expected_version in path.name]
        if exact:
            candidates = exact
    if not candidates:
        raise LmlpError(f"{worktree} 没有可部署的构建 JAR")
    return max(candidates, key=lambda path: path.stat().st_mtime_ns)


def prepare_dependencies(mc_version: str, entry: dict, worktree: Path) -> None:
    if not entry.get("local_dependencies", False):
        return
    destination = worktree / ".lmlp" / "dependencies"
    destination.mkdir(parents=True, exist_ok=True)
    dependencies = {
        "litematica": entry["litematica"],
        "malilib": entry["malilib"],
        "jei": entry["jei"],
        "modmenu": entry["modmenu"],
    }
    for artifact, version in dependencies.items():
        target = destination / f"{artifact}-{version}.jar"
        if target.is_file() and target.stat().st_size > 0:
            continue
        url = ("https://api.modrinth.com/maven/maven/modrinth/"
               f"{artifact}/{version}/{artifact}-{version}.jar")
        print(f"下载 {artifact} {version}（Minecraft {mc_version}）")
        request = urllib.request.Request(url, headers={"User-Agent": "LMLP-Build-Tool/1"})
        temporary = target.with_suffix(".jar.tmp")
        try:
            with urllib.request.urlopen(request, timeout=60) as source, temporary.open("wb") as output:
                shutil.copyfileobj(source, output)
            temporary.replace(target)
        finally:
            temporary.unlink(missing_ok=True)


def active_lmlp_jars(mods: Path) -> list[Path]:
    patterns = ("litematica_material_list_plus-*.jar", "litematica-material-list-plus-*.jar")
    result: list[Path] = []
    for pattern in patterns:
        result.extend(path for path in mods.glob(pattern) if path.is_file())
    return sorted(set(result))


def read_jar_metadata(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        fabric = json.loads(archive.read("fabric.mod.json"))
        try:
            raw_build = archive.read("lmlp-build.properties").decode("utf-8")
        except KeyError:
            raw_build = ""
        build: dict[str, str] = {}
        for line in raw_build.splitlines():
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                build[key.strip()] = value.strip()
        class_name = "io/github/huanmeng06/lmlp/LitematicaMaterialListPlus.class"
        bytecode = None
        if class_name in archive.namelist():
            header = archive.read(class_name)[:8]
            if len(header) == 8 and header[:4] == b"\xca\xfe\xba\xbe":
                bytecode = struct.unpack(">H", header[6:8])[0]
    return {"fabric": fabric, "build": build, "bytecode": bytecode}


def verify_jar(mc_version: str, entry: dict, jar: Path, *, deployed: Path | None = None,
               expected_release: str | None = None) -> dict:
    metadata = read_jar_metadata(jar)
    fabric = metadata["fabric"]
    errors: list[str] = []
    if fabric.get("id") != "litematica_material_list_plus":
        errors.append("fabric.mod.json 的模组 ID 不正确")
    minecraft_dep = fabric.get("depends", {}).get("minecraft", "")
    if mc_version not in str(minecraft_dep):
        errors.append(f"Minecraft 依赖不匹配：{minecraft_dep}")
    if "litematica_material_list_plus.mixins.json" not in fabric.get("mixins", []):
        errors.append("fabric.mod.json 缺少 Mixin 配置")
    build = metadata["build"]
    for key in ("version", "minecraft", "gitCommit", "gitDirty", "buildTime"):
        if not build.get(key):
            errors.append(f"构建信息缺少 {key}")
    if build.get("minecraft") and build["minecraft"] != mc_version:
        errors.append(f"构建信息 Minecraft 版本错误：{build['minecraft']}")
    if expected_release and not build.get("version", "").startswith(expected_release + "+mc"):
        errors.append(f"模组版本不是 {expected_release}：{build.get('version')}")
    release = int(entry.get("bytecode", entry["java"]))
    expected_major = release + 44
    if metadata["bytecode"] != expected_major:
        errors.append(f"Java 字节码应为 {expected_major}，实际为 {metadata['bytecode']}")
    if deployed and deployed.is_file() and sha256(deployed) != sha256(jar):
        errors.append("实例内 JAR 与构建 JAR 的 SHA-256 不一致")
    if errors:
        raise LmlpError(f"{mc_version} 校验失败：\n- " + "\n- ".join(errors))
    return {
        "minecraft": mc_version,
        "jar": str(jar),
        "sha256": sha256(jar),
        "version": build.get("version"),
        "commit": build.get("gitCommit"),
        "dirty": build.get("gitDirty"),
        "bytecode": metadata["bytecode"],
    }


def build_one(mc_version: str, entry: dict, release_version: str | None = None) -> Path:
    worktree = ensure_worktree(entry)
    prepare_dependencies(mc_version, entry, worktree)
    state = git_state(worktree)
    if state["branch"] != entry["branch"]:
        raise LmlpError(f"{worktree} 当前分支为 {state['branch']}，应为 {entry['branch']}")
    required_java = int(entry["java"])
    home = java_home(required_java)
    env = os.environ.copy()
    env["JAVA_HOME"] = str(home)
    env["PATH"] = str(home / "bin") + os.pathsep + env.get("PATH", "")
    gradlew = worktree / "gradlew"
    if not gradlew.is_file():
        raise LmlpError(f"{worktree} 缺少 Gradle Wrapper")
    command = [str(gradlew), "clean", "build", "--stacktrace"]
    expected_mod_version = None
    if release_version:
        expected_mod_version = f"{release_version}+mc{mc_version}"
        command.append(f"-Pmod_version={expected_mod_version}")
    print(f"\n构建 Minecraft {mc_version}（{entry['branch']}，Java {java_major(home)}）")
    run(command, worktree, env=env)
    return artifact_for(worktree, expected_mod_version)


def deploy_one(mc_version: str, entry: dict, *, build_first: bool = True) -> tuple[Path, Path]:
    worktree = ensure_worktree(entry)
    jar = build_one(mc_version, entry) if build_first else artifact_for(worktree)
    instance = Path(entry["instance"]).expanduser()
    mods = instance / "mods"
    if not mods.is_dir():
        raise LmlpError(f"实例 mods 文件夹不存在：{mods}")
    old = active_lmlp_jars(mods)
    if old:
        stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
        backup = mods / ".lmlp-backups" / stamp
        backup.mkdir(parents=True, exist_ok=True)
        for path in old:
            shutil.move(str(path), backup / path.name)
    target = mods / jar.name
    shutil.copy2(jar, target)
    verify_jar(mc_version, entry, jar, deployed=target)
    print(f"已部署：{target}")
    print(f"SHA-256：{sha256(target)}")
    return jar, target


def command_list(config: dict, _args: argparse.Namespace) -> None:
    print("Minecraft  分支         构建Java  字节码  Litematica  MaLiLib  JEI          状态")
    for mc, entry in config["versions"].items():
        status = "支持" if entry.get("supported") else f"停止（最后 {entry.get('last_release', '-')}）"
        print(f"{mc:<10} {entry['branch']:<12} {str(entry.get('java', '-')):<8} "
              f"{str(entry.get('bytecode', entry.get('java', '-'))):<6} "
              f"{entry.get('litematica', '-'):<10} {entry.get('malilib', '-'):<8} "
              f"{entry.get('jei', '-'):<12} {status}")


def command_status(config: dict, args: argparse.Namespace) -> None:
    print("Minecraft  分支         Commit    工作区  远端       已部署")
    mappings = worktree_map()
    for mc, entry in selected_versions(config, args.versions):
        worktree = mappings.get(entry["branch"])
        if not worktree:
            print(f"{mc:<10} {entry['branch']:<12} {'-':<9} 未检出")
            continue
        state = git_state(worktree)
        remote = "无上游" if state["ahead"] is None else f"+{state['ahead']}/-{state['behind']}"
        mods = Path(entry["instance"]).expanduser() / "mods"
        deployed = active_lmlp_jars(mods) if mods.is_dir() else []
        print(f"{mc:<10} {entry['branch']:<12} {state['commit']:<9} "
              f"{'有改动' if state['dirty'] else '干净':<5} {remote:<10} "
              f"{deployed[0].name if len(deployed) == 1 else str(len(deployed)) + ' 个 JAR'}")


def command_build(config: dict, args: argparse.Namespace) -> None:
    mc, entry = version_entry(config, args.version)
    jar = build_one(mc, entry, args.release_version)
    result = verify_jar(mc, entry, jar, expected_release=args.release_version)
    print(json.dumps(result, ensure_ascii=False, indent=2))


def command_build_all(config: dict, args: argparse.Namespace) -> None:
    results = []
    for mc, entry in selected_versions(config):
        jar = build_one(mc, entry, args.release_version)
        results.append(verify_jar(mc, entry, jar, expected_release=args.release_version))
    print(json.dumps(results, ensure_ascii=False, indent=2))


def command_deploy(config: dict, args: argparse.Namespace) -> None:
    mc, entry = version_entry(config, args.version)
    deploy_one(mc, entry, build_first=not args.no_build)


def command_verify(config: dict, args: argparse.Namespace) -> None:
    mc, entry = version_entry(config, args.version)
    worktree = ensure_worktree(entry, create=False)
    jar = artifact_for(worktree)
    deployed_jars = active_lmlp_jars(Path(entry["instance"]).expanduser() / "mods")
    deployed = deployed_jars[0] if len(deployed_jars) == 1 else None
    result = verify_jar(mc, entry, jar, deployed=deployed)
    print(json.dumps(result, ensure_ascii=False, indent=2))


REGRESSION_MARKERS = [
    ("src/main/java/io/github/huanmeng06/lmlp/recipe/RecipeResolvers.java", "isUnaryConversion(nested.get(0))", "单输入循环保护"),
    ("src/main/java/io/github/huanmeng06/lmlp/gui/MinimalSubMaterialListView.java", "isLogLike(itemPath(icon))", "原木终止拆分"),
    ("src/main/java/io/github/huanmeng06/lmlp/recipe/jei/JeiRecipeResolver.java", "skipped invalid entry", "JEI 单配方异常隔离"),
    ("src/main/java/io/github/huanmeng06/lmlp/gui/MinimalSubMaterialListView.java", "budgetNs", "最小子材料计算预算"),
]


def regression_check(mc: str, entry: dict) -> list[str]:
    worktree = ensure_worktree(entry, create=False)
    failures: list[str] = []
    for relative, marker, label in REGRESSION_MARKERS:
        path = worktree / relative
        if not path.is_file() or marker not in path.read_text(encoding="utf-8"):
            failures.append(f"{label}：{relative} 缺少 `{marker}`")
    for required in ("build.gradle", "settings.gradle", "gradle.properties", "gradlew"):
        if not (worktree / required).is_file():
            failures.append(f"可复现构建缺少 {required}")
    if failures:
        return [f"{mc}: {failure}" for failure in failures]
    return []


def command_regression(config: dict, args: argparse.Namespace) -> None:
    failures: list[str] = []
    for mc, entry in selected_versions(config, args.versions):
        failures.extend(regression_check(mc, entry))
        if not failures or not any(item.startswith(mc + ":") for item in failures):
            print(f"{mc}: OK")
    if failures:
        raise LmlpError("回归保护检查失败：\n- " + "\n- ".join(failures))


def command_debug_bundle(config: dict, args: argparse.Namespace) -> None:
    mc, entry = version_entry(config, args.version)
    instance = Path(args.instance).expanduser() if args.instance else Path(entry["instance"]).expanduser()
    if not instance.is_dir():
        raise LmlpError(f"找不到实例：{instance}")
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    output = Path(args.output).expanduser() if args.output else ROOT / "debug-bundles" / f"lmlp-debug-mc{mc}-{stamp}.zip"
    output.parent.mkdir(parents=True, exist_ok=True)
    manifest = {
        "createdAt": stamp,
        "minecraft": mc,
        "expected": entry,
        "system": {"platform": platform.platform(), "python": platform.python_version()},
        "files": [],
    }
    candidates = [
        instance / "logs" / "latest.log",
        instance / "config" / "litematica_material_list_plus.json",
        instance / "config" / "litematica" / "litematica_material_list_plus.json",
    ]
    crash_dir = instance / "crash-reports"
    if crash_dir.is_dir():
        candidates.extend(sorted(crash_dir.glob("*.txt"), key=lambda path: path.stat().st_mtime, reverse=True)[:3])
    mods = instance / "mods"
    deployed = active_lmlp_jars(mods) if mods.is_dir() else []
    if deployed:
        try:
            manifest["deployedJar"] = {
                "name": deployed[0].name,
                "sha256": sha256(deployed[0]),
                "metadata": read_jar_metadata(deployed[0]),
            }
        except Exception as exc:  # diagnostics must continue
            manifest["deployedJarError"] = str(exc)
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in candidates:
            if path.is_file():
                arcname = f"files/{path.relative_to(instance)}"
                archive.write(path, arcname)
                manifest["files"].append(arcname)
        if mods.is_dir():
            names = "\n".join(sorted(path.name for path in mods.iterdir() if path.is_file())) + "\n"
            archive.writestr("mod-list.txt", names)
        archive.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2, default=str))
    print(f"调试包：{output}")


def ensure_release_ready(config: dict) -> None:
    failures = []
    for mc, entry in selected_versions(config):
        worktree = ensure_worktree(entry)
        state = git_state(worktree)
        if state["dirty"]:
            failures.append(f"{mc} 工作区有未提交改动")
        if state["ahead"] is None:
            failures.append(f"{mc} 没有远端上游")
        elif state["ahead"] or state["behind"]:
            failures.append(f"{mc} 与远端不同步（+{state['ahead']}/-{state['behind']}）")
    if failures:
        raise LmlpError("不能准备 Release：\n- " + "\n- ".join(failures))


def command_release_prepare(config: dict, args: argparse.Namespace) -> None:
    ensure_release_ready(config)
    tag = f"v{args.release_version}"
    if git(["rev-parse", "-q", "--verify", f"refs/tags/{tag}"], check=False).returncode == 0:
        raise LmlpError(f"Tag {tag} 已存在；正式版本禁止覆盖")
    remote_tag = git(["ls-remote", "--tags", "origin", f"refs/tags/{tag}"], check=False).stdout.strip()
    if remote_tag:
        raise LmlpError(f"远端 Tag {tag} 已存在；正式版本禁止覆盖")
    destination = ROOT / "dist" / tag
    if destination.exists():
        raise LmlpError(f"发布目录已存在：{destination}")
    destination.mkdir(parents=True)
    results = []
    for mc, entry in selected_versions(config):
        jar = build_one(mc, entry, args.release_version)
        checked = verify_jar(mc, entry, jar, expected_release=args.release_version)
        target = destination / jar.name
        shutil.copy2(jar, target)
        checked["asset"] = target.name
        checked["branch"] = entry["branch"]
        checked["sourceCommit"] = git(["rev-parse", "HEAD"], ensure_worktree(entry)).stdout.strip()
        results.append(checked)
    sums = "\n".join(f"{item['sha256']}  {item['asset']}" for item in results) + "\n"
    (destination / "SHA256SUMS.txt").write_text(sums, encoding="utf-8")
    manifest = {"tag": tag, "createdAt": dt.datetime.now(dt.timezone.utc).isoformat(), "assets": results}
    (destination / "release-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Release 候选已准备：{destination}")
    print("请先完成游戏内测试，再执行 release-publish。")


def command_release_publish(config: dict, args: argparse.Namespace) -> None:
    tag = f"v{args.release_version}"
    destination = ROOT / "dist" / tag
    manifest = destination / "release-manifest.json"
    notes = Path(args.notes).resolve()
    if not manifest.is_file() or not notes.is_file():
        raise LmlpError("缺少 release-manifest.json 或 Release description")
    existing = run(["gh", "release", "view", tag], ROOT, capture=True, check=False)
    if existing.returncode == 0:
        raise LmlpError(f"GitHub Release {tag} 已存在；禁止替换附件")
    assets = sorted(destination.glob("*.jar")) + [destination / "SHA256SUMS.txt", manifest]
    command = ["gh", "release", "create", tag, "--target", config["repository"]["default_branch"],
               "--title", f"LMLP {tag}", "--notes-file", str(notes), *map(str, assets)]
    run(command, ROOT)
    print(f"已发布不可变 Release：{tag}")


def command_ci_metadata(config: dict, args: argparse.Namespace) -> None:
    branch = args.branch
    for mc, entry in config["versions"].items():
        if entry.get("branch") == branch and entry.get("supported"):
            print(f"minecraft={mc}")
            print(f"java={entry['java']}")
            print(f"bytecode={entry.get('bytecode', entry['java'])}")
            return
    raise LmlpError(f"分支 {branch} 不在受支持版本清单中")


def command_ci_verify(config: dict, args: argparse.Namespace) -> None:
    mc, entry = version_entry(config, args.version)
    source = Path(args.source).resolve()
    failures: list[str] = []
    for relative, marker, label in REGRESSION_MARKERS:
        path = source / relative
        if not path.is_file() or marker not in path.read_text(encoding="utf-8"):
            failures.append(f"{label}：{relative} 缺少 `{marker}`")
    if failures:
        raise LmlpError("回归保护检查失败：\n- " + "\n- ".join(failures))
    jar = artifact_for(source)
    result = verify_jar(mc, entry, jar)
    print(json.dumps(result, ensure_ascii=False, indent=2))


def command_prepare(config: dict, args: argparse.Namespace) -> None:
    mc, entry = version_entry(config, args.version)
    source = Path(args.source).resolve() if args.source else ensure_worktree(entry)
    prepare_dependencies(mc, entry, source)
    print(f"{mc}: 依赖准备完成")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="LMLP 统一版本管理工具")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("list", help="显示支持版本和依赖")
    status = sub.add_parser("status", help="显示分支、远端和部署状态")
    status.add_argument("versions", nargs="*")
    build = sub.add_parser("build", help="构建并校验一个版本")
    build.add_argument("version")
    build.add_argument("--release-version")
    build_all = sub.add_parser("build-all", help="构建并校验全部版本")
    build_all.add_argument("--release-version")
    deploy = sub.add_parser("deploy", help="部署到对应测试实例")
    deploy.add_argument("version")
    deploy.add_argument("--no-build", action="store_true")
    verify = sub.add_parser("verify", help="校验最新构建与部署文件")
    verify.add_argument("version")
    regression = sub.add_parser("regression-check", help="检查历史 Bug 保护逻辑")
    regression.add_argument("versions", nargs="*")
    debug = sub.add_parser("debug-bundle", help="收集不含存档的调试包")
    debug.add_argument("version")
    debug.add_argument("--instance")
    debug.add_argument("--output")
    release_prepare = sub.add_parser("release-prepare", help="准备不可变 Release 附件")
    release_prepare.add_argument("release_version")
    release_publish = sub.add_parser("release-publish", help="发布新的 GitHub Release")
    release_publish.add_argument("release_version")
    release_publish.add_argument("--notes", required=True)
    ci = sub.add_parser("ci-metadata", help=argparse.SUPPRESS)
    ci.add_argument("--branch", required=True)
    ci_verify = sub.add_parser("ci-verify", help=argparse.SUPPRESS)
    ci_verify.add_argument("version")
    ci_verify.add_argument("--source", required=True)
    prepare = sub.add_parser("prepare", help="准备版本所需的本地依赖缓存")
    prepare.add_argument("version")
    prepare.add_argument("--source")
    return parser


COMMANDS = {
    "list": command_list,
    "status": command_status,
    "build": command_build,
    "build-all": command_build_all,
    "deploy": command_deploy,
    "verify": command_verify,
    "regression-check": command_regression,
    "debug-bundle": command_debug_bundle,
    "release-prepare": command_release_prepare,
    "release-publish": command_release_publish,
    "ci-metadata": command_ci_metadata,
    "ci-verify": command_ci_verify,
    "prepare": command_prepare,
}


def main() -> int:
    args = build_parser().parse_args()
    try:
        COMMANDS[args.command](load_config(), args)
        return 0
    except (LmlpError, subprocess.CalledProcessError, zipfile.BadZipFile) as exc:
        print(f"错误：{exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
