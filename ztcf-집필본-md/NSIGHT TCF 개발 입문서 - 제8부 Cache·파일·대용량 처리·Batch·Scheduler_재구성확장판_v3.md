<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제8부 Cache·파일·대용량 처리·Batch·Scheduler_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제8부. Cache·파일·대용량 처리·Batch·Scheduler의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

인증·인가, 시스템 간 계약, 캐시·배치 등 분산 환경의 경계를 이해합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

3단계 · 보안과 연계

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제8부. Cache·파일·대용량 처리·Batch·Scheduler**

# **1\. 도입 전 안내말**

제7부에서는 다른 업무 WAR와 외부 시스템을 안전하게 호출하는 방법을 배웠습니다.

이번 제8부에서는 다음과 같은 기능을 다룹니다.

자주 조회하는 기준정보를 빠르게 제공한다.

보고서와 엑셀 파일을 생성한다.

대용량 파일을 업로드하거나 다운로드한다.

수십만 건의 데이터를 일괄 처리한다.

정해진 시각에 배치를 자동 실행한다.

실패한 작업을 다시 처리한다.

이 기능들은 일반적인 온라인 조회·등록 거래와 처리 특성이 다릅니다.

예를 들어 사용자가 화면에서 보고서 생성 버튼을 눌렀다고 가정합니다.

보고서 대상 데이터
300만 건

예상 생성시간
20분

파일 크기
5GB

이 작업을 일반 온라인 거래처럼 처리하면 다음 문제가 발생합니다.

화면 연결이 20분 동안 유지된다.

Tomcat Thread가 장시간 점유된다.

DB Connection이 오래 사용된다.

Gateway와 화면 Timeout이 발생한다.

사용자는 실패했다고 생각해 다시 요청한다.

같은 보고서가 여러 번 생성된다.

WAS 메모리가 부족해질 수 있다.

따라서 다음 구조로 분리해야 합니다.

온라인 요청
→ 작업 접수
→ Job ID 반환

백그라운드 작업
→ 데이터 분할 처리
→ 파일 생성
→ 저장소 보관
→ 완료 상태 기록

사용자
→ 작업상태 조회
→ 완료 후 다운로드

Cache도 마찬가지입니다.

Cache를 적용하면 조회속도를 높일 수 있지만 잘못 사용하면 오래된 데이터, 서버 간 불일치, 메모리 부족이 발생할 수 있습니다.

Cache 적용
\= 항상 성능 개선

이 아닙니다.

정확한 판단은 다음과 같습니다.

Cache 적용
\= DB 조회를 줄이는 대신
데이터 최신성·메모리·무효화 책임을 추가

Batch와 Scheduler도 구분해야 합니다.

Batch
\= 대량 데이터를 처리하는 작업

Scheduler
\= Batch를 언제 실행할지 결정하는 기능

Scheduler가 업무 로직을 직접 처리하는 구조를 만들면 재처리와 수동 실행이 어려워집니다.

이번 부에서는 다음 원칙을 중심으로 설명합니다.

온라인 거래는 짧게 끝낸다.

장시간 작업은 Job으로 분리한다.

대용량 데이터는 한 번에 메모리에 적재하지 않는다.

파일은 WAS 로컬 디스크에 영구 저장하지 않는다.

Batch 업무 로직과 실행 스케줄을 분리한다.

실패한 작업은 재시도와 재처리가 가능해야 한다.

모든 Job은 상태와 처리건수를 추적할 수 있어야 한다.

# **2\. 제8부 개요**

## **2.1 목적**

제8부의 목적은 초보 개발자가 Cache, 파일, 대용량 데이터, Batch, Scheduler를 온라인 거래와 구분하여 안전하고 운영 가능한 구조로 설계하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

1.  Cache 적용 대상과 비적용 대상을 구분한다.
2.  Cache Key, TTL, 무효화 정책을 설계한다.
3.  로컬 Cache와 분산 Cache의 차이를 설명한다.
4.  파일 업로드와 다운로드의 보안 기준을 적용한다.
5.  대용량 파일을 Streaming 방식으로 처리한다.
6.  장시간 작업을 비동기 Job으로 전환한다.
7.  Job 상태와 진행률을 설계한다.
8.  Batch의 Reader·Processor·Writer 구조를 이해한다.
9.  Chunk 크기와 Commit 단위를 결정한다.
10.  Scheduler와 Batch 업무 로직을 분리한다.
11.  Batch 중복실행과 서버 다중화 문제를 통제한다.
12.  실패 Job의 재시도·재시작·재처리를 구분한다.
13.  대량 작업의 성능·용량·보안·감사를 검증한다.
14.  OM에서 Batch와 파일 작업을 운영할 수 있도록 설계한다.

## **2.2 적용범위**

| **영역** | **주요 내용** |
| --- | --- |
| Cache | Local Cache, Shared Cache, TTL |
| Cache 정합성 | 무효화, 갱신, 버전 |
| 파일 업로드 | 검증, 저장, 악성파일 |
| 파일 다운로드 | Streaming, 권한, 감사 |
| 대용량 파일 | 분할, Streaming, 비동기 |
| 비동기 Job | 접수, 상태, 진행률 |
| Batch | Reader, Processor, Writer |
| Chunk | 처리 단위, Commit 단위 |
| Scheduler | 실행시각, 중복방지 |
| 재처리 | Retry, Restart, Rerun |
| 운영 | Job Control, 통계, 경보 |
| 품질 | 용량시험, 장애시험, 보안시험 |

## **2.3 대상 독자**

-   Cache를 적용하면 무조건 빨라진다고 생각하는 개발자
-   파일 전체를 byte\[\]로 읽어 처리하는 개발자
-   대용량 조회 결과를 한 번에 List로 만드는 개발자
-   Batch와 Scheduler를 같은 기능으로 이해하는 개발자
-   @Scheduled 메서드 안에 모든 업무 로직을 작성하는 개발자
-   실패 Batch를 처음부터 다시 실행하는 개발자
-   다중 WAS에서 Scheduler가 여러 번 실행되는 문제를 겪는 개발자
-   대용량 작업의 운영·재처리 기능을 설계해야 하는 개발자

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

TCF 온라인 거래는 ServiceId 기반으로 실행된다.

ServiceId별 Timeout과 거래통제가 적용된다.

외부 연계는 Timeout·Retry·Circuit 정책을 가진다.

변경 거래는 Idempotency가 필요할 수 있다.

GUID와 TraceId로 거래 흐름을 추적한다.

## **2.5 주요 용어**

| **용어** | **쉬운 설명** |
| --- | --- |
| Cache | 자주 사용하는 데이터를 가까운 저장소에 임시 보관 |
| Cache Hit | Cache에서 데이터를 찾은 경우 |
| Cache Miss | Cache에 없어 원본을 조회하는 경우 |
| TTL | Cache 데이터가 유효한 시간 |
| Eviction | Cache 데이터를 제거하는 것 |
| Invalidation | 원본 변경에 맞춰 Cache를 무효화하는 것 |
| Local Cache | 한 애플리케이션 인스턴스 내부 Cache |
| Shared Cache | 여러 인스턴스가 함께 사용하는 Cache |
| Streaming | 전체 데이터를 메모리에 올리지 않고 순차 처리 |
| Chunk | Batch가 한 번에 읽고 Commit하는 데이터 묶음 |
| Job | 하나의 대량 업무 작업 |
| Step | Job을 구성하는 세부 처리단계 |
| Job Instance | Job 이름과 기준 파라미터로 구분되는 실행 대상 |
| Job Execution | 실제 한 번의 Job 실행 |
| Scheduler | 정해진 시각이나 조건에 따라 Job을 시작하는 기능 |
| Retry | 같은 실행 안에서 일시 오류를 다시 시도 |
| Restart | 실패 지점부터 이어서 실행 |
| Rerun | 새로운 Job 실행으로 처음 또는 지정 범위부터 재실행 |
| Checkpoint | 재시작을 위한 처리 위치 |
| DLQ | 반복 실패 데이터를 별도로 보관하는 영역 |

# **제55장. Cache를 왜 사용하나요?**

학습 목표 | 55장. Cache를 왜 사용하나요?의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **55.1 Cache의 기본 개념**

Cache는 자주 사용하는 데이터를 DB보다 가까운 곳에 임시 저장하는 방식입니다.

첫 번째 요청
→ Cache Miss
→ DB 조회
→ Cache 저장
→ 응답

두 번째 요청
→ Cache Hit
→ DB 조회 없이 응답

대표 적용 대상:

공통코드
업무코드
오류메시지
Service Catalog
Timeout 정책
거래통제 정책
메뉴·권한 기준정보
자주 조회하고 변경이 적은 기준정보

## **55.2 Cache를 적용하는 이유**

| **목적** | **효과** |
| --- | --- |
| DB 부하 감소 | 반복 SQL 감소 |
| 응답시간 개선 | 메모리에서 빠른 조회 |
| 외부 연계 감소 | 반복 호출 방지 |
| 장애 완화 | 원본 장애 시 제한적 Fallback |
| 처리량 향상 | 동일 자원으로 더 많은 요청 처리 |

## **55.3 Cache 적용이 적합한 데이터**

다음 조건이 많을수록 Cache에 적합합니다.

조회 빈도가 높다.

변경 빈도가 낮다.

데이터 크기가 작다.

약간 오래된 값이 허용된다.

