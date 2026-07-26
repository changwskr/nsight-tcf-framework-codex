NSIGHT 표준 프로그램 설계서

SV.Customer.selectSummary — 고객 종합정보 조회

1. 도입 전 안내말

NSIGHT 프로그램 설계서는 Java 클래스 목록을 나열하는 문서가 아니다.

하나의 ServiceId가 어떤 프로그램을 거쳐 실행되고, 각 프로그램이 어떤 책임을 가지며, 어떤 프로그램을 호출할 수 있고, 어떤 행위를 해서는 안 되는지를 정의하는 구현 책임 설계서다.

```
화면 이벤트
  ↓
ServiceId
  ↓
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  ├─ DAO
  │   ↓
  │ Mapper Interface
  │   ↓
  │ Mapper XML / SQL
  └─ 외부 Client
  ↓
응답 DTO
  ↓
ETF 표준 응답
```

NSIGHT TCF에서는 업무별 Controller가 Service나 Mapper를 직접 호출하지 않는다. 공통 OnlineTransactionController가 요청을 수신하고 ServiceId를 기준으로 Handler를 선택한 뒤 업무 프로그램을 실행한다. 따라서 업무 프로그램 설계의 시작점은 Controller가 아니라 ServiceId와 Handler다.

프로그램 계층은 Handler → Facade → Service → Rule·DAO → Mapper 순서를 따르며, 트랜잭션 경계는 Facade에 두는 것을 원칙으로 한다.

2. 문서 개요

2.1 목적

본 설계서의 목적은 SV.Customer.selectSummary 거래를 구현하는 프로그램의 구조, 책임, 인터페이스와 처리 흐름을 정의하는 것이다.

| 목적 | 설명 |
| --- | --- |
| 프로그램 식별 | 거래를 구성하는 전체 프로그램 목록 정의 |
| 책임 분리 | Handler·Facade·Service·Rule·DAO·Mapper 책임 정의 |
| 호출 통제 | 허용 호출과 금지 호출 정의 |
| DTO 표준화 | 계층 간 전달 객체와 필드 정의 |
| 트랜잭션 | 트랜잭션 시작·종료 위치 정의 |
| 예외 처리 | 업무·시스템·DB 오류 변환 기준 정의 |
| 데이터 접근 | DAO·Mapper·SQL 연결 관계 정의 |
| 테스트 | 프로그램별 단위·통합 테스트 기준 정의 |
| 추적성 | 화면·ServiceId·프로그램·SQL 연결 |
| 변경관리 | 프로그램 변경 영향과 호환성 관리 |

2.2 적용범위

| 영역 | 대상 |
| --- | --- |
| 업무 WAR | sv-service |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 화면 | SV-CUS-0001 고객 종합정보 조회 |
| 이벤트 | SV-CUS-0001-E01 |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerInquiryRule |
| DAO | SvCustomerDao, SvCustomerGradeDao, SvCustomerProductDao |
| Mapper | 고객요약·등급·상품 Mapper |
| DTO | 요청·응답·조회조건·결과 DTO |
| DB | 고객요약·등급·상품 테이블 |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 업무 개발자 | 프로그램 구현 기준 |
| 프레임워크 개발자 | Handler 등록과 TCF 연계 검토 |
| 아키텍트 | 계층·책임·의존성 검증 |
| DBA·DA | Mapper·SQL·DB 객체 검토 |
| 테스트 담당자 | 클래스·메서드 단위 테스트 작성 |
| 운영 담당자 | ServiceId와 프로그램 장애 추적 |
| 품질 담당자 | 코드와 설계 정합성 확인 |

2.4 선행조건

- SV.Customer.selectSummary ServiceId가 확정되어 있어야 한다.
- 표준 요청·응답 전문이 정의되어 있어야 한다.
- DTO 명명규칙이 확정되어 있어야 한다.
- 패키지 구조가 확정되어 있어야 한다.
- 트랜잭션 경계가 Facade로 정의되어 있어야 한다.
- Mapper XML과 DB 객체가 식별되어 있어야 한다.
- 오류코드와 예외 클래스가 정의되어 있어야 한다.
- OM Service Catalog와 Timeout 정책이 등록되어 있어야 한다.
2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| Handler | ServiceId와 업무 유스케이스를 연결하는 진입 프로그램 |
| Facade | 유스케이스 조립과 트랜잭션 경계를 담당하는 프로그램 |
| Service | 업무 처리 흐름과 도메인 로직을 수행하는 프로그램 |
| Rule | 부작용 없이 업무 규칙을 검증하는 프로그램 |
| DAO | 데이터 접근을 추상화하고 Mapper를 호출하는 프로그램 |
| Mapper | MyBatis SQL 실행 인터페이스 |
| Request DTO | 거래 요청 데이터를 표현하는 객체 |
| Response DTO | 거래 응답 데이터를 표현하는 객체 |
| Query DTO | DAO·Mapper 조회조건을 표현하는 객체 |
| Result DTO | DB 조회 결과를 표현하는 객체 |

3. 문제 정의 및 설계 배경

3.1 문제 정의

프로그램 계층이 명확하지 않으면 다음 문제가 발생한다.

| 문제 | 영향 |
| --- | --- |
| Handler에서 DB 호출 | 트랜잭션·업무 흐름 분리 실패 |
| Service에서 표준 응답 생성 | ETF 책임 침범 |
| Rule에서 Mapper 호출 | 검증 로직과 데이터 접근 결합 |
| DAO에서 업무 판단 | SQL 실행과 업무 규칙 혼합 |
| Mapper에 복잡한 업무 로직 | 테스트·재사용·변경 어려움 |
| DTO 무분별한 재사용 | 화면·DB·업무 모델 결합 |
| 클래스 간 순환 참조 | 유지보수와 테스트 어려움 |
| Facade 없는 Service 직접 호출 | 트랜잭션 경계 불명확 |
| 메서드명과 ServiceId 불일치 | 운영 추적성 저하 |

3.2 설계 배경

