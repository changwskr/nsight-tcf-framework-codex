# 부록 E. Mapper XML 템플릿

| 항목 | 내용 |
| --- | --- |
| **부록** | E |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## E.1 Mapper XML의 역할

NSIGHT TCF에서 MyBatis Mapper XML은 SQL을 담는 단순 파일이 아니다. SQL 위치, SQL ID, Query Timeout, RDW/ADW 접근 경계, 결과 매핑, 페이징을 **표준 산출물**로 통제하는 메커니즘이다. 업무 판단·권한 판단·트랜잭션 경계·사용자 메시지 생성은 Mapper가 아니라 Service·Rule·ETF가 담당한다.

```text
Handler → Facade → Service / Rule → DAO → Mapper Interface → Mapper XML → RDW / ADW / SESSIONDB
```

Mapper Interface와 Mapper XML은 1:1로 대응하며, DAO는 Mapper Interface만 호출한다. Java 코드에 SQL을 직접 작성하지 않는다.

---

## E.2 파일 위치 기준

Mapper XML은 업무코드 소문자 디렉터리 아래에 둔다.

```text
src/main/resources/mapper/{업무코드소문자}/{MapperName}.xml
```

| 업무 | Mapper Interface | Mapper XML |
| --- | --- | --- |
| SV | `SvCustomerMapper` | `mapper/sv/SvCustomerMapper.xml` |
| SV | `SvCustomerRdwMapper` | `mapper/sv/SvCustomerRdwMapper.xml` |
| CM | `CmCampaignMapper` | `mapper/cm/CmCampaignMapper.xml` |
| OM | `OmUserMapper` | `mapper/om/OmUserMapper.xml` |
| UD | `UdFileMapper` | `mapper/ud/UdFileMapper.xml` |

실제 코드베이스의 SV 고객요약 Mapper는 `sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml`에 위치한다.

---

## E.3 Namespace 작성 기준

`namespace`는 Java Mapper Interface의 **FQCN(완전한 클래스명)** 과 반드시 일치해야 한다.

```java
package com.nh.nsight.marketing.sv.persistence.mapper;

@Mapper
public interface SvCustomerMapper {
    CustomerSummaryRow selectCustomerSummary(CustomerSummaryCriteria criteria);
}
```

```xml
<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">
```

| 항목 | 기준 |
| --- | --- |
| Mapper Interface | `{업무}XxxMapper` |
| Mapper XML | `mapper/{업무소문자}/{업무}XxxMapper.xml` |
| XML namespace | Mapper Interface FQCN |
| SQL ID | Mapper Interface **메서드명**과 일치 |
| 호출 구조 | DAO → Mapper Interface → Mapper XML |

---

## E.4 기본 템플릿 (ResultMap · SQL Fragment · Select)

Mapper XML 상단에는 업무·DB·설명 주석을 남긴다. ResultMap, SQL Fragment, Select를 순서대로 배치한다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

    <!--
        Mapper Name  : SvCustomerMapper
        업무코드      : SV
        DB           : RDW (운영) / H2 (로컬)
        SQL_ID       : SV.Customer.selectSummary
        설명         : 고객 요약조회
    -->

    <!-- 1. ResultMap -->
    <resultMap id="SvCustomerSummaryResultMap"
               type="com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow">
        <id     property="customerNo"          column="CUSTOMER_NO"/>
        <result property="customerName"        column="CUSTOMER_NAME"/>
        <result property="customerGrade"       column="CUSTOMER_GRADE"/>
        <result property="branchCode"          column="BRANCH_CODE"/>
        <result property="branchName"          column="BRANCH_NAME"/>
        <result property="totalBalance"        column="TOTAL_BALANCE"/>
        <result property="lastTransactionDate" column="LAST_TRANSACTION_DATE"/>
    </resultMap>

    <!-- 2. SQL Fragment -->
    <sql id="CustomerSummaryColumns">
          A.CUSTOMER_NO
        , A.CUSTOMER_NAME
        , A.CUSTOMER_GRADE
        , A.BRANCH_CODE
        , B.BRANCH_NAME
        , A.TOTAL_BALANCE
        , A.LAST_TRANSACTION_DATE
    </sql>

    <!-- 3. Select 단건 -->
    <select id="selectCustomerSummary"
            parameterType="com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryCriteria"
            resultMap="SvCustomerSummaryResultMap"
            timeout="3">
        /* SQL_ID: SV.Customer.selectSummary */
        SELECT
        <include refid="CustomerSummaryColumns"/>
        FROM RDW_CUSTOMER_SUMMARY A
        LEFT JOIN RDW_BRANCH B
          ON A.BRANCH_CODE = B.BRANCH_CODE
        WHERE A.CUSTOMER_NO = #{customerNo}
    </select>

