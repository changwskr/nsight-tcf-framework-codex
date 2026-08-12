# PDMG Service CRUD 프롬프트 가이드

이 문서는 AI에게 `pdmg-service`의 CRUD 기능을 정확하고 안전하게 요청하기 위한 실전 가이드다. 단순히 “CRUD를 만들어줘”라고 요청하지 않고, 업무 규칙·DB 계약·TCF 전문·트랜잭션·오류·테스트까지 명확하게 전달하는 것을 목표로 한다.

## 1. 이 가이드의 사용법

가장 안전한 사용 순서는 다음과 같다.

1. 2장의 입력 양식을 채운다.
2. 모르는 항목은 추측하지 말고 `[미정—질문 필요]`라고 적는다.
3. 처음 개발하는 기능이면 4장의 단계별 프롬프트를 사용한다.
4. 요구사항이 이미 확정됐다면 5장의 완성형 프롬프트를 사용한다.
5. 일부 기능만 필요하면 6장의 짧은 프롬프트를 사용한다.
6. 구현 후 9장의 체크리스트로 결과를 검수한다.

프롬프트의 `[대괄호]` 부분을 실제 값으로 바꾸고 그대로 복사하면 된다.

### 반드시 지킬 범위

```text
작업 대상: pdmg-service만
작업 제외: pdmg-ui와 상위 저장소의 다른 프로젝트 (UI 포함 시 명시)
기준 문서: docs/네이밍원칙.md, docs/MG-NAMING_CONVENTION.md
참조 구현: mgcoa8888 Handler/Facade/Service/DAO/DTO/MyBatis
패키지: nhnis.mg.co.a.entry.* / application.* / dto / persistence.dao
```

AI가 업무 규칙, 스키마, 외부 계약을 확실히 알 수 없다면 구현을 추측하게 하지 말고 질문하게 해야 한다.

## 2. 프롬프트 전에 준비할 정보

아래 양식을 먼저 채운다.

### 2.1 기본 정보

```text
프로그램 ID: [예: mgcoa8888]
업무명: [예: 영업 팁 관리]
업무 설명: [누가 무엇을 왜 관리하는지]
Java 기본 패키지: [예: nhnis.mg.co.a]
serviceId: [예: mgcoa8888S0, mgcoa8888D0]
Mapper: [예: rdw.mg.co.a/]
작업 범위: [전체 CRUD / 목록 / 상세 / 등록 / 수정 / 삭제]
참조할 기존 프로그램: [예: mgcoa8888]
```

### 2.2 DB 정보

```text
DB 종류: [Oracle / H2 로컬 병행]
테이블명: [예: TB_CR_AH_SALES_TIP_RACT]
PK: [컬럼 목록]
UK 또는 중복 판단 기준: [컬럼 목록 또는 없음]
삭제 방식: [물리 삭제 / 논리 삭제]
논리 삭제 컬럼과 값: [예: DEL_YN, Y/N]
낙관적 잠금 컬럼: [버전 또는 수정시각 / 없음]
감사 컬럼: [등록자, 등록시각, 수정자, 수정시각]
```

컬럼은 표로 제공하는 것이 가장 정확하다.

| DB 컬럼 | Java 필드 | DB 타입 | Java 타입 | PK | NULL 허용 | 입력 검증 | 설명 |
|---|---|---|---|---|---|---|---|
| `[TRT_BRC]` | `[trtBrc]` | `[VARCHAR2(5)]` | `[String]` | `[Y]` | `[N]` | `[필수, 길이 5]` | `[취급점 코드]` |

### 2.3 조회 정보

```text
목록 검색조건: [필드, 정확 일치/부분 일치/범위]
기본 정렬: [예: BAS_DT DESC, TRT_BRC ASC]
페이지 번호 시작: [1]
기본 pageSize: [20]
최대 pageSize: [100]
상세 조회키: [PK 전체]
조회 결과 없음 처리: [빈 목록 / FW 코드 오류]
```

### 2.4 쓰기 업무 규칙