고객 종합정보 조회 거래는 고객 기본정보, 고객등급, 상품현황을 조합한다.

따라서 프로그램을 다음 책임으로 분리한다.

```
Handler
= 거래 진입과 요청 DTO 변환

Facade
= 유스케이스·트랜잭션 경계

Service
= 고객요약 조회 흐름 조립

Rule
= 조회조건과 데이터권한 검증

DAO
= 고객·등급·상품 조회

Mapper
= SQL 실행

Response DTO
= 화면 반환 데이터 조립
```

4. 요구사항과 제약조건

4.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| PRG-01 | ServiceId를 기준으로 Handler가 실행되어야 한다. |
| PRG-02 | 요청 Body를 SvCustomerSummaryRequest로 변환해야 한다. |
| PRG-03 | 조회조건과 사용자 데이터권한을 검증해야 한다. |
| PRG-04 | 고객 기본정보를 조회해야 한다. |
| PRG-05 | 고객등급을 조회해야 한다. |
| PRG-06 | 고객 상품현황을 조회해야 한다. |
| PRG-07 | 결과를 하나의 응답 DTO로 조립해야 한다. |
| PRG-08 | 개인정보를 권한에 따라 마스킹해야 한다. |
| PRG-09 | 고객 미존재와 시스템 오류를 구분해야 한다. |
| PRG-10 | 모든 DB 조회는 읽기전용 트랜잭션에서 실행해야 한다. |

4.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 응답시간 | p95 3초 이내 |
| 테스트 가능성 | 각 계층 독립 단위 테스트 가능 |
| 결합도 | 상위 계층이 하위 구현 상세를 알지 않음 |
| 추적성 | ServiceId와 클래스·메서드 연결 |
| 보안 | 사용자 입력값과 인증 문맥 분리 |
| 운영성 | 오류 발생 프로그램과 SQL ID 식별 가능 |
| 확장성 | 등급·상품 조회 로직 독립 변경 가능 |
| 품질 | 계층 위반 자동검증 가능 |

4.3 제약조건

- Handler에서 DAO·Mapper를 직접 호출하지 않는다.
- Facade에서 SQL이나 Mapper를 직접 호출하지 않는다.
- Service에서 표준 응답을 생성하지 않는다.
- Rule에서 DB·Cache·외부 API를 호출하지 않는다.
- DAO에서 업무 예외를 판단하지 않는다.
- Mapper는 SQL 실행만 수행한다.
- DB Entity나 Map을 화면 응답으로 직접 반환하지 않는다.
- 요청 DTO와 DB Result DTO를 동일 객체로 사용하지 않는다.
- 공통 예외 외에 임의 RuntimeException을 그대로 전달하지 않는다.
- 트랜잭션은 Facade 외 계층에 중복 선언하지 않는다.
5. 설계 원칙

5.1 단방향 호출 원칙

```
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  └─ DAO
       ↓
     Mapper
```

허용되는 호출 방향은 위에서 아래 방향이다.

| 호출 주체 | 호출 가능 대상 |
| --- | --- |
| Handler | Facade |
| Facade | Service |
| Service | Rule, DAO, 공통 업무 Service |
| Rule | 순수 Utility |
| DAO | Mapper |
| Mapper | SQL |

다음 호출은 금지한다.

```
DAO → Service
Rule → DAO
Mapper → Rule
Service → Handler
Facade → Mapper
Handler → DAO
```

5.2 클래스당 단일 책임

| 클래스 | 단일 책임 |
| --- | --- |
| Handler | ServiceId 요청 진입 |
| Facade | 유스케이스와 트랜잭션 |
| Service | 업무 처리 |
| Rule | 업무 규칙 검증 |
| DAO | 데이터 접근 |
| Mapper | SQL 실행 |
| DTO | 계층 간 데이터 전달 |

5.3 메서드명 정합성

동일 거래의 핵심 메서드명은 가능한 한 통일한다.

```
ServiceId
SV.Customer.selectSummary

Handler
handleSelectSummary()

Facade
selectSummary()

Service
selectSummary()

DAO
selectSummary()

Mapper
selectSummary()
```

메서드명이 일치하면 로그·설계·소스 추적이 쉬워진다.

5.4 DTO 분리 원칙

```
화면 요청
→ Request DTO

DAO 조회조건
→ Query DTO

DB 조회 결과
→ Result DTO

화면 응답
→ Response DTO
```

하나의 DTO를 전 계층에서 공용으로 사용하지 않는다.

5.5 불변 객체 우선

DTO는 가능한 한 record 또는 불변 객체를 사용한다.

```
public record SvCustomerSummaryRequest(
    String customerNo,
    String customerName,
    String searchBranchId,
    LocalDate baseDate,
    boolean includeProducts
) {}
```

6. 목표 패키지 구조

```
com.nh.nsight.sv.customer
├─ handler
│  └─ SvCustomerHandler.java
├─ facade
│  └─ SvCustomerFacade.java
├─ service
│  ├─ SvCustomerService.java
│  └─ SvCustomerMaskingService.java
├─ rule
│  └─ SvCustomerInquiryRule.java
├─ dao
│  ├─ SvCustomerDao.java
│  ├─ SvCustomerGradeDao.java
│  └─ SvCustomerProductDao.java
├─ mapper
│  ├─ SvCustomerMapper.java
│  ├─ SvCustomerGradeMapper.java
│  └─ SvCustomerProductMapper.java
├─ dto
│  ├─ request
│  │  └─ SvCustomerSummaryRequest.java
│  ├─ response
│  │  └─ SvCustomerSummaryResponse.java
│  ├─ query
│  │  └─ SvCustomerSummaryQuery.java
│  └─ result
│     ├─ SvCustomerSummaryResult.java
│     ├─ SvCustomerGradeResult.java
│     └─ SvCustomerProductResult.java
└─ exception
   └─ SvCustomerException.java
```

Mapper XML 위치:

