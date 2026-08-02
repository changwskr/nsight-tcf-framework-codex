# NSIGHT TCF Framework Agent Instructions

## 적용 범위

이 파일은 저장소 전체에 적용되는 Codex 작업 지침이다. 하위 디렉터리에 더 구체적인 `AGENTS.md`가 있으면 해당 범위에서는 하위 지침을 함께 따른다.

## 프로젝트 기준

- Java 21, Spring Boot 3.3.5와 Gradle 멀티모듈 구성을 사용한다.
- 기본 의존 방향은 `tcf-util → tcf-core → tcf-web → 업무/플랫폼 모듈`이다.
- 기반 모듈이 업무 모듈을 참조하거나 모듈 사이에 순환 의존성을 만들지 않는다.
- 기존 미커밋 변경은 사용자 소유로 간주하고 덮어쓰거나 되돌리지 않는다.
- 소스, 설정과 Markdown 문서는 UTF-8을 유지한다.

## 필수 탐색 절차

1. `git status --short`로 기존 변경을 확인한다.
2. 루트 `README.md`와 대상 모듈의 `README.md`, `build.gradle`을 확인한다.
3. `rg`와 `rg --files`로 호출부, 구현부, 설정, 테스트와 문서 사용처를 검색한다.
4. `serviceId` 변경 시 Handler, Service Catalog, UI, 샘플 요청과 도움말 색인을 함께 확인한다.
5. 설정 변경 시 공통 설정과 Local, Dev, Prod Profile의 차이를 확인한다.

## 아키텍처 규칙

업무 코드는 다음 호출 방향을 따른다.

```text
entry/handler
  → entry/facade
    → application/service
      → application/rule
        → persistence/dao 또는 persistence/mapper
```

- 업무 도메인마다 하나의 `TransactionHandler`를 사용한다.
- Handler는 `serviceIds()`로 `{BusinessCode}.{Domain}.{action}` 형식의 거래를 등록한다.
- Handler는 거래 분기와 Facade 호출만 담당한다.
- Facade는 DTO 변환, 유스케이스 조정과 트랜잭션 경계를 담당한다.
- Service는 업무 흐름, Rule은 검증과 계산, DAO/Mapper는 데이터 접근을 담당한다.
- 외부 서비스 호출은 `client` 계층과 `tcf-eai` 사용을 우선한다.
- STF/TCF/ETF가 제공하는 검증, 거래 통제, 타임아웃, 로그와 오류 처리를 업무 모듈에 복제하지 않는다.

## 변경 원칙

- 요청 범위 안에서 가장 작은 일관된 변경을 수행한다.
- 관련 없는 정리, 대규모 이름 변경과 포맷 변경을 섞지 않는다.
- 공개 계약, 표준 전문, 설정 키 또는 DB Schema 변경은 호환성과 롤백 방법을 기록한다.
- ThreadLocal 또는 MDC 컨텍스트를 추가하면 모든 종료 경로에서 정리한다.
- 자동 생성 결과는 초안으로 취급하고 Diff, Compile, Test와 리뷰를 거친다.
- 코드 동작과 문서가 불일치하면 구현을 검증하고 관련 문서를 함께 갱신한다.

## 보안 기준

- 비밀번호, Access/Refresh Token, Private Key, 세션 ID와 개인정보를 코드나 로그에 남기지 않는다.
- 인증·권한 면제와 내부 호출 판정 범위를 임의로 확대하지 않는다.
- SQL에는 MyBatis 또는 JDBC 파라미터 바인딩을 사용한다.
- 외부 입력을 SQL, 파일 경로, URL 또는 명령에 사용하기 전에 검증한다.
- 내부 예외, SQL과 Stack Trace를 클라이언트 응답에 노출하지 않는다.
- 운영 Secret은 환경변수 또는 승인된 Secret Store에서 주입한다.

## 검증 기준

가장 작은 관련 단위부터 검증한 뒤 영향도에 따라 범위를 넓힌다.

```powershell
# 대상 모듈 테스트 예시
.\gradlew.bat :tcf-core:test

# 대상 업무 모듈 빌드 예시
.\gradlew.bat :sv-service:build

# 전체 빌드
.\gradlew.bat build
```

- `tcf-util` 변경: 해당 테스트와 직접 의존 모듈 컴파일
- `tcf-core` 변경: Core와 Web 테스트, 대표 업무 모듈 빌드
- `tcf-web` 변경: Web 테스트와 대표 업무 WAR 빌드
- 업무 거래 변경: Handler/Service/Rule 테스트, Mapper 검증과 해당 WAR 빌드
- Gateway/JWT 변경: 인증 성공, 실패, 만료와 우회 방지 검증
- 도움말 변경: Help 검증 Task와 링크/색인 확인

테스트를 실행할 수 없으면 실행한 명령, 실패 원인과 미검증 범위를 결과에 명시한다.

## 완료 보고

- 결과를 먼저 설명하고 핵심 변경 파일을 제시한다.
- 수행한 테스트와 결과를 정확히 기록한다.
- 미검증 사항, 기존 변경, 호환성 위험과 후속 작업을 구분한다.
- 실행하지 않은 테스트를 실행했다고 표현하지 않는다.

## 역할별 지침

작업 성격에 따라 다음 역할 문서를 참고한다.

- [Framework Agent](xdoc/agents/framework-agent.md)
- [Business Agent](xdoc/agents/business-agent.md)
- [CRUD Codegen Agent](xdoc/agents/crud-codegen-agent.md) — CRUD 방법론 `결과*` 마크다운 → C15 소스 생성
- [Security Agent](xdoc/agents/security-agent.md)
- [Quality Agent](xdoc/agents/quality-agent.md)
- [Documentation Agent](xdoc/agents/documentation-agent.md)
- [공통 개발 지침](xdoc/agents/development-agent-guide.md)

CRUD 통합 실행 작업 공간: [ztcf-다이어리/2026-07-26-Agents/](ztcf-다이어리/2026-07-26-Agents/README.md)  
권장 진입점: [MASTER-CRUD-DEVELOPER.md](ztcf-다이어리/2026-07-26-Agents/prompts/MASTER-CRUD-DEVELOPER.md)  
설계 스펙: [docs/superpowers/specs/2026-07-26-nsight-crud-prompt-package-design.md](docs/superpowers/specs/2026-07-26-nsight-crud-prompt-package-design.md)

