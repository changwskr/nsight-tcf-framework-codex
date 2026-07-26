
## NSIGHT 화면 이벤트–ServiceId–프로그램 전체 추적성 매트릭스

### 1. 도입 전 안내말

화면 이벤트–ServiceId–프로그램 전체 추적성 매트릭스는 단순한 프로그램 목록이 아니다.

사용자가 화면에서 버튼을 클릭하거나 행을 선택한 시점부터 어떤 거래가 실행되고, 어떤 Java 프로그램과 SQL을 거쳐 어떤 데이터가 조회·변경되는지를 하나의 연결 구조로 관리하는 기준 산출물이다.

```
화면
  ↓
화면 이벤트
  ↓
표준 전문 및 ServiceId
  ↓
OnlineTransactionController
  ↓
TCF → STF → TransactionDispatcher
  ↓
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  ├─ DAO → Mapper → SQL → DB 객체
  └─ 외부연계 Client → 외부 시스템
  ↓
ETF
  ↓
표준 응답
  ↓
화면 성공·실패 처리
```

NSIGHT TCF에서는 업무별 Controller가 Service를 직접 호출하지 않는다. 공통 OnlineTransactionController가 요청을 수신하고, serviceId를 기준으로 Dispatcher가 Handler를 찾아 업무 계층을 실행한다. 따라서 화면 이벤트와 업무 프로그램을 연결하는 핵심 식별자는 Controller가 아니라 ServiceId다.

또한 화면과 이벤트는 일대다이고, 하나의 이벤트가 여러 ServiceId를 순차 또는 병렬 호출할 수 있으며, 하나의 ServiceId가 여러 DAO·Mapper·테이블을 사용할 수 있다. 따라서 매트릭스는 이러한 다중 관계를 표현할 수 있어야 한다.

## 2. 문서 개요

### 2.1 목적

| 목적 | 설명 |
| --- | --- |
| 정방향 추적 | 화면 이벤트에서 ServiceId, 프로그램, SQL, DB 객체까지 추적 |
| 역방향 추적 | 테이블·SQL·프로그램 변경 시 영향 화면과 이벤트 식별 |
| 설계 정합성 | 화면설계서, 프로그램설계서, DB설계서 간 불일치 방지 |
| 개발 누락 방지 | ServiceId, Handler, Rule, DAO, Mapper 누락 확인 |
| 테스트 추적 | 화면 이벤트와 단위·통합·거래 테스트 연결 |
| 운영 추적 | ServiceId, 거래코드, GUID, 감사로그 연결 |
| 변경 영향 분석 | 프로그램·SQL·테이블 변경 시 영향 범위 산출 |
| 품질 Gate | 미등록 ServiceId, 미사용 SQL, 미연결 이벤트 자동검증 |

### 2.2 적용범위

- WEBTOPSUITE 화면
- BI포털 React 화면
- TCF-UI 및 UJ 채널
- 온라인 조회·등록·변경·삭제 거래
- 팝업·탭·그리드 이벤트
- 엑셀·파일 다운로드
- 업무 WAR 간 호출
- 외부 시스템 연계
- Handler → Facade → Service → Rule → DAO → Mapper
- OM Service Catalog, 거래통제, Timeout, 감사로그

### 2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 업무 분석가 | 화면 기능과 거래 정의 |
| UI 개발자 | 이벤트와 ServiceId 연결 |
| 업무 개발자 | Handler 이하 프로그램 구현 |
| 아키텍트 | 계층과 책임 경계 검증 |
| DBA·DA | SQL과 DB 객체 영향 분석 |
| 테스트팀 | 이벤트별 테스트 시나리오 작성 |
| 운영팀 | ServiceId와 장애 프로그램 추적 |
| PMO·품질팀 | 산출물 완전성과 정합성 점검 |

### 2.4 선행조건

- 화면 ID와 화면명이 확정되어 있어야 한다.
- 화면 이벤트 ID 체계가 정의되어 있어야 한다.
- 업무코드와 업무세구분코드가 확정되어 있어야 한다.
- ServiceId와 거래코드 명명규칙이 확정되어 있어야 한다.
- 업무 계층과 패키지 구조가 확정되어 있어야 한다.
- Mapper Namespace와 SQL ID 기준이 확정되어 있어야 한다.
- DB 논리·물리 객체가 식별되어 있어야 한다.

## 3. 매트릭스 작성 원칙

### 3.1 한 행의 기준

매트릭스의 한 행은 다음 단위를 나타낸다.

```
화면 이벤트 1건
× ServiceId 호출 1건
× 최종 데이터 처리 경로 1건
```

예를 들어 하나의 조회 버튼이 2개 ServiceId를 호출하면 2개 행으로 작성한다.

```
조회 버튼
  ├─ SV.Customer.selectSummary
  └─ CT.Contact.selectHistory
```

하나의 ServiceId가 2개의 Mapper SQL을 호출해도 2개 행으로 작성한다.

```
SV.Customer.selectSummary
  ├─ SvCustomerMapper.selectSummary
  └─ SvCustomerMapper.selectGrade
```

상위 화면·이벤트·ServiceId 컬럼은 반복해서 기록하고, 데이터 처리 순번으로 구분한다.

### 3.2 추적 ID 형식

```
{화면ID}-{이벤트순번}-{호출순번}-{데이터순번}
```

예시:

