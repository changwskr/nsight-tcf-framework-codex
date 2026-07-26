# 43. Cache 사용 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 43. Cache 사용 기준

### 43.1 도입 전 안내말

Cache는 단순히 DB 조회를 빠르게 하기 위한 기능이 아니다.NSIGHT TCF Framework에서 Cache는 공통코드, ServiceId 카탈로그, 권한, 메뉴, 오류코드, Timeout 정책, 거래통제 정책처럼 반복 조회되는 기준정보를 안정적으로 제공하기 위한 공통 성능·운영 제어 계층이다.
NSIGHT Cache 구조는 tcf-cache 공통 모듈을 기준으로 하며, 기술적으로는 Spring Cache + EhCache 3 + JCache를 사용하고, 운영관리는 tcf-om의 Cache 관리 화면에서 수행하는 구조가 적합하다. 또한 Cache의 원장은 DB이고, Cache는 조회 가속 계층이며, 업무 데이터는 캐시하지 않고 기준정보·정책정보·카탈로그성 데이터만 캐시하는 것이 원칙이다.
핵심 문장은 다음이다.
DB는 기준정보의 원장이고, Cache는 반복 조회를 줄이기 위한 조회 가속 계층이다.고객정보·거래내역·Token·파일 원본은 Cache 대상이 아니다.

### 43.2 Cache 사용 기준 결론

| 구분 | 기준 |
| --- | --- |
| Cache 실행 모듈 | tcf-cache |
| Cache 기술 | Spring Cache + EhCache 3 + JCache |
| Cache 위치 | 각 업무 WAR 로컬 메모리 Cache |
| Cache 관리 주체 | tcf-om |
| Cache 사용 주체 | 전체 업무 WAR, tcf-om, 필요 시 tcf-gateway |
| Cache 원장 | OMDB / 기준정보 DB |
| Cache 대상 | 공통코드, ServiceId, 권한, 메뉴, 오류코드, Timeout, 거래통제, Gateway Route |
| Cache 금지 대상 | 고객정보, 계좌정보, 거래내역, 개인정보, Token, 비밀번호, 파일 원본 |
| Cache 갱신 | OM 기준정보 변경 시 Evict 또는 Reload |
| Cache 장애 시 | DB 조회 Fallback |
| 다중 WAS 기준 | 로컬 Cache + TTL + Cache Version + OM 전체 Evict |
| 운영관리 | OM Cache 관리 화면에서 조회·삭제·재적재·이력 관리 |
| 감사 기준 | Evict, Reload, Warm-up은 감사로그 대상 |

### 43.3 Cache 적용 위치

```text
[사용자 / WebTopSuite / API]
        ↓
[Apache / tcf-gateway]
        ↓

```

```text
[업무 WAR / tcf-om]
        ↓
[TCF Framework]
        ↓
┌────────────────────────────────────┐
│ tcf-cache                           │
│ - Spring Cache                      │
│ - EhCache 3                         │
│ - JCache                            │
│ - TcfCacheNames                     │
│ - TcfCacheSupport                   │
└────────────────────────────────────┘
        ↓ Cache Miss

```

```text
[DAO / Mapper]
        ↓
```

[OMDB / LOGDB / SESSIONDB / RDW / ADW]

tcf-cache는 독립 실행 WAR가 아니라 공통 라이브러리 JAR이다. 업무 WAR 또는 tcf-om이 의존성으로 포함하고, Spring Cache AOP의 @Cacheable, @CacheEvict 방식으로 사용한다.

### 43.4 Cache 대상 / 비대상 기준

