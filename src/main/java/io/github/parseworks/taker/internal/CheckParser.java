package io.github.parseworks.taker.internal;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.NoMatch;

import java.util.ArrayDeque;

public class CheckParser<A> extends Taker<A> {

    private final ThreadLocal<IntObjectMap<ArrayDeque<Taker<?>>>> contextLocal =
        ThreadLocal.withInitial(IntObjectMap::new);

    @Override
    public Result<A> apply(Input in) {
        int pos = in.position();
        IntObjectMap<ArrayDeque<Taker<?>>> ctx = contextLocal.get();

        ArrayDeque<Taker<?>> stack = ctx.get(pos);
        if (stack == null) {
            stack = new ArrayDeque<>();
            ctx.put(pos, stack);
        }

        for (Taker<?> p : stack) {
            if (p == this) {
                return new NoMatch<>(in, "no infinite recursion");
            }
        }

        stack.push(this);
        try {
            return applyHandler.apply(in);
        } catch (RuntimeException e) {
            if (e instanceof IllegalStateException && "Taker not initialized".equals(e.getMessage())) {
                throw e;
            }
            return new NoMatch<>(in, "parser to function correctly");
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                ctx.remove(pos);
            }
        }
    }
}
