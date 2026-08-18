package com.k1ngtle.vsia.signality.engineering.cellular.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HarqManager {
    private final List<HarqProcess> processes;
    private int nextProcess;

    public HarqManager(int processCount) {
        if (processCount < 1) {
            throw new IllegalArgumentException("processCount");
        }

        List<HarqProcess> list = new ArrayList<>(processCount);

        for (int i = 0; i < processCount; i++) {
            list.add(new HarqProcess(i));
        }

        this.processes = Collections.unmodifiableList(list);
    }

    public HarqProcess allocate(long transportBlockId) {
        for (int i = 0; i < processes.size(); i++) {
            int index = (nextProcess + i) % processes.size();
            HarqProcess process = processes.get(index);

            if (!process.awaitingAck()) {
                process.begin(transportBlockId);
                nextProcess = (index + 1) % processes.size();
                return process;
            }
        }

        return null;
    }

    public HarqProcess process(int processId) {
        if (processId < 0 || processId >= processes.size()) {
            return null;
        }

        return processes.get(processId);
    }

    public List<HarqProcess> processes() {
        return processes;
    }
}
