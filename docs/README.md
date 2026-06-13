# Taker Documentation Map

Use this page to choose the right document without reading the same material in
three places.

## Where to Start

- [../README.md](../README.md): project overview, installation, quick examples,
  links to policy and benchmarks.
- [user-guide.md](user-guide.md): first full learning path for day-to-day use.
- [advanced-user-guide.md](advanced-user-guide.md): advanced combinators,
  diagnostics, performance choices, and complex grammar techniques.
- [parser-design-guide.md](parser-design-guide.md): language/parser design
  process and parser architecture patterns.

## Reference and Policy

- [api-contract.md](api-contract.md): source of truth for public API and parser
  semantics. Update this when behavior changes.
- [release-policy.md](release-policy.md): compatibility policy and release
  checklist.
- [benchmarks.md](benchmarks.md): representative JMH records and interpretation.
- [design_considerations.md](design_considerations.md): internal design notes
  explaining why the API and semantics look the way they do.

## Avoiding Duplication

- Put exact parser semantics in `api-contract.md`, then link to it.
- Put executable tutorial examples in tests under
  `src/test/java/io/github/parseworks/taker/examples`, then reference them from
  docs.
- Keep benchmark commands in `README.md` brief; keep recorded numbers in
  `benchmarks.md`.
- Use `advanced-user-guide.md` for parser-combinator technique, and
  `parser-design-guide.md` for grammar and AST design process.
