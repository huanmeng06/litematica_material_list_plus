# PlacementOriginMarker 多版本移植备忘录

最后更新：2026-07-15
当前视觉基准：Minecraft 1.21.11 / 26.1.2

本文只记录原点光束、靶心和注视标签的最终行为。完整迁移流程和其他历史问题见 [version-migration-guide.md](version-migration-guide.md)。

## 最终行为契约

1. 使用对应 Minecraft 版本原版信标光束纹理、几何和 UV 动画，不再使用自绘方柱或方框光束。
2. 光束为红色，宽度、内外半径、滚动速度和高度保持当前 OMMC 风格。
3. 光束必须穿实体方块显示，并位于方块、Litematica 蓝图、云层、天气和粒子之上。
4. 光束、靶心、标签固定在世界坐标，不能随视角粘在屏幕上。
5. 绘制顺序固定为：光束 → 靶心 → 标签背景 → 标签文字，标签不得被光束覆盖。
6. 标签使用红框黑底，包含 placement 名称、坐标和距离；只有注视目标时显示。
7. 同一时间只有一个 active marker；到达原点附近后自动清除。
8. 当前维度显示完整光束、靶心和标签；主世界/下界换算只保留现有业务规则，不扩展到末地或自定义维度。

## 不要改动的业务链路

- `originPosition` / `parseOrigin` / `resolveRenderTarget`
- dimension 与主世界/下界换算
- `KnownPlacementContext` / `Marker` / `activeMarker`
- `handleOriginClick` / `canHighlightOrigin` / `hasHighlight` / `hasBeam`
- 到达后 `clear()`、缓存和列表点击逻辑

出现错位时先核对实际部署 JAR 和渲染阶段，不要先修改坐标数据。

## 分版本渲染基线

### 1.20.1 / 1.20.4 / 1.20.6

- 使用该版本最后可用的世界渲染事件。
- 使用对应版本原版 Beacon Renderer 的几何和纹理。
- 关闭 depth test、关闭 depth write，绘制后恢复 blend、cull、depth mask、shader color 等全部状态。
- 旧 debug/translucent buffer 容易被蓝图覆盖或出现白色闪面，不能作为最终 layer。

### 1.21.1

- 已验证方案是最终世界事件 + 显式 no-depth RenderLayer。
- 注入普通 `WorldRenderer.render` TAIL 曾导致光束完全不可见，不要复用该失败方案。

### 1.21.10 / 1.21.11

- 世界渲染已进入 FrameGraph。
- 必须在完整 `FrameGraphBuilder.execute(...)` 之后绘制，才能压过蓝图、云层、天气和粒子。
- 1.21.10 与 1.21.11 的 pipeline/RenderLayer 工厂不同，使用各自版本实际类签名。

### 26.1.2

- `LevelRenderer.renderLevel` 参数为：
  `GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState, Matrix4fc, GpuBufferSlice, Vector4f, boolean, ChunkSectionsToRender`。
- 仍在 FrameGraph execute 后注入，但 Mixin callback 必须严格匹配上述参数。
- no-depth pipeline：`new DepthStencilState(CompareOp.ALWAYS_PASS, false)`。
- translucent blend：`new ColorTargetState(BlendFunction.TRANSLUCENT)`。
- `CustomFeatureRenderer` 已拆分为 `renderSolid` 和 `renderTranslucent`，两者都要提交。

## 重点验收

- [ ] 红色原版信标纹理与动画正确。
- [ ] 隔着实体方块仍可见。
- [ ] 不被蓝图、云层、天气或粒子压住。
- [ ] 靶心存在且在光束上方。
- [ ] 未注视时不显示标签；注视时显示红框黑底标签。
- [ ] 标签始终在光束上方，没有被覆盖。
- [ ] 转动视角、跨区块和改变窗口尺寸时位置稳定。
- [ ] 到达目标后自动清除，切换 marker 时旧 marker 释放。

## 构建后静态检查

- `PlacementOriginMarker`、自定义 RenderType/Pipeline 和 FrameGraph Mixin 都已进入 JAR。
- Mixin 的方法名、调用点 descriptor 和 callback 参数与目标版本 `javap` 输出一致。
- JAR 内不含旧自绘光柱诊断代码、固定屏幕 HUD 光柱或 stale class。
- 实际部署 JAR 的 SHA-256 与 `build/libs` 一致。