```text
등록 필수값: [필드 목록]
등록 중복 처리: [오류 코드와 메시지]
수정 가능 필드: [필드 목록]
수정 불가 필드: [PK, 생성자 등]
수정 대상 없음 처리: [오류 코드]
동시 수정 충돌 처리: [오류 코드 / 미사용]
삭제 전 확인조건: [연관 데이터, 상태값 등]
삭제 대상 없음 처리: [멱등 성공 / 오류 코드]
한 요청에서 여러 SQL이 실패할 때: [전체 rollback]
```

### 2.5 serviceId·트랜잭션·보안 정보

CRUD 거래별로 작성한다. (`mgcoa` + 식별4 + 구분자S/C/U/D + 순번)

| 동작 | serviceId | Facade TX | timeout |
|---|---|---|---|
| 목록 | `[mgcoaXXXXS0]` | `readOnly = true` | `[초 또는 미지정]` |
| 상세 | `[mgcoaXXXXS1]` | `readOnly = true` | `[초 또는 미지정]` |
| 등록 | `[mgcoaXXXXC0]` | `rollbackFor = Exception.class` | `[초 또는 미지정]` |
| 수정 | `[mgcoaXXXXU0]` | `rollbackFor = Exception.class` | `[초 또는 미지정]` |
| 삭제 | `[mgcoaXXXXD0]` | `rollbackFor = Exception.class` | `[초 또는 미지정]` |

```text
인증 필요 여부: [Y/N]
필요 권한: [권한명 또는 미정]
사용자 ID 출처: [SecurityContext / Header 계약]
민감정보 마스킹: [필드 목록]
감사 로그 요구: [등록/수정/삭제 전후 값 여부]
```

`@TcfTransaction` / `transactionCode` / `processingType` 은 PDMG 현행에서 쓰지 않는다.

### 2.6 오류 코드와 완료 조건

```text
필수값 누락: [예: FW0001]
조회 결과 없음: [예: FW0003 또는 신규 코드]
중복 데이터: [신규 오류 코드]
동시 수정 충돌: [신규 오류 코드]
삭제 불가: [신규 오류 코드]
권한 없음: [예: FW0403]
시스템 오류: FW9999
```

완료 조건을 예시 데이터와 함께 적는다.

```text
Given: [초기 DB 상태]
When:  [요청 JSON { hdr_nhnis, dto }]
Then:  [응답 dto 또는 NH_NIS_ERR_DTO, DB 상태]
```

## 3. 권장 작업 순서

한 번에 구현을 시키는 것보다 다음 순서가 안전하다.

```text
1. 저장소·규칙 조사
2. 빠진 요구사항 질문
3. 설계와 영향 파일 승인
4. 실패하는 테스트 작성
5. DTO와 검증 구현
6. DAO와 MyBatis 구현
7. Service 업무 구현 (TX는 Facade)
8. Handler + Facade 연결 (TCF OFF 시 Controller)
9. 전체 테스트·참조·SQL 검증
10. 변경 파일과 검증 결과 보고
```

각 단계에서 AI가 사용자 변경을 보존하고 관련 없는 리팩터링을 하지 않도록 명시한다.

## 4. 단계별 프롬프트

### 4.1 저장소와 규칙 조사

```text
pdmg-service에 CRUD 기능을 추가하려고 한다. 아직 코드를 수정하지 마라.

다음을 먼저 조사해 보고해라.
- docs/네이밍원칙.md, docs/MG-NAMING_CONVENTION.md
- mgcoa8888의 Handler, Facade, Service, DAO, DTO, MyBatis XML
- pdmg-fw OnlineTransactionController, GlobalExceptionHandler, NH_NIS_ERR_DTO
- BizException, exceptionCode.yml
- RdwDataSourceConfig와 Facade @Transactional 구성
- H2 schema/data 구조

작업 범위는 pdmg-service뿐이다. 현재 사용자 변경을 보존하고 관련 없는 파일은 건드리지 마라.
조사 결과로 따라야 할 패턴, 주의할 호환 계약, 필요한 입력정보를 목록으로 제시해라.
```

### 4.2 빠진 요구사항 질문

```text
다음 CRUD 요구사항을 검토하고 구현에 필요한데 빠진 정보를 질문해라.
[2장의 작성된 요구사항 붙여넣기]

DB 컬럼, PK, 중복 기준, 삭제 방식, serviceId, timeout, 오류 코드,
권한, 동시성 정책을 추측하지 마라. 질문은 구현 결과를 바꾸는 항목부터 한 번에 하나씩 해라.
```

