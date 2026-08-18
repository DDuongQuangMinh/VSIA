package com.k1ngtle.vsia.signality.engineering.conformance;

public record KnownAnswerResult(
        String id,
        boolean passed,
        String expected,
        String actual,
        String note
) {
}
