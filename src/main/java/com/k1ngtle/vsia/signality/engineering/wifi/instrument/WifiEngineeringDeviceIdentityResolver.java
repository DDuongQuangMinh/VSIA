package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class WifiEngineeringDeviceIdentityResolver {
    private WifiEngineeringDeviceIdentityResolver() {
    }

    public static NetworkDeviceBlockEntity resolve(
            ServerLevel level,
            UUID deviceId
    ) {
        if (level == null || deviceId == null) {
            return null;
        }

        for (ISignalReceiver receiver : SignalBus.receiversInLevel(level)) {
            if (!deviceId.equals(receiver.id())) {
                continue;
            }

            if (receiver instanceof NetworkDeviceBlockEntity device) {
                return device;
            }
        }

        return null;
    }
}
