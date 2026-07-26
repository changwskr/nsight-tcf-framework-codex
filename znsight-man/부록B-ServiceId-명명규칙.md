# 부록 B. ServiceId 명명 규칙

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## B. ServiceId 명명 규칙

### B.1 도입 전 안내말

본 부록은 NSIGHT TCF Framework에서 사용하는 ServiceId 명명 규칙을 정의한다.
ServiceId는 단순한 프로그램명이 아니다. NSIGHT에서는 ServiceId가 온라인 거래 실행, Handler 선택, 거래통제, 권한검증, Timeout 정책, 거래로그, 감사로그, 오류 추적의 핵심 기준이 된다. 기존 Service ID 설계에서도 Service ID는 TransactionDispatcher가 어떤 Handler를 실행할지 결정하는 Key이며, Header 검증, 권한, 거래로그, 감사로그, Timeout, 중복요청 통제의 기준으로 정의되어 있다.
```text
StandardRequest.header.serviceId
        ↓
STF 전처리 검증
        ↓
ServiceId Catalog 조회
        ↓
```

거래통제 / 권한 / Timeout 정책 확인
```text
        ↓
TransactionDispatcher
        ↓
```

TransactionHandler 선택
```text
        ↓
```

Handler → Facade → Service → Rule → DAO/Mapper
```text
        ↓
```

ETF 표준 응답 / 거래로그 / 감사로그

### B.2 ServiceId 기본 형식

ServiceId의 표준 형식은 다음과 같다.
{업무코드}.{업무대상}.{처리행위}

| 구성요소 | 표기 방식 | 설명 |
| --- | --- | --- |
| 예시 | 업무코드 | 대문자 2자리 |
| 업무 WAR / Context 기준 | SV, CM, OM, MG | 업무대상 |
| PascalCase | 처리 대상 도메인 또는 기능 영역 | Customer, Campaign, User, Message |
| 처리행위 | lowerCamelCase | 실제 수행 기능 |
예시는 다음과 같다.

| selectSummary, selectList, save, delete | SV.Customer.selectSummary |
| CM.Campaign.selectList | MG.Message.send | OM.User.inquiry |

OM.ServiceCatalog.save

기존 Service ID 설계에서도 권장 형식을 {업무코드}.{업무대상}.{처리행위}로 정의하고, SV.Customer.selectSummary, CM.Campaign.create, MG.Message.send, OM.ServiceCatalog.inquiry 등을 예시로 제시한다.

### B.3 ServiceId 명명 원칙

| 원칙 | 기준 | 예시 |
| --- | --- | --- |
| 업무코드로 시작 | 첫 번째 구간은 반드시 업무코드 표준표의 업무코드 사용 | SV.Customer.selectSummary |
| URL과 혼동 금지 | URL은 /sv/online, 실행 기준은 serviceId | /sv/online + SV.Customer.selectSummary |
| 업무대상 명확화 | 두 번째 구간은 업무 객체 또는 관리 대상 | Customer, Campaign, ServiceCatalog |
| 행위 표준화 | 세 번째 구간은 표준 처리행위 사용 | selectList, save, execute |
| 약어 남용 금지 | 의미 불명확한 축약어 금지 | custSel, proc, svc 금지 |
| Handler와 연결 | ServiceId는 하나의 Handler와 매핑되어야 함 | SvCustomerSummaryHandler |
| 거래코드와 연결 | ServiceId는 대표 거래코드와 매핑되어야 함 | SV-INQ-0001 |
| 운영관리 등록 | 코드에만 두지 않고 OM Service Catalog에 등록 | OM_SERVICE_CATALOG |

명명 규칙 문서도 이름 하나가 업무코드, Context, WAR, Package, ServiceId, Handler, Mapper XML, SQL ID, 거래로그, 오류코드, 감사로그까지 연결되어야 한다고 정의한다.

### B.4 처리행위 표준

