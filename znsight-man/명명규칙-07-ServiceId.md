# ServiceId 명명규칙

> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

ServiceId 명명규칙 설계기준
## 1. 도입 전 안내말

ServiceId는 단순한 프로그램 이름이 아니다.NSIGHT TCF Framework에서 ServiceId는 온라인 거래를 실행·통제·추적하는 핵심 식별자이다.
NSIGHT TCF 구조에서는 사용자가 POST /{businessCode}/online으로 요청하지만, 실제 어떤 업무 Handler를 실행할지는 URL이 아니라 표준 전문 Header의 serviceId로 결정한다. 기존 설계에서도 Dispatcher는 URL을 보고 업무를 실행하지 않고, Header의 serviceId를 보고 업무 Handler를 실행한다고 정의되어 있다.
따라서 ServiceId 명명규칙의 핵심은 다음이다.
ServiceId만 보고 업무코드, 업무대상, 처리행위, Handler, 거래코드, 로그, 오류코드까지 추적 가능해야 한다.

## 2. ServiceId 설계 결론

ServiceId는 다음 형식을 표준으로 한다.
{업무코드}.{업무대상}.{처리행위}

| 구성요소 | 표기 기준 | 설명 | 예시 |
| --- | --- | --- | --- |
| 업무코드 | 대문자 2자리 | 업무 WAR / Context / Package 식별 | SV, CM, OM, MG |
| 업무대상 | PascalCase | 처리 대상 도메인 또는 업무 객체 | Customer, Campaign, User, Message |
| 처리행위 | lowerCamelCase | 수행 기능 또는 행위 | selectSummary, selectList, save, delete |

예시는 다음과 같다.
| 업무 | ServiceId | 의미 |
| --- | --- | --- |
| SV | SV.Customer.selectSummary | 고객 요약 조회 |
| SV | SV.Customer.selectDetail | 고객 상세 조회 |
| SV | SV.Product.selectHoldingList | 보유상품 목록 조회 |
| CM | CM.Campaign.selectList | 캠페인 목록 조회 |
| CM | CM.Campaign.save | 캠페인 저장 |
| MG | MG.Message.send | 메시지 발송 |
| OM | OM.User.selectList | 사용자 목록 조회 |
| OM | OM.ServiceCatalog.save | ServiceId 등록 |
| OM | OM.ErrorCode.update | 오류코드 수정 |

Service ID 관리 설계에서도 권장 형식을 {업무코드}.{업무대상}.{처리행위}로 두고, SV.Customer.selectSummary, CM.Campaign.create, MG.Message.send, OM.ServiceCatalog.inquiry 등을 예시로 제시한다.

## 3. ServiceId와 URL의 관계

ServiceId와 URL은 역할이 다르다.
| 구분 | 역할 | 예시 |
| --- | --- | --- |
| URL Context | 어느 업무 WAR로 들어갈지 결정 | /sv |
| Online Endpoint | 온라인 전문 공통 진입점 | /sv/online |
| businessCode | 요청 업무코드 검증 | SV |
| serviceId | 실제 실행할 Handler 결정 | SV.Customer.selectSummary |
| transactionCode | 거래로그, 감사, 재처리 식별 | SV-INQ-0001 |

예를 들면 다음과 같다.
```text
POST /sv/online
```

header.businessCode = SV
```text
header.serviceId    = SV.Customer.selectSummary
```

header.transactionCode = SV-INQ-0001

실행 Handler = SvCustomerSummaryHandler

/sv/online은 SV 업무로 들어오는 공통 입구이고, SV.Customer.selectSummary는 SV 업무 안에서 실제 실행할 프로그램을 식별한다. 기존 설계도 URL Routing은 업무 WAR를 찾는 기준이고, ServiceId Dispatching은 어떤 업무 Handler를 실행할지 결정하는 기준으로 구분한다.

## 4. ServiceId 최상위 원칙

