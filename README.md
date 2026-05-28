# Litematica Material List Plus

Litematica Material List Plus 是一个用于 Minecraft 1.20.6 的 Fabric 客户端模组，用来增强 Litematica 材料列表的备料体验。

当前版本：`1.1.12+mc1.20.6`

## 功能简介

- 在材料列表中显示更易读的数量格式，例如 `9 盒 + 16 组 + 13 个`。
- 单击材料行可展开配方摘要。
- `Shift + 单击`材料行可打开配方详情页。
- 配方详情页复用 REI 的原生配方显示和物品交互。
- 配方详情页的子材料可继续展开为 REI 原生配方框。
- 保留 Litematica 原有材料统计、排序和导出行为。
<img width="2541" height="1440" alt="image" src="https://github.com/user-attachments/assets/f22f42bb-e4ec-45e6-bd7d-18a6d9ba72c4" />
<img width="2554" height="1436" alt="image" src="https://github.com/user-attachments/assets/c5d9d8bb-6999-48e5-9d46-e9eaa2d74d1b" />

## 兼容环境

本项目会按 Minecraft 版本逐步适配。当前已适配并测试的版本组合如下：

<div align="center">

| Minecraft | 状态 | Fabric Loader | Java | 前置模组 |
| :---: | :---: | :---: | :---: | :--- |
| `1.20.6` | 已适配并测试 | `0.19.2` | `21` | Fabric API `0.100.8+1.20.6`<br>Litematica `0.18.3`<br>MaLiLib `0.19.2`<br>Roughly Enough Items / REI `15.0.787` |

</div>

注意：REI 是必要前置。本模组的配方数据与配方界面依赖 REI。Architectury API 和 Cloth Config API 为 REI 相关前置，请按实际 REI 安装要求一并安装。

## 已知问题

- 目前仅对上方兼容表中列出的版本组合进行适配与测试。
- 其他 Minecraft 版本或前置模组版本正在进一步适配中，暂不保证兼容。

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

本项目使用 MIT License 发布。Litematica、MaLiLib、REI 等依赖模组遵循其各自许可证，本项目不随 jar 打包这些依赖。