ServiceId의 마지막 구간인 처리행위는 다음 표준을 사용한다.
| 처리유형 | 표준 Action | 사용 기준 | 예시 |
| --- | --- | --- | --- |
| 목록 조회 | selectList | 목록, 조건검색 | CM.Campaign.selectList |
| 단건 조회 | selectDetail | 상세조회 | PD.Product.selectDetail |
| 요약 조회 | selectSummary | 요약정보 조회 | SV.Customer.selectSummary |
| 등록 | create | 신규 생성 | CM.Campaign.create |
| 수정 | update | 기존 데이터 수정 | OM.User.update |
| 삭제 | delete | 논리삭제 또는 삭제 | OM.ErrorCode.delete |
| 저장 | save | 등록/수정 통합 | OM.ServiceCatalog.save |
| 발송 | send | 메시지, 알림 발송 | MG.Message.send |
| 실행 | execute | 배치, 작업, 룰 실행 | OM.Batch.execute |
| 검증 | verify | 토큰, 권한, 규칙 검증 | JWT.Auth.verify |
| 발급 | issue | 토큰, 번호 발급 | JWT.Auth.issue |
| 다운로드 | download | 파일, 엑셀 다운로드 | UD.File.download |
| 업로드 | upload | 파일 업로드 | UD.File.upload |
| 재처리 | retry | 실패 거래 재처리 | OM.Batch.retry |
| 중지 | stop | 배치, 거래 중지 | OM.Batch.stop |

기존 Service ID 설계에서도 inquiry, select, save, update, delete, send, download, execute 등을 처리행위 표준으로 제시한다.

### B.5 조회성 ServiceId 기준

조회성 ServiceId는 조회 성격을 구분해서 작성한다.

| 조회 유형 | Action | 사용 예 |
| --- | --- | --- |
| 조건 목록 조회 | selectList | CM.Campaign.selectList |

| 단건 상세 조회 | selectDetail |
| --- | --- |
| SV.Customer.selectDetail | 요약 조회 |
| selectSummary | SV.Customer.selectSummary |
| 코드/기준정보 조회 | inquiry |

OM.ServiceCatalog.inquiry

| 건수 조회 | count |
| --- | --- |
| SV.Customer.count | 존재 여부 확인 |
| exists | OM.User.exists |

조회성 거래 예시는 다음과 같다.
| 업무 | ServiceId |
| --- | --- |
| 거래코드 | 설명 |
| SV | SV.Customer.selectSummary |
| SV-INQ-0001 | 고객 요약 조회 |
| SV | SV.Customer.selectDetail |
| SV-INQ-0002 | 고객 상세 조회 |
| CM | CM.Campaign.selectList |
| CM-INQ-0001 | 캠페인 목록 조회 |
| OM | OM.User.inquiry |
| OM-INQ-0001 | 사용자 조회 |
| OM | OM.ServiceCatalog.inquiry |
| OM-INQ-0002 | ServiceId 목록 조회 |

### B.6 변경성 ServiceId 기준

등록, 수정, 삭제, 저장은 명확하게 구분한다.
| 처리 유형 | Action | 사용 기준 |
| --- | --- | --- |
| 예시 | 신규 등록 | create |
| 신규 데이터만 생성 | CM.Campaign.create | 수정 |
| update | 기존 데이터 변경 | CM.Campaign.update |
| 삭제 | delete | 삭제 또는 사용중지 |
| OM.User.delete | 저장 | save |
기준은 다음과 같다.

| 등록/수정을 통합 처리 | OM.ServiceCatalog.save |
| create = 신규 등록 | update = 기존 수정 | delete = 삭제 / 사용중지 |

save   = 등록과 수정을 한 화면에서 통합 처리할 때만 사용

save를 무분별하게 사용하면 거래로그에서 등록인지 수정인지 구분하기 어렵다. 따라서 업무적으로 등록과 수정이 분명히 나뉘는 경우에는 create, update를 사용한다.

