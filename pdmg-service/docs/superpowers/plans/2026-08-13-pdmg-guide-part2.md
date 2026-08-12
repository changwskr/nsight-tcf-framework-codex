# PDMG 애플리케이션 아키텍처와 개발 가이드 제2부 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 목차의 제2부 61개 절을 보존하여 PDMG 식별체계와 개발 구조를 설명하는 Markdown 4개 장을 작성한다.

**Architecture:** 실행 코드와 설정을 최우선 근거로 삼고 `MG-NAMING_CONVENTION.md`를 네이밍 기준으로 사용한다. 장별 파일은 원 목차의 절 번호·제목을 그대로 유지하며, AS-IS와 저장소 표준의 차이를 명시한다.

**Tech Stack:** Markdown, Java 21, Spring Boot, Gradle, Spring MVC, Spring AOP, MyBatis

## Global Constraints

- 결과물은 `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/` 아래에 둔다.
- 4~7장을 각각 별도 Markdown 파일로 작성한다.
- 목차의 61개 절 번호와 제목을 그대로 유지한다.
- AS-IS 코드와 저장소 표준을 구분하고 TO-BE 구현을 제안하지 않는다.
- 개인 경로, 계정, Secret과 Token 값을 기록하지 않는다.
- 기존 미커밋 파일을 덮어쓰거나 되돌리지 않는다.

---

### Task 1: 식별체계·네이밍·패키지·호출 근거 확정

**Files:**
- Read: `pdmg-service/docs/00.NSIGHT 애플리케이션 코드 분류표.md`
- Read: `pdmg-service/docs/MG-NAMING_CONVENTION.md`
- Read: `pdmg-service/docs/09.서비스ID.md`
- Read: `pdmg-service/docs/09.서비스ID-1.md`
- Read: `pdmg-service/docs/04.패키지구조.md`
- Read: `pdmg-service/docs/04.패키지구조-1.md`
- Read: `pdmg-service/docs/07.도메인 정의 및 호출방식.md`
- Read: `pdmg-service/docs/07.도메인 정의 및 호출방식-1.md`
- Read: relevant sources/configurations in `pdmg-service`, `pdmg-fw`, `pdmg-ui`, `pdmg-jwt`

**Interfaces:**
- Consumes: approved Part 2 design
- Produces: verified facts shared by Tasks 2~5

- [ ] **Step 1: Search actual identifiers, registrations and package paths**

Run `rg` for `serviceIds()`, Controller mappings, Handler registry, timeout/transaction-control lookup, component scan, mapper scan and Aspect pointcuts.

- [ ] **Step 2: Compare naming reference with actual files**

Record differences as AS-IS exceptions instead of silently normalizing them.

### Task 2: Write Chapter 4

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/04장.애플리케이션 분류와 Service ID.md`

**Interfaces:**
- Consumes: Task 1 verified classification and routing facts
- Produces: sections 4.1~4.15

- [ ] **Step 1: Write every Chapter 4 section in TOC order**
- [ ] **Step 2: Verify sections 4.1~4.15 each occur once**

### Task 3: Write Chapter 5

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/05장.네이밍 규칙.md`

**Interfaces:**
- Consumes: `MG-NAMING_CONVENTION.md` and Task 1 actual-name comparison
- Produces: sections 5.1~5.17

- [ ] **Step 1: Write every Chapter 5 section in TOC order**
- [ ] **Step 2: Verify sections 5.1~5.17 each occur once**

### Task 4: Write Chapter 6

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/06장.패키지와 프로젝트 구조.md`

**Interfaces:**
- Consumes: Task 1 package, scan and resource-path facts
- Produces: sections 6.1~6.16

- [ ] **Step 1: Write every Chapter 6 section in TOC order**
- [ ] **Step 2: Verify sections 6.1~6.16 each occur once**

### Task 5: Write Chapter 7

**Files:**
- Create: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/07장.도메인 정의와 호출 방식.md`

**Interfaces:**
- Consumes: Chapters 4~6 terminology and Task 1 call-path facts
- Produces: sections 7.1~7.13

- [ ] **Step 1: Write every Chapter 7 section in TOC order**
- [ ] **Step 2: Verify sections 7.1~7.13 each occur once**

### Task 6: Validate Part 2

**Files:**
- Verify: `pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/04장*.md` through `07장*.md`

**Interfaces:**
- Consumes: Tasks 2~5
- Produces: verified Part 2 manuscript

- [ ] **Step 1: Compare all numbered headings to the source TOC**
- [ ] **Step 2: Resolve and validate every relative Markdown link**
- [ ] **Step 3: Run `git diff --check` and forbidden-text scan**
- [ ] **Step 4: Review identifiers, paths and AS-IS labels against source evidence**
