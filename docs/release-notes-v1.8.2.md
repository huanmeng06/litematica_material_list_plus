# LMLP v1.8.2

v1.8.2 改进了子材料 XLSX 汇总表，让未换算前的材料总数可以直接按数值筛选和排序。

## ⚠️ 重要版本说明

> **LMLP v1.8.2 继续使用 JEI 集成。**
>
> Minecraft `1.21.4` 与 `1.21.5` 仍不受支持，它们的最后兼容版本为 LMLP `v1.7.0`，请勿混用其他 Minecraft 版本的 JAR。

## ✨ 新功能

| 功能 | 说明 |
| --- | --- |
| XLSX 原始总数列 | `Sub-material Totals` 工作表在格式化后的 `Total` 右侧新增 `Raw Total`，显示盒、组、个换算前的材料总数。 |
| 数值筛选与排序 | `Raw Total` 使用真正的 Excel 数值单元格，而不是文本，可直接按数值大小筛选和排序。 |

原有的 `Total`、`Missing` 与 `Available` 显示方式保持不变，第一张 `Material Tree` 工作表也不受影响。

## 📦 安装

1. 下载 Assets 中与 Minecraft 版本完全对应的 LMLP JAR。
2. 删除 `mods` 文件夹中的旧版 LMLP，确保只保留一个 LMLP JAR。
3. 安装对应版本的 Fabric API、Litematica、MaLiLib 和 JEI。

---

Full Changelog: https://github.com/huanmeng06/litematica_material_list_plus/compare/v1.8.1...v1.8.2
