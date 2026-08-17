package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
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
    private String managementIp = "192.168.1.1";
    private String deviceName = "ciscoasa";
    private boolean strictMode = true;

    // Connections
    private BlockPos wanConnection = null;
    private BlockPos lanConnection = null;

    // Rules
    private final List<FirewallRule> activeRules = new ArrayList<>();

    public FirewallBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.FIREWALL_BE.get(), pos, state);
        if (activeRules.isEmpty()) {
            activeRules.add(new FirewallRule("Block Suspicious Traffic", "DROP", "ANY", "WAN", true));
            activeRules.add(new FirewallRule("Allow LAN Outbound", "ALLOW", "LAN", "WAN", true));
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
        this.deviceName = "ASA" + id;
        setChanged();
    }

    public int getDeviceId() { return deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String name) { this.deviceName = name; setChanged(); }

    public String getManagementIp() { return managementIp; }
    public void setManagementIp(String ip) { this.managementIp = ip; setChanged(); }

    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; setChanged(); }

    public List<FirewallRule> getRules() { return activeRules; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DeviceId", deviceId);
        tag.putString("DeviceName", deviceName);
        tag.putString("ManagementIP", managementIp);
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("DeviceId")) deviceId = tag.getInt("DeviceId");
        if (tag.contains("DeviceName")) deviceName = tag.getString("DeviceName");
        if (tag.contains("ManagementIP")) managementIp = tag.getString("ManagementIP");
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