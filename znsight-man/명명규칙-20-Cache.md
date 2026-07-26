# Cache 명명규칙

> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

21장. Cache 명명규칙 설계기준
### 21.1 도입 전 안내말

Cache 명명규칙은 단순히 commonCode, serviceCatalog 같은 이름을 정하는 기준이 아니다.NSIGHT TCF에서 Cache 이름은 공통코드, ServiceId, 권한, 메뉴, 오류코드, Timeout 정책, 거래통제 정책을 빠르게 조회하기 위한 운영 식별자다.
NSIGHT Cache 구조는 tcf-cache 공통 모듈을 중심으로 Spring Cache + EhCache 3 + JCache를 사용하고, tcf-om에서 Cache 조회·삭제·재적재를 운영관리하는 구조가 적합하다. 또한 Cache 대상은 기준정보·정책정보·카탈로그성 데이터로 제한하고, 고객정보·거래내역·개인정보·토큰·비밀번호는 Cache 대상에서 제외해야 한다.

### 21.2 Cache 명명 최상위 원칙

| 원칙 | 설계 기준 |
| --- | --- |
| DB 원장 원칙 | Cache는 원장이 아니라 DB 기준정보의 조회 가속 계층이다 |
| 기준정보 중심 | 공통코드, ServiceId, 권한, 메뉴, 오류코드, Timeout, 거래통제만 Cache 대상으로 한다 |
| 업무데이터 금지 | 고객정보, 계좌정보, 거래내역, 대량조회 결과는 Cache하지 않는다 |
| Cache Name 표준화 | Cache 영역명은 lowerCamelCase로 통일한다 |
| Cache Key 표준화 | Cache Key는 DOMAIN:TYPE:KEY1:KEY2 형식을 사용한다 |
| 개인정보 금지 | Cache Name, Cache Key, Cache Value에 민감정보를 넣지 않는다 |
| TTL 명확화 | Cache Name별 TTL, Max Entry, Evict 시점을 정의한다 |
| OM 통제 | 운영 삭제, 전체 삭제, 재적재, Warm-up은 OM Cache 관리에서 수행한다 |
| 변경이력 필수 | Evict, Reload, Warm-up은 이력과 감사로그를 남긴다 |
| 다중 WAS 고려 | EhCache는 WAS 로컬 Cache이므로 Version 비교 또는 전체 Evict 기준을 둔다 |

### 21.3 Cache 명명 체계 전체 구조

```text
Cache Name
↓
Cache Key
↓
Cache Value
↓
Cache Version
↓
Cache History
↓
OM Cache 관리
↓
```

거래로그 / 감사로그 / 장애추적

예시는 다음과 같다.
Cache Name  = commonCode
Cache Key   = CC:CODE:CHANNEL_ID
Cache Value = 채널 코드 목록
Version     = OM_CACHE_VERSION.commonCode
History     = OM_CACHE_HISTORY
ServiceId   = OM.Cache.evict

### 21.4 Cache Name 명명규칙

#### 21.4.1 기본 형식

{domainName}{targetName}

Cache Name은 lowerCamelCase를 사용한다.
| 구분 | 기준 |
| --- | --- |
| 표기법 | lowerCamelCase |
| 구분자 | 사용하지 않음 |
| 길이 | 30자 이내 권장 |
| 의미 | Cache 영역을 나타내야 함 |
| 예시 | commonCode, serviceCatalog, timeoutPolicy |

#### 21.4.2 표준 Cache Name

