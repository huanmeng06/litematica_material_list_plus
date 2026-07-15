# LMLP 版本迁移指南

最后更新：2026-07-15
当前最高基准：Minecraft 26.1.2 / LMLP `1.8.0+mc26.1.2`

本文记录 LMLP 从 1.20.1 到 26.1.2 的实际迁移方法和已经发生过的故障。迁移新版本时先按本文检查，再开始改 API，避免重复引入已经修过的问题。

## 不可改变的行为契约

- UI、按钮、Tooltip、热键、配方跳转和快速合成行为必须与当前开发基准一致。
- 后台优化不能改变材料总数、缺少数、配方产出缩放和最小子材料结果。
- 原点标记必须使用对应版本的原版信标光束纹理、几何和动画，保持红色 OMMC 样式。
- 光束必须穿过实体方块，并位于方块、Litematica 蓝图、云层、天气和粒子之上。
- 靶心、红框黑底标签、坐标文字和“仅注视时显示标签”的条件不能丢失。
- 每个版本独立编译、静态校验和部署；不自动启动游戏。

## 标准迁移顺序

1. 从已验证的最高版本分支创建目标版本分支，不从旧发行包逆向拼功能。
2. 读取目标实例中的 Minecraft、Fabric Loader、Fabric API、Litematica、MaLiLib、JEI 和 Java 版本。
3. 优先使用实例中已安装的 JEI、Litematica 和 MaLiLib JAR 校对真实 API。
4. 先建立可编译的构建环境，再迁移业务代码；不要边猜映射边改功能。
5. 按“基础类型 → GUI → JEI → Mixin → 世界渲染”的顺序处理编译错误。
6. 执行完整构建，检查 JAR 元数据、Java class major、Mixin 配置和残留旧 API。
7. 部署到目标实例 `mods` 目录，确认只存在一个启用的 LMLP JAR，并核对 SHA-256。
8. 由用户进行游戏内视觉与大型整合包性能测试；确认后再 commit/push。

## 版本与构建边界

| Minecraft | Java | class major | 关键差异 |
| --- | ---: | ---: | --- |
| 1.20.1 / 1.20.4 | 17 | 61 | 旧 GUI、旧世界渲染状态；部分滚轮 API 为单轴 |
| 1.20.6 ～ 1.21.11 | 21 | 65 | GUI/RenderPipeline/FrameGraph 分阶段变化 |
| 26.1.2 | 25 | 69 | Minecraft 不再混淆，不再使用 Fabric intermediary 命名 |

26.1.2 已验证依赖：

- Fabric Loader `0.19.3`
- Fabric API `0.154.2+26.1.2`
- Litematica `0.27.10`
- MaLiLib `0.28.9`
- JEI `29.16.0.47`
- Mod Menu `18.0.0`

## 26.1+：官方命名空间迁移

这是 26.1+ 最先处理的破坏性变化。

