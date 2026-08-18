package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.BitSet;

public final class QcLdpcExpandedMatrix {
    private final QcLdpcBaseMatrix base;
    private final BitSet[] rows;

    public QcLdpcExpandedMatrix(
            QcLdpcBaseMatrix base
    ) {
        this.base =
                base;

        this.rows =
                expand(
                        base
                );
    }

    public QcLdpcBaseMatrix base() {
        return base;
    }

    public int rows() {
        return rows.length;
    }

    public int columns() {
        return base.codewordBits();
    }

    public BitSet row(
            int index
    ) {
        return (BitSet) rows[index]
                .clone();
    }

    public BitSet[] rowsCopy() {
        BitSet[] copy =
                new BitSet[
                        rows.length
                        ];

        for (int i = 0;
             i < rows.length;
             i++) {
            copy[i] =
                    row(
                            i
                    );
        }

        return copy;
    }

    private static BitSet[] expand(
            QcLdpcBaseMatrix base
    ) {
        int z =
                base.expansionFactor();

        int[][] shifts =
                base.shifts();

        BitSet[] result =
                new BitSet[
                        base.baseRows()
                                * z
                        ];

        for (int baseRow = 0;
             baseRow < base.baseRows();
             baseRow++) {
            for (int localRow = 0;
                 localRow < z;
                 localRow++) {
                BitSet row =
                        new BitSet(
                                base.codewordBits()
                        );

                for (int baseColumn = 0;
                     baseColumn < base.baseColumns();
                     baseColumn++) {
                    int shift =
                            shifts[baseRow][baseColumn];

                    if (shift
                            == QcLdpcBaseMatrix.ZERO_BLOCK) {
                        continue;
                    }

                    int localColumn =
                            Math.floorMod(
                                    localRow + shift,
                                    z
                            );

                    int column =
                            baseColumn * z
                                    + localColumn;

                    row.set(
                            column
                    );
                }

                result[
                        baseRow * z
                                + localRow
                        ] =
                        row;
            }
        }

        return result;
    }
}
