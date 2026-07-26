# 49. 릴리즈 전략 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 49 |
| 제목 | Release Strategy Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [16-deploy.md](16-deploy.md), [22-build-project.md](22-build-project.md), [25-env-profile.md](25-env-profile.md), [45-disaster-recovery.md](45-disaster-recovery.md), [48-multi-module-dependencies.md](48-multi-module-dependencies.md) |
| 상세 매뉴얼 | [07-Git-브랜치-기준.md](../../znsight-man/07-Git-브랜치-기준.md), [65-CICD-파이프라인-기준.md](../../znsight-man/65-CICD-파이프라인-기준.md), [66-배포-절차.md](../../znsight-man/66-배포-절차.md), [67-롤백-절차.md](../../znsight-man/67-롤백-절차.md), [68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md) |
| 대상 | 개발·릴리즈·운영·DevOps 담당자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF의 **버전·브랜치·빌드·배포·롤백**을 아우르는 릴리즈 전략을 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| 배포 토폴로지·WAR | [16-deploy.md](16-deploy.md) |
| Gradle 빌드 | [22-build-project.md](22-build-project.md) |
| 환경 프로파일 | [25-env-profile.md](25-env-profile.md) |
| 장애·롤백 | [45-disaster-recovery.md](45-disaster-recovery.md) |
| **릴리즈 전략 (본 문서)** | 브랜치·Tag·파이프라인·승인·산출물 |

핵심 문장:

> 릴리즈는 **Git Tag + WAR Artifact + OM 배포 이력**으로 식별한다. 운영 반영은 **승인된 파이프라인**만 수행하고, 롤백은 **직전 Tag/WAR**로 복구한다.

---

## 2. 릴리즈 단위

### 2.1 식별자 3종

| 식별자 | 예 | 용도 |
|--------|-----|------|
| **Git Tag** | `v1.2.0`, `v1.2.1-hotfix` | 소스 스냅샷·롤백 기준 |
| **Gradle version** | `1.0.0-SNAPSHOT` (루트) | 빌드 메타·MANIFEST |
| **Artifact ID** | `sv.war@abc123` | 저장소·OM 배포 이력 |

운영 배포 시 **Tag ↔ Artifact ↔ OM 배포 ID**를 1:1로 기록한다.

### 2.2 배포 단위

| 단위 | 설명 |
|------|------|
| **WAR** | 업무·플랫폼 독립 배포 (`sv.war`, `om.war`, …) |
| **설정** | `tcf-cicd/{profile}/spring/*` — sync 또는 runtime mount |
| **인프라** | Apache `prod/apache`, Tomcat `setenv` |
| **기준정보** | OM DB (Catalog·TC·코드) — WAR와 **동기** 필요 |

공통 lib(`tcf-core` 등)는 **단독 배포 불가** — 포함 WAR 재빌드 ([48-multi-module-dependencies.md](48-multi-module-dependencies.md)).

---

## 3. 브랜치·릴리즈 흐름

### 3.1 표준 브랜치 모델

```text
main          ← 운영 배포 기준 (직접 push 금지)
  ↑
release/v*    ← 배포 준비·결함 수정만
  ↑
develop       ← 통합 개발 (MR merge)
  ↑
feature/*     ← 기능 개발
hotfix/*      ← main에서 분기 → main + develop 병합
```

상세: [07-Git-브랜치-기준.md](../../znsight-man/07-Git-브랜치-기준.md)

### 3.2 릴리즈 타임라인

```text
[1] feature/* → develop (MR + 리뷰 + CI)
[2] develop 안정화 → release/v1.x.y 분기
[3] release: ztomcat 통합 검증, Smoke, 보안 Gate
[4] release → main merge + Tag v1.x.y
[5] CI: bootWar 전 모듈 → Artifact 저장
[6] Staging 수동 승인 배포 → Health 4단계
[7] 운영 승인 → Rolling deploy (WAS 단위)
[8] OM 배포 이력·운영 전환 체크리스트 완료
[9] release → develop back-merge
```

### 3.3 Hotfix

```text
main → hotfix/inc-YYYYMMDD-*
  → main merge + Tag v1.x.(y+1)
  → develop back-merge (필수)
  → 동일 Artifact 재배포 금지 — 신규 빌드
```

---

## 4. 환경·프로모션

### 4.1 환경 맵

| 환경 | 프로파일 | 실행 모델 | 설정 SoT |
|------|----------|-----------|----------|
| 로컬 | `local` | bootRun / ztomcat | `tcf-cicd/local/` + 모듈 yml |
| 통합 | `dev` | ztomcat :8080 | `tcf-cicd/dev/` |
| 스테이징 | `stg` (목표) | Tomcat cluster | `tcf-cicd/prod/` 샘플 |
| 운영 | `prod` | Apache → Tomcat | `tcf-cicd/prod/` + vault |

`sync-to-framework.ps1` — 빌드 전 `tcf-cicd` → framework yml 반영 ([tcf-cicd/README.md](../../tcf-cicd/README.md)).

