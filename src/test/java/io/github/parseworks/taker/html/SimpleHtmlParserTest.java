package io.github.parseworks.taker.html;

import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleHtmlParserTest {

    @Test
    public void testParseTag() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<div>");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.StartTag.class, result.value());
        SimpleHtmlParser.StartTag tag = (SimpleHtmlParser.StartTag) result.value();
        assertEquals("div", tag.getName());
    }

    @Test
    public void testParseTagWithAttributes() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<div id=\"main\" class=\"container\">");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.StartTag.class, result.value());
        SimpleHtmlParser.StartTag tag = (SimpleHtmlParser.StartTag) result.value();
        assertEquals("div", tag.getName());
        assertEquals(2, tag.getAttributes().size());
        assertEquals("main", tag.getAttributes().get("id"));
        assertEquals("container", tag.getAttributes().get("class"));
    }

    @Test
    public void testParseHtml4AttributeForms() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<INPUT DISABLED value=hello data-id='42' />");

        assertTrue(result.matches(), () -> result.error());
        assertInstanceOf(SimpleHtmlParser.StartTag.class, result.value());
        SimpleHtmlParser.StartTag tag = (SimpleHtmlParser.StartTag) result.value();

        assertEquals("input", tag.getName());
        assertTrue(tag.isSelfClosing());
        assertEquals("", tag.getAttributes().get("disabled"));
        assertEquals("hello", tag.getAttributes().get("value"));
        assertEquals("42", tag.getAttributes().get("data-id"));
    }

    @Test
    public void testParseAttributesAcrossLinesAndReportsSpan() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("""
            <a
              href="/docs"
              title="Docs">link""");

        assertTrue(result.matches(), () -> result.error());
        assertInstanceOf(SimpleHtmlParser.StartTag.class, result.value());
        SimpleHtmlParser.StartTag tag = (SimpleHtmlParser.StartTag) result.value();

        assertEquals("a", tag.getName());
        assertEquals("/docs", tag.getAttributes().get("href"));
        assertEquals("Docs", tag.getAttributes().get("title"));
        assertEquals(0, tag.getStart());
        assertTrue(tag.getEnd() > tag.getStart());
    }

    @Test
    public void testParseEndTag() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("</div>");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.EndTag.class, result.value());
        SimpleHtmlParser.EndTag tag = (SimpleHtmlParser.EndTag) result.value();
        assertEquals("div", tag.getName());
        assertEquals(0, tag.getStart());
        assertEquals(6, tag.getEnd());
    }

    @Test
    public void testParseComment() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<!-- This is a comment -->");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.Declaration.class, result.value());
        SimpleHtmlParser.Declaration comment = (SimpleHtmlParser.Declaration) result.value();
        assertEquals(" This is a comment ", comment.getAttributeValue("data"));
        assertEquals(0, comment.getStart());
        assertEquals("<!-- This is a comment -->".length(), comment.getEnd());
    }

    @Test
    public void testParseText() {
        Result<SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("Hello, world!");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.TextData.class, result.value());
        SimpleHtmlParser.TextData text = (SimpleHtmlParser.TextData) result.value();
        assertEquals("Hello, world!", text.getText());
    }

    @Test
    public void testParseMultipleElements() {
        List<SimpleHtmlParser.Element> elements = SimpleHtmlParser.parseAll("<div>Hello, world!</div>");
        assertEquals(3, elements.size());

        assertInstanceOf(SimpleHtmlParser.StartTag.class, elements.get(0));
        SimpleHtmlParser.StartTag startTag = (SimpleHtmlParser.StartTag) elements.get(0);
        assertEquals("div", startTag.getName());

        assertInstanceOf(SimpleHtmlParser.TextData.class, elements.get(1));
        SimpleHtmlParser.TextData text = (SimpleHtmlParser.TextData) elements.get(1);
        assertEquals("Hello, world!", text.getText());

        assertInstanceOf(SimpleHtmlParser.EndTag.class, elements.get(2));
        SimpleHtmlParser.EndTag endTag = (SimpleHtmlParser.EndTag) elements.get(2);
        assertEquals("div", endTag.getName());
    }

    @Test
    public void testParseDocumentRejectsMalformedTagInsteadOfSkipping() {
        Result<List<SimpleHtmlParser.Element>> result = SimpleHtmlParser.parseDocument("<div");

        assertFalse(result.matches());
        assertTrue(result.error().contains("expected"));
    }

    @Test
    public void testParseComplexHtml() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test Page</title>
                </head>
                <body>
                    <!-- Header section -->
                    <header id="main-header">
                        <h1>Welcome</h1>
                    </header>
                    <div class="content">
                        <p>This is a <strong>test</strong> paragraph.</p>
                    </div>
                </body>
                </html>""";

        List<SimpleHtmlParser.Element> elements = SimpleHtmlParser.parseAll(html);
        assertFalse(elements.isEmpty());

        // Count the number of each element type
        int startTagCount = 0;
        int endTagCount = 0;
        int textCount = 0;
        int commentCount = 0;

        for (SimpleHtmlParser.Element element : elements) {
            if (element instanceof SimpleHtmlParser.StartTag tag) {
                startTagCount++;
            } else if (element instanceof SimpleHtmlParser.EndTag tag) {
                endTagCount++;
            } else if (element instanceof SimpleHtmlParser.TextData text) {
                textCount++;
            } else if (element instanceof SimpleHtmlParser.Declaration declaration) {
                if (declaration.getName().equals("--")){
                    commentCount++;
                }

            }
        }

        assertTrue(startTagCount > 0, "Should have start tags");
        assertTrue(endTagCount > 0, "Should have end tags");
        assertTrue(textCount > 0, "Should have text data");
        assertEquals(1, commentCount, "Should have one comment");

        // Verify that the number of start and end tags match
        assertEquals(startTagCount, endTagCount, "Number of start and end tags should match");
    }
}
