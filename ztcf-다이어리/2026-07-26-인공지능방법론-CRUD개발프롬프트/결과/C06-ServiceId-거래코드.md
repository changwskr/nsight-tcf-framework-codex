# C06 — ServiceId·거래코드 (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | C06 |
| 사용자 답변 | 형식=1 / 행위명=1 selectList·selectDetail / 거래코드=1 LN-INQ / OM=1 2건등록 |
| 확정사항 | 아래 표 |
| 가정·미확정 | 거래코드 공식 채번; Timeout 수치(초) |
| 설계 영향 | Handler `serviceIds()` 2건, tcf-ui TX, OM Catalog·Timeout 초안 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | C07 — 요청·응답 DTO |

---

## ServiceId 표

| 이벤트 | ServiceId | 거래코드 | processingType | OM |
| --- | --- | --- | --- | --- |
| E01 | `LN.CustomerContact.selectList` | `LN-INQ-0001` | INQUIRY | Catalog + Timeout |
| E02 | `LN.CustomerContact.selectDetail` | `LN-INQ-0002` | INQUIRY | Catalog + Timeout |

형식: `{업무코드}.{도메인}.{행위}`  
CUD ServiceId: **없음**

Handler(목표명 `[설계 예시]`): `LnCustomerContactHandler` — 도메인 1 Handler에 위 2건.

## Gate

- [x] 이벤트↔ServiceId
- [x] 중복 없음 (신규 LN)
- [x] OM 등록 대상 정의
