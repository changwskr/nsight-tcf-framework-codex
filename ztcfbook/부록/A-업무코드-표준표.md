# 부록 A. 업무코드 표준표

| 항목 | 내용 |
| --- | --- |
| **부록** | A |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## A.1 업무코드의 역할

업무코드(Business Code)는 NSIGHT TCF Framework에서 **모든 식별자 체계의 최상위 기준**이다. 단순한 약어가 아니라, 업무 경계·배포 단위·패키지 구조·거래 식별까지 한 줄로 연결하는 루트 키이다. 업무코드 하나가 확정되면 Context Path, WAR 파일명, Gradle 모듈, Java Package, ServiceId 접두어, 거래코드 접두어, Mapper/SQL ID, 오류코드, 거래로그·감사로그 필드가 동일한 규칙으로 정렬된다.

NSIGHT TCF는 REST Resource URL이 업무 의미를 담지 않는다. `POST /sv/online`처럼 URL은 업무 WAR 진입점만 표시하고, 실제 실행 대상은 Header의 `serviceId`가 결정한다. 그럼에도 `businessCode`는 Header 필수값이며, URL Path의 업무코드와 반드시 일치해야 한다. STF 전처리 단계에서 이 정합성을 검증하지 않으면 거래통제·권한·로그 추적이 모두 어긋난다.

표준표에는 17개 업무코드(CC부터 OM까지)가 정의되어 있다. 이는 마케팅 플랫폼 전체 도메인을 논리적으로 분리한 **설계 기준**이다. 현재 코드베이스(develop 기준)에 실제 배포되는 업무 WAR는 9개(`ic`, `pc`, `ms`, `sv`, `pd`, `eb`, `ep`, `ss`, `mg`)이며, 운영관리는 별도 모듈 `tcf-om`이 담당한다. CC, BC, CM 등 표준표에만 존재하는 코드는 공통 라이브러리·향후 확장·타 업무 WAR 내부 패키지로 흡수되거나, 별도 WAR로 분리 예정인 영역이다. 신규 개발 시에는 **먼저 표준표에 등록**한 뒤, 배포 WAR와 Package에 반영하는 순서를 따른다.

```text
업무코드
  ↓
Context Path  →  /{업무코드 소문자}
WAR 파일명    →  {업무코드 소문자}.war
Gradle Module →  {업무코드 소문자}-service
Java Package  →  com.nh.nsight.marketing.{업무코드 소문자}
ServiceId     →  {업무코드}.{Domain}.{action}
거래코드      →  {업무코드}-{거래유형}-{일련번호}
Mapper/SQL ID →  {업무코드}.{대상}.{행위}
오류코드      →  E-{업무코드}-{분류}-{일련번호}
거래로그      →  businessCode + serviceId + transactionCode + GUID
```

업무코드가 흔들리면 Package가 어긋나고, Mapper XML 위치가 달라지며, SQL ID와 Service Catalog 매핑이 깨진다. 운영 환경에서 "어느 WAR의 어느 Handler가 이 거래를 처리했는가"를 GUID 하나로 역추적하려면 업무코드 표준을 개발 초기에 고정하는 것이 필수이다.

---

## A.2 업무코드 표준표 (17개)

아래 표는 NSIGHT TCF 전체 업무코드 표준이다. **배포 WAR** 열은 현재 코드베이스 기준 실제 WAR 존재 여부를 나타낸다.

