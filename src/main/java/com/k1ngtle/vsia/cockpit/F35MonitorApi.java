package com.k1ngtle.vsia.cockpit;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class F35MonitorApi {
    private F35MonitorApi() {
    }

    public static boolean edit(
            Level level,
            BlockPos pos,
            Consumer<CcTerminalBuffer> operation
    ) {
        if (level == null
                || pos == null
                || operation == null) {
            return false;
        }

        if (!(level.getBlockEntity(
                pos
        ) instanceof F35CockpitSeatBlockEntity cockpit)) {
            return false;
        }

        cockpit.setDemoMode(
                false
        );

        operation.accept(
                cockpit.terminal()
        );

        cockpit.sync();

        return true;
    }

    public static boolean clear(
            Level level,
            BlockPos pos
    ) {
        return edit(
                level,
                pos,
                CcTerminalBuffer::clear
        );
    }

    public static boolean setDemoMode(
            Level level,
            BlockPos pos,
            boolean demo
    ) {
        if (level == null
                || pos == null) {
            return false;
        }

        if (!(level.getBlockEntity(
                pos
        ) instanceof F35CockpitSeatBlockEntity cockpit)) {
            return false;
        }

        cockpit.setDemoMode(
                demo
        );

        return true;
    }
}
