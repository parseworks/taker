package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CommonParsersTest {

    @Test
    void testIsoDates() {
        Result<LocalDate> dateResult = IsoDates.date.parse("2023-04-23");
        assertTrue(dateResult.matches());
        assertEquals(LocalDate.of(2023, 4, 23), dateResult.value());

        Result<LocalDateTime> ldtResult = IsoDates.localDateTime.parse("2023-04-23T10:20:30.123");
        assertTrue(ldtResult.matches());
        assertEquals(LocalDateTime.of(2023, 4, 23, 10, 20, 30, 123000000), ldtResult.value());

        Result<OffsetDateTime> odtResult = IsoDates.offsetDateTime.parse("2023-04-23T10:20:30.123Z");
        assertTrue(odtResult.matches());
        assertEquals(OffsetDateTime.parse("2023-04-23T10:20:30.123Z"), odtResult.value());

        Result<OffsetDateTime> odtResult2 = IsoDates.offsetDateTime.parse("2023-04-23T10:20:30+05:30");
        assertTrue(odtResult2.matches());
        assertEquals(OffsetDateTime.parse("2023-04-23T10:20:30+05:30"), odtResult2.value());
    }

    @Test
    void testCsv() {
        String input = "name,age,city\n\"Bob, The Builder\",30,London\nAlice,25,\"New York\"";
        Result<List<List<String>>> result = Csv.csv.parse(input);
        assertTrue(result.matches());
        List<List<String>> data = result.value();
        assertEquals(3, data.size());
        assertEquals(List.of("name", "age", "city"), data.get(0));
        assertEquals(List.of("Bob, The Builder", "30", "London"), data.get(1));
        assertEquals(List.of("Alice", "25", "New York"), data.get(2));
    }

    @Test
    void testJson() {
        String input = "{\"name\":\"John Doe\",\"age\":30}";
        Result<Object> result = Json.json.parse(input);
        assertTrue(result.matches(), result::error);
        Map<String, Object> map = (Map<String, Object>) result.value();
        assertEquals("John Doe", map.get("name"));
    }
}
