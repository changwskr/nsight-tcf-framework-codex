
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제9부. 테스트·품질 Gate·CI/CD·배포·운영전환

## 1. 도입 전 안내말

제8부에서는 Cache, 파일, 대용량 처리, Batch와 Scheduler를 배웠습니다.

이제 프로그램 기능이 모두 구현되었다고 가정해 봅시다.

```
ServiceId가 등록되었다.

Handler·Facade·Service·Rule이 구현되었다.

DAO·Mapper와 SQL이 작성되었다.

화면에서 정상 응답이 확인되었다.
```

초보 개발자는 이 시점에 다음과 같이 생각하기 쉽습니다.

```
“개발이 끝났다.”
```

하지만 기업 시스템에서 개발 완료는 단순히 자신의 PC에서 기능이 동작하는 상태를 의미하지 않습니다.

```
다른 개발자의 환경에서도 빌드되어야 한다.

테스트를 반복 실행해도 같은 결과가 나와야 한다.

기존 기능을 깨뜨리지 않아야 한다.

보안·성능·장애 조건을 검증해야 한다.

배포 결과를 추적할 수 있어야 한다.

장애가 발생하면 이전 버전으로 돌아갈 수 있어야 한다.

운영자가 무엇을 확인해야 하는지 알아야 한다.
```

따라서 진정한 개발 완료는 다음과 같이 정의합니다.

```
구현 완료
+ 자동 테스트
+ 품질 Gate 통과
+ 재현 가능한 빌드
+ 승인된 배포
+ 운영 점검
+ Rollback 가능
```

다음과 같은 배포 상황을 생각해 봅시다.

```
개발자 PC
→ 정상

개발 서버
→ 정상

통합 테스트 서버
→ 정상

운영 서버
→ 기동 실패
```

가능한 원인은 다양합니다.

```
운영 환경설정 누락

DB 접속정보 오류

Mapper XML이 WAR에 포함되지 않음

Java 버전 불일치

운영에서만 사용하는 권한정보 누락

ServiceId가 OM에 등록되지 않음

이전 버전 WAR가 일부 서버에 남아 있음

배포 중 사용자 요청이 유입됨
```

이러한 문제를 사람의 기억과 수작업만으로 막기는 어렵습니다.

그래서 다음 자동화 구조가 필요합니다.

```
소스 Commit
→ 컴파일
→ 단위 테스트
→ 정적분석
→ 통합 테스트
→ 품질 Gate
→ WAR 생성
→ Artifact 저장
→ 승인
→ 환경별 배포
→ Health Check
→ 거래 Smoke Test
→ 모니터링
→ 성공 또는 Rollback
```

제9부의 핵심 원칙은 다음과 같습니다.

```
테스트되지 않은 코드는 배포하지 않는다.

한 번 만든 Artifact를 환경마다 다시 빌드하지 않는다.

환경설정과 소스코드를 분리한다.

배포 성공은 파일 복사가 아니라
기동·Health Check·거래 검증 완료를 의미한다.

Rollback은 장애가 발생한 뒤 생각하는 것이 아니라
배포 전에 준비한다.
```

## 2. 제9부 개요

### 2.1 목적

제9부의 목적은 초보 개발자가 개발한 NSIGHT TCF 기능을 자동으로 검증하고, 재현 가능한 Artifact를 생성하며, 안전하게 배포하고 운영으로 전환하는 전체 과정을 이해하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 단위·통합·거래·계약 테스트를 구분한다.
- 계층별로 적절한 테스트 범위를 결정한다.
- TCF 표준 요청을 이용한 거래 테스트를 작성한다.
- 정상·업무 오류·시스템 오류·Timeout을 검증한다.
- 성능·보안·장애 테스트의 목적을 설명한다.
- 정적분석과 Architecture Test를 품질 Gate로 적용한다.
- Gradle 멀티모듈 프로젝트를 빌드한다.
- 실행 가능한 WAR와 배포 대상 WAR를 구분한다.
- 동일 Artifact를 개발·검증·운영 환경에 승격한다.
- CI와 CD의 차이를 설명한다.
- 배포 전·중·후 점검항목을 정의한다.
- Rolling·Blue-Green·일괄 배포를 비교한다.
- Health Check와 Smoke Test를 구분한다.
- Rollback 기준과 절차를 설계한다.
- 운영전환 체크리스트와 인수 증적을 작성한다.

### 2.2 적용범위

| 영역 | 주요 내용 |
| --- | --- |
| 단위 테스트 | Rule·Service·Handler |
| 통합 테스트 | Mapper·DB·Spring Context |
| 거래 테스트 | Controller부터 ETF까지 |
| 계약 테스트 | 요청·응답·연계 전문 |
| 성능 테스트 | TPS·p95·부하·Stress |
| 장애 테스트 | DB·외부연계·WAS 장애 |
| 보안 테스트 | 인증·권한·입력·로그 |
| 품질 Gate | 정적분석·Coverage·Architecture |
| Build | Gradle 멀티모듈·WAR |
| Artifact | 버전·Checksum·보관 |
| CI | Commit 이후 자동검증 |
| CD | 승인·배포·검증·Rollback |
| 운영전환 | 인수·모니터링·비상연락 |
| 변경관리 | 호환성·폐기·Release Note |

### 2.3 대상 독자

- 자신의 PC에서만 테스트하는 초보 개발자
- 단위 테스트와 통합 테스트의 차이가 어려운 개발자
- Gradle 빌드와 WAR 생성 과정을 이해하고 싶은 개발자
- CI와 CD가 같은 것이라고 생각하는 개발자
- 운영 서버에 직접 파일을 복사해 배포하는 개발자
- 배포 실패 후 Rollback 방법을 알지 못하는 개발자
- 테스트 결과와 배포 증적을 작성해야 하는 개발자
- 운영전환을 처음 준비하는 개발자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
ServiceId는 거래의 실행 식별자다.

Handler는 Facade를 호출한다.

Facade는 트랜잭션 경계를 가진다.

Rule은 업무 규칙을 검증한다.

Mapper는 SQL을 실행한다.

STF는 인증·권한·거래통제·Timeout을 처리한다.

ETF는 거래 종료와 표준 응답을 처리한다.
```

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| Unit Test | 클래스나 메서드를 작은 단위로 검증 |
| Integration Test | 여러 구성요소와 실제 기술 연결을 검증 |
| Transaction Test | 표준 요청부터 표준 응답까지 전체 거래 검증 |
| Contract Test | 호출자와 피호출자의 요청·응답 약속 검증 |
| Regression Test | 변경으로 기존 기능이 깨지지 않았는지 검증 |
| Smoke Test | 배포 직후 핵심 기능을 짧게 확인 |
| Static Analysis | 실행하지 않고 소스의 문제를 검사 |
| Code Coverage | 테스트가 실행한 코드의 비율 |
| Quality Gate | 배포 단계로 넘어가기 위한 통과조건 |
| CI | 소스 변경을 자동 빌드·테스트·검증하는 과정 |
| CD | 검증된 Artifact를 환경에 배포하는 과정 |
| Artifact | 빌드 결과로 생성된 WAR·JAR·설정 패키지 |
| Immutable Artifact | 배포 후 내용을 변경하지 않는 Artifact |
| Release | 배포 가능한 버전과 변경내용 묶음 |
| Rollback | 이전 정상 버전으로 되돌리는 것 |
| Roll Forward | 수정 버전을 새로 배포해 복구하는 것 |
| Blue-Green | 구·신 환경을 나누어 전환하는 배포 |
| Rolling | 서버를 일부씩 순차 교체하는 배포 |
| Canary | 일부 사용자·트래픽에 먼저 적용하는 배포 |

## 제64장. 테스트 전략 세우기

### 64.1 테스트는 오류를 찾는 작업만이 아니다

테스트는 다음 질문에 답하기 위한 활동입니다.

```
요구사항대로 동작하는가?

잘못된 요청을 안전하게 거부하는가?

기존 기능을 깨뜨리지 않는가?

장애가 발생해도 데이터가 보호되는가?

목표 처리량과 응답시간을 만족하는가?

운영자가 오류 원인을 찾을 수 있는가?
```

### 64.2 테스트 피라미드

권장 구조:

```
             거래·E2E 테스트
            /              \
           계약·통합 테스트
          /                \
         단위 테스트
```

단위 테스트는 많고 빠르게 실행되어야 합니다.

거래·E2E 테스트는 실제 구성요소를 많이 사용하므로 상대적으로 적고 핵심 시나리오 중심으로 작성합니다.

### 64.3 테스트 종류

| 종류 | 검증 대상 | 속도 | 외부 의존 |
| --- | --- | --- | --- |
| Rule 단위 테스트 | 업무 규칙 | 매우 빠름 | 없음 |
| Service 단위 테스트 | 업무 흐름 | 빠름 | Mock |
| Handler 테스트 | ServiceId 분기 | 빠름 | Mock |
| Mapper 테스트 | SQL·매핑 | 보통 | DB |
| 통합 테스트 | Spring·트랜잭션 | 보통 | 일부 |
| 거래 테스트 | TCF 전체 흐름 | 느림 | 다수 |
| 계약 테스트 | 전문 호환성 | 보통 | Mock Server 가능 |
| 성능 테스트 | 처리량·응답시간 | 느림 | 유사 운영환경 |
| 장애 테스트 | 장애·복구 | 느림 | 통제된 환경 |

### 64.4 무엇을 어디에서 테스트할까요?

#### Rule

```
날짜가 올바른가?
상태전이가 허용되는가?
중복인가?
업무 계산결과가 맞는가?
```

#### Service

```
Rule과 DAO를 올바른 순서로 호출하는가?
조회결과 없음에 맞는 예외를 발생시키는가?
응답 DTO를 정확히 생성하는가?
```

#### Handler

```
ServiceId가 등록되었는가?
올바른 Facade 메서드로 연결되는가?
요청 Body가 DTO로 변환되는가?
```

#### Mapper

```
SQL이 실행되는가?
조건이 정확한가?
컬럼과 DTO가 매핑되는가?
영향 건수가 맞는가?
```

#### 거래 테스트

```
STF가 Header를 검증하는가?
권한이 없으면 Handler가 실행되지 않는가?
ETF가 표준 오류 응답을 생성하는가?
TraceId와 거래로그가 기록되는가?
```

### 64.5 정상 시나리오만 테스트하면 안 된다

최소한 다음 네 가지 흐름을 검증해야 합니다.

```
정상 처리

