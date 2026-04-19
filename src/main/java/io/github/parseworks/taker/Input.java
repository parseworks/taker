package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.inputs.*;

/**
 * Represents a position in a stream of input symbols.
 *e
 */
public interface Input {
    /** Creates an {@code Input} from a {@code char} array. */
    static Input of(char[] data) {
        return new CharArrayInput(data);
    }

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

    CharSequence data();

    /** Returns true if at the end of input. */
    boolean isEof();

    /** Returns the current symbol. Throws if {@code isEof} is true. */
    char current();

    /** Returns the next position. Throws if {@code isEof} is true. */
    Input next();

    /** Returns the current position. */
    int position();

    /** Returns a new input advanced by the given offset. */
    Input skip(int offset);

    default boolean hasMore(){
        return !isEof();
    }



}

