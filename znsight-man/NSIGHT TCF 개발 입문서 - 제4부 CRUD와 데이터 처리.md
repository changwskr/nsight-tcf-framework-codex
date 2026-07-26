
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제4부. CRUD와 데이터 처리 제대로 구현하기

## 1. 도입 전 안내말

제3부에서는 고객 요약정보를 조회하는 첫 번째 TCF 거래를 구현했습니다.

```
화면
→ ServiceId
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ DB
→ ETF
→ 표준 응답
```

이번 제4부에서는 업무 시스템에서 가장 자주 개발하는 CRUD 기능을 다룹니다.

```
C: Create
   데이터를 등록한다.

R: Read
   데이터를 조회한다.

U: Update
   데이터를 변경한다.

D: Delete
   데이터를 삭제한다.
```

CRUD는 단순히 SQL 네 개를 작성하는 작업이 아닙니다.

기업 시스템에서 CRUD 거래를 구현할 때는 다음 사항을 함께 판단해야 합니다.

```
조회 결과가 없으면 정상인가, 오류인가?

목록은 몇 건까지 조회할 수 있는가?

등록 요청이 두 번 들어오면 어떻게 할 것인가?

두 사용자가 동시에 같은 데이터를 변경하면 어떻게 할 것인가?

삭제는 실제 행 삭제인가, 사용중지 처리인가?

변경 이력과 감사로그를 남겨야 하는가?

SQL이 느리면 어느 Timeout이 적용되는가?

일부 SQL만 성공했을 때 전체를 Rollback할 것인가?
```

같은 테이블을 사용하는 거래라도 조회와 변경의 위험 수준은 다릅니다.

```
조회 실패
→ 사용자가 정보를 보지 못함

등록 중복
→ 동일 데이터가 두 번 생성됨

변경 충돌
→ 다른 사용자의 변경사항을 덮어씀

삭제 오류
→ 복구하기 어려운 데이터 손실
```

따라서 CRUD를 구현할 때는 SQL 문법보다 먼저 다음 내용을 설계해야 합니다.

```
업무 목적
→ 데이터 소유권
→ 거래 유형
→ 검증 규칙
→ 트랜잭션 경계
→ 동시성 정책
→ 중복방지 정책
→ 감사·이력 정책
→ 성능·용량 기준
```

제4부에서는 캠페인 업무를 중심으로 다음 거래를 구현합니다.

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 단건 조회 | CM.Campaign.selectDetail | CM-INQ-0001 |
| 목록 조회 | CM.Campaign.selectList | CM-INQ-0002 |
| 신규 등록 | CM.Campaign.create | CM-REG-0001 |
| 정보 변경 | CM.Campaign.update | CM-UPD-0001 |
| 사용 중지 | CM.Campaign.deactivate | CM-UPD-0002 |

## 2. 제4부 개요

### 2.1 목적

제4부의 목적은 초보 개발자가 CRUD 거래를 단순 SQL 실행이 아니라 운영 가능한 업무 거래로 설계하고 구현하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 단건 조회와 목록 조회를 구분한다.
- 조회 결과 없음의 처리기준을 정의한다.
- 검색조건과 페이징 DTO를 작성한다.
- 등록 거래의 중복방지와 식별자 생성 방식을 이해한다.
- 변경 전 현재 상태와 버전을 검증한다.
- 물리 삭제와 논리 삭제를 구분한다.
- Facade에 적절한 트랜잭션 경계를 설정한다.
- 일부 실패 시 Rollback 여부를 판단한다.
- Mapper SQL의 성능과 보안 기준을 적용한다.
- 변경 이력·감사로그·운영로그를 연결한다.
- CRUD별 정상·오류·동시성 테스트를 작성한다.
- CI/CD 품질 Gate에 SQL과 데이터 변경 검증을 포함한다.

### 2.2 적용범위

| 영역 | 학습 내용 |
| --- | --- |
| 조회 | 단건·목록·요약·상세 조회 |
| 검색 | 조건 검색, 선택조건, 코드값 |
| 페이징 | 페이지 번호, 크기, 전체 건수 |
| 정렬 | 허용 컬럼과 정렬 방향 |
| 등록 | 신규 데이터 생성, 키 발급 |
| 변경 | 상태 검증, 동시성 제어 |
| 삭제 | 물리 삭제, 논리 삭제, 폐기 |
| 트랜잭션 | Commit, Rollback, 읽기 전용 |
| 중복방지 | Idempotency Key, 업무 중복검사 |
| 데이터 이력 | 변경 전후 값과 변경자 기록 |
| SQL 품질 | Binding, 인덱스, 전체조회 제한 |
| 운영 | ServiceId, Timeout, 감사, 로그 |

### 2.3 대상 독자

- 단건 조회 이후 목록과 등록 기능을 구현해야 하는 개발자
- MyBatis 동적 SQL을 처음 작성하는 개발자
- 트랜잭션과 Rollback이 어려운 초보 개발자
- 중복 등록과 동시 변경 문제를 처음 접하는 개발자
- 물리 삭제와 논리 삭제의 차이를 알고 싶은 개발자
- SQL 성능과 운영 추적성을 함께 고려해야 하는 개발자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
ServiceId는 Handler를 찾는 실행 식별자다.

거래코드는 통제·감사·통계용 식별자다.

Handler는 Facade를 호출한다.

트랜잭션은 Facade에 둔다.

Service는 Rule과 DAO를 조합한다.

DAO는 Mapper를 호출한다.

Mapper는 SQL을 실행한다.
```

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| CRUD | 등록·조회·변경·삭제의 기본 데이터 처리 |
| 단건 조회 | 하나의 식별자로 한 건을 조회 |
| 목록 조회 | 조건에 맞는 여러 건을 조회 |
| 페이징 | 전체 결과를 일정 크기로 나누어 조회 |
| 물리 삭제 | DB 행을 실제로 삭제 |
| 논리 삭제 | 행을 유지하고 삭제·사용중지 상태로 변경 |
| 트랜잭션 | 여러 DB 작업을 하나의 작업처럼 처리 |
| Commit | 처리 결과를 DB에 최종 반영 |
| Rollback | 처리 중 발생한 변경을 취소 |
| Idempotency | 같은 요청이 반복되어도 결과가 한 번만 반영되는 특성 |
| 낙관적 잠금 | 버전값을 비교하여 동시 변경 충돌을 막는 방식 |
| 비관적 잠금 | DB 행을 잠가 다른 변경을 기다리게 하는 방식 |
| 이력 테이블 | 변경 전후 상태를 기록하는 테이블 |
| 감사로그 | 누가 어떤 데이터를 조회·변경했는지 기록 |
| N+1 | 목록 한 번 이후 행마다 추가 SQL이 반복되는 문제 |

## 제21장. CRUD 거래를 설계하는 기준

### 21.1 CRUD는 SQL 종류가 아니다

CRUD를 다음처럼만 이해하면 안 됩니다.

```
Create = INSERT
Read   = SELECT
Update = UPDATE
Delete = DELETE
```

실무에서는 같은 SQL 종류라도 업무 의미가 다릅니다.

예를 들어 UPDATE SQL은 다음 업무를 모두 표현할 수 있습니다.

```
고객 연락처 변경
캠페인 승인
상품 판매중지
배치 처리상태 갱신
오류 재처리 상태 변경
논리 삭제
```

따라서 거래는 SQL 종류가 아니라 업무 목적을 기준으로 설계합니다.

```
금지
CM.Campaign.updateData

