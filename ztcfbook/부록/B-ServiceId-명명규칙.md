# 부록 B. ServiceId 명명규칙 요약

| 항목 | 내용 |
| --- | --- |
| **부록** | B |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## B.1 ServiceId의 역할

ServiceId는 NSIGHT TCF에서 **실제 업무 Handler를 실행하는 최상위 식별자**이다. URL(`/sv/online`)은 업무 WAR 진입점일 뿐이며, 어떤 기능을 실행할지는 `StandardRequest.header.serviceId`가 결정한다. TransactionDispatcher는 기동 시점에 `serviceId → Handler` Registry를 구성하고, 요청마다 이 맵으로 Bean을 조회한다.

ServiceId는 거래통제, 권한 검증, Timeout 정책, 거래로그, 감사로그, 오류 추적의 공통 키이다. OM Service Catalog(`OM_SERVICE_CATALOG`)에 등록되지 않은 ServiceId는 운영 환경에서 실행이 차단된다. 개발자가 Handler만 작성하고 Catalog 등록을 누락하면 로컬에서는 동작해도 통합·운영 환경에서 `E-TCF-DSP-0001`류 오류가 발생한다.

```text
StandardRequest.header.serviceId
        ↓
STF 전처리 (형식·필수값·businessCode 정합성)
        ↓
ServiceId Catalog 조회 (USE_YN, ACTIVE, 권한, Timeout)
        ↓
TransactionDispatcher → Handler Registry
        ↓
Handler → Facade → Service → Rule → DAO/Mapper
        ↓
ETF → StandardResponse / 거래로그 / 감사로그
```

ServiceId와 TransactionCode는 역할이 다르다. ServiceId는 **어떤 프로그램(Handler)을 실행할지**, TransactionCode는 **어떤 거래로 기록·감사·재처리할지**를 식별한다. 둘 다 Header에 필수이며, Catalog에서 서로 매핑되어야 한다.

---

## B.2 ServiceId 기본 형식

표준 형식은 세 구간의 점(`.`) 구분 문자열이다.

```text
{업무코드}.{업무대상}.{처리행위}
```

| 구성요소 | 표기 | 설명 | 예시 |
| --- | --- | --- | --- |
| 업무코드 | 대문자 2~3자리 | 부록 A 업무코드 표준표 | SV, CM, OM, MG |
| 업무대상 | PascalCase | 도메인·관리 대상 | Customer, Campaign, User, Message |
| 처리행위 | lowerCamelCase | 수행 기능(표준 Action) | selectSummary, create, send, inquiry |

**표준 예시**

| ServiceId | 설명 |
| --- | --- |
| SV.Customer.selectSummary | SV 고객 요약 조회 |
| CM.Campaign.selectList | 캠페인 목록 조회 |
| CM.Campaign.create | 캠페인 신규 등록 |
| MG.Message.send | 메시지 발송 |
| OM.User.inquiry | 운영 사용자 조회 |
| OM.ServiceCatalog.save | ServiceId Catalog 저장 |

구간 수는 정확히 3개이다. `SV.Customer`처럼 두 구간만 쓰거나 `SV.Customer.Summary.select`처럼 네 구간 이상을 쓰지 않는다. 업무대상이 복합 개념이면 `CustomerAccount`처럼 PascalCase 한 덩어리로 표현한다.

---

## B.3 ServiceId 명명 원칙

