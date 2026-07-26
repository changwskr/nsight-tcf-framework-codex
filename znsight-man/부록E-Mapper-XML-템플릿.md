# 부록 E. Mapper XML 템플릿

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## E. Mapper XML 템플릿

### E.1 도입 전 안내말

본 부록은 NSIGHT TCF Framework에서 사용하는 MyBatis Mapper XML 표준 템플릿을 정의한다.
Mapper XML은 단순히 SQL을 적는 파일이 아니다. NSIGHT에서는 Mapper XML이 SQL 위치, SQL ID, Query Timeout, RDW/ADW 접근 경계, SQL 추적성, 페이징, 결과 매핑을 통제하는 표준 산출물이다. NSIGHT 운영 기준에서도 MyBatis는 Controller → Facade → Service/Rule → DAO/Mapper → MyBatis XML → RDW/ADW/SESSIONDB 구조에서 SQL 위치와 Query Timeout, RDW/ADW 접근 경계, Mapper 명명, SQL 추적성을 통제하는 메커니즘으로 정의되어 있다.
```text
Handler
  ↓
Facade
  ↓
Service / Rule
  ↓
DAO
  ↓
Mapper Interface
  ↓
Mapper XML
  ↓
RDW / ADW / SESSIONDB / LOGDB / OMDB
```

### E.2 Mapper XML 위치 기준

src/main/resources/mapper/{업무코드소문자}/{MapperName}.xml

예시는 다음과 같다.
| 업무 | Mapper Interface | Mapper XML |
| --- | --- | --- |
| SV | SvCustomerMapper | mapper/sv/SvCustomerMapper.xml |
| SV | SvCustomerRdwMapper | mapper/sv/SvCustomerRdwMapper.xml |
| CM | CmCampaignMapper | mapper/cm/CmCampaignMapper.xml |
| OM | OmUserMapper | mapper/om/OmUserMapper.xml |
| UD | UdFileMapper | mapper/ud/UdFileMapper.xml |

DAO 설계 기준에서도 Mapper XML 위치는 src/main/resources/mapper/{업무코드소문자}이며, SQL은 Java 코드에 직접 작성하지 않고 Mapper XML 기준으로 관리하도록 정의한다.

### E.3 Mapper XML 기본 템플릿

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

    <!--
        Mapper Name  : SvCustomerMapper
        업무코드      : SV
        업무명        : Single View
        DB           : RDW
        설명         : 고객 요약조회 관련 SQL
        작성 기준    : NSIGHT TCF MyBatis Mapper XML 표준
    -->

    <!-- =========================================================
## 1. ResultMap

         ========================================================= -->
    <resultMap id="SvCustomerSummaryResultMap"
               type="com.nh.nsight.marketing.sv.dto.response.SvCustomerSummaryDto">
        <id     property="customerNo"          column="CUSTOMER_NO"/>
        <result property="customerName"        column="CUSTOMER_NAME"/>
        <result property="customerGrade"       column="CUSTOMER_GRADE"/>
        <result property="branchCode"          column="BRANCH_CODE"/>
        <result property="branchName"          column="BRANCH_NAME"/>
        <result property="totalBalance"        column="TOTAL_BALANCE"/>
        <result property="lastTransactionDate" column="LAST_TRANSACTION_DATE"/>
    </resultMap>

    <!-- =========================================================
## 2. SQL Fragment

         ========================================================= -->
    <sql id="CustomerSummaryColumns">
          A.CUSTOMER_NO
        , A.CUSTOMER_NAME
        , A.CUSTOMER_GRADE
        , A.BRANCH_CODE
        , B.BRANCH_NAME
        , A.TOTAL_BALANCE
        , A.LAST_TRANSACTION_DATE
    </sql>

    <!-- =========================================================
## 3. Select 단건

         ========================================================= -->
