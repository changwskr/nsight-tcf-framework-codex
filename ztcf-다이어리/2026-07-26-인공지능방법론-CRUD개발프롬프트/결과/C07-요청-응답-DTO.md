# C07 — 요청·응답 DTO (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | C07 |
| 사용자 답변 | 기준=1 av / 목록필드=1 / 상세필드=1 / DTO분리·페이징=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | 상세 응답 래핑 형태(단건 map vs item 키명) |
| 설계 영향 | application/dto + persistence/dto. sample-requests LN 2건 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | C08 — 패키지·프로그램 구조 |

---

## 기준 전문

| 구분 | 경로 |
| --- | --- |
| DTO | `av-service/.../dto/sample/*` |
| JSON | `tcf-ui/.../sample-requests/av-sample-inquiry.json` |

## selectList

| 방향 | 필드 |
| --- | --- |
| 요청 | `customerNo`(필수), `contactType`(선택), `pageNo`, `pageSize` |
| 응답 | `businessCode`, `serviceId`, `guid`, `rows`, `totalCount`, `pageNo`, `pageSize` |
| row | `contactId`, `customerNo`, `contactType`, `contactValue`, `useYn`, `regDtm`, `updDtm` |

- `useYn` 요청 필드 **없음** (서버 Y 강제)
- 페이징: default 15 / max 100 (av Rule)

## selectDetail

| 방향 | 필드 |
| --- | --- |
| 요청 | `contactId`(필수) |
| 응답 | 단건 + 메타(`businessCode`/`serviceId`/`guid`) |

`regDtm`/`updDtm`: 응답만.

## DTO 클래스 `[설계 예시]`

| 클래스 | 역할 |
| --- | --- |
| `CustomerContactSelectListRequest` | 목록 요청 |
| `CustomerContactSearchCriteria` | 검색·페이징 조건 |
| `CustomerContactSelectListResponse` | 목록 응답 |
| `CustomerContactSelectDetailRequest` | 상세 요청 |
| `CustomerContactSelectDetailResponse` | 상세 응답 |
| `CustomerContactRow` | persistence row |
| CUD DTO | 없음 |

## 마스킹·오류

| 항목 | 내용 |
| --- | --- |
| 마스킹 | 없음(시범, C05) |
| 목록 0건 | 정상 + 빈 rows |
| 검증 오류 | LN-CCT-V001/V002 |
| 업무 오류 | LN-CCT-E404 |
| 시스템/Timeout/권한 | 공통 프레임 처리 `[확인 필요]` |

## Gate

- [x] 요청↔테이블·규칙
- [x] 응답↔SQL 컬럼
- [x] 마스킹 정책 반영(없음=시범)
