package com.k1ngtle.vsia.signality.engineering.wifi.dns;

import java.util.List;

public record DnsMessage(
        int id,
        boolean response,
        boolean authoritative,
        boolean truncated,
        boolean recursionDesired,
        boolean recursionAvailable,
        int responseCode,
        List<DnsQuestion> questions,
        List<DnsResourceRecord> answers
) {
    public DnsMessage {
        questions = questions == null ? List.of() : List.copyOf(questions);
        answers = answers == null ? List.of() : List.copyOf(answers);
    }

    public boolean noError() {
        return responseCode == 0;
    }

    public boolean nameError() {
        return responseCode == 3;
    }
}
