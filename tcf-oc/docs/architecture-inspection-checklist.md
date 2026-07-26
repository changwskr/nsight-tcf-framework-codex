# TCF-OC 아키텍처 점검리스트

> NSIGHT 마케팅플랫폼 **용량·환경설정** 아키텍처를 점검하기 위한 통합 체크리스트입니다.  
> 아래 문서를 교차 참조하여 작성했습니다.

| 문서 | 경로 |
|------|------|
| 모듈 README | [`../README.md`](../README.md) |
| 용량산정 설계기준 | [`capacity-design-standard.md`](capacity-design-standard.md) |
| cap-new Wizard 설계 | [`cap-new-design.md`](cap-new-design.md) |
| HELP — OC 포털 흐름 | `tcf-help/docs/20-oc-용량-환경/01-oc-포털-흐름.md` |
| HELP — CAP 산정 단계 | `tcf-help/docs/20-oc-용량-환경/02-cap-산정-단계.md` |
| HELP — ENV-002~004 | `tcf-help/docs/20-oc-용량-환경/03-env-002-004.md` |
| HELP — Rule 점검 | `tcf-help/docs/20-oc-용량-환경/04-rule-check.md` |
| HELP — cap-new Wizard | `tcf-help/docs/20-oc-용량-환경/05-cap-new-wizard.md` |
| 용량산정 가이드 (57장) | [`../../ztcf-book-capacity-md/README.md`](../../ztcf-book-capacity-md/README.md) |

## 사용 방법

1. **점검 순서**: §1 배포 → §2 산정 체인 → §3~5 CAP/cap-new/ENV → §6 Rule → §7~9 DR·VM·템플릿 → §10 문서
2. **결과** 열에 `Pass` / `Fail` / `N/A` / `보류` 기록
3. Fail 시 **비고**에 근거·조치·담당 기록
4. cap-new 시나리오 점검 시 **템플릿 코드**·**운영 기준 시나리오**를 비고에 명시

---

## §1 모듈·배포 아키텍처

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| A-01 | UI·API 분리 | 브라우저는 tcf-ui(8099) 정적 화면 + Relay, 업무 로직은 tcf-oc(8094) | README §기능·패키지 | `tcf-ui` bootRun → `/oc/index.html` 접속, Network 탭에서 `/api/oc/*` Relay 확인 | |
| A-02 | WAR 배포 경로 | `oc.war` → ztomcat `/oc`, Tomcat 시 UI 접두사 `/ui` 자동 보정 | README §화면 | WAR 배포 후 `/ui/oc/capacity.html` 접근·`ui-context.js` 동작 확인 | |
| A-03 | CAP / cap-new / ENV API 분리 | prefix 각각 `/api/oc/capacity`, `/api/oc/cap-new`, `/api/oc/env` | capacity-design-standard §2 | Swagger·Network 또는 컨트롤러 매핑 대조 | |
| A-04 | 공통 산정 엔진 공유 | CAP·cap-new가 `support` 패키지 참조, 코드·DB는 분리 | capacity-design-standard §2, cap-new-design §7 | `NsightCapacityDerivation` 등 import 경로 확인 | |
| A-05 | eb-service 패키지 규약 | `entry` / `application` / `persistence` / `support` 계층 준수 | README §패키지 구조 | 패키지 트리·클래스 위치 점검 | |
| A-06 | MapperScan·DB 기동 | cap-new·템플릿 Mapper 빈 정상 등록, H2 schema 적용 | cap-new-design §5, README | `tcf-oc` bootRun 로그·`CAP_NEW_*` 테이블 존재 확인 | |
| A-07 | 권장 운영 흐름 | CAP 또는 cap-new → ENV-002 산정 → ENV-003/004 → Rule → check | HELP 01-oc-포털-흐름 | 운영 Runbook·화면 링크 순서 대조 | |

---

