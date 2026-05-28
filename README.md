# Litematica Material List Plus

Litematica Material List Plus 是一个用于 Minecraft Fabric 客户端的 Litematica 材料列表增强模组，用来改善备料统计、材料拆解和配方查看体验。

当前正式版本：`1.1.0`

当前发布构建：`1.1.0+mc1.20.6`

## 功能简介

- 在材料列表中显示更易读的数量格式，例如 `9 盒 + 16 组 + 13 个`。
- 单击材料行可在列表内展开配方摘要，并继续展开子材料。
- `Shift + 单击`材料行可打开配方详情页。
- 配方详情页复用 REI 的原生配方显示、物品交互和 tooltip。
- 详情页的子材料可以继续展开为完整 REI 配方框。
- 可识别并简化替代材料显示，例如“任意木板”，并循环展示可替代图标。
- 将材料悬浮信息固定到右上方空白区域，减少鼠标附近 tooltip 遮挡。
- 保留 Litematica 原有材料统计、排序、忽略和导出行为。

<img width="2541" height="1440" alt="image" src="https://github.com/user-attachments/assets/f22f42bb-e4ec-45e6-bd7d-18a6d9ba72c4" />
<img width="2554" height="1436" alt="image" src="https://github.com/user-attachments/assets/c5d9d8bb-6999-48e5-9d46-e9eaa2d74d1b" />

## 1.1.0 新功能

- 新增“套娃式”材料展开：例如粘性活塞可以展开为活塞和粘液球，活塞还能继续展开为木板、圆石、铁锭和红石粉。
- 摘要页支持内联树形展开，展开后不会让同级材料消失。
- 详情页支持对子材料继续展开，并直接显示 REI 原生配方界面。
- 详情页会为同一材料显示 REI 返回的多个配方来源。
- 切石机、高炉等非工作台配方会保留 REI 风格的工作站小图标。
- 展开箭头改用 Minecraft 原版 `minecraft:recipe_book/page_forward` 资源，并支持资源包替换。
- 优化底部材料展开后的滚动范围，减少列表最底部内容看不全的问题。
- 将材料信息 tooltip 改为固定小面板，避免跟随鼠标遮挡视野。
- 适配 Minecraft `1.20.6`、Java `21` 和 REI `15.0.787`。

## 与 1.0.1 相比

| 项目 | 1.0.1 | 1.1.0 |
| :--- | :--- | :--- |
| Minecraft 版本 | 主要面向 1.20.1 / 1.20.4 | 新增 1.20.6 发布构建 |
| 材料展开 | 单层配方摘要 | 支持递归子材料展开 |
| 详情页 | 主材料显示 REI 配方 | 主材料和子材料都可显示 REI 原生配方 |
| 多配方来源 | 摘要式展示为主 | 详情页直接渲染 REI 返回的多个配方 |
| 替代材料 | 原始标签较长 | 简化替代材料名称并循环图标 |
| UI | 鼠标附近悬浮信息 | 固定右上小面板，减少遮挡 |
| 滚动 | 展开底部条目时可能看不全 | 动态高度和滚动范围已优化 |

## 前置模组

- Fabric API
- Litematica
- MaLiLib
- Roughly Enough Items (REI)

## 兼容环境

本项目会按 Minecraft 版本分别维护和发布。当前已适配并测试的版本组合如下：

<div align="center">

| Minecraft | 最新发布 | Fabric Loader | Java | 前置模组 |
| :---: | :---: | :---: | :---: | :--- |
| `1.20.1` | `1.0.1+mc1.20.1` | `>=0.16.9` | `17` | Fabric API `0.92.9+1.20.1`<br>Litematica `0.15.4`<br>MaLiLib `0.16.3`<br>REI `12.1.785` |
| `1.20.4` | `1.0.1+mc1.20.4` | `0.19.2` | `17` | Fabric API `0.97.3+1.20.4`<br>Litematica `0.17.4`<br>MaLiLib `0.18.4-alpha.1`<br>REI `14.1.786` |
| `1.20.6` | `1.1.0+mc1.20.6` | `0.19.2` | `21` | Fabric API `0.100.8+1.20.6`<br>Litematica `0.18.3`<br>MaLiLib `0.19.2`<br>REI `15.0.787` |

</div>

注意：REI 是必要前置。本模组的配方数据与配方界面依赖 REI。Architectury API、Cloth Config API 和 Cloth Basic Math 等为 REI 相关前置，请按实际 REI 安装要求一并安装。

## 安装方式

1. 从 GitHub Release 下载与你的 Minecraft 版本匹配的 jar。
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

构建 Minecraft 1.20.6 版本时需要使用 Java 21：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -InstanceDir "D:\path\to\your\1.20.6-instance" -JavaHome "C:\path\to\jdk-21" -JavaRelease 21
```

构建产物会输出到：

```text
build\libs\
```

## 已知问题

- 当前 `1.1.0` 的新功能优先在 Minecraft `1.20.6` 上发布和测试。
- `1.20.1` / `1.20.4` 暂时保留 `1.0.1` 系列功能，后续可按需回移植。
- 其他 Minecraft 版本或前置模组版本暂不保证兼容。

## 许可证

本项目使用 MIT License 发布。Litematica、MaLiLib、REI 等依赖模组遵循其各自许可证，本项目不随 jar 打包这些依赖。
