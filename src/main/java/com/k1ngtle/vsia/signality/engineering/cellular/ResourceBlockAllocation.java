package com.k1ngtle.vsia.signality.engineering.cellular;
import java.util.UUID;
public record ResourceBlockAllocation(
        UUID ueId,
        int startResourceBlock,
        int resourceBlockCount,
        int cqi
) {}
