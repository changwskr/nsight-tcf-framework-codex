---
title: NSIGHT-TCF-FRAMEWORK 아키텍처 정의서
version: 0.1
baseline_date: 2026-07-26
source_branch: develop
status: conditional-baseline
owner: AA
approver: AA
gate: 조건부 통과 (A16)
open_issue_count: 7
---

# 1. 도입 전 안내말

본 문서는 A00~A16 대화 확정과 Branch `develop` 소스를 근거로 한 **온라인 SI 아키텍처 Baseline**이다.  
Architecture Gate는 **조건부 통과**이다. OM·NFR 수치·Git·시범 인증 Gap·배포문서 Drift는 감추지 않고 Explicit로 둔다.  
**AA 승인 전 운영 반영 금지.**

부속: `01-ADR.md` … `11-Executive-Summary.md` · 단계 원문: `A00`~`A16`

# 2. 문서 개요

## 2.1 목적

공통 개발·운영 Baseline 문서화 + AI·CRUD 방법론과 정합되는 실행 기준. (A01)

## 2.2 적용범위

| 구분 | 내용 |
| --- | --- |
| 적용 | 온라인 `/online` · ServiceId · Handler · OM · 업무 WAR 계층 · **현행** Gateway(JWT) |
| 제외 | 배치·파일 · Gateway **신규** 라우팅 |
| 시범 앵커 | av-service (8101), ln-service (8103) |

## 2.3 대상 독자

업무 개발자 · AA · AI·방법론 설계자 · (참고) 운영

## 2.4 선행조건

`develop` 소스 · A-MASTER · AA · 방법론 `00-NSIGHT-TCF-AI-LLM-SI-개발방법론.md`

## 2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| SoT | 사실 기준 — 현재 Branch 소스 |
| Baseline | AA 승인 설계 |
| ServiceId | `{업무코드}.{Domain}.{action}` |
| Gap Explicit | 미확정·불일치를 숨기지 않고 표기 |

# 3. 본문

## 3.1 문제 정의 및 설계 배경

#1 문제: 문서·소스·OM 불일치. As-Is는 런타임 경로 기준으로 기술. Gaps는 Explicit. (A02)

## 3.2 현행 구조와 문제점

- 공통 `OnlineTransactionController`가 실진입점 `[실제 소스 확인]`
- 논리 6단계 ≠ 디스크 6패키지 → `entry` / `application` / `persistence`
- bootRun `context-path=/` vs Tomcat `/{biz}` As-Is/To-Be 분리 (A08)
- zarchitecture/ztomcat과 AV·LN 등록 Gap 가능

## 3.3 요구사항과 제약조건

NFR Top3 우선순위: **정합성 > 운영 > 보안**. TPS/p95/Pool은 **빈 란(미확정)**. (A03·A11)

## 3.4 설계 원칙

1. SoT = Branch 소스  
2. 온라인 진입 = 공통 `/online`  
3. 계층·WAR 경계 준수  
충돌 시 위 NFR 순서. 예외 = AA + ADR. (A04)

## 3.5 대안 비교 및 의사결정

**ADR-001 Accepted:** 공통 `OnlineTransactionController` 채택 · 업무 WAR 거래용 REST Controller 폐기.  
상세: `01-ADR.md` · Open: ADR-002 WAR분할 · ADR-003 JWT방어.

## 3.6 목표 아키텍처

### 3.6.1 논리 아키텍처

| 구분 | 내용 |
| --- | --- |
| 디스크 | `entry` / `application` / `persistence` |
| 논리 | Handler → Facade → Service → Rule → Dao → Mapper |
| 예외 패키지 | `client` · `config` (표로 명시) |

경계: 채널(tcf-ui) · Gateway(현행 JWT) · 플랫폼(tcf-*) · 업무 WAR · 진입 ADR-001. (A06)

### 3.6.2 물리 아키텍처

업무 WAR 단위 배포 · 포트 SoT = `tcf-ui` `BusinessModuleDefinitions` · AV=8101 · LN=8103. (A08)

### 3.6.3 런타임 아키텍처

```text
POST /online | /{businessCode}/online
  → OnlineTransactionController
  → TCF → STF
  → OnlineTransactionTimeoutExecutor
  → TransactionDispatcher
  → Handler → Facade → Service → Rule/Dao → Mapper
  → ETF
```

실패: businessFail / systemError / TimeoutExecutor. (A07) · 상세 `04-…`

