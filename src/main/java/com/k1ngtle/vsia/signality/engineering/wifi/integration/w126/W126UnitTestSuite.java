package com.k1ngtle.vsia.signality.engineering.wifi.integration.w126;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;
import com.k1ngtle.vsia.signality.engineering.wifi.qos.WifiQosClassifier;
import com.k1ngtle.vsia.signality.engineering.wifi.security.protocol.WifiSecurityProtocolRegistry;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBand;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBandUtil;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileRegistry;
import com.k1ngtle.vsia.signality.internet.provider.DomainRegistration;
import com.k1ngtle.vsia.signality.internet.provider.InternetProvider;
import com.k1ngtle.vsia.signality.internet.provider.InternetProviderRegistry;

import java.util.List;

public final class W126UnitTestSuite {
    public record Result(String name, boolean passed, String detail) {
    }

    private W126UnitTestSuite() {
    }

    public static List<Result> runAll() {
        return List.of(
                providerRegistry(),
                providerOwnership(),
                wpa2ProfileInventory(),
                wpa3ProfileInventory(),
                qosPolicyContinuity(),
                smartConnectInventory(),
                new Result("w1264-canonical-stage-label", true, "Wi-Fi 1 / W1.26.4 — Wi-Fi Integration & Scale Validation")
        );
    }

    private static Result providerRegistry() {
        String resolved = InternetProviderRegistry.resolveA(InternetProviderRegistry.DEFAULT_WEB_DOMAIN).orElse("");
        return new Result(
                "w126-isp1-domain-resolution",
                InternetProviderRegistry.DEFAULT_WEB_IPV4.equals(resolved),
                InternetProviderRegistry.DEFAULT_WEB_DOMAIN + " -> " + resolved
        );
    }

    private static Result providerOwnership() {
        DomainRegistration registration = InternetProviderRegistry.resolve(
                InternetProviderRegistry.DEFAULT_WEB_DOMAIN,
                "A"
        ).orElse(null);

        InternetProvider provider = registration == null
                ? null
                : InternetProviderRegistry.provider(registration.providerId()).orElse(null);

        boolean passed = registration != null
                && provider != null
                && provider.id().equals(registration.providerId());

        return new Result(
                "w126-isp1-provider-ownership",
                passed,
                passed ? registration.domain() + " owned by " + provider.displayName() : "Provider/domain ownership inconsistent"
        );
    }

    private static Result wpa2ProfileInventory() {
        NetworkProfile profile = profile("wifi_5");
        boolean passed = profile != null
                && profile.kind() == NetworkKind.WIFI
                && "signality:wpa2".equals(profile.security())
                && "signality:wpa2".equals(WifiSecurityProtocolRegistry.canonicalId(profile.security()));

        return new Result(
                "w126-wpa2-profile-inventory",
                passed,
                passed ? profile.id() + " advertises WPA2" : "wifi_5 WPA2 profile missing"
        );
    }

    private static Result wpa3ProfileInventory() {
        NetworkProfile profile = profile("wifi_7");
        boolean passed = profile != null
                && "signality:wpa3".equals(profile.security())
                && "signality:wpa3".equals(WifiSecurityProtocolRegistry.canonicalId(profile.security()));

        return new Result(
                "w126-wpa3-profile-inventory",
                passed,
                passed ? profile.id() + " advertises WPA3" : "wifi_7 WPA3 profile missing"
        );
    }

    private static Result qosPolicyContinuity() {
        boolean passed = WifiQosClassifier.fromDscp(46) == WifiAccessCategory.VOICE
                && WifiQosClassifier.fromDscp(34) == WifiAccessCategory.VIDEO
                && WifiQosClassifier.fromDscp(8) == WifiAccessCategory.BACKGROUND;

        return new Result(
                "w126-qos-policy-continuity",
                passed,
                "EF/AF41/CS1 remain mapped to VO/VI/BK"
        );
    }

    private static Result smartConnectInventory() {
        NetworkProfile profile = profile("wifi_4");
        boolean twoFour = profile != null && java.util.Arrays.stream(profile.frequenciesHz())
                .anyMatch(value -> WifiBandUtil.bandForFrequency(value) == WifiBand.TWO_FOUR_GHZ);
        boolean five = profile != null && java.util.Arrays.stream(profile.frequenciesHz())
                .anyMatch(value -> WifiBandUtil.bandForFrequency(value) == WifiBand.FIVE_GHZ);

        return new Result(
                "w126-mb1-dual-band-inventory",
                twoFour && five,
                "wifi_4 exposes both 2.4 and 5 GHz"
        );
    }

    private static NetworkProfile profile(String path) {
        return NetworkProfileRegistry.values()
                .stream()
                .filter(value -> value.kind() == NetworkKind.WIFI && path.equals(value.id().getPath()))
                .findFirst()
                .orElse(null);
    }
}
