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
                be instanceof NetworkSwitchBlockEntity ||
                be instanceof FirewallBlockEntity;
    }

    private boolean isSwitchPortUp(NetworkSwitchBlockEntity netSwitch, BlockPos connectedPos) {
        List<BlockPos> devices = netSwitch.getConnectedDevices();
        int index = devices.indexOf(connectedPos);
        if (index == -1) return false;
        String portName = index < 24 ? "FastEthernet0/" + (index + 1) : "GigabitEthernet0/" + (index - 23);
        SwitchOsSimulator.PortConfig pc = netSwitch.osSimulators[0].portConfigs.get(portName);
        return pc != null && pc.up;
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
            FirewallBlockEntity firewall = null;

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
            } else if (targetEntity instanceof FirewallBlockEntity && storedEntity instanceof ServerRackBlockEntity) {
                firewall = (FirewallBlockEntity) targetEntity;
                serverRack = (ServerRackBlockEntity) storedEntity;
                if (firewall.connectDevice(storedPos)) {
                    serverRack.connectCable(pos);
                    isValidConnection = true;
                }
            } else if (storedEntity instanceof FirewallBlockEntity && targetEntity instanceof ServerRackBlockEntity) {
                firewall = (FirewallBlockEntity) storedEntity;
                serverRack = (ServerRackBlockEntity) targetEntity;
                if (firewall.connectDevice(pos)) {
                    serverRack.connectCable(storedPos);
                    isValidConnection = true;
                }
            } else if (targetEntity instanceof FirewallBlockEntity && storedEntity instanceof NetworkSwitchBlockEntity) {
                firewall = (FirewallBlockEntity) targetEntity;
                netSwitch = (NetworkSwitchBlockEntity) storedEntity;
                if (firewall.connectDevice(storedPos)) {
                    netSwitch.connectDevice(pos);
                    isValidConnection = true;
                }
            } else if (storedEntity instanceof FirewallBlockEntity && targetEntity instanceof NetworkSwitchBlockEntity) {
                firewall = (FirewallBlockEntity) storedEntity;
                netSwitch = (NetworkSwitchBlockEntity) targetEntity;
                if (firewall.connectDevice(pos)) {
                    netSwitch.connectDevice(storedPos);
                    isValidConnection = true;
                }
            } else if (targetEntity instanceof FirewallBlockEntity && storedEntity instanceof StorageServerBlockEntity) {
                firewall = (FirewallBlockEntity) targetEntity;
                storageServer = (StorageServerBlockEntity) storedEntity;
                if (firewall.connectDevice(storedPos)) {
                    storageServer.setConnectedRackPos(pos);
                    isValidConnection = true;
                }
            } else if (storedEntity instanceof FirewallBlockEntity && targetEntity instanceof StorageServerBlockEntity) {
                firewall = (FirewallBlockEntity) storedEntity;
                storageServer = (StorageServerBlockEntity) targetEntity;
                if (firewall.connectDevice(pos)) {
                    storageServer.setConnectedRackPos(storedPos);
                    isValidConnection = true;
                }
            } else if (targetEntity instanceof FirewallBlockEntity && storedEntity instanceof FirewallBlockEntity) {
                firewall = (FirewallBlockEntity) targetEntity;
                FirewallBlockEntity firewall2 = (FirewallBlockEntity) storedEntity;
                if (firewall.connectDevice(storedPos)) {
                    firewall2.connectDevice(pos);
                    isValidConnection = true;
                }
            }

            if (isValidConnection) {
                boolean handledDhcp = false;

                // Storage Server DHCP Logic
                if (storageServer != null && storageServer.isDhcpEnabled()) {
                    boolean portUp = true;
                    if (netSwitch != null) portUp = isSwitchPortUp(netSwitch, storageServer.getBlockPos());

                    if (portUp) {
                        ServerRackBlockEntity rackToUse = serverRack;
                        if (rackToUse == null && netSwitch != null) rackToUse = netSwitch.findFirstRack(new java.util.HashSet<>());

                        if (rackToUse != null) {
                            String assignedIp = rackToUse.requestDynamicIp("vsia:storage_server_" + storageServer.getBlockPos().asLong(), false);
                            if (assignedIp != null) {
                                storageServer.setIpAddress(assignedIp);
                                storageServer.setSubnetMask(rackToUse.subnetMask());
                                storageServer.setGateway(rackToUse.gatewayIp());
                            }
                            String assignedIpv6 = rackToUse.requestDynamicIp("vsia:storage_server_" + storageServer.getBlockPos().asLong(), true);
                            if (assignedIpv6 != null) storageServer.setIpv6Address(assignedIpv6);
                            handledDhcp = true;
                            player.displayClientMessage(Component.literal("Network connection established! DHCP IP assigned: " + assignedIp).withStyle(ChatFormatting.GREEN), true);
                        }
                    } else {
                        handledDhcp = true;
                        player.displayClientMessage(Component.literal("Network connection established! (Switch port is administratively down)").withStyle(ChatFormatting.YELLOW), true);
                    }
                }
                // Firewall DHCP Logic
                else if (firewall != null && firewall.isDhcpEnabled()) {
                    boolean portUp = true;
                    if (netSwitch != null) portUp = isSwitchPortUp(netSwitch, firewall.getBlockPos());

                    if (portUp) {
                        ServerRackBlockEntity rackToUse = serverRack;
                        if (rackToUse == null && netSwitch != null) rackToUse = netSwitch.findFirstRack(new java.util.HashSet<>());

                        if (rackToUse != null) {
                            String assignedIp = rackToUse.requestDynamicIp("vsia:firewall_" + firewall.getBlockPos().asLong(), false);
                            if (assignedIp != null) {
                                firewall.setManagementIp(assignedIp);
                                firewall.setSubnetMask(rackToUse.subnetMask());
                            }
                            String assignedIpv6 = rackToUse.requestDynamicIp("vsia:firewall_" + firewall.getBlockPos().asLong(), true);
                            if (assignedIpv6 != null) firewall.setIpv6Address(assignedIpv6);
                            handledDhcp = true;
                            player.displayClientMessage(Component.literal("Network connection established! DHCP assigned to Firewall: " + (assignedIp != null ? assignedIp : assignedIpv6)).withStyle(ChatFormatting.GREEN), true);
                        }
                    } else {
                        handledDhcp = true;
                        player.displayClientMessage(Component.literal("Network connection established! (Switch port is administratively down)").withStyle(ChatFormatting.YELLOW), true);
                    }
                }

                // Switch and Rack propagation logic
                if (serverRack != null && netSwitch != null) {
                    if (isSwitchPortUp(netSwitch, serverRack.getBlockPos())) {
                        netSwitch.refreshNetworkDhcp(new java.util.HashSet<>());
                        player.displayClientMessage(Component.literal("Uplink established. Transmitting DHCP to downstream devices...").withStyle(ChatFormatting.GREEN), true);
                    } else {
                        player.displayClientMessage(Component.literal("Uplink established. (Switch port is administratively down)").withStyle(ChatFormatting.YELLOW), true);
                    }
                } else if (targetEntity instanceof NetworkSwitchBlockEntity && storedEntity instanceof NetworkSwitchBlockEntity) {
                    NetworkSwitchBlockEntity tSwitch = (NetworkSwitchBlockEntity) targetEntity;
                    NetworkSwitchBlockEntity sSwitch = (NetworkSwitchBlockEntity) storedEntity;
                    if (isSwitchPortUp(tSwitch, storedPos) && isSwitchPortUp(sSwitch, pos)) {
                        tSwitch.refreshNetworkDhcp(new java.util.HashSet<>());
                        player.displayClientMessage(Component.literal("Switch to Switch connection established. Refreshing DHCP...").withStyle(ChatFormatting.GREEN), true);
                    } else {
                        player.displayClientMessage(Component.literal("Switch to Switch connection established. (Link is administratively down)").withStyle(ChatFormatting.YELLOW), true);
                    }
                } else if (!handledDhcp) {
                    if (storageServer != null) {
                        player.displayClientMessage(Component.literal("Network connection established! (Using static IP / No DHCP available)").withStyle(ChatFormatting.YELLOW), true);
                    } else if (firewall != null) {
                        player.displayClientMessage(Component.literal("Network connection established to Firewall!").withStyle(ChatFormatting.GREEN), true);
                    } else if (netSwitch != null) {
                        player.displayClientMessage(Component.literal("Network connection established to Switch!").withStyle(ChatFormatting.GREEN), true);
                    } else if (serverRack != null) {
                        player.displayClientMessage(Component.literal("Network connection established to Server Rack!").withStyle(ChatFormatting.GREEN), true);
                    }
                }

                // Ensure block updates are sent
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                level.sendBlockUpdated(storedPos, level.getBlockState(storedPos), level.getBlockState(storedPos), 3);
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