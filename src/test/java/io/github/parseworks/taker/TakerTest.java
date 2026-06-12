package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import io.github.parseworks.taker.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Combinators.isNot;
import static io.github.parseworks.taker.parsers.Combinators.not;
import static io.github.parseworks.taker.parsers.Lexical.*;
import static io.github.parseworks.taker.parsers.Numeric.numeric;
import static org.junit.jupiter.api.Assertions.*;

public class TakerTest {

    @Test
    public void testRecursiveParser() {
        Taker<String> expr = Taker.ref();
        Taker<String> atom = chr(Character::isLetter).collectString();
        Taker<String> grouped = expr.between('(', ')');
        expr.set(atom.or(grouped));

        assertEquals("a", expr.parse("a").value());
        assertEquals("a", expr.parse("(a)").value());
        assertEquals("a", expr.parse("((a))").value());
        assertFalse(expr.parse("(((a))").matches());
    }

    @Test
    public void testBetweenSameBracket() {
        // Create a parser for content (letters)
        Taker<String> content = chr(Character::isLetter).collectString();

        // Create a parser that parses content between matching quote characters
        Taker<String> quotedParser = content.between('"');

        // Test with properly quoted input
        String test = "hello";
        Result<String> result = quotedParser.parse("\"" + test + "\"");

        assertTrue(result.matches());
        assertEquals(test, result.value());

        // Test with mismatched or missing quotes
        Result<String> resultMissingClosing = quotedParser.parse("\"" + test);
        assertTrue(!resultMissingClosing.matches());

        Result<String> resultMissingOpening = quotedParser.parse(test + "\"");
        assertTrue(!resultMissingOpening.matches());

        Result<String> resultNoQuotes = quotedParser.parse(test);
        assertTrue(!resultNoQuotes.matches());
    }

    @Test
    public void testBetweenWithParsers() {
        Taker<Character> open = chr('[');
        Taker<Character> close = chr(']');
        Taker<Character> content = chr('a');

        Taker<Character> parser = content.between(open, close);
        Result<Character> result = parser.parse("[a]");

        assertTrue(result.matches());
        assertEquals('a', result.value());
    }

    @Test
    public void testAs() {
        Taker<String> parser = chr('a').as("constant");
        Result<String> result = parser.parse("a");

        assertTrue(result.matches());
        assertEquals("constant", result.value());
    }

    @Test
    public void testMap() {
        Taker<Integer> parser = chr('a').map(c -> (int)c);
        Result<Integer> result = parser.parse("a");

        assertTrue(result.matches());
        assertEquals(Integer.valueOf('a'), result.value());
    }

    @Test
    public void testFlatMapDependentCount() {
        Taker<String> parser = Numeric.unsignedInteger.flatMap(n ->
            chr(',').skipThen(chr('a').repeat(n)).map(Lists::join)
        );

        Result<String> result = parser.parse("3,aaa");
        assertTrue(result.matches());
        assertEquals("aaa", result.value());
    }

    @Test
    public void testMultipleThen() {
        Taker<Character> a = chr('a');
        Taker<Character> b = chr('b');
        Taker<Character> c = chr('c');

        Taker<String> parser = a.then(b).then(c)
                .map((first, second, third) -> String.valueOf(first) + second + third);

        Result<String> result = parser.parse("abc");
        assertTrue(result.matches());
        assertEquals("abc", result.value());
    }


    @Test
    public void testZeroOrMore() {
        Taker<List<Character>> parser = Lexical.chr(Character::isLetter).zeroOrMore().then(Lexical.chr(Character::isDigit).zeroOrMore()).map(Lists::appendAll);
        Input input = Input.of("abc123");
        Result<List<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(6, result.value().size());
    }

    @Test
    public void testChainr1() {
        Taker<Long> number = Numeric.number;
        Taker<BinaryOperator<Long>> plus = chr('+').map(op -> Long::sum);
        Taker<Long> parser = number.chainRightOneOrMore(plus);
        Input input = Input.of("1+2+3");
        Result<Long> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(6, result.value());
    }


