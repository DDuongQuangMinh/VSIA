package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.client.screen.ServerRackScreen;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ServerRackNetwork {
    private static final String VERSION = "1";
    private static int nextId;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Vsia.MOD_ID, "server_rack"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ServerRackNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(nextId++, OpenRackPacket.class, OpenRackPacket::encode,
                OpenRackPacket::decode, OpenRackPacket::handle);
        CHANNEL.registerMessage(nextId++, SaveConfigPacket.class, SaveConfigPacket::encode,
                SaveConfigPacket::decode, SaveConfigPacket::handle);
    }

    public static void openFor(ServerPlayer player, ServerRackBlockEntity rack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenRackPacket(rack));
    }

    public record OpenRackPacket(BlockPos pos, String displayName, String ip, String subnet,
                                 String gateway, String dns, boolean dhcp,
                                 boolean http, boolean dnsService, boolean dhcpService,
                                 boolean mail) {
        OpenRackPacket(ServerRackBlockEntity rack) {
            this(rack.getBlockPos(), rack.displayName(), rack.ipAddress(), rack.subnetMask(),
                    rack.gatewayIp(), rack.dnsServer(), rack.usesDhcp(), rack.httpEnabled(),
                    rack.dnsEnabled(), rack.dhcpEnabled(), rack.mailEnabled());
        }

        static void encode(OpenRackPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos); b.writeUtf(p.displayName, 32); b.writeUtf(p.ip, 15);
            b.writeUtf(p.subnet, 15); b.writeUtf(p.gateway, 15); b.writeUtf(p.dns, 15);
            b.writeBoolean(p.dhcp); b.writeBoolean(p.http); b.writeBoolean(p.dnsService);
            b.writeBoolean(p.dhcpService); b.writeBoolean(p.mail);
        }

        static OpenRackPacket decode(FriendlyByteBuf b) {
            return new OpenRackPacket(b.readBlockPos(), b.readUtf(32), b.readUtf(15),
                    b.readUtf(15), b.readUtf(15), b.readUtf(15), b.readBoolean(),
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean());
        }

        static void handle(OpenRackPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> Minecraft.getInstance().setScreen(new ServerRackScreen(p))));
            context.setPacketHandled(true);
        }
    }

    public record SaveConfigPacket(BlockPos pos, String displayName, String ip, String subnet,
                                   String gateway, String dns, boolean dhcp,
                                   boolean http, boolean dnsService, boolean dhcpService,
                                   boolean mail) {
        public static void encode(SaveConfigPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos); b.writeUtf(p.displayName, 32); b.writeUtf(p.ip, 15);
            b.writeUtf(p.subnet, 15); b.writeUtf(p.gateway, 15); b.writeUtf(p.dns, 15);
            b.writeBoolean(p.dhcp); b.writeBoolean(p.http); b.writeBoolean(p.dnsService);
            b.writeBoolean(p.dhcpService); b.writeBoolean(p.mail);
        }

        static SaveConfigPacket decode(FriendlyByteBuf b) {
            return new SaveConfigPacket(b.readBlockPos(), b.readUtf(32), b.readUtf(15),
                    b.readUtf(15), b.readUtf(15), b.readUtf(15), b.readBoolean(),
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean());
        }

        static void handle(SaveConfigPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || player.distanceToSqr(p.pos.getX() + 0.5,
                        p.pos.getY() + 0.5, p.pos.getZ() + 0.5) > 64.0) return;
                if (player.level().getBlockEntity(p.pos) instanceof ServerRackBlockEntity rack) {
                    String error = rack.applyGuiConfiguration(p.displayName, p.ip, p.subnet,
                            p.gateway, p.dns, p.dhcp, p.http, p.dnsService,
                            p.dhcpService, p.mail);
                    if (error == null) player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Server configuration saved."), true);
                    else player.displayClientMessage(net.minecraft.network.chat.Component.literal(error), true);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
