# NSIGHT TCF 개발자 핵심 지식 상세판

> 목적: 신규 개발자가 NSIGHT TCF의 구조를 이해하고 거래 하나를 설계·구현·테스트·배포·장애분석할 수 있게 하는 실무 문서  
> 조사 범위: 지정 폴더의 Markdown·텍스트·설정·Java·XML·스크립트와 현재 저장소 설정  
> 제외 범위: `.doc`, `.docx`, `.docm`, `.dot`, `.dotx` 등 Word 파일은 열거나 추출하지 않음  
> 기준 우선순위: 현재 소스/Gradle → 루트·모듈 README → 상세 Markdown → 집필본·샘플  
> 기준일: 2026-07-25

## 1. 이 문서의 학습 목표

이 문서를 읽은 개발자는 다음 질문에 답할 수 있어야 한다.

1. 요청이 `/sv/online`에 도착한 뒤 어떤 검증을 거쳐 Handler가 실행되는가?
2. URL, ServiceId, 거래코드, GUID는 각각 무엇을 식별하는가?
3. Handler, Facade, Service, Rule, DAO, Mapper의 책임은 어떻게 다른가?
4. 새 거래를 만들 때 코드 외에 OM에 무엇을 등록해야 하는가?
5. Online, Transaction, DB Query Timeout은 어떤 관계인가?
6. “등록되지 않은 ServiceId”, 거래 차단, DB 지연을 어떤 순서로 진단하는가?
7. bootRun과 통합 Tomcat 검증은 무엇이 다른가?

## 2. 먼저 기억할 핵심

- NSIGHT는 URL 중심 REST 묶음이 아니라 **ServiceId 중심 거래 처리 플랫폼**이다.
- 요청은 `header + body` 표준 전문으로 들어온다.
- 공통 흐름은 `STF → Dispatcher/Handler → ETF`다.
- STF는 Header, 세션, 인증, 권한, 거래통제, Timeout, 멱등성, 거래로그를 처리한다.
- Dispatcher는 `header.serviceId`로 `TransactionHandler`를 찾는다.
- 업무 흐름은 `Handler → Facade → Service → Rule → DAO/Mapper`다.
- ETF는 성공·업무실패·시스템실패를 표준 응답으로 만들고 거래로그·감사·메트릭을 마감한다.
- ServiceId와 Handler만 추가해서 끝나는 것이 아니다. Catalog, 통제, 권한, Timeout, 오류코드가 함께 맞아야 한다.
- 장애 추적의 연결키는 `GUID/TraceId + ServiceId + TransactionCode + SQL ID`다.
- 현재 루트 빌드는 JDK 21과 Spring Boot BOM 3.3.5를 사용한다.

## 3. TCF를 사용하는 이유

일반 Spring MVC는 URL이 Controller 메서드를 선택하고 Controller가 응답을 만든다. 금융 정보계의 수많은 거래에 같은 운영 기준을 적용하면 다음 문제가 생긴다.

- Controller마다 Header·권한 검증 방식이 달라진다.
- 거래를 URL, 클래스명, 화면명 중 무엇으로 추적해야 할지 불명확하다.
- 거래 차단이나 Timeout 변경을 위해 재배포하게 된다.
- 업무 개발자가 예외 응답을 제각각 만든다.
- Timeout 후 이미 처리된 변경 요청을 재시도해 중복 처리될 수 있다.
- 사용자 요청, 업무 프로그램, SQL, 감사로그를 연결하기 어렵다.

TCF는 모든 온라인 거래를 같은 파이프라인으로 통과시켜 이 문제를 해결한다.

```text
HTTP 경로
  어느 업무 WAR로 보낼 것인가

ServiceId
  업무 WAR 안에서 어떤 기능을 실행할 것인가

거래코드
  운영·감사·통제 관점에서 어떤 거래인가

GUID/TraceId
  한 번의 실행을 여러 시스템과 로그에서 어떻게 연결할 것인가
```

“API 하나 개발”은 다음 전체 연결을 만든다는 뜻이다.

```text
업무 요구사항
→ 업무코드 / ServiceId / 거래코드
→ 표준 요청·응답
→ Handler 이하 업무 계층
→ DB·외부 연동
→ 오류코드
→ 권한·거래통제·Timeout·멱등성
→ 거래로그·감사·메트릭
→ 테스트·WAR·배포·Rollback
```

## 4. 전체 아키텍처

```text
사용자 / Web / 외부 API
  ↓
GSLB → L4 → Apache HTTPD
  ↓
Gateway 또는 업무 WAR
  ↓
POST /{businessCode}/online
  ↓
OnlineTransactionController / TcfGateway
  ↓
TCF.process()
  ├─ STF.preProcess()
  ├─ OnlineTransactionTimeoutExecutor
  │    └─ TransactionDispatcher
  │         └─ TransactionHandler
  │              └─ Facade → Service → Rule → DAO/Mapper
  └─ ETF.success / businessFail / systemError
  ↓
StandardResponse
```

배포 관점:

- 업무별 독립 WAR가 각 Context를 가진다.
- 개발 시 `bootRun`으로 모듈별 별도 JVM과 포트를 쓴다.
- 로컬 통합 검증은 `ztomcat`의 8080에 여러 WAR를 올린다.
- 확장 구성은 Apache sticky session과 Tomcat Cluster를 사용할 수 있다.
- WAR의 `WEB-INF/lib`에 필요한 `tcf-*` JAR를 포함한다.

## 5. 모듈 구성과 의존 방향

