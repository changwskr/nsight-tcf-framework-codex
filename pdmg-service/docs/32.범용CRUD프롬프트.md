# PDMG 범용 CRUD 프롬프트

이 문서는 AI에게 `pdmg-service`의 CRUD 기능을 정확하고 안전하게 설계·구현하도록 요청하기 위한 재사용 프롬프트다. 단순히 “CRUD를 만들어 달라”고 요청하는 대신 업무 규칙, DB 계약, 서비스 ID, 트랜잭션, 오류 처리와 검증 기준을 명시하여 추정 구현을 방지한다.

---

## 1. 사용 방법

1. 이 문서의 **요구사항 입력 양식**을 먼저 작성한다.
2. 미정 항목은 임의 값으로 채우지 말고 `[미정 — 질문 필요]`로 표시한다.
3. 요구사항이 확정되면 **범용 CRUD 최종 프롬프트**의 대괄호 부분을 실제 값으로 바꾼다.
4. AI에게 먼저 조사·설계·영향 파일을 보고하게 하고, 승인 후 구현하게 한다.
5. 구현이 끝나면 마지막 체크리스트로 결과를 검수한다.

AI가 테이블, PK, 업무 규칙, 서비스 ID, 권한 또는 오류 코드를 확정할 근거가 없다면 추정하지 않고 질문해야 한다.

---

## 2. 기본 아키텍처 계약

현행 `pdmg-service`의 기본 호출 구조는 다음과 같다.

```text
HTTP { hdr_nhnis, dto }
  → DefaultFilter / ServicePreventionInterceptor
  → OnlineTransactionController (TCF ON)
  → TransactionDispatcher
  → TransactionHandler
  → Business Facade (@Transactional)
  → BizPrePostAspect
  → Business Service
  → DAO / MyBatis Mapper
  → ResponseBodyAdvice
  → HTTP { hdr_nhnis, dto } 또는 { hdr_nhnis, result }
```

계층별 책임은 다음과 같다.

| 계층 | 책임 | 금지사항 |
|---|---|---|
| Handler | `serviceId` 등록·분기, Facade 호출 | SQL, 상세 업무 규칙, 트랜잭션 선언 |
| Facade | `Object` → `DTOin` 변환, 유스케이스 조정, TX 경계 | SQL 직접 실행 |
| Service | 검증, 계산, 업무 흐름, DAO 호출 | HTTP 요청·응답 조립 |
| DAO | Mapper 인터페이스 계약 | 업무 정책 판단 |
| Mapper XML | SQL과 DB 매핑 | 문자열 치환 기반의 위험한 SQL |
| DTO | 입력·출력 데이터 계약 | DB 접근, Service 호출 |

기본 의존 방향:

```text
entry.handler
  → application.facade
    → application.service
      → persistence.dao
        → MyBatis XML
```

---

## 3. 작업 범위 및 보호 원칙

```text
작업 대상 모듈: [예: pdmg-service]
연계 변경 허용 모듈: [없음 / pdmg-ui / pdmg-fw / 기타]
변경 금지 모듈: [예: pdmg-fw, pdmg-ui]
기존 변경 보존: Y
신규 파일 생성 허용: [Y/N]
기존 API 호환 유지: [Y/N]
DB Schema 변경 허용: [Y/N]
```

반드시 지킬 원칙:

- 기존 미커밋 변경은 사용자 소유로 보고 덮어쓰거나 되돌리지 않는다.
- 관련 없는 리팩터링, 이름 변경과 포맷 변경을 섞지 않는다.
- 공통 Framework 기능을 업무 모듈에 복제하지 않는다.
- 공개 전문, 설정 키 또는 DB Schema가 바뀌면 호환성과 롤백 방법을 기록한다.
- Secret, Token, 비밀번호와 개인정보를 코드·문서·로그에 기록하지 않는다.

---

## 4. 요구사항 입력 양식

### 4.1 기본 정보

