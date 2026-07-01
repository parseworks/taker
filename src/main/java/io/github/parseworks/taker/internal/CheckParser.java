package io.github.parseworks.taker.internal;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import io.github.parseworks.taker.results.NoMatch;

/**
 * A parser wrapper that checks for infinite recursion.
 *
 * @param <A> result type
 */
public class CheckParser<A> extends Taker<A> {

    /**
     * Applies the parser to the input, ensuring that the same parser is not
     * applied at the same position recursively.
     *
     * @param in the input to parse
     * @return the parse result
     */
    @Override
    public Result<A> apply(Input in) {
        int pos = in.position();
        LinearMap ctx = in.context();

        if (LinearMap.contains(ctx, pos, this)) {
            return new NoMatch<>(in, "no infinite recursion");
        }

        Input inWithCtx = in.withContext(LinearMap.push(ctx, pos, this));

        try {
            return applyHandler.apply(inWithCtx);
        } catch (RuntimeException e) {
            if (e instanceof IllegalStateException && "Taker not initialized".equals(e.getMessage())) {
                throw e;
            }
            return new NoMatch<>(in, "parser to function correctly");
        }
    }
}
