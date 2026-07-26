# 샘플1 — LN(여신) 신규 모듈 + 단건 조회

> 시나리오: 업무 모듈이 아직 없는 새 업무코드(LN)로 모듈을 생성하고
> 기본키 단건 조회 거래 1건을 구현한다.
> 레퍼런스: eb-service Sample 도메인(단순 분기) + User 도메인(DTO 패턴)

```text
너는 NSIGHT-TCF-FRAMEWORK의 애플리케이션 아키텍트이자 시니어 Java 개발자다.
저장소의 실제 구현과 표준을 분석한 뒤, 신규 업무 모듈 ln-service를
eb-service와 동일한 방식으로 설계·구현·등록·검증하라.
레퍼런스에 없는 구조를 임의로 발명하지 마라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 고정 개발 입력정보 (임의 변경 금지)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| 구분            | 확정값                          |
|-----------------|---------------------------------|
| 업무코드        | LN                              |
| 업무명          | 여신                            |
| 모듈            | ln-service (신규 생성)          |
| 도메인          | Loan                            |
| ServiceId       | LN.Loan.inquiry                 |
| 거래코드        | LN-INQ-0001                     |
| Processing Type | INQUIRY (단건 조회)             |
| 메인 클래스     | NsightLnServiceApplication      |
| BASE 패키지     | com.nh.nsight.marketing.ln      |
| WAR / Context   | ln.war / /ln                    |
| 권장 bootRun 포트| 8103 (임시 — 충돌 검증 필수)   |
| DB 테이블       | LN_LOAN (설계 예시, local 한정) |
| 기준 모듈       | eb-service                      |

포트 8103은 임시 권장값이다. tcf-ui BusinessModuleDefinitions.java(포트 SoT),
application-*.yml, Gateway Route를 검색해 충돌을 확인하고,
충돌하면 임의 변경하지 말고 현황과 후보 포트를 먼저 보고하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 구현 전 필수 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
다음 파일을 실제로 읽고 분석 결과 표(점검항목·확인값·LN 적용방식)를
먼저 보고한 뒤 코드를 변경하라.
- settings.gradle, 루트 build.gradle(businessModules)
- eb-service/NsightEbServiceApplication.java, build.gradle,
  application.yml, application-local.yml, schema.sql
- eb-service Sample 도메인: EbSampleHandler, EbSampleFacade,
  EbSampleService, EbSampleRule, EbSampleDao, EbSampleMapper(.xml)
- tcf-ui/support/BusinessModuleDefinitions.java,
  sample-requests/eb-sample-inquiry.json (header 전체 필드 기준)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 설계 — LN_LOAN (설계 예시)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
schema.sql(local 전용, spring.sql.init.mode=always)에 작성하고
"설계 예시: 실제 DB 정의 확정 시 교체" 주석을 명시하라.

| 컬럼          | 타입          | 제약          | 설명         |
|---------------|---------------|---------------|--------------|
| LOAN_NO       | VARCHAR(20)   | PK            | 대출번호     |
| CUSTOMER_ID   | VARCHAR(20)   | NOT NULL      | 고객번호     |
| LOAN_AMOUNT   | NUMBER(15)    | NOT NULL      | 대출금액     |
| LOAN_STATUS   | VARCHAR(10)   | 기본 'NORMAL' | 대출상태     |
| CREATED_AT    | TIMESTAMP     | 기본 현재시각 | 등록일시     |

시드 데이터 5건 이상 포함.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 6계층 구현
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
com.nh.nsight.marketing.ln
├── NsightLnServiceApplication — NsightWarBootstrap 상속,
│     scanBasePackages="com.nh.nsight",
│     @MapperScan("com.nh.nsight.marketing.ln.persistence.mapper")
├── entry/handler/LnLoanHandler — serviceIds()에 "LN.Loan.inquiry",
│     default → BusinessException(ErrorCode.SERVICE_NOT_FOUND)
├── entry/facade/LnLoanFacade — @Transactional(readOnly=true, timeout=5)
├── application/service/LnLoanService — rule 검증 → dao 단건 조회 →
│     결과 없으면 BusinessException(ErrorCode.BUSINESS_ERROR,
│     "대출번호 미존재: " + loanNo) → Response 조립
├── application/rule/LnLoanRule — validateInquiry(loanNo 필수·형식),
│     buildSearchCriteria
├── application/dto/loan/ — LoanInquiryRequest(fromMap: loanNo),
│     LoanInquiryResponse(toMap: businessCode·serviceId·guid·loan 단건),
│     LoanSearchCriteria
├── persistence/dao/LnLoanDao — selectLoan(criteria) 1:1 위임
├── persistence/mapper/LnLoanMapper + persistence/dto/loan/LoanRow
└── resources/mapper/ln/LnLoanMapper.xml — selectLoan
    (LOAN_NO = #{loanNo} 단건, SELECT * 금지, ${} 금지)

호출 순서 고정: Handler → Facade → Service → Rule → DAO → Mapper.
금지: Handler→DAO, Service→Mapper, Rule→DB, @RestController, Lombok.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 등록 절차 (순서대로)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. settings.gradle 에 include 'ln-service' (업무 모듈 나열 위치)
2. 루트 build.gradle ext.businessModules 에 'ln-service' 추가
3. ln-service/build.gradle — eb-service 복사, archiveFileName='ln.war'
4. application.yml — nsight.tcf.runtime.business-code=LN,
   spring.application.name=nsight-ln-service
5. application-local.yml — server.port=8103,
   jdbc:h2:mem:nsight_ln;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false,
   거래로그 datasource는 eb-service 값 복사
6. tcf-ui BusinessModuleDefinitions.ALL 에
   new ModuleDefinition("LN", "Loan", "여신", 8103) 추가
7. tcf-ui/src/main/resources/sample-requests/ln-sample-inquiry.json 생성
   — eb-sample-inquiry.json의 header 전체 필드를 LN 값으로 교체
   (이 파일이 없으면 tcf-ui 기동이 실패하므로 필수)
8. 게이트: gradle :ln-service:compileJava && gradle :tcf-ui:compileJava

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. compileJava 성공 (ln-service, tcf-ui)
2. gradle :ln-service:bootRun — 8103 기동, schema.sql 초기화 확인
3. 정상 호출: POST http://localhost:8103/online
   header(전체 필드: systemId=NSIGHT-MP, businessCode=LN,
   serviceId=LN.Loan.inquiry, transactionCode=LN-INQ-0001,
   processingType=INQUIRY, guid, channelId=WEBTOP, userId, branchId,
   requestTime, systemDate, bizDate, clientIp)
   + body{loanNo: 시드 값} → result.resultCode == "S0000", 단건 반환
4. 음성 테스트:
   - loanNo 누락 → BUSINESS_ERROR (필수 조회키 없음)
   - 미존재 loanNo → BUSINESS_ERROR (대출번호 미존재)
   - 미지원 serviceId(LN.Loan.unknown) → SERVICE_NOT_FOUND
5. Health: GET http://localhost:8103/actuator/health (bootRun은
   context-path가 / 이므로 /ln/actuator/health 아님)
6. 검증 결과를 표(항목·기대값·실측값·판정)로 보고하고,
   실행하지 않은 명령은 '미실행'으로 표시하라.

모호한 부분은 구현 전에 나에게 질문할 것.
```
