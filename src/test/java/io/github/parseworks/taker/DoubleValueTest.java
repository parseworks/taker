package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Numeric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoubleValueTest {

    @Test
    public void testNegativeDoubleValue() {
        String input = "-123.45";
        Result<Double> result = Numeric.doubleValue.parse(input);
        
        assertTrue(result.matches(), "Taker should match '" + input + "'");
        assertEquals(-123.45, result.value(), 0.000, "Parsed value should be -123.45");
    }
}