예상 가능한 업무 오류

기술적인 시스템 오류

Timeout·장애
```

CRUD 변경 거래라면 추가로 다음을 검증합니다.

```
중복 요청

동시성 충돌

Rollback

변경 이력

감사로그
```

### 64.6 테스트 데이터

테스트 데이터는 반복 실행할 수 있어야 합니다.

금지:

```
운영 DB의 실제 고객번호를 테스트에 사용

다른 테스트가 만든 데이터에 의존

실행할 때마다 현재 날짜에 따라 결과 변경
```

권장:

```
테스트 전용 고객번호

테스트 시작 시 데이터 준비

테스트 종료 후 Rollback 또는 삭제

Clock을 주입해 날짜 고정
```

### 64.7 독립성

테스트 A의 결과가 테스트 B에 영향을 주면 안 됩니다.

금지:

```
테스트 A가 캠페인 등록

테스트 B가 A의 캠페인을 변경
```

테스트 B를 단독 실행하면 실패합니다.

각 테스트는 필요한 데이터를 직접 준비해야 합니다.

### 64.8 반복 가능성

다음 실행에서 결과가 바뀌는 테스트는 신뢰하기 어렵습니다.

```
assertThat(LocalDate.now())
    .isEqualTo(LocalDate.of(2026, 7, 17));
```

날짜와 시각에 의존하는 Rule은 Clock을 주입해 고정할 수 있습니다.

```
Clock fixedClock =
    Clock.fixed(
        Instant.parse("2026-07-17T00:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );
```

### 64.9 테스트 이름

테스트 이름은 조건과 결과를 표현합니다.

권장:

```
shouldRejectFutureBaseDate

shouldRollbackWhenHistoryInsertFails

shouldReturnExistingResultForSameIdempotencyKey
```

금지:

```
test1
testCreate
testError
```

### 제64장 요약

```
Rule은 규칙을,
Service는 흐름을,
Mapper는 SQL을,
거래 테스트는 TCF 전체를 검증한다.

정상 흐름뿐 아니라
업무 오류·시스템 오류·Timeout·Rollback을 테스트한다.
```

## 제65장. 단위 테스트 작성하기

### 65.1 Rule 테스트

Rule은 외부 의존성이 적어 가장 빠르게 테스트할 수 있습니다.

```
class CmCampaignRuleTest {

    private final CmCampaignRule rule =
        new CmCampaignRule(
            Clock.fixed(
                Instant.parse(
                    "2026-07-17T00:00:00Z"
                ),
                ZoneId.of("Asia/Seoul")
            )
        );

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        CampaignCreateRequest request =
            new CampaignCreateRequest(
                "여름 캠페인",
                "20260831",
                "20260801",
                "MARKETING"
            );

        assertThatThrownBy(
            () -> rule.validateCreateRequest(request)
        )
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(
            "종료일은 시작일보다 빠를 수 없습니다"
        );
    }
}
```

### 65.2 경계값 테스트

문자열 최대길이가 100자라면 다음을 확인합니다.

```
99자
100자
101자
```

날짜 범위가 1년이라면 다음을 확인합니다.

```
364일
365일
366일
```

경계에서 오류가 많이 발생하기 때문입니다.

### 65.3 Service 단위 테스트

Service의 의존성을 Mock으로 대체합니다.

```
@ExtendWith(MockitoExtension.class)
class CmCampaignServiceTest {

    @Mock
    private CmCampaignRule campaignRule;

    @Mock
    private CmCampaignDao campaignDao;

    @Mock
    private CampaignIdGenerator idGenerator;

    @Mock
    private TransactionContext context;

    @InjectMocks
    private CmCampaignService campaignService;

    @Test
    void shouldCreateCampaign() {
        CampaignCreateRequest request =
            new CampaignCreateRequest(
                "여름 캠페인",
                "20260801",
                "20260831",
                "MARKETING"
            );

        given(context.getUserId())
            .willReturn("user01");

        given(campaignDao.existsSameCampaign(
            anyString(),
            anyString(),
            anyString()
        )).willReturn(false);

        given(idGenerator.generate())
            .willReturn("CMP202600001");

        given(campaignDao.insertCampaign(any()))
            .willReturn(1);

        CampaignCreateResponse response =
            campaignService.create(
                request,
                context
            );

        assertThat(response.campaignId())
            .isEqualTo("CMP202600001");

        then(campaignDao)
            .should()
            .insertCampaignHistory(any());
    }
}
```

### 65.4 호출 순서 검증

업무 순서가 중요한 경우 호출 순서를 확인할 수 있습니다.

```
InOrder inOrder =
    inOrder(campaignRule, campaignDao);

inOrder.verify(campaignRule)
    .validateCreateRequest(request);

inOrder.verify(campaignDao)
    .existsSameCampaign(
        anyString(),
        anyString(),
        anyString()
    );

inOrder.verify(campaignDao)
    .insertCampaign(any());
```

단순 구현 세부사항까지 지나치게 검증하면 리팩터링이 어려워질 수 있습니다.

업무적으로 중요한 순서만 검증합니다.

### 65.5 예외 테스트

```
@Test
void shouldRejectDuplicatedCampaign() {
    given(campaignDao.existsSameCampaign(
        anyString(),
        anyString(),
        anyString()
    )).willReturn(true);

    assertThatThrownBy(
        () -> campaignService.create(
            request,
            context
        )
    )
    .isInstanceOf(BusinessException.class)
    .extracting("errorCode")
    .isEqualTo("E-CM-CAM-0005");
}
```

메시지만 확인하기보다 오류코드도 확인해야 합니다.

### 65.6 Handler 테스트

```
@ExtendWith(MockitoExtension.class)
class CmCampaignHandlerTest {

    @Mock
    private CmCampaignFacade campaignFacade;

    @InjectMocks
    private CmCampaignHandler handler;

    @Test
    void shouldRegisterCampaignServiceIds() {
        assertThat(handler.serviceIds())
            .contains(
                "CM.Campaign.selectDetail",
                "CM.Campaign.selectList",
                "CM.Campaign.create",
                "CM.Campaign.update"
            );
    }
}
```

### 65.7 잘못된 단위 테스트

다음 테스트는 실제 기능을 거의 검증하지 않습니다.

```
@Test
void testGetter() {
    CampaignDto dto = new CampaignDto();
    dto.setName("A");

    assertThat(dto.getName()).isEqualTo("A");
}
```

단순 Getter보다 업무 규칙과 업무 흐름을 우선 테스트합니다.

### 65.8 Mock 남용

모든 객체를 Mock으로 만들면 실제 협력구조가 검증되지 않을 수 있습니다.

```
단순 값 객체
→ 실제 객체 사용

업무 Rule
→ 실제 객체 사용 가능

DB·외부 Client
→ Mock

시간·ID 생성기
→ 고정 대역
```

### 65.9 테스트 커버리지

Coverage가 높다고 테스트 품질이 무조건 좋은 것은 아닙니다.

```
Coverage 90%
하지만 Assertion 없음
→ 의미 없는 테스트
```

Coverage는 누락 영역을 찾는 보조지표로 사용합니다.

권장 관점:

```
핵심 Rule
→ 높은 분기 Coverage

DTO Getter
→ 낮은 우선순위

오류·Rollback 경로
→ 반드시 포함
```

### 제65장 요약

```
단위 테스트는 빠르고 독립적이어야 한다.

Rule은 경계값과 업무 오류를,
Service는 업무 흐름과 협력관계를 검증한다.

Coverage 숫자보다
핵심 업무 분기와 오류경로 검증이 중요하다.
```

## 제66장. Mapper·통합·거래 테스트

### 66.1 Mapper 테스트가 필요한 이유

Mock Mapper는 SQL이 실제로 올바른지 확인하지 못합니다.

```
given(mapper.selectCustomer(...))
    .willReturn(data);
```

이 테스트는 다음 문제를 찾지 못합니다.

```
Mapper Namespace 불일치

SQL ID 오타

컬럼명 오류

ResultMap 오류

Oracle 문법 오류

조회조건 누락

Update 영향 건수 오류
```

### 66.2 Mapper 테스트

```
@SpringBootTest
@Transactional
class CmCampaignMapperTest {

    @Autowired
    private CmCampaignMapper campaignMapper;

    @Test
    void shouldSelectCampaignDetail() {
        CampaignDetailData data =
            campaignMapper.selectCampaignDetail(
                "CMP_TEST_001"
            );

        assertThat(data).isNotNull();
        assertThat(data.campaignName())
            .isEqualTo("테스트 캠페인");
    }
}
```

### 66.3 테스트 DB

가능한 선택:

| 방식 | 장점 | 주의 |
| --- | --- | --- |
| H2 | 빠르고 간단 | Oracle 문법 차이 |
| Testcontainers | 실제 DB에 가까움 | 실행환경 필요 |
| 공용 개발 DB | 실제 구조 사용 | 데이터 충돌 |
| 전용 테스트 DB | 안정적 | 운영비용 |

Oracle 전용 SQL이 많다면 H2 테스트만으로 충분하지 않을 수 있습니다.

### 66.4 SQL 영향 건수

변경 SQL은 영향 건수를 검증합니다.

```
@Test
void shouldUpdateOnlyMatchingVersion() {
    int updated =
        campaignMapper.updateCampaign(
            commandWithVersion(3)
        );

    assertThat(updated).isEqualTo(1);
}
```

잘못된 Version:

```
@Test
void shouldNotUpdateWithOldVersion() {
    int updated =
        campaignMapper.updateCampaign(
            commandWithVersion(2)
        );

    assertThat(updated).isZero();
}
```

### 66.5 트랜잭션 Rollback 테스트

```
@SpringBootTest
class CampaignTransactionTest {

    @Autowired
    private CmCampaignFacade campaignFacade;

    @Autowired
    private CmCampaignMapper campaignMapper;

    @Test
    void shouldRollbackWhenHistoryFails() {
        assertThatThrownBy(
            () -> campaignFacade.create(
                requestCausingHistoryFailure(),
                context()
            )
        ).isInstanceOf(SystemException.class);

        CampaignDetailData data =
            campaignMapper.selectCampaignDetail(
                "CMP_ROLLBACK_TEST"
            );

        assertThat(data).isNull();
    }
}
```

단순히 예외 발생만 확인하지 않고 DB가 실제로 Rollback되었는지 확인합니다.

### 66.6 TCF 거래 테스트

거래 테스트는 표준 Endpoint를 통해 요청합니다.

```
@SpringBootTest
@AutoConfigureMockMvc
class CampaignCreateTransactionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateCampaignThroughTcf()
            throws Exception {

        String request = """
        {
          "header": {
            "businessCode": "CM",
            "serviceId": "CM.Campaign.create",
            "transactionCode": "CM-REG-0001",
            "processingType": "REGISTRATION",
            "channelId": "TEST",
            "userId": "test-user",
            "branchId": "001234",
            "idempotencyKey": "TEST-CM-001"
          },
          "body": {
            "campaignName": "테스트 캠페인",
            "startDate": "20260801",
            "endDate": "20260831",
            "campaignType": "MARKETING"
          }
        }
        """;

        mockMvc.perform(
            post("/cm/online")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .header(
                    "Authorization",
                    "Bearer " + testAccessToken()
                )
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.result.resultStatus")
                .value("SUCCESS")
        )
        .andExpect(
            jsonPath("$.body.campaignId")
                .exists()
        );
    }
}
```

### 66.7 STF 검증 테스트

다음 시나리오를 확인합니다.

```
JWT 없음
→ Controller 이전 또는 Filter에서 차단

