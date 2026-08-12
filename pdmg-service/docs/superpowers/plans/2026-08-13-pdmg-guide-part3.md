# PDMG 애플리케이션 아키텍처와 개발 가이드 제3부 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** HTTP 요청부터 TCF OFF까지 원 목차 74개 절을 보존한 6개 장을 작성한다.

**Architecture:** `pdmg-fw`와 `pdmg-service` 실행 코드를 우선하여 요청 생명주기를 순서대로 설명한다. 시스템 처리, Worker 실행과 업무 트랜잭션 경계를 분리한다.

**Tech Stack:** Markdown, Spring MVC, Servlet Filter, Spring AOP/Transaction, MyBatis

## Global Constraints

- AS-IS만 기술하고 74개 절 제목과 순서를 유지한다.
- 결과는 기존 책 디렉터리의 `08장`부터 `13장`까지 별도 파일로 둔다.
- 민감정보와 개인 경로를 기록하지 않는다.

### Task 1: 근거 확정

- [ ] Filter, Interceptor, Context, TCF, Timeout, TCF OFF Controller의 실제 호출·조건을 검색한다.

### Task 2: 8~9장 작성

- [ ] 8.1~8.13과 9.1~9.13을 원 제목으로 작성한다.

### Task 3: 10~11장 작성

- [ ] 10.1~10.10과 11.1~11.14를 원 제목으로 작성한다.

### Task 4: 12~13장 작성

- [ ] 12.1~12.13과 13.1~13.11을 원 제목으로 작성한다.

### Task 5: 검증

- [ ] 목차의 74개 절과 자동 대조한다.
- [ ] 상대 링크, Markdown 공백과 금지 문자열을 검사한다.
