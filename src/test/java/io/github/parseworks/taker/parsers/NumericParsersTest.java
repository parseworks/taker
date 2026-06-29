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

import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Numeric.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Numeric class.
 * These tests verify that the numeric parsers correctly parse different types of numeric values.
 */
public class NumericParsersTest {

    @Test
    public void testNonZeroDigit() {
        // Test successful parsing of non-zero digits
        for (char c = '1'; c <= '9'; c++) {
            Result<Character> result = nonZeroDigit.parse(String.valueOf(c));
            assertTrue(result.matches());
            assertEquals(c, result.value());
        }

        // Test failure for zero
        Result<Character> zeroResult = nonZeroDigit.parse("0");
        assertFalse(zeroResult.matches());

        // Test failure for non-digit
        Result<Character> nonDigitResult = nonZeroDigit.parse("a");
        assertFalse(nonDigitResult.matches());
    }

    @Test
    public void testNumeric() {
        // Test successful parsing of all digits
        for (char c = '0'; c <= '9'; c++) {
            Result<Character> result = numeric.parse(String.valueOf(c));
            assertTrue(result.matches());
            assertEquals(c, result.value());
        }

        // Test failure for non-digit
        Result<Character> nonDigitResult = numeric.parse("a");
        assertFalse(nonDigitResult.matches());
    }

    @Test
    public void testNumericParsersAreAsciiDigitBased() {
        String arabicIndicOne = "\u0661";

        assertFalse(numeric.parse(arabicIndicOne).matches());
        assertFalse(nonZeroDigit.parse(arabicIndicOne).matches());
        assertFalse(integer.parse(arabicIndicOne).matches());
        assertFalse(longValue.parse(arabicIndicOne).matches());
        assertFalse(doubleValue.parse(arabicIndicOne).matches());
        assertFalse(number.parse(arabicIndicOne).matches());
    }

    @Test
    public void testSign() {
        // Test positive sign
        Result<Boolean> plusResult = sign.parse("+");
        assertTrue(plusResult.matches());
        assertTrue(plusResult.value());

        // Test negative sign
        Result<Boolean> minusResult = sign.parse("-");
        assertTrue(minusResult.matches());
        assertFalse(minusResult.value());

        // Test no sign (default to positive)
        Result<Boolean> noSignResult = sign.parse("123");
        assertTrue(noSignResult.matches());
        assertTrue(noSignResult.value());
    }

    @Test
    public void testUnsignedInteger() {
        // Test zero
        Result<Integer> zeroResult = unsignedInteger.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test single digit
        Result<Integer> singleDigitResult = unsignedInteger.parse("5");
        assertTrue(singleDigitResult.matches());
        assertEquals(5, singleDigitResult.value());

        // Test multiple digits
        Result<Integer> multiDigitResult = unsignedInteger.parse("123");
        assertTrue(multiDigitResult.matches());
        assertEquals(123, multiDigitResult.value());

        // Test max value
        Result<Integer> maxValueResult = unsignedInteger.parse("2147483647");
        assertTrue(maxValueResult.matches());
        assertEquals(Integer.MAX_VALUE, maxValueResult.value());

        // Test overflow
        Result<Integer> overflowResult = unsignedInteger.parse("2147483648");
        assertFalse(overflowResult.matches());

        // Test leading zero followed by other digits (should only parse the zero)
        Result<Integer> leadingZeroResult = unsignedInteger.parse("0123");
        assertTrue(leadingZeroResult.matches());
        assertEquals(0, leadingZeroResult.value());

        // Test failure for non-digit
        Result<Integer> nonDigitResult = unsignedInteger.parse("a");
        assertFalse(nonDigitResult.matches());
    }