업무코드 불일치
→ STF 오류

ServiceId 미등록
→ Dispatcher 오류

권한 없음
→ Handler 미실행

거래상태 SUSPENDED
→ Handler 미실행

Timeout 정책 없음
→ 기본정책 또는 등록 오류
```

### 66.8 Handler 미실행 확인

권한 실패 시 Facade가 호출되지 않았는지 검증할 수 있습니다.

```
then(campaignFacade)
    .shouldHaveNoInteractions();
```

공통 검증이 Handler 이전에 동작하는지 확인하는 중요한 테스트입니다.

### 66.9 ETF 응답 테스트

업무 오류:

```
resultStatus = FAIL
errorType = BUSINESS
resultCode = 업무 오류코드
```

시스템 오류:

```
resultStatus = ERROR
errorType = SYSTEM
내부 Stack Trace 미노출
```

Timeout:

```
errorType = TIMEOUT
거래로그 status = TIMEOUT
```

### 66.10 거래로그 검증

거래 완료 후 다음을 확인합니다.

```
GUID 존재

TraceId 존재

ServiceId 일치

시작·종료시각 존재

결과상태 일치

처리시간 존재

오류코드 일치
```

### 66.11 계약 테스트

다른 WAR 호출 계약:

```
요청 필드
응답 필드
필수 여부
오류코드
계약 버전
```

피호출자가 필드를 제거하면 호출자의 계약 테스트가 실패하도록 구성할 수 있습니다.

### 66.12 Stub과 Mock Server

외부 시스템이 없어도 다음 응답을 재현할 수 있습니다.

```
정상 응답

업무 오류

HTTP 500

Connection Reset

Read Timeout

잘못된 JSON

필수필드 누락
```

외부 연계 장애를 실제 기관에서 발생시키지 않고 검증할 수 있습니다.

### 제66장 요약

```
Mapper 테스트는 실제 SQL과 매핑을 검증한다.

거래 테스트는
Filter·STF·Dispatcher·Handler·ETF를 연결해 검증한다.

오류 응답뿐 아니라
Handler 미실행·Rollback·거래로그까지 확인한다.
```

## 제67장. 성능·장애·보안 테스트

### 67.1 기능이 정상이어도 운영에서 실패할 수 있다

한 명이 조회할 때 0.5초가 걸리는 기능도 1,000명이 동시에 호출하면 느려질 수 있습니다.

성능 테스트는 다음을 확인합니다.

```
목표 TPS를 처리하는가?

p95 응답시간을 만족하는가?

Thread와 DB Pool이 고갈되지 않는가?

GC Pause가 과도하지 않은가?

특정 WAR가 자원을 독점하지 않는가?
```

### 67.2 NSIGHT 성능 목표 예

프로젝트 기준 예:

```
전체 사용자
36,000명

동시요청 가정
10%

목표 응답시간
p95 3초 이하
```

정확한 TPS는 업무 시나리오와 사용자 행동모델을 기반으로 산정합니다.

### 67.3 성능 테스트 유형

| 유형 | 목적 |
| --- | --- |
| Baseline | 단일·소량 요청 기준 성능 |
| Load | 예상 정상 부하 |
| Peak | 업무 집중시간 부하 |
| Stress | 한계를 넘겨 장애지점 확인 |
| Spike | 순간적인 급증 |
| Endurance | 장시간 메모리·누수 확인 |
| Capacity | 최대 처리 가능량 산정 |
| Batch | 대량 Job 처리시간 검증 |

### 67.4 테스트 시나리오 구성

실제 사용 비율을 반영합니다.

예:

| 거래 | 비율 |
| --- | --- |
| 고객 요약조회 | 35% |
| 고객 목록조회 | 20% |
| 상품조회 | 15% |
| 캠페인 조회 | 15% |
| 캠페인 등록·변경 | 5% |
| 기타 | 10% |

모든 요청을 한 ServiceId에 집중하면 실제 부하와 다를 수 있습니다.

### 67.5 확인 지표

```
TPS

평균 응답시간

p95·p99 응답시간

오류율

Timeout율

Tomcat Busy Thread

Hikari Active·Pending

DB CPU·I/O

Slow SQL

JVM Heap

GC Pause

WAR별 처리량
```

### 67.6 장애 테스트

다음 장애를 통제된 환경에서 검증합니다.

```
DB Connection 실패

DB 응답 지연

외부 시스템 Timeout

Gateway 한 대 중단

업무 WAR 한 대 중단

Cache 장애

Scheduler 중복실행

Storage 용량 부족

Thread Pool 고갈

JVM 재기동
```

### 67.7 장애 테스트의 질문

```
사용자 요청은 어느 오류코드로 끝나는가?

다른 ServiceId까지 영향받는가?

진행 중 트랜잭션은 Rollback되는가?

L4가 정상 인스턴스로 전환하는가?

미확정 거래가 남는가?

운영 경보가 발생하는가?

복구 후 자동 정상화되는가?
```

### 67.8 보안 테스트

필수 시나리오:

```
JWT 없음

JWT 위조

JWT 만료

잘못된 Audience

권한 없는 ServiceId 호출

Header userId 위조

SQL Injection 문자열

Path Traversal 파일명

허용되지 않은 파일

민감정보 로그 노출

다른 사용자 파일 다운로드
```

### 67.9 입력 변조

예:

```
{
  "customerNo": "' OR '1'='1",
  "pageSize": 999999999,
  "sortField": "CAMPAIGN_ID DESC; DROP TABLE..."
}
```

서버는 DTO Validation, Binding, 화이트리스트로 차단해야 합니다.

### 67.10 Timeout 테스트

```
ServiceId Timeout
3000ms

하위 Client
700ms

SQL
2000ms
```

각 계층의 Timeout이 의도한 순서로 발생하는지 확인합니다.

### 67.11 성능시험 결과 해석

단순히 “평균 1초”라고 기록하지 않습니다.

```
평균 1초
p95 2.8초
p99 8초
```

평균은 좋아 보여도 일부 사용자는 8초를 기다립니다.

운영 목표는 p95·p99와 오류율을 함께 봅니다.

### 제67장 요약

```
성능 테스트는 평균시간만 측정하지 않는다.

TPS·p95·Thread·DB Pool·GC를 함께 본다.

장애 테스트는
오류 응답뿐 아니라 격리·Rollback·복구까지 검증한다.
```

## 제68장. 품질 Gate와 자동검증

### 68.1 품질 Gate란 무엇인가요?

품질 Gate는 다음 단계로 넘어가기 전에 반드시 통과해야 하는 기준입니다.

```
컴파일 실패
→ Test 단계 진입 금지

단위 테스트 실패
→ Artifact 생성 금지

보안 취약점 발견
→ 운영 배포 금지
```

### 68.2 기본 Gate

```
컴파일

단위 테스트

통합 테스트

정적분석

코드 스타일

Coverage

Architecture 규칙

보안 취약점

Artifact 생성

Checksum
```

### 68.3 Architecture Test

코드 의존관계를 자동검증할 수 있습니다.

검증 예:

```
Handler는 Facade만 호출

Handler는 Mapper를 참조하지 않음

Rule은 DAO·Mapper를 참조하지 않음

Service는 Controller에 의존하지 않음

다른 업무 Mapper Import 금지