동일한 Key로 반복 조회된다.

원본 조회 비용이 높다.

예:

공통코드
오류메시지
ServiceId 운영정책
지점 기본정보
상품분류 코드

## **55.4 Cache 적용이 위험한 데이터**

다음 데이터는 신중하게 검토합니다.

계좌 잔액
실시간 한도
승인 상태
재고 수량
실시간 거래 결과
사용자별 민감정보
대용량 조회 결과
변경 직후 즉시 반영이 필요한 데이터

예를 들어 캠페인 승인 상태를 오래 Cache하면 다음 문제가 발생할 수 있습니다.

DB 상태
APPROVED

Cache 상태
DRAFT

사용자
다시 수정 시도

## **55.5 Cache Hit와 Miss**

Cache Hit
\= 원하는 값이 Cache에 있음

Cache Miss
\= Cache에 값이 없음

Cache Hit Ratio:

Cache Hit 수
÷ 전체 Cache 조회 수

Hit Ratio가 낮은 Cache는 메모리와 복잡도만 증가시킬 수 있습니다.

## **55.6 Cache에 없는 값**

데이터가 없다는 결과도 Cache할 수 있습니다.

이를 Negative Cache라고 합니다.

예:

존재하지 않는 상품코드 조회
→ 매 요청마다 DB 조회

짧은 TTL로 “없음”을 Cache하면 반복 부하를 줄일 수 있습니다.

주의:

새 데이터가 등록되었는데
없음 Cache가 남아 있으면
등록된 데이터가 보이지 않을 수 있다.

따라서 Negative Cache TTL은 짧게 적용합니다.

## **55.7 Cache는 원본 데이터가 아니다**

기본 원칙:

DB·원천 시스템
\= Source of Truth

Cache
\= 임시 복사본

Cache가 유실되어도 원본에서 다시 만들 수 있어야 합니다.

중요 업무 데이터를 Cache에만 저장하면 안 됩니다.

## **55.8 Cache 장애 시 처리**

Cache가 장애라고 모든 업무가 실패해야 하는 것은 아닙니다.

일반적인 조회 Cache:

Cache 장애
→ DB 직접 조회
→ 성능 저하 상태로 계속 처리

거래통제 정책 Cache:

Cache 장애
→ 마지막 정상 정책 사용
또는
→ 중요 거래 Fail-closed

Cache의 업무 중요도에 따라 정책이 달라집니다.

## **제55장 요약**

학습 목표 | 55장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Cache는 원본 데이터를 대신하는 저장소가 아니다.

조회가 많고 변경이 적으며
약간의 지연 반영이 허용되는 데이터에 적합하다.

Cache 장애 시 원본 조회로 전환할 수 있어야 한다.

# **제56장. Cache Key·TTL·무효화 설계**

학습 목표 | 56장. Cache Key·TTL·무효화 설계의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **56.1 Cache Key**

Cache Key는 데이터를 정확하게 구분해야 합니다.

예:

공통코드 그룹
COMMON\_CODE:CHANNEL\_TYPE

고객등급
CUSTOMER\_GRADE:CUST000001

ServiceId 정책
SERVICE\_POLICY:SV.Customer.selectSummary

## **56.2 Key 설계 원칙**

업무영역을 포함한다.

데이터 종류를 포함한다.

고유 식별값을 포함한다.

환경별 충돌을 방지한다.

개인정보 원문 사용을 최소화한다.

길이가 지나치게 길지 않도록 한다.

예:

{환경}:{업무}:{데이터유형}:{식별자}

PRD:SV:SERVICE\_POLICY:SV.Customer.selectSummary

## **56.3 사용자별 Cache**

사용자별 결과를 Cache할 때는 Key에 사용자와 권한 범위가 포함되어야 합니다.

금지:

CUSTOMER\_LIST:PAGE1

서로 다른 사용자가 같은 결과를 받을 수 있습니다.

권장 개념:

CUSTOMER\_LIST:{USER\_SCOPE\_HASH}:{SEARCH\_HASH}:PAGE1

다만 사용자별 Cache가 많아지면 메모리가 급격히 증가할 수 있습니다.

## **56.4 TTL**

TTL은 Cache가 자동 만료되는 시간입니다.

예:

| **데이터** | **TTL 예** |
| --- | --- |
| 오류메시지 | 1시간 |
| 공통코드 | 30분 |
| 지점정보 | 10분 |
| 거래통제 정책 | 30초 또는 Event 갱신 |
| 사용자 권한 | 1~5분 |
| 실시간 업무정보 | Cache 비권장 또는 매우 짧게 |

실제 값은 변경빈도와 최신성 요구를 기준으로 결정합니다.

## **56.5 TTL이 너무 길면**

원본 변경
→ Cache에는 이전 값
→ 사용자에게 오래된 정보 제공

특히 권한과 거래통제의 TTL이 길면 보안·운영 문제가 발생할 수 있습니다.

## **56.6 TTL이 너무 짧으면**

Cache가 자주 만료
→ DB 조회 반복
→ Hit Ratio 저하
→ Cache 효과 감소

모든 데이터를 동일 TTL로 설정하지 않습니다.

## **56.7 Cache 무효화 방식**

### **시간 기반**

TTL 만료
→ 다음 요청에서 다시 조회

### **변경 시 삭제**

DB 변경 성공
→ 해당 Cache Key 삭제

### **변경 시 갱신**

DB 변경 성공
→ Cache에 새 값 저장

### **Event 기반**

기준정보 변경
→ CacheInvalidated Event
→ 여러 WAR Cache 삭제

## **56.8 Cache-Aside Pattern**

가장 일반적인 조회 방식입니다.

public CommonCode findCode(String codeGroup) {

CommonCode cached =
cache.get(codeGroup);

if (cached != null) {
return cached;
}

CommonCode data =
commonCodeDao.selectCode(codeGroup);

cache.put(codeGroup, data);

return data;
}

프레임워크 Cache Annotation을 사용할 수도 있습니다.

@Cacheable(
cacheNames = "commonCode",
key = "#codeGroup"
)
public CommonCode findCode(String codeGroup) {
return commonCodeDao.selectCode(codeGroup);
}

## **56.9 변경과 Cache 무효화**

@Transactional
public void updateCommonCode(
CommonCodeUpdateRequest request) {

commonCodeDao.update(request);

cacheEvictPublisher.publish(
"COMMON\_CODE:" + request.codeGroup()
);
}

주의:

DB Commit 전에 Cache 삭제
→ DB Rollback
→ Cache는 비어 있음

성능 문제는 있지만 정합성 오류는 비교적 적습니다.

반대로 DB Commit 전에 Cache에 새 값을 넣으면 Rollback된 값이 Cache에 남을 수 있습니다.

가능하면 Commit 이후 Cache를 갱신하거나 삭제합니다.

## **56.10 Cache Stampede**

인기 Key가 만료되면 동시에 많은 요청이 DB를 조회할 수 있습니다.

Cache 만료
→ 요청 1,000건 동시 유입
→ 모두 DB 조회

방지:

-   Key별 Lock
-   Single Flight
-   만료 전 갱신
-   TTL에 작은 Random 값
-   Background Refresh
-   요청 제한

## **56.11 Hot Key**

특정 하나의 Key에 요청이 집중되는 문제입니다.

예:

전체 시스템 공통 정책
SERVICE\_POLICY:GLOBAL

분산 Cache에서는 특정 Node에 부하가 집중될 수 있습니다.

Local Cache와 Shared Cache의 2단 구조를 검토할 수 있습니다.

## **56.12 Cache Poisoning**

잘못된 데이터가 Cache에 저장되면 많은 사용자에게 영향을 줄 수 있습니다.

방지:

-   원본 응답 검증
-   Cache 저장 전 Validation
-   데이터 버전
-   관리자 강제 무효화
-   Cache 변경 감사
-   잘못된 값의 TTL 제한

## **제56장 요약**

학습 목표 | 56장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Cache Key는 업무와 데이터 범위를 정확히 표현해야 한다.

TTL은 데이터별 최신성 요구에 따라 결정한다.

DB 변경 후에는
해당 Cache를 삭제하거나 갱신해야 한다.

Cache 만료 순간의 동시 DB 조회도 통제해야 한다.

# **제57장. Local Cache와 Shared Cache**

학습 목표 | 57장. Local Cache와 Shared Cache의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **57.1 Local Cache**

각 WAS 인스턴스의 메모리에 Cache를 저장합니다.

WAS 1 Cache
WAS 2 Cache
WAS 3 Cache

대표 기술:

-   Caffeine
-   Ehcache
-   Java Concurrent Map

## **57.2 Local Cache 장점**

네트워크 호출이 없다.

응답속도가 빠르다.

구성이 단순하다.

외부 Cache 장애 영향을 받지 않는다.

## **57.3 Local Cache 단점**

WAS마다 값이 다를 수 있습니다.

WAS 1
정책 ACTIVE

WAS 2
정책 SUSPENDED

사용자 요청이 어느 WAS로 가는지에 따라 결과가 달라질 수 있습니다.

## **57.4 Shared Cache**

여러 인스턴스가 하나의 Cache 저장소를 공유합니다.

WAS 1 ─┐
WAS 2 ─┼→ Shared Cache
WAS 3 ─┘

대표적으로 Redis 같은 외부 Cache가 사용될 수 있습니다.