### 4.3 설계와 영향 파일 제안

```text
확정된 요구사항은 다음과 같다.
[확정 요구사항 붙여넣기]

아직 구현하지 말고 다음을 설계해라.
- 요청·응답 DTO와 필드
- serviceId(URL)와 Handler/Facade 매핑
- Handler → Facade(@Transactional) → Service → DAO/MyBatis 흐름
- 읽기/쓰기 트랜잭션(Facade)과 rollback 범위
- 목록 페이징과 정렬
- 필수값, 중복, 미존재, 동시 수정 오류
- Oracle/H2 SQL 차이
- 단위·통합 테스트 시나리오
- 생성·수정할 정확한 파일 목록

docs/네이밍원칙.md를 따르고 설계 승인 전 코드를 수정하지 마라.
```

### 4.4 테스트 우선 구현

```text
승인된 설계를 테스트 우선으로 구현해라.

1. Service 업무 규칙과 트랜잭션 실패 시나리오의 실패하는 테스트를 먼저 작성한다.
2. 실행해서 기대한 이유로 실패함을 확인한다.
3. 최소 구현을 작성한다.
4. 관련 테스트와 전체 gradlew.bat test를 실행한다.

테스트가 없는 상태를 성공으로 보고하지 말고, NO-SOURCE이면 테스트가 없다고 명시해라.
```

### 4.5 DTO와 검증

```text
승인된 CRUD 설계 중 DTO와 입력 검증만 구현해라.
- Java/JSON 필드는 camelCase로 맞춘다.
- 요청, 출력, 목록 응답 DTO 역할을 분리한다.
- 필수값, 길이, 형식, 허용값 규칙을 명시한다.
- 업무 오류는 BizException과 exceptionCode.yml을 사용한다.
- DTO에 DB 접근이나 업무 서비스를 넣지 않는다.

구현 후 DTO 매핑과 검증 테스트를 실행하고 결과를 보고해라.
```

### 4.6 DAO와 MyBatis

```text
승인된 CRUD 설계 중 DAO와 MyBatis XML을 구현해라.
- mapper namespace는 DAO FQCN과 일치시킨다.
- DAO 메서드명과 statement id를 일치시킨다.
- parameterType/resultType을 실제 DTO와 맞춘다.
- DB 컬럼 alias를 Java 필드 camelCase와 맞춘다.
- 바인딩 변수 #{...}를 사용하고 문자열 SQL 조합을 피한다.
- PK 조건 없는 UPDATE/DELETE를 금지한다.
- Oracle 운영 SQL과 H2 로컬 실행 가능성을 함께 검증한다.

SQL별 기대 row count와 0건 처리 정책도 보고해라.
```

### 4.7 Service 트랜잭션과 오류

```text
Service 업무 로직을 구현해라. (@Transactional 은 Facade에 둔다)
- 조회 Facade: @Transactional(rdwTransactionManager, readOnly = true)
- 쓰기 Facade: @Transactional(rdwTransactionManager, rollbackFor = Exception.class)
- 공통 온라인 SLA는 OnlineTimeoutExecutor(nhnis.fw.timeout)가 담당한다. Facade timeout은 보조이며, [확정값/미지정]일 때만 넣고 임의 값을 만들지 않는다.
- 여러 쓰기 SQL은 하나의 Facade(및 Executor) 트랜잭션으로 묶는다.
- 필수값, 중복, 미존재, 동시 수정 충돌은 BizException으로 구분한다.
- 예외를 삼키거나 실패를 정상 응답으로 바꾸지 않는다.

RuntimeException과 BizException 발생 시 rollback되는 테스트를 포함해라.
```

### 4.8 Handler·Facade (TCF ON)

