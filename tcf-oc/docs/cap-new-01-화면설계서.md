# NSIGHT TCF-OC NEW 용량산정 화면설계서

| 항목 | 내용 |
|------|------|
| **문서 ID** | OC-CAP-NEW-SCR-001 |
| **버전** | V1.0 |
| **작성일** | 2026-07-15 |
| **대상 모듈** | tcf-oc / tcf-ui |
| **관련 문서** | `cap-new-design.md`, `cap-new-02-거래설계서.md` |
| **문서 목적** | 8단계 Wizard 기반 용량산정 화면의 UI 구조, 입력·출력 항목, 검증·판정 규칙, 화면 간 이동을 정의한다. |

---

## 1. 도입

### 1.1 설계 배경

용량산정 화면은 한 화면에 모든 입력값을 나열하는 방식보다, 사용자가 다음 질문에 **순차적으로 답하도록** 설계한다.

1. 누가 얼마나 사용하는가?
2. 피크 시간에 몇 명이 동시에 요청하는가?
3. 목표 응답시간은 얼마인가?
4. 업무 한 건이 얼마나 무거운가?
5. 어떤 VM을 사용할 것인가?
6. 몇 대의 AP가 필요한가?
7. Tomcat Thread는 몇 개가 필요한가?
8. DB Connection Pool은 몇 개가 필요한가?
9. 한 센터 장애 시에도 처리 가능한가?

기존 `capacity.html`(3단계)과 ENV-002/003은 본 설계의 STEP 1~8 흐름으로 **통합**한다. cap-new는 기존 CAP API(`/api/oc/capacity`)와 **완전 분리**된 신규 메뉴이다.

### 1.2 적용 대상

| 구분 | 내용 |
|------|------|
| 플랫폼 | NSIGHT 마케팅플랫폼 |
| 업무 모듈 | tcf-oc (Operations Center) |
| UI 경로 | `/oc/cap-new/*.html` |
| API 경로 | `/api/oc/cap-new/*` |
| 기준 전제 | 6,000지점 × 6명 = 36,000명, 세션 여유율 30%, 설계 피크 10%/3초 |

### 1.3 화면 목록

| 화면 ID | 화면명 | URL | 목적 |
|---------|--------|-----|------|
| CAPN-001 | 시나리오 목록 | `/oc/cap-new/index.html` | 산정 시나리오 조회·신규 시작·템플릿 선택 |
| CAPN-002 | 8단계 Wizard | `/oc/cap-new/wizard.html` | STEP 1~8 순차 입력·산정·저장 |
| CAPN-003 | 시나리오 비교 | `/oc/cap-new/compare.html` | 2개 이상 시나리오 지표 비교 |
| CAPN-004 | 승인·확정 | `/oc/cap-new/approved.html` | 확정·검토·이력 조회 |

---

## 2. 전체 화면 구조

### 2.1 공통 레이아웃

```
┌────────────────────────────────────────────────────────────────────┐
│ NSIGHT TCF-OC 용량산정                                             │
│ 프로젝트: NSIGHT 마케팅플랫폼   환경: 운영   상태: 작성 중         │
├────────────────────────────────────────────────────────────────────┤
│ ① 기본정보 ─ ② 사용자 ─ ③ TPS ─ ④ VM ─ ⑤ AP/DR ─ ⑥ WAS ─ ⑦ DB ─ ⑧ 결과 │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│                         단계별 입력 영역                           │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│ [이전]       [임시저장]                      [다음 단계]            │
└────────────────────────────────────────────────────────────────────┘
```

### 2.2 단계 트랙 상태

| 표시 | state | 의미 |
|------|-------|------|
| ● | active | 현재 단계 (클라이언트 오버레이) |
| ✓ | done | 저장·검증 통과 |
| ! | warn | 경고 또는 STEP 5~8 위험 판정 |
| × | error | 미입력·검증 오류 |
| ○ | pending | 미진행 단계 |