| Cache Name | 의미 | 주요 Key 예시 | TTL 권장 | Evict 시점 |
| --- | --- | --- | --- | --- |
| commonCode | 공통코드 | CC:CODE:CHANNEL_ID | 30분 | 코드 등록·수정·중지 |
| serviceCatalog | ServiceId 카탈로그 | TCF:SERVICE:SV.Customer.selectSummary | 60분 | ServiceId 등록·수정·중지 |
| authPolicy | 기능권한·데이터권한 | OM:AUTH:ROLE_ADMIN:OM.User.update | 10분 | 권한 변경 |
| menuTree | 메뉴 트리 | OM:MENU:ROLE_ADMIN | 10분 | 메뉴 변경 |
| screenServiceMap | 화면-ServiceId 매핑 | OM:SCREEN:SVC:SVLIST0001:SEARCH | 10분 | 화면/ServiceId 매핑 변경 |
| errorCode | 오류코드·메시지 | TCF:ERROR:E-TCF-HDR-0001 | 60분 | 오류코드 변경 |
| timeoutPolicy | Timeout 정책 | TCF:TIMEOUT:SV.Customer.selectSummary | 10분 | Timeout 정책 변경 |
| transactionControl | 거래통제 정책 | TCF:TXCTL:SV.Customer.selectSummary:WEBTOP | 1~5분 | 거래통제 변경 |
| gatewayRoute | Gateway 라우팅 | GW:ROUTE:PRD:SV:ONLINE | 5분 | Route 변경 |
| cacheVersion | Cache Version | OM:CACHE:VERSION:commonCode | 1분 | Version 변경 |
| sessionRegion | 세션/센터 보조정보 | TCF:SESSION_REGION:CENTER01 | 10분 | 센터/세션 정책 변경 |

기본 Cache Region은 commonCode, serviceCatalog, sessionRegion으로 시작하고, 운영 표준 확장 시 권한, 메뉴, 오류코드, Timeout, 거래통제 Cache를 추가하는 방식이 적합하다.

### 21.5 Cache Key 명명규칙

#### 21.5.1 기본 형식

{DOMAIN}:{TYPE}:{KEY1}:{KEY2}:{KEY3}

| 구성요소 | 설명 | 예시 |
| --- | --- | --- |
| DOMAIN | 업무 또는 공통영역 | CC, TCF, OM, GW, SV |
| TYPE | Cache 대상 유형 | CODE, SERVICE, AUTH, MENU |
| KEY1 | 1차 식별자 | CHANNEL_ID |
| KEY2 | 2차 식별자 | WEBTOP |
| KEY3 | 추가 식별자 | 필요 시 사용 |
Cache Key는 운영자가 로그에서 보고 의미를 알 수 있어야 한다.

| 21.5.2 표준 Cache Key 예시 | |
| 대상 | Cache Name | Cache Key |
| 공통코드 그룹 | commonCode | CC:CODE:CHANNEL_ID |
| 공통코드 단건 | commonCode | CC:CODE:CHANNEL_ID:WEBTOP |
| ServiceId | serviceCatalog | TCF:SERVICE:SV.Customer.selectSummary |
| Timeout 정책 | timeoutPolicy | TCF:TIMEOUT:SV.Customer.selectSummary |
| 거래통제 | transactionControl | TCF:TXCTL:SV.Customer.selectSummary:SV-INQ-0001:WEBTOP |
| 메뉴 트리 | menuTree | OM:MENU:ROLE_ADMIN |
| 기능권한 | authPolicy | OM:AUTH:ROLE_ADMIN:SVLIST0001:SEARCH |
| 화면-ServiceId | screenServiceMap | OM:SCREEN:SVC:SVLIST0001:SEARCH:SV.Customer.selectSummary |
| 오류코드 | errorCode | TCF:ERROR:E-TCF-HDR-0001 |
| Gateway Route | gatewayRoute | GW:ROUTE:PRD:SV:ONLINE |
| Cache Version | cacheVersion | OM:CACHE:VERSION:commonCode |

### 21.6 DOMAIN 표준

