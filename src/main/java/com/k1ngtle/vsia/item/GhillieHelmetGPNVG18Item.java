package com.k1ngtle.vsia.item;

import com.k1ngtle.vsia.client.renderer.GhillieHelmetGPNVG18ItemRenderer;
import com.k1ngtle.vsia.client.renderer.GhillieHelmetGPNVG18Renderer;
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

public class GhillieHelmetGPNVG18Item extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GhillieHelmetGPNVG18Item(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GhillieHelmetGPNVG18Renderer armorRenderer;
            private GhillieHelmetGPNVG18ItemRenderer itemRenderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new GhillieHelmetGPNVG18Renderer();
                }

                this.armorRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.armorRenderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new GhillieHelmetGPNVG18ItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    private PlayState predicate(AnimationState<GhillieHelmetGPNVG18Item> event) {
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