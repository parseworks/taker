package io.github.parseworks.taker;

/** Functional interfaces for functions with arity 3 to 8. */
public class Functions {
    /** Function of arity 3. */
    @FunctionalInterface
    public interface Func3<A, B, C, R> {
        /**
         * Apply this function.
         *
         * @param a the function's left argument
         * @param b the function's right argument
         * @param c the function's third argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c);
    }

    /** Function of arity 4. */
    @FunctionalInterface
    public interface Func4<A, B, C, D, R> {

        /**
         * Apply this function.
         *
         * @param a the function's left argument
         * @param b the function's right argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d);

    }

    /** Function of arity 5. */
    @FunctionalInterface
    public interface Func5<A, B, C, D, E, R> {

        /**
         * Apply this function.
         *
         * @param a the function's left argument
         * @param b the function's right argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e);

    }

    /** Function of arity 6. */
    @FunctionalInterface
    public interface Func6<A, B, C, D, E, G, R> {
        /**
         * Apply this function.
         *
         * @param a the function's left argument
         * @param b the function's right argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @param g the function's sixth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e, G g);
    }

    /** Function of arity 7. */
    @FunctionalInterface
    public interface Func7<A, B, C, D, E, G, H, R> {

        /**
         * Apply this function.
         *
         * @param a the function's left argument
         * @param b the function's right argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @param g the function's sixth argument
         * @param h the function's seventh argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e, G g, H h);

    }

    /** Function of arity 8. */
    @FunctionalInterface
    public interface Func8<A, B, C, D, E, G, H, I, R> {

        /**
         * Apply this function.
         *
         * @param a the function's left argument
         * @param b the function's right argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @param g the function's sixth argument
         * @param h the function's seventh argument
         * @param i the function's eighth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e, G g, H h, I i);

    }
}
