# TCF Aspect Rename Design

## Goal

Rename `TcfTransactionAspect` to `TCFAspect` consistently without changing runtime behavior.

## Scope

- Rename `TcfTransactionAspect.java` to `TCFAspect.java`.
- Rename the public class and its constructor to `TCFAspect`.
- Change console log tags from `[TcfTransactionAspect]` to `[TCFAspect]`.
- Update references in `docs/tcf.md` to `TCFAspect`.
- Keep the package, annotations, dependencies, pointcut, and transaction flow unchanged.

## Verification

- Search the source and documentation for remaining `TcfTransactionAspect` references.
- Run the Gradle test task to verify compilation and existing tests.

## Out of Scope

- Changing AOP behavior or the `@TcfTransaction` annotation.
- Refactoring STF, ETF, controller, service, or DAO code.