</mapper>
```

단순 1:1 컬럼 매핑은 `resultType`과 `AS` Alias로 대체할 수 있다. Join 결과·컬럼명 불일치·중첩 객체가 있으면 `resultMap`을 사용한다.

---

## E.5 Select · Count · Paging 템플릿

### 단건 조회

| 항목 | 기준 |
| --- | --- |
| SQL ID | `select{업무대상}{처리내용}` |
| 예시 | `selectCustomerSummary` |
| `SELECT *` | **금지** |
| 조건 | PK 또는 업무 Key 명확화 |
| Timeout | RDW 기본 **3초** |
| 미조회 | Mapper는 null 반환, 업무 판단은 Service/Rule |

### 목록 조회

목록 조회는 **정렬 기준을 반드시 명시**한다. 정렬이 없으면 페이징 결과가 불안정해진다.

```xml
<select id="selectCustomerList"
        parameterType="com.nh.nsight.marketing.sv.application.dto.customer.CustomerListCriteria"
        resultMap="SvCustomerSummaryResultMap"
        timeout="3">
    SELECT
          A.CUSTOMER_NO
        , A.CUSTOMER_NAME
        , A.CUSTOMER_GRADE
        , A.BRANCH_CODE
        , A.TOTAL_BALANCE
        , A.LAST_TRANSACTION_DATE
    FROM RDW_CUSTOMER_SUMMARY A
    WHERE 1 = 1
    <if test="customerName != null and customerName != ''">
        AND A.CUSTOMER_NAME LIKE '%' || #{customerName} || '%'
    </if>
    <if test="customerGrade != null and customerGrade != ''">
        AND A.CUSTOMER_GRADE = #{customerGrade}
    </if>
    ORDER BY A.LAST_TRANSACTION_DATE DESC, A.CUSTOMER_NO ASC
</select>
```

### Count 조회

목록 조회와 **동일한 WHERE 조건**을 공유하는 Count SQL을 한 쌍으로 작성한다.

```xml
<select id="countCustomerList"
        parameterType="com.nh.nsight.marketing.sv.application.dto.customer.CustomerListCriteria"
        resultType="long"
        timeout="3">
    SELECT COUNT(1)
    FROM RDW_CUSTOMER_SUMMARY A
    WHERE 1 = 1
    <if test="customerName != null and customerName != ''">
        AND A.CUSTOMER_NAME LIKE '%' || #{customerName} || '%'
    </if>
    <if test="customerGrade != null and customerGrade != ''">
        AND A.CUSTOMER_GRADE = #{customerGrade}
    </if>
</select>
```

| SQL ID | 역할 |
| --- | --- |
| `selectCustomerList` | 실제 목록 조회 |
| `countCustomerList` | 전체 건수 조회 |

### Oracle 페이징 (OFFSET … FETCH)

Oracle 12c 이상에서는 `OFFSET … FETCH` 방식을 사용한다.

```xml
<select id="selectCustomerListPaging"
        parameterType="com.nh.nsight.marketing.sv.application.dto.customer.CustomerListCriteria"
        resultMap="SvCustomerSummaryResultMap"
        timeout="3">
    SELECT
          A.CUSTOMER_NO
        , A.CUSTOMER_NAME
        , A.CUSTOMER_GRADE
        , A.BRANCH_CODE
        , A.TOTAL_BALANCE
        , A.LAST_TRANSACTION_DATE
    FROM RDW_CUSTOMER_SUMMARY A
    WHERE 1 = 1
    <if test="customerName != null and customerName != ''">
        AND A.CUSTOMER_NAME LIKE '%' || #{customerName} || '%'
    </if>
    ORDER BY A.LAST_TRANSACTION_DATE DESC, A.CUSTOMER_NO ASC
    OFFSET #{offset} ROWS
    FETCH NEXT #{pageSize} ROWS ONLY