| DOMAIN | 의미 |
| --- | --- |
| 사용 예 | TCF |
| TCF Framework 공통 | ServiceId, Timeout, 거래통제 |
| CC | 공통코드 |
| 코드그룹, 코드 | OM |
| 운영관리 | 권한, 메뉴, 화면, Cache 관리 |
| GW | Gateway |
| Route, Target | JWT |
| JWT 인증 | 공개키 메타, 정책 정보 |
| SV | Single View |
| SV 기준정보 | CM |
| 캠페인 | 캠페인 기준정보 |
| MG | 메시지 |
| 메시지 기준정보 | UD |
| 파일관리 | 파일 정책 |
| BT | Batch |
| Job 정책, Scheduler 정책 |  |

### 21.7 TYPE 표준

| TYPE | 의미 |
| --- | --- |
| Cache Key 예시 | CODE |
| 공통코드 | CC:CODE:CHANNEL_ID |
| SERVICE | ServiceId |
| TCF:SERVICE:SV.Customer.selectSummary | AUTH |
| 권한 | OM:AUTH:ROLE_ADMIN:SVLIST0001:SEARCH |
| MENU | 메뉴 |
| OM:MENU:ROLE_ADMIN | SCREEN |
| 화면 | OM:SCREEN:SVLIST0001 |
| SCREEN:SVC | 화면-ServiceId 매핑 |
| OM:SCREEN:SVC:SVLIST0001:SEARCH | ERROR |
| 오류코드 | TCF:ERROR:E-TCF-HDR-0001 |
| TIMEOUT | Timeout 정책 |
| TCF:TIMEOUT:SV.Customer.selectSummary | TXCTL |
| 거래통제 | TCF:TXCTL:SV.Customer.selectSummary:WEBTOP |
| ROUTE | Gateway Route |
| GW:ROUTE:PRD:SV:ONLINE | FILE_POLICY |
| 파일 정책 | UD:FILE_POLICY:UPLOAD |
| BATCH_JOB | Batch Job 정책 |

BT:BATCH_JOB:OM-BAT-0001

### 21.8 예약 Key 명명규칙

전체 목록, 인덱스, 기본값은 예약 Key를 사용한다.

| 예약 Key | 의미 | 사용 예 |
| --- | --- | --- |
| __ALL__ | 전체 목록 | CC:CODE:__ALL__ |
| __ALL_GROUPS__ | 전체 코드그룹 | CC:CODE:__ALL_GROUPS__ |
| __DEFAULT__ | 기본 정책 | TCF:TIMEOUT:__DEFAULT__ |
| __ROOT__ | Root 메뉴 | OM:MENU:__ROOT__ |
| __VERSION__ | Cache Version | OM:CACHE:VERSION:commonCode |
| __WARMUP__ | Warm-up 대상 | OM:CACHE:WARMUP:commonCode |

공통코드 변경 시에는 특정 코드그룹뿐 아니라 전체 그룹 목록 인덱스인 __ALL_GROUPS__도 함께 Evict해야 불일치를 줄일 수 있다.

### 21.9 Cache Version 명명규칙

EhCache는 기본적으로 WAS 로컬 메모리 Cache이므로, 다중 WAS 환경에서는 Version 동기화 기준이 필요하다.
#### 21.9.1 Cache Version Key

OM:CACHE:VERSION:{cacheName}

예시:

| Cache Name | Version Key | commonCode |
| --- | --- | --- |
| OM:CACHE:VERSION:commonCode | serviceCatalog | OM:CACHE:VERSION:serviceCatalog |
| authPolicy | OM:CACHE:VERSION:authPolicy | timeoutPolicy |

OM:CACHE:VERSION:timeoutPolicy
#### 21.9.2 Version 테이블

| 테이블 | 역할 |
| --- | --- |
| OM_CACHE_VERSION | Cache Name별 Version 관리 |
| OM_CACHE_HISTORY | Cache Evict, Reload, Warm-up 이력 관리 |

OM_CACHE_VERSION은 CACHE_NAME, VERSION_NO, UPDATED_AT을 기준으로 각 WAS가 로컬 Version과 DB Version을 비교하도록 설계하고, OM_CACHE_HISTORY는 Cache 삭제·재적재 이력을 남기는 구조가 적합하다.

