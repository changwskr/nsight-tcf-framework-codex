## ●●●●●●●●●●● 메타프롬프팅 — 신규 업무 모듈 개발 프롬프트 생성기 ●●●●●●●●●●●●●

```text
너는 세계 최고의 프롬프트 엔지니어이자
NSIGHT-TCF-FRAMEWORK 아키텍처 전문가야.

내가 새로운 업무 모듈(예: av-service)을
이 프레임워크의 표준 패턴대로 개발하려 할 때 사용할,
가장 완벽한 '개발 지시 프롬프트'를 만들어줘.

프롬프트에는 반드시 다음이 포함되어야 해:
- 6계층 패키지 규약 (entry/handler → facade → service → rule → dao/mapper)
- ServiceId 형식 ({업무코드}.{업무명}.{처리유형})
- NsightWarBootstrap 상속, settings.gradle·build.gradle 등록 절차
- 레퍼런스로 삼을 파일 경로 (eb-service 기준)
- 완료 검증 방법 (bootRun 포트, POST /{업무코드}/online 샘플 호출)

내가 반드시 제공해야 할 정보들
(업무코드, 도메인 엔티티, 거래 목록, DB 테이블 등)이 있다면
프롬프트를 만들기 전에 나에게 먼저 질문해줘.
```

# AV(av-service) 신규 업무 모듈 개발 지시 프롬프트

> 4번 메타프롬프팅 산출물 (2026-07-26)
> 확정 정보: 업무코드 **AV** · 도메인 **Sample** · 거래 **목록 조회 1건(페이징)** ·
> 테이블 **AV_SAMPLE**(자동 설계) · 방식 **수작업(eb-service 표준 복제) + tcf-ui 화면**
>
> 아래 프롬프트 전문을 Cursor 채팅에 그대로 붙여넣으면 됩니다.

---

