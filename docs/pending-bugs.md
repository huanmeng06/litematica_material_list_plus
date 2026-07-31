# 待修复问题

## BUG-20260730-01：半成品抵扣缺少详情

- 状态：已修复，等待实例验证
- 目标版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2
- 修复版本：1.9.2-beta12

### 现象

在最小子材料的缺失数量悬浮窗中，即使“显示分配详情”已经打开，“半成品抵扣”仍然只显示汇总数量，不显示具体由什么半成品抵扣。

例如背包中的玻璃被用于抵扣灰色染色玻璃所需的沙子时，目前只显示：

```text
半成品抵扣    1 个
```

### 原因

当前 `IntermediateInventoryBudget.consume(...)` 虽然按物品 ID 维护了被消耗的库存数量，但只向递归材料计算返回一个合计整数。具体由哪些候选物品提供了这次抵扣会在这里丢失。后续的 `Accumulator`、`DisplayData` 和 `AllocationTooltip` 也只保存 `intermediatePreparedCount` 汇总值，因此 UI 没有可展开的数据。

“显示分配详情”本身工作正常；它目前只能展开已经携带来源列表的背包候选材料和已有制品抵扣。修复时需要让半成品库存消费返回逐物品明细，并随递归叶子材料一起累计，同时纳入超时回滚 checkpoint，避免详情与实际抵扣结果不一致。

### 预期效果

打开“显示分配详情”时，在“半成品抵扣”下面缩进显示实际抵扣来源，并保持数字严格对齐：

```text
半成品抵扣       1 个
    玻璃         1 个
已有制品抵扣     0 个
```

关闭“显示分配详情”时，继续只显示“半成品抵扣”的汇总行。

### 验收标准

1. 半成品名称、图标和实际抵扣数量在计算阶段被完整记录。
2. 同时存在多种半成品时，每种材料独立显示一行，合计与“半成品抵扣”一致。
3. 详情开关只控制子条目的显示，不改变材料计算结果。
4. 悬浮窗缩进、数字对齐和组数格式与现有明细一致。
5. 所有受支持版本完成构建、部署和对应实例测试后再提交。

## BUG-20260730-02：分配 Tooltip 渲染层级错误

- 状态：已修复，等待实例验证
- 目标版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2
- 修复版本：1.9.2-beta12

### 现象

显示最小子材料的分配 Tooltip 时，材料条目的其他悬浮文字仍会绘制在 Tooltip 上方，导致文字穿过 Tooltip 的边框和内容区域。

### 原因

自定义分配 Tooltip 目前在 `WidgetListBase.drawContents(...)` 末尾绘制。这个位置只保证它晚于列表条目，却仍早于 GUI 其余顶层控件以及 Malilib 最终的 `postRenderHovered(...)` 悬浮阶段。因此后续产生的“不匹配的方块”等原生提示会覆盖在自定义面板上。

这不是单纯的颜色或 Z 数值问题，而是绘制阶段错误。修复时应把分配 Tooltip 延后到页面最终悬浮层统一绘制，并在命中分配 Tooltip 时抑制同一帧的冲突原生提示。

### 预期效果

分配 Tooltip 应处于该页面悬浮内容的最上层。显示 Tooltip 后，条目文字、来源提示及其他页面元素都不能覆盖其背景、边框或内容。

### 验收标准

1. 分配 Tooltip 的背景、边框、图标和文字作为同一最上层内容绘制。
2. 条目文字及其他悬浮提示不会穿透或覆盖分配 Tooltip。
3. Tooltip 靠近屏幕边缘自动调整位置时，层级仍然正确。
4. 展开和滚动材料条目后，层级仍然正确。
5. 所有受支持版本完成对应场景验证。

## BUG-20260730-03：旧版本应用偏好原理图后原点发生偏移

- 状态：已修复，等待实例验证
- 已确认版本：1.20.1、1.20.4、1.20.6
- 已测试未发现问题：1.21.10
- 待排查版本：1.21.1、1.21.11、26.1.2
- 修复版本：1.9.2-beta12

### 现象

在材料列表中完成“替换为偏好”并应用新原理图后，新投影没有出现在原投影设定的位置，而是跳到距离原位置很远的地方。1.20.1、1.20.4 和 1.20.6 已复现。

### 初步原因

三个 1.20.x 版本使用旧版 Litematica 公共 API 复制投影。当前代码把 `SubRegionPlacement.getPos()` 返回的子区域相对坐标直接传给 `moveSubRegionTo(...)`；但该 API 接收的是世界绝对坐标，内部还会减去投影原点并进行逆旋转/镜像变换。因此原点被重复参与换算，子区域会产生很大的坐标偏移。

1.21.11 使用 placement NBT 整体复制，不经过这条旧版坐标换算路径，所以代码层面看不受同一原因影响；其余较新版本仍需实际确认。

修复时应直接复制子区域的相对位置和状态，或先严格换算为该旧版 API 所要求的世界坐标，不能继续把相对坐标传给 `moveSubRegionTo(...)`。

### 预期效果

应用 preferred 原理图后，新投影必须与原投影保持完全相同的世界放置状态，包括：