| No | 원칙 | 기준 |
| --- | --- | --- |
| 1 | 업무코드 우선 | ServiceId는 반드시 업무코드로 시작한다 |
| 2 | Context와 일치 | /sv/online 요청의 ServiceId는 SV.로 시작해야 한다 |
| 3 | businessCode와 일치 | Header businessCode=SV이면 ServiceId Prefix도 SV.이어야 한다 |
| 4 | 의미 중심 | 업무대상과 처리행위가 명확해야 한다 |
| 5 | URL 기능명 금지 | 기능명을 URL에 넣지 않고 ServiceId에 표현한다 |
| 6 | Handler 매핑 가능 | 하나의 ServiceId는 실행 Handler와 매핑되어야 한다 |
| 7 | 운영 등록 필수 | ServiceId는 OM Service Catalog 또는 Service Master에 등록되어야 한다 |
| 8 | 미등록 차단 | 등록되지 않은 ServiceId는 실행하지 않는다 |
| 9 | 로그 추적 가능 | 거래로그, 오류로그, 감사로그에 ServiceId를 남긴다 |
| 10 | 변경 최소화 | 운영 반영 후 ServiceId 변경은 원칙적으로 금지한다 |

## 5. 9개 업무코드별 ServiceId Prefix

| No | 업무코드 | 업무명 | ServiceId Prefix | 예시 |
| --- | --- | --- | --- | --- |
| 1 | CC | Common | CC. | CC.Code.selectList |
| 2 | IC | Integration Customer | IC. | IC.Customer.selectBasic |
| 3 | PC | Private Customer | PC. | PC.Customer.selectDetail |
| 4 | BC | Business Customer | BC. | BC.Customer.selectBusiness |
| 5 | MS | Mini Single View | MS. | MS.Customer.selectMiniView |
| 6 | SV | Single View | SV. | SV.Customer.selectSummary |
| 7 | PD | Product | PD. | PD.Product.selectHoldingList |
| 8 | CM | Campaign | CM. | CM.Campaign.selectTarget |
| 9 | EB | EBM | EB. | EB.Event.processMarketing |
| 10 | EP | Event Processing | EP. | EP.Event.processRealtime |
| 11 | BP | Behavior Processing | BP. | BP.Behavior.collectEvent |
| 12 | BD | Behavior Data | BD. | BD.Behavior.selectData |
| 13 | SS | Sales Support | SS. | SS.Sales.selectSupport |
| 14 | CS | Common Service | CS. | CS.Common.executeService |
| 15 | CT | Contents | CT. | CT.Content.selectBanner |
| 16 | MG | Message | MG. | MG.Message.send |
| 17 | OM | Operation Management | OM. | OM.User.selectList |

## 6. 처리행위 Action 표준

ServiceId의 세 번째 요소인 처리행위는 표준 동사를 사용한다.
| 처리유형 | 표준 Action | 사용 기준 | 예시 |
| --- | --- | --- | --- |
| 목록 조회 | selectList | 조건에 맞는 목록 조회 | OM.User.selectList |
| 단건 조회 | selectDetail | 상세 1건 조회 | SV.Customer.selectDetail |
| 요약 조회 | selectSummary | 요약정보 조회 | SV.Customer.selectSummary |
| 건수 조회 | count | 조회 건수 확인 | CM.Campaign.countTarget |
| 존재 확인 | exists | 존재 여부 확인 | IC.Customer.exists |
| 등록 | create | 신규 생성 | CM.Campaign.create |
| 수정 | update | 기존 데이터 수정 | OM.User.update |
| 삭제 | delete | 삭제 또는 논리삭제 | OM.ErrorCode.delete |
| 저장 | save | 등록/수정 통합 | OM.ServiceCatalog.save |
| 발송 | send | 메시지, 알림 발송 | MG.Message.send |
| 실행 | execute | 배치, 작업, 프로세스 실행 | OM.Batch.execute |
| 재처리 | retry | 실패 거래 재처리 | EB.Event.retry |
| 업로드 | upload | 파일 업로드 | OM.File.upload |
| 다운로드 | download | 파일 다운로드 | SV.Customer.downloadExcel |
| 발급 | issue | 토큰, 번호 발급 | JWT.Auth.issue |
| 검증 | verify | 토큰, 인증 검증 | JWT.Auth.verify |
| 취소 | cancel | 요청, 예약, 작업 취소 | CM.Campaign.cancel |
| 승인 | approve | 승인 처리 | OM.Deploy.approve |
| 반려 | reject | 반려 처리 | OM.Deploy.reject |
| 중지 | suspend | 사용 중지 | OM.ServiceCatalog.suspend |
| 재개 | resume | 중지 상태 해제 | OM.ServiceCatalog.resume |

