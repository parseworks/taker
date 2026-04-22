package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.impl.result.PartialMatch;

import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.parseworks.taker.parsers.Combinators.attempt;

/**
 * Fluent builder for combining multiple parsers sequentially.
 * <p>
 * Created by {@link Parser#then(Parser)}. Chain with {@code .then()}, {@code .thenSkip()},
 * or {@code .skipThen()}, and conclude with {@code .map()}.
 * <pre>{@code
 * Parser<Integer> sum =
 *     Numeric.number.thenSkip(Lexical.chr('+'))
 *                   .then(Numeric.number)
 *                   .map((a, b) -> a + b);
 * }</pre>

 * @param <A> first parser result type
 * @param <B> second parser result type
 */
public class ApplyBuilder<A, B> {

    protected final Parser<A> pa;
    protected final Parser<B> pb;

    public ApplyBuilder(Parser<A> pa, Parser<B> pb) {
        this.pa = pa;
        this.pb = pb;
    }

    /**
     * Creates a new {@code ApplyBuilder} instance with the given parsers.
     *
     * @param pa  the first parser
     * @param pb  the second parser
     * @param <A> the type of the result of the first parser
     * @param <B> the type of the result of the second parser
     * @return a new {@code ApplyBuilder} instance with the given parsers
     */
    public static <A, B> ApplyBuilder<A, B> of(Parser<A> pa, Parser<B> pb) {
        return new ApplyBuilder<>(pa, pb);
    }

    /**
     * Applies a function provided by one parser to the result of another parser.
     *
     * @param functionProvider the parser that provides the function
     * @param valueParser      the parser that provides the value
     * @param <A>              the type of the parsed value
     * @param <B>              the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <A, B> Parser<B> apply(Parser<Function<A, B>> functionProvider, Parser<A> valueParser) {
        return new Parser<>(in -> {
            Result<Function<A, B>> functionResult = functionProvider.apply(in);
            if (!functionResult.matches()) {
                return functionResult.cast();
            }
            Function<A, B> func = functionResult.value();
            Input in2 = functionResult.input();
            Result<A> valueResult = valueParser.apply(in2);
            if (!valueResult.matches()) {
                if (valueResult.input().position() > in.position()) {
                    return new PartialMatch<>(valueResult.input(), (Failure<A>) valueResult).cast();
                }
                return valueResult.cast();
            }
            return valueResult.map(func);
        });
    }

    /**
     * Applies a function to a parser's result (applicative functor pattern).
     * <pre>{@code
     * Function<Integer, Integer> doubleIt = n -> n * 2;
     * Parser<Integer> doubled = ApplyBuilder.apply(doubleIt, Numeric.integer);
     * doubled.parse("42").value();  // 84
     * }</pre>
     *
     * @param f   function to apply
     * @param pa  parser providing the value
     * @return parser applying the function to the parsed value
     * @see Parser#map(Function) for simpler transformation
     */
    public static <A, B> Parser<B> apply(Function<A, B> f, Parser<A> pa) {
        return ApplyBuilder.apply(Parser.pure(f), pa);
    }

    /**
     * Applies a parser-produced function to a constant value.
     * <pre>{@code
     * Parser<Function<Integer, Integer>> opParser =
     *     Lexical.chr('+').as(n -> n + 1);
     * Parser<Integer> result = ApplyBuilder.apply(opParser, 10);
     * result.parse("+").value();  // 11
     * }</pre>
     *
     * @param pf  parser providing the function
     * @param a   constant value
     * @return parser applying the parsed function to the constant
     */
    public static <A, B> Parser<B> apply(Parser<Function<A, B>> pf, A a) {
        return ApplyBuilder.apply(pf, Parser.pure(a));
    }

    /**
     * Maps the results of the parsers to a new result using the provided function.
     *
     * @param f   the function to map the results
     * @param <R> the result type
     * @return a new parser with the mapped result
     */
    public <R> Parser<R> map(Function<A, Function<B, R>> f) {
        return apply(pa.map(f), pb);
    }

