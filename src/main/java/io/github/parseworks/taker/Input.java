package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.inputs.*;

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
        return new CharSequenceInput(s);
    }

    /** Returns an {@code Input} that will always return characters as lowercase. */
    static Input lowercase(Input input) {
        if (input instanceof LowercaseInput) {
            return input;
        }
        if (input instanceof TextInput textInput) {
            return new LowercaseTextInput(textInput);
        }
        return new LowercaseInput(input);
    }

    /** Returns an {@code Input} that will always return characters as uppercase. */
    static Input uppercase(Input input) {
        if (input instanceof UppercaseInput) {
            return input;
        }
        if (input instanceof TextInput textInput) {
            return new UppercaseTextInput(textInput);
        }
        return new UppercaseInput(input);
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

