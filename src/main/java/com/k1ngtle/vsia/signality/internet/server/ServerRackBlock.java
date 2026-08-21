package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ServerRackBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Slightly inset from the block edges. This gives the rack a visible selection outline
    // instead of using a completely invisible or empty shape.
    private static final VoxelShape RACK_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public ServerRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ServerRackBlockEntity rack) {
            rack.setProfile(ServerRackItem.getProfile(stack));
            if (placer instanceof ServerPlayer serverPlayer) rack.setOwner(serverPlayer);
        }
    }

    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {
        // The lower half is checked before Minecraft creates the upper half.
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        }

        BlockState lowerState = level.getBlockState(pos.below());

        return lowerState.is(this)
                && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction requiredDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;

        if (direction == requiredDirection &&
                (!neighborState.is(this) || neighborState.getValue(HALF) == half)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? pos.above()
                    : pos.below();
            BlockState otherState = level.getBlockState(otherPos);

            if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The GeckoLib model is approximately two blocks tall and is rendered once from
        // the lower block entity. Rendering the upper half would duplicate the model.
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? RenderShape.ENTITYBLOCK_ANIMATED
                : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return RACK_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return RACK_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new ServerRackBlockEntity(pos, state)
                : null;
    }

    // W1.20 HOST TICKER
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return (tickLevel,tickPos,tickState,be) -> {
            if (be instanceof ServerRackBlockEntity rack) rack.w120Tick();
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // Let the cable item receive the click instead of opening the rack door or GUI.
        // Minecraft calls the held item's useOn method after the block returns PASS.
        if (player.getItemInHand(hand).getItem() instanceof NetworkCableItem) {
            return InteractionResult.PASS;
        }

        BlockPos basePos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(basePos) instanceof ServerRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer && !rack.canConfigure(serverPlayer)) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Access denied. This rack is owned by " + rack.ownerName()), true);
            return InteractionResult.CONSUME;
        }

        if (!rack.beginInteraction(level.getGameTime())) {
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            if (rack.isDoorOpen()) rack.closeDoor();
        } else if (!rack.isDoorOpen()) {
            rack.openDoor();
        } else if (player instanceof ServerPlayer serverPlayer) {
            ServerRackNetwork.openFor(serverPlayer, rack);
        }

        return InteractionResult.CONSUME;
    }
}
