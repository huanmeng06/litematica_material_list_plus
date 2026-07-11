# Minecraft 1.20.1 - 1.21.5 适配记录

这份记录只覆盖 LMLP 当前源码会触碰到的 Minecraft / Litematica / MaLiLib / REI 接口。它不是完整的 Minecraft API 索引，目的是在以后做多版本编译或回移功能时，先查这里，再决定要改哪一层。

最后更新：2026-06-10，分支：`dev-newFeature`。

## 验证范围

本地已实测到完整 `client-intermediary.jar` 和实例依赖的版本：

| MC | Fabric Loader | Java class major | Java 目标 | Litematica | MaLiLib | REI | Fabric API | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1.20.1 | 0.19.2 | 61 | 17 | 0.15.4 | 0.16.3 | 12.1.785 | 0.92.9+1.20.1 | 已验证 |
| 1.20.4 | 0.19.2 | 61 | 17 | 0.17.4 | 0.18.4-alpha.1 | 14.1.786 | 0.97.3+1.20.4 | 已验证 |
| 1.20.6 | 0.19.2 | 65 | 21 | 0.18.3 | 0.19.2 | 15.0.787 | 0.100.8+1.20.6 | 已验证 |
| 1.21 | - | - | 21 | - | - | - | - | 仅有 intermediary mappings |
| 1.21.1 | 0.19.2 | 65 | 21 | 0.19.60 | 0.21.10 | 16.0.799 | 0.116.12+1.21.1 | 已验证 |
| 1.21.4 | 0.19.3 | 65 | 21 | 0.21.6 | 0.23.5 | 18.0.808 | 0.119.4+1.21.4 | 已验证 |
| 1.21.5 | 0.19.3 | 65 | 21 | 0.22.4 | 0.24.3 | 19.0.809 | 0.128.2+1.21.5 | 已验证 |

目标范围内但本机暂时没有完整实例或 remapped client jar 的版本：`1.20.2`、`1.20.3`、`1.20.5`、`1.21.2`、`1.21.3`。以后 PCL2 启动过这些版本后，运行 `tools/scan-mc-compat.ps1` 补齐。

## 先看这几条

1. `1.20.1` 和 `1.20.4` 必须产出 Java 17 字节码，class major 是 `61`。不要用 Java 21 目标编译 1.20.1，否则 Mixin 应用后会在 Java 17 环境报 `UnsupportedClassVersionError`。
2. `1.20.6` 和 `1.21.x` 的 client jar 是 Java 21 字节码，class major 是 `65`。这些版本的构建可以使用 Java 21。
3. `MaLiLib WidgetListBase.onMouseScrolled` 在 `1.20.1` 是 `(int mouseX, int mouseY, double amount)`，从本地 `1.20.6` 起是 `(int mouseX, int mouseY, double horizontalAmount, double verticalAmount)`。这会直接影响 `WidgetListBaseMixin` 的注入描述符。
4. `Identifier` (`net.minecraft.class_2960`) 在 `1.21.1+` 构造器变成 private。`new class_2960(namespace, path)` 只能用于 `1.20.x`，`1.21.x` 要改用静态工厂。
5. `DrawContext.drawTexture` (`class_332.method_25290`) 在 `1.21.4+` 增加了 `Function<class_2960, class_1921>` render layer 参数。1.20.x / 1.21.1 的旧签名不能直接搬过去。
6. Litematica 0.22.x 开始有部分路径 API 从 `java.io.File` 转向 `java.nio.file.Path`，例如 `SchematicPlacement.getSchematicFile()` 和 `DataManager.getSchematicsBaseDirectory()`。当前 LMLP 主逻辑尽量不要依赖这些返回类型。

## LMLP 当前源码直接引用的 MC intermediary 类