### B.7 운영관리 ServiceId 기준

OM 업무는 운영관리 대상별로 ServiceId를 작성한다.

| 관리 대상 | ServiceId 예시 | 설명 |
| --- | --- | --- |
| 로그인 | OM.Auth.login | OM 로그인 |

SSO 로그인
OM.Auth.ssoLogin

| SSO 인증 로그인 | 대시보드 |
| --- | --- |
| OM.Dashboard.inquiry | 운영 대시보드 조회 |
| 사용자 | OM.User.inquiry |
| 사용자 목록/상세 조회 | 사용자 저장 |
| OM.User.save | 사용자 등록/수정 |

권한그룹
OM.AuthGroup.inquiry

| 권한그룹 조회 | 메뉴 |
| --- | --- |
| OM.Menu.inquiry | 메뉴 조회 |

기능권한
OM.FunctionAuth.save

| 기능권한 저장 | ServiceId |
| --- | --- |
| OM.ServiceCatalog.inquiry | ServiceId 조회 |
| ServiceId 저장 | OM.ServiceCatalog.save |
| ServiceId 등록/수정 | 거래통제 |

OM.TransactionControl.save

| 거래통제 저장 | Timeout |
| --- | --- |
| OM.TimeoutPolicy.save | Timeout 정책 저장 |

오류코드
OM.ErrorCode.save

| 오류코드 저장 | 공통코드 |
| --- | --- |
| OM.CommonCode.save | 공통코드 저장 |

배치
OM.Batch.execute

| 배치 수동 실행 | 파일 |
| --- | --- |
| OM.File.inquiry | 파일 메타 조회 |

OM 관리 화면도 ServiceId 관리, 거래로그, 사용자/권한/메뉴, 오류코드, Cache, 세션, 배치/배포, 파일관리, 환경설정 등을 포함하는 구조로 정의되어 있다.

### B.8 ServiceId와 TransactionCode 관계

ServiceId와 TransactionCode는 둘 다 필요하다.
| 구분 | 역할 | 예시 |
| --- | --- | --- |
| businessCode | 업무 WAR / Context 식별 | SV |
| serviceId | 실제 실행 Handler 식별 | SV.Customer.selectSummary |
| transactionCode | 거래로그, 감사, 재처리 기준 | SV-INQ-0001 |

권장 관계는 다음과 같다.
businessCode + transactionCode
= 업무 거래 식별

serviceId
= 실행 Handler 식별

serviceId + transactionCode
= 실행 프로그램과 거래로그 정합성 검증

기존 Service ID 설계에서도 serviceId는 실제 실행할 업무 Handler 식별자이고, transactionCode는 거래로그·전문·재처리·감사 기준의 거래 식별자이며, businessCode는 업무 WAR/Context 식별자로 구분한다.

### B.9 ServiceId와 Handler 명명 매핑

ServiceId와 Handler Class는 규칙적으로 연결한다.
ServiceId:
SV.Customer.selectSummary

Handler:
SvCustomerSummaryHandler

| ServiceId | Handler Bean | Handler Class |
| --- | --- | --- |
| SV.Customer.selectSummary | svCustomerSummaryHandler | SvCustomerSummaryHandler |
| SV.Customer.selectDetail | svCustomerDetailHandler | SvCustomerDetailHandler |
| CM.Campaign.selectList | cmCampaignListHandler | CmCampaignListHandler |
| MG.Message.send | mgMessageSendHandler | MgMessageSendHandler |
| OM.User.inquiry | omUserInquiryHandler | OmUserInquiryHandler |
| OM.ServiceCatalog.save | omServiceCatalogSaveHandler | OmServiceCatalogSaveHandler |

Dispatcher는 요청마다 Bean을 검색하는 것이 아니라, 애플리케이션 기동 시점에 serviceId → Handler 매핑 정보를 Registry로 구성하고, 동일 ServiceId 중복 등록은 기동 실패로 처리해야 한다.

