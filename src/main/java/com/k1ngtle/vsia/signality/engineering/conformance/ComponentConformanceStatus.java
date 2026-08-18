package com.k1ngtle.vsia.signality.engineering.conformance;

public record ComponentConformanceStatus(
        String component,
        ImplementationLevel level,
        String referenceId,
        String note
) {
}
