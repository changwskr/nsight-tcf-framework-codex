# NSIGHT TCF 업무모델 자동화 개발 체계

> **도구 런타임 안내 (2026-07-25)**  
> 실행 모듈은 Spring Boot `tcf-ai-methodology` (포트 8787, H2 DB)입니다.  
> 아래 본문은 방법론·절차 설명이며, Python `run.bat` 언급이 있으면  
> `./gradlew :tcf-ai-methodology:bootRun` 또는 모듈 `run.bat`으로 대체하십시오.  
> 사용 안내: [README.md](README.md) · 모듈: [../README.md](../README.md)

요청하신 내용을 **개발 방법론 + 화면 기반 모델링 절차 + 실행 가능한 자동화 도구**로 구현했습니다.

NSIGHT TCF에서 화면 이벤트와 업무 프로그램을 연결하는 기준은 Controller가 아니라 `ServiceId`이며, 전체 추적 경로는 `화면 → 이벤트 → ServiceId → Handler → Facade → Service → Rule·DAO → Mapper → SQL → Table`로 유지해야 합니다.
또한 현재 기준 소스는 도메인 Handler 하나가 `serviceIds()`를 통해 여러 ServiceId를 처리할 수 있으므로, 자동 생성기도 ServiceId마다 Handler를 복제하지 않고 **동일 도메인의 거래를 하나의 Handler로 병합**하도록 구현했습니다.

---

## 1. 자동화 개발의 기본 원칙

```text
업무 요구사항
  ↓
업무코드·도메인 정의
  ↓
테이블·컬럼 정의
  ↓
화면·이벤트 정의
  ↓
ServiceId·거래코드 정의
  ↓
DTO 입출력·조회조건 정의
  ↓
Rule·DAO·Mapper·SQL 모델링
  ↓
자동검증
  ↓
코드·SQL·설계서 생성
  ↓
개발자 업무규칙 보완
  ↓
Compile·Test·코드리뷰
  ↓
OM 등록·배포·운영
```

자동화 대상과 사람의 판단 영역을 다음처럼 분리했습니다.

| 구분     | 자동화 대상                                      | 사람의 검토 대상             |
| -------- | ------------------------------------------------ | ---------------------------- |
| 식별자   | 화면 ID, 이벤트 ID, ServiceId, 거래코드 형식     | 업무 소유권과 도메인 경계    |
| DTO      | Request, Criteria, Response, Row 생성            | 업무 의미와 개인정보 분류    |
| 프로그램 | Handler, Facade, Service, Rule, DAO, Mapper 골격 | 복잡한 업무 규칙과 예외      |
| SQL      | SELECT·INSERT·UPDATE·DELETE 초안                 | Join, 실행계획, 인덱스, 성능 |
| 운영     | OM Catalog 등록 SQL, Timeout 초안                | 실제 운영정책과 장애 영향    |
| 품질     | 명명·중복·추적성·필수항목 검증                   | 최종 승인과 예외 수용        |
| 문서     | 화면·거래설계서, 추적성 CSV                      | 설계 의사결정과 책임 승인    |

---

# 2. 자동화 개발 방법론

## 단계별 표준 절차

| 단계 | 수행 내용                    | 주요 산출물                          | 완료 Gate                    |
| ---: | ---------------------------- | ------------------------------------ | ---------------------------- |
|   M0 | Workspace·프로젝트 기준 등록 | 프로젝트, BASE 패키지, 업무코드, WAR | 업무코드–WAR–Context 정합성  |
|   M1 | 업무 도메인 정의             | 도메인, 담당 조직, 데이터 소유권     | 도메인 간 직접 DAO 접근 금지 |
|   M2 | 테이블 모델 정의             | 테이블, 컬럼, PK, 타입, NULL         | DA·DBA 검토                  |
|   M3 | 화면·이벤트 정의             | 화면 ID, 이벤트 ID, UI 객체          | 이벤트별 호출 거래 확정      |
|   M4 | 서비스·거래 정의             | ServiceId, 거래코드, Timeout, 권한   | 중복·명명·OM 정책 검증       |
|   M5 | DTO 정의                     | 요청·조건·응답 필드                  | 필수값·타입·길이 정합성      |
|   M6 | Rule 정의                    | 필수·길이·업무검증 규칙              | 오류코드·금지조건 확정       |
|   M7 | DAO·Mapper·SQL 정의          | DAO Method, SQL ID, Table 관계       | WHERE·PK·변경컬럼 검토       |
|   M8 | 모델 자동검증                | 오류·경고 목록                       | ERROR 0건                    |
|   M9 | 코드·산출물 자동생성         | Java, XML, SQL, 문서, 테스트         | 생성 manifest 확인           |
|  M10 | 개발자 보완·코드리뷰         | 실제 Rule·SQL·외부연계               | Compile·구조검사 통과        |
|  M11 | 통합·오류·Timeout 시험       | 거래·권한·장애 테스트                | 테스트 결함 해소             |
|  M12 | OM 등록·배포·운영            | Catalog, 거래통제, 감사, 배포        | 운영 추적성 확인             |