1. 投影原点和世界坐标。
2. 整体旋转、镜像及启用状态。
3. 每个子区域的相对位置、旋转、镜像、渲染和实体设置。
4. 原投影重新加载后仍位于原来的位置，并保持关闭状态。

### 验收标准

1. 在已确认的旧版本中，应用 preferred 前后原点坐标完全一致。
2. 多子区域、经过旋转或镜像的原理图不会发生位置偏移。
3. 关闭 preferred 并重新打开原投影后，原投影位置和方块完整性均正常。
4. 逐一检查所有受支持版本，确认问题的实际影响范围。

## BUG-20260730-04：1.21.1 创建偏好副本时崩溃

- 状态：已修复，等待实例验证
- 已确认版本：1.21.1
- 修复版本：1.9.2-beta12
- 崩溃报告：`crash-2026-07-30_00.55.54-client.txt`

### 现象

在 1.21.1 中执行“替换为偏好”并保存副本时，游戏在保存页面点击事件中崩溃。

### 已确认原因

`PreferredSchematicReplacement.createCopy(...)` 使用以下方式创建尚未保存的内存副本：

```java
new LitematicaSchematic(null, original.writeToNBT(), FileType.LITEMATICA_SCHEMATIC)
```

Litematica 0.19.60 的该构造器不接受空路径，会立即对传入的 `Path` 调用 `toFile()`，因此抛出 `NullPointerException`。这是 1.21.1 的 Litematica API 兼容性问题，与 1.20.x 的子区域坐标偏移不是同一个原因。

### 预期效果

1. 创建 preferred 内存副本时不依赖空文件路径。
2. 保存新副本和按住 Shift 覆盖现有副本均不会崩溃。
3. 保存后的 preferred 原理图可以正常加载并应用。
4. 修复后继续检查 1.21.1 是否还存在原点或子区域位置偏移。

## BUG-20260730-05：26.1.2 的 preferred 原理图丢失被替换方块

- 状态：已修复，等待实例验证
- 已确认版本：26.1.2
- 修复版本：1.9.2-beta12

### 现象

在 26.1.2 中生成并应用 preferred 原理图后，原本应替换为偏好材料的方块在新原理图中全部消失；未参与替换的方块仍然存在。

### 初步排查

该过程没有抛出异常，preferred 文件能够保存、加载并应用。日志显示材料条目由原图的 44 项减少为 preferred 的 33 项，说明数据确实发生了变化。

对实际生成文件进行 NBT 级只读检查后已确认：

1. 原图调色板包含 101 个状态，合法索引为 0～100。
2. preferred 的方块索引数据已经把被替换位置改成了 101～117。
3. preferred 保存出的 `BlockStatePalette` 却仍然只有原来的 101 个状态，没有写入 101～117 对应的 17 个目标状态。
4. 重新加载时这些索引全部越出调色板范围，于是被 Litematica 回退为空气。

因此问题已经定位为：26.1.2 / Litematica 0.27.10 中，当前逐格 `container.set(...)` 替换方式更新了压缩方块索引，却没有让最终序列化使用的调色板同步包含新增目标状态。这不是渲染缓存问题。

修复时应采用两阶段写入：先收集完整目标状态并建立稳定的新调色板映射，再写入方块索引；保存后立即回读并校验所有索引均小于调色板长度，不能继续直接依赖当前逐格扩展调色板的行为。

### 预期效果

1. 被替换方块应以目标偏好材料出现在 preferred 原理图中，不能变为空气。
2. 未参与替换的方块、方块状态、方块实体及计划刻保持不变。
3. 保存前容器回读和保存后文件回读的目标方块数量一致。
4. 重新进入世界或重新载入 preferred 后，替换结果仍然存在。

## BUG-20260730-06：26.2 点击原点时渲染崩溃

- 状态：已修复，等待实例验证
- 已确认版本：26.2
- 修复版本：1.9.2+mc26.2
- 崩溃报告：`crash-2026-07-30_03.38.44-client.txt`

### 现象

在投影列表中点击原点后，客户端立即崩溃。

### 已确认原因

26.2 的 `FeatureRenderDispatcher` 在一帧内复用同一个 `PreparedFrame`。LMLP 的原点标记渲染位于 `LevelRenderer.render(...)` 的当前渲染生命周期内，却再次调用：

```java
client.gameRenderer.featureRenderDispatcher().renderAllFeatures(BEAM_COMMANDS);
```

此时 Minecraft 当前帧的 `PreparedFrame` 仍处于使用状态，重复进入 `renderAllFeatures(...)` 会再次申请同一个对象，因此抛出：

```text
java.lang.IllegalStateException: PreparedFrame already in use
```

这不是 Mixin 注入失败，也不是光柱顶点或 Metal/Sodium 渲染数据错误，而是 26.2 渲染生命周期发生了嵌套冲突。

### 修复方式

不再从当前帧内部重新调用 `FeatureRenderDispatcher.renderAllFeatures(...)`。现在于 `LevelRenderer.submitBlockEntities(PoseStack, LevelRenderState, SubmitNodeCollector)` 尾部注入，只把原点光柱提交到已有的 `SubmitNodeCollector`，由当前 `PreparedFrame` 统一执行。准星、名称、坐标和距离改为 Fabric HUD 最终覆盖层绘制。

