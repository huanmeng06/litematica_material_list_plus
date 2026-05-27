# Litematica Material List Plus 1.0.0

首个正式发布版本，面向 Minecraft 1.20.1 / 1.20.4 的 Litematica 材料列表增强模组。

---

### 可下载文件

请根据你的 Minecraft 版本下载对应 jar：

- Minecraft 1.20.1：`litematica-material-list-plus-1.0.0.jar`
- Minecraft 1.20.4：`litematica-material-list-plus-1.0.0+mc1.20.4.jar`

不要混用不同 Minecraft 版本的 jar。

### 主要功能

- 优化 Litematica 材料列表的数量显示，支持“盒 / 组 / 个”格式。
- 单击材料行可展开配方摘要。
- Shift + 单击材料行可打开完整配方详情页。
- 配方详情页复用 REI 原生配方显示，支持物品 tooltip、高亮和交互。
- 支持熔炉、切石机等非工作台配方，并显示对应加工台图标。
- 保留 Litematica 原有材料统计、排序和导出行为。

### 安装要求

#### Minecraft 1.20.1

- Minecraft 1.20.1
- Fabric Loader >= 0.16.9
- Java 17
- Fabric API 0.92.9+1.20.1
- Litematica 0.15.4
- MaLiLib 0.16.3
- Roughly Enough Items / REI 12.1.785

#### Minecraft 1.20.4

- Minecraft 1.20.4
- Fabric Loader 0.19.2
- Java 17
- Fabric API 0.97.3+1.20.4
- Litematica 0.17.4
- MaLiLib 0.18.4-alpha.1
- Roughly Enough Items / REI 14.1.786

REI 是必要前置，请确保已安装。Architectury API、Cloth Config API 和 Cloth Basic Math 等为 REI 相关前置，请按实际 REI 安装要求一并安装。

### 安装方式

将与你的 Minecraft 版本对应的 jar 放入 Minecraft 实例的 `mods` 文件夹，然后启动游戏即可。

---

### 已知问题

- 目前仅针对 Minecraft 1.20.1 / 1.20.4，以及上方列出的 Litematica、MaLiLib、REI 与 Fabric API 版本组合进行适配与测试。
- 其他 Minecraft / Litematica / REI 版本暂未测试，可能存在兼容性问题。