이 절차는 문서 존재 여부만 확인하는 것이 아니라 요구사항·설계·코드·OM 기준정보·시험 결과가 서로 일치하는지를 Gate로 판단하는 방식입니다.

---

# 3. 자동화 도구 구조

도구명은 **NSIGHT Model Studio v0.1.0**으로 구성했습니다.

```text
[브라우저 모델링 화면]
 프로젝트·도메인
 화면·이벤트
 서비스·거래
 테이블·필드
 검증·추적성
 코드 미리보기
        │
        ▼
[Model API]
 저장·조회·복제·삭제
        │
        ├──────────────┐
        ▼              ▼
[Model Repository] [Validation Engine]
 JSON 저장          명명·중복·필드·보안·추적성
        │              │
        └───────┬──────┘
                ▼
       [Generation Engine]
 Java / XML / SQL / Markdown / CSV
                │
                ▼
       Preview / Workspace ZIP
```

외부 라이브러리 없이 Python 표준 라이브러리만 사용하므로 개발 PC에서 바로 실행할 수 있습니다.

---

# 4. 화면 구성

| 단계 | 화면 기능                                                    |
| ---: | ------------------------------------------------------------ |
|    1 | 프로젝트명, BASE 패키지, 업무코드, WAR, Context Path, 도메인 |
|    2 | 화면 ID, 화면명, 이벤트 ID, UI 객체, 성공·실패 처리          |
|    3 | ServiceId, 거래코드, 처리유형, Method, Timeout, 권한, 감사   |
|    4 | 테이블, 컬럼, Java·DB 타입, PK, 요청·조건·응답, 민감정보     |
|    5 | 모델 오류·경고와 End-to-End 추적 경로                        |
|    6 | 생성 파일 목록, 코드 미리보기, ZIP 생성                      |

여러 모델을 저장한 뒤 **전체 Workspace 생성**을 실행하면 같은 업무코드·도메인의 거래를 묶어 하나의 Handler·Facade·Service·Rule·DAO·Mapper로 병합합니다.

---

# 5. 생성되는 프로그램 구조

현재 소스 호환형 프로파일의 기본 구조입니다.

```text
com.nh.nsight.marketing.sv
 ├─ entry
 │   ├─ handler
 │   │   └─ SvCustomerHandler.java
 │   └─ facade
 │       └─ SvCustomerFacade.java
 ├─ application
 │   ├─ service
 │   │   └─ SvCustomerService.java
 │   ├─ rule
 │   │   └─ SvCustomerRule.java
 │   └─ dto/customer
 │       ├─ CustomerSummaryRequest.java
 │       ├─ CustomerSummaryCriteria.java
 │       └─ CustomerSummaryResponse.java
 └─ persistence
     ├─ dao
     │   └─ SvCustomerDao.java
     ├─ dto/customer
     │   └─ CustomerSummaryRow.java
     └─ mapper
         └─ SvCustomerMapper.java
```

MyBatis XML은 다음 경로로 생성됩니다.

```text
src/main/resources/mapper/sv/SvCustomerMapper.xml
```

업무 패키지는 업무코드·도메인·계층 책임을 식별할 수 있어야 하며, 패키지 구조 자체가 의존성과 변경 경계를 통제하는 수단입니다.

도구에는 두 가지 프로파일을 넣었습니다.

| 프로파일         | 목적                                      |
| ---------------- | ----------------------------------------- |
| `CURRENT_SOURCE` | 현재 NSIGHT 기준 소스와 즉시 병합         |
| `DOMAIN_FIRST`   | `업무코드 → 도메인 → 계층` 목표 구조 적용 |

---

# 6. 자동 생성 산출물

한 개 업무모델 기준으로 다음 파일을 생성합니다.

| 영역     | 생성 결과                                      |
| -------- | ---------------------------------------------- |
| Java     | Handler, Facade, Service, Rule, DAO, Mapper    |
| DTO      | Request, Criteria, Response, Row               |
| MyBatis  | Mapper XML과 SQL Statement                     |
| DB       | 테이블 DDL 초안                                |
| OM       | Service Catalog 등록 SQL 초안                  |
| 테스트   | Rule 단위테스트 골격                           |
| 거래시험 | 표준 HTTP 요청 파일                            |
| 화면설계 | 화면·이벤트 정의서                             |
| 거래설계 | ServiceId 기반 거래설계서                      |
| 추적성   | 화면–ServiceId–프로그램–SQL–Table CSV          |
| 품질     | 자동검증·CI/CD Gate 체크리스트                 |
| 관리     | 생성 파일과 ServiceId를 기록한 `manifest.json` |

