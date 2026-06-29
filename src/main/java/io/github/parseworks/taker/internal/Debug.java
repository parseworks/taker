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

package io.github.parseworks.taker.internal;

import io.github.parseworks.taker.*;

public final class Debug {

    private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    private Debug() {
    }

    public static <A> Taker<A> systemOut(Taker<A> parser, String label) {
        return new Taker<>(input -> {
            int currentDepth = depth.get();
            String indent = "  ".repeat(currentDepth);
            String name = label != null ? label : "Taker";
            String snippet = getSnippet(input);

            System.out.printf("%s%s starting at pos %d: [%s]%n",
                indent, name, input.position(), snippet);

            depth.set(currentDepth + 1);
            long start = System.nanoTime();
            try {
                Result<A> result = parser.apply(input);
                long elapsed = System.nanoTime() - start;
                double ms = elapsed / 1_000_000.0;

                if (result.matches()) {
                    System.out.printf("%s%s succeeded in %.3fms with value: %s%n",
                        indent, name, ms, result.value());
                } else {
                    System.out.printf("%s%s failed in %.3fms: %s%n",
                        indent, name, ms, result.error());
                }
                return result;
            } finally {
                depth.set(currentDepth);
            }
        });
    }

    private static String getSnippet(Input input) {
        StringBuilder sb = new StringBuilder();
        Input temp = input;
        for (int i = 0; i < 20 && !temp.isEof(); i++) {
            char c = temp.current();
            if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
            temp = temp.next();
        }
        if (!temp.isEof()) sb.append("...");
        return sb.toString();
    }
}
