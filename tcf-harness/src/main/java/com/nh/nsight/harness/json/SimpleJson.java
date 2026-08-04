package com.nh.nsight.harness.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.toString();
    }

    public static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected trailing JSON content at index " + parser.index);
        }
        return value;
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String string) {
            out.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append("{\n");
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(out, depth + 1);
                out.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
                write(entry.getValue(), out, depth + 1);
                if (++index < map.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            indent(out, depth);
            out.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            List<Object> items = new ArrayList<>();
            iterable.forEach(items::add);
            out.append("[\n");
            for (int i = 0; i < items.size(); i++) {
                indent(out, depth + 1);
                write(items.get(i), out, depth + 1);
                if (i + 1 < items.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            indent(out, depth);
            out.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
        }
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(Math.max(0, depth)));
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private Object parseValue() {
            skipWhitespace();
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected JSON token at index " + index);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!atEnd()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return out.toString();
                }
                if (ch != '\\') {
                    out.append(ch);
                    continue;
                }
                if (atEnd()) {
                    throw new IllegalArgumentException("Invalid JSON escape");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("Invalid unicode escape");
                        }
                        out.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException("Unsupported JSON escape: " + escaped);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private Object parseNumber() {
            int start = index;
            if (peek('-')) index++;
            while (!atEnd() && Character.isDigit(text.charAt(index))) index++;
            boolean decimal = false;
            if (!atEnd() && text.charAt(index) == '.') {
                decimal = true;
                index++;
                while (!atEnd() && Character.isDigit(text.charAt(index))) index++;
            }
            if (!atEnd() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (!atEnd() && (text.charAt(index) == '+' || text.charAt(index) == '-')) index++;
                while (!atEnd() && Character.isDigit(text.charAt(index))) index++;
            }
            String token = text.substring(start, index);
            return decimal ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Invalid JSON literal at index " + index);
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (atEnd() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
            }
            index++;
        }

        private boolean peek(char expected) {
            return !atEnd() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(text.charAt(index))) index++;
        }

        private boolean atEnd() {
            return index >= text.length();
        }
    }
}
