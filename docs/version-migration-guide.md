# LMLP 多版本移植总指南

最后更新：2026-07-29
当前开发基线：`dev-newFeature` / Minecraft `1.21.11`
适用范围：Minecraft `1.20.1`～`26.1.2`，以及后续新增版本

本文是 Litematica Material List Plus（LMLP）唯一的版本移植参考。以后新增版本、回移功能或记录跨版本踩坑时，只更新本文，不再为单个功能另建移植文档。

本文合并并取代了：

- `minecraft-1.20.1-to-1.21.5-compatibility.md`
- `placement-origin-marker-porting-notes.md`
- `jei-migration-feasibility.md`
- `jei-migration-implementation.md`
- `main` 分支原有的 `docs/version-migration-guide.md`

## 1. 信息源与基本原则

移植时按以下优先级判断，避免文档和实际构建配置互相矛盾：

1. `main/versions.toml`：受支持版本、分支、Java、依赖和实例路径的唯一数据源。
2. `main/tools/lmlp.py`：构建、校验、部署、回归检查和 Release 的实际行为。
3. 目标版本分支的 `gradle.properties`、`fabric.mod.json` 和目标依赖 JAR：编译与运行时事实。
4. 本文：已验证的兼容差异、实现约束和人工验收清单。

核心原则：

- 每个 Minecraft 版本使用独立 `mc...` 分支和 worktree，不在一个源码目录反复切换版本。
- 功能语义从已确认的开发提交迁移；API 写法以目标分支和目标依赖为准。
- 不能只修到“能够编译”。Mixin、JEI、GUI、材料计算和世界渲染必须分别实机验证。
- 不能因为相邻版本构建成功，就直接发布目标版本 JAR。
- 正式 Release 不覆盖既有 Tag 或附件；修复使用新版本号。
- 旧文档或旧分支与当前实现冲突时，以目标分支源码、`versions.toml` 和实机结果为准。

## 2. 当前版本矩阵

下表是 `main/versions.toml` 在 2026-07-29 的快照。发生冲突时以 `versions.toml` 为准。

| Minecraft | 分支 | 构建 JDK | 成品字节码 | Fabric API | Litematica | MaLiLib | JEI |
| --- | --- | ---: | ---: | --- | --- | --- | --- |
| `1.20.1` | `mc1.20.1` | 21 | Java 17 / major 61 | `0.92.9+1.20.1` | `0.15.4` | `0.16.3` | `15.20.0.134` |
| `1.20.4` | `mc1.20.4` | 21 | Java 17 / major 61 | `0.97.3+1.20.4` | `0.17.4` | `0.18.4-alpha.1` | `17.3.1.5` |
| `1.20.6` | `mc1.20.6` | 21 | Java 21 / major 65 | `0.100.8+1.20.6` | `0.18.3` | `0.19.2` | `18.0.0.66` |
| `1.21.1` | `mc1.21.1` | 21 | Java 21 / major 65 | `0.116.12+1.21.1` | `0.19.60` | `0.21.10` | `19.38.0.366` |
| `1.21.10` | `mc1.21.10` | 21 | Java 21 / major 65 | `0.138.4+1.21.10` | `0.24.9` | `0.26.8` | `26.3.0.31` |
| `1.21.11` | `mc1.21.11` | 21 | Java 21 / major 65 | `0.141.4+1.21.11` | `0.26.12` | `0.27.16` | `27.17.0.50` |
| `26.1.2` | `mc26.1.2` | 26 | Java 25 / major 69 | `0.154.2+26.1.2` | `0.27.10` | `0.28.9` | `29.16.0.47` |

停止参与当前发布的版本：

| Minecraft | 分支 | 最后版本 | 原因 |
| --- | --- | --- | --- |
| `1.21.4` | `mc1.21.4` | `1.7.0` | v1.8.0 改用 JEI 后，没有对应的受支持 JEI 目标 |
| `1.21.5` | `mc1.21.5` | `1.7.0` | v1.8.0 改用 JEI 后，没有对应的受支持 JEI 目标 |

注意：1.20.1 和 1.20.4 现在使用 Java 21 运行 Loom/Gradle，但 `java_release` 仍必须是 17，最终 class major 必须是 61。

## 3. 固定移植流程

所有管理命令都在 `main` worktree 执行。

### 3.1 确认迁移源

1. 确认要迁移的功能对应哪个提交、Tag 或正式版本。
2. 源工作区必须干净，不能把未提交实验代码带入版本分支。
3. 先列出功能涉及的源码、资源、Mixin、配置和语言键，不要直接整分支复制。
4. 涉及缓存格式、配置迁移或序列化时，先写清楚向后兼容规则。

