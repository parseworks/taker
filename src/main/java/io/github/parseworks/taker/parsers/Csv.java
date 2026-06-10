package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Taker;

import java.util.List;
import java.util.Map;

import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.*;

public class Csv {

    private static final Taker<String> quotedField = escapedString('"', '"', Map.of('"', '"'));

    private static final Taker<String> unquotedField = Taker.takeWhile(c -> c != ',' && c != '\n' && c != '\r');

    private static final Taker<String> field = quotedField.or(unquotedField);

    public static final Taker<List<String>> row = field.oneOrMoreSeparatedBy(chr(','));

    public static final Taker<List<List<String>>> csv = row.oneOrMoreSeparatedBy(oneOf(string("\r\n"), string("\n"), string("\r")));
}
