package com.k1ngtle.vsia.signality.internet.web;

import java.util.Map;

public final class W129Expression {
    private W129Expression() {
    }

    public static Object evaluate(
            String expression,
            Map<String, Object> variables
    ) {
        Parser parser = new Parser(
                expression == null ? "" : expression,
                variables == null ? Map.of() : variables
        );

        Object value = parser.expression();
        parser.skipWhitespace();

        if (!parser.atEnd()) {
            throw new IllegalArgumentException(
                    "Unexpected token near: "
                            + parser.remaining()
            );
        }

        return value;
    }

    public static String printable(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Double d
                && Math.rint(d) == d) {
            return Long.toString(d.longValue());
        }

        return String.valueOf(value);
    }

    private static final class Parser {
        private final String input;
        private final Map<String, Object> variables;
        private int index;

        private Parser(
                String input,
                Map<String, Object> variables
        ) {
            this.input = input;
            this.variables = variables;
        }

        private Object expression() {
            Object value = term();

            while (true) {
                skipWhitespace();

                if (match('+')) {
                    Object right = term();

                    if (value instanceof String
                            || right instanceof String) {
                        value = printable(value)
                                + printable(right);
                    } else {
                        value = number(value)
                                + number(right);
                    }
                } else if (match('-')) {
                    value = number(value)
                            - number(term());
                } else {
                    return value;
                }
            }
        }

        private Object term() {
            Object value = factor();

            while (true) {
                skipWhitespace();

                if (match('*')) {
                    value = number(value)
                            * number(factor());
                } else if (match('/')) {
                    double divisor =
                            number(factor());

                    if (divisor == 0.0D) {
                        throw new IllegalArgumentException(
                                "Division by zero"
                        );
                    }

                    value = number(value)
                            / divisor;
                } else if (match('%')) {
                    double divisor =
                            number(factor());

                    if (divisor == 0.0D) {
                        throw new IllegalArgumentException(
                                "Modulo by zero"
                        );
                    }

                    value = number(value)
                            % divisor;
                } else {
                    return value;
                }
            }
        }

        private Object factor() {
            skipWhitespace();

            if (match('+')) {
                return number(factor());
            }

            if (match('-')) {
                return -number(factor());
            }

            if (match('(')) {
                Object value = expression();
                skipWhitespace();

                if (!match(')')) {
                    throw new IllegalArgumentException(
                            "Missing ')'"
                    );
                }

                return value;
            }

            if (peek('"') || peek('\'')) {
                return string();
            }

            if (index < input.length()
                    && (
                    Character.isDigit(input.charAt(index))
                            || input.charAt(index) == '.'
            )) {
                return numberLiteral();
            }

            String identifier = identifier();

            if (!identifier.isEmpty()) {
                if ("true".equals(identifier)) {
                    return 1.0D;
                }

                if ("false".equals(identifier)) {
                    return 0.0D;
                }

                if ("null".equals(identifier)
                        || "None".equals(identifier)) {
                    return null;
                }

                if (variables.containsKey(identifier)) {
                    return variables.get(identifier);
                }

                throw new IllegalArgumentException(
                        "Unknown variable: "
                                + identifier
                );
            }

            throw new IllegalArgumentException(
                    "Expected expression"
            );
        }

        private String string() {
            char quote = input.charAt(index++);
            StringBuilder out = new StringBuilder();
            boolean escape = false;

            while (index < input.length()) {
                char c = input.charAt(index++);

                if (escape) {
                    switch (c) {
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case '\\' -> out.append('\\');
                        case '\'' -> out.append('\'');
                        case '"' -> out.append('"');
                        default -> out.append(c);
                    }

                    escape = false;
                    continue;
                }

                if (c == '\\') {
                    escape = true;
                    continue;
                }

                if (c == quote) {
                    return out.toString();
                }

                out.append(c);
            }

            throw new IllegalArgumentException(
                    "Unterminated string literal"
            );
        }

        private Double numberLiteral() {
            int start = index;

            while (index < input.length()) {
                char c = input.charAt(index);

                if (!Character.isDigit(c)
                        && c != '.') {
                    break;
                }

                index++;
            }

            return Double.parseDouble(
                    input.substring(start, index)
            );
        }

        private String identifier() {
            skipWhitespace();
            int start = index;

            while (index < input.length()) {
                char c = input.charAt(index);

                if (!Character.isLetterOrDigit(c)
                        && c != '_'
                        && c != '$') {
                    break;
                }

                index++;
            }

            return input.substring(start, index);
        }

        private static double number(Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }

            if (value instanceof String string) {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException ignored) {
                }
            }

            throw new IllegalArgumentException(
                    "Expected numeric value"
            );
        }

        private boolean match(char expected) {
            if (index < input.length()
                    && input.charAt(index) == expected) {
                index++;
                return true;
            }

            return false;
        }

        private boolean peek(char expected) {
            return index < input.length()
                    && input.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < input.length()
                    && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean atEnd() {
            return index >= input.length();
        }

        private String remaining() {
            return input.substring(
                    Math.min(index, input.length())
            );
        }
    }
}
