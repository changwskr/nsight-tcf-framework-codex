# sv-service 페이징 프로그램 가이드

**페이징 프로그램은 결국 “화면이 pageNo/pageSize를 내면, Rule이 offset을 계산하고, MyBatis가 필요한 만큼만 DB에서 잘라오는 구조”**입니다.

[sv-service-페이징.md](sv-service-페이징.md) 기준으로도 TCF Core·Dispatcher는 페이징에 관여하지 않고, `Rule → Service → DAO → MyBatis`에서 처리하도록 되어 있습니다.  
`pageNo` 기본값은 **1**, `pageSize` 기본값은 **100**, 일반 목록 최대값은 **500**, `offset`은 서버에서 `(pageNo - 1) × pageSize`로 계산합니다.

| 문서 | 대상 |
|------|------|
| **본 문서** | 업무 개발자용 쉬운 설명·프로그램 예시 |
| [sv-service-페이징.md](sv-service-페이징.md) | 설계·계약·테스트·소스 맵 |
| [업무페이징.md](업무페이징.md) | 업무 WAR 공통 표준 |

---

## 1. 가장 쉬운 전체 흐름

```text
화면
  pageNo = 2
  pageSize = 10
  sampleKey = "A00"
        ↓
Handler
  그냥 Facade로 넘김
        ↓
Facade
  조회용 트랜잭션 시작
        ↓
Service
  Rule에게 검색조건 정리 요청
        ↓
Rule
  pageNo/pageSize 확인
  offset 계산
  offset = (2 - 1) × 10 = 10
        ↓
DAO
  Mapper 호출
        ↓
MyBatis XML
  10건 건너뛰고 다음 10건 조회
        ↓
DB
  필요한 10건만 반환
        ↓
Service
  list + totalCount + totalPage 조립
        ↓
화면 응답
```

즉, **2페이지 10건 조회**는 DB 입장에서 이렇게 이해하면 됩니다.

```text
처음 10건은 건너뛴다.
그 다음 10건만 가져온다.
```

---

## 2. 프로그램 파일 구조

업무팀은 아래 파일만 만들면 됩니다.

```text
sv-service/
├── entry/
│   ├── handler/
│   │   └── SvSampleHandler.java
│   └── facade/
│       └── SvSampleFacade.java
│
├── application/
│   ├── rule/
│   │   └── SvSampleRule.java
│   └── service/
│       └── SvSampleService.java
│
├── persistence/
│   ├── dao/
│   │   └── SvSampleDao.java
│   └── mapper/
│       └── SvSampleMapper.java
│
└── resources/
    └── mapper/sv/
        └── SvSampleMapper.xml
```

테스트 화면(선택):

```text
tcf-ui/
├── static/sv/sample-list.html
└── static/_shared/sv-admin.js
```

[sv-service-페이징.md](sv-service-페이징.md) §9 소스 맵과 동일한 구조입니다.

---

## 3. 화면 요청 예시

화면에서는 이렇게 보냅니다.

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Sample.inquiry",
    "transactionCode": "SV-INQ-0001"
  },
  "body": {
    "pageNo": 2,
    "pageSize": 10,
    "sampleKey": "A00"
  }
}
```

여기서 중요한 것은 **화면은 offset을 보내지 않는다**는 것입니다.

```text
화면이 보내는 값:
- pageNo
- pageSize
- 검색조건

화면이 내면 안 되는 값:
- offset
```

`offset`은 서버 Rule에서 계산합니다.

---

## 4. Handler 프로그램

Handler는 페이징을 모릅니다.  
그냥 요청 body를 Facade로 넘깁니다.

```java
@Component
public class SvSampleHandler implements TransactionHandler {

    private static final String INQUIRY = "SV.Sample.inquiry";

    private final SvSampleFacade facade;

    public SvSampleHandler(SvSampleFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request,
                           TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND, serviceId);
        };
    }
}
```

### 쉽게 말하면

```text
Handler 역할:
"SV.Sample.inquiry 거래가 들어왔네?
 그러면 SvSampleFacade.inquiry()를 호출하자."
```

Handler에는 이런 로직을 넣지 않습니다.

```text
금지:
- pageNo 계산
- offset 계산
- SQL 호출
- count 계산
```

---

## 5. Facade 프로그램

Facade는 트랜잭션 경계만 잡습니다.

```java
@Service
public class SvSampleFacade {

    private final SvSampleService service;

    public SvSampleFacade(SvSampleService service) {
        this.service = service;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body,
                                       TransactionContext context) {
        return service.inquiry(body, context);
    }
}
```

### 쉽게 말하면

```text
Facade 역할:
"이 거래는 조회 거래다.
 DB를 변경하지 않는다.
 5초 안에 끝나야 한다."
```

---

## 6. Rule 프로그램

Rule이 페이징에서 제일 중요합니다.  
`pageNo`, `pageSize`를 정리하고 `offset`을 계산합니다.

```java
@Component
public class SvSampleRule {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 500;

    public Map<String, Object> buildSearchCriteria(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 없습니다.");
        }

