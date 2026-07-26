
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제5부. 오류처리와 안정적인 거래 제어

## 1. 도입 전 안내말

제3부에서는 첫 번째 고객조회 거래를 구현했습니다.

제4부에서는 조회·등록·변경·삭제 거래를 구현하면서 트랜잭션, 중복방지, 동시성, SQL 품질을 배웠습니다.

하지만 프로그램은 정상적으로 처리될 때보다 오류가 발생했을 때 구조의 품질이 더 분명하게 드러납니다.

다음 상황을 생각해 봅시다.

```
고객번호가 입력되지 않았다.

사용자가 해당 고객을 조회할 권한이 없다.

운영자가 해당 ServiceId를 일시 중지했다.

같은 등록 요청이 두 번 전송되었다.

DB Connection을 얻지 못했다.

SQL이 3초 안에 끝나지 않았다.

외부 시스템이 응답하지 않았다.

프로그램에서 NullPointerException이 발생했다.
```

이 상황들을 모두 다음과 같이 처리하면 안 됩니다.

```
catch (Exception e) {
    return "처리 중 오류가 발생했습니다.";
}
```

모든 오류를 하나로 처리하면 화면과 운영자는 다음 사실을 알 수 없습니다.

```
사용자가 잘못 입력한 것인가?

권한이 없는 것인가?

거래가 운영 중지된 것인가?

다시 요청해도 되는 오류인가?

DB가 장애인 것인가?

Timeout이 발생한 것인가?

프로그램 결함인가?

이미 처리는 성공했지만 응답만 실패한 것인가?
```

NSIGHT TCF에서 오류처리는 단순한 예외 문법이 아닙니다.

```
오류 감지
→ 오류 분류
→ 업무 실행 중단
→ 트랜잭션 Rollback
→ 표준 오류코드 변환
→ 거래로그 종료
→ 감사로그 기록
→ Metric 반영
→ 표준 응답 반환
→ 운영 경보 및 재처리 판단
```

TCF는 온라인 거래가 Handler에 도달하기 전에 Header, 인증, 권한, 거래통제, Timeout, 중복요청과 거래로그 시작을 공통으로 처리하고, 업무 실행 후에는 ETF가 정상·업무 오류·시스템 오류의 종료 상태를 표준화합니다.

제5부에서는 다음 질문에 답합니다.

```
업무 오류와 시스템 오류는 무엇이 다른가?

오류코드는 어떻게 만들어야 하는가?

예외는 어느 계층에서 발생시키고 어디에서 잡아야 하는가?

Timeout이 발생하면 DB 작업도 즉시 멈추는가?

운영자가 거래를 중지하면 Handler는 실행되는가?

중복 요청과 재시도는 어떻게 구분하는가?

GUID와 TraceId는 왜 필요한가?

장애가 발생했을 때 무엇부터 확인해야 하는가?
```

## 2. 제5부 개요

### 2.1 목적

제5부의 목적은 초보 개발자가 정상 흐름뿐 아니라 오류·Timeout·중복·장애 상황을 안전하게 처리하는 거래를 구현하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 오류를 입력·인증·권한·통제·업무·시스템·Timeout으로 구분한다.
- 업무 예외와 시스템 예외를 구분하여 발생시킨다.
- 표준 오류코드를 설계하고 오류 응답을 작성한다.
- 계층별 예외 책임을 이해한다.
- ETF를 통해 거래 종료 상태를 일관되게 기록한다.
- ServiceId별 Timeout 정책을 적용한다.
- SQL과 외부 연계 Timeout을 함께 설계한다.
- OM 거래통제로 Handler 실행 전 거래를 차단한다.
- Idempotency Key를 이용하여 중복 변경을 방지한다.
- 재시도 가능한 오류와 재시도하면 안 되는 오류를 구분한다.
- GUID·TraceId·ServiceId로 전체 실행 흐름을 추적한다.
- 장애 발생 시 로그·Thread·DB Pool·SQL 순서로 원인을 찾는다.

### 2.2 적용범위

| 영역 | 주요 내용 |
| --- | --- |
| 예외 | BusinessException, SystemException |
| 오류 분류 | 입력, 인증, 권한, 통제, 업무, 시스템, Timeout |
| 오류코드 | 공통·프레임워크·업무 코드 |
| 표준 응답 | Result, Error, TraceId |
| ETF | 성공·실패·Timeout 종료 처리 |
| Timeout | Gateway, TCF, 트랜잭션, SQL, 외부 호출 |
| 거래통제 | ServiceId 실행 허용·중지 |
| 중복방지 | Idempotency Key와 상태관리 |
| 재시도 | 일시 장애와 영구 오류 구분 |
| 거래로그 | 거래 시작·종료·처리시간 |
| 감사로그 | 중요 조회·변경 행위 기록 |
| 장애 대응 | 원인 분류, 경보, 복구, 재처리 |
| 품질 Gate | 오류코드·Timeout·로그 자동검증 |

### 2.3 대상 독자

- try-catch 사용은 알지만 오류 설계가 어려운 개발자
- 모든 오류를 RuntimeException으로 처리하는 개발자
- Timeout과 SQL Timeout의 차이를 모르는 개발자
- 중복 요청과 재시도의 차이를 이해하고 싶은 개발자
- 로그는 많지만 장애 원인을 찾기 어려운 개발자
- OM 거래통제와 ServiceId 운영구조를 처음 접하는 개발자
- 장애 테스트와 운영 전환을 준비하는 개발자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
STF는 Handler 실행 전 공통 검증을 수행한다.

Dispatcher는 ServiceId로 Handler를 찾는다.

Facade는 트랜잭션 경계를 가진다.

업무 예외가 발생하면 업무 결과는 실패한다.

ETF는 표준 응답과 거래 종료 처리를 담당한다.

TransactionContext에는 GUID와 TraceId가 존재한다.
```

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| 예외 | 정상 처리 흐름을 계속할 수 없는 상황 |
| 업무 오류 | 업무 규칙에 따라 예상할 수 있는 실패 |
| 시스템 오류 | 프로그램·DB·인프라에서 발생한 비정상 실패 |
| 입력 오류 | 필수값·길이·형식이 올바르지 않은 요청 |
| 인증 오류 | 로그인 또는 토큰이 유효하지 않은 상태 |
| 권한 오류 | 로그인했지만 해당 기능을 실행할 수 없는 상태 |
| 거래통제 | 운영자가 거래 실행을 허용하거나 차단하는 기능 |
| Timeout | 정해진 시간 안에 처리가 끝나지 않은 상태 |
| Idempotency | 같은 요청이 반복되어도 한 번만 반영되는 특성 |
| 재시도 | 실패한 요청을 다시 실행하는 행위 |
| 거래로그 | 거래 시작·종료·처리시간·결과를 기록한 로그 |
| 감사로그 | 누가 어떤 중요 데이터를 조회·변경했는지 기록한 로그 |
| GUID | 여러 시스템을 연결하는 전체 거래 식별자 |
| TraceId | 하나의 실행 경로와 로그를 연결하는 식별자 |
| MDC | 로그에 TraceId 등의 공통값을 자동 포함시키는 문맥 |
| Fail-closed | 정책을 확인할 수 없으면 안전하게 실행을 차단하는 방식 |
| Fail-open | 정책을 확인할 수 없어도 일단 실행을 허용하는 방식 |

## 제29장. 오류를 제대로 구분하기

### 29.1 오류를 구분해야 하는 이유

다음 두 상황은 사용자에게 모두 실패로 보입니다.

```
상황 A
고객번호를 입력하지 않았다.

상황 B
DB 서버가 응답하지 않는다.
```

하지만 처리방법은 완전히 다릅니다.

| 구분 | 고객번호 미입력 | DB 서버 장애 |
| --- | --- | --- |
| 원인 | 사용자 요청 | 시스템·인프라 |
| 재입력 | 필요 | 불필요 |
| 재시도 | 입력 수정 후 | 복구 후 가능 |
| Rollback | DB 작업 전 | 진행 중 작업 Rollback |
| 운영 경보 | 보통 불필요 | 필요 |
| 사용자 메시지 | 구체적 안내 | 일반적 장애 안내 |
| 상세 로그 | 낮은 수준 | 예외와 원인 기록 |

오류를 구분하지 않으면 사용자에게 잘못된 행동을 요구할 수 있습니다.

```
DB 장애인데
“입력값을 확인하세요.”

권한 오류인데
“잠시 후 다시 시도하세요.”

업무 규칙 오류인데
“시스템 관리자에게 문의하세요.”
```

### 29.2 권장 오류 분류

```
입력 오류
인증 오류
권한 오류
거래통제 오류
중복요청 오류
업무 오류
Timeout
외부연계 오류
데이터 접근 오류
시스템 오류
```

### 29.3 입력 오류

입력 오류는 요청 형식이 계약과 맞지 않는 상황입니다.

예:

```
고객번호 미입력
날짜가 YYYYMMDD 형식이 아님
페이지 크기가 최대값 초과
허용되지 않은 코드값
문자열 길이 초과
```

발생 위치:

```
JSON 역직렬화
DTO Validation
STF Header 검증
Rule의 추가 형식검증
```

예시 오류코드:

```
E-COM-REQ-0001
필수 요청값이 없습니다.
```

입력 오류는 일반적으로 Handler 실행 전 또는 업무 처리 초기에 발견해야 합니다.

### 29.4 인증 오류

인증 오류는 요청 사용자를 신뢰할 수 없는 상황입니다.

예:

```
JWT 없음
JWT 만료
JWT 서명 오류
잘못된 issuer
잘못된 audience
세션 만료
인증 Context 없음
```

인증 오류에서는 업무 Handler를 실행하지 않습니다.

```
인증 실패
→ STF 실패
→ Handler 미실행
→ DB 트랜잭션 미시작
```

### 29.5 권한 오류

권한 오류는 사용자는 인증되었지만 기능 또는 데이터에 접근할 수 없는 상황입니다.

예:

```
고객조회 권한 없음
캠페인 승인 권한 없음
다른 지점 고객 조회 금지
운영자 전용 기능 접근
```

권한은 두 단계로 구분할 수 있습니다.

| 구분 | 예 | 처리 위치 |
| --- | --- | --- |
| 기능권한 | 캠페인 승인 기능 사용 가능 | STF·공통 권한 |
| 데이터권한 | 자기 지점 고객만 조회 | Service·Rule·SQL |

### 29.6 거래통제 오류

거래통제 오류는 기능이 정상적으로 개발되어 있어도 운영정책상 실행할 수 없는 상황입니다.

예:

```
해당 ServiceId 일시 중지
특정 채널의 거래 차단
점검시간 중 변경거래 차단
특정 지점 거래 차단
장애 발생 WAR의 신규 요청 차단
```

거래통제는 업무 오류와 다릅니다.

```
업무 오류
= 데이터와 업무 상태 때문에 처리 불가

