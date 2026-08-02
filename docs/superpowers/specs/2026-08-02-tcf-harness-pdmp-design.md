# TCF Harness PDMP 독립 프로젝트 설계

## 1. 목적

`tcf-harness-pdmp`를 범용 하네스 복제본에서 `pdmp-service` 전용 Codex 개발 하네스로 전환한다. 하네스는 PDMP의 실제 Controller → Service → DAO → MyBatis 구조를 유지하며 CRUD, TCF 거래 메타데이터, JWT·보안, 예외·성능·설정과 품질 검증을 조정한다.

## 2. 완료 기준

- 문서, 스킬, 역할, 명령과 테스트가 모두 `tcf-harness-pdmp`를 독립 프로젝트로 지칭한다.
- 실행 시 `tcf-harness-world` 파일이나 경로에 의존하지 않는다.
- `pdmp-service`를 유일한 기본 대상 프로젝트로 지정한다.
- PDMP 종합 개발과 CRUD 생성을 별도 스킬로 제공한다.
- `@TcfTransaction`, serviceId, transactionCode, ProcessingType 규칙을 검증한다.
- JWT, Spring Security, 개인정보, Secret과 로그 노출 검토 절차를 제공한다.
- 하네스 자체 테스트와 검증기는 잘못된 원본 경로 및 필수 구성 누락을 실패 처리한다.
- 가능한 환경에서 `pdmp-service` 테스트와 WAR 빌드 명령을 실행할 수 있다.

## 3. 범위

### 포함

- `tcf-harness-pdmp`의 이름, 경로, 문서와 실행 명령 독립화
- PDMP 전문 역할 계약 네 개
- 종합 개발, CRUD, TCF, 보안, 품질 스킬
- `pdmp-service` 구조와 문서를 요약한 로컬 references
- PowerShell 및 POSIX 검증기
- 정상·금지 경로·필수 역할·필수 스킬 누락 테스트
- 오프라인 PDMP 워크플로우 시뮬레이션

### 제외

- `pdmp-service` 소스나 빌드 설정 변경
- Controller → Service → DAO 구조를 공통 TCF 6계층으로 마이그레이션
- 실제 DB, Oracle, 외부 API 또는 인증 서버 호출
- 사용자 홈의 Codex 스킬 자동 설치
- `tcf-harness-world` 수정

## 4. 대상 프로젝트 기준

`pdmp-service`는 Java 21 독립 Gradle WAR 프로젝트다. 현재 Spring Boot 3.5.14, Spring Security, MyBatis, H2 로컬 환경과 Oracle 폐쇄망 전환 주석을 사용한다. 하네스는 다음 구현 구조를 기준으로 한다.

```text
Controller (@TcfTransaction)
  → Service
    → DAO
      → rdw.* MyBatis XML
```

주요 규칙은 다음과 같다.

- Java 패키지: `nhnis.mp.{업무영역}.{세부영역}`
- 프로그램 식별자: `mpcoa8888`과 같은 기존 형식
- serviceId: `MP.{Domain}.{action}`
- CRUD action: `list`, `detail`, `create`, `update`, `delete`
- 거래 선언: Controller 메서드의 `@TcfTransaction`
- 거래 필드: serviceId, transactionCode, processingType, serviceName
- SQL: MyBatis 파라미터 바인딩
- 로컬 DB: H2, 폐쇄망 운영 후보: Oracle
- 빌드: `pdmp-service\gradlew.bat test`, `pdmp-service\gradlew.bat war`

## 5. 목표 구조

```text
tcf-harness-pdmp/
├── AGENTS.md
├── README.md
├── docs/
│   ├── quickstart.md
│   ├── pdmp-architecture.md
│   └── workflow.md
├── agents/
│   ├── pdmp-analyst.md
│   ├── pdmp-builder.md
│   ├── pdmp-security-reviewer.md
│   └── pdmp-qa.md
├── skills/
│   ├── pdmp-development/
│   │   ├── SKILL.md
│   │   └── references/
│   ├── pdmp-crud/
│   │   ├── SKILL.md
│   │   └── references/
│   ├── pdmp-tcf/SKILL.md
│   ├── pdmp-security/SKILL.md
│   └── pdmp-quality/SKILL.md
├── samples/pdmp-development/
│   ├── scripts/
│   └── _workspace/
├── scripts/
│   ├── verify-pdmp-harness.ps1
│   └── verify-pdmp-harness.sh
└── tests/test-pdmp-harness.ps1
```

범용 `skills/harness`와 `orchestrator-sample` 이름은 제거해 PDMP 스킬과 충돌하지 않도록 한다.

## 6. 역할

### PDMP Analyst

요청, `pdmp-service` 문서, 기존 프로그램 ID, DTO, Controller, Service, DAO, SQL, 테스트와 설정을 조사한다. 신규 개발인지 기존 기능 확장인지 구분하고 serviceId·transactionCode 충돌, H2/Oracle 차이와 보안 영향을 기록한다.

