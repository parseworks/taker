package io.github.parseworks.taker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.parseworks.taker.parsers.Combinators.commit;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepeatInternalIssueTest {

    @Test
    public void testRepeatInternalFailsWhenRepetitionFailsCritically() {
        // Case 1: Complex item fails partially
        Taker<String> ab = commit(string("A").then(string("B")).map(a -> b -> a + b));
        Taker<List<String>> manyAB = ab.oneOrMore();
        
        // Input "ABA" -> "AB" matches, then "A" matches, then "B" fails.
        // The second "ab" fails at position 3 (the end of input).
        // Since it advanced from position 2 to 3, it's a partial match.
        Result<List<String>> res2 = manyAB.parse(Input.of("ABA"));
        
        assertTrue(!res2.matches(), "Should fail because item failed partially");
        assertTrue(res2.error().contains("expected B"), "Error should mention 'B'");

        // Case 2: oneOrMoreUntil without reaching terminator
        Taker<String> simpleAB = string("AB");
        Taker<List<String>> manyABUntilExcl = simpleAB.oneOrMoreUntil(chr('!'));
        
        // Input "ABC" -> "AB" matches, then "C" is not "!" and not "AB".
        // It should fail because "!" was not reached.
        Result<List<String>> res4 = manyABUntilExcl.parse(Input.of("ABC"));
        assertTrue(!res4.matches(), "Should fail because terminator was not reached");
    }
}
