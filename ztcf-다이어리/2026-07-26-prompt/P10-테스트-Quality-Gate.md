# P10 — 테스트·Quality Gate 프롬프트

사용: `P0` + `P10` + 모듈/ServiceId/Release 요구

```text
[P10 테스트·품질 검증 작업]

다음 기능 또는 모듈의 종합 테스트와 Quality Gate를 설계하라.

대상: {모듈/ServiceId/Release}
요구사항: {내용}
목표 응답시간: {기준}
보안 요구: {내용}
가용성 요구: {내용}
배포 방식: {내용}

다음 테스트 계층을 작성하라.

1. Rule 단위테스트
2. Service 단위테스트
3. DAO·Mapper 통합테스트
4. Handler·TCF 거래테스트
5. 표준 전문 계약테스트
6. 인증·권한 테스트
7. 거래통제·Timeout 테스트
8. 멱등성·중복요청 테스트
9. Transaction·Rollback 테스트
10. SQL 성능테스트
11. 동시성 테스트
12. 통합·외부연계 테스트
13. 장애·Failover 테스트
14. 보안 테스트
15. 배포·Rollback 테스트
16. 운영 로그·감사 추적 테스트

각 테스트에는 다음을 포함하라.

- 테스트 ID
- 목적
- 선행조건
- 입력
- 실행절차
- 예상결과
- DB 결과
- 로그 결과
- 판정기준
- 자동화 여부
- 증적 위치

Quality Gate를 다음처럼 구분하라.

- Source Gate
- Architecture Gate
- SQL Gate
- Security Gate
- Integration Gate
- Deployment Gate
- Operation Gate

Blocker·Critical·Major 기준과
Gate 통과·조건부 통과·부적합 조건도 정의하라.
```
