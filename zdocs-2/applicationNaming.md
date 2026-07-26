# TCF 어플리케이션 네이밍 정리 (v1)

NSIGHT TCF **어플리케이션 네이밍 원칙 v1.0** 기준입니다.  
핵심은 **업무코드 → Context → WAR → Package → ServiceId → Handler → Facade → Service → Rule → DAO/Mapper → SQL ID → 거래로그/오류코드**가 하나의 선으로 이어지게 만드는 것입니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **업무코드 우선** | 모든 이름의 시작점 | `SV`, `OM`, `EB` … |
| **실행 키** | TCF 라우팅 | `serviceId` (URL 아님) |
| **6계층** | 표준 패키지 | `entry` → `application` → `persistence` |
| **추적성** | 운영·로그 | ServiceId ↔ Handler ↔ SQL ID ↔ errorCode |
| **DB 분리** | RDW / ADW / SESSIONDB | Mapper 물리 분리 |

원칙 문서: `docs/설계자료/NSIGHT 애플리케이션 네이밍 원칙 v1.pdf`  
관련: [네이밍.md](네이밍.md) · [어플리케이션계층.md](어플리케이션계층.md) · [mybatisNaming.md](mybatisNaming.md)

---

## 1. 최상위 네이밍 원칙

| 원칙 | 기준 | 예시 |
|------|------|------|
| **업무코드 우선** | 모든 이름은 업무코드로 시작 | `SV`, `OM`, `MG` |
| **Context 일치** | 하나의 업무코드 = 하나의 Context | `/sv`, `sv.war`, 패키지 `.sv` |
| **실행 기준은 ServiceId** | Dispatcher는 URL이 아니라 `serviceId`로 Handler 탐색 | `SV.Customer.selectSummary` |
| **Java Class** | `PascalCase`, 의미 있는 이름 | `SvCustomerHandler` |
| **Java Method/Field** | `lowerCamelCase`, 동사+대상 | `selectCustomerSummary()` |
| **Package** | 소문자, 업무코드+계층 | `com.nh.nsight.marketing.sv.entry.handler` |
| **DB 객체** | `UPPER_SNAKE_CASE` | `TCF_TRANSACTION_LOG` |
| **MyBatis SQL ID** | 업무·대상·행위 연결 | `selectCustomerSummary` |
| **약어 남용 금지** | `Svc`, `Mgr`, `Proc` 금지 | `Service`, `Manager` 사용 |
| **운영 추적성** | 로그·거래코드·오류코드에서 원업무 즉시 식별 | `E-SV-BIZ-0001` |

---

## 2. 업무코드 기준 네이밍

`settings.gradle` 기준 모듈과 업무코드 매핑입니다. 향후 `CC`, `BC`, `CM` 등 확장 시에도 동일 규칙을 적용합니다.

| 업무코드 | 업무명 | Context | WAR | Gradle Module | Java Prefix | Package |
|----------|--------|---------|-----|---------------|-------------|---------|
| IC | Integration Customer | `/ic` | `ic.war` | `ic-service` | `Ic` | `ic` |
| PC | Private Customer | `/pc` | `pc.war` | `pc-service` | `Pc` | `pc` |
| MS | Mini Single View | `/ms` | `ms.war` | `ms-service` | `Ms` | `ms` |
| SV | Single View | `/sv` | `sv.war` | `sv-service` | `Sv` | `sv` |
| PD | Product | `/pd` | `pd.war` | `pd-service` | `Pd` | `pd` |
| EB | EBM | `/eb` | `eb.war` | `eb-service` | `Eb` | `eb` |
| EP | Event Processing | `/ep` | `ep.war` | `ep-service` | `Ep` | `ep` |
| SS | Sales Support | `/ss` | `ss.war` | `ss-service` | `Ss` | `ss` |
| MG | Message | `/mg` | `mg.war` | `mg-service` | `Mg` | `mg` |
| OM | Operation Management | `/om` | `om.war` | `tcf-om` | `Om` | `om` |
| GW | Gateway | `/gw` | `gw.war` | `tcf-gateway` | `Gateway` | `gateway` |
| JWT | JWT Auth | `/jwt` | `jwt.war` | `tcf-jwt` | `Jwt` | `jwt` |
| UI | UI Relay | `/ui` | `ui.war` | `tcf-ui` | `Ui` | `ui` |
| UJ | Gateway Test UI | `/uj` | `uj.war` | `tcf-uj` | `Uj` | `uj` |
| BATCH | Batch | `/batch` | `batch.war` | `tcf-batch` | `Batch` | `batch` |