| 원칙 | 기준 | 올바른 예 | 잘못된 예 |
| --- | --- | --- | --- |
| 업무코드로 시작 | 첫 구간 = 표준 업무코드 | SV.Customer.selectSummary | selectCustomer |
| URL과 혼동 금지 | 실행 기준은 serviceId | SV.Customer.selectDetail | /sv/customer/detail |
| 업무대상 명확화 | 두 번째 구간 = 업무 객체 | Customer, ServiceCatalog | SV.selectCustomer |
| 행위 표준화 | 세 번째 구간 = 표준 Action | selectList, create, inquiry | get, proc, manage |
| 약어 남용 금지 | 의미 불명확 축약 금지 | SV.Customer.selectSummary | SV.Cust.selSum |
| Handler 1:1 | ServiceId당 Handler 하나 | SvCustomerSummaryHandler | 동일 ID 중복 Bean |
| 거래코드 연결 | 대표 transactionCode 매핑 | SV-INQ-0001 | Catalog 매핑 없음 |
| Catalog 등록 | OM_SERVICE_CATALOG 필수 | ACTIVE, USE_YN=Y | 코드에만 존재 |

`businessCode` Header와 ServiceId 접두어는 반드시 일치한다. `businessCode: SV`인데 `serviceId: CM.Campaign.selectList`이면 STF에서 업무코드 불일치 오류를 반환한다. Gateway가 업무 WAR로 라우팅할 때도 Path의 업무코드와 Header를 대조한다.

---

## B.4 처리행위(Action) 표준

세 번째 구간에 쓸 수 있는 표준 Action 목록이다. 신규 Action이 필요하면 프레임워크·OM 담당자와 협의해 표준에 추가한 뒤 사용한다.

| 처리유형 | 표준 Action | 사용 기준 | ServiceId 예시 |
| --- | --- | --- | --- |
| 목록 조회 | selectList | 조건 검색·목록 | CM.Campaign.selectList |
| 단건 조회 | selectDetail | 상세 단건 | PD.Product.selectDetail |
| 요약 조회 | selectSummary | 요약·카드형 정보 | SV.Customer.selectSummary |
| 기준정보 조회 | inquiry | 코드·Catalog·관리 화면 | OM.ServiceCatalog.inquiry |
| 건수 조회 | count | 건수만 | SV.Customer.count |
| 존재 확인 | exists | 존재 여부 | OM.User.exists |
| 신규 등록 | create | 신규 생성만 | CM.Campaign.create |
| 수정 | update | 기존 데이터 변경 | OM.User.update |
| 삭제 | delete | 논리삭제·물리삭제 | OM.ErrorCode.delete |
| 통합 저장 | save | 등록·수정 통합 화면 | OM.ServiceCatalog.save |
| 발송 | send | 메시지·알림 | MG.Message.send |
| 실행 | execute | 배치·룰·캠페인 실행 | OM.Batch.execute |
| 검증 | verify | 토큰·권한·룰 | JWT.Auth.verify |
| 발급 | issue | 토큰·번호 발급 | JWT.Auth.issue |
| 다운로드 | download | 파일·엑셀 | UD.File.download |
| 업로드 | upload | 파일 업로드 | UD.File.upload |
| 재처리 | retry | 실패 거래 재시도 | OM.Batch.retry |
| 중지 | stop | 배치·작업 중지 | OM.Batch.stop |

`inquiry`는 OM·관리성 조회에, `selectList`·`selectDetail`은 업무 도메인 조회에 주로 쓴다. `save`는 등록·수정이 한 화면에서 구분되지 않을 때만 사용하고, 분리 가능하면 `create`와 `update`를 쓴다. 그래야 거래로그에서 변경 유형을 transactionCode(CRT/UPD)와 함께 분석할 수 있다.

---

## B.5 조회성 vs 변경성 ServiceId

### 조회성

조회 거래는 `processingType: INQUIRY`와 거래유형 `INQ`를 사용한다. Action으로 조회 성격을 구분한다.

| 조회 유형 | Action | ServiceId 예시 | 거래코드 예시 |
| --- | --- | --- | --- |
| 조건 목록 | selectList | CM.Campaign.selectList | CM-INQ-0001 |
| 단건 상세 | selectDetail | SV.Customer.selectDetail | SV-INQ-0002 |
| 요약 | selectSummary | SV.Customer.selectSummary | SV-INQ-0001 |
| 기준정보 | inquiry | OM.ServiceCatalog.inquiry | OM-INQ-0002 |
| 건수 | count | SV.Customer.count | SV-INQ-0003 |
| 존재 여부 | exists | OM.User.exists | OM-INQ-0004 |

