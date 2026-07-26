# NSIGHT TCF 업무모델 자동화 개발 방법론 및 도구 설계서

## 1. 도입 전 안내말

NSIGHT TCF 기반 개발 자동화의 목적은 개발자가 화면 몇 개를 입력하면 무조건 동작하는 프로그램을 만들어 주는 데 있지 않다. 자동화의 핵심 목적은 다음 연결을 하나의 기준정보로 관리하고, 반복 가능한 코드와 산출물은 기계가 생성하며, 업무 판단과 예외 판단은 사람이 승인하도록 만드는 것이다.

```text
요구사항
  → 업무코드·도메인
  → 화면·이벤트
  → ServiceId·거래코드
  → Request/Response DTO
  → Handler·Facade·Service·Rule
  → DAO·Mapper·SQL ID
  → Table·Column
  → OM Service Catalog·Timeout·감사
  → 테스트·배포·운영로그
```

NSIGHT TCF의 기준 실행 경로는 공통 Controller에서 요청을 수신한 후 TCF/STF와 ServiceId Dispatcher를 거쳐 도메인 Handler, Facade, Service, Rule, DAO, Mapper로 이동한다. 따라서 자동화 도구가 업무별 Controller를 임의 생성하거나 Service에서 Mapper를 직접 호출하도록 만들어서는 안 된다.

본 도구의 핵심 판단은 다음과 같다.

```text
자동화 대상
= 식별자 채번·형식검증·DTO·계층 골격·Mapper·SQL 초안·산출물·추적성·테스트 골격

사람의 승인 대상
= 업무 규칙·데이터 소유권·SQL 성능·권한·감사·트랜잭션·예외·운영 위험
```

---

# 2. 문서 개요

## 2.1 목적

본 방법론의 목적은 NSIGHT TCF 업무모델을 화면 중심의 메타데이터로 정의하고, 승인된 모델로부터 표준 코드·SQL·설계 산출물·테스트 골격을 자동 생성하는 절차와 책임 기준을 수립하는 것이다.

| 목적 | 설명 |
|---|---|
| 개발 절차 표준화 | 분석·모델링·생성·검증·배포의 선후관계를 통일 |
| 추적성 확보 | 화면 이벤트에서 ServiceId·프로그램·SQL·테이블까지 연결 |
| 반복작업 제거 | DTO, 계층 클래스, Mapper XML, 문서 초안 자동 생성 |
| 품질 내재화 | 명명·계층·SQL 안전성·중복·감사·Timeout 자동검증 |
| 변경 영향 관리 | 화면·필드·테이블 변경 시 영향 거래와 산출물 식별 |
| 운영 연계 | OM Catalog, 거래통제, Timeout, 감사로그 기준정보 생성 |
| 생성물 통제 | 수동 수정 영역과 재생성 영역을 분리하여 덮어쓰기 방지 |
| 프로젝트 확장 | 업무코드와 도메인이 늘어나도 동일 절차 반복 적용 |

## 2.2 적용범위

- WEBTOPSUITE·React 화면과 화면 이벤트
- NSIGHT 표준 요청·응답 전문
- 업무코드, 도메인, 화면 ID, 이벤트 ID, ServiceId, 거래코드
- Handler, Facade, Service, Rule, DAO, Mapper
- Request·Criteria·Response·Row DTO
- MyBatis Mapper Interface와 XML Statement
- 테이블·컬럼·PK·조회조건·입출력 매핑
- OM Service Catalog·Timeout·감사 대상 등록정보
- 단위테스트 골격, HTTP 거래 요청 예시
- 화면설계서·거래설계서·추적성 매트릭스·DDL 초안
- CI/CD 품질 Gate와 변경관리

본 MVP에서 직접 구현하지 않은 항목은 다음과 같다.

- 실 DB 접속을 통한 스키마 역공학
- WEBTOPSUITE 화면 파일 자체 생성
- Git 저장소 자동 Commit·Merge Request 생성
- 실제 OM DB/API 직접 등록
- SQL 실행계획·통계정보 기반 자동 튜닝
- 사내 SSO·권한 적용
- 생성 코드의 실제 NSIGHT 전체 빌드와 배포

이 항목은 2단계 확장 대상으로 정의한다.

## 2.3 대상 독자

| 대상 | 활용 목적 |
|---|---|
| 업무 분석가 | 화면·이벤트·업무용어·입출력 정의 |
| UI 개발자 | 화면 필드와 ServiceId 요청 연결 |
| 업무 개발자 | 생성 코드 보완과 업무 규칙 구현 |
| 애플리케이션 아키텍트 | 계층·도메인·트랜잭션·의존성 검토 |
| DA·DBA | 테이블·컬럼·SQL·인덱스 검토 |
| 프레임워크팀 | TCF 계약과 생성 템플릿 관리 |
| 보안 담당자 | 권한·개인정보·감사 대상 검토 |
| 테스트팀 | 모델 기반 테스트 시나리오 작성 |
| DevOps팀 | 생성·검증·빌드 Gate 운영 |
| 운영팀 | ServiceId·SQL·테이블 추적성과 운영 기준정보 확인 |
| PMO·품질팀 | 산출물 완전성·승인상태·예외 관리 |

