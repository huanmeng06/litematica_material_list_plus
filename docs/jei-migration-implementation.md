# JEI 适配实施与后续移植指南

最后更新：2026-07-13
基准分支：`dev-newFeature`
基准环境：Minecraft `1.20.6`、Java `21`、JEI Fabric `18.0.0.66`

本文记录 Litematica Material List Plus（LMLP）从 REI 前置迁移到 JEI 后的实际实现、行为约定和已踩过的坑。迁移前的 API 调研与风险分析仍见 [jei-migration-feasibility.md](./jei-migration-feasibility.md)。后续向 Minecraft 1.21.x 移植时，应以本文的桥接边界和验收清单为准，不能直接复制 JEI 18 的方法签名。

## 最终目标

JEI 适配必须满足以下约束：

1. 游戏只启用 JEI、完全禁用 REI 时，LMLP 可以启动并使用材料列表、配方详情、物品选择器和快速合成。
2. LMLP 核心界面不直接引用 `mezz.jei.*` 类型，JEI 类型只能出现在 `recipe/jei` 实现层。
3. 配方查询结果继续转换为 LMLP 自有的 `RecipeSummary`、`RecipeSlotSummary` 和 `IngredientSummary`，材料树与数量计算不依赖 JEI UI 实现。
4. 原生配方布局、槽位点击、Catalyst 和配方转移通过 bridge 接入；第三方 API 变化不能扩散到整个 `RecipeDetailScreen`。
5. 不再保留 REI 运行时依赖，但从 REI 提取并继续使用的按钮素材必须保留 MIT 许可声明。

## 依赖和运行环境

Minecraft 1.20.6 使用 Java 21。开发实例中的依赖状态应为：

- JEI `jei-1.20.6-fabric-18.0.0.66.jar`：启用。
- REI `RoughlyEnoughItems-15.0.787-fabric.jar.disabled`：禁用。
- LMLP JEI 开发包：启用。

`fabric.mod.json` 应依赖 JEI 18，不再声明 REI。构建脚本中也不能残留 REI Maven/API 依赖。

## 代码结构

### JEI 运行时

`JeiRuntimeBridge` 通过 JEI 插件生命周期保存 `IJeiRuntime`：

- `onRuntimeAvailable()`：记录 runtime。
- runtime 尚未就绪或已经失效：返回空结果，不抛异常。
- 不要永久缓存“runtime 未就绪时的空 resolver”，否则进入世界后仍可能一直查不到配方。

### 配方查询

`JeiRecipeResolver` 的处理流程：

1. 用目标物品建立 `RecipeIngredientRole.OUTPUT` 的 `IFocus`。
2. 查询匹配的配方类别与配方。
3. 使用 `IRecipeManager.createRecipeLayoutDrawable()` 创建标准 JEI 布局。
4. 从 `IRecipeSlotsView` 读取 INPUT / OUTPUT 槽位。
5. 将槽位转换为 LMLP 自有摘要结构。

数量计算必须使用：

```text
crafts = ceil(required / outputCount)
ingredientTotal = ingredientCountPerCraft * crafts
```

同一输入在多个槽位出现时，要先累加单次配方用量，再乘合成次数。不能把槽位数量、候选物品数量和合成次数混为一谈。

### 原生配方显示

`RecipeNativeDisplayBridge` 是核心界面与第三方配方查看器之间的边界。JEI 实现为 `JeiNativeDisplayBridge`，负责：

- 获取原生布局宽高。
- 调用 `IRecipeLayoutDrawable.setPosition()` 和 `drawRecipe()`。
- 调用 `drawOverlays()` 绘制 JEI 槽位 tooltip。
- 获取鼠标下的 `IRecipeSlotDrawable` 和当前显示物品。
- 左键槽位时以 OUTPUT focus 打开配方；右键时以 INPUT focus 打开用途。
- 绘制并处理加工台 Catalyst 标签。

`IRecipeLayoutDrawable` 的位置是可变状态。同一布局对象被绘制到不同递归节点后，处理 tooltip 或点击前必须重新设置本次区域的位置，不能假设它仍停留在上次的坐标。

### 配方转移

`RecipeTransferBridge` 隔离配方转移功能，JEI 实现为 `JeiRecipeTransferBridge`。