    /**
     * Maps the results of the parsers to a new result using the provided bi-function.
     *
     * @param f   the bi-function to map the results
     * @param <R> the result type
     * @return a new parser with the mapped result
     */
    public <R> Parser<R> map(BiFunction<A, B, R> f) {
        Parser<Function<B, R>> pf = pa.map(a -> b -> f.apply(a, b));
        return apply(pf, pb);
    }

    /**
     * Adds a parser to be skipped after the current parser.
     *
     * @param pc  the parser to be skipped
     * @param <C> the type of the skipped parser's result
     * @return a new {@code ApplyBuilder} instance with the skipped parser
     */
    public <C> ApplyBuilder<A, B> thenSkip(Parser<C> pc) {
        return new ApplyBuilder<>(pa, pb.thenSkip(pc));
    }

    /**
     * Adds a parser to be applied after the current parser, DISCARDING the results
     * of the current builder after validating they are correct and returning only
     * the result of the next parser.
     *
     * @return a {@link Parser} (not chainable in a builder pattern)
     */
    public <C> Parser<C> skipThen(Parser<C> pc) {
        return attempt(allSkipped().skipThen(pc));
    }

    private Parser<Void> allSkipped() {
        return pa.thenSkip(pb).map(any -> null);
    }

    /**
     * Adds a parser to the chain of parsers, allowing for further chaining.
     *
     * @param pc  the parser to be applied
     * @param <C> the type of the next parser's result
     * @return a new {@code ApplyBuilder3} instance with the next parser
     */
    public <C> ApplyBuilder3<C> then(Parser<C> pc) {
        return new ApplyBuilder3<>(pc);
    }

    /**
     * {@code ApplyBuilder3} is a builder class for combining parsers with three levels of parsing.
     * <p>
     * This class allows for the sequential combination of parsers, where each parser can be followed
     * by another parser or skipped. The results of the parsers can be mapped to a new result using
     * a function that takes three arguments.
     *
     * @param <C> the type of the third parser's result
     */
    public class ApplyBuilder3<C> {
        private final Parser<C> pc;

        private ApplyBuilder3(Parser<C> pc) {
            this.pc = pc;
        }

        /**
         * Maps the results of the parsers to a new result using the provided function.
         *
         * @param f   the function to map the results
         * @param <R> the result type
         * @return a new parser with the mapped result
         */
        public <R> Parser<R> map(Function<A, Function<B, Function<C, R>>> f) {
            return apply(ApplyBuilder.this.map(f), pc);
        }

        /**
         * Maps the results of the parsers to a new result using the provided function with three arguments.
         *
         * @param f   the function to map the results
         * @param <R> the result type
         * @return a new parser with the mapped result
         */
        public <R> Parser<R> map(Functions.Func3<A, B, C, R> f) {
            return map(a -> b -> c -> f.apply(a, b, c));
        }

        /**
         * Adds a parser to be skipped after the current parser.
         *
         * @param pd  the parser to be skipped
         * @param <D> the type of the skipped parser's result
         * @return a new {@code ApplyBuilder3} instance with the skipped parser
         */
        public <D> ApplyBuilder3<C> thenSkip(Parser<D> pd) {
            return new ApplyBuilder3<>(pc.thenSkip(pd));
        }

        /**
         * Adds a parser to be applied after the current parser, DISCARDING the results
         * of the current builder after validating they are correct and returning only
         * the result of the next parser.
         *
         * @return a {@link Parser} (not chainable in a builder pattern)
         */
        public <D> Parser<D> skipThen(Parser<D> pd) {
            return attempt(ApplyBuilder.this.allSkipped().thenSkip(pc).skipThen(pd));
        }

        /**
         * Adds a parser to be applied after the current parser.
         *
         * @param pd  the parser to be applied
         * @param <D> the type of the next parser's result
         * @return a new {@code ApplyBuilder4} instance with the next parser
         */
        public <D> ApplyBuilder4<D> then(Parser<D> pd) {
            return new ApplyBuilder4<>(pd);
        }