```xml
<select id="selectCustomerSummary"
            parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerSummaryRequest"
            resultMap="SvCustomerSummaryResultMap"
            timeout="3">
        SELECT
        <include refid="CustomerSummaryColumns"/>
        FROM RDW_CUSTOMER_SUMMARY A
        LEFT JOIN RDW_BRANCH B
          ON A.BRANCH_CODE = B.BRANCH_CODE
        WHERE A.CUSTOMER_NO = #{customerNo}
    </select>
```

</mapper>

### E.4 Namespace 작성 기준

namespace는 반드시 Java Mapper Interface의 FQCN과 일치시킨다.
package com.nh.nsight.marketing.sv.persistence.mapper;

```java
@Mapper
public interface SvCustomerMapper {
    SvCustomerSummaryDto selectCustomerSummary(SvCustomerSummaryRequest request);
}
```

<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

| 항목 | 기준 |
| --- | --- |
| Mapper Interface | SvCustomerMapper |
| Mapper XML | mapper/sv/SvCustomerMapper.xml |
| XML namespace | Mapper Interface FQCN |
| SQL ID | Mapper Interface Method명과 일치 |
| 호출 구조 | DAO → Mapper Interface → Mapper XML |

Mapper Interface와 Mapper XML은 업무코드와 업무기능 단위로 관리하며, Mapper는 SQL 실행, 파라미터 매핑, 결과 매핑만 담당하고 업무 판단·권한 판단·트랜잭션 경계 설정·사용자 메시지 생성은 수행하지 않는다.

### E.5 Select 단건 조회 템플릿

```xml
<select id="selectCustomerSummary"
        parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerSummaryRequest"
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
    WHERE A.CUSTOMER_NO = #{customerNo}
      AND A.BASE_DATE   = #{baseDate}
</select>
```

단건 조회 기준은 다음과 같다.
| 항목 | 기준 |
| --- | --- |
| SQL ID | select{업무대상}{처리내용} |
| 예시 | selectCustomerSummary |
| SELECT * | 금지 |
| 조건 | PK 또는 업무 Key 명확화 |
| Timeout | RDW 기본 3초 |
| 반환 | DTO 또는 ResultMap |
| 미조회 | Mapper는 null 반환 가능, 업무 판단은 Service/Rule에서 처리 |

NSIGHT 운영 기준에서도 SELECT *는 금지 또는 예외 승인 대상으로 보고, 목록 조회는 Paging을 필수로 하며, RDW 3초·ADW 5초 기준의 SQL Timeout을 운영 기준으로 둔다.

### E.6 Select 목록 조회 템플릿

<select id="selectCustomerList"
        parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerListRequest"
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

    <if test="branchCode != null and branchCode != ''">
        AND A.BRANCH_CODE = #{branchCode}
    </if>

    ORDER BY A.LAST_TRANSACTION_DATE DESC, A.CUSTOMER_NO ASC
</select>

목록 조회는 반드시 정렬 기준을 명시한다. 정렬 기준이 없으면 페이징 결과가 불안정해질 수 있다.

### E.7 Count 조회 템플릿

<select id="countCustomerList"
        parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerListRequest"
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

    <if test="branchCode != null and branchCode != ''">
        AND A.BRANCH_CODE = #{branchCode}
    </if>
</select>

목록 조회는 보통 다음 2개 SQL을 한 쌍으로 작성한다.

| SQL ID | 역할 | selectCustomerList |
| --- | --- | --- |
| 실제 목록 조회 | countCustomerList | 전체 건수 조회 |

### E.8 Oracle 페이징 조회 템플릿

Oracle 12c 이상 기준으로 OFFSET … FETCH 방식을 사용한다.
<select id="selectCustomerListPaging"
        parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerListRequest"
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

    <if test="branchCode != null and branchCode != ''">
        AND A.BRANCH_CODE = #{branchCode}
    </if>

    ORDER BY A.LAST_TRANSACTION_DATE DESC, A.CUSTOMER_NO ASC

    OFFSET #{offset} ROWS
    FETCH NEXT #{pageSize} ROWS ONLY
</select>

