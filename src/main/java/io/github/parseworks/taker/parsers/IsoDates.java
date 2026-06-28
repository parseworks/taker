package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static io.github.parseworks.taker.CharPredicate.asciiDigit;
import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Chars.takeWhile;
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
    private static final Taker<LocalTime> timePart = d2
            .thenSkip(chr(':')).then(d2)
            .thenSkip(chr(':')).then(d2)
            .map(LocalTime::of);

    /** Matches .SSS (optional), returns nanos */
    private static final Taker<Integer> nanosPart = new Taker<>(in -> {
        Result<Character> dot = chr('.').apply(in);
        if (!dot.matches()) {
            return new Match<>(0, in);
        }
        Input current = dot.input();
        CharSequence data = current.data();
        int start = current.position();
        int pos = start;
        int limit = data.length();
        long value = 0;
        int count = 0;
        while (pos < limit) {
            char c = data.charAt(pos);
            if (c < '0' || c > '9') break;
            if (count < 9) {
                value = value * 10 + (c - '0');
            }
            count++;
            pos++;
        }
        if (count == 0) {
            return new NoMatch<>(in, "fractional seconds");
        }
        while (count < 9) {
            value *= 10;
            count++;
        }
        return new Match<>((int) value, current.skip(pos - start));
    });

    /** Matches Z or +HH:mm or -HH:mm */
    private static final Taker<ZoneOffset> zonePart = chr('Z').as(ZoneOffset.UTC)
            .or(chr('+').or(chr('-'))
                    .then(d2)
                    .thenSkip(chr(':').optional())
                    .then(d2)
                    .map((sign, h, m) -> {
                        int totalSeconds = (h * 3600 + m * 60) * (sign == '+' ? 1 : -1);
                        return ZoneOffset.ofTotalSeconds(totalSeconds);
                    }));

    /** Matches YYYY-MM-DDTHH:mm:ss[.SSS][Z|[+-]HH:mm] */
    public static final Taker<OffsetDateTime> offsetDateTime = date
            .thenSkip(chr('T').or(chr(' ')))
            .then(timePart)
            .then(nanosPart)
            .then(zonePart)
            .map((d, t, ns, z) -> OffsetDateTime.of(d, t.withNano(ns), z));

    public static final Taker<LocalDateTime> localDateTime = date
            .thenSkip(chr('T').or(chr(' ')))
            .then(timePart)
            .then(nanosPart)
            .map((d, t, ns) -> LocalDateTime.of(d, t.withNano(ns)));
}