## §2 산정 체인·전제 (공통)

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| C-01 | 체인 연결성 | 사용자 → 동시요청 → TPS → AP → WAS Thread → DB Pool → DR 잔여가 단절 없음 | capacity-design-standard §1·§4, ztcf-book 전체 흐름 | cap-new STEP 8 종합표·ENV Grid에서 상·하위 수치 추적 | |
| C-02 | 전체 사용자 산정 | `totalUsers = 지점×지점당 + 본부 + 기타` (또는 DIRECT) | capacity-design-standard §4 STEP2 | STEP 2 저장값·검증 메시지 확인 | |
| C-03 | 세션 설계 | `designedSessions = ceil(totalUsers × (1 + sessionMarginRate))` | capacity-design-standard §4 STEP2 | STEP 2 결과·세션 여유율(기본 30%) 확인 | |
| C-04 | TPS 공식 | `concurrentUsers = round(totalUsers × rate)`, `targetTps = ceil(concurrent / responseSec)` | capacity-design-standard §4 STEP3 | STEP 3 실시간 TPS·시나리오표 대조 | |
| C-05 | 운영 기준 시나리오 | `operatingBaseline` 1개 지정, AP·WAS·Pool·종합 판정의 기준 TPS로 사용 | capacity-design-standard §3.2·§4 | STEP 3·STEP 6 `baselineScenarioCode` 일치 확인 | |
| C-06 | STEP6 baseline 정합 | STEP 3 `operatingBaseline` = STEP 6 `baselineScenarioCode`, `targetTps > 0` | capacity-design-standard §4 STEP6, cap-new-design §6 | PEAK_OPS·STG_SMALL 템플릿 STEP 6 저장·검증 | |
| C-07 | 스트레스 ≥ 운영 | 활성 STRESS 시 `STRESS TPS ≥ 운영 기준 TPS` | capacity-design-standard §4 STEP3 | STEP 3 검증·PERF_STRESS 템플릿 | |
| C-08 | Core TPS·TPMC 연동 | `tpsPerCore = floor(35 × 3000 / tpmcPerTps)` | capacity-design-standard §4 STEP4 | STEP 4 업무유형·TPMC 변경 시 Core TPS 역산 | |
| C-09 | VM 보정 TPS | 보정 ON 시 `vmAdjustedTps = floor(이론 × cpu × virt × ops / perfSafety)` | capacity-design-standard §4 STEP4 | STEP 4 보정계수 토글·minRequiredAp 변화 | |
| C-10 | AP 산정 | `minRequiredAp = ceil(designPeakTps / vmAdjustedTps)` | capacity-design-standard §4 STEP4 | STEP 4·5 AP 수치·설계 피크 TPS 연계 | |
| C-11 | WAS Thread | `apTps × avgHold × margin` → `threadsPerVm`, `recommendedMaxThreads` | capacity-design-standard §4 STEP6 | STEP 6 Tomcat 권장표·maxThreads | |
| C-12 | DB Pool 4단계 | 이론 Pool → Thread 상한 → min/cap → `totalDbSessions` | capacity-design-standard §4 STEP7 | STEP 7 Pool·DB Session·WAR 배분 | |
| C-13 | WAR Pool 합산 | 다중 WAR 시 `warPoolTotalSessions` ≤ 한도, 비중 분배 검증 | capacity-design-standard §4 STEP7 | STEP 7 WAR별 Pool 표·판정 | |
| C-14 | 종합 판정 | AP·WAS·DB·WAR Pool → NORMAL / WARN / CRITICAL | capacity-design-standard §4 STEP8·§9 | STEP 8 `overallJudgment`·riskSummary | |
| C-15 | 연쇄 재산정 | 상위 STEP 저장 시 하위 STEP 자동 재계산 | cap-new-design §16, capacity-design-standard §9 | STEP 2~7 변경 후 `cascadeImpact`·트랙 상태 | |

---

## §3 기존 CAP (CAP-010~050)

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| CAP-01 | 화면·API 가용 | `/oc/capacity.html`, `/api/oc/capacity` 정상 | README §API, HELP 02 | CAP-010~050 단계별 산정 실행 | |
| CAP-02 | 단계 매핑 | 020 TPS → 030 AP → 040 WAS → 050 DB Pool | capacity-design-standard §7, HELP 02 | `CapacityCalcStep`·화면 탭 대조 | |
| CAP-03 | 입력 의미 구분 | 실요청 사용자 vs 스트레스 TPS 숫자 의미 분리 (3%/5%/15% 등) | HELP 02 | CAP-010·020 입력 라벨·도움말 | |
| CAP-04 | ENV 연계 | 산정 값이 ENV-002 「산정 실행」에 반영 가능 | HELP 02·03 | CAP 산정 후 ENV-002 플래너 입력·실행 | |
| CAP-05 | 기존 전제 인지 | 가이드 1차안(3,600지점·21,600명) vs cap-new 기본(6,000·36,000) 병행 존재 | capacity-design-standard §3.3 | 문서·입력값 출처 기록 | |
| CAP-06 | cap-new 대조 | `legacy-compare` API로 운영 기준 지표 대조, 사용자 등가 변환 적용 | cap-new-design §13, HELP 05 | Wizard STEP 8 「기존 CAP 대조」 | |

---