페이징 처리 기준은 다음과 같다.
| 항목 | 기준 |
| --- | --- |
| pageNo | 1 이상 |
| pageSize | 기본 100 |
| 최대 pageSize | 일반 조회 500 또는 1,000 이하 |
| offset | (pageNo - 1) × pageSize |
| 정렬 | 반드시 고정 ORDER BY 사용 |
| 대량 다운로드 | 일반 목록 조회와 분리 |

페이징 설계 기준에서도 page 정보를 표준 전문에 포함하고 MyBatis SQL에서 DB 레벨 페이징을 수행하는 방식을 기준으로 정리한다.

### E.9 Insert 템플릿

<insert id="insertCampaign"
        parameterType="com.nh.nsight.marketing.cm.dto.request.CmCampaignSaveRequest"
        timeout="3">
    INSERT INTO CM_CAMPAIGN (
          CAMPAIGN_ID
        , CAMPAIGN_NAME
        , CAMPAIGN_TYPE
        , CAMPAIGN_STATUS
        , START_DATE
        , END_DATE
        , DESCRIPTION
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
        , #{description}
        , #{createdBy}
        , SYSTIMESTAMP
        , #{updatedBy}
        , SYSTIMESTAMP
    )
</insert>

Insert 기준은 다음과 같다.
| 항목 | 기준 |
| --- | --- |
| SQL ID | insert{업무대상} |
| 생성자 | CREATED_BY, CREATED_AT 포함 권장 |
| 수정자 | 최초 등록 시 UPDATED_BY, UPDATED_AT 함께 입력 가능 |
| Key 생성 | Service 또는 DB Sequence 기준을 명확히 결정 |
| 업무 판단 | Insert SQL 내부에 복잡한 업무 조건 삽입 금지 |

### E.10 Update 템플릿

<update id="updateCampaign"
        parameterType="com.nh.nsight.marketing.cm.dto.request.CmCampaignSaveRequest"
        timeout="3">
    UPDATE CM_CAMPAIGN
       SET CAMPAIGN_NAME   = #{campaignName}
         , CAMPAIGN_TYPE   = #{campaignType}
         , CAMPAIGN_STATUS = #{campaignStatus}
         , START_DATE      = #{startDate}
         , END_DATE        = #{endDate}
         , DESCRIPTION     = #{description}
         , UPDATED_BY      = #{updatedBy}
         , UPDATED_AT      = SYSTIMESTAMP
     WHERE CAMPAIGN_ID     = #{campaignId}
</update>

Update 기준은 다음과 같다.
| 항목 | 기준 |
| --- | --- |
| SQL ID | update{업무대상} |
| WHERE 조건 | PK 또는 업무 Key 필수 |
| 무조건 전체 Update | 금지 |
| 변경자 정보 | UPDATED_BY, UPDATED_AT 포함 |
| 낙관적 잠금 | 필요 시 VERSION 또는 UPDATED_AT 조건 추가 |

### E.11 Delete / 논리삭제 템플릿

운영 시스템에서는 물리삭제보다 논리삭제를 기본으로 한다.
<update id="deleteCampaign"
        parameterType="com.nh.nsight.marketing.cm.dto.request.CmCampaignDeleteRequest"
        timeout="3">
    UPDATE CM_CAMPAIGN
       SET DELETE_YN  = 'Y'
         , USE_YN     = 'N'
         , UPDATED_BY = #{updatedBy}
         , UPDATED_AT = SYSTIMESTAMP
     WHERE CAMPAIGN_ID = #{campaignId}
</update>

물리삭제가 필요한 경우에는 별도 승인 또는 배치 정리 정책을 둔다.
<delete id="deleteCampaignPhysical"
        parameterType="string"
        timeout="3">
    DELETE FROM CM_CAMPAIGN
     WHERE CAMPAIGN_ID = #{campaignId}
</delete>

### E.12 Merge / Upsert 템플릿