        /**
         * {@code ApplyBuilder4} is a builder class for combining parsers with four levels of parsing.
         * <p>
         * This class allows for the sequential combination of parsers, where each parser can be followed
         * by another parser or skipped. The results of the parsers can be mapped to a new result using
         * a function that takes four arguments.
         *
         * @param <D> the type of the fourth parser's result
         */
        public class ApplyBuilder4<D> {
            private final Parser<D> pd;

            private ApplyBuilder4(Parser<D> pd) {
                this.pd = pd;
            }

            /**
             * Maps the results of the parsers to a new result using the provided function.
             *
             * @param f   the function to map the results
             * @param <R> the result type
             * @return a new parser with the mapped result
             */
            public <R> Parser<R> map(Function<A, Function<B, Function<C, Function<D, R>>>> f) {
                return apply(ApplyBuilder3.this.map(f), pd);
            }

            /**
             * Maps the results of the parsers to a new result using the provided function with four arguments.
             *
             * @param f   the function to map the results
             * @param <R> the result type
             * @return a new parser with the mapped result
             */
            public <R> Parser<R> map(Functions.Func4<A, B, C, D, R> f) {
                return map(a -> b -> c -> d -> f.apply(a, b, c, d));
            }

            /**
             * Adds a parser to be skipped after the current parser.
             *
             * @param pe  the parser to be skipped
             * @param <E> the type of the skipped parser's result
             * @return a new {@code ApplyBuilder4} instance with the skipped parser
             */
            public <E> ApplyBuilder4<D> thenSkip(Parser<E> pe) {
                return new ApplyBuilder4<>(pd.thenSkip(pe));
            }

            /**
             * Adds a parser to be applied after the current parser, DISCARDING the results
             * of the current builder after validating they are correct and returning only
             * the result of the next parser.
             *
             * @return a {@link Parser} (not chainable in a builder pattern)
             */
            public <E> Parser<E> skipThen(Parser<E> pe) {
                return attempt(ApplyBuilder.this.allSkipped().thenSkip(pc).thenSkip(pd).skipThen(pe));
            }

            /**
             * Adds a parser to be applied after the current parser.
             *
             * @param pe  the parser to be applied
             * @param <E> the type of the next parser's result
             * @return a new {@code ApplyBuilder5} instance with the next parser
             */
            public <E> ApplyBuilder5<E> then(Parser<E> pe) {
                return new ApplyBuilder5<>(pe);
            }

            /**
             * {@code ApplyBuilder5} is a builder class for combining parsers with five levels of parsing.
             * <p>
             * This class allows for the sequential combination of parsers, where each parser can be followed
             * by another parser or skipped. The results of the parsers can be mapped to a new result using
             * a function that takes five arguments.
             *
             * @param <E> the type of the fifth parser's result
             */
            public class ApplyBuilder5<E> {
                private final Parser<E> pe;

                private ApplyBuilder5(Parser<E> pe) {
                    this.pe = pe;
                }

                /**
                 * Maps the results of the parsers to a new result using the provided function.
                 *
                 * @param f   the function to map the results
                 * @param <R> the result type
                 * @return a new parser with the mapped result
                 */
                public <R> Parser<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) {
                    return apply(ApplyBuilder4.this.map(f), pe);
                }

                /**
                 * Maps the results of the parsers to a new result using the provided function with five arguments.
                 *
                 * @param f   the function to map the results
                 * @param <R> the result type
                 * @return a new parser with the mapped result
                 */
                public <R> Parser<R> map(Functions.Func5<A, B, C, D, E, R> f) {
                    return map(a -> b -> c -> d -> e -> f.apply(a, b, c, d, e));
                }

                /**
                 * Adds a parser to be skipped after the current parser.
                 *
                 * @param pg  the parser to be skipped
                 * @param <G> the type of the skipped parser's result
                 * @return a new {@code ApplyBuilder5} instance with the skipped parser
                 */
                public <G> ApplyBuilder5<E> thenSkip(Parser<G> pg) {
                    return new ApplyBuilder5<>(pe.thenSkip(pg));
                }