NSIGHT 환경에서 Redis를 사용하지 않는다면 다음 방식을 검토할 수 있습니다.

-   DB 기반 기준정보
-   Local Cache + 변경 Event
-   Local Cache + 짧은 TTL
-   OM Push·Refresh API
-   파일·설정 배포 기반 기준정보

## **57.5 Local Cache 정합성**

Local Cache를 여러 WAR와 WAS에서 사용할 경우 무효화 전파가 필요합니다.

OM에서 정책 변경
→ 정책변경 Event
→ 모든 업무 WAR 수신
→ 해당 Key 삭제

Event 수신 실패에 대비해 TTL도 함께 적용합니다.

Event 기반 즉시 반영
\+ TTL 기반 최종 보정

## **57.6 다단 Cache**

L1 Local Cache
→ Miss
→ L2 Shared Cache
→ Miss
→ DB

장점:

-   매우 빠른 Local 조회
-   인스턴스 간 공유
-   DB 부하 감소

단점:

-   무효화 복잡
-   여러 Cache 상태 관리
-   장애 원인 분석 어려움

초기 프로젝트에서는 필요성을 검증한 뒤 적용합니다.

## **57.7 Cache 용량**

Cache에 객체 100만 건을 저장하면 메모리가 크게 증가할 수 있습니다.

고려항목:

객체 수
평균 객체 크기
Key 크기
Java 객체 Overhead
복제본 수
만료 전 최대 증가량

Local Cache는 반드시 최대 크기를 제한합니다.

Caffeine.newBuilder()
.maximumSize(100\_000)
.expireAfterWrite(
Duration.ofMinutes(10)
)
.build();

## **57.8 Cache Eviction**

Cache가 최대 크기에 도달하면 일부 항목을 제거합니다.

대표 기준:

-   최근 사용빈도
-   최근 사용시각
-   TTL
-   Weight
-   우선순위

Eviction이 너무 자주 발생하면 Cache 크기가 부족하거나 Key 설계가 잘못되었을 수 있습니다.

## **57.9 Cache 모니터링**

| **지표** | **의미** |
| --- | --- |
| Hit Count | Cache 적중 |
| Miss Count | 원본 조회 |
| Hit Ratio | Cache 효과 |
| Load Time | 원본 적재시간 |
| Eviction Count | 용량 부족 가능성 |
| Entry Count | 현재 항목 수 |
| Memory Size | 사용 메모리 |
| Load Failure | 원본 조회 실패 |
| Invalidation Delay | 변경 반영 지연 |

## **제57장 요약**

학습 목표 | 57장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Local Cache는 빠르지만
여러 WAS 간 데이터가 달라질 수 있다.

Shared Cache는 정합성이 좋지만
외부 의존성과 운영 복잡도가 증가한다.

NSIGHT에서는 데이터 중요도에 따라
Local Cache와 변경 Event·TTL을 조합할 수 있다.

# **제58장. 파일 업로드 설계**

학습 목표 | 58장. 파일 업로드 설계의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **58.1 파일 업로드는 일반 JSON 거래와 다르다**

일반 온라인 전문:

작은 JSON Body

파일 업로드:

수 MB
수 GB
수십 GB

파일을 일반 JSON의 Base64 문자열로 넣으면 크기가 증가하고 메모리 사용량이 커집니다.

금지 예:

{
"fileName": "customer.csv",
"fileData": "매우 큰 Base64 문자열"
}

대용량 파일은 Multipart 또는 전용 업로드 방식을 사용합니다.

## **58.2 파일 업로드 흐름**

사용자
→ 업로드 요청

업무 WAR
→ 권한 확인
→ 파일 메타정보 검증
→ 임시 저장
→ 바이러스 검사
→ 파일 등록정보 저장
→ Job 접수
→ Job ID 반환

Batch
→ 파일 Streaming 처리
→ 결과 저장

## **58.3 파일 메타정보**

| **항목** | **설명** |
| --- | --- |
| File ID | 내부 파일 식별자 |
| Original Name | 사용자가 올린 이름 |
| Stored Name | 서버 저장 이름 |
| File Size | 바이트 크기 |
| Extension | 확장자 |
| MIME Type | 파일 유형 |
| Hash | 파일 무결성 |
| Upload User | 업로드 사용자 |
| Upload Dtm | 업로드 시각 |
| Status | 업로드·검사·처리 상태 |
| Storage Path | 논리 저장위치 |
| Retention Dtm | 보관 만료시각 |
| TraceId | 업로드 거래 추적 |

## **58.4 파일명 보안**

사용자 파일명을 그대로 저장경로로 사용하면 안 됩니다.

금지:

../../server.xml
C:\\windows\\system32\\...

권장:

원본 파일명
→ 화면 표시용 Metadata

실제 저장명
→ UUID 또는 File ID

예:

원본
고객목록.xlsx

저장명
4f2a8d7c-...-001.bin

## **58.5 확장자와 MIME 검증**

파일명 확장자만 믿으면 안 됩니다.

malware.exe
→ malware.xlsx로 이름 변경

검증항목:

-   허용 확장자
-   MIME Type
-   File Signature·Magic Number
-   최대 크기
-   압축파일 내부
-   암호화 파일 허용 여부
-   실행파일 포함 여부

## **58.6 허용 목록**

예:

허용
CSV
XLSX
TXT
ZIP

금지
EXE
BAT
CMD
JSP
CLASS
WAR
SH

업무별 허용 파일을 별도로 관리합니다.

## **58.7 파일 크기 제한**

파일 크기 제한은 여러 계층에 정합성 있게 적용합니다.

Browser
Apache
Gateway
Tomcat
Spring Multipart
업무 ServiceId
Storage

예:

업무 최대 파일 크기
5GB

Apache 제한
5.2GB 이상

Gateway 제한
5.1GB 이상

Spring 제한
5GB

상위 계층이 더 작은 값이면 업무 설정까지 도달하지 못합니다.

## **58.8 메모리 적재 금지**

금지:

byte\[\] bytes =
multipartFile.getBytes();

대용량 파일 전체가 Heap에 적재될 수 있습니다.

권장:

try (InputStream input =
multipartFile.getInputStream()) {

storageService.storeStreaming(
fileId,
input
);
}

## **58.9 저장 위치**

WAS 로컬 디스크에 영구 저장하면 다음 문제가 발생합니다.

요청이 WAS 1에 업로드
→ 다운로드 요청이 WAS 2로 전달
→ 파일 없음

또한 서버 교체·재배포 시 파일이 사라질 수 있습니다.

권장 저장소:

-   공용 파일시스템
-   NAS
-   Object Storage
-   문서관리 저장소
-   전용 파일 서버

로컬 디스크는 임시 처리공간으로만 사용합니다.

## **58.10 악성파일 검사**

외부에서 업로드된 파일은 악성코드 검사를 검토해야 합니다.

UPLOADED
→ SCANNING
→ SAFE
또는
→ INFECTED

SAFE 상태가 되기 전에는 업무 Batch가 처리하지 않도록 합니다.

## **58.11 압축폭탄**

작은 ZIP 파일이 압축 해제 후 수백 GB가 될 수 있습니다.

검증항목:

-   압축 해제 최대 크기
-   파일 개수
-   중첩 압축 깊이
-   압축률
-   허용 확장자
-   경로 이동 공격

## **58.12 업로드 상태**

RECEIVING
→ UPLOADED
→ SCANNING
→ SAFE
→ PROCESSING
→ COMPLETED

SCANNING
→ INFECTED

PROCESSING
→ FAILED

## **제58장 요약**

학습 목표 | 58장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

대용량 파일은 Streaming으로 저장한다.

원본 파일명을 저장경로로 사용하지 않는다.

확장자뿐 아니라 MIME·파일 Signature를 검증한다.

WAS 로컬 디스크는 영구 저장소로 사용하지 않는다.

# **제59장. 파일 다운로드와 보고서 생성**

학습 목표 | 59장. 파일 다운로드와 보고서 생성의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **59.1 작은 파일 다운로드**

작은 파일은 동기 Streaming으로 내려줄 수 있습니다.

@GetMapping("/files/{fileId}")
public void download(
@PathVariable String fileId,
HttpServletResponse response,
TransactionContext context) {

FileMetadata metadata =
fileService.authorizeAndFind(
fileId,
context
);

response.setContentType(
metadata.contentType()
);

try (InputStream input =
storageService.open(fileId);

OutputStream output =
response.getOutputStream()) {

input.transferTo(output);
}
}

실제 다운로드 Endpoint는 프로젝트의 파일 표준과 보안 Filter를 적용해야 합니다.

## **59.2 전체 파일 메모리 적재 금지**

금지:

byte\[\] file =
Files.readAllBytes(path);

return ResponseEntity.ok(file);

대용량 파일은 Heap 부족을 일으킬 수 있습니다.

## **59.3 다운로드 권한**

File ID를 알고 있다고 누구나 다운로드할 수 있어서는 안 됩니다.

검증:

파일 소유자
업무권한
지점·조직 범위
파일 상태
보관기간
1회성 Token
다운로드 횟수 제한

## **59.4 다운로드 감사**

고객정보나 대량 업무파일은 감사대상이 될 수 있습니다.

기록항목:

