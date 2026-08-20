package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.world.inventory.NetworkSwitchMenu;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkSwitchBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    public static final int MAX_PORTS = 26; // 24 FE + 2 GE
    private final List<BlockPos> connectedDevices = new ArrayList<>();

    private String switchName = "Switch0";
    private int switchId = -1;

    // Internal L2 Switching Engine
    public final SwitchOsSimulator[] osSimulators = new SwitchOsSimulator[7];

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NetworkSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.NETWORK_SWITCH_BE.get(), pos, state);
        for (int i = 0; i < 7; i++) {
            this.osSimulators[i] = new SwitchOsSimulator(0, this.switchName + "_" + (i + 1), this::setChanged);
        }
    }

    public void tick() {
        if (level != null && !level.isClientSide) {
            // Forward ticking into our advanced STP/L2 simulation engines
            for (SwitchOsSimulator sim : osSimulators) {
                sim.tick(this::broadcastPacketOutwards);
            }
        }
    }

    private void broadcastPacketOutwards(OSINetworkPacket packet) {
        // Internal helper for the SwitchOsSimulator to emit dynamic BPDUs to cables
        for (BlockPos p : connectedDevices) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof NetworkSwitchBlockEntity sw) {
                sw.receiveWiredPacket(packet, this.worldPosition);
            }
        }
    }

    public List<BlockPos> getConnectedDevices() {
        return connectedDevices;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && switchId == -1) {
            assignAutomaticId(ServerRackIdSavedData.allocate(serverLevel));
        }
    }

    public void assignAutomaticId(int id) {
        if (this.switchId != -1) return;
        this.switchId = id;
        this.switchName = "Switch" + id;
        for (int i = 0; i < 7; i++) {
            this.osSimulators[i].id = id * 10 + i;
            this.osSimulators[i].switchHostname = this.switchName + "_" + (i + 1);
            this.osSimulators[i].macAddress = String.format("00:1A:2B:3C:%02X:%02X", id, i + 1);
        }
        setChanged();
    }

    public int getSwitchId() {
        return switchId;
    }

    public String getSwitchName() {
        return switchName;
    }

    public void setSwitchName(String name) {
        this.switchName = name;
        setChanged();
    }

    // ========================================================================
    // CABLE CONNECTIONS & PORT MAPPING
    // ========================================================================

    public boolean connectDevice(BlockPos pos) {
        if (connectedDevices.size() >= MAX_PORTS || connectedDevices.contains(pos) || pos.equals(this.worldPosition)) {
            return false;
        }
        connectedDevices.add(pos);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public boolean disconnectDevice(BlockPos pos) {
        boolean removed = connectedDevices.remove(pos);
        if (removed) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
        return removed;
    }

    private String getInterfaceNameForPos(BlockPos pos) {
        int index = connectedDevices.indexOf(pos);
        if (index == -1) return null;
        if (index < 24) return "FastEthernet0/" + (index + 1);
        else return "GigabitEthernet0/" + (index - 23);
    }

    private BlockPos getPosForInterfaceName(String name) {
        int index = -1;
        if (name.startsWith("FastEthernet0/")) {
            index = Integer.parseInt(name.substring(14)) - 1;
        } else if (name.startsWith("GigabitEthernet0/")) {
            index = Integer.parseInt(name.substring(17)) + 23;
        }
        if (index >= 0 && index < connectedDevices.size()) {
            return connectedDevices.get(index);
        }
        return null;
    }

    // ========================================================================
    // LAYER 2 PACKET FORWARDING
    // ========================================================================

    public void receiveWiredPacket(OSINetworkPacket packet, BlockPos ingressPos) {
        if (level == null || level.isClientSide) return;

        String ingressPort = getInterfaceNameForPos(ingressPos);
        if (ingressPort == null) return;

        // Process through the Primary Switch OS Simulator (VLANs, MAC learning, Forwarding, STP)
        List<String> egressPorts = osSimulators[0].processAndForwardPacket(packet, ingressPort);

        for (String egressPort : egressPorts) {
            BlockPos targetPos = getPosForInterfaceName(egressPort);
            if (targetPos != null) {
                BlockEntity be = level.getBlockEntity(targetPos);

                // Clone the packet if we are flooding to prevent cross-reference bugs
                OSINetworkPacket forwardedPacket = egressPorts.size() > 1 ?
                        OSINetworkPacket.deserializeNBT(packet.serializeNBT().copy()) : packet;

                if (be instanceof ServerRackBlockEntity rack) {
                    rack.receiveWiredPacket(forwardedPacket);
                } else if (be instanceof NetworkSwitchBlockEntity sw) {
                    sw.receiveWiredPacket(forwardedPacket, this.worldPosition);
                } else if (be instanceof FirewallBlockEntity fw) {
                    fw.receiveWiredPacket(forwardedPacket, this.worldPosition);
                }
            }
        }
    }

    public boolean w1161InjectProbeToward(
            BlockPos targetPos,
            OSINetworkPacket packet
    ) {
        if (level == null
                || level.isClientSide
                || targetPos == null
                || packet == null) {
            return false;
        }

        String targetPort =
                getInterfaceNameForPos(
                        targetPos
                );

        if (targetPort == null) {
            return false;
        }

        SwitchOsSimulator.PortConfig targetConfig =
                osSimulators[0]
                        .portConfigs
                        .get(targetPort);

        if (targetConfig == null
                || !targetConfig.up
                || !"FWD".equals(targetConfig.stpState)) {
            return false;
        }

        String syntheticIngress = null;

        for (String candidate :
                osSimulators[0].portConfigs.keySet()) {
            if (candidate.equals(targetPort)) {
                continue;
            }

            SwitchOsSimulator.PortConfig candidateConfig =
                    osSimulators[0]
                            .portConfigs
                            .get(candidate);

            if (candidateConfig == null
                    || !candidateConfig.up
                    || !"FWD".equals(candidateConfig.stpState)) {
                continue;
            }

            if (!candidateConfig.accessVlan.equals(
                    targetConfig.accessVlan
            )) {
                continue;
            }

            syntheticIngress = candidate;
            break;
        }

        if (syntheticIngress == null) {
            return false;
        }

        OSINetworkPacket probe =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        List<String> egressPorts =
                osSimulators[0]
                        .processAndForwardPacket(
                                probe,
                                syntheticIngress
                        );

        if (!egressPorts.contains(targetPort)) {
            return false;
        }

        BlockEntity be =
                level.getBlockEntity(
                        targetPos
                );

        if (be instanceof FirewallBlockEntity fw) {
            fw.receiveWiredPacket(
                    probe,
                    this.worldPosition
            );
            return true;
        }

        if (be instanceof ServerRackBlockEntity rack) {
            rack.receiveWiredPacket(
                    probe
            );
            return true;
        }

        if (be instanceof NetworkSwitchBlockEntity sw) {
            sw.receiveWiredPacket(
                    probe,
                    this.worldPosition
            );
            return true;
        }

        return false;
    }

    // ========================================================================
    // DHCP PROPAGATION (Uplink to Rack)
    // ========================================================================

    public void propagateDhcp(ServerRackBlockEntity rack, Set<BlockPos> visited) {
        if (level == null || level.isClientSide) return;
        visited.add(this.getBlockPos());

        for (int i = 0; i < connectedDevices.size(); i++) {
            BlockPos p = connectedDevices.get(i);
            if (visited.contains(p)) continue;

            String portName = getInterfaceNameForPos(p);
            if (portName != null) {
                SwitchOsSimulator.PortConfig pc = osSimulators[0].portConfigs.get(portName);
                if (pc != null && (!pc.up || pc.stpState.equals("BLK"))) {
                    continue; // Accurately skip propagation if port is shut down or blocked by STP!
                }
            }

            BlockEntity be = level.getBlockEntity(p);

            if (be instanceof StorageServerBlockEntity storage) {
                if (storage.isDhcpEnabled()) {
                    String ip = rack.requestDynamicIp("vsia:storage_server_" + p.asLong(), false);
                    if (ip != null) {
                        storage.setIpAddress(ip);
                        storage.setSubnetMask(rack.subnetMask());
                        storage.setGateway(rack.gatewayIp());
                    }
                    String ipv6 = rack.requestDynamicIp("vsia:storage_server_" + p.asLong(), true);
                    if (ipv6 != null) {
                        storage.setIpv6Address(ipv6);
                    }
                }
            } else if (be instanceof FirewallBlockEntity fw) {
                if (fw.isDhcpEnabled()) {
                    String ip = rack.requestDynamicIp("vsia:firewall_" + p.asLong(), false);
                    if (ip != null) {
                        fw.setManagementIp(ip);
                        fw.setSubnetMask(rack.subnetMask());
                    }
                    String ipv6 = rack.requestDynamicIp("vsia:firewall_" + p.asLong(), true);
                    if (ipv6 != null) {
                        fw.setIpv6Address(ipv6);
                    }
                    if (ip != null || ipv6 != null) {
                        level.sendBlockUpdated(p, fw.getBlockState(), fw.getBlockState(), 3);
                    }
                }
            } else if (be instanceof NetworkSwitchBlockEntity netSwitch) {
                netSwitch.propagateDhcp(rack, visited);
            }
        }
    }

    public void refreshNetworkDhcp(Set<BlockPos> visited) {
        if (level == null || level.isClientSide) return;
        ServerRackBlockEntity rack = findFirstRack(new HashSet<>());
        if (rack != null) {
            propagateDhcp(rack, new HashSet<>());
        }
    }

    public ServerRackBlockEntity findFirstRack(Set<BlockPos> visited) {
        if (level == null || level.isClientSide) return null;
        visited.add(this.getBlockPos());
        for (BlockPos p : connectedDevices) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof ServerRackBlockEntity r && r.dhcpEnabled()) return r;
            if (be instanceof NetworkSwitchBlockEntity sw && !visited.contains(p)) {
                ServerRackBlockEntity r = sw.findFirstRack(visited);
                if (r != null) return r;
            }
        }
        return null;
    }

    // ========================================================================
    // NBT & SERIALIZATION
    // ========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("SwitchName", switchName);
        tag.putInt("SwitchId", switchId);

        ListTag links = new ListTag();
        for (BlockPos p : connectedDevices) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", p.asLong());
            links.add(entry);
        }
        tag.put("Connections", links);

        ListTag osList = new ListTag();
        for (int i = 0; i < 7; i++) {
            osList.add(osSimulators[i].saveToNBT());
        }
        tag.put("OsStates", osList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SwitchName")) {
            switchName = tag.getString("SwitchName");
            for (int i = 0; i < 7; i++) osSimulators[i].switchHostname = switchName + "_" + (i + 1);
        }
        if (tag.contains("SwitchId")) {
            switchId = tag.getInt("SwitchId");
            for (int i = 0; i < 7; i++) {
                osSimulators[i].id = switchId * 10 + i;
                osSimulators[i].macAddress = String.format("00:1A:2B:3C:%02X:%02X", switchId, i + 1);
            }
        }

        connectedDevices.clear();
        if (tag.contains("Connections", Tag.TAG_LIST)) {
            ListTag links = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (int i = 0; i < links.size(); i++) {
                connectedDevices.add(BlockPos.of(links.getCompound(i).getLong("Pos")));
            }
        }

        if (tag.contains("OsStates", Tag.TAG_LIST)) {
            ListTag osList = tag.getList("OsStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < 7 && i < osList.size(); i++) {
                osSimulators[i].loadFromNBT(osList.getCompound(i));
            }
        } else if (tag.contains("OsState")) {
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
        return Component.literal(switchName);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkSwitchMenu(containerId, playerInventory, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}