## §4 cap-new Wizard (8단계)

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| CN-01 | 화면 맵 | index / wizard / compare / approved 4화면 동작 | HELP 05, cap-new-design §1 | 각 URL 접속·HELP 화면 맵 일치 | |
| CN-02 | API prefix | `/api/oc/cap-new` — scenarios·templates·compare·approve·export | cap-new-design §4 | Relay·컨트롤러 엔드포인트 목록 | |
| CN-03 | DB 영속 | `CAP_NEW_SCENARIO` STEP_PAYLOAD JSON, 상태 DRAFT/COMPLETED/APPROVED | cap-new-design §5 | H2·시나리오 CRUD·버전 필드 | |
| CN-04 | 템플릿 DB | `CAP_NEW_SCENARIO_TEMPLATE` 6종 builtin, API `GET /templates` | capacity-design-standard §6 | index 템플릿 카드·seed materialize | |
| CN-05 | STEP 1 검증 | projectId·projectName·scenarioName·targetEnv·purpose 필수 | cap-new-design §6 | 빈 값 저장 시 오류 메시지 | |
| CN-06 | STEP 2 모드 | 지점 기준 / 직접 입력, Session Timeout 프리셋 | HELP 05 §P2 | UI 라디오·직접 입력 전환 | |
| CN-07 | STEP 3 시나리오 | 프리셋 6종·사용자 정의·DR 프리셋·성능시험 기준 다중 선택 | capacity-design-standard §3.2, HELP 05 | STEP 3 표·operatingBaseline 지정 | |
| CN-08 | STEP 4~7 산정 | `CapNewDerivationService` 연동, 자동 계산 필드 채움 | cap-new-design §9 | 각 STEP 저장·derived 필드 확인 | |
| CN-09 | STEP 8 완료 | 저장 시 STATUS=COMPLETED, 단계별·시나리오별 비교표 | cap-new-design §9, HELP 05 | STEP 8 종합·트랙 전부 ✓ | |
| CN-10 | 트랙 상태 | ● ✓ ! × ○ — `stepTrack`·tooltip 이슈 | cap-new-design §17, HELP 05 | 저장/오류/경고 시 심볼 변화 | |
| CN-11 | 상단 컨텍스트 | 프로젝트·시나리오·환경·목적·상태·버전 카드 표시 | HELP 05 §P1 | wizard 상단 컨텍스트 바 가독성 | |
| CN-12 | 시나리오 비교 | `POST /compare`, compare.html 매트릭스·diffHighlights | cap-new-design §10 | 2개 이상 COMPLETED 시나리오 비교 | |
| CN-13 | 확정·버전 | approve/revoke/clone, `CAP_NEW_APPROVAL` 이력 | cap-new-design §11 | approved.html·PROD 검토자·CRITICAL override | |
| CN-14 | ENV handoff | `GET /env-handoff` → sessionStorage → ENV-002 prefill | cap-new-design §12, HELP 05 | STEP 8 ENV 연동 버튼·ENV-002 폼 반영 | |
| CN-15 | Excel export | 시나리오·비교·VM 대안 시트 다운로드 | cap-new-design §12·§15 | `.xlsx` 생성·시트 구성 | |
| CN-16 | VM 대안 비교 | 8C/16C/32C AP·Core·판단 문구 | cap-new-design §15 | STEP 8 VM 대안 버튼·API | |
| CN-17 | 트랙 클릭 이동 | 저장 완료 단계만 클릭 이동 | HELP 05 §P2 | 미완료 단계 클릭 차단 | |

---

## §5 ENV (ENV-001~004)

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| ENV-01 | 산정 선행 | ENV-002 「산정 실행」 없이 ENV-003/004 접근 시 안내 | HELP 03 | 산정 전 ENV-003 열기 | |
| ENV-02 | 플래너 연동 | `CapacityPlannerService`·`CapacityDesignService.analyze()` 결과 캐시 | capacity-design-standard §8, README | ENV-002 산정 실행·브라우저 저장값 | |
| ENV-03 | ENV-003 표 | 시나리오별 TPS·TPMC·VM·DB Pool, Core당 TPS 선정 | HELP 03 | ENV-003 Grid·가이드 요약 | |
| ENV-04 | ENV-004 Grid | UI→GSLB→L4→…→MyBatis 계층 탭, 권장 vs 현재 | HELP 03 | 계층별 탭·JVM 사이징 | |
| ENV-05 | cap-new Bridge | STEP 1~7 → `CapacityPlannerRequest` 변환 정확 | capacity-design-standard §8, cap-new-design §12 | handoff JSON·ENV-002 필드 1:1 대조 | |
| ENV-06 | 종합 보고서 | `/oc/check.html` — ENV + Rule 결론 요약 | HELP 01·03·04 | check.html 산정·Rule 후 요약 표시 | |
| ENV-07 | 설정 연결 | 산정 결과가 Grid·Timeout·Pool 매트릭스에 반영 | ztcf-book, capacity-design-standard §8 | ENV-004 권장값 출처 추적 | |