| No | 업무구분 | 업무코드 | 영문명 | 업무설명 | Context | Package | WAR | 배포 WAR |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 공통 | CC | Common | 마케팅 플랫폼 공통 기능 및 공통 업무 | /cc | cc | cc.war | — |
| 2 | 고객 | IC | Integration Customer | 통합고객 기준정보 및 고객통합 관리 | /ic | ic | ic.war | ic |
| 3 | 고객 | PC | Private Customer | 개인고객 대상 처리 업무 | /pc | pc | pc.war | pc |
| 4 | 고객 | BC | Business Customer | 기업고객 대상 처리 업무 | /bc | bc | bc.war | — |
| 5 | 고객 | MS | Mini Single View | 고객 단위 조회·요약 제공 업무 | /ms | ms | ms.war | ms |
| 6 | 마케팅 | SV | Single View | 통합고객 기준 고객 단위 종합 조회 | /sv | sv | sv.war | sv |
| 7 | 마케팅 | PD | Product | 상품정보 관리 및 활용 업무 | /pd | pd | pd.war | pd |
| 8 | 마케팅 | CM | Campaign | 캠페인 실행 및 관리 업무 | /cm | cm | cm.war | — |
| 9 | 마케팅 | EB | EBM | 이벤트 기반 마케팅 업무 | /eb | eb | eb.war | eb |
| 10 | 실시간 | EP | Event Processing | 이벤트 수집 및 실시간 처리 업무 | /ep | ep | ep.war | ep |
| 11 | 실시간 | BP | Behavior Processing | 고객 행동정보 처리 업무 | /bp | bp | bp.war | — |
| 12 | 데이터 | BD | Behavior Data | 고객 행동 데이터 관리 업무 | /bd | bd | bd.war | — |
| 13 | 지원 | SS | Sales Support | 영업활동 지원 업무 | /ss | ss | ss.war | ss |
| 14 | 지원 | CS | Common Service | 공통 서비스 운영 업무 | /cs | cs | cs.war | — |
| 15 | 지원 | CT | Contents | 마케팅 콘텐츠 관리 업무 | /ct | ct | ct.war | — |
| 16 | 지원 | MG | Message | 메시지 생성·발송·관리 업무 | /mg | mg | mg.war | mg |
| 17 | 운영 | OM | Operation Management | 운영관리, 관리자, 기준정보, 권한, 메뉴, 배치, 감사 조회 | /om | om | om.war | tcf-om |

**MG와 OM 분리 원칙:** MG는 Message(메시지 발송) 업무 전용이다. 운영관리·관리자 기능은 OM(Operation Management)으로 분리한다. 과거 Management 의미로 MG를 중복 사용하지 않기 위한 기준이며, `OM.User.inquiry`와 `MG.Message.send`가 서로 다른 WAR·Catalog·권한 정책을 갖는다.

**현재 배포 구성:** 업무 WAR 9개(`ic`, `pc`, `ms`, `sv`, `pd`, `eb`, `ep`, `ss`, `mg`) + 플랫폼 `tcf-om`. `ztomcat/deploy-wars.sh`는 프레임워크·게이트웨이 WAR를 포함해 총 13개 WAR를 배포하고, `buildZtomcatWars` 태스크는 15개 WAR 빌드를 대상으로 한다. 로컬 `bootRun` 기준 포트는 gateway 8100, uj 8102, jwt 8110, ui 8099이다.

---

## A.3 업무코드 적용 원칙

업무코드는 아래 영역에 동일한 규칙을 적용한다. 한 영역만 표준을 따르고 다른 영역을 임의로 작성하면 Catalog·로그·배포 스크립트 간 불일치가 발생한다.

| 적용 영역 | 표준 형식 | 예시 |
| --- | --- | --- |
| 업무코드 | 영문 대문자 2~3자리 | SV, CM, OM |
| Context Path | `/{업무코드 소문자}` | /sv, /cm, /om |
| WAR 파일명 | `{업무코드 소문자}.war` | sv.war, mg.war |
| Gradle Module | `{업무코드 소문자}-service` | sv-service, ic-service |
| Java Package | `com.nh.nsight.marketing.{업무코드 소문자}` | com.nh.nsight.marketing.sv |
| Java Class Prefix | 업무코드 PascalCase + 도메인 | SvCustomerService, OmUserService |
| Mapper XML 위치 | `mapper/{업무코드 소문자}/` | mapper/sv/SvCustomerMapper.xml |
| SQL ID | `{업무코드}.{업무대상}.{행위}` | SV.Customer.selectSummary |
| ServiceId | `{업무코드}.{Domain}.{action}` | SV.Customer.selectSummary |
| 거래코드 | `{업무코드}-{거래유형}-{일련번호}` | SV-INQ-0001 |
| 오류코드 | `E-{업무코드}-{오류분류}-{일련번호}` | E-SV-BIZ-0001 |

