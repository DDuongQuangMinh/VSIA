package com.k1ngtle.vsia.weapon.item;

import com.k1ngtle.vsia.weapon.client.render.ModernGunItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ModernGunItem extends AbstractWeaponItem implements GeoItem {
    public static final String CONTROLLER = "weapon";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ModernGunItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged || oldStack.getItem() != newStack.getItem()) return true;
        return !getWeaponId(oldStack).equals(getWeaponId(newStack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.stopUsingItem();
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 0;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        entity.stopUsingItem();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ModernGunItemRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ModernGunItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, state -> {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.weapon.idle"));
            return PlayState.CONTINUE;
        }).triggerableAnim("fire", RawAnimation.begin().thenPlay("animation.weapon.fire"))
                .triggerableAnim("idle", RawAnimation.begin().thenLoop("animation.weapon.idle"))
                .triggerableAnim("reload", RawAnimation.begin().thenPlay("animation.weapon.reload"))
                .triggerableAnim("reload_empty", RawAnimation.begin().thenPlay("animation.weapon.reload_empty"))
                .triggerableAnim("draw", RawAnimation.begin().thenPlay("animation.weapon.draw"))
                .triggerableAnim("inspect", RawAnimation.begin().thenPlay("animation.weapon.inspect")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