```
SV-CUS-0001-E01-C01-D01
SV-CUS-0001-E01-C01-D02
SV-CUS-0001-E01-C02-D01
```

| 구성 | 의미 |
| --- | --- |
| SV-CUS-0001 | 화면 ID |
| E01 | 화면 이벤트 순번 |
| C01 | ServiceId 호출 순번 |
| D01 | DAO·SQL·외부연계 처리 순번 |

### 3.3 공통 호출 구조

모든 TCF 온라인 거래의 공통 구간은 다음과 같이 기록한다.

| 구분 | 표준값 |
| --- | --- |
| Endpoint | POST /{businessCode}/online |
| Controller | OnlineTransactionController.online() |
| 실행 엔진 | TCF.process() |
| 전처리 | STF.preProcess() |
| 라우팅 | TransactionDispatcher.dispatch() |
| 후처리 | ETF.success/businessFail/systemError() |

공통 프로그램은 매 행에 반복하여 기록하거나, 별도 공통 경로 코드 TCF-ONLINE-01로 관리할 수 있다.

## 4. 전체 매트릭스 컬럼 정의

### 4.1 화면·이벤트 영역

| 컬럼 | 필수 | 설명 | 예시 |
| --- | --- | --- | --- |
| 추적 ID | 예 | 행 단위 고유 식별자 | SV-CUS-0001-E01-C01-D01 |
| 업무코드 | 예 | 업무 WAR 또는 업무 영역 | SV |
| 업무세구분코드 | 조건 | 업무 하위 분류 | CUS |
| 화면 ID | 예 | 화면 고유 식별자 | SV-CUS-0001 |
| 화면명 | 예 | 사용자 화면명 | 고객 종합정보 조회 |
| 기능 ID | 예 | 화면 기능 식별 | SV-CUS-0001-F01 |
| 이벤트 ID | 예 | 화면 이벤트 식별 | SV-CUS-0001-E01 |
| 이벤트명 | 예 | 이벤트 설명 | 고객요약 조회 |
| 이벤트 유형 | 예 | 로딩·클릭·변경·선택 | 버튼 클릭 |
| UI 객체 ID | 예 | 버튼·그리드·필드 ID | btnSearch |
| 발생 조건 | 예 | 이벤트 실행 조건 | 고객번호 입력 |
| 선행 검증 | 예 | UI 입력 검증 | 고객번호 필수 |
| 호출 순번 | 예 | 이벤트 내 호출 순서 | 1 |
| 호출 방식 | 예 | 동기·비동기·병렬 | 동기 |
| 필수 여부 | 예 | 실패 시 전체 이벤트 중단 여부 | 필수 |

### 4.2 거래·통제 영역

| 컬럼 | 필수 | 설명 | 예시 |
| --- | --- | --- | --- |
| Context Path | 예 | 업무 WAR 경로 | /sv |
| Endpoint | 예 | 온라인 진입 URL | /sv/online |
| ServiceId | 예 | 업무 거래 식별자 | SV.Customer.selectSummary |
| 거래코드 | 예 | 통제·감사·통계 식별자 | SV-INQ-0001 |
| 처리유형 | 예 | 조회·등록·변경·삭제·다운로드 | INQUIRY |
| 요청 DTO | 예 | 거래 요청 객체 | SvCustomerSummaryRequest |
| 응답 DTO | 예 | 거래 응답 객체 | SvCustomerSummaryResponse |
| 권한코드 | 조건 | 실행 기능권한 | SV_CUSTOMER_VIEW |
| 인증 필요 | 예 | 인증 필요 여부 | Y |
| 감사 대상 | 예 | 감사로그 기록 여부 | Y |
| Timeout | 예 | 거래 제한시간 | 3초 |
| 중복방지 | 예 | Idempotency 적용 | N |
| OM 등록 | 예 | Service Catalog 등록 여부 | Y |
| 거래통제 | 예 | Allow·Block 정책 대상 | Y |

### 4.3 프로그램 영역

| 컬럼 | 필수 | 설명 |
| --- | --- | --- |
| Controller | 예 | 공통 온라인 Controller |
| Controller Method | 예 | 공통 진입 메서드 |
| Handler | 예 | ServiceId 대응 업무 진입 클래스 |
| Handler Method | 예 | ServiceId 분기·Facade 호출 메서드 |
| Facade | 예 | 유스케이스 조립 클래스 |
| Facade Method | 예 | 트랜잭션 경계 메서드 |
| 트랜잭션 유형 | 예 | 읽기전용·일반·없음 |
| Service | 예 | 업무 처리 클래스 |
| Service Method | 예 | 업무 처리 메서드 |
| Rule | 조건 | 업무 규칙 클래스 |
| Rule Method | 조건 | 규칙 검증 메서드 |
| DAO | 조건 | 데이터 접근 클래스 |
| DAO Method | 조건 | Mapper 호출 메서드 |
| 외부 Client | 조건 | 외부 시스템 호출 클래스 |
| 외부 Client Method | 조건 | 외부 호출 메서드 |

### 4.4 데이터·연계 영역

