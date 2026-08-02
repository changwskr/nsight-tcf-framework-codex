# Naming Principles Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the damaged `docs/네이밍규칙.md` with a comprehensive UTF-8 `docs/네이밍원칙.md` covering Java and PDMP architectural naming.

**Architecture:** Use one canonical document organized from general Java rules to PDMP-specific compatibility rules. Separate mandatory rules, recommendations, prohibited forms, and legacy exceptions so new-code reviews have an unambiguous standard without forcing unrelated source renames.

**Tech Stack:** Markdown, Java 21, Spring Boot 3.5, Spring Security, Spring AOP, MyBatis, Log4j2

## Global Constraints

- Work only inside `pdmp-service/docs`.
- `docs/네이밍원칙.md` is the single authoritative naming document.
- Remove `docs/네이밍규칙.md` after incorporating its valid content.
- Write Korean prose in UTF-8 and preserve exact source identifiers in code formatting.
- Document current architecture and naming; do not rename Java, API, database, or resource identifiers.
- Clearly distinguish mandatory rules, recommendations, prohibited names, and compatibility exceptions.

---

### Task 1: Consolidate naming and architecture conventions

**Files:**
- Create: `docs/네이밍원칙.md`
- Delete: `docs/네이밍규칙.md`

**Interfaces:**
- Consumes: Current source names under `nhnis.fw`, `nhnis.mp`, `rdw.mp.co.a`, application configuration, exception codes, logging fields, and MyBatis mapper identifiers.
- Produces: One review-ready naming standard for developers adding or reviewing `pdmp-service` code.

- [ ] **Step 1: Record the required document structure**

Use these exact top-level sections:

```text
1. 목적과 적용 범위
2. 규칙의 강도
3. 공통 언어와 표기 원칙
4. Java 패키지 명명
5. Java 타입과 파일 명명
6. 메서드 명명
7. 변수·필드·상수 명명
8. 계층별 컴포넌트 명명
9. DTO와 표준 전문 명명
10. TCF 아키텍처 명명
11. API·JSON·HTTP 명명
12. DB·MyBatis 명명
13. Spring 설정과 Bean 명명
14. 예외·결과 코드 명명
15. 로그·MDC·로그 파일 명명
16. 테스트 명명
17. 리소스와 문서 명명
18. 레거시 호환 예외
19. 금지 목록
20. 신규 코드 체크리스트
```

- [ ] **Step 2: Write Java and abbreviation rules**

Document lowercase dot-separated packages, PascalCase types, camelCase members, UPPER_SNAKE_CASE constants, boolean prefixes, collection plurals, annotation/enum/exception suffixes, and file/type identity. Define the project rule that standalone framework component initialisms remain uppercase (`TCFAspect`, `STF`, `ETF`) while established mixed-case family names remain compatible (`TcfTransaction`, `TcfContext`, `JwtAuthenticationFilter`, `DtoIn`).

- [ ] **Step 3: Write layer and architecture rules**

Define responsibilities and suffixes for `Controller`, `Service`, `Dao`, DTOs, `Config`, `Properties`, `Filter`, `Aspect`, `Handler`, `EntryPoint`, `Interceptor`, `Exception`, and `Validator`. State the dependency direction:

```text
Filter/Security → Controller → Service → DAO/MyBatis → DB
                    ↑
            TCFAspect: STF before, ETF after
```

State that controllers handle HTTP adaptation, services own business rules and Spring transactions, DAOs own persistence contracts, and DTOs contain data rather than business behavior.

- [ ] **Step 4: Write PDMP and TCF-specific rules**

Document `nhnis.fw` versus `nhnis.mp`, package `nhnis.mp.co.a`, program identifier `mpcoa9999`, current type patterns such as `mpcoa9999Controller`, `mpcoa9999Service`, `mpcoa9999Dao`, `mpcoa9999DtoIn`, `mpcoa9999DtoOut`, and `mpcoa9999ListResponseDto`. Document `serviceId` as `MP.SalesTip.list`, transaction codes as `MP-INQ-0001`, and processing types as enum values such as `INQUIRY`.

- [ ] **Step 5: Write API, persistence, configuration, error, and logging rules**

Include:

```text
JSON/Java fields: camelCase
HTTP standard headers: canonical hyphenated form such as X-Request-Id
DB tables/columns: UPPER_SNAKE_CASE
MyBatis namespace: DAO fully qualified class name
Mapper statement ID: identical to DAO method name
SQL aliases: Java DTO field name in camelCase
YAML keys: lowercase kebab-case
Spring beans: lowerCamelCase, explicit infrastructure names when multiple beans exist
Business error codes: FW plus four digits
Success/failure result codes: S0000 / E0001
MDC fields: guid, traceId, userId, serviceId, ip, sqlId, ifId, errCode
Log files: project prefix plus purpose, such as pd_framework.log and pd_service.log
```

Explain that the proposed `pd_` log prefix is the naming principle, while any operational collector contract must be checked before changing deployed names.

- [ ] **Step 6: Add examples, exceptions, prohibitions, and checklist**

Include side-by-side good/bad examples. Mark lowercase-leading `mpcoa9999...` types and existing `Tcf...`/`Jwt...` families as compatibility exceptions. Prohibit ambiguous names such as `data`, `obj`, `temp`, `proc`, `doIt`, numbered variables without domain meaning, layer leakage, mismatched Java filename/type names, invented abbreviations, and duplicate naming standards.

- [ ] **Step 7: Remove the superseded document**

Delete:

```text
docs/네이밍규칙.md
```

Do not retain a second canonical naming document.

- [ ] **Step 8: Verify the consolidated document**

Run checks that prove:

```text
docs/네이밍원칙.md exists
docs/네이밍규칙.md does not exist
all 20 top-level sections exist
the UTF-8 replacement character U+FFFD does not occur
no unfinished draft markers occur
current identifiers TCFAspect, STF, ETF, TcfTransaction, mpcoa9999Dao, FW9999, guid, and sqlId are covered
```

Read the rendered Markdown structure and confirm tables and code blocks are balanced.

- [ ] **Step 9: Commit only the consolidated documentation**

```powershell
git add -- 'docs/네이밍원칙.md' 'docs/네이밍규칙.md' 'docs/superpowers/plans/2026-08-01-naming-principles.md'
git commit -m "docs: consolidate naming principles"
```
