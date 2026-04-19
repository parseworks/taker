package io.github.parseworks.taker;

import io.github.parseworks.taker.Result;

import java.util.List;

public interface ResultError<A> extends Result<A> {

    String message();

    String message(int depth);

    String expected();

    ResultError<?> cause();

    List<ResultError<A>> combinedFailures();
}
