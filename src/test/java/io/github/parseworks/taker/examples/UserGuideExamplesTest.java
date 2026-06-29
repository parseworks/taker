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

package io.github.parseworks.taker.examples;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.regex;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that the examples in the user-guide.md work correctly.
 */
public class UserGuideExamplesTest {

    /**
     * Test for Tutorial 1: Creating Your First Taker
     */
    @Test
    public void testTutorial1() {
        // Create a parser that recognizes the string "hello"
        Taker<String> helloParser = Lexical.string("hello");

        // Parse the input "hello world"
        Result<String> result = helloParser.parse(Input.of("hello world"));

        // Check if parsing succeeded
        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals(' ', result.input().current()); // Check the current character of the remaining input

        // Test the handle method
        String message = result.handle(
            success -> "Successfully parsed: " + success.value(),
            failure -> "Parsing failed: " + failure.error()
        );
        assertEquals("Successfully parsed: hello", message);
    }

    /**
     * Test for Tutorial 2: Combining Parsers
     */
    @Test
    public void testTutorial2() {
        // Taker for the word "hello"
        Taker<String> helloParser = Lexical.string("hello");

        // Taker for the word "world"
        Taker<String> worldParser = Lexical.string("world");

        // Taker for whitespace
        Taker<String> whitespaceParser = chr(' ').skipOneOrMore().as("");

        // Taker for "hello world" that ignores whitespace
        Taker<String> cleanerParser = helloParser
            .thenSkip(whitespaceParser)
            .then(worldParser)
            .map(hello -> world -> hello + " " + world);

        Result<String> result = cleanerParser.parse(Input.of("hello world"));
        assertTrue(result.matches());
        assertEquals("hello world", result.value());
    }

    /**
     * Test for Tutorial 3: Parsing Structured Data
     */
    @Test
    public void testTutorial3() {
        // Taker for keys (alphanumeric strings)
        Taker<String> keyParser = regex("[a-zA-Z0-9]+");

        // Taker for the equals sign
        Taker<Character> equalsParser = chr('=');

        // Taker for values (any string until end of line)
        Taker<String> valueParser = regex("[^\\n]*");

        // Taker for a key-value pair
        Taker<KeyValue> keyValueParser = keyParser
            .thenSkip(equalsParser)
            .then(valueParser)
            .map(key -> value -> new KeyValue(key, value));

        // Taker for multiple key-value pairs separated by newlines
        Taker<List<KeyValue>> configParser = keyValueParser
            .oneOrMoreSeparatedBy(chr('\n'));

        // Parse a configuration file
        String config = "server=localhost\nport=8080\nuser=admin";
        Result<List<KeyValue>> result = configParser.parse(Input.of(config));

        assertTrue(result.matches());
        List<KeyValue> keyValues = result.value();
        assertEquals(3, keyValues.size());
        assertEquals("server", keyValues.get(0).getKey());
        assertEquals("localhost", keyValues.get(0).getValue());
        assertEquals("port", keyValues.get(1).getKey());
        assertEquals("8080", keyValues.get(1).getValue());
        assertEquals("user", keyValues.get(2).getKey());
        assertEquals("admin", keyValues.get(2).getValue());
    }

    /**
     * Test for Tutorial 4: Error Handling
     */
    @Test
    public void testTutorial4() {
        // Taker for keys (alphanumeric strings)
        Taker<String> keyParser = regex("[a-zA-Z0-9]+").expecting("key");

        // Taker for the equal sign
        Taker<Character> equalsParser = chr('=').expecting("equals");

        // Taker for values (any string until the end of line)
        Taker<String> valueParser = chr(c -> c != '\n' && c != ',' && c != '}')
            .collectString()
            .expecting("value");

        // Taker for a key-value pair
        Taker<KeyValue> keyValueParser = keyParser
            .thenSkip(equalsParser)
            .then(valueParser)
            .map(key -> value -> new KeyValue(key, value))
            .expecting("key-value pair");

        // Taker for a JSON-like object
        Taker<Map<String, String>> objectParser = chr('{')
            .skipThen(
                keyValueParser.<String, Map<String, String>>foldSeparatedByFrom(Lexical.string(","), HashMap::new, (map, kv) -> {
                    map.put(kv.getKey(), kv.getValue());
                    return map;
                })
            )
            .thenSkip(chr('}'));

        // Test with valid input
        String validInput = "{name=John,age=30}";
        Result<Map<String, String>> validResult = objectParser.parse(Input.of(validInput));
        assertTrue(validResult.matches());
        Map<String, String> map = validResult.value();
        assertEquals(2, map.size());
        assertEquals("John", map.get("name"));
        assertEquals("30", map.get("age"));

        // Test with invalid input
        String invalidInput = "{name=John,age="; // Missing closing brace
        Result<Map<String, String>> invalidResult = objectParser.parse(Input.of(invalidInput));
        assertFalse(invalidResult.matches());
        assertTrue(invalidResult.errorOptional().isPresent());
    }

