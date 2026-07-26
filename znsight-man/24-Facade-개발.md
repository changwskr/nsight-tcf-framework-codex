# 24. Facade 개발

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 24. Facade 개발 기준

### 24.1 도입 전 안내말

NSIGHT TCF Framework에서 Facade는 Handler와 업무 Service 사이에서 하나의 업무 유스케이스를 조정하는 계층이다.
Handler가 serviceId 기준으로 호출되는 업무 진입 Adapter라면, Facade는 해당 ServiceId가 수행해야 할 업무 흐름을 정리하고, 필요한 Service들을 호출하여 최종 업무 결과를 반환하는 역할을 한다.
```text
TransactionHandler
↓
Facade
↓
Service
↓
Rule
↓
DAO / Mapper
```

Facade는 업무 로직을 모두 넣는 곳이 아니다.Facade는 업무 처리 순서와 유스케이스 흐름을 조정하는 곳이다.

### 24.2 Facade 개발 결론

NSIGHT Facade는 다음 기준으로 개발한다.
| 구분 | 기준 |
| --- | --- |
| 역할 | ServiceId 단위 유스케이스 흐름 조정 |
| 위치 | 업무 WAR 내부 application.facade 패키지 |
| 호출 주체 | TransactionHandler |
| 호출 대상 | Service |
| 기본 관계 | Handler 1개 → Facade Method 1개 |
| 업무 로직 | 상세 업무 판단은 Rule 또는 Service에 위임 |
| DB 접근 | 직접 접근 금지 |
| Mapper 호출 | 직접 호출 금지 |
| DTO 변환 | 필요 시 Request → Command/Query 변환 가능 |
| 트랜잭션 | 유스케이스 단위 트랜잭션 경계 설정 가능 |
| 외부 연동 | 직접 HTTP 호출 금지, Integration Client 또는 Service 사용 |
| 응답 전문 조립 | 금지, ETF 또는 공통 Response Builder 책임 |

핵심 원칙은 다음이다.
Facade는 업무 흐름을 조정한다.
Service는 업무 처리를 수행한다.
Rule은 업무 규칙을 판단한다.
DAO/Mapper는 DB에 접근한다.

### 24.3 Facade의 위치

Facade는 업무 WAR의 application.facade 패키지에 둔다.
sv-service
```text
 └─ src/main/java
    └─ com.nh.nsight.marketing.sv
       ├─ entry
       │  ├─ handler
       │  └─ dto
       │
       ├─ application
       │  ├─ facade
       │  │  └─ SvCustomerFacade.java
       │  ├─ service
       │  │  └─ SvCustomerService.java
       │  ├─ rule
       │  │  └─ SvCustomerRule.java
       │  └─ dto
       │     ├─ command
       │     ├─ query
       │     └─ result
       │
       └─ persistence
          ├─ dao
          └─ mapper
```

| 패키지 | 역할 |
| --- | --- |
| entry.handler | ServiceId별 업무 진입 |
| application.facade | 유스케이스 흐름 조정 |
| application.service | 업무 처리 |
| application.rule | 업무 규칙 검증 |
| persistence.dao | DB 접근 |
| persistence.mapper | SQL 매핑 |

### 24.4 Facade 기본 작성 예시

SV 고객 요약 조회 Facade 예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {
    private final SvCustomerService svCustomerService;
    public SvCustomerSummaryResponse selectCustomerSummary(
            SvCustomerSummaryRequest request,
            TransactionContext context
    ) {
        return svCustomerService.selectCustomerSummary(request, context);
    }
}
```

이 Facade는 다음 역할만 수행한다.
| 순서 | 처리 |
| --- | --- |
| 1 | Handler로부터 Request DTO와 TransactionContext 수신 |
| 2 | 필요한 Service 호출 |
| 3 | Service 결과를 Handler로 반환 |

단순 조회 거래에서는 Facade가 매우 얇아도 된다.Facade가 얇은 것은 문제가 아니다. 오히려 Handler와 Service 사이의 표준 진입 구조를 유지하는 것이 중요하다.

### 24.5 Facade 명명 규칙

Facade 이름은 다음 형식을 사용한다.
{BusinessCode}{Domain}Facade

예시는 다음과 같다.
| 업무 | 도메인 | Facade명 |
| --- | --- | --- |
| SV | Customer | SvCustomerFacade |
| CM | Campaign | CmCampaignFacade |
| OM | User | OmUserFacade |
| MG | Message | MgMessageFacade |
| UD | File | UdFileFacade |
| EB | Batch | EbBatchFacade |

금지 예시는 다음과 같다.
| 금지 이름 | 문제점 | 권장 이름 |
| --- | --- | --- |
| CommonFacade | 책임 범위 불명확 | 도메인별 Facade 분리 |
| ProcessFacade | 의미 없음 | CmCampaignFacade |
| SvFacade | 업무 범위가 너무 넓음 | SvCustomerFacade |
| TempFacade | 운영 반영 위험 | 명확한 도메인명 사용 |
| CustomerServiceFacadeImpl | 이름 과도 | SvCustomerFacade |

### 24.6 Handler와 Facade 관계

Handler는 반드시 Facade를 호출한다.
```text
Handler
   ↓
