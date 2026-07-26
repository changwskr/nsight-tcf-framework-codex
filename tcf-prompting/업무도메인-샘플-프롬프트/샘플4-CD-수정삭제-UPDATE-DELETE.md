# 샘플4 — CD(카드) 신규 모듈 + 수정(UPDATE)·삭제(DELETE) 거래

> 시나리오: 새 업무코드(CD)로 모듈을 생성하고 한 도메인 Handler에
> 조회·수정·삭제 3개 거래를 등록한다. 대상 존재 검증과
> 논리 삭제(상태 변경) 정책이 핵심.
> 레퍼런스: eb-service User 도메인 (복수 거래 분기·쓰기 트랜잭션)

```text
너는 NSIGHT-TCF-FRAMEWORK의 애플리케이션 아키텍트이자 시니어 Java 개발자다.
저장소의 실제 구현과 표준을 분석한 뒤, 신규 업무 모듈 cd-service를
eb-service와 동일한 방식으로 설계·구현·등록·검증하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 고정 개발 입력정보 (임의 변경 금지)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| 구분            | 확정값                                     |
|-----------------|--------------------------------------------|
| 업무코드        | CD                                         |
| 업무명          | 카드                                       |
| 모듈            | cd-service (신규 생성)                     |
| 도메인          | Card                                       |
| 거래 1 (조회)   | CD.Card.inquiry / CD-INQ-0001 / INQUIRY    |
| 거래 2 (수정)   | CD.Card.update / CD-UPD-0001 / UPDATE      |
| 거래 3 (해지)   | CD.Card.delete / CD-DEL-0001 / DELETE      |
| 메인 클래스     | NsightCdServiceApplication                 |
| BASE 패키지     | com.nh.nsight.marketing.cd                 |
| WAR / Context   | cd.war / /cd                               |
| 권장 bootRun 포트| 8105 (임시 — 충돌 검증 필수)              |
| DB 테이블       | CD_CARD (설계 예시, local 한정)            |
| 삭제 정책       | 물리 삭제 금지 — CARD_STATUS='CLOSED' 논리 삭제 |
| 기준 모듈       | eb-service (User 도메인)                   |

포트 8105는 임시 권장값이다. tcf-ui BusinessModuleDefinitions.java,
application-*.yml, Gateway Route에서 충돌을 확인하고,
충돌하면 임의 변경하지 말고 먼저 보고하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 구현 전 필수 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
다음 파일을 실제로 읽고 분석 결과 표를 먼저 보고하라.
- eb-service EbUserHandler(복수 serviceId switch 분기 패턴),
  EbUserFacade(읽기/쓰기 트랜잭션 구분), EbUserService, EbUserRule,
  EbUserDao, EbUserMapper.xml
- settings.gradle, 루트 build.gradle, eb-service build.gradle,
  application*.yml, schema.sql
- tcf-ui BusinessModuleDefinitions.java, sample-requests 명명 규약

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 설계 — CD_CARD (설계 예시)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
schema.sql(local 전용)에 "설계 예시" 주석과 함께 작성.

| 컬럼         | 타입          | 제약          | 설명                        |
|--------------|---------------|---------------|-----------------------------|
| CARD_NO      | VARCHAR(20)   | PK            | 카드번호                    |
| CUSTOMER_ID  | VARCHAR(20)   | NOT NULL      | 고객번호                    |
| CARD_NAME    | VARCHAR(50)   | NOT NULL      | 카드명                      |
| MONTHLY_LIMIT| NUMBER(12)    | 기본 1000000  | 월 한도                     |
| CARD_STATUS  | VARCHAR(10)   | 기본 'ACTIVE' | ACTIVE·SUSPENDED·CLOSED     |
| UPDATED_AT   | TIMESTAMP     |               | 수정일시                    |
| CREATED_AT   | TIMESTAMP     | 기본 현재시각 | 등록일시                    |

시드 데이터 5건 (ACTIVE 4건, CLOSED 1건).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 6계층 구현
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
com.nh.nsight.marketing.cd
├── NsightCdServiceApplication — NsightWarBootstrap 상속, @MapperScan
├── entry/handler/CdCardHandler — serviceIds()에 3건 등록,
│     switch 분기, default → SERVICE_NOT_FOUND
├── entry/facade/CdCardFacade
│     - inquiry: @Transactional(readOnly=true, timeout=5)
│     - update / delete: @Transactional(timeout=5)
├── application/service/CdCardService
│     - update: rule.validateUpdate → 대상 조회(없으면 BUSINESS_ERROR
│       "카드번호 미존재") → CLOSED 상태면 BUSINESS_ERROR
│       ("해지된 카드는 수정 불가") → dao.updateCard →
│       변경 건수 1 확인 → 수정 후 데이터로 Response
│     - delete: rule.validateDelete → 대상 조회·상태 확인 →
│       dao.closeCard (CARD_STATUS='CLOSED', UPDATED_AT 갱신) → Response
│     - inquiry: 단건 조회
├── application/rule/CdCardRule
│     - validateUpdate: cardNo 필수, cardName·monthlyLimit 중
│       최소 1개 존재, monthlyLimit > 0
│     - validateDelete: cardNo 필수
│     상태 전이 판단(CLOSED 수정 불가)은 조회 결과가 필요하므로
│     Service에서 수행한다 — Rule은 DB를 호출하지 않는다.
├── application/dto/card/ — CardInquiryRequest·CardUpdateRequest·
│     CardDeleteRequest(각 fromMap)·CardInquiryResponse·
│     CardUpdateResponse·CardDeleteResponse
├── persistence/dao/CdCardDao — selectCard·updateCard·closeCard
├── persistence/mapper/CdCardMapper + persistence/dto/card/CardRow·CardUpdateRow
└── resources/mapper/cd/CdCardMapper.xml
    - selectCard: CARD_NO = #{cardNo}
    - updateCard: <set><if> 동적 UPDATE (전달된 필드만 갱신,
      UPDATED_AT = CURRENT_TIMESTAMP)
    - closeCard: CARD_STATUS='CLOSED' 논리 삭제 — DELETE 구문 금지

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 등록 절차 (순서대로)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. settings.gradle include 'cd-service' + 루트 businessModules 추가
2. cd-service/build.gradle — eb-service 복사, archiveFileName='cd.war'
3. application.yml(business-code=CD) / application-local.yml
   (port=8105, jdbc:h2:mem:nsight_cd;MODE=Oracle;DB_CLOSE_DELAY=-1;
   DATABASE_TO_UPPER=false, 거래로그 datasource 복사)
4. tcf-ui BusinessModuleDefinitions.ALL 에
   new ModuleDefinition("CD", "Card", "카드", 8105) 추가
5. tcf-ui sample-requests/cd-sample-inquiry.json 생성 (header 전체 필드,
   serviceId=CD.Card.inquiry — 파일명 규약 준수, 없으면 tcf-ui 기동 실패)
6. 게이트: gradle :cd-service:compileJava && gradle :tcf-ui:compileJava

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. compileJava 성공 (cd-service, tcf-ui)
2. gradle :cd-service:bootRun — 8105 기동
3. 수정: CD.Card.update (processingType=UPDATE) —
   body{cardNo: 시드 ACTIVE 카드, monthlyLimit:2000000} → S0000,
   inquiry 재조회로 한도 변경·UPDATED_AT 갱신 확인
4. 부분 수정: cardName만 전달 → cardName만 변경, monthlyLimit 유지 확인
5. 해지: CD.Card.delete (processingType=DELETE) → S0000,
   재조회 시 CARD_STATUS='CLOSED' 확인 (행은 존재해야 함 — 논리 삭제)
6. 음성 테스트:
   - 미존재 cardNo 수정 → BUSINESS_ERROR (미존재)
   - CLOSED 카드 수정 → BUSINESS_ERROR (해지 카드 수정 불가)
   - 수정 필드 전부 누락 → BUSINESS_ERROR (변경 항목 없음)
   - monthlyLimit:0 → BUSINESS_ERROR
   - CD.Card.unknown → SERVICE_NOT_FOUND
7. 검증 결과를 표(항목·기대값·실측값·판정)로 보고하고,
   실행하지 않은 명령은 '미실행'으로 표시하라.

모호한 부분은 구현 전에 나에게 질문할 것.
```