                /**
                 * Adds a parser to be applied after the current parser, DISCARDING the results
                 * of the current builder after validating they are correct and returning only
                 * the result of the next parser.
                 *
                 * @return a {@link Parser} (not chainable in a builder pattern)
                 */
                public <G> Parser<G> skipThen(Parser<G> pg) {
                    return attempt(ApplyBuilder.this.allSkipped().thenSkip(pc).thenSkip(pd).thenSkip(pe).skipThen(pg));
                }

                /**
                 * Adds a parser to be applied after the current parser.
                 *
                 * @param pg  the parser to be applied
                 * @param <G> the type of the next parser's result
                 * @return a new {@code ApplyBuilder6} instance with the next parser
                 */
                public <G> ApplyBuilder6<G> then(Parser<G> pg) {
                    return new ApplyBuilder6<>(pg);
                }

                /**
                 * {@code ApplyBuilder6} is a builder class for combining parsers with six levels of parsing.
                 * <p>
                 * This class allows for the sequential combination of parsers, where each parser can be followed
                 * by another parser or skipped. The results of the parsers can be mapped to a new result using
                 * a function that takes six arguments.
                 *
                 * @param <G> the type of the sixth parser's result
                 */
                public class ApplyBuilder6<G> {
                    private final Parser<G> pg;

                    private ApplyBuilder6(Parser<G> pg) {
                        this.pg = pg;
                    }

                    /**
                     * Maps the results of the parsers to a new result using the provided function.
                     *
                     * @param f   the function to map the results
                     * @param <R> the result type
                     * @return a new parser with the mapped result
                     */
                    public <R> Parser<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f) {
                        return apply(ApplyBuilder5.this.map(f), pg);
                    }

                    /**
                     * Maps the results of the parsers to a new result using the provided function with six arguments.
                     *
                     * @param f   the function to map the results
                     * @param <R> the result type
                     * @return a new parser with the mapped result
                     */
                    public <R> Parser<R> map(Functions.Func6<A, B, C, D, E, G, R> f) {
                        return map(a -> b -> c -> d -> e -> g -> f.apply(a, b, c, d, e, g));
                    }

                    /**
                     * Adds a parser to be skipped after the current parser.
                     *
                     * @param ph  the parser to be skipped
                     * @param <H> the type of the skipped parser's result
                     * @return a new {@code ApplyBuilder6} instance with the skipped parser
                     */
                    public <H> ApplyBuilder6<G> thenSkip(Parser<H> ph) {
                        return new ApplyBuilder6<>(pg.thenSkip(ph));
                    }

                    /**
                     * Adds a parser to be applied after the current parser, skipping the results of the current builder.
                     *
                     * @param ph  the parser to be applied
                     * @param <H> the type of the next parser's result
                     * @return a new parser that applies the current parsers and then the next parser, returning only the result of the next parser
                     */
                    public <H> Parser<H> skipThen(Parser<H> ph) {
                        return attempt(ApplyBuilder.this.allSkipped().thenSkip(pc).thenSkip(pd).thenSkip(pe).thenSkip(pg).skipThen(ph));
                    }

                    /**
                     * Adds a parser to be applied after the current parser.
                     *
                     * @param ph  the parser to be applied
                     * @param <H> the type of the next parser's result
                     * @return a new {@code ApplyBuilder7} instance with the next parser
                     */
                    public <H> ApplyBuilder7<H> then(Parser<H> ph) {
                        return new ApplyBuilder7<>(ph);
                    }

                    /**
                     * {@code ApplyBuilder7} is a builder class for combining parsers with seven levels of parsing.
                     * <p>
                     * This class allows for the sequential combination of parsers, where each parser can be followed
                     * by another parser or skipped. The results of the parsers can be mapped to a new result using
                     * a function that takes seven arguments.
                     *
                     * @param <H> the type of the seventh parser's result
                     */
                    public class ApplyBuilder7<H> {
                        private final Parser<H> ph;

                        private ApplyBuilder7(Parser<H> ph) {
                            this.ph = ph;
                        }

                        /**
                         * Maps the results of the parsers to a new result using the provided function.
                         *
                         * @param f   the function to map the results
                         * @param <R> the result type
                         * @return a new parser with the mapped result
                         */
                        public <R> Parser<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f) {
                            return apply(ApplyBuilder6.this.map(f), ph);
                        }

