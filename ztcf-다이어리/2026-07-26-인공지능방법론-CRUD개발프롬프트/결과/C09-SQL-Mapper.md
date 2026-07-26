# C09 — SQL·Mapper (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | C09 |
| 사용자 답변 | 목록조건=1 / 상세=1 / Mapper정합=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | 인덱스 DDL 사람 승인; H2 vs 운영 SQL 방언 |
| 설계 영향 | Mapper XML 3 select. schema.sql 시범 테이블·인덱스 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | C10 — 트랜잭션·오류·Timeout |

---

## 목록

| 항목 | 내용 |
| --- | --- |
| id | `searchContacts` / `countContacts` |
| WHERE | CUSTOMER_NO= / CONTACT_TYPE=(동적) / USE_YN='Y' 고정 |
| ORDER BY | UPD_DTM DESC, CONTACT_ID DESC (고정) |
| 페이징 | OFFSET/FETCH + count |

## 상세

| 항목 | 내용 |
| --- | --- |
| id | `selectByContactId` |
| WHERE | CONTACT_ID = (PK) |
| USE_YN | 필터 없음 |

## CUD

없음 (LN SELECT만). UPDATE/DELETE 안전 Gate = N/A.

## Mapper 정합 `[설계 예시]`

| 항목 | 값 |
| --- | --- |
| Interface | `com.nh.nsight.marketing.ln.persistence.mapper.LnCustomerContactMapper` |
| XML | `mapper/ln/LnCustomerContactMapper.xml` |
| 연결 | ServiceId → Service → Dao → Mapper id |

안전: SELECT * 금지 / 동적 ORDER BY 금지.

## 인덱스 `[설계 예시·승인 전]`

- PK: CONTACT_ID  
- `(CUSTOMER_NO, USE_YN, CONTACT_TYPE)`

## Gate

- [x] Service↔SQL 연결
- [x] 테이블 정의 부합
- [x] UPDATE/DELETE 안전 (해당 없음)