```
src/main/resources
└─ mapper
   └─ sv
      └─ customer
         ├─ SvCustomerMapper.xml
         ├─ SvCustomerGradeMapper.xml
         └─ SvCustomerProductMapper.xml
```

7. 프로그램 목록

| 프로그램 ID | 계층 | 클래스명 | 주요 책임 |
| --- | --- | --- | --- |
| PGM-SV-CUS-001 | Handler | SvCustomerHandler | ServiceId 분기와 Facade 호출 |
| PGM-SV-CUS-002 | Facade | SvCustomerFacade | 읽기전용 트랜잭션 경계 |
| PGM-SV-CUS-003 | Service | SvCustomerService | 고객요약 조회 흐름 조립 |
| PGM-SV-CUS-004 | Service | SvCustomerMaskingService | 개인정보 마스킹 |
| PGM-SV-CUS-005 | Rule | SvCustomerInquiryRule | 요청·권한·조회조건 검증 |
| PGM-SV-CUS-006 | DAO | SvCustomerDao | 고객 기본정보 조회 |
| PGM-SV-CUS-007 | DAO | SvCustomerGradeDao | 고객등급 조회 |
| PGM-SV-CUS-008 | DAO | SvCustomerProductDao | 상품현황 조회 |
| PGM-SV-CUS-009 | Mapper | SvCustomerMapper | 고객요약 SQL 실행 |
| PGM-SV-CUS-010 | Mapper | SvCustomerGradeMapper | 고객등급 SQL 실행 |
| PGM-SV-CUS-011 | Mapper | SvCustomerProductMapper | 상품현황 SQL 실행 |

8. Handler 설계

8.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 클래스명 | SvCustomerHandler |
| 패키지 | com.nh.nsight.sv.customer.handler |
| 계층 | Handler |
| Spring Bean | 예 |
| ServiceId | SV.Customer.selectSummary |
| 의존성 | SvCustomerFacade |
| 트랜잭션 | 금지 |

8.2 책임

- 지원 ServiceId 선언
- 요청 Body를 요청 DTO로 변환
- 요청 DTO 기본 형식 검증 호출
- Facade 메서드 호출
- 업무 결과를 TCF에 반환
8.3 금지 책임

- DAO·Mapper 직접 호출
- @Transactional 선언
- SQL 조립
- 개인정보 마스킹
- 표준 응답 생성
- 거래로그 직접 종료
- 업무 복잡 로직 수행
8.4 메서드 설계

| 메서드 | 입력 | 출력 | 설명 |
| --- | --- | --- | --- |
| serviceIds() | 없음 | Set<String> | 지원 ServiceId 목록 |
| handle() | TransactionContext, Body | Object | 공통 Handler 진입 |
| handleSelectSummary() | Body | SvCustomerSummaryResponse | 고객요약 Facade 호출 |
| toRequest() | Body | SvCustomerSummaryRequest | 요청 DTO 변환 |

8.5 인터페이스 예시

```
@Component
@RequiredArgsConstructor
public class SvCustomerHandler implements TransactionHandler {

    private static final String SELECT_SUMMARY =
        "SV.Customer.selectSummary";

    private final SvCustomerFacade customerFacade;
    private final ObjectMapper objectMapper;

    @Override
    public Set<String> serviceIds() {
        return Set.of(SELECT_SUMMARY);
    }

    @Override
    public Object handle(
            TransactionContext context,
            Object body) {

        return switch (context.serviceId()) {
            case SELECT_SUMMARY -> handleSelectSummary(body);
            default -> throw new ServiceNotSupportedException(
                context.serviceId()
            );
        };
    }

    private SvCustomerSummaryResponse handleSelectSummary(
            Object body) {

        SvCustomerSummaryRequest request =
            objectMapper.convertValue(
                body,
                SvCustomerSummaryRequest.class
            );

        return customerFacade.selectSummary(request);
    }
}
```

8.6 Handler 오류 처리

| 오류 | 처리 |
| --- | --- |
| Body 변환 실패 | 표준 입력 오류로 변환 |
| 미지원 ServiceId | SERVICE_NOT_SUPPORTED |
| 필수 Body 누락 | 업무 입력 오류 |
| Facade 업무 예외 | 상위로 전달 |
| 예상하지 못한 예외 | 시스템 예외로 변환 |

9. Facade 설계

9.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 클래스명 | SvCustomerFacade |
| 패키지 | com.nh.nsight.sv.customer.facade |
| 계층 | Facade |
| 의존성 | SvCustomerService |
| 트랜잭션 | 읽기전용 |
| Timeout | 3초 |

9.2 책임

- 고객요약 유스케이스 진입
- 트랜잭션 시작·Commit·Rollback
- Service 호출
- 유스케이스 단위 예외 경계 제공
9.3 금지 책임

- Mapper 직접 호출
- SQL 작성
- 화면 응답 포맷 작성
- 복잡한 업무 규칙
- 외부 인증 처리
- UI 상태 처리
9.4 메서드 설계

| 메서드 | 입력 | 출력 | 트랜잭션 |
| --- | --- | --- | --- |
| selectSummary() | SvCustomerSummaryRequest | SvCustomerSummaryResponse | readOnly=true, 3초 |

9.5 구현 예시

```
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {

    private final SvCustomerService customerService;

    @Transactional(
        readOnly = true,
        timeout = 3
    )
    public SvCustomerSummaryResponse selectSummary(
            SvCustomerSummaryRequest request) {

        return customerService.selectSummary(request);
    }
}
```

9.6 Rollback 기준

| 상황 | 처리 |
| --- | --- |
| 정상 조회 | 정상 종료 |
| 고객 미존재 | 업무 예외 또는 빈 결과 |
| 권한 오류 | 업무 예외 |
| SQL 오류 | Rollback |
| Timeout | Rollback |
| RuntimeException | Rollback |

조회 거래는 데이터 변경이 없지만 DB Connection과 자원의 일관된 반환을 위해 트랜잭션 경계를 유지한다.

10. Service 설계

