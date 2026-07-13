# JEI 前置迁移可行性实验（Minecraft 1.20.6）

最后验证：2026-07-12，分支：`dev-newFeature`，JEI：`18.0.0.66-fabric`。

## 结论

迁移到 JEI **可行**，JEI 18 已提供 LMLP 所需的配方查询、配方布局、物品集合和配方转移接口。当前开发分支已完成 JEI 插件入口、运行时获取、按输出物品查询配方、输入/输出槽转换、原生布局、配方转移桥以及物品选择器数据源替换，并通过 Minecraft 1.20.6 / Java 21 完整构建。

`RecipeDetailScreen` 原先直接使用 REI 的控件、布局、tooltip 和 transfer 类型；本次移植已将原生布局和配方转移收口到两个 bridge，并删除 REI 源码与构建依赖。当前剩余工作是游戏内验证，尚不应在未验证前替换正式 Release。

## 当前实施状态

- 已新增 `JeiRuntimeBridge`，处理 JEI runtime 上线与下线。
- 已新增 `JeiRecipeResolver`，将 JEI 配方与槽位转换为 LMLP 现有摘要结构。
- 已新增 `JeiNativeDisplayBridge`，负责 JEI 原生布局、尺寸和槽位 tooltip。
- 已新增 `RecipeTransferBridge` / `JeiRecipeTransferBridge`，隔离 JEI transfer API。
- `RecipeDetailScreen` 主源码不再引用任何 `me.shedaniel.*` 或 `mezz.jei.*` 类型。
- 后备 3×3 配方布局改为 LMLP 自绘槽位和箭头，不再读取 REI 贴图。
- 物品列表编辑器改用 JEI `IIngredientManager`。
- `fabric.mod.json` 已将前置改为 JEI 18，REI 已从主源码和生成 jar 中移除。
- JEI-only jar 已部署到 1.20.6 开发实例；REI 保持 `.disabled`，等待游戏内验收。

## 已验证的 JEI 路径

- 通过 `IModPlugin.onRuntimeAvailable()` 保存 `IJeiRuntime`，不依赖 JEI 内部实现类。
- 使用输出物品 `IFocus` 限定 `IRecipeCategoriesLookup` 和 `IRecipeLookup`，可以获得所有可见的匹配配方。
- 使用 `IRecipeManager.createRecipeLayoutDrawable()` 获得标准化布局，并从 `IRecipeSlotsView` 读取 INPUT / OUTPUT 槽位。
- JEI 槽位能够转换为现有 `RecipeSummary`、`RecipeSlotSummary` 和 `IngredientSummary`，所以材料递归分解、总需求/缺少数量计算和 LMLP 自带配方布局可以继续复用。
- `IIngredientManager.getAllItemStacks()` 可以替代 REI `EntryRegistry`，作为物品列表编辑器的候选数据源。
- 本地 JEI API 编译实验成功，生成 jar 的 Minecraft 版本为 1.20.6，class major 为 65。

## 正式迁移需要完成的工作

1. **配方详情桥接**：把 `RecipeDetailScreen` 中 REI 的 `Display`、`Widget`、`Tooltip`、`ButtonArea` 和 transfer 逻辑移出主界面类。主界面只依赖 `RecipeNativeDisplayBridge` 和新的配方转移 bridge。
2. **JEI 原生布局**：使用实验中保存的 `IRecipeLayoutDrawable` 实现 JEI 版 native display bridge，负责尺寸、绘制、槽位 tooltip 和鼠标命中。
3. **自动放入配方**：使用 `IRecipeTransferManager` 替代 REI `TransferHandlerRegistry`；若 JEI 不允许在当前 Litematica 自定义界面直接转移，则保留按钮但明确显示不可用状态。
4. **tooltip 与选择器**：普通物品 tooltip 可继续使用 Minecraft 原生渲染；选择器使用 JEI `IIngredientManager`，并在 JEI runtime 尚未就绪时显示空状态而不是崩溃。
5. **移除 REI**：删除 `recipe/rei`、REI Maven 依赖、REI 语言文案和所有主源码中的 `me.shedaniel.*` 引用；`fabric.mod.json` 改为依赖 `jei`。
6. **多版本适配**：先在原生开发版本 1.20.6 完成并实机验证，再分别确认目标 Minecraft 版本对应的 JEI API 版本，不能假设 JEI 18 API 可直接用于 1.21.x。

