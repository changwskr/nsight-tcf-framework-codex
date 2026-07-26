# NSIGHT SV 업무 샘플 프로그램 상세 설계서 v1.0

샘플 거래: **`SV.Customer.selectSummary`** (Single View 고객 요약 조회)  
개발팀이 다른 SV 거래를 만들 때 그대로 복제하는 표준 패턴입니다.

> **본 문서는 현재 프레임워크 구현 기준입니다.**
> 원 설계서 초안의 DTO/Lombok/`@Component("serviceId")` 방식 대신, 실제 TCF 규약인
> **Map 기반 body + `serviceIds()`/`doHandle` 도메인 Handler + `BusinessException(code, msg)`** 로 정리했습니다.
> DTO 버전이 필요하면 §12를 참고하세요.

| 관련 문서 | 내용 |
|-----------|------|
| [업무페이징.md](업무페이징.md) | 목록 거래(`searchList`) 페이징 표준 |
| [sv-service-페이징.md](sv-service-페이징.md) | SV 페이징 참조 구현 |
| [DAO처리.md](DAO처리.md) | DAO/Mapper 계층 규약 |
| [예외처리.md](예외처리.md) | 오류코드·StandardResponse |

---

## 1. 목적

| 목적 | 설명 |
|------|------|
| 개발 표준화 | SV 업무 프로그램의 패키지·클래스·Mapper·SQL 작성 방식 통일 |
| TCF 연계 | STF·Dispatcher·ETF 공통 처리 흐름과 업무 프로그램 연결 |
| 운영 추적성 | ServiceId·거래코드·GUID·SQL ID 기준 추적 |
| 장애 대응 | 오류코드·거래로그·Timeout·SQL ID 기준 분석 |
| 확장성 | 고객요약·상세·상품보유·거래이력 등 동일 패턴 확장 |

---

## 2. 샘플 거래 정의

| 항목 | 값 |
|------|-----|
| 업무코드 | `SV` |
| Endpoint | `POST /sv/online` |
| ServiceId | `SV.Customer.selectSummary` |
| 거래코드 | **`SV-INQ-0002`** (※ `SV-INQ-0001`은 `SV.Sample.inquiry`가 사용 중) |
| 거래명 | 고객 요약 조회 |
| 처리유형 | `INQUIRY` |
| DB | RDW (로컬은 H2 `SV_CUSTOMER`) |
| Timeout | 3초 |
| 감사로그 | Y |
| 페이징 | 미사용 (단건 조회) |
| 담당 WAR | `sv.war` (`sv-service`) |
| Handler | `com.nh.nsight.marketing.sv.entry.handler.SvCustomerHandler` |

### 2.1 카탈로그 (OM_SERVICE_CATALOG)

| 컬럼 | 값 |
|------|-----|
| `CATALOG_ID` | `CAT-002` |
| `SERVICE_ID` | `SV.Customer.selectSummary` |
| `BUSINESS_CODE` | `SV` |
| `TRANSACTION_CODE` | `SV-INQ-0002` |
| `PROCESSING_TYPE` | `INQUIRY` |
| `HANDLER_CLASS` | `SvCustomerHandler` |
| `AUTH_CODE` | `ROLE_SV_INQ` |
| `TIMEOUT_SEC` | `3` |
| `AUDIT_YN` | `Y` |
| `USE_YN` | `Y` |

등록 위치: `ServiceCatalogSeedData.java`(CAT-002) + `tcf-om/.../data.sql`.

---

## 3. 전체 처리 흐름

```text
[WebTopSuite / tcf-ui / 외부]
        │  POST /sv/online  (StandardRequest JSON)
        ▼
[tcf-web] OnlineTransactionController
        ▼
[tcf-core] TCF.process()
        ├─ STF.preProcess()  Header검증·GUID·세션·권한·거래통제·Timeout정책·거래로그(PROCESSING)
        ├─ Dispatcher        serviceId = SV.Customer.selectSummary → SvCustomerHandler
        ▼
[SV 업무]  Handler → Facade → Service → Rule → DAO → Mapper.xml
        ▼
[RDW / H2]  SV_CUSTOMER
        ▼
[ETF.postProcess]  StandardResponse 조립·오류매핑·마스킹·거래로그(SUCCESS/FAIL)·감사로그
```

