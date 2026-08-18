package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class LayeredMinSumLdpcDecoder {
    private final QcLdpcExpandedMatrix matrix;
    private final int informationBits;
    private final int[][] checkVariables;

    public LayeredMinSumLdpcDecoder(
            QcLdpcBaseMatrix base
    ) {
        this.matrix =
                new QcLdpcExpandedMatrix(
                        base
                );

        this.informationBits =
                base.informationBits();

        this.checkVariables =
                buildCheckVariables(
                        matrix
                );
    }

    public LdpcDecodeResult decode(
            double[] channelLlrs,
            int maxIterations,
            double normalization
    ) {
        if (channelLlrs.length
                != matrix.columns()) {
            throw new IllegalArgumentException(
                    "LLR length mismatch"
            );
        }

        if (maxIterations < 1) {
            throw new IllegalArgumentException(
                    "maxIterations"
            );
        }

        double alpha =
                Math.max(
                        0.1,
                        Math.min(
                                1.0,
                                normalization
                        )
                );

        double[] beliefs =
                channelLlrs.clone();

        double[][] checkToVariable =
                new double[
                        checkVariables.length
                        ][];

        for (int row = 0;
             row < checkVariables.length;
             row++) {
            checkToVariable[row] =
                    new double[
                            checkVariables[row].length
                            ];
        }

        int[] hard =
                hardDecision(
                        beliefs
                );

        int syndrome =
                syndromeWeight(
                        hard
                );

        if (syndrome == 0) {
            return result(
                    hard,
                    0,
                    true,
                    syndrome
            );
        }

        int iteration =
                0;

        for (; iteration < maxIterations;
             iteration++) {
            for (int row = 0;
                 row < checkVariables.length;
                 row++) {
                int[] variables =
                        checkVariables[row];

                double[] oldMessages =
                        checkToVariable[row];

                double min1 =
                        Double.POSITIVE_INFINITY;

                double min2 =
                        Double.POSITIVE_INFINITY;

                int minIndex =
                        -1;

                int signProduct =
                        1;

                double[] extrinsic =
                        new double[
                                variables.length
                                ];

                for (int edge = 0;
                     edge < variables.length;
                     edge++) {
                    int variable =
                            variables[edge];

                    double value =
                            beliefs[variable]
                                    - oldMessages[edge];

                    extrinsic[edge] =
                            value;

                    if (value < 0.0) {
                        signProduct =
                                -signProduct;
                    }

                    double magnitude =
                            Math.abs(
                                    value
                            );

                    if (magnitude < min1) {
                        min2 =
                                min1;

                        min1 =
                                magnitude;

                        minIndex =
                                edge;
                    } else if (magnitude < min2) {
                        min2 =
                                magnitude;
                    }
                }

                for (int edge = 0;
                     edge < variables.length;
                     edge++) {
                    int variable =
                            variables[edge];

                    int sign =
                            extrinsic[edge] < 0.0
                                    ? -1
                                    : 1;

                    double magnitude =
                            edge == minIndex
                                    ? min2
                                    : min1;

                    if (!Double.isFinite(
                            magnitude
                    )) {
                        magnitude =
                                0.0;
                    }

                    double newMessage =
                            alpha
                                    * signProduct
                                    * sign
                                    * magnitude;

                    oldMessages[edge] =
                            newMessage;

                    beliefs[variable] =
                            extrinsic[edge]
                                    + newMessage;
                }
            }

            hard =
                    hardDecision(
                            beliefs
                    );

            syndrome =
                    syndromeWeight(
                            hard
                    );

            if (syndrome == 0) {
                return result(
                        hard,
                        iteration + 1,
                        true,
                        0
                );
            }
        }

        return result(
                hard,
                iteration,
                false,
                syndrome
        );
    }

    private LdpcDecodeResult result(
            int[] codeword,
            int iterations,
            boolean converged,
            int syndromeWeight
    ) {
        int[] information =
                new int[
                        informationBits
                        ];

        System.arraycopy(
                codeword,
                0,
                information,
                0,
                informationBits
        );

        return new LdpcDecodeResult(
                codeword,
                information,
                iterations,
                converged,
                syndromeWeight
        );
    }

    private int syndromeWeight(
            int[] codeword
    ) {
        int failures =
                0;

        for (int[] variables : checkVariables) {
            int parity =
                    0;

            for (int variable : variables) {
                parity ^=
                        codeword[variable]
                                & 1;
            }

            failures +=
                    parity;
        }

        return failures;
    }

    private static int[] hardDecision(
            double[] llrs
    ) {
        int[] bits =
                new int[
                        llrs.length
                        ];

        for (int i = 0;
             i < bits.length;
             i++) {
            bits[i] =
                    llrs[i] >= 0.0
                            ? 1
                            : 0;
        }

        return bits;
    }

    private static int[][] buildCheckVariables(
            QcLdpcExpandedMatrix matrix
    ) {
        int[][] result =
                new int[
                        matrix.rows()
                        ][];

        for (int row = 0;
             row < matrix.rows();
             row++) {
            BitSet bits =
                    matrix.row(
                            row
                    );

            List<Integer> variables =
                    new ArrayList<>();

            for (int bit = bits.nextSetBit(
                    0
            );
                 bit >= 0;
                 bit = bits.nextSetBit(
                         bit + 1
                 )) {
                variables.add(
                        bit
                );
            }

            result[row] =
                    new int[
                            variables.size()
                            ];

            for (int i = 0;
                 i < variables.size();
                 i++) {
                result[row][i] =
                        variables.get(
                                i
                        );
            }
        }

        return result;
    }
}