inquiry는 기존 OM 예시에서 사용되고 있으나, 신규 표준에서는 의미를 더 명확히 하기 위해 selectList, selectDetail, selectSummary를 우선 권장한다. 단, 이미 OM 관리화면 또는 기존 Handler에 inquiry가 등록되어 있으면 하위 호환을 위해 허용할 수 있다.

## 7. ServiceId와 Handler 명명 연결

ServiceId는 Handler 이름과 자연스럽게 연결되어야 한다.
| ServiceId | Handler Class | Handler Bean |
| --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerSummaryHandler | svCustomerSummaryHandler |
| SV.Customer.selectDetail | SvCustomerDetailHandler | svCustomerDetailHandler |
| SV.Product.selectHoldingList | SvProductHoldingListHandler | svProductHoldingListHandler |
| CM.Campaign.selectList | CmCampaignListHandler | cmCampaignListHandler |
| MG.Message.send | MgMessageSendHandler | mgMessageSendHandler |
| OM.User.selectList | OmUserListHandler | omUserListHandler |
| OM.ServiceCatalog.save | OmServiceCatalogSaveHandler | omServiceCatalogSaveHandler |

Handler는 ServiceId별 업무 진입점이며, Dispatcher가 serviceId → TransactionHandler Bean 기준으로 선택한다. 설계 기준에서도 Dispatcher 역할은 serviceId를 기준으로 TransactionHandler를 선택하는 것이며, 등록된 ServiceId만 실행하도록 되어 있다.

## 8. ServiceId와 거래코드 관계

ServiceId와 거래코드는 모두 필요하다.
| 항목 | 역할 |
| --- | --- |
| 예시 | businessCode |
| 업무 WAR / Context 식별 | SV |
| serviceId | 실제 Handler 실행 식별 |
| SV.Customer.selectSummary | transactionCode |
| 거래로그, 감사, 재처리 식별 | SV-INQ-0001 |
| 권장 관계는 다음이다. | businessCode + transactionCode = 거래 식별 |
| serviceId = 실행 Handler 식별 | serviceId + transactionCode = 실행과 로그 정합성 검증 |

Service ID 설계 문서에서도 serviceId는 실제 실행할 업무 Handler 식별자이고, transactionCode는 거래로그·전문·재처리·감사 기준의 거래 식별자이며, businessCode는 업무 WAR/Context 식별자로 구분한다.
| 예시 | 업무 | ServiceId | 거래코드 |
| --- | --- | --- | --- |
| 처리유형 | SV | SV.Customer.selectSummary | SV-INQ-0001 |
| 조회 | SV | SV.Customer.selectDetail | SV-INQ-0002 |
| 조회 | CM | CM.Campaign.create | CM-REG-0001 |
| 등록 | CM | CM.Campaign.update | CM-UPD-0001 |
| 수정 | MG | MG.Message.send | MG-SND-0001 |
| 발송 | OM | OM.User.selectList | OM-INQ-0001 |
| 조회 | OM | OM.ServiceCatalog.save | OM-REG-0001 |

등록

## 9. ServiceId와 SQL ID 관계

조회·등록·수정·삭제 거래는 SQL ID와도 연결되어야 한다.
| 항목 | 기준 |
| --- | --- |
| 예시 | ServiceId |
| 업무 실행 식별자 | SV.Customer.selectSummary |
| Mapper Interface | 업무 + 대상 Mapper |
| SvCustomerMapper | Mapper XML |
| 업무코드별 XML | mapper/sv/SvCustomerMapper.xml |
| SQL ID | ServiceId와 동일 또는 유사 |
| SV.Customer.selectSummary | 거래로그 |
| ServiceId / SQL ID 함께 기록 | SV.Customer.selectSummary |