| Intermediary | 常见 Yarn 名 | 当前用途 | 适配风险 |
| --- | --- | --- | --- |
| `class_124` | `Formatting` | 配方详情文本颜色 | 低 |
| `class_1799` | `ItemStack` | 材料、配方、tooltip、背包计数 | 低，当前用到的方法跨已验证版本稳定 |
| `class_1923` | `ChunkPos` | 判断投影触碰区块是否加载 | 低 |
| `class_2338` | `BlockPos` | schematic/cache 统计坐标 | 低 |
| `class_2382` | `Vec3i` | schematic 区域尺寸 | 低 |
| `class_2561` | `Text` | REI tooltip 文本 | 低 |
| `class_2680` | `BlockState` | schematic/cache 方块统计 | 低 |
| `class_2960` | `Identifier` | 贴图 ID、物品 ID、REI display ID | 高，`1.21.1+` 构造器变 private |
| `class_310` | `MinecraftClient` | 当前客户端、玩家、窗口、世界 | 中，字段名稳定但要按目标 jar 编译 |
| `class_327` | `TextRenderer` | tooltip 和文本渲染 | 低 |
| `class_332` | `DrawContext` | GUI 绘制、tooltip、clip、矩阵栈 | 高，`method_25290` 在 `1.21.4+` 变签名 |
| `class_437` | `Screen` | 打开 Litematica 材料列表和自定义详情页 | 低 |
| `class_465` | `HandledScreen` | 容器界面内热键打开材料列表 | 中，Mixin 必须用目标版本描述符确认 |
| `class_638` | `ClientWorld` | 区块加载判断、维度判断 | 低 |
| `class_7923` | `Registries` | 物品注册表 ID 获取 | 中，1.21 registry 周边变化多，编译时确认 |

## Minecraft API 差异

### Java class major

| MC | `MinecraftClient` class major | 对应 Java |
| --- | --- | --- |
| 1.20.1 | 61 | Java 17 |
| 1.20.4 | 61 | Java 17 |
| 1.20.6 | 65 | Java 21 |
| 1.21.1 | 65 | Java 21 |
| 1.21.4 | 65 | Java 21 |
| 1.21.5 | 65 | Java 21 |

经验规则：`1.20.1`、`1.20.4` 用 `--release 17`，`1.20.6+` 用 `--release 21`。`1.20.5` 本机尚未实测，但它属于 Mojang 切 Java 21 的区间，补实例后再用脚本确认。

### `class_2960` / Identifier

| MC | 可直接 `new class_2960(String, String)` | 可直接 `new class_2960(String)` | 可用静态工厂 |
| --- | --- | --- | --- |
| 1.20.1 | 是 | 是 | 有，但当前未依赖 |
| 1.20.4 | 是 | 是 | 有，但当前未依赖 |
| 1.20.6 | 是 | 是 | 有，但当前未依赖 |
| 1.21.1 | 否，构造器 private | 否 | `method_60655(String,String)`、`method_60654(String)`、`method_60656(String)`、`method_12829(String)` 等 |
| 1.21.4 | 否，构造器 private | 否 | 同 1.21.1 |
| 1.21.5 | 否，构造器 private | 否 | 同 1.21.1 |

当前源码风险点：

- `ToggleArrowRenderer` 使用 `new class_2960(MOD_ID, "...")`，适配 `1.21.x` 时必须改。
- 如果新增贴图、注册表 ID、REI display ID，不要在共享源码里直接 new `class_2960`；做一个版本分支 helper。

### `class_332` / DrawContext

| 方法 | 1.20.1 / 1.20.4 / 1.20.6 / 1.21.1 | 1.21.4 / 1.21.5 | 当前用途 |
| --- | --- | --- | --- |
| `method_51448()` | `() -> MatrixStack` | 相同 | 图标旋转和矩阵 push/pop |
| `method_44379(int,int,int,int)` | scissor 开启 | 相同 | 材料列表裁剪 |
| `method_44380()` | scissor 关闭 | 相同 | 材料列表裁剪 |
| `method_51427(ItemStack,int,int)` | 绘制物品 | 相同 | 列表、配方、详情页 |
| `method_51431(TextRenderer,ItemStack,int,int)` | 绘制物品 tooltip | 相同 | 详情页/悬浮 |
| `method_51434(TextRenderer,List<Text>,int,int)` | 绘制文本 tooltip | 相同 | REI tooltip fallback |
| `method_51433(TextRenderer,String,int,int,int,boolean)` | 绘制字符串 | 相同 | 详情页文本 |
| `method_25290` | `(Identifier,int,int,float,float,int,int,int,int)` | `(Function<Identifier,RenderLayer>,Identifier,int,int,float,float,int,int,int,int)` | `ToggleArrowRenderer` 贴图绘制 |