        int pageNo = toInt(body.get("pageNo"), DEFAULT_PAGE_NO);
        int pageSize = toInt(body.get("pageSize"), DEFAULT_PAGE_SIZE);

        if (pageNo < 1) {
            pageNo = DEFAULT_PAGE_NO;
        }

        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        int offset = (pageNo - 1) * pageSize;

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("pageNo", pageNo);
        criteria.put("pageSize", pageSize);
        criteria.put("offset", offset);

        Object sampleKey = body.get("sampleKey");
        if (sampleKey != null && !sampleKey.toString().isBlank()) {
            criteria.put("sampleKey", sampleKey.toString().trim());
        }

        return criteria;
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
```

> 실제 소스는 `validateInquiry`와 `normalizePaging`을 분리해 두었습니다. 동작은 위와 같습니다.

### 쉽게 말하면

Rule은 이런 일을 합니다.

```text
pageNo가 없으면 1로 한다.
pageSize가 없으면 100으로 한다.
pageSize가 500보다 크면 500으로 줄인다.
offset을 계산한다.
검색조건 sampleKey를 정리한다.
```

예를 들면 다음과 같습니다.

| pageNo | pageSize | 계산식 | offset |
|-------:|---------:|--------|-------:|
| 1 | 10 | `(1 - 1) × 10` | 0 |
| 2 | 10 | `(2 - 1) × 10` | 10 |
| 3 | 10 | `(3 - 1) × 10` | 20 |
| 2 | 100 | `(2 - 1) × 100` | 100 |

---

## 7. Service 프로그램

Service는 Rule이 만든 검색조건으로 DAO를 **두 번** 호출합니다.

```java
@Service
public class SvSampleService {

    private final SvSampleRule rule;
    private final SvSampleDao dao;

    public SvSampleService(SvSampleRule rule, SvSampleDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public Map<String, Object> inquiry(Map<String, Object> body,
                                       TransactionContext context) {

        rule.validateInquiry(body);
        Map<String, Object> criteria = rule.buildSearchCriteria(body);

        List<Map<String, Object>> list = dao.searchSamples(criteria);
        int totalCount = dao.countSamples(criteria);

        int pageNo = (int) criteria.get("pageNo");
        int pageSize = (int) criteria.get("pageSize");

        int totalPage = totalCount == 0
                ? 0
                : (totalCount + pageSize - 1) / pageSize;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "SV");
        result.put("serviceId", context.getHeader().getServiceId());
        result.put("guid", context.getHeader().getGuid());

        result.put("list", list);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("totalCount", totalCount);
        result.put("totalPage", totalPage);

        return result;
    }
}
```

### 쉽게 말하면

Service는 이렇게 일합니다.

```text
1. Rule에게 검색조건을 정리하라고 시킨다.
2. DAO에게 현재 페이지 목록을 조회하라고 한다.
3. DAO에게 전체 건수를 조회하라고 한다.
4. totalPage를 계산한다.
5. 화면에 줄 응답을 만든다.
```

**`list.size()`를 `totalCount`로 대체하지 않습니다.**  
전체 건수는 반드시 `count*` 쿼리 결과를 씁니다.

---

## 8. DAO 프로그램

DAO는 Mapper만 호출합니다.

```java
@Repository
public class SvSampleDao {

    private final SvSampleMapper mapper;

    public SvSampleDao(SvSampleMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> searchSamples(Map<String, Object> criteria) {
        return mapper.searchSamples(criteria);
    }

    public int countSamples(Map<String, Object> criteria) {
        return mapper.countSamples(criteria);
    }
}
```

### 쉽게 말하면

```text
DAO 역할:
"Service가 달라고 한 데이터를 Mapper에게 요청한다."
```

DAO에서 하면 안 되는 일:

```text
금지:
- 업무 판단
- 화면 응답 생성
- totalPage 계산
- SQL 문자열 직접 작성
```

NSIGHT 계층 기준에서도 DAO는 DB 접근을 캡슐화하고 Mapper를 호출하는 역할이며, 업무 판단은 Rule/Service에서 처리합니다. ([DAO처리.md](DAO처리.md))

---

## 9. Mapper Interface 프로그램

```java
@Mapper
public interface SvSampleMapper {

    List<Map<String, Object>> searchSamples(Map<String, Object> criteria);