| 컬럼 | 필수 | 설명 |
| --- | --- | --- |
| Mapper Namespace | 조건 | Mapper 인터페이스·XML Namespace |
| Mapper Method | 조건 | Mapper Interface 메서드 |
| Mapper XML | 조건 | MyBatis XML 파일 |
| SQL ID | 조건 | Statement ID |
| SQL 유형 | 조건 | SELECT·INSERT·UPDATE·DELETE·MERGE |
| DB 스키마 | 조건 | 대상 Schema |
| DB 객체 | 조건 | Table·View |
| CRUD | 조건 | C·R·U·D |
| 개인정보 | 예 | 개인정보 포함 여부 |
| 마스킹 | 조건 | 결과 마스킹 적용 여부 |
| Cache | 조건 | Cache명·Key |
| 외부 시스템 | 조건 | 연계 대상 시스템 |
| 인터페이스 ID | 조건 | 대외·대내 인터페이스 식별자 |

### 4.5 응답·운영·시험 영역

| 컬럼 | 필수 | 설명 |
| --- | --- | --- |
| 화면 성공 처리 | 예 | 성공 시 UI 처리 |
| 화면 실패 처리 | 예 | 실패 시 UI 처리 |
| 주요 오류코드 | 예 | 예상 업무·시스템 오류 |
| 거래로그 | 예 | 거래로그 대상 여부 |
| 감사로그 | 예 | 감사로그 대상 여부 |
| 테스트 케이스 ID | 예 | 연결된 테스트 ID |
| 개발 담당 | 예 | 개발 담당 조직·담당자 |
| 검토 담당 | 예 | 설계·코드 검토 책임 |
| 구현 상태 | 예 | 설계·개발·완료·폐기 |
| 적용 버전 | 예 | 배포 버전 |
| 변경 요청 ID | 조건 | 변경관리 번호 |
| 비고 | 아니오 | 제약·예외사항 |

## 5. 매트릭스 A — 화면 이벤트와 ServiceId

| 추적 ID | 화면 ID | 화면명 | 이벤트 ID | 이벤트명 | UI 객체 | 발생 조건 | 호출 순번 | ServiceId | 거래코드 | 처리유형 | 호출 방식 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001-E00-C01-D01 | SV-CUS-0001 | 고객 종합정보 조회 | E00 | 화면 초기화 | screen.onload | 화면 최초 진입 | 1 | SV.Customer.selectInitial | SV-INQ-0000 | 조회 | 동기 |
| SV-CUS-0001-E01-C01-D01 | SV-CUS-0001 | 고객 종합정보 조회 | E01 | 고객요약 조회 | btnSearch | 고객번호 입력 | 1 | SV.Customer.selectSummary | SV-INQ-0001 | 조회 | 동기 |
| SV-CUS-0001-E01-C01-D02 | SV-CUS-0001 | 고객 종합정보 조회 | E01 | 고객등급 동시조회 | btnSearch | 고객요약 조회 실행 | 1 | SV.Customer.selectSummary | SV-INQ-0001 | 조회 | 동기 |
| SV-CUS-0001-E02-C01-D01 | SV-CUS-0001 | 고객 종합정보 조회 | E02 | 고객 상세행 선택 | grdCustomer | 조회 결과행 선택 | 1 | SV.Customer.selectDetail | SV-INQ-0002 | 조회 | 동기 |
| SV-CUS-0001-E03-C01-D01 | SV-CUS-0001 | 고객 종합정보 조회 | E03 | 접촉이력 팝업 | btnContact | 고객 선택 완료 | 1 | CT.Contact.selectHistory | CT-INQ-0001 | 조회 | 동기 |
| SV-CUS-0001-E04-C01-D01 | SV-CUS-0001 | 고객 종합정보 조회 | E04 | 엑셀 다운로드 | btnExcel | 조회 결과 존재 | 1 | SV.Customer.downloadSummary | SV-DWN-0001 | 다운로드 | 동기 |
| CM-CAM-0001-E01-C01-D01 | CM-CAM-0001 | 캠페인 관리 | E01 | 캠페인 목록 조회 | btnSearch | 검색조건 입력 | 1 | CM.Campaign.selectList | CM-INQ-0001 | 조회 | 동기 |
| CM-CAM-0001-E02-C01-D01 | CM-CAM-0001 | 캠페인 관리 | E02 | 캠페인 등록 | btnSave | 필수값 검증 성공 | 1 | CM.Campaign.create | CM-REG-0001 | 등록 | 동기 |
| CM-CAM-0001-E03-C01-D01 | CM-CAM-0001 | 캠페인 관리 | E03 | 캠페인 승인 | btnApprove | 작성상태 캠페인 선택 | 1 | CM.Campaign.approve | CM-UPD-0001 | 변경 | 동기 |
| CM-CAM-0001-E04-C01-D01 | CM-CAM-0001 | 캠페인 관리 | E04 | 캠페인 삭제 | btnDelete | 삭제 가능 상태 | 1 | CM.Campaign.delete | CM-DEL-0001 | 삭제 | 동기 |
| CM-CAM-0001-E05-C01-D01 | CM-CAM-0001 | 캠페인 관리 | E05 | 상품 선택 팝업 | btnProduct | 상품선택 버튼 클릭 | 1 | PD.Product.selectList | PD-INQ-0001 | 조회 | 동기 |
| IC-CRD-0001-E01-C01-D01 | IC-CRD-0001 | 신용실적 조회 | E01 | 신용실적 조회 | btnSearch | 고객번호 입력 | 1 | IC.Credit.selectResult | IC-INQ-0001 | 외부조회 | 동기 |

## 6. 매트릭스 B — ServiceId와 프로그램