### 3.2 登记目标版本

在 `main/versions.toml` 中登记：

- Minecraft 与版本分支；
- 构建 JDK 和成品字节码；
- Fabric Loader / Fabric API；
- Litematica / MaLiLib / JEI / Mod Menu；
- 本地测试实例；
- 是否受支持、是否参与 Release。

新增版本后先运行：

```bash
./tools/lmlp list
./tools/lmlp status
./tools/lmlp prepare <minecraft-version>
```

### 3.3 建立版本分支

1. 创建 `mc<version>` 分支和独立 worktree。
2. 先让 Gradle Wrapper、仓库和依赖解析可复现。
3. 先同步构建元数据，再迁移业务源码。
4. 不要用手工复制 JAR 代替可复现 Gradle 构建。

### 3.4 按风险顺序迁移

推荐顺序：

1. `gradle.properties`、`build.gradle`、`fabric.mod.json` 和资源处理。
2. 基础类型、注册表、Identifier、ItemStack、路径 API。
3. GUI、DrawContext、滚轮、scissor、Tooltip。
4. JEI resolver、native display、transfer 和选择器。
5. Litematica / MaLiLib Mixin 目标与描述符。
6. 缓存、材料计算和异步任务。
7. 原点光束、RenderPipeline、FrameGraph 和 HUD。
8. 三份语言文件、贴图和第三方许可。

### 3.5 构建、部署和回归

```bash
./tools/lmlp build <minecraft-version>
./tools/lmlp regression-check <minecraft-version>
./tools/lmlp deploy <minecraft-version>
./tools/lmlp verify <minecraft-version>
```

管理工具会校验：

- Minecraft 依赖；
- `fabric.mod.json`、Mixin 和资源是否入包；
- 构建版本、Git Commit、dirty 状态和构建时间；
- Java class major；
- 构建 JAR 与部署 JAR 的 SHA-256。

实机验收完成后才能提交、推送或准备 Release。

## 4. 通用兼容差异

### 4.1 Java、映射和命名空间

| 范围 | 构建与命名规则 |
| --- | --- |
| `1.20.1` / `1.20.4` | Loom 使用 Java 21，成品使用 `--release 17` |
| `1.20.6`～`1.21.11` | Java 21，成品 class major 65 |
| `26.1.2` | Java 26 构建、Java 25 字节码；使用 Mojang 官方名称 |

1.20.1～1.21.11 源码主要使用 Fabric intermediary 名称（`class_`、`method_`、`field_`）。26.1+ 不再沿用这套名称，不能通过机械替换类名完成移植。

Mixin 字符串目标不受 Java 编译器完整检查。每次移植都要从目标 Minecraft、Litematica 和 MaLiLib JAR 使用 `javap -p -s` 核对方法描述符。

目标 JAR 优先扫描这些 intermediary 接口：

| 类 | 常见名称 | 当前用途 | 风险 |
| --- | --- | --- | --- |
| `class_1799` | ItemStack | 材料、配方、Tooltip、背包计数 | 低，但高版本仍需编译确认 |
| `class_1923` | ChunkPos | 区块加载判断 | 低 |
| `class_2338` | BlockPos | 投影原点、缓存统计坐标 | 低 |
| `class_2382` | Vec3i | 原理图区域尺寸 | 低 |
| `class_2561` | Text | GUI 和 Tooltip 文本 | 中，部分 TextRenderer 签名变化 |
| `class_2680` | BlockState | 原理图与缓存方块统计 | 低 |
| `class_2960` | Identifier | 贴图、注册表和 JEI 插件 ID | 高 |
| `class_310` | MinecraftClient | 客户端、玩家、世界和窗口 | 中 |
| `class_327` | TextRenderer | GUI、Tooltip 和世界标签 | 中 |
| `class_332` | DrawContext | GUI、矩阵、scissor 和 Tooltip | 高 |
| `class_437` | Screen | 材料列表和自定义页面 | 中 |
| `class_465` | HandledScreen | 容器界面热键 | 高，Mixin 描述符必须确认 |
| `class_638` | ClientWorld | 区块与维度判断 | 中 |
| `class_7923` | Registries | 物品/方块注册表 | 中到高 |

旧版本环境可使用 `tools/scan-mc-compat.ps1` 扫描 class major、Identifier、DrawContext、ItemStack、世界维度和 MaLiLib 滚轮签名。该脚本的版本范围和依赖列是历史辅助信息，不能代替 `main/versions.toml`。

### 4.2 Identifier

