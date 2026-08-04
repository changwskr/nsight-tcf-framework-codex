# 오류·Timeout·트랜잭션·안정성 TASK 상세 설명서

이 문서는 REL 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [REL-01](./REL-01-오류-분류-detail.md) | 오류 분류 | P0 | FW |
| [REL-02](./REL-02-오류코드-체계-detail.md) | 오류코드 체계 | P0 | FW |
| [REL-03](./REL-03-사용자-메시지-detail.md) | 사용자 메시지 | P0 | BA |
| [REL-04](./REL-04-예외-변환-detail.md) | 예외 변환 | P0 | FW |
| [REL-05](./REL-05-Timeout-계층-detail.md) | Timeout 계층 | P0 | TA |
| [REL-06](./REL-06-ServiceId-Timeout-detail.md) | ServiceId Timeout | P0 | AA |
| [REL-07](./REL-07-DB-Timeout-detail.md) | DB Timeout | P0 | DBA |
| [REL-08](./REL-08-Timeout-이후-상태-detail.md) | Timeout 이후 상태 | P0 | AA |
| [REL-09](./REL-09-재시도-적격성-detail.md) | 재시도 적격성 | P0 | AA |
| [REL-10](./REL-10-멱등성-Key-detail.md) | 멱등성 Key | P0 | FW |
| [REL-11](./REL-11-Rollback-기준-detail.md) | Rollback 기준 | P0 | AA |
| [REL-12](./REL-12-부분-실패-detail.md) | 부분 실패 | P0 | AA |
| [REL-13](./REL-13-Circuit-Breaker-detail.md) | Circuit Breaker | P1 | EAI |
| [REL-14](./REL-14-Bulkhead-detail.md) | Bulkhead | P1 | TA |
| [REL-15](./REL-15-사용자-재처리-detail.md) | 사용자 재처리 | P1 | BA |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