| 추적 ID | ServiceId | Controller.Method | Handler.Method | Facade.Method | 트랜잭션 | Service.Method | Rule.Method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001-E00-C01-D01 | SV.Customer.selectInitial | OnlineTransactionController.online | SvCustomerHandler.handleSelectInitial | SvCustomerFacade.selectInitial | Read Only | SvCustomerService.selectInitial | SvCustomerInquiryRule.validateInitial |
| SV-CUS-0001-E01-C01-D01 | SV.Customer.selectSummary | OnlineTransactionController.online | SvCustomerHandler.handleSelectSummary | SvCustomerFacade.selectSummary | Read Only | SvCustomerService.selectSummary | SvCustomerInquiryRule.validateInquiry |
| SV-CUS-0001-E01-C01-D02 | SV.Customer.selectSummary | OnlineTransactionController.online | SvCustomerHandler.handleSelectSummary | SvCustomerFacade.selectSummary | Read Only | SvCustomerService.selectSummary | SvCustomerInquiryRule.validateInquiry |
| SV-CUS-0001-E02-C01-D01 | SV.Customer.selectDetail | OnlineTransactionController.online | SvCustomerHandler.handleSelectDetail | SvCustomerFacade.selectDetail | Read Only | SvCustomerService.selectDetail | SvCustomerInquiryRule.validateDetailInquiry |
| SV-CUS-0001-E03-C01-D01 | CT.Contact.selectHistory | OnlineTransactionController.online | CtContactHandler.handleSelectHistory | CtContactFacade.selectHistory | Read Only | CtContactService.selectHistory | CtContactInquiryRule.validate |
| SV-CUS-0001-E04-C01-D01 | SV.Customer.downloadSummary | OnlineTransactionController.online | SvCustomerHandler.handleDownloadSummary | SvCustomerFacade.downloadSummary | Read Only | SvCustomerDownloadService.createDownloadFile | SvCustomerDownloadRule.validate |
| CM-CAM-0001-E01-C01-D01 | CM.Campaign.selectList | OnlineTransactionController.online | CmCampaignHandler.handleSelectList | CmCampaignFacade.selectList | Read Only | CmCampaignService.selectList | CmCampaignInquiryRule.validateSearch |
| CM-CAM-0001-E02-C01-D01 | CM.Campaign.create | OnlineTransactionController.online | CmCampaignHandler.handleCreate | CmCampaignFacade.create | Read Write | CmCampaignService.create | CmCampaignCreateRule.validate |
| CM-CAM-0001-E03-C01-D01 | CM.Campaign.approve | OnlineTransactionController.online | CmCampaignHandler.handleApprove | CmCampaignFacade.approve | Read Write | CmCampaignService.approve | CmCampaignApprovalRule.validate |
| CM-CAM-0001-E04-C01-D01 | CM.Campaign.delete | OnlineTransactionController.online | CmCampaignHandler.handleDelete | CmCampaignFacade.delete | Read Write | CmCampaignService.delete | CmCampaignDeleteRule.validate |
| CM-CAM-0001-E05-C01-D01 | PD.Product.selectList | OnlineTransactionController.online | PdProductHandler.handleSelectList | PdProductFacade.selectList | Read Only | PdProductService.selectList | PdProductInquiryRule.validateSearch |
| IC-CRD-0001-E01-C01-D01 | IC.Credit.selectResult | OnlineTransactionController.online | IcCreditHandler.handleSelectResult | IcCreditFacade.selectResult | 없음 | IcCreditService.selectResult | IcCreditInquiryRule.validate |

## 7. 매트릭스 C — 프로그램과 DAO·Mapper·DB·외부연계

| 추적 ID | DAO.Method | Mapper Namespace.Method | SQL ID | SQL 유형 | DB 객체·외부 시스템 | CRUD | 개인정보 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001-E00-C01-D01 | SvCustomerDao.selectInitialCodes | SvCustomerMapper.selectInitialCodes | selectInitialCodes | SELECT | OM_COMMON_CODE | R | N |
| SV-CUS-0001-E01-C01-D01 | SvCustomerDao.selectSummary | SvCustomerMapper.selectSummary | selectCustomerSummary | SELECT | SV_CUSTOMER_SUMMARY | R | Y |
| SV-CUS-0001-E01-C01-D02 | SvCustomerDao.selectGrade | SvCustomerMapper.selectGrade | selectCustomerGrade | SELECT | SV_CUSTOMER_GRADE | R | Y |
| SV-CUS-0001-E02-C01-D01 | SvCustomerDao.selectDetail | SvCustomerMapper.selectDetail | selectCustomerDetail | SELECT | SV_CUSTOMER_DETAIL | R | Y |
| SV-CUS-0001-E03-C01-D01 | CtContactDao.selectHistory | CtContactMapper.selectHistory | selectContactHistory | SELECT | CT_CONTACT_HISTORY | R | Y |
| SV-CUS-0001-E04-C01-D01 | SvCustomerDao.selectDownloadData | SvCustomerMapper.selectDownloadData | selectCustomerDownloadData | SELECT | SV_CUSTOMER_SUMMARY | R | Y |
| CM-CAM-0001-E01-C01-D01 | CmCampaignDao.selectList | CmCampaignMapper.selectList | selectCampaignList | SELECT | CM_CAMPAIGN_MASTER | R | N |
| CM-CAM-0001-E02-C01-D01 | CmCampaignDao.insertCampaign | CmCampaignMapper.insertCampaign | insertCampaign | INSERT | CM_CAMPAIGN_MASTER | C | N |
| CM-CAM-0001-E03-C01-D01 | CmCampaignDao.updateApprovalStatus | CmCampaignMapper.updateApprovalStatus | updateCampaignApprovalStatus | UPDATE | CM_CAMPAIGN_MASTER | U | N |
| CM-CAM-0001-E04-C01-D01 | CmCampaignDao.updateDeleteStatus | CmCampaignMapper.updateDeleteStatus | updateCampaignDeleteStatus | UPDATE | CM_CAMPAIGN_MASTER | U | N |
| CM-CAM-0001-E05-C01-D01 | PdProductDao.selectList | PdProductMapper.selectList | selectProductList | SELECT | PD_PRODUCT_MASTER | R | N |
| IC-CRD-0001-E01-C01-D01 | - | - | - | 외부호출 | 신용정보 시스템 / IF-IC-CRD-001 | R | Y |

