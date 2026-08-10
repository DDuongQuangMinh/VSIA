package com.k1ngtle.vsia.item;

import com.k1ngtle.vsia.client.renderer.HelmetGPNVG18SandItemRenderer;
import com.k1ngtle.vsia.client.renderer.HelmetGPNVG18SandRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class HelmetGPNVG18SandItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HelmetGPNVG18SandItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HelmetGPNVG18SandRenderer armorRenderer;
            private HelmetGPNVG18SandItemRenderer itemRenderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new HelmetGPNVG18SandRenderer();
                }

                this.armorRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.armorRenderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new HelmetGPNVG18SandItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    private PlayState predicate(AnimationState<HelmetGPNVG18SandItem> event) {
        ItemStack stack = event.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);

        // Check the NBT tag updated by our network packet
        if (stack != null && stack.hasTag() && stack.getTag().getBoolean("nvg_active")) {
            event.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("gpnvg18_active"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("gpnvg18_deactive"));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}