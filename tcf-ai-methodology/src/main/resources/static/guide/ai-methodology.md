# NSIGHT TCF 업무모델 자동화 개발 절차서

## 화면·ServiceId·DTO·Rule·DAO·Mapper·SQL 기반 개발 표준

# 1. 도입 전 안내말

NSIGHT TCF 기반 업무 개발은 Java 클래스를 먼저 만드는 방식으로 시작해서는 안 된다.

개발자는 먼저 업무 요구사항을 화면, 거래, 데이터와 프로그램 구조로 모델링하고, 검증된 모델을 기준으로 반복적인 코드와 설계 산출물을 자동 생성해야 한다.

```text
업무 요구사항 확인
  ↓
업무코드·도메인 확인
  ↓
테이블·컬럼 모델링
  ↓
화면·이벤트 모델링
  ↓
ServiceId·거래코드 모델링
  ↓
DTO·Rule·DAO·Mapper·SQL 모델링
  ↓
모델 자동검증
  ↓
코드·SQL·설계서 자동생성
  ↓
업무 로직 보완
  ↓
Compile·단위테스트
  ↓
TCF 통합거래 테스트
  ↓
코드리뷰·OM 등록
  ↓
배포·운영 확인
```

자동화 도구가 생성하는 것은 완성된 업무 프로그램이 아니라 다음 항목의 **표준 골격과 초안**이다.

| 자동 생성 대상       | 개발자가 직접 완성해야 하는 대상      |
| -------------------- | ------------------------------------- |
| Handler 분기 구조    | 업무별 세부 처리 판단                 |
| Facade 트랜잭션 골격 | 트랜잭션 범위와 예외 시 Rollback 판단 |
| Service 호출 구조    | 업무 처리 순서와 도메인 조합          |
| Rule 검증 골격       | 실제 업무 규칙과 오류코드             |
| DAO·Mapper Interface | 데이터 접근 전략                      |
| Mapper XML·SQL 초안  | Join, 조건, 실행계획, 성능            |
| Request·Response DTO | 업무 의미, 개인정보, 변환 규칙        |
| OM 등록 SQL 초안     | 운영 Timeout·통제·감사 정책           |
| 테스트 클래스 골격   | 정상·오류·경계·장애 테스트            |
| 설계서·추적성 초안   | 최종 업무 설명과 승인정보             |

핵심 원칙은 다음과 같다.

```text
자동화 도구는 반복작업을 수행하고,
개발자는 업무 판단을 구현한다.

생성 성공은 개발 완료가 아니며,
Compile·Test·Review·운영등록까지 완료되어야
하나의 업무 거래가 완성된다.
```

---

# 2. 문서 개요

## 2.1 목적

본 절차서의 목적은 업무 개발자가 NSIGHT Model Studio를 이용하여 업무모델을 정의하고, 생성된 코드를 NSIGHT TCF Framework에 적용하여 개발·검증·배포하는 표준 절차를 정의하는 것이다.

| 목적             | 설명                                       |
| ---------------- | ------------------------------------------ |
| 개발 순서 표준화 | 요구사항부터 배포까지 동일한 순서 적용     |
| 모델 우선 개발   | 코드 작성 전에 화면·거래·데이터 관계 확정  |
| 반복작업 자동화  | DTO, 계층 클래스, Mapper, 문서 자동 생성   |
| 책임 분리        | 자동 생성 영역과 개발자 구현 영역 구분     |
| 추적성 확보      | 화면 이벤트에서 SQL·테이블까지 연결        |
| 누락 방지        | ServiceId, Timeout, 감사, 테스트 누락 차단 |
| 품질 확보        | 자동검증·Compile·Test·Review를 Gate로 운영 |
| 변경 통제        | 모델 변경과 생성 코드 변경을 함께 관리     |

## 2.2 적용범위

본 절차는 다음 개발에 적용한다.

- WEBTOPSUITE 화면 기반 업무
- React·BI포털 화면 기반 업무
- 조회·등록·수정·삭제 업무
- 엑셀·파일 다운로드 업무
- 업무 WAR 내부 거래
- 업무 WAR 간 연계 거래
- `Handler → Facade → Service → Rule → DAO → Mapper` 구조
- Request·Criteria·Response·Row DTO
- MyBatis Mapper Interface와 XML
- OM Service Catalog와 Timeout 등록
- 거래로그·감사로그가 필요한 업무
- 신규 개발 및 기존 거래 변경

다음 항목은 별도 설계가 필요하다.

- 대용량 파일·300GB 이상 파일 처리
- 복잡한 비동기 메시징
- 장시간 Batch
- 분산 트랜잭션
- 복잡한 Cache 정합성
- 외부기관 전문 변환
- 다중 DB와 JTA
- 실시간 Event Processing

## 2.3 대상 독자

| 대상                  | 활용 내용                            |
| --------------------- | ------------------------------------ |
| 업무 개발자           | 모델 작성, 코드 생성, 업무 로직 구현 |
| UI 개발자             | 화면 이벤트와 ServiceId 연결         |
| 업무 분석가           | 요구사항·업무 규칙·입출력 정의       |
| 애플리케이션 아키텍트 | 도메인·계층·트랜잭션 검토            |
| DA·DBA                | 테이블·컬럼·SQL·인덱스 검토          |
| 프레임워크팀          | 생성 템플릿과 TCF 계약 관리          |
| 테스트팀              | 모델 기반 테스트 시나리오 작성       |
| 보안 담당자           | 권한·개인정보·감사 검토              |
| DevOps팀              | Build·Quality Gate·배포              |
| 운영팀                | ServiceId·SQL·오류 추적 및 OM 등록   |

## 2.4 선행조건

개발 착수 전 다음 항목이 확정되어야 한다.

| 선행조건        | 확인 내용                                   |
| --------------- | ------------------------------------------- |
| 업무코드        | `SV`, `IC`, `PC`, `MG` 등 공식 업무코드     |
| 업무 WAR        | `sv-service`, `ic-service` 등 배포 단위     |
| Context Path    | `/sv`, `/ic`, `/mg` 등                      |
| 업무 도메인     | Customer, Product, Campaign 등              |
| 화면 ID 규칙    | `{업무코드}-{업무세구분}-{4자리}`           |
| ServiceId 규칙  | `{업무코드}.{도메인}.{행위}`                |
| 거래코드 규칙   | `{업무코드}-{유형}-{4자리}`                 |
| 패키지 프로파일 | 현재 소스 호환형 또는 도메인 우선형         |
| 표준 Header     | businessCode, serviceId, transactionCode 등 |
| 오류코드        | 업무·검증·시스템 오류코드 기준              |
| Timeout 기준    | 기본·조회·변경·외부연계 Timeout             |
| Git 기준        | 대상 Branch와 Pull Request 절차             |

## 2.5 주요 용어

| 용어           | 정의                                                    |
| -------------- | ------------------------------------------------------- |
| 업무모델       | 화면·거래·DTO·프로그램·SQL 관계를 정의한 메타데이터     |
| Workspace      | 동일 프로젝트에서 함께 검증·생성하는 업무모델 집합      |
| Model Baseline | 검토와 승인을 마친 모델 버전                            |
| 생성 코드      | Model Studio가 자동으로 만든 Java·XML·SQL               |
| 수동 코드      | 개발자가 직접 구현·유지하는 업무 로직                   |
| Quality Gate   | 다음 단계 진입 전 반드시 통과해야 하는 기준             |
| ServiceId      | 화면 이벤트와 서버 업무 프로그램을 연결하는 거래 식별자 |
| SQL ID         | MyBatis Mapper XML의 Statement 식별자                   |
| Traceability   | 화면부터 DB까지 정방향·역방향으로 추적할 수 있는 관계   |

---

# 3. 전체 개발 단계

## 3.1 단계 요약

