package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Taker;

import java.util.function.BinaryOperator;

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
            return Combinators.chainLeft(parser, op);
        }
        return Combinators.chainRight(parser, op);
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
