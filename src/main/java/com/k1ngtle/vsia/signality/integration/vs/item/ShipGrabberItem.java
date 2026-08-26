package com.k1ngtle.vsia.signality.integration.vs.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.util.List;

public final class ShipGrabberItem extends Item {
    private static final String TAG_SHIP_ID = "VsiaGrabbedShipId";

    private static final double HOLD_DISTANCE = 4.0D;
    private static final double SPRING_ACCELERATION = 8.0D;
    private static final double DAMPING = 4.0D;
    private static final double MAX_ACCELERATION = 30.0D;
    private static final double MAX_DISTANCE = 96.0D;

    public ShipGrabberItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();

        LoadedServerShip ship =
                VSGameUtilsKt.getShipManagingPos(
                        level,
                        context.getClickedPos()
                );

        if (ship == null) {
            player.displayClientMessage(
                    Component.literal("No Valkyrien Skies ship at this block"),
                    true
            );
            return InteractionResult.CONSUME;
        }

        long shipId = ship.getId();

        if (hasShip(stack) && getShipId(stack) == shipId) {
            clearShip(stack);
            player.displayClientMessage(
                    Component.literal("Released ship " + shipId),
                    true
            );
            return InteractionResult.CONSUME;
        }

        stack.getOrCreateTag().putLong(TAG_SHIP_ID, shipId);

        player.displayClientMessage(
                Component.literal(
                        "Grabbed ship " + shipId
                                + " - keep the item selected to pull it"
                ),
                true
        );

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && hasShip(stack)) {
            long shipId = getShipId(stack);
            clearShip(stack);

            player.displayClientMessage(
                    Component.literal("Released ship " + shipId),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slot,
            boolean selected
    ) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof Player player)
                || !hasShip(stack)) {
            return;
        }

        if (!selected) {
            clearShip(stack);
            return;
        }

        long shipId = getShipId(stack);

        Ship found =
                VSGameUtilsKt.getShipObjectWorld(serverLevel)
                        .getLoadedShips()
                        .getById(shipId);

        if (!(found instanceof LoadedServerShip ship)) {
            clearShip(stack);
            player.displayClientMessage(
                    Component.literal("Grabbed ship is no longer loaded"),
                    true
            );
            return;
        }

        Vector3dc shipPosition =
                ship.getTransform().getPositionInWorld();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        Vec3 target = eye.add(
                look.x * HOLD_DISTANCE,
                look.y * HOLD_DISTANCE,
                look.z * HOLD_DISTANCE
        );

        Vector3d error = new Vector3d(
                target.x - shipPosition.x(),
                target.y - shipPosition.y(),
                target.z - shipPosition.z()
        );

        if (error.lengthSquared() > MAX_DISTANCE * MAX_DISTANCE) {
            clearShip(stack);
            player.displayClientMessage(
                    Component.literal(
                            "Ship Grabber released: ship is too far away"
                    ),
                    true
            );
            return;
        }

        Vector3dc velocity = ship.getVelocity();

        Vector3d acceleration = new Vector3d(error)
                .mul(SPRING_ACCELERATION)
                .sub(
                        new Vector3d(velocity)
                                .mul(DAMPING)
                );

        double accelerationLength = acceleration.length();

        if (accelerationLength > MAX_ACCELERATION
                && accelerationLength > 1.0E-9D) {
            acceleration.mul(
                    MAX_ACCELERATION / accelerationLength
            );
        }

        double mass = ship.getInertiaData().getMass();

        if (!Double.isFinite(mass) || mass <= 0.0D) {
            return;
        }

        Vector3d force = acceleration.mul(mass);

        ValkyrienSkiesMod.INSTANCE
                .getOrCreateGTPA(
                        ship.getChunkClaimDimension()
                )
                .applyWorldForce(
                        ship.getId(),
                        force,
                        null
                );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasShip(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal("Right-click a VS ship to grab it")
                        .withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.literal(
                                "Keep selected to pull it; right-click air to release"
                        )
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        if (hasShip(stack)) {
            tooltip.add(
                    Component.literal(
                                    "Holding ship " + getShipId(stack)
                            )
                            .withStyle(ChatFormatting.AQUA)
            );
        }
    }

    private static boolean hasShip(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_SHIP_ID);
    }

    private static long getShipId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? -1L : tag.getLong(TAG_SHIP_ID);
    }

    private static void clearShip(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(TAG_SHIP_ID);

        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }
}