10.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 클래스명 | SvCustomerService |
| 패키지 | com.nh.nsight.sv.customer.service |
| 계층 | Service |
| 의존성 | Rule·DAO·Masking Service |
| 트랜잭션 | 선언하지 않음 |

10.2 책임

- 고객요약 조회 흐름 조립
- Rule 호출
- Query DTO 생성
- 고객·등급·상품 DAO 호출
- 결과 없음 판단
- 개인정보 마스킹 호출
- 응답 DTO 조립
10.3 처리 순서

```
요청 수신
  ↓
Rule 검증
  ↓
Query DTO 생성
  ↓
고객 기본정보 조회
  ↓
고객 미존재 판단
  ↓
고객등급 조회
  ↓
상품현황 조회
  ↓
개인정보 마스킹
  ↓
응답 DTO 조립
```

10.4 메서드 설계

| 메서드 | 입력 | 출력 | 설명 |
| --- | --- | --- | --- |
| selectSummary() | Request | Response | 전체 조회 흐름 |
| createQuery() | Request | Query | DAO 조회조건 생성 |
| assembleResponse() | Result 목록 | Response | 응답 조립 |
| validateCustomerExists() | Customer Result | void | 결과 없음 판단 |

10.5 구현 예시

```
@Service
@RequiredArgsConstructor
public class SvCustomerService {

    private final SvCustomerInquiryRule inquiryRule;
    private final SvCustomerDao customerDao;
    private final SvCustomerGradeDao gradeDao;
    private final SvCustomerProductDao productDao;
    private final SvCustomerMaskingService maskingService;

    public SvCustomerSummaryResponse selectSummary(
            SvCustomerSummaryRequest request) {

        inquiryRule.validateInquiry(request);

        SvCustomerSummaryQuery query =
            SvCustomerSummaryQuery.from(request);

        SvCustomerSummaryResult customer =
            customerDao.selectSummary(query)
                .orElseThrow(CustomerNotFoundException::new);

        SvCustomerGradeResult grade =
            gradeDao.selectGrade(query.customerNo(),
                                 query.baseDate())
                .orElse(null);

        List<SvCustomerProductResult> products =
            request.includeProducts()
                ? productDao.selectProducts(query.customerNo())
                : List.of();

        SvCustomerSummaryResult maskedCustomer =
            maskingService.maskCustomer(customer);

        List<SvCustomerProductResult> maskedProducts =
            maskingService.maskProducts(products);

        return SvCustomerSummaryResponse.of(
            maskedCustomer,
            grade,
            maskedProducts
        );
    }
}
```

10.6 Service 설계 판단

고객 기본정보가 없으면 거래를 결과 없음으로 종료한다.

고객등급이 없는 경우는 다음처럼 처리한다.

| 상황 | 처리 |
| --- | --- |
| 등급 데이터 없음 | grade=null 허용 |
| 등급 SQL 오류 | 전체 거래 시스템 오류 |
| 상품 없음 | 빈 목록 |
| 상품 SQL 오류 | 기본 정책은 전체 오류 |
| 부분 성공 정책 승인 | 별도 오류정보 포함 가능 |

11. Rule 설계

11.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 클래스명 | SvCustomerInquiryRule |
| 패키지 | com.nh.nsight.sv.customer.rule |
| 계층 | Rule |
| 의존성 | 인증 Context 조회 객체·순수 Utility |
| DB 접근 | 금지 |
| 외부 호출 | 금지 |

11.2 책임

- 조회조건 필수 검증
- 고객번호 형식 검증
- 고객명·지점 조합 검증
- 기준일 검증
- 인증 사용자·지점 문맥 존재 검증
- 요청 조회지점과 사용자 권한범위 검증
11.3 메서드 설계

| 메서드 | 입력 | 출력 | 설명 |
| --- | --- | --- | --- |
| validateInquiry() | Request | void | 전체 검증 |
| validateSearchCondition() | Request | void | 검색조건 |
| validateBaseDate() | LocalDate | void | 기준일 |
| validateBranchScope() | Request·Auth Context | void | 지점 권한 |

11.4 구현 예시

```
@Component
@RequiredArgsConstructor
public class SvCustomerInquiryRule {

    private final AuthenticationContextHolder authContextHolder;

    public void validateInquiry(
            SvCustomerSummaryRequest request) {

        validateSearchCondition(request);
        validateBaseDate(request.baseDate());
        validateBranchScope(request);
    }

    private void validateSearchCondition(
            SvCustomerSummaryRequest request) {

        boolean hasCustomerNo =
            hasText(request.customerNo());

        boolean hasNameAndBranch =
            hasText(request.customerName())
            && hasText(request.searchBranchId());

        if (!hasCustomerNo && !hasNameAndBranch) {
            throw new InvalidSearchConditionException();
        }
    }

    private void validateBaseDate(LocalDate baseDate) {
        if (baseDate == null || baseDate.isAfter(LocalDate.now())) {
            throw new InvalidBaseDateException();
        }
    }

    private void validateBranchScope(
            SvCustomerSummaryRequest request) {

        AuthenticationContext auth =
            authContextHolder.getRequired();

        if (!auth.canAccessBranch(request.searchBranchId())) {
            throw new BranchAccessDeniedException();
        }
    }
}
```

11.5 금지 예시

```
public void validateInquiry(Request request) {
    Customer customer =
        customerMapper.selectCustomer(request.customerNo());
}
```

Rule에서 DB를 조회하는 방식은 금지한다.

DB 조회 결과에 따른 판단이 필요하면 Service가 DAO를 호출한 뒤 Rule에 결과를 전달한다.

12. 개인정보 마스킹 Service 설계

12.1 기본정보

| 항목 | 내용 |
| --- | --- |
| 클래스명 | SvCustomerMaskingService |
| 역할 | 고객정보 응답 마스킹 |
| 의존성 | 인증 권한 Context·공통 Masking Utility |
| DB 접근 | 금지 |

12.2 메서드

