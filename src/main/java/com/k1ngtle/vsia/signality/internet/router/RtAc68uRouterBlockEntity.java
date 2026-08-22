package com.k1ngtle.vsia.signality.internet.router;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.world.inventory.RtAc68uRouterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.router.RouterEngineeringSnapshot;
import java.util.ArrayList;
import java.util.List;

public final class RtAc68uRouterBlockEntity
        extends NetworkDeviceBlockEntity
        implements GeoBlockEntity, MenuProvider {

    private static final RawAnimation POWER_UP =
            RawAnimation.begin()
                    .thenPlay("power_up");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public final RouterOsSimulator routerOs =
            new RouterOsSimulator(this::onRouterOsChanged);

    private boolean routerManagementLoaded = false;

    // W1.21 FULL V5 ROUTED ETHERNET BINDINGS
    private final Map<String, BlockPos> w121EthernetPeers =
            new LinkedHashMap<>();

    private static final String W121_LAN0 = "lan0";
    private static final String W121_LAN1 = "lan1";

    public RtAc68uRouterBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                SignalityBlocks.RT_AC68U_ROUTER_BE.get(),
                pos,
                state
        );

        // W1.21 FULL V3 CLI PING BINDING
        routerOs.setLivePingTransmitter(
                this::sendRouterCliRealPing
        );

        // W1.21 FULL V5.1 CLI DIAGNOSTIC BINDING
        routerOs.setLiveEthernetDiagnostics(this::w121EthernetDiagnosticLines);
        routerOs.setLiveArpDiagnostics(this::wifiRouterArpStateLines);

    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level instanceof ServerLevel) {
            if (routerManagementLoaded) {
                applyRouterOsToLiveNetwork();
            } else if (!wifiRouterHasConfiguration()) {
                applyRouterDefaults();
                syncRouterOsWithDefaults();
            } else {
                syncRouterOsWithDefaults();
            }

            w121RecoverNearbySwitchBindings();
        }
    }

    private void applyRouterDefaults() {
        configureWifiStaticIpv4(
                "192.168.1.1",
                "255.255.255.0",
                ""
        );

        configureWifiLiveRouterInterface(
                "lan0",
                "192.168.1.1",
                24
        );

        configureWifiLiveRouterInterface(
                "lan1",
                "192.168.2.1",
                24
        );

        setWifiLiveRouterEnabled(true);
    }


    private void syncRouterOsWithDefaults() {
        if (routerManagementLoaded) {
            return;
        }

        routerOs.wlanIp = wifiIpAddress();
        routerOs.wlanMask = "255.255.255.0";
        routerOs.wlanGateway = "";
        routerOs.lan0Ip = "192.168.1.1";
        routerOs.lan0Mask = "255.255.255.0";
        routerOs.lan1Ip = "192.168.2.1";
        routerOs.lan1Mask = "255.255.255.0";
        routerOs.forwardingEnabled = wifiLiveRouterEnabled();
    }

    private void onRouterOsChanged() {
        routerManagementLoaded = true;
        applyRouterOsToLiveNetwork();
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    getBlockPos(),
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    public void applyRouterOsToLiveNetwork() {
        configureWifiStaticIpv4(
                routerOs.wlanIp,
                routerOs.wlanMask,
                routerOs.wlanGateway == null ? "" : routerOs.wlanGateway
        );

        configureWifiLiveRouterInterface(
                "lan0",
                routerOs.lan0Ip,
                RouterOsSimulator.maskToPrefix(routerOs.lan0Mask)
        );

        configureWifiLiveRouterInterface(
                "lan1",
                routerOs.lan1Ip,
                RouterOsSimulator.maskToPrefix(routerOs.lan1Mask)
        );

        setWifiLiveRouterEnabled(
                routerOs.forwardingEnabled
        );

        // W1.21 FULL LIVE ROUTING SYNCHRONIZATION
        clearWifiLiveRouterStaticRoutes();

        for (RouterOsSimulator.RouteEntry route
                : routerOs.staticRoutes) {

            int prefix =
                    RouterOsSimulator.maskToPrefix(
                            route.mask
                    );

            String egress =
                    resolveW121RouteEgress(
                            route.nextHop
                    );

            if (!egress.isBlank()) {
                addWifiLiveRouterRoute(
                        route.network,
                        prefix,
                        route.nextHop,
                        egress,
                        1
                );
            }
        }
    }


    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "router_power",
                        0,
                        state -> {
                            state.setAnimation(POWER_UP);
                            return PlayState.CONTINUE;
                        }
                )
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("W1204RouterOs", routerOs.save());
        tag.putBoolean("W1204RouterManagementLoaded", routerManagementLoaded);

        CompoundTag ethernetPeers = new CompoundTag();
        for (Map.Entry<String, BlockPos> entry : w121EthernetPeers.entrySet()) {
            CompoundTag peer = new CompoundTag();
            peer.putInt("X", entry.getValue().getX());
            peer.putInt("Y", entry.getValue().getY());
            peer.putInt("Z", entry.getValue().getZ());
            ethernetPeers.put(entry.getKey(), peer);
        }
        tag.put("W121EthernetPeers", ethernetPeers);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        w121EthernetPeers.clear();
        if (tag.contains("W121EthernetPeers")) {
            CompoundTag ethernetPeers = tag.getCompound("W121EthernetPeers");
            for (String interfaceName : new String[]{W121_LAN0, W121_LAN1}) {
                if (!ethernetPeers.contains(interfaceName)) continue;
                CompoundTag peer = ethernetPeers.getCompound(interfaceName);
                w121EthernetPeers.put(
                        interfaceName,
                        new BlockPos(
                                peer.getInt("X"),
                                peer.getInt("Y"),
                                peer.getInt("Z")
                        )
                );
            }
        }

        if (tag.contains("W1204RouterOs")) {
            routerOs.load(
                    tag.getCompound("W1204RouterOs")
            );
            routerManagementLoaded = true;
        } else {
            routerManagementLoaded =
                    tag.getBoolean("W1204RouterManagementLoaded");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(routerOs.displayName);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new RtAc68uRouterMenu(
                containerId,
                playerInventory,
                this
        );
    }


    private String resolveW121RouteEgress(
            String nextHop
    ) {
        if (nextHop == null
                || nextHop.isBlank()
                || "0.0.0.0".equals(nextHop)) {
            return "";
        }

        if (w121SameSubnet(
                nextHop,
                routerOs.lan0Ip,
                routerOs.lan0Mask
        )) {
            return "lan0";
        }

        if (w121SameSubnet(
                nextHop,
                routerOs.lan1Ip,
                routerOs.lan1Mask
        )) {
            return "lan1";
        }

        return "";
    }

    private static boolean w121SameSubnet(
            String a,
            String b,
            String mask
    ) {
        try {
            String[] aa = a.split("\\.");
            String[] bb = b.split("\\.");
            String[] mm = mask.split("\\.");

            if (aa.length != 4
                    || bb.length != 4
                    || mm.length != 4) {
                return false;
            }

            for (int i = 0; i < 4; i++) {
                int av = Integer.parseInt(aa[i]) & 0xFF;
                int bv = Integer.parseInt(bb[i]) & 0xFF;
                int mv = Integer.parseInt(mm[i]) & 0xFF;

                if ((av & mv) != (bv & mv)) {
                    return false;
                }
            }

            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    // W1.21 FULL V4 ROUTER PING REPLY HOOK
    @Override
    protected void onRouterLocalIcmpReply(
            String sourceIp
    ) {
        routerOs.noteLivePingReply(
                sourceIp == null
                        ? ""
                        : sourceIp
        );
        setChanged();
    }


    public BlockPos w121EthernetPeer(String interfaceName) {
        return w121EthernetPeers.get(interfaceName);
    }

    public String w121InterfaceForPeer(BlockPos peerPos) {
        if (peerPos == null) return "";
        for (Map.Entry<String, BlockPos> entry : w121EthernetPeers.entrySet()) {
            if (peerPos.equals(entry.getValue())) return entry.getKey();
        }
        return "";
    }

    public String w121BindEthernetPeerAuto(BlockPos peerPos) {
        if (peerPos == null || peerPos.equals(worldPosition)) return "";

        String existing = w121InterfaceForPeer(peerPos);
        if (!existing.isBlank()) return existing;

        if (!w121EthernetPeers.containsKey(W121_LAN0)
                && w121BindEthernetPeer(W121_LAN0, peerPos)) {
            return W121_LAN0;
        }

        if (!w121EthernetPeers.containsKey(W121_LAN1)
                && w121BindEthernetPeer(W121_LAN1, peerPos)) {
            return W121_LAN1;
        }

        return "";
    }

    public boolean w121BindEthernetPeer(String interfaceName, BlockPos peerPos) {
        if ((!W121_LAN0.equals(interfaceName) && !W121_LAN1.equals(interfaceName))
                || peerPos == null || peerPos.equals(worldPosition)) {
            return false;
        }

        String previous = w121InterfaceForPeer(peerPos);
        if (!previous.isBlank() && !previous.equals(interfaceName)) {
            w121EthernetPeers.remove(previous);
        }

        BlockPos occupied = w121EthernetPeers.get(interfaceName);
        if (occupied != null && !occupied.equals(peerPos)) {
            return false;
        }

        w121EthernetPeers.put(interfaceName, peerPos.immutable());
        traceRoutedEthernet(
                "BIND dev=" + interfaceName + " peer=" + peerPos.toShortString()
        );
        setChanged();
        return true;
    }

    public boolean w121DisconnectEthernetPeer(BlockPos peerPos) {
        String interfaceName = w121InterfaceForPeer(peerPos);
        if (interfaceName.isBlank()) return false;

        w121EthernetPeers.remove(interfaceName);
        traceRoutedEthernet(
                "UNBIND dev=" + interfaceName + " peer=" + peerPos.toShortString()
        );
        setChanged();
        return true;
    }

    public String w121CompatibleInterfaceWith(RtAc68uRouterBlockEntity peer) {
        if (peer == null) return "";

        if (!w121EthernetPeers.containsKey(W121_LAN0)
                && (w121SameSubnet(routerOs.lan0Ip, peer.routerOs.lan0Ip, routerOs.lan0Mask)
                || w121SameSubnet(routerOs.lan0Ip, peer.routerOs.lan1Ip, routerOs.lan0Mask))) {
            return W121_LAN0;
        }

        if (!w121EthernetPeers.containsKey(W121_LAN1)
                && (w121SameSubnet(routerOs.lan1Ip, peer.routerOs.lan0Ip, routerOs.lan1Mask)
                || w121SameSubnet(routerOs.lan1Ip, peer.routerOs.lan1Ip, routerOs.lan1Mask))) {
            return W121_LAN1;
        }

        return "";
    }

    public void w121ReceiveRoutedEthernetPacket(
            OSINetworkPacket packet,
            BlockPos ingressPeer
    ) {
        if (level == null || level.isClientSide
                || packet == null || ingressPeer == null) {
            return;
        }

        String interfaceName = w121InterfaceForPeer(ingressPeer);
        if (interfaceName.isBlank()) {
            traceRoutedEthernet(
                    "RX DROP unknown-peer=" + ingressPeer.toShortString()
            );
            return;
        }

        packet.payload.putString("router_ingress_interface", interfaceName);

        traceRoutedEthernet(
                "RX dev=" + interfaceName
                        + " peer=" + ingressPeer.toShortString()
                        + " protocol=" + packet.applicationProtocol
                        + " " + packet.sourceIp + "->" + packet.targetIp
        );

        receiveRoutedEthernetIngress(interfaceName, packet);
    }

    @Override
    protected boolean transmitRoutedEthernetEgress(
            String interfaceName,
            OSINetworkPacket packet
    ) {
        if (level == null || level.isClientSide || packet == null) {
            return false;
        }

        BlockPos peerPos = w121EthernetPeers.get(interfaceName);
        if (peerPos == null) {
            traceRoutedEthernet(
                    "TX DROP dev=" + interfaceName + " reason=no-physical-peer"
            );
            return false;
        }

        BlockEntity peer = level.getBlockEntity(peerPos);
        if (peer == null) {
            traceRoutedEthernet(
                    "TX DROP dev=" + interfaceName + " peer="
                            + peerPos.toShortString()
                            + " reason=missing-block-entity"
            );
            return true;
        }

        OSINetworkPacket frame =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        frame.payload.putString("router_egress_interface", interfaceName);

        traceRoutedEthernet(
                "TX dev=" + interfaceName
                        + " peer=" + peerPos.toShortString()
                        + " protocol=" + frame.applicationProtocol
                        + " " + frame.sourceIp + "->" + frame.targetIp
                        + " l2dst=" + frame.targetMac
        );

        if (peer instanceof NetworkSwitchBlockEntity networkSwitch) {
            networkSwitch.receiveWiredPacket(frame, worldPosition);
            return true;
        }

        if (peer instanceof RtAc68uRouterBlockEntity peerRouter) {
            peerRouter.w121ReceiveRoutedEthernetPacket(frame, worldPosition);
            return true;
        }

        if (peer instanceof ServerRackBlockEntity rack) {
            rack.receiveWiredPacket(frame);
            return true;
        }

        if (peer instanceof FirewallBlockEntity firewall) {
            firewall.receiveWiredPacket(frame, worldPosition);
            return true;
        }

        traceRoutedEthernet(
                "TX DROP dev=" + interfaceName
                        + " peer-type=" + peer.getClass().getSimpleName()
        );
        return true;
    }

    private void w121RecoverNearbySwitchBindings() {
        if (level == null || level.isClientSide) return;

        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos candidate = worldPosition.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(candidate);

                    if (!(blockEntity instanceof NetworkSwitchBlockEntity networkSwitch)) {
                        continue;
                    }

                    if (!networkSwitch.getConnectedDevices().contains(worldPosition)) {
                        continue;
                    }

                    if (!w121InterfaceForPeer(candidate).isBlank()) {
                        continue;
                    }

                    String bound = w121BindEthernetPeerAuto(candidate);
                    if (!bound.isBlank()) {
                        traceRoutedEthernet(
                                "RECOVER SWITCH dev=" + bound
                                        + " peer=" + candidate.toShortString()
                        );
                    }
                }
            }
        }
    }

    // W1.21 FULL V5.1 PHYSICAL BINDING SNAPSHOT
    public List<String> w121EthernetDiagnosticLines() {
        List<String> out = new ArrayList<>();
        out.add(w121EthernetLine(W121_LAN0, "GigabitEthernet0/0/0", routerOs.lan0Ip, routerOs.lan0Mask));
        out.add(w121EthernetLine(W121_LAN1, "GigabitEthernet0/0/1", routerOs.lan1Ip, routerOs.lan1Mask));
        return List.copyOf(out);
    }

    private String w121EthernetLine(String logical, String display, String ip, String mask) {
        BlockPos peerPos = w121EthernetPeers.get(logical);
        String peer = peerPos == null ? "none" : peerPos.toShortString();
        String link = "DOWN";
        String type = "none";

        if (peerPos != null && level != null) {
            BlockEntity be = level.getBlockEntity(peerPos);
            if (be != null) {
                type = be.getClass().getSimpleName();
                link = "UP";
                if (be instanceof RtAc68uRouterBlockEntity peerRouter) {
                    String remote = peerRouter.w121InterfaceForPeer(worldPosition);
                    if (remote.isBlank()) {
                        link = "ONE_WAY";
                    } else {
                        type = type + "/" + remote;
                    }
                }
            }
        }

        return display + " / " + logical
                + " ip=" + ip + "/" + RouterOsSimulator.maskToPrefix(mask)
                + " peer=" + peer
                + " link=" + link
                + " type=" + type;
    }

    @Override
    public RouterEngineeringSnapshot wifiRouterEngineeringSnapshot() {
        RouterEngineeringSnapshot base = super.wifiRouterEngineeringSnapshot();
        List<String> diagnostics = new ArrayList<>();
        diagnostics.add("W1.21 V5.1 ROUTED ETHERNET");
        diagnostics.addAll(w121EthernetDiagnosticLines());
        diagnostics.addAll(wifiRouterArpStateLines());
        diagnostics.addAll(base.diagnostics());
        return new RouterEngineeringSnapshot(
                base.enabled(),
                base.interfaces(),
                base.routes(),
                base.neighborCount(),
                List.copyOf(diagnostics)
        );
    }

}
