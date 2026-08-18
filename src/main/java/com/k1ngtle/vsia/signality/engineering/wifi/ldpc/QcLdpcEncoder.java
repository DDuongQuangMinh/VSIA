package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.BitSet;

public final class QcLdpcEncoder {
    private final QcLdpcExpandedMatrix matrix;
    private final int k;
    private final int m;
    private final BitSet[] parityMatrix;

    public QcLdpcEncoder(
            QcLdpcBaseMatrix base
    ) {
        this.matrix =
                new QcLdpcExpandedMatrix(
                        base
                );

        this.k =
                base.informationBits();

        this.m =
                base.checkBits();

        this.parityMatrix =
                buildParityMatrix();
    }

    public int informationBits() {
        return k;
    }

    public int codewordBits() {
        return matrix.columns();
    }

    public int[] encode(
            int[] information
    ) {
        if (information.length != k) {
            throw new IllegalArgumentException(
                    "Expected "
                            + k
                            + " information bits"
            );
        }

        int[] rhs =
                new int[
                        m
                        ];

        for (int row = 0;
             row < m;
             row++) {
            BitSet check =
                    matrix.row(
                            row
                    );

            int parity =
                    0;

            for (int bit = check.nextSetBit(
                    0
            );
                 bit >= 0
                         && bit < k;
                 bit = check.nextSetBit(
                         bit + 1
                 )) {
                parity ^=
                        information[bit]
                                & 1;
            }

            rhs[row] =
                    parity;
        }

        int[] parity =
                Gf2LinearSolver.solve(
                        parityMatrix,
                        m,
                        rhs
                );

        int[] codeword =
                new int[
                        k + m
                        ];

        System.arraycopy(
                information,
                0,
                codeword,
                0,
                k
        );

        System.arraycopy(
                parity,
                0,
                codeword,
                k,
                m
        );

        if (syndromeWeight(
                codeword
        ) != 0) {
            throw new IllegalStateException(
                    "Encoded LDPC codeword has non-zero syndrome"
            );
        }

        return codeword;
    }

    public int syndromeWeight(
            int[] codeword
    ) {
        if (codeword.length
                != matrix.columns()) {
            throw new IllegalArgumentException(
                    "Codeword length mismatch"
            );
        }

        int failures =
                0;

        for (int row = 0;
             row < matrix.rows();
             row++) {
            BitSet check =
                    matrix.row(
                            row
                    );

            int parity =
                    0;

            for (int bit = check.nextSetBit(
                    0
            );
                 bit >= 0;
                 bit = check.nextSetBit(
                         bit + 1
                 )) {
                parity ^=
                        codeword[bit]
                                & 1;
            }

            failures +=
                    parity;
        }

        return failures;
    }

    public QcLdpcExpandedMatrix matrix() {
        return matrix;
    }

    private BitSet[] buildParityMatrix() {
        BitSet[] result =
                new BitSet[
                        m
                        ];

        for (int row = 0;
             row < m;
             row++) {
            BitSet source =
                    matrix.row(
                            row
                    );

            BitSet parity =
                    new BitSet(
                            m
                    );

            for (int bit = source.nextSetBit(
                    k
            );
                 bit >= 0;
                 bit = source.nextSetBit(
                         bit + 1
                 )) {
                parity.set(
                        bit - k
                );
            }

            result[row] =
                    parity;
        }

        return result;
    }
}
