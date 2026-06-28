package io.github.parseworks.taker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Edge-case tests for Input.skip with negative offsets */
public class InputsNegativeSkipTest {

    @Test
    void skipByZero_returnsSelf() {
        Input in = Input.of("abc");
        assertSame(in, in.skip(0));
    }

    @Test
    void skipByPositive_withinBounds() {
        Input in = Input.of("hello");
        Input after = in.skip(3);
        assertEquals(3, after.position());
        assertEquals('l', after.current());
    }

    @Test
    void skipByPositive_toEof_clampsToLength() {
        Input in = Input.of("hi");
        Input tooFar = in.skip(2);
        assertTrue(tooFar.isEof());
    }

    /** Skip past length should clamp rather than throwing */
    @Test
    void skipPastLength_clampsToEnd() {
        Input in = Input.of("abc");
        Input after = in.skip(50);
        assertEquals(3, after.position());
        assertTrue(after.isEof());
    }

    /** Negative skip should clamp to beginning */
    @Test
    void skipNegative_clampsToBeginning() {
        Input in = Input.of("hello");
        Input after = in.skip(-1);
        assertEquals(0, after.position());
        assertEquals('h', after.current());
    }

    /** Skip by exact negative of current offset returns to start */  
    @Test
    void skipNegativeFromCurrent_returnsToBeginning() {
        Input in = Input.of("hello");
        Input after3 = in.skip(3);
        Input back = after3.skip(-3);
        assertEquals(0, back.position());
        assertEquals('h', back.current());
    }

    /** Negative skip from position 0 should stay at beginning */
    @Test  
    void skipNegativeAtBeginning_staysAtStart() {
        Input in = Input.of("abc");
        Input after = in.skip(-10);
        assertEquals(0, after.position());
    }
}