| **항목** | **설명** |
| --- | --- |
| 사용자 | 다운로드 사용자 |
| 파일 ID | 내부 식별자 |
| 업무 목적 | 다운로드 사유 |
| 대상 건수 | 파일 데이터 건수 |
| 파일 크기 | Byte |
| 다운로드 시각 | 시작·종료 |
| 결과 | 성공·실패 |
| TraceId | 거래 추적 |
| 사용자 IP | 정책에 따라 |
| 승인번호 | 대량파일 승인 시 |

## **59.5 대용량 보고서 생성**

대용량 보고서는 온라인 요청에서 직접 생성하지 않습니다.

보고서 생성 요청
→ 권한·조건 검증
→ Job 생성
→ Job ID 반환

Batch
→ 데이터 분할 조회
→ 파일 Streaming 작성
→ Storage 저장
→ 완료 상태

화면
→ 상태 조회
→ 다운로드

## **59.6 보고서 Job 요청**

{
"header": {
"serviceId": "SV.Report.requestCustomerExport",
"transactionCode": "SV-REG-0100"
},
"body": {
"baseDate": "20260717",
"searchCondition": {
"customerGrade": "VIP"
},
"fileType": "CSV"
}
}

응답:

{
"result": {
"resultStatus": "SUCCESS",
"resultCode": "S0000"
},
"body": {
"jobId": "JOB202607170001",
"status": "RECEIVED"
}
}

## **59.7 Job 상태 조회**

ServiceId:

OM.Job.selectStatus

또는 업무별 상태 조회:

SV.Report.selectJobStatus

응답:

{
"body": {
"jobId": "JOB202607170001",
"status": "PROCESSING",
"processedCount": 450000,
"totalCount": 1000000,
"progressRate": 45,
"fileId": null
}
}

## **59.8 다운로드 URL**

파일 저장소가 직접 다운로드를 지원한다면 짧은 수명의 서명 URL을 사용할 수 있습니다.

주의사항:

-   유효시간 제한
-   사용자 권한 확인 후 발급
-   재사용 제한
-   파일명 Header 검증
-   감사로그
-   외부 노출 경로 제한

## **59.9 HTTP Range**

매우 큰 파일은 부분 다운로드와 재개를 지원할 수 있습니다.

Range: bytes=1000000-

이를 직접 구현하기보다 저장소나 Web Server의 검증된 기능을 활용하는 것이 안전합니다.

## **59.10 파일 보관기간**

생성 완료
→ 7일 보관
→ EXPIRED
→ 파일 삭제
→ Metadata 보존 또는 정리

업무·보안·감사 기준에 따라 기간을 결정합니다.

사용자에게 만료일시를 안내해야 합니다.

## **제59장 요약**

학습 목표 | 59장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

작은 파일은 Streaming 다운로드할 수 있다.

대용량 보고서는
온라인에서 생성하지 않고 Job으로 접수한다.

파일 다운로드 전 권한을 확인하고
중요 파일은 감사로그를 남긴다.

# **제60장. 대용량 데이터를 안전하게 처리하기**

학습 목표 | 60장. 대용량 데이터를 안전하게 처리하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **60.1 대용량 처리의 기본 위험**

다음 코드는 소량 데이터에서는 동작합니다.

List<Customer> customers =
customerMapper.selectAll();

for (Customer customer : customers) {
process(customer);
}

데이터가 1,000만 건이면 다음 문제가 발생합니다.

Heap 부족
GC 증가
SQL 응답 지연
DB Connection 장기 점유
트랜잭션 장기 유지
장애 시 처음부터 재실행

## **60.2 한 번에 처리하지 않는다**

대용량 데이터는 분할하여 처리합니다.

1~1,000건
→ 처리·Commit

1,001~2,000건
→ 처리·Commit

반복

이 처리 묶음을 Chunk라고 합니다.

## **60.3 Chunk 크기**

Chunk가 너무 작으면:

Commit 횟수 증가
DB 부하 증가
처리속도 저하

Chunk가 너무 크면:

메모리 증가
Rollback 범위 증가
Lock 유지시간 증가
재시작 부담 증가

예:

초기값
500건 또는 1,000건

성능시험 후 조정

정확한 값은 데이터 크기와 SQL 특성에 따라 결정합니다.

## **60.4 Offset Paging의 문제**

대량 데이터에서 다음 방식은 뒤 페이지로 갈수록 느려질 수 있습니다.

OFFSET 9000000 ROWS
FETCH NEXT 1000 ROWS ONLY

대안으로 Keyset Paging을 검토합니다.

WHERE CUSTOMER\_ID > #{lastCustomerId}
ORDER BY CUSTOMER\_ID
FETCH NEXT 1000 ROWS ONLY

## **60.5 처리 기준점**

Keyset Paging에는 정렬 가능한 안정적인 Key가 필요합니다.

예:

CUSTOMER\_ID
CREATE\_DTM + ID
SEQUENCE\_NO

처리 중 데이터가 추가·변경되는 경우 일관성 기준을 정해야 합니다.

## **60.6 기준시각 Snapshot**

Batch 시작시각을 기준으로 대상을 고정할 수 있습니다.

batchBaseDtm = 2026-07-17 01:00:00

WHERE UPDATE\_DTM < batchBaseDtm

실행 중 새로 들어온 데이터는 다음 Batch에서 처리합니다.

## **60.7 Streaming 조회**

MyBatis Cursor 또는 ResultHandler를 사용할 수 있습니다.

개념 예:

try (Cursor<CustomerData> cursor =
customerMapper.streamCustomers(query)) {

for (CustomerData customer : cursor) {
process(customer);
}
}

주의:

-   Cursor 사용 중 DB Connection 유지
-   너무 긴 처리 금지
-   Fetch Size 설정
-   예외 시 자원 정리
-   장시간 트랜잭션 방지

## **60.8 파일 Streaming 작성**

CSV 작성 예:

try (BufferedWriter writer =
storageService.openWriter(fileId)) {

writer.write("customerNo,customerName");
writer.newLine();

for (CustomerData customer : customers) {
writer.write(toCsvLine(customer));
writer.newLine();
}
}

데이터 전체를 문자열로 만든 후 한 번에 저장하지 않습니다.

## **60.9 Excel 대용량 처리**

일반 Excel Workbook은 모든 Cell을 메모리에 보관할 수 있습니다.

대용량은 Streaming Workbook을 사용하거나 CSV를 우선 검토합니다.

수백만 건
→ XLSX보다 CSV가 적합할 수 있음

Excel의 최대 행 수와 사용자의 실제 활용방식도 확인해야 합니다.

## **60.10 대량 Insert**

한 건씩 Insert:

INSERT 100만 번

보다 Batch Insert를 검토합니다.

500건씩 Batch
→ Commit

MyBatis ExecutorType.BATCH 또는 DB Bulk 기능을 사용할 수 있습니다.

주의:

-   Batch 실패 위치
-   생성 Key 처리
-   메모리 Flush
-   중복 데이터
-   부분 실패 정책

## **60.11 대량 Update**

조건 없는 대량 Update를 온라인에서 실행하면 안 됩니다.

UPDATE CUSTOMER
SET STATUS = 'X'

필요한 통제:

-   대상 사전 산정
-   예상 건수 확인
-   승인
-   실행시간대
-   분할 처리
-   Undo·Redo 용량
-   Lock 영향
-   Rollback 계획
-   결과 검증

## **60.12 예상 건수와 실제 건수**

예상 대상
100,000건

실제 처리
2,500,000건

차이가 크면 조건 오류일 수 있습니다.

Batch 실행 전에 허용범위를 검증합니다.

## **60.13 온라인과 Batch 분리 기준**

다음 조건 중 하나라도 해당하면 Batch 또는 비동기 Job을 검토합니다.

예상 처리시간이 온라인 Timeout 초과

대상 건수가 수만 건 이상

파일 생성이 필요

외부 호출을 반복

대량 DB 변경

사용자가 즉시 결과를 볼 필요 없음

실패 재처리가 필요

## **제60장 요약**

학습 목표 | 60장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

대용량 데이터는 List로 한 번에 적재하지 않는다.

Chunk·Streaming·Keyset Paging을 사용한다.

처리 기준시각과 정렬 Key를 고정하여
중복과 누락을 방지한다.

# **제61장. Batch 구조 이해하기**

학습 목표 | 61장. Batch 구조 이해하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **61.1 Batch란 무엇인가요?**

Batch는 많은 데이터를 일정한 규칙으로 일괄 처리하는 작업입니다.

예:

일별 고객등급 계산

캠페인 대상자 생성

거래실적 집계

파일 수신 데이터 적재

오류 거래 재처리

보관기간 만료 데이터 삭제

## **61.2 Batch 기본 구조**

Job
├─ Step 1
├─ Step 2
└─ Step 3

예:

CampaignTargetGenerationJob
├─ 대상조건 검증
├─ 대상 고객 생성
└─ 처리결과 집계

## **61.3 Reader·Processor·Writer**

Reader
→ 데이터를 읽는다.

Processor
→ 업무 규칙을 적용한다.

Writer
→ 결과를 저장한다.

예:

Reader
고객 데이터 1,000건 조회

Processor
캠페인 대상조건 판단

Writer
대상자 테이블 Insert

## **61.4 Batch Service와 온라인 Service**

Batch가 온라인 ServiceId를 반복 호출하는 구조는 비효율적일 수 있습니다.

100만 건
→ 온라인 ServiceId 100만 번 호출

