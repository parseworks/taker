package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** CSV parsing edge cases: empty, multiline, special values */
public class CsvEdgeCaseTest {

    @Test
    void parseEmptyString_failsRow() {
        Result<List<String>> result = Csv.row.parse(Input.of(""));
        assertFalse(result.matches(), "empty string should fail to parse as row");
    }

    @Test
    void parseSingleField_returnsOneItem() {
        Result<List<String>> result = Csv.row.parseAll("hello");
        assertTrue(result.matches(), () -> "single field should succeed: " + result.error());
        assertEquals(1, result.value().size());
        assertEquals("hello", result.value().get(0));
    }

    @Test
    void parseRowWithTrailingNewline() {
        Result<List<String>> result = Csv.row.parseAll("a,b");
        assertTrue(result.matches());
        assertEquals(2, result.value().size());
    }

    @Test
    void parseMultipleRows_withTrailingNewline() {
        String input = "a,b\n";
        Result<List<List<String>>> result = Csv.csv.parse(input);
        assertTrue(result.matches(), () -> "multi row with trailing newline should pass: " + result.error());
    }

    @Test  
    void parseEmptyFirstField_returnsEmptyString() {
        String input = ",b,c";
        Result<List<String>> result = Csv.row.parseAll(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
        assertEquals("", result.value().get(0), "first field should be empty string");
    }

    @Test
    void parseOnlyComma_returnsTwoEmptyFields() {
        String input = ",";
        Result<List<String>> result = Csv.row.parseAll(input);
        assertTrue(result.matches());
        assertEquals(2, result.value().size());
        assertEquals("", result.value().get(0));
        assertEquals("", result.value().get(1));
    }

    @Test
    void parseRowEndsAtComma() {
        String input = "a,,";
        Result<List<String>> result = Csv.row.parseAll(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    void parseQuotedFieldWithEmbeddedComma() {
        String input = "\"hello,world\",b";
        Result<List<String>> result = Csv.row.parseAll(input);
        assertTrue(result.matches());
        assertEquals("hello,world", result.value().get(0));
        assertEquals("b", result.value().get(1));
    }

    @Test
    void parseQuotedFieldWithEscapedQuote() {
        String input = "\"he said \"\"hi\"\"\",b";
        Result<List<String>> result = Csv.row.parseAll(input);
        assertTrue(result.matches());
        assertEquals("he said \"hi\"", result.value().get(0));
    }

    @Test
    void parseEmptyCsv_fails() {
        String input = "";
        Result<List<List<String>>> result = Csv.csv.parse(input);
        assertFalse(result.matches(), "empty CSV should fail");
    }

    @Test  
    void singleRowCsv_returnsOneRow() {
        String input = "a,b,c";
        Result<List<List<String>>> result = Csv.csv.parseAll(input);
        assertTrue(result.matches());
        assertEquals(1, result.value().size());
        assertEquals(3, result.value().get(0).size());
    }

    @Test
    void multiRowWithMixedLineEndings() {
        String input = "a,b\nc,d\rne,f";
        Result<List<List<String>>> result = Csv.csv.parseAll(input);
        assertTrue(result.matches(), () -> "mixed line endlngs: " + result.error());
        assertEquals(3, result.value().size());
    }
}
