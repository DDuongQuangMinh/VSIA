package com.k1ngtle.vsia.phone.subscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PhysicalSimCardItem extends Item {
    public PhysicalSimCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("VSIA Mobile SIM");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PhoneSubscriberService.insertPhysicalSimFromStack(
                    serverPlayer,
                    stack
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        String number = stack.getOrCreateTag()
                .getString("VSIA_SIM_MSISDN");
        String iccid = stack.getOrCreateTag()
                .getString("VSIA_SIM_ICCID");

        tooltip.add(Component.literal(
                number.isBlank()
                        ? "Unprovisioned physical USIM"
                        : "Number: " + number
        ).withStyle(ChatFormatting.GRAY));

        if (!iccid.isBlank()) {
            tooltip.add(Component.literal(
                    "ICCID: " + mask(iccid)
            ).withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.literal(
                "Right-click to insert into your Temporary iPhone"
        ).withStyle(ChatFormatting.AQUA));
    }

    private static String mask(String value) {
        if (value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4)
                + "••••••"
                + value.substring(value.length() - 4);
    }
}
