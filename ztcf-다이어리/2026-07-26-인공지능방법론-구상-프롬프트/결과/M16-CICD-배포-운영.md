# M16 — CI/CD·배포·운영 연결 (확정 / 일부 미확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | M16 |
| 사용자 답변 | Git반영=⑤ 미결정 → 최소고정=① |
| 확정사항 | Rollback=코드+DB+OM / Gateway신규 제외 / 배포 도구 개념=`ztomcat`·`tcf-scripts` |
| 미확정사항 | Commit vs PR vs 자동PR, CI YAML 상세, 운영 관찰기간 일수 |
| 설계 영향 | M18 통합 시 Git 연계는 미확정으로 명시. 배포·롤백 원칙만 기준서에 실음 |
| 산출물 반영 | 본 문서 |
| 다음 단계 | M17 — 선도개발·확산 |
| 선행 | [`M0`](./M0-방법론-수립범위.md), [`M15`](./M15-테스트-Quality-Gate.md) |

---

## 1. Git 연계방안

| 항목 | 상태 |
| --- | --- |
| 반영 방식 | **미확정** (①개발자 Commit / ②생성 Branch+PR / ③자동PR 미선택) |
| 전제 | AA 승인 전 코드 “사용·완료” 금지 (M13·M4) |

## 2. CI/CD Pipeline (개념)

| 단계 | 내용 | 상태 |
| --- | --- | --- |
| 빌드 | `gradle` / `tcf-scripts` build | 기존 저장소 활용 |
| 자동검증 | M15 ①~③ | 목표 |
| 배포 | `ztomcat` deploy / `verify-deploy` | 개념 채택 |
| 신규 YAML | 만들지 않음 | 확정 |

## 3. 배포·롤백 절차 (고정)

**배포 (1차)**

1. AA 승인·증적 확인  
2. `av.war` 빌드  
3. ztomcat(또는 동등) 배포  
4. health 확인 (`/av/actuator/health` — 스크립트 등록 여부는 Gap)  
5. 표준 거래 스모크  

**Rollback — 코드만으로 부족**

| 대상 | 이유 |
| --- | --- |
| DB 스키마/데이터 | CUD 후 구조·데이터 잔존 |
| OM Catalog·Timeout | 신규 ServiceId 행 잔존 |
| Gateway Route | 1차 제외이나 향후 해당 시 포함 |

## 4. OM 연계방안

- CRUD ServiceId OM 등록은 완료 증적 후보 (M3에서 OM은 Gap → 해소 과제)  
- AA 승인 대상 (M4)

## 5. 운영전환 체크리스트 (최소)

- [ ] Git 반영 방식 결정 (미확정 해소)  
- [ ] WAR 배포·health  
- [ ] OM·Timeout (해당 시)  
- [ ] 거래로그 GUID 추적  
- [ ] Rollback 계획(DB·OM 포함)  
- [ ] Gateway 신규 없음 확인 (M0)