                        /**
                         * Maps the results of the parsers to a new result using the provided function with seven arguments.
                         *
                         * @param f   the function to map the results
                         * @param <R> the result type
                         * @return a new parser with the mapped result
                         */
                        public <R> Parser<R> map(Functions.Func7<A, B, C, D, E, G, H, R> f) {
                            return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a, b, c, d, e, g, h));
                        }

                        /**
                         * Adds a parser to be skipped after the current parser.
                         *
                         * @param pj  the parser to be skipped
                         * @param <J> the type of the skipped parser's result
                         * @return a new {@code ApplyBuilder7} instance with the skipped parser
                         */
                        public <J> ApplyBuilder7<H> thenSkip(Parser<J> pj) {
                            return new ApplyBuilder7<>(ph.thenSkip(pj));
                        }

                        /**
                         * Adds a parser to be applied after the current parser, skipping the results of the current builder.
                         *
                         * @param pj  the parser to be applied
                         * @param <J> the type of the next parser's result
                         * @return a new parser that applies the current parsers and then the next parser, returning only the result of the next parser
                         */
                        public <J> Parser<J> skipThen(Parser<J> pj) {
                            return attempt(ApplyBuilder.this.allSkipped().thenSkip(pc).thenSkip(pd).thenSkip(pe).thenSkip(pg).thenSkip(ph).skipThen(pj));
                        }

                        /**
                         * Adds a parser to be applied after the current parser.
                         *
                         * @param pj  the parser to be applied
                         * @param <J> the type of the next parser's result
                         * @return a new {@code ApplyBuilder8} instance with the next parser
                         */
                        public <J> ApplyBuilder8<J> then(Parser<J> pj) {
                            return new ApplyBuilder8<>(pj);
                        }

                        /**
                         * {@code ApplyBuilder8} is a builder class for combining parsers with eight levels of parsing.
                         * <p>
                         * This class allows for the sequential combination of parsers, where each parser can be followed
                         * by another parser or skipped. The results of the parsers can be mapped to a new result using
                         * a function that takes eight arguments.</p>
                         *
                         * @param <J> the type of the eighth parser's result
                         */
                        public class ApplyBuilder8<J> {
                            private final Parser<J> pj;

                            private ApplyBuilder8(Parser<J> pj) {
                                this.pj = pj;
                            }

                            /**
                             * Maps the results of the parsers to a new result using the provided function.
                             *
                             * @param f   the function to map the results
                             * @param <R> the result type
                             * @return a new parser with the mapped result
                             */
                            public <R> Parser<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f) {
                                return apply(ApplyBuilder7.this.map(f), pj);
                            }

                            /**
                             * Maps the results of the parsers to a new result using the provided function with eight arguments.
                             *
                             * @param f   the function to map the results
                             * @param <R> the result type
                             * @return a new parser with the mapped result
                             */
                            public <R> Parser<R> map(Functions.Func8<A, B, C, D, E, G, H, J, R> f) {
                                return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a, b, c, d, e, g, h, j));
                            }

                            /**
                             * Adds a parser to be skipped after the current parser.
                             *
                             * @param pk  the parser to be skipped
                             * @param <K> the type of the skipped parser's result
                             * @return a new {@code ApplyBuilder8} instance with the skipped parser
                             */
                            public <K> ApplyBuilder8<J> thenSkip(Parser<K> pk) {
                                return new ApplyBuilder8<>(pj.thenSkip(pk));
                            }

                            /**
                             * Adds a parser to be applied after the current parser, skipping the results of the current builder.
                             *
                             * @param pk  the parser to be applied
                             * @param <K> the type of the next parser's result
                             * @return a new parser that applies the current parsers and then the next parser, returning only the result of the next parser
                             */
                            public <K> Parser<K> skipThen(Parser<K> pk) {
                                return attempt(ApplyBuilder.this.allSkipped().thenSkip(pc).thenSkip(pd).thenSkip(pe).thenSkip(pg).thenSkip(ph).thenSkip(pj).skipThen(pk));
                            }
                        }
                    }
                }
            }
        }
    }
}