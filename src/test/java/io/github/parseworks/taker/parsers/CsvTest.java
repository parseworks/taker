package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvTest {

    @Test
    void parsesRowsWithQuotedCommas() {
        String input = "name,age,city\n\"Bob, The Builder\",30,London\nAlice,25,\"New York\"";

        Result<List<List<String>>> result = Csv.csv.parseAll(input);

        assertTrue(result.matches(), result::error);
        assertEquals(List.of(
            List.of("name", "age", "city"),
            List.of("Bob, The Builder", "30", "London"),
            List.of("Alice", "25", "New York")
        ), result.value());
    }

    @Test
    void parsesEmptyFields() {
        Result<List<List<String>>> result = Csv.csv.parseAll("a,,c\n,2,");

        assertTrue(result.matches(), result::error);
        assertEquals(List.of(
            List.of("a", "", "c"),
            List.of("", "2", "")
        ), result.value());
    }

    @Test
    void parsesEscapedQuotesInsideQuotedFields() {
        Result<List<String>> result = Csv.row.parseAll("\"Bob \"\"The Builder\"\"\",30");

        assertTrue(result.matches(), result::error);
        assertEquals(List.of("Bob \"The Builder\"", "30"), result.value());
    }

    @Test
    void parsesCrLfAndAllowsTrailingLineEnding() {
        Result<List<List<String>>> result = Csv.csv.parseAll("a,b\r\nc,d\r\n");

        assertTrue(result.matches(), result::error);
        assertEquals(List.of(
            List.of("a", "b"),
            List.of("c", "d")
        ), result.value());
    }

    @Test
    void rejectsMalformedQuotedField() {
        Result<List<List<String>>> result = Csv.csv.parseAll("\"unterminated");

        assertFalse(result.matches());
    }

    @Test
    void rejectsQuoteInsideUnquotedField() {
        Result<List<List<String>>> result = Csv.csv.parseAll("a,b\"c");

        assertFalse(result.matches());
    }

    @Test
    void rejectsEmptyInput() {
        Result<List<List<String>>> result = Csv.csv.parseAll("");

        assertFalse(result.matches());
    }
}