### 预期效果

1. 点击原点后正常显示原点标记，不再导致客户端崩溃。
2. 原点光柱进入 Minecraft 当前帧的正常提交与绘制流程；准星和文字固定显示在世界投影位置对应的 HUD 坐标。
3. 连续选择不同投影原点或重复点击时不会重复占用 `PreparedFrame`。
4. 不影响普通投影渲染以及 Sodium/Metal 环境下的其他渲染内容。

## BUG-20260730-07：26.2 物品 ID 列表被截断并写坏配置

- 状态：已修复，等待实例验证
- 已确认版本：26.2
- 修复版本：1.9.2+mc26.2
- 受影响配置：`recipeStopItems`、`keepAsLeafItems`

### 现象

打开物品 ID 文本列表后，完整条目会变成最多 12 个字符的残缺内容，例如：

```text
minecraft:iron_ingot -> minecraft:ir
minecraft:gold_ingot -> minecraft:go
minecraft:{color}_dye -> minecraft:{c
```

被截断的 ID 无法解析为物品，因此条目前方图标消失。退出页面后，残缺内容还会写回配置文件，并非单纯的显示裁剪。

当前 26.2 实例的配置已经受到影响：`recipeStopItems` 中原来的 14 项被截断；下次载入时，默认项迁移逻辑发现这些完整默认值不存在，又追加了一套默认项；再次进入编辑器后，新追加的默认项也被截断，最终形成 28 条残缺和重复记录。`keepAsLeafItems` 也有同样的截断。

### 已确认原因

`WidgetItemIdStringListEditEntry.addEntryTextField(...)` 先把文本框最大长度设为较大值并填入完整 ID，随后再通过 MaLiLib 的 `TextFieldWrapper` 注册文本框。

MaLiLib 的包装器使用全局共享且可变的 `TextFieldType.STRING`。当此前某个字符串文本框把这个共享类型的最大长度收缩到 12 后，后续物品 ID 文本框在包装时也会被强制改成 12。Minecraft 的 `EditBox.setMaxLength(12)` 会立即截断已有文本；退出页面时，LMLP 又从文本框读取截断值并通过 `config.setStrings(...)` 保存，导致磁盘配置永久受损。

### 修复方式

1. 物品 ID 文本框完成 `TextFieldWrapper` 注册后，再独立恢复足够的最大长度并重新写入完整原值，避免受到全局 `TextFieldType.STRING` 状态污染。
2. 保存前比较编辑结果和当前配置，页面没有实际变化时不再触发写入。
3. 当前 26.2 测试实例中可以明确识别的截断默认项和重复项已经恢复；修复前配置保留为 `litematica_material_list_plus.pre-list-fix.json`。

### 预期效果

1. 所有完整物品 ID 和通配符条目均能完整显示、编辑和保存。
2. 打开并直接关闭列表不会改变任何配置值。
3. 条目图标能根据完整 ID 正常解析和显示。
4. 已损坏的内置默认项恢复且不再重复，用户自定义项不被错误覆盖。
5. 重新启动游戏并多次进入列表后，配置仍保持一致。

## BUG-20260730-08：拖动物品 ID 条目时提示在部分版本消失

- 状态：已修复，等待实例验证
- 已同步版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2、26.2

### 现象与原因

拖动物品 ID 列表条目时，部分 Minecraft 版本不显示“移动：物品名”提示。拖动提示原本在列表的 scissor 裁剪区内登记；不同版本对延迟 Tooltip 的提取时机不同，提示可能继承列表裁剪范围并被裁掉。

### 修复方式

列表只计算拖动提示内容，不再立即登记 Tooltip。页面完成列表绘制并关闭 scissor 后，在最终悬浮层统一登记 Tooltip，保证其位于条目图标之上且不受列表裁剪影响。

## BUG-20260730-09：26.2 原点信息框层级错误且视觉尺寸变化

- 状态：已修复，等待实例验证
- 已确认版本：26.2
- 修复版本：1.9.2+mc26.2

### 现象与原因

光柱恢复后，原点准星和信息框仍在世界渲染层绘制，会被 Litematica 后绘制的投影线覆盖。原实现使用近似距离补偿缩放世界空间面板，因此玩家移动时面板的屏幕视觉尺寸也会变化。

### 修复方式

光柱继续留在世界渲染中；准星和三行信息使用 `GameRenderer.projectPointToScreen(...)` 投影到 GUI 坐标，并通过 Fabric HUD 最后一层绘制。准星固定为 20 x 20 GUI 像素，信息框只受用户配置的文字缩放控制，不再随距离变化，同时始终位于投影线之上。

## BUG-20260731-01：跨维度缓存材料列表无法打开材料偏好

- 状态：已修复，待 RC2 实例验证
- 已确认版本：1.20.1
- 发现版本：1.9.4-RC1
- 修复版本：1.9.4-RC2
- 已同步版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2、26.2

### 现象

跨维度选择投影并打开材料列表后，列表能够以“维度缓存”状态正常显示；但点击“替换为偏好”会提示：

```text
当前材料列表没有可编辑的已加载原理图。
```

### 已确认的代码原因

