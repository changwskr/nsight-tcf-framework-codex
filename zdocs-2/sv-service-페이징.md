# sv-service 페이징 설계

`sv-service` 모듈의 목록 조회 페이징은 **[업무페이징.md](업무페이징.md)** 신규 WAR 표준을 따릅니다.  
TCF Core·Dispatcher는 페이징에 관여하지 않으며, **Rule → Service → DAO → MyBatis**에서 처리합니다.

| 문서 | 역할 |
|------|------|
| [업무페이징.md](업무페이징.md) | 업무 WAR 공통 5규칙·계약 |
| **본 문서** | sv-service 적용·참조 구현 (`SV.Sample.inquiry`) |
| [sv-service-페이징-가이드.md](sv-service-페이징-가이드.md) | 업무 개발자용 쉬운 프로그램 설명 |
| [화면페이징.md](화면페이징.md) | OM Admin 레거시 (`rows`, max 100) — SV와 **별도** |
| [DAO처리.md](DAO처리.md) | DAO search/count 쌍 |
| [mybatisNaming.md](mybatisNaming.md) | SQL id 네이밍 |

---

## 1. 개요

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `sv-service` |
| 업무코드 | `SV` |
| 참조 거래 | `SV.Sample.inquiry` (SV-INQ-0001) |
| 참조 테이블 | `SV_SAMPLE` |
| 페이징 유형 | **offset** (1-based `pageNo`) |
| 목록 응답 키 | `list` (OM `rows` 아님) |

`SV.Sample.inquiry`는 신규 SV 목록 INQUIRY를 추가할 때 **복사·확장할 템플릿**입니다.

---

## 2. SV 페이징 정책

공통 표준([업무페이징.md](업무페이징.md) §2) 중 SV에 적용된 값입니다.

| 항목 | SV 값 | 정의 위치 |
|------|-------|-----------|
| `pageNo` 기본값 | `1` | `SvSampleRule.DEFAULT_PAGE_NO` |
| `pageSize` 기본값 | `100` | `SvSampleRule.DEFAULT_PAGE_SIZE` |
| `pageSize` 상한 | **500** (일반 목록) | `SvSampleRule.MAX_PAGE_SIZE` |
| `offset` | 서버 계산 `(pageNo - 1) × pageSize` | Rule `normalizePaging` |
| 클라이언트 `offset` | **수신 안 함** | — |

로그·대량 이력 목록을 SV에 추가할 때는 상한 **1000**을 쓰는 별도 Rule 상수를 두는 것을 권장합니다 (EP `EpUserEventRule` 참고).

---

## 3. 처리 흐름

```text
[Browser / tcf-ui]
  POST /sv/online
  serviceId: SV.Sample.inquiry
  body: { pageNo, pageSize, sampleKey? }
       │
       ▼
SvSampleHandler                 ← 페이징 로직 없음
       │
       ▼
SvSampleFacade.inquiry          @Transactional(readOnly=true)
       │
       ▼
SvSampleService.inquiry
  1. SvSampleRule.validateInquiry(body)
  2. criteria = SvSampleRule.buildSearchCriteria(body)
       → pageNo, pageSize, offset, sampleKey?
  3. list  = SvSampleDao.searchSamples(criteria)
  4. total = SvSampleDao.countSamples(criteria)   // 동일 WHERE
  5. totalPage 계산 후 응답 Map 조립
       │
       ▼
SvSampleDao → SvSampleMapper.xml
  searchSamples : OFFSET … FETCH NEXT …
  countSamples  : COUNT(1)
```

---

## 4. 계층별 구현

### 4.1 Handler / Facade

| 클래스 | 파일 | 페이징 책임 |
|--------|------|-------------|
| `SvSampleHandler` | `entry/handler/SvSampleHandler.java` | 없음 (body 전달만) |
| `SvSampleFacade` | `entry/facade/SvSampleFacade.java` | TX 경계 |

### 4.2 Rule — `SvSampleRule`

**역할:** 검증 + criteria 조립 + 페이징 정규화

```java
// 핵심 메서드
validateInquiry(body)           // body null 금지
buildSearchCriteria(body)       // paging + 검색조건 → criteria Map
normalizePaging(criteria, body, MAX_PAGE_SIZE)
```

criteria Map에 들어가는 키:

| 키 | 출처 | 용도 |
|----|------|------|
| `pageNo` | body (정규화) | 응답·UI |
| `pageSize` | body (정규화) | 응답·FETCH |
| `offset` | Rule 계산 | Mapper만 사용 |
| `sampleKey` | body (trim, 선택) | WHERE LIKE |

### 4.3 Service — `SvSampleService`

**역할:** DAO 호출 + **응답 계약** 조립

```java
int totalPage = totalCount == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
result.put("list", list);
result.put("pageNo", pageNo);
result.put("pageSize", pageSize);
result.put("totalCount", totalCount);
result.put("totalPage", totalPage);
```

`list.size()`로 `totalCount`를 대체하지 않습니다.

### 4.4 DAO / Mapper

| DAO | Mapper id | SQL |
|-----|-----------|-----|
| `searchSamples` | `searchSamples` | 목록 slice |
| `countSamples` | `countSamples` | 전체 건수 |

