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
import java.util.Arrays;

final class Inputs {

    private Inputs() {
    }

    static Input of(CharSequence data) {
        return new CharSequenceInput(data);
    }

    private static final class CharSequenceInput implements TextInput {

        private final int position;
        private final CharSequence data;
        private final LinearMap context;
        private volatile int[] lineOffsets;

        private CharSequenceInput(int position, CharSequence data, LinearMap context) {
            this.position = position;
            this.data = data;
            this.context = context;
        }

        private CharSequenceInput(int position, CharSequence data, LinearMap context, int[] lineOffsets) {
            this.position = position;
            this.data = data;
            this.context = context;
            this.lineOffsets = lineOffsets;
        }

        private CharSequenceInput(CharSequence data) {
            this(0, data, LinearMap.empty());
        }

        private int[] getLineOffsets() {
            int[] offsets = lineOffsets;
            if (offsets == null) {
                synchronized (this) {
                    offsets = lineOffsets;
                    if (offsets == null) {
                        offsets = computeLineOffsets(data);
                        lineOffsets = offsets;
                    }
                }
            }
            return offsets;
        }

        private static int[] computeLineOffsets(CharSequence data) {
            int[] temp = new int[Math.min(data.length() / 20 + 10, 1000)];
            int count = 0;
            temp[count++] = 0;
            for (int i = 0; i < data.length(); i++) {
                if (data.charAt(i) == '\n') {
                    if (count == temp.length) {
                        temp = Arrays.copyOf(temp, temp.length * 2);
                    }
                    temp[count++] = i + 1;
                }
            }
            return Arrays.copyOf(temp, count);
        }

        @Override
        public int position() {
            return position;
        }

        @Override
        public CharSequence data() {
            return data;
        }

        @Override
        public int line() {
            int[] offsets = getLineOffsets();
            int idx = Arrays.binarySearch(offsets, position);
            if (idx >= 0) {
                return idx + 1;
            }
            return -idx - 1;
        }

        @Override
        public int column() {
            int[] offsets = getLineOffsets();
            int idx = Arrays.binarySearch(offsets, position);
            if (idx >= 0) {
                return 1;
            }
            int lineStart = offsets[-idx - 2];
            return position - lineStart + 1;
        }

        @Override
        public boolean isEof() {
            return position >= data.length();
        }

        @Override
        public char current() {
            return data.charAt(position);
        }

        @Override
        public Input next() {
            if (isEof()) {
                throw new IllegalStateException("End of input");
            }
            return new CharSequenceInput(position + 1, data, context, lineOffsets);
        }

        @Override
        public Input skip(int offset) {
            if (offset == 0) {
                return this;
            }
            int newPosition = position + offset;
            if (newPosition > data.length()) {
                newPosition = data.length();
            }
            if (newPosition < 0) {
                newPosition = 0;
            }
            return new CharSequenceInput(newPosition, data, context, lineOffsets);
        }

        @Override
        public LinearMap context() {
            return context;
        }

        @Override
        public Input withContext(LinearMap context) {
            return new CharSequenceInput(position, data, context, lineOffsets);
        }

        @Override
        public String getLine(int lineNumber) {
            if (lineNumber < 1) {
                return null;
            }
            int[] offsets = getLineOffsets();
            if (lineNumber > offsets.length) {
                return null;
            }
            int start = offsets[lineNumber - 1];
            int end;
            if (lineNumber == offsets.length) {
                end = data.length();
            } else {
                end = offsets[lineNumber] - 1;
            }
            return data.subSequence(start, end).toString();
        }

        @Override
        public String getSnippet(int before, int after) {
            if (isEof()) {
                return "EOF";
            }
            int start = Math.max(0, position - before - 2);
            int end = Math.clamp(position + after - 1, 0, data.length());
            if (end < start) {
                end = start;
            }
            return data.subSequence(start, end).toString();
        }

        @Override
        public String getFormattedSnippet(int linesBefore, int linesAfter) {
            int line = line();
            int column = column();
            int[] offsets = getLineOffsets();
            int total = offsets.length;
            int startLine = Math.max(1, line - linesBefore);
            int endLine = Math.min(total, line + linesAfter);

            StringBuilder snippet = new StringBuilder();
            int lineNumberWidth = String.valueOf(endLine).length();

            for (int i = startLine; i <= endLine; i++) {
                String lineText = getLine(i);
                if (lineText == null) {
                    lineText = "";
                }
                snippet.append(String.format("%" + lineNumberWidth + "d | %s%n", i, lineText));
                if (i == line) {
                    int totalSpaces = lineNumberWidth + 3 + Math.max(0, column - 1);
                    snippet.repeat(" ", totalSpaces).append("^").append(System.lineSeparator());
                }
            }
            return snippet.toString();
        }

        @Override
        public String toString() {
            final String dataStr = isEof() ? "EOF" : String.valueOf(data.charAt(position));
            return "CharSequenceInput{position=" + position + ", line=" + line()
                    + ", column=" + column() + ", data=\"" + dataStr + "\"}";
        }
    }

}
