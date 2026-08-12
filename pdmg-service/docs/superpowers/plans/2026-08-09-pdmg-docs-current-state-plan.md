# PDMG Documentation Current-State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `pdmg-service/docs`의 Markdown을 현재 `pdmg-service`, `pdmg-fw`, `pdmg-ui` 구현과 일치하도록 현행화한다.

**Architecture:** 실행 코드와 설정을 사실의 정본으로 삼고, 번호 문서는 개발자가 읽는 AS-IS 흐름으로 정리한다. 분석 답변 사본인 `*-1.md`는 정본과 구분하고, 절대 로컬 경로·과거 패키지·과거 UI Relay·과거 오류 전문 설명을 제거한다.

**Tech Stack:** Java 21, Spring Boot, Spring MVC/Security, MyBatis, Gradle, Markdown.

## Global Constraints

- Java 및 설정 소스는 수정하지 않는다.
- 기존 사용자 변경을 되돌리지 않는다.
- Markdown은 UTF-8을 유지한다.
- 구현되지 않은 개선안은 AS-IS로 표현하지 않고 `주의` 또는 `개선 필요`로 표시한다.
- 문서 링크는 저장소 상대 경로를 사용하고 개인 PC 절대 경로를 제거한다.

---

### Task 1: 문서 인벤토리와 사실 기준표

**Files:**
- Inspect: `pdmg-service/docs/*.md`
- Inspect: `pdmg-service/src/main/**`
- Inspect: `pdmg-fw/src/main/**`
- Inspect: `pdmg-ui/src/main/**`

- [ ] 문서 목록과 `-1` 분석 사본을 분류한다.
- [ ] HTTP, 전문, 예외, 트랜잭션, Timeout, 패키지, 설정의 현재 사실을 코드에서 확인한다.
- [ ] 오래된 패키지, Relay, 오류 봉투 및 절대 경로를 검색한다.

### Task 2: 핵심 흐름 문서 현행화

**Files:**
- Modify: `pdmg-service/docs/00.BigPicture Tx 흐름.md`
- Modify: `pdmg-service/docs/03.어플리케이션 레이어드 아키텍처.md`
- Modify: `pdmg-service/docs/05.전체 빅픽처 흐름.md`
- Modify: `pdmg-service/docs/09.서비스ID.md`
- Modify: `pdmg-service/docs/10.전문.md`
- Modify: `pdmg-service/docs/11.Http CORS적용.md`
- Modify: `pdmg-service/docs/11.예외처리.md`
- Modify: `pdmg-service/docs/12.http요청.md`

- [ ] UI 직접 호출+CORS 흐름으로 통일한다.
- [ ] 성공 `dto`, 실패 `result` 전문으로 통일한다.
- [ ] 서비스 ID 우선순위와 URL 불일치 위험을 기록한다.
- [ ] Filter 오류와 일반 예외의 비표준 경로를 기록한다.

### Task 3: 트랜잭션·Timeout·선후처리 문서 현행화

**Files:**
- Modify: `pdmg-service/docs/01.트랜잭션처리 변경.md`
- Modify: `pdmg-service/docs/14.시스템 선후처리.md`
- Modify: `pdmg-service/docs/15.Interceptor-ServicePrevention.preHandle.md`
- Modify: `pdmg-service/docs/20.타임아웃.md`
- Modify: `pdmg-service/docs/21.업무선처리-BizPrePostAspect.md`
- Modify: `pdmg-service/docs/25.업무후처리-BizPrePostAspect.md`
- Modify: `pdmg-service/docs/26.시스템후처리.md`
- Modify: `pdmg-service/docs/pdmg-service 트랜잭션흐름.md`
- Modify: `pdmg-service/docs/트랜잭션처리.md`

- [ ] Facade 트랜잭션과 Timeout Executor 외곽 트랜잭션의 실제 관계를 기록한다.
- [ ] `InterruptedException`을 삼키는 현재 결함을 AS-IS 주의사항으로 기록한다.
- [ ] Advice 처리 예외와 미처리 예외의 ImageLog 흐름을 구분한다.

### Task 4: 구조·계층·DAO·설정 문서 현행화

**Files:**
- Modify: `pdmg-service/docs/02.어플리케이션 컴포넌트 구조.md`
- Modify: `pdmg-service/docs/04.패키지구조.md`
- Modify: `pdmg-service/docs/06.네이밍 형식.md`
- Modify: `pdmg-service/docs/07.도메인 정의 및 호출방식.md`
- Modify: `pdmg-service/docs/08.대용량 페이징 처리방식.md`
- Modify: `pdmg-service/docs/16.Service Context.md`
- Modify: `pdmg-service/docs/16.TCF-OnlineTransactionController.md`
- Modify: `pdmg-service/docs/17.TCF-TcfFace-Dispatcher.md`
- Modify: `pdmg-service/docs/18.Business-Handler.md`
- Modify: `pdmg-service/docs/19.Business-Facade.md`
- Modify: `pdmg-service/docs/22.Business-Service.md`
- Modify: `pdmg-service/docs/23.DAO.md`
- Modify: `pdmg-service/docs/24.DAO-Mapper.md`
- Modify: `pdmg-service/docs/24.DAO-Namespace.md`
- Modify: `pdmg-service/docs/27.전문 DTO.md`
- Modify: `pdmg-service/docs/28.Spring 환경구성정보.md`

- [ ] `nhnis.mg.co.a` 단일 업무 패키지를 기준으로 정리한다.
- [ ] 현재 8개 서비스 ID와 Mapper namespace를 대조한다.
- [ ] local/dev 설정과 운영 설정 부재를 구분한다.

### Task 5: 문서 품질 검증

**Files:**
- Verify: `pdmg-service/docs/**/*.md`

- [ ] 개인 PC 절대 경로가 남지 않았는지 검색한다.
- [ ] 존재하지 않는 상대 Markdown 링크를 검사한다.
- [ ] 구 패키지와 과거 Relay 표현을 검사한다.
- [ ] `dto`/`result`, 서비스 ID 길이, Timeout 취소 설명을 검사한다.
- [ ] `git diff --check`와 문서 Diff를 검토한다.
