# PlacementOriginMarker 多版本移植备忘录

最后更新：2026-06-13
基准分支：`dev-newFeature`
适用范围：`PlacementOriginMarker` 原点光束、原点靶心图标、坐标标签、HUD 标签，以及相关渲染注册点。

这份文档只记录 LMLP 已经踩过、并且以后很容易复发的跨版本问题。它不是 Minecraft/Fabric API 全量索引。

## 最终行为契约

原点标记在各版本中必须保持同一套用户可感知行为：

1. 点击同一维度的投影原点坐标后，显示红色半透明光束和红色靶心图标。
2. 只允许点击当前维度的原点。其他维度的列表文字应保持不可点击状态。
3. 同一时间只能有一个 active marker。点击另一个原点时，旧原点的光束、靶心、文字状态都要释放。
4. 到达原点附近后自动清除，当前阈值是 `ARRIVAL_DISTANCE_SQUARED = 4.0D`。
5. 标签内容必须包含 placement 名称、坐标 `[x, y, z]`、距离 `xxm`。
6. 指向目标时才显示标签；未指向时只保留光束和靶心。
7. 光束、靶心、标签必须固定在世界坐标。玩家站着只转视角时，不能像贴在屏幕上一样不动。
8. 显示层级必须是：`world blocks / portal / projection / translucent geometry < beam < target icon < label background < label text`。
9. 不允许出现白色/灰白色闪烁面，也不允许出现大块红色透明脏面板。
10. 不使用 vanilla beacon beam 作为最终效果。

当前源码仍保留 Overworld/Nether 之间的 render target 换算逻辑：跨 Overworld/Nether 时只显示映射后的 beam，不显示同维度专属的靶心和标签。不要擅自扩展到 End 或自定义维度，也不要在没有明确需求时删除这段逻辑。

## 移植禁区

做渲染适配时，不要顺手改这些业务链路：

- `originPosition`
- `parseOrigin`
- `resolveRenderTarget`
- dimension / Nether 换算
- `KnownPlacementContext`
- `Marker`
- `activeMarker`
- `handleOriginClick`
- `canHighlightOrigin`
- `hasHighlight`
- `hasBeam`
- 到达后 `clear()` 逻辑
- 缓存逻辑
- 列表点击、hover、字体状态逻辑

如果光束或标签位置异常，先验证实际运行 jar 里的 class 是否更新，再查渲染阶段和矩阵链路。不要先怀疑 origin 数据。

## 渲染层级基线

### 1.20.1 - 1.21.4

已验证稳定的基线是：

- `InitHandler.registerModHandlers()` 中注册 `WorldRenderEvents.LAST.register(PlacementOriginMarker::render);`
- 在 `PlacementOriginMarker.render(WorldRenderContext context)` 中使用 `context.matrixStack()`。
- 使用 `camera.method_19326()` 得到 camera position。
- 世界位置换算使用 `pos + 0.5 - cameraPos`。
- 使用受控 `RenderSystem` 状态做最终 overlay：
  - blend enabled
  - depth test disabled
  - depth mask false
  - cull disabled
  - 绘制完成后恢复 cull、depth mask、depth test、blend、shader color、line width
- 绘制顺序保持：
  1. beam fill
  2. beam outline
  3. target icon
  4. label background
  5. label text

不要把 beam fill 放进 `RenderLayer.getDebugQuads()` / `class_1921.method_49042()`。这个 layer 在 1.20.1 中会导致白色/灰白色或红色半透明面闪烁。

不要用普通 `context.consumers()` 的 translucent/debug layer 作为最终方案。它的 flush 顺序容易被 Litematica projection、portal 或其他 translucent 内容压住。

### 1.21.5+

1.21.5 的 beam 和 target icon 仍然走 world overlay，但 label 使用 HUD fallback：

- `PlacementOriginMarker.render()`：
  - `WorldRenderEvents.LAST`
  - `OriginMarkerHudLabelRenderer.captureWorldFrame(context)`
  - world-space draw beam
  - world-space draw target icon
  - 不再用 world-space label 去压 Litematica projection
- `PlacementOriginMarker.renderHudLabel(DrawContext, RenderTickCounter)`：
  - 判断 active marker、same dimension、pointed at
  - 使用 captured world frame 投影到 screen
  - 调用 `OriginMarkerHudLabelRenderer.drawHudLabel(...)`
- `OriginMarkerHudLabelRenderer.register()` 必须在 Fabric client entrypoint 中尽早调用：
  ```java
  @Override
  public void onInitializeClient() {
      OriginMarkerHudLabelRenderer.register();
      InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
  }
  ```
