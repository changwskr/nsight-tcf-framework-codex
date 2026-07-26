# TCF-OM 런타임·장애진단 설계 요약

> 원본: `NSIGHT TCF-OM 런타임 장애 진단 화면 설계서.docx` (2026-07-12)  
> 구현 모듈: `tcf-om` (수집·판정) + `tcf-ui` (OM Admin 화면) + `tcf-web` (`/internal/runtime/*`)

## 1. 목적

일반 모니터링 대시보드가 아니라, **장애 발생 시 3단계 이내**에 원인을 좁히는 운영 진단 UI입니다.

```
사용자 → 동시요청 → … (용량산정) 와 별도
장애 시: 인스턴스? → WAR? → ServiceId/SQL/외부연계?
```

## 2. 아키텍처

```text
tcf-ui (/om/admin/runtime-*.html)
    │  OmAdmin.inquiry('runtimeDiagnostics')
    ▼
tcf-om  OM.Runtime.inquiry  (OmRuntimeHandler)
    │  RuntimeStatusCollector → 각 WAR /internal/runtime/*
    │  RuntimeCauseAnalyzer   → 1차 원인·카드·판정
    ▼
tcf-web TcfRuntimeDiagnosticsController
    │  ActiveTransactionRegistry, SlowSqlTracker, …
    ▼
업무 WAR (IC, SV, …) — Tomcat 공유 JVM/Thread
```

| 구분 | API / 거래 | 비고 |
|------|------------|------|
| OM 거래 | `OM.Runtime.inquiry` / `OM-RTM-0001` | 단일 API로 전 화면 데이터 |
| WAR 내부 | `GET /internal/runtime/status` 등 | JWT 필터 제외 경로 |
| 설정 | `nsight.om.runtime-diagnostics.targets` | businessCode·baseUrl 목록 |

## 3. 화면 맵 (RTM ↔ UI)

| 설계 ID | 화면명 | tcf-ui 경로 |
|---------|--------|-------------|
| **워크스페이스** | RTM-010/020/030 통합 탭 | `/om/admin/runtime-workspace.html` |
| RTM-010 | 종합 상태 대시보드 | 워크스페이스 `?tab=rtm010` |
| RTM-010 상세 | WAR·거래 raw 테이블 | `/om/admin/runtime-diagnostics.html` |
| RTM-020 | Tomcat 인스턴스 상세 | 워크스페이스 `?tab=rtm020` |
| RTM-020 (레거시) | Thread/JVM 분석 | `runtime-thread-analysis.html`, `runtime-jvm-analysis.html` |
| RTM-030 | WAR 자원 상세 | 워크스페이스 `?tab=rtm030` |
| RTM-040 | 실행 거래·Slow ServiceId | 워크스페이스 `?tab=rtm040` |
| RTM-050 | 거래 추적 상세 | 워크스페이스 `?tab=rtm050` |
| RTM-060 | Slow SQL·외부연계 | 워크스페이스 `?tab=rtm060` |
| RTM-070 | 장애 진단 및 보고서 | `runtime-cause-analysis.html` |
| RTM-080 | 장애 이력 | `runtime-incident-history.html` (브라우저 localStorage, 서버 Snapshot 후속) |
| RTM-090 | 임계치·수집설정 | `runtime-threshold-policy.html` |
| RTM-100 | 자동 원인 추적 | 워크스페이스 `?tab=rtm100` |

## 4. 자동 원인판정 우선순위

1. Deadlock → 2. DB Pool 고갈 → 3. GC 압박 → 4. CPU 과부하 → 5. Thread 포화  
6. Slow SQL → 7. 외부 연계 지연 → 8. 업무/ServiceId 독점 → 9. 원인 불명

구현: `RuntimeCauseAnalyzer`, `OmRuntimeService.buildCauseAnalysis()`

## 5. 판정 임계치 (요약)

| 코드 | 조건 |
|------|------|
| `DB_POOL_EXHAUSTED` | active=max, idle=0, pending>0 |
| `GC_PRESSURE` | heap≥80%, GC 1분≥3초 |
| `CPU_OVERLOAD` | processCpu≥90%, dbPending=0 |
| `THREAD_SATURATION` | busyRatio≥85% |
| `SLOW_SQL` | 최근 1분 slow SQL≥3 |
| `BUSINESS_DOMINANCE` | 업무 점유≥60% |
| `SERVICE_DOMINANCE` | ServiceId 점유≥40% |

## 6. 구현 클래스

| 영역 | 클래스 |
|------|--------|
| Handler | `OmRuntimeHandler` |
| 수집 | `RuntimeStatusCollector`, `OmRuntimeRemoteClient` |
| 판정 | `RuntimeCauseAnalyzer`, `OmRuntimeService` |
| 설정 | `OmRuntimeDiagnosticsProperties` |
| WAR 런타임 | `tcf-web` `TcfRuntimeMonitor` |

## 7. UI 공통

- 사이드바: **런타임·장애진단** 전용 섹션 (`om-admin.js` `NAV_RUNTIME`)
- 공통 JS: `_shared/om-runtime.js` — `loadDiagnostics()`, 이력 저장
- 메뉴 DB: `OM_GRP_RTM` 하위 15개 (`OmDatabaseMigration.ensureRuntimeDiagnosticsMenus`)

## 8. 실행

```bash
gradle :tcf-om:bootRun :tcf-ui:bootRun
# http://localhost:8099/om/admin/runtime-diagnostics.html
```

Tomcat 통합: `http://localhost:8080/ui/om/admin/runtime-diagnostics.html`

## 9. 변경 이력

| 일자 | 내용 |
|------|------|
| 2026-07-12 | 설계서 기준 메뉴 그룹·RTM-080/090·문서 정리 |
