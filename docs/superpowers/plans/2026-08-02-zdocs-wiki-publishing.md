# ZDocs GitHub Wiki Publishing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `zdocs-1`과 `zdocs-2`의 Markdown 92개를 추적 가능하고 링크가 정상인 독립 GitHub Wiki 문서군으로 게시한다.

**Architecture:** 원본 저장소는 읽기 전용 입력으로 사용하고, 임시 Wiki clone에서 결정적 변환 스크립트가 페이지명 매핑·출처 블록·링크 변환·색인을 생성한다. 별도 검증 스크립트가 페이지 수, 이름 충돌, 링크, 코드 펜스, 보안 패턴과 금지 확장자를 확인한 뒤 단일 Wiki 커밋을 원격에 게시한다.

**Tech Stack:** PowerShell 7/Windows PowerShell, Git, GitHub Wiki Markdown, 정규식 기반 링크 분석

## Global Constraints

- Source Baseline은 게시 시작 시점의 원본 저장소 `HEAD` 전체 SHA로 고정한다.
- `zdocs-1` Markdown 65개와 `zdocs-2` Markdown 27개만 상세 Wiki 페이지로 게시한다.
- DOCX 22개와 JSON 1개를 Wiki 저장소에 추가하지 않는다.
- 기존 사용자 변경과 기존 Wiki 페이지를 덮어쓰거나 삭제하지 않는다.
- `zdocs-1`은 `ZDOC1-`, `zdocs-2`는 `ZDOC2-` 접두어를 사용한다.
- 통합 및 문서군 색인은 `ZDocs-Index`, `ZDocs-1-Index`, `ZDocs-2-Index`로 생성한다.
- 원격 변경은 강제 푸시하지 않고 rebase로 통합한다.

---

### Task 1: 게시 입력과 페이지명 매핑 고정

**Files:**
- Create: `C:/tmp/build-zdocs-wiki.ps1`
- Read: `zdocs-1/**/*.md`
- Read: `zdocs-2/**/*.md`
- Read: `C:/tmp/nsight-tcf-wiki-sample/*.md`

**Interfaces:**
- Consumes: 원본 저장소 경로, Wiki clone 경로, Source Baseline SHA
- Produces: 원본 상대경로를 유일한 Wiki 페이지명으로 대응시키는 `$pageMap`

- [ ] **Step 1: 원본과 Wiki 상태 확인**

Run:

```powershell
git status --short
git rev-parse HEAD
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample status --short
```

Expected: 원본 기존 변경은 기록만 하고, Wiki 작업 트리는 clean이다.

- [ ] **Step 2: 정확한 입력 개수 검증을 스크립트에 작성**

```powershell
$zdocs1 = @(Get-ChildItem $sourceRoot/zdocs-1 -Recurse -Filter '*.md')
$zdocs2 = @(Get-ChildItem $sourceRoot/zdocs-2 -Recurse -Filter '*.md')
if ($zdocs1.Count -ne 65 -or $zdocs2.Count -ne 27) {
    throw "Unexpected source count: zdocs-1=$($zdocs1.Count), zdocs-2=$($zdocs2.Count)"
}
```

- [ ] **Step 3: 경로 기반 페이지명 함수를 작성**

```powershell
function Get-WikiPageName([string]$group, [string]$relativePath) {
    $stem = [IO.Path]::ChangeExtension($relativePath, $null) -replace '[\\/]+', '-'
    if ($stem -eq 'README') { return "ZDocs-$group-Index" }
    return "ZDOC$group-$stem"
}
```

- [ ] **Step 4: 매핑의 유일성과 기존 페이지 충돌 검증**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:/tmp/build-zdocs-wiki.ps1 -ValidateMapOnly
```

Expected: `mapped=92 duplicate=0 existingCollision=0`.

### Task 2: Markdown 페이지와 색인 생성

**Files:**
- Modify: `C:/tmp/build-zdocs-wiki.ps1`
- Create: `C:/tmp/nsight-tcf-wiki-sample/ZDOC1-*.md`
- Create: `C:/tmp/nsight-tcf-wiki-sample/ZDOC2-*.md`
- Create: `C:/tmp/nsight-tcf-wiki-sample/ZDocs-Index.md`
- Create: `C:/tmp/nsight-tcf-wiki-sample/ZDocs-1-Index.md`
- Create: `C:/tmp/nsight-tcf-wiki-sample/ZDocs-2-Index.md`

**Interfaces:**
- Consumes: Task 1의 `$pageMap`, Source Baseline SHA
- Produces: 출처 블록과 변환된 링크를 포함한 92개 상세 페이지 및 3개 색인

- [ ] **Step 1: 제목 다음에 출처 블록 삽입 기능 작성**

```markdown
> **문서 계열:** `zdocs-1`
> **Source Baseline:** [`<full-sha>`](<repository>/tree/<full-sha>)
> **원본 파일:** [`zdocs-1/<path>`](<repository>/blob/<full-sha>/zdocs-1/<encoded-path>)
> **적용 원칙:** 실제 구현과 차이가 있으면 구현 검증과 승인된 아키텍처 의사결정을 우선합니다.
```

- [ ] **Step 2: 게시 대상 Markdown 상대 링크 변환 기능 작성**

대상 경로를 현재 파일 디렉터리 기준으로 정규화하고 `$pageMap`에 있으면 `[[대상 페이지]]`로 바꾼다. `#anchor`는 Wiki 링크의 앵커로 보존한다.

