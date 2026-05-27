# Litematica Material List Plus

Litematica Material List Plus 是一个用于 Minecraft 1.20.1 的 Fabric 客户端模组，用来增强 Litematica 材料列表的备料体验。

当前版本：`0.1.15+mc1.20.1`

## 功能简介

- 在材料列表中显示更易读的数量格式，例如 `9 盒 + 16 组 + 13 个`。
- 单击材料行可展开配方摘要。
- `Shift + 单击`材料行可打开配方详情页。
- 配方详情页复用 REI 的原生配方显示和物品交互。
- 保留 Litematica 原有材料统计、排序和导出行为。

## 安装环境

必须使用以下环境：

- Minecraft `1.20.1`
- Fabric Loader `>=0.16.9`
- Java `17`
- Litematica `>=0.15.4`
- MaLiLib `>=0.16.3`
- Roughly Enough Items / REI `12.x`

注意：REI 是必要前置。本模组的配方数据与配方界面依赖 REI。

## 安装方式

1. 下载或构建本模组 jar。
2. 将 jar 放入 Minecraft 实例的 `mods` 文件夹。
3. 确认同一实例中已安装 Fabric、Litematica、MaLiLib 和 REI。
4. 启动游戏，打开 Litematica 的材料列表界面。

## 本地构建

本仓库当前使用 PowerShell 脚本基于本地 Minecraft 实例编译。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

如果需要指定实例路径：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -InstanceDir "D:\path\to\your\instance"
```

构建产物会输出到：

```text
build\libs\
```

## 许可证

MIT