问题发生在材料列表按钮与偏好表单之间，不是维度缓存丢失：

1. `GuiMaterialListMixin.PreferredReplacementButtonListener` 点击后调用 `GuiPreferredMaterialForm.forMaterialList(...)`，随后只通过 `form.hasSchematic()` 判断能否打开；失败时统一显示“没有可编辑的已加载原理图”。
2. `GuiPreferredMaterialForm.resolvePlacement(...)` 只识别 `ChunkMissingMaterialList` 和带 `MaterialListPlacementAccess` 的实时 `MaterialListPlacement`。
3. 跨维度列表实际类型是 `PersistedDimensionMaterialList`。它只保存 `contextKey`，没有实时 `SchematicPlacement`，因此 `resolvePlacement(...)` 必然返回 `null`。
4. `forMaterialList(...)` 随后同时把 `placement`、`schematic` 和 `sourceFile` 设为空，最终触发上述通用错误。

因此，材料列表能够正确读取“维度缓存”，但材料偏好入口仍错误地把“存在实时投影”当成唯一可用条件。

### 现有数据是否足够

足够。`PersistedDimensionMaterialList.contextKey()` 可以定位 `ChunkMissingMaterialListCache` 中对应的 `PlacementContext`，而持久化记录已经包含：

- `schematicPath`：原始 `.litematic` 文件的绝对路径。
- `schematicName`、投影名称和所属维度。
- `placementIdentity` 与 `placementSignature`。
- 材料缓存条目及其更新时间。

世界缓存恢复时还会通过 `NativePlacementStorageIndex` 对照 Litematica 自己保存的各维度投影文件；只有仍被原生维度文件确认、且属于其他维度的记录才会恢复为 `PERSISTED_DIMENSION`。因此修复无需新增第二份“离线投影”数据，也不应放宽现有原生文件校验。

点击按钮时仍需重新检查路径，因为原理图文件可能在缓存恢复后被移动或删除。现有 `PlacementContext.schematicMissing()` 只检查 `File.isFile()`，实际打开前还应检查路径非空、文件可读，并让 Litematica 验证文件类型和内容。

### 建议修复方案

1. 为材料偏好入口增加一个明确的源解析结果，例如 `PreferredSchematicSource`，包含可选的实时 `SchematicPlacement`、必需的 `LitematicaSchematic` 和规范化的源文件路径；不要继续用 `placement != null` 同时代表“可读取原理图”和“可立即应用投影”。
2. 实时材料列表继续走现有路径，保留 `placement`，从而在保存 preferred 副本后继续显示“应用到当前投影”的确认界面。
3. `PersistedDimensionMaterialList` 应通过其 `contextKey` 查询缓存内部的 `PlacementContext`，读取经过校验的 `schematicPath`。不要从 `contextKey` 字符串中拆路径，因为键还包含维度、名称和投影身份，且分隔符可能与路径内容冲突。
4. 对有效路径复用“加载原理图”页面已经使用的 `LitematicaSchematic.createFromFile(...)` 读取逻辑，并显式要求 `LITEMATICA_SCHEMATIC`。各 Minecraft 分支的 `Path`/`File` 和方法重载不同，移植时保留各分支现有适配。
5. 缓存入口应以 `placement = null` 调用现有 `GuiPreferredMaterialForm.forSchematicFile(...)`。`GuiPreferredSchematicSave` 已有安全分支：当 `sourcePlacement == null` 时只保存 preferred 副本并返回，不进入投影替换确认。
6. 为“缓存上下文不存在”“路径为空”“文件不存在或不可读”“Litematica 解析失败”提供不同错误提示和日志字段，避免继续全部显示成“没有已加载原理图”。

### 安全边界

- 不自动切换玩家维度。
- 不把其他维度的投影注入当前维度的 `SchematicPlacementManager`。
- 不尝试从材料缓存条目重建原理图；材料列表不包含方块坐标、方块状态和方块实体等完整数据。
- 不在异维度缓存入口保存后自动替换原投影。此入口只能生成 preferred 原理图副本；回到对应维度后再由用户加载或应用。
- 不绕过 `NativePlacementStorageIndex` 的原生维度文件身份校验，也不因为源文件仍存在就恢复一个已被 Litematica 删除的投影记录。

### 预计涉及文件

- `ChunkMissingMaterialListCache.java`：按材料列表/`contextKey` 提供只读且经过校验的偏好源信息。
- `GuiPreferredMaterialForm.java`：把“实时投影源”和“仅文件源”统一为明确的解析结果。
- `GuiMaterialListMixin.java`：根据解析失败原因显示准确提示。
- 三个语言文件：补充路径缺失、不可读和解析失败提示。
- 各版本分支的原理图文件读取兼容代码，尤其是 1.20.x、1.21.1 的 `File`/`Path` 差异。

### 主要风险

1. 把缓存源误当成实时投影，会在当前维度应用错误的投影状态；必须保证缓存路径的 `sourcePlacement` 始终为空。
2. 直接信任过期路径可能编辑错误文件；应先用上下文键定位记录，再执行文件存在性、可读性和 Litematica 类型校验。
3. 若只修改 `resolvePlacement(...)`，仍无法得到缓存原理图，因为该方法返回类型只有 `SchematicPlacement`；修复需要把“源文件可编辑”从“投影可编辑”中拆开。
4. 不同 Litematica 版本的 `createFromFile(...)` 参数和目录条目 API 不一致，必须逐版本编译，并重点检查旧版 Mixin remap 警告。