```text
너는 NSIGHT-TCF-FRAMEWORK 표준 패턴을 정확히 따르는 업무 모듈 개발자야.
신규 업무 모듈 av-service를 아래 명세대로 구현해.
표준 레퍼런스는 eb-service의 Sample 도메인이며,
레퍼런스에 없는 구조를 임의로 발명하지 마.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1] 모듈 명세
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- 업무코드: AV (nsight.tcf.runtime.business-code=AV)
- 모듈: av-service (기존 폴더 재사용, 안의 kkk.java는 삭제)
- 베이스 패키지: com.nh.nsight.marketing.av
- 메인 클래스: NsightAvServiceApplication
  → com.nh.nsight.tcf.web.support.NsightWarBootstrap 상속 (eb-service의
    NsightEbServiceApplication과 동일 구조)
- 실행: Spring Boot WAR (av.war) · bootRun 포트 8084 · Tomcat context /av
- 거래 엔드포인트: POST /online (bootRun) / POST /av/online (ztomcat)
- DB: H2(local, MODE=Oracle) + MyBatis mapper XML (classpath:/mapper/av/*.xml)
- 거래로그: 공유 H2 (nsight.txlog.path — eb-service application.yml 값 복사)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2] 거래(serviceId) 정의 — 1건
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
| serviceId         | transactionCode | processingType | 설명                     |
|-------------------|-----------------|----------------|--------------------------|
| AV.Sample.inquiry | AV-INQ-0001     | INQUIRY        | 샘플 목록 조회(페이징)   |

ServiceId 규칙: {업무코드}.{도메인}.{행위} — 도메인은 UpperCamelCase,
행위는 lowerCamelCase. 이 형식을 벗어나면 안 됨.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[3] 테이블 설계 — AV_SAMPLE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
src/main/resources/schema.sql 에 작성 (spring.sql.init.mode=always):

| 컬럼        | 타입           | 제약     | 설명       |
|-------------|----------------|----------|------------|
| SAMPLE_KEY  | VARCHAR2(30)   | PK       | 샘플 키    |
| SAMPLE_NAME | VARCHAR2(100)  | NOT NULL | 샘플명     |
| USE_YN      | CHAR(1)        | 기본 'Y' | 사용 여부  |
| CREATED_AT  | TIMESTAMP      | 기본 현재시각 | 등록일시 |

시드 데이터 5건 이상 INSERT 포함 (조회 결과 확인용).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[4] 6계층 패키지 규약 — 클래스 목록
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
com.nh.nsight.marketing.av
├── entry/handler/AvSampleHandler        ← TransactionHandler 구현,
│     serviceIds()에 "AV.Sample.inquiry" 등록, switch 분기,
│     default는 BusinessException(ErrorCode.SERVICE_NOT_FOUND)
├── entry/facade/AvSampleFacade          ← @Transactional(readOnly=true, timeout=5)
│     inquiry(Map body, TransactionContext) → Request 변환 → service → toMap()
├── application/service/AvSampleService  ← rule 검증 → criteria →
│     dao.searchSamples + dao.countSamples (이중 조회) → Response 조립
├── application/rule/AvSampleRule        ← validateInquiry(페이징 기본값
│     pageNo=1, pageSize=15, 최대 100), buildSearchCriteria
├── application/dto/sample/              ← SampleInquiryRequest(fromMap),
│     SampleInquiryResponse(toMap: rows·totalCount·pageNo·pageSize·guid),
│     SampleSearchCriteria
├── persistence/dao/AvSampleDao          ← Mapper 1:1 위임
├── persistence/mapper/AvSampleMapper    ← @Mapper 인터페이스
├── persistence/dto/sample/SampleRow
└── (메인) NsightAvServiceApplication

Mapper XML: src/main/resources/mapper/av/AvSampleMapper.xml
- searchSamples: 동적 WHERE(<where><if>) — sampleKey LIKE, sampleName LIKE,
  useYn = 조건 · CREATED_AT DESC · OFFSET/FETCH 페이징
- countSamples: 동일 WHERE 건수

처리 흐름 (절대 순서): entry/handler → entry/facade →
application/service → application/rule → persistence/dao → mapper

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[5] 레퍼런스 파일 (구조를 그대로 따라할 것)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- eb-service/src/main/java/com/nh/nsight/marketing/eb/entry/handler/EbSampleHandler.java
- eb-service/src/main/java/com/nh/nsight/marketing/eb/entry/facade/EbUserFacade.java
  (페이징 조회 패턴은 User 도메인이 기준: search+count 이중 조회)
- eb-service/src/main/java/com/nh/nsight/marketing/eb/application/service/EbUserService.java
- eb-service/src/main/java/com/nh/nsight/marketing/eb/application/rule/EbUserRule.java
- eb-service/src/main/java/com/nh/nsight/marketing/eb/persistence/dao/EbUserDao.java
- eb-service/src/main/resources/mapper/eb/EbUserMapper.xml (동적 WHERE·페이징 SQL)
- eb-service/src/main/java/com/nh/nsight/marketing/eb/NsightEbServiceApplication.java
- eb-service/src/main/resources/application.yml (포트·DB·거래로그 설정)
- eb-service/build.gradle (의존성 그대로, 아카이브명만 av.war)
- 설계 문서: ztcf-methodology/EB-프로그램-설계서.md (9섹션 구조)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[6] 빌드 등록 절차 (순서대로)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. settings.gradle 에 include 'av-service' 추가
2. av-service/build.gradle 생성 — eb-service/build.gradle 복사 후
   archiveFileName을 'av.war'로 변경
3. 루트 build.gradle 의 ext.businessModules 배열에 'av-service' 추가
4. av-service/src/main/resources/application.yml — server.port=8084,
   business-code=AV, DB URL jdbc:h2:mem:nsight_av;MODE=Oracle
5. 게이트: gradle :av-service:compileJava 성공 확인

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[7] UI 화면 (tcf-ui)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- 경로: tcf-ui/src/main/resources/static/av/sample-list.html (+ index.html 링크)
- 레퍼런스: tcf-ui/src/main/resources/static/eb/ 의 화면과 _shared 리소스
- 구성 (EB-UI-레이아웃-설계서.md 공통 레이아웃 준수):
  Header → Meta Bar(연결 대상·거래 AV.Sample.inquiry/AV-INQ-0001) →
  조회 필터(sampleKey·sampleName·useYn) → 결과 그리드
  (샘플키·샘플명·사용여부·등록일시 4컬럼) → 페이지네이션(페이지당 15건)
- 거래 호출: 표준전문 header(businessCode=AV, serviceId, transactionCode,
  processingType=INQUIRY, channelId) + body(필터·pageNo·pageSize)로
  POST /av/online — eb-admin.js 패턴 참조
- Enter 키 조회, 빈 결과 문구, 오류 alert 공통 규칙 적용

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[8] 완료 검증 (모두 통과해야 완료)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. gradle :av-service:compileJava — 컴파일 성공
2. gradle :av-service:bootRun — 8084 기동, schema.sql 초기화 로그 확인
3. 샘플 호출 (tcf-ui/src/main/resources/sample-requests/ 형식 참조):
   curl -X POST http://localhost:8084/online -H "Content-Type: application/json"
   body: header{businessCode:"AV", serviceId:"AV.Sample.inquiry",
   transactionCode:"AV-INQ-0001", processingType:"INQUIRY"} +
   body{pageNo:1, pageSize:15}
   → result.resultCode == "S0000", rows 5건 이상, totalCount 일치 확인
4. 필터 조건(sampleName LIKE) 호출 → WHERE 반영 확인
5. 미지원 serviceId(AV.Sample.unknown) 호출 → SERVICE_NOT_FOUND 오류 확인
6. tcf-ui 기동(:8099) 후 /av/sample-list.html 에서 조회·페이징 동작 확인
7. 검증 결과를 표(항목·기대값·실측값·판정)로 보고

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[9] 금지 사항
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- 명세에 없는 거래·클래스·의존성 추가 금지
- @RestController 직접 생성 금지 — 진입은 TCF 파이프라인(/online)만 사용
- 계층 건너뛰기 금지 (Handler에서 DAO 직접 호출 등)
- Lombok 사용 금지 (레퍼런스 코드와 동일하게 명시적 생성자·getter)
- 모호한 부분은 구현 전에 나에게 질문할 것

완료 후: 8번 역공학 프롬프트로 ztcf-methodology/AV-프로그램-설계서.md 를
생성해 문서화까지 마무리한다.
```