업무 패키지는 표준 경로 사용
```

개념 예:

```
@AnalyzeClasses(
    packages = "com.nh.nsight.marketing"
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule handlersShouldNotUseMappers =
        noClasses()
            .that()
            .resideInAPackage("..handler..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..mapper..");
}
```

### 68.4 ServiceId 자동검증

검증항목:

```
ServiceId 중복 없음

업무코드 Prefix 일치

Handler 등록 존재

OM Catalog 등록 존재

거래코드 존재

Timeout 정책 존재

권한코드 연결
```

### 68.5 Mapper 자동검증

```
Java Interface와 XML Namespace 일치

Mapper 메서드와 SQL ID 일치

중복 SQL ID 없음

SELECT * 금지

조건 없는 UPDATE·DELETE 금지

검토되지 않은 ${} 사용 금지
```

### 68.6 보안 Gate

```
Private Key Commit 금지

비밀번호·Token 문자열 검사

취약 라이브러리 검사

SQL Injection 위험 검사

민감정보 로그 검사

허용되지 않은 Endpoint 검사
```

### 68.7 Coverage Gate 주의사항

예:

```
전체 Line Coverage 80%
```

하나의 숫자만 강제하면 의미 없는 테스트가 늘어날 수 있습니다.

권장:

```
핵심 Rule 분기 Coverage

신규 코드 Coverage

오류 경로 Coverage

전체 Coverage 추세
```

### 68.8 코드 리뷰 Gate

자동화가 모든 문제를 찾지는 못합니다.

리뷰항목:

```
업무 규칙의 정확성

책임 경계

트랜잭션 범위

동시성

개인정보

성능

운영 추적성

변경 호환성
```

### 68.9 Gate 예외

긴급 장애 수정으로 일부 Gate를 생략해야 할 수 있습니다.

필요 통제:

- 생략 사유
- 승인자
- 영향범위
- 사후 테스트
- 원복 계획
- 기술부채 등록
- 완료기한
예외를 일상적인 절차로 사용하면 안 됩니다.

### 68.10 Definition of Done

개발 완료조건 예:

```
요구사항 추적 완료

설계서 갱신

코드 구현

단위 테스트

통합 테스트

오류 테스트

코드 리뷰

품질 Gate 통과

OM 기준정보 등록

배포·Rollback 절차

운영 모니터링 확인
```

### 제68장 요약

```
품질 Gate는 사람의 기억을 자동규칙으로 바꾼다.

코드 품질뿐 아니라
ServiceId·Mapper·보안·아키텍처 규칙을 검사한다.

Gate 예외는 승인과 사후 조치가 필요하다.
```

## 제69장. Gradle 빌드와 Artifact 관리

### 69.1 빌드란 무엇인가요?

빌드는 소스코드를 실행·배포 가능한 결과물로 만드는 과정입니다.

```
Java Source
→ Compile
→ Test
→ Resource 포함
→ Package
→ WAR·JAR
```

### 69.2 Gradle Wrapper

프로젝트에 포함된 Wrapper를 사용합니다.

Windows:

```
gradlew.bat clean build
```

Linux:

```
./gradlew clean build
```

개발자마다 설치된 Gradle 버전을 직접 사용하면 결과가 달라질 수 있습니다.

### 69.3 멀티모듈 빌드

전체 빌드:

```
./gradlew clean build
```

특정 업무 WAR:

```
./gradlew :sv-service:clean :sv-service:build
```

테스트만:

```
./gradlew :sv-service:test
```

의존 모듈이 변경되었다면 전체 영향 모듈을 빌드해야 합니다.

### 69.4 clean의 의미

기존 Build 결과를 삭제합니다.

```
이전 Class·Resource
→ 삭제

현재 Source
→ 새로 Compile
```

로컬에서는 clean 없이 빠르게 빌드할 수 있지만 Release Build는 깨끗한 환경에서 수행하는 것이 안전합니다.

### 69.5 WAR 구조

일반적인 WAR:

```
sv-service.war
├─ META-INF
├─ WEB-INF
│  ├─ classes
│  │  ├─ Java Class
│  │  ├─ application.yml
│  │  └─ mapper XML
│  └─ lib
│     └─ 의존 JAR
└─ 정적 Resource
```

Mapper XML이 WEB-INF/classes 아래에 포함되는지 확인합니다.

### 69.6 WAR와 JAR

| 구분 | WAR | JAR |
| --- | --- | --- |
| 배포 | 외부 Tomcat 가능 | 독립 실행 가능 |
| 구조 | WEB-INF | 일반 Archive |
| 사용 예 | 업무 WAR | 공통 라이브러리 |
| 서버 | Tomcat 배포 | java -jar 가능 |

NSIGHT가 하나의 Tomcat에 여러 업무 WAR를 배포한다면 업무 모듈은 WAR로 패키징할 수 있습니다.

tcf-core, tcf-util 등 공통 모듈은 JAR로 업무 WAR에 포함될 수 있습니다.

### 69.7 공통 JAR 버전

업무 WAR마다 서로 다른 공통 모듈 버전을 포함하면 동작 차이가 발생할 수 있습니다.

```
sv-service
tcf-core 1.5.0

ic-service
tcf-core 1.4.2
```

독립 배포를 허용할 수 있지만 호환성 매트릭스를 관리해야 합니다.

### 69.8 Artifact 버전

예:

```
sv-service-1.3.0.war
```

버전 구성:

```
Major.Minor.Patch
```

또는 조직 표준:

```
1.3.0+build.20260717.15
```

Artifact에는 다음 Metadata를 연결합니다.

```
Git Commit ID

Build Number

Build Dtm

Branch

Java Version

Dependency Version

Checksum
```

### 69.9 Snapshot과 Release

```
1.3.0-SNAPSHOT
→ 개발 중 변경 가능

1.3.0
→ Release, 변경 금지
```

운영에는 변경 가능한 Snapshot Artifact를 배포하지 않습니다.

### 69.10 Artifact 불변성

권장:

```
CI가 WAR 생성

Artifact Repository 저장

개발·검증·운영이 같은 WAR 사용
```

금지:

```
개발환경용 별도 빌드

검증환경용 다시 빌드

운영환경용 다시 빌드
```

소스는 같아도 빌드시점과 의존성이 달라질 수 있습니다.

### 69.11 환경설정 분리

WAR에는 환경에 공통적인 코드만 포함합니다.

환경별 값:

```
DB URL

계정정보

외부 시스템 URL

Log 경로

Timeout 조정값

키·인증서

운영 기능 Flag
```

는 외부 설정이나 환경변수·Secret으로 주입합니다.

### 69.12 Checksum

Artifact가 이동 중 변경되지 않았는지 확인합니다.

예:

```
SHA-256
```

배포 전 Artifact Repository의 Checksum과 배포 파일을 비교합니다.

### 69.13 SBOM

SBOM은 Artifact에 포함된 라이브러리 목록입니다.

용도:

- 취약 라이브러리 확인
- 라이선스 확인
- 영향 분석
- 보안사고 대응

### 제69장 요약

```
Gradle Wrapper로 재현 가능한 빌드를 수행한다.

WAR에는 Class·Mapper·Resource가 정확히 포함되어야 한다.

한 번 생성한 Release Artifact를
모든 환경에 동일하게 승격한다.
```

## 제70장. CI Pipeline 구성하기

### 70.1 CI란 무엇인가요?

CI는 개발자가 소스를 변경할 때 자동으로 통합하고 검증하는 과정입니다.

```
Commit
→ Build
→ Test
→ Analysis
→ Artifact
```

### 70.2 기본 Pipeline

```
1. Checkout

2. JDK·Gradle 환경 준비

3. Compile

4. Unit Test

5. Integration Test

6. Static Analysis

7. Security Scan

8. Architecture Test

9. WAR Package

10. Checksum·SBOM

11. Artifact Repository Upload
```

### 70.3 Branch와 Pipeline

예:

| Branch | Pipeline |
| --- | --- |
| Feature | Compile·Unit Test |
| Develop | 통합 테스트·정적분석 |
| Release | 전체 Gate·Artifact |
| Main | Release 승인·배포 대상 |
| Hotfix | 긴급 검증·Release |

프로젝트의 Git 정책을 우선합니다.

### 70.4 Pull Request Gate

Merge 전 조건:

```
리뷰 승인

CI 성공

충돌 없음

단위 테스트 통과

신규 취약점 없음

Architecture Rule 통과

설계·추적성 갱신
```

### 70.5 Pipeline 실패 시

```
컴파일 실패
→ 개발자가 수정

테스트 실패
→ 원인 분석

정적분석 실패
→ 코드 개선 또는 승인된 예외

보안 취약점
→ 버전 교체·완화

Artifact Upload 실패
→ Release 생성 중단
```

실패 상태에서 수동으로 WAR를 만들어 운영에 배포하면 안 됩니다.

### 70.6 Pipeline Cache

Gradle Dependency Cache를 사용하면 빌드가 빨라질 수 있습니다.

주의:

- 오래된 Cache
- 손상된 Dependency
- Wrapper 버전 변경
- 의존성 Lock
Release Build는 Cache를 사용하더라도 Dependency Hash를 검증해야 합니다.

### 70.7 Test Report

CI는 테스트 결과를 저장합니다.

```
전체 테스트 수

성공

실패

Skip

Coverage

실행시간

실패 Stack Trace
```

Release 증적에 포함할 수 있습니다.

### 70.8 병렬 Build

모듈과 테스트를 병렬 실행할 수 있습니다.

장점:

- Pipeline 시간 단축
주의:

- 공용 테스트 DB 충돌
- 동일 Port 충돌
- 테스트 데이터 충돌
- Thread·Memory 부족
테스트 독립성이 확보된 뒤 적용합니다.

### 70.9 Secret 관리

CI Pipeline에 다음 값을 직접 작성하지 않습니다.

```
DB_PASSWORD: password123
PRIVATE_KEY: ...
```

CI Secret Store나 Vault를 사용합니다.

로그에 Secret이 출력되지 않도록 마스킹합니다.

### 70.10 Artifact Promotion

```
CI Build
→ Candidate Artifact