Facade
   ↓
Service
```

Handler와 Facade의 관계는 다음 기준을 따른다.
| 구분 | 기준 |
| --- | --- |
| Handler | ServiceId별 진입 Adapter |
| Facade | ServiceId 유스케이스 흐름 조정 |
| 호출 방향 | Handler → Facade |
| 반환값 | 업무 Response DTO |
| Header 정보 | TransactionContext로 전달 |
| 업무 로직 | Facade 이하 계층에서 처리 |

예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler implements TransactionHandler {
    private final SvCustomerFacade svCustomerFacade;
    private final TcfRequestValidator tcfRequestValidator;
    private final ObjectMapper objectMapper;
    @Override
    public String serviceId() {
        return "SV.Customer.selectSummary";
    }
    @Override
    public Object handle(StandardRequest<?> request, TransactionContext context) {
        SvCustomerSummaryRequest body =
                objectMapper.convertValue(
                        request.getBody(),
                        SvCustomerSummaryRequest.class
                );
        tcfRequestValidator.validate(body);
        return svCustomerFacade.selectCustomerSummary(body, context);
    }
}
```

Facade는 다음과 같이 받는다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {
    private final SvCustomerService svCustomerService;
    public SvCustomerSummaryResponse selectCustomerSummary(
            SvCustomerSummaryRequest request,
            TransactionContext context
    ) {
        return svCustomerService.selectCustomerSummary(request, context);
    }
}
```

### 24.7 Facade Method 작성 기준

Facade Method는 ServiceId의 Action과 맞춘다.
| ServiceId | Handler | Facade Method |
| --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerSummaryHandler | selectCustomerSummary() |
| SV.Customer.selectDetail | SvCustomerDetailHandler | selectCustomerDetail() |
| CM.Campaign.create | CmCampaignCreateHandler | createCampaign() |
| CM.Campaign.update | CmCampaignUpdateHandler | updateCampaign() |
| OM.User.update | OmUserUpdateHandler | updateUser() |
| MG.Message.send | MgMessageSendHandler | sendMessage() |
| UD.File.prepareDownload | UdFilePrepareDownloadHandler | prepareDownload() |

Facade Method 작성 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 메서드명은 업무 행위가 드러나야 함 | selectCustomerSummary, updateUser |
| Request DTO를 명확히 받음 | SvCustomerSummaryRequest |
| TransactionContext를 함께 받음 | 사용자, 지점, 채널, 추적 정보 전달 |
| 반환 타입은 Response DTO | SvCustomerSummaryResponse |
| Map<String,Object> 반환 지양 | 타입 안정성 확보 |
| Object 반환 지양 | 계약 불명확 방지 |

### 24.8 Facade에서 해야 하는 일

Facade에서 해야 하는 일은 다음과 같다.
| 해야 하는 일 | 설명 |
| --- | --- |
| 유스케이스 흐름 조정 | 여러 Service 호출 순서 관리 |
| Service 호출 | 업무 처리를 Service에 위임 |
| TransactionContext 전달 | 사용자, 지점, 채널, GUID 정보 전달 |
| Command/Query 변환 | 필요 시 Request DTO를 내부 DTO로 변환 |
| 트랜잭션 경계 설정 | 유스케이스 단위 원자성 보장 |
| 외부 연동 흐름 조정 | Integration Service 또는 Client 호출 순서 관리 |
| 최종 Response DTO 반환 | Handler로 업무 결과 반환 |

예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class CmCampaignFacade {
    private final CmCampaignService cmCampaignService;
    private final CmCampaignApprovalService cmCampaignApprovalService;
    @Transactional
    public CmCampaignCreateResponse createCampaign(
            CmCampaignCreateRequest request,
            TransactionContext context
    ) {
        CmCampaignCreateCommand command =
                CmCampaignCreateCommand.from(request, context);
        CmCampaignCreateResult result =
                cmCampaignService.createCampaign(command);
        cmCampaignApprovalService.createInitialApproval(result.getCampaignId(), context);
        return CmCampaignCreateResponse.from(result);
    }
}
```