| 구분 | 모듈 | 핵심 책임 |
|---|---|---|
| 유틸 | `tcf-util` | 문자열, 날짜, 마스킹 |
| 거래 엔진 | `tcf-core` | 전문, STF/TCF/ETF, Dispatcher, Context, Timeout |
| Web | `tcf-web` | `/online`, Controller, Filter, WAR bootstrap |
| 연동 | `tcf-eai` | 표준 전문 기반 업무 간 HTTP/JSON 호출 |
| Cache | `tcf-cache` | 공통코드와 정책 Cache |
| OM | `tcf-om` | 사용자·권한·ServiceId·통제·Timeout·오류·파일 |
| 용량/환경 | `tcf-oc` | 용량 산정과 환경설정 |
| 인증 관문 | `tcf-gateway`, `tcf-jwt` | 라우팅·검증, 토큰 발급·JWKS |
| 운영지원 | `tcf-batch`, `tcf-ui`, `tcf-uj` | 배치, 관리자/테스트 UI |
| 업무 | `ic/pc/ms/sv/pd/eb/ep/ss/mg-service` | 업무 거래 구현 |
| 통합 실행 | `ztomcat` | 여러 WAR를 8080에서 검증 |

`om-service`는 레거시이며 현재는 `tcf-om`을 기준으로 한다.

```text
tcf-util
  → tcf-core
    → tcf-web
      → tcf-cache / tcf-eai
        → 업무 WAR / OM / Batch / Gateway / JWT
```

금지할 의존:

- 공통 모듈이 업무 모듈 참조
- 업무 WAR가 다른 업무 WAR의 Service 직접 참조
- 업무가 Gateway/JWT 구현체 직접 참조
- `tcf-util`에 Spring Web, DB, 업무 규칙 추가

업무 간 호출은 표준 전문 기반 Client로 수행해야 배포 독립성과 통제·추적이 유지된다.

## 6. 요청 처리 순서

### 6.1 TCF

현재 `TCF.process()`의 순서:

1. 클라이언트 Header 복사본 생성
2. 요청 진입 로그
3. STF 전처리
4. 런타임 Hook 거래 시작
5. Online Timeout 안에서 Dispatcher 실행
6. 성공 시 ETF 성공 처리
7. `BusinessException`이면 업무실패 처리
8. 기타 예외가 Timeout인지 판별
9. Timeout은 표준 Timeout 오류로 변환
10. 나머지는 시스템오류 처리
11. 모든 Thread Context와 MDC 정리

정리 대상:

```text
TransactionContextHolder
AuthenticationContextHolder
TimeoutContextHolder
SLF4J MDC
```

WAS Thread는 재사용된다. Context를 지우지 않으면 이전 사용자의 GUID나 인증정보가 다음 요청에 섞일 수 있다.

### 6.2 STF

현재 실제 순서:

```text
1. StandardHeaderValidator
2. GUID/TraceId 생성
3. TransactionContext 생성·저장
4. MDC 등록
5. SessionValidator
6. AuthenticationContextValidator
7. AuthorizationValidator
8. TransactionControlService
9. TimeoutPolicyService
10. IdempotencyChecker
11. TransactionLogService.start
```

의미:

- 구조가 잘못된 요청을 먼저 거절한다.
- GUID를 먼저 확보해 모든 실패를 추적한다.
- 인증·권한을 통과한 요청만 업무 코드에 도달시킨다.
- 차단 거래를 실행 전에 막는다.
- 실행 전에 Timeout 예산을 Context에 적용한다.
- 중복 키를 처리 중 상태로 표시하고 거래로그를 시작한다.

### 6.3 Dispatcher

기동 시 모든 `TransactionHandler.serviceIds()`를 읽어 다음 Map을 만든다.

```text
Map<ServiceId, TransactionHandler>
```

요청 시:

```text
header.serviceId
  → 없음: INVALID_HEADER
  → Map 조회 실패: SERVICE_NOT_FOUND
  → 성공: handler.handle(request, context)
```

Handler 클래스가 있어도 실행되지 않는 경우:

- Spring Bean 등록이 안 됨
- Package Scan 범위 밖
- `serviceIds()` 문자열 불일치
- 다른 WAR에 배포
- 이전 WAR 버전 실행

OM Catalog는 운영정책의 기준이고 Dispatcher Map은 Java 실행의 기준이다. 둘 다 맞아야 한다.

### 6.4 ETF

성공:

```text
멱등성 SUCCESS
→ 거래로그 S0000 종료
→ 감사로그
→ 메트릭
→ StandardResponse.success
```

업무실패:

```text
멱등성 FAIL
→ 거래로그 E0001 + 상세 오류코드
→ 감사로그
→ 메트릭
→ StandardResponse.fail
```

시스템실패도 거래 상태를 닫고 외부에는 내부 StackTrace 대신 표준 시스템 오류를 준다. Handler가 표준 응답이나 거래로그를 직접 조립하면 안 된다.

## 7. StandardHeader 상세

| 필드 | 의미 | 주의사항 |
|---|---|---|
| `systemId` | 호출 시스템 | 서버 기본값 가능 |
| `businessCode` | 대상 업무 | Context·ServiceId Prefix와 일치 |
| `serviceId` | 실행 기능 | Dispatcher Key |
| `serviceName` | 서비스명 | 통제 정책 필수값 가능 |
| `transactionCode` | 운영 거래 | Catalog·통제·로그와 일치 |
| `processingType` | 처리 유형 | 대문자 정규화 |
| `guid` | End-to-End 추적 | 하위 시스템 전달 |
| `traceId` | 내부 추적 | Span 정책과 구분 |
| `channelId` | 호출 채널 | 통제 조건 |
| `userId` | 사용자 | 인증 Context에서 확정 |
| `branchId` | 지점 | 데이터 권한 |
| `centerId` | 센터 | 장애 분석 |
| `requestTime` | 요청 시각 | ISO Offset DateTime |
| `clientIp` | 원 IP | Gateway에서 신뢰값 구성 |
| `idempotencyKey` | 중복 요청 Key | 변경 거래에서 중요 |