## 验收重点

- JEI 启用且 REI 禁用时，游戏可以启动并进入材料清单、配置列表编辑器和配方详情页。
- 最小子材料递归分解结果与 REI 版本一致，木材选择组、无序配方、输出数量和配方偏好顺序不回退。
- JEI 原生配方布局、槽位 tooltip、滚轮和点击不会穿透 LMLP 面板。
- JEI runtime 尚未完成初始化或重载时返回空结果，不能缓存永久空 resolver。
- jar 中不再存在主路径对 REI 类的直接引用，REI 可以从实例中完全移除。

## 实验边界

当前开发包已经完成代码层面的 JEI-only 移植，但还不是可发布版本。必须通过游戏内启动、材料递归分解、配置物品选择器、原生配方布局、槽位 tooltip 和自动放入配方测试后，才能提交并向其他 Minecraft 版本移植。

## `RecipeDetailScreen` 深度研判

`RecipeDetailScreen` 是本次 JEI 迁移中风险最高的文件。它不只是显示配方，还同时负责 LMLP 配方树、递归展开、滚动与动画、REI 原生布局、槽位 tooltip、鼠标事件以及自动放入配方。LMLP 自身的配方树与动画可以完整保留；REI 原生布局和 transfer 必须从主界面类中彻底隔离。

### 当前耦合

现有 `RecipeNativeDisplayBridge` 的方向正确，已经抽象了尺寸、绘制、tooltip 和鼠标事件。但是主界面仍直接判断 REI `Display`，并在工厂中写死加载 `ReiNativeDisplayBridge`。即使 JEI 已成功查询配方，界面也无法据此选择 JEI 渲染器。

自动放入配方部分完全绕过了 bridge，直接使用以下 REI 类型：

- `Display`
- `Button` / `Widget` / `Tooltip`
- `CategoryRegistry` / `ButtonArea`
- `TransferHandler` / `TransferHandlerRegistry`
- `TransferHandlerRenderer`

这些类型还出现在字段和内部 record 中。REI 被移除后，JVM 加载 `RecipeDetailScreen` 时就可能因类型解析失败而崩溃，而不只是某个 transfer 按钮失效。

### 后备布局仍隐藏依赖 REI

LMLP 自带的 3×3 后备配方布局仍从 `roughlyenoughitems:textures/gui/display.png` 读取槽位框和合成箭头。这意味着即使关闭原生 REI 布局，只要 REI 完全移除，后备布局仍会缺少纹理。

正式迁移必须把槽位框与合成箭头改成 LMLP 自有资源或基础矩形绘制。不要把 REI 的贴图直接复制到 LMLP 资源中。

### JEI 原生布局映射

JEI 18 的 `IRecipeLayoutDrawable` 已覆盖原生布局所需的核心能力：

- `setPosition(x, y)`
- `drawRecipe(context, mouseX, mouseY)`
- `drawOverlays(context, mouseX, mouseY)`
- `getRect()`
- `getRecipeTransferButtonArea()`
- `getRecipeSlotsView()`

建议由 `JeiNativeDisplayBridge` 承担以下行为：

1. 内容绘制阶段调用 `setPosition()` 和 `drawRecipe()`。
2. 离开滚动裁剪区域后，通过 `drawOverlays()` 绘制槽位高亮与 tooltip，避免 tooltip 被面板 scissor 截断。
3. JEI 布局没有公开的拖动、释放和滚轮接口，这些 bridge 方法对 JEI 返回 `false` 即可。
4. 若需要点击槽位打开 JEI 配方/用途界面，应读取鼠标下的物品，创建 INPUT / OUTPUT `IFocus`，再调用 `IRecipesGui.show()`。

