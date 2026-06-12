package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.result.PartialMatch;
import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Combinators.commit;
import static org.junit.jupiter.api.Assertions.*;

public class PartialMatchTest {

    @Test
    public void testPartialMatchInParse() {
        Taker<String> abc = Lexical.string("abc");
        Input input = Input.of("abcdef");

        // parse(input, false) should return a Match
        Result<String> result1 = abc.parse(input, false);
        assertTrue(result1.matches());
        assertNotSame(ResultType.PARTIAL, result1.type());
        assertEquals("abc", result1.value());
        assertEquals(3, result1.input().position());

        // parse(input, true) should return a PartialMatch now (instead of NoMatch)
        Result<String> result2 = abc.parse(input, true);
        assertFalse(result2.matches());
        assertEquals(ResultType.PARTIAL, result2.type());
        // value() should throw exception for PartialMatch
        assertThrows(RuntimeException.class, result2::value);
        // It still advanced the index to 3
        assertEquals(3, result2.input().position());
    }

    @Test
    public void testFullMatchInParse() {
        Taker<String> abc = Lexical.string("abc");
        Input input = Input.of("abc");

        Result<String> result = abc.parse(input, true);
        assertTrue(result.matches());
        assertNotSame(ResultType.PARTIAL, result.type());
        assertEquals("abc", result.value());
        assertTrue(result.input().isEof());
    }


    @Test
    public void testNoStringMatch() {
        Taker<String> abcd = Lexical.string("abcd");
        Input input = Input.of("xyz");

        Result<String> result = abcd.parse(input);
        assertFalse(result.matches());
        assertNotSame(ResultType.PARTIAL, result.type());
        // value() should throw exception for NoMatch
        assertThrows(RuntimeException.class, result::value);
    }

    @Test
    public void testPartialMatch() {
        Taker<String> abcd = commit(Lexical.string("abcd"));
        Input input = Input.of("abc");
        
        Result<String> result = abcd.parse(input);
        
        assertEquals(ResultType.PARTIAL, result.type());
        PartialMatch<String> partial = (PartialMatch<String>) result;
        //TODO:assertEquals(3, partial.input().position());
    }

    @Test
    public void testNoCommitBacktrack() {
        Taker<String> abcd = Lexical.string("abcd");
        Input fullInput = Input.of("prefixabcd");
        Input startInput = fullInput.skip(6); // at 'a'

        // Input is "prefixabcX"
        Input testInput = Input.of("prefixabcX").skip(6);

        Result<String> result = abcd.apply(testInput);

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        // Lexical.string returns NoMatch but at the failing position (consumed input)
        // because it's non-committing, but it reports where it failed.
        assertEquals(9, result.input().position(), "Lexical.string should report where it failed");
    }
}