## 2.4 선행조건

1. 업무코드와 업무 WAR·Context Path 매핑이 확정되어야 한다.
2. 도메인 경계와 데이터 소유권이 정의되어야 한다.
3. 화면 ID·이벤트 ID·ServiceId·거래코드 명명규칙이 승인되어야 한다.
4. TCF 계층 책임과 패키지 프로파일이 확정되어야 한다.
5. 표준 Header, 오류코드, Timeout, 감사 기준이 정의되어야 한다.
6. 생성물의 공식 저장소와 리뷰·승인 절차가 지정되어야 한다.
7. 템플릿 버전과 대상 TCF Framework 버전의 호환표가 관리되어야 한다.

## 2.5 용어 정의

| 용어 | 정의 |
|---|---|
| 업무모델 | 화면·거래·DTO·데이터·프로그램 관계를 표현하는 메타데이터 |
| 메타모델 | 업무모델이 가질 수 있는 객체·속성·관계의 구조 |
| 생성기 | 승인 모델을 Java·XML·SQL·문서로 변환하는 컴포넌트 |
| 템플릿 | 계층별 생성 파일의 표준 구조 |
| Workspace | 하나의 프로젝트에서 함께 검증·생성할 업무모델 집합 |
| 생성 소유영역 | 재생성 시 도구가 덮어쓸 수 있는 코드 영역 |
| 수동 소유영역 | 개발자가 작성하며 도구가 덮어쓰지 않는 영역 |
| Quality Gate | 다음 상태로 전환하기 전에 반드시 통과해야 하는 자동·수동 검증 |
| Model Baseline | 승인되어 변경통제를 받는 업무모델 버전 |
| Traceability | 화면에서 DB까지, DB에서 화면까지 추적 가능한 관계 |

---

# 3. 본문

## 3.1 문제 정의 및 설계 배경

기존 SI 개발에서는 화면설계서, 테이블정의서, 거래설계서, 프로그램설계서, Mapper XML과 Java 코드가 서로 다른 파일과 담당자에 의해 작성되는 경우가 많다. 이 방식에서는 동일 정보를 여러 번 입력하며 다음 문제가 발생한다.

| 문제 | 결과 |
|---|---|
| 화면과 ServiceId를 별도 관리 | 버튼이 호출하는 거래를 찾기 어려움 |
| DTO와 DB 컬럼을 별도 정의 | 필드명·타입·필수 여부 불일치 |
| 프로그램명을 수동 작성 | 계층별 명명 불일치와 누락 |
| Mapper SQL ID 수동 작성 | DAO Method와 SQL ID 추적 단절 |
| OM 등록 사후 수행 | 미등록 ServiceId, Timeout 누락 발생 |
| 문서를 개발 후 작성 | 실제 코드와 산출물이 다름 |
| 복사 개발 | 이전 업무의 코드·오류코드·테이블명이 잔존 |
| 변경 영향 수작업 | 테이블 변경 시 영향 화면 누락 |

자동화는 이 문제를 해결하기 위해 **한 번 정의한 메타데이터를 여러 산출물로 투영**해야 한다.

```text
하나의 업무모델
  ├─ 화면·이벤트 정의서
  ├─ ServiceId·거래설계서
  ├─ Handler·Facade·Service·Rule·DAO·Mapper
  ├─ Request·Criteria·Response·Row DTO
  ├─ Mapper XML·SQL ID
  ├─ DDL·OM 등록 SQL
  ├─ HTTP 거래 요청 예시
  ├─ 테스트 골격
  └─ End-to-End 추적성 매트릭스
```

## 3.2 현행 구조와 문제점

기준 소스는 다음 특징을 가진다.

- 공통 `OnlineTransactionController`가 요청을 수신한다.
- `TransactionDispatcher`가 ServiceId를 기준으로 Handler를 선택한다.
- 도메인 Handler가 `serviceIds()`로 여러 ServiceId를 등록할 수 있다.
- 업무 계층은 `Handler → Facade → Service → Rule·DAO → Mapper`로 분리된다.
- Transaction 경계는 Facade에 위치한다.
- DAO Method는 Mapper Statement를 호출한다.
- Mapper XML에는 Statement Timeout과 SQL ID가 정의된다.

현행 코드와 목표 문서 사이에는 패키지 구조의 두 가지 관점이 존재한다.

| 구분 | 형태 | 판단 |
|---|---|---|
| 현재 소스 호환형 | `business.entry/application/persistence` | 기존 소스 즉시 적용에 유리 |
| 도메인 우선 목표형 | `business.domain.layer` | 도메인 경계 표현과 자동검증에 유리 |

따라서 도구는 어느 하나를 강제하지 않고 **패키지 프로파일**로 선택하게 한다. 초기 도입은 현재 소스 호환형을 기본으로 하며, 신규 업무 또는 구조개편 시 도메인 우선형을 적용할 수 있다.

## 3.3 요구사항과 제약조건

### 기능 요구사항

