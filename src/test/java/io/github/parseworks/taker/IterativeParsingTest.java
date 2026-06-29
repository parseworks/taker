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

package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static io.github.parseworks.taker.parsers.Chars.alpha;
import static org.junit.jupiter.api.Assertions.*;

public class IterativeParsingTest {

    @Test
    public void testIterateParseWithWords() {
        // Parse space-separated words
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        String input = "hello world test";
        Input charInput = Input.of(input);

        Iterator<String> iterator = wordParser.iterateParse(charInput);

        List<String> words = new ArrayList<>();
        while (iterator.hasNext()) {
            words.add(iterator.next());
        }

        assertEquals(3, words.size());
        assertEquals("hello", words.get(0));
        assertEquals("world", words.get(1));
        assertEquals("test", words.get(2));
    }

    @Test
    public void testIterateParseWithNumbers() {
        Taker<Integer> numberParser = Numeric.integer;
        String input = "123 456 789";
        Input charInput = Input.of(input);

        Iterator<Integer> iterator = numberParser.iterateParse( charInput);

        List<Integer> numbers = new ArrayList<>();
        while (iterator.hasNext()) {
            numbers.add(iterator.next());
        }

        assertEquals(3, numbers.size());
        assertEquals(123, numbers.get(0));
        assertEquals(456, numbers.get(1));
        assertEquals(789, numbers.get(2));
    }

    @Test
    public void testStreamParseWithWords() {
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        String input = "hello world test";
        Input charInput = Input.of(input);

        List<String> words = wordParser.stream(charInput)
            .toList();

        assertEquals(3, words.size());
        assertEquals("hello", words.get(0));
        assertEquals("world", words.get(1));
        assertEquals("test", words.get(2));
    }

    @Test
    public void testEmptyInput() {
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        Input charInput = Input.of("");
        Iterator<String> iterator = wordParser.iterateParse(charInput);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorNoSuchElementException() {
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        Input charInput = Input.of("test");
        Iterator<String> iterator = wordParser.iterateParse(charInput);

        assertTrue(iterator.hasNext());
        assertEquals("test", iterator.next());
        assertFalse(iterator.hasNext());

        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    public void testParserWithErrors() {
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        // Input with numbers between words - should skip over them
        String input = "hello123world456test";
        Input charInput = Input.of(input);

        List<String> words = wordParser.stream(charInput)
            .toList();

        assertEquals(3, words.size());
        assertEquals("hello", words.get(0));
        assertEquals("world", words.get(1));
        assertEquals("test", words.get(2));
    }

    @Test
    public void testMultipleCallsToHasNext() {
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        Input charInput = Input.of("test");
        Iterator<String> iterator = wordParser.iterateParse(charInput);

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext()); // Multiple calls should return the same result
        assertEquals("test", iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext()); // Multiple calls should return the same result
    }

    @Test
    public void testParallelStreamProcessing() {
        Taker<String> wordParser = alpha.oneOrMore().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        String input = "one two three four five";
        Input charInput = Input.of(input);

        List<String> words = wordParser.stream(charInput)
            .parallel()
            .map(String::toUpperCase)
            .toList();

        assertEquals(5, words.size());
        assertTrue(words.contains("ONE"));
        assertTrue(words.contains("TWO"));
        assertTrue(words.contains("THREE"));
        assertTrue(words.contains("FOUR"));
        assertTrue(words.contains("FIVE"));
    }
}