개발 검증 통과
→ Test 승격

통합·성능 검증 통과
→ Production Candidate

배포 승인
→ 운영 배포
```

Artifact를 복사하더라도 동일 Checksum을 유지합니다.

### 70.11 Pipeline 권한

| 활동 | 권한 |
| --- | --- |
| Feature Build | 개발자 |
| Release Build | 승인된 Pipeline |
| Artifact 삭제 | 제한 |
| 운영 배포 | 운영 승인 |
| Rollback | 운영·장애 담당 |
| Secret 조회 | 최소 권한 |

### 제70장 요약

```
CI는 Commit마다
빌드·테스트·정적분석을 자동 수행한다.

실패한 Pipeline의 Artifact를
수동 우회해 배포하지 않는다.

Release Artifact와 테스트 증적을 함께 보관한다.
```

## 제71장. CD와 안전한 배포

### 71.1 CD란 무엇인가요?

CD는 검증된 Artifact를 목표 환경에 배포하고 정상 여부를 확인하는 과정입니다.

```
승인된 Artifact
→ 환경설정 확인
→ 배포
→ 기동
→ Health Check
→ Smoke Test
→ 모니터링
```

### 71.2 배포 전 확인

```
Artifact Version

Checksum

대상 서버

대상 WAR

배포 순서

환경설정

DB 변경

OM 기준정보

사용자 영향

Rollback 파일

작업 승인
```

### 71.3 배포 단위

NSIGHT 다중 WAR 예:

```
Tomcat
├─ sv-service.war
├─ ic-service.war
├─ cm-service.war
└─ om-service.war
```

변경 대상이 sv-service뿐이라면 독립 배포를 검토할 수 있습니다.

하지만 공통 JAR나 Tomcat 설정이 변경되면 여러 WAR 영향이 있을 수 있습니다.

### 71.4 일괄 배포

모든 서버를 중지하고 한 번에 배포합니다.

장점:

- 단순함
- 버전 혼재 없음
단점:

- 서비스 중단
- 실패 시 전체 영향
점검시간이 확보된 시스템에 사용할 수 있습니다.

### 71.5 Rolling 배포

서버를 한 대씩 교체합니다.

```
WAS 1 트래픽 제외
→ 배포·검증
→ 트래픽 복귀

WAS 2 반복
```

장점:

- 무중단 가능
- 장애 영향 축소
주의:

- 구·신 버전 혼재
- Session
- DB Schema 호환성
- Cache 정합성
- API 계약 호환성

### 71.6 Blue-Green 배포

```
Blue
현재 운영

Green
신규 버전
```

Green에 배포·검증한 뒤 트래픽을 전환합니다.

장점:

- 빠른 전환·원복
- 운영과 유사한 사전검증
단점:

- 자원 비용
- DB 공유 시 호환성 필요
- 파일·Session·Cache 고려

### 71.7 Canary 배포

일부 트래픽만 신규 버전으로 보냅니다.

```
신규 버전
5%

기존 버전
95%
```

지표가 정상이면 점진적으로 확대합니다.

정교한 Route와 사용자 그룹 통제가 필요합니다.

### 71.8 WAR 배포 절차 예

```
1. 변경 작업 승인

2. 대상 서버 Drain

3. 현재 WAR 백업

4. 신규 WAR 배치

5. Tomcat 재기동 또는 Context 재배포

6. 기동 로그 확인

7. Health Check

8. 핵심 ServiceId Smoke Test

9. Thread·Heap·DB Pool 확인

10. 트래픽 복귀

11. 다음 서버 반복

12. 배포 완료 기록
```

### 71.9 Tomcat 다중 WAR 주의

한 Tomcat을 재기동하면 동일 인스턴스의 모든 WAR에 영향이 있습니다.

```
sv-service만 변경
→ Tomcat 재기동
→ ic·cm·om도 일시 중단
```

배포 독립성이 중요하면 Tomcat 인스턴스 분리 또는 배포 방식 개선을 검토합니다.

### 71.10 Context Path

배포 후 다음을 확인합니다.

```
WAR 이름

Context Path

Apache Proxy Route

Gateway Route

Health Endpoint

업무코드
```

예:

```
sv-service.war
→ /sv
```

WAR 이름 변경으로 Context Path가 바뀌지 않도록 명시 설정을 사용할 수 있습니다.

### 71.11 Health Check

Health Check는 애플리케이션이 요청을 받을 준비가 되었는지 확인합니다.

#### Liveness

```
프로세스가 살아 있는가?
```

#### Readiness

```
업무 요청을 받을 준비가 되었는가?
```

검증 대상:

- Spring Context 기동
- 필수 설정
- DB Connection
- Mapper 등록
- 중요 Client 상태
- Cache 초기화
- Service Catalog Load

### 71.12 Health Check 과도한 의존

모든 외부 시스템이 잠시 느리다고 업무 WAR 전체를 DOWN으로 표시하면 L4가 모든 인스턴스를 제외할 수 있습니다.

```
필수 의존성
→ Readiness 반영

선택 의존성
→ DEGRADED·별도 지표
```

로 구분할 수 있습니다.

### 71.13 Smoke Test

Health Check가 성공해도 업무 거래가 실패할 수 있습니다.

Smoke Test 예:

```
공통 Health

JWT 검증

대표 조회 ServiceId

대표 DB 조회

OM 정책 조회

표준 응답 확인
```

변경 거래는 운영 데이터를 만들지 않는 전용 테스트 거래나 Rollback 가능한 방식으로 수행합니다.

### 71.14 배포 성공 기준

```
WAR 복사 완료
```

가 아닙니다.

정확한 기준:

```
기동 성공

Health 정상

Smoke Test 성공

오류율 정상

p95 정상

Thread·DB Pool 정상

로그 이상 없음

배포 서버 전체 동일 Version
```

### 제71장 요약

```
배포 성공은 WAR 복사가 아니라
기동·Health·거래 검증 완료를 의미한다.

Rolling 배포에서는
구·신 버전과 DB Schema의 호환성을 보장해야 한다.

다중 WAR Tomcat 재기동은
다른 업무에도 영향을 준다.
```

## 제72장. DB 변경과 무중단 호환성

### 72.1 애플리케이션과 DB는 함께 변경된다

다음 변경을 생각해 봅시다.

```
CAMPAIGN_MASTER에
CAMPAIGN_CATEGORY 컬럼 추가
```

신규 코드가 먼저 배포되면 컬럼이 없어 SQL이 실패할 수 있습니다.

DB를 먼저 변경하면 기존 코드가 새 구조와 호환되어야 합니다.

### 72.2 Expand and Contract

무중단 변경에 유용한 방식입니다.

#### Expand

```
신규 컬럼 추가
기존 코드는 계속 동작
```

#### Migrate

```
신규 코드가 새 컬럼 사용
기존 데이터 이관
```

#### Contract

```
더 이상 사용하지 않는 구 컬럼 제거
```

한 번의 배포에서 컬럼 추가·코드 전환·기존 컬럼 삭제를 모두 수행하지 않습니다.

### 72.3 필수 컬럼 추가

다음 변경은 위험합니다.

```
ALTER TABLE CM_CAMPAIGN_MASTER
ADD NEW_COLUMN VARCHAR2(20) NOT NULL;
```

기존 데이터에 값이 없어 실패할 수 있습니다.

단계적 접근:

```
Nullable 컬럼 추가

기본값·데이터 이관

신규 코드 배포

검증

NOT NULL 제약 추가
```

### 72.4 컬럼 이름 변경

DB에서 바로 Rename하면 기존 코드가 실패합니다.

대안:

```
신규 컬럼 추가

구·신 컬럼 병행 기록

데이터 이관

조회 전환

구 컬럼 제거
```

### 72.5 배포 버전 혼재

Rolling 중:

```
WAS 1
신규 코드

WAS 2
기존 코드
```

DB는 두 코드가 동시에 사용할 수 있어야 합니다.

### 72.6 데이터 이관

대량 데이터 이관은 운영 배포와 분리된 Batch로 수행할 수 있습니다.

확인항목:

- 대상 건수
- 처리시간
- Lock
- Undo·Redo
- 중단·재시작
- 검증 SQL
- Rollback
- 온라인 영향

### 72.7 DB Script 관리

DB 변경 Script도 소스와 함께 버전관리합니다.

예:

```
V20260717_01__add_campaign_category.sql
```

운영에서 임의로 SQL을 직접 수정하지 않습니다.

### 72.8 DB Rollback 주의

컬럼 추가는 되돌리기 쉬울 수 있지만 데이터 변환·삭제는 단순 Rollback이 어렵습니다.

```
애플리케이션 Rollback 가능

DB 데이터 변경 Rollback 불가능
```

인 경우가 있습니다.

따라서 배포 전 Backup·복구·보상 계획이 필요합니다.

### 제72장 요약

```
애플리케이션과 DB 변경은
구·신 버전이 함께 동작할 수 있도록 설계한다.

컬럼 추가·데이터 이관·코드 전환·기존 컬럼 제거를
단계적으로 수행한다.
```

## 제73장. Rollback과 장애 복구

### 73.1 Rollback이 필요한 상황

```
기동 실패

Health Check 실패

Smoke Test 실패

시스템 오류율 급증

p95 응답시간 악화

DB Pool 고갈

메모리 누수

중대한 업무 오류

개인정보 노출
```

### 73.2 Rollback 기준

배포 전에 수치로 정의합니다.

예:

```
시스템 오류율
5분간 1% 초과

Timeout율
5분간 0.5% 초과

