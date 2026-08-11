package com.k1ngtle.vsia.signality.example;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractRadarBlock extends HorizontalDirectionalBlock implements EntityBlock {
   private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);
   private final Supplier<? extends BlockEntityType<? extends AbstractSweepRadarBlockEntity>> beType;

   protected AbstractRadarBlock(Properties properties, Supplier<? extends BlockEntityType<? extends AbstractSweepRadarBlockEntity>> beType) {
      super(properties);
      this.beType = beType;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Nullable
   public abstract BlockEntity newBlockEntity(BlockPos var1, BlockState var2);

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.isClientSide && type == this.beType.get() ? (lvl, pos, st, be) -> {
         if (be instanceof AbstractSweepRadarBlockEntity radar) {
            radar.serverTick();
         }
      } : null;
   }
}
