# TCF MyBatis 네이밍 정리

NSIGHT TCF의 MyBatis 네이밍은 **`serviceId` → Service → DAO → Mapper → SQL id** 가 한 줄로 추적되도록 설계합니다.  
Mapper method와 XML `id`는 **반드시 동일**하며, DAO method도 같은 이름을 사용합니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **파일·패키지** | 모듈별 XML 분리 | `mapper/{bc}/{Business}{Domain}Mapper.xml` |
| **인터페이스** | SQL 진입점 | `@Mapper` + `{Business}{Domain}Mapper` |
| **SQL id** | 검색·추적 키 | `select*`, `search*`, `count*`, `insert*`, `update*`, `merge*`, `delete*` |
| **파라미터** | 조건 Map | `camelCase` 키 (`userId`, `pageNo`, `offset`) |
| **결과 alias** | Java Map 키 | `AS "camelCase"` (권장) |
| **재사용 SQL** | WHERE 공통화 | `<sql id="{domain}{Purpose}Where">` |

관련 문서: [네이밍.md](네이밍.md) · [DAO처리.md](DAO처리.md) · [테이블정보.md](테이블정보.md)

---

## 1. 목표

| 목표 | 설명 |
|------|------|
| **추적성** | IDE에서 `searchErrorCodes` 검색 시 DAO·Mapper·XML이 동시에 조회됨 |
| **일관성** | 업무코드(OM/SV/EB…)가 달라도 동일 접두·동사 규칙 유지 |
| **계층 분리** | SQL id는 영속 동작만 표현, `serviceId` 문자열을 SQL id에 넣지 않음 |
| **유지보수** | 목록/건수 쌍(`search*` / `count*`)으로 페이징 WHERE 중복 최소화 |

---

## 2. 전체 추적 구조

```text
serviceId: OM.ErrorCode.update
   │
   ├─ Service  : OmErrorCodeService.update(...)
   ├─ DAO      : OmOperationDao.updateErrorCode(params)
   ├─ Mapper   : OmOperationMapper.updateErrorCode(params)
   └─ SQL id   : updateErrorCode   ← XML <update id="updateErrorCode">
```

**3자 일치 (강제)**

```text
DAO method  ==  Mapper interface method  ==  Mapper XML id
```

---

## 3. 패키지 · 파일 · 클래스

### 3.1 디렉터리 구조

```text
src/main/java/com/nh/nsight/marketing.{bc}/
└── persistence/
    ├── dao/       {Business}{Domain}Dao.java       @Repository
    └── mapper/    {Business}{Domain}Mapper.java    @Mapper

src/main/resources/mapper/{bc}/
    └── {Business}{Domain}Mapper.xml
```

| 항목 | 규칙 | 예 |
|------|------|-----|
| `{bc}` | 업무코드 소문자 | `om`, `sv`, `eb`, `jwt` |
| `{Business}` | 업무 약어 PascalCase | `Om`, `Sv`, `Eb`, `Jwt` |
| `{Domain}` | 도메인·집합 단위 | `Operation`, `User`, `Token`, `Sample` |
| Mapper XML 파일명 | 인터페이스와 동일 | `OmOperationMapper.xml` |

**예시 경로**

```text
tcf-om/.../persistence/mapper/OmOperationMapper.java
tcf-om/.../resources/mapper/om/OmOperationMapper.xml

tcf-jwt/.../persistence/mapper/JwtTokenMapper.java
tcf-jwt/.../resources/mapper/jwt/JwtTokenMapper.xml

eb-service/.../persistence/mapper/EbUserMapper.java
eb-service/.../resources/mapper/eb/EbUserMapper.xml
```

### 3.2 Mapper XML namespace (강제)

```xml
<mapper namespace="com.nh.nsight.marketing.om.persistence.mapper.OmOperationMapper">
```

- namespace = Mapper 인터페이스 **FQCN**
- 파일명·클래스명·namespace **3종 일치**

### 3.3 @MapperScan

```java
@MapperScan("com.nh.nsight.marketing.{bc}.persistence.mapper")
```

