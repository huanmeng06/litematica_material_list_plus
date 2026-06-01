# Litematica Material List Plus

Litematica Material List Plus, 简称 LMLP, 是一个面向 Minecraft Fabric 客户端的 Litematica 材料清单增强模组。

它围绕“备料时到底还缺什么”这个问题扩展 Litematica 的材料列表：把数量换算成更容易理解的盒、组、个，直接在材料列表中查看配方摘要和递归子材料，选择首选配方，并把最终需要准备的子材料导出为 Excel 表格。

LMLP 会尽量保留 Litematica 原本的列表、排序、忽略、导出和热键习惯，只在数量阅读、配方查看、材料拆分和导出整理这些地方补上更顺手的体验。

最新正式版：`v1.4.0`

当前主要发布构建：`1.4.0+mc1.20.6`

适配方向：Minecraft `1.20.6` / Fabric / Litematica / MaLiLib / REI

## 功能概览

| 功能 | 说明 |
| --- | --- |
| 数量显示优化 | 支持把总数换算为盒、组、个，也可以切换为多种显示样式。 |
| 配方摘要展开 | 在材料列表中单击材料，即可内联展开配方和总子材料。 |
| 配方详情页 | `Shift + 单击`材料行打开完整配方详情页，复用 REI 原生配方布局。 |
| 递归子材料 | 摘要页和详情页都可以继续展开子材料，逐层拆到基础材料。 |
| 配方置顶 | 为同一物品设置首选配方，影响详情页排序、摘要页快速查看和递归拆分结果。 |
| 停止拆分列表 | 可配置哪些物品被当作基础材料，不再继续往下拆配方。 |
| 悬停浮窗模式 | 可选择 LMLP 浮窗、Litematica 原版浮窗，或完全隐藏浮窗。 |
| 容器界面热键 | 在工作台、切石机、高炉等容器界面中，也可以用材料列表热键打开材料列表。 |
| 子材料 XLSX 导出 | 导出当前置顶配方下的最小子材料明细和汇总表。 |

## 核心功能

### 更易读的材料数量

LMLP 可以把材料数量格式化成更适合备料的形式，例如：

```text
755 = 11 组 + 51 个
4437 = 2 盒 + 15 组 + 21 个
```

数量显示方式可以在配置中选择：

<div align="center">

<table>
  <tr>
    <th align="center">样式</th>
    <th align="center">显示方式</th>
  </tr>
  <tr>
    <td align="center">样式1</td>
    <td align="center">盒数 + 组数 + 个数</td>
  </tr>
  <tr>
    <td align="center">样式2</td>
    <td align="center">总数 = 盒数 + 组数 + 个数</td>
  </tr>
  <tr>
    <td align="center">样式3</td>
    <td align="center">A × SB + B × 64 (16) + C</td>
  </tr>
  <tr>
    <td align="center">样式4</td>
    <td align="center">总数，保持 Litematica 原版显示</td>
  </tr>
</table>

</div>

当样式较长时，材料列表会按当前最长数量文本自适应扩展宽度，减少总计、缺少和可用列互相重叠。

<div align="center">

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/d6fb2bb4-4deb-498d-a753-0012e0f19833" width="500"></td>
    <td><img src="https://github.com/user-attachments/assets/056952d6-aea6-4d93-b932-cf2d95c6eb39" width="500"></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/74a5d175-1a1b-443f-9bc3-59ccc61531d1" width="500"></td>
    <td><img src="https://github.com/user-attachments/assets/5811a986-a89f-4a02-ba7d-093d3fe6084f" width="500"></td>
  </tr>
</table>

</div>


### 配方摘要

在 Litematica 材料列表中，单击材料行可以直接展开配方摘要。可合成材料会显示当前首选配方、每次产出数量、需要合成的次数，以及该配方对应的总子材料。

摘要中的子材料也可以继续展开，形成递归层级。例如：

- 活塞可以继续拆成木板、圆石、铁锭和红石粉。
- 粘性活塞可以继续拆成活塞和粘液球。
- 木板可以继续拆成任意原木。

展开和收起带有短动画，下面的材料行会跟随高度自然移动。靠近底部的材料展开时，也会预留足够滚动空间，避免展开后还要继续下滑才能看全。

> 图片占位符：材料列表内联配方摘要截图

### 配方详情页

`Shift + 单击`材料行可以打开完整配方详情页。

详情页用于查看一个材料的所有可用配方来源。它会尽量复用 REI 原生配方布局、物品交互、tooltip 和配方转移按钮，因此工作台、切石机、高炉等不同类型的配方都能保持接近 REI 的展示方式。

详情页支持：

- 同一材料的多个配方逐项展示。
- 子材料继续展开为完整配方框。
- 小箭头跟随展开/收起进度旋转。
- 展开和收起带高度动画。
- 使用材料列表热键返回原加工界面。
- 从加工界面进入时，可以使用 REI 转移逻辑把材料摆入当前容器。

> 图片占位符：配方详情页截图

### 配方置顶

如果同一个物品存在多个配方，可以在配方详情页点击星标按钮设置首选配方。

置顶配方会保存到配置文件，并同步影响：

- 配方详情页中的配方排序。
- 材料列表摘要中的快速查看结果。
- 递归子材料拆分时使用的配方。
- XLSX 导出时的最小子材料结果。