현재 `normalize()`는 기본 `systemId`, 현재 `requestTime`, 대문자 `businessCode/processingType`을 보완한다.

브라우저가 보낸 `userId`, `branchId`, `clientIp`를 그대로 신뢰하면 안 된다. 신원 정보는 인증된 Gateway 또는 Session Context가 확정해야 한다.

논리 정합:

```text
businessCode = ServiceId 첫 구간
businessCode = 요청 Context의 업무
transactionCode 첫 구간 = businessCode
요청 ServiceId = Catalog ServiceId = Handler serviceIds()
요청 거래코드 = Catalog·통제 거래코드
processingType = 거래코드 유형과 호환
```

## 8. 표준 전문 예제

### 요청

```json
{
  "header": {
    "systemId": "NSIGHT-WEB",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "serviceName": "고객요약조회",
    "transactionCode": "SV-INQ-0002",
    "processingType": "INQUIRY",
    "guid": "",
    "traceId": "",
    "channelId": "WEB",
    "userId": "authenticated-user",
    "branchId": "001",
    "requestTime": "2026-07-25T13:00:00+09:00"
  },
  "body": {
    "customerNo": "CUST00000001",
    "baseDate": "20260725"
  }
}
```

### 성공

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0002",
    "guid": "generated-guid",
    "traceId": "generated-trace"
  },
  "result": {
    "resultCode": "S0000",
    "resultMessage": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST00000001",
    "customerGrade": "GOLD"
  }
}
```

### 업무실패

```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "guid": "same-guid"
  },
  "result": {
    "resultCode": "E0001",
    "resultMessage": "처리 중 오류가 발생했습니다.",
    "errorCode": "E-SV-BIZ-0001",
    "errorMessage": "조회된 고객 정보가 없습니다.",
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-07-25T13:00:01+09:00"
  },
  "body": null
}
```

`resultCode`는 큰 성공/실패 분류이고 `errorCode`는 원인·조치·감사·모니터링용 세부 식별자다.

## 9. 식별자와 명명

| 항목 | 규칙 | 예 |
|---|---|---|
| 업무코드 | 대문자 2~3자리 | `SV` |
| Context | 업무코드 소문자 | `/sv` |
| WAR | Context 이름 | `sv.war` |
| Package | 현재 소스 기준 | `com.nh.nsight.marketing.sv` |
| Endpoint | 공통 온라인 진입 | `POST /sv/online` |

ServiceId:

```text
{BusinessCode}.{Domain}.{action}

SV.Customer.selectSummary
OM.User.inquiry
MG.Message.send
```

- BusinessCode는 대문자
- Domain은 명사와 PascalCase
- Action은 동사와 camelCase
- `get`, `proc`, `manage` 같은 모호한 표현과 약어 남용 금지

거래코드:

```text
{업무코드}-{처리유형}-{4자리 일련번호}
```

| 유형 | 의미 | 예 |
|---|---|---|
| `INQ` | 조회 | `SV-INQ-0002` |
| `REG` | 등록 | `OM-REG-0001` |
| `UPD` | 수정 | `OM-UPD-0001` |
| `DEL` | 삭제 | `OM-DEL-0001` |
| `EXE` | 실행 | `BT-EXE-0001` |
| `UPL`/`DWN` | 파일 | `UD-DWN-0001` |
| `APR` | 승인 | `OM-APR-0001` |
| `SND` | 발송 | `MG-SND-0001` |

ServiceId는 실행 기능, 거래코드는 운영 거래다.

## 10. 업무 6계층

```text
com.nh.nsight.marketing.{business}
├─ entry/
│  ├─ handler/        ServiceId 진입점
│  ├─ facade/         유스케이스·트랜잭션
│  └─ web/            별도 REST 진입
├─ application/
│  ├─ service/        처리 절차
│  ├─ rule/           검증·판단·계산
│  └─ scheduler/      스케줄
├─ persistence/
│  ├─ dao/            Mapper 호출 경계
│  └─ mapper/         MyBatis
├─ client/            외부·타업무 연동
├─ config/            Spring 설정
└─ support/           내부 도우미
```

| 계층 | 할 일 | 하지 않을 일 |
|---|---|---|
| Handler | ServiceId 등록, Body 전달, Facade 호출 | SQL, 복잡한 규칙, 응답 조립 |
| Facade | DTO 변환, 유스케이스, 트랜잭션 | SQL 직접 실행 |
| Service | Rule/DAO/Client 호출 순서 | HTTP 응답 생성 |
| Rule | 검증, 상태 전이, 계산 | Mapper 직접 호출 남발 |
| DAO | Mapper 호출과 영속성 경계 | 업무 판단 |
| Mapper/XML | SQL과 결과 매핑 | 화면·업무 정책 |

## 11. SV 고객요약 구현 해설

Handler:

```java
@Component
public class SvCustomerHandler implements TransactionHandler {
    private static final String SELECT_SUMMARY =
        "SV.Customer.selectSummary";

    @Override
    public Collection<String> serviceIds() {
        return List.of(SELECT_SUMMARY);
    }

