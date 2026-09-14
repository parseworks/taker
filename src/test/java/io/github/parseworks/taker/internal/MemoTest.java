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

package io.github.parseworks.taker.internal;

import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.parseworks.taker.parsers.Combinators.pure;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MemoTest {

    @Test
    void doublesCapacityBeforeApplyingTheLimit() {
        Memo memo = new Memo(32);
        List<Taker<String>> takers = new ArrayList<>();
        List<Result<String>> results = new ArrayList<>();

        for (int i = 0; i < 16; i++) {
            Taker<String> taker = pure("value-" + i);
            Result<String> result = taker.parse("");
            takers.add(taker);
            results.add(result);
            memo.put(0, taker, result);
        }

        for (int i = 0; i < takers.size(); i++) {
            assertSame(results.get(i), memo.get(0, takers.get(i)));
        }

        Taker<String> rejected = pure("rejected");
        memo.put(0, rejected, rejected.parse(""));
        assertNull(memo.get(0, rejected));
    }

    @Test
    void stopsCachingNewKeysAtMaximumLoadButStillUpdatesExistingKeys() {
        Memo memo = new Memo(4);
        Taker<String> first = pure("first");
        Taker<String> second = pure("second");
        Taker<String> rejected = pure("rejected");
        Result<String> firstResult = first.parse("");
        Result<String> secondResult = second.parse("");
        Result<String> rejectedResult = rejected.parse("");

        memo.put(0, first, firstResult);
        memo.put(0, second, secondResult);
        memo.put(0, rejected, rejectedResult);

        assertSame(firstResult, memo.get(0, first));
        assertSame(secondResult, memo.get(0, second));
        assertNull(memo.get(0, rejected));

        Result<String> replacement = pure("replacement").parse("");
        memo.put(0, first, replacement);

        assertSame(replacement, memo.get(0, first));
    }
}
