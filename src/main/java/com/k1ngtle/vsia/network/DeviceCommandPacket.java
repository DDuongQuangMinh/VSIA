package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeviceCommandPacket {
    public final BlockPos pos;
    public final int deviceIndex;
    public final String[] commands;

    public DeviceCommandPacket(BlockPos pos, int deviceIndex, String... commands) {
        this.pos = pos;
        this.deviceIndex = deviceIndex;
        this.commands = commands;
    }

    public DeviceCommandPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.deviceIndex = buf.readInt();
        int count = buf.readInt();
        this.commands = new String[count];
        for (int i = 0; i < count; i++) {
            this.commands[i] = buf.readUtf(1024);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(deviceIndex);
        buf.writeInt(commands.length);
        for (String cmd : commands) {
            buf.writeUtf(cmd, 1024);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof NetworkSwitchBlockEntity sw && deviceIndex >= 0 && deviceIndex < 7) {
                    for (String cmd : commands) {
                        sw.osSimulators[deviceIndex].executeCliCore(cmd, false);
                    }
                    sw.setChanged();
                } else if (be instanceof FirewallBlockEntity fw && deviceIndex >= 0 && deviceIndex < 7) {
                    for (String cmd : commands) {
                        fw.osSimulators[deviceIndex].executeCliCore(cmd, false);
                    }
                    fw.setChanged();
                } else if (be instanceof RtAc68uRouterBlockEntity router) {
                    for (String cmd : commands) {
                        router.routerOs.executeCliCore(cmd, false);
                    }
                    router.applyRouterOsToLiveNetwork();

                    // W1.21 FULL V6.3 AUTHORITATIVE ROUTER CLI SYNC
                    router.w121SyncDiagnosticsToClients();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}