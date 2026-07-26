# 제10장. TransactionHandler 개발

| 항목 | 내용 |
| --- | --- |
| **편** | 제3편 · 거래 개발 실무 |
| **장** | 제10장 |
| **파일** | `제03편/10-TransactionHandler-개발.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 10.1 Handler 등록·serviceIds() 패턴

TransactionHandler는 `serviceId`로 식별되는 **업무 진입 Adapter**이다. Controller가 아니고, Service도 아니고, DAO도 아니다. TCF 공통 처리와 업무 계층 사이를 연결한다.

개발 기준 요약:

| 구분 | 기준 |
| --- | --- |
| 역할 | serviceId 기준 업무 처리 진입점 |
| 위치 | `entry.handler` 패키지 |
| 실행 주체 | TransactionDispatcher |
| 호출 대상 | Facade |
| 업무 로직 | 작성 금지 |
| DB 접근 | 작성 금지 |
| 응답 전문 조립 | 작성 금지 (ETF 담당) |
| 등록 | `@Component` + `serviceIds()` |

`TransactionHandler` 인터페이스 핵심 메서드:

```java
public interface TransactionHandler {
  List<String> serviceIds();
  Object doHandle(StandardRequest request, TransactionContext context);
}
```

단일 ServiceId Handler 예시:

```java
@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler implements TransactionHandler {

  private final SvCustomerFacade customerFacade;
  private final ObjectMapper objectMapper;

  @Override
  public List<String> serviceIds() {
    return List.of("SV.Customer.selectSummary");
  }

  @Override
  public Object doHandle(StandardRequest request, TransactionContext context) {
    SvCustomerSummaryReq req = objectMapper.convertValue(
        request.getBody(), SvCustomerSummaryReq.class);
    return customerFacade.selectSummary(req, context);
  }
}
```

복수 ServiceId Handler는 `serviceIds()`에 여러 ID를 등록하고 `doHandle`에서 분기한다. 도메인 단위로 Handler를 묶는 패턴이 일반적이다. Spring 기동 시 Dispatcher가 Registry를 구성하고, 동일 ServiceId가 두 Handler에 등록되면 기동 오류 또는 나중 등록 우선 정책이 적용된다(환경 설정 확인).

---

## 10.2 Online Endpoint 호출 규약

Online Endpoint는 `POST /{businessCode}/online`이다. HTTP Method는 POST, Content-Type은 `application/json`, Charset은 UTF-8이다.

호출 URL 예시:

```text
로컬 bootRun:  POST http://localhost:8086/sv/online
ztomcat:       POST http://localhost:8080/sv/online
Gateway 경유:  POST http://localhost:8100/gw/sv/online
```

요청 본문은 StandardRequest JSON이다. 세션 거래는 `Cookie: JSESSIONID=...`를 포함한다. JWT 거래는 `Authorization: Bearer {token}`을 포함한다(Gateway·tcf-jwt 경유 시).

curl 테스트 예시:

```bash
curl -X POST http://localhost:8086/sv/online \
  -H "Content-Type: application/json" \
  -d '{
    "header": {
      "businessCode": "SV",
      "serviceId": "SV.Customer.selectSummary",
      "transactionCode": "SV-INQ-0001",
      "channelId": "TCFUI",
      "userId": "TESTUSER"
    },
    "body": { "customerNo": "CUST00000001" }
  }'
```

업무 WAR에 별도 REST Endpoint를 추가하지 않는다. 모든 온라인 거래는 `/online` 하나로 통일한다. 파일 업·다운로드 등 예외는 `entry/web` Controller를 사용한다(제11장 참조).

---

## 10.3 Facade · Service · Rule 구현

Handler 다음 계층인 Facade·Service·Rule은 실제 업무 로직을 담당한다.

**Facade** (`entry/facade`): 유스케이스 오케스트레이션, `@Transactional` 경계.

```java
@Service
@RequiredArgsConstructor
public class SvCustomerFacade {

  private final SvCustomerService customerService;
  private final SvCustomerRule customerRule;