| 구분 | Cache 대상 |
| --- | --- |
| 예시 | Cache 여부 |
| 기준 | 공통코드 |
| 코드그룹 / 코드 | CHANNEL_ID, USE_YN, BUSINESS_CODE |
| 허용 | 화면·전문·검증에서 반복 조회 |
| Service Catalog | ServiceId 기준정보 |
| SV.Customer.selectSummary | 허용 |
| Dispatcher, 권한, 감사 기준 | 권한정책 |
| 메뉴/기능/데이터 권한 | 조회/등록/수정/삭제 권한 |
| 조건부 허용 | 변경 시 Evict 필수 |
| 메뉴정보 | 메뉴 트리, 화면 URL |
| OM 메뉴, 업무 메뉴 | 허용 |
| 로그인·화면 진입 시 반복 조회 | 오류코드 |
| 오류코드 / 메시지 | E-TCF-HDR-0001 |
| 허용 | ETF 후처리에서 반복 조회 |
| Timeout 정책 | 서비스별 Timeout |
| online=5s, query=3s | 허용 |
| STF 전처리에서 반복 조회 | 거래통제 |
| 거래 허용 기준 | serviceId + user + channel + branch |
| 조건부 허용 | TTL 짧게, 변경 시 Evict |
| Gateway Route | 업무코드 라우팅 |
| SV → sv-service:8080 | 조건부 허용 |
| 라우팅 변경 시 즉시 Evict | 세션 보조정보 |
| Region성 보조 데이터 | sessionRegion |
| 제한 허용 | 세션 원장은 SESSIONDB |
| 고객정보 | 고객명, 주민번호, 계좌 |
| 고객 상세 | 금지 |
| 개인정보·정합성 위험 | 거래내역 |
| 계좌거래, 상담이력 | 거래 상세 |
| 금지 | 변경 가능성·감사 위험 |
| 조회결과 | 대량 목록, 검색 결과 |
| 고객목록, 캠페인 대상 | 금지 |
| Heap 증가·현행성 문제 | 인증정보 |
| JWT, Refresh Token, 비밀번호 | Token, Secret |
| 금지 | 보안 위험 |
| 파일 데이터 | Excel, PDF, CSV 원본 |
| 다운로드 파일 | 금지 |
공통코드는 단순한 화면 콤보박스 값이 아니라 화면, 전문 Header, 입력값 검증, 업무 분기, 권한·감사·로그 분류에 사용되는 기준정보이므로 Cache 대상으로 적합하다.

| Storage 사용 | |

### 43.5 Cache Region 표준

| Cache Name | 용도 |
| --- | --- |
| Key 예시 | TTL 권장 |
| Evict 시점 | commonCode |
| 공통코드 | CC:CODE:CHANNEL_ID |
| 30분 | 코드 등록·수정·사용중지 |
| serviceCatalog | ServiceId 기준정보 |
| TCF:SERVICE:SV.Customer.selectSummary | 60분 |
| ServiceId 수정·중지·배포 | authPolicy |
| 권한정책 | OM:AUTH:ROLE_ADMIN:OM.User.save |
| 10분 | 권한 변경 |
| menuTree | 메뉴 트리 |
| OM:MENU:ROLE_ADMIN | 10분 |
| 메뉴 변경 | errorCode |
| 오류코드/메시지 | TCF:ERROR:E-TCF-HDR-0001 |
| 60분 | 오류코드 변경 |
| timeoutPolicy | Timeout 정책 |
| TCF:TIMEOUT:SV.Customer.selectSummary | 10분 |
| Timeout 정책 변경 | transactionControl |
| 거래통제 | TCF:TXCTL:{serviceId}:{user}:{channel}:{branch} |
| 1~5분 | 거래통제 변경 |
| gatewayRoute | Gateway 라우팅 |
| GW:ROUTE:prd:SV | 5~10분 |
| Route 변경 | sessionRegion |
| 세션 보조 정보 | SESSION:REGION:CENTER-A |
| 10분 | 세션 정책 변경 |

기본 Cache Region은 commonCode, serviceCatalog, sessionRegion으로 시작하고, 운영 표준으로 확장하면서 authPolicy, menuTree, errorCode, timeoutPolicy, transactionControl, gatewayRoute를 추가하는 것이 좋다. 기존 Cache 구조에서도 Cache 대상은 공통코드, ServiceId 카탈로그, 정책성 기준정보로 정의되어 있다.

