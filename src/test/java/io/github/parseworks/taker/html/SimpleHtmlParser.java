package io.github.parseworks.taker.html;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.parsers.Lexical;

import java.util.*;

import static io.github.parseworks.taker.Taker.*;
import static io.github.parseworks.taker.CharPredicate.noneOf;
import static io.github.parseworks.taker.parsers.Combinators.not;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.*;

/**
 * SimpleHtmlParser is a simplified parser for HTML/XML documents using parseworks.
 * This is a conversion of the JavaCC-based TagParser.
 */
public class SimpleHtmlParser {

    // Element types
    public static class Element {
        private String data;
        public String getData() {
            return data;
        }
    }

    public static class TextData extends Element {
        private final String text;

        public TextData(String text) {
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

        public StartTag(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes != null ? attributes : new HashMap<>();
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            return "StartTag[" + name + ", " + attributes + "]";
        }
    }

    public static class Declaration extends Element {
        private final String name;
        private final Map<String, String> attributes;

        public Declaration(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes != null ? attributes : new HashMap<>();
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
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "EndTag[" + name + "]";
        }
    }

    // Token parsers
    private static final Taker<Void> WHITESPACE = takeWhile(CharPredicate.whitespace).map(chars -> null);

    static String ILLEGAL_IDENTIFIER_CHARS = "=/> \t\n\r\f";

    static Taker<Void> END_TAG = oneOf( string(">"), string("/>")).map(data -> null);

    private static final Taker<String> IDENTIFIER = takeWhile(noneOf(ILLEGAL_IDENTIFIER_CHARS));

    private static final Taker<String> QUOTED_STRING =
        oneOf(
            escapedString('\'', '\0',Map.of()),
            escapedString('"', '\0', Map.of())
        );

    private static final Taker<Map<String, String>> COMMENT_BODY = Lexical.takeUntil("-->").map(
        data -> Map.of("data", data)
    );

    private static final Taker<Character> TAG_START = chr('<').peek(not(oneOf('!','#','/')));

    record KV(String k, String v){};


    // Element parsers
    public static final Taker<Element> element = Taker.ref();
    private static final Taker<Element> tagBody = Taker.ref();
    private static final Taker<Element> endTagBody = Taker.ref();
    private static final Taker<Map<String, String>> attributeList = Taker.ref();

    static {
        // Initialize recursive parsers

        // Attribute parser - parse a single attribute (name=value or just name)
        Taker<KV> attribute = trim(IDENTIFIER).then(
                    trim(chr('='))
                        .skipThen(oneOf( QUOTED_STRING,
                            takeWhile(noneOf(">\"' \t\n\r")))).optional()
                ).map(name -> valOpt -> new KV(name, (String) valOpt.orElse("")));

        // Attribute list parser - parse multiple attributes
        attributeList.set(
            attribute.zeroOrMore().map(kvs -> {
                Map<String,String> m = new HashMap<>(Math.max(4, kvs.size()*2));
                for (KV kv : kvs) m.put(kv.k, kv.v);
                return m;
            })
        );

        // Tag parser - parse a start tag with optional attributes
        tagBody.set(
            IDENTIFIER.then(attributeList.thenSkip(END_TAG))
                .map(name -> attrs -> new StartTag(name, attrs))
        );

        // End tag parser - parse an end tag
        endTagBody.set(IDENTIFIER.thenSkip(chr('>'))
            .map(EndTag::new)
        );

        // Comment parser - parse an HTML comment
        Taker<Element> commentTagBody = string("--").skipThen(COMMENT_BODY).thenSkip(Lexical.string("--"))
                .map(data -> new Declaration("--", data));

        // Declaration parser - parse a declaration tag like <!DOCTYPE ...>
        Taker<Element> declarationTagBody = IDENTIFIER.then(attributeList.thenSkip(END_TAG))
            .map(name -> attrs -> new Declaration(name, attrs));
        Taker<Element> rawText = takeUntil(c -> c == '<').map(TextData::new);
        //Taker<Element> rawText = takeUntil(CharPredicate.is('<')).map(TextData::new);

        Taker<Element> anyTag =
            chr('<').skipThen(
                oneOf(
                    // order: the next char after '<' decides which one is cheap to try first
                    Lexical.chr('!').skipThen(oneOf(commentTagBody, declarationTagBody)),
                    Lexical.chr('/').skipThen(endTagBody),
                    // fallback to content
                    tagBody
                )
            );
        // element tries either a tag (when '<') or raw text, without wasting work
        element.set(anyTag.or(rawText));
    }

    /**
     * Parse an HTML document.
     * 
     * @param input the HTML document to parse
     * @return the result of parsing
     */
    public static Result<Element> parse(String input) {
        return element.parse(Input.of(input));
    }

    /**
     * Parse an HTML document and return all elements.
     * 
     * @param input the HTML document to parse
     * @return the list of elements
     */
    public static List<Element> parseAll(String input) {
        List<Element> elements = new ArrayList<>();
        Input currentInput = Input.of(input);

        while (!currentInput.isEof()) {
            Result<Element> result = element.parse(currentInput);
            if (!result.matches()) {
                // Skip one character and try again
                currentInput = currentInput.next();
                continue;
            }

            elements.add(result.value());
            currentInput = result.input();
        }

        return Collections.unmodifiableList(elements);
    }
}
