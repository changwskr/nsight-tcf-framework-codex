'use strict';

/** @returns {Record<string, string>} key -> markdown (## Master 해설 section body ONLY, no ## heading) */
function buildNarratives() {
  return {
    '서문/00-서문': `NSIGHT TCF 문서 체계는 Word 설계안, zman, zarchitecture, ztcfbook 세 계층으로 쌓여 있으며, ztcfbook-h Master Edition은 이 위에 코드베이스 샘플과 아키텍트 관점 해설을 얹은 최상위 레이어입니다. 아키텍트와 시니어는 본문 절 번호만으로는 런타임 동작을 추적하기 어렵기 때문에, STF→Dispatcher→Handler→ETF 파이프라인과 OM Catalog·Gateway 라우팅을 문서 간 교차 참조로 묶어 읽어야 합니다.

설계서에는 17개 업무코드가 정의되어 있으나 nsight-tcf-framework에는 9개 업무 WAR와 tcf-om·tcf-gateway 등 플랫폼 모듈이 실제 배포됩니다. 이 Gap은 장애 분석 시 "설계에는 있는데 코드에 없는 것"과 "코드에만 있는 OM Handler"를 구분하는 데 핵심이며, 제10편과 zman/00-설계서-코드베이스-대조표가 판단 근거가 됩니다.

역할별 읽기 경로(업무 개발자·플랫폼·OM·DevOps)는 문서 중복을 줄이기 위한 설계이지만, 운영 장애는 경계를 넘어 발생합니다. Gateway JWT 검증 실패가 STF 5단 AuthenticationContextValidator까지 전파되는 식의 End-to-End 추적 능력이 Master Edition의 실질적 가치입니다.

코드 리뷰·운영 인수인계 시에는 ztcfbook-h의 구현 샘플 경로가 최신 main 브랜치와 일치하는지, Deep Dive 체크리스트가 MR evidence로 남았는지 확인하십시오. 문서만 읽고 curl /{bc}/online E2E를 생략하면 Catalog·거래통제 누락을 놓치기 쉽습니다.`,

    '제01편/01-NSIGHT-TCF란-무엇인가': `NSIGHT TCF는 REST Resource Controller 대신 Online Endpoint(\`/{businessCode}/online\`) 하나로 모든 업무 거래를 수용하는 프레임워크입니다. ServiceId가 TransactionDispatcher의 라우팅 키이고, 거래코드는 감사·거래통제·Catalog 메타데이터로 쓰이므로 두 식별자의 역할 분리를 이해하지 못하면 설계 단계부터 Handler와 OM 등록이 어긋납니다.

클라이언트 요청은 tcf-gateway 또는 tcf-ui Relay를 거쳐 업무 WAR에 도달하고, OnlineTransactionController가 StandardRequest를 받아 TCF.process()를 호출합니다. 이후 STF 전처리, TimeoutExecutor, Dispatcher, Handler, ETF 후처리까지 한 파이프라인으로 귀결되므로 "Controller에서 비즈니스 로직 처리"는 프레임워크 계약 위반입니다.

bootRun 단독 개발과 ztomcat WAR 통합 배포의 이중 모델은 프로파일·Context Path·Gateway downstream URL 차이를 만듭니다. 로컬에서 통과한 거래가 ztomcat 8080 통합 환경에서 실패하는 경우, 대부분 Gateway route·session-validation·OM H2 seed 불일치에서 기인합니다.

아키텍트 리뷰에서는 신규 거래마다 OM Service Catalog 등록, Handler serviceIds() 선언, Facade @Transactional 경계 세 가지가 동시에 충족되는지 확인해야 합니다. Handler만 추가하고 Catalog를 빼면 런타임 SERVICE_NOT_FOUND 또는 거래통제 E-TCF-CTL-* 오류가 운영 직후에 터집니다.`,

    '제01편/02-전체-시스템-구조': `GSLB·Apache·tcf-gateway·업무 WAR·tcf-om으로 이어지는 물리 배치는 장애 격리와 확장 단위를 결정합니다. Gateway(:8100)가 \`/{bc}/online\` 프록시를 담당하고, tcf-ui(:8099)는 개발 Relay, tcf-uj(:8102)는 Gateway 경유 UI 채널로 분리되어 있어 "UI에서 되는데 Gateway 경유는 안 된다" 유형의 이슈는 라우팅 계층부터 분해해야 합니다.

9개 업무 WAR(ic·pc·ms·sv·pd·eb·ep·ss·mg)와 플랫폼 모듈은 Gradle 멀티모듈 settings.gradle이 SoT이며, 포트·Context Path·WAR 파일명은 부록 K와 BusinessModuleDefinitions에서 1:1로 맞춰져 있습니다. 업무 WAR는 tcf-web을 transitively 의존하므로 tcf-core STF/TCF 변경은 전 WAR에 파급됩니다.

채널별 통계와 거래통제는 StandardHeader channelId·businessCode와 Gateway ROUTING_TABLE 정합에 달려 있습니다. 외부 REST 연계를 추가할 때 Online Endpoint 규약을 깨고 ad-hoc Controller를 만들면 STF Header 검증·TxLog·MDC 추적이 모두 우회됩니다.

운영 점검 시 ztomcat deploy-wars.sh ALL_MODULES 스모크, Gateway downstream health, OM Dashboard feed(tcf-batch) 수신 여부를 주기적으로 확인하십시오. 포트 충돌이나 Context Path 오타는 기동은 되지만 Proxy 404로만 드러나는 경우가 많습니다.`,

    '제01편/03-TCF-처리-엔진': `TCF.process()는 STF preProcess → TimeoutExecutor → TransactionDispatcher → ETF postProcess의 고정 순서를 강제합니다. STF 10단(Header 검증, GUID/TraceId, TransactionContext·MDC, Session, Auth, Authorization, 거래통제, Timeout, Idempotency, TxLog.start) 중 하나라도 실패하면 Handler와 DB 트랜잭션은 실행되지 않습니다. 이 "선 검증 후 실행" 모델이 프레임워크의 안전장치입니다.

TransactionDispatcher는 ServiceId→TransactionHandler 레지스트리에서 Handler를 찾고, 중복 serviceId 등록 시 IllegalStateException으로 기동을 중단합니다. Handler는 body Object를 반환할 뿐 StandardResponse를 조립하지 않으며, ETF가 success·businessFail·systemError 세 경로로 result·errorCode·TxLog.end·audit·metric을 일괄 처리합니다.

clientHeader 스냅샷 echo 규칙은 응답 Header가 요청 Header와 필드 단위로 대응되도록 ETF에 위임되어 있습니다. finally 블록에서 ContextHolder·AuthHolder·TimeoutHolder·MDC clear가 누락되면 스레드 풀 reuse 시 세션·권한 정보가 다음 거래로 유출되는 치명적 결함이 됩니다.

코드 리뷰에서는 TimeoutPolicyService가 STF 8단에서 resolve된 값과 OnlineTransactionTimeoutExecutor 적용 구간이 일치하는지, 장애 추적 시 guid/traceId와 TCF_TX_LOG INSERT가 모든 WAR에서 동작하는지 검증하십시오. STF 단계별 BusinessException과 E-TCF-HDR-* 오류코드 매핑도 ETF 경로와 대조해야 합니다.`,

    '제01편/04-애플리케이션-6계층': `Handler→Facade→Service→Rule→DAO→Mapper 6계층은 HTTP 진입(OnlineTransactionController) 이후 업무 코드의 유일한 허용 구조입니다. Handler는 serviceIds() 선언과 switch 분기만 담당하고 @Transactional을 붙이면 안 되며, 트랜잭션 경계는 Facade에만 둡니다.

Rule 계층은 순수 로직·부수효과·DB 접근 금지가 원칙입니다. Service에서 Rule을 호출해 검증하고, DAO는 MyBatis Mapper만 호출합니다. Mapper XML namespace는 DAO FQCN과 일치해야 Spring-MyBatis 바인딩이 깨지지 않습니다.

Controller를 업무 WAR에 만들지 않는 이유는 STF~ETF 공통 파이프라인 일원화입니다. 예외적으로 파일 업로드 등은 tcf-web 또는 OM Handler 패턴을 따르며, RESTful URL 설계와 Online Endpoint 혼용은 Gateway 라우팅 표를 복잡하게 만듭니다.

리뷰 시 SvCustomerHandler 같은 표준 샘플과 diff를 비교해 계층 역전(Handler에서 Mapper 직접 호출, Service에서 HttpSession 접근)이 없는지 확인하십시오. Facade @Transactional(readOnly=true) 조회·쓰기 혼합 메서드 분리도 데이터 정합성 관점에서 필수입니다.`,

    '제02편/05-개발-표준-총정리': `개발 표준 12축(업무코드→Context/WAR→패키지 com.nh.nsight.marketing.{bc}→ServiceId→Handler→OM Catalog→배포)은 신규 모듈마다 동일 순서로 적용됩니다. 한 축이라도 건너뛰면 "코드는 merge됐는데 운영 불가" 상태가 됩니다.

Gradle 모듈명·WAR명·Context Path·Gateway ProxyController 접두가 businessCode와 정합해야 STF Header 7항 검증과 URL Allow-List가 통과합니다. Git MR·Commit 메시지 규칙은 Catalog 등록·통제 등록 evidence와 짝을 이루도록 설계되어 있습니다.

표준 17업무코드와 배포 9 WAR 차이는 설계 확장성과 구현 범위의 의도적 Gap입니다. CC·BC·CM 등 미배포 코드를 Header에 넣으면 통제 정책·라우트 부재로 실패하므로, 부록 A와 zarchitecture/04를 항상 함께 봐야 합니다.

아키텍트 관점 MR 게이트에서는 settings.gradle 모듈 추가 시 deploy-wars.sh·ROUTING_TABLE·BusinessModuleDefinitions 동시 갱신 여부를 Blocker로 두는 것이 좋습니다. 패키지 루트가 marketing.{bc}에서 벗어나면 Handler 스캔·Mapper 경로가 어긋납니다.`,

    '제02편/06-식별자-명명규칙': `ServiceId \`{BC}.{Domain}.{action}\`은 TransactionDispatcher registry 키이며, action 표준(selectSummary, save, update, delete 등)을 따르지 않으면 OM Catalog 검색·운영자 교육 비용이 커집니다. 거래코드 \`{BC}-{TYPE}-{NNNN}\`은 Header transactionCode·감사·거래통제·Catalog 상태와 연결됩니다.

OM prefix \`OM.*\`(예: OM.Auth.login)와 업무 \`SV.*\`·\`IC.*\`는 Handler 패키지와 WAR 배치가 다릅니다. Gateway route \`/{bc}/online\`의 bc와 Header businessCode 불일치는 STF TransactionControlService에서 E-TCF-CTL-* 또는 E-TCF-HDR-*로 즉시 거절됩니다.

화면번호·menuId·functionCode·channelId는 AuthorizationValidator와 OM functionAuth seed와 삼각 정합을 이룹니다. Gateway·Batch·Cache 명명(명명규칙 18~20)은 플랫폼 모듈 간 설정 충돌을 막기 위한 별도 축입니다.

코드 리뷰에서 Handler serviceIds() 배열·switch case·OM Catalog service_id 컬럼 세 곳의 문자열 완전 일치를 diff 도구로 확인하십시오. 대소문자·도메인명 오타는 기동 시점 duplicate가 아니면 런타임 SERVICE_NOT_FOUND로만 나타납니다.`,

    '제02편/07-코드-DB-명명규칙': `Java 클래스 suffix(Handler, Facade, Service, Rule, Dao)와 DTO 유형(RequestBody, ResponseBody 등)은 정적 분석·코드 검색·온보딩 비용을 좌우합니다. Method·Field camelCase와 StandardHeader·MDC 필드명 동기는 로그 상관관계 분석에 직접 영향을 줍니다.

MyBatis Mapper namespace = DAO fully qualified name, SQL ID camelCase 규칙을 어기면 "Invalid bound statement"가 기동 후 첫 쿼리에서 터집니다. TCF_*·OM_* 테이블·컬럼·인덱스 명명은 schema.sql과 docs/architecture/19-tcf-table.md가 SoT입니다.

ErrorCode \`E-{BC|COM|TCF}-*\`는 BusinessException→ETF.businessFail→StandardResponse.result.errorCode 경로와 OM OmErrorCodeHandler CRUD가 연결됩니다. 임의 RuntimeException 메시지만 던지면 systemError 경로로 떨어져 운영 메시지 품질이 떨어집니다.

리뷰·운영에서는 로그·감사로그 항목이 명명규칙-16과 일치하는지, SQL에 \${} 대신 #{} 바인딩을 쓰는지, 업무 E-SV-* 코드가 부록 F에 등록·OM 반영됐는지 확인하십시오.`,

    '제03편/08-거래-설계': `거래 설계 단계에서 ServiceId·거래코드·Header 7 Allow-List·processingType·channelId·Timeout 정책을 한 세트로 확정해야 STF와 OM이 동일 전제로 동작합니다. Handler 구현 전 Catalog·거래통제·Timeout OM 등록 순서를 문서화하지 않으면 개발 완료 후 운영 반려가 반복됩니다.

TransactionControlService는 STF 7단에서 OM TCF_TX_CONTROL 메타를 lookup하여 URL·businessCode·채널·점포·시간대 정책을 적용합니다. TimeoutPolicyService는 STF 8단에서 resolve되어 OnlineTransactionTimeoutExecutor에 전달됩니다. 설계서의 Timeout만 적고 OM 미등록이면 기본값 또는 무제한으로 Handler가 실행될 수 있습니다.

화면–ServiceId 1:N 매핑은 UI tcf-ui·tcf-uj와 functionAuth 설계와 맞물립니다. 한 화면에서 여러 ServiceId를 호출할 때 transactionCode·processingType(CREATE/UPDATE/INQUIRY) 구분이 감사로그 품질을 결정합니다.

설계 리뷰 체크: OM Catalog status, 거래통제 allow channel, Timeout ms, Idempotency 필요 여부, tcf-eai 연동 시 대상 WAR ServiceId. curl 샘플 JSON header 필드 7항이 설계서와 byte-level로 일치하는지 E2E 전에 검증하십시오.`,

    '제03편/09-표준-전문과-DTO': `StandardRequest(header+body)와 StandardResponse(header+result+body)는 tcf-core message 패키지와 ETF 조립 규칙으로 고정됩니다. Header echo는 clientHeader 스냅샷을 ETF가 처리하므로 Handler에서 header를 수동 덮어쓰면 안 됩니다.

Body는 Map 또는 typed DTO로 Facade 진입 전후 변환할 수 있으나, Validation은 znsight-man DTO 기준과 Hibernate Validator 어노테이션으로 일관되게 적용해야 STF 이후 단계에서 NPE·형변환 오류가 줄어듭니다. Idempotency-Key 헤더는 STF 9단 IdempotencyChecker와 InMemory/DB 구현체에 연동됩니다.

result.success, result.errorCode, result.errorMessage는 ETF 세 경로(success/businessFail/systemError)에서만 설정됩니다. Handler가 errorCode를 직접 Response에 넣는 패턴은 금지입니다.

리뷰 시 부록 D JSON·tcf-ui sample-requests·실제 Handler 입출력 타입을 삼向 대조하고, 중복 요청 시 Idempotency 동작·processing 표시, 오류 시 E-TCF-* vs E-SV-* 코드가 부록 F와 일치하는지 확인하십시오.`,

    '제03편/10-TransactionHandler-개발': `TransactionHandler는 @Component로 등록되고 serviceIds()로 담당 ServiceId를 선언하며, handle() 내부 switch에서 Facade로 위임합니다. default 분기는 SERVICE_NOT_FOUND BusinessException으로 ETF까지 일관된 오류 응답을 보장해야 합니다.

도메인당 1 Handler 파일에 여러 ServiceId를 모으는 패턴이 표준입니다(SvCustomerHandler 참조). Handler에 @Transactional·DAO·Mapper 주입은 계층 위반입니다. WAR 간 동작은 REST가 아니라 tcf-eai TcfServiceClient가 StandardRequest POST로 \`/{targetBc}/online\`을 호출합니다.

Online Endpoint 호출 규약(POST, Content-Type application/json, Header 7항)은 tcf-ui Relay·Gateway Proxy·curl 스크립트 모두 동일합니다. ic-service와 sv-service 연동 데모는 EAI 헤더 전파·Timeout 중첩 이슈를 검증하는 좋은 샘플입니다.

MR evidence: serviceIds()와 switch case 완전성, Facade 트랜잭션 경계, Rule 테스트, Mapper SQL EXPLAIN(필요 시). OM Catalog·통제·Timeout 등록 스크린샷 또는 SQL seed diff를 PR에 첨부하는 것을 권장합니다.`,

    '제03편/11-품질-속성-구현': `BusinessException, Timeout, 기타 Exception은 ETF businessFail·businessFail(timeout)·systemError로 수렴합니다. 예외 타입별 TxLog.end 코드(S0000, E0001, E9999 등)와 audit·metric emission이 ETF에서 공통 처리되므로 Handler catch 블록에서 임의 JSON을 만들면 관측성이 깨집니다.

거래로그는 STF TxLog.start와 ETF end가 쌍을 이루며, TCF_TX_LOG는 OM H2(로컬) 또는 운영 LOGDB에 INSERT됩니다. 감사로그는 민감 거래(CREATE/UPDATE/DELETE processingType)에 선택 적용됩니다. Cache는 tcf-cache SPI와 OM cache evict Handler로 무효화 전략을 맞춥니다.

파일 업·다운로드는 OM Handler 및 znsight-man 44장 기준을 따르며, Online Endpoint body와 별도 스트림 API 혼용 시 Gateway timeout과 맞지 않을 수 있습니다. 트랜잭션 경계는 Facade 단일 메서드 단위가 원칙입니다.

운영 이슈 다발: Timeout OM 값과 실제 SQL slow query 불일치, Idempotency processing stuck, Cache stale after Catalog 변경. 코드 리뷰에서 BusinessException errorCode 필수·로그 마스킹·@Transactional rollbackFor 확인하십시오.`,

    '제04편/12-세션-로그인-권한': `Spring Session과 SESSIONDB(또는 로컬 H2 session table)는 OM.Auth.login·ssoLogin·logout·session ServiceId와 STF 4단 SessionValidator에 연결됩니다. 로컬 profile에서 tcf.session-validation-enabled=false로 끄면 개발 속도는 올라가나 운영과 auth path가 달라집니다.

STF 5단 AuthenticationContextValidator와 6단 AuthorizationValidator는 OM functionAuth·dataAuth 메타를 runtime lookup합니다. OmAuthHandler·OmFunctionAuthHandler seed(data.sql, seed-function-auth.sql)가 없으면 로그인 후 모든 거래가 권한 오류로 실패합니다.

업무 WAR에서 HttpSession 직접 접근은 금지에 가깝고, AuthHolder·TransactionContext를 통해야 테스트·Gateway JWT path와 일관됩니다. menuId·functionCode Header는 UI와 OM 등록이 일치해야 합니다.

리뷰·운영: login E2E 후 SV 거래 Authorization 통과, session timeout·동시 로그인 정책, prod SESSIONDB 연결. JWT-only 채널과 session 채널 병행 시 STF 단계별 skip/profile 설정을 tcf-cicd yml에서 추적하십시오.`,

    '제04편/13-JWT-SSO-Gateway': `tcf-jwt(:8110)는 JWKS(\`/.well-known/jwks.json\`)를 제공하고, tcf-gateway(:8100) GatewayJwtValidator가 Bearer 토큰을 검증한 뒤 업무 WAR \`/{bc}/online\`으로 프록시합니다. auth.jwt.enabled는 profile별로 다르며, 로컬 Relay-only와 ztomcat 통합 검증을 구분해야 합니다.

JWT claim과 StandardHeader(userId, channelId 등) 정합은 STF AuthenticationContextValidator와 맞물립니다. Session+JWT 이중 auth path는 tcf-web JWT Filter(znsight-man 80)와 Gateway GEF 계층에서 순서가 정의되어 있습니다. tcf-uj는 Gateway 경유 UI로 채널 ID·CORS·cookie 정책이 tcf-ui Relay와 다릅니다.

운영에서 흔한 장애: JWKS fetch 실패(cache stale), clock skew, claim businessCode와 route bc mismatch, Gateway ROUTING_TABLE 미갱신. E-JWT-AUTH-* 오류코드는 부록 F와 Gateway 로그에서 상관 분석합니다.

점검: ztomcat context /gw·/jwt, SvProxyController 등 AbstractBusinessProxyController 상속 일관성, prod secret rotation 후 Gateway restart, SSO 연계 시 logout·token revoke 경로 E2E.`,

    '제04편/14-거래통제-정책': `Header 7 Allow-List(businessCode, transactionCode, channelId, processingType 등)는 TransactionControlService가 STF 7단에서 OM TCF_TX_CONTROL과 대조합니다. URL \`/{bc}/online\`과 Header businessCode 불일치, 미등록 channelId, 허용 시간 외 호출은 E-TCF-HDR-*·E-TCF-CTL-*로 거절됩니다.

OM 거래통제 등록(OmTransactionControlHandler)은 Catalog·Timeout과 함께 신규 거래 출시 3종 세트입니다. 운영자가 OM UI에서 정책 변경 시 runtime cache evict API 호출 여부를 확인하지 않으면 구 정책이 남는 incident가 발생합니다.

채널·점포·URL prefix 정책은 Gateway STF/GRF와 업무 WAR STF가 이중 검증하는 경우가 있어, 설계 변경 시 양쪽 yml과 OM row를 동시에 갱신해야 합니다.

코드 리뷰·운영 전환: Catalog service_id 존재, TCF_TX_CONTROL allow row, TimeoutPolicy row, curl negative test(잘못된 channelId). prod OM seed와 dev diff를 부록 J 체크리스트로 승인받으십시오.`,

    '제05편/15-OM-아키텍처와-개발': `tcf-om(:8097, context /om)은 24개 Om*Handler와 nsight_om H2(로컬)·OMDB(운영) schema.sql 23테이블로 운영 메타의 Single Source of Truth입니다. om-service 레거시는 폐기되었으며, Catalog·거래통제·Timeout·Auth·Dashboard·ErrorCode·Cache evict가 모두 Handler+ServiceId 패턴입니다.

업무 WAR STF는 기동 시·거래 시 OM 메타를 lookup합니다. tcf-om 기동 seed(data.sql)와 cache evict API는 Catalog 변경 propagation의 핵심입니다. OmServiceCatalogHandler·OmTransactionControlHandler·OmTimeoutPolicyHandler가 플랫폼·업무 거래 모두에 영향을 줍니다.

운영 Dashboard·헬스체크·tcf-batch metrics feed는 docs/architecture/44-observability와 연결되나, Gap 항목(제32장)으로 완전 자동화는 미완입니다. OM 화면↔Handler↔Word 설계 절 매핑은 제31장 표를 따릅니다.

아키텍트 점검: schema.sql migration 순서, Handler serviceIds OM.* prefix, prod OMDB backup·seed-function-auth, 장애 시 OM unavailable가 전 WAR 거래 거절로 이어지는 blast radius. OM read replica 또는 cache TTL 전략을 DR 계획에 포함하십시오.`,

    '제05편/16-API-Gateway-UI-채널': `tcf-gateway는 STF/GRF/GSF/GEF 4계층으로 외부 요청을 필터링·라우팅·프록시합니다. GatewayRouteDispatcher와 businessCode별 ProxyController(SvProxyController 등)가 downstream \`http://host:port/{bc}/online\`을 호출합니다. ROUTING_TABLE.md와 tcf-cicd profile yml이 SoT입니다.

tcf-ui는 TransactionRelayService로 개발 중 bootRun WAR에 직접 POST(gateway-relay-enabled=false)하거나 Gateway 경유(true)를 profile로 전환합니다. tcf-uj(:8102)는 Browser→Gateway→WAR 경로를 운영 UI에 가깝게 재현합니다. channelId Header는 채널별 통계·통제·로그 집계 키입니다.

Apache·Spring 이중 라우팅(zarchitecture/06) 환경에서는 path prefix(/gw)와 cookie domain 불일치가 세션 단절을 유발합니다. 외부 REST 연계 시 Online Endpoint 규약 유지 여부가 STF 적용 범위를 결정합니다.

운영: Gateway auth.jwt.enabled, downstream connection pool, Proxy timeout ≤ OM TimeoutPolicy. UI Relay sample JSON과 Gateway curl E2E를 릴리즈마다 smoke. channelId 신규 추가 시 OM 통제·functionAuth 동시 등록.`,

    '제05편/17-Batch-Scheduler-이벤트': `tcf-batch Scheduler(SessionStatusCollectScheduler 등)는 OM Dashboard metrics·헬스 feed를 비동기로 공급합니다. 온라인 TCF 파이프라인(STF~ETF)과 lifecycle·장애 모드가 분리되어 있어, batch down이 online 거래를 막지는 않지만 observability blind spot이 생깁니다.

eb-service(EbEventHandler)는 이벤트 브릿지·배치성 Handler, ep-service(EpUserEventHandler)는 실시간 이벤트 처리 WAR입니다. tcf-eai 동기 POST와 eb/ep 비동기는 역할 분리이며, 동일 도메인 로직을 두 경로에 중복 구현하면 정합성 incident가 납니다. Batch Job ID는 명명규칙-20을 따릅니다.

Gateway EbProxy 등 이벤트 ingress는 REST처럼 보일 수 있으나 내부적으로는 OM·EP Handler로 귀결됩니다. Scheduler cron과 OM maintenance window 정합도 운영 이슈입니다.

점검: batch log·OM dashboard 수치 갱신, eb→ep 이벤트 E2E, Job failure alert( Gap: 자동 alert 미완). 코드 리뷰에서 online Handler에 @Scheduled 혼입 금지.`,

    '제05편/18-데이터-DB-아키텍처': `RDW·ADW(업무), OMDB·nsight_om(H2 로컬), LOGDB(TCF_TX_LOG), SESSIONDB로 데이터stores가 분리됩니다. sv-service 등 업무 WAR는 RDW에 MyBatis DAO만 사용하며 JPA 업무 엔티티는 표준 밖입니다. OM 23테이블(schema.sql)은 Catalog·통제·Auth·공통코드·ErrorCode를 담습니다.

TCF_TX_LOG는 모든 WAR가 INSERT하는 cross-cutting table로, STF start·ETF end와 correlation guid가 핵심 컬럼입니다. Spring Session table은 SESSIONDB에 별도 존재합니다. application.yml datasource·profile은 tcf-cicd local/dev/prod SoT와 sync해야 합니다.

데이터 거버넌스·마스킹은 OM DataAuth Handler와 znsight-man 로그 기준에 따릅니다. Facade에서 RDW+OMDB 이중 datasource 트랜잭션은 2PC 없이 분리하는 것이 원칙입니다.

리뷰·운영: Hikari pool size, slow query on TxLog INSERT, H2→운영 DB migration script, PII mask in Mapper result. 장애 시 OMDB readonly fallback 정책(미구현 Gap)을 DR 문서에 명시하십시오.`,

    '제06편/19-로컬-개발환경': `JDK 21·Gradle·IDE·application-local.yml·H2(nsight_om+TxLog) 구성은 sv-service bootRun만으로도 STF~ETF·Handler·Mapper까지 검증 가능하게 설계되었습니다. tcf.session-validation-enabled=false, InMemory Idempotency는 속도와 운영 fidelity tradeoff입니다.

profile local/dev/prod와 application-tcf.yml fragment는 tcf-cicd가 SoT이며, sv-service/src/main/resources와 tcf-cicd/local/spring/sv-service/application-local.yml diff를 주기적으로 맞춥니다. seed-function-auth.sql로 로컬 권한 없이 막히는 문제를 줄입니다.

ztomcat 8080 ALL_MODULES는 Gateway·JWT·9 WAR·OM 통합 smoke용입니다. bootRun 단독 통과 ≠ ztomcat 통과이므로, 릴리즈 전 deploy-wars.sh와 curl-sv-sample.sh를 ztomcat 대상으로 실행하십시오.

온보딩 체크: Gradle wrapper, H2 console(필요 시), tcf-ui Relay URL, 포트 충돌(부록 K). application.yml에 secret hardening 금지. IDE run config active profile=local 확인.`,

    '제06편/20-CICD-릴리즈-DR': `tcf-cicd manifest.yaml·cicd-build.sh·cicd-deploy.sh는 WAR 빌드·profile별 yml sync·Tomcat·Apache 배포 절차의 SoT입니다. MR→Build→Test→WAR→Deploy→Smoke 실패 시 Rollback WAR pin은 znsight-man 67~68·부록 J와 연결됩니다.

13 WAR(ztomcat deploy-wars.sh ALL_MODULES) 배포 순서와 Gateway route 갱신 누락이 통합 smoke fail의 주원인입니다. Git branch·MR 품질 게이트(부록 I·H)는 Catalog OM prod seed evidence를 요구합니다.

DR·observability 자동화는 docs/architecture/45·44와 제32장 Gap에 명시된 미완 영역입니다. 릴리즈 전략 docs/49-release-strategy와 hotfix branch 규칙을 함께 적용하십시오.

DevOps 점검: cicd-build.sh exit code, manifest module list vs settings.gradle, prod smoke curl through Gateway, rollback drill 분기별. 장애 FAQ znsight-man 69~70을 runbook과 링크.`,

    '제07편/21-테스트-전략': `테스트 3층( Handler/Service 단위 → /online 통합 → curl JSON E2E )은 STF~ETF 포함 여부를 명시적으로 요구합니다. MockMvc Controller 테스트는 Online Endpoint 아키텍처와 맞지 않으므로 TCF.process 또는 @SpringBootTest + OnlineTransactionController 호출 패턴을 씁니다.

MyBatis SQL·보안·성능·장애 테스트는 MR 품질 게이트(znsight-man 57~60)와 연동됩니다. sv-sample-inquiry.json·curl-sv-sample.sh는 회귀 테스트 artifact로 tcf-scripts에 유지합니다.

부록 H·I 체크리스트는 "테스트 evidence 없는 MR 승인 금지" Blocker로 사용할 수 있습니다. Idempotency·Timeout·거래통제 negative case는 E2E에서 필수입니다.

아키텍트 리뷰: 통합 테스트가 TransactionControlService mock 없이 OM H2 seed를 쓰는지, TxLog assertion, Gateway path smoke(optional CI stage). 커버리지 숫자보다 Catalog·통제 등록 E2E 통과가 출시 기준입니다.`,

    '제08편/22-조회-거래-SV-고객요약': `SV.Customer.selectSummary는 nsight-tcf-framework 표준 실습 거래로, POST /sv/online → SvCustomerHandler → SvCustomerFacade → SvCustomerMapper → StandardResponse까지 E2E 교재 역할을 합니다. ServiceId·거래코드·JSON sample·OM Catalog row가 서로 reference implementation입니다.

조회 processingType(INQUIRY)은 @Transactional(readOnly=true) Facade와 count 없는 단건/요약 SQL 패턴을 보여줍니다. STF~ETF 전 구간이 실행되므로 curl 한 번으로 TxLog·Header echo·result.success 검증이 가능합니다.

OM 등록(Catalog·통제·Timeout) 없이 Handler만 있으면 STF 7단 또는 Dispatcher에서 실패합니다. tcf-ui Relay로 UI에서 동일 JSON 재현 시 channelId·session cookie 이슈를 분리 테스트할 수 있습니다.

실습 확장 시 ic-service 연동·Gateway 경유·JWT Bearer를 단계별로 추가하며 회귀하십시오. 코드 리뷰 baseline으로 SvCustomer* 파일 구조를 다른 BC에 복제할 때 diff가 최소화되도록 하십시오.`,

    '제08편/23-목록-페이징-등록-변경': `목록 조회는 count SQL과 select SQL 분리·offset/limit 페이징(znsight-man 72)이 표준이며, Mapper XML dynamic where와 Facade에서 PageDto 조립 패턴을 따릅니다. 등록·변경(save/update)은 CREATE/UPDATE processingType과 Facade @Transactional 쓰기 경계·감사로그 대상입니다.

TcfServiceClient를 통한 WAR 간 호출(znsight-man 74)은 StandardRequest POST·Header 전파·Timeout 중첩을 이해하는 실습입니다. ic↔sv 데모는 tcf-eai contract docs/46-service-integration-contract와 맞춥니다.

Validation Rule + BusinessException(E-SV-*) 패턴, 파일 다운로드·Batch Job 샘플(75~76)은 온라인 Handler 패턴의 변형이므로 동일 STF~ETF 전제를 유지합니다.

리뷰: 페이징 total count 정합, duplicate key business error, EAI failure rollback, Idempotency on save. SQL injection in dynamic sort/order by whitelist.`,

    '제09편/24-tcf-core-web-util': `tcf-core는 STF·TCF·ETF·Dispatcher·TransactionControl·Timeout·Idempotency·ErrorCode의 프레임워크 kernel이고, tcf-web은 OnlineTransactionController·NsightWarBootstrap·Security autoconfig를 제공합니다. tcf-util은 HTTP·Gateway session helper 등 cross-cutting util입니다.

모든 업무 *-service WAR는 tcf-web에 의존하므로 tcf-core minor 변경도 전 WAR regression을 요구합니다. TcfCoreAutoConfiguration·TcfWebAutoConfiguration 순서와 AOP Timeout·@Transactional Facade pointcut이 기동 로그에서 conflict 없이 올라와야 합니다.

프레임워크 수정 시 TCF.process() finally MDC clear, STF 10단 순서 변경은 breaking change로 취급하고 zman·docs/33~36 동시 갱신하십시오.

플랫폼 MR은 tcf-core 단위 테스트·sv-service integration·ztomcat smoke를 최소 regression으로 요구합니다. tcf-util 변경은 Gateway·UI compile만으로는 부족하고 업무 WAR bootRun으로 STF~ETF 경로를 확인해야 합니다.`,

    '제09편/25-tcf-om-ui-uj': `tcf-om의 24개 Om*Handler는 Service Catalog·거래통제·Timeout·Auth·Dashboard 등 운영 메타의 Single Source of Truth API입니다. tcf-ui(:8099)는 TransactionRelayService와 sample JSON·Admin HTML로 bootRun WAR에 직접 POST하는 개발 Relay를, tcf-uj(:8102)는 Browser→Gateway→업무 WAR 경로를 재현하는 운영 유사 UI 채널입니다.

gateway-relay-enabled 프로파일 한 줄 전환이 개발 편의와 운영 fidelity tradeoff를 결정하며, 장애 triage 시 "UI Relay 경로 vs uj Gateway 경로"를 먼저 분리해야 합니다. OmServiceCatalogHandler 등 OM.* ServiceId는 marketing.om 패키지에만 존재하고, BusinessModuleDefinitions의 bc→port→context 하드코딩은 신규 WAR 추가 시 tcf-ui·Gateway·ztomcat 동시 수정을 요구합니다.

OM Catalog 변경 후 cache evict API 미호출 시 업무 WAR STF 7~8단 lookup이 stale policy를 읽는 incident가 발생합니다. uj의 CORS·cookie·Gateway /gw prefix 정합, tcf-ui direct bootRun port mismatch도 통합 smoke 전에 확인합니다.

리뷰·운영: Relay header 7항 completeness, OM Handler switch default branch, prod profile Admin endpoint 노출 차단.`,

    '제09편/26-tcf-gateway-jwt': `tcf-gateway GatewayRouteDispatcher와 AbstractBusinessProxyController 하위 SvProxyController 등은 businessCode별 downstream \`/{bc}/online\` HTTP 프록시입니다. tcf-jwt(:8110) JwkSetController가 제공하는 JWKS는 GatewayJwtValidator가 fetch·cache하여 Bearer 검증에 쓰며, ztomcat context /gw·/jwt는 bootRun root URL과 다릅니다.

GatewayRouteCatalog·ROUTING_TABLE.md·tcf-cicd Apache config 삼각 불일치가 404·502 burst의 최빈 원인입니다. Session+JWT 이중 인증 path에서 anonymous health check와 authenticated online 거래를 route table에서 분리해야 합니다.

JWT claim businessCode·userId가 StandardHeader에 매핑되는지 STF integration test로 검증하고, auth.jwt.enabled=false profile에서도 Gateway-only smoke를 유지합니다. JWT key rollover·JWKS cache TTL·Proxy read timeout vs OM TimeoutPolicy 불일치는 E-JWT-AUTH-* runbook과 연결됩니다.

운영 rotation 절차: key 발급→Gateway JWKS refresh→ztomcat /gw smoke→claim mapping regression.`,

    '제09편/27-tcf-eai-cache-batch': `tcf-eai TcfServiceClient는 WAR 간 StandardRequest POST 클라이언트로, ad-hoc REST URL 없이 ServiceId 수준 contract를 유지합니다. Header guid propagation·error mapping·Timeout 중첩은 docs/46-service-integration-contract와 zguide/tcf-eai가 SoT입니다.

tcf-cache SPI와 OM cache evict Handler는 Catalog·공통코드 변경 propagation을 맞추며, evict 누락 시 STF 이후 Handler가 stale meta를 읽는 production issue가 빈번합니다. tcf-batch Scheduler→OM Dashboard metrics는 online TCF 장애와 lifecycle이 decouple되어 batch down이 거래를 막지는 않으나 observability blind spot을 만듭니다.

eai·cache·batch 모듈은 bootRun 독립 기동이 가능하나 ztomcat ALL_MODULES integration verify가 릴리즈 gate입니다. 온라인 Handler에 @Scheduled 혼입은 antipattern입니다.

리뷰: EAI target ServiceId Catalog 존재, foreign WAR RestTemplate 직접 호출 금지, batch job idempotent, scheduler cluster singleton(운영 Gap 문서화).`,

    '제09편/28-tcf-cicd-scripts': `tcf-cicd는 profile별 application yml·Apache·Tomcat layout의 SoT이며 sync-to-framework·pull-from-framework로 nsight-tcf-framework와 양방향 sync합니다. tcf-scripts build-all.sh·curl-sv-sample.sh·deploy helper는 로컬·CI shortcut으로 ztomcat smoke·sv-sample E2E regression artifact를 제공합니다.

cicd-build.sh→deploy-wars.sh 13 WAR pipeline은 settings.gradle module list와 manifest.yaml 일치를 전제하며, ztomcat README ALL_MODULES smoke는 릴리즈 전 권장 gate입니다. 스크립트 hardcoded path·Windows vs Linux line ending·secret repo commit은 CI fail·보안 incident 원인입니다.

DevOps 운영: manifest version pin, rollback script dry-run, tcf-scripts re-run idempotency, cicd-deploy smoke fail→znsight-man 67 rollback 분기.

아키텍트: tcf-cicd yml 변경은 전 WAR STF flag·Gateway auth.jwt.enabled blast radius review 동반.`,

    '제09편/29-업무-WAR-ic-pc-ms-sv-pd': `ic·pc·ms·sv·pd 다섯 업무 WAR는 동일 6계층·\`/{bc}/online\`·com.nh.nsight.marketing.{bc} 패키지 패턴을 공유합니다. sv-service는 SV.Customer.selectSummary 표준 실습·zguide/sv-service SoT이고, ic-service는 tcf-eai WAR 간 연동 데모 상대입니다. pc·ms·pd는 SampleHandler 확장 템플릿으로 신규 BC 온보딩 boilerplate입니다.

포트 8082~8087(부록 K)과 businessCode별 Gateway ProxyController는 bc 문자열만 바꿔 복제 가능하나, OM Catalog·거래통제·Timeout·RDW schema는 BC별 독립 등록이 필수입니다. WAR 간 duplicate ServiceId는 기동 fail, contract 없는 shared DB table은 정합성 incident를 유발합니다.

IcCustomerHandler vs SvCustomerHandler diff minimal pattern을 MR template으로 두면 review noise를 줄입니다. deploy-wars.sh·settings.gradle war project name·ROUTING_TABLE downstream 동시 갱신을 Blocker로 둡니다.

점검: BC별 curl /{bc}/online smoke, OM seed per BC, Gateway Proxy health.`,

    '제09편/30-업무-WAR-eb-ep-ss-mg': `eb-service(EbEventHandler)는 이벤트 브릿지·배치성 ingress, ep-service(EpUserEventHandler)는 실시간 이벤트 처리 WAR입니다. ss·mg는 지원 도메인 Sample로 sv 패턴 parity를 유지합니다. Gateway EbProxy ingress는 REST surface처럼 보이나 내부는 Online TCF Handler dispatch로 귀결됩니다.

tcf-eai 동기 StandardRequest POST vs eb/ep 비동기 이벤트는 zarchitecture/14 tradeoff를 코드로 구현한 것이며, 동일 business rule 이중 구현은 drift·bug duplicate 위험이 큽니다. eb Batch-heavy Handler와 ep low-latency Handler는 SLA·TimeoutPolicy·감사 processingType 설계가 다릅니다.

점검: eb→ep E2E event, Gateway route to eb, ep processingType audit log, ss/mg sample Handler 6계층 compliance.

리뷰: online Handler에 이벤트 consumer logic 혼입 금지, 이벤트 payload vs StandardRequest header mapping 문서화.`,

    '제10편/31-공식-설계안-매핑': `docs/설계자료 Word 20+→zman→zarchitecture→Handler·OM 화면 3-way traceability는 감사·인증·레거시 장애 대응의 기본 도구입니다. NSIGHT-FINAL-ARCHITECTURE-DECISION ADR은 Handler 중심·Online Endpoint·tcf-om SoT 등 최종 아키텍처 결정 근거를 코드 review dispute 시 인용합니다.

OmDashboardHandler 등 OM 화면 ServiceId와 Word 절 번호 매핑 표는 Gap 식별 출발점입니다. 설계-only CC/BC/CM WAR는 코드 grep 0건이 정상이며 zman/00-설계서-코드베이스-대조표에 명시됩니다. Word revision→zman diff→ztcfbook 갱신 절차가 어긋나면 "설계서상 필수·코드 optional" friction이 생깁니다.

아키텍트 deliverable: framework change 시 ADR append, OM UI screenshot↔Handler↔Word 절 삼각 링크, 설계 MR에 Word anchor citation.

리뷰: ADR conflict check, OM Catalog row vs design transaction list, Gap은 제32장 cross-ref.`,

    '제10편/32-Gap-보완-향후-과제': `zman/23 소스 Gap 분석과 zman/24 보완과제 우선순위는 nsight-tcf-framework의 공식 로드맵 SoT입니다. 설계서 17업무 WAR 대비 코드 9 WAR 배포, CC·BC·CM 미구현, Observability metrics 통합 export 미완, DR·Rollback 자동화 부분 구현 등이 rank되어 있으며, 코드베이스 TODO comment만으로 우선순위를 정하면 설계·운영과 어긋납니다.

Observability Gap은 tcf-batch→OM Dashboard feed는 존재하나 guid/traceId 기반 cross-WAR tracing export·alert 연동이 미완입니다. DR Gap은 cicd-deploy 실패 시 수동 WAR version pin rollback, OMDB backup·restore runbook partial 상태입니다. 제31장 설계안 매핑과 함께 Gap closure evidence(smoke·metric·runbook) 없이 "완료" 선언하지 않습니다.

향후 17업무 WAR 확장 시 Gradle settings.gradle, Gateway ProxyController·ROUTING_TABLE, ztomcat deploy-wars.sh, OM seed blast radius를 사전 estimate해야 STF~ETF·Catalog·통제 일괄 누락을 막을 수 있습니다. docs/architecture/44~45 observability·DR ADR과 zman/24 priority를 분기 review에서 delta 갱신하십시오.

아키텍트 분기 점검: Gap list 변경분, smoke coverage vs closed Gap, 미배포 BC Header 사용 incident postmortem 반영.`,

    '부록/A-업무코드-표준표': `부록 A 업무코드 표준표는 businessCode가 StandardHeader, URL /{bc}/online, Gradle module명, Tomcat Context Path, Java 패키지 com.nh.nsight.marketing.{bc}, tcf-gateway Proxy route의 공통 primary key임을 정의합니다. 이 한 글자·두 글자 코드 하나가 STF Header 7항·Dispatcher WAR 선택·OM Catalog partition·Gateway ROUTING_TABLE row를 동시에 결정합니다.

설계서 17업무코드와 nsight-tcf-framework 배포 9 WAR(ic·pc·ms·sv·pd·eb·ep·ss·mg) 차이는 의도적 구현 범위 Gap입니다. 미배포 BC(CC·BC·CM 등)를 sample JSON·신규 Header에 넣으면 STF TransactionControlService 거절 또는 Gateway 404가 발생하므로, 부록 A·BusinessModuleDefinitions·deploy-wars.sh WAR list를 신규 거래 설계 전 교차 확인합니다.

신규 업무코드 추가는 ADR·부록 A row·settings.gradle·Gateway·ztomcat·OM seed를 한 MR bundle로 처리하는 것이 표준입니다. Word 설계안 BC와 표 A 불일치는 제31장 매핑으로 trace하되, 코드에 없는 BC는 "미구현"으로 명시합니다.

리뷰·운영: OnlineTransactionController @RequestMapping bc vs Header businessCode, ztomcat context path typo, functionAuth seed bc scope.`,

    '부록/B-ServiceId-명명규칙': `부록 B ServiceId 명명규칙 \`{BC}.{Domain}.{action}\`은 TransactionDispatcher registry key이며, Handler serviceIds() 배열·handle() switch case·OM OM_SERVICE_CATALOG.service_id 컬럼 문자열이 완전 일치해야 합니다. action 표준(selectSummary, save, update, delete 등) deviation은 OM 검색·운영 교육·로그 집계에서 지속적 friction을 만듭니다.

중복 serviceId 등록은 Spring ApplicationContext 기동 시 IllegalStateException으로 fail-fast합니다. Catalog 미등록 ServiceId는 Dispatcher까지 도달해도 SERVICE_NOT_FOUND BusinessException으로 ETF businessFail 처리됩니다. OM.Auth.login 같은 OM.* prefix와 SV.Customer.selectSummary 같은 업무 prefix는 WAR·패키지 marketing.om vs marketing.sv로 분리됩니다.

Gateway downstream POST debug log와 TCF_TX_LOG service_id column은 장애 triage 시 ServiceId 문자열을 역추적하는 주요 경로입니다. Gateway·UI Relay JSON header.serviceId 필드명도 Java StandardHeader와 동기해야 STF 1단 Header 검증을 통과합니다.

MR Blocker: ServiceId triple string match diff, 신규 action 표준表 등록, E2E curl with exact serviceId.`,

    '부록/C-거래코드-명명규칙': `부록 C 거래코드 \`{BC}-{TYPE}-{NNNN}\`은 StandardHeader transactionCode, 감사로그, 거래통제 OM TCF_TX_CONTROL, Catalog processingType(INQ/REG/UPD/DEL)과 연결됩니다. 하나의 ServiceId에 여러 거래코드(1:N)를 둘 수 있어 화면·채널별 감사 granularity를 조정합니다.

STF Header 검증에서 transactionCode 형식 불일치는 E-TCF-HDR-002 등으로 조기 fail하며 Handler DB access 전에 차단됩니다. Catalog lifecycle state machine(설계·승인·운영)과 거래코드 TYPE enum은 OM 등록 절차(znsight-man 17·48)와 동기됩니다.

설계 단계에서 신규 TYPE(예: APR 승인) 추가 시 부록 C row·OM enum·거래통제 policy·감사로그 필드 mapping을 세트로 작성합니다. ServiceId만 등록하고 transactionCode를 임의 문자열로 두면 운영 감사·통계가 깨집니다.

코드 리뷰·운영: Header transactionCode vs TCF_TX_LOG·audit log field, negative test wrong TYPE, Catalog NNNN sequence collision check.`,

    '부록/D-표준-전문-JSON-예시': `부록 D 표준 전문 JSON은 curl·tcf-ui Relay·통합 테스트에서 재사용하는 요청·응답 형태의 기준점입니다. 요청은 header와 body, 응답은 header·result·body(오류 시 errorCode·errorMessage) 구조를 SV·OM 시나리오별로 보여 주며, STF Header 검증과 ETF result 조립 규칙과 byte-level로 맞춰져 있습니다.

성공 시 result.success=true와 body payload, 실패 시 BusinessException 경로의 errorCode는 Handler가 아니라 ETF가 채웁니다. clientHeader echo 대상 필드는 샘플 JSON 주석과 znsight-man 20장을 함께 참고해야 Gateway·Relay 경유 E2E에서 Header 불일치를 줄일 수 있습니다.

운영 장애 재현 시 prod channelId·userId를 그대로 복사하지 않고 구조만 차용하는 것이 보안상 안전합니다. 릴리즈마다 StandardRequest·StandardResponse Java 필드와 샘플 JSON 키 이름 diff를 자동화하면 STF E-TCF-HDR-* 회귀를 조기에 잡을 수 있습니다.

코드 리뷰에서는 신규 거래 MR에 부록 D 형식의 최소 sample JSON 또는 tcf-ui sample-requests 추가를 evidence로 요구하고, negative sample(잘못된 transactionCode)도 STF 거절을 검증하는 데 포함하십시오.`,

    '부록/E-Mapper-XML-템플릿': `부록 E Mapper XML 템플릿은 namespace=DAO FQCN, ResultMap, dynamic where, count·select 분리 페이징 패턴을 SvCustomerMapper.xml로 고정합니다. MyBatis 바인딩은 \${} 문자열 치환이 아니라 #{} 파라미터 바인딩만 허용하며, 이는 SQL injection 방어와 코드 리뷰의 Blocker 항목입니다.

RDW·ADW datasource는 application.yml과 tcf-cicd profile별로 분리되고, SQL ID camelCase는 Dao interface method와 1:1입니다. SELECT * 지양·where 절 인덱스·Facade readOnly 조회 거래 연계는 znsight-man 28~29장과 동일합니다.

운영 이슈는 N+1 query, 페이징 count·select 불일치 total, dynamic order by 화이트리스트 누락에서 자주 발생합니다. STF~ETF는 SQL slow를 Timeout으로만 처리하므로 OM TimeoutPolicy와 DB 튜닝을 함께 봐야 합니다.

리뷰 시 Mapper XML namespace diff, EXPLAIN(가능 시), DAO only from Facade/Service, Rule에서 Mapper 호출 금지를 확인하십시오.`,

    '부록/F-오류코드-표준표': `부록 F 오류코드 표는 E-COM-* 공통, E-TCF-HDR/CTL/TIME-* 프레임워크, E-JWT-AUTH-* Gateway, E-{BC}-* 업무 확장 규칙을 한곳에 모읍니다. BusinessException errorCode는 ETF businessFail에서 StandardResponse.result.errorCode로 매핑되며, OmErrorCodeHandler로 OM CRUD와 동기화됩니다.

systemError(E9999 계열)와 businessFail(E0001·업무 E-* )는 TxLog.end 코드·운영 runbook·사용자 메시지 노출 정책이 다릅니다. Handler catch에서 임의 문자열 error를 Response에 넣으면 표준 표와 어긋나 observability correlation이 깨집니다.

Gateway JWT 검증 실패, STF Header 7항 거절, Timeout 초과 각각 다른 prefix를 쓰므로 장애 triage 시 errorCode prefix만으로 계층(GW vs STF vs Handler)을 좁힐 수 있습니다.

신규 E-SV-* 등록 시 부록 F row·OM seed·znsight-man 33장 사용자/운영 메시지를 세트로 MR에 포함하십시오.`,

    '부록/G-application-yml-템플릿': `부록 G application.yml 템플릿은 HikariCP·MyBatis·TCF feature flag·spring.profiles·application-tcf.yml import를 모듈별로 표준화합니다. tcf-cicd local/dev/prod가 profile별 SoT이므로 sv-service/src/main/resources와 tcf-cicd/local/spring/*/application-local.yml drift를 릴리즈 전 sync-to-framework로 맞춥니다.

로컬 tcf.session-validation-enabled=false는 STF 4단 SessionValidator skip tradeoff를 문서화합니다. datasource URL은 업무 RDW와 OM H2(nsight_om)·TxLog를 혼동하지 않도록 module별로 분리합니다.

repo에 DB password·JWT secret hardcoding은 금지이며, management endpoint exposure는 prod profile에서 제한합니다. Gateway auth.jwt.enabled와 tcf-web security flag 불일치는 ztomcat 통합 smoke fail로 나타납니다.

리뷰·운영: active profile CI 명시, cicd yml 변경 시 ALL_MODULES smoke, connection pool leak, H2→운영 DB 전환 checklist(부록 J).`,

    '부록/H-개발-완료-체크리스트': `부록 H 개발 완료 체크리스트는 ServiceId·거래코드·OM Catalog 3점 일치, 6계층+Mapper SQL, curl E2E 통과, functionAuth OM seed, negative STF case를 출시 전 gate로 묶습니다. Handler merge만으로 "완료"가 될 수 없고, OM 등록 evidence가 없으면 운영 반려가 정상입니다.

curl-sv-sample.sh 또는 동등 JSON E2E 로그, OM 화면 캡처·export, MR description checklist tick을 PR evidence로 남깁니다. znsight-man 62 품질 게이트와 연동하면 DevOps 파이프라인 조건으로 승격할 수 있습니다.

H2 integration에서 Catalog·거래통제·Timeout prod-like seed로 STF 7~8단을 mock 없이 통과시키는 것이 아키텍트 sign-off 권장 기준입니다. Idempotency·Timeout·Authorization negative test 포함 여부도 확인하십시오.

운영 인수 전 부록 H와 I·J 중복 항목을 한 번에 점검하면 Catalog 누락형 incident를 줄입니다.`,

    '부록/I-코드-리뷰-체크리스트': `부록 I 코드 리뷰 체크리스트 15항은 Handler=switch only, Facade=@Transactional, SQL injection 방어, 로그 PII 마스킹, OM 등록 Blocker, MR test evidence를 강제합니다. 업무 WAR Controller 추가·Handler @Transactional·Service에서 HttpSession 접근은 계층 위반으로 반려합니다.

OM Catalog·거래통제·Timeout 미등록은 기능 동작 여부와 무관하게 Blocker입니다. tcf-core·tcf-web 변경 MR은 sv-service bootRun+ztomcat smoke regression 범위가 업무 WAR MR보다 넓습니다.

보안 코딩(znsight-man 42): dynamic SQL order by whitelist, log masking, JWT secret not in repo. 테스트 evidence 없는 승인은 장애 시 rollback 근거가 없어집니다.

플랫폼·업무 MR 모두 부록 I tick list를 PR template에 embed하는 것을 권장합니다.`,

    '부록/J-운영-전환-체크리스트': `부록 J 운영 전환 체크리스트 10영역은 WAR build version pin, OM prod seed(Catalog·통제·Timeout·ErrorCode), Gateway·Apache route prod yml, Tomcat deploy, Gateway 경유 smoke, rollback WAR pin, 운영자 인수인계를 포함합니다. Smoke fail 시 znsight-man 67 롤백 절차로 즉시 분기합니다.

cicd-deploy.sh·deploy-wars.sh·data.sql prod seed diff는 변경 관리 ticket과 연결합니다. JWT key rotation·SESSIONDB migration·OMDB backup restore는 DR drill checklist로 분기별 rehearsal합니다.

Gateway downstream URL·Context Path·부록 K 포트 표 triad mismatch는 전환 직후 404·502 burst를 유발합니다. tcf-batch·Dashboard feed가 online smoke와 별도임을 runbook에 명시하십시오.

운영 전환 승인 서명 전 부록 H 개발 완료와 부록 J를 연속 tick하고, rollback pin WAR file name을 release note에 기록하십시오.`,

    '부록/K-모듈-포트-Context-WAR-매핑표': `부록 K는 settings.gradle 22모듈·bootRun port·Context Path·WAR 파일명·ztomcat /gw·/jwt·Gateway downstream /{bc}/online 매핑의 단일 표입니다. BusinessModuleDefinitions·ROUTING_TABLE.md·ztomcat README·부록 K 네 곳 불일치가 integration smoke fail의 최빈 원인입니다.

참조 포트: tcf-ui 8099, tcf-uj 8102, tcf-gateway 8100, tcf-jwt 8110, tcf-om 8097, sv-service 8086 등. bootRun root context vs ztomcat nested context 차이를 curl URL 작성 시 반드시 구분합니다.

포트 충돌은 다른 WAR가 뜬 것처럼 보이 yet wrong Handler 응답을 주거나, Proxy 502만 발생합니다. 신규 module 추가 시 부록 K row·Gradle·Gateway ProxyController·deploy-wars.sh를 동시 MR로 묶으십시오.

장애 triage: (1) 부록 K port listen (2) Context Path (3) Gateway ROUTING_TABLE downstream host:port.`,

    '부록/L-TCF-핵심-테이블-DDL-요약': `부록 L은 tcf-om schema.sql 23테이블·TCF_TX_LOG·Spring Session table DDL 요약입니다. OM_SERVICE_CATALOG·TCF_TX_CONTROL·TCF_TIMEOUT_POLICY는 STF 7~8단 runtime lookup core이며, TCF_TX_LOG는 모든 업무 WAR가 INSERT하는 cross-cutting audit trail입니다.

data.sql seed와 schema migration 순서는 tcf-om 기동·OM Handler integration test 전제입니다. docs/architecture/19-tcf-table.md와 DDL diff가 어긋나면 문서 기반 장애 분석이 실패합니다.

운영: TxLog INSERT slow·disk full, OMDB migration rollback script, SESSIONDB separate from RDW. OM upgrade without cache evict → stale Catalog incident.

리뷰: schema.sql PR은 docs/19·부록 L·OM Handler 영향 분석 동반, index on guid·service_id.`,

    '부록/M-명명규칙-21주제-색인': `부록 M은 znsight-man 명명규칙 01~21주제를 제02편·부록 A~C·docs/architecture/53-naming-conventions.md에 역매핑하는 색인입니다. ServiceId(07)·거래코드(08)·Header(21)·Gateway·Batch·Cache(18~20) 등 신규 식별자 논의 시 명명규칙-00 목차에서 출발합니다.

Word docx→md 재생성 절차와 ztcfbook 갱신 주기가 어긋나면 "문서상 허용·코드 review deny" friction이 생깁니다. 21주제 예외 ADR은 아키텍트 승인 없이 MR에 반영하지 않습니다.

플랫폼(Gateway route id, Batch Job ID, Cache region name)과 업무(ServiceId, 거래코드) 명명 축이 다르므로 혼용 금지. 부록 M→해당 명명규칙 md→Handler/OM 실제 문자열 triple check.

운영·리뷰: naming convention change는 breaking change로 분류하고 Catalog·ROUTING_TABLE 영향 목록 첨부.`,

    '부록/N-소스-인덱스': `부록 N은 docs/SOURCE_INDEX.md 기반 패키지→핵심 클래스 인덱스로, TCF.process·STF·ETF·OnlineTransactionController·TransactionDispatcher·{bc}/entry/handler/*Handler 경로를 장애 triage starting point로 제공합니다. 온라인 10단(Controller→TCF→STF→Dispatcher→Handler→ETF) 네비게이션은 제01편 3장과 동일합니다.

ztcfbook-h 구현 샘플 file path가 SOURCE_INDEX·실 repo layout과 drift하면 Master Edition 신뢰도가 떨어집니다. 신규 module·Handler 추가 시 SOURCE_INDEX.md 갱신을 MR checklist에 포함하십시오.

Cross-module jump: tcf-gateway ProxyController→업무 Handler, tcf-om Om*Handler→schema.sql table, tcf-eai TcfServiceClient→target ServiceId Catalog.

아키텍트: INDEX를 IDE 대체가 아닌 onboarding·incident map으로 유지하고, 분기별 stale link scan을 권장합니다.`,
  };
}

module.exports = { buildNarratives };