1. 프로젝트·업무코드·도메인·패키지를 정의한다.
2. 화면·이벤트와 성공·실패 처리를 정의한다.
3. ServiceId·거래코드·처리유형·Timeout·감사·권한을 정의한다.
4. 테이블·컬럼·Java 타입·DB 타입·PK·입출력·조건을 정의한다.
5. 명명·중복·필수·추적성·감사·SQL 안전성 규칙을 검증한다.
6. 동일 도메인의 여러 ServiceId를 하나의 Handler로 병합한다.
7. 코드와 문서를 미리보기하고 ZIP으로 생성한다.
8. 저장된 전체 모델을 Workspace 단위로 검증·생성한다.

### 비기능 요구사항

| 항목 | 기준 |
|---|---|
| 재현성 | 동일 모델·동일 템플릿 버전은 동일 산출물 생성 |
| 결정성 | 생성 순서·파일명·코드 구조가 실행할 때마다 변하지 않음 |
| 안전성 | 검증 오류가 있으면 생성 차단 |
| 독립성 | MVP는 외부 패키지 설치 없이 Python 3만으로 실행 |
| 이식성 | Windows·Linux 개발 PC에서 실행 |
| 감사성 | 모델·템플릿·생성파일 목록을 manifest로 기록 |
| 보안 | 로컬 MVP에는 운영 비밀정보·실 데이터 입력 금지 |
| 확장성 | 생성기·검증기·Repository를 독립 모듈로 분리 |

### 제약조건

- 실제 업무 규칙은 모델만으로 완전 자동 생성할 수 없다.
- SQL 성능은 테이블 통계·데이터 분포·실행계획 없이는 확정할 수 없다.
- 생성 코드를 운영 반영하기 전에 사람의 설계·코드 리뷰가 필요하다.
- 실 DB·Git·OM 연동은 기관 보안정책과 접속 인터페이스가 확정되어야 한다.

## 3.4 설계 원칙

### 원칙 1. Model First

코드부터 작성하지 않고 화면·거래·데이터 모델을 먼저 확정한다.

### 원칙 2. Single Source of Truth

화면·DTO·DB 컬럼을 각각 입력하지 않고 하나의 필드 메타데이터를 여러 산출물에 재사용한다.

### 원칙 3. ServiceId 중심 추적

화면 이벤트와 업무 코드를 연결하는 핵심 식별자는 Controller URL이 아니라 ServiceId다.

### 원칙 4. 도메인 Handler

ServiceId마다 Handler 클래스를 생성하지 않고 동일 도메인의 거래를 하나의 Handler에 병합한다.

### 원칙 5. 책임 경계 보존

- Handler: ServiceId 분기와 Facade 호출
- Facade: 유스케이스·Transaction 경계
- Service: 업무 처리 조립
- Rule: 부작용 없는 검증·판단
- DAO: 데이터 접근 추상화
- Mapper: SQL 계약

### 원칙 6. 생성과 수동코드 분리

생성기는 전체 파일을 재생성할 수 있으므로 개발자가 임의로 생성 파일에 복잡한 로직을 추가하지 않는다. 운영형 버전에서는 다음 중 하나를 적용한다.

- 생성 클래스 + 수동 확장 클래스 분리
- protected region 주석과 AST 기반 병합
- 생성물은 별도 Branch에 생성 후 Diff 승인

MVP는 **생성 ZIP을 검토 후 대상 저장소에 병합**하는 방식이다.

### 원칙 7. Validation Before Generation

명명·중복·필드·감사·SQL 조건 오류가 있으면 코드 생성보다 먼저 차단한다.

### 원칙 8. Human in the Loop

자동화 결과는 초안이며 업무·보안·데이터·운영 담당자의 승인 없이는 Baseline으로 전환하지 않는다.

## 3.5 대안 비교 및 의사결정

| 대안 | 장점 | 단점 | 판단 |
|---|---|---|---|
| Excel 매크로 | 도입이 쉬움 | 관계·버전·병합·검증 확장 어려움 | 입력·이관 보조 수단 |
| IDE Plugin | 개발자 경험 우수 | IDE 종속, 분석가·DA 접근 어려움 | 2단계 보조 기능 |
| CLI Generator | CI/CD 통합 우수 | 모델링 UI 부족 | 운영형 배치 생성에 적합 |
| Web Model Studio | 역할 간 공동사용, 중앙 검증 | 인증·Repository 운영 필요 | 기본 플랫폼으로 채택 |
| Low-code BPM | 복잡한 Workflow 지원 | TCF 코드 구조와 괴리 가능 | 비채택 |

최종 목표는 다음 조합이다.

```text
Web Model Studio
  + Model Repository
  + CLI Generator
  + CI/CD Quality Gate
  + Git/OM/DB Adapter
```

현재 제공한 MVP는 Web Model Studio, JSON Repository, Generator, Validation Engine을 단일 로컬 애플리케이션으로 구현한다.

## 3.6 목표 아키텍처