예: `✓ 기본정보 ─ ✓ 사용자 ─ ● TPS ─ ○ VM ─ ○ AP/DR ─ ○ WAS ─ ○ DB ─ ○ 결과`

- 저장된 단계(✓/!/×) 클릭 시 해당 STEP으로 이동 가능
- `stepTrack[]` API 응답 기준으로 렌더링 (`oc-cap-new-wizard.js`)

### 2.3 상단 컨텍스트 바

프로젝트 ID·시나리오명·대상 환경·산정 목적·상태(DRAFT/COMPLETED/APPROVED)·버전을 항상 표시한다.

### 2.4 공통 버튼

| 버튼 | 동작 | API |
|------|------|-----|
| 이전 | 이전 STEP 이동 (저장 없음) | — |
| 임시저장 | 현재 STEP 저장·검증·자동계산 | `PUT /scenarios/{id}/step/{n}` |
| 다음 단계 | 저장 후 다음 STEP 이동 | `PUT /scenarios/{id}/step/{n}` |

---

## 3. STEP별 화면 상세

### 3.1 STEP 1 — 프로젝트 기본정보 (CAPN-002-S1)

#### 화면 목업

```
┌────────────────────────────────────────────────────────────────────┐
│ STEP 1. 프로젝트 기본정보                                          │
├────────────────────────────────────────────────────────────────────┤
│ 프로젝트 ID *       [ NSIGHT-MP                         ]           │
│ 프로젝트명 *        [ NSIGHT 마케팅플랫폼               ]           │
│ 산정 시나리오명 *   [ 2026 운영용량 기준안               ]           │
│ 대상 환경 *         [ 운영 PROD                       ▼ ]           │
│ 기준일              [ 2026-07-12                        ]           │
│ 산정 버전           [ V1.0                              ]           │
│ 작성자              [ 홍길동                            ]           │
│ 설명                [ 6,000지점 운영 기준 용량산정       ]           │
├────────────────────────────────────────────────────────────────────┤
│ 산정 목적                                                          │
│ (●) 신규 구축   ( ) 증설 검토   ( ) 설정 점검   ( ) DR 검증        │
├────────────────────────────────────────────────────────────────────┤
│ [기존 시나리오 불러오기] [초기화]                  [다음: 사용자 조건]│
└────────────────────────────────────────────────────────────────────┘
```

#### 입력 항목

| 항목 | 필드명 | 필수 | 기본값 | 검증 |
|------|--------|------|--------|------|
| 프로젝트 ID | projectId | Y | NSIGHT-MP | 영문·숫자·하이픈 |
| 프로젝트명 | projectName | Y | — | 200자 이내 |
| 시나리오명 | scenarioName | Y | — | 중복 시 버전 안내 |
| 대상 환경 | targetEnv | Y | PROD | DEV/STG/PROD/DR |
| 기준일 | baseDate | Y | 오늘 | DATE 형식 |
| 산정 버전 | versionNo | N | V1.0 | — |
| 작성자 | author | N | — | — |
| 설명 | description | N | — | 500자 이내 |
| 산정 목적 | purpose | Y | NEW_BUILD | NEW_BUILD/SCALE_REVIEW/CONFIG_CHECK/DR_VERIFY |

#### 부가 기능

| 기능 | 설명 |
|------|------|
| 기존 시나리오 불러오기 | `GET /scenarios` 목록 선택 → STEP 1 필드 적용 |
| 초기화 | `GET /defaults` → `defaults.step1` 기본값 리셋 |
| 템플릿 기반 생성 | `POST /scenarios` + `templateCode` → seed payload 적용 |

#### 다음 단계 이동 조건

프로젝트 ID·프로젝트명·시나리오명·대상 환경·산정 목적 입력 완료

---

### 3.2 STEP 2 — 사용자·세션 조건 (CAPN-002-S2)

#### 입력 항목

