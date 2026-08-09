# TCF Aspect Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename `TcfTransactionAspect` to `TCFAspect` consistently without changing AOP behavior.

**Architecture:** Preserve the existing Spring AOP component, package, annotations, constructor injection, pointcut, and STF/ETF orchestration. Change only the Java type/file identity, diagnostic tag, and documentation references.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring AOP, Gradle

## Global Constraints

- The resulting public class and source file must both be named `TCFAspect`.
- The package remains `nhnis.fw.tcf.aspect`.
- Runtime behavior and transaction flow must remain unchanged.
- All source and `docs/tcf.md` references must use `TCFAspect`.

---

### Task 1: Rename the TCF transaction aspect consistently

**Files:**
- Rename: `src/main/java/nhnis/fw/tcf/aspect/TcfTransactionAspect.java` to `src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java`
- Modify: `src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java`
- Modify: `docs/tcf.md`

**Interfaces:**
- Consumes: Spring discovery through `@Aspect` and `@Component`, plus constructor dependencies `Stf` and `Etf`.
- Produces: `public class TCFAspect` with constructor `TCFAspect(Stf stf, Etf etf)` and the unchanged `aroundTransaction(ProceedingJoinPoint, TcfTransaction)` advice.

- [ ] **Step 1: Verify the current baseline**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL` before the rename.

- [ ] **Step 2: Rename the source file**

Use a filesystem rename so the destination is exactly:

```text
src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java
```

- [ ] **Step 3: Update the Java type and diagnostic tags**

In `TCFAspect.java`, make these exact substitutions:

```java
public class TCFAspect {

    public TCFAspect(Stf stf, Etf etf) {
```

Change both diagnostic prefixes to:

```java
"=========[TCFAspect][aroundTransaction]"
```

Do not change annotations, fields, exception handling, pointcut, or method signatures.

- [ ] **Step 4: Update documentation references**

Replace every `TcfTransactionAspect` occurrence in `docs/tcf.md` with `TCFAspect`.

- [ ] **Step 5: Check reference consistency**

Run:

```powershell
rg -n "TcfTransactionAspect|TCFAspect" src docs
```

Expected: no `TcfTransactionAspect` references; `TCFAspect` appears in the renamed Java file and `docs/tcf.md`. Historical design and plan documents may mention the old name when describing the rename.

- [ ] **Step 6: Run verification**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL` with the renamed Spring component compiled and tests passing.

- [ ] **Step 7: Commit the implementation**

```powershell
git add -- src/main/java/nhnis/fw/tcf/aspect/TCFAspect.java src/main/java/nhnis/fw/tcf/aspect/TcfTransactionAspect.java docs/tcf.md docs/superpowers/plans/2026-08-01-tcf-aspect-rename.md
git commit -m "refactor: rename TCF aspect"
```
