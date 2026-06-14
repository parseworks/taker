package io.github.parseworks.taker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;

/**
 * Structured information about a parser failure.
 * <p>
 * Diagnostics are a lazy view over an existing {@link Failure}: successful
 * parses do not create diagnostics, and expensive rendering work is deferred
 * until {@link #render()} or {@link #render(CharSequence)} is called.
 *
 * @param type result type that produced the diagnostic
 * @param offset zero-based input offset where parsing failed
 * @param line one-based line number, or {@code -1} when unavailable
 * @param column one-based column number, or {@code -1} when unavailable
 * @param found text describing the input found at the failure offset
 * @param expected distinct expectations reported at the farthest failure point
 * @param contexts grammar context labels from {@code label(...)}, outermost first
 * @param causes nested failure causes, outermost first
 */
public record ParseDiagnostics(
    ResultType type,
    int offset,
    int line,
    int column,
    String found,
    List<String> expected,
    List<String> contexts,
    List<Cause> causes
) {

    public ParseDiagnostics {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(found, "found");
        expected = List.copyOf(expected);
        contexts = List.copyOf(contexts);
        causes = List.copyOf(causes);
    }

    /**
     * Creates diagnostics from a failed result.
     *
     * @param failure failure to inspect
     * @return structured diagnostics for the failure
     */
    public static ParseDiagnostics from(Failure<?> failure) {
        Objects.requireNonNull(failure, "failure");
        List<? extends Failure<?>> failures = rootFailures(failure);
        Failure<?> first = failures.getFirst();
        Input input = first.input();
        if (input == null) {
            input = failure.input();
        }

        SequencedSet<String> expected = new LinkedHashSet<>();
        for (Failure<?> item : failures) {
            collectExpected(item, expected);
        }

        return new ParseDiagnostics(
            failure.type(),
            input == null ? -1 : input.position(),
            input instanceof TextInput text ? text.line() : -1,
            input instanceof TextInput text ? text.column() : -1,
            found(input),
            new ArrayList<>(expected),
            collectContexts(failure),
            collectCauses(failure)
        );
    }

    /**
     * Renders these diagnostics using the source text available from the failed
     * input cursor when possible.
     *
     * @return formatted diagnostic message
     */
    public String render() {
        return render(null);
    }

    /**
     * Renders these diagnostics using {@code source} for a caret snippet.
     *
     * @param source original input text, or {@code null} to omit the snippet
     * @return formatted diagnostic message
     */
    public String render(CharSequence source) {
        StringBuilder builder = new StringBuilder();
        builder.append(type == ResultType.PARTIAL ? "Partial match failed" : "Parse failed");
        if (line > 0 && column > 0) {
            builder.append(" at line ").append(line).append(", column ").append(column);
        } else if (offset >= 0) {
            builder.append(" at offset ").append(offset);
        }
        builder.append('\n');

        String snippet = snippet(source);
        if (!snippet.isEmpty()) {
            builder.append('\n').append(snippet).append('\n');
        }

        if (!expected.isEmpty()) {
            builder.append('\n').append("expected ").append(String.join(" or ", expected));
        } else {
            builder.append('\n').append("expected correct input");
        }
        builder.append(", found ").append(found).append('\n');

        if (!contexts.isEmpty()) {
            builder.append('\n');
            for (String context : contexts) {
                builder.append("while parsing ").append(context).append('\n');
            }
        }

        if (!causes.isEmpty()) {
            builder.append('\n').append("causes:").append('\n');
            for (Cause cause : causes) {
                builder.append("  - ");
                if (cause.context()) {
                    builder.append("while parsing ");
                    builder.append(cause.expected() == null || cause.expected().isBlank() ? "unknown rule" : cause.expected());
                } else {
                    builder.append("expected ");
                    if (cause.expected() == null || cause.expected().isBlank()) {
                        builder.append("correct input");
                    } else {
                        builder.append(cause.expected());
                    }
                    builder.append(", found ").append(cause.found());
                }
                if (cause.line() > 0 && cause.column() > 0) {
                    builder.append(" at line ").append(cause.line())
                        .append(", column ").append(cause.column());
                } else if (cause.offset() >= 0) {
                    builder.append(" at offset ").append(cause.offset());
                }
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    private String snippet(CharSequence source) {
        if (source == null || offset < 0 || offset > source.length()) {
            return "";
        }

        int lineStart = offset;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n' && source.charAt(lineStart - 1) != '\r') {
            lineStart--;
        }

        int lineEnd = offset;
        while (lineEnd < source.length() && source.charAt(lineEnd) != '\n' && source.charAt(lineEnd) != '\r') {
            lineEnd++;
        }

        int caretColumn = Math.max(0, offset - lineStart);
        String lineText = source.subSequence(lineStart, lineEnd).toString();
        return lineText + System.lineSeparator() + " ".repeat(caretColumn) + "^";
    }

    private static List<? extends Failure<?>> rootFailures(Failure<?> failure) {
        List<? extends Failure<?>> combined = failure.combinedFailures();
        if (combined != null && !combined.isEmpty()) {
            return combined;
        }
        return Collections.singletonList(failure);
    }

    private static void collectExpected(Failure<?> failure, SequencedSet<String> expected) {
        String value = failure.expected();
        if (!failure.context() && value != null && !value.isBlank()) {
            expected.add(value);
        }
        Failure<?> cause = failure.cause();
        if (cause != null) {
            collectExpected(cause, expected);
        }
    }

    private static List<String> collectContexts(Failure<?> failure) {
        List<String> contexts = new ArrayList<>();
        Failure<?> current = failure;
        while (current != null) {
            String value = current.expected();
            if (current.context() && value != null && !value.isBlank()) {
                contexts.add(value);
            }
            current = current.cause();
        }
        return contexts;
    }

    private static List<Cause> collectCauses(Failure<?> failure) {
        List<Cause> causes = new ArrayList<>();
        Failure<?> current = failure.cause();
        while (current != null) {
            Input input = current.input();
            causes.add(new Cause(
                current.expected(),
                found(input),
                input == null ? -1 : input.position(),
                input instanceof TextInput text ? text.line() : -1,
                input instanceof TextInput text ? text.column() : -1,
                current.context()
            ));
            current = current.cause();
        }
        return causes;
    }

    private static String found(Input input) {
        if (input == null) {
            return "unknown input";
        }
        if (!input.hasMore()) {
            return "end of input";
        }
        return "'" + display(input.current()) + "'";
    }

    private static String display(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\f' -> "\\f";
            case '\b' -> "\\b";
            default -> Character.isISOControl(c) ? "\\u%04x".formatted((int) c) : String.valueOf(c);
        };
    }

    /**
     * One nested failure cause in a diagnostic chain.
     *
     * @param expected expectation reported by the cause
     * @param found text describing the input found by the cause
     * @param offset zero-based offset for the cause
     * @param line one-based line for the cause, or {@code -1}
     * @param column one-based column for the cause, or {@code -1}
     * @param context whether this cause is a grammar context
     */
    public record Cause(String expected, String found, int offset, int line, int column, boolean context) {
        public Cause {
            Objects.requireNonNull(found, "found");
        }
    }
}
