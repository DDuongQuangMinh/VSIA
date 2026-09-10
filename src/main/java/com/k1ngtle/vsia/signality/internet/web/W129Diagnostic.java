package com.k1ngtle.vsia.signality.internet.web;

public record W129Diagnostic(
        Severity severity,
        int line,
        int column,
        String message
) {
    public W129Diagnostic {
        severity = severity == null ? Severity.INFO : severity;
        line = Math.max(1, line);
        column = Math.max(1, column);
        message = message == null ? "" : message.replace('\n', ' ').replace('\r', ' ');
    }

    public String toWire() {
        return severity.name()
                + "|"
                + line
                + "|"
                + column
                + "|"
                + message.replace("|", "/");
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }
}