```text
프로그램 ID: [예: mgcoa9000]
업무명: [예: 거래 파라미터 관리]
업무 설명: [관리 대상과 목적]
Java 기본 패키지: [예: nhnis.mg.co.a]
작업 범위: [전체 CRUD / 목록 / 상세 / 등록 / 수정 / 삭제]
참조 프로그램: [예: mgcoa8888]
TCF 사용: [ON / OFF / 양쪽 호환]
```

참조 프로그램에서는 패키지 구조와 구현 패턴만 참고한다. 테이블, PK, 필드, 업무 규칙, 서비스 ID와 오류 코드를 복사하지 않는다.

### 4.2 API와 서비스 ID

| 기능 | HTTP Method | URL | serviceId | 필요 여부 |
|---|---|---|---|---|
| 목록 | POST | `[/프로그램IDS0]` | `[프로그램IDS0]` | `[Y/N]` |
| 상세 | POST | `[/프로그램IDS1]` | `[프로그램IDS1]` | `[Y/N]` |
| 등록 | POST | `[/프로그램IDC0]` | `[프로그램IDC0]` | `[Y/N]` |
| 수정 | POST | `[/프로그램IDU0]` | `[프로그램IDU0]` | `[Y/N]` |
| 삭제 | POST | `[/프로그램IDD0]` | `[프로그램IDD0]` | `[Y/N]` |

```text
serviceId 결정 규칙: [헤더 rms_svc_c / URL / 기타]
한 도메인 Handler에서 처리할 serviceId 목록: [목록]
기존 serviceId와 중복 여부: [확인 결과]
```

### 4.3 DB 정보

```text
운영 DB: [Oracle / 기타]
로컬 DB: [H2 Oracle Mode / 기타]
테이블명: [테이블]
PK: [컬럼 목록]
UK 또는 업무 중복 기준: [컬럼 목록 / 없음]
삭제 방식: [물리 삭제 / 논리 삭제]
논리 삭제 컬럼과 값: [예: DEL_YN, Y/N / 해당 없음]
동시성 제어: [버전 / 수정시각 / 잠금 / 미사용]
감사 컬럼: [등록자, 등록일시, 수정자, 수정일시]
```

컬럼 정의:

| DB 컬럼 | Java 필드 | DB 타입 | Java 타입 | PK | NULL | 길이/범위 | 입력 검증 | 민감정보 | 설명 |
|---|---|---|---|---|---|---|---|---|---|
| `[TX_ID]` | `[txId]` | `[VARCHAR2(30)]` | `[String]` | `[Y]` | `[N]` | `[30]` | `[필수]` | `[N]` | `[거래 ID]` |

### 4.4 목록 조회

```text
검색 조건: [필드와 정확/부분/범위 검색 방식]
기본 정렬: [예: CHG_DTM DESC, TX_ID ASC]
페이지 번호 시작: [1]
기본 pageSize: [20]
최대 pageSize: [100]
0건 처리: [빈 목록 + totalCount=0 / 오류]
목록 응답 필드: [필드 목록]
```

목록 SQL과 Count SQL은 검색 조건이 반드시 같아야 한다. 정렬은 같은 값이 존재해도 순서가 안정적이도록 PK를 마지막 정렬 기준에 포함한다.

### 4.5 상세 조회

```text
조회 키: [PK 전체]
필수 입력: [필드]
0건 처리: [오류 코드 / 빈 결과]
응답 필드: [필드]
```

### 4.6 등록

```text
필수 필드: [필드]
중복 기준: [PK / UK / 업무 기준]
중복 오류 코드: [코드]
서버 생성값: [등록일시, 등록자 등]
등록 성공 기준: [영향 행 수]
등록 후 반환: [처리 건수 / 생성 데이터 / 상세 재조회]
```

### 4.7 수정