기준정보성 데이터는 MERGE를 사용할 수 있다.
<insert id="mergeCommonCode"
        parameterType="com.nh.nsight.marketing.om.dto.request.OmCommonCodeSaveRequest"
        timeout="3">
    MERGE INTO OM_COMMON_CODE T
    USING (
        SELECT
              #{codeGroup} AS CODE_GROUP
            , #{code}      AS CODE
        FROM DUAL
    ) S
    ON (
        T.CODE_GROUP = S.CODE_GROUP
        AND T.CODE = S.CODE
    )

| WHEN MATCHED THEN | UPDATE SET |
| --- | --- |
| T.CODE_NAME  = #{codeName} | , T.SORT_ORDER = #{sortOrder} |
| , T.USE_YN     = #{useYn} | , T.UPDATED_BY = #{updatedBy} |
| , T.UPDATED_AT = SYSTIMESTAMP | WHEN NOT MATCHED THEN |
| INSERT ( | CODE_GROUP |
| , CODE | , CODE_NAME |
| , SORT_ORDER | , USE_YN |
| , CREATED_BY | , CREATED_AT |
| , UPDATED_BY | , UPDATED_AT |
| ) | VALUES ( |

              #{codeGroup}
            , #{code}
            , #{codeName}
            , #{sortOrder}
            , #{useYn}
            , #{createdBy}
            , SYSTIMESTAMP
            , #{updatedBy}
            , SYSTIMESTAMP
        )
</insert>

MERGE는 편리하지만 등록과 수정의 감사 이력을 구분하기 어려울 수 있으므로, 감사 대상 업무는 insert와 update를 분리하는 것이 좋다.

### E.13 Dynamic SQL 템플릿

동적 SQL은 <where>, <if>, <choose>를 사용한다.
<select id="selectCampaignList"
        parameterType="com.nh.nsight.marketing.cm.dto.request.CmCampaignListRequest"
        resultType="com.nh.nsight.marketing.cm.dto.response.CmCampaignListItem"
        timeout="3">
    SELECT
          A.CAMPAIGN_ID
        , A.CAMPAIGN_NAME
        , A.CAMPAIGN_TYPE
        , A.CAMPAIGN_STATUS
        , A.START_DATE
        , A.END_DATE
    FROM CM_CAMPAIGN A

    <where>
        A.DELETE_YN = 'N'

        <if test="campaignName != null and campaignName != ''">
            AND A.CAMPAIGN_NAME LIKE '%' || #{campaignName} || '%'
        </if>

        <if test="campaignStatus != null and campaignStatus != ''">
            AND A.CAMPAIGN_STATUS = #{campaignStatus}
        </if>

        <if test="fromDate != null and fromDate != ''">
            AND A.START_DATE <![CDATA[>=]]> #{fromDate}
        </if>

        <if test="toDate != null and toDate != ''">
            AND A.END_DATE <![CDATA[<=]]> #{toDate}
        </if>
    </where>

    ORDER BY A.START_DATE DESC, A.CAMPAIGN_ID DESC
</select>

동적 SQL 사용 기준은 다음과 같다.
| 구분 | 기준 |
| --- | --- |
| 조건 분기 | <if> 사용 |
| 복잡한 조건 선택 | <choose>, <when>, <otherwise> 사용 |
| WHERE 자동 처리 | <where> 사용 |
| SET 자동 처리 | <set> 사용 |
| 비교 연산자 | XML 충돌 방지를 위해 CDATA 사용 |
| 정렬 컬럼 | 사용자 입력 직접 삽입 금지 |

### E.14 In 조건 템플릿

```xml
<select id="selectCustomerByGradeList"
        parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerGradeListRequest"
        resultMap="SvCustomerSummaryResultMap"
        timeout="3">
    SELECT
          A.CUSTOMER_NO
        , A.CUSTOMER_NAME
        , A.CUSTOMER_GRADE
        , A.BRANCH_CODE
    FROM RDW_CUSTOMER_SUMMARY A
    WHERE A.CUSTOMER_GRADE IN
    <foreach collection="gradeList"
             item="grade"
             open="("
             separator=","
             close=")">
        #{grade}
    </foreach>
    ORDER BY A.CUSTOMER_NO ASC
</select>
```