### 43.6 Cache Key 명명 규칙

Cache Key는 운영자가 로그나 OM 화면에서 봤을 때 의미를 바로 알 수 있어야 한다.
{DOMAIN}:{TYPE}:{KEY1}:{KEY2}:{KEY3}

| 구분 | Key 예시 |
| --- | --- |
| 설명 | 공통코드 그룹 |
| CC:CODE:CHANNEL_ID | 채널 코드 전체 |
| 공통코드 단건 | CC:CODE:CHANNEL_ID:WEBTOP |
| 특정 코드 | ServiceId |
| TCF:SERVICE:SV.Customer.selectSummary | Service Catalog |
| 메뉴 | OM:MENU:ROLE_ADMIN |
| 권한그룹별 메뉴 | 기능권한 |
| OM:AUTH:ROLE_ADMIN:OM.User.save | 특정 기능권한 |
| Timeout | TCF:TIMEOUT:SV.Customer.selectSummary |
| 서비스 Timeout | 거래통제 |
| TCF:TXCTL:SV.Customer.selectSummary:U10001:WEBTOP:001234 | 거래 허용 여부 |
| Gateway Route | GW:ROUTE:prd:SV |
| 운영환경 SV 라우팅 | 금지 Key |
| commonCode::{guid} | customerInfo::{customerNo} |
| token::{accessToken} | searchResult::{userId}:{timestamp} |

fileDownload::{fileBinary}

| 금지 기준 | 사유 |
| --- | --- |
| GUID / TraceId를 Key로 사용 | 매 요청마다 달라져 Cache 효과 없음 |
| 고객번호 / 계좌번호를 Key로 사용 | 개인정보·감사 위험 |
| Token 값을 Key로 사용 | 인증정보 유출 위험 |
| 요청시각을 Key로 사용 | Cache Entry 폭증 |
| 파일 원본을 Key 또는 Value로 사용 | Heap 고갈 위험 |

### 43.7 Cache 조회 흐름

업무 Service
```text
   ↓
```

@Cacheable 확인
```text
   ↓
```

Cache Hit?
```text
   ├─ Yes → Cache 데이터 반환
   │
   └─ No
       ↓
DAO / Mapper 조회
       ↓
      DB 조회
       ↓
      Cache 저장
       ↓
      업무 Service 반환

```

| 단계 | 처리 내용 |
| --- | --- |
| 1 | 업무 Service 또는 OM Service에서 기준정보 조회 요청 |
| 2 | Spring Cache AOP가 Cache Name과 Key 확인 |
| 3 | Cache Hit이면 DB 조회 없이 반환 |
| 4 | Cache Miss이면 DAO / Mapper를 통해 DB 조회 |
| 5 | 조회 결과를 Cache에 저장 |
| 6 | 이후 동일 Key 요청은 Cache에서 반환 |

### 43.8 Cache 변경 / Evict 흐름

```text
OM 기준정보 변경
   ↓
STF 권한 검증
   ↓
DB 원장 UPDATE
   ↓
```

변경이력 INSERT
```text
   ↓
```

감사로그 INSERT
```text
   ↓
```

해당 Cache Evict
```text
   ↓
```

Cache Version 증가
```text
   ↓
```

각 업무 WAR가 다음 조회 시 재적재

| 변경 대상 | Evict 대상 | 설명 |
| --- | --- | --- |
| 공통코드 변경 | commonCode::{codeGroup} | 변경된 코드그룹 삭제 |

공통코드 그룹 변경
commonCode::__ALL_GROUPS__

| 전체 그룹 목록 삭제 | ServiceId 변경 |
| --- | --- |
| serviceCatalog::{serviceId} | Dispatcher 기준정보 삭제 |
| 권한 변경 | authPolicy::{authGroupId} |
| 기능권한·데이터권한 삭제 | 메뉴 변경 |

menuTree::{authGroupId}

| 메뉴 트리 삭제 | 오류코드 변경 |
| --- | --- |
| errorCode::{errorCode} | ETF 오류 메시지 삭제 |

