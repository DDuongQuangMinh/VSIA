package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class StorageServerBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    public static final int MAX_FILE_STORAGE_BYTES = 10 * 1024 * 1024; // 10 MB limit for text files
    public static final int MAX_ITEM_CAPACITY = 600000; // Updated to 600,000 custom stack size limit
    private static final int INVENTORY_SLOTS = 360; // Huge inventory, 40 rows of 9 slots

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Network Configuration Properties
    private String ipAddress = "192.168.1.100";
    private String ipv6Address = "fe80::1";
    private String subnetMask = "255.255.255.0";
    private String gateway = "192.168.1.1";
    private boolean dhcpEnabled = true;
    private BlockPos connectedRackPos = null;

    // File Storage System
    private final List<StoredFile> storedFiles = new ArrayList<>();

    // Item Storage System
    private final ItemStackHandler itemHandler = new ItemStackHandler(INVENTORY_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_ITEM_CAPACITY;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return MAX_ITEM_CAPACITY;
        }
    };

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    public StorageServerBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.STORAGE_SERVER_BE.get(), pos, state);
        initDefaultFilesIfEmpty();
    }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; setChanged(); }

    public String getIpv6Address() { return ipv6Address; }
    public void setIpv6Address(String ipv6Address) { this.ipv6Address = ipv6Address; setChanged(); }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String subnetMask) { this.subnetMask = subnetMask; setChanged(); }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; setChanged(); }

    public boolean isDhcpEnabled() { return dhcpEnabled; }
    public void setDhcpEnabled(boolean dhcpEnabled) { this.dhcpEnabled = dhcpEnabled; setChanged(); }

    public BlockPos getConnectedRackPos() { return connectedRackPos; }
    public void setConnectedRackPos(BlockPos connectedRackPos) { this.connectedRackPos = connectedRackPos; setChanged(); }

    private void initDefaultFilesIfEmpty() {
        if (storedFiles.isEmpty()) {
            storedFiles.add(new StoredFile("readme.txt", "text", "Welcome to TrueNAS SCALE VSIA Edition.\nThis server provides robust item and script storage.\nCapacity: 9TiB Block Storage / 10MB Object Storage."));
            storedFiles.add(new StoredFile("config.json", "json", "{\n  \"server_name\": \"re-minir-102\",\n  \"pool_name\": \"tank\",\n  \"auto_sync\": true\n}"));
        }
    }

    public List<StoredFile> getStoredFiles() {
        return storedFiles;
    }

    public boolean addStoredFile(StoredFile file) {
        int currentSize = storedFiles.stream().mapToInt(StoredFile::getSizeInBytes).sum();
        if (currentSize + file.getSizeInBytes() > MAX_FILE_STORAGE_BYTES) {
            return false;
        }
        storedFiles.add(file);
        setChanged();
        return true;
    }

    public void removeStoredFile(StoredFile file) {
        storedFiles.remove(file);
        setChanged();
    }

    public static int getRealCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.hasTag() && stack.getTag().contains("VSIA_Count")) {
            return stack.getTag().getInt("VSIA_Count");
        }
        return stack.getCount();
    }

    public static void setRealCount(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            stack.setCount(0);
            if (stack.hasTag()) {
                stack.getTag().remove("VSIA_Count");
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
            return;
        }

        stack.setCount(1); // Vanilla stack size limit workaround
        stack.getOrCreateTag().putInt("VSIA_Count", count);
    }

    public static boolean canMergeItems(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.getItem() != b.getItem()) return false;
        if (a.getDamageValue() != b.getDamageValue()) return false;

        // Custom NBT comparison ignoring our VSIA_Count tag
        CompoundTag tagA = a.hasTag() ? a.getTag().copy() : new CompoundTag();
        CompoundTag tagB = b.hasTag() ? b.getTag().copy() : new CompoundTag();
        tagA.remove("VSIA_Count");
        tagB.remove("VSIA_Count");

        return tagA.equals(tagB);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());

        ListTag fileList = new ListTag();
        for (StoredFile file : storedFiles) {
            fileList.add(file.serializeNBT());
        }
        tag.put("StoredFiles", fileList);

        tag.putString("IPAddress", ipAddress);
        tag.putString("IPv6Address", ipv6Address);
        tag.putString("SubnetMask", subnetMask);
        tag.putString("Gateway", gateway);
        tag.putBoolean("DHCP", dhcpEnabled);

        if (connectedRackPos != null) {
            tag.putLong("ConnectedRack", connectedRackPos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }

        storedFiles.clear();
        if (tag.contains("StoredFiles", Tag.TAG_LIST)) {
            ListTag fileList = tag.getList("StoredFiles", Tag.TAG_COMPOUND);
            for (int i = 0; i < fileList.size(); i++) {
                storedFiles.add(StoredFile.deserializeNBT(fileList.getCompound(i)));
            }
        } else {
            initDefaultFilesIfEmpty();
        }

        if (tag.contains("IPAddress")) ipAddress = tag.getString("IPAddress");
        if (tag.contains("IPv6Address")) ipv6Address = tag.getString("IPv6Address");
        if (tag.contains("SubnetMask")) subnetMask = tag.getString("SubnetMask");
        if (tag.contains("Gateway")) gateway = tag.getString("Gateway");
        if (tag.contains("DHCP")) dhcpEnabled = tag.getBoolean("DHCP");

        if (tag.contains("ConnectedRack")) {
            connectedRackPos = BlockPos.of(tag.getLong("ConnectedRack"));
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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.vsia.storage_server");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageServerMenu(containerId, playerInventory, this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenPlay("checking"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}