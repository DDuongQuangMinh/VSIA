package com.k1ngtle.vsia.item;

import com.k1ngtle.vsia.client.renderer.F35HelmetItemRenderer;
import com.k1ngtle.vsia.client.renderer.F35HelmetRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Normal head-slot armor; client HUD reads the seated cockpit, never mission keys. */
public final class F35HelmetItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public F35HelmetItem(Properties properties){super(ArmorMaterials.IRON,Type.HELMET,properties);}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){
        // Renderer instances are created lazily only through Forge's client hook.
        consumer.accept(new IClientItemExtensions(){
            private F35HelmetRenderer armor;
            private F35HelmetItemRenderer item;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
                if(armor==null)armor=new F35HelmetRenderer();
                armor.prepForRender(entity,stack,slot,original);return armor;
            }
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(item==null)item=new F35HelmetItemRenderer();return item;}
        });
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){ }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,level,tooltip,flag);
        tooltip.add(Component.translatable("tooltip.vsia.f35_helmet.hud"));
        tooltip.add(Component.translatable("tooltip.vsia.f35_helmet.link"));
    }
}