| Minecraft | 规则 |
| --- | --- |
| `1.20.x` | `new class_2960(namespace, path)` 仍可用 |
| `1.21.1+` | 构造器为 private，使用 `method_60655(namespace, path)` / `method_60654(id)` 等工厂 |
| `26.1+` | 按官方名称和目标版本工厂重新适配 |

不要在共享逻辑中散落 Identifier 构造方式。新增贴图、物品 ID、JEI 插件 ID 时，应让版本差异集中在 helper 或版本分支内。

### 4.3 DrawContext 与 GUI 绘制

已验证的稳定方法包括物品绘制、字符串绘制、scissor 和矩阵栈；高风险点是贴图方法：

- 1.20.x / 1.21.1：`method_25290(texture, ...)` 使用旧式参数。
- 1.21.4+：贴图方法开始要求 render layer / pipeline 参数。
- 1.21.10 / 1.21.11：当前源码会传入对应 GUI RenderPipeline，例如 `class_10799.field_56883`。
- 26.1+：进入新的 GUI 提取和官方命名路径，必须查目标 API，不能只改方法名。

Tooltip 必须在已有物品批次 flush 后绘制。背景、边框、图标底板、物品和文字应处于同一矩阵层级，否则 JEI 槽位或按钮会穿透背景。

### 4.4 滚轮与输入

| Minecraft / MaLiLib | `WidgetListBase.onMouseScrolled` |
| --- | --- |
| `1.20.1 / MaLiLib 0.16.3` | `(int mouseX, int mouseY, double amount)` |
| `1.20.6+` | `(int mouseX, int mouseY, double horizontalAmount, double verticalAmount)` |

回移到 1.20.1 时，要同时检查：

- `WidgetListBaseMixin`；
- `Screen` 的滚轮入口；
- 配方详情页；
- JEI native display 的输入转发。

### 4.5 Litematica 与 MaLiLib

重点检查：

- `SchematicPlacement.getSchematicFile()`；
- `DataManager.getSchematicsBaseDirectory()`；
- `WidgetListBase` 的滚轮和列表重建；
- `GuiMaterialList`、`WidgetMaterialListEntry` 的构造器和注入点；
- `TaskCountBlocksPlacement` 构造器；
- `HandledScreen` 的按键与关闭入口。

Litematica 0.22.x 起，部分路径 API 从 `File` 转为 `Path`。核心逻辑应尽量在边界立即统一为 `Path`，不要让两种类型扩散。

`SchematicPlacement` 没有稳定的维度字段。实时/缓存判断仍应以“当前 placement manager 是否包含该 placement”并结合已记录维度为准，不能只看相同 chunk 坐标。

旧 intermediary 版本中，`ClientWorld.method_8393(chunkX, chunkZ)` 用于区块加载判断，`World.method_27983()` 用于取得维度 key。移植到官方命名版本时要保留这两项语义，而不是保留方法名。

### 4.6 MaLiLib 配置翻译键

MaLiLib 的配置名称翻译机制在旧版本和新版本之间不同。配置能够编译、语言 JSON 中存在对应键，并不代表配置页面会实际读取该键。

| Minecraft | MaLiLib | 配置名称解析方式 |
| --- | --- | --- |
| `1.20.1` | `0.16.3` | `IConfigBase.getConfigGuiDisplayName()` 固定查找 `config.name.<内部名称小写>` |
| `1.20.4` | `0.18.4-alpha.1` | 同上 |
| `1.20.6` | `0.19.2` | 同上 |
| `1.21.1+` | `0.21.10+` | 支持独立的 translated name；当前配置使用五参数构造器传入翻译键 |

旧版 `ConfigBoolean` 和 `ConfigOptionList` 的四参数构造器虽然可以接收 pretty name，但 `getConfigGuiDisplayName()` 仍会优先按内部配置名拼接旧式键。例如内部名称 `preferredWoodEnabled` 实际查找：

```text
config.name.preferredwoodenabled
config.comment.preferredwoodenabled
```

因此，把 `lmlp.config.name.preferred_wood_enabled` 作为四参数构造器的 pretty name 传入，不能保证旧版配置页面显示该翻译。v1.9.1 首次移植偏好表单时，`1.20.1`、`1.20.4` 和 `1.20.6` 均受此差异影响：木材、石材、玻璃等新增偏好标题会直接显示 `preferredWoodEnabled` 一类内部名称，悬浮说明也可能显示原始翻译键。`1.21.1`、`1.21.10`、`1.21.11` 和 `26.1.2` 不受影响。

