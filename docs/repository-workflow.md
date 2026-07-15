# LMLP 版本管理与调试流程

## 你只需要记住的规则

1. `main` 是项目总入口，保存版本表、管理工具和说明。
2. `mc...` 分支分别保存对应 Minecraft 版本的源码。
3. 已发布的 Release 不覆盖；修复后发布新补丁版本，例如 `1.8.1`。
4. 测试包使用 `1.8.1-rc.1`，确认后再发布正式版。
5. 每个 JAR 都包含 Git 提交号，可以从日志确认玩家实际运行的文件。

注意：Minecraft 1.20.1/1.20.4 的成品仍是 Java 17 字节码，但当前 Gradle/Loom
构建进程使用 Java 21；工具会分别校验这两个版本，玩家不需要因此升级到 Java 21。

## 常用命令

在 `main` 工作目录执行：

```bash
./tools/lmlp list
./tools/lmlp status
./tools/lmlp build 1.21.11
./tools/lmlp deploy 1.21.11
./tools/lmlp verify 1.21.11
./tools/lmlp build-all
```

出现问题时收集调试包：

```bash
./tools/lmlp debug-bundle 1.21.11
```

调试包会收集最新日志、LMLP 配置、模组文件名和构建信息，不包含存档和完整模组 JAR。

## 正式发布

先准备候选附件：

```bash
./tools/lmlp release-prepare 1.8.1
```

工具会构建所有受支持版本、校验 JAR、生成 SHA-256 和分支提交清单。人工测试通过后才执行发布：

```bash
./tools/lmlp release-publish 1.8.1 --notes docs/release-notes-v1.8.1.md
```

如果 GitHub 已经存在同名 Tag 或 Release，工具会直接停止，避免覆盖正式版本。

## 新版本迁移

1. 在 `versions.toml` 登记 Minecraft、Java、Fabric API、Litematica、MaLiLib 和 JEI 版本。
2. 创建对应的 `mc...` 分支和 worktree。
3. 迁移源码并运行 `build`、`verify`、`deploy`。
4. 运行 `regression-check`，再按照 `docs/version-migration-guide.md` 进行游戏内检查。
5. 确认后提交和推送，不直接覆盖旧 Release。

## 历史问题回归项

自动静态检查覆盖以下保护逻辑是否仍然存在：

- 配方循环只沿单输入转换继续检查。
- 原木、木材、菌柄和菌核作为最终原材料停止拆分。
- 单条第三方 JEI 配方异常不会清空全部配方。
- 最小子材料计算保留时间预算，避免长时间卡死。

光束、HUD、Tooltip、热键和列表布局仍需在实例中人工检查，因为它们依赖实际渲染和输入环境。
