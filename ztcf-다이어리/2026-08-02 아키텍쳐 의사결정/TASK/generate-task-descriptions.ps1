param(
    [string]$DecisionRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$listPath = Join-Path $DecisionRoot '농협 상호금융 NSIGHT 아키텍처 의사결정 사항 목록.md'
$detailPath = Join-Path $DecisionRoot '2026-08-02-아키테처-의사결정-TASK-상세.md'
$listLines = Get-Content -Encoding UTF8 $listPath
$detailLines = Get-Content -Encoding UTF8 $detailPath

$prefixFolders = [ordered]@{
    GOV  = '01_거버넌스_아키텍처_관리'
    APP  = '02_업무_도메인_애플리케이션_아키텍처'
    STD  = '03_명명_식별자_추적성'
    UI   = '04_단말_UI_아키텍처'
    SEC  = '05_개인정보_보안_암호화'
    AUTH = '06_인증_인가_JWT_세션'
    MCA  = '07_MCA_채널_전문_아키텍처'
    INT  = '08_EAI_외부연계_업무_간_연계'
    DATA = '09_데이터_DB_SQL_아키텍처'
    REL  = '10_오류_Timeout_트랜잭션_안정성'
    INF  = '11_기술_인프라_용량_아키텍처'
    BFC  = '12_배치_파일_캐시_아키텍처'
    OPS  = '13_운영_로그_감사_모니터링'
    QLT  = '14_DevOps_CI_CD_품질검증'
}

$areaNames = [ordered]@{
    GOV='거버넌스·아키텍처 관리'; APP='업무·도메인·애플리케이션'; STD='명명·식별자·추적성'
    UI='단말·UI'; SEC='개인정보·보안·암호화'; AUTH='인증·인가·JWT·세션'; MCA='MCA·채널·전문'
    INT='EAI·외부연계·업무 간 연계'; DATA='데이터·DB·SQL'; REL='오류·Timeout·트랜잭션·안정성'
    INF='기술·인프라·용량'; BFC='배치·파일·캐시'; OPS='운영·로그·감사·모니터링'; QLT='DevOps·CI/CD·품질검증'
}

$referenceMap = @{
    GOV=@('AGENTS.md','xdoc/agents/development-agent-guide.md','znsight-man/NSIGHT TCF 아키텍처 구축 방법론 - 3. Architecture Gate 단계별 체크리스트.md','ztcf-book-capacity-md/부록/AC-아키텍처-의사결정-기록.md')
    APP=@('zarchitecture/02-TCF-프레임워크-아키텍처.md','zarchitecture/03-애플리케이션-6계층-아키텍처.md','zarchitecture/04-업무-도메인-서비스-아키텍처.md','zdocs-1/architecture/01-application-layer.md','zdocs-1/architecture/29-facade.md')
    STD=@('zdocs-1/architecture/53-naming-conventions.md','zdocs-1/architecture/06-naming.md','zdocs-1/SOURCE_INDEX.md','zarchitecture/16-모듈-포트-의존성-레퍼런스.md')
    UI=@('zarchitecture/13-UI-채널-아키텍처.md','tcf-ui/README.md','tcf-uj/README.md','zdocs-1/architecture/18-fileupdownload.md')
    SEC=@('zdocs-1/architecture/43-security-operations.md','zarchitecture/07-세션-인증-보안-아키텍처.md','xdoc/agents/security-agent.md')
    AUTH=@('zdocs-1/architecture/42-jwt.md','zdocs-1/architecture/51-api-gateway.md','zarchitecture/06-API-Gateway-아키텍처.md','tcf-jwt/README.md','tcf-gateway/README.md')
    MCA=@('zdocs-1/architecture/02-junmun.md','zarchitecture/13-UI-채널-아키텍처.md','zdocs-1/architecture/46-service-integration-contract.md')
    INT=@('zarchitecture/08-서비스-간-연동-아키텍처.md','zdocs-1/architecture/46-service-integration-contract.md','tcf-eai/build.gradle','zarchitecture/14-이벤트-연계-아키텍처.md')
    DATA=@('zarchitecture/09-데이터-DB-아키텍처.md','zdocs-1/architecture/07-DAO.md','zdocs-1/architecture/26-mybatis.md','zdocs-1/architecture/27-paging.md','zdocs-1/architecture/47-data-governance.md')
    REL=@('zarchitecture/10-거래통제-Timeout-로깅-아키텍처.md','zdocs-1/architecture/03-transaction.md','zdocs-1/architecture/05-exception.md','zdocs-1/architecture/08-timeout.md','zdocs-1/architecture/41-service-timeout-policy.md')
    INF=@('zarchitecture/01-전체-시스템-아키텍처.md','zarchitecture/15-배포-환경-CICD-아키텍처.md','zarchitecture/16-모듈-포트-의존성-레퍼런스.md','ztomcat/README.md','zdocs-1/architecture/45-disaster-recovery.md')
    BFC=@('zarchitecture/11-캐시-아키텍처.md','zarchitecture/12-배치-모니터링-아키텍처.md','zdocs-1/architecture/12-cache.md','zdocs-1/architecture/13-batch.md','zdocs-1/architecture/18-fileupdownload.md')
    OPS=@('zdocs-1/architecture/37-transaction-log.md','zdocs-1/architecture/44-observability.md','zdocs-1/architecture/52-om-operations.md','zarchitecture/05-운영관리-OM-아키텍처.md','tcf-om/README.md')
    QLT=@('build.gradle','zdocs-1/architecture/50-test-architecture.md','zdocs-1/architecture/49-release-strategy.md','tcf-cicd/README.md','xdoc/agents/quality-agent.md')
}

function Parse-TaskRows([string[]]$lines) {
    $result = @{}
    foreach ($line in $lines) {
        if ($line -match '^\|\s*([A-Z]+-\d+)\s*\|(.+)$') {
            $cells = $line.Trim('|').Split('|') | ForEach-Object { $_.Trim() }
            if ($cells.Count -ge 2) { $result[$cells[0]] = $cells }
        }
    }
    return $result
}

function Safe-Name([string]$value) {
    $name = $value -replace '[\\/:*?"<>|`]', '-' -replace '\s+', '-'
    return $name.Trim('-', '.', ' ')
}

function Link-For([string]$path) {
    $encoded = ($path -replace '\\','/') -replace ' ','%20'
    return "[$path](../../../$encoded)"
}

function Write-Utf8([string]$path, [string]$content) {
    $content = $content.Replace(('`' + [string][char]9 + 'ext'), '```text')
    $content = [regex]::Replace($content, '(?m)^`$', '```')
    $content = $content.Replace('- 기본 의존 방향은 `tcf-util → tcf-core → tcf-web → 업무/플랫폼 모듈이다.', '- 기본 의존 방향은 `tcf-util → tcf-core → tcf-web → 업무/플랫폼 모듈`이다.')
    $parent = Split-Path -Parent $path
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent | Out-Null }
    Set-Content -LiteralPath $path -Value $content -Encoding UTF8
}

