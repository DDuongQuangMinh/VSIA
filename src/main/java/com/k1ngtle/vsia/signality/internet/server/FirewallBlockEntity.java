package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117InterfaceNeighborEngine;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117ArpFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class FirewallBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Core Network Logic
    private int deviceId = -1;
    private String managementIp = "unassigned";
    private String deviceName = "ciscoasa";
    private boolean strictMode = true;

    // IP Configuration
    private String subnetMask = "unassigned";
    private String ipv6Address = "unassigned";
    private boolean dhcpEnabled = true;

    // Connections
    private BlockPos wanConnection = null;
    private BlockPos lanConnection = null;

    private long w1161LanRx = 0L;
    private long w1161WanRx = 0L;
    private long w1161LanTx = 0L;
    private long w1161WanTx = 0L;
    private long w1161Drops = 0L;
    private long w1161TtlExpired = 0L;
    private String w1161LastTransit = "READY";

    private final W117InterfaceNeighborEngine w117LanNeighbors =
            new W117InterfaceNeighborEngine();

    private final W117InterfaceNeighborEngine w117WanNeighbors =
            new W117InterfaceNeighborEngine();

    private long w117ArpRx = 0L;
    private long w117ArpTx = 0L;
    private long w117QueuedIpv4 = 0L;
    private long w117ResolvedIpv4 = 0L;
    private boolean w117DuplicateInside = false;
    private boolean w117DuplicateOutside = false;
    private String w117LastNeighborEvent = "READY";


    // Rules
    private final List<FirewallRule> activeRules = new ArrayList<>();

    public final FirewallOsSimulator[] osSimulators = new FirewallOsSimulator[7];

    public FirewallBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.FIREWALL_BE.get(), pos, state);
        for (int i = 0; i < 7; i++) {
            this.osSimulators[i] = new FirewallOsSimulator(0, i + 1, "ciscoasa", this::setChanged);
        }
        if (activeRules.isEmpty()) {
            activeRules.add(new FirewallRule("Block Suspicious Traffic", "DROP", "ANY", "WAN", true));
            activeRules.add(new FirewallRule("Allow LAN Outbound", "ALLOW", "LAN", "WAN", true));
        }
    }

    public void tick() {
        if (level != null && !level.isClientSide) {
            for (FirewallOsSimulator sim : osSimulators) {
                sim.tick(this::broadcastPacketOutwards);
            }

            w117TickNeighbors(
                    System.currentTimeMillis()
            );
        }
    }

    private void broadcastPacketOutwards(OSINetworkPacket packet) {
        if (lanConnection != null) {
            FirewallOsSimulator.PortConfig pc = osSimulators[0].portConfigs.get("GigabitEthernet1/1");
            if (pc != null && pc.up) {
                BlockEntity be = level.getBlockEntity(lanConnection);
                if (be instanceof NetworkSwitchBlockEntity sw) sw.receiveWiredPacket(packet, this.worldPosition);
                if (be instanceof ServerRackBlockEntity rack) rack.receiveWiredPacket(packet);
            }
        }
        if (wanConnection != null) {
            FirewallOsSimulator.PortConfig pc = osSimulators[0].portConfigs.get("GigabitEthernet1/2");
            if (pc != null && pc.up) {
                BlockEntity be = level.getBlockEntity(wanConnection);
                if (be instanceof NetworkSwitchBlockEntity sw) sw.receiveWiredPacket(packet, this.worldPosition);
                if (be instanceof ServerRackBlockEntity rack) rack.receiveWiredPacket(packet);
            }
        }
    }

    public void receiveWiredPacket(
            OSINetworkPacket packet,
            BlockPos ingressPos
    ) {
        if (level == null
                || level.isClientSide
                || packet == null
                || ingressPos == null) {
            return;
        }

        String ingressPort;

        if (ingressPos.equals(lanConnection)) {
            ingressPort = "GigabitEthernet1/1";
        } else if (ingressPos.equals(wanConnection)) {
            ingressPort = "GigabitEthernet1/2";
        } else {
            w1161Drops++;
            w1161LastTransit =
                    "DROP UNKNOWN_INGRESS pos="
                            + ingressPos.toShortString();
            setChanged();
            return;
        }

        if (W117ArpFrame.isArp(packet)) {
            w117HandleArp(packet, ingressPort);
            setChanged();
            return;
        }

        if ("GigabitEthernet1/1".equals(ingressPort)) {
            w1161LanRx++;
        } else {
            w1161WanRx++;
        }

        OSINetworkPacket filtered =
                osSimulators[0].w1161FilterAndRoutePacket(
                        packet,
                        ingressPort
                );

        if (filtered == null) {
            w1161Drops++;
            w1161LastTransit =
                    osSimulators[0].w1161LastPipelineStatus();
            setChanged();
            return;
        }

        String egressPort =
                osSimulators[0].w1161LastEgressInterface();

        if (egressPort == null || egressPort.isBlank()) {
            w1161Drops++;
            w1161LastTransit =
                    "DROP NO_ROUTE dst=" + filtered.targetIp;
            setChanged();
            return;
        }

        if (egressPort.equals(ingressPort)) {
            w1161Drops++;
            w1161LastTransit =
                    "DROP SAME_INTERFACE " + ingressPort;
            setChanged();
            return;
        }

        if (!osSimulators[0].w1161InterfaceUp(egressPort)) {
            w1161Drops++;
            w1161LastTransit =
                    "DROP EGRESS_DOWN " + egressPort;
            setChanged();
            return;
        }

        if (filtered.ttl <= 1) {
            w1161Drops++;
            w1161TtlExpired++;
            w1161LastTransit =
                    "DROP TTL_EXPIRED ingress=" + ingressPort;
            setChanged();
            return;
        }

        filtered.ttl--;

        String nextHopIp =
                osSimulators[0].w117NextHopIp(
                        filtered.targetIp,
                        egressPort
                );

        if (nextHopIp == null || nextHopIp.isBlank()) {
            w1161Drops++;
            w1161LastTransit =
                    "DROP NO_NEXT_HOP dst="
                            + filtered.targetIp
                            + " egress="
                            + egressPort;
            setChanged();
            return;
        }

        W117InterfaceNeighborEngine neighbors =
                w117NeighborEngine(egressPort);

        if (neighbors == null) {
            w1161Drops++;
            w1161LastTransit =
                    "DROP NO_NEIGHBOR_ENGINE egress=" + egressPort;
            setChanged();
            return;
        }

        long now = System.currentTimeMillis();

        var resolved = neighbors.lookup(nextHopIp, now);

        if (resolved.isPresent()) {
            filtered.sourceMac = w117MacForInterface(egressPort);
            filtered.targetMac = resolved.get().mac();

            if (!sendPacketOutPort(filtered, egressPort)) {
                w1161Drops++;
                w1161LastTransit =
                        "DROP NO_WORLD_LINK egress=" + egressPort;
                setChanged();
                return;
            }

            w1161LastTransit =
                    "FORWARD "
                            + ingressPort
                            + " -> "
                            + egressPort
                            + " nextHop="
                            + nextHopIp
                            + " l2dst="
                            + resolved.get().mac()
                            + " "
                            + filtered.sourceIp
                            + ":"
                            + filtered.sourcePort
                            + " -> "
                            + filtered.targetIp
                            + ":"
                            + filtered.targetPort
                            + " ttl="
                            + filtered.ttl;

            setChanged();
            return;
        }

        boolean queued =
                neighbors.queue(
                        nextHopIp,
                        filtered,
                        egressPort,
                        now
                );

        if (!queued) {
            w1161Drops++;
            w1161LastTransit =
                    "DROP NEIGHBOR_QUEUE_FULL nextHop=" + nextHopIp;
            setChanged();
            return;
        }

        w117QueuedIpv4++;

        if (neighbors.needsInitialRequest(nextHopIp)) {
            neighbors.markRequestSent(nextHopIp, now);

            OSINetworkPacket request =
                    W117ArpFrame.request(
                            w117MacForInterface(egressPort),
                            w117IpForInterface(egressPort),
                            nextHopIp,
                            "W1.17-FIREWALL-ARP"
                    );

            w117SendLayer2Frame(request, egressPort);
        }

        w1161LastTransit =
                "ARP_PENDING egress="
                        + egressPort
                        + " nextHop="
                        + nextHopIp
                        + " queued="
                        + neighbors.pendingCount();

        setChanged();
    }

    private boolean sendPacketOutPort(
            OSINetworkPacket packet,
            String egressPort
    ) {
        if (level == null || packet == null || egressPort == null) {
            return false;
        }

        BlockPos target;

        if (egressPort.equals("GigabitEthernet1/1")) {
            target = lanConnection;
        } else if (egressPort.equals("GigabitEthernet1/2")) {
            target = wanConnection;
        } else {
            return false;
        }

        if (target == null) {
            return false;
        }

        BlockEntity be =
                level.getBlockEntity(
                        target
                );

        OSINetworkPacket forwarded =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        if (be instanceof NetworkSwitchBlockEntity sw) {
            sw.receiveWiredPacket(
                    forwarded,
                    this.worldPosition
            );
        } else if (be instanceof ServerRackBlockEntity rack) {
            rack.receiveWiredPacket(
                    forwarded
            );
        } else {
            return false;
        }

        if (egressPort.equals("GigabitEthernet1/1")) {
            w1161LanTx++;
        } else {
            w1161WanTx++;
        }

        return true;
    }

    private void w117HandleArp(
            OSINetworkPacket packet,
            String ingressPort
    ) {
        w117ArpRx++;

        W117InterfaceNeighborEngine neighbors =
                w117NeighborEngine(ingressPort);

        if (neighbors == null) {
            w117LastNeighborEvent =
                    "ARP_DROP_NO_ENGINE " + ingressPort;
            return;
        }

        String senderIp = W117ArpFrame.senderIp(packet);
        String senderMac = W117ArpFrame.senderMac(packet);

        if (senderIp != null
                && !senderIp.isBlank()
                && senderMac != null
                && !senderMac.isBlank()) {
            String ownIp = w117IpForInterface(ingressPort);
            String ownMac = w117MacForInterface(ingressPort);

            if (ownIp.equals(senderIp)
                    && !ownMac.equalsIgnoreCase(senderMac)) {
                if ("GigabitEthernet1/1".equals(ingressPort)) {
                    w117DuplicateInside = true;
                } else {
                    w117DuplicateOutside = true;
                }

                w117LastNeighborEvent =
                        "DUPLICATE_IPV4 interface="
                                + ingressPort
                                + " senderMac="
                                + senderMac;
            }

            neighbors.learn(
                    senderIp,
                    senderMac,
                    System.currentTimeMillis()
            );
        }

        if (W117ArpFrame.isRequest(packet)
                && w117IpForInterface(ingressPort).equals(
                W117ArpFrame.targetIp(packet)
        )) {
            OSINetworkPacket reply =
                    W117ArpFrame.reply(
                            w117MacForInterface(ingressPort),
                            w117IpForInterface(ingressPort),
                            senderMac,
                            senderIp,
                            packet.sessionId
                    );

            neighbors.noteReplyTx();
            w117SendLayer2Frame(reply, ingressPort);

            w117LastNeighborEvent =
                    "ARP_REPLY interface="
                            + ingressPort
                            + " target="
                            + senderIp;
        }

        if (W117ArpFrame.isReply(packet)) {
            w117FlushResolved(neighbors);
        }
    }

    private void w117FlushResolved(
            W117InterfaceNeighborEngine neighbors
    ) {
        for (W117InterfaceNeighborEngine.ResolvedPacket resolved :
                neighbors.drainResolved()) {
            OSINetworkPacket packet = resolved.packet();

            packet.sourceMac =
                    w117MacForInterface(resolved.egressPort());

            packet.targetMac = resolved.nextHopMac();

            if (sendPacketOutPort(packet, resolved.egressPort())) {
                w117ResolvedIpv4++;

                w1161LastTransit =
                        "FORWARD_RESOLVED egress="
                                + resolved.egressPort()
                                + " nextHop="
                                + resolved.nextHopIp()
                                + " l2dst="
                                + resolved.nextHopMac()
                                + " "
                                + packet.sourceIp
                                + ":"
                                + packet.sourcePort
                                + " -> "
                                + packet.targetIp
                                + ":"
                                + packet.targetPort
                                + " ttl="
                                + packet.ttl;
            } else {
                w1161Drops++;
                w1161LastTransit =
                        "DROP RESOLVED_NO_WORLD_LINK egress="
                                + resolved.egressPort();
            }
        }
    }

    private void w117TickNeighbors(long nowMillis) {
        w117TickNeighborEngine(
                w117LanNeighbors,
                "GigabitEthernet1/1",
                nowMillis
        );

        w117TickNeighborEngine(
                w117WanNeighbors,
                "GigabitEthernet1/2",
                nowMillis
        );

        w117FlushResolved(w117LanNeighbors);
        w117FlushResolved(w117WanNeighbors);
    }

    private void w117TickNeighborEngine(
            W117InterfaceNeighborEngine neighbors,
            String egressPort,
            long nowMillis
    ) {
        for (W117InterfaceNeighborEngine.Retry retry :
                neighbors.tick(nowMillis)) {
            OSINetworkPacket request =
                    W117ArpFrame.request(
                            w117MacForInterface(egressPort),
                            w117IpForInterface(egressPort),
                            retry.nextHopIp(),
                            "W1.17-FIREWALL-ARP-RETRY-"
                                    + retry.attempt()
                    );

            w117SendLayer2Frame(request, egressPort);

            w117LastNeighborEvent =
                    "ARP_RETRY interface="
                            + egressPort
                            + " nextHop="
                            + retry.nextHopIp()
                            + " attempt="
                            + retry.attempt();
        }
    }

    private W117InterfaceNeighborEngine w117NeighborEngine(String port) {
        if ("GigabitEthernet1/1".equals(port)) {
            return w117LanNeighbors;
        }

        if ("GigabitEthernet1/2".equals(port)) {
            return w117WanNeighbors;
        }

        return null;
    }

    private String w117IpForInterface(String port) {
        FirewallOsSimulator.PortConfig config =
                osSimulators[0].portConfigs.get(port);

        if (config == null || config.ipAddress == null) {
            return "";
        }

        return config.ipAddress;
    }

    public String w117MacForInterface(String port) {
        int portId =
                "GigabitEthernet1/2".equals(port)
                        ? 2
                        : 1;

        int id = Math.max(0, deviceId) & 0xFF;

        return String.format(
                "02:FA:%02X:%02X:00:01",
                id,
                portId
        );
    }

    private boolean w117SendLayer2Frame(
            OSINetworkPacket packet,
            String egressPort
    ) {
        if (level == null
                || packet == null
                || egressPort == null) {
            return false;
        }

        BlockPos target;

        if ("GigabitEthernet1/1".equals(egressPort)) {
            target = lanConnection;
        } else if ("GigabitEthernet1/2".equals(egressPort)) {
            target = wanConnection;
        } else {
            return false;
        }

        if (target == null) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(target);

        OSINetworkPacket forwarded =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        if (be instanceof NetworkSwitchBlockEntity sw) {
            sw.receiveWiredPacket(forwarded, this.worldPosition);
        } else if (be instanceof ServerRackBlockEntity rack) {
            rack.receiveWiredPacket(forwarded);
        } else {
            return false;
        }

        w117ArpTx++;
        return true;
    }

    public void w117ClearNeighborState() {
        w117LanNeighbors.clear();
        w117WanNeighbors.clear();
        w117ArpRx = 0L;
        w117ArpTx = 0L;
        w117QueuedIpv4 = 0L;
        w117ResolvedIpv4 = 0L;
        w117DuplicateInside = false;
        w117DuplicateOutside = false;
        w117LastNeighborEvent = "READY";
        setChanged();
    }

    public boolean w117SendGratuitousArp(String port) {
        String ip = w117IpForInterface(port);

        if (ip == null
                || ip.isBlank()
                || "unassigned".equalsIgnoreCase(ip)) {
            return false;
        }

        OSINetworkPacket packet =
                W117ArpFrame.gratuitous(
                        w117MacForInterface(port),
                        ip
                );

        return w117SendLayer2Frame(packet, port);
    }

    public String w117NeighborStatus() {
        long now = System.currentTimeMillis();

        return "W1.17 FIREWALL NEIGHBORS"
                + " | inside{"
                + w117LanNeighbors.status(now)
                + "}"
                + " | outside{"
                + w117WanNeighbors.status(now)
                + "}"
                + " | arpRx="
                + w117ArpRx
                + " | arpTx="
                + w117ArpTx
                + " | queuedIpv4="
                + w117QueuedIpv4
                + " | resolvedIpv4="
                + w117ResolvedIpv4
                + " | dupInside="
                + w117DuplicateInside
                + " | dupOutside="
                + w117DuplicateOutside
                + " | last="
                + w117LastNeighborEvent;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && deviceId == -1) {
            assignAutomaticId(ServerRackIdSavedData.allocate(serverLevel));
        }
    }

    public void assignAutomaticId(int id) {
        if (this.deviceId != -1) return;
        this.deviceId = id;
        this.deviceName = "ASA" + id + "_1";
        for (int i = 0; i < 7; i++) {
            this.osSimulators[i].hostname = "ASA" + id + "_" + (i + 1);
            this.osSimulators[i].displayName = "ASA" + id + "_" + (i + 1);
            this.osSimulators[i].macAddress = String.format("0002.4A0B.%02X%02X", id, i + 1);
        }
        setChanged();
    }

    public int getDeviceId() { return deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String name) {
        this.deviceName = name;
        setChanged();
    }

    public String getManagementIp() { return managementIp; }
    public void setManagementIp(String ip) {
        this.managementIp = ip;
        FirewallOsSimulator.PortConfig pc = osSimulators[0].portConfigs.get("Management1/1");
        if (pc != null) pc.ipAddress = ip;
        setChanged();
    }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String mask) {
        this.subnetMask = mask;
        FirewallOsSimulator.PortConfig pc = osSimulators[0].portConfigs.get("Management1/1");
        if (pc != null) pc.subnetMask = mask;
        setChanged();
    }

    public String getIpv6Address() { return ipv6Address; }
    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
        FirewallOsSimulator.PortConfig pc = osSimulators[0].portConfigs.get("Management1/1");
        if (pc != null) pc.ipv6Address = ipv6Address;
        setChanged();
    }

    public boolean isDhcpEnabled() { return dhcpEnabled; }
    public void setDhcpEnabled(boolean dhcpEnabled) { this.dhcpEnabled = dhcpEnabled; setChanged(); }

    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; setChanged(); }

    public List<FirewallRule> getRules() { return activeRules; }

    public BlockPos getLanConnection() {
        return lanConnection;
    }

    public BlockPos getWanConnection() {
        return wanConnection;
    }

    public boolean connectLanDevice(
            BlockPos pos
    ) {
        return connectTransitDevice(
                pos,
                true
        );
    }

    public boolean connectWanDevice(
            BlockPos pos
    ) {
        return connectTransitDevice(
                pos,
                false
        );
    }

    private boolean connectTransitDevice(
            BlockPos pos,
            boolean lan
    ) {
        if (pos == null
                || pos.equals(this.worldPosition)) {
            return false;
        }

        BlockPos other =
                lan
                        ? wanConnection
                        : lanConnection;

        if (pos.equals(other)) {
            return false;
        }

        if (lan) {
            if (pos.equals(lanConnection)) {
                return true;
            }

            lanConnection = pos;
        } else {
            if (pos.equals(wanConnection)) {
                return true;
            }

            wanConnection = pos;
        }

        setChanged();

        if (level != null
                && !level.isClientSide) {
            level.sendBlockUpdated(
                    getBlockPos(),
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }

        return true;
    }

    public void disconnectAllTransitDevices() {
        lanConnection = null;
        wanConnection = null;
        setChanged();

        if (level != null
                && !level.isClientSide) {
            level.sendBlockUpdated(
                    getBlockPos(),
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    public void w1161ClearCounters() {
        w1161LanRx = 0L;
        w1161WanRx = 0L;
        w1161LanTx = 0L;
        w1161WanTx = 0L;
        w1161Drops = 0L;
        w1161TtlExpired = 0L;
        w1161LastTransit = "READY";
        setChanged();
    }

    public String w1161TransitStatus() {
        return "W1.16.1 TRANSIT"
                + " | LAN="
                + (
                lanConnection == null
                        ? "NONE"
                        : lanConnection.toShortString()
        )
                + " | WAN="
                + (
                wanConnection == null
                        ? "NONE"
                        : wanConnection.toShortString()
        )
                + " | lanRx="
                + w1161LanRx
                + " | wanRx="
                + w1161WanRx
                + " | lanTx="
                + w1161LanTx
                + " | wanTx="
                + w1161WanTx
                + " | drops="
                + w1161Drops
                + " | ttlExpired="
                + w1161TtlExpired
                + " | last="
                + w1161LastTransit;
    }

    public boolean connectDevice(BlockPos pos) {
        if (pos.equals(this.worldPosition)) return false;
        if (pos.equals(lanConnection) || pos.equals(wanConnection)) return false;

        if (lanConnection == null) {
            lanConnection = pos;
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            return true;
        } else if (wanConnection == null) {
            wanConnection = pos;
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            return true;
        }
        return false;
    }

    public boolean disconnectDevice(BlockPos pos) {
        boolean removed = false;
        if (pos.equals(lanConnection)) {
            lanConnection = null;
            removed = true;
        } else if (pos.equals(wanConnection)) {
            wanConnection = null;
            removed = true;
        }
        if (removed) {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return removed;
    }

    public List<BlockPos> getConnectedDevices() {
        List<BlockPos> connections = new ArrayList<>();
        if (lanConnection != null) connections.add(lanConnection);
        if (wanConnection != null) connections.add(wanConnection);
        return connections;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DeviceId", deviceId);
        tag.putString("DeviceName", deviceName);
        tag.putString("ManagementIP", managementIp);
        tag.putString("SubnetMask", subnetMask);
        tag.putString("Ipv6Address", ipv6Address);
        tag.putBoolean("DhcpEnabled", dhcpEnabled);
        tag.putBoolean("StrictMode", strictMode);

        if (wanConnection != null) tag.putLong("WanPos", wanConnection.asLong());
        if (lanConnection != null) tag.putLong("LanPos", lanConnection.asLong());

        tag.putLong("W1161LanRx", w1161LanRx);
        tag.putLong("W1161WanRx", w1161WanRx);
        tag.putLong("W1161LanTx", w1161LanTx);
        tag.putLong("W1161WanTx", w1161WanTx);
        tag.putLong("W1161Drops", w1161Drops);
        tag.putLong("W1161TtlExpired", w1161TtlExpired);
        tag.putString("W1161LastTransit", w1161LastTransit);

        ListTag rulesList = new ListTag();
        for (FirewallRule rule : activeRules) {
            CompoundTag rt = new CompoundTag();
            rt.putString("Name", rule.name);
            rt.putString("Action", rule.action);
            rt.putString("Source", rule.source);
            rt.putString("Dest", rule.destination);
            rt.putBoolean("Enabled", rule.enabled);
            rulesList.add(rt);
        }
        tag.put("Rules", rulesList);

        ListTag osList = new ListTag();
        for (int i = 0; i < 7; i++) {
            osList.add(osSimulators[i].saveToNBT());
        }
        tag.put("OsStates", osList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("DeviceId")) deviceId = tag.getInt("DeviceId");
        if (tag.contains("DeviceName")) deviceName = tag.getString("DeviceName");
        if (tag.contains("ManagementIP")) managementIp = tag.getString("ManagementIP");
        if (tag.contains("SubnetMask")) subnetMask = tag.getString("SubnetMask");
        if (tag.contains("Ipv6Address")) ipv6Address = tag.getString("Ipv6Address");
        if (tag.contains("DhcpEnabled")) dhcpEnabled = tag.getBoolean("DhcpEnabled");
        if (tag.contains("StrictMode")) strictMode = tag.getBoolean("StrictMode");

        if (tag.contains("WanPos")) wanConnection = BlockPos.of(tag.getLong("WanPos"));
        if (tag.contains("LanPos")) lanConnection = BlockPos.of(tag.getLong("LanPos"));

        if (tag.contains("W1161LanRx")) w1161LanRx = tag.getLong("W1161LanRx");
        if (tag.contains("W1161WanRx")) w1161WanRx = tag.getLong("W1161WanRx");
        if (tag.contains("W1161LanTx")) w1161LanTx = tag.getLong("W1161LanTx");
        if (tag.contains("W1161WanTx")) w1161WanTx = tag.getLong("W1161WanTx");
        if (tag.contains("W1161Drops")) w1161Drops = tag.getLong("W1161Drops");
        if (tag.contains("W1161TtlExpired")) w1161TtlExpired = tag.getLong("W1161TtlExpired");
        if (tag.contains("W1161LastTransit")) w1161LastTransit = tag.getString("W1161LastTransit");

        if (tag.contains("Rules", Tag.TAG_LIST)) {
            activeRules.clear();
            ListTag rulesList = tag.getList("Rules", Tag.TAG_COMPOUND);
            for (int i = 0; i < rulesList.size(); i++) {
                CompoundTag rt = rulesList.getCompound(i);
                activeRules.add(new FirewallRule(
                        rt.getString("Name"), rt.getString("Action"),
                        rt.getString("Source"), rt.getString("Dest"), rt.getBoolean("Enabled")
                ));
            }
        }

        if (tag.contains("OsStates", Tag.TAG_LIST)) {
            ListTag osList = tag.getList("OsStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < 7 && i < osList.size(); i++) {
                osSimulators[i].loadFromNBT(osList.getCompound(i));
            }
        } else if (tag.contains("OsState")) {
            // Fallback for previous saves
            osSimulators[0].loadFromNBT(tag.getCompound("OsState"));
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
        return Component.literal(deviceName);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.k1ngtle.vsia.world.inventory.FirewallMenu(containerId, playerInventory, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return software.bernie.geckolib.core.object.PlayState.STOP;
        }).triggerableAnim("installing_drive", RawAnimation.begin().thenPlay("installing_drive")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static class FirewallRule {
        public String name;
        public String action;
        public String source;
        public String destination;
        public boolean enabled;

        public FirewallRule(String name, String action, String source, String destination, boolean enabled) {
            this.name = name;
            this.action = action;
            this.source = source;
            this.destination = destination;
            this.enabled = enabled;
        }
    }
}