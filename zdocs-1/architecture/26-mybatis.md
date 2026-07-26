# 26. MyBatis 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 26 |
| 제목 | MyBatis Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [07-DAO.md](07-DAO.md), [06-naming.md](06-naming.md), [53-naming-conventions.md](53-naming-conventions.md), [08-timeout.md](08-timeout.md), [09-transaction log.md](09-transaction log.md), [19-tcf-table.md](19-tcf-table.md), [20-env-spring.md](20-env-spring.md), [27-paging.md](27-paging.md) |
| 구현 모듈 | `tcf-web`, `*-service`, `tcf-om` |
| 대상 | Service/DAO/Mapper 개발자 |

---

## 1. 개요

NSIGHT TCF는 영속 접근에 **MyBatis 3 + mybatis-spring-boot-starter** 를 사용한다.

| 구분 | 내용 |
|------|------|
| ORM 스타일 | SQL Mapper (XML 중심, 동적 SQL) |
| 공통 자동설정 | `tcf-web` (`TcfMyBatisAutoConfiguration` 등) |
| 업무 확장 | WAR별 `@MapperScan` + `mapper/{bc}/*.xml` |
| 반환 타입 | `Map<String,Object>` / `List<Map<...>>` 중심 (DTO 점진 도입) |

DAO 계층의 **책임·경계·트랜잭션**은 [07-DAO.md](07-DAO.md)를 따르고, 본 문서는 **MyBatis 기술 구조·설정·SQL 패턴**에 집중한다.

---

## 2. 호출 구조

```text
Handler
  → Facade (@Transactional)
    → Service
      → Rule (검증, 페이징 offset 계산)
      → DAO (@Repository)
        → Mapper Interface (@Mapper)
          → Mapper XML (SQL id)
            → SqlSessionFactory → dataSource (HikariCP)
              → DB
```

핵심 원칙:

1. **Service는 SQL/Mapper를 모른다** — DAO 메서드만 호출
2. **DAO는 Mapper만 호출** — 비즈니스 판단·응답 Map 조립 금지
3. **Mapper method name == SQL id** — [06-naming.md](06-naming.md) 7장

---

## 3. 프레임워크 자동설정 (`tcf-web`)

MyBatis·DataSource 연동은 업무 WAR가 `tcf-web`을 의존하면 자동 등록된다.

