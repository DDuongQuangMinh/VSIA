package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NetworkCableItem extends Item {

    public NetworkCableItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private boolean isNetworkDevice(BlockEntity be) {
        return be instanceof StorageServerBlockEntity ||
                be instanceof ServerRackBlockEntity ||
                be instanceof NetworkSwitchBlockEntity;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity targetEntity = level.getBlockEntity(pos);

        // 1. If the item already has a stored position (we are finishing a connection)
        if (stack.hasTag() && stack.getTag().contains("StoredX")) {
            CompoundTag tag = stack.getTag();
            BlockPos storedPos = new BlockPos(tag.getInt("StoredX"), tag.getInt("StoredY"), tag.getInt("StoredZ"));

            if (storedPos.equals(pos)) {
                player.displayClientMessage(Component.literal("Cannot connect a device to itself.").withStyle(ChatFormatting.RED), true);
                stack.setTag(null); // Clear the stored position
                return InteractionResult.FAIL;
            }

            BlockEntity storedEntity = level.getBlockEntity(storedPos);

            // Validate the connection endpoints (Storage Server <-> Server Rack <-> Switch)
            boolean isValidConnection = false;
            StorageServerBlockEntity storageServer = null;
            ServerRackBlockEntity serverRack = null;
            NetworkSwitchBlockEntity netSwitch = null;

            if (targetEntity instanceof StorageServerBlockEntity && storedEntity instanceof ServerRackBlockEntity) {
                storageServer = (StorageServerBlockEntity) targetEntity;
                serverRack = (ServerRackBlockEntity) storedEntity;
                storageServer.setConnectedRackPos(storedPos);
                serverRack.connectCable(pos);
                isValidConnection = true;

            } else if (storedEntity instanceof StorageServerBlockEntity && targetEntity instanceof ServerRackBlockEntity) {
                storageServer = (StorageServerBlockEntity) storedEntity;
                serverRack = (ServerRackBlockEntity) targetEntity;
                storageServer.setConnectedRackPos(pos);
                serverRack.connectCable(storedPos);
                isValidConnection = true;

            } else if (targetEntity instanceof NetworkSwitchBlockEntity && storedEntity instanceof ServerRackBlockEntity) {
                netSwitch = (NetworkSwitchBlockEntity) targetEntity;
                serverRack = (ServerRackBlockEntity) storedEntity;
                netSwitch.connectDevice(storedPos);
                serverRack.connectCable(pos);
                isValidConnection = true;

            } else if (storedEntity instanceof NetworkSwitchBlockEntity && targetEntity instanceof ServerRackBlockEntity) {
                netSwitch = (NetworkSwitchBlockEntity) storedEntity;
                serverRack = (ServerRackBlockEntity) targetEntity;
                netSwitch.connectDevice(pos);
                serverRack.connectCable(storedPos);
                isValidConnection = true;

            } else if (targetEntity instanceof NetworkSwitchBlockEntity && storedEntity instanceof StorageServerBlockEntity) {
                netSwitch = (NetworkSwitchBlockEntity) targetEntity;
                storageServer = (StorageServerBlockEntity) storedEntity;
                netSwitch.connectDevice(storedPos);
                storageServer.setConnectedRackPos(pos);
                isValidConnection = true;

            } else if (storedEntity instanceof NetworkSwitchBlockEntity && targetEntity instanceof StorageServerBlockEntity) {
                netSwitch = (NetworkSwitchBlockEntity) storedEntity;
                storageServer = (StorageServerBlockEntity) targetEntity;
                netSwitch.connectDevice(pos);
                storageServer.setConnectedRackPos(storedPos);
                isValidConnection = true;
            } else if (targetEntity instanceof ServerRackBlockEntity && storedEntity instanceof ServerRackBlockEntity) {
                // Rack to Rack backbone connection
                serverRack = (ServerRackBlockEntity) targetEntity;
                ServerRackBlockEntity serverRack2 = (ServerRackBlockEntity) storedEntity;
                serverRack.connectCable(storedPos);
                serverRack2.connectCable(pos);
                isValidConnection = true;
            } else if (targetEntity instanceof NetworkSwitchBlockEntity && storedEntity instanceof NetworkSwitchBlockEntity) {
                // Switch to Switch uplink connection
                netSwitch = (NetworkSwitchBlockEntity) targetEntity;
                NetworkSwitchBlockEntity netSwitch2 = (NetworkSwitchBlockEntity) storedEntity;
                netSwitch.connectDevice(storedPos);
                netSwitch2.connectDevice(pos);
                isValidConnection = true;
            }

            if (isValidConnection) {
                // Logic to request a DHCP IP if connecting a storage server to a rack
                if (storageServer != null && serverRack != null && storageServer.isDhcpEnabled()) {
                    String assignedIp = serverRack.requestDynamicIp("vsia:storage_server_" + storageServer.getBlockPos().asLong(), false);
                    if (assignedIp != null) {
                        storageServer.setIpAddress(assignedIp);
                        player.displayClientMessage(Component.literal("Network connection established! DHCP IP assigned: " + assignedIp).withStyle(ChatFormatting.GREEN), true);
                    } else {
                        player.displayClientMessage(Component.literal("Network connection established! (DHCP Pool exhausted, using static IP)").withStyle(ChatFormatting.YELLOW), true);
                    }
                } else if (netSwitch != null) {
                    player.displayClientMessage(Component.literal("Network connection established to Switch!").withStyle(ChatFormatting.GREEN), true);
                    BlockPos switchPos = netSwitch.getBlockPos();
                    level.sendBlockUpdated(switchPos, level.getBlockState(switchPos), level.getBlockState(switchPos), 3);
                    // Also update the other switch if it was a switch-to-switch connection
                    if (storedEntity instanceof NetworkSwitchBlockEntity) {
                        level.sendBlockUpdated(storedPos, level.getBlockState(storedPos), level.getBlockState(storedPos), 3);
                    }
                } else {
                    player.displayClientMessage(Component.literal("Network connection established! (DHCP OFF, using static IP)").withStyle(ChatFormatting.GREEN), true);
                }
            } else {
                player.displayClientMessage(Component.literal("Invalid connection endpoints. Requires Storage Server, Server Rack, or Switch.").withStyle(ChatFormatting.RED), true);
            }

            // Always clear the tool after attempting a connection
            stack.setTag(null);

        } else {
            // 2. We are starting a connection
            if (isNetworkDevice(targetEntity)) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putInt("StoredX", pos.getX());
                tag.putInt("StoredY", pos.getY());
                tag.putInt("StoredZ", pos.getZ());
                player.displayClientMessage(Component.literal("Started connection. Click the target device.").withStyle(ChatFormatting.YELLOW), true);
            } else {
                player.displayClientMessage(Component.literal("Invalid start point. Click a Server Rack, Storage Server, or Switch.").withStyle(ChatFormatting.RED), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.literal("Use on network devices (Rack, Server, Switch) to connect them.").withStyle(ChatFormatting.GRAY));

        if (stack.hasTag() && stack.getTag().contains("StoredX")) {
            CompoundTag tag = stack.getTag();
            tooltipComponents.add(Component.literal("Stored Connection: " + tag.getInt("StoredX") + ", " + tag.getInt("StoredY") + ", " + tag.getInt("StoredZ")).withStyle(ChatFormatting.YELLOW));
        }
    }
}