```text
PK: [필드]
수정 가능 필드: [필드]
수정 금지 필드: [PK, 등록자, 등록일시 등]
동시성 조건: [버전/수정일시/없음]
0건 또는 충돌 오류 코드: [코드]
수정 성공 기준: [영향 행 수]
```

PK 없는 UPDATE와 무조건 전체 행 UPDATE는 금지한다.

### 4.8 삭제

```text
PK: [필드]
삭제 방식: [물리/논리]
삭제 전 확인 조건: [연관 데이터/상태/권한]
대상 0건 처리: [멱등 성공 / 오류 코드]
삭제 불가 오류 코드: [코드]
복수 삭제: [허용/불허, 최대 건수]
```

PK나 승인된 업무 범위 조건이 없는 DELETE/UPDATE는 금지한다.

### 4.9 오류 코드

| 상황 | 오류 코드 | HTTP 상태 | 메시지 | 처리 방식 |
|---|---|---:|---|---|
| 필수값 누락 | `[코드]` | `[400/500/현행 정책]` | `[메시지]` | `[BizException/결과 DTO]` |
| 대상 없음 | `[코드]` | `[...]` | `[...]` | `[...]` |
| 중복 | `[코드]` | `[...]` | `[...]` | `[...]` |
| 동시성 충돌 | `[코드]` | `[...]` | `[...]` | `[...]` |
| 삭제 불가 | `[코드]` | `[...]` | `[...]` | `[...]` |
| 권한 없음 | `[예: FW0403]` | `[...]` | `[...]` | `[...]` |
| 시스템 오류 | `FW9999` | `500` | 공통 메시지 | 공통 처리 |

예외 응답 계약:

```text
정상: { hdr_nhnis, dto }
예외: { hdr_nhnis, result }
```

업무 실패를 `RSLT_CD`로 정상 반환할지 `BizException`으로 처리할지는 거래별로 명시한다. AI가 임의로 선택하지 않는다.

### 4.10 트랜잭션과 타임아웃

| 기능 | Facade 트랜잭션 | 추가 TX timeout |
|---|---|---|
| 목록 | `@Transactional(transactionManager="rdwTransactionManager", readOnly=true)` | `[없음/초]` |
| 상세 | `@Transactional(transactionManager="rdwTransactionManager", readOnly=true)` | `[없음/초]` |
| 등록 | `@Transactional(transactionManager="rdwTransactionManager", rollbackFor=Exception.class)` | `[없음/초]` |
| 수정 | `@Transactional(transactionManager="rdwTransactionManager", rollbackFor=Exception.class)` | `[없음/초]` |
| 삭제 | `@Transactional(transactionManager="rdwTransactionManager", rollbackFor=Exception.class)` | `[없음/초]` |

공통 온라인 SLA는 `nhnis.fw.timeout.*`의 `OnlineTimeoutExecutor`가 담당한다. 특정 거래의 Spring TX timeout은 명시적인 요구가 있을 때만 Facade에 추가한다.

쓰기 작업에서 여러 SQL 중 하나가 실패하면 전체를 롤백해야 한다.

### 4.11 보안과 감사

```text
인증 필요: [Y/N]
필요 권한: [권한]
사용자 ID 출처: [ServiceContext header / SecurityContext]
지점 코드 출처: [필드]
민감정보 필드: [필드]
로그 마스킹: [규칙]
등록/수정 사용자 저장: [Y/N]
감사 로그 요구: [내용]
```

---

## 5. 구현 규칙

### 5.1 DTO

- 패키지는 `nhnis.mg.co.a.dto`를 사용한다.
- 거래별로 `{serviceId}DTOin`, `{serviceId}DTOout`을 분리한다.
- 목록 행은 `{serviceId}DTOSub0`을 사용한다.
- JSON 업무 필드는 기본적으로 camelCase를 사용한다.
- 레거시 필드 호환이 필요하면 `@JsonAlias`를 명시하고 표준 출력 이름은 하나로 유지한다.
- DataObject의 보조 `*List`, `*Array` Getter가 실제 와이어 필드와 충돌하지 않도록 검증한다.
- DTO에 DB 접근이나 업무 로직을 넣지 않는다.

