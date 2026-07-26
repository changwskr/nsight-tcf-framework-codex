# 1단계 — Phase 실행 프롬프트 (LN 완성본)

> 원본: `2026-07-26-nsight-tcf-prompts.md` 7번 (방법론 절차 실행, Phase 0~6)
> 입력 설계서: 같은 폴더의 `LN-프로그램-설계서-예시.md` · `LN-UI-레이아웃-설계서-예시.md`
> 다른 업무에 쓸 때는 업무코드·포트·설계서 경로만 교체하면 됩니다.

```text
너는 NSIGHT-TCF-FRAMEWORK 방법론에 따라 새 업무 모듈을 구현하는 개발자야.

입력 설계서 (구현의 유일한 근거 — 반드시 먼저 읽을 것):
- tcf-prompting/업무도메인-개발방법론-샘플-프롬프트/LN-프로그램-설계서-예시.md
- tcf-prompting/업무도메인-개발방법론-샘플-프롬프트/LN-UI-레이아웃-설계서-예시.md

표준 레퍼런스: eb-service (구조·어노테이션·명명은 EB 소스를 그대로 따를 것)

아래 절차를 Phase 단위로 진행하고, 각 Phase 완료 시
검증 게이트 통과 여부를 보고한 뒤 다음 Phase로 넘어가.
게이트에 실패하면 다음 Phase로 넘어가지 말고 원인을 고쳐서 재시도해.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 0] 모듈 등록
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- 포트 사전 확인: tcf-ui BusinessModuleDefinitions.java(포트 SoT)와
  저장소 전체 application-*.yml에서 8103 충돌 여부 확인.
  충돌하면 임의 변경하지 말고 현황과 후보 포트를 먼저 보고.
- settings.gradle include 'ln-service' (업무 모듈 나열 위치)
- 루트 build.gradle ext.businessModules 에 'ln-service' 추가
- ln-service/build.gradle — eb-service 복사, archiveFileName='ln.war'
- NsightLnServiceApplication — NsightWarBootstrap 상속,
  scanBasePackages="com.nh.nsight",
  @MapperScan("com.nh.nsight.marketing.ln.persistence.mapper")
- application.yml(business-code=LN) / application-local.yml
  (설계서 §9의 값 그대로: port 8103, nsight_ln H2 URL, 거래로그 datasource)
- tcf-ui 등록 (누락 시 릴레이·화면 불가):
  · BusinessModuleDefinitions.ALL 에
    new ModuleDefinition("LN", "Loan", "여신", 8103) 추가
  · sample-requests/ln-sample-inquiry.json 생성 — 파일명 규약 필수
    (BusinessModuleCatalog가 강제 로드, 없으면 tcf-ui 기동 실패),
    header는 eb-sample-inquiry.json의 전체 필드를 LN 값으로 교체
- 게이트: gradle :ln-service:compileJava && gradle :tcf-ui:compileJava 성공

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 1] 설계서 §6 테이블 → schema.sql, §5 DTO → 클래스
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- 설계서 §6의 LN_LOAN 정의 그대로 schema.sql 작성
  ("설계 예시" 주석 명시, 시드 10건 이상 — NORMAL 7·OVERDUE 2·CLOSED 1)
- 설계서 §5의 DTO 5종 구현: LoanInquiryRequest·LoanListInquiryRequest
  (fromMap, trim→null 보정), LoanInquiryResponse·LoanListInquiryResponse
  (toMap — §5.2 필드 구성 그대로), LoanRow (+ LoanSearchCriteria)
- 게이트: bootRun 기동 시 schema 초기화 로그 확인 (기동 후 종료)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 2] 설계서 §4.5 → persistence 계층
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- LnLoanMapper 인터페이스(@Mapper): selectLoan·searchLoans·countLoans
- LnLoanMapper.xml (resources/mapper/ln/): namespace=FQCN,
  §4.5 SQL 요약 그대로 — selectLoan 단건, searchLoans 동적 WHERE
  (customerId =, loanStatus =, loanNo LIKE) + CREATED_AT DESC +
  OFFSET/FETCH, countLoans는 <sql> WHERE 공유. SELECT *·${} 금지.
- LnLoanDao(@Repository) — Mapper 1:1 위임
- 게이트: 매퍼 XML의 SQL이 설계서 §4.5 "SQL 요약"과 일치함을
  표(메서드·설계서 요약·구현 SQL 발췌)로 증명

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 3] 설계서 §4.4 → application 계층 (Rule → Service 순서)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- LnLoanRule — §4.4 표의 규칙을 그대로 구현:
  validateInquiry(loanNo 필수·20자), validateInquiryList
  (pageSize≤100, pageNo≥1), buildSearchCriteria(기본값 pageNo=1·
  pageSize=15·offset 계산·trim→null)
- LnLoanService — §4.3 처리 로직 그대로:
  inquiry(단건, 미존재 시 BUSINESS_ERROR),
  inquiryList(search+count 이중 조회)
- 게이트: Rule 위반 입력이 BusinessException(BUSINESS_ERROR)으로
  이어지는 것을 확인 (§8 오류 표의 메시지와 대조)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 4] 설계서 §3 거래 목록 → entry 계층 (Facade → Handler)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- LnLoanFacade — §4.2 속성 그대로 (@Transactional(readOnly=true,
  timeout=5), Map→DTO→service→toMap)
- LnLoanHandler — serviceIds()에 §3의 2건 등록, switch 분기,
  default → SERVICE_NOT_FOUND("LnLoanHandler 미지원 serviceId: ...")
- 게이트: bootRun(8103) 후 §3의 모든 serviceId를 POST로 호출 성공.
  bootRun은 context-path가 / 이므로 POST http://localhost:8103/online
  (또는 /ln/online — 둘 다 매핑됨). header는 전체 필드 필수.
  - LN.Loan.inquiry: body{loanNo: 시드 값} → S0000, 단건
  - LN.Loan.inquiryList: body{pageNo:1, pageSize:5} → S0000,
    rows 5건, totalCount=시드 건수
  - 음성: loanNo 누락 → BUSINESS_ERROR, pageSize=999 → BUSINESS_ERROR,
    LN.Loan.unknown → SERVICE_NOT_FOUND

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 5] UI 설계서 §4 → tcf-ui/static/ln/ 화면 구현
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- _shared/ln-admin.js — av-admin.js 패턴 복제 (BUSINESS_CODE='LN',
  LOCAL_PORT=8103, TX에 loanListInquiry 정의)
- static/ln/loan-list.html — UI 설계서 §4.1과 1:1 일치:
  필터 필드 ID(filterLoanNo·filterCustomerId·filterLoanStatus),
  그리드 5컬럼(§4.1.3 순서·표시 형식 그대로 — 금액 콤마·상태 칩),
  이벤트(§4.1.4 — Enter 조회·필터 초기화·페이지 이동·빈 결과 문구)
- 게이트: tcf-ui(8099) 기동 → /ln/loan-list.html 접속 →
  조회·필터·페이징 동작과 그리드 렌더링 확인
  (브라우저 확인이 불가하면 릴레이 API 호출로 대체 검증 후 그 사실을 명시)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase 6] 마감
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- OM 기준정보: 저장소의 실제 OM Service Catalog 등록 방식을 먼저
  확인하고(tcf-om·om-service), 적용 가능하면 §3의 2거래를 등록,
  불가하면 등록안(SQL/시드)만 작성하고 '미실행'으로 구분 보고
- 거래로그: 거래 호출 후 거래로그 DB에 LN 거래가 기록되는지 확인
- ln-service/README.md 작성 (모듈·포트·serviceId·실행·호출 예시)
- 게이트: 설계서 대비 구현 커버리지 100% 매트릭스 제출 —
  §3 거래·§4 클래스·§5 DTO·§6 테이블·UI §4 필드/컬럼/이벤트 각각에
  대해 [설계서 항목 | 구현 파일 | 상태(완료/미완료/미실행)] 표

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
절대 규칙
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- 설계서에 없는 클래스·거래·필드를 임의로 추가하지 마
- 설계서와 저장소 실제 구조가 충돌하면 구현 전에 나에게 질문해
- Lombok·@RestController 금지, 계층 건너뛰기 금지
- 실행하지 않은 명령·검증은 '미실행'으로 표시하고 성공으로 보고하지 마
```
