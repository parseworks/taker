package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;

import io.github.parseworks.taker.Lists;
import io.github.parseworks.taker.Taker;

import java.util.function.Function;

import static io.github.parseworks.taker.CharPredicate.anyOf;
import static io.github.parseworks.taker.parsers.Combinators.satisfy;
import static io.github.parseworks.taker.parsers.Combinators.takeWhile;
import static io.github.parseworks.taker.parsers.Lexical.*;

public class Numeric {

    /**
     * Matches a single non-zero digit (1-9).
     * <p>
     * Useful for parsing numbers without leading zeros.
     * <pre>{@code
     * nonZeroDigit.parse("5").value();    // '5'
     * nonZeroDigit.parse("0").matches();  // false
     * }</pre>
     *
     * @see #numeric for any digit including '0'
     * @see #unsignedInteger for complete integer parsing
     */
    public static final Taker<Character> nonZeroDigit = satisfy( "<nonZeroDigit>", c -> c != '0' && Character.isDigit(c));


    /**
     * Matches a single digit (0-9).
     * <pre>{@code
     * numeric.parse("5abc").value();             // '5'
     * numeric.oneOrMore().parse("123abc");       // ['1', '2', '3']
     * numeric.map2(Character::getNumericValue);   // converts to int
     * }</pre>
     *
     * @see #nonZeroDigit for digits 1-9 only
     * @see #number for multi-digit parsing
     */
    public static final Taker<Character> numeric = satisfy("<number>", Character::isDigit);


    /**
     * Matches an optional sign (+ or -), defaulting to positive.
     * <p>
     * Returns {@code true} for positive (+) or no sign, {@code false} for negative (-).
     * <pre>{@code
     * sign.parse("+123").value();  // true
     * sign.parse("-123").value();  // false
     * sign.parse("123").value();   // true (default positive)
     * }</pre>
     *
     * @see #intValue for signed integer parsing
     */
    public static final Taker<Boolean> sign = Combinators.oneOf(
            chr('+').as(true),
            chr('-').as(false),
            pure(true)
    );

    /** Matches '0' and returns 0. */
    private static final Taker<Integer> unsignedIntegerZero = chr('0').as(0);

    /** Matches '0' and returns 0L. */
    private static final Taker<Long> unsignedLongZero = chr('0').as( 0L);


    private static final Taker<Integer> unSignedIntegerNotZero = nonZeroDigitParser(Integer::parseInt);

    private static final Taker<Long> unsignedLongNotZero = nonZeroDigitParser(Long::parseLong);

    public static final Taker<Integer> unsignedInteger = unsignedIntegerZero.or(unSignedIntegerNotZero);

    /**
     * Matches a signed integer with optional sign (+/-).
     * <pre>{@code
     * integer.parse("123").value();   // 123
     * integer.parse("+123").value();  // 123
     * integer.parse("-123").value();  // -123
     * }</pre>
     *
     * @see #unsignedInteger for unsigned parsing
     * @see #longValue for larger integers
     */
    public static final Taker<Integer> intValue = sign.then(unsignedInteger)
            .map2((sign, i) -> sign ? i : -i);

    private static final Taker<Integer> exponent = (chr('e').or(chr('E'))).skipThen(intValue);

    /**
     * Matches an unsigned long integer without leading zeros.
     * <p>
     * Similar to {@link #unsignedInteger} but returns {@code Long}.
     * <pre>{@code
     * unsignedLong.parse("9223372036854775807").value();  // Long.MAX_VALUE
     * unsignedLong.parse("0").value();                    // 0L
     * }</pre>
     *
     * @see #longValue for signed longs
     * @see #unsignedInteger for Integer range
     */
    public static final Taker<Long> unsignedLong = unsignedLongZero.or(unsignedLongNotZero);

    /**
     * Matches a signed long integer with optional sign (+/-).
     * <pre>{@code
     * longValue.parse("9223372036854775807").value();  // Long.MAX_VALUE
     * longValue.parse("-42").value();                  // -42L
     * }</pre>
     *
     * @see #unsignedLong for unsigned parsing
     * @see #intValue for Integer range
     */
    public static final Taker<Long> longValue = sign.then(unsignedLong)
            .map2((sign, i) -> sign ? i : -i);

    private static final Taker<Double> floating = numeric.zeroOrMore()
            .map(digits -> {
                double result = 0.0;
                double factor = 0.1;
                for (Character c : digits) {
                    result += Character.getNumericValue(c) * factor;
                    factor *= 0.1;
                }
                return result;
            });
    /**
     * Matches a double-precision floating point number with optional sign, decimal, and exponent.
     * <p>
     * Supports scientific notation (e.g., "6.022E23").
     * <pre>{@code
     * doubleValue.parse("123.45").value();   // 123.45
     * doubleValue.parse("-3.14").value();    // -3.14
     * doubleValue.parse("6.022E23").value(); // 6.022 × 10²³
     * doubleValue.parse("42").value();       // 42.0
     * }</pre>
     *
     * @see #intValue for integer parsing
     * @see #longValue for long integer parsing
     */
    public static final Taker<Double> doubleValue = sign.then(unsignedLong)
            .then((chr('.').skipThen(floating)).optional())
            .then(exponent.optional())
            .map4((sn, i, f, exp) -> {
                double r = i.doubleValue();
                if (f.isPresent()) {
                    r += f.get();
                }
                if (exp.isPresent()) {
                    r = r * Math.pow(10.0, exp.get());
                }
                return sn ? r : -r;
            });

    public static final Taker<Integer> number = satisfy("<digit>", Character::isDigit).oneOrMore()
            .map(Lists::join)
            .map(Integer::parseInt);

    private static final Taker<String> hexDigits = takeWhile( c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
            .expecting("hex value");

 
    /**
     * Matches a hexadecimal integer with "0x" or "0X" prefix.
     * <pre>{@code
     * hex.parse("0xFF").value();   // 255
     * hex.parse("0x2A").value();   // 42
     * }</pre>
     */
    public static final Taker<Integer>  hex = chr('0').then(take(anyOf("xX")))
        .skipThen(hexDigits)
        .map(hexStr -> Integer.parseInt(hexStr, 16));

    /**
     * A parser that parses a non-zero digit followed by zero or more digits.
     * This parser will succeed if the next input symbols form a non-zero digit followed by zero or more digits,
     * and will return the parsed result converted by the given converter function.
     *
     * @param converter the function to convert the parsed digits
     * @param <T>       the type of the parsed value
     * @return a parser that parses a non-zero digit followed by zero or more digits and converts the result
     */
    private static <T> Taker<T> nonZeroDigitParser(Function<String, T> converter) {
        return nonZeroDigit.then(takeWhile(Character::isDigit))
                .map2((d,ds) ->  converter.apply(d + ds));
    }
}
