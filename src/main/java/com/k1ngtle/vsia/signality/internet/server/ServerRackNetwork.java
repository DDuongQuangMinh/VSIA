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
    private static final String VERSION = "11";
    private static int nextId;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Vsia.MOD_ID, "server_rack"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ServerRackNetwork() {}

    private static boolean authorize(ServerPlayer player, ServerRackBlockEntity rack) {
        if (rack.canConfigure(player)) return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Access denied. This rack is owned by " + rack.ownerName()), true);
        return false;
    }

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
        CHANNEL.registerMessage(nextId++, SyslogCommandPacket.class,SyslogCommandPacket::encode,SyslogCommandPacket::decode,SyslogCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, SyslogResultPacket.class,SyslogResultPacket::encode,SyslogResultPacket::decode,SyslogResultPacket::handle);
        CHANNEL.registerMessage(nextId++, AaaCommandPacket.class,AaaCommandPacket::encode,AaaCommandPacket::decode,AaaCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, AaaResultPacket.class,AaaResultPacket::encode,AaaResultPacket::decode,AaaResultPacket::handle);
        CHANNEL.registerMessage(nextId++, RadiusCommandPacket.class,RadiusCommandPacket::encode,RadiusCommandPacket::decode,RadiusCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, RadiusResultPacket.class,RadiusResultPacket::encode,RadiusResultPacket::decode,RadiusResultPacket::handle);
        CHANNEL.registerMessage(nextId++, IotCommandPacket.class,IotCommandPacket::encode,IotCommandPacket::decode,IotCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, IotResultPacket.class,IotResultPacket::encode,IotResultPacket::decode,IotResultPacket::handle);
        CHANNEL.registerMessage(nextId++, VmCommandPacket.class,VmCommandPacket::encode,VmCommandPacket::decode,VmCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, VmResultPacket.class,VmResultPacket::encode,VmResultPacket::decode,VmResultPacket::handle);
        CHANNEL.registerMessage(nextId++, PrpCommandPacket.class,PrpCommandPacket::encode,PrpCommandPacket::decode,PrpCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, PrpResultPacket.class,PrpResultPacket::encode,PrpResultPacket::decode,PrpResultPacket::handle);
        CHANNEL.registerMessage(nextId++, HttpFileCommandPacket.class,HttpFileCommandPacket::encode,HttpFileCommandPacket::decode,HttpFileCommandPacket::handle);
        CHANNEL.registerMessage(nextId++, HttpFileResultPacket.class,HttpFileResultPacket::encode,HttpFileResultPacket::decode,HttpFileResultPacket::handle);
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
                    if (!authorize(player, rack)) return;
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
        static void handle(DesktopToolPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=rack.executeDesktopTool(p.tool,p.input,player.serverLevel());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DesktopResultPacket(p.tool,result));}});c.setPacketHandled(true);}
    }
    public record DesktopResultPacket(String tool,String result){
        static void encode(DesktopResultPacket p,FriendlyByteBuf b){b.writeUtf(p.tool,32);b.writeUtf(p.result,8192);}
        static DesktopResultPacket decode(FriendlyByteBuf b){return new DesktopResultPacket(b.readUtf(32),b.readUtf(8192));}
        static void handle(DesktopResultPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptDesktopResult(p.tool,p.result)));c.setPacketHandled(true);}
    }
    public record ProgramPacket(BlockPos pos,String action,String source){
        public static void encode(ProgramPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,8);b.writeUtf(p.source,16384);}
        static ProgramPacket decode(FriendlyByteBuf b){return new ProgramPacket(b.readBlockPos(),b.readUtf(8),b.readUtf(16384));}
        static void handle(ProgramPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result;String source=p.source;if(p.action.equals("open")){source=rack.programSource();result=rack.programOutput();}else if(p.action.equals("save"))result=rack.saveProgram(source);else result=rack.runProgram(source,player.serverLevel());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ProgramResultPacket(source,result));}});c.setPacketHandled(true);}
    }
    public record ProgramResultPacket(String source,String result){
        static void encode(ProgramResultPacket p,FriendlyByteBuf b){b.writeUtf(p.source,16384);b.writeUtf(p.result,16384);}
        static ProgramResultPacket decode(FriendlyByteBuf b){return new ProgramResultPacket(b.readUtf(16384),b.readUtf(16384));}
        static void handle(ProgramResultPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptProgramResult(p.source,p.result)));c.setPacketHandled(true);}
    }
    public record DnsRecordPacket(BlockPos pos,String action,String name,String type,String detail,int ttl){
        public static void encode(DnsRecordPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.name,253);b.writeUtf(p.type,8);b.writeUtf(p.detail,253);b.writeVarInt(p.ttl);}
        static DnsRecordPacket decode(FriendlyByteBuf b){return new DnsRecordPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(253),b.readUtf(8),b.readUtf(253),b.readVarInt());}
        static void handle(DnsRecordPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String message=rack.manageDnsRecord(p.action,p.name,p.type,p.detail,p.ttl);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DnsResultPacket(message,rack.dnsRecordData()));}});c.setPacketHandled(true);}
    }
    public record DnsResultPacket(String message,String records){
        static void encode(DnsResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.records,16384);}
        static DnsResultPacket decode(FriendlyByteBuf b){return new DnsResultPacket(b.readUtf(256),b.readUtf(16384));}
        static void handle(DnsResultPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptDnsResult(p.message,p.records)));c.setPacketHandled(true);}
    }
    public record DhcpPoolPacket(BlockPos pos,String action,String name,boolean ipv6,String start,String end,String prefix,String gateway,String dns,int lease,String exclusions){
        public static void encode(DhcpPoolPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.name,32);b.writeBoolean(p.ipv6);b.writeUtf(p.start,45);b.writeUtf(p.end,45);b.writeUtf(p.prefix,15);b.writeUtf(p.gateway,45);b.writeUtf(p.dns,45);b.writeVarInt(p.lease);b.writeUtf(p.exclusions,1024);}
        static DhcpPoolPacket decode(FriendlyByteBuf b){return new DhcpPoolPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(32),b.readBoolean(),b.readUtf(45),b.readUtf(45),b.readUtf(15),b.readUtf(45),b.readUtf(45),b.readVarInt(),b.readUtf(1024));}
        static void handle(DhcpPoolPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String m=rack.manageDhcpPool(p.action,p.name,p.ipv6,p.start,p.end,p.prefix,p.gateway,p.dns,p.lease,p.exclusions);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new DhcpResultPacket(m,rack.dhcpData(p.ipv6),p.ipv6));}});c.setPacketHandled(true);}
    }
    public record DhcpResultPacket(String message,String data,boolean ipv6){static void encode(DhcpResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.data,16384);b.writeBoolean(p.ipv6);}static DhcpResultPacket decode(FriendlyByteBuf b){return new DhcpResultPacket(b.readUtf(256),b.readUtf(16384),b.readBoolean());}static void handle(DhcpResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptDhcpResult(p.message,p.data,p.ipv6)));c.setPacketHandled(true);}}
    public record NtpConfigPacket(BlockPos pos,boolean server,boolean client,int stratum,int poll,String source,int drift){
        public static void encode(NtpConfigPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeBoolean(p.server);b.writeBoolean(p.client);b.writeVarInt(p.stratum);b.writeVarInt(p.poll);b.writeUtf(p.source,45);b.writeInt(p.drift);}
        static NtpConfigPacket decode(FriendlyByteBuf b){return new NtpConfigPacket(b.readBlockPos(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readUtf(45),b.readInt());}
        static void handle(NtpConfigPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String message=rack.configureNtp(p.server,p.client,p.stratum,p.poll,p.source,p.drift);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new NtpResultPacket(message,rack.clockSyncStatus(),rack.deviceTimeMillis(),rack.lastNtpSyncMillis()));}});c.setPacketHandled(true);}
    }
    public record NtpResultPacket(String message,String status,long deviceTime,long lastSync){
        static void encode(NtpResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.status,256);b.writeLong(p.deviceTime);b.writeLong(p.lastSync);}
        static NtpResultPacket decode(FriendlyByteBuf b){return new NtpResultPacket(b.readUtf(256),b.readUtf(256),b.readLong(),b.readLong());}
        static void handle(NtpResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptNtpResult(p.message,p.status,p.deviceTime,p.lastSync)));c.setPacketHandled(true);}
    }
    public record SyslogCommandPacket(BlockPos pos,String action,int minimumSeverity,boolean acceptRemote,String facility,int severity,String message){
        public static void encode(SyslogCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeVarInt(p.minimumSeverity);b.writeBoolean(p.acceptRemote);b.writeUtf(p.facility,16);b.writeVarInt(p.severity);b.writeUtf(p.message,512);}
        static SyslogCommandPacket decode(FriendlyByteBuf b){return new SyslogCommandPacket(b.readBlockPos(),b.readUtf(16),b.readVarInt(),b.readBoolean(),b.readUtf(16),b.readVarInt(),b.readUtf(512));}
        static void handle(SyslogCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=p.action.equals("QUERY")?"Syslog refreshed.":rack.manageSyslog(p.action,p.minimumSeverity,p.acceptRemote,p.facility,p.severity,p.message,player.getGameProfile().getName());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new SyslogResultPacket(result,rack.syslogData(),rack.syslogMinimumSeverity(),rack.syslogAcceptRemote()));}});c.setPacketHandled(true);}
    }
    public record SyslogResultPacket(String message,String data,int minimumSeverity,boolean acceptRemote){
        static void encode(SyslogResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.data,32767);b.writeVarInt(p.minimumSeverity);b.writeBoolean(p.acceptRemote);}
        static SyslogResultPacket decode(FriendlyByteBuf b){return new SyslogResultPacket(b.readUtf(256),b.readUtf(32767),b.readVarInt(),b.readBoolean());}
        static void handle(SyslogResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptSyslogResult(p.message,p.data,p.minimumSeverity,p.acceptRemote)));c.setPacketHandled(true);}
    }
    public record AaaCommandPacket(BlockPos pos,String action,String username,String password,int privilege,boolean enabled,String service){
        public static void encode(AaaCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,24);b.writeUtf(p.username,32);b.writeUtf(p.password,64);b.writeVarInt(p.privilege);b.writeBoolean(p.enabled);b.writeUtf(p.service,32);}
        static AaaCommandPacket decode(FriendlyByteBuf b){return new AaaCommandPacket(b.readBlockPos(),b.readUtf(24),b.readUtf(32),b.readUtf(64),b.readVarInt(),b.readBoolean(),b.readUtf(32));}
        static void handle(AaaCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=p.action.equals("QUERY")?"AAA data refreshed.":rack.manageAaa(p.action,p.username,p.password,p.privilege,p.enabled,p.service,player.getGameProfile().getName());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new AaaResultPacket(result,rack.aaaUserData(),rack.aaaAccountingData()));}});c.setPacketHandled(true);}
    }
    public record AaaResultPacket(String message,String users,String accounting){
        static void encode(AaaResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.users,16384);b.writeUtf(p.accounting,32767);}
        static AaaResultPacket decode(FriendlyByteBuf b){return new AaaResultPacket(b.readUtf(256),b.readUtf(16384),b.readUtf(32767));}
        static void handle(AaaResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptAaaResult(p.message,p.users,p.accounting)));c.setPacketHandled(true);}
    }
    public record RadiusCommandPacket(BlockPos pos,String action,String name,String address,String secret,boolean enabled,String username,String password,int privilege){
        public static void encode(RadiusCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.name,32);b.writeUtf(p.address,15);b.writeUtf(p.secret,64);b.writeBoolean(p.enabled);b.writeUtf(p.username,32);b.writeUtf(p.password,64);b.writeVarInt(p.privilege);}
        static RadiusCommandPacket decode(FriendlyByteBuf b){return new RadiusCommandPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(32),b.readUtf(15),b.readUtf(64),b.readBoolean(),b.readUtf(32),b.readUtf(64),b.readVarInt());}
        static void handle(RadiusCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=p.action.equals("QUERY")?"RADIUS data refreshed.":rack.manageRadius(p.action,p.name,p.address,p.secret,p.enabled,p.username,p.password,p.privilege);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new RadiusResultPacket(result,rack.radiusClientData(),rack.radiusEventData()));}});c.setPacketHandled(true);}
    }
    public record RadiusResultPacket(String message,String clients,String events){
        static void encode(RadiusResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.clients,16384);b.writeUtf(p.events,32767);}
        static RadiusResultPacket decode(FriendlyByteBuf b){return new RadiusResultPacket(b.readUtf(256),b.readUtf(16384),b.readUtf(32767));}
        static void handle(RadiusResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptRadiusResult(p.message,p.clients,p.events)));c.setPacketHandled(true);}
    }
    public record IotCommandPacket(BlockPos pos,String action,String id,String name,String type,String value){
        static void encode(IotCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.id,32);b.writeUtf(p.name,32);b.writeUtf(p.type,24);b.writeUtf(p.value,128);}
        static IotCommandPacket decode(FriendlyByteBuf b){return new IotCommandPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(32),b.readUtf(32),b.readUtf(24),b.readUtf(128));}
        static void handle(IotCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=rack.manageIot(p.action,p.id,p.name,p.type,p.value);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new IotResultPacket(result,rack.iotDeviceData()));}});c.setPacketHandled(true);}
    }
    public record IotResultPacket(String message,String devices){
        static void encode(IotResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.devices,32767);}
        static IotResultPacket decode(FriendlyByteBuf b){return new IotResultPacket(b.readUtf(256),b.readUtf(32767));}
        static void handle(IotResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptIotResult(p.message,p.devices)));c.setPacketHandled(true);}
    }
    public record VmCommandPacket(BlockPos pos,String action,String name,String operatingSystem,int cpuCores,int memoryMb,int storageGb){
        static void encode(VmCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.name,32);b.writeUtf(p.operatingSystem,32);b.writeVarInt(p.cpuCores);b.writeVarInt(p.memoryMb);b.writeVarInt(p.storageGb);}
        static VmCommandPacket decode(FriendlyByteBuf b){return new VmCommandPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(32),b.readUtf(32),b.readVarInt(),b.readVarInt(),b.readVarInt());}
        static void handle(VmCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=rack.manageVirtualMachine(p.action,p.name,p.operatingSystem,p.cpuCores,p.memoryMb,p.storageGb);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new VmResultPacket(result,rack.virtualMachineData()));}});c.setPacketHandled(true);}
    }
    public record VmResultPacket(String message,String machines){
        static void encode(VmResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.machines,32767);}
        static VmResultPacket decode(FriendlyByteBuf b){return new VmResultPacket(b.readUtf(256),b.readUtf(32767));}
        static void handle(VmResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptVmResult(p.message,p.machines)));c.setPacketHandled(true);}
    }
    public record PrpCommandPacket(BlockPos pos,String action,boolean enabled,boolean laneA,boolean laneB,String peerIp){
        static void encode(PrpCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,24);b.writeBoolean(p.enabled);b.writeBoolean(p.laneA);b.writeBoolean(p.laneB);b.writeUtf(p.peerIp,15);}
        static PrpCommandPacket decode(FriendlyByteBuf b){return new PrpCommandPacket(b.readBlockPos(),b.readUtf(24),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readUtf(15));}
        static void handle(PrpCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String result=rack.managePrp(p.action,p.enabled,p.laneA,p.laneB,p.peerIp);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new PrpResultPacket(result,rack.prpStatusData()));}});c.setPacketHandled(true);}
    }
    public record PrpResultPacket(String message,String status){
        static void encode(PrpResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.status,1024);}
        static PrpResultPacket decode(FriendlyByteBuf b){return new PrpResultPacket(b.readUtf(256),b.readUtf(1024));}
        static void handle(PrpResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptPrpResult(p.message,p.status)));c.setPacketHandled(true);}
    }
    public record HttpFileCommandPacket(BlockPos pos,String action,String filename,String content,boolean readable,boolean writable,boolean https,int httpPort,int httpsPort){
        static void encode(HttpFileCommandPacket p,FriendlyByteBuf b){b.writeBlockPos(p.pos);b.writeUtf(p.action,16);b.writeUtf(p.filename,64);b.writeUtf(p.content,32768);b.writeBoolean(p.readable);b.writeBoolean(p.writable);b.writeBoolean(p.https);b.writeVarInt(p.httpPort);b.writeVarInt(p.httpsPort);}
        static HttpFileCommandPacket decode(FriendlyByteBuf b){return new HttpFileCommandPacket(b.readBlockPos(),b.readUtf(16),b.readUtf(64),b.readUtf(32768),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt());}
        static void handle(HttpFileCommandPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||player.distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>64)return;if(player.level().getBlockEntity(p.pos)instanceof ServerRackBlockEntity rack){if(!authorize(player,rack))return;String message;ServerRackHostedFile file=null;if(p.action.equals("QUERY")){message="Web files refreshed.";}else if(p.action.equals("CONFIG")){message=rack.configureWebServices(p.https,p.httpPort,p.httpsPort);}else if(p.action.equals("OPEN")){file=rack.hostedFile(p.filename);message=file==null?"File not found.":"File loaded.";}else{message=rack.manageHostedFile(p.action,"HTTP",p.filename,p.content,p.readable,p.writable);if(p.action.equals("SAVE"))file=rack.hostedFile(p.filename);}CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new HttpFileResultPacket(message,rack.hostedFileData(),file==null?"":file.name(),file==null?"":file.content(),file==null||file.readable(),file==null||file.writable(),rack.httpsEnabled(),rack.httpPort(),rack.httpsPort()));}});c.setPacketHandled(true);}
    }
    public record HttpFileResultPacket(String message,String files,String filename,String content,boolean readable,boolean writable,boolean https,int httpPort,int httpsPort){
        static void encode(HttpFileResultPacket p,FriendlyByteBuf b){b.writeUtf(p.message,256);b.writeUtf(p.files,16384);b.writeUtf(p.filename,64);b.writeUtf(p.content,32768);b.writeBoolean(p.readable);b.writeBoolean(p.writable);b.writeBoolean(p.https);b.writeVarInt(p.httpPort);b.writeVarInt(p.httpsPort);}
        static HttpFileResultPacket decode(FriendlyByteBuf b){return new HttpFileResultPacket(b.readUtf(256),b.readUtf(16384),b.readUtf(64),b.readUtf(32768),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt());}
        static void handle(HttpFileResultPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ServerRackScreen.acceptHttpFileResult(p.message,p.files,p.filename,p.content,p.readable,p.writable,p.https,p.httpPort,p.httpsPort)));c.setPacketHandled(true);}
    }
}
