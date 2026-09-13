package com.k1ngtle.vsia.signality.internet.radio.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

public record RadioGuiTarget(
        Kind kind,
        BlockPos blockPos
) {
    public enum Kind {
        BLOCK,
        MAIN_HAND,
        OFF_HAND
    }

    public RadioGuiTarget {
        if (kind == null) {
            throw new IllegalArgumentException("kind");
        }

        if (blockPos == null) {
            blockPos = BlockPos.ZERO;
        }
    }

    public static RadioGuiTarget block(BlockPos pos) {
        return new RadioGuiTarget(
                Kind.BLOCK,
                pos == null ? BlockPos.ZERO : pos.immutable()
        );
    }

    public static RadioGuiTarget hand(InteractionHand hand) {
        return new RadioGuiTarget(
                hand == InteractionHand.OFF_HAND
                        ? Kind.OFF_HAND
                        : Kind.MAIN_HAND,
                BlockPos.ZERO
        );
    }

    public InteractionHand hand() {
        return kind == Kind.OFF_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
    }

    public boolean isBlock() {
        return kind == Kind.BLOCK;
    }

    public boolean isHeld() {
        return !isBlock();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(kind);
        buffer.writeBlockPos(blockPos);
    }

    public static RadioGuiTarget decode(FriendlyByteBuf buffer) {
        return new RadioGuiTarget(
                buffer.readEnum(Kind.class),
                buffer.readBlockPos()
        );
    }
}
