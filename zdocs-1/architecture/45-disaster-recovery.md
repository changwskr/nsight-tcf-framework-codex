# 45. 장애복구·DR 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 45 |
| 제목 | Disaster Recovery & Business Continuity Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [16-deploy.md](16-deploy.md), [44-observability.md](44-observability.md), [43-security-operations.md](43-security-operations.md) |
| 상세 매뉴얼 | [67-롤백-절차.md](../../znsight-man/67-롤백-절차.md), [66-배포-절차.md](../../znsight-man/66-배포-절차.md), [68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md) |
| 대상 | 운영·인프라·아키텍트·업무 담당자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF의 **장애 복구(DR)·롤백·업무 연속성** 기준을 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| 배포 구조·WAR | [16-deploy.md](16-deploy.md) |
| 롤백 상세 절차 | [67-롤백-절차.md](../../znsight-man/67-롤백-절차.md) |
| 로그·추적 | [44-observability.md](44-observability.md) |
| DR·RTO/RPO·시나리오 | **본 문서 (45)** |

핵심 문장:

> 운영 전환은 개발 완료가 아니라, **장애 시 복구 가능함**을 확인하는 절차이다. 복구는 WAR만이 아니라 **설정·DB·Gateway·세션**을 함께 본다.

---

## 2. DR 범위·목표

### 2.1 복구 대상 계층

```text
[L4 / Apache]  → 라우팅·SSL·정적
      ▼
[tcf-gateway]  → JWT/세션 관문·라우팅 테이블
      ▼
[Tomcat WAR]   → 업무 9 + om + batch + ui + jwt
      ▼
[데이터]       → 업무 DB · SESSIONDB · TXLOG · JWT DB · Cache
```

### 2.2 목표 지표 (운영 설계 기준)

| 지표 | 설계 목표 | 비고 |
|------|-----------|------|
| 가용성 | 99.99% | 운영 전환 체크리스트 기준 |
| RTO (복구 목표시간) | **모듈별 30분 이내** (WAR 롤백) | 전체 센터 DR은 인프라 정책 따름 |
| RPO (데이터 손실 허용) | **업무 DB: 0~15분** (백업 주기) | TXLOG·감사는 별도 |
| P95 응답 | 3초 이내 | 부하 정상 시 |
| 동시 세션 | 43,200 | SESSIONDB 용량 계획 |

실제 수치는 조직 인프라 SLA에 맞게 [68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md)에서 확정한다.

---

## 3. 장애 유형·복구 전략

| 유형 | 예시 | 1차 복구 | 2차 복구 |
|------|------|----------|----------|
| **A. 단일 WAR** | sv-service 기동 실패 | 직전 WAR 롤백 | 원인 분석 후 재배포 |
| **B. Tomcat** | JVM OOM, Thread 고갈 | 인스턴스 재기동 | Heap·Pool 튜닝 |
| **C. Gateway** | 라우팅·JWT 검증 장애 | Gateway 롤백 또는 JWT off (승인) | JWKS·Route 복구 |
| **D. DB** | 업무 DB 장애 | DR DB 전환 (인프라) | Failback 절차 |
| **E. SESSIONDB** | 세션 DB 불가 | 세션 재로그인 유도 | SESSIONDB 복구 |
| **F. TXLOG DB** | 거래로그 INSERT 실패 | **거래는 계속** (설계상) | TXLOG DS 복구 |
| **G. 전체 센터** | IDC 장애 | DR 센터 기동 | 동기화·역전환 |

---

## 4. WAR·애플리케이션 복구

### 4.1 배포 단위

| 단위 | 복구 방법 |
|------|-----------|
| 단일 업무 WAR | `/{code}` context 교체 — **다른 WAR 영향 없음** |
| tcf-om | OM 전체 영향 — 우선 복구 대상 |
| tcf-ui | 화면·Relay — OM과 함께 검증 |
| tcf-jwt | 발급 중단 → Gateway JWT 검증 실패 연쇄 |
| tcf-gateway | 전 업무 프록시 영향 |
| tcf-batch | 모니터링·수집만 영향 (온라인과 분리) |

### 4.2 롤백 원칙 (요약)

상세: [67-롤백-절차.md](../../znsight-man/67-롤백-절차.md)

