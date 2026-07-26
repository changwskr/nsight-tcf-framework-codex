
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제1부. TCF Framework 이해하기

## 1. 도입 전 안내말

처음 IT 개발 프로젝트에 참여하면 낯선 단어가 한꺼번에 등장합니다.

```
서버
API
Controller
Service
Transaction
Gateway
JWT
ServiceId
Handler
DAO
Mapper
Timeout
로그
```

이 단어들을 하나씩 검색하면 뜻은 알 수 있습니다. 하지만 실제 프로젝트에서 더 어려운 것은 각 기술이 어떤 순서로 연결되는지 이해하는 것입니다.

예를 들어 사용자가 화면에서 고객 조회 버튼을 클릭했다고 생각해 봅시다.

```
사용자가 고객 조회 버튼을 클릭
→ 화면이 서버에 요청
→ 서버가 사용자를 확인
→ 실행할 기능을 찾음
→ 업무 프로그램 실행
→ DB에서 고객정보 조회
→ 결과를 표준 형식으로 반환
→ 화면에 고객정보 표시
```

겉으로는 단순한 조회처럼 보이지만 실제 서버에서는 인증, 권한, 거래통제, Timeout, 로그, SQL 실행, 오류 처리 등이 함께 수행됩니다.

개발자가 이 모든 기능을 매번 직접 구현하면 프로그램마다 처리 방식이 달라집니다.

```
A 개발자의 고객 조회
- 자체 인증 처리
- 자체 오류 형식
- Timeout 없음
- 거래로그 없음

B 개발자의 캠페인 조회
- 다른 인증 처리
- 다른 오류 형식
- 자체 Timeout
- 별도 로그 형식
```

이렇게 되면 프로그램 수가 늘어날수록 시스템은 관리하기 어려워집니다.

NSIGHT TCF는 이 문제를 해결하기 위해 만들어진 공통 거래 처리 구조입니다.

```
업무 개발자는 업무 기능을 구현하고,
TCF는 거래가 실행되는 공통 절차를 책임진다.
```

제1부에서는 복잡한 소스코드를 깊게 분석하지 않습니다. 먼저 다음 네 가지 질문에 답할 수 있도록 설명합니다.

- TCF가 무엇인가?
- 사용자의 요청은 어느 시스템을 거쳐 가는가?
- 요청 한 건은 서버 내부에서 어떤 순서로 처리되는가?
- Handler, Facade, Service, Rule, DAO, Mapper는 각각 무엇을 하는가?

## 2. 제1부 개요

### 2.1 목적

제1부의 목적은 초보 개발자가 NSIGHT TCF의 전체 구조를 한 장의 그림처럼 이해하도록 하는 것입니다.

학습을 마친 뒤에는 다음 문장을 설명할 수 있어야 합니다.

```
화면 요청은 Gateway와 업무 WAR를 거쳐
공통 OnlineTransactionController에 도착한다.

TCF는 STF에서 요청을 검사하고,
ServiceId로 Handler를 찾는다.

Handler 아래에서는
Facade → Service → Rule → DAO → Mapper 순서로 업무를 처리한다.

처리가 끝나면 ETF가 표준 응답과 거래로그를 마무리한다.
```

### 2.2 적용 범위

제1부에서는 다음 내용을 다룹니다.

| 구분 | 학습 내용 |
| --- | --- |
| TCF 기본 개념 | TCF가 필요한 이유와 역할 |
| 시스템 구성 | UI, Gateway, 업무 WAR, TCF, DB 관계 |
| 거래 처리 흐름 | Controller부터 ETF까지의 처리 순서 |
| ServiceId | 실행할 기능을 식별하는 방법 |
| 6계층 | Handler부터 Mapper까지의 책임 |
| 정상 처리 | 고객 조회 요청의 전체 흐름 |
| 오류 처리 | 검증 오류, 업무 오류, 시스템 오류, Timeout |
| 운영 추적 | GUID, TraceId, ServiceId의 의미 |
| 기본 품질 기준 | 초보자가 지켜야 할 구조적 규칙 |

### 2.3 대상 독자

이 책은 다음 독자를 대상으로 합니다.

- IT 개발자로 처음 프로젝트에 참여한 사람
- Java와 Spring Boot를 기초 수준으로 배운 사람
- Controller와 Service는 알지만 대형 시스템 구조는 처음인 사람
- NSIGHT TCF 업무 개발에 참여할 예정인 사람
- 화면과 테이블은 알지만 거래설계와 프로그램 구조가 낯선 사람
- 운영 로그에서 ServiceId나 TraceId를 처음 접한 사람

### 2.4 선행조건

아래 내용을 완벽하게 알 필요는 없습니다.

| 항목 | 필요한 수준 |
| --- | --- |
| Java | 클래스와 메서드가 무엇인지 이해 |
| Spring | @Component, @Service를 본 경험 |
| JSON | { "name": "홍길동" } 형태 이해 |
| SQL | 기본적인 SELECT, INSERT, UPDATE 이해 |
| HTTP | 브라우저가 서버에 요청한다는 개념 |
| DB | 테이블과 컬럼의 개념 |

모르는 용어가 나오더라도 일단 전체 흐름을 먼저 이해하십시오. 세부 기술은 뒤에서 다시 설명합니다.

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| 거래 | 사용자가 요청한 하나의 업무 처리 |
| TCF | 모든 온라인 거래를 공통 규칙으로 실행하는 엔진 |
| STF | 업무 실행 전에 요청을 검사하는 전처리 영역 |
| ETF | 업무 실행 후 응답과 로그를 마무리하는 후처리 영역 |
| ServiceId | 어떤 업무 기능을 실행할지 나타내는 이름 |
| Handler | ServiceId를 보고 해당 업무를 시작하는 프로그램 |
| Facade | 하나의 업무 흐름을 조립하고 트랜잭션을 관리하는 프로그램 |
| Service | 실제 업무 처리 순서를 수행하는 프로그램 |
| Rule | 업무 조건과 규칙을 검사하는 프로그램 |
| DAO | DB 접근을 담당하는 프로그램 |
| Mapper | MyBatis SQL을 실행하는 프로그램 |
| 업무 WAR | 업무별로 배포되는 웹 애플리케이션 |
| Gateway | 요청을 적절한 업무 WAR로 전달하는 관문 |
| Timeout | 정해진 시간 안에 끝나지 않은 거래를 중단하는 정책 |
| GUID | 여러 시스템을 거치는 거래를 추적하는 번호 |
| TraceId | 서버 내부 로그를 연결하는 추적 번호 |

## 제1장. TCF가 뭐예요?

### 1.1 개발자가 처음 만나는 두 가지 방식

Spring Boot를 처음 배우면 보통 다음과 같이 기능별 URL을 만듭니다.

```
GET  /customers/1001
POST /customers
PUT  /customers/1001
GET  /campaigns
```

고객을 조회하는 주소와 캠페인을 조회하는 주소가 서로 다릅니다.

```
URL이 다르면
실행되는 Controller도 다르다.
```

이 방식은 일반적인 REST API에서 널리 사용됩니다.

NSIGHT TCF의 업무 온라인 거래는 조금 다릅니다.

```
POST /sv/online
POST /ic/online
POST /cm/online
```

같은 업무 WAR 안에서는 온라인 거래가 공통 주소로 들어옵니다. 어떤 기능을 실행할지는 URL이 아니라 요청 JSON의 serviceId로 구분합니다.

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary"
  },
  "body": {
    "customerNo": "CUST0001"
  }
}
```

여기에서 중요한 값은 다음 두 개입니다.

| 항목 | 값 | 의미 |
| --- | --- | --- |
| businessCode | SV | 어느 업무 영역인가 |
| serviceId | SV.Customer.selectSummary | 어떤 기능을 실행할 것인가 |

### 1.2 안내 데스크 비유

일반 REST 방식과 TCF 방식을 건물의 민원 창구에 비유해 보겠습니다.

#### 일반 REST 방식

```
고객 조회 창구
고객 등록 창구
고객 수정 창구
캠페인 조회 창구
캠페인 등록 창구
```

기능마다 서로 다른 창구가 있습니다.

#### TCF 방식

```
공통 안내 데스크
      ↓
접수번호 확인
      ↓