---

## §6 Rule 점검·운영 설정

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| R-01 | Rule 화면 | `/oc/rule-check.html` 업로드·점검 실행 | HELP 04 | 설정 파일 업로드·실행 | |
| R-02 | 필수 파일 | application.yml, Tomcat, mybatis-config.xml | HELP 04 | 3종 업로드·파싱 성공 | |
| R-03 | Rule Engine | THRESHOLD·RELATION 규칙 평가 | HELP 04, README | 점검 실행·위반 목록 | |
| R-04 | SC-007 대조 | 계층별 가이드 vs 현재값, 계층 그룹 표시 | HELP 04 | SC-007 표·그룹 헤더 | |
| R-05 | SC-008/009 | Timeout Map, 동시 요청 Flow 카드 | HELP 04 | 점검 후 카드 노출 | |
| R-06 | ENV 산정 반영 | 프로젝트 기준정보에 ENV-002 산정 반영 여부 확인 | HELP 04 §절차 1 | Rule 화면 기준정보 패널 | |
| R-07 | Tomcat·Hikari | maxThreads·accept-count·maximumPoolSize가 산정 대비 | capacity-design-standard §4·§9, TomcatHikariSizingGuide | Rule 결과 WARN/CRITICAL·실제 yml | |
| R-08 | DB Session 한도 | `totalDbSessions ≤ dbSessionLimit`(기본 800) | capacity-design-standard §3.1·§9 | STEP 7·Rule·운영 RDW 한도 | |

---

## §7 DR·센터·HA

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| DR-01 | 센터 모드 | ACTIVE_ACTIVE / DR_STANDBY / SINGLE별 AP 산정 규칙 적용 | capacity-design-standard §4 STEP5 | STEP 5 centerMode·AP 테이블 | |
| DR-02 | A-A 50:50 | 2센터 트래픽 분배, 센터당 `ceil(TPS/2/vmTps)+margin` | capacity-design-standard §4 STEP5 | STEP 5 per-center AP | |
| DR-03 | DR 전부하 | `drSingleCenterFullLoad=true` 시 failover AP 산정 | capacity-design-standard §3.1·§4 | DR_FAULT 템플릿·STEP 5 판정 | |
| DR-04 | 센터당 AP 여유 | `apMarginPerCenter`(+1 기본) 반영 | capacity-design-standard §3.1 | STEP 5 margin·totalDeploymentAp | |
| DR-05 | DR 판정 | CRITICAL: DR 전부하 시 단일 센터 TPS 초과 | capacity-design-standard §4·§9 | DR_FAULT·고TPS 시나리오 STEP 5 | |
| DR-06 | 장애 잔여 용량 | 설계 피크 대비 DR·페일오버 후 잔여 TPS·AP 여유 문서화 | capacity-design-standard §1·§4 | STEP 8 riskSummary·운영 Runbook | |

---

## §8 VM·인프라 프로파일

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| VM-01 | VM Profile 정의 | 4C/8C/16C/32C — Core·RAM·guideNominalTps·Hikari cap | capacity-design-standard §5 | `VmProfile` enum·STEP 4 선택 목록 | |
| VM-02 | 운영 표준 VM | cap-new 기본 16CORE-128GB | capacity-design-standard §3.1 | PROD_STANDARD·PEAK_OPS 템플릿 | |
| VM-03 | Scale-Out vs Up | 8C 다수 vs 16C 표준 vs 32C 집중 위험 판단 | cap-new-design §15 | vm-compare API·Excel VM 대안 시트 | |
| VM-04 | JVM 사이징 | `JvmSizingGuide.recommend(vmProfile)` Heap·GC·Stack | capacity-design-standard §4 STEP6 | STEP 6 JVM 권장표 | |
| VM-05 | Tomcat 상한 | VM별 maxThreads 권장 범위 내 | capacity-design-standard §5·§9 | STEP 6 vs VmProfile thread 범위 | |
| VM-06 | 가상화·운영 계수 | virtFactor·opsFactor·perfSafety 반영 시 AP 증가 합리 | capacity-design-standard §4 STEP4 | 보정 ON/OFF diff·문서 가정 기록 | |