| 원칙 | 내용 |
|------|------|
| 서비스 복구 우선 | 장시간 원인 분석보다 직전 정상본 복구 |
| WAS 단위 순차 | 전체 동시 롤백 금지 |
| 트래픽 제외 | L4/Apache에서 대상 인스턴스 drain |
| 설정 동반 | `application-prod.yml`, env, Gateway Route 함께 확인 |
| 검증 4단계 | Liveness → Readiness → Deep → Smoke |
| 이력 | OM 배포·롤백 이력 필수 |

### 4.3 롤백 발생 조건

| 조건 | 판단 |
|------|------|
| Context 기동 실패 | `/actuator/health` 무응답 |
| Smoke Test 실패 | 대표 `serviceId` 표준 응답 실패 |
| 오류율 급증 | 배포 전 대비 5xx·업무 오류 |
| Timeout 급증 | `E-TIMEOUT-*`, UNKNOWN 증가 |
| 리소스 | Thread >70%, Heap >70%, Hikari Pool 고갈 지속 |

### 4.4 ztomcat / 로컬 검증 (개발·통합)

운영 DR 전에 로컬에서 복구 절차를 리허설한다.

```bash
cd ztomcat
./deploy-wars.sh all      # 또는 대상 WAR만
./verify-deploy.sh        # health·smoke
```

`verify-deploy` 실패 시: `logs/catalina.out`, `nsight-app.log`, `guid` 기준 거래로그 ([44-observability.md](44-observability.md)).

---

## 5. 데이터·DB 복구

### 5.1 DB 역할 분리

| DB | 용도 | DR 우선순위 | 거래 중단 여부 |
|----|------|-------------|----------------|
| 업무 DB (RDW/ADW) | 업무 데이터 | **최우선** | 중단 |
| SESSIONDB | Spring Session | 높음 | 재로그인으로 완화 가능 |
| TXLOG DB | `TCF_TX_LOG` | 중간 | **온라인 거래 계속** (warn) |
| JWT DB | 토큰·Denylist | 높음 (JWT 모드) | 발급·검증 영향 |
| OM 메타 | 기준정보·배포이력 | 높음 | OM·통제 영향 |
| H2 (local) | 개발 전용 | 해당 없음 | — |

### 5.2 RPO·백업 정책 (권장)

| 대상 | 백업 | 보관 | 복구 |
|------|------|------|------|
| 업무 DB | 일일 full + archive log | 30일+ | PITR / DR 스탠바이 |
| SESSIONDB | 일일 스냅샷 | 7일 | 스냅샷 복원 (세션 무효화 안내) |
| TXLOG | 일일 + 파티션 | 규정 따름 | 조회용 — 거래 재처리 아님 |
| JWT DB | 일일 | 14일 | 토큰 재발급 병행 |
| WAR Artifact | CI 보관 | N버전 | 직전 빌드 재배포 |
| 설정 (Git) | tag/branch | 영구 | checkout + 재배포 |

### 5.3 DDL vs DML 롤백

| 변경 | 롤백 |
|------|------|
| DML (기준정보) | 백업 테이블·스크립트로 역반영 |
| DDL (스키마) | **별도 DR 계획** — 앱 롤백만으로 불충분 |

배포 시 DDL 포함 여부는 Change Advisory Board 승인 필수.

---

## 6. Gateway·세션·JWT 장애

### 6.1 Gateway 장애

| 증상 | 조치 |
|------|------|
| 전 업무 502/504 | Gateway 인스턴스 롤백·재기동 |
| JWT만 실패 | JWKS URL·시계·키 확인 ([43-security-operations.md](43-security-operations.md)) |
| Route 오류 | `TCF_GATEWAY_ROUTE` 이전 스냅샷 복구 |

**임시 우회 (승인·시한):** UI Relay 직통 + 업무 WAR JWT Filter ON — Gateway 우회는 보안 리스크 있음.

### 6.2 SESSIONDB 장애

| 영향 | 완화 |
|------|------|
| Cookie 세션 검증 실패 | 사용자 재로그인 |
| Gateway 4단계 검증 실패 | JWT Bearer 경로 유지 (정책 허용 시) |
| tcf-batch 세션 집계 | 대시보드 지표만 지연 |

### 6.3 JWT(tcf-jwt) 장애

| 영향 | 완화 |
|------|------|
| 발급 불가 | 세션-only 모드 (단기·승인) |
| JWKS 불가 | Gateway/WAR JWT 검증 실패 — **즉시 복구 우선** |
| Denylist DB | 폐기 즉시 차단 미동작 — 짧은 Access TTL로 완화 |

---

## 7. 장애 시나리오별 런북

### 7.1 시나리오 S1 — 단일 업무 WAR 배포 후 Smoke 실패