    @Test
    public void testBetween() {
        Taker<String> content = chr(Character::isLetter).oneOrMore().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });
        String test = "compute";
        Taker<String> parser = content.between('(', ')');
        Input input = Input.of("(" + test + ")");
        Result<String> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(test, result.value());
    }

    @Test
    public void testChainl() {
        Taker<Long> number = Numeric.number;
        Taker<BinaryOperator<Long>> plus = chr('-').map(op -> (a, b) -> a - b);
        Taker<Long> parser = number.chainLeftOneOrMore(plus);
        Input input = Input.of("1-2-3");
        Result<Long> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(-4, result.value());
    }

    // Note: additional chain-right tests pruned; behavior covered by testChainr1 and AssociativityTest

    @Test
    public void testZeroOrMoreSeparatedBy() {
        Taker<List<Character>> parser = chr(Character::isLetter).zeroOrMoreSeparatedBy(chr(','));
        Input input = Input.of("a,b,c");
        Result<List<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testBetweenDifferentContent() {
        Taker<Character> open = chr('[');
        Taker<Character> close = chr(']');
        Taker<String> content = chr(Character::isLetter).oneOrMore().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });
        String test = "example";
        Taker<String> parser = content.between(open, close);
        Input input = Input.of("[" + test + "]");
        Result<String> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(test, result.value());
    }

    @Test
    public void testRepeat() {
        Taker<List<Character>> parser = chr('a').repeat(3);
        Input input = Input.of("aaa");
        Result<List<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testRepeatAtLeast() {
        Taker<List<Character>> parser = chr('a').repeatAtLeast(2);
        Input input = Input.of("aaa");
        Result<List<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testRepeatBetween() {
        Taker<List<Character>> parser = chr('a').repeat(2, 4);
        Input input = Input.of("aaa");
        Result<List<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testRepeatAtMost() {
        Taker<List<Character>> parser = chr('a').repeatAtMost(3);

        // Test case 1: Less than max
        Result<List<Character>> result1 = parser.parse("aa");
        assertTrue(result1.matches());
        assertEquals(2, result1.value().size());

        // Test case 2: Exactly max
        Result<List<Character>> result2 = parser.parse("aaa");
        assertTrue(result2.matches());
        assertEquals(3, result2.value().size());

        // Test case 3: More than max (should only take max)
        Result<List<Character>> result3 = parser.parse("aaaaa");
        assertTrue(result3.matches());
        assertEquals(3, result3.value().size());

        // Test case 4: Zero matches
        Result<List<Character>> result4 = parser.parse("bbb");
        assertTrue(result4.matches());
        assertEquals(0, result4.value().size());
    }

    @Test
    public void testZeroOrMoreUntil() {
        Taker<List<Character>> parser = chr('a').zeroOrMoreUntil(chr(';'));

        // Test case 1: Zero matches with terminator
        Result<List<Character>> result1 = parser.parse(";");
        assertTrue(result1.matches());
        assertEquals(0, result1.value().size());

        // Test case 2: Multiple matches with terminator
        Result<List<Character>> result2 = parser.parse("aaa;");
        assertTrue(result2.matches());
        assertEquals(3, result2.value().size());

        // Test case 3: No terminator (should fail)
        Result<List<Character>> result3 = parser.parse("aaa");
        assertTrue(!result3.matches());
    }

    @Test
    public void testOneOrManyUntil() {
        Taker<List<Character>> parser = chr('a').oneOrMoreUntil(chr(';'));

        // Test case 1: Multiple matches with terminator
        Result<List<Character>> result1 = parser.parse("aaa;");
        assertTrue(result1.matches());
        assertEquals(3, result1.value().size());

        // Test case 2: Zero matches with terminator (should fail)
        Result<List<Character>> result2 = parser.parse(";");
        assertTrue(!result2.matches());
    }

    @Test
    public void testThenSkipAndSkipThen() {
        // Test thenSkip - keep first result, skip second
        Taker<Character> thenSkipParser = chr('a').thenSkip(chr('b'));
        Result<Character> result1 = thenSkipParser.parse("ab");
        assertTrue(result1.matches());
        assertEquals('a', result1.value());

        // Test skipThen - skip first result, keep second
        Taker<Character> skipThenParser = chr('a').skipThen(chr('b'));
        Result<Character> result2 = skipThenParser.parse("ab");
        assertTrue(result2.matches());
        assertEquals('b', result2.value());
    }

    @Test
    public void testIsNot() {
        Taker<Character> parser = chr(Character::isLetter).onlyIf(isNot('b'));

        // Should succeed when current character is 'a'
        Result<Character> result1 = parser.parse("a");
        assertTrue(result1.matches());
        assertEquals('a', result1.value());

        // Should fail when current character is 'b'
        Result<Character> result2 = parser.parse("b");
        assertTrue(!result2.matches());
    }

    @Test
    public void testOneOrMoreSeparatedBy() {
        Taker<List<Character>> parser = chr('a').oneOrMoreSeparatedBy(chr(','));

        // Test with multiple separated elements
        Result<List<Character>> result1 = parser.parse("a,a,a");
        assertTrue(result1.matches());
        assertEquals(3, result1.value().size());

        // Test with single element (no separators)
        Result<List<Character>> result2 = parser.parse("a");
        assertTrue(result2.matches());
        assertEquals(1, result2.value().size());

        // Test with no elements (should fail)
        Result<List<Character>> result3 = parser.parse("");
        assertTrue(!result3.matches());
    }

    @Test
    public void testChainZeroOrMore() {
        Taker<Integer> number = Numeric.integer;
        Taker<BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);

        // Test chainLeftZeroOrMany
        Taker<Integer> leftParser = number.chainLeftZeroOrMore(plus, 0);
        Result<Integer> leftResult = leftParser.parse("");
        assertTrue(leftResult.matches());
        assertEquals(0, leftResult.value());  // Should return default value for empty input

        // Test chainRightZeroOrMany
        Taker<Integer> rightParser = number.chainRightZeroOrMore(plus, 0);
        Result<Integer> rightResult = rightParser.parse("");
        assertTrue(rightResult.matches());
        assertEquals(0, rightResult.value());  // Should return the default value for empty input
    }

    @Test
    public void testNot() {
        // Create a parser that recognizes the letter 'a'
        Taker<Character> aParser = chr('a');

        // Create a parser that recognizes digits
        Taker<Character> digitParser = chr(Character::isDigit);

        // Create a parser that recognizes 'a' followed by a non-digit
        Taker<Character> aNotDigitParser = aParser.peek(not(digitParser));

        // Test case 1: Input 'a' - should fail because there's no character after 'a' for not(digitParser) to check
        Result<Character> result1 = aNotDigitParser.parse("a");
        assertFalse(result1.matches());

        // Test case 2: Input '5' - should fail because it doesn't start with 'a'
        Result<Character> result2 = aNotDigitParser.parse("5");
        assertFalse(result2.matches());
        //assertEquals("Taker to fail", result2.fullErrorMessage());

        // Test case 3: Input 'a5' - should fail because '5' is a digit
        Result<Character> result3 = aNotDigitParser.parse("a5");
        assertFalse(result3.matches());

        // Test case 4: Multiple negations - parser that matches 'a' but not 'a' followed by 'b'
        Taker<Character> abParser = chr('a').then(chr('b')).map((a, b) -> a);
        Taker<Character> aNotAbParser = aParser.onlyIf(not(abParser));

        // Should fail on "ab" because abParser succeeds
        Result<Character> result4 = aNotAbParser.parse("ab");
        assertFalse(result4.matches());

        // Should succeed on "ac" because abParser fails
        Result<Character> result5 = aNotAbParser.parse("ac");
        assertTrue(result5.matches());
        assertEquals('a', result5.value());
    }

    @Test
    public void testBetweenParsers() {
        // Define a parser for the bracketed content (digits)
        Taker<Integer> contentParser = numeric.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Taker<Character> openBracketParser = chr('[');
        Taker<Character> closeBracketParser = chr(']');

        // Create a parser that parses content between brackets
        Taker<Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input
        String input = "[5]";
        Integer result = betweenParser.parse(Input.of(input)).value();

        // Verify the result
        assertEquals(5, result);
    }

    @Test
    public void testBetweenParsersEmptyContent() {
        // Define a parser for the bracketed content (digits)
        Taker<Integer> contentParser = numeric.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Taker<Character> openBracketParser = chr('[');
        Taker<Character> closeBracketParser = chr(']');

        // Create a parser that parses content between brackets
        Taker<Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input with empty content
        String input = "[]";
        var result = betweenParser.parse(Input.of(input));

        // Verify the result
        assertFalse(result.matches());
    }

    @Test
    public void testBetweenParsersNonNumericContent() {
        // Define a parser for the bracketed content (digits)
        Taker<Integer> contentParser = numeric.map(Character::getNumericValue);

        // Create a parser that parses content between brackets
        Taker<Integer> betweenParser = contentParser.between('[', ']');

        // Test input with non-numeric content
        String input = "[a]";
        var result = betweenParser.parse(Input.of(input));

        // Verify the result
        assertFalse(result.matches());
    }


    @Test
    public void testOneOrMoreSeparatedByEmptyInput() {
        // Define a parser for comma-separated integers
        Taker<Integer> integerParser = numeric.map(Character::getNumericValue);
        Taker<List<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(chr(','));

        // Test input\
        String input = "fish";
        var result = separatedByManyParser.parse(Input.of(input));


        // Verify the result (avoid detailed error text checks outside error-focused suites)
        assertFalse(result.matches());
    }

    @Test
    public void testOneOrMoreSeparatedBySingleElement() {
        // Define a parser for comma-separated integers
        Taker<Integer> integerParser = numeric.map(Character::getNumericValue);
        Taker<List<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(chr(','));

        // Test input
        String input = "7";
        List<Integer> result = separatedByManyParser.parse(Input.of(input)).value();

        // Verify the result
        assertEquals(List.of(7), result);
    }

    @Test
    public void testOneOrMoreSeparatedByTrailingSeparator() {
        // Define a parser for comma-separated integers
        Taker<Integer> integerParser = numeric.map(Character::getNumericValue);
        Taker<List<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(chr(','));

        // Test input
        String input = "1,2,3,";
        Result<List<Integer>> result = separatedByManyParser.parse(Input.of(input));

        // Verify the result
        assertFalse(result.matches());
    }

    @Test
    public void testOneOrMoreSeparatedByMultipleSeparators() {
        // Define a parser for comma-separated integers
        Taker<Integer> integerParser = numeric.map(Character::getNumericValue);
        Taker<List<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(chr(','));

        // Test input
        String input = "1,,2,3";
        Result<List<Integer>> result = separatedByManyParser.parse(Input.of(input));
        //this would return the list on the case of an optional number.
        // Verify the result
        assertFalse(result.matches(), "result should be an error");
    }

    @Test
    public void testOneOrMoreSeparatedByNonNumericInput() {
        // Define a parser for comma-separated integers
        Taker<Integer> integerParser = numeric.map(Character::getNumericValue);
        Taker<List<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(chr(','));

        // Test input
        String input = "a,b,c";
        Result<List<Integer>> result = separatedByManyParser.parse(Input.of(input));

        // Verify the result
        assertFalse(result.matches());
    }
}