IN 조건은 최대 건수를 제한해야 한다.
| 항목 | 기준 |
| --- | --- |
| IN 목록 최대 | 100~500건 이하 권장 |
| 대량 목록 | 임시 테이블, 배치, 파일 처리 검토 |
| 빈 목록 | Service/Rule에서 사전 차단 |

### E.15 Update Dynamic SET 템플릿

<update id="updateUser"
        parameterType="com.nh.nsight.marketing.om.dto.request.OmUserUpdateRequest"
        timeout="3">
    UPDATE OM_USER
    <set>
        <if test="userName != null and userName != ''">
            USER_NAME = #{userName},
        </if>
        <if test="userStatus != null and userStatus != ''">
            USER_STATUS = #{userStatus},
        </if>
        <if test="authGroupId != null and authGroupId != ''">
            AUTH_GROUP_ID = #{authGroupId},
        </if>
        UPDATED_BY = #{updatedBy},
        UPDATED_AT = SYSTIMESTAMP
    </set>
    WHERE USER_ID = #{userId}
</update>

동적 Update는 변경 대상 컬럼이 명확한 경우에만 사용한다. 핵심 기준정보는 명시적 Update SQL을 권장한다.

### E.16 ResultMap 작성 기준

<resultMap id="OmUserResultMap"
           type="com.nh.nsight.marketing.om.dto.response.OmUserDto">
    <id     property="userId"       column="USER_ID"/>
    <result property="userName"     column="USER_NAME"/>
    <result property="branchId"     column="BRANCH_ID"/>
    <result property="authGroupId"  column="AUTH_GROUP_ID"/>
    <result property="userStatus"   column="USER_STATUS"/>
    <result property="lastLoginAt"  column="LAST_LOGIN_AT"/>
    <result property="createdAt"    column="CREATED_AT"/>
    <result property="updatedAt"    column="UPDATED_AT"/>
</resultMap>

| 구분 | 기준 |
| --- | --- |
| 단순 DTO | resultType 사용 가능 |
| 컬럼명 불일치 | resultMap 사용 |
| Join 결과 | resultMap 권장 |
| 중복 컬럼 | Alias 명시 |
| 날짜/금액 | Java 타입 명확화 |
| Null 컬럼 | call-setters-on-nulls 설정 고려 |

### E.17 RDW / ADW Mapper 분리 템플릿

RDW와 ADW는 성격이 다르므로 Mapper를 분리한다.
SvCustomerRdwMapper.xml
= 온라인 실시간 조회
= Timeout 3초
= 소량 조회 / 페이징 필수

SvCustomerAdwMapper.xml
= 분석성 조회
= Timeout 5초
= 대량 조회 주의
= 온라인 직접 호출 제한

<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerRdwMapper">

```xml
<select id="selectCustomerSummary"
            parameterType="string"
            resultType="com.nh.nsight.marketing.sv.dto.response.SvCustomerSummaryDto"
            timeout="3">
        SELECT
              CUSTOMER_NO
            , CUSTOMER_NAME
            , CUSTOMER_GRADE
            , TOTAL_BALANCE
        FROM RDW_CUSTOMER_SUMMARY
        WHERE CUSTOMER_NO = #{customerNo}
    </select>
```

</mapper>

<mapper namespace="com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerAdwMapper">

```xml
<select id="selectCustomerAnalysisList"
            parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerAnalysisRequest"
            resultType="com.nh.nsight.marketing.sv.dto.response.SvCustomerAnalysisDto"
            timeout="5">
        SELECT
              CUSTOMER_SEGMENT
            , CUSTOMER_GRADE
            , COUNT(1) AS CUSTOMER_COUNT
            , SUM(TOTAL_BALANCE) AS TOTAL_BALANCE
        FROM ADW_CUSTOMER_ANALYSIS
        WHERE BASE_DATE = #{baseDate}
        GROUP BY CUSTOMER_SEGMENT, CUSTOMER_GRADE
    </select>
```