```text
TCF ON 진입을 현재 PDMG 계약에 맞게 구현해라.
- Handler(entry.handler)는 serviceId 라우팅만 하고 Facade를 호출한다.
- Facade(application.facade)에 @Transactional(rdwTransactionManager)를 둔다.
- 요청/응답은 { hdr_nhnis, dto }. DTO는 nhnis.mg.co.a.dto.
- 실패는 BizException → GlobalExceptionHandler → NH_NIS_ERR_DTO.
- TCF OFF 호환이 필요하면 application.controller 를 추가하되 Service/Facade만 호출한다.
- 인증·권한 요구를 기존 Security 구조와 맞춘다.

serviceId, 요청 JSON, 성공/실패 응답 예시를 함께 제시해라.
```

### 4.9 검증과 인계

```text
구현을 완료하기 전에 다음을 새로 검증해라.
- DAO 메서드와 MyBatis statement id 일치
- DTO 필드와 SQL alias 일치
- 읽기/쓰기 @Transactional 설정
- PK 없는 UPDATE/DELETE 없음
- 오류 코드가 exceptionCode.yml에 존재
- serviceId·Handler·Facade 메서드 매핑 누락 없음
- Oracle/H2 호환
- 관련 테스트와 gradlew.bat test
- 비밀값·개인정보·관련 없는 변경 없음

최종 보고에는 변경 파일, API, 트랜잭션, 오류 코드, 테스트 명령과 실제 결과를 포함해라.
```

## 5. 전체 CRUD 완성형 프롬프트

아래 블록을 복사하고 대괄호를 채운다.

```text
pdmg-service에 다음 CRUD 서비스를 설계하고 구현해라.

[범위]
- 작업 프로젝트: pdmg-service만
- pdmg-ui와 다른 프로젝트는 수정 금지
- 사용자 기존 변경 보존
- 관련 없는 리팩터링 금지
- 기준: docs/네이밍원칙.md와 현재 mgcoa8888/TCF 구조

[업무]
- 프로그램 ID: [프로그램ID]
- 업무명/목적: [업무명과 설명]
- 기본 패키지: nhnis.mg.co.a (handler/facade/service/dto/dao)
- serviceId: [/mgcoaXXXXS0 등]
- Mapper: rdw.mg.co.a/
- 구현 범위: 목록, 상세, 등록, 수정, 삭제

[DB]
- 테이블: [테이블명]
- PK: [PK 컬럼]
- 중복 기준: [UK 또는 업무키]
- 삭제 방식: [물리/논리]
- 동시성: [버전/수정시각/미사용]
- 컬럼 정의:
  [DB 컬럼 | Java 필드 | DB 타입 | Java 타입 | NULL | 검증 | 설명]

[조회]
- 검색조건: [조건과 비교 방식]
- 정렬: [정렬 순서]
- 페이징: pageNo 기본 [1], pageSize 기본 [20], 최대 [100]
- 목록 0건: [빈 목록]
- 상세 0건: [오류 코드]

[쓰기]
- 등록 필수값과 기본값: [내용]
- 중복 등록: [오류 코드]
- 수정 가능/불가 필드: [내용]
- 수정 대상 없음: [오류 코드]
- 동시 수정 충돌: [정책과 오류 코드]
- 삭제 조건과 대상 없음: [정책]
- 여러 SQL 실패 시 전체 rollback

[serviceId]
- 목록: [mgcoaXXXXS0]
- 상세: [mgcoaXXXXS1]
- 등록: [mgcoaXXXXC0]
- 수정: [mgcoaXXXXU0]
- 삭제: [mgcoaXXXXD0]

[트랜잭션]
- 선언 위치: application.facade
- 조회: @Transactional(rdwTransactionManager, readOnly = true)
- 쓰기: @Transactional(rdwTransactionManager, rollbackFor = Exception.class)
- timeout: [초 또는 미지정]
- timeout 값을 추측하지 말 것

[오류·보안]
- 필수값/중복/미존재/충돌/삭제불가 오류 코드: [코드 목록]
- 메시지는 exceptionCode.yml에서 관리
- 업무 오류는 BizException 사용
- 실패 응답은 NH_NIS_ERR_DTO / GlobalExceptionHandler
- 인증과 권한: [요구사항]
- 민감정보 마스킹: [필드]

[구현 계약]
- 요청/응답은 { hdr_nhnis, dto }
- Handler → Facade(@Transactional) → Service → DAO
- Service는 업무 규칙만 (TX 미보유가 기본)
- DAO/MyBatis는 영속성 담당
- DAO 메서드명과 MyBatis statement id 일치
- mapper namespace는 DAO FQCN과 일치
- SQL alias는 DTO 필드와 일치
- 바인딩 변수 사용, PK 없는 UPDATE/DELETE 금지
- Oracle 운영과 H2 로컬 호환 고려

[테스트·완료 조건]
- 테스트를 먼저 작성하고 실패 이유를 확인한 뒤 구현
- 목록 페이징, 상세 0건, 중복, 등록, 수정 0건, 동시성, 삭제, rollback 테스트
- gradlew.bat test 실행
- NO-SOURCE이면 테스트 없음으로 명시
- 변경 파일, API 예시, 트랜잭션, 오류 코드와 실제 검증 결과 보고

먼저 현재 저장소와 요구사항을 조사해라. 구현 결과를 바꾸는 정보가 빠졌다면 추측하지 말고 질문해라.
그다음 설계, 테스트 시나리오, 영향 파일을 제시하고 내 승인을 받은 후 구현해라.
```

