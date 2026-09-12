package com.k1ngtle.vsia.phone;

import com.k1ngtle.vsia.phone.client.ClientPhoneHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class TemporaryIPhoneItem extends Item {
    public TemporaryIPhoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPhoneHooks::openPhone);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