담당 업무 창구로 전달
```

공통 안내 데스크가 요청을 받은 뒤 접수번호를 확인합니다.

TCF에서 이 접수번호에 해당하는 것이 ServiceId입니다.

```
공통 안내 데스크 = OnlineTransactionController와 TCF
접수번호          = ServiceId
담당 업무 창구     = Handler
```

### 1.3 TCF의 정의

TCF는 Transaction Control Framework의 약자입니다.

쉽게 말하면 다음과 같습니다.

TCF는 온라인 거래를 같은 방식으로 받고, 검사하고, 실행하고, 응답하고, 기록하는 공통 처리 엔진이다.

TCF가 처리하는 대표적인 기능은 다음과 같습니다.

| 기능 | 설명 |
| --- | --- |
| 표준 요청 검증 | 필수 Header와 Body 형식 확인 |
| 인증 확인 | 로그인한 사용자인지 확인 |
| 권한 확인 | 해당 기능을 사용할 수 있는지 확인 |
| 거래통제 | 현재 실행이 허용된 거래인지 확인 |
| Timeout | 지정된 시간 안에 끝나는지 통제 |
| 중복 요청 방지 | 같은 요청이 반복 처리되지 않도록 확인 |
| Handler 연결 | ServiceId에 맞는 업무 프로그램 선택 |
| 오류 표준화 | 오류를 표준 코드와 메시지로 변환 |
| 거래로그 | 거래 시작과 종료 결과 기록 |
| 감사로그 | 중요 조회나 변경 행위 기록 |
| 추적정보 | GUID와 TraceId 생성 및 전달 |

### 1.4 왜 TCF가 필요한가요?

#### TCF가 없는 경우

개발자마다 다음 기능을 직접 만들 수 있습니다.

```
@PostMapping("/customer")
public ResponseEntity<?> getCustomer(...) {
    // 로그인 확인
    // 사용자 권한 확인
    // 거래 가능 여부 확인
    // 시작 로그 기록
    // Timeout 처리
    // 고객 조회
    // 오류 처리
    // 종료 로그 기록
    // 응답 JSON 만들기
}
```

고객 조회뿐 아니라 캠페인 조회, 상품 조회, 메시지 전송에도 비슷한 코드가 반복됩니다.

이 구조에는 문제가 있습니다.

| 문제 | 설명 |
| --- | --- |
| 중복 코드 | 인증·로그·오류 코드가 프로그램마다 반복됨 |
| 처리 불일치 | 개발자마다 다른 방식으로 구현함 |
| 누락 위험 | 어떤 거래는 Timeout이나 로그가 빠질 수 있음 |
| 운영 어려움 | 오류 응답과 로그 형식이 제각각임 |
| 변경 어려움 | 보안 정책 변경 시 모든 Controller 수정 필요 |
| 장애 추적 어려움 | 공통 GUID와 ServiceId가 없을 수 있음 |

#### TCF가 있는 경우

```
공통 처리
- 인증
- 권한
- 거래통제
- Timeout
- 중복 요청
- 거래로그
- 오류 응답

업무 처리
- 고객 조회
- 캠페인 등록
- 상품 추천
- 메시지 전송
```

개발자는 업무 기능에 집중할 수 있습니다.

### 1.5 TCF와 업무 개발자의 책임

```
[TCF의 책임]
요청을 안전하고 일관되게 실행하는 방법

[업무 개발자의 책임]
사용자가 요청한 실제 업무 기능
```

| 구분 | TCF | 업무 개발자 |
| --- | --- | --- |
| 표준 Header 검사 | 담당 | 사용하지 않음 |
| GUID 생성 | 담당 | 로그에서 활용 |
| ServiceId로 프로그램 찾기 | 담당 | ServiceId와 Handler 등록 |
| 인증 문맥 확인 | 담당 | 업무 상세 권한 검토 |
| 거래통제 | 담당 | 운영정책 요청 |
| Timeout | 담당 | 적정 시간 산정 |
| 고객정보 조회 | 해당 없음 | 담당 |
| 캠페인 등록 | 해당 없음 | 담당 |
| SQL 작성 | 해당 없음 | 담당 |
| 업무 규칙 판단 | 해당 없음 | 담당 |
| 표준 응답 조립 | 담당 | Body 결과 반환 |

### 1.6 온라인 거래 한 건의 모습

고객 요약 조회를 예로 들어 보겠습니다.

```
화면 기능: 고객 요약 조회
업무코드: SV
ServiceId: SV.Customer.selectSummary
거래코드: SV-INQ-0001
```

요청은 다음처럼 들어옵니다.

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "user01",
    "branchId": "001234"
  },
  "body": {
    "customerNo": "CUST0001"
  }
}
```

TCF는 요청을 보고 다음을 확인합니다.

```
1. businessCode가 있는가?
2. serviceId가 있는가?
3. 로그인한 사용자인가?
4. 고객 조회 권한이 있는가?
5. 해당 거래가 현재 허용되어 있는가?
6. Timeout 정책이 등록되어 있는가?
7. 같은 요청이 이미 처리 중인가?
8. serviceId를 처리할 Handler가 있는가?
```

검사를 통과하면 업무 프로그램이 실행됩니다.

### 1.7 TCF를 통과하지 않는 프로그램이 위험한 이유

다음과 같은 별도 Controller를 만들었다고 가정하겠습니다.

```
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @GetMapping("/{customerNo}")
    public Customer getCustomer(@PathVariable String customerNo) {
        return customerMapper.selectCustomer(customerNo);
    }
}
```

기능은 동작할 수 있습니다. 하지만 다음 공통 처리를 우회할 가능성이 있습니다.

```
STF 요청 검증
인증 문맥 검증
권한 검증
거래통제
Timeout
중복 요청 방지
거래로그
감사로그
ETF 표준 응답
```

따라서 NSIGHT의 업무 온라인 거래에서는 임의의 업무 Controller를 추가하기보다 공통 Online Endpoint와 ServiceId를 사용해야 합니다.

다만 다음과 같은 기술·운영 목적의 Endpoint는 별도 기준으로 존재할 수 있습니다.

- Health Check
- 내부 진단 API
- JWKS 공개키 조회
- 파일 전송 전용 API
- 운영관리 전용 API
중요한 것은 일반 업무 온라인 거래를 임의의 Controller로 우회하지 않는 것입니다.

### 1.8 초보자가 기억할 세 가지

처음부터 모든 규칙을 외우지 않아도 됩니다.

다음 세 가지만 먼저 기억하십시오.

```
첫째,
업무 온라인 요청은 표준 JSON으로 보낸다.

둘째,
실행할 기능은 ServiceId로 구분한다.

셋째,
업무 코드는 Handler 아래 6계층에 작성한다.
```

### 1.9 정상 예시와 금지 예시

#### 정상 예시

```
POST /sv/online
  ↓
serviceId = SV.Customer.selectSummary
  ↓
TCF
  ↓
SvCustomerHandler
  ↓
SvCustomerFacade
  ↓
SvCustomerService
  ↓
SvCustomerRule
  ↓
SvCustomerDao
  ↓
SvCustomerMapper
```

#### 금지 예시

```
POST /api/customer
  ↓
CustomerController
  ↓
CustomerMapper
```

금지 사유는 단순히 코드 스타일이 나쁘기 때문이 아닙니다.

```
TCF 공통 통제와
거래 추적 구조를 우회하기 때문이다.
```

### 1.10 제1장 점검문제

#### 문제 1

TCF에서 어떤 업무 기능을 실행할지 구분하는 값은 무엇인가요?

정답:

```
ServiceId
```

#### 문제 2

다음 중 업무 개발자가 직접 구현해야 하는 것은 무엇인가요?

```
① GUID 생성
② 거래로그 공통 종료
③ 고객 조회 업무 로직
④ 표준 오류 응답 조립
```

정답:

```
③ 고객 조회 업무 로직
```

#### 문제 3

Handler가 Mapper를 직접 호출하면 안 되는 이유는 무엇인가요?

정답:

```
업무 계층의 책임과 트랜잭션 구조를 우회하고,
변경·테스트·운영 추적이 어려워지기 때문이다.
```

### 제1장 요약

```
TCF는 모든 온라인 거래를
같은 규칙으로 실행하는 공통 엔진이다.

업무 기능은 URL이 아니라
ServiceId로 구분한다.

개발자는 업무 로직을 만들고,
TCF는 인증·통제·Timeout·로그·오류 응답을 담당한다.
```

## 제2장. 시스템 그림 한 장

### 2.1 전체 구조부터 보기

NSIGHT 시스템을 처음 볼 때 소스코드부터 열지 마십시오.

먼저 사용자 요청이 어디를 거쳐 가는지 큰 그림을 보아야 합니다.

```
[사용자 / 전용브라우저 / BI포털]
                 │
                 ▼
        [GSLB / L4 / Apache]
                 │
                 ▼
           [tcf-gateway]
                 │
                 ▼
        [SV·IC·CM 등 업무 WAR]
                 │
                 ▼
             [tcf-web]
      OnlineTransactionController
                 │
                 ▼
             [tcf-core]
       TCF → STF → Dispatcher
                 │
                 ▼
        [업무 프로그램 6계층]
 Handler → Facade → Service → Rule → DAO → Mapper
                 │
                 ▼
       [RDW / ADW / 업무 DB]
                 │
                 ▼
       [ETF → 표준 응답 → 화면]
```

운영관리 기능은 옆에서 전체 거래를 지원합니다.

```
[tcf-om]
- Service Catalog
- 거래통제
- Timeout 정책
- 사용자·권한
- 오류코드
- 공통코드
- 거래로그
- 감사로그
- 배포·운영정보
```

### 2.2 사용자와 UI

사용자는 다음과 같은 채널을 통해 시스템을 사용합니다.

- WEBTOPSUITE 전용브라우저
- BI포털 React 화면
- 업무 전용 UI
- 내부 운영관리 화면
- 외부 연계 시스템
화면의 역할은 다음과 같습니다.

```
사용자 입력 받기
→ 표준 요청 JSON 만들기
→ 서버에 요청 전송
→ 결과 JSON 받기
→ 화면에 표시
```