| 메서드 | 입력 | 출력 |
| --- | --- | --- |
| maskCustomer() | 고객 Result | 마스킹 고객 Result |
| maskProducts() | 상품 목록 | 마스킹 상품 목록 |
| canViewOriginal() | 인증 Context | boolean |

12.3 처리 기준

| 필드 | 일반권한 | 원문권한 |
| --- | --- | --- |
| 고객명 | 홍*동 | 홍길동 |
| 전화번호 | 010-****-1234 | 원문 |
| 이메일 | h***@example.com | 원문 |
| 잔액 | 권한에 따른 제한 | 원문 |
| 주민번호 | 반환 금지 | 별도 화면 |

13. DAO 설계

13.1 공통 원칙

DAO는 Mapper 호출을 캡슐화한다.

DAO는 다음을 수행할 수 있다.

- Mapper 호출
- Mapper 결과의 Optional·빈 목록 변환
- 데이터 접근 예외 변환
- DB 조회를 위한 기술적 파라미터 처리
DAO는 다음을 수행하지 않는다.

- 업무 권한 판단
- 업무 오류 메시지 결정
- 응답 DTO 조립
- 여러 유스케이스 흐름 제어
- 트랜잭션 선언
13.2 SvCustomerDao

| 항목 | 내용 |
| --- | --- |
| 클래스명 | SvCustomerDao |
| Mapper | SvCustomerMapper |
| 책임 | 고객 기본정보 조회 |

```
@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

    private final SvCustomerMapper customerMapper;

    public Optional<SvCustomerSummaryResult> selectSummary(
            SvCustomerSummaryQuery query) {

        return Optional.ofNullable(
            customerMapper.selectSummary(query)
        );
    }
}
```

13.3 SvCustomerGradeDao

```
@Repository
@RequiredArgsConstructor
public class SvCustomerGradeDao {

    private final SvCustomerGradeMapper gradeMapper;

    public Optional<SvCustomerGradeResult> selectGrade(
            String customerNo,
            LocalDate baseDate) {

        return Optional.ofNullable(
            gradeMapper.selectGrade(customerNo, baseDate)
        );
    }
}
```

13.4 SvCustomerProductDao

```
@Repository
@RequiredArgsConstructor
public class SvCustomerProductDao {

    private final SvCustomerProductMapper productMapper;

    public List<SvCustomerProductResult> selectProducts(
            String customerNo) {

        List<SvCustomerProductResult> results =
            productMapper.selectProducts(customerNo);

        return results == null ? List.of() : results;
    }
}
```

14. Mapper 설계

14.1 Mapper Interface

```
@Mapper
public interface SvCustomerMapper {

    SvCustomerSummaryResult selectSummary(
        SvCustomerSummaryQuery query
    );
}
@Mapper
public interface SvCustomerGradeMapper {

    SvCustomerGradeResult selectGrade(
        @Param("customerNo") String customerNo,
        @Param("baseDate") LocalDate baseDate
    );
}
@Mapper
public interface SvCustomerProductMapper {

    List<SvCustomerProductResult> selectProducts(
        @Param("customerNo") String customerNo
    );
}
```

14.2 Mapper XML 정보

| Mapper | Namespace | SQL ID |
| --- | --- | --- |
| 고객요약 | com.nh.nsight.sv.customer.mapper.SvCustomerMapper | selectSummary |
| 고객등급 | com.nh.nsight.sv.customer.mapper.SvCustomerGradeMapper | selectGrade |
| 상품현황 | com.nh.nsight.sv.customer.mapper.SvCustomerProductMapper | selectProducts |

14.3 고객요약 SQL 예시

```
<select id="selectSummary"
        parameterType="SvCustomerSummaryQuery"
        resultType="SvCustomerSummaryResult"
        timeout="3">

    SELECT
        C.CUSTOMER_NO,
        C.CUSTOMER_NAME,
        C.CUSTOMER_TYPE_CODE,
        C.MANAGEMENT_BRANCH_ID,
        C.MOBILE_NO,
        C.EMAIL,
        C.MARKETING_CONSENT_YN,
        C.LAST_UPDATE_DTM
    FROM SV_CUSTOMER_SUMMARY C
    WHERE C.CUSTOMER_NO = #{customerNo}
      AND C.BASE_DATE = #{baseDate}

</select>
```

14.4 Mapper 금지사항

- SELECT *
- 문자열 치환 ${} 사용
- SQL 내 업무 권한 하드코딩
- 개인정보 불필요 조회
- SQL ID 중복
- Result Map 없는 복잡한 자동 매핑
- 과도한 동적 SQL
- Map 반환
- SQL 내부 오류코드 결정
15. DTO 설계

15.1 Request DTO

```
public record SvCustomerSummaryRequest(
    String customerNo,
    String customerName,
    String searchBranchId,
    LocalDate baseDate,
    boolean includeProducts
) {}
```

| 필드 | 유형 | 필수 | 설명 |
| --- | --- | --- | --- |
| customerNo | String | 조건 | 고객번호 |
| customerName | String | 조건 | 고객명 |
| searchBranchId | String | 조건 | 조회 대상 지점 |
| baseDate | LocalDate | 예 | 기준일 |
| includeProducts | boolean | 아니오 | 상품 조회 여부 |

15.2 Query DTO

```
public record SvCustomerSummaryQuery(
    String customerNo,
    String customerName,
    String branchId,
    LocalDate baseDate
) {
    public static SvCustomerSummaryQuery from(
            SvCustomerSummaryRequest request) {

        return new SvCustomerSummaryQuery(
            request.customerNo(),
            request.customerName(),
            request.searchBranchId(),
            request.baseDate()
        );
    }
}
```

15.3 Result DTO

```
public record SvCustomerSummaryResult(
    String customerNo,
    String customerName,
    String customerTypeCode,
    String managementBranchId,
    String mobileNo,
    String email,
    String marketingConsentYn,
    LocalDateTime lastUpdateDtm
) {}
```

15.4 Response DTO

