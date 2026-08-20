package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public final class VsShipIpv6Validator {
    private VsShipIpv6Validator() {
    }

    public static Validation validate(
            String mac,
            Ipv6Prefix prefix,
            double worldX,
            double worldY,
            double worldZ,
            double shipX,
            double shipY,
            double shipZ
    ) {
        Ipv6Address linkLocal = Ipv6Address.linkLocalFromMac(mac);
        Ipv6Address global = SlaacEngine.formAddress(prefix, mac);

        boolean addressIndependentOfCoordinates =
                linkLocal.equals(Ipv6Address.linkLocalFromMac(mac))
                        && global.equals(SlaacEngine.formAddress(prefix, mac));

        boolean finite =
                Double.isFinite(worldX)
                        && Double.isFinite(worldY)
                        && Double.isFinite(worldZ)
                        && Double.isFinite(shipX)
                        && Double.isFinite(shipY)
                        && Double.isFinite(shipZ);

        return new Validation(
                linkLocal,
                global,
                addressIndependentOfCoordinates && finite,
                finite
                        ? "IPv6 identity remains interface/MAC based across VS coordinate transforms"
                        : "Non-finite VS coordinate supplied"
        );
    }

    public record Validation(
            Ipv6Address linkLocal,
            Ipv6Address global,
            boolean valid,
            String detail
    ) {
    }
}