向三个旧版本移植新配置项时，必须选择一种兼容方式：

1. 在简体中文、繁体中文和英文中同步补充 `config.name.<内部名称小写>` 与 `config.comment.<内部名称小写>` 旧式别名；或
2. 使用仅限旧分支的兼容配置类，明确覆盖 `getConfigGuiDisplayName()` 和 `getComment()`，再调用 LMLP 自己的翻译键。

不要通过修改内部配置名称来迁就显示文本，否则会改变 JSON 中的持久化键并破坏已有配置。也不要只检查 `lmlp.config.name.*` 是否存在；必须按目标 MaLiLib 的真实查找路径验收。

确认目标 API 时可直接检查依赖 JAR：

```bash
javap -classpath <malilib.jar> -c -p fi.dy.masa.malilib.config.IConfigBase
javap -classpath <malilib.jar> -p fi.dy.masa.malilib.config.options.ConfigBoolean
javap -classpath <malilib.jar> -p fi.dy.masa.malilib.config.options.ConfigOptionList
```

### 4.7 资源、语言和打包

- `fabric.mod.json`、Mixin JSON 和三份语言 JSON 必须解析成功。
- JAR 根目录必须包含 `fabric.mod.json`、Mixin 配置和 `assets/`。
- 新增语言键要同步简体中文、繁体中文和英文。
- 新增 MaLiLib 配置项时，要同时检查目标版本实际使用的是旧式 `config.name.*`，还是新版 translated name。
- 实例中只允许一个启用的 LMLP JAR。
- 从 REI 保留的按钮素材必须继续保留在 `THIRD_PARTY_NOTICES.md` 中；实际文件名变更时许可说明也要同步。

## 5. 各版本特别注意事项

### 5.1 Minecraft 1.20.1

- Loom 使用 Java 21，但成品必须是 Java 17 / class major 61。
- Identifier 构造器可直接使用。
- DrawContext 使用旧贴图签名，不存在高版本 render pipeline 参数。
- MaLiLib 和 Screen 滚轮是单轴参数。
- MaLiLib 0.16.3 的配置标题和说明使用旧式 `config.name.<内部名称小写>` / `config.comment.<内部名称小写>` 查找。
- 若绕过 Gradle 手工编译，class path 需要包含 Fabric API 处理后的子模块；只放聚合 JAR 可能缺类。
- Windows `javac @argfile` 必须使用无 BOM UTF-8、正斜杠路径，并正确引用含空格路径。
- 手工打包必须同时包含 classes 和 resources，否则 Fabric Loader 不会识别模组。
- 构建后必须用 `javap -verbose` 确认 major 61。

### 5.2 Minecraft 1.20.4

- Loom 使用 Java 21，成品仍是 Java 17 / major 61。
- Identifier 构造器仍可用。
- MaLiLib 0.18.4-alpha.1 仍使用旧式配置名称和说明翻译键。
- 不要带入 1.21.4+ 的 DrawContext render layer 参数。
- 原点渲染沿用旧世界渲染事件与 no-depth overlay 路径。

### 5.3 Minecraft 1.20.6

- Java 21 / major 65。
- Identifier 构造器仍可用。
- MaLiLib 滚轮已是横向/纵向双轴。
- MaLiLib 0.19.2 仍使用旧式配置名称和说明翻译键；不要误按 1.21.1 的五参数配置 API 移植。
- JEI 18 是最早完成 REI → JEI 迁移的基准实现，但不能把 JEI 18 方法签名直接复制到高版本。

### 5.4 Minecraft 1.21.1

- Java 21 / major 65。
- Identifier 改用静态工厂。
- DrawContext 仍接近旧式贴图签名。
- Litematica 0.19.60 已从 Modrinth 下架；构建可使用 API 兼容的 0.19.61，运行时最低依赖仍按 0.19.60。
- 原点标记使用最终世界渲染阶段和明确的 no-depth layer。

### 5.5 Minecraft 1.21.4 / 1.21.5

这两个分支目前停止参与 v1.8.0+ 发布。不要把它们加入构建矩阵，除非先解决目标 JEI 支持并重新完成整套验收。

历史实现注意点：

- Identifier 使用静态工厂。
- 1.21.4 的贴图方法要求 render layer factory。
- 1.21.4 的部分 `TextRenderer.method_27522(...)` 路径要求 `Text`，不能继续直接传 String。
- 1.21.5 的 GPU pipeline 已发生明显变化。
- 1.21.5 曾使用 world-space beam/icon + HUD label fallback 解决蓝图覆盖标签的问题。

