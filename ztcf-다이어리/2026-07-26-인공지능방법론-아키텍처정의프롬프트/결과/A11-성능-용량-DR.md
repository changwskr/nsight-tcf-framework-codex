# A11 — 성능·용량·DR (확정)

| 구분 | 내용 |
| --- | --- |
| 단계 | A11 |
| 사용자 답변 | 수치=1 란 / 구조=1 |
| 확정사항 | 아래 절 |
| 가정·미확정 | TPS·p95·풀·DR 사이트 |
| 설계 영향 | A12 모니터링 지표 란과 연결 |
| 원장 반영 | `_확정정보원장.md` |
| 다음 단계 | A12 — 운영·모니터링·장애 |

---

## 수치 정책

| 항목 | 값 |
| --- | --- |
| TPS / p95 / Pool | **미확정 란** |
| Timeout (시범 기록) | online 예: 5초 (Facade/OM과 정합 시) |

## 구조

| 항목 | 내용 |
| --- | --- |
| 공유 | Tomcat/JVM/Connector |
| 분리 | WAR별 HikariCP·업무 DB |
| Timeout 실행 | OnlineTransactionTimeoutExecutor |
| DR | 구조만 · 세부 미확정 |

## Gate

- [x] 수치 미확정이어도 구조·란 존재
