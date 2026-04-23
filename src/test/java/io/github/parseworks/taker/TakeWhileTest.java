package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.CharPredicate.noneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TakeWhileTest {
    @Test
    public void testTakeWhileNoneOf() {
        Taker<String> parser = Lexical.takeWhile(noneOf("=/>\t\n\r\f"));
        Result<String> result = parser.parse(Input.of("div>"));
        assertTrue(result.matches());
        assertEquals("div", result.value());
        assertEquals('>', result.input().current());
    }

    @Test
    public void testInfiniteLoop() {
        Taker<String> emptyParser = Lexical.takeWhile(c -> false);
        // This should not infinite loop
        Result<java.util.List<String>> result = emptyParser.zeroOrMore().parse(Input.of("abc"));
        assertTrue(result.matches());
        assertTrue(result.value().isEmpty());
    }
}
