package io.github.parseworks.taker;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Fluent builder for combining multiple parsers sequentially.
 * <p>
 * Created by {@link Taker#then(Taker)}. Chain with {@code .then()}, {@code .thenSkip()},
 * or {@code .skipThen()}, and conclude with {@code .map()}.
 * <pre>{@code
 * Taker<Integer> sum =
 *     Numeric.number.thenSkip(Lexical.chr('+'))
 *                   .then(Numeric.number)
 *                   .map((a, b) -> a + b);
 * }</pre>

 * @param <A> first parser result type
 * @param <B> second parser result type
 */
public class ApplyBuilder<A, B> {

    protected final Taker<A> pa;
    protected final Taker<B> pb;

    public ApplyBuilder(Taker<A> pa, Taker<B> pb) {
        this.pa = pa;
        this.pb = pb;
    }

    public static <A, B> ApplyBuilder<A, B> of(Taker<A> pa, Taker<B> pb) {
        return new ApplyBuilder<>(pa, pb);
    }

    public static <A, B> Taker<B> apply(Taker<Function<A, B>> functionProvider, Taker<A> valueParser) {
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

    public static <A, B> Taker<B> apply(Function<A, B> f, Taker<A> pa) {
        return apply(Taker.pure(f), pa);
    }

    public static <A, B> Taker<B> apply(Taker<Function<A, B>> pf, A a) {
        return apply(pf, Taker.pure(a));
    }

    public <R> Taker<R> map(Function<A, Function<B, R>> f) {
        return apply(pa.map(f), pb);
    }

    public <R> Taker<R> map(BiFunction<A, B, R> f) {
        return ApplyBuilder.<B, R>apply(pa.map(a -> b -> f.apply(a, b)), pb);
    }

    public <C> ApplyBuilder<A, B> thenSkip(Taker<C> pc) {
        return new ApplyBuilder<>(pa, pb.thenSkip(pc));
    }

    public <C> Taker<C> skipThen(Taker<C> pc) {
        return allSkipped().skipThen(pc);
    }

    private Taker<Void> allSkipped() {
        return pa.thenSkip(pb).map(any -> null);
    }

    public <C> ApplyBuilder3<C> then(Taker<C> pc) {
        return new ApplyBuilder3<>(pc);
    }

    public class ApplyBuilder3<C> {
        private final Taker<C> pc;
        private ApplyBuilder3(Taker<C> pc) { this.pc = pc; }
        public <R> Taker<R> map(Function<A, Function<B, Function<C, R>>> f) { return apply(ApplyBuilder.this.map(f), pc); }
        public <R> Taker<R> map(Functions.Func3<A, B, C, R> f) { return map(a -> b -> c -> f.apply(a, b, c)); }
        public <D> ApplyBuilder3<C> thenSkip(Taker<D> pd) { return new ApplyBuilder3<>(pc.thenSkip(pd)); }
        public <D> Taker<D> skipThen(Taker<D> pd) { return allSkipped().skipThen(pd); }
        private Taker<Void> allSkipped() { return ApplyBuilder.this.allSkipped().thenSkip(pc).map(any -> null); }
        public <D> ApplyBuilder4<D> then(Taker<D> pd) { return new ApplyBuilder4<>(pd); }

        public class ApplyBuilder4<D> {
            private final Taker<D> pd;
            private ApplyBuilder4(Taker<D> pd) { this.pd = pd; }
            public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, R>>>> f) { return apply(ApplyBuilder3.this.map(f), pd); }
            public <R> Taker<R> map(Functions.Func4<A, B, C, D, R> f) { return map(a -> b -> c -> d -> f.apply(a, b, c, d)); }
            public <E> ApplyBuilder4<D> thenSkip(Taker<E> pe) { return new ApplyBuilder4<>(pd.thenSkip(pe)); }
            public <E> Taker<E> skipThen(Taker<E> pe) { return allSkipped().skipThen(pe); }
            private Taker<Void> allSkipped() { return ApplyBuilder3.this.allSkipped().thenSkip(pd).map(any -> null); }
            public <E> ApplyBuilder5<E> then(Taker<E> pe) { return new ApplyBuilder5<>(pe); }

            public class ApplyBuilder5<E> {
                private final Taker<E> pe;
                private ApplyBuilder5(Taker<E> pe) { this.pe = pe; }
                public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) { return apply(ApplyBuilder4.this.map(f), pe); }
                public <R> Taker<R> map(Functions.Func5<A, B, C, D, E, R> f) { return map(a -> b -> c -> d -> e -> f.apply(a, b, c, d, e)); }
                public <G> ApplyBuilder5<E> thenSkip(Taker<G> pg) { return new ApplyBuilder5<>(pe.thenSkip(pg)); }
                public <G> Taker<G> skipThen(Taker<G> pg) { return allSkipped().skipThen(pg); }
                private Taker<Void> allSkipped() { return ApplyBuilder4.this.allSkipped().thenSkip(pe).map(any -> null); }
                public <G> ApplyBuilder6<G> then(Taker<G> pg) { return new ApplyBuilder6<>(pg); }

                public class ApplyBuilder6<G> {
                    private final Taker<G> pg;
                    private ApplyBuilder6(Taker<G> pg) { this.pg = pg; }
                    public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f) { return apply(ApplyBuilder5.this.map(f), pg); }
                    public <R> Taker<R> map(Functions.Func6<A, B, C, D, E, G, R> f) { return map(a -> b -> c -> d -> e -> g -> f.apply(a, b, c, d, e, g)); }
                    public <H> ApplyBuilder6<G> thenSkip(Taker<H> ph) { return new ApplyBuilder6<>(pg.thenSkip(ph)); }
                    public <H> Taker<H> skipThen(Taker<H> ph) { return allSkipped().skipThen(ph); }
                    private Taker<Void> allSkipped() { return ApplyBuilder5.this.allSkipped().thenSkip(pg).map(any -> null); }
                    public <H> ApplyBuilder7<H> then(Taker<H> ph) { return new ApplyBuilder7<>(ph); }

                    public class ApplyBuilder7<H> {
                        private final Taker<H> ph;
                        private ApplyBuilder7(Taker<H> ph) { this.ph = ph; }
                        public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f) { return apply(ApplyBuilder6.this.map(f), ph); }
                        public <R> Taker<R> map(Functions.Func7<A, B, C, D, E, G, H, R> f) { return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a, b, c, d, e, g, h)); }
                        public <J> ApplyBuilder7<H> thenSkip(Taker<J> pj) { return new ApplyBuilder7<>(ph.thenSkip(pj)); }
                        public <J> Taker<J> skipThen(Taker<J> pj) { return allSkipped().skipThen(pj); }
                        private Taker<Void> allSkipped() { return ApplyBuilder6.this.allSkipped().thenSkip(ph).map(any -> null); }
                        public <J> ApplyBuilder8<J> then(Taker<J> pj) { return new ApplyBuilder8<>(pj); }

                        public class ApplyBuilder8<J> {
                            private final Taker<J> pj;
                            private ApplyBuilder8(Taker<J> pj) { this.pj = pj; }
                            public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f) { return apply(ApplyBuilder7.this.map(f), pj); }
                            public <R> Taker<R> map(Functions.Func8<A, B, C, D, E, G, H, J, R> f) { return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a, b, c, d, e, g, h, j)); }
                            public <K> ApplyBuilder8<J> thenSkip(Taker<K> pk) { return new ApplyBuilder8<>(pj.thenSkip(pk)); }
                            public <K> Taker<K> skipThen(Taker<K> pk) { return allSkipped().skipThen(pk); }
                            private Taker<Void> allSkipped() { return ApplyBuilder7.this.allSkipped().thenSkip(pj).map(any -> null); }
                        }
                    }
                }
            }
        }
    }
}