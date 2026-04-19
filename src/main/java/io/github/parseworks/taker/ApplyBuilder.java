package io.github.parseworks.taker;

import io.github.parseworks.taker.impl.result.Match;
import io.github.parseworks.taker.parsers.Lexical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Fluent builder for combining multiple parsers sequentially.
 */
abstract class BaseApplyBuilder {
    protected final List<Taker<Object>> ps;

    protected BaseApplyBuilder(List<Taker<Object>> ps) {
        this.ps = ps;
    }

    protected <R> Result<R> applyAll(Input in, Function<Object[], R> f) {
        Object[] results = new Object[ps.size()];
        Input current = in;
        for (int i = 0; i < ps.size(); i++) {
            Result<Object> r = ps.get(i).apply(current);
            if (!r.matches()) return r.cast();
            results[i] = r.value();
            current = r.input();
        }
        return new Match<>(f.apply(results), current);
    }

    protected <J> List<Taker<Object>> append(Taker<J> pj) {
        List<Taker<Object>> next = new ArrayList<>(ps);
        next.add((Taker<Object>) pj);
        return next;
    }

    protected <J> List<Taker<Object>> updateLast(Taker<J> pj) {
        List<Taker<Object>> next = new ArrayList<>(ps);
        int last = next.size() - 1;
        next.set(last, next.get(last).thenSkip(pj));
        return next;
    }

    public <J> Taker<J> skipThen(Taker<J> pj) {
        Taker<Object> combined = ps.get(0);
        for (int i = 1; i < ps.size(); i++) {
            combined = combined.thenSkip(ps.get(i));
        }
        return combined.skipThen(pj);
    }
}

public class ApplyBuilder<A, B> extends BaseApplyBuilder {

    @SuppressWarnings("unchecked")
    public ApplyBuilder(Taker<A> pa, Taker<B> pb) {
        super(List.of((Taker<Object>) pa, (Taker<Object>) pb));
    }

    protected ApplyBuilder(List<Taker<Object>> parsers) {
        super(parsers);
    }

    public static <A, B> ApplyBuilder<A, B> of(Taker<A> pa, Taker<B> pb) {
        return new ApplyBuilder<>(pa, pb);
    }

    public static <A, B> Taker<B> apply(Taker<Function<A, B>> pf, Taker<A> pa) {
        return new Taker<>(in -> {
            Result<Function<A, B>> rf = pf.apply(in);
            if (!rf.matches()) return rf.cast();
            Result<A> ra = pa.apply(rf.input());
            if (!ra.matches()) return ra.cast();
            return ra.map(rf.value());
        });
    }

    public static <A, B> Taker<B> apply(Function<A, B> f, Taker<A> pa) {
        return apply(Lexical.pure(f), pa);
    }

    public static <A, B> Taker<B> apply(Taker<Function<A, B>> pf, A a) {
        return apply(pf, Lexical.pure(a));
    }

    public <R> Taker<R> map2(Function<A, Function<B, R>> f) {
        return apply(pa().map(f), pb());
    }

    @SuppressWarnings("unchecked")
    private Taker<A> pa() { return (Taker<A>) ps.get(0); }
    @SuppressWarnings("unchecked")
    private Taker<B> pb() { return (Taker<B>) ps.get(1); }

