/*
 * Copyright (c) 2026 jason bailey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.parseworks.taker;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.parseworks.taker.parsers.Combinators.pure;

/**
 * Fluent builder for combining multiple parsers sequentially.
 * <p>
 * Created by {@link Taker#then(Taker)}. Chain with {@code then},
 * {@code thenSkip}, or {@code skipThen}, and conclude with {@code map}.

 * @param <A> first parser result type
 * @param <B> second parser result type
 */
public class ApplyBuilder<A, B> {
    /**
     * Function of arity 3.
     *
     * @param <A> first argument type
     * @param <B> second argument type
     * @param <C> third argument type
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Func3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    /**
     * Function of arity 4.
     *
     * @param <A> first argument type
     * @param <B> second argument type
     * @param <C> third argument type
     * @param <D> fourth argument type
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Func4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    /**
     * Function of arity 5.
     *
     * @param <A> first argument type
     * @param <B> second argument type
     * @param <C> third argument type
     * @param <D> fourth argument type
     * @param <E> fifth argument type
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Func5<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    /**
     * Function of arity 6.
     *
     * @param <A> first argument type
     * @param <B> second argument type
     * @param <C> third argument type
     * @param <D> fourth argument type
     * @param <E> fifth argument type
     * @param <G> sixth argument type
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Func6<A, B, C, D, E, G, R> {
        R apply(A a, B b, C c, D d, E e, G g);
    }

    /**
     * Function of arity 7.
     *
     * @param <A> first argument type
     * @param <B> second argument type
     * @param <C> third argument type
     * @param <D> fourth argument type
     * @param <E> fifth argument type
     * @param <G> sixth argument type
     * @param <H> seventh argument type
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Func7<A, B, C, D, E, G, H, R> {
        R apply(A a, B b, C c, D d, E e, G g, H h);
    }

    /**
     * Function of arity 8.
     *
     * @param <A> first argument type
     * @param <B> second argument type
     * @param <C> third argument type
     * @param <D> fourth argument type
     * @param <E> fifth argument type
     * @param <G> sixth argument type
     * @param <H> seventh argument type
     * @param <I> eighth argument type
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Func8<A, B, C, D, E, G, H, I, R> {
        R apply(A a, B b, C c, D d, E e, G g, H h, I i);
    }

    private final Taker<A> pa;
    private final Taker<B> pb;

    /**
     * Creates a sequence builder.
     *
     * @param pa first parser
     * @param pb second parser
     */
    public ApplyBuilder(Taker<A> pa, Taker<B> pb) {
        Objects.requireNonNull(pa, "pa");
        Objects.requireNonNull(pb, "pb");
        this.pa = pa;
        this.pb = pb;
    }

    /**
     * Creates a sequence builder.
     *
     * @param pa first parser
     * @param pb second parser
     * @param <A> first parser result type
     * @param <B> second parser result type
     * @return a builder for the two parsers
     */
    public static <A, B> ApplyBuilder<A, B> of(Taker<A> pa, Taker<B> pb) {
        return new ApplyBuilder<>(pa, pb);
    }

    /**
     * Applies a parsed function to a parsed value.
     *
     * @param functionProvider parser that produces a function
     * @param valueParser parser that produces a value
     * @param <A> function input type
     * @param <B> function output type
     * @return a parser returning the applied result
     */
    public static <A, B> Taker<B> apply(Taker<Function<A, B>> functionProvider, Taker<A> valueParser) {
        Objects.requireNonNull(functionProvider, "functionProvider");
        Objects.requireNonNull(valueParser, "valueParser");
        return new Taker<>(in -> {
            Result<Function<A, B>> functionResult = functionProvider.apply(in);
            if (!functionResult.matches()) return functionResult.cast();
            Function<A, B> func = functionResult.value();
            Result<A> valueResult = valueParser.apply(functionResult.input());
            if (!valueResult.matches()) {
                return valueResult.cast();
            }
            return valueResult.map(func);
        });
    }

    /**
     * Applies a function to a parsed value.
     *
     * @param f function to apply
     * @param pa value parser
     * @param <A> function input type
     * @param <B> function output type
     * @return a parser returning the applied result
     */
    public static <A, B> Taker<B> apply(Function<A, B> f, Taker<A> pa) {
        Objects.requireNonNull(f, "f");
        return apply(pure(f), pa);
    }

    /**
     * Applies a parsed function to a constant value.
     *
     * @param pf function parser
     * @param a constant value
     * @param <A> function input type
     * @param <B> function output type
     * @return a parser returning the applied result
     */
    public static <A, B> Taker<B> apply(Taker<Function<A, B>> pf, A a) {
        Objects.requireNonNull(pf, "pf");
        return apply(pf, pure(a));
    }

    /**
     * Maps the two parsed values with a curried function.
     *
     * @param f curried mapper
     * @param <R> mapped result type
     * @return a mapped parser
     */
    public <R> Taker<R> map(Function<A, Function<B, R>> f) {
        Objects.requireNonNull(f, "f");
        return apply(pa.map(f), pb);
    }

