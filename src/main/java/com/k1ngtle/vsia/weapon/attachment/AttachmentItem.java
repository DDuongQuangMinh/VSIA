package com.k1ngtle.vsia.weapon.attachment;

import net.minecraft.world.item.Item;
import java.util.HashSet;
import java.util.Set;

/**
 * A gun attachment - scope, muzzle device, underbarrel grip, etc.
 * Equipping is expected to happen through your own workstation/menu UI
 * (the same place you already hook Point Blank compatibility into),
 * which should call GunItem.setAttachment(gunStack, this) once the
 * player confirms the swap. This class only carries the attachment's
 * own definition data, not the equip interaction itself.
 */
public class AttachmentItem extends Item {

    private final AttachmentCategory category;
    /** Fine-grained compatibility tags, e.g. "pistol", "rifle_5_56". */
    private final Set<String> compatibilityGroups;
    private final float damageModifier;
    private final float recoilModifier;
    private final float inaccuracyModifier;
    private final double aimingZoomModifier;

    protected AttachmentItem(Builder builder) {
        super(builder.properties);
        this.category = builder.category;
        this.compatibilityGroups = builder.compatibilityGroups;
        this.damageModifier = builder.damageModifier;
        this.recoilModifier = builder.recoilModifier;
        this.inaccuracyModifier = builder.inaccuracyModifier;
        this.aimingZoomModifier = builder.aimingZoomModifier;
    }

    public AttachmentCategory getCategory() { return category; }
    public Set<String> getCompatibilityGroups() { return compatibilityGroups; }
    public float getDamageModifier() { return damageModifier; }
    public float getRecoilModifier() { return recoilModifier; }
    public float getInaccuracyModifier() { return inaccuracyModifier; }
    public double getAimingZoomModifier() { return aimingZoomModifier; }

    public boolean isCompatibleWithGroup(String gunGroup) {
        return compatibilityGroups.isEmpty() || compatibilityGroups.contains(gunGroup);
    }

    public static class Builder {
        private Properties properties = new Properties().stacksTo(1);
        private AttachmentCategory category;
        private final Set<String> compatibilityGroups = new HashSet<>();
        private float damageModifier = 0f;
        private float recoilModifier = 0f;
        private float inaccuracyModifier = 0f;
        private double aimingZoomModifier = 0.0;

        public Builder withProperties(Properties properties) {
            this.properties = properties;
            return this;
        }

        public Builder withCategory(AttachmentCategory category) {
            this.category = category;
            return this;
        }

        public Builder withCompatibilityGroup(String... groups) {
            this.compatibilityGroups.addAll(Set.of(groups));
            return this;
        }

        /** Flat damage added/removed when this attachment is equipped. */
        public Builder withDamageModifier(float damageModifier) {
            this.damageModifier = damageModifier;
            return this;
        }

        /** Multiplier applied to the gun's recoil kick, e.g. 0.7 for a compensator. */
        public Builder withRecoilModifier(float recoilModifier) {
            this.recoilModifier = recoilModifier;
            return this;
        }

        /** Multiplier applied to the gun's base inaccuracy. */
        public Builder withInaccuracyModifier(float inaccuracyModifier) {
            this.inaccuracyModifier = inaccuracyModifier;
            return this;
        }

        /** Additional zoom applied while aiming, e.g. for a scope. */
        public Builder withAimingZoomModifier(double aimingZoomModifier) {
            this.aimingZoomModifier = aimingZoomModifier;
            return this;
        }

        public AttachmentItem build() {
            if (category == null) {
                throw new IllegalStateException("AttachmentItem.Builder requires withCategory(...)");
            }
            return new AttachmentItem(this);
        }
    }
}