    @Override
    public Object doHandle(
            StandardRequest<Map<String, Object>> request,
            TransactionContext context) {
        return switch (context.getHeader().getServiceId()) {
            case SELECT_SUMMARY ->
                facade.selectCustomerSummary(request.getBody(), context);
            default ->
                throw new BusinessException(
                    ErrorCode.SERVICE_NOT_FOUND,
                    "미지원 serviceId");
        };
    }
}
```

- `@Component`라서 Spring과 Dispatcher가 발견한다.
- `serviceIds()`가 Dispatcher Map의 Key다.
- 같은 도메인의 여러 ServiceId를 한 Handler에 묶을 수 있다.
- 실제 업무 처리는 Facade로 위임한다.

Facade:

```java
@Transactional(readOnly = true, timeout = 3)
public Map<String, Object> selectCustomerSummary(
        Map<String, Object> body,
        TransactionContext context) {
    CustomerSummaryRequest request =
        CustomerSummaryRequest.fromMap(body);
    return service.selectCustomerSummary(request, context).toMap();
}
```

외부 Map을 DTO로 바꾸고 트랜잭션 경계를 연다. 고정 Annotation Timeout과 OM 동적 정책의 우선순위는 설정으로 확인해야 한다.

Service:

```java
public CustomerSummaryResponse selectCustomerSummary(
        CustomerSummaryRequest request,
        TransactionContext context) {
    var criteria = rule.buildSummaryCriteria(request);
    CustomerSummaryRow row = dao.selectCustomerSummary(criteria);
    rule.validateSummaryResult(row);
    return CustomerSummaryResponse.of(context, row);
}
```

“조건 생성 → 조회 → 결과 검증 → 응답 변환” 절차가 드러난다.

현재 Rule:

- 고객번호 필수: `E-SV-VAL-0001`
- 최대 20자: `E-SV-VAL-0002`
- 결과 없음: `E-SV-BIZ-0001`

DAO와 Mapper:

```java
public CustomerSummaryRow selectCustomerSummary(
        CustomerSummaryCriteria criteria) {
    return mapper.selectCustomerSummary(criteria);
}
```

```java
@Mapper
public interface SvCustomerMapper {
    CustomerSummaryRow selectCustomerSummary(
        CustomerSummaryCriteria criteria);
}
```

XML:

```xml
<mapper namespace=
 "com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">
  <select id="selectCustomerSummary"
          parameterType="..."
          resultType="..."
          timeout="3">
    ...
  </select>
</mapper>
```

Java 메서드명, XML `id`, namespace, 파라미터, 결과 타입 중 하나라도 다르면 오류가 난다.

## 12. 신규 거래 구현 순서

1. 거래 성격을 조회/등록/변경/삭제/승인/발송으로 구분한다.
2. 업무코드, ServiceId, 거래코드를 확정한다.
3. Request/Response와 Validation 표를 만든다.
4. 기능·데이터 권한, 감사, 멱등성 요구를 정한다.
5. Online/TX/DB/연동 Timeout 예산을 정한다.
6. Handler `serviceIds()`를 등록하고 Facade로 위임한다.
7. Facade에서 DTO 변환과 트랜잭션 경계를 둔다.
8. Service에서 유스케이스를 조립한다.
9. Rule에 형식·업무 규칙·상태 전이를 둔다.
10. DAO/Mapper와 제한된 SQL을 구현한다.
11. 표준 업무/시스템 오류코드를 연결한다.
12. OM Catalog, 통제, 권한, Timeout, 오류를 등록한다.
13. 정상·검증·권한·DB·Timeout·중복 테스트를 작성한다.
14. bootRun과 통합 Tomcat에서 Smoke Test한다.

## 13. 조회·등록·변경·삭제 설계

조회:

- `readOnly = true`
- 목록 Pagination과 최대 건수
- 결과 없음의 의미 정의
- 실행계획과 Query Timeout
- Cache 최신성·TTL·Evict

등록:

- 중복 판단 Key와 `idempotencyKey`
- DB Unique Constraint
- 생성자·생성시각
- 감사로그
- 외부 호출 실패 시 보상/상태 확인

변경:

- 허용 상태 전이
- 낙관적 잠금 Version 또는 조건부 Update
- 변경 전/후 감사
- Timeout 후 Commit 결과 확인
- 동일 요청 재전송 정책

삭제:

- 물리/논리 삭제
- 보존 기간과 감사
- 참조 무결성
- 승인 절차
- Cache·외부 동기화

## 14. Validation 설계

| 종류 | 예 | 위치 |
|---|---|---|
| 전문 구조 | Header/Body 없음 | TCF Validator |
| Header | ServiceId 없음 | STF |
| 형식 | 날짜, 길이 | DTO/Rule |
| 코드 | 유효 공통코드 | Rule/코드 서비스 |
| 인증 | Session/JWT | Gateway/TCF |
| 기능 권한 | ServiceId 실행 | Authorization |
| 데이터 권한 | 담당 지점 고객 | Rule/권한 서비스 |
| 상태 전이 | DRAFT에서만 승인 | Rule |
| 무결성 | Unique, FK | DB Constraint |

```java
if (!StringUtils.hasText(request.getCustomerNo())) {
    throw new BusinessException(
        "E-SV-VAL-0001",
        "고객번호는 필수입니다.");
}
```

사용자가 고칠 수 있는 메시지를 주되 SQL·테이블·내부 정책을 노출하지 않는다.

## 15. MyBatis와 SQL

```text
src/main/java/.../persistence/mapper/SvCustomerMapper.java
src/main/resources/mapper/sv/SvCustomerMapper.xml
```

기준:

- 필요한 컬럼만 조회
- 조건 컬럼의 인덱스와 선택도 확인
- `${}` 직접 치환 금지, `#{}` 바인딩
- 동적 정렬 컬럼은 Allow-List
- 안정적인 정렬과 Pagination
- 대량 `IN`, 깊은 Offset Paging 주의
- 복잡한 결과는 명시적 `resultMap`
- SQL ID와 GUID/ServiceId 연결
- 결과 없음과 DB 장애 구분