샘플 모델에서는 총 **20개 산출물**이 생성됩니다.

---

# 7. 적용된 자동검증

## 식별자 검증

- 업무코드 대문자 2~3자리
- 화면 ID `{업무}-{세구분}-{4자리}`
- 이벤트 ID `{화면ID}-E{2자리}`
- ServiceId `{업무}.{도메인}.{행위}`
- 거래코드 `{업무}-{INQ|REG|UPD|DEL|EXE}-{4자리}`
- Java 필드 lowerCamelCase
- DB 객체 UPPER_SNAKE_CASE

## 데이터·SQL 검증

- 필드명과 컬럼명 중복
- 조회·변경·삭제 거래의 조건 필드 존재
- 조회 거래의 응답 필드 존재
- 등록·변경 거래의 요청 필드 존재
- PK 누락 경고
- 처리유형과 거래코드 유형 정합성
- UPDATE·DELETE 안전조건
- Mapper Method와 SQL ID 일치

## 운영·보안 검증

- 변경 거래의 감사로그 대상 여부
- 민감정보의 마스킹 규칙
- ServiceId·거래코드 Workspace 중복
- 동일 도메인 DTO·Method 중복
- 화면 ID·이벤트 ID·업무코드 정합성

---

# 8. 실행 방법

압축을 해제한 뒤 Windows에서는 다음 파일을 실행합니다.

```bat
run.bat
```

Linux 또는 macOS에서는 다음과 같습니다.

```bash
chmod +x run.sh
./run.sh
```

브라우저 주소:

```text
http://127.0.0.1:8787
```

Python 3.10 이상만 필요하며 별도의 패키지 설치는 필요하지 않습니다.

---

# 9. 검증 결과

구현 후 다음 시험을 수행했습니다.

| 시험                                    | 결과      |
| --------------------------------------- | --------- |
| Python 구문검사                         | 통과      |
| JavaScript 구문검사                     | 통과      |
| 자동화 도구 Unit Test                   | 5건 통과  |
| 샘플 모델 자동검증                      | 오류 0건  |
| 동일 도메인 ServiceId 병합              | 통과      |
| 중복 ServiceId 차단                     | 통과      |
| 생성 Java JDK 21 컴파일                 | 통과      |
| API Health·Validate·Generate Smoke Test | 통과      |
| ZIP 무결성 검사                         | 통과      |
| 샘플 생성 산출물                        | 20개 생성 |

생성 Java는 Spring·MyBatis·TCF 계약 Stub을 함께 구성하여 JDK 21 `javac`로 실제 컴파일 검증했습니다.

---

# 10. 현재 버전의 경계

이 결과물은 **실행 가능한 로컬 MVP**이며, 다음 항목은 운영형 2단계 개발이 필요합니다.

- Oracle Dictionary 기반 테이블 자동 Import
- Git 저장소 자동 적용·Diff·Merge Request
- 실제 OM Service Catalog API 연계
- WEBTOPSUITE·React 화면 정의 Import
- 생성 코드 보호영역 또는 AST 기반 병합
- 중앙 DB 기반 모델 버전·승인·Baseline
- 사내 SSO·RBAC·감사로그
- 실제 NSIGHT 전체 Gradle Build Pipeline 연계
- DAVIS-CODER를 이용한 Rule·테스트 초안 보완

생성된 Rule과 SQL은 업무·DA·DBA·보안 담당자의 검토 없이 바로 운영에 적용해서는 안 됩니다.

---

## 결과 파일

- [NSIGHT Model Studio 전체 소스](sandbox:/mnt/data/NSIGHT_Model_Studio_v0.1.0.zip)
- [자동화 개발 방법론 전문](sandbox:/mnt/data/nsight_model_studio/docs/NSIGHT_Automated_Development_Methodology.md)
- [기준 소스 정합성 검토](sandbox:/mnt/data/nsight_model_studio/docs/SOURCE_ALIGNMENT.md)
- [SV 고객조회 샘플 생성 결과](sandbox:/mnt/data/nsight_model_studio/examples/NSIGHT_SV_Customer_Sample_Generated.zip)
- [실행 및 사용 안내](sandbox:/mnt/data/nsight_model_studio/README.md)

핵심적으로 이번 결과는 단순 코드 생성기가 아니라 **화면·테이블·ServiceId·DTO·Rule·Mapper·SQL·OM·테스트를 하나의 모델로 통제하는 개발 절차의 기준본**입니다.
