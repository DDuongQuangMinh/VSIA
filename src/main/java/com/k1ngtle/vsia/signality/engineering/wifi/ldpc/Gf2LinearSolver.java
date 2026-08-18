package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.BitSet;

public final class Gf2LinearSolver {
    private Gf2LinearSolver() {
    }

    public static int[] solve(
            BitSet[] coefficientRows,
            int variables,
            int[] rhs
    ) {
        if (coefficientRows.length != variables
                || rhs.length != variables) {
            throw new IllegalArgumentException(
                    "Square GF(2) system required"
            );
        }

        BitSet[] augmented =
                new BitSet[
                        variables
                        ];

        for (int row = 0;
             row < variables;
             row++) {
            augmented[row] =
                    (BitSet) coefficientRows[row]
                            .clone();

            if ((rhs[row] & 1) != 0) {
                augmented[row].set(
                        variables
                );
            }
        }

        for (int column = 0;
             column < variables;
             column++) {
            int pivot =
                    findPivot(
                            augmented,
                            column,
                            column
                    );

            if (pivot < 0) {
                throw new IllegalArgumentException(
                        "GF(2) matrix is singular at column "
                                + column
                );
            }

            if (pivot != column) {
                BitSet swap =
                        augmented[column];

                augmented[column] =
                        augmented[pivot];

                augmented[pivot] =
                        swap;
            }

            for (int row = 0;
                 row < variables;
                 row++) {
                if (row == column
                        || !augmented[row].get(
                        column
                )) {
                    continue;
                }

                augmented[row].xor(
                        augmented[column]
                );
            }
        }

        int[] solution =
                new int[
                        variables
                        ];

        for (int row = 0;
             row < variables;
             row++) {
            solution[row] =
                    augmented[row].get(
                            variables
                    )
                            ? 1
                            : 0;
        }

        return solution;
    }

    private static int findPivot(
            BitSet[] rows,
            int column,
            int startRow
    ) {
        for (int row = startRow;
             row < rows.length;
             row++) {
            if (rows[row].get(
                    column
            )) {
                return row;
            }
        }

        return -1;
    }
}
