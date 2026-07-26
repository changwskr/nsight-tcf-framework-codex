# 제32장. Gap·보완·향후 과제

| 항목 | 내용 |
| --- | --- |
| **편** | 제10편 · 설계 근거와 로드맵 |
| **장** | 제32장 |
| **파일** | `제10편/32-Gap-보완-향후-과제.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 32.1 설계서 vs 코드 Gap

### 핵심 결론

| | 내용 |
| --- | --- |
| **현재** | TCF 골격·표준전문·Dispatcher·OM·Gateway·JWT·Cache **보유** |
| **운영** | 보안·기준정보 운영·CI/CD·17 WAR·품질 Gate **보강 필요** |

NSIGHT TCF는 **프레임워크 골격은 설계와 정합**하나, **운영 가능 수준**까지는 Gap이 남아 있습니다. "코드가 없다"기보다 **운영 프로세스·확장·품질 Gate**가 과제입니다.

### 현재 강점

- Gradle 멀티 모듈 (tcf-* + 9 WAR)
- STF / Dispatcher / ETF 파이프라인
- **도메인 Handler** (tcf-om 24개, serviceIds() 패턴)
- **tcf-eai** IC↔SV 연동 데모
- tcf-gateway, jwt, batch, cache, ztomcat
- docs / zdoc / **zman** / znsight-man / zguide / **ztcfbook**

### Gap 축소 (최근)

| 항목 | 이전 | 현재 |
| --- | --- | --- |
| Handler | serviceId당 1개 | **도메인당 1**, serviceIds() |
| OM Handler | 83개 | **24개** |
| EAI | 설계만 | **tcf-eai** 구현 |
| 문서 | Word docx | **zman + docs + ztcfbook** |

### 목표 운영 규모 (설계 기준)

| 지표 | 목표 |
| --- | --- |
| 사용자 | 36K |
| 동시 세션 | 43K |
| TPS | 720 |
| P95 | 3초 |
| 가용성 | 99.99% |
| WAR | **17** (현재 9) |

### 영역별 Gap

| 영역 | Gap | 우선순위 |
| --- | --- | --- |
| 업무 WAR | 9 → 17 확장 | P3 |
| TCF Core | Idempotency, 마스킹, 감사정책 운영화 | P1~P2 |
| OM | UI·프로세스 **실운영 검증** | P1 |
| Gateway | Route DB 운영·동기화 | P2 |
| JWT/SSO | IdP 연동, Token 정책 | P1 |
| DB/MyBatis | RDW/ADW 분리, SQL 표준 | P2 |
| Spring | prd Profile, Secret Vault | P1 |
| CI/CD | SonarQube, Nexus, Pipeline 완성 | P1 |

---

## 32.2 보완 우선순위

### 우선순위 정의

| 등급 | 의미 |
| --- | --- |
| **P1** | 운영 반영 **전 필수** |
| **P2** | 운영 **안정성** |
| **P3** | **확장·생산성** |

### P1 — 운영 반영 전 필수

| ID | 과제 | 완료 기준 |
| --- | --- | --- |
| P1-1 | 보안·인가 | JWT+세션, Header 위변조 차단, Internal Call 통제 |
| P1-2 | Session JDBC | 43K session, DR, 다중 WAR 공유 |
| P1-3 | Catalog·TC | OM Allow-List **실운영** 등록·검증 |
| P1-4 | TX·감사로그 | PROCESSING→종료, UNKNOWN 처리 |
| P1-5 | CI/CD | Build, Test, Deploy, Health, **Rollback** |

### P2 — 운영 안정성

- Gateway Route 운영 DB·Evict
- Cache Evict OM 연동
- Batch Lock/이력·재처리
- File 업·다운로드 감사
- Hikari/MyBatis SQL 표준·Slow Query
- 운영 Dashboard·알람

### P3 — 확장·생산성

- **17 WAR** 확장 (Handler 템플릿)
- tcf-eai 연동 범위 확대
- SonarQube Quality Gate 정착
- docs ↔ OM 화면 자동 정합

**이미 완료:** 도메인 Handler, serviceIds(), tcf-eai 데모, zman/docs/ztcfbook

### 우선순위 매트릭스

| 영역 | P1 | P2 | P3 |
| --- | ---: | ---: | ---: |
| 보안·세션 | ● | ○ | |
| TC·Timeout·로그 | ● | ○ | |
| CI/CD | ● | ○ | |
| Gateway·Cache·Batch | | ● | ○ |
| Handler·WAR 확장 | | ○ | ● |

---

## 32.3 17업무 WAR 확장·관측성·DR

### 17 WAR 로드맵

현재 **9 WAR** (ic, pc, ms, sv, pd, eb, ep, ss, mg) + 플랫폼. 설계 목표 **17 WAR**는 업무 도메인 추가에 따른 확장입니다.

| 단계 | 내용 |
| --- | --- |
| 1 | 9 WAR Handler·Catalog·테스트 **표준화** (sv 템플릿) |
| 2 | 신규 8 WAR — Gradle 모듈·6계층·OM Seed |
| 3 | Gateway Route·ztomcat 그룹 배포 |
| 4 | tcf-eai 연동 Contract 확대 |

신규 WAR 추가 체크: [부록 K](../부록/K-모듈-포트-Context-WAR-매핑표.md), [znsight-man/09-업무-WAR-구조.md](../../znsight-man/09-업무-WAR-구조.md)

### 관측성 (Observability)

| 영역 | 현재 | 목표 |
| --- | --- | --- |
| 거래로그 | TCF_TX_LOG, OM 조회 | Gateway↔WAR GUID 연계 |
| 메트릭 | tcf-batch 수집 | Prometheus/Grafana (P2) |
| 헬스 | Actuator | Liveness/Readiness/Deep/Smoke |
| 대시보드 | OM Dashboard | TPS·P95·Timeout 실시간 |
| 추적 | GUID, TraceId MDC | 분산 추적(OpenTelemetry, P3) |

문서: [docs/architecture/44-observability.md](../../docs/architecture/44-observability.md)

### DR (Disaster Recovery)

| 영역 | 과제 |
| --- | --- |
| SESSIONDB | JDBC 복제·Failover |
| RDW/ADW | 백업·Point-in-Time Recovery |
| LOGDB | 보존 정책·아카이브 |
| WAR | Rolling Deploy + **Rollback** (tcf-cicd) |
| Gateway | Route DB 백업 |

문서: [docs/architecture/45-disaster-recovery.md](../../docs/architecture/45-disaster-recovery.md)

### 로드맵 3단계

```text
Phase 1 (P1) — "안전하게 올라가는가?"
  → 보안·세션·Catalog·TC·CI/CD·Rollback