권장
CM.Campaign.update
CM.Campaign.approve
CM.Campaign.cancel
CM.Campaign.deactivate
```

### 21.2 처리유형별 위험도

| 유형 | 대표 위험 | 핵심 통제 |
| --- | --- | --- |
| 단건 조회 | 개인정보 과다조회 | 권한·마스킹 |
| 목록 조회 | 대량조회·DB 부하 | 페이징·조회 제한 |
| 등록 | 중복 생성 | Idempotency·중복검사 |
| 변경 | 동시 수정 충돌 | 버전검사·상태검사 |
| 삭제 | 데이터 손실 | 논리 삭제·승인 |
| 승인 | 권한 오남용 | 분리된 권한·감사 |
| 대량 변경 | 장시간 Lock | Batch·분할 Commit |

### 21.3 거래를 나누는 기준

다음 기능을 하나의 ServiceId로 구현한다고 가정합니다.

```
CM.Campaign.manage
```

요청 Body의 actionType에 따라 조회·등록·수정·삭제를 모두 처리합니다.

```
{
  "actionType": "UPDATE",
  "campaignId": "CMP001"
}
```

이 구조는 권장하지 않습니다.

이유:

- 권한이 서로 다름
- Timeout이 서로 다름
- 감사 기준이 서로 다름
- 중복방지 필요 여부가 다름
- 조회와 변경 트랜잭션이 혼합됨
- 장애 통계가 모호해짐
- 테스트 범위가 지나치게 커짐
권장 구조:

```
CM.Campaign.selectDetail
CM.Campaign.selectList
CM.Campaign.create
CM.Campaign.update
CM.Campaign.deactivate
```

### 21.4 CRUD 거래별 기본정책

| 항목 | 조회 | 등록 | 변경 | 삭제·중지 |
| --- | --- | --- | --- | --- |
| 트랜잭션 | readOnly | 쓰기 | 쓰기 | 쓰기 |
| Idempotency | 보통 불필요 | 필요 검토 | 필요 검토 | 필요 검토 |
| 감사 | 개인정보 조회 시 | 중요 데이터 | 중요 데이터 | 필수 검토 |
| 권한 | 조회권한 | 등록권한 | 변경권한 | 삭제·중지 권한 |
| Timeout | 짧게 | 업무에 따라 | 업무에 따라 | 업무에 따라 |
| 중복검사 | 해당 없음 | 중요 | 경우에 따라 | 상태검사 |
| 버전검사 | 해당 없음 | 해당 없음 | 권장 | 권장 |
| 이력 | 조회 감사 | 생성 이력 | 변경 이력 | 폐기 이력 |

### 21.5 데이터 소유권

업무 데이터를 변경할 수 있는 주체를 명확히 해야 합니다.

```
캠페인 데이터
→ CM 업무가 소유

고객 기본정보
→ 고객 소유 업무가 관리

상품 기준정보
→ PD 업무가 관리
```

다른 업무가 테이블을 볼 수 있다고 해서 직접 변경할 수 있는 것은 아닙니다.

금지:

```
SV 업무 Service
→ CM_CAMPAIGN_MASTER 직접 UPDATE
```

권장:

```
SV 업무
→ CM.Campaign.update ServiceId 호출
→ CM 업무가 검증 후 변경
```

### 21.6 CRUD 구현 순서

```
업무 규칙 정의
→ 요청·응답 계약
→ 권한·감사 정책
→ 트랜잭션·동시성 정책
→ ServiceId와 거래코드
→ 프로그램 설계
→ SQL 설계
→ 테스트 설계
→ 구현
```

SQL을 먼저 만들고 나중에 업무 규칙을 맞추는 방식은 피합니다.

### 제21장 요약

```
CRUD는 SQL 네 개가 아니다.

조회·등록·변경·삭제는
권한, Timeout, 감사, 동시성,
중복방지 정책이 서로 다른 별도 거래다.
```

## 제22장. 단건 조회 구현하기

### 22.1 단건 조회의 정의

단건 조회는 고유 식별값을 이용해 하나의 업무 대상을 조회하는 거래입니다.

예:

```
캠페인 ID로 캠페인 상세 조회
고객번호로 고객 상세 조회
상품코드로 상품 상세 조회
```

이번 ServiceId:

```
CM.Campaign.selectDetail
```

### 22.2 요청 DTO

```
public record CampaignDetailRequest(

    @NotBlank(message = "캠페인 ID는 필수입니다.")
    @Size(max = 30)
    String campaignId

) {
}
```

요청 전문:

```
{
  "header": {
    "businessCode": "CM",
    "serviceId": "CM.Campaign.selectDetail",
    "transactionCode": "CM-INQ-0001",
    "processingType": "INQUIRY"
  },
  "body": {
    "campaignId": "CMP202600001"
  }
}
```

### 22.3 응답 DTO

```
public record CampaignDetailResponse(
    String campaignId,
    String campaignName,
    String campaignStatus,
    String startDate,
    String endDate,
    long version
) {
    public static CampaignDetailResponse from(
            CampaignDetailData data) {

        return new CampaignDetailResponse(
            data.campaignId(),
            data.campaignName(),
            data.campaignStatus(),
            data.startDate(),
            data.endDate(),
            data.version()
        );
    }
}
```

version은 이후 변경 거래의 동시성 검증에 사용합니다.

### 22.4 Service 흐름

```
요청 검증
→ 조회권한 검증
→ 캠페인 상세 조회
→ 조회결과 존재 검증
→ 개인정보·민감정보 마스킹
→ 응답 DTO 변환
@Service
@RequiredArgsConstructor
public class CmCampaignService {

    private final CmCampaignRule campaignRule;
    private final CmCampaignDao campaignDao;

    public CampaignDetailResponse selectDetail(
            CampaignDetailRequest request,
            TransactionContext context) {

        campaignRule.validateDetailRequest(request);
        campaignRule.validateInquiryAuthority(context);

        CampaignDetailData data =
            campaignDao.selectCampaignDetail(
                request.campaignId()
            );

        campaignRule.validateCampaignExists(data);

        return CampaignDetailResponse.from(data);
    }
}
```

### 22.5 Mapper SQL

```
<select id="selectCampaignDetail"
        resultMap="campaignDetailResultMap">

    SELECT CAMPAIGN_ID,
           CAMPAIGN_NAME,
           CAMPAIGN_STATUS,
           START_DATE,
           END_DATE,
           VERSION_NO
      FROM CM_CAMPAIGN_MASTER
     WHERE CAMPAIGN_ID = #{campaignId}
       AND DELETE_YN   = 'N'

</select>
```

### 22.6 조회 결과 없음 처리

조회 결과 없음은 거래 성격에 따라 다르게 처리합니다.

#### 상세 조회

사용자가 특정 ID의 상세정보를 요청한 경우:

```
결과 없음
→ 업무 오류
```

예:

```
E-CM-CAM-0002
캠페인을 찾을 수 없습니다.
```

#### 선택 조회

있으면 사용하고 없어도 정상인 경우:

```
결과 없음
→ null 또는 빈 결과
→ 정상 처리 가능
```

예:

```
고객의 선택적 마케팅 동의정보
최근 상담이력
선택 부가정보
```

따라서 “SELECT 결과가 없으면 무조건 오류”로 만들면 안 됩니다.

### 22.7 정상 응답

```
{
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "campaignId": "CMP202600001",
    "campaignName": "우수고객 캠페인",
    "campaignStatus": "DRAFT",
    "startDate": "20260801",
    "endDate": "20260831",
    "version": 3
  }
}
```

### 22.8 금지 예시

```
SELECT *
  FROM CM_CAMPAIGN_MASTER
 WHERE CAMPAIGN_ID = #{campaignId}
```

문제:

- 불필요한 컬럼 조회
- 민감정보 노출 가능
- 테이블 컬럼 추가 영향
- 응답 계약 불명확

### 제22장 요약

```
단건 조회는
고유 식별값으로 하나의 대상을 조회한다.

조회 결과 없음이
업무 오류인지 정상 빈 결과인지
거래별로 정의해야 한다.
```

## 제23장. 목록 조회와 페이징 구현하기

### 23.1 목록 조회의 위험

목록 조회는 단건 조회보다 시스템 부하가 클 수 있습니다.

```
검색조건 없음
→ 수백만 건 조회
→ DB CPU·I/O 증가
→ WAS 메모리 증가
→ 응답 지연
→ Tomcat Thread 장기 점유
```

따라서 온라인 목록 조회에는 기본적으로 제한이 필요합니다.

```
필수 검색조건
페이징
최대 페이지 크기
정렬 컬럼 제한
조회기간 제한
Timeout
```

### 23.2 목록 조회 ServiceId

```
CM.Campaign.selectList
```

거래코드:

```
CM-INQ-0002
```

### 23.3 요청 DTO

```
public record CampaignListRequest(

    String campaignName,

    String campaignStatus,

    @Pattern(regexp = "\\d{8}")
    String startDateFrom,

    @Pattern(regexp = "\\d{8}")
    String startDateTo,

    @Min(1)
    int pageNumber,

    @Min(1)
    @Max(100)
    int pageSize,

    String sortField,

    String sortDirection

) {
}
```

### 23.4 페이징 기본값

클라이언트가 값을 보내지 않았을 때 기본값을 적용할 수 있습니다.

```
pageNumber = 1
pageSize   = 20
```

최대 크기:

```
pageSize ≤ 100
```

개발자가 화면 요청값을 그대로 사용하게 두면 사용자가 pageSize=1000000을 보낼 수 있습니다.

서버에서 반드시 제한해야 합니다.

### 23.5 페이징 응답

```
public record PageResponse<T>(
    List<T> items,
    long totalCount,
    int pageNumber,
    int pageSize,
    int totalPages,
    boolean hasNext
) {
}
```

응답 예:

```
{
  "body": {
    "items": [
      {
        "campaignId": "CMP001",
        "campaignName": "여름 캠페인",
        "campaignStatus": "DRAFT"
      },
      {
        "campaignId": "CMP002",
        "campaignName": "가을 캠페인",
        "campaignStatus": "APPROVED"
      }
    ],
    "totalCount": 47,
    "pageNumber": 1,
    "pageSize": 20,
    "totalPages": 3,
    "hasNext": true
  }
}
```

### 23.6 Offset 계산

```
offset
= (pageNumber - 1) × pageSize
```

예:

```
pageNumber = 3
pageSize = 20