如果以后恢复支持，应先检查现有版本分支，而不是从当前 1.21.11 反向猜测 API。

### 5.6 Minecraft 1.21.10 / 1.21.11

- Java 21 / major 65。
- 世界渲染使用 FrameGraph。
- 原点标记必须在完整 FrameGraph 执行后绘制；当前 1.21.11 通过 `WorldRendererLateOriginMarkerMixin` 注入。
- 使用 `RenderPipeline`、no-depth RenderLayer 和目标版本 Buffer API。
- 当前功能开发基线是 1.21.11，向其他版本迁移时保留行为，不保留 intermediary 写法。

### 5.7 Minecraft 26.1.2

- Java 26 构建，成品目标 Java 25 / major 69。
- 使用 Mojang 官方名称，GUI、渲染和 Mixin 需要按目标名称重新适配。
- GUI 使用新的提取阶段；旧 `render` / `GuiGraphics` 入口不能只靠改名猜测。
- RenderPipeline、DepthStencilState 和渲染回调描述符与 1.21.11 不同。
- 依赖由管理工具下载到 `.lmlp/dependencies`，再交给 Gradle 使用，避免 CDN/TLS 不稳定。

## 6. JEI 集成移植

LMLP 从 v1.8.0 起以 JEI 为必要前置，REI 迁移已经完成。当前移植目标是保持 JEI 集成边界，而不是再次重写 `RecipeDetailScreen`。

### 6.1 固定架构

| 模块 | 职责 |
| --- | --- |
| `JeiRuntimeBridge` | 保存/释放 `IJeiRuntime`，管理 JEI 生命周期 |
| `JeiRecipeResolver` | 查询配方并转换为 LMLP 自有摘要模型 |
| `JeiNativeDisplayBridge` | 原生布局、尺寸、槽位、Tooltip、配方/用途跳转和 Catalyst |
| `JeiRecipeTransferBridge` | 检查与执行配方转移 |
| `JeiItemTooltipRenderer` | JEI 物品 Tooltip 边界 |
| `RecipeNativeDisplayBridge` / `RecipeTransferBridge` | 核心 GUI 与第三方 API 的隔离层 |

核心 `RecipeDetailScreen`、`RecipeSummary`、材料树和数量计算不得直接依赖 JEI 类型。JEI 类型应限制在 `recipe/jei`，主界面只通过 bridge 或反射工厂接入。

LMLP 自带的 3×3 后备布局必须使用自己的槽位和箭头绘制，不能重新依赖 REI 资源路径。

### 6.2 配方解析不变量

```text
crafts = ceil(required / outputCount)
ingredientTotal = ingredientCountPerCraft * crafts
```

- 同一输入出现在多个槽位时，先累加单次配方用量，再乘合成次数。
- 候选物品数量、槽位数量和合成次数是三个不同概念。
- 每个分类、每条配方分别隔离异常；一个第三方坏 layout 不能清空此前已解析配方。
- JEI runtime 尚未就绪时返回空结果，但不能永久缓存“尚未就绪”的空 resolver。
- runtime 上线、失效或重载时，要清查询缓存、配方树缓存和最小子材料缓存。

### 6.3 原生布局与 Tooltip

- 每次绘制、点击或显示 Tooltip 前都重新设置 `IRecipeLayoutDrawable` 的实际位置。
- 同一布局对象可能在多个递归节点复用，不能把最后一次坐标当成全局坐标。
- 内容绘制在 scissor 内，overlay 和 Tooltip 在正确时机离开 scissor 后绘制。
- 左键槽位用 OUTPUT focus 打开配方，右键用 INPUT focus 打开用途。
- Catalyst 点击普通物品时优先聚焦该物品；无法建立物品 focus 时再打开类别。
- Tooltip 前先 flush，背景必须使用读取当前矩阵的绘制路径，禁止用“隐藏底层 JEI 面板”掩盖穿透。

材料摘要到详情页的导航也要保持：

- 点击摘要标题打开主材料详情；
- 点击一级子材料名称进入同一详情页并定位、展开对应节点；
- 小三角只控制当前摘要展开，不与名称共用命中区；
- 目标位置按配方 ID、物品 ID 和实际内容坐标计算，不用固定像素猜测。

Catalyst 标签必须显示配方所需工作站，悬停有物品 Tooltip 与高亮，点击普通工作站物品时打开该工作站自身的配方。视觉参考保留在：

- [`assets/jei/catalyst_tab_original.png`](assets/jei/catalyst_tab_original.png)
- [`assets/jei/catalyst_tab_refined.png`](assets/jei/catalyst_tab_refined.png)

