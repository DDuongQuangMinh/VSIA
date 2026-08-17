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
    public final SwitchOsSimulator osSimulator;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NetworkSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.NETWORK_SWITCH_BE.get(), pos, state);
        this.osSimulator = new SwitchOsSimulator(0, this.switchName, this::setChanged);
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
        this.osSimulator.id = id;
        this.osSimulator.switchHostname = this.switchName;
        this.osSimulator.macAddress = String.format("00:1A:2B:3C:4D:%02X", 0x5E + id);
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
        this.osSimulator.switchHostname = name;
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

        // Process through the Switch OS Simulator (VLANs, MAC learning, Forwarding)
        List<String> egressPorts = osSimulator.processAndForwardPacket(packet, ingressPort);

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
                    // Placeholder: Target firewall routing in future.
                }
            }
        }
    }

    // ========================================================================
    // DHCP PROPAGATION (Uplink to Rack)
    // ========================================================================

    public void propagateDhcp(ServerRackBlockEntity rack, Set<BlockPos> visited) {
        if (level == null || level.isClientSide) return;
        visited.add(this.getBlockPos());

        for (BlockPos p : connectedDevices) {
            if (visited.contains(p)) continue;
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

        tag.put("OsState", osSimulator.saveToNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SwitchName")) {
            switchName = tag.getString("SwitchName");
            osSimulator.switchHostname = switchName;
        }
        if (tag.contains("SwitchId")) {
            switchId = tag.getInt("SwitchId");
            osSimulator.id = switchId;
            osSimulator.macAddress = String.format("00:1A:2B:3C:4D:%02X", 0x5E + switchId);
        }

        connectedDevices.clear();
        if (tag.contains("Connections", Tag.TAG_LIST)) {
            ListTag links = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (int i = 0; i < links.size(); i++) {
                connectedDevices.add(BlockPos.of(links.getCompound(i).getLong("Pos")));
            }
        }

        if (tag.contains("OsState")) {
            osSimulator.loadFromNBT(tag.getCompound("OsState"));
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