- 只使用 JEI 公共 `IRecipeTransferManager` / transfer handler API。
- 普通点击转移一次；Shift 点击执行最大批量转移。
- 材料不足、当前容器不支持、可转移三种状态必须分别反馈。
- 不直接实例化 JEI 内部的 RecipeTransferButton。
- 2×2 背包合成使用玩家 inventory handler 作为后备容器；JEI 会拒绝无法放进 2×2 的 3×3 配方。

## RecipeDetailScreen 行为

`RecipeDetailScreen` 保留 LMLP 自己的滚动、递归材料树、展开动画和配方偏好顺序。JEI 只负责原生配方面板与第三方交互。

### 配方槽位点击

- 左键原生配方中的物品：打开 JEI 的该物品配方。
- 右键原生配方中的物品：打开 JEI 的该物品用途。
- 不再要求按 Shift 才进入详情。

### 摘要到详情的导航

材料列表内联摘要支持：

- 点击摘要标题：打开主材料的配方详情页。
- 点击一级子材料名称：打开同一详情页，定位到该配方的对应材料节点并自动展开。
- 左侧三角箭头仍只负责当前摘要内的展开/收起，名称点击和箭头点击不能共用命中区域。
- 可点击名称悬停时显示手型光标，并使用加粗、下划线作为反馈。

详情页的初始定位参数应包含配方 ID 和物品 ID。进入页面后先建立展开路径，再根据材料行的内容坐标设置滚动条；不要用固定像素猜测目标位置。

### Catalyst 加工台标签

配方右侧的 Catalyst 标签显示该配方所需的加工台：

- 连接板使用 LMLP 自有 `catalyst_tab.png`。
- 槽位底板和加工台物品由 JEI 标准 drawable / catalyst 数据绘制。
- 悬停显示标准物品 tooltip 和高亮面。
- 点击工作台 Catalyst 时，以工作台本身建立 OUTPUT focus，进入 JEI 的“4 个木板 → 工作台”配方页。
- 如果 Catalyst 不是可建立物品 focus 的普通物品，才回退到 `showTypes()` 打开配方类别。

### 转移按钮视觉

转移按钮复用 REI 原版按钮素材和状态，不手绘替代品：

- 普通状态：浅灰按钮、灰白 `+`。
- 悬停状态：高亮按钮、黄色 `+`。
- 禁用状态：使用 REI 对应禁用纹理与文字颜色。
- 按钮尺寸、居中方式和 tooltip 文案与原 REI 版本一致。

素材位于 `assets/litematica_material_list_plus/textures/gui/rei_button.png`。来源和 MIT 许可必须记录在 `THIRD_PARTY_NOTICES.md`。

## 任意类与悬浮候选面板

“任意木板”“任意原木”等 choice group 使用黄色普通字体，不加粗、不加下划线；只有作为可点击链接悬停时才临时加粗并加下划线。

候选面板使用配置项 `hoverPanelMaxRows`（界面名称“悬浮面板最大行数”）：

- 默认值 `10`，范围 `1–40`。
- 标题不计入最大行数。
- 每列先填满配置数量，再换到下一列。
- 屏幕宽度不足时允许减少列数并重新平衡行数，不能让面板越界。
- 材料列表和完整配方详情页必须共用相同布局规则。

## Tooltip 图层陷阱

JEI 原生槽位中的物品使用 GUI item renderer，内部深度通常高于普通二维矩形。仅给矩阵栈增加 `Z=400` 并不能保证 tooltip 背景位于最上层。

已经确认的根因是：MaLiLib 的 `RenderUtils.drawOutlinedBox()` / `drawRect()` 不读取 `DrawContext` 当前矩阵位移。结果会变成：

- tooltip 文字和候选物品在高 Z 层；
- tooltip 黑色背景仍在 Z=0；
- JEI 工作台、输出数量或转移按钮从背景中穿透。

正确做法：

1. tooltip 前调用 `DrawContext` 的 flush，提交已有物品批次。
2. 背景、边框和图标底板全部使用 `DrawContext.fill` 对应的 intermediary 方法绘制，使其读取当前矩阵。
3. 再在同一个矩阵层级绘制候选图标和文字。

禁止采用“tooltip 出现时停止绘制 JEI 面板”的规避方案。底层内容应正常存在，只由正确层级的 tooltip 覆盖。

## 循环配方与数量膨胀

允许染料继续拆分时，JEI 可能返回以下配方链：