```
public record SvCustomerSummaryResponse(
    Customer customer,
    Grade grade,
    List<Product> products,
    LocalDateTime dataAsOfDtm
) {
    public static SvCustomerSummaryResponse of(
            SvCustomerSummaryResult customer,
            SvCustomerGradeResult grade,
            List<SvCustomerProductResult> products) {

        return new SvCustomerSummaryResponse(
            Customer.from(customer),
            Grade.fromNullable(grade),
            products.stream()
                .map(Product::from)
                .toList(),
            LocalDateTime.now()
        );
    }
}
```

15.5 DTO 사용 범위

| DTO | Handler | Facade | Service | DAO | Mapper | UI 응답 |
| --- | --- | --- | --- | --- | --- | --- |
| Request DTO | O | O | O | X | X | 입력 |
| Query DTO | X | X | O | O | O | X |
| Result DTO | X | X | O | O | O | X |
| Response DTO | O | O | O | X | X | 출력 |

16. 프로그램 호출 흐름

```
SvCustomerHandler.handle()
  ↓
SvCustomerHandler.handleSelectSummary()
  ↓
SvCustomerFacade.selectSummary()
  ↓
SvCustomerService.selectSummary()
  ├─ SvCustomerInquiryRule.validateInquiry()
  ├─ SvCustomerDao.selectSummary()
  │    └─ SvCustomerMapper.selectSummary()
  ├─ SvCustomerGradeDao.selectGrade()
  │    └─ SvCustomerGradeMapper.selectGrade()
  ├─ SvCustomerProductDao.selectProducts()
  │    └─ SvCustomerProductMapper.selectProducts()
  ├─ SvCustomerMaskingService.maskCustomer()
  └─ SvCustomerSummaryResponse.of()
```

16.1 시퀀스

```
Handler          Facade          Service          Rule          DAO/Mapper
  │                │                │               │               │
  │ selectSummary  │                │               │               │
  ├───────────────▶│                │               │               │
  │                │ selectSummary  │               │               │
  │                ├───────────────▶│               │               │
  │                │                │ validate      │               │
  │                │                ├──────────────▶│               │
  │                │                │◀──────────────┤               │
  │                │                │ selectCustomer               │
  │                │                ├──────────────────────────────▶│
  │                │                │◀──────────────────────────────┤
  │                │                │ selectGrade                  │
  │                │                ├──────────────────────────────▶│
  │                │                │◀──────────────────────────────┤
  │                │                │ selectProducts               │
  │                │                ├──────────────────────────────▶│
  │                │                │◀──────────────────────────────┤
  │                │                │ assembleResponse             │
  │                │◀───────────────┤                              │
  │◀───────────────┤                                               │
```

17. 예외 처리 설계

17.1 예외 분류

| 계층 | 예외 | 변환 |
| --- | --- | --- |
| Handler | 요청 변환 실패 | 입력 오류 |
| Rule | 검증 실패 | BusinessException |
| Service | 고객 미존재 | CustomerNotFoundException |
| DAO | DataAccessException | TcfDataAccessException |
| Mapper | SQLException | Spring/MyBatis 예외 |
| Facade | RuntimeException | Rollback |
| ETF | 업무·시스템 응답 변환 | 표준 결과 |

17.2 예외 코드 매핑

| 예외 클래스 | 오류코드 |
| --- | --- |
| InvalidSearchConditionException | SV-CUS-001 |
| CustomerNotFoundException | SV-CUS-002 |
| BranchAccessDeniedException | SV-CUS-004 |
| InvalidBaseDateException | SV-CUS-005 |
| TcfDataAccessException | TCF-DB-001 |
| ServiceNotSupportedException | TCF-SVC-404 |

17.3 예외 처리 원칙

- Mapper 예외를 UI까지 그대로 전달하지 않는다.
- Service에서 예외를 무조건 잡아 성공으로 바꾸지 않는다.
- 업무 오류와 시스템 오류를 구분한다.
- 예외 로그에는 GUID·TraceId·ServiceId를 포함한다.
- 개인정보와 SQL Parameter는 로그에 남기지 않는다.
18. 로깅 설계

18.1 프로그램별 로그

| 계층 | 로그 기준 |
| --- | --- |
| Handler | ServiceId 진입·변환 오류 |
| Facade | 유스케이스 시작·종료 |
| Service | 주요 업무 단계 |
| Rule | 검증 실패 사유코드 |
| DAO | Mapper ID·소요시간 |
| Mapper Interceptor | SQL ID·실행시간 |
| Exception Handler | 오류코드·예외 유형 |

18.2 로그 예시

```
INFO
guid=G...
traceId=T...
serviceId=SV.Customer.selectSummary
class=SvCustomerService
method=selectSummary
step=SELECT_CUSTOMER
elapsedMs=125
result=SUCCESS
```

18.3 금지 로그

```
customerName=홍길동
mobileNo=01012341234
email=hong@example.com
sqlParameter={...}
responseBody={전체 응답}
```

19. 성능 설계

| 프로그램 | 목표 |
| --- | --- |
| Handler 변환 | 10ms 이하 |
| Rule 검증 | 10ms 이하 |
| 고객요약 DAO | 300ms 이하 |
| 등급 DAO | 200ms 이하 |
| 상품 DAO | 300ms 이하 |
| 마스킹·조립 | 50ms 이하 |
| 전체 Service | 1초 내 목표 |
| 전체 거래 p95 | 3초 이하 |

19.1 성능 유의사항

- 동일 고객을 여러 SQL에서 반복 조회하지 않는다.
- 코드명은 공통코드 Cache를 활용한다.
- 상품 건수는 최대 100건으로 제한한다.
- N+1 조회를 금지한다.
- 고객번호와 기준일 인덱스를 검토한다.
- 고객명 검색은 별도 SQL과 실행계획을 적용한다.
- Mapper 호출 전 DB Connection 획득시간을 측정한다.
20. 보안 설계