offset = 40
```

### 23.7 Query DTO

```
public record CampaignListQuery(
    String campaignName,
    String campaignStatus,
    String startDateFrom,
    String startDateTo,
    int offset,
    int pageSize,
    String sortColumn,
    String sortDirection
) {
}
```

Request DTO와 Query DTO를 분리하면 화면의 정렬필드명을 DB 컬럼명으로 안전하게 변환할 수 있습니다.

### 23.8 정렬 컬럼 화이트리스트

사용자 요청:

```
{
  "sortField": "campaignName",
  "sortDirection": "ASC"
}
```

서버 변환:

```
private String resolveSortColumn(String sortField) {
    return switch (sortField) {
        case "campaignName" -> "CAMPAIGN_NAME";
        case "startDate" -> "START_DATE";
        case "createdAt" -> "CREATE_DTM";
        default -> "CAMPAIGN_ID";
    };
}
```

정렬 방향:

```
private String resolveSortDirection(
        String direction) {

    return "DESC".equalsIgnoreCase(direction)
        ? "DESC"
        : "ASC";
}
```

### 23.9 정렬 시 ${} 사용 주의

동적 정렬 컬럼에는 일반 Binding을 사용할 수 없습니다.

```
ORDER BY #{sortColumn}
```

은 컬럼이 아니라 문자열 값으로 처리될 수 있습니다.

따라서 다음처럼 ${}를 사용해야 하는 경우가 있습니다.

```
ORDER BY ${sortColumn} ${sortDirection}
```

하지만 클라이언트 입력을 그대로 사용하면 SQL Injection 위험이 있습니다.

반드시 서버에서 화이트리스트로 변환된 값만 전달해야 합니다.

```
금지
클라이언트 sortField를 그대로 SQL에 삽입

권장
서버 허용목록으로 DB 컬럼명을 결정
```

### 23.10 목록 SQL

Oracle 환경의 개념 예:

```
<select id="selectCampaignList"
        resultMap="campaignListResultMap">

    SELECT CAMPAIGN_ID,
           CAMPAIGN_NAME,
           CAMPAIGN_STATUS,
           START_DATE,
           END_DATE
      FROM CM_CAMPAIGN_MASTER
     WHERE DELETE_YN = 'N'

    <if test="campaignName != null
              and campaignName != ''">
       AND CAMPAIGN_NAME LIKE '%' || #{campaignName} || '%'
    </if>

    <if test="campaignStatus != null
              and campaignStatus != ''">
       AND CAMPAIGN_STATUS = #{campaignStatus}
    </if>

    <if test="startDateFrom != null
              and startDateFrom != ''">
       AND START_DATE &gt;= #{startDateFrom}
    </if>

    <if test="startDateTo != null
              and startDateTo != ''">
       AND START_DATE &lt;= #{startDateTo}
    </if>

     ORDER BY ${sortColumn} ${sortDirection}
    OFFSET #{offset} ROWS
     FETCH NEXT #{pageSize} ROWS ONLY

</select>
```

### 23.11 전체 건수 SQL

```
<select id="countCampaignList"
        resultType="long">

    SELECT COUNT(*)
      FROM CM_CAMPAIGN_MASTER
     WHERE DELETE_YN = 'N'

    <if test="campaignName != null
              and campaignName != ''">
       AND CAMPAIGN_NAME LIKE '%' || #{campaignName} || '%'
    </if>

    <if test="campaignStatus != null
              and campaignStatus != ''">
       AND CAMPAIGN_STATUS = #{campaignStatus}
    </if>

</select>
```

목록 SQL과 Count SQL의 조건이 서로 달라지면 전체 건수가 잘못 표시됩니다.

공통 SQL Fragment를 사용할 수 있습니다.

```
<sql id="campaignSearchCondition">
    ...
</sql>
```

### 23.12 전체 건수 조회 생략

모든 목록 조회에서 반드시 전체 건수가 필요한 것은 아닙니다.

```
전체 건수가 필요함
→ 화면에 총 12,350건 표시

전체 건수가 불필요함
→ 다음 페이지 존재 여부만 확인
```

대량 테이블의 COUNT(*)는 비쌀 수 있습니다.

다음 페이지 존재 여부만 필요하다면 pageSize + 1건을 조회하는 방식을 사용할 수 있습니다.

### 23.13 검색기간 제한

다음과 같은 온라인 조회는 위험합니다.

```
최근 10년 전체 캠페인
검색조건 없음
페이지 크기 1000
```

기준 예:

```
기본 조회기간: 최근 1개월
최대 조회기간: 1년
장기 조회: 별도 다운로드·배치
```

### 23.14 목록 없음 처리

목록 조회 결과가 0건인 것은 일반적으로 정상입니다.

```
{
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000"
  },
  "body": {
    "items": [],
    "totalCount": 0,
    "pageNumber": 1,
    "pageSize": 20,
    "totalPages": 0,
    "hasNext": false
  }
}
```

빈 목록을 업무 오류로 처리하면 화면에서 불필요한 오류 팝업이 발생합니다.

### 23.15 N+1 문제

금지 구조:

```
캠페인 목록 100건 조회
→ 각 캠페인마다 담당자 조회 SQL
→ 총 101번 SQL 실행
```

이를 N+1 문제라고 합니다.

개선 방법:

- 필요한 데이터를 Join으로 조회
- 여러 ID를 한 번에 조회
- 기준정보 Cache 활용
- 조회모델 구성

### 23.16 목록 조회 체크리스트

| 항목 | 확인 |
| --- | --- |
| 기본 검색조건이 있는가? | □ |
| 조회기간 제한이 있는가? | □ |
| 페이징이 적용되는가? | □ |
| 최대 pageSize가 있는가? | □ |
| 정렬 컬럼이 화이트리스트인가? | □ |
| 전체 건수가 실제로 필요한가? | □ |
| 목록 없음은 정상 빈 목록인가? | □ |
| N+1 SQL이 발생하지 않는가? | □ |
| 인덱스를 활용하는 조건인가? | □ |
| 다운로드는 별도 거래인가? | □ |

### 제23장 요약

```
목록 조회는 반드시
페이징과 조회 제한을 적용한다.

정렬 컬럼은 화이트리스트로 통제하고,
빈 목록은 일반적으로 정상 처리한다.
```

## 제24장. 등록 거래 구현하기

### 24.1 등록 거래의 기본 흐름

캠페인 신규 등록 거래:

```
CM.Campaign.create
```

처리 순서:

```
요청 검증
→ 등록 권한 확인
→ 업무 중복 확인
→ 신규 ID 발급
→ 캠페인 기본정보 등록
→ 변경 이력 등록
→ 감사로그 기록
→ 성공 응답
```

### 24.2 요청 DTO

```
public record CampaignCreateRequest(

    @NotBlank
    @Size(max = 100)
    String campaignName,

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    String startDate,

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    String endDate,

    @NotBlank
    String campaignType

) {
}
```

### 24.3 응답 DTO

```
public record CampaignCreateResponse(
    String campaignId,
    String campaignStatus,
    long version
) {
}
```

### 24.4 Facade 트랜잭션

```
@Component
@RequiredArgsConstructor
public class CmCampaignFacade {

    private final CmCampaignService campaignService;

    @Transactional
    public CampaignCreateResponse create(
            CampaignCreateRequest request,
            TransactionContext context) {

        return campaignService.create(request, context);
    }
}
```

등록은 DB 변경 거래이므로 readOnly = true를 사용하지 않습니다.

### 24.5 Service 구현

```
@Service
@RequiredArgsConstructor
public class CmCampaignService {