    /**
     * Maps the two parsed values.
     *
     * @param f mapper function
     * @param <R> mapped result type
     * @return a mapped parser
     */
    public <R> Taker<R> map(BiFunction<A, B, R> f) {
        Objects.requireNonNull(f, "f");
        return ApplyBuilder.<B, R>apply(pa.map(a -> b -> f.apply(a, b)), pb);
    }

    /**
     * Parses another parser after this sequence and discards its value.
     *
     * @param pc parser to skip
     * @param <C> skipped parser result type
     * @return this builder with the skipped parser appended
     */
    public <C> ApplyBuilder<A, B> thenSkip(Taker<C> pc) {
        Objects.requireNonNull(pc, "pc");
        return new ApplyBuilder<>(pa, pb.thenSkip(pc));
    }

    /**
     * Parses another parser after this sequence and returns its value.
     *
     * @param pc parser to return
     * @param <C> returned parser result type
     * @return a parser returning {@code pc}'s value
     */
    public <C> Taker<C> skipThen(Taker<C> pc) {
        Objects.requireNonNull(pc, "pc");
        return allSkipped().skipThen(pc);
    }

    private Taker<Void> allSkipped() {
        return pa.thenSkip(pb).map(any -> null);
    }

    /**
     * Appends a parser to this sequence.
     *
     * @param pc parser to append
     * @param <C> appended parser result type
     * @return a builder for three parsed values
     */
    public <C> ApplyBuilder3<C> then(Taker<C> pc) {
        Objects.requireNonNull(pc, "pc");
        return new ApplyBuilder3<>(pc);
    }

    /** Builder for mapping three parsed values. */
    public class ApplyBuilder3<C> {
        private final Taker<C> pc;

        private ApplyBuilder3(Taker<C> pc) {
            this.pc = Objects.requireNonNull(pc, "pc");
        }

        public <R> Taker<R> map(Function<A, Function<B, Function<C, R>>> f) {
            Objects.requireNonNull(f, "f");
            return apply(ApplyBuilder.this.map(f), pc);
        }

        public <R> Taker<R> map(Func3<A, B, C, R> f) {
            Objects.requireNonNull(f, "f");
            return map(a -> b -> c -> f.apply(a, b, c));
        }

        public <D> ApplyBuilder3<C> thenSkip(Taker<D> pd) {
            Objects.requireNonNull(pd, "pd");
            return new ApplyBuilder3<>(pc.thenSkip(pd));
        }

        public <D> Taker<D> skipThen(Taker<D> pd) {
            Objects.requireNonNull(pd, "pd");
            return allSkipped().skipThen(pd);
        }

        private Taker<Void> allSkipped() {
            return ApplyBuilder.this.allSkipped().thenSkip(pc).map(any -> null);
        }

        public <D> ApplyBuilder4<D> then(Taker<D> pd) {
            Objects.requireNonNull(pd, "pd");
            return new ApplyBuilder4<>(pd);
        }

        /** Builder for mapping four parsed values. */
        public class ApplyBuilder4<D> {
            private final Taker<D> pd;

            private ApplyBuilder4(Taker<D> pd) {
                this.pd = Objects.requireNonNull(pd, "pd");
            }

            public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, R>>>> f) {
                Objects.requireNonNull(f, "f");
                return apply(ApplyBuilder3.this.map(f), pd);
            }

            public <R> Taker<R> map(Func4<A, B, C, D, R> f) {
                Objects.requireNonNull(f, "f");
                return map(a -> b -> c -> d -> f.apply(a, b, c, d));
            }

            public <E> ApplyBuilder4<D> thenSkip(Taker<E> pe) {
                Objects.requireNonNull(pe, "pe");
                return new ApplyBuilder4<>(pd.thenSkip(pe));
            }

            public <E> Taker<E> skipThen(Taker<E> pe) {
                Objects.requireNonNull(pe, "pe");
                return allSkipped().skipThen(pe);
            }

            private Taker<Void> allSkipped() {
                return ApplyBuilder3.this.allSkipped().thenSkip(pd).map(any -> null);
            }

            public <E> ApplyBuilder5<E> then(Taker<E> pe) {
                Objects.requireNonNull(pe, "pe");
                return new ApplyBuilder5<>(pe);
            }

            /** Builder for mapping five parsed values. */
            public class ApplyBuilder5<E> {
                private final Taker<E> pe;

                private ApplyBuilder5(Taker<E> pe) {
                    this.pe = Objects.requireNonNull(pe, "pe");
                }

                public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) {
                    Objects.requireNonNull(f, "f");
                    return apply(ApplyBuilder4.this.map(f), pe);
                }

                public <R> Taker<R> map(Func5<A, B, C, D, E, R> f) {
                    Objects.requireNonNull(f, "f");
                    return map(a -> b -> c -> d -> e -> f.apply(a, b, c, d, e));
                }

