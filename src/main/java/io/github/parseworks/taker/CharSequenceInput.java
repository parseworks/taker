package io.github.parseworks.taker;

/**
 * An implementation of the {@link TextInput} interface that uses a {@link CharSequence} as the input source.
 * This class is immutable and now stores only position and data. Line, column, and line content
 * are computed lazily on demand to reduce memory and copying.
 */
final class CharSequenceInput implements TextInput {

    private final int position;
    private final CharSequence data;
    private volatile int[] lineOffsets;

    CharSequenceInput(int position, CharSequence data) {
        this.position = position;
        this.data = data;
    }

    private CharSequenceInput(int position, CharSequence data, int[] lineOffsets) {
        this.position = position;
        this.data = data;
        this.lineOffsets = lineOffsets;
    }

    /**
     * Constructs a new {@code CharSequenceInput} starting at the beginning of the given {@code CharSequence}.
     *
     * @param data the {@code CharSequence} to be used as the input source
     */
    CharSequenceInput(CharSequence data) {
        this(0, data);
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
                    temp = java.util.Arrays.copyOf(temp, temp.length * 2);
                }
                temp[count++] = i + 1;
            }
        }
        return java.util.Arrays.copyOf(temp, count);
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public CharSequence data() {
        return data;
    }

    // --- Derived properties (computed lazily) ---
    @Override
    public int line() {
        int[] offsets = getLineOffsets();
        int idx = java.util.Arrays.binarySearch(offsets, position);
        if (idx >= 0) return idx + 1;
        return -idx - 1;
    }

    @Override
    public int column() {
        int[] offsets = getLineOffsets();
        int idx = java.util.Arrays.binarySearch(offsets, position);
        if (idx >= 0) return 1;
        int lineStart = offsets[-idx - 2];
        return position - lineStart + 1;
    }

    private static int totalLines(CharSequence data, int[] offsets) {
        return offsets.length;
    }

    /**
     * Checks if the end of the input has been reached.
     */
    @Override
    public boolean isEof() {
        return position >= data.length();
    }

    /**
     * Returns the current character at the current position in the input.
     */
    @Override
    public char current() {
        return data.charAt(position);
    }

    /**
     * Move to the next position.
     */
    @Override
    public Input next() {
        if (isEof()) {
            throw new IllegalStateException("End of input");
        }
        return new CharSequenceInput(position + 1, data, lineOffsets);
    }

    /**
     * Skip by an offset with bounds checking.
     */
    @Override
    public Input skip(int offset) {
        if (offset == 0) {
            return this;
        }
        int newPosition = position + offset;
        if (newPosition > data.length()) newPosition = data.length();
        if (newPosition < 0) newPosition = 0;
        return new CharSequenceInput(newPosition, data, lineOffsets);
    }

    /**
     * Returns the line of text at the specified line number (1-based), computed on demand.
     */
    @Override
    public String getLine(int lineNumber) {
        if (lineNumber < 1) return null;
        int[] offsets = getLineOffsets();
        if (lineNumber > offsets.length) return null;
        int start = offsets[lineNumber - 1];
        int end;
        if (lineNumber == offsets.length) {
            end = data.length();
        } else {
            end = offsets[lineNumber] - 1; // Exclude newline
        }
        return data.subSequence(start, end).toString();
    }

    /**
     * Returns a snippet of the input around the current position.
     */
    @Override
    public String getSnippet(int before, int after) {
        if (isEof()) {
            return "EOF";
        }
        // Keep previous behavior
        int start = Math.max(0, position - before - 2);
        int end = Math.min(data.length(), Math.max(0, position + after - 1));
        if (end < start) {
            end = start;
        }
        return data.subSequence(start, end).toString();
    }

    /**
     * Returns a formatted snippet with line numbers and a caret at the current column.
     */
    @Override
    public String getFormattedSnippet(int linesBefore, int linesAfter) {
        // Even at EOF, render a caret-based snippet to provide consistent context
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
            if (lineText == null) lineText = "";
            snippet.append(String.format("%" + lineNumberWidth + "d | %s%n", i, lineText));
            if (i == line) {
                int totalSpaces = lineNumberWidth + 3 + Math.max(0, column - 1);
                snippet.append(" ".repeat(totalSpaces)).append("^").append(System.lineSeparator());
            }
        }
        return snippet.toString();
    }

    @Override
    public String toString() {
        final String dataStr = isEof() ? "EOF" : String.valueOf(data.charAt(position));
        return "CharSequenceInput{position=" + position + ", line=" + line() + ", column=" + column() + ", data=\"" + dataStr + "\"}";
    }
}