거래통제 오류
= 운영정책 때문에 실행 자체를 차단
```

### 29.7 업무 오류

업무 오류는 예상 가능한 업무 규칙 위반입니다.

예:

```
고객을 찾을 수 없음
승인할 수 없는 캠페인 상태
잔여 한도 부족
동일 캠페인 중복 등록
다른 사용자가 먼저 변경함
미래 기준일 조회 불가
```

업무 오류는 프로그램 결함이 아닙니다.

따라서 다음과 같이 처리합니다.

```
표준 업무 오류코드
→ 사용자가 이해할 수 있는 메시지
→ 불필요한 시스템 경보 제외
→ 거래로그 BUSINESS_FAIL
```

중요 업무 오류의 빈도가 급증한다면 운영 이상징후로 볼 수 있으므로 Metric은 기록해야 합니다.

### 29.8 시스템 오류

시스템 오류는 정상적인 업무 흐름에서 예상하지 못한 기술적 실패입니다.

예:

```
NullPointerException
설정값 누락
Mapper Namespace 오류
DB Connection 실패
JSON 변환 실패
Thread Pool 거부
파일시스템 오류
메모리 부족
```

시스템 오류는 사용자에게 내부 상세정보를 보여주지 않습니다.

```
사용자 응답
시스템 처리 중 오류가 발생했습니다.
TraceId: T202607170001
```

운영 로그에는 원인 예외를 기록합니다.

### 29.9 Timeout

Timeout은 설정된 시간 안에 처리가 끝나지 않은 상황입니다.

예:

```
ServiceId Timeout 3000ms
실제 처리시간 5100ms
```

Timeout은 단순한 시스템 오류로만 보면 안 됩니다.

Timeout에는 다음 정보가 필요합니다.

```
어느 ServiceId인가?
어느 단계에서 오래 걸렸는가?
SQL인가?
DB Connection 대기인가?
외부 호출인가?
Thread 대기인가?
실제 작업은 중단되었는가?
```

### 29.10 오류 분류표

| 오류유형 | 예상 가능 | 재시도 | 운영 경보 | 사용자 조치 |
| --- | --- | --- | --- | --- |
| 입력 오류 | 예 | 입력 수정 후 | 보통 불필요 | 값 수정 |
| 인증 오류 | 예 | 재로그인 후 | 급증 시 | 로그인 |
| 권한 오류 | 예 | 권한 변경 전 불필요 | 감사 필요 | 권한 요청 |
| 거래통제 | 예 | 통제 해제 후 | 운영 이벤트 | 잠시 대기 |
| 중복 요청 | 예 | 기존 결과 확인 | 급증 시 | 재전송 중지 |
| 업무 오류 | 예 | 상태 변경 후 | 통계 기록 | 업무조건 확인 |
| Timeout | 일부 | 멱등성 확인 후 | 필요 | 처리결과 확인 |
| DB 장애 | 아니오 | 복구 후 | 필요 | 잠시 대기 |
| 프로그램 오류 | 아니오 | 수정 전 무의미 | 필요 | 관리자 문의 |

### 제29장 요약

```
모든 실패를 하나의 오류로 처리하지 않는다.

입력·인증·권한·통제·업무·Timeout·시스템 오류를
서로 구분해야
사용자 처리와 운영 대응이 정확해진다.
```

## 제30장. 표준 예외와 오류코드 설계

### 30.1 예외 클래스가 필요한 이유

다음 코드는 오류 의미를 알기 어렵습니다.

```
throw new RuntimeException("오류");
```

호출자는 다음을 판단할 수 없습니다.

```
업무 오류인가?
시스템 오류인가?
Rollback해야 하는가?
사용자에게 어떤 메시지를 보여줄 것인가?
운영 경보를 발생시킬 것인가?
```

따라서 오류 종류를 나타내는 표준 예외를 사용합니다.

### 30.2 권장 예외 계층

```
TcfException
├─ BusinessException
│  ├─ ValidationException
│  ├─ AuthorizationException
│  ├─ TransactionControlException
│  ├─ DuplicateRequestException
│  └─ ConcurrencyException
│
├─ TimeoutException
│
└─ SystemException
   ├─ DataAccessSystemException
   ├─ ExternalSystemException
   └─ ConfigurationException