    private final CmCampaignRule campaignRule;
    private final CmCampaignDao campaignDao;
    private final CampaignIdGenerator idGenerator;

    public CampaignCreateResponse create(
            CampaignCreateRequest request,
            TransactionContext context) {

        CampaignPeriod period =
            campaignRule.validateCreateRequest(request);

        campaignRule.validateCreateAuthority(context);

        boolean duplicated =
            campaignDao.existsSameCampaign(
                request.campaignName(),
                request.startDate(),
                request.endDate()
            );

        campaignRule.validateNotDuplicated(duplicated);

        String campaignId =
            idGenerator.generate();

        CampaignCreateCommand command =
            CampaignCreateCommand.of(
                campaignId,
                request,
                period,
                context.getUserId()
            );

        int inserted =
            campaignDao.insertCampaign(command);

        campaignRule.validateInsertedCount(inserted);

        campaignDao.insertCampaignHistory(
            CampaignHistoryCommand.created(
                campaignId,
                context.getUserId()
            )
        );

        return new CampaignCreateResponse(
            campaignId,
            "DRAFT",
            1L
        );
    }
}
```

### 24.6 업무 중복과 요청 중복

두 종류의 중복을 구분해야 합니다.

#### 요청 중복

사용자가 같은 등록 요청을 두 번 전송한 경우입니다.

```
Idempotency Key로 통제
```

#### 업무 중복

서로 다른 요청이지만 동일한 업무 데이터가 이미 존재하는 경우입니다.

```
캠페인명 + 기간 + 유형이 동일
→ 업무 중복검사
```

두 중복은 서로 다른 기준입니다.

### 24.7 Idempotency Key

요청 Header:

```
{
  "idempotencyKey": "CM-USER01-20260717-0001"
}
```

처리 흐름:

```
Idempotency Key 조회

미등록
→ PROCESSING 등록
→ 업무 처리
→ SUCCESS 저장

이미 SUCCESS
→ 기존 결과 반환

이미 PROCESSING
→ 중복 진행 오류 또는 대기정책

이미 FAIL
→ 재처리 정책에 따라 판단
```

### 24.8 DB Unique Constraint

애플리케이션 중복검사만으로 완전히 안전하지 않을 수 있습니다.

두 요청이 동시에 다음 순서로 실행될 수 있습니다.

```
요청 A: 중복 없음 확인
요청 B: 중복 없음 확인
요청 A: INSERT
요청 B: INSERT
```

따라서 업무적으로 반드시 유일해야 하는 값에는 DB Unique Constraint도 검토합니다.

```
애플리케이션 검증
+ DB 제약조건
```

두 계층을 함께 사용합니다.

### 24.9 ID 생성 방식

대표적인 방식:

| 방식 | 설명 |
| --- | --- |
| DB Sequence | Oracle Sequence 사용 |
| UUID | 전역 고유 문자열 |
| 채번 테이블 | 업무규칙에 맞는 번호 생성 |
| 날짜+순번 | 업무용 의미 있는 키 |
| 별도 ID 서비스 | 중앙 발급 서비스 |

초보 개발자가 다음처럼 ID를 만들면 안 됩니다.

```
String id =
    "CMP" + System.currentTimeMillis();
```

동시성, 길이, 운영 추적, 테스트 문제가 발생할 수 있습니다.

프로젝트 표준 ID 생성기를 사용합니다.

### 24.10 등록 SQL

```
<insert id="insertCampaign">

    INSERT INTO CM_CAMPAIGN_MASTER (
        CAMPAIGN_ID,
        CAMPAIGN_NAME,
        CAMPAIGN_TYPE,
        CAMPAIGN_STATUS,
        START_DATE,
        END_DATE,
        VERSION_NO,
        DELETE_YN,
        CREATE_USER_ID,
        CREATE_DTM,
        UPDATE_USER_ID,
        UPDATE_DTM
    ) VALUES (
        #{campaignId},
        #{campaignName},
        #{campaignType},
        'DRAFT',
        #{startDate},
        #{endDate},
        1,
        'N',
        #{createUserId},
        CURRENT_TIMESTAMP,
        #{createUserId},
        CURRENT_TIMESTAMP
    )

</insert>
```

### 24.11 등록 건수 확인

```
int inserted =
    campaignDao.insertCampaign(command);

if (inserted != 1) {
    throw new SystemException(
        "E-CM-DB-0002",
        "캠페인 등록 결과가 올바르지 않습니다."
    );
}
```

한 건 등록이 예상되는데 0건 또는 여러 건으로 처리되었다면 오류입니다.

### 24.12 등록 이력

중요 업무 데이터는 등록 시점부터 이력을 남길 수 있습니다.

```
CAMPAIGN_ID
ACTION_TYPE = CREATE
BEFORE_DATA = null
AFTER_DATA
CHANGE_USER_ID
CHANGE_DTM
TRACE_ID
```

이력에는 개인정보와 대용량 전문 원문을 무조건 저장하지 않습니다.

### 24.13 등록 실패와 Rollback

다음 처리 중 이력 등록이 실패했다고 가정합니다.

```
캠페인 기본정보 INSERT 성공
캠페인 이력 INSERT 실패
```

두 작업이 같은 트랜잭션이라면 전체가 Rollback됩니다.

```
캠페인 기본정보도 취소
이력도 없음
```

이력 기록 실패를 무시할 수 있는지는 업무·감사 요구에 따라 결정합니다.

중요 변경 이력이라면 함께 Rollback하는 것이 일반적으로 안전합니다.

### 제24장 요약

```
등록 거래는
중복 요청과 업무 중복을 모두 검토한다.

애플리케이션 검증만 믿지 말고
DB 제약조건을 함께 사용한다.

등록과 필수 이력은
하나의 트랜잭션으로 처리한다.
```

## 제25장. 변경 거래와 동시성 제어

### 25.1 변경 거래의 위험

사용자 A와 B가 같은 캠페인을 동시에 열었습니다.

```
현재 Version = 3

사용자 A
→ 캠페인명 변경
→ 저장

사용자 B
→ 종료일 변경
→ 저장
```

버전 검증이 없으면 B의 저장이 A의 변경을 덮어쓸 수 있습니다.

이를 갱신 손실이라고 합니다.

### 25.2 요청 DTO

```
public record CampaignUpdateRequest(

    @NotBlank
    String campaignId,

    @NotBlank
    @Size(max = 100)
    String campaignName,

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    String startDate,

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    String endDate,

    @Min(1)
    long version

) {
}
```

조회 응답에서 받은 version을 변경 요청에 다시 전달합니다.

### 25.3 변경 처리 흐름

```
요청 검증
→ 변경 권한 검증
→ 현재 데이터 조회
→ 대상 존재 확인
→ 변경 가능한 상태 확인
→ 요청 Version과 현재 Version 비교
→ 변경 SQL 실행
→ 변경 건수 확인
→ 변경 이력 저장
→ 성공 응답
```

### 25.4 변경 가능한 상태 검증

캠페인 상태에 따라 변경 가능 여부가 다를 수 있습니다.

```
DRAFT
→ 변경 가능

APPROVED
→ 일부 항목만 변경

RUNNING
→ 일반 변경 금지

COMPLETED
→ 변경 금지

CANCELLED
→ 변경 금지
```

Rule 예:

```
public void validateUpdatableStatus(
        String status) {

    if (!"DRAFT".equals(status)) {
        throw new BusinessException(
            "E-CM-CAM-0010",
            "작성 중인 캠페인만 변경할 수 있습니다."
        );
    }
}
```

### 25.5 낙관적 잠금

Update SQL에서 Version을 조건으로 사용합니다.

```
<update id="updateCampaign">

    UPDATE CM_CAMPAIGN_MASTER
       SET CAMPAIGN_NAME = #{campaignName},
           START_DATE = #{startDate},
           END_DATE = #{endDate},
           VERSION_NO = VERSION_NO + 1,
           UPDATE_USER_ID = #{updateUserId},
           UPDATE_DTM = CURRENT_TIMESTAMP
     WHERE CAMPAIGN_ID = #{campaignId}
       AND VERSION_NO = #{version}
       AND DELETE_YN = 'N'

</update>
```

정상 변경:

```
업데이트 건수 = 1
```

동시 변경 충돌:

```
업데이트 건수 = 0
```

### 25.6 변경 충돌 처리

```
int updated =
    campaignDao.updateCampaign(command);