$tasks = Parse-TaskRows $listLines
$details = Parse-TaskRows $detailLines

foreach ($prefix in $prefixFolders.Keys) {
    $folder = Join-Path $DecisionRoot $prefixFolders[$prefix]
    $areaTasks = $tasks.GetEnumerator() | Where-Object { $_.Key -like "$prefix-*" } | Sort-Object Name
    $indexRows = New-Object System.Collections.Generic.List[string]

    foreach ($entry in $areaTasks) {
        $id = $entry.Key
        $cells = $entry.Value
        $title = $cells[1]
        $decisionScope = if ($cells.Count -gt 2) { $cells[2] } else { $title }
        $owner = if ($cells.Count -gt 3) { $cells[3] } else { '-' }
        $consulted = if ($cells.Count -gt 4) { $cells[4] } else { '-' }
        $approver = if ($cells.Count -gt 5) { $cells[5] } else { '-' }
        $priority = if ($cells.Count -gt 6) { $cells[6] } else { '-' }
        $deliverable = if ($cells.Count -gt 7) { $cells[7] } else { '-' }
        $detail = $details[$id]
        $recommendation = if ($null -ne $detail -and $detail.Count -gt 1) { $detail[1] } else { "$decisionScope에 대한 프로젝트 표준을 확정한다." }
        $execution = if ($null -ne $detail -and $detail.Count -gt 2) { $detail[2] } else { '현행 분석, 대안 비교, 승인, 표준 반영과 자동검증 순서로 이행한다.' }
        $detailResponsibility = if ($null -ne $detail -and $detail.Count -gt 3) { $detail[3] } else { "$owner 주관, $approver 승인" }
        $detailOutput = if ($null -ne $detail -and $detail.Count -gt 4) { $detail[4] } else { $deliverable }
        $fileName = "$id-$(Safe-Name $title).md"
        $indexRows.Add("| [$id](./$($fileName -replace ' ','%20')) | $title | $priority | $owner | $approver |")
        $refs = ($referenceMap[$prefix] | ForEach-Object { "- $(Link-For $_)" }) -join "`n"

        $content = @"
# $id $title

> 상태: **결정 전 설명서 초안**  
> 영역: $($areaNames[$prefix]) · 우선순위: **$priority** · 주관: **$owner** · 승인: **$approver**

## 1. 도입 전 안내말

이 문서는 `$id` 아키텍처 의사결정을 검토·승인·이행하기 위한 설명서다. 현재 저장소의 구현과 참조 문서를 근거로 작성했으며, ARB 승인 전까지는 권고 초안으로 취급한다.

## 2. 문서 개요

| 항목 | 내용 |
|---|---|
| 목적 | $title 기준을 프로젝트 단일 원칙으로 확정한다. |
| 결정 범위 | $decisionScope |
| 적용 대상 | NSIGHT TCF 프레임워크, 플랫폼 모듈, 업무 WAR와 관련 운영 절차 |
| 대상 독자 | $owner, $consulted, $approver 및 구현·시험·운영 담당자 |
| 선행조건 | 현행 코드·설정·운영 기준 확인, 영향 대상 식별, 책임자 지정 |
| 주요 산출물 | $deliverable |

## 3. 문제 정의 및 설계 배경

이 기준이 확정되지 않으면 업무·모듈별 구현이 달라져 호환성, 보안, 운영 추적성과 변경 영향 분석이 약화될 수 있다. 결정은 문서 승인에 그치지 않고 코드·설정·OM 기준정보·테스트·운영 증적까지 이어져야 한다.

## 4. 현행 구조와 확인사항

- Java 21, Spring Boot 3.3.5, Gradle 멀티모듈 구성을 기준으로 한다.
- 기본 의존 방향은 `tcf-util → tcf-core → tcf-web → 업무/플랫폼 모듈`이다.
- 온라인 업무는 공통 TCF 처리와 `serviceId` 기반 Handler 디스패치를 기본으로 한다.
- 아래 참조 문서와 실제 소스·설정·테스트 사이의 차이는 결정 전 반드시 기록한다.

## 5. 요구사항과 제약조건

- 요구사항: $decisionScope
- 기존 공개 계약, 표준 전문, 설정 키, DB Schema의 호환성을 보존하거나 전환·롤백 계획을 명시한다.
- 비밀번호, Token, Private Key, 세션 ID와 개인정보를 소스·설정·로그에 노출하지 않는다.
- 공통 기능을 업무 모듈에 중복 구현하거나 기반 모듈이 업무 모듈을 참조하게 만들지 않는다.

## 6. 설계 원칙

1. 단일 주관과 승인자를 둔다.
2. 실행 위치와 책임 경계를 명확히 한다.
3. 정상·오류·Timeout·복구 흐름을 함께 정의한다.
4. 문서 규칙을 샘플, 공통 컴포넌트와 자동검증으로 전환한다.
5. 예외에는 사유, 보완통제, 책임자와 만료일을 둔다.

## 7. 대안 비교 및 의사결정

| 대안 | 설명 | 장점 | 위험 |
|---|---|---|---|
| A. 중앙 표준·공통 통제 | 프로젝트 공통 계약과 검증 수단으로 관리 | 일관성, 추적성, 운영 통제 강화 | 초기 합의와 공통화 비용 |
| B. 영역 자율 + 계약 통제 | 영역 구현은 자율화하되 경계 계약만 강제 | 유연성과 팀 자율성 | 중복 구현과 편차 관리 필요 |
| C. 업무별 개별 구현 | 각 업무가 독립 결정 | 단기 착수 속도 | 장기 호환성·보안·운영 위험 큼 |

**권고 초안:** $recommendation

**이행 방향:** $execution

최종 선택안과 기각 근거는 ARB 검토 후 ADR에 기록한다.

## 8. 목표 아키텍처

```text
요구사항·현행 근거
  → TASK 대안·영향 분석
  → ARB 결정 / ADR 기준선
  → 개발표준·공통 구현·샘플
  → CI/CD 및 테스트 검증
  → 배포·운영 증적·정기 재검토
```

## 9. 표준 형식

- ADR ID: `ADR-$prefix-{4자리}`
- 상태: 제안 → 검토 → 승인 → 표준 반영 → 적용 완료 → 폐기·대체
- 필수 기록: 결정기한, 대안, 평가기준, 선택 근거, 적용범위, 예외·폐기조건
- 추적 키: 관련 요구사항, 화면, ServiceId, 프로그램, SQL, 설정, 테스트, OM 항목

## 10. 구성요소 및 속성

| 구성요소 | 필수 속성 |
|---|---|
| 결정 원장 | TASK ID, 제목, 상태, 우선순위, 책임자, 기한 |
| ADR | 대안, 결정, 근거, 영향, 예외·폐기조건 |
| 구현 증적 | 소스·설정 경로, 버전, 변경 이력 |
| 검증 증적 | 테스트 결과, 자동검사, 운영 확인 |

## 11. 책임 경계와 RACI

| 역할 | 담당 |
|---|---|
| 주관(Responsible) | $owner |
| 승인(Accountable) | $approver |
| 협의(Consulted) | $consulted |
| 구현·검증 | $detailResponsibility |
| 공유(Informed) | 영향받는 업무팀·운영팀·QA |

## 12. 정상 처리 흐름

1. `$id`를 결정 원장에 등록하고 영향 범위를 식별한다.
2. 현행 코드·설정·문서와 요구사항의 Gap을 기록한다.
3. 두 개 이상의 대안을 보안·성능·개발·운영·비용 기준으로 평가한다.
4. 필요 시 대표 거래나 설정으로 PoC를 수행한다.
5. ARB 승인 후 ADR, 표준, 샘플과 검증 규칙을 기준선으로 등록한다.
6. 업무팀 적용과 운영 증적을 확인한 뒤 완료 처리한다.

## 13. 오류·Timeout·장애 흐름

- 결정 충돌: 기존 ADR과 우선순위·대체관계를 확인하고 승인 전 구현을 보류한다.
- 적용 실패: 영향 범위를 격리하고 승인된 롤백 절차로 복구한다.
- 검증 실패: 배포 Gate에서 차단하고 원인·조치·재검증 결과를 남긴다.
- 운영 예외: 임시 우회가 필요하면 만료일 있는 예외승인을 등록한다.

## 14. 정상 예시

`$id` 결정, 관련 코드·설정, `$detailOutput`, 자동검증 결과와 운영 확인 내역이 동일 변경 단위로 추적된다.

## 15. 금지 예시

- 승인 전 임시 구현을 사실상 표준으로 확산하는 행위
- 문서만 승인하고 공통 구현·샘플·테스트를 제공하지 않는 행위
- 보안·운영 영향을 누락하거나 영구 예외를 허용하는 행위
- 저장소 실제 구현과 다른 내용을 현재 기준으로 단정하는 행위

## 16. 연계 규칙

관련 화면·채널·Gateway·TCF·업무 계층·EAI·DB·OM·CI/CD 가운데 영향을 받는 경계를 식별하고, 각 경계의 입력·출력 계약과 오류 책임을 ADR에 연결한다.

## 17. 데이터 및 상태관리

결정 상태, 버전, 승인일, 대체 ADR, 예외 만료일과 적용 현황을 이력으로 보존한다. 개인정보나 Secret이 증적에 포함되면 비식별·마스킹 후 저장한다.

## 18. 성능·용량·확장성

해당 결정이 응답시간, TPS, Thread, Connection Pool, Heap, 전문 크기 또는 저장량에 영향을 주는지 평가한다. 영향이 있으면 기준값·측정환경·합격조건을 산출물에 포함한다.

## 19. 보안·개인정보·감사

인증·권한 우회, 개인정보 원문 노출, Secret 저장, 로그 노출과 감사 누락 가능성을 검토한다. 중요 결정과 예외의 승인·변경·폐기 이력을 감사 가능하게 보존한다.

## 20. 운영·모니터링·장애 대응

운영 지표, 경보 조건, 담당자, 확인 절차, 복구·Rollback과 에스컬레이션을 정의한다. OM 기준정보와 Runbook 변경이 필요하면 같은 완료 범위에 포함한다.

## 21. 자동검증 및 품질 Gate

- 문서·소스·설정·OM 값의 정합성을 검사한다.
- 적용 가능한 경우 ArchUnit, Checkstyle, Gradle Task, 계약·통합·보안 테스트로 규칙을 강제한다.
- 필수 증적이 없거나 Critical 결함이 남으면 승인·배포를 차단한다.

## 22. 테스트 시나리오

| 구분 | 시나리오 | 기대 결과 |
|---|---|---|
| 정상 | 승인된 기준으로 대표 거래·기능 수행 | 표준 계약과 산출물이 일치 |
| 경계 | 최소·최대값, 미등록·중복·만료 조건 | 명시된 정책으로 처리 |
| 오류 | 잘못된 입력·설정·권한·의존 시스템 실패 | 안전한 오류 응답과 Rollback·격리 |
| 회귀 | 기존 공개 계약과 대표 업무 재실행 | 비의도 변경 없음 |
| 운영 | 로그·메트릭·알림·Runbook 확인 | 원인과 조치 경로 추적 가능 |

## 23. 체크리스트

- [ ] 주관·협의·승인과 결정기한이 지정되었다.
- [ ] 현행 코드·설정·테스트 근거를 확인했다.
- [ ] 대안 2개 이상과 선택·기각 근거가 있다.
- [ ] 보안·성능·운영·호환성 영향을 검토했다.
- [ ] `$deliverable`이 작성되고 승인되었다.
- [ ] 샘플·공통 구현과 자동검증 적용 여부를 결정했다.
- [ ] 테스트와 운영 증적을 확인했다.
- [ ] 예외·전환·Rollback·폐기조건을 기록했다.

## 24. 변경·호환성·폐기 관리

기존 계약 변경 시 병행 운영기간, 마이그레이션 순서, 데이터·설정 전환, Rollback과 제거 버전을 명시한다. 이 결정을 대체하는 ADR이 승인되면 본 문서를 `폐기·대체` 상태로 바꾸고 양방향 링크를 남긴다.

## 25. 시사점

### 핵심 아키텍처 판단

$recommendation

### 주요 위험

결정 지연, 업무별 편차, 문서와 구현의 불일치, 자동검증 부재가 핵심 위험이다.

### 우선 보완 과제

$execution

### 중장기 발전 방향

TASK를 요구사항–ADR–소스–설정–테스트–OM–운영 증적과 연결해 지속적으로 검증되는 실행 아키텍처로 발전시킨다.

## 26. 마무리말

`$id`는 문서 승인만으로 완료되지 않는다. `$deliverable`, 구현·설정 반영, 테스트, 자동검증과 업무팀 적용 확인까지 충족해야 완료로 판정한다.

## 참조 문서

$refs
- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
"@
        $content = $content.Replace('$id', $id).Replace('$detailOutput', $detailOutput).Replace('$deliverable', $deliverable)
        $content = $content.Replace(([string][char]9 + 'cf-util'), '`tcf-util').Replace(('`' + [string][char]9 + 'ext'), '```text')
        Write-Utf8 (Join-Path $folder $fileName) $content
    }

    $readme = @"
# $($areaNames[$prefix]) 아키텍처 의사결정

이 디렉터리는 `$prefix` 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
$($indexRows -join "`n")

## 공통 완료 기준

```text
ADR 승인
+ 개발표준 반영
+ 공통 샘플 또는 모듈 제공
+ 테스트 기준 및 결과
+ CI/CD 자동검증
+ 업무팀·운영 적용 확인
= TASK 완료
```

## 기준 문서

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
"@
    $readme = $readme.Replace('$prefix', $prefix).Replace(('`' + [string][char]9 + 'ext'), '```text')
    Write-Utf8 (Join-Path $folder 'README.md') $readme
}