```text
29 白色染料
→ 29 骨粉
→ ceil(29 / 9) = 4 骨块
→ 4 × 9 = 36 骨粉
```

旧逻辑在第二次遇到骨粉时才发现循环，因此错误保留了整组取整后的 `36`。正确需求仍是 `29`。

循环检查必须发生在采用配方、执行输出取整之前：

- 如果首选配方链会回到当前物品，把当前需求直接作为叶子。
- 最小子材料汇总保留进入循环前的数量。
- `MaterialTreeBuilder.hasChildren()` 和实际建树使用同一循环判断，不能显示一个最终只会绕回自己的展开箭头。

这不是骨粉专用规则。循环检测按物品 ID 和首选配方链工作，后续遇到可逆加工、压缩块/解压缩配方时也必须保持同样语义。

## REI 迁移后受影响的功能

| 功能 | 迁移后的实现 | 主要风险 |
| --- | --- | --- |
| 配方查询 | JEI focus + recipe lookup | 输出数量、候选槽位聚合 |
| 原生配方布局 | `IRecipeLayoutDrawable` | 可变坐标、裁剪、tooltip 层级 |
| 快速合成 | JEI transfer manager | 容器 handler、2×2/3×3 限制 |
| 槽位配方/用途跳转 | JEI INPUT/OUTPUT focus | 左右键角色不能反转 |
| 加工台 Catalyst | JEI category/catalyst | 点击应聚焦加工台物品 |
| 物品选择器 | `IIngredientManager` | runtime 未就绪时空状态 |
| 普通物品 tooltip | Minecraft 原生 | 与自定义候选面板的层级顺序 |
| REI 风格按钮 | 提取素材 + LMLP 绘制 | 许可声明、纹理状态索引 |

## 多版本移植注意事项

1. 先确认目标 Minecraft 版本对应的 JEI Fabric 版本；不要假设 JEI 18 可用于所有 1.21.x。
2. 检查 `IRecipeLayoutDrawable`、`IRecipeTransferManager`、`IRecipesGui`、`IIngredientManager` 的公开签名。
3. Minecraft 1.21.x 的 Identifier 和 DrawContext 贴图方法有变化，参见 [minecraft-1.20.1-to-1.21.5-compatibility.md](./minecraft-1.20.1-to-1.21.5-compatibility.md)。
4. JEI 实现层可以按版本调整；核心 `RecipeDetailScreen`、摘要模型和 bridge 接口应尽量保持一致。
5. 每次移植都要重新验证 tooltip 背景是否真正使用目标版本的矩阵感知绘制路径。

## 构建与部署

Minecraft 1.20.6 使用：

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home ./gradlew clean build
```

输出文件：

```text
build/libs/litematica_material_list_plus-1.7.1+mc1.20.6.jar
```

部署前应把实例中其他启用的 LMLP jar 改为 `.disabled`，只保留一个待测 jar。复制后对构建产物和实例 jar 执行 SHA-256 校验，必须完全一致。

## 验收清单

- [ ] JEI 启用、REI 禁用时正常启动。
- [ ] 材料列表、最小子材料页、配置物品选择器均可打开。
- [ ] 多配方、无序配方、不同输出数量的合成次数正确。
- [ ] 任意类候选面板标题不占最大行数，列数随配置变化。
- [ ] JEI 工作台、输出数量和转移按钮不会穿透 tooltip 背景。
- [ ] 左键槽位打开配方，右键槽位打开用途。
- [ ] 点击 Catalyst 工作台进入工作台自身的 JEI 配方页。
- [ ] 普通点击和 Shift 点击转移行为正确。
- [ ] 背包 2×2 配方可以快速合成，3×3 配方不会错误转入 2×2。
- [ ] 摘要标题可进入详情；子材料名称可定位并自动展开。
- [ ] 循环配方不会放大需求量，例如骨粉保持 `29` 而不是 `36`。
- [ ] 简体中文、繁体中文和英文没有缺失词条或布局溢出。
- [ ] `git diff --check` 和完整 Gradle build 通过。

## 当前边界

本文记录的是 `dev-newFeature` 上 Minecraft 1.20.6 的 JEI 实现。同步到其他版本分支前必须单独完成 API 编译和游戏内验证；不能仅因 1.20.6 构建成功就直接发布 1.21.x 文件。