if (updated == 0) {
    throw new BusinessException(
        "E-CM-CAM-0011",
        "다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요."
    );
}
```

사용자에게 무조건 시스템 오류를 보여주지 않습니다.

동시 변경은 예상 가능한 업무 상황이므로 업무 오류로 처리할 수 있습니다.

### 25.7 비관적 잠금

DB에서 다음처럼 행을 잠글 수도 있습니다.

```
SELECT ...
  FROM CM_CAMPAIGN_MASTER
 WHERE CAMPAIGN_ID = :campaignId
   FOR UPDATE
```

장점:

- 변경 처리 동안 다른 변경을 대기시킴
- 즉시 강한 일관성 확보
단점:

- Lock 대기 증가
- 교착상태 가능
- 긴 트랜잭션 시 성능 저하
- Timeout 위험
일반 화면 수정에는 낙관적 잠금을 우선 검토하고, 반드시 직렬 처리가 필요한 짧은 구간에서 비관적 잠금을 제한적으로 사용합니다.

### 25.8 변경 이력

변경 전후 값을 기록합니다.

```
BEFORE
campaignName = 기존 캠페인
endDate = 20260831

AFTER
campaignName = 우수고객 캠페인
endDate = 20260915
```

감사 목적의 이력은 누가, 언제, 왜 변경했는지 포함해야 합니다.

| 항목 | 설명 |
| --- | --- |
| 대상 ID | 캠페인 ID |
| 변경유형 | UPDATE |
| 변경 전 | 이전 값 |
| 변경 후 | 신규 값 |
| 변경자 | 사용자 ID |
| 변경지점 | 지점 ID |
| 변경시각 | 서버 기준시각 |
| TraceId | 거래 추적값 |
| 변경사유 | 업무상 필요 시 |

### 25.9 수정자와 수정시각

클라이언트가 보내는 수정자와 수정시각을 사용하지 않습니다.

금지:

```
{
  "updateUserId": "admin",
  "updateDtm": "20991231235959"
}
```

권장:

```
updateUserId
= TransactionContext 사용자

updateDtm
= DB 또는 서버 현재시각
```

### 25.10 변경하지 않은 항목

전체 객체를 받아 모든 컬럼을 Update하면 사용자가 보지 못한 값까지 덮어쓸 수 있습니다.

대안:

- 변경 가능한 필드만 Request DTO에 포함
- 업무별 변경 ServiceId 분리
- Patch 방식은 허용 필드를 엄격히 통제
- 변경 전 현재 데이터 검증
예:

```
CM.Campaign.updatePeriod
CM.Campaign.updateTargetCondition
CM.Campaign.updateMessage
```

복잡한 상태 변경은 하나의 거대한 update보다 목적별 거래로 나누는 것이 안전합니다.

### 제25장 요약

```
변경 거래는
현재 상태와 Version을 검증한다.

낙관적 잠금은
다른 사용자의 변경사항을
조용히 덮어쓰는 문제를 방지한다.
```

## 제26장. 삭제와 사용중지 설계

### 26.1 삭제는 가장 신중해야 한다

DB 행을 실제로 삭제하면 다음 정보가 사라질 수 있습니다.

- 업무 이력
- 감사 증적
- 다른 테이블의 참조
- 장애 분석 정보
- 통계 기준정보
- 복구 가능성
따라서 기업 시스템에서는 물리 삭제보다 논리 삭제나 상태 변경을 많이 사용합니다.

### 26.2 삭제 방식 비교

| 방식 | 처리 | 장점 | 단점 |
| --- | --- | --- | --- |
| 물리 삭제 | DELETE | 데이터 완전 제거 | 복구·감사 어려움 |
| 논리 삭제 | DELETE_YN='Y' | 복구·이력 가능 | 모든 조회에 조건 필요 |
| 상태 변경 | STATUS='INACTIVE' | 업무 의미 명확 | 상태모델 관리 필요 |
| 유효기간 종료 | 종료일 갱신 | 기간 데이터에 적합 | 조회조건 복잡 |
| 별도 보관 | Archive 이동 | 운영 DB 경량화 | 구조와 운영 복잡 |

### 26.3 캠페인 사용중지 거래

ServiceId:

```
CM.Campaign.deactivate
```

단순 delete보다 업무 의미가 명확합니다.

처리 흐름:

```
대상 조회
→ 존재 확인
→ 중지 가능 상태 확인
→ Version 확인
→ 상태를 INACTIVE로 변경
→ 중지 이력 저장
→ 감사로그
```

### 26.4 논리 삭제 SQL

```
<update id="deactivateCampaign">

    UPDATE CM_CAMPAIGN_MASTER
       SET CAMPAIGN_STATUS = 'INACTIVE',
           DELETE_YN = 'Y',
           VERSION_NO = VERSION_NO + 1,
           UPDATE_USER_ID = #{updateUserId},
           UPDATE_DTM = CURRENT_TIMESTAMP
     WHERE CAMPAIGN_ID = #{campaignId}
       AND VERSION_NO = #{version}
       AND DELETE_YN = 'N'

</update>
```

### 26.5 모든 조회의 삭제조건

논리 삭제를 사용하면 모든 조회에 다음 조건이 필요합니다.

```
DELETE_YN = 'N'
```

이 조건이 누락되면 삭제된 데이터가 다시 화면에 보일 수 있습니다.

자동검증 또는 공통 조회 View를 사용할 수 있습니다.

### 26.6 물리 삭제가 필요한 경우

다음 경우에는 물리 삭제를 검토할 수 있습니다.

- 임시 업로드 데이터
- 업무가 확정되기 전 Draft 보조 데이터
- 법적 보유기간이 만료된 개인정보
- 테스트 데이터
- 별도 Archive가 완료된 데이터
- 생성 직후 전체 거래가 Rollback되는 데이터
단, 개인정보 파기는 별도의 보안·법무·데이터 폐기 정책을 따라야 합니다.

### 26.7 참조 데이터 확인

캠페인을 삭제하기 전에 다음 데이터가 참조하는지 확인합니다.

```
캠페인 대상자
캠페인 메시지
실행 이력
성과 데이터
감사 이력
```

참조 데이터가 존재한다면 다음 중 하나를 선택해야 합니다.

- 삭제 금지
- 하위 데이터 함께 비활성화
- Archive 후 삭제
- 상태만 종료
- 업무상 취소 거래 수행
DB Cascade Delete를 무분별하게 사용하면 대량 데이터가 의도하지 않게 삭제될 수 있습니다.

### 26.8 삭제 권한과 승인

중요 데이터의 삭제·중지에는 일반 변경권한보다 강한 권한을 적용할 수 있습니다.

```
CM_CAMPAIGN_UPDATE
≠ CM_CAMPAIGN_DELETE
```

필요하면 다음 절차를 적용합니다.

```
삭제 요청
→ 승인자 승인
→ 실행
→ 감사로그
```

### 제26장 요약

```
삭제는 단순 DELETE가 아니다.

복구·감사·참조관계를 고려하여
논리 삭제, 상태 변경, Archive 중
적절한 방식을 선택한다.
```

## 제27장. 트랜잭션과 Rollback 이해하기

### 27.1 트랜잭션이란 무엇인가요?

트랜잭션은 여러 DB 작업을 하나의 작업처럼 처리하는 기능입니다.

예:

```
캠페인 기본정보 등록
캠페인 대상조건 등록
캠페인 이력 등록
```

세 작업이 모두 성공해야 하나의 캠페인이 완성됩니다.

```
전체 성공
→ Commit

