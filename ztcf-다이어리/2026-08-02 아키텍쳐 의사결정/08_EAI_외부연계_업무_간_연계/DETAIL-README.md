# EAI·외부연계·업무 간 연계 TASK 상세 설명서

이 문서는 INT 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [INT-01](./INT-01-EAI-적용범위-detail.md) | EAI 적용범위 | P0 | EAI |
| [INT-02](./INT-02-동기·비동기-선택-detail.md) | 동기·비동기 선택 | P0 | EAI |
| [INT-03](./INT-03-연계-표준-전문-detail.md) | 연계 표준 전문 | P0 | EAI |
| [INT-04](./INT-04-업무-WAR-간-연계-detail.md) | 업무 WAR 간 연계 | P0 | AA |
| [INT-05](./INT-05-연계-Timeout-detail.md) | 연계 Timeout | P0 | EAI |
| [INT-06](./INT-06-재시도-정책-detail.md) | 재시도 정책 | P0 | EAI |
| [INT-07](./INT-07-멱등성-detail.md) | 멱등성 | P0 | AA |
| [INT-08](./INT-08-부분-성공-detail.md) | 부분 성공 | P0 | AA |
| [INT-09](./INT-09-보상-처리-detail.md) | 보상 처리 | P0 | AA |
| [INT-10](./INT-10-전문-대사-detail.md) | 전문 대사 | P1 | EAI |
| [INT-11](./INT-11-연계-오류-매핑-detail.md) | 연계 오류 매핑 | P0 | EAI |
| [INT-12](./INT-12-Circuit-Breaker-detail.md) | Circuit Breaker | P1 | EAI |
| [INT-13](./INT-13-연계-Pool-분리-detail.md) | 연계 Pool 분리 | P1 | EAI |
| [INT-14](./INT-14-연계-변경관리-detail.md) | 연계 변경관리 | P1 | EAI |
| [INT-15](./INT-15-파일-연계-detail.md) | 파일 연계 | P1 | EAI |
| [INT-16](./INT-16-메시지-연계-detail.md) | 메시지 연계 | P1 | EAI |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
