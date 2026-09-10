package com.k1ngtle.vsia.signality.internet.web;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class W129AssemblyVm {
    public static final int REGISTER_COUNT = 16;
    public static final int MAX_INSTRUCTIONS = 10_000;
    public static final int MAX_STACK = 4_096;

    private W129AssemblyVm() {
    }

    public static W129RunResult run(String source) {
        ParseResult parsed = parse(source);

        if (!parsed.diagnostics().isEmpty()) {
            return W129RunResult.fail(
                    "Assembly failed.",
                    parsed.diagnostics()
            );
        }

        long[] registers = new long[REGISTER_COUNT];
        Deque<Long> stack = new ArrayDeque<>();
        StringBuilder output = new StringBuilder();

        int pc = 0;
        long cycles = 0L;
        int cmp = 0;

        while (pc >= 0 && pc < parsed.instructions().size()) {
            if (cycles >= MAX_INSTRUCTIONS) {
                return new W129RunResult(
                        false,
                        124,
                        output + "\nExecution stopped: instruction budget exceeded.",
                        cycles,
                        stack.size() * Long.BYTES,
                        List.of(
                                new W129Diagnostic(
                                        W129Diagnostic.Severity.ERROR,
                                        parsed.instructions().get(pc).line(),
                                        1,
                                        "Instruction budget exceeded."
                                )
                        )
                );
            }

            Instruction instruction =
                    parsed.instructions().get(pc);

            cycles++;

            try {
                String op = instruction.op();
                List<String> args = instruction.args();

                switch (op) {
                    case "nop" -> pc++;

                    case "halt", "ret" -> {
                        return new W129RunResult(
                                true,
                                0,
                                output.toString().stripTrailing(),
                                cycles,
                                stack.size() * Long.BYTES,
                                List.of()
                        );
                    }

                    case "mov" -> {
                        requireArgs(instruction, 2);
                        int target = registerIndex(args.get(0));
                        registers[target] =
                                operand(args.get(1), registers);
                        pc++;
                    }

                    case "add" -> {
                        requireArgs(instruction, 2);
                        int target = registerIndex(args.get(0));
                        registers[target] +=
                                operand(args.get(1), registers);
                        pc++;
                    }

                    case "sub" -> {
                        requireArgs(instruction, 2);
                        int target = registerIndex(args.get(0));
                        registers[target] -=
                                operand(args.get(1), registers);
                        pc++;
                    }

                    case "mul" -> {
                        requireArgs(instruction, 2);
                        int target = registerIndex(args.get(0));
                        registers[target] *=
                                operand(args.get(1), registers);
                        pc++;
                    }

                    case "div" -> {
                        requireArgs(instruction, 2);
                        int target = registerIndex(args.get(0));
                        long divisor =
                                operand(args.get(1), registers);

                        if (divisor == 0L) {
                            throw new IllegalArgumentException(
                                    "Division by zero"
                            );
                        }

                        registers[target] /= divisor;
                        pc++;
                    }

                    case "mod" -> {
                        requireArgs(instruction, 2);
                        int target = registerIndex(args.get(0));
                        long divisor =
                                operand(args.get(1), registers);

                        if (divisor == 0L) {
                            throw new IllegalArgumentException(
                                    "Modulo by zero"
                            );
                        }

                        registers[target] %= divisor;
                        pc++;
                    }

                    case "inc" -> {
                        requireArgs(instruction, 1);
                        registers[
                                registerIndex(args.get(0))
                        ]++;
                        pc++;
                    }

                    case "dec" -> {
                        requireArgs(instruction, 1);
                        registers[
                                registerIndex(args.get(0))
                        ]--;
                        pc++;
                    }

                    case "cmp" -> {
                        requireArgs(instruction, 2);
                        long left =
                                operand(args.get(0), registers);
                        long right =
                                operand(args.get(1), registers);
                        cmp = Long.compare(left, right);
                        pc++;
                    }

                    case "jmp" -> {
                        requireArgs(instruction, 1);
                        pc = label(
                                args.get(0),
                                parsed.labels()
                        );
                    }

                    case "je", "jz" -> {
                        requireArgs(instruction, 1);
                        pc = cmp == 0
                                ? label(
                                args.get(0),
                                parsed.labels()
                        )
                                : pc + 1;
                    }

                    case "jne", "jnz" -> {
                        requireArgs(instruction, 1);
                        pc = cmp != 0
                                ? label(
                                args.get(0),
                                parsed.labels()
                        )
                                : pc + 1;
                    }

                    case "jg" -> {
                        requireArgs(instruction, 1);
                        pc = cmp > 0
                                ? label(
                                args.get(0),
                                parsed.labels()
                        )
                                : pc + 1;
                    }

                    case "jl" -> {
                        requireArgs(instruction, 1);
                        pc = cmp < 0
                                ? label(
                                args.get(0),
                                parsed.labels()
                        )
                                : pc + 1;
                    }

                    case "jge" -> {
                        requireArgs(instruction, 1);
                        pc = cmp >= 0
                                ? label(
                                args.get(0),
                                parsed.labels()
                        )
                                : pc + 1;
                    }

                    case "jle" -> {
                        requireArgs(instruction, 1);
                        pc = cmp <= 0
                                ? label(
                                args.get(0),
                                parsed.labels()
                        )
                                : pc + 1;
                    }

                    case "push" -> {
                        requireArgs(instruction, 1);

                        if (stack.size() >= MAX_STACK) {
                            throw new IllegalArgumentException(
                                    "Stack limit exceeded"
                            );
                        }

                        stack.push(
                                operand(
                                        args.get(0),
                                        registers
                                )
                        );
                        pc++;
                    }

                    case "pop" -> {
                        requireArgs(instruction, 1);

                        if (stack.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Stack underflow"
                            );
                        }

                        registers[
                                registerIndex(args.get(0))
                        ] = stack.pop();
                        pc++;
                    }

                    case "print" -> {
                        requireArgs(instruction, 1);

                        if (!output.isEmpty()) {
                            output.append('\n');
                        }

                        String raw =
                                args.get(0).trim();

                        if (
                                (
                                        raw.startsWith("\"")
                                                && raw.endsWith("\"")
                                )
                                        || (
                                        raw.startsWith("'")
                                                && raw.endsWith("'")
                                )
                        ) {
                            output.append(
                                    decodeString(raw)
                            );
                        } else {
                            output.append(
                                    operand(raw, registers)
                            );
                        }

                        pc++;
                    }

                    default -> throw new IllegalArgumentException(
                            "Unknown opcode: "
                                    + op
                    );
                }
            } catch (IllegalArgumentException exception) {
                return new W129RunResult(
                        false,
                        1,
                        output.toString().stripTrailing(),
                        cycles,
                        stack.size() * Long.BYTES,
                        List.of(
                                new W129Diagnostic(
                                        W129Diagnostic.Severity.ERROR,
                                        instruction.line(),
                                        1,
                                        exception.getMessage()
                                )
                        )
                );
            }
        }

        return new W129RunResult(
                true,
                0,
                output.toString().stripTrailing(),
                cycles,
                stack.size() * Long.BYTES,
                List.of()
        );
    }

    public static List<W129Diagnostic> validate(
            String source
    ) {
        return parse(source).diagnostics();
    }

    private static ParseResult parse(
            String source
    ) {
        String normalized =
                source == null
                        ? ""
                        : source
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');

        String[] lines = normalized.split(
                "\n",
                -1
        );

        List<Instruction> instructions =
                new ArrayList<>();

        Map<String, Integer> labels =
                new HashMap<>();

        List<W129Diagnostic> diagnostics =
                new ArrayList<>();

        for (int lineIndex = 0;
             lineIndex < lines.length;
             lineIndex++) {
            String raw =
                    stripComment(lines[lineIndex])
                            .trim();

            int lineNumber = lineIndex + 1;

            if (raw.isEmpty()) {
                continue;
            }

            String lower =
                    raw.toLowerCase(Locale.ROOT);

            if (lower.startsWith("section ")
                    || lower.startsWith("global ")
                    || lower.startsWith("bits ")) {
                continue;
            }

            if (raw.endsWith(":")) {
                String label =
                        raw.substring(
                                0,
                                raw.length() - 1
                        ).trim();

                if (!validLabel(label)) {
                    diagnostics.add(
                            new W129Diagnostic(
                                    W129Diagnostic.Severity.ERROR,
                                    lineNumber,
                                    1,
                                    "Invalid label: "
                                            + label
                            )
                    );
                    continue;
                }

                if (labels.putIfAbsent(
                        label,
                        instructions.size()
                ) != null) {
                    diagnostics.add(
                            new W129Diagnostic(
                                    W129Diagnostic.Severity.ERROR,
                                    lineNumber,
                                    1,
                                    "Duplicate label: "
                                            + label
                            )
                    );
                }

                continue;
            }

            int firstSpace =
                    firstWhitespace(raw);

            String op =
                    (
                            firstSpace < 0
                                    ? raw
                                    : raw.substring(
                                    0,
                                    firstSpace
                            )
                    ).toLowerCase(Locale.ROOT);

            String argumentText =
                    firstSpace < 0
                            ? ""
                            : raw.substring(
                            firstSpace
                    ).trim();

            List<String> args =
                    splitArguments(argumentText);

            if (!knownOpcode(op)) {
                diagnostics.add(
                        new W129Diagnostic(
                                W129Diagnostic.Severity.ERROR,
                                lineNumber,
                                1,
                                "Unknown opcode: "
                                        + op
                        )
                );
                continue;
            }

            instructions.add(
                    new Instruction(
                            lineNumber,
                            op,
                            args
                    )
            );
        }

        for (Instruction instruction : instructions) {
            if (isJump(instruction.op())
                    && instruction.args().size() == 1) {
                String target =
                        instruction.args().get(0);

                if (!labels.containsKey(target)) {
                    diagnostics.add(
                            new W129Diagnostic(
                                    W129Diagnostic.Severity.ERROR,
                                    instruction.line(),
                                    1,
                                    "Unknown label: "
                                            + target
                            )
                    );
                }
            }
        }

        return new ParseResult(
                List.copyOf(instructions),
                Map.copyOf(labels),
                List.copyOf(diagnostics)
        );
    }

    private static boolean knownOpcode(String op) {
        return switch (op) {
            case "nop",
                 "halt",
                 "ret",
                 "mov",
                 "add",
                 "sub",
                 "mul",
                 "div",
                 "mod",
                 "inc",
                 "dec",
                 "cmp",
                 "jmp",
                 "je",
                 "jz",
                 "jne",
                 "jnz",
                 "jg",
                 "jl",
                 "jge",
                 "jle",
                 "push",
                 "pop",
                 "print" -> true;
            default -> false;
        };
    }

    private static boolean isJump(String op) {
        return switch (op) {
            case "jmp",
                 "je",
                 "jz",
                 "jne",
                 "jnz",
                 "jg",
                 "jl",
                 "jge",
                 "jle" -> true;
            default -> false;
        };
    }

    private static String stripComment(
            String line
    ) {
        boolean single = false;
        boolean dbl = false;

        for (int i = 0;
             i < line.length();
             i++) {
            char c = line.charAt(i);

            if (c == '\''
                    && !dbl) {
                single = !single;
            } else if (c == '"'
                    && !single) {
                dbl = !dbl;
            } else if (c == ';'
                    && !single
                    && !dbl) {
                return line.substring(0, i);
            }
        }

        return line;
    }

    private static List<String> splitArguments(
            String text
    ) {
        if (text == null
                || text.isBlank()) {
            return List.of();
        }

        List<String> args =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean single = false;
        boolean dbl = false;

        for (int i = 0;
             i < text.length();
             i++) {
            char c = text.charAt(i);

            if (c == '\''
                    && !dbl) {
                single = !single;
                current.append(c);
                continue;
            }

            if (c == '"'
                    && !single) {
                dbl = !dbl;
                current.append(c);
                continue;
            }

            if (c == ','
                    && !single
                    && !dbl) {
                args.add(
                        current.toString().trim()
                );
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String tail =
                current.toString().trim();

        if (!tail.isEmpty()) {
            args.add(tail);
        }

        return List.copyOf(args);
    }

    private static int registerIndex(
            String token
    ) {
        String normalized =
                token.trim()
                        .toLowerCase(Locale.ROOT);

        if (!normalized.matches("r(?:1[0-5]|[0-9])")) {
            throw new IllegalArgumentException(
                    "Expected register r0-r15: "
                            + token
            );
        }

        return Integer.parseInt(
                normalized.substring(1)
        );
    }

    private static long operand(
            String token,
            long[] registers
    ) {
        String normalized =
                token.trim();

        if (normalized
                .toLowerCase(Locale.ROOT)
                .matches("r(?:1[0-5]|[0-9])")) {
            return registers[
                    registerIndex(normalized)
            ];
        }

        try {
            if (normalized.startsWith("0x")
                    || normalized.startsWith("0X")) {
                return Long.parseUnsignedLong(
                        normalized.substring(2),
                        16
                );
            }

            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid operand: "
                            + token
            );
        }
    }

    private static int label(
            String name,
            Map<String, Integer> labels
    ) {
        Integer target =
                labels.get(name);

        if (target == null) {
            throw new IllegalArgumentException(
                    "Unknown label: "
                            + name
            );
        }

        return target;
    }

    private static void requireArgs(
            Instruction instruction,
            int count
    ) {
        if (instruction.args().size() != count) {
            throw new IllegalArgumentException(
                    instruction.op()
                            + " expects "
                            + count
                            + " argument(s)"
            );
        }
    }

    private static int firstWhitespace(
            String value
    ) {
        for (int i = 0;
             i < value.length();
             i++) {
            if (Character.isWhitespace(
                    value.charAt(i)
            )) {
                return i;
            }
        }

        return -1;
    }

    private static boolean validLabel(
            String value
    ) {
        return value.matches(
                "[A-Za-z_.$][A-Za-z0-9_.$]*"
        );
    }

    private static String decodeString(
            String token
    ) {
        String body =
                token.substring(
                        1,
                        token.length() - 1
                );

        return body
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\");
    }

    private record Instruction(
            int line,
            String op,
            List<String> args
    ) {
    }

    private record ParseResult(
            List<Instruction> instructions,
            Map<String, Integer> labels,
            List<W129Diagnostic> diagnostics
    ) {
    }
}
