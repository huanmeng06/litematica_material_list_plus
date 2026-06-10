param(
    [string] $PclRoot = "E:\Minecraft\[MC]PCL\PCL2-Mod_Dev",
    [string[]] $MinecraftVersions = @(
        "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
        "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5"
    )
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$OutputWidth = 240

function Find-ClientJar {
    param([string] $Version)

    $versionsDir = Join-Path $PclRoot "versions"
    if (-not (Test-Path -LiteralPath $versionsDir)) {
        return $null
    }

    Get-ChildItem -LiteralPath $versionsDir -Recurse -Filter "client-intermediary.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -like "*minecraft-$Version-*" } |
        Select-Object -First 1
}

function Find-MappingJar {
    param([string] $Version)

    $path = Join-Path $PclRoot "libraries\net\fabricmc\intermediary\$Version\intermediary-$Version.jar"
    if (Test-Path -LiteralPath $path) {
        return Get-Item -LiteralPath $path
    }

    return $null
}

function Find-InstanceDir {
    param([string] $Version)

    $versionsDir = Join-Path $PclRoot "versions"
    if (-not (Test-Path -LiteralPath $versionsDir)) {
        return $null
    }

    Get-ChildItem -LiteralPath $versionsDir -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "$Version-Fabric*" } |
        Sort-Object Name |
        Select-Object -First 1
}

function Find-ModName {
    param(
        [System.IO.DirectoryInfo] $InstanceDir,
        [string] $Pattern
    )

    if ($null -eq $InstanceDir) {
        return "-"
    }

    $modsDir = Join-Path $InstanceDir.FullName "mods"
    if (-not (Test-Path -LiteralPath $modsDir)) {
        return "-"
    }

    $mod = Get-ChildItem -LiteralPath $modsDir -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match $Pattern -and $_.Name -notmatch "\.disabled$" } |
        Sort-Object Name |
        Select-Object -First 1

    if ($null -eq $mod) {
        return "-"
    }

    return $mod.Name
}

function Invoke-Javap {
    param(
        [string] $Classpath,
        [string] $ClassName
    )

    $output = & javap -classpath $Classpath -p -s $ClassName 2>$null
    if ($LASTEXITCODE -ne 0) {
        return @()
    }

    return $output
}

function Get-ClassMajor {
    param(
        [string] $Classpath,
        [string] $ClassName
    )

    $output = & javap -verbose -classpath $Classpath $ClassName 2>$null
    if ($LASTEXITCODE -ne 0) {
        return "-"
    }

    $line = $output | Select-String -Pattern "major version" | Select-Object -First 1
    if ($null -eq $line) {
        return "-"
    }

    return ($line.ToString() -replace "[^0-9]", "")
}

function Has-Line {
    param(
        [string[]] $Lines,
        [string] $Pattern
    )

    $match = $Lines | Select-String -Pattern $Pattern | Select-Object -First 1
    return $null -ne $match
}

function First-Line {
    param(
        [string[]] $Lines,
        [string] $Pattern
    )

    $match = $Lines | Select-String -Pattern $Pattern | Select-Object -First 1
    if ($null -eq $match) {
        return "-"
    }

    return $match.ToString().Trim()
}

function Java-FromMajor {
    param([string] $Major)

    switch ($Major) {
        "61" { return "17" }
        "65" { return "21" }
        default { return "-" }
    }
}

$rows = @()
$apiRows = @()
$modRows = @()

