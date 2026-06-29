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
