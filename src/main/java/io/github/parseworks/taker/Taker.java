package io.github.parseworks.taker;

import io.github.parseworks.taker.ApplyBuilder;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Lists;
import io.github.parseworks.taker.impl.IntObjectMap;
import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;
import io.github.parseworks.taker.parsers.Chains;
import io.github.parseworks.taker.parsers.Lexical;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.parseworks.taker.parsers.Chains.chain;
import static io.github.parseworks.taker.parsers.Combinators.is;
/**
 * Core parser class for consuming input of type {@code I} and producing results of type {@code A}.
 * <pre>{@code
 * Taker<Character, Integer> parser = Numeric.integer;
 * }</pre>
 *
 * @param <A> result type
 */
public class Taker<A> implements Function<Input, Result<A>> {

    /**
     * Replaces the result of this parser with a constant value on success.
     * <pre>{@code
     * Lexical.take("true").as(true).value(); // true
     * }</pre>
     *
     * @param value constant value to return
     * @param <R>   result type
     * @return a parser returning the constant value
     */
    public <R> Taker<R> as(R value) {
        return this.skipThen(Lexical.pure(value));
    }

    /**
     * Creates a parser that matches an expression enclosed by matching bracket symbols.
     * <p>
     * This is equivalent to {@code between(bracket, bracket)} and is useful for
     * symmetric delimiters like parentheses, brackets, or quotes.
     *
     * @param bracket the symbol used as both opening and closing delimiter
     * @return a parser that matches content between matching bracket symbols
     */
    public Taker<A> between(char bracket) {
        return between(bracket, bracket);
    }

    /**
     * Creates a parser that matches an expression between distinct opening and closing symbols.
     * <p>
     * This is useful for asymmetric delimiters like XML tags or different bracket styles.
     *
     * @param open the opening delimiter symbol
     * @param close the closing delimiter symbol
     * @return a parser that matches content between the specified delimiters
     */

    public Taker<A> between(char open, char close) {
        return is(open).skipThen(this).thenSkip(is(close));
    }

    /**
     * Applies two parsers in sequence and returns the result of this parser.
     * <pre>{@code
     * Taker<Character, Integer> p = Numeric.integer.thenSkip(Lexical.chr(';'));
     * p.parse("42;").value(); // 42
     * }</pre>
     *
     * @param pb  next parser
     * @param <B> result type of pb
     * @return a parser returning this parser's result
     */
    public <B> Taker<A> thenSkip(Taker<B> pb) {
        return new Taker<>(in -> {
            Result<A> res = this.apply(in);
            if (!res.matches()) return res;
            Result<B> res2 = pb.apply(res.input());
            if (!res2.matches()) return res2.cast();
            return new Match<>(res.value(), res2.input());
        });
    }


    /**
     * Applies two parsers in sequence and returns the result of the second parser.
     * <pre>{@code
     * Taker<Character, Integer> p = Lexical.take("key:").skipThen(Numeric.integer);
     * p.parse("key:42").value(); // 42
     * }</pre>
     *
     * @param pb  next parser
     * @param <B> result type of pb
     * @return a parser returning pb's result
     */
    public <B> Taker<B> skipThen(Taker<B> pb) {
        return new Taker<>(in -> {
            Result<A> res = this.apply(in);
            if (!res.matches()) return res.cast();
            return pb.apply(res.input());
        });
    }


    /**
     * Applies two parsers in sequence and returns an {@link ApplyBuilder} for combining results.
     * <pre>{@code
     * Taker<Character, String> p = Lexical.chr('a').then(Lexical.chr('b'))
     *                                      .map2((a, b) -> "" + a + b);
     * p.parse("ab").value(); // "ab"
     * }</pre>
     *
     * @param next next parser
     * @param <B>  result type of next
     * @return an ApplyBuilder
     */
    public <B> ApplyBuilder<A, B> then(Taker<B> next) {
        return ApplyBuilder.of(this, next);
    }





    /**
     * Tries this parser first, and if it fails, tries the alternative parser.
     * <pre>{@code
     * Taker<Character, Integer> p = Numeric.integer.or(Taker.pure(0));
     * p.parse("42").value(); // 42
     * p.parse("abc").value(); // 0
     * }</pre>
     *
     * @param other alternative parser
     * @return a choice parser
     */
    public Taker<A> or(Taker<A> other) {
        return new Taker<>(in -> {
            Result<A> result = this.apply(in);
            return result.matches() ? result : other.apply(in);
        });
    }

    public Taker<A> chainRightOneOrMore(Taker<BinaryOperator<A>> op) {
        return chain(this, op, Chains.Associativity.RIGHT);
    }

    public Taker<List<A>> zeroOrMore() { return repeat(0, Integer.MAX_VALUE); }
    public Taker<List<A>> oneOrMore() { return repeat(1, Integer.MAX_VALUE); }
    public Taker<List<A>> oneOrMoreUntil(Taker<?> until) { return repeatInternal(1, Integer.MAX_VALUE, until); }


