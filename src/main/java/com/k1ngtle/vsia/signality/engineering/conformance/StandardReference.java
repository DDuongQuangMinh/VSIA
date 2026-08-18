package com.k1ngtle.vsia.signality.engineering.conformance;

public record StandardReference(
        String id,
        StandardsOrganization organization,
        String document,
        String revision,
        String scope,
        String sourceArtifact
) {
    public StandardReference {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }

        if (organization == null) {
            throw new IllegalArgumentException("organization");
        }

        document = document == null ? "" : document;
        revision = revision == null ? "" : revision;
        scope = scope == null ? "" : scope;
        sourceArtifact = sourceArtifact == null ? "" : sourceArtifact;
    }
}
