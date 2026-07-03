package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Chars;
import io.github.parseworks.taker.parsers.Combinators;
import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.parsers.Lexical;
import io.github.parseworks.taker.parsers.Numeric;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

/**
 * Manual-only error display tests. These tests are tagged {@code manual} so
 * standard Maven test runs exclude them. Execute individually to inspect error
 * messages and diagnostics.
 *
 * <pre>
 *   mvn test "-Dtest=ErrorDisplayTest#simpleMismatch" "-Dtaker.excludedTestGroups="
 * </pre>
 */
@Tag("manual")
class ErrorDisplayTest {

    /* ── simple parse failures ─────────────────────────────────────── */

    @Test
    void simpleMismatch() {
        // Expected: clean "expected 'a'" at position 0
        Taker<Character> p = Chars.oneOf("a");
        printResult(p.parse(Inputs.of("b c d")));
    }

    @Test
    void eofBeforeExpected() {
        // Expected: "expected 'a'" at EOF
        Taker<Character> p = Chars.oneOf("a");
        printResult(p.parse(Inputs.of("")));
    }

    @Test
    void stringPartialMatch() {
        // Expected: "expected 'c'" at position 2 (matched "ab")
        Taker<String> p = Lexical.string("abc");
        printResult(p.parse(Inputs.of("abx")));
    }

    /* ── choice / oneOf failures ───────────────────────────────────── */

    @Test
    void oneOfMultipleAlternatives() {
        // Expected: all three failures reported at position 0
        Taker<Character> p = Combinators.oneOf(
            Chars.oneOf("x"),
            Chars.oneOf("y"),
            Chars.oneOf("z")
        );
        printResult(p.parse(Inputs.of("a")));
    }

    @Test
    void oneOfWithAdvancedFailure() {
        // Expected: farthest failure position is from the "abc" match
        Taker<String> p = Combinators.oneOf(
            Lexical.string("abc"),    // matches "ab", fails at pos 2
            Lexical.string("a"),      // matches at pos 0
            Lexical.string("a")       // matches at pos 0
        );
        printResult(p.parse(Inputs.of("abx")));
    }

    /* ── nested / composed failures ────────────────────────────────── */

    @Test
    void nestedFailure() {
        // Expected: failure propagates through map
        Taker<Integer> p = Numeric.integer.map(Math::abs);
        printResult(p.parse(Inputs.of("not-a-number")));
    }

    @Test
    void sequenceFailure() {
        // Expected: failure at position where sequence breaks
        Taker<Integer> p = Numeric.integer
            .then(Chars.oneOf(","))
            .then(Numeric.integer)
            .map((a, sep, b) -> a + b);
        printResult(p.parse(Inputs.of("42 x")));
    }

    /* ── recursion errors ──────────────────────────────────────────── */

    @Test
    void recursionGuardError() {
        // Expected: "no infinite recursion" message
        // Direct left recursion: ref calls itself without consuming input
        Taker<Character> ref = Taker.ref();
        ref.set(ref::apply);  // infinite recursion
        printResult(ref.parse(Inputs.of("xxx")));
    }

    /* ── calculator grammar errors ─────────────────────────────────── */

    @Test
    void calculatorMissingOperand() {
        Taker<Double> expr = buildCalculator();
        printResult(expr.parse(Inputs.of("3 +")));
    }

    @Test
    void calculatorUnmatchedParen() {
        Taker<Double> expr = buildCalculator();
        printResult(expr.parse(Inputs.of("(3 + 5")));
    }

    @Test
    void calculatorBadToken() {
        Taker<Double> expr = buildCalculator();
        printResult(expr.parse(Inputs.of("3 @ 5")));
    }

    /* ── attempt (exception handling) ──────────────────────────────── */

    @Test
    void attemptCatchesExpectedException() {
        // parseInt throws NumberFormatException on non-digit input
        Taker<Integer> safeInt = Combinators.attempt(
            Chars.takeWhile(c -> Character.isDigit(c)).collectString()
                .map(s -> {
                    if (s.length() > 10) throw new ArithmeticException("too long");
                    return Integer.parseInt(s);
                }),
            ArithmeticException.class,
            "expected integer <= 10 digits"
        );

        // Normal input works
        System.out.println("Normal: " + safeInt.parse(Inputs.of("42")));

        // Too-long input catches the exception
        printResult(safeInt.parse(Inputs.of("123456789012345")));
    }