| 단계 | 단계명                | 주 작업                  | 핵심 산출물          | 완료조건          |
| ---: | --------------------- | ------------------------ | -------------------- | ----------------- |
|    0 | 개발 착수 준비        | 요구사항·환경·기준 확인  | 개발 착수 체크리스트 | 선행조건 충족     |
|    1 | 업무 요구사항 분석    | 기능·규칙·입출력 분석    | 요구사항 목록        | 범위 승인         |
|    2 | 프로젝트·도메인 설정  | 업무코드·WAR·패키지 정의 | 프로젝트 모델        | 구조 검증 완료    |
|    3 | 데이터 모델링         | 테이블·컬럼·PK 정의      | 데이터 모델          | DA·DBA 확인       |
|    4 | 화면·이벤트 모델링    | 화면과 사용자 행위 정의  | 화면·이벤트 모델     | 이벤트 누락 없음  |
|    5 | 거래·ServiceId 모델링 | 거래 식별자·정책 정의    | 거래 모델            | OM 정책 초안 확인 |
|    6 | DTO 모델링            | 요청·조건·응답 필드 정의 | DTO 모델             | 필드 정합성 완료  |
|    7 | Rule 모델링           | 검증·업무 규칙 정의      | Rule 목록            | 오류코드 연결     |
|    8 | DAO·Mapper·SQL 모델링 | 데이터 접근과 SQL 정의   | SQL 모델             | 안전조건 확인     |
|    9 | 모델 통합검증         | 중복·명명·추적성 검증    | 검증 결과            | 오류 0건          |
|   10 | 코드·산출물 생성      | Java·XML·SQL·문서 생성   | 생성 ZIP·Manifest    | 생성물 확인       |
|   11 | 생성 코드 적용        | Git 작업공간에 병합      | 변경 파일            | Diff 검토 완료    |
|   12 | 업무 로직 구현        | Service·Rule·SQL 보완    | 구현 코드            | 개발자 자체검토   |
|   13 | 단위·로컬 테스트      | Compile·Unit Test        | 테스트 결과          | 테스트 통과       |
|   14 | TCF 통합거래 테스트   | 전체 호출 흐름 검증      | 통합시험 결과        | 정상·오류 검증    |
|   15 | 품질·보안·성능 검토   | 구조·SQL·권한 검토       | Review 결과          | 중대결함 0건      |
|   16 | 산출물·OM 등록        | 설계서와 운영정보 등록   | OM·추적성 산출물     | 운영정보 정합     |
|   17 | Pull Request·승인     | 코드리뷰와 병합          | 승인 PR              | Merge 완료        |
|   18 | 배포·운영 확인        | 배포·Smoke Test          | 배포 결과            | 운영 거래 정상    |
|   19 | 변경·폐기 관리        | 변경영향·버전 관리       | 변경이력             | Baseline 갱신     |

---

# 4. 단계별 상세 개발 절차

# 단계 0. 개발 착수 준비

## 4.1 목적

개발자가 코드를 작성하기 전에 개발 대상과 적용할 표준을 확정한다.

## 4.2 입력자료

- 업무 요구사항
- 화면 목록
- 테이블 정의서
- 업무코드 목록
- 대상 업무 WAR
- 기존 유사 거래
- ServiceId 명명규칙
- 오류코드 표준
- Git 저장소와 대상 Branch

## 4.3 개발자 수행 절차

```text
1. 대상 Git 저장소 Clone
2. 개발 Branch 생성
3. JDK·Gradle 버전 확인
4. 대상 업무 WAR Build
5. 유사 ServiceId 실행 확인
6. 업무코드·도메인·패키지 확인
7. Model Studio 실행
8. 신규 업무모델 생성
```

## 4.4 확인 명령 예시

```bash
java -version
./gradlew --version
git status
git branch --show-current
./gradlew :sv-service:clean :sv-service:test
```

## 4.5 완료조건

- 대상 업무 WAR가 변경 전 상태에서 정상 Build된다.
- 업무코드·도메인·화면·테이블 담당자가 확인된다.
- 개발할 ServiceId의 신규·변경 여부가 구분된다.
- 기존 오류가 개발자의 변경으로 오인되지 않도록 기준 Commit이 기록된다.

## 4.6 금지사항

- Build 실패 상태에서 신규 개발을 시작하지 않는다.
- 기준 Branch를 확인하지 않고 작업하지 않는다.
- 기존 유사 거래를 무조건 복사하지 않는다.
- 운영 비밀번호·Token·Private Key를 로컬 모델에 입력하지 않는다.

---

# 단계 1. 업무 요구사항 분석

## 4.7 목적

화면에서 무엇을 수행하며 어떤 데이터를 조회·변경해야 하는지 정의한다.

## 4.8 개발자가 확인할 질문

| 구분        | 확인 질문                                       |
| ----------- | ----------------------------------------------- |
| 사용자 행위 | 사용자가 어떤 버튼·메뉴·행을 선택하는가         |
| 처리 목적   | 조회·등록·변경·삭제·실행 중 무엇인가            |
| 입력        | 화면에서 어떤 값을 전달하는가                   |
| 출력        | 화면에 어떤 값을 표시하는가                     |
| 업무 규칙   | 어떤 조건에서 허용·금지되는가                   |
| 데이터      | 어떤 테이블을 조회·변경하는가                   |
| 권한        | 어떤 사용자·지점·역할이 수행하는가              |
| 감사        | 개인정보 조회·변경 이력 대상인가                |
| Timeout     | DB·외부연계 지연 가능성이 있는가                |
| 오류        | 사용자에게 구분해서 보여줄 업무 오류는 무엇인가 |

## 4.9 요구사항 정리표

| 항목          | 예시                      |
| ------------- | ------------------------- |
| 업무명        | 고객 종합정보 조회        |
| 업무코드      | `SV`                      |
| 도메인        | `Customer`                |
| 처리유형      | 조회                      |
| 주요 입력     | 고객번호                  |
| 주요 출력     | 고객명, 고객등급, 상품 수 |
| 주요 규칙     | 고객번호 필수             |
| 권한          | 고객정보 조회 권한        |
| 개인정보      | 고객명 마스킹 대상        |
| 목표 응답시간 | 3초 이내                  |
| 감사대상      | 고객정보 상세 조회        |

## 4.10 완료조건

- 정상 흐름과 오류 흐름이 정의된다.
- 입력·출력·업무 규칙이 구분된다.
- 화면 변경과 서버 변경 범위가 식별된다.
- 신규 테이블과 기존 테이블 사용 여부가 확인된다.

---

# 단계 2. 프로젝트·도메인 모델 설정

## 4.11 Model Studio 입력항목

| 구분            | 입력 예시                 |
| --------------- | ------------------------- |
| 프로젝트명      | NSIGHT Marketing          |
| BASE 패키지     | `com.nh.nsight.marketing` |
| 업무코드        | `SV`                      |
| 업무 WAR        | `sv-service`              |
| Context Path    | `/sv`                     |
| 도메인          | `Customer`                |
| 패키지 프로파일 | 현재 소스 호환형          |
| 소유팀          | SV 고객정보 개발팀        |

## 4.12 패키지 프로파일 선택

### 현재 소스 호환형

```text
com.nh.nsight.marketing.sv.entry.handler
com.nh.nsight.marketing.sv.entry.facade
com.nh.nsight.marketing.sv.application.service
com.nh.nsight.marketing.sv.application.rule
com.nh.nsight.marketing.sv.persistence.dao
com.nh.nsight.marketing.sv.persistence.mapper
```

기존 소스에 신규 거래를 추가할 때 우선 적용한다.

### 도메인 우선형

```text
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper
```

신규 업무 또는 패키지 개편 대상에 적용한다.

## 4.13 개발자 확인사항

