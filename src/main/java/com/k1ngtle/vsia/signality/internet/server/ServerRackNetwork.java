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
    private static final String VERSION = "4";
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
        CHANNEL.registerMessage(nextId++, DesktopToolPacket.class, DesktopToolPacket::encode,
                DesktopToolPacket::decode, DesktopToolPacket::handle);
        CHANNEL.registerMessage(nextId++, DesktopResultPacket.class, DesktopResultPacket::encode,
                DesktopResultPacket::decode, DesktopResultPacket::handle);
        CHANNEL.registerMessage(nextId++, ProgramPacket.class, ProgramPacket::encode,
                ProgramPacket::decode, ProgramPacket::handle);
        CHANNEL.registerMessage(nextId++, ProgramResultPacket.class, ProgramResultPacket::encode,
                ProgramResultPacket::decode, ProgramResultPacket::handle);
        CHANNEL.registerMessage(nextId++, DnsRecordPacket.class, DnsRecordPacket::encode,DnsRecordPacket::decode,DnsRecordPacket::handle);
        CHANNEL.registerMessage(nextId++, DnsResultPacket.class, DnsResultPacket::encode,DnsResultPacket::decode,DnsResultPacket::handle);
        CHANNEL.registerMessage(nextId++, DhcpPoolPacket.class,DhcpPoolPacket::encode,DhcpPoolPacket::decode,DhcpPoolPacket::handle);
        CHANNEL.registerMessage(nextId++, DhcpResultPacket.class,DhcpResultPacket::encode,DhcpResultPacket::decode,DhcpResultPacket::handle);
        CHANNEL.registerMessage(nextId++, NtpConfigPacket.class,NtpConfigPacket::encode,NtpConfigPacket::decode,NtpConfigPacket::handle);
        CHANNEL.registerMessage(nextId++, NtpResultPacket.class,NtpResultPacket::encode,NtpResultPacket::decode,NtpResultPacket::handle);
    }

    public static void openFor(ServerPlayer player, ServerRackBlockEntity rack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenRackPacket(rack));
    }

    public record OpenRackPacket(BlockPos pos, String displayName, String ip, String subnet,
                                 String gateway, String dns, boolean dhcp,
                                 boolean http, boolean dnsService, boolean dhcpService,
                                 boolean mail, String ipv6, int ipv6Prefix, String gateway6, String dns6,
                                 boolean automatic6, long deviceTime, long clockOffset, String ptpMode, String ptpProfile,
                                 long serviceMask, String dnsRecordData,String dhcp4Data,String dhcp6Data,
                                 boolean ntpServer,boolean ntpClient,int ntpStratum,int ntpPoll,String ntpSource,int clockDrift,long lastNtpSync,String clockStatus) {
        OpenRackPacket(ServerRackBlockEntity rack) {
            this(rack.getBlockPos(), rack.displayName(), rack.ipAddress(), rack.subnetMask(),
                    rack.gatewayIp(), rack.dnsServer(), rack.usesDhcp(), rack.httpEnabled(),
                    rack.dnsEnabled(), rack.dhcpEnabled(), rack.mailEnabled(),rack.ipv6Address(),rack.ipv6PrefixLength(),rack.ipv6Gateway(),rack.ipv6DnsServer(),rack.ipv6Automatic(),rack.deviceTimeMillis(),rack.clockOffsetMillis(),rack.ptpMode().name(),rack.ptpProfile().name(),rack.serviceMask(),rack.dnsRecordData(),rack.dhcpData(false),rack.dhcpData(true),rack.ntpServerEnabled(),rack.ntpClientEnabled(),rack.ntpStratum(),rack.ntpPollSeconds(),rack.ntpSourceIp(),rack.clockDriftPpm(),rack.lastNtpSyncMillis(),rack.clockSyncStatus());
        }

        static void encode(OpenRackPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos); b.writeUtf(p.displayName, 32); b.writeUtf(p.ip, 15);
            b.writeUtf(p.subnet, 15); b.writeUtf(p.gateway, 15); b.writeUtf(p.dns, 15);
            b.writeBoolean(p.dhcp); b.writeBoolean(p.http); b.writeBoolean(p.dnsService);
            b.writeBoolean(p.dhcpService); b.writeBoolean(p.mail);
            b.writeUtf(p.ipv6,45);b.writeVarInt(p.ipv6Prefix);b.writeUtf(p.gateway6,45);b.writeUtf(p.dns6,45);b.writeBoolean(p.automatic6);b.writeLong(p.deviceTime);b.writeLong(p.clockOffset);b.writeUtf(p.ptpMode,16);b.writeUtf(p.ptpProfile,16);
            b.writeLong(p.serviceMask);
            b.writeUtf(p.dnsRecordData,16384);
            b.writeUtf(p.dhcp4Data,16384);b.writeUtf(p.dhcp6Data,16384);
            b.writeBoolean(p.ntpServer);b.writeBoolean(p.ntpClient);b.writeVarInt(p.ntpStratum);b.writeVarInt(p.ntpPoll);b.writeUtf(p.ntpSource,45);b.writeInt(p.clockDrift);b.writeLong(p.lastNtpSync);b.writeUtf(p.clockStatus,256);
        }

        static OpenRackPacket decode(FriendlyByteBuf b) {
            return new OpenRackPacket(b.readBlockPos(), b.readUtf(32), b.readUtf(15),
                    b.readUtf(15), b.readUtf(15), b.readUtf(15), b.readBoolean(),
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(),b.readUtf(45),b.readVarInt(),b.readUtf(45),b.readUtf(45),b.readBoolean(),b.readLong(),b.readLong(),b.readUtf(16),b.readUtf(16),b.readLong(),b.readUtf(16384),b.readUtf(16384),b.readUtf(16384),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readUtf(45),b.readInt(),b.readLong(),b.readUtf(256));
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
                                   boolean mail, String ipv6, int ipv6Prefix, String gateway6, String dns6,
                                   boolean automatic6, long clockOffset, String ptpMode, String ptpProfile,
                                   long serviceMask) {
        public static void encode(SaveConfigPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos); b.writeUtf(p.displayName, 32); b.writeUtf(p.ip, 15);
            b.writeUtf(p.subnet, 15); b.writeUtf(p.gateway, 15); b.writeUtf(p.dns, 15);
            b.writeBoolean(p.dhcp); b.writeBoolean(p.http); b.writeBoolean(p.dnsService);
            b.writeBoolean(p.dhcpService); b.writeBoolean(p.mail);
            b.writeUtf(p.ipv6,45);b.writeVarInt(p.ipv6Prefix);b.writeUtf(p.gateway6,45);b.writeUtf(p.dns6,45);b.writeBoolean(p.automatic6);b.writeLong(p.clockOffset);b.writeUtf(p.ptpMode,16);b.writeUtf(p.ptpProfile,16);
            b.writeLong(p.serviceMask);
        }

        static SaveConfigPacket decode(FriendlyByteBuf b) {
            return new SaveConfigPacket(b.readBlockPos(), b.readUtf(32), b.readUtf(15),
                    b.readUtf(15), b.readUtf(15), b.readUtf(15), b.readBoolean(),
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(),b.readUtf(45),b.readVarInt(),b.readUtf(45),b.readUtf(45),b.readBoolean(),b.readLong(),b.readUtf(16),b.readUtf(16),b.readLong());
        }

        static void handle(SaveConfigPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || player.distanceToSqr(p.pos.getX() + 0.5,
                        p.pos.getY() + 0.5, p.pos.getZ() + 0.5) > 64.0) return;
                if (player.level().getBlockEntity(p.pos) instanceof ServerRackBlockEntity rack) {
                    String error = rack.applyGuiConfiguration(p.displayName, p.ip, p.subnet,
                            p.gateway, p.dns,p.ipv6,p.ipv6Prefix,p.gateway6,p.dns6,p.automatic6,p.clockOffset,p.ptpMode,p.ptpProfile, p.dhcp, p.http, p.dnsService,
                            p.dhcpService, p.mail);
                    if (error == null) player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Server configuration saved."), true);
                    else player.displayClientMessage(net.minecraft.network.chat.Component.literal(error), true);
                    if(error==null)rack.applyServiceMask(p.serviceMask);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record DesktopToolPacket(BlockPos pos,String tool,String input){
        public static void encode(DesktopToolPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.tool,32);b.writeUtf(p.input,256);}
        static DesktopToolPacket decode(FriendlyByteBuf b){return new DesktopToolPacket(b.readBlockPos(),b.readUtf(32),b.readUtf(256));}
        static void handle(DesktopToolPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){String result=rack.executeDesktopTool(p.tool,p.input,player.serverLevel());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DesktopResultPacket(p.tool,result));}});c.setPacketHandled(true);}
    }
    public record DesktopResultPacket(String tool,String result){
        static void encode(DesktopResultPacket p,FriendlyByteBuf b){b.writeUtf(p.tool,32);b.writeUtf(p.result,8192);}
        static DesktopResultPacket decode(FriendlyByteBuf b){return new DesktopResultPacket(b.readUtf(32),b.readUtf(8192));}
        static void handle(DesktopResultPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptDesktopResult(p.tool,p.result)));c.setPacketHandled(true);}
    }
    public record ProgramPacket(BlockPos pos,String action,String source){
        public static void encode(ProgramPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,8);b.writeUtf(p.source,16384);}
        static ProgramPacket decode(FriendlyByteBuf b){return new ProgramPacket(b.readBlockPos(),b.readUtf(8),b.readUtf(16384));}
        static void handle(ProgramPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){String result;String source=p.source;if(p.action.equals("open")){source=rack.programSource();result=rack.programOutput();}else if(p.action.equals("save"))result=rack.saveProgram(source);else result=rack.runProgram(source,player.serverLevel());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ProgramResultPacket(source,result));}});c.setPacketHandled(true);}
    }
    public record ProgramResultPacket(String source,String result){
        static void encode(ProgramResultPacket p,FriendlyByteBuf b){b.writeUtf(p.source,16384);b.writeUtf(p.result,16384);}
        static ProgramResultPacket decode(FriendlyByteBuf b){return new ProgramResultPacket(b.readUtf(16384),b.readUtf(16384));}
        static void handle(ProgramResultPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptProgramResult(p.source,p.result)));c.setPacketHandled(true);}
    }
    public record DnsRecordPacket(BlockPos pos,String action,String name,String type,String detail,int ttl){
        public static void encode(DnsRecordPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.name,253);b.writeUtf(p.type,8);b.writeUtf(p.detail,253);b.writeVarInt(p.ttl);}
        static DnsRecordPacket decode(FriendlyByteBuf b){return new DnsRecordPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(253),b.readUtf(8),b.readUtf(253),b.readVarInt());}
        static void handle(DnsRecordPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){String message=rack.manageDnsRecord(p.action,p.name,p.type,p.detail,p.ttl);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DnsResultPacket(message,rack.dnsRecordData()));}});c.setPacketHandled(true);}
    }
    public record DnsResultPacket(String message,String records){
        static void encode(DnsResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.records,16384);}
        static DnsResultPacket decode(FriendlyByteBuf b){return new DnsResultPacket(b.readUtf(256),b.readUtf(16384));}
        static void handle(DnsResultPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptDnsResult(p.message,p.records)));c.setPacketHandled(true);}
    }
    public record DhcpPoolPacket(BlockPos pos,String action,String name,boolean ipv6,String start,String end,String prefix,String gateway,String dns,int lease,String exclusions){
        public static void encode(DhcpPoolPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.name,32);b.writeBoolean(p.ipv6);b.writeUtf(p.start,45);b.writeUtf(p.end,45);b.writeUtf(p.prefix,15);b.writeUtf(p.gateway,45);b.writeUtf(p.dns,45);b.writeVarInt(p.lease);b.writeUtf(p.exclusions,1024);}
        static DhcpPoolPacket decode(FriendlyByteBuf b){return new DhcpPoolPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(32),b.readBoolean(),b.readUtf(45),b.readUtf(45),b.readUtf(15),b.readUtf(45),b.readUtf(45),b.readVarInt(),b.readUtf(1024));}
        static void handle(DhcpPoolPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){String m=rack.manageDhcpPool(p.action,p.name,p.ipv6,p.start,p.end,p.prefix,p.gateway,p.dns,p.lease,p.exclusions);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DhcpResultPacket(m,rack.dhcpData(p.ipv6),p.ipv6));}});c.setPacketHandled(true);}
    }
    public record DhcpResultPacket(String message,String data,boolean ipv6){static void encode(DhcpResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.data,16384);b.writeBoolean(p.ipv6);}static DhcpResultPacket decode(FriendlyByteBuf b){return new DhcpResultPacket(b.readUtf(256),b.readUtf(16384),b.readBoolean());}static void handle(DhcpResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptDhcpResult(p.message,p.data,p.ipv6)));c.setPacketHandled(true);}}
    public record NtpConfigPacket(BlockPos pos,boolean server,boolean client,int stratum,int poll,String source,int drift){
        public static void encode(NtpConfigPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeBoolean(p.server);b.writeBoolean(p.client);b.writeVarInt(p.stratum);b.writeVarInt(p.poll);b.writeUtf(p.source,45);b.writeInt(p.drift);}
        static NtpConfigPacket decode(FriendlyByteBuf b){return new NtpConfigPacket(b.readBlockPos(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readUtf(45),b.readInt());}
        static void handle(NtpConfigPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){String message=rack.configureNtp(p.server,p.client,p.stratum,p.poll,p.source,p.drift);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new NtpResultPacket(message,rack.clockSyncStatus(),rack.deviceTimeMillis(),rack.lastNtpSyncMillis()));}});c.setPacketHandled(true);}
    }
    public record NtpResultPacket(String message,String status,long deviceTime,long lastSync){
        static void encode(NtpResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.status,256);b.writeLong(p.deviceTime);b.writeLong(p.lastSync);}
        static NtpResultPacket decode(FriendlyByteBuf b){return new NtpResultPacket(b.readUtf(256),b.readUtf(256),b.readLong(),b.readLong());}
        static void handle(NtpResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptNtpResult(p.message,p.status,p.deviceTime,p.lastSync)));c.setPacketHandled(true);}
    }
}
