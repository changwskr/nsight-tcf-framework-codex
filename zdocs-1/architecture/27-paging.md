# 27. 페이징 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 27 |
| 제목 | Paging Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [26-mybatis.md](26-mybatis.md), [07-DAO.md](07-DAO.md), [02-junmun.md](02-junmun.md), [04-messaging.md](04-messaging.md), [14-online-arc.md](14-online-arc.md) |
| 구현 모듈 | `tcf-om`, `tcf-ui` (OM Admin), 업무 `*-service` (동일 패턴 적용) |
| 대상 | Service/DAO/Mapper/UI 개발자 |

---

## 1. 개요

TCF 목록 조회(`*.inquiry`)는 **offset 기반 페이지 페이징**을 사용한다.

| 항목 | 규칙 |
|------|------|
| 요청 | `pageNo`(1부터), `pageSize` |
| 내부 계산 | `offset = (pageNo - 1) * pageSize` |
| 응답 | `pageNo`, `pageSize`, `totalCount`, `rows` |
| DB | `search*` + `count*` 쌍, SQL `OFFSET … FETCH NEXT …` |
| 기본값 | `pageNo=1`, `pageSize=20`, **최대 `pageSize=100`** |

페이징 **정규화(기본값·상한·offset 계산)** 는 Rule, **DB slice** 는 MyBatis, **UI 컨트롤** 은 `tcf-ui` OM Admin 공통 함수가 담당한다.

---

## 2. End-to-End 흐름

```text
[Browser / tcf-ui]
  body: { pageNo, pageSize, ...검색조건 }
       │
       ▼ POST /om/online  (serviceId: OM.Xxx.inquiry)
[Handler → Facade → Service]
  1. criteria 구성 (검색 필드 + pageNo/pageSize)
  2. Rule.normalizePaging(criteria)  → pageNo, pageSize, offset 확정
  3. DAO.searchXxx(criteria)           → 현재 페이지 rows
  4. DAO.countXxx(criteria)            → totalCount (동일 WHERE)
  5. 응답 Map 조립
       │
       ▼
[ETF 응답 body]
  { pageNo, pageSize, totalCount, rows, ... }
       │
       ▼
[tcf-ui OmAdmin.renderPagination]
  PREV / 페이지 번호 / NEXT
```

---

## 3. 요청·응답 계약

### 3.1 요청 body (INQUIRY)

```json
{
  "pageNo": 1,
  "pageSize": 10,
  "businessCode": "OM",
  "serviceId": "SV"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `pageNo` | number | N | 1-based 페이지 번호 (미입력 시 1) |
| `pageSize` | number | N | 페이지당 건수 (미입력 시 20, 최대 100) |
| 기타 | varies | N | 화면별 검색 조건 |

### 3.2 응답 body

```json
{
  "businessCode": "OM",
  "screen": "ServiceId / 거래코드 관리",
  "pageNo": 1,
  "pageSize": 10,
  "totalCount": 72,
  "rows": [ { "...": "..." } ]
}
```

| 필드 | 설명 |
|------|------|
| `pageNo` | 적용된 페이지 (정규화 후 값) |
| `pageSize` | 적용된 페이지 크기 |
| `totalCount` | **검색 조건 전체** 건수 (현재 페이지 rows 길이와 다름) |
| `rows` | **현재 페이지** 데이터만 |

---

## 4. Rule — `normalizePaging`

OM 표준: `OmOperationRule.normalizePaging(Map)`

```java
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

| 입력 | 처리 |
|------|------|
| `pageNo` null/0/음수 | `1` |
| `pageNo` 잘못된 문자열 | 기본값 |
| `pageSize` null | `20` |
| `pageSize` > 100 | `100`으로 cap |
| `offset` | **Rule이 계산해 criteria에 주입** — 클라이언트가 보내지 않음 |

업무 WAR에도 동일 Rule 패턴을 두거나, `tcf-core`/`tcf-web` 공통 유틸로 추출할 수 있다. 현재 **구현체는 `tcf-om`에 집중**되어 있다.

---

## 5. Service 패턴

### 5.1 DB 페이징 (표준)

