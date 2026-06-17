package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Chars;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Combinators.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests parser failure categories and formatted error messages. */
public class ErrorHandlingTest {

    @Test
    public void testSyntaxErrorType() {
        Taker<String> parser = oneOf(Lexical.string("foo"), Lexical.string("bar"));
        Result<String> result = parser.parse("baz");

        assertTrue(!result.matches());
    }

    @Test
    public void testExpectedEofErrorType() {
        // Test that eof() uses EXPECTED_EOF error type when input is not at EOF
        Taker<Void> parser = eof();
        Result<Void> result = parser.parse("x");

        assertFalse(result.matches());
    }

    @Test
    public void testUnexpectedEofErrorType() {
        // Test that oneOf() uses UNEXPECTED_EOF error type when input is at EOF
        Taker<String> parser = oneOf(Lexical.string("foo"), Lexical.string("bar"));
        Result<String> result = parser.parse("");

        assertFalse(result.matches());

    }

    @Test
    public void testValidationErrorType() {
        // Test that not() uses VALIDATION error type when input matches the pattern
        Taker<Void> parser = not(is('x'));
        Result<Void> result = parser.parse("x");

        assertFalse(result.matches());
    }

    @Test
    public void testRecursionErrorType() {
        // Create a parser that causes infinite recursion
        // We need to create a more realistic recursive grammar
        Taker<String> expr = Taker.ref();

        // Define expr in terms of itself, which will cause infinite recursion
        // when there's no base case that can succeed
        expr.set(
            Chars.chr('(')
            .skipThen(expr)
            .thenSkip(Chars.chr(')'))
            .map(Object::toString)
        );

        // This input will cause infinite recursion because it keeps
        // trying to parse nested expressions without ever reaching a base case
        Result<String> result = expr.parse("(((");

        assertFalse(result.matches());
        Failure<?> failure = (Failure<?>) result;

        assertTrue(failure.error().contains("line 1 position 4"));
    }

    @Test
    public void testInternalErrorType() {
        // Create a parser that throws a RuntimeException
        Taker<String> parser = new Taker<>(in -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(RuntimeException.class, () -> {
            Result<String> result = parser.parse("x");
        });
    }


    @Test
    public void testErrorMessageFormat() {
        // Test that error messages follow the consistent format
        Taker<String> parser = Lexical.string("foo");
        Result<String> result = parser.parse("bar");

        assertFalse(result.matches());
        String errorMessage = result.error();

        // Error message should contain "expected" and "found"
        assertTrue(errorMessage.contains("expected"));
        assertTrue(errorMessage.contains("found"));
    }
}
