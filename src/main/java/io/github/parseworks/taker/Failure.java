package io.github.parseworks.taker;

import java.util.List;
import java.util.Objects;

/**
 * Represents a failure result.
 *
 * @param <A> result type
 */
public interface Failure<A> extends Result<A> {
    /** Returns the underlying failure cause, or null. */
    Failure<?> cause();

    /** Returns what was expected by the failed parser. */
    String expected();

    /** Returns combined failures from multiple alternative parsers. */
    List<Failure<A>> combinedFailures();

    /** Returns a formatted error message. */
    @Override
    default String error() {
        List<Failure<A>> failures = combinedFailures();
        if (failures == null || failures.isEmpty()) {
            failures = List.of(this);
        }

        Input errorInput = failures.stream()
                .map(Result::input)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(input());

        StringBuilder sb = new StringBuilder("Error:");
        if (errorInput instanceof TextInput text) {
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
     * Returns a human-friendly error message for this failure with indentation based on depth.
     */
    default String error(int depth) {
        var expected = this.expected();
        var cause = this.cause();

        if (cause != null && Objects.equals(expected, cause.expected())) {
            return cause.error(depth);
        }

        StringBuilder builder = new StringBuilder("  ".repeat(depth)).append("- ");
        if (depth > 0) builder.append("caused by: ");
        builder.append(expected != null && !expected.isEmpty() ? "expected " + expected : "expected correct input");

        Input in = input();
        if (in != null) {
            builder.append(in.hasMore() ? " found " + in.current() : " reached end of input");
        } else {
            builder.append(" found unknown input");
        }

        builder.append("\n");
        if (cause != null) {
            builder.append(cause.error(depth + 1));
        }

        return builder.toString();
    }
}