- `tcf-jwt`: `com.nh.nsight.auth.jwt.persistence.mapper`
- 스캔 범위는 `persistence.mapper` 패키지로 고정

### 3.4 Mapper 분리 기준

| 기준 | 권장 |
|------|------|
| 도메인 단위 | `EbUserMapper`, `EbEventMapper` — 테이블·유스케이스별 분리 |
| OM 운영 집합 | `OmOperationMapper` — OM Admin 공통 CRUD 집합 (기존 관례) |
| 금지 | `CommonMapper`, `BaseMapper`, `SqlMapper` — 책임 모호 |

---

## 4. DAO · Mapper 메서드 네이밍

### 4.1 SQL id 접두어 (표준)

| 접두어 | 용도 | 반환 타입 | 예 |
|--------|------|-----------|-----|
| `select` | 단건·집계·상태 조회 | `Map`, `List<Map>`, `int`, `Integer` | `selectErrorCodeByCode`, `selectTxSummary` |
| `search` | 목록·페이징 조회 | `List<Map>` | `searchErrorCodes`, `searchUsers` |
| `count` | 건수 (페이징 total) | `int` | `countErrorCodes`, `countUsers` |
| `sum` | 합계·집계 | `int` | `sumSessionStatusActiveCount` |
| `insert` | 단건 등록 | `int` | `insertUser`, `insertJwtToken` |
| `update` | 단건 수정 | `int` | `updateErrorCode`, `updateUserLastLoginTime` |
| `merge` | MERGE / UPSERT | `int` | `mergeErrorCode` |
| `delete` | 물리 삭제·전체 삭제 | `int` | `deleteTransactionControl`, `deleteAllApStatus` |
| `disable` | 논리 삭제 (USE_YN='N') | `int` | `disableErrorCode`, `disableUser` |
| `revoke` | 토큰·권한 폐기 | `int` | `revokeJwtToken` |
| `mark` | 플래그 전환 | `int` | `markRefreshTokenRotated` |

### 4.2 메서드명 패턴

| 패턴 | 용도 | 예 |
|------|------|-----|
| `select{Domain}By{Key}` | PK·UK 단건 | `selectUserById`, `selectErrorCodeByCode` |
| `select{Domain}For{Purpose}` | 목적별 단건 | `selectUserForLogin` |
| `search{DomainPlural}` | 목록 | `searchMenus`, `searchTransactionLogs` |
| `count{DomainPlural}` | 목록 건수 | `countMenus`, `countTransactionLogs` |
| `insert{Domain}` | 등록 | `insertAuthGroup` |
| `update{Domain}` | 일반 수정 | `updateMenu` |
| `update{Domain}{Detail}` | 부분 수정 | `updateUserWithPassword`, `updateUserPasswordHash` |
| `disable{Domain}` | 비활성 | `disableServiceCatalog` |
| `delete{Domain}By{Key}` | 조건 삭제 | `deleteFunctionAuthById` |
| `deleteAll{Domain}` | 관리용 전체 삭제 | `deleteAllAuditLogs` |

### 4.3 금지·지양

| 금지 | 이유 | 대안 |
|------|------|------|
| `upd01`, `getData`, `qryList` | 검색·의미 불명 | `updateErrorCode`, `searchErrorCodes` |
| `doSelect`, `execSql` | 동사 중복·모호 | `select*`, `search*` |
| `OM_ErrorCode_UPDATE` | serviceId·거래코드 혼입 | `updateErrorCode` |
| Mapper에 비즈니스 판단 메서드 | 계층 침범 | Service에서 판단 |
| DAO ≠ Mapper 이름 | 추적 단절 | 동일 메서드명 유지 |

---

## 5. Mapper XML 태그 · SQL id

### 5.1 태그와 id 매핑

| SQL 종류 | XML 태그 | id 예 |
|----------|----------|-------|
| 조회 | `<select id="...">` | `searchUsers` |
| 등록 | `<insert id="...">` | `insertUser` |
| 수정 | `<update id="...">` | `updateErrorCode` |
| 삭제 | `<delete id="...">` | `deleteSpringSession` |

