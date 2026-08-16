package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
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

    public static final int MAX_PORTS = 24;
    private final List<BlockPos> connectedDevices = new ArrayList<>();
    private String switchName = "Core Switch 1";
    private int switchId = -1;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NetworkSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.NETWORK_SWITCH_BE.get(), pos, state);
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
        setChanged();
    }

    public int getSwitchId() {
        return switchId;
    }

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

    public String getSwitchName() {
        return switchName;
    }

    public void setSwitchName(String name) {
        this.switchName = name;
        setChanged();
    }

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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SwitchName")) {
            switchName = tag.getString("SwitchName");
        }
        if (tag.contains("SwitchId")) {
            switchId = tag.getInt("SwitchId");
        }

        connectedDevices.clear();
        if (tag.contains("Connections", Tag.TAG_LIST)) {
            ListTag links = tag.getList("Connections", Tag.TAG_COMPOUND);
            for (int i = 0; i < links.size(); i++) {
                connectedDevices.add(BlockPos.of(links.getCompound(i).getLong("Pos")));
            }
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
        // Add animations here later if the switch requires moving parts
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}