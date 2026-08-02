# NSIGHT TCF Framework 문서 허브

`xdoc`는 NSIGHT TCF Framework를 이해하고 변경하기 위한 실행 중심 문서 공간이다. 기존 설계서와 매뉴얼을 대체하지 않으며, 코드 작업에 필요한 핵심 정보와 검증 기준을 짧은 경로로 제공한다.

## 빠른 시작

1. [프로젝트 개요](docs/product-specs/framework-overview.md)를 읽는다.
2. [시스템 아키텍처](architecture/system-architecture.md)에서 모듈과 거래 흐름을 확인한다.
3. 코드 변경 전 [개발 에이전트 지침](agents/development-agent-guide.md)을 확인한다.
4. [품질 기준](quality-score/quality-gates.md)과 [보안 기준](security/security-baseline.md)을 완료 조건에 반영한다.
5. 규모가 있는 변경은 [실행 계획 템플릿](docs/exec-plans/README.md)을 사용한다.

## 문서 지도

| 영역 | 문서 | 목적 |
|---|---|---|
| 제품 | [Framework Overview](docs/product-specs/framework-overview.md) | 범위, 사용자, 핵심 기능 정의 |
| 아키텍처 | [System Architecture](architecture/system-architecture.md) | 모듈 경계와 런타임 처리 흐름 설명 |
| 설계 | [Business Transaction Design](docs/design-docs/business-transaction-design.md) | 업무 거래 구현 규칙 설명 |
| 개발 | [Development Agent Guide](agents/development-agent-guide.md) | 탐색, 수정, 검증 작업 원칙 제공 |
| 품질 | [Quality Gates](quality-score/quality-gates.md) | 변경 유형별 완료 조건 제공 |
| 보안 | [Security Baseline](security/security-baseline.md) | 인증, 입력, 로그, 비밀정보 기준 제공 |
| 계획 | [Execution Plans](docs/exec-plans/README.md) | 복합 변경의 실행 계획 형식 제공 |
| 참조 | [Reference Map](docs/references/reference-map.md) | 원본 문서와 코드 진입점 안내 |

## 문서 운영 원칙

- 사실의 우선순위는 실행 코드, 빌드 설정, 자동화된 테스트, 운영 설정, 설명 문서 순이다.
- 포트, 모듈, API, 설정 키가 변경되면 관련 `xdoc` 문서를 같은 변경에서 갱신한다.
- 아직 구현되지 않은 내용은 현재 기능처럼 표현하지 않고 `계획`, `제안`, `미구현`으로 표시한다.
- 민감정보, 실제 사용자 정보, 운영 토큰과 비밀번호를 예시에 넣지 않는다.
- 모든 문서는 UTF-8 Markdown으로 관리한다.

