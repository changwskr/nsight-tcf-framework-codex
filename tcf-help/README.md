# tcf-help — NSIGHT TCF HELP 문서

브라우저 HELP 화면(`tcf-ui` :8099)에서 **운영 가이드**와 **저장소 전체 마크다운 라이브러리**를 제공합니다.

## 구조

```
tcf-help/
├── docs/                 # HELP 네이티브 운영 가이드 (12편)
├── meta/                 # 코퍼스별 설명·문서 지도
├── help-index.yml        # 네이티브 HELP 네비게이션
├── doc-catalog.json      # 전체 repo .md 카탈로그 (자동 생성)
├── _scripts/
│   ├── scan-repo-docs.mjs
│   ├── mirror-library.mjs
│   └── build-index.mjs
└── build/help/           # export 산출물
    ├── docs/ meta/
    ├── help-index.json
    ├── doc-catalog.json
    └── library/          # 저장소 md 미러 (638+)
```

## 코퍼스 분류 (자동)

| ID | 설명 | 약 개수 |
|----|------|--------|
| help-native | tcf-help/docs | 12 |
| zguide | 모듈별 개발 가이드 | 22 |
| zman | 설계·코드 대조 | 27 |
| zdocs-2 | 기능·운영 개발 노트 | 자동 산정 |
| znsight-man | 개발 표준 | 119 |
| capacity | 용량·환경설정 | 90 |
| architecture | zdocs-1, zarchitecture | 자동 산정 |
| ztcfbook* | TCF 교재 3계열 | 183 |
| config | 설정 참고 | 46 |
| module-readme | 모듈 README | 25 |

## 빌드

```bash
gradle :tcf-help:exportHelp :tcf-ui:processResources
gradle :tcf-help:verifyHelp    # 품질 검증 (Phase 4)
```

내부 순서: `scanDocCatalog` → `scanBusinessScreens` → `buildHelpIndex` → `mirrorLibrary` → `exportHelp`

## 화면 HELP 맵

| 파일 | 설명 |
|------|------|
| `help-screen-map.json` | OC·HELP 네이티브 (`help-index.yml` screens) |
| `help-business-map.json` | 업무·OM·JWT HTML 자동 스캔 |
| `help-screen-overrides.yml` | 화면별 수동 보강 (Phase 5) |
| `help-link-report.json` | 깨진 링크·미매핑 화면 리포트 |

## 품질·CI

```bash
gradle :tcf-help:verifyHelp          # 전체 검증
gradle :tcf-help:checkHelpLinks      # 링크 리포트만
```

- 브라우저: `/help/health.html` — 링크·화면 매핑 대시보드
- pre-commit (선택): `tcf-help/scripts/install-githooks.ps1`

## HELP URL

| URL | 설명 |
|-----|------|
| `/help.html` | HELP 허브 |
| `/help/library.html` | **전체 문서 검색·목록** |
| `/help/view.html?doc=...` | 네이티브·메타 가이드 |
| `/help/view.html?src=zguide/...` | 저장소 원문 md |

## 문서 추가

- **운영 가이드**: `docs/` + `help-index.yml`
- **저장소 다른 md**: 원본 위치에 작성 → `scan-repo-docs`가 자동 카탈로그

```bash
node _scripts/scan-repo-docs.mjs
node _scripts/mirror-library.mjs
```
