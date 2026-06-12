package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.results.PartialMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.parseworks.taker.CharPredicate.anyOf;
import static io.github.parseworks.taker.parsers.Combinators.commit;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.*;

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
