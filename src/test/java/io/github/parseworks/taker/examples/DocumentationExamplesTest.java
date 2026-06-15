package io.github.parseworks.taker.examples;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Combinators.any;
import static io.github.parseworks.taker.parsers.Combinators.not;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.collectChars;
import static io.github.parseworks.taker.parsers.Lexical.skipWhile;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static io.github.parseworks.taker.parsers.Numeric.integer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationExamplesTest {

    @Test
    void readmeQuickStartExampleWorks() {
        Result<Integer> result = integer.parse("-123");

        assertTrue(result.matches());
        assertEquals(-123, result.value());
    }

    @Test
    void readmeKeyValueExampleWorks() {
        Taker<String> identifier = collectChars(CharPredicate.asciiLetterOrDigit);
        Taker<String> kvPair = identifier
            .thenSkip(trim(chr(':')))
            .then(collectChars(CharPredicate.lineBreak.negate()))
            .map((key, value) -> key + " => " + value);

        assertEquals("User => Bob", kvPair.parse("User: Bob").value());
    }

    @Test
    void readmeScannerPrimitiveExampleWorks() {
        Taker<String> word = collectChars(CharPredicate.asciiLetter);
        Taker<Void> spaces = skipWhile(CharPredicate.horizontalWhitespace);
        Taker<String> slower = chr(CharPredicate.asciiLetter).collectString();

        assertEquals("hello", word.parse("hello  ").value());
        assertTrue(spaces.parse("   next").matches());
        assertEquals("hello", slower.parse("hello  ").value());
    }

    @Test
    void readmeRecursiveGrammarExampleWorks() {
        Taker<String> nested = Taker.ref();
        Taker<String> atom = chr(CharPredicate.asciiLetter).collectString();
        Taker<String> grouped = chr('(')
            .skipThen(nested)
            .thenSkip(chr(')'));

        nested.set(oneOf(atom, grouped));

        assertEquals("hello", nested.parse("((hello))").value());
    }

    @Test
    void userGuideStructuredDataExampleWorks() {
        Taker<String> identifier = collectChars(CharPredicate.asciiLetterOrDigit)
            .expecting("identifier");
        Taker<String> lineValue = collectChars(CharPredicate.lineBreak.negate())
            .expecting("value");

        Taker<KV> kvParser = identifier
            .thenSkip(chr('='))
            .then(lineValue)
            .map(KV::new);

        Taker<List<KV>> configParser = kvParser.oneOrMoreSeparatedBy(chr('\n'));

        List<KV> config = configParser.parseAll("server=localhost\nport=8080").value();

        assertEquals(List.of(new KV("server", "localhost"), new KV("port", "8080")), config);
    }

    @Test
    void userGuideCalculatorExampleWorks() {
        Taker<Integer> expr = Taker.ref();
        Taker<Integer> term = Taker.ref();
        Taker<Integer> factor = Taker.ref();

        Taker<Integer> parenFactor = chr('(')
            .skipThen(trim(expr))
            .thenSkip(chr(')'));

        factor.set(trim(integer.or(parenFactor)));

        Taker<BinaryOperator<Integer>> mulOp = trim(chr('*')).as((a, b) -> a * b);
        Taker<BinaryOperator<Integer>> divOp = trim(chr('/')).as((a, b) -> a / b);
        term.set(factor.chainLeftOneOrMore(oneOf(mulOp, divOp)).or(factor));

        Taker<BinaryOperator<Integer>> addOp = trim(chr('+')).as(Integer::sum);
        Taker<BinaryOperator<Integer>> subOp = trim(chr('-')).as((a, b) -> a - b);
        expr.set(term.chainLeftOneOrMore(oneOf(addOp, subOp)).or(term));

        assertEquals(20, expr.parseAll("(2 + 3) * 4").value());
    }

    @Test
    void advancedGuideRecursiveTroubleshootingExampleWorks() {
        Taker<String> goodRecursion = Taker.ref();
        goodRecursion.set(
            string("x")
                .then(goodRecursion.optional())
                .map((head, tail) -> head + tail.orElse(""))
        );

        assertEquals("xxx", goodRecursion.parseAll("xxx").value());
    }

    @Test
    void advancedGuideNegationExampleWorks() {
        Taker<Character> notDigit = not(chr(CharPredicate.asciiDigit)).skipThen(any());

        Result<Character> letter = notDigit.parse("x");
        Result<Character> digit = notDigit.parse("5");

        assertTrue(letter.matches());
        assertEquals('x', letter.value());
        assertFalse(digit.matches());
    }

    record KV(String key, String value) {
    }
}