### 3.6.4 배포 아키텍처

Baseline: **bootRun 우선 + WAR(Tomcat) 동등**. Context As-Is `/` · To-Be `/{biz}` 가능. Gradle include + `{biz}.war`. (A08)

## 3.7 표준 형식

- API: `POST /online`, `POST /{businessCode}/online`
- ServiceId: `{Biz}.{Domain}.{action}`
- 패키지: entry / application / persistence
- Timeout 클래스: `OnlineTransactionTimeoutExecutor` (구칭 TimeoutExecutor 금지)

## 3.8 구성요소 및 속성

| 구성요소 | 역할 |
| --- | --- |
| OnlineTransactionController | 공통 진입 |
| TCF/STF/ETF | 공통 전후처리 |
| OnlineTransactionTimeoutExecutor | Timeout |
| TransactionDispatcher | ServiceId → Handler |
| Facade | TX 경계 |
| Mapper | SQL SoT |

## 3.9 책임 경계와 RACI

상세: `03-RACI.md`. 승인자 AA.

## 3.10 정상 처리 흐름

§3.6.3 동일. 시범 ServiceId: `AV.Sample.inquiry` · `LN.CustomerContact.selectList/Detail` (CRUD 결과 참조).

## 3.11 오류·Timeout·장애 흐름

`04-정상-오류-Timeout-장애-흐름.md`

## 3.12 정상 예시

- 공통 Controller + Handler 계층 + Facade TX + WAR 내 Mapper
- Gateway JWT (현행) + Header 단독 신뢰 금지 (목표)

## 3.13 금지 예시

- 업무 WAR에 거래용 REST Controller 추가
- 타 WAR DAO/테이블 직접 참조
- Mapper 직호출로 Facade/TX 우회
- 미실행 시험을 성공으로 기록
- 시범 auth-off를 운영 Baseline으로 승격

## 3.14 연계 규칙

외부 = `client` 계층. 타 업무 = API·연계. (A09)

## 3.15 데이터 및 상태관리

WAR 경계 내 테이블만 직접 접근. TX = Facade. 시범 H2 ≠ 운영 DB SoT. (A09)

## 3.16 성능·용량·확장성

구조: JVM 공유 / WAR별 Pool. 수치·DR 사이트 = 미확정 란. `05-성능-용량-DR.md`

## 3.17 보안·개인정보·감사

목표 JWT+방어. 시범 validation-off = Gap. `06-보안-개인정보-감사.md`

## 3.18 운영·모니터링·장애 대응

Health · 거래/감사 로그 · OM · 추적키 guid/traceId. `07-운영-Runbook.md` · OM Gap Explicit.

## 3.19 자동검증 및 품질 Gate

자동: compile/unit/구조. 수동: POST/배포/OM. Git 미확정. Gate=A16 조건부 통과. `08-자동검증-Gate.md`

## 3.20 테스트 시나리오

축: 화면→ServiceId→Handler→SQL→로그 · 요구→ADR→시험→운영. 미실행≠성공. `02-추적성-매트릭스.md`

## 3.21 체크리스트

- [ ] ADR-001 준수 (공통 /online)
- [ ] 업무 거래 Controller 없음
- [ ] 계층·WAR 경계
- [ ] OM Catalog/Timeout 등록 (Gap 해소)
- [ ] NFR 수치 기입 (Gap 해소)
- [ ] AA 승인 후 운영 반영

## 3.22 변경·호환성·폐기 관리

`09-변경-호환-폐기.md`

# 4. 시사점

## 4.1 핵심 아키텍처 판단

온라인 진입·계층·런타임·금지는 Baseline으로 고정. 조건부 영역은 운영 전 AA 해소.

## 4.2 주요 위험

문서·소스·OM 불일치 방치 → 운영 장애. (A02·`10-Open-Issue…`)

## 4.3 우선 보완 과제

1. OM 실등록  
2. NFR 수치  
3. 시범 auth Gap 운영 분리 확인  
4. ztomcat/zarchitecture Drift (A18)  
5. ADR-002/003

## 4.4 중장기 발전 방향

A18 Drift 반복 · 방법론·CRUD 시리즈와 동일 SoT·Gate 언어 유지.

# 5. 마무리말

본 정의서는 **조건부 Baseline**이다. A18에서 As-Built Drift를 확인하고, AA 승인 후에만 운영에 반영한다.
