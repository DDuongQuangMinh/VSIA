package com.k1ngtle.vsia.cockpit.program;

import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import com.k1ngtle.vsia.item.DisplayHardDriveItem;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One persisted physical slot per placed laptop. Source is sent only to an authorized editor. */
public final class DisplayLaptopBlockEntity extends BlockEntity {
    private UUID laptopId = UUID.randomUUID();
    private ItemStack drive = ItemStack.EMPTY;

    public DisplayLaptopBlockEntity(BlockPos pos, BlockState state) {
        super(DisplayLaptopRegistry.LAPTOP_ENTITY.get(), pos, state);
    }
    public UUID laptopId() { return laptopId; }
    public ItemStack displayDrive() { return drive; }
    public boolean hasDrive() { return DisplayHardDriveItem.isDisplayDrive(drive); }

    public boolean canUse(ServerPlayer player) {
        boolean loaded = level != null && level.hasChunkAt(worldPosition);
        if (!loaded || isRemoved() || level != player.serverLevel() || level.getBlockEntity(worldPosition) != this) return false;
        return DisplayLaptopSessionRules.accessible(laptopId, laptopId, true, loaded,
                !player.isSpectator() && player.getAbilities().mayBuild && level.mayInteract(player, worldPosition),
                player.position().distanceToSqr(F35VsShipHelper.blockWorldPosition(this)));
    }

    public boolean insert(ItemStack held) {
        if (level == null || level.isClientSide || hasDrive() || !DisplayHardDriveItem.isDisplayDrive(held)) return false;
        drive = held.copy();
        drive.setCount(1);
        DisplayHardDriveItem.ensureDriveId(drive);
        sync();
        return true;
    }
    public ItemStack takeDrive() {
        if (level == null || level.isClientSide) return ItemStack.EMPTY;
        ItemStack result = drive;
        drive = ItemStack.EMPTY;
        sync(); // Empty the slot before returning the item, invalidating old sessions.
        return result;
    }
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** No program source/layout or arbitrary item NBT in public chunk/update packets. */
    public static ItemStack publicDrive(ItemStack original) {
        if (!DisplayHardDriveItem.isDisplayDrive(original)) return ItemStack.EMPTY;
        ItemStack visual = new ItemStack(original.getItem(), 1);
        CompoundTag tag = visual.getOrCreateTag();
        UUID id = DisplayHardDriveItem.driveId(original);
        if (id != null) tag.putUUID("VsiaDriveId", id);
        tag.putLong("VsiaDriveRevision", DisplayHardDriveItem.revision(original));
        String name = DisplayHardDriveItem.programName(original);
        tag.putString("VsiaDisplayName", name.substring(0, Math.min(48, name.length())));
        return visual;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("VsiaLaptopId", laptopId);
        if (hasDrive()) tag.put("LaptopDrive", drive.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("VsiaLaptopId")) laptopId = tag.getUUID("VsiaLaptopId");
        ItemStack stored = ItemStack.of(tag.getCompound(tag.contains("LaptopDrive") ? "LaptopDrive" : "DriveVisual"));
        drive = DisplayHardDriveItem.isDisplayDrive(stored) ? stored : ItemStack.EMPTY;
        if (!drive.isEmpty()) drive.setCount(1);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("VsiaLaptopId", laptopId);
        if (hasDrive()) tag.put("DriveVisual", publicDrive(drive).save(new CompoundTag()));
        return tag;
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