Phase 2 (P2) — "장애를 보고·조치할 수 있는가?"
  → Gateway·Cache·Batch·Dashboard·SQL 표준

Phase 3 (P3) — "같은 방식으로 확산할 수 있는가?"
  → 17 WAR·Handler 템플릿·SonarQube·EAI·관측성 고도화
```

### PMO 관점

| | 현재 | 목표 |
| --- | --- | --- |
| WAR | 9 | 17 |
| Handler | 도메인 패턴 ✅ | 전 WAR 동일 |
| CI | 골격 | Full Pipeline |
| 문서 | ztcfbook ✅ | OM↔docs 자동 sync |

### 성공 기준

1. **통제** — 모든 serviceId OM Catalog·거래통제 등록
2. **설명** — 장애 시 GUID→로그→SQL 추적 가능
3. **확장** — sv 샘플 복제로 신규 WAR·거래 2주 내 추가

---

## 장 요약

설계서 대비 코드는 **TCF 골격·Handler·EAI·문서화**에서 Gap이 크게 줄었고, 남은 과제는 **운영 가능성**(P1: 보안·세션·Catalog·CI/CD)과 **확장**(P3: 17 WAR)입니다. P1→P2→P3 로드맵으로 관측성·DR·Gateway Route를 단계적으로 보완하며, sv-service 표준 샘플을 템플릿으로 전 WAR에 확산하는 것이 향후 과제의 핵심입니다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제31장 공식 설계안 매핑](./31-공식-설계안-매핑.md) |
| → 다음 | [부록 A](../부록/A-업무코드-표준표.md) |

---

## 출처 색인

| 절 | 출처 |
| --- | --- |
| 32.1 | [zman/23-소스Gap분석.md](../../zman/23-소스Gap분석.md), [zman/00-설계서-코드베이스-대조표.md](../../zman/00-설계서-코드베이스-대조표.md) |
| 32.2 | [zman/24-보완과제-우선순위.md](../../zman/24-보완과제-우선순위.md) |
| 32.3 | [docs/architecture/44-observability.md](../../docs/architecture/44-observability.md), [45-disaster-recovery.md](../../docs/architecture/45-disaster-recovery.md), [architecture.md §3](../../docs/architecture/architecture.md) |