## 6. 작업별 짧은 프롬프트

### 6.1 목록 조회

```text
pdmg-service의 [프로그램ID] 목록 조회를 구현해라.
검색조건은 [조건], 정렬은 [순서], 페이징은 pageNo [1], pageSize [20], 최대 [100]이다.
조회는 Facade @Transactional(rdwTransactionManager, readOnly = true)를 사용하고 0건은 빈 records와 totalCount=0으로 반환한다.
count SQL과 목록 SQL의 조건을 일치시키고 Oracle/H2에서 검증해라.
{ hdr_nhnis, dto } 전문과 serviceId [예: mgcoaXXXXS0]를 적용하고 테스트 결과를 보고해라.
```

### 6.2 상세 조회

```text
pdmg-service의 [프로그램ID] 상세 조회를 구현해라.
조회키는 [PK 전체]이고 누락은 [오류 코드], 결과 없음은 [오류 코드] BizException으로 처리한다.
Facade @Transactional(rdwTransactionManager, readOnly = true), { hdr_nhnis, dto }, serviceId [예: mgcoaXXXXS1]를 적용한다.
PK 모든 조건이 SQL에 포함되는지와 정상/누락/0건 테스트를 검증해라.
```

### 6.3 등록

```text
pdmg-service의 [프로그램ID] 등록을 구현해라.
필수값은 [필드], 중복 기준은 [업무키], 중복 오류는 [코드]다.
쓰기 Facade @Transactional(rollbackFor=Exception.class)을 적용하고 후속 SQL 실패 시 전체 rollback되게 한다.
(공통 온라인 timeout은 OnlineTimeoutExecutor. Facade에 timeout=[확정값]을 넣는 것은 보조·명시 요청 시에만.)
DAO/MyBatis insert, exceptionCode.yml, serviceId와 중복/rollback 테스트를 포함해라.
```

timeout을 정하지 않았다면 Facade 애노테이션에 임의 `timeout=` 를 넣지 않는다. 공통 SLA는 [20.타임아웃.md](./20.타임아웃.md).

### 6.4 수정

```text
pdmg-service의 [프로그램ID] 수정을 구현해라.
PK는 [필드], 수정 가능 필드는 [필드], 동시성 기준은 [버전/수정시각/미사용]이다.
update row count가 0이면 [미존재/충돌 오류 코드]를 발생시킨다.
PK 없는 UPDATE를 금지하고 쓰기 Facade 트랜잭션, serviceId, 정상/0건/rollback 테스트를 검증해라.
```

### 6.5 삭제

```text
pdmg-service의 [프로그램ID] 삭제를 구현해라.
삭제 방식은 [물리/논리], PK는 [필드], 사전 삭제 조건은 [내용]이다.
대상 없음은 [멱등 성공/오류 코드] 정책을 따른다.
PK 없는 DELETE/UPDATE를 금지하고 쓰기 Facade 트랜잭션, serviceId, 삭제불가/0건/rollback 테스트를 포함해라.
```

### 6.6 기존 CRUD 디버깅