- [ ] **Step 3: 비게시 파일과 저장소 외부 상대 링크 변환 기능 작성**

DOCX, JSON, 이미지와 다른 저장소 경로는 `<repository>/blob/<sha>/<encoded-path>` 절대 링크로 바꾼다. 파일이 원본에 없으면 변환 실패 목록에 추가한다.

- [ ] **Step 4: 세 개 색인 생성**

`ZDocs-Index`에는 두 문서군의 목적과 진입 링크를, 문서군 색인에는 디렉터리별 페이지 목록과 원본 비게시 자료 정책을 기록한다.

- [ ] **Step 5: 생성 실행**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:/tmp/build-zdocs-wiki.ps1
```

Expected: `detailPages=92 indexes=3 unresolvedLinks=0`.

### Task 3: 기존 Wiki 탐색 구조 연결

**Files:**
- Modify: `C:/tmp/nsight-tcf-wiki-sample/Home.md`
- Modify: `C:/tmp/nsight-tcf-wiki-sample/_Sidebar.md`
- Modify: `C:/tmp/nsight-tcf-wiki-sample/Documentation-Architecture.md`

**Interfaces:**
- Consumes: Task 2의 세 색인 페이지명
- Produces: 기존 Wiki에서 ZDocs 문서군으로 들어가는 최소 탐색 경로

- [ ] **Step 1: Home에 통합 진입 섹션 추가**

`ZDocs-Index` 링크와 `zdocs-1`, `zdocs-2`의 역할 차이를 한 문단으로 설명하고, 동일 섹션이 있으면 중복 추가하지 않는다.

- [ ] **Step 2: Sidebar에 색인 세 개만 추가**

```markdown
**ZDocs 설계·개발 문서**
- [[ZDocs-Index]]
- [[ZDocs-1-Index]]
- [[ZDocs-2-Index]]
```

- [ ] **Step 3: Documentation Architecture에 문서군 관계 추가**

ZArchitecture는 주제별 상세 아키텍처, ZDocs는 원본 설계·개발 자료 계열이라는 관계를 명시한다.

### Task 4: 게시 전 정적 검증

**Files:**
- Create: `C:/tmp/validate-zdocs-wiki.ps1`
- Read: `C:/tmp/nsight-tcf-wiki-sample/*.md`

**Interfaces:**
- Consumes: 생성된 Wiki 작업 트리와 `$pageMap`
- Produces: 실패 시 non-zero exit code를 반환하는 검증 보고서

- [ ] **Step 1: 페이지 수와 금지 확장자 검사 작성**

검사는 `ZDOC1-*`와 `ZDOC2-*` 상세 페이지가 합계 92개인지, 세 색인이 존재하는지, Git 변경 목록에 `.docx`와 `.json`이 없는지 확인한다.

- [ ] **Step 2: 전체 Wiki 내부 링크 검사 작성**

모든 `[[Page]]` 대상이 실제 `.md` 파일명으로 존재하는지 검사하고 깨진 링크가 있으면 파일명과 대상명을 출력한다.

- [ ] **Step 3: 상대 링크·출처·코드 펜스 검사 작성**

92개 페이지에서 `](./`와 `](../`가 0건인지, Source Baseline 및 원본 링크가 92/92인지, 각 파일의 삼중 백틱 개수가 짝수인지 확인한다.

- [ ] **Step 4: 보안 패턴 검사 작성**

개인키 헤더, AWS Access Key, 비마스킹 password/secret/token 할당, 내부 접속정보 후보를 출력한다. 후보는 문맥 검토 후 실제 비밀만 비식별화하고 검사를 재실행한다.

- [ ] **Step 5: 전체 검증 실행**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:/tmp/validate-zdocs-wiki.ps1
```

Expected: `details=92 indexes=3 broken=0 relative=0 provenance=92 fences=0 secrets=0 forbiddenFiles=0`.

### Task 5: Wiki 커밋, 원격 통합과 게시

**Files:**
- Commit: `C:/tmp/nsight-tcf-wiki-sample/*.md`

**Interfaces:**
- Consumes: Task 4를 통과한 cleanly generated Wiki 변경
- Produces: 원격 GitHub Wiki `master`의 게시 커밋

- [ ] **Step 1: 변경 범위 확인**

Run:

```powershell
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample status --short
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample diff --stat
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample diff --check
```

Expected: 기존 문서 세 개 수정, Markdown 상세 92개와 색인 3개 추가, 바이너리 추가 없음.

- [ ] **Step 2: Wiki 변경 커밋**

```powershell
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample add -- '*.md'
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample commit -m 'docs(wiki): publish zdocs reference collections'
```

- [ ] **Step 3: 원격 최신 변경 rebase**

```powershell
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample pull --rebase origin master
```

충돌이 있으면 기존 원격 내용을 보존해 해결하고 Task 4 전체 검증을 다시 실행한다.

- [ ] **Step 4: 원격 게시**

```powershell
git -c safe.directory='C:/tmp/nsight-tcf-wiki-sample' -C C:/tmp/nsight-tcf-wiki-sample push origin master
```

- [ ] **Step 5: 최종 원격 일치 검증**

Run Task 4 검증을 새로 실행한 뒤 `rev-parse HEAD`, `ls-remote origin refs/heads/master`, `status --porcelain`을 비교한다.

Expected: 로컬 HEAD와 원격 HEAD가 같고 Wiki 작업 트리가 clean이다.
