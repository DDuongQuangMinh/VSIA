package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player.isCrouching()) {
            toggleMode(stack, player);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    private void toggleMode(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean isIpv6 = tag.getBoolean("IPv6Mode");

        tag.putBoolean("IPv6Mode", !isIpv6);

        String modeName = !isIpv6 ? "IPv6" : "IPv4";
        player.displayClientMessage(Component.literal("Network Cable mode set to: " + modeName).withStyle(ChatFormatting.AQUA), true);
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

        if (player.isCrouching()) {
            toggleMode(stack, player);
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

            // Validate the connection endpoints (Storage Server <-> Server Rack)
            boolean isValidConnection = false;
            StorageServerBlockEntity storageServer = null;
            ServerRackBlockEntity serverRack = null;

            if (targetEntity instanceof StorageServerBlockEntity && storedEntity instanceof ServerRackBlockEntity) {
                // Clicking Storage Server second
                storageServer = (StorageServerBlockEntity) targetEntity;
                serverRack = (ServerRackBlockEntity) storedEntity;
                storageServer.setConnectedRackPos(storedPos);
                serverRack.connectCable(pos);
                isValidConnection = true;

            } else if (storedEntity instanceof StorageServerBlockEntity && targetEntity instanceof ServerRackBlockEntity) {
                // Clicking Storage Server first, Rack second
                storageServer = (StorageServerBlockEntity) storedEntity;
                serverRack = (ServerRackBlockEntity) targetEntity;
                storageServer.setConnectedRackPos(pos);
                serverRack.connectCable(storedPos);
                isValidConnection = true;
            }

            if (isValidConnection) {
                if (storageServer.isDhcpEnabled()) {
                    boolean isIpv6 = stack.hasTag() && stack.getTag().getBoolean("IPv6Mode");

                    // Generate a pseudo-MAC address based on the Storage Server's block coordinates
                    String pseudoMac = "st:or:" + Integer.toHexString(storageServer.getBlockPos().getX() & 0xFF) + ":" +
                            Integer.toHexString(storageServer.getBlockPos().getY() & 0xFF) + ":" +
                            Integer.toHexString(storageServer.getBlockPos().getZ() & 0xFF);

                    // Request a dynamic lease from the Server Rack's internal DHCP Pool manager
                    String newIp = serverRack.requestDynamicIp(pseudoMac, isIpv6);

                    if (newIp != null) {
                        if (isIpv6) {
                            storageServer.setIpv6Address(newIp);
                            player.displayClientMessage(Component.literal("Network connection established! Assigned DHCPv6: " + newIp).withStyle(ChatFormatting.GREEN), true);
                        } else {
                            storageServer.setIpAddress(newIp);
                            player.displayClientMessage(Component.literal("Network connection established! Assigned DHCPv4: " + newIp).withStyle(ChatFormatting.GREEN), true);
                        }
                    } else {
                        player.displayClientMessage(Component.literal("Network connection established, but DHCP pool is exhausted or disabled!").withStyle(ChatFormatting.YELLOW), true);
                    }
                } else {
                    player.displayClientMessage(Component.literal("Network connection established! DHCP is OFF, using static IP.").withStyle(ChatFormatting.GREEN), true);
                }

                // Force the server to sync the BlockEntity data to the client immediately
                BlockPos serverPos = storageServer.getBlockPos();
                level.sendBlockUpdated(serverPos, level.getBlockState(serverPos), level.getBlockState(serverPos), 3);

            } else {
                player.displayClientMessage(Component.literal("Invalid connection endpoints. Requires Storage Server and Server Rack.").withStyle(ChatFormatting.RED), true);
            }

            // Always clear the tool after attempting a connection
            stack.setTag(null);

        } else {
            // 2. We are starting a connection
            if (targetEntity instanceof StorageServerBlockEntity || targetEntity instanceof ServerRackBlockEntity) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putInt("StoredX", pos.getX());
                tag.putInt("StoredY", pos.getY());
                tag.putInt("StoredZ", pos.getZ());
                player.displayClientMessage(Component.literal("Started connection. Click the target device.").withStyle(ChatFormatting.YELLOW), true);
            } else {
                player.displayClientMessage(Component.literal("Invalid start point. Click a Server Rack or Storage Server.").withStyle(ChatFormatting.RED), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.literal("Use on a Server Rack, then a Storage Server to connect them.").withStyle(ChatFormatting.GRAY));

        boolean isIpv6 = stack.hasTag() && stack.getTag().getBoolean("IPv6Mode");
        String currentMode = isIpv6 ? "IPv6" : "IPv4";
        tooltipComponents.add(Component.literal("Current Mode: " + currentMode).withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.literal("Sneak + Right-Click to toggle mode.").withStyle(ChatFormatting.DARK_GRAY));

        if (stack.hasTag() && stack.getTag().contains("StoredX")) {
            CompoundTag tag = stack.getTag();
            tooltipComponents.add(Component.literal("Stored Connection: " + tag.getInt("StoredX") + ", " + tag.getInt("StoredY") + ", " + tag.getInt("StoredZ")).withStyle(ChatFormatting.YELLOW));
        }
    }
}