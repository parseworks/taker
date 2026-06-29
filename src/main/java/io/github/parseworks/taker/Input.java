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
import io.github.parseworks.taker.internal.LinearMap;

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

    /** Returns the current parsing context. Internal use only. */
    default LinearMap context() {
        return LinearMap.empty();
    }

    /** Returns a new cursor with the specified parsing context. Internal use only. */
    default Input withContext(LinearMap context) {
        return this;
    }

}

