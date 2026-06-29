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

import io.github.parseworks.taker.results.Match;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for text input diagnostics and recovery behavior. */
public class EnhancedErrorMessagingTest {

    @Test
    public void testTextInputLineAndColumn() {
        TextInput input = (TextInput) Input.of("line 1\nline 2\nline 3");
        
        // Skip to position 8 (line 2, column 2)
        TextInput skipped = (TextInput) input.skip(8);
        
        assertEquals(2, skipped.line());
        assertEquals(2, skipped.column());
        assertEquals("line 2", skipped.getLine(2));
    }
    
    @Test
    public void testTextInputSnippets() {
        TextInput input = (TextInput) Input.of("line 1\nline 2\nline 3");
        
        // Skip to position 8 (line 2, column 2)
        TextInput skipped = (TextInput) input.skip(8);
        
        // Get a snippet around the current position
        String snippet = skipped.getSnippet(3, 3);
        assertEquals("e 1\nlin", snippet);
        
        // Get a formatted snippet
        String formatted = skipped.getFormattedSnippet(1, 1);
        assertTrue(formatted.contains("1 | line 1"));
        assertTrue(formatted.contains("2 | line 2"));
        assertTrue(formatted.contains("3 | line 3"));
        assertTrue(formatted.contains("     ^"));  // Caret marker at column 2
    }
    
    @Test
    public void testErrorRecovery() {
        // Create a parser that recovers from errors
        Taker<String> parser = string("hello")
            .recover(string("world"));
            
        // Test successful parsing with the primary parser
        Result<String> result1 = parser.parse("hello");
        assertTrue(result1.matches());
        assertEquals("hello", result1.value());
        
        // Test recovery with the alternative parser
        Result<String> result2 = parser.parse("world");
        assertTrue(result2.matches());
        assertEquals("world", result2.value());
        
        // Test failure when neither parser matches
        Result<String> result3 = parser.parse("other");
        assertFalse(result3.matches());
    }
    
    @Test
    public void testErrorRecoveryWithFunction() {
        // Create a parser that recovers from errors with a function
        Taker<String> parser = string("hello")
            .recoverWith(failure -> 
                new Match<>("default", failure.input()));
                
        // Test successful parsing with the primary parser
        Result<String> result1 = parser.parse("hello");
        assertTrue(result1.matches());
        assertEquals("hello", result1.value());
        
        // Test recovery with the function
        Result<String> result2 = parser.parse("other");
        assertTrue(result2.matches());
        assertEquals("default", result2.value());
    }

}