    /**
     * Validation for Tutorial 4 Step 5: Label failures with expecting(...)
     * Mirrors the user-guide example to ensure it stays correct.
     */
    @Test
    public void testTutorial4_ExpectingExample() {
        // Suppose an identifier is a letter followed by zero or more alphanumerics
        // Use a regex-based parser for a concise identifier definition
        Taker<String> identifier =
            regex("[A-Za-z][A-Za-z0-9]*")
                .expecting("identifier");

        Result<String> r = identifier.parse("123");
        assertTrue(!r.matches(), "Identifier should fail when starting with a digit");
        String msg = r.error();
        assertTrue(msg.contains("expected identifier"),
                () -> "Error should mention relabeled expectation, but was:\n" + msg);
    }

    /**
     * Test for Tutorial 5: Creating a Calculator Taker
     */
    @Test
    public void testTutorial5() {

        // Taker for numbers
        Taker<Integer> number = regex("[0-9]+")
            .map(Integer::parseInt);

        // Create references for recursive parsers
        Taker<Integer> expr = Taker.ref();
        Taker<Integer> term = Taker.ref();
        Taker<Integer> factor = Taker.ref();

        // Factor can be a number or an expression in parentheses
        Taker<Integer> parenFactor = chr('(')
            .skipThen(trim(expr))
            .thenSkip(chr(')')); // Using trim from Lexical

        factor.set(
            trim(oneOf(number, parenFactor))
        );

        // Taker for multiplication operator
        Taker<BinaryOperator<Integer>> mulOp = trim(chr('*'))
            .as((a, b) -> a * b);

        // Taker for division operator
        Taker<BinaryOperator<Integer>> divOp = trim(chr('/'))
            .as((a, b) -> a / b);

        // Term handles multiplication and division
        term.set(
            factor.chainLeftZeroOrMore(oneOf(mulOp, divOp), 0)
        );

        // Taker for addition operator
        Taker<BinaryOperator<Integer>> addOp = trim(chr('+'))
            .as(Integer::sum);

        // Taker for subtraction operator
        Taker<BinaryOperator<Integer>> subOp = trim(chr('-'))
            .as((a, b) -> a - b);

        // Expression handles addition and subtraction
        expr.set(
            term.chainLeftZeroOrMore(oneOf(addOp, subOp), 0)
        );

        // Parse and evaluate expressions
        String[] expressions = {
            "2 + 3",
            "2 * 3 + 4",
            "2 + 3 * 4",
            "(2 + 3) * 4",
            "8 / 4 / 2"
        };

        int[] expectedResults = {
            5,    // 2 + 3 = 5
            10,   // 2 * 3 + 4 = 10
            14,   // 2 + 3 * 4 = 14
            20,   // (2 + 3) * 4 = 20
            1     // 8 / 4 / 2 = 1
        };

        for (int i = 0; i < expressions.length; i++) {
            Result<Integer> result = expr.parseAll(Input.of(expressions[i]));
            assertTrue(result.matches(), "Failed to parse: " + expressions[i]);
            assertEquals(expectedResults[i], result.value(), "Incorrect result for: " + expressions[i]);
        }
    }

    /**
     * Simple KeyValue class for testing
     */
    static class KeyValue {
        private final String key;
        private final String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public String getValue() { return value; }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    private static String join(List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            sb.append(item);
        }
        return sb.toString();
    }
}