### 7.1 외부연계 프로그램 보완표

| 추적 ID | 외부 Client.Method | 인터페이스 ID | 프로토콜 | 대상 시스템 | Timeout | 실패 처리 |
| --- | --- | --- | --- | --- | --- | --- |
| IC-CRD-0001-E01-C01-D01 | CreditResultClient.selectResult | IF-IC-CRD-001 | HTTP/JSON | 신용정보 시스템 | 2초 | IC-EAI-001 반환, 화면 오류 표시 |

## 8. 매트릭스 D — 거래통제·화면 응답·감사·테스트

| 추적 ID | Timeout | 중복방지 | 감사 | 권한코드 | 화면 성공 처리 | 화면 실패 처리 | 테스트 ID |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001-E00-C01-D01 | 3초 | N | N | SV_CUSTOMER_VIEW | 검색조건과 공통코드 초기화 | 화면 진입 오류 메시지 | TC-SV-0001-001 |
| SV-CUS-0001-E01-C01-D01 | 3초 | N | Y | SV_CUSTOMER_VIEW | 고객요약 영역 표시 | 조회조건 유지, 오류 표시 | TC-SV-0001-010 |
| SV-CUS-0001-E01-C01-D02 | 3초 | N | Y | SV_CUSTOMER_VIEW | 등급 영역 표시 | 등급 영역 오류 표시 | TC-SV-0001-011 |
| SV-CUS-0001-E02-C01-D01 | 3초 | N | Y | SV_CUSTOMER_DETAIL | 상세 탭 표시 | 선택행 유지, 오류 표시 | TC-SV-0001-020 |
| SV-CUS-0001-E03-C01-D01 | 3초 | N | Y | CT_CONTACT_VIEW | 접촉이력 팝업 표시 | 팝업 미오픈, 오류 표시 | TC-SV-0001-030 |
| SV-CUS-0001-E04-C01-D01 | 10초 | Y | Y | SV_CUSTOMER_DOWNLOAD | 다운로드 파일 전달 | 파일 생성 실패 안내 | TC-SV-0001-040 |
| CM-CAM-0001-E01-C01-D01 | 3초 | N | N | CM_CAMPAIGN_VIEW | 캠페인 그리드 표시 | 조건 유지, 오류 표시 | TC-CM-0001-010 |
| CM-CAM-0001-E02-C01-D01 | 5초 | Y | Y | CM_CAMPAIGN_CREATE | 등록 완료 후 상세 재조회 | 입력값 유지, 오류 표시 | TC-CM-0001-020 |
| CM-CAM-0001-E03-C01-D01 | 5초 | Y | Y | CM_CAMPAIGN_APPROVE | 승인상태로 갱신 | 상태 미변경, 오류 표시 | TC-CM-0001-030 |
| CM-CAM-0001-E04-C01-D01 | 5초 | Y | Y | CM_CAMPAIGN_DELETE | 목록에서 제거 후 재조회 | 삭제상태 유지, 오류 표시 | TC-CM-0001-040 |
| CM-CAM-0001-E05-C01-D01 | 3초 | N | N | PD_PRODUCT_VIEW | 상품선택 팝업 표시 | 팝업 오류 표시 | TC-CM-0001-050 |
| IC-CRD-0001-E01-C01-D01 | 3초 | N | Y | IC_CREDIT_VIEW | 신용실적 결과 표시 | 외부연계 오류 표시 | TC-IC-0001-010 |

고객 상세조회, 고객정보 다운로드, 권한·상태 변경, 대량 추출과 같은 거래는 감사 대상 여부를 매트릭스에 반드시 표시해야 한다. 감사로그에는 GUID, ServiceId, 거래코드, 사용자, 지점, 대상 식별자, 행위 및 결과가 연결되어야 한다.

## 9. 단일 행 완전 전개 예시

