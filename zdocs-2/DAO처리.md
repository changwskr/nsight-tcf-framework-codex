# TCF DAO 처리 정리

NSIGHT TCF에서 **DAO(Data Access Object)** 는 Service 아래·Mapper 위에 위치하며, **DB 접근을 캡슐화**하는 계층입니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **영속 캡슐화** | SQL 상세를 Service에서 분리 | `@Repository` + Mapper 위임 |
| **호출 위치** | Service → DAO → Mapper → XML | Handler/Rule에서 DAO 직접 호출 금지 |
| **트랜잭션** | Facade TX 안에서 실행 | DAO는 TX 경계 없음 |
| **반환** | 데이터 사실만 전달 | `Map`, `List<Map>`, `int` |
| **페이징** | search + count 쌍 | Rule이 offset 계산, DAO는 slice |
| **Timeout** | SQL 1건 제한 | `PolicyDrivenQueryTimeoutInterceptor` |

---

## 1. 전체 호출 구조

```text
Handler
  → Facade (@Transactional)
    → Service
      → Rule (검증, 페이징 normalize)
      → DAO (@Repository)
        → Mapper Interface (@Mapper)
          → Mapper XML (SQL id)
            → SqlSessionFactory → HikariCP → DB
```

DAO는 **Facade 트랜잭션 컨텍스트 안**에서 실행됩니다. DAO 자체는 `@Transactional`을 선언하지 않습니다.

---

## 2. 계층 경계

### 2.1 허용 호출

| From | To |
|------|-----|
| Service | DAO |
| DAO | Mapper |
| DAO | JdbcTemplate (예외적) |

### 2.2 금지 호출

| 금지 | 이유 |
|------|------|
| Handler → DAO | 계층 우회, 검증·TX 경계 붕괴 |
| Rule → DAO | 검증과 조회/저장 혼합 |
| DAO → Service | 역방향 의존, 순환 참조 |
| DAO에서 응답 Map 조립 | Service 책임 |
| DAO에서 BusinessException 남발 | 도메인 판단은 Service/Rule |

상위 계층: [어플리케이션계층.md](어플리케이션계층.md)

---

## 3. DAO 책임

### 3.1 해야 하는 일

- Service 조건을 DB 파라미터 `Map`으로 정리
- Mapper 메서드 호출 (`select*`, `search*`, `count*`, `insert*`, `update*`, `merge*`, `delete*`)
- 결과를 **그대로** 또는 **최소 변환** 후 반환 (예: Clob → String)
- CUD **영향 행 수(`int`)** 반환 → Service가 0건 여부 판단

### 3.2 하면 안 되는 일

- 화면/채널별 응답 Map 조립
- 업무 정책(권한, 상태전이) 결정
- `@Transactional` 선언
- 사용자 친화적 예외 메시지 가공

---

## 4. 패키지·클래스 표준

```text
com.nh.nsight.marketing.{bc}
└── persistence/
    ├── dao/       {Business}{Domain}Dao      @Repository
    └── mapper/    {Business}{Domain}Mapper   @Mapper

src/main/resources/mapper/{bc}/
    └── {Business}{Domain}Mapper.xml
```

| 항목 | 규칙 | 예 |
|------|------|-----|
| DAO 클래스 | `{Business}{Domain}Dao` | `OmOperationDao`, `EbUserDao` |
| Mapper 인터페이스 | `{Business}{Domain}Mapper` | `OmOperationMapper` |
| Mapper XML | 동일 이름 | `mapper/om/OmOperationMapper.xml` |
| namespace | Mapper FQCN | `com.nh.nsight.marketing.om.persistence.mapper.OmOperationMapper` |

**WAR 기동**

```java
@MapperScan("com.nh.nsight.marketing.{bc}.persistence.mapper")
```

---

## 5. 구현 패턴

### 5.1 표준 — Mapper 위임 (권장)

```java
@Repository
public class EbUserDao {
    private final EbUserMapper mapper;

    public EbUserDao(EbUserMapper mapper) {
        this.mapper = mapper;
    }

    public int insertUser(Map<String, Object> row) {
        return mapper.insertUser(row);
    }

    public List<Map<String, Object>> searchUsers(Map<String, Object> criteria) {
        return mapper.searchUsers(criteria);
    }

    public int countUsers(Map<String, Object> criteria) {
        return mapper.countUsers(criteria);
    }
}
```

### 5.2 파라미터 조립 — OmOperationDao

DAO가 **여러 인자 → Map** 으로 묶어 Mapper에 전달하는 패턴도 허용됩니다.

```18:29:tcf-om/src/main/java/com/nh/nsight/marketing/om/persistence/dao/OmOperationDao.java
    public Map<String, Object> selectTxSummary(String baseDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("baseDate", baseDate);
        return mapper.selectTxSummary(params);
    }

    public List<Map<String, Object>> selectErrorTop(String fromTime, String toTime, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("fromTime", fromTime);
        params.put("toTime", toTime);
        params.put("limit", limit);
        return mapper.selectErrorTop(params);
    }
```

