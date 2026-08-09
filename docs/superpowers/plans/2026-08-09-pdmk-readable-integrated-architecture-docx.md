# PDMK Readable Integrated Architecture DOCX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `통합 아키텍처 정의서 copy.md`의 내용을 중복 없이 학습·참조 가능한 개발자·아키텍트용 Word 문서로 재구성한다.

**Architecture:** 원본 29절을 17개 독자 중심 장으로 매핑하고, 본문은 핵심 설명·다이어그램·실행 계약으로 구성하며 상세 목록은 부록 색인으로 이동한다. 문서 생성기를 사용해 스타일과 표 구조를 일관되게 적용하고 전 페이지 렌더링으로 검증한다.

**Tech Stack:** Python 3, python-docx, Pillow, OOXML, LibreOffice, Poppler

## Global Constraints

- 원본 Markdown을 수정하지 않는다.
- 현재 구현 사실과 조건부 구현을 구분한다.
- 중복 설명을 통합하되 핵심 API·설정·클래스·테이블 정보는 보존한다.
- 최종 산출물은 DOCX 하나다.

---

### Task 1: 원본 구조 매핑

- [ ] 원본 29절의 제목과 핵심 표를 추출한다.
- [ ] 각 주제를 승인된 17개 장·부록에 매핑한다.
- [ ] 중복 설명과 참조용 상세 데이터를 구분한다.

### Task 2: 문서 생성

- [ ] compact reference guide 스타일 토큰을 구현한다.
- [ ] 시스템·모듈·거래·데이터 흐름 다이어그램을 생성한다.
- [ ] 17개 장의 학습 목표, 설명, 표, 코드 예시, 체크리스트를 작성한다.
- [ ] API·클래스·설정·테이블·용어·영향 매트릭스를 부록으로 작성한다.

### Task 3: 구조 검증

- [ ] DOCX 제목, 표, 다이어그램, 페이지 설정을 감사한다.
- [ ] 원본 핵심 주제의 새 목차 매핑을 점검한다.
- [ ] 접근성 감사를 수행한다.

### Task 4: 렌더링 검증

- [ ] DOCX를 PDF와 페이지 PNG로 렌더링한다.
- [ ] 모든 페이지에서 잘림, 겹침, 표 분할, 글꼴 문제를 확인한다.
- [ ] 결함을 수정하고 재렌더링한다.

### Task 5: 전달

- [ ] 최종 DOCX와 검증 결과를 사용자에게 전달한다.

