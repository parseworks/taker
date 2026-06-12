package io.github.parseworks.taker;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Combinators.commit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AtomicPartialMatchTest {

    @Test
    public void testNoCommitByDefault() {
        // string("abc") will produce a NoMatch (but consumed input) on input "abx"
        Taker<String> p = string("abc");
        Input input = Input.of("abx");

        Result<String> result = p.apply(input);

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type(), "string should return a NoMatch by default");
        assertEquals(2, result.input().position(), "string should report how far it matched");
    }

    @Test
    public void testCommitWithPartialMatch() {
        // commit(string("abc")) will produce a PartialMatch on input "abx"
        Taker<String> p = commit(string("abc"));
        Input input = Input.of("abx");

        Result<String> result = p.apply(input);

        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type(), "Committed parser should return a PartialMatch");
        assertEquals(2, result.input().position(), "PartialMatch should report how far it matched");
    }
}