화면이 업무 판단의 최종 책임을 가져서는 안 됩니다.

예를 들어 화면에서 고객번호가 비어 있는지 먼저 확인할 수는 있습니다. 하지만 서버에서도 반드시 다시 확인해야 합니다.

```
화면 검증
= 사용 편의

서버 Rule 검증
= 실제 업무 통제
```

### 2.3 GSLB, L4, Apache

초보자는 이 세 가지를 처음부터 세부적으로 구분하기 어렵습니다.

처음에는 다음처럼 이해하면 됩니다.

```
GSLB
= 여러 센터 중 어디로 보낼지 결정

L4
= 여러 서버 중 어느 서버로 보낼지 결정

Apache
= URL 경로와 서버 연결을 중계
```

비유하면 다음과 같습니다.

| 구성요소 | 비유 |
| --- | --- |
| GSLB | 어느 지역 물류센터로 보낼지 결정 |
| L4 | 센터 안의 어느 작업대로 보낼지 결정 |
| Apache | 건물 입구에서 내부 위치 안내 |
| Gateway | 업무코드를 보고 담당 부서로 전달 |

초보 업무 개발자가 이 장비들의 상세 설정을 직접 변경하는 경우는 많지 않습니다. 하지만 장애가 발생했을 때 요청이 업무 WAR까지 도착했는지 판단하려면 전체 위치는 알아야 합니다.

### 2.4 Gateway

Gateway는 시스템의 공통 관문입니다.

```
클라이언트
  ↓
Gateway
  ├─ 인증 확인
  ├─ 업무코드 확인
  ├─ 라우팅 대상 확인
  ├─ 필요한 Header 보정
  └─ 업무 WAR로 전달
```

예를 들어 요청의 업무코드가 SV라면 Gateway는 SV 업무 WAR로 요청을 보냅니다.

```
businessCode = SV
→ sv-service

businessCode = IC
→ ic-service

businessCode = OM
→ tcf-om
```

#### Gateway가 있으면 좋은 점

| 장점 | 설명 |
| --- | --- |
| 접속 주소 통일 | 클라이언트가 업무 WAR별 주소를 몰라도 됨 |
| 인증 집중 | 외부 JWT를 1차 검증 가능 |
| 라우팅 통제 | 업무코드별 전달 대상을 중앙 관리 |
| 접근 통제 | 업무 WAR 직접 접근 차단 가능 |
| 로그 통합 | 외부 요청 흐름을 공통 기록 |

#### Gateway가 없는 경우

Gateway가 없더라도 TCF 거래를 처리할 수는 있습니다.

```
클라이언트
→ 업무 WAR
→ 공통 JWT Filter
→ OnlineTransactionController
→ TCF
```

이 경우 각 업무 WAR 진입부에서 토큰을 검증해야 합니다.

중요한 기준은 다음입니다.

```
Gateway가 없다고 인증이 없어지는 것이 아니다.
인증 검증 위치가 업무 WAR 앞으로 이동하는 것이다.
```

### 2.5 업무 WAR

WAR는 Java 웹 애플리케이션을 배포하는 파일입니다.

예:

```
sv.war
ic.war
pc.war
cm.war
mg.war
tcf-om.war
```

각 WAR는 특정 업무 영역을 담당합니다.

| 업무코드 예 | 업무 영역 예 |
| --- | --- |
| SV | 고객 Single View |
| IC | 통합 고객 |
| PC | 개인 고객 |
| PD | 상품 |
| CM | 캠페인 |
| MG | 관리·메시지 |
| OM | 운영관리 |

실제 업무코드와 배포 WAR의 범위는 프로젝트 단계에 따라 달라질 수 있습니다.

#### 업무를 WAR로 나누는 이유

```
업무 책임 분리
배포 단위 분리
장애 영향 분리
팀별 소유권 구분
설정과 DB Pool 구분
```

하지만 하나의 Tomcat에 여러 WAR를 배포하면 일부 자원은 공유됩니다.

```
공유되는 것
- JVM
- Heap
- GC
- Tomcat Connector Thread
- Tomcat 프로세스 장애

WAR별로 분리되는 것
- Spring ApplicationContext
- 업무 Bean
- 일반적인 HikariCP
- 업무 설정
- 업무 로그
```

따라서 WAR가 나뉘었다고 해서 실행 환경이 완전히 독립된 것은 아닙니다.

### 2.6 tcf-web

tcf-web은 여러 업무 WAR가 공통으로 사용하는 웹 진입 기능을 제공합니다.

대표적인 구성요소는 다음과 같습니다.

```
OnlineTransactionController
공통 요청 변환
공통 예외 연결
보안 Filter
웹 관련 설정
```

업무 개발자는 일반적으로 OnlineTransactionController를 새로 만들지 않습니다.

각 업무 WAR가 공통 tcf-web 모듈을 사용하고, 업무 기능은 Handler 이하에 구현합니다.

### 2.7 tcf-core

tcf-core는 TCF 실행 엔진의 중심입니다.

```
tcf-core
├─ TCF
├─ STF
├─ ETF
├─ TransactionDispatcher
├─ TransactionContext
├─ Timeout 처리
├─ 거래통제
├─ 중복요청 처리
├─ 오류코드
└─ 거래로그
```

tcf-web이 요청을 받는 입구라면 tcf-core는 실제 거래를 통제하는 엔진입니다.

```
tcf-web
= 요청을 받는 문

tcf-core
= 요청을 검사하고 실행하는 기계
```

### 2.8 업무 프로그램

TCF 검사를 통과하면 업무 프로그램이 실행됩니다.

```
Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
```

업무 개발자가 가장 많이 작성하는 영역입니다.

### 2.9 DB

Mapper가 SQL을 실행하면 DB에서 데이터를 조회하거나 변경합니다.

```
Mapper XML
  ↓
SELECT / INSERT / UPDATE / DELETE
  ↓
Table / View
```

NSIGHT에서는 데이터 성격에 따라 여러 DB가 사용될 수 있습니다.

- RDW
- ADW
- 업무 DB
- OM DB
- 운영 로그 저장소
초보 개발자는 자신이 작성하는 Mapper가 어느 DB와 어떤 테이블을 사용하는지 반드시 알아야 합니다.

### 2.10 tcf-om

tcf-om은 단순 관리자 화면이 아닙니다.

TCF가 거래를 실행할 때 참고하는 운영 기준정보를 관리합니다.

```
ServiceId가 등록되어 있는가?
현재 거래가 허용되어 있는가?
Timeout은 몇 초인가?
사용자에게 권한이 있는가?
오류코드와 메시지는 무엇인가?
```

이 정보가 OM에서 관리됩니다.

신규 거래를 만들 때는 소스코드만 작성해서는 부족합니다.

```
소스코드
+ OM Service Catalog
+ 거래통제 정책
+ Timeout 정책
```

이 세 가지가 함께 준비되어야 운영 가능한 거래가 됩니다.

### 2.11 시스템 전체 흐름 예시

사용자가 고객번호 CUST0001을 조회한다고 가정하겠습니다.

```
1. 사용자가 고객 조회 버튼 클릭

2. 화면이 표준 JSON 생성
   serviceId = SV.Customer.selectSummary

3. Gateway가 요청 수신

4. businessCode = SV 확인

5. SV 업무 WAR로 전달

6. OnlineTransactionController가 요청 수신

7. TCF.process() 실행

8. STF가 인증·권한·거래통제·Timeout 확인

9. Dispatcher가 SvCustomerHandler 선택

10. Handler 이하 업무 프로그램 실행

11. Mapper가 고객정보 SQL 실행

12. ETF가 표준 응답 생성

13. 화면이 고객정보 표시
```

### 2.12 장애가 발생한 위치를 구분하는 방법

| 증상 | 먼저 확인할 위치 |
| --- | --- |
| 서버 주소에 접속되지 않음 | L4, Apache, Gateway |
| 401 인증 오류 | JWT, Gateway, 업무 WAR Filter |
| ServiceId 없음 | 요청 Header, Dispatcher |
| 권한 없음 | STF 권한검증, OM 권한정보 |
| 거래 중지 | OM 거래통제 |
| Timeout | TCF Timeout, SQL, 외부연계 |
| DB 오류 | DAO, Mapper, SQL, DB |
| 응답 형식 오류 | ETF 또는 TCF 우회 여부 |
| 특정 WAR 전체 지연 | Tomcat Thread, JVM, DB Pool |

초보 개발자가 모든 장애를 해결할 필요는 없습니다. 하지만 어느 영역의 문제인지 구분할 수 있어야 담당자에게 정확하게 전달할 수 있습니다.

### 제2장 요약

```
사용자 요청은
Gateway → 업무 WAR → tcf-web → tcf-core → 업무 프로그램 → DB
순서로 이동한다.

Gateway는 업무를 찾아 전달하고,
TCF는 거래를 통제하며,
업무 WAR는 실제 업무 기능을 수행한다.

OM은 ServiceId·거래통제·Timeout 같은
운영 기준정보를 관리한다.
```

## 제3장. 요청이 지나가는 길

### 3.1 전체 처리 흐름