```

실제 프로젝트는 클래스 수를 단순화할 수 있습니다.

중요한 것은 최소한 다음 두 범주를 구분하는 것입니다.

```
BusinessException
SystemException
```

### 30.3 업무 예외

```
public class BusinessException
        extends RuntimeException {

    private final String errorCode;

    public BusinessException(
            String errorCode,
            String message) {

        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

사용 예:

```
if (campaign == null) {
    throw new BusinessException(
        "E-CM-CAM-0002",
        "캠페인을 찾을 수 없습니다."
    );
}
```

### 30.4 시스템 예외

```
public class SystemException
        extends RuntimeException {

    private final String errorCode;

    public SystemException(
            String errorCode,
            String message,
            Throwable cause) {

        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

DAO 예:

```
try {
    return campaignMapper.selectCampaignDetail(
        campaignId
    );

} catch (DataAccessException e) {
    throw new SystemException(
        "E-CM-DB-0001",
        "캠페인 조회 중 데이터 오류가 발생했습니다.",
        e
    );
}
```

### 30.5 원인 예외 보존

금지:

```
catch (DataAccessException e) {
    throw new SystemException(
        "E-CM-DB-0001",
        "DB 오류",
        null
    );
}
```

원인 예외를 버리면 운영자가 실제 원인을 찾기 어렵습니다.

권장:

```
throw new SystemException(
    "E-CM-DB-0001",
    "캠페인 조회 중 데이터 오류가 발생했습니다.",
    e
);
```

사용자 응답에는 Stack Trace를 노출하지 않지만 운영 로그에는 원인 예외를 보존합니다.

### 30.6 오류코드 형식

권장 개념 형식:

```
E-{업무 또는 영역}-{도메인 또는 유형}-{일련번호}
```

예:

```
E-COM-REQ-0001
E-COM-AUTH-0001
E-TCF-CTRL-0001
E-TCF-TIME-0001
E-SV-CUST-0002
E-CM-CAM-0011
E-CM-DB-0001
```

구성 해석:

| 구성 | 예 | 의미 |
| --- | --- | --- |
| 오류구분 | E | Error |
| 업무·영역 | CM | 캠페인 업무 |
| 도메인·유형 | CAM | Campaign |
| 일련번호 | 0011 | 상세 오류 |

실제 오류코드 자릿수와 약어는 공식 코드사전을 우선합니다.

### 30.7 오류코드 영역

| 영역 | 예 | 관리 주체 |
| --- | --- | --- |
| 공통 요청 | E-COM-REQ-* | 프레임워크 |
| 인증 | E-COM-AUTH-* | 보안·프레임워크 |
| 권한 | E-COM-AUZ-* | 보안·업무 |
| TCF | E-TCF-* | 프레임워크 |
| Timeout | E-TCF-TIME-* | 프레임워크 |
| 거래통제 | E-TCF-CTRL-* | OM·프레임워크 |
| 업무 | E-SV-CUST-* | 업무팀 |
| DB | E-SV-DB-* | 업무·프레임워크 |
| 외부연계 | E-EAI-* | 연계 플랫폼 |

### 30.8 오류 메시지의 두 종류

#### 사용자 메시지

사용자가 다음 행동을 판단할 수 있어야 합니다.

```
고객번호를 입력해 주세요.

다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요.

현재 점검 중인 거래입니다. 잠시 후 다시 시도해 주세요.
```

#### 운영 상세정보

운영 로그에는 기술 원인을 기록합니다.

```
serviceId=CM.Campaign.update
mapperId=CmCampaignMapper.updateCampaign
sqlState=08006
rootCause=Connection reset
```

사용자 메시지와 운영 상세정보를 같은 문자열로 사용하지 않습니다.

### 30.9 금지 메시지

```
오류
실패
Exception
Null
SQL Error
관리자에게 문의
```

문제가 무엇인지 알 수 없는 메시지는 줄여야 합니다.

다만 시스템 내부정보를 과도하게 노출해서도 안 됩니다.

금지:

```
ORA-00942: table or view does not exist
at com.nh.nsight.marketing.cm...
DB URL=jdbc:oracle:thin:@10.1.2.3
```

### 30.10 오류코드 Registry

오류코드는 코드와 문서에서 중복 정의하지 않도록 Registry 또는 Enum으로 관리할 수 있습니다.

```
public enum CmCampaignError {

    CAMPAIGN_NOT_FOUND(
        "E-CM-CAM-0002",
        "캠페인을 찾을 수 없습니다."
    ),

    INVALID_STATUS(
        "E-CM-CAM-0010",
        "현재 상태에서는 처리할 수 없습니다."
    ),

    CONCURRENT_UPDATE(
        "E-CM-CAM-0011",
        "다른 사용자가 먼저 변경했습니다."
    );

    private final String code;
    private final String message;
}
```

사용:

```
throw new BusinessException(
    CmCampaignError.CAMPAIGN_NOT_FOUND
);
```

실제 공통 예외가 Enum 생성자를 지원하도록 설계할 수 있습니다.

### 30.11 오류코드 변경관리

오류코드는 화면과 연계 시스템이 판단기준으로 사용할 수 있습니다.

따라서 기존 코드의 의미를 임의로 바꾸면 안 됩니다.

```
기존
E-CM-CAM-0002 = 캠페인 없음

변경
E-CM-CAM-0002 = 캠페인 기간 오류
```

금지 이유:

- 화면 처리 오류
- 테스트케이스 오판
- 운영 통계 왜곡
- 연계 시스템 호환성 훼손
기존 오류코드는 유지하고 새로운 코드를 추가합니다.

### 제30장 요약

```
예외 클래스는 오류 의미를 전달한다.

BusinessException
= 예상 가능한 업무 실패

SystemException
= 기술적·예상하지 못한 실패

오류코드는 한 번 배포되면
외부 계약처럼 변경을 관리한다.
```

## 제31장. 계층별 예외처리와 ETF

### 31.1 예외를 어디에서 잡아야 하나요?

초보 개발자는 모든 메서드에 try-catch를 작성하는 경우가 많습니다.

```
public Object handle(...) {
    try {
        return facade.execute(...);
    } catch (Exception e) {
        return null;
    }
}
```

이 방식은 오류를 숨기고 표준 종료 처리를 방해합니다.

기본 원칙은 다음과 같습니다.

```
업무 의미를 아는 계층에서
의미 있는 예외로 변환한다.

최종 응답 처리는
TCF와 ETF가 담당한다.
```

### 31.2 계층별 책임

| 계층 | 예외처리 책임 | 금지 |
| --- | --- | --- |
| Controller | TCF 호출 | 업무별 오류 응답 조립 |
| TCF | 예외 최종 분류 | 업무 규칙 판단 |
| STF | 공통 검증 예외 | 업무 DB 처리 |
| Handler | ServiceId 연결 | 모든 예외를 Exception으로 변환 |
| Facade | 트랜잭션 Rollback | 예외 숨김 |
| Service | 업무 흐름에 맞는 예외 발생 | 표준 응답 생성 |
| Rule | 업무 오류 발생 | 기술 예외 생성 남용 |
| DAO | DB 예외를 시스템 예외로 변환 | 업무 오류 판단 |
| Mapper | SQL 실행 | 예외 메시지 조립 |
| ETF | 표준 종료·응답 | 업무 데이터 변경 |

### 31.3 Rule의 예외

Rule은 업무 조건이 맞지 않을 때 BusinessException을 발생시킵니다.

```
public void validateCampaignExists(
        CampaignDetailData campaign) {

    if (campaign == null) {
        throw new BusinessException(
            "E-CM-CAM-0002",
            "캠페인을 찾을 수 없습니다."
        );
    }
}
```

### 31.4 DAO의 예외

DAO는 기술 예외를 업무팀이 이해할 수 있는 시스템 예외로 변환할 수 있습니다.

```
public CampaignDetailData selectCampaignDetail(
        String campaignId) {

    try {
        return campaignMapper.selectCampaignDetail(
            campaignId
        );

    } catch (DataAccessException e) {
        throw new SystemException(
            "E-CM-DB-0001",
            "캠페인 조회 중 데이터 오류가 발생했습니다.",
            e
        );
    }
}
```

공통 MyBatis 예외 변환기가 존재한다면 DAO마다 같은 try-catch를 반복하지 않습니다.

### 31.5 Facade와 Rollback

```
@Transactional
public CampaignCreateResponse create(
        CampaignCreateRequest request,
        TransactionContext context) {

    return campaignService.create(request, context);
}
```

Service에서 예외가 발생하면 Facade의 트랜잭션이 Rollback되어야 합니다.

주의:

```
BusinessException이 Checked Exception인 경우
기본 설정에서는 Rollback되지 않을 수 있음
```

따라서 프로젝트 예외는 일반적으로 RuntimeException을 상속하거나 명시적인 Rollback 정책을 사용합니다.

### 31.6 예외를 잡아도 되는 경우

예외를 잡는 행위 자체가 금지되는 것은 아닙니다.

다음 목적이 있을 때 잡을 수 있습니다.

```
기술 예외를 표준 예외로 변환
자원 정리
추가 문맥을 로그에 기록
대체 경로 수행
보상 처리
```

예:

```
try {
    return externalClient.call(request);

} catch (SocketTimeoutException e) {
    throw new ExternalSystemException(
        "E-EAI-TIME-0001",
        "외부 시스템 응답시간을 초과했습니다.",
        e
    );
}
```

### 31.7 예외를 잡으면 안 되는 방식

#### 예외 무시

```
catch (Exception e) {
    // 아무 처리 없음
}
```

#### 성공으로 변환

```
catch (Exception e) {
    return new CampaignResponse(
        "SUCCESS"
    );
}
```

#### 원인 제거

```
catch (Exception e) {
    throw new RuntimeException(
        "처리 실패"
    );
}
```

#### 중복 로그

```
DAO에서 Stack Trace
Service에서 같은 Stack Trace
Facade에서 같은 Stack Trace
TCF에서 같은 Stack Trace
```

같은 예외를 모든 계층에서 Error 로그로 남기면 한 번의 장애가 여러 건처럼 보입니다.

### 31.8 TCF의 예외 분기

개념적인 구조:

```
public StandardResponse<?> process(
        StandardRequest<?> request) {

    try {
        PreProcessResult preResult =
            stf.preProcess(request);

        Object result =
            timeoutExecutor.execute(
                () -> dispatcher.dispatch(request),
                preResult.timeout()
            );

        return etf.success(request, result);

    } catch (BusinessException e) {
        return etf.businessFail(request, e);

    } catch (TransactionTimeoutException e) {
        return etf.timeout(request, e);

    } catch (Exception e) {
        return etf.systemError(request, e);

    } finally {
        contextCleaner.clear();
    }
}
```

현재 구현이 별도의 ETF.timeout()을 제공하지 않는다면 systemError() 안에서 오류유형을 TIMEOUT으로 구분할 수 있습니다.

중요한 것은 Timeout을 일반 시스템 오류와 같은 통계로만 기록하지 않는 것입니다.

### 31.9 ETF의 역할

ETF는 거래 종료상태를 확정합니다.

| 경로 | 거래로그 | Idempotency | Metric | 응답 |
| --- | --- | --- | --- | --- |
| 성공 | SUCCESS | SUCCESS | 성공 건수 | 정상 응답 |
| 업무 오류 | BUSINESS_FAIL | FAIL | 업무 오류 건수 | 업무 오류 |
| 시스템 오류 | SYSTEM_ERROR | FAIL | 시스템 오류 건수 | 시스템 오류 |
| Timeout | TIMEOUT | TIMEOUT | Timeout 건수 | Timeout 오류 |
| 거래 차단 | REJECTED | 미시작 | 차단 건수 | 통제 오류 |

ETF는 정상·업무 오류·시스템 오류의 거래로그 종료, 감사와 Metric 기록을 일관되게 처리하도록 설계됩니다.

### 31.10 표준 오류 응답

```
{
  "header": {
    "businessCode": "CM",
    "serviceId": "CM.Campaign.update",
    "transactionCode": "CM-UPD-0001",
    "guid": "G202607170001",
    "traceId": "T202607170001"
  },
  "result": {
    "resultStatus": "FAIL",
    "resultCode": "E-CM-CAM-0011",
    "message": "다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요."
  },
  "body": null,
  "error": {
    "errorCode": "E-CM-CAM-0011",
    "errorType": "BUSINESS",
    "retryable": false
  }
}
```

### 31.11 시스템 오류 응답

```
{
  "header": {
    "businessCode": "CM",
    "serviceId": "CM.Campaign.update",
    "transactionCode": "CM-UPD-0001",
    "guid": "G202607170002",
    "traceId": "T202607170002"
  },
  "result": {
    "resultStatus": "ERROR",
    "resultCode": "E-COM-SYS-0001",
    "message": "시스템 처리 중 오류가 발생했습니다."
  },
  "body": null,
  "error": {
    "errorCode": "E-COM-SYS-0001",
    "errorType": "SYSTEM",
    "retryable": false
  }
}
```

사용자는 TraceId를 운영자에게 전달할 수 있습니다.

### 31.12 Context 정리

Tomcat Thread는 요청이 끝난 후 다른 사용자의 요청을 처리하는 데 재사용됩니다.

따라서 다음 문맥을 반드시 정리해야 합니다.

```
TransactionContextHolder
AuthenticationContextHolder
TimeoutContextHolder
MDC
ThreadLocal
```

정리하지 않으면 다음 요청 로그에 이전 사용자의 정보가 섞일 수 있습니다.

```
finally {
    TransactionContextHolder.clear();
    AuthenticationContextHolder.clear();
    TimeoutContextHolder.clear();
    MDC.clear();
}
```

### 제31장 요약

```
업무 의미가 있는 계층에서
표준 예외를 발생시킨다.

예외를 중간 계층에서 숨기지 않는다.

TCF와 ETF가
거래 종료·로그·Metric·응답을
한 번만 표준 처리한다.
```

## 제32장. Timeout 설계와 구현

### 32.1 Timeout은 왜 필요한가요?

처리시간 제한이 없으면 하나의 느린 요청이 자원을 계속 점유할 수 있습니다.

```
느린 요청
→ Tomcat Thread 장기 점유
→ DB Connection 장기 점유
→ 다른 요청 대기
→ 전체 시스템 응답 지연
→ 장애 확대
```

Timeout은 사용자를 불편하게 만드는 기능이 아니라 시스템 전체를 보호하는 기능입니다.

### 32.2 Timeout 계층

온라인 거래에는 여러 Timeout이 존재할 수 있습니다.

```
사용자 화면 Timeout
        ↓
Gateway Timeout
        ↓
TCF ServiceId Timeout
        ↓
외부 호출 Timeout
        ↓
DB Query Timeout
        ↓
Connection 획득 Timeout
```

각 Timeout의 책임은 다릅니다.

| Timeout | 보호 대상 |
| --- | --- |
| 화면 | 사용자의 무한 대기 방지 |
| Gateway | 외부 연결과 Route 보호 |
| ServiceId | 업무 거래 전체 실행시간 |
| 외부 호출 | 하위 시스템 대기 제한 |
| SQL Query | DB Statement 실행시간 |
| Connection | DB Pool 대기시간 |
| Transaction | DB 트랜잭션 실행시간 |

### 32.3 Timeout 값의 관계

일반 원칙:

```
하위 처리 Timeout
< 상위 거래 Timeout
< Gateway Timeout
< 화면 Timeout
```

예:

```
DB Query Timeout       2.0초
외부 호출 Timeout      2.0초
TCF ServiceId Timeout  3.0초
Gateway Timeout        4.0초
화면 Timeout           5.0초
```

단순히 모든 값을 3초로 설정하면 어떤 계층이 먼저 종료될지 불명확합니다.

### 32.4 시간 예산

ServiceId Timeout이 3000ms인 거래를 생각해 봅시다.

```
STF 전처리          100ms
업무 처리          2300ms
ETF 후처리          100ms
예비시간            500ms
-------------------------
전체               3000ms
```

업무 처리 안에 SQL 두 개와 외부 호출 한 개가 있다면 각각의 예산을 나누어야 합니다.

```
SQL 1              400ms
SQL 2              400ms
외부 호출          900ms
업무 계산          200ms
기타               400ms
```

하위 호출 각각에 3초를 허용하면 전체 거래는 3초를 훨씬 초과할 수 있습니다.

### 32.5 OM Timeout 정책

Timeout은 코드에만 작성하지 않고 OM 기준정보로 관리합니다.

| 항목 | 예 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| 기본 Timeout | 3000ms |
| 조회 Timeout | 3000ms |
| 변경 Timeout | 5000ms |
| 최대 허용값 | 10000ms |
| 운영 상태 | ACTIVE |
| 변경 사유 | SQL 튜닝 전 임시 조정 |
| 승인자 | AA·TA·OPS |
| 적용일시 | 2026-07-17 22:00 |

TCF 아키텍처 구축방법론에서도 Timeout, Idempotency, 오류코드, 거래로그를 각각 독립적인 설계·운영 기준으로 관리하도록 정의합니다.

### 32.6 TimeoutExecutor

개념 예:

```
public <T> T execute(
        Callable<T> task,
        Duration timeout) {

    Future<T> future =
        executorService.submit(task);

    try {
        return future.get(
            timeout.toMillis(),
            TimeUnit.MILLISECONDS
        );

    } catch (java.util.concurrent.TimeoutException e) {
        future.cancel(true);

        throw new TransactionTimeoutException(
            "E-TCF-TIME-0001",
            "거래 처리시간을 초과했습니다.",
            e
        );
    }
}
```

### 32.7 future.cancel(true)의 한계

cancel(true)를 호출했다고 해서 모든 작업이 즉시 멈추는 것은 아닙니다.

```
Java Thread interrupt 요청
≠ DB SQL 즉시 중단 보장
```

다음 작업은 interrupt를 무시하거나 늦게 반응할 수 있습니다.

- JDBC SQL
- 외부 HTTP 호출
- 파일 I/O
- 네이티브 라이브러리
- 무한 반복 코드
따라서 하위 기술별 Timeout도 필요합니다.

```
TCF Timeout
+ HTTP Client Timeout
+ MyBatis Query Timeout
+ JDBC Statement Timeout
```

### 32.8 MyBatis Query Timeout

Mapper 예:

```
<select id="selectCampaignList"
        resultMap="campaignResultMap"
        timeout="2">

    SELECT ...
      FROM CM_CAMPAIGN_MASTER
     WHERE ...

</select>
```

프로젝트 공통 설정을 사용할 수도 있습니다.

단위는 설정 방식에 따라 초 단위일 수 있으므로 반드시 실제 프레임워크 기준을 확인합니다.

### 32.9 외부 호출 Timeout

외부 호출에는 최소한 다음 값을 구분합니다.

```
Connection Timeout
= 연결을 맺는 시간 제한

Read Timeout
= 응답 데이터를 기다리는 시간 제한

Call Timeout
= 전체 호출 시간 제한
```

예:

```
tcf:
  clients:
    customer:
      connect-timeout-ms: 500
      read-timeout-ms: 1500
      call-timeout-ms: 2000
```

### 32.10 Connection Pool Timeout

HikariCP의 connectionTimeout은 SQL 실행시간이 아닙니다.

```
connectionTimeout
= Pool에서 Connection을 얻기 위해 기다리는 시간
```

예:

```
spring:
  datasource:
    hikari:
      connection-timeout: 3000
```

Pool이 모두 사용 중이면 최대 3초 기다린 뒤 Connection 획득 실패가 발생합니다.

### 32.11 트랜잭션 Timeout

Facade:

```
@Transactional(timeout = 3)
public CampaignUpdateResponse update(...) {
    ...
}
```

Spring Transaction Timeout과 TCF Timeout은 목적이 유사하지만 적용 범위와 실제 동작이 다를 수 있습니다.

두 설정을 함께 사용할 경우 우선순위와 값을 정합성 있게 관리해야 합니다.

### 32.12 Timeout 이후 실제 상태

등록 거래 Timeout이 발생했다고 가정합니다.

```
사용자
→ 등록 요청

서버
→ DB INSERT 성공
→ 응답 직전에 Timeout
```

사용자는 실패 응답을 받았지만 DB에는 등록되었을 수 있습니다.

따라서 변경 거래의 Timeout 응답에는 다음 안내가 필요할 수 있습니다.

```
처리결과를 확인한 후 다시 시도해 주세요.
```

무조건 재시도하면 중복 등록이 발생할 수 있습니다.

변경 거래에는 Idempotency Key가 중요한 이유입니다.

### 32.13 Timeout 오류 응답

```
{
  "result": {
    "resultStatus": "ERROR",
    "resultCode": "E-TCF-TIME-0001",
    "message": "거래 처리시간을 초과했습니다. 처리결과를 확인해 주세요."
  },
  "body": null,
  "error": {
    "errorCode": "E-TCF-TIME-0001",
    "errorType": "TIMEOUT",
    "retryable": false
  }
}
```

조회 거래는 다시 조회해도 안전할 가능성이 높습니다.

등록·변경 거래는 Idempotency 상태와 DB 반영 여부를 먼저 확인해야 합니다.

### 32.14 Timeout을 늘리기 전에 확인할 것

```
SQL이 느린가?
DB Connection 획득을 기다리는가?
외부 시스템이 느린가?
Thread Pool이 부족한가?
GC가 길게 발생했는가?
Lock 대기가 있는가?
대량 데이터를 한 번에 처리하는가?
```

원인을 확인하지 않고 Timeout만 늘리면 자원 점유시간이 늘어나 장애가 더 커질 수 있습니다.

### 32.15 정상·금지 예시

정상:

```
조회 ServiceId Timeout = 3000ms
SQL Timeout = 2000ms
Gateway Timeout = 4000ms
화면 Timeout = 5000ms
```

금지:

```
화면 Timeout = 3000ms
Gateway Timeout = 10000ms
ServiceId Timeout = 30000ms
SQL Timeout 없음
```

화면은 이미 종료되었지만 서버와 DB는 계속 작업할 수 있습니다.

### 제32장 요약

```
TCF Timeout 하나만 설정해서는 부족하다.

ServiceId·외부 호출·SQL·Connection·Gateway Timeout을
계층적으로 정렬해야 한다.

Timeout 이후에는
실제 데이터 반영 여부를 확인해야 한다.
```

## 제33장. 거래통제와 실행 차단

### 33.1 거래통제란 무엇인가요?

거래통제는 운영자가 코드 배포 없이 특정 거래의 실행을 허용하거나 차단하는 기능입니다.

예:

```
장애가 발생한 고객조회만 중지한다.

캠페인 변경거래는 중지하고 조회는 허용한다.

점검시간에는 등록·변경을 차단한다.

특정 채널의 요청만 임시 차단한다.
```

### 33.2 서버를 내리는 것과 다른 점

거래 하나에 문제가 있다고 전체 업무 WAR를 중지하면 정상 거래도 사용할 수 없습니다.

```
sv-service 중지
→ 고객조회
→ 상품조회
→ 활동조회
→ 모든 SV 거래 중지
```

ServiceId 단위 거래통제를 사용하면 문제가 있는 거래만 차단할 수 있습니다.

```
SV.Customer.selectSummary
→ SUSPENDED

SV.Product.selectList
→ ACTIVE
```

### 33.3 통제 위치

거래통제는 Handler 실행 전에 수행해야 합니다.

```
STF
→ TransactionControlService.check()
→ 허용 시 Handler
→ 차단 시 오류 응답
```

차단 후 Handler를 실행했다가 결과만 버리는 구조는 의미가 없습니다.

### 33.4 통제 기준

| 통제 기준 | 예 |
| --- | --- |
| 전체 시스템 | 모든 온라인 변경 중지 |
| 업무코드 | CM 업무 중지 |
| ServiceId | 캠페인 등록만 중지 |
| 거래코드 | 모든 다운로드 거래 중지 |
| 채널 | 외부 API 채널만 차단 |
| 지점 | 특정 지점 임시 차단 |
| 사용자 | 비정상 사용자 차단 |
| 시간대 | 점검시간 변경 차단 |
| 처리유형 | 등록·변경만 차단 |
| 운영환경 | 운영에서만 정책 적용 |

### 33.5 통제 상태

권장 상태 예:

| 상태 | 의미 |
| --- | --- |
| ACTIVE | 정상 실행 |
| SUSPENDED | 일시 중지 |
| DISABLED | 사용 중지 |
| READ_ONLY | 조회만 허용 |
| DEPRECATED | 폐기 예정 |
| MAINTENANCE | 점검 중 |

모든 상태를 처음부터 구현할 필요는 없습니다.

초기에는 다음 세 가지로 시작할 수 있습니다.

```
ACTIVE
SUSPENDED
DISABLED
```

### 33.6 통제 우선순위

여러 정책이 동시에 존재할 수 있습니다.

예:

```
전체 시스템 ACTIVE
CM 업무 ACTIVE
CM.Campaign.create SUSPENDED
WEBTOP 채널 ACTIVE
```

최종 결과:

```
CM.Campaign.create
→ SUSPENDED
```

권장 우선순위:

```
긴급 전체 차단
→ ServiceId 차단
→ 업무코드 차단
→ 채널·지점·사용자 차단
→ 기본 허용 정책
```

정확한 우선순위는 OM 설계서에 고정해야 합니다.

### 33.7 Fail-closed와 Fail-open

OM 정책 저장소를 조회할 수 없다고 가정합니다.

#### Fail-closed

```
정책 확인 실패
→ 거래 차단
```

장점:

- 보안과 통제 우선
- 미등록 거래 실행 방지
단점:

- OM 장애가 전체 거래 장애로 확대될 수 있음

#### Fail-open

```
정책 확인 실패
→ 기본 허용
```

장점:

- 가용성 우선
단점:

- 차단해야 할 거래가 실행될 수 있음
금융권 중요 변경 거래는 Fail-closed를 우선 검토하고, 저위험 조회는 캐시된 최종 정책을 사용하는 방법을 검토할 수 있습니다.

### 33.8 정책 Cache

모든 요청마다 OM DB를 조회하면 부하와 의존성이 커질 수 있습니다.

권장 구조:

```
OM DB
→ 정책 Cache
→ STF 조회
```

필요 기능:

```
초기 Load
변경 이벤트 반영
TTL
마지막 정상 정책 유지
Cache 상태 모니터링
긴급 무효화
```

### 33.9 통제 오류 응답

```
{
  "result": {
    "resultStatus": "FAIL",
    "resultCode": "E-TCF-CTRL-0001",
    "message": "현재 점검 중인 거래입니다. 잠시 후 다시 시도해 주세요."
  },
  "error": {
    "errorCode": "E-TCF-CTRL-0001",
    "errorType": "TRANSACTION_CONTROL",
    "retryable": true
  }
}
```

내부 응답에 통제 사유 전체를 노출하지 않을 수 있습니다.

운영 로그에는 다음을 기록합니다.

```
policyId
serviceId
controlStatus
controlReason
effectiveFrom
effectiveTo
operatorId
```

### 33.10 거래통제 감사

거래중지는 운영에 큰 영향을 줄 수 있습니다.

따라서 다음 정보가 필요합니다.

| 항목 | 설명 |
| --- | --- |
| 대상 | 업무코드·ServiceId |
| 변경 전 상태 | ACTIVE |
| 변경 후 상태 | SUSPENDED |
| 변경 사유 | 장애 확산 방지 |
| 작업자 | 운영자 ID |
| 승인자 | 승인자 ID |
| 적용일시 | 실제 적용시각 |
| 종료일시 | 자동 해제시각 |
| 관련 장애번호 | Incident ID |

### 33.11 금지 예시

```
if ("SV.Customer.selectSummary".equals(serviceId)) {
    throw new RuntimeException("임시 중지");
}
```

문제:

- 코드 배포 필요
- 작업자와 사유 추적 불가
- 자동 해제 불가
- 환경별 통제 어려움
- 운영자가 직접 제어할 수 없음

### 제33장 요약

```
거래통제는 서버 전체를 내리지 않고
ServiceId 단위로 실행을 차단하는 기능이다.

STF에서 Handler 실행 전에 확인하고,
변경 이력과 승인정보를 감사해야 한다.
```

## 제34장. Idempotency·재시도·재처리

### 34.1 같은 요청이 두 번 오는 이유

중복 요청은 특별한 상황이 아닙니다.

```
사용자가 버튼을 두 번 클릭

화면이 응답을 받지 못해 자동 재시도

Gateway가 연결 오류 후 재전송

네트워크가 끊겨 사용자가 다시 요청

Batch가 실패 건을 재처리
```

조회 거래는 반복되어도 데이터가 바뀌지 않지만 등록·변경 거래는 문제가 될 수 있습니다.

### 34.2 Idempotency란 무엇인가요?

```
같은 요청을 한 번 실행해도
여러 번 실행해도
최종 업무 결과가 같도록 하는 특성
```

예:

```
캠페인 등록 요청 3번
→ 캠페인 1건만 생성
```

### 34.3 Idempotency Key

클라이언트는 변경 요청에 고유 키를 보냅니다.

```
{
  "header": {
    "serviceId": "CM.Campaign.create",
    "idempotencyKey": "CM-USER01-20260717-000001"
  }
}
```

서버는 키만 비교하지 않고 다음 정보도 함께 검증할 수 있습니다.

```
사용자
ServiceId
요청 Hash
업무코드
유효기간
```

같은 키에 서로 다른 Body를 보내는 요청은 차단해야 합니다.

### 34.4 Idempotency 상태

| 상태 | 의미 |
| --- | --- |
| PROCESSING | 현재 처리 중 |
| SUCCESS | 정상 완료 |
| FAIL | 실패 완료 |
| TIMEOUT | 시간 초과 |
| UNKNOWN | 최종 상태 확인 필요 |
| EXPIRED | 보관기간 만료 |

처리 흐름:

```
키 없음
→ 요청 성격에 따라 오류 또는 일반 처리

신규 키
→ PROCESSING 저장
→ 거래 실행

SUCCESS 키
→ 기존 결과 반환

PROCESSING 키
→ 중복 진행 차단

FAIL 키
→ 재시도 정책 판단

TIMEOUT·UNKNOWN
→ 실제 업무 반영 여부 확인
```

### 34.5 요청 Hash

동일 키의 요청 Body가 같은지 확인하기 위해 Hash를 저장할 수 있습니다.

```
idempotencyKey = ABC001
requestHash = SHA-256(정규화된 요청 Body)
```

다음 요청은 오류입니다.

```
첫 요청
key=ABC001
campaignName=여름 캠페인

두 번째 요청
key=ABC001
campaignName=겨울 캠페인
```

같은 키가 다른 업무 요청을 가리키면 안 됩니다.

### 34.6 성공 결과 재사용

첫 번째 요청:

```
Idempotency = SUCCESS
campaignId = CMP202600001
```

같은 키가 다시 들어오면 새 캠페인을 등록하지 않고 기존 결과를 반환합니다.

```
{
  "body": {
    "campaignId": "CMP202600001",
    "campaignStatus": "DRAFT"
  }
}
```

응답을 재사용할 때 개인정보와 보관기간을 고려해야 합니다.

### 34.7 PROCESSING 고착

서버가 작업 중 비정상 종료되면 상태가 PROCESSING에 남을 수 있습니다.

```
PROCESSING 등록
→ 서버 중단
→ 성공·실패 상태 미기록
```

필요한 운영정책:

```
PROCESSING 최대 유지시간
Stale 상태 탐지
실제 업무 데이터 확인
UNKNOWN 전환
운영자 재처리
```

### 34.8 재시도 가능한 오류

일반적으로 다음 오류는 제한적 재시도를 검토할 수 있습니다.

```
일시적인 네트워크 연결 실패
외부 시스템 503
DB 일시 접속 실패
Lock Timeout
일시적 Rate Limit
```

다음 오류는 자동 재시도하면 안 됩니다.

```
필수값 오류
권한 오류
중복 업무 데이터
승인 불가 상태
SQL 문법 오류
프로그램 NullPointerException
```

### 34.9 재시도 정책

권장 요소:

```
최대 재시도 횟수
재시도 간격
지수 증가
Jitter
재시도 대상 오류코드
전체 시간 예산
```

예:

```
1차 실패
→ 200ms 대기

2차 실패
→ 500ms 대기

3차 실패
→ 1000ms 대기

최종 실패
→ 오류 반환 또는 재처리 Queue
```

온라인 요청에서 과도한 재시도는 ServiceId Timeout을 소진할 수 있습니다.

### 34.10 변경 거래 재시도 주의

다음 상황을 생각해 봅시다.

```
외부기관에 등록 요청
→ 외부기관은 등록 성공
→ 응답 네트워크 단절
→ 내부에서는 실패로 판단
```

그대로 재시도하면 외부기관에 두 번 등록될 수 있습니다.

따라서 다음 중 하나가 필요합니다.

- 외부기관 Idempotency Key
- 업무 조회를 통한 처리결과 확인
- 요청번호 기반 중복검사
- 보상 거래
- 수동 확인 후 재처리

### 34.11 재시도와 재처리 차이

| 구분 | 재시도 | 재처리 |
| --- | --- | --- |
| 시점 | 즉시 또는 짧은 시간 | 운영 판단 후 |
| 주체 | 프로그램 | 운영자·Batch |
| 대상 | 일시 장애 | 실패·미확정 거래 |
| 횟수 | 제한적 | 정책 기반 |
| 기록 | Retry Count | 재처리 이력 |
| 승인 | 보통 없음 | 중요 거래는 필요 |

### 34.12 Dead Letter와 실패보관

비동기 거래나 Batch에서는 반복 실패한 요청을 별도로 보관할 수 있습니다.

```
정상 Queue
→ 처리 실패
→ 재시도
→ 최대 횟수 초과
→ 실패 Queue·DLQ
→ 운영자 확인
→ 재처리
```

실패 데이터를 무한 반복 처리하면 장애가 계속 확대될 수 있습니다.

### 제34장 요약

```
중복방지는 같은 요청의 중복 반영을 막는다.

재시도는 일시 장애를 다시 실행하는 것이다.

등록·변경 거래는
Idempotency 없이 자동 재시도하면 안 된다.

Timeout과 UNKNOWN 상태는
실제 반영 여부를 확인한 후 재처리한다.
```

## 제35장. 거래로그·감사로그·추적 ID

### 35.1 로그가 많다고 추적 가능한 것은 아니다

다음 로그는 운영에 큰 도움이 되지 않습니다.

```
조회 시작
조회 종료
오류 발생
처리 완료
```

어느 사용자, 어느 ServiceId, 어느 요청인지 알 수 없기 때문입니다.

좋은 로그는 다음 질문에 답할 수 있어야 합니다.

```
어느 사용자의 요청인가?

어느 ServiceId인가?

어느 업무 WAR에서 처리했는가?

언제 시작하고 끝났는가?

어느 SQL이 느렸는가?

어떤 오류코드로 실패했는가?

다른 시스템 호출과 연결되는가?
```

### 35.2 로그 종류

| 로그 | 목적 |
| --- | --- |
| 애플리케이션 로그 | 개발·오류 분석 |
| 거래로그 | 거래 시작·종료·처리시간 |
| 감사로그 | 중요 데이터 접근·변경 증적 |
| SQL 로그 | Mapper와 수행시간 |
| 연계로그 | 외부 송수신 상태 |
| 보안로그 | 인증·권한·비정상 접근 |
| Metric | 집계·경보·대시보드 |

하나의 로그에 모든 정보를 넣는 것이 아니라 목적별로 구분합니다.

### 35.3 GUID와 TraceId

#### GUID

```
화면
→ Gateway
→ SV
→ IC
→ 외부 시스템
```

전체 흐름에서 같은 GUID를 유지합니다.

#### TraceId

하나의 애플리케이션 실행 흐름과 로그를 연결합니다.

```
GUID
= 전체 업무 거래

TraceId
= 내부 실행과 로그 추적
```

분산 추적 도구를 적용하면 SpanId를 추가할 수 있습니다.

### 35.4 거래로그 시작과 종료

STF:

```
TxLog.start
status = PROCESSING
startDtm
serviceId
guid
traceId
userId
```

ETF:

```
TxLog.end
status = SUCCESS | BUSINESS_FAIL |
         SYSTEM_ERROR | TIMEOUT | REJECTED
endDtm
elapsedMs
resultCode
```

STF의 마지막 단계에서 거래로그를 시작하고 ETF에서 종료상태를 확정하는 구조가 TCF의 기본 추적 모델입니다.

### 35.5 거래로그 항목

| 항목 | 설명 |
| --- | --- |
| GUID | 시스템 간 거래 식별 |
| TraceId | 내부 실행 식별 |
| ServiceId | 기능 식별 |
| 거래코드 | 운영 분류 |
| 업무코드 | 업무 WAR |
| 채널 | WEBTOP·API 등 |
| 사용자 | 사용자 ID |
| 지점 | 지점 ID |
| 시작시각 | 거래 시작 |
| 종료시각 | 거래 종료 |
| 처리시간 | elapsedMs |
| 결과상태 | SUCCESS 등 |
| 결과코드 | 오류코드 |
| 대상 WAR | 실행 인스턴스 |
| Server ID | 서버·Pod·Tomcat |
| Retry Count | 재시도 횟수 |
| Idempotency Key | 변경 거래 중복키 |

### 35.6 애플리케이션 로그와 MDC

MDC에 다음 값을 설정할 수 있습니다.

```
MDC.put("traceId", context.getTraceId());
MDC.put("guid", context.getGuid());
MDC.put("serviceId", context.getServiceId());
MDC.put("businessCode", context.getBusinessCode());
```

로그 패턴:

```
2026-07-17 10:30:00
traceId=T001
serviceId=SV.Customer.selectSummary
level=INFO
message=customer summary inquiry started
```

개발자가 매 로그마다 TraceId를 직접 문자열로 붙이지 않아도 됩니다.

### 35.7 구조화 로그

문자열 로그보다 JSON 구조화 로그를 사용하면 검색과 집계가 쉽습니다.

```
{
  "timestamp": "2026-07-17T10:30:00.123+09:00",
  "level": "INFO",
  "traceId": "T202607170001",
  "guid": "G202607170001",
  "serviceId": "SV.Customer.selectSummary",
  "businessCode": "SV",
  "event": "TRANSACTION_END",
  "result": "SUCCESS",
  "elapsedMs": 145
}
```

### 35.8 SQL 추적 로그

```
{
  "traceId": "T202607170001",
  "serviceId": "SV.Customer.selectSummary",
  "mapperId": "SvCustomerMapper.selectCustomerSummary",
  "elapsedMs": 42,
  "rowCount": 1,
  "result": "SUCCESS"
}
```

SQL 원문 전체와 Parameter 전체를 항상 기록하지 않습니다.

### 35.9 감사로그

감사로그는 단순 디버깅 로그가 아닙니다.

예:

```
고객 개인정보 조회
캠페인 승인
권한 변경
사용자 상태 변경
대량 파일 다운로드
거래통제 상태 변경
```

감사 항목:

| 항목 | 설명 |
| --- | --- |
| 행위자 | 사용자·운영자 |
| 행위 | 조회·변경·승인·다운로드 |
| 대상 | 고객·캠페인·권한 |
| 목적 | 업무 목적 |
| 시각 | 서버 기준 |
| 결과 | 성공·실패 |
| TraceId | 거래 추적 |
| 변경 전후 | 중요 변경 |
| 접속 채널 | 단말·API |
| 지점 | 사용자 소속 |

### 35.10 로그 마스킹

기록 금지 또는 마스킹 대상:

```
비밀번호
JWT 원문
Refresh Token
Private Key
주민등록번호
계좌번호
카드번호
전화번호
주소
고객정보 전체 JSON
```

예:

```
고객번호
CUST000001
→ CU******01

주민번호
900101-1234567
→ 900101-1******
```

마스킹 방식은 개인정보 표준에 따라야 합니다.

### 35.11 로그 레벨

| 레벨 | 사용 예 |
| --- | --- |
| DEBUG | 개발용 상세 흐름 |
| INFO | 거래 시작·종료·중요 상태 |
| WARN | 재시도·지연·예상 가능한 이상 |
| ERROR | 시스템 오류·처리 실패 |

업무 오류를 모두 ERROR로 기록하면 정상적인 업무 거절이 시스템 장애처럼 보입니다.

예:

```
고객 없음
→ INFO 또는 WARN + BUSINESS_FAIL

DB Connection 실패
→ ERROR + SYSTEM_ERROR
```

### 35.12 중복 로그 방지

한 예외는 최종 책임 계층에서 한 번 Stack Trace를 기록하는 것이 좋습니다.

```
DAO
→ 예외 변환만 수행

Service
→ 추가 Error 로그 없음

Facade
→ 예외 전파

TCF·ETF
→ TraceId와 함께 최종 Error 로그
```

업무 문맥 추가가 필요하면 Stack Trace 없이 보조 로그를 남길 수 있습니다.

### 35.13 로그 보관과 용량

모든 요청·응답 원문을 저장하면 저장공간과 개인정보 위험이 커집니다.

설계 항목:

```
로그 유형별 보관기간
압축
Archive
삭제
개인정보 마스킹
검색 인덱스
최대 필드 길이
Sampling
```

정상 거래의 상세로그는 Sampling하고 오류 거래는 상세 보관하는 방식도 검토할 수 있습니다.

### 제35장 요약

```
거래로그는 거래 상태를 기록하고,
감사로그는 중요 행위를 증명하며,
애플리케이션 로그는 원인을 분석한다.

GUID·TraceId·ServiceId가
모든 로그를 연결하는 공통 열쇠다.
```

## 제36장. 장애 흐름과 운영 대응

### 36.1 장애와 오류의 차이

모든 오류가 장애는 아닙니다.

```
고객번호 미입력
→ 오류이지만 시스템 장애 아님

캠페인 상태 오류
→ 업무 오류

DB Connection 전체 실패
→ 시스템 장애

모든 거래 Timeout 급증
→ 시스템 장애 가능
```

장애는 서비스 품질이나 여러 사용자의 업무에 영향을 주는 상태입니다.

### 36.2 장애 발생 시 첫 질문

```
모든 거래가 느린가?

특정 업무 WAR만 느린가?

특정 ServiceId만 느린가?

특정 SQL만 느린가?

특정 사용자·지점·채널만 실패하는가?

최근 배포 이후 발생했는가?
```

처음부터 소스코드 전체를 확인하지 않습니다.

범위를 좁히는 것이 먼저입니다.

### 36.3 장애 점검 순서

```
1. 영향범위 확인
2. ServiceId 확인
3. TraceId 표본 확보
4. 거래로그 결과 분포 확인
5. Tomcat Thread 확인
6. JVM CPU·Heap·GC 확인
7. DB Pool 확인
8. Slow SQL·Lock 확인
9. 외부 연계 상태 확인
10. 최근 배포·설정 변경 확인
```

### 36.4 증상별 판단

#### 전체 거래가 느리다

가능성:

```
JVM CPU 과다
Full GC
Tomcat Thread 고갈
DB Pool 고갈
DB 전체 부하
네트워크 장애
```

#### 특정 ServiceId만 느리다

가능성:

```
특정 SQL
대량 데이터
외부 호출
비효율적 반복
ServiceId Timeout 부적정
```

#### Connection 획득 Timeout

가능성:

```
느린 SQL이 Connection 점유
Pool 크기 부족
Connection 누수
DB 장애
장기 트랜잭션
```

#### 특정 WAR만 느리다

가능성:

```
업무 WAR 자원 독점
해당 WAR의 DB Pool 문제
특정 배포 결함
특정 업무 트래픽 급증
```

### 36.5 운영 대시보드 핵심 항목

| 영역 | 지표 |
| --- | --- |
| 거래 | TPS, 성공률, 오류율 |
| 응답시간 | 평균, p95, p99 |
| Timeout | ServiceId별 건수 |
| Thread | Active, Busy, Queue |
| JVM | CPU, Heap, GC Pause |
| DB Pool | Active, Idle, Pending |
| SQL | Slow SQL, 평균시간 |
| 오류 | 오류코드 Top N |
| 외부연계 | 성공률, Timeout |
| WAR | 업무 WAR별 자원 사용량 |

### 36.6 장애 완화 조치

원인이 확인되기 전에도 영향 확대를 막아야 할 수 있습니다.

가능한 조치:

```
문제 ServiceId 일시 중지
다운로드·대량조회 차단
특정 채널 트래픽 제한
Timeout 단축
외부 호출 Circuit Open
WAS 인스턴스 격리
이전 버전 Rollback
DB 세션·Lock 조치
```

무조건 서버 재시작부터 하면 원인 증적이 사라질 수 있습니다.

재시작 전에 가능한 범위에서 Thread Dump, JVM 상태, 거래로그, DB 상태를 확보합니다.

### 36.7 거래통제 활용

특정 캠페인 목록조회가 DB에 과부하를 발생시킨다고 가정합니다.

```
CM.Campaign.selectList
→ SUSPENDED
```

다른 캠페인 기능은 계속 사용할 수 있습니다.

```
CM.Campaign.selectDetail
→ ACTIVE

CM.Campaign.update
→ ACTIVE
```

### 36.8 Circuit Breaker 개념

외부 시스템이 계속 실패하는데 매 요청마다 호출하면 Thread가 계속 대기합니다.

```
외부 호출 연속 실패
→ Circuit OPEN
→ 일정 시간 즉시 실패
→ 복구 확인 후 HALF_OPEN
→ 정상 시 CLOSED
```

TCF의 거래통제와 외부 Client의 Circuit Breaker는 목적이 다릅니다.

```
거래통제
= 운영자가 ServiceId 실행을 제어

Circuit Breaker
= 하위 시스템 장애 전파 자동 차단
```

### 36.9 장애 복구 후 확인

```
거래통제 해제
→ 소량 요청 검증
→ 오류율 확인
→ p95 확인
→ DB Pool 확인
→ 점진적 정상화
```

장애 원인이 해결되었다고 즉시 전체 트래픽을 허용하면 다시 장애가 발생할 수 있습니다.

### 36.10 미확정 거래 확인

장애와 Timeout 이후에는 다음 거래를 별도로 찾아야 합니다.

```
PROCESSING 상태가 오래 지속
TIMEOUT 상태
UNKNOWN 상태
외부 성공·내부 실패
내부 성공·응답 실패
```

업무 데이터와 Idempotency 상태를 대조하여 재처리 여부를 판단합니다.

### 36.11 장애 보고 항목

| 항목 | 내용 |
| --- | --- |
| 장애 시작·종료 | 정확한 시각 |
| 영향범위 | 사용자·업무·ServiceId |
| 증상 | 오류율·응답시간 |
| 직접 원인 | SQL, Pool, 배포 등 |
| 근본 원인 | 설계·검증·운영 Gap |
| 임시 조치 | 거래중지·Rollback |
| 복구 조치 | 패치·설정 변경 |
| 미확정 거래 | 건수와 처리 |
| 재발방지 | 자동검증·모니터링 |
| 담당·기한 | Action Item |

### 36.12 장애 대응 금지 예시

```
원인 확인 없이 모든 서버 재시작

Timeout을 무조건 10배 증가

오류로그 전체 삭제

실패 거래를 일괄 재처리

운영 DB 데이터를 직접 수정

통제 해제 후 검증 없이 전체 트래픽 허용
```

### 제36장 요약

```
장애 대응은
영향범위를 좁히고 원인을 찾는 과정이다.

ServiceId와 TraceId를 기준으로
Thread·JVM·DB Pool·SQL·외부연계를
순서대로 확인한다.

복구 후에는 미확정 거래와 재발방지를 확인한다.
```

## 3. 목표 아키텍처

```
[사용자 / UI]
      │
      ▼
[Gateway]
 인증·Route·외부 Timeout
      │
      ▼
[OnlineTransactionController]
      │
      ▼
[TCF.process]
      │
      ▼
[STF]
 ├─ Header 검증
 ├─ GUID·TraceId
 ├─ 인증·권한
 ├─ 거래통제
 ├─ Timeout 정책
 ├─ Idempotency
 └─ TxLog.start(PROCESSING)
      │
      ▼
[TimeoutExecutor]
      │
      ▼
[Dispatcher]
      │
      ▼
[Handler → Facade → Service]
          │
          ├─ Rule
          ├─ DAO → Mapper → DB
          └─ 외부 Client
      │
      ▼
[TCF 예외 분류]
 ├─ BusinessException
 ├─ TimeoutException
 └─ SystemException
      │
      ▼
[ETF]
 ├─ Idempotency 종료
 ├─ TxLog.end
 ├─ Audit
 ├─ Metric
 └─ StandardResponse
      │
      ▼
[OM / 운영]
 Service Catalog
 거래통제
 Timeout
 오류통계
 거래조회
 장애대응
```

## 4. 표준 형식

### 4.1 오류 응답 표준

```
{
  "header": {
    "businessCode": "CM",
    "serviceId": "CM.Campaign.create",
    "transactionCode": "CM-REG-0001",
    "guid": "G202607170001",
    "traceId": "T202607170001"
  },
  "result": {
    "resultStatus": "FAIL",
    "resultCode": "E-CM-CAM-0005",
    "message": "동일한 캠페인이 이미 존재합니다."
  },
  "body": null,
  "error": {
    "errorCode": "E-CM-CAM-0005",
    "errorType": "BUSINESS",
    "retryable": false
  }
}
```

### 4.2 거래로그 표준

```
{
  "guid": "G202607170001",
  "traceId": "T202607170001",
  "businessCode": "CM",
  "serviceId": "CM.Campaign.create",
  "transactionCode": "CM-REG-0001",
  "status": "SUCCESS",
  "resultCode": "S0000",
  "startDtm": "2026-07-17T10:30:00.100+09:00",
  "endDtm": "2026-07-17T10:30:00.480+09:00",
  "elapsedMs": 380,
  "serverId": "cm-was-01"
}
```

### 4.3 Idempotency 표준

```
{
  "idempotencyKey": "CM-USER01-20260717-000001",
  "serviceId": "CM.Campaign.create",
  "requestHash": "a72f...",
  "status": "SUCCESS",
  "resultReference": "CMP202600001",
  "createdDtm": "2026-07-17T10:30:00+09:00",
  "completedDtm": "2026-07-17T10:30:00+09:00"
}
```

## 5. 구성요소 및 속성

| 구성요소 | 핵심 속성 |
| --- | --- |
| Error Registry | 오류코드, 메시지, 유형 |
| Exception | 오류코드, 원인 예외 |
| Timeout Policy | ServiceId, 제한시간 |
| Transaction Control | 대상, 상태, 사유 |
| Idempotency Store | Key, Hash, 상태, 결과 |
| Transaction Log | 시작·종료·처리시간 |
| Audit Log | 행위자·대상·행위 |
| MDC | GUID·TraceId·ServiceId |
| Metric | 성공·오류·Timeout 건수 |
| Alert | 임계치·심각도·대상 |

## 6. 책임 경계와 RACI

| 활동 | AA | FW | DEV | OM | TA | DBA | SEC | QA | OPS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 오류 분류 기준 | A | R | C | C | C | I | C | C | I |
| 업무 오류코드 | C | C | A/R | C | I | I | I | C | I |
| 공통 오류코드 | A | R | C | C | C | I | C | C | I |
| ETF 구현 | A | R | C | C | C | I | C | C | I |
| Timeout 정책 | A | C | C | C | R | C | I | C | C |
| SQL Timeout | C | C | R | I | C | A/C | I | C | C |
| 거래통제 | A | C | I | R | C | I | C | C | A/C |
| Idempotency | A | R | R | C | C | C | I | C | C |
| 거래로그 | A | R | C | C | C | I | C | C | R |
| 감사로그 | C | C | C | C | I | I | A/R | C | C |
| 장애 테스트 | C | C | C | C | C | C | C | A/R | R |
| 운영 대응 | C | C | I | C | R | R | C | C | A/R |

```
R = 수행
A = 최종 책임
C = 협의
I = 공유
```

## 7. 정상 처리 흐름

```
1. 요청 수신

2. STF가 TraceId와 Context 생성

3. 인증·권한 확인

4. 거래통제 확인

5. Timeout 정책 적용

6. Idempotency PROCESSING 기록

7. 거래로그 PROCESSING 시작

8. Handler 이하 업무 실행

9. 업무 트랜잭션 Commit

10. ETF 성공 처리

11. Idempotency SUCCESS

12. 거래로그 SUCCESS 종료

13. 감사·Metric 기록

14. 표준 성공 응답
```

## 8. 오류·Timeout·장애 흐름

### 8.1 입력 오류

```
DTO Validation 실패
→ Handler 실행 전 또는 초기에 차단
→ BUSINESS·VALIDATION 응답
→ 시스템 경보 없음
```

### 8.2 업무 오류

```
Rule에서 BusinessException
→ Facade Rollback
→ ETF.businessFail
→ TxLog BUSINESS_FAIL
→ 업무 오류 응답
```

### 8.3 시스템 오류

```
DB·프로그램 예외
→ SystemException
→ Facade Rollback
→ ETF.systemError
→ TxLog SYSTEM_ERROR
→ 운영 경보
```

### 8.4 Timeout

```
실행시간 초과
→ 작업 취소 시도
→ 트랜잭션 Rollback
→ Idempotency TIMEOUT·UNKNOWN
→ TxLog TIMEOUT
→ 실제 반영 여부 확인
```

### 8.5 거래통제

```
OM 상태 SUSPENDED
→ STF 차단
→ Handler 미실행
→ TxLog REJECTED
→ 통제 오류 응답
```

## 9. 정상 예시

```
ServiceId
CM.Campaign.create

Idempotency Key
CM-USER01-20260717-000001

Timeout
5000ms

처리시간
430ms

DB
Master INSERT 성공
History INSERT 성공
Commit

거래로그
SUCCESS

응답
campaignId=CMP202600001
```

## 10. 금지 예시

### 10.1 모든 오류를 RuntimeException으로 처리

```
throw new RuntimeException("오류");
```

### 10.2 예외 무시

```
catch (Exception e) {
}
```

### 10.3 시스템 상세정보 응답

```
{
  "message": "ORA-00942 at CmCampaignMapper.xml line 35"
}
```

### 10.4 Timeout만 증가

```
SQL 튜닝 없음
Pool 점검 없음
Timeout 3초 → 60초
```

### 10.5 Timeout 후 무조건 재시도

```
등록 결과 확인 없음
Idempotency 없음
같은 요청 재전송
```

### 10.6 거래통제 코드 하드코딩

```
if (serviceId.equals("...")) {
    throw new RuntimeException();
}
```

### 10.7 개인정보 원문 로그

```
log.info("request={}", request);
```

## 11. 연계 규칙

외부 시스템 호출 시 다음 정보를 전달합니다.

```
GUID
TraceId
ServiceId
호출 ServiceId
요청시각
Idempotency Key 또는 업무 요청번호
```

하위 오류는 내부 표준 오류로 변환합니다.

```
외부 404
→ 무조건 고객 없음으로 변환하지 않음

외부 Timeout
→ EXTERNAL_TIMEOUT

외부 503
→ EXTERNAL_UNAVAILABLE

외부 업무코드
→ 내부 업무 오류 매핑
```

외부 원본 오류 메시지를 사용자에게 그대로 노출하지 않습니다.

## 12. 데이터 및 상태관리

### 12.1 거래 상태

```
RECEIVED
→ PROCESSING
→ SUCCESS

PROCESSING
→ BUSINESS_FAIL

PROCESSING
→ SYSTEM_ERROR

PROCESSING
→ TIMEOUT

PROCESSING
→ UNKNOWN

RECEIVED
→ REJECTED
```

### 12.2 미종료 거래

다음 조건을 정기 점검합니다.

```
PROCESSING 상태가 최대 처리시간 초과
종료시각 없음
결과코드 없음
Idempotency와 업무 데이터 불일치
```

미종료 거래는 무조건 실패로 바꾸지 말고 실제 처리결과를 확인합니다.

## 13. 성능·용량·확장성

| 영역 | 기준 |
| --- | --- |
| 거래로그 | 온라인 처리에 과도한 동기 부하 금지 |
| 감사로그 | 유실 방지와 성능 균형 |
| Idempotency | Key 인덱스와 TTL |
| 정책 Cache | 매 요청 OM DB 조회 방지 |
| Timeout | 하위·상위 계층 정렬 |
| Metric | 고카디널리티 Label 제한 |
| 로그 | 대용량 Body 원문 기록 금지 |
| 오류 급증 | Rate 기반 경보 |
| 재시도 | Retry Storm 방지 |
| 미확정 거래 | 주기적 정리 Job |

## 14. 보안·개인정보·감사

```
오류 응답에 Stack Trace를 노출하지 않는다.

권한 실패는 감사 대상 여부를 검토한다.

거래통제 변경은 작업자·승인자를 기록한다.

Idempotency 저장소에 요청 원문을 무조건 저장하지 않는다.

로그와 감사정보는 역할별 접근권한을 분리한다.

개인정보는 마스킹·암호화·보관기간을 적용한다.
```

## 15. 운영·모니터링·장애 대응

운영 경보 예:

| 지표 | 경보 예 |
| --- | --- |
| 시스템 오류율 | 5분간 1% 초과 |
| Timeout율 | 5분간 0.5% 초과 |
| 특정 오류코드 | 평소 대비 급증 |
| p95 응답시간 | 목표 3초 초과 |
| DB Pool Pending | 지속 발생 |
| Busy Thread | 70% 초과 |
| PROCESSING 고착 | 최대시간 초과 |
| 외부 실패율 | 임계치 초과 |
| 거래통제 변경 | 즉시 알림 |

실제 임계값은 운영 기준과 성능시험 결과로 확정합니다.

## 16. 자동검증 및 품질 Gate

| Gate | 검증 기준 |
| --- | --- |
| 예외 | RuntimeException("오류") 금지 |
| 오류코드 | Registry 미등록 코드 금지 |
| 메시지 | Stack Trace·SQL 노출 금지 |
| Timeout | ServiceId 정책 등록 |
| SQL | Query Timeout 적용 대상 확인 |
| 외부 Client | Connection·Read Timeout 존재 |
| 거래통제 | OM Catalog와 정책 존재 |
| Idempotency | 변경 거래 적용 여부 |
| 로그 | TraceId·ServiceId 포함 |
| 개인정보 | 민감정보 로그 검사 |
| ETF | 모든 종료 경로 테스트 |
| Context | finally 정리 테스트 |
| 재시도 | 대상 오류와 최대 횟수 제한 |

## 17. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| ERR-001 | 필수값 누락 | Validation 오류 |
| ERR-002 | JWT 만료 | 인증 오류·Handler 미실행 |
| ERR-003 | 권한 없음 | 권한 오류 |
| ERR-004 | ServiceId 중지 | 거래통제 오류 |
| ERR-005 | 고객 없음 | BusinessException |
| ERR-006 | DB Connection 실패 | SystemException |
| ERR-007 | 예상하지 못한 예외 | 표준 시스템 오류 |
| ERR-008 | SQL 2초 초과 | Query Timeout |
| ERR-009 | ServiceId 3초 초과 | 거래 Timeout |
| ERR-010 | Timeout 후 DB 미반영 | 재시도 가능 판단 |
| ERR-011 | Timeout 후 DB 반영 | 기존 결과 확인 |
| ERR-012 | 동일 Key 동일 Body | 기존 결과 반환 |
| ERR-013 | 동일 Key 다른 Body | 중복키 오류 |
| ERR-014 | PROCESSING 고착 | UNKNOWN 전환·점검 |
| ERR-015 | 외부 503 | 제한적 재시도 |
| ERR-016 | 업무 오류 자동 재시도 | 재시도하지 않음 |
| ERR-017 | MDC 정리 | 다음 요청 정보 혼입 없음 |
| ERR-018 | 개인정보 로그 | 원문 미노출 |
| ERR-019 | 거래로그 시작 후 오류 | 종료상태 기록 |
| ERR-020 | ETF 처리 실패 | 보조 경로·경보 |
| ERR-021 | OM 정책 저장소 장애 | Fail 정책 확인 |
| ERR-022 | 정책 Cache 변경 | 최신 통제 반영 |
| ERR-023 | 거래통제 변경 | 감사로그 기록 |
| ERR-024 | 동일 예외 | Stack Trace 한 번 기록 |
| ERR-025 | TraceId 검색 | 전체 로그 연결 |

## 18. 제5부 체크리스트

### 18.1 오류 분류

| 점검 항목 | 확인 |
| --- | --- |
| 입력·업무·시스템 오류가 구분되는가? | □ |
| 인증과 권한 오류가 구분되는가? | □ |
| Timeout이 별도 유형으로 기록되는가? | □ |
| 거래통제 오류가 업무 오류와 구분되는가? | □ |
| 재시도 가능 여부가 정의되어 있는가? | □ |

### 18.2 예외

| 점검 항목 | 확인 |
| --- | --- |
| BusinessException을 사용하는가? | □ |
| SystemException에 원인 예외가 보존되는가? | □ |
| 예외를 잡고 무시하지 않는가? | □ |
| Facade Rollback이 검증되었는가? | □ |
| 같은 Stack Trace를 중복 기록하지 않는가? | □ |

### 18.3 오류코드

| 점검 항목 | 확인 |
| --- | --- |
| 공식 형식을 따르는가? | □ |
| Registry에 등록되었는가? | □ |
| 기존 코드 의미를 바꾸지 않았는가? | □ |
| 사용자 메시지가 이해 가능한가? | □ |
| 내부 SQL·경로가 노출되지 않는가? | □ |

### 18.4 Timeout

| 점검 항목 | 확인 |
| --- | --- |
| ServiceId Timeout이 등록되었는가? | □ |
| SQL Query Timeout이 있는가? | □ |
| 외부 Client Timeout이 있는가? | □ |
| 하위 Timeout이 상위보다 짧은가? | □ |
| Timeout 이후 반영 여부를 확인하는가? | □ |
| Timeout만 임의로 늘리지 않는가? | □ |

### 18.5 거래통제

| 점검 항목 | 확인 |
| --- | --- |
| Handler 실행 전에 확인하는가? | □ |
| ServiceId 단위 통제가 가능한가? | □ |
| 정책 우선순위가 정의되었는가? | □ |
| OM 장애 시 처리기준이 있는가? | □ |
| 통제 변경 감사로그가 있는가? | □ |
| 자동 해제 또는 종료일시가 있는가? | □ |

### 18.6 Idempotency와 재시도

| 점검 항목 | 확인 |
| --- | --- |
| 변경 거래에 중복방지 필요 여부를 검토했는가? | □ |
| 동일 Key의 요청 Hash를 검증하는가? | □ |
| PROCESSING 고착 처리기준이 있는가? | □ |
| 성공 결과 재사용 정책이 있는가? | □ |
| 자동 재시도 대상 오류가 제한되어 있는가? | □ |
| 최대 재시도 횟수가 있는가? | □ |

### 18.7 로그

| 점검 항목 | 확인 |
| --- | --- |
| GUID와 TraceId가 존재하는가? | □ |
| ServiceId와 거래코드가 기록되는가? | □ |
| 거래 시작·종료가 모두 기록되는가? | □ |
| SQL 처리시간을 추적할 수 있는가? | □ |
| 개인정보가 마스킹되는가? | □ |
| MDC가 finally에서 정리되는가? | □ |
| 감사로그가 일반 로그와 분리되는가? | □ |

## 19. 변경·호환성·폐기 관리

### 19.1 오류코드 변경

```
기존 오류코드 삭제 금지
기존 의미 변경 금지
신규 오류코드 추가
폐기 상태 관리
연계 시스템 영향 확인
```

### 19.2 Timeout 변경

Timeout 변경은 단순 설정 변경이 아닙니다.

확인 항목:

```
응답시간 영향
Thread 점유
DB Pool 점유
Gateway 설정
화면 Timeout
성능시험 결과
변경 사유
원복 기준
```

### 19.3 거래통제 정책 변경

```
변경 요청
→ 영향분석
→ 승인
→ 적용
→ 적용 확인
→ 감사로그
→ 해제 또는 종료
```

긴급 통제도 사후 승인과 장애번호 연결이 필요합니다.

### 19.4 Idempotency 보관기간

보관기간이 너무 짧으면 늦게 재전송된 요청을 막지 못합니다.

너무 길면 저장공간과 개인정보 위험이 커집니다.

```
업무 재전송 가능시간
+ 운영 재처리 기간
+ 감사 요구
```

를 고려하여 결정합니다.

## 20. 시사점

### 20.1 핵심 아키텍처 판단

TCF의 안정성은 정상 거래보다 실패 거래에서 결정됩니다.

```
정상 처리
= 업무 기능의 품질

오류 처리
= 시스템 전체의 품질
```

TCF의 공통 처리영역은 다음을 일관되게 수행해야 합니다.

```
STF
= 실행해도 되는 거래인지 확인

TimeoutExecutor
= 얼마나 오래 실행할지 통제

Dispatcher
= 어느 Handler를 실행할지 결정

ETF
= 어떤 상태로 종료되었는지 확정
```

### 20.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 모든 오류를 시스템 오류 처리 | 사용자·운영 대응 혼란 |
| 예외 무시 | 부분 Commit·데이터 오류 |
| Timeout 계층 불일치 | 종료 후 하위 작업 지속 |
| SQL Timeout 없음 | DB Connection 장기 점유 |
| 거래통제 하드코딩 | 운영 대응 지연 |
| Idempotency 없음 | 중복 등록·중복 승인 |
| Timeout 후 자동 재시도 | 중복 업무처리 |
| TraceId 누락 | 장애 원인 추적 불가 |
| 개인정보 로그 | 보안·감사 위반 |
| MDC 미정리 | 사용자 로그 혼입 |
| 재시도 무제한 | 장애 트래픽 증폭 |
| PROCESSING 고착 방치 | 미확정 거래 누적 |

### 20.3 우선 보완 과제

```
1. 오류 분류와 코드사전 확정
2. BusinessException·SystemException 표준화
3. ETF 종료경로 통합
4. ServiceId별 Timeout 등록
5. SQL·외부 Client Timeout 적용
6. 거래통제 OM 화면 구축
7. 변경 거래 Idempotency 적용
8. 거래로그·TraceId 표준화
9. 미확정 거래 점검 기능
10. 장애 시 ServiceId 단위 차단 절차
```

### 20.4 중장기 발전 방향

```
기본 오류로그
→ 구조화 로그
→ ServiceId Metric
→ 분산 Trace
→ 자동 이상징후 탐지
→ Circuit Breaker
→ 자동 거래통제
→ 실패 거래 자동 분류
→ 원인 후보 자동 제시
→ 안전한 자동 복구
```

자동 거래통제와 자동 복구는 충분한 검증과 승인체계 없이 바로 적용해서는 안 됩니다.

## 21. 마무리말

제5부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

```
업무 오류
= 예상 가능한 업무 실패

시스템 오류
= 기술적·예상하지 못한 실패

Timeout
= 시간 예산 초과

거래통제
= 운영정책에 의한 실행 차단

Idempotency
= 같은 변경 요청의 중복 반영 방지

TraceId
= 장애 로그를 연결하는 열쇠
```

안정적인 거래는 다음 구조를 가집니다.

```
요청
→ STF 검증
→ 거래통제
→ Timeout
→ Idempotency
→ 거래로그 시작
→ 업무 실행
→ 예외 분류
→ Rollback
→ ETF 종료
→ 감사·Metric
→ 표준 응답
```

초보 개발자는 오류가 발생했을 때 단순히 다음 코드를 추가해서는 안 됩니다.

```
try {
    ...
} catch (Exception e) {
    ...
}
```

대신 다음 질문을 먼저 해야 합니다.

```
이 오류는 사용자가 수정할 수 있는가?

예상 가능한 업무 상황인가?

Handler가 실행되기 전에 막아야 하는가?

트랜잭션을 Rollback해야 하는가?

Timeout 이후 실제 데이터는 반영되었는가?

다시 실행해도 중복 처리되지 않는가?

운영 경보가 필요한가?

TraceId로 원인을 찾을 수 있는가?

거래로그가 최종 상태로 종료되는가?
```

이 질문에 답할 수 있다면 단순히 오류를 잡는 개발자를 넘어, 장애가 발생해도 시스템과 데이터를 안전하게 보호하는 개발자가 될 수 있습니다.

