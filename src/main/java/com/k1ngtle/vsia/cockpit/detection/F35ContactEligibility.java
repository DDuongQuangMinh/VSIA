package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35SeatController;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared server-side eligibility, not merely Entity.isAlive(): embedded arrows remain alive. */
public final class F35ContactEligibility {
    private F35ContactEligibility() { }
    public static F35DetectionType classify(Entity e) {
        if(!e.isAlive()||e.isRemoved()||e.isInvisible()||F35SeatController.isSeatRenderMarker(e)||e instanceof ArmorStand||e instanceof FishingHook)return null;
        if(e instanceof Projectile||missileLike(e)) {
            boolean embedded=false;
            if(e instanceof AbstractArrow arrow){CompoundTag tag=new CompoundTag();arrow.addAdditionalSaveData(tag);embedded=tag.getBoolean("inGround");}
            return flying(true,false,embedded,e.onGround(),e.getDeltaMovement().lengthSqr())?F35DetectionType.MISSILE:null;
        }
        if(e instanceof Player)return F35DetectionType.PLAYER;
        return e instanceof Mob?F35DetectionType.MOB:null;
    }
    public static boolean flying(boolean alive,boolean removed,boolean embedded,boolean onGround,double motionSquared) {
        return alive&&!removed&&!embedded&&Double.isFinite(motionSquared)&&!(onGround&&motionSquared<1.0e-10);
    }
    public static boolean allowed(F35DetectionType type,F35DetectionFilter filter) {
        if(type==null)return true; // explicitly registered non-entity radar targets remain supported
        return switch(type){case MOB->filter.mobs();case PLAYER->filter.players();case SHIP->filter.ships();case MISSILE->filter.missiles();};
    }
    private static boolean missileLike(Entity e){
        ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        String name=(e.getClass().getSimpleName()+" "+(id==null?"":id.toString())).toLowerCase(Locale.ROOT);
        return name.contains("missile")||name.contains("rocket")||name.contains("munition")||name.contains("torpedo")||name.contains("guided_bomb")||name.contains("guidedbomb")||name.contains("bullet")||name.contains("projectile");
    }
}