### 4.2 프로모션 규칙

| 단계 | Gate |
|------|------|
| local → dev | 개발자 자가 검증 |
| dev → stg | MR merge develop + `buildZtomcatWars` + Smoke |
| stg → prod | 릴리즈 Tag + CAB 승인 + [68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md) |

동일 **Git commit**에서 환경별로 **프로파일만** 바꿔 빌드 — 소스 분기 배포 금지.

---

## 5. CI/CD 파이프라인

### 5.1 역할 분리

| 주체 | 책임 |
|------|------|
| **개발자** | feature 브랜치·MR·단위 테스트 |
| **CI (GitLab Runner)** | compile · test · bootWar · Artifact |
| **CD** | 배포·Health·Rollback **실행** |
| **OM** | 요청·**승인**·이력·롤백 **지시** |

### 5.2 표준 Stage

```text
checkout → secret-scan → gradle build → unit-test
  → quality-gate (SonarQube 목표)
  → bootWar (변경 모듈 또는 전체)
  → artifact-upload
  → [dev] auto-deploy
  → [stg/prod] manual-approval
  → deploy (drain → WAR → health → traffic)
  → om-callback (배포 이력)
```

상세: [65-CICD-파이프라인-기준.md](../../znsight-man/65-CICD-파이프라인-기준.md)

### 5.3 로컬 CI 스크립트 (현행)

| 스크립트 | 동작 |
|----------|------|
| `tcf-cicd/scripts/cicd-build.ps1` | sync + Gradle |
| `tcf-cicd/scripts/cicd-deploy.ps1` | sync → build → webapps |
| `tcf-cicd/local/script/build-all.ps1` | local 전체 빌드 |
| `ztomcat/verify-deploy.*` | Health 검증 |

GitLab `.gitlab-ci.yml`은 설계 기준 — Runner에서 `gradlew`/`cicd-deploy` 호출.

### 5.4 품질 Gate

| Gate | 기준 |
|------|------|
| Compile | 전 모듈 또는 affected modules |
| Unit Test | `:tcf-core:test` + 변경 WAR |
| Mapper | XML 유효성 (목표) |
| 정적 분석 | SonarQube (목표) |
| 보안 | Secret·의존 취약점 스캔 |
| Smoke | 대표 `serviceId` — 배포 후 |

---

## 6. 빌드·산출물

### 6.1 Gradle 집계 태스크

| 명령 | 산출물 |
|------|--------|
| `gradle buildBusinessWars` | 9 업무 + `tcf-om` |
| `gradle buildZtomcatWars` | business + batch + ui + uj + jwt + gateway |
| `:sv-service:bootWar` | 단일 WAR |

버전: `com.nh.nsight` / `1.0.0-SNAPSHOT` — 릴리즈 시 `-SNAPSHOT` 제거 또는 Tag로 고정.

### 6.2 WAR 명명

| 모듈 | 파일명 | Context |
|------|--------|---------|
| `sv-service` | `sv.war` | `/sv` |
| `tcf-om` | `tcf-om.war` → `om.war` | `/om` |
| `tcf-gateway` | `gw.war` | `/gw` |
| `tcf-jwt` | `jwt.war` | `/jwt` |

ztomcat 배포: **13 WAR** (gateway·uj는 bootRun 전용). 상세: [16-deploy.md](16-deploy.md), [64-WAR-생성-기준.md](../../znsight-man/64-WAR-생성-기준.md).

### 6.3 Artifact 보관

| 항목 | 정책 |
|------|------|
| 보관소 | Nexus / GitLab Package (목표) |
| 보관 기간 | 운영 N버전 + 1 (최소 직전 정상본) |
| 메타데이터 | Git commit SHA, Tag, 빌드 시각, 모듈 목록 |
| 비밀 | Artifact에 credentials **포함 금지** |

---

## 7. 배포 전략

### 7.1 배포 모드

| 모드 | 용도 |
|------|------|
| **Rolling** (기본) | WAS 단위 순차 — 트래픽 drain 후 WAR 교체 |
| **Blue-Green** (선택) | 전체 전환 — 스테이징 검증 후 DNS/L4 스위치 |
| **WAR 단위** | 단일 업무코드만 배포 (영향 격리) |

### 7.2 배포 순서 (권장)

```text
1. tcf-om (기준정보·통제)
2. tcf-jwt (JWT 모드 시)
3. tcf-gateway
4. 업무 WAR (의존·Smoke 순)
5. tcf-batch, tcf-ui
```

연동 변경 시 호출·피호출 WAR **호환 버전** 동시 또는 피호출 선행 ([46-service-integration-contract.md](46-service-integration-contract.md)).

### 7.3 배포 절차 요약

1. OM 배포 요청·승인
2. L4/Apache drain (대상 인스턴스)
3. 직전 WAR 백업
4. 신규 WAR 전개 + Context reload
5. Liveness → Readiness → Deep → Smoke
6. 트래픽 복구
7. OM `OM_DEPLOY_*` 기록