Full Scan은 Timeout만 늘려 해결하지 않는다. 실행계획, 통계, 인덱스, 조회 범위를 먼저 고친다.

## 16. 서비스 간 연동

업무 WAR끼리 Java 클래스를 직접 호출하면 배포 독립성이 깨지고 공통 Timeout·인증·추적을 우회한다. `tcf-eai` 또는 표준 Client를 사용한다.

전달 정보:

```text
대상 businessCode / ServiceId / transactionCode
GUID/TraceId
인증된 사용자·채널 Context
요청 Body
Connect/Read Timeout
```

Timeout 예산 예:

```text
온라인 전체 5.0초
  자체 검증/DB 1.0초
  외부 호출    2.5초
  응답/여유    1.5초
```

재시도가 비교적 안전한 경우:

- 멱등적인 조회
- 연결 전에 실패
- 상대 시스템이 재시도 가능으로 명시

위험한 경우:

- 등록·발송처럼 이미 처리됐을 수 있음
- Read Timeout 후 상대 Commit 여부를 모름
- 같은 멱등성 Key를 보존하지 않음

## 17. 트랜잭션

- 일반적으로 Facade를 경계로 둔다.
- 조회는 `readOnly = true`.
- 외부 HTTP 호출을 DB 트랜잭션 안에 오래 포함하지 않는다.
- 여러 DB/외부 시스템을 로컬 트랜잭션처럼 오해하지 않는다.
- 등록·변경은 멱등성과 결과 조회 방식을 설계한다.
- Runtime 예외의 Rollback 규칙과 Checked 예외 정책을 확인한다.
- 큰 트랜잭션은 Lock, Connection 점유, Rollback 비용을 키운다.

외부 호출과 DB 변경이 함께 있을 때 선택:

```text
DB 먼저 → 외부 실패 시 보상 필요
외부 먼저 → DB 실패 시 외부 보상 필요
Outbox/Event → 최종 일관성, 재처리 필요
```

업무 중요도와 일관성 요구에 따라 설계하고 단순히 `@Transactional`로 해결됐다고 생각하지 않는다.

## 18. Timeout

현재 세 계층:

| 계층 | 정책 | 적용 |
|---|---|---|
| Online | `ONLINE_TIMEOUT_SEC` | Handler 전체 실행 |
| Transaction | `TX_TIMEOUT_SEC` | Spring Transaction |
| DB | `DB_QUERY_TIMEOUT_SEC` | MyBatis Statement |

현재 fallback 기본 상수:

```text
Online      5초
Transaction 5초
DB Query    3초
```

실제 거래는 정책 저장소 값이 적용될 수 있다.

권장 관계:

```text
DB/외부 개별 제한
  < Transaction
  < Online
  < Apache/Client
  < L4 Idle
```

상위 Future Timeout이 하위 DB 작업을 반드시 중단시키지는 않는다. JDBC Statement Timeout, HTTP Client Timeout, Thread interrupt, Rollback 시점을 각각 확인해야 한다.

## 19. 멱등성과 중복 요청

```text
클라이언트 idempotencyKey
→ STF 상태 조회
→ 최초: PROCESSING
→ 업무 처리
→ 성공: SUCCESS
→ 재요청: 기존 결과 또는 명확한 중복 응답
```

상태 예:

```text
PROCESSING  아직 처리 중
SUCCESS     이미 성공
FAIL        실패, 재시도 가능성 판단
UNKNOWN     결과 확인 필요
```

현재 In-Memory 구현은 단일 프로세스에는 유용하지만 다중 WAS에서는 상태를 공유하지 못한다. 운영은 DB나 분산 저장소 기반인지 확인해야 한다.

## 20. Service Catalog와 거래통제

Catalog 연결정보:

```text
ServiceId
업무코드
거래코드
Handler
처리유형
권한코드
Timeout
감사 여부
사용 상태
담당 조직
```

불일치 결과:

- 코드는 있지만 미등록으로 차단
- Catalog는 있지만 Handler 없음
- 잘못된 거래코드로 감사·통계 오염
- 잘못된 Timeout
- 권한 과다/누락

거래통제는 ServiceId, 거래코드, 업무코드, 사용자, 채널, 지점, IP 등을 조건으로 전체/부분 차단할 수 있다. 통제 저장소 장애 시 Fail-open인지 Fail-closed인지 업무 위험도에 따라 명시해야 한다.

## 21. 오류 처리

형식:

```text
E-{DOMAIN}-{CATEGORY}-{NNNN}
```

예:

```text
E-TCF-HDR-0001
E-TCF-SVC-0001
E-SV-VAL-0001
E-SV-BIZ-0001
E-TCF-DB-0001
E-TCF-TIME-0001
E-TCF-SYS-9999
```

구분:

- Validation: 필수값·형식·코드
- Business: 결과 없음, 허용되지 않은 상태
- Authentication/Authorization
- DB/Interface/Timeout
- System: 미처리 예외

업무 계층은 의미 있는 `BusinessException`을 발생시키고 ETF가 표준 응답으로 변환한다. 사용자 메시지에 StackTrace, SQL, 서버명, 개인정보를 노출하지 않는다. 신규 오류코드는 OM에 등록하고 번호를 재사용하지 않는다.

