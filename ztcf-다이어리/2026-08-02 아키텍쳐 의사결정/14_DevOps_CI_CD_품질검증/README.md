# DevOps·CI/CD·품질검증 아키텍처 의사결정

이 디렉터리는 QLT 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [QLT-01](./QLT-01-Git-Branch-전략.md) | Git Branch 전략 | P0 | DVO | SA |
| [QLT-02](./QLT-02-Commit-기준.md) | Commit 기준 | P0 | DVO | AA |
| [QLT-03](./QLT-03-Pull-Request.md) | Pull Request | P0 | DVO | SA |
| [QLT-04](./QLT-04-빌드-환경.md) | 빌드 환경 | P0 | DVO | AA |
| [QLT-05](./QLT-05-Artifact-버전.md) | Artifact 버전 | P1 | DVO | AA |
| [QLT-06](./QLT-06-계층-자동검증.md) | 계층 자동검증 | P1 | FW | AA |
| [QLT-07](./QLT-07-ServiceId-중복검사.md) | ServiceId 중복검사 | P0 | FW | AA |
| [QLT-08](./QLT-08-명명규칙-검사.md) | 명명규칙 검사 | P1 | DVO | AA |
| [QLT-09](./QLT-09-보안-정적분석.md) | 보안 정적분석 | P1 | SEC | SEC |
| [QLT-10](./QLT-10-단위테스트.md) | 단위테스트 | P0 | QA | AA |
| [QLT-11](./QLT-11-통합테스트.md) | 통합테스트 | P1 | QA | SA |
| [QLT-12](./QLT-12-계약테스트.md) | 계약테스트 | P1 | QA | SA |
| [QLT-13](./QLT-13-성능시험.md) | 성능시험 | P1 | QA | SA |
| [QLT-14](./QLT-14-보안시험.md) | 보안시험 | P1 | SEC | SEC |
| [QLT-15](./QLT-15-장애시험.md) | 장애시험 | P2 | QA | SA |
| [QLT-16](./QLT-16-배포-승인-Gate.md) | 배포 승인 Gate | P1 | QA | SA |
| [QLT-17](./QLT-17-배포-후-검증.md) | 배포 후 검증 | P1 | OPS | TA |
| [QLT-18](./QLT-18-Drift-검증.md) | Drift 검증 | P2 | QA | SA |

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