TCF의 대표 처리 흐름은 다음과 같습니다.

```
OnlineTransactionController
  ↓
TCF.process()
  ↓
STF.preProcess()
  ↓
OnlineTransactionTimeoutExecutor
  ↓
TransactionDispatcher.dispatch()
  ↓
TransactionHandler.handle()
  ↓
Facade → Service → Rule → DAO → Mapper
  ↓
ETF.success
또는 ETF.businessFail
또는 ETF.systemError
  ↓
StandardResponse
```

한 문장으로 줄이면 다음과 같습니다.

```
요청 수신
→ 사전 검사
→ 시간 제한 안에서 업무 실행
→ 결과와 로그 마무리
```

TCF는 모든 거래가 이 순서를 따르도록 강제합니다. STF 검증에서 실패하면 Handler와 DB 업무처리를 실행하지 않는 것이 기본 원칙입니다.

### 3.2 1단계: 화면이 표준 요청을 보낸다

요청은 크게 header와 body로 구성됩니다.

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "user01",
    "branchId": "001234"
  },
  "body": {
    "customerNo": "CUST0001"
  }
}
```

#### Header

Header는 거래를 처리하기 위한 공통 정보입니다.

| 항목 | 설명 |
| --- | --- |
| businessCode | 업무 영역 |
| serviceId | 실행할 업무 기능 |
| transactionCode | 거래통제·감사·통계용 코드 |
| processingType | 조회·등록·변경 등의 유형 |
| channelId | 요청 채널 |
| userId | 사용자 식별자 |
| branchId | 지점 식별자 |
| guid | 시스템 간 거래 추적번호 |
| traceId | 서버 내부 추적번호 |

#### Body

Body는 실제 업무 데이터입니다.

```
{
  "customerNo": "CUST0001"
}
```

다른 거래라면 Body 내용이 달라집니다.

```
{
  "campaignId": "CMP0001",
  "campaignName": "우수고객 캠페인"
}
```

### 3.3 Header를 무조건 신뢰하면 안 되는 이유

화면이 보낸 userId를 서버가 그대로 신뢰하면 안 됩니다.

사용자가 브라우저 개발자 도구로 값을 바꿀 수 있기 때문입니다.

```
화면 Header userId
= 요청자가 주장하는 값

JWT 또는 인증 Context userId
= 서버가 검증한 값
```

STF는 두 정보가 일치하는지 확인해야 합니다.

```
JWT userId = user01
Header userId = admin

→ 위변조 가능성
→ 거래 차단
```

### 3.4 2단계: OnlineTransactionController

OnlineTransactionController는 공통 진입점입니다.

개념적으로 다음 역할만 수행합니다.

```
@PostMapping("/{businessCode}/online")
public StandardResponse online(
        @PathVariable String businessCode,
        @RequestBody StandardRequest request) {

    return tcf.process(request, businessCode);
}
```

실제 구현은 더 많은 공통 정보를 처리할 수 있지만 핵심은 단순합니다.

```
JSON 요청을 받는다.
→ TCF.process()에 전달한다.
```

Controller에서 직접 고객조회나 SQL 실행을 하지 않습니다.

### 3.5 3단계: TCF.process()

TCF.process()는 전체 거래의 진행 관리자입니다.

```
STF 실행
→ Timeout 안에서 업무 실행
→ 결과에 따라 ETF 실행
→ 마지막에 Context 정리
```

개념적인 구조는 다음과 같습니다.

```
public StandardResponse process(StandardRequest request) {
    TransactionContext context = null;

    try {
        context = stf.preProcess(request);

        Object body = timeoutExecutor.execute(
            () -> dispatcher.dispatch(request, context)
        );

        return etf.success(request, body, context);

    } catch (BusinessException e) {
        return etf.businessFail(request, e, context);

    } catch (Exception e) {
        return etf.systemError(request, e, context);

    } finally {
        contextHolder.clear();
        mdc.clear();
    }
}
```

이 코드는 이해를 위한 단순 예시입니다.

### 3.6 4단계: STF 전처리

STF는 업무 프로그램이 실행되기 전에 요청을 검사하고 거래를 준비합니다.

```
STF
= Standard Transaction Framework
= 표준 거래 전처리
```

STF가 수행하는 대표 순서는 다음과 같습니다.

```
1. 표준 Header 검증
2. GUID·TraceId 생성
3. TransactionContext 생성
4. 세션 검증
5. 인증 Context 검증
6. 권한 검증
7. 거래통제
8. Timeout 정책 결정
9. 중복 요청 검사
10. 거래로그 시작
```

순서는 중요합니다.

### 3.7 Header 검증

다음과 같은 항목을 확인합니다.

```
businessCode가 있는가?
serviceId가 있는가?
transactionCode가 있는가?
channelId 형식이 맞는가?
URL 업무코드와 Header 업무코드가 같은가?
```

예:

```
요청 URL: /sv/online
Header businessCode: OM

→ 업무코드 불일치
→ Handler 실행 전 오류
```

### 3.8 GUID와 TraceId

#### GUID

GUID는 시스템 간 거래를 연결하는 전역 식별자입니다.

```
화면
→ Gateway
→ SV
→ IC
→ 외부 시스템
```

여러 시스템을 거쳐도 같은 GUID를 전달하면 하나의 거래로 추적할 수 있습니다.

#### TraceId

TraceId는 애플리케이션 내부 로그를 연결하는 값입니다.

```
2026-07-17 INFO traceId=abc123 TCF started
2026-07-17 INFO traceId=abc123 Handler selected
2026-07-17 INFO traceId=abc123 SQL executed
2026-07-17 INFO traceId=abc123 TCF success
```

장애가 발생했을 때 TraceId로 검색하면 관련 로그를 함께 찾을 수 있습니다.

#### ServiceId와의 차이

| 식별자 | 의미 |
| --- | --- |
| ServiceId | 어떤 기능인가 |
| GUID | 어느 거래 요청인가 |
| TraceId | 어느 로그 흐름인가 |
| 거래코드 | 운영·감사상 어떤 거래인가 |

### 3.9 TransactionContext

TransactionContext는 거래 처리 동안 필요한 공통 정보를 보관합니다.

예:

```
businessCode
serviceId
transactionCode
userId
branchId
channelId
guid
traceId
startTime
timeout
```

업무 프로그램이 매번 Header를 직접 분석하지 않고 검증된 Context를 사용할 수 있게 합니다.

```
원본 요청값을 계속 신뢰하지 않고,
STF가 검증한 거래 문맥을 사용한다.
```

### 3.10 인증과 권한

인증과 권한은 서로 다릅니다.

```
인증
= 이 사용자가 누구인가?

권한
= 이 사용자가 이 기능을 실행해도 되는가?
```

예:

```
user01이 정상 로그인함
→ 인증 성공

user01이 고객조회 권한을 가짐
→ 권한 성공

user01이 관리자 설정 변경 권한은 없음
→ 관리자 변경 거래는 권한 실패
```

### 3.11 거래통제

운영자는 특정 거래를 일시적으로 중지할 수 있어야 합니다.

예:

```
SV.Customer.selectSummary
상태 = ALLOW

SV.Customer.downloadAll
상태 = DENY
사유 = 개인정보 점검
```

코드는 존재하지만 OM 정책에서 차단되면 실행되지 않습니다.

이 기능을 거래통제라고 합니다.

### 3.12 Timeout 정책

모든 거래가 끝날 때까지 무한정 기다릴 수는 없습니다.

```
고객 단건 조회: 3초
고객 목록 조회: 5초
대용량 다운로드: 별도 비동기 처리
```

Timeout이 없으면 느린 SQL이나 외부 시스템이 Tomcat Thread와 DB Connection을 오래 점유할 수 있습니다.

```
느린 거래 증가
→ Thread 점유
→ DB Pool 점유
→ 다른 정상 거래도 대기
→ 시스템 전체 지연
```

Timeout은 단순히 사용자에게 오류를 빨리 보여주는 기능이 아닙니다.

```
시스템 전체를 보호하는 안전장치
```

### 3.13 중복 요청 방지

사용자가 등록 버튼을 여러 번 클릭할 수 있습니다.

```
첫 번째 클릭 → 정상 등록
두 번째 클릭 → 같은 데이터 중복 등록
세 번째 클릭 → 다시 중복 등록
```

조회 거래는 중복되어도 문제가 작을 수 있지만 송금, 등록, 승인, 메시지 발송 같은 거래는 중복 처리되면 문제가 됩니다.

TCF는 요청 식별값을 이용해 같은 요청이 이미 처리 중인지 확인할 수 있습니다.

```
요청 시작 → PROCESSING
정상 종료 → SUCCESS
오류 종료 → FAIL
```

### 3.14 거래로그 시작

업무가 실행되기 전에 거래로그를 시작합니다.

```
GUID: G202607170001
ServiceId: SV.Customer.selectSummary
상태: PROCESSING
시작시간: 09:00:00.100
```

업무가 끝난 후 ETF가 종료 결과를 기록합니다.

```
상태: SUCCESS
종료시간: 09:00:00.350
처리시간: 250ms
```

오류라면 다음처럼 기록할 수 있습니다.

```
상태: FAIL
오류코드: SV-CUST-0001
처리시간: 80ms
```

### 3.15 5단계: TimeoutExecutor

STF가 Timeout 값을 결정하면 실제 업무 실행은 제한시간 안에서 수행됩니다.

```
Timeout = 3초

