package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OnlyIfTest {

    @Test
    public void testOnlyIfWithCharPredicate() {
        Parser<Character> parser = Lexical.chr('a').onlyIf(CharPredicate.is('a'));
        Result<Character> result = parser.parse("a");
        assertTrue(result.matches(), "Should match 'a'");
        assertEquals('a', result.value());

        result = parser.parse("b");
        assertFalse(result.matches(), "Should not match 'b' because of onlyIf");
    }

    @Test
    public void testOnlyIfEOF() {
        Parser<Character> parser = Lexical.chr('a').onlyIf(CharPredicate.is('a'));
        Result<Character> result = parser.parse("");
        assertFalse(result.matches(), "Should not match EOF");
    }
}