- 不要把 HUD layer 注册放到 MaLiLib `InitHandler.registerModHandlers()`，否则可能错过 HUD layer registration 时机。
- `OriginMarkerHudLabelRenderer.register()` 必须有静态幂等 guard。
- 最终发布版不能保留 `LMLP HUD TEST`、`drawHudBeamGuide`、调试日志或 HUD 光柱辅助线。

1.21.5 的 GPU 绘制 API 已经变化，不能直接照搬旧版 `BufferRenderer/Tessellator` 提交方式。当前可用方向是 `RenderPipeline` + `RenderSystem.getDevice().createCommandEncoder().createRenderPass(...)`，并且 pipeline 必须配置 no depth test、no depth write、blend、no cull。

## 版本差异清单

### 1.20.1

- Java 目标必须是 17，class major 必须是 `61`。
- `fabric.mod.json` 依赖保持 1.20.1：
  - `minecraft: 1.20.1`
  - `litematica: >=0.15.4`
  - `malilib: >=0.16.3`
- `MaLiLib WidgetListBase.onMouseScrolled` 是单轴签名：`(int mouseX, int mouseY, double amount)`。
- `Screen.method_25401` / REI widget scroll 也是单轴转发，不能带 1.20.6+ 的 horizontal/vertical 双轴参数。
- `DrawContext` 不要使用 `method_52706`。
- 绘制贴图用旧 `method_25290(texture, x, y, u, v, width, height, textureWidth, textureHeight)`。
- REI 12 tooltip context 使用 `net.minecraft.class_1836.field_41070`，不要回移 `class_1792.class_9635.field_51353`。
- 手动 `javac @argfile` 时必须重新扫描所有 `src/main/java/**/*.java`，确认包含 `PlacementOriginMarker.java`。之前 stale class 会让“修了没用”的判断全部失效。
- Windows argfile 如遇中文路径或中文 jar 名，使用 ASCII classpath 副本，例如 `build/cp/mod001.jar`。
- 打包 jar 时必须同时 `-C build/classes .` 和 `-C src/main/resources .`，否则会漏掉根目录 `fabric.mod.json`，Fabric Loader 会完全不识别 mod。

### 1.20.4

- Java 目标仍然按 Java 17 / class major `61`。
- `Identifier` 构造器仍可用 `new class_2960(...)`。
- `ToggleArrowRenderer` 可以使用当前 1.20.4 可编译的 `DrawContext.method_52706(...)`，但不要把 1.21.4+ 的 render layer factory 参数带回来。
- 原点标记渲染基线跟 1.20.1/1.20.6 一致：`WorldRenderEvents.LAST` + 受控 `RenderSystem` overlay。

### 1.20.6

- Java 目标按 Java 21 / class major `65`。
- `Identifier` 构造器仍可用 `new class_2960(...)`。
- `MaLiLib WidgetListBase.onMouseScrolled` 是双轴签名：`(int mouseX, int mouseY, double horizontalAmount, double verticalAmount)`。
- 这是 1.20.x 中最接近 dev 主线的功能源，但移植回 1.20.1 时不能把 1.20.6 API 生搬硬套。
- 当前原点标记最终视觉应与 1.20.1 保持一致：red translucent beam、target icon、label，全部最高优先级 overlay。

### 1.21.1

- Java 目标按 Java 21 / class major `65`。
- `Identifier` 构造器变 private，使用 `class_2960.method_60655(namespace, path)`。
- `DrawContext.method_25290(...)` 仍是旧式 texture 参数，不需要 1.21.4+ 的 render layer factory。
- `TextRenderer.method_27522(...)` 仍可按当前分支的 String 路径适配。
- 原点标记 world overlay 逻辑与 1.20.6 接近，但所有 texture id 都要走 Identifier factory。

### 1.21.4

- Java 目标按 Java 21 / class major `65`。
- `Identifier` 使用 `class_2960.method_60655(...)`。
- `DrawContext.method_25290(...)` 和 `method_52706(...)` 需要 `Function<Identifier, RenderLayer>` 参数。当前分支使用 `class_1921::method_62277`。
- `TextRenderer.method_27522(...)` 需要 `Text` 参数，字符串要包成 `class_2561.method_30163(...)`。
- 原点标记仍走 world overlay，不要提前切到 1.21.5 HUD label，除非 1.21.4 也实际复现 projection 压 label 的问题。

### 1.21.5

- Java 目标按 Java 21 / class major `65`。
- `Identifier` 使用 `class_2960.method_60655(...)`。
- Litematica 0.22.x 之后部分路径 API 偏向 `Path`，不要假设都是 `File`。
- `PlacementOriginMarker` 需要适配新的 GPU pipeline：
  - `RenderPipeline.builder()`
  - `GpuBuffer`
  - `GpuTexture`
  - `RenderPass`
  - `RenderSystem.getDevice()`
