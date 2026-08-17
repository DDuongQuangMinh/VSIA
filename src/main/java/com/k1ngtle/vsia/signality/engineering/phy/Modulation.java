package com.k1ngtle.vsia.signality.engineering.phy;

public enum Modulation {
    BPSK(2),
    QPSK(4),
    QAM16(16),
    QAM64(64),
    QAM256(256),
    QAM1024(1024),
    QAM4096(4096);

    private final int order;

    Modulation(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }

    public int bitsPerSymbol() {
        return Integer.numberOfTrailingZeros(order);
    }

    public boolean isSquareQam() {
        int side = (int) Math.round(Math.sqrt(order));
        return side * side == order;
    }
}
