package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public record QcLdpcBaseMatrix(
        String id,
        int expansionFactor,
        int[][] shifts,
        boolean standardized
) {
    public static final int ZERO_BLOCK =
            -1;

    public QcLdpcBaseMatrix {
        if (id == null
                || id.isBlank()) {
            throw new IllegalArgumentException(
                    "id"
            );
        }

        if (expansionFactor < 1) {
            throw new IllegalArgumentException(
                    "expansionFactor"
            );
        }

        if (shifts == null
                || shifts.length == 0) {
            throw new IllegalArgumentException(
                    "shifts"
            );
        }

        int columns =
                shifts[0].length;

        if (columns == 0) {
            throw new IllegalArgumentException(
                    "matrix has no columns"
            );
        }

        int[][] copy =
                new int[
                        shifts.length
                        ][
                        columns
                        ];

        for (int r = 0;
             r < shifts.length;
             r++) {
            if (shifts[r].length != columns) {
                throw new IllegalArgumentException(
                        "ragged base matrix"
                );
            }

            for (int c = 0;
                 c < columns;
                 c++) {
                int shift =
                        shifts[r][c];

                if (shift < ZERO_BLOCK
                        || shift >= expansionFactor) {
                    throw new IllegalArgumentException(
                            "invalid circulant shift"
                    );
                }

                copy[r][c] =
                        shift;
            }
        }

        shifts =
                copy;
    }

    @Override
    public int[][] shifts() {
        int[][] copy =
                new int[
                        shifts.length
                        ][];

        for (int i = 0;
             i < shifts.length;
             i++) {
            copy[i] =
                    shifts[i].clone();
        }

        return copy;
    }

    public int baseRows() {
        return shifts.length;
    }

    public int baseColumns() {
        return shifts[0].length;
    }

    public int checkBits() {
        return baseRows()
                * expansionFactor;
    }

    public int codewordBits() {
        return baseColumns()
                * expansionFactor;
    }

    public int informationBits() {
        return codewordBits()
                - checkBits();
    }

    public double nominalRate() {
        return informationBits()
                / (double) codewordBits();
    }
}