### 5.3 샘플 — Mapper 미연동 Stub

`sv-service`의 `SvSampleDao`는 학습용 **인메모리 Stub**입니다. 실제 업무는 Mapper 위임 패턴을 따릅니다.

---

## 6. Mapper · SQL ID

### 6.1 3자 일치 (강제)

```text
DAO method  == Mapper method == SQL id

예) searchErrorCodes(criteria)
    → OmOperationMapper.searchErrorCodes(params)
    → <select id="searchErrorCodes">
```

### 6.2 SQL ID 접두어

| 접두어 | 용도 | 예 |
|--------|------|-----|
| `select` | 단건 조회 | `selectErrorCodeByCode` |
| `search` | 목록 조회 | `searchErrorCodes` |
| `count` | 건수 | `countErrorCodes` |
| `insert` | 등록 | `insertUser` |
| `update` | 수정 | `updateErrorCode` |
| `merge` | MERGE/UPSERT | `mergeApStatus` |
| `delete` / `disable` | 삭제·비활성 | `disableErrorCode` |

상세: [네이밍.md](네이밍.md)

### 6.3 Mapper XML 예

```xml
<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvSampleMapper">
    <select id="selectSample" parameterType="map" resultType="map">
        SELECT #{sampleKey} AS SAMPLE_KEY, 'SV Sample' AS SAMPLE_NAME
    </select>
</mapper>
```

- `parameterType="map"` — 조건 Map
- `resultType="map"` — `Map<String, Object>` 반환 (컬럼 alias → camelCase 변환 설정 따름)

---

## 7. 반환 타입 가이드

| 시나리오 | DAO 반환 | Service 판단 |
|----------|----------|--------------|
| 단건 조회 | `Map<String,Object>` (null 가능) | null → `BusinessException` |
| 목록 조회 | `List<Map<...>>` | 빈 리스트 허용 |
| 건수 | `int` | 페이징 totalCount |
| CUD | `int` (영향 행 수) | 0건 → 업무 오류 가능 |

**원칙:** null/0건에 대한 **비즈니스 판단은 Service**에서, DAO는 **데이터 사실만** 전달합니다.

```java
// Service (OM 예)
Map<String, Object> row = dao.selectErrorCodeByCode(errorCode);
if (row == null) {
    throw new BusinessException("E-OM-BIZ-0002", "오류코드를 찾을 수 없습니다.");
}
```

---

## 8. 페이징 처리

목록 조회(`*.inquiry`)는 **offset 기반 페이징** + **search/count 쌍**을 사용합니다.

### 8.1 흐름

```text
Service
  1. criteria Map 구성 (검색조건 + pageNo + pageSize)
  2. Rule.normalizePaging(criteria)  → offset 확정
  3. DAO.searchXxx(criteria)         → rows
  4. DAO.countXxx(criteria)         → totalCount (동일 WHERE)
  5. Service가 응답 Map 조립
```

### 8.2 Rule — offset 계산

```17:25:tcf-om/src/main/java/com/nh/nsight/marketing/om/application/rule/OmOperationRule.java
    public void normalizePaging(Map<String, Object> body) {
        int pageNo = Math.max(1, toInt(body.get("pageNo"), 1));
        int pageSize = toInt(body.get("pageSize"), 20);
        if (pageSize > 100) {
            pageSize = 100;
        }
        body.put("pageNo", pageNo);
        body.put("pageSize", pageSize);
        body.put("offset", (pageNo - 1) * pageSize);
    }
```

| 항목 | 기본값 | 상한 |
|------|--------|------|
| `pageNo` | 1 | 1-based |
| `pageSize` | 20 | **최대 100** |
| `offset` | `(pageNo-1)*pageSize` | Rule에서 계산 |

### 8.3 DAO · SQL

```java
public List<Map<String, Object>> searchMessageStructs(Map<String, Object> criteria) {
    return mapper.searchMessageStructs(criteria);
}
public int countMessageStructs(Map<String, Object> criteria) {
    return mapper.countMessageStructs(criteria);
}
```

```xml
<!-- OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY -->
```

**응답 body 관례:** `pageNo`, `pageSize`, `totalCount`, `rows`

---

## 9. 트랜잭션 · Timeout

### 9.1 트랜잭션

```text
Facade @Transactional
   ├─ Service A → DAO A → Mapper A
   └─ Service B → DAO B → Mapper B
```

- 다중 DAO 호출은 **Facade 단일 TX**로 묶음
- DAO 단위 개별 TX 분리 **지양**

### 9.2 Query Timeout