```text
pdmg-service의 기존 [프로그램ID] CRUD 문제를 진단해라.
증상: [요청, 기대결과, 실제결과, 로그, 재현절차]

아직 수정하지 말고 다음 순서로 조사해라.
1. Handler/OnlineController serviceId 매핑
2. Facade 트랜잭션과 Service 검증
3. DAO 메서드와 MyBatis statement/바인딩
4. Oracle/H2 차이
5. GlobalExceptionHandler / NH_NIS_ERR_DTO 오류 변환

근본 원인과 증거, 재현 테스트, 최소 수정안을 제시하고 승인 후 수정해라.
```

## 7. mgcoa8888 형식 작성 예시

아래는 형식을 보여주는 예시이며, 실제 코드를 변경하라는 요청이 아니다.

```text
프로그램 ID: mgcoa8888
업무명: 영업 팁 관리
패키지: nhnis.mg.co.a
API: /mgcoa8888S0
테이블: TB_CR_AH_SALES_TIP_RACT
PK: TRT_BRC, TRTMN_ENO, SALZ_TIP_KDC, BAS_DT

목록:
- salzTipKdc 선택 검색
- BAS_DT DESC, TRT_BRC, TRTMN_ENO 정렬
- pageNo 기본 1, pageSize 기본 20, 최대 100
- @Transactional(rdwTransactionManager, readOnly = true) on Facade
- serviceId: mgcoa8888S0

상세:
- PK 네 필드 모두 필수
- 결과 없음은 FW0003
- serviceId: mgcoa8888S1 (예시; 미정이면 질문)

등록·수정·삭제:
- serviceId(C0/U0/D0), 중복/충돌/삭제 오류 코드는 아직 미정
- 이 정보는 AI가 추측하지 말고 구현 전에 질문해야 함
```

이 예시처럼 “확정된 것”과 “미정인 것”을 분리하면 잘못된 업무 코드를 만드는 것을 막을 수 있다.

## 8. 나쁜 프롬프트와 개선 예시

### 나쁜 예 1

```text
CRUD 만들어줘.
```

문제점: 테이블, PK, 오류, 트랜잭션, API와 완료 조건을 알 수 없다.

### 개선 예 1

```text
pdmg-service의 [프로그램ID] CRUD를 만들어라.
테이블과 PK는 [정의], 목록 조건과 정렬은 [정의], 삭제는 [논리/물리]다.
읽기는 Facade readOnly, 쓰기는 Facade rollback 트랜잭션을 사용하고 timeout은 [값/미지정]이다.
serviceId와 오류 코드는 [정의]를 사용한다.
먼저 누락 요구사항을 질문하고 설계와 테스트를 승인받은 뒤 구현해라.
```

### 나쁜 예 2

```text
mgcoa8888 참고해서 알아서 만들어줘.
```

문제점: 참조할 구조와 복사하면 안 되는 업무 규칙을 구분하지 못한다.

### 개선 예 2

```text
mgcoa8888에서는 패키지 구조, { hdr_nhnis, dto }, Handler/Facade TX, 페이징, MyBatis 연결 방식만 참고해라.
테이블·컬럼·PK·serviceId·오류코드는 아래 신규 요구사항을 사용하고 기존 값을 복사하지 마라.
[신규 요구사항]
```

### 나쁜 예 3

```text
4초 지나면 롤백되게 해줘.
```

문제점: 어떤 Service와 동작에 적용할지, **공통 OnlineTimeoutExecutor인지 Facade `@Transactional(timeout)`인지** 알 수 없다.

### 개선 예 3

```text
공통 온라인 SLA는 OnlineTimeoutExecutor(nhnis.fw.timeout)를 유지한다.
[등록/수정/삭제]에 Facade Spring TX timeout을 추가로 둘 때만 초를 명시하고,
발생 시 쓰기 전체 rollback 테스트를 추가해라.
성능 경고 로그만으로 롤백시키지 마라.
```

## 9. AI 결과 검수 체크리스트

### 범위와 구조

- [ ] `pdmg-service`만 변경했는가?
- [ ] 사용자 기존 변경과 관련 없는 파일을 보존했는가?
- [ ] Handler → Facade → Service → DAO/MyBatis 의존 방향을 지켰는가?
- [ ] `docs/네이밍원칙.md` / `MG-NAMING_CONVENTION.md`를 따르는가?