```text
1. L4/Apache에서 해당 /{code} 인스턴스 drain
2. 직전 WAR로 교체 (Context 삭제 후 재전개)
3. /{code}/actuator/health 확인
4. 대표 serviceId Smoke (예: SV.Sample.inquiry)
5. 트래픽 복구 · OM 배포이력 롤백 기록
6. 동일 Artifact 재배포 금지 — 원인 분석 후 신규 빌드
```

### 7.2 시나리오 S2 — TXLOG DB 장애

```text
1. 온라인 거래는 계속됨 (설계) — 사용자 영향 없을 수 있음
2. 로그에서 "Failed to persist transaction log" warn 급증 확인
3. NSIGHT_TXLOG_PATH · TXLOG DB · Pool 연결 복구
4. OM 거래로그 조회 공백 구간 사후 보정(정책)
5. 근본 원인: DS 설정·디스크·권한
```

### 7.3 시나리오 S3 — 전체 Tomcat 인스턴스 다운

```text
1. 대체 인스턴스 또는 수평 확장 노드로 트래픽 전환
2. 미배포 WAR 일괄 deploy (검증된 Artifact 세트)
3. verify-deploy 4단계
4. OM·대표 업무 Smoke
5. 장애 인스턴스 Heap/Thread 덤프 후 재기동
```

### 7.4 시나리오 S4 — DR 센터 전환 (개요)

```text
1. 선언: DR 모드 (운영 총괄 승인)
2. DNS/L4 → DR Apache/Tomcat
3. DR DB (스탠바이) promote · 연결문자열 전환
4. SESSIONDB — 재로그인 공지 또는 DR SESSIONDB
5. Gateway·JWT·OM 순서 기동
6. Smoke 전 업무 · 모니터링 2배 강화
7. Failback: PRIMARY 복구 후 역전환 (별도 계획)
```

---

## 8. 복구 검증 체크리스트

### 8.1 기술 검증

- [ ] Liveness `/actuator/health` UP
- [ ] Readiness — DB·Cache·연동 Deep Check
- [ ] 대표 `serviceId` Smoke — `resultCode=S0000`
- [ ] Gateway 경유·직통 경로 각각 1건
- [ ] JWT 모드: Bearer 발급·검증·STF 정합성
- [ ] `TX_START`/`TX_END` 쌍 정상 ([44-observability.md](44-observability.md))
- [ ] OM 거래로그·감사로그 적재

### 8.2 업무 검증

- [ ] 업무 담당자 대표 거래 확인 (서명)
- [ ] 배치·스케줄 정상 (tcf-batch)
- [ ] 파일 업·다운로드 (해당 시)

### 8.3 복구 완료 선언

| 항목 | 기록 |
|------|------|
| 장애 시작·종료 시각 | |
| 영향 업무코드 | |
| 복구 방식 (롤백/DR/재기동) | |
| Artifact 버전 (Git tag) | |
| 담당·승인자 | |
| 사후 RCA 일정 | |

---

## 9. 역할·연락 (RACI 요약)

| 활동 | 개발 | 운영 | 인프라 | 업무 |
|------|:----:|:----:|:------:|:----:|
| 롤백 판단·실행 | C | R/A | C | I |
| DR 센터 전환 | I | R | A | I |
| DB 복구 | C | C | R/A | I |
| Smoke·업무 확인 | C | R | I | A |
| 사후 RCA | R | C | C | C |

---

## 10. 정기 DR 리허설

| 주기 | 내용 |
|------|------|
| 분기 | 단일 WAR 롤백 + Smoke (스테이징) |
| 반기 | Gateway·JWT·SESSIONDB 장애 시뮬레이션 |
| 연 1회 | DR 센터 전환 리허설 (인프라 주관) |

리허설 결과는 OM 또는 전환대장에 기록하고, RTO 실측치를 본 문서 §2.2와 비교한다.

---

## 11. 관련 소스·스크립트

| 경로 | 역할 |
|------|------|
| `ztomcat/deploy-wars.*` | WAR 배포 |
| `ztomcat/verify-deploy.*` | 배포 검증 |
| `tcf-cicd/` | CI/CD 파이프라인 |
| `gradle buildZtomcatWars` | WAR 일괄 빌드 |
| `tcf-core/.../TransactionLogService` | TXLOG 실패 시 warn (거래 지속) |

---

← [44-observability.md](44-observability.md) · [46-service-integration-contract.md](46-service-integration-contract.md) →
