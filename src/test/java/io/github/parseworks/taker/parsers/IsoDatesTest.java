package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsoDatesTest {

    @Test
    void parsesIsoDate() {
        Result<LocalDate> result = IsoDates.date.parseAll("2023-04-23");

        assertTrue(result.matches(), result::error);
        assertEquals(LocalDate.of(2023, 4, 23), result.value());
    }

    @Test
    void parsesLocalDateTimeWithTOrSpaceSeparator() {
        Result<LocalDateTime> withT = IsoDates.localDateTime.parseAll("2023-04-23T10:20:30.123");
        Result<LocalDateTime> withSpace = IsoDates.localDateTime.parseAll("2023-04-23 10:20:30");

        assertTrue(withT.matches(), withT::error);
        assertEquals(LocalDateTime.of(2023, 4, 23, 10, 20, 30, 123000000), withT.value());

        assertTrue(withSpace.matches(), withSpace::error);
        assertEquals(LocalDateTime.of(2023, 4, 23, 10, 20, 30), withSpace.value());
    }

    @Test
    void parsesOffsetDateTimeWithZuluAndNumericOffsets() {
        Result<OffsetDateTime> zulu = IsoDates.offsetDateTime.parseAll("2023-04-23T10:20:30.123Z");
        Result<OffsetDateTime> colonOffset = IsoDates.offsetDateTime.parseAll("2023-04-23T10:20:30+05:30");
        Result<OffsetDateTime> compactOffset = IsoDates.offsetDateTime.parseAll("2023-04-23T10:20:30-0330");

        assertTrue(zulu.matches(), zulu::error);
        assertEquals(OffsetDateTime.parse("2023-04-23T10:20:30.123Z"), zulu.value());

        assertTrue(colonOffset.matches(), colonOffset::error);
        assertEquals(OffsetDateTime.parse("2023-04-23T10:20:30+05:30"), colonOffset.value());

        assertTrue(compactOffset.matches(), compactOffset::error);
        assertEquals(OffsetDateTime.parse("2023-04-23T10:20:30-03:30"), compactOffset.value());
    }

    @Test
    void preservesMillisecondAndNanosecondPrecision() {
        Result<LocalDateTime> tenths = IsoDates.localDateTime.parseAll("2023-04-23T10:20:30.1");
        Result<LocalDateTime> nanos = IsoDates.localDateTime.parseAll("2023-04-23T10:20:30.123456789");

        assertTrue(tenths.matches(), tenths::error);
        assertEquals(LocalDateTime.of(2023, 4, 23, 10, 20, 30, 100000000), tenths.value());

        assertTrue(nanos.matches(), nanos::error);
        assertEquals(LocalDateTime.of(2023, 4, 23, 10, 20, 30, 123456789), nanos.value());
    }

    @Test
    void rejectsMissingDateFields() {
        assertFalse(IsoDates.date.parseAll("2023-04").matches());
        assertFalse(IsoDates.localDateTime.parseAll("2023-04-23T10:20").matches());
        assertFalse(IsoDates.offsetDateTime.parseAll("2023-04-23T10:20:30").matches());
    }

    @Test
    void invalidCalendarValuesThrowDateTimeException() {
        assertThrows(DateTimeException.class, () -> IsoDates.date.parseAll("2023-02-30"));
        assertThrows(DateTimeException.class, () -> IsoDates.localDateTime.parseAll("2023-04-23T25:20:30"));
        assertThrows(DateTimeException.class, () -> IsoDates.offsetDateTime.parseAll("2023-04-23T10:20:30+25:00"));
    }
}
