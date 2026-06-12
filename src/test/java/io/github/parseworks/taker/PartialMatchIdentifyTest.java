package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.parseworks.taker.parsers.Combinators.commit;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartialMatchIdentifyTest {

    @Test
    public void testStringPartialMatch() {
        Taker<String> abc = commit(Lexical.string("abc"));
        Input input = Input.of("abd"); // matches 'ab', fails on 'c' vs 'd'
        
        Result<String> result = abc.apply(input);
        
        // If it returns PARTIAL, it means it started matching but failed.
        // This is useful for debugging and for 'oneOf' to know it shouldn't try other options if it's a hard failure.
        assertEquals(ResultType.PARTIAL, result.type(), "Combinators.commit(Lexical.string) should return PARTIAL on partial match");
    }

    @Test
    public void testStringNoPartialMatchByDefault() {
        Taker<String> abc = Lexical.string("abc");
        Input input = Input.of("abd");

        Result<String> result = abc.apply(input);

        assertEquals(ResultType.NO_MATCH, result.type(), "Lexical.string should NOT return PARTIAL by default");
    }

    @Test
    public void testSequencePartialMatch() {
        Taker<Character> a = Lexical.chr('a');
        Taker<Character> b = Lexical.chr('b');
        Taker<Character> c = Lexical.chr('c');
        
        Taker<Character> abc = commit(a.skipThen(b).skipThen(c));
        Input input = Input.of("abd");
        
        Result<Character> result = abc.apply(input);
        
        // a matches, b matches, c fails. 
        assertEquals(ResultType.PARTIAL, result.type(), "Committed Sequence should return PARTIAL if a later element fails");
    }
    
    @Test
    public void testRepeatPartialMatch() {
        Taker<Character> a = Lexical.chr('a');
        Taker<List<Character>> a3 = commit(a.repeat(3));
        Input input = Input.of("aab");
        
        Result<List<Character>> result = a3.apply(input);
        
        // Matches 'a' twice, fails on third 'a'.
        assertEquals(ResultType.PARTIAL, result.type(), "Committed Repeat should return PARTIAL if it met min requirement but failed subsequently");
    }
}