Timeout 변경
timeoutPolicy::{serviceId}

| STF Timeout 정책 삭제 | 거래통제 변경 |
| --- | --- |
| transactionControl::* | 허용/차단 정책 삭제 |

Gateway Route 변경
gatewayRoute::{env}:{businessCode}
라우팅 정책 삭제
공통코드 관리 구조도 OM.CommonCode.* 거래에서 DB 원장을 변경하고, 변경이력 저장 후 tcf-cache / EhCache를 통해 전체 업무 WAR가 조회 사용하는 구조로 정의되어 있다.

### 43.9 다중 WAS Cache 동기화 기준

EhCache는 기본적으로 각 WAS의 로컬 메모리 Cache이다.따라서 AP01에서 Evict해도 AP02, AP03의 Cache가 그대로 남을 수 있다.
| 방식 | 설명 | 적용 판단 | TTL 만료 |
| --- | --- | --- | --- |
| 일정 시간 후 자동 만료 | 기본 적용 | OM 전체 Evict 호출 | 운영자가 전체 WAS에 Evict 요청 |
| 필수 | Cache Version 비교 | DB의 버전값과 로컬 버전 비교 | 권장 |
| 배포 시 초기화 | WAR 기동 시 Cache Clear 후 Warm-up | 권장 | 분산 Cache |

Redis 등 외부 Cache 사용
차후 검토
권장 구조는 다음과 같다.
```text
OM_CACHE_VERSION
  CACHE_NAME
  VERSION_NO
  UPDATED_AT
        ↓
```

각 WAS가 주기적으로 VERSION 확인
```text
        ↓
```

로컬 VERSION과 다르면 Evict 후 재적재

기존 Cache 관리 설계에서도 다중 WAS 환경에서는 TTL 만료, OM 전체 Evict, Cache Version 비교, 배포 시 초기화, 필요 시 분산 Cache 검토를 안전한 방식으로 제시한다.

### 43.10 Cache 관리 테이블

#### 43.10.1 OM_CACHE_VERSION

CREATE TABLE OM_CACHE_VERSION (
    CACHE_NAME     VARCHAR2(100) NOT NULL,
    VERSION_NO     NUMBER(18)    NOT NULL,
    UPDATED_BY     VARCHAR2(50),
    UPDATED_AT     TIMESTAMP     NOT NULL,
    PRIMARY KEY (CACHE_NAME)
);

| 컬럼 | 설명 |
| --- | --- |
| CACHE_NAME | Cache Region 이름 |
| VERSION_NO | 현재 Cache Version |
| UPDATED_BY | 변경 사용자 |
| UPDATED_AT | 변경 시각 |

#### 43.10.2 OM_CACHE_HISTORY

CREATE TABLE OM_CACHE_HISTORY (
    HISTORY_ID      VARCHAR2(64)  NOT NULL,
    CACHE_NAME      VARCHAR2(100) NOT NULL,
    CACHE_KEY       VARCHAR2(300),
    ACTION_TYPE     VARCHAR2(30)  NOT NULL,
    BEFORE_VERSION  NUMBER(18),
    AFTER_VERSION   NUMBER(18),
    REASON          VARCHAR2(500),
    EXECUTED_BY     VARCHAR2(50),
    EXECUTED_AT     TIMESTAMP,
    PRIMARY KEY (HISTORY_ID)
);

| 컬럼 | 설명 |
| --- | --- |
| HISTORY_ID | 이력 ID |
| CACHE_NAME | Cache Region |
| CACHE_KEY | 삭제 또는 재적재 대상 Key |
| ACTION_TYPE | EVICT, EVICT_ALL, RELOAD, WARMUP |
| BEFORE_VERSION | 변경 전 Version |
| AFTER_VERSION | 변경 후 Version |
| REASON | 운영자 조치 사유 |
| EXECUTED_BY | 실행자 |
| EXECUTED_AT | 실행시각 |

### 43.11 OM Cache 관리 기준

