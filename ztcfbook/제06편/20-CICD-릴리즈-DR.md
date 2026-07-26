# 제20장. CI/CD · 릴리즈 · DR

| 항목 | 내용 |
| --- | --- |
| **편** | 제6편 · 환경·빌드·배포 |
| **장** | 제20장 |
| **파일** | `제06편/20-CICD-릴리즈-DR.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 20.1 WAR 생성·배포 절차

NSIGHT TCF 운영 배포 단위는 **WAR 파일**이다. tcf-core, tcf-web, tcf-cache는 독립 배포하지 않고 업무 WAR의 `WEB-INF/lib`에 포함된다. Gradle task **`bootWar`**(또는 `war`)로 산출물을 만들며, Artifact Repository에 버전·Git commit·빌드 시각 메타를 함께 저장한다.

표준 배포 순서(GitLab CI/CD·OM 배포관리 설계 정렬):

```text
Source Checkout → 설정 동기화(tcf-cicd)
→ Gradle clean build → Unit/Integration Test
→ bootWar 생성 → Artifact 저장
→ 배포 승인( stg/prd )
→ 기존 WAR 백업 → Tomcat에 신규 WAR 반영
→ Reload/Restart → Health Check → Smoke Test
→ OM 배포 이력 기록 → 서비스 오픈
```

WAR 명명: `sv.war`, `om.war`, `gw.war`, `ui.war` 등 context와 매핑(부록 K). 단일 업무 WAR 교체는 다른 context에 영향 없음. **tcf-om** 장애는 Catalog·SESSION·Dashboard 전체에 영향 → 우선 복구 대상.

배포 전 체크: Catalog·TC·Route seed diff, profile secret 주입, DB migration 순서, Gateway session-datasource URL, JWT JWKS reachable. 배포 후: Actuator health, sample `POST /{bc}/online` smoke, Gateway guid log 상관.

WAR 산출물 검증: `bootWar` 후 WAR 내부 `WEB-INF/lib/tcf-core-*.jar` 버전이 Git Tag와 일치하는지 spot check한다. 공통 lib만 변경된 MR은 **모든 영향 WAR rebuild**가 필수이다.

배포 전 체크리스트(요약):

| # | 항목 |
| --- | --- |
| 1 | MR merge + Tag |
| 2 | Gradle test green |
| 3 | bootWar Artifact upload |
| 4 | DB migration (stg 선행) |
| 5 | tcf-cicd profile secret |
| 6 | OM seed·Route diff |
| 7 | Rollback WAR N-1 준비 |

---

## 20.2 CI/CD 파이프라인

CI/CD 핵심 원칙: **Git 기준 배포**, Merge Request 기반(develop/main/release direct push 금지), Gradle Wrapper, 전체 검증 후 배포, WAR 단위, 환경별 설정 분리, 운영 배포 승인, Health Check 필수, Rollback 가능, OM 이력 기록.

표준 Pipeline Stage:

| Stage | 주요 작업 |
| --- | --- |
| validate | Branch 정책, Secret·금지 파일 검사 |
| build | compileJava, processResources, bootWar |
| test | Unit, Mapper, Context, TCF Transaction Test |
| quality | 정적 분석, 보안 스캔(SonarQube 등) |
| publish | Artifact Repository 업로드 |
| deploy-dev | Dev 자동 배포 |
| deploy-stg | Staging 수동 승인 |
| deploy-prd | 운영 승인 + Rolling Deploy |
| verify | Liveness, Readiness, Deep, Smoke |

`tcf-cicd` 모듈과 zguide tcf-cicd 개발가이드에 profile별 yaml, deploy script, Tomcat path가 정리되어 있다. GitLab Runner는 `./gradlew`만 사용해 로컬·CI 재현성을 맞춘다.

공통 모듈만 변경된 MR은 **영향 WAR 전체 rebuild**가 필요하다. tcf-core 변경 → 모든 업무 WAR bootWar· regression test. Pipeline parallel matrix로 WAR별 job 분할을 권장한다.

Pipeline과 로컬 parity: CI에서 `./gradlew clean build`와 동일 명령을 개발자 PC에서 먼저 실행한다. `-x test`로 deploy only 하는 것은 **hotfix 승인 runbook**에만 허용하고, 정상 릴리즈는 test stage 필수 pass.

```text
feature/* ──MR──► develop ──MR──► release/* ──► main + Tag vX.Y.Z
                                              │
                                              ▼
                                    CI: build → test → publish
                                              │
                         deploy-dev (auto) → deploy-stg (manual) → deploy-prd (approval)
```

---

## 20.3 롤백·운영 전환

롤백은 **직전 WAR 백업본으로 context 교체** + Tomcat restart/reload + Health Check이다. Artifact Repository에서 N-1 버전 WAR를 재배포할 수 있어야 하며, DB migration이 forward-only면 롤백 시 schema 호환성을 사전 검토한다.

DR·업무 연속성 범위:

```text
L4/Apache → tcf-gateway → Tomcat WAR → DB·SESSION·TXLOG·JWT·Cache
```

장애 유형별 1차 복구:

| 유형 | 1차 복구 |
| --- | --- |
| 단일 WAR | WAR 롤백 |
| Tomcat JVM | 인스턴스 재기동 |
| Gateway | gw.war 롤백 또는 JWT off(승인) |
| SESSIONDB | 재로그인 유도 + DB 복구 |
| TXLOG DB | 거래는 계속(설계), LOG DS 복구 |
| 전체 센터 | DR 센터 기동(인프라 SLA) |

설계 목표(운영 체크리스트 기준): 가용성 99.99%, WAR 롤백 RTO **30분 이내**, RPO 업무 DB 0~15분. 운영 전환은 "개발 완료"가 아니라 **장애 시 복구 가능함** 확인 절차(부록 J).

OM `OM.Deploy.*`에 롤백 지시·이력을 남기고, tcf-batch DeployStatusCollect가 반영 확인한다. Gateway Route·TCF_GATEWAY_ROUTE H2/Oracle backup은 DR runbook에 포함한다.

롤백 시나리오 drill(분기 1회 권장):

1. stg에 vN 배포 → smoke pass
2. vN+1 배포 → 의도적 defect inject
3. N-1 WAR restore → Health Check → smoke
4. DB migration rollback 불가 시 forward fix branch 준비 확인

운영 전환 Gate(부록 J): 기능·성능·보안·관측·DR·runbook·OM Catalog·TC·Route·교육 완료.

---

## 20.4 릴리즈·브랜치 전략

Git 브랜치(매뉴얼 07장): **feature/** → MR → **develop** → release/** → **main** + Tag. hotfix/**는 main에서 분기 후 develop back-merge. release 브랜치에서 version bump, changelog, 최종 QA 후 Tag `v{major}.{minor}.{patch}`.

릴리즈 산출물: WAR set, tcf-cicd config bundle, DB migration script, OM seed diff, Route seed, release note. Tag와 Artifact immutable binding — prod deploy는 Tag만 허용.

환경 promotion: local → dev(auto) → stg(manual) → prd(approval). 각 gate에서 smoke test checklist(부록 H 개발 완료, J 운영 전환). Feature flag는 OM SystemConfig 또는 profile로 prod 전 점진 오픈.

버전 정책: SemVer. Breaking Catalog/TC change는 minor/major bump + migration guide. 공통 lib(tcf-core) 변경은 **모든 WAR 동일 릴리즈 train** 또는 compatibility matrix 문서화.

hotfix 절차: main에서 `hotfix/*` 분기 → fix → main Tag → prod deploy → develop back-merge. hotfix는 scope 최소화·전체 regression 축소 불가 시 영향 WAR만 rebuild하되 smoke는 필수.

| 브랜치 | 용도 | deploy |
| --- | --- | --- |
| feature/* | 개발 | local only |
| develop | 통합 | dev auto |
| release/* | QA·버전 고정 | stg |
| main | 운영 Tag | prd |
| hotfix/* | 긴급 수정 | prd (승인) |

---

## 20.5 장애 대응·FAQ

개발자 장애 확인 항목(매뉴얼 69): 동일 guid로 Gateway log·TCF_TX_LOG·Step log·ERROR_LOG 조회, STF 차단 코드(E-TCF-CTL, E-COM-AUTH, E-JWT-AUTH), Timeout 초과, DB pool exhausted, H2 lock, Route 404, JWKS connection refused.

FAQ 예:

| 증상 | 확인 |
| --- | --- |
| 401 Gateway | Route, session DB, JWT, login-required |
| E-TCF-CTL-001 | OM TC Row, cache Evict |
| Catalog 없음 | tcf-om, data.sql |
| Dashboard empty | tcf-batch Job, OM_BATCH_HISTORY |
| Set-Cookie lost | UI Relay, Gateway GEF |

장애 등급별: P1 단일 WAR 롤백, P2 Gateway/OM 우선, P3 batch/UI. 커뮤니케이션: OM Dashboard, 감사팀 guid list, postmortem template. 재발 방지: code fix + Catalog/TC + pipeline test + runbook update.

운영 FAQ(70장)와 observability(44장)를 runbook과 링크한다. DR drill 분기 1회: WAR rollback, SESSIONDB fail, Gateway JWT off 시나리오 tabletop.

장애 triage 순서:

```text
1. HTTP status / result.code 확인
2. guid로 TCF_GATEWAY_TX_LOG ↔ TCF_TX_LOG 상관
3. TCF_TX_STEP_LOG에서 slow/fail SQL ID
4. TCF_ERROR_LOG stack trace
5. OM Dashboard AP/DB/Session
6. 최근 deploy·seed·cache Evict 이력
```

CI/CD 장애: pipeline fail 시 Artifact publish skip → **이전 Tag prod 유지**. deploy stage fail 시 자동 rollback script 실행 여부를 pipeline 정의에 명시한다. postmortem에 "로컬 재현 명령 `./gradlew :module:test`"를 포함해 재발 방지한다.

---

## 장 요약

NSIGHT TCF CI/CD는 Gradle build·test·bootWar·Artifact·환경별 deploy·Health Check·OM 이력을 표준화한다. 배포 단위는 WAR이며 공통 lib는 WAR에 포함한다. 롤백·DR은 WAR·Gateway·SESSION·DB 계층별 runbook과 RTO/RPO 목표를 따른다. Git Tag 릴리즈·브랜치 전략과 장애 guid 추적·FAQ로 운영 전환과 복구 가능성을 확보한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제19장 로컬 개발환경](../제06편/19-로컬-개발환경.md) |
| → 다음 | [제21장 테스트 전략](../제07편/21-테스트-전략.md) |

---

## 출처 색인

| 절 | 참고 문서 |
| --- | --- |
| 20.1 | [znsight-man/64-WAR-생성-기준.md](../../znsight-man/64-WAR-생성-기준.md), [znsight-man/66-배포-절차.md](../../znsight-man/66-배포-절차.md), [docs/architecture/16-deploy.md](../../docs/architecture/16-deploy.md) |
| 20.2 | [znsight-man/65-CICD-파이프라인-기준.md](../../znsight-man/65-CICD-파이프라인-기준.md), [docs/architecture/49-release-strategy.md](../../docs/architecture/49-release-strategy.md), [zguide/tcf-cicd-개발가이드.md](../../zguide/tcf-cicd-개발가이드.md) |
| 20.3 | [znsight-man/67-롤백-절차.md](../../znsight-man/67-롤백-절차.md), [znsight-man/68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md), [docs/architecture/45-disaster-recovery.md](../../docs/architecture/45-disaster-recovery.md), [ztcfbook/부록/J-운영-전환-체크리스트.md](../부록/J-운영-전환-체크리스트.md) |
| 20.4 | [docs/architecture/49-release-strategy.md](../../docs/architecture/49-release-strategy.md), [znsight-man/07-Git-브랜치-기준.md](../../znsight-man/07-Git-브랜치-기준.md) |
| 20.5 | [znsight-man/69-장애-개발자-확인-항목.md](../../znsight-man/69-장애-개발자-확인-항목.md), [znsight-man/70-장애-FAQ.md](../../znsight-man/70-장애-FAQ.md), [docs/architecture/44-observability.md](../../docs/architecture/44-observability.md) |