### 预期效果

1. 跨维度缓存记录包含有效且可读的原理图路径时，“替换为偏好”应直接打开对应原理图的材料偏好界面。
2. 原理图文件不存在、不可读或与缓存身份不一致时，应明确提示具体原因，不能误称为没有已加载原理图。
3. 当前维度的实时投影材料列表保持现有编辑行为不变。
4. 在所有支持版本中验证跨维度缓存和实时投影两条入口。

### 补充验收场景

1. 在主世界加载投影并形成材料缓存，进入末地，从跨维度列表打开其材料列表，再点击“替换为偏好”；应看到与源原理图对应的可替换材料行。
2. 从缓存入口保存 preferred 副本后，当前维度的投影列表和选中项不得发生变化，也不得出现“应用到当前投影”确认界面。
3. 回到原维度后，原投影位置、启用状态和选择状态保持不变。
4. 删除或移动源 `.litematic` 后重复点击，应显示文件缺失/不可读提示，不崩溃、不生成空副本。
5. 手工制造缓存键存在但上下文已被原生维度索引拒绝的情况时，不得仅凭磁盘路径重新启用入口。
6. 重启游戏后重复有效路径场景，确认持久化恢复的上下文仍能打开偏好界面。
7. 实时投影材料列表保存 preferred 后，原有“确认应用到当前投影”流程仍正常。
8. 配置关闭“在材料列表中显示材料偏好按钮”后，实时与缓存材料列表都不显示该按钮，加载原理图页面入口仍保留。

## BUG-20260731-02：1.21.1 配置项 `hoverPanelMaxRows` 没有翻译

- 状态：已修复，待 RC2 实例验证
- 已确认版本：1.21.1
- 发现版本：1.9.4-RC1
- 可能受影响版本：使用同一旧版配置构造器的 1.20.1、1.20.4、1.20.6
- 修复版本：1.9.4-RC2
- 已同步版本：1.20.1、1.20.4、1.20.6、1.21.1

### 现象

在配置界面中，“悬浮面板最大行数”显示成原始键名 `hoverPanelMaxRows`，没有显示中文翻译（截图已确认）。

### 初步分析

1.21.1 分支的 `Configs.Generic.HOVER_PANEL_MAX_ROWS` 使用旧版 `ConfigInteger` 六参数构造器，最后一个参数是普通注释文本，而不是显示名称翻译键。

当前语言文件虽然包含：

```text
lmlp.config.name.hover_panel_max_rows
```

但该配置实例没有把这个键传给 MaLiLib 的显示名称字段，配置表格因此回退到内部配置名 `hoverPanelMaxRows`。开发分支和较新 MaLiLib 已支持带显示名称键的更长构造器，所以同一代码在 1.21.11 等版本表现不同。

### 建议修复方向

1. 在支持多参数构造器的版本中，补充 `lmlp.config.comment.hover_panel_max_rows` 和 `lmlp.config.name.hover_panel_max_rows` 的正确参数位置。
2. 对只提供旧构造器的版本，沿用项目已有的翻译配置包装/匿名子类模式覆盖显示名称，不要把翻译键当作普通说明文字。
3. 三份语言文件都保留并校验 `lmlp.config.name.hover_panel_max_rows`，同时检查其他新增配置项是否也使用了错误的旧构造器参数。
4. 不修改配置文件中的持久化键 `hoverPanelMaxRows`，只修复界面显示名称。

### 验收场景

1. 1.21.1 中文界面显示“悬浮面板最大行数”，不再显示 `hoverPanelMaxRows`。
2. 英文和繁体中文界面分别显示对应翻译。
3. 修改数值、重启游戏后配置仍读写 `hoverPanelMaxRows`，旧配置不迁移、不丢失。
4. 检查 1.20.1、1.20.4、1.20.6 及其他支持版本的同一配置项，确认没有同类原始键名泄漏。

## BUG-20260731-03：低版本加载原理图页面缺少“替换为偏好”按钮

- 状态：已修复，待 RC2 实例验证
- 已确认版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2、26.2
- 发现版本：1.9.4-RC1
- 修复版本：1.9.4-RC2
- 已同步版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2、26.2

### 现象

在低版本的“加载原理图”页面中，没有出现“替换为偏好”按钮。无论配置文件中“在材料列表中显示材料偏好按钮”开启还是关闭，现象都一样。

### 已确认的代码原因

低版本 Litematica 的 `GuiSchematicLoad` 没有可注入的 `createButtons()`，RC1 兼容代码改为在 `initGui()` 尾部注入。当前 `GuiSchematicLoadMixin.lmlp$addPreferredReplacementButton(...)` 一开始就执行：

```java
DirectoryEntry entry = browser != null ? browser.getLastSelectedEntry() : null;
if (entry == null || FileType.fromFile(entry.getFullPath()) != FileType.LITEMATICA_SCHEMATIC) {
    return;
}
```