“任意木板”“任意原木”等候选面板保持以下规则：

- 普通状态使用黄色常规字体；仅可点击链接悬停时加粗、加下划线。
- `hoverPanelMaxRows` 默认 10，范围 1～40，标题不计入行数。
- 每列先填满最大行数，再换列；空间不足时减少列数并重新平衡，不能越过屏幕。
- 材料列表和完整配方详情页共用同一布局规则。

### 6.4 配方转移

- 只使用目标 JEI 的公共 transfer API。
- 普通点击转移一次，Shift 点击最大批量转移。
- 分别处理可转移、材料不足、容器不支持。
- 不实例化 JEI 内部按钮；LMLP 保留自己的按钮与视觉。
- 背包 2×2 使用玩家 inventory handler 作为后备，3×3 配方不能错误转入 2×2。
- 保留转移按钮普通、悬停和禁用三种状态；悬停使用黄色 `+`，禁用状态要有明确 Tooltip。
- 若继续使用 REI 来源的按钮纹理，文件和 MIT 许可必须与 `THIRD_PARTY_NOTICES.md` 同步。

### 6.5 每个版本必须重新确认的 JEI API

- `IRecipeLayoutDrawable`
- `IRecipeTransferManager` / transfer handler
- `IRecipesGui`
- `IIngredientManager`
- `IRecipeSlotsView` / slot drawable
- focus factory 与 INPUT / OUTPUT role
- Catalyst 和 recipe category API

必须实测原生布局、Tooltip、点击、转移、选择器和 runtime 重载，不能只依赖编译。

## 7. 材料计算与缓存不变量

这些规则属于功能语义，移植时不能因 API 修复而改变：

- 循环检查发生在采用配方和输出取整之前。
- 深层循环只沿单输入转换继续检查；多输入配方可能包含工具槽，沿完整链会制造假循环。
- 例如需求 29 骨粉时，不能因“骨粉 → 骨块 → 骨粉”被放大成 36。
- 原木、木材、菌柄、菌核和竹块是木材链终点，不继续进入剥皮配方和工具槽。
- 煤炭块、白色床、选择组和多产出配方不得被错误拆分或错误缩放。
- 任意木板、任意原木等选择组要保留候选并集，不能塌成代表项。
- 最小子材料按每帧预算分段计算，并允许后台预热。
- 后台任务只能发布签名仍匹配的完整结果，不能发布旧任务的部分结果。
- 缓存签名要覆盖物品、数量、JEI runtime、配方、配置和库存快照。
- 优化或移植前后要逐项比较物品和数量，不能只比较总行数。

管理工具的静态回归保护目前覆盖：

- 单输入循环保护；
- 原木类终止拆分；
- JEI 单配方异常隔离；
- 最小子材料计算预算。

静态标记存在不代表行为正确，仍需游戏内验收。

## 8. GUI、HUD 与列表约束

- 热键监听器：`initGui()` 先清理旧监听器，再由基类创建新列表和新监听器。
- 热键绑定后只刷新 LMLP 自己的按钮，保留 MaLiLib 原生 `IKeybind.isModified()`。
- 不要为刷新单个按钮重建整个配置列表。
- 展开列表内容必须成对启用和关闭 scissor。
- 初始排序即使没有排序箭头，也必须保留表头框和点击区域。
- Tooltip 提交顺序保持背景、图标、文字。
- HUD 开关必须注册或移除当前 `MaterialListHudRenderer`，不能只改布尔值。
- 列表切换、缓存刷新或 MaterialList 替换后，InfoHud 必须指向新渲染器。
- 禁用图标使用正常物品模型加中性灰遮罩，不恢复同步 framebuffer 读回。
- 高版本 Litematica 自带导出按钮与 LMLP 重叠时，只移除明确冲突的按钮。
- 搜索框、滚轮、ESC、列表高度动画和最后一行自动滚动都要在目标 MaLiLib 上重新测试。

## 9. 原点光束与标签

### 9.1 行为契约

各版本必须保持：

1. 点击当前维度的原点后显示红色光束和靶心。
2. 其他维度原点不可直接点击。
3. 同时只保留一个 active marker。
4. 距目标 2 格内自动清除（`ARRIVAL_DISTANCE_SQUARED = 4.0D`）。
5. 标签包含投影名称、`[x, y, z]` 和距离。
6. 只在注视目标时显示标签。
7. 光束、靶心和标签固定在世界坐标。
8. 显示层级为世界/蓝图/透明内容 < 光束 < 靶心 < 标签背景 < 标签文字。
9. 不出现白色闪面、灰白闪面或大块透明脏面。
10. Overworld/Nether 跨维度只显示坐标换算后的光束，不显示同维度靶心和标签。

