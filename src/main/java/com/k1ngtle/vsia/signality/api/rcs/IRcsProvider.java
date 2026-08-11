package com.k1ngtle.vsia.signality.api.rcs;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;

@FunctionalInterface
public interface IRcsProvider {
   double computeRcs(IRadarTarget var1, IRadarEmitter var2, double var3);
}
