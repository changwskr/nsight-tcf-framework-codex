# 샘플2 — AV 기존 모듈에 도메인 추가 + 페이징 목록 조회

> 시나리오: **이미 구현·등록된 av-service**(포트 8101, Sample 도메인 운영 중)에
> AssetValuation(자산평가) 도메인을 추가한다. 모듈 생성·Gradle 등록·tcf-ui
> 모듈 등록은 이미 완료 상태이므로 건드리지 않는다.
> 레퍼런스: av-service Sample 도메인(같은 모듈 내 최신 표준) + eb-service User 도메인

```text
너는 NSIGHT-TCF-FRAMEWORK의 애플리케이션 아키텍트이자 시니어 Java 개발자다.
기존 업무 모듈 av-service에 자산평가(AssetValuation) 도메인을 추가하라.
모듈 신규 생성이 아니다 — settings.gradle, build.gradle,
NsightAvServiceApplication, application*.yml, tcf-ui 모듈 등록은
이미 완료되어 있으므로 수정하지 마라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 고정 개발 입력정보 (임의 변경 금지)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| 구분            | 확정값                                |
|-----------------|---------------------------------------|
| 업무코드        | AV (기존)                             |
| 모듈            | av-service (기존 재사용)              |
| 추가 도메인     | AssetValuation                        |
| ServiceId       | AV.AssetValuation.inquiryList         |
| 거래코드        | AV-INQ-0002 (0001은 Sample이 사용 중) |
| Processing Type | INQUIRY (페이징 목록 조회)            |
| bootRun 포트    | 8101 (기존 — 변경 금지)               |
| DB 테이블       | AV_ASSET_VALUATION (설계 예시)        |
| 기준 도메인     | av-service Sample + eb-service User   |

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 구현 전 필수 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
다음 파일을 실제로 읽고 기존 Sample 도메인과 동일한 규약을 확인하라.
- av-service/src/main/java/com/nh/nsight/marketing/av/ 전체
  (Handler·Facade·Service·Rule·DTO·Dao·Mapper — 이 모듈의 현행 표준)
- av-service/src/main/resources/schema.sql, mapper/av/AvSampleMapper.xml
- eb-service EbUserRule·EbUserMapper.xml (search+count 이중 조회·
  동적 WHERE·OFFSET/FETCH 페이징의 원본 패턴)
기존 거래(AV.Sample.inquiry)가 깨지지 않아야 한다.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 추가 — AV_ASSET_VALUATION (설계 예시)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
기존 schema.sql 에 CREATE TABLE IF NOT EXISTS 로 추가하고
"설계 예시" 주석을 명시하라. 기존 AV_SAMPLE 정의는 수정 금지.

| 컬럼               | 타입          | 제약          | 설명         |
|--------------------|---------------|---------------|--------------|
| VALUATION_NO       | VARCHAR(20)   | PK            | 평가번호     |
| ASSET_ID           | VARCHAR(20)   | NOT NULL      | 자산번호     |
| CUSTOMER_ID        | VARCHAR(20)   |               | 고객번호     |
| VALUATION_AMOUNT   | NUMBER(15)    | NOT NULL      | 평가금액     |
| VALUATION_STATUS   | VARCHAR(10)   | 기본 'DONE'   | 평가상태     |
| VALUATION_BASE_DATE| VARCHAR(8)    | NOT NULL      | 평가기준일   |
| CREATED_AT         | TIMESTAMP     | 기본 현재시각 | 등록일시     |

시드 데이터 10건 이상 (페이징 2페이지 이상 검증용).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 추가 클래스 (기존 com.nh.nsight.marketing.av 아래)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
├── entry/handler/AvAssetValuationHandler — 신규 도메인 핸들러,
│     serviceIds()에 "AV.AssetValuation.inquiryList" 등록.
│     기존 AvSampleHandler는 수정 금지 (도메인당 핸들러 1개 원칙)
├── entry/facade/AvAssetValuationFacade — @Transactional(readOnly=true, timeout=5)
├── application/service/AvAssetValuationService —
│     rule 검증 → criteria → dao.searchValuations + dao.countValuations
│     (이중 조회) → Response 조립
├── application/rule/AvAssetValuationRule — validateInquiry
│     (pageNo≥1, pageSize 기본 15·최대 100,
│      valuationBaseDate는 있으면 yyyyMMdd 8자리 검증),
│     buildSearchCriteria(assetId·customerId·valuationStatus·기준일)
├── application/dto/assetvaluation/ —
│     AssetValuationInquiryRequest(fromMap) /
│     AssetValuationInquiryResponse(toMap: rows·totalCount·pageNo·pageSize·guid) /
│     AssetValuationSearchCriteria
├── persistence/dao/AvAssetValuationDao, persistence/mapper/AvAssetValuationMapper
├── persistence/dto/assetvaluation/AssetValuationRow
└── resources/mapper/av/AvAssetValuationMapper.xml —
    searchValuations(동적 WHERE <where><if>: ASSET_ID LIKE·CUSTOMER_ID =·
    VALUATION_STATUS =·VALUATION_BASE_DATE =, CREATED_AT DESC,
    OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY) +
    countValuations(동일 WHERE <sql refid> 공유)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 샘플 전문·UI (선택 반영)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- tcf-ui/src/main/resources/sample-requests/av-sample-inquiry.json 은
  BusinessModuleCatalog가 로드하는 필수 파일이므로 삭제·이름변경 금지.
  자산평가용 예문이 필요하면 av-asset-valuation-inquiry.json 을 별도 추가.
- 화면이 필요하면 tcf-ui/static/av/sample-list.html 과
  _shared/av-admin.js 패턴을 따라 valuation-list.html 을 추가
  (av-admin.js TX 객체에 거래 정의 추가 방식 검토 후 보고).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. gradle :av-service:compileJava 성공
2. gradle :av-service:bootRun — 8101 기동
3. 회귀: 기존 AV.Sample.inquiry 호출 → 여전히 S0000
4. 신규: POST http://localhost:8101/online —
   header(전체 필드, serviceId=AV.AssetValuation.inquiryList,
   transactionCode=AV-INQ-0002) + body{pageNo:1, pageSize:5}
   → S0000, rows 5건, totalCount=시드 건수
5. 필터: valuationStatus·assetId LIKE 조건 반영 확인
6. 페이징: pageNo=2 호출 → 다음 구간 rows 확인
7. 음성: pageSize=999 → BUSINESS_ERROR,
   valuationBaseDate="2026-7-1" → BUSINESS_ERROR(형식),
   AV.AssetValuation.unknown → SERVICE_NOT_FOUND
8. 검증 결과를 표(항목·기대값·실측값·판정)로 보고하고,
   실행하지 않은 명령은 '미실행'으로 표시하라.

모호한 부분은 구현 전에 나에게 질문할 것.
```