页面首次初始化时，文件浏览器通常还没有选中条目，因此 `entry == null`，方法提前返回，按钮不会被添加。之后用户选中原理图时，低版本页面不会再次调用 LMLP 的 `initGui()` 注入逻辑，所以按钮仍然不存在。

这与材料列表配置无关：按当前产品设计，加载原理图页面的入口应始终保留；配置只控制材料列表页面中的入口。

### 建议修复方案

1. 创建按钮时不要要求存在已选文件；只要浏览器和原版“材料列表”按钮已经创建，就始终添加“替换为偏好”按钮。
2. 把 `DirectoryEntry` 存在性、文件类型、可读性和 Litematica 解析检查全部留在按钮点击回调 `lmlp$openPreferredReplacement()` 中。
3. 未选文件时显示“未选择原理图”；选中了非 `.litematic` 文件、文件不可读或解析失败时显示对应错误，不要让初始化阶段静默隐藏入口。
4. 1.20.1、1.20.4、1.20.6 使用 `initGui()` 注入；较新版本保留其实际可用的 `createButtons()` 注入点，但共用“创建时不依赖选中条目”的逻辑。
5. 不读取材料缓存来决定该按钮是否显示，也不受 `showPreferredReplacementButtonInMaterialList` 配置影响。

### 预计涉及文件

- 各版本分支的 `GuiSchematicLoadMixin.java`：拆分按钮创建条件与点击时文件校验。
- 各版本语言文件：确认未选择、文件类型不支持、不可读和解析失败提示完整。
- 如需共用逻辑，抽取到 GUI 辅助类时必须保留旧版 `File`/新版 `Path` 适配，不改变原版加载按钮行为。

### 主要风险

1. 如果按钮在每次 `initGui()` 重建时没有随页面控件一起清理，可能产生重复按钮；应确认先由原版重置按钮列表，再添加一次。
2. 如果把选中文件检查完全删除而不放入点击回调，可能在空选择时抛出空指针；点击路径必须保留完整校验。
3. 如果错误地读取该配置，会违背“加载原理图页面始终保留入口”的需求。
4. 低版本和高版本的按钮列表类型、文件路径类型、原理图读取重载不同，需逐版本编译并检查 remap 日志。

### 验收场景

1. 在 1.20.1、1.20.4、1.20.6 打开“加载原理图”页面，尚未选择文件时也能看到“替换为偏好”按钮。
2. 选择 `.litematic` 文件后点击按钮，正常进入材料偏好界面。
3. 未选择文件点击按钮，显示明确提示且不崩溃。
4. 选择非原理图文件、损坏文件或删除后的路径，显示对应错误且不崩溃。
5. 开关“在材料列表中显示材料偏好按钮”不会改变加载原理图页面按钮的显示。
6. 实时材料列表和跨维度缓存材料列表的既有入口行为不回归。

## BUG-20260731-04：26.x 启动阶段创建冰 ItemStack 导致客户端崩溃

- 状态：已修复，待 RC2 实例验证
- 已确认版本：26.1.2、26.2
- 发现版本：1.9.4-RC1
- 修复版本：1.9.4-RC2
- 26.1.2 崩溃报告：`26.1.2-Fabric 0.19.3-Mod_Dev/crash-reports/crash-2026-07-31_17.38.52-client.txt`、`crash-2026-07-31_17.39.23-client.txt`
- 26.2 崩溃报告：`26.2-Fabric 0.19.3-Mod_Dev/crash-reports/crash-2026-07-31_17.40.42-client.txt`

### 现象

26.1.2 和 26.2 启动后在资源加载阶段立即崩溃，无法进入游戏；重复启动会重复生成崩溃报告。

### 日志证据

两版本的首个致命异常均为：

```text
java.lang.NullPointerException: Components not bound yet
    at net.minecraft.core.Holder$Reference.components(...)
    at net.minecraft.world.item.ItemStack.<init>(...)
    at io.github.huanmeng06.lmlp.material.WaterBucketIceSubstitution.iceStack(WaterBucketIceSubstitution.java:107)
    at io.github.huanmeng06.lmlp.material.WaterBucketIceSubstitution.refreshAvailableCounts(WaterBucketIceSubstitution.java:83)
    at io.github.huanmeng06.lmlp.material.InventoryCounts.captureAndPublish(InventoryCounts.java:44)
    at io.github.huanmeng06.lmlp.material.InventoryCounts.refresh(InventoryCounts.java:32)
    at io.github.huanmeng06.lmlp.InitHandler.lambda$registerModHandlers$2(InitHandler.java:39)
    at ...ClientTickEvents...onEndTick(...)
```

26.1.2 报告显示客户端只运行了 1 个 tick，且 `Last reload: Finished: No`；26.2 日志同样在资源重载未完成时报告相同堆栈。LMLP 本身已加载到 `1.9.4-RC1`，前面的可选 JEI、Carpet、Sodium 类缺失警告不是该崩溃的直接原因。

### 已确认的代码原因

