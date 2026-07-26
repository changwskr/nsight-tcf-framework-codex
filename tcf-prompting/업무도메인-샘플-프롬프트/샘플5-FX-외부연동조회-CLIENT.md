# 샘플5 — FX(외환) 신규 모듈 + client 계층 외부 연동 조회

> 시나리오: 새 업무코드(FX)로 모듈을 생성하고, 외부 환율 API를
> client 계층으로 호출하는 조회 거래를 구현한다. 외부 미가용 시
> 캐시 테이블 fallback이 핵심 (6계층 + client 계층 사용 예시).
> 레퍼런스: eb-service의 EpOnlineClient (RestClient 기반 외부 호출 표준)

```text
너는 NSIGHT-TCF-FRAMEWORK의 애플리케이션 아키텍트이자 시니어 Java 개발자다.
저장소의 실제 구현과 표준을 분석한 뒤, 신규 업무 모듈 fx-service를
eb-service와 동일한 방식으로 설계·구현·등록·검증하라.
외부 시스템 호출은 반드시 client 패키지 계층으로 분리한다.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 고정 개발 입력정보 (임의 변경 금지)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| 구분            | 확정값                                       |
|-----------------|----------------------------------------------|
| 업무코드        | FX                                           |
| 업무명          | 외환                                         |
| 모듈            | fx-service (신규 생성)                       |
| 도메인          | ExchangeRate                                 |
| ServiceId       | FX.ExchangeRate.inquiry                      |
| 거래코드        | FX-INQ-0001                                  |
| Processing Type | INQUIRY                                      |
| 메인 클래스     | NsightFxServiceApplication                   |
| BASE 패키지     | com.nh.nsight.marketing.fx                   |
| WAR / Context   | fx.war / /fx                                 |
| 권장 bootRun 포트| 8106 (임시 — 충돌 검증 필수)                |
| 외부 연동       | 환율 API (URL은 설정 주입 — 하드코딩 금지)   |
| Fallback 테이블 | FX_RATE_CACHE (설계 예시, local 한정)        |
| 기준 모듈       | eb-service (client 계층은 EpOnlineClient)    |

포트 8106은 임시 권장값이다. tcf-ui BusinessModuleDefinitions.java,
application-*.yml, Gateway Route에서 충돌을 확인하고,
충돌하면 임의 변경하지 말고 먼저 보고하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 구현 전 필수 분석
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
다음 파일을 실제로 읽고 분석 결과 표를 먼저 보고하라.
- eb-service/client/EpOnlineClient.java — RestClient 구성,
  외부 URL을 파라미터·설정으로 주입받는 방식, 실패 시 false 반환과
  로그 처리 패턴 (이 모듈의 client 계층 표준)
- eb-service application-local.yml 의 nsight.eb.event-publish.ep-online-url
  (외부 URL을 설정으로 관리하는 방식)
- eb-service 6계층 (EbSampleHandler 계열) + application*.yml, schema.sql
- settings.gradle, 루트 build.gradle
- tcf-ui BusinessModuleDefinitions.java, sample-requests 명명 규약

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 설계 — FX_RATE_CACHE (설계 예시)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
schema.sql(local 전용)에 "설계 예시" 주석과 함께 작성.
외부 API 실패 시 fallback으로 사용하는 최근 환율 캐시다.

| 컬럼          | 타입          | 제약          | 설명              |
|---------------|---------------|---------------|-------------------|
| CURRENCY_CODE | VARCHAR(3)    | PK            | 통화코드(USD 등)  |
| BASE_DATE     | VARCHAR(8)    | NOT NULL      | 고시 기준일       |
| EXCHANGE_RATE | NUMBER(12,4)  | NOT NULL      | 매매기준율        |
| SOURCE        | VARCHAR(10)   | 기본 'CACHE'  | 출처(API·CACHE)   |
| UPDATED_AT    | TIMESTAMP     | 기본 현재시각 | 갱신일시          |

시드 데이터: USD·EUR·JPY·CNY 4건 (fallback 검증용).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 6계층 + client 구현
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
com.nh.nsight.marketing.fx
├── NsightFxServiceApplication — NsightWarBootstrap 상속, @MapperScan
├── entry/handler/FxExchangeRateHandler — serviceIds()에
│     "FX.ExchangeRate.inquiry", default → SERVICE_NOT_FOUND
├── entry/facade/FxExchangeRateFacade — @Transactional(readOnly=true, timeout=5)
├── application/service/FxExchangeRateService — 처리 흐름:
│     rule.validateInquiry → client.fetchRate(외부 API) 시도
│     → 성공: source="API"로 Response 조립 + (선택) 캐시 갱신은
│       조회 거래이므로 하지 않는다 — readOnly 위반 금지
│     → 실패(연결 불가·타임아웃·비정상 응답): dao.selectCachedRate
│       fallback → source="CACHE"로 Response 조립
│     → 캐시에도 없으면 BusinessException(ErrorCode.BUSINESS_ERROR,
│       "환율 정보를 조회할 수 없습니다: " + currencyCode)
├── application/rule/FxExchangeRateRule — currencyCode 필수·3자리 영문,
│     baseDate는 있으면 yyyyMMdd 검증. Rule은 client·DAO를 호출하지 않는다.
├── application/dto/exchangerate/ — ExchangeRateInquiryRequest(fromMap)·
│     ExchangeRateInquiryResponse(toMap: currencyCode·rate·baseDate·
│     source·guid)·ExchangeRateSearchCriteria
├── client/FxRateClient — EpOnlineClient 패턴을 따라 RestClient 사용,
│     @Component, URL은 생성자 주입이 아닌 메서드 파라미터 또는
│     @Value("${nsight.fx.rate-api.url}") 설정 주입.
│     타임아웃·예외는 잡아서 Optional.empty() 반환 (Service가 fallback 판단).
│     외부 호출 로그에 개인정보·인증키 원문 출력 금지.
├── client/dto/rate/FxRateApiResponse — 외부 응답 전용 DTO
│     (외부 스키마를 업무 DTO에 직접 노출 금지)
├── persistence/dao/FxRateDao — selectCachedRate(currencyCode)
├── persistence/mapper/FxRateMapper + persistence/dto/rate/FxRateRow
└── resources/mapper/fx/FxRateMapper.xml — selectCachedRate

호출 규약: Service → client (Facade·Rule·DAO에서 client 호출 금지).
외부 URL·인증키는 application-*.yml 설정으로만 관리하고 하드코딩 금지.

설정 (application-local.yml):
nsight:
  fx:
    rate-api:
      url: http://127.0.0.1:9999/mock-rate   # local 미기동 URL —
      connect-timeout-ms: 2000               # fallback 경로 검증용
      read-timeout-ms: 3000

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 등록 절차 (순서대로)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. settings.gradle include 'fx-service' + 루트 businessModules 추가
2. fx-service/build.gradle — eb-service 복사, archiveFileName='fx.war'
3. application.yml(business-code=FX) / application-local.yml
   (port=8106, jdbc:h2:mem:nsight_fx;MODE=Oracle;DB_CLOSE_DELAY=-1;
   DATABASE_TO_UPPER=false, 거래로그 datasource 복사, rate-api 설정)
4. tcf-ui BusinessModuleDefinitions.ALL 에
   new ModuleDefinition("FX", "Exchange Rate", "외환", 8106) 추가
5. tcf-ui sample-requests/fx-sample-inquiry.json 생성 (header 전체 필드,
   serviceId=FX.ExchangeRate.inquiry — 파일명 규약 준수,
   없으면 tcf-ui 기동 실패)
6. 게이트: gradle :fx-service:compileJava && gradle :tcf-ui:compileJava

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. compileJava 성공 (fx-service, tcf-ui)
2. gradle :fx-service:bootRun — 8106 기동
3. Fallback 경로 (기본 — 외부 API는 local에서 미기동):
   POST http://localhost:8106/online —
   header(전체 필드, serviceId=FX.ExchangeRate.inquiry,
   transactionCode=FX-INQ-0001) + body{currencyCode:"USD"}
   → S0000, source="CACHE", 시드 환율값 일치.
   외부 호출 실패가 오류 응답이 아니라 fallback으로 이어지는지 확인.
4. 응답 시간: 외부 타임아웃(2~3초) + fallback 이 온라인 거래
   타임아웃(5초) 안에 완료되는지 확인
5. 음성 테스트:
   - currencyCode 누락 → BUSINESS_ERROR (필수)
   - currencyCode:"US" (2자리) → BUSINESS_ERROR (형식)
   - 캐시에 없는 통화(GBP) → BUSINESS_ERROR (조회 불가)
   - FX.ExchangeRate.unknown → SERVICE_NOT_FOUND
6. (선택) 외부 정상 경로: 임의 mock 서버를 9999 포트에 띄울 수 있으면
   source="API" 경로도 확인하고, 불가하면 '미실행'으로 표시
7. 검증 결과를 표(항목·기대값·실측값·판정)로 보고하고,
   실행하지 않은 명령은 '미실행'으로 표시하라.

모호한 부분은 구현 전에 나에게 질문할 것.
```