| 영역 | 항목 | 내용 |
| --- | --- | --- |
| 관리 | 추적 ID | SV-CUS-0001-E01-C01-D01 |
| 화면 | 화면 ID | SV-CUS-0001 |
| 화면 | 화면명 | 고객 종합정보 조회 |
| 이벤트 | 이벤트 ID | SV-CUS-0001-E01 |
| 이벤트 | 이벤트명 | 고객요약 조회 |
| 이벤트 | UI 객체 | btnSearch |
| 거래 | ServiceId | SV.Customer.selectSummary |
| 거래 | 거래코드 | SV-INQ-0001 |
| 거래 | 처리유형 | 조회 |
| 전문 | 요청 DTO | SvCustomerSummaryRequest |
| 전문 | 응답 DTO | SvCustomerSummaryResponse |
| 공통 | Controller | OnlineTransactionController.online() |
| 공통 | TCF | TCF.process() |
| 공통 | Dispatcher | TransactionDispatcher.dispatch() |
| 업무 | Handler | SvCustomerHandler.handleSelectSummary() |
| 업무 | Facade | SvCustomerFacade.selectSummary() |
| 업무 | Service | SvCustomerService.selectSummary() |
| 업무 | Rule | SvCustomerInquiryRule.validateInquiry() |
| 데이터 | DAO | SvCustomerDao.selectSummary() |
| 데이터 | Mapper | SvCustomerMapper.selectSummary() |
| 데이터 | SQL ID | selectCustomerSummary |
| 데이터 | DB 객체 | SV_CUSTOMER_SUMMARY |
| 통제 | Timeout | 3초 |
| 통제 | 감사 | 대상 |
| 통제 | 권한 | SV_CUSTOMER_VIEW |
| 화면 | 성공 처리 | 고객요약 영역 표시 |
| 화면 | 실패 처리 | 조회조건 유지 후 오류 메시지 |
| 시험 | 테스트 ID | TC-SV-0001-010 |

## 10. 관계 관리 규칙

### 10.1 화면 이벤트와 ServiceId

| 규칙 | 기준 |
| --- | --- |
| 이벤트 없는 ServiceId 금지 | 화면 호출 거래는 반드시 이벤트와 연결 |
| ServiceId 없는 서버 이벤트 금지 | 서버 호출 이벤트는 ServiceId 필수 |
| 다중 호출 분리 | ServiceId별로 행을 분리 |
| 호출 순서 표시 | 순차·병렬·조건부 호출 관계 명시 |
| 재사용 표시 | 여러 화면이 동일 ServiceId를 호출할 수 있음 |
| 화면전용 이름 금지 | ServiceId에 화면번호를 포함하지 않음 |

### 10.2 ServiceId와 Handler

| 규칙 | 기준 |
| --- | --- |
| ServiceId당 Handler | 논리적으로 1개 Handler만 허용 |
| Handler 다중 거래 | 하나의 Handler가 여러 ServiceId 처리 가능 |
| 중복 등록 | 애플리케이션 기동 실패 처리 |
| 미등록 ServiceId | SERVICE_NOT_FOUND 처리 |
| OM 정합성 | 코드의 ServiceId와 OM Catalog가 일치해야 함 |

### 10.3 계층 책임

| 계층 | 허용 책임 | 금지 |
| --- | --- | --- |
| Handler | ServiceId 분기, DTO 변환, Facade 호출 | DB 호출, 트랜잭션, 응답 조립 |
| Facade | 유스케이스 조립, 트랜잭션 경계 | SQL 직접 호출 |
| Service | 업무 처리, 결과 조립, 외부연계 | Controller 역할 |
| Rule | 입력·업무 규칙 검증 | DB·외부호출·데이터 변경 |
| DAO | Mapper 호출, 영속 접근 추상화 | 업무 판단 |
| Mapper | SQL 실행 | 업무 흐름·권한 판단 |

업무 프로그램은 Handler → Facade → Service → Rule → DAO → Mapper 구조를 따르며, 트랜잭션 경계는 Facade에 두는 것이 기준이다.

### 10.4 DAO와 Mapper

- DAO Method와 SQL ID는 원칙적으로 일대일로 관리한다.
- 하나의 DAO Method가 여러 SQL을 호출하면 SQL별 행을 분리한다.
- Mapper XML Namespace는 Mapper Interface 전체명과 일치시킨다.
- SQL ID 변경 시 영향 ServiceId와 화면을 역추적한다.
- SQL 원문이 같아도 업무 의미가 다르면 SQL ID를 분리한다.
- Mapper에서 업무 상태 판단이나 권한 검사를 수행하지 않는다.

## 11. 정상 예시

```
화면 조회 버튼
→ SV.Customer.selectSummary
→ SvCustomerHandler
→ SvCustomerFacade
→ SvCustomerService
→ SvCustomerInquiryRule
→ SvCustomerDao
→ SvCustomerMapper.selectSummary
→ SV_CUSTOMER_SUMMARY
→ ETF 표준응답
→ 화면 요약정보 표시
```

정상 예시는 다음 조건을 모두 충족해야 한다.

- 화면 ID와 이벤트 ID가 존재한다.
- ServiceId와 거래코드가 등록되어 있다.
- Handler가 ServiceId를 선언한다.
- Facade에서 트랜잭션 경계를 관리한다.
- Service와 Rule의 책임이 분리되어 있다.
- DAO·Mapper·SQL ID·테이블이 연결되어 있다.
- Timeout, 권한, 감사 여부가 정의되어 있다.
- 테스트 케이스가 연결되어 있다.

## 12. 금지 예시

### 12.1 업무 Controller 직접 호출

```
SV 화면
→ SvCustomerController
→ SvCustomerService
→ Mapper
```

금지 사유:

- TCF 거래통제를 우회한다.
- ServiceId 기반 추적이 끊긴다.
- Timeout과 거래로그가 누락될 수 있다.
- 화면과 프로그램의 표준 매핑이 불가능하다.