- 동일 업무코드가 여러 WAR에 중복되지 않는가
- 도메인이 단순 메뉴 분류가 아니라 업무 책임 단위인가
- 해당 도메인이 사용하는 테이블의 소유권이 명확한가
- 다른 업무 WAR의 Java 클래스를 직접 참조하지 않는가

## 4.14 완료조건

- 업무코드·도메인·WAR·Context Path가 일치한다.
- 기존 프로젝트의 Component Scan·Mapper Scan 범위와 일치한다.
- 패키지 프로파일이 아키텍트의 승인을 받는다.

---

# 단계 3. 테이블·컬럼 모델링

## 4.15 목적

화면과 서비스가 사용할 데이터 구조를 정의한다.

## 4.16 Model Studio 입력항목

| 항목        | 설명                           |
| ----------- | ------------------------------ |
| 테이블명    | 물리 테이블명                  |
| 테이블 설명 | 업무 의미                      |
| 컬럼명      | DB 물리 컬럼                   |
| Java 필드명 | DTO와 Row 객체 필드            |
| Java 타입   | String, Long, BigDecimal 등    |
| DB 타입     | VARCHAR2, NUMBER, TIMESTAMP 등 |
| 길이·정밀도 | 컬럼 크기                      |
| PK 여부     | 기본키                         |
| NULL 여부   | 필수 여부                      |
| 요청 사용   | Request DTO 포함               |
| 조건 사용   | Criteria·WHERE 포함            |
| 응답 사용   | Response DTO 포함              |
| 민감정보    | 개인정보·중요정보 여부         |
| 마스킹      | 마스킹 규칙                    |

## 4.17 예시

| Java 필드     | DB 컬럼        | 타입          |  PK | 요청 | 조건 | 응답 |
| ------------- | -------------- | ------------- | --: | ---: | ---: | ---: |
| customerNo    | CUSTOMER_NO    | VARCHAR2(20)  |   Y |    Y |    Y |    Y |
| customerName  | CUSTOMER_NAME  | VARCHAR2(100) |   N |    N |    N |    Y |
| customerGrade | CUSTOMER_GRADE | VARCHAR2(10)  |   N |    N |    N |    Y |
| updateDtm     | UPDATE_DTM     | TIMESTAMP     |   N |    N |    N |    Y |

## 4.18 검토사항

- Java 필드명과 DB 컬럼이 일관되게 변환되는가
- 조회조건에 인덱스를 사용할 수 있는가
- PK 없이 UPDATE·DELETE하지 않는가
- 개인정보가 Response에 무조건 노출되지 않는가
- 화면에 필요하지 않은 컬럼을 전체 조회하지 않는가
- 금액·비율·날짜 타입이 String으로 남용되지 않는가

## 4.19 완료조건

- DA·DBA가 컬럼과 데이터 타입을 확인한다.
- PK·인덱스·NULL 여부가 확정된다.
- 개인정보 등급과 마스킹 여부가 정의된다.
- 필드가 요청·조건·응답 중 어디에 사용되는지 명확하다.

---

# 단계 4. 화면·이벤트 모델링

## 4.20 목적

사용자의 화면 행위와 실행할 서버 거래를 정의한다.

## 4.21 Model Studio 입력항목

| 항목           | 예시                  |
| -------------- | --------------------- |
| 화면 ID        | `SV-CUS-0001`         |
| 화면명         | 고객 종합정보 조회    |
| 이벤트 ID      | `SV-CUS-0001-E01`     |
| 이벤트명       | 조회 버튼 클릭        |
| 이벤트 유형    | CLICK                 |
| UI 객체 ID     | `btnSearch`           |
| 성공 처리      | 조회결과 그리드 표시  |
| 빈 결과 처리   | 조회 결과 없음 메시지 |
| 실패 처리      | 표준 오류 메시지 표시 |
| 중복 클릭 방지 | 사용                  |
| 로딩 표시      | 사용                  |

## 4.22 화면 이벤트 흐름

```text
화면 진입
  ↓
검색조건 입력
  ↓
조회 버튼 클릭
  ↓
입력값 1차 검증
  ↓
표준 요청 생성
  ↓
ServiceId 호출
  ↓
정상·빈 결과·업무 오류·시스템 오류 처리
```

## 4.23 개발자 확인사항

- 화면 하나가 여러 ServiceId를 호출할 수 있는가
- 이벤트 하나가 순차적으로 여러 거래를 호출하는가
- 중복 클릭 시 중복 등록 위험이 있는가
- 조회 결과가 없을 때와 오류가 발생했을 때를 구분하는가
- 화면의 사용자 입력값을 서버가 다시 검증하는가

## 4.24 완료조건

- 모든 버튼·선택·다운로드 이벤트에 이벤트 ID가 있다.
- 서버 호출 이벤트에는 ServiceId가 연결된다.
- 정상·빈 결과·업무 오류·시스템 오류 처리 방식이 정의된다.
- 화면 ID와 업무코드가 일치한다.

---

# 단계 5. ServiceId·거래 모델링

## 4.25 목적

화면 이벤트가 실행할 서버 업무 거래와 운영정책을 정의한다.

## 4.26 Model Studio 입력항목

| 항목        | 예시                        |
| ----------- | --------------------------- |
| ServiceId   | `SV.Customer.selectSummary` |
| 거래코드    | `SV-INQ-0001`               |
| 처리유형    | INQUIRY                     |
| HTTP Method | POST                        |
| Endpoint    | `/sv/online`                |
| Timeout     | 3,000ms                     |
| 권한코드    | `SV_CUSTOMER_VIEW`          |
| 감사대상    | Y                           |
| 거래통제    | 사용                        |
| 멱등성      | 조회는 불필요               |
| 설명        | 고객번호 기준 종합정보 조회 |

## 4.27 ServiceId 설계 기준

```text
{업무코드}.{도메인}.{행위}
```

권장 행위 예시:

| 처리유형  | 권장 행위                  |
| --------- | -------------------------- |
| 단건 조회 | `select`, `get`, `find`    |
| 목록 조회 | `selectList`, `searchList` |
| 등록      | `create`, `register`       |
| 변경      | `update`, `modify`         |
| 삭제      | `delete`, `remove`         |
| 승인      | `approve`                  |
| 실행      | `execute`, `process`       |
| 다운로드  | `download`, `export`       |

## 4.28 금지 예시

| 금지 ServiceId               | 문제             |
| ---------------------------- | ---------------- |
| `SV.Service.service1`        | 업무 의미가 없음 |
| `SV.Customer.doWork`         | 행위가 불명확    |
| `CUSTOMER_SELECT`            | 표준 형식 위반   |
| `SV.Customer.selectSummary2` | 임시 숫자 사용   |
| `SV.Customer.test`           | 테스트성 식별자  |

## 4.29 완료조건

- 화면 이벤트와 ServiceId가 연결된다.
- ServiceId와 거래코드의 업무코드가 일치한다.
- 권한·Timeout·감사 여부가 정의된다.
- 동일 Workspace에 중복 ServiceId가 없다.
- 기존 Handler에 추가할 거래인지 신규 도메인인지 결정된다.

---

# 단계 6. DTO 모델링

## 4.30 DTO 구분

```text
Request DTO
  = 화면·호출자가 전달한 원본 입력

Criteria DTO
  = 조회·변경에 사용하는 내부 조건

Response DTO
  = 화면·호출자에게 반환할 결과

Row DTO
  = Mapper가 DB에서 조회한 결과
```

## 4.31 DTO 변환 흐름

```text
StandardRequest Body
  ↓
Request DTO
  ↓ 검증·정규화
Criteria DTO
  ↓
DAO·Mapper
  ↓
Row DTO
  ↓ 가공·마스킹
Response DTO
  ↓
StandardResponse
```

## 4.32 개발자가 정의할 내용