공통 업무 규칙은 재사용하되 Batch용 Application Service를 별도로 설계할 수 있습니다.

공통 Rule
공통 Domain Service
Batch Application Service
Online Application Service

## **61.5 Job Parameter**

Job 실행을 구분하는 입력값입니다.

예:

baseDate=20260717
campaignId=CMP001
fileId=FILE001
executionType=NORMAL

동일 Job과 동일 식별 파라미터의 중복실행 정책을 정의해야 합니다.

## **61.6 Job Instance와 Execution**

Job Instance
\= 2026-07-17 고객등급 산정

Execution 1
\= 최초 실행, 실패

Execution 2
\= 재시작, 성공

같은 업무일자의 재시작인지 새로운 재실행인지 구분해야 합니다.

## **61.7 Step 상태**

STARTING
STARTED
COMPLETED
FAILED
STOPPING
STOPPED
ABANDONED
UNKNOWN

운영 화면에서는 Job 전체와 Step별 상태를 확인할 수 있어야 합니다.

## **61.8 Skip**

일부 잘못된 데이터를 건너뛰고 계속 처리하는 기능입니다.

예:

100만 건 중
형식 오류 10건

→ 10건 Skip
→ 999,990건 처리

모든 오류를 Skip하면 데이터 품질 문제가 숨겨집니다.

필요 정책:

-   Skip 가능한 오류
-   최대 Skip 건수
-   Skip 비율
-   오류 데이터 저장
-   운영 경보

## **61.9 Retry**

일시적인 DB Lock이나 외부 503은 제한적으로 다시 시도할 수 있습니다.

Retry 가능
일시적 네트워크 오류
Lock Timeout
HTTP 503

Retry 금지
입력형식 오류
업무상 대상 아님
SQL 문법 오류

## **61.10 Restart**

실패 지점부터 이어서 처리합니다.

예:

전체 100만 건

650,000건 처리 후 실패

Restart
→ Checkpoint 이후부터 처리

Restart를 지원하려면 Reader 순서와 처리 위치가 안정적이어야 합니다.

## **61.11 Rerun**

새로운 실행으로 다시 처리합니다.

Rerun
→ 처음부터 다시 실행
또는
→ 업무 범위를 새로 지정

이미 처리된 데이터가 중복 반영되지 않도록 멱등성이 필요합니다.

## **61.12 Batch Idempotency**

다음 방법을 사용할 수 있습니다.

-   처리완료 상태 컬럼
-   업무 기준 Unique Key
-   Job Execution ID
-   Event ID
-   대상자 생성 버전
-   처리이력 테이블

예:

WHERE PROCESS\_STATUS = 'READY'

성공 후:

PROCESS\_STATUS = 'COMPLETED'

## **제61장 요약**

학습 목표 | 61장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Batch는 Job과 Step으로 구성한다.

Step은 Reader·Processor·Writer로 분리한다.

Retry, Skip, Restart, Rerun은 서로 다른 기능이다.

재실행 시 중복처리를 막을 수 있어야 한다.

# **제62장. Scheduler와 실행 통제**

학습 목표 | 62장. Scheduler와 실행 통제의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 처리량과 정합성: 성능 최적화는 원본 데이터와 처리 책임을 흐리지 않는 범위에서 적용해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **62.1 Scheduler란 무엇인가요?**

Scheduler는 Job을 언제 시작할지 결정합니다.

매일 01:00 실행

매월 말일 실행

파일 도착 후 실행

선행 Job 완료 후 실행

Scheduler는 업무 처리 자체가 아닙니다.

## **62.2 잘못된 Scheduler 구조**

@Scheduled(cron = "0 0 1 \* \* \*")
public void execute() {

// 대상 100만 건 조회
// 업무규칙 수행
// DB 저장
// 파일 생성
// 외부 호출
}

문제:

-   수동 실행 어려움
-   재시작 어려움
-   상태 추적 어려움
-   테스트 어려움
-   다중 서버 중복실행
-   실행 파라미터 관리 불가

## **62.3 권장 구조**

@Scheduled(cron = "0 0 1 \* \* \*")
public void trigger() {

jobLauncher.launch(
"CustomerGradeCalculationJob",
JobParameters.of(
"baseDate",
businessDateProvider.today()
)
);
}

Scheduler는 Job 실행만 요청합니다.

실제 업무는 Batch Job이 처리합니다.

## **62.4 Cron 표현식**

예:

매일 01:00
0 0 1 \* \* \*

매시간 정각
0 0 \* \* \* \*

월요일 02:00
0 0 2 \* \* MON

프레임워크별 Cron 필드 수가 다를 수 있으므로 프로젝트 기준을 확인합니다.

## **62.5 다중 서버 중복실행**

WAS가 4대이고 각 서버에 같은 Scheduler가 있으면 Job이 4번 실행될 수 있습니다.

WAS 1 실행
WAS 2 실행
WAS 3 실행
WAS 4 실행

방지 방법:

-   전용 Batch 서버
-   DB Lock
-   Scheduler Cluster
-   Leader Election
-   중앙 Job Control
-   실행 Unique Constraint

## **62.6 DB Lock 방식**

개념:

Job 실행 전 Lock 획득

성공
→ Job 실행

실패
→ 다른 서버가 실행 중
→ 종료

Lock에는 만료시간이 필요합니다.

서버가 비정상 종료되면 Lock이 영구적으로 남을 수 있기 때문입니다.

## **62.7 실행 달력**

금융 업무는 단순 요일이 아니라 영업일 기준이 필요할 수 있습니다.

매월 말일
≠ 매월 마지막 영업일

필요 기준:

-   영업일
-   휴일
-   월말
-   분기말
-   연말
-   특별 영업일
-   시스템 점검일

운영 달력은 공통 기준정보로 관리합니다.

## **62.8 선행·후행 Job**

고객 데이터 적재
→ 고객등급 산정
→ 캠페인 대상 생성
→ 결과 파일 생성

선행 Job 실패 시 후행 Job을 실행하면 안 됩니다.

의존관계를 DAG 형태로 관리할 수 있습니다.

## **62.9 실행 Window**

Job은 허용된 시간 안에 끝나야 합니다.

예:

실행 가능시간
01:00~05:00

온라인 개시
06:00

05:00까지 끝나지 않으면 다음 정책이 필요합니다.

-   계속 실행
-   안전 중지
-   온라인 자원 제한
-   운영자 승인
-   다음 실행 연기

## **62.10 Misfire**

정해진 시각에 Scheduler가 중단되어 실행되지 못한 상황입니다.

복구 시 정책:

즉시 실행

다음 주기까지 대기

누락된 모든 회차 실행

가장 최근 회차만 실행

업무별로 결정해야 합니다.

## **62.11 수동 실행**

운영자는 필요 시 Job을 수동 실행할 수 있어야 합니다.

필수 통제:

-   실행 권한
-   파라미터 검증
-   중복실행 확인
-   승인
-   실행 사유
-   감사로그
-   예상 대상 건수
-   Dry Run

## **62.12 Job 중지**

중지 방식:

Graceful Stop
→ 현재 Chunk 완료 후 중지

Force Stop
→ Thread 강제 종료 시도

가능하면 Graceful Stop을 사용합니다.

강제 종료는 처리상태가 불명확해질 수 있습니다.

## **제62장 요약**

학습 목표 | 62장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Scheduler는 Job 실행시각을 결정할 뿐
업무 로직을 직접 처리하지 않는다.

다중 서버에서는 중복실행을 통제해야 한다.

영업일·선행 Job·실행 Window·Misfire 정책을
업무별로 정의해야 한다.

# **제63장. Batch 운영·재처리·장애 대응**

학습 목표 | 63장. Batch 운영·재처리·장애 대응의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **63.1 Batch 운영에서 필요한 정보**

운영자는 다음 질문에 답할 수 있어야 합니다.

어느 Job이 실행 중인가?

누가 실행했는가?

몇 건을 처리했는가?

어디에서 실패했는가?

재시작 가능한가?

중복 처리 위험은 없는가?

후행 Job에 영향이 있는가?

온라인 서비스에 부하를 주는가?

## **63.2 Job 실행정보**

| **항목** | **설명** |
| --- | --- |
| Job Name | Job 식별자 |
| Job Execution ID | 실행 식별자 |
| Business Date | 업무 기준일 |
| Parameters | 실행조건 |
| Start Dtm | 시작시각 |
| End Dtm | 종료시각 |
| Status | 실행상태 |
| Read Count | 읽은 건수 |
| Write Count | 저장 건수 |
| Skip Count | 제외 건수 |
| Retry Count | 재시도 건수 |
| Commit Count | Commit 횟수 |
| Rollback Count | Rollback 횟수 |
| Error Code | 실패 코드 |
| Server ID | 실행 서버 |
| TraceId | 실행 추적 |

## **63.3 진행률**

진행률
\= 처리 건수 ÷ 전체 대상 건수

전체 대상 건수를 미리 산정하기 어려운 경우 다음 정보를 제공합니다.

처리 건수
현재 Key
처리 속도
예상 종료시각
마지막 진행시각

진행률은 정확한 보장이 아니라 추정값일 수 있습니다.

## **63.4 Heartbeat**

장시간 Job이 살아 있는지 확인하기 위해 Heartbeat를 기록합니다.

lastHeartbeatDtm
lastProcessedKey
processedCount