### 21.10 Cache Action 명명규칙

OM Cache 관리에서 수행하는 행위는 ActionCode로 표준화한다.
ActionCode
| 의미 | 설명 | INQUIRY | Cache 조회 |
| --- | --- | --- | --- |
| Cache 목록, Entry 조회 | EVICT | 단건 삭제 | 특정 Cache Key 삭제 |
| EVICT_ALL | 전체 삭제 | Cache Name 단위 전체 삭제 | RELOAD |
| 재적재 | DB 원장에서 다시 적재 | WARMUP | 선적재 |
| 기동 또는 배포 후 주요 Cache 적재 | VERSION_UP | Version 증가 | 기준정보 변경 후 Version 변경 |
| SNAPSHOT | Snapshot 조회 | 현재 WAS Cache 상태 조회 | CLEAR_LOCAL |
| 로컬 삭제 | 특정 WAS 로컬 Cache 삭제 | CLEAR_ALL_WAS | 전체 WAS 삭제 |

모든 WAS Cache 삭제

### 21.11 OM Cache ServiceId 명명규칙

OM Cache 관리 거래는 OM.Cache.{action} 형식을 사용한다.
| ServiceId | 거래코드 | 설명 |
| --- | --- | --- |
| OM.Cache.inquiry | OM-CACHE-0001 | Cache 목록 조회 |
| OM.Cache.entryInquiry | OM-CACHE-0002 | Cache Entry 조회 |
| OM.Cache.evict | OM-CACHE-0003 | 특정 Key Evict |
| OM.Cache.evictAll | OM-CACHE-0004 | Cache Name 전체 Evict |
| OM.Cache.reload | OM-CACHE-0005 | DB 기준 재적재 |
| OM.Cache.versionInquiry | OM-CACHE-0006 | Cache Version 조회 |
| OM.Cache.warmup | OM-CACHE-0007 | 주요 Cache Warm-up |
| OM.Cache.historyInquiry | OM-CACHE-0008 | Cache 변경이력 조회 |

OM Cache 관리 기능은 조회, Entry 조회, Evict, 전체 Evict, Reload, Version 조회, Warm-up, 변경이력 조회를 포함하는 구조가 적합하다.

### 21.12 Java 상수 명명규칙

Cache Name은 문자열 하드코딩을 금지하고 상수로 관리한다.
```java
public final class TcfCacheNames {
    public static final String COMMON_CODE = "commonCode";
    public static final String SERVICE_CATALOG = "serviceCatalog";
    public static final String AUTH_POLICY = "authPolicy";
    public static final String MENU_TREE = "menuTree";
    public static final String SCREEN_SERVICE_MAP = "screenServiceMap";
    public static final String ERROR_CODE = "errorCode";
    public static final String TIMEOUT_POLICY = "timeoutPolicy";
    public static final String TRANSACTION_CONTROL = "transactionControl";
    public static final String GATEWAY_ROUTE = "gatewayRoute";
    public static final String SESSION_REGION = "sessionRegion";
    private TcfCacheNames() {
    }
}
```

| 구분 | 명명규칙 | 예시 |
| --- | --- | --- |
| Java 상수명 | UPPER_SNAKE_CASE | COMMON_CODE |
| Cache Name 값 | lowerCamelCase | commonCode |
| Cache Key 조립 Method | lowerCamelCase | buildCommonCodeKey() |
| Cache Support Class | PascalCase | TcfCacheSupport |
| Cache Service Class | PascalCase | OmCacheService |

### 21.13 Cache Key Builder 명명규칙

