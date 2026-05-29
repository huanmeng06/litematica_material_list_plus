# Litematica Material List Plus - dev-newFeature

这个分支用于记录 Minecraft `1.20.6` 开发过程、测试构建和提交更新。

完整项目介绍、安装说明、功能说明和长期维护文档以后放在 `main` 分支 README 中；各 Minecraft 版本分支只保留必要的版本状态和开发日志，避免多处分散维护同一份长文档。

当前正式版本：`v1.3.0`

当前构建：`1.3.0+mc1.20.6`

适配目标：Minecraft `1.20.6` / Fabric / Litematica / MaLiLib / REI

## v1.3.0 更新摘要

本次正式版基于 `1.2.25+mc1.20.6` 开发构建整理发布。

### 新功能

- 材料列表支持从容器界面打开，并尽量保留原加工界面上下文。
- 新增完整配方详情页，支持 `Shift + 单击`材料行打开。
- 配方详情页复用 REI 原生配方布局、物品交互、tooltip 和转移按钮逻辑。
- 配方详情页支持返回原加工界面，并能恢复上次详情页状态。
- 悬停浮窗模式扩展为三档：LMLP、Litematica 原版、不显示悬停浮窗。
- 摘要页和详情页的嵌套材料展开/收起加入短动画。

### 修复与优化

- 修复容器界面中材料列表热键、返回、搜索框输入等交互冲突。
- 修复配方详情页在单人世界中暂停游戏的问题。
- 修复 REI 转移按钮 tooltip、配方 ID、本地化文本和收藏提示过滤相关问题。
- 修复详情页展开动画中内容框边框线缺失的问题。
- 修复摘要页递归子材料动画中第二层及更深层级被错误裁切的问题。
- 优化配方详情页标题、材料概览框和详情框布局。
- 补充中文、英文、香港繁体中文语言文件并保持 key 对齐。

## 开发日志

### v1.3.0

- `c020e11` Animate material row recipe panel
- `27a536a` Animate inline recipe expansion
- `749a485` Animate recipe detail expansion
- `53408dd` Add hidden hover tooltip mode
- `52af1ff` Align recipe detail layout
- `22c9a58` Add recipe detail mod title
- `a4ab1ea` Keep world running on recipe details
- `afa7f68` Restore recipe detail hotkey state
- `773bf9c` Limit material list hotkey closing to recipe details
- `58d5df0` Improve material list hotkey toggling
- `7bb5591` Revert "Fix material list hotkey close"
- `dbfaca4` Fix material list hotkey close
- `e031519` Toggle material list from container screens
- `46608fa` Shorten recipe stop config tooltip
- `eb58b51` Fix config option name translations
- `d1ffe66` Add Hong Kong Chinese language
- `f18a858` Improve displayed text localization
- `9c6e841` Use REI gray for transfer recipe id
- `0805866` Fix transfer tooltip recipe id text
- `c21213a` Avoid unknown REI tooltip components
- `1a4ef68` Render REI transfer tooltips in detail screen
- `843f865` Match REI transfer button behavior
- `5b5a666` Use REI button for recipe transfer
- `b632ab1` Add REI transfer button to recipe details
- `02d36eb` Preserve handled screen for material list
- `33c6182` Keep material list over handled screens
- `a7d79ce` Open material list from handled screens
- `6cb0e9d` Update README.md
- `eb2a963` Add screenshots; remove SVG placeholders
- `fd2fca6` Update project README

## 本地构建

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 `
  -InstanceDir "E:\Minecraft\[MC]PCL\PCL2-Mod_Dev\versions\1.20.6-Fabric 0.19.2-Mod_Dev" `
  -JavaHome "C:\Users\Huan_meeng\AppData\Roaming\.minecraft\runtime\java-runtime-delta" `
  -JavaRelease 21
```
