package com.k1ngtle.vsia.signality.internet.router;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class RtAc68uRouterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE =
            Block.box(
                    2.5D,
                    0.0D,
                    4.5D,
                    13.5D,
                    5.5D,
                    11.5D
            );

    public RtAc68uRouterBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
                );
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity
                    instanceof RtAc68uRouterBlockEntity router) {
                player.sendSystemMessage(
                        Component.literal(
                                "RT-AC68U Router | "
                                        + router.wifiIpAddress()
                                        + " | forwarding "
                                        + (
                                        router.wifiLiveRouterEnabled()
                                                ? "ON"
                                                : "OFF"
                                )
                                        + " | lan0 192.168.1.1/24 | lan1 192.168.2.1/24"
                        )
                );
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new RtAc68uRouterBlockEntity(
                pos,
                state
        );
    }
}
