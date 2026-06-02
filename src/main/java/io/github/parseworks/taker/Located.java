package io.github.parseworks.taker;

/**
 * A parsed value together with the source offsets consumed to produce it.
 *
 * @param value parsed value
 * @param start zero-based inclusive start offset
 * @param end zero-based exclusive end offset
 * @param <A> parsed value type
 */
public record Located<A>(A value, int start, int end) {
    public Located {
        if (start < 0) {
            throw new IllegalArgumentException("start cannot be negative");
        }
        if (end < start) {
            throw new IllegalArgumentException("end cannot be before start");
        }
    }

    /** Number of characters covered by this location. */
    public int length() {
        return end - start;
    }
}
