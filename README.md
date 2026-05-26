# Litematica Material List Plus

Fabric client mod for Minecraft 1.20.1 that enhances Litematica's material list UI.

## v1 features

- Shows grouped counts in the material list, for example `85 = 1x64+21`.
- Keeps Litematica's original material calculation and file export behavior intact.
- Click a material row to expand recipe summaries inline.
- Shift-click a material row to open a dedicated recipe detail screen.
- Uses REI recipes when Roughly Enough Items is installed.

## Build

This repository includes a local build script that compiles against the existing Minecraft instance jars.

```powershell
.\scripts\build.ps1
```

The default instance path is:

`E:\Minecraft\[MC]PCL\PCL2-1.20\versions\[诶就自己玩儿]1.20.1-Fabric 0.16.9`

You can override it:

```powershell
.\scripts\build.ps1 -InstanceDir "D:\path\to\your\instance"
```
