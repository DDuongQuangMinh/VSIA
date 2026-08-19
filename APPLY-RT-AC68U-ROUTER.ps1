param(
    [string]$ProjectRoot = "."
)

$registryPath = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\SignalityBlocks.java"

if (-not (Test-Path $registryPath)) {
    throw "SignalityBlocks.java not found at $registryPath"
}

$text = [System.IO.File]::ReadAllText($registryPath)
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n", "`n").Replace("`r", "`n")

if (-not $text.Contains("RtAc68uRouterBlock")) {
    $importAnchor = "import com.k1ngtle.vsia.signality.internet.server.StorageServerItem;"

    if (-not $text.Contains($importAnchor)) {
        throw "Router install could not find the SignalityBlocks import anchor."
    }

    $imports = @'
import com.k1ngtle.vsia.signality.internet.server.StorageServerItem;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlock;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterItem;
'@

    $text = $text.Replace(
        $importAnchor,
        $imports
    )
}

if (-not $text.Contains("RT_AC68U_ROUTER_BE")) {
    $anchor = @'
    // --- Utilities & Cables ---
'@

    if (-not $text.Contains($anchor)) {
        throw "Router install could not find the Utilities & Cables registry anchor."
    }

    $registration = @'
    public static final RegistryObject<Block> RT_AC68U_ROUTER = BLOCKS.register(
            "rt_ac68u_router",
            () -> new RtAc68uRouterBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F, 6.0F)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> RT_AC68U_ROUTER_ITEM = ITEMS.register(
            "rt_ac68u_router",
            () -> new RtAc68uRouterItem(
                    RT_AC68U_ROUTER.get(),
                    new Item.Properties()
            )
    );

    public static final RegistryObject<BlockEntityType<RtAc68uRouterBlockEntity>> RT_AC68U_ROUTER_BE = BLOCK_ENTITIES.register(
            "rt_ac68u_router",
            () -> BlockEntityType.Builder.of(
                    RtAc68uRouterBlockEntity::new,
                    RT_AC68U_ROUTER.get()
            ).build(null)
    );

    // --- Utilities & Cables ---
'@

    $text = $text.Replace(
        $anchor,
        $registration
    )
}

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n", "`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $registryPath,
    $text,
    $utf8
)

$langPath = Join-Path $ProjectRoot "src\main\resources\assets\vsia\lang\en_us.json"

if (Test-Path $langPath) {
    try {
        $json = Get-Content $langPath -Raw | ConvertFrom-Json
        $json | Add-Member -NotePropertyName "block.vsia.rt_ac68u_router" -NotePropertyValue "RT-AC68U Router" -Force
        $json | Add-Member -NotePropertyName "item.vsia.rt_ac68u_router" -NotePropertyValue "RT-AC68U Router" -Force
        $json | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 $langPath
    } catch {
        Write-Warning "Could not patch en_us.json automatically. The router will still work."
    }
}

Write-Host "RT-AC68U Router registration installed."
Write-Host "Registry: $registryPath"