| DTO         | 정의 내용                                                |
| ----------- | -------------------------------------------------------- |
| Request     | 사용자 입력 필드와 필수 여부                             |
| Criteria    | WHERE 조건, 페이징, 정렬                                 |
| Response    | 화면에 반환할 최종 필드                                  |
| Row         | DB 조회 컬럼과 Java 타입                                 |
| 공통 Header | TCF 표준 Header를 사용하며 업무 DTO에 중복 정의하지 않음 |

## 4.33 금지사항

- Request DTO를 Mapper Parameter로 그대로 전달하지 않는다.
- DB Row DTO를 화면 응답으로 그대로 반환하지 않는다.
- 비밀번호·Token·민감정보를 Response에 포함하지 않는다.
- 모든 숫자와 날짜를 String으로 정의하지 않는다.
- 한 DTO를 조회·등록·변경에서 무조건 재사용하지 않는다.

## 4.34 완료조건

- 필드별 입력·조건·응답 역할이 구분된다.
- 요청 필수값과 Rule 검증이 연결된다.
- Row와 Response의 개인정보 처리 차이가 정의된다.
- DTO가 표준 Header 정보를 중복 보유하지 않는다.

---

# 단계 7. Rule 모델링

## 4.35 목적

업무 처리 전에 확인할 조건과 업무 판단을 명시한다.

## 4.36 Rule 분류

| 구분             | 예시                             |
| ---------------- | -------------------------------- |
| 형식 검증        | 고객번호 길이·형식               |
| 필수 검증        | 고객번호 필수                    |
| 범위 검증        | 조회기간 최대 1년                |
| 상태 검증        | 해지 고객 변경 금지              |
| 권한 검증        | 소속 지점 고객만 조회            |
| 중복 검증        | 동일 기준정보 중복 등록 금지     |
| 조합 검증        | 시작일이 종료일보다 늦을 수 없음 |
| 데이터 존재 검증 | 변경 대상 고객 존재 여부         |
| 업무시간 검증    | 특정 업무시간 이외 실행 금지     |

## 4.37 Rule 작성 원칙

```text
Rule은 업무 판단을 수행한다.

Rule은 화면을 알지 않는다.
Rule은 HTTP를 알지 않는다.
Rule은 직접 SQL을 실행하지 않는다.
Rule은 가능하면 부작용 없이 같은 입력에 같은 결과를 반환한다.
```

## 4.38 Rule 정의표

| Rule ID       | 조건                 | 실패 오류코드 | 메시지                             |
| ------------- | -------------------- | ------------- | ---------------------------------- |
| `SV-CUS-R001` | 고객번호가 비어 있음 | `SV-VAL-001`  | 고객번호는 필수입니다.             |
| `SV-CUS-R002` | 고객번호 길이 초과   | `SV-VAL-002`  | 고객번호 형식이 올바르지 않습니다. |
| `SV-CUS-R003` | 조회 권한 없음       | `SV-AUTH-001` | 조회 권한이 없습니다.              |

## 4.39 완료조건

- Rule별 조건과 오류코드가 정의된다.
- 화면 JavaScript 검증만으로 끝나지 않는다.
- Rule에서 DAO·Mapper를 직접 호출하지 않는다.
- 검증 순서와 오류 우선순위가 정의된다.

---

# 단계 8. DAO·Mapper·SQL 모델링

## 4.40 전체 관계

```text
Service
  ↓
DAO Method
  ↓
Mapper Interface Method
  ↓
Mapper XML Statement ID
  ↓
SQL
  ↓
Table·View
```

## 4.41 명명 예시

| 구분             | 예시                       |
| ---------------- | -------------------------- |
| DAO              | `SvCustomerDao`            |
| DAO Method       | `selectCustomerSummary`    |
| Mapper Interface | `SvCustomerMapper`         |
| Mapper Method    | `selectCustomerSummary`    |
| Mapper Namespace | Mapper Interface 전체 경로 |
| SQL ID           | `selectCustomerSummary`    |
| Table            | `SV_CUSTOMER_SUMMARY`      |

DAO Method, Mapper Method와 SQL ID는 특별한 이유가 없다면 동일하게 유지한다.

## 4.42 처리유형별 SQL 기준

### 조회

```sql
SELECT CUSTOMER_NO,
       CUSTOMER_NAME,
       CUSTOMER_GRADE
  FROM SV_CUSTOMER_SUMMARY
 WHERE CUSTOMER_NO = #{customerNo}
```

### 등록

```sql
INSERT INTO SV_CUSTOMER_SUMMARY (
       CUSTOMER_NO,
       CUSTOMER_NAME,
       CREATE_DTM
) VALUES (
       #{customerNo},
       #{customerName},
       CURRENT_TIMESTAMP
)
```

### 변경

```sql
UPDATE SV_CUSTOMER_SUMMARY
   SET CUSTOMER_NAME = #{customerName},
       UPDATE_DTM = CURRENT_TIMESTAMP
 WHERE CUSTOMER_NO = #{customerNo}
```

### 삭제

```sql
DELETE
  FROM SV_CUSTOMER_SUMMARY
 WHERE CUSTOMER_NO = #{customerNo}
```

## 4.43 SQL 검토사항

- UPDATE·DELETE에 식별 가능한 WHERE 조건이 있는가
- 조회조건 컬럼에 적절한 인덱스가 있는가
- `SELECT *`를 사용하지 않았는가
- 대량 목록에 페이징 또는 제한조건이 있는가
- Dynamic SQL이 의도하지 않은 전체 조회를 만들지 않는가
- DB 함수 사용이 인덱스를 무력화하지 않는가
- Mapper Timeout이 거래 Timeout보다 짧은가
- NULL과 빈 문자열 처리 기준이 명확한가
- 날짜·금액 변환을 SQL에 과도하게 넣지 않았는가

## 4.44 완료조건

- DAO Method와 SQL ID가 일치한다.
- 사용 테이블과 데이터 소유 도메인이 확인된다.
- UPDATE·DELETE 안전조건이 있다.
- DA·DBA가 SQL과 인덱스를 검토한다.
- 조회 결과 건수와 페이징 정책이 정의된다.

---

# 단계 9. 업무모델 통합검증

## 4.45 Model Studio 검증 실행

```text
모델 저장
  ↓
단일 모델 검증
  ↓
Workspace 전체 검증
  ↓
오류 수정
  ↓
재검증
  ↓
ERROR 0건
```

## 4.46 주요 자동검증 항목

| 영역      | 검증                              |
| --------- | --------------------------------- |
| 업무코드  | 대문자·길이·WAR 정합성            |
| 화면 ID   | 명명 형식과 업무코드              |
| 이벤트 ID | 화면 ID와의 관계                  |
| ServiceId | 형식·업무코드·중복                |
| 거래코드  | 유형·업무코드·중복                |
| 필드      | Java명·컬럼명·타입·중복           |
| 조회      | 조건·응답 필드 존재               |
| 등록·변경 | 요청 필드 존재                    |
| 변경·삭제 | PK·조건 필드 존재                 |
| 개인정보  | 민감정보 마스킹                   |
| 감사      | 중요 조회·변경 감사 여부          |
| Handler   | 동일 도메인 ServiceId 병합        |
| 추적성    | 화면–거래–프로그램–SQL–Table 연결 |

## 4.47 오류와 경고 처리

| 등급      | 조치                        |
| --------- | --------------------------- |
| ERROR     | 생성 금지, 반드시 수정      |
| WARNING   | 사유 검토 후 승인 또는 수정 |
| INFO      | 참고정보                    |
| EXCEPTION | 아키텍처 예외 승인 필요     |

## 4.48 완료조건