Heartbeat가 일정 시간 갱신되지 않으면 고착 가능성을 경보합니다.

## **63.5 실패 유형**

| **실패** | **예** | **처리** |
| --- | --- | --- |
| 데이터 오류 | 잘못된 날짜 | Skip·오류파일 |
| 일시 오류 | DB Lock | Retry |
| 시스템 오류 | DB 연결 실패 | Step 실패 |
| 용량 오류 | 디스크 부족 | 즉시 중지 |
| 계약 오류 | 컬럼 불일치 | 수정 후 재실행 |
| 운영 중지 | 관리자 Stop | Restart 가능 |
| 서버 장애 | 프로세스 종료 | 상태 복구 필요 |

## **63.6 오류 데이터 저장**

Skip한 데이터는 다음 정보를 저장합니다.

Job Execution ID
원본 Record Key
오류코드
오류메시지
발생 Step
발생시각
재처리 상태

개인정보 원문 전체를 오류 테이블에 저장하지 않도록 합니다.

## **63.7 재처리 방식**

### **건별 재처리**

실패한 10건만 다시 처리

### **범위 재처리**

고객번호 10000~20000 재처리

### **Step 재시작**

실패 Step의 Checkpoint부터 재시작

### **Job 전체 재실행**

전체 결과를 안전하게 초기화한 후 재실행

업무 영향과 멱등성을 확인한 뒤 선택합니다.

## **63.8 Dry Run**

실제 DB를 변경하지 않고 예상 결과를 확인합니다.

대상 건수
예상 변경 건수
오류 예상 건수
처리 예상시간

대량 Update·Delete 전에 유용합니다.

## **63.9 Batch와 온라인 자원 분리**

같은 Tomcat과 DB Pool을 공유하면 Batch가 온라인 거래 자원을 독점할 수 있습니다.

분리 방법:

-   전용 Batch 인스턴스
-   Batch 전용 DataSource
-   별도 DB Pool
-   실행시간 분리
-   Resource Limit
-   업무별 동시성 제한
-   Query Resource Group

## **63.10 Batch DB Pool**

Batch가 Connection을 너무 많이 사용하면 온라인 업무가 Connection을 얻지 못할 수 있습니다.

온라인 Pool
별도

Batch Pool
별도

또는 전체 DB 허용 Connection 범위 안에서 크기를 제한합니다.

## **63.11 Batch 장애 대응 순서**

1\. Job 상태 확인

2\. 마지막 Heartbeat 확인

3\. 처리건수와 마지막 Key 확인

4\. DB Lock·Pool·SQL 확인

5\. 파일·디스크 용량 확인

6\. 외부 연계 상태 확인

7\. 중복실행 여부 확인

8\. Stop·Restart·Rerun 결정

9\. 후행 Job 영향 확인

10\. 오류 데이터 재처리

## **63.12 재실행 전 확인**

이미 처리된 데이터가 있는가?

다시 처리해도 중복되지 않는가?

부분 Commit이 발생했는가?

Checkpoint가 신뢰할 수 있는가?

외부 시스템에 이미 전달되었는가?

후행 Job이 일부 결과를 사용했는가?

## **63.13 Job Control 상태**

ENABLED
→ 자동 실행 허용

DISABLED
→ 실행 금지

PAUSED
→ 일시 중지

MANUAL\_ONLY
→ 수동 실행만 허용

DEPRECATED
→ 폐기 예정

## **제63장 요약**

학습 목표 | 63장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Batch 운영에는
상태·처리건수·Checkpoint·Heartbeat가 필요하다.

실패 후 무조건 처음부터 다시 실행하지 않는다.

온라인과 Batch의 Thread·DB Pool·실행시간을
가능한 범위에서 분리해야 한다.

# **3\. 목표 아키텍처**

## **3.1 Cache 구조**

\[업무 Service\]
│
▼
\[Cache Service\]
│
├─ Hit → 결과 반환
│
└─ Miss
│
▼
\[DAO·DB\]
│
▼
Cache 저장

\[OM 기준정보 변경\]
│
▼
\[Cache Invalidation Event\]
│
├─ SV WAR Cache 삭제
├─ IC WAR Cache 삭제
└─ CM WAR Cache 삭제

## **3.2 대용량 파일 처리**

\[사용자\]
│
│ 파일 업로드
▼
\[Gateway·업무 WAR\]
├─ 인증·권한
├─ 파일 검증
├─ Streaming 저장
└─ Job 접수
│
▼
\[공용 Storage\]
│
▼
\[Batch Job\]
Reader
Processor
Writer
│
├─ 업무 DB
├─ 결과 파일
└─ 오류 파일

## **3.3 Scheduler·Batch 구조**

\[Operating Calendar\]
│
▼
\[Scheduler\]
│
Job 실행 요청
│
▼
\[Job Launcher\]
│
▼
\[Batch Job\]
┌─────┼─────┐
▼ ▼ ▼
\[Step1\]\[Step2\]\[Step3\]
│
Reader
Processor
Writer
│
▼
\[Job Repository\]
상태·Checkpoint·건수
│
▼
\[OM Batch Control\]
조회·중지·재시작·재처리

# **4\. 표준 형식**

## **4.1 Job 요청**

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Report.requestCustomerExport",
"transactionCode": "SV-REG-0100",
"processingType": "REGISTRATION",
"idempotencyKey": "SV-USER01-20260717-REPORT001"
},
"body": {
"baseDate": "20260717",
"fileType": "CSV",
"searchCondition": {
"customerGrade": "VIP"
}
}
}

## **4.2 Job 접수 응답**

{
"result": {
"resultStatus": "SUCCESS",
"resultCode": "S0000"
},
"body": {
"jobId": "JOB202607170001",
"jobName": "CustomerExportJob",
"status": "RECEIVED",
"requestedDtm": "2026-07-17T10:30:00+09:00"
}
}

## **4.3 Job 상태 응답**

{
"body": {
"jobId": "JOB202607170001",
"status": "PROCESSING",
"totalCount": 1000000,
"readCount": 460000,
"writeCount": 459990,
"skipCount": 10,
"progressRate": 46,
"lastHeartbeatDtm": "2026-07-17T10:35:12+09:00",
"fileId": null
}
}

## **4.4 완료 응답**

{
"body": {
"jobId": "JOB202607170001",
"status": "COMPLETED",
"readCount": 1000000,
"writeCount": 999980,
"skipCount": 20,
"fileId": "FILE202607170001",
"fileName": "customer\_export\_20260717.csv",
"fileSize": 524288000,
"expiresDtm": "2026-07-24T23:59:59+09:00"
}
}

# **5\. 구성요소 및 속성**

| **구성요소** | **주요 속성** |
| --- | --- |
| Cache Registry | Cache명, TTL, 최대크기 |
| Cache Key | 업무·데이터·식별자 |
| Cache Event | 대상 Key, 변경버전 |
| File Metadata | File ID, 크기, 상태 |
| Storage | 공용 저장위치 |
| Job Request | Job명, 파라미터, 요청자 |
| Job Repository | 상태, Execution, Checkpoint |
| Batch Step | Reader, Processor, Writer |
| Scheduler | Cron, 달력, Misfire |
| Job Lock | 중복실행 방지 |
| Error Store | 실패 Record |
| OM Batch Control | 실행·중지·재시작 |
| Audit Log | 업로드·다운로드·수동 실행 |

# **6\. 책임 경계와 RACI**

| **활동** | **AA** | **FW** | **DEV** | **DBA** | **OM** | **SEC** | **QA** | **OPS** | **Storage** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cache 적용 결정 | A | C | R | C | C | C | C | I | I |
| Cache 공통기능 | C | A/R | C | I | C | C | C | C | I |
| 파일 검증 | C | C | R | I | C | A/C | C | C | C |
| Storage 운영 | C | I | C | I | C | C | I | A/C | R |
| Batch 설계 | A | C | R | C | C | I | C | C | I |
| SQL·Chunk 검토 | C | I | R | A | I | I | C | C | I |
| Scheduler 등록 | C | C | C | I | A/R | I | C | C | I |
| 재처리 | C | C | C | C | A/C | I | C | R | I |
| Batch 보안 | C | C | C | I | C | A/R | C | C | C |
| 성능시험 | C | C | C | C | I | I | A/R | C | C |
| 운영 인수 | C | C | C | C | C | C | C | A/R | C |

# **7\. 정상 처리 흐름**

## **7.1 Cache 조회**

1\. Service가 Cache Service 호출

2\. Cache Key 생성

3\. Cache Hit 확인

4\. Hit면 즉시 반환

5\. Miss면 DB 조회

6\. 조회결과 검증

7\. Cache 저장

8\. 업무 응답

## **7.2 파일 업로드**

1\. 사용자 인증·업로드 권한 확인

2\. 파일 크기·확장자·Signature 검증

3\. Streaming으로 임시 저장

4\. Hash 생성

5\. 악성파일 검사

6\. File Metadata 저장

7\. Batch Job 접수

8\. Job ID 반환

## **7.3 Batch 실행**

1\. Scheduler가 Job 실행 요청

2\. 중복실행 Lock 확인

3\. Job Execution 생성

4\. Reader가 Chunk 조회

5\. Processor가 업무 규칙 수행

6\. Writer가 결과 저장

7\. Chunk Commit

8\. Checkpoint 갱신

9\. 다음 Chunk 반복