조회 거래도 권한·감사 대상일 수 있다. 고객정보 조회(SV, IC, PC)는 Catalog에 `AUDIT_REQUIRED_YN: Y`를 설정하는 것이 일반적이다.

### 변경성

등록·수정·삭제·실행은 Action과 `processingType`·거래유형을 일치시킨다.

| 처리 유형 | Action | processingType | 거래유형 | 예시 |
| --- | --- | --- | --- | --- |
| 신규 등록 | create | CREATE | CRT | CM.Campaign.create → CM-CRT-0001 |
| 수정 | update | UPDATE | UPD | CM.Campaign.update → CM-UPD-0001 |
| 삭제 | delete | DELETE | DEL | OM.User.delete → OM-DEL-0001 |
| 통합 저장 | save | SAVE | SAV | OM.ServiceCatalog.save → OM-SAV-0001 |
| 실행 | execute | EXECUTE | EXE | OM.Batch.execute → OM-BAT-0001 |
| 발송 | send | (업무 정의) | SND | MG.Message.send → MG-SND-0001 |

변경성 거래는 `idempotencyKey` Header 사용을 권장한다. 중복 클릭·네트워크 재전송 시 이중 등록·이중 발송을 막을 수 있다.

---

## B.6 운영관리(OM) ServiceId

OM 업무는 관리 대상별로 Domain 구간을 나눈다. `tcf-om` 모듈에 구현되며 Context `/om`을 사용한다.

| 관리 대상 | ServiceId 예시 | 설명 |
| --- | --- | --- |
| 인증 | OM.Auth.login | OM 로그인 |
| SSO | OM.Auth.ssoLogin | SSO 연동 로그인 |
| 대시보드 | OM.Dashboard.inquiry | 운영 대시보드 |
| 사용자 | OM.User.inquiry | 사용자 목록·상세 |
| 사용자 저장 | OM.User.save | 사용자 등록·수정 |
| 권한그룹 | OM.AuthGroup.inquiry | 권한그룹 조회 |
| 메뉴 | OM.Menu.inquiry | 메뉴 조회 |
| 기능권한 | OM.FunctionAuth.save | 기능권한 저장 |
| ServiceId | OM.ServiceCatalog.inquiry | Catalog 조회 |
| ServiceId 저장 | OM.ServiceCatalog.save | Catalog 등록·수정 |
| 거래통제 | OM.TransactionControl.save | 거래통제 정책 |
| Timeout | OM.TimeoutPolicy.save | Timeout 정책 |
| 오류코드 | OM.ErrorCode.save | 오류코드 관리 |
| 공통코드 | OM.CommonCode.save | 공통코드 관리 |
| 배치 | OM.Batch.execute | 배치 수동 실행 |
| 파일 | OM.File.inquiry | 파일 메타 조회 |

OM 화면에서 ServiceId를 등록·수정하는 거래(`OM.ServiceCatalog.save`)는 메타 거래이다. 이 거래 자체도 Catalog에 등록되어 있어야 하며, 변경 시 감사로그에 이전·이후 값이 남아야 한다.

---

## B.7 Handler 명명 매핑

ServiceId와 Handler 클래스·Bean 이름은 규칙적으로 대응한다.

| ServiceId | Handler Bean | Handler Class |
| --- | --- | --- |
| SV.Customer.selectSummary | svCustomerSummaryHandler | SvCustomerSummaryHandler |
| SV.Customer.selectDetail | svCustomerDetailHandler | SvCustomerDetailHandler |
| CM.Campaign.selectList | cmCampaignListHandler | CmCampaignListHandler |
| MG.Message.send | mgMessageSendHandler | MgMessageSendHandler |
| OM.User.inquiry | omUserInquiryHandler | OmUserInquiryHandler |
| OM.ServiceCatalog.save | omServiceCatalogSaveHandler | OmServiceCatalogSaveHandler |