function Parse-NumberedTable([string[]]$lines, [string]$headingPattern) {
    $start = ($lines | Select-String -Pattern $headingPattern | Select-Object -First 1).LineNumber
    $items = @()
    if (-not $start) { return $items }
    foreach ($line in $lines[($start)..($lines.Count - 1)]) {
        if ($line -match '^# ' -and $items.Count -gt 0) { break }
        if ($line -match '^\|\s*(\d+)\s*\|') {
            $cells = $line.Trim('|').Split('|') | ForEach-Object { $_.Trim() }
            if ($cells[0] -match '^\d+$') { $items += ,$cells }
        }
    }
    return $items
}

function Write-MetaSet([string]$folderName, [string]$code, [string]$title, [object[]]$items, [string]$purpose) {
    $folder = Join-Path $DecisionRoot $folderName
    $rows = New-Object System.Collections.Generic.List[string]
    foreach ($cells in $items) {
        $number = [int]$cells[0]
        $itemTitle = $cells[1]
        $owner = if ($cells.Count -gt 2) { $cells[2] } else { '-' }
        $id = '{0}-{1:D2}' -f $code,$number
        $fileName = "$id-$(Safe-Name $itemTitle).md"
        $rows.Add("| [$id](./$($fileName -replace ' ','%20')) | $itemTitle | $owner |")
        $body = @"
# $id $itemTitle

> 분류: **$title** · 순서: **$number** · 주관/참여: **$owner**

## 목적

$purpose

## 설명

`$itemTitle`은 전체 TASK 중 선행 합의와 집중 검토가 필요한 관리 단위다. 관련 개별 TASK 문서에서 현행, 대안, 결정, 이행과 검증 증적을 확인하고 하나의 승인 결과로 연결한다.

## 수행 항목

1. 관련 TASK ID와 책임자를 식별한다.
2. 저장소의 코드·설정·테스트·운영 문서를 기준으로 현행을 확인한다.
3. 보안·성능·운영·호환성 영향을 포함해 대안을 비교한다.
4. ADR 승인 후 개발표준, 샘플, 공통 구현과 자동검증에 반영한다.
5. 적용 현황과 예외 만료일을 추적한다.

## 완료 증적

- [ ] 관련 TASK와 ADR이 연결되었다.
- [ ] 주관·협의·승인과 기한이 지정되었다.
- [ ] 선택안과 기각안의 근거가 기록되었다.
- [ ] 구현·설정·테스트·운영 반영 범위가 확정되었다.
- [ ] 자동검증 또는 수동 Gate와 예외 절차가 정의되었다.

## 참조

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
"@
        Write-Utf8 (Join-Path $folder $fileName) $body
    }
    $readme = @"
# $title

$purpose

| ID | 항목 | 주관/참여 영역 |
|---|---|---|
$($rows -join "`n")

## 공통 원칙

- 개별 문서는 요약·진입점이며 최종 결정은 관련 TASK와 ADR에서 관리한다.
- 문서 승인, 구현, 테스트, 자동검증과 운영 증적이 모두 연결되어야 완료다.
"@
    $readme = $readme.Replace('$prefix', $prefix).Replace(('`' + [string][char]9 + 'ext'), '```text')
    Write-Utf8 (Join-Path $folder 'README.md') $readme
}