</mapper>

NSIGHT MyBatis 관리 기준에서도 RDW와 ADW는 SQL 특성과 Timeout 기준이 다르므로 Mapper를 분리하도록 정의한다.

### E.18 파일 다운로드용 조회 템플릿

<mapper namespace="com.nh.nsight.marketing.ud.persistence.mapper.UdFileMapper">

    <resultMap id="UdFileMetaResultMap"
               type="com.nh.nsight.marketing.ud.dto.UdFileMeta">
        <id     property="fileId"           column="FILE_ID"/>
        <result property="originalName"     column="ORIGINAL_NAME"/>
        <result property="storedName"       column="STORED_NAME"/>
        <result property="storagePath"      column="STORAGE_PATH"/>
        <result property="contentType"      column="CONTENT_TYPE"/>
        <result property="fileSize"         column="FILE_SIZE"/>
        <result property="businessCode"     column="BUSINESS_CODE"/>
        <result property="securityLevel"    column="SECURITY_LEVEL"/>
        <result property="expireTime"       column="EXPIRE_TIME"/>
        <result property="useYn"            column="USE_YN"/>
        <result property="deleteYn"         column="DELETE_YN"/>
    </resultMap>

```xml
<select id="selectFileMeta"
            parameterType="string"
            resultMap="UdFileMetaResultMap"
            timeout="3">
        SELECT
              FILE_ID
            , ORIGINAL_NAME
            , STORED_NAME
            , STORAGE_PATH
            , CONTENT_TYPE
            , FILE_SIZE
            , BUSINESS_CODE
            , SECURITY_LEVEL
            , EXPIRE_TIME
            , USE_YN
            , DELETE_YN
        FROM UD_FILE_META
        WHERE FILE_ID = #{fileId}
    </select>
```

</mapper>

파일 원본은 DB에 저장하지 않고, DB에는 파일 메타정보만 저장한다. 실제 파일 Stream 처리는 파일 다운로드 전용 API 또는 Storage Service에서 담당한다.

### E.19 Batch Job Mapper 템플릿

<mapper namespace="com.nh.nsight.tcf.batch.persistence.mapper.BatchExecutionMapper">

    <insert id="insertExecutionStart"
            parameterType="com.nh.nsight.tcf.batch.model.BatchContext"
            timeout="3">
        INSERT INTO TCF_BATCH_EXECUTION (
              EXECUTION_ID
            , JOB_ID
            , RUN_TYPE
            , BUSINESS_DATE
            , STATUS
            , START_TIME
            , REQUESTED_BY
            , GUID
            , TRACE_ID
        ) VALUES (
              #{executionId}
            , #{jobId}
            , #{runType}
            , #{businessDate}
            , 'RUNNING'
            , SYSTIMESTAMP
            , #{requestedBy}
            , #{guid}
            , #{traceId}
        )
    </insert>

    <update id="updateExecutionEnd"
            timeout="3">
        UPDATE TCF_BATCH_EXECUTION
           SET STATUS      = #{status}
             , END_TIME    = SYSTIMESTAMP
             , READ_COUNT  = #{readCount}
             , WRITE_COUNT = #{writeCount}
             , SKIP_COUNT  = #{skipCount}
             , ERROR_COUNT = #{errorCount}
             , MESSAGE     = #{message}
         WHERE EXECUTION_ID = #{executionId}
    </update>

</mapper>

### E.20 SQL ID 명명 기준

Mapper XML의 SQL ID는 Java Mapper Method명과 일치시킨다.

| 처리 | SQL ID | Java Method |
| --- | --- | --- |
| 단건 조회 | selectCustomerSummary | selectCustomerSummary() |

| 목록 조회 | selectCustomerList |
| --- | --- |
| selectCustomerList() | 건수 조회 |

countCustomerList
countCustomerList()

| 등록 | insertCampaign |
| --- | --- |
| insertCampaign() | 수정 |

