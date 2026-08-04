# 업무·도메인·애플리케이션 TASK 상세 설명서

이 문서는 APP 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [APP-01](./APP-01-업무-도메인-분할-detail.md) | 업무 도메인 분할 | P0 | AA |
| [APP-02](./APP-02-업무코드-체계-detail.md) | 업무코드 체계 | P0 | SA |
| [APP-03](./APP-03-업무-WAR-구성-detail.md) | 업무 WAR 구성 | P0 | AA |
| [APP-04](./APP-04-애플리케이션-계층-detail.md) | 애플리케이션 계층 | P0 | AA |
| [APP-05](./APP-05-공통-Controller-적용-detail.md) | 공통 Controller 적용 | P0 | FW |
| [APP-06](./APP-06-TCF-진입-강제-detail.md) | TCF 진입 강제 | P0 | FW |
| [APP-07](./APP-07-Handler-책임-detail.md) | Handler 책임 | P0 | FW |
| [APP-08](./APP-08-Facade-책임-detail.md) | Facade 책임 | P0 | AA |
| [APP-09](./APP-09-Service-책임-detail.md) | Service 책임 | P0 | AA |
| [APP-10](./APP-10-Rule-책임-detail.md) | Rule 책임 | P1 | AA |
| [APP-11](./APP-11-DAO·Mapper-책임-detail.md) | DAO·Mapper 책임 | P0 | DA |
| [APP-12](./APP-12-DTO-분리-detail.md) | DTO 분리 | P0 | AA |
| [APP-13](./APP-13-트랜잭션-경계-detail.md) | 트랜잭션 경계 | P0 | AA |
| [APP-14](./APP-14-읽기-전용-거래-detail.md) | 읽기 전용 거래 | P1 | AA |
| [APP-15](./APP-15-도메인-간-호출-detail.md) | 도메인 간 호출 | P0 | AA |
| [APP-16](./APP-16-WAR-간-호출-detail.md) | WAR 간 호출 | P0 | AA |
| [APP-17](./APP-17-공통-Util-기준-detail.md) | 공통 Util 기준 | P1 | FW |
| [APP-18](./APP-18-비동기-처리-detail.md) | 비동기 처리 | P1 | FW |
| [APP-19](./APP-19-API-예외-Endpoint-detail.md) | API 예외 Endpoint | P0 | AA |
| [APP-20](./APP-20-업무-프로그램-추적성-detail.md) | 업무 프로그램 추적성 | P1 | AA |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