```text
[사용자]
 BA / UI / DEV / AA / DA / QA
        │
        ▼
[Model Studio UI]
 프로젝트·화면·서비스·테이블·필드 Wizard
        │ JSON API
        ▼
[Model API]
 CRUD / 상태전환 / Workspace 조회
        │
        ├─────────────┐
        ▼             ▼
[Model Repository] [Validation Engine]
 Version/Baseline   Naming/Trace/Layer/SQL/Security
        │             │
        └──────┬──────┘
               ▼
[Generation Engine]
 Java / XML / SQL / Docs / Tests / OM / Manifest
               │
        ┌──────┼────────┐
        ▼      ▼        ▼
     Preview  ZIP     CLI/CI
               │
               ▼
[Git Merge Review]
 Compile/Test/ArchUnit/SQL Review
               │
               ▼
[OM 등록·배포·운영]
```

### MVP 구성

| 구성요소 | 구현 |
|---|---|
| UI | HTML·CSS·JavaScript 6단계 Wizard |
| API | Python 표준 `http.server` 기반 JSON API |
| Repository | 로컬 JSON 파일 저장 |
| Validation | Python 규칙 엔진 |
| Generator | Java/XML/SQL/Markdown/CSV ZIP 생성 |
| Test | Python Unit Test + 생성 Java 계약 Stub 컴파일 |

## 3.7 표준 형식

### 식별자

| 대상 | 표준 형식 | 예시 |
|---|---|---|
| 업무코드 | 대문자 2~3자리 | `SV` |
| 화면 ID | `{업무}-{세구분}-{4자리}` | `SV-CUS-0001` |
| 이벤트 ID | `{화면ID}-E{2자리}` | `SV-CUS-0001-E01` |
| ServiceId | `{업무}.{도메인}.{행위}` | `SV.Customer.selectSummary` |
| 거래코드 | `{업무}-{유형}-{4자리}` | `SV-INQ-0001` |
| SQL ID | DAO/Mapper Method와 동일 | `selectCustomerSummary` |
| 테이블 | 업무·의미 기반 대문자 | `SV_CUSTOMER` |
| Java 필드 | lowerCamelCase | `customerNo` |
| DB 컬럼 | UPPER_SNAKE_CASE | `CUSTOMER_NO` |

### 패키지 프로파일

```text
현재 소스 호환형
com.nh.nsight.marketing.sv.entry.handler
com.nh.nsight.marketing.sv.entry.facade
com.nh.nsight.marketing.sv.application.service
com.nh.nsight.marketing.sv.application.rule
com.nh.nsight.marketing.sv.persistence.dao
com.nh.nsight.marketing.sv.persistence.mapper

도메인 우선형
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
...
```

## 3.8 구성요소 및 속성

### Project Model

| 속성 | 설명 |
|---|---|
| projectName | 프로젝트명 |
| basePackage | Java BASE 패키지 |
| packageProfile | 현재소스형/도메인우선형 |
| businessCode | 업무코드 |
| moduleName | Gradle/WAR 모듈 |
| contextPath | 업무 URL Context |
| domainCode | ServiceId·클래스의 도메인 구간 |

### Screen/Event Model

| 속성 | 설명 |
|---|---|
| screenId, screenName | 화면 식별 |
| eventId, eventName | 사용자 이벤트 식별 |
| uiObjectId | 버튼·그리드·입력 컴포넌트 |
| successAction | 정상 결과 처리 |
| failureAction | 오류 결과 처리 |
| idempotencyRequired | 중복 요청 차단 여부 |

### Transaction Model

| 속성 | 설명 |
|---|---|
| serviceId | Dispatcher 실행 식별자 |
| transactionCode | 통제·감사·통계 식별자 |
| operation | SELECT_ONE/LIST, INSERT, UPDATE, DELETE |
| methodName | Facade·Service·DAO·Mapper 공통 Method |
| timeoutSeconds | Transaction·Statement 기준 초안 |
| permissionCode | 기능권한 식별 |
| auditRequired | 감사로그 대상 |

### Data Field Model

| 속성 | 설명 |
|---|---|
| name/column | Java·DB 이름 |
| javaType/dbType | 타입 매핑 |
| nullable/pk | 데이터 제약 |
| request/condition/response | DTO·SQL 사용 역할 |
| validation | 필수·길이·형식 규칙 |
| sensitive/maskingRule | 개인정보·금액·마스킹 기준 |
| sampleValue | HTTP 요청·테스트 샘플 |

## 3.9 책임 경계와 RACI

| 활동 | BA | UI | DEV | AA/FW | DA/DBA | SEC | QA | DVO/OPS |
|---|---|---|---|---|---|---|---|---|
| 업무·화면 정의 | A/R | C | C | C | I | I | C | I |
| 도메인·ServiceId | C | I | R | A | C | C | C | I |
| 테이블·컬럼 | C | I | C | C | A/R | C | C | I |
| DTO·프로그램 생성 | I | C | R | A | C | I | C | I |
| Rule 업무로직 | A/C | I | R | C | C | C | C | I |
| SQL·인덱스 | C | I | R | C | A | I | C | I |
| 권한·감사 | C | C | R | C | C | A | C | C |
| 자동검증 규칙 | I | I | C | A/R | C | C | C | C |
| 코드 리뷰·병합 | I | C | R | A | C | C | C | C |
| 통합·성능시험 | C | C | R | C | C | C | A/R | C |
| OM·배포·운영 | I | I | C | C | I | C | C | A/R |

