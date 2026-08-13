package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.client.renderer.ServerRackItemRenderer;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ServerRackItem extends BlockItem implements GeoItem {

    public static final String PROFILE_TAG = "ServerRackProfile";

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public ServerRackItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ServerRackProfile getProfile(ItemStack stack) {
        return stack.hasTag()
                ? ServerRackProfile.byName(stack.getTag().getString(PROFILE_TAG))
                : ServerRackProfile.INTRA_DATA_CENTER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ServerRackProfile next = getProfile(stack).next();
        stack.getOrCreateTag().putString(PROFILE_TAG, next.name());

        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("Server profile: ")
                    .append(next.description()), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        ServerRackProfile profile = getProfile(stack);
        tooltip.add(Component.literal("Profile: " + profile.displayName()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Maximum range: " + profile.rangeText()).withStyle(ChatFormatting.GRAY));
        if (profile.wiredBeyondCampus()) {
            tooltip.add(Component.literal("Wire required beyond 10,000 blocks")
                    .withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.literal("Right-click in the air to change profile")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {
        consumer.accept(new IClientItemExtensions() {

            private ServerRackItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ServerRackItemRenderer();
                }

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        // The inventory item does not need an animation controller.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
