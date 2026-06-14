package io.github.parseworks.taker;

import java.util.List;
import java.util.Objects;

/**
 * A failed parser result with structured diagnostic information.
 * <p>
 * Failures may be nested through {@link #cause()} or combined when multiple
 * alternatives fail at the same farthest input position.
 *
 * @param <A> result type
 */
public interface Failure<A> extends Result<A> {
    /** Returns the underlying failure cause, or {@code null}. */
    Failure<?> cause();

    /** Returns what was expected by the failed parser. */
    String expected();

    /**
     * Returns {@code true} when {@link #expected()} names grammar context rather
     * than an expected token or value.
     */
    default boolean context() {
        return false;
    }

    /** Returns failures from tied alternatives, or {@code null}. */
    List<Failure<A>> combinedFailures();

    /** Returns a formatted error message. */
    @Override
    default String error() {
        List<Failure<A>> failures;
        var combinedFailures = this.combinedFailures();
        if (combinedFailures != null && !combinedFailures.isEmpty()) {
            failures = combinedFailures;
        } else {
            failures = List.of(this);
        }

        Failure<A> first = failures.getFirst();
        Input errorInput = first.input();

        if (errorInput == null) {
            for (Failure<A> f : failures) {
                if (f.input() != null) {
                    errorInput = f.input();
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (this.type() == ResultType.PARTIAL) {
            sb.append("Partial match failed: ");
        } else {
            sb.append("Error:");
        }
        TextInput text = (errorInput instanceof TextInput ti) ? ti : null;

        if (text != null) {
            sb.append(" line ").append(text.line())
                .append(" position ").append(text.column())
                .append('\n')
                .append(text.getFormattedSnippet(1, 1));
        } else if (errorInput != null) {
            sb.append(" at position ").append(errorInput.position()).append('\n');
        } else {
            sb.append(" at unknown position\n");
        }

        sb.append("Reasons at this location:\n");
        failures.stream()
            .map(f -> f.error(0))
            .distinct()
            .forEach(sb::append);

        return sb.toString();
    }

    /**
     * Returns structured diagnostics for this failure.
     *
     * @return diagnostics for this failure
     */
    @Override
    default ParseDiagnostics diagnostics() {
        return ParseDiagnostics.from(this);
    }

    /**
     * Returns a human-friendly error message for this failure at the given depth.
     *
     * @param depth the depth of the failure in the chain
     * @return the indented error message
     */
    default String error(int depth) {
        String indent = "  ".repeat(depth);
        StringBuilder builder = new StringBuilder(indent);
        builder.append("- ");
        var expected = this.expected();
        var cause = this.cause();

        // If this failure has the same expectation as its cause, skip this level to reduce nesting
        if (cause != null && context() == cause.context() && Objects.equals(expected, cause.expected())) {
            return cause.error(depth);
        }

        if (context()) {
            builder.append("while parsing ");
            builder.append(expected == null || expected.isEmpty() ? "unknown rule" : expected);
        } else {
            if (depth > 0) builder.append("caused by: ");
            if (expected != null && !expected.isEmpty()) {
                builder.append("expected ").append(expected);
            } else {
                builder.append("expected correct input");
            }

            String foundValue = null;
            var input = this.input();
            if (input != null && input.hasMore()) {
                foundValue = "'" + display(input.current()) + "'";
            }

            if (foundValue != null) {
                builder.append(" found ").append(foundValue);
            } else if (input != null && !input.hasMore()) {
                builder.append(" reached end of input");
            } else {
                builder.append(" found unknown input");
            }
        }

        builder.append("\n");
        if (cause != null) {
            builder.append(cause.error(depth + 1));
        }

        return builder.toString();
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

}
