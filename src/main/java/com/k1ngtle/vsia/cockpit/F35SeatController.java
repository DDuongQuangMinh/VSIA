package com.k1ngtle.vsia.cockpit;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class F35SeatController {
    public static final String SEAT_RENDER_MARKER =
            "vsia:f35_seat_carrier";

    private static final double MINECART_SEAT_LIFT_Y =
            1.50;

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

        Minecart seat =
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
            seat =
                    createSeat(
                            serverLevel,
                            cockpit
                    );
        }

        if (seat == null) {
            return false;
        }

        alignSeat(
                seat,
                cockpit
        );

        if (!(player.isPassenger()
                && player.getVehicle()
                == seat)) {
            player.startRiding(
                    seat,
                    true
            );
        }

        /*
         * The carrier is an invisible vanilla Minecart on purpose.
         *
         * That gives the cockpit the same camera/perspective behavior as
         * sitting in a normal minecart:
         *
         * - first person remains normal
         * - F5 third-person shows the local player model
         * - the camera may orbit/look around normally
         * - no rider yaw/head/pitch is forced by VSIA
         */
        return true;
    }

    public static void tickPassenger(
            ServerPlayer player
    ) {
        Entity vehicle =
                player.getVehicle();

        if (!(vehicle
                instanceof Minecart seat)
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

    private static Minecart createSeat(
            ServerLevel level,
            F35CockpitSeatBlockEntity cockpit
    ) {
        Vec3 seatPosition =
                adjustedSeatPosition(
                        F35VsShipHelper.seatWorldPosition(
                                cockpit,
                                cockpit.getBlockState()
                                        .getValue(
                                                F35CockpitSeatBlock.FACING
                                        )
                        )
                );

        Minecart seat =
                new Minecart(
                        level,
                        seatPosition.x,
                        seatPosition.y,
                        seatPosition.z
                );

        seat.setPos(
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

        /*
         * Synced client-side marker used only to suppress the vanilla
         * MinecartRenderer for this invisible cockpit carrier.
         */
        seat.setCustomName(
                Component.literal(
                        SEAT_RENDER_MARKER
                )
        );

        seat.setCustomNameVisible(
                false
        );

        seat.setDeltaMovement(
                Vec3.ZERO
        );

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
            Minecart seat,
            F35CockpitSeatBlockEntity cockpit
    ) {
        Direction facing =
                cockpit.getBlockState()
                        .getValue(
                                F35CockpitSeatBlock.FACING
                        );

        Vec3 seatPosition =
                adjustedSeatPosition(
                        F35VsShipHelper.seatWorldPosition(
                                cockpit,
                                facing
                        )
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

        seat.setDeltaMovement(
                Vec3.ZERO
        );

        seat.setYRot(
                yaw
        );

        seat.setXRot(
                0.0F
        );
    }

    private static Vec3 adjustedSeatPosition(
            Vec3 baseSeatPosition
    ) {
        return baseSeatPosition.add(
                0.0,
                MINECART_SEAT_LIFT_Y,
                0.0
        );
    }

    private static Minecart findSeat(
            ServerLevel level,
            F35CockpitSeatBlockEntity cockpit
    ) {
        Vec3 center =
                Vec3.atCenterOf(
                        cockpit.getBlockPos()
                );

        List<Minecart> seats =
                level.getEntitiesOfClass(
                        Minecart.class,
                        AABB.ofSize(
                                center,
                                4.0,
                                4.0,
                                4.0
                        ),
                        seat ->
                                isSeatForCockpit(
                                        seat,
                                        cockpit
                                )
                );

        return seats.isEmpty()
                ? null
                : seats.get(0);
    }

    private static boolean isSeatForCockpit(
            Minecart seat,
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
            Minecart seat
    ) {
        return seat.getPersistentData()
                .getBoolean(
                        SEAT_FLAG
                );
    }
}