## 3.10 정상 처리 흐름

### M0. Workspace 생성

- 프로젝트명, BASE 패키지, 업무코드, 모듈, Context Path 등록
- 패키지 프로파일 선택
- 완료 Gate: 업무코드·WAR·패키지 매핑 승인

### M1. 도메인 정의

- 도메인 코드·업무명·데이터 소유권 정의
- 동일 도메인에 포함할 ServiceId 범위 정의
- 완료 Gate: 다른 도메인의 DAO/테이블 직접 사용 금지 확인

### M2. 테이블 정의

- 테이블·컬럼·PK·타입·NULL·민감정보 등록
- 실 DB가 있는 경우 향후 Adapter로 Import
- 완료 Gate: DA/DBA 논리·물리 모델 승인

### M3. 화면·이벤트 정의

- 화면 ID·화면명·UI 객체·이벤트·성공/실패 처리 입력
- 완료 Gate: 화면 이벤트마다 호출 ServiceId 또는 비서버 이벤트 여부 확정

### M4. 서비스·거래 정의

- ServiceId·거래코드·처리유형·Timeout·권한·감사 입력
- 완료 Gate: ServiceId 중복 없음, OM 정책 초안 존재

### M5. DTO 매핑

- 각 필드의 요청·조건·응답 역할 선택
- Request·Criteria·Response·Row DTO 구조 자동 결정
- 완료 Gate: 필수·타입·길이·민감정보 규칙 확인

### M6. Rule 정의

- 자동 생성 가능한 필수·길이 검증 정의
- 복잡한 업무 규칙은 별도 규칙 목록으로 개발자에게 할당
- 완료 Gate: 규칙별 오류코드와 정상/오류 예시 존재

### M7. DAO·Mapper·SQL 정의

- DAO Method와 Mapper Statement ID를 1:1로 생성
- 처리유형에 따라 SELECT/INSERT/UPDATE/DELETE 초안 생성
- 완료 Gate: WHERE 조건·변경 컬럼·PK·Timeout 검토

### M8. 자동검증

```text
명명규칙
→ 업무코드 정합성
→ ServiceId·거래코드 중복
→ 화면·이벤트 정합성
→ 처리유형·거래코드 유형
→ PK·조건·요청·응답 완전성
→ 민감정보 마스킹
→ 변경 거래 감사대상
```

오류가 1개라도 있으면 생성 승인을 차단한다.

### M9. 코드·산출물 생성

- 동일 업무·도메인 모델을 그룹화
- 도메인 Handler에 ServiceId 상수·목록·switch 병합
- Facade·Service·Rule·DAO·Mapper에 Method 병합
- 거래별 DTO·SQL·문서 생성

### M10. Diff·코드 리뷰

- 생성 ZIP을 대상 Branch에 적용
- 수동 구현과 충돌 여부 확인
- 업무 규칙·SQL·보안·운영 항목 리뷰
- 완료 Gate: Compile·Unit Test·구조검사 통과

### M11. 통합 검증

- 대표 표준 요청으로 TCF 전체 흐름 시험
- 업무오류·DB오류·Timeout·중복요청·권한 오류 시험
- 화면 이벤트와 응답 처리 검증

### M12. OM·배포·운영 전환

- OM Service Catalog·거래통제·Timeout·감사 등록
- Commit·Artifact·ServiceId·배포이력 연결
- 운영 로그에서 GUID·TraceId·ServiceId·SQL ID 추적 검증

## 3.11 오류·Timeout·장애 흐름

### 모델 검증 오류

```text
모델 입력
→ Validation Engine
→ ERROR 발견
→ 생성 차단
→ 필드 경로·오류코드·조치 메시지 표시
→ 수정 후 재검증
```

### 생성 오류

- 템플릿 변수 누락: 시스템 오류로 생성 중단
- 동일 파일 경로 충돌: Workspace 오류로 차단
- 지원하지 않는 타입: 모델 오류로 차단
- 저장소 쓰기 실패: 기존 모델 보존, 임시파일 제거

### 실제 거래 Timeout

자동생성 초안은 다음 Timeout 계층을 구분한다.

```text
TCF 전체 거래 Timeout
  ≥ Facade Transaction Timeout
  ≥ Mapper Statement Timeout
  ≥ DB Connection 획득·외부호출 개별 Timeout
```

단순히 모두 같은 숫자로 생성하는 것은 초기 초안일 뿐이며, 연계가 있는 거래는 예산분해 검토가 필요하다.

### 장애 시 복구

- Model Repository 백업본으로 복원
- 마지막 Baseline 모델과 Template 버전으로 재생성
- manifest 파일로 생성 파일 목록과 ServiceId 확인
- Git에서 수동 변경과 생성 변경을 분리하여 Rollback

## 3.12 정상 예시

```text
화면 ID        SV-CUS-0001
이벤트 ID      SV-CUS-0001-E01
ServiceId      SV.Customer.selectSummary
거래코드       SV-INQ-0001
업무/도메인    SV / Customer
메서드         selectCustomerSummary
테이블         SV_CUSTOMER
조건           customerNo → CUSTOMER_NO
응답           customerName, customerGrade, totalBalance
```

