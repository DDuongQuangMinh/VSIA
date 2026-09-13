package com.k1ngtle.vsia.signality.internet.radio.device;

import com.k1ngtle.vsia.signality.internet.server.NetworkCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TemporaryRadioBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape BODY =
            Block.box(3.0D, 0.0D, 4.0D, 13.0D, 10.0D, 12.0D);

    private static final VoxelShape ANTENNA =
            Block.box(11.0D, 9.0D, 7.0D, 12.0D, 16.0D, 8.0D);

    private static final VoxelShape SHAPE =
            Shapes.or(BODY, ANTENNA);

    public TemporaryRadioBlock() {
        super(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(2.5F, 6.0F)
                        .noOcclusion()
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
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
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return BODY;
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
        if (player.getItemInHand(hand).getItem() instanceof NetworkCableItem) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TemporaryRadioBlockEntity radio) {

            String frequency = String.format(
                    java.util.Locale.ROOT,
                    "%.4f MHz",
                    radio.activeFrequencyHz() / 1_000_000.0
            );

            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "VSIA Temporary Radio | "
                                    + radio.networkProfileId()
                                    + " | "
                                    + radio.radioMode()
                                    + " | "
                                    + frequency
                    )
            );

            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "Use /vsiaradio for tune, packet, voice, mesh, repeater and FHSS tests."
                    )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TemporaryRadioBlockEntity(pos, state);
    }
}