```xml
<update id="updateErrorCode" parameterType="map">
    UPDATE OM_ERROR_CODE
       SET ERROR_MESSAGE = #{errorMessage},
           USE_YN = #{useYn}
     WHERE ERROR_CODE = #{errorCode}
</update>
```

```java
// OmOperationMapper.java
int updateErrorCode(Map<String, Object> params);
```

### 5.2 parameterType · resultType

| 항목 | 규칙 | 예 |
|------|------|-----|
| 복합 조건 | `parameterType="map"` | 검색·CUD 대부분 |
| 단일 키 | `parameterType="string"` | `selectUserById(String userId)` |
| 단건 Map | `resultType="map"` | `Map<String, Object>` |
| 목록 | `resultType="map"` | `List<Map<String, Object>>` |
| 건수·영향행 | `resultType="int"` | `count*`, `insert*`, `update*` |

- 별도 DTO/Entity 클래스명을 resultType에 쓰지 않음 — TCF 표준은 **`Map` 기반** 전달

---

## 6. 파라미터(Map) 키 네이밍

### 6.1 규칙

| 항목 | 규칙 | 예 |
|------|------|-----|
| 키 이름 | `camelCase` | `userId`, `errorCode`, `fromTime` |
| 페이징 | `pageNo`, `pageSize`, `offset` | Rule에서 `offset` 계산 |
| 기간 | `fromTime`, `toTime`, `baseDate` | 로그·통계 조회 |
| 제한 | `limit` | TOP N 조회 |
| YN 플래그 | `useYn`, `revokedYn` | DB `USE_YN` 대응 |

### 6.2 DAO에서 Map 조립

Service가 넘긴 `criteria`를 그대로 쓰거나, DAO가 인자를 Map으로 묶습니다.

```java
public List<Map<String, Object>> selectErrorTop(String fromTime, String toTime, int limit) {
    Map<String, Object> params = new HashMap<>();
    params.put("fromTime", fromTime);
    params.put("toTime", toTime);
    params.put("limit", limit);
    return mapper.selectErrorTop(params);
}
```

- XML `#{fromTime}` 키와 Java `params.put("fromTime", ...)` **철자 일치** 필수

---

## 7. SELECT 컬럼 alias (결과 Map 키)

### 7.1 권장 — 따옴표 camelCase

```xml
SELECT USER_ID AS "userId",
       ERROR_CODE AS "errorCode",
       USE_YN AS "useYn"
  FROM OM_ERROR_CODE
```

- Service·화면에서 `row.get("userId")`로 **안정적** 접근
- `map-underscore-to-camel-case` 설정과 무관하게 키 고정

### 7.2 허용 — 따옴표 없는 camelCase

```xml
SELECT u.USER_ID AS userId,
       u.USER_NAME AS userName
  FROM EB_USER u
```

- `map-underscore-to-camel-case: true` 환경에서 동작
- **신규 SQL은 따옴표 camelCase 권장** (`tcf-om` 관례)

### 7.3 금지

| 금지 | 이유 |
|------|------|
| alias 없이 `SELECT *` | 스키마 변경 시 키 불안정 |
| `AS USER_ID` (DB 컬럼 그대로) | Java Map 키가 대문자·언더스코어로 고정 |
| 한 쿼리 내 alias 스타일 혼용 | 팀 내 가독성 저하 |

---

## 8. 테이블 · 컬럼 (SQL 내부)

### 8.1 테이블명

| 구분 | 규칙 | 예 |
|------|------|-----|
| 프레임워크 | `TCF_` | `TCF_TX_LOG`, `TCF_JWT_TOKEN` |
| OM 운영 | `OM_` | `OM_USER`, `OM_ERROR_CODE` |
| 업무 | `{BC}_` | `EB_USER`, `EP_USER_EVENT` |
| 세션 | Spring 표준 | `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` |

상세: [테이블정보.md](테이블정보.md)

