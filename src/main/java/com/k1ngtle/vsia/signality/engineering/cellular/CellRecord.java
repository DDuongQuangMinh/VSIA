package com.k1ngtle.vsia.signality.engineering.cellular;
import java.util.UUID;
public record CellRecord(
        UUID baseStationId,
        int physicalCellId,
        String plmn,
        long cellIdentity,
        double frequencyHz,
        double rsrpDbm,
        double snrDb,
        long lastSeenNanos
) {}
