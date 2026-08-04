param(
    [string]$DecisionRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$listPath = Join-Path $DecisionRoot '농협 상호금융 NSIGHT 아키텍처 의사결정 사항 목록.md'
$detailPath = Join-Path $DecisionRoot '2026-08-02-아키테처-의사결정-TASK-상세.md'
$listLines = Get-Content -Encoding UTF8 $listPath
$detailLines = Get-Content -Encoding UTF8 $detailPath

$prefixFolders = [ordered]@{
    GOV='01_거버넌스_아키텍처_관리'; APP='02_업무_도메인_애플리케이션_아키텍처'; STD='03_명명_식별자_추적성'
    UI='04_단말_UI_아키텍처'; SEC='05_개인정보_보안_암호화'; AUTH='06_인증_인가_JWT_세션'
    MCA='07_MCA_채널_전문_아키텍처'; INT='08_EAI_외부연계_업무_간_연계'; DATA='09_데이터_DB_SQL_아키텍처'
    REL='10_오류_Timeout_트랜잭션_안정성'; INF='11_기술_인프라_용량_아키텍처'; BFC='12_배치_파일_캐시_아키텍처'
    OPS='13_운영_로그_감사_모니터링'; QLT='14_DevOps_CI_CD_품질검증'
}

$areaGuide = @{
    GOV=@{
        Name='거버넌스·아키텍처 관리'; Plain='누가, 언제, 어떤 근거로 기술 결정을 내리고 변경·폐기까지 책임질지를 정하는 영역'
        Boundary='프로젝트 책임자·ARB·SA·각 영역 아키텍트·PMO·QA 사이의 의사결정 권한과 증적 흐름'
        Components='TASK 원장, ADR, RACI, Architecture Gate, 예외승인서, Risk·Gap·기술부채 원장'
        Failure='구두 결정, 승인자 부재, 조건부 조치 미추적, 만료 없는 예외, 문서와 구현의 기준선 불일치'
        Verify='승인 이력과 Baseline, 대체 ADR 연결, Gate 차단항목, 조건부 조치의 책임자·기한·검증자'
        Example='ServiceId 규칙 변경 시 SA가 TASK를 등록하고 FW·AA·OM·QA가 영향을 검토한 뒤 ARB가 호환기간과 자동검사를 포함해 승인'
    }
    APP=@{
        Name='업무·도메인·애플리케이션'; Plain='업무 기능을 어느 WAR와 도메인에 배치하고 Handler부터 Mapper까지 책임을 어떻게 나눌지 정하는 영역'
        Boundary='공통 Controller/TCF → Handler → Facade → Service → Rule → DAO/Mapper와 WAR 간 계약'
        Components='tcf-core, tcf-web, 업무 WAR, TransactionHandler, Facade, Service, Rule, DAO, Mapper, DTO'
        Failure='TCF 우회, Handler의 업무로직·SQL 포함, Facade 밖 트랜잭션 혼재, 다른 WAR 코드·DB 직접참조, 순환 의존'
        Verify='ArchUnit 계층검사, Handler serviceIds 중복검사, 업무 WAR 빌드, 정상·업무오류·Rollback 통합시험'
        Example='SV.Customer.selectSummary 요청이 공통 /sv/online에서 TCF를 거쳐 CustomerHandler가 Facade를 호출하고 Mapper가 Bind SQL만 실행'
    }
    STD=@{
        Name='명명·식별자·추적성'; Plain='화면에서 SQL과 운영로그까지 같은 거래를 일관된 식별자로 찾아갈 수 있게 이름과 관계를 정하는 영역'
        Boundary='화면 ID·이벤트 ID·ServiceId·거래코드·Java 이름·Mapper ID·DB 객체·설정 Key·TraceId'
        Components='명명사전, 관리대장, Service Catalog, 소스 색인, Checkstyle, ArchUnit, 정규식 검사'
        Failure='동일 대상을 서로 다른 이름으로 표현, 임시 ID 고착, 중복 ServiceId, 소스와 OM 불일치, 추적 단절'
        Verify='정규식·중복검사, 화면–ServiceId–Handler–Mapper–Table–로그 추적성 매트릭스 대조'
        Example='화면 이벤트가 SV.Customer.selectSummary와 Handler 상수, OM Catalog, 거래로그 serviceId 및 Mapper SQL ID로 연결'
    }
    UI=@{
        Name='단말·UI'; Plain='사용자 화면이 입력·오류·권한·파일·상태를 안전하고 일관되게 처리하도록 정하는 영역'
        Boundary='WEBTOPSUITE·React·일반 Web → Gateway/MCA → 공통 온라인 Endpoint'
        Components='tcf-ui, tcf-uj, 공통 UI 컴포넌트, Validation, 오류표시, Token 저장, 파일·엑셀 처리'
        Failure='UI 검증만 신뢰, 원문 개인정보 수신 후 숨김, Token 장기저장, origin 미검증, 전체 데이터 브라우저 적재'
        Verify='브라우저 호환·접근성·보안·대용량 시험과 서버 우회 요청을 포함한 통합시험'
        Example='조회 버튼은 중복 클릭을 막지만 서버도 멱등키를 검증하고, 서버가 권한에 맞게 마스킹한 응답만 화면에 전달'
    }
    SEC=@{
        Name='개인정보·보안·암호화'; Plain='민감정보가 저장·전송·표시·로그·파일 어디에서도 불필요하게 노출되지 않도록 통제하는 영역'
        Boundary='데이터 분류 → 권한 확인 → 서버 마스킹/암복호화 → 응답·로그·파일 → 감사'
        Components='개인정보 분류표, Security Adapter, Key Provider/KMS/HSM, 마스킹 정책, 감사로그, Secret Store'
        Failure='업무별 암호화 구현, 소스·YAML 평문 Key, 화면만 마스킹, 암호문·Token 로그, 영구 복호화 예외'
        Verify='권한별 원문·부분·제거 응답, 키 교체, 복호화 실패, 로그·Dump·파일 노출과 서버 우회 보안시험'
        Example='고객식별정보는 서버 정책이 사용자 목적과 권한을 확인해 원문·부분마스킹·필드제거 중 하나로 응답하고 원문조회는 감사'
    }
    AUTH=@{
        Name='인증·인가·JWT·세션'; Plain='사용자가 누구인지 확인하고 어떤 기능·데이터를 사용할 수 있는지 서버가 검증하는 영역'
        Boundary='SSO/IdP → tcf-jwt → Gateway 또는 WAR Filter → STF AuthenticationContext → Service/Rule 권한검사'
        Components='tcf-jwt, tcf-gateway, JWT Claim, StandardHeader, SessionValidator, AuthorizationValidator, OM 권한정보'
        Failure='클라이언트 Header 신뢰, 업무 Service별 서명검증, Private Key 분산, 만료·재사용 미검사, 인증 예외 URL 확대'
        Verify='정상·위변조·만료·재사용·강제로그아웃·기능권한·데이터권한·예외 URL 우회 시험'
        Example='Gateway가 JWT 서명을 검증하고 STF가 Claim과 Header를 대조한 뒤 Service/Rule이 지점·고객 범위의 데이터권한을 확인'
    }
    MCA=@{
        Name='MCA·채널·전문'; Plain='서로 다른 단말과 서버가 같은 요청·응답 의미를 공유하도록 전문 구조·필드·버전·라우팅을 정하는 영역'
        Boundary='단말 모델 → MCA 표준 Header/Body → Gateway/TCF StandardRequest → StandardResponse'
        Components='표준 전문 Schema, StandardHeader, Result, 필드사전, 버전정책, 채널 라우팅·Timeout·오류 매핑'
        Failure='채널별 필드 의미 불일치, 길이 증가 미반영, Header 위변조, 무제한 전문, 하위호환 없는 변경'
        Verify='Schema/Contract Test, 필수·길이·코드·민감도 검증, 구·신 버전 병행, 오류·Timeout 변환 시험'
        Example='channelId와 serviceId가 검증된 표준 Header로 전달되고 업무 Body는 계약 버전별 Schema와 최대 크기를 준수'
    }
    INT=@{
        Name='EAI·외부연계·업무 간 연계'; Plain='다른 WAR·기간계·외부기관을 호출할 때 계약, 장애격리, 중복방지와 데이터 정합성을 보장하는 영역'
        Boundary='업무 Service/Facade → tcf-eai 또는 공통 Client → EAI/외부 시스템 → 응답·대사·보상'
        Components='tcf-eai, 서비스 계약, 동기·비동기 Client, 멱등키, 재시도, Circuit Breaker, 대사·보상'
        Failure='다른 WAR Java Import, 타 업무 DB 직접변경, 무조건 재시도, Timeout 역전, 부분성공 상태 미관리'
        Verify='계약·Timeout·중복·재시도·부분실패·보상·외부 오류 매핑·대사·장애격리 시험'
        Example='업무 WAR 간 호출은 serviceId 계약과 tcf-eai를 사용하고 변경 요청은 멱등키와 처리상태로 중복을 차단'
    }
    DATA=@{
        Name='데이터·DB·SQL'; Plain='데이터의 소유권·모델·접근·동시성·성능·보존과 변경 배포 방식을 정하는 영역'
        Boundary='업무 도메인 → DAO/Mapper → 소유 데이터베이스와 승인된 조회·연계 계약'
        Components='논리·물리 모델, 표준용어, MyBatis Mapper, Bind SQL, Index, Lock, Migration, 보존·폐기 정책'
        Failure='타 업무 테이블 직접변경, 문자열 결합 SQL, 무제한 조회, 실행계획 미검증, 스키마 선후관계·Rollback 누락'
        Verify='모델·DDL·Mapper 정합성, SQL Injection, 실행계획, 페이징, Lock, Rollback, Migration 전·후 검증'
        Example='소유 도메인만 테이블을 변경하고 다른 업무는 공개 계약으로 요청하며 Mapper는 파라미터 바인딩을 사용'
    }
    REL=@{
        Name='오류·Timeout·트랜잭션·안정성'; Plain='실패가 발생해도 응답·데이터·외부 처리 상태가 예측 가능하고 복구 가능하도록 정하는 영역'
        Boundary='UI/MCA Timeout → 온라인 Timeout → Transaction Timeout → DB Query/외부호출 Timeout과 ETF 오류 응답'
        Components='ErrorCode, BusinessException, ETF, Timeout Policy, @Transactional, 멱등성, 재시도, 보상·재처리'
        Failure='상위 Timeout 후 하위 처리 지속, Checked Exception 미Rollback, 비멱등 변경 재시도, 내부 예외 노출'
        Verify='정상·업무오류·시스템오류·Timeout·Rollback·중복·부분성공·재처리 시나리오와 최종 상태 대조'
        Example='온라인 Timeout보다 DB Query Timeout을 짧게 두고 실패 시 ETF가 표준 오류를 만들며 변경 데이터는 Rollback'
    }
    INF=@{
        Name='기술·인프라·용량'; Plain='WAR·Tomcat·JVM·Thread·Connection·네트워크를 목표 부하와 장애범위에 맞게 배치하는 영역'
        Boundary='L4/Apache/Gateway → Tomcat/JVM/WAR → Thread/DB Pool → DB·외부 시스템과 DR'
        Components='ztomcat, tcf-cicd, Context/Port, JVM/GC, Connector Thread, HikariCP, Health, 배포·Rollback, DR'
        Failure='근거 없는 Pool 확대, 단일 JVM 장애 확산, Context 충돌, 환경값 하드코딩, Health와 준비상태 혼동'
        Verify='TPS·p95·Stress·Soak·Failover·DR·배포·Rollback 시험과 Thread/Heap/GC/Pool/DB Session 합산 확인'
        Example='WAR별 Hikari Pool 합계가 DB Session 한도를 넘지 않도록 산정하고 장애 시 잔여 인스턴스가 목표 부하를 처리'
    }
    BFC=@{
        Name='배치·파일·캐시'; Plain='대량·주기·파일·반복조회 작업을 온라인 거래와 분리하고 재시작·대사·일관성을 보장하는 영역'
        Boundary='Scheduler/Job → Step/Chunk → DB·파일·외부연계, 그리고 Cache 원천·갱신·무효화'
        Components='tcf-batch, tcf-cache, Job/Step, Checkpoint, 파일명·Hash·암호화, EhCache Region, TTL·Eviction'
        Failure='전체 파일 Heap 적재, 중복 Job 실행, 재시작 위치 없음, 대사 누락, 캐시 원천 불명확·무효화 실패'
        Verify='재시작·중복·부분실패·대용량·파일 대사·암호화·캐시 Hit/Miss·TTL·무효화·장애복구 시험'
        Example='대용량 파일은 Streaming과 Checkpoint로 처리하고 완료 후 건수·금액·Hash를 대사하며 실패 파일을 격리'
    }
    OPS=@{
        Name='운영·로그·감사·모니터링'; Plain='거래와 장애를 빠르게 찾고 중요 행위의 책임과 변경 이력을 증명할 수 있게 운영 정보를 정하는 영역'
        Boundary='Gateway/TCF/업무/EAI/DB 로그·메트릭 → OM/수집·대시보드 → 알림·Runbook·감사'
        Components='TransactionLogService, TraceId, 구조화 로그, 감사 이벤트, tcf-om, tcf-batch, Service Catalog, Runbook'
        Failure='개인정보·Token 로그, Trace 단절, 성공 로그만 기록, 증상 중심 과다 알림, 운영 기준정보 Drift'
        Verify='GUID·TraceId 종단 추적, 로그 마스킹, 거래 시작·종료, 감사 완전성, 알림 조치가능성, Runbook 복구훈련'
        Example='TraceId 하나로 Gateway, TCF 거래로그, ServiceId, Mapper SQL과 외부연계 결과를 찾고 담당 Runbook으로 연결'
    }
    QLT=@{
        Name='DevOps·CI/CD·품질검증'; Plain='표준 위반과 결함을 사람의 기억이 아니라 빌드·테스트·배포 Gate에서 반복 차단하는 영역'
        Boundary='Commit/PR → Build → 정적·보안·구조·계약·통합 시험 → Artifact → 배포 승인 → Smoke/Health 검증'
        Components='Gradle Wrapper, CI Pipeline, ArchUnit, Checkstyle, SAST/SCA/Secret Scan, 테스트, Artifact Manifest, Drift 검사'
        Failure='환경별 비재현 빌드, main 직접반영, 검사 실패 무시, 미등록 ServiceId 배포, 근거 없는 예외 승인'
        Verify='동일 JDK·Wrapper 재현, 필수 Gate 실패 차단, Artifact 추적, 배포 후 대표 거래·Health, Rollback 검증'
        Example='PR에서 계층·ServiceId·보안·테스트 검사를 통과한 동일 Artifact만 환경별 설정을 주입해 순차 배포'
    }
}

$referenceMap = @{
    GOV=@('AGENTS.md','xdoc/agents/development-agent-guide.md','znsight-man/NSIGHT TCF 아키텍처 구축 방법론 - 3. Architecture Gate 단계별 체크리스트.md','ztcf-book-capacity-md/부록/AC-아키텍처-의사결정-기록.md')
    APP=@('zarchitecture/02-TCF-프레임워크-아키텍처.md','zarchitecture/03-애플리케이션-6계층-아키텍처.md','zarchitecture/04-업무-도메인-서비스-아키텍처.md','zdocs-1/architecture/01-application-layer.md')
    STD=@('zdocs-1/architecture/53-naming-conventions.md','zdocs-1/architecture/06-naming.md','zdocs-1/SOURCE_INDEX.md')
    UI=@('zarchitecture/13-UI-채널-아키텍처.md','tcf-ui/README.md','tcf-uj/README.md','zdocs-1/architecture/18-fileupdownload.md')
    SEC=@('zdocs-1/architecture/43-security-operations.md','zarchitecture/07-세션-인증-보안-아키텍처.md','xdoc/agents/security-agent.md')
    AUTH=@('zdocs-1/architecture/42-jwt.md','zdocs-1/architecture/51-api-gateway.md','tcf-jwt/README.md','tcf-gateway/README.md')
    MCA=@('zdocs-1/architecture/02-junmun.md','zarchitecture/13-UI-채널-아키텍처.md','zdocs-1/architecture/46-service-integration-contract.md')
    INT=@('zarchitecture/08-서비스-간-연동-아키텍처.md','zdocs-1/architecture/46-service-integration-contract.md','tcf-eai/build.gradle')
    DATA=@('zarchitecture/09-데이터-DB-아키텍처.md','zdocs-1/architecture/07-DAO.md','zdocs-1/architecture/26-mybatis.md','zdocs-1/architecture/47-data-governance.md')
    REL=@('zarchitecture/10-거래통제-Timeout-로깅-아키텍처.md','zdocs-1/architecture/03-transaction.md','zdocs-1/architecture/05-exception.md','zdocs-1/architecture/41-service-timeout-policy.md')
    INF=@('zarchitecture/01-전체-시스템-아키텍처.md','zarchitecture/15-배포-환경-CICD-아키텍처.md','ztomcat/README.md','zdocs-1/architecture/45-disaster-recovery.md')
    BFC=@('zarchitecture/11-캐시-아키텍처.md','zarchitecture/12-배치-모니터링-아키텍처.md','zdocs-1/architecture/13-batch.md','zdocs-1/architecture/18-fileupdownload.md')
    OPS=@('zdocs-1/architecture/37-transaction-log.md','zdocs-1/architecture/44-observability.md','zdocs-1/architecture/52-om-operations.md','tcf-om/README.md')
    QLT=@('build.gradle','zdocs-1/architecture/50-test-architecture.md','zdocs-1/architecture/49-release-strategy.md','tcf-cicd/README.md','xdoc/agents/quality-agent.md')
}

function Parse-TaskRows([string[]]$lines) {
    $result = @{}
    foreach ($line in $lines) {
        if ($line -match '^\|\s*([A-Z]+-\d+)\s*\|') {
            $cells = $line.Trim('|').Split('|') | ForEach-Object { $_.Trim() }
            if ($cells.Count -ge 2) { $result[$cells[0]] = $cells }
        }
    }
    return $result
}

function Relative-Link([string]$path) {
    $encoded = ($path -replace '\\','/') -replace ' ','%20'
    return "[$path](../../../$encoded)"
}

function Write-Utf8([string]$path, [string]$content) {
    Set-Content -LiteralPath $path -Value $content -Encoding UTF8
}

$template = @'
# {{ID}} {{TITLE}} 상세 설명서

> 문서 성격: **이해·검토용 상세 초안**  
> 상태: **ARB 승인 전** · 영역: **{{AREA}}** · 우선순위: **{{PRIORITY}}**

## 1. 이 문서를 먼저 읽는 이유

이 문서는 요약본 [{{BASEFILE}}](./{{BASELINK}})의 내용을 처음 접하는 사람도 {{ID}}의 의미, 필요한 이유, 실제로 결정할 항목과 완료 판단 방법을 이해할 수 있도록 풀어서 설명한다.

{{ID}}의 핵심 주제는 **{{TITLE}}**이다. 쉽게 말하면 **{{PLAIN}}**에서 `{{SCOPE}}`에 관한 공통 기준을 정하는 일이다. 이 기준을 정하지 않으면 팀마다 서로 다른 판단을 구현하고, 통합·보안·운영 단계에서 뒤늦게 충돌할 가능성이 높다.

## 2. 한눈에 보는 결정 카드

| 항목 | 내용 |
|---|---|
| TASK ID | {{ID}} |
| 의사결정 사항 | {{TITLE}} |
| 이번에 확정할 범위 | {{SCOPE}} |
| 권고 초안 | {{RECOMMENDATION}} |
| 이행 방향 | {{EXECUTION}} |
| 주관 | {{OWNER}} |
| 협의 | {{CONSULTED}} |
| 승인 | {{APPROVER}} |
| 우선순위 | {{PRIORITY}} |
| 필수 산출물 | {{DELIVERABLE}} |

## 3. 용어와 배경지식

### 3.1 영역의 의미

{{AREA}}는 **{{PLAIN}}**이다.

### 3.2 주요 용어

| 용어 | 이 문서에서의 의미 |
|---|---|
| TASK | 아직 확정되지 않은 아키텍처 질문과 필요한 조치를 추적하는 관리 단위 |
| ADR | 대안, 최종 선택, 선택·기각 근거, 영향과 폐기조건을 남기는 의사결정 기록 |
| Baseline | 승인되어 이후 설계·구현·시험이 따라야 하는 기준 버전 |
| Gate | 필수 결정과 증적을 확인해 다음 단계 진입 가능 여부를 판정하는 절차 |
| 예외 | 표준을 일시적으로 지키지 못할 때 위험·보완책·책임자·만료일을 승인받은 상태 |
| 추적성 | 요구사항에서 코드·설정·테스트·운영 증적까지 같은 결정을 찾아갈 수 있는 성질 |

## 4. 적용 범위와 적용하지 않는 범위

### 적용 범위

- 의사결정 주제: `{{SCOPE}}`
- 책임 경계: {{BOUNDARY}}
- 관련 구성요소: {{COMPONENTS}}
- 설계·개발·시험·배포·운영 중 이 결정의 영향을 받는 산출물
- 기존 구현을 변경할 경우 호환성, 전환, Rollback과 폐기 계획

### 기본적으로 적용하지 않는 범위

- {{ID}}와 직접 관련 없는 코드 정리나 대규모 이름 변경
- 별도 TASK가 소유하는 정책을 이 문서에서 임의로 재결정하는 작업
- 실제 근거 없이 특정 제품·수치·조직 절차를 확정 사실로 선언하는 작업
- 승인되지 않은 예외를 정상 기준처럼 문서화하는 작업

## 5. 왜 이 결정이 필요한가

### 결정하지 않았을 때

- 팀별로 `{{SCOPE}}` 해석이 달라져 동일 기능이 여러 방식으로 구현된다.
- 설계서와 코드·설정·OM·테스트가 서로 다른 기준을 가질 수 있다.
- 장애나 보안사고가 발생했을 때 책임과 복구 순서를 빠르게 판단하기 어렵다.
- 후반 통합시험에서 공통 규칙을 다시 맞추느라 대규모 재작업이 생긴다.

### 잘못 결정했을 때

대표 실패 유형은 **{{FAILURE}}**이다. 단기적으로 기능이 동작해도 운영 안정성, 변경 용이성, 감사 가능성과 장애 격리 능력이 약해질 수 있다.

### 올바르게 결정했을 때

- 개발자가 같은 질문을 반복하지 않고 승인된 기준을 바로 적용할 수 있다.
- 검토자는 문서 주장과 실제 구현을 동일한 증적으로 대조할 수 있다.
- 자동검증 가능한 규칙은 CI/CD에서 반복 차단된다.
- 예외와 기술부채가 숨겨지지 않고 책임자와 종료조건을 갖는다.

## 6. 현재 NSIGHT에서 확인할 기준

현재 저장소는 Java 21, Spring Boot 3.3.5와 Gradle 멀티모듈 구조를 사용한다. 기본 의존 방향은 `tcf-util → tcf-core → tcf-web → 업무/플랫폼 모듈`이며, 온라인 업무는 공통 TCF 진입과 `serviceId` 기반 Handler 디스패치를 기준으로 한다.

{{ID}} 검토자는 다음 순서로 사실을 확인한다.

1. 실제 실행 코드와 Spring Bean·Filter·AOP 등록
2. `build.gradle`, `settings.gradle`, Local·Dev·Prod 설정
3. 자동화 테스트와 샘플 요청
4. 배포 스크립트, OM 기준정보와 운영 설정
5. 현재 README와 아키텍처 문서
6. 과거 개발북과 참고 자료

문서와 구현이 다르면 구현을 우선 확인하되, 차이를 `현행`, `목표`, `전환 필요`로 구분해 기록한다.

## 7. 회의에서 반드시 답해야 할 질문

| 순서 | 결정 질문 | 답변에 포함할 내용 |
|---:|---|---|
| 1 | `{{SCOPE}}`의 단일 기준은 무엇인가? | 허용·권고·금지 규칙과 적용 대상 |
| 2 | 기준은 어느 경계에서 실행되는가? | {{BOUNDARY}} |
| 3 | 누가 작성·구현·검증·승인·운영하는가? | {{OWNER}}·{{CONSULTED}}·{{APPROVER}}의 역할 분리 |
| 4 | 기존 구현과 어떤 차이가 있는가? | 유지·변경·폐기 대상과 영향 경로 |
| 5 | 정상뿐 아니라 오류·Timeout·장애 시 어떻게 되는가? | 실패 감지, 안전한 종료, Rollback·복구·재처리 |
| 6 | 호환성을 어떻게 보장하는가? | 버전, 병행기간, Migration, 제거 시점 |
| 7 | 예외가 필요한 조건은 무엇인가? | 사유, 보완통제, 책임자, 만료일, 재검토일 |
| 8 | 무엇으로 준수를 증명하는가? | {{VERIFY}} |
| 9 | 완료로 인정할 최소 증적은 무엇인가? | {{DELIVERABLE}}, 테스트, 자동검증, 운영 확인 |

## 8. 선택 가능한 대안과 판단 방법

| 대안 | 적용 방식 | 적합한 상황 | 주의점 |
|---|---|---|---|
| A. 중앙 표준·공통 강제 | 프레임워크·공통 컴포넌트·Pipeline이 규칙을 실행 | 보안, 계약, 식별자처럼 편차 허용이 어려운 주제 | 공통 변경의 영향과 호환기간 관리 필요 |
| B. 영역 자율 + 경계 계약 | 내부 구현은 자율, 입력·출력·검증 계약만 공통화 | 구현 다양성이 필요하나 상호운용성이 중요한 주제 | 계약 밖 편차와 중복을 지속 점검해야 함 |
| C. 승인된 제한 예외 | 표준 적용이 불가능한 범위만 기간을 정해 예외 처리 | 외부기관 제약, 단계적 전환, 검증된 레거시 호환 | 영구 예외와 무기한 기술부채로 변질되기 쉬움 |

평가 시 보안·개인정보, 데이터 정합성, 성능·용량, 개발 난이도, 운영·장애대응, 비용, 전환 위험을 같은 표에서 비교한다. 단기 개발속도만으로 대안을 선택하지 않는다.

## 9. 권고안의 구체적 의미

### 권고 초안

**{{RECOMMENDATION}}**

이 문장은 방향만 제시한다. 실제 ADR에서는 다음을 값으로 확정해야 한다.

- 적용 모듈·업무·환경과 제외 범위
- 실행 계층·컴포넌트와 입력·출력 계약
- 오류코드·Timeout·Rollback·재처리 규칙
- 설정 Key, 기준정보, Schema 또는 ID 형식
- 보안·개인정보·감사 요구
- 성능·용량 기준값과 측정 조건
- 전환 일정, 호환기간, Rollback과 폐기조건

### 이행 방향

**{{EXECUTION}}**

이행 결과는 `{{DELIVERABLE}}` 하나만 제출하는 것으로 끝나지 않는다. 관련 소스·설정·테스트·자동검증·운영 절차가 같은 결정 ID로 추적되어야 한다.

## 10. 목표 구조와 정보 흐름

```text
요구사항·현행 문제
  → {{ID}} TASK 등록
  → 코드·설정·문서·운영 현행 조사
  → 대안과 영향도 비교
  → PoC 또는 대표 시나리오 검증
  → {{OWNER}} 권고안 작성 / {{CONSULTED}} 협의
  → {{APPROVER}} 승인 및 ADR Baseline
  → 개발표준·공통 구현·샘플 반영
  → 테스트·CI/CD·운영 검증
  → 적용 확인·예외 추적·정기 재검토
```

이 영역의 핵심 책임 경계는 다음과 같다.

```text
{{BOUNDARY}}
```

## 11. 역할별로 실제 해야 할 일

| 역할 | 수행 내용 | 제출 증적 |
|---|---|---|
| 주관 {{OWNER}} | 현행·문제·대안·권고안 작성, 영향 대상 조정 | TASK 분석서, 권고안, ADR 초안 |
| 협의 {{CONSULTED}} | 영역 요구와 부작용 검토, 테스트·운영 조건 제시 | 검토 의견, 영향도, 조건부 조치 |
| 승인 {{APPROVER}} | 선택·기각 근거, 잔여 위험과 예외 수용 여부 결정 | 승인 기록, ADR Baseline |
| 개발·플랫폼 | 승인 기준을 코드·설정·공통 모듈·샘플에 반영 | Diff, 빌드·테스트 결과 |
| QA·보안 | 정상·경계·오류·우회·회귀 시나리오 검증 | 시험 결과, 결함·조치 이력 |
| 운영·DevOps | 기준정보·관측·배포·Rollback·Runbook 반영 | OM 등록, Pipeline, 운영 확인 |

## 12. 단계별 수행 절차

### 12.1 등록

TASK ID, 제목, 문제, 결정기한, 우선순위, 주관·협의·승인자를 기록한다. P0는 개발팀이 서로 다른 구현을 시작하기 전에 확정하는 것이 원칙이다.

### 12.2 현행 조사

`rg`와 `rg --files`로 구현, 설정, 테스트, 샘플, 도움말과 운영 사용처를 찾는다. 화면·ServiceId·Handler·Mapper·DB·OM처럼 여러 영역을 잇는 주제는 한쪽 문서만 보고 판단하지 않는다.

### 12.3 대안 분석

대안별 장점뿐 아니라 실패모드, 전환비용, 운영부담과 폐기 가능성을 비교한다. 수치 판단이 필요하면 측정환경과 합격조건을 먼저 정한 뒤 PoC를 수행한다.

### 12.4 승인

ARB는 결론만 승인하지 않고 선택 근거, 기각안, 잔여 위험, 예외, 적용일과 폐기조건을 함께 승인한다. 조건부 승인은 책임자·기한·검증자가 없으면 허용하지 않는다.

### 12.5 구현·전파

가이드, 템플릿, Golden Sample, 공통 컴포넌트와 자동검사 중 필요한 수단을 제공한다. 교육과 공지만으로 반복 위반을 막기 어려운 규칙은 Pipeline이나 프레임워크로 강제한다.

### 12.6 완료·재검토

업무팀 적용과 운영 증적을 확인한 뒤 완료한다. 관련 기술·법규·외부 계약 또는 부하 조건이 바뀌면 재검토하고, 새 ADR이 기존 결정을 대체하면 양방향 링크를 남긴다.

## 13. 구체적인 적용 예시

### 정상적인 적용 예

{{EXAMPLE}}.

이 경우 TASK, ADR, 변경 파일, 테스트와 운영 등록이 연결되므로 누가 어떤 근거로 결정했고 실제로 어디에 적용되었는지 확인할 수 있다.

### 잘못된 적용 예

- 회의에서 구두 합의한 뒤 ADR·승인 기록 없이 여러 업무팀에 적용한다.
- 대표 정상경로만 확인하고 권한실패·Timeout·부분실패·Rollback을 검증하지 않는다.
- 문서에는 공통 기준이 있지만 업무마다 자체 구현을 유지하고 자동검증도 없다.
- 예외 사유는 기록했지만 책임자·만료일·제거 계획이 없다.

## 14. 오류·장애·예외 처리

| 상황 | 처리 원칙 | 남길 증적 |
|---|---|---|
| 기존 ADR과 충돌 | 신규 TASK에서 대체 여부를 명시하고 승인 전 확산 중지 | 충돌 분석, 대체 링크 |
| PoC 실패 | 실패 조건과 측정값을 보존하고 대안 또는 요구조건 재검토 | PoC 결과, 재검토 사유 |
| 구현 중 호환성 문제 | 병행 운영·Adapter·Migration 또는 Rollback 선택 | 영향분석, 전환 계획 |
| 배포·검증 실패 | Gate에서 차단하고 원인 조치 후 같은 조건으로 재검증 | 실패·조치·재시험 결과 |
| 긴급 운영 예외 | 최소 범위·최단 기간으로 승인하고 보완통제 적용 | 예외승인서, 만료 알림 |
| 결정 폐기 | 대체 ADR과 제거 버전을 연결하고 잔여 사용처 검사 | 폐기 기록, Drift 결과 |

## 15. 산출물 작성 예시

### 15.1 TASK 등록 카드

```yaml
taskId: {{ID}}
title: "{{TITLE}}"
status: 분석-중
priority: {{PRIORITY}}
owner: {{OWNER}}
consulted: "{{CONSULTED}}"
approver: "{{APPROVER}}"
decisionScope: "{{SCOPE}}"
dueDate: "승인 회의에서 확정"
deliverable: "{{DELIVERABLE}}"
```

### 15.2 ADR 핵심 항목

```yaml
adrId: ADR-{{PREFIX}}-0001
taskId: {{ID}}
status: proposed
decision: "{{RECOMMENDATION}}"
implementation: "{{EXECUTION}}"
alternatives:
  - 중앙 표준·공통 강제
  - 영역 자율·경계 계약
  - 승인된 제한 예외
consequences:
  positive: 일관성·추적성·자동검증 강화
  negative: 초기 전환·공통화 비용
exceptionExpiryRequired: true
supersedes: []
```

### 15.3 구현 추적표

| 구분 | 기록할 값 |
|---|---|
| 요구사항·TASK·ADR | 요구사항 ID, {{ID}}, ADR ID |
| 설계·가이드 | 문서 경로와 승인 버전 |
| 코드·설정 | 모듈, 파일, 설정 Key, Migration |
| 운영 기준정보 | Service Catalog, Timeout, Route, 권한 등 해당 항목 |
| 검증 | 테스트 클래스·명령·결과·CI 실행 URL 또는 증적 ID |
| 전환 | 적용일, 병행기간, Rollback, 제거 버전 |

## 16. 검증 전략과 합격 기준

### 문서 검증

- 제목·범위·권고안·이행안·RACI·산출물이 원본 관리대장과 일치해야 한다.
- 선택안뿐 아니라 기각안, 영향, 예외와 폐기조건이 있어야 한다.
- 상대 링크와 파일 경로가 실제 저장소에서 열려야 한다.

### 구현 검증

- {{VERIFY}}
- 가장 작은 관련 테스트부터 실행하고 영향도에 따라 모듈 빌드·통합시험으로 확대한다.
- 구현되지 않은 권고는 `계획` 또는 `미구현`으로 표시하고 현재 동작처럼 표현하지 않는다.

### 합격 기준

```text
ADR 승인
+ {{DELIVERABLE}} Baseline
+ 코드·설정·OM 반영
+ 정상·경계·오류·회귀 테스트
+ 자동검증 또는 승인된 수동 Gate
+ 업무팀·운영 적용 확인
= {{ID}} 완료
```

## 17. 검토 체크리스트

### 이해와 범위

- [ ] 처음 읽는 사람이 `{{SCOPE}}`의 의미와 결정 이유를 설명할 수 있다.
- [ ] 적용 대상과 비적용 대상이 구분되어 있다.
- [ ] 현행, 목표와 전환 필요사항이 섞이지 않았다.

### 의사결정 품질

- [ ] 최소 두 개 이상의 실질적인 대안을 비교했다.
- [ ] 선택·기각 근거가 보안·성능·운영·비용 관점을 포함한다.
- [ ] 실패·Timeout·Rollback·복구 흐름을 검토했다.
- [ ] 호환성·전환·폐기와 예외 만료 조건이 있다.

### 실행 가능성

- [ ] 주관 {{OWNER}}, 협의 {{CONSULTED}}, 승인 {{APPROVER}}의 행동이 구분되어 있다.
- [ ] `{{DELIVERABLE}}`의 작성자·검토자·기한이 있다.
- [ ] 코드·설정·테스트·OM·운영 반영 위치가 식별되었다.
- [ ] 자동화할 규칙과 수동 심의할 판단을 구분했다.

### 완료 증적

- [ ] 관련 테스트와 Gate가 통과했다.
- [ ] 업무팀 적용 결과와 미적용 예외가 확인되었다.
- [ ] 운영 모니터링·Runbook·감사 영향이 반영되었다.
- [ ] 새 결정이 기존 결정을 대체한다면 양방향 링크가 있다.

## 18. 주요 위험과 대응

| 위험 | 조기 신호 | 대응 |
|---|---|---|
| 결정 지연 | 임시 구현과 팀별 질문 증가 | P0 우선순위와 결정기한 지정, 선도 구현 확대 제한 |
| 문서–구현 Drift | 같은 항목의 이름·값이 문서와 코드에서 다름 | 자동 대조와 Release Gate 적용 |
| 과도한 중앙화 | 작은 변경도 공통팀 대기 | 경계 계약은 고정하고 내부 구현 자율범위 명시 |
| 예외의 영구화 | 만료일 경과·동일 예외 반복 | 만료 알림, 재승인, 제거 책임자와 버전 지정 |
| 검증 부족 | 정상 시나리오만 통과 | 오류·Timeout·우회·Rollback·장애 시험 추가 |
| 책임 불명확 | 조치 기한과 검증자 부재 | 단일 주관과 최종 승인자, 조건부 조치 담당 지정 |

## 19. 변경·호환성·폐기 관리

공개 계약, 표준 전문, 설정 Key, DB Schema 또는 운영 기준정보가 바뀌면 영향 사용처를 검색하고 병행 운영기간을 둔다. 변경은 다음 순서로 관리한다.

```text
영향 사용처 식별
→ 호환 전략과 Migration 작성
→ 구·신 기준 병행 검증
→ 배포와 Rollback 리허설
→ 적용률·예외 확인
→ 제거 버전 승인
→ 구 기준과 미사용 설정 폐기
```

폐기된 문서를 삭제해서 이력을 없애지 않는다. `폐기·대체` 상태와 대체 ADR을 표시하고, 실제 코드·설정·OM에서 잔여 사용처가 없는지 확인한다.

## 20. 결론

{{ID}}의 목적은 **{{TITLE}}**에 관한 결정을 문서 한 장으로 끝내는 것이 아니라, `{{SCOPE}}` 기준을 프로젝트 전체가 같은 방식으로 이해하고 실행·검증하게 만드는 것이다.

최종 승인 시에는 **{{RECOMMENDATION}}**이라는 방향을 구체적인 값과 경계로 확정하고, **{{EXECUTION}}**을 코드·설정·테스트·운영 증적으로 입증해야 한다. 그때 비로소 {{ID}}를 완료로 판정할 수 있다.

## 21. 참조 문서

{{REFERENCES}}

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
'@

$tasks = Parse-TaskRows $listLines
$details = Parse-TaskRows $detailLines

foreach ($prefix in $prefixFolders.Keys) {
    $folder = Join-Path $DecisionRoot $prefixFolders[$prefix]
    $guide = $areaGuide[$prefix]
    $indexRows = New-Object System.Collections.Generic.List[string]
    $areaTasks = $tasks.GetEnumerator() | Where-Object { $_.Key -like "$prefix-*" } | Sort-Object Name

    foreach ($entry in $areaTasks) {
        $id = $entry.Key
        $cells = $entry.Value
        $title = $cells[1]
        $scope = $cells[2]
        $owner = $cells[3]
        $consulted = $cells[4]
        $approver = $cells[5]
        $priority = $cells[6]
        $deliverable = $cells[7]
        $detail = $details[$id]
        $recommendation = if ($detail -and $detail.Count -gt 1) { $detail[1] } else { "$scope 기준을 프로젝트 표준으로 확정한다." }
        $execution = if ($detail -and $detail.Count -gt 2) { $detail[2] } else { '현행 조사, 대안 비교, 승인, 구현과 검증 순서로 이행한다.' }

        $base = Get-ChildItem -File $folder -Filter "$id-*.md" | Where-Object { $_.BaseName -notlike '*-detail' } | Select-Object -First 1
        if (-not $base) { throw "Base document not found for $id" }
        $detailName = "$($base.BaseName)-detail.md"
        $references = ($referenceMap[$prefix] | ForEach-Object { "- $(Relative-Link $_)" }) -join "`n"
        $values = [ordered]@{
            '{{ID}}'=$id; '{{TITLE}}'=$title; '{{AREA}}'=$guide.Name; '{{PRIORITY}}'=$priority
            '{{BASEFILE}}'=$base.Name; '{{BASELINK}}=BROKEN'=''; '{{BASELINK}}'=($base.Name -replace ' ','%20')
            '{{PLAIN}}'=$guide.Plain; '{{SCOPE}}'=$scope; '{{RECOMMENDATION}}'=$recommendation; '{{EXECUTION}}'=$execution
            '{{OWNER}}'=$owner; '{{CONSULTED}}'=$consulted; '{{APPROVER}}'=$approver; '{{DELIVERABLE}}'=$deliverable
            '{{BOUNDARY}}'=$guide.Boundary; '{{COMPONENTS}}'=$guide.Components; '{{FAILURE}}'=$guide.Failure
            '{{VERIFY}}'=$guide.Verify; '{{EXAMPLE}}'=$guide.Example; '{{PREFIX}}'=$prefix; '{{REFERENCES}}'=$references
        }
        $content = $template
        foreach ($key in $values.Keys) { $content = $content.Replace($key, [string]$values[$key]) }
        Write-Utf8 (Join-Path $folder $detailName) $content
        $indexRows.Add("| [$id](./$($detailName -replace ' ','%20')) | $title | $priority | $owner |")
    }

    $detailIndex = @"
# $($guide.Name) TASK 상세 설명서

이 문서는 $prefix 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
$($indexRows -join "`n")

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
"@
    Write-Utf8 (Join-Path $folder 'DETAIL-README.md') $detailIndex
}

Write-Output "Generated $($tasks.Count) detailed task documents and $($prefixFolders.Count) detail indexes."