适配 `1.21.4+` 时，凡是旧版 `context.method_25290(texture, ...)` 都要加 render layer factory。可优先查目标 Yarn 名称中的 `DrawContext.drawTexture` 变体，再回到 intermediary 名。

### `class_1799` / ItemStack

已验证版本中，当前 LMLP 用到的方法保持稳定：

| 方法/字段 | 语义 | 1.20.1 - 1.21.5 已验证状态 |
| --- | --- | --- |
| `field_8037` | 空 ItemStack | 稳定 |
| `method_7960()` | 是否为空 | 稳定 |
| `method_7909()` | 获取 Item | 稳定 |
| `method_7914()` | 最大堆叠数 | 稳定 |
| `method_7947()` | 当前堆叠数量 | 稳定 |
| `method_7972()` | copy | 稳定 |

### `class_638` / ClientWorld 与 `class_1937` / World

| 方法 | 语义 | 已验证状态 | 当前用途 |
| --- | --- | --- | --- |
| `ClientWorld.method_8393(int chunkX, int chunkZ)` | 判断区块是否加载 | 1.20.1 - 1.21.5 稳定 | 决定材料列表实时/缓存 |
| `World.method_27983()` | 当前维度 key | 1.20.1 - 1.21.5 稳定 | 跨维度时强制使用 cache |

跨维度逻辑不要只按 chunk 坐标判断，因为下界/末地同坐标区块加载并不代表主世界投影可实时扫描。当前实现记录 placement 首次出现的维度，并要求 `placement` 在当前 `SchematicPlacementManager` 中且维度一致，才允许实时路径。

## MaLiLib / Litematica Mixin 差异

### MaLiLib `WidgetListBase`

| MC / MaLiLib | `onMouseScrolled` descriptor | LMLP mixin 形态 |
| --- | --- | --- |
| 1.20.1 / 0.16.3 | `(IID)Z` | `int mouseX, int mouseY, double amount` |
| 1.20.6 / 0.19.2 | `(IIDD)Z` | `int mouseX, int mouseY, double horizontalAmount, double verticalAmount` |
| 1.21.5 / 0.24.3 | `(IIDD)Z` | 同 1.20.6 |

其他当前注入点在已验证版本里保持稳定：

- `drawContents(DrawContext, int, int, float)`
- `refreshBrowserEntries()`
- `getBrowserEntryHeightFor(Object)`
- `reCreateListEntryWidgets()`
- `offsetSelectionOrScrollbar(int, boolean)`

### Litematica 关键类

已验证版本中，当前 LMLP 依赖的主要方法保持稳定：

| 类 | 方法 | 用途 | 风险 |
| --- | --- | --- | --- |
| `SchematicPlacement` | `getMaterialList()` | 接管材料列表入口 | 低 |
| `SchematicPlacement` | `getTouchedChunks()` | 判断是否可实时扫描 | 低 |
| `SchematicPlacement` | `getAllSubRegionsPlacements()` | cache 统计子区域 | 低 |
| `SchematicPlacement` | `getSchematicFile()` | 文件路径 | 中，`1.21.5` 返回 `Path`，旧版返回 `File` |
| `MaterialListPlacement` | `reCreateMaterialList()` | 刷新按钮兜底到 cache | 低 |
| `MaterialListBase` | `setMaterialListEntries(List)` | 写入 cache/scan 结果 | 低 |
| `MaterialListBase` | `setMaterialListType(BlockInfoListType)` | 切换 All/RenderedLayer 等 | 低 |
| `GuiMaterialList` | `<init>(MaterialListBase)` | 进入 GUI 时刷新当前状态 | 低 |
| `GuiMaterialList` | `initGui()` | 加按钮和状态提示 | 低 |
| `GuiMaterialList` | `getBrowserHeight()` | 为状态行预留高度 | 低 |
| `WidgetMaterialListEntry` | `setMaxNameLength(List,int)` | 列宽计算 | 低 |
| `TaskCountBlocksPlacement` | `(SchematicPlacement, IMaterialList)` / `(SchematicPlacement, IMaterialList, boolean)` | 实时扫描任务 | 低 |
| `DataManager` | `getSchematicPlacementManager()` | 当前维度 placement manager | 低 |
| `DataManager` | `getSchematicsBaseDirectory()` | schematic 目录 | 中，`1.21.5` 返回 `Path` |

