# ETF and STF Uppercase Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the `Etf` and `Stf` Java types to `ETF` and `STF` consistently without changing TCF behavior.

**Architecture:** Preserve the lowercase packages and all Spring component behavior. Rename the source files and public types atomically, then update every production consumer, logger tag, and primary TCF document reference.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring AOP, Gradle

## Global Constraints

- Work only inside `pdmp-service`.
- Rename `Etf` to `ETF` and `Stf` to `STF` everywhere in production source and `docs/tcf.md`.
- Keep package names `nhnis.fw.tcf.etf` and `nhnis.fw.tcf.stf` unchanged.
- Do not change transaction, exception, authentication, or response behavior.

---

### Task 1: Rename ETF and STF types and consumers

**Files:**
- Rename: `src/main/java/nhnis/fw/tcf/etf/Etf.java` to `src/main/java/nhnis/fw/tcf/etf/ETF.java`
- Rename: `src/main/java/nhnis/fw/tcf/stf/Stf.java` to `src/main/java/nhnis/fw/tcf/stf/STF.java`
- Modify: `src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java`
- Modify: `src/main/java/nhnis/fw/exception/GlobalExceptionHandler.java`
- Modify: `src/main/java/nhnis/fw/tcf/web/TcfAuthenticationEntryPoint.java`
- Modify: `docs/tcf.md`

**Interfaces:**
- Consumes: Existing `StandardHeaderValidator`, `TcfProperties`, `ExceptionCodeProperties`, and Spring constructor injection.
- Produces: Components `ETF` and `STF` with unchanged public methods; all consumers import and inject the uppercase types.

- [ ] **Step 1: Verify the baseline**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Rename and update the STF component**

Rename the file to `STF.java` and use these declarations:

```java
public class STF {
    private static final Logger log = LoggerFactory.getLogger(STF.class);

    public STF(StandardHeaderValidator headerValidator) {
```

Change its console diagnostic tags from `[Stf]` to `[STF]`. Do not change `preProcess` or its supporting logic.

- [ ] **Step 3: Rename and update the ETF component**

Rename the file to `ETF.java` and use these declarations:

```java
public class ETF {
    private static final Logger log = LoggerFactory.getLogger(ETF.class);

    public ETF(TcfProperties properties, ExceptionCodeProperties exceptionCodes) {
```

Change its console diagnostic tags from `[Etf]` to `[ETF]`. Do not change success, business failure, system failure, or response-building logic.

- [ ] **Step 4: Update production consumers**

Make these exact type-level substitutions in `TCFAspect`, `GlobalExceptionHandler`, and `TcfAuthenticationEntryPoint`:

```java
import nhnis.fw.tcf.etf.ETF;
import nhnis.fw.tcf.stf.STF;
```

Use `ETF` and `STF` for fields and constructor parameter types. Preserve lowercase variable names `etf` and `stf`.

- [ ] **Step 5: Update primary documentation**

Replace standalone `Etf` and `Stf` type/file references in `docs/tcf.md` with `ETF` and `STF` while leaving lowercase package concepts unchanged.

- [ ] **Step 6: Verify names and compilation**

Run:

```powershell
rg -n "\bEtf\b|\bStf\b" src docs/tcf.md
.\gradlew.bat test
```

Expected: the search returns no old production or primary-document type references, and Gradle reports `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit only the scoped implementation files**

```powershell
git add -- src/main/java/nhnis/fw/tcf/etf/ETF.java src/main/java/nhnis/fw/tcf/stf/STF.java src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java src/main/java/nhnis/fw/exception/GlobalExceptionHandler.java src/main/java/nhnis/fw/tcf/web/TcfAuthenticationEntryPoint.java docs/tcf.md docs/superpowers/plans/2026-08-01-etf-stf-uppercase-rename.md
git commit -m "refactor: rename ETF and STF components"
```
