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

package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.*;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.results.PartialMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.parseworks.taker.CharPredicate.anyOf;
import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Combinators.commit;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.escapedString;
import static io.github.parseworks.taker.parsers.Lexical.string;

public class Csv {

    private static final Taker<String> quotedField = escapedString('"', '"', Map.of('"', '"'));

    private static final CharPredicate unquotedDelimiter = anyOf(",\n\r\"");

    private static final Taker<String> unquotedField = new Taker<>(in -> {
        CharSequence data = in.data();
        int start = in.position();
        int current = start;
        while (current < data.length() && !unquotedDelimiter.test(data.charAt(current))) {
            current++;
        }
        return new Match<>(
            data.subSequence(start, current).toString(),
            in.skip(current - start)
        );
    });

    private static final Taker<String> field = commit(quotedField).or(unquotedField);

    private static final Taker<String> lineEnd = oneOf(string("\r\n"), string("\n"), string("\r"));

    public static final Taker<List<String>> row = new Taker<>(in -> {
        if (in.isEof()) {
            return new NoMatch<>(in, "CSV row");
        }

        List<String> fields = new ArrayList<>();
        Input current = in;
        while (true) {
            Result<String> parsed = field.apply(current);
            if (!parsed.matches()) {
                return parsed.cast();
            }

            fields.add(parsed.value());
            current = parsed.input();

            Result<Character> comma = chr(',').apply(current);
            if (!comma.matches()) {
                return new Match<>(Collections.unmodifiableList(fields), current);
            }
            current = comma.input();
        }
    });

    public static final Taker<List<List<String>>> csv = new Taker<>(in -> {
        Result<List<String>> first = row.apply(in);
        if (!first.matches()) {
            return first.cast();
        }

        List<List<String>> rows = new ArrayList<>();
        rows.add(first.value());
        Input current = first.input();

        while (true) {
            Result<String> separator = lineEnd.apply(current);
            if (!separator.matches()) {
                return new Match<>(Collections.unmodifiableList(rows), current);
            }

            Input afterSeparator = separator.input();
            if (afterSeparator.isEof()) {
                return new Match<>(Collections.unmodifiableList(rows), afterSeparator);
            }

            Result<List<String>> next = row.apply(afterSeparator);
            if (!next.matches()) {
                if (next.input().position() > current.position() || afterSeparator.position() > current.position()) {
                    return new PartialMatch<>(next.input(), (Failure<List<String>>) next).cast();
                }
                return next.cast();
            }

            rows.add(next.value());
            current = next.input();
        }
    });
}
