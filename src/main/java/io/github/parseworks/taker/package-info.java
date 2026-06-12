/**
 * Core parser types for Taker.
 * <p>
 * The main entry point is {@link io.github.parseworks.taker.Taker}, a parser
 * value with fluent instance combinators such as {@code then}, {@code or},
 * {@code map}, {@code oneOrMore}, {@code located}, and {@code expecting}.
 * Supporting types model immutable input cursors, parser results, failures, and
 * source spans. Inputs represent the original source text; case-insensitive
 * matching is handled by parser and predicate helpers rather than transformed
 * input cursors.
 * <p>
 * Static parser factories are intentionally kept out of this package-level API
 * surface where possible. Use {@link io.github.parseworks.taker.parsers.Lexical}
 * for text and character parsers, {@link io.github.parseworks.taker.parsers.Numeric}
 * for numeric parsers, and {@link io.github.parseworks.taker.parsers.Combinators}
 * for general parser constructors and control combinators.
 * <p>
 * Custom low-level parsers can return concrete results from
 * {@link io.github.parseworks.taker.results}.
 */
package io.github.parseworks.taker;