이 예시에서 Facade는 직접 DB에 접근하지 않고, Service 호출 순서만 조정한다.

### 24.9 Facade에서 하지 말아야 하는 일

Facade에는 상세 업무 규칙이나 DB 접근 코드를 넣지 않는다.
| 금지 사항 | 사유 | 권장 위치 |
| --- | --- | --- |
| DB 직접 조회 | 계층 책임 위반 | DAO / Mapper |
| Mapper 직접 호출 | DAO 계층 우회 | DAO |
| SQL 작성 | Persistence 책임 침범 | Mapper XML |
| 상세 업무 규칙 판단 | Facade 비대화 | Rule |
| 공통 Header 검증 | STF 책임 | STF |
| 권한 검증 직접 구현 | Security / Rule 책임 | STF / Rule |
| StandardResponse 조립 | ETF 책임 | ETF |
| 오류코드 직접 매핑 | 공통 예외 처리 책임 | GlobalExceptionHandler |
| HTTP Client 직접 호출 | 연동 표준 우회 | Integration Client / Service |
| 여러 ServiceId를 if문으로 분기 | Facade 책임 불명확 | Method 분리 |
| 화면 전용 분기 남용 | 화면과 업무 결합 | ServiceId 또는 DTO 분리 |

나쁜 예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class BadSvCustomerFacade {
    private final SvCustomerMapper svCustomerMapper;
    public Map<String, Object> selectCustomerSummary(Map<String, Object> request) {
        // 금지: Facade에서 Mapper 직접 호출
        Map<String, Object> result =
                svCustomerMapper.selectCustomerSummary(request);
        // 금지: Facade에서 응답 전문 직접 조립
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", "SUCCESS");
        response.put("data", result);
        return response;
    }
}
```

권장 예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {
    private final SvCustomerService svCustomerService;
    public SvCustomerSummaryResponse selectCustomerSummary(
            SvCustomerSummaryRequest request,
            TransactionContext context
    ) {
        return svCustomerService.selectCustomerSummary(request, context);
    }
}
```

### 24.10 Facade와 Service 관계

Facade는 Service를 호출하고, Service는 실제 업무 처리를 수행한다.
```text
Facade
   ↓
Service
   ↓
Rule
   ↓
DAO / Mapper
```

| 계층 | 책임 |
| --- | --- |
| Facade | 유스케이스 조정 |
| Service | 업무 처리 |
| Rule | 업무 규칙 검증 |
| DAO | DB 접근 캡슐화 |
| Mapper | SQL 실행 |

예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {
    private final SvCustomerService svCustomerService;
    private final SvProductService svProductService;
    public SvCustomerDetailResponse selectCustomerDetail(
            SvCustomerDetailRequest request,
            TransactionContext context
    ) {
        SvCustomerBasicInfo basicInfo =
                svCustomerService.selectCustomerBasicInfo(request.getCustomerNo(), context);
        List<SvCustomerProductItem> productList =
                svProductService.selectCustomerProductList(request.getCustomerNo(), context);
        return SvCustomerDetailResponse.of(basicInfo, productList);
    }
}
```

이 구조에서 Facade는 여러 Service를 조합한다.하지만 고객 존재 여부, 상품 조회 조건, 권한 판단 등 세부 업무 규칙은 Service 또는 Rule에서 처리한다.

### 24.11 Facade와 Rule 관계

Facade는 원칙적으로 Rule을 직접 호출하지 않는다.Rule은 Service 내부에서 호출하는 것을 기본으로 한다.
```text
Facade
   ↓
Service
   ↓
