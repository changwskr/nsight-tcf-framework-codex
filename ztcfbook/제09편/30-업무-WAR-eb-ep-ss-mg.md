# 제30장. eb · ep · ss · mg (업무 WAR 4)

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 · 모듈별 레퍼런스 (Quick Start) |
| **장** | 제30장 |
| **파일** | `제09편/30-업무-WAR-eb-ep-ss-mg.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 30.1 모듈 개요

9개 업무 WAR 중 **이벤트·시스템·관리 도메인 4종**입니다. eb/ep는 **이벤트 연계**, ss/mg는 **시스템·관리** 샘플을 제공합니다.

| WAR | 업무 | 포트 | Context | 특징 |
| --- | --- | --- | --- | --- |
| **eb-service** | Event Bridge | 8089 | `/eb` | Outbox → EP 발행 |
| **ep-service** | Event Processor | 8090 | `/ep` | 이벤트 수신·처리 |
| **ss-service** | System Service | 8093 | `/ss` | 시스템 공통 샘플 |
| **mg-service** | Management | 8096 | `/mg` | 관리·설정 샘플 |

공통 패턴: [제29장 §29.1](./29-업무-WAR-ic-pc-ms-sv-pd.md) — Handler 6계층, `/online`, tcf-eai(선택).

---

## 30.2 eb-service — Event Bridge

### 역할

사용자·이벤트 등록 후 **Outbox 패턴**으로 ep-service에 이벤트를 발행합니다.

```text
EB.User.create → EB_USER + EB_EVENT(READY)
@Scheduled 배치 → POST ep-service /ep/online (EP.UserEvent.receive)
성공/실패 → EB_EVENT = SENT / FAIL
```

### Quick Start

```bash
gradle :eb-service:bootRun

curl -X POST http://127.0.0.1:8089/eb/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/eb-sample-inquiry.json
```

UI: http://localhost:8099/eb/index.html

### 주요 Handler

| Handler | serviceId | 거래코드 |
| --- | --- | --- |
| EbSampleHandler | `EB.Sample.inquiry` | EB-INQ-0001 |
| EbUserHandler | `EB.User.inquiry`, `EB.User.create` | EB-USR-* |
| EbEventHandler | `EB.Event.inquiry` | EB-EVT-0001 |
| EbBatchHandler | `EB.Batch.inquiry` | EB-BAT-0001 |

### 주요 테이블

| 테이블 | 역할 |
| --- | --- |
| `EB_USER` | 사용자 |
| `EB_EVENT` | EP 발행 대기 (`READY` / `SENT` / `FAIL`) |

### EB → EP 연동

- **클라이언트:** `EpOnlineClient` — `POST /ep/online`
- **스케줄러:** `EbEventPublishScheduler` (기본 60초)
- **설정:** `nsight.eb.event-publish` → EP URL 8090

> WAR 간 Java 직접 참조 금지. tcf-eai 도입 시 동일 HTTP 패턴 적용 가능.

---

## 30.3 ep-service — Event Processor

### 역할

EB에서 발행한 이벤트를 **수신·처리**합니다.

| Handler | serviceId |
| --- | --- |
| EpSampleHandler | `EP.Sample.inquiry` |
| EpUserEventHandler | `EP.UserEvent.receive` |

### Quick Start

```bash
gradle :ep-service:bootRun
# EB + EP 동시 기동 후 EB.User.create → Scheduler → EP.UserEvent.receive 확인
```

End-to-End: EB create → `EB_EVENT.READY` → Scheduler → EP receive → `SENT`

아키텍처: [zarchitecture/14-이벤트-연계-아키텍처.md](../../zarchitecture/14-이벤트-연계-아키텍처.md)

---

## 30.4 ss-service — System Service

| 항목 | 값 |
| --- | --- |
| 포트 | 8093 |
| Context | `/ss` |
| 용도 | 시스템 공통·인프라 연동 샘플 WAR |

```bash
gradle :ss-service:bootRun
curl -X POST http://127.0.0.1:8093/ss/online ...
```

신규 거래 prefix: **`SS.`** — ic/sv와 동일 Handler 패턴.

---

## 30.5 mg-service — Management

| 항목 | 값 |
| --- | --- |
| 포트 | 8096 |
| Context | `/mg` |
| 용도 | 관리·설정·내부 운영 샘플 WAR |

```bash
gradle :mg-service:bootRun
```

신규 거래 prefix: **`MG.`**

OM(`tcf-om`)과 역할 구분: **OM = 플랫폼 운영 메타**, **mg = 업무 관리 도메인**(설계 확장 시).

---

## 30.6 이벤트 WAR vs tcf-eai

| 패턴 | 적합 |
| --- | --- |
| **tcf-eai** | 동기 요청-응답 (SV→IC 조회) |
| **EB/EP Outbox** | 비동기 이벤트, 재시도, SENT/FAIL 추적 |
| **Batch Scheduler** | 주기적 대량 처리 |

이벤트 연계 상세: [제5편 §17.4](../제05편/17-Batch-Scheduler-이벤트.md)

---

## 장 요약

**eb/ep**는 Outbox 기반 이벤트 발행·수신의 참조 구현이고, **ss/mg**는 시스템·관리 도메인 확장용 샘플 WAR입니다. 4 WAR 모두 `POST /{code}/online` + Handler 6계층 패턴을 따르며, EB→EP는 Scheduler + HTTP로 End-to-End 검증합니다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제29장 ic · pc · ms · sv · pd](./29-업무-WAR-ic-pc-ms-sv-pd.md) |
| → 다음 | [제31장 공식 설계안 매핑](../제10편/31-공식-설계안-매핑.md) |

---

## 출처 색인

| 절 | 출처 |
| --- | --- |
| 30.1~30.2 | [zguide/eb-service-개발가이드.md](../../zguide/eb-service-개발가이드.md) |
| 30.3 | [zguide/ep-service-개발가이드.md](../../zguide/ep-service-개발가이드.md) |
| 30.4~30.5 | [zguide/ss-service-개발가이드.md](../../zguide/ss-service-개발가이드.md), [mg-service-개발가이드.md](../../zguide/mg-service-개발가이드.md) |
| 30.6 | [zarchitecture/14-이벤트-연계-아키텍처.md](../../zarchitecture/14-이벤트-연계-아키텍처.md) |
