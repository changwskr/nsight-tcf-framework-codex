# 인증·인가·JWT·세션 아키텍처 의사결정

이 디렉터리는 AUTH 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [AUTH-01](./AUTH-01-SSO-인증-흐름.md) | SSO 인증 흐름 | P0 | SEC | SA |
| [AUTH-02](./AUTH-02-세션·JWT-전략.md) | 세션·JWT 전략 | P0 | AA | SA |
| [AUTH-03](./AUTH-03-JWT-발급-주체.md) | JWT 발급 주체 | P0 | SEC | SA |
| [AUTH-04](./AUTH-04-JWT-검증-위치.md) | JWT 검증 위치 | P0 | AA | SEC |
| [AUTH-05](./AUTH-05-JWT-Claim.md) | JWT Claim | P0 | SEC | SA |
| [AUTH-06](./AUTH-06-Claim–Header-정합성.md) | Claim–Header 정합성 | P0 | FW | SEC |
| [AUTH-07](./AUTH-07-Access-Token-수명.md) | Access Token 수명 | P0 | SEC | SEC |
| [AUTH-08](./AUTH-08-Refresh-Token.md) | Refresh Token | P0 | SEC | SEC |
| [AUTH-09](./AUTH-09-Token-폐기.md) | Token 폐기 | P1 | SEC | SA |
| [AUTH-10](./AUTH-10-기능권한.md) | 기능권한 | P0 | AA | SEC |
| [AUTH-11](./AUTH-11-데이터권한.md) | 데이터권한 | P0 | BA | SEC |
| [AUTH-12](./AUTH-12-관리자-권한.md) | 관리자 권한 | P0 | SEC | SA |
| [AUTH-13](./AUTH-13-인증-예외-URL.md) | 인증 예외 URL | P0 | SEC | SEC |
| [AUTH-14](./AUTH-14-동시-로그인.md) | 동시 로그인 | P1 | SEC | SA |

## 공통 완료 기준

```text
ADR 승인
+ 개발표준 반영
+ 공통 샘플 또는 모듈 제공
+ 테스트 기준 및 결과
+ CI/CD 자동검증
+ 업무팀·운영 적용 확인
= TASK 완료
```

## 기준 문서

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