## 22. 로그와 추적

최소 로그 Context:

```text
GUID / TraceId
ServiceId
TransactionCode
BusinessCode
User / Branch / Channel
SQL ID
Elapsed Time
Result / Error Code
```

금지:

- 비밀번호, JWT, Session ID
- 주민번호·계좌번호 원문
- 전체 요청 Body 무조건 출력
- 운영 `System.out` 의존

현재 소스에 콘솔 진단 출력이 있으므로 운영 적용 전 구조화 로그와 마스킹 정책을 반드시 검토해야 한다.

## 23. 세션·JWT·Gateway

```text
JWT
  토큰 발급·서명·만료·JWKS

Gateway
  토큰 검증·라우팅·신뢰 Header 구성

TCF
  세션·인증 Context·기능 권한·거래통제

업무 Rule
  고객·지점·조직 데이터 권한
```

Sticky Session:

```text
JSESSIONID=....tc01
Apache route=tc01
Tomcat jvmRoute=tc01
```

세 값이 일치해야 한다.

DeltaManager 주의:

- 세션 객체 `Serializable`
- 대형 DTO/파일 저장 금지
- 버전 간 클래스 호환성
- 복제 트래픽과 메모리 측정
- 센터 간 미복제 시 재로그인

## 24. Cache

적합:

- 공통코드
- Service Catalog
- 거래통제·Timeout 정책
- 작고 변경 빈도가 낮은 기준정보

신중:

- 대량 고객 데이터
- 즉시 정합성이 필요한 상태
- 사용자별 민감정보

정의할 항목:

```text
Key / 크기 / TTL / 최대 Entry
갱신 주체 / Evict / Reload
DB 장애 시 오래된 값 사용 여부
다중 노드 동기화
hit/miss/eviction 지표
```

OM 변경이 반영되지 않으면 DB뿐 아니라 Cache TTL, Evict, 노드별 상태를 확인한다.

## 25. Batch와 Scheduler

- Chunk 단위 처리
- Job Instance와 실행 이력
- 재시작 위치와 재처리 범위
- 중복 실행 방지 Lock
- 실패 건 격리
- 온라인 DB 부하 영향
- Scheduler는 트리거, 업무는 Job/Service

전체 데이터를 한 트랜잭션으로 처리하면 Lock과 Rollback 비용이 커진다. Chunk 크기는 Commit 비용과 재처리 범위를 함께 고려한다.

## 26. 파일 처리

업로드:

- 확장자와 실제 MIME/Signature
- Path Traversal 제거
- 크기 제한과 바이러스 검사
- 임시 저장 후 확정
- 보존기간과 Metadata

다운로드:

- 파일 ID 기반 조회
- 기능·데이터 권한
- 저장 경로 비노출
- 대용량 Streaming
- 감사로그

나쁜 예:

```text
GET /download?path=C:\data\secret.txt
```

권장:

```text
GET /files/{fileId}
→ Metadata
→ 권한
→ 안전한 저장소 Key로 Stream
```

## 27. 빌드와 실행

현재 루트 기준:

| 항목 | 값 |
|---|---|
| Java Toolchain | 21 |
| Spring Boot BOM | 3.3.5 |
| H2 | 2.2.224 |
| Test | JUnit Platform |
| 기본 bootRun Profile | `local` |

Windows:

```powershell
.\gradlew.bat :sv-service:test
.\gradlew.bat :sv-service:bootRun
.\gradlew.bat :sv-service:bootWar
.\gradlew.bat buildBusinessWars
.\gradlew.bat buildZtomcatWars
```

| 모드 | 용도 | 예 |
|---|---|---|
| bootRun | 단일 모듈 개발 | `localhost:8086/sv/online` |
| ztomcat | 통합 WAR 검증 | `localhost:8080/sv/online` |

문서의 고정 WAR 개수보다 현재 `build.gradle` 태스크 의존성과 실제 산출물을 기준으로 한다. `-x test`는 원인 분리용이지 공식 배포 기준이 아니다.

## 28. 환경설정

일반적인 우선순위:

```text
명령행
환경변수
System Property
application-{profile}.yml
application.yml
코드 기본값
```

문서 예시와 실행값이 다를 수 있다. 기동 로그와 승인된 환경 설정을 확인한다. Secret은 Git, 평문 YAML, 로그, 공개 Actuator에 노출하지 않는다.

## 29. 테스트 전략

| 구분 | 검증 |
|---|---|
| Rule 단위 | 필수값·형식·상태 전이 |
| Service 단위 | 호출 순서·분기·응답 변환 |
| Handler | ServiceId 등록·Facade 위임 |
| Mapper | namespace·SQL ID·파라미터·결과 |
| 통합 | Bean·트랜잭션·DB·예외 변환 |
| 거래 | 실제 `/online` 정상·실패 |
| 통제 | 미등록·차단·권한 없음 |
| 안정성 | 지연·Timeout·중복 |
| 보안 | 마스킹·권한 우회·입력 변조 |
| 배포 | WAR Context·Health·Smoke |

Rule 테스트 예:

```java
@Test
void customerNo가_없으면_검증오류() {
    var request = new CustomerSummaryRequest();

    var ex = assertThrows(
        BusinessException.class,
        () -> rule.buildSummaryCriteria(request));

    assertEquals("E-SV-VAL-0001", ex.getErrorCode());
}
```

거래 실패 테스트는 응답뿐 아니라 거래로그와 멱등성 상태가 종료됐는지 확인한다.

공통 변경:

