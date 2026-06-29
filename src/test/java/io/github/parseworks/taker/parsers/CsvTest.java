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

import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