Cache Key는 각 Service에서 문자열로 직접 조립하지 않고 Builder를 사용한다.
```java
public final class TcfCacheKeyBuilder {
    public static String commonCode(String codeGroup) {
        return "CC:CODE:" + codeGroup;
    }
    public static String commonCodeItem(String codeGroup, String code) {
        return "CC:CODE:" + codeGroup + ":" + code;
    }
    public static String serviceCatalog(String serviceId) {
        return "TCF:SERVICE:" + serviceId;
    }
    public static String timeoutPolicy(String serviceId) {
        return "TCF:TIMEOUT:" + serviceId;
    }
    public static String screenServiceMap(String screenId, String functionCode, String serviceId) {
        return "OM:SCREEN:SVC:" + screenId + ":" + functionCode + ":" + serviceId;
    }
}
```

| Builder Method | Cache Key 예시 |
| --- | --- |
| commonCode("CHANNEL_ID") | CC:CODE:CHANNEL_ID |
| serviceCatalog("SV.Customer.selectSummary") | TCF:SERVICE:SV.Customer.selectSummary |
| timeoutPolicy("SV.Customer.selectSummary") | TCF:TIMEOUT:SV.Customer.selectSummary |
| screenServiceMap("SVLIST0001", "SEARCH", "SV.Customer.selectSummary") | OM:SCREEN:SVC:SVLIST0001:SEARCH:SV.Customer.selectSummary |

### 21.14 Annotation 적용 명명규칙

#### 21.14.1 조회 Cache

@Cacheable(
    cacheNames = TcfCacheNames.COMMON_CODE,
    key = "T(com.nh.nsight.tcf.cache.TcfCacheKeyBuilder).commonCode(#codeGroup)"
)
```java
public List<CommonCode> selectCommonCodeList(String codeGroup) {
    return commonCodeMapper.selectCommonCodeList(codeGroup);
}
```

#### 21.14.2 변경 시 Evict

@CacheEvict(
    cacheNames = TcfCacheNames.COMMON_CODE,
    key = "T(com.nh.nsight.tcf.cache.TcfCacheKeyBuilder).commonCode(#command.codeGroup)"
)
```java
public void updateCommonCode(CommonCodeUpdateCommand command) {
    commonCodeMapper.updateCommonCode(command);
    cacheVersionService.increaseVersion(TcfCacheNames.COMMON_CODE);
}
```

Annotation
사용 기준
@Cacheable
기준정보 조회
@CacheEvict
기준정보 변경 후 해당 Key 삭제
@CachePut
명시적으로 Cache 값 갱신이 필요한 경우 제한 사용
@Caching
여러 Cache를 동시에 Evict해야 하는 경우

### 21.15 EhCache Alias 명명규칙

ehcache.xml의 Alias는 TcfCacheNames의 값과 반드시 일치해야 한다.
<cache alias="commonCode">
    <expiry>
        <ttl unit="minutes">30</ttl>
    </expiry>
    <resources>
        <heap unit="entries">5000</heap>
    </resources>
</cache>

<cache alias="serviceCatalog">
    <expiry>
        <ttl unit="minutes">60</ttl>
    </expiry>
    <resources>
        <heap unit="entries">2000</heap>
    </resources>
</cache>

| 점검 항목 | 기준 | Alias |
| --- | --- | --- |
| lowerCamelCase | Java 상수값 | Alias와 동일 |
| TTL | Cache Name별 명시 | Heap Entry |
| Cache Name별 최대 건수 명시 | 운영 점검 | Alias와 TcfCacheNames 불일치 금지 |

운영 점검 기준에서도 ehcache.xml Alias와 TcfCacheNames 상수 일치, classpath:ehcache.xml 존재, spring.cache.type=jcache 설정을 확인해야 한다.

### 21.16 Cache 설정 Key 명명규칙

Spring 환경설정은 다음 형식을 사용한다.
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
      version-check-enabled: true
      version-check-interval-seconds: 60
      warmup-on-startup: true
      allow-om-evict: true
