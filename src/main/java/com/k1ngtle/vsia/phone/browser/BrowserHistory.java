package com.k1ngtle.vsia.phone.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserHistory {
    private final List<String> entries = new ArrayList<>();

    public void visit(String url) {
        if (url != null && !url.isBlank()) {
            entries.add(url);
        }
    }

    public List<String> entries() {
        return Collections.unmodifiableList(entries);
    }
}