    @Test
    public void testInteger() {
        // Test positive integer without sign
        Result<Integer> positiveNoSignResult = integer.parse("123");
        assertTrue(positiveNoSignResult.matches());
        assertEquals(123, positiveNoSignResult.value());

        // Test positive integer with sign
        Result<Integer> positiveWithSignResult = integer.parse("+123");
        assertTrue(positiveWithSignResult.matches());
        assertEquals(123, positiveWithSignResult.value());

        // Test negative integer
        Result<Integer> negativeResult = integer.parse("-123");
        assertTrue(negativeResult.matches());
        assertEquals(-123, negativeResult.value());

        // Test boundary values
        Result<Integer> maxValueResult = integer.parse("2147483647");
        assertTrue(maxValueResult.matches());
        assertEquals(Integer.MAX_VALUE, maxValueResult.value());

        Result<Integer> minValueResult = integer.parse("-2147483648");
        assertTrue(minValueResult.matches());
        assertEquals(Integer.MIN_VALUE, minValueResult.value());

        // Test overflow
        Result<Integer> positiveOverflowResult = integer.parse("2147483648");
        assertFalse(positiveOverflowResult.matches());

        Result<Integer> negativeOverflowResult = integer.parse("-2147483649");
        assertFalse(negativeOverflowResult.matches());

        // Test zero
        Result<Integer> zeroResult = integer.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test negative zero
        Result<Integer> negativeZeroResult = integer.parse("-0");
        assertTrue(negativeZeroResult.matches());
        assertEquals(0, negativeZeroResult.value());

        // Test failure for non-digit
        Result<Integer> nonDigitResult = integer.parse("a");
        assertFalse(nonDigitResult.matches());
    }

    @Test
    public void testUnsignedLong() {
        // Test zero
        Result<Long> zeroResult = unsignedLong.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0L, zeroResult.value());

        // Test single digit
        Result<Long> singleDigitResult = unsignedLong.parse("5");
        assertTrue(singleDigitResult.matches());
        assertEquals(5L, singleDigitResult.value());

        // Test multiple digits
        Result<Long> multiDigitResult = unsignedLong.parse("123456789");
        assertTrue(multiDigitResult.matches());
        assertEquals(123456789L, multiDigitResult.value());

        // Test large number
        Result<Long> largeNumberResult = unsignedLong.parse("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(largeNumberResult.matches());
        assertEquals(Long.MAX_VALUE, largeNumberResult.value());

        // Test overflow
        Result<Long> overflowResult = unsignedLong.parse("9223372036854775808");
        assertFalse(overflowResult.matches());

        // Test leading zero followed by other digits (should only parse the zero)
        Result<Long> leadingZeroResult = unsignedLong.parse("0123");
        assertTrue(leadingZeroResult.matches());
        assertEquals(0L, leadingZeroResult.value());

        // Test failure for non-digit
        Result<Long> nonDigitResult = unsignedLong.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testLongValue() {
        // Test positive long without sign
        Result<Long> positiveNoSignResult = longValue.parse("123");
        assertTrue(positiveNoSignResult.matches());
        assertEquals(123L, positiveNoSignResult.value());

        // Test positive long with sign
        Result<Long> positiveWithSignResult = longValue.parse("+123");
        assertTrue(positiveWithSignResult.matches());
        assertEquals(123L, positiveWithSignResult.value());

        // Test negative long
        Result<Long> negativeResult = longValue.parse("-123");
        assertTrue(negativeResult.matches());
        assertEquals(-123L, negativeResult.value());

        // Test large positive number
        Result<Long> largePositiveResult = longValue.parse("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(largePositiveResult.matches());
        assertEquals(Long.MAX_VALUE, largePositiveResult.value());

        // Test large negative number
        Result<Long> largeNegativeResult = longValue.parse("-9223372036854775808"); // Long.MIN_VALUE
        assertTrue(largeNegativeResult.matches());
        assertEquals(Long.MIN_VALUE, largeNegativeResult.value());

        // Test overflow
        Result<Long> positiveOverflowResult = longValue.parse("9223372036854775808");
        assertFalse(positiveOverflowResult.matches());

        Result<Long> negativeOverflowResult = longValue.parse("-9223372036854775809");
        assertFalse(negativeOverflowResult.matches());

        // Test zero
        Result<Long> zeroResult = longValue.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0L, zeroResult.value());

        // Test negative zero
        Result<Long> negativeZeroResult = longValue.parse("-0");
        assertTrue(negativeZeroResult.matches());
        assertEquals(0L, negativeZeroResult.value());

        // Test failure for non-digit
        Result<Long> nonDigitResult = longValue.parse("a");
        assertFalse(nonDigitResult.matches());
    }

    @Test
    public void testDoubleValue() {
        // Test integer part only
        Result<Double> integerOnlyResult = doubleValue.parse("123");
        assertTrue(integerOnlyResult.matches());
        assertEquals(123.0, integerOnlyResult.value());

        // Test with decimal point
        Result<Double> decimalResult = doubleValue.parse("123.45");
        assertTrue(decimalResult.matches());
        assertEquals(123.45, decimalResult.value());

        // Test with positive sign
        Result<Double> positiveSignResult = doubleValue.parse("+123.45");
        assertTrue(positiveSignResult.matches());
        assertEquals(123.45, positiveSignResult.value());

        // Test with negative sign
        Result<Double> negativeSignResult = doubleValue.parse("-123.45");
        assertTrue(negativeSignResult.matches());
        assertEquals(-123.45, negativeSignResult.value());

        // Test with exponent (positive)
        Result<Double> positiveExponentResult = doubleValue.parse("1.23e2");
        assertTrue(positiveExponentResult.matches());
        assertEquals(123.0, positiveExponentResult.value());

        // Test with exponent (negative)
        Result<Double> negativeExponentResult = doubleValue.parse("1.23e-2");
        assertTrue(negativeExponentResult.matches());
        assertEquals(0.0123, negativeExponentResult.value());

        // Test with uppercase exponent
        Result<Double> uppercaseExponentResult = doubleValue.parse("1.23E2");
        assertTrue(uppercaseExponentResult.matches());
        assertEquals(123.0, uppercaseExponentResult.value());

        // Test zero
        Result<Double> zeroResult = doubleValue.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0.0, zeroResult.value());

        // Test negative zero
        Result<Double> negativeZeroResult = doubleValue.parse("-0");
        assertTrue(negativeZeroResult.matches());
        assertEquals(-0.0, negativeZeroResult.value());

        // Test failure for non-numeric
        Result<Double> nonNumericResult = doubleValue.parse("abc");
        assertFalse(nonNumericResult.matches());
    }

