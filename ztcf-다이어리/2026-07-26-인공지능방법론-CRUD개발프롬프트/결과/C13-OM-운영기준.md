# C13 — OM·운영기준 (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | C13 |
| 사용자 답변 | 등록방식=1 시드초안+AA승인 / OM항목=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | 권한코드 값; 거래통제 세부; OM 테이블 스키마 |
| 설계 영향 | data.sql/시드 초안. AA 승인 전 운영 반영 금지 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | C14 — 설계 Gate |

---

## 등록 방식

data.sql/시드 **초안** 작성 → **AA 승인** 후 반영.  
AV inquiry OM 미등록은 알려진 Gap — LN은 초안으로 추적.

## OM 항목 `[설계 예시]`

| 항목 | selectList | selectDetail |
| --- | --- | --- |
| ServiceId | LN.CustomerContact.selectList | LN.CustomerContact.selectDetail |
| 거래코드 | LN-INQ-0001 | LN-INQ-0002 |
| 거래명 | 고객연락처 목록조회 | 고객연락처 상세조회 |
| 처리유형 | INQUIRY | INQUIRY |
| Timeout | 5 | 5 |
| 거래통제 | 시범 기본(없음) | 시범 기본(없음) |
| 권한코드 | 로그인 필수 `[미확정]` | 동일 |
| 감사 | Y | Y |
| Handler | LnCustomerContactHandler | 동일 |
| 배포모듈 | ln-service / ln.war | 동일 |

## Gate

- [x] ServiceId↔OM
- [x] Handler↔OM 일치
- [x] Timeout·통제·감사