### B.10 ServiceId Catalog 등록 기준

ServiceId는 코드에만 존재하면 안 된다. 운영관리와 거래통제를 위해 반드시 Catalog에 등록한다.
| 항목 | 설명 |
| --- | --- |
| 예시 | SERVICE_ID |
| 서비스 ID | SV.Customer.selectSummary |
| BUSINESS_CODE | 업무코드 |
| SV | TRANSACTION_CODE |
| 거래코드 | SV-INQ-0001 |
| SERVICE_NAME | 서비스명 |
| 고객 요약 조회 | PROCESSING_TYPE |
| 처리유형 | INQUIRY |
| MODULE_NAME | 모듈명 |
| sv-service | CONTEXT_PATH |
| Context | /sv |
| HANDLER_BEAN_NAME | Handler Bean |
| svCustomerSummaryHandler | SESSION_REQUIRED_YN |
| 세션 필요 여부 | Y |
| AUTH_REQUIRED_YN | 권한 필요 여부 |
| Y | AUDIT_REQUIRED_YN |
| 감사 여부 | Y |
| DEFAULT_TIMEOUT_SEC | 기본 Timeout |
| 3 | USE_YN |
| 사용 여부 | Y |
| SERVICE_STATUS | 상태 |
| ACTIVE | OWNER_TEAM |
| 담당팀 | SV업무팀 |

Service ID 관리는 Service ID Master, Handler Registry, 권한 정책, Timeout 정책, 감사 정책, 거래통제와 연결되어야 한다.

### B.11 업무별 ServiceId 예시

| 업무코드 | ServiceId | 거래코드 |
| --- | --- | --- |
| 설명 | CC | CC.Code.selectList |
| CC-INQ-0001 | 공통코드 목록 조회 | IC |
| IC.Customer.selectIntegration | IC-INQ-0001 | 통합고객 조회 |
| PC | PC.Customer.selectPrivate | PC-INQ-0001 |
| 개인고객 조회 | BC | BC.Customer.selectBusiness |
| BC-INQ-0001 | 기업고객 조회 | MS |
| MS.Customer.selectSummary | MS-INQ-0001 | 미니 싱글뷰 조회 |
| SV | SV.Customer.selectSummary | SV-INQ-0001 |
| Single View 고객요약 조회 | PD | PD.Product.selectDetail |
| PD-INQ-0001 | 상품 상세 조회 | CM |
| CM.Campaign.selectList | CM-INQ-0001 | 캠페인 목록 조회 |
| EB | EB.Rule.checkEvent | EB-CHK-0001 |
| 이벤트 룰 검증 | EP | EP.Event.process |
| EP-EXE-0001 | 이벤트 처리 | BP |
| BP.Behavior.process | BP-EXE-0001 | 행동정보 처리 |
| BD | BD.Behavior.selectData | BD-INQ-0001 |
| 행동 데이터 조회 | SS | SS.Sales.selectSupport |
| SS-INQ-0001 | 영업지원 조회 | CS |
| CS.Common.selectService | CS-INQ-0001 | 공통서비스 조회 |
| CT | CT.Contents.selectList | CT-INQ-0001 |
| 콘텐츠 목록 조회 | MG | MG.Message.send |
| MG-EXE-0001 | 메시지 발송 | OM |
| OM.User.inquiry | OM-INQ-0001 | 운영 사용자 조회 |

### B.12 잘못된 ServiceId 예시

