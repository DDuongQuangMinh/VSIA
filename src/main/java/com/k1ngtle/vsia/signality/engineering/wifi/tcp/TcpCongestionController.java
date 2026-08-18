package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

public final class TcpCongestionController {
    private final int smssBytes;

    private long cwndBytes;

    private long ssthreshBytes;

    private int duplicateAckCount;

    private boolean fastRecovery;

    private long recoverSequence;

    public TcpCongestionController(
            int smssBytes
    ) {
        if (smssBytes <= 0) {
            throw new IllegalArgumentException(
                    "smssBytes"
            );
        }

        this.smssBytes =
                smssBytes;

        this.cwndBytes =
                initialWindow(
                        smssBytes
                );

        this.ssthreshBytes =
                65_535L;
    }

    public long cwndBytes() {
        return cwndBytes;
    }

    public long ssthreshBytes() {
        return ssthreshBytes;
    }

    public int duplicateAckCount() {
        return duplicateAckCount;
    }

    public boolean fastRecovery() {
        return fastRecovery;
    }

    public int smssBytes() {
        return smssBytes;
    }

    public long usableWindowBytes(
            int receiverWindowBytes,
            long bytesInFlight
    ) {
        long sendWindow =
                Math.min(
                        cwndBytes,
                        Math.max(
                                0,
                                receiverWindowBytes
                        )
                );

        return Math.max(
                0L,
                sendWindow
                        - Math.max(
                        0L,
                        bytesInFlight
                )
        );
    }

    public void onNewAcknowledgement(
            int newlyAcknowledgedBytes,
            long acknowledgementNumber
    ) {
        duplicateAckCount =
                0;

        if (fastRecovery) {
            if (TcpSequence.beforeOrEqual(
                    recoverSequence,
                    acknowledgementNumber
            )) {
                fastRecovery =
                        false;

                cwndBytes =
                        Math.max(
                                smssBytes,
                                ssthreshBytes
                        );
            }

            return;
        }

        if (newlyAcknowledgedBytes <= 0) {
            return;
        }

        if (cwndBytes < ssthreshBytes) {
            cwndBytes +=
                    Math.min(
                            newlyAcknowledgedBytes,
                            smssBytes
                    );
        } else {
            long increment =
                    Math.max(
                            1L,
                            (
                                    (
                                            long
                                    ) smssBytes
                                            * smssBytes
                            )
                                    / Math.max(
                                    1L,
                                    cwndBytes
                            )
                    );

            cwndBytes +=
                    increment;
        }
    }

    public boolean onDuplicateAcknowledgement(
            long highestSequenceSent
    ) {
        duplicateAckCount++;

        if (!fastRecovery
                && duplicateAckCount == 3) {
            long flightEstimate =
                    Math.max(
                            2L
                                    * smssBytes,
                            cwndBytes
                    );

            ssthreshBytes =
                    Math.max(
                            2L
                                    * smssBytes,
                            flightEstimate
                                    / 2L
                    );

            cwndBytes =
                    ssthreshBytes
                            + 3L
                            * smssBytes;

            fastRecovery =
                    true;

            recoverSequence =
                    TcpSequence.normalize(
                            highestSequenceSent
                    );

            return true;
        }

        if (fastRecovery
                && duplicateAckCount > 3) {
            cwndBytes +=
                    smssBytes;
        }

        return false;
    }

    public void onRetransmissionTimeout(
            long flightSizeBytes
    ) {
        ssthreshBytes =
                Math.max(
                        2L
                                * smssBytes,
                        Math.max(
                                0L,
                                flightSizeBytes
                        )
                                / 2L
                );

        cwndBytes =
                smssBytes;

        duplicateAckCount =
                0;

        fastRecovery =
                false;
    }

    private static long initialWindow(
            int smssBytes
    ) {
        if (smssBytes > 2190) {
            return 2L
                    * smssBytes;
        }

        if (smssBytes > 1095) {
            return 3L
                    * smssBytes;
        }

        return 4L
                * smssBytes;
    }
}
