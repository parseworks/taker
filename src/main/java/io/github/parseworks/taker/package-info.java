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

/**
 * Core parser types for Taker.
 * <p>
 * The main entry point is {@link io.github.parseworks.taker.Taker}, a parser
 * value with fluent instance combinators such as {@code then}, {@code or},
 * {@code map}, {@code oneOrMore}, {@code located}, and {@code expecting}.
 * Supporting types model immutable input cursors, parser results, failures, and
 * source spans. Failed results expose lazy structured diagnostics through
 * {@link io.github.parseworks.taker.Result#diagnostics()} and
 * {@link io.github.parseworks.taker.Result#diagnosticsOptional()}. Inputs represent the original source text; case-insensitive
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