Rule
```

권장 구조는 다음과 같다.
```java
@Service
@RequiredArgsConstructor
public class SvCustomerService {
    private final SvCustomerRule svCustomerRule;
    private final SvCustomerDao svCustomerDao;
    public SvCustomerSummaryResponse selectCustomerSummary(
            SvCustomerSummaryRequest request,
            TransactionContext context
    ) {
        svCustomerRule.validateCustomerInquiry(request.getCustomerNo(), context);
        SvCustomerSummaryResult result =
                svCustomerDao.selectCustomerSummary(
                        SvCustomerSummaryCriteria.from(request, context)
                );
        return SvCustomerSummaryResponse.from(result);
    }
}
```

Facade에서 Rule을 직접 호출하지 않는 이유는 다음과 같다.
| 이유 | 설명 |
| --- | --- |
| 책임 분리 | Facade는 흐름, Rule은 판단 |
| 재사용성 | Service를 재사용할 때 Rule 검증 누락 방지 |
| 테스트 용이성 | Service 단위로 업무 검증 테스트 가능 |
| 변경 영향 최소화 | 업무 규칙 변경이 Facade 흐름에 과도하게 영향 주지 않음 |

### 24.12 Facade와 DTO 변환 기준

Facade에서는 필요 시 Request DTO를 내부 Command/Query DTO로 변환할 수 있다.
```text
Request DTO
↓
Command / Query DTO
↓
Service
↓
Result DTO
↓
Response DTO
```

예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class OmUserFacade {
    private final OmUserService omUserService;
    @Transactional
    public OmUserDetailResponse updateUser(
            OmUserUpdateRequest request,
            TransactionContext context
    ) {
        OmUserUpdateCommand command =
                OmUserUpdateCommand.builder()
                        .targetUserId(request.getTargetUserId())
                        .userName(request.getUserName())
                        .useYn(request.getUseYn())
                        .modifiedBy(context.getUserId())
                        .modifiedBranchId(context.getBranchId())
                        .build();
        OmUserDetailResult result = omUserService.updateUser(command);
        return OmUserDetailResponse.from(result);
    }
}
```

DTO 변환 기준은 다음과 같다.
| 변환 | 권장 위치 |
| --- | --- |
| 설명 | Body → Request DTO |
| Handler | 표준 전문 Body 변환 |
| Request → Command | Facade 또는 Service |
| 저장·수정 명령 생성 | Request → Query |
| Facade 또는 Service | 조회 조건 생성 |
| Result → Response | Service 또는 Facade |
| 화면 응답 DTO 생성 | Response → StandardResponse |
| ETF | 표준 응답 전문 조립 |

단순 업무에서는 Service에서 변환해도 된다.중요한 것은 Mapper나 DAO가 화면 Request DTO에 직접 의존하지 않도록 하는 것이다.

### 24.13 Facade 트랜잭션 기준

Facade는 유스케이스 단위 트랜잭션 경계를 설정할 수 있다.
| 거래 유형 | 트랜잭션 기준 | 단순 조회 |
| --- | --- | --- |
| 여러 Service 조합 저장 | Facade에 @Transactional 권장 | 외부 연동 포함 |
| DB 트랜잭션과 외부 호출 경계 분리 검토 | 배치 실행 | Job/Step 단위 트랜잭션 별도 설계 |
조회 예시는 다음과 같다.

| 파일 다운로드 준비 | 메타/감사 로그 저장 범위만 트랜잭션 적용 |
| TransactionContext context | ) { | return svCustomerService.selectCustomerSummary(request, context); |
저장 예시는 다음과 같다.

| } | |
| TransactionContext context | ) { | CmCampaignCreateResult result = |
| cmCampaignService.createCampaign(request, context); |  | cmCampaignApprovalService.createInitialApproval(result.getCampaignId(), context); |

```java
@Transactional(readOnly = true) 가능
단일 등록/수정/삭제
@Transactional 적용 가능
@Transactional(readOnly = true)
public SvCustomerSummaryResponse selectCustomerSummary(
SvCustomerSummaryRequest request,
@Transactional
public CmCampaignCreateResponse createCampaign(
CmCampaignCreateRequest request,
```

    return CmCampaignCreateResponse.from(result);
}

트랜잭션 기준은 다음 원칙을 따른다.
하나의 ServiceId가 하나의 업무 유스케이스라면,
Facade를 트랜잭션 경계로 두는 것이 이해하기 쉽다.

단, 외부 시스템 호출과 DB 트랜잭션을 무조건 하나로 묶지 않는다.

### 24.14 외부 연동이 있는 Facade 기준