Context Path와 WAR 파일명은 **소문자 업무코드**를 그대로 사용한다. `businessCode` Header 값은 **대문자**이므로 STF에서 URL Path(`/sv`)와 Header(`SV`)를 대소문자 무시 비교하거나 정규화한다. Package와 Mapper 디렉터리는 소문자만 허용하며, Java 클래스명은 업무코드를 PascalCase 접두어로 쓴다.

Gradle 멀티 모듈에서는 `{code}-service`가 업무 로직 모듈, `{code}-web` 또는 공통 `tcf-web` 의존으로 Online Endpoint를 노출하는 패턴을 따른다. 신규 업무 WAR 추가 시 `settings.gradle`·`ztomcat/deploy-wars.sh`·OM Context-WAR 매핑(부록 K)을 함께 갱신해야 한다.

---

## A.4 대표 ServiceId·거래코드 예시

업무코드별 대표 ServiceId와 거래코드 매핑이다. ServiceId는 실행 Handler 식별, 거래코드는 로그·감사·재처리 식별에 사용한다(부록 B·C 참고).

| 업무코드 | ServiceId | 거래코드 | 설명 |
| --- | --- | --- | --- |
| CC | CC.Code.selectList | CC-INQ-0001 | 공통코드 목록 조회 |
| IC | IC.Customer.selectIntegration | IC-INQ-0001 | 통합고객 조회 |
| PC | PC.Customer.selectPrivate | PC-INQ-0001 | 개인고객 조회 |
| BC | BC.Customer.selectBusiness | BC-INQ-0001 | 기업고객 조회 |
| MS | MS.Customer.selectSummary | MS-INQ-0001 | 고객 요약 조회(미니 SV) |
| SV | SV.Customer.selectSummary | SV-INQ-0001 | Single View 고객요약 조회 |
| PD | PD.Product.selectDetail | PD-INQ-0001 | 상품 상세 조회 |
| CM | CM.Campaign.selectList | CM-INQ-0001 | 캠페인 목록 조회 |
| EB | EB.Rule.checkEvent | EB-CHK-0001 | 이벤트 룰 검증 |
| EP | EP.Event.process | EP-EXE-0001 | 이벤트 실시간 처리 |
| BP | BP.Behavior.process | BP-EXE-0001 | 행동정보 처리 |
| BD | BD.Behavior.selectData | BD-INQ-0001 | 행동 데이터 조회 |
| SS | SS.Sales.selectSupport | SS-INQ-0001 | 영업지원 조회 |
| CS | CS.Common.selectService | CS-INQ-0001 | 공통서비스 조회 |
| CT | CT.Contents.selectList | CT-INQ-0001 | 콘텐츠 목록 조회 |
| MG | MG.Message.send | MG-SND-0001 | 메시지 발송 |
| OM | OM.User.inquiry | OM-ADM-0001 | 운영 사용자 조회 |

CM·CC 등 아직 독립 WAR가 없는 업무코드도 ServiceId·거래코드 표준은 동일하게 적용한다. 해당 로직이 다른 WAR 패키지에 임시 포함된 경우에도 Header `businessCode`와 ServiceId 접두어는 표준 업무코드를 유지한다.

---

## A.5 Mapper 표준

MyBatis Mapper는 업무코드 접두어와 디렉터리 규칙을 따른다. SQL ID는 ServiceId의 도메인·행위와 가능한 한 일치시켜 Handler → DAO → Mapper 추적을 단순화한다.

