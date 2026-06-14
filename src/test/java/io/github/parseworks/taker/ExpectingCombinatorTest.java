package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Taker.expecting(String) combinator.
 */
public class ExpectingCombinatorTest {

    @Test
    public void expectingRelabelsFailure() {
        // Simple identifier: first a letter, then letters or digits
        // Using regex to avoid extra mapping helpers
        Taker<String> identifier = Lexical.regex("[A-Za-z][A-Za-z0-9]*")
                .expecting("identifier");

        Result<String> r = identifier.parse("123");
        assertFalse(r.matches(), "Taker should fail on input that doesn't start with a letter");
        String msg = r.error();
        assertTrue(msg.toLowerCase().contains("expected identifier"),
                () -> "Error message should contain relabeled expectation, but was:\n" + msg);
    }

    @Test
    public void expectingIsNoOpOnSuccess() {
        Taker<String> p = Lexical.string("abc").expecting("ABC");
        Result<String> r = p.parse("abc");
        assertTrue(r.matches(), "Taker should succeed");
        assertEquals("abc", r.value());
    }

    @Test
    public void expectingWithinCompositeParser() {
        // key '=' value where '=' is labeled for a clearer message
        Taker<String> key = Lexical.regex("[A-Za-z]+");
        Taker<Character> equalsLabeled = Lexical.chr('=')
                .expecting("'=' after key");
        Taker<String> value = Lexical.regex("[A-Za-z0-9]+");

        Taker<String> pair = key
                .thenSkip(equalsLabeled)
                .then(value)
                .map(k -> v -> k + "=" + v);

        // Missing '=' should fail and include our labeled expectation
        Result<String> r = pair.parse("nameJohn");
        assertFalse(r.matches(), "Taker should fail when '=' is missing");
        String msg = r.error();
        assertTrue(msg.toLowerCase().contains("expected '=' after key"),
                () -> "Composite parser error should include labeled expectation, but was:\n" + msg);
    }

    @Test
    public void labelAddsGrammarContextAndPreservesCause() {
        Taker<String> identifier = Lexical.string("name").label("identifier");
        Taker<String> assignment = identifier
                .thenSkip(Lexical.chr('='))
                .then(identifier)
                .map(k -> v -> k + "=" + v)
                .label("assignment");

        Result<String> result = assignment.parse("name:x");

        assertFalse(result.matches(), "Taker should fail when assignment syntax is missing '='");
        Failure<?> failure = (Failure<?>) result;
        assertEquals("assignment", failure.expected());
        assertTrue(failure.context());
        assertNotNull(failure.cause());
        assertTrue(failure.error().contains("while parsing assignment"));
        assertTrue(failure.error().contains("expected '='"));
    }

    @Test
    public void labelIsNoOpOnSuccess() {
        Taker<String> parser = Lexical.string("abc").label("letters");
        Result<String> result = parser.parse("abc");

        assertTrue(result.matches(), "Taker should succeed");
        assertEquals("abc", result.value());
    }
}
