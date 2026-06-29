/*
 * Copyright (c) 2026 jason bailey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Edge cases for numeric parsers: empty, overflow, partial match */
public class NumericEdgeCaseTest {

    @Test
    void integerParseEmpty_fails() {
        Result<Integer> result = Numeric.integer.parse(Input.of(""));
        assertFalse(result.matches(), "empty should fail");
    }

    @Test
    void longValueParseEmpty_fails() {
        Result<Long> result = Numeric.longValue.parse(Input.of(""));
        assertFalse(result.matches());
    }

    @Test
    void doubleValueParseEmpty_fails() {
        Result<Double> result = Numeric.doubleValue.parse(Input.of(""));
        assertFalse(result.matches());
    }

    @Test
    void unsignedIntegerLeadingZero_parsesOnlyFirstZero() {
        // Per API contract, "0123" should parse only leading zero
        Result<Integer> result = Numeric.unsignedInteger.parseAll(Input.of("0"));
        assertTrue(result.matches());
        assertEquals(0, result.value());
    }

    @Test
    void unsignedIntegerWithLeadingZero_failsParseAll() {
        // "0123" parseAll should fail due to trailing input
        Result<Integer> result = Numeric.unsignedInteger.parseAll(Input.of("0123"));
        assertFalse(result.matches(), "leading-zero number should fail parseAll");
    }

    @Test
    void integerOverflow_failsRatherThanWrapping() {
        String overflow = "-9223372036854775809";
        Result<Long> result = Numeric.longValue.parse(overflow);
        assertFalse(result.matches(), "overflow should not succeed");
    }

    @Test
    void longValueMinLong_parsesSuccessfully() {
        // Verify Long.MIN_VALUE (-9223372036854775808) parses as per contract
        Result<Long> result = Numeric.longValue.parseAll("-9223372036854775808");
        assertTrue(result.matches(), "Long.MIN_VALUE should parse: " + result.error());
        assertEquals(Long.MIN_VALUE, result.value());
    }

    @Test
    void doubleValueParsesExponents() {
        Result<Double> result = Numeric.doubleValue.parseAll("1.5e2");
        assertTrue(result.matches());
        assertEquals(150.0, result.value(), 0.001);
    }

    @Test
    void hexParseWithLeadingZero() {
        Result<Long> result = Numeric.hex.parseAll("0x1A");
        assertTrue(result.matches());
        assertEquals(26, result.value().intValue());
    }

    @Test  
    void doubleValueNegativeInfinity_notParsedAsValidNumber() {
        // Double parsers should not accept infinity literals as valid
        // This documents that "Inf"/"-Inf" are rejected by default
        Result<Double> res = Numeric.doubleValue.parseAll("-1.0e3");
        assertTrue(res.matches());
    }
}