```java
public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
    rule.validateOperation(context);
    Map<String, Object> criteria = new HashMap<>(OmBodySupport.searchCriteria(body));
    rule.normalizePaging(criteria);
    copyIfPresent(body, criteria, "businessCode", "serviceId", "useYn");

    List<Map<String, Object>> rows = dao.searchServiceCatalog(criteria);
    int totalCount = dao.countServiceCatalog(criteria);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("pageNo", criteria.get("pageNo"));
    result.put("pageSize", criteria.get("pageSize"));
    result.put("totalCount", totalCount);
    result.put("rows", rows);
    return result;
}
```

**체크리스트:**

1. `normalizePaging` **전에** `pageNo`/`pageSize`가 criteria에 포함되어야 함  
   - `searchCriteria(body)` 전체 복사 **또는** `copyIfPresent`에 `pageNo`, `pageSize` 포함
2. `search*`와 `count*`에 **동일한 WHERE** (`<include refid="..."/>`) 사용
3. `totalCount`는 count 쿼리 결과 — `rows.size()`로 대체하지 않음

### 5.2 적용 Service (tcf-om)

| Service | 비고 |
|---------|------|
| `OmServiceCatalogService` | DB 페이징 |
| `OmErrorCodeService` | DB 페이징 |
| `OmTransactionLogService` | DB + `summarize*` |
| `OmUserService` / `OmSessionService` | DB 페이징 |
| `OmAuditLogService` / `OmAuthHistoryService` | DB 페이징 |
| `OmBatchService` | 실행 이력 페이징 |
| `OmCommonCodeService` | **DB + 메모리** 혼합 (아래 5.3) |

### 5.3 메모리 페이징 (캐시 조회)

EhCache 등에서 **전체 목록을 먼저 로드**한 뒤 Java에서 slice 하는 경우:

```java
List<Map<String, Object>> filtered = commonCodeCacheService.loadByCodeGroup(codeGroup);
totalCount = filtered.size();
rows = paginate(filtered, criteria);  // subList((pageNo-1)*pageSize, ...)
```

`paginate()` 내부 offset 계산은 Rule과 동일: `(pageNo - 1) * pageSize`.

| 방식 | when |
|------|------|
| DB 페이징 | 대용량 테이블, 인덱스 활용 |
| 메모리 페이징 | 소량 캐시 데이터, 코드 그룹 목록 등 |

### 5.4 JDBC 직접 페이징 (예외)

파일 업·다운로드(`OmUpdownloadService`)처럼 MyBatis 없이 `JdbcTemplate` 사용 시 Service에서 직접 cap:

```java
int safePageNo = Math.max(1, pageNo);
int safePageSize = Math.min(Math.max(1, pageSize), 100);
int offset = (safePageNo - 1) * safePageSize;
// COUNT(*) + OFFSET ? FETCH NEXT ? ROWS ONLY
```

---

## 6. DAO · MyBatis

상세: [26-mybatis.md](26-mybatis.md) 8.3절

### 6.1 Mapper 메서드 쌍

```java
List<Map<String, Object>> searchServiceCatalog(Map<String, Object> params);
int countServiceCatalog(Map<String, Object> params);
```

### 6.2 XML

**목록** — `offset`, `pageSize` 사용:

```xml
<select id="searchServiceCatalog" parameterType="map" resultType="map">
    SELECT ...
      FROM OM_SERVICE_CATALOG
     WHERE 1 = 1
    <include refid="serviceCatalogSearchWhere"/>
     ORDER BY BUSINESS_CODE, SERVICE_ID
    <if test="pageSize != null">
        OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
    </if>
</select>
```

**건수** — 페이징 절 **없음**, WHERE만 동일:

```xml
<select id="countServiceCatalog" parameterType="map" resultType="int">
    SELECT COUNT(*)
      FROM OM_SERVICE_CATALOG
     WHERE 1 = 1
    <include refid="serviceCatalogSearchWhere"/>
</select>
```

### 6.3 SQL dialect

로컬 H2 `MODE=Oracle` 기준:

```sql
OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
```

일부 쿼리는 `FETCH FIRST #{limit} ROWS ONLY` (Top-N, 대시보드용) — **목록 페이징과 구분**.

운영 DBMS(Oracle 등) 전환 시 dialect 검증 필요.

---

## 7. UI (tcf-ui OM Admin)