**확장 예:** `cc-service` → `/cc`, `cc.war`, `CcXxxHandler`, `com.nh.nsight.marketing.cc...`

---

## 3. 표준 Package 구조

```text
com.nh.nsight.marketing.{businessCode}
├─ entry
│  ├─ handler          TCF TransactionHandler
│  ├─ facade           유스케이스·트랜잭션 경계
│  └─ web              REST Controller (필요 시)
├─ application
│  ├─ service          업무 처리 절차
│  ├─ rule             업무 규칙·판단
│  └─ scheduler        @Scheduled
├─ persistence
│  ├─ dao              Mapper 호출·조건 조립
│  └─ mapper           MyBatis Interface
├─ client              외부 API/WAS 호출
├─ config              Spring 설정
└─ support             상수·유틸·도메인 헬퍼
```

**루트 패키지 예외**

| 모듈 | 루트 패키지 |
|------|-------------|
| 업무 WAR | `com.nh.nsight.marketing.{bc}` |
| JWT | `com.nh.nsight.auth.jwt` |
| Gateway | `com.nh.nsight.gateway` |
| TCF Core/Web | `com.nh.nsight.tcf.{area}` |

| Package | 역할 | 클래스 예시 |
|---------|------|-------------|
| `entry.handler` | TCF 거래 진입 (도메인당 1개) | `SvCustomerHandler` |
| `entry.facade` | Handler↔Service 유스케이스 조립 | `SvCustomerFacade` |
| `entry.web` | 별도 REST | `SvFileController` |
| `application.service` | 업무 처리 | `SvCustomerService` |
| `application.rule` | 업무 규칙 | `SvCustomerGradeRule` |
| `application.scheduler` | 스케줄러 | `EbEventCollectScheduler` |
| `persistence.dao` | DB 접근 조립 | `SvCustomerDao` |
| `persistence.mapper` | MyBatis Interface | `SvCustomerRdwMapper` |
| `client` | 외부 연동 | `SvCustomerProfileClient` |
| `config` | Spring 설정 | `SvDataSourceConfig` |
| `support` | 헬퍼·시드 | `SvConstants`, `*SeedData` |

---

## 4. Java 클래스 네이밍

### 4.1 계층별 규칙

| 계층 | 규칙 | 예시 |
|------|------|------|
| **Handler** | `{Prefix}{대상}Handler` (도메인당 1개) | `SvCustomerHandler` |
| **Facade** | `{Prefix}{대상}Facade` | `SvCustomerFacade` |
| **Service** | `{Prefix}{대상}Service` | `SvCustomerService` |
| **Rule** | `{Prefix}{대상}{규칙명}Rule` | `SvCustomerEligibilityRule` |
| **DAO** | `{Prefix}{대상}Dao` | `SvCustomerDao` |
| **Mapper** | `{Prefix}{대상}Mapper` / `{Prefix}{대상}RdwMapper` | `SvCustomerRdwMapper` |

### 4.2 DTO · 부가 클래스 (도입 시)

| 유형 | 규칙 | 예시 |
|------|------|------|
| Request DTO | `{대상}{행위}Request` | `CustomerSummaryInquiryRequest` |
| Response DTO | `{대상}{행위}Response` | `CustomerSummaryInquiryResponse` |
| Result DTO | `{대상}{결과명}Result` | `CustomerSummaryResult` |
| Command DTO | `{대상}{행위}Command` | `CampaignCreateCommand` |
| Criteria DTO | `{대상}SearchCriteria` | `CustomerSearchCriteria` |
| Config | `{Prefix}{대상}Config` | `SvMyBatisConfig` |
| Properties | `{Prefix}{대상}Properties` | `SvTimeoutProperties` |
| Validator | `{Prefix}{대상}Validator` | `SvCustomerValidator` |
| Client | `{Prefix}{외부대상}Client` | `SvProfileClient` |

> 현재 tcf-framework 다수 모듈은 **Map 기반 body**를 사용합니다. DTO 도입 시 위 규칙을 따릅니다.

### 4.3 Handler ↔ ServiceId 연결 (강제)