| 잘못된 ServiceId | 문제 | 표준 예시 |
| --- | --- | --- |
| selectCustomer | 업무코드 없음 | SV.Customer.selectDetail |
| SV.selectCustomer | 업무대상 구간 불명확 | SV.Customer.selectDetail |
| SV.Customer.get | 행위가 모호함 | SV.Customer.selectDetail |
| SV.Cust.selSum | 약어 남용 | SV.Customer.selectSummary |
| singleview.customer.summary | 업무코드 표준 위반 | SV.Customer.selectSummary |
| SV.Customer.select_summary | snake_case 사용 | SV.Customer.selectSummary |
| SV.Customer.SelectSummary | Action 대문자 시작 | SV.Customer.selectSummary |
| OM.User.manage | 행위가 불명확 | OM.User.inquiry, OM.User.save |
| CM.Campaign.proc | 처리 의미 불명확 | CM.Campaign.execute |
| MG.SendMessage.send | 업무대상·행위 중복 | MG.Message.send |

### B.13 ServiceId 검증 기준

STF 전처리와 Dispatcher는 다음 항목을 검증해야 한다.
| 검증 항목 | 기준 |
| --- | --- |
| 오류 시 처리 | 필수값 |
| serviceId는 필수 | E-TCF-DSP-0001 |
| 형식 | {업무코드}.{업무대상}.{처리행위} |
| ServiceId 형식 오류 | 업무코드 |
| Prefix가 businessCode와 일치 | 업무코드 불일치 오류 |
| Catalog 등록 | OM_SERVICE_CATALOG 등록 여부 |
| 미등록 ServiceId 차단 | 사용 여부 |
| USE_YN = Y | 사용중지 서비스 차단 |
| 상태 | ACTIVE만 실행 |
| SUSPENDED, DEPRECATED 차단 | Handler 존재 |
| Registry에 Handler 존재 | Handler 미존재 오류 |
| 거래코드 매핑 | Header transactionCode와 Catalog 매핑 일치 |
| 거래코드 불일치 오류 | 권한 |
| 사용자에게 ServiceId 실행 권한 존재 | 권한 오류 |
| Timeout | ServiceId별 Timeout 정책 존재 |

기본값 또는 정책 오류
Dispatcher 구조에서도 serviceId 필수값 확인, businessCode와 ServiceId Prefix 일치 여부 확인, Service Catalog 조회, 사용 여부 확인, Handler Registry 조회, ETF 응답 조립 흐름을 처리 알고리즘으로 정의한다.

### B.14 개발 시 준수사항

| 구분 | 준수사항 |
| --- | --- |
OM Service Catalog에 먼저 등록한다.

| 신규 ServiceId | |
Handler의 serviceId() 반환값과 Catalog의 SERVICE_ID를 일치시킨다.

| Handler 구현 | |
ServiceId Prefix와 Header businessCode를 일치시킨다.

| 업무코드 | |
대표 transactionCode를 반드시 매핑한다.

| 거래코드 | |
조회, 변경, 외부연계 여부에 따라 Timeout을 등록한다.

| Timeout | |
고객정보, 다운로드, 관리자 기능은 감사 대상으로 등록한다.

| 감사 여부 | |
ServiceId 단위 권한 정책과 연결한다.

| 권한 | |
미등록 ServiceId, 중복 ServiceId, Handler 없음 케이스를 테스트한다.

| 테스트 | |
거래로그에 ServiceId, transactionCode, businessCode, GUID를 함께 남긴다.

| 로그 | |
사용하지 않는 ServiceId는 삭제보다 SUSPENDED 또는 DEPRECATED 상태로 관리한다.

| 폐기 | |

### B.15 최종 정리

ServiceId 명명 규칙의 핵심은 다음이다.
업무코드
  → ServiceId
  → Handler
  → 거래코드
  → 거래통제
  → Timeout
  → 거래로그
  → 감사로그

따라서 NSIGHT TCF Framework에서 ServiceId는 다음 한 문장으로 정의할 수 있다.
ServiceId는 URL이 아니라 실제 업무 Handler를 실행하는 기준이며, 운영자가 거래를 통제하고 추적하는 최상위 실행 식별자이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (73).docx`

| [applicationNaming.md](../zdoc/applicationNaming.md) |
| [07-ServiceIdDispatcher.md](../zman/07-ServiceIdDispatcher.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)