`InitHandler` 无条件注册 `ClientTickEvents.END_CLIENT_TICK` 调用 `InventoryCounts.refresh()`。第一次 tick 可能发生在标题界面或初始资源重载期间，此时 `InventoryCounts.capture()` 因 `client.player == null` 返回空快照，但 `captureAndPublish()` 仍继续调用 `WaterBucketIceSubstitution.refreshAvailableCounts(...)`。

`refreshAvailableCounts()` 无条件调用 `iceStack()`；`iceStack()` 从 `BuiltInRegistries.ITEM` 取得冰物品后立即执行 `new ItemStack(item, 1)`。26.x 的 `ItemStack` 构造会读取物品 Holder 的 data components，而此时 Holder 尚未完成绑定，于是抛出 `Components not bound yet`，异常没有被捕获并终止客户端。

26.1.2 与 26.2 的 `WaterBucketIceSubstitution` 和 `InitHandler` 代码路径相同，因此应视为同一个跨版本启动时序 Bug，而不是两个独立崩溃。

### 建议修复方向

1. 启动阶段没有 `client.player` 或有效世界时，不运行库存计数刷新及冰替换可用数量回写；首次进入世界后再执行初始化刷新。
2. 在 `WaterBucketIceSubstitution.iceStack()` 增加版本兼容的失败保护，遇到组件尚未绑定时返回空结果并等待下一 tick，不能让生命周期回调把异常抛到 Minecraft 主循环。
3. 保持进入世界后的正常语义：库存为空也应能把已替换条目的可用数更新为 0，因此不能简单永久跳过所有空快照刷新；应区分“启动前无玩家”和“已进入世界但确实没有物品”。
4. 检查 `InventoryCounts.current()`、材料列表打开路径和断开/重连回调，确保它们在玩家尚未建立时也不会间接创建冰 `ItemStack`。
5. 同步修复并验证 26.1.2、26.2，再检查其他支持版本的库存刷新没有回归。

### 不应采用的修复

- 不要删除水桶替换为冰功能。
- 不要通过吞掉所有 `Throwable` 隐藏真实问题；保护应只覆盖组件尚未绑定/启动时机，日志仍需保留一次可诊断信息。
- 不要用未经注册的临时 `ItemStack` 或空堆栈参与 `InventoryCounts.countAny(...)`，否则可能把启动保护变成错误库存数据。

### 验收场景

1. 26.1.2 和 26.2 冷启动至少三次，均能到达主菜单并进入世界，日志不再出现 `Components not bound yet`。
2. 进入世界后开启“水桶替换为冰”，确认冰的可用数量和 6 个冰的统计仍正确。
3. 断开世界回到标题界面，再次进入另一个世界，不发生同类崩溃。
4. 随后检查 1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11 的库存刷新没有回归。

## BUG-20260801-01：重启后跨维度缓存无法恢复

- 状态：修复开发中（beta12）
- 严重性：中
- 已确认版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2、26.2
- 待确认版本：无
- 发现版本：1.9.4-RC2
- 修复版本：1.9.4-beta12（当前仅 `mc1.21.11`）
- 分支/提交：`mc1.20.1 / 3cd0296`
- 相关日志：`.../Minecraft/versions/1.20.1-Fabric 0.19.2-Mod_Dev/logs/latest.log`

### 现象

在主世界加载原理图并生成材料列表后进入末地，末地中可以看到主世界原理图的维度缓存。若在末地退出游戏并重新进入，主世界缓存不再出现在原理图列表中，必须先回到主世界后才能恢复。

期望结果是：缓存已经持久化且原理图未被用户删除时，即使从末地退出并在末地重新进入，也应直接恢复主世界的维度缓存；不应要求玩家先切回主世界。

### 复现步骤

1. 在主世界加载一个原理图并打开一次材料列表，使维度缓存产生。
2. 进入末地，在原理图列表中确认能看到并打开主世界的维度缓存。
3. 在末地退出游戏，再次启动并进入同一个世界，保持当前维度为末地。
4. 打开原理图列表或材料列表，观察主世界缓存是否仍然存在。
5. 回到主世界后再次观察，作为对照。

### 日志证据

### 用户报告与日志关联

- 用户报告摘要：从末地重启后主世界缓存消失，回主世界后恢复。
- 对应日志时间窗口：`02:07:05`、`02:07:51-02:07:54`、`02:09:01-02:09:04`。
- 关联结论：支持。日志显示运行期间缓存可保留，但重启进入末地时恢复被拒绝，之后缓存被写为空。

### 直接证据

- `latest.log:64-66`：加载的是 `litematica_material_list_plus 1.9.4-RC2+mc1.20.1`、Minecraft `1.20.1`。
- `latest.log:128-132`：进入末地时缓存文件读取成功（`restoredContexts=2`），但世界会话只恢复 `restoredDimensionContexts=0`、拒绝 `rejectedContexts=2`，随后 `retainedDimensionCaches=0`。
- `latest.log:230-234`：回到主世界加载原理图后生成 28 条材料缓存；切回末地时运行时上下文被保留为 `retainedDimensionCaches=1`。
- `latest.log:238-240`：运行期间能够刷新并打开主世界的持久化维度材料列表（`entries=28`）。
- `latest.log:351-355`：再次从末地进入时缓存文件仍被读取（`restoredContexts=1`），但恢复结果为 `restoredDimensionContexts=0`、`rejectedContexts=1`，并再次变成 `retainedDimensionCaches=0`。
- `latest.log:452-456`：随后缓存文件被写成 `entries=0`，打开材料列表时报告 `knownContextCount=0` 和 `no_placement_selected`。