등록 클래스 (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`):

상세: [31-autoconfiguration.md](31-autoconfiguration.md)

| 클래스 | 역할 |
|--------|------|
| `TcfPrimaryDataSourceAutoConfiguration` | 업무 DB `dataSource` 빈 (`@Primary`, Hikari) |
| `TcfMyBatisAutoConfiguration` | `SqlSessionFactory`, `SqlSessionTemplate` |
| `TcfTransactionLogDataSourceConfiguration` | 거래로그 전용 DataSource (별도) |
| `TcfTransactionLogConfiguration` | `TransactionLogRepository` (Jdbc, MyBatis 아님) |

### 3.1 이중 DataSource와 MyBatis

거래로그(`TCF_TX_LOG`)는 `nsight.tcf.transaction-log-datasource` 로 **별도 DataSource**를 둔다.

이때 Spring Boot 기본 `DataSourceAutoConfiguration`이 비활성화될 수 있어, `TcfPrimaryDataSourceAutoConfiguration`이 업무 DB `dataSource`를 **명시 재등록**한다.

```text
dataSource (@Primary)              → MyBatis (업무 SQL)
transactionLogDataSource          → JdbcTransactionLogRepository (거래로그 INSERT)
```

MyBatis는 **항상 `@Qualifier("dataSource")`** 에만 연결된다.

### 3.2 SqlSessionFactory 생성 조건

`TcfMyBatisAutoConfiguration`은 다음일 때 동작한다.

- classpath에 MyBatis 존재
- `dataSource` 빈 존재
- `SqlSessionFactory` 빈이 아직 없음

`application.yml`의 `mybatis.*` 설정(`mapper-locations`, `configuration`)을 읽어 Factory에 적용한다.

---

## 4. 애플리케이션 설정

### 4.1 공통 `mybatis` 블록

업무 WAR·`tcf-om` 공통 (`application.yml`):

```yaml
mybatis:
  mapper-locations:
    - classpath:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    jdbc-type-for-null: NULL
    call-setters-on-nulls: true
    default-fetch-size: 500
    cache-enabled: false
```

| 설정 | 목적 |
|------|------|
| `mapper-locations` | `classpath:/mapper/{bc}/**` XML 스캔 |
| `map-underscore-to-camel-case` | snake_case ↔ camelCase (XML alias와 병행) |
| `default-statement-timeout: 3` | DB 쿼리 3초 ([08-timeout.md](08-timeout.md) `db-query-seconds`) |
| `cache-enabled: false` | MyBatis 2nd-level cache 비활성 (일관성) |

### 4.2 DataSource (프로파일별)

| 프로파일 | 업무 DB | 거래로그 DB |
|----------|---------|-------------|
| `local` (bootRun) | H2 mem per service (`nsight_{bc}`) | H2 file `nsight_om` 또는 mem (테스트) |
| `dev` / `prod` (ztomcat) | H2 mem / 운영 DBMS | H2 file/TCP `nsight_om` 공유 |

상세: [20-env-spring.md](20-env-spring.md), [25-env-profile.md](25-env-profile.md)

---

## 5. Mapper 등록

각 WAR Application 클래스에서 **해당 업무 mapper 패키지만** 스캔한다.

```java
@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.om.mapper")
public class NsightTcfOmApplication extends NsightWarBootstrap { ... }
```

| 모듈 | `@MapperScan` 패키지 |
|------|---------------------|
| `tcf-om` | `com.nh.nsight.marketing.om.mapper` |
| `bc-service` | `com.nh.nsight.marketing.bc.mapper` |
| `sv-service` | `com.nh.nsight.marketing.sv.mapper` |
| … | `{bc}-service` 동일 패턴 |

Mapper 인터페이스에는 `@Mapper`를 붙인다 (`org.apache.ibatis.annotations.Mapper`).

---

## 6. 코드·파일 배치

### 6.1 패키지·파일 규칙

| 계층 | 패키지/경로 | 예 |
|------|-------------|-----|
| DAO | `{root}.dao` | `com.nh.nsight.marketing.om.dao.OmOperationDao` |
| Mapper IF | `{root}.mapper` | `com.nh.nsight.marketing.om.mapper.OmOperationMapper` |
| Mapper XML | `src/main/resources/mapper/{bc}/` | `mapper/om/OmOperationMapper.xml` |

`{bc}` = context path 소문자 (`om`, `sv`, `bc`, …)

### 6.2 namespace

XML `namespace`는 Mapper 인터페이스 **FQCN**과 일치해야 한다.

```xml
<mapper namespace="com.nh.nsight.marketing.om.mapper.OmOperationMapper">
```

### 6.3 DAO 스켈레톤

```java
@Repository
public class OmOperationDao {
    private final OmOperationMapper mapper;

    public OmOperationDao(OmOperationMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> searchServiceCatalog(Map<String, Object> criteria) {
        return mapper.searchServiceCatalog(criteria);
    }

    public int countServiceCatalog(Map<String, Object> criteria) {
        return mapper.countServiceCatalog(criteria);
    }
}
```

---

## 7. SQL ID·네이밍

[06-naming.md](06-naming.md) 7장과 동일하게 유지한다.

| 액션 | SQL id 접두 | 예 |
|------|-------------|-----|
| 단건 조회 | `select` | `selectServiceCatalogByKey` |
| 목록 조회 | `search` | `searchServiceCatalog` |
| 건수 | `count` | `countServiceCatalog` |
| 등록 | `insert` | `insertServiceCatalog` |
| 수정 | `update` | `updateServiceCatalog` |
| 병합 | `merge` | `mergeServiceCatalog` |
| 삭제/비활성 | `delete` / `disable` | `disableServiceCatalog` |

**강제 규칙:** `DAO method == Mapper method == SQL id`

---

## 8. SQL 작성 패턴

### 8.1 반환 타입

| 용도 | `resultType` | Java 타입 |
|------|--------------|-----------|
| 단건/행 Map | `map` | `Map<String,Object>` |
| 목록 | `map` | `List<Map<String,Object>>` |
| 건수/CUD | `int` | `int` |

컬럼 alias는 JSON 응답과 맞추기 위해 **쌍따옴표 camelCase**를 사용한다.

```xml
SELECT CATALOG_ID AS "catalogId", SERVICE_ID AS "serviceId"
  FROM OM_SERVICE_CATALOG
```

### 8.2 동적 SQL

검색 조건은 `<sql id="...">` + `<include refid="..."/>` 로 분리하고, `<if test="...">` 로 optional 조건을 처리한다.

```xml
<sql id="serviceCatalogSearchWhere">
    <if test="businessCode != null and businessCode != ''">
        AND BUSINESS_CODE = #{businessCode}
    </if>
    <if test="useYn != null and useYn != ''">
        AND USE_YN = #{useYn}
    </if>
</sql>

<select id="searchServiceCatalog" parameterType="map" resultType="map">
    SELECT ...
      FROM OM_SERVICE_CATALOG
     WHERE 1 = 1
    <include refid="serviceCatalogSearchWhere"/>
     ORDER BY BUSINESS_CODE, SERVICE_ID
</select>
```

### 8.3 페이징

목록 조회는 **`search*` + `count*`** 쌍을 제공한다. 페이징 계약·UI 연동: [27-paging.md](27-paging.md)

Service/Rule에서 `pageNo`, `pageSize` → `offset` 계산 후 criteria에 넣는다.

```xml
<if test="pageSize != null">
    OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
</if>
```

H2 `MODE=Oracle` 기준 구문. Oracle/PostgreSQL 운영 전환 시 dialect 점검 필요.

### 8.4 파라미터

- 다건 조건: `parameterType="map"`, `#{key}` 바인딩
- 단일 키: `parameterType="string"` 등 명시 가능

---

## 9. Service ↔ DAO 연동 예

`OmServiceCatalogService.inquiry()` 흐름:

```text
1. Rule: validateOperation, normalizePaging (pageNo/pageSize → offset)
2. DAO: searchServiceCatalog(criteria), countServiceCatalog(criteria)
3. Service: rows + totalCount + pageNo/pageSize → 응답 Map
```

Service는 MyBatis/SQL을 알지 못하고 DAO 메서드명만 사용한다.

---

## 10. 트랜잭션·예외

| 항목 | 규칙 |
|------|------|
| 트랜잭션 시작 | Facade `@Transactional` ([03-transaction.md](03-transaction.md)) |
| DAO | 트랜잭션 경계 없음, Mapper 호출만 |
| DB 예외 | `DataAccessException` → Service/ETF에서 표준 오류 변환 ([05-exception.md](05-exception.md)) |
| BusinessException | Service/Rule에서 발생 (DAO에서 남발 금지) |
| CUD 결과 | DAO `int` 반환 → Service가 0건 여부 판단 |

---

## 11. 테스트

### 11.1 통합 테스트 (`@SpringBootTest`)

`sv-service` 예: `src/test/resources/application.yml`에 **업무 DataSource** 와 **거래로그 DataSource** 를 모두 정의해야 MyBatis 컨텍스트가 기동된다.

필수 항목:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:nsight_sv;...
  session:
    store-type: none   # Spring Session JDBC 비활성 (테스트)

nsight:
  tcf:
    transaction-log-enabled: true
    transaction-log-datasource:
      url: jdbc:h2:mem:nsight_sv_txlog;...
```

`local` 프로파일 yml만 있고 test yml에 `spring.datasource.url`이 없으면 `dataSource` 빈 누락으로 실패할 수 있다.

### 11.2 Mapper 단위 테스트

현재 프레임워크 표준은 **Service/DAO 통합 테스트** 위주. Mapper XML 단독 테스트는 선택 사항.

---

## 12. 모듈별 구현 현황

| 모듈 | MyBatis | 비고 |
|------|---------|------|
| `tcf-om` | **풀 구현** | `OmOperationMapper.xml` — OM 운영·카탈로그·세션·거래로그 조회 |
| 16개 `*-service` | 스켈레톤 | Mapper IF + XML 존재, 일부 DAO는 stub (Mapper 미연결) |
| `tcf-batch` | JDBC Template | MyBatis 미사용 (`OmDashboardStatusRepository`) |
| `tcf-ui` | 없음 | 정적 UI + HTTP Relay |

신규 업무 개발 시: XML/SQL 작성 → Mapper IF 추가 → **DAO에서 Mapper 위임** → Service 연결 순으로 완성한다.

---

## 13. 신규 Mapper 추가 체크리스트

1. `src/main/resources/mapper/{bc}/{Bc}{Domain}Mapper.xml` 생성, namespace = Mapper FQCN
2. `{Bc}{Domain}Mapper.java` 인터페이스 + `@Mapper`
3. `{Bc}{Domain}Dao.java` — Mapper 생성자 주입, 메서드 위임
4. Application `@MapperScan("...mapper")` 패키지 확인
5. SQL id = Mapper method = DAO method
6. 목록 API면 `search*` + `count*` + 페이징 offset
7. SELECT alias `"camelCase"` — UI/JSON 필드명과 일치
8. `application-{profile}.yml`에 `spring.datasource.url` 정의
9. 통합 테스트 yml에 test용 DataSource 포함

---

## 14. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-web/.../TcfPrimaryDataSourceAutoConfiguration.java` | 업무 DataSource |
| `tcf-web/.../TcfMyBatisAutoConfiguration.java` | SqlSessionFactory |
| `tcf-web/.../TcfTransactionLogDataSourceConfiguration.java` | 거래로그 DataSource |
| `tcf-om/.../dao/OmOperationDao.java` | DAO 대표 구현 |
| `tcf-om/.../mapper/OmOperationMapper.java` | Mapper 인터페이스 |
| `tcf-om/.../mapper/om/OmOperationMapper.xml` | SQL·동적·페이징 예 |
| `bc-service/.../mapper/bc/BcSampleMapper.xml` | 업무 WAR XML 스켈레톤 |
| `sv-service/src/test/resources/application.yml` | 테스트 DataSource 예 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — MyBatis 3계층·tcf-web 자동설정·SQL 패턴·테스트 가이드 |