상세: [66-배포-절차.md](../../znsight-man/66-배포-절차.md)

---

## 8. 롤백·릴리즈 실패

### 8.1 롤백 트리거

| 조건 | 조치 |
|------|------|
| Context 기동 실패 | 즉시 직전 WAR |
| Smoke 실패 | 롤백 또는 drain 유지 |
| 오류율 급증 | 배포 직전 Artifact로 복구 |

### 8.2 롤백 실행

```text
1. 트래픽 제외
2. Tag/Artifact 직전 정상본 배포
3. 설정·Gateway Route 동반 확인
4. Health 4단계
5. OM 롤백 이력 (사유·버전·담당자)
6. 동일 실패 Artifact 재배포 금지
```

상세: [67-롤백-절차.md](../../znsight-man/67-롤백-절차.md), [45-disaster-recovery.md](45-disaster-recovery.md)

---

## 9. 변경 영향·릴리즈 범위

### 9.1 모듈 변경 시 릴리즈 범위

| 변경 | 릴리즈 범위 |
|------|-------------|
| `tcf-core` / `tcf-web` | **전체 업무 WAR** 재빌드 권장 |
| 단일 `sv-service` | `sv.war`만 |
| `tcf-cicd/prod` only | 설정 sync — WAR 재빌드 없을 수 있음 |
| OM 기준정보 (DML) | WAR 무관 — Cache Evict |
| OM DDL | 별도 DB 릴리즈 계획 |

### 9.2 릴리즈 노트 항목

- Tag·commit 범위
- 포함 WAR 목록
- OM 기준정보 변경 요약
- Breaking change (serviceId·API·DDL)
- 롤백 Tag·Artifact ID
- Smoke 대표 거래 목록

---

## 10. 버전·Tag 규칙

### 10.1 Semantic Versioning

| 구분 | 증가 | 예 |
|------|------|-----|
| MAJOR | 비호환 Contract·DDL | v2.0.0 |
| MINOR | 기능·serviceId 추가 | v1.3.0 |
| PATCH | 결함·hotfix | v1.3.1 |

### 10.2 Tag 명명

| 유형 | 형식 |
|------|------|
| 정기 릴리즈 | `v{major}.{minor}.{patch}` |
| Hotfix | `v{major}.{minor}.{patch}` 또는 `v{major}.{minor}.{patch}-hotfix.{n}` |
| RC (선택) | `v1.3.0-rc.1` |

`main` merge 후 **반드시 Tag** — 운영 롤백 기준.

---

## 11. 승인·감사

| 활동 | 승인자 |
|------|--------|
| develop merge | 코드 리뷰어 (MR) |
| release → main | 릴리즈 매니저 + QA |
| 운영 배포 | 운영 총괄 + OM 승인 |
| hotfix | 장애 총괄 + 사후 RCA 일정 |
| `tcf-core` 변경 | 프레임워크 리드 |

모든 운영 배포는 OM 배포 이력·감사 로그에 남긴다.

---

## 12. 현행 vs 목표 (Gap)

| 항목 | 현행 (develop) | 목표 |
|------|----------------|------|
| CI | `tcf-cicd` 스크립트 | GitLab Pipeline 전면 |
| 품질 Gate | 부분 테스트 | SonarQube 필수 |
| Artifact | 로컬 `build/libs` | 중앙 저장소 |
| 업무 WAR | 9개 | 17개 확장 |
| 자동 롤백 | 수동 | Health 실패 시 파이프라인 롤백 |
| OM Callback | 설계 | CI/CD → OM API 연동 |

---

## 13. 릴리즈 체크리스트

### 13.1 릴리즈 전

- [ ] `release/v*` 브랜치·Tag 계획
- [ ] `gradle buildZtomcatWars` 성공
- [ ] `verify-deploy` / Smoke 전 업무
- [ ] OM Catalog·TC·Timeout 반영
- [ ] `tcf-cicd` prod sync 검토
- [ ] 롤백 Artifact 확보
- [ ] [68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md)

### 13.2 릴리즈 후

- [ ] OM 배포 ID·Tag 기록
- [ ] 모니터링 2배 강화 (24h)
- [ ] 릴리즈 노트 배포
- [ ] develop back-merge 확인 (release/hotfix)

---

## 14. 관련 소스

| 경로 | 설명 |
|------|------|
| `build.gradle` | version, `buildBusinessWars`, `buildZtomcatWars` |
| `tcf-cicd/manifest.yaml` | 모듈·프로파일 매핑 |
| `tcf-cicd/scripts/cicd-deploy.ps1` | sync·build·deploy |
| `ztomcat/deploy-wars.*` | WAR 배포 |
| `ztomcat/verify-deploy.*` | 배포 검증 |

---

← [48-multi-module-dependencies.md](48-multi-module-dependencies.md) · [50-test-architecture.md](50-test-architecture.md) →