Facade에서 외부 연동 흐름을 조정할 수는 있지만, 직접 HTTP Client를 사용하면 안 된다.
나쁜 예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class BadIcCustomerFacade {
    private final RestTemplate restTemplate;
    public Object callSvService(String customerNo) {
        return restTemplate.postForObject(
                "http://sv-service:8080/sv/online",
                customerNo,
                Object.class
        );
    }
}
```

권장 예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class IcCustomerFacade {
    private final SvIntegrationService svIntegrationService;
    public IcCustomerSummaryResponse selectCustomerSummary(
            IcCustomerSummaryRequest request,
            TransactionContext context
    ) {
        SvCustomerSummaryResponse svResponse =
                svIntegrationService.selectCustomerSummary(
                        request.getCustomerNo(),
                        context
                );
        return IcCustomerSummaryResponse.from(svResponse);
    }
}
```

연동 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 직접 URL 호출 금지 | 연동 표준 우회 방지 |
| Integration Service 사용 | 표준 전문, Header 전달, Timeout, 오류 처리 일원화 |
| TransactionContext 전달 | GUID, TraceId, 사용자, 지점, 채널 유지 |
| Timeout 정책 적용 | 서비스별 연동 Timeout 사용 |
| 오류코드 매핑 | 연동 오류를 표준 오류코드로 변환 |
| 재시도 정책 분리 | 무분별한 Retry 금지 |

### 24.15 Facade와 TransactionContext 기준

Facade는 반드시 TransactionContext를 인자로 받는 것을 원칙으로 한다.
```java
public SvCustomerSummaryResponse selectCustomerSummary(
        SvCustomerSummaryRequest request,
        TransactionContext context
) {
    ...
}
```

TransactionContext를 전달해야 하는 이유는 다음과 같다.

| Context 항목 | 사용 목적 | guid |
| --- | --- | --- |
| 로그 추적 | traceId | 내부 추적 |

| transactionId | 거래 상태 관리 | serviceId |
| --- | --- | --- |
| 현재 실행 서비스 식별 | transactionCode | 거래로그·감사로그 기준 |

| businessCode | 업무 구분 | userId |
| --- | --- | --- |
| 사용자 기준 처리 | branchId | 지점 권한, 데이터 범위 |
| channelId | 채널별 정책 | clientIp |
| 감사, 보안 | centerId | 센터/DR 추적 |

Facade에서 Header를 직접 재해석하지 않고, STF에서 검증·보정된 TransactionContext를 사용한다.

### 24.16 Facade 오류 처리 기준

Facade에서는 예외를 임의로 잡아 삼키지 않는다.
나쁜 예시는 다음과 같다.
```java
public SvCustomerSummaryResponse selectCustomerSummary(
        SvCustomerSummaryRequest request,
        TransactionContext context
) {
    try {
        return svCustomerService.selectCustomerSummary(request, context);
    } catch (Exception e) {
        return new SvCustomerSummaryResponse();
    }
}
```

권장 기준은 다음과 같다.
```java
public SvCustomerSummaryResponse selectCustomerSummary(
        SvCustomerSummaryRequest request,
        TransactionContext context
) {
    return svCustomerService.selectCustomerSummary(request, context);
}
```

오류 처리 흐름은 다음과 같다.
```text
Service / Rule / DAO 오류 발생
   ↓
BusinessException / SystemException
   ↓
Facade는 예외 전파
   ↓
GlobalExceptionHandler 또는 ETF
   ↓
StandardResponse.error 조립
   ↓
거래로그 FAIL 기록
```

Facade에서 예외를 잡는 경우는 다음처럼 보상 처리 또는 명확한 오류 변환이 필요한 경우로 제한한다.
```java
@Transactional
public MgMessageSendResponse sendMessage(
        MgMessageSendRequest request,
        TransactionContext context
) {
    try {
        MgMessageSendResult result =
                mgMessageService.sendMessage(request, context);
        return MgMessageSendResponse.from(result);
    } catch (ExternalSystemException e) {
        throw new BusinessException(
                "E-MG-SND-0001",
                "메시지 발송 처리 중 오류가 발생했습니다.",
                e
        );
    }
}
```

### 24.17 Facade와 응답 DTO 기준

Facade는 최종 업무 응답 DTO를 반환한다.
```java
public SvCustomerSummaryResponse selectCustomerSummary(
        SvCustomerSummaryRequest request,
        TransactionContext context
) {
    ...
}
```

