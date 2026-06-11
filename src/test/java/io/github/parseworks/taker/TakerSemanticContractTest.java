package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Combinators;
import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.parseworks.taker.parsers.Combinators.eof;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.*;

public class TakerSemanticContractTest {

    @Test
    void pureAlwaysSucceedsWithoutConsumingInput() {
        Result<String> result = Taker.pure("value").parse("abc");

        assertTrue(result.matches());
        assertEquals("value", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    void mapTransformsSuccessAndPreservesInputPosition() {
        Result<Integer> result = chr('a').map(c -> (int) c).parse("abc");

        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertEquals(1, result.input().position());
    }

    @Test
    void mapDoesNotRunOnFailure() {
        AtomicBoolean mapperCalled = new AtomicBoolean(false);

        Result<Integer> result = chr('a').map(c -> {
            mapperCalled.set(true);
            return (int) c;
        }).parse("b");

        assertFalse(result.matches());
        assertFalse(mapperCalled.get());
    }

    @Test
    void locatedWrapsSuccessfulValueWithConsumedOffsets() {
        Result<Located<String>> result = Lexical.regex("[A-Za-z]+").located().parse(Input.of("name = value"));

        assertTrue(result.matches());
        assertEquals("name", result.value().value());
        assertEquals(0, result.value().start());
        assertEquals(4, result.value().end());
        assertEquals(4, result.value().length());
        assertEquals(4, result.input().position());
    }

    @Test
    void locatedUsesCurrentInputPositionAsStartOffset() {
        Input input = Input.of("let name").skip(4);

        Result<Located<String>> result = Lexical.word.located().parse(input);

        assertTrue(result.matches());
        assertEquals("name", result.value().value());
        assertEquals(4, result.value().start());
        assertEquals(8, result.value().end());
        assertEquals(8, result.input().position());
    }

    @Test
    void locatedCoversWholeComposedParser() {
        Taker<String> keyValue = Lexical.word
            .thenSkip(chr('='))
            .then(Lexical.word)
            .map((key, value) -> key + ":" + value);

        Result<Located<String>> result = keyValue.located().parse("name=value;");

        assertTrue(result.matches());
        assertEquals("name:value", result.value().value());
        assertEquals(0, result.value().start());
        assertEquals(10, result.value().end());
        assertEquals(10, result.input().position());
    }

    @Test
    void locatedCanBeAppliedToSubParsersForAstStyleMapping() {
        Taker<String> spaces = Taker.takeWhile(c -> c == ' ').orElse("");
        Taker<Located<String>> identifier = Lexical.word.located();
        Taker<List<Located<String>>> assignment = identifier
            .thenSkip(spaces)
            .thenSkip(chr('='))
            .thenSkip(spaces)
            .then(identifier)
            .map((left, right) -> List.of(left, right));

        Result<List<Located<String>>> result = assignment.parse("name = value");

        assertTrue(result.matches());
        assertEquals(new Located<>("name", 0, 4), result.value().get(0));
        assertEquals(new Located<>("value", 7, 12), result.value().get(1));
    }

    @Test
    void locatedPropagatesFailureWithoutChangingFailurePosition() {
        Result<Located<Character>> result = chr('a').located().parse("b");

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        assertEquals(0, result.input().position());
    }

    @Test
    void locatedSupportsZeroWidthSuccessfulParsers() {
        Result<Located<String>> result = Taker.pure("empty").located().parse("abc");

        assertTrue(result.matches());
        assertEquals("empty", result.value().value());
        assertEquals(0, result.value().start());
        assertEquals(0, result.value().end());
        assertEquals(0, result.value().length());
        assertEquals(0, result.input().position());
    }

    @Test
    void flatMapChoosesNextParserFromPreviousValue() {
        Taker<String> parser = chr('3').map(Character::getNumericValue)
            .flatMap(n -> chr('a').repeat(n).map(TakerSemanticContractTest::join));

        Result<String> result = parser.parse("3aaa");

        assertTrue(result.matches());
        assertEquals("aaa", result.value());
        assertEquals(4, result.input().position());
    }

    @Test
    void flatMapPropagatesFirstParserFailureWithoutRunningFunction() {
        AtomicBoolean functionCalled = new AtomicBoolean(false);

        Result<Character> result = chr('a').flatMap(c -> {
            functionCalled.set(true);
            return chr('b');
        }).parse("x");

        assertFalse(result.matches());
        assertFalse(functionCalled.get());
    }

    @Test
    void thenSkipReturnsFirstValueAndConsumesBothParsers() {
        Result<Character> result = chr('a').thenSkip(chr('b')).parse("abc");

        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertEquals(2, result.input().position());
    }

    @Test
    void skipThenReturnsSecondValueAndConsumesBothParsers() {
        Result<Character> result = chr('a').skipThen(chr('b')).parse("abc");

        assertTrue(result.matches());
        assertEquals('b', result.value());
        assertEquals(2, result.input().position());
    }

    @Test
    void sequenceFailureDoesNotBecomePartialUnlessCommitted() {
        Result<String> result = string("ab").thenSkip(chr('c')).parse("abx");

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        assertEquals(2, result.input().position());
    }

    @Test
    void choiceBacktracksAfterNoMatchEvenWhenFailureReportsAdvancedPosition() {
        Taker<String> parser = string("ab").or(string("ac"));

        Result<String> result = parser.parse("ac");

        assertTrue(result.matches());
        assertEquals("ac", result.value());
        assertEquals(2, result.input().position());
    }

    @Test
    void choiceCanSelectEofBranchAtEndOfInput() {
        Taker<String> parser = oneOf(
            eof().as("end"),
            chr('a').as("letter")
        );

        Result<String> result = parser.parse("");

        assertTrue(result.matches());
        assertEquals("end", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void choiceCombinesNoMatchFailuresWhenAllAlternativesFail() {
        Result<Character> result = oneOf(chr('a'), chr('b'), chr('c')).parse("x");

        assertFalse(result.matches());
        Failure<?> failure = (Failure<?>) result;
        assertNotNull(failure.combinedFailures());
        assertEquals(3, failure.combinedFailures().size());
    }

    @Test
    void choiceReportsOnlyFailuresAtFarthestPosition() {
        Taker<String> shallow = new Taker<>(input -> new io.github.parseworks.taker.impl.result.NoMatch<>(input, "shallow"));
        Taker<String> deepA = string("abz").expecting("abz");
        Taker<String> deepB = string("aby").expecting("aby");

        Result<String> result = oneOf(shallow, deepA, deepB).parse("abx");

        assertFalse(result.matches());
        assertEquals(2, result.input().position());
        Failure<?> failure = (Failure<?>) result;
        assertNotNull(failure.combinedFailures());
        assertEquals(2, failure.combinedFailures().size());
        assertTrue(result.error().contains("expected abz"));
        assertTrue(result.error().contains("expected aby"));
        assertFalse(result.error().contains("expected shallow"));
    }

    @Test
    void committedFailureStopsLaterAlternatives() {
        Taker<String> parser = Taker.commit(string("ab")).or(string("ac"));

        Result<String> result = parser.parse("ac");

        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type());
    }

    @Test
    void commitOnlyConvertsFailuresThatAdvanceInput() {
        Result<Character> result = Taker.commit(chr('a')).parse("b");

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        assertEquals(0, result.input().position());
    }

    @Test
    void parseAllRequiresEofAfterSuccess() {
        Result<Character> result = chr('a').parseAll("ab");

        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type());
        assertEquals(1, result.input().position());
    }

    @Test
    void parseAllowsTrailingInputAfterSuccess() {
        Result<Character> result = chr('a').parse("ab");

        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertEquals(1, result.input().position());
    }

    @Test
    void optionalSucceedsWithoutConsumingInputOnFailure() {
        Result<Optional<Character>> result = chr('a').optional().parse("b");

        assertTrue(result.matches());
        assertEquals(Optional.empty(), result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    void orElseSucceedsWithoutConsumingInputOnFailure() {
        Result<Character> result = chr('a').orElse('x').parse("b");

        assertTrue(result.matches());
        assertEquals('x', result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    void repeatRejectsZeroWidthSuccessToPreventInfiniteLoops() {
        Result<List<Character>> result = Taker.pure('x').zeroOrMore().parse("abc");

        assertFalse(result.matches());
        assertEquals(0, result.input().position());
    }

    @Test
    void repeatReturnsUnmodifiableSuccessfulList() {
        Result<List<Character>> result = chr('a').oneOrMore().parse("aaa");

        assertTrue(result.matches());
        assertEquals(List.of('a', 'a', 'a'), result.value());
        assertThrows(UnsupportedOperationException.class, () -> result.value().add('a'));
    }

    @Test
    void repeatUntilConsumesTerminatorWhenFound() {
        Result<List<Character>> result = chr('a').zeroOrMoreUntil(chr(';')).parse("aa;z");

        assertTrue(result.matches());
        assertEquals(List.of('a', 'a'), result.value());
        assertEquals(3, result.input().position());
    }

    @Test
    void separatedByRequiresElementAfterConsumedSeparator() {
        Result<List<Character>> result = chr('a').oneOrMoreSeparatedBy(chr(',')).parse("a,");

        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type());
    }

    @Test
    void peekRequiresLookaheadWithoutConsumingIt() {
        Result<Character> result = chr('a').peek(chr('b')).parse("ab");

        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertEquals(1, result.input().position());
    }

    @Test
    void onlyIfUsesValidationAsLookaheadFromOriginalPosition() {
        Result<Character> result = chr('a').onlyIf(chr('a')).parse("ab");

        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertEquals(1, result.input().position());
    }

    @Test
    void recoverRunsRecoveryAtOriginalInputPosition() {
        Result<Character> result = string("ab").recover(chr('a')).parse("ax");

        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertEquals(1, result.input().position());
    }

    @Test
    void recoverWithReceivesFailureAndCanReturnReplacementResult() {
        Result<String> result = chr('a').recoverWith(failure ->
            new Taker<>(in -> Taker.pure("fallback").apply(in)).apply(failure.input())
        ).parse("b");

        assertTrue(result.matches());
        assertEquals("fallback", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    void recoverWithReceivesCommittedFailure() {
        Result<String> result = Taker.commit(string("ab")).recoverWith(failure -> {
            assertEquals(ResultType.PARTIAL, failure.type());
            return Taker.pure("recovered").apply(failure.input());
        }).parse("ax");

        assertTrue(result.matches());
        assertEquals("recovered", result.value());
        assertEquals(1, result.input().position());
    }

    @Test
    void expectingRelabelsFailureAndPreservesCause() {
        Result<Character> result = chr('a').expecting("letter a").parse("b");

        assertFalse(result.matches());
        Failure<?> failure = (Failure<?>) result;
        assertEquals("letter a", failure.expected());
        assertNotNull(failure.cause());
    }

    @Test
    void takeWhileRequiresAtLeastOneCharacter() {
        Result<String> result = Taker.takeWhile(CharPredicate.digit).parse("abc");

        assertFalse(result.matches());
        assertEquals(0, result.input().position());
    }

    @Test
    void takeUntilSucceedsToEofWhenTerminatorIsMissing() {
        Result<String> result = Taker.takeUntil(CharPredicate.is(';')).parse("abc");

        assertTrue(result.matches());
        assertEquals("abc", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void iterateParseSkipsFailuresAndYieldsMatchesInOrder() {
        var iterator = Lexical.regex("\\d+").iterateParse(Input.of("a12b3"));

        assertTrue(iterator.hasNext());
        assertEquals("12", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("3", iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void refCanBeInitializedExactlyOnce() {
        Taker<Character> ref = Taker.ref();
        ref.set(chr('a'));

        assertEquals('a', ref.parse("a").value());
        assertThrows(IllegalStateException.class, () -> ref.set(chr('b')));
    }

    @Test
    void uninitializedRefThrowsWhenApplied() {
        Taker<Character> ref = Taker.ref();

        assertThrows(IllegalStateException.class, () -> ref.parse("a"));
    }

    @Test
    void directRecursiveRefFailsInsteadOfRecursingForever() {
        Taker<String> ref = Taker.ref();
        ref.set(Taker.pure("").skipThen(ref));

        Result<String> result = ref.parse("a");

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
    }

    @Test
    void emptyChoiceIsRejectedAtConstructionTime() {
        assertThrows(IllegalArgumentException.class, () -> Combinators.oneOf(List.<Taker<Character>>of()));
    }

    private static String join(List<?> values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            builder.append(value);
        }
        return builder.toString();
    }
}