```

| 설정 Key | 설명 |
| --- | --- |
| nsight.tcf.cache.enabled | Cache 사용 여부 |
| nsight.tcf.cache.config-location | EhCache 설정 파일 위치 |
| nsight.tcf.cache.version-check-enabled | Version 비교 사용 여부 |
| nsight.tcf.cache.version-check-interval-seconds | Version 확인 주기 |
| nsight.tcf.cache.warmup-on-startup | 기동 시 Warm-up 여부 |
| nsight.tcf.cache.allow-om-evict | OM Evict 허용 여부 |
| nsight.tcf.cache.default-ttl-seconds | 기본 TTL |
| nsight.tcf.cache.caches.common-code.name | 공통코드 Cache Name |
| nsight.tcf.cache.caches.common-code.ttl-seconds | 공통코드 TTL |
| nsight.tcf.cache.caches.service-catalog.name | ServiceId Cache Name |

### 21.17 Cache Log / Event 명명규칙

Cache 운영 이벤트는 로그와 감사로그에 남긴다.
CACHE-{ACTION}

| EventCode | 의미 |
| --- | --- |
| CACHE-HIT | Cache Hit |
| CACHE-MISS | Cache Miss |
| CACHE-EVICT | 특정 Key 삭제 |
| CACHE-EVICT-ALL | Cache Name 전체 삭제 |
| CACHE-RELOAD | DB 기준 재적재 |
| CACHE-WARMUP | 주요 Cache 선적재 |
| CACHE-VERSION-UP | Cache Version 증가 |
| CACHE-VERSION-MISMATCH | WAS 로컬 Version 불일치 |
| CACHE-ERROR | Cache 처리 오류 |

로그 필드는 다음을 포함한다.
| 필드 | 설명 |
| --- | --- |
| cacheName | Cache 영역명 |
| cacheKey | Cache Key |
| actionType | EVICT, RELOAD, WARMUP |
| beforeVersion | 변경 전 Version |
| afterVersion | 변경 후 Version |
| executedBy | 수행자 |
| reason | 삭제·재적재 사유 |
| apId | WAS ID |
| resultCode | 결과코드 |
| errorCode | 오류코드 |

### 21.18 Cache 오류코드 명명규칙

E-CACHE-{CATEGORY}-{NNNN}

| 오류코드 | 의미 |
| --- | --- |
| E-CACHE-NAME-0001 | 미등록 Cache Name |
| E-CACHE-KEY-0001 | 잘못된 Cache Key |
| E-CACHE-EVICT-0001 | Cache Evict 실패 |
| E-CACHE-RELOAD-0001 | Cache Reload 실패 |
| E-CACHE-VERSION-0001 | Cache Version 조회 실패 |
| E-CACHE-VERSION-0002 | Cache Version 불일치 |
| E-CACHE-WARMUP-0001 | Cache Warm-up 실패 |
| E-CACHE-CONFIG-0001 | Cache 설정 오류 |
| E-CACHE-SEC-0001 | 민감정보 Cache 감지 |

### 21.19 Cache 대상 / 비대상 기준

| 구분 | Cache 대상 여부 | 기준 |
| --- | --- | --- |
| 공통코드 | 허용 | 변경 빈도 낮고 조회 빈도 높음 |
| ServiceId 카탈로그 | 허용 | Dispatcher, 권한, 감사에서 반복 조회 |
| 메뉴 트리 | 허용 | 권한그룹별 반복 조회 |
| 기능권한 | 조건부 허용 | TTL 짧게, 변경 시 Evict |
| Timeout 정책 | 조건부 허용 | 변경 시 즉시 Evict |
| 거래통제 | 조건부 허용 | TTL 짧게, 변경 시 전체 WAS Evict |
| 오류코드 메시지 | 허용 | ETF 후처리에서 반복 조회 |
| Gateway Route | 조건부 허용 | 변경 시 Version 증가 |
| 고객정보 | 금지 | 개인정보·현행성 위험 |
| 계좌정보 | 금지 | 민감정보·정합성 위험 |
| 거래내역 | 금지 | 감사·현행성 위험 |
| 대량조회 결과 | 금지 | Heap 증가 위험 |
| 파일 데이터 | 금지 | Storage 사용 원칙 |
| JWT / Token | 금지 | 보안 위험 |
| 비밀번호 / Secret | 금지 | 보안 사고 위험 |

### 21.20 금지 명명규칙

| 금지 예시 | 문제 | 표준 예시 |
| --- | --- | --- |
| cache1 | 의미 없음 | commonCode |
| code_cache | 표기법 불일치 | commonCode |
| COMMON_CODE | Cache Name 표기법 불일치 | commonCode |
| commonCode:홍길동 | 개인정보 포함 | CC:CODE:CHANNEL_ID |
| customerInfo:C123456 | 고객정보 Cache | Cache 금지 |
| token:eyJ... | Token Cache | Cache 금지 |
| searchResult:U123:20260705113000 | 고카디널리티 Key | 조회 결과 Cache 금지 |

guid:7f9c...

| 매 거래마다 Key 증가 | GUID Key 금지 | tmpCache |
| --- | --- | --- |
| 임시명 | 정식 Cache Name 채번 | all |

범위 불명확
__ALL__, __ALL_GROUPS__

### 21.21 검토 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| Cache Name이 lowerCamelCase인가? | □ |
| Cache Key가 DOMAIN:TYPE:KEY1:KEY2 형식인가? | □ |
| Cache 대상이 기준정보·정책정보·카탈로그성 데이터인가? | □ |
| 고객정보, 계좌정보, 거래내역, Token, Secret을 Cache하지 않는가? | □ |
| ehcache.xml Alias와 TcfCacheNames 상수값이 일치하는가? | □ |
| Cache Name별 TTL과 Max Entry가 정의되어 있는가? | □ |
| 변경 거래에 @CacheEvict 또는 Version 증가가 포함되어 있는가? | □ |
| 다중 WAS 환경에서 Version 비교 또는 전체 Evict 기준이 있는가? | □ |
| OM Cache 관리 화면에서 조회·삭제·재적재가 가능한가? | □ |
| Evict, Reload, Warm-up 이력이 OM_CACHE_HISTORY에 남는가? | □ |
| Cache 운영 이벤트가 감사로그 대상인가? | □ |
| Cache 장애 시 DB 조회 Fallback이 가능한가? | □ |
| Cache Key에 GUID, Timestamp, 개인정보가 포함되지 않는가? | □ |

### 21.22 마무리말

Cache 명명규칙의 핵심은 다음과 같다.
Cache Name은 무엇을 담는지 설명하고,
Cache Key는 어떤 기준정보인지 식별하며,
Cache Version은 다중 WAS 정합성을 맞추고,
Cache History는 운영 변경을 증명한다.

NSIGHT에서는 Cache를 많이 사용하는 것이 목표가 아니다.목표는 기준정보는 빠르게 조회하되, 업무 데이터와 개인정보는 Cache하지 않는 것이다.
따라서 Cache 명명은 다음 네 가지를 표준으로 고정한다.
Cache Name    = lowerCamelCase
Cache Key     = {DOMAIN}:{TYPE}:{KEY1}:{KEY2}
Version Key   = OM:CACHE:VERSION:{cacheName}
ActionCode    = INQUIRY / EVICT / EVICT_ALL / RELOAD / WARMUP

이 기준을 적용하면 운영자는 OM Cache 관리 화면과 로그만 보고도 어떤 Cache가, 어떤 Key로, 어느 WAS에서, 언제 삭제·재적재되었는지 추적할 수 있다.

---

## 관련 Manual 장

- [43장](./43-Cache-사용-기준.md)

## 원본

- [`znsight-guide-word`](../znsight-guide-word/) — `명명규칙 상세 (20).docx`
