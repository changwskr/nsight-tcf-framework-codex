<!-- source: ztcf-집필본/NSIGHT TCF Chapter 12- 변경 영향도 분석.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제12장. 변경 영향도 분석

## 이 장을 시작하며

제9장에서는 저장소 전체 구조를 파악했고, 제10장에서는 화면과 ServiceId에서 프로그램과 SQL까지 정방향으로 추적했다.

제11장에서는 SQL과 테이블에서 시작하여 ServiceId와 영향 화면까지 역방향으로 추적했다.

이제 그 결과를 이용해 실제 변경 범위를 결정해야 한다.

개발자가 받는 변경 요청은 대개 간단한 문장으로 전달된다.

\`\`\`text id=“dwa67c” 고객번호 길이를 10자리에서 12자리로 변경해 주세요.

고객요약 화면에 최종 거래일을 추가해 주세요.

조회 Timeout을 3초에서 5초로 늘려 주세요.

오류 메시지만 조금 수정해 주세요.

공통 Header에 채널 상세코드를 추가해 주세요.

SV\_CUSTOMER 테이블 컬럼명을 변경해 주세요.

이 ServiceId를 새로운 이름으로 바꿔 주세요.



문장은 짧지만 실제 변경 범위는 짧지 않을 수 있다.

예를 들어 고객번호 길이를 변경하면 다음 영역이 영향을 받을 수 있다.

\`\`\`text id="n0fyho"
화면 입력 길이
→ 화면 Validation
→ 요청 DTO
→ 공통 전문 길이
→ 업무 Rule
→ Mapper Parameter
→ DB 컬럼
→ Index
→ 파일 Layout
→ 외부 연계 전문
→ 로그 마스킹
→ 테스트 데이터
→ 운영 매뉴얼

개발자가 요청 DTO와 SQL만 수정하면 로컬에서는 동작할 수 있다.

그러나 외부 연계가 10자리만 허용하거나, DB Index와 통계정보가 갱신되지 않았거나, 다른 화면이 기존 길이를 전제로 동작한다면 운영에서 장애가 발생한다.

따라서 변경 영향도 분석은 다음과 같은 작업이 아니다.

text id="z6eoba" 수정할 Java 파일을 찾는다. → 수정한다. → Build가 성공하면 완료한다.

변경 영향도 분석의 실제 목적은 다음과 같다.

\`\`\`text id=“eflk99” 무엇을 변경해야 하는가?

무엇을 변경하지 않아야 하는가?

직접 호출하지 않지만 함께 영향을 받는 것은 무엇인가?

기존 사용자와 연계 시스템은 계속 동작하는가?

어떤 순서로 배포해야 하는가?

실패하면 어느 상태로 돌아갈 수 있는가?

어떤 테스트로 변경의 안전성을 증명할 것인가?



NSIGHT TCF의 변경 영향은 다음 전체 연결관계를 기준으로 판단해야 한다.

\`\`\`text id="shiz00"
요구사항
↓
화면·이벤트
↓
ServiceId·거래코드
↓
Handler·Facade·Service·Rule
↓
DAO·Mapper·SQL·DB 객체
↓
업무 WAR·공통 모듈
↓
OM Catalog·거래통제·Timeout
↓
권한·개인정보·감사
↓
Build·Artifact·배포
↓
로그·모니터링·장애 대응
↓
테스트·운영 증적

화면 이벤트와 ServiceId, 프로그램, SQL, DB 객체를 양방향으로 연결하는 추적성 매트릭스가 변경 영향 분석의 기본 자료가 된다.

## 핵심 관점

\`\`\`text id=“0brxlu” 변경 영향도는 수정 파일의 개수가 아니다.

변경으로 인해 행동·계약·데이터·운영 결과가 달라지는 전체 범위다.


\---

\## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 변경 영향도 분석의 목적을 설명한다. |
| 2 | 직접 영향과 간접 영향을 구분한다. |
| 3 | 변경 요청의 기준선과 완료조건을 정의한다. |
| 4 | 화면에서 DB까지 정방향 영향을 추적한다. |
| 5 | DB 객체에서 영향 화면까지 역방향으로 추적한다. |
| 6 | 호출관계가 없는 공유 자원의 간접 영향을 찾는다. |
| 7 | 공통 모듈 변경이 전체 업무 WAR에 미치는 영향을 분석한다. |
| 8 | 요청·응답 계약의 호환성을 판단한다. |
| 9 | ServiceId 변경이 UI·Handler·OM에 미치는 영향을 분석한다. |
| 10 | DB 스키마와 데이터 이행 영향을 분석한다. |
| 11 | Cache·Batch·파일·BI 사용 영향을 확인한다. |
| 12 | 다른 업무 WAR와 외부 시스템 소비자를 식별한다. |
| 13 | Timeout·재시도·멱등성 정책 변경 영향을 판단한다. |
| 14 | 보안·권한·개인정보·감사 영향을 분석한다. |
| 15 | WAR·Tomcat·환경설정·배포순서 영향을 분석한다. |
| 16 | 변경에 맞는 단위·통합·회귀·성능 테스트를 선정한다. |
| 17 | 배포 실패와 데이터 오류에 대한 Rollback 방안을 정의한다. |
| 18 | 변경 위험등급과 승인 수준을 결정한다. |
| 19 | 변경 영향도 분석서를 작성한다. |
| 20 | Pull Request와 배포승인에 영향분석 결과를 연결한다. |

\---

\# 한눈에 보는 변경 영향도 분석 흐름

\`\`\`text id="5h9hnu"
┌────────────────────────────────────────────────────────────┐
│ 1. 변경 요청 기준선 확정 │
│ 요구사항·결함·장애·보안 개선 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. 변경 대상 식별 │
│ 화면·ServiceId·프로그램·SQL·DB·설정 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. 직접 영향 추적 │
│ 호출자·피호출자·입출력·데이터 변경 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. 간접 영향 추적 │
│ 공통 모듈·공유 테이블·Cache·Batch·권한·운영정책 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. 호환성·위험 판단 │
│ 기존 소비자·데이터·성능·보안·가용성 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. 수정 범위와 제외 범위 확정 │
│ 코드·설정·DB·문서·OM·배포 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. 테스트·배포·Rollback 설계 │
│ 회귀시험·배포순서·복구조건 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. 영향도 분석서 검토·승인 │
│ 업무·AA·DA·DBA·보안·운영 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. 개발·자동검증·배포 │
│ Commit·PR·Artifact·운영 증적 │
└────────────────────────────────────────────────────────────┘

# 변경 영향도 분석의 핵심 질문

| 관점 | 핵심 질문 | 주요 증적 |
| --- | --- | --- |
| 요구사항 | 무엇이 왜 변경되는가 | 요구사항·결함·장애 ID |
| 사용자 | 어떤 행동과 결과가 달라지는가 | 화면·이벤트·사용 시나리오 |
| 계약 | 입력·출력·오류 의미가 달라지는가 | DTO·전문·API 계약 |
| 프로그램 | 어느 계층이 변경되는가 | Call Hierarchy·소스 |
| 데이터 | 어떤 데이터가 조회·변경되는가 | SQL·Table·Lineage |
| 연계 | 누가 이 기능을 소비하는가 | Client·ServiceId·TraceId |
| 운영 | 통제·Timeout·권한이 달라지는가 | OM·설정·로그 |
| 배포 | 어떤 순서와 단위로 배포하는가 | Artifact·Runbook |
| 복구 | 실패하면 무엇을 되돌리는가 | Rollback Plan |
| 검증 | 안전하다는 것을 어떻게 증명하는가 | 테스트·로그·DB 결과 |

# 변경 유형별 출발점

| 변경 유형 | 출발점 | 우선 확인 |
| --- | --- | --- |
| 신규 기능 | 요구사항·화면 | 신규 ServiceId·데이터·권한 |
| 결함 수정 | 오류 거래·GUID | 원인 계층·회귀 범위 |
| 성능 개선 | Slow SQL·Metric | SQL·Index·Thread·Pool |
| DB 변경 | Table·Column | Mapper·ServiceId·화면 |
| 공통 모듈 변경 | tcf-core, tcf-web | 전체 업무 WAR |
| 보안 개선 | JWT·권한·취약점 | 인증·호환성·접근 경로 |
| 외부 연계 변경 | Contract·Endpoint | 소비자·Timeout·재시도 |
| 설정 변경 | Property·OM | 환경별 적용값·재기동 |
| 배포구조 변경 | WAR·Tomcat·VM | 자원·장애영역·라우팅 |
| 폐기 | ServiceId·Table·화면 | 실제 사용량·대체 기능 |

# 변경 위험등급

| 등급 | 범위 | 대표 사례 | 검토 수준 |
| --- | --- | --- | --- |
| L1 | 단일 클래스 내부 | 메시지 오탈자·내부 리팩터링 | 개발팀 리뷰 |
| L2 | 단일 거래 | Rule·Service·Mapper 변경 | 업무팀·리뷰어 |
| L3 | 단일 업무 WAR | 공통 DTO·설정·테이블 변경 | AA·DBA·QA |
| L4 | 여러 WAR·시스템 | 공통 모듈·계약·ServiceId 변경 | Architecture Review |
| L5 | 플랫폼·데이터 전환 | 인증·공통 Header·DB 이행 | 공식 Gate·전환승인 |

위험등급은 변경 파일 수가 아니라 다음 기준으로 판단한다.

text id="9fey0u" 영향 사용자 수 계약 비호환성 데이터 변경 가능성 업무 중단 가능성 보안·감사 영향 복구 난이도 다중 시스템 영향

# 12.1 직접 영향과 간접 영향

## 12.1.1 직접 영향이란 무엇인가

직접 영향은 변경 대상과 명시적인 호출·참조·데이터 관계로 연결된 영역이다.

예:

text id="d61gm0" CustomerSummaryResponse 필드 추가 ↓ Facade 결과 조립 ↓ Service 결과 ↓ Mapper Result ↓ 화면 표시

또는:

text id="9akdum" SV\_CUSTOMER 컬럼 변경 ↓ Mapper SQL ↓ Result DTO ↓ Service ↓ ServiceId

직접 영향은 다음 도구로 비교적 쉽게 찾을 수 있다.

text id="oxlfzz" Find Usages Call Hierarchy 문자열 검색 Mapper Namespace Dependency Graph 화면–ServiceId 매트릭스 DB 객체 사용 목록

## 12.1.2 직접 영향 범위

| 변경 대상 | 대표 직접 영향 |
| --- | --- |
| 화면 필드 | UI Validation·Request DTO |
| ServiceId | UI·Handler·Catalog·로그 |
| Handler | Facade 호출·등록 ServiceId |
| Facade | Transaction·Service 호출 |
| Service | Rule·DAO·Client·응답 |
| Rule | 업무 결과·오류코드 |
| DAO | Mapper·예외변환 |
| Mapper | SQL·Parameter·Result |
| DB 컬럼 | SQL·DTO·Index·데이터 |
| Property | Bean·실행 동작 |
| Error Code | 화면 메시지·운영 통계 |
| 권한코드 | 메뉴·기능·감사 |
| 공통 DTO | 전체 소비 모듈 |

## 12.1.3 간접 영향이란 무엇인가

간접 영향은 소스상 직접 호출관계가 없지만 공유 자원, 정책, 데이터 또는 런타임 구조 때문에 함께 영향을 받는 영역이다.

예:

\`\`\`text id=“whvjot” SV\_CUSTOMER 컬럼 변경 ↓ SV Mapper 직접 영향

동시에 ↓ 해당 Table을 참조하는 View Batch 적재 BI Report Cache Key 파일 추출 외부 연계 간접 영향



또 다른 예:

\`\`\`text id="h3bvce"
tcf-core의 StandardHeader 변경
↓
직접 참조하는 tcf-web

동시에
↓
모든 업무 WAR의 JSON 역직렬화
Gateway Header 전달
거래로그
감사로그
외부 Client
간접 영향

## 12.1.4 직접·간접 영향 비교

| 구분 | 직접 영향 | 간접 영향 |
| --- | --- | --- |
| 관계 | 호출·참조가 명확 | 공유·정책·런타임 관계 |
| 탐색 | Find Usages로 발견 가능 | 별도 Catalog·운영지식 필요 |
| 예측 | 상대적으로 쉬움 | 누락 가능성이 높음 |
| 대표 | Method 호출자 | Cache·Batch·공통설정 |
| 위험 | 기능 오류 | 운영·성능·데이터 오류 |
| 검증 | 단위·통합테스트 | 회귀·성능·운영시험 |

## 12.1.5 영향의 전파 단계

변경 영향은 한 단계에서 끝나지 않는다.

text id="r22wem" 변경점 ↓ 1차 직접 영향 ↓ 2차 호출·데이터 영향 ↓ 3차 운영·배포 영향 ↓ 4차 사용자·조직 영향

예:

text id="c0i7gg" Mapper SQL 변경 ↓ Service 결과 변경 ↓ ServiceId 응답 변경 ↓ 화면 표시 변경 ↓ 사용자 업무 절차 변경 ↓ 운영 매뉴얼·교육 변경

## 12.1.6 정방향 분석

변경 대상에서 피호출 대상으로 내려간다.

text id="bm2psv" 화면 → 요청 DTO → Handler → Facade → Service → Rule·DAO → Mapper → Table

정방향 분석의 질문:

\`\`\`text id=“1f78mx” 이 변경으로 무엇이 실행되는가?

어떤 데이터를 읽고 변경하는가?

실패하면 어디에서 오류가 변환되는가?


\---

\## 12.1.7 역방향 분석

변경 대상을 사용하는 상위 호출자를 찾는다.

\`\`\`text id="3zf1fc"
Table
→ Mapper
→ DAO
→ Service
→ Facade
→ Handler
→ ServiceId
→ 화면·Client·Batch

역방향 분석의 질문:

\`\`\`text id=“78bcq1” 누가 이 변경 대상을 사용하는가?

어떤 화면과 외부 소비자가 영향을 받는가?

어떤 회귀테스트가 필요한가?


\---

\## 12.1.8 양방향 분석 원칙

변경 영향은 정방향과 역방향을 모두 수행해야 한다.

\`\`\`text id="d27136"
정방향만 수행
→ 하위 데이터 영향은 알 수 있음
→ 다른 호출자 누락 가능

역방향만 수행
→ 호출자는 알 수 있음
→ 내부 데이터·정책 영향 누락 가능

완료 기준:

text id="z233uq" 정방향 호출 경로 + 역방향 소비자 경로 + 런타임 실행 증거

## 12.1.9 요구사항 변경의 직접 영향

예:

text id="dbko8r" 고객요약 화면에 최종 거래일 추가

직접 영향:

text id="hdp28l" 화면 표시영역 Response DTO Service 결과 조립 Mapper Result SELECT 컬럼

간접 영향:

text id="g3ucpe" 엑셀 다운로드 화면 인쇄 응답 계약 소비자 개인정보 분류 조회 성능 Index 테스트 데이터

## 12.1.10 Request 필드 변경

기존:

json id="0w8okc" { "customerNo": "C000001234" }

변경:

json id="b6k7uo" { "customerNo": "C000001234", "baseDate": "2026-07-18" }

확인:

| 영역 | 영향 |
| --- | --- |
| 화면 | 기준일 입력 |
| UI Validation | 미래일자 방지 |
| Request DTO | 필드 추가 |
| Rule | 기준일 검증 |
| Mapper | 조건 추가 |
| Index | 날짜조건 성능 |
| 기존 소비자 | 필드 미전송 시 기본값 |
| 계약 | 필수·선택 여부 |
| 테스트 | 과거·당일·미래일 |

선택 필드라면 하위 호환 가능성이 높지만, 필수 필드로 추가하면 기존 소비자가 모두 실패할 수 있다.

## 12.1.11 Response 필드 변경

### 선택 필드 추가

\`\`\`text id=“ckjp37” 대체로 하위 호환 가능

그러나 고정 Schema 검증 엄격한 역직렬화 화면 Grid 순서 파일 Layout

은 별도 확인 필요


\### 필드명 변경

\`\`\`text id="vevgge"
customerGrade
→ customerLevel

비호환 변경에 해당할 수 있다.

권장:

text id="z2oo3n" 기존 필드 유지 + 신규 필드 추가 + 소비자 전환 + 호출량 확인 + 기존 필드 폐기

## 12.1.12 ServiceId 변경

기존:

text id="j0l91o" SV.Customer.selectSummary

신규:

text id="hf0h33" SV.Customer.selectProfile

직접 영향:

text id="6ie81g" UI Handler 통합테스트 Client

간접 영향:

text id="elrfda" OM Catalog 거래통제 Timeout 권한 감사 Metric 로그 조회조건 운영 Dashboard 외부 소비자

운영 중인 ServiceId를 단순 문자열 Rename으로 변경하지 않는다.

text id="hba46m" 신규 ServiceId 등록 → 기존 ServiceId 병행 → 소비자 순차 전환 → 기존 호출량 0 확인 → 기존 ServiceId 폐기

## 12.1.13 Mapper SQL 변경

예:

sql id="8wzi4j" WHERE CUSTOMER\_NO = #{customerNo}

변경:

sql id="3d84lo" WHERE CUSTOMER\_NO = #{customerNo} AND BASE\_DATE = #{baseDate}

직접 영향:

text id="3t1tkw" Criteria DTO Mapper Test 조회 결과

간접 영향:

text id="od5jt8" Index 실행계획 응답시간 Timeout Cache Key 조회 결과 건수 업무 오류 의미

## 12.1.14 공통 모듈 변경

공통 모듈 변경은 파일 한 개라도 위험등급이 높다.

예:

text id="9bdazx" tcf-core tcf-web tcf-util tcf-eai

확인:

text id="p1z94k" 어떤 업무 WAR가 해당 버전을 사용하는가? Binary 호환성은 유지되는가? Spring Bean 등록이 바뀌는가? 모든 WAR 재빌드가 필요한가? Tomcat 재기동이 필요한가? Rollback Artifact가 준비됐는가?

공통 StandardRequest나 StandardHeader 변경은 모든 업무 거래의 직렬화와 로그에 영향을 줄 수 있다.

## 12.1.15 공통 설정 변경

예:

yaml id="060ifs" mybatis: configuration: default-statement-timeout: 3

변경:

yaml id="oey3fa" default-statement-timeout: 5

직접 영향:

text id="xfrjyy" 기본값을 사용하는 Mapper

간접 영향:

text id="t0i54d" DB Connection 점유시간 Hikari Pool 사용량 Tomcat Busy Thread 전체 Timeout 장애 전파시간

Timeout을 늘리는 것은 성능문제를 해결하는 것이 아니라 대기시간과 자원 점유를 늘릴 수 있다.

## 12.1.16 공유 Tomcat의 간접 영향

하나의 Tomcat에 여러 WAR가 배포되어 있다면 특정 WAR의 변경이 다른 WAR에 간접 영향을 줄 수 있다.

text id="4csxy4" SV SQL 지연 ↓ SV DB Connection 장기 점유 ↓ Tomcat Thread 점유 증가 ↓ 공유 JVM Heap·GC 부담 ↓ IC·PC·MG 응답지연

WAR는 Spring Context 기준으로 분리되지만 JVM·Heap·GC·Connector Thread는 공유될 수 있으므로 성능 영향은 Tomcat 인스턴스 단위로도 검토한다.

## 12.1.17 영향이 없는 범위도 기록한다

영향 분석서에는 수정 대상뿐 아니라 확인 후 제외한 범위도 남긴다.

예:

text id="elq374" DB 구조 변경 없음 외부 계약 변경 없음 권한 변경 없음 공통 모듈 변경 없음 다른 업무 WAR 영향 없음

이는 검토하지 않았다는 의미가 아니라 확인 후 영향이 없다고 판정했다는 증적이다.

# 12.2 데이터·연계·배포·운영 영향

## 12.2.1 데이터 영향 분석

데이터 영향은 다음 다섯 영역으로 나눈다.

text id="bv5omk" 구조 데이터값 접근경로 정합성 생명주기

| 영역 | 확인 |
| --- | --- |
| 구조 | Table·Column·Type·Constraint |
| 데이터값 | 기존 데이터 변환·기본값 |
| 접근경로 | SQL·View·Procedure·Index |
| 정합성 | PK·FK·중복·상태 |
| 생명주기 | 생성·변경·삭제·보존·파기 |

## 12.2.2 컬럼 추가

예:

sql id="ff95mt" ALTER TABLE SV\_CUSTOMER ADD LAST\_TRANSACTION\_DATE DATE;

확인:

text id="ktbf4n" Nullable 여부 Default 기존 Row 값 Backfill Mapper Result Response DTO Index 통계정보 DB 배포시간 Rollback

대용량 테이블에 Default와 NOT NULL을 동시에 적용하면 Lock과 장시간 DDL이 발생할 수 있다.

## 12.2.3 컬럼 타입 변경

예:

text id="ixongj" CUSTOMER\_NO VARCHAR2(10) → VARCHAR2(12)

영향:

text id="p54uyx" Java Validation Request·Response DTO Mapper Parameter PK·Index Join 컬럼 파일 Layout 외부 전문 Cache Key 마스킹 테스트 데이터

Join 대상 컬럼의 길이와 타입도 함께 변경해야 할 수 있다.

## 12.2.4 컬럼명 변경

기존:

text id="kcvvq0" CUSTOMER\_GRADE

신규:

text id="n5vpog" CUSTOMER\_LEVEL\_CODE

안전한 전환 예:

text id="ocme4l" 1. 신규 컬럼 추가 2. 기존 데이터 이행 3. 애플리케이션 양쪽 호환 4. 신규 컬럼 전환 5. 기존 컬럼 사용량 확인 6. 기존 컬럼 폐기

운영 중인 컬럼을 즉시 Rename하면 구버전 WAR와 Batch가 실패할 수 있다.

## 12.2.5 Expand–Migrate–Contract

비호환 데이터 변경은 다음 단계로 수행하는 것이 안전하다.

text id="s34aoz" Expand 신규 구조를 추가하되 기존 구조 유지 ↓ Migrate 데이터와 소비자를 순차 전환 ↓ Contract 기존 구조 사용량 0 확인 후 제거

예:

text id="khud5e" 신규 Column 추가 → Dual Read·Write → 데이터 Backfill → 신규 WAR 배포 → 소비자 전환 → 구 Column 제거

## 12.2.6 데이터 이행

데이터 이행 계획에 포함할 항목:

| 항목 | 내용 |
| --- | --- |
| 대상 건수 | 전체·증분 건수 |
| 변환규칙 | 기존→신규 값 |
| 수행시간 | 예상 소요시간 |
| Lock | Online 영향 |
| 검증 | 건수·합계·Checksum |
| 실패처리 | 재실행·부분 실패 |
| 백업 | 복구 기준 |
| Cutover | 전환 시점 |
| 책임 | DBA·업무팀 |
| 감사 | 수행자·승인자·결과 |

## 12.2.7 Cache 영향

text id="pdf34a" DB 변경 ≠ 화면 즉시 변경

Cache가 있다면 다음을 확인한다.

text id="s098mq" Cache Key Value Schema TTL 무효화 조건 분산 Cache 동기화 구버전 Value 호환성

응답 DTO 구조가 바뀌었는데 Cache에 구버전 객체가 남아 있으면 역직렬화 오류가 발생할 수 있다.

## 12.2.8 Batch 영향

온라인 거래가 사용하는 테이블은 Batch가 생성하거나 정리할 수 있다.

확인:

text id="02okqp" Job Scheduler Reader Writer 파일 적재 마감시간 재처리 보관·삭제

DB 변경 시 Online 배포뿐 아니라 Batch 배포와 실행순서도 함께 설계한다.

## 12.2.9 BI·보고서 영향

확인 대상:

text id="f41ksa" BI Portal DataEye Report Excel Ad-hoc Query Mart 통계

애플리케이션 저장소에서 참조가 없더라도 보고서가 테이블이나 View를 직접 사용할 수 있다.

## 12.2.10 연계 영향 분석

연계 영향은 제공자와 소비자 양쪽에서 분석한다.

text id="0n4fbk" 제공 시스템 ↓ 공개 Contract ↓ Client·Adapter ↓ 소비 시스템

확인:

| 영역 | 주요 질문 |
| --- | --- |
| 요청 | 필드·형식·필수 여부 변경인가 |
| 응답 | 필드명·타입·오류 의미 변경인가 |
| 인증 | Token·인증서가 바뀌는가 |
| Endpoint | URL·Route가 바뀌는가 |
| Timeout | 호출 예산이 바뀌는가 |
| Retry | 중복 위험이 있는가 |
| 멱등성 | 재요청 결과가 동일한가 |
| 버전 | 기존 소비자가 유지되는가 |
| 추적 | TraceId가 전달되는가 |
| 장애 | Fallback·복구책임은 무엇인가 |

## 12.2.11 동일 WAR 내 도메인 연계

동일 WAR라도 다른 도메인의 내부 Mapper를 직접 호출하지 않는다.

\`\`\`text id=“gq1cq1” CustomerService → ProductMapper

금지 검토 대상



권장:

\`\`\`text id="ehbw2s"
CustomerFacade
→ Product 공개 Service·Query 계약

도메인 경계를 넘는 변경은 제공 도메인과 소비 도메인의 테스트를 함께 수행한다.

## 12.2.12 다른 WAR 간 연계

text id="5ul772" IC Service ↓ SvCustomerClient ↓ SV.Customer.selectSummary ↓ SV Handler

SV 응답 변경 시 직접 SV 화면뿐 아니라 IC Client도 영향을 받을 수 있다.

따라서 ServiceId 검색 결과를 다음과 같이 분류한다.

text id="w5voxc" Handler 등록 UI 호출 다른 WAR Client Batch 호출 테스트 운영설정

## 12.2.13 외부 연계 호환성

비호환 변경:

text id="ak3y8e" 필수 필드 추가 필드 삭제 타입 변경 의미 변경 오류코드 의미 변경

하위 호환 가능성이 높은 변경:

text id="770d17" 선택 필드 추가 새 오류코드 추가 기존 의미를 유지하는 확장

그러나 엄격한 Schema 검증 소비자는 선택 필드 추가도 실패할 수 있으므로 계약테스트로 확인한다.

## 12.2.14 Timeout 영향

전체 제한시간을 다음처럼 분해한다.

text id="zdq0i4" TCF 전체 Timeout > DB Query Timeout 외부 Client Timeout Connection 획득 Timeout

변경 시 확인:

text id="6qgxq5" 응답시간 목표 Thread 점유 DB Pool 점유 Retry 횟수 Fallback 사용자 재요청

Timeout을 늘릴 때는 성능시험과 장애시험을 다시 수행한다.

## 12.2.15 재시도와 멱등성

조회 거래:

text id="xczuwp" 일시 장애에 제한적 재시도 가능

등록·변경 거래:

text id="ny2fgm" 재시도 전 멱등성 키 업무 유일키 처리상태 조회 중복 결과 계약 필수

중복 처리 방지를 화면 버튼 비활성화에만 의존하지 않는다.

## 12.2.16 배포 단위 영향

| 변경 위치 | 일반 배포 단위 |
| --- | --- |
| UI 소스 | UI Artifact |
| 업무 Java | 해당 업무 WAR |
| Mapper XML | 해당 업무 WAR |
| 공통 JAR | 모든 소비 WAR 재빌드 검토 |
| TCF Core | 전체 업무 WAR 영향 |
| DB DDL | DB Script |
| OM Catalog | 운영 기준정보 |
| Apache·Gateway | 라우팅 설정 |
| Tomcat | 인스턴스 설정·재기동 |
| Batch | Batch Artifact |

## 12.2.17 배포순서

호환 변경의 권장 순서 예:

text id="rjsb0w" 1. DB 확장 DDL 2. OM·환경설정 등록 3. 제공자 WAR 배포 4. 소비자 WAR 배포 5. UI 배포 6. Smoke Test 7. 트래픽 확대

비호환 변경은 단일 시점 동시배포보다 병행 운용과 단계적 전환을 우선 검토한다.

## 12.2.18 배포 전후 버전 혼재

Rolling 배포 중에는 구버전과 신버전이 동시에 실행될 수 있다.

text id="dtxjhg" 구버전 WAR + 신버전 WAR + 신규 DB 구조

따라서 다음 조합이 모두 동작해야 한다.

| App | DB·Contract | 검증 |
| --- | --- | --- |
| 구버전 | 확장된 신규 DB | 정상 |
| 신버전 | 확장된 신규 DB | 정상 |
| 구 소비자 | 신 제공자 | 정상 |
| 신 소비자 | 신 제공자 | 정상 |

## 12.2.19 하나의 Tomcat에 여러 WAR가 있는 경우

특정 업무 WAR만 변경해도 다음을 확인한다.

text id="1dqr6f" Tomcat 전체 재기동 여부 공유 Connector 공유 JVM Option 공유 Heap·GC ClassLoader 영향 WAR별 Hikari Pool 로그 경로 Health Check

전체 Tomcat 재기동이 필요하면 다른 WAR의 업무 중단도 변경 영향에 포함한다.

## 12.2.20 환경설정 영향

설정 변경은 소스 변경보다 추적이 어려울 수 있다.

확인:

text id="kmg7zl" Property Key 기본값 환경별 값 환경변수 JVM -D Secret 적용 Bean 재기동 여부 운영 승인

설정 우선순위 때문에 YML 수정이 실제 운영값에 반영되지 않을 수 있다.

## 12.2.21 OM 운영정보 영향

신규·변경 거래는 다음 운영정보를 확인한다.

| 영역 | 확인 |
| --- | --- |
| Service Catalog | ServiceId·업무코드·사용 여부 |
| 거래통제 | 허용·중지·시간대 |
| Timeout | 전체 제한시간 |
| 권한 | 메뉴·기능·데이터 권한 |
| 감사 | 감사대상·조회사유 |
| 오류코드 | 사용자 메시지·담당 조직 |
| 성능 | 목표 응답시간 |
| 배포 | 적용 버전 |
| 폐기 | 종료일·대체 거래 |

## 12.2.22 로그·모니터링 영향

변경 후에도 다음 정보로 거래를 추적할 수 있어야 한다.

text id="7hgc6p" GUID TraceId ServiceId 거래코드 App Version Commit ID Mapper Statement ID 오류코드 처리시간

필드명이나 ServiceId가 변경되면 Dashboard와 로그 검색조건도 수정해야 할 수 있다.

## 12.2.23 보안 영향

| 영역 | 확인 |
| --- | --- |
| 인증 | JWT Claim·Session 영향 |
| 권한 | 기능·데이터권한 변경 |
| 개인정보 | 신규 개인정보 필드 |
| 마스킹 | 화면·로그·파일 |
| 암호화 | 저장·전송 |
| 감사 | 조회·변경·다운로드 |
| Secret | 설정 저장 방식 |
| 직접 접근 | Gateway 우회 가능성 |
| 취약점 | 신규 Library·Endpoint |

## 12.2.24 개인정보 영향

신규 응답 필드가 개인정보라면 다음을 확인한다.

text id="2wc4j7" 업무 필요성 최소 수집 조회권한 마스킹 감사로그 화면 노출 다운로드 보존기간 파기

DB에 이미 존재하는 정보라도 새로운 화면이나 API로 노출되면 개인정보 영향이 새로 발생한다.

## 12.2.25 성능·용량 영향

변경 전후 비교:

text id="c0i923" SQL 실행시간 반환 Row 수 응답 Payload CPU Heap GC Tomcat Busy Thread DB Pool 외부 호출시간

조회 컬럼 하나의 추가도 대용량 결과에서 Network와 Heap 사용량을 증가시킬 수 있다.

## 12.2.26 가용성·DR 영향

확인:

text id="f8zc6u" 센터별 설정 동일성 DB 복제 대상 신규 테이블 DR 반영 배포순서 Failover RTO·RPO Rollback Artifact

주센터에서만 신규 DDL이 적용되면 DR 전환 시 장애가 발생한다.

## 12.2.27 Rollback 구분

Rollback은 하나의 행위가 아니다.

| Rollback 유형 | 대상 |
| --- | --- |
| 코드 Rollback | 이전 WAR·JAR |
| 설정 Rollback | 이전 Property·OM |
| 라우팅 Rollback | Gateway·Apache |
| DB 구조 Rollback | DDL 복구 |
| 데이터 Rollback | 변경 데이터 원복 |
| 기능 Rollback | Feature Toggle |
| 트래픽 Rollback | 이전 인스턴스 전환 |

코드만 되돌려도 변경된 데이터는 그대로 남을 수 있다.

## 12.2.28 데이터 Rollback의 어려움

text id="2qxhdi" 신규 코드 배포 → 데이터 변경 → 코드 Rollback

이때 신규 구조로 변경된 데이터가 구버전 코드와 호환되지 않을 수 있다.

따라서 변경 거래는 다음을 사전에 정의한다.

text id="ux1gwl" 어떤 데이터가 변경되는가? 이전 값을 저장하는가? 역변환 가능한가? 부분 처리 건을 어떻게 식별하는가? Rollback 후 재처리는 가능한가?

# 정상 변경 처리 흐름

text id="0tfhkj" 변경 요청 접수 ↓ 기준선·완료조건 확정 ↓ 정방향·역방향 영향 분석 ↓ 직접·간접 영향 분류 ↓ 호환성·위험등급 결정 ↓ 수정·제외 범위 승인 ↓ 테스트·배포·Rollback 설계 ↓ 개발·자동검증 ↓ 배포 전 Gate ↓ 배포·Smoke Test ↓ 운영로그·데이터 검증 ↓ 변경 종료

# 영향 누락 발견 흐름

text id="kwth42" 개발·테스트 중 추가 영향 발견 ↓ 개발 임의 확장 금지 ↓ 영향도 분석서 갱신 ↓ 위험등급 재평가 ↓ 일정·테스트·배포계획 수정 ↓ 재승인

# 비호환 계약 발견 흐름

text id="ubgnh8" 소비자 계약 실패 ↓ 배포 중지 ↓ 기존 Contract 유지 ↓ 신규 Version 또는 병행 필드 설계 ↓ 소비자 전환계획 수립 ↓ 계약테스트 재수행

# DB 이행 실패 흐름

text id="js6p1h" DDL·Data Migration 실패 ↓ 업무 트래픽 개방 금지 ↓ 실패 지점·변경 건수 확인 ↓ 재실행 가능성 판단 ├─ 재실행 └─ 백업 복구 ↓ 데이터 정합성 검증 ↓ 전환 재승인

# 배포 실패 흐름

text id="bus84v" 신규 WAR 기동 실패 ↓ Health·Handler·Mapper 확인 ↓ 트래픽 미전환 또는 차단 ↓ 이전 Artifact 복구 ↓ OM·설정·DB 호환성 확인 ↓ 영향 거래 Smoke Test

# 운영 이상 발견 흐름

text id="qlr2r9" 배포 후 오류·지연 증가 ↓ GUID·ServiceId·App Version 확인 ↓ 신규 변경과 상관관계 분석 ↓ Feature 비활성 또는 트래픽 Rollback ↓ 데이터 정합성 확인 ↓ 원인·영향 분석 ↓ 재배포 또는 개선

# 12.3 변경 전 확인 체크리스트

## 12.3.1 변경 요청 기준선

| 확인 항목 | 완료 |
| --- | --- |
| 요구사항·결함·장애 ID가 있다. | □ |
| 변경 이유가 명확하다. | □ |
| 현재 동작과 변경 후 동작이 정의됐다. | □ |
| 성공 기준이 측정 가능하다. | □ |
| 제외 범위가 정의됐다. | □ |
| 업무 담당자가 기준선을 승인했다. | □ |

## 12.3.2 화면·사용자 영향

| 확인 항목 | 완료 |
| --- | --- |
| 영향 화면 ID를 확인했다. | □ |
| 영향 이벤트 ID를 확인했다. | □ |
| 초기화·조회·저장 이벤트를 구분했다. | □ |
| 입력 Validation 변경을 확인했다. | □ |
| 화면 표시·Grid·Popup 영향을 확인했다. | □ |
| 오류 메시지와 사용자 행동을 확인했다. | □ |
| 메뉴·기능권한 영향을 확인했다. | □ |
| 사용자 매뉴얼·교육 영향을 확인했다. | □ |

## 12.3.3 거래·ServiceId 영향

| 확인 항목 | 완료 |
| --- | --- |
| 영향 ServiceId를 확인했다. | □ |
| 거래코드를 확인했다. | □ |
| Handler 등록 변경을 확인했다. | □ |
| 다른 화면·Client 호출자를 확인했다. | □ |
| OM Catalog를 확인했다. | □ |
| 거래통제를 확인했다. | □ |
| Timeout을 확인했다. | □ |
| 권한·감사를 확인했다. | □ |
| 폐기 거래와 호환성을 확인했다. | □ |

## 12.3.4 프로그램 영향

| 확인 항목 | 완료 |
| --- | --- |
| Handler를 확인했다. | □ |
| Facade Transaction을 확인했다. | □ |
| Service 업무 흐름을 확인했다. | □ |
| Rule 변경을 확인했다. | □ |
| DAO·Mapper를 확인했다. | □ |
| Client·Adapter를 확인했다. | □ |
| 공통 모듈 의존성을 확인했다. | □ |
| 계층 위반 여부를 확인했다. | □ |
| 예외·오류코드 변경을 확인했다. | □ |

## 12.3.5 계약·DTO 영향

| 확인 항목 | 완료 |
| --- | --- |
| Request 필드 변경을 확인했다. | □ |
| Response 필드 변경을 확인했다. | □ |
| 필수·선택 여부를 확인했다. | □ |
| 타입·길이·형식을 확인했다. | □ |
| 기본값을 정의했다. | □ |
| 구버전 소비자 호환성을 확인했다. | □ |
| 파일·메시지 계약을 확인했다. | □ |
| 계약테스트 범위를 정했다. | □ |

## 12.3.6 SQL·DB 영향

| 확인 항목 | 완료 |
| --- | --- |
| Mapper Statement를 확인했다. | □ |
| 조회·등록·변경·삭제를 구분했다. | □ |
| Table·Column을 확인했다. | □ |
| View·Synonym·Procedure를 확인했다. | □ |
| Trigger·History Table을 확인했다. | □ |
| Index·실행계획을 확인했다. | □ |
| Lock·영향 행 수를 확인했다. | □ |
| 데이터 소유권을 확인했다. | □ |
| 데이터 이행·검증을 설계했다. | □ |
| DB Rollback 방안을 정의했다. | □ |

## 12.3.7 Cache·Batch·BI 영향

| 확인 항목 | 완료 |
| --- | --- |
| Cache Key·Value 변경을 확인했다. | □ |
| Cache 무효화 방안을 정의했다. | □ |
| Batch Job 사용 여부를 확인했다. | □ |
| Scheduler 실행순서를 확인했다. | □ |
| 파일 Layout을 확인했다. | □ |
| BI·보고서 사용을 확인했다. | □ |
| 데이터 기준시각을 확인했다. | □ |
| 재처리 방안을 정의했다. | □ |

## 12.3.8 외부 연계 영향

| 확인 항목 | 완료 |
| --- | --- |
| 제공자·소비자를 식별했다. | □ |
| Endpoint·Route를 확인했다. | □ |
| 요청·응답 계약을 확인했다. | □ |
| 인증방식을 확인했다. | □ |
| Timeout 예산을 확인했다. | □ |
| Retry 정책을 확인했다. | □ |
| 멱등성을 확인했다. | □ |
| TraceId 전달을 확인했다. | □ |
| 장애·Fallback 책임을 확인했다. | □ |

## 12.3.9 보안·개인정보·감사

| 확인 항목 | 완료 |
| --- | --- |
| 신규 인증·권한 영향이 없다. | □ |
| 개인정보 추가·변경을 확인했다. | □ |
| 최소 수집 원칙을 확인했다. | □ |
| 마스킹을 확인했다. | □ |
| 로그 노출을 확인했다. | □ |
| 다운로드·파일 노출을 확인했다. | □ |
| 감사로그 대상을 확인했다. | □ |
| Secret·Key 관리 영향을 확인했다. | □ |
| 보안 검토가 필요한지 판단했다. | □ |

## 12.3.10 성능·용량 영향

| 확인 항목 | 완료 |
| --- | --- |
| SQL 실행시간을 비교했다. | □ |
| 예상 Row 수를 확인했다. | □ |
| Payload 증가량을 확인했다. | □ |
| CPU·Heap 영향을 확인했다. | □ |
| Tomcat Thread 영향을 확인했다. | □ |
| DB Pool 영향을 확인했다. | □ |
| 전체 Timeout을 확인했다. | □ |
| 성능시험 필요 여부를 결정했다. | □ |

## 12.3.11 배포·운영 영향

| 확인 항목 | 완료 |
| --- | --- |
| 변경 Artifact를 확인했다. | □ |
| 공통 모듈 소비 WAR를 확인했다. | □ |
| 배포순서를 정의했다. | □ |
| 구·신버전 혼재를 검증했다. | □ |
| Tomcat 재기동 범위를 확인했다. | □ |
| 환경설정 변경을 확인했다. | □ |
| Health·Smoke Test를 정의했다. | □ |
| 로그·Dashboard 변경을 확인했다. | □ |
| 운영 Runbook을 갱신했다. | □ |
| Rollback 조건을 정의했다. | □ |

## 12.3.12 테스트 범위

| 확인 항목 | 완료 |
| --- | --- |
| 단위테스트를 정의했다. | □ |
| Mapper Test를 정의했다. | □ |
| 거래 통합테스트를 정의했다. | □ |
| 계약테스트를 정의했다. | □ |
| 오류·Timeout 테스트를 정의했다. | □ |
| 동시성·멱등성 테스트를 정의했다. | □ |
| 회귀테스트 범위를 정의했다. | □ |
| 성능·장애 테스트 필요성을 판단했다. | □ |
| 배포 후 Smoke Test를 정의했다. | □ |
| 데이터 검증 기준을 정의했다. | □ |

## 12.3.13 변경 착수 금지 조건

다음 중 하나라도 해당하면 개발 착수를 보류한다.

\`\`\`text id=“wd3xr6” 변경 목적이 불명확하다.

현재 동작이 확인되지 않았다.

영향 ServiceId를 찾지 못했다.

데이터 소유권이 불명확하다.

외부 소비자가 확인되지 않았다.

비호환 변경인데 전환계획이 없다.

데이터 이행·Rollback이 정의되지 않았다.

운영 등록정보 변경 책임자가 없다.

테스트 완료기준이 없다.


\---

\## 12.3.14 배포 No-Go 조건

\`\`\`text id="zzfcch"
Blocker·Critical 결함 미해결
DB 이행 검증 실패
Handler·Catalog 불일치
계약테스트 실패
Rollback Artifact 없음
데이터 복구방안 없음
운영자 Runbook 미확정
성능 목표 미달
보안 승인 미완료

아키텍처 결함이 발견되면 Risk·Gap 등록, 영향 범위 분석, 설계 수정과 관련 코드·설정·시험 재수행을 거쳐야 한다.

# 책임 경계와 RACI

| 활동 | 업무분석 | UI | 업무개발 | FW | DA·DBA | DevOps | 운영 | 아키텍트 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 변경 목적·범위 | R | C | C | I | C | I | C | A |
| 화면 영향 | C | R/A | C | I | I | I | C | C |
| ServiceId 영향 | C | C | R | C | I | I | C | A |
| 프로그램 영향 | I | I | R/A | C | C | I | C | C |
| DB 영향 | C | I | R | I | R/A | I | C | C |
| 연계 영향 | R | C | R | C | C | I | C | A |
| 보안 영향 | C | C | C | C | C | I | C | A |
| 배포계획 | I | C | C | C | C | R/A | R | C |
| Rollback | I | C | R | C | R | R | C | A |
| 테스트 범위 | C | R | R | C | C | C | C | A |
| 최종 승인 | C | C | C | C | C | C | C | A |

# 12.4 영향도 분석서 작성 예

## 12.4.1 변경 개요

| 항목 | 내용 |
| --- | --- |
| 변경 ID | CHG-SV-2026-021 |
| 요구사항 ID | REQ-SV-CUS-021 |
| 변경명 | 고객요약 조회에 기준일과 최종거래일 추가 |
| 대상 화면 | SV-CUS-0001 |
| 대상 이벤트 | SV-CUS-0001-E01 |
| 대상 ServiceId | SV.Customer.selectSummary |
| 업무코드 | SV |
| 대상 WAR | sv-service |
| 변경유형 | 기능 확장·SQL 변경 |
| 위험등급 | L3 |
| 변경 목적 | 기준일 시점의 고객등급과 최종거래일 제공 |

## 12.4.2 현행과 목표

### 현행

json id="5ncrvu" { "header": { "serviceId": "SV.Customer.selectSummary" }, "body": { "customerNo": "C000001234" } }

응답:

json id="vnw2ec" { "customerNo": "C000001234", "customerName": "홍\*동", "customerGrade": "VIP" }

### 목표

요청:

json id="be7sck" { "header": { "serviceId": "SV.Customer.selectSummary" }, "body": { "customerNo": "C000001234", "baseDate": "2026-07-18" } }

응답:

json id="zwakm0" { "customerNo": "C000001234", "customerName": "홍\*동", "customerGrade": "VIP", "customerGradeName": "우수고객", "lastTransactionDate": "2026-07-17" }

## 12.4.3 핵심 변경 판단

\`\`\`text id=“lr8tcr” ServiceId는 유지한다.

baseDate는 선택 필드로 추가하고, 미입력 시 현재일을 적용한다.

응답 필드는 추가 방식으로 확장한다.

기존 필드는 삭제·변경하지 않는다.

기존 소비자와 하위 호환을 유지한다.


\---

\## 12.4.4 직접 영향

| 영역 | 변경 대상 | 변경 내용 |
|---|---|---|
| 화면 | 고객요약 조회조건 | 기준일 입력 추가 |
| UI | 요청 생성 | \`baseDate\` 추가 |
| Request DTO | Summary Request | \`baseDate\` 추가 |
| Rule | 기준일 검증 | 미래일자 차단 |
| Criteria | 조회조건 | 기준일 추가 |
| Mapper | 조회 SQL | 등급·최종거래일 조회 |
| Result DTO | Summary Row | 신규 필드 추가 |
| Response DTO | Summary Response | 신규 필드 추가 |
| 화면 | 결과 영역 | 등급명·최종거래일 표시 |
| Test | 거래 테스트 | 기준일별 결과 확인 |

\---

\## 12.4.5 간접 영향

| 영역 | 영향 | 조치 |
|---|---|---|
| Index | 기준일 조건 추가 | 실행계획 검토 |
| 응답크기 | 필드 2개 증가 | Payload 비교 |
| 권한 | 최종거래일 노출 | 기존 고객조회권한 적용 |
| 개인정보 | 거래일 정보 | 감사대상 검토 |
| 엑셀 | 동일 응답 재사용 | 출력 컬럼 반영 |
| 다른 WAR | IC Client 소비 가능 | 계약테스트 |
| Cache | 고객요약 Cache | Key에 기준일 포함 |
| Timeout | SQL Join 증가 | 성능시험 |
| 운영로그 | 요청 기준일 | 민감정보 아닌 범위 기록 |
| 문서 | 화면·거래·프로그램 | 산출물 갱신 |

\---

\## 12.4.6 변경 프로그램

\`\`\`text id="p8jxha"
UI
└─ CustomerSummaryPage·Script

sv-service
├─ SvCustomerHandler
├─ SvCustomerFacade
├─ SvCustomerService
├─ SvCustomerRule
├─ CustomerSummaryRequest
├─ CustomerSummaryCriteria
├─ CustomerSummaryRow
├─ CustomerSummaryResponse
├─ SvCustomerDao
├─ SvCustomerMapper
└─ SvCustomerMapper.xml

Handler의 ServiceId 등록은 변경하지 않고 기존 Facade Method의 계약을 확장한다.

## 12.4.7 데이터 영향

| DB 객체 | 사용 | 변경 |
| --- | --- | --- |
| SV\_CUSTOMER | 고객기본 | 변경 없음 |
| SV\_CUSTOMER\_GRADE | 기준일 등급 | 조회조건 추가 |
| SV\_TRANSACTION\_SUMMARY | 최종거래일 | 신규 Join |
| 관련 Index | 조회성능 | 검토·추가 가능 |
| DDL | 구조 | 원칙적으로 없음 |
| Data Migration | 데이터 | 없음 |

DDL이 없더라도 신규 Join과 날짜조건으로 실행계획이 변경될 수 있으므로 DBA 검토가 필요하다.

## 12.4.8 연계 영향

| 소비자 | 영향 | 검증 |
| --- | --- | --- |
| SV 화면 | 직접 영향 | UI 통합테스트 |
| IC Client | 응답 필드 추가 | 계약테스트 |
| Batch | 호출 없음 확인 | 검색·운영 확인 |
| 외부 시스템 | 없음 | 제외 근거 기록 |
| BI Report | 별도 직접조회 | 영향 없음 확인 |

## 12.4.9 운영 영향

| 항목 | 변경 |
| --- | --- |
| Service Catalog | 변경 없음 |
| ServiceId | 변경 없음 |
| 거래코드 | 변경 없음 |
| 거래통제 | 변경 없음 |
| Timeout | 우선 3초 유지 |
| 권한 | 기존 고객조회권한 사용 |
| 감사 | 최종거래일 조회 포함 검토 |
| Slow SQL | 신규 Join 감시 |
| Dashboard | Statement ID 유지 |
| Runbook | Slow SQL 확인항목 추가 |

## 12.4.10 호환성 판단

| 항목 | 판단 |
| --- | --- |
| Request | 선택 필드 추가로 하위 호환 |
| Response | 선택 필드 추가, 소비자 검증 필요 |
| ServiceId | 유지 |
| Error Code | 유지 |
| DB | 구조 변경 없음 |
| 구버전 UI | 신규 필드 무시 가능 |
| 구버전 Client | 엄격한 Schema 여부 확인 필요 |
| Rolling 배포 | 가능 후보 |

## 12.4.11 테스트 범위

| ID | 테스트 | 기대 결과 |
| --- | --- | --- |
| IMP-001 | 기준일 미입력 | 현재일 기준 조회 |
| IMP-002 | 과거 기준일 | 해당 시점 등급 조회 |
| IMP-003 | 미래 기준일 | 입력 오류 |
| IMP-004 | 고객 미존재 | 기존 업무 오류 유지 |
| IMP-005 | 등급 미존재 | 정책에 따른 기본 결과 |
| IMP-006 | 거래내역 없음 | 최종거래일 null |
| IMP-007 | 기존 Request | 기존과 동일 결과 |
| IMP-008 | IC 구버전 Client | 계약 정상 |
| IMP-009 | SQL 성능 | p95 목표 충족 |
| IMP-010 | Timeout | 3초 내 종료 |
| IMP-011 | 권한 없음 | 기존 권한 오류 |
| IMP-012 | Cache Hit | 기준일별 데이터 혼선 없음 |
| IMP-013 | Cache Miss | DB 조회 정상 |
| IMP-014 | 로그 | GUID·Statement ID 연결 |
| IMP-015 | 회귀 | 기존 고객요약 기능 정상 |

## 12.4.12 배포순서

text id="nw8hjg" 1. Mapper·업무 WAR Build 2. 계약·회귀·성능시험 3. 운영설정 변경 없음 확인 4. sv-service 배포 5. Health Check 6. 기존 Request Smoke Test 7. 신규 Request Smoke Test 8. UI 배포 9. IC Client 회귀 확인 10. Slow SQL 모니터링

## 12.4.13 Rollback 계획

text id="qyzbfe" 신규 SQL 성능 저하 또는 오류 발생 ↓ UI 신규 필드 비활성 ↓ 이전 sv-service WAR 재배포 ↓ 기존 ServiceId Smoke Test ↓ 신규 DDL·데이터 변경 없음

DB 구조와 데이터 변경이 없으므로 코드 Rollback 위험은 상대적으로 낮다.

## 12.4.14 잔여 위험

| 위험 | 수준 | 대응 |
| --- | --- | --- |
| 신규 Join 성능 | 중 | 실행계획·부하테스트 |
| 엄격한 Client Schema | 중 | 계약테스트 |
| Cache Key 누락 | 중 | 기준일 포함 검증 |
| 거래일 개인정보 해석 | 낮음·검토 | 보안·감사 확인 |
| 운영 데이터 편차 | 중 | 운영 유사 데이터 시험 |

## 12.4.15 수정 범위 최종안

### 변경 대상

text id="ythv81" UI 화면·요청 Request·Criteria·Result·Response DTO Rule Service DAO·Mapper XML 단위·Mapper·통합·계약·성능 Test 화면·거래·프로그램 설계서

### 변경하지 않는 대상

text id="491atl" ServiceId 거래코드 Handler 등록 목록 OM 거래통제 DB 물리 구조 공통 TCF 모듈 Gateway Route JWT 인증방식

## 12.4.16 승인

| 검토영역 | 담당 | 결과 |
| --- | --- | --- |
| 업무 | 업무 책임자 | 승인 |
| UI | UI 책임자 | 승인 |
| 애플리케이션 | AA | 승인 |
| 데이터·SQL | DBA·DA | 조건부 승인 |
| 보안·감사 | 보안 담당 | 확인 |
| 운영 | 운영 담당 | 승인 |
| 테스트 | QA | 시나리오 승인 |

# 변경 영향도 분석서 표준 양식

## 1\. 기본정보

| 항목 | 내용 |
| --- | --- |
| 변경 ID |  |
| 요구사항·결함 ID |  |
| 변경명 |  |
| 업무코드 |  |
| 위험등급 |  |
| 요청자 |  |
| 분석자 |  |
| 기준 Branch·Commit |  |
| 목표 배포일 |  |

## 2\. 변경 전·후

| 구분 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 사용자 동작 |  |  |
| 요청 계약 |  |  |
| 처리 규칙 |  |  |
| 응답 계약 |  |  |
| 데이터 |  |  |
| 운영정책 |  |  |

## 3\. 영향 대상

| 영역 | 영향 대상 | 직접·간접 | 변경 여부 | 근거 |
| --- | --- | --- | --- | --- |
| 화면 |  |  |  |  |
| ServiceId |  |  |  |  |
| 프로그램 |  |  |  |  |
| DB |  |  |  |  |
| 연계 |  |  |  |  |
| 설정 |  |  |  |  |
| 보안 |  |  |  |  |
| 운영 |  |  |  |  |
| 배포 |  |  |  |  |

## 4\. 테스트

| 테스트 유형 | 대상 | 완료기준 | 결과 |
| --- | --- | --- | --- |
| 단위 |  |  |  |
| Mapper |  |  |  |
| 통합 |  |  |  |
| 계약 |  |  |  |
| 회귀 |  |  |  |
| 성능 |  |  |  |
| 보안 |  |  |  |
| 장애 |  |  |  |
| Smoke |  |  |  |

## 5\. 배포·Rollback

| 항목 | 내용 |
| --- | --- |
| 배포 Artifact |  |
| DB Script |  |
| OM 변경 |  |
| 배포순서 |  |
| 서비스 중단 |  |
| Smoke Test |  |
| Rollback 조건 |  |
| 코드 Rollback |  |
| 데이터 Rollback |  |
| 책임자 |  |

# 자동검증 및 품질 Gate

## 1\. 추적성 완전성 Gate

text id="8opja8" 요구사항 → 화면 이벤트 → ServiceId → Handler → 프로그램 → SQL → DB 객체 → Test

연결이 끊긴 변경은 완료로 판정하지 않는다.

## 2\. ServiceId 정합성 Gate

text id="4e5llq" UI ↔ Handler ↔ OM Catalog ↔ 거래통제 ↔ Timeout ↔ Test

## 3\. 계약 호환성 Gate

검사:

text id="u5ihjd" 필수 필드 추가 필드 삭제 타입 변경 Enum 값 삭제 오류 의미 변경

비호환 변경은 Version 또는 병행 운영계획이 있어야 한다.

## 4\. DB 변경 Gate

text id="ib1a29" DDL 검토 데이터 이행 실행시간 Lock 정합성 검증 Rollback DR 반영

## 5\. 구조 Gate

금지:

text id="yq48hk" Handler → Mapper Rule → DAO 업무 WAR → 다른 업무 WAR Java Import 다른 도메인 Table 직접 UPDATE

## 6\. 설정 Gate

text id="b2x0y4" 신규 Property 정의 환경별 값 Secret 제외 기본값 재기동 여부 운영 적용 증적

## 7\. 보안 Gate

text id="i60hwz" 신규 Endpoint 인증 권한 개인정보 마스킹 감사 로그 노출 취약 Library

## 8\. 배포 Gate

text id="f2y94s" Artifact DB Script OM 기준정보 배포순서 Health Smoke Test Rollback 운영 승인

Pull Request에도 요구사항, 화면, ServiceId, 변경 프로그램, 데이터, 설정, 보안, 테스트, 배포와 Rollback 정보를 포함해야 변경 검토가 가능하다.

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| IA-001 | 변경 요구 기준선 | 전·후 동작 명확 |
| IA-002 | 직접 호출자 검색 | 전체 호출자 식별 |
| IA-003 | 역방향 소비자 검색 | 전체 화면·Client 식별 |
| IA-004 | 공통 모듈 변경 | 소비 WAR 식별 |
| IA-005 | ServiceId 변경 | UI·Handler·OM 식별 |
| IA-006 | Request 선택 필드 | 구버전 호출 정상 |
| IA-007 | Request 필수 필드 | 비호환 판정 |
| IA-008 | Response 필드 추가 | 소비자 계약 정상 |
| IA-009 | Response 필드 삭제 | Gate 실패 |
| IA-010 | DB 컬럼 추가 | 구·신버전 호환 |
| IA-011 | DB 컬럼 타입 변경 | 전체 소비자 확인 |
| IA-012 | Data Migration | 건수·정합성 일치 |
| IA-013 | View 간접 영향 | 소비 Mapper 식별 |
| IA-014 | Batch 간접 영향 | Job 재검증 |
| IA-015 | BI 간접 영향 | Report 정상 |
| IA-016 | Cache Schema 변경 | 구 Cache 처리 |
| IA-017 | 다른 WAR Client | 계약테스트 |
| IA-018 | 외부 API 변경 | 소비자 전환 |
| IA-019 | Timeout 증가 | Thread·Pool 영향 검증 |
| IA-020 | Retry 추가 | 중복 처리 없음 |
| IA-021 | 권한 변경 | 허용·거부 정상 |
| IA-022 | 개인정보 필드 추가 | 마스킹·감사 정상 |
| IA-023 | 단일 WAR 배포 | 타 WAR 영향 없음 |
| IA-024 | Tomcat 재기동 | 전체 WAR 복구 |
| IA-025 | Rolling 배포 | 구·신버전 혼재 정상 |
| IA-026 | 신규 DDL 후 구버전 | 호환 정상 |
| IA-027 | 코드 Rollback | 이전 기능 복구 |
| IA-028 | 데이터 Rollback | 정합성 복구 |
| IA-029 | 배포 실패 | 트래픽 미전환 |
| IA-030 | 운영 오류 증가 | App Version 추적 |
| IA-031 | GUID 추적 | 변경 거래 식별 |
| IA-032 | 미등록 Catalog | 배포 Gate 실패 |
| IA-033 | 영향도 누락 | 재분석·재승인 |
| IA-034 | 회귀테스트 | 기존 거래 정상 |
| IA-035 | 다른 개발자 재현 | 동일 영향범위 도출 |

# 따라 하는 실무 절차

## 1단계. 변경 요청을 한 문장으로 정의한다

text id="55yxyj" 누가 어떤 상황에서 무엇을 요청하고 어떤 결과를 받아야 하는가

## 2단계. 기준선을 기록한다

text id="35b6c9" 현재 동작 변경 후 동작 성공 기준 제외 범위 Branch·Commit

## 3단계. 시작 식별자를 확정한다

text id="k1cw5l" 화면 ID ServiceId 프로그램 SQL ID Table Property

## 4단계. 정방향으로 추적한다

text id="384qgg" 변경점 → 하위 처리 → 데이터·외부 시스템

## 5단계. 역방향으로 추적한다

text id="e87hj8" 변경점 → 호출자 → ServiceId → 화면·Client·Batch

## 6단계. 간접 영향을 조사한다

text id="vs0vvt" 공통 모듈 공유 DB Cache Batch BI 권한 OM 배포 Tomcat

## 7단계. 호환성과 위험을 판정한다

text id="1iwfv2" 하위 호환 여부 데이터 손상 가능성 업무 중단 가능성 Rollback 가능성

## 8단계. 수정·제외 범위를 확정한다

text id="0zttai" 변경 대상 변경하지 않는 대상 확인 후 영향 없음 추가 확인 필요

## 9단계. 테스트를 설계한다

text id="0l40ct" 정상 경계 실패 회귀 성능 보안 배포 Rollback

## 10단계. 배포순서와 복구방법을 작성한다

text id="hz0sfh" DB → 제공자 → 소비자 → UI → Smoke Test

## 11단계. 영향도 분석서를 검토받는다

text id="agw42u" 업무 애플리케이션 데이터 보안 운영 DevOps

## 12단계. Commit·PR·배포에 연결한다

text id="z6dsis" 변경 ID → Commit → Pull Request → Artifact → 배포 → 운영로그

# 완료 체크리스트

| 완료 기준 | 완료 |
| --- | --- |
| 변경 전·후 동작이 정의됐다. | □ |
| 직접 영향을 식별했다. | □ |
| 간접 영향을 식별했다. | □ |
| 정방향·역방향 추적을 수행했다. | □ |
| 화면·ServiceId·프로그램을 연결했다. | □ |
| SQL·DB 영향을 연결했다. | □ |
| 공통 모듈 영향을 확인했다. | □ |
| Cache·Batch·BI를 확인했다. | □ |
| 외부 소비자를 확인했다. | □ |
| 계약 호환성을 판단했다. | □ |
| 보안·개인정보·감사를 확인했다. | □ |
| 성능·용량 영향을 확인했다. | □ |
| OM 운영정보를 확인했다. | □ |
| 배포단위와 순서를 정의했다. | □ |
| Rollback을 정의했다. | □ |
| 회귀테스트 범위를 정했다. | □ |
| 위험등급과 승인자를 결정했다. | □ |
| 영향 없음 범위도 기록했다. | □ |
| PR에 영향분석을 반영했다. | □ |
| 다른 검토자가 결과를 재현했다. | □ |

# 변경·호환성·폐기 관리

## 영향도 분석서 변경

개발 중 영향 범위가 달라지면 분석서를 갱신한다.

text id="ch9gnz" 초기 분석 → 추가 영향 발견 → 분석서 Version 증가 → 위험등급 재평가 → 테스트·일정 조정 → 재승인

## 긴급 변경

긴급 장애조치도 영향분석을 생략하지 않는다.

최소 기록:

text id="wlfkx0" 장애 ID 영향 ServiceId 변경 파일 데이터 영향 임시조치 Rollback 사후 회귀시험 정식 개선과제

## 예외 승인

표준을 지킬 수 없는 경우:

text id="6cuegw" 예외 사유 대안 위험 보완통제 책임자 만료일 정상화 계획

만료일 없는 영구 예외를 만들지 않는다.

## 폐기 변경

text id="0o7tj9" 사용자·소비자 조사 ↓ 대체 기능 제공 ↓ Deprecated ↓ 호출량 0 확인 ↓ 권한·Catalog·Route 제거 ↓ 코드·DB 객체 폐기 ↓ 로그·문서 보존

# 시사점

## 핵심 아키텍처 판단

첫째, 변경 영향 분석은 개발 이후 확인하는 문서가 아니다.

text id="ucmhux" 개발 전에 수정 범위와 위험을 결정하는 설계 활동이다.

둘째, 수정 파일 수는 영향도의 크기를 나타내지 않는다.

\`\`\`text id=“vfm60h” 공통 Header 한 줄 변경 → 전체 업무 WAR 영향

업무 Service 여러 파일 변경 → 단일 거래 내부 영향



셋째, 직접 호출관계가 없다고 영향이 없는 것은 아니다.

\`\`\`text id="wd322f"
공유 테이블
공통 설정
Cache
Batch
운영 Catalog
Tomcat 자원

을 통한 간접 영향을 반드시 확인해야 한다.

넷째, 변경 범위를 좁히는 것과 영향을 누락하는 것은 다르다.

\`\`\`text id=“4tz3ou” 좋은 변경 = 필요한 범위만 수정

나쁜 변경 = 필요한 영향을 확인하지 않음



다섯째, 영향도 분석은 테스트와 Rollback까지 연결되어야 한다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| 수정 파일만 나열 | 간접 영향 누락 |
| 정방향 분석만 수행 | 다른 소비자 누락 |
| 역방향 분석만 수행 | 하위 데이터 영향 누락 |
| 계약 호환성 미검토 | 외부 소비자 장애 |
| DB 이행 미검토 | 데이터 손상 |
| Cache 영향 누락 | 구 데이터 노출 |
| Batch·BI 누락 | 마감·보고 장애 |
| Timeout 단순 증가 | Thread·Pool 고갈 |
| 공통 모듈 영향 축소 | 다수 WAR 장애 |
| 코드 Rollback만 준비 | 데이터 복구 실패 |
| OM 등록 누락 | 개발 성공·운영 실패 |
| 영향도 문서 미갱신 | 실제 변경과 불일치 |

\---

\## 우선 보완 과제

1\. 화면–ServiceId–프로그램–SQL 추적성 매트릭스를 기준정보화한다.
2\. Mapper와 테이블 사용관계를 자동 추출한다.
3\. ServiceId와 UI·Handler·OM Catalog를 자동 대조한다.
4\. 공통 모듈 소비 WAR 목록을 자동 생성한다.
5\. 요청·응답 Schema 호환성 검사를 CI에 추가한다.
6\. DDL 변경 시 영향 SQL과 소비자를 자동 연결한다.
7\. Property Key의 정의와 사용 위치를 자동 추출한다.
8\. PR Template에 영향도·배포·Rollback 항목을 의무화한다.
9\. 위험등급별 필수 검토자를 자동 지정한다.
10\. 배포 Artifact에 Commit ID와 변경 ID를 포함한다.
11\. 운영 로그에 App Version·Commit ID를 기록한다.
12\. 영향도 분석 결과와 회귀테스트를 자동 연결한다.

\---

\## 중장기 발전 방향

\`\`\`text id="fvinhk"
수동 영향도 분석
↓
추적성 기준정보
↓
소스·DB·OM 자동 연결
↓
변경 Diff 기반 영향 후보 생성
↓
계약·구조·데이터 자동 Gate
↓
테스트 범위 자동 추천
↓
운영 실행이력 기반 영향 분석

# 마무리말

변경 영향도 분석은 다음 질문에 근거를 가지고 답하는 과정이다.

\`\`\`text id=“qabdyi” 무엇이 바뀌는가?

왜 바뀌는가?

누가 이 기능을 사용하는가?

어떤 데이터가 달라지는가?

어떤 계약과 운영정책이 영향을 받는가?

기존 사용자는 계속 동작하는가?

어떤 순서로 배포하는가?

실패하면 무엇을 되돌리는가?

어떤 테스트로 안전성을 증명하는가?



제12장에서 기억할 핵심 흐름은 다음과 같다.

\`\`\`text id="g2tnq3"
변경 요청
↓
기준선
↓
정방향 분석
↓
역방향 분석
↓
직접·간접 영향
↓
호환성·위험
↓
수정·제외 범위
↓
테스트
↓
배포·Rollback
↓
승인·운영 증적

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“yoe15o” 코드를 수정하기 시작한 뒤 영향 범위를 찾는 것이 아니다.

영향 범위를 확인한 뒤 수정할 코드를 결정한다. \`\`\`

제12장을 마치면 기존 프로그램을 읽고 변경 범위를 정하는 제3부의 학습이 완료된다.

다음 부에서는 요구사항을 개발 가능한 거래 단위로 분해하고, 요청·응답 DTO와 Validation을 설계하여 실제 업무 기능 구현을 시작한다.