    @Test
    void attemptWithCustomHandler() {
        Taker<Double> safeDouble = Combinators.attempt(
            Chars.oneOf("0123456789.").collectString().map(Double::parseDouble),
            NumberFormatException.class,
            e -> new NoMatch<>(Inputs.of(""), "expected valid double, got: '" + e.getMessage() + "'")
        );

        printResult(safeDouble.parse(Inputs.of("abc")));
    }

    @Test
    void attemptDoesNotSwallowUnexpectedExceptions() {
        // This parser will throw NPE
        Taker<String> broken = Chars.oneOf("a").map(s -> {
            throw new NullPointerException("real bug");
        });

        // Only catching NumberFormatException — NPE should propagate
        Taker<String> wrapped = Combinators.attempt(
            broken, NumberFormatException.class, "expected number"
        );

        try {
            wrapped.parse(Inputs.of("a"));
            System.out.println("ERROR: NPE was swallowed!");
        } catch (NullPointerException e) {
            System.out.println("Correct: NPE propagated — " + e.getMessage());
        }
    }

    /* ── deeply nested failures ────────────────────────────────────── */

    @Test
    void deeplyNestedFailure() {
        // Grammar: outer1 -> outer2 -> outer3 -> inner (map -> map -> between -> or -> string)
        // Failure deep inside should bubble up with context
        Taker<String> inner = Lexical.string("xyz")
                .map(String::toUpperCase)
                .map(s -> "[" + s + "]")
                .map(s -> "prefix:" + s);

        Taker<String> outer = inner
                .between('{', '}')
                .map(s -> "outer:" + s);

        Taker<String> full = Chars.oneOf("start:")
                .collectString()
                .then(Chars.oneOf(" "))
                .then(outer)
                .map((a, b, c) -> a + b + c);

        // "xyz" fails inside, after consuming "start: {prefix"
        printResult(full.parse(Inputs.of("start: {abc}")));
    }

    @Test
    void deeplyNestedRecursiveFailure() {
        // Recursive grammar with failure deep in the tree
        // list = item { ',' item } ; item = '(' list ')' | number
        Taker<List<String>> list = Taker.ref();
        Taker<String> item = Numeric.unsignedInteger
                .map(Object::toString)
                .or(list.between('(', ')').map(l -> l.get(0)));
        list.set(item.zeroOrMoreSeparatedBy(Chars.oneOf(",")));

        // Failure: unmatched '(' deep in nested structure
        String input = "1,(2,(3";
        printResult(list.parse(Inputs.of(input)));
    }

    /* ── helpers ───────────────────────────────────────────────────── */

    private static <A> void printResult(Result<A> result) {
        System.out.println("═══ " + result.getClass().getSimpleName() + " ═══");
        System.out.println(result);

        if (result instanceof Failure<?> f) {
            System.out.println("Error message: " + f.error());
        }
        System.out.println();
    }

    // Minimal calculator: expr = term { (+|-) term }; term = atom { (*|/) atom }; atom = number | (expr)
    private static Taker<Double> buildCalculator() {
        Taker<Double> expr = Taker.ref();
        Taker<Double> number = Chars.oneOf("0123456789.")
                .collectString().map(Double::parseDouble);

        Taker<BinaryOperator<Double>> addSub = Chars.oneOf("+-").map(c ->
                c == '+' ? (BinaryOperator<Double>) (a, b) -> a + b
                         : (BinaryOperator<Double>) (a, b) -> a - b);

        Taker<BinaryOperator<Double>> mulDiv = Chars.oneOf("*/").map(c ->
                c == '*' ? (BinaryOperator<Double>) (a, b) -> a * b
                         : (BinaryOperator<Double>) (a, b) -> a / b);

        Taker<Double> atom = number.or(expr.between('(', ')'));
        Taker<Double> term = atom.chainLeftOneOrMore(mulDiv);
        expr.set(term.chainLeftOneOrMore(addSub));
        return expr;
    }
}
