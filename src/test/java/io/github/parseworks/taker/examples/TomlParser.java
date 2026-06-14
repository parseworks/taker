package io.github.parseworks.taker.examples;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.examples.TomlModel.TomlArray;
import io.github.parseworks.taker.examples.TomlModel.TomlBoolean;
import io.github.parseworks.taker.examples.TomlModel.TomlDate;
import io.github.parseworks.taker.examples.TomlModel.TomlDocument;
import io.github.parseworks.taker.examples.TomlModel.TomlFloat;
import io.github.parseworks.taker.examples.TomlModel.TomlInteger;
import io.github.parseworks.taker.examples.TomlModel.TomlString;
import io.github.parseworks.taker.examples.TomlModel.TomlTable;
import io.github.parseworks.taker.examples.TomlModel.TomlValue;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.collectChars;
import static io.github.parseworks.taker.parsers.Lexical.escapedString;
import static io.github.parseworks.taker.parsers.Lexical.regex;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Lexical.takeUntil;

/**
 * A compact TOML parser suitable for executable documentation.
 * <p>
 * This intentionally covers the common configuration-file subset: tables,
 * dotted keys, basic and literal strings, booleans, integers, floats, local
 * dates, arrays, inline tables, blank lines, and comments. Full TOML 1.0 also
 * includes multiline strings, date-times, array-of-tables, and stricter numeric
 * edge cases; those would extend the same parser shape.
 */
final class TomlParser {
    private static final Taker<?> HORIZONTAL_SPACE = chr(CharPredicate.anyOf(" \t")).skipOneOrMore();

    private static final Taker<String> BASIC_STRING = escapedString('"', '\\', Map.of(
        '"', '"',
        '\\', '\\',
        'b', '\b',
        't', '\t',
        'n', '\n',
        'f', '\f',
        'r', '\r'
    )).expecting("basic string");

    private static final Taker<String> LITERAL_STRING = chr('\'')
        .skipThen(takeUntil("'"))
        .thenSkip(chr('\''))
        .expecting("literal string");

    private static final Taker<String> BARE_KEY = collectChars(
        CharPredicate.asciiLetterOrDigit.or(CharPredicate.anyOf("_-"))
    ).expecting("bare key");

    private static final Taker<String> KEY_PART = token(oneOf(BASIC_STRING, LITERAL_STRING, BARE_KEY));
    private static final Taker<List<String>> KEY_PATH = KEY_PART.oneOrMoreSeparatedBy(token(chr('.')));

    private static final Taker<TomlValue> VALUE = Taker.ref();

    private static final Taker<TomlValue> STRING_VALUE = token(oneOf(BASIC_STRING, LITERAL_STRING))
        .map(TomlString::new);

    private static final Taker<TomlValue> BOOLEAN_VALUE = oneOf(
        token(string("true")).as(new TomlBoolean(true)),
        token(string("false")).as(new TomlBoolean(false))
    );

    private static final Taker<TomlValue> DATE_VALUE = token(regex("\\d{4}-\\d{2}-\\d{2}"))
        .map(value -> (TomlValue) new TomlDate(LocalDate.parse(value)))
        .expecting("local date");

    private static final Taker<TomlValue> NUMBER_VALUE = token(regex("[+-]?(?:\\d+\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?"))
        .map(TomlParser::number)
        .expecting("number");

    private static final Taker<TomlValue> ARRAY_VALUE = token(chr('['))
        .skipThen(
            VALUE.oneOrMoreSeparatedBy(token(chr(',')))
                .thenSkip(token(chr(',')).optional())
                .orElse(List.of())
        )
        .thenSkip(token(chr(']')))
        .map(TomlArray::new);

    private static final Taker<TomlPair> INLINE_PAIR = KEY_PATH
        .thenSkip(token(chr('=')))
        .then(VALUE)
        .map(TomlPair::new);

    private static final Taker<TomlValue> INLINE_TABLE_VALUE = token(chr('{'))
        .skipThen(INLINE_PAIR.zeroOrMoreSeparatedBy(token(chr(','))))
        .thenSkip(token(chr('}')))
        .flatMap(TomlParser::inlineTable);