</select>
```

| 항목 | 기준 |
| --- | --- |
| pageNo | 1 이상 |
| pageSize | 기본 100 |
| 최대 pageSize | 일반 조회 500~1,000 이하 |
| offset | `(pageNo - 1) × pageSize` |
| 대량 다운로드 | 일반 목록 조회 SQL과 **분리** |

---

## E.6 Insert · Update · Delete 템플릿

### Insert

```xml
<insert id="insertCampaign"
        parameterType="com.nh.nsight.marketing.cm.application.dto.campaign.CampaignSaveCommand"
        timeout="3">
    INSERT INTO CM_CAMPAIGN (
          CAMPAIGN_ID
        , CAMPAIGN_NAME
        , CAMPAIGN_TYPE
        , CAMPAIGN_STATUS
        , START_DATE
        , END_DATE
        , CREATED_BY
        , CREATED_AT
        , UPDATED_BY
        , UPDATED_AT
    ) VALUES (
          #{campaignId}
        , #{campaignName}
        , #{campaignType}
        , #{campaignStatus}
        , #{startDate}
        , #{endDate}
        , #{createdBy}
        , SYSTIMESTAMP
        , #{updatedBy}
        , SYSTIMESTAMP
    )
</insert>
```

| 항목 | 기준 |
| --- | --- |
| SQL ID | `insert{업무대상}` |
| 감사 컬럼 | `CREATED_BY`, `CREATED_AT`, `UPDATED_BY`, `UPDATED_AT` |
| Key 생성 | Service 또는 DB Sequence — 사전 결정 |
| 업무 판단 | Insert SQL 내부에 복잡한 업무 조건 삽입 **금지** |

### Update

```xml
<update id="updateCampaign"
        parameterType="com.nh.nsight.marketing.cm.application.dto.campaign.CampaignSaveCommand"
        timeout="3">
    UPDATE CM_CAMPAIGN
       SET CAMPAIGN_NAME   = #{campaignName}
         , CAMPAIGN_TYPE   = #{campaignType}
         , CAMPAIGN_STATUS = #{campaignStatus}
         , START_DATE      = #{startDate}
         , END_DATE        = #{endDate}
         , UPDATED_BY      = #{updatedBy}
         , UPDATED_AT      = SYSTIMESTAMP
     WHERE CAMPAIGN_ID     = #{campaignId}
</update>
```

WHERE 조건 없는 전체 Update는 금지한다. 낙관적 잠금이 필요하면 `VERSION` 또는 `UPDATED_AT` 조건을 추가한다.

### 논리삭제 (기본)

운영 시스템에서는 물리삭제보다 논리삭제를 기본으로 한다.

```xml
<update id="deleteCampaign"
        parameterType="com.nh.nsight.marketing.cm.application.dto.campaign.CampaignDeleteCommand"
        timeout="3">
    UPDATE CM_CAMPAIGN
       SET DELETE_YN  = 'Y'
         , USE_YN     = 'N'
         , UPDATED_BY = #{updatedBy}
         , UPDATED_AT = SYSTIMESTAMP
     WHERE CAMPAIGN_ID = #{campaignId}
