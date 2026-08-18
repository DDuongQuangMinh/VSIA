package com.k1ngtle.vsia.signality.engineering.wifi;

import java.util.Random;

public final class CsmaCaController {
    private final int cwMin;
    private final int cwMax;

    private int cw;
    private int backoff;
    private int retries;

    public CsmaCaController(
            int cwMin,
            int cwMax,
            Random random
    ) {
        if (cwMin < 1 || cwMax < cwMin) {
            throw new IllegalArgumentException(
                    "Invalid contention window"
            );
        }

        this.cwMin = cwMin;
        this.cwMax = cwMax;
        this.cw = cwMin;

        select(random);
    }

    public int contentionWindow() {
        return cw;
    }

    public int backoffSlots() {
        return backoff;
    }

    public int retryCount() {
        return retries;
    }

    public boolean tickIdleSlot() {
        if (backoff > 0) {
            backoff--;
        }

        return backoff == 0;
    }

    public void onMediumBusy() {
    }

    public void onSuccess(Random random) {
        retries = 0;
        cw = cwMin;
        select(random);
    }

    public void onFailure(Random random) {
        retries++;

        cw = Math.min(
                cwMax,
                ((cw + 1) * 2) - 1
        );

        select(random);
    }

    public int consumeBackoffForSimplifiedExecution() {
        int value = backoff;
        backoff = 0;
        return value;
    }

    private void select(Random random) {
        backoff =
                random.nextInt(
                        cw + 1
                );
    }
}
