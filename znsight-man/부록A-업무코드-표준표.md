# 부록 A. 업무코드 표준표

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## A. 업무코드 표준표

### A.1 도입 전 안내말

본 부록은 NSIGHT TCF Framework에서 사용하는 업무코드 표준표를 정의한다.
업무코드는 단순한 약어가 아니다. NSIGHT에서는 업무코드를 기준으로 다음 항목이 모두 연결된다.
업무코드
```text
  ↓
Context Path
↓
WAR 파일명
↓
Gradle Module
↓
Java Package
↓
ServiceId
↓
거래코드
↓
Mapper / SQL ID
↓
오류코드
↓

```

거래로그 / 감사로그

기존 NSIGHT 기준에서도 9개 업무코드를 기준으로 URL Context, WAR, Package, Mapper, SQL ID, 거래코드, 에러코드, 로그 ServiceId를 통일하도록 정리되어 있다.

### A.2 업무코드 표준표

| No | 업무구분 | 업무코드 | 영문명 | 업무설명 | Context | Package | WAR |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 공통 | CC | Common | 마케팅 플랫폼 공통 기능 및 공통 업무 | /cc | cc | cc.war |
| 2 | 고객 | IC | Integration Customer | 통합고객 기준정보 및 고객통합 관리 | /ic | ic | ic.war |
| 3 | 고객 | PC | Private Customer | 개인고객 대상 처리 업무 | /pc | pc | pc.war |
| 4 | 고객 | BC | Business Customer | 기업고객 대상 처리 업무 | /bc | bc | bc.war |
| 5 | 고객 | MS | Mini Single View | 고객 단위 조회/요약 제공 업무 | /ms | ms | ms.war |
| 6 | 마케팅 | SV | Single View | 통합고객 기준의 고객 단위 조회 제공 업무 | /sv | sv | sv.war |
| 7 | 마케팅 | PD | Product | 상품정보 관리 및 활용 업무 | /pd | pd | pd.war |
| 8 | 마케팅 | CM | Campaign | 캠페인 실행 및 관리 업무 | /cm | cm | cm.war |
| 9 | 마케팅 | EB | EBM | 이벤트 기반 마케팅 업무 | /eb | eb | eb.war |
| 10 | 실시간 | EP | Event Processing | 이벤트 수집 및 실시간 처리 업무 | /ep | ep | ep.war |
| 11 | 실시간 | BP | Behavior Processing | 고객 행동정보 처리 업무 | /bp | bp | bp.war |
| 12 | 데이터 | BD | Behavior Data | 고객 행동 데이터 관리 업무 | /bd | bd | bd.war |
| 13 | 지원 | SS | Sales Support | 영업활동 지원 업무 | /ss | ss | ss.war |
| 14 | 지원 | CS | Common Service | 공통 서비스 운영 업무 | /cs | cs | cs.war |
| 15 | 지원 | CT | Contents | 마케팅 콘텐츠 관리 업무 | /ct | ct | ct.war |
| 16 | 지원 | MG | Message | 메시지 생성/발송/관리 업무 | /mg | mg | mg.war |
| 17 | 운영 | OM | Operation Management | 운영관리, 관리자 기능, 기준정보, 권한, 메뉴, 배치, 감사 조회 업무 | /om | om | om.war |

MG는 Message 업무로 사용하고, 운영관리·관리자 업무는 OM(Operation Management)으로 분리한다. 이는 Management 의미로 MG를 중복 사용하지 않기 위한 기준이다.

### A.3 업무코드 적용 원칙

| 적용 영역 | 표준 형식 | 예시 |
| --- | --- | --- |
| 업무코드 | 영문 대문자 2자리 | SV, CM, OM |

Context Path
/{업무코드 소문자}
/sv, /cm, /om
WAR 파일명
{업무코드 소문자}.war
sv.war, cm.war, om.war
Gradle Module
{업무코드 소문자}-service
sv-service, (미포함·확장 예정)
Java Package
com.nh.nsight.marketing.{업무코드 소문자}
com.nh.nsight.marketing.sv

| Java Class Prefix | 업무코드 PascalCase | SvCustomerService |
| --- | --- | --- |
| Mapper XML 위치 | mapper/{업무코드 소문자} | mapper/sv/SvCustomerMapper.xml |

SQL ID
{업무코드}.{업무대상}.{행위}
SV.Customer.selectSummary
거래코드
{업무코드}-{거래유형}-{일련번호}
SV-INQ-0001
오류코드
E-{업무코드}-{오류분류}-{일련번호}
E-SV-BIZ-0001
업무코드는 업무 경계, URL, WAR, Package, Mapper 기준을 통일하기 위한 최상위 표준 항목이며, ServiceId·거래코드·오류코드·로그 표준과 함께 관리되어야 한다.

### A.4 대표 ServiceId 예시

| 업무코드 | ServiceId 예시 | 거래코드 예시 |
| --- | --- | --- |
| 설명 | CC | CC.Code.selectList |
| CC-INQ-0001 | 공통코드 목록 조회 | IC |
| IC.Customer.selectIntegration | IC-INQ-0001 | 통합고객 조회 |
| PC | PC.Customer.selectPrivate | PC-INQ-0001 |
| 개인고객 조회 | BC | BC.Customer.selectBusiness |
| BC-INQ-0001 | 기업고객 조회 | MS |
| MS.Customer.selectSummary | MS-INQ-0001 | 고객 요약 조회 |
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
| OM.User.inquiry | OM-ADM-0001 | 운영 사용자 조회 |