권장 기준은 다음이다.
대표 SQL이 1개인 단순 조회는 ServiceId와 SQL ID를 동일하게 둔다.복합 거래는 ServiceId를 대표 실행명으로 두고, SQL ID는 세부 행위명으로 분리한다.
예시는 다음과 같다.
| 거래 유형 | ServiceId | SQL ID |
| --- | --- | --- |
| 단순 조회 | SV.Customer.selectSummary | SV.Customer.selectSummary |
| 상세 조회 | SV.Customer.selectDetail | SV.Customer.selectDetail |
| 목록 조회 | CM.Campaign.selectList | CM.Campaign.selectList |
| 복합 조회 | SV.Customer.selectTotalView | SV.Customer.selectBasic, SV.Product.selectHoldingList, SV.Campaign.selectRecentList |
| 저장 거래 | OM.ServiceCatalog.save | OM.ServiceCatalog.insert, OM.ServiceCatalog.update |

## 10. ServiceId Catalog 관리 기준

ServiceId는 코드에만 존재하면 안 된다.운영관리, 권한, 거래통제, Timeout, 감사, 로그 추적을 위해 Catalog로 관리해야 한다.
| 관리 항목 | 설명 |
| --- | --- |
| 예시 | SERVICE_ID |
| 서비스 ID | SV.Customer.selectSummary |
| BUSINESS_CODE | 업무코드 |
| SV | TRANSACTION_CODE |
| 거래코드 | SV-INQ-0001 |
| SERVICE_NAME | 서비스명 |
| 고객 요약 조회 | PROCESS_TYPE |
| 처리유형 | INQUIRY |
| MODULE_NAME | 모듈명 |
| sv-service | CONTEXT_PATH |
| Context | /sv |
| HANDLER_BEAN_NAME | Handler Bean |
| svCustomerSummaryHandler | TIMEOUT_SECONDS |
| 제한시간 | 3 |
| SESSION_REQUIRED_YN | 세션 필요 여부 |
| Y | AUTH_REQUIRED_YN |
| 권한 필요 여부 | Y |
| AUDIT_YN | 감사 대상 여부 |
| Y | MASKING_YN |
| 마스킹 대상 여부 | Y |
| USE_YN | 사용 여부 |
| Y | SERVICE_STATUS |
| 상태 | DRAFT, ACTIVE, SUSPENDED, DEPRECATED |
| OWNER_TEAM | 담당팀 |

SV업무팀
Service ID 관리 설계에서도 Service ID Master, Handler Registry, 권한 정책, Timeout 정책, 감사 정책, 거래통제 구조를 함께 설계하고, 등록된 Service ID만 실행하도록 제시한다.

## 11. ServiceId 상태 기준

상태
| 의미 | 실행 가능 여부 | DRAFT | 개발 또는 등록 준비 중 |
| --- | --- | --- | --- |
| 불가 | ACTIVE | 운영 사용 가능 | 가능 |
| SUSPENDED | 일시 중지 | 불가 | DEPRECATED |
| 폐기 예정, 신규 사용 금지 | 원칙적 불가 | DELETED | 논리 삭제 |

불가
운영 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 신규 등록 | DRAFT로 등록 후 검토 |
| 테스트 승인 | 테스트 완료 후 ACTIVE 전환 |
| 장애 차단 | 문제 발생 시 SUSPENDED 전환 |
| 폐기 예정 | 대체 ServiceId가 있는 경우 DEPRECATED |
| 완전 삭제 | 거래로그 추적을 위해 물리 삭제는 지양 |

## 12. ServiceId 정합성 검증 규칙