- `tcf-core`, `tcf-web`: 주요 업무 전체 회귀
- Gateway/JWT: 인증·라우팅 End-to-End
- Mapper: 실행계획과 거래 통합
- 설정: 환경별 기동·Health·Rollback

## 30. 성능과 용량

```text
동시 요청
→ Tomcat Thread
→ Timeout Executor Thread
→ Hikari Connection
→ DB Session/CPU/IO
```

한 Pool만 늘리면 다음 계층이 병목이 된다.

```text
Tomcat Thread 300
Hikari Max 20
→ Connection 대기
→ 응답 지연
→ Timeout
→ 재시도 부하
```

필요 지표:

- Peak TPS
- 평균/p95/p99 응답시간
- 요청당 SQL 수와 점유시간
- 외부 호출 비율
- Session 크기
- Heap·GC Pause
- Thread/Connection 대기
- Timeout·오류율

직관:

```text
평균 동시 처리량 ≈ TPS × 평균 응답시간(초)
```

100 TPS와 평균 0.2초면 평균 동시 처리량은 약 20이다. Peak와 꼬리 지연을 포함해 여유를 둔다.

## 31. 관측성

애플리케이션:

- ServiceId별 TPS
- 성공/업무오류/시스템오류/Timeout
- 평균·p95·p99
- Thread active/queue/reject

DB:

- Hikari active/idle/pending
- Connection 획득시간
- SQL ID별 시간·호출수
- Lock·Deadlock·Long TX

JVM/WAS:

- Heap·Old Gen·GC Pause
- Thread와 Blocked
- Session 수·평균 크기
- Context Health

인프라:

- Apache Backend 오류
- Sticky route 분포
- Gateway Downstream Timeout
- L4 Health

정상 기준선을 먼저 수집해야 장애 수치와 비교할 수 있다.

## 32. 장애 분석

기본 순서:

1. 환경과 bootRun/Tomcat 구분
2. 실패 모듈·URL·ServiceId·거래코드
3. GUID/TraceId로 계층 로그 연결
4. STF/Dispatcher/업무/ETF 중 실패 지점
5. 최근 WAR·설정·DB·OM·Cache 변경
6. 복구와 Rollback 판단

ServiceId 미등록:

```text
요청 철자
→ Header serviceId
→ Handler serviceIds()
→ @Component
→ Package Scan
→ Dispatcher Map
→ OM Catalog
→ 배포 WAR 버전
```

거래 차단:

```text
오류코드
→ 통제 사용 여부
→ ServiceId/거래코드/사용자/채널/지점/IP
→ 일치 Rule과 우선순위
→ Cache 반영
→ 비상 차단 이력
```

SQL은 빠른데 Timeout:

- Connection 대기
- SQL 전후 외부 호출
- 여러 SQL 합산
- Executor Queue
- 응답 직렬화
- Lock
- GC Pause

개별 SQL만 보지 말고 GUID Timeline을 만든다.

로그인은 되지만 권한 없음:

```text
인증 성공
≠ ServiceId 기능 권한
≠ 고객/지점 데이터 권한
```

노드 장애 후 세션 유실:

- JSESSIONID route
- Apache route와 Tomcat jvmRoute
- `<distributable/>`
- 직렬화 오류
- Cluster Membership
- 센터 간 전환 여부

## 33. 보안 코딩

입력:

- 길이·허용문자·범위
- Enum/코드 Allow-List
- SQL 바인딩
- 파일 경로 정규화
- HTML Context별 Encoding

인증정보:

- JWT·Session ID 로그 금지
- Secret 평문 Git/YAML 금지
- Key Rotation과 Refresh Token 폐기

개인정보:

- 수집 최소화
- 외부 DTO 필드 명시
- 로그 마스킹
- Cache·세션 저장 최소화
- 파일 다운로드 감사

권한:

- 사용자가 Body에 보낸 사용자/지점 값을 권한 판단에 사용하지 않는다.
- Gateway 인증, TCF 기능권한, 업무 데이터권한을 분리한다.

## 34. 코드 리뷰 질문

구조:

- ServiceId에서 Handler와 SQL까지 찾기 쉬운가?
- Handler가 얇은가?
- 규칙이 Rule/Service에 읽기 좋게 표현됐는가?
- 업무 간 직접 의존이 없는가?

데이터:

- Request/Response/DB Row가 분리됐는가?
- 목록 최대 건수와 정렬이 있는가?
- SQL Injection·Full Scan 위험이 없는가?
- 트랜잭션이 과도하게 크지 않은가?

안정성:

- Timeout이 계층별로 일관적인가?
- 외부 호출 부분 실패를 처리하는가?
- 재시도가 안전한가?
- 중복 요청을 막는가?

운영:

- 오류코드와 조치가 구체적인가?
- GUID로 SQL·연동 로그가 연결되는가?
- OM 등록과 Cache 반영 절차가 있는가?
- Rollback 단위가 명확한가?

보안:

- 클라이언트 신원 값을 신뢰하지 않는가?
- 민감정보가 로그·응답·세션에 없는가?
- 기능권한과 데이터권한을 모두 검증하는가?

## 35. 기존 프로그램 역추적

화면에서:

```text
화면 이벤트
→ URL
→ header.serviceId
→ Handler.serviceIds()
→ doHandle
→ Facade → Service → Rule
→ DAO/Mapper
→ XML/SQL
→ 테이블
```

ServiceId에서:

```text
문자열 전체 검색
→ 요청 샘플
→ Handler
→ OM Catalog/통제/Timeout
→ 거래로그
→ 테스트
```

테이블에서:

