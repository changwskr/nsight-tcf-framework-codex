# PDMK AS-IS Architecture DOCX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 `pdmk-fw`, `pdmk-service`, `pdmk-ui` 구현을 근거로 개발자·아키텍트용 「PDMK AS-IS 아키텍처 상세 정의서」 DOCX를 생성하고 전 페이지 렌더링 검증한다.

**Architecture:** 정본 Markdown의 설명 구조를 사용하되 Java, YAML, Gradle, Mapper XML과 샘플 요청을 교차 검증한다. 문서 생성기는 근거 데이터, 다이어그램, Word 스타일·표·본문 생성을 분리하며 최종 DOCX는 `compact_reference_guide` 프리셋과 `editorial_cover` 첫 페이지를 사용한다.

**Tech Stack:** Java 21/Spring Boot 소스 분석, Python 3, python-docx, matplotlib, OOXML, LibreOffice/Poppler 기반 `render_docx.py`

## Global Constraints

- 독자는 PDMK 업무·프레임워크 개발자와 애플리케이션·솔루션 아키텍트다.
- 현재 저장소에서 확인되는 AS-IS만 기술하고 TO-BE·개선 로드맵은 포함하지 않는다.
- 문서와 구현이 다르면 소스 코드와 실행 설정을 우선한다.
- 기존 `pdmk-fw`, `pdmk-service`, `pdmk-ui` 파일은 수정하지 않는다.
- 최종 산출물은 DOCX 하나이며, 렌더 PNG와 PDF는 내부 QA 전용이다.

---

### Task 1: AS-IS 근거 데이터 수집

**Files:**
- Create: `build/pdmk-architecture/evidence.md`
- Read: `pdmk-service/docs/PDMK_아키텍처_정의서.md`
- Read: `pdmk-fw/src/main/**`, `pdmk-service/src/main/**`, `pdmk-ui/src/main/**`

**Interfaces:**
- Consumes: 승인된 제작 설계와 저장소 소스
- Produces: API, 포트, 모듈 책임, 실행 체인, 설정 키, 데이터 접근, 로그·보안 근거 목록

- [ ] **Step 1:** `git status --short`, 세 모듈 README·Gradle·설정 파일을 다시 확인한다.
- [ ] **Step 2:** `rg`로 Controller API, Filter/Interceptor/Aspect/Resolver, Service/DAO/Mapper, DataSource, Security, Context/MDC 호출부를 추출한다.
- [ ] **Step 3:** 정본 Markdown의 각 장을 소스 근거와 대조해 `build/pdmk-architecture/evidence.md`에 사실만 기록한다.
- [ ] **Step 4:** `rg -n "TBD|TODO|TO-BE|권장|개선" build/pdmk-architecture/evidence.md`로 범위 이탈을 검사한다.

### Task 2: 다이어그램과 문서 생성기 작성

**Files:**
- Create: `build/pdmk-architecture/create_pdmk_architecture_docx.py`
- Create: `build/pdmk-architecture/assets/system-context.png`
- Create: `build/pdmk-architecture/assets/module-dependencies.png`
- Create: `build/pdmk-architecture/assets/transaction-sequence.png`
- Create: `build/pdmk-architecture/assets/data-flow.png`

**Interfaces:**
- Consumes: `evidence.md`, 정본 Markdown, 세 모듈 소스 경로
- Produces: `create_document(output_path: Path) -> None`와 4개 PNG 다이어그램

- [ ] **Step 1:** 워크스페이스 의존성 로더로 Python과 문서 라이브러리 경로를 확인한다.
- [ ] **Step 2:** `compact_reference_guide`의 Letter·1인치 여백·Calibri 11pt·1.25줄·고정 9360 DXA 표 토큰을 코드 상수로 정의한다.
- [ ] **Step 3:** matplotlib로 시스템 컨텍스트, 모듈 의존관계, 거래 시퀀스, 데이터 흐름 다이어그램을 생성한다.
- [ ] **Step 4:** real Word heading/list/numbering, 고정폭 표, 반복 표 머리글, 머리글·바닥글·페이지 필드를 생성하는 헬퍼를 구현한다.
- [ ] **Step 5:** 승인된 15개 장과 부록을 실제 클래스·API·설정·테이블 근거로 작성한다.

### Task 3: DOCX 생성 및 구조 감사

**Files:**
- Create: `pdmk-service/docs/PDMK_AS-IS_아키텍처_상세_정의서.docx`
- Read: `build/pdmk-architecture/create_pdmk_architecture_docx.py`

**Interfaces:**
- Consumes: `create_document(output_path)`
- Produces: 최종 후보 DOCX

- [ ] **Step 1:** 번들 Python으로 생성기를 실행해 DOCX를 만든다.
- [ ] **Step 2:** DOCX를 다시 열어 제목 계층, 표 개수, 그림 개수, 섹션·여백을 검사한다.
- [ ] **Step 3:** `table_geometry.py` 또는 동등한 OOXML 감사로 모든 표의 `tblW`, `tblGrid`, `tcW`, `tblInd`를 확인한다.
- [ ] **Step 4:** `a11y_audit.py`를 실행해 제목 구조, 표 머리글과 이미지 대체 텍스트를 확인하고 안전한 항목은 수정한다.

### Task 4: 렌더링 및 전 페이지 시각 검증

**Files:**
- Create: `build/pdmk-architecture/rendered/page-*.png`
- Read: `pdmk-service/docs/PDMK_AS-IS_아키텍처_상세_정의서.docx`

**Interfaces:**
- Consumes: 최종 후보 DOCX
- Produces: 전 페이지 시각 QA 결과와 수정된 최종 DOCX

- [ ] **Step 1:** `render_docx.py`로 DOCX를 PNG와 QA용 PDF로 렌더링한다.
- [ ] **Step 2:** 모든 페이지 이미지를 100% 기준으로 확인해 글자·표·다이어그램·머리글·바닥글의 잘림과 겹침을 찾는다.
- [ ] **Step 3:** 결함이 있으면 생성기 또는 스타일 토큰을 수정하고 DOCX 생성·렌더링을 반복한다.
- [ ] **Step 4:** 최종 렌더의 페이지 수와 PNG 존재 여부를 확인하고 DOCX 수정 시각과 렌더 시각의 선후를 확인한다.

### Task 5: 최종 인계

**Files:**
- Deliver: `pdmk-service/docs/PDMK_AS-IS_아키텍처_상세_정의서.docx`

**Interfaces:**
- Consumes: 렌더 검증을 통과한 DOCX
- Produces: 사용자에게 전달할 단일 Word 문서

- [ ] **Step 1:** 최종 DOCX에 내부 도구 토큰, 임시 문구, TO-BE 권고가 없는지 검색한다.
- [ ] **Step 2:** 생성 근거, 문서 범위, 렌더 검증 결과를 요약하고 DOCX를 한 번만 출력 인용한다.