Handler는 `TransactionHandler`를 구현하고 `serviceId()` 메서드가 Catalog의 `SERVICE_ID`와 **문자열 완전 일치**해야 한다. Registry에 동일 ServiceId가 두 Bean에 등록되면 애플리케이션 기동이 실패한다. Bean 이름은 lowerCamelCase, 클래스는 `{업무접두}{Domain}{Action}Handler` 패턴을 따른다.

---

## B.8 ServiceId Catalog 등록

ServiceId는 소스코드뿐 아니라 DB Catalog에 등록한다. 주요 컬럼은 다음과 같다.

| 항목 | 설명 | 예시 |
| --- | --- | --- |
| SERVICE_ID | 서비스 ID | SV.Customer.selectSummary |
| BUSINESS_CODE | 업무코드 | SV |
| TRANSACTION_CODE | 대표 거래코드 | SV-INQ-0001 |
| SERVICE_NAME | 서비스명 | 고객 요약 조회 |
| PROCESSING_TYPE | 처리유형 | INQUIRY |
| MODULE_NAME | 모듈명 | sv-service |
| CONTEXT_PATH | Context | /sv |
| HANDLER_BEAN_NAME | Handler Bean | svCustomerSummaryHandler |
| SESSION_REQUIRED_YN | 세션 필요 | Y |
| AUTH_REQUIRED_YN | 권한 필요 | Y |
| AUDIT_REQUIRED_YN | 감사 여부 | Y |
| DEFAULT_TIMEOUT_SEC | 기본 Timeout(초) | 3 |
| USE_YN | 사용 여부 | Y |
| SERVICE_STATUS | 상태 | ACTIVE |
| OWNER_TEAM | 담당팀 | SV업무팀 |

Catalog는 Handler Registry, 권한 정책, Timeout 정책, 거래통제 Allow-List와 연동된다. `SUSPENDED`·`DEPRECATED` 상태 ServiceId는 Dispatcher 실행 전에 차단한다.

---

## B.9 업무별 ServiceId 예시 (17개 업무코드)

| 업무코드 | ServiceId | 거래코드 | 설명 |
| --- | --- | --- | --- |
| CC | CC.Code.selectList | CC-INQ-0001 | 공통코드 목록 |
| IC | IC.Customer.selectIntegration | IC-INQ-0001 | 통합고객 조회 |
| PC | PC.Customer.selectPrivate | PC-INQ-0001 | 개인고객 조회 |
| BC | BC.Customer.selectBusiness | BC-INQ-0001 | 기업고객 조회 |
| MS | MS.Customer.selectSummary | MS-INQ-0001 | 미니 싱글뷰 |
| SV | SV.Customer.selectSummary | SV-INQ-0001 | 고객요약 조회 |
| PD | PD.Product.selectDetail | PD-INQ-0001 | 상품 상세 |
| CM | CM.Campaign.selectList | CM-INQ-0001 | 캠페인 목록 |
| EB | EB.Rule.checkEvent | EB-CHK-0001 | 이벤트 룰 검증 |
| EP | EP.Event.process | EP-EXE-0001 | 이벤트 처리 |
| BP | BP.Behavior.process | BP-EXE-0001 | 행동정보 처리 |
| BD | BD.Behavior.selectData | BD-INQ-0001 | 행동 데이터 |
| SS | SS.Sales.selectSupport | SS-INQ-0001 | 영업지원 |
| CS | CS.Common.selectService | CS-INQ-0001 | 공통서비스 |
| CT | CT.Contents.selectList | CT-INQ-0001 | 콘텐츠 목록 |
| MG | MG.Message.send | MG-SND-0001 | 메시지 발송 |
| OM | OM.User.inquiry | OM-ADM-0001 | 운영 사용자 |

---

## B.10 잘못된 ServiceId 예시