</update>
```

---

## E.7 Dynamic SQL · IN 조건

동적 SQL은 `<where>`, `<if>`, `<choose>`를 사용한다. 비교 연산자는 XML 충돌 방지를 위해 CDATA를 쓴다.

```xml
<select id="selectCampaignList"
        parameterType="com.nh.nsight.marketing.cm.application.dto.campaign.CampaignListCriteria"
        resultType="com.nh.nsight.marketing.cm.application.dto.campaign.CampaignListItem"
        timeout="3">
    SELECT
          A.CAMPAIGN_ID
        , A.CAMPAIGN_NAME
        , A.CAMPAIGN_STATUS
        , A.START_DATE
        , A.END_DATE
    FROM CM_CAMPAIGN A
    <where>
        A.DELETE_YN = 'N'
        <if test="campaignName != null and campaignName != ''">
            AND A.CAMPAIGN_NAME LIKE '%' || #{campaignName} || '%'
        </if>
        <if test="fromDate != null and fromDate != ''">
            AND A.START_DATE <![CDATA[>=]]> #{fromDate}
        </if>
    </where>
    ORDER BY A.START_DATE DESC, A.CAMPAIGN_ID DESC
</select>
```

IN 조건은 `<foreach>`를 사용하며, 목록 최대 건수는 100~500건 이하로 제한한다. 빈 목록은 Service/Rule에서 사전 차단한다.

```xml
<foreach collection="gradeList" item="grade" open="(" separator="," close=")">
    #{grade}
</foreach>
```

**정렬 컬럼에 사용자 입력을 직접 삽입하지 않는다.** `${}` 사용은 SQL Injection 위험이 있다.

---

## E.8 Timeout · RDW/ADW 분리

| DB | 용도 | Mapper Timeout | 비고 |
| --- | --- | --- | --- |
| RDW | 온라인 실시간 조회 | **3초** | 페이징 필수 |
| ADW | 분석성 조회 | **5초** | 온라인 직접 호출 제한 |
| SESSIONDB | 세션 | 3초 | Spring Session JDBC |
| LOGDB | 거래·감사 로그 | 3초 | INSERT 중심 |

RDW와 ADW는 SQL 특성과 Timeout 기준이 다르므로 **Mapper 파일을 분리**한다.

```text
SvCustomerRdwMapper.xml   — 온라인 실시간, timeout="3"
SvCustomerAdwMapper.xml   — 분석 조회, timeout="5"
```

개별 `<select>`에 `timeout` 속성을 명시하고, `application.yml`의 `default-statement-timeout`과 함께 이중으로 통제한다.

---

## E.9 SQL ID 명명 기준

Mapper XML의 SQL ID는 Java Mapper **메서드명과 일치**시킨다.

| 처리 | SQL ID | Java Method |
| --- | --- | --- |
| 단건 조회 | `selectCustomerSummary` | `selectCustomerSummary()` |
| 목록 조회 | `selectCustomerList` | `selectCustomerList()` |
| 건수 조회 | `countCustomerList` | `countCustomerList()` |
| 페이징 | `selectCustomerListPaging` | `selectCustomerListPaging()` |
| 등록 | `insertCampaign` | `insertCampaign()` |
| 수정 | `updateCampaign` | `updateCampaign()` |
| 삭제 | `deleteCampaign` | `deleteCampaign()` |

ServiceId와 SQL ID는 같을 필요는 없지만, 거래로그와 SQL 로그에서 **추적 가능**해야 한다.

```text
ServiceId  = SV.Customer.selectSummary
Mapper     = SvCustomerMapper
SQL ID     = selectCustomerSummary
Full SQL ID = SvCustomerMapper.selectCustomerSummary
```

SQL 주석에 `/* SQL_ID: SV.Customer.selectSummary */` 형태로 ServiceId를 남기면 운영 추적이 수월하다.

---

## E.10 DAO 호출 패턴

DAO는 Mapper Interface만 주입받아 호출한다. SQL ID를 DAO에서 직접 참조하지 않는다.

```java
@Repository
public class SvCustomerDao {
    private final SvCustomerMapper mapper;

    public SvCustomerDao(SvCustomerMapper mapper) {
        this.mapper = mapper;
    }