### PDMP Builder

승인된 설계와 계획을 테스트 우선으로 구현한다. Controller → Service → DAO → XML 경계를 지키고 공개 계약·설정·스키마 변경 시 호환성과 롤백을 기록한다.

### PDMP Security Reviewer

JWT, Spring Security, CORS, 인증 실패 응답, 개인정보 마스킹, Secret·세션·토큰·SQL·Stack Trace 노출과 SQL 바인딩을 검토한다. 권한 면제 범위를 임의로 확대하지 않는다.

### PDMP QA

요구사항 추적성, TCF 메타데이터, 네이밍, 테스트, DAO 통합 테스트, 설정 Profile과 WAR 빌드를 검증한다. 실행하지 못한 검증과 원인을 분리해 보고한다.

## 7. 스킬

### `pdmp-development`

모든 PDMP 개발 요청의 기본 진입점이다. 탐색 → 분석 → 설계 승인 → 계획 → 구현 → 보안 검토 → QA 순서를 조정하고 필요한 하위 스킬만 선택한다.

### `pdmp-crud`

CRUD 요구사항을 프로그램 ID, DTO, Controller 거래 5종, Service, DAO, XML, H2 데이터와 테스트로 변환한다. delete는 기존 정책을 조사하고 물리 삭제를 기본값으로 가정하지 않는다.

### `pdmp-tcf`

`@TcfTransaction`의 네 필드, serviceId 형식과 중복, transactionCode 형식, ProcessingType 일치, 표준 헤더와 MDC 추적성을 검사한다.

### `pdmp-security`

JWT 필터, SecurityConfig, CORS, 개인정보와 로그·응답 노출, Secret 관리, MyBatis 파라미터 바인딩을 검사한다.

### `pdmp-quality`

가장 작은 테스트부터 Controller, Service, DAO 통합 테스트와 전체 test/war로 검증 범위를 넓힌다. H2 검증과 Oracle 미검증 범위를 분리한다.

## 8. 제어 및 산출물 흐름

```text
사용자 요청
  → PDMP Analyst: analysis-summary.md
  → 사용자 설계 승인
  → PDMP Builder: 구현 및 verification-report.md
  → PDMP Security Reviewer: security-review.md
  → PDMP QA: qa-report.md
  → PASS 또는 담당 역할에 제한된 수정 요청
```

역할 간 전달은 파일 경로, 상태, 입력 출처, 완료 기준과 미해결 항목을 포함한다. 독립 작업만 사용자 또는 상위 지침의 허가 아래 병렬화한다.

## 9. 오류 처리

- `pdmp-service`가 없거나 필수 문서·구조가 누락되면 후속 역할을 시작하지 않는다.
- serviceId나 프로그램 ID가 충돌하면 구현 전에 사용자 결정을 요청한다.
- H2와 Oracle SQL 호환성이 불확실하면 로컬 통과를 운영 호환으로 표현하지 않는다.
- 인증·권한·개인정보 요구가 모호하면 안전 범위를 임의로 확대하거나 축소하지 않는다.
- 테스트 또는 빌드 실패는 재현 명령, 실제 오류와 영향 범위를 기록한다.
- 동일 원인의 재시도는 유한 횟수로 제한한다.

## 10. 검증 설계

### 하네스 정적 검증

- 필수 역할·스킬·문서·스크립트 존재
- 모든 스킬 frontmatter와 로컬 링크
- `tcf-harness-world` 실행 경로와 범용 스킬명 부재
- `pdmp-service` 대상 경로와 PDMP 규칙 존재
- 역할 계약의 필수 섹션 존재

### 하네스 동적 검증

- 오프라인 Analyst → Builder → Security Reviewer → QA 시뮬레이션
- 필수 산출물 생성과 QA PASS
- 잘못된 원본 경로 주입 시 FAIL
- 필수 역할 또는 스킬 제거 시 FAIL
- 재실행 시 산출물 중복·손상 없음

### 대상 프로젝트 검증

```powershell
.\pdmp-service\gradlew.bat test
.\pdmp-service\gradlew.bat war
```

네트워크, 폐쇄망 의존성 또는 환경 제약으로 실행할 수 없으면 명령, 실패 원인과 미검증 범위를 보고한다.

## 11. 호환성과 롤백

`tcf-harness-pdmp`는 아직 미추적 독립 디렉터리이므로 기존 공개 계약을 변경하지 않는다. 변환 전 파일은 사용자 소유로 취급하며 구현 시 대상 디렉터리 밖을 수정하지 않는다. 롤백은 `tcf-harness-pdmp`와 관련 설계·계획 문서만 제거하거나 이전 복제본으로 되돌리는 방식으로 가능하다.