### Litematica 维度注意点

`SchematicPlacement` 本身在当前这些 jar 中没有可直接读取的维度字段。维度由 `DataManager.getSchematicPlacementManager()` 这一层体现：不同维度会有不同当前 placement manager。跨维度缓存功能应该继续以“当前 manager 是否包含该 placement + 记录的维度 key 是否匹配”为判断依据。

## REI 差异记录

本地实例版本：

| MC | REI |
| --- | --- |
| 1.20.1 | 12.1.785 |
| 1.20.4 | 14.1.786 |
| 1.20.6 | 15.0.787 |
| 1.21.1 | 16.0.799 |
| 1.21.4 | 18.0.808 |
| 1.21.5 | 19.0.809 |

当前源码依赖的 REI API：

- `DisplayRegistry.getInstance().getAll()`
- `DisplayRegistry.isDisplayVisible(Display)`
- `Display.getInputEntries()`
- `Display.getOutputEntries()`
- `Display.getCategoryIdentifier()`
- `Display.getDisplayLocation()`
- `DefaultCraftingDisplay.getOrganisedInputEntries(3, 3)`
- `EntryStack.cheatsAs().getValue()`
- `EntryStacks.of(stack).getTooltip(TooltipContext.of(point), false)`
- `CategoryRegistry.getInstance().get(CategoryIdentifier)`
- `CategoryRegistry.CategoryConfiguration.getView(display)`
- `DisplayCategoryView.setupDisplay(display, bounds)`
- `Widgets.createSlot(Rectangle)`
- `Slot.getCurrentTooltip(TooltipContext.of(point))`

已踩坑：

- `1.20.1 / REI 12` 的 tooltip、滚轮事件、贴图绘制要按 1.20.1 的 Minecraft GUI API 处理，不能直接复用 1.20.6 的写法。
- `1.20.1 / REI 12` 的 `TooltipContext.of(...)` 仍应使用 `net.minecraft.class_1836.field_41070` 作为 tooltip context。不要把 `1.20.6+` 的 `net.minecraft.class_1792.class_9635.field_51353` 直接回移到 1.20.1，否则 `ReiTooltipBridge` 和 `ReiNativeDisplayBridge` 会编译失败。
- REI API 比 Minecraft intermediary 更容易在大版本间改泛型或 widget 行为。适配时先编译 `recipe/rei/*`，再进游戏看 tooltip 和 native display。

## 1.20.1 回移当前功能时的额外踩坑

这些是 `1.5.33+mc1.20.6` 回移到 `1.5.33+mc1.20.1` 时补充确认到的点，之前没有写得足够具体：

