/*
 * Copyright (c) 2026 jason bailey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.parseworks.taker;


/**
 * Immutable cursor over a character input.
 * <p>
 * Advancing methods such as {@link #next()} and {@link #skip(int)} return a new
 * cursor and leave the original cursor unchanged. Positions are zero-based
 * character offsets into {@link #data()}.
 */
public interface Input {
    /**
     * Creates an {@code Input} from a {@link CharSequence}.
     *
     * @param s source text
     * @return an input cursor at the start of {@code s}
     */
    static Input of(CharSequence s) {
        return Inputs.of(s);
    }

    /**
     * Returns the complete backing character data.
     *
     * @return backing data
     */
    CharSequence data();

    /**
     * Returns true if at the end of input.
     *
     * @return whether this cursor is at EOF
     */
    boolean isEof();

    /**
     * Returns the current character. Throws if {@link #isEof()} is true.
     *
     * @return current character
     */
    char current();

    /**
     * Returns a cursor advanced by one character. Throws if {@link #isEof()} is true.
     *
     * @return advanced cursor
     */
    Input next();

    /**
     * Returns the current zero-based character offset.
     *
     * @return current offset
     */
    int position();

    /**
     * Returns a cursor advanced by {@code offset} characters.
     *
     * @param offset number of characters to advance
     * @return advanced cursor
     */
    Input skip(int offset);

    /**
     * Returns {@code true} when this cursor has at least one current character.
     *
     * @return whether input remains
     */
    default boolean hasMore(){
        return !isEof();
    }

    /**
     * Returns the current parsing context.
     * <p>
     * Internal use only. Used for features like recursion detection.
     *
     * @return the current parsing context
     */
    default Context context() {
        return Context.empty();
    }

    /**
     * Returns a new cursor with the specified parsing context.
     * <p>
     * Internal use only.
     *
     * @param context the new parsing context
     * @return a new cursor with the updated context
     */
    default Input withContext(Context context) {
        return this;
    }

}