- 游戏 JAR 和新 Litematica/MaLiLib 已直接使用 Mojang 官方名称，如 `Minecraft`、`ItemStack`、`GuiGraphicsExtractor`。
- `net.fabricmc:intermediary` 对 26.1 返回 `0.0.0`；不能继续用 `class_310`、`method_1507`、`field_1724` 编译。
- 26.1 的 Fabric 示例工程不声明 mappings 依赖，并使用新插件 ID：

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${loader_version}"
}
```

- 在升级 Minecraft 前，先在 1.21.11 环境把 intermediary/Yarn 源码迁到 Mojang mappings：

```sh
./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.11"
```

- Loom 的迁移不是完整语义重写。继承自 MaLiLib `GuiContext` 的方法、Mixin shadow 字段、反射字符串仍可能保留 `method_`/`field_`，必须再次全仓搜索。
- 迁移完成后的硬性检查：

```sh
rg 'class_[0-9]+|method_[0-9]+|field_[0-9]+' src/main/java src/main/resources
```

- Mixin 字符串目标也要改成官方名称；编译成功不代表 `@Inject` 目标和回调描述符正确。

## GUI 管线问题

### 26.1.2 GUI 提取阶段

- `GuiGraphics` 改为 `GuiGraphicsExtractor`。
- `Screen.render(...)` 改为 `Screen.extractRenderState(...)`。
- `AbstractWidget.renderWidget(...)` 改为 `extractWidgetRenderState(...)`。
- MaLiLib 的 `GuiContext` 继承 `GuiGraphicsExtractor`，应使用 `GuiContext.fromGuiGraphics(...)` 包装后继续调用 MaLiLib 的 `renderItem`、`drawString` 等便捷方法。
- 直接持有 `GuiGraphicsExtractor` 时使用原版新名称：`item`、`text`、`fill`、`blit`、`pose`、`enableScissor`、`disableScissor`。
- `GuiBase` 上针对 `render` 的 Mixin 必须改投 `extractRenderState`，否则构建能通过但启动时会因 required injection 找不到目标而失败。

### 列表和悬浮面板

历史故障：列表超行、文字越过裁剪区、悬浮候选面板被截断或覆盖。

- 每个可滚动列表必须在条目内容前后成对调用 scissor。
- 展开动画只能裁剪可见高度，不能提前发布不完整的材料结果。
- 候选物品过多时按屏幕高度计算行列，不要固定单列向下渲染。
- Tooltip/悬浮面板必须最后提交，背景、图标、文字的顺序固定为背景 → 图标 → 文字。

### 灰度图标

- 各版本统一采用“先渲染正常物品，再覆盖 `0x99505050` 中性灰遮罩”的禁用效果，遮罩范围必须与物品图标尺寸完全一致。
- `prewarm` 保留为无操作兼容入口；该效果不需要纹理缓存，也不需要预热。
- 不要使用离屏 framebuffer 渲染、同步读回像素、转灰度后重新上传纹理。该方案会增加首次显示开销，并容易受到 scissor、投影矩阵和 OpenGL 状态影响而发生闪烁或图标消失。
- 1.20.1～1.21.1 使用对应版本 `DrawContext` 的物品渲染和矩形填充方法；1.21.10 以后使用队列式 GUI 对应接口，但颜色、顺序和尺寸保持不变。
- 验收时检查普通物品、方块、药水等带颜色和透明像素的图标：模型仍应可辨认，同时整体呈现与新版本一致的压暗禁用状态。

### Litematica 原版导出按钮冲突

- 目标实例必须直接检查 `GuiMaterialList.initGui` 的字节码，不能只凭语言文件判断功能是否启用。1.21.1 的语言文件含原材料导出文本，但界面没有创建新增按钮；当前 1.21.10、1.21.11 和 26.1.2 会创建 `WRITE_TO_JSON` 与 `EXPORT`。
- LMLP 保留 Litematica 原有普通文件导出和自身 XLSX 子材料导出；对 1.21.10 及以上移除新增的原材料/自定义 JSON 两个控件，避免按钮和 Tooltip 重叠。
- Litematica 把按钮类型放在私有内部类中，不要从兼容 Mixin 直接引用该类型。应在 `initGui` 完成后根据两个按钮各自唯一的 Tooltip 翻译键精确移除控件。

### HUD 开关与渲染器注册

- HUD 开关不能只修改布尔值。切换后必须立刻同步当前 `MaterialListHudRenderer`：开启时重新加入 `InfoHud`，关闭时移除。
- 缓存、页面切换或扫描任务替换 `MaterialListBase` 后，还要把 `InfoHud` 指向当前列表实例的渲染器。
- 验收时分别测试普通材料页、最小子材料页、刷新列表和切换投影；按钮显示为开启后，关闭界面必须能立即看到 HUD。

## 热键重置问题

历史故障：绑定热键后“重置”仍为灰色，只有点击搜索框后才刷新；错误改法还曾导致整个热键页面内容消失。

最终原则：

- 参考目标版本 MaLiLib/Litematica 原生 `GuiConfigsBase`、`ConfigOptionChangeListenerKeybind` 的监听生命周期。
- 在键盘或鼠标完成绑定后立即调用 `updateKeybindButtons()`，不能依赖搜索框焦点变化触发重绘。
- 在 `ConfigOptionChangeListenerKeybind.updateButtons()` 尾部按当前值与默认值直接设置 reset button：

```java
button.setEnabled(!Objects.equals(
        keybind.getStringValue(),
        keybind.getDefaultStringValue()));