Cache 운영관리는 OM.Cache.* 거래로 수행한다.
| ServiceId | 거래코드 | 설명 | 권한 |
| --- | --- | --- | --- |
| OM.Cache.inquiry | OM-CACHE-0001 | Cache 목록 조회 | 조회 |
| OM.Cache.entryInquiry | OM-CACHE-0002 | Cache Entry 조회 | 조회 |
| OM.Cache.evict | OM-CACHE-0003 | 특정 Key Evict | 운영자 |
| OM.Cache.evictAll | OM-CACHE-0004 | Cache Region 전체 Evict | 관리자 |
| OM.Cache.reload | OM-CACHE-0005 | DB 기준 재적재 | 관리자 |
| OM.Cache.versionInquiry | OM-CACHE-0006 | Cache Version 조회 | 조회 |
| OM.Cache.warmup | OM-CACHE-0007 | 주요 Cache Warm-up | 관리자 |
| OM.Cache.historyInquiry | OM-CACHE-0008 | Cache 변경이력 조회 | 감사 |

```text
[OM Cache 관리 화면]
        ↓
POST /om/online
serviceId = OM.Cache.inquiry
        ↓
OmCacheHandler
        ↓
OmCacheFacade
        ↓
OmCacheService
        ↓
TcfCacheSupport
        ↓
EhCache Snapshot / Evict
```

Cache 삭제는 운영 영향이 있으므로 반드시 삭제 사유를 입력받고 감사로그에 남긴다. 기존 Cache 구조도 OM.Cache.inquiry, OM.Cache.delete, Cache Snapshot/Evict와 같은 운영 기능을 TcfCacheSupport와 연계하는 방향을 제시한다.

### 43.12 application.yml 표준

```yaml
spring:
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml
nsight:
  tcf:
    cache:
      enabled: true
      config-location: classpath:ehcache.xml
      version-check-enabled: true
      version-check-interval-seconds: 60
      warmup-on-startup: true
      allow-om-evict: true
      default-ttl-seconds: 1800
      caches:
        common-code:
          name: commonCode
          ttl-seconds: 1800
          max-entries: 5000
        service-catalog:
          name: serviceCatalog
          ttl-seconds: 3600
          max-entries: 2000
        auth-policy:
          name: authPolicy
          ttl-seconds: 600
          max-entries: 10000
        menu-tree:
          name: menuTree
          ttl-seconds: 600
          max-entries: 1000
        timeout-policy:
          name: timeoutPolicy
          ttl-seconds: 600
          max-entries: 3000
        transaction-control:
          name: transactionControl
          ttl-seconds: 300
          max-entries: 50000
        gateway-route:
          name: gatewayRoute
          ttl-seconds: 600
          max-entries: 1000
```

기존 tcf-cache 기본 설정도 spring.cache.type: jcache, spring.cache.jcache.config: classpath:ehcache.xml, nsight.tcf.cache.enabled: true 구조로 정리되어 있다.

### 43.13 Java 구현 예시

#### 43.13.1 Cache Name 상수

package com.nh.nsight.tcf.cache.support;

```java
public final class TcfCacheNames {
    public static final String COMMON_CODE = "commonCode";
    public static final String SERVICE_CATALOG = "serviceCatalog";
    public static final String SESSION_REGION = "sessionRegion";
    public static final String AUTH_POLICY = "authPolicy";
    public static final String MENU_TREE = "menuTree";
    public static final String ERROR_CODE = "errorCode";
    public static final String TIMEOUT_POLICY = "timeoutPolicy";
    public static final String TRANSACTION_CONTROL = "transactionControl";
    public static final String GATEWAY_ROUTE = "gatewayRoute";
    private TcfCacheNames() {
    }
}
```

#### 43.13.2 공통코드 조회