- world-space label 难以稳定压过 Litematica projection；当前稳定方案是 beam/icon world overlay + label HUD overlay。
- HUD label 注册必须在 `LitematicaMaterialListPlus.onInitializeClient()` 直接调用，不能晚到 MaLiLib 初始化阶段。
- `OriginMarkerHudLabelRenderer.drawHudLabel(...)` 之后调用 `DrawContext.method_51452()` flush，确保 HUD label 实际提交。
- 最终 jar 中不能出现：
  - `LMLP HUD TEST`
  - `drawHudBeamGuide`
  - `WorldRenderEvents.END`
  - `PendingLabel`
  - Litematica TAIL Mixin
  - vanilla beacon beam

## 禁止作为最终方案的写法

这些方案可以短时用于诊断，但不要留在最终发布版：

- `class_822.method_3545(...)` / vanilla beacon beam。
- `textures/entity/beacon_beam.png`。
- `WorldRenderEvents.AFTER_TRANSLUCENT` 作为最终 marker 绘制阶段。
- `WorldRenderEvents.AFTER_ENTITIES` 作为最终 marker 绘制阶段，除非目标版本证明 `LAST` 不可用。
- `context.consumers().getBuffer(class_1921.method_49042())` 画 beam fill。
- 普通 world translucent layer 承载 label text。
- 用 HUD 固定画线替代 world-space beam。
- 调试用 `LMLP HUD TEST`、低频日志、diagnostic return-immediately、beacon diagnostic jar。

## 构建和部署核对表

每个版本适配完后都做这些检查：

1. `git status -sb`
2. 清理旧 class / build cache，尤其是 `build/classes`
3. 重新扫描 `src/main/java` 下全部 `.java`
4. 确认 `PlacementOriginMarker.java` 被编译进 jar
5. 打包时同时包含：
   - `build/classes`
   - `src/main/resources`
6. 确认 jar 根目录有 `fabric.mod.json`
7. 确认 jar 中有：
   - `assets/`
   - `litematica_material_list_plus.mixins.json`
8. 部署到实际 PCL 实例 mods 目录后，只保留一个启用的 LMLP `.jar`
9. 对实际部署 jar，而不是 `build/libs` 里的 jar，检查：
   ```powershell
   jar tf "<actual mods jar>" | findstr /i "fabric.mod.json assets mixins"
   javap -classpath "<actual mods jar>" -c io.github.huanmeng06.lmlp.gui.PlacementOriginMarker
   ```
10. 搜索实际源码和反编译结果，确认没有 stale/diagnostic 内容：
    ```text
    class_822.method_3545
    beacon_beam
    method_49042
    LMLP HUD TEST
    drawHudBeamGuide
    WorldRenderEvents.END
    PendingLabel
    RenderHandlerMixin
    ```
11. `git diff --check`
12. 解析 `fabric.mod.json` 和三份语言 JSON
13. commit + push
14. 最后 `git status -sb` 必须干净

当前约定：部署后不要再自动运行启动脚本。需要关闭游戏时，只能关闭窗口标题明确为 `Minecraft {version} Developer` 的 Java 进程。

## 故障判断

### 光束像贴在屏幕上

先查实际运行 jar 是否是新 jar。历史上出现过 `PlacementOriginMarker.java` 没有进 `javac argfile`，导致测试结果全部作废。确认 class 已更新后，再查 render event 和 world->camera transform。

### Fabric 完全不识别新 jar

优先查 jar 根目录是否缺 `fabric.mod.json`。手动打包时不能只打 `build/classes`，必须追加 `src/main/resources`。

### 光束出现白色或灰白色闪烁面

通常是用了 debug quads 或普通 translucent buffer。不要用 `class_1921.method_49042()` 作为 beam fill 的最终 layer。

### label 被 Litematica projection 压住

1.20.1 - 1.21.4 优先确认 marker 是否在 `WorldRenderEvents.LAST`、depth test 是否 disabled、depth mask 是否 false、text 是否最后绘制。
1.21.5 优先确认 HUD label 是否早注册、HUD callback 是否进入、`captureWorldFrame` 是否被调用、`drawHudLabel` 后是否 flush。

### HUD label 完全不显示

先查注册时机。`OriginMarkerHudLabelRenderer.register()` 必须在 Fabric client entrypoint 中调用，并带 `registered` guard。不要只在 MaLiLib initialization handler 中注册。

### label 位置不对或消失

不要先改 `clip.w`、camera forward、projection 算法。先确认：

- `captureWorldFrame(context)` 是否每帧调用；
- `context.positionMatrix()` / `context.projectionMatrix()` 是否可用；
- 是否 same dimension；
- `isPointedAt(...)` 是否为 true；
- HUD layer 是否实际执行。
