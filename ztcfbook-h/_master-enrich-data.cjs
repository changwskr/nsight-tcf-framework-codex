'use strict';

/** @param {string} title @param {string[]} points @param {string[]} [checklist] */
function deepDive(title, points, checklist) {
  const cl = checklist ?? [
    '상단 **구현 샘플**을 실제 코드와 대조한다.',
    '**심화 참고**와 ztcfbook 본문 절 번호를 매핑한다.',
    '운영·배포 관점은 ztcfbook-h Master 블록을 우선 본다.',
  ];
  return `## Master Deep Dive — ${title}

${points.map((p) => `- ${p}`).join('\n')}

### 아키텍트 체크리스트

${cl.map((c) => `- ${c}`).join('\n')}`;
}

/** @type {Record<string, import('./_gen-book-h.cjs').EnrichEntry>} */
const MASTER_ENRICH = {
  '서문/00-서문': {
    mermaid: `flowchart TB
  Word[docs/설계자료 Word]
  Arch[docs/architecture 01-53]
  Zman[zman 설계서]
  Zman --> Book[ztcfbook]
  Book --> M[ztcfbook-m 입문]
  Book --> H[ztcfbook-h Master]
  Zguide[zguide 22모듈] --> Book`,
    refs: ['docs/architecture/architecture.md', 'zman/README.md', 'zarchitecture/README.md', 'znsight-man/01-문서개요.md'],
    deepDive: deepDive('서문 · 문서 체계', [
      'Word → zman → zarchitecture → ztcfbook 3단계 문서 계층',
      'ztcfbook-h = ztcfbook 전문 + mermaid + 코드 샘플 + Deep Dive',
      '설계 17업무 vs 코드 9 WAR + tcf-om Gap은 제10편·부록 A에서 확인',
      '역할별 경로: 개발자(ztcfbook-m) · 운영(ztcfbook) · 아키텍트(ztcfbook-h)',
    ]),
  },

  '제01편/01-NSIGHT-TCF란-무엇인가': {
    mermaid: `flowchart LR
  Client[채널/UI] --> GW[tcf-gateway]
  GW --> WAR[업무 WAR]
  WAR --> TCF[TCF.process]
  TCF --> Handler[TransactionHandler]
  Handler --> DB[(RDW/H2)]`,
    samples: [
      { title: 'OnlineTransactionController', file: 'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java', start: 15, end: 58 },
      { title: 'NsightWarBootstrap', file: 'tcf-web/src/main/java/com/nh/nsight/tcf/web/support/NsightWarBootstrap.java', start: 1, end: 30 },
    ],
    refs: ['docs/architecture/architecture.md', 'znsight-man/03-TCF-개발원칙.md', 'zman/05-TCF처리구조.md', 'znsight-man/22-Online-Endpoint-기준.md'],
    deepDive: deepDive('NSIGHT TCF 개요', [
      'Online Endpoint(`/{bc}/online`)만 사용 — REST Resource Controller 아님',
      'ServiceId가 Dispatcher 라우팅 키, 거래코드는 감사·통제용',
      'bootRun(개발) vs ztomcat WAR(통합) 이중 배포 모델',
      '신규 거래 = OM Catalog + Handler serviceIds() + Facade @Transactional',
    ]),
  },

  '제01편/02-전체-시스템-구조': {
    mermaid: `flowchart TB
  GSLB[GSLB/L4] --> Apache[Apache]
  Apache --> GW[tcf-gateway :8100]
  GW --> WAR1[sv-service :8086]
  GW --> WAR2[tcf-om :8097]
  UI[tcf-ui :8099] -->|Relay| WAR1
  UJ[tcf-uj :8102] --> GW`,
    samples: [
      { title: 'BusinessModuleDefinitions (tcf-ui)', file: 'tcf-ui/src/main/java/com/nh/nsight/tcf/ui/support/BusinessModuleDefinitions.java', start: 1, end: 35 },
      { title: 'deploy-wars.sh', file: 'ztomcat/deploy-wars.sh', start: 1, end: 35 },
    ],
    refs: ['zarchitecture/01-전체-시스템-아키텍처.md', 'zarchitecture/16-모듈-포트-의존성-레퍼런스.md', 'docs/architecture/48-multi-module-dependencies.md'],
    deepDive: deepDive('전체 시스템 구조', [
      '9 업무 WAR + 플랫폼(tcf-om/ui/uj/jwt/gateway/batch/eai/cache)',
      '포트·Context Path·WAR명 1:1 — 부록 K 참조',
      'Gradle 멀티모듈, 업무 WAR는 tcf-web transitively 의존',
      'tcf-ui=개발 Relay, tcf-uj=Gateway 경유 UI',
    ]),
  },

  '제01편/03-TCF-처리-엔진': {
    mermaid: `sequenceDiagram
  autonumber
  participant C as OnlineTransactionController
  participant T as TCF
  participant S as STF
  participant TO as TimeoutExecutor
  participant D as TransactionDispatcher
  participant H as TransactionHandler
  participant E as ETF
  C->>T: process(StandardRequest)
  Note over C: path /online 또는 /{businessCode}/online
  T->>S: preProcess(request, clientHeader)
  Note over S: Header7 · GUID/TraceId · Session · Auth · Control · Timeout · Idempotency · TxLog.start · MDC
  S-->>T: TransactionContext
  T->>TO: execute(() -> dispatch)
  TO->>D: dispatch(request, context)
  D->>H: handle(request, context)
  H-->>D: body Object
  D-->>TO: body
  TO-->>T: body
  alt 정상
    T->>E: success(request, body, context, clientHeader)
    Note over E: idempotency OK · txLog.end S0000 · audit · metric
  else BusinessException
    T->>E: businessFail(...)
    Note over E: txLog.end E0001 · audit · metric
  else Timeout
    T->>E: businessFail(timeout)
  else 기타 Exception
    T->>E: systemError(...)
  end
  E-->>C: StandardResponse
  Note over T: finally — ContextHolder · AuthHolder · TimeoutHolder · MDC clear`,
    mermaidExtra: `flowchart TB
  subgraph STF["STF preProcess (순서 고정)"]
    H1[StandardHeaderValidator]
    H2[GUID / TraceId 부여]
    H3[TransactionContext + MDC]
    H4[SessionValidator]
    H5[AuthenticationContextValidator]
    H6[AuthorizationValidator]
    H7[TransactionControlService.check]
    H8[TimeoutPolicyService.resolveAndApply]
    H9[IdempotencyChecker.checkAndMarkProcessing]
    H10[TransactionLogService.start]
    H1 --> H2 --> H3 --> H4 --> H5 --> H6 --> H7 --> H8 --> H9 --> H10
  end
  STF --> DISPATCH[TransactionDispatcher.dispatch]
  DISPATCH --> BTF[BTF Handler → Facade @Transactional]
  BTF --> ETF["ETF success | businessFail | systemError"]
  ETF --> CLEAN["finally: Holder·MDC clear"]`,
    mermaidExtraTitle: 'STF 내부 · ETF 합류',
    samples: [
      { title: 'OnlineTransactionController', file: 'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java', start: 15, end: 58 },
      { title: 'TCF.process()', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java', start: 35, end: 87 },
      { title: 'STF.preProcess()', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java', start: 51, end: 103 },
      { title: 'TransactionDispatcher', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/dispatch/TransactionDispatcher.java', start: 22, end: 64 },
      { title: 'ETF 3경로', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java', start: 37, end: 93 },
    ],
    deepDive: `## Master Deep Dive — TCF 처리 엔진

### clientHeader echo · STF 10단 · ETF 3경로

- \`clientHeader\` 스냅샷으로 응답 Header echo 규칙 유지
- STF 실패 시 Handler·DB 트랜잭션 **미실행**
- \`TimeoutExecutor\`로 Handler 구간 타임아웃 적용
- ETF: success / businessFail / systemError — audit·metric 공통
- \`finally\`: ContextHolder·AuthHolder·TimeoutHolder·MDC clear 필수

### 아키텍트 체크리스트

- OM Catalog + Handler \`serviceIds()\` + Facade \`@Transactional\` 일치
- 중복 serviceId → 기동 실패(IllegalStateException)
- 장애 추적: guid/traceId + TCF_TX_LOG`,
    refs: ['docs/architecture/33-TCF.md', 'docs/architecture/34-STF.md', 'docs/architecture/35-BTF.md', 'docs/architecture/36-ETF.md', 'zarchitecture/02-TCF-프레임워크-아키텍처.md'],
  },

  '제01편/04-애플리케이션-6계층': {
    mermaid: `flowchart TB
  H[Handler serviceIds switch] --> F[Facade @Transactional]
  F --> S[Service]
  S --> R[Rule]
  R --> D[DAO]
  D --> M[Mapper XML]`,
    samples: [
      { title: 'SvCustomerHandler', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java', start: 1, end: 45 },
      { title: 'SvCustomerFacade', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/facade/SvCustomerFacade.java', start: 1, end: 40 },
    ],
    refs: ['docs/architecture/01-application-layer.md', 'zarchitecture/03-애플리케이션-6계층-아키텍처.md', 'znsight-man/12-애플리케이션-계층구조.md'],
    deepDive: deepDive('애플리케이션 6계층', [
      'HTTP 진입 = OnlineTransactionController 단일 — 업무 Controller 없음',
      'Facade만 @Transactional, Handler/Service/Rule/DAO는 트랜잭션 경계 밖',
      'Rule = 순수 로직(부수효과·DB 접근 금지)',
      'Mapper namespace = DAO FQCN',
    ]),
  },

  '제02편/05-개발-표준-총정리': {
    mermaid: `flowchart LR
  BC[업무코드] --> CTX[Context/WAR]
  CTX --> PKG[Package]
  PKG --> SID[ServiceId]
  SID --> H[Handler]
  H --> OM[OM Catalog]`,
    samples: [
      { title: 'BusinessModuleDefinitions', file: 'tcf-ui/src/main/java/com/nh/nsight/tcf/ui/support/BusinessModuleDefinitions.java', start: 1, end: 35 },
      { title: 'settings.gradle 모듈', file: 'settings.gradle', start: 1, end: 40 },
    ],
    refs: ['znsight-man/04-개발표준-전체요약.md', 'znsight-man/14-명명-규칙.md', 'znsight-man/09-업무-WAR-구조.md', 'znsight-man/07-Git-브랜치-기준.md'],
    deepDive: deepDive('개발 표준 총정리', [
      '12축 표준: 업무코드→패키지→ServiceId→Handler→OM→배포',
      '패키지 `com.nh.nsight.marketing.{bc}` 고정',
      'Git MR·Commit 규칙 + Catalog 등록 순서',
      '표준 17업무코드 vs 배포 9 WAR 차이 인지',
    ]),
  },

  '제02편/06-식별자-명명규칙': {
    mermaid: `flowchart LR
  SID["ServiceId BC.Domain.action"] --> DISPATCH[Dispatcher]
  TXC["거래코드 BC-TYPE-NNNN"] --> CTRL[거래통제/감사]`,
    samples: [
      { title: 'SvCustomerHandler serviceIds', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java', start: 1, end: 35 },
      { title: 'OmServiceCatalogHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmServiceCatalogHandler.java', start: 1, end: 35 },
    ],
    refs: ['znsight-man/명명규칙-07-ServiceId.md', 'znsight-man/부록B-ServiceId-명명규칙.md', 'znsight-man/명명규칙-08-거래코드.md', 'zman/07-ServiceIdDispatcher.md'],
    deepDive: deepDive('식별자 명명규칙', [
      'ServiceId = Dispatcher 키, 거래코드 = 감사·통제·Catalog',
      'action 표준: selectSummary, save, update, delete 등',
      'OM prefix `OM.*` vs 업무 `SV.*`/`IC.*`',
      'Gateway route `/{bc}/online`과 businessCode 정합',
    ]),
  },

  '제02편/07-코드-DB-명명규칙': {
    mermaid: `flowchart TB
  CLS[Java *Handler/*Facade/*Service] --> XML[Mapper namespace]
  XML --> SQL[SQL ID camelCase]
  SQL --> TBL[TCF_* / OM_* 테이블]`,
    samples: [
      { title: 'SvCustomerMapper.xml', file: 'sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml', start: 1, end: 50 },
      { title: 'ErrorCode', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/ErrorCode.java', start: 1, end: 40 },
    ],
    refs: ['znsight-man/명명규칙-09-Java-Class.md', 'znsight-man/명명규칙-12-MyBatis-Mapper-SQL.md', 'docs/architecture/19-tcf-table.md', 'znsight-man/명명규칙-14-오류코드.md'],
    deepDive: deepDive('코드·DB 명명규칙', [
      'Handler/Facade/Service/Rule/DAO suffix 규칙 준수',
      'Mapper namespace = DAO fully qualified name',
      'ErrorCode `E-{BC|COM|TCF}-*` — BusinessException과 ETF 연동',
      '로그·MDC 필드명 = StandardHeader 필드와 동기',
    ]),
  },

  '제03편/08-거래-설계': {
    mermaid: `flowchart LR
  DESIGN[ServiceId 설계] --> CAT[OM Catalog]
  CAT --> CTRL[거래통제]
  CTRL --> TO[Timeout 정책]`,
    samples: [
      { title: 'TransactionControlService', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/control/TransactionControlService.java', start: 1, end: 55 },
      { title: 'OmTransactionControlHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmTransactionControlHandler.java', start: 1, end: 40 },
    ],
    refs: ['znsight-man/16-ServiceId-설계.md', 'znsight-man/47-ServiceId-등록-절차.md', 'docs/architecture/40-header-7-transaction-control.md'],
    deepDive: deepDive('거래 설계', [
      'Header 7 Allow-List + processingType + channelId',
      'OM Catalog 등록 전 Handler만 있으면 운영 실패',
      'TimeoutPolicyService가 STF 8단에서 적용',
      '화면-ServiceId 1:N 매핑 문서화',
    ]),
  },

  '제03편/09-표준-전문과-DTO': {
    mermaid: `flowchart LR
  REQ[StandardRequest Header+Body] --> STF[검증]
  STF --> BTF[Handler]
  BTF --> RES[StandardResponse Header+Result+Body]`,
    samples: [
      { title: 'SV 조회 요청 JSON', file: 'tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json' },
      { title: 'StandardRequest', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardRequest.java', start: 1, end: 35 },
    ],
    refs: ['znsight-man/20-표준-전문-구조.md', 'znsight-man/부록D-표준-전문-예시.md', 'docs/architecture/36-ETF.md', 'znsight-man/38-Idempotency-중복요청.md'],
    deepDive: deepDive('표준 전문과 DTO', [
      '요청 header+body, 응답 header+result+body(+error)',
      'Header echo — clientHeader 스냅샷 규칙',
      'Idempotency-Key 헤더와 STF 9단 연동',
      'Body Map vs typed DTO — Facade에서 변환',
    ]),
  },

  '제03편/10-TransactionHandler-개발': {
    mermaid: `flowchart TB
  COMP[@Component Handler] --> IDS[serviceIds]
  IDS --> SW[switch serviceId]
  SW --> FAC[Facade]`,
    samples: [
      { title: 'SvCustomerHandler', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java' },
      { title: 'IcCustomerHandler', file: 'ic-service/src/main/java/com/nh/nsight/marketing/ic/entry/handler/IcCustomerHandler.java', start: 1, end: 40 },
    ],
    refs: ['znsight-man/23-TransactionHandler-개발.md', 'zman/08-업무Handler개발.md', 'zguide/tcf-eai-개발가이드.md'],
    deepDive: deepDive('TransactionHandler 개발', [
      '도메인당 1 Handler, serviceIds()로 담당 거래 선언',
      'switch default → SERVICE_NOT_FOUND',
      'Handler에 @Transactional 금지 — Facade에서만',
      'WAR간 호출은 tcf-eai TcfServiceClient',
    ]),
  },

  '제03편/11-품질-속성-구현': {
    mermaid: `flowchart TB
  BE[BusinessException] --> ETF[ETF.businessFail]
  TO[Timeout] --> ETF
  IDEM[Idempotency] --> STF
  LOG[TxLog/Audit/Metric] --> ETF`,
    samples: [
      { title: 'BusinessException', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/BusinessException.java' },
      { title: 'OnlineTransactionTimeoutExecutor', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/OnlineTransactionTimeoutExecutor.java', start: 1, end: 50 },
    ],
    refs: ['docs/architecture/05-exception.md', 'docs/architecture/37-transaction-log.md', 'docs/architecture/08-timeout.md', 'docs/architecture/12-cache.md'],
    deepDive: deepDive('품질 속성 구현', [
      '예외 3경로: Business / Timeout / System → ETF',
      '거래로그 STF start + ETF end, 감사는 민감 거래',
      'Cache SPI — tcf-cache + OM evict',
      '파일 up/download OM Handler 연동',
    ]),
  },

  '제04편/12-세션-로그인-권한': {
    mermaid: `sequenceDiagram
  participant OM as OM.Auth.login
  participant SS as Spring Session
  participant STF as STF SessionValidator
  participant AUTH as AuthorizationValidator
  OM->>SS: 세션 생성
  SS->>STF: STF 4단 검증
  STF->>AUTH: STF 6단 권한`,
    samples: [
      { title: 'SessionValidator', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/security/SessionValidator.java', start: 1, end: 40 },
      { title: 'OmAuthHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmAuthHandler.java', start: 1, end: 50 },
    ],
    refs: ['docs/architecture/10-session.md', 'docs/architecture/11-login.md', 'znsight-man/39-세션-사용-기준.md', 'znsight-man/40-권한-검증-기준.md'],
    deepDive: deepDive('세션·로그인·권한', [
      'STF 4·5·6단: Session → AuthContext → Authorization',
      '로컬 `tcf.session-validation-enabled=false` 가능',
      'OM `OM.Auth.login/ssoLogin/logout/session`',
      'functionAuth·dataAuth OM Handler 분리',
    ]),
  },

  '제04편/13-JWT-SSO-Gateway': {
    mermaid: `flowchart LR
  JWT[tcf-jwt :8110 JWKS] --> GW[tcf-gateway :8100]
  GW -->|Bearer 검증| WAR[업무 WAR]
  UJ[tcf-uj] --> GW`,
    samples: [
      { title: 'JwkSetController', file: 'tcf-jwt/src/main/java/com/nh/nsight/auth/jwt/entry/web/JwkSetController.java', start: 1, end: 30 },
      { title: 'GatewayJwtValidator', file: 'tcf-gateway/src/main/java/com/nh/nsight/gateway/application/service/GatewayJwtValidator.java', start: 1, end: 55 },
    ],
    refs: ['docs/architecture/42-jwt.md', 'docs/architecture/51-api-gateway.md', 'znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md'],
    deepDive: deepDive('JWT · SSO · Gateway', [
      '`/.well-known/jwks.json` — Gateway가 JWKS fetch',
      'Gateway auth.jwt.enabled 프로파일별',
      'JWT claim ↔ StandardHeader 정합',
      'Session + JWT 이중 auth path',
    ]),
  },

  '제04편/14-거래통제-정책': {
    mermaid: `flowchart LR
  STF[STF TransactionControlService] --> OM[OM TCF_TX_CONTROL]
  OM --> VAL[Header 7 검증]`,
    samples: [
      { title: 'TransactionControlService', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/control/TransactionControlService.java', start: 1, end: 55 },
      { title: 'OmTransactionControlHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmTransactionControlHandler.java', start: 1, end: 45 },
    ],
    refs: ['docs/architecture/40-header-7-transaction-control.md', 'znsight-man/48-거래통제-등록-절차.md'],
    deepDive: deepDive('거래통제 정책', [
      'Header 7 Allow-List — businessCode URL 일치',
      '채널·점포·시간대 OM 정책',
      'E-TCF-HDR-* / E-TCF-CTL-* ErrorCode',
      'Catalog·통제·Timeout 등록 3종 세트',
    ]),
  },

  '제05편/15-OM-아키텍처와-개발': {
    mermaid: `flowchart TB
  OM[tcf-om 24 Handlers] --> DB[(nsight_om H2)]
  RUN[업무 WAR STF] -->|runtime lookup| DB`,
    samples: [
      { title: 'OmServiceCatalogHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmServiceCatalogHandler.java', start: 1, end: 50 },
      { title: 'schema.sql', file: 'tcf-om/src/main/resources/schema.sql', start: 1, end: 50 },
    ],
    refs: ['zarchitecture/05-운영관리-OM-아키텍처.md', 'docs/architecture/52-om-operations.md', 'znsight-man/46-OM-운영관리-개발.md'],
    deepDive: deepDive('OM 아키텍처', [
      'tcf-om port 8097, context `/om`',
      '24 Handler — Catalog·통제·Timeout·Auth·Dashboard',
      '기동 시 seed + cache evict API',
      'om-service 레거시 폐기, tcf-om 단일 SoT',
    ]),
  },

  '제05편/16-API-Gateway-UI-채널': {
    mermaid: `flowchart LR
  Browser --> UI[tcf-ui Relay]
  Browser --> UJ[tcf-uj]
  UJ --> GW[Gateway GEF]
  GW --> SV["sv-service /bc/online"]`,
    samples: [
      { title: 'TransactionRelayService (tcf-ui)', file: 'tcf-ui/src/main/java/com/nh/nsight/tcf/ui/client/TransactionRelayService.java', start: 1, end: 55 },
      { title: 'GatewayRouteDispatcher', file: 'tcf-gateway/src/main/java/com/nh/nsight/gateway/client/GatewayRouteDispatcher.java', start: 1, end: 55 },
    ],
    refs: ['docs/architecture/51-api-gateway.md', 'zarchitecture/13-UI-채널-아키텍처.md', 'tcf-gateway/docs/ROUTING_TABLE.md'],
    deepDive: deepDive('Gateway · UI · 채널', [
      'Gateway STF/GRF/GSF/GEF 4계층',
      'tcf-ui gateway-relay-enabled vs direct bootRun',
      'channelId Header — 채널별 통계·통제',
      'ProxyController per businessCode',
    ]),
  },

  '제05편/17-Batch-Scheduler-이벤트': {
    mermaid: `flowchart LR
  BATCH[tcf-batch Scheduler] --> OM[OM Dashboard]
  EB[eb-service Event] --> EP[ep-service Process]`,
    samples: [
      { title: 'SessionStatusCollectScheduler', file: 'tcf-batch/src/main/java/com/nh/nsight/tcf/batch/application/scheduler/SessionStatusCollectScheduler.java', start: 1, end: 40 },
      { title: 'EbEventHandler', file: 'eb-service/src/main/java/com/nh/nsight/marketing/eb/entry/handler/EbEventHandler.java', start: 1, end: 40 },
    ],
    refs: ['zarchitecture/12-배치-모니터링-아키텍처.md', 'zarchitecture/14-이벤트-연계-아키텍처.md'],
    deepDive: deepDive('Batch · Scheduler · 이벤트', [
      'tcf-batch → OM metrics·Dashboard feed',
      'EB=이벤트 브릿지, EP=실시간 처리 WAR',
      'Batch Job ID 명명규칙(명명규칙-20)',
      '온라인 TCF 파이프라인과 분리된 비동기 영역',
    ]),
  },

  '제05편/18-데이터-DB-아키텍처': {
    mermaid: `flowchart TB
  subgraph omfile [nsight_om]
    CAT[OM_SERVICE_CATALOG]
    TXL[TCF_TX_LOG]
  end
  SV[sv-service] -->|INSERT| TXL
  OM[tcf-om] --> CAT
  RDW[(업무 RDW)] --> SV`,
    samples: [
      { title: 'schema.sql', file: 'tcf-om/src/main/resources/schema.sql', start: 1, end: 60 },
      { title: 'application.yml (sv)', file: 'sv-service/src/main/resources/application.yml', start: 1, end: 40 },
    ],
    refs: ['zarchitecture/09-데이터-DB-아키텍처.md', 'docs/architecture/19-tcf-table.md'],
    deepDive: deepDive('데이터·DB 아키텍처', [
      'OM 23테이블 + Spring Session + TCF_TX_LOG',
      '업무 RDW vs OM H2(local) 분리',
      'MyBatis DAO only — JPA 업무 사용 금지',
      '마스킹·거버넌스 OM DataAuth',
    ]),
  },

  '제06편/19-로컬-개발환경': {
    mermaid: `flowchart LR
  JDK[JDK 17] --> GRADLE[Gradle bootRun]
  YML[application-local.yml] --> H2[H2 OM+TxLog]
  H2 --> CURL[curl /online]`,
    samples: [
      { title: 'application-local.yml', file: 'sv-service/src/main/resources/application-local.yml', start: 1, end: 40 },
      { title: 'ztomcat README', file: 'ztomcat/README.md', start: 1, end: 40 },
    ],
    refs: ['znsight-man/63-로컬-빌드-방법.md', 'znsight-man/06-로컬-개발환경-구성.md', 'ztomcat/README.md'],
    deepDive: deepDive('로컬 개발환경', [
      'profile local/dev/prod — tcf-cicd SoT',
      'H2 + InMemory Idempotency + session skip',
      'bootRun 단독 vs ztomcat ALL_MODULES',
      'seed-function-auth.sql 로컬 권한',
    ]),
  },

  '제06편/20-CICD-릴리즈-DR': {
    mermaid: `flowchart LR
  MR --> Build --> Test --> WAR --> Deploy --> Smoke
  Smoke -->|fail| Rollback`,
    samples: [
      { title: 'cicd-build.sh', file: 'tcf-cicd/scripts/cicd-build.sh', start: 1, end: 40 },
      { title: 'manifest.yaml', file: 'tcf-cicd/manifest.yaml', start: 1, end: 40 },
    ],
    refs: ['docs/architecture/49-release-strategy.md', 'docs/architecture/45-disaster-recovery.md', 'znsight-man/65-CICD-파이프라인-기준.md'],
    deepDive: deepDive('CI/CD · 릴리즈 · DR', [
      'tcf-cicd = profile별 yml·Apache·Tomcat SoT',
      'Smoke 실패 → Rollback WAR pin',
      'DR·관측성 Gap — 제10편 32장',
      'Git branch + MR 품질 게이트',
    ]),
  },

  '제07편/21-테스트-전략': {
    mermaid: `flowchart LR
  UNIT[단위 Handler/Service] --> INT[통합 /online]
  INT --> E2E[curl JSON E2E]
  E2E --> GATE[품질 게이트]`,
    samples: [
      { title: 'sv-sample-inquiry.json', file: 'tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json' },
      { title: 'curl-sv-sample.sh', file: 'tcf-scripts/curl-sv-sample.sh', start: 1, end: 30 },
    ],
    refs: ['docs/architecture/50-test-architecture.md', 'znsight-man/56-TCF-거래-테스트-기준.md', 'znsight-man/부록H-개발-완료-체크리스트.md'],
    deepDive: deepDive('테스트 전략', [
      'Handler 단위 + /online 통합 + curl E2E 3층',
      'STF~ETF 포함 통합 테스트 권장',
      'SQL·보안·성능 테스트 MR 게이트',
      '부록 H·I 체크리스트와 연계',
    ]),
  },

  '제08편/22-조회-거래-SV-고객요약': {
    mermaid: `sequenceDiagram
  participant C as curl/UI
  participant SV as sv-service
  participant H as SvCustomerHandler
  participant M as SvCustomerMapper
  C->>SV: POST /sv/online
  SV->>H: selectSummary
  H->>M: SQL
  M-->>C: StandardResponse`,
    samples: [
      { title: 'SvCustomerHandler', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java' },
      { title: 'SvCustomerMapper.xml', file: 'sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml', start: 1, end: 80 },
      { title: 'curl JSON', file: 'tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json' },
    ],
    refs: ['znsight-man/71-SV-고객요약조회-샘플.md', 'zguide/sv-service-개발가이드.md', 'zman/22-업무서비스샘플.md'],
    deepDive: deepDive('SV 고객요약 조회 실습', [
      '`SV.Customer.selectSummary` — 표준 실습 거래',
      'E2E: JSON → Handler → Facade → Mapper',
      'OM Catalog·통제·Timeout 등록 필수',
      'tcf-ui Relay로 UI 테스트',
    ]),
  },

  '제08편/23-목록-페이징-등록-변경': {
    mermaid: `flowchart TB
  INQ[목록 count+select] --> PAGE[offset/limit]
  REG[등록 save] --> TX[@Transactional Facade]
  EAI[tcf-eai] --> EXT[외부 WAR 호출]`,
    samples: [
      { title: 'SvCustomerMapper paging', file: 'sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml', start: 1, end: 80 },
      { title: 'TcfServiceClient', file: 'tcf-eai/src/main/java/com/nh/nsight/tcf/eai/client/TcfServiceClient.java', start: 1, end: 70 },
    ],
    refs: ['znsight-man/72-목록조회-페이징-샘플.md', 'znsight-man/73-등록변경-거래-샘플.md', 'znsight-man/74-외부-서비스-호출-샘플.md'],
    deepDive: deepDive('목록·페이징·등록·변경', [
      'count + select SQL 분리 페이징',
      'CREATE/UPDATE processingType + 감사로그',
      'WAR간 호출 = 표준전문 POST via tcf-eai',
      'Validation Rule + BusinessException',
    ]),
  },

  '제09편/24-tcf-core-web-util': {
    mermaid: `flowchart TB
  UTIL[tcf-util] --> CORE[tcf-core STF/TCF/ETF]
  CORE --> WEB[tcf-web OnlineController]
  WEB --> WAR[업무 *-service WAR]`,
    samples: [
      { title: 'OnlineTransactionController', file: 'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java' },
      { title: 'STF', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java', start: 1, end: 35 },
    ],
    refs: ['zguide/tcf-core-개발가이드.md', 'docs/architecture/33-TCF.md', 'tcf-web/README.md'],
    deepDive: deepDive('tcf-core · web · util', [
      'AutoConfiguration — TcfCore/Web/Security',
      '모든 업무 WAR가 tcf-web 의존',
      'tcf-util HTTP·Gateway session helpers',
      'AOP Timeout·@Transactional Facade',
    ]),
  },

  '제09편/25-tcf-om-ui-uj': {
    mermaid: `flowchart LR
  OM[tcf-om :8097/om] --> UI[tcf-ui :8099 Relay]
  UI --> UJ[tcf-uj :8102 Gateway UI]`,
    samples: [
      { title: 'OmServiceCatalogHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmServiceCatalogHandler.java', start: 1, end: 50 },
      { title: 'TransactionRelayService (ui)', file: 'tcf-ui/src/main/java/com/nh/nsight/tcf/ui/client/TransactionRelayService.java', start: 1, end: 55 },
    ],
    refs: ['zguide/tcf-om-개발가이드.md', 'zguide/tcf-ui-개발가이드.md', 'zguide/tcf-uj-개발가이드.md', 'zarchitecture/05-운영관리-OM-아키텍처.md'],
    deepDive: deepDive('tcf-om · ui · uj', [
      'tcf-om 24 Handler — 운영 SoT',
      'tcf-ui = 개발 Relay + sample JSON + Admin HTML',
      'tcf-uj = Gateway 경유 UI (8102)',
      'gateway-relay-enabled 프로파일 전환',
    ]),
  },

  '제09편/26-tcf-gateway-jwt': {
    mermaid: `flowchart LR
  Client --> GW[tcf-gateway :8100]
  JWT[tcf-jwt :8110] -->|JWKS| GW
  GW -->|Proxy| WAR["/{bc}/online"]`,
    samples: [
      { title: 'GatewayRouteDispatcher', file: 'tcf-gateway/src/main/java/com/nh/nsight/gateway/client/GatewayRouteDispatcher.java', start: 1, end: 55 },
      { title: 'SvProxyController', file: 'tcf-gateway/src/main/java/com/nh/nsight/gateway/entry/web/SvProxyController.java', start: 1, end: 35 },
      { title: 'JwkSetController', file: 'tcf-jwt/src/main/java/com/nh/nsight/auth/jwt/entry/web/JwkSetController.java', start: 1, end: 25 },
    ],
    refs: ['zguide/tcf-gateway-개발가이드.md', 'zguide/tcf-jwt-개발가이드.md', 'docs/architecture/51-api-gateway.md', 'tcf-gateway/docs/ROUTING_TABLE.md'],
    deepDive: deepDive('tcf-gateway · jwt', [
      'ztomcat context `/gw`, `/jwt`',
      'GatewayRouteCatalog + ROUTING_TABLE.md',
      'ProxyController per BC — AbstractBusinessProxyController',
      'Session + JWT 이중 인증 path',
    ]),
  },

  '제09편/27-tcf-eai-cache-batch': {
    mermaid: `flowchart LR
  EAI[tcf-eai Client] --> WAR2[타 WAR /online]
  CACHE[tcf-cache] --> OM[OM Cache evict]
  BATCH[tcf-batch] --> OM`,
    samples: [
      { title: 'TcfServiceClient', file: 'tcf-eai/src/main/java/com/nh/nsight/tcf/eai/client/TcfServiceClient.java', start: 1, end: 70 },
      { title: 'SessionStatusCollectScheduler', file: 'tcf-batch/src/main/java/com/nh/nsight/tcf/batch/application/scheduler/SessionStatusCollectScheduler.java', start: 1, end: 35 },
    ],
    refs: ['zguide/tcf-eai-개발가이드.md', 'zguide/tcf-cache-개발가이드.md', 'zguide/tcf-batch-개발가이드.md'],
    deepDive: deepDive('tcf-eai · cache · batch', [
      'WAR간 표준전문 POST — TcfServiceClient',
      'Cache eviction OM Handler 연동',
      'batch scheduler → OM Dashboard metrics',
      '온라인 TCF와 별도 모듈 lifecycle',
    ]),
  },

  '제09편/28-tcf-cicd-scripts': {
    mermaid: `flowchart LR
  CICD[tcf-cicd yml] --> BUILD[cicd-build.sh]
  BUILD --> DEPLOY[deploy-wars.sh]
  SCRIPTS[tcf-scripts curl/build] --> LOCAL[로컬 검증]`,
    samples: [
      { title: 'cicd-build.sh', file: 'tcf-cicd/scripts/cicd-build.sh', start: 1, end: 40 },
      { title: 'deploy-wars.sh', file: 'ztomcat/deploy-wars.sh', start: 1, end: 35 },
      { title: 'build-all.sh', file: 'tcf-scripts/build-all.sh', start: 1, end: 30 },
    ],
    refs: ['zguide/tcf-cicd-개발가이드.md', 'tcf-cicd/README.md', 'zguide/tcf-scripts-개발가이드.md', 'ztomcat/README.md'],
    deepDive: deepDive('tcf-cicd · scripts', [
      'tcf-cicd = local/dev/prod profile SoT',
      'sync-to-framework / pull-from-framework',
      'tcf-scripts = curl·build·deploy 단축',
      '13 WAR deploy-wars.sh ALL_MODULES',
    ]),
  },

  '제09편/29-업무-WAR-ic-pc-ms-sv-pd': {
    mermaid: `flowchart TB
  subgraph wars [8082-8087]
    IC[ic-service]
    PC[pc-service]
    MS[ms-service]
    SV[sv-service]
    PD[pd-service]
  end
  wars --> PAT[동일 6계층 패턴]`,
    samples: [
      { title: 'SvCustomerHandler (표준)', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java', start: 1, end: 40 },
      { title: 'IcCustomerHandler', file: 'ic-service/src/main/java/com/nh/nsight/marketing/ic/entry/handler/IcCustomerHandler.java', start: 1, end: 40 },
    ],
    refs: ['zarchitecture/04-업무-도메인-서비스-아키텍처.md', 'zguide/sv-service-개발가이드.md', 'zguide/ic-service-개발가이드.md'],
    deepDive: deepDive('업무 WAR ic·pc·ms·sv·pd', [
      'sv = 표준 실습·샘플 WAR',
      'ic = tcf-eai 연동 데모 상대',
      'pc/ms/pd = SampleHandler 확장 템플릿',
      '공통 `/{bc}/online` + marketing.{bc} 패키지',
    ]),
  },

  '제09편/30-업무-WAR-eb-ep-ss-mg': {
    mermaid: `flowchart LR
  EB[eb-service Event] --> EP[ep-service Process]
  SS[ss-service] --> MG[mg-service]
  GW[Gateway EbProxy] --> EB`,
    samples: [
      { title: 'EbEventHandler', file: 'eb-service/src/main/java/com/nh/nsight/marketing/eb/entry/handler/EbEventHandler.java', start: 1, end: 45 },
      { title: 'EpUserEventHandler', file: 'ep-service/src/main/java/com/nh/nsight/marketing/ep/entry/handler/EpUserEventHandler.java', start: 1, end: 40 },
    ],
    refs: ['zguide/eb-service-개발가이드.md', 'zguide/ep-service-개발가이드.md', 'zarchitecture/14-이벤트-연계-아키텍처.md'],
    deepDive: deepDive('업무 WAR eb·ep·ss·mg', [
      'eb = 이벤트 브릿지·배치 Handler',
      'ep = 실시간 이벤트 처리',
      'ss/mg = 지원 도메인 Sample',
      'tcf-eai(동기) vs eb/ep(이벤트) 역할 분리',
    ]),
  },

  '제10편/31-공식-설계안-매핑': {
    mermaid: `flowchart LR
  WORD[docs/설계자료 Word] --> ZMAN[zman]
  ZMAN --> CODE[Handler/OM 화면]
  ADR[NSIGHT-FINAL-ARCHITECTURE-DECISION] --> CODE`,
    samples: [
      { title: 'OmDashboardHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmDashboardHandler.java', start: 1, end: 35 },
    ],
    refs: ['docs/설계자료/README.md', 'docs/architecture/52-om-operations.md', 'zman/00-설계서-코드베이스-대조표.md'],
    deepDive: deepDive('공식 설계안 매핑', [
      'Word → zman → zarchitecture → code 3-way',
      'ADR 최종 아키텍처 결정 문서',
      'OM 화면 ↔ Handler ↔ Word 절 매핑',
      'Gap은 제32장·zman/23 참조',
    ]),
  },

  '제10편/32-Gap-보완-향후-과제': {
    mermaid: `flowchart TB
  GAP[zman Gap분석] --> PRI[우선순위]
  PRI --> OBS[관측성]
  PRI --> DR[DR]
  PRI --> WAR[17 WAR 미배포]`,
    samples: [
      { title: 'Gap 분석 (zman)', file: 'zman/23-소스Gap분석.md', start: 1, end: 40 },
      { title: '보완과제 우선순위', file: 'zman/24-보완과제-우선순위.md', start: 1, end: 40 },
    ],
    refs: ['zman/23-소스Gap분석.md', 'zman/24-보완과제-우선순위.md', 'docs/architecture/44-observability.md', 'docs/architecture/45-disaster-recovery.md'],
    deepDive: deepDive('Gap · 보완 · 향후 과제', [
      'CC/BC/CM 등 설계-only WAR',
      'Observability·Metrics Gap',
      'DR·Rollback 자동화 미완',
      '로드맵 = zman/24 우선순위 따름',
    ]),
  },

  '부록/A-업무코드-표준표': {
    mermaid: `flowchart TB
  BC17[17 업무코드] --> DEPLOY[9 WAR 배포]
  BC17 --> PKG[Package prefix]`,
    samples: [
      { title: 'BusinessModuleDefinitions', file: 'tcf-ui/src/main/java/com/nh/nsight/tcf/ui/support/BusinessModuleDefinitions.java', start: 1, end: 35 },
      { title: 'deploy-wars.sh', file: 'ztomcat/deploy-wars.sh', start: 1, end: 30 },
    ],
    refs: ['znsight-man/부록A-업무코드-표준표.md', 'zdoc/applicationNaming.md', 'zarchitecture/16-모듈-포트-의존성-레퍼런스.md'],
    deepDive: deepDive('부록 A · 업무코드', [
      'Header businessCode 필수',
      'URL `/{bc}/online` 패턴',
      '17 표준 vs 9 배포 — 신규 코드 등록 순서',
      'Context Path = WAR명 = Gradle 모듈',
    ]),
  },

  '부록/B-ServiceId-명명규칙': {
    mermaid: `flowchart LR
  FMT["BC.Domain.action"] --> REG[Handler.serviceIds]
  REG --> OM[OM Catalog]`,
    samples: [
      { title: 'SvCustomerHandler', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java', start: 1, end: 35 },
      { title: 'OmAuthHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmAuthHandler.java', start: 1, end: 35 },
    ],
    refs: ['znsight-man/부록B-ServiceId-명명규칙.md', 'zman/07-ServiceIdDispatcher.md', 'zdoc/applicationNaming.md'],
    deepDive: deepDive('부록 B · ServiceId', [
      'Dispatcher registry 키 = ServiceId',
      'action 표준表 — select/save/update/delete',
      '중복 serviceId 기동 실패',
      'Catalog 미등록 = SERVICE_NOT_FOUND',
    ]),
  },

  '부록/C-거래코드-명명규칙': {
    mermaid: `flowchart LR
  TX["BC-TYPE-NNNN"] --> HDR[Header transactionCode]
  HDR --> CTRL[거래통제·감사]`,
    samples: [
      { title: 'StandardHeader transactionCode', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardHeader.java', start: 1, end: 50 },
      { title: 'TransactionControlService', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/control/TransactionControlService.java', start: 1, end: 40 },
    ],
    refs: ['znsight-man/부록C-거래코드-명명규칙.md', 'zdoc/applicationNaming.md'],
    deepDive: deepDive('부록 C · 거래코드', [
      'ServiceId 1:N 거래코드 가능',
      'TYPE: INQ/REG/UPD/DEL 등',
      'Catalog 상태머신과 연동',
      'E-TCF-HDR-002 transactionCode 검증',
    ]),
  },

  '부록/D-표준-전문-JSON-예시': {
    mermaid: `flowchart LR
  REQ["Request JSON"] --> API["POST /online"]
  API --> RES["Response success/fail"]`,
    samples: [
      { title: 'sv-sample-inquiry.json', file: 'tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json' },
    ],
    refs: ['znsight-man/부록D-표준-전문-예시.md', 'zdoc/전문관리.md', 'docs/architecture/02-junmun.md'],
    deepDive: deepDive('부록 D · 표준 전문 JSON', [
      'header + body 요청, header + result + body 응답',
      'SV/OM 시나리오별 샘플',
      '오류 시 result.errorCode/errorMessage',
      'curl·tcf-ui Relay 테스트용',
    ]),
  },

  '부록/E-Mapper-XML-템플릿': {
    mermaid: `flowchart LR
  DAO[SvCustomerDao] --> XML[SvCustomerMapper.xml]
  XML --> SQL[RDW SELECT]`,
    samples: [
      { title: 'SvCustomerMapper.xml', file: 'sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml', start: 1, end: 50 },
    ],
    refs: ['znsight-man/부록E-Mapper-XML-템플릿.md', 'znsight-man/28-MyBatis-Mapper-개발.md', 'docs/architecture/26-mybatis.md'],
    deepDive: deepDive('부록 E · Mapper XML', [
      'namespace = DAO FQCN',
      'ResultMap + dynamic SQL',
      'paging = count + select 분리',
      'RDW/ADW datasource 분리',
    ]),
  },

  '부록/F-오류코드-표준표': {
    mermaid: `flowchart LR
  BE[BusinessException] --> ETF[ETF Result]
  ETF --> OM[OM ErrorCode CRUD]`,
    samples: [
      { title: 'ErrorCode', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/ErrorCode.java', start: 1, end: 50 },
      { title: 'BusinessException', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/BusinessException.java' },
      { title: 'OmErrorCodeHandler', file: 'tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmErrorCodeHandler.java', start: 1, end: 35 },
    ],
    refs: ['znsight-man/부록F-오류코드-표준표.md', 'zdoc/예외처리.md', 'znsight-man/35-예외처리-기준.md'],
    deepDive: deepDive('부록 F · 오류코드', [
      'E-COM-* 공통, E-TCF-HDR/CTL/TIME-*',
      'E-JWT-AUTH-* Gateway',
      'ETF가 errorCode → result 매핑',
      '업무 E-{BC}-* 확장 규칙',
    ]),
  },

  '부록/G-application-yml-템플릿': {
    mermaid: `flowchart TB
  APP[application.yml] --> PROF[profile local/dev/prod]
  PROF --> TCF[application-tcf.yml]`,
    samples: [
      { title: 'application.yml', file: 'sv-service/src/main/resources/application.yml', start: 1, end: 50 },
      { title: 'application-local (cicd)', file: 'tcf-cicd/local/spring/sv-service/application-local.yml', start: 1, end: 35 },
    ],
    refs: ['znsight-man/부록G-application-yml-템플릿.md', 'tcf-cicd/README.md', 'docs/architecture/20-env-spring.md'],
    deepDive: deepDive('부록 G · application.yml', [
      'Hikari + MyBatis + TCF flags',
      'secret 외부화 — repo에 비밀번호 금지',
      'tcf-cicd profile SoT',
      'session-validation-enabled 로컬',
    ]),
  },

  '부록/H-개발-완료-체크리스트': {
    mermaid: `flowchart LR
  ID[ServiceId·거래코드] --> CODE[6계층+SQL]
  CODE --> TEST[curl 통과]
  TEST --> OM[OM 등록]`,
    samples: [
      { title: 'SvCustomerHandler', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java', start: 1, end: 30 },
      { title: 'curl-sv-sample.sh', file: 'tcf-scripts/curl-sv-sample.sh', start: 1, end: 25 },
    ],
    refs: ['znsight-man/부록H-개발-완료-체크리스트.md', 'znsight-man/62-품질-게이트-기준.md', 'znsight-man/47-ServiceId-등록-절차.md'],
    deepDive: deepDive('부록 H · 개발 완료', [
      'ServiceId·거래코드·Catalog 3점 일치',
      'Mapper·SQL review',
      '로컬 curl E2E 통과',
      'functionAuth OM seed',
    ]),
  },

  '부록/I-코드-리뷰-체크리스트': {
    mermaid: `flowchart LR
  MR[Pull Request] --> REV[15항 체크]
  REV --> APPROVE[승인/반려]`,
    samples: [
      { title: 'SvCustomerHandler layering', file: 'sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java', start: 1, end: 40 },
      { title: 'SvCustomerMapper.xml', file: 'sv-service/src/main/resources/mapper/sv/SvCustomerMapper.xml', start: 1, end: 30 },
    ],
    refs: ['znsight-man/부록I-코드-리뷰-체크리스트.md', 'znsight-man/61-코드-리뷰-기준.md', 'znsight-man/42-보안-코딩-기준.md'],
    deepDive: deepDive('부록 I · 코드 리뷰', [
      'Handler = switch only, Facade = @Transactional',
      'SQL injection·로그 마스킹',
      'OM 등록 누락 = Blocker',
      '테스트 evidence MR 필수',
    ]),
  },

  '부록/J-운영-전환-체크리스트': {
    mermaid: `flowchart LR
  BUILD[WAR 빌드] --> OMSEED[OM prod seed]
  OMSEED --> DEPLOY[배포]
  DEPLOY --> SMOKE[Smoke]
  SMOKE -->|fail| RB[Rollback]`,
    samples: [
      { title: 'cicd-deploy.sh', file: 'tcf-cicd/scripts/cicd-deploy.sh', start: 1, end: 35 },
      { title: 'deploy-wars.sh', file: 'ztomcat/deploy-wars.sh', start: 1, end: 30 },
      { title: 'data.sql seed', file: 'tcf-om/src/main/resources/data.sql', start: 1, end: 30 },
    ],
    refs: ['znsight-man/부록J-운영-전환-체크리스트.md', 'znsight-man/68-운영-전환-체크리스트.md', 'znsight-man/67-롤백-절차.md'],
    deepDive: deepDive('부록 J · 운영 전환', [
      '10영역 체크 — Catalog·통제 prod',
      'Gateway route prod yml',
      'Rollback WAR version pin',
      '운영자 인수인계·Smoke',
    ]),
  },

  '부록/K-모듈-포트-Context-WAR-매핑표': {
    mermaid: `flowchart TB
  MOD[22 모듈] --> PORT[포트]
  PORT --> CTX[Context Path]
  CTX --> WAR[WAR 파일명]`,
    samples: [
      { title: 'BusinessModuleDefinitions', file: 'tcf-ui/src/main/java/com/nh/nsight/tcf/ui/support/BusinessModuleDefinitions.java', start: 1, end: 35 },
      { title: 'ROUTING_TABLE', file: 'tcf-gateway/docs/ROUTING_TABLE.md', start: 1, end: 40 },
    ],
    refs: ['zarchitecture/16-모듈-포트-의존성-레퍼런스.md', 'ztomcat/README.md', 'tcf-gateway/docs/ROUTING_TABLE.md'],
    deepDive: deepDive('부록 K · 모듈·포트·WAR', [
      'bootRun root vs ztomcat `/gw` `/jwt`',
      'Gateway downstream `/{bc}/online`',
      'tcf-ui 8099, tcf-uj 8102, gateway 8100',
      'settings.gradle 모듈 목록 = SoT',
    ]),
  },

  '부록/L-TCF-핵심-테이블-DDL-요약': {
    mermaid: `flowchart TB
  OM23[OM 23 tables] --> CAT[OM_SERVICE_CATALOG]
  TXL[TCF_TX_LOG] --> ALL[모든 WAR]`,
    samples: [
      { title: 'schema.sql', file: 'tcf-om/src/main/resources/schema.sql', start: 1, end: 60 },
      { title: 'data.sql', file: 'tcf-om/src/main/resources/data.sql', start: 1, end: 30 },
    ],
    refs: ['docs/architecture/19-tcf-table.md', 'tcf-om/src/main/resources/schema.sql'],
    deepDive: deepDive('부록 L · DDL', [
      'OM_SERVICE_CATALOG·TCF_TX_CONTROL 핵심',
      'TCF_TX_LOG cross-WAR INSERT',
      'Spring Session tables',
      'seed data.sql + migration',
    ]),
  },

  '부록/M-명명규칙-21주제-색인': {
    mermaid: `flowchart LR
  N01[명명규칙 01~21] --> MAN[znsight-man]
  MAN --> BOOK[제02편·부록A~C]`,
    samples: [
      { title: '명명규칙-07-ServiceId', file: 'znsight-man/명명규칙-07-ServiceId.md', start: 1, end: 30 },
      { title: '명명규칙-12-MyBatis', file: 'znsight-man/명명규칙-12-MyBatis-Mapper-SQL.md', start: 1, end: 30 },
    ],
    refs: ['znsight-man/명명규칙-00-목차.md', 'znsight-man/명명규칙-01-총정리.md', 'docs/architecture/53-naming-conventions.md'],
    deepDive: deepDive('부록 M · 명명규칙 색인', [
      '21주제 → 본편·부록 역매핑',
      'Gateway·Batch·Cache = 18~20',
      'docx → md 재생성 절차',
      '신규 식별자 = 명명규칙-00 목차',
    ]),
  },

  '부록/N-소스-인덱스': {
    mermaid: `flowchart TB
  CORE[tcf-core/web] --> WAR[업무 *-service]
  GW[tcf-gateway/jwt] --> UI[tcf-ui/uj]
  OM[tcf-om 24 handlers]`,
    samples: [
      { title: 'TCF.process', file: 'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java', start: 1, end: 35 },
      { title: 'OnlineTransactionController', file: 'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java', start: 1, end: 35 },
      { title: 'SOURCE_INDEX', file: 'docs/SOURCE_INDEX.md', start: 1, end: 40 },
    ],
    refs: ['docs/SOURCE_INDEX.md', 'docs/architecture/33-TCF.md', 'zarchitecture/16-모듈-포트-의존성-레퍼런스.md'],
    deepDive: deepDive('부록 N · 소스 인덱스', [
      '패키지별 핵심 클래스表',
      'Handler 경로 `{bc}/entry/handler/*Handler`',
      '온라인 10단: Controller→TCF→STF→Dispatcher→Handler→ETF',
      'docs/SOURCE_INDEX.md 최신 유지',
    ]),
  },
};

module.exports = { MASTER_ENRICH, deepDive };