```java
@Service
@RequiredArgsConstructor
public class CommonCodeQueryService {
    private final CommonCodeMapper commonCodeMapper;
    @Cacheable(
            cacheNames = TcfCacheNames.COMMON_CODE,
            key = "'CC:CODE:' + #codeGroup"
    )
    public List<CommonCode> findByGroup(String codeGroup) {
        return commonCodeMapper.selectByGroup(codeGroup);
    }
}
```

#### 43.13.3 공통코드 변경 시 Evict

```java
@Service
@RequiredArgsConstructor
public class CommonCodeCommandService {
    private final CommonCodeMapper commonCodeMapper;
    private final CommonCodeHistoryMapper commonCodeHistoryMapper;
    private final CacheVersionService cacheVersionService;
    @Transactional
    @CacheEvict(
            cacheNames = TcfCacheNames.COMMON_CODE,
            key = "'CC:CODE:' + #command.codeGroup"
    )
    public void updateCommonCode(CommonCodeUpdateCommand command) {
        commonCodeMapper.update(command);
        commonCodeHistoryMapper.insert(command.toHistory());
        cacheVersionService.increaseVersion(
                TcfCacheNames.COMMON_CODE,
                command.getUpdatedBy()
        );
    }
}
```

#### 43.13.4 Timeout 정책 조회

```java
@Service
@RequiredArgsConstructor
public class TimeoutPolicyCacheService {
    private final TimeoutPolicyMapper timeoutPolicyMapper;
    @Cacheable(
            cacheNames = TcfCacheNames.TIMEOUT_POLICY,
            key = "'TCF:TIMEOUT:' + #serviceId"
    )
    public TimeoutPolicy findByServiceId(String serviceId) {
        TimeoutPolicy policy = timeoutPolicyMapper.selectByServiceId(serviceId);
        if (policy == null) {
            return TimeoutPolicy.defaultPolicy(serviceId);
        }
        return policy;
    }
}
```

Timeout 정책은 매 거래마다 DB에서 조회하면 부하가 발생하므로 Cache 대상으로 관리하고, 변경 시 OM에서 Evict 또는 Reload하는 구조가 적합하다.

### 43.14 개발자 사용 기준

| 기준 | 설명 |
| --- | --- |
| 업무 데이터는 Cache하지 않는다 | 고객정보, 거래내역, 조회결과 금지 |
| 기준정보만 Cache한다 | 공통코드, ServiceId, 권한, 정책정보 |
| Cache는 Service 계층에서 사용한다 | Controller, Handler, DAO에서 남용 금지 |
| Mapper 결과를 무조건 Cache하지 않는다 | Cache 대상 여부를 명확히 판단 |
| 변경 거래에는 Evict를 반드시 포함한다 | DB 변경 후 Cache 무효화 |
| Cache Key에 개인정보를 넣지 않는다 | 고객번호, 계좌번호, Token 금지 |
| TTL을 무한대로 두지 않는다 | 변경 반영 지연 방지 |
| Cache Miss 시 DB Fallback이 가능해야 한다 | Cache 장애 대비 |
| 운영 Evict 기능을 제공해야 한다 | OM Cache 관리 화면 |
| Evict/Reload는 감사로그를 남긴다 | 운영 추적성 확보 |

### 43.15 금지 기준

| 금지 항목 | 사유 |
| --- | --- |
| 고객정보 Cache | 개인정보·정합성 위험 |
| 계좌정보 Cache | 금융정보 유출 위험 |
| 거래내역 Cache | 변경 가능성·감사 위험 |
| 대량 목록 결과 Cache | Heap 증가 위험 |
| 파일 원본 Cache | 메모리 고갈 위험 |
| JWT / Refresh Token Cache | 인증정보 유출 위험 |
| 비밀번호 / Secret Cache | 보안 사고 위험 |
| GUID, TraceId, timestamp 기반 Key | Entry 폭증 |
| 업무 WAR에서 기준정보 직접 수정 | OM 원장 관리 위반 |
| Cache만 믿고 DB 원장 미관리 | 정합성 붕괴 |
| Evict 없이 기준정보 수정 | 이전 값 사용 위험 |
| MyBatis 2차 Cache 활성화 | 통제 어려움, 정합성 위험 |