### 5.2 Handler

- 업무 도메인당 하나의 `TransactionHandler`를 사용한다.
- `serviceIds()`에 실제 거래 ID를 등록한다.
- `context.getServiceId()` 기준으로 Facade를 분기 호출한다.
- 등록되지 않은 ID는 공통 예외로 처리한다.
- Handler에는 SQL, DTO 필드 조립, 트랜잭션을 넣지 않는다.

### 5.3 Facade

- `ObjectMapper.convertValue(dtoBody, DTOin.class)`로 입력을 변환한다.
- 조회는 `readOnly=true`, 쓰기는 `rollbackFor=Exception.class`를 사용한다.
- `rdwTransactionManager`를 명시한다.
- Service 호출을 트랜잭션 범위 안에 둔다.
- self-invocation으로 `@Transactional`을 우회하지 않는다.

### 5.4 Service

- 입력 정규화, 필수값, 길이, 값 범위와 업무 규칙을 검증한다.
- 목록 `pageNo/pageSize` 기본값과 최대값을 보정한다.
- DAO 결과를 DTOout/DTOSub0으로 변환한다.
- 업무 예외 정책에 따라 `BizException` 또는 명시된 결과 DTO를 사용한다.
- 트랜잭션 경계는 Service가 아니라 Facade에 둔다.

### 5.5 DAO와 MyBatis

- Mapper namespace는 DAO 인터페이스 FQCN과 정확히 일치시킨다.
- DAO 메서드명과 XML statement ID를 일치시킨다.
- `#{...}` 파라미터 바인딩을 사용한다.
- 사용자 입력을 `${...}`로 SQL에 직접 삽입하지 않는다.
- 컬럼 Alias와 Java 필드 매핑을 명시한다.
- UPDATE/DELETE에는 PK 또는 승인된 업무 범위 조건을 반드시 넣는다.
- 목록 SQL과 Count SQL의 검색 조건을 일치시킨다.
- Oracle 운영 SQL과 H2 Oracle Mode에서의 실행 가능성을 확인한다.

### 5.6 설정과 문서

- 신규 오류 코드는 `exceptionCode.yml`에 추가한다.
- H2 테스트에 테이블이나 데이터가 필요하면 `schema.sql`, `data.sql`을 최소 범위로 수정한다.
- serviceId 변경 시 Handler, UI/Catalog, 샘플 요청과 문서 사용처를 함께 검색한다.
- 설정 변경 시 공통/local/dev 차이와 환경변수 우선순위를 확인한다.

---

## 6. 테스트 시나리오

### 6.1 공통

- DTO JSON 바인딩과 레거시 Alias
- 필수값 누락, 공백, 최대 길이와 허용값
- 등록되지 않은 serviceId
- 표준 정상 응답 `{hdr_nhnis, dto}`
- 표준 오류 응답 `{hdr_nhnis, result}`
- 민감정보가 로그와 오류 응답에 노출되지 않음

### 6.2 조회

- 기본 페이지 값
- 최대 pageSize 보정
- 검색 조건과 정렬
- 0건 빈 목록
- 목록 SQL과 Count SQL 조건 일치
- 상세 정상, 필수키 누락, 0건

### 6.3 등록

- 정상 등록
- 필수값 누락
- PK/UK 중복
- 후속 SQL 실패 시 전체 롤백

### 6.4 수정

- 정상 수정
- PK 누락
- 대상 0건
- 동시성 충돌
- 수정 금지 필드 불변
- 후속 SQL 실패 시 전체 롤백

### 6.5 삭제

- 정상 삭제
- PK 누락
- 대상 0건 정책
- 삭제 불가 조건
- 복수 삭제 최대 건수
- 후속 SQL 실패 시 전체 롤백

### 6.6 검증 명령