**핸들러는 도메인(application Service)당 1개**이며, 같은 도메인의 여러 `serviceId`를 `serviceIds()`로 선언하고 `doHandle`에서 분기합니다.

| 도메인(Handler) | 담당 ServiceId (`serviceIds()`) |
|-----------------|---------------------------------|
| `SvCustomerHandler` | `SV.Customer.selectSummary`, `SV.Customer.selectDetail`, … |
| `OmUserHandler` | `OM.User.inquiry` / `.save` / `.update` / `.delete` |
| `OmServiceCatalogHandler` | `OM.ServiceCatalog.inquiry` / `.save` / `.update` / `.delete` |

> `tcf-jwt`는 아직 거래별 핸들러(`JwtAuthLoginHandler` 등) 구조로, 도메인 핸들러 전환 대상입니다.

**Handler 1개 = 도메인 1개.** 한 도메인의 여러 행위(`{Action}`)는 하나의 핸들러가 `serviceId` 분기로 처리합니다. 단일 거래만 있으면 `serviceId()` 하나만 재정의해도 됩니다(하위호환).

### 4.4 Facade · Service 메서드

- Facade·Service 메서드명 = ServiceId의 **`{Action}`** (`selectSummary`, `create`, `inquiry` …)
- 시그니처 관례: `(Map<String, Object> body, TransactionContext context)` 또는 DTO
- **`@Transactional`은 Facade에만** 선언

---

## 5. ServiceId · 거래코드

### 5.1 ServiceId 형식

```text
{BUSINESS_CODE}.{BusinessObject}.{Action}
```

| 구분 | 규칙 | 예시 |
|------|------|------|
| `BUSINESS_CODE` | 대문자 업무코드 | `SV`, `OM`, `JWT` |
| `BusinessObject` | 업무대상 PascalCase | `Customer`, `Campaign`, `User` |
| `Action` | 처리행위 lowerCamelCase | `selectSummary`, `create`, `update` |

### 5.2 표준 Action 어휘

| 처리유형 | Action | 설명 |
|----------|--------|------|
| 단건 조회 | `selectDetail` | 상세 조회 |
| 목록 조회 | `selectList` | 목록 조회 |
| 요약 조회 | `selectSummary` | 요약 정보 |
| 존재 확인 | `exists` | 존재 여부 |
| 건수 조회 | `count` | 건수 |
| 등록 | `create` | 신규 생성 |
| 수정 | `update` | 수정 |
| 삭제 | `delete` | 논리 삭제 우선 |
| 저장 | `save` | 등록/수정 통합 |
| 발급 | `issue` | 토큰·번호 발급 |
| 검증 | `verify` | 토큰·권한 검증 |
| 실행 | `execute` | 배치·거래 실행 |
| 재처리 | `retry` | 실패 재처리 |
| 다운로드 | `download` | 파일 다운로드 |
| 업로드 | `upload` | 파일 업로드 |

**인증·운영 확장 Action (현재 구현 포함):** `login`, `logout`, `session`, `ssoLogin`, `ssoIssue`, `refresh`, `revoke`, `deleteAll`, `inquiry` (OM Admin 레거시)

### 5.3 거래코드

```text
{BUSINESS_CODE}-{TYPE}-{NNNN}
```

| TYPE | 의미 | 예시 |
|------|------|------|
| `INQ` | 조회 | `SV-INQ-0001` |
| `REG` | 등록 | `CM-REG-0001` |
| `UPD` | 수정 | `OM-UPD-0001` |
| `DEL` | 삭제 | `OM-DEL-0001` |
| `EXE` | 실행 | `BT-EXE-0001` |
| `DWN` | 다운로드 | `UD-DWN-0001` |
| `UPL` | 업로드 | `UD-UPL-0001` |
| `AUTH` | 인증 | `JWT-AUTH-0001` |

- `serviceId`와 **1:1 대응** 권장
- 도메인별 3자 코드(`ERR`, `USR` 등)는 OM Admin 기존 카탈로그와 병행 가능 — 신규는 위 TYPE 표준 우선

---

## 6. MyBatis 연계 (요약)

상세: [mybatisNaming.md](mybatisNaming.md)

