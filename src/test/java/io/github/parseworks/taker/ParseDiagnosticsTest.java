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

package io.github.parseworks.taker;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.*;

class ParseDiagnosticsTest {

    @Test
    void successfulResultsDoNotExposeDiagnostics() {
        Result<String> result = string("ok").parse("ok");

        assertTrue(result.matches());
        assertTrue(result.diagnosticsOptional().isEmpty());
        assertThrows(IllegalStateException.class, result::diagnostics);
    }

    @Test
    void failedResultsExposeStructuredDiagnostics() {
        Result<String> result = string("hello").expecting("greeting").parse("world");

        assertFalse(result.matches());

        ParseDiagnostics diagnostics = result.diagnostics();
        assertEquals(ResultType.NO_MATCH, diagnostics.type());
        assertEquals(0, diagnostics.offset());
        assertEquals(1, diagnostics.line());
        assertEquals(1, diagnostics.column());
        assertEquals("'w'", diagnostics.found());
        assertEquals("greeting", diagnostics.expected().getFirst());
        assertFalse(diagnostics.causes().isEmpty());
    }

    @Test
    void combinedFailuresExposeDistinctExpectations() {
        Result<Character> result = oneOf(
            chr('a').expecting("'a'"),
            chr('b').expecting("'b'")
        ).parse("c");

        ParseDiagnostics diagnostics = result.diagnostics();

        assertEquals(0, diagnostics.offset());
        assertEquals("'c'", diagnostics.found());
        assertEquals(2, diagnostics.expected().size());
        assertTrue(diagnostics.expected().contains("'a'"));
        assertTrue(diagnostics.expected().contains("'b'"));
    }

    @Test
    void rendererIncludesLocationSnippetExpectationAndCauses() {
        Result<String> result = string("port")
            .thenSkip(chr('='))
            .then(string("8080").expecting("port value"))
            .map((key, value) -> key + value)
            .label("assignment")
            .parse("port=oops");

        String rendered = result.diagnostics().render("port=oops");

        assertEquals("assignment", result.diagnostics().contexts().getFirst());
        assertTrue(result.diagnostics().causes().stream()
            .noneMatch(cause -> "assignment".equals(cause.expected())));
        assertTrue(rendered.contains("Parse failed at line 1, column 6"), rendered);
        assertTrue(rendered.contains("port=oops"), rendered);
        assertTrue(rendered.contains("     ^"), rendered);
        assertTrue(rendered.contains("expected port value"), rendered);
        assertTrue(rendered.contains("while parsing assignment"), rendered);
        assertTrue(rendered.contains("causes:"), rendered);
    }

    @Test
    void partialMatchDiagnosticsKeepPartialType() {
        Result<String> result = string("hello").parseAll("hello!");

        ParseDiagnostics diagnostics = result.diagnostics();

        assertEquals(ResultType.PARTIAL, diagnostics.type());
        assertEquals("end of input", diagnostics.expected().getFirst());
        assertTrue(diagnostics.render("hello!").startsWith("Partial match failed"));
    }
}