| 통제 | 구현 위치 |
| --- | --- |
| 인증 사용자 확보 | Filter·STF |
| 기능권한 검증 | STF |
| 지점 데이터권한 | Rule·Service |
| 개인정보 마스킹 | Masking Service |
| SQL Injection 방지 | MyBatis 바인딩 |
| 개인정보 로그 방지 | 공통 Log Masker |
| 응답 최소화 | Response DTO |
| 감사로그 | ETF·Audit Service |

Handler나 Service에서 클라이언트가 보낸 userId를 신뢰하지 않는다.

검증된 AuthenticationContext를 사용한다.

21. 책임 경계와 RACI

| 활동 | 업무개발팀 | 프레임워크팀 | 아키텍처팀 | DBA | 보안팀 | 테스트팀 |
| --- | --- | --- | --- | --- | --- | --- |
| Handler 구현 | A/R | C | C | I | I | C |
| Facade·Service 구현 | A/R | I | C | I | I | C |
| Rule 구현 | A/R | I | C | I | C | C |
| DAO·Mapper 구현 | R | I | C | A/C | I | C |
| DTO 정의 | R | C | A | I | C | C |
| 트랜잭션 기준 | C | C | A/R | C | I | C |
| 개인정보 마스킹 | R | C | C | I | A | C |
| 단위 테스트 | R | C | C | I | I | A |
| 계층 검증 | C | R | A | I | I | C |

22. 정상 예시

```
SvCustomerHandler
→ Request DTO 변환
→ SvCustomerFacade 호출
→ 읽기전용 트랜잭션 시작
→ SvCustomerService
→ Rule 검증
→ DAO·Mapper 조회
→ Masking Service
→ Response DTO 조립
→ Facade 종료
→ ETF 표준 응답
```

23. 금지 예시

23.1 Handler에서 Mapper 호출

```
public Object handle(Object body) {
    return customerMapper.selectSummary(body);
}
```

금지한다.

23.2 Service에서 표준 응답 생성

```
public StandardResponse selectSummary(Request request) {
    return StandardResponse.success(...);
}
```

ETF 책임이므로 금지한다.

23.3 DAO에서 업무 오류 판단

```
public Customer selectSummary(Query query) {
    Customer result = mapper.selectSummary(query);

    if (result == null) {
        throw new CustomerNotFoundException();
    }

    return result;
}
```

고객 미존재 업무 판단은 Service가 수행한다.

23.4 Entity 직접 응답

```
return customerMapper.selectSummary(query);
```

DB Result를 화면에 직접 반환하지 않는다.

23.5 Facade 외 트랜잭션

```
@Service
@Transactional
public class SvCustomerService {
}
```

클래스 전체 트랜잭션 선언은 원칙적으로 금지한다.

24. 자동검증 및 품질 Gate

| 검증 ID | 검증 내용 | 기준 |
| --- | --- | --- |
| PGM-001 | Handler ServiceId 중복 | 중복 시 실패 |
| PGM-002 | Handler → DAO 의존 | 금지 |
| PGM-003 | Handler → Mapper 의존 | 금지 |
| PGM-004 | Facade → Mapper 의존 | 금지 |
| PGM-005 | Rule → DAO·Mapper 의존 | 금지 |
| PGM-006 | DAO → Service 의존 | 금지 |
| PGM-007 | Facade 외 @Transactional | 원칙적 실패 |
| PGM-008 | Mapper의 ${} 사용 | 금지 |
| PGM-009 | SELECT * 사용 | 금지 |
| PGM-010 | DTO의 Map 사용 | 제한 |
| PGM-011 | Response DTO 개인정보 원문 | 보안 검증 |
| PGM-012 | 테스트 없는 프로그램 | 배포 차단 |
| PGM-013 | ServiceId–Handler 불일치 | 배포 차단 |
| PGM-014 | DAO Method–SQL ID 미연결 | 설계 Gate 실패 |
| PGM-015 | 순환 의존성 | 빌드 실패 |

ArchUnit 예시:

```
@ArchTest
static final ArchRule handlers_must_not_access_dao =
    noClasses()
        .that().resideInAPackage("..handler..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..dao..", "..mapper..");

@ArchTest
static final ArchRule rules_must_be_pure =
    noClasses()
        .that().resideInAPackage("..rule..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..dao..", "..mapper..");
```

25. 테스트 시나리오

25.1 Handler 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| UT-HDL-001 | 정상 Body 변환 | Facade 호출 |
| UT-HDL-002 | 잘못된 Body | 입력 오류 |
| UT-HDL-003 | 미지원 ServiceId | 서비스 미지원 |
| UT-HDL-004 | Facade 업무 예외 | 예외 전달 |

25.2 Rule 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| UT-RUL-001 | 고객번호 존재 | 정상 |
| UT-RUL-002 | 고객명+지점 존재 | 정상 |
| UT-RUL-003 | 조건 모두 없음 | SV-CUS-001 |
| UT-RUL-004 | 미래 기준일 | SV-CUS-005 |
| UT-RUL-005 | 지점 권한 없음 | SV-CUS-004 |

25.3 Service 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| UT-SVC-001 | 전체 데이터 정상 | 응답 조립 |
| UT-SVC-002 | 고객 미존재 | SV-CUS-002 |
| UT-SVC-003 | 등급 없음 | grade=null |
| UT-SVC-004 | 상품 없음 | 빈 목록 |
| UT-SVC-005 | 개인정보 일반권한 | 마스킹 |
| UT-SVC-006 | DAO 예외 | 시스템 예외 전달 |

25.4 DAO·Mapper 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| UT-DAO-001 | 고객 1건 조회 | Optional 데이터 |
| UT-DAO-002 | 고객 없음 | Optional.empty |
| UT-MAP-001 | 고객번호 인덱스 조회 | 정상 실행계획 |
| UT-MAP-002 | 상품 100건 | 최대 건수 준수 |
| UT-MAP-003 | SQL Timeout | 3초 내 중단 |

25.5 통합 테스트

```
표준 요청
→ TCF
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ DB
→ ETF 응답
```

통합 테스트에서는 ServiceId 등록, 트랜잭션, SQL, 마스킹, 오류코드와 거래로그를 함께 검증한다.