생성 결과:

```text
SvCustomerHandler.serviceIds()
  → SV.Customer.selectSummary
SvCustomerFacade.selectCustomerSummary()
SvCustomerService.selectCustomerSummary()
SvCustomerRule.buildCustomerSummaryCriteria()
SvCustomerDao.selectCustomerSummary()
SvCustomerMapper.selectCustomerSummary
SV_CUSTOMER
```

## 3.13 금지 예시

| 금지 | 이유 |
|---|---|
| 화면별 업무 Controller 생성 | 공통 TCF 진입·통제 우회 |
| ServiceId마다 Handler 클래스 생성 | 도메인 응집도 저하·클래스 폭증 |
| Handler에 SQL·업무규칙 작성 | 책임 혼합·테스트 어려움 |
| Service에서 Mapper 직접 호출 | DAO 경계·추적성 훼손 |
| Rule에서 DB·외부 API 호출 | 부작용 없는 규칙 원칙 위반 |
| UPDATE/DELETE에 WHERE 조건 없음 | 대량 데이터 훼손 위험 |
| 생성 파일에 대규모 수동 로직 추가 | 재생성 시 소실·충돌 |
| 민감정보를 샘플값에 실데이터로 입력 | 개인정보 유출 위험 |
| OM 등록 없이 코드만 배포 | 거래통제·Timeout·운영 추적 누락 |
| 검증 오류 상태에서 강제 생성 | 구조적 결함을 코드로 확산 |

## 3.14 연계 규칙

### UI 연계

- 화면 이벤트는 `StandardRequest.header.serviceId`를 명시한다.
- 화면 Header의 사용자 정보는 JWT/인증문맥과 정합성을 유지한다.
- URL 또는 개별 Controller 이름으로 업무를 식별하지 않는다.

### TCF 연계

- 생성 Handler는 `TransactionHandler` 계약을 구현한다.
- 동일 도메인의 ServiceId를 `serviceIds()`에 등록한다.
- 미지원 ServiceId는 `SERVICE_NOT_FOUND`로 차단한다.

### OM 연계

- ServiceId, 거래코드, 업무코드, 화면·이벤트, Timeout, 감사 여부를 생성한다.
- 운영 등록 전에 코드의 Handler 등록정보와 비교한다.

### DB 연계

- DAO Method와 Mapper SQL ID는 동일하게 유지한다.
- Mapper namespace는 Java Mapper FQCN과 일치한다.
- 테이블 변경은 역추적 매트릭스로 영향 화면·ServiceId를 식별한다.

### Git·CI/CD 연계

- 모델 JSON, 템플릿 버전, 생성 manifest를 함께 버전관리한다.
- 생성 결과는 별도 Commit으로 만들어 리뷰 가능하게 한다.
- Build, Unit Test, ArchUnit, SQL 정적검사 후 Merge한다.

## 3.15 데이터 및 상태관리

### 권장 상태

```text
DRAFT
→ VALIDATED
→ REVIEWED
→ APPROVED
→ GENERATED
→ MERGED
→ TESTED
→ RELEASED
→ RETIRED
```

| 상태 | 전환조건 |
|---|---|
| DRAFT | 최초 입력 |
| VALIDATED | 자동 오류 0건 |
| REVIEWED | BA·DEV·AA·DA 검토 완료 |
| APPROVED | 승인자 Baseline 확정 |
| GENERATED | 승인 모델로 산출물 생성 |
| MERGED | Git 리뷰·병합 완료 |
| TESTED | 단위·통합·거래시험 통과 |
| RELEASED | OM 등록·배포 완료 |
| RETIRED | 화면·ServiceId·SQL 폐기 완료 |

### 버전관리

- Model Version: 업무 정의 변경 이력
- Template Version: 코드 생성 구조 변경 이력
- Framework Version: TCF 계약 호환 버전
- Artifact Version: 빌드·배포 산출물 버전

운영형 저장소는 모델을 덮어쓰지 않고 변경 이력과 승인자를 보존해야 한다.

## 3.16 성능·용량·확장성

MVP는 로컬 개발도구이므로 수백 개 모델 수준을 대상으로 한다. 운영형 중앙 도구는 다음 기준을 권장한다.

| 항목 | 초기 기준 |
|---|---|
| 모델 수 | 10,000개 이상 |
| 동시 편집자 | 50명 이상 |
| Workspace 생성 | 1,000 거래 기준 60초 이내 목표 |
| 미리보기 | 단일 거래 2초 이내 |
| 검색 | ServiceId·화면·테이블 검색 p95 2초 이내 |
| 저장 | 낙관적 Lock으로 동시수정 충돌 방지 |
| 생성 | 작업 Queue와 Template Cache 적용 |

대규모 프로젝트에서는 전체 Workspace를 매번 생성하지 않고 변경 모델과 같은 도메인의 파일만 증분 생성한다.

## 3.17 보안·개인정보·감사