p95
3초 초과 지속

핵심 ServiceId
Smoke Test 실패

보안
인증 우회 발견
```

운영 상황에 맞게 확정합니다.

### 73.3 애플리케이션 Rollback

```
신규 WAR 트래픽 제외

이전 WAR 복구

Tomcat 기동

Health Check

Smoke Test

트래픽 복귀
```

이전 Artifact와 설정이 즉시 사용 가능해야 합니다.

### 73.4 설정 Rollback

코드는 정상이지만 설정 때문에 장애가 날 수 있습니다.

예:

```
DB URL 오류

Timeout 오설정

Route 오류

권한코드 누락

Cache TTL 오류
```

설정도 버전과 변경이력을 관리해야 합니다.

### 73.5 DB가 변경된 경우

신규 코드가 데이터를 새로운 형식으로 저장했다면 이전 코드가 읽지 못할 수 있습니다.

```
WAR만 이전 버전으로 복구
→ DB 데이터 형식 불일치
```

배포 전부터 하위 호환성을 설계해야 합니다.

### 73.6 Rollback과 Roll Forward

#### Rollback

이전 정상 버전으로 복구합니다.

적합:

- 배포 직후 오류
- 이전 버전 호환 가능
- 원인 분석에 시간 필요

#### Roll Forward

오류를 수정한 새 버전을 배포합니다.

적합:

- DB 변경을 되돌리기 어려움
- 이전 버전과 데이터 호환 불가
- 수정이 작고 명확함

### 73.7 배포 중 생성된 데이터

신규 버전이 일부 데이터를 잘못 생성했다면 WAR Rollback만으로 해결되지 않습니다.

필요 조치:

```
오류 데이터 식별

영향 건수 산정

보상·정정 Script

업무 승인

감사로그

재검증
```

### 73.8 Rollback 테스트

운영 배포 전에 검증환경에서 실제로 수행합니다.

```
신규 배포

Smoke Test

이전 Version Rollback

기동 확인

기존 거래 확인
```

문서만 있고 실행해 보지 않은 Rollback 절차는 신뢰하기 어렵습니다.

### 73.9 Rollback 금지 상황

다음 상태에서 무조건 원복하면 더 큰 문제가 발생할 수 있습니다.

```
신규 Schema로 대량 데이터 저장 완료

외부 시스템 계약이 신규 버전으로 전환

이전 버전이 보안 취약

기존 버전이 신규 Event를 처리하지 못함
```

이 경우 Roll Forward 또는 호환 Patch가 필요합니다.

### 제73장 요약

```
Rollback 기준과 절차는 배포 전에 확정한다.

WAR뿐 아니라 설정·DB·데이터까지 고려한다.

이전 버전이 신규 데이터와 호환되지 않으면
Roll Forward가 더 안전할 수 있다.
```

## 제74장. 운영전환과 Release 관리

### 74.1 운영전환이란 무엇인가요?

운영전환은 개발된 기능을 실제 사용자가 안전하게 사용할 수 있도록 준비하고 책임을 운영조직에 인계하는 과정입니다.

단순 배포보다 넓은 개념입니다.

```
코드

설정

DB

OM 기준정보

모니터링

운영 매뉴얼

장애 대응

권한

교육

비상연락망
```

을 모두 포함합니다.

### 74.2 Release Note

Release별로 다음 내용을 기록합니다.

| 항목 | 내용 |
| --- | --- |
| Release Version | 1.3.0 |
| 배포일시 | 예정 시각 |
| 대상 시스템 | SV·CM |
| 변경 ServiceId | 목록 |
| 신규 기능 | 주요 내용 |
| 오류 수정 | 수정사항 |
| DB 변경 | Script 목록 |
| 설정 변경 | Key 목록 |
| OM 변경 | Catalog·Timeout·권한 |
| 영향 화면 | 화면 ID |
| 테스트 결과 | 증적 위치 |
| Rollback | 이전 Version |
| 알려진 제약 | Known Issue |

### 74.3 운영 인수자료

```
시스템 구성도

ServiceId 목록

업무·거래코드

WAR·Context Path

배포 절차

기동·중지 절차

Health Check

주요 로그 경로

오류코드

Timeout 정책

Batch Schedule

장애 점검순서

Rollback 절차

담당자·비상연락망
```

### 74.4 OM 기준정보 확인

운영전환 전:

```
Service Catalog

거래통제 상태

Timeout

기능권한

오류코드

Batch Job

Scheduler

Cache 정책

외부 Client 정책

파일 보관정책
```

이 코드와 일치해야 합니다.

### 74.5 모니터링 준비

신규 ServiceId에 대해 다음을 확인합니다.

```
호출 건수

성공률

업무 오류율

시스템 오류율

p95

Timeout

Slow SQL

권한 실패

거래통제

Batch 처리건수
```

운영 배포 후 지표가 보이지 않으면 장애를 조기에 발견하기 어렵습니다.

### 74.6 초기 안정화 기간

배포 직후 일정 기간을 집중 모니터링할 수 있습니다.

예:

```
배포 후 2시간
실시간 모니터링

다음 영업일
피크 시간 집중 확인

1주일
오류·성능 추세 확인
```

### 74.7 비상연락 체계

| 장애 유형 | 1차 담당 |
| --- | --- |
| 업무 오류 | 업무 개발 |
| TCF 오류 | Framework |
| WAS·JVM | TA·운영 |
| DB | DBA |
| 인증·권한 | 보안 |
| 외부연계 | EAI·상대 시스템 |
| 파일 Storage | 인프라 |
| Batch | Batch 운영 |

### 74.8 운영 승인

운영전환 승인에 필요한 예:

```
기능 테스트 통과

성능 테스트 통과

보안 테스트 통과

장애·Rollback 테스트

운영 매뉴얼

모니터링 구성

잔여 위험 승인

배포 계획

Rollback 계획
```

### 74.9 잔여 위험

모든 위험을 제거하지 못할 수 있습니다.

예:

```
외부기관 성능시험 제한

일부 Batch 대량 데이터 미검증

특정 브라우저 제약

임시 Timeout 상향
```

잔여 위험은 숨기지 않고 다음을 기록합니다.

```
위험 내용

영향

발생 가능성

임시 통제

담당자

해결기한
```

### 74.10 운영전환 완료기준

```
Release 배포 성공

Health·Smoke Test 성공

모니터링 정상

운영자 인수 완료

장애 연락체계 확인

Rollback Artifact 준비

잔여 위험 승인

사용자 공지 완료
```

### 제74장 요약

```
운영전환은 코드 배포만을 의미하지 않는다.

운영 기준정보·모니터링·장애절차·담당체계를
함께 인계해야 한다.

Release Note와 잔여 위험을 명확히 기록한다.
```

## 3. 목표 아키텍처

```
[개발자]
    │
    │ Commit / Pull Request
    ▼
[Source Repository]
    │
    ▼
[CI Pipeline]
 ├─ Compile
 ├─ Unit Test
 ├─ Integration Test
 ├─ Architecture Test
 ├─ Static Analysis
 ├─ Security Scan
 ├─ Coverage
 └─ WAR Package
    │
    ▼
[Artifact Repository]
 Version
 Checksum
 SBOM
 Test Report
    │
    ▼
[Release Approval]
    │
    ▼
[CD Pipeline]
 ├─ 환경설정 검증
 ├─ DB Script
 ├─ OM 기준정보
 ├─ WAS Drain
 ├─ WAR 배포
 ├─ Health Check
 ├─ Smoke Test
 └─ Metric 확인
    │
    ├─────────────┐
    ▼             ▼
[배포 성공]    [Rollback]
    │             │
    ▼             ▼
[운영 모니터링] [이전 Artifact]
```

## 4. 표준 형식

### 4.1 Build Metadata

```
{
  "artifactName": "sv-service",
  "version": "1.3.0",
  "buildNumber": "20260717.15",
  "gitCommit": "a7d92f1",
  "branch": "release/1.3.0",
  "javaVersion": "21",
  "buildDtm": "2026-07-17T14:30:00+09:00",
  "checksum": "sha256:..."
}
```

### 4.2 배포 실행정보

```
{
  "releaseId": "REL20260717001",
  "artifact": "sv-service-1.3.0.war",
  "environment": "PROD",
  "targetServers": [
    "sv-was-01",
    "sv-was-02"
  ],
  "deploymentType": "ROLLING",
  "status": "SUCCESS",
  "startedDtm": "2026-07-17T22:00:00+09:00",
  "completedDtm": "2026-07-17T22:18:00+09:00"
}
```

### 4.3 배포 검증 결과

```
{
  "releaseId": "REL20260717001",
  "health": "UP",
  "smokeTests": {
    "SV.Customer.selectSummary": "SUCCESS",
    "SV.Customer.selectList": "SUCCESS"
  },
  "metrics": {
    "systemErrorRate": 0.0,
    "timeoutRate": 0.0,
    "p95Ms": 420
  },
  "rollbackRequired": false
}
```

## 5. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| Source Repository | Branch, Commit, Tag |
| CI Runner | JDK, Gradle |
| Test Framework | Unit·Integration·Transaction |
| Quality Server | 정적분석·Coverage |
| Artifact Repository | Version·Checksum |
| Release Registry | Release ID·승인 |
| CD Runner | 환경별 배포 |
| Configuration Store | 환경설정 |
| Secret Store | 비밀번호·키 |
| DB Migration | Script Version |
| Health Check | Liveness·Readiness |
| Smoke Test | 핵심 ServiceId |
| Rollback Store | 이전 Artifact |
| Monitoring | Metric·Log·Alert |

## 6. 책임 경계와 RACI

| 활동 | DEV | AA | FW | QA | SEC | DBA | OPS | PMO |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 단위 테스트 | A/R | C | C | C | I | I | I | I |
| 거래 테스트 | R | C | C | A/C | C | C | I | I |
| 성능 테스트 | C | C | C | A/R | I | C | C | I |
| 보안 테스트 | C | C | C | C | A/R | I | C | I |
| 품질 Gate | C | A | R | C | C | I | I | I |
| Release Build | C | C | R | C | C | I | I | I |
| DB Script | C | C | I | C | I | A/R | C | I |
| 배포 계획 | C | C | C | C | C | C | A/R | C |
| 운영 배포 승인 | I | C | C | C | C | C | A/R | A/C |
| Rollback | C | C | C | C | C | C | A/R | I |
| 운영 인수 | C | C | C | C | C | C | A/R | C |
| 잔여 위험 승인 | C | A/C | C | C | C | C | C | A/R |

```
R = 수행
A = 최종 책임
C = 협의
I = 공유
```

## 7. 정상 처리 흐름

### 7.1 개발·검증 흐름

```
1. 개발자가 Feature Branch에서 구현