### API·DTO·TCF

- [ ] 요청/응답이 `{ hdr_nhnis, dto }` 인가?
- [ ] DTO 필드와 JSON 필드가 camelCase로 맞는가?
- [ ] 각 API의 serviceId·Handler·Facade 매핑이 확정 요구사항과 맞는가?
- [ ] Handler/Controller에 업무 규칙이나 SQL이 들어가지 않았는가?

### 트랜잭션

- [ ] 조회 Facade가 `@Transactional(..., readOnly = true)`인가?
- [ ] 등록·수정·삭제 Facade가 쓰기 트랜잭션인가?
- [ ] 여러 쓰기 SQL이 하나의 rollback 범위에 있는가?
- [ ] timeout을 임의로 정하지 않았는가?
- [ ] `BizException`과 런타임 예외 rollback을 검증했는가?

### DB·MyBatis

- [ ] DAO 메서드와 statement ID가 일치하는가?
- [ ] namespace가 DAO FQCN과 일치하는가?
- [ ] parameterType/resultType과 DTO가 일치하는가?
- [ ] SQL alias와 DTO 필드가 일치하는가?
- [ ] `#{...}` 바인딩을 사용했는가?
- [ ] UPDATE/DELETE에 PK 또는 안전한 업무키 조건이 있는가?
- [ ] 목록과 count SQL 조건이 일치하는가?
- [ ] Oracle과 H2 차이를 검증했는가?

### 업무·오류·보안

- [ ] 필수값, 중복, 미존재, 충돌, 삭제불가 정책이 구현됐는가?
- [ ] 업무 오류가 `BizException`을 사용하는가?
- [ ] 오류 코드가 `exceptionCode.yml`에 존재하는가?
- [ ] 실패 응답이 `NH_NIS_ERR_DTO` / GlobalExceptionHandler 흐름을 유지하는가?
- [ ] 인증·권한 요구가 적용됐는가?
- [ ] 비밀번호, 토큰, 개인정보가 로그나 문서에 노출되지 않는가?

### 테스트와 증거

- [ ] 정상 CRUD뿐 아니라 실패·경계값 테스트가 있는가?
- [ ] 페이징 기본값과 최대값을 테스트했는가?
- [ ] 0건, 중복, rollback, 동시성 정책을 테스트했는가?
- [ ] `gradlew.bat test`가 실제로 실행됐는가?
- [ ] `NO-SOURCE`를 테스트 통과로 과장하지 않았는가?
- [ ] 최종 보고에 변경 파일, 명령, 실제 결과가 있는가?

## 10. 최종 요청 예시

요구사항을 이미 별도 문서나 표로 작성했다면 다음처럼 짧게 요청할 수 있다.

```text
첨부한 [CRUD 요구사항 문서/테이블 정의서]를 기준으로 pdmg-service CRUD를 구현해라.

반드시 다음을 지켜라.
- 작업 범위는 pdmg-service만
- docs/네이밍원칙.md·MG-NAMING_CONVENTION.md와 현재 mgcoa8888 구조 준수
- 모르는 스키마·업무·serviceId·timeout은 추측하지 말고 질문
- { hdr_nhnis, dto } 전문과 Handler/Facade 적용
- 조회 Facade readOnly, 쓰기 Facade rollback 트랜잭션 적용
- BizException, exceptionCode.yml, NH_NIS_ERR_DTO 오류 흐름 유지
- DAO/MyBatis 계약과 Oracle/H2 호환 검증
- 테스트 우선 구현과 gradlew.bat test 실행
- 기존 사용자 변경 보존과 관련 없는 리팩터링 금지

먼저 저장소 조사 결과, 누락 질문, 설계, 테스트 시나리오, 영향 파일을 제시해라.
내가 설계를 승인한 뒤 구현하고, 마지막에는 변경 파일과 실제 검증 결과를 보고해라.
```

좋은 CRUD 프롬프트의 핵심은 기술 이름을 많이 쓰는 것이 아니라, AI가 추측해서는 안 되는 업무 계약과 완료 증거를 명확히 주는 것이다.
