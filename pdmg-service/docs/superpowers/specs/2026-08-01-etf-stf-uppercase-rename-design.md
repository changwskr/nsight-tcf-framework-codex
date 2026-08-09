# ETF and STF Uppercase Rename Design

## Goal

Rename the `Etf` and `Stf` Java types to the uppercase initialisms `ETF` and `STF` consistently without changing runtime behavior.

## Scope

- Rename `etf/Etf.java` to `etf/ETF.java` and its public class and constructor to `ETF`.
- Rename `stf/Stf.java` to `stf/STF.java` and its public class and constructor to `STF`.
- Update imports and injected types in `TCFAspect`, `GlobalExceptionHandler`, and `TcfAuthenticationEntryPoint`.
- Update logger class references and console log tags to `ETF` and `STF`.
- Update all applicable references in `docs/tcf.md`.
- Keep the lowercase package names `etf` and `stf` unchanged.

## Behavior

Spring component discovery, constructor injection, transaction processing, exception handling, and response formatting remain unchanged. This is a type and documentation rename only.

## Verification

- Confirm the old source files no longer exist and the new source files exist.
- Search production source and `docs/tcf.md` for remaining standalone `Etf` or `Stf` references.
- Run `gradlew.bat test` to verify compilation and available tests.

## Out of Scope

- Renaming the `etf` and `stf` packages.
- Changing TCF transaction behavior.
- Modifying `pdmk-ui` or any project outside `pdmk-service`.