2. 단위 테스트 작성

3. Local Build 성공

4. Pull Request 생성

5. CI Compile·Test·Analysis

6. 코드 리뷰

7. Develop Merge

8. 통합·거래 테스트

9. Release Branch 생성

10. 전체 품질 Gate

11. Release Artifact 생성

12. Artifact Repository 저장
```

### 7.2 운영 배포 흐름

```
1. Release 승인

2. 대상 Artifact Checksum 확인

3. DB·설정·OM 변경 확인

4. 대상 서버 트래픽 제외

5. 기존 WAR 백업

6. 신규 WAR 배포

7. 기동 로그 확인

8. Readiness 확인

9. Smoke Test

10. Metric 확인

11. 트래픽 복귀

12. 전체 서버 반복

13. 안정화 모니터링

14. Release 완료
```

## 8. 오류·Timeout·장애 흐름

### 8.1 CI 테스트 실패

```
단위 테스트 실패
→ Merge 차단
→ 개발자 수정
→ Pipeline 재실행
```

### 8.2 Artifact 생성 실패

```
WAR Package 실패
→ Release 생성 안 함
→ 운영 배포 불가
```

### 8.3 기동 실패

```
신규 WAR 배포
→ Spring Context 실패
→ 트래픽 복귀 금지
→ 기동 로그 확보
→ Rollback
```

### 8.4 Health 실패

```
프로세스는 살아 있음
→ DB 연결 실패
→ Readiness DOWN
→ L4 투입 금지
```

### 8.5 Smoke Test 실패

```
Health UP
→ 대표 ServiceId 실패
→ 배포 실패 판단
→ 원인분석 또는 Rollback
```

### 8.6 배포 후 오류율 급증

```
트래픽 복귀
→ SYSTEM_ERROR 급증
→ 배포 중단
→ 신규 서버 제외
→ Rollback 판단
```

## 9. 정상 예시

```
Release
1.3.0

대상
sv-service

Build
전체 테스트 성공

Artifact
sv-service-1.3.0.war

Checksum
정상

배포 방식
Rolling

서버
2대 순차 배포

Health
UP

Smoke Test
SV.Customer.selectSummary 성공

p95
450ms

오류율
0%

결과
Release 성공
```

## 10. 금지 예시

### 10.1 테스트 실패 Artifact 배포

```
Pipeline FAILED
→ 개발자가 로컬 WAR 생성
→ 운영 배포
```

### 10.2 환경별 재빌드

```
개발 WAR
검증 WAR
운영 WAR
```

를 각각 별도 빌드합니다.

### 10.3 운영 서버 직접 수정

```
운영 WAR 내부 Class 교체

운영 application.yml 직접 수정

운영 Mapper XML 직접 수정
```

### 10.4 Rollback Artifact 없음

이전 WAR와 설정을 준비하지 않고 배포합니다.

### 10.5 Health만 확인

```
/health = UP
→ 업무 거래 확인 없이 완료
```

### 10.6 모든 서버 동시 교체

무중단이 필요한데 모든 서버를 동시에 내립니다.

### 10.7 DB 호환성 없는 Rolling

신규 코드만 사용할 수 있는 Schema로 변경한 뒤 구 버전과 혼재합니다.

## 11. 연계 규칙

```
Source Repository
→ CI Pipeline

CI Pipeline
→ Artifact Repository

Artifact Repository
→ CD Pipeline

CD Pipeline
→ Configuration·Secret Store

CD Pipeline
→ Tomcat·WAS

CD Pipeline
→ Health·Smoke Test

배포 결과
→ Release Registry·OM

운영 Metric
→ 모니터링·Rollback 판단
```

배포도 수동 파일 전달이 아니라 추적 가능한 시스템 간 연계로 봅니다.

## 12. 데이터 및 상태관리

### 12.1 Pipeline 상태

```
QUEUED
→ RUNNING
→ SUCCESS

RUNNING
→ FAILED

RUNNING
→ CANCELLED
```

### 12.2 Release 상태

```
DRAFT
→ TESTING
→ READY
→ APPROVED
→ DEPLOYING
→ DEPLOYED

DEPLOYING
→ FAILED
→ ROLLING_BACK
→ ROLLED_BACK
```

### 12.3 서버 배포 상태

```
PENDING
→ DRAINING
→ DEPLOYING
→ STARTING
→ HEALTHY
→ IN_SERVICE

STARTING
→ FAILED
```

## 13. 성능·용량·확장성

| 영역 | 기준 |
| --- | --- |
| CI Runner | 병렬 Build 자원 |
| Test DB | 테스트 격리 |
| Artifact | 보관기간·용량 |
| Deployment | 동시 배포 서버 수 |
| Tomcat | 기동시간 |
| Health | 과도한 DB 부하 방지 |
| Smoke Test | 최소 핵심 거래 |
| Log | 배포로그 보관 |
| Release | 버전 수명주기 |
| Rollback | 이전 Artifact 즉시 접근 |
| Security Scan | Pipeline 시간 |
| Test | 중요도별 병렬화 |

## 14. 보안·개인정보·감사

```
CI·CD Secret을 소스에 저장하지 않는다.

운영 배포 권한을 개발자 개인계정에 과도하게 부여하지 않는다.

Artifact Checksum을 검증한다.

배포자·승인자·시각·대상을 감사한다.

운영 서버 직접 수정은 비상절차 외 금지한다.

테스트 데이터에 실제 개인정보를 사용하지 않는다.

보안 취약점 Gate를 운영 배포 전에 수행한다.