- ERROR가 0건이다.
- WARNING에 조치 또는 승인 사유가 기록된다.
- Workspace 중복 ServiceId가 없다.
- 화면에서 테이블까지 End-to-End 연결이 확인된다.
- 검증된 모델을 Baseline 후보로 저장한다.

---

# 단계 10. 코드·설계 산출물 생성

## 4.49 생성 절차

```text
코드 미리보기
  ↓
생성 파일 목록 확인
  ↓
단일 모델 또는 전체 Workspace 생성
  ↓
ZIP 다운로드
  ↓
manifest.json 확인
  ↓
임시 디렉터리에 압축 해제
```

## 4.50 생성 결과

```text
generated/
 ├─ src/main/java/
 │   ├─ Handler.java
 │   ├─ Facade.java
 │   ├─ Service.java
 │   ├─ Rule.java
 │   ├─ DAO.java
 │   ├─ Mapper.java
 │   └─ DTO.java
 ├─ src/main/resources/
 │   └─ mapper/...Mapper.xml
 ├─ db/
 │   ├─ ddl.sql
 │   └─ om-service-catalog.sql
 ├─ docs/
 │   ├─ screen-design.md
 │   ├─ transaction-design.md
 │   └─ traceability.csv
 ├─ http/
 │   └─ transaction.http
 ├─ test/
 │   └─ RuleTest.java
 └─ manifest.json
```

## 4.51 개발자 생성물 확인사항

- 파일 경로가 대상 업무 WAR와 일치하는가
- 패키지가 기존 Component Scan 범위에 포함되는가
- Mapper XML 위치가 MyBatis 설정과 일치하는가
- 동일 도메인 Handler에 기존 ServiceId가 보존되는가
- 기존 파일을 의도치 않게 덮어쓰지 않는가
- manifest의 모델·템플릿 버전이 기록되었는가

## 4.52 금지사항

- 생성 ZIP을 업무 저장소에 그대로 덮어쓰지 않는다.
- 생성 코드를 검토하지 않고 Commit하지 않는다.
- 자동 생성 SQL을 운영 DB에 바로 실행하지 않는다.
- OM 등록 SQL을 승인 없이 운영에 적용하지 않는다.

---

# 단계 11. 생성 코드 Git 적용

## 4.53 권장 적용 절차

```text
생성 ZIP 임시 경로 해제
  ↓
대상 업무 WAR와 파일 비교
  ↓
신규 파일과 변경 파일 구분
  ↓
필요 파일만 작업 Branch에 적용
  ↓
git diff 확인
  ↓
기존 ServiceId·Method 보존 확인
```

## 4.54 Git 확인 명령

```bash
git status
git diff --stat
git diff
```

## 4.55 적용 구분

| 파일 상태         | 처리                            |
| ----------------- | ------------------------------- |
| 신규 파일         | 패키지·명명 확인 후 추가        |
| 기존 파일과 동일  | 적용하지 않음                   |
| 기존 Handler 변경 | ServiceId 병합 내용 수동 검토   |
| 기존 Mapper 변경  | 기존 SQL 보존 후 Statement 추가 |
| 기존 DTO 변경     | 호환성·사용처 영향 확인         |
| 기존 테스트 변경  | 기존 시나리오 삭제 여부 확인    |

## 4.56 완료조건

- 생성물과 기존 코드의 Diff가 검토된다.
- 기존 기능 삭제·변경이 없는지 확인된다.
- 충돌 해결 근거가 기록된다.
- 생성 코드 적용 Commit과 업무 로직 Commit을 가능하면 분리한다.

---

# 단계 12. 업무 로직 구현

## 4.57 계층별 개발 책임

| 계층    | 개발자가 구현할 내용                  | 금지 내용            |
| ------- | ------------------------------------- | -------------------- |
| Handler | ServiceId 분기, DTO 변환, Facade 호출 | SQL·복잡한 업무 로직 |
| Facade  | 유스케이스 조립, 트랜잭션 경계        | 화면 로직            |
| Service | 업무 처리 순서, Rule·DAO·Client 조합  | HTTP 객체 직접 사용  |
| Rule    | 업무 검증과 판단                      | DB 직접 접근         |
| DAO     | Mapper 호출과 데이터 접근 추상화      | 업무 규칙            |
| Mapper  | SQL 계약                              | Java 업무 판단       |
| DTO     | 계층 간 데이터 계약                   | 무분별한 공통 재사용 |

## 4.58 표준 처리 흐름

```text
Handler
  ├─ ServiceId 확인
  ├─ Request DTO 변환
  └─ Facade 호출
        ↓
Facade
  ├─ Transaction 시작
  └─ Service 호출
        ↓
Service
  ├─ Rule 검증
  ├─ DAO 조회·변경
  ├─ 결과 조립
  └─ Response 반환
        ↓
Facade
  └─ Commit 또는 Rollback
        ↓
Handler
  └─ 표준 응답 Body 반환
```

## 4.59 오류처리 원칙

| 오류               | 처리                               |
| ------------------ | ---------------------------------- |
| 필수값 누락        | Rule에서 업무 검증 오류            |
| 데이터 없음        | 업무 정의에 따라 빈 결과 또는 오류 |
| 권한 없음          | 인증·권한 표준 오류                |
| 중복 데이터        | 중복 업무 오류                     |
| DB 제약 오류       | 표준 DB 오류로 변환                |
| SQL Timeout        | Timeout 오류로 변환                |
| 예상하지 못한 오류 | 시스템 오류로 변환                 |
| 외부연계 오류      | 대상 시스템·오류코드·TraceId 기록  |

## 4.60 완료조건

- 계층별 책임을 위반하지 않는다.
- 모든 업무 오류에 오류코드가 있다.
- 변경 거래의 트랜잭션 범위가 명확하다.
- 개인정보 마스킹과 감사 처리가 구현된다.
- 로그에 비밀번호·Token·개인정보 원문이 남지 않는다.

---

# 단계 13. Compile·단위·로컬 테스트

## 4.61 권장 테스트 순서

```text
정적 Compile
  ↓
Rule 단위테스트
  ↓
Service 단위테스트
  ↓
DAO·Mapper 통합테스트
  ↓
업무 WAR 기동
  ↓
HTTP 대표 거래 호출
```

## 4.62 명령 예시

```bash
./gradlew :sv-service:clean
./gradlew :sv-service:compileJava
./gradlew :sv-service:test
./gradlew :sv-service:bootRun
```

## 4.63 필수 단위테스트

| 대상        | 테스트                                  |
| ----------- | --------------------------------------- |
| Rule        | 정상값, NULL, 빈 값, 경계길이, 금지상태 |
| Service     | Rule 실패, 데이터 없음, 정상 조립       |
| DAO·Mapper  | SQL ID, Parameter, Result Mapping       |
| DTO         | 타입·필드·직렬화                        |
| Handler     | ServiceId별 분기                        |
| Transaction | 예외 발생 시 Rollback                   |
| 개인정보    | 마스킹 여부                             |

## 4.64 로컬 테스트 확인사항

- Handler가 ServiceId에 정상 등록되는가
- 중복 ServiceId로 기동 실패하지 않는가
- Mapper Interface와 XML이 연결되는가
- SQL Parameter와 DTO 필드명이 일치하는가
- 거래로그에 GUID·TraceId·ServiceId가 남는가
- 업무 오류와 시스템 오류가 구분되는가

## 4.65 완료조건

- Clean Build가 성공한다.
- 신규·변경 테스트가 모두 성공한다.
- 대표 거래가 로컬에서 정상 처리된다.
- 로그와 DB 결과가 예상과 일치한다.

---

# 단계 14. TCF 통합거래 테스트

## 4.66 전체 통합 경로

