package com.k1ngtle.vsia.cockpit;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class F35SeatController {
    private static final String SEAT_FLAG =
            "VsiaF35Seat";

    private static final String COCKPIT_POS =
            "VsiaF35CockpitPos";

    private static final String COCKPIT_DIM =
            "VsiaF35CockpitDim";

    private F35SeatController() {
    }

    public static boolean trySeat(
            Player player,
            F35CockpitSeatBlockEntity cockpit
    ) {
        if (!(cockpit.getLevel()
                instanceof ServerLevel serverLevel)) {
            return false;
        }

        ArmorStand seat =
                findSeat(
                        serverLevel,
                        cockpit
                );

        if (seat != null
                && !seat.getPassengers()
                .isEmpty()
                && !seat.getPassengers()
                .contains(player)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Cockpit occupied"
                    ),
                    true
            );

            return false;
        }

        if (seat == null) {
            seat = createSeat(
                    serverLevel,
                    cockpit
            );
        }

        alignSeat(
                seat,
                cockpit
        );

        if (!(player.isPassenger()
                && player.getVehicle() == seat)) {
            player.startRiding(
                    seat,
                    true
            );
        }

        player.setYRot(
                seat.getYRot()
        );
        player.setYHeadRot(
                seat.getYRot()
        );
        player.setYBodyRot(
                seat.getYRot()
        );

        return true;
    }

    public static void tickPassenger(
            ServerPlayer player
    ) {
        Entity vehicle =
                player.getVehicle();

        if (!(vehicle
                instanceof ArmorStand seat)
                || !isSeatEntity(
                seat
        )) {
            return;
        }

        CompoundTag tag =
                seat.getPersistentData();

        if (!tag.contains(
                COCKPIT_POS
        )
                || !tag.contains(
                COCKPIT_DIM
        )) {
            player.stopRiding();
            seat.discard();
            return;
        }

        String dimensionKey =
                tag.getString(
                        COCKPIT_DIM
                );

        ServerLevel level =
                player.server.getLevel(
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                new ResourceLocation(
                                        dimensionKey
                                )
                        )
                );

        if (level == null) {
            player.stopRiding();
            seat.discard();
            return;
        }

        BlockPos pos =
                NbtUtils.readBlockPos(
                        tag.getCompound(
                                COCKPIT_POS
                        )
                );

        if (!(level.getBlockEntity(
                pos
        ) instanceof F35CockpitSeatBlockEntity cockpit)) {
            player.stopRiding();
            seat.discard();
            return;
        }

        alignSeat(
                seat,
                cockpit
        );

        if (seat.getPassengers()
                .isEmpty()) {
            seat.discard();
        }
    }

    private static ArmorStand createSeat(
            ServerLevel level,
            F35CockpitSeatBlockEntity cockpit
    ) {
        Vec3 seatPosition =
                F35VsShipHelper.seatWorldPosition(
                        cockpit,
                        cockpit.getBlockState()
                                .getValue(
                                        F35CockpitSeatBlock.FACING
                                )
                );

        ArmorStand seat =
                new ArmorStand(
                        level,
                        seatPosition.x,
                        seatPosition.y,
                        seatPosition.z
                );

        seat.setInvisible(
                true
        );
        seat.setNoGravity(
                true
        );
        seat.setInvulnerable(
                true
        );
        seat.setSilent(
                true
        );
        // Minecraft 1.20.1 Mojmap does not expose public setters for
        // ArmorStand small/base-plate flags. The seat entity is invisible, so
        // these flags are unnecessary for rendering and are intentionally not
        // changed here.

        CompoundTag tag =
                seat.getPersistentData();

        tag.putBoolean(
                SEAT_FLAG,
                true
        );
        tag.put(
                COCKPIT_POS,
                NbtUtils.writeBlockPos(
                        cockpit.getBlockPos()
                )
        );
        tag.putString(
                COCKPIT_DIM,
                cockpit.getLevel()
                        .dimension()
                        .location()
                        .toString()
        );

        level.addFreshEntity(
                seat
        );

        return seat;
    }

    private static void alignSeat(
            ArmorStand seat,
            F35CockpitSeatBlockEntity cockpit
    ) {
        Direction facing =
                cockpit.getBlockState()
                        .getValue(
                                F35CockpitSeatBlock.FACING
                        );

        Vec3 seatPosition =
                F35VsShipHelper.seatWorldPosition(
                        cockpit,
                        facing
                );

        float yaw =
                F35VsShipHelper.seatViewYaw(
                        facing
                );

        seat.setPos(
                seatPosition.x,
                seatPosition.y,
                seatPosition.z
        );
        seat.setYRot(
                yaw
        );
        seat.setYHeadRot(
                yaw
        );
        seat.setYBodyRot(
                yaw
        );
        seat.setXRot(
                0.0F
        );
    }

    private static ArmorStand findSeat(
            ServerLevel level,
            F35CockpitSeatBlockEntity cockpit
    ) {
        Vec3 center =
                Vec3.atCenterOf(
                        cockpit.getBlockPos()
                );

        List<ArmorStand> seats =
                level.getEntitiesOfClass(
                        ArmorStand.class,
                        AABB.ofSize(
                                center,
                                3.0,
                                3.0,
                                3.0
                        ),
                        seat -> isSeatForCockpit(
                                seat,
                                cockpit
                        )
                );

        return seats.isEmpty()
                ? null
                : seats.get(0);
    }

    private static boolean isSeatForCockpit(
            ArmorStand seat,
            F35CockpitSeatBlockEntity cockpit
    ) {
        if (!isSeatEntity(
                seat
        )) {
            return false;
        }

        CompoundTag tag =
                seat.getPersistentData();

        if (!tag.contains(
                COCKPIT_POS
        )
                || !tag.contains(
                COCKPIT_DIM
        )) {
            return false;
        }

        if (!tag.getString(
                COCKPIT_DIM
        ).equals(
                cockpit.getLevel()
                        .dimension()
                        .location()
                        .toString()
        )) {
            return false;
        }

        return NbtUtils.readBlockPos(
                tag.getCompound(
                        COCKPIT_POS
                )
        ).equals(
                cockpit.getBlockPos()
        );
    }

    private static boolean isSeatEntity(
            ArmorStand seat
    ) {
        return seat.getPersistentData()
                .getBoolean(
                        SEAT_FLAG
                );
    }
}