10\. Job 완료상태 기록

11\. 후행 Job·알림 실행

# **8\. 오류·Timeout·장애 흐름**

## **8.1 Cache 장애**

Cache 조회 실패
→ 원본 DB 조회
→ Cache 오류 Metric
→ 업무는 성능 저하 상태로 계속

중요 통제정책은 별도 Fail 정책을 적용합니다.

## **8.2 파일 검증 실패**

허용되지 않은 파일
→ 저장 중단
→ 임시 파일 삭제
→ 업로드 오류
→ 보안로그

## **8.3 Storage 용량 부족**

파일 저장 실패
→ Job 접수 안 함
→ 임시 파일 정리
→ 시스템 오류
→ 운영 경보

## **8.4 Batch Chunk 실패**

Chunk 처리 중 오류
→ 현재 Chunk Rollback
→ Retry 정책 확인
→ 실패 시 Step FAILED
→ Checkpoint 유지

## **8.5 Scheduler 중복실행**

Job Lock 획득 실패
→ 다른 인스턴스 실행 중
→ 신규 실행 거부
→ 중복실행 로그

## **8.6 Heartbeat 고착**

Job 상태 PROCESSING
\+ Heartbeat 장시간 미갱신
→ STALE 경보
→ 실제 프로세스 확인
→ STOPPED·UNKNOWN 결정

# **9\. 정상 예시**

Job
CustomerExportJob

대상
1,000,000건

Chunk
1,000건

Commit
1,000회

Reader
Keyset Paging

Writer
CSV Streaming

처리결과
999,980건 성공
20건 Skip

파일
500MB

상태
COMPLETED

보관기간
7일

# **10\. 금지 예시**

## **10.1 무제한 Cache**

new ConcurrentHashMap<>();

최대 크기와 만료정책 없이 사용합니다.

## **10.2 모든 업무 결과 Cache**

실시간 잔액
승인상태
거래결과

를 장시간 Cache합니다.

## **10.3 파일 전체 메모리 적재**

byte\[\] file =
multipartFile.getBytes();

## **10.4 WAS 로컬 영구 저장**

/tomcat/webapps/files

에 업무파일을 영구 저장합니다.

## **10.5 온라인 대량 보고서 생성**

HTTP 요청
→ 20분 동안 300만 건 조회
→ Excel 생성

## **10.6 Scheduler 내부 업무 구현**

@Scheduled
public void run() {
// 전체 Batch 업무 코드
}

## **10.7 다중 서버 중복실행 방치**

WAS 4대
→ 동일 Job 4회 실행

## **10.8 실패 Batch 무조건 전체 재실행**

부분 Commit과 외부 처리결과를 확인하지 않고 재실행합니다.

# **11\. 연계 규칙**

온라인 거래
→ Job 접수 ServiceId

Job 실행
→ Batch Application Service

파일 저장
→ 공용 Storage Adapter

외부 대량 송신
→ EAI·Queue·Batch Client

완료 알림
→ Event·Message

운영 제어
→ OM Job Control

온라인 Service가 Batch 내부 Step을 직접 실행하지 않습니다.

Batch도 화면 Controller를 호출하지 않습니다.

공통 업무 Rule과 Domain Service는 재사용할 수 있습니다.

# **12\. 데이터 및 상태관리**

## **12.1 Job 상태**

RECEIVED
→ QUEUED
→ STARTING
→ PROCESSING
→ COMPLETED

PROCESSING
→ FAILED

PROCESSING
→ STOPPING
→ STOPPED

PROCESSING
→ UNKNOWN

## **12.2 파일 상태**

RECEIVING
→ UPLOADED
→ SCANNING
→ SAFE
→ PROCESSING
→ AVAILABLE
→ EXPIRED
→ DELETED

## **12.3 Cache 상태**

LOADED
INVALIDATED
EXPIRED
LOAD\_FAILED
DISABLED

## **12.4 재처리 상태**

NOT\_REQUIRED
RETRYABLE
RETRYING
REPROCESSED
MANUAL\_REVIEW
ABANDONED

# **13\. 성능·용량·확장성**

| **영역** | **기준** |
| --- | --- |
| Cache | 최대 Entry·Memory 제한 |
| Cache TTL | 데이터별 차등 |
| 파일 업로드 | Streaming |
| 파일 다운로드 | Streaming·Range |
| 대용량 조회 | Keyset Paging |
| Chunk | 성능시험 후 결정 |
| Batch Pool | 온라인 Pool과 분리 검토 |
| Thread | Job별 동시성 제한 |
| Storage | 최대용량·보관기간 |
| Scheduler | 중복실행 방지 |
| Queue | 적체량·처리율 |
| Outbox | 정리주기·인덱스 |
| 로그 | 건별 과다 로그 방지 |

# **14\. 보안·개인정보·감사**

파일 확장자와 실제 내용을 함께 검증한다.

사용자 파일명을 저장경로로 사용하지 않는다.

중요 파일은 악성코드 검사를 수행한다.

다운로드 권한을 File ID 기준으로 확인한다.

대량 개인정보 파일은 암호화·보관기간을 적용한다.

수동 Batch 실행과 재처리는 감사로그를 남긴다.

오류 파일과 DLQ에도 개인정보 보호기준을 적용한다.

만료 파일은 정책에 따라 안전하게 삭제한다.

# **15\. 운영·모니터링·장애 대응**

운영 지표:

| **지표** | **의미** |
| --- | --- |
| Cache Hit Ratio | Cache 효과 |
| Cache Eviction | 용량 부족 가능성 |
| File Upload Failure | 업로드 문제 |
| Storage Usage | 저장공간 |
| Running Jobs | 실행 중 Job |
| Job Failure Rate | Batch 안정성 |
| Read·Write Count | 처리 진척 |
| Skip·Retry Count | 데이터 품질·일시 오류 |
| Heartbeat Delay | Job 고착 |
| Queue Depth | 비동기 적체 |
| DLQ Count | 반복 실패 |
| Batch DB Pool | 온라인 영향 |

# **16\. 자동검증 및 품질 Gate**

| **Gate** | **검증** |
| --- | --- |
| Cache | 최대크기·TTL 필수 |
| Cache Key | 사용자·업무 범위 정합성 |
| Cache 데이터 | 민감정보 저장 검토 |
| Upload | 크기·확장자·Signature |
| 파일명 | 경로 이동 문자 차단 |
| Storage | WAS 로컬 영구저장 금지 |
| Memory | getBytes() 대용량 사용 검사 |
| 온라인 | 장시간 파일 생성 금지 |
| Batch | Chunk 처리 |
| Paging | 대량 Offset 사용 검토 |
| Scheduler | 업무로직 직접 작성 금지 |
| 중복실행 | Job Lock 존재 |
| Restart | Checkpoint 검증 |
| 보안 | 다운로드 권한·감사 |
| 운영 | Job 상태·Heartbeat 필수 |

# **17\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| CBF-001 | Cache Hit | DB 미호출 |
| CBF-002 | Cache Miss | DB 조회 후 저장 |
| CBF-003 | TTL 만료 | 원본 재조회 |
| CBF-004 | 원본 변경 | Cache 무효화 |
| CBF-005 | Cache 장애 | DB Fallback |
| CBF-006 | Cache Stampede | 단일 Load |
| CBF-007 | 최대 Entry 초과 | Eviction |
| FILE-001 | 정상 파일 업로드 | SAFE 상태 |
| FILE-002 | 허용되지 않은 확장자 | 차단 |
| FILE-003 | MIME 위조 | 차단 |
| FILE-004 | 최대크기 초과 | 업로드 중단 |
| FILE-005 | 경로 이동 파일명 | 안전한 이름으로 저장 |
| FILE-006 | 악성파일 | INFECTED |
| FILE-007 | Storage 부족 | 오류·경보 |
| FILE-008 | 다운로드 권한 없음 | 차단 |
| FILE-009 | 만료 파일 다운로드 | 만료 오류 |
| FILE-010 | 대용량 다운로드 | Heap 증가 없이 Streaming |
| BAT-001 | 정상 Batch | COMPLETED |
| BAT-002 | Chunk 중간 실패 | 현재 Chunk Rollback |
| BAT-003 | 일시 DB 오류 | 제한적 Retry |
| BAT-004 | 데이터 오류 | Skip·오류저장 |
| BAT-005 | Skip 한도 초과 | Step FAILED |
| BAT-006 | 서버 장애 후 Restart | Checkpoint부터 재개 |
| BAT-007 | 동일 파라미터 중복실행 | 차단 |
| BAT-008 | 다중 서버 Scheduler | 1회만 실행 |
| BAT-009 | Heartbeat 중단 | STALE 경보 |
| BAT-010 | 수동 실행 | 권한·감사 검증 |
| BAT-011 | Graceful Stop | Chunk 후 중지 |
| BAT-012 | 전체 Rerun | 중복 데이터 없음 |
| BAT-013 | 후행 Job 선행실패 | 실행 차단 |
| BAT-014 | 영업일 휴일 | 스케줄 미실행 |
| BAT-015 | Misfire 복구 | 정의된 정책 적용 |
| BAT-016 | 온라인 부하 중 Batch | 자원 격리 |
| BAT-017 | 오류 파일 개인정보 | 마스킹·암호화 |
| BAT-018 | 보관기간 만료 | 파일 안전 삭제 |