STF 전처리 또는 Dispatcher 이전에 다음 정합성을 확인한다.
| 검증 항목 | 정상 기준 | 오류 예시 |
| --- | --- | --- |
| 필수값 | serviceId가 존재해야 함 | null, blank |
| 형식 | {업무코드}.{업무대상}.{처리행위} | SV_selectCustomer |
| 업무코드 일치 | businessCode=SV이면 serviceId는 SV.로 시작 | businessCode=SV, serviceId=CM.Campaign.selectList |
| Context 일치 | /sv/online 요청은 SV. ServiceId 사용 | /sv/online, OM.User.selectList |
| Catalog 등록 | OM Service Catalog에 등록되어야 함 | 미등록 ServiceId |
| 사용 여부 | USE_YN=Y | USE_YN=N |
| 상태 | SERVICE_STATUS=ACTIVE | DRAFT, SUSPENDED |
| Handler 존재 | Handler Registry에 Bean 존재 | Catalog만 있고 Handler 없음 |
| 거래코드 일치 | Header transactionCode와 Catalog transactionCode 일치 | SV-INQ-0001 불일치 |
| 거래통제 | Allow-List 등록 | 미등록 거래 |
| Timeout 정책 | 기본값 또는 서비스별 정책 존재 | 정책 누락 |
| 권한 정책 | 사용자 권한 보유 | 미권한 사용자 |

Dispatcher 처리 알고리즘도 Header에서 serviceId를 추출하고, businessCode와 serviceId Prefix 일치 여부를 확인한 뒤, Catalog 또는 Cache 조회, 사용 여부 확인, 거래통제 확인, Handler Registry 조회 순서로 실행하도록 정의되어 있다.

## 13. ServiceId 오류코드 기준

| 오류 상황 | 오류코드 예시 | 설명 |
| --- | --- | --- |
| ServiceId 누락 | E-TCF-SVC-0001 | 필수 Header 누락 |
| ServiceId 형식 오류 | E-TCF-SVC-0002 | {업무}.{대상}.{행위} 형식 위반 |
| 업무코드 불일치 | E-TCF-SVC-0003 | businessCode와 ServiceId Prefix 불일치 |
| 미등록 ServiceId | E-TCF-SVC-0004 | Catalog 미등록 |
| 중지 ServiceId | E-TCF-SVC-0005 | SUSPENDED 또는 USE_YN=N |
| Handler 없음 | E-TCF-DSP-0004 | Registry에 Handler 없음 |
| ServiceId 중복 | E-TCF-DSP-0005 | 동일 ServiceId를 둘 이상 Handler가 등록 |
| 거래코드 불일치 | E-TCF-SVC-0006 | transactionCode와 Catalog 불일치 |
| 권한 없음 | E-TCF-AUTHZ-0001 | ServiceId 실행 권한 없음 |
| 거래통제 차단 | E-TCF-CTL-0001 | Allow-List 미등록 |

오류코드는 기존 오류코드 표준처럼 E-{DOMAIN}-{CATEGORY}-{NNNN} 형식을 따르며, ServiceId 관련 오류는 SVC, Dispatcher 관련 오류는 DSP, 거래통제는 CTL 범주로 구분한다.

## 14. 금지 명명 사례

| 잘못된 ServiceId | 문제 | 표준 예시 |
| --- | --- | --- |
| selectCustomerSummary | 업무코드 없음 | SV.Customer.selectSummary |
| SV.selectCustomerSummary | 업무대상 분리 없음 | SV.Customer.selectSummary |
| SV.Customer.summary | 행위가 모호함 | SV.Customer.selectSummary |
| SV.Customer.getInfo | getInfo 의미가 불명확 | SV.Customer.selectDetail |
| SV.Customer.list | 동사 기준 불명확 | SV.Customer.selectList |
| SV.Customer.saveData | Data 불필요 | SV.Customer.save |
| SV.Customer.proc | 약어·의미 모호 | SV.Customer.execute |
| SV001 | 거래코드와 혼동 | SV.Customer.selectSummary |
| SingleView.Customer.selectSummary | 업무코드 기준 아님 | SV.Customer.selectSummary |
| SV.Customer.SelectSummary | action 대문자 시작 | SV.Customer.selectSummary |
| sv.customer.selectSummary | 업무코드 대문자 위반 | SV.Customer.selectSummary |
| SV.Customer.select_summary | snake_case 사용 | SV.Customer.selectSummary |

## 15. ServiceId 등록 절차

## 1. 업무팀이 신규 거래 정의

## 2. 업무코드 / 업무대상 / 처리행위 확정