### 9.2 不要顺手修改的业务链路

- 原点解析与 `originPosition`
- `resolveRenderTarget`
- Overworld/Nether 8 倍换算
- `KnownPlacementContext`
- `Marker` / `activeMarker`
- 点击、hover 和字体状态
- 到达目标后的 `clear()`
- 缓存与维度判断

位置异常时，先确认实例运行的 JAR 和 class 已更新，再查渲染阶段和矩阵。

### 9.3 渲染基线

- 使用目标版本原版 beacon beam 纹理、几何和 UV，但由 LMLP 自己的 no-depth pipeline 绘制。
- 不直接调用 vanilla beacon renderer，也不恢复旧自绘实心方柱。
- depth test 关闭、depth write 关闭；需要透明混合的层明确配置 blend。
- 绘制顺序为 beam、target icon、label background、label text。
- 1.20.1～1.21.1 使用目标版本最后的世界渲染阶段。
- 1.21.10 / 1.21.11 在完整 FrameGraph 执行后绘制。
- 26.1.2 重新适配目标 RenderPipeline、DepthStencilState 和回调描述符。

禁止把以下诊断方案留在最终版本：

- 普通 translucent/debug quads 作为光束最终层；
- 固定 HUD 画线代替 world-space beam；
- 临时 `LMLP HUD TEST`、辅助线和低频诊断日志；
- 通过隐藏蓝图或 JEI 内容掩盖渲染层级问题。

### 9.4 故障判断

| 表现 | 优先检查 |
| --- | --- |
| 光束像贴在屏幕上 | JAR 是否更新；world → camera 变换是否正确 |
| Fabric 不识别 JAR | 根目录是否有 `fabric.mod.json`、Mixin 和资源 |
| 白色/灰白闪面 | 是否误用 debug/translucent buffer |
| 标签被蓝图覆盖 | 绘制阶段、no-depth、FrameGraph 注入位置和提交顺序 |
| 标签完全不显示 | active marker、维度、注视判断、渲染回调是否执行 |
| 标签位置错误 | camera、position/projection matrix 和实际运行 class |

## 10. 高风险文件

| 文件/区域 | 迁移关注点 |
| --- | --- |
| `build.gradle` / `gradle.properties` | Java release、依赖、映射、资源展开 |
| `fabric.mod.json` | Minecraft、Litematica、MaLiLib、JEI 依赖与入口 |
| `config/Configs.java` 与三份语言 JSON | MaLiLib 新旧配置翻译键、持久化内部名称 |
| `gui/ToggleArrowRenderer.java` | Identifier、贴图和 GUI pipeline |
| `gui/RecipeDetailScreen.java` | DrawContext、scissor、Tooltip、bridge 工厂和输入 |
| `recipe/jei/*` | 目标 JEI API、runtime、layout、transfer |
| `mixin/WidgetListBaseMixin.java` | MaLiLib 滚轮描述符 |
| `mixin/HandledScreenMixin.java` | 容器热键和关闭注入 |
| `cache/ChunkMissingMaterialListCache.java` | 区块、维度、placement manager 和异步缓存 |
| `gui/MinimalSubMaterialListView.java` | 计算预算、选择组、循环和原木终止 |
| `gui/PlacementOriginMarker.java` | 世界坐标、光束、靶心和标签 |
| `gui/OriginMarkerRenderLayers.java` | RenderPipeline、depth、blend 和纹理 |
| `mixin/WorldRendererLateOriginMarkerMixin.java` | FrameGraph 和目标描述符 |
| `material/ItemStackTexts.java` | Registry / Identifier |

## 11. 构建产物核对

每个目标版本至少检查：

1. `git status -sb` 干净。
2. `git diff --check` 通过。
3. Gradle 完整构建通过。
4. JAR 内有：
   - `fabric.mod.json`
   - `litematica_material_list_plus.mixins.json`
   - `assets/`
   - `lmlp-build.properties`
5. `fabric.mod.json` 的 Minecraft 与依赖版本正确。
6. class major 与目标一致。
7. JAR 内 Git Commit 等于版本分支当前提交。
8. `gitDirty=false`。
9. 实例中只有一个启用的 LMLP JAR。
10. 构建 JAR 与部署 JAR 的 SHA-256 相同。
11. JAR 内没有临时日志、诊断渲染或 stale class。

不要用 `build/libs` 中的文件名推断实例实际加载内容；必须检查 `mods` 中的最终 JAR 和启动日志里的 `[LMLP build]`。