| 항목 | 필드명 | 기본값 | 검증 |
|------|--------|--------|------|
| 산정 방식 | calcMode | BRANCH | BRANCH / DIRECT |
| 지점 수 | branchCount | 6,000 | ≥ 1 |
| 지점당 사용자 | userPerBranch | 6 | ≥ 1 |
| 본부·센터 사용자 | hqUsers | 0 | ≥ 0 |
| 기타 사용자 | otherUsers | 0 | ≥ 0 |
| 세션 여유율 | sessionMarginRate | 0.30 | 0~100% |
| Session Timeout | sessionTimeoutMin | 60 | 60/90/직접입력 |

#### 자동 계산

```
전체 사용자 = 지점 수 × 지점당 사용자 + 본부 사용자 + 기타 사용자
설계 세션 = ceil(전체 사용자 × (1 + 세션 여유율))
```

#### 실시간 요약 패널

전체 사용자, 설계 세션, 세션 Timeout, 세션 여유 개수를 하단에 표시한다.

---

### 3.3 STEP 3 — 동시요청·TPS 시나리오 (CAPN-002-S3)

#### 표준 시나리오 프리셋

| 시나리오 코드 | 라벨 | 동시요청률 | 응답시간 | 목적 |
|---------------|------|-----------|---------|------|
| NORMAL | 평시 | 3% | 3초 | 일반 업무시간 |
| PEAK | 정상 피크 | 5% | 3초 | 일반적인 피크 |
| DESIGN_PEAK | 설계 피크 | 10% | 3초 | 운영 용량 기준 |
| STRESS | 스트레스 | 15% | 3초 | 한계 성능 검증 |
| SLOW_RESPONSE | 응답지연 | 10% | 5초 | 느린 SQL·외부 연계 |
| DR_FAULT | DR 장애 | 10% | 3초 | 한 센터 전체 처리 |
| CUSTOM_n | 사용자 정의 | 직접 | 직접 | 특수 상황 |

#### 자동 계산

```
실요청자 = 전체 사용자 × 동시요청률
목표 TPS = ceil(실요청자 ÷ 목표 응답시간)
```

#### 추가 입력

| 항목 | 필드명 | 설명 |
|------|--------|------|
| 운영 기준 시나리오 | operatingBaseline | 반드시 1개 지정 (라디오) |
| 성능시험 기준 | performanceTestTargets[] | 활성 시나리오 다중 체크 |

#### 검증

- 최소 1개 시나리오 활성
- 운영 기준 시나리오 1개 지정
- 스트레스 TPS ≥ 운영 기준 TPS
- 동시요청률 100% 초과 시 경고

---

### 3.4 STEP 4 — 업무복잡도·CPU·VM (CAPN-002-S4)

#### 업무 유형 선택표

| 코드 | 업무 유형 | TPMC/TPS | TPS/Core | 설명 |
|------|----------|----------|----------|------|
| CACHE_LOOKUP | 단순 캐시 조회 | 1,500 | 71 | DB 부하 낮음 |
| SIMPLE_INQ | 일반 단건 조회 | 2,000 | 53 | 단건 SQL |
| SINGLE_VIEW | SingleView 조회 | 3,000 | 36 | 다중 조회 (기본) |
| COMPLEX_CUST | 복합 고객 조회 | 4,000 | 27 | 다중 조인 |
| CHANGE_EXT | 변경·대외 연계 | 5,000 | 21 | 원장·승인 |
| CUSTOM | 직접 입력 | — | — | 사용자 정의 |

#### VM Profile 선택표

| 코드 | VM 사양 | Core | Memory | VM 기준 TPS | 용도 |
|------|---------|------|--------|------------|------|
| 8CORE-64GB | 8C/64GB | 8 | 64GB | 288 | 개발·소형 |
| 16CORE-128GB | 16C/128GB | 16 | 128GB | 576 | 운영 표준 (기본) |
| 32CORE-256GB | 32C/256GB | 32 | 256GB | 1,152 | 대형·특수 |
| CUSTOM | 직접 입력 | — | — | 자동계산 | 사용자 정의 |

#### 보정 계수

