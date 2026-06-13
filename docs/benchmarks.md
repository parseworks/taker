# Benchmark Records

This document records representative JMH runs for Taker. These numbers are not
release guarantees; they are sanity-check baselines for spotting large
performance or allocation regressions.

Run benchmarks from a clean target directory when comparing normal tests and JMH:

```bash
mvn clean
mvn -Pjmh test-compile exec:exec "-Djmh.prof=gc"
```

After running the JMH profile, run `mvn clean` before a plain `mvn test`.
Generated JMH classes are written under `target/test-classes` and can confuse
Surefire when JMH is no longer on the test classpath.

## 2026-06-13 Baseline

Context:

- Java baseline: 21+
- Mode: JMH throughput
- Count: 15
- Profiler: `gc`
- Units: `ops/ms` for benchmark score, `B/op` for normalized allocation
- Notes: recorded after zero-width `not(...)`, standardized escaped literal
  expectations, and construction-time API guardrails.

| Benchmark | Score ops/ms | Error | Allocation B/op | Notes |
| --- | ---: | ---: | ---: | --- |
| `collectCharsLetters` | 17.618 | 0.071 | 72.389 | Scanner-level character collection; low allocation. |
| `collectExpectedFailures` | 11826.679 | 390.262 | 328.001 | Failure collection path; expected labels are precomputed. |
| `collectStringLetters` | 3.809 | 0.042 | 1007193.812 | Repeated parser string collection; allocation-heavy. |
| `countWhileLetters` | 17.730 | 0.076 | 88.387 | Scanner-level count; low allocation. |
| `foldRepeatedLettersAndDigits` | 2.125 | 0.061 | 1838051.246 | Repeated combinator path; allocation-heavy. |
| `listJoinLetters` | 3.161 | 0.006 | 1176186.184 | List materialization plus join; allocation-heavy. |
| `parseCsv` | 0.355 | 0.005 | 6671131.385 | Full CSV materialization. |
| `parseCsvCountFields` | 1.467 | 0.053 | 1776084.700 | Combinator-based CSV field counting. |
| `parseLocatedNumber` | 10660.301 | 706.496 | 168.001 | `located()` adds about 72 B/op over `parseNumber`. |
| `parseNumber` | 12774.003 | 556.058 | 96.001 | Simple scalar parser; low allocation. |
| `parseNumberAll` | 12657.793 | 51.092 | 96.001 | EOF check adds no normalized allocation. |
| `parseRepeatedLettersAndDigits` | 2.141 | 0.011 | 1849043.212 | Repeated combinator materialization. |
| `scanCsvCountFields` | 6.958 | 0.912 | 0.998 | Hand scanner path; effectively allocation-free. |
| `skipWhileLetters` | 18.761 | 0.022 | 72.365 | Scanner-level skip; low allocation. |
| `trimSpacesChar` | 16181.977 | 426.908 | 240.000 | Space-only trim around a char parser. |
| `trimWhitespaceChar` | 16810.068 | 61.322 | 240.000 | Java-whitespace trim around a char parser. |

Interpretation:

- Scanner primitives (`collectChars`, `countWhile`, `skipWhile`, direct CSV
  scanning) are fast and low allocation.
- Simple scalar parsers such as `parseNumber` are fast with stable low
  allocation.
- `located()` has a small, predictable allocation cost.
- Failure-heavy paths are cheaper than the previous baseline because exact
  character expectation labels are computed once during parser construction.
- Repeated combinator materialization and full CSV parsing are intentionally
  more allocation-heavy; use scanner primitives for raw character runs and
  inspection workloads.
