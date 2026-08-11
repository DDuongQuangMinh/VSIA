package com.k1ngtle.vsia.weapon.attachment;

/**
 * The fixed slot types every gun can have. A gun declares which of these
 * slots it exposes (via GunItem.Builder#withAttachmentSlot), and an
 * AttachmentItem declares which single category it belongs to.
 */
public enum AttachmentCategory {
    SCOPE,
    MUZZLE,
    RAIL,
    UNDERBARREL,
    STOCK,
    MAGAZINE,
    LASER,
    SKIN;

    public String key() {
        return name().toLowerCase();
    }

    public static AttachmentCategory fromKey(String key) {
        return valueOf(key.toUpperCase());
    }
}