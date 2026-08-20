package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;

import java.util.Arrays;

public final class RawIpv4HeaderValidator {
    private RawIpv4HeaderValidator() {
    }

    public static ValidationResult validate(
            byte[] raw
    ) {
        if (raw == null
                || raw.length < 20) {
            return ValidationResult.invalid(
                    Issue.TRUNCATED_BASE_HEADER,
                    0,
                    0,
                    false,
                    "",
                    "",
                    0,
                    "IPv4 base header is shorter than 20 bytes"
            );
        }

        int version =
                (raw[0] >>> 4)
                        & 0x0F;

        int ihlWords =
                raw[0]
                        & 0x0F;

        String source =
                address(
                        raw,
                        12
                );

        String destination =
                address(
                        raw,
                        16
                );

        int flagsAndOffset =
                read16(
                        raw,
                        6
                );

        int fragmentOffset =
                flagsAndOffset
                        & 0x1FFF;

        if (version != 4) {
            return ValidationResult.invalid(
                    Issue.NOT_IPV4,
                    0,
                    0,
                    false,
                    source,
                    destination,
                    fragmentOffset,
                    "Version field is not IPv4"
            );
        }

        if (ihlWords < 5) {
            return ValidationResult.invalid(
                    Issue.INVALID_IHL,
                    0,
                    0,
                    shouldSendParameterProblem(
                            source,
                            destination,
                            fragmentOffset
                    ),
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 IHL is smaller than 5"
            );
        }

        int headerBytes =
                ihlWords * 4;

        if (headerBytes > raw.length) {
            return ValidationResult.invalid(
                    Issue.TRUNCATED_HEADER,
                    0,
                    0,
                    false,
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 header length exceeds received bytes"
            );
        }

        int totalLength =
                read16(
                        raw,
                        2
                );

        if (totalLength < headerBytes) {
            return ValidationResult.invalid(
                    Issue.BAD_TOTAL_LENGTH,
                    2,
                    2,
                    shouldSendParameterProblem(
                            source,
                            destination,
                            fragmentOffset
                    ),
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 Total Length is smaller than header length"
            );
        }

        if (totalLength > raw.length) {
            return ValidationResult.invalid(
                    Issue.TRUNCATED_PACKET,
                    2,
                    2,
                    false,
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 Total Length exceeds received bytes"
            );
        }

        byte[] header =
                Arrays.copyOfRange(
                        raw,
                        0,
                        headerBytes
                );

        if (InternetChecksum.compute(
                header
        ) != 0) {
            return ValidationResult.invalid(
                    Issue.BAD_HEADER_CHECKSUM,
                    10,
                    0,
                    false,
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 header checksum is invalid"
            );
        }

        boolean reserved =
                (flagsAndOffset & 0x8000)
                        != 0;

        boolean df =
                (flagsAndOffset & 0x4000)
                        != 0;

        boolean mf =
                (flagsAndOffset & 0x2000)
                        != 0;

        if (reserved) {
            return ValidationResult.invalid(
                    Issue.RESERVED_FLAG_SET,
                    6,
                    0,
                    shouldSendParameterProblem(
                            source,
                            destination,
                            fragmentOffset
                    ),
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 reserved flag must be zero"
            );
        }

        if (df && mf) {
            return ValidationResult.invalid(
                    Issue.DF_MF_CONFLICT,
                    6,
                    0,
                    shouldSendParameterProblem(
                            source,
                            destination,
                            fragmentOffset
                    ),
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 DF and MF cannot both be set"
            );
        }

        if (df && fragmentOffset != 0) {
            return ValidationResult.invalid(
                    Issue.DF_FRAGMENT_OFFSET_CONFLICT,
                    6,
                    0,
                    false,
                    source,
                    destination,
                    fragmentOffset,
                    "IPv4 DF cannot accompany a non-zero fragment offset"
            );
        }

        ValidationResult optionIssue =
                validateOptions(
                        raw,
                        headerBytes,
                        source,
                        destination,
                        fragmentOffset
                );

        if (optionIssue != null) {
            return optionIssue;
        }

        return ValidationResult.valid(
                source,
                destination,
                fragmentOffset,
                headerBytes,
                totalLength
        );
    }