    int countSamples(Map<String, Object> criteria);
}
```

### 쉽게 말하면

```text
searchSamples = 현재 페이지 목록 조회
countSamples  = 전체 건수 조회
```

---

## 10. MyBatis XML 프로그램

### 10.1 WHERE 조건은 공통으로 뺀다

```xml
<sql id="sampleSearchWhere">
    <where>
        <if test="sampleKey != null and sampleKey != ''">
            AND SAMPLE_KEY LIKE CONCAT('%', #{sampleKey}, '%')
        </if>
    </where>
</sql>
```

`searchSamples`와 `countSamples`가 **같은 조건**을 써야 목록 건수와 `totalCount`가 일치합니다.

### 10.2 목록 조회 SQL

```xml
<select id="searchSamples" parameterType="map" resultType="map">
    SELECT
          SAMPLE_KEY    AS sampleKey
        , SAMPLE_NAME   AS sampleName
        , CREATED_AT    AS createdAt
      FROM SV_SAMPLE
    <include refid="sampleSearchWhere"/>
     ORDER BY SAMPLE_KEY
     OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
</select>
```

### 10.3 전체 건수 조회 SQL

```xml
<select id="countSamples" parameterType="map" resultType="int">
    SELECT COUNT(1)
      FROM SV_SAMPLE
    <include refid="sampleSearchWhere"/>
</select>
```

### 쉽게 말하면

```text
searchSamples:
현재 페이지에 보여줄 데이터만 가져온다.

countSamples:
전체 검색 결과가 몇 건인지 센다.
```

목록 SQL은 반드시 **`ORDER BY`를 고정**합니다. 정렬이 없으면 페이지마다 행 순서가 바뀔 수 있습니다.

---

## 11. 실행 예시

### 11.1 화면 요청

```json
{
  "body": {
    "pageNo": 2,
    "pageSize": 10,
    "sampleKey": "A00"
  }
}
```

### 11.2 Rule 계산 결과

```text
pageNo = 2
pageSize = 10
offset = (2 - 1) * 10 = 10
```

### 11.3 MyBatis에 전달되는 criteria

```json
{
  "pageNo": 2,
  "pageSize": 10,
  "offset": 10,
  "sampleKey": "A00"
}
```

### 11.4 실제 SQL 의미

```sql
SELECT SAMPLE_KEY, SAMPLE_NAME, CREATED_AT
  FROM SV_SAMPLE
 WHERE SAMPLE_KEY LIKE '%A00%'
 ORDER BY SAMPLE_KEY
 OFFSET 10 ROWS FETCH NEXT 10 ROWS ONLY
```

### 11.5 의미

```text
A00이 포함된 데이터 중
SAMPLE_KEY 순서로 정렬한 뒤
처음 10건은 건너뛰고
그 다음 10건을 가져온다.
```

---

## 12. 응답 예시

```json
{
  "businessCode": "SV",
  "serviceId": "SV.Sample.inquiry",
  "guid": "202607011234560001",
  "list": [
    {
      "sampleKey": "A011",
      "sampleName": "SV 샘플 11",
      "createdAt": "2026-06-27T12:00:00"
    },
    {
      "sampleKey": "A012",
      "sampleName": "SV 샘플 12",
      "createdAt": "2026-06-27T12:01:00"
    }
  ],
  "pageNo": 2,
  "pageSize": 10,
  "totalCount": 35,
  "totalPage": 4
}
```

### 응답 의미

| 값 | 의미 |
|----|------|
| `list` | 현재 페이지 데이터 |
| `pageNo` | 현재 2페이지 |
| `pageSize` | 한 페이지 10건 |
| `totalCount` | 전체 검색 결과 35건 |
| `totalPage` | 전체 4페이지 |

`totalPage` 계산:

```text
totalCount = 35
pageSize = 10

35 / 10 = 3.5 → 올림 = 4페이지
```

```java
int totalPage = totalCount == 0
        ? 0
        : (totalCount + pageSize - 1) / pageSize;
```

---

## 13. 업무팀 개발자가 기억할 5가지

| 번호 | 규칙 | 이유 |
|-----:|------|------|
| 1 | `pageNo`, `pageSize`만 화면에서 받는다 | 단순하게 하기 위해 |
| 2 | `offset`은 서버 Rule에서 계산한다 | 조작 방지 |
| 3 | `pageSize`는 최대 500으로 제한한다 | DB/WAS 보호 |
| 4 | `search`와 `count`는 같은 WHERE를 쓴다 | 목록과 총 건수 일치 |
| 5 | 목록 SQL에는 반드시 `ORDER BY`를 둔다 | 페이지 결과 흔들림 방지 |

---

## 14. 전체 프로그램을 한 문장으로 설명

```text
화면이 pageNo/pageSize를 내면,
Rule이 pageNo/pageSize를 검증하고 offset을 계산한다.
Service는 DAO에게 목록과 전체 건수를 조회하게 한다.
DAO는 Mapper를 호출한다.
MyBatis XML은 OFFSET/FETCH로 DB에서 필요한 데이터만 가져온다.
Service는 list, totalCount, totalPage를 만들어 화면에 돌려준다.
```

---

## 15. 최종 결론

페이징은 TCF가 관리할 필요 없습니다.

```text
TCF
= 이 거래가 정상 요청인지 확인하고 Handler를 찾아주는 역할

업무 프로그램
= pageNo / pageSize / offset / list / totalCount / totalPage 처리

MyBatis
= DB에서 필요한 만큼만 잘라서 가져오는 역할
```

따라서 업무팀은 **Rule, Service, DAO, Mapper XML**만 정확히 만들면 됩니다.

---

## 16. 테스트 화면

| 환경 | URL |
|------|-----|
| tcf-ui bootRun | http://localhost:8099/sv/sample-list.html |
| Tomcat `/ui` | http://localhost:8080/ui/sv/sample-list.html |

사전 조건: `sv-service` (8086) + `tcf-ui` (8099) 기동.
