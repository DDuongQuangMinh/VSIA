package com.k1ngtle.vsia.weapon;

import com.k1ngtle.vsia.weapon.attachment.AttachmentCategory;
import com.k1ngtle.vsia.weapon.attachment.AttachmentItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class GunItem extends Item implements GeoItem {

    private static final RawAnimation ANIM_DRAW =
            RawAnimation.begin().thenPlayAndHold("animation.model.draw");

    private static final RawAnimation ANIM_FIRE =
            RawAnimation.begin().thenPlayAndHold("animation.model.fire");

    private static final RawAnimation ANIM_RELOAD =
            RawAnimation.begin().thenPlayAndHold("animation.model.reload");

    private static final RawAnimation ANIM_INSPECT =
            RawAnimation.begin().thenPlayAndHold("animation.model.inspect");

    private static final String TAG_AMMO = "Ammo";
    private static final String TAG_ATTACHMENTS = "Attachments";
    private static final String TAG_TRIGGER_ANIM = "TriggerAnim";
    private static final String TAG_FIRE_MODE = "FireMode";

    private final AnimatableInstanceCache animatableCache =
            GeckoLibUtil.createInstanceCache(this);

    private final String name;
    private final String compatibilityGroup;
    private final int rpm;
    private final int burstSize;
    private final Set<FireMode> fireModes;
    private final int maxAmmoCapacity;
    private final int reloadTicks;
    private final float damage;
    private final double range;
    private final double recoilPitch;
    private final double recoilYaw;
    private final double recoilRecoverySpeed;
    private final double baseInaccuracy;
    private final double aimingZoom;
    private final Set<Supplier<? extends Item>> compatibleAmmo;
    private final Set<AttachmentCategory> attachmentSlots;

    protected GunItem(Builder builder) {
        super(builder.properties);
        this.name = builder.name;
        this.compatibilityGroup = builder.compatibilityGroup;
        this.rpm = builder.rpm;
        this.burstSize = builder.burstSize;
        this.fireModes = builder.fireModes;
        this.maxAmmoCapacity = builder.maxAmmoCapacity;
        this.reloadTicks = builder.reloadTicks;
        this.damage = builder.damage;
        this.range = builder.range;
        this.recoilPitch = builder.recoilPitch;
        this.recoilYaw = builder.recoilYaw;
        this.recoilRecoverySpeed = builder.recoilRecoverySpeed;
        this.baseInaccuracy = builder.baseInaccuracy;
        this.aimingZoom = builder.aimingZoom;
        this.compatibleAmmo = builder.compatibleAmmo;
        this.attachmentSlots = builder.attachmentSlots;
    }

    public String getGunName() {
        return name;
    }

    public String getCompatibilityGroup() {
        return compatibilityGroup;
    }

    public int getRpm() {
        return rpm;
    }

    public int getBurstSize() {
        return burstSize;
    }

    public Set<FireMode> getFireModes() {
        return fireModes;
    }

    public int getMaxAmmoCapacity() {
        return maxAmmoCapacity;
    }

    public int getReloadTicks() {
        return reloadTicks;
    }

    public float getBaseDamage() {
        return damage;
    }

    public double getRange() {
        return range;
    }

    public double getRecoilPitch() {
        return recoilPitch;
    }

    public double getRecoilYaw() {
        return recoilYaw;
    }

    public double getRecoilRecoverySpeed() {
        return recoilRecoverySpeed;
    }

    public double getBaseInaccuracy() {
        return baseInaccuracy;
    }

    public double getAimingZoom() {
        return aimingZoom;
    }

    public Set<Supplier<? extends Item>> getCompatibleAmmo() {
        return compatibleAmmo;
    }

    public Set<AttachmentCategory> getAttachmentSlots() {
        return attachmentSlots;
    }

    public int getTicksBetweenShots() {
        return Math.max(1, Math.round(1200f / rpm));
    }

    public boolean isAmmoCompatible(Item ammoItem) {
        for (Supplier<? extends Item> supplier : compatibleAmmo) {
            if (supplier.get() == ammoItem) {
                return true;
            }
        }

        return compatibleAmmo.isEmpty();
    }

    public float getEffectiveDamage(ItemStack gunStack) {
        float total = damage;

        for (AttachmentCategory slot : attachmentSlots) {
            AttachmentItem attachment = getAttachmentItem(gunStack, slot);

            if (attachment != null) {
                total += attachment.getDamageModifier();
            }
        }

        return Math.max(0f, total);
    }

    public double getEffectiveInaccuracy(ItemStack gunStack) {
        double total = baseInaccuracy;

        for (AttachmentCategory slot : attachmentSlots) {
            AttachmentItem attachment = getAttachmentItem(gunStack, slot);

            if (attachment != null) {
                total *= 1.0 + attachment.getInaccuracyModifier();
            }
        }

        return Math.max(0.0, total);
    }

    public double getEffectiveRecoilPitch(ItemStack gunStack) {
        double total = recoilPitch;

        for (AttachmentCategory slot : attachmentSlots) {
            AttachmentItem attachment = getAttachmentItem(gunStack, slot);

            if (attachment != null) {
                total *= 1.0 + attachment.getRecoilModifier();
            }
        }

        return Math.max(0.0, total);
    }

    public double getEffectiveAimingZoom(ItemStack gunStack) {
        double total = aimingZoom;

        for (AttachmentCategory slot : attachmentSlots) {
            AttachmentItem attachment = getAttachmentItem(gunStack, slot);

            if (attachment != null) {
                total += attachment.getAimingZoomModifier();
            }
        }

        return Math.max(0.0, total);
    }

    public int getAmmo(ItemStack gunStack) {
        CompoundTag tag = gunStack.getTag();

        if (tag == null || !tag.contains(TAG_AMMO)) {
            return 0;
        }

        return tag.getInt(TAG_AMMO);
    }

    public void setAmmo(ItemStack gunStack, int ammo) {
        gunStack.getOrCreateTag().putInt(
                TAG_AMMO,
                Math.max(0, Math.min(ammo, maxAmmoCapacity))
        );
    }

    public boolean canFire(ItemStack gunStack) {
        return getAmmo(gunStack) > 0;
    }

    public FireMode getFireMode(ItemStack gunStack) {
        CompoundTag tag = gunStack.getTag();
        if (tag != null && tag.contains(TAG_FIRE_MODE)) {
            try {
                return FireMode.valueOf(tag.getString(TAG_FIRE_MODE));
            } catch (IllegalArgumentException ignored) {}
        }
        return fireModes.isEmpty() ? FireMode.SINGLE : fireModes.iterator().next();
    }

    public void cycleFireMode(ItemStack gunStack) {
        if (fireModes.size() <= 1) return;
        FireMode current = getFireMode(gunStack);
        FireMode[] supported = fireModes.toArray(new FireMode[0]);
        FireMode next = FireMode.next(current, supported);
        gunStack.getOrCreateTag().putString(TAG_FIRE_MODE, next.name());
    }

    public void setAttachment(ItemStack gunStack, AttachmentItem attachment) {
        if (!attachmentSlots.contains(attachment.getCategory())) {
            throw new IllegalArgumentException(
                    name + " has no " + attachment.getCategory() + " slot"
            );
        }

        CompoundTag attachments =
                gunStack.getOrCreateTag().getCompound(TAG_ATTACHMENTS);

        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(attachment);

        attachments.putString(
                attachment.getCategory().key(),
                id.toString()
        );

        gunStack.getOrCreateTag().put(
                TAG_ATTACHMENTS,
                attachments
        );
    }

    public void clearAttachment(
            ItemStack gunStack,
            AttachmentCategory category
    ) {
        if (!gunStack.hasTag()) {
            return;
        }

        CompoundTag attachments =
                gunStack.getTag().getCompound(TAG_ATTACHMENTS);

        attachments.remove(category.key());
    }

    public AttachmentItem getAttachmentItem(
            ItemStack gunStack,
            AttachmentCategory category
    ) {
        if (!gunStack.hasTag()) {
            return null;
        }

        CompoundTag attachments =
                gunStack.getTag().getCompound(TAG_ATTACHMENTS);

        if (!attachments.contains(category.key())) {
            return null;
        }

        ResourceLocation id =
                ResourceLocation.tryParse(
                        attachments.getString(category.key())
                );

        if (id == null) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(id);

        if (item instanceof AttachmentItem attachmentItem) {
            return attachmentItem;
        }

        return null;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "main",
                        0,
                        this::animationPredicate
                )
        );
    }

    private PlayState animationPredicate(
            software.bernie.geckolib.core.animation.AnimationState<GunItem> state
    ) {
        ItemStack stack =
                state.getData(
                        software.bernie.geckolib.constant.DataTickets.ITEMSTACK
                );

        if (stack != null && stack.hasTag()) {
            CompoundTag tag = stack.getTag();

            if (tag.contains(TAG_TRIGGER_ANIM)) {
                String trigger =
                        tag.getString(TAG_TRIGGER_ANIM);

                if (!trigger.isEmpty()) {
                    tag.putString(TAG_TRIGGER_ANIM, "");

                    switch (trigger) {
                        case "inspect" -> {
                            state.getController().forceAnimationReset();
                            state.getController().setAnimation(ANIM_INSPECT);
                        }

                        case "reload" -> {
                            state.getController().forceAnimationReset();
                            state.getController().setAnimation(ANIM_RELOAD);
                        }

                        case "fire" -> {
                            state.getController().forceAnimationReset();
                            state.getController().setAnimation(ANIM_FIRE);
                        }
                    }

                    return PlayState.CONTINUE;
                }
            }
        }

        if (state.getController().getCurrentAnimation() == null) {
            state.getController().setAnimation(ANIM_DRAW);
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    public void initializeClient(
            java.util.function.Consumer<
                    net.minecraftforge.client.extensions.common.IClientItemExtensions
                    > consumer
    ) {
        consumer.accept(
                com.k1ngtle.vsia.weapon.client.GunItemClientExtensions.INSTANCE
        );
    }

    public static class Builder {

        private Properties properties =
                new Properties().stacksTo(1);

        private String name;
        private String compatibilityGroup = "";
        private int rpm = 600;
        private int burstSize = 3;
        private Set<FireMode> fireModes =
                EnumSet.of(FireMode.SINGLE);
        private int maxAmmoCapacity = 30;
        private int reloadTicks = 60;
        private float damage = 4.0f;
        private double range = 100.0;
        private double recoilPitch = 2.0;
        private double recoilYaw = 0.4;
        private double recoilRecoverySpeed = 0.15;
        private double baseInaccuracy = 2.0;
        private double aimingZoom = 1.0;

        private final Set<Supplier<? extends Item>> compatibleAmmo =
                new HashSet<>();

        private final Set<AttachmentCategory> attachmentSlots =
                new HashSet<>();

        public Builder withProperties(Properties properties) {
            this.properties = properties;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withCompatibilityGroup(String group) {
            this.compatibilityGroup = group;
            return this;
        }

        public Builder withRpm(int rpm) {
            this.rpm = rpm;
            return this;
        }

        public Builder withBurstSize(int burstSize) {
            this.burstSize = burstSize;
            return this;
        }

        public Builder withFireModes(FireMode... modes) {
            this.fireModes =
                    EnumSet.copyOf(Set.of(modes));
            return this;
        }

        public Builder withMaxAmmoCapacity(
                int maxAmmoCapacity
        ) {
            this.maxAmmoCapacity = maxAmmoCapacity;
            return this;
        }

        public Builder withReloadTicks(
                int reloadTicks
        ) {
            this.reloadTicks = reloadTicks;
            return this;
        }

        public Builder withDamage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder withRange(double range) {
            this.range = range;
            return this;
        }

        public Builder withRecoil(
                double pitch,
                double yaw,
                double recoverySpeed
        ) {
            this.recoilPitch = pitch;
            this.recoilYaw = yaw;
            this.recoilRecoverySpeed = recoverySpeed;
            return this;
        }

        public Builder withInaccuracy(
                double baseInaccuracy
        ) {
            this.baseInaccuracy = baseInaccuracy;
            return this;
        }

        public Builder withAimingZoom(
                double aimingZoom
        ) {
            this.aimingZoom = aimingZoom;
            return this;
        }

        @SafeVarargs
        public final Builder withCompatibleAmmo(
                Supplier<? extends Item>... ammo
        ) {
            this.compatibleAmmo.addAll(Set.of(ammo));
            return this;
        }

        public Builder withAttachmentSlot(
                AttachmentCategory... categories
        ) {
            this.attachmentSlots.addAll(
                    Set.of(categories)
            );
            return this;
        }

        public GunItem build() {
            if (name == null) {
                throw new IllegalStateException(
                        "GunItem.Builder requires withName(...)"
                );
            }

            return new GunItem(this);
        }
    }
}