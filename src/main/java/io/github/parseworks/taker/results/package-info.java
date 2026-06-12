/**
 * Low-level result implementations for custom parser authors.
 * <p>
 * Most users should depend on {@link io.github.parseworks.taker.Result} and
 * {@link io.github.parseworks.taker.Failure}. These concrete records are
 * exposed for code that builds custom {@link io.github.parseworks.taker.Taker}
 * instances and needs to return success, recoverable failure, or committed
 * failure results directly.
 * <p>
 * {@link io.github.parseworks.taker.results.Match} represents success,
 * {@link io.github.parseworks.taker.results.NoMatch} represents recoverable
 * failure, and {@link io.github.parseworks.taker.results.PartialMatch}
 * represents committed failure.
 */
package io.github.parseworks.taker.results;