# **18\. 제8부 체크리스트**

## **18.1 Cache**

| **점검 항목** | **확인** |
| --- | --- |
| Cache가 실제로 필요한가? | □ |
| 원본 데이터가 명확한가? | □ |
| Cache Key가 업무범위를 포함하는가? | □ |
| TTL이 데이터별로 정의되었는가? | □ |
| 최대 크기가 제한되는가? | □ |
| 원본 변경 시 무효화되는가? | □ |
| Cache 장애 Fallback이 있는가? | □ |
| Hit Ratio를 측정하는가? | □ |

## **18.2 파일**

| **점검 항목** | **확인** |
| --- | --- |
| 파일 크기를 제한하는가? | □ |
| 확장자·MIME·Signature를 검사하는가? | □ |
| 파일명을 안전하게 변환하는가? | □ |
| Streaming 방식인가? | □ |
| 공용 Storage를 사용하는가? | □ |
| 악성파일 검사를 수행하는가? | □ |
| 다운로드 권한이 있는가? | □ |
| 보관기간과 삭제정책이 있는가? | □ |
| 중요 다운로드를 감사하는가? | □ |

## **18.3 대용량 처리**

| **점검 항목** | **확인** |
| --- | --- |
| 전체 데이터를 List로 적재하지 않는가? | □ |
| Chunk 크기가 정의되었는가? | □ |
| Keyset Paging을 검토했는가? | □ |
| 처리 기준시각이 고정되는가? | □ |
| 예상 대상 건수를 검증하는가? | □ |
| 부분 Commit 정책이 있는가? | □ |
| 실패 지점부터 재시작 가능한가? | □ |

## **18.4 Batch**

| **점검 항목** | **확인** |
| --- | --- |
| Job과 Step이 분리되었는가? | □ |
| Reader·Processor·Writer 책임이 명확한가? | □ |
| Retry와 Skip 기준이 있는가? | □ |
| Skip 최대건수가 있는가? | □ |
| Job Parameter가 명확한가? | □ |
| 중복실행을 차단하는가? | □ |
| Job 상태와 건수를 기록하는가? | □ |
| Heartbeat가 있는가? | □ |
| 재처리 절차가 있는가? | □ |

## **18.5 Scheduler**

| **점검 항목** | **확인** |
| --- | --- |
| Scheduler가 Job 실행만 요청하는가? | □ |
| 업무 달력을 사용하는가? | □ |
| 다중 서버 중복실행이 방지되는가? | □ |
| Misfire 정책이 있는가? | □ |
| 선행·후행 Job 관계가 정의되었는가? | □ |
| 실행 Window가 있는가? | □ |
| 수동 실행 권한과 감사가 있는가? | □ |
| Graceful Stop이 가능한가? | □ |

## **18.6 운영**

| **점검 항목** | **확인** |
| --- | --- |
| OM에서 Job 상태를 조회할 수 있는가? | □ |
| 처리건수와 진행률을 볼 수 있는가? | □ |
| 오류 Record를 확인할 수 있는가? | □ |
| Restart·Rerun을 구분하는가? | □ |
| Queue·DLQ 적체를 모니터링하는가? | □ |
| Batch 자원이 온라인과 격리되는가? | □ |
| 파일과 Job의 TraceId가 연결되는가? | □ |

# **19\. 변경·호환성·폐기 관리**

## **19.1 Cache 정책 변경**

TTL과 최대 크기 변경 시 다음 영향을 확인합니다.

DB 조회량
Heap 사용량
Hit Ratio
데이터 최신성
WAS 간 정합성
장애 시 Fallback

운영환경에서 즉시 큰 폭으로 변경하지 않고 단계적으로 적용합니다.

## **19.2 파일 형식 변경**

CSV 컬럼 추가·삭제·순서 변경도 외부 계약 변경입니다.

확인 대상:

업로드 Parser
Batch Reader
오류 파일
다운로드 사용자
외부 시스템
계약 버전

## **19.3 Batch 로직 변경**

Batch 변경 전 다음을 확인합니다.

기존 미완료 Job
Checkpoint 호환성
Job Parameter
테이블 구조
재시작 가능 여부
동일 업무일자 재실행
후행 Job 영향

코드 변경 후 이전 Execution을 Restart하면 상태 구조가 호환되지 않을 수 있습니다.

## **19.4 Scheduler 변경**

실행시각 변경은 다른 Job과 DB 부하에 영향을 줄 수 있습니다.

선행 Job 종료시각
온라인 개시시간
DB 백업시간
외부기관 운영시간
동시 실행 Job

을 함께 확인합니다.

## **19.5 Job 폐기**

ACTIVE
→ MANUAL\_ONLY
→ DEPRECATED
→ DISABLED
→ REMOVED

폐기 전 확인:

-   최근 실행이력
-   호출 화면
-   선행·후행 Job
-   운영 매뉴얼
-   Scheduler 등록
-   오류 데이터
-   파일 보관
-   관련 테이블

# **20\. 시사점**

## **20.1 핵심 아키텍처 판단**

제8부의 핵심 판단은 다음과 같습니다.

빠른 반복 조회
→ Cache

작은 즉시 파일
→ Streaming 온라인 처리

대용량 파일·보고서
→ 비동기 Job

대량 데이터
→ Chunk Batch

정기 실행
→ Scheduler

실패 복구
→ Checkpoint·Restart·재처리

## **20.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| 무제한 Cache | Heap 부족 |
| Cache 무효화 누락 | 오래된 데이터 |
| Local Cache 불일치 | 서버별 다른 결과 |
| 파일 전체 메모리 적재 | OutOfMemory |
| WAS 로컬 파일 저장 | 파일 유실·불일치 |
| 악성파일 미검사 | 보안사고 |
| 온라인 대량작업 | Thread·DB Pool 고갈 |
| Offset 대량 Paging | 후반부 성능 저하 |
| Scheduler 업무로직 결합 | 재처리·테스트 어려움 |
| 다중 서버 중복실행 | 중복 데이터 |
| Retry·Skip 남용 | 오류 은폐 |
| Restart 불가 | 전체 재실행 부담 |
| Batch·온라인 자원 공유 | 온라인 장애 |
| 파일 보관정책 없음 | 저장공간·개인정보 위험 |

## **20.3 우선 보완 과제**

1\. Cache 대상과 비대상 분류
2\. Cache Registry·TTL·최대크기 표준화
3\. Cache 무효화 Event 설계
4\. 파일 Metadata와 공용 Storage 적용
5\. 대용량 업·다운로드 Streaming 적용
6\. 장시간 보고서 Job 전환
7\. Batch Job·Step 표준 템플릿
8\. Chunk·Checkpoint·Restart 표준화
9\. Scheduler와 Job 로직 분리
10\. 다중 서버 중복실행 방지
11\. OM Batch Control 구축
12\. 파일·Batch 보안·감사 강화

## **20.4 중장기 발전 방향**

기본 Local Cache
→ Event 기반 무효화
→ 다단 Cache
→ Cache 정책 자동조정

단순 파일 저장
→ Object Storage
→ 서명 URL
→ 파일 생명주기 자동화

단일 Batch 서버
→ 분산 Batch
→ Partition
→ 원격 Chunk
→ Queue 기반 확장

수동 장애대응
→ 자동 고착 탐지
→ 안전한 자동 Restart
→ 처리량 예측
→ 용량 자동조정

자동 재시작은 멱등성과 Checkpoint 신뢰성이 충분히 검증된 Job에만 적용해야 합니다.

# **21\. 마무리말**

제8부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

Cache
\= 자주 조회하는 데이터의 임시 복사본

파일 Storage
\= WAS와 분리된 공용 저장소

Streaming
\= 전체 데이터를 메모리에 올리지 않는 처리

Batch
\= 대량 데이터를 분할하여 처리하는 작업

Scheduler
\= Batch를 언제 시작할지 결정하는 기능

Checkpoint
\= 실패 후 다시 시작할 위치

안전한 대용량 처리 구조는 다음과 같습니다.

화면
→ 작업 접수
→ Job ID 반환
→ Batch 실행
→ Chunk 처리
→ 진행상태 저장
→ 파일 생성
→ 완료 알림
→ 권한 확인 후 다운로드

초보 개발자가 Cache·파일·Batch 기능을 구현하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

이 데이터는 정말 Cache가 필요한가?

오래된 값이 얼마나 허용되는가?

원본 변경 시 Cache는 어떻게 삭제되는가?

파일 전체를 메모리에 올리고 있지는 않은가?

WAS 로컬 디스크에 영구 저장하고 있지는 않은가?

이 작업이 온라인 Timeout 안에 끝나는가?

대량 데이터를 몇 건씩 Commit할 것인가?

실패하면 어디서부터 다시 시작하는가?

같은 Job이 두 번 실행되지 않는가?

다중 서버에서 Scheduler가 중복 실행되지 않는가?

오류 데이터만 별도로 재처리할 수 있는가?

Batch가 온라인 Thread와 DB Pool을 독점하지 않는가?

운영자가 Job 상태와 처리건수를 확인할 수 있는가?

파일과 Batch 작업을 TraceId로 추적할 수 있는가?

이 질문에 답할 수 있다면 단순히 반복문을 작성하거나 파일을 저장하는 개발자를 넘어, 대용량 업무를 안정적으로 처리하고 운영할 수 있는 개발자가 될 수 있습니다.