| 항목 | v1 표준 |
|------|---------|
| Mapper Interface | `{Prefix}{대상}Mapper` / `{Prefix}{대상}RdwMapper` |
| Mapper XML | `mapper/{bc}/{MapperName}.xml` |
| namespace | Mapper Interface **FQCN** |
| SQL ID | Mapper method와 **1:1** |

### 6.1 SQL ID (v1 표준)

| SQL 유형 | Method / SQL ID | 예시 |
|----------|-----------------|------|
| 단건 조회 | `select{대상}Detail` | `selectCustomerDetail` |
| 목록 조회 | `select{대상}List` | `selectCustomerList` |
| 요약 조회 | `select{대상}Summary` | `selectCustomerSummary` |
| 건수 | `count{대상}` | `countCustomerList` |
| 존재 | `exists{대상}` | `existsCustomer` |
| 등록 | `insert{대상}` | `insertCampaign` |
| 수정 | `update{대상}` | `updateCampaign` |
| 삭제 | `delete{대상}` | `deleteCampaign` |
| 논리삭제 | `update{대상}DeleteYn` | `updateUserDeleteYn` |
| 병합 | `merge{대상}` | `mergeServiceCatalog` |

```java
public interface SvCustomerMapper {
    CustomerSummaryResult selectCustomerSummary(CustomerSummaryInquiryRequest request);
    CustomerDetailResult selectCustomerDetail(CustomerDetailInquiryRequest request);
    List<CustomerResult> selectCustomerList(CustomerSearchCriteria criteria);
    int countCustomerList(CustomerSearchCriteria criteria);
}
```

---

## 7. RDW / ADW / SESSIONDB / LOGDB Mapper 분리

| DB | Mapper 명명 | 예시 | 용도 |
|----|-------------|------|------|
| RDW | `{Prefix}{대상}RdwMapper` | `SvCustomerRdwMapper` | 실시간 조회 |
| ADW | `{Prefix}{대상}AdwMapper` | `CmCampaignAdwMapper` | 분석·집계 |
| SESSIONDB | `{Prefix}{대상}SessionMapper` | `OmSessionMapper` | 세션 |
| LOGDB | `{Prefix}{대상}LogMapper` | `OmTransactionLogMapper` | 거래로그·감사 |
| OMDB | `{Prefix}{대상}Mapper` | `OmUserMapper` | 운영·기준정보 |

**같은 업무대상이라도 RDW와 ADW SQL을 한 Mapper에 혼재하지 않습니다.**

```text
좋음: SvCustomerRdwMapper + SvCustomerAdwMapper
나쁨: SvCustomerMapper 안에 RDW SQL + ADW SQL 혼재
```

---

## 8. DB 객체 네이밍

| 객체 | 규칙 | 예시 |
|------|------|------|
| 업무 테이블 | `{업무코드}_{대상}` | `SV_CUSTOMER_SUMMARY` |
| TCF 공통 | `TCF_{대상}` | `TCF_TX_LOG` |
| OM 관리 | `OM_{대상}` | `OM_USER`, `OM_SERVICE_CATALOG` |
| JWT | `TCF_JWT_*` / `JWT_*` | `TCF_JWT_TOKEN` |
| UD 파일 | `UD_{대상}` | `UD_FILE_META` |
| 컬럼 | `UPPER_SNAKE_CASE` | `SERVICE_ID`, `TRANSACTION_CODE` |
| PK | `PK_{TABLE}` | `PK_OM_USER` |
| FK | `FK_{FROM}_{TO}` | `FK_OM_USER_AUTH_GROUP` |
| UK | `UK_{TABLE}_{컬럼}` | `UK_OM_USER_LOGIN_ID` |
| Index | `IX_{TABLE}_{컬럼}` | `IX_TCF_TX_LOG_GUID` |
| Sequence | `SQ_{TABLE}` | `SQ_TCF_TRANSACTION_LOG` |
| View | `VW_{업무}_{대상}` | `VW_SV_CUSTOMER_SUMMARY` |

**공통 컬럼:** `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY`, `USE_YN`, `DELETE_YN`, `VERSION_NO`, `DESCRIPTION`

상세: [테이블정보.md](테이블정보.md)

---

## 9. URL · API · Controller

NSIGHT는 **POST `/{businessCode}/online` + JSON 표준 전문 + ServiceId Dispatcher**가 중심입니다.