작은 단위부터 실행한다.

```powershell
cd pdmg-service
.\gradlew.bat test
.\gradlew.bat compileJava
.\gradlew.bat war
```

`NO-SOURCE`는 테스트 성공이 아니라 테스트 소스가 없다는 뜻이다. 실행할 수 없는 검증은 명령, 실패 원인과 미검증 범위를 보고한다.

---

## 7. 범용 CRUD 최종 프롬프트

아래 블록을 복사하고 대괄호를 실제 요구사항으로 교체한다.

```text
pdmg-service에 [프로그램 ID / 업무명] CRUD 기능을 구현해줘.

[작업 범위]
- 대상 모듈: [pdmg-service]
- 허용 범위: [대상 파일/모듈]
- 변경 금지: [pdmg-fw, pdmg-ui 등]
- 기존 미커밋 변경을 보존하고 관련 없는 리팩터링을 하지 않는다.

[업무/API]
- 프로그램 ID: [값]
- 업무 설명: [값]
- 기능: [목록/상세/등록/수정/삭제]
- serviceId: [S0/S1/C0/U0/D0 목록]
- 요청: { hdr_nhnis, dto }
- 정상 응답: { hdr_nhnis, dto }
- 오류 응답: { hdr_nhnis, result }
- 참조 프로그램: [값, 구조만 참고]

[DB]
- 운영/로컬 DB: [Oracle / H2 Oracle Mode]
- 테이블: [값]
- PK: [값]
- UK/중복 기준: [값]
- 삭제 방식: [물리/논리]
- 동시성 기준: [값]
- 컬럼 정의:
[컬럼 표 붙여넣기]

[조회]
- 검색 조건: [값]
- 기본 정렬: [값]
- pageNo/pageSize/최대값: [값]
- 목록 0건 정책: [값]
- 상세 조회 키와 0건 정책: [값]

[등록/수정/삭제]
- 등록 필수값과 중복 정책: [값]
- 수정 가능/금지 필드와 0건 정책: [값]
- 삭제 조건과 0건/삭제불가 정책: [값]
- 여러 쓰기 SQL 중 하나라도 실패하면 전체 rollback한다.

[오류/보안]
- 오류 코드와 메시지: [표 붙여넣기]
- 업무 실패 처리: [BizException / RSLT_CD 정상 반환]
- 인증/권한: [값]
- 사용자/지점 정보 출처: [값]
- 민감정보와 마스킹: [값]

[아키텍처]
- Handler → Facade(@Transactional) → Service → DAO/MyBatis 방향을 지킨다.
- Handler는 serviceId 분기와 Facade 호출만 담당한다.
- Facade는 DTO 변환과 트랜잭션 경계를 담당한다.
- 조회 Facade는 rdwTransactionManager, readOnly=true를 사용한다.
- 쓰기 Facade는 rdwTransactionManager, rollbackFor=Exception.class를 사용한다.
- 공통 timeout은 nhnis.fw.timeout OnlineTimeoutExecutor를 사용한다.
- 특정 거래 TX timeout은 [없음/초]이며, 미정이면 임의로 추가하지 않는다.
- 업무 선후처리는 Service Pointcut에서 Facade TX 안에 유지한다.

[DAO/MyBatis]
- DAO FQCN과 Mapper namespace, DAO 메서드와 statement ID를 일치시킨다.
- #{...} 바인딩을 사용하고 사용자 입력을 문자열 치환하지 않는다.
- UPDATE/DELETE는 PK 또는 승인된 업무 범위 조건 없이 작성하지 않는다.
- 목록과 Count SQL 조건을 일치시킨다.
- Oracle과 H2 Oracle Mode 호환성을 검증한다.

[진행 방식]
1. 먼저 AGENTS.md, README, build.gradle, application.yml, 네이밍 문서와 참조 구현을 조사한다.
2. git status로 사용자 변경을 확인한다.
3. 요구사항 중 결과를 바꾸는 미정 정보는 추정하지 말고 한 번에 하나씩 질문한다.
4. 구현 전에 호출 흐름, DTO, SQL, TX, 오류, 테스트 설계와 영향 파일을 제시하고 승인받는다.
5. 실패하는 테스트를 먼저 작성하고 실패 원인을 확인한다.
6. 승인된 최소 범위만 구현한다.
7. 관련 테스트, compileJava와 WAR 빌드를 실제 실행한다.
8. 변경 파일, API 예시, 트랜잭션 범위, 오류 코드, 실행 명령과 실제 결과를 보고한다.

[완료 조건]
- 정상 CRUD뿐 아니라 필수값, 0건, 중복, 충돌, 삭제불가와 rollback 테스트가 있다.
- 서비스 ID, Handler, Facade, DAO, Mapper와 샘플 요청이 일치한다.
- Secret, Token, 개인정보와 내부 Stack Trace가 응답이나 로그에 노출되지 않는다.
- 테스트를 실행하지 않았거나 NO-SOURCE이면 성공으로 표현하지 않는다.

먼저 구현하지 말고 저장소 조사 결과, 미정 질문, 설계, 테스트 시나리오와 영향 파일을 보여줘.
```

