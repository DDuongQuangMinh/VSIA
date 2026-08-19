package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class RouterPersistenceCodec {
    private static final int VERSION =
            1;

    private RouterPersistenceCodec() {
    }

    public static CompoundTag encode(
            RouterEngine router,
            boolean enabled
    ) {
        CompoundTag root =
                new CompoundTag();

        root.putInt(
                "Version",
                VERSION
        );

        root.putBoolean(
                "Enabled",
                enabled
        );

        ListTag interfaces =
                new ListTag();

        for (RouterInterface iface
                : router.interfaces()) {
            CompoundTag value =
                    new CompoundTag();

            value.putString(
                    "Name",
                    iface.name()
            );

            value.putString(
                    "Ipv4",
                    iface.ipv4Address()
            );

            value.putInt(
                    "Prefix",
                    iface.prefixLength()
            );

            value.putString(
                    "Mac",
                    iface.macAddress()
            );

            value.putBoolean(
                    "Enabled",
                    iface.enabled()
            );

            interfaces.add(value);
        }

        root.put(
                "Interfaces",
                interfaces
        );

        ListTag routes =
                new ListTag();

        for (RouterRoute route
                : router.staticRoutes()) {
            CompoundTag value =
                    new CompoundTag();

            value.putString(
                    "Network",
                    route.network()
            );

            value.putInt(
                    "Prefix",
                    route.prefixLength()
            );

            value.putString(
                    "NextHop",
                    route.nextHop()
            );

            value.putString(
                    "Interface",
                    route.egressInterface()
            );

            value.putInt(
                    "Metric",
                    route.metric()
            );

            value.putString(
                    "Source",
                    route.source()
            );

            routes.add(value);
        }

        root.put(
                "StaticRoutes",
                routes
        );

        return root;
    }

    public static boolean decode(
            CompoundTag root,
            RouterEngine router
    ) {
        if (root == null) {
            return false;
        }

        router.clearConfiguration();

        ListTag interfaces =
                root.getList(
                        "Interfaces",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0;
                i < interfaces.size();
                i++) {
            CompoundTag value =
                    interfaces.getCompound(i);

            try {
                router.putInterface(
                        new RouterInterface(
                                value.getString(
                                        "Name"
                                ),
                                value.getString(
                                        "Ipv4"
                                ),
                                value.getInt(
                                        "Prefix"
                                ),
                                value.getString(
                                        "Mac"
                                ),
                                value.getBoolean(
                                        "Enabled"
                                )
                        )
                );
            } catch (IllegalArgumentException ignored) {
            }
        }

        ListTag routes =
                root.getList(
                        "StaticRoutes",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0;
                i < routes.size();
                i++) {
            CompoundTag value =
                    routes.getCompound(i);

            try {
                router.addRoute(
                        new RouterRoute(
                                value.getString(
                                        "Network"
                                ),
                                value.getInt(
                                        "Prefix"
                                ),
                                value.getString(
                                        "NextHop"
                                ),
                                value.getString(
                                        "Interface"
                                ),
                                value.getInt(
                                        "Metric"
                                ),
                                value.getString(
                                        "Source"
                                )
                        )
                );
            } catch (IllegalArgumentException ignored) {
            }
        }

        return root.getBoolean(
                "Enabled"
        );
    }
}