성공/실패 판단은 HTTP Status가 아니라 `result.resultCode` 기준입니다.

---

## 4. 요청 전문

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0002",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "012345"
  },
  "body": {
    "customerNo": "CUST00000001",
    "baseDate": "20260703"
  }
}
```

| Body 항목 | 타입 | 필수 | 검증 |
|-----------|------|:---:|------|
| `customerNo` | String | Y | Null 금지, 20자 이하 |
| `baseDate` | String | N | `yyyyMMdd`, 미입력 시 당일 |

---

## 5. 응답 전문

### 5.1 정상

```json
{
  "header": { "serviceId": "SV.Customer.selectSummary", "guid": "..." },
  "result": { "resultCode": "SUCCESS", "messageCode": "S0000", "message": "정상 처리되었습니다." },
  "body": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "guid": "...",
    "customerNo": "CUST00000001",
    "customerName": "홍길동",
    "customerGrade": "GOLD",
    "branchCode": "012345",
    "branchName": "농협중앙지점",
    "totalBalance": 15000000,
    "loanBalance": 3000000,
    "productCount": 5,
    "lastTransactionDate": "20260702"
  }
}
```

### 5.2 오류

```json
{
  "result": { "resultCode": "FAIL", "messageCode": "E-SV-VAL-0001", "message": "고객번호는 필수입니다." },
  "body": null
}
```

---

## 6. 패키지 구조 (구현됨)

```text
sv-service/src/main/java/com/nh/nsight/marketing/sv
├── entry
│   ├── handler/SvCustomerHandler.java
│   └── facade/SvCustomerFacade.java
├── application
│   ├── service/SvCustomerService.java
│   └── rule/SvCustomerRule.java
└── persistence
    ├── dao/SvCustomerDao.java
    └── mapper/SvCustomerMapper.java

sv-service/src/main/resources
├── mapper/sv/SvCustomerMapper.xml
├── schema.sql   (SV_CUSTOMER)
└── data.sql     (고객 시드 3건)
```

> 현재 SV 모듈은 **Map 기반**이라 별도 DTO 패키지를 두지 않습니다. DTO 버전은 §12 참고.

---

## 7. 클래스별 구현

### 7.1 Handler — `SvCustomerHandler`

Dispatcher 진입점(SV.Customer 도메인 핸들러). `serviceIds()`로 담당 거래를 선언하고 `doHandle`에서 `serviceId`로 분기, body를 Facade로 전달만 합니다. (SQL·업무로직·트랜잭션 금지)

```java
@Component
public class SvCustomerHandler implements TransactionHandler {
    private static final String SELECT_SUMMARY = "SV.Customer.selectSummary";

    private final SvCustomerFacade facade;

    public SvCustomerHandler(SvCustomerFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(SELECT_SUMMARY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case SELECT_SUMMARY -> facade.selectCustomerSummary(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND, serviceId);
        };
    }
}
```

### 7.2 Facade — `SvCustomerFacade`

트랜잭션 경계(조회 = `readOnly`, timeout 3초).

```java
@Service
public class SvCustomerFacade {
    private final SvCustomerService service;

    public SvCustomerFacade(SvCustomerService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 3)
    public Map<String, Object> selectCustomerSummary(Map<String, Object> body, TransactionContext context) {
        return service.selectCustomerSummary(body, context);
    }
}
```

### 7.3 Service — `SvCustomerService`

Rule 검증 → DAO 조회 → 결과 검증 → 응답 조립.

```java
public Map<String, Object> selectCustomerSummary(Map<String, Object> body, TransactionContext context) {
    Map<String, Object> criteria = rule.buildSummaryCriteria(body);
    Map<String, Object> customer = dao.selectCustomerSummary(criteria);
    rule.validateSummaryResult(customer);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("businessCode", "SV");
    result.put("serviceId", context.getHeader().getServiceId());
    result.put("guid", context.getHeader().getGuid());
    result.putAll(customer);
    return result;
}
```

### 7.4 Rule — `SvCustomerRule`

입력값 검증 + criteria 조립 + 결과 존재 검증.

```java
public Map<String, Object> buildSummaryCriteria(Map<String, Object> body) {
    if (body == null) {
        throw new BusinessException("E-SV-VAL-0001", "고객번호는 필수입니다.");
    }
    String customerNo = stringValue(body.get("customerNo"));
    if (!StringUtils.hasText(customerNo)) {
        throw new BusinessException("E-SV-VAL-0001", "고객번호는 필수입니다.");
    }
    if (customerNo.length() > 20) {
        throw new BusinessException("E-SV-VAL-0002", "고객번호 길이가 올바르지 않습니다.");
    }
    Map<String, Object> criteria = new HashMap<>();
    criteria.put("customerNo", customerNo);
    // baseDate 있으면 추가
    return criteria;
}