`SvSampleMapper.xml`에서 `<sql id="sampleSearchWhere">`를 **search·count가 공유**합니다.

```xml
ORDER BY SAMPLE_KEY
OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
```

- `ORDER BY`는 페이징 안정성을 위해 **고정**
- H2 `MODE=Oracle` 로컬·Oracle 운영 dialect 동일 패턴

---

## 5. 요청·응답 계약 (`SV.Sample.inquiry`)

### 5.1 요청 body

```json
{
  "pageNo": 1,
  "pageSize": 10,
  "sampleKey": "A00"
}
```

| 필드 | 필수 | 기본값 | 설명 |
|------|------|--------|------|
| `pageNo` | N | `1` | 1-based 페이지 |
| `pageSize` | N | `100` | 최대 **500**으로 clamp |
| `sampleKey` | N | — | `SAMPLE_KEY` 부분 일치 (`LIKE %…%`) |

### 5.2 응답 body (성공 시)

```json
{
  "businessCode": "SV",
  "serviceId": "SV.Sample.inquiry",
  "guid": "...",
  "list": [
    {
      "sampleKey": "A001",
      "sampleName": "SV 샘플 01",
      "createdAt": "2026-06-27T12:00:00"
    }
  ],
  "pageNo": 1,
  "pageSize": 10,
  "totalCount": 3,
  "totalPage": 1
}
```

### 5.3 OM Admin과의 차이

| 항목 | sv-service | OM Admin |
|------|------------|----------|
| 목록 키 | `list` | `rows` |
| `totalPage` | 서버 계산·응답 | UI가 계산 |
| 기본 pageSize | 100 | 20 |
| max pageSize | 500 | 100 |

SV 화면을 OM Admin `renderPagination`에 붙일 경우 키 매핑(`list` → `rows`) 또는 OM 계약 유지 여부를 **화면별로** 결정합니다.

---

## 6. 데이터 (로컬)

| 리소스 | 내용 |
|--------|------|
| `schema.sql` | `SV_SAMPLE` DDL |
| `data.sql` | 시드 5건 (A001~A003, B001~B002) |
| `application-local.yml` | `spring.sql.init` always |

---

## 7. 테스트

### 7.1 tcf-ui 화면 (권장)

| 환경 | URL |
|------|-----|
| bootRun (tcf-ui 8099) | http://localhost:8099/sv/sample-list.html |
| Tomcat `/ui` | http://localhost:8080/ui/sv/sample-list.html |

화면: `tcf-ui/.../static/sv/sample-list.html`  
릴레이: `tcf-ui/.../static/_shared/sv-admin.js` → `POST /api/relay/SV/online`

**사전 조건:** `sv-service` bootRun (포트 **8086**), `tcf-ui` bootRun (포트 **8099**)

### 7.2 curl

```bash
curl -X POST http://localhost:8086/sv/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json
```

### 7.3 통합 테스트

`sv-service` — `SvTransactionLogIntegrationTest`  
`src/test/resources/sv-sample-inquiry.json` (pageNo/pageSize 포함)

---

## 8. 신규 SV 목록 INQUIRY 추가 절차

1. **Rule** — `validateInquiry`, `buildSearchCriteria`, `MAX_PAGE_SIZE` (일반 500 / 로그 1000)
2. **Service** — `search*` + `count*` → `list`·`totalPage` 응답
3. **DAO / Mapper** — `<sql id="…SearchWhere">` 공유, `ORDER BY` 고정
4. **Handler / Facade** — `serviceId` 등록, Facade `readOnly` TX
5. **시드·샘플 JSON** — 로컬 H2·tcf-ui sample-requests
6. **화면** (선택) — `sv-admin.js` TX 맵 + HTML (또는 `sample-list.html` 패턴 복제)
7. **문서** — 본 문서 § 참조 거래 표 갱신

---

## 9. 소스 맵

```text
sv-service/
├── application/
│   ├── rule/SvSampleRule.java          페이징 정규화·검색조건
│   └── service/SvSampleService.java    inquiry·응답 조립
├── entry/
│   ├── handler/SvSampleHandler.java
│   └── facade/SvSampleFacade.java
├── persistence/
│   ├── dao/SvSampleDao.java
│   └── mapper/SvSampleMapper.java
└── resources/
    ├── mapper/sv/SvSampleMapper.xml
    ├── schema.sql
    └── data.sql

tcf-ui/
├── static/sv/sample-list.html          페이징 테스트 UI
└── static/_shared/sv-admin.js
```

---

## 10. 체크리스트 (SV 목록 INQUIRY)

- [ ] `SvSampleRule` 또는 도메인 Rule에 `DEFAULT_PAGE_*`, `MAX_PAGE_SIZE`
- [ ] criteria에 `offset` 계산, 클라이언트에는 노출하지 않음
- [ ] `search*` / `count*` WHERE 동일
- [ ] 응답 `list`, `totalCount`, `totalPage`
- [ ] 대량 다운로드는 별도 `serviceId`
- [ ] [업무페이징.md](업무페이징.md) 공통 규칙과 충돌 없음