updateCampaign
updateCampaign()

| 삭제 | deleteCampaign |
| --- | --- |
| deleteCampaign() | 저장 |
| mergeCommonCode | mergeCommonCode() |
ServiceId와 SQL ID는 완전히 같을 필요는 없지만, 거래로그와 SQL 로그에서 추적 가능해야 한다.

| ServiceId     = SV.Customer.selectSummary | |
| Mapper        = SvCustomerMapper | SQL ID        = selectCustomerSummary |

Full SQL ID   = SvCustomerMapper.selectCustomerSummary

### E.21 MyBatis 설정 예시

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
    safe-row-bounds-enabled: true
    safe-result-handler-enabled: true
    cache-enabled: false
nsight:
  mybatis:
    sql-id-required: true
    default-page-size: 100
    max-page-size: 1000
    rdw-query-timeout-seconds: 3
    adw-query-timeout-seconds: 5
    block-full-table-scan: true
```

NSIGHT 운영 기준에서도 mapper-locations는 업무별 Mapper 관리 기준으로 보고, default-statement-timeout, default-fetch-size, cache-enabled=false, max-page-size, RDW/ADW Query Timeout을 MyBatis 운영 기준으로 관리한다.

### E.22 Mapper XML 작성 금지 패턴

| 금지 패턴 | 사유 |
| --- | --- |
| SELECT * | 컬럼 변경 시 장애, 불필요 데이터 조회 |

조건 없는 대량 목록 조회
AP Thread, DB, 네트워크 부하 증가

| Paging 없는 목록 조회 | 대량 조회 장애 위험 |
| --- | --- |
| ${} 사용자 입력 직접 사용 | SQL Injection 위험 |

Java 코드에 SQL 작성
SQL 추적·표준관리 불가
Mapper에서 업무 판단
Rule/Service 책임 침범
Mapper에서 사용자 메시지 생성
ETF 오류처리 표준 위반
하나의 Mapper에 RDW/ADW/LOGDB 혼합
DB 접근 책임 불명확

| 정렬 컬럼 사용자 입력 직접 사용 | SQL Injection 위험 |
| --- | --- |
| 운영 SQL에 임시 주석·개발자 테스트 조건 방치 | 운영 장애·보안 리스크 |

### E.23 Mapper XML 최종 체크리스트

| 점검 항목 | 확인 기준 | 파일 위치 |
| --- | --- | --- |
| resources/mapper/{업무코드소문자} 하위인가? | Namespace | Java Mapper Interface FQCN과 일치하는가? |
| SQL ID | Mapper Method명과 일치하는가? | 컬럼 |
| SELECT * 없이 필요한 컬럼만 명시했는가? | ResultMap | 컬럼과 DTO 필드 매핑이 명확한가? |
| Timeout | RDW 3초, ADW 5초 등 기준을 반영했는가? | Paging |
| 목록 조회에 Page Size 제한이 있는가? | Count SQL | 목록 조회의 Count SQL이 분리되어 있는가? |
| 정렬 | ORDER BY가 명확한가? | Injection |
| ${} 사용을 피했는가? | DB 분리 | RDW/ADW/SESSIONDB/LOGDB Mapper가 분리되어 있는가? |
| 로그 추적 | ServiceId, SQL ID 기준 추적이 가능한가? |  |

### E.24 최종 정리

Mapper XML 템플릿의 핵심은 다음이다.
Mapper XML
= SQL 위치 표준
+ SQL ID 표준
+ ResultMap 표준
+ Timeout 표준
+ Paging 표준
+ RDW/ADW 접근 경계 표준

따라서 NSIGHT TCF Framework에서 Mapper XML은 다음 한 문장으로 정의할 수 있다.
Mapper XML은 업무 로직이 아니라 SQL 실행 책임만 가지며, ServiceId·거래코드·GUID 기준으로 운영자가 추적 가능한 표준 SQL 산출물이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (76).docx`

| [mybatisNaming.md](../zdoc/mybatisNaming.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)