# 업무·도메인·애플리케이션 아키텍처 의사결정

이 디렉터리는 APP 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [APP-01](./APP-01-업무-도메인-분할.md) | 업무 도메인 분할 | P0 | AA | SA |
| [APP-02](./APP-02-업무코드-체계.md) | 업무코드 체계 | P0 | SA | ARB |
| [APP-03](./APP-03-업무-WAR-구성.md) | 업무 WAR 구성 | P0 | AA | SA |
| [APP-04](./APP-04-애플리케이션-계층.md) | 애플리케이션 계층 | P0 | AA | SA |
| [APP-05](./APP-05-공통-Controller-적용.md) | 공통 Controller 적용 | P0 | FW | SA |
| [APP-06](./APP-06-TCF-진입-강제.md) | TCF 진입 강제 | P0 | FW | SA |
| [APP-07](./APP-07-Handler-책임.md) | Handler 책임 | P0 | FW | AA |
| [APP-08](./APP-08-Facade-책임.md) | Facade 책임 | P0 | AA | SA |
| [APP-09](./APP-09-Service-책임.md) | Service 책임 | P0 | AA | SA |
| [APP-10](./APP-10-Rule-책임.md) | Rule 책임 | P1 | AA | SA |
| [APP-11](./APP-11-DAO·Mapper-책임.md) | DAO·Mapper 책임 | P0 | DA | SA |
| [APP-12](./APP-12-DTO-분리.md) | DTO 분리 | P0 | AA | SA |
| [APP-13](./APP-13-트랜잭션-경계.md) | 트랜잭션 경계 | P0 | AA | SA |
| [APP-14](./APP-14-읽기-전용-거래.md) | 읽기 전용 거래 | P1 | AA | SA |
| [APP-15](./APP-15-도메인-간-호출.md) | 도메인 간 호출 | P0 | AA | SA |
| [APP-16](./APP-16-WAR-간-호출.md) | WAR 간 호출 | P0 | AA | SA |
| [APP-17](./APP-17-공통-Util-기준.md) | 공통 Util 기준 | P1 | FW | AA |
| [APP-18](./APP-18-비동기-처리.md) | 비동기 처리 | P1 | FW | SA |
| [APP-19](./APP-19-API-예외-Endpoint.md) | API 예외 Endpoint | P0 | AA | SA |
| [APP-20](./APP-20-업무-프로그램-추적성.md) | 업무 프로그램 추적성 | P1 | AA | SA |

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
