# 운영·로그·감사·모니터링 TASK 상세 설명서

이 문서는 OPS 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [OPS-01](./OPS-01-애플리케이션-로그-detail.md) | 애플리케이션 로그 | P0 | FW |
| [OPS-02](./OPS-02-거래로그-detail.md) | 거래로그 | P0 | FW |
| [OPS-03](./OPS-03-감사로그-detail.md) | 감사로그 | P0 | SEC |
| [OPS-04](./OPS-04-SQL-로그-detail.md) | SQL 로그 | P1 | DBA |
| [OPS-05](./OPS-05-연계로그-detail.md) | 연계로그 | P0 | EAI |
| [OPS-06](./OPS-06-로그-상관관계-detail.md) | 로그 상관관계 | P0 | FW |
| [OPS-07](./OPS-07-로그-보존-detail.md) | 로그 보존 | P1 | OPS |
| [OPS-08](./OPS-08-OM-Service-Catalog-detail.md) | OM Service Catalog | P0 | FW |
| [OPS-09](./OPS-09-거래통제-detail.md) | 거래통제 | P0 | OM |
| [OPS-10](./OPS-10-Slow-ServiceId-detail.md) | Slow ServiceId | P1 | OPS |
| [OPS-11](./OPS-11-Slow-SQL-detail.md) | Slow SQL | P1 | DBA |
| [OPS-12](./OPS-12-런타임-진단-detail.md) | 런타임 진단 | P1 | OPS |
| [OPS-13](./OPS-13-알림-정책-detail.md) | 알림 정책 | P2 | OPS |
| [OPS-14](./OPS-14-장애-원인코드-detail.md) | 장애 원인코드 | P2 | OPS |
| [OPS-15](./OPS-15-Runbook-detail.md) | Runbook | P2 | OPS |
| [OPS-16](./OPS-16-운영-변경관리-detail.md) | 운영 변경관리 | P1 | OPS |
| [OPS-17](./OPS-17-운영자-권한-detail.md) | 운영자 권한 | P1 | OPS |
| [OPS-18](./OPS-18-장애-보고-detail.md) | 장애 보고 | P2 | OPS |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