Private Key와 인증서는 Secret Store를 통해 배포한다.
```

## 15. 운영·모니터링·장애 대응

운영 배포 시 확인 지표:

| 지표 | 목적 |
| --- | --- |
| 기동 성공률 | 배포 상태 |
| Health | 요청 가능 여부 |
| Smoke Test | 업무 기능 |
| 시스템 오류율 | 결함 탐지 |
| Timeout율 | 성능 이상 |
| p95 | 응답시간 |
| Busy Thread | Thread 이상 |
| DB Pool Pending | DB 대기 |
| Heap·GC | 메모리 문제 |
| WAR Version | 서버별 버전 |
| 배포 소요시간 | 운영 개선 |
| Rollback 시간 | 복구 역량 |

## 16. 자동검증 및 품질 Gate

| Gate | 기준 |
| --- | --- |
| Compile | 오류 0건 |
| Unit Test | 전부 성공 |
| Integration Test | 핵심 SQL 성공 |
| Transaction Test | 핵심 ServiceId 성공 |
| Coverage | 프로젝트 기준 |
| Architecture | 계층 위반 없음 |
| Mapper | Namespace·SQL ID 일치 |
| Security | 중대 취약점 없음 |
| Secret | 노출 0건 |
| ServiceId | 중복·누락 없음 |
| OM | Catalog·Timeout·권한 등록 |
| Artifact | Release Version·Checksum |
| DB | Script 승인·호환성 |
| Rollback | 이전 Artifact 존재 |
| Release | 승인 완료 |

## 17. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| REL-001 | Rule 단위 테스트 | 업무 규칙 검증 |
| REL-002 | Service Mock 테스트 | 호출 순서 검증 |
| REL-003 | Mapper 실제 SQL | 정상 매핑 |
| REL-004 | Update Version 불일치 | 0건 처리 |
| REL-005 | 이력 저장 실패 | 전체 Rollback |
| REL-006 | 권한 없는 거래 | Handler 미실행 |
| REL-007 | 거래통제 | Handler 미실행 |
| REL-008 | ServiceId Timeout | 표준 Timeout |
| REL-009 | 계약 필드 누락 | 계약 오류 |
| REL-010 | 목표 Load | p95 기준 만족 |
| REL-011 | DB 장애 | 격리·경보 |
| REL-012 | JWT 위조 | 인증 차단 |
| REL-013 | SQL Injection | Binding으로 차단 |
| REL-014 | Architecture 위반 | CI 실패 |
| REL-015 | Mapper Namespace 오류 | CI·통합 테스트 실패 |
| REL-016 | Release WAR 생성 | Artifact 저장 |
| REL-017 | Checksum 불일치 | 배포 차단 |
| REL-018 | 설정 누락 | 배포 전 검증 실패 |
| REL-019 | 신규 WAR 기동 실패 | Rollback |
| REL-020 | Health DOWN | 트래픽 투입 금지 |
| REL-021 | Smoke Test 실패 | 배포 실패 |
| REL-022 | Rolling 구·신 혼재 | 계약 호환 |
| REL-023 | DB Schema 구 버전 | 기존 WAR 정상 |
| REL-024 | 배포 후 오류 급증 | 자동·수동 Rollback |
| REL-025 | 이전 Artifact Rollback | 정상 복구 |
| REL-026 | 운영 서버 버전 비교 | 전체 동일 |
| REL-027 | Secret 로그 검사 | 미노출 |
| REL-028 | 승인 없는 운영 배포 | 권한 차단 |
| REL-029 | Release Note 확인 | 변경 추적 |
| REL-030 | 운영 인수 | 체크리스트 완료 |

## 18. 제9부 체크리스트

### 18.1 테스트

| 점검 항목 | 확인 |
| --- | --- |
| Rule 단위 테스트가 있는가? | □ |
| Service 흐름 테스트가 있는가? | □ |
| Mapper 실제 SQL 테스트가 있는가? | □ |
| 정상 거래 테스트가 있는가? | □ |
| 업무 오류 테스트가 있는가? | □ |
| 시스템 오류 테스트가 있는가? | □ |
| Timeout 테스트가 있는가? | □ |
| Rollback을 DB 결과로 확인하는가? | □ |
| 테스트가 독립적이고 반복 가능한가? | □ |

### 18.2 품질 Gate

| 점검 항목 | 확인 |
| --- | --- |
| Compile Gate가 있는가? | □ |
| 테스트 실패 시 Merge가 차단되는가? | □ |
| Architecture Rule을 검사하는가? | □ |
| ServiceId 중복을 검사하는가? | □ |
| Mapper 정합성을 검사하는가? | □ |
| 보안 취약점을 검사하는가? | □ |
| Secret 노출을 검사하는가? | □ |
| Gate 예외 승인절차가 있는가? | □ |

### 18.3 Build·Artifact

| 점검 항목 | 확인 |
| --- | --- |
| Gradle Wrapper를 사용하는가? | □ |
| Release Build가 깨끗한 환경에서 실행되는가? | □ |
| WAR에 Mapper XML이 포함되는가? | □ |
| Artifact Version이 명확한가? | □ |
| Commit ID가 기록되는가? | □ |
| Checksum이 생성되는가? | □ |
| 동일 Artifact를 모든 환경에 사용하는가? | □ |
| Snapshot을 운영에 배포하지 않는가? | □ |

### 18.4 CI

| 점검 항목 | 확인 |
| --- | --- |
| Commit 이후 자동 Build되는가? | □ |
| Unit·Integration Test가 실행되는가? | □ |
| 정적분석과 보안검사가 실행되는가? | □ |
| 테스트 Report가 보관되는가? | □ |
| 실패 Pipeline Artifact가 배포되지 않는가? | □ |
| Secret이 안전하게 관리되는가? | □ |
| Release Artifact가 Repository에 저장되는가? | □ |

### 18.5 배포

| 점검 항목 | 확인 |
| --- | --- |
| 배포 대상 서버가 명확한가? | □ |
| 배포 전 Checksum을 확인하는가? | □ |
| 기존 WAR가 백업되는가? | □ |
| 환경설정이 검증되는가? | □ |
| Health Check가 있는가? | □ |
| Smoke Test가 있는가? | □ |
| 모든 서버 Version을 확인하는가? | □ |
| Rolling 중 구·신 버전이 호환되는가? | □ |

### 18.6 Rollback

| 점검 항목 | 확인 |
| --- | --- |
| Rollback 기준이 수치화되어 있는가? | □ |
| 이전 Artifact가 즉시 준비되는가? | □ |
| 이전 설정이 보관되는가? | □ |
| DB 변경의 복구 가능성을 확인했는가? | □ |
| Rollback 절차를 실제 테스트했는가? | □ |
| Roll Forward 조건이 정의되었는가? | □ |
| 배포 중 생성 데이터 처리방안이 있는가? | □ |

### 18.7 운영전환

| 점검 항목 | 확인 |
| --- | --- |
| Release Note가 있는가? | □ |
| ServiceId 변경목록이 있는가? | □ |
| OM 기준정보가 등록되었는가? | □ |
| 모니터링이 구성되었는가? | □ |
| 장애 대응절차가 있는가? | □ |
| 비상연락망이 있는가? | □ |
| 잔여 위험이 승인되었는가? | □ |
| 운영 인수 증적이 있는가? | □ |

## 19. 변경·호환성·폐기 관리

### 19.1 테스트 변경

업무 요구가 바뀌면 코드만 수정하지 않고 테스트도 변경합니다.

```
요구사항

설계

코드

테스트

운영 기준
```

이 다섯 영역이 함께 변경되어야 합니다.

### 19.2 Pipeline 변경

CI/CD Pipeline도 운영 시스템입니다.

변경 시 확인:

```
Runner Version

JDK Version

Gradle Version

보안 Plugin

Artifact 경로

Secret

운영 배포권한
```

Pipeline 변경 자체도 리뷰와 테스트가 필요합니다.

### 19.3 Java·Framework Upgrade

Java나 Spring Boot를 변경하면 다음을 검증합니다.

```
Compile

전체 테스트

Tomcat 호환성

JDBC Driver

MyBatis

보안 라이브러리

JVM 옵션

성능

WAR 배포
```

### 19.4 Artifact 폐기

오래된 Artifact를 무기한 보관할 필요는 없지만, 운영 복구에 필요한 버전은 유지해야 합니다.

예:

```
현재 운영 Version

직전 정상 Version

법적·감사 보관 Version

장기지원 Version
```

을 구분합니다.

### 19.5 ServiceId 폐기

ServiceId 폐기 절차:

```
ACTIVE
→ DEPRECATED
→ 호출자 확인
→ DISABLED
→ 모니터링
→ 코드·OM·테스트 제거
```

사용 중인 ServiceId를 코드에서 먼저 삭제하면 안 됩니다.

## 20. 시사점

### 20.1 핵심 아키텍처 판단

제9부의 핵심은 다음과 같습니다.

```
개발
= 코드 작성

완료
= 테스트·품질·배포·복구 준비
```

안전한 Release는 다음 조건을 가집니다.

```
재현 가능한 Build

자동화된 Test

승인된 Artifact

추적 가능한 Deployment

검증 가능한 Health·Smoke Test

준비된 Rollback
```

### 20.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 정상 시나리오만 테스트 | 오류 경로 장애 |
| Mock 테스트만 사용 | SQL·설정 오류 누락 |
| Coverage 숫자만 추구 | 의미 없는 테스트 |
| 수동 WAR 생성 | 재현 불가 |
| 환경마다 재빌드 | Artifact 차이 |
| Secret 소스 저장 | 보안사고 |
| 운영 직접 수정 | 변경 추적 불가 |
| Health만 확인 | 업무 오류 미탐지 |
| 구·신 DB 비호환 | Rolling 장애 |
| Rollback 미검증 | 복구 지연 |
| 다중 WAR Tomcat 재기동 | 다른 업무 중단 |
| Release Note 누락 | 영향 추적 불가 |
| 모니터링 미준비 | 장애 발견 지연 |

### 20.3 우선 보완 과제

```
1. 계층별 테스트 표준 수립

2. 핵심 ServiceId 거래 테스트 구축

3. Architecture Test 적용

4. Mapper·SQL 자동검증

5. Gradle Release Build 표준화

6. Artifact Repository와 Checksum

7. CI 품질 Gate 적용

8. 환경설정·Secret 외부화

9. 배포 Health·Smoke Test 자동화

10. Rolling·Rollback 절차 검증

11. DB Expand·Contract 적용

12. 운영전환 체크리스트와 Release Note 표준화
```

### 20.4 중장기 발전 방향

```
수동 테스트
→ 자동 단위·거래 테스트

수동 빌드
→ CI 품질 Gate

수동 배포
→ 승인형 CD

일괄 배포
→ Rolling·Blue-Green

사후 장애 확인
→ 자동 Metric 검증

수동 Rollback
→ 검증된 자동 Rollback

단순 Release Note
→ ServiceId·DB·설정 통합 추적
```

자동 Rollback은 단순 오류 한 건이 아니라 오류율·응답시간·핵심 거래 결과를 종합하여 안전하게 판단해야 합니다.

## 21. 마무리말

제9부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

```
단위 테스트
= 작은 업무 규칙과 흐름 검증

통합 테스트
= DB·Mapper·Spring 연결 검증

거래 테스트
= 표준 요청부터 ETF 응답까지 검증

품질 Gate
= 다음 단계로 넘어갈 수 있는 조건

CI
= 소스 변경의 자동 검증

CD
= 검증된 Artifact의 안전한 배포

Rollback
= 이전 정상 상태로 복구
```

NSIGHT TCF의 안전한 Release 흐름은 다음과 같습니다.

```
요구사항
→ 설계
→ 코드
→ 단위 테스트
→ 통합·거래 테스트
→ 품질 Gate
→ WAR 생성
→ Artifact 저장
→ 승인
→ 배포
→ Health Check
→ Smoke Test
→ 모니터링
→ 완료 또는 Rollback
```

초보 개발자가 기능 구현을 마친 뒤 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
정상 흐름만 테스트한 것은 아닌가?

업무 오류와 시스템 오류도 검증했는가?

SQL이 실제 DB에서 실행되는가?

Rollback 후 데이터가 남지 않는가?

권한 실패 시 Handler가 실행되지 않는가?

같은 소스로 언제든 동일 WAR를 만들 수 있는가?

개발·검증·운영에 같은 Artifact를 사용하는가?

환경설정과 Secret이 코드에서 분리되어 있는가?

배포 후 Health와 ServiceId 거래를 확인하는가?

구·신 버전이 동시에 동작할 수 있는가?

오류율이 증가하면 언제 Rollback할 것인가?

이전 WAR와 설정이 즉시 준비되어 있는가?

운영자가 Release의 변경내용과 장애 대응방법을 알고 있는가?
```

이 질문에 답할 수 있다면 단순히 코드를 작성하는 개발자를 넘어, 자신이 만든 기능을 검증하고 안전하게 운영환경에 전달하며 장애 발생 시 복구할 수 있는 개발자가 될 수 있습니다.