하나라도 실패
→ Rollback
```

### 27.2 은행 창구 비유

계좌이체를 생각해 봅시다.

```
A 계좌 출금
→ B 계좌 입금
```

출금은 성공했지만 입금이 실패하면 안 됩니다.

둘을 하나의 트랜잭션으로 묶어야 합니다.

CRUD에서도 같은 원리가 적용됩니다.

### 27.3 트랜잭션 위치

권장:

```
@Transactional
public CampaignCreateResponse create(...) {
    return campaignService.create(...);
}
```

Facade가 유스케이스 전체의 트랜잭션 경계를 가집니다.

금지:

```
Handler에 @Transactional
Rule에 @Transactional
Mapper에 @Transactional
```

### 27.4 조회 트랜잭션

```
@Transactional(readOnly = true)
public CampaignDetailResponse selectDetail(...) {
    ...
}
```

조회 거래는 읽기 전용임을 명확히 표현합니다.

조회 중 데이터를 변경하는 SQL을 실행하지 않습니다.

### 27.5 Rollback 대상

일반적으로 RuntimeException 계열의 예외는 Rollback 대상이 됩니다.

프로젝트의 BusinessException 상속구조와 Transaction 설정을 확인해야 합니다.

다음처럼 업무 오류가 발생했는데 Rollback되지 않는 잘못된 구조가 생길 수 있습니다.

```
기본정보 INSERT 성공
Rule에서 BusinessException 발생
Rollback 안 됨
```

공통 예외정책에서 업무 예외와 시스템 예외의 Rollback 기준을 명확히 해야 합니다.

### 27.6 예외를 잡아 숨기지 않는다

금지:

```
@Transactional
public void createCampaign(...) {
    try {
        campaignDao.insertCampaign(...);
        historyDao.insertHistory(...);
    } catch (Exception e) {
        log.error("오류", e);
    }
}
```

예외를 잡고 다시 던지지 않으면 트랜잭션이 정상 종료되어 Commit될 수 있습니다.

권장:

```
catch (DataAccessException e) {
    throw new SystemException(
        "E-CM-DB-0001",
        "캠페인 등록 중 DB 오류가 발생했습니다.",
        e
    );
}
```

### 27.7 외부 시스템과 DB 트랜잭션

다음 흐름은 로컬 DB 트랜잭션만으로 완전히 Rollback하기 어렵습니다.

```
DB 캠페인 등록 성공
→ 외부 메시지 시스템 호출 성공
→ DB 이력 등록 실패
```

DB는 Rollback할 수 있어도 이미 외부 시스템에 전송된 메시지는 자동 취소되지 않습니다.

대안:

- 외부 호출을 Commit 이후 수행
- Outbox Pattern
- 비동기 이벤트
- 보상 거래
- 상태 기반 재처리
초보 개발자가 외부 호출을 DB 트랜잭션 안에 무조건 포함하면 긴 Lock과 장애 전파가 발생할 수 있습니다.

### 27.8 긴 트랜잭션 피하기

금지 구조:

```
트랜잭션 시작
→ 외부 API 10초 대기
→ 파일 생성
→ 사용자 입력 대기
→ DB Update
→ Commit
```

문제:

- DB Connection 장기 점유
- Lock 장기 유지
- Timeout 증가
- 다른 거래 대기
- Rollback 비용 증가
트랜잭션 내부에는 필요한 DB 작업만 짧게 유지합니다.

### 27.9 일부 성공 허용

모든 유스케이스가 전체 Rollback만 사용하는 것은 아닙니다.

예:

```
고객 100명에게 알림 발송
93명 성공
7명 실패
```

이 경우 전체 Rollback보다 개별 결과 기록과 재처리가 적절할 수 있습니다.

```
온라인 단건 변경
→ 전체 원자성

대량 처리
→ 건별 상태와 재처리
```

업무 특성에 맞게 결정합니다.

### 27.10 트랜잭션 테스트

| 시나리오 | 기대 결과 |
| --- | --- |
| 기본정보·이력 모두 성공 | Commit |
| 기본정보 실패 | 전체 Rollback |
| 이력 저장 실패 | 전체 Rollback |
| Rule 오류 | 변경 없음 |
| 동시성 충돌 | 변경 없음 |
| DB Timeout | Rollback |
| 외부 호출 실패 | 설계된 보상·재처리 |

### 제27장 요약

```
트랜잭션은
여러 DB 작업을 하나의 업무로 묶는다.

Facade에 경계를 두고,
예외를 숨기지 않으며,
트랜잭션 시간을 짧게 유지한다.
```

## 제28장. SQL 품질과 성능 기준

### 28.1 SQL은 동작만 하면 끝이 아니다

개발환경에 데이터가 100건 있을 때 빠른 SQL도 운영환경에서 1억 건을 처리하면 느릴 수 있습니다.

SQL 검토 시 다음을 확인해야 합니다.

```
조회 건수
검색조건
인덱스
Join 방식
정렬
전체 건수
페이징
Lock
Timeout
실행계획
```

### 28.2 필요한 컬럼만 조회

금지:

```
SELECT *
  FROM CM_CAMPAIGN_MASTER
```

권장:

```
SELECT CAMPAIGN_ID,
       CAMPAIGN_NAME,
       CAMPAIGN_STATUS,
       START_DATE,
       END_DATE
  FROM CM_CAMPAIGN_MASTER
```

### 28.3 조건 없는 전체 조회 금지

```
SELECT CAMPAIGN_ID,
       CAMPAIGN_NAME
  FROM CM_CAMPAIGN_MASTER
```

온라인 화면에서 전체 데이터를 조회하면 안 됩니다.

최소한 다음 중 하나가 필요합니다.

- 검색조건
- 기간조건
- 상태조건
- 페이징
- 최대 건수
- Top N

### 28.4 함수 사용과 인덱스

다음 SQL은 인덱스 사용에 불리할 수 있습니다.

```
WHERE SUBSTR(CREATE_DTM, 1, 8) = :baseDate
```

가능하면 범위조건을 사용합니다.

```
WHERE CREATE_DTM >= :startDtm
  AND CREATE_DTM <  :endDtm
```

실제 실행계획은 DBA와 확인합니다.

### 28.5 Leading Wildcard

```
WHERE CAMPAIGN_NAME LIKE '%' || :name || '%'
```

앞에 %가 있으면 일반 인덱스를 활용하기 어려울 수 있습니다.

대량 데이터에서는 다음을 검토합니다.

- 앞부분 일치 검색
- 전문검색
- 별도 검색 인덱스
- 조회기간 제한
- 필수 추가조건

### 28.6 대량 IN 조건

```
WHERE CUSTOMER_NO IN (...)
```

목록이 수천·수만 건이면 SQL 길이와 성능 문제가 생깁니다.

대안:

- 임시 테이블
- Table Type
- Batch 분할
- Join 대상 적재
- 별도 대량처리

### 28.7 Row Count 검증

변경 SQL에서는 영향 건수를 확인합니다.

```
단건 Update 예상
→ 1건

0건
→ 존재하지 않음 또는 버전 충돌

2건 이상
→ 조건 오류 또는 데이터 정합성 문제
```

### 28.8 Query Timeout

ServiceId Timeout만 설정하면 DB SQL이 즉시 중단되지 않을 수 있습니다.

다음 Timeout의 관계를 함께 설계합니다.

```
MyBatis Query Timeout
DB Statement Timeout
DB Connection Timeout
ServiceId Timeout
Gateway Timeout
화면 Timeout
```

하위 Timeout이 상위 Timeout보다 짧아야 원인을 명확하게 반환할 수 있습니다.

### 28.9 Connection Pool

느린 SQL은 DB만 느리게 하지 않습니다.

```
느린 SQL
→ Connection 장기 점유
→ Hikari Pool 부족
→ 다른 거래 Connection 대기
→ Tomcat Thread 대기
→ 전체 응답 지연
```

따라서 SQL 성능은 WAS Thread와 DB Pool 용량에도 영향을 줍니다.

### 28.10 SQL 로그

운영 로그에는 다음 정보를 기록할 수 있습니다.

```
TraceId
ServiceId
Mapper ID
SQL ID
처리시간
조회건수
오류코드
```

개인정보 Parameter 원문과 SQL 전체를 무조건 기록하지 않습니다.

### 28.11 Slow SQL 기준

예:

```
정상
< 300ms

주의
300ms ~ 1000ms

위험
> 1000ms
```

실제 기준은 거래 특성·DB 구조·운영 목표에 따라 결정합니다.

Slow SQL은 ServiceId, TraceId와 연결되어야 영향 화면을 찾을 수 있습니다.

### 28.12 SQL 품질 Gate

| 항목 | 자동·수동 검증 |
| --- | --- |
| SELECT * 사용 | 자동검출 |
| ${} 사용 | 자동검출·승인 |
| 조건 없는 Update/Delete | 자동검출 |
| Mapper Namespace | 자동검증 |
| SQL ID 중복 | 자동검증 |
| 신규 SQL 실행계획 | DBA 검토 |
| 페이징 없는 목록 | 설계·테스트 |
| 최대 조회건수 | 통합 테스트 |
| Index 적정성 | DBA 검토 |
| Slow SQL 기준 | 성능시험 |

### 제28장 요약

```
SQL은 DB 내부 코드가 아니다.

ServiceId의 응답시간,
DB Pool, Tomcat Thread,
운영 장애에 직접 영향을 준다.

필요한 데이터만
제한된 범위로 조회한다.
```

## 3. CRUD 전체 목표 아키텍처

```
[화면 이벤트]
      │
      ▼
