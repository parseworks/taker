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
