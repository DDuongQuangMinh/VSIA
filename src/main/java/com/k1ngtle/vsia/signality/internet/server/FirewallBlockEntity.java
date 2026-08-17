package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
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
            // Forward ticking into our advanced OSPF/IPsec simulators for all 7 instances
            for (FirewallOsSimulator sim : osSimulators) {
                sim.tick(this::broadcastPacketOutwards);
            }
        }
    }

    private void broadcastPacketOutwards(OSINetworkPacket packet) {
        // Find correct egress port based on routing table
        // This is a simplified interface hook for the OS simulator
        if (lanConnection != null) {
            BlockEntity be = level.getBlockEntity(lanConnection);
            if (be instanceof NetworkSwitchBlockEntity sw) sw.receiveWiredPacket(packet, this.worldPosition);
            if (be instanceof ServerRackBlockEntity rack) rack.receiveWiredPacket(packet);
        }
        if (wanConnection != null) {
            BlockEntity be = level.getBlockEntity(wanConnection);
            if (be instanceof NetworkSwitchBlockEntity sw) sw.receiveWiredPacket(packet, this.worldPosition);
            if (be instanceof ServerRackBlockEntity rack) rack.receiveWiredPacket(packet);
        }
    }

    public void receiveWiredPacket(OSINetworkPacket packet, BlockPos ingressPos) {
        if (level == null || level.isClientSide) return;

        String ingressPort = ingressPos.equals(lanConnection) ? "GigabitEthernet1/1" :
                ingressPos.equals(wanConnection) ? "GigabitEthernet1/2" : null;
        if (ingressPort == null) return;

        // Apply Stateful Packet Inspection & VPN decryption/encryption via primary blade
        OSINetworkPacket filtered = osSimulators[0].filterAndRoutePacket(packet, ingressPort);
        if (filtered != null) {
            broadcastPacketOutwards(filtered);
        }
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
        for (int i = 0; i < 7; i++) {
            this.osSimulators[i].hostname = name + "_" + (i + 1);
        }
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