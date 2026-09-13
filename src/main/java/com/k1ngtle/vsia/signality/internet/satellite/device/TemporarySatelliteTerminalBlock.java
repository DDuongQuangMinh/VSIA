package com.k1ngtle.vsia.signality.internet.satellite.device;

import com.k1ngtle.vsia.signality.internet.satellite.client.ClientSatelliteHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public final class TemporarySatelliteTerminalBlock
        extends BaseEntityBlock {

    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    private static final VoxelShape BASE =
            Block.box(
                    2.0D,
                    0.0D,
                    2.0D,
                    14.0D,
                    4.0D,
                    14.0D
            );

    private static final VoxelShape MAST =
            Block.box(
                    7.0D,
                    4.0D,
                    7.0D,
                    9.0D,
                    11.0D,
                    9.0D
            );

    private static final VoxelShape DISH =
            Block.box(
                    3.0D,
                    9.0D,
                    5.0D,
                    13.0D,
                    15.0D,
                    11.0D
            );

    private static final VoxelShape SHAPE =
            Shapes.or(
                    BASE,
                    MAST,
                    DISH
            );

    public TemporarySatelliteTerminalBlock(
            Properties properties
    ) {
        super(
                properties
        );

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
        builder.add(
                FACING
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context
                                .getHorizontalDirection()
                                .getOpposite()
                );
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
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
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            BlockPos target =
                    pos.immutable();

            DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () ->
                            ClientSatelliteHooks
                                    .open(
                                            target
                                    )
            );
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
        return new TemporarySatelliteTerminalBlockEntity(
                pos,
                state
        );
    }
}
