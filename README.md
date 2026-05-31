# Litematica Material List Plus

Litematica Material List Plus, 简称 LMLP, 是一个面向 Minecraft Fabric 客户端的 Litematica 材料清单增强模组。

它让材料列表不只告诉你“还缺多少”，还尽量告诉你“这些东西继续往下拆以后，到底需要准备哪些基础材料”。同时，它保留 Litematica 原本的列表、排序、忽略、导出和热键习惯，只在备料、查配方和看数量这些地方补上更顺手的体验。

最新正式版：`v1.3.0`

当前主要发布构建：`1.3.0+mc1.20.6`

适配方向：Minecraft / Fabric / Litematica / MaLiLib / REI

## 核心功能

### 更易读的材料数量

LMLP 会把材料数量格式化成更适合备料的形式：

```text
493 = 7 组 + 45 个
4437 = 2 盒 + 15 组 + 21 个
```

数量单位来自语言文件，方便多语言显示，也减少手动换算整组、潜影盒和散件数量的麻烦。

### 配方摘要

在 Litematica 材料列表中，单击材料行可以直接展开配方摘要。可合成材料会显示主要配方、产出数量、需要合成的次数，以及它的总子材料。

<img width="2541" height="1440" alt="配方摘要" src="https://github.com/user-attachments/assets/f22f42bb-e4ec-45e6-bd7d-18a6d9ba72c4" />

摘要页里的子材料也可以继续展开，形成树状结构。例如：

- 漏斗可以继续拆成箱子和铁锭。
- 箱子可以继续拆成任意木板。
- 木板可以继续拆成对应原木。

展开和收起带有短动画，下面的材料行会跟随高度自然移动，不会突然跳到最终位置。

### 配方详情页

`Shift + 单击`材料行可以打开完整配方详情页。

<img width="2554" height="1436" alt="配方详情页" src="https://github.com/user-attachments/assets/c5d9d8bb-6999-48e5-9d46-e9eaa2d74d1b" />

详情页用于查看一个材料的完整配方来源。它会尽量复用 REI 原生配方布局、物品交互、tooltip 和配方转移按钮，因此工作台、切石机、高炉等不同类型的配方都能保持接近 REI 的展示方式。

详情页支持：

- 同一材料的多个配方逐项展示。
- 子材料继续展开为完整配方框。
- 使用材料列表热键返回原加工界面。
- 在从加工界面进入时，可以使用 REI 转移逻辑把材料摆入当前容器。

### 容器界面支持打开材料列表

在工作台、高炉、切石机等容器界面中，可以使用 Litematica 原版材料列表热键打开材料列表。方便进行物品合成：

- 从容器界面打开材料列表后，不会直接丢掉原加工界面。
- 从配方详情页返回时，可以回到原加工界面。
- 再次打开时会恢复上次的详情页状态。

### 可控制的递归拆分

有些材料不应该继续往下拆。比如红石粉通常应该作为基础材料，而不是继续拆成红石块配方。

LMLP 提供 `配方停止拆分物品` 配置。默认包含：

```text
minecraft:redstone
```

你可以继续加入：

```text
minecraft:iron_ingot
minecraft:gold_ingot
...
```

### 鼠标悬停浮窗

LMLP 提供更简洁的悬停浮窗。默认状态显示简略信息，按住热键（默认为`左Alt`）后显示更完整的数量：

```text
物品名
总数
缺少
可用
```

如果你更喜欢 Litematica 原本的材料悬停显示，或者想彻底隐藏悬停浮窗，可以在配置中切换：

```text
LMLP / Litematica 原版 / 不显示悬停浮窗
```

## 默认配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `悬停浮窗模式` | `LMLP` | 可选择 LMLP、Litematica 原版，或不显示材料悬停浮窗。 |
| `配方停止拆分物品` | `minecraft:redstone` | 列表中的物品会被当作基础材料，不再继续递归拆分配方。 |
| `打开配置界面` | `M + =` | 打开 LMLP 配置菜单。 |
| `显示详细浮窗` | `LEFT_ALT` | 悬浮在材料行上时，按住显示详细数量信息。 |

所有热键都可以在 MaLiLib 配置菜单中重新绑定或清空。

## 使用方式

1. 在 Litematica 中打开材料列表。
2. 单击材料行，在列表内展开配方摘要。
3. 单击子材料左侧箭头，继续展开嵌套材料。
4. `Shift + 单击`材料行，打开完整配方详情页。
5. 如果某些材料不应该继续拆，在配置菜单的 `配方停止拆分物品` 中加入物品 ID。

## 安装

1. 从 [Releases](https://github.com/huanmeng06/litematica_material_list_plus/releases) 下载与你的 Minecraft 版本匹配的 jar。
2. 将 jar 放入对应 Minecraft 版本的 `mods` 文件夹。
3. 确认同一实例中已经安装 Fabric API、Litematica、MaLiLib 和 REI。
4. 启动游戏，打开 Litematica 材料列表。

## 兼容环境

| Minecraft | 最新发布 | Java | 前置模组 |
| --- | --- | --- | --- |
| `1.20.6` | `1.3.0+mc1.20.6` | `21` | Fabric API `0.100.8+1.20.6`<br>Litematica `0.18.3`<br>MaLiLib `0.19.2`<br>REI `15.0.787` |
| `1.20.4` | `1.0.1+mc1.20.4` | `17` | Fabric API `0.97.3+1.20.4`<br>Litematica `0.17.4`<br>MaLiLib `0.18.4-alpha.1`<br>REI `14.1.786` |
| `1.20.1` | `1.0.1+mc1.20.1` | `17` | Fabric API `0.92.9+1.20.1`<br>Litematica `0.15.4`<br>MaLiLib `0.16.3`<br>REI `12.1.785` |

REI 是必要前置。LMLP 的配方数据、配方详情页和原生配方显示都依赖 REI。Architectury API、Cloth Config API 和 Cloth Basic Math 等为 REI 相关前置，请按实际 REI 安装要求一并安装。

## 本地构建

仓库提供了一个 PowerShell 构建脚本，会基于本地 Minecraft 实例中的 remapped jar 和 mods 目录编译。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

如果需要指定实例路径：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 `
  -InstanceDir "D:\path\to\your\instance"
```

构建 Minecraft `1.20.6` 版本时建议指定 Java 21：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 `
  -InstanceDir "D:\path\to\your\1.20.6-instance" `
  -JavaHome "C:\path\to\jdk-21" `
  -JavaRelease 21
```

构建产物会输出到：

```text
build\libs\
```

## 分支说明

- `main` 用于项目主页文档、通用说明和发布入口。
- `dev-newFeature` 用于记录开发过程和小版本更新日志。
- `mc1.xx.x` 分别维护对应 Minecraft 版本代码。

## 许可证

本项目使用 LGPL-3.0-or-later 发布。Litematica、MaLiLib、REI、Fabric API 等依赖模组遵循其各自许可证，本项目不随 jar 打包这些依赖。
