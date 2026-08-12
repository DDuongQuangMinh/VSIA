package com.k1ngtle.vsia.weapon.state;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class WeaponActionController {
    private static final Map<WeaponAction, EnumSet<WeaponAction>> TRANSITIONS = createTransitions();
    private WeaponAction current = WeaponAction.IDLE;

    public WeaponAction current() { return current; }

    public boolean canTransitionTo(WeaponAction next) {
        return next == current || TRANSITIONS.getOrDefault(current, EnumSet.noneOf(WeaponAction.class)).contains(next);
    }

    public boolean tryTransitionTo(WeaponAction next) {
        if (!canTransitionTo(next)) return false;
        current = next;
        return true;
    }

    public void reset() { current = WeaponAction.IDLE; }

    public boolean isLocked() {
        return switch (current) {
            case DRAW, HOLSTER, RELOAD, RELOAD_EMPTY, BOLT, INSPECT, MELEE -> true;
            default -> false;
        };
    }

    private static Map<WeaponAction, EnumSet<WeaponAction>> createTransitions() {
        Map<WeaponAction, EnumSet<WeaponAction>> map = new EnumMap<>(WeaponAction.class);
        map.put(WeaponAction.IDLE, EnumSet.of(WeaponAction.DRAW, WeaponAction.HOLSTER, WeaponAction.AIM,
                WeaponAction.FIRE, WeaponAction.RELOAD, WeaponAction.RELOAD_EMPTY, WeaponAction.BOLT,
                WeaponAction.INSPECT, WeaponAction.MELEE));
        map.put(WeaponAction.AIM, EnumSet.of(WeaponAction.IDLE, WeaponAction.FIRE,
                WeaponAction.RELOAD, WeaponAction.RELOAD_EMPTY));
        map.put(WeaponAction.FIRE, EnumSet.of(WeaponAction.IDLE, WeaponAction.AIM, WeaponAction.FIRE));
        for (WeaponAction action : WeaponAction.values()) {
            map.putIfAbsent(action, EnumSet.of(WeaponAction.IDLE));
        }
        return Map.copyOf(map);
    }
}
