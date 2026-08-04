# DevOps·CI/CD·품질검증 TASK 상세 설명서

이 문서는 QLT 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [QLT-01](./QLT-01-Git-Branch-전략-detail.md) | Git Branch 전략 | P0 | DVO |
| [QLT-02](./QLT-02-Commit-기준-detail.md) | Commit 기준 | P0 | DVO |
| [QLT-03](./QLT-03-Pull-Request-detail.md) | Pull Request | P0 | DVO |
| [QLT-04](./QLT-04-빌드-환경-detail.md) | 빌드 환경 | P0 | DVO |
| [QLT-05](./QLT-05-Artifact-버전-detail.md) | Artifact 버전 | P1 | DVO |
| [QLT-06](./QLT-06-계층-자동검증-detail.md) | 계층 자동검증 | P1 | FW |
| [QLT-07](./QLT-07-ServiceId-중복검사-detail.md) | ServiceId 중복검사 | P0 | FW |
| [QLT-08](./QLT-08-명명규칙-검사-detail.md) | 명명규칙 검사 | P1 | DVO |
| [QLT-09](./QLT-09-보안-정적분석-detail.md) | 보안 정적분석 | P1 | SEC |
| [QLT-10](./QLT-10-단위테스트-detail.md) | 단위테스트 | P0 | QA |
| [QLT-11](./QLT-11-통합테스트-detail.md) | 통합테스트 | P1 | QA |
| [QLT-12](./QLT-12-계약테스트-detail.md) | 계약테스트 | P1 | QA |
| [QLT-13](./QLT-13-성능시험-detail.md) | 성능시험 | P1 | QA |
| [QLT-14](./QLT-14-보안시험-detail.md) | 보안시험 | P1 | SEC |
| [QLT-15](./QLT-15-장애시험-detail.md) | 장애시험 | P2 | QA |
| [QLT-16](./QLT-16-배포-승인-Gate-detail.md) | 배포 승인 Gate | P1 | QA |
| [QLT-17](./QLT-17-배포-후-검증-detail.md) | 배포 후 검증 | P1 | OPS |
| [QLT-18](./QLT-18-Drift-검증-detail.md) | Drift 검증 | P2 | QA |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