### 代码证据

- `mc1.20.1/src/main/java/io/github/huanmeng06/lmlp/cache/ChunkMissingMaterialListCache.java:1377-1389`：新的世界会话先清理运行时上下文，再加载缓存，并只在原生投影索引同时确认记录且记录属于其他维度时进入恢复分支。
- `mc1.20.1/src/main/java/io/github/huanmeng06/lmlp/cache/ChunkMissingMaterialListCache.java:1394-1396`：未通过上述条件的缓存记录直接计入 `rejectedContexts`，不会进入内存中的维度上下文。
- `mc1.20.1/src/main/java/io/github/huanmeng06/lmlp/cache/NativePlacementStorageIndex.java:120-127`：`contains(...)` 要求维度文件名和 `PlacementIdentity` 都精确匹配；`isOtherDimension(...)` 在 `:151-156` 再检查记录维度与当前维度不同。
- `mc1.20.1/src/main/java/io/github/huanmeng06/lmlp/cache/ChunkMissingMaterialListCache.java:1452-1463`、`:1480-1490`：恢复失败后运行时上下文为空，后续持久化会把当前空上下文列表写回缓存文件，与日志中的 `entries=0` 一致。

### 无关或待排除信息

- 日志中的方块状态读取警告、JEI 初始化信息和系统更新提示与本次缓存恢复失败没有直接调用链关联，不作为根因。
- 日志没有记录 `NativePlacementStorageIndex.contains(...)` 具体是路径、投影名称、原点/区域身份还是加载时序不匹配，因此不能据此断言唯一的身份字段。

### 初步成因

- 置信度：高概率
- 触发事件：客户端重启后直接在末地建立世界会话，恢复主世界维度缓存。
- 调用链：`startWorldSession(...)` -> `WorldMaterialCacheIndex.load(...)` -> `NativePlacementStorageIndex.load(...)` -> `contains(...) && isOtherDimension(...)` 过滤 -> 记录被拒绝 -> 后续空上下文持久化。
- 失败状态：缓存文件本身可读，但恢复阶段依赖的原生投影身份校验未通过（或在校验时原生维度文件尚未处于可匹配状态），代码没有延期重试而是丢弃本次内存恢复结果。
- 用户可见结果：末地重启后看不到主世界维度缓存；回主世界重新加载原理图后，运行时上下文再次建立，所以缓存又出现。

当前日志足以确认“缓存读到后被拒绝并最终写为空”，但不足以区分“原生投影身份确实变化”和“恢复时序过早”两种具体原因，需后续补充原生维度文件及身份比对日志。

### 当前修复状态

- `1.9.4-beta12` 在 `dev-newFeature` 的 Minecraft `1.21.11` 分支中，为跨维度快照持久化“待原生索引确认”状态；重启时对其他维度的非空快照执行受限恢复，并继续在原生索引确认后切换为正常维度缓存。
- 旧版本已有非空维度快照没有该新字段时也会迁移到受限恢复路径；当前维度记录、空记录和显式删除记录不会因此变成缓存投影。
- 用户完成 beta12 验证后，再把同一逻辑适配到其余已确认版本并递增到下一 RC。

### 影响范围

- 受影响版本：1.20.1、1.20.4、1.20.6、1.21.1、1.21.10、1.21.11、26.1.2、26.2 均已确认。
- 已排除版本：无。
- 可能的回归风险：若为恢复时序问题，简单放宽身份校验可能恢复错误的旧投影；若为空列表直接持久化，可能覆盖仍然有效的维度缓存。

### 安全边界与不应采用的修复

- 不要删除维度缓存功能，也不要从材料条目重建缺失的完整原理图。
- 不要仅为通过本例而移除 `NativePlacementStorageIndex` 的身份校验，或把所有未匹配记录强行恢复到当前维度。
- 在恢复状态未确认前，不应把非空缓存无条件覆盖成空列表；应保留可诊断日志并考虑等待原生维度索引就绪后重试。

### 验收场景

1. 在 1.20.1 主世界建立一个有材料条目的原理图缓存，进入末地并退出游戏；重新进入时直接打开原理图列表，主世界缓存仍存在且材料条目数量不变。
2. 重复退出、重启和进入末地至少三次，不需要回主世界触发重新加载；缓存文件不会在未执行删除操作时从非零条目变成 `entries=0`。
3. 当前维度确实没有对应原生投影、原理图被用户删除或身份发生变化时，不恢复错误的缓存，并给出可诊断日志。
4. beta12 先在 1.21.11 验证；通过后将同一恢复逻辑适配到其余七个版本，并在下一 RC 中逐版本验收。

### 待补充信息

- 记录重启时对应的 Litematica 原生维度投影文件名、投影身份字段和缓存记录身份字段，以确定是身份不匹配还是加载顺序问题。
- 若修复引入延期恢复，需补充“原生索引就绪后重试且不覆盖缓存”的日志断言。