### 8.2 컬럼·바인딩

| DB 컬럼 | SQL 바인딩 `#{...}` |
|---------|---------------------|
| `USER_ID` | `#{userId}` |
| `USE_YN` | `#{useYn}` |
| `LAST_LOGIN_TIME` | `#{lastLoginTime}` |

- SQL 본문: **대문자 스네이크** (`USER_ID`, `ERROR_CODE`)
- MyBatis 바인딩: **camelCase** (`#{userId}`)

---

## 9. 동적 SQL · SQL fragment

### 9.1 `<sql id>` 네이밍

| 패턴 | 용도 | 예 |
|------|------|-----|
| `{domain}SearchWhere` | 목록·건수 공통 WHERE | `userSearchWhere`, `errorCodeSearchWhere` |
| `{domain}WithMetricsWhere` | 집계용 필터 | `sessionStatusWithMetricsWhere` |
| `{table}Columns` | INSERT 컬럼 목록 (필요 시) | `jwtTokenColumns` |

```xml
<sql id="userSearchWhere">
    <where>
        <if test="userId != null and userId != ''">
            AND u.USER_ID LIKE CONCAT('%', #{userId}, '%')
        </if>
        <if test="userName != null and userName != ''">
            AND u.USER_NAME LIKE CONCAT('%', #{userName}, '%')
        </if>
    </where>
</sql>

<select id="searchUsers" parameterType="map" resultType="map">
    SELECT u.USER_ID AS "userId", u.USER_NAME AS "userName"
      FROM EB_USER u
    <include refid="userSearchWhere"/>
     ORDER BY u.CREATED_AT DESC
     OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
</select>

<select id="countUsers" parameterType="map" resultType="int">
    SELECT COUNT(1) FROM EB_USER u
    <include refid="userSearchWhere"/>
</select>
```

### 9.2 동적 SQL 규칙

| 항목 | 규칙 |
|------|------|
| null·빈문자 | `!= null and != ''` 동시 검사 |
| 목록/건수 | **동일 `<sql id>`** 로 WHERE 공유 |
| 정렬·페이징 | `search*`에만 포함, `count*`에는 제외 |
| 비교 연산자 | XML 이스케이프 `&gt;`, `&lt;` 사용 |

---

## 10. 페이징 네이밍

### 10.1 요청·SQL 파라미터

| 키 | 의미 | 비고 |
|----|------|------|
| `pageNo` | 1-based 페이지 | Rule 기본값 1 |
| `pageSize` | 페이지 크기 | Rule 상한 100 |
| `offset` | `(pageNo - 1) * pageSize` | Rule에서 계산 후 Map에 put |

### 10.2 SQL id 쌍 (강제)

```text
search{DomainPlural}  +  count{DomainPlural}
```

- **동일 WHERE** — `<include refid="...Where"/>`
- 응답 body 관례: `pageNo`, `pageSize`, `totalCount`, `rows`

```xml
OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
```

---

## 11. MyBatis 설정 (공통)

`application.yml` 기준:

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

| 설정 | 의미 |
|------|------|
| `mapper-locations` | `mapper/{bc}/*.xml` 일괄 로드 |
| `map-underscore-to-camel-case` | alias 미지정 시 `USER_ID` → `userId` |
| `default-statement-timeout` | 기본 3초 (Timeout 정책과 연동) |
| `cache-enabled: false` | 2차 캐시 비활성 (TCF Cache 별도 운영) |

---

## 12. 도메인별 Mapper 예시

### 12.1 OM ErrorCode (CUD + 단건)

```text
Service   OmErrorCodeService.update(...)
DAO       OmOperationDao.updateErrorCode(criteria)
Mapper    OmOperationMapper.updateErrorCode(criteria)
SQL id    updateErrorCode
```

관련 id 집합:

```text
searchErrorCodes / countErrorCodes
selectErrorCodeByCode
insertErrorCode / updateErrorCode / mergeErrorCode / disableErrorCode
```

### 12.2 JWT 토큰