| 구분 | 표준 | 예시 |
|------|------|------|
| 업무 온라인 | `/{businessCode}/online` | `/sv/online` |
| OM 온라인 | `/om/online` | `/om/online` |
| Gateway | `/gw/{businessCode}/online` | `/gw/sv/online` |
| 파일 API | `/ud/files` | `/om/ud/files` |
| Actuator | `/{context}/actuator/health` | `/sv/actuator/health` |

| 구분 | 클래스명 |
|------|----------|
| 공통 온라인 | `OnlineTransactionController` (tcf-web) |
| Gateway | `GatewayOnlineController` |
| 파일 | `UdFileController` |
| UI Relay | `TransactionRelayService` (tcf-ui) |

- 업무별 Controller를 남발하지 않고 **TCF 공통 진입점** 우선
- REST 리소스 URL(`/sv/customer/list`)과 TCF URL(`/sv/online`) **혼용 금지** — 기본은 `/online`

---

## 10. 오류코드 네이밍

```text
E-{DOMAIN}-{CATEGORY}-{NNNN}
```

| 구분 | 예시 | 의미 |
|------|------|------|
| Header | `E-TCF-HDR-0001` | 필수 Header 누락 |
| 거래통제 | `E-TCF-CTL-0001` | 미등록 거래 |
| Timeout | `E-TCF-TIME-0001` | 온라인 Timeout |
| SV 업무 | `E-SV-BIZ-0001` | Single View 업무 오류 |
| OM 관리 | `E-OM-BIZ-0001` | 운영관리 오류 |
| JWT | `E-JWT-AUTH-0001` | 토큰 검증 실패 |
| 파일 | `E-UD-FILE-0001` | 파일 업로드 실패 |

상세: [예외처리.md](예외처리.md)

---

## 11. 전체 네이밍 연결 예시

### 11.1 SV 고객 요약 조회

| 항목 | 표준값 |
|------|--------|
| 업무코드 | `SV` |
| Context / WAR | `/sv`, `sv.war` |
| Module | `sv-service` |
| Package Root | `com.nh.nsight.marketing.sv` |
| ServiceId | `SV.Customer.selectSummary` |
| 거래코드 | `SV-INQ-0001` |
| Handler | `SvCustomerHandler` (SV.Customer.* 도메인, selectSummary로 분기) |
| Facade | `SvCustomerFacade` |
| Service | `SvCustomerService` |
| Rule | `SvCustomerEligibilityRule` |
| DAO | `SvCustomerDao` |
| Mapper | `SvCustomerRdwMapper` |
| Mapper XML | `mapper/sv/SvCustomerRdwMapper.xml` |
| SQL ID | `selectCustomerSummary` |
| Result DTO | `CustomerSummaryResult` |
| 오류코드 | `E-SV-BIZ-0001` |

### 11.2 OM 사용자 조회

| 항목 | 표준값 |
|------|--------|
| 업무코드 | `OM` |
| Context / WAR | `/om`, `om.war` |
| Module | `tcf-om` |
| Package Root | `com.nh.nsight.marketing.om` |
| ServiceId | `OM.User.inquiry` |
| 거래코드 | `OM-INQ-0001` |
| Handler | `OmUserHandler` (OM.User.* 도메인, inquiry로 분기) |
| Facade | `OmUserFacade` |
| Service | `OmUserService` |
| DAO | `OmUserDao` |
| Mapper | `OmUserMapper` |
| Mapper XML | `mapper/om/OmUserMapper.xml` |
| SQL ID | `selectUserList` |
| 오류코드 | `E-OM-BIZ-0001` |

### 11.3 JWT SSO 토큰 발급 (내부)

| 항목 | 표준값 |
|------|--------|
| ServiceId | `JWT.Auth.ssoIssue` |
| Handler | `JwtAuthSsoIssueHandler` |
| Facade | `JwtAuthFacade` |
| Service | `JwtAuthService` |
| Mapper | `JwtTokenMapper` |
| SQL ID | `insertJwtToken` |
| 오류코드 | `E-JWT-AUTH-0001` |

---

## 12. 금지 규칙

