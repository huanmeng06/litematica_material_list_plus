# LMLP v1.8.1

v1.8.1 是 v1.8.0 的配方交互与兼容性修复版本，统一改进了 JEI Tooltip、可替代材料轮播、缺材料高亮和配方转移体验。

## ⚠️ 重要版本说明

> **LMLP v1.8.1 继续使用 JEI 集成。**
>
> Minecraft `1.21.4` 与 `1.21.5` 仍不受支持，它们的最后兼容版本为 LMLP `v1.7.0`，请勿混用其他 Minecraft 版本的 JAR。

## ✨ 新功能

| 功能 | 说明 |
| --- | --- |
| JEI 风格物品 Tooltip | LMLP 中的物品图标统一使用 JEI 风格提示，显示物品名称、模组来源及高级提示中的物品 ID；轮播材料的提示会与当前图标同步。 |
| 同步可替代材料轮播 | 嵌套配方中的木板、原木等可替代材料会以匹配的一组材质同步轮播，配方树不再出现不同木材混搭。 |
| 精确配方转移 | 普通点击 `+` 只转移完成当前缺少合成次数所需的材料；`Shift` 点击则尽可能转移全部可用材料。 |

## 🛠 修复与体验优化

| 项目 | 说明 |
| --- | --- |
| 缺材料高亮 | 修复红色缺材料高亮的绘制范围；配方面板被滚动或遮挡时，高亮会随面板裁剪，不会溢出。 |
| 配方转移提示 | 统一 `+` 按钮的 JEI Tooltip 和缺材料提示，修复各版本中 Tooltip 重叠、缺行与旧样式残留。 |
| 配方交互兼容 | 修复不同 Minecraft/JEI 版本间普通点击仅转移一份材料的问题，保持普通点击与 `Shift` 点击的行为一致。 |

## 📦 安装

1. 下载 Assets 中与 Minecraft 版本完全对应的 `litematica-material-list-plus`。
2. 删除 `mods` 文件夹中的旧版 LMLP，确保只保留一个 LMLP JAR。
3. 安装对应版本的 Fabric API、Litematica、MaLiLib 和 JEI。

---

Full Changelog: https://github.com/huanmeng06/litematica_material_list_plus/compare/v1.8.0...v1.8.1