```text
JwtTokenDao  →  JwtTokenMapper  →  mapper/jwt/JwtTokenMapper.xml

insertJwtToken / insertRefreshToken
selectRefreshTokenByHash
revokeJwtToken / markRefreshTokenRotated
selectUserForLogin
```

### 12.3 EB 사용자 (페이징)

```text
EbUserDao.searchUsers  →  searchUsers
EbUserDao.countUsers   →  countUsers
EbUserDao.countByUserId  →  countByUserId   (존재 여부 — 예외적 단건 count)
```

---

## 13. 좋은 예 / 나쁜 예

### 13.1 좋은 예

```text
파일     : mapper/om/OmOperationMapper.xml
namespace: com.nh.nsight.marketing.om.persistence.mapper.OmOperationMapper
DAO      : OmOperationDao.searchErrorCodes(criteria)
Mapper   : OmOperationMapper.searchErrorCodes(params)
SQL id   : searchErrorCodes
alias    : ERROR_CODE AS "errorCode"
페이징   : searchErrorCodes + countErrorCodes + errorCodeSearchWhere
```

### 13.2 나쁜 예

```text
파일     : mapper/om/om_sql.xml
namespace: omMapper
SQL id   : upd01, list2
DAO      : OmDao.update()  →  Mapper.updateData()
alias    : SELECT * (alias 없음)
페이징   : search만 있고 count 없음 / WHERE 복붙
```

---

## 14. 신규 SQL 추가 체크리스트

1. **Mapper 인터페이스** — `{Business}{Domain}Mapper`에 메서드 선언
2. **XML** — `mapper/{bc}/` 아래 동일 파일에 `<select|insert|update|delete id="...">`
3. **id 일치** — Mapper method == XML id == DAO method
4. **namespace** — Mapper FQCN
5. **접두어** — `select` / `search` / `count` / `insert` / `update` / `merge` / `delete` / `disable`
6. **alias** — `AS "camelCase"` (권장)
7. **파라미터** — `#{camelCase}` ↔ Map 키 일치
8. **목록** — `search*` + `count*` + 공통 `<sql id="*Where">`
9. **페이징** — `offset`, `pageSize` (Rule에서 `offset` 확정)
10. **카탈로그** — 신규 거래면 `serviceId`·`transactionCode`는 [네이밍.md](네이밍.md) 따로 등록 (SQL id와 혼동 금지)

---

## 15. 개발자 체크리스트

1. DAO · Mapper · XML `id`가 **완전히 같은가?**
2. SQL id만 보고 **조회/등록/수정/삭제** 의도가 드러나는가?
3. 목록 조회에 **count 쌍**과 공통 WHERE fragment가 있는가?
4. 결과 Map 키가 **camelCase**로 통일되어 있는가?
5. `#{...}` 바인딩명이 Map 키·alias와 **일관**되는가?
6. `CommonMapper`, 축약 id(`upd01`)를 **사용하지 않았**는가?
7. Handler/Service에 SQL 문자열·MyBatis API를 **직접 넣지 않았**는가?

---

## 16. 구현 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `tcf-om/.../persistence/dao/OmOperationDao.java` |
| 2 | `tcf-om/.../persistence/mapper/OmOperationMapper.java` |
| 3 | `tcf-om/.../resources/mapper/om/OmOperationMapper.xml` |
| 4 | `tcf-jwt/.../resources/mapper/jwt/JwtTokenMapper.xml` |
| 5 | `eb-service/.../resources/mapper/eb/EbUserMapper.xml` |
| 6 | `tcf-om/src/main/resources/application.yml` (`mybatis` 섹션) |
| 7 | `tcf-web/.../config/TcfMyBatisAutoConfiguration.java` |

---

## 관련 문서

- [네이밍.md](네이밍.md) — 계층·serviceId·SQL id 개요
- [DAO처리.md](DAO처리.md) — DAO 책임·페이징·트랜잭션
- [테이블정보.md](테이블정보.md) — 물리 테이블·컬럼
- [타임아웃관리.md](타임아웃관리.md) — SQL timeout 정책
