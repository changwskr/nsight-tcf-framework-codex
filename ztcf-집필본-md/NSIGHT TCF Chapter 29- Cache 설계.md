<!-- source: ztcf-집필본/NSIGHT TCF Chapter 29- Cache 설계.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제29장. 캐시 설계

## 도입 전 안내말

제28장에서는 로그·메트릭·추적을 이용해 어떤 거래가 느리고, 어느 SQL이나 연계 구간에서 시간이 소요됐는지 확인하는 방법을 살펴보았다.

성능 분석 결과 다음과 같은 상황이 발견될 수 있다.

\`\`\`text id=“cac29001” 공통코드 한 건을 조회할 때마다 OM DB를 반복 조회한다.

ServiceId 실행 가능 여부를 확인할 때마다 같은 정책 Table을 조회한다.

권한그룹별 메뉴를 화면 요청마다 다시 조립한다.

오류코드 메시지를 만들기 위해 매 오류 응답마다 DB를 조회한다.



이런 데이터는 다음 특성을 가진다.

\`\`\`text id="cac29002"
조회 빈도가 높다.

데이터 크기가 비교적 작다.

변경 빈도가 낮다.

여러 거래가 같은 값을 반복 사용한다.

이 경우 Cache를 적용하면 DB 조회를 줄이고 응답시간을 개선할 수 있다.

\`\`\`text id=“cac29003” 첫 요청 → Cache Miss → DB 조회 → Cache 저장

다음 요청 → Cache Hit → DB 미호출



그러나 Cache를 사용한다고 항상 시스템이 빨라지고 안정적으로 바뀌는 것은 아니다.

잘못된 Cache는 다음 문제를 만든다.

\`\`\`text id="cac29004"
DB 데이터는 변경됐지만
Cache에는 이전 값이 남아 있다.

AP01의 Cache만 삭제되고
AP02·AP03에는 이전 값이 남아 있다.

인기 Cache Key가 만료되는 순간
수천 건의 요청이 동시에 DB를 조회한다.

존재하지 않는 고객번호를 반복 조회해
매번 DB Full Scan이 발생한다.

Cache에 대량 검색결과를 저장해
JVM Heap이 부족해진다.

권한 변경 후 Cache가 삭제되지 않아
회수된 권한이 계속 허용된다.

Cache 장애 후 모든 요청이 DB로 몰려
DB Pool이 고갈된다.

따라서 Cache 설계의 핵심은 “무엇을 Cache에 저장할 것인가”보다 다음 질문에 답하는 것이다.

\`\`\`text id=“cac29005” 원본 데이터는 어디에 있는가?

Cache 값은 얼마나 오래 틀려도 되는가?

원본이 변경되면 Cache를 누가 삭제하는가?

여러 WAS의 Cache는 어떻게 함께 갱신되는가?

Cache가 비어 있거나 장애가 나도 원본 시스템을 보호할 수 있는가?

Cache에 절대로 저장하면 안 되는 데이터는 무엇인가?



Cache의 기본 원칙은 다음과 같다.

\`\`\`text id="cac29006"
DB
\= 데이터의 원본

Cache
\= 반복 조회를 줄이기 위한 임시 복제본

Cache에 값이 있다는 이유로 DB보다 Cache를 원본으로 판단해서는 안 된다.

\`\`\`text id=“cac29007” DB 값 ACTIVE

Cache 값 INACTIVE

정답 DB의 ACTIVE



현재 NSIGHT TCF의 \`tcf-cache\`는 독립 실행 WAR가 아니라 업무 WAR와 \`tcf-om\`이 사용하는 공통 Cache Library다. 업무 패키지는 Cache에 직접 의존하기보다 승인된 Cache Adapter를 통해 접근하도록 설계해야 한다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 NSIGHT TCF에서 Cache 대상·Key·TTL·무효화·다중 WAS 동기화·장애 대응·운영관리 기준을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="cac29008"
Cache 적용 대상과 비대상 구분

원본 데이터와 Cache 책임 분리

Cache Hit·Miss 처리 표준화

Cache Key 명명·보안 기준 정의

데이터별 TTL 결정기준 수립

DB 변경과 Cache 무효화 순서 정의

Transaction Commit 이후 Evict 보장

다중 WAS Cache 일관성 확보

Cache Version·Evict Event 관리

Cache Stampede·Penetration·Avalanche 방지

Cache 장애 시 원본 DB 보호

Heap·Entry 수·용량 관리

권한·인증 관련 Cache 보안 강화

Cache Metric·Dashboard·Alert 정의

OM Cache 운영·감사 체계 구축

자동검증과 품질 Gate 적용

## 적용범위

| 적용 영역 | 적용 내용 |
| --- | --- |
| 공통코드 | 코드그룹·코드 목록 |
| Service Catalog | ServiceId 기준정보 |
| 권한정책 | 기능·메뉴·데이터권한 |
| 오류코드 | 표준 오류메시지 |
| Timeout | ServiceId별 정책 |
| 거래통제 | 업무·채널·사용자·지점 통제 |
| Gateway Route | 업무코드별 Routing |
| 세션 보조정보 | Region·정책성 정보 |
| 업무 기준정보 | 승인된 소규모 불변·저변경 데이터 |
| 로컬 Cache | Ehcache·JCache |
| 분산 Cache | Redis 등 향후 적용 대상 |
| 운영관리 | 조회·Evict·Reload·Warm-up |
| 관측성 | Hit·Miss·Load·Evict·Size |
| 배포 | 초기화·Warm-up·Version 확인 |

## 대상 독자

\`\`\`text id=“cac29009” 업무 개발자

프레임워크 개발자

애플리케이션 아키텍트

데이터 아키텍트·DBA

성능·용량 담당자

보안 담당자

운영·관제 담당자

QA·장애 테스트 담당자

DevOps·배포 담당자

OM 개발·운영 담당자


\## 선행조건

\`\`\`text id="cac29010"
데이터 소유권

데이터 원본 Table

조회·변경 ServiceId

변경 빈도

허용 불일치시간

동시 사용자 수

Peak TPS

WAS·WAR 배치구조

JVM Heap

DB Pool

보안·개인정보 분류

운영 Evict 책임자

# 핵심 관점

\`\`\`text id=“cac29011” Cache는 성능을 얻는 대신 일정 시간 동안 원본과 다를 가능성을 추가하는 구조다.

따라서 Cache 적용 여부는 조회속도만으로 결정하지 않고,

얼마나 오래 틀려도 되는지와 틀렸을 때 어떤 업무위험이 생기는지를 먼저 판단해야 한다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | Cache와 원본 데이터의 차이를 설명한다. |
| 2 | Cache Hit·Miss 흐름을 설명한다. |
| 3 | Cache-Aside와 Read-Through를 구분한다. |
| 4 | Cache 적용 대상과 금지 대상을 구분한다. |
| 5 | 데이터 변경주기와 허용 불일치시간으로 TTL을 결정한다. |
| 6 | Cache Key에 포함할 업무 범위를 판단한다. |
| 7 | 사용자·지점·권한별 결과의 Key 범위를 설계한다. |
| 8 | 개인정보·Token을 Cache Key로 사용하지 않는다. |
| 9 | 변경 Transaction Commit 이후 Cache를 무효화한다. |
| 10 | Key Evict와 Region Evict를 구분한다. |
| 11 | TTL만으로 정합성을 보장할 수 없는 이유를 설명한다. |
| 12 | 다중 WAS 로컬 Cache 불일치를 설명한다. |
| 13 | Cache Version과 Evict Event를 설계한다. |
| 14 | Stampede·Penetration·Avalanche를 구분한다. |
| 15 | 빈 결과 Cache의 필요성과 위험을 설명한다. |
| 16 | Single Flight와 분산 Lock의 차이를 설명한다. |
| 17 | TTL Jitter와 Refresh-Ahead를 적용한다. |
| 18 | Cache 장애 시 Fail Open·Fail Closed를 구분한다. |
| 19 | Cache Miss 폭증으로부터 DB를 보호한다. |
| 20 | Cache Entry 수와 Heap 사용량을 산정한다. |
| 21 | Local Cache와 Distributed Cache를 비교한다. |
| 22 | Cache Hit Rate를 올리는 것만이 목표가 아님을 설명한다. |
| 23 | 권한·거래통제 Cache의 보안위험을 설명한다. |
| 24 | OM Cache 조회·삭제·Warm-up을 운영한다. |
| 25 | Cache Metric과 Alert를 정의한다. |
| 26 | Cache 장애·재기동·배포 테스트를 수행한다. |
| 27 | Cache 품질 Gate와 변경관리를 적용한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| Cache | 원본 조회를 줄이기 위한 임시 데이터 저장소 |
| Source of Truth | 최종적으로 신뢰해야 하는 원본 데이터 |
| Cache Hit | Cache에 값이 있어 원본을 조회하지 않는 상태 |
| Cache Miss | Cache에 값이 없어 원본을 조회하는 상태 |
| Hit Rate | 전체 Cache 조회 중 Hit 비율 |
| Cache-Aside | 애플리케이션이 Cache 조회 후 Miss 시 DB를 조회하는 방식 |
| Read-Through | Cache Provider가 원본 조회까지 담당하는 방식 |
| Write-Through | 원본 변경과 동시에 Cache도 동기 갱신하는 방식 |
| Write-Behind | Cache 변경 후 원본에 비동기 반영하는 방식 |
| TTL | Cache Entry의 유효시간 |
| TTI | 마지막 접근 이후 만료시간 |
| Evict | 특정 Cache Entry 삭제 |
| Evict All | Cache Region 전체 삭제 |
| Reload | 원본에서 다시 적재 |
| Warm-up | 서비스 시작 전 주요 데이터를 미리 적재 |
| Stale Data | 원본보다 오래된 Cache 데이터 |
| Negative Cache | 존재하지 않음·빈 결과를 짧게 Cache하는 방식 |
| Hot Key | 요청이 집중되는 Cache Key |
| Stampede | 인기 Key 만료 시 요청이 원본으로 동시에 몰리는 현상 |
| Penetration | 존재하지 않는 Key 요청이 Cache를 통과해 계속 원본을 조회하는 현상 |
| Avalanche | 많은 Cache Key가 동시에 만료되는 현상 |
| Single Flight | 같은 Key의 원본 조회를 한 실행으로 합치는 방식 |
| Refresh-Ahead | 만료 전에 데이터를 미리 갱신하는 방식 |
| Soft TTL | Stale 값을 임시 허용하는 논리적 만료 |
| Hard TTL | 값을 반드시 제거하는 최종 만료 |
| Cache Version | Cache 변경상태를 나타내는 번호 |
| Local Cache | 각 JVM 내부에 존재하는 Cache |
| Distributed Cache | 여러 JVM이 공유하는 외부 Cache |
| L1 Cache | JVM 로컬 1차 Cache |
| L2 Cache | Redis 등 공유 2차 Cache |
| Fail Open | Cache 실패 시 원본 DB로 처리 |
| Fail Closed | Cache 실패 시 거래를 차단 |
| Maximum Entries | Cache가 보관할 수 있는 최대 Entry 수 |
| Eviction Policy | 최대 용량 초과 시 제거기준 |
| Cache Poisoning | 잘못되거나 조작된 값을 Cache에 저장하는 공격·오류 |

\---

\# Cache 적용 판단 질문

\`\`\`text id="cac29012"
이 데이터는 여러 요청이 반복 조회하는가?

데이터 크기가 작고 상한이 명확한가?

변경 빈도가 낮은가?

원본보다 오래된 값을
일정 시간 허용할 수 있는가?

변경 시 Evict 책임자를 지정할 수 있는가?

Cache 장애 시 DB 조회로 처리할 수 있는가?

사용자·권한 범위가 Key에 정확히 반영되는가?

개인정보·Token·대형 Binary가 아닌가?

한 질문이라도 명확하지 않으면 Cache 적용을 보류한다.

# Cache 대상 판단표

| 데이터 | 기본 판단 | 주요 조건 |
| --- | --- | --- |
| 공통코드 | 적합 | 변경 시 즉시 Evict |
| Service Catalog | 적합 | 중지·변경 즉시 반영 |
| 오류코드 | 적합 | 메시지 변경 Evict |
| Timeout 정책 | 적합 | 짧은 TTL·즉시 Evict |
| 거래통제 | 조건부 | 매우 짧은 TTL·Fail Safe |
| 메뉴 트리 | 조건부 | 권한그룹별 Key |
| 기능권한 | 조건부 | 변경 즉시 Evict |
| 사용자 상태 | 매우 제한적 | 짧은 TTL·중지 즉시 반영 |
| Gateway Route | 조건부 | Route 변경 즉시 Evict |
| JWKS | 적합 | Key Rotation 대응 |
| 고객 상세 | 원칙적 금지 | 개인정보·정합성 |
| 계좌·잔액 | 금지 | 최신성·금융위험 |
| 거래내역 | 금지 | 변경·감사·대용량 |
| 검색결과 전체 | 금지 | Key 폭증·Heap |
| 파일 원본 | 금지 | 대용량 |
| Access Token | 금지 | 보안 |
| Refresh Token | 금지 | 보안 |
| DB Connection | 금지 | 기술 상태객체 |
| Transaction 객체 | 금지 | Thread·수명 문제 |

# Cache 방식 비교

| 방식 | 읽기 | 쓰기 | 장점 | 위험 | NSIGHT 판단 |
| --- | --- | --- | --- | --- | --- |
| Cache-Aside | App가 Cache→DB | DB 변경 후 Evict | 단순·통제 용이 | Evict 누락 | 기본 권장 |
| Read-Through | Cache가 원본 Load | 별도 | 호출 단순 | Provider 종속 | 제한 적용 |
| Write-Through | Cache와 DB 동기 | 동시 | 쓰기 후 일관성 | 쓰기 지연·부분실패 | 기준정보 제한 |
| Write-Behind | Cache 후 DB 비동기 | 지연 | 쓰기 성능 | 데이터 유실·순서 | 업무 데이터 금지 |
| Refresh-Ahead | 만료 전 재적재 | 원본 중심 | Hot Key 안정 | 구현 복잡 | 주요 정책 검토 |
| L1+L2 | Local+Distributed | 이벤트·버전 | 성능·공유 | 일관성 복잡 | 중장기 대안 |

NSIGHT TCF의 기본 패턴은 **DB 원장 + Cache-Aside + 변경 후 Evict**다.

# 목표 아키텍처

text id="cac29013" 사용자 요청 ↓ Gateway·업무 WAR ↓ Service ↓ Domain Cache Reader ↓ ┌───────────────────────────────┐ │ tcf-cache │ │ │ │ Spring Cache │ │ JCache │ │ Ehcache │ │ Cache Name │ │ Cache Key │ │ TTL·Max Entries │ │ Metric │ └───────────────┬───────────────┘ │ Cache Miss ↓ DAO·Mapper ↓ 원본 DB

변경 흐름:

text id="cac29014" OM·업무 변경 요청 ↓ Facade Transaction ↓ DB 원본 UPDATE ↓ 변경이력·감사 ↓ Commit ↓ Cache Evict Event ↓ 전체 WAS Cache Version 갱신 ↓ 다음 조회 시 재적재

# 현재 구현과 목표 구조

## 현재 기준 소스에서 확인되는 구성

현재 tcf-cache는 다음 구조를 가진다.

text id="cac29015" tcf-cache ├─ Spring Cache ├─ Ehcache 3 ├─ JCache ├─ TcfCacheProperties ├─ TcfCacheNames └─ TcfCacheSupport

tcf-cache는 실행 WAR가 아닌 공통 JAR다.

현재 기본 Cache Region:

| Cache Name | 용도 | TTL | 최대 Entry |
| --- | --- | --- | --- |
| commonCode | 공통코드 | 30분 | 200 |
| serviceCatalog | ServiceId 카탈로그 | 60분 | 100 |
| sessionRegion | 세션·Region 보조정보 | 10분 | 100 |

현재 공통 Template 기본값:

\`\`\`text id=“cac29016” TTL 30분

Heap 500 Entries



현재 제공 기능:

\`\`\`text id="cac29017"
@Cacheable

@CacheEvict

특정 Key Evict

Region 전체 Evict

Cache 목록 Snapshot

Cache Entry 조회

OM Cache 조회

OM Cache 삭제

삭제 사유·감사이력

## 공통코드 Cache 구현

현재 OmCommonCodeCacheService는 코드그룹을 Key로 사용한다.

\`\`\`java id=“cac29018” @Cacheable( cacheNames = TcfCacheNames.COMMON\_CODE, key = “#codeGroup” ) public List<Map<String, Object>> loadByCodeGroup(String codeGroup) {

return dao.searchCommonCodes(
Map.of("codeGroup", codeGroup)
);

}



전체 코드그룹 목록에는 별도 Key를 사용한다.

\`\`\`text id="cac29019"
\_\_ALL\_GROUPS\_\_

코드그룹 변경 시 다음 두 Key를 함께 삭제한다.

\`\`\`text id=“cac29020” 변경된 codeGroup

**ALL\_GROUPS**



이 방식은 파생 Cache를 함께 무효화한다는 점에서 적절하다.

\## OM Cache 운영 구현

현재 \`OmCacheService\`는 다음 기능을 제공한다.

\`\`\`text id="cac29021"
Cache Name·Key 조회

특정 Key 삭제

Cache 전체 삭제

삭제사유 필수

관리자 감사기록

변경이력 기록

Cache Adapter는 업무 패키지의 별도 cache 영역으로 캡슐화하고 Rule·Mapper에서 직접 Cache를 호출하지 않는 것이 패키지 설계기준이다.

## 현재 구현의 주요 Gap

| 항목 | 현재 상태 | 목표 판단 |
| --- | --- | --- |
| Local Cache | 구현 | 기본 기준정보에 적합 |
| 다중 WAS 전파 | 구현 확인 부족 | Version·Event 필요 |
| Cache Version | 설계자료 존재 | 코드 구현 필요 |
| Commit 이후 Evict | Annotation 적용 위치 확인 필요 | Transaction 동기화 필요 |
| Stampede 방지 | 확인 안 됨 | Single Flight·Jitter 필요 |
| Negative Cache | 확인 안 됨 | 짧은 TTL 적용 검토 |
| Cache Metric | JCache 통계 활성 | Micrometer 연계 필요 |
| Hit·Miss Dashboard | 확인 안 됨 | OM Metric 추가 |
| Warm-up | 운영 거래 미구현 | 주요 Cache만 적용 |
| Reload | 목표 기준 | 구현 보완 |
| Entry 실제 TTL | Snapshot은 기본값 표시 | Provider 실제 만료정보 필요 |
| Last Updated | 현재 - 표시 | 저장시각·Version 필요 |
| Entry 크기 | 확인 안 됨 | 추정·상한 필요 |
| 사용자 Key 보안 | 개별 구현 의존 | 공통 Key Builder 필요 |
| Local Evict 범위 | 현재 JVM | 전체 Instance 전파 필요 |
| Cache 장애정책 | 명시 부족 | 데이터별 Fail 정책 필요 |
| 테스트 | 설정 Default 중심 | Hit·Miss·Evict·동시성 확대 |
| Map Value | 일부 사용 | Typed 불변 DTO 권장 |

# 설계 원칙

## 원칙 1. Cache는 원본이 아니다

\`\`\`text id=“cac29022” 원본 변경 → DB Transaction

Cache 변경 → 원본 Commit 이후 반영



Cache만 변경하고 DB를 나중에 반영하는 방식은 기준정보에서도 원칙적으로 사용하지 않는다.

\## 원칙 2. 무효화 책임 없는 Cache는 만들지 않는다

Cache 적용 전 다음을 확정한다.

\`\`\`text id="cac29023"
어떤 변경 ServiceId가 값을 바꾸는가?

어느 Key를 삭제해야 하는가?

파생 Cache는 무엇인가?

다중 WAS에 어떻게 알리는가?

Evict 실패 시 누가 복구하는가?

## 원칙 3. TTL은 정합성 요구로 정한다

text id="cac29024" “일단 30분”

과 같은 일괄 기준을 사용하지 않는다.

## 원칙 4. Cache Key는 데이터 범위를 완전히 표현한다

권한·지점·언어·기준일이 결과를 바꾸면 Key에도 반영해야 한다.

## 원칙 5. Cache 장애가 DB 장애로 확대되지 않게 한다

text id="cac29025" Cache 전체 Miss → 모든 요청 DB 조회 → DB Pool 고갈

을 방어해야 한다.

# 29.1 Cache Hit·Miss와 원본 데이터

## 29.1.1 Cache Hit

text id="cac29026" Service ↓ Cache 조회 ↓ Key 존재 ↓ Cache Value 반환 ↓ DAO·Mapper 미호출

Hit 시에도 확인할 항목:

\`\`\`text id=“cac29027” Value가 Null이 아닌가?

논리적 만료시간을 넘지 않았는가?

Cache Version이 최신인가?

권한 범위가 현재 요청과 같은가?

Schema Version이 호환되는가?


\---

\## 29.1.2 Cache Miss

\`\`\`text id="cac29028"
Service
↓
Cache 조회
↓
Key 없음
↓
DAO·Mapper
↓
DB 조회
↓
Value 변환
↓
Cache 저장
↓
응답

Miss는 오류가 아니다.

다음 상황에서 정상적으로 발생한다.

\`\`\`text id=“cac29029” 최초 조회

TTL 만료

Evict 이후

애플리케이션 재기동

최대 Entry 초과로 제거

Cache Version 변경


\---

\## 29.1.3 원본 데이터

Cache 적용 문서에는 반드시 다음 항목이 있어야 한다.

| 항목 | 예 |
|---|---|
| Cache Name | \`commonCode\` |
| 원본 System | OM |
| 원본 Table | \`OM\_COMMON\_CODE\` |
| 원본 Owner | OM 운영관리 |
| 조회 Service | \`OM.CommonCode.selectByGroup\` |
| 변경 Service | \`OM.CommonCode.update\` |
| Cache Key | \`CC:CODE:{codeGroup}\` |
| TTL | 30분 |
| Evict | Commit 후 Code Group |
| 허용 Stale | 원칙적 즉시 반영 |
| 장애정책 | DB Fallback |

\---

\## 29.1.4 Cache-Aside 조회

\`\`\`java id="cac29030"
@Service
@RequiredArgsConstructor
public class CommonCodeCacheReader {

private final CommonCodeRepository repository;

@Cacheable(
cacheNames = TcfCacheNames.COMMON\_CODE,
key = "'CC:CODE:' + #codeGroup",
sync = true
)
public List<CommonCode> findByGroup(
String codeGroup) {

return List.copyOf(
repository.findByGroup(codeGroup)
);
}
}

sync=true는 같은 JVM 내 동일 Key 동시 Load를 줄이는 데 도움을 줄 수 있다.

주의:

text id="cac29031" AP01의 Lock ≠ AP02의 Lock

다중 JVM 전체 Stampede를 막지는 못한다.

## 29.1.5 Cache Value

권장:

\`\`\`text id=“cac29032” 불변 객체

작은 DTO

직렬화 가능한 값

업무 의미가 명확한 값

방어적 복사



금지:

\`\`\`text id="cac29033"
가변 List 원본

JPA Entity

MyBatis Session

DB Connection

HttpServletRequest

TransactionContext

Spring Bean

InputStream

파일 Binary

## 29.1.6 가변 객체 위험

\`\`\`java id=“cac29034” List codes = cacheReader.findByGroup(“CHANNEL”);

codes.remove(0);



Cache가 같은 List 객체를 반환하면 한 요청의 변경이 이후 모든 요청에 영향을 줄 수 있다.

권장:

\`\`\`text id="cac29035"
List.copyOf()

불변 Record

수정 불가능 Collection

## 29.1.7 DB Fallback

Cache Provider 오류:

text id="cac29036" Cache get 실패 ↓ DB 조회 ↓ 업무 계속

이 방식은 기준정보 조회에 적합할 수 있다.

그러나 다음 Cache는 단순 DB Fallback이 위험할 수 있다.

\`\`\`text id=“cac29037” 거래통제

긴급 사용자 사용중지

권한회수

Token DenyList



오래된 값이나 원본 조회 실패가 보안위험을 만들 수 있으므로 Fail Closed를 검토한다.

\---

\## 29.1.8 Cache 장애정책 분류

| 데이터 | Cache 장애 | 권장 |
|---|---|---|
| 공통코드 | DB 조회 | Fail Open |
| 오류코드 | 기본 메시지 | 제한적 Fail Open |
| Service Catalog | DB 또는 등록 Registry | 정책 결정 |
| 메뉴 | DB 조회 | Fail Open |
| Timeout 정책 | 기본 안전값 | 보수적 Fallback |
| 거래통제 | 원본 검증 실패 | Fail Closed 검토 |
| 기능권한 | 원본 검증 실패 | Fail Closed |
| DenyList | 검증 불가 | Fail Closed 또는 위험등급별 |
| Gateway Route | Route Registry | 안전한 이전 Version |

\---

\## 29.1.9 빈 결과 Cache

존재하지 않는 Key를 매번 조회하면 Cache Penetration이 발생한다.

\`\`\`text id="cac29038"
잘못된 codeGroup
UNKNOWN

요청 1
→ Cache Miss
→ DB 0건

요청 2
→ Cache Miss
→ DB 0건

Negative Cache:

text id="cac29039" UNKNOWN → EMPTY\_MARKER → TTL 30초

주의:

text id="cac29040" 빈 결과 Cache가 너무 길면 신규 데이터 등록 후에도 없음으로 보일 수 있다.

따라서 일반 값보다 짧은 TTL을 사용하고 등록 시 Evict한다.

## 29.1.10 Null Cache

Spring Cache Provider별로 Null 저장 지원방식이 다를 수 있다.

권장:

text id="cac29041" Null 직접 저장보다 명시적 Optional·Empty DTO·Marker 사용

## 29.1.11 Hit Rate 해석

text id="cac29042" Hit Rate 90%

만으로 Cache가 좋다고 판단하지 않는다.

함께 확인한다.

\`\`\`text id=“cac29043” 원본 DB 부하 감소

Stale 오류 건수

Heap 사용량

Evict 실패

Miss Load 시간

Cache 장애 시 영향

Key 수


\---

\## 29.1.12 낮은 Hit Rate 원인

\`\`\`text id="cac29044"
Key에 GUID·Timestamp 포함

요청마다 다른 불필요한 값 포함

TTL이 너무 짧음

Cache 크기가 너무 작음

조회 패턴이 반복되지 않음

사용자별 Key 폭증

반복되지 않는 데이터는 Cache 대상이 아닐 수 있다.

# 29.2 Key·TTL·무효화

## 29.2.1 Cache Key의 역할

Cache Key는 다음 질문에 답해야 한다.

\`\`\`text id=“cac29045” 어떤 데이터인가?

어느 환경·업무 범위인가?

누구의 권한 결과인가?

어느 기준일·Version의 데이터인가?


\---

\## 29.2.2 Key 명명 표준

권장 형식:

\`\`\`text id="cac29046"
{DOMAIN}:{TYPE}:{VERSION}:{SCOPE...}

예:

\`\`\`text id=“cac29047” CC:CODE:V1:CHANNEL\_ID

TCF:SERVICE:V1:SV.Customer.selectSummary

OM:MENU:V2:ROLE\_ADMIN:KO

TCF:TIMEOUT:V1:SV.Customer.selectSummary

GW:ROUTE:V1:PRD:SV


\---

\## 29.2.3 Key에 포함할 범위

| 결과를 바꾸는 조건 | Key 포함 |
|---|:---:|
| 업무코드 | 필요 시 |
| 코드그룹 | O |
| 권한그룹 | O |
| 지점 | 결과가 다르면 O |
| 사용자 | 개인화 결과일 때만 |
| 언어 | 다국어 결과면 O |
| 기준일 | 일자별 결과면 O |
| Contract Version | 호환성 필요 시 O |
| 환경 | 공유 Cache면 O |
| GUID | X |
| 요청시각 | X |
| Token 원문 | X |

\---

\## 29.2.4 사용자 범위 Key

예:

\`\`\`text id="cac29048"
OM:MENU:ROLE\_ADMIN

모든 ROLE\_ADMIN 사용자의 메뉴가 같다면 사용자 ID를 넣을 필요가 없다.

금지:

\`\`\`text id=“cac29049” OM:MENU:U000001

OM:MENU:U000002

OM:MENU:U000003



불필요한 사용자별 Key는 Entry 수를 폭증시킨다.

\---

\## 29.2.5 개인정보 Key

고객번호·계좌번호를 Cache Key로 직접 노출하지 않는다.

불가피한 승인 사례:

\`\`\`text id="cac29050"
업무상 제한된 단건 Cache

짧은 TTL

암호화·HMAC Key

접근통제

감사

최대 Entry

그러나 NSIGHT 기본 정책은 고객·계좌성 업무 데이터를 Cache하지 않는 것이다.

## 29.2.6 Key Builder

\`\`\`java id=“cac29051” public final class CacheKeys {

public static String commonCode(
String codeGroup) {

return "CC:CODE:V1:"
\+ normalize(codeGroup);
}

public static String timeoutPolicy(
String serviceId) {

return "TCF:TIMEOUT:V1:"
\+ normalize(serviceId);
}

private static String normalize(String value) {
if (value == null
|| !value.matches(
"\[A-Za-z0-9.\_-\]{1,100}")) {
throw new IllegalArgumentException(
"Invalid cache key component"
);
}
return value;
}

private CacheKeys() {
}

}



업무별 문자열 결합을 반복하지 않는다.

\---

\## 29.2.7 TTL 결정요소

\`\`\`text id="cac29052"
원본 변경주기

허용 Stale 시간

조회량

원본 조회비용

Cache 크기

장애 시 Fallback

Evict 전파 신뢰도

보안 중요도

## 29.2.8 TTL 결정 예

| 데이터 | 변경 빈도 | 허용 불일치 | TTL 예 |
| --- | --- | --- | --- |
| 공통코드 | 낮음 | 수분 | 30분+Evict |
| 오류코드 | 낮음 | 수분 | 60분+Evict |
| Service Catalog | 낮음 | 짧게 | 60분+Evict |
| 메뉴 | 중간 | 수분 | 10분+Evict |
| 권한 | 중간 | 매우 짧게 | 1~10분+즉시 Evict |
| Timeout | 중간 | 매우 짧게 | 5~10분+Evict |
| 거래통제 | 긴급 변경 | 거의 0 | 1분 이하+Event |
| JWKS | Rotation 기준 | Key 겹침기간 | 수시간+Refresh |
| DenyList | 수시 | 0 | Token 잔여수명 |

표의 값은 출발점이며 프로젝트 측정값과 업무 위험으로 확정한다.

## 29.2.9 TTL만 사용하면 안 되는 이유

\`\`\`text id=“cac29053” 권한 TTL 10분

14:00 권한 회수

14:00~14:10 기존 권한 계속 사용 가능



보안·통제 정보는 변경 시 즉시 Evict가 필요하다.

\---

\## 29.2.10 TTL Jitter

모든 Entry TTL이 정확히 30분이면 동시에 적재된 값이 동시에 만료될 수 있다.

\`\`\`text id="cac29054"
30분
± 10%

예:

text id="cac29055" 27분~33분

로 분산해 Avalanche를 줄일 수 있다.

Ehcache XML의 고정 TTL만 사용할 경우 애플리케이션 수준 Refresh 분산이나 Cache별 기동 Warm-up 분산을 검토한다.

## 29.2.11 Soft TTL·Hard TTL

\`\`\`text id=“cac29056” Soft TTL 10분

Hard TTL 30분



동작:

\`\`\`text id="cac29057"
0~10분
→ Fresh 반환

10~30분
→ Stale 반환 가능
→ Background Refresh

30분 초과
→ 반환 금지
→ 원본 Load

오래된 데이터를 일정 시간 허용할 수 있는 기준정보에만 적용한다.

## 29.2.12 무효화 우선순위

text id="cac29058" 1. 특정 Key Evict 2. 관련 파생 Key Evict 3. Cache Version 증가 4. 필요 시 Region Evict 5. 전체 WAS 전파

무조건 allEntries=true를 사용하면 Cache Miss 폭증이 발생할 수 있다.

## 29.2.13 변경 Transaction과 Evict

잘못된 순서:

text id="cac29059" Cache Evict ↓ DB UPDATE ↓ DB Rollback

결과:

\`\`\`text id=“cac29060” DB는 이전 값

Cache는 삭제됨

다음 조회에서 이전 값 재적재



데이터 오류는 아니지만 불필요한 Miss와 부하가 발생한다.

더 위험한 순서:

\`\`\`text id="cac29061"
DB UPDATE

Cache 새 값 Put

DB Rollback

결과:

\`\`\`text id=“cac29062” DB는 이전 값

Cache는 Rollback되지 않은 새 값


\---

\## 29.2.14 Commit 이후 Evict

권장:

\`\`\`text id="cac29063"
Facade Transaction
↓
DB 변경
↓
변경이력
↓
Commit
↓
AFTER\_COMMIT Event
↓
Cache Evict

예:

\`\`\`java id=“cac29064” @Transactional public void updateCommonCode( CommonCodeUpdateCommand command) {

repository.update(command);
historyRepository.insert(command);

eventPublisher.publishEvent(
new CommonCodeChangedEvent(
command.codeGroup()
)
);

}


\`\`\`java id="cac29065"
@TransactionalEventListener(
phase = TransactionPhase.AFTER\_COMMIT
)
public void onChanged(
CommonCodeChangedEvent event) {

cacheSupport.evict(
TcfCacheNames.COMMON\_CODE,
CacheKeys.commonCode(
event.codeGroup()
)
);
}

## 29.2.15 Evict 실패

DB Commit은 완료됐지만 Evict가 실패할 수 있다.

\`\`\`text id=“cac29066” DB 변경 성공

Cache Evict 실패



대응:

\`\`\`text id="cac29067"
Cache Version 증가

Evict Event Outbox

재시도

짧은 TTL

운영 Alert

OM 수동 Evict

DB Transaction을 다시 Rollback할 수는 없다.

## 29.2.16 다중 WAS 문제

\`\`\`text id=“cac29068” AP01 Cache Version 5

AP02 Cache Version 4

AP03 Cache Version 4



AP01에서만 Evict하면 사용자 요청이 어느 서버로 라우팅되는지에 따라 결과가 달라진다.

다중 WAS 환경에서는 Cache 대상·TTL·무효화·장애 대응을 별도 아키텍처 검토항목으로 관리해야 한다.

\---

\## 29.2.17 다중 WAS 동기화 대안

| 방식 | 장점 | 단점 | 판단 |
|---|---|---|---|
| TTL만 사용 | 단순 | Stale 지속 | 보조수단 |
| 모든 Instance API Evict | 즉시 | 대상 누락 가능 | 기본 운영수단 |
| DB Cache Version Polling | 안정 | Polling 지연 | 권장 |
| Message Event | 빠름 | Broker 의존 | 권장 확장 |
| Redis 공유 Cache | 일관된 저장소 | 네트워크·운영 복잡 | 중장기 |
| 배포 시 전체 초기화 | 확실 | Cold Start | 배포 기준 |

\---

\## 29.2.18 Cache Version

\`\`\`sql id="cac29069"
CREATE TABLE OM\_CACHE\_VERSION (
CACHE\_NAME VARCHAR2(100) NOT NULL,
VERSION\_NO NUMBER(18) NOT NULL,
UPDATED\_BY VARCHAR2(50),
UPDATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_OM\_CACHE\_VERSION
PRIMARY KEY (CACHE\_NAME)
);

흐름:

text id="cac29070" OM 기준정보 변경 ↓ DB Commit ↓ CACHE\_VERSION 6 ↓ 각 WAS Version 확인 ↓ 로컬 Version 5 발견 ↓ Evict·Reload ↓ 로컬 Version 6

## 29.2.19 Version 범위

전체 Cache Name 단위:

text id="cac29071" commonCode Version 10

Key 단위:

text id="cac29072" commonCode:CHANNEL\_ID Version 7

Key 단위는 정밀하지만 관리량이 증가한다.

기준정보 규모에 맞춰 선택한다.

## 29.2.20 배포 시 Cache

배포 절차:

\`\`\`text id=“cac29073” 신규 WAR 기동

Cache Empty

필수 설정 확인

주요 Cache Warm-up

Readiness UP

트래픽 복귀



여러 WAS가 동시에 Warm-up하면 DB 부하가 증가할 수 있다.

Rolling으로 한 Instance씩 수행하고 Warm-up 간격을 둔다.

\---

\## 29.2.21 OM Cache 운영 ServiceId

권장:

| ServiceId | 기능 |
|---|---|
| \`OM.Cache.inquiry\` | Cache 목록 조회 |
| \`OM.Cache.entryInquiry\` | Entry 조회 |
| \`OM.Cache.evict\` | 특정 Key 삭제 |
| \`OM.Cache.evictAll\` | Region 전체 삭제 |
| \`OM.Cache.reload\` | 원본 재적재 |
| \`OM.Cache.versionInquiry\` | Version 조회 |
| \`OM.Cache.warmup\` | 주요 Cache 적재 |
| \`OM.Cache.historyInquiry\` | 변경이력 조회 |

Evict·Reload·Warm-up은 운영 영향이 있으므로 권한·사유·감사 대상이다.

\---

\# 29.3 Cache Stampede와 장애

\## 29.3.1 Cache Stampede

\`\`\`text id="cac29074"
인기 Key
Tcf Service Catalog

TTL 만료
↓
동시에 1,000개 요청
↓
모두 Cache Miss
↓
1,000개 DB 조회

Cache가 DB를 보호하는 대신 순간적으로 DB 부하를 증폭시킨다.

## 29.3.2 Single Flight

같은 JVM에서 같은 Key Load를 하나로 합친다.

\`\`\`text id=“cac29075” Thread 1 → DB 조회

Thread 2~100 → Thread 1 결과 대기



Spring Cache \`sync=true\` 또는 별도 Key Lock을 검토한다.

주의:

\`\`\`text id="cac29076"
Lock 대기 Timeout

Load 실패

Lock 정리

다중 JVM

## 29.3.3 분산 Single Flight

다중 WAS 전체에서 한 Instance만 원본을 Load하려면 다음이 필요할 수 있다.

\`\`\`text id=“cac29077” 분산 Lock

Redis SET NX

Lease Time

Lock Owner

Fencing Token



구현 복잡성이 커지므로 기준정보 중요도와 원본 부하를 기준으로 적용한다.

\---

\## 29.3.4 Refresh-Ahead

\`\`\`text id="cac29078"
TTL 30분

25분 경과
→ Background Refresh

기존 값
→ 계속 제공

Hot Key 만료 순간의 Miss를 줄인다.

주의:

\`\`\`text id=“cac29079” 갱신 실패 시 Stale 허용시간

동시 Refresh 방지

Background Thread 상한

원본 DB 부하


\---

\## 29.3.5 Cache Avalanche

\`\`\`text id="cac29080"
애플리케이션 09:00 기동

모든 Cache 09:00 적재

09:30
모든 30분 TTL 동시 만료

대응:

\`\`\`text id=“cac29081” TTL Jitter

Cache별 다른 TTL

분산 Warm-up

Refresh-Ahead

Instance별 기동 간격


\---

\## 29.3.6 Cache Penetration

\`\`\`text id="cac29082"
존재하지 않는 고객·코드 Key

공격·오류 요청 반복
↓
항상 Cache Miss
↓
항상 DB 조회

대응:

\`\`\`text id=“cac29083” 입력 Validation

Negative Cache

Bloom Filter 검토

Rate Limit

오류요청 Metric


\---

\## 29.3.7 Hot Key

\`\`\`text id="cac29084"
TCF:SERVICE:SV.Customer.selectSummary

같은 Key에 요청이 집중되면 다음 문제가 생길 수 있다.

\`\`\`text id=“cac29085” Lock 경합

Serialization 비용

Network 집중

만료순간 Stampede



Hot Key Metric과 Refresh 전략이 필요하다.

\---

\## 29.3.8 Big Key

\`\`\`text id="cac29086"
한 Cache Entry
고객목록 100만 건

문제:

\`\`\`text id=“cac29087” Heap 고갈

GC Pause

Serialization 지연

Evict 지연

Network 전송



Cache Entry 크기 상한을 둔다.

\---

\## 29.3.9 JVM Heap 산정

대략적인 계산:

\`\`\`text id="cac29088"
Cache Heap
≈ Entry 수
× Entry 평균 크기
× 객체 Overhead 보정

예:

\`\`\`text id=“cac29089” 5,000 Entries

평균 Value 4KB

Key·객체 Overhead 2배 가정

약 40MB



실제 Java 객체는 문자열·Map·List 구조에 따라 더 커질 수 있으므로 Heap Dump·JFR 등으로 측정한다.

\---

\## 29.3.10 다중 WAS 총 메모리

Local Cache는 WAS마다 복제된다.

\`\`\`text id="cac29090"
WAS 4대

WAR 9개

WAR별 Cache 50MB

이론적 총 사용량:

text id="cac29091" 4 × 9 × 50MB = 1.8GB

각 JVM Heap뿐 아니라 전체 인프라 메모리와 DB Miss 부하를 함께 계산한다.

## 29.3.11 최대 Entry

TTL만 설정하고 최대 Entry가 없으면 지속적으로 새로운 Key가 유입돼 Heap이 증가할 수 있다.

\`\`\`text id=“cac29092” TTL

-   Maximum Entries
-   Entry 크기 상한
-   Eviction Policy



를 함께 설정한다.

\---

\## 29.3.12 Cache Provider 장애

Local Ehcache는 외부 Network 장애는 없지만 다음 장애가 가능하다.

\`\`\`text id="cac29093"
JVM OOM

CacheManager 초기화 실패

설정 XML 오류

ClassLoader 문제

Serialization 오류

Concurrent Load 실패

Provider 내부 예외

Distributed Cache는 추가로 다음 장애가 있다.

\`\`\`text id=“cac29094” Network Timeout

Cluster Failover

Split Brain

Connection Pool 고갈

Replication Lag

Authentication 실패


\---

\## 29.3.13 Cache 장애 시 DB 보호

단순 Fallback:

\`\`\`text id="cac29095"
Cache 장애
→ 모든 요청 DB

문제:

\`\`\`text id=“cac29096” DB Pool 폭증

SQL 증가

Thread 대기

전체 거래 Timeout



방어:

\`\`\`text id="cac29097"
DB Fallback 동시성 제한

Rate Limit

Bulkhead

짧은 Local Emergency Cache

Stale Value 허용

기본 안전정책

거래통제

## 29.3.14 Stale-While-Revalidate

조건부 패턴:

\`\`\`text id=“cac29098” Fresh 만료

원본 조회 중 ↓ 기존 Stale 값 임시 반환



적합:

\`\`\`text id="cac29099"
오류메시지

공통코드

메뉴

비중요 화면표시

부적합:

\`\`\`text id=“cac29100” 권한 회수

거래통제

고객 잔액

긴급 사용중지


\---

\## 29.3.15 안전 기본값

Timeout Cache 오류:

\`\`\`text id="cac29101"
기본 Timeout
5초

같은 안전한 기본값을 사용할 수 있다.

거래통제 Cache 오류:

text id="cac29102" 허용으로 기본 처리

는 보안·운영 위험이 크다.

데이터 유형별 기본값을 정의한다.

## 29.3.16 Cache Warm-up 실패

\`\`\`text id=“cac29103” WAR 기동

Warm-up DB 오류

Readiness 판단



필수 Cache:

\`\`\`text id="cac29104"
Readiness DOWN

선택 Cache:

\`\`\`text id=“cac29105” Readiness UP

첫 요청 Lazy Load

경고 Metric


\---

\## 29.3.17 Region 전체 Evict

\`\`\`text id="cac29106"
OM 운영자

evictAll commonCode

영향:

\`\`\`text id=“cac29107” 모든 코드그룹 동시 Miss

전체 업무 DB 조회

응답시간 상승



따라서:

\`\`\`text id="cac29108"
사유

승인

영향 예상

실행시간

Warm-up

Metric 관찰

감사

가 필요하다.

## 29.3.18 Evict Event 유실

\`\`\`text id=“cac29109” OM DB 변경

Evict Event 발행

Broker 장애

일부 WAS 미수신



보완:

\`\`\`text id="cac29110"
Cache Version Polling

Event Outbox

재전송

Instance별 적용상태

TTL

Event만을 유일한 정합성 수단으로 사용하지 않는다.

## 29.3.19 Instance별 상태

OM Dashboard:

| Instance | Cache | Local Version | Target Version | Entry | 상태 |
| --- | --- | --- | --- | --- | --- |
| AP01 | commonCode | 12 | 12 | 85 | 정상 |
| AP02 | commonCode | 11 | 12 | 84 | 불일치 |
| AP03 | commonCode | 12 | 12 | 85 | 정상 |

불일치 Instance에 Evict·Reload를 재실행한다.

# 29.4 적용하면 안 되는 데이터

## 29.4.1 고객 개인정보

\`\`\`text id=“cac29111” 고객명

주민등록번호

주소

전화번호

신용정보

고객 상세 프로필



원칙적으로 Cache하지 않는다.

이유:

\`\`\`text id="cac29112"
개인정보 복제범위 증가

삭제·정정 반영 어려움

권한 변경 반영 위험

Heap Dump 노출

감사·파기 복잡

## 29.4.2 계좌·잔액·금액

\`\`\`text id=“cac29113” 계좌잔액

한도

실시간 실적

승인 금액

거래 상태



오래된 값이 업무 판단에 직접 영향을 주므로 Cache 금지다.

별도 Read Model이 필요하면 다음을 명확히 표시한다.

\`\`\`text id="cac29114"
조회 기준시각

지연시간

분석용 데이터

원장 아님

## 29.4.3 거래내역·상담이력

\`\`\`text id=“cac29115” 변경 가능성

대량 데이터

정렬·페이징

권한

감사



문제로 일반 Local Cache에 저장하지 않는다.

\---

\## 29.4.4 대량 검색결과

금지:

\`\`\`text id="cac29116"
searchResult:{
userId
}:{
timestamp
}

이유:

\`\`\`text id=“cac29117” 재사용률 낮음

Key 폭증

Heap 고갈

권한 범위 복잡

정렬·조건 조합 폭증



검색 성능은 Index·SQL·페이징·Read Model로 해결한다.

\---

\## 29.4.5 Token·Secret

절대 금지:

\`\`\`text id="cac29118"
Access Token 원문

Refresh Token 원문

비밀번호

JWT Private Key

OAuth Client Secret

API Key

인증서 비밀번호

Token 상태관리가 필요하면 다음 최소 정보만 승인된 저장소에 둔다.

\`\`\`text id=“cac29119” jti

tokenVersion

deny 상태

만료시각

Token Hash



권한·사용자 정책 변경은 관련 Cache와 Token Version에 즉시 반영해야 하며, 모든 요청에서 OM DB를 직접 조회하는 방식도 피해야 한다.

\---

\## 29.4.6 파일·Binary

\`\`\`text id="cac29120"
Excel

PDF

CSV 전체

Image

압축파일

보고서 Binary

Local Heap Cache에 저장하지 않는다.

대안:

\`\`\`text id=“cac29121” 파일 Storage

Object Storage

Download Token

Metadata Cache



파일 자체가 아니라 작은 Metadata만 Cache할 수 있다.

\---

\## 29.4.7 DB·Transaction 객체

금지:

\`\`\`text id="cac29122"
Connection

SqlSession

ResultSet

TransactionStatus

EntityManager

Statement

수명과 Thread가 제한된 기술객체다.

## 29.4.8 Request·Session 객체

금지:

\`\`\`text id=“cac29123” HttpServletRequest

HttpServletResponse

HttpSession

AuthenticationContext

TransactionContext

MDC Map



요청 간 사용자정보가 혼입될 수 있다.

\---

\## 29.4.9 가변 Domain 객체

변경 가능한 객체를 Cache한 뒤 업무 코드에서 수정하면 원본과 관계없이 Cache 상태가 변한다.

\`\`\`text id="cac29124"
Campaign domainObject

domainObject.approve()

Cache 값도 변경

Cache Value는 불변 Snapshot DTO를 사용한다.

## 29.4.10 권한 결정 결과

다음과 같은 최종 Boolean만 장시간 Cache하면 위험하다.

text id="cac29125" U001 canDownload=true

권한 Cache가 필요하다면 다음을 포함한다.

\`\`\`text id=“cac29126” 권한 Version

권한그룹

ServiceId

데이터 범위

짧은 TTL

즉시 Evict

사용자 상태


\---

\## 29.4.11 거래통제 결과

\`\`\`text id="cac29127"
ServiceId 실행 허용=true

는 긴급 장애통제 변경이 즉시 반영돼야 한다.

장시간 TTL만 적용해서는 안 된다.

## 29.4.12 오류 객체

Exception·Stack Trace를 Cache하지 않는다.

일시적인 DB 장애 결과가 TTL 동안 모든 사용자에게 반복될 수 있다.

\`\`\`text id=“cac29128” DB Timeout

→ Exception Cache

→ 30분 동안 모두 같은 실패



오류결과는 일반적으로 Cache하지 않는다.

외부 장애 Circuit 상태는 전용 복원력 구성요소가 관리한다.

\---

\## 29.4.13 페이지·화면 객체

금지:

\`\`\`text id="cac29129"
WebTopSuite Component

React State

화면 전체 Response

사용자별 검색조건

HTML

Cache는 화면 세션 저장소가 아니다.

## 29.4.14 무제한 Collection

Entry 하나에 다음을 저장하지 않는다.

\`\`\`text id=“cac29130” 전체 고객

전체 캠페인 대상

전체 거래내역

전체 로그



상한이 없는 데이터는 Cache 대상이 아니다.

\---

\# 표준 Cache Region

| Cache Name | 대상 | Key | TTL 출발점 | 무효화 |
|---|---|---|---:|---|
| \`commonCode\` | 공통코드 | \`CC:CODE:{group}\` | 30분 | 그룹 변경 |
| \`serviceCatalog\` | ServiceId | \`TCF:SERVICE:{id}\` | 60분 | 등록·중지 |
| \`errorCode\` | 오류코드 | \`TCF:ERROR:{code}\` | 60분 | 메시지 변경 |
| \`timeoutPolicy\` | Timeout | \`TCF:TIMEOUT:{id}\` | 5~10분 | 정책 변경 |
| \`transactionControl\` | 통제 | 정책범위 조합 | 1분 이하 | 즉시 |
| \`authPolicy\` | 권한 | 그룹·ServiceId | 1~10분 | 즉시 |
| \`menuTree\` | 메뉴 | 그룹·언어 | 10분 | 메뉴 변경 |
| \`gatewayRoute\` | Route | 환경·업무 | 5~10분 | Route 변경 |
| \`jwks\` | 공개키 | issuer·kid | Rotation 기준 | Key 변경 |
| \`sessionRegion\` | 보조정보 | Region | 10분 | 정책 변경 |

TTL은 프로젝트 운영값으로 확정한다.

\---

\# 표준 설정 예

\`\`\`yaml id="cac29131"
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

stampede:
single-flight-enabled: true
load-wait-timeout-ms: 1000
ttl-jitter-percent: 10

fallback:
max-concurrent-db-loads: 30
allow-stale-reference-data: true

caches:
common-code:
name: commonCode
ttl-seconds: 1800
negative-ttl-seconds: 30
max-entries: 5000

service-catalog:
name: serviceCatalog
ttl-seconds: 3600
max-entries: 2000

auth-policy:
name: authPolicy
ttl-seconds: 300
max-entries: 10000
fail-closed: true

transaction-control:
name: transactionControl
ttl-seconds: 60
max-entries: 50000
fail-closed: true

# Cache 관리 테이블

## Cache Version

sql id="cac29132" CREATE TABLE OM\_CACHE\_VERSION ( CACHE\_NAME VARCHAR2(100) NOT NULL, SCOPE\_KEY VARCHAR2(300) DEFAULT '\*' NOT NULL, VERSION\_NO NUMBER(18) NOT NULL, UPDATED\_BY VARCHAR2(50) NOT NULL, UPDATED\_AT TIMESTAMP NOT NULL, CONSTRAINT PK\_OM\_CACHE\_VERSION PRIMARY KEY ( CACHE\_NAME, SCOPE\_KEY ) );

## Cache 변경이력

sql id="cac29133" CREATE TABLE OM\_CACHE\_HISTORY ( HISTORY\_ID VARCHAR2(64) NOT NULL, CACHE\_NAME VARCHAR2(100) NOT NULL, CACHE\_KEY VARCHAR2(300), ACTION\_TYPE VARCHAR2(30) NOT NULL, BEFORE\_VERSION NUMBER(18), AFTER\_VERSION NUMBER(18), REASON VARCHAR2(500) NOT NULL, TARGET\_INSTANCES VARCHAR2(2000), SUCCESS\_COUNT NUMBER(10), FAIL\_COUNT NUMBER(10), EXECUTED\_BY VARCHAR2(50) NOT NULL, EXECUTED\_AT TIMESTAMP NOT NULL, GUID VARCHAR2(100) NOT NULL, CONSTRAINT PK\_OM\_CACHE\_HISTORY PRIMARY KEY (HISTORY\_ID) );

# 정상 처리 흐름

## Cache Hit

text id="cac29134" 1. 사용자가 기준정보 조회를 요청한다. 2. Service가 Cache Reader를 호출한다. 3. Cache Name과 Key를 생성한다. 4. Local Version과 Target Version을 비교한다. 5. Cache Entry가 존재하고 유효하다. 6. Cache Hit Metric을 증가시킨다. 7. DB를 호출하지 않고 값을 반환한다. 8. 거래가 정상 종료된다.

## Cache Miss

text id="cac29135" 1. Cache Entry가 없거나 만료된다. 2. Single Flight Lock을 획득한다. 3. 다른 Thread가 이미 Load했는지 다시 확인한다. 4. DAO·Mapper로 원본 DB를 조회한다. 5. 결과를 불변 DTO로 변환한다. 6. Cache에 저장한다. 7. Load 시간과 결과를 Metric에 기록한다. 8. Lock을 해제한다. 9. 값을 반환한다.

## 기준정보 변경

text id="cac29136" 1. OM 관리자가 기준정보 변경을 요청한다. 2. 권한과 변경사유를 검증한다. 3. Facade Transaction을 시작한다. 4. DB 원본을 변경한다. 5. 변경이력과 감사로그를 저장한다. 6. Transaction을 Commit한다. 7. Cache Version을 증가시킨다. 8. Evict Event를 발행한다. 9. 각 Instance가 해당 Key를 Evict한다. 10. 적용결과를 OM에 기록한다. 11. 다음 조회에서 최신 원본을 적재한다.

# 오류·Timeout·장애 흐름

## Cache Load DB 오류

text id="cac29137" Cache Miss ↓ DB 조회 ↓ DB Timeout ↓ Cache 저장 없음 ↓ 업무 정책에 따른 오류 ↓ Load Failure Metric

DB 오류를 Cache Entry로 저장하지 않는다.

## Cache Provider 오류

text id="cac29138" Cache Get 예외 ↓ 데이터 유형 확인 ├─ 공통코드 │ → 제한 DB Fallback ├─ 권한·통제 │ → Fail Closed 또는 원본 검증 └─ 선택 데이터 → 기본값·기능축소

## Stampede

text id="cac29139" Hot Key 만료 ↓ 동일 Key 요청 폭증 ↓ Single Flight ↓ 한 요청만 DB 조회 ↓ 나머지 대기 ↓ 대기 Timeout 시 Stale·빠른 실패

## 다중 WAS 불일치

text id="cac29140" AP02 Version 불일치 감지 ↓ 해당 Cache Evict ↓ 새 Version 적재 ↓ OM 상태 정상화

## Evict 실패

text id="cac29141" DB 변경 Commit ↓ AP03 Evict 실패 ↓ History FAIL 1 ↓ 재전송 ↓ Version Polling 감지 ↓ AP03 Evict·Reload

## Cache 전체 장애

text id="cac29142" Cache 사용불가 ↓ Fallback 동시성 제한 ↓ DB 보호 Rate Limit ↓ 중요도 낮은 기능 축소 ↓ 통제·권한 거래 Fail Closed ↓ 운영 경보

# 정상 예시

\`\`\`text id=“cac29143” Cache commonCode

Key CC:CODE:V1:CHANNEL\_ID

원본 OM\_COMMON\_CODE

TTL 1,800초

최대 Entry 5,000

첫 요청 Miss → DB 12건 → Cache 저장

두 번째 요청 Hit → DB 미호출

코드 변경 DB Commit → Version 증가 → 전체 WAS Evict

다음 요청 Miss → 변경된 13건 적재

결과 모든 WAS 동일


\---

\# 금지 예시

\`\`\`text id="cac29144"
Cache를 데이터 원본으로 사용한다.

DB 변경 없이 Cache 값만 수정한다.

Cache 적용 전에 원본 Owner를 정하지 않는다.

TTL을 모든 Cache에 30분으로 일괄 적용한다.

권한·거래통제 Cache를 TTL 만으로 갱신한다.

DB Transaction Commit 전에 새 값을 Cache에 Put한다.

DB 변경 후 Cache Evict를 누락한다.

AP01 Cache만 삭제하고 전체 반영으로 판단한다.

다중 WAS에서 Local Cache Version을 확인하지 않는다.

모든 변경에 \`allEntries=true\`를 사용한다.

Cache 전체 삭제 후 DB 부하를 확인하지 않는다.

GUID·TraceId를 Cache Key로 사용한다.

요청시각을 Cache Key로 사용한다.

Access Token을 Cache Key나 Value로 사용한다.

고객번호·계좌번호를 평문 Key로 사용한다.

고객 상세정보와 계좌잔액을 Cache한다.

대량 검색결과를 Local Heap에 저장한다.

JPA Entity·DB Connection·Request 객체를 Cache한다.

가변 List를 그대로 반환한다.

존재하지 않는 Key를 무한히 DB 조회한다.

오류 Exception을 Cache한다.

TTL 만료시 모든 요청이 동시에 DB를 조회하게 한다.

Cache 장애 시 제한 없이 모든 요청을 DB로 전달한다.

Cache Hit Rate만 보고 성공으로 판단한다.

Cache Entry 수와 Heap 크기를 산정하지 않는다.

운영자가 사유 없이 Cache 전체를 삭제한다.

Cache Evict 실패를 로그만 남기고 방치한다.

Cache Metric과 Instance Version을 수집하지 않는다.

# 성능·용량·확장성

## Entry 수 산정

text id="cac29145" Entry 수 = Cache 가능한 고유 Key 수 × Scope 조합

예:

\`\`\`text id=“cac29146” 권한그룹 100

ServiceId 500

기능권한 조합 최대 50,000 Entries


\---

\## Heap 산정

\`\`\`text id="cac29147"
Cache Heap 예상
\=
Entry Count
× 평균 직렬화 크기
× 객체 Overhead 계수

Map 중심 Value는 DTO보다 객체 수와 Overhead가 커질 수 있다.

Typed 불변 DTO를 권장한다.

## Cache Hit 효과

text id="cac29148" 원본 조회 TPS = 전체 조회 TPS × Miss Rate

예:

\`\`\`text id=“cac29149” 전체 1,000 TPS

Hit Rate 95%

DB 조회 50 TPS



Cache 전체 초기화 후 Hit Rate가 0%가 되면 DB는 순간적으로 1,000 TPS를 받을 수 있다.

\---

\## Warm-up 용량

Warm-up은 다음을 제한한다.

\`\`\`text id="cac29150"
동시 Load 수

Cache별 순서

Instance별 간격

DB Connection 수

전체 Timeout

## Local Cache와 Distributed Cache 비교

| 기준 | Local Ehcache | Distributed Cache |
| --- | --- | --- |
| 조회속도 | 매우 빠름 | Network 비용 |
| 외부 의존 | 없음 | Cache Cluster |
| 다중 WAS 일관성 | 별도 전파 | 공유 |
| Heap | JVM 사용 | 외부 사용 |
| 장애범위 | 해당 JVM | Cluster 영향 |
| 운영복잡도 | 낮음 | 높음 |
| 적합 | 기준정보 | 공유 상태·대규모 |
| 보안 | Heap Dump 고려 | Network·저장 암호화 |

NSIGHT의 현재 기본은 기준정보성 Local Cache이며, 강한 공유 일관성이나 대규모 공통 상태가 필요한 경우에만 분산 Cache를 검토한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| Cache 대상 | 기준·정책정보 중심 |
| 개인정보 | 원칙적 금지 |
| Token·Secret | 저장 금지 |
| Key | 평문 민감정보 금지 |
| Value | 불변·최소 필드 |
| Heap Dump | 접근통제·암호화 |
| 운영 조회 | Entry Value 노출 제한 |
| Evict | 권한·사유 |
| Evict All | 관리자·이중승인 검토 |
| Reload | 감사 대상 |
| 권한 Cache | 즉시 Evict |
| 거래통제 | Fail Closed 검토 |
| 분산 Cache | TLS·인증·암호화 |
| 변경이력 | GUID·실행자·결과 |

OM 정책·권한 변경은 Commit 후 관련 Cache를 무효화하고 Gateway·업무 WAR·JWT 계층에 반영해야 한다.

# 운영·모니터링·장애 대응

## 핵심 Metric

\`\`\`text id=“cac29151” cache.request.count

cache.hit.count

cache.miss.count

cache.hit.ratio

cache.load.count

cache.load.duration

cache.load.failure.count

cache.eviction.count

cache.evict.manual.count

cache.entry.count

cache.estimated.size.bytes

cache.version.mismatch.count

cache.stale.served.count

cache.negative.hit.count

cache.singleflight.wait.count

cache.singleflight.timeout.count

cache.db.fallback.count

cache.warmup.duration


\---

\## 권장 Label

\`\`\`text id="cac29152"
cacheName

result

instanceId

businessCode

loadSource

금지:

\`\`\`text id=“cac29153” cacheKey

userId

customerNo

guid

traceId



Cache Key를 Metric Label에 넣으면 Cardinality가 폭증할 수 있다.

\---

\## Dashboard

\`\`\`text id="cac29154"
Cache별 Hit Rate

Entry 수

Heap 추정량

Load p95

Load 실패율

Evict 건수

Version 불일치 Instance

DB Fallback TPS

Warm-up 상태

Stampede 대기

Stale 반환

## Alert 예

| 조건 | 등급 |
| --- | --- |
| Hit Rate 급감 | Warning |
| DB Fallback 급증 | Major |
| Load 실패 지속 | Major |
| Version 불일치 5분 | Major |
| 권한 Cache Evict 실패 | Critical |
| 거래통제 Cache 불일치 | Critical |
| Entry 상한 90% | Warning |
| Cache로 인한 Heap 80% | Major |
| Stampede 대기 증가 | Major |
| Region 전체 Evict | 운영 Event |

## 장애 점검 순서

text id="cac29155" 1. 영향 ServiceId 확인 2. 사용 Cache Name 확인 3. Hit·Miss 변화 확인 4. Instance별 Entry·Version 비교 5. 최근 OM 변경·Evict 이력 확인 6. Cache Load 원본 SQL 확인 7. DB Pool·Thread 영향 확인 8. Stampede·Hot Key 확인 9. Cache Provider 오류 확인 10. 특정 Key Evict·Reload 11. Region Evict 필요성 판단 12. 데이터 정합성 재검증

## 운영 복구 순서

text id="cac29156" 특정 Key Evict ↓ 특정 Scope Reload ↓ Version 재동기화 ↓ Instance별 확인 ↓ 필요 시 Region Evict ↓ 분산 Warm-up ↓ Hit Rate·DB 부하 관찰

처음부터 전체 Cache를 삭제하지 않는다.

# 책임 경계와 RACI

| 활동 | 업무개발 | FW | AA | DBA | 보안 | 운영 | OM | QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cache 대상 선정 | R | C | A | C | C | C | I | C |
| 원본 데이터 정의 | R/C | I | C | A/R | I | I | C | I |
| Key 설계 | R | R/C | A | C | C | I | C | C |
| TTL 설계 | R | C | A | C | C | R/C | C | C |
| Evict 구현 | R | R/C | A/C | I | C | C | R/C | C |
| 다중 WAS 전파 | C | R | A | I | C | R/C | R/C | C |
| 권한 Cache | C | R/C | A/C | I | A/R | C | R | C |
| Cache 용량 | C | R/C | A | C | I | R | I | C |
| OM 운영기능 | I | C | C | I | C | C | A/R | C |
| Evict 승인 | I | I | C | I | C | A/R | R | I |
| Metric·Alert | C | R | A/C | C | C | A/R | R/C | C |
| 장애 복구 | R/C | R/C | C | C | C | A/R | R/C | I |
| 보안·감사 | C | C | C | I | A/R | R/C | R | C |
| 테스트 | R | R | C | C | C | C | C | A/R |

# 자동검증 및 품질 Gate

## 1\. Cache 대상 Gate

검출·검토 대상:

\`\`\`text id=“cac29157” 고객 DTO

계좌·잔액

거래내역

Token

Secret

파일·Binary

Request·Session 객체

DB Connection

무제한 Collection



발견 시 기본 차단한다.

\---

\## 2. Cache Annotation Gate

\`\`\`text id="cac29158"
@Cacheable Cache Name 등록 여부

Key 명시 여부

TTL Region 존재 여부

Maximum Entries

동일 메서드 Self Invocation

Final·Private Method

조건부 Cache 정책

Spring AOP Proxy를 우회하는 Self Invocation도 검사한다.

## 3\. Key Gate

\`\`\`text id=“cac29159” Key Prefix 표준

GUID·Timestamp 금지

민감정보 금지

길이 제한

Version 포함

Scope 완전성

허용문자


\---

\## 4. TTL Gate

\`\`\`text id="cac29160"
TTL 존재

허용 Stale 근거

보안정보 짧은 TTL

Negative TTL 분리

Jitter·Warm-up 검토

## 5\. 변경·Evict Gate

변경 ServiceId마다 확인:

\`\`\`text id=“cac29161” 원본 DB 변경

Cache Evict

파생 Key

AFTER\_COMMIT

Version 증가

다중 WAS 전파

Evict 실패 복구


\---

\## 6. 용량 Gate

\`\`\`text id="cac29162"
최대 Entry

Entry 평균 크기

JVM Heap 비율

전체 WAR 합계

다중 WAS 복제량

Cold Start DB TPS

## 7\. 보안 Gate

\`\`\`text id=“cac29163” Token·Secret Value 금지

개인정보 Key 금지

OM Entry 조회 마스킹

Evict 권한

감사로그

분산 Cache TLS


\---

\## 8. 운영 Gate

\`\`\`text id="cac29164"
Hit·Miss Metric

Entry Count

Load Failure

Version 불일치

Manual Evict History

Warm-up

Runbook

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CACHE-001 | 첫 공통코드 조회 | Miss·DB 1회 |
| CACHE-002 | 두 번째 동일 조회 | Hit·DB 0회 |
| CACHE-003 | 다른 코드그룹 | 별도 Entry |
| CACHE-004 | TTL 만료 | DB 재조회 |
| CACHE-005 | Key Evict | 해당 Key만 Miss |
| CACHE-006 | Evict All | Region 전체 Miss |
| CACHE-007 | 코드그룹 변경 | 그룹 Key Evict |
| CACHE-008 | 그룹 신규 등록 | 전체 그룹 Key Evict |
| CACHE-009 | DB Rollback | Evict 미실행 |
| CACHE-010 | Commit 성공 | AFTER\_COMMIT Evict |
| CACHE-011 | Evict 실패 | Version 재동기화 |
| CACHE-012 | AP01 Evict | AP02·AP03 전파 |
| CACHE-013 | Instance Event 유실 | Version Polling 복구 |
| CACHE-014 | Cache Version 불일치 | 자동 Evict |
| CACHE-015 | Cache Version 동일 | Hit 유지 |
| CACHE-016 | 정상 Negative Cache | DB 1회 |
| CACHE-017 | Negative TTL 만료 | DB 재조회 |
| CACHE-018 | 신규 데이터 등록 | Negative Key Evict |
| CACHE-019 | 동일 Key 100 Thread | DB 1회 수준 |
| CACHE-020 | Single Flight Load 실패 | 대기 Thread 종료 |
| CACHE-021 | Single Flight Timeout | 빠른 실패·Stale |
| CACHE-022 | 다중 WAS Stampede | 분산보호 정책 |
| CACHE-023 | TTL Jitter | 만료 분산 |
| CACHE-024 | 전체 Warm-up | DB 부하 상한 |
| CACHE-025 | Rolling Warm-up | Instance 순차 |
| CACHE-026 | Cache Manager 초기화 실패 | 정책대로 기동 |
| CACHE-027 | Cache Get 예외 | DB Fallback |
| CACHE-028 | DB Fallback 폭증 | Rate Limit |
| CACHE-029 | 공통코드 Cache 장애 | 업무 지속 |
| CACHE-030 | 권한 Cache 장애 | Fail Closed |
| CACHE-031 | 거래통제 Cache 장애 | 안전 정책 |
| CACHE-032 | Cache Value 가변 List | 테스트 실패 |
| CACHE-033 | 불변 DTO | 변경 불가 |
| CACHE-034 | GUID Key | Gate 실패 |
| CACHE-035 | Timestamp Key | Gate 실패 |
| CACHE-036 | Token Key | Security Gate 실패 |
| CACHE-037 | 고객번호 평문 Key | Gate 실패 |
| CACHE-038 | 파일 Binary Value | Gate 실패 |
| CACHE-039 | 대량 목록 Value | 용량 Gate 실패 |
| CACHE-040 | DB Connection Value | Gate 실패 |
| CACHE-041 | Entry 최대치 도달 | Eviction 작동 |
| CACHE-042 | Entry 크기 초과 | 저장 차단 |
| CACHE-043 | Heap 사용 급증 | Alert |
| CACHE-044 | Hit Rate 95% | 정상 |
| CACHE-045 | Hit Rate 급락 | Alert |
| CACHE-046 | Hit Rate 높고 Stale | 정합성 실패 |
| CACHE-047 | Cache Load p95 증가 | 원본 SQL 분석 |
| CACHE-048 | Hot Key 만료 | Stampede 방지 |
| CACHE-049 | Avalanche | DB 보호 |
| CACHE-050 | Penetration 공격 | Validation·Negative |
| CACHE-051 | Cache Poisoning 입력 | Key·Value 검증 |
| CACHE-052 | Cache Name 미등록 | 기동·CI 실패 |
| CACHE-053 | Ehcache XML 오류 | 기동 실패 |
| CACHE-054 | TTL 설정 누락 | Gate 실패 |
| CACHE-055 | maxEntries 누락 | Gate 실패 |
| CACHE-056 | Self Invocation | Cache 미적용 탐지 |
| CACHE-057 | 동일 클래스 내부 호출 | Test 실패 |
| CACHE-058 | Cache Disabled Profile | DB 정상 처리 |
| CACHE-059 | 운영 Profile Cache | 설정 정상 |
| CACHE-060 | OM Cache 조회 | 권한 시 성공 |
| CACHE-061 | OM Entry 조회 | 민감값 마스킹 |
| CACHE-062 | OM Key Evict | 사유·감사 |
| CACHE-063 | OM Region Evict | 관리자 승인 |
| CACHE-064 | 미인가 Evict | 권한 오류 |
| CACHE-065 | 사유 없는 Evict | Validation 오류 |
| CACHE-066 | Reload | 최신 원본 적재 |
| CACHE-067 | Warm-up 실패 | 필수·선택 구분 |
| CACHE-068 | 배포 후 Cold Start | p95·DB 정상 |
| CACHE-069 | WAR 재기동 | Cache 재적재 |
| CACHE-070 | 다중 WAR 총 Heap | 예산 이내 |
| CACHE-071 | Cache Metric Label Key | Gate 실패 |
| CACHE-072 | Version Dashboard | Instance별 표시 |
| CACHE-073 | Evict History | 실행자·GUID |
| CACHE-074 | 권한 변경 | 즉시 차단 |
| CACHE-075 | 사용자 사용중지 | Cache·Token 반영 |
| CACHE-076 | Timeout 정책 변경 | 다음 거래 반영 |
| CACHE-077 | Gateway Route 변경 | Route Cache Evict |
| CACHE-078 | 오류코드 변경 | 신규 메시지 |
| CACHE-079 | Cache 장애 복구 | 정합성 검증 |
| CACHE-080 | 전체 거래 회귀 | 기능·성능 정상 |

# 따라 하는 실무 절차

## 1단계. Cache 후보 데이터를 선정한다

완료 증적:

\`\`\`text id=“cac29165” 데이터명

원본 Owner

원본 Table

조회량

변경량

평균 크기


\## 2단계. 허용 불일치시간을 결정한다

\`\`\`text id="cac29166"
0초

1분

10분

30분

업무 Owner와 보안담당자의 승인을 받는다.

## 3단계. Cache Name과 Key를 설계한다

\`\`\`text id=“cac29167” Cache Name

Key Prefix

Scope

Version

민감정보 여부


\## 4단계. TTL·최대 Entry를 결정한다

Heap·Hit Rate·원본 부하를 함께 산정한다.

\## 5단계. 조회 Adapter를 구현한다

\`\`\`text id="cac29168"
Service

Cache Reader

Repository

불변 DTO

## 6단계. 변경·Evict 흐름을 구현한다

\`\`\`text id=“cac29169” DB Commit

AFTER\_COMMIT

Evict

Version

Event


\## 7단계. 다중 WAS 전파를 구현한다

Instance별 적용결과를 확인한다.

\## 8단계. Stampede와 장애정책을 적용한다

\`\`\`text id="cac29170"
Single Flight

Jitter

Fallback 제한

Stale 정책

## 9단계. Metric과 OM 운영기능을 등록한다

\`\`\`text id=“cac29171” Hit

Miss

Size

Version

Evict

Load Failure


\## 10단계. 정상·동시성·장애 테스트를 실행한다

DB 조회 횟수와 Heap·Pool 영향을 함께 검증한다.

\---

\# 완료 체크리스트

\## Cache 대상

| 확인 항목 | 완료 |
|---|:---:|
| 원본 System과 Owner가 명확하다. | □ |
| 반복 조회되는 작은 데이터다. | □ |
| 변경 빈도가 낮다. | □ |
| 허용 Stale 시간이 정의됐다. | □ |
| 개인정보·Token·파일이 아니다. | □ |
| Entry 최대 크기가 있다. | □ |
| Cache 없이도 업무 흐름을 설명할 수 있다. | □ |

\## Key·TTL

| 확인 항목 | 완료 |
|---|:---:|
| Cache Name이 표준이다. | □ |
| Key Prefix가 표준이다. | □ |
| 업무·권한 Scope가 완전하다. | □ |
| GUID·Timestamp가 없다. | □ |
| 평문 개인정보가 없다. | □ |
| Contract·Schema Version을 검토했다. | □ |
| TTL 근거가 있다. | □ |
| Negative TTL이 분리됐다. | □ |
| Maximum Entries가 있다. | □ |
| TTL Jitter를 검토했다. | □ |

\## 변경·정합성

| 확인 항목 | 완료 |
|---|:---:|
| 변경 ServiceId가 식별됐다. | □ |
| DB Commit 후 Evict한다. | □ |
| 파생 Key를 함께 Evict한다. | □ |
| Cache Version을 증가시킨다. | □ |
| 전체 WAS에 전파된다. | □ |
| Event 유실 복구가 있다. | □ |
| Evict 실패 Alert가 있다. | □ |
| 운영 수동 Evict가 있다. | □ |
| Evict·Reload가 감사된다. | □ |

\## 장애·성능

| 확인 항목 | 완료 |
|---|:---:|
| Stampede 방지기능이 있다. | □ |
| Cache Penetration을 차단한다. | □ |
| Avalanche를 완화한다. | □ |
| DB Fallback 동시성을 제한한다. | □ |
| Fail Open·Closed가 정의됐다. | □ |
| Cache Entry Heap을 산정했다. | □ |
| 다중 WAR·WAS 합계를 계산했다. | □ |
| Warm-up DB 부하를 검증했다. | □ |
| Cache 장애 후 정상 거래를 검증했다. | □ |

\## 운영·보안

| 확인 항목 | 완료 |
|---|:---:|
| Hit·Miss Metric이 있다. | □ |
| Load·Failure Metric이 있다. | □ |
| Instance Version을 확인할 수 있다. | □ |
| Cache Value 조회가 통제된다. | □ |
| OM Evict 권한이 분리됐다. | □ |
| 삭제사유가 필수다. | □ |
| 감사로그가 남는다. | □ |
| Runbook이 있다. | □ |
| 전체 Evict 영향분석이 있다. | □ |

\---

\# 변경·호환성·폐기 관리

\## Cache Key 변경

\`\`\`text id="cac29172"
V1 Key

CC:CODE:V1:CHANNEL

V2 Key

CC:CODE:V2:CHANNEL

신규 Key를 병행한 뒤 구 Key를 폐기한다.

기존 Key를 같은 이름으로 의미만 바꾸면 구 Entry가 신 로직에 사용될 수 있다.

## Value Schema 변경

Cache Value 클래스가 변경되면 다음 문제가 발생할 수 있다.

\`\`\`text id=“cac29173” 구 WAR → V1 Value 기대

신 WAR → V2 Value 저장



Local Cache는 WAR별 ClassLoader로 분리되지만 분산 Cache에서는 구·신 Version 충돌이 가능하다.

대안:

\`\`\`text id="cac29174"
Key Version

Value Schema Version

구·신 Decoder

Rolling 호환

전체 Evict

## TTL 변경

TTL 확대:

\`\`\`text id=“cac29175” Hit Rate 증가

Stale 기간 증가



TTL 축소:

\`\`\`text id="cac29176"
정합성 개선

DB 부하 증가

변경 전후 Hit Rate·DB TPS·Stale Risk를 비교한다.

## Maximum Entries 변경

증가:

\`\`\`text id=“cac29177” Heap 증가

GC 영향



감소:

\`\`\`text id="cac29178"
Eviction 증가

Hit Rate 하락

성능시험으로 검증한다.

## Local에서 Distributed Cache로 전환

text id="cac29179" 1. Cache Contract 분리 2. Serialization Schema 정의 3. Key Namespace 확정 4. TTL·Evict Event 적용 5. Network Timeout·Pool 설정 6. 장애 Fallback 검증 7. Dual Read·Shadow 검증 8. 단계 전환 9. Local Cache 정책 재정의

## Cache 폐기

\`\`\`text id=“cac29180” 신규 Put 중지

호출량 확인

Cache Reader 제거

DB 성능 검증

Region Evict

설정 제거

OM Catalog 제거

Metric·Dashboard 제거

이력 보존



Cache를 제거한 뒤 원본 DB가 Peak TPS를 처리할 수 있는지 검증한다.

\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, Cache의 원본은 항상 DB·승인된 기준정보 저장소이며 Cache는 조회 가속을 위한 임시 복제본이다.

둘째, Cache 적용 여부는 조회 TPS만이 아니라 데이터가 얼마 동안 원본과 달라도 되는지를 기준으로 결정해야 한다.

셋째, TTL은 자동 만료를 위한 안전망이지 데이터 변경을 반영하는 주된 방법이 아니다.

\`\`\`text id="cac29181"
정합성
\= Commit 이후 Evict
\+ Version
\+ TTL

넷째, 현재 Ehcache 기반 Local Cache는 공통코드·Service Catalog·오류코드 같은 기준정보에는 적합하지만 다중 WAS의 즉시 일관성은 별도 전파구조가 필요하다.

다섯째, Cache Stampede와 전체 Cache 장애는 원본 DB와 DB Pool을 고갈시켜 Cache 적용 전보다 더 큰 장애를 만들 수 있다.

여섯째, Hit Rate가 높아도 오래된 권한이나 잘못된 정책을 반환한다면 실패한 Cache다.

일곱째, 고객정보·계좌·거래내역·Token·대형 파일은 Cache 적용으로 얻는 이익보다 보안·정합성·용량위험이 크다.

여덟째, OM Cache 운영기능은 단순 삭제 화면이 아니라 Instance별 Version·Evict 결과·Warm-up·감사를 관리하는 통제점으로 발전해야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Cache를 원본으로 사용 | 데이터 손실·불일치 |
| TTL 일괄 적용 | 정합성·DB 부하 왜곡 |
| Evict 누락 | 오래된 값 노출 |
| Commit 전 Cache Put | Rollback 후 잘못된 값 |
| Local Evict만 수행 | 서버별 다른 결과 |
| 전체 Evict 남용 | DB 부하 폭증 |
| GUID·시간 Key | Hit Rate 0·Entry 폭증 |
| 개인정보 Key | 보안·감사 위험 |
| 가변 객체 Cache | 요청 간 데이터 오염 |
| Negative Cache 없음 | DB 공격·반복 Miss |
| Stampede 방지 없음 | DB Pool 고갈 |
| Max Entry 없음 | Heap·GC 장애 |
| Cache 장애 무제한 Fallback | 전체 DB 장애 |
| 권한 Cache Fail Open | 인가 우회 |
| Event 유실 대응 없음 | 장기 불일치 |
| Metric 없음 | 장애 조기탐지 불가 |
| OM 전체삭제 권한 과다 | 운영 장애·감사 위반 |

## 우선 보완 과제

1.  Cache 대상·원본·Owner·TTL·Evict를 관리하는 Cache Catalog를 작성한다.
2.  commonCode, serviceCatalog, sessionRegion의 현재 운영 사용처와 실제 Entry 규모를 측정한다.
3.  Cache Key Builder와 Cache Name 상수를 공통화한다.
4.  Cache Value를 Map 중심에서 불변 Typed DTO로 개선한다.
5.  기준정보 변경 시 AFTER\_COMMIT Evict를 적용한다.
6.  OM\_CACHE\_VERSION과 Instance별 Version 점검을 구현한다.
7.  Evict Event Outbox와 재전송 구조를 구현한다.
8.  Hot Key에 Single Flight·TTL Jitter를 적용한다.
9.  존재하지 않는 기준정보에 짧은 Negative Cache를 적용한다.
10.  Cache 장애 시 DB Fallback 동시성·Rate Limit을 구현한다.
11.  권한·거래통제·사용자 상태 Cache의 Fail Closed 정책을 확정한다.
12.  Hit·Miss·Load·Entry·Version·Fallback Metric을 Micrometer로 제공한다.
13.  OM Cache 화면에 Instance·Version·Entry·Evict 결과를 표시한다.
14.  Region 전체 Evict에 승인·영향분석·분산 Warm-up을 적용한다.
15.  Cache 대상 금지 DTO·Token·Binary를 CI 정적검사로 차단한다.

## 중장기 발전 방향

\`\`\`text id=“cac29182” 개별 @Cacheable ↓ Cache Adapter·Catalog

TTL 중심 ↓ Commit Evict·Version·TTL

단일 JVM Evict ↓ 전 Instance Event·Version

고정 TTL ↓ Jitter·Refresh-Ahead

Cache Miss 즉시 DB ↓ Single Flight·DB 보호

단순 Snapshot ↓ Hit·Size·Version Dashboard

수동 전체삭제 ↓ 승인형 Key Evict·Reload

Local Cache ↓ 필요 영역 L1+L2 Cache

경험 기반 설정 ↓ Heap·Hit Rate·Stale Risk 기반 운영


\---

\# 마무리말

Cache를 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id="cac29183"
Cache하려는 데이터의 원본은 어디인가?

누가 데이터를 변경하는가?

몇 초·몇 분 동안 오래된 값을 허용할 수 있는가?

Cache Key가 데이터 범위를 완전히 표현하는가?

사용자·지점·권한 조건이 빠지지 않았는가?

Key와 Value에 개인정보·Token이 포함되지 않는가?

TTL과 최대 Entry의 근거는 무엇인가?

원본 DB 변경 후 언제 Cache를 삭제하는가?

Transaction이 Rollback되면 Cache 상태는 어떻게 되는가?

AP01에서 삭제한 값이 AP02에서도 삭제되는가?

Evict Event가 유실되면 어떻게 복구하는가?

인기 Key가 만료되면 DB 조회가 몇 건 발생하는가?

존재하지 않는 Key 반복요청을 어떻게 막는가?

Cache 장애 시 DB가 Peak 부하를 견딜 수 있는가?

권한·거래통제 Cache 장애 시 허용할 것인가, 차단할 것인가?

Cache Entry가 JVM Heap을 얼마나 사용하는가?

다중 WAR·WAS 전체 Cache 메모리는 얼마인가?

운영자가 어떤 Key를 왜 삭제했는지 감사할 수 있는가?

Hit Rate·Version·DB Fallback을 Dashboard에서 확인할 수 있는가?

Cache를 제거해도 원본 시스템이 안전하게 동작하는가?

제29장의 핵심 흐름은 다음과 같다.

text id="cac29184" Cache 후보 선정 ↓ 원본·Owner 확인 ↓ Key·TTL·용량 설계 ↓ Hit·Miss 구현 ↓ Commit 후 Evict ↓ 다중 WAS Version 동기화 ↓ Stampede·장애 방어 ↓ Metric·OM 운영 ↓ 정합성 검증

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“cac29185” Cache는 데이터를 빠르게 보여 주는 기술이지만, 잘못된 데이터를 빠르게 보여 줄 수도 있다.

따라서 Cache 성능보다 먼저 원본과 허용 불일치시간, 무효화 책임과 장애 복구를 결정해야 한다.

Cache가 없어도 정확하게 동작하고, Cache가 있을 때 더 빠르게 동작해야

안전한 Cache 설계라고 할 수 있다. \`\`\`