### 12.2 Service에서 Mapper 직접 호출

```
Handler
→ Service
→ Mapper
```

금지 사유:

- DAO 책임이 사라진다.
- 데이터 접근 변경 영향 분석이 어려워진다.
- SQL 실행 책임이 업무 처리와 혼합된다.

### 12.3 하나의 셀에 여러 프로그램 기록

```
DAO:
CustomerDao, GradeDao, ContactDao
```

금지 사유:

- 프로그램별 호출순서와 SQL 관계를 알 수 없다.
- 변경 영향 분석이 불가능하다.
수정 방법:

```
DAO·SQL별로 행을 분리하고 데이터 순번 D01, D02, D03 부여
```

### 12.4 물리 삭제를 무조건 DELETE로 표현

캠페인 삭제가 실제로 상태값을 변경하는 논리 삭제라면 다음처럼 기록한다.

```
화면 기능: 삭제
처리유형: 삭제
SQL 유형: UPDATE
SQL ID: updateCampaignDeleteStatus
CRUD: U
```

화면 행위와 물리 SQL 유형을 동일한 것으로 간주하면 안 된다.

## 13. 책임 경계와 RACI

| 활동 | 업무팀 | UI팀 | 프레임워크팀 | 아키텍처팀 | DA·DBA | 테스트팀 | 운영팀 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 화면·이벤트 정의 | A | R | C | C | I | C | I |
| ServiceId 정의 | R | C | C | A | I | C | I |
| Handler·Facade 설계 | R | I | C | A | I | C | I |
| Rule·Service 설계 | R | I | C | A | C | C | I |
| DAO·Mapper 설계 | R | I | C | C | A | C | I |
| DB 객체 연결 | C | I | I | C | A/R | C | I |
| 감사·권한 정의 | R | C | C | A | I | C | C |
| 테스트 연결 | C | C | C | C | C | A/R | I |
| 운영 기준정보 등록 | R | I | C | A | I | C | R |
| 변경 영향 검토 | R | C | C | A | C | C | C |

R: 수행, A: 최종 책임, C: 협의, I: 통보

## 14. 자동검증 및 품질 Gate

### 14.1 정합성 자동검증

| 검증 ID | 검증 내용 | 실패 기준 |
| --- | --- | --- |
| TRC-001 | 화면 서버호출 이벤트에 ServiceId 존재 | ServiceId 공란 |
| TRC-002 | ServiceId가 Handler에 등록 | 미등록 |
| TRC-003 | ServiceId 중복 등록 없음 | 2개 이상 Handler 등록 |
| TRC-004 | OM Service Catalog 등록 | OM 미등록 |
| TRC-005 | 거래코드 등록 | 거래코드 공란·미등록 |
| TRC-006 | Handler에서 Facade 호출 | Service·DAO 직접 호출 |
| TRC-007 | Facade 트랜잭션 기준 준수 | Handler·Rule에 @Transactional |
| TRC-008 | DAO와 Mapper 연결 | Mapper 없는 DAO |
| TRC-009 | Mapper Method와 SQL ID 일치 | XML Statement 미존재 |
| TRC-010 | SQL 대상 객체 등록 | DB 객체 미식별 |
| TRC-011 | 변경 거래 중복방지 정의 | 등록·변경인데 미정의 |
| TRC-012 | 중요 거래 감사 여부 정의 | 감사 여부 공란 |
| TRC-013 | 테스트 케이스 연결 | 테스트 ID 공란 |
| TRC-014 | 폐기 프로그램 참조 없음 | 폐기 ServiceId를 화면에서 호출 |
| TRC-015 | 개인정보 표시 기준 존재 | 마스킹 여부 미정의 |

### 14.2 CI/CD 적용

```
화면 이벤트 정의 추출
  ↓
ServiceId 목록 추출
  ↓
Handler Registry와 비교
  ↓
OM Catalog와 비교
  ↓
Mapper Interface·XML 비교
  ↓
DB 객체 목록 비교
  ↓
추적성 누락 보고서 생성
  ↓
Critical 누락 시 빌드·배포 차단
```

### 14.3 품질 Gate 통과 기준

| 등급 | 기준 |
| --- | --- |
| Blocker | 화면 호출 ServiceId 미등록, Handler 중복, Mapper SQL 미존재 |
| Critical | 거래코드·권한·감사·Timeout 누락 |
| Major | 테스트 케이스·담당자·버전 누락 |
| Minor | 설명·비고·화면 성공 메시지 미보완 |

운영 반영 전 기준:

```
Blocker 0건
Critical 0건
Major 보완계획 100%
화면 이벤트–ServiceId 연결률 100%
ServiceId–Handler 연결률 100%
DAO–Mapper–SQL 연결률 100%
변경 거래 테스트 연결률 100%
```

## 15. 체크리스트

