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

## 2026-06-12 Baseline

Context:

- Java baseline: 21+
- Mode: JMH throughput
- Count: 15
- Profiler: `gc`
- Units: `ops/ms` for benchmark score, `B/op` for normalized allocation
- Notes: recorded after replacing transformed input wrappers with explicit
  case-insensitive parser and predicate helpers.

| Benchmark | Score ops/ms | Error | Allocation B/op | Notes |
| --- | ---: | ---: | ---: | --- |
| `collectCharsLetters` | 15.707 | 0.919 | 72.438 | Scanner-level character collection; low allocation. |
| `collectExpectedFailures` | 6622.531 | 644.790 | 472.001 | Failure collection path. |
| `collectStringLetters` | 3.227 | 0.143 | 1007194.137 | Repeated parser string collection; allocation-heavy. |
| `countWhileLetters` | 16.117 | 0.633 | 88.425 | Scanner-level count; low allocation. |
| `foldRepeatedLettersAndDigits` | 1.701 | 0.175 | 1838052.094 | Repeated combinator path; allocation-heavy. |
| `listJoinLetters` | 2.656 | 0.063 | 1176186.595 | List materialization plus join; allocation-heavy. |
| `parseCsv` | 0.258 | 0.017 | 8255186.810 | Full CSV materialization. |
| `parseCsvCountFields` | 1.046 | 0.140 | 1872134.669 | Combinator-based CSV field counting. |
| `parseLocatedNumber` | 8796.938 | 314.297 | 168.001 | `located()` adds about 72 B/op over `parseNumber`. |
| `parseNumber` | 10802.377 | 679.359 | 96.001 | Simple scalar parser; low allocation. |
| `parseNumberAll` | 10057.040 | 1236.146 | 96.001 | EOF check adds no normalized allocation. |
| `parseRepeatedLettersAndDigits` | 1.735 | 0.170 | 1849044.005 | Repeated combinator materialization. |
| `scanCsvCountFields` | 6.376 | 0.261 | 1.077 | Hand scanner path; effectively allocation-free. |
| `skipWhileLetters` | 16.807 | 1.281 | 72.411 | Scanner-level skip; low allocation. |
| `trimSpacesChar` | 14481.247 | 1331.201 | 240.000 | Space-only trim around a char parser. |
| `trimWhitespaceChar` | 14226.856 | 709.974 | 240.000 | Java-whitespace trim around a char parser. |

Interpretation:

- Scanner primitives (`collectChars`, `countWhile`, `skipWhile`, direct CSV
  scanning) are fast and low allocation.
- Simple scalar parsers such as `parseNumber` are fast with stable low
  allocation.
- `located()` has a small, predictable allocation cost.
- Repeated combinator materialization and full CSV parsing are intentionally
  more allocation-heavy; use scanner primitives for raw character runs and
  inspection workloads.
