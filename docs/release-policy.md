# Compatibility and Release Policy

This document describes what Taker treats as public API, how compatibility is
managed, and what should be checked before publishing a release.

## Java Baseline

Taker targets **Java 21+**. The Maven build sets `maven.compiler.release=21`.

Changing the Java baseline is a breaking change and should wait for a major
release unless the project has not yet published a stable release.

## Public API

The public API is the surface exported by the JPMS module:

- `io.github.parseworks.taker`
- `io.github.parseworks.taker.parsers`
- `io.github.parseworks.taker.results`

The following are stable API candidates:

- `Taker`
- `Input`
- `TextInput`
- `Result`
- `Failure`
- `Located`
- `ResultType`
- `CharPredicate`
- `ApplyBuilder`
- public parser libraries under `io.github.parseworks.taker.parsers`
- concrete result records under `io.github.parseworks.taker.results`

The `io.github.parseworks.taker.impl` package is internal. Users should not
depend on it even when classes are visible in source form.

## Pre-1.0 Policy

Before the first stable release, API changes are allowed when they make the
library clearer, safer, or more internally consistent. Prefer making those
changes before publishing rather than carrying compatibility shims too early.

Even before 1.0, breaking changes should be intentional:

- update `docs/api-contract.md`;
- update user-facing docs and examples;
- add or update semantic tests;
- mention the change in release notes or the changelog.

## Version Compatibility

After 1.0, use semantic versioning.

Patch releases should not:

- remove or rename stable public types or methods;
- change documented parser result types;
- change documented input consumption behavior;
- change backtracking behavior of `commit`, `oneOf`, or `parseAll`;
- change documented error positions;
- raise the Java baseline;
- add stdout/stderr logging outside explicit debugging helpers.

Minor releases may:

- add parser helpers;
- add overloads;
- add convenience parser collections;
- improve diagnostics while preserving structured failure semantics;
- add provisional APIs when clearly documented as provisional.

Major releases may:

- remove provisional APIs;
- rename packages, classes, or methods;
- change parser semantics;
- change the Java baseline;
- remove compatibility shims.

## Behavioral Compatibility

The primary compatibility source of truth is `docs/api-contract.md`.

When behavior changes, update or add tests in the relevant area:

- `TakerSemanticContractTest` for core parser semantics.
- `parsers/*Test` for parser-library behavior.
- `examples/*Test` for executable documentation.
- focused regression tests for bugs or edge cases.

Avoid relying on exact full error-message strings unless the test is specifically
about diagnostics. Prefer checking the structured result type, failure position, and
important expected labels.

## Release Checklist

Before cutting a release:

1. Confirm `README.md`, `docs/user-guide.md`, and `docs/api-contract.md` match
   the current API.
2. Confirm `docs/release-policy.md` still reflects the intended compatibility
   promise.
3. Run the unit and semantic test suite:

   ```bash
   mvn test
   ```

4. Compile the JMH profile:

   ```bash
   mvn -Pjmh test-compile
   ```

5. Run a benchmark sanity check for representative parser paths:

   ```bash
   mvn -Pjmh test-compile exec:exec "-Djmh.include=parseNumber|parseCsv|collectCharsLetters"
   ```

6. Run dependency hygiene:

   ```bash
   mvn dependency:analyze
   ```

7. Generate Javadocs and check for broken links or misleading public summaries:

   ```bash
   mvn javadoc:javadoc
   ```

8. Confirm package docs render for:

   - `io.github.parseworks.taker`
   - `io.github.parseworks.taker.parsers`
   - `io.github.parseworks.taker.results`

9. Confirm the MIT license is present and referenced from the README.
10. Review the public API for accidental exposure of internal types.
11. Update version numbers.
12. Write release notes that call out:

    - breaking changes;
    - new parser helpers;
    - behavior changes;
    - performance-sensitive changes;
    - benchmark highlights or notable allocation changes.
13. Record representative benchmark results in `docs/benchmarks.md` when
    performance-sensitive code or benchmark coverage changes.

## Post-Release Sanity

After publishing:

1. Verify the artifact can be consumed from a fresh project.
2. Verify module exports are correct.
3. Verify README installation coordinates match the published artifact.
4. Tag the release in source control.
