package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class IPhoneMapsScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF66666C;
    private static final int BLUE = 0xFF0A84FF;

    private int mapX;
    private int mapY;
    private int mapW;
    private int mapH;

    public IPhoneMapsScreen() {
        super(Component.literal("Maps"));
    }

    @Override
    protected void init() {
        super.init();
        mapX = phoneX + 14;
        mapY = phoneY + 78;
        mapW = PHONE_WIDTH - 28;
        mapH = 230;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Maps", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        Minecraft mc = Minecraft.getInstance();

        roundedRect(g, mapX, mapY, mapW, mapH, 14, 0xFFD7E7D2);

        if (mc.level != null && mc.player != null) {
            int centerX = mc.player.blockPosition().getX();
            int centerZ = mc.player.blockPosition().getZ();

            int samples = 13;
            int cell = 14;
            int drawW = samples * cell;
            int startX = mapX + (mapW - drawW) / 2;
            int startY = mapY + 17;

            int baseHeight = mc.level.getHeight(
                    Heightmap.Types.WORLD_SURFACE,
                    centerX,
                    centerZ
            );

            for (int gz = 0; gz < samples; gz++) {
                for (int gx = 0; gx < samples; gx++) {
                    int worldX = centerX + (gx - samples / 2) * 4;
                    int worldZ = centerZ + (gz - samples / 2) * 4;

                    int height = mc.level.getHeight(
                            Heightmap.Types.WORLD_SURFACE,
                            worldX,
                            worldZ
                    );

                    BlockPos top = new BlockPos(
                            worldX,
                            Math.max(mc.level.getMinBuildHeight(), height - 1),
                            worldZ
                    );

                    boolean water = mc.level.getBlockState(top).is(Blocks.WATER);
                    int color = water
                            ? 0xFF67A9D8
                            : terrainColor(height - baseHeight);

                    int px = startX + gx * cell;
                    int py = startY + gz * cell;
                    g.fill(px, py, px + cell, py + cell, color);
                }
            }

            int cx = startX + samples / 2 * cell + cell / 2;
            int cy = startY + samples / 2 * cell + cell / 2;

            roundedRect(g, cx - 4, cy - 4, 8, 8, 4, BLUE);
            roundedRect(g, cx - 2, cy - 2, 4, 4, 2, 0xFFFFFFFF);

            drawUiCentered(
                    g,
                    centerX + ", " + mc.player.blockPosition().getY() + ", " + centerZ,
                    phoneX + PHONE_WIDTH / 2,
                    mapY + mapH - 29,
                    TEXT
            );

            drawUiCentered(
                    g,
                    mc.level.dimension().location().toString(),
                    phoneX + PHONE_WIDTH / 2,
                    mapY + mapH - 14,
                    MUTED
            );
        } else {
            drawUiCentered(g, "Map unavailable", phoneX + PHONE_WIDTH / 2, mapY + 100, MUTED);
        }

        drawUiWrappedCentered(
                g,
                "Live top-down terrain samples around your current player position.",
                phoneX + PHONE_WIDTH / 2,
                mapY + mapH + 20,
                mapW - 18,
                11,
                3,
                MUTED
        );

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private static int terrainColor(int deltaHeight) {
        if (deltaHeight >= 18) return 0xFF7C806C;
        if (deltaHeight >= 8) return 0xFF8DA36C;
        if (deltaHeight <= -8) return 0xFFB8D8A0;
        return 0xFF91C77B;
    }
}
