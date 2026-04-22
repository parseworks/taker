package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Parser.expecting(String) combinator.
 */
public class ExpectingCombinatorTest {

    @Test
    public void expectingRelabelsFailure() {
        // Simple identifier: first a letter, then letters or digits
        // Using regex to avoid extra mapping helpers
        Parser<String> identifier = Lexical.regex("[A-Za-z][A-Za-z0-9]*")
                .expecting("identifier");

        Result<String> r = identifier.parse("123");
        assertTrue(!r.matches(), "Parser should fail on input that doesn't start with a letter");
        String msg = r.error();
        assertTrue(msg.toLowerCase().contains("expected identifier"),
                () -> "Error message should contain relabeled expectation, but was:\n" + msg);
    }

    @Test
    public void expectingIsNoOpOnSuccess() {
        Parser<String> p = Lexical.string("abc").expecting("ABC");
        Result<String> r = p.parse("abc");
        assertTrue(r.matches(), "Parser should succeed");
        assertEquals("abc", r.value());
    }

    @Test
    public void expectingWithinCompositeParser() {
        // key '=' value where '=' is labeled for a clearer message
        Parser<String> key = Lexical.regex("[A-Za-z]+");
        Parser<Character> equalsLabeled = Lexical.chr('=')
                .expecting("'=' after key");
        Parser<String> value = Lexical.regex("[A-Za-z0-9]+");

        Parser<String> pair = key
                .thenSkip(equalsLabeled)
                .then(value)
                .map(k -> v -> k + "=" + v);

        // Missing '=' should fail and include our labeled expectation
        Result<String> r = pair.parse("nameJohn");
        assertTrue(!r.matches(), "Parser should fail when '=' is missing");
        String msg = r.error();
        assertTrue(msg.toLowerCase().contains("expected '=' after key"),
                () -> "Composite parser error should include labeled expectation, but was:\n" + msg);
    }
}