응답 DTO 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| StandardResponse 직접 반환 금지 | ETF 책임 |
| 업무 Response DTO 반환 | 화면 Body에 들어갈 데이터 |
| Entity 직접 반환 금지 | DB 구조 노출 방지 |
| Mapper Result 직접 반환 지양 | 내부 구조와 화면 결합 방지 |
| 개인정보 마스킹 확인 | 고객명, 전화번호 등 |
| 코드명 변환 확인 | 화면 표시용 코드명 필요 시 변환 |

좋은 예시는 다음과 같다.
return SvCustomerSummaryResponse.from(result);

금지 예시는 다음과 같다.
return StandardResponse.success(result);

### 24.18 Facade 개발 예시: SV 고객 상세 조회

#### 24.18.1 ServiceId

SV.Customer.selectDetail

#### 24.18.2 Request DTO

@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerDetailRequest {
    @NotBlank(message = "고객번호는 필수입니다.")
    private String customerNo;
}
```

#### 24.18.3 Facade

```java
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {
    private final SvCustomerService svCustomerService;
    private final SvProductService svProductService;
    private final SvConsultingService svConsultingService;
    @Transactional(readOnly = true)
    public SvCustomerDetailResponse selectCustomerDetail(
            SvCustomerDetailRequest request,
            TransactionContext context
    ) {
        SvCustomerBasicInfo basicInfo =
                svCustomerService.selectCustomerBasicInfo(
                        request.getCustomerNo(),
                        context
                );
        List<SvCustomerProductItem> productList =
                svProductService.selectCustomerProductList(
                        request.getCustomerNo(),
                        context
                );
        SvConsultingSummary consultingSummary =
                svConsultingService.selectConsultingSummary(
                        request.getCustomerNo(),
                        context
                );
        return SvCustomerDetailResponse.builder()
                .basicInfo(basicInfo)
                .productList(productList)
                .consultingSummary(consultingSummary)
                .build();
    }
}
```

이 예시는 Facade가 여러 Service를 조합하는 구조이다.하지만 각 Service 내부의 고객 존재 여부, 지점 접근권한, 상품 조회 조건은 Service 또는 Rule에서 검증한다.

### 24.19 Facade 개발 예시: OM 사용자 수정

```java
@Component
@RequiredArgsConstructor
public class OmUserFacade {
    private final OmUserService omUserService;
    private final OmAuditService omAuditService;
    @Transactional
    public OmUserDetailResponse updateUser(
            OmUserUpdateRequest request,
            TransactionContext context
    ) {
        OmUserUpdateCommand command =
                OmUserUpdateCommand.from(request, context);
        OmUserDetailResult result =
                omUserService.updateUser(command);
        omAuditService.saveUserChangeAudit(
                result.getUserId(),
                "USER_UPDATE",
                context
        );
        return OmUserDetailResponse.from(result);
    }
}
```

이 예시에서 Facade는 다음을 조정한다.
| 순서 | 처리 |
| --- | --- |
| 1 | Request DTO를 Command DTO로 변환 |
| 2 | 사용자 수정 Service 호출 |
| 3 | 감사로그 저장 Service 호출 |
| 4 | Response DTO 반환 |

### 24.20 Facade 개발 예시: MG 메시지 발송

```java
@Component
@RequiredArgsConstructor
public class MgMessageFacade {
    private final MgMessageService mgMessageService;
    private final MgMessageHistoryService mgMessageHistoryService;
    @Transactional
    public MgMessageSendResponse sendMessage(
            MgMessageSendRequest request,
            TransactionContext context
    ) {
        MgMessageSendCommand command =
                MgMessageSendCommand.from(request, context);
        MgMessageSendResult result =
                mgMessageService.sendMessage(command);
        mgMessageHistoryService.saveSendHistory(result, context);
        return MgMessageSendResponse.from(result);
    }
}
```

메시지 발송 거래에서는 다음을 고려한다.
| 항목 | 기준 |
| --- | --- |
| 중복요청 | idempotencyKey 확인 |
| 발송 이력 | 거래 결과 저장 |
| 오류 처리 | 외부 발송 실패 표준 오류코드 변환 |
| Timeout | 메시지 발송 Timeout 정책 적용 |
| 감사 | 대량 발송 또는 고객정보 포함 시 감사 대상 |

### 24.21 Facade 테스트 기준

Facade는 단위 테스트를 작성해야 한다.

| 테스트 항목 | 설명 | Service 호출 순서 |
| --- | --- | --- |
| 여러 Service 호출 순서 검증 | Command 변환 | Request + Context → Command 변환 검증 |
| 정상 응답 | Response DTO 생성 검증 | 업무 오류 전파 |

Service 오류가 전파되는지 검증

| 트랜잭션 경계 | 저장 거래에서 롤백 기준 검증 | 외부 연동 오류 |
| --- | --- | --- |
| Integration Service 오류 처리 검증 | 감사로그 호출 | 감사 대상 거래에서 Audit Service 호출 검증 |

```java
테스트 예시는 다음과 같다.
@ExtendWith(MockitoExtension.class)
class OmUserFacadeTest {
```

    @Mock
    private OmUserService omUserService;

    @Mock
    private OmAuditService omAuditService;

    private OmUserFacade omUserFacade;

    @BeforeEach
```java
void setUp() {
        omUserFacade = new OmUserFacade(omUserService, omAuditService);
    }
@Test
    void 사용자수정_후_감사로그를_저장한다() {
        OmUserUpdateRequest request = new OmUserUpdateRequest();
        request.setTargetUserId("U123456");
        request.setUserName("홍길동");
        request.setUseYn("Y");
        TransactionContext context = TransactionContext.builder()
                .userId("ADMIN001")
                .branchId("000001")
                .serviceId("OM.User.update")
                .transactionCode("OM-UPD-0001")
                .build();
        OmUserDetailResult result =
                OmUserDetailResult.builder()
                        .userId("U123456")
                        .userName("홍길동")
                        .useYn("Y")
                        .build();
        given(omUserService.updateUser(any(OmUserUpdateCommand.class)))
                .willReturn(result);
        OmUserDetailResponse response =
                omUserFacade.updateUser(request, context);
        assertThat(response.getUserId()).isEqualTo("U123456");
        verify(omUserService).updateUser(any(OmUserUpdateCommand.class));
        verify(omAuditService).saveUserChangeAudit(
                eq("U123456"),
                eq("USER_UPDATE"),
                eq(context)
        );
    }
```

### 24.22 Facade 통합 테스트 기준

Facade는 Handler 통합 테스트와 함께 검증한다.
```json
{
  "header": {
    "businessCode": "OM",
    "serviceId": "OM.User.update",
    "transactionCode": "OM-UPD-0001",
    "processingType": "UPDATE",
    "serviceName": "사용자정보수정",
    "channelId": "OM-PORTAL",
    "userId": "ADMIN001",
    "branchId": "000001"
  },
  "body": {
    "targetUserId": "U123456",
    "userName": "홍길동",
    "useYn": "Y"
  }
}
```

확인 항목은 다음과 같다.
| 확인 항목 | 기준 |
| --- | --- |
| Handler 실행 | OmUserUpdateHandler 선택 |
| Facade 호출 | OmUserFacade.updateUser() 호출 |
| Service 호출 | OmUserService.updateUser() 호출 |
| 감사로그 호출 | 감사 대상이면 Audit Service 호출 |
| 트랜잭션 | 오류 발생 시 DB 변경 롤백 |
| 응답 구조 | StandardResponse |
| 거래로그 | ServiceId, 거래코드, 처리상태 기록 |

### 24.23 Facade 오류코드 기준

Facade 자체 오류는 많지 않아야 한다.오류 대부분은 Service, Rule, DAO, Integration 계층에서 발생하고 Facade는 이를 전파한다.
| 오류 상황 | 오류코드 예시 | 기준 |
| --- | --- | --- |
| 유스케이스 흐름 오류 | E-TCF-FCD-0001 | Facade 조정 중 오류 |
| Command 변환 오류 | E-TCF-DTO-0002 | Request → Command 변환 실패 |
| 필수 Service 미호출 | 테스트로 검출 | 개발 오류 |
| 외부 연동 오류 | E-{업무코드}-IF-0001 | Integration Service에서 변환 |
| 업무 규칙 오류 | E-{업무코드}-VAL-0001 | Rule에서 발생 |
| DB 정합성 오류 | E-{업무코드}-DB-0001 | Service/DAO에서 발생 |
| 시스템 오류 | E-TCF-SYS-0001 | 공통 시스템 오류 |

Facade에서 별도 오류코드를 만들기보다는, 하위 계층의 오류코드를 유지하는 것이 장애 분석에 유리하다.

### 24.24 Facade 작성 시 금지 사항

| 금지 사항 | 사유 |
| --- | --- |
| Facade에서 Mapper 직접 호출 | Persistence 계층 우회 |
| Facade에서 SQL 작성 | 계층 책임 위반 |
| Facade에서 업무 규칙을 대량 작성 | Rule 계층 무력화 |
| Facade에서 StandardResponse 생성 | ETF 책임 침범 |
| Facade에서 HTTP Client 직접 사용 | 연동 표준 우회 |
| Facade에서 Header를 직접 파싱 | STF와 TransactionContext 책임 침범 |
| Facade에서 try-catch 후 오류 무시 | 장애 추적 불가 |
| Facade에서 화면 분기 로직 작성 | 화면과 업무 결합 |
| Facade Method 하나에 여러 ServiceId 분기 | 유지보수성 저하 |
| Facade가 너무 많은 Service를 무질서하게 호출 | 유스케이스 분리 필요 |
| Facade에서 Entity를 화면에 직접 반환 | DB 구조 노출 위험 |

### 24.25 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| Facade가 application.facade 패키지에 위치하는가? | □ |
| Facade 이름이 {업무코드}{도메인}Facade 형식인가? | □ |
| Handler가 Facade를 호출하는가? | □ |
| Facade Method가 ServiceId의 업무 행위와 일치하는가? | □ |
| Facade가 Service를 호출하고 있는가? | □ |
| Facade에서 DAO/Mapper를 직접 호출하지 않는가? | □ |
| Facade에서 SQL을 작성하지 않았는가? | □ |
| Facade에서 상세 업무 Rule을 직접 구현하지 않았는가? | □ |
| TransactionContext를 인자로 받아 하위 계층에 전달하는가? | □ |
| 저장 거래에 트랜잭션 경계를 검토했는가? | □ |
| 조회 거래에 readOnly = true 적용을 검토했는가? | □ |
| 외부 연동은 Integration Service를 통해 수행하는가? | □ |
| StandardResponse를 직접 생성하지 않는가? | □ |
| 예외를 임의로 삼키지 않는가? | □ |
| Response DTO를 반환하는가? | □ |
| Facade 단위 테스트가 있는가? | □ |

### 24.26 마무리말

Facade는 NSIGHT 업무 유스케이스를 읽기 쉽게 만드는 계층이다.Handler가 ServiceId 진입점이라면, Facade는 해당 ServiceId가 실제로 어떤 순서로 업무를 수행하는지를 보여준다.
Facade가 없으면 Handler가 Service를 직접 호출하게 되고, 시간이 지날수록 Handler에 업무 흐름, 트랜잭션, 외부 연동, 감사로그, 응답 조립이 섞일 가능성이 높다.
따라서 Facade는 다음 원칙으로 개발한다.
Facade는 얇고 명확하게 만든다.
Facade는 유스케이스 흐름만 조정한다.
상세 업무 판단은 Rule과 Service에 둔다.
DB 접근은 DAO/Mapper에 둔다.
응답 전문 조립은 ETF에 둔다.

### 24.27 시사점

Facade 개발 기준을 지키면 다음 효과가 있다.
| 효과 | 설명 |
| --- | --- |
| 유스케이스 가독성 향상 | ServiceId별 업무 흐름을 한눈에 파악 가능 |
| Handler 단순화 | Handler는 DTO 변환과 Facade 호출에 집중 |
| 트랜잭션 관리 용이 | 유스케이스 단위 트랜잭션 경계 설정 가능 |
| 테스트 용이성 | Service 호출 순서와 응답 조립 검증 가능 |
| 유지보수성 향상 | 업무 흐름 변경 시 Facade 중심으로 수정 가능 |
| 운영 추적성 향상 | ServiceId와 Facade Method 매핑이 명확해짐 |

최종 원칙은 다음이다.
Facade는 업무 로직을 담는 곳이 아니라 업무 유스케이스 흐름을 조정하는 곳이다.Handler는 Facade를 호출하고, Facade는 Service를 호출하며, 상세 업무 판단은 Rule에서 처리해야 한다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (29).docx`

| [어플리케이션계층.md](../zdoc/어플리케이션계층.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [23. TransactionHandler 개발](./23-TransactionHandler-개발.md) · [25. Service 개발](./25-Service-개발.md) →