Handler 업무가 1초에 종료
→ 정상

Handler 업무가 3초 초과
→ Timeout 처리
```

Timeout이 발생하면 업무 결과를 성공으로 반환해서는 안 됩니다.

또한 DB 작업이 이미 진행되었다면 트랜잭션 Rollback 가능 여부도 확인해야 합니다.

### 3.16 6단계: Dispatcher

Dispatcher는 ServiceId를 보고 Handler를 찾습니다.

```
serviceId
→ Handler Registry
→ 담당 Handler
```

예:

```
SV.Customer.selectSummary
→ SvCustomerHandler

SV.Customer.selectDetail
→ SvCustomerHandler

CM.Campaign.create
→ CmCampaignHandler
```

최신 구조에서는 하나의 도메인 Handler가 여러 ServiceId를 등록할 수 있습니다.

```
@Override
public Set<String> serviceIds() {
    return Set.of(
        "SV.Customer.selectSummary",
        "SV.Customer.selectDetail"
    );
}
```

Handler 내부에서는 ServiceId에 따라 Facade 메서드를 선택합니다.

```
return switch (serviceId) {
    case "SV.Customer.selectSummary" ->
        customerFacade.selectSummary(request, context);

    case "SV.Customer.selectDetail" ->
        customerFacade.selectDetail(request, context);

    default ->
        throw new ServiceNotFoundException(serviceId);
};
```

#### ServiceId 중복

두 Handler가 같은 ServiceId를 등록하면 어느 Handler를 실행해야 할지 결정할 수 없습니다.

```
Handler A
→ SV.Customer.selectSummary

Handler B
→ SV.Customer.selectSummary
```

이 경우 서버 기동 단계에서 오류를 발생시키는 것이 안전합니다.

### 3.17 7단계: Handler와 업무 처리

Dispatcher가 Handler를 찾으면 여기서부터 업무 개발자가 작성한 코드가 시작됩니다.

```
Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
```

Handler는 요청을 적절한 DTO로 변환하고 Facade를 호출합니다.

```
public Object handle(
        StandardRequest request,
        TransactionContext context) {

    CustomerSummaryRequest input =
        request.bodyAs(CustomerSummaryRequest.class);

    return customerFacade.selectSummary(input, context);
}
```

Handler에서 SQL을 실행하거나 표준 응답을 직접 만들지 않습니다.

### 3.18 8단계: ETF 후처리

ETF는 업무 처리가 끝난 뒤 결과를 표준화합니다.

```
ETF
= End Transaction Framework
= 거래 종료 처리
```

ETF는 처리 결과를 크게 세 가지로 구분합니다.

#### 정상

```
ETF.success()
```

#### 업무 오류

```
ETF.businessFail()
```

예:

- 고객번호가 없음
- 조회 권한 없음
- 캠페인 기간이 유효하지 않음
- 상품이 판매 중지 상태임

#### 시스템 오류

```
ETF.systemError()
```

예:

- DB 연결 실패
- NullPointerException
- 외부 시스템 접속 실패
- 설정 오류

### 3.19 표준 응답

정상 응답 예시는 다음과 같습니다.

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "guid": "G202607170001",
    "traceId": "TABC123"
  },
  "result": {
    "status": "SUCCESS",
    "code": "S0000",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST0001",
    "customerName": "홍길동",
    "grade": "VIP"
  }
}
```

업무 오류 예시는 다음과 같습니다.

```
{
  "header": {
    "serviceId": "SV.Customer.selectSummary",
    "guid": "G202607170002",
    "traceId": "TABC124"
  },
  "result": {
    "status": "FAIL",
    "code": "SV-CUST-0001",
    "message": "고객을 찾을 수 없습니다."
  },
  "body": null
}
```

### 3.20 정상·오류·Timeout 흐름 비교

| 구분 | 업무 실행 | ETF 경로 | 응답 |
| --- | --- | --- | --- |
| 정상 | 실행 | success | 성공 코드와 Body |
| Header 오류 | 미실행 | businessFail | 요청 오류 |
| 권한 오류 | 미실행 | businessFail | 권한 오류 |
| 거래통제 | 미실행 | businessFail | 거래 중지 |
| 업무 규칙 오류 | 일부 실행 | businessFail | 업무 오류 |
| Timeout | 중단 시도 | Timeout 오류 경로 | 시간 초과 |
| 시스템 예외 | 실행 중 실패 | systemError | 시스템 오류 |

### 3.21 마지막 정리 작업

거래가 성공하든 실패하든 마지막에 정리해야 하는 정보가 있습니다.

```
TransactionContext
인증 Context
Timeout Context
MDC 로그 정보
ThreadLocal
```

이 정보를 정리하지 않으면 다음 요청이 이전 요청의 정보를 잘못 사용할 수 있습니다.

따라서 finally 영역에서 반드시 정리합니다.

### 3.22 요청 흐름을 외우는 방법

다음 문장만 기억하십시오.

```
Controller가 받고,
TCF가 관리하고,
STF가 검사하고,
Dispatcher가 찾고,
Handler가 시작하고,
6계층이 업무를 처리하고,
ETF가 끝낸다.
```

### 제3장 요약

```
표준 JSON 요청
→ OnlineTransactionController
→ TCF.process()
→ STF 사전검사
→ TimeoutExecutor
→ Dispatcher
→ Handler와 6계층
→ ETF 표준 응답
```

STF에서 실패하면 업무 프로그램은 실행하지 않습니다.

ServiceId는 Dispatcher가 Handler를 찾는 핵심 식별자입니다.

GUID와 TraceId는 장애 발생 시 전체 거래와 로그를 추적하는 기준입니다.

## 제4장. 6계층, 역할만 기억하기

### 4.1 왜 프로그램을 여러 계층으로 나누나요?

고객 조회 기능을 한 클래스에 모두 작성할 수도 있습니다.

```
public CustomerResponse selectCustomer(String customerNo) {
    if (customerNo == null) {
        throw new RuntimeException();
    }

    // SQL 실행
    // 고객등급 계산
    // 권한 판단
    // 응답 조립
    // 로그 기록
}
```

처음에는 파일 하나라서 쉬워 보입니다.

하지만 기능이 커지면 다음 문제가 생깁니다.

- 검증과 SQL이 섞임
- 업무 규칙을 재사용하기 어려움
- 단위 테스트가 어려움
- 트랜잭션 위치가 불명확함
- DB 변경 영향이 커짐
- 개발자마다 코드 구조가 달라짐
- 장애 발생 시 어느 책임의 문제인지 구분하기 어려움
따라서 NSIGHT는 프로그램을 책임에 따라 나눕니다.

```
Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
```

### 4.2 식당 비유

6계층을 식당에 비유해 보겠습니다.

| 계층 | 식당 비유 | 역할 |
| --- | --- | --- |
| Handler | 주문 접수 직원 | 어떤 주문인지 확인 |
| Facade | 주방 총괄 | 주문 전체 순서와 완료 책임 |
| Service | 조리 담당 | 실제 조리 절차 수행 |
| Rule | 조리 기준서 | 재료·조건·규칙 확인 |
| DAO | 창고 담당자 | 필요한 재료 요청 |
| Mapper | 창고 출고 시스템 | 실제 재료 조회·출고 |

주문 접수 직원이 직접 창고에 들어가 재료를 가져오면 역할이 무너집니다.

```
Handler가 Mapper를 직접 호출
= 주문 접수 직원이 창고 출고까지 직접 처리
```

기능은 동작할 수 있지만 조직 전체가 복잡해집니다.

### 4.3 전체 계층 구조

```
TCF
  ↓
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  ├─ DAO
  │    ↓
  │  Mapper
  │    ↓
  │  SQL / DB
  └─ 외부 Client
```

Rule은 항상 Service 뒤에 직선으로만 호출되는 것은 아닙니다. Service가 필요한 시점에 Rule을 호출하고, 검증이 끝나면 DAO나 외부 Client를 호출할 수 있습니다.

따라서 논리적인 관계는 다음과 같습니다.

```
Handler → Facade → Service
                     ├→ Rule
                     ├→ DAO → Mapper
                     └→ 외부 Client
```

### 4.4 Handler

#### Handler의 책임

Handler는 ServiceId와 업무 유스케이스를 연결합니다.

```
ServiceId 확인
→ 요청 Body를 DTO로 변환
→ 적절한 Facade 메서드 호출
→ 결과 반환
```

#### 정상 예시

```
@Component
@RequiredArgsConstructor
public class SvCustomerHandler implements TransactionHandler {

    private final SvCustomerFacade customerFacade;

    @Override
    public Set<String> serviceIds() {
        return Set.of(
            "SV.Customer.selectSummary",
            "SV.Customer.selectDetail"
        );
    }

    @Override
    public Object handle(
            StandardRequest request,
            TransactionContext context) {

        String serviceId = context.getServiceId();

        return switch (serviceId) {
            case "SV.Customer.selectSummary" -> {
                CustomerSummaryRequest input =
                    request.bodyAs(CustomerSummaryRequest.class);

                yield customerFacade.selectSummary(input, context);
            }

            case "SV.Customer.selectDetail" -> {
                CustomerDetailRequest input =
                    request.bodyAs(CustomerDetailRequest.class);

                yield customerFacade.selectDetail(input, context);
            }

            default -> throw new ServiceNotFoundException(serviceId);
        };
    }
}
```