  @Transactional(readOnly = true)
  public SvCustomerSummaryRes selectSummary(
      SvCustomerSummaryReq req, TransactionContext ctx) {
    customerRule.validateSummaryRequest(req);
    return customerService.selectSummary(req);
  }

  @Transactional
  public SvCustomerUpdateRes updateCustomer(
      SvCustomerUpdateReq req, TransactionContext ctx) {
    customerRule.validateUpdateRequest(req);
    return customerService.updateCustomer(req, ctx);
  }
}
```

**Service** (`application/service`): 도메인 로직, DAO 호출, EAI 연동, Cache.

```java
@Service
@RequiredArgsConstructor
public class SvCustomerService {

  private final SvCustomerDao customerDao;

  public SvCustomerSummaryRes selectSummary(SvCustomerSummaryReq req) {
    SvCustomerSummaryRes res = customerDao.selectSummary(req);
    if (res == null) {
      throw new BusinessException("E-SV-0001", "고객번호를 찾을 수 없습니다.");
    }
    return res;
  }
}
```

**Rule** (`application/rule`): 입력·업무 규칙 검증.

```java
@Component
public class SvCustomerRule {

  public void validateSummaryRequest(SvCustomerSummaryReq req) {
    if (req.getCustomerNo() == null || req.getCustomerNo().isBlank()) {
      throw new BusinessException("E-COM-0001", "고객번호는 필수입니다.");
    }
  }
}
```

호출 순서: Handler → Facade → Rule → Service → DAO. Rule은 Service 호출 전에 실행한다. Service에서 다른 Service를 직접 호출하는 것보다 Facade에서 조합하는 것을 권장한다.

---

## 10.4 DAO · MyBatis Mapper

DAO는 Mapper를 래핑하는 Repository 역할이다. `@Repository`로 등록한다.

```java
@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

  private final SvCustomerMapper customerMapper;

  public SvCustomerSummaryRes selectSummary(SvCustomerSummaryReq req) {
    return customerMapper.selectSummary(req);
  }
}
```

Mapper 인터페이스와 XML:

```java
@Mapper
public interface SvCustomerMapper {
  SvCustomerSummaryRes selectSummary(SvCustomerSummaryReq req);
  List<SvCustomerListItemRes> selectList(SvCustomerListReq req);
  int insertCustomer(SvCustomerCreateReq req);
}
```

```xml
<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">
  <select id="selectSummary"
          resultType="com.nh.nsight.marketing.sv.entry.dto.response.SvCustomerSummaryRes">
    SELECT customer_no, customer_name, grade, total_asset
    FROM sv_customer
    WHERE customer_no = #{customerNo}
  </select>
</mapper>
```

DAO에서 비즈니스 판단을 하지 않는다. "조회 결과 null이면 예외"는 Service 책임이다. Mapper XML은 `resources/mapper/sv/`에 위치한다.

---

## 10.5 SQL 작성·페이징

SQL 작성 기준: RDW DataSource 사용(실시간 거래), Query Timeout 설정, 인덱스 컬럼 WHERE 조건, `SELECT *` 금지, 명시적 컬럼 나열, 대량 조회 시 페이징 필수.

페이징 패턴:

```xml
<select id="selectList" resultType="SvCustomerListItemRes">
  SELECT customer_no, customer_name, grade
  FROM sv_customer
  WHERE grade = #{grade}
  ORDER BY customer_no
  LIMIT #{pageSize} OFFSET #{offset}
</select>

<select id="countList" resultType="int">
  SELECT COUNT(*)
  FROM sv_customer
  WHERE grade = #{grade}