    private static final Taker<TomlLine> TABLE_HEADER = token(chr('['))
        .skipThen(KEY_PATH)
        .thenSkip(token(chr(']')))
        .map(TomlTableHeader::new);

    private static final Taker<TomlLine> ASSIGNMENT = KEY_PATH
        .thenSkip(token(chr('=')))
        .then(VALUE)
        .map(TomlAssignment::new);

    private static final Taker<Void> COMMENT = token(chr('#')).skipThen(takeUntil(CharPredicate.is('\n'))).as(null);
    private static final Taker<Void> TRAILING = token(COMMENT.optional()).as(null);
    private static final Taker<TomlLine> LINE = oneOf(TABLE_HEADER, ASSIGNMENT).thenSkip(TRAILING);

    static {
        VALUE.set(oneOf(
            ARRAY_VALUE,
            INLINE_TABLE_VALUE,
            STRING_VALUE,
            BOOLEAN_VALUE,
            DATE_VALUE,
            NUMBER_VALUE
        ).expecting("TOML value"));
    }

    private TomlParser() {
    }

    static Result<TomlDocument> parse(String source) {
        TomlDocument document = new TomlDocument();
        List<String> currentTable = List.of();
        Input sourceInput = Input.of(source);

        int offset = 0;
        for (String line : source.split("\\R", -1)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                Result<TomlLine> parsed = LINE.parseAll(line);
                if (!parsed.matches()) {
                    Failure<?> failure = (Failure<?>) parsed;
                    return new NoMatch<>(
                        sourceInput.skip(offset + failure.input().position()),
                        failure.expected(),
                        failure
                    );
                }

                TomlLine tomlLine = parsed.value();
                if (tomlLine instanceof TomlTableHeader(List<String> path)) {
                    if (!document.canCreateTable(path)) {
                        return new NoMatch<>(sourceInput.skip(offset), "TOML table path");
                    }
                    document.table(path);
                    currentTable = path;
                } else if (tomlLine instanceof TomlAssignment(List<String> key, TomlValue value)) {
                    String duplicate = document.put(join(currentTable, key), value);
                    if (duplicate != null) {
                        return new NoMatch<>(sourceInput.skip(offset), "unique TOML key \"" + duplicate + "\"");
                    }
                }
            }
            offset += line.length() + 1;
        }

        return new Match<>(document, sourceInput.skip(source.length()));
    }

    private static <A> Taker<A> token(Taker<A> parser) {
        return io.github.parseworks.taker.parsers.Lexical.lexeme(parser, HORIZONTAL_SPACE);
    }

    private static Taker<TomlValue> inlineTable(List<TomlPair> pairs) {
        return new Taker<>(in -> {
            TomlTable table = new TomlTable();
            for (TomlPair pair : pairs) {
                String duplicate = table.put(pair.key(), pair.value());
                if (duplicate != null) {
                    return new NoMatch<>(in, "unique inline TOML key \"" + duplicate + "\"");
                }
            }
            return new Match<>(table, in);
        });
    }

    private static TomlValue number(String value) {
        if (value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0) {
            return new TomlFloat(Double.parseDouble(value));
        }
        return new TomlInteger(Long.parseLong(value));
    }

    private static List<String> join(List<String> left, List<String> right) {
        ArrayList<String> joined = new ArrayList<>(left.size() + right.size());
        joined.addAll(left);
        joined.addAll(right);
        return List.copyOf(joined);
    }

    private sealed interface TomlLine permits TomlTableHeader, TomlAssignment {
    }

    private record TomlTableHeader(List<String> path) implements TomlLine {
        private TomlTableHeader {
            path = List.copyOf(path);
        }
    }

    private record TomlAssignment(List<String> key, TomlValue value) implements TomlLine {
        private TomlAssignment {
            key = List.copyOf(key);
        }
    }

    private record TomlPair(List<String> key, TomlValue value) {
        private TomlPair {
            key = List.copyOf(key);
        }
    }
}