    @Test
    public void testNumber() {
        // Test single digit
        Result<Long> singleDigitResult = number.parse("5");
        assertTrue(singleDigitResult.matches());
        assertEquals(5L, singleDigitResult.value());

        // Test multiple digits
        Result<Long> multiDigitResult = number.parse("123");
        assertTrue(multiDigitResult.matches());
        assertEquals(123L, multiDigitResult.value());

        // Test leading zero
        Result<Long> leadingZeroResult = number.parse("0123");
        assertTrue(leadingZeroResult.matches());
        assertEquals(123L, leadingZeroResult.value());

        // Test boundary values
        Result<Long> maxValueResult = number.parse("9223372036854775807");
        assertTrue(maxValueResult.matches());
        assertEquals(Long.MAX_VALUE, maxValueResult.value());

        // Test overflow
        Result<Long> overflowResult = number.parse("9223372036854775808");
        assertFalse(overflowResult.matches());

        // Test zero
        Result<Long> zeroResult = number.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0L, zeroResult.value());

        // Test failure for non-digit
        Result<Long> nonDigitResult = number.parse("a");
        assertFalse(nonDigitResult.matches());
    }

    @Test
    public void testHex() {
        // Test lowercase prefix
        Result<Long> lowercasePrefixResult = hex.parse("0x1a");
        assertTrue(lowercasePrefixResult.matches());
        assertEquals(26, lowercasePrefixResult.value());

        // Test uppercase prefix
        Result<Long> uppercasePrefixResult = hex.parse("0X1A");
        assertTrue(uppercasePrefixResult.matches());
        assertEquals(26, uppercasePrefixResult.value());

        // Test mixed case digits
        Result<Long> mixedCaseResult = hex.parse("0xaBcD");
        assertTrue(mixedCaseResult.matches());
        assertEquals(0xABCD, mixedCaseResult.value());

        // Test zero
        Result<Long> zeroResult = hex.parse("0x0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test max value
        Result<Long> maxValueResult = hex.parse("0x7fffffffffffffff");
        assertTrue(maxValueResult.matches());
        assertEquals(Long.MAX_VALUE, maxValueResult.value());

        // Test overflow
        Result<Long> overflowResult = hex.parse("0x8000000000000000");
        assertFalse(overflowResult.matches());

        // Test failure for invalid prefix
        Result<Long> invalidPrefixResult = hex.parse("0y1a");
        assertFalse(invalidPrefixResult.matches());

        // Test failure for missing hex digits
        Result<Long> missingDigitsResult = hex.parse("0x");
        assertFalse(missingDigitsResult.matches());
    }
}