#### Handler에서 하면 안 되는 일

```
SQL 실행
Mapper 직접 호출
복잡한 업무 계산
대량 반복 처리
트랜잭션 선언
표준 응답 직접 생성
JWT 직접 해석
세션 직접 변경
```

#### 금지 예시

```
public Object handle(StandardRequest request) {
    String customerNo = (String) request.getBody().get("customerNo");

    if (customerNo == null) {
        return Map.of("error", "customerNo missing");
    }

    return customerMapper.selectCustomer(customerNo);
}
```

문제점:

- Handler에서 직접 검증
- Mapper 직접 호출
- 표준 오류코드 없음
- Facade와 Service 우회
- ETF 표준 응답과 충돌 가능

### 4.5 Facade

#### Facade의 책임

Facade는 하나의 거래 흐름을 조립하고 트랜잭션 경계를 관리합니다.

```
하나의 ServiceId
→ 하나의 업무 유스케이스
→ 하나의 트랜잭션 경계
```

#### 정상 예시

```
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {

    private final SvCustomerService customerService;

    @Transactional(readOnly = true)
    public CustomerSummaryResponse selectSummary(
            CustomerSummaryRequest request,
            TransactionContext context) {

        return customerService.selectSummary(request, context);
    }
}
```

조회 거래에서는 readOnly = true를 적용할 수 있습니다.

등록이나 변경 거래에서는 일반 트랜잭션을 사용합니다.

```
@Transactional
public CampaignCreateResponse createCampaign(
        CampaignCreateRequest request,
        TransactionContext context) {

    return campaignService.createCampaign(request, context);
}
```

#### Facade가 필요한 이유

한 거래가 여러 Service를 조합할 수 있기 때문입니다.

```
@Transactional
public CampaignCreateResponse createCampaign(...) {
    customerService.validateTargetCustomers(...);
    Campaign campaign = campaignService.create(...);
    messageService.reserveCampaignMessage(...);

    return CampaignCreateResponse.from(campaign);
}
```

#### Facade에서 하면 안 되는 일

- SQL 직접 실행
- Mapper 직접 호출
- HTTP 요청·응답 처리
- JWT 문자열 직접 해석
- 복잡한 업무 규칙을 모두 작성
- 표준 응답 객체 직접 생성

### 4.6 Service

#### Service의 책임

Service는 실제 업무 처리 순서를 구현합니다.

```
입력 검증
→ 필요한 데이터 조회
→ 업무 판단
→ 데이터 가공
→ 저장 또는 결과 반환
```

#### 정상 예시

```
@Service
@RequiredArgsConstructor
public class SvCustomerService {

    private final SvCustomerRule customerRule;
    private final SvCustomerDao customerDao;

    public CustomerSummaryResponse selectSummary(
            CustomerSummaryRequest request,
            TransactionContext context) {

        customerRule.validateSummaryRequest(request);

        CustomerSummaryData data =
            customerDao.selectCustomerSummary(request.customerNo());

        customerRule.validateCustomerExists(data);

        return CustomerSummaryResponse.from(data);
    }
}
```

#### Service의 특징

- 업무 처리 순서를 읽을 수 있어야 함
- Rule과 DAO를 조합함
- 필요한 경우 외부 Client 호출
- DTO를 업무 결과로 변환
- 기술 세부사항보다 업무 흐름 중심

#### Service에서 하면 안 되는 일

- Controller 역할
- HttpServletRequest 직접 사용
- JWT 문자열 직접 분석
- MyBatis Mapper 직접 남용
- SQL 문자열 작성
- 표준 Header 임의 변경
- 모든 메서드에 무분별한 @Transactional

### 4.7 Rule

#### Rule의 책임

Rule은 업무 조건을 검사하고 계산합니다.

예:

```
고객번호는 필수인가?
조회 가능한 고객 상태인가?
캠페인 시작일이 종료일보다 빠른가?
상품이 판매 가능한 상태인가?
사용자 지점에서 해당 고객을 조회할 수 있는가?
```

#### 정상 예시

```
@Component
public class SvCustomerRule {

    public void validateSummaryRequest(
            CustomerSummaryRequest request) {

        if (request == null ||
            request.customerNo() == null ||
            request.customerNo().isBlank()) {

            throw new BusinessException(
                "SV-CUST-0001",
                "고객번호는 필수입니다."
            );
        }
    }

    public void validateCustomerExists(
            CustomerSummaryData data) {

        if (data == null) {
            throw new BusinessException(
                "SV-CUST-0002",
                "고객을 찾을 수 없습니다."
            );
        }
    }
}
```

#### 좋은 Rule의 특징

```
입력이 같으면 결과가 같다.
DB 상태를 직접 변경하지 않는다.
외부 시스템을 호출하지 않는다.
업무 의미가 메서드명에 드러난다.
단위 테스트가 쉽다.
```

#### Rule에서 하면 안 되는 일

```
Mapper 직접 호출
DB Connection 사용
외부 HTTP 호출
파일 저장
세션 변경
로그인 상태 변경
```

단, 데이터가 필요한 복잡한 업무 검증은 Service가 DAO로 데이터를 조회한 뒤 Rule에 전달합니다.

```
Service가 데이터 조회
→ Rule이 검증
```

### 4.8 DAO

#### DAO의 책임

DAO는 데이터 접근을 추상화하고 Mapper를 호출합니다.

```
Service
→ DAO
→ Mapper
→ SQL
```

#### 정상 예시

```
@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

    private final SvCustomerMapper customerMapper;

    public CustomerSummaryData selectCustomerSummary(
            String customerNo) {

        return customerMapper.selectCustomerSummary(customerNo);
    }
}
```

DAO는 DB 예외를 시스템 표준 예외로 변환하거나 데이터 접근 정책을 적용할 수 있습니다.

#### DAO가 필요한 이유

Service가 MyBatis 구현 세부사항에 직접 의존하지 않도록 하기 위해서입니다.

```
Service는 업무를 알고,
DAO는 데이터를 가져오는 방법을 안다.
```

#### DAO에서 하면 안 되는 일

- 고객등급 산정 같은 업무 판단
- 사용자 권한 판단
- 화면 메시지 생성
- 거래통제
- 표준 응답 생성
- 여러 업무를 조합하는 복잡한 흐름

### 4.9 Mapper

#### Mapper의 책임

Mapper는 SQL 실행을 담당합니다.

Java 인터페이스 예:

```
@Mapper
public interface SvCustomerMapper {

    CustomerSummaryData selectCustomerSummary(
        String customerNo);
}
```

Mapper XML 예:

```
<mapper namespace=
  "com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper">

    <select id="selectCustomerSummary"
            parameterType="string"
            resultType=
              "com.nh.nsight.marketing.sv.customer.dto.CustomerSummaryData">

        SELECT CUSTOMER_NO,
               CUSTOMER_NAME,
               CUSTOMER_GRADE
          FROM SV_CUSTOMER_SUMMARY
         WHERE CUSTOMER_NO = #{customerNo}

    </select>

</mapper>
```

#### Mapper에서 하면 안 되는 일

- 사용자 권한 검증
- 거래통제
- 업무 흐름 조정
- HTTP 처리
- 화면 메시지 조립
- Java 업무 로직 작성
SQL 안에 지나치게 많은 업무 판단을 넣는 것도 피해야 합니다.

### 4.10 DTO

6계층 사이에서는 DTO를 사용합니다.

대표적인 DTO는 다음과 같습니다.

| DTO | 용도 |
| --- | --- |
| Request DTO | 화면 요청을 업무 입력으로 변환 |
| Response DTO | 화면에 반환할 업무 결과 |
| Query DTO | DAO와 Mapper의 조회조건 |
| Result DTO | DB 조회 결과 |
| Command DTO | 등록·변경 명령 |
| External DTO | 외부 시스템 요청·응답 |

#### 요청 DTO

```
public record CustomerSummaryRequest(
    String customerNo
) {
}
```

#### 응답 DTO

```
public record CustomerSummaryResponse(
    String customerNo,
    String customerName,
    String grade
) {
    public static CustomerSummaryResponse from(
            CustomerSummaryData data) {

        return new CustomerSummaryResponse(
            data.customerNo(),
            data.customerName(),
            data.grade()
        );
    }
}
```

DB 조회 결과와 화면 응답 DTO를 무조건 같은 객체로 사용하지 않는 것이 좋습니다.

```
DB 구조가 바뀌어도
화면 계약은 유지할 수 있어야 한다.
```

### 4.11 권장 패키지 구조

업무 패키지는 다음 순서를 기준으로 구성합니다.

```
기관·시스템
→ 업무 영역
→ 업무코드
→ 도메인
→ 계층
```

권장 형식:

```
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}
```

고객 도메인 예:

```
com.nh.nsight.marketing.sv.customer
├─ handler
│   └─ SvCustomerHandler.java
├─ facade
│   └─ SvCustomerFacade.java
├─ service
│   └─ SvCustomerService.java
├─ rule
│   └─ SvCustomerRule.java
├─ dao
│   └─ SvCustomerDao.java
├─ mapper
│   └─ SvCustomerMapper.java
└─ dto
    ├─ CustomerSummaryRequest.java
    ├─ CustomerSummaryResponse.java
    └─ CustomerSummaryData.java
```

Mapper XML:

```
src/main/resources/mapper/sv/customer/
└─ SvCustomerMapper.xml
```

### 4.12 고객 요약 조회 전체 예시

#### 1단계: 화면 요청

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001"
  },
  "body": {
    "customerNo": "CUST0001"
  }
}
```

#### 2단계: Handler

```
ServiceId 확인
→ CustomerSummaryRequest 변환
→ Facade.selectSummary 호출
```

#### 3단계: Facade

```
조회용 트랜잭션 시작
→ Service.selectSummary 호출
```

#### 4단계: Service

```
Rule로 고객번호 검증
→ DAO로 고객요약 조회
→ Rule로 조회결과 확인
→ Response DTO 생성
```

#### 5단계: Rule

```
고객번호 필수 확인
조회결과 존재 확인
```

#### 6단계: DAO

```
Mapper.selectCustomerSummary 호출
```

#### 7단계: Mapper

```
SELECT CUSTOMER_NO,
       CUSTOMER_NAME,
       CUSTOMER_GRADE
  FROM SV_CUSTOMER_SUMMARY
 WHERE CUSTOMER_NO = :customerNo
```

#### 8단계: 결과 반환

```
Mapper
→ DAO
→ Service
→ Facade
→ Handler
→ Dispatcher
→ TCF
→ ETF
→ StandardResponse
→ 화면
```

### 4.13 나쁜 구조와 좋은 구조 비교

#### 나쁜 구조

```
CustomerController
  ↓
CustomerService
  ├─ 사용자 권한 확인
  ├─ SQL 실행
  ├─ 응답 JSON 생성
  ├─ 오류코드 생성
  └─ 거래로그 기록
```

문제:

- 공통 통제와 업무 로직 혼합
- TCF 우회
- 테스트 어려움
- 책임 불명확
- 코드 재사용 어려움
- 운영 추적성 부족

#### 좋은 구조

```
OnlineTransactionController
  ↓
TCF / STF
  ↓
Dispatcher
  ↓
CustomerHandler
  ↓
CustomerFacade
  ↓
CustomerService
  ├─ CustomerRule
  └─ CustomerDao
       ↓
     CustomerMapper
  ↓
ETF
```

장점:

- 책임이 분명함
- 공통 정책을 TCF가 일괄 적용
- Rule 단위 테스트 가능
- Mapper 통합 테스트 가능
- ServiceId로 운영 추적 가능
- DB와 화면의 변경 영향 분리 가능

### 4.14 계층 간 허용 호출

| 호출 주체 | 호출 허용 대상 |
| --- | --- |
| Handler | Facade |
| Facade | Service, 승인된 도메인 계약 |
| Service | Rule, DAO, Client |
| Rule | 기본적으로 다른 계층 호출 금지 |
| DAO | Mapper |
| Mapper | DB |

금지 관계:

```
Handler → Mapper
Handler → DAO
Rule → Mapper
Rule → 외부 시스템
DAO → Handler
Mapper → Service
업무 WAR → 다른 업무 WAR의 DAO
```

다른 업무 WAR를 호출해야 한다면 Java 클래스를 직접 Import하지 않고 ServiceId 기반 표준 연동을 사용합니다.

### 4.15 트랜잭션 책임

트랜잭션은 여러 DB 작업을 하나의 작업처럼 처리하는 기능입니다.

예:

```
캠페인 기본정보 등록
+ 캠페인 대상조건 등록
+ 캠페인 메시지 등록
```

세 작업 중 하나가 실패하면 전체를 취소해야 할 수 있습니다.

```
Facade
= 유스케이스와 트랜잭션 경계
```

따라서 일반적으로 @Transactional은 Facade에 둡니다.

금지 예시:

```
Handler @Transactional
Rule @Transactional
Mapper @Transactional
```

### 4.16 오류 책임

| 계층 | 오류 처리 책임 |
| --- | --- |
| Handler | 잘못된 ServiceId 분기 |
| Facade | 트랜잭션 Rollback 전파 |
| Service | 업무 흐름상 오류 판단 |
| Rule | 업무 규칙 위반 예외 |
| DAO | 데이터 접근 예외 변환 |
| Mapper | DB 오류 발생 지점 |
| ETF | 최종 오류코드와 표준 응답 조립 |

업무 개발자는 아무 예외나 RuntimeException으로 발생시키지 말고 표준 업무 예외와 오류코드를 사용해야 합니다.

### 4.17 보안 책임

| 영역 | 주요 책임 |
| --- | --- |
| Gateway·Filter | JWT 서명과 만료 검증 |
| STF | 인증 Context와 Header 정합성 |
| 공통 권한 | 기능 실행 권한 |
| Service·Rule | 업무 데이터 범위와 상세 권한 |
| DAO·Mapper | 권한 없는 데이터가 조회되지 않도록 조건 반영 |
| ETF·로그 | 개인정보 원문 노출 방지 |

초보자가 특히 주의할 점:

```
사용자 ID를 Body에서 받아 신뢰하지 않는다.
JWT 원문을 로그에 출력하지 않는다.
주민번호·계좌번호를 그대로 로그에 남기지 않는다.
권한 검증을 화면에만 의존하지 않는다.
```

### 4.18 성능 책임

각 계층은 성능에도 영향을 줍니다.

| 계층 | 성능 주의사항 |
| --- | --- |
| Handler | 불필요한 반복과 변환 최소화 |
| Facade | 트랜잭션 범위를 짧게 유지 |
| Service | 불필요한 중복 조회 방지 |
| Rule | 과도한 연산과 외부 호출 금지 |
| DAO | 필요한 데이터만 조회 |
| Mapper | 인덱스 활용, 전체 조회 방지 |
| 외부 Client | Timeout 필수 |
| TCF | ServiceId별 Timeout 정책 적용 |

다음과 같은 SQL은 주의해야 합니다.

```
SELECT *
  FROM SV_CUSTOMER_SUMMARY;
```

조건 없이 전체 데이터를 온라인으로 조회하면 많은 메모리와 DB 자원을 사용할 수 있습니다.

### 4.19 테스트 방법

#### Rule 단위 테스트

```
고객번호 정상
고객번호 null
고객번호 빈 문자열
고객 없음
```

#### Service 단위 테스트

```
Rule 정상 + DAO 결과 존재
Rule 오류
DAO 결과 없음
DAO 예외
```

#### Mapper 통합 테스트

```
정상 고객 조회
없는 고객 조회
컬럼 매핑 확인
SQL 성능 확인
```

#### 거래 통합 테스트

```
POST /sv/online
→ STF
→ Dispatcher
→ Handler
→ 6계층
→ ETF
```

#### 오류 테스트

```
ServiceId 오타
업무코드 불일치
권한 없음
거래통제
Timeout
DB 오류
```

### 4.20 자동 품질 Gate

프로젝트에서는 다음 위반을 자동검증할 수 있습니다.

| 검증 항목 | 예 |
| --- | --- |
| 업무 Controller 금지 | 임의 @RestController 탐지 |
| Handler 직접 DB 호출 금지 | Handler가 DAO·Mapper 의존 |
| Rule DB 접근 금지 | Rule이 Mapper 의존 |
| 패키지 구조 | 업무코드·도메인·계층 위치 |
| ServiceId 중복 | 둘 이상의 Handler 등록 |
| ServiceId 미등록 | Handler는 있으나 OM Catalog 없음 |
| Mapper 정합성 | Interface와 XML Namespace 불일치 |
| SQL ID 정합성 | DAO 메서드와 Mapper ID 불일치 |
| 테스트 존재 | 신규 ServiceId 테스트 누락 |
| 로그 보안 | 토큰·개인정보 로그 탐지 |

자동검증 도구로는 ArchUnit, Checkstyle, 단위 테스트, Gradle Task, CI/CD Script 등을 사용할 수 있습니다.

### 4.21 초보 개발자의 책임 경계

| 역할 | 주된 책임 |
| --- | --- |
| 업무 개발자 | Handler 이하 업무 프로그램 작성 |
| 프레임워크팀 | TCF·STF·ETF·Dispatcher 유지 |
| 아키텍트 | 계층·ServiceId·패키지·연계 기준 |
| OM 운영팀 | Catalog·거래통제·Timeout 관리 |
| DBA·DA | 테이블·SQL·인덱스 검토 |
| 보안팀 | JWT·권한·개인정보 기준 |
| 운영팀 | 로그·모니터링·장애 대응 |
| 테스트팀 | 정상·오류·성능·장애 검증 |

초보 개발자는 모든 영역을 직접 결정하는 사람이 아닙니다.

다만 자신의 프로그램이 다른 영역과 어떻게 연결되는지는 알아야 합니다.

### 제4장 요약

```
Handler
= ServiceId를 보고 Facade 호출

