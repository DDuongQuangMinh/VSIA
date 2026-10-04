package com.k1ngtle.vsia.cockpit.program;

import com.k1ngtle.vsia.item.DisplayHardDriveItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DisplayLaptopBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public DisplayLaptopBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DisplayLaptopBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape lid = switch (state.getValue(FACING)) {
            case NORTH -> Block.box(1, 1, 12, 15, 12, 15);
            case SOUTH -> Block.box(1, 1, 1, 15, 12, 4);
            case EAST -> Block.box(1, 1, 1, 4, 12, 15);
            default -> Block.box(12, 1, 1, 15, 12, 15);
        };
        // Symmetric base bounds include the external drive dock in every orientation.
        return Shapes.or(Block.box(0, 0, 0, 16, 3.5, 16), lid);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof DisplayLaptopBlockEntity laptop) {
            if (!laptop.canUse(server)) { player.displayClientMessage(Component.literal("Laptop unavailable: move closer or check interaction permissions."), true); return InteractionResult.CONSUME; }
            ItemStack held = player.getItemInHand(hand);
            if (DisplayHardDriveItem.isDisplayDrive(held)) {
                if (laptop.insert(held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    player.displayClientMessage(Component.literal("Drive inserted. Right-click the laptop to edit it."), true);
                } else player.displayClientMessage(Component.literal("Laptop already contains a drive. Sneak-right-click with an empty hand to eject it."), true);
            } else if (player.isShiftKeyDown() && held.isEmpty()) {
                ItemStack removed = laptop.takeDrive();
                boolean ejected = !removed.isEmpty();
                if (!removed.isEmpty() && !player.getInventory().add(removed)) player.drop(removed, false);
                player.displayClientMessage(Component.literal(ejected ? "Drive ejected." : "Laptop drive slot is empty."), true);
            } else if (laptop.hasDrive()) DisplayProgrammingServer.openPlaced(server, laptop);
            else player.displayClientMessage(Component.literal("Insert a display hard drive first: hold it and right-click this laptop."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof DisplayLaptopBlockEntity laptop) {
            ItemStack drive = laptop.takeDrive();
            if (!drive.isEmpty()) Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.25, pos.getZ()+0.5, drive);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