```text
화면 또는 HTTP Client
  ↓
Gateway 또는 JWT Filter
  ↓
OnlineTransactionController
  ↓
TCF.process()
  ↓
STF
  ├─ Header
  ├─ 인증·권한
  ├─ 거래통제
  ├─ Timeout
  └─ 거래로그 시작
  ↓
TransactionDispatcher
  ↓
Handler
  ↓
Facade → Service → Rule → DAO → Mapper
  ↓
ETF
  ↓
StandardResponse
```

## 4.67 필수 통합 테스트

| 구분        | 테스트 내용                   |
| ----------- | ----------------------------- |
| 정상        | 정상 입력과 정상 결과         |
| 입력 오류   | 필수값 누락·형식 오류         |
| 권한 오류   | 기능권한 없는 사용자          |
| 데이터 없음 | 0건 결과                      |
| 업무 오류   | 상태·중복·조건 위반           |
| DB 오류     | 제약조건·접속 오류            |
| Timeout     | 느린 SQL·외부연계             |
| 중복 요청   | 등록 버튼 중복 클릭           |
| 거래통제    | OM 차단 상태                  |
| 감사        | 중요 조회·변경 감사로그       |
| 추적성      | GUID·TraceId로 전체 로그 연결 |
| Rollback    | 일부 처리 후 오류 발생        |

## 4.68 완료조건

- 정상·업무 오류·시스템 오류·Timeout 응답이 구분된다.
- 실패 시 불완전한 DB 변경이 남지 않는다.
- 거래로그의 시작·종료 상태가 일치한다.
- 화면 오류 처리와 서버 오류코드가 일치한다.
- ServiceId로 Handler·SQL·테이블을 추적할 수 있다.

---

# 단계 15. 품질·보안·성능 검토

## 4.69 애플리케이션 구조 검토

- Handler가 DAO·Mapper를 직접 호출하지 않는가
- Rule이 데이터베이스에 접근하지 않는가
- 다른 업무 WAR의 Service·DAO를 Import하지 않는가
- 트랜잭션 경계가 Facade에 있는가
- ServiceId가 중복 등록되지 않는가
- 패키지 구조가 공식 프로파일과 일치하는가

## 4.70 SQL 검토

- 실행계획을 확인했는가
- Full Scan이 의도된 것인가
- 인덱스가 유효한가
- 예상 조회건수가 정의되었는가
- 대량 결과에 페이징이 있는가
- SQL Timeout이 적절한가
- 개인정보가 불필요하게 조회되지 않는가

## 4.71 보안 검토

- 사용자 입력값을 서버에서 검증하는가
- SQL 문자열 결합을 사용하지 않는가
- JWT Claim과 Header 정합성을 확인하는가
- 권한 검증이 화면에만 존재하지 않는가
- 개인정보를 로그에 남기지 않는가
- 다운로드·변경 거래가 감사 대상인가

## 4.72 성능 검토

| 대상          | 확인                |
| ------------- | ------------------- |
| 응답시간      | 목표 p95 이내       |
| SQL           | 평균·최대 실행시간  |
| DB Pool       | Connection 점유시간 |
| Tomcat Thread | 장시간 점유 여부    |
| 외부연계      | Timeout과 재시도    |
| 결과 건수     | 대량 조회 제한      |
| DTO           | 과도한 데이터 반환  |
| Mapper        | N+1 조회 여부       |

## 4.73 완료조건

- 아키텍처 구조 위반이 없다.
- Critical·High 보안결함이 없다.
- SQL 실행계획과 인덱스 검토가 완료된다.
- 목표 응답시간을 충족하거나 보완계획이 승인된다.

---

# 단계 16. 설계 산출물·OM 기준정보 정리

## 4.74 최종 산출물

| 산출물          | 주요 내용                         |
| --------------- | --------------------------------- |
| 화면설계서      | 화면·객체·이벤트·성공·실패 처리   |
| 거래설계서      | ServiceId·전문·권한·Timeout·오류  |
| 프로그램설계서  | Handler부터 Mapper까지 책임       |
| DTO 정의서      | 요청·조건·응답·Row 필드           |
| SQL 설계서      | SQL ID·테이블·조건·결과           |
| 추적성 매트릭스 | 화면–ServiceId–프로그램–SQL–Table |
| 테스트 결과     | 정상·오류·Timeout·Rollback        |
| OM 등록정보     | Catalog·거래통제·Timeout·감사     |
| 배포정보        | Commit·Artifact·환경설정          |
| 변경영향서      | 기존 기능·화면·DB 영향            |

## 4.75 OM 등록 확인

- ServiceId
- 거래코드
- 업무코드
- 서비스명
- 사용 여부
- Timeout
- 권한코드
- 감사 여부
- 담당 조직
- 대상 WAR
- 배포 버전
- 오류코드 범위

## 4.76 완료조건

- 설계서와 실제 코드명이 일치한다.
- ServiceId와 Handler 등록정보가 일치한다.
- Mapper Method와 SQL ID가 일치한다.
- OM 등록정보와 실제 Timeout 설정이 일치한다.
- 추적성 CSV에 누락 경로가 없다.

---

# 단계 17. Pull Request·코드리뷰·승인

## 4.77 권장 Commit 분리

```text
Commit 1: 자동 생성 골격 적용
Commit 2: 업무 규칙과 SQL 구현
Commit 3: 테스트 코드 추가
Commit 4: 설계·OM·설정 변경
```

## 4.78 Pull Request 필수 내용

- 요구사항 번호
- 화면 ID
- 이벤트 ID
- ServiceId
- 거래코드
- 변경 프로그램
- 변경 SQL·테이블
- 권한·감사 영향
- Timeout 영향
- 테스트 결과
- 배포·Rollback 방법
- Model·Template 버전

## 4.79 리뷰 역할

| 역할         | 리뷰 내용               |
| ------------ | ----------------------- |
| 업무 개발자  | 로직·오류·테스트        |
| AA           | 계층·트랜잭션·의존성    |
| DA·DBA       | 데이터·SQL·인덱스       |
| 보안         | 권한·개인정보·감사      |
| 프레임워크팀 | TCF 계약·ServiceId 등록 |
| 테스트팀     | 시나리오·회귀범위       |
| 운영팀       | 로그·모니터링·장애대응  |

## 4.80 완료조건

- 필수 리뷰어가 승인한다.
- 자동 Build와 테스트가 성공한다.
- 미해결 Critical·High 결함이 없다.
- 배포 및 Rollback 방법이 확인된다.
- 승인된 Branch로 Merge된다.

---

# 단계 18. 배포·Smoke Test·운영 확인

## 4.81 배포 전 확인

- Artifact 버전
- Commit ID
- 대상 WAR
- Context Path
- DB Script
- OM 등록 Script
- 환경설정 변경
- 배포 순서
- 재기동 필요 여부
- Rollback Artifact

## 4.82 배포 후 확인 순서

```text
WAR 배포
  ↓
Tomcat 기동 확인
  ↓
Spring Context 정상
  ↓
Handler·Mapper 등록 확인
  ↓
Health Check
  ↓
대표 ServiceId 호출
  ↓
DB 결과 확인
  ↓
거래로그·감사로그 확인
  ↓
화면 Smoke Test
```

## 4.83 Smoke Test

| 번호 | 테스트                   |
| ---: | ------------------------ |
|    1 | 업무 WAR 정상 기동       |
|    2 | 대표 조회 거래 정상      |
|    3 | 대표 변경 거래 정상      |
|    4 | 권한 없는 사용자 차단    |
|    5 | 업무 오류 메시지 정상    |
|    6 | 거래로그 시작·종료 정상  |
|    7 | 감사로그 정상            |
|    8 | Timeout 정책 정상        |
|    9 | 기존 핵심 거래 회귀 정상 |
|   10 | Rollback 가능성 확인     |

## 4.84 완료조건

