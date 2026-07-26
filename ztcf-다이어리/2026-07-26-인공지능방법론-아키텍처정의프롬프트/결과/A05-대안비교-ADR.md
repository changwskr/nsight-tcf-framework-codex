# A05 — 대안비교·ADR (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | A05 |
| 사용자 답변 | 주제=1 진입점 / ADR-001=1 확정 |
| 확정사항 | ADR-001 |
| 가정·미확정 | WAR분할·JWT ADR = Open Issue |
| 설계 영향 | A06~A07 진입 경로 고정 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | A06 — 목표 논리·애플리케이션 |

---

## ADR-001 — 온라인 거래 진입점

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted (대화 확정 · AA 최종 서명은 운영 전) |
| 결정 | 공통 `OnlineTransactionController` |
| API | `POST /online`, `POST /{businessCode}/online` |
| 폐기 | 업무 WAR 거래용 REST Controller 신설 |
| 근거 | tcf-web 실소스 · 방법론 · A04 · 정합성/운영 |
| 결과 | ServiceId → TransactionDispatcher → Handler |

### 대안 비교 (요약)

| 대안 | 정합성 | 운영 | 보안 | 비용 | 결과 |
| --- | --- | --- | --- | --- | --- |
| A. 공통 /online | 고 | 고 | 중~고 | 저 | **채택** |
| B. 업무 Controller | 저 | 중 | 중 | 중 | 폐기 |

## Open Issue

- ADR-002 후보: WAR 단위 분할  
- ADR-003 후보: Gateway JWT + 직접접근 방어  

## Gate

- [x] 최소 1건 ADR