### A.5 대표 Mapper 표준

| 업무코드 | 대표 Mapper | Mapper XML 위치 |
| --- | --- | --- |
| 주요 역할 | CC | CcCodeMapper |
| mapper/cc/CcCodeMapper.xml | 공통코드 | IC |
| IcCustomerIntegrationMapper | mapper/ic/IcCustomerIntegrationMapper.xml | 통합고객 |
| PC | PcPrivateCustomerMapper | mapper/pc/PcPrivateCustomerMapper.xml |
| 개인고객 | BC | BcBusinessCustomerMapper |
| mapper/bc/BcBusinessCustomerMapper.xml | 기업고객 | MS |
| MsCustomerSummaryMapper | mapper/ms/MsCustomerSummaryMapper.xml | 고객 요약 |
| SV | SvSingleViewCustomerMapper | mapper/sv/SvSingleViewCustomerMapper.xml |
| 고객 종합조회 | PD | PdProductMapper |
| mapper/pd/PdProductMapper.xml | 상품정보 | CM |
| CmCampaignMapper | mapper/cm/CmCampaignMapper.xml | 캠페인 |
| EB | EbEbmRuleMapper | mapper/eb/EbEbmRuleMapper.xml |
| EBM Rule | EP | EpEventProcessingMapper |
| mapper/ep/EpEventProcessingMapper.xml | 이벤트 처리 | BP |
| BpBehaviorProcessingMapper | mapper/bp/BpBehaviorProcessingMapper.xml | 행동정보 처리 |
| BD | BdBehaviorDataMapper | mapper/bd/BdBehaviorDataMapper.xml |
| 행동 데이터 | SS | SsSalesSupportMapper |
| mapper/ss/SsSalesSupportMapper.xml | 영업지원 | CS |
| CsCommonServiceMapper | mapper/cs/CsCommonServiceMapper.xml | 공통 서비스 |
| CT | CtContentsMapper | mapper/ct/CtContentsMapper.xml |
| 콘텐츠 | MG | MgMessageMapper |
| mapper/mg/MgMessageMapper.xml | 메시지 | OM |
| OmOperationMapper | mapper/om/OmOperationMapper.xml | 운영관리 |

### A.6 사용 예시

SV 고객요약조회
업무코드        : SV
Context         : /sv
WAR             : sv.war
Package         : com.nh.nsight.marketing.sv
ServiceId       : SV.Customer.selectSummary
TransactionCode : SV-INQ-0001
Handler         : SvCustomerSummaryHandler
Facade          : SvCustomerFacade
Service         : SvCustomerService
Rule            : SvCustomerRule
DAO             : SvCustomerDao
Mapper          : SvSingleViewCustomerMapper
Mapper XML      : mapper/sv/SvSingleViewCustomerMapper.xml
SQL ID          : SV.Customer.selectSummary
Error Code      : E-SV-BIZ-0001

OM 사용자관리
업무코드        : OM
Context         : /om
WAR             : om.war
Package         : com.nh.nsight.marketing.om
ServiceId       : OM.User.inquiry
TransactionCode : OM-ADM-0001
Handler         : OmUserInquiryHandler
Facade          : OmUserFacade
Service         : OmUserService
Rule            : OmUserRule
DAO             : OmUserDao
Mapper          : OmUserMapper
Mapper XML      : mapper/om/OmUserMapper.xml
Table           : OM_USER
Error Code      : E-OM-AUTHZ-0001

### A.7 개발 시 준수사항

| 구분 | 준수사항 |
| --- | --- |
업무코드 표준표에 먼저 등록한 뒤 개발한다.

| 신규 업무 추가 | |
업무코드 소문자와 Context를 일치시킨다.

| Context 정의 | |

WAR 생성
WAR 파일명은 업무코드 소문자 기준으로 생성한다.
Package 작성
업무 Root Package는 업무코드 소문자를 사용한다.
ServiceId 작성
업무코드 대문자로 시작한다.
거래코드 작성
업무코드 + 거래유형 + 일련번호 형식을 따른다.
Mapper 작성
업무코드 Prefix를 사용한다.
오류코드 작성
업무코드가 포함된 표준 오류코드를 사용한다.
로그 기록
GUID, ServiceId, 업무코드, 거래코드를 함께 남긴다.
임의 약어 금지
업무팀별 임의 코드, 약어, 별칭 사용을 금지한다.

### A.8 마무리말

업무코드 표준표는 NSIGHT TCF 개발 표준의 출발점이다.업무코드가 확정되어야 URL, WAR, Package, ServiceId, Mapper, SQL ID, 거래코드, 오류코드, 로그가 하나의 기준으로 연결된다.
업무코드가 흔들리면
→ Package가 흔들리고
→ Mapper가 흔들리고
→ SQL ID가 흔들리고
→ 거래로그 추적이 어려워진다.

따라서 NSIGHT TCF Framework에서는 9개 업무코드를 최상위 식별 기준으로 삼고, 모든 개발·배포·운영 표준을 이 코드체계에 맞춰 정렬한다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (72).docx`

| [applicationNaming.md](../zdoc/applicationNaming.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)