| 업무코드 | 대표 Mapper 클래스 | Mapper XML 위치 | 주요 역할 |
| --- | --- | --- | --- |
| CC | CcCodeMapper | mapper/cc/CcCodeMapper.xml | 공통코드 |
| IC | IcCustomerIntegrationMapper | mapper/ic/IcCustomerIntegrationMapper.xml | 통합고객 |
| PC | PcPrivateCustomerMapper | mapper/pc/PcPrivateCustomerMapper.xml | 개인고객 |
| BC | BcBusinessCustomerMapper | mapper/bc/BcBusinessCustomerMapper.xml | 기업고객 |
| MS | MsCustomerSummaryMapper | mapper/ms/MsCustomerSummaryMapper.xml | 고객 요약 |
| SV | SvSingleViewCustomerMapper | mapper/sv/SvSingleViewCustomerMapper.xml | 고객 종합조회 |
| PD | PdProductMapper | mapper/pd/PdProductMapper.xml | 상품정보 |
| CM | CmCampaignMapper | mapper/cm/CmCampaignMapper.xml | 캠페인 |
| EB | EbEbmRuleMapper | mapper/eb/EbEbmRuleMapper.xml | EBM Rule |
| EP | EpEventProcessingMapper | mapper/ep/EpEventProcessingMapper.xml | 이벤트 처리 |
| BP | BpBehaviorProcessingMapper | mapper/bp/BpBehaviorProcessingMapper.xml | 행동정보 처리 |
| BD | BdBehaviorDataMapper | mapper/bd/BdBehaviorDataMapper.xml | 행동 데이터 |
| SS | SsSalesSupportMapper | mapper/ss/SsSalesSupportMapper.xml | 영업지원 |
| CS | CsCommonServiceMapper | mapper/cs/CsCommonServiceMapper.xml | 공통 서비스 |
| CT | CtContentsMapper | mapper/ct/CtContentsMapper.xml | 콘텐츠 |
| MG | MgMessageMapper | mapper/mg/MgMessageMapper.xml | 메시지 |
| OM | OmOperationMapper | mapper/om/OmOperationMapper.xml | 운영관리 |

Mapper XML namespace는 보통 Mapper 인터페이스 FQCN과 동일하다. `<select id="selectSummary">`에 대응하는 SQL ID는 `SV.Customer.selectSummary` 형식으로 DAO에서 참조한다. 부록 E의 Mapper XML 템플릿을 복사해 업무코드별로 치환하는 것을 권장한다.

---

## A.6 사용 예시: SV 고객요약조회

SV(Single View) 업무의 고객 요약 조회 거래는 업무코드 표준이 계층 전체에 어떻게 전파되는지 보여주는 대표 사례이다.

| 항목 | 값 |
| --- | --- |
| 업무코드 | SV |
| Context | /sv |
| WAR | sv.war |
| Package | com.nh.nsight.marketing.sv |
| ServiceId | SV.Customer.selectSummary |
| TransactionCode | SV-INQ-0001 |
| processingType | INQUIRY |
| Handler | SvCustomerSummaryHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerRule |
| DAO | SvCustomerDao |
| Mapper | SvSingleViewCustomerMapper |
| Mapper XML | mapper/sv/SvSingleViewCustomerMapper.xml |
| SQL ID | SV.Customer.selectSummary |
| Error Code | E-SV-BIZ-0001 |

요청은 `POST /sv/online`으로 들어오고, Header에 `businessCode: SV`, `serviceId: SV.Customer.selectSummary`, `transactionCode: SV-INQ-0001`이 포함된다. Dispatcher는 `svCustomerSummaryHandler` Bean을 실행하고, Facade 이하에서 Rule 검증·DAO 조회가 수행된다. 거래로그에는 GUID, ServiceId, TransactionCode, businessCode가 함께 기록되어 OM 거래조회 화면에서 필터링할 수 있다.

---

## A.7 사용 예시: OM 사용자관리