| 잘못된 ServiceId | 문제 | 표준 예시 |
| --- | --- | --- |
| selectCustomer | 업무코드 없음 | SV.Customer.selectDetail |
| SV.selectCustomer | 업무대상 구간 누락 | SV.Customer.selectDetail |
| SV.Customer.get | 행위 모호 | SV.Customer.selectDetail |
| SV.Cust.selSum | 약어 남용 | SV.Customer.selectSummary |
| singleview.customer.summary | 표기 규칙 위반 | SV.Customer.selectSummary |
| SV.Customer.select_summary | snake_case | SV.Customer.selectSummary |
| SV.Customer.SelectSummary | Action 대문자 시작 | SV.Customer.selectSummary |
| OM.User.manage | 행위 불명확 | OM.User.inquiry / OM.User.save |
| CM.Campaign.proc | 처리 의미 불명 | CM.Campaign.execute |
| MG.SendMessage.send | Domain·행위 중복 | MG.Message.send |

---

## B.11 ServiceId 검증 기준

STF·Dispatcher가 확인하는 항목이다.

| 검증 항목 | 기준 | 오류 시 (예시) |
| --- | --- | --- |
| 필수값 | serviceId 필수 | E-TCF-DSP-0001 |
| 형식 | `{BC}.{Domain}.{action}` 3구간 | 형식 오류 |
| 업무코드 | Prefix = Header businessCode | 업무코드 불일치 |
| Catalog | OM_SERVICE_CATALOG 등록 | 미등록 차단 |
| 사용 여부 | USE_YN = Y | 사용중지 차단 |
| 상태 | SERVICE_STATUS = ACTIVE | SUSPENDED 차단 |
| Handler | Registry에 Bean 존재 | Handler 미존재 |
| 거래코드 | Header transactionCode = Catalog 매핑 | 불일치 오류 |
| 권한 | 사용자 실행 권한 | E-TCF-AUTHZ-0001 |
| Timeout | ServiceId별 정책 적용 | E-TCF-TIME-0001 |

---

## B.12 개발 시 준수사항

| 구분 | 준수사항 |
| --- | --- |
| 신규 ServiceId | OM Service Catalog에 먼저 등록 |
| Handler 구현 | `serviceId()` 반환값 = Catalog SERVICE_ID |
| 업무코드 | ServiceId Prefix = Header businessCode |
| 거래코드 | 대표 transactionCode Catalog 매핑 |
| Timeout | 조회·변경·외부연계별 정책 등록 |
| 감사 | 고객정보·다운로드·관리자 기능 감사 Y |
| 권한 | ServiceId 단위 권한 정책 연결 |
| 테스트 | 미등록·중복·Handler 없음 케이스 |
| 로그 | ServiceId, transactionCode, businessCode, GUID |
| 폐기 | 삭제 대신 SUSPENDED / DEPRECATED |

---

## 요약

ServiceId `{업무코드}.{업무대상}.{처리행위}`는 URL이 아니라 Handler 실행·거래통제·로그의 기준 키이다. 조회는 selectList/selectDetail/selectSummary/inquiry, 변경은 create/update/delete/save/execute 등 표준 Action을 쓰고, OM 관리 기능은 OM.* Domain으로 분리한다. Catalog 등록·Handler 1:1 매핑·거래코드 연동 없이는 운영 환경에서 거래가 실행되지 않는다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 A](./A-업무코드-표준표.md) |
| → 다음 | [부록 C](./C-거래코드-명명규칙.md) |

---

## 출처 색인

| 참고 | 경로 |
| --- | --- |
| NSIGHT TCF 개발 매뉴얼 (원본) | `znsight-guide-word/통합 (73).docx` |
| znsight-man 부록 B | `znsight-man/부록B-ServiceId-명명규칙.md` |
| ServiceId Dispatcher | `zman/07-ServiceIdDispatcher.md` |
| 애플리케이션 명명 규칙 | `zdoc/applicationNaming.md` |
