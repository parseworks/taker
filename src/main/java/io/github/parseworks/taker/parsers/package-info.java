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
 * Ready-made parser factories and reusable grammar building blocks.
 * <p>
 * This package is organized by parser responsibility:
 * <ul>
 *     <li>{@link io.github.parseworks.taker.parsers.Lexical} contains text,
 *     character, token, whitespace, and scanner-level parsers.</li>
 *     <li>{@link io.github.parseworks.taker.parsers.Numeric} contains integer,
 *     long, double, and hexadecimal numeric parsers.</li>
 *     <li>{@link io.github.parseworks.taker.parsers.Combinators} contains
 *     parser constructors and control combinators such as {@code pure},
 *     {@code fail}, {@code oneOf}, {@code eof}, {@code commit}, and
 *     {@code not}.</li>
 *     <li>{@link io.github.parseworks.taker.parsers.TokensParser} provides an
 *     opt-in token facade with a caller-defined ignored-input policy.</li>
 *     <li>{@link io.github.parseworks.taker.parsers.Csv} and
 *     {@link io.github.parseworks.taker.parsers.IsoDates} provide focused
 *     domain parsers that can be used directly or studied as examples.</li>
 * </ul>
 * <p>
 * Prefer scanner primitives such as {@code collectChars}, {@code skipWhile},
 * {@code countWhile}, and {@code takeUntil} when parsing long raw character
 * spans. Prefer repeated {@code Taker} combinators when each item is a
 * structured parser with its own grammar. For case-insensitive text, prefer
 * explicit helpers such as {@code chrIgnoreCase}, {@code stringIgnoreCase}, and
 * {@code oneOfIgnoreCase}.
 */
package io.github.parseworks.taker.parsers;