## 3. ServiceId 채번

## 4. 거래코드 채번

## 5. Handler Class 설계

## 6. OM Service Catalog 등록

## 7. 거래통제 등록

## 8. Timeout 정책 등록

## 9. 권한 / 감사 / 마스킹 정책 등록

## 10. 개발 및 단위 테스트

## 11. 통합 테스트

## 12. 운영 반영 승인

## 13. ACTIVE 전환

| 단계 | 산출물 |
| --- | --- |
| 거래 정의 | ServiceId 정의서 |
| 코드 개발 | Handler, Facade, Service, Rule, DAO/Mapper |
| 기준정보 등록 | Service Catalog, 거래통제, Timeout, 권한 |
| 테스트 | ServiceId 호출 테스트, 오류 테스트, 권한 실패 테스트 |
| 운영전환 | Smoke Test, Health Check, 거래로그 확인 |

## 16. ServiceId 설계 예시

### 16.1 SV 고객 요약 조회

| 항목 | 값 |
| --- | --- |
| 업무코드 | SV |
| Context | /sv |
| WAR | sv.war |
| Package | com.nh.nsight.marketing.sv |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| Handler | SvCustomerSummaryHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerRule |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper |
| SQL ID | SV.Customer.selectSummary |
| 오류코드 | E-SV-BIZ-0001 |

### 16.2 OM ServiceId 등록

| 항목 | 값 |
| --- | --- |
| 업무코드 | OM |
| Context | /om |
| WAR | om.war |
| Module | tcf-om |
| ServiceId | OM.ServiceCatalog.save |
| 거래코드 | OM-REG-0001 |
| Handler | OmServiceCatalogSaveHandler |
| 처리유형 | SAVE |
| 권한 | 운영관리자 |
| 감사로그 | Y |

## 17. ServiceId 설계 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| ServiceId가 {업무코드}.{업무대상}.{처리행위} 형식인가 | □ |
| 업무코드가 17개 표준 업무코드 중 하나인가 | □ |
| Header businessCode와 ServiceId Prefix가 일치하는가 | □ |
| Context Path와 ServiceId Prefix가 일치하는가 | □ |
| ServiceId가 OM Service Catalog에 등록되어 있는가 | □ |
| ServiceId와 Handler Bean이 1:1로 매핑되는가 | □ |
| Handler Class명이 ServiceId 의미와 일치하는가 | □ |
| 거래코드가 ServiceId와 매핑되어 있는가 | □ |
| 거래통제 Allow-List에 등록되어 있는가 | □ |
| Timeout 정책이 등록되어 있는가 | □ |
| 권한 정책이 등록되어 있는가 | □ |
| 감사로그 대상 여부가 정의되어 있는가 | □ |
| 고객정보 취급 여부와 마스킹 정책이 정의되어 있는가 | □ |
| SQL ID 또는 Mapper ID와 추적 가능하게 연결되는가 | □ |
| 미등록 또는 중지 ServiceId 호출 시 차단되는가 | □ |
| 거래로그에 serviceId, transactionCode, businessCode가 기록되는가 | □ |

## 18. 마무리말

ServiceId 명명규칙은 NSIGHT TCF Framework의 실행 구조를 고정하는 기준이다.
업무코드
```text
  ↓
ServiceId
  ↓
Handler
  ↓
Facade / Service / Rule / DAO / Mapper
  ↓
SQL ID
  ↓

```

거래코드 / 오류코드 / 거래로그

따라서 NSIGHT에서는 다음 기준을 고정한다.
URL은 업무 WAR로 들어가는 입구이고, ServiceId는 실제 업무 Handler를 실행하는 기준이다.
ServiceId를 표준화하면 개발자는 어디에 무엇을 구현해야 하는지 알 수 있고, 운영자는 거래로그와 오류코드만으로 어느 업무, 어느 Handler, 어느 SQL에서 문제가 발생했는지 추적할 수 있다.

---

## 관련 Manual 장

- [16장](./16-ServiceId-설계.md)

## 원본

- [`znsight-guide-word`](../znsight-guide-word/) — `명명규칙 상세 (7).docx`