    private static ValidationResult validateOptions(
            byte[] raw,
            int headerBytes,
            String source,
            String destination,
            int fragmentOffset
    ) {
        int cursor =
                20;

        boolean eol =
                false;

        while (cursor < headerBytes) {
            int type =
                    raw[cursor]
                            & 0xFF;

            if (eol) {
                if (type != 0) {
                    return ValidationResult.invalid(
                            Issue.NON_ZERO_AFTER_EOL,
                            cursor,
                            0,
                            shouldSendParameterProblem(
                                    source,
                                    destination,
                                    fragmentOffset
                            ),
                            source,
                            destination,
                            fragmentOffset,
                            "Non-zero byte follows End of Option List"
                    );
                }

                cursor++;
                continue;
            }

            if (type == 0) {
                eol =
                        true;

                cursor++;
                continue;
            }

            if (type == 1) {
                cursor++;
                continue;
            }

            if (cursor + 1
                    >= headerBytes) {
                return ValidationResult.invalid(
                        Issue.TRUNCATED_OPTION_LENGTH,
                        cursor,
                        0,
                        shouldSendParameterProblem(
                                source,
                                destination,
                                fragmentOffset
                        ),
                        source,
                        destination,
                        fragmentOffset,
                        "IPv4 option length byte is missing"
                );
            }

            int length =
                    raw[cursor + 1]
                            & 0xFF;

            if (length < 2) {
                return ValidationResult.invalid(
                        Issue.INVALID_OPTION_LENGTH,
                        cursor + 1,
                        0,
                        shouldSendParameterProblem(
                                source,
                                destination,
                                fragmentOffset
                        ),
                        source,
                        destination,
                        fragmentOffset,
                        "IPv4 option length is smaller than 2"
                );
            }

            if (cursor + length
                    > headerBytes) {
                return ValidationResult.invalid(
                        Issue.OPTION_OVERRUN,
                        cursor + 1,
                        0,
                        shouldSendParameterProblem(
                                source,
                                destination,
                                fragmentOffset
                        ),
                        source,
                        destination,
                        fragmentOffset,
                        "IPv4 option overruns IHL"
                );
            }

            if (fragmentOffset != 0
                    && (type & 0x80) == 0) {
                return ValidationResult.invalid(
                        Issue.NON_COPIED_OPTION_ON_NON_INITIAL_FRAGMENT,
                        cursor,
                        0,
                        false,
                        source,
                        destination,
                        fragmentOffset,
                        "Copy=0 option appears on a non-initial fragment"
                );
            }

            cursor +=
                    length;
        }

        return null;
    }

    private static boolean shouldSendParameterProblem(
            String source,
            String destination,
            int fragmentOffset
    ) {
        if (fragmentOffset != 0) {
            return false;
        }

        if (!usableErrorSource(
                source
        )) {
            return false;
        }

        return !isMulticast(
                destination
        )
                && !"255.255.255.255".equals(
                destination
        );
    }

    private static boolean usableErrorSource(
            String ip
    ) {
        return ip != null
                && !ip.isBlank()
                && !"0.0.0.0".equals(ip)
                && !"255.255.255.255".equals(ip)
                && !isMulticast(ip);
    }

    private static boolean isMulticast(
            String ip
    ) {
        if (ip == null
                || ip.isBlank()) {
            return false;
        }

        int dot =
                ip.indexOf('.');

        if (dot <= 0) {
            return false;
        }

        try {
            int first =
                    Integer.parseInt(
                            ip.substring(
                                    0,
                                    dot
                            )
                    );

            return first >= 224
                    && first <= 239;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static int read16(
            byte[] data,
            int offset
    ) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    private static String address(
            byte[] data,
            int offset
    ) {
        return (data[offset] & 0xFF)
                + "."
                + (data[offset + 1] & 0xFF)
                + "."
                + (data[offset + 2] & 0xFF)
                + "."
                + (data[offset + 3] & 0xFF);
    }

    public enum Issue {
        NONE,
        TRUNCATED_BASE_HEADER,
        NOT_IPV4,
        INVALID_IHL,
        TRUNCATED_HEADER,
        BAD_TOTAL_LENGTH,
        TRUNCATED_PACKET,
        BAD_HEADER_CHECKSUM,
        RESERVED_FLAG_SET,
        DF_MF_CONFLICT,
        DF_FRAGMENT_OFFSET_CONFLICT,
        NON_ZERO_AFTER_EOL,
        TRUNCATED_OPTION_LENGTH,
        INVALID_OPTION_LENGTH,
        OPTION_OVERRUN,
        NON_COPIED_OPTION_ON_NON_INITIAL_FRAGMENT
    }

    public record ValidationResult(
            boolean valid,
            Issue issue,
            int pointer,
            int icmpCode,
            boolean sendParameterProblem,
            String sourceIp,
            String destinationIp,
            int fragmentOffset,
            int headerBytes,
            int totalLength,
            String detail
    ) {
        public ValidationResult {
            issue =
                    issue == null
                            ? Issue.NONE
                            : issue;

            sourceIp =
                    sourceIp == null
                            ? ""
                            : sourceIp;

            destinationIp =
                    destinationIp == null
                            ? ""
                            : destinationIp;

            detail =
                    detail == null
                            ? ""
                            : detail;
        }

        public static ValidationResult valid(
                String sourceIp,
                String destinationIp,
                int fragmentOffset,
                int headerBytes,
                int totalLength
        ) {
            return new ValidationResult(
                    true,
                    Issue.NONE,
                    0,
                    0,
                    false,
                    sourceIp,
                    destinationIp,
                    fragmentOffset,
                    headerBytes,
                    totalLength,
                    ""
            );
        }

        public static ValidationResult invalid(
                Issue issue,
                int pointer,
                int icmpCode,
                boolean sendParameterProblem,
                String sourceIp,
                String destinationIp,
                int fragmentOffset,
                String detail
        ) {
            return new ValidationResult(
                    false,
                    issue,
                    Math.max(
                            0,
                            Math.min(
                                    255,
                                    pointer
                            )
                    ),
                    icmpCode,
                    sendParameterProblem,
                    sourceIp,
                    destinationIp,
                    fragmentOffset,
                    0,
                    0,
                    detail
            );
        }
    }
}
