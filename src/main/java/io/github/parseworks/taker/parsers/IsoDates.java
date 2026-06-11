package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Taker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static io.github.parseworks.taker.CharPredicate.asciiDigit;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Numeric.numeric;

public class IsoDates {

    private static final Taker<Integer> d1 = numeric.map(Character::getNumericValue);
    private static final Taker<Integer> d2 = d1.then(d1).map((a, b) -> a * 10 + b);
    private static final Taker<Integer> d4 = d2.then(d2).map((a, b) -> a * 100 + b);

    /** Matches YYYY-MM-DD */
    public static final Taker<LocalDate> date = d4
            .thenSkip(chr('-')).then(d2)
            .thenSkip(chr('-')).then(d2)
            .map(LocalDate::of);

    /** Matches HH:mm:ss */
    private static final Taker<String> timePart = d2
            .thenSkip(chr(':')).then(d2)
            .thenSkip(chr(':')).then(d2)
            .map((h, m, s) -> String.format("%02d:%02d:%02d", h, m, s));

    /** Matches .SSS (optional) */
    private static final Taker<String> millisPart = chr('.')
            .skipThen(Taker.takeWhile(asciiDigit))
            .map(ms -> "." + ms)
            .orElse("");

    /** Matches Z or +HH:mm or -HH:mm */
    private static final Taker<String> zonePart = chr('Z').map(Object::toString)
            .or(chr('+').or(chr('-'))
                    .then(d2)
                    .thenSkip(chr(':').optional())
                    .then(d2)
                    .map((sign, h, m) -> String.format("%c%02d:%02d", sign, h, m)));

    /** Matches YYYY-MM-DDTHH:mm:ss[.SSS][Z|[+-]HH:mm] */
    public static final Taker<OffsetDateTime> offsetDateTime = date
            .thenSkip(chr('T').or(chr(' ')))
            .then(timePart)
            .then(millisPart)
            .then(zonePart)
            .map((d, t, ms, z) -> OffsetDateTime.parse(d.toString() + "T" + t + ms + z));

    public static final Taker<LocalDateTime> localDateTime = date
            .thenSkip(chr('T').or(chr(' ')))
            .then(timePart)
            .then(millisPart)
            .map((d, t, ms) -> LocalDateTime.parse(d.toString() + "T" + t + ms));
}