```text
테이블명
→ Mapper XML
→ SQL ID
→ Mapper Interface
→ DAO → Service → Handler
→ ServiceId
→ 화면/호출자
```

영향도 축:

```text
호출자 / ServiceId / Java 계층 / SQL·테이블
공통 모듈 / OM 정책 / Cache / 로그·감사
테스트 / 배포 WAR
```

## 36. 신규 거래 설계 템플릿

```markdown
# 거래명

## 식별자
- 업무코드:
- Context/WAR:
- ServiceId:
- 거래코드:
- 처리유형:

## 입출력
- Request:
- Response:
- 필수값:
- 민감정보:

## 업무 흐름
1.
2.
3.

## 계층
- Handler:
- Facade:
- Service/Rule:
- DAO/Mapper:
- Client:

## 데이터
- DB/테이블:
- SQL ID:
- 인덱스/예상 건수:
- Paging:

## 운영정책
- 기능/데이터 권한:
- 거래통제:
- Online/TX/DB Timeout:
- 멱등성:
- 감사로그:
- 오류코드:

## 테스트
- 정상/Validation/업무오류:
- DB/연동/Timeout/중복/권한:

## 배포
- WAR:
- 설정:
- OM/Cache:
- Smoke:
- Rollback:
```

## 37. 개발 완료 체크리스트

설계·등록:

- [ ] 업무코드·Context·WAR·Package 정합
- [ ] ServiceId·거래코드 표준
- [ ] Handler와 Catalog 일치
- [ ] 권한·통제·Timeout·오류 등록

구현:

- [ ] Handler가 얇음
- [ ] Request/Response/DB Row 분리
- [ ] Rule에 입력·업무 검증
- [ ] Facade 트랜잭션 적절
- [ ] Mapper 제한 조회
- [ ] 타 업무 표준 연동
- [ ] 변경 거래 멱등성

품질·운영:

- [ ] 정상·검증·업무·DB·Timeout 테스트
- [ ] GUID·ServiceId·거래코드·SQL ID 추적
- [ ] 개인정보·인증정보 마스킹
- [ ] WAR Context와 Health
- [ ] 배포·Rollback 절차

## 38. 문서 차이 해석

자료 작성시점이 달라 다음 값이 충돌할 수 있다.

- JDK 17 권장과 현재 JDK 21
- 업무/관리 WAR 개수
- 샘플 거래코드 일련번호
- 과거 Package와 현재 `com.nh.nsight.marketing.*`
- bootRun, 단일 Tomcat, Cluster 토폴로지
- 예시 Timeout과 환경 정책값

판단 순서:

1. 현재 `settings.gradle`, `build.gradle`
2. 현재 Java, Mapper XML, `application-*.yml`
3. 루트·모듈 README
4. OM 현재 기준정보와 배포 설정
5. 설계 Markdown의 원칙
6. 집필본·샘플의 예시

예시 계정·주소·수치를 운영값으로 간주하지 않는다.

## 39. 권장 학습 순서

1단계:

- 이 문서 1~10장
- 루트 README
- `TCF`, `STF`, `ETF`, `TransactionDispatcher`

목표: 요청이 어떻게 검증되고 Handler를 찾는지 설명한다.

2단계:

- `SvCustomerHandler`
- `SvCustomerFacade`
- `SvCustomerService`
- `SvCustomerRule`
- `SvCustomerDao`
- `SvCustomerMapper.xml`

목표: `SV.Customer.selectSummary`를 Header에서 SQL까지 추적한다.

3단계:

- Catalog
- 거래통제
- Timeout
- 오류코드
- 거래/감사로그
- 세션·권한

목표: 코드가 정상인데 실행되지 않는 이유를 진단한다.

4단계:

- 조회 거래 추가
- Rule·Mapper 테스트
- 표준 전문 통합 테스트
- OM 등록
- Tomcat Smoke

목표: 각 계층을 나눈 이유를 설명하면서 구현한다.

## 40. 최종 요약

NSIGHT TCF 개발의 핵심은 Java 클래스를 많이 만드는 것이 아니다. 한 업무 요청을 식별자, 코드, 데이터, 운영정책, 로그, 테스트, 배포까지 끊기지 않게 연결하는 것이다.

```text
업무 요구사항
  ↓
업무코드 / ServiceId / 거래코드
  ↓
표준 Header + Body
  ↓
STF 공통 검증과 통제
  ↓
Handler → Facade → Service → Rule → DAO/Mapper
  ↓
ETF 표준 응답과 거래 마감
  ↓
OM 정책 / 로그 / 감사 / 모니터링
  ↓
테스트 / WAR / 배포 / Rollback
```

항상 세 질문에 답해야 한다.

1. 이 요청은 어떤 ServiceId와 거래코드로 식별되는가?
2. 이 로직은 어느 계층의 책임이며 어디까지가 트랜잭션인가?
3. 실패하거나 느려졌을 때 GUID로 원인과 처리 결과를 확인할 수 있는가?

## 41. 비 Word 근거 자료

- 루트 `README.md`, `settings.gradle`, `build.gradle`
- `ztcf-집필본-md`
- `zarchitecture`
- `zdocs-1`, `zdocs-2`
- `zguide`, `zman`, `znsight-man`
- `ztcfbook`, `ztcfbook-h`, `ztcfbook-m`
- `ztcf-book-capacity-md`
- `znsight-config-info`, `znsight-config-value-word`
- `ztcf-engine-config-info`
- `ztcf-다이어리`
- 현재 `tcf-core`, `tcf-web`, `sv-service` 소스와 설정

Word 파일만 존재하거나 비 Word 자료가 없는 폴더는 내용 요약에서 제외했다.