| 항목 | 필드명 | 기본값 |
|------|--------|--------|
| CPU 목표 사용률 | cpuTargetUtilization | 0.70 |
| 성능 안전계수 | perfSafetyFactor | 1.20 |
| 가상화 보정계수 | virtualizationFactor | 0.90 |
| 운영 효율 보정 | opsEfficiencyFactor | 0.85 |

#### 산출 결과 패널

이론 VM TPS, 운영 보정 TPS, 설계 피크 TPS, 최소 필요 Core

---

### 3.5 STEP 5 — AP 대수·센터·DR (CAPN-002-S5)

#### 입력 항목

| 항목 | 필드명 | 기본값 |
|------|--------|--------|
| 센터 구성 | centerMode | ACTIVE_ACTIVE |
| 정상 트래픽 분배 | trafficSplit | 50:50 |
| 한 센터 장애 시 100% 수용 | drFullCapacity | true |
| AP 대수 여유 | apSpareCount | 1 |
| 최소 센터당 AP | minApPerCenter | 2 |

#### 시나리오별 AP·DR 판정표

| 시나리오 | 목표TPS | 단일센터필요 | 정상 센터 | 장애센터 | 판정 |
|---------|---------|-------------|----------|---------|------|
| (자동 생성) | — | — | — | — | 정상/주의/위험 |

#### DR 판정 기준

| 판정 | 조건 |
|------|------|
| 정상 | 한 센터 AP만으로 목표 TPS 처리 가능 |
| 주의 | 정상 피크는 처리, 스트레스 부족 |
| 위험 | 설계 피크를 한 센터에서 처리 불가 |

---

### 3.6 STEP 6 — WAS Thread·JVM (CAPN-002-S6)

#### 입력 항목

| 항목 | 필드명 | 기본값 |
|------|--------|--------|
| 기준 시나리오 | baselineScenario | operatingBaseline |
| 평균 Thread 점유시간 | avgThreadHoldSec | 1.2초 |
| Thread 산정 여유율 | threadMarginRate | 1.20 |
| maxThreads 추가 배율 | maxThreadMarginRate | 1.30 |

#### 산출 결과

| 항목 | 산식 |
|------|------|
| 총 필요 Thread | TPS × 평균 점유시간 × 여유율 |
| AP당 필요 Thread | 총 Thread ÷ 배포 AP |
| 권장 maxThreads | AP당 Thread × 배율 (올림) |
| minSpareThreads | maxThreads × 20~25% |
| acceptCount | maxThreads × 40~60% |
| maxConnections | 10,000 |

#### JVM 권장값 표

VM Memory, JVM Xms/Xmx, GC, MaxGCPauseMillis, Thread Stack, Heap 목표 사용률

---

### 3.7 STEP 7 — DB Pool·DB Session (CAPN-002-S7)

#### 입력 항목

| 항목 | 필드명 | 기본값 |
|------|--------|--------|
| AP 유형 | apType | SINGLE_VIEW |
| 평균 DB Connection 점유시간 | avgDbHoldSec | 0.20초 |
| DB 사용 거래 비율 | dbUsageRate | 100% |
| Pool 안전계수 | poolSafetyFactor | 1.30 |
| Thread→DB 사용비율 | threadToDbRatio | 30% |
| 운영 최소 Pool/VM | minPoolPerVm | 30 |
| DB 전체 Session 한도 | dbSessionLimit | 800 |

#### Pool 계산 단계 표시

① AP당 TPS → ② TPS기준 Pool → ③ Thread기준 상한 → ④ 운영 최소 → ⑤ 최종 Pool/VM → ⑥ 전체 DB Session

#### WAR별 Pool 배분 (warPoolEnabled=true)

| 업무 WAR | 비중 | Pool/VM | AP 대수 | 전체Pool | 판정 |
|---------|------|---------|---------|---------|------|
| SV | 40% | — | — | — | — |
| IC | 25% | — | — | — | — |
| MG | 20% | — | — | — | — |
| 기타 | 15% | — | — | — | — |
| **합계** | 100% | — | — | — | 정상/위험 |