[표준 요청]
 businessCode
 serviceId
 transactionCode
 idempotencyKey
 version
      │
      ▼
[TCF / STF]
 인증·권한
 거래통제
 Timeout
 중복방지
 거래로그
      │
      ▼
[Handler]
 ServiceId 분기
 DTO 변환
      │
      ▼
[Facade]
 트랜잭션 경계
      │
      ▼
[Service]
 업무 처리 순서
      ├────────────┐
      ▼            ▼
   [Rule]         [DAO]
 상태·권한          │
 중복·동시성         ▼
                [Mapper]
                    │
                    ▼
                  [DB]
       Master / History / Idempotency
                    │
                    ▼
                  [ETF]
          성공·업무오류·시스템오류
```

## 4. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| Service Catalog | ServiceId, 거래코드, 상태, Timeout |
| Request DTO | 업무 입력, Page, Version |
| Rule | 상태전이, 중복, 권한, 날짜규칙 |
| Facade | 읽기·쓰기 트랜잭션 |
| DAO | Mapper 호출과 DB 예외 |
| Mapper | Select·Insert·Update·논리 삭제 |
| Master Table | 현재 업무 상태 |
| History Table | 변경 전후 이력 |
| Idempotency Table | 요청 중복 상태 |
| 거래로그 | 성공·실패·처리시간 |
| 감사로그 | 조회·변경 사용자와 대상 |

## 5. 책임 경계와 RACI

| 활동 | AA | DEV | DA/DBA | FW | OM | SEC | QA | OPS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CRUD 거래 분리 | A | R | C | C | C | C | C | I |
| 데이터 소유권 | A | C | R | I | I | C | I | I |
| DTO·Validation | C | R | I | C | I | C | C | I |
| 트랜잭션 설계 | A | R | C | C | I | I | C | I |
| 동시성 정책 | A | R | C | C | I | I | C | I |
| SQL 작성 | C | R | C | I | I | I | C | I |
| 실행계획 검토 | C | C | A/R | I | I | I | C | I |
| 감사·이력 | C | R | C | I | C | A | C | C |
| OM 등록 | C | C | I | I | A/R | C | C | C |
| 성능시험 | C | C | C | C | I | I | A/R | C |
| 운영 인수 | C | C | C | C | C | C | C | A/R |

## 6. 정상 처리 흐름

### 6.1 목록 조회

```
화면 검색
→ 검색조건 검증
→ Page 크기 제한
→ 정렬 화이트리스트
→ Count·목록 SQL
→ 빈 목록 또는 결과 반환
```

### 6.2 신규 등록

```
등록 요청
→ 권한·업무규칙
→ Idempotency 확인
→ 업무 중복 확인
→ ID 발급
→ Master INSERT
→ History INSERT
→ Commit
```

### 6.3 변경

```
변경 요청
→ 대상 조회
→ 상태 검증
→ Version 검증
→ UPDATE
→ 영향 건수 확인
→ History INSERT
→ Commit
```

### 6.4 사용중지

```
중지 요청
→ 참조·상태 검증
→ Version 검증
→ 상태·DELETE_YN 변경
→ 이력 저장
→ 감사로그
```

## 7. 오류·Timeout·장애 흐름

| 오류 | 처리 |
| --- | --- |
| 필수 검색조건 없음 | 요청 오류 |
| 최대 조회기간 초과 | 업무 오류 |
| Page 크기 초과 | 요청 오류 또는 서버 보정 |
| 등록 중복 | 업무 오류 |
| 동일 요청 재전송 | 기존 결과 또는 중복 차단 |
| 변경 대상 없음 | 업무 오류 |
| Version 불일치 | 동시 변경 업무 오류 |
| 변경 불가 상태 | 업무 오류 |
| Update 0건 | 충돌·대상 없음 판단 |
| Update 2건 이상 | 시스템·데이터 오류 |
| SQL Timeout | Rollback·Timeout 응답 |
| Connection 획득 실패 | 시스템 오류 |
| 이력 저장 실패 | 필수 이력이면 전체 Rollback |
| 감사로그 실패 | 정책에 따라 Rollback 또는 경보 |

## 8. 정상 예시

```
ServiceId
CM.Campaign.update

요청
campaignId = CMP001
version = 3

현재 DB
version = 3
status = DRAFT

Update
WHERE CAMPAIGN_ID = CMP001
  AND VERSION_NO = 3

결과
1건 변경
version = 4

이력
변경 전·후 기록

응답
SUCCESS
```

## 9. 금지 예시

### 9.1 하나의 ServiceId로 모든 CRUD 처리

```
CM.Campaign.manage
actionType = SELECT | CREATE | UPDATE | DELETE
```

### 9.2 목록 전체조회

```
SELECT *
  FROM CM_CAMPAIGN_MASTER
```

### 9.3 버전 없는 변경

```
UPDATE CM_CAMPAIGN_MASTER
   SET CAMPAIGN_NAME = #{campaignName}
 WHERE CAMPAIGN_ID = #{campaignId}
```

### 9.4 조건 없는 삭제

```
DELETE FROM CM_CAMPAIGN_MASTER
```

### 9.5 예외 숨김

```
try {
    dao.update(...);
} catch (Exception e) {
    log.error("오류", e);
}
```

### 9.6 클라이언트 수정자 신뢰

```
{
  "updateUserId": "admin"
}
```

## 10. 연계 규칙

다른 업무 데이터를 CRUD할 때는 데이터 소유 업무의 공개 계약을 사용합니다.

```
동일 도메인
→ 내부 Service·DAO 사용

동일 WAR의 다른 도메인
→ 승인된 Application Contract

다른 WAR
→ ServiceId 기반 TCF·EAI 호출

대량 비동기
→ 이벤트·메시지·Batch
```

금지:

```
다른 업무 WAR의 Mapper Import
다른 업무 테이블 직접 Update
DB Link를 이용한 무단 변경
```

## 11. 데이터 및 상태관리

### 11.1 상태 전이

캠페인 예:

```
DRAFT
  ↓ 승인
APPROVED
  ↓ 실행
RUNNING
  ↓ 완료
COMPLETED

DRAFT
  ↓ 취소
CANCELLED
```

허용되지 않는 상태 전이를 Rule에서 차단합니다.

```
COMPLETED → DRAFT
금지
```

### 11.2 공통 관리 컬럼

| 컬럼 | 설명 |
| --- | --- |
| CREATE_USER_ID | 생성자 |
| CREATE_DTM | 생성시각 |
| UPDATE_USER_ID | 최종 변경자 |
| UPDATE_DTM | 최종 변경시각 |
| VERSION_NO | 낙관적 잠금 |
| DELETE_YN | 논리 삭제 |
| CAMPAIGN_STATUS | 업무 상태 |

프로젝트 DB 표준 컬럼명을 우선합니다.

## 12. 성능·용량·확장성

| 영역 | 기준 |
| --- | --- |
| 단건 조회 | 인덱스 기반 한 건 조회 |
| 목록 조회 | 기본 20건, 최대 100건 |
| 조회기간 | 업무별 최대기간 |
| Count SQL | 필요 여부 검토 |
| 변경 트랜잭션 | 짧게 유지 |
| 대량 등록 | 온라인 반복 대신 Batch |
| 대량 삭제 | 분할 처리·Archive |
| 외부 호출 | DB 트랜잭션과 분리 검토 |
| Slow SQL | ServiceId·Mapper ID 연결 |
| DB Pool | 장기 점유 방지 |

## 13. 보안·개인정보·감사

```
조회권한과 변경권한을 분리한다.

개인정보 목록은 필요한 컬럼만 반환한다.

등록·변경·삭제는 감사 대상 여부를 검토한다.

사용자 ID와 지점 ID는 인증 Context를 사용한다.

동적 SQL은 Binding과 화이트리스트를 적용한다.