- 대표 신규 거래가 운영 환경에서 정상이다.
- 기존 핵심 거래에 회귀 오류가 없다.
- 모니터링과 로그에서 ServiceId가 확인된다.
- 장애 시 이전 Artifact로 되돌릴 수 있다.

---

# 단계 19. 변경·호환성·폐기 관리

## 4.85 변경은 모델부터 수행

```text
변경 요구사항
  ↓
기존 업무모델 복제 또는 수정
  ↓
영향 화면·ServiceId·SQL 확인
  ↓
모델 재검증
  ↓
코드 재생성
  ↓
기존 코드와 Diff
  ↓
개발·시험·승인
  ↓
Baseline 갱신
```

## 4.86 변경 유형별 처리

| 변경           | 영향                               |
| -------------- | ---------------------------------- |
| 화면 필드 추가 | 화면·Request·Response·테스트       |
| ServiceId 변경 | 화면·Handler·OM·로그·권한          |
| 컬럼 추가      | DTO·Mapper·DDL·SQL·테스트          |
| 컬럼 타입 변경 | DTO·SQL·호환성·데이터 이행         |
| Rule 변경      | 오류코드·테스트·업무 영향          |
| Timeout 변경   | OM·Mapper·운영 영향                |
| SQL ID 변경    | DAO·Mapper·운영 추적 영향          |
| ServiceId 폐기 | 화면 호출 제거·OM 비활성·로그 보존 |

## 4.87 폐기 절차

```text
신규 호출 여부 조사
  ↓
화면·외부 소비자 확인
  ↓
대체 ServiceId 제공
  ↓
Deprecated 상태 등록
  ↓
호출 로그 관찰
  ↓
OM 비활성
  ↓
코드·SQL 제거
  ↓
문서와 모델 폐기 상태 변경
```

---

# 5. 책임 경계와 RACI

| 활동             |  BA |  UI | DEV |  AA | DA·DBA |  FW |  QA | SEC | OPS |
| ---------------- | --: | --: | --: | --: | -----: | --: | --: | --: | --: |
| 요구사항 정의    | A/R |   C |   C |   C |      C |   I |   C |   C |   I |
| 화면·이벤트 정의 |   C | A/R |   C |   C |      I |   I |   C |   I |   I |
| 도메인·ServiceId |   C |   C |   R |   A |      C |   C |   I |   I |   I |
| 테이블·컬럼      |   C |   I |   C |   C |    A/R |   I |   I |   C |   I |
| DTO·Rule 모델    |   C |   C | A/R |   C |      C |   I |   C |   C |   I |
| DAO·Mapper·SQL   |   I |   I |   R |   C |      A |   I |   C |   I |   I |
| 모델 검증        |   C |   C |   R |   A |      C |   C |   C |   C |   I |
| 코드 생성        |   I |   I |   R |   C |      I |   A |   I |   I |   I |
| 업무 구현        |   C |   I | A/R |   C |      C |   C |   C |   C |   I |
| 단위·통합시험    |   C |   C |   R |   C |      C |   I |   A |   C |   I |
| 보안검토         |   I |   C |   R |   C |      I |   C |   C |   A |   I |
| OM 등록          |   I |   I |   R |   C |      I |   C |   C |   C |   A |
| 배포·운영확인    |   I |   C |   R |   C |      C |   C |   C |   C |   A |

`R`: 수행, `A`: 최종 책임, `C`: 협의, `I`: 공유

---

# 6. 정상 처리 흐름

```text
[1] 화면에서 조회 버튼 클릭
  ↓
[2] UI가 Request Body와 ServiceId 생성
  ↓
[3] OnlineTransactionController가 요청 수신
  ↓
[4] STF가 Header·권한·거래통제·Timeout 확인
  ↓
[5] Dispatcher가 ServiceId의 Handler 선택
  ↓
[6] Handler가 Request DTO로 변환
  ↓
[7] Facade가 트랜잭션 시작
  ↓
[8] Service가 Rule 검증
  ↓
[9] DAO가 Mapper 호출
  ↓
[10] Mapper가 SQL 실행
  ↓
[11] Row를 Response DTO로 변환
  ↓
[12] Facade Commit
  ↓
[13] ETF가 표준 응답 생성
  ↓
[14] 거래로그·감사로그 종료
  ↓
[15] 화면이 결과 표시
```

---

# 7. 오류·Timeout·장애 흐름

## 7.1 입력 오류

```text
Request 입력
  ↓
Rule 검증 실패
  ↓
업무 검증 예외
  ↓
ETF 업무 오류 응답
  ↓
화면 입력항목 강조
```

## 7.2 DB 오류

```text
Mapper SQL 실행
  ↓
DB 예외
  ↓
표준 데이터 접근 예외로 변환
  ↓
Facade Rollback
  ↓
ETF 시스템 오류 응답
  ↓
오류로그·거래로그 종료
```

## 7.3 Timeout

```text
거래 실행
  ↓
거래 Timeout 초과
  ↓
Timeout Executor 중단 요청
  ↓
Facade Rollback
  ↓
Timeout 오류 응답
  ↓
ServiceId·SQL ID·경과시간 기록
```

## 7.4 OM 거래통제

```text
STF 거래통제 확인
  ↓
ServiceId 사용중지 상태
  ↓
Handler 실행 금지
  ↓
거래통제 오류 응답
  ↓
운영 통제 로그 기록
```

---

# 8. 정상 예시

```text
화면 ID       : SV-CUS-0001
이벤트 ID     : SV-CUS-0001-E01
ServiceId     : SV.Customer.selectSummary
거래코드      : SV-INQ-0001
Handler       : SvCustomerHandler
Facade        : SvCustomerFacade
Service       : SvCustomerService
Rule          : SvCustomerRule
DAO           : SvCustomerDao
Mapper        : SvCustomerMapper
SQL ID        : selectCustomerSummary
Table         : SV_CUSTOMER_SUMMARY
Timeout       : 3,000ms
감사대상      : Y
권한코드      : SV_CUSTOMER_VIEW
```

---

# 9. 금지 예시

```text
화면
  → 업무별 Controller
  → Service
  → Mapper
```

금지 이유:

- TCF 거래통제를 우회한다.
- ServiceId Dispatcher 추적성이 사라진다.
- 거래로그·Timeout·감사 정책이 누락될 수 있다.
- 업무별 Controller가 무분별하게 증가한다.

또한 다음 구현을 금지한다.

| 금지 구현                           | 이유                         |
| ----------------------------------- | ---------------------------- |
| Handler에서 Mapper 직접 호출        | 계층과 트랜잭션 경계 위반    |
| Rule에서 DAO 호출                   | 업무 판단과 데이터 접근 혼합 |
| Service에서 HttpServletRequest 사용 | 업무와 Web 계층 결합         |
| Mapper에서 업무 상태 판단           | SQL과 업무 규칙 혼합         |
| Request DTO를 Response로 반환       | 입력·출력 계약 혼합          |
| 생성 코드 무검토 운영 반영          | 자동 생성은 초안임           |
| UPDATE·DELETE WHERE 누락            | 대량 데이터 훼손 위험        |
| 운영정보를 모델에 저장              | 비밀번호·Token 유출 위험     |

---

# 10. 개발자 일일 실행 체크리스트

## 개발 시작

- [ ] 최신 기준 Branch를 받았다.
- [ ] 변경 전 Clean Build가 성공한다.
- [ ] 요구사항과 화면 ID를 확인했다.
- [ ] ServiceId 신규·변경 여부를 확인했다.
- [ ] 사용 테이블의 소유 도메인을 확인했다.

## 모델링

- [ ] 프로젝트·업무코드·도메인을 입력했다.
- [ ] 화면·이벤트를 입력했다.
- [ ] ServiceId·거래코드를 입력했다.
- [ ] Timeout·권한·감사 여부를 입력했다.
- [ ] 테이블·컬럼·PK를 입력했다.
- [ ] Request·Criteria·Response 사용 필드를 구분했다.
- [ ] Rule과 오류코드를 정의했다.
- [ ] DAO Method와 SQL ID를 정의했다.

