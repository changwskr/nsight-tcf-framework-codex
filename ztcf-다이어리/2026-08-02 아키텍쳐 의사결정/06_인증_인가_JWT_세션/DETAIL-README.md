# 인증·인가·JWT·세션 TASK 상세 설명서

이 문서는 AUTH 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [AUTH-01](./AUTH-01-SSO-인증-흐름-detail.md) | SSO 인증 흐름 | P0 | SEC |
| [AUTH-02](./AUTH-02-세션·JWT-전략-detail.md) | 세션·JWT 전략 | P0 | AA |
| [AUTH-03](./AUTH-03-JWT-발급-주체-detail.md) | JWT 발급 주체 | P0 | SEC |
| [AUTH-04](./AUTH-04-JWT-검증-위치-detail.md) | JWT 검증 위치 | P0 | AA |
| [AUTH-05](./AUTH-05-JWT-Claim-detail.md) | JWT Claim | P0 | SEC |
| [AUTH-06](./AUTH-06-Claim–Header-정합성-detail.md) | Claim–Header 정합성 | P0 | FW |
| [AUTH-07](./AUTH-07-Access-Token-수명-detail.md) | Access Token 수명 | P0 | SEC |
| [AUTH-08](./AUTH-08-Refresh-Token-detail.md) | Refresh Token | P0 | SEC |
| [AUTH-09](./AUTH-09-Token-폐기-detail.md) | Token 폐기 | P1 | SEC |
| [AUTH-10](./AUTH-10-기능권한-detail.md) | 기능권한 | P0 | AA |
| [AUTH-11](./AUTH-11-데이터권한-detail.md) | 데이터권한 | P0 | BA |
| [AUTH-12](./AUTH-12-관리자-권한-detail.md) | 관리자 권한 | P0 | SEC |
| [AUTH-13](./AUTH-13-인증-예외-URL-detail.md) | 인증 예외 URL | P0 | SEC |
| [AUTH-14](./AUTH-14-동시-로그인-detail.md) | 동시 로그인 | P1 | SEC |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