### 43.16 장애 대응 기준

| 장애 현상 | 가능 원인 | 확인 방법 |
| --- | --- | --- |
| 조치 | 코드값 변경 후 화면 미반영 | Cache 미삭제 |

OM Cache 화면에서 Entry 확인

| 해당 코드그룹 Evict | 특정 WAS만 다르게 동작 | WAS별 로컬 Cache 불일치 |
| --- | --- | --- |
| WAS별 Cache Version 비교 | 전체 WAS Evict | 미등록 ServiceId 오류 |

Service Catalog Cache 미갱신

| serviceCatalog 조회 | Service Catalog Reload | 권한 변경 후 반영 지연 |
| --- | --- | --- |
| authPolicy Cache 유지 | 사용자 권한 Cache 확인 | 권한 Cache Evict |

Timeout 변경 미반영
timeoutPolicy Cache 유지

| Timeout Cache 확인 | Timeout Cache Evict | 거래통제 변경 미반영 |
| --- | --- | --- |
| transactionControl TTL 잔존 | 거래통제 Cache 확인 | 거래통제 Cache Clear |

Gateway 라우팅 변경 미반영
gatewayRoute Cache 잔존

| Route Cache 확인 | Route Cache Reload | Heap 사용량 증가 |
| --- | --- | --- |
| Cache Entry 과다 | Entry 수, Heap 사용률 확인 | TTL 단축, max-entry 축소 |

Cache 초기화 실패
ehcache.xml 오류
기동 로그, Health Check

| 설정 수정 후 재기동 | Cache 장애 | EhCache Bean 미생성 |
| --- | --- | --- |
Deep Health Check는 DB, SessionDB, TCF, Cache, 외부연계까지 확인해야 하므로 Cache 상태도 헬스체크 대상에 포함해야 한다.

| Deep Health Check | Cache 비활성 후 DB 조회 |

### 43.17 보안 기준

| 보안 항목 | 기준 |
| --- | --- |
| 개인정보 | Cache Value와 Key 모두 저장 금지 |
| Token | Access Token, Refresh Token 원문 저장 금지 |
| Secret | DB 비밀번호, API Key, 암호화 Key 저장 금지 |
| 권한정보 | TTL 짧게, 변경 시 즉시 Evict |
| 감사 | Evict, Reload, Warm-up 수행자·사유 기록 |
| OM 권한 | Cache 전체 삭제는 관리자만 허용 |
| 로그 | Cache Entry Value 원문 로그 금지 |
| 운영 조회 | 민감한 Value는 마스킹 또는 건수만 표시 |

### 43.18 오류코드 기준

| 오류코드 | 오류명 | 발생 위치 |
| --- | --- | --- |
| 사용자 메시지 | E-CACHE-0001 | Cache 설정 오류 |

Startup
Cache 설정이 올바르지 않습니다.
E-CACHE-0002

| Cache 조회 실패 | Service |
| --- | --- |
기준정보 조회 중 오류가 발생했습니다.

| E-CACHE-0003 | |
| Cache Evict 실패 | OM |
Cache 삭제 중 오류가 발생했습니다.

| E-CACHE-0004 | |
| Cache Reload 실패 | OM |
Cache 재적재 중 오류가 발생했습니다.

| E-CACHE-0005 | |
| Cache Version 불일치 | Version Check |
| Cache 동기화가 필요합니다. | E-CACHE-0006 |
| 허용되지 않은 Cache 접근 | OM |
| Cache 관리 권한이 없습니다. | E-CACHE-0007 |
| Cache Key 형식 오류 | Service |
Cache Key가 올바르지 않습니다.

| E-CACHE-0008 | |
| 금지 대상 Cache 시도 | Service |
Cache 대상이 아닌 데이터입니다.

운영자 로그에는 cacheName, cacheKey, actionType, executedBy, guid, traceId를 남긴다.

### 43.19 테스트 케이스

