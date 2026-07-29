<div align="center">

<img width="128" height="128" alt="Litematica Material List Plus" src="https://github.com/user-attachments/assets/90ff5206-95dc-4032-b5dd-d6de8339c653" />

<h1>Litematica Material List Plus</h1>

<p>
  <strong>让 Litematica 的材料清单更适合备料、查配方和导出统计。</strong>
</p>

</div>

Litematica Material List Plus（简称 **LMLP**）是一个面向 Minecraft Fabric 客户端的 Litematica 扩展模组。它保留 Litematica 原有的投影材料列表，并补充数量格式化、配方递归、库存抵扣、材料偏好替换和 XLSX 导出。

如果您喜欢我的模组，不妨点击右上角 ⭐️Stars 支持一下！您的支持就是我更新的最大动力！🥰

> **当前最新正式版**：[v1.9.2](https://github.com/huanmeng06/litematica_material_list_plus/releases/tag/v1.9.2)

## 主要功能

### 更适合备料的材料列表

- 将数量显示为盒、组、个等格式，便于直接准备材料。
- 支持按总计、缺失等列排序，并保留 Litematica 的隐藏、忽略和 HUD 行为。
- 材料列表会自动跟随当前选中的投影。
- 可切换 LMLP 悬浮窗、Litematica 原生悬浮窗或关闭悬浮窗。

<p align="center">
  <img src="https://raw.githubusercontent.com/huanmeng06/litematica_material_list_plus/main/docs/assets/readme/material-list.png" alt="LMLP 材料列表主界面" width="100%">
</p>

### 配方摘要与详情

- 单击材料行，展开当前首选配方和递归子材料。
- `Shift + 单击` 打开完整配方详情页，使用 JEI 的原生配方布局。
- 同一物品存在多个配方时，可以设置首选配方；该选择会同步影响摘要、递归拆分和导出结果。
- 支持工作台、熔炉、高炉、切石机等 JEI 配方类型。

<p align="center">
  <img src="https://raw.githubusercontent.com/huanmeng06/litematica_material_list_plus/main/docs/assets/readme/recipe-details.png" alt="LMLP 配方详情页" width="100%">
</p>

### 最小子材料与库存抵扣

最小子材料视图会把成品递归换算为真正需要准备的基础材料，并识别背包中已经拥有的内容：

- 背包已有的基础材料。
- 背包已有、但还需要继续加工的半成品，例如玻璃抵扣染色玻璃所需的沙子。
- 已经拥有的成品。
- 同一组候选材料之间的自动分配。

当材料存在候选歧义时，缺失数量后会显示详情标记。悬浮后可以查看抵扣来源；在配置中可以选择完整明细或简洁汇总。

<p align="center">
  <img src="https://raw.githubusercontent.com/huanmeng06/litematica_material_list_plus/main/docs/assets/readme/allocation-tooltip.png" alt="LMLP 最小子材料抵扣明细" width="100%">
</p>

### 材料偏好替换

在材料列表中点击 **替换为偏好**，可以把原理图中的材料替换成生存时更常用的一组材料。配置表单使用受限的 JEI 选择器，只展示当前材料类别允许的目标物品。

目前支持：

- 木种、石材、玻璃
- 羊毛与地毯
- 陶瓦与带釉陶瓦
- 混凝土与混凝土粉末
- 床、蜡烛、潜影盒

替换结果会另存为 `_preferred` 原理图，不覆盖原文件。应用偏好原理图时，会保留原投影的位置、朝向、子区域状态和实体设置，并关闭旧投影而不删除它。

<p align="center">
  <img src="https://raw.githubusercontent.com/huanmeng06/litematica_material_list_plus/main/docs/assets/readme/preferred-materials.png" alt="LMLP 偏好材料配置与替换明细" width="100%">
</p>

### XLSX 导出

从材料列表导出子材料表格，可用于整理采购或生存备料。导出内容包括：

- 材料名称与图标
- 未换算前的总数
- 最小子材料需求
- 缺失数量
- 候选材料与来源信息

<table>
  <tr>
    <td width="50%" align="center"><img src="https://raw.githubusercontent.com/huanmeng06/litematica_material_list_plus/main/docs/assets/readme/xlsx-material-tree.png" alt="XLSX Material Tree 工作表"></td>
    <td width="50%" align="center"><img src="https://raw.githubusercontent.com/huanmeng06/litematica_material_list_plus/main/docs/assets/readme/xlsx-sub-material-totals.png" alt="XLSX Sub-material Totals 工作表"></td>
  </tr>
  <tr>
    <td align="center">材料树</td>
    <td align="center">最小子材料合计</td>
  </tr>
</table>

## 安装

从 [Releases](https://github.com/huanmeng06/litematica_material_list_plus/releases) 下载与 Minecraft 版本完全对应的 JAR。不要混用不同 Minecraft 版本的文件。

同时安装对应版本的：

- Fabric Loader 和 Fabric API
- Litematica
- MaLiLib
- JEI

安装步骤：

1. 关闭 Minecraft。
2. 将 LMLP JAR 放入对应实例的 `mods` 文件夹。
3. 确认 `mods` 中只保留一个 LMLP JAR。
4. 启动游戏并打开 Litematica 材料列表。

## 支持版本

| Minecraft | 分支 | Java | 最新 LMLP 版本 | 状态 |
| --- | --- | --- | --- | --- |
| 1.20.1 | `mc1.20.1` | 17 | v1.9.2 | ✅ |
| 1.20.4 | `mc1.20.4` | 17 | v1.9.2 | ✅ |
| 1.20.6 | `mc1.20.6` | 21 | v1.9.2 | ✅ |
| 1.21.1 | `mc1.21.1` | 21 | v1.9.2 | ✅ |
| 1.21.4 | `mc1.21.1` | 21 | v1.7.0 | ⚠️ |
| 1.21.5 | `mc1.21.1` | 21 | v1.7.0 | ⚠️ |
| 1.21.10 | `mc1.21.10` | 21 | v1.9.2 | ✅ |
| 1.21.11 | `mc1.21.11` | 21 | v1.9.2 | ✅ |
| 26.1.2 | `mc26.1.2` | 25 | v1.9.2 | ✅ |

由于 JEI 适配问题， Minecraft `1.21.4` 和 `1.21.5` 暂不支持。它们最后兼容的 LMLP 版本是 `v1.7.0`，使用 REI 作为前置。

## 使用入口

- 使用 Litematica 的材料列表热键打开材料列表，通常为 `M + L`。
- 在材料行上单击查看摘要。
- 点击物品名称查看完整配方详情。

## 许可

Litematica Material List Plus 使用 [LGPL-3.0-or-later](LICENSE) 发布。