    /**
     * Succeeds only if validation succeeds without consuming input.
     * <pre>{@code
     * Taker<Character, Integer> p = Numeric.integer.onlyIf(Lexical.chr('+'));
     * p.parse("+123").value(); // 123
     * p.parse("-123").matches(); // false
     * }</pre>
     *
     * @param validation validation parser
     * @param <B>        validation result type
     * @return a conditional parser
     */
    public <B> Taker<A> onlyIf(Taker<B> validation) {
        return new Taker<>(input -> {
            Result<B> validationResult = validation.apply(input);
            if (!validationResult.matches()) {
                return validationResult.cast();
            }
            return this.apply(input);
        });
    }

    /**
     * Succeeds if followed by lookahead without consuming lookahead input.
     * <pre>{@code
     * Taker<Character, String> p = Lexical.word.peek(Lexical.chr('='));
     * p.parse("id=42").value(); // "id"
     * }</pre>
     *
     * @param lookahead lookahead parser
     * @param <B>       lookahead result type
     * @return a lookahead parser
     */
    public <B> Taker<A> peek(Taker<B> lookahead) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (!result.matches()) return result;
            Result<B> peek = lookahead.apply(result.input());
            return peek.matches() ? result : new NoMatch<>(input, "Expected lookahead", (NoMatch<?>) peek);
        });
    }


    /**
     * Creates a parser that logs its progress and results to standard output while behaving exactly like this parser.
     * <p>
     * The {@code logSystemOut} method wraps this parser with logging functionality that prints information about:
     * <ul>
     *   <li>The input position where parsing starts</li>
     *   <li>Whether parsing succeeded or failed</li>
     *   <li>The parsed value (on success) or error message (on failure)</li>
     * </ul>
     * <p>
     * This method is particularly useful for:
     * <ul>
     *   <li>Debugging parser behavior</li>
     *   <li>Understanding why certain inputs fail to parse</li>
     *   <li>Tracing the execution of complex parser combinations</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser for integers with logging
     * Taker<Character, Integer> debugParser = Numeric.integer.logSystemOut();
     *
     * // When parsing "123", outputs:
     * // Taker starting at position: 0 succeeded with value: 123
     *
     * // When parsing "abc", outputs:
     * // Taker starting at position: 0 failed: Expected digit but found 'a'
     * }</pre>
     *
     * @return a new parser that logs its progress while behaving like this parser
     * @see Result for the structure of success and failure results that are logged
     */
    public Taker<A> logSystemOut() {
        return new Taker<>(input -> {
            System.out.print("Taker starting at position: " + input.position());
            Result<A> result = this.apply(input);
            if (result.matches()) {
                System.out.println(" succeeded with value: " + result.value());
            } else {
                System.out.println(" failed: " + result.error());
            }
            return result;
        });
    }



    /**
     * Creates a parser that always succeeds, optionally containing this parser's result.
     * <p>
     * The {@code optional} method creates a parser that attempts to apply this parser, but always
     * succeeds regardless of the result. The parsing process works as follows:
     * <ol>
     *   <li>First attempts to apply this parser to the input</li>
     *   <li>If this parser succeeds, its result is wrapped in a non-empty {@link Optional}</li>
     *   <li>If this parser fails, an empty {@link Optional} is returned without consuming any input</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing optional elements in a grammar, such as optional
     * parameters, modifiers, or any syntax structures that may or may not be present. It provides
     * a convenient way to handle the presence or absence of elements without disrupting the overall
     * parsing flow.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines {@link #map(Function)} with {@link #orElse(Object)} to achieve the optional behavior</li>
     *   <li>Always succeeds, never causing parsing failure</li>
     *   <li>No input is consumed when the parser fails</li>
     *   <li>The result type is transformed from {@code A} to {@code Optional<A>}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse signed numbers
     * Taker<Character, Integer> number = Numeric.integer;
     * Taker<Character, Integer> signedNumber = Lexical.chr('-').optional().then(number)
     *     .map2((sign, num) -> sign.isPresent() ? -num : num);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with -42 for input "-42"
     * // Fails for input "abc" (no number found)
     * }</pre>
     *
     * @return a parser that always succeeds, returning either an Optional containing this
     * parser's result or an empty Optional
     * @see #orElse(Object) for providing a default value instead of an Optional
     * @see #zeroOrMore() for collecting zero or more occurrences of a pattern
     */
    public Taker< Optional<A>> optional() {
        return this.map(Optional::of).orElse(Optional.empty());
    }

    /**
     * Creates a parser that provides a default value if this parser fails.
     * <p>
     * The {@code orElse} method provides a way to handle parser failures by substituting a default value
     * rather than propagating the failure. This is different from the {@code or} method which tries an
     * alternative parsing strategy.
     * <p>
     * When applied to input:
     * <ol>
     *   <li>First attempts to apply this parser to the input</li>
     *   <li>If this parser succeeds, its result is returned</li>
     *   <li>If this parser fails, a success result containing the default value is returned</li>
     * </ol>
     * <p>
     * Important: When returning the default value, the input position remains unchanged from the original position.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse an integer, or use 0 as default if parsing fails
     * Taker<Character, Integer> parser = Numeric.integer.orElse(0);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with 0 for input "abc"
     * }</pre>
     *
     * @param other the default value to return if this parser fails
     * @return a parser that returns either the successful parse result or the default value
     */
    public Taker<A> orElse(A other) {
        return new Taker<>(in -> {
            Result<A> result = this.apply(in);
            return result.matches() ? result : new Match<>(other, in);
        });
    }

    /**
     * Parses the input without requiring that the entire input is consumed.
     *
     * @param in the input to parse
     * @return the result of parsing the input
     */
    public Result<A> parse(Input in) {
        return parse(in, false);
    }

    /**
     * Creates an iterator that incrementally parses the input, allowing for streaming processing of parse results.
     * <p>
     * The {@code iterateParse} method provides a way to parse input incrementally by returning an iterator
     * that processes the input one element at a time. This is particularly useful when:
     * <ul>
     *   <li>Processing large inputs that shouldn't be parsed all at once</li>
     *   <li>Implementing streaming or lazy parsing scenarios</li>
     *   <li>Searching for multiple occurrences of a pattern in the input</li>
     * </ul>
     * <p>
     * The iterator works as follows:
     * <ol>
     *   <li>Attempts to parse the input at the current position using this parser</li>
     *   <li>If parsing succeeds, returns the parsed value and advances the input position</li>
     *   <li>If parsing fails, skips one character and tries again at the next position</li>
     *   <li>Continues until the end of input is reached</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The iterator maintains its own input position state</li>
     *   <li>Results are computed lazily when {@code hasNext()} or {@code next()} is called</li>
     *   <li>Failed parse attempts are skipped automatically</li>
     *   <li>The iterator follows the standard Java Iterator contract</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that matches integers
     * Taker<Character, Integer> intParser = integer;
     *
     * // Create an input from a string containing mixed content
     * Input<Character> input = Input.fromString("123 abc 456 def 789");
     *
     * // Iterate over all integers in the input
     * Iterator<Integer> numbers = intParser.iterateParse(input);
     * while (numbers.hasNext()) {
     *     Integer number = numbers.next();
     *     System.out.println(number); // Prints: 123, 456, 789
     * }
     * }</pre>
     *
     * @param input the input to parse
     * @return an iterator that yields parse results one at a time
     * @throws IllegalArgumentException if the input is null
     * @see Input for input handling
     * @see Result for parse result handling
     */
    public Iterator<A> iterateParse(Input input) {
        final Taker<A> parser = this;
        return new Iterator<>() {
            private Input currentInput = input;
            private Result<A> nextResult = null;


            @Override
            public boolean hasNext() {
                if (currentInput.isEof()) {
                    return false;
                }

                if (nextResult != null) {
                    return true;
                }

                // Try to find the next valid result
                while (!currentInput.isEof()) {
                    Result<A> result = parser.parse(currentInput, false);
                    if (result.matches()) {
                        nextResult = result;
                        return true;
                    }
                    // Skip one character and try again
                    currentInput = currentInput.next();
                }

                return false;
            }

            @Override
            public A next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements to parse");
                }

                // We already have the next result from hasNext()
                A value = nextResult.value();
                currentInput = nextResult.input();
                nextResult = null;
                return value;
            }
        };
    }

    /**
     * Creates a Stream that lazily parses the input, providing a modern streaming interface for parse results.
     * <p>
     * The {@code streamParse} method converts the parsing process into a Java Stream, allowing for:
     * <ul>
     *   <li>Functional-style processing of parse results</li>
     *   <li>Lazy evaluation of parse operations</li>
     *   <li>Integration with the Java Stream API</li>
     *   <li>Composition with other stream operations</li>
     * </ul>
     * <p>
     * The returned Stream has the following characteristics:
     * <ul>
     *   <li>ORDERED - elements are processed in the order they appear in the input</li>
     *   <li>NONNULL - all elements are guaranteed to be non-null</li>
     *   <li>Non-parallel - parsing is performed sequentially</li>
     *   <li>Unknown size - the number of elements is not known in advance</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser for integers
     * Taker<Character, Integer> intParser = intr;
     * Input<Character> input = Input.fromString("123 456 789");
     *
     * // Process integers using stream operations
     * intParser.streamParse(input)
     *         .filter(n -> n > 200)
     *         .map2(n -> n * 2)
     *         .forEach(System.out::println);
     *
     * // Collect all numbers into a list
     * List<Integer> numbers = intParser.streamParse(input)
     *                                 .collect(Collectors.toList());
     * }</pre>
     * <p>
     * Note that this method internally uses {@link #iterateParse(Input)} and wraps it in a Stream.
     * The stream will automatically close any resources associated with the parsing process when
     * the stream is closed or fully consumed.
     *
     * @param input the input to parse
     * @return a Stream containing the parse results
     * @throws IllegalArgumentException if the input is null
     * @see #iterateParse(Input) for the underlying iterator implementation
     * @see Stream for available stream operations
     */
    public  Stream<A> streamParse(Input input) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterateParse(input),
                Spliterator.ORDERED | Spliterator.NONNULL
            ),
            false
        );
    }

    /**
     * Parses the input with an option to require complete input consumption.
     * <p>
     * The {@code parse} method is the core parsing method that applies this parser to the given input.
     * It serves as the foundation for all other parsing methods in this class. The parsing process
     * works as follows:
     * <ol>
     *   <li>Applies this parser to the provided input</li>
     *   <li>If the parser succeeds and {@code consumeAll} is {@code true}, verifies that all input has been consumed</li>
     *   <li>Returns a {@link Result} object containing either the successful parse result or an error</li>
     * </ol>
     * <p>
     * The {@code consumeAll} parameter provides control over whether parsing should succeed only when
     * the entire input is processed. This is useful for distinguishing between:
     * <ul>
     *   <li>Complete parsing: When you expect the parser to consume all available input</li>
     *   <li>Partial parsing: When you want to parse just a portion of the input, leaving the rest for later processing</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>First applies this parser using the {@link #apply(Input)} method</li>
     *   <li>If {@code consumeAll} is {@code true} and the parser succeeds but doesn't consume all input,
     *       returns a failure result with an "eof" error message</li>
     *   <li>Thread-safe, maintaining parser immutability</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a complete JSON document
     * Taker<Character, JsonValue> jsonParser = // ... json parser definition
     * Input<Character> input = Input.of("{\"name\":\"John\",\"age\":30}");
     * Result<Character, JsonValue> result = jsonParser.parse(input, true);
     * // result.matches() == true only if the entire JSON document was valid and fully consumed
     *
     * // Parse just a number from the beginning of input
     * Taker<Character, Integer> numberParser = intr;
     * Input<Character> partialInput = Input.of("42 and more text");
     * Result<Character, Integer> partialResult = numberParser.parse(partialInput, false);
     * // partialResult.matches() == true, partialResult.value() == 42
     * }</pre>
     *
     * @param in         the input to parse
     * @param consumeAll whether to require that the entire input is consumed
     * @return the result of parsing the input
     * @see #parse(Input) for parsing without requiring complete input consumption
     * @see #parseAll(Input) for parsing with mandatory complete input consumption
     * @see Result for the structure of successful and failure results
     */
    public Result<A> parse(Input in, boolean consumeAll) {
        Result<A> result = this.apply(in);
        if (consumeAll && result.matches()) {
            if (!result.input().isEof()) {
                return new NoMatch<>(result.input(), "end of input");
            }
        }
        return result;
    }

    @Override
    public Result<A> apply(Input in) {
        return applyHandler.apply(in);
    }

    /**
     * Parses the input string without requiring that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    @SuppressWarnings("unchecked")
    public Result<A> parse(CharSequence input) {
        return parse((Input) Input.of(input), false);
    }

    /**
     * Parses the input and ensures that the entire input is consumed.
     * <p>
     * The {@code parseAll} method applies this parser to the given input and verifies
     * that no unconsumed input remains. It is useful for scenarios where the input
     * must be fully parsed without leaving any trailing characters.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>Applies this parser to the provided input</li>
     *   <li>If the parser succeeds, checks whether the input has been fully consumed</li>
     *   <li>If any unconsumed input remains, the method returns a failure result</li>
     *   <li>If the parser fails or unconsumed input exists, an error is returned</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a complete integer from the input
     * Taker<Character, Integer> intParser = Numeric.integer;
     * Input<Character> input = Input.of("123");
     * Result<Character, Integer> result = intParser.parseAll(input);
     *
     * if (result.matches()) {
     *     Integer value = result.value(); // Successfully parsed value
     * } else {
     *     String error = result.error(); // Error message for failure
     * }
     * }</pre>
     *
     * @param in the input to parse
     * @return the result of parsing the input, ensuring the entire input is consumed
     * @see #parse(Input) for parsing without requiring complete input consumption
     * @see #parse(Input, boolean) for parsing with an explicit consumeAll flag
     */
    public Result<A> parseAll(Input in) {
        return parse(in, true);
    }

    /**
     * Parses the input string and ensures that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    @SuppressWarnings("unchecked")
    public Result<A> parseAll(CharSequence input) {
        return parse((Input) Input.of(input), true);
    }

    public Taker<List<A>> repeat(int target) { return repeatInternal(target, target, null); }
    public Taker<List<A>> repeat(int min, int max) { return repeatInternal(min, max, null); }

    public Taker<List<A>> repeatAtLeast(int target) { return repeatInternal(target, Integer.MAX_VALUE, null); }
    public Taker<List<A>> repeatAtMost(int max) { return repeatInternal(0, max, null); }

    /**
     * Creates a parser that parses a potentially empty sequence of elements separated by a delimiter.
     * <p>
     * The {@code separatedByZeroOrMany} method creates a parser that matches elements using this parser,
     * with each element separated by the specified separator parser. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to parse a first element using this parser</li>
     *   <li>If the first element is found, then repeatedly tries to parse a separator followed by another element</li>
     *   <li>Collects all parsed elements (ignoring separators) into an {@code FList}</li>
     *   <li>Returns an empty list if no elements are found (unlike {@link #oneOrMoreSeparatedBy(Taker)} which requires at least one element)</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing common data formats like comma-separated lists,
     * space-delimited tokens, or any syntax that involves items separated by delimiters, where empty
     * collections are valid. Examples include empty parameter lists, empty arrays, or optional sequences.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Builds on {@link #oneOrMoreSeparatedBy(Taker)} but succeeds even when no elements are found</li>
     *   <li>Only the elements are collected; separator values are discarded</li>
     *   <li>Always succeeds, returning an empty list if no elements match</li>
     *   <li>The input position remains unchanged if no elements are found</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a comma-separated list of numbers, allowing empty lists
     * Taker<Character, Integer> number = Numeric.integer;
     * Taker<Character, Character> comma = Lexical.chr(',');
     * Taker<Character, List<Integer>> optionalList = number.zeroOrMoreSeparatedBy(comma);
     *
     * // Succeeds with [1,2,3] for input "1,2,3"
     * // Succeeds with [] for input "" (empty list)
     * // Succeeds with [42] for input "42"
     * // Succeeds with [] for input ";" (empty list, no input consumed)
     * }</pre>
     *
     * @param sep   the parser that recognizes the separator between elements
     * @param <SEP> the type of the separator parse result (which is discarded)
     * @return a parser that parses zero or more elements separated by the given separator
     * @throws IllegalArgumentException if the separator parser is null
     * @see #oneOrMoreSeparatedBy(Taker) for a version that requires at least one element
     * @see #zeroOrMore() for collecting repeated elements without separators
     * @see #repeat(int, int) for collecting a specific range of elements
     */
    public <SEP> Taker<List<A>> zeroOrMoreSeparatedBy(Taker<SEP> sep) {
        return this.oneOrMoreSeparatedBy(sep).orElse(List.of());
    }

    /**
     * Transforms the result of this parser using the given function.
     * <pre>{@code
     * Taker<Character, Integer> p = Lexical.chr('5').map2(Character::getNumericValue);
     * p.parse("5").value(); // 5
     * }</pre>
     *
     * @param func transformation function
     * @param <R>  transformed result type
     * @return a transformed parser
     */
    public <R> Taker< R> map(Function<A, R> func) {
        return new Taker<>(in -> apply(in).map(func));
    }

    /**
     * Creates a parser that parses a non-empty sequence of elements separated by a delimiter.
     * <p>
     * The {@code oneOrMoreSeparatedBy} method creates a parser that matches elements using this parser,
     * with each element separated by the specified separator parser. The parsing process works as follows:
     * <ol>
     *   <li>First parses an initial element using this parser (required)</li>
     *   <li>Then repeatedly tries to parse a separator followed by another element</li>
     *   <li>Collects all parsed elements (ignoring separators) into an {@code FList}</li>
     *   <li>Succeeds only if at least one element is found</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing common data formats like comma-separated lists,
     * space-delimited tokens, or any syntax that requires at least one element with optional additional
     * elements separated by delimiters. Examples include non-empty parameter lists, argument sequences,
     * or any collection that must contain at least one item.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The first element is parsed with this parser</li>
     *   <li>Subsequent elements are parsed as pairs of separator followed by element</li>
     *   <li>Only the elements are collected; separator values are discarded</li>
     *   <li>Fails if no elements can be parsed</li>
     *   <li>The input position is advanced after each successful match</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a comma-separated list of numbers, requiring at least one number
     * Taker<Character, Integer> number = Numeric.integer;
     * Taker<Character, Character> comma = Lexical.chr(',');
     * Taker<Character, List<Integer>> numberList = number.oneOrMoreSeparatedBy(comma);
     *
     * // Succeeds with [1,2,3] for input "1,2,3"
     * // Succeeds with [42] for input "42"
     * // Fails for input "" (empty input)
     * // Fails for input "," (no elements found)
     * }</pre>
     *
     * @param sep   the parser that recognizes the separator between elements
     * @param <SEP> the type of the separator parse result (which is discarded)
     * @return a parser that parses one or more elements separated by the given separator
     * @throws IllegalArgumentException if the separator parser is null
     * @see #zeroOrMoreSeparatedBy(Taker) for a version that allows empty sequences
     * @see #oneOrMore() for collecting repeated elements without separators
     * @see #repeat(int, int) for collecting a specific range of elements
     */
    public <SEP> Taker<List<A>> oneOrMoreSeparatedBy(Taker<SEP> sep) {
        return this.then(sep.skipThen(this).zeroOrMore()).map2((a, l) -> Lists.prepend(a, l));
    }

    /**
     * Initializes a parser reference with another parser's behavior.
     */
    public synchronized void set(Taker<A> parser) {
        if (parser == null) throw new IllegalArgumentException("parser cannot be null");
        if (this.applyHandler != defaultApplyHandler) throw new IllegalStateException("Taker already initialized");
        this.applyHandler = parser.applyHandler;
    }

    /**
     * Initializes a parser reference with a custom apply handler function.
     * <p>
     * The {@code set} method initializes a parser reference created by {@link #ref()} with a
     * custom function that defines the parsing behavior. This provides more control than
     * {@link #set(Taker)} by allowing direct specification of the parsing logic. The method
     * works as follows:
     * <ol>
     *   <li>Takes a function that defines how the parser should process input</li>
     *   <li>Sets this function as the parser's apply handler</li>
     *   <li>Can only be called once on a given parser reference</li>
     * </ol>
     * <p>
     * This method is primarily used for creating advanced recursive parsers where more control
     * is needed over the parsing behavior than simply using an existing parser. It provides
     * direct access to the core parsing mechanism for implementing custom parser logic.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Thread-safe with synchronized access to prevent concurrent initialization</li>
     *   <li>Throws an exception if the parser is already initialized</li>
     *   <li>The apply handler function should handle input appropriately and return valid results</li>
     *   <li>Must be called before the reference parser is used for parsing</li>
     * </ul>
     * <p>
     * Example usage for a custom recursive parser:
     * <pre>{@code
     * // Create a forward reference for an expression parser
     * Taker<Character, Integer> expr = Taker.ref();
     *
     * // Define parsers for basic components
     * Taker<Character, Integer> number = intr;
     * Taker<Character, Character> plus = chr('+');
     * Taker<Character, Character> openParen = chr('(');
     * Taker<Character, Character> closeParen = chr(')');
     *
     * // Define a custom apply handler for the expression parser
     * expr.set(input -> {
     *     // First try to parse a number
     *     Result<Character, Integer> numResult = number.apply(input);
     *     if (numResult.matches()) {
     *         return numResult;
     *     }
     *
     *     // Then try to parse a parenthesized expression
     *     if (input.atEnd() || input.value() != '(') {
     *         return new NoMatch<>(input, "Expected number or expression");
     *     }
     *
     *     // Parse: (expr + expr)
     *     Input<Character> afterOpen = input.advance(1);
     *     Result<Character, Integer> leftResult = expr.apply(afterOpen);
     *     if (!leftResult.matches()) {
     *         return leftResult;
     *     }
     *
     *     Result<Character, Character> opResult = plus.apply(leftResult.input());
     *     if (!opResult.matches()) {
     *         return new NoMatch<>(leftResult.input(), "Expected '+'");
     *     }
     *
     *     Result<Character, Integer> rightResult = expr.apply(opResult.input());
     *     if (!rightResult.matches()) {
     *         return rightResult;
     *     }
     *
     *     Result<Character, Character> closeResult = closeParen.apply(rightResult.input());
     *     if (!closeResult.matches()) {
     *         return new NoMatch<>(rightResult.input(), "Expected ')'");
     *     }
     *
     *     int resultValue = leftResult.value() + rightResult.value();
     *     return new Match<>(resultValue, closeResult.input());
     * });
     *
     * // Now expr can parse recursive expressions like "5" or "(1+2)" or "((1+2)+3)"
     * }</pre>
     *
     * @param applyHandler the function that defines the parser's behavior
     * @throws IllegalArgumentException if the applyHandler is null
     * @throws IllegalStateException    if this parser has already been initialized
     * @see #ref() for creating an uninitialized parser reference
     * @see #set(Taker) for initializing with another parser's behavior
     * @see Result for the result type that should be returned by the apply handler
     * @see Input for the input type that will be provided to the apply handler
     */
    public synchronized void set(Function<Input, Result<A>> applyHandler) {
        if (applyHandler == null) throw new IllegalArgumentException("handler cannot be null");
        if (this.applyHandler != defaultApplyHandler) throw new IllegalStateException("Taker already initialized");
        this.applyHandler = applyHandler;
    }

    /**
     * Creates a repeating parser that applies this parser zero or more times until a terminator parser succeeds.
     */
    public Taker<List<A>> zeroOrMoreUntil(Taker<?> terminator) {
        return repeatInternal(0, Integer.MAX_VALUE, terminator);
    }

    /**
     * Creates a repeating parser that applies this parser a specified number of times.
     */
    private Taker<List<A>> repeatInternal(int min, int max, Taker<?> until) {
        return new Taker<>(in -> {
            List<A> buffer = new ArrayList<>();
            Input current = in;
            while (buffer.size() < max) {
                if (until != null) {
                    Result<?> termRes = until.apply(current);
                    if (termRes.matches()) {
                        if (buffer.size() < min) return new NoMatch<>(current, "at least " + min + " items");
                        return new Match<>(List.copyOf(buffer), termRes.input());
                    }
                }
                if (current.isEof()) {
                    if (buffer.size() >= min && until == null) return new Match<>(List.copyOf(buffer), current);
                    return new NoMatch<>(current, min + " repetitions (EOF)");
                }
                Result<A> res = this.apply(current);
                if (!res.matches()) {
                    if (res.input().position() > current.position() || until != null) return res.cast();
                    if (buffer.size() >= min) return new Match<>(List.copyOf(buffer), current);
                    return new NoMatch<>(current, "at least " + min + " repetitions", (NoMatch<?>) res);
                }
                if (current.position() == res.input().position()) return new NoMatch<>(current, "infinite loop");
                buffer.add(res.value());
                current = res.input();
            }
            return new Match<>(List.copyOf(buffer), current);
        });
    }

    /**
     * A function that defines how this parser applies to an input.
     * <p>
     * This function is the core of the parser, defining how it processes input and produces results.
     * It takes an {@link Input} object representing the current parsing state and returns a {@link Result}
     * object containing either a successful parse result or an error.
     */
    protected Function<Input, Result<A>> applyHandler;
    /**
     * A default apply handler that throws an exception if the parser is not initialized.
     */
    private Function<Input, Result<A>> defaultApplyHandler;


    /**
     * Private constructor to create a parser reference that can be initialized later.
     */
    protected Taker() {
        this.applyHandler = defaultApplyHandler = in -> {
            throw new IllegalStateException("Taker not initialized");
        };
    }

    /**
     * Creates a new parser with the specified apply handler function.
     * <p>
     * The {@code Taker} constructor creates a parser that uses the provided function to process input and
     * produce results. The apply handler is the core parsing function that defines how this parser
     * transforms input into parsed results. The function works as follows:
     * <ol>
     *   <li>Receives an {@link Input} object representing the current parsing state</li>
     *   <li>Processes the input according to the parser's grammar rules</li>
     *   <li>Returns a {@link Result} object containing either a Match or a NoMatch result</li>
     * </ol>
     * <p>
     * This constructor is the primary way to create custom parsers with specific parsing logic. Most users
     * will not need to use this constructor directly, instead using combinators and factory methods to
     * build parsers from simpler components.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The apply handler should return a Match result with the parsed value if parsing succeeds</li>
     *   <li>The apply handler should return a NoMatch result with an error message if parsing fails</li>
     *   <li>The apply handler is responsible for properly advancing the input position on success</li>
     *   <li>Thread safety is maintained as parsers are immutable after construction</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a custom parser that recognizes a specific pattern
     * Taker<Character, String> customParser = new Taker<>(input -> {
     *     if (input.atEnd()) {
     *         return new NoMatch<>(input, "Unexpected end of input");
     *     }
     *
     *     // Check for a specific pattern
     *     if (input.value() == 'a' && input.value(1) == 'b') {
     *         return new Match<>("ab", input.advance(2));
     *     }
     *
     *     return new NoMatch<>(input, "Expected 'ab'");
     * });
     * }</pre>
     *
     * @param applyHandler the function that defines this parser's behavior
     * @throws IllegalArgumentException if the applyHandler is null
     * @see #apply(Input) for the method that uses this handler to parse input
     * @see Result for the result type returned by the apply handler
     * @see Input for the input type consumed by the apply handler
     */
    public Taker(Function<Input, Result<A>> applyHandler) {
        this.applyHandler = Objects.requireNonNull(applyHandler);
    }

    /**
     * Creates a reference to a parser that can be initialized later, enabling recursive grammar definitions.
     * <p>
     * The {@code ref} method addresses the challenge of defining recursive parsers by creating a
     * placeholder parser that can be initialized after its creation. This allows for handling
     * self-referential grammar rules that would otherwise cause initialization issues. The process
     * works as follows:
     * <ol>
     *   <li>Create a parser reference using {@code ref()}</li>
     *   <li>Use this reference in other parser definitions as needed</li>
     *   <li>Later, initialize the reference using {@link #set(Taker)} or {@link #set(Function)}</li>
     * </ol>
     * <p>
     * This technique is essential for parsing recursive structures such as:
     * <ul>
     *   <li>Nested expressions (e.g., arithmetic expressions with parentheses)</li>
     *   <li>Recursive data structures (e.g., JSON objects containing other JSON objects)</li>
     *   <li>Self-referential grammar rules (e.g., a term that can contain other terms)</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Creates a parser with a default handler that throws an exception if used before initialization</li>
     *   <li>Must be initialized with {@link #set(Taker)} or {@link #set(Function)} before use</li>
     *   <li>Thread-safe, allowing for concurrent parser creation and initialization</li>
     *   <li>Cannot be reinitialized after being set once</li>
     * </ul>
     * <p>
     * Example usage for parsing nested arithmetic expressions:
     * <pre>{@code
     * // Create a forward reference for an expression parser
     * Taker<Character, Integer> expr = Taker.ref();
     *
     * // Define a parser for simple numbers
     * Taker<Character, Integer> number = intr;
     *
     * // Define a parser for parenthesized expressions using the reference
     * Taker<Character, Integer> parens = expr.between('(', ')');
     *
     * // Define operators
     * Taker<Character, BinaryOperator<Integer>> addOp =
     *     chr('+').as((a, b) -> a + b);
     *
     * // Now initialize the expression parser with a definition that references itself
     * expr.set(number.or(parens).chainLeftOneOrMore(addOp));
     *
     * // Can now parse recursive expressions like "1+(2+3)"
     * }</pre>
     *
     * @param <A> the type of the parsed value
     * @return a new uninitialized parser reference
     * @throws IllegalStateException if the parser is used before being initialized
     * @see #set(Taker) for initializing the parser reference with another parser
     * @see #set(Function) for initializing the parser reference with an apply handler
     */
    public static < A> Taker<A> ref() {
        return new CheckParser<>();
    }

    private static class CheckParser<A> extends Taker<A> {

        private final ThreadLocal<IntObjectMap<ArrayDeque<Taker<?>>>> contextLocal =
            ThreadLocal.withInitial(IntObjectMap::new);

        @Override
        public Result<A> apply(Input in) {
            int pos = in.position();
            IntObjectMap<ArrayDeque<Taker<?>>> ctx = contextLocal.get();

            ArrayDeque<Taker<?>> stack = ctx.get(pos);
            if (stack == null) {
                stack = new ArrayDeque<>();
                ctx.put(pos, stack);
            }

            for (Taker<?> p : stack) {
                if (p == this) {
                    return new NoMatch<>(in, "no infinite recursion");
                }
            }

            stack.push(this);
            try {
                return applyHandler.apply(in);
            } catch (RuntimeException e) {
                return new NoMatch<>(in, "parser to function correctly");
            } finally {
                stack.pop();
                if (stack.isEmpty()) {
                    ctx.remove(pos);
                }
            }
        }
    }

    /**
     * Creates a parser that attempts to recover from errors by trying an alternative parser.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the recovery parser is applied
     * to the same input position.
     * <p>
     * This is useful for error recovery in situations where there are multiple valid alternatives,
     * and you want to try them in sequence.
     *
     * @param recovery the parser to try if this parser fails
     * @param <B> the result type of the recovery parser
     * @return a new parser that tries the recovery parser if this parser fails
     */
    public <B> Taker<B> recover(Taker<B> recovery) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply(input);
        });
    }

    /**
     * Creates a parser that attempts to recover from errors by applying a function
     * to the failure result.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the recovery function is applied
     * to the failure result to produce a new result.
     * <p>
     * This is useful for custom error recovery strategies, such as:
     * <ul>
     *   <li>Transforming errors into default values</li>
     *   <li>Implementing domain-specific error recovery logic</li>
     * </ul>
     *
     * @param recovery the function to apply to the failure result
     * @param <B> the result type of the recovery function
     * @return a new parser that applies the recovery function if this parser fails
     */
    public <B> Taker<B> recoverWith(Function<NoMatch<A>, Result<B>> recovery) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply((NoMatch< A>) result);
        });
    }

    /**
     * Labels this parser with a human-readable expectation for error messages.
     * <pre>{@code
     * Taker<Character, String> p = Lexical.word.expecting("identifier");
     * p.parse("123").matches(); // false, error: "Expected identifier"
     * }</pre>
     *
     * @param label descriptive label
     * @return a labeled parser
     */
    public <B> Taker<A> expecting(String label) {
        return new Taker<>(input -> {
            Result<A> result = this.apply(input);
            if (result.matches()) return result;
            return new NoMatch<>(input, label, (Failure<?>) result);
        });
    }

    /**
     * This parser applies a function to the result of this parser, returning a new parser.
     * This is a monadic operation that allows chaining parsers based on the result of the current parser.
     * The following example shows where we want to parse a number of characters exactly, based on the initial number
     * provided.
     * This allows for dynamic parsing based on the result of the current parser.
     * <pre>{@code
     * Taker<Character, String> p = Numeric.unsignedInteger.flatMap(n ->
     *     Lexical.chr(',').skipThen(Lexical.chr('a').repeat(n)).map2(Lists::join)
     * );
     * p.parse("3,aaa").value(); // "aaa"
     * }</pre>
     *
     * @param f   function returning the next parser
     * @param <B> next result type
     * @return a monadic parser
     */
    public <B> Taker<B> flatMap(Function<A, Taker<B>> f) {
        if (f == null) {
            throw new IllegalArgumentException("flatMap function cannot be null");
        }
        return new Taker<>(in -> {
            Result<A> r = this.apply(in);
            if (!r.matches()) {
                // propagate the original failure
                return r.cast();
            }
            Taker<B> next = f.apply(r.value());
            if (next == null) {
                // be defensive to help users diagnose nulls
                return new NoMatch<B>(r.input(), "parser to function correctly").cast();
            }
            Result<B> rb = next.apply(r.input());
            if (!rb.matches()) {
                return rb;
            }
            return rb;
        });
    }

}