OM(Operation Management)은 `tcf-om` 모듈로 배포되며 Context `/om`을 사용한다. 업무 WAR와 동일한 TCF 파이프라인(STF → Dispatcher → ETF)을 따른다.

| 항목 | 값 |
| --- | --- |
| 업무코드 | OM |
| Context | /om |
| WAR | tcf-om (om.war) |
| Package | com.nh.nsight.marketing.om |
| ServiceId | OM.User.inquiry |
| TransactionCode | OM-ADM-0001 |
| processingType | INQUIRY |
| Handler | OmUserInquiryHandler |
| Facade | OmUserFacade |
| Service | OmUserService |
| Rule | OmUserRule |
| DAO | OmUserDao |
| Mapper | OmUserMapper |
| Mapper XML | mapper/om/OmUserMapper.xml |
| Table | OM_USER |
| Error Code | E-OM-AUTHZ-0001 |

OM 거래는 관리자·감사 대상이 많으므로 Catalog에 `AUDIT_REQUIRED_YN`, `AUTH_REQUIRED_YN`을 명시적으로 등록한다. 권한 오류 시 `E-OM-AUTHZ-0001`처럼 업무코드가 포함된 표준 오류코드를 반환하고, 상세 권한 구조는 응답 Body가 아닌 서버 로그·감사로그에만 남긴다.

---

## A.8 개발 시 준수사항

| 구분 | 준수사항 |
| --- | --- |
| 신규 업무 추가 | 업무코드 표준표에 먼저 등록한 뒤 개발·배포한다 |
| Context 정의 | 업무코드 소문자와 Context Path를 일치시킨다 (`/sv` ↔ SV) |
| WAR 생성 | WAR 파일명은 업무코드 소문자 기준 (`sv.war`) |
| Package 작성 | Root Package는 `com.nh.nsight.marketing.{code}` |
| ServiceId 작성 | 업무코드 대문자로 시작 (`SV.Customer.selectSummary`) |
| 거래코드 작성 | `{업무코드}-{거래유형}-{4자리 일련번호}` |
| Mapper 작성 | 클래스·XML 모두 업무코드 PascalCase/소문자 디렉터리 준수 |
| 오류코드 작성 | `E-{업무코드}-...` 표준 사용 |
| 로그 기록 | GUID, ServiceId, businessCode, transactionCode 동시 기록 |
| 임의 약어 금지 | 팀별 임의 코드·별칭·snake_case 업무코드 사용 금지 |

표준표에 없는 업무코드를 Header에 넣으면 STF 또는 거래통제 단계에서 차단된다. 테스트 환경에서만 쓰는 임시 코드도 Catalog `DRAFT` 상태로 등록하고, 운영 반영 전 `ACTIVE`로 전환한다.

---

## 요약

업무코드 17개(CC~OM)는 NSIGHT TCF 식별 체계의 루트이며, Context·WAR·Package·ServiceId·거래코드·Mapper·오류코드·로그가 모두 이 기준에 정렬된다. 현재 코드베이스는 9개 업무 WAR와 `tcf-om`이 배포되나, 신규 개발·명명은 전체 표준표를 따른다. 업무코드를 먼저 확정하지 않고 Handler나 Mapper부터 작성하면 운영 추적·Catalog·배포 스크립트 전반에서 재작업이 발생한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [32-Gap-보완-향후-과제](../제10편/32-Gap-보완-향후-과제.md) |
| → 다음 | [부록 B](./B-ServiceId-명명규칙.md) |

---

## 출처 색인

| 참고 | 경로 |
| --- | --- |
| NSIGHT TCF 개발 매뉴얼 (원본) | `znsight-guide-word/통합 (72).docx` |
| znsight-man 부록 A | `znsight-man/부록A-업무코드-표준표.md` |
| 애플리케이션 명명 규칙 | `zdoc/applicationNaming.md` |
| WAR 배포 스크립트 | `ztomcat/deploy-wars.sh` |