26. 프로그램 설계 체크리스트

| No. | 점검 항목 | 확인 |
| --- | --- | --- |
| 1 | ServiceId와 Handler가 연결되었는가 | □ |
| 2 | Handler가 Facade만 호출하는가 | □ |
| 3 | Facade에 트랜잭션이 선언되었는가 | □ |
| 4 | Service가 업무 흐름을 담당하는가 | □ |
| 5 | Rule이 순수 검증만 수행하는가 | □ |
| 6 | DAO가 Mapper 호출만 담당하는가 | □ |
| 7 | Mapper SQL ID가 메서드와 연결되는가 | □ |
| 8 | Request·Query·Result·Response DTO가 분리되었는가 | □ |
| 9 | DB Result를 화면에 직접 반환하지 않는가 | □ |
| 10 | 사용자 ID를 요청값에서 신뢰하지 않는가 | □ |
| 11 | 개인정보 마스킹이 서버에서 수행되는가 | □ |
| 12 | 오류코드와 예외 클래스가 연결되었는가 | □ |
| 13 | Timeout과 SQL Timeout이 적용되었는가 | □ |
| 14 | 로그에 개인정보가 포함되지 않는가 | □ |
| 15 | 계층별 단위 테스트가 있는가 | □ |
| 16 | ArchUnit 계층검증이 적용되었는가 | □ |
| 17 | ServiceId와 OM Catalog가 일치하는가 | □ |
| 18 | 프로그램과 SQL·DB 객체 추적이 가능한가 | □ |
| 19 | 성능 목표와 최대 건수가 정의되었는가 | □ |
| 20 | 변경·폐기 절차가 정의되었는가 | □ |

27. 변경·호환성·폐기 관리

27.1 메서드 변경

공개 메서드의 파라미터나 반환형을 변경할 때는 호출 프로그램과 테스트를 함께 변경한다.

```
Facade Method
→ Service Method
→ DTO
→ Handler
→ 테스트
→ 거래설계서
```

27.2 DTO 변경

| 변경 | 영향 |
| --- | --- |
| 선택 필드 추가 | 일반적으로 호환 가능 |
| 필수 필드 추가 | UI·Handler 영향 |
| 필드 삭제 | 호환성 위반 |
| 자료형 변경 | Mapper·UI 영향 |
| 마스킹 변경 | 보안·화면 영향 |

27.3 DAO·SQL 변경

```
SQL ID 변경
→ Mapper Method
→ DAO Method
→ Service
→ ServiceId
→ 화면 이벤트
→ 테스트
```

SQL ID는 운영 Slow SQL 추적에 사용되므로 사전 영향 분석 없이 변경하지 않는다.

27.4 프로그램 폐기

```
화면 호출 제거
→ ServiceId 호출 0건 확인
→ Handler 등록 제거
→ Facade·Service 사용처 확인
→ DAO·Mapper 재사용 확인
→ OM 정책 폐기
→ 테스트 제거
→ 프로그램 삭제
```

DAO·Mapper가 다른 ServiceId에서 재사용되는 경우에는 삭제하지 않는다.

28. 시사점

28.1 핵심 아키텍처 판단

프로그램 설계의 핵심은 클래스를 많이 나누는 것이 아니라 각 클래스의 변경 이유를 하나로 제한하는 것이다.

```
Handler가 바뀌는 이유
= 거래 진입 방식 변경

Facade가 바뀌는 이유
= 유스케이스·트랜잭션 변경

Service가 바뀌는 이유
= 업무 처리 변경

Rule이 바뀌는 이유
= 업무 규칙 변경

DAO·Mapper가 바뀌는 이유
= 데이터 접근·SQL 변경
```

28.2 주요 위험

- Handler가 업무 Service 역할까지 수행하는 경우
- Facade가 단순 전달 클래스에 그치는 경우
- Service가 지나치게 비대해지는 경우
- Rule이 DB를 조회하는 경우
- DAO가 업무 오류를 판단하는 경우
- DTO 하나를 모든 계층에서 공용하는 경우
- 트랜잭션이 여러 계층에 중복 선언되는 경우
- SQL ID와 DAO Method가 연결되지 않는 경우
28.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | ServiceId별 Handler·Facade·Service 목록 확정 |
| 2 | 클래스별 책임과 금지 책임 정의 |
| 3 | DTO 유형 분리 |
| 4 | 트랜잭션 Facade 집중 |
| 5 | Rule 순수성 확보 |
| 6 | DAO·Mapper·SQL 추적성 확보 |
| 7 | ArchUnit 계층 검증 적용 |
| 8 | 프로그램별 테스트 자동화 |

28.4 중장기 발전 방향

소스 분석을 통해 다음 정보를 자동 생성하는 구조로 발전해야 한다.

```
Handler의 ServiceId
+ Java 의존관계
+ @Transactional 위치
+ Mapper Interface
+ Mapper XML
+ SQL ID
+ 테스트 결과
= 자동 프로그램 설계서
```

29. 마무리말

SV.Customer.selectSummary 프로그램은 하나의 클래스가 아니라 다음 프로그램 집합으로 구현된다.

```
SvCustomerHandler
→ SvCustomerFacade
→ SvCustomerService
→ SvCustomerInquiryRule
→ SvCustomerDao
→ SvCustomerMapper
```

각 프로그램의 책임과 호출 방향이 명확해야 화면 이벤트, 거래, SQL과 운영로그를 안정적으로 연결할 수 있다.

```
화면 ID로 사용자 기능을 찾고,
ServiceId로 거래를 찾고,
Handler로 프로그램 진입점을 찾고,
DAO·Mapper로 SQL을 찾고,
GUID·TraceId로 실제 장애를 찾는다.
```

모든 NSIGHT 업무 프로그램은 본 설계서 형식을 기준으로 ServiceId 단위 프로그램 집합을 정의하고, 화면설계서·거래설계서·SQL설계서·테스트설계서와 연결하여 관리한다.

