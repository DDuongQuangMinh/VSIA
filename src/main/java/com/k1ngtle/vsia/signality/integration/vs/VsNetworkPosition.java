package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.signality.engineering.channel.VsWorldPoseResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class VsNetworkPosition {
    private VsNetworkPosition() {
    }

    public static Vec3 blockCenterWorld(
            ServerLevel level,
            BlockPos blockPos
    ) {
        if (blockPos == null) {
            return Vec3.ZERO;
        }

        Vec3 local =
                Vec3.atCenterOf(
                        blockPos
                );

        if (level == null) {
            return local;
        }

        Vec3 world =
                VsWorldPoseResolver.toWorld(
                        level,
                        local
                );

        return world == null
                ? local
                : world;
    }

    public static Vec3 blockEntityWorld(
            BlockEntity blockEntity
    ) {
        if (blockEntity == null
                || !(blockEntity.getLevel()
                instanceof ServerLevel level)) {
            return blockEntity == null
                    ? Vec3.ZERO
                    : Vec3.atCenterOf(
                    blockEntity.getBlockPos()
            );
        }

        return blockCenterWorld(
                level,
                blockEntity.getBlockPos()
        );
    }

    public static boolean onVsShip(
            ServerLevel level,
            BlockPos blockPos
    ) {
        if (level == null
                || blockPos == null
                || !VsCompat.isLoaded()) {
            return false;
        }

        return VsCompat.hook()
                .shipManagingPos(
                        level,
                        blockPos
                )
                != null;
    }

    public static String describe(
            ServerLevel level,
            BlockPos blockPos
    ) {
        Vec3 world =
                blockCenterWorld(
                        level,
                        blockPos
                );

        return String.format(
                Locale.ROOT,
                "%s | world %.2f %.2f %.2f%s",
                blockPos == null
                        ? "unknown"
                        : blockPos.toShortString(),
                world.x,
                world.y,
                world.z,
                onVsShip(
                        level,
                        blockPos
                )
                        ? " | VS SHIP"
                        : ""
        );
    }
}
