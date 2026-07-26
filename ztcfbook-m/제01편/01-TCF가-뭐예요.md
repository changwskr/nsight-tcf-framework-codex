# 제1장. TCF가 뭐예요?

| 항목 | 내용 |
| --- | --- |
| **편** | 제1편 |
| **장** | 제1장 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제1장](../ztcfbook/제01편/01-NSIGHT-TCF란-무엇인가.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  Client[채널/UI] --> GW[tcf-gateway]
  GW --> WAR[업무 WAR]
  WAR --> TCF[TCF.process]
  TCF --> Handler[TransactionHandler]
  Handler --> DB[(RDW/H2)]
```

---

## 1.1 REST와 TCF, 뭐가 다른가요?

Spring Boot tutorial에서는 보통 이렇게 배웁니다.

```text
GET  /customers/1        → 고객 1명 조회
POST /customers          → 고객 등록
GET  /campaigns          → 캠페인 목록
```

**URL = 기능** 입니다. NSIGHT **업무 WAR** 안에서는 이 패턴을 쓰지 **않습니다.**

대신 이렇게 합니다.

```text
모든 기능 → POST /sv/online  (주소는 하나)
어떤 기능? → JSON header 안의 serviceId (예: SV.Customer.selectSummary)
```

**비유:** REST는 “메뉴마다 다른 창구”, TCF는 “**안내 데스크 하나** + **접수 번호(serviceId)** ”입니다.

| | 일반 REST | NSIGHT TCF |
| --- | --- | --- |
| 주소 | 기능마다 다름 | `POST /{업무코드}/online` |
| 기능 구분 | URL | **serviceId** |
| 로그인·검사 | 개발자가 각 API에 구현 | **STF**가 공통으로 처리 |

---

## 1.2 TCF는 정확히 뭔가요?

**TCF(Transaction Control Framework)** 는 “온라인 거래를 **한 가지 규칙**으로 받고, 실행하고, 응답하고, 기록하는 **공통 엔진**”입니다.

- 여러분이 만드는 것: **업무 로직** (고객 조회, 캠페인 저장 등)
- TCF가 하는 것: **Header 검사, 권한, Timeout, 로그, 오류 응답 형식**

한 문장으로:

> **업무 코드는 개발자가 짜고, “거래를 어떻게 실행·기록할지”는 TCF가 맡는다.**

---

## 1.3 개발자가 지켜야 할 것 (10가지 → 3가지만)

원본 책에는 원칙이 10개입니다. 처음에는 **아래 3가지만** 기억하세요.

1. **JSON 표준 전문**으로 요청·응답한다 (`header` + `body`).
2. **serviceId**로 Handler를 찾는다 (URL로 기능을 나누지 않는다).
3. **Handler는 Facade만** 부른다 — Handler 안에 SQL·긴 if문을 넣지 않는다.

나머지(등록된 거래만 실행, GUID 로그, Timeout 등)는 실습하면서 익히면 됩니다.

---

## 1.4 ⚠️ 초보자가 자주 하는 실수

| 하면 안 되는 것 | 왜? |
| --- | --- |
| `@RestController`로 `/api/customer` 새로 만들기 | TCF 파이프라인을 **건너뜁니다** |
| Handler에서 DB 직접 조회 | **DAO·Mapper** 계층이 있습니다 |
| serviceId 없이 내부 메서드만 호출 | 거래로그·OM에 **안 남습니다** |
| catch에서 아무 JSON이나 반환 | **ETF**가 표준 응답을 만듭니다 |

---

## 1.5 누가 뭘 하나요? (그림)

```text
[여러분]  Handler → Facade → Service → Rule → DAO → Mapper
[TCF]     STF(검사) → Dispatcher(연결) → ETF(응답·로그)
```

STF·Dispatcher·ETF 이름은 3장에서 다시 설명합니다. 지금은 **“내 코드는 Handler 아래”** 만 알면 됩니다.

---

## 요약

- TCF = **주소 하나 + serviceId** 로 거래를 통일하는 프레임워크.
- REST처럼 **Controller URL**을 늘리지 마세요.
- **Handler → Facade → …** 아래에 업무를 작성하세요.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [서문](../서문/00-서문.md) |
| → 다음 | [2장 시스템 그림 한 장](./02-시스템-그림-한-장.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/제01편/01-NSIGHT-TCF란-무엇인가.md](../ztcfbook/제01편/01-NSIGHT-TCF란-무엇인가.md)
- [ztcfbook/제01편/03-TCF-처리-엔진.md](../ztcfbook/제01편/03-TCF-처리-엔진.md) — STF·ETF 상세
