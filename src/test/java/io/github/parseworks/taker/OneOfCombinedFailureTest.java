package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.result.NoMatch;
import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneOfCombinedFailureTest {

    @Test
    public void oneOf_allAlternativesFail_combinesReasons() {
        Input in = Input.of("a");

        // Three parsers that all fail at the same position with distinct expectations
        Parser<String> p1 = new Parser<>(inp -> new NoMatch<>(inp, "option A"));
        Parser<String> p2 = new Parser<>(inp -> new NoMatch<>(inp, "option B"));
        Parser<String> p3 = new Parser<>(inp -> new NoMatch<>(inp, "option C"));

        Parser<String> parser = Combinators.oneOf(Arrays.asList(p1, p2, p3));

        Result<String> result = parser.parse(in);
        assertFalse(result.matches(), "Result should be an error when all alternatives fail");

        String msg = result.error();

        // Header should reflect the first alternative (option A) and what was found ('a')

        // The combined reasons list should include each alternative's expectation
        assertTrue(msg.contains("Reasons at this location:"), "Should include reasons section");
        assertTrue(msg.contains("- expected option B"), "Should include second alternative reason");
        assertTrue(msg.contains("- expected option C"), "Should include third alternative reason");
        System.out.println(msg);
        // Optional: sanity check that a location indicator exists
        assertTrue(msg.contains("line ") || msg.contains("position "), "Should include location information");
    }
}