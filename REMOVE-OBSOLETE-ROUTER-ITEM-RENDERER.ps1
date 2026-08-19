param(
    [string]$ProjectRoot = "."
)

$paths = @(
    "src\main\java\com\k1ngtle\vsia\client\model\RtAc68uRouterItemModel.java",
    "src\main\java\com\k1ngtle\vsia\client\renderer\RtAc68uRouterItemRenderer.java"
)

foreach ($relative in $paths) {
    $path = Join-Path $ProjectRoot $relative
    if (Test-Path $path) {
        Remove-Item $path -Force
        Write-Host "Removed $relative"
    }
}

$required = @(
    "src\main\resources\assets\vsia\geo\block\rt_ac68u_router.geo.json",
    "src\main\resources\assets\vsia\animations\block\rt_ac68u_router.animation.json",
    "src\main\resources\assets\vsia\textures\block\rt_ac68u_router.png"
)

foreach ($relative in $required) {
    $path = Join-Path $ProjectRoot $relative
    if (-not (Test-Path $path)) {
        throw "Missing required router asset: $relative"
    }
    Write-Host "OK $relative"
}

Write-Host "RT-AC68U inventory render hotfix applied."
