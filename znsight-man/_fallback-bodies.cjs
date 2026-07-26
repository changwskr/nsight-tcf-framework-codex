'use strict';

/** guide docx 없을 때 Manual 장 전용 fallback 본문 */

const FALLBACK_BY_CHAPTER = {
  50: `## 50.1 도입 전 안내말

NSIGHT TCF에서 공통코드는 채널, 상태, 업무구분, 화면유형 등 **반복 사용 기준정보**를 OM에서 중앙 관리하는 기능이다. 업무 WAR에 하드코딩하지 않고 \`OM.CommonCode.*\` ServiceId로 조회·캐시한다.

## 50.2 공통코드 사용 결론

| 구분 | 기준 |
| --- | --- |
| 관리 주체 | OM (tcf-om) |
| Handler | \`OmCommonCodeHandler\` |
| ServiceId Prefix | \`OM.CommonCode.*\` |
| Cache | \`commonCode\` Cache Name (\`CC:CODE:{GROUP}\` Key) |
| 조회 시점 | STF 전처리, 화면 INIT, 드롭다운 로딩 |
| 변경 반영 | OM 등록·수정 후 Cache Evict (\`OM.Cache.evict\`) |

## 50.3 공통코드 등록·사용 흐름

\`\`\`text
[업무 개발자] 코드 그룹·코드값 정의
        ↓
[OM 관리자] OM 공통코드 화면 등록 (GROUP_CODE, CODE, CODE_NAME)
        ↓
[OM] OM.CommonCode.selectList / selectDetail 등록 (Service Catalog)
        ↓
[Cache] commonCode Cache 적재
        ↓
[업무 화면] INIT 시 ServiceId 호출 → Header + body(groupCode)
        ↓
[업무 Service] 공통코드 목록을 Response DTO로 반환
\`\`\`

## 50.4 대표 ServiceId

| ServiceId | 용도 |
| --- | --- |
| OM.CommonCode.selectList | 코드 그룹별 목록 조회 |
| OM.CommonCode.selectDetail | 단건 코드 조회 |
| OM.CommonCode.create | 신규 코드 등록 |
| OM.CommonCode.update | 코드 수정 |
| CC.CommonCode.selectList | 업무 WAR 공통 조회 (CC 업무) |

## 50.5 업무 코드에서 사용 예

\`\`\`json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "CC.CommonCode.selectList",
    "transactionCode": "CC-INQ-0001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234"
  },
  "body": {
    "groupCode": "CHANNEL_ID"
  }
}
\`\`\`

## 50.6 Cache 연계

| 항목 | 기준 |
| --- | --- |
| Cache Name | \`commonCode\` |
| Cache Key | \`CC:CODE:{GROUP_CODE}\` |
| TTL | 30분 (운영 정책) |
| Evict | OM 코드 등록·수정·중지 시 \`OM.Cache.evict\` |

## 50.7 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| 코드 그룹·코드값이 OM에 등록되었는가 | □ |
| Service Catalog에 OM.CommonCode.* 가 등록되었는가 | □ |
| 화면 INIT ServiceId와 공통코드 ServiceId가 분리되었는가 | □ |
| Cache Evict 정책이 등록되었는가 | □ |
| 업무 코드에 상수 하드코딩이 없는가 | □ |
`,

  60: `## 60.1 도입 전 안내말

장애 테스트(Chaos / Resilience Test)는 NSIGHT TCF가 **DB·세션·외부연계·Gateway·WAS** 장애 시에도 거래를 안전하게 차단·복구·추적할 수 있는지 검증한다. 성능 테스트(59장)와 달리 **장애 주입 후 오류코드·롤백·로그·알림**을 확인한다.

## 60.2 장애 테스트 결론

| 구분 | 기준 |
| --- | --- |
| 목적 | 장애 시 Fail-Fast, 데이터 정합성, 추적성, 복구 가능성 검증 |
| 대상 | DB, SESSIONDB, Gateway, 외부 API, Cache, Tomcat Thread 고갈 |
| 호출 | \`POST /{businessCode}/online\` + 표준 Header |
| 완료 기준 | 표준 오류코드 반환, 거래로그 FAIL/TIMEOUT 기록, 중복·유실 없음 |
| 환경 | STG (운영 DB 직접 주입 금지) |
| 연계 | [69장 장애 확인](./69-장애-개발자-확인-항목.md), [67장 롤백](./67-롤백-절차.md) |

## 60.3 장애 시나리오

| 시나리오 | 주입 방법 | 기대 결과 |
| --- | --- | --- |
| RDW 연결 실패 | DB Pool down / 방화벽 차단 | \`E-TCF-DB-*\`, FAIL 로그, 사용자 안내 메시지 |
| SQL Timeout | Query Timeout 1ms 강제 | \`E-TCF-TMO-*\`, TIMEOUT 상태, MDC traceId 유지 |
| SESSIONDB 장애 | Session JDBC unreachable | 인증·세션 재검증 실패, \`E-TCF-AUTHN-*\` |
| Gateway Target DOWN | \`HEALTH_STATUS=DOWN\` | 라우팅 차단, Gateway Relay Log |
| 외부 API Timeout | Stub 지연 / 연결 거부 | Rule/Client Timeout, \`E-{BC}-INT-*\` |
| Cache Miss 폭주 | Cache Evict 전체 | DB 부하 증가 허용 범위, STF latency 관측 |
| Thread Pool 고갈 | 동시 요청 과다 | 503/Timeout, Busy Thread 알림 |

## 60.4 검증 항목

| 검증 항목 | 기준 |
| --- | --- |
| 오류코드 | 표준 \`E-TCF-*\` / \`E-{BC}-*\` 반환 |
| 거래로그 | PROCESSING → FAIL/TIMEOUT, GUID·ServiceId 기록 |
| MDC | guid, traceId, transactionId 유지 |
| Idempotency | 변경 거래 중복 없음 |
| 롤백 | DB 트랜잭션 롤백, 부분 커밋 없음 |
| 알림 | APM/운영 Alert 발생 |

## 60.5 실행 절차

\`\`\`text
1. STG 환경·대표 ServiceId 선정
2. 정상 Smoke Test (기준선)
3. 장애 시나리오별 주입
4. HTTP 응답·오류코드·거래로그·MDC 확인
5. 복구 후 재Smoke Test
6. 결과·개선과제 OM/배포 이력 기록
\`\`\`

## 60.6 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| DB/세션/Gateway/외부연계 시나리오가 정의되었는가 | □ |
| 장애 시 표준 오류코드가 반환되는가 | □ |
| 거래로그에 FAIL/TIMEOUT이 기록되는가 | □ |
| 복구 후 정상 거래가 재개되는가 | □ |
| 롤백·배포 절차와 연계되었는가 | □ |
`,

  78: `## 78.1 도입 전 안내말

NSIGHT TCF 테스트 코드는 **JUnit 5 + Mockito + AssertJ + Spring Boot Test**를 기준으로 한다. 단위 테스트(54장), 통합 테스트(55~57장)와 연계하여 Handler·Core·업무 WAR 샘플을 참고한다.

## 78.2 테스트 유형별 샘플 위치

| 유형 | 모듈 | 예시 클래스 |
| --- | --- | --- |
| Core 단위 | tcf-core | \`TransactionControlTest\`, \`StandardHeaderEchoTest\` |
| Timeout | tcf-core | \`TimeoutPolicyTest\`, \`OnlineTransactionTimeoutExecutorTest\` |
| Cache | tcf-cache | \`TcfCacheConfigurationTest\` |
| Web/Policy | tcf-web | \`JdbcTransactionControlRepositoryTest\` |
| 업무 통합 | sv-service | \`SvTransactionLogIntegrationTest\` |

## 78.3 Handler 단위 테스트 (Mockito)

\`\`\`java
@ExtendWith(MockitoExtension.class)
class SvCustomerSummaryHandlerTest {

    @Mock private SvCustomerSummaryFacade facade;
    @InjectMocks private SvCustomerSummaryHandler handler;

    @Test
    void handle_delegatesToFacade() {
        var request = new StandardRequest<SvCustomerSummaryRequest>();
        request.setBody(new SvCustomerSummaryRequest());
        when(facade.selectSummary(any())).thenReturn(new SvCustomerSummaryResponse());

        var response = handler.handle(request);

        assertThat(response).isNotNull();
        verify(facade).selectSummary(any());
    }
}
\`\`\`

## 78.4 MockMvc 온라인 통합 테스트 (sv-service)

\`\`\`java
@SpringBootTest
@AutoConfigureMockMvc
class SvTransactionLogIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void onlineCallPersistsTransactionLog() throws Exception {
        String json = StreamUtils.copyToString(
            new ClassPathResource("sv-sample-inquiry.json").getInputStream(),
            StandardCharsets.UTF_8);

        int before = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM TCF_TX_LOG", Integer.class);

        mockMvc.perform(post("/online")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());

        int after = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM TCF_TX_LOG", Integer.class);
        assertThat(after).isGreaterThan(before);
    }
}
\`\`\`

## 78.5 Core Header 검증 테스트

\`\`\`java
@Test
void standardHeader_echoesTraceFields() {
    StandardHeader header = new StandardHeader();
    header.setServiceId("SV.Customer.selectSummary");
    header.setBusinessCode("SV");
    header.setTraceId("T202607050001");

    assertThat(header.getServiceId()).startsWith("SV.");
    assertThat(header.getTraceId()).isNotBlank();
}
\`\`\`

## 78.6 실행 방법

\`\`\`bash
# 모듈 단위
./gradlew :tcf-core:test
./gradlew :sv-service:test

# 전체 (CI와 동일)
./gradlew test
\`\`\`

## 78.7 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| JUnit 5 / Mockito / AssertJ 사용 | □ |
| Handler는 Facade Mock 위주 | □ |
| DB 검증은 @SpringBootTest + Test DB | □ |
| 표준 전문 JSON fixture 사용 | □ |
| CI에서 \`./gradlew test\` 통과 | □ |
`,
};

function getFallbackBody(chapterNum) {
  return FALLBACK_BY_CHAPTER[chapterNum] || '';
}

module.exports = { getFallbackBody, FALLBACK_BY_CHAPTER };