再次点击已置顶配方可以取消置顶。置顶按钮使用四态像素素材，支持未置顶、悬浮未置顶、已置顶、悬浮已置顶，并带有白色悬浮边框和 tooltip。

当配方置顶导致列表重排时，配方框会按现有展开动画速度移动到新的排序位置。页面不会跟着被置顶配方滚到顶部，方便继续浏览剩余配方。子材料里的配方列表也支持同样的置顶重排动画。

> 图片占位符：配方置顶按钮和重排效果截图

### 可控制的递归拆分

有些材料不应该继续往下拆。比如铁锭、金锭、红石粉、石英等，在很多备料场景里应该被当作基础材料，而不是继续拆成其他配方来源。

LMLP 提供 `recipeStopItems` 配置。列表中的物品会被当作基础材料，不再继续递归拆分配方。

默认停止拆分列表包含：

```text
minecraft:iron_ingot
minecraft:gold_ingot
minecraft:slime_ball
minecraft:quartz
minecraft:honey_bottle
minecraft:redstone
```

你也可以继续加入其他物品 ID。配置会影响摘要页、详情页和子材料导出。

### 子材料 XLSX 导出

LMLP 在 Litematica 原版“写入文件”按钮旁新增“写入子材料文件”按钮。点击后会导出一个 XLSX 文件，用当前置顶配方和停止拆分列表，把所有材料递归拆到最小子材料。

导出的工作簿包含两张表：

| 工作表 | 内容 |
| --- | --- |
| `Material Tree` | 单表树状层级结构，展示每个主材料和它向下拆出的递归子材料。 |
| `Sub-material Totals` | 汇总所有最小子材料的总数、缺少和可用数量。 |

`Material Tree` 的可见列为：

```text
Material | Type | Total | Missing | Available
```

其中 `Material` 列会使用树状前缀表示层级：

```text
黏性活塞    Main
├─ 粘液球   Sub
└─ 活塞     Sub
   ├─ 木板  Sub-sub
   ├─ 圆石  Sub-sub
   ├─ 铁锭  Sub-sub
   └─ 红石粉 Sub-sub
```

XLSX 会保留当前配置选择的数量显示样式，并包含表头样式、首行冻结、筛选、自动列宽和适合树状字符显示的字体设置。

> 图片占位符：XLSX Material Tree 工作表截图

> 图片占位符：XLSX Sub-material Totals 工作表截图

### 悬停浮窗

LMLP 提供材料行悬停浮窗模式配置：

```text
LMLP / Litematica 原版 / 不显示悬停浮窗
```

如果使用 LMLP 浮窗，悬浮材料行时会显示更适合备料查看的数量信息。喜欢原版行为时可以切回 Litematica 原版，也可以完全关闭悬停浮窗减少打扰。

### 容器界面支持打开材料列表

在工作台、高炉、切石机等容器界面中，可以使用 Litematica 原版材料列表热键打开材料列表，方便边看材料边合成。

容器界面相关行为：

- 从容器界面打开材料列表后，不会直接丢掉原加工界面。
- 从配方详情页返回时，可以回到原加工界面。
- 再次打开时会恢复上次的详情页状态。

## 默认配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `countDisplayStyle` | `样式1` | 控制材料数量显示方式。默认显示盒数、组数和个数。 |
| `enableLmlpHoverTooltip` | `LMLP` | 可选择 LMLP、Litematica 原版，或不显示材料悬停浮窗。 |
| `recipeStopItems` | 铁锭、金锭、粘液球、石英、蜂蜜瓶、红石 | 列表中的物品会被当作基础材料，不再继续递归拆分配方。 |
| `preferredRecipes` | 空 | 保存配方置顶结果。通常通过详情页星标按钮修改，不需要手动编辑。 |
| `openConfigGui` | `M + =` | 打开 LMLP 配置菜单。 |
| `showHoverDetails` | `LEFT_ALT` | 悬浮在材料行上时，按住显示详细数量信息。 |

所有热键都可以在 MaLiLib 配置菜单中重新绑定或清空。

## 使用方式

1. 在 Litematica 中打开材料列表。
2. 单击材料行，在列表内展开配方摘要。
3. 单击子材料左侧箭头，继续展开嵌套材料。
4. `Shift + 单击`材料行，打开完整配方详情页。
5. 在详情页点击星标按钮，设置或取消首选配方。
6. 如果某些材料不应该继续拆，在配置菜单的 `recipeStopItems` 中加入物品 ID。
7. 点击“写入子材料文件”，导出当前材料列表的最小子材料 XLSX。

## 安装

1. 从 [Releases](https://github.com/huanmeng06/litematica_material_list_plus/releases) 下载与你的 Minecraft 版本匹配的 jar。
2. 将 jar 放入对应 Minecraft 版本的 `mods` 文件夹。
3. 确认同一实例中已经安装 Fabric API、Litematica、MaLiLib 和 REI。
4. 启动游戏，打开 Litematica 材料列表。

## 兼容环境

| Minecraft | 最新发布 | Java | 前置模组 |
| --- | --- | --- | --- |
| `1.20.6` | `1.4.0+mc1.20.6` | `21` | Fabric API `0.100.8+1.20.6`<br>Litematica `0.18.3`<br>MaLiLib `0.19.2`<br>REI `15.0.787` |
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
