package com.k1ngtle.vsia.signality.engineering.conformance;

public record LabCheckResult(
        String id,
        boolean passed,
        String detail
) {
}