Facade
= 거래 흐름과 트랜잭션 관리

Service
= 업무 처리 순서

Rule
= 업무 조건과 규칙

DAO
= 데이터 접근 추상화

Mapper
= SQL 실행
```

가장 중요한 규칙은 다음입니다.

```
Handler는 얇게 만든다.
트랜잭션은 Facade에 둔다.
업무 흐름은 Service에 둔다.
검증은 Rule에 둔다.
DB 접근은 DAO를 거친다.
SQL은 Mapper XML에 둔다.
```

## 5. 제1부 종합 실습

### 5.1 실습 문제

다음 요구사항을 읽고 각 정보를 정의해 보십시오.

```
SV 고객 화면에서 고객번호를 입력하고
‘고객 요약 조회’ 버튼을 클릭하면
고객명, 고객등급, 보유상품 수를 조회한다.
```

### 5.2 화면과 거래 식별

| 항목 | 예시 |
| --- | --- |
| 화면 ID | SV-CUS-0001 |
| 이벤트 ID | SV-CUS-0001-E01 |
| 업무코드 | SV |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 처리유형 | INQUIRY |
| 업무 WAR | sv-service |
| Endpoint | POST /sv/online |

### 5.3 프로그램 정의

| 계층 | 프로그램 |
| --- | --- |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerRule |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper |
| Mapper XML | SvCustomerMapper.xml |
| 요청 DTO | CustomerSummaryRequest |
| 응답 DTO | CustomerSummaryResponse |
| DB 결과 DTO | CustomerSummaryData |

### 5.4 전체 처리 흐름

```
고객 요약 조회 버튼
  ↓
POST /sv/online
  ↓
serviceId = SV.Customer.selectSummary
  ↓
OnlineTransactionController
  ↓
TCF.process()
  ↓
STF
  ├─ Header
  ├─ 인증
  ├─ 권한
  ├─ 거래통제
  ├─ Timeout
  └─ 거래로그 시작
  ↓
TransactionDispatcher
  ↓
SvCustomerHandler
  ↓
SvCustomerFacade
  ↓
SvCustomerService
  ├─ SvCustomerRule
  └─ SvCustomerDao
       ↓
     SvCustomerMapper
       ↓
     SV_CUSTOMER_SUMMARY
  ↓
CustomerSummaryResponse
  ↓
ETF
  ↓
StandardResponse
  ↓
화면 표시
```

### 5.5 실습 검증 시나리오

| 번호 | 조건 | 기대 결과 |
| --- | --- | --- |
| 1 | 정상 고객번호 | 고객 요약 정상 반환 |
| 2 | 고객번호 없음 | 필수값 업무 오류 |
| 3 | 존재하지 않는 고객 | 고객 없음 업무 오류 |
| 4 | ServiceId 오타 | Handler 미등록 오류 |
| 5 | 업무코드 불일치 | Header 검증 오류 |
| 6 | 사용자 권한 없음 | 권한 오류 |
| 7 | 거래통제 DENY | 업무 미실행 |
| 8 | SQL 3초 초과 | Timeout 오류 |
| 9 | DB 연결 실패 | 시스템 오류 |
| 10 | 동일 변경요청 반복 | 중복 요청 차단 |

## 6. 제1부 체크리스트

### 6.1 개념 이해

| 점검 질문 | 확인 |
| --- | --- |
| TCF가 필요한 이유를 설명할 수 있는가? | □ |
| REST URL 방식과 ServiceId 방식의 차이를 설명할 수 있는가? | □ |
| 업무 WAR와 TCF의 관계를 설명할 수 있는가? | □ |
| Gateway의 역할을 설명할 수 있는가? | □ |
| OM이 단순 관리자 화면이 아닌 이유를 설명할 수 있는가? | □ |

### 6.2 처리 흐름

| 점검 질문 | 확인 |
| --- | --- |
| 요청이 Controller부터 ETF까지 가는 순서를 말할 수 있는가? | □ |
| STF가 업무 실행 전에 하는 일을 설명할 수 있는가? | □ |
| Dispatcher가 ServiceId를 사용하는 이유를 설명할 수 있는가? | □ |
| Timeout이 시스템 보호 기능인 이유를 설명할 수 있는가? | □ |
| GUID와 TraceId의 차이를 설명할 수 있는가? | □ |

### 6.3 6계층

| 점검 질문 | 확인 |
| --- | --- |
| Handler의 책임을 설명할 수 있는가? | □ |
| 트랜잭션이 Facade에 있는 이유를 설명할 수 있는가? | □ |
| Service와 Rule의 차이를 설명할 수 있는가? | □ |
| DAO와 Mapper의 차이를 설명할 수 있는가? | □ |
| Handler가 Mapper를 직접 호출하면 안 되는 이유를 설명할 수 있는가? | □ |

### 6.4 개발 준비

| 점검 질문 | 확인 |
| --- | --- |
| 신규 거래의 ServiceId를 정의할 수 있는가? | □ |
| 업무코드·도메인·계층 패키지를 구분할 수 있는가? | □ |
| 요청·응답 DTO를 구분할 수 있는가? | □ |
| 신규 거래에 필요한 OM 등록정보를 알고 있는가? | □ |
| 정상·오류·Timeout 테스트를 작성할 수 있는가? | □ |

## 7. 시사점

### 7.1 핵심 아키텍처 판단

NSIGHT TCF의 핵심은 많은 공통 클래스를 제공하는 데 있지 않습니다.

```
모든 거래가
동일한 사전검사,
동일한 실행경로,
동일한 오류처리,
동일한 로그체계를
통과하도록 만드는 것
```

개발자가 TCF를 우회해 별도의 Controller와 응답 형식을 만들면 기능은 빠르게 완성될 수 있지만 운영 통제와 추적성은 약해집니다.

### 7.2 주요 위험

초보 개발자가 가장 자주 만드는 구조적 위험은 다음과 같습니다.

| 위험 | 결과 |
| --- | --- |
| 임의 업무 Controller 추가 | TCF 공통 통제 우회 |
| ServiceId 임의 변경 | 화면·Handler·OM·로그 연결 단절 |
| Handler에서 DB 호출 | 계층과 트랜잭션 경계 훼손 |
| Rule에서 외부 호출 | 검증 로직의 부수효과 발생 |
| 사용자 ID를 요청값으로 신뢰 | 위변조 위험 |
| Timeout 없는 외부 호출 | Thread와 DB Pool 고갈 |
| Mapper에 과도한 업무 로직 | 변경과 테스트 어려움 |
| 거래로그 누락 | 장애 추적 불가 |
| OM 미등록 | 코드는 있으나 운영 불가 |

### 7.3 우선 보완 과제

초보 개발자가 다음 단계로 넘어가기 전에 우선 학습해야 할 내용은 다음과 같습니다.

```
1. 표준 요청·응답 JSON
2. ServiceId와 거래코드
3. Handler 개발 방법
4. 6계층 패키지 구조
5. DTO와 Validation
6. 오류코드와 BusinessException
7. Mapper XML 작성
8. 거래 테스트 방법
```

### 7.4 중장기 발전 방향

제1부에서는 전체 길을 이해했습니다.

이후에는 다음 순서로 실력을 확장해야 합니다.

```
전체 흐름 이해
→ 표준 전문 이해
→ ServiceId 설계
→ Handler 개발
→ 6계층 구현
→ SQL·Mapper 작성
→ 오류·Timeout 처리
→ 인증·권한 이해
→ OM 운영관리 이해
→ 배포·테스트·장애 대응
```

초보 개발자의 목표는 처음부터 TCF 내부 엔진을 수정하는 것이 아닙니다.

```
주어진 표준 안에서
안전하고 추적 가능한 업무 거래를
정확하게 구현하는 것
```

## 8. 마무리말

제1부에서 가장 중요하게 기억해야 할 문장은 다음과 같습니다.

```
사용자의 요청은
Gateway와 업무 WAR를 거쳐 TCF로 들어온다.

TCF는 STF에서 요청을 검사하고
ServiceId로 Handler를 찾는다.

업무는
Handler → Facade → Service → Rule → DAO → Mapper
구조로 처리한다.

ETF는 성공과 오류를 표준 응답으로 만들고
거래로그를 종료한다.
```

처음에는 클래스와 용어가 많아 보이지만, 전체 구조는 다음 세 구간으로 단순화할 수 있습니다.

```
들어오기
= Gateway → Controller → STF

처리하기
= Dispatcher → Handler → 6계층

끝내기
= ETF → 표준 응답 → 거래로그
```

이 세 구간을 이해하면 이후에 배우게 될 ServiceId, 표준 전문, JWT, Timeout, DAO, Mapper, OM, 배포와 장애 대응이 서로 어떻게 연결되는지 이해할 수 있습니다.

제1부의 학습 목표는 소스코드를 많이 작성하는 것이 아닙니다.

```
요청 한 건의 전체 이동 경로를
자신의 말로 설명할 수 있게 되는 것
```

그것이 NSIGHT TCF 개발자로 성장하기 위한 첫 번째 단계입니다.

