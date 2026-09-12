package com.k1ngtle.vsia.phone.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserHistory {
    private final List<String> entries = new ArrayList<>();
    private int index = -1;

    public void visit(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        if (index >= 0
                && index < entries.size()
                && entries.get(index).equals(url)) {
            return;
        }

        while (entries.size() > index + 1) {
            entries.remove(entries.size() - 1);
        }

        entries.add(url);
        index = entries.size() - 1;
    }

    public boolean canGoBack() {
        return index > 0;
    }

    public boolean canGoForward() {
        return index >= 0 && index < entries.size() - 1;
    }

    public String back() {
        if (!canGoBack()) {
            return current();
        }

        index--;
        return current();
    }

    public String forward() {
        if (!canGoForward()) {
            return current();
        }

        index++;
        return current();
    }

    public String current() {
        if (index < 0 || index >= entries.size()) {
            return "";
        }

        return entries.get(index);
    }

    public List<String> entries() {
        return Collections.unmodifiableList(entries);
    }
}
