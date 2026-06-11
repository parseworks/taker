package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Numeric;
import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.NoMatch;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.github.parseworks.taker.Taker.takeWhile;
import static io.github.parseworks.taker.parsers.Combinators.not;
import static io.github.parseworks.taker.parsers.Combinators.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.escapedString;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static io.github.parseworks.taker.parsers.Lexical.trimSpaces;
import static io.github.parseworks.taker.parsers.Lexical.trimWhitespace;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ParserBenchmarks {

    @State(Scope.Thread)
    public static class ParserState {
        Taker<Long> number;
        Taker<Long> locatedNumber;
        Taker<Character> trimmedSpaceChar;
        Taker<Character> trimmedWhitespaceChar;
        String numberInput;
        String trimInput;

        Taker<List<Character>> repeatedLettersAndDigits;
        Taker<Integer> foldedLetterDigitCount;
        String repeatedInput;

        Taker<String> collectedLetters;
        Taker<String> listedLettersJoined;
        String lettersInput;

        Taker<List<List<String>>> csvParser;
        Taker<Integer> csvFieldCounter;
        String csvInput;

        Taker<String> expectedIdentifier;
        String invalidIdentifier;

        @Setup
        public void setUp() {
            number = Numeric.number;
            locatedNumber = Numeric.number.located().map(Located::value);
            trimmedSpaceChar = trimSpaces(chr('a'));
            trimmedWhitespaceChar = trimWhitespace(chr('a'));
            numberInput = "123456789012345";
            trimInput = "   a   ";

            repeatedLettersAndDigits = chr(Character::isLetter)
                .then(chr(Character::isDigit))
                .map((letter, digit) -> digit)
                .zeroOrMore();
            foldedLetterDigitCount = chr(Character::isLetter)
                .then(chr(Character::isDigit))
                .map((letter, digit) -> digit)
                .foldZeroOrMore(0, (count, digit) -> count + 1);
            repeatedInput = repeatedInput(10_000);

            collectedLetters = chr(Character::isLetter).collectString();
            listedLettersJoined = chr(Character::isLetter).oneOrMore().map(Lists::join);
            lettersInput = lettersInput(10_000);

            csvParser = csvParser();
            csvFieldCounter = csvFieldCounter();
            csvInput = csvInput(1_000);

            expectedIdentifier = oneOf(
                string("class").expecting("class keyword"),
                string("interface").expecting("interface keyword"),
                string("record").expecting("record keyword")
            );
            invalidIdentifier = "enum";

            requireMatch("number", number.parseAll(numberInput));
            requireMatch("locatedNumber", locatedNumber.parseAll(numberInput));
            requireMatch("trimmedSpaceChar", trimmedSpaceChar.parseAll(trimInput));
            requireMatch("trimmedWhitespaceChar", trimmedWhitespaceChar.parseAll(trimInput));
            requireMatch("repeatedLettersAndDigits", repeatedLettersAndDigits.parseAll(repeatedInput));
            requireMatch("foldedLetterDigitCount", foldedLetterDigitCount.parseAll(repeatedInput));
            requireMatch("collectedLetters", collectedLetters.parseAll(lettersInput));
            requireMatch("listedLettersJoined", listedLettersJoined.parseAll(lettersInput));
            requireMatch("csvParser", csvParser.parseAll(csvInput));
            requireMatch("csvFieldCounter", csvFieldCounter.parseAll(csvInput));
            if (countCsvFields(csvInput) != 10_000) {
                throw new IllegalStateException("csv scanner did not count all fields");
            }
        }

        private static void requireMatch(String name, Result<?> result) {
            if (!result.matches() || !result.input().isEof()) {
                throw new IllegalStateException(name + " benchmark parser did not consume the intended input");
            }
        }

        private static Taker<List<List<String>>> csvParser() {
            Taker<Character> comma = trim(chr(','));
            Taker<Character> eol = string("\r\n").as('\n').or(chr('\n'));

            Taker<String> quotedField = escapedString('"', '\\', Map.of('"', '"'));
            Taker<String> unquotedFieldCore = takeWhile(c -> c != ',' && c != '\n' && c != '\r');
            Taker<String> unquotedField = unquotedFieldCore.onlyIf(not(chr('"')));

            Taker<String> boolToken = oneOf(string("true"), string("false"));
            Taker<String> nullToken = oneOf(string("NULL"), string("null"));
            Taker<String> numberToken = Numeric.doubleValue.map(String::valueOf);

            Taker<String> field = oneOf(
                boolToken.expecting("boolean"),
                nullToken.expecting("null"),
                numberToken.expecting("number"),
                quotedField.expecting("quoted"),
                unquotedField.expecting("unquoted")
            );

            Taker<List<String>> row = field.oneOrMoreSeparatedBy(comma);
            return row.oneOrMoreSeparatedBy(eol);
        }

        private static Taker<Integer> csvFieldCounter() {
            Taker<Character> comma = trim(chr(','));
            Taker<Character> eol = string("\r\n").as('\n').or(chr('\n'));
            Taker<Integer> field = csvField().as(1);

            return new Taker<>(in -> {
                Result<Integer> first = field.apply(in);
                if (!first.matches()) {
                    return first.cast();
                }

                int count = 1;
                Input current = first.input();
                while (true) {
                    Result<Character> commaResult = comma.apply(current);
                    if (commaResult.matches()) {
                        Result<Integer> next = field.apply(commaResult.input());
                        if (!next.matches()) {
                            return next.cast();
                        }
                        count++;
                        current = next.input();
                        continue;
                    }

                    Result<Character> eolResult = eol.apply(current);
                    if (eolResult.matches()) {
                        Result<Integer> next = field.apply(eolResult.input());
                        if (!next.matches()) {
                            return next.cast();
                        }
                        count++;
                        current = next.input();
                        continue;
                    }

                    return new Match<>(count, current);
                }
            });
        }

        private static Taker<Void> csvField() {
            return new Taker<>(in -> {
                if (in.isEof()) {
                    return new NoMatch<>(in, "csv field");
                }

                CharSequence data = in.data();
                int position = in.position();
                char first = data.charAt(position);
                if (first == '"') {
                    int current = position + 1;
                    while (current < data.length()) {
                        char c = data.charAt(current);
                        if (c == '\\') {
                            current += 2;
                        } else if (c == '"') {
                            return new Match<>(null, in.skip(current - position + 1));
                        } else {
                            current++;
                        }
                    }
                    return new NoMatch<>(in.skip(current - position), "closing quote");
                }

                int current = position;
                while (current < data.length()) {
                    char c = data.charAt(current);
                    if (c == ',' || c == '\n' || c == '\r') {
                        break;
                    }
                    current++;
                }

                if (current == position) {
                    return new NoMatch<>(in, "csv field");
                }
                return new Match<>(null, in.skip(current - position));
            });
        }

        private static String repeatedInput(int pairs) {
            StringBuilder input = new StringBuilder(pairs * 2);
            for (int i = 0; i < pairs; i++) {
                input.append('a').append(i % 10);
            }
            return input.toString();
        }

        private static String lettersInput(int length) {
            StringBuilder input = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                input.append((char) ('a' + (i % 26)));
            }
            return input.toString();
        }

        private static String csvInput(int rows) {
            StringBuilder input = new StringBuilder(rows * 80);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < 10; j++) {
                    switch (j % 5) {
                        case 0 -> input.append("\"field\\\"").append(j).append("\"");
                        case 1 -> input.append("true");
                        case 2 -> input.append("-123.45");
                        case 3 -> input.append("NULL");
                        default -> input.append("plain_").append(j);
                    }
                    if (j < 9) input.append(", ");
                }
                if (i < rows - 1) input.append('\n');
            }
            return input.toString();
        }

        private static int countCsvFields(String input) {
            int count = 0;
            int current = 0;
            while (current < input.length()) {
                while (current < input.length() && input.charAt(current) == ' ') {
                    current++;
                }
                if (current >= input.length()) {
                    return count;
                }

                if (input.charAt(current) == '"') {
                    current++;
                    while (current < input.length()) {
                        char c = input.charAt(current);
                        if (c == '\\') {
                            current += 2;
                        } else if (c == '"') {
                            current++;
                            break;
                        } else {
                            current++;
                        }
                    }
                } else {
                    while (current < input.length()) {
                        char c = input.charAt(current);
                        if (c == ',' || c == '\n' || c == '\r') {
                            break;
                        }
                        current++;
                    }
                }
                count++;

                while (current < input.length() && input.charAt(current) == ' ') {
                    current++;
                }
                if (current < input.length() && input.charAt(current) == ',') {
                    current++;
                } else if (current < input.length() && input.charAt(current) == '\r') {
                    current++;
                    if (current < input.length() && input.charAt(current) == '\n') {
                        current++;
                    }
                } else if (current < input.length() && input.charAt(current) == '\n') {
                    current++;
                }
            }
            return count;
        }
    }

    @Benchmark
    public void parseNumber(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.number.parse(state.numberInput));
    }

    @Benchmark
    public void parseNumberAll(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.number.parseAll(state.numberInput));
    }

    @Benchmark
    public void parseLocatedNumber(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.locatedNumber.parse(state.numberInput));
    }

    @Benchmark
    public void trimSpacesChar(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.trimmedSpaceChar.parse(state.trimInput));
    }

    @Benchmark
    public void trimWhitespaceChar(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.trimmedWhitespaceChar.parse(state.trimInput));
    }

    @Benchmark
    public void parseRepeatedLettersAndDigits(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.repeatedLettersAndDigits.parseAll(state.repeatedInput));
    }

    @Benchmark
    public void foldRepeatedLettersAndDigits(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.foldedLetterDigitCount.parseAll(state.repeatedInput));
    }

    @Benchmark
    public void collectStringLetters(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.collectedLetters.parseAll(state.lettersInput));
    }

    @Benchmark
    public void listJoinLetters(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.listedLettersJoined.parseAll(state.lettersInput));
    }

    @Benchmark
    public void parseCsv(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.csvParser.parseAll(state.csvInput));
    }

    @Benchmark
    public void parseCsvCountFields(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.csvFieldCounter.parseAll(state.csvInput));
    }

    @Benchmark
    public void scanCsvCountFields(ParserState state, Blackhole blackhole) {
        blackhole.consume(ParserState.countCsvFields(state.csvInput));
    }

    @Benchmark
    public void collectExpectedFailures(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.expectedIdentifier.parse(state.invalidIdentifier));
    }
}