| No | 테스트 케이스 | 기대 결과 |
| --- | --- | --- |
| 1 | 공통코드 최초 조회 | DB 조회 후 Cache 적재 |
| 2 | 공통코드 재조회 | Cache Hit |
| 3 | 공통코드 변경 | DB 변경 + History 저장 + Cache Evict |
| 4 | ServiceId 변경 | serviceCatalog Evict 후 재조회 |
| 5 | 권한 변경 | authPolicy, menuTree Evict |
| 6 | Timeout 정책 변경 | timeoutPolicy Evict |
| 7 | 거래통제 변경 | transactionControl Evict |
| 8 | 특정 WAS만 이전 값 보유 | Cache Version 비교 후 Evict |
| 9 | Cache 비활성화 | DB Fallback 조회 |
| 10 | 고객정보 Cache 시도 | 코드리뷰 또는 테스트에서 차단 |
| 11 | Token Cache 시도 | 보안 테스트 실패 처리 |
| 12 | OM Cache 전체 삭제 | 관리자 권한 필요, 감사로그 저장 |
| 13 | Warm-up 실행 | 주요 Cache 사전 적재 |
| 14 | Cache Entry 과다 | max-entry 또는 TTL로 제한 |
| 15 | Deep Health Check | Cache Region 상태 확인 |

### 43.20 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| Cache 대상이 기준정보·정책정보·카탈로그성 데이터인가 | □ |
| 고객정보, 거래내역, Token, 파일 원본을 Cache하지 않는가 | □ |
| Cache Key에 개인정보가 포함되지 않는가 | □ |
| Cache Name이 TcfCacheNames 상수로 관리되는가 | □ |
| ehcache.xml Alias와 Java 상수가 일치하는가 | □ |
| TTL이 정의되어 있는가 | □ |
| max-entry 또는 Heap 제한이 있는가 | □ |
| 변경 거래에 @CacheEvict 또는 명시적 Evict가 있는가 | □ |
| OM 변경 시 Cache Version이 증가하는가 | □ |
| 다중 WAS Evict 방안이 있는가 | □ |
| Cache 비활성 시 DB Fallback이 가능한가 | □ |
| Evict, Reload, Warm-up 이력이 저장되는가 | □ |
| Cache 상태가 Health Check에 포함되는가 | □ |
| 운영자가 OM 화면에서 조회·삭제할 수 있는가 | □ |
| MyBatis 2차 Cache를 사용하지 않는가 | □ |

### 43.21 마무리말

Cache 사용 기준의 핵심은 무엇을 캐시할 것인가보다 무엇을 캐시하지 않을 것인가를 명확히 하는 것이다.
NSIGHT에서는 Cache를 성능 향상 수단으로 사용하되, 데이터 정합성, 개인정보 보호, 감사 추적, 운영 통제를 우선한다. 따라서 고객정보나 거래정보를 캐시하지 않고, 공통코드·ServiceId·권한·정책정보처럼 변경 빈도가 낮고 반복 조회가 많은 기준정보만 Cache 대상으로 삼는다.
### 43.22 시사점

| 관점 | 시사점 | 개발 관점 |
| --- | --- | --- |
| 업무 개발자는 Cache를 직접 남용하지 말고 tcf-cache 표준 Region과 Key 규칙을 따른다 | 운영 관점 | OM에서 Cache 조회, Evict, Reload, Warm-up, 이력관리가 가능해야 한다 |
| 보안 관점 | 개인정보, Token, 비밀번호, 파일 원본은 Cache 대상에서 제외한다 | 아키텍처 관점 |
| DB는 원장, Cache는 조회 가속 계층이며 다중 WAS에서는 Version 기반 동기화가 필요하다 |  |  |

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (47).docx`

| [캐시관리.md](../zdoc/캐시관리.md) |
| [16-Cache구조.md](../zman/16-Cache구조.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [42. 보안 코딩 기준](./42-보안-코딩-기준.md) · [44. 파일 업다운로드 기준](./44-파일-업다운로드-기준.md) →