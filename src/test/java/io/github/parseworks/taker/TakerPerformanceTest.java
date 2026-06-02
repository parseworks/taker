package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.parseworks.taker.Taker.*;
import static io.github.parseworks.taker.parsers.Combinators.not;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TakerPerformanceTest {

    @Test
    public void testRepetitionPerformance() {
        Taker<List<Character>> letterParser = chr(Character::isLetter).zeroOrMore();
        Taker<List<Character>> digitParser = chr(Character::isDigit).zeroOrMore();

        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            input.append("a").append(i % 10);
        }

        long startTime = System.nanoTime();

        var parser = letterParser.then(digitParser).map(Lists::appendAll);

        for (int i = 0; i < 5; i++) {
            Result<List<Character>> result = parser.parse(input.toString());
            assertTrue(result.matches(), "Parsing should succeed");
        }

        long duration = System.nanoTime() - startTime;
        assertTrue(duration < 5_000_000_000L,
                "Parsing should complete within reasonable time");
    }

    @Test
    public void testLargeInputPerformance() {
        Taker<Character> commaOnly = chr(',');
        Taker<Character> comma = trim(commaOnly);
        Taker<Character> eol = string("\r\n").as('\n').or(chr('\n'));

        Taker<String> quotedField = escapedString('"', '\\', Map.of('"', '"'));
        Taker<String> unquotedFieldCore = takeWhile(c -> c != ',' && c != '\n' && c != '\r');
        Taker<String> unquotedField = unquotedFieldCore.onlyIf(not(chr('"')));

        Taker<String> boolToken = oneOf(
            string("true"),
            string("false")
        );

        Taker<String> nullToken = oneOf(
            string("NULL"),
            string("null")
        );

        Taker<String> numberToken = Numeric.doubleValue.map(String::valueOf);

        Taker<String> field = oneOf(
            boolToken.expecting("boolean"),
            nullToken.expecting("null"),
            numberToken.expecting("number"),
            quotedField.expecting("quoted"),
            unquotedField.expecting("unquoted")
        );

        Taker<List<String>> row = field.oneOrMoreSeparatedBy(comma);
        Taker<List<List<String>>> csvParser = row.oneOrMoreSeparatedBy(eol);

        String input = getInput();

        long startTime = System.nanoTime();
        Result<List<List<String>>> result = csvParser.parse(input);
        long duration = System.nanoTime() - startTime;

        assertTrue(result.matches(), () -> "Parsing should succeed: " + result.error());
        assertEquals(500_000, result.value().size(), "Should parse all lines");
        assertTrue(duration < 100_000_000_000L,
            "Large input parsing should be efficient");
    }

    private static String getInput() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 500_000; i++) {
            for (int j = 0; j < 10; j++) {
                switch (j % 5) {
                    case 0 -> input.append("\"field\\\"").append(j).append("\"");
                    case 1 -> input.append("true");
                    case 2 -> input.append("-123.45");
                    case 3 -> input.append("NULL");
                    default -> input.append("plain_").append(j);
                }
                if (j < 9) input.append(", ");
            }
            if (i < 499_999) {
                input.append("\n");
            }
        }
        return input.toString();
    }

    @Test
    public void testStringCachingPerformance() {
        Taker<Character> letterParser = chr('a');
        String input = "a";

        long startTime = System.nanoTime();

        Result<Character> firstResult = letterParser.parse(input);
        assertTrue(firstResult.matches());
        long firstParseDuration = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Result<Character> result = letterParser.parse(input);
            assertTrue(result.matches());
        }
        long subsequentParsesDuration = System.nanoTime() - startTime;

        double avgTimePerParse = (double) subsequentParsesDuration / 1000;
        assertTrue(avgTimePerParse < firstParseDuration,
                "Subsequent parses should be faster due to caching");
    }
}
