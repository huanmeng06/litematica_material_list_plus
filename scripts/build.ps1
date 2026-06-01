param(
    [string] $InstanceDir = ""
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
$NestedJarsDir = Join-Path $BuildDir "nested-jars"

if (Test-Path -LiteralPath $BuildDir) {
    Remove-Item -LiteralPath $BuildDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $ClassesDir, $ResourcesDir, $LibsDir | Out-Null
New-Item -ItemType Directory -Force -Path $NestedJarsDir | Out-Null

$ClasspathJars = New-Object System.Collections.Generic.List[string]
$ClasspathJars.Add($ClientJar.FullName)

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Add-ClasspathJar {
    param(
        [System.IO.FileInfo] $Jar
    )

    $ClasspathJars.Add($Jar.FullName)

    try {
        $zip = [IO.Compression.ZipFile]::OpenRead($Jar.FullName)
        try {
            foreach ($entry in $zip.Entries) {
                if ($entry.FullName.StartsWith("META-INF/jars/") -and $entry.FullName.EndsWith(".jar")) {
                    $nestedName = [IO.Path]::GetFileName($entry.FullName)
                    $safePrefix = ($Jar.BaseName -replace '[^A-Za-z0-9_.-]', '_')
                    $nestedPath = Join-Path $NestedJarsDir "$safePrefix-$nestedName"
                    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $nestedPath, $true)
                    $ClasspathJars.Add($nestedPath)
                }
            }
        } finally {
            $zip.Dispose()
        }
    } catch {
        Write-Warning "Unable to inspect nested jars in $($Jar.FullName): $($_.Exception.Message)"
    }
}

Get-ChildItem -LiteralPath (Join-Path $MinecraftRoot "libraries") -Recurse -Filter "*.jar" | ForEach-Object {
    Add-ClasspathJar $_
}

Get-ChildItem -LiteralPath (Join-Path $Instance "mods") -Filter "*.jar" | ForEach-Object {
    Add-ClasspathJar $_
}

$ProcessedModsDir = Join-Path $Instance ".fabric\processedMods"
if (Test-Path -LiteralPath $ProcessedModsDir) {
    Get-ChildItem -LiteralPath $ProcessedModsDir -Recurse -Filter "*.jar" | ForEach-Object {
        Add-ClasspathJar $_
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
@(
    "-encoding", "UTF-8",
    "-source", "17",
    "-target", "17",
    "-classpath", "`"$Classpath`"",
    "-d", "`"$ClassesDirForJavac`""
) + $SourceArgs | Set-Content -LiteralPath $ArgFile -Encoding Default

javac "@$ArgFile"
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
