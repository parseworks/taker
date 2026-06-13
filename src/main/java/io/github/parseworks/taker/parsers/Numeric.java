package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import static io.github.parseworks.taker.parsers.Combinators.pure;
import static io.github.parseworks.taker.parsers.Lexical.chr;

public class Numeric {

    /** Matches a non-zero digit (1-9). */
    public static final Taker<Character> nonZeroDigit = chr(CharPredicate.range('1', '9')).expecting("non-zero digit");


    /** Matches a single digit (0-9). */
    public static final Taker<Character> numeric = chr(CharPredicate.asciiDigit);


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
    public static final Taker<Integer> unsignedInteger = unsignedIntegerZero.or(integerDigits(false));

    /** Matches a signed integer. */
    public static final Taker<Integer> integer = new Taker<>(in -> {
        Input current = in;
        boolean positive = true;
        if (!current.isEof()) {
            char c = current.current();
            if (c == '+' || c == '-') {
                positive = c == '+';
                current = current.next();
            }
        }

        Result<Integer> result = integerDigits(!positive).apply(current);
        if (!result.matches()) {
            return result.cast();
        }

        int value = result.value();
        return new Match<>(positive ? value : -value, result.input());
    });

    private static Taker<Integer> integerDigits(boolean allowIntegerMinAbs) {
        return new Taker<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, "integer value");
            }

            CharSequence data = in.data();
            int start = in.position();
            char first = data.charAt(start);
            if (first == '0') {
                return new Match<>(0, in.next());
            }
            if (first < '1' || first > '9') {
                return new NoMatch<>(in, "integer value");
            }

            int current = start + 1;
            while (current < data.length() && isAsciiDigit(data.charAt(current))) {
                current++;
            }

            String digits = data.subSequence(start, current).toString();
            if (allowIntegerMinAbs && digits.equals("2147483648")) {
                return new Match<>(Integer.MIN_VALUE, in.skip(current - start));
            }
            try {
                return new Match<>(Integer.parseInt(digits), in.skip(current - start));
            } catch (NumberFormatException e) {
                return new NoMatch<>(in.skip(current - start), "integer value within range");
            }
        });
    }

    /** Matches an unsigned long without leading zeros. */
    public static final Taker<Long> unsignedLong = unsignedLongZero.or(longDigits(false));

    /** Matches signed long. */
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
            while (current < data.length() && isAsciiDigit(data.charAt(current))) {
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
        while (current < length && isAsciiDigit(data.charAt(current))) {
            current++;
            hasDigits = true;
        }

        if (current < length && data.charAt(current) == '.') {
            current++;
            while (current < length && isAsciiDigit(data.charAt(current))) {
                current++;
                hasDigits = true;
            }
        }

        if (!hasDigits) return new NoMatch<>(in, "doubleValue");

        if (current < length && (data.charAt(current) == 'e' || data.charAt(current) == 'E')) {
            int exponentStart = current++;
            if (current < length && (data.charAt(current) == '+' || data.charAt(current) == '-')) current++;
            boolean hasExpDigits = false;
            while (current < length && isAsciiDigit(data.charAt(current))) {
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

    public static final Taker<Long> number = new Taker<>(in -> {
        CharSequence data = in.data();
        int start = in.position();
        int current = start;

        while (current < data.length() && isAsciiDigit(data.charAt(current))) {
            current++;
        }

        if (current == start) {
            return new NoMatch<>(in, "number");
        }

        try {
            return new Match<>(Long.parseLong(data.subSequence(start, current).toString()), in.skip(current - start));
        } catch (NumberFormatException e) {
            return new NoMatch<>(in.skip(current - start), "long value within range");
        }
    });

    private static final Taker<String> hexDigits = Lexical.takeWhile(
            CharPredicate.anyOf(CharPredicate.asciiDigit, CharPredicate.range('a', 'f'), CharPredicate.range('A', 'F')))
        .expecting("hex value");

    /** Matches a hexadecimal integer with "0x" or "0X" prefix. */
    public static final Taker<Long> hex = Lexical.string("0x").or(Lexical.string("0X"))
        .skipThen(hexDigits)
        .flatMap(Numeric::parseHex);

    private static Taker<Long> parseHex(String digits) {
        return new Taker<>(in -> {
            try {
                return new Match<>(Long.parseLong(digits, 16), in);
            } catch (NumberFormatException e) {
                return new NoMatch<>(in, "hex value within range");
            }
        });
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

}