public void validateSummaryResult(Map<String, Object> customer) {
    if (customer == null || customer.isEmpty()) {
        throw new BusinessException("E-SV-BIZ-0001", "조회된 고객 정보가 없습니다.");
    }
}
```

### 7.5 DAO — `SvCustomerDao`

Mapper 호출만 캡슐화. 업무 판단·트랜잭션 선언 금지.

```java
@Repository
public class SvCustomerDao {
    private final SvCustomerMapper mapper;

    public SvCustomerDao(SvCustomerMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> selectCustomerSummary(Map<String, Object> criteria) {
        return mapper.selectCustomerSummary(criteria);
    }
}
```

### 7.6 Mapper Interface — `SvCustomerMapper`

```java
@Mapper
public interface SvCustomerMapper {
    Map<String, Object> selectCustomerSummary(Map<String, Object> criteria);
}
```

---

## 8. MyBatis XML

위치: `src/main/resources/mapper/sv/SvCustomerMapper.xml`  
Namespace: `com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper`  
SQL ID: `selectCustomerSummary` (운영 추적 SQL ID: `SV.Customer.selectSummary`)

```xml
<select id="selectCustomerSummary" parameterType="map" resultType="map" timeout="3">
    /* SQL_ID: SV.Customer.selectSummary */
    SELECT
          CUSTOMER_NO            AS customerNo
        , CUSTOMER_NAME          AS customerName
        , CUSTOMER_GRADE         AS customerGrade
        , BRANCH_CODE            AS branchCode
        , BRANCH_NAME            AS branchName
        , TOTAL_BALANCE          AS totalBalance
        , LOAN_BALANCE           AS loanBalance
        , PRODUCT_COUNT          AS productCount
        , LAST_TRANSACTION_DATE  AS lastTransactionDate
      FROM SV_CUSTOMER
     WHERE CUSTOMER_NO = #{customerNo}
</select>
```

> **로컬(H2)** 은 단일 `SV_CUSTOMER` 테이블.
> **운영(RDW)** 은 `RDW_CUSTOMER` + 잔액/여신/상품/거래요약 테이블 LEFT JOIN으로 동일 컬럼을 구성합니다.

### 8.1 운영 RDW JOIN 참고 SQL

```sql
SELECT C.CUSTOMER_NO AS customerNo, C.CUSTOMER_NAME AS customerName, ...
  FROM RDW_CUSTOMER C
  LEFT JOIN RDW_BRANCH B                      ON C.BRANCH_CODE = B.BRANCH_CODE
  LEFT JOIN RDW_CUSTOMER_BALANCE A            ON C.CUSTOMER_NO = A.CUSTOMER_NO
  LEFT JOIN RDW_CUSTOMER_LOAN L               ON C.CUSTOMER_NO = L.CUSTOMER_NO
  LEFT JOIN RDW_CUSTOMER_PRODUCT_SUMMARY P    ON C.CUSTOMER_NO = P.CUSTOMER_NO
  LEFT JOIN RDW_CUSTOMER_TRANSACTION_SUMMARY T ON C.CUSTOMER_NO = T.CUSTOMER_NO
 WHERE C.CUSTOMER_NO = #{customerNo}
```

---

## 9. 로컬 데이터

`schema.sql` — `SV_CUSTOMER` DDL  
`data.sql` — 고객 시드 3건 (`CUST00000001~3`)

| CUSTOMER_NO | 이름 | 등급 |
|-------------|------|------|
| CUST00000001 | 홍길동 | GOLD |
| CUST00000002 | 김영희 | SILVER |
| CUST00000003 | 이철수 | VIP |

---

## 10. 목록(페이징) 확장

`selectSummary`는 단건이라 페이징이 없습니다.  
목록 거래 `SV.Customer.searchList`(SV-INQ 계열)는 [업무페이징.md](업무페이징.md)·[sv-service-페이징.md](sv-service-페이징.md) 표준을 따릅니다.

| 항목 | 값 |
|------|-----|
| ServiceId | `SV.Customer.searchList` |
| 페이징 | 사용 (`pageNo`/`pageSize`) |
| 기본/최대 pageSize | 100 / 500 |
| offset | 서버 Rule `(pageNo-1)×pageSize` |

---

## 11. 오류코드 (OM_ERROR_CODE 등록됨)

| 오류코드 | 발생 위치 | 설명 | 사용자 메시지 |
|----------|-----------|------|---------------|
| `E-SV-VAL-0001` | Rule | `customerNo` 미입력 | 고객번호는 필수입니다. |
| `E-SV-VAL-0002` | Rule | 고객번호 길이 초과 | 고객번호 길이가 올바르지 않습니다. |
| `E-SV-BIZ-0001` | Rule | 조회 결과 없음 | 조회된 고객 정보가 없습니다. |
| `E-SV-DB-0001` | DAO/Mapper | SQL 오류 | 일시적으로 조회할 수 없습니다. |
| `E-SV-TIME-0001` | TCF/Facade/MyBatis | 3초 초과 | 고객정보 조회가 지연되고 있습니다. |

모든 예외는 TCF 공통 후처리에서 표준 오류코드로 변환되어 `StandardResponse`·거래로그·감사로그에 기록됩니다.

---

## 12. (참고) DTO 버전

DTO 기반으로 가려면 `dto/SvCustomerSummaryRequest`·`SvCustomerSummaryResponse`를 만들고
`request.getBodyAs(...)`를 사용합니다. 단, **현재 SV 모듈 전체가 Map 기반**이므로
혼용 시 팀 규약을 먼저 정하세요. (본 샘플은 Map 기반으로 통일)

---

## 13. Timeout

| 구분 | 값 | 위치 |
|------|---:|------|
| Online Transaction | 3초 | TCF Timeout Policy (카탈로그 `TIMEOUT_SEC=3`) |
| Spring Transaction | 3초 | `SvCustomerFacade @Transactional(timeout=3)` |
| MyBatis Query | 3초 | `SvCustomerMapper.xml timeout="3"` |

---

## 14. 테스트

### 14.1 tcf-ui 화면

| 화면 | 방법 |
|------|------|
| SV 다중 서비스 테스트 | `/sv/index-multi.html` → **Customer Summary Inquiry** 선택 |

샘플 요청: `tcf-ui/.../sample-requests/sv-customer-summary.json`

### 14.2 curl

```bash
curl -X POST http://localhost:8086/sv/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/sv-customer-summary.json
```

### 14.3 테스트 케이스

| No | 입력 | 기대 |
|---:|------|------|
| 1 | `CUST00000001` | SUCCESS, 고객 요약 |
| 2 | `customerNo` 누락 | `E-SV-VAL-0001` |
| 3 | 20자 초과 | `E-SV-VAL-0002` |
| 4 | 미존재 고객 | `E-SV-BIZ-0001` |
| 5 | 미등록 serviceId | Dispatcher 오류 |

---

## 15. 개발자 체크리스트

- [x] ServiceId가 `OM_SERVICE_CATALOG`에 등록 (CAT-002)
- [x] Handler `serviceIds()` 목록에 카탈로그 SERVICE_ID 포함
- [x] Facade 트랜잭션 timeout 설정
- [x] Rule 필수값 검증
- [x] DAO는 Mapper 호출만
- [x] SQL은 Mapper XML + SQL_ID 주석
- [x] MyBatis Timeout 설정
- [x] 오류코드 `OM_ERROR_CODE` 등록
- [ ] (운영) RDW JOIN SQL로 교체

---

## 16. 확장

`SV.Customer.selectSummary`를 복제해 `SV.Customer.selectDetail`,
`SV.Product.selectHoldingList`, `SV.Transaction.searchHistory`,
`SV.Customer.searchList`(목록=페이징) 등을 동일 구조로 만들 수 있습니다.

```text
카탈로그 등록 → Handler → Facade/Service/Rule → DAO/Mapper → TCF 공통 처리 자동 연결
```
