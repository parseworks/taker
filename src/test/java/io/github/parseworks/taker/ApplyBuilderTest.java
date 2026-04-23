package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Lexical.regex;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplyBuilderTest {

    // Test two-parser combination
    @Test
    public void testTwoParserCombination() {
        Taker<String> p1 = stringParser("hello");
        Taker<String> p2 = stringParser("world");
        
        Taker<String> combined = p1.then(p2)
            .map((s1, s2) -> s1 + " " + s2);
        
        Result<String> result = combined.parseAll("helloworld");
        assertTrue(result.matches());
        assertEquals("hello world", result.value());
    }
    
    // Test three-parser combination
    @Test
    public void testThreeParserCombination() {
        Taker<String> p1 = stringParser("a");
        Taker<String> p2 = stringParser("b");
        Taker<String> p3 = stringParser("c");
        
        Taker<String> combined = p1.then(p2).then(p3)
            .map((a, b, c) -> a + b + c);
        
        Result<String> result = combined.parseAll("abc");
        assertTrue(result.matches());
        assertEquals("abc", result.value());
    }
    
    // Test four-parser combination
    @Test
    public void testFourParserCombination() {
        Taker<Integer> p1 = intParser(1);
        Taker<Integer> p2 = intParser(2);
        Taker<Integer> p3 = intParser(3);
        Taker<Integer> p4 = intParser(4);
        
        Taker<Integer> combined = p1.then(p2).then(p3).then(p4)
            .map((a, b, c, d) -> a + b + c + d);
        
        Result<Integer> result = combined.parse("1234");
        assertTrue(result.matches());
        assertEquals(10, result.value()); // 1+2+3+4 = 10
    }
    
    // Test five-parser combination
    @Test
    public void testFiveParserCombination() {
        Taker<Integer> p1 = intParser(1);
        Taker<Integer> p2 = intParser(2);
        Taker<Integer> p3 = intParser(3);
        Taker<Integer> p4 = intParser(4);
        Taker<Integer> p5 = intParser(5);
        
        Taker<Integer> combined = p1.then(p2).then(p3).then(p4).then(p5)
            .map((a, b, c, d, e) -> a + b + c + d + e);
        
        Result<Integer> result = combined.parse("12345");
        assertTrue(result.matches());
        assertEquals(15, result.value()); // 1+2+3+4+5 = 15
    }
    
    // Test six-parser combination
    @Test
    public void testSixParserCombination() {
        Taker<Integer> p1 = intParser(1);
        Taker<Integer> p2 = intParser(2);
        Taker<Integer> p3 = intParser(3);
        Taker<Integer> p4 = intParser(4);
        Taker<Integer> p5 = intParser(5);
        Taker<Integer> p6 = intParser(6);
        
        Taker<Integer> combined = p1.then(p2).then(p3).then(p4).then(p5).then(p6)
            .map((a, b, c, d, e, f) -> a + b + c + d + e + f);
        
        Result<Integer> result = combined.parse("123456");
        assertTrue(result.matches());
        assertEquals(21, result.value()); // Sum = 21
    }
    
    // Test seven-parser combination
    @Test
    public void testSevenParserCombination() {
        Taker<String> p1 = stringParser("a");
        Taker<String> p2 = stringParser("b");
        Taker<String> p3 = stringParser("c");
        Taker<String> p4 = stringParser("d");
        Taker<String> p5 = stringParser("e");
        Taker<String> p6 = stringParser("f");
        Taker<String> p7 = stringParser("g");
        
        Taker<String> combined = p1.then(p2).then(p3).then(p4).then(p5).then(p6).then(p7)
            .map((a, b, c, d, e, f, g) -> a + b + c + d + e + f + g);
        
        Result<String> result = combined.parse("abcdefg");
        assertTrue(result.matches());
        assertEquals("abcdefg", result.value());
    }
    
    // Test eight-parser combination
    @Test
    public void testEightParserCombination() {
        Taker<String> p1 = stringParser("1");
        Taker<String> p2 = stringParser("2");
        Taker<String> p3 = stringParser("3");
        Taker<String> p4 = stringParser("4");
        Taker<String> p5 = stringParser("5");
        Taker<String> p6 = stringParser("6");
        Taker<String> p7 = stringParser("7");
        Taker<String> p8 = stringParser("8");
        
        Taker<String> combined = p1.then(p2).then(p3).then(p4).then(p5).then(p6).then(p7).then(p8)
            .map((a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h);
        
        Result<String> result = combined.parse("12345678");
        assertTrue(result.matches());
        assertEquals("12345678", result.value());
    }
    
    // Test with mixed types
    @Test
    public void testMixedTypesCombination() {
        Taker<String> p1 = stringParser("user");
        Taker<Character> p2 = charParser(':');
        Taker<Integer> p3 = intParser(42);
        
        Taker<String> combined = p1.then(p2).then(p3)
            .map((user, colon, id) -> user + id);
        
        Result<String> result = combined.parse("user:42");
        assertTrue(result.matches());
        assertEquals("user42", result.value());
    }
    
    // Test parsing failure
    @Test
    public void testParsingFailure() {
        Taker<String> p1 = stringParser("hello");
        Taker<String> p2 = stringParser("world");
        
        Taker<String> combined = p1.then(p2)
            .map((s1, s2) -> s1 + " " + s2);
        
        Result<String> result = combined.parse("helloplanet");
        assertTrue(!result.matches());
    }
    
    // Test with incomplete input
    @Test
    public void testIncompleteInput() {
        Taker<String> p1 = stringParser("hello");
        Taker<String> p2 = stringParser("world");
        
        Taker<String> combined = p1.then(p2)
            .map((s1, s2) -> s1 + " " + s2);
        
        Result<String> result = combined.parse("hello");
        assertTrue(!result.matches());
    }
    
    // Test real-world scenario: parsing a simple assignment statement
    @Test
    public void testAssignmentParsing() {
        Taker<String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
        Taker<Character> equals = charParser('=');
        Taker<Integer> number = regexIntParser("[0-9]+");
        Taker<Character> semicolon = charParser(';');
        
        // Parse "x = 42;"
        Taker<Assignment> assignmentParser = trim(identifier).then(equals)
            .then(trim(number)).then(semicolon)
            .map((name, eq, value, semi) -> new Assignment(name, value));
        
        Result<Assignment> result = assignmentParser.parseAll("myVar = 42;");
        assertTrue(result.matches());
        Assignment assignment = result.value();
        assertEquals("myVar", assignment.name);
        assertEquals(42, assignment.value);
    }
    
    // Helper class for assignment test
    private static class Assignment {
        final String name;
        final int value;
        
        Assignment(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
    
    // Helper parser methods
    private Taker<Character> charParser(char expected) {
        return new Taker<>(input -> {
            if (input.isEof() || input.current() != expected) {
                return new NoMatch<>(input, String.valueOf(expected));
            }
            return new Match<>(expected, input.next());
        });
    }
    
    private Taker<String> stringParser(String expected) {
        return new Taker<>(input -> {
            Input current = input;
            for (int i = 0; i < expected.length(); i++) {
                if (current.isEof() || current.current() != expected.charAt(i)) {
                    return new NoMatch<>(input, expected);
                }
                current = current.next();
            }
            return new Match<>(expected, current);
        });
    }
    
    private Taker<Integer> intParser(int value) {
        return stringParser(Integer.toString(value)).map(s -> value);
    }

    
    private Taker<Integer> regexIntParser(String pattern) {
        return regex(pattern).map(Integer::parseInt);
    }
}