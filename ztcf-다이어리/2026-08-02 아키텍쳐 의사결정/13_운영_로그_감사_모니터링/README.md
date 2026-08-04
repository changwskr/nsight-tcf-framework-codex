# 운영·로그·감사·모니터링 아키텍처 의사결정

이 디렉터리는 OPS 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [OPS-01](./OPS-01-애플리케이션-로그.md) | 애플리케이션 로그 | P0 | FW | AA |
| [OPS-02](./OPS-02-거래로그.md) | 거래로그 | P0 | FW | AA |
| [OPS-03](./OPS-03-감사로그.md) | 감사로그 | P0 | SEC | SA |
| [OPS-04](./OPS-04-SQL-로그.md) | SQL 로그 | P1 | DBA | DA |
| [OPS-05](./OPS-05-연계로그.md) | 연계로그 | P0 | EAI | SA |
| [OPS-06](./OPS-06-로그-상관관계.md) | 로그 상관관계 | P0 | FW | AA |
| [OPS-07](./OPS-07-로그-보존.md) | 로그 보존 | P1 | OPS | SEC |
| [OPS-08](./OPS-08-OM-Service-Catalog.md) | OM Service Catalog | P0 | FW | AA |
| [OPS-09](./OPS-09-거래통제.md) | 거래통제 | P0 | OM | SA |
| [OPS-10](./OPS-10-Slow-ServiceId.md) | Slow ServiceId | P1 | OPS | SA |
| [OPS-11](./OPS-11-Slow-SQL.md) | Slow SQL | P1 | DBA | DA |
| [OPS-12](./OPS-12-런타임-진단.md) | 런타임 진단 | P1 | OPS | SA |
| [OPS-13](./OPS-13-알림-정책.md) | 알림 정책 | P2 | OPS | SA |
| [OPS-14](./OPS-14-장애-원인코드.md) | 장애 원인코드 | P2 | OPS | SA |
| [OPS-15](./OPS-15-Runbook.md) | Runbook | P2 | OPS | SA |
| [OPS-16](./OPS-16-운영-변경관리.md) | 운영 변경관리 | P1 | OPS | SA |
| [OPS-17](./OPS-17-운영자-권한.md) | 운영자 권한 | P1 | OPS | SEC |
| [OPS-18](./OPS-18-장애-보고.md) | 장애 보고 | P2 | OPS | SA |

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