### 7.1 요청

화면별 상수 + 현재 페이지 state:

```javascript
const PAGE_SIZE = 10;
let currentPage = 1;

const body = { pageNo: currentPage, pageSize: PAGE_SIZE, ...filters };
const { body: data } = await OmAdmin.call('serviceCatalog', 'inquiry', body);
```

검색 조건 변경 시 **`currentPage = 1`** 로 리셋.

### 7.2 `OmAdmin.renderPagination`

```javascript
OmAdmin.renderPagination(
  document.getElementById('pagination'),
  body.pageNo || page,
  body.pageSize || PAGE_SIZE,
  body.totalCount || 0,
  async (p) => { currentPage = p; await loadList(p); },
  false   // prevNextOnly: true면 PREV/NEXT만
);
```

| 인자 | 설명 |
|------|------|
| `container` | `.om-pagination` DOM |
| `pageNo` | 현재 페이지 |
| `pageSize` | 페이지 크기 |
| `totalCount` | 서버 totalCount |
| `onPage` | 페이지 변경 콜백 |
| `prevNextOnly` | `false` → 페이지 번호 버튼 표시 |

`totalCount === 0` 이면 pagination 숨김.

### 7.3 OM Admin 화면별 pageSize

| 화면 | `PAGE_SIZE` |
|------|-------------|
| service-catalog, error-code, common-code | 10 |
| transaction-log, audit-log, batch | 10~20 |
| session | 10 |
| file-management | 파일 10 / 로그 별도 |

UI `PAGE_SIZE`와 서버 `pageSize`를 **일치**시키는 것이 UX상 자연스럽다. 서버 기본 20과 UI 10이 다르면 요청 body의 `pageSize`가 우선한다.

---

## 8. REST Relay 페이징 (파일 UD)

`tcf-ui` → `tcf-om` 파일 API는 query string으로 전달:

```
GET /ui/api/updownload/files?pageNo=1&pageSize=10
```

`UpdownloadApiController` → `OmUpdownloadService.list(...)` — 동일 cap(100) 적용.

---

## 9. 자주 하는 실수

| 문제 | 원인 | 해결 |
|------|------|------|
| 항상 20건/첫 페이지 | Service가 `pageNo`/`pageSize`를 criteria에 안 넣음 | `searchCriteria(body)` 또는 copy에 포함 |
| totalCount = rows.size() | count 쿼리 누락 | `count*` DAO/Mapper 추가 |
| count와 rows 불일치 | WHERE/refid 다름 | `<include refid="sameWhere"/>` 공유 |
| offset을 클라이언트에 노출 | 보안/일관성 | Rule에서만 계산 |
| pageSize 1000 요청 | 상한 없음 | Rule cap 100 |
| UI 페이지 버튼 안 보임 | `prevNextOnly: true` | service-catalog 등은 `false` |

---

## 10. 신규 목록 API 추가 체크리스트

1. **요청**: body에 `pageNo`, `pageSize` 수용
2. **Rule**: `normalizePaging(criteria)` 호출
3. **DAO/Mapper**: `search*` + `count*`, 공통 WHERE refid
4. **SQL**: `OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY`
5. **응답**: `pageNo`, `pageSize`, `totalCount`, `rows` 필수
6. **UI**: `PAGE_SIZE` 상수, 검색 시 page 1 리셋, `renderPagination` 연동
7. **serviceId** 카탈로그·샘플 JSON에 pageNo/pageSize 예시 추가

---

## 11. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-om/.../rule/OmOperationRule.java` | `normalizePaging` |
| `tcf-om/.../service/OmServiceCatalogService.java` | DB 페이징 Service 예 |
| `tcf-om/.../service/OmCommonCodeService.java` | 메모리 페이징 예 |
| `tcf-om/.../mapper/om/OmOperationMapper.xml` | search/count SQL |
| `tcf-ui/.../_shared/om-admin.js` | `renderPagination` |
| `tcf-ui/.../om/admin/service-catalog.html` | UI 페이징 연동 예 |
| `tcf-ui/.../sample-requests/om-transactions.json` | pageNo/pageSize 샘플 |

---

## 12. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — offset 페이징·Rule/Service/DAO/UI 계약 정리 |