### 可变布局位置风险

`IRecipeLayoutDrawable` 是可变对象，`setPosition()` 会改变内部坐标。同一个配方可能同时出现在根列表和递归子列表；如果同一个布局对象一帧内被绘制在多个位置，当前只接收 `RecipeSummary` 的 tooltip 接口可能使用最后一次绘制位置，从而让前一个位置的 tooltip 命中错误。

现有 `NativeDisplayArea` 已保存 `x / y / width / height`。应扩展 `RecipeNativeDisplayBridge.renderTooltip()`，把当前命中区域的坐标传给 bridge，并在 tooltip 前重新设置布局位置。不要仅按 `RecipeSummary` 或配方类别缓存当前位置。

### JEI 自动放入配方

JEI 公共 API 可以覆盖现有 transfer 行为：

- 通过 `IRecipeTransferManager.getRecipeTransferHandler()` 获取当前容器与配方类别的 handler。
- 调用 `transferRecipe(..., maxTransfer, doTransfer)`：
  - `doTransfer=false` 用于检查可行性；
  - `doTransfer=true` 执行转移；
  - Shift 对应 `maxTransfer=true`。
- `IRecipeTransferError.getType().allowsTransfer` 可用于决定按钮是否允许点击。
- `getButtonHighlightColor()` 可用于错误高亮。
- `showError()` 可在配方槽位上绘制缺失材料等错误提示。

不建议直接使用 JEI 内部的 `RecipeTransferButton`。它不属于稳定公共 API，跨 JEI 版本容易失效。LMLP 应保留自己的按钮，只把检查、错误覆盖层和实际转移委托给 bridge。

### 推荐的桥接边界

新增独立的 `RecipeTransferBridge`，让 `RecipeDetailScreen` 只接触通用数据：

- 是否支持当前配方与容器；
- 是否允许转移；
- 按钮区域和高亮颜色；
- 普通 tooltip 文本；
- 错误覆盖层绘制；
- 执行单次或批量转移。

`RecipeSummary.nativeDisplay()` 可以暂时继续保存不透明的 `Object`，但主界面不得再自行强制转换。REI 或 JEI 类型判断只能存在于对应 bridge 内部。更长期可以引入无第三方依赖的 marker handle，但这不是首轮迁移的必要条件。

### 推荐实施顺序

1. 从 `RecipeDetailScreen` 中移除全部 `me.shedaniel.*` 类型和 REI 专用日志/常量命名。
2. 新建 `RecipeTransferBridge`，迁出当前 REI transfer 逻辑，并先提供禁用实现。
3. 将后备配方布局的 REI 纹理替换为 LMLP 自有绘制。
4. 让集成工厂根据已加载前置创建对应的 native display bridge，不再写死 REI 类名。
5. 扩展 native tooltip 接口，传递本次布局的实际坐标。
6. 实现并验证 `JeiNativeDisplayBridge`。
7. 实现 JEI transfer bridge，并测试普通点击、Shift 批量转移、材料不足和不支持容器。
8. 确认 JEI-only 实例能够进入配方详情页后，再删除 `recipe/rei` 和 REI 构建依赖。

### 风险判断

| 项目 | 风险 | 说明 |
| --- | --- | --- |
| JEI 原生配方布局 | 低到中 | 公共 API 完整，主要注意 scissor 和 tooltip 绘制顺序。 |
| 重复配方布局位置 | 中 | 必须按实际 `NativeDisplayArea` 重设布局坐标。 |
| 槽位点击打开 JEI | 中 | 需要正确区分查看配方与查看用途的 focus 角色。 |
| 自动放入配方 | 中到高 | 需要适配容器 handler、错误覆盖层以及 Shift 批量语义。 |
| 完全解除主界面 REI 类型 | 中到高 | 改动范围较大，但边界明确，可以分阶段验证。 |

最终判断：不需要重写整个 `RecipeDetailScreen`。应保留 LMLP 的配方树、递归展开、动画、滚动和自带布局，只重构“原生配方展示”和“自动放入配方”两个集成边界。
