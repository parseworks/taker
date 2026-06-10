package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Numeric;
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

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ParserBenchmarks {

    @State(Scope.Thread)
    public static class ParserState {
        Taker<Long> number;
        String numberInput;

        Taker<List<Character>> repeatedLettersAndDigits;
        String repeatedInput;

        Taker<List<List<String>>> csvParser;
        String csvInput;

        Taker<String> expectedIdentifier;
        String invalidIdentifier;

        @Setup
        public void setUp() {
            number = Numeric.number;
            numberInput = "123456789012345";

            Taker<List<Character>> letterParser = chr(Character::isLetter).zeroOrMore();
            Taker<List<Character>> digitParser = chr(Character::isDigit).zeroOrMore();
            repeatedLettersAndDigits = letterParser.then(digitParser).map(Lists::appendAll);
            repeatedInput = repeatedInput(10_000);

            csvParser = csvParser();
            csvInput = csvInput(1_000);

            expectedIdentifier = oneOf(
                string("class").expecting("class keyword"),
                string("interface").expecting("interface keyword"),
                string("record").expecting("record keyword")
            );
            invalidIdentifier = "enum";
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

        private static String repeatedInput(int pairs) {
            StringBuilder input = new StringBuilder(pairs * 2);
            for (int i = 0; i < pairs; i++) {
                input.append('a').append(i % 10);
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
    }

    @Benchmark
    public void parseNumber(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.number.parse(state.numberInput));
    }

    @Benchmark
    public void parseRepeatedLettersAndDigits(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.repeatedLettersAndDigits.parse(state.repeatedInput));
    }

    @Benchmark
    public void parseCsv(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.csvParser.parse(state.csvInput));
    }

    @Benchmark
    public void collectExpectedFailures(ParserState state, Blackhole blackhole) {
        blackhole.consume(state.expectedIdentifier.parse(state.invalidIdentifier));
    }
}
