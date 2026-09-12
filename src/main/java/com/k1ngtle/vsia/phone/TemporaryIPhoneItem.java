package com.k1ngtle.vsia.phone;

import com.k1ngtle.vsia.phone.client.ClientPhoneHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class TemporaryIPhoneItem extends BlockItem {

    public TemporaryIPhoneItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        openPhone(level);

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();

        if (player != null && player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        openPhone(context.getLevel());

        return InteractionResult.sidedSuccess(
                context.getLevel().isClientSide()
        );
    }

    private static void openPhone(Level level) {
        if (!level.isClientSide()) {
            return;
        }

        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPhoneHooks.openPhone()
        );
    }
}