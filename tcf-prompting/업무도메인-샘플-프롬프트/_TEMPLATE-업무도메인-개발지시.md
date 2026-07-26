# 업무도메인 개발지시 템플릿

`{{ }}` 자리를 채운 뒤 코드 블록 전문을 채팅에 붙여넣으세요.
샘플1~5는 이 템플릿의 완성 예시입니다.

```text
너는 NSIGHT-TCF-FRAMEWORK의 애플리케이션 아키텍트이자 시니어 Java 개발자다.
저장소의 실제 구현과 표준을 분석한 뒤, 아래 명세대로 업무 도메인을
설계·구현·등록·검증하라. 레퍼런스에 없는 구조를 임의로 발명하지 마라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 고정 개발 입력정보 (임의 변경 금지)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| 구분            | 확정값                                    |
|-----------------|-------------------------------------------|
| 업무코드        | {{업무코드 2자리, 예: LN}}                 |
| 업무명          | {{한글 업무명}}                            |
| 모듈            | {{모듈명}}-service ({{신규 생성|기존 재사용}}) |
| 도메인          | {{도메인 UpperCamelCase, 예: Loan}}        |
| ServiceId       | {{업무코드}}.{{도메인}}.{{행위 lowerCamelCase}} |
| 거래코드        | {{업무코드}}-{{유형약어}}-0001             |
| Processing Type | {{INQUIRY|CREATE|UPDATE|DELETE}}           |
| 메인 클래스     | Nsight{{업무코드 Pascal}}ServiceApplication |
| BASE 패키지     | com.nh.nsight.marketing.{{업무코드 소문자}} |
| WAR / Context   | {{코드소문자}}.war / /{{코드소문자}}       |
| 권장 bootRun 포트| {{포트 — README의 예약표와 충돌 검증 필수}} |
| DB 테이블       | {{테이블명 또는 "미확정 — 질문할 것"}}     |
| 기준 모듈       | eb-service                                 |

포트는 임시 권장값이다. BusinessModuleDefinitions.java·application-*.yml·
Gateway Route를 검색해 충돌을 확인하고, 충돌하면 임의 변경하지 말고
현황과 후보 포트를 먼저 보고하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 구현 전 필수 분석 (파일을 실제로 읽을 것)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- settings.gradle / 루트 build.gradle (businessModules)
- eb-service: NsightEbServiceApplication, build.gradle,
  application*.yml, schema.sql
- eb-service 6계층: {{거래 유형에 맞는 레퍼런스 도메인 —
  조회 페이징은 EbUser, 단순 분기는 EbSample}}의
  Handler/Facade/Service/Rule/Dao/Mapper(+XML)
- tcf-core: TransactionHandler, StandardRequest/Response,
  TransactionContext, BusinessException, ErrorCode
- tcf-ui: BusinessModuleDefinitions.java, sample-requests/ 명명 규약

분석 결과를 표(점검항목·확인값·적용방식)로 먼저 보고한 뒤 코드를 변경하라.
저장소 실제 구조가 본 지시와 다르면 실제 구조를 우선하고 차이를 보고하라.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 설계
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
{{확정 시: 컬럼 표 기재.
미확정 시: "운영 테이블·컬럼을 추측하지 말고 필요한 입력을 표로
한 번에 질문하라. 구조 검증용으로만 local 한정 설계 예시 H2 테이블을
사용하고 '설계 예시' 주석을 명시하라."}}

schema.sql은 spring.sql.init.mode=always(local)로 초기화하고
시드 데이터 5건 이상을 포함한다.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 6계층 구현 (Handler → Facade → Service → Rule → DAO → Mapper)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
com.nh.nsight.marketing.{{코드소문자}}
├── entry/handler/{{코드Pascal}}{{도메인}}Handler
│     serviceIds() 등록, switch 분기,
│     default → BusinessException(ErrorCode.SERVICE_NOT_FOUND)
├── entry/facade/{{코드Pascal}}{{도메인}}Facade
│     @Transactional({{readOnly=true|기본}}, timeout=5),
│     Map body → Request DTO → service → toMap()
├── application/service/…Service — rule 검증 → criteria → dao → Response 조립
├── application/rule/…Rule — 검증·조건 생성만, DB/외부 호출 금지
├── application/dto/{{도메인소문자}}/ — Request(fromMap)·Response(toMap)·Criteria
├── persistence/dao/…Dao — Mapper 1:1 위임 (@Repository)
├── persistence/mapper/…Mapper — @Mapper 인터페이스
├── persistence/dto/{{도메인소문자}}/…Row
└── Mapper XML: src/main/resources/mapper/{{코드소문자}}/…Mapper.xml
    (namespace=Mapper FQCN, #{} 바인딩만, SELECT *·${} 금지)

금지 호출: Handler→DAO/Mapper, Facade→Mapper, Service→Mapper,
Rule→DAO/외부API, 업무별 @RestController 신규 생성, Lombok,
다른 업무 WAR 클래스 직접 import.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 등록 절차
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
{{신규 모듈인 경우:
1. settings.gradle include + 루트 build.gradle businessModules 추가
2. build.gradle — eb-service 복사, archiveFileName만 변경
3. application.yml(business-code)·application-local.yml(포트·H2 URL)
4. tcf-ui BusinessModuleDefinitions.ALL 에 모듈 추가
5. tcf-ui sample-requests/{{코드소문자}}-sample-inquiry.json 생성
   (전체 표준 header 필드 포함 — eb-sample-inquiry.json 기준)
/ 기존 모듈인 경우: 1~5 생략, 신규 도메인 클래스·XML·거래만 추가}}

게이트: gradle :{{모듈}}:compileJava (+ tcf-ui 수정 시 :tcf-ui:compileJava)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. compileJava 성공
2. bootRun 기동 (포트 {{포트}}) — Started 로그·schema 초기화 확인
3. POST http://localhost:{{포트}}/online — 전체 header + body 로 호출,
   result.resultCode == "S0000" 확인
4. {{유형별 검증: 조회=필터·페이징 / CREATE=INSERT 후 재조회 /
   UPDATE·DELETE=변경 전후 비교}}
5. 음성 테스트: 미지원 serviceId → SERVICE_NOT_FOUND,
   {{규칙 위반 케이스}} → BUSINESS_ERROR
6. 검증 결과를 표(항목·기대값·실측값·판정)로 보고
7. 실행하지 않은 명령은 '미실행'으로 표시하고 성공으로 보고하지 마라

모호한 부분은 구현 전에 나에게 질문할 것.
```
