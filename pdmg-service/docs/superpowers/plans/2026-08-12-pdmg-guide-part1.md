# PDMG 애플리케이션 아키텍처와 개발 가이드 제1부 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** PDMG의 AS-IS 시스템 구조, 전체 온라인 거래 흐름, 레이어드 아키텍처를 개발자·아키텍트용 상세 Markdown 3개 장으로 작성한다.

**Architecture:** 목차가 지정한 절을 유지하되 기본 문서, 재분석 문서, 다이어그램 문서를 통합한다. 실제 실행 코드와 설정을 최우선 근거로 사용하고 각 장을 개념, AS-IS 구현, 흐름, 설정·소스, 주의사항, 체크리스트 순서로 구성한다.

**Tech Stack:** Markdown, Java 21, Spring Boot, Gradle, Spring MVC, Spring AOP, Spring Transaction, MyBatis, JWT

## Global Constraints

- 결과물은 `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/` 아래에 둔다.
- 제1부의 1장, 2장, 3장을 각각 별도 Markdown 파일로 작성한다.
- 현재 저장소에서 확인되는 AS-IS만 기술하고 TO-BE 제안은 넣지 않는다.
- 코드와 문서가 다르면 실행 코드와 설정을 기준으로 한다.
- 텍스트 다이어그램과 표를 우선 사용한다.
- 개인 경로, 계정, Secret과 Token 값을 기록하지 않는다.
- 기존 미커밋 변경을 수정하거나 되돌리지 않는다.

---

### Task 1: 제1부 근거 자료 확정

**Files:**
- Read: `pdmg-service/docs/zz.PDMG 애플리케이션 아키텍처와 개발 가이드 목차.md`
- Read: `pdmg-service/docs/00.Big Picture Image.md`
- Read: `pdmg-service/docs/00.BigPicture Tx 처리-1.md`
- Read: `pdmg-service/docs/00.BigPicture Tx 흐름.md`
- Read: `pdmg-service/docs/02.어플리케이션 컴포넌트 구조.md`
- Read: `pdmg-service/docs/02.어플리케이션 컴포넌트 구조-1.md`
- Read: `pdmg-service/docs/03.어플리케이션 레이어드 아키텍처.md`
- Read: `pdmg-service/docs/03.어플리케이션 레이어드 아키텍처-1.md`
- Read: `pdmg-service/docs/05.전체 빅픽처 흐름.md`
- Read: `pdmg-service/docs/05.전체 빅픽처 흐름-1.md`
- Read: `pdmg-service/docs/46.전체 아키텍처 구조.md`
- Read: `pdmg-service/docs/50.기술스택 다이어그램.md`
- Read: `pdmg-service/docs/75.PDMG 컴포넌트 다이어그램.md`
- Read: `pdmg-service`, `pdmg-fw`, `pdmg-ui`, `pdmg-jwt` source/config/build files found by `rg`

**Interfaces:**
- Consumes: approved design `pdmg-service/docs/superpowers/specs/2026-08-12-pdmg-guide-part1-design.md`
- Produces: verified fact set for Tasks 2~4

- [ ] **Step 1: Locate source, configuration, test and document evidence**

Run:

```powershell
rg -n "OnlineTransactionController|DefaultFilter|ServicePrevention|TransactionDispatcher|TransactionHandler|TcfFacade|ServiceContext|TransactionContext|ConditionalOnProperty|server.port" pdmg-service pdmg-fw pdmg-ui pdmg-jwt
```

Expected: representative implementations and configuration keys are identified for all four modules.

- [ ] **Step 2: Record AS-IS discrepancies while drafting**

Compare code to documents and include only verified current behavior. Where multiple paths exist, label them TCF ON, TCF OFF, direct call, or relay call.

- [ ] **Step 3: Verify the target directory state**

Run:

```powershell
Get-ChildItem -LiteralPath "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드" -ErrorAction SilentlyContinue
```

Expected: existing user files, if any, are identified before creation.

### Task 2: 1장 PDMG 시스템 개요 집필

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/01장.PDMG 시스템 개요.md`

**Interfaces:**
- Consumes: Task 1 verified module, build, port and terminology facts
- Produces: system-level context referenced by Chapters 2 and 3

- [ ] **Step 1: Create the chapter with all TOC sections**

Include sections 1.1 through 1.9 exactly, with an opening learning-goal block, overall module diagram, module responsibility table, dependency diagram, technology table, port/call table, TCF ON/OFF comparison, glossary, checklist and chapter summary.

- [ ] **Step 2: Verify chapter completeness**

Run:

```powershell
rg -n "^## 1\.[1-9]" "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/01장.PDMG 시스템 개요.md"
```

Expected: sections 1.1 through 1.9 each appear once.

### Task 3: 2장 전체 온라인 거래 빅픽처 집필

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/02장.전체 온라인 거래 빅픽처.md`

**Interfaces:**
- Consumes: Chapter 1 terminology and Task 1 verified runtime flow
- Produces: end-to-end flow used by Chapter 3 responsibility analysis

- [ ] **Step 1: Create the chapter with all TOC sections**

Include sections 2.1 through 2.12 exactly, with a full request diagram, phase boundary table, normal/failure sequence diagrams, online request versus DB transaction comparison, request/worker Thread distinction, checklist and chapter summary.

- [ ] **Step 2: Verify chapter completeness**

Run:

```powershell
rg -n "^## 2\.(?:[1-9]|1[0-2])" "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/02장.전체 온라인 거래 빅픽처.md"
```

Expected: sections 2.1 through 2.12 each appear once.

### Task 4: 3장 레이어드 아키텍처와 컴포넌트 집필

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/03장.레이어드 아키텍처와 컴포넌트.md`

**Interfaces:**
- Consumes: Chapters 1~2 terminology and verified package/class map
- Produces: final Part 1 responsibility and dependency reference

- [ ] **Step 1: Create the chapter with all TOC sections**

Include sections 3.1 through 3.14 exactly, with layered diagram, component responsibility matrix, allowed/forbidden dependency diagram, package/class examples, clean-architecture mapping, AS-IS deviation table, checklist and part summary.

- [ ] **Step 2: Verify chapter completeness**

Run:

```powershell
rg -n "^## 3\.(?:[1-9]|1[0-4])" "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/03장.레이어드 아키텍처와 컴포넌트.md"
```

Expected: sections 3.1 through 3.14 each appear once.

### Task 5: 제1부 통합 검증

**Files:**
- Verify: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/*.md`

**Interfaces:**
- Consumes: Chapters 1~3
- Produces: verified Part 1 manuscript

- [ ] **Step 1: Check Markdown whitespace and Git diff**

Run:

```powershell
git diff --check -- "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드"
git diff --stat -- "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드"
```

Expected: no whitespace errors and exactly three new chapter files.

- [ ] **Step 2: Check forbidden placeholders and sensitive local data**

Run:

```powershell
rg -n "TBD|TODO|C:\\Users|pdmk-local-dev-secret|BEGIN (RSA )?PRIVATE KEY" "pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드"
```

Expected: no matches.

- [ ] **Step 3: Validate local Markdown links**

Extract relative Markdown links from the three chapters, resolve them from each file's directory and confirm that every local target exists.

- [ ] **Step 4: Review scope and terminology consistency**

Confirm that all statements are AS-IS, TCF ON/OFF are separated, online lifecycle and DB transaction are distinguished, and class/config names match Task 1 evidence.