변경 전후 이력에 민감정보 원문을 과도하게 저장하지 않는다.
```

## 14. 운영·모니터링·장애 대응

운영자는 다음 기준으로 CRUD 거래를 확인할 수 있어야 합니다.

```
ServiceId별 호출 건수
성공·업무오류·시스템오류
평균·p95 응답시간
Timeout 건수
중복 요청 건수
동시성 충돌 건수
Mapper SQL 처리시간
영향 행 수
DB Pool 대기
Rollback 건수
```

변경 거래에서 Update 건수가 예상과 다르면 별도 경보를 검토합니다.

## 15. 자동검증 및 품질 Gate

| Gate | 기준 |
| --- | --- |
| ServiceId | CRUD 기능 분리 |
| 패키지 | 업무코드·도메인·계층 |
| 조회 | 페이징·최대건수 |
| SQL | SELECT * 금지 |
| Binding | 사용자값 ${} 금지 |
| Update | 조건과 Version 검토 |
| Delete | 조건 없는 삭제 금지 |
| 트랜잭션 | Facade 적용 |
| 중복방지 | 등록·변경 정책 |
| 이력 | 중요 변경 이력 |
| 테스트 | 정상·오류·동시성·Rollback |
| OM | Timeout·권한·감사 등록 |

## 16. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CRUD-001 | 단건 정상 조회 | 상세 반환 |
| CRUD-002 | 없는 ID 조회 | 업무 오류 |
| CRUD-003 | 목록 0건 | 빈 목록 성공 |
| CRUD-004 | Page Size 100 초과 | 차단·보정 |
| CRUD-005 | 정렬 컬럼 변조 | 기본 정렬·오류 |
| CRUD-006 | 정상 등록 | Master·History 저장 |
| CRUD-007 | 동일 Idempotency Key | 중복 반영 없음 |
| CRUD-008 | 업무 중복 등록 | 업무 오류 |
| CRUD-009 | 정상 변경 | Version 증가 |
| CRUD-010 | 오래된 Version 변경 | 충돌 오류 |
| CRUD-011 | 변경 불가 상태 | 업무 오류 |
| CRUD-012 | 이력 저장 실패 | 전체 Rollback |
| CRUD-013 | 정상 사용중지 | 논리 삭제 |
| CRUD-014 | 참조 데이터 존재 | 중지·삭제 정책 적용 |
| CRUD-015 | SQL Timeout | Rollback |
| CRUD-016 | Connection Pool 부족 | 시스템 오류·경보 |
| CRUD-017 | Update 2건 이상 | 데이터 정합성 오류 |
| CRUD-018 | 개인정보 로그 검사 | 원문 미노출 |

## 17. 제4부 체크리스트

### 17.1 조회

| 점검 항목 | 확인 |
| --- | --- |
| 단건과 목록 ServiceId가 분리되었는가? | □ |
| 조회 결과 없음 기준이 정의되었는가? | □ |
| 목록에 페이징이 적용되었는가? | □ |
| 최대 조회건수가 있는가? | □ |
| 정렬 컬럼이 화이트리스트인가? | □ |
| N+1 문제가 없는가? | □ |

### 17.2 등록

| 점검 항목 | 확인 |
| --- | --- |
| Idempotency 필요 여부를 검토했는가? | □ |
| 업무 중복 기준이 있는가? | □ |
| DB Unique Constraint가 필요한가? | □ |
| 표준 ID 생성기를 사용하는가? | □ |
| 등록 이력이 필요한가? | □ |
| 예상 Insert 건수를 확인하는가? | □ |

### 17.3 변경

| 점검 항목 | 확인 |
| --- | --- |
| 현재 상태를 검증하는가? | □ |
| Version을 이용하는가? | □ |
| Update 0건을 충돌로 처리하는가? | □ |
| 수정자는 Context에서 가져오는가? | □ |
| 변경 전후 이력을 기록하는가? | □ |
| 변경 가능 필드가 제한되어 있는가? | □ |

### 17.4 삭제

| 점검 항목 | 확인 |
| --- | --- |
| 물리·논리 삭제를 결정했는가? | □ |
| 참조 데이터 영향을 확인했는가? | □ |
| 별도 삭제권한이 있는가? | □ |
| 삭제 이력이 남는가? | □ |
| 모든 조회에 삭제조건이 적용되는가? | □ |

### 17.5 트랜잭션

| 점검 항목 | 확인 |
| --- | --- |
| Facade에 트랜잭션이 있는가? | □ |
| 조회는 readOnly인가? | □ |
| 예외를 숨기지 않는가? | □ |
| 필수 이력 실패 시 Rollback되는가? | □ |
| 외부 호출이 트랜잭션을 길게 만들지 않는가? | □ |

### 17.6 SQL

| 점검 항목 | 확인 |
| --- | --- |
| SELECT *를 사용하지 않는가? | □ |
| 조건 없는 Update·Delete가 없는가? | □ |
| Parameter Binding을 사용하는가? | □ |
| 인덱스 검토가 되었는가? | □ |
| Query Timeout이 있는가? | □ |
| SQL 처리시간을 추적할 수 있는가? | □ |

## 18. 변경·호환성·폐기 관리

### 18.1 DTO 변경

기존 필드를 제거하거나 의미를 바꾸면 화면과 연계 시스템에 영향을 줍니다.

```
필드 추가
→ 하위 호환 가능 여부 검토

필드 삭제
→ 계약 버전 변경 검토

필드 의미 변경
→ 신규 필드 또는 신규 ServiceId 검토
```

### 18.2 DB 컬럼 변경

```
DB 컬럼 변경
→ Mapper
→ DAO
→ Response DTO
→ ServiceId
→ 화면
→ 테스트
```

영향 매트릭스를 확인해야 합니다.

### 18.3 삭제 거래 폐기

물리 삭제 기능을 논리 삭제로 변경하거나 ServiceId를 폐기할 때는 다음 상태를 관리합니다.

```
ACTIVE
→ DEPRECATED
→ DISABLED
→ REMOVED
```

기존 화면·연계가 더 이상 호출하지 않는지 확인한 후 제거합니다.

## 19. 시사점

### 19.1 핵심 아키텍처 판단

CRUD 개발의 핵심은 SQL을 빨리 작성하는 것이 아닙니다.

```
조회
= 데이터 범위와 성능 통제

등록
= 중복과 원자성 통제

변경
= 상태와 동시성 통제

삭제
= 데이터 생명주기와 감사 통제
```

### 19.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 목록 전체조회 | DB·WAS 부하 |
| 동적 정렬값 직접 삽입 | SQL Injection |
| 등록 중복방지 없음 | 중복 데이터 |
| Version 없는 변경 | 갱신 손실 |
| 물리 삭제 남용 | 복구·감사 불가 |
| 예외 숨김 | 부분 Commit |
| 긴 트랜잭션 | Lock·Pool 고갈 |
| 다른 업무 테이블 직접 변경 | 데이터 소유권 훼손 |
| 이력 누락 | 변경 원인 추적 불가 |
| Timeout 불일치 | 상위 응답 종료 후 하위 작업 지속 |

### 19.3 우선 보완 과제

초보 개발자는 다음 순서로 연습하는 것이 좋습니다.

```
단건 조회
→ 페이징 목록
→ 단일 테이블 등록
→ Version 기반 변경
→ 논리 삭제
→ Master·History 트랜잭션
→ Idempotency
→ 외부 연동과 보상처리
```

### 19.4 중장기 발전 방향

CRUD 구현 능력은 이후 다음 영역으로 확장됩니다.

```
복합 조회
→ 도메인 조합
→ Cache
→ 이벤트 처리
→ 대량 Batch
→ CQRS·Read Model
→ 비동기 처리
→ 데이터 파티셔닝
→ 성능 튜닝
→ 자동 장애 진단
```

## 20. 마무리말

제4부에서는 CRUD를 단순한 SQL 작성이 아니라 데이터의 전체 생명주기를 통제하는 업무 거래로 살펴보았습니다.

핵심 기준은 다음과 같습니다.

```
조회는 제한하고,
등록은 중복을 막고,
변경은 충돌을 확인하고,
삭제는 복구와 감사를 고려한다.
```

프로그램 구조는 제3부와 동일합니다.

```
Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
```

하지만 CRUD 유형에 따라 내부 정책이 달라집니다.

```
조회
→ readOnly·페이징·마스킹

등록
→ Idempotency·중복검사·ID 발급

변경
→ 상태검사·Version·변경이력

삭제
→ 참조검사·논리삭제·승인
```

초보 개발자가 CRUD 기능을 구현할 때 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
이 데이터의 소유 업무는 어디인가?

조회 범위가 제한되어 있는가?

같은 등록 요청이 두 번 들어오면 어떻게 되는가?

다른 사용자가 먼저 변경했으면 어떻게 되는가?

삭제된 데이터는 복구할 수 있는가?

일부 SQL만 성공했을 때 Rollback되는가?

누가 무엇을 변경했는지 추적할 수 있는가?

운영자가 ServiceId와 SQL을 연결해 볼 수 있는가?
```

이 질문에 답할 수 있다면 단순 CRUD 개발을 넘어 운영 가능한 기업 업무 거래를 구현할 수 있습니다.

