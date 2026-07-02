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
 * Extends {@link Input} with line, column, and error reporting information.
 */
public interface TextInput extends Input {
    /**
     * Returns the 1-based line number.
     *
     * @return current line number
     */
    int line();
    
    /**
     * Returns the 1-based column number.
     *
     * @return current column number
     */
    int column();
    
    /**
     * Returns the text at the specified line number.
     *
     * @param lineNumber 1-based line number
     * @return line text, or {@code null}
     */
    String getLine(int lineNumber);
    
    /**
     * Returns a snippet around the current position.
     *
     * @param before characters before the current position
     * @param after characters after the current position
     * @return snippet text
     */
    String getSnippet(int before, int after);
    
    /**
     * Returns a formatted snippet with line numbers and a caret marker.
     *
     * @param linesBefore lines before the current line
     * @param linesAfter lines after the current line
     * @return formatted snippet
     */
    String getFormattedSnippet(int linesBefore, int linesAfter);
}