</select>
```

Service에서 `pageNo`, `pageSize`로 offset 계산: `offset = (pageNo - 1) * pageSize`. Response에 `totalCount`, `pageNo`, `pageSize`, `items`를 포함한다.

| 항목 | 기준 |
| --- | --- |
| 기본 pageSize | 20 |
| 최대 pageSize | 100 (Rule에서 검증) |
| COUNT 쿼리 | 목록 쿼리와 WHERE 조건 동일 |
| Query Timeout | 10초 (목록 조회) |

동적 SQL은 MyBatis `<if>`, `<choose>`를 사용한다. 문자열 concat으로 SQL을 조립하지 않는다(SQL Injection 방지).

---

## 10.6 서비스 간 연동 (tcf-eai)

업무 WAR 간 연동은 직접 의존이 아닌 **tcf-eai** HTTP/JSON Client를 사용한다. sv-service가 ic-service의 고객 조회를 호출하는 경우가 대표적이다.

설정 (`application.yml`):

```yaml
nsight:
  integration:
    services:
      IC:
        baseUrl: http://localhost:8082/ic
      SV:
        baseUrl: http://localhost:8086/sv
```

Client 호출 (Service 계층):

```java
@Service
@RequiredArgsConstructor
public class SvIntegrationService {

  private final TcfEaiClient eaiClient;

  public IcCustomerInquiryRes callIcCustomer(String customerNo, TransactionContext ctx) {
  StandardRequest request = StandardRequest.builder()
      .header(StandardHeader.builder()
          .businessCode("IC")
          .serviceId("IC.Customer.inquiry")
          .transactionCode("IC-INQ-0001")
          .channelId(ctx.getChannelId())
          .userId(ctx.getUserId())
          .build())
      .body(Map.of("customerNo", customerNo))
      .build();

  StandardResponse response = eaiClient.call("IC", request);
  // result 검증 후 body 변환
  return objectMapper.convertValue(response.getBody(), IcCustomerInquiryRes.class);
  }
}
```

EAI 호출은 Facade 트랜잭션 **밖**에서 실행하거나, 보상 로직을 Facade에 설계한다. EAI Timeout은 Online Timeout보다 짧게 설정한다. 연동 Contract는 `docs/architecture/46-service-integration-contract.md`를 참조한다.

---

## 장 요약

TransactionHandler는 `serviceIds()`로 등록하고 Facade만 호출하는 얇은 Adapter이다. Online Endpoint `POST /{businessCode}/online` 규약으로 표준 전문을 전송한다. Facade·Service·Rule·DAO·Mapper 6계층으로 업무 로직을 구현하고, 페이징·SQL 표준을 따른다. WAR 간 연동은 tcf-eai Client로 ServiceId 기반 HTTP 호출한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제9장 표준 전문과 DTO](./09-표준-전문과-DTO.md) |
| → 다음 | [제11장 품질 속성 구현](./11-품질-속성-구현.md) |

---

## 출처 색인

- [znsight-man/23-TransactionHandler-개발.md](../../znsight-man/23-TransactionHandler-개발.md)
- [zman/08-업무Handler개발.md](../../zman/08-업무Handler개발.md)
- [znsight-man/22-Online-Endpoint-기준.md](../../znsight-man/22-Online-Endpoint-기준.md)
- [znsight-man/24-Facade-개발.md](../../znsight-man/24-Facade-개발.md)
- [znsight-man/25-Service-개발.md](../../znsight-man/25-Service-개발.md)
- [znsight-man/26-Rule-개발.md](../../znsight-man/26-Rule-개발.md)
- [znsight-man/27-DAO-개발.md](../../znsight-man/27-DAO-개발.md)
- [znsight-man/28-MyBatis-Mapper-개발.md](../../znsight-man/28-MyBatis-Mapper-개발.md)
- [docs/architecture/07-DAO.md](../../docs/architecture/07-DAO.md)
- [docs/architecture/26-mybatis.md](../../docs/architecture/26-mybatis.md)
- [znsight-man/29-SQL-작성-기준.md](../../znsight-man/29-SQL-작성-기준.md)
- [znsight-man/30-페이징-처리-기준.md](../../znsight-man/30-페이징-처리-기준.md)
- [docs/architecture/27-paging.md](../../docs/architecture/27-paging.md)
- [znsight-man/31-서비스간-연동-개발.md](../../znsight-man/31-서비스간-연동-개발.md)
- [docs/architecture/46-service-integration-contract.md](../../docs/architecture/46-service-integration-contract.md)
- [zguide/tcf-eai-개발가이드.md](../../zguide/tcf-eai-개발가이드.md)