| 금지 | 나쁜 예 | 표준 |
|------|---------|------|
| 의미 없는 약어 | `SvCustSvc` | `SvCustomerService` |
| 계층명 누락 | `SvCustomer` | `SvCustomerService`, `SvCustomerMapper` |
| DB 혼합 Mapper | `SvMapper` | `SvCustomerRdwMapper` |
| ServiceId ↔ Handler 도메인 불일치 | `SV.Customer.selectSummary` / `SvSearchHandler` | `SvCustomerHandler` |
| SQL ID 중복·모호 | 여러 Mapper에 `selectList`만 | `selectCustomerList`, `selectCampaignList` |
| REST·TCF URL 혼용 | `/sv/customer/list` + `/sv/online` | `/sv/online` + ServiceId |
| Java SQL 직접 작성 | `String sql = "SELECT..."` | Mapper XML |
| 업무로직 SQL 은닉 | `CASE WHEN` 남용 | Rule / Service 분리 |
| RDW/ADW 혼재 | 한 Mapper에 분석·실시간 SQL | `RdwMapper`, `AdwMapper` 분리 |
| Handler → DAO 직접 | 계층 우회 | Facade 경유 |
| `Common*`, `Util2` | 책임 모호 | 도메인명 명시 |

---

## 13. 최종 적용 흐름

```text
업무코드
  ↓
Context / WAR / Module
  ↓
Package
  ↓
ServiceId
  ↓
Handler
  ↓
Facade
  ↓
Service
  ↓
Rule
  ↓
DAO
  ↓
Mapper Interface
  ↓
Mapper XML
  ↓
SQL ID
  ↓
거래로그 / 오류코드 / 감사로그
```

---

## 14. 현재 코드베이스와의 차이 (참고)

v1 PDF를 **목표 표준**으로 하되, 기존 `tcf-om` 등에는 아래 레거시 패턴이 남아 있을 수 있습니다. **신규 개발은 v1 표준**을 따르고, 기존 코드는 점진 정렬합니다.

| 영역 | v1 표준 | 현재 tcf-om 등 (레거시) |
|------|---------|-------------------------|
| 목록 Action | `selectList` | `inquiry` |
| SQL ID | `selectUserList` | `searchUsers` |
| Mapper 집합 | `OmUserMapper` | `OmOperationMapper` (다도메인 집합) |
| 거래코드 TYPE | `INQ`, `UPD`, `DEL` | `ERR`, `USR`, `CAT` 등 도메인 3자 |

---

## 15. 신규 거래 체크리스트

1. 업무코드·Context·Module·Package prefix 정의
2. `serviceId` = `{BC}.{Object}.{Action}` (표준 Action 어휘)
3. `Handler` = `{Prefix}{Object}Handler`(도메인당 1개), `serviceIds()`에 거래 추가 + `case` 분기
4. `Facade` / `Service` 메서드명 = `{Action}`
5. `Rule` 검증 — Body는 Rule, Header는 STF
6. `DAO` / `Mapper` / SQL ID — [mybatisNaming.md](mybatisNaming.md)
7. RDW/ADW Mapper 분리 여부 확인
8. `transactionCode` = `{BC}-{TYPE}-{NNNN}`
9. `errorCode` = `E-{DOMAIN}-{CATEGORY}-{NNNN}`
10. 샘플 JSON · 카탈로그 · Timeout 시드 등록

---

## 16. 구현 소스 (학습 순서)

| 순서 | 모듈 | 파일 |
|------|------|------|
| 1 | tcf-core | `dispatch/TransactionDispatcher.java` |
| 2 | tcf-web | `entry/web/OnlineTransactionController.java` |
| 3 | sv-service | `SvSampleHandler` → `SvSampleFacade` → `SvSampleService` |
| 4 | tcf-om | `OmUserHandler`, `OmErrorCodeFacade` |
| 5 | tcf-jwt | `JwtAuthSsoIssueHandler`, `JwtAuthFacade` (거래별 핸들러, 전환 대상) |
| 6 | eb-service | `EbUserHandler`, `EbEventPublishScheduler` |

---

## 관련 문서

- [어플리케이션계층.md](어플리케이션계층.md) — 계층 책임·호출 규칙·TX
- [네이밍.md](네이밍.md) — TCF 통합 네이밍 (기존 OM 카탈로그 포함)
- [mybatisNaming.md](mybatisNaming.md) — MyBatis·SQL ID 상세
- [DAO처리.md](DAO처리.md) — DAO 패턴·페이징
- [테이블정보.md](테이블정보.md) — 물리 테이블
- [예외처리.md](예외처리.md) — BusinessException