한도 초과 시 권장 조치 4건 제시 (재배분, AP 재계산, DB 분리, Session 증설)

---

### 3.8 STEP 8 — 종합 결과·비교·확정 (CAPN-002-S8)

#### 종합 결론 카드

프로젝트, 기준 시나리오, VM Profile, 종합 판정(정상/주의/위험), 주요 사유

| 전체 사용자 | 설계 세션 | 피크 TPS | 센터당 AP | 전체 AP |
| maxThreads | JVM Heap | DB Pool/VM | DB Session | DR 판정 |

#### 단계별 결과표

STEP 1~7 산정 항목·결과·판정·[보기] 단계 이동 링크

#### 시나리오 비교표

평시·정상 피크·설계 피크·스트레스·DR 장애별 TPS·AP·Thread·Pool

#### 하단 액션 버튼

| 버튼 | 동작 | API/이동 |
|------|------|----------|
| 이전 단계 | STEP 7 이동 | — |
| 조건 수정 | 해당 STEP 이동 | — |
| 시나리오 복사 | 새 버전 DRAFT 생성 | `POST /scenarios/{id}/clone` |
| VM 대안 비교 | 모달/패널 | `GET /scenarios/{id}/vm-compare` |
| 기존 CAP 대조 | 모달/패널 | `GET /scenarios/{id}/legacy-compare` |
| ENV 연동 | ENV-002 prefill | `GET /scenarios/{id}/env-handoff` |
| Excel 다운로드 | 파일 다운로드 | `POST /scenarios/{id}/export/excel` |
| 검토 요청 | approved.html 이동 | `/oc/cap-new/approved.html?id=` |
| 최종 확정 | COMPLETED → APPROVED | `POST /scenarios/{id}/approve` |

---

## 4. 보조 화면

### 4.1 CAPN-001 시나리오 목록

| 기능 | 설명 |
|------|------|
| 목록 조회 | 상태별 필터 (DRAFT/COMPLETED/APPROVED) |
| 신규 산정 | Wizard STEP 1 이동 |
| 템플릿 선택 | `GET /templates` → seed 기반 생성 |
| 시나리오 열기 | Wizard 해당 STEP 이동 |
| 삭제 | DRAFT만 `DELETE /scenarios/{id}` |

### 4.2 CAPN-003 시나리오 비교

- 2개 이상 COMPLETED/APPROVED 시나리오 체크박스 선택
- 기준 시나리오 지정
- 지표 매트릭스: TPS, AP, Thread, Pool, DB Session, DR 판정
- 차이 요약(`diffHighlights`)·운영 권장안(`recommendation`)
- Excel 비교 출력: `POST /export/excel`

### 4.3 CAPN-004 승인·확정

- 확정 요청: approver, reviewer(운영 PROD 필수), approvalNote
- CRITICAL 판정 시 override 사유 필수
- 확정 취소: `POST /scenarios/{id}/revoke`
- 확정 이력: `GET /approvals`

### 4.4 VM 대안 비교 (STEP 8 내장)

8C/16C/32C Profile별 VM TPS·필요 AP·전체 Core·장애범위·판단 비교

---

## 5. 입력값 변경 영향 (연쇄 재산정)

| 저장 STEP | 재산정 대상 |
|-----------|------------|
| 2 | 3 → 4 → 5 → 6 → 7 → 8 |
| 3 | 4 → 5 → 6 → 7 → 8 |
| 4 | 5 → 6 → 7 → 8 |
| 5 | 6 → 7 → 8 |
| 6 | 7 → 8 |
| 7 | 8 |

화면 메시지 예:

> VM Profile이 16C/128GB에서 8C/64GB로 변경되었습니다.  
> 영향받는 항목: 필요 AP, 센터당 AP, AP당 TPS, maxThreads, DB Pool, 전체 DB Session  
> [전체 다시 산정]

