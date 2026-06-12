package io.github.parseworks.taker;

/**
 * Immutable cursor over a character input.
 * <p>
 * Advancing methods such as {@link #next()} and {@link #skip(int)} return a new
 * cursor and leave the original cursor unchanged. Positions are zero-based
 * character offsets into {@link #data()}.
 */
public interface Input {
    /** Creates an {@code Input} from a {@link CharSequence}. */
    static Input of(CharSequence s) {
        return Inputs.of(s);
    }

    /** Returns an {@code Input} that will always return characters as lowercase. */
    static Input lowercase(Input input) {
        return Inputs.lowercase(input);
    }

    /** Returns an {@code Input} that will always return characters as uppercase. */
    static Input uppercase(Input input) {
        return Inputs.uppercase(input);
    }

    /** Returns the complete backing character data. */
    CharSequence data();

    /** Returns true if at the end of input. */
    boolean isEof();

    /** Returns the current character. Throws if {@link #isEof()} is true. */
    char current();

    /** Returns a cursor advanced by one character. Throws if {@link #isEof()} is true. */
    Input next();

    /** Returns the current zero-based character offset. */
    int position();

    /** Returns a cursor advanced by {@code offset} characters. */
    Input skip(int offset);

    /** Returns {@code true} when this cursor has at least one current character. */
    default boolean hasMore(){
        return !isEof();
    }

}