foreach ($version in $MinecraftVersions) {
    $clientJar = Find-ClientJar -Version $version
    $mappingJar = Find-MappingJar -Version $version
    $instanceDir = Find-InstanceDir -Version $version

    if ($null -eq $clientJar) {
        $rows += [PSCustomObject]@{
            MC = $version
            ClientJar = "missing"
            MappingJar = if ($null -eq $mappingJar) { "missing" } else { "present" }
            Major = "-"
            Java = "-"
        }
        continue
    }

    $major = Get-ClassMajor -Classpath $clientJar.FullName -ClassName "net.minecraft.class_310"
    $identifier = Invoke-Javap -Classpath $clientJar.FullName -ClassName "net.minecraft.class_2960"
    $drawContext = Invoke-Javap -Classpath $clientJar.FullName -ClassName "net.minecraft.class_332"
    $clientWorld = Invoke-Javap -Classpath $clientJar.FullName -ClassName "net.minecraft.class_638"
    $world = Invoke-Javap -Classpath $clientJar.FullName -ClassName "net.minecraft.class_1937"
    $itemStack = Invoke-Javap -Classpath $clientJar.FullName -ClassName "net.minecraft.class_1799"

    $rows += [PSCustomObject]@{
        MC = $version
        ClientJar = "present"
        MappingJar = if ($null -eq $mappingJar) { "missing" } else { "present" }
        Major = $major
        Java = Java-FromMajor -Major $major
    }

    $apiRows += [PSCustomObject]@{
        MC = $version
        IdentifierCtor = if (Has-Line $identifier "public net.minecraft.class_2960\(java.lang.String, java.lang.String\);") { "public" } else { "not public" }
        IdentifierFactory = First-Line $identifier "public static net.minecraft.class_2960 method_60655"
        DrawTexture = First-Line $drawContext "method_25290"
        DrawItem = if (Has-Line $drawContext "method_51427\(net.minecraft.class_1799, int, int\)") { "present" } else { "missing" }
        ClientWorldChunkLoaded = if (Has-Line $clientWorld "method_8393\(int, int\)") { "present" } else { "missing" }
        WorldDimension = if (Has-Line $world "method_27983") { "present" } else { "missing" }
        ItemStackCore = if (
            (Has-Line $itemStack "field_8037") -and
            (Has-Line $itemStack "method_7960") -and
            (Has-Line $itemStack "method_7909") -and
            (Has-Line $itemStack "method_7914") -and
            (Has-Line $itemStack "method_7947") -and
            (Has-Line $itemStack "method_7972")
        ) { "present" } else { "missing" }
    }

    $modRows += [PSCustomObject]@{
        MC = $version
        Instance = if ($null -eq $instanceDir) { "-" } else { $instanceDir.Name }
        FabricApi = Find-ModName -InstanceDir $instanceDir -Pattern "fabric-api-.*\.jar$"
        Litematica = Find-ModName -InstanceDir $instanceDir -Pattern "litematica.*\.jar$"
        MaLiLib = Find-ModName -InstanceDir $instanceDir -Pattern "malilib.*\.jar$"
        Rei = Find-ModName -InstanceDir $instanceDir -Pattern "RoughlyEnoughItems.*\.jar$"
        ModMenu = Find-ModName -InstanceDir $instanceDir -Pattern "modmenu.*\.jar$"
    }
}

Write-Host ""
Write-Host "== Client jars and Java target =="
($rows | Format-Table -AutoSize | Out-String -Width $OutputWidth).TrimEnd()

Write-Host ""
Write-Host "== Minecraft API surface used by LMLP =="
($apiRows | Format-List | Out-String -Width $OutputWidth).TrimEnd()

Write-Host ""
Write-Host "== Instance mod dependencies =="
($modRows | Format-List | Out-String -Width $OutputWidth).TrimEnd()

$maliRows = @()
foreach ($row in $modRows) {
    if ($row.Instance -eq "-") {
        continue
    }

    $instanceDir = Join-Path (Join-Path $PclRoot "versions") $row.Instance
    $mali = Get-ChildItem -LiteralPath (Join-Path $instanceDir "mods") -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "malilib.*\.jar$" -and $_.Name -notmatch "\.disabled$" } |
        Select-Object -First 1

    if ($null -eq $mali) {
        continue
    }

    $lines = Invoke-Javap -Classpath $mali.FullName -ClassName "fi.dy.masa.malilib.gui.widgets.WidgetListBase"
    $maliRows += [PSCustomObject]@{
        MC = $row.MC
        MaLiLib = $mali.Name
        OnMouseScrolled = First-Line $lines "onMouseScrolled"
        DrawContents = First-Line $lines "drawContents"
    }
}

Write-Host ""
Write-Host "== MaLiLib WidgetListBase signatures =="
($maliRows | Format-List | Out-String -Width $OutputWidth).TrimEnd()