1. `fabric-api-0.92.9+1.20.1.jar` 是聚合包，直接把它放进 `javac` classpath 时，不一定能解析到所有 Fabric API 子模块。若出现 `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents` 或 `net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents` 找不到，class path 需要同时加入实例里的 `.fabric/processedMods/*.jar`。
2. 1.20.1 的编译 class path 建议包含：`client-intermediary.jar`、PCL `libraries/**/*.jar`、当前实例 active mods、`.fabric/processedMods/*.jar`、输出目录。只加 `mods/fabric-api-*.jar` 很容易漏掉 Fabric API 子模块或 cloth basic math。
3. Windows 上用 `javac @argfile` 时，PowerShell 5 的 `Set-Content -Encoding UTF8` 会写入 BOM，`javac` 可能把第一个参数读成 `?-encoding`。生成 argfile 时使用无 BOM UTF-8，例如 `New-Object System.Text.UTF8Encoding($false)`。
4. `javac @argfile` 中的 Windows 反斜杠会被当成转义字符处理，路径可能被吃成 `E:Minecraft[MC]PCL...`。写 argfile 时把路径统一转为 `/`，并给含空格的 class path 和源码路径加双引号。
5. 1.20.1 的 `DrawContext` 没有 `method_52706(Identifier,int,int,int,int)`。例如 `ToggleArrowRenderer` 必须回到旧版 `method_25290(texture, x, y, u, v, width, height, textureWidth, textureHeight)`。
6. 使用 1.20.1 `method_25290(...)` 绘制 LMLP 自带贴图时，`Identifier` 建议写完整资源路径，例如 `textures/gui/sprites/recipe_book/page_forward.png`，不要直接使用 1.21 风格 sprite id。
7. 1.20.1 / MaLiLib `0.16.3` 的滚轮链路是单轴参数：`Screen.method_25401(double mouseX, double mouseY, double amount)`、REI widget `method_25401(mouseX, mouseY, amount)`、`WidgetListBase.onMouseScrolled(int mouseX, int mouseY, double amount)`。从 1.20.6 回移时，要同时改普通详情页、REI native display 转发和 `WidgetListBaseMixin` 注入签名。
8. 成功回移后务必用 `javap -verbose -classpath <jar> io.github.huanmeng06.lmlp.LitematicaMaterialListPlus` 确认 `major version: 61`。只要出现 `major version: 65`，这个 jar 就不适合 1.20.1 的 Java 17 运行环境。

## 当前源码高风险文件

| 文件 | 关注点 |
| --- | --- |
| `src/main/java/io/github/huanmeng06/lmlp/gui/ToggleArrowRenderer.java` | `new class_2960(...)` 和 `DrawContext.method_25290(...)`，1.21.x/1.21.4+ 必改 |
| `src/main/java/io/github/huanmeng06/lmlp/mixin/WidgetListBaseMixin.java` | `onMouseScrolled` 注入签名必须跟 MaLiLib 版本一致 |
| `src/main/java/io/github/huanmeng06/lmlp/mixin/HandledScreenMixin.java` | `HandledScreen` key/close 注入使用 intermediary 方法名，按目标 jar 确认 |
| `src/main/java/io/github/huanmeng06/lmlp/cache/ChunkMissingMaterialListCache.java` | `ClientWorld.method_8393`、维度判断、Litematica placement manager |
| `src/main/java/io/github/huanmeng06/lmlp/recipe/rei/*.java` | REI major 版本差异集中区 |
| `src/main/java/io/github/huanmeng06/lmlp/material/ItemStackTexts.java` | Registry/Identifier 相关变更集中区 |

## 以后适配新版本的流程

1. 在 PCL2 里至少启动一次目标 MC + Fabric 实例，让 `.fabric/remappedJars/.../client-intermediary.jar` 和 mods 依赖落到本地。
2. 运行：

   ```powershell
   Set-Location -LiteralPath 'E:\Minecraft\[MC]PCL\mod_dev\_analysis_lmlp_dev_feature'
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\scan-mc-compat.ps1
   ```

3. 先看输出里的 Java class major、`Identifier`、`DrawContext.method_25290`、`WidgetListBase.onMouseScrolled`。
4. 按目标版本调整源码和 `fabric.mod.json` 依赖。
5. 编译时确认目标字节码：

   ```powershell
   javap -verbose -classpath '.\build\libs\<artifact>.jar' io.github.huanmeng06.lmlp.LitematicaMaterialListPlus |
     Select-String -Pattern 'major version'
   ```

6. 启动游戏后优先测试：

   - 从容器界面热键打开材料列表。
   - Litematica 材料列表刷新按钮。
   - 区块内实时扫描 / 远距离 cache / 跨维度 cache。
   - REI tooltip、配方详情页、native display 滚轮和点击。
