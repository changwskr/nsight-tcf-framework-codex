# 아키텍처 결정기록 (ADR)

근거: `A05-대안비교-ADR.md`

## ADR-001 — 온라인 거래 진입점

| 항목 | 내용 |
| --- | --- |
| 상태 | Accepted (대화 확정 · AA 운영 전 서명) |
| 결정 | 공통 `OnlineTransactionController` |
| API | `POST /online`, `POST /{businessCode}/online` |
| 폐기 | 업무 WAR 거래용 REST Controller |
| 근거 | tcf-web 실소스 · 방법론 · A04 · 정합성/운영 |
| 결과 | ServiceId → TransactionDispatcher → Handler |

## Open Issue (후속 ADR)

| ID | 주제 |
| --- | --- |
| ADR-002 | WAR 단위 분할 vs 모놀리스 |
| ADR-003 | Gateway JWT + 업무 WAR 직접접근 방어 세부 |
