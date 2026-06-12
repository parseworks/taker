package io.github.parseworks.taker.examples;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.parseworks.taker.CharPredicate.noneOf;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.collectChars;
import static io.github.parseworks.taker.parsers.Lexical.escapedString;
import static io.github.parseworks.taker.parsers.Lexical.skipWhile;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Lexical.takeUntil;
import static io.github.parseworks.taker.parsers.Lexical.trimSpaces;
import static io.github.parseworks.taker.parsers.Lexical.trimWhitespace;
import static io.github.parseworks.taker.parsers.Numeric.doubleValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RealisticExamplesTest {

    @Test
    void parsesSectionedConfigFile() {
        Taker<IniLine> section = chr('[')
            .skipThen(collectChars(noneOf("]\r\n")).expecting("section name"))
            .thenSkip(chr(']'))
            .map(Section::new);

        Taker<String> key = collectChars(
            CharPredicate.asciiLetterOrDigit.or(CharPredicate.anyOf("._-"))
        ).expecting("key");
        Taker<Entry> entry = key
            .thenSkip(trimSpaces(chr('=')))
            .then(takeUntil(CharPredicate.lineBreak).map(String::trim))
            .map(Entry::new);

        Taker<IniLine> line = trimSpaces(oneOf(
            section.map(s -> (IniLine) s),
            entry.map(e -> (IniLine) e)
        ));
        Taker<List<IniLine>> file = line.oneOrMoreSeparatedBy(oneOf(string("\r\n"), string("\n")));

        Result<List<IniLine>> result = file.parseAll("""
            [server]
            host = localhost
            port = 8080
            [features]
            cache.enabled = true""");

        assertTrue(result.matches(), () -> result.error());
        Config config = Config.from(result.value());

        assertEquals("localhost", config.get("server", "host"));
        assertEquals("8080", config.get("server", "port"));
        assertEquals("true", config.get("features", "cache.enabled"));
    }

    @Test
    void parsesNestedJsonValue() {
        Taker<JsonValue> value = Taker.ref();
        Taker<Character> comma = trimWhitespace(chr(','));
        Taker<Character> colon = trimWhitespace(chr(':'));

        Taker<String> quoted = trimWhitespace(
            escapedString('"', '\\', Map.of(
                '"', '"',
                '\\', '\\',
                '/', '/',
                'n', '\n',
                'r', '\r',
                't', '\t'
            ))
        ).expecting("string");

        Taker<JsonValue> jsonString = quoted.map(s -> (JsonValue) new JsonString(s));
        Taker<JsonValue> jsonNumber = trimWhitespace(doubleValue)
            .map(n -> (JsonValue) new JsonNumber(n));
        Taker<JsonValue> jsonBoolean = oneOf(
            trimWhitespace(string("true")).as((JsonValue) new JsonBoolean(true)),
            trimWhitespace(string("false")).as((JsonValue) new JsonBoolean(false))
        );
        Taker<JsonValue> jsonNull = trimWhitespace(string("null")).as(JsonNull.INSTANCE);

        Taker<JsonMember> member = quoted
            .thenSkip(colon)
            .then(value)
            .map(JsonMember::new);

        Taker<JsonValue> array = trimWhitespace(chr('['))
            .skipThen(value.zeroOrMoreSeparatedBy(comma))
            .thenSkip(trimWhitespace(chr(']')))
            .map(items -> (JsonValue) new JsonArray(items));

        Taker<JsonValue> object = trimWhitespace(chr('{'))
            .skipThen(member.zeroOrMoreSeparatedBy(comma))
            .thenSkip(trimWhitespace(chr('}')))
            .map(members -> {
                Map<String, JsonValue> fields = new LinkedHashMap<>();
                for (JsonMember memberValue : members) {
                    fields.put(memberValue.name(), memberValue.value());
                }
                return (JsonValue) new JsonObject(fields);
            });

        value.set(oneOf(object, array, jsonString, jsonNumber, jsonBoolean, jsonNull));

        Result<JsonValue> result = value.parseAll("""
            {
              "name": "taker",
              "stars": 10,
              "tags": ["parser", "java", "combinator"],
              "published": false,
              "meta": { "license": "MIT" }
            }""");

        assertTrue(result.matches(), () -> result.error());
        JsonObject root = (JsonObject) result.value();

        assertEquals(new JsonString("taker"), root.fields().get("name"));
        assertEquals(new JsonNumber(10.0), root.fields().get("stars"));
        assertEquals(new JsonBoolean(false), root.fields().get("published"));

        JsonArray tags = (JsonArray) root.fields().get("tags");
        assertEquals(List.of(
            new JsonString("parser"),
            new JsonString("java"),
            new JsonString("combinator")
        ), tags.items());

        JsonObject meta = (JsonObject) root.fields().get("meta");
        assertEquals(new JsonString("MIT"), meta.fields().get("license"));
    }

    private sealed interface IniLine permits Section, Entry {
    }

    private record Section(String name) implements IniLine {
    }

    private record Entry(String key, String value) implements IniLine {
    }

    private record Config(Map<String, Map<String, String>> sections) {
        static Config from(List<IniLine> lines) {
            Map<String, Map<String, String>> sections = new LinkedHashMap<>();
            String currentSection = "";
            sections.put(currentSection, new LinkedHashMap<>());

            for (IniLine line : lines) {
                if (line instanceof Section(String name)) {
                    currentSection = name;
                    sections.computeIfAbsent(currentSection, ignored -> new LinkedHashMap<>());
                } else if (line instanceof Entry(String key, String value)) {
                    sections.get(currentSection).put(key, value);
                }
            }

            return new Config(sections);
        }

        String get(String section, String key) {
            return sections.getOrDefault(section, Map.of()).get(key);
        }
    }

    private sealed interface JsonValue permits JsonString, JsonNumber, JsonBoolean, JsonNull, JsonArray, JsonObject {
    }

    private record JsonString(String value) implements JsonValue {
    }

    private record JsonNumber(double value) implements JsonValue {
    }

    private record JsonBoolean(boolean value) implements JsonValue {
    }

    private enum JsonNull implements JsonValue {
        INSTANCE
    }

    private record JsonArray(List<JsonValue> items) implements JsonValue {
        private JsonArray {
            items = List.copyOf(items);
        }
    }

    private record JsonObject(Map<String, JsonValue> fields) implements JsonValue {
        private JsonObject {
            fields = new LinkedHashMap<>(fields);
        }
    }

    private record JsonMember(String name, JsonValue value) {
    }
}
