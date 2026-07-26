# A18 — As-Built·Drift (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | A18 |
| 사용자 답변 | 범위=1 / 차이처리=1 기록·보류 / 판정=1 조건부 완료 |
| 확정사항 | 아래 Drift 표 · 판정문 |
| 가정·미확정 | OM·POST·포트 yml 교차·ztomcat 전수 = 미실행 |
| 설계 영향 | 정의서 Baseline 유지 · 문서≠운영완료 · AA 승인 전 운영 금지 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | 시리즈 종료 (재검증 선택) |

---

## 검증 범위

구조·명명·모듈·ServiceId·금지패턴 + 런타임 클래스명 정합 (범위1)

## Drift 표 `[실제 소스 확인]`

| ID | 항목 | 결과 | 근거 |
| --- | --- | --- | --- |
| D1 | `OnlineTransactionController` `/online`, `/{businessCode}/online` | 성공 | `tcf-web` |
| D2 | `OnlineTransactionTimeoutExecutor` + TCF 위임 | 성공 | `tcf-core` |
| D3 | `TransactionDispatcher` | 성공 | `tcf-core` |
| D4 | av/ln `entry`·`application`·`persistence` | 성공 | 패키지 |
| D5 | av/ln 업무 거래 Controller 없음 | 성공 | `@RestController` 없음 |
| D6 | `AV.Sample.inquiry` | 성공 | `AvSampleHandler` |
| D7 | `LN.CustomerContact.selectList/Detail` | 성공 | `LnCustomerContactHandler` |
| D8 | `settings.gradle` include av/ln | 성공 | |
| D9 | 포트 AV=8101·LN=8103 정의 | 성공 | `BusinessModuleDefinitions` |
| D10 | bootRun `context-path: /` | 성공 | av/ln As-Is |
| — | OM·샘플 POST·bootRun 포트 yml 교차·ztomcat/zarchitecture | **미실행** | 범위 밖 |
| R1 | `sv-service` `SvOnlineTransactionController` | 참고·범위외·보류 | 시정 안 함 |

## 위험·시정

| 항목 | 처리 |
| --- | --- |
| 범위1 실패 | 없음 |
| R1 | 기록만·보류 |
| Open Issue (A16) | 유지 Explicit |

## 판정문

**범위1 정합 — A18 조건부 완료.**  
정의서 vs 소스(진입·Timeout 실명·계층·ServiceId·금지·모듈) 일치.  
미실행(OM·POST·문서전수) ≠ 성공. 운영 반영은 AA 승인 후.

## Gate

- [x] Drift 표에 성공/실패/미실행
- [x] 최종 판정문