                public <G> ApplyBuilder5<E> thenSkip(Taker<G> pg) {
                    Objects.requireNonNull(pg, "pg");
                    return new ApplyBuilder5<>(pe.thenSkip(pg));
                }

                public <G> Taker<G> skipThen(Taker<G> pg) {
                    Objects.requireNonNull(pg, "pg");
                    return allSkipped().skipThen(pg);
                }

                private Taker<Void> allSkipped() {
                    return ApplyBuilder4.this.allSkipped().thenSkip(pe).map(any -> null);
                }

                public <G> ApplyBuilder6<G> then(Taker<G> pg) {
                    Objects.requireNonNull(pg, "pg");
                    return new ApplyBuilder6<>(pg);
                }

                /** Builder for mapping six parsed values. */
                public class ApplyBuilder6<G> {
                    private final Taker<G> pg;

                    private ApplyBuilder6(Taker<G> pg) {
                        this.pg = Objects.requireNonNull(pg, "pg");
                    }

                    public <R> Taker<R> map(
                        Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f
                    ) {
                        Objects.requireNonNull(f, "f");
                        return apply(ApplyBuilder5.this.map(f), pg);
                    }

                    public <R> Taker<R> map(Func6<A, B, C, D, E, G, R> f) {
                        Objects.requireNonNull(f, "f");
                        return map(a -> b -> c -> d -> e -> g -> f.apply(a, b, c, d, e, g));
                    }

                    public <H> ApplyBuilder6<G> thenSkip(Taker<H> ph) {
                        Objects.requireNonNull(ph, "ph");
                        return new ApplyBuilder6<>(pg.thenSkip(ph));
                    }

                    public <H> Taker<H> skipThen(Taker<H> ph) {
                        Objects.requireNonNull(ph, "ph");
                        return allSkipped().skipThen(ph);
                    }

                    private Taker<Void> allSkipped() {
                        return ApplyBuilder5.this.allSkipped().thenSkip(pg).map(any -> null);
                    }

                    public <H> ApplyBuilder7<H> then(Taker<H> ph) {
                        Objects.requireNonNull(ph, "ph");
                        return new ApplyBuilder7<>(ph);
                    }

                    /** Builder for mapping seven parsed values. */
                    public class ApplyBuilder7<H> {
                        private final Taker<H> ph;

                        private ApplyBuilder7(Taker<H> ph) {
                            this.ph = Objects.requireNonNull(ph, "ph");
                        }

                        public <R> Taker<R> map(
                            Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f
                        ) {
                            Objects.requireNonNull(f, "f");
                            return apply(ApplyBuilder6.this.map(f), ph);
                        }

                        public <R> Taker<R> map(Func7<A, B, C, D, E, G, H, R> f) {
                            Objects.requireNonNull(f, "f");
                            return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a, b, c, d, e, g, h));
                        }

                        public <J> ApplyBuilder7<H> thenSkip(Taker<J> pj) {
                            Objects.requireNonNull(pj, "pj");
                            return new ApplyBuilder7<>(ph.thenSkip(pj));
                        }

                        public <J> Taker<J> skipThen(Taker<J> pj) {
                            Objects.requireNonNull(pj, "pj");
                            return allSkipped().skipThen(pj);
                        }

                        private Taker<Void> allSkipped() {
                            return ApplyBuilder6.this.allSkipped().thenSkip(ph).map(any -> null);
                        }

                        public <J> ApplyBuilder8<J> then(Taker<J> pj) {
                            Objects.requireNonNull(pj, "pj");
                            return new ApplyBuilder8<>(pj);
                        }

                        /** Builder for mapping eight parsed values. */
                        public class ApplyBuilder8<J> {
                            private final Taker<J> pj;

                            private ApplyBuilder8(Taker<J> pj) {
                                this.pj = Objects.requireNonNull(pj, "pj");
                            }

                            public <R> Taker<R> map(
                                Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f
                            ) {
                                Objects.requireNonNull(f, "f");
                                return apply(ApplyBuilder7.this.map(f), pj);
                            }

                            public <R> Taker<R> map(Func8<A, B, C, D, E, G, H, J, R> f) {
                                Objects.requireNonNull(f, "f");
                                return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a, b, c, d, e, g, h, j));
                            }

                            public <K> ApplyBuilder8<J> thenSkip(Taker<K> pk) {
                                Objects.requireNonNull(pk, "pk");
                                return new ApplyBuilder8<>(pj.thenSkip(pk));
                            }

                            public <K> Taker<K> skipThen(Taker<K> pk) {
                                Objects.requireNonNull(pk, "pk");
                                return allSkipped().skipThen(pk);
                            }

                            private Taker<Void> allSkipped() {
                                return ApplyBuilder7.this.allSkipped().thenSkip(pj).map(any -> null);
                            }
                        }
                    }
                }
            }
        }
    }
}