## 생성

- [ ] 단일 모델 검증을 통과했다.
- [ ] Workspace 전체 검증을 통과했다.
- [ ] ERROR가 0건이다.
- [ ] 생성 미리보기를 확인했다.
- [ ] manifest를 확인했다.
- [ ] 생성 ZIP을 임시 경로에 해제했다.

## 구현

- [ ] Git Diff를 확인했다.
- [ ] Handler가 기존 ServiceId를 보존한다.
- [ ] Rule에 실제 업무 규칙을 구현했다.
- [ ] SQL을 DA·DBA 기준으로 보완했다.
- [ ] 오류코드와 메시지를 연결했다.
- [ ] 개인정보 마스킹을 적용했다.
- [ ] 트랜잭션과 Rollback 범위를 확인했다.

## 검증

- [ ] Compile이 성공한다.
- [ ] Rule 단위테스트가 성공한다.
- [ ] Mapper 통합테스트가 성공한다.
- [ ] 정상 거래가 성공한다.
- [ ] 업무 오류가 정상 반환된다.
- [ ] 시스템 오류가 표준화된다.
- [ ] Timeout이 정상 적용된다.
- [ ] 거래로그와 감사로그가 기록된다.
- [ ] 기존 거래 회귀테스트가 성공한다.

## 완료

- [ ] 화면·거래·프로그램·SQL 설계서를 갱신했다.
- [ ] 추적성 매트릭스를 갱신했다.
- [ ] OM 등록정보를 작성했다.
- [ ] Pull Request에 테스트 결과를 첨부했다.
- [ ] 배포·Rollback 방법을 기록했다.

---

# 11. 자동검증 및 품질 Gate

| Gate             | 통과조건                          |
| ---------------- | --------------------------------- |
| G1 요구사항 Gate | 화면·입출력·규칙·오류 정의        |
| G2 모델 Gate     | 업무코드·화면·ServiceId·필드 정합 |
| G3 데이터 Gate   | 테이블·PK·SQL·인덱스 검토         |
| G4 생성 Gate     | 모델 ERROR 0건, 생성물 확인       |
| G5 구현 Gate     | 계층 책임·트랜잭션·보안 준수      |
| G6 Build Gate    | Clean Build와 단위테스트 성공     |
| G7 통합 Gate     | 정상·오류·Timeout·Rollback 성공   |
| G8 Review Gate   | AA·DA·보안·업무 리뷰 승인         |
| G9 운영 Gate     | OM·로그·모니터링·배포 준비        |
| G10 완료 Gate    | 배포·Smoke·회귀 테스트 성공       |

---

# 12. 핵심 아키텍처 판단

## 12.1 모델이 코드보다 먼저다

코드는 모델을 구현한 결과여야 한다. 개발자가 코드부터 작성하면 화면·ServiceId·DTO·SQL·OM 정보가 서로 달라질 가능성이 커진다.

## 12.2 ServiceId가 업무 추적의 중심이다

화면 URL이나 Controller 클래스가 아니라 ServiceId가 화면 이벤트, Handler, 운영정책, 거래로그와 테스트를 연결해야 한다.

## 12.3 자동 생성과 자동 완성은 다르다

자동 생성기는 구조적 반복작업을 줄이는 도구다. 실제 업무 규칙, SQL 성능, 권한, 감사, 장애 영향은 개발자와 담당자가 판단해야 한다.

## 12.4 생성 파일의 수동 수정은 통제해야 한다

생성 파일을 무분별하게 수정하면 다음 재생성 때 변경이 사라진다. 생성 영역과 수동 영역을 분리하거나 생성 결과를 Diff 방식으로 병합해야 한다.

## 12.5 개발 완료는 Build 성공이 아니다

다음 조건이 모두 충족되어야 개발 완료로 판단한다.

```text
모델 검증
+ 코드 구현
+ Compile
+ 단위테스트
+ TCF 통합거래 테스트
+ SQL·보안 검토
+ 설계서·추적성 갱신
+ OM 등록
+ 코드리뷰
+ 배포·Smoke Test
```

---

# 13. 시사점

## 13.1 핵심 아키텍처 판단

NSIGHT Model Studio는 단순 CRUD 코드 생성기가 아니라 다음 관계를 통제하는 개발 기준정보 플랫폼으로 운영해야 한다.

```text
화면
↔ 이벤트
↔ ServiceId
↔ Handler
↔ Facade
↔ Service
↔ Rule
↔ DAO
↔ Mapper
↔ SQL
↔ Table
↔ OM
↔ 테스트
↔ 운영로그
```

## 13.2 주요 위험

| 위험                        | 대응                                |
| --------------------------- | ----------------------------------- |
| 생성 코드를 완성품으로 오해 | Human Review를 필수 Gate로 운영     |
| 기존 코드 덮어쓰기          | ZIP·Diff 병합 방식 적용             |
| 모델과 소스 불일치          | CI에서 모델–코드 정합성 검사        |
| SQL 품질 저하               | DA·DBA 실행계획 승인                |
| Rule 골격만 존재            | 업무 테스트와 코드리뷰 강화         |
| OM 사후 등록                | ServiceId 생성 시 OM 정보 동시 생성 |
| 템플릿 버전 불일치          | TCF 버전–템플릿 호환표 관리         |
| 도구 우회 개발              | Architecture Gate에서 추적성 확인   |

## 13.3 우선 보완 과제

1. DB Dictionary 기반 테이블·컬럼 Import
2. 기존 Git 소스 분석을 통한 ServiceId·Mapper 역수집
3. Model과 실제 코드 정합성 검사
4. Git Branch·Pull Request 자동 생성
5. OM Service Catalog API 직접 연동
6. WEBTOPSUITE·React 화면정의 Import
7. Protected Region 또는 AST 기반 코드 병합
8. ArchUnit·Checkstyle 품질 Gate 연계
9. DAVIS-CODER 기반 Rule·테스트 초안 생성
10. 중앙 Repository 기반 모델 Baseline·승인 관리

## 13.4 중장기 발전 방향

```text
1단계
로컬 Model Studio와 코드 생성

2단계
Git·DB·OM·CI/CD 연동

3단계
화면·요구사항·소스 자동 역분석

4단계
LLM 기반 업무 규칙·테스트 추천

5단계
모델 변경 시 영향분석·회귀테스트 자동선정

6단계
설계–코드–운영정보 Digital Thread 구축
```

---

# 14. 마무리말

NSIGHT TCF 자동화 개발의 핵심은 개발자를 코드 작성에서 제외하는 것이 아니다.

반복적이고 오류가 발생하기 쉬운 DTO·계층 클래스·Mapper·설계서 작성은 자동화하고, 개발자는 다음 업무에 집중하게 하는 것이다.

```text
업무 요구사항을 정확하게 이해한다.

도메인과 데이터 소유권을 판단한다.

업무 규칙과 예외를 구현한다.

안전하고 빠른 SQL을 작성한다.

권한·개인정보·감사를 검토한다.

오류·Timeout·장애를 시험한다.

운영자가 추적할 수 있는 시스템을 만든다.
```

따라서 개발자는 다음 원칙을 지켜야 한다.

```text
코드부터 만들지 않는다.

모델을 먼저 정의한다.

검증되지 않은 모델로 생성하지 않는다.

생성 코드를 그대로 운영에 반영하지 않는다.

ServiceId에서 화면·프로그램·SQL·테이블을 추적한다.

개발 완료를 Build 성공으로 판단하지 않는다.

운영에서 탐지·추적·복구할 수 있을 때
하나의 거래가 완성된 것으로 판단한다.
```