    public CustomerSummaryRow selectCustomerSummary(CustomerSummaryCriteria criteria) {
        return mapper.selectCustomerSummary(criteria);
    }
}
```

| 계층 | 책임 |
| --- | --- |
| Service / Rule | 업무 판단, null 처리, 오류코드 변환 |
| DAO | Mapper 호출, 파라미터 전달 |
| Mapper Interface | SQL ID 선언 |
| Mapper XML | SQL 실행, 결과 매핑 |

Query Timeout 예외는 DAO 또는 Service에서 `E-TCF-DB-0002` 등 표준 오류코드로 변환한다.

---

## E.11 MyBatis 설정 (application.yml 연계)

```yaml
mybatis:
  mapper-locations:
    - classpath:/mapper/**/*.xml
  type-aliases-package: com.nh.nsight
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    jdbc-type-for-null: NULL
    call-setters-on-nulls: true
    default-fetch-size: 500
    cache-enabled: false
nsight:
  mybatis:
    rdw-query-timeout-seconds: 3
    adw-query-timeout-seconds: 5
    default-page-size: 100
    max-page-size: 1000
```

---

## E.12 작성 금지 패턴

| 금지 패턴 | 사유 |
| --- | --- |
| `SELECT *` | 컬럼 변경 시 장애, 불필요 데이터 조회 |
| Paging 없는 목록 조회 | 대량 조회 장애 위험 |
| `${}` 사용자 입력 직접 사용 | SQL Injection |
| Java 코드에 SQL 작성 | SQL 추적·표준관리 불가 |
| Mapper에서 업무 판단 | Rule/Service 책임 침범 |
| 하나의 Mapper에 RDW/ADW 혼합 | DB 접근 책임 불명확 |
| 정렬 컬럼 사용자 입력 직접 사용 | SQL Injection |
| 운영 SQL에 테스트 조건 방치 | 운영 장애·보안 리스크 |

---

## E.13 준수 체크리스트

| 점검 항목 | 확인 기준 |
| --- | --- |
| 파일 위치 | `resources/mapper/{업무코드소문자}` 하위인가? |
| Namespace | Java Mapper Interface FQCN과 일치하는가? |
| SQL ID | Mapper Method명과 일치하는가? |
| 컬럼 | `SELECT *` 없이 필요한 컬럼만 명시했는가? |
| ResultMap | 컬럼과 DTO 필드 매핑이 명확한가? |
| Timeout | RDW 3초, ADW 5초 등 기준을 반영했는가? |
| Paging | 목록 조회에 Page Size 제한이 있는가? |
| Count SQL | 목록 조회의 Count SQL이 분리되어 있는가? |
| 정렬 | `ORDER BY`가 명확한가? |
| Injection | `${}` 사용을 피했는가? |
| DB 분리 | RDW/ADW/SESSIONDB Mapper가 분리되어 있는가? |
| 로그 추적 | ServiceId, SQL ID 기준 추적이 가능한가? |

---

## 요약

Mapper XML은 업무 로직이 아니라 **SQL 실행 책임**만 가진다. 파일은 `mapper/{업무소문자}/` 아래에 두고, namespace는 Mapper Interface FQCN, SQL ID는 메서드명과 일치시킨다. RDW 3초·ADW 5초 Timeout, 페이징·Count 분리, `SELECT *` 금지, RDW/ADW Mapper 분리가 핵심이다. DAO → Mapper Interface → Mapper XML 호출 구조를 유지하고, ServiceId·SQL ID로 운영 추적이 가능해야 한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 D](./D-표준-전문-JSON-예시.md) |
| → 다음 | [부록 F](./F-오류코드-표준표.md) |

---

## 출처 색인

- [znsight-man/부록E-Mapper-XML-템플릿.md](../../znsight-man/부록E-Mapper-XML-템플릿.md)
- [znsight-man/28-MyBatis-Mapper-개발.md](../../znsight-man/28-MyBatis-Mapper-개발.md)
- [znsight-man/29-SQL-작성-기준.md](../../znsight-man/29-SQL-작성-기준.md)
- [znsight-man/30-페이징-처리-기준.md](../../znsight-man/30-페이징-처리-기준.md)
- [docs/architecture/26-mybatis.md](../../docs/architecture/26-mybatis.md)
- [docs/architecture/07-DAO.md](../../docs/architecture/07-DAO.md)
- [sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml](../../sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml)
