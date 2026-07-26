# 샘플3 — DP(수신) 신규 모듈 + 등록(CREATE) 거래

> 시나리오: 새 업무코드(DP)로 모듈을 생성하고 등록 거래 1건 + 확인용 조회
> 거래 1건을 구현한다. 변경 거래의 트랜잭션 경계·중복 검증이 핵심.
> 레퍼런스: eb-service User 도메인 (EB.User.create / EB.User.inquiry)

```text
너는 NSIGHT-TCF-FRAMEWORK의 애플리케이션 아키텍트이자 시니어 Java 개발자다.
저장소의 실제 구현과 표준을 분석한 뒤, 신규 업무 모듈 dp-service를
eb-service와 동일한 방식으로 설계·구현·등록·검증하라.
등록(CREATE) 거래의 표준 패턴은 eb-service의 EB.User.create 이다.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 고정 개발 입력정보 (임의 변경 금지)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| 구분            | 확정값                                    |
|-----------------|-------------------------------------------|
| 업무코드        | DP                                        |
| 업무명          | 수신                                      |
| 모듈            | dp-service (신규 생성)                    |
| 도메인          | Deposit                                   |
| 거래 1 (등록)   | DP.Deposit.create / DP-CRE-0001 / CREATE  |
| 거래 2 (조회)   | DP.Deposit.inquiry / DP-INQ-0001 / INQUIRY|
| 메인 클래스     | NsightDpServiceApplication                |
| BASE 패키지     | com.nh.nsight.marketing.dp                |
| WAR / Context   | dp.war / /dp                              |
| 권장 bootRun 포트| 8104 (임시 — 충돌 검증 필수)             |
| DB 테이블       | DP_DEPOSIT (설계 예시, local 한정)        |
| 기준 모듈       | eb-service (User 도메인)                  |

포트 8104는 임시 권장값이다. tcf-ui BusinessModuleDefinitions.java,
application-*.yml, Gateway Route에서 충돌을 확인하고,
충돌하면 임의 변경하지 말고 먼저 보고하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 구현 전 필수 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
다음 파일을 실제로 읽고 분석 결과 표를 먼저 보고하라.
- eb-service EbUserHandler(복수 serviceId 분기),
  EbUserFacade(조회 readOnly / 등록 쓰기 트랜잭션 구분),
  EbUserService, EbUserRule(validateCreate 필수값 검증),
  EbUserDao(existsByUserId·insertUser), EbUserMapper.xml(insert 구문)
- eb-service persistence/dto/user/UserInsertRow.java (INSERT 전용 Row DTO)
- settings.gradle, 루트 build.gradle, eb-service build.gradle,
  application*.yml, schema.sql
- tcf-ui BusinessModuleDefinitions.java, sample-requests 명명 규약

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 설계 — DP_DEPOSIT (설계 예시)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
schema.sql(local 전용)에 "설계 예시" 주석과 함께 작성.

| 컬럼           | 타입          | 제약          | 설명         |
|----------------|---------------|---------------|--------------|
| ACCOUNT_NO     | VARCHAR(20)   | PK            | 계좌번호     |
| CUSTOMER_ID    | VARCHAR(20)   | NOT NULL      | 고객번호     |
| PRODUCT_CODE   | VARCHAR(10)   | NOT NULL      | 상품코드     |
| BALANCE        | NUMBER(15)    | 기본 0        | 잔액         |
| ACCOUNT_STATUS | VARCHAR(10)   | 기본 'ACTIVE' | 계좌상태     |
| CREATED_AT     | TIMESTAMP     | 기본 현재시각 | 등록일시     |

조회 검증용 시드 3건 포함.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 6계층 구현
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
com.nh.nsight.marketing.dp
├── NsightDpServiceApplication — NsightWarBootstrap 상속, @MapperScan
├── entry/handler/DpDepositHandler — serviceIds()에 create·inquiry 2건,
│     switch 분기, default → SERVICE_NOT_FOUND
├── entry/facade/DpDepositFacade
│     - inquiry(...): @Transactional(readOnly=true, timeout=5)
│     - create(...):  @Transactional(timeout=5)  ← 쓰기 트랜잭션.
│       EbUserFacade의 조회/등록 메서드 구분 방식을 그대로 따를 것
├── application/service/DpDepositService
│     - create: rule.validateCreate → dao.existsByAccountNo 중복 검사
│       (중복 시 BusinessException(ErrorCode.BUSINESS_ERROR,
│        "이미 존재하는 계좌번호")) → dao.insertDeposit →
│       등록 결과 Response(accountNo·customerId 반환)
│     - inquiry: rule 검증 → dao 단건 조회 → Response
├── application/rule/DpDepositRule
│     - validateCreate: accountNo·customerId·productCode 필수
│       (StringUtils.hasText), balance 음수 금지
│     - validateInquiry: accountNo 필수
├── application/dto/deposit/ — DepositCreateRequest(fromMap)·
│     DepositCreateResponse·DepositInquiryRequest·DepositInquiryResponse
├── persistence/dao/DpDepositDao — insertDeposit(DepositInsertRow)·
│     existsByAccountNo(countByAccountNo > 0)·selectDeposit
├── persistence/mapper/DpDepositMapper +
│     persistence/dto/deposit/DepositInsertRow·DepositRow
└── resources/mapper/dp/DpDepositMapper.xml —
    insertDeposit(#{} 바인딩 INSERT)·countByAccountNo·selectDeposit

주의: INSERT 입력은 화면 Request DTO가 아니라 별도 InsertRow DTO로
전달한다 (EB UserInsertRow 패턴). Rule은 DB를 호출하지 않으므로
중복 검사는 Service에서 DAO로 수행한다.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 등록 절차 (순서대로)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. settings.gradle include 'dp-service' + 루트 businessModules 추가
2. dp-service/build.gradle — eb-service 복사, archiveFileName='dp.war'
3. application.yml(business-code=DP) / application-local.yml
   (port=8104, jdbc:h2:mem:nsight_dp;MODE=Oracle;DB_CLOSE_DELAY=-1;
   DATABASE_TO_UPPER=false, 거래로그 datasource 복사)
4. tcf-ui BusinessModuleDefinitions.ALL 에
   new ModuleDefinition("DP", "Deposit", "수신", 8104) 추가
5. tcf-ui sample-requests/dp-sample-inquiry.json 생성 (header 전체 필드,
   serviceId=DP.Deposit.inquiry — 파일명 규약 준수, 없으면 tcf-ui 기동 실패)
6. 게이트: gradle :dp-service:compileJava && gradle :tcf-ui:compileJava

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. compileJava 성공 (dp-service, tcf-ui)
2. gradle :dp-service:bootRun — 8104 기동
3. 등록: POST http://localhost:8104/online —
   header(전체 필드, serviceId=DP.Deposit.create,
   transactionCode=DP-CRE-0001, processingType=CREATE) +
   body{accountNo:"1002-001", customerId:"C001", productCode:"DP01",
   balance:1000000} → S0000
4. 등록 확인: DP.Deposit.inquiry 로 방금 등록한 accountNo 조회 →
   S0000, 입력값과 일치
5. 음성 테스트:
   - 동일 accountNo 재등록 → BUSINESS_ERROR (중복)
   - customerId 누락 → BUSINESS_ERROR (필수값)
   - balance:-100 → BUSINESS_ERROR (음수 금지)
   - DP.Deposit.unknown → SERVICE_NOT_FOUND
6. 트랜잭션: 등록 실패 케이스 후 재조회하여 INSERT가 남지 않았는지
   (Rollback) 확인
7. 검증 결과를 표(항목·기대값·실측값·판정)로 보고하고,
   실행하지 않은 명령은 '미실행'으로 표시하라.

모호한 부분은 구현 전에 나에게 질문할 것.
```
