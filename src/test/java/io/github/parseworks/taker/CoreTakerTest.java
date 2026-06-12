package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.github.parseworks.taker.parsers.Combinators.pure;
import static org.junit.jupiter.api.Assertions.*;

public class CoreTakerTest {

    @Test
    public void testBetween() {
        Taker<Character> p = Lexical.chr('a').between('(', ')');
        assertTrue(p.parse("(a)").matches());
        assertFalse(p.parse("a").matches());
        assertFalse(p.parse("(a").matches());
        assertFalse(p.parse("a)").matches());
    }

    @Test
    public void testAs() {
        Taker<String> p = Lexical.chr('a').as("found a");
        Result<String> result = p.parse("a");
        assertTrue(result.matches());
        assertEquals("found a", result.value());
    }

    @Test
    public void testPure() {
        Taker<String> p = pure("pure value");
        Result<String> result = p.parse("anything");
        assertTrue(result.matches());
        assertEquals("pure value", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    public void testOptional() {
        Taker<Optional<Character>> p = Lexical.chr('a').optional();
        Result<Optional<Character>> r1 = p.parse("a");
        assertTrue(r1.matches());
        assertEquals(Optional.of('a'), r1.value());

        Result<Optional<Character>> r2 = p.parse("b");
        assertTrue(r2.matches());
        assertEquals(Optional.empty(), r2.value());
    }

    @Test
    public void testOrElse() {
        Taker<Character> p = Lexical.chr('a').orElse('b');
        assertEquals('a', p.parse("a").value());
        assertEquals('b', p.parse("x").value());
    }

    @Test
    public void testRepeatAtLeast() {
        Taker<List<Character>> p = Lexical.chr('a').repeatAtLeast(2);
        assertFalse(p.parse("a").matches());
        assertTrue(p.parse("aa").matches());
        assertTrue(p.parse("aaa").matches());
        assertEquals(3, p.parse("aaa").value().size());
    }

    @Test
    public void testRepeatAtMost() {
        Taker<List<Character>> p = Lexical.chr('a').repeatAtMost(2);
        assertTrue(p.parse("").matches());
        assertTrue(p.parse("a").matches());
        assertTrue(p.parse("aa").matches());
        Result<List<Character>> res = p.parse("aaa");
        assertTrue(res.matches());
        assertEquals(2, res.value().size());
        assertEquals(2, res.input().position());
    }

    @Test
    public void testExpecting() {
        Taker<Character> p = Lexical.chr('a').expecting("the letter a");
        Result<Character> res = p.parse("b");
        assertFalse(res.matches());
        assertTrue(res.error().contains("the letter a"));
    }

    @Test
    public void testRecover() {
        Taker<Character> p = Lexical.chr('a').recover(Lexical.chr('b'));
        assertEquals('a', p.parse("a").value());
        assertEquals('b', p.parse("b").value());
    }
}
