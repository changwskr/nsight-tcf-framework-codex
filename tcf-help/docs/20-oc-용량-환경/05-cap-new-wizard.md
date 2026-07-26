---
id: cap-new-wizard
title: NEW 용량산정 Wizard
section: oc
order: 25
screens:
  - /oc/cap-new/index.html
  - /oc/cap-new/wizard.html
  - /oc/cap-new/compare.html
  - /oc/cap-new/approved.html
---

# NEW 용량산정 (cap-new)

기존 [용량 산정 (CAP)](/oc/capacity.html)과 **분리된** 8단계 Wizard입니다. API prefix는 `/api/oc/cap-new`이며 `tcf-oc`의 `com.nh.nsight.marketing.oc.capnew` 패키지에서 처리합니다.

## 화면 맵

| 화면 | 경로 | 역할 |
|------|------|------|
| 목록 | [/oc/cap-new/index.html](/oc/cap-new/index.html) | 시나리오 목록·신규 생성 |
| Wizard | [/oc/cap-new/wizard.html](/oc/cap-new/wizard.html) | STEP 1~8 입력·산정 |
| 비교 | [/oc/cap-new/compare.html](/oc/cap-new/compare.html) | 완료 시나리오 지표 비교 |
| 확정 | [/oc/cap-new/approved.html](/oc/cap-new/approved.html) | 검토·확정·버전 복제 |

## Wizard 단계

| STEP | 내용 |
|------|------|
| 1 | 프로젝트·시나리오 기본정보 |
| 2 | 사용자·세션 (지점/본부/기타) |
| 3 | TPS 시나리오·운영 기준 지정 |
| 4 | VM·Core TPS·설계 피크 TPS |
| 5 | AP·DR·센터 모드 |
| 6 | WAS Thread·JVM |
| 7 | DB Pool·DB Session (WAR별 Pool 배분 검증) |
| 8 | 종합 결과·비교·확정 |

STEP 2~7 저장 시 이미 완료된 하위 단계는 **자동 재산정**됩니다 (`cascadeImpact` 응답 참고).

## 단계 트랙 상태

| 표시 | 의미 |
|------|------|
| ● | 현재 편집 중인 단계 |
| ✓ | 저장·검증 완료 |
| ! | 저장됐으나 주의(경고·위험 판정) |
| × | 미입력 또는 검증 오류 |
| ○ | 아직 도달하지 않은 단계 |

상태는 `GET/PUT /scenarios/{id}` 응답의 `stepTrack` 배열로 계산됩니다. 항목에 마우스를 올리면 오류·주의 메시지를 확인할 수 있습니다.

## Wizard 상단 컨텍스트 (P1)

트랙 위 **프로젝트·시나리오·환경·목적·상태·버전** 바가 항상 표시됩니다.

## STEP 8 종합 화면 (P1)

- **단계별 결과표** — STEP 1~7 요약·판정·[보기] 이동
- **시나리오별 비교표** — 활성 TPS 시나리오 AP·Thread·Pool (운영 기준은 저장값)

## 판정 표기

화면 판정은 **정상 / 주의 / 위험 / 미확인** 한글 배지로 표시합니다 (`NORMAL`·`WARN`·`CRITICAL` 매핑).

## P2 UX (입력·탐색)

- **트랙 클릭** — 저장 완료된 단계만 클릭 이동
- **STEP 2** — 지점 기준 / 직접 입력, Session Timeout 60·90·직접
- **STEP 4** — 업무·VM **표 선택** (라디오)
- **STEP 6** — Tomcat Connector·JVM **권장값 표** (minSpare, acceptCount 등)

## P3 UX (시나리오·검토)

- **STEP 1** — [기존 시나리오 불러오기]·[초기화]
- **STEP 3** — 사용자 정의 시나리오 추가·삭제, DR 장애 프리셋, **성능시험 기준** 다중 선택 (`performanceTestTargets`)
- **STEP 8** — [시나리오 복사] (COMPLETED/APPROVED), [검토 요청] → 확정 화면

## STEP 8 주요 기능

- **시나리오 비교** — 다른 완료 시나리오와 지표 매트릭스 비교
- **ENV 연동** — `GET /scenarios/{id}/env-handoff` → [ENV-002](/oc/env-002.html) 폼 prefill
- **Excel export** — 시나리오·비교 결과 `.xlsx` 다운로드
- **기존 CAP 대조** — `GET /scenarios/{id}/legacy-compare`로 기존 `ASMSC71001` 산정과 운영 기준 지표 비교
- **VM 대안 비교** — `GET /scenarios/{id}/vm-compare`로 8C/16C/32C VM TPS·AP·Core 비교

## 기존 CAP 대조 시 유의

- 기존 CAP은 `지점 수 × 지점당 사용자`만 사용자 수로 씁니다. cap-new의 본부·기타 사용자는 **전체 사용자 수로 등가 변환** 후 대조합니다.
- cap-new AP 산정은 센터별 여유·DR 페일오버 규칙이 포함되어 기존 CAP과 AP 대수가 다를 수 있습니다.
- VM 보정 계수(가상화·운영효율)를 적용한 경우 TPS·AP 비교에 차이가 날 수 있습니다.

## 다음 단계

- 확정(APPROVED) 후 ENV-002에서 환경 점검
- [Rule 점검](/oc/rule-check.html)으로 Timeout·자원 고갈 흐름 검토

## 심화 설계

- `tcf-oc/docs/cap-new-design.md`
- `tcf-oc/README.md`