1. 운영 비밀번호·JWT·Private Key를 모델에 저장하지 않는다.
2. 샘플값에는 실제 고객정보를 입력하지 않는다.
3. 민감정보 필드는 분류·마스킹·감사 대상 여부를 필수 검토한다.
4. 모델 조회·수정·승인·생성·폐기 이력을 감사로그로 기록한다.
5. 승인자와 생성자는 분리할 수 있어야 한다.
6. 실 DB Import Adapter는 읽기전용 계정과 스키마 Allowlist를 사용한다.
7. 생성된 SQL과 문서에 개인정보 Sample이 포함되지 않도록 DLP 검사를 적용한다.
8. 중앙 도구는 SSO·RBAC·TLS·암호화 저장을 적용한다.

## 3.18 운영·모니터링·장애 대응

운영형 Model Studio는 다음 지표를 수집한다.

| 지표 | 목적 |
|---|---|
| 모델 저장 성공/실패 | Repository 상태 |
| 검증 오류 코드별 건수 | 표준 미준수 추세 |
| 생성시간·파일수 | Generator 성능 |
| Template별 실패율 | 템플릿 결함 탐지 |
| 승인 대기시간 | 개발 병목 식별 |
| 생성 후 Compile 실패율 | 모델·템플릿 품질 |
| 운영 ServiceId와 모델 불일치 | Drift 탐지 |

장애 대응 순서는 Repository → Validation → Generator → Adapter 순으로 격리한다. 생성기 장애가 모델 편집과 조회를 중단시키지 않도록 분리하는 것이 목표다.

## 3.19 자동검증 및 품질 Gate

### Gate 1. 모델 완전성

- 필수 속성 존재
- 이름·코드 형식
- 필드 타입·컬럼 중복
- PK·조건·요청·응답 존재

### Gate 2. 추적성

- 화면 이벤트 ↔ ServiceId
- ServiceId ↔ Handler Method
- Method ↔ Mapper SQL ID
- SQL ID ↔ Table
- ServiceId ↔ OM Catalog

### Gate 3. 구조

- Handler는 Facade만 호출
- Facade에 Transaction 경계
- Service는 Rule·DAO·Client만 호출
- Rule은 외부 의존 없음
- DAO는 Mapper만 호출

### Gate 4. SQL

- UPDATE/DELETE WHERE 조건
- Parameter Binding 사용
- Statement Timeout 존재
- SELECT 응답 컬럼과 Row DTO 정합성
- PK·인덱스 검토 필요 표시

### Gate 5. 보안·운영

- 변경 거래 감사대상
- 민감정보 마스킹 규칙
- 권한코드 존재
- 오류코드·Timeout·멱등성 검토

### Gate 6. 빌드·테스트

- Java Compile
- Unit Test
- ArchUnit/Checkstyle
- Mapper XML Parser
- ServiceId 중복검사
- 표준 HTTP 거래 Smoke Test

## 3.20 테스트 시나리오

| 분류 | 시나리오 | 기대결과 |
|---|---|---|
| 모델 | 필수 ServiceId 누락 | 생성 차단 |
| 명명 | 업무코드와 ServiceId Prefix 불일치 | 오류 표시 |
| 중복 | 동일 ServiceId 2개 | Workspace 생성 차단 |
| Handler | 동일 도메인 ServiceId 2개 | Handler 1개에 병합 |
| DTO | 요청·조건·응답 조합 | 올바른 DTO별 필드 생성 |
| SQL | 단건조회 | SELECT·WHERE·resultType 생성 |
| SQL | UPDATE 조건 없음 | 검증 오류 |
| 감사 | UPDATE인데 Audit=N | 경고 또는 정책상 오류 |
| 컴파일 | 생성 Java | 계약 Stub 기준 Compile 성공 |
| Mapper | Namespace와 Interface | FQCN 일치 |
| 추적성 | 화면→테이블 | CSV 한 행에서 전체 연결 |
| 재현성 | 같은 모델 2회 생성 | 동일 파일 목록·내용 |
| 장애 | Repository JSON 손상 | 빈 목록 또는 백업 복원, 원본 덮어쓰기 방지 |
| 보안 | 민감 필드 마스킹 없음 | 경고 표시 |

## 3.21 체크리스트

### 모델링 전

- [ ] 업무코드·WAR·Context가 확정되었다.
- [ ] 도메인과 데이터 소유권이 확정되었다.
- [ ] 화면·ServiceId 명명규칙이 승인되었다.

### 생성 전

- [ ] 자동검증 오류가 0건이다.
- [ ] 화면 이벤트와 ServiceId가 연결되었다.
- [ ] 변경 거래에 감사·권한이 정의되었다.
- [ ] 조회·변경 SQL에 안전한 조건이 있다.
- [ ] 민감정보 마스킹 기준이 있다.

### 병합 전

- [ ] 생성 Diff를 검토했다.
- [ ] 기존 도메인 Handler와 중복되지 않는다.
- [ ] 업무 규칙을 Rule에 보완했다.
- [ ] SQL과 인덱스를 DA/DBA가 검토했다.
- [ ] Compile·Unit Test·구조검사를 통과했다.

### 운영 전환 전