노란색 **하위 단계 자동 재산정** 패널로 `cascadeImpact` 표시

---

## 6. 판정 기준

### 6.1 등급 정의

| 등급 | 표시 | 의미 |
|------|------|------|
| 정상 | NORMAL | 권장범위 이내 |
| 주의 | WARN | 운영 가능, 성능시험·조정 필요 |
| 위험 | CRITICAL | 운영 기준 미충족 |
| 미확인 | UNKNOWN | 필수 입력·설정 미수집 |

### 6.2 대표 판정 임계치

| 항목 | 정상 | 주의 | 위험 |
|------|------|------|------|
| AP 여유 | ≥ 20% | 10~20% | < 10% |
| CPU 목표 | ≤ 70% | 70~85% | > 85% |
| Busy Thread | ≤ 70% | 70~85% | > 85% |
| Heap | ≤ 70% | 70~80% | > 80% |
| DB Pool 사용률 | ≤ 70% | 70~85% | > 85% |
| DB Session | 한도 80% 이하 | 80~100% | 한도 초과 |
| DR 처리량 | 피크 수용 | 정상 피크만 | 정상 피크 미수용 |

경고(!)는 다음 단계 이동 허용. 위험(×)은 사유 입력 또는 조건 수정 후 최종 확정 가능.

---

## 7. 메뉴 구성

```
NSIGHT TCF-OC
├─ 용량산정
│   ├─ NEW 용량산정 (cap-new)     ← 본 설계 대상
│   ├─ (기존) 용량산정             ← capacity.html 유지
│   ├─ 산정 시나리오 목록
│   ├─ 시나리오 비교
│   └─ 승인된 기준안
├─ 환경설정 점검
└─ 기준정보
```

---

## 8. UI 자산

| 파일 | 역할 |
|------|------|
| `tcf-ui/.../oc/cap-new/index.html` | 목록 |
| `tcf-ui/.../oc/cap-new/wizard.html` | Wizard |
| `tcf-ui/.../oc/cap-new/compare.html` | 비교 |
| `tcf-ui/.../oc/cap-new/approved.html` | 확정 |
| `oc-cap-new-wizard.js` | Wizard 로직·트랙·연쇄 |
| `oc-cap-new-export.js` | Excel 다운로드 |
| `oc-cap-new-env-handoff.js` | ENV-002 연동 |
| `oc-cap-new-legacy-compare.js` | 기존 CAP 대조 |
| `oc-cap-new-vm-compare.js` | VM 대안 비교 |

---

## 9. 화면 개발 체크리스트

| 구분 | 체크 항목 | 완료 기준 |
|------|----------|----------|
| 입력 | 8단계 순차 이동 | 트랙 상태·이전/다음 동작 |
| 입력 | 실시간 자동계산 | STEP 2~3 입력 즉시 갱신 |
| 산정 | STEP 4~7 엔진 연동 | 보정 TPS·AP·Thread·Pool 산출 |
| 연쇄 | 하위 STEP 재산정 | cascadeImpact 패널 표시 |
| 판정 | 정상/주의/위험 배지 | 한글화·색상 구분 |
| 출력 | Excel·ENV·비교 | 각 버튼 API 연동 |
| 확정 | PROD 검토자 분리 | 운영 환경 확정 규칙 |

---

## 10. 마무리

본 화면의 최종 목적은 단순 숫자 출력이 아니라 **아키텍처 의사결정 지원**이다.

```
조건 → 계산 → 비교 → 위험판정 → 권장안 → 승인
```

몇 명이 사용하는가 → 몇 TPS가 필요한가 → 어떤 VM을 몇 대 배치해야 하는가 → Tomcat Thread는 몇 개인가 → DB Pool은 몇 개인가 → 한 센터 장애 시에도 처리할 수 있는가.

이 흐름을 8단계 Wizard로 구현하여 운영자가 복잡한 산식을 직접 계산하지 않아도 용량 기준안을 수립·비교·확정할 수 있도록 한다.
