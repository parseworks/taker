package io.github.parseworks.taker.examples;

import io.github.parseworks.taker.Failure;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.examples.TomlModel.TomlArray;
import io.github.parseworks.taker.examples.TomlModel.TomlBoolean;
import io.github.parseworks.taker.examples.TomlModel.TomlDate;
import io.github.parseworks.taker.examples.TomlModel.TomlDocument;
import io.github.parseworks.taker.examples.TomlModel.TomlFloat;
import io.github.parseworks.taker.examples.TomlModel.TomlInteger;
import io.github.parseworks.taker.examples.TomlModel.TomlString;
import io.github.parseworks.taker.examples.TomlModel.TomlTable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TomlExampleTest {

    @Test
    void parsesPracticalTomlDocument() {
        Result<TomlDocument> result = TomlParser.parse("""
            # This follows the TOML shape used by many small config files.
            title = "TOML Example"

            [owner]
            name = "Tom Preston-Werner"
            dob = 1979-05-27

            [database]
            enabled = true
            ports = [8000, 8001, 8002]
            temp_targets = { cpu = 79.5, case = 72.0 }

            [servers.alpha]
            ip = "10.0.0.1"
            role = 'frontend'

            [servers.beta]
            ip = "10.0.0.2"
            role = 'backend'
            """);

        assertTrue(result.matches(), result::error);
        TomlDocument document = result.value();

        assertEquals("TOML Example", ((TomlString) document.get("title")).value());
        assertEquals("Tom Preston-Werner", ((TomlString) document.get("owner", "name")).value());
        assertEquals(LocalDate.of(1979, 5, 27), ((TomlDate) document.get("owner", "dob")).value());
        assertTrue(((TomlBoolean) document.get("database", "enabled")).value());

        TomlArray ports = (TomlArray) document.get("database", "ports");
        assertEquals(List.of(new TomlInteger(8000), new TomlInteger(8001), new TomlInteger(8002)), ports.items());

        TomlTable tempTargets = (TomlTable) document.get("database", "temp_targets");
        assertEquals(new TomlFloat(79.5), tempTargets.get("cpu"));
        assertEquals(new TomlFloat(72.0), tempTargets.get("case"));

        assertEquals("10.0.0.1", ((TomlString) document.get("servers", "alpha", "ip")).value());
        assertEquals("frontend", ((TomlString) document.get("servers", "alpha", "role")).value());
        assertEquals("backend", ((TomlString) document.get("servers", "beta", "role")).value());
    }

    @Test
    void parsesDottedKeysIntoNestedTables() {
        Result<TomlDocument> result = TomlParser.parse("""
            name = "taker"
            package.version = "1.0.0"
            package.java = 21
            """);

        assertTrue(result.matches(), result::error);
        TomlDocument document = result.value();

        assertEquals("taker", ((TomlString) document.get("name")).value());
        assertEquals("1.0.0", ((TomlString) document.get("package", "version")).value());
        assertEquals(21L, ((TomlInteger) document.get("package", "java")).value());
    }

    @Test
    void rejectsDuplicateKeys() {
        Result<TomlDocument> result = TomlParser.parse("""
            title = "one"
            title = "two"
            """);

        assertFalse(result.matches());
        assertInstanceOf(Failure.class, result);
        assertTrue(result.error().contains("unique TOML key"));
    }

    @Test
    void rejectsScalarAndTablePathConflicts() {
        Result<TomlDocument> result = TomlParser.parse("""
            package = "taker"
            package.version = "1.0.0"
            """);

        assertFalse(result.matches());
        assertTrue(result.error().contains("unique TOML key"));
    }
}
