package com.k1ngtle.vsia.signality.engineering.cellular;
public final class CellularCqiModel {
    private CellularCqiModel() {}
    public static int fromSnrDb(double snrDb) {
        if (!Double.isFinite(snrDb)) return 1;
        int cqi = 1 + (int)Math.floor((snrDb + 6.0) / 2.0);
        return Math.max(1, Math.min(15, cqi));
    }
}
