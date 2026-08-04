# 오류·Timeout·트랜잭션·안정성 아키텍처 의사결정

이 디렉터리는 REL 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [REL-01](./REL-01-오류-분류.md) | 오류 분류 | P0 | FW | AA |
| [REL-02](./REL-02-오류코드-체계.md) | 오류코드 체계 | P0 | FW | AA |
| [REL-03](./REL-03-사용자-메시지.md) | 사용자 메시지 | P0 | BA | AA |
| [REL-04](./REL-04-예외-변환.md) | 예외 변환 | P0 | FW | AA |
| [REL-05](./REL-05-Timeout-계층.md) | Timeout 계층 | P0 | TA | SA |
| [REL-06](./REL-06-ServiceId-Timeout.md) | ServiceId Timeout | P0 | AA | TA |
| [REL-07](./REL-07-DB-Timeout.md) | DB Timeout | P0 | DBA | DA |
| [REL-08](./REL-08-Timeout-이후-상태.md) | Timeout 이후 상태 | P0 | AA | SA |
| [REL-09](./REL-09-재시도-적격성.md) | 재시도 적격성 | P0 | AA | SA |
| [REL-10](./REL-10-멱등성-Key.md) | 멱등성 Key | P0 | FW | AA |
| [REL-11](./REL-11-Rollback-기준.md) | Rollback 기준 | P0 | AA | SA |
| [REL-12](./REL-12-부분-실패.md) | 부분 실패 | P0 | AA | SA |
| [REL-13](./REL-13-Circuit-Breaker.md) | Circuit Breaker | P1 | EAI | TA |
| [REL-14](./REL-14-Bulkhead.md) | Bulkhead | P1 | TA | SA |
| [REL-15](./REL-15-사용자-재처리.md) | 사용자 재처리 | P1 | BA | AA |

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