## 12. 游戏内最低验收

### 启动与基础界面

- [ ] 目标 Minecraft、Fabric、Litematica、MaLiLib 和 JEI 能正常启动。
- [ ] 日志显示正确的 LMLP 版本、Minecraft、Git Commit 和 clean build。
- [ ] 材料列表、配置、偏好表单和物品选择器可打开。
- [ ] 简体中文、繁体中文和英文无缺词、溢出或错位。
- [ ] 配置标题与悬浮说明没有显示 `preferredWoodEnabled`、`lmlp.config.*` 等内部名称或翻译键。

### 材料列表与缓存

- [ ] 容器界面热键可以打开材料列表并正确返回。
- [ ] 当前维度实时扫描、远距离区块缓存和跨维度缓存正确。
- [ ] 列表刷新、页面切换、排序、忽略、HUD 和导出正常。
- [ ] 当前选中投影与材料列表一致。

### 配方与最小子材料

- [ ] JEI-only 环境可使用配方摘要、详情、选择器和转移。
- [ ] 左键查看配方、右键查看用途。
- [ ] 普通点击与 Shift 点击转移正确。
- [ ] Tooltip 不被 JEI、按钮或 scissor 截断/穿透。
- [ ] 多配方、无序配方、多产出和候选槽位数量正确。
- [ ] 循环配方不会放大数量。
- [ ] 原木、煤炭块、白色床和选择组结果正确。
- [ ] 首次进入最小子材料页不会长时间卡死。

### 原点标记与渲染

- [ ] 光束穿过实体方块并位于蓝图、云层、天气和粒子之上。
- [ ] 靶心和标签层级正确。
- [ ] 转动视角时标记保持世界坐标。
- [ ] 跨 Overworld/Nether 换算正确。
- [ ] 到达目标后自动清除。

### 部署

- [ ] 实例 `mods` 中只有一个启用的 LMLP JAR。
- [ ] 部署 JAR SHA-256 与构建产物一致。
- [ ] 没有新增 crash report。
- [ ] 新日志中的异常已区分为模组问题或既有网络/账户问题。

## 13. 常见故障

### 编译通过但启动时 Mixin 崩溃

目标方法描述符不匹配。使用目标 JAR 的 `javap -p -s` 核对，不要相信相邻版本签名。

### 1.20.1 / 1.20.4 报 `UnsupportedClassVersionError`

成品被编译成 Java 21。检查 `java_release=17`，并确认 class major 为 61。

### JEI 页面为空

检查：

1. JEI runtime 是否已经上线；
2. 是否永久缓存了 runtime 未就绪时的空结果；
3. 单条配方异常是否错误中止整个类别；
4. 目标 JEI 版本与 Minecraft 是否匹配。

### Tooltip 背景被内容穿透

先 flush 已有 GUI 物品批次，再用读取当前矩阵的路径绘制背景、图标和文字。不要通过停止绘制底层内容规避。

### 材料数量异常膨胀

先查输出数量取整和循环检查顺序，再查多输入工具槽是否被误当作单输入循环链。

### 修复看起来完全没有生效

先核对：

- 当前实例中启用的 JAR 数量；
- 启动日志 `[LMLP build]`；
- JAR 内 Git Commit；
- 部署文件 SHA-256；
- 目标 class 是否确实进入 JAR。

### 旧版配置页面显示 `preferredWoodEnabled`

这是 MaLiLib `0.16.3`～`0.19.2` 的旧式配置翻译键差异，不是语言 JSON 完全缺失。检查：

1. 目标版本是否为 `1.20.1`、`1.20.4` 或 `1.20.6`；
2. 是否只添加了 `lmlp.config.name.*`，却没有提供旧式 `config.name.<内部名称小写>`；
3. 配置说明是否同样缺少 `config.comment.<内部名称小写>`；
4. 是否错误修改了持久化内部名称来解决显示问题。

修复后必须分别打开简体中文、繁体中文和英文的偏好表单检查标题、目标材料行、搜索结果与悬浮说明。

## 14. 本文维护规则

以后每次移植结束后：

1. 更新“最后更新”日期。
2. 若支持矩阵变化，先更新 `main/versions.toml`，再同步本文快照。
3. 把新差异写入对应主题或版本小节，不新建第二份移植文档。
4. 已失效方案移动到“禁止方案”或删除，不保留互相冲突的现行结论。
5. 记录行为契约和验证方法，不把一次性的调试流水账当作长期指南。
6. 新增自动保护时，同步 `tools/lmlp.py` 的 regression guard 和本文清单。