---

## 8. 기능별 축약 프롬프트

### 8.1 목록 조회

```text
pdmg-service의 [프로그램ID] 목록 조회를 구현해줘.
serviceId는 [S0], 테이블은 [테이블], 검색조건은 [조건], 정렬은 [정렬]이다.
pageNo 기본 1, pageSize 기본 [20], 최대 [100]이며 0건은 빈 목록과 totalCount=0을 반환한다.
Facade는 rdwTransactionManager readOnly TX를 사용하고 목록/Count SQL 조건을 일치시킨다.
Handler → Facade → Service → DAO/MyBatis 구조와 {hdr_nhnis,dto} 전문을 유지한다.
먼저 현행 패턴과 영향 파일을 조사하고 설계 승인 후 테스트 우선으로 구현해줘.
```

### 8.2 상세 조회

```text
pdmg-service의 [프로그램ID] 상세 조회를 구현해줘.
serviceId는 [S1], 조회 키는 [PK 전체], 0건은 [오류 코드/빈 결과]로 처리한다.
Facade readOnly TX, 안전한 MyBatis 바인딩과 정상/필수키 누락/0건 테스트를 적용한다.
미정된 오류 코드나 필드는 추정하지 말고 먼저 질문해줘.
```

### 8.3 등록

```text
pdmg-service의 [프로그램ID] 등록을 구현해줘.
serviceId는 [C0], 필수값은 [필드], 중복 기준은 [기준], 중복 오류는 [코드]다.
쓰기 Facade는 rdwTransactionManager와 rollbackFor=Exception.class를 사용한다.
후속 SQL 실패 시 전체 rollback되도록 정상/중복/rollback 테스트를 포함한다.
```

### 8.4 수정

```text
pdmg-service의 [프로그램ID] 수정을 구현해줘.
serviceId는 [U0], PK는 [필드], 수정 가능 필드는 [필드], 동시성 기준은 [기준]이다.
PK 없는 UPDATE를 금지하고 영향 행 수 0이면 [오류 코드]를 발생시킨다.
정상/PK 누락/0건/충돌/rollback 테스트를 포함한다.
```

### 8.5 삭제

```text
pdmg-service의 [프로그램ID] 삭제를 구현해줘.
serviceId는 [D0], 삭제 방식은 [물리/논리], PK는 [필드], 삭제 조건은 [조건]이다.
대상 0건은 [정책], 삭제 불가는 [오류 코드]로 처리한다.
PK 없는 DELETE/UPDATE를 금지하고 정상/0건/삭제불가/rollback 테스트를 포함한다.
```

### 8.6 기존 CRUD 진단