---

## §9 시나리오 템플릿·프리셋

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| T-01 | PROD_STANDARD | DESIGN_PEAK·16C·~1,200 TPS·8 AP seed | capacity-design-standard §6 | 템플릿 선택 → Wizard STEP 1~7 prefill | |
| T-02 | PEAK_OPS | PEAK·16C·~600 TPS·4 AP, STEP6 baseline 일치 | capacity-design-standard §6 | 저장·검증 Pass | |
| T-03 | PERF_STRESS | STRESS·16C·~1,800 TPS·12 AP | capacity-design-standard §6 | 스트레스 ≥ 운영 검증 | |
| T-04 | DR_FAULT | DR_FAULT·16C·DR 전부하 AP | capacity-design-standard §6 | STEP 5 DR 판정 확인 | |
| T-05 | SCALE_8C | DESIGN_PEAK·8C·Scale-Out AP 증가 | capacity-design-standard §6 | vm-compare·AP 대수 | |
| T-06 | STG_SMALL | PEAK·4C·1,000지점·6,000명·~100 TPS | capacity-design-standard §3.3·§6 | 소형 환경 STEP 6 Pass | |
| T-07 | 템플릿 갱신 | 기동 시 builtin seed upsert, ENABLED/SORT_ORDER 유지 | cap-new-design (템플릿 구현) | 재기동 후 기존 커스텀 템플릿 보존 | |
| T-08 | TPS 프리셋 표 | NORMAL 3%/PEAK 5%/DESIGN 10%/STRESS 15%/DR 10% | capacity-design-standard §3.2 | 36,000명 기준 TPS 360/600/1200/1800 | |

---

## §10 문서·HELP·품질

| ID | 점검항목 | 합격 기준 | 근거 문서 | 확인 방법 | 결과 |
|----|----------|-----------|-----------|-----------|------|
| D-01 | HELP 인덱스 | `help-index.yml` cap-new·OC 화면 등록 | cap-new-design §13 | `:tcf-help:verifyHelp` Pass | |
| D-02 | 화면 오버라이드 | cap-new 4화면 docId·설계서 링크 | cap-new-design §13 | Wizard ?help= 또는 HELP 패널 | |
| D-03 | 카탈로그 | `catalog-overrides.yml`에 cap-new-design 등록 | cap-new-design §13 | HELP 카탈로그 검색 | |
| D-04 | 설계기준 문서 | capacity-design-standard 최신 (공식·템플릿·클래스맵) | capacity-design-standard §10~11 | 문서 버전·변경 이력 | |
| D-05 | ztcf-book 연계 | 57장 가이드·필드 매핑과 UI 필드 대응 | ztcf-book README, cap-field-mapping.js | 샘플 챕터·ENV Grid 필드명 | |
| D-06 | 구현 클래스 맵 | 설계 영역별 Java 클래스 존재·책임 일치 | capacity-design-standard §10 | 코드 리뷰·패키지 대조 | |
| D-07 | API 응답 래퍼 | `CapNewApiResponse { success, message, data }` | cap-new-design §4 | API 호출·에러 메시지 형식 | |
| D-08 | 판정 한글화 | NORMAL/WARN/CRITICAL → 정상/주의/위험/미확인 | HELP 05 | Wizard 배지·STEP 8 표 | |

---

## §11 판정 임계값 요약 (참조)

| 영역 | WARN | CRITICAL | 근거 |
|------|------|----------|------|
| AP/DR | 센터 용량 85% 근접 | DR 전부하 초과 | capacity-design-standard §9 |
| WAS Thread | 권장 maxThreads 근접 | Tomcat 상한 초과 | capacity-design-standard §9 |
| DB Pool | 이론 Pool > Thread 상한 | totalDbSessions > dbSessionLimit | capacity-design-standard §9 |
| WAR Pool | 비중 불균형 | warPoolTotalSessions 초과 | capacity-design-standard §9 |

---

## 점검 결과 요약 (작성용)

| 항목 | 값 |
|------|-----|
| 점검 일자 | |
| 점검자 | |
| 대상 환경 (DEV/STG/PROD) | |
| cap-new 시나리오 ID / 템플릿 | |
| 운영 기준 시나리오 코드 | |
| Pass / Fail / N/A 건수 | |
| 종합 의견 | |
| 후속 조치 | |

---

## 변경 이력

| 일자 | 내용 |
|------|------|
| 2026-07-12 | 최초 작성 — tcf-oc·cap-new·ENV·HELP·ztcf-book 통합 점검리스트 |
