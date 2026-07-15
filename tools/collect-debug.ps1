param(
    [Parameter(Mandatory = $true)]
    [string]$Instance,
    [string]$Output
)

$ErrorActionPreference = "Stop"
$instancePath = (Resolve-Path $Instance).Path
$stamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
if (-not $Output) {
    $Output = Join-Path (Get-Location) "lmlp-debug-$stamp.zip"
}

$temp = Join-Path ([System.IO.Path]::GetTempPath()) "lmlp-debug-$stamp"
New-Item -ItemType Directory -Force -Path $temp | Out-Null
try {
    $files = @(
        "logs/latest.log",
        "config/litematica_material_list_plus.json",
        "config/litematica/litematica_material_list_plus.json"
    )
    foreach ($relative in $files) {
        $source = Join-Path $instancePath $relative
        if (Test-Path $source) {
            $target = Join-Path $temp $relative
            New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
            Copy-Item $source $target
        }
    }

    $crashDir = Join-Path $instancePath "crash-reports"
    if (Test-Path $crashDir) {
        $targetDir = Join-Path $temp "crash-reports"
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
        Get-ChildItem $crashDir -Filter "*.txt" |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 3 |
            Copy-Item -Destination $targetDir
    }

    $modsDir = Join-Path $instancePath "mods"
    if (Test-Path $modsDir) {
        Get-ChildItem $modsDir -File | Sort-Object Name | Select-Object -ExpandProperty Name |
            Set-Content -Encoding UTF8 (Join-Path $temp "mod-list.txt")
    }

    [ordered]@{
        createdAt = $stamp
        system = [System.Environment]::OSVersion.VersionString
        instanceName = Split-Path $instancePath -Leaf
    } | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $temp "manifest.json")

    Compress-Archive -Path (Join-Path $temp "*") -DestinationPath $Output -Force
    Write-Host "调试包：$Output"
}
finally {
    Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
}