```

- 不要覆写或清空整个配置选项列表来刷新一个按钮；这会造成“页面只剩搜索图标/全部配置消失”。
- 每次升级 MaLiLib 后用 `javap` 确认 `onKeyTyped`、`onMouseClicked`、`updateKeybindButtons`、`updateButtons` 的实际名称和参数。

## 原点信标光束与层级

完整渲染细节见 [placement-origin-marker-porting-notes.md](placement-origin-marker-porting-notes.md)。迁移时至少确认：

- 使用原版 `beacon_beam` 纹理及对应版本原版 Beacon 几何/UV 计算，不使用自绘半透明方柱。
- 颜色、内外半径、高度和滚动速度与当前 OMMC 风格基准一致。
- 自定义 RenderType/Pipeline 关闭深度测试并禁止深度写入。
- 1.20.1/1.20.4 的旧状态 API 必须在绘制后完整恢复。
- 1.21.1 的稳定点是最终世界事件 + 明确 no-depth layer；错误地注入普通 WorldRenderer TAIL 会导致光束完全消失。
- 1.21.10/1.21.11 使用 FrameGraph，必须在完整 FrameGraph `execute` 之后绘制，否则光束会在蓝图、云层或天气下方。
- 1.21.10 与 1.21.11 的 pipeline 类和工厂签名不同，不能直接复制类名。
- 26.1.2 `LevelRenderer.renderLevel` 回调参数已改为 `CameraRenderState`、`Matrix4fc`、`ChunkSectionsToRender`；Mixin 回调描述符必须逐项对齐。
- 26.1.2 深度状态改为 `DepthStencilState(new CompareOp.ALWAYS_PASS, false)`；blend 改由 `ColorTargetState` 配置。
- 26.1.2 `CustomFeatureRenderer.render(...)` 拆成 `renderSolid(...)` 和 `renderTranslucent(...)`，两类提交都要绘制。
- 光束先画，靶心和标签后画，防止光束覆盖红框黑底标签。

## 材料计算与大型整合包性能

历史故障包括首次进入最小子材料页面卡死、过度优化后页面无法切换、结果直接不显示，以及大型整合包才复现而测试存档不复现。

保持结果不变的优化边界：

- 最小子材料按预算分段计算，并在普通材料列表阶段后台预热。
- UI 线程每帧只消费固定预算；不能在第一次切页同步完成整棵配方树。
- 缓存键必须包含目标物品、数量、JEI runtime/配方签名、配置签名和库存快照签名。
- 配方、循环检测、库存统计和 JEI 查询分别缓存；配置或 runtime 变化时精确失效。
- 后台任务只发布完整且签名仍匹配的结果，不能把旧任务的部分结果覆盖到新页面。
- 预热失败或超预算时允许下一帧继续，不能让页面切换等待 Future 完成。
- 大型整合包必须设置 JEI 查询截止时间和递归/节点预算，循环配方应尽早终止。

性能回归时同时检查：

- 第一次与第二次进入最小子材料页的耗时。
- 材料数量级很大时 UI 是否仍可响应。
- 切换最小子材料页、渲染层页能否立即完成。
- 结果与优化前 JSON/列表逐项一致，而不只是总行数一致。

## 配方正确性问题

已发生过的错误及约束：

- 配方产出大于 1 时，材料数和合成次数必须按 output count 缩放。
- 缓存基础配方时以数量 1 保存；展示不同数量时重新缩放，不能把已经缩放的可变对象重复复用。
- JEI native layout/display 是带位置和悬浮状态的可变对象，递归页面不能共享同一实例；必须 fork/重建。
- 循环检测必须在取整和递归扩张前执行，避免染色/还原类配方无限放大。
- 白色床、煤炭块等不应因为存在可逆或替代配方而被错误继续拆分。
- “任意原木”只展开木材选择组，不要把选择组递归解释成煤炭块等目标的通用原料。
- 新版本原木必须从当前注册表/标签动态枚举，不能维护旧版本硬编码列表。
- 新增原木进入候选悬浮面板后，还要验证 Tooltip、图标循环和点击选择均正常。

## JEI 交互问题

- 始终以目标实例的 JEI JAR 编译/校对；不要凭相邻版本猜接口。
- 验证配方原生布局、配方跳转、分类按钮、原料 Tooltip 和右下角“移动到容器中”按钮。
- Transfer 按钮既要校验点击行为，也要校验悬浮 Tooltip 和禁用原因渲染。
- GUI 提取管线升级时，JEI drawable 接收的上下文类型可能同步变化。
- JEI deprecated API 可暂时编译，但下次迁移前应记录替代接口，避免在大版本一次性集中失效。

## MaLiLib/Litematica API 易错点

- 26.1.2 `JsonUtils.parseJsonFile` / `writeJsonToFile` 使用 `Path`，旧 `File` 要调用 `toPath()`。
- `ItemType` 已迁到 `fi.dy.masa.malilib.util.data.ItemType`。
- `ChunkPos` 成为 record，字段访问改为 `x()`、`z()`。
- 原版/基类新增 final 方法时，自定义辅助方法不能同名；本次 `isDragging()` 必须重命名为业务专用的 `isReordering()`。
- 对 `remap = false` 的 MaLiLib/Litematica Mixin，Java 编译器不会验证字符串形式的注入目标，必须使用 `javap` 静态核对。

## 构建、JAR 与部署检查

```sh
./gradlew build
git diff --check
```

随后检查：

- `fabric.mod.json` 的 Minecraft、Litematica、MaLiLib、JEI 依赖版本正确。
- `litematica_material_list_plus.mixins.json` 的 Java compatibility level 与目标 Java 一致。
- 目标 JAR 根目录包含 `fabric.mod.json`、Mixin JSON、assets 和语言文件。
- `javap -verbose` 的 major version 正确：Java 17=61、Java 21=65、Java 25=69。
- 26.1+ 源码/JAR 不含 intermediary token。
- 没有诊断代码、测试标签、旧自绘光柱或 stale class。
- 部署前不要盲目 `clean`：某些分支的 `build/dependencies` 保存了本地依赖，清理后必须恢复。
- 部署后计算构建 JAR 与实例 JAR 的 SHA-256，两者必须一致。
- 实例 `mods` 中只保留一个启用的 LMLP JAR。

## 用户调试最小清单

- [ ] 打开配置页面，绑定一个热键后无需点击搜索框，重置按钮立即可用。
- [ ] 点击重置后按钮立即变灰，其他配置项仍完整显示。
- [ ] 原点光束为红色原版信标样式，穿实体方块可见。
- [ ] 光束位于蓝图、云层、天气和粒子上方。
- [ ] 靶心存在；只有注视目标时显示红框黑底坐标标签。
- [ ] 首次切换最小子材料页不卡死，渲染层页也能立即切换。
- [ ] 煤炭块、白色床、循环配方和多产出配方结果正确。
- [ ] 新原木出现在“任意原木”候选面板。
- [ ] JEI Tooltip、配方跳转和移动到容器按钮正常。
- [ ] 大型整合包结果与测试存档一致，无长时间卡死。

## 故障报告应附带

- Minecraft、Fabric Loader、Fabric API、Litematica、MaLiLib、JEI、Java 版本。
- 实际部署 JAR 文件名和 SHA-256。
- `latest.log`、crash report；卡死问题附线程 dump 或 watchdog 信息。
- 复现步骤、所在维度、是否加载蓝图、材料列表规模。
- 视觉问题附截图；材料错误附导出 JSON 和目标物品/配方。