| No. | 점검 항목 | 확인 |
| --- | --- | --- |
| 1 | 모든 화면에 화면 ID가 부여되었는가 | □ |
| 2 | 서버 호출 이벤트에 이벤트 ID가 있는가 | □ |
| 3 | 이벤트별 ServiceId 호출순서가 정의되었는가 | □ |
| 4 | 조건부·병렬 호출이 구분되어 있는가 | □ |
| 5 | ServiceId와 거래코드가 일치하는가 | □ |
| 6 | ServiceId당 Handler가 하나인가 | □ |
| 7 | Handler가 Facade만 호출하는가 | □ |
| 8 | 트랜잭션 경계가 Facade에 있는가 | □ |
| 9 | Rule에서 DB와 외부호출을 하지 않는가 | □ |
| 10 | DAO와 Mapper Method가 연결되었는가 | □ |
| 11 | Mapper Method와 XML SQL ID가 일치하는가 | □ |
| 12 | SQL 대상 테이블·뷰가 식별되었는가 | □ |
| 13 | 개인정보와 마스킹 여부가 정의되었는가 | □ |
| 14 | 다운로드·변경 거래가 감사 대상인지 검토했는가 | □ |
| 15 | 등록·변경·삭제 거래에 중복방지가 정의되었는가 | □ |
| 16 | ServiceId별 Timeout이 등록되었는가 | □ |
| 17 | 화면 성공·실패 처리가 정의되었는가 | □ |
| 18 | 테스트 케이스가 연결되었는가 | □ |
| 19 | OM Catalog와 코드가 일치하는가 | □ |
| 20 | 폐기된 프로그램을 참조하는 화면이 없는가 | □ |

## 16. 변경·호환성·폐기 관리

### 16.1 화면 이벤트 변경

화면 버튼명이나 UI 객체 ID만 변경되고 업무 기능이 동일하면 ServiceId는 유지할 수 있다.

```
btnSearch → btnCustomerSearch
ServiceId 유지:
SV.Customer.selectSummary
```

### 16.2 ServiceId 변경

ServiceId 변경은 다음 항목을 모두 변경해야 한다.

- 화면 이벤트 정의
- UI 호출 소스
- Handler Registry
- OM Service Catalog
- 거래통제 정책
- Timeout 정책
- 권한정보
- 거래로그·감사로그 검색기준
- 테스트 케이스
- Gateway 또는 내부 라우팅 설정
단순 Java 메서드명 변경보다 영향 범위가 크므로 ServiceId는 가급적 변경하지 않는다.

### 16.3 SQL·테이블 변경

SQL 또는 테이블 변경 전 다음 역추적을 수행한다.

```
DB 객체
  ↓
SQL ID
  ↓
Mapper Method
  ↓
DAO Method
  ↓
Service
  ↓
ServiceId
  ↓
화면 이벤트
  ↓
영향 화면과 테스트
```

### 16.4 폐기

폐기 순서는 다음과 같다.

```
화면 이벤트 사용 중지
→ 신규 호출 차단
→ OM Service Catalog 폐기예정
→ 소스 참조 여부 확인
→ Handler·ServiceId 제거
→ DAO·Mapper 미사용 확인
→ SQL·DB 객체 영향 확인
→ 운영로그 보존기간 경과 후 최종 폐기
```

## 17. 시사점

### 17.1 핵심 아키텍처 판단

화면 이벤트와 Java 클래스만 연결하는 것으로는 충분하지 않다.

최소한 다음 연결이 완성되어야 한다.

```
화면 이벤트
↔ ServiceId
↔ 거래코드
↔ Handler
↔ Facade
↔ Service
↔ Rule
↔ DAO
↔ Mapper SQL
↔ DB 객체·외부 시스템
↔ 권한·감사·Timeout
↔ 테스트 케이스
```

### 17.2 주요 위험

- 화면 이벤트에는 거래가 있으나 ServiceId가 등록되지 않은 경우
- 코드에는 ServiceId가 있으나 OM Catalog에 없는 경우
- Handler가 Service나 DAO를 직접 호출하는 경우
- Mapper SQL이 어떤 화면에서 사용되는지 알 수 없는 경우
- 다운로드와 개인정보 조회가 감사로그에 연결되지 않은 경우
- ServiceId 변경 후 화면과 테스트가 과거 값을 계속 사용하는 경우

### 17.3 우선 보완 과제

- 전체 화면 목록과 화면 이벤트 목록을 확정한다.
- 모든 서버 호출 이벤트에 ServiceId를 연결한다.
- 소스에서 Handler·Facade·Service·DAO·Mapper 관계를 추출한다.
- Mapper XML에서 SQL ID와 DB 객체를 추출한다.
- 화면 정의와 소스 추출 결과를 비교한다.
- 누락·불일치 목록을 품질 Gate로 관리한다.

### 17.4 중장기 발전 방향

최종적으로 추적성 매트릭스는 사람이 수작업으로만 관리하는 Excel이 아니라 다음 원천에서 자동 생성되어야 한다.

```
화면 이벤트 정의
+ Java Annotation·Handler Registry
+ Spring Bean 의존관계
+ MyBatis Mapper XML
+ DB 객체 목록
+ OM Service Catalog
+ 테스트 결과
= 자동 추적성 매트릭스
```

## 18. 마무리말

화면 이벤트–ServiceId–프로그램 전체 추적성 매트릭스는 화면설계서와 프로그램설계서 사이에 추가되는 보조표가 아니다.

이 매트릭스는 다음 산출물을 연결하는 중심 기준정보다.

```
화면설계서
→ 거래설계서
→ 프로그램설계서
→ SQL·DB 설계서
→ 테스트설계서
→ 운영 Service Catalog
→ 변경 영향 분석
```

따라서 프로젝트의 기준 산출물은 화면 단위 프로그램 목록이 아니라, 화면 이벤트에서 데이터와 운영 통제까지 연결되는 End-to-End 추적성 매트릭스로 관리해야 한다.