    @SuppressWarnings("unchecked")
    public <R> Taker<R> map2(BiFunction<A, B, R> f) {
        return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1])));
    }

    @SuppressWarnings("unchecked")
    public <C> ApplyBuilder<A, B> thenSkip(Taker<C> pc) {
        return new ApplyBuilder<>(updateLast(pc));
    }

    @SuppressWarnings("unchecked")
    public <C> ApplyBuilder3<C> then(Taker<C> pc) {
        return new ApplyBuilder3<>(append(pc));
    }

    public class ApplyBuilder3<C> extends BaseApplyBuilder {

        protected ApplyBuilder3(List<Taker<Object>> ps) {
            super(ps);
        }

        public <R> Taker<R> map3(Function<A, Function<B, Function<C, R>>> f) {
            return map3(a -> b -> c -> f.apply(a).apply(b).apply(c));
        }

        @SuppressWarnings("unchecked")
        public <R> Taker<R> map3(Functions.Func3<A, B, C, R> f) {
            return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1], (C) res[2])));
        }

        @SuppressWarnings("unchecked")
        public <D> ApplyBuilder4<C, D> then(Taker<D> pd) {
            return new ApplyBuilder4<>(append(pd));
        }

        public <D> ApplyBuilder3<C> thenSkip(Taker<D> pd) {
            return new ApplyBuilder3<>(updateLast(pd));
        }
    }

    public class ApplyBuilder4<C, D> extends BaseApplyBuilder {

        protected ApplyBuilder4(List<Taker<Object>> ps) {
            super(ps);
        }

        public <R> Taker<R> map4(Function<A, Function<B, Function<C, Function<D, R>>>> f) {
            return map4(a -> b -> c -> d -> f.apply(a).apply(b).apply(c).apply(d));
        }

        @SuppressWarnings("unchecked")
        public <R> Taker<R> map4(Functions.Func4<A, B, C, D, R> f) {
            return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1], (C) res[2], (D) res[3])));
        }

        @SuppressWarnings("unchecked")
        public <E> ApplyBuilder5<C, D, E> then(Taker<E> pe) {
            return new ApplyBuilder5<>(append(pe));
        }

        public <E> ApplyBuilder4<C, D> thenSkip(Taker<E> pe) {
            return new ApplyBuilder4<>(updateLast(pe));
        }
    }

    public class ApplyBuilder5<C, D, E> extends BaseApplyBuilder {

        protected ApplyBuilder5(List<Taker<Object>> ps) {
            super(ps);
        }

        public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) {
            return map(a -> b -> c -> d -> e -> f.apply(a).apply(b).apply(c).apply(d).apply(e));
        }

        @SuppressWarnings("unchecked")
        public <R> Taker<R> map(Functions.Func5<A, B, C, D, E, R> f) {
            return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1], (C) res[2], (D) res[3], (E) res[4])));
        }

        @SuppressWarnings("unchecked")
        public <G> ApplyBuilder6<C, D, E, G> then(Taker<G> pg) {
            return new ApplyBuilder6<>(append(pg));
        }

        public <G> ApplyBuilder5<C, D, E> thenSkip(Taker<G> pg) {
            return new ApplyBuilder5<>(updateLast(pg));
        }
    }

    public class ApplyBuilder6<C, D, E, G> extends BaseApplyBuilder {

        protected ApplyBuilder6(List<Taker<Object>> ps) {
            super(ps);
        }

        public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f) {
            return map(a -> b -> c -> d -> e -> g -> f.apply(a).apply(b).apply(c).apply(d).apply(e).apply(g));
        }

        @SuppressWarnings("unchecked")
        public <R> Taker<R> map(Functions.Func6<A, B, C, D, E, G, R> f) {
            return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1], (C) res[2], (D) res[3], (E) res[4], (G) res[5])));
        }

        @SuppressWarnings("unchecked")
        public <H> ApplyBuilder7<C, D, E, G, H> then(Taker<H> ph) {
            return new ApplyBuilder7<>(append(ph));
        }

        public <H> ApplyBuilder6<C, D, E, G> thenSkip(Taker<H> ph) {
            return new ApplyBuilder6<>(updateLast(ph));
        }
    }

    public class ApplyBuilder7<C, D, E, G, H> extends BaseApplyBuilder {

        protected ApplyBuilder7(List<Taker<Object>> ps) {
            super(ps);
        }

        public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f) {
            return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a).apply(b).apply(c).apply(d).apply(e).apply(g).apply(h));
        }

        @SuppressWarnings("unchecked")
        public <R> Taker<R> map(Functions.Func7<A, B, C, D, E, G, H, R> f) {
            return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1], (C) res[2], (D) res[3], (E) res[4], (G) res[5], (H) res[6])));
        }

        @SuppressWarnings("unchecked")
        public <J> ApplyBuilder8<C, D, E, G, H, J> then(Taker<J> pj) {
            return new ApplyBuilder8<>(append(pj));
        }

        public <J> ApplyBuilder7<C, D, E, G, H> thenSkip(Taker<J> pj) {
            return new ApplyBuilder7<>(updateLast(pj));
        }
    }

    public class ApplyBuilder8<C, D, E, G, H, J> extends BaseApplyBuilder {

        protected ApplyBuilder8(List<Taker<Object>> ps) {
            super(ps);
        }

        public <R> Taker<R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f) {
            return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a).apply(b).apply(c).apply(d).apply(e).apply(g).apply(h).apply(j));
        }

        @SuppressWarnings("unchecked")
        public <R> Taker<R> map(Functions.Func8<A, B, C, D, E, G, H, J, R> f) {
            return new Taker<>(in -> applyAll(in, res -> f.apply((A) res[0], (B) res[1], (C) res[2], (D) res[3], (E) res[4], (G) res[5], (H) res[6], (J) res[7])));
        }

        public <K> ApplyBuilder8<C, D, E, G, H, J> thenSkip(Taker<K> pk) {
            return new ApplyBuilder8<>(updateLast(pk));
        }
    }
}
