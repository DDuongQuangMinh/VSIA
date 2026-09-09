package com.k1ngtle.vsia.signality.engineering.wifi.integration.w124;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;

public record W124QosClassResult(
        WifiAccessCategory category,
        int accepted,
        long successes,
        long retries,
        long drops,
        long deferrals,
        int queuePeak,
        boolean macCategoryObserved
) {
}