```text
pdmg-service의 기존 [프로그램ID] CRUD 문제를 진단해줘.
증상은 [요청/기대 결과/실제 결과/로그/재현 절차]다.
아직 수정하지 말고 serviceId 라우팅, Facade TX, Service 검증, DAO/Mapper 바인딩, SQL, Oracle/H2 차이와 오류 변환을 추적해 근본 원인을 증거와 함께 제시해줘.
원인 확인 후 최소 수정안, 영향 파일과 회귀 테스트를 보여주고 승인받은 뒤 수정해줘.
```

---

## 9. AI 결과 검수 체크리스트

### 범위와 구조

- [ ] 기존 사용자 변경을 보존했는가?
- [ ] 승인한 모듈과 파일만 변경했는가?
- [ ] Handler → Facade → Service → DAO/MyBatis 방향을 지켰는가?
- [ ] Handler에 SQL·상세 업무 로직·트랜잭션이 없는가?
- [ ] Facade에 DTO 변환과 TX 경계가 있는가?

### API와 DTO

- [ ] serviceId, URL, Handler 분기와 Facade 메서드가 일치하는가?
- [ ] 요청/응답 DTO가 거래별로 분리되었는가?
- [ ] 성공은 `{hdr_nhnis,dto}`, 예외는 `{hdr_nhnis,result}`인가?
- [ ] camelCase와 레거시 Alias 정책이 명확한가?
- [ ] 실제 업무 `*List/*Array` 필드가 응답 정리에서 사라지지 않는가?

### 트랜잭션과 타임아웃

- [ ] 조회 Facade가 `readOnly=true`인가?
- [ ] 쓰기 Facade가 `rollbackFor=Exception.class`인가?
- [ ] `rdwTransactionManager`를 사용했는가?
- [ ] 여러 쓰기 SQL이 하나의 rollback 범위에 있는가?
- [ ] 공통 timeout과 특정 거래 TX timeout을 혼동하지 않았는가?
- [ ] timeout 후 실제 rollback을 테스트했는가?

### DAO와 SQL

- [ ] DAO FQCN과 Mapper namespace가 일치하는가?
- [ ] DAO 메서드와 statement ID가 일치하는가?
- [ ] `#{...}` 바인딩을 사용하는가?
- [ ] UPDATE/DELETE에 PK 또는 승인된 범위 조건이 있는가?
- [ ] 목록과 Count SQL의 조건이 같은가?
- [ ] 정렬이 안정적인가?
- [ ] Oracle과 H2에서 검증했는가?

### 업무·오류·보안

- [ ] 필수값, 길이, 형식, 중복, 0건과 충돌 정책이 구현되었는가?
- [ ] 오류 코드가 `exceptionCode.yml`에 존재하는가?
- [ ] BizException과 결과 DTO 방식이 요구사항과 일치하는가?
- [ ] 인증과 권한 요구가 적용되었는가?
- [ ] 개인정보, Token, Secret과 Stack Trace가 응답·로그에 노출되지 않는가?

### 테스트와 완료 증거

- [ ] 정상 CRUD 외에 실패·경계 테스트가 있는가?
- [ ] rollback 테스트가 실제 DB 상태를 확인하는가?
- [ ] 실행한 명령과 실제 결과를 보고했는가?
- [ ] `NO-SOURCE`를 테스트 성공으로 표현하지 않았는가?
- [ ] 미실행·미검증 범위와 이유를 명시했는가?

---

## 10. 핵심 원칙

좋은 CRUD 프롬프트의 목적은 기술 이름을 많이 나열하는 것이 아니다. AI가 임의로 결정하면 안 되는 업무 계약을 명확히 하고, 구현 전에 불확실성을 질문하게 하며, 완료 후 검증 가능한 증거를 요구하는 것이다.

```text
확정된 요구사항
  + 현행 아키텍처 준수
  + 추정 금지
  + 테스트 우선
  + 실제 검증 결과
= 안전한 CRUD 구현
```
