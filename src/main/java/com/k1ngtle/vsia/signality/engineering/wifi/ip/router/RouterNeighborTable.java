package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RouterNeighborTable {
    private final Map<String, String> neighbors =
            new LinkedHashMap<>();

    public void learn(
            String iface,
            String ip,
            String mac
    ) {
        if (iface == null
                || iface.isBlank()
                || ip == null
                || ip.isBlank()
                || mac == null
                || mac.isBlank()) {
            return;
        }

        neighbors.put(
                iface + "|" + ip,
                mac
        );
    }

    public String lookup(
            String iface,
            String ip
    ) {
        return neighbors.getOrDefault(
                iface + "|" + ip,
                ""
        );
    }

    public String interfaceForMac(
            String mac
    ) {
        if (mac == null
                || mac.isBlank()) {
            return "";
        }

        for (Map.Entry<String, String> entry
                : neighbors.entrySet()) {
            if (!entry.getValue()
                    .equalsIgnoreCase(
                            mac
                    )) {
                continue;
            }

            String key =
                    entry.getKey();

            int separator =
                    key.indexOf('|');

            if (separator > 0) {
                return key.substring(
                        0,
                        separator
                );
            }
        }

        return "";
    }

    public String ipForMac(
            String iface,
            String mac
    ) {
        if (iface == null
                || iface.isBlank()
                || mac == null
                || mac.isBlank()) {
            return "";
        }

        String prefix =
                iface + "|";

        for (Map.Entry<String, String> entry
                : neighbors.entrySet()) {
            if (!entry.getKey()
                    .startsWith(
                            prefix
                    )
                    || !entry.getValue()
                    .equalsIgnoreCase(
                            mac
                    )) {
                continue;
            }

            return entry.getKey()
                    .substring(
                            prefix.length()
                    );
        }

        return "";
    }

    public void clear() {
        neighbors.clear();
    }

    public int size() {
        return neighbors.size();
    }
}
