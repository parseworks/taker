package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Lists;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.impl.Pair;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class Chains {


    /**
     * Handles operator expressions with specified associativity.
     * <pre>{@code
     * Chains.chain(number, add, Associativity.LEFT); // (1+2)+3
     * }</pre>
     *
     * @param op            binary operator parser
     * @param associativity LEFT or RIGHT
     * @return an operator chain parser
     */
    public static <A> Taker<A> chain(Taker<A> parser, Taker<BinaryOperator<A>> op, Associativity associativity) {
        if (associativity == Associativity.LEFT) {
            final Taker<UnaryOperator<A>> plo =
                op.then(parser)
                    .map((f, y) -> x -> f.apply(x, y));
            return parser.then(plo.zeroOrMore())
                .map((a, lf) -> Lists.foldLeft(lf, a, (acc, f) -> f.apply(acc)));
        } else {
            return parser.then(op.then(parser).map(Pair::new).zeroOrMore())
                .map((a, pairs) -> pairs.stream().reduce(a, (acc, tuple) -> tuple.left().apply(tuple.right(), acc), (a1, a2) -> a1));
        }
    }

    /**
     * Operator associativity rules.
     */
    public enum Associativity {
        /** Left-to-right (e.g., 5-3-2 = (5-3)-2) */
        LEFT,

        /** Right-to-left (e.g., 2^3^2 = 2^(3^2)) */
        RIGHT
    }
}
