param(
    [string] $InstanceDir = "",
    [string] $JavaHome = "",
    [int] $JavaRelease = 17
)

$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$DefaultMinecraftRoot = "E:\Minecraft\[MC]PCL\PCL2-1.20"

if ([string]::IsNullOrWhiteSpace($InstanceDir)) {
    $versionsDir = Join-Path $DefaultMinecraftRoot "versions"
    $candidate = Get-ChildItem -LiteralPath $versionsDir -Directory |
        Where-Object { $_.Name -like "*1.20.1-Fabric 0.16.9" } |
        Select-Object -First 1

    if ($null -eq $candidate) {
        throw "No default 1.20.1 Fabric 0.16.9 instance found under $versionsDir. Pass -InstanceDir explicitly."
    }

    $InstanceDir = $candidate.FullName
}

$Instance = (Resolve-Path -LiteralPath $InstanceDir).Path
$MinecraftRoot = (Resolve-Path -LiteralPath (Join-Path $Instance "..\..")).Path
$ClientJar = Get-ChildItem -LiteralPath (Join-Path $Instance ".fabric\remappedJars") -Recurse -Filter "client-intermediary.jar" | Select-Object -First 1

if ($null -eq $ClientJar) {
    throw "Unable to find Fabric client-intermediary.jar under $Instance"
}

$BuildDir = Join-Path $RepoRoot "build"
$ClassesDir = Join-Path $BuildDir "classes"
$ResourcesDir = Join-Path $BuildDir "resources"
$LibsDir = Join-Path $BuildDir "libs"

if (Test-Path -LiteralPath $BuildDir) {
    Remove-Item -LiteralPath $BuildDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $ClassesDir, $ResourcesDir, $LibsDir | Out-Null

$ClasspathJars = New-Object System.Collections.Generic.List[string]
$ClasspathJars.Add($ClientJar.FullName)

Get-ChildItem -LiteralPath (Join-Path $MinecraftRoot "libraries") -Recurse -Filter "*.jar" | ForEach-Object {
    $ClasspathJars.Add($_.FullName)
}

Get-ChildItem -LiteralPath (Join-Path $Instance "mods") -Filter "*.jar" | ForEach-Object {
    $ClasspathJars.Add($_.FullName)
}

$ProcessedModsDir = Join-Path $Instance ".fabric\processedMods"
if (Test-Path -LiteralPath $ProcessedModsDir) {
    Get-ChildItem -LiteralPath $ProcessedModsDir -Recurse -Filter "*.jar" | ForEach-Object {
        $ClasspathJars.Add($_.FullName)
    }
}

$SourceFiles = Get-ChildItem -LiteralPath (Join-Path $RepoRoot "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if ($SourceFiles.Count -eq 0) {
    throw "No Java source files found"
}

$Classpath = [string]::Join([IO.Path]::PathSeparator, ($ClasspathJars | ForEach-Object { $_.Replace('\', '/') }))
$ArgFile = Join-Path $BuildDir "javac.args"
$SourceArgs = $SourceFiles | ForEach-Object { "`"$($_.Replace('\', '/'))`"" }
$ClassesDirForJavac = $ClassesDir.Replace('\', '/')
$Javac = "javac"
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $Javac = Join-Path $JavaHome "bin\javac.exe"
    if (-not (Test-Path -LiteralPath $Javac)) {
        throw "Unable to find javac.exe under JavaHome: $JavaHome"
    }
}

$JavacArgs = @(
    "-encoding", "UTF-8",
    "--release", "$JavaRelease",
    "-classpath", "`"$Classpath`"",
    "-d", "`"$ClassesDirForJavac`""
) + $SourceArgs
[System.IO.File]::WriteAllLines($ArgFile, $JavacArgs, [System.Text.UTF8Encoding]::new($false))

& $Javac "@$ArgFile"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

Copy-Item -Path (Join-Path $RepoRoot "src\main\resources\*") -Destination $ResourcesDir -Recurse -Force

$ModMetadata = Get-Content -Raw -LiteralPath (Join-Path $RepoRoot "src\main\resources\fabric.mod.json") | ConvertFrom-Json
$JarFile = Join-Path $LibsDir "litematica-material-list-plus-$($ModMetadata.version).jar"
jar --create --file $JarFile -C $ClassesDir . -C $ResourcesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "Built $JarFile"