$p0Items = Parse-NumberedTable $listLines '^# 16\. 우선 확정 대상'
$planItems = Parse-NumberedTable $listLines '^# 17\. 권장 방안서 작성 순서'
Write-MetaSet '15_우선_확정_대상' 'P0' 'P0 우선 확정 대상' $p0Items '개발 착수 전에 미확정 위험이 큰 핵심 의사결정을 우선 확정하기 위한 목록이다.'
Write-MetaSet '16_권장_방안서_작성_순서' 'PLAN' '권장 방안서 작성 순서' $planItems '상호 의존성과 변경 파급도를 고려해 상세 방안서를 작성·승인하는 권장 순서를 정의한다.'

$management = @"
# 아키텍처 의사결정 관리 원칙

## 목적

NSIGHT 아키텍처 의사결정을 문서 승인에서 끝내지 않고 설계·코드·설정·테스트·OM·운영 증적까지 일관되게 관리한다.

## 상태 흐름

```text
미착수 → 분석 중 → 대안 검토 → PoC 진행 → 승인 대기 → 승인
→ 표준 반영 → 공통모듈 반영 → 자동검증 반영 → 적용 완료 → 폐기·대체
```

## 상태 전이 기준

| 상태 | 진입 조건 | 필수 증적 |
|---|---|---|
| 분석 중 | 주관·협의·승인과 기한 지정 | TASK 등록부 |
| 대안 검토 | 현행·문제·제약 확인 | 대안 비교표 |
| PoC 진행 | 검증 질문·범위·합격조건 승인 | PoC 계획·결과 |
| 승인 대기 | 선택안·영향·전환·예외조건 완성 | ADR 초안 |
| 승인 | ARB 의결 | 승인 회의록·ADR |
| 표준 반영 | 개발·설계 기준 갱신 | 가이드·샘플 |
| 자동검증 반영 | 반복 가능한 검사 제공 | CI/CD·테스트 결과 |
| 적용 완료 | 업무팀 및 운영 적용 확인 | 적용·운영 증적 |
| 폐기·대체 | 대체 ADR 승인 | 양방향 대체 링크 |

## 완료 기준

- ADR 승인
- 개발표준 반영
- 공통 샘플 또는 모듈 제공
- 테스트 기준과 결과 제공
- CI/CD 자동검증 반영
- 업무팀·운영 적용 확인

## 예외와 변경관리

예외는 사유, 위험, 보완통제, 책임자와 만료일을 포함해야 한다. 공개 계약·표준 전문·설정 키·DB Schema 변경은 호환기간, 전환 순서, Rollback과 폐기 버전을 기록한다.

## 참조

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
"@
$managementFolder = Join-Path $DecisionRoot '17_관리_원칙'
Write-Utf8 (Join-Path $managementFolder 'README.md') $management
Write-Utf8 (Join-Path $managementFolder '의사결정-상태-전이-및-완료기준.md') $management

Write-Output "Generated $($tasks.Count) task files, $($p0Items.Count) P0 files, and $($planItems.Count) plan files."
