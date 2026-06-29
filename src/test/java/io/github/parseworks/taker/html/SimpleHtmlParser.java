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

package io.github.parseworks.taker.html;

import io.github.parseworks.taker.parsers.Chars;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.ResultType;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.Match;
import io.github.parseworks.taker.results.NoMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.parseworks.taker.CharPredicate.notAnyOf;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Chars.collectChars;
import static io.github.parseworks.taker.parsers.Lexical.escapedString;
import static io.github.parseworks.taker.parsers.Lexical.lexeme;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Lexical.takeUntil;

/**
 * Practical HTML4-oriented tokenizer example.
 * <p>
 * This is not a browser-grade HTML parser. It intentionally parses the common
 * token stream shape: start tags, self-closing tags, end tags, comments,
 * declarations such as {@code <!DOCTYPE html>}, attributes, and text nodes.
 * It demonstrates scanner primitives, labels, lookahead-free branch ordering,
 * allocation-conscious folds, and strict document scanning.
 */
public class SimpleHtmlParser {

    public static class Element {
        private final int start;
        private final int end;

        Element() {
            this(-1, -1);
        }

        Element(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public static class TextData extends Element {
        private final String text;

        public TextData(String text) {
            this(text, -1, -1);
        }

        public TextData(String text, int start, int end) {
            super(start, end);
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "TextData[" + text + "]";
        }
    }

    public static class StartTag extends Element {
        private final String name;
        private final Map<String, String> attributes;
        private final boolean selfClosing;

        public StartTag(String name, Map<String, String> attributes) {
            this(name, attributes, false, -1, -1);
        }

        public StartTag(String name, Map<String, String> attributes, boolean selfClosing, int start, int end) {
            super(start, end);
            this.name = normalizeName(name);
            this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
            this.selfClosing = selfClosing;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public boolean isSelfClosing() {
            return selfClosing;
        }

        @Override
        public String toString() {
            return "StartTag[" + name + ", " + attributes + ", selfClosing=" + selfClosing + "]";
        }
    }

    public static class Declaration extends Element {
        private final String name;
        private final Map<String, String> attributes;

        public Declaration(String name, Map<String, String> attributes) {
            this(name, attributes, -1, -1);
        }

        public Declaration(String name, Map<String, String> attributes, int start, int end) {
            super(start, end);
            this.name = name;
            this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public String getAttributeValue(String name) {
            return attributes.get(name);
        }

        @Override
        public String toString() {
            return "Declaration[" + name + ", " + attributes + "]";
        }
    }

    public static class EndTag extends Element {
        private final String name;

        public EndTag(String name) {
            this(name, -1, -1);
        }

        public EndTag(String name, int start, int end) {
            super(start, end);
            this.name = normalizeName(name);
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "EndTag[" + name + "]";
        }
    }

    private record Attribute(String name, String value) {
    }

    private record StartTagParts(String name, Map<String, String> attributes, boolean selfClosing) {
    }

    private record AttributesAndClose(Map<String, String> attributes, boolean selfClosing) {
    }

    private record DeclarationParts(String name, String data) {
    }

    private static final Taker<?> HTML_SPACE = chr(CharPredicate.asciiWhitespace).skipOneOrMore();
    private static final Taker<String> NAME = collectChars(
        CharPredicate.asciiLetterOrDigit.or(CharPredicate.anyOf("_:-"))
    ).expecting("HTML name");
    private static final Taker<String> QUOTED_VALUE = oneOf(
        escapedString('"', '\0', Map.of()),
        escapedString('\'', '\0', Map.of())
    ).expecting("quoted attribute value");
    private static final Taker<String> UNQUOTED_VALUE = collectChars(notAnyOf(" \t\n\r\f\"'=<>`"))
        .expecting("unquoted attribute value");

    private static final Taker<String> ATTRIBUTE_VALUE = oneOf(QUOTED_VALUE, UNQUOTED_VALUE)
        .expecting("attribute value");

    private static final Taker<Attribute> ATTRIBUTE = token(NAME)
        .then(token(chr('=')).skipThen(token(ATTRIBUTE_VALUE)).optional())
        .map(name -> value -> new Attribute(normalizeName(name), value.orElse("")))
        .label("attribute");

    private static final Taker<Map<String, String>> ATTRIBUTES = ATTRIBUTE.foldZeroOrMoreFrom(
        LinkedHashMap::new,
        (attributes, attribute) -> {
            attributes.put(attribute.name(), attribute.value());
            return attributes;
        }
    );

    private static final Taker<Boolean> TAG_CLOSE = oneOf(
        token(string("/>")).as(true),
        token(string(">")).as(false)
    ).expecting("tag close");

    private static final Taker<Element> COMMENT = string("<!--")
        .skipThen(takeUntil("-->"))
        .thenSkip(string("-->"))
        .located()
        .map(located -> (Element) new Declaration("--", Map.of("data", located.value()), located.start(), located.end()))
        .label("HTML comment");

    private static final Taker<Element> DECLARATION = string("<!")
        .skipThen(NAME)
        .then(takeUntil(">"))
        .thenSkip(chr('>'))
        .map(DeclarationParts::new)
        .located()
        .map(located -> (Element) new Declaration(
            located.value().name().toUpperCase(Locale.ROOT),
            Map.of("data", located.value().data().trim()),
            located.start(),
            located.end()
        ))
        .label("HTML declaration");

    private static final Taker<StartTagParts> START_TAG_PARTS = chr('<')
        .skipThen(NAME)
        .then(ATTRIBUTES.then(TAG_CLOSE).map(AttributesAndClose::new))
        .map(name -> attrsAndClose -> new StartTagParts(name, attrsAndClose.attributes(), attrsAndClose.selfClosing()))
        .label("start tag");

    private static final Taker<Element> START_TAG = START_TAG_PARTS
        .located()
        .map(located -> new StartTag(
            located.value().name(),
            located.value().attributes(),
            located.value().selfClosing(),
            located.start(),
            located.end()
        ));

    private static final Taker<Element> END_TAG = string("</")
        .skipThen(NAME)
        .thenSkip(token(chr('>')))
        .located()
        .map(located -> (Element) new EndTag(located.value(), located.start(), located.end()))
        .label("end tag");

    private static final Taker<Element> TEXT = collectChars(CharPredicate.not(CharPredicate.is('<')))
        .located()
        .map(located -> (Element) new TextData(located.value(), located.start(), located.end()))
        .label("text");

    public static final Taker<Element> element = oneOf(
        COMMENT,
        DECLARATION,
        END_TAG,
        START_TAG,
        TEXT
    ).expecting("HTML element");

    private SimpleHtmlParser() {
    }

    /**
     * Parse one HTML token.
     *
     * @param input the HTML document fragment to parse
     * @return the result of parsing one element
     */
    public static Result<Element> parse(String input) {
        return element.parse(Input.of(input));
    }

    /**
     * Strictly parse an HTML document into tokens.
     *
     * @param input the HTML document to parse
     * @return a parser result containing every parsed token
     */
    public static Result<List<Element>> parseDocument(String input) {
        Input current = Input.of(input);
        List<Element> elements = new ArrayList<>();

        while (!current.isEof()) {
            Result<Element> result = element.apply(current);
            if (!result.matches()) {
                return result.cast();
            }
            if (result.input().position() == current.position()) {
                return new NoMatch<>(current, "HTML element to consume input");
            }

            elements.add(result.value());
            current = result.input();
        }

        return new Match<>(Collections.unmodifiableList(elements), current);
    }

    /**
     * Parse an HTML document and return all tokens.
     *
     * @param input the HTML document to parse
     * @return the list of parsed elements
     */
    public static List<Element> parseAll(String input) {
        Result<List<Element>> result = parseDocument(input);
        if (result.type() != ResultType.MATCH) {
            throw new IllegalArgumentException(result.error());
        }
        return result.value();
    }

    private static <A> Taker<A> token(Taker<A> parser) {
        return lexeme(parser, HTML_SPACE);
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