- [ ] OM Service Catalog가 코드와 일치한다.
- [ ] Timeout·거래통제·감사 정책이 등록되었다.
- [ ] 정상·오류·Timeout·권한 시험을 완료했다.
- [ ] 거래로그에서 ServiceId·GUID·SQL ID를 추적할 수 있다.
- [ ] 배포·Rollback·폐기 절차가 준비되었다.

## 3.22 변경·호환성·폐기 관리

### 모델 변경

모델 변경은 식별자 변경과 비식별자 변경으로 구분한다.

| 변경 | 처리 |
|---|---|
| 화면명·설명 | 문서 재생성, 호환 영향 낮음 |
| 응답 필드 추가 | 소비 화면 호환성 검토 |
| 필드 삭제·타입 변경 | Breaking Change, 버전 또는 신규 ServiceId 검토 |
| ServiceId 변경 | 기존 ServiceId 폐기·호환 기간 필요 |
| 거래코드 변경 | OM·로그·통계 영향 검토 |
| 테이블·컬럼 변경 | 역추적 후 전체 회귀시험 |
| 패키지 프로파일 변경 | 대규모 Import·Component Scan 영향, 별도 전환계획 |
| 템플릿 변경 | Golden Sample 회귀시험 후 배포 |

### ServiceId 폐기

```text
신규 호출 차단 공지
→ UI·연계 사용처 제거
→ OM 사용중지
→ 로그 관찰 기간
→ Handler serviceIds() 제거
→ Mapper·SQL·테스트 제거
→ 추적성 상태 RETIRED
```

### 템플릿 호환성

템플릿은 TCF Framework 계약 버전과 함께 관리한다. `TransactionHandler`, `StandardRequest`, `TransactionContext`, 오류 계약이 변경되면 Generator 호환성을 먼저 검증한다.

---

# 4. 시사점

## 4.1 핵심 아키텍처 판단

1. 자동화의 기준점은 테이블 단독도 화면 단독도 아닌 **화면 이벤트–ServiceId–데이터 관계 모델**이어야 한다.
2. 생성기의 가장 중요한 기능은 파일 생성량이 아니라 **책임 경계와 추적성을 강제하는 것**이다.
3. 동일 도메인의 ServiceId를 하나의 Handler로 병합해야 현재 TCF 소스 구조와 정합성을 유지할 수 있다.
4. 업무 규칙과 SQL 성능은 완전 자동화 대상이 아니라 사람의 검토가 필요한 설계 영역이다.
5. Model Repository와 Template 버전이 공식 SoT가 되어야 재현·변경·폐기 관리가 가능하다.

## 4.2 주요 위험

| 위험 | 대응 |
|---|---|
| 생성 코드 맹신 | 승인·리뷰 Gate 의무화 |
| 생성 파일 수동 수정 소실 | 생성/수동 소유영역 분리 |
| 실제 소스와 템플릿 Drift | Golden Sample Compile·Diff 자동화 |
| 잘못된 DB 모델 확산 | DA/DBA 승인 없이는 Baseline 금지 |
| 권한·감사 누락 | 변경 거래 정책을 검증 오류로 상향 가능 |
| 도구 자체가 병목 | CLI·API·Batch 생성 병행 |
| 모델과 운영 OM 불일치 | 정기 Drift 비교 |

## 4.3 우선 보완 과제

1. 실제 NSIGHT Git 저장소 Adapter와 Diff/Merge 기능
2. Oracle Dictionary Import 및 테이블 모델 동기화
3. OM Service Catalog API 연계
4. 보호영역 또는 AST 기반 안전한 재생성
5. 상태·승인·버전 이력 Repository
6. ArchUnit·Mapper·ServiceId CI Plugin
7. WEBTOPSUITE/React 화면 정의 Import
8. 오류코드·공통코드·권한 모델 연계

## 4.4 중장기 발전 방향

```text
1단계: 로컬 코드·문서 생성 MVP
2단계: 중앙 Model Repository + SSO/RBAC
3단계: DB·Git·OM Adapter + CI/CD Gate
4단계: 영향도 그래프·Drift 탐지·증분 생성
5단계: 사내 LLM/DAVIS-CODER 연계
       - 모델 설명 보완
       - Rule 초안 제안
       - 테스트케이스 생성
       - 코드리뷰 보조
       단, 승인 모델과 표준 템플릿을 최우선 근거로 사용
```

---

# 5. 마무리말

NSIGHT TCF 개발 자동화는 개발자를 없애는 도구가 아니라, 개발자가 반복적인 클래스·DTO·Mapper·문서 작성에서 벗어나 업무 규칙, 데이터 품질, SQL 성능, 보안과 운영 가능성에 집중하도록 만드는 개발 플랫폼이다.

성공적인 도입을 위해서는 다음 순서를 지켜야 한다.

```text
모델을 먼저 확정하고,
자동검증으로 오류를 차단하며,
표준 템플릿으로 생성하고,
사람이 업무·데이터·보안을 승인하고,
CI/CD가 코드와 운영 기준정보의 정합성을 다시 검증한다.
```

현재 MVP는 이 전체 방향 중 **모델 입력 → 자동검증 → 도메인 병합 생성 → 추적성·산출물 ZIP**까지 실행 가능하게 구현한 기준본이다.