`PolicyDrivenQueryTimeoutInterceptor`가 `TimeoutContextHolder`의 `dbQueryTimeoutSec`을 Mapper 실행 시 Statement timeout으로 적용합니다.

- 정책: `TCF_SERVICE_TIMEOUT_POLICY` — [타임아웃관리.md](타임아웃관리.md)
- SQL timeout → `E-TCF-TIME-003` — [예외처리.md](예외처리.md)

---

## 10. 예외 처리 (DAO 관점)

| 원칙 | 내용 |
|------|------|
| DB 예외 전파 | `DataAccessException` 등은 DAO에서 과도하게 wrapping 하지 않음 |
| 표준 변환 | TCF `ETF.systemError()` → `E-COM-SYS-0001` |
| BusinessException | **Service/Rule**에서 발생 (DAO X) |
| 영향 0건 | Service에서 `int == 0` 판단 후 throw |

---

## 11. tcf-om 대표 DAO

`OmOperationDao` — OM Admin 다수 화면이 **하나의 DAO + `OmOperationMapper.xml`** 을 공유합니다.

| 영역 | DAO 메서드 예 |
|------|---------------|
| 대시보드 | `selectApStatus`, `selectTxSummary` |
| 오류코드 | `searchErrorCodes`, `updateErrorCode` |
| Service Catalog | `searchServiceCatalog`, `mergeServiceCatalog` |
| 전문구조 | `searchMessageStructs`, `countMessageStructs` |
| Timeout 정책 | `searchTimeoutPolicies`, `mergeTimeoutPolicy` |
| 배치·배포 | `selectDeployStatus`, `deleteAllApStatus` |

대형 XML(`OmOperationMapper.xml`) — 도메인별 SQL id로 구분, DAO 메서드 1:1 매핑.

---

## 12. 좋은 예 / 나쁜 예

### 12.1 좋은 예

```text
OmErrorCodeService.update(...)
  └─ OmOperationDao.updateErrorCode(row)
      └─ OmOperationMapper.updateErrorCode
          └─ SQL id: updateErrorCode
```

### 12.2 나쁜 예

```text
CommonService.process()
  └─ OmDao.exec01(...)
      ├─ 권한 검증 + 응답 Map 조립
      └─ Mapper: runSql
```

---

## 13. 신규 DAO 추가 절차

1. **Mapper 인터페이스** — `persistence/mapper/{Domain}Mapper.java`
2. **Mapper XML** — `resources/mapper/{bc}/{Domain}Mapper.xml`, namespace FQCN
3. **DAO** — `@Repository`, Mapper 주입, method == SQL id
4. **Service** — DAO 호출, null/0건 BusinessException
5. **목록** — `search*` + `count*` + Rule `normalizePaging`
6. **@MapperScan** — Application 클래스에 패키지 등록
7. **검증** — 단건·목록·CUD·페이징·실패 케이스

---

## 14. 개발자 체크리스트

1. Service가 **SQL/Mapper를 직접 import하지 않**는가?
2. DAO method == Mapper method == **SQL id** 인가?
3. Handler/Rule에서 **DAO를 호출하지 않**는가?
4. **응답 Map 조립**은 Service에서 하는가?
5. 목록 inquiry는 **search + count** 쌍이 있는가?
6. 페이징 **offset**은 Rule에서 계산하는가?
7. **트랜잭션**은 Facade에만 있는가?
8. XML 경로가 `mapper/{bc}/` 규칙을 따르는가?

---

## 15. 구현 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `eb-service/.../persistence/dao/EbUserDao.java` |
| 2 | `eb-service/.../persistence/mapper/EbUserMapper.java` |
| 3 | `eb-service/.../mapper/eb/EbUserMapper.xml` |
| 4 | `tcf-om/.../persistence/dao/OmOperationDao.java` |
| 5 | `tcf-om/.../mapper/om/OmOperationMapper.xml` |
| 6 | `tcf-om/.../rule/OmOperationRule.java` — normalizePaging |
| 7 | `tcf-web/.../PolicyDrivenQueryTimeoutInterceptor.java` |

---

## 관련 문서

- [docs/architecture/07-DAO.md](../docs/architecture/07-DAO.md) — DAO 아키텍처 (상세)
- [docs/architecture/26-mybatis.md](../docs/architecture/26-mybatis.md) — MyBatis 설정·SQL 패턴
- [docs/architecture/27-paging.md](../docs/architecture/27-paging.md) — 페이징 아키텍처
- [zdoc/어플리케이션계층.md](어플리케이션계층.md) — Service·Rule·DAO 위치
- [zdoc/네이밍.md](네이밍.md) — DAO/Mapper/SQL id 네이밍
- [zdoc/예외처리.md](예외처리.md) — BusinessException·systemError
- [zdoc/타임아웃관리.md](타임아웃관리.md) — DB Query timeout
