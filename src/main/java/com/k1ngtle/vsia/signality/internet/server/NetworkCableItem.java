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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NetworkCableItem extends Item {
    private static final int MODEL_SEARCH_HORIZONTAL_RADIUS = 4;
    private static final int MODEL_SEARCH_VERTICAL_RADIUS = 3;

    public NetworkCableItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private boolean isNetworkDevice(BlockEntity be) {
        return be instanceof StorageServerBlockEntity
                || be instanceof ServerRackBlockEntity
                || be instanceof NetworkSwitchBlockEntity
                || be instanceof FirewallBlockEntity;
    }

    private int devicePriority(BlockEntity be) {
        if (be instanceof NetworkSwitchBlockEntity) {
            return 0;
        }

        if (be instanceof ServerRackBlockEntity) {
            return 1;
        }

        if (be instanceof FirewallBlockEntity) {
            return 2;
        }

        if (be instanceof StorageServerBlockEntity) {
            return 3;
        }

        return 100;
    }

    private ResolvedCableEndpoint resolveEndpoint(
            Level level,
            BlockPos clickedPos
    ) {
        BlockPos clicked =
                clickedPos.immutable();

        BlockEntity exact =
                level.getBlockEntity(clicked);

        if (isNetworkDevice(exact)) {
            return new ResolvedCableEndpoint(
                    clicked,
                    exact,
                    true,
                    0
            );
        }

        List<ResolvedCableEndpoint> candidates =
                new ArrayList<>();

        for (int dy = -MODEL_SEARCH_VERTICAL_RADIUS;
             dy <= MODEL_SEARCH_VERTICAL_RADIUS;
             dy++) {
            if (dy == 0) {
                continue;
            }

            BlockPos candidatePos =
                    clicked.offset(
                            0,
                            dy,
                            0
                    );

            BlockEntity candidateEntity =
                    level.getBlockEntity(candidatePos);

            if (!isNetworkDevice(candidateEntity)) {
                continue;
            }

            candidates.add(
                    new ResolvedCableEndpoint(
                            candidatePos,
                            candidateEntity,
                            false,
                            sameColumnScore(
                                    dy,
                                    candidateEntity
                            )
                    )
            );
        }

        for (int dx = -MODEL_SEARCH_HORIZONTAL_RADIUS;
             dx <= MODEL_SEARCH_HORIZONTAL_RADIUS;
             dx++) {
            for (int dz = -MODEL_SEARCH_HORIZONTAL_RADIUS;
                 dz <= MODEL_SEARCH_HORIZONTAL_RADIUS;
                 dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = -MODEL_SEARCH_VERTICAL_RADIUS;
                     dy <= MODEL_SEARCH_VERTICAL_RADIUS;
                     dy++) {
                    BlockPos candidatePos =
                            clicked.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    BlockEntity candidateEntity =
                            level.getBlockEntity(candidatePos);

                    if (!isNetworkDevice(candidateEntity)) {
                        continue;
                    }

                    candidates.add(
                            new ResolvedCableEndpoint(
                                    candidatePos,
                                    candidateEntity,
                                    false,
                                    nearbyScore(
                                            dx,
                                            dy,
                                            dz,
                                            candidateEntity
                                    )
                            )
                    );
                }
            }
        }

        return candidates.stream()
                .min(
                        Comparator
                                .comparingInt(
                                        ResolvedCableEndpoint::score
                                )
                                .thenComparingInt(
                                        value ->
                                                devicePriority(
                                                        value.blockEntity()
                                                )
                                )
                                .thenComparingInt(
                                        value ->
                                                Math.abs(
                                                        value.pos()
                                                                .getY()
                                                                - clicked.getY()
                                                )
                                )
                )
                .orElse(null);
    }

    private int sameColumnScore(
            int dy,
            BlockEntity blockEntity
    ) {
        return Math.abs(dy) * 10
                + devicePriority(blockEntity);
    }

    private int nearbyScore(
            int dx,
            int dy,
            int dz,
            BlockEntity blockEntity
    ) {
        int horizontal =
                dx * dx + dz * dz;

        int vertical =
                dy * dy;

        return 100
                + horizontal * 100
                + vertical * 25
                + devicePriority(blockEntity);
    }

    private boolean isSwitchPortUp(
            NetworkSwitchBlockEntity netSwitch,
            BlockPos connectedPos
    ) {
        List<BlockPos> devices =
                netSwitch.getConnectedDevices();

        int index =
                devices.indexOf(connectedPos);

        if (index == -1) {
            return false;
        }

        String portName =
                index < 24
                        ? "FastEthernet0/" + (index + 1)
                        : "GigabitEthernet0/" + (index - 23);

        SwitchOsSimulator.PortConfig pc =
                netSwitch.osSimulators[0]
                        .portConfigs
                        .get(portName);

        return pc != null && pc.up;
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        Level level =
                context.getLevel();

        Player player =
                context.getPlayer();

        BlockPos clickedPos =
                context.getClickedPos();

        ItemStack stack =
                context.getItemInHand();

        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        ResolvedCableEndpoint target =
                resolveEndpoint(
                        level,
                        clickedPos
                );

        if (stack.hasTag()
                && stack.getTag()
                .contains("StoredX")) {
            CompoundTag tag =
                    stack.getTag();

            BlockPos rawStoredPos =
                    new BlockPos(
                            tag.getInt("StoredX"),
                            tag.getInt("StoredY"),
                            tag.getInt("StoredZ")
                    );

            ResolvedCableEndpoint stored =
                    resolveEndpoint(
                            level,
                            rawStoredPos
                    );

            if (stored == null) {
                player.displayClientMessage(
                        Component.literal(
                                "Stored cable endpoint no longer resolves to a VSIA network device: "
                                        + rawStoredPos.toShortString()
                        ).withStyle(
                                ChatFormatting.RED
                        ),
                        true
                );

                stack.setTag(null);

                return InteractionResult.FAIL;
            }

            if (target == null) {
                player.displayClientMessage(
                        Component.literal(
                                "No VSIA network device found near "
                                        + clickedPos.toShortString()
                                        + " within "
                                        + MODEL_SEARCH_HORIZONTAL_RADIUS
                                        + " horizontal / "
                                        + MODEL_SEARCH_VERTICAL_RADIUS
                                        + " vertical blocks."
                        ).withStyle(
                                ChatFormatting.RED
                        ),
                        true
                );

                stack.setTag(null);

                return InteractionResult.FAIL;
            }

            BlockPos storedPos =
                    stored.pos();

            BlockPos pos =
                    target.pos();

            BlockEntity storedEntity =
                    stored.blockEntity();

            BlockEntity targetEntity =
                    target.blockEntity();

            if (storedPos.equals(pos)) {
                player.displayClientMessage(
                        Component.literal(
                                "Cannot connect a device to itself. Both clicks resolve to "
                                        + pos.toShortString()
                        ).withStyle(
                                ChatFormatting.RED
                        ),
                        true
                );

                stack.setTag(null);

                return InteractionResult.FAIL;
            }

            boolean isValidConnection =
                    false;

            StorageServerBlockEntity storageServer =
                    null;

            ServerRackBlockEntity serverRack =
                    null;

            NetworkSwitchBlockEntity netSwitch =
                    null;

            FirewallBlockEntity firewall =
                    null;

            if (targetEntity instanceof StorageServerBlockEntity
                    && storedEntity instanceof ServerRackBlockEntity) {
                storageServer =
                        (StorageServerBlockEntity) targetEntity;

                serverRack =
                        (ServerRackBlockEntity) storedEntity;

                storageServer.setConnectedRackPos(
                        storedPos
                );

                serverRack.connectCable(
                        pos
                );

                isValidConnection =
                        true;

            } else if (storedEntity instanceof StorageServerBlockEntity
                    && targetEntity instanceof ServerRackBlockEntity) {
                storageServer =
                        (StorageServerBlockEntity) storedEntity;

                serverRack =
                        (ServerRackBlockEntity) targetEntity;

                storageServer.setConnectedRackPos(
                        pos
                );

                serverRack.connectCable(
                        storedPos
                );

                isValidConnection =
                        true;

            } else if (targetEntity instanceof NetworkSwitchBlockEntity
                    && storedEntity instanceof ServerRackBlockEntity) {
                netSwitch =
                        (NetworkSwitchBlockEntity) targetEntity;

                serverRack =
                        (ServerRackBlockEntity) storedEntity;

                netSwitch.connectDevice(
                        storedPos
                );

                serverRack.connectCable(
                        pos
                );

                isValidConnection =
                        true;

            } else if (storedEntity instanceof NetworkSwitchBlockEntity
                    && targetEntity instanceof ServerRackBlockEntity) {
                netSwitch =
                        (NetworkSwitchBlockEntity) storedEntity;

                serverRack =
                        (ServerRackBlockEntity) targetEntity;

                netSwitch.connectDevice(
                        pos
                );

                serverRack.connectCable(
                        storedPos
                );

                isValidConnection =
                        true;

            } else if (targetEntity instanceof NetworkSwitchBlockEntity
                    && storedEntity instanceof StorageServerBlockEntity) {
                netSwitch =
                        (NetworkSwitchBlockEntity) targetEntity;

                storageServer =
                        (StorageServerBlockEntity) storedEntity;

                netSwitch.connectDevice(
                        storedPos
                );

                storageServer.setConnectedRackPos(
                        pos
                );

                isValidConnection =
                        true;

            } else if (storedEntity instanceof NetworkSwitchBlockEntity
                    && targetEntity instanceof StorageServerBlockEntity) {
                netSwitch =
                        (NetworkSwitchBlockEntity) storedEntity;

                storageServer =
                        (StorageServerBlockEntity) targetEntity;

                netSwitch.connectDevice(
                        pos
                );

                storageServer.setConnectedRackPos(
                        storedPos
                );

                isValidConnection =
                        true;

            } else if (targetEntity instanceof ServerRackBlockEntity
                    && storedEntity instanceof ServerRackBlockEntity) {
                serverRack =
                        (ServerRackBlockEntity) targetEntity;

                ServerRackBlockEntity serverRack2 =
                        (ServerRackBlockEntity) storedEntity;

                serverRack.connectCable(
                        storedPos
                );

                serverRack2.connectCable(
                        pos
                );

                isValidConnection =
                        true;

            } else if (targetEntity instanceof NetworkSwitchBlockEntity
                    && storedEntity instanceof NetworkSwitchBlockEntity) {
                netSwitch =
                        (NetworkSwitchBlockEntity) targetEntity;

                NetworkSwitchBlockEntity netSwitch2 =
                        (NetworkSwitchBlockEntity) storedEntity;

                netSwitch.connectDevice(
                        storedPos
                );

                netSwitch2.connectDevice(
                        pos
                );

                isValidConnection =
                        true;

            } else if (targetEntity instanceof FirewallBlockEntity
                    && storedEntity instanceof ServerRackBlockEntity) {
                firewall =
                        (FirewallBlockEntity) targetEntity;

                serverRack =
                        (ServerRackBlockEntity) storedEntity;

                if (firewall.connectDevice(storedPos)) {
                    serverRack.connectCable(
                            pos
                    );

                    isValidConnection =
                            true;
                }

            } else if (storedEntity instanceof FirewallBlockEntity
                    && targetEntity instanceof ServerRackBlockEntity) {
                firewall =
                        (FirewallBlockEntity) storedEntity;

                serverRack =
                        (ServerRackBlockEntity) targetEntity;

                if (firewall.connectDevice(pos)) {
                    serverRack.connectCable(
                            storedPos
                    );

                    isValidConnection =
                            true;
                }

            } else if (targetEntity instanceof FirewallBlockEntity
                    && storedEntity instanceof NetworkSwitchBlockEntity) {
                firewall =
                        (FirewallBlockEntity) targetEntity;

                netSwitch =
                        (NetworkSwitchBlockEntity) storedEntity;

                if (firewall.connectDevice(storedPos)) {
                    netSwitch.connectDevice(
                            pos
                    );

                    isValidConnection =
                            true;
                }

            } else if (storedEntity instanceof FirewallBlockEntity
                    && targetEntity instanceof NetworkSwitchBlockEntity) {
                firewall =
                        (FirewallBlockEntity) storedEntity;

                netSwitch =
                        (NetworkSwitchBlockEntity) targetEntity;

                if (firewall.connectDevice(pos)) {
                    netSwitch.connectDevice(
                            storedPos
                    );

                    isValidConnection =
                            true;
                }

            } else if (targetEntity instanceof FirewallBlockEntity
                    && storedEntity instanceof StorageServerBlockEntity) {
                firewall =
                        (FirewallBlockEntity) targetEntity;

                storageServer =
                        (StorageServerBlockEntity) storedEntity;

                if (firewall.connectDevice(storedPos)) {
                    storageServer.setConnectedRackPos(
                            pos
                    );

                    isValidConnection =
                            true;
                }

            } else if (storedEntity instanceof FirewallBlockEntity
                    && targetEntity instanceof StorageServerBlockEntity) {
                firewall =
                        (FirewallBlockEntity) storedEntity;

                storageServer =
                        (StorageServerBlockEntity) targetEntity;

                if (firewall.connectDevice(pos)) {
                    storageServer.setConnectedRackPos(
                            storedPos
                    );

                    isValidConnection =
                            true;
                }

            } else if (targetEntity instanceof FirewallBlockEntity
                    && storedEntity instanceof FirewallBlockEntity) {
                firewall =
                        (FirewallBlockEntity) targetEntity;

                FirewallBlockEntity firewall2 =
                        (FirewallBlockEntity) storedEntity;

                if (firewall.connectDevice(storedPos)) {
                    firewall2.connectDevice(
                            pos
                    );

                    isValidConnection =
                            true;
                }
            }

            if (isValidConnection) {
                boolean handledDhcp =
                        false;

                if (storageServer != null
                        && storageServer.isDhcpEnabled()) {
                    boolean portUp =
                            true;

                    if (netSwitch != null) {
                        portUp =
                                isSwitchPortUp(
                                        netSwitch,
                                        storageServer.getBlockPos()
                                );
                    }

                    if (portUp) {
                        ServerRackBlockEntity rackToUse =
                                serverRack;

                        if (rackToUse == null
                                && netSwitch != null) {
                            rackToUse =
                                    netSwitch.findFirstRack(
                                            new java.util.HashSet<>()
                                    );
                        }

                        if (rackToUse != null) {
                            String assignedIp =
                                    rackToUse.requestDynamicIp(
                                            "vsia:storage_server_"
                                                    + storageServer
                                                    .getBlockPos()
                                                    .asLong(),
                                            false
                                    );

                            if (assignedIp != null) {
                                storageServer.setIpAddress(
                                        assignedIp
                                );

                                storageServer.setSubnetMask(
                                        rackToUse.subnetMask()
                                );

                                storageServer.setGateway(
                                        rackToUse.gatewayIp()
                                );
                            }

                            String assignedIpv6 =
                                    rackToUse.requestDynamicIp(
                                            "vsia:storage_server_"
                                                    + storageServer
                                                    .getBlockPos()
                                                    .asLong(),
                                            true
                                    );

                            if (assignedIpv6 != null) {
                                storageServer.setIpv6Address(
                                        assignedIpv6
                                );
                            }

                            handledDhcp =
                                    true;

                            player.displayClientMessage(
                                    Component.literal(
                                            "Network connection established! DHCP IP assigned: "
                                                    + assignedIp
                                    ).withStyle(
                                            ChatFormatting.GREEN
                                    ),
                                    true
                            );
                        }
                    } else {
                        handledDhcp =
                                true;

                        player.displayClientMessage(
                                Component.literal(
                                        "Network connection established! (Switch port is administratively down)"
                                ).withStyle(
                                        ChatFormatting.YELLOW
                                ),
                                true
                        );
                    }

                } else if (firewall != null
                        && firewall.isDhcpEnabled()) {
                    boolean portUp =
                            true;

                    if (netSwitch != null) {
                        portUp =
                                isSwitchPortUp(
                                        netSwitch,
                                        firewall.getBlockPos()
                                );
                    }

                    if (portUp) {
                        ServerRackBlockEntity rackToUse =
                                serverRack;

                        if (rackToUse == null
                                && netSwitch != null) {
                            rackToUse =
                                    netSwitch.findFirstRack(
                                            new java.util.HashSet<>()
                                    );
                        }

                        if (rackToUse != null) {
                            String assignedIp =
                                    rackToUse.requestDynamicIp(
                                            "vsia:firewall_"
                                                    + firewall
                                                    .getBlockPos()
                                                    .asLong(),
                                            false
                                    );

                            if (assignedIp != null) {
                                firewall.setManagementIp(
                                        assignedIp
                                );

                                firewall.setSubnetMask(
                                        rackToUse.subnetMask()
                                );
                            }

                            String assignedIpv6 =
                                    rackToUse.requestDynamicIp(
                                            "vsia:firewall_"
                                                    + firewall
                                                    .getBlockPos()
                                                    .asLong(),
                                            true
                                    );

                            if (assignedIpv6 != null) {
                                firewall.setIpv6Address(
                                        assignedIpv6
                                );
                            }

                            handledDhcp =
                                    true;

                            player.displayClientMessage(
                                    Component.literal(
                                            "Network connection established! DHCP assigned to Firewall: "
                                                    + (
                                                    assignedIp != null
                                                            ? assignedIp
                                                            : assignedIpv6
                                            )
                                    ).withStyle(
                                            ChatFormatting.GREEN
                                    ),
                                    true
                            );
                        }
                    } else {
                        handledDhcp =
                                true;

                        player.displayClientMessage(
                                Component.literal(
                                        "Network connection established! (Switch port is administratively down)"
                                ).withStyle(
                                        ChatFormatting.YELLOW
                                ),
                                true
                        );
                    }
                }

                if (serverRack != null
                        && netSwitch != null) {
                    if (isSwitchPortUp(
                            netSwitch,
                            serverRack.getBlockPos()
                    )) {
                        netSwitch.refreshNetworkDhcp(
                                new java.util.HashSet<>()
                        );

                        player.displayClientMessage(
                                Component.literal(
                                        "Uplink established. Transmitting DHCP to downstream devices..."
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                                true
                        );
                    } else {
                        player.displayClientMessage(
                                Component.literal(
                                        "Uplink established. (Switch port is administratively down)"
                                ).withStyle(
                                        ChatFormatting.YELLOW
                                ),
                                true
                        );
                    }

                } else if (targetEntity instanceof NetworkSwitchBlockEntity
                        && storedEntity instanceof NetworkSwitchBlockEntity) {
                    NetworkSwitchBlockEntity tSwitch =
                            (NetworkSwitchBlockEntity) targetEntity;

                    NetworkSwitchBlockEntity sSwitch =
                            (NetworkSwitchBlockEntity) storedEntity;

                    if (isSwitchPortUp(
                            tSwitch,
                            storedPos
                    )
                            && isSwitchPortUp(
                            sSwitch,
                            pos
                    )) {
                        tSwitch.refreshNetworkDhcp(
                                new java.util.HashSet<>()
                        );

                        player.displayClientMessage(
                                Component.literal(
                                        "Switch to Switch connection established. Refreshing DHCP..."
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                                true
                        );
                    } else {
                        player.displayClientMessage(
                                Component.literal(
                                        "Switch to Switch connection established. (Link is administratively down)"
                                ).withStyle(
                                        ChatFormatting.YELLOW
                                ),
                                true
                        );
                    }

                } else if (!handledDhcp) {
                    if (storageServer != null) {
                        player.displayClientMessage(
                                Component.literal(
                                        "Network connection established! (Using static IP / No DHCP available)"
                                ).withStyle(
                                        ChatFormatting.YELLOW
                                ),
                                true
                        );
                    } else if (firewall != null) {
                        player.displayClientMessage(
                                Component.literal(
                                        "Network connection established to Firewall!"
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                                true
                        );
                    } else if (netSwitch != null) {
                        player.displayClientMessage(
                                Component.literal(
                                        "Network connection established to Switch!"
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                                true
                        );
                    } else if (serverRack != null) {
                        player.displayClientMessage(
                                Component.literal(
                                        "Network connection established to Server Rack!"
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                                true
                        );
                    }
                }

                level.sendBlockUpdated(
                        pos,
                        level.getBlockState(pos),
                        level.getBlockState(pos),
                        3
                );

                level.sendBlockUpdated(
                        storedPos,
                        level.getBlockState(storedPos),
                        level.getBlockState(storedPos),
                        3
                );

                player.displayClientMessage(
                        Component.literal(
                                "Cable endpoints: "
                                        + storedPos.toShortString()
                                        + " <-> "
                                        + pos.toShortString()
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                        true
                );

            } else {
                player.displayClientMessage(
                        Component.literal(
                                "Invalid connection endpoints after normalization. "
                                        + storedEntity.getClass()
                                        .getSimpleName()
                                        + " <-> "
                                        + targetEntity.getClass()
                                        .getSimpleName()
                        ).withStyle(
                                ChatFormatting.RED
                        ),
                        true
                );
            }

            stack.setTag(null);

        } else {
            if (target == null) {
                player.displayClientMessage(
                        Component.literal(
                                "No VSIA network device found near "
                                        + clickedPos.toShortString()
                                        + ". Click closer to the visible Rack, Storage Server, Switch, or Firewall."
                        ).withStyle(
                                ChatFormatting.RED
                        ),
                        true
                );

                return InteractionResult.FAIL;
            }

            CompoundTag tag =
                    stack.getOrCreateTag();

            BlockPos normalized =
                    target.pos();

            tag.putInt(
                    "StoredX",
                    normalized.getX()
            );

            tag.putInt(
                    "StoredY",
                    normalized.getY()
            );

            tag.putInt(
                    "StoredZ",
                    normalized.getZ()
            );

            tag.putInt(
                    "ClickedX",
                    clickedPos.getX()
            );

            tag.putInt(
                    "ClickedY",
                    clickedPos.getY()
            );

            tag.putInt(
                    "ClickedZ",
                    clickedPos.getZ()
            );

            player.displayClientMessage(
                    Component.literal(
                            target.direct()
                                    ? "Started connection at "
                                    + normalized.toShortString()
                                    : "Started connection. Visible point "
                                    + clickedPos.toShortString()
                                    + " normalized to "
                                    + normalized.toShortString()
                                    + " ("
                                    + target.blockEntity()
                                    .getClass()
                                    .getSimpleName()
                                    + ")"
                    ).withStyle(
                            ChatFormatting.YELLOW
                    ),
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltipComponents,
            TooltipFlag isAdvanced
    ) {
        tooltipComponents.add(
                Component.literal(
                        "Use on network devices (Rack, Server, Switch, Firewall) to connect them."
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        if (stack.hasTag()
                && stack.getTag()
                .contains("StoredX")) {
            CompoundTag tag =
                    stack.getTag();

            tooltipComponents.add(
                    Component.literal(
                            "Stored logical endpoint: "
                                    + tag.getInt("StoredX")
                                    + ", "
                                    + tag.getInt("StoredY")
                                    + ", "
                                    + tag.getInt("StoredZ")
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );
        }
    }

    private record ResolvedCableEndpoint(
            BlockPos pos,
            BlockEntity blockEntity,
            boolean direct,
            int score
    ) {
        private ResolvedCableEndpoint {
            pos =
                    pos.immutable();
        }
    }
}
