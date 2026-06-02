package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;

import static io.github.parseworks.taker.Taker.pure;
import static io.github.parseworks.taker.parsers.Combinators.satisfy;
import static io.github.parseworks.taker.parsers.Lexical.chr;

public class Numeric {

    /** Matches a non-zero digit (1-9). */
    public static final Taker<Character> nonZeroDigit = satisfy("<nonZeroDigit>", (CharPredicate) (c -> c != '0' && Character.isDigit(c)));


    /** Matches a single digit (0-9). */
    public static final Taker<Character> numeric = satisfy("<number>", (CharPredicate) Character::isDigit);


    /** Matches an optional sign (+ or -), defaulting to positive. */
    public static final Taker<Boolean> sign = Combinators.oneOf(
        chr('+').as(true),
        chr('-').as(false),
        pure(true)
    );

    /** Matches '0' and returns 0. */
    private static final Taker<Integer> unsignedIntegerZero = chr('0').as(0);

    /** Matches '0' and returns 0L. */
    private static final Taker<Long> unsignedLongZero = chr('0').as(0L);


    /** Matches an unsigned integer without leading zeros. */
    public static final Taker<Integer> unsignedInteger = unsignedIntegerZero.or(
        nonZeroDigit.then(Taker.takeWhile(CharPredicate.digit).orElse(""))
            .map((d, ds) -> Integer.parseInt(d + ds))
    );

    /** Matches a signed integer. */
    public static final Taker<Integer> integer = sign.then(unsignedInteger)
        .map((sign, i) -> sign ? i : -i);

    private static final Taker<Integer> exponent = (chr('e').or(chr('E')))
        .skipThen(integer);

    /** Matches an unsigned long without leading zeros. */
    public static final Taker<Long> unsignedLong = unsignedLongZero.or(longDigits(false));

    /** Matches a signed long. */
    public static final Taker<Long> longValue = new Taker<>(in -> {
        Input current = in;
        boolean positive = true;
        if (!current.isEof()) {
            char c = current.current();
            if (c == '+' || c == '-') {
                positive = c == '+';
                current = current.next();
            }
        }

        Result<Long> result = longDigits(!positive).apply(current);
        if (!result.matches()) {
            return result.cast();
        }

        long value = result.value();
        return new Match<>(positive ? value : -value, result.input());
    });

    private static Taker<Long> longDigits(boolean allowLongMinAbs) {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, "long value");
            }

            CharSequence data = in.data();
            int start = in.position();
            char first = data.charAt(start);
            if (first == '0') {
                return new Match<>(0L, in.next());
            }
            if (first < '1' || first > '9') {
                return new NoMatch<>(in, "long value");
            }

            int current = start + 1;
            while (current < data.length() && Character.isDigit(data.charAt(current))) {
                current++;
            }

            String digits = data.subSequence(start, current).toString();
            if (allowLongMinAbs && digits.equals("9223372036854775808")) {
                return new Match<>(Long.MIN_VALUE, in.skip(current - start));
            }
            try {
                return new Match<>(Long.parseLong(digits), in.skip(current - start));
            } catch (NumberFormatException e) {
                return new NoMatch<>(in.skip(current - start), "long value within range");
            }
        });
    }

    /** Matches a double-precision floating point number. */
    public static final Taker<Double> doubleValue = new Taker<>(in -> {
        CharSequence data = in.data();
        int length = data.length();
        int start = in.position();
        int current = start;

        if (current < length && (data.charAt(current) == '+' || data.charAt(current) == '-')) current++;

        boolean hasDigits = false;
        while (current < length && Character.isDigit(data.charAt(current))) {
            current++;
            hasDigits = true;
        }

        if (current < length && data.charAt(current) == '.') {
            current++;
            while (current < length && Character.isDigit(data.charAt(current))) {
                current++;
                hasDigits = true;
            }
        }

        if (!hasDigits) return new NoMatch<>(in, "doubleValue");

        if (current < length && (data.charAt(current) == 'e' || data.charAt(current) == 'E')) {
            int exponentStart = current++;
            if (current < length && (data.charAt(current) == '+' || data.charAt(current) == '-')) current++;
            boolean hasExpDigits = false;
            while (current < length && Character.isDigit(data.charAt(current))) {
                current++;
                hasExpDigits = true;
            }
            if (!hasExpDigits) current = exponentStart;
        }

        try {
            return new Match<>(Double.parseDouble(data.subSequence(start, current).toString()), in.skip(current - start));
        } catch (NumberFormatException e) {
            return new NoMatch<>(in, "doubleValue");
        }
    });

    public static final Taker<Long> number = Taker.takeWhile(CharPredicate.digit).map(Long::parseLong);

    private static final Taker<String> hexDigits = Taker.takeWhile(
            (CharPredicate) (c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
        .expecting("hex value");

    /** Matches a hexadecimal integer with "0x" or "0X" prefix. */
    public static final Taker<Long> hex = Lexical.string("0x").or(Lexical.string("0X"))
        .skipThen(hexDigits)
        .map(h -> Long.parseLong(h, 16));

}
