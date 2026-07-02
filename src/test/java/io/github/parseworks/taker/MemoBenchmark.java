/*
 * Demonstrates the impact of .memoize() on recursive grammars.
 * Run with: mvn test-compile && mvn exec:java -Dexec.mainClass=... -Dexec.classpathScope=test
 */
package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Chars;
import java.util.function.BinaryOperator;

/**
 * Benchmark: recursive expression grammar with and without packrat memoization.
 *
 * Grammar:
 *   expression = term (('+' | '-') term)*
 *   term       = atom (('*' | '/') atom)*
 *   atom       = NUMBER | '(' expression ')'
 *
 * Without memo: expression re-enters at same position through every '(' branch
 * With memo:    each (expression, position) pair computes once
 */
public class MemoBenchmark {

    static Taker<Double> number = Chars.oneOf("0123456789")
            .collectString().map(Double::parseDouble);

    static Taker<BinaryOperator<Double>> addOrSub = Chars.oneOf("+-").map(c ->
            c == '+' ? Double::sum
                     :  (a, b) -> a - b);

    static Taker<BinaryOperator<Double>> mulOrDiv = Chars.oneOf("*/").map(c ->
            c == '*' ?  (a, b) -> a * b
                     :  (a, b) -> a / b);

    static Taker<Double> buildGrammar() {
        Taker<Double> e = Taker.ref();
        Taker<Double> atom = number.or(e.between('(', ')'));
        Taker<Double> term = atom.chainLeftOneOrMore(mulOrDiv);
        e.set(term.chainLeftOneOrMore(addOrSub));
        return e;
    }

    // 1+(1+(1+(1+...)))
    static String rightDeep(int depth) {
        if (depth == 2) return "1+2";
        return "1+(1+(1+" + (depth - 2) + "))";
    }

    // Balanced: (1+2)+((3+4)+(5+6))
    static int nn = 0;
    static String balanced(int d) {
        if (d == 0) return String.valueOf(nn++);
        return "(" + balanced(d - 1) + "+" + balanced(d - 1) + ")";
    }

    static void runNoMemo(Taker<Double> g, String input, int iters) {
        String name = "parse()      [no memo]";
        for (int i = 0; i < 3; i++) g.parse(input);
        long t0 = System.nanoTime();
        Result<Double> last = null;
        for (int i = 0; i < iters; i++) last = g.parse(input);
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        boolean ok = last.matches();
        System.out.printf("%-25s len=%4d  result=%-8s  avg=%8.2f ms%n",
                name, input.length(), ok ? "=" + last.value() : "FAIL", ms / iters);
    }

    static void runMemoized(Taker<Double> g, String input, int iters) {
        Taker<Double> mg = g.memoize();
        for (int i = 0; i < 3; i++) mg.parse(input);
        long t0 = System.nanoTime();
        Result<Double> last = null;
        for (int i = 0; i < iters; i++) last = mg.parse(input);
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        boolean ok = last.matches();
        System.out.printf("%-25s len=%4d  result=%-8s  avg=%8.2f ms%n",
                ".memoize().parse()", input.length(),
                ok ? "=" + last.value() : "FAIL", ms / iters);
    }

    public static void main(String[] args) {
        System.out.println("=== ref() with/without memo: Recursive Expression Grammar ===\n");

        // 1: Right-deep nesting
        System.out.println("--- Right-deep: 1+(1+(1+...)) ---");
        for (int d = 4; d <= 14; d++) {
            StringBuilder input = new StringBuilder("1+");
            for (int i = 1; i < d; i++) input.insert(0, "1+(");
            for (int i = 1; i < d; i++) input.append(")");
            input.append(d);
            runNoMemo(buildGrammar(), input.toString(), 5);
            runMemoized(buildGrammar(), input.toString(), 5);
            System.out.println();
        }

        // 2: Balanced tree
        System.out.println("--- Balanced: (1+2)+((3+4)+(5+6)) ---");
        for (int d = 1; d <= 7; d++) {
            nn = 1;
            String input = balanced(d);
            runNoMemo(buildGrammar(), input, 5);
            runMemoized(buildGrammar(), input, 5);
            System.out.println();
        }

        // 3: Linear — no recursion
        System.out.println("--- Linear: 1+2+3+...+200 (no backtracking) ---");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 200; i++) { if (i > 1) sb.append('+'); sb.append(i); }
        String lin = sb.toString();
        runNoMemo(buildGrammar(), lin, 50);
        runMemoized(buildGrammar(), lin, 50);
    }
}
