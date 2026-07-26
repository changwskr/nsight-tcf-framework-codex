# EB(Event Bridge) UI 레이아웃 설계서

> 대상 모듈: `eb-service` (업무코드 **EB**) · UI 소스: `tcf-ui/src/main/resources/static/eb/`
> 작성일: 2026-07-25 · 문서 위치: `ztcf-methodology/EB-UI-레이아웃-설계서.md`

---

## 1. 문서 개요

| 항목 | 내용 |
|------|------|
| 문서 목적 | EB 업무 화면의 레이아웃 구조·입력 필드·그리드·이벤트를 정의한다 |
| 대상 서비스 | `eb-service` (bootRun 8089, Tomcat context `/eb`) |
| UI 서빙 모듈 | `tcf-ui` (bootRun 8099, ztomcat `/ui`) — 정적 HTML/CSS/JS |
| 거래 방식 | 모든 화면은 `POST /eb/online` 단일 엔드포인트로 표준전문(JSON header+body) 거래 호출 |
| 공통 리소스 | `/_shared/online.css`, `/_shared/eb-admin.css`, `/_shared/ui-context.js`, `/_shared/eb-admin.js` |

## 2. 화면 목록

| 화면번호 | 화면명 | URL | 주요 거래(serviceId) | 성격 |
|----------|--------|-----|----------------------|------|
| - | EB 거래 테스트 | `/eb/index.html` | 전체 EB 거래 | 개발자용 거래 테스트 |
| 19101 | EB 사용자 관리 | `/eb/user-management.html` | `EB.User.create`, `EB.User.inquiry` | 등록 + 목록 조회 |
| 19410 | 시스템 거래 현황 | `/eb/system-tx-status.html` | `EB.SystemTx.inquiry` | 조회 전용(페이징) |
| 19200 | EB 이벤트·배치 모니터 | `/eb/event-monitor.html` | `EB.Event.inquiry`, `EB.Batch.inquiry` | 모니터링(자동 갱신) |

## 3. 공통 레이아웃 구조

| 영역 | 구성 요소 | 설명 |
|------|-----------|------|
| Header | `h1` 제목, 설명문, `nav-links` | 화면 제목과 타 화면 이동 링크(업무 목록/거래 테스트/타 화면) |
| Meta Bar | `eb-meta-card` 카드 N개 | 연결 대상 URL, 사용 거래(serviceId + transactionCode), 상태 표시 |
| Form/Filter | `eb-form-panel`, `eb-filter-bar` | 등록 폼 또는 조회 조건 입력 영역 |
| Table Panel | `eb-table-panel` + `eb-table` | 조회 결과 그리드 |
| Pagination | `eb-pagination` | `pageNo`/`pageSize`/`totalCount` 기반 페이지 이동 |
| Alert | `eb-alert` | 등록/조회 성공·실패 메시지 (ok/fail 스타일) |
| Error Popup | `NsightErrorPopup` | 공통 오류 팝업(초기화 실패 등 시스템 오류) |

## 4. 화면별 상세 설계

### 4.1 EB 사용자 관리 (`user-management.html`, 화면 19101)

#### 4.1.1 화면 구성 블록

| 순서 | 블록 | 내용 |
|------|------|------|
| 1 | Header | 제목 "EB 사용자 관리", 설명(등록 시 `EB_EVENT(READY)` 생성 안내), 이동 링크 |
| 2 | Meta Bar | 연결 대상 URL / 조회 거래 `EB.User.inquiry`(EB-USR-0002) / 등록 거래 `EB.User.create`(EB-USR-0001) |
| 3 | 사용자 등록 폼 | 3필드 입력 + 등록/초기화 버튼 + 결과 알림 |
| 4 | 조회 필터 바 | 3필드 필터 + 조회/필터 초기화 버튼 |
| 5 | 사용자 목록 그리드 | 6컬럼 테이블 + 페이지네이션(페이지당 15건) |

#### 4.1.2 입력 필드 — 사용자 등록 폼

| 필드 ID | 라벨 | 타입 | 필수 | 최대길이 | placeholder | 매핑 body 필드 |
|---------|------|------|------|----------|-------------|----------------|
| `createUserId` | 사용자 ID | text | Y | 50 | U001 | `userId` |
| `createUserName` | 사용자 이름 | text | Y | 100 | 홍길동 | `userName` |
| `createBranchId` | 지점 ID | text | N | 20 | 001 | `branchId` (미입력 시 null) |

#### 4.1.3 입력 필드 — 조회 필터

| 필드 ID | 라벨 | 타입 | 매칭 방식 | 매핑 body 필드 |
|---------|------|------|-----------|----------------|
| `filterUserId` | 사용자 ID | text | LIKE 부분 일치 | `userId` |
| `filterUserName` | 사용자 이름 | text | LIKE 부분 일치 | `userName` |
| `filterBranchId` | 지점 ID | text | = 정확히 일치 | `branchId` |

#### 4.1.4 그리드 컬럼 — 사용자 목록

| 순번 | 컬럼명 | 응답 필드 | 표시 형식 |
|------|--------|-----------|-----------|
| 1 | 사용자 ID | `userId` | 강조(bold) |
| 2 | 이름 | `userName` | 텍스트 |
| 3 | 지점 | `branchId` | 텍스트 |
| 4 | 등록일시 | `createdAt` | timestamp 포맷 |
| 5 | 최근 이벤트 | `eventType` + `eventId` | `유형 · 이벤트ID(mono)` 조합, 없으면 `-` |
| 6 | 이벤트 상태 | `eventStatus` | 상태 칩(READY/SENT/FAIL 색상) |

#### 4.1.5 이벤트(액션) 정의

| 트리거 | 액션 | 호출 거래 | 처리 내용 |
|--------|------|-----------|-----------|
| `btnCreate` 클릭 / 등록폼 Enter | 사용자 등록 | `EB.User.create` (CREATE) | 필수값 검증 → 등록 → 알림 표시 → 폼 초기화 → 1페이지 재조회 |
| `btnResetForm` 클릭 | 입력 초기화 | - | 등록 폼 3필드 및 알림 초기화 |
| `btnSearch` 클릭 / 필터 Enter | 조회 | `EB.User.inquiry` | 필터+페이징 body 구성 후 조회, 그리드 렌더링 |
| `btnClearFilter` 클릭 | 필터 초기화 | `EB.User.inquiry` | 필터 비우고 1페이지 재조회 |
| 페이지네이션 클릭 | 페이지 이동 | `EB.User.inquiry` | `pageNo` 변경 후 재조회 |
| 화면 로드 | 초기화 | `EB.User.inquiry` | `EbAdmin.loadConfig()` → 연결 대상 표시 → 1페이지 조회 |

### 4.2 시스템 거래 현황 (`system-tx-status.html`, 화면 19410)

#### 4.2.1 화면 구성 블록

| 순서 | 블록 | 내용 |
|------|------|------|
| 1 | Topbar | 제목 "시스템 거래 현황", 화면번호 19410 · `EB.SystemTx.inquiry`(EB-STX-0001) 표기, 화면초기화/도움말 버튼 |
| 2 | Meta Bar | 연결 대상 URL / 조회 거래 / 조회 상태(대기·조회중·완료) |
| 3 | 조회 조건 영역 | 3열 그리드(`stx-filter-grid`) 6개 조건 + 조회/새로고침 버튼 |
| 4 | 결과 그리드 | 13컬럼 테이블(가로 스크롤, min-width 1400px) + 총건수 라벨 + 페이지네이션(페이지당 20건) |
| 5 | 찾기 모달 | 직원/화면 목록 선택 팝업(`eb-modal`) — 행 클릭 시 조회조건 반영 |

#### 4.2.2 조회 조건 필드

| 필드 ID | 라벨 | 타입 | 기본값 | 매핑 body 필드 | 비고 |
|---------|------|------|--------|----------------|------|
| `filterFrom` ~ `filterTo` | 거래일자 | datetime-local 범위 | 2026-03-05 09:00 ~ 23:59 (샘플 기준일) | `txDateFrom`, `txDateTo` | 서버에서 날짜만 입력 시 00:00:00/23:59:59 보정 |
| `filterTxType` | 거래구분 | select | 전체 | `txType` | 전체("") / 정상(NORMAL) / 오류(ERROR) |
| `filterTxSeqNo` | 거래일련번호 | text(40) | - | `txSeqNo` | LIKE 부분 일치 |
| `filterEmpNo` | 직원번호 | text(20) + 찾기 버튼 + 직원명(readonly) | - | `empNo` | 「찾기」 팝업으로 입력 보조, LIKE 부분 일치 |
| `filterScreenId` | 화면번호 | text(20) + 찾기 버튼 + 화면명(readonly) | - | `screenId` | 「찾기」 팝업 선택 시 번호·화면명 자동 채움, LIKE 부분 일치 |
| `filterServiceId` | 서비스ID | text(80) | - | `serviceId` | LIKE 부분 일치 |

#### 4.2.3 그리드 컬럼 — 시스템 거래 현황 (13컬럼)

| 순번 | 컬럼명 | 응답 필드 | DB 컬럼(EB_SYSTEM_TX) |
|------|--------|-----------|------------------------|
| 1 | 순번 | `rowNo` | (서버 계산: 페이지 오프셋 + i + 1) |
| 2 | 거래일련번호 | `txSeqNo` | TX_SEQ_NO |
| 3 | 거래일자 | `txDate` | TX_DATE |
| 4 | 마감화면ID | `screenId` | SCREEN_ID |
| 5 | 거래서비스ID | `serviceId` | SERVICE_ID |
| 6 | 글로벌ID | `globalId` | GLOBAL_ID |
| 7 | 요청일시 | `requestAt` | REQUEST_AT |
| 8 | 응답일시 | `responseAt` | RESPONSE_AT |
| 9 | 거래소요시간(초) | `elapsedSec` | ELAPSED_SEC |
| 10 | 거래입력내용 | `inputContent` | INPUT_CONTENT |
| 11 | 거래자개인번호 | `empNo` | EMP_NO |
| 12 | 거래사무소코드 | `branchCode` | BRANCH_CODE |
| 13 | 거래단말기IP주소 | `terminalIp` | TERMINAL_IP |

#### 4.2.4 찾기 모달(팝업) 설계

| 항목 | 직원 찾기 | 화면 찾기 |
|------|-----------|-----------|
| 트리거 | `btnEmpPopup` | `btnScreenPopup` |
| 데이터 | 화면 내 정적 목록(E1001 김운영 등 4건) | 정적 목록(19410/19101/19200) |
| 컬럼 | 번호 / 이름 | 번호 / 이름 |
| 행 클릭 동작 | `filterEmpNo`+`filterEmpName` 채움 | `filterScreenId`+`filterScreenName` 채움 |
| 닫기 | `btnLookupClose` / 백드롭 클릭 | 동일 |

#### 4.2.5 이벤트(액션) 정의

| 트리거 | 액션 | 호출 거래 | 처리 내용 |
|--------|------|-----------|-----------|
| `btnSearch` 클릭 | 조회 | `EB.SystemTx.inquiry` | 조건 body 구성 → 조회 → 그리드/총건수/페이지네이션 갱신 |
| `btnRefresh` 클릭 | 새로고침 | `EB.SystemTx.inquiry` | 현재 조건 그대로 재조회 |
| `btnScreenReset` 클릭 | 화면초기화 | - | 조건을 기본값(샘플 기준일)으로 복원 |
| `btnHelp` 클릭 | 도움말 | - | 화면 사용법 안내 |
| 페이지네이션 클릭 | 페이지 이동 | `EB.SystemTx.inquiry` | `pageNo` 변경 후 재조회 (pageSize 20) |

### 4.3 EB 이벤트·배치 모니터 (`event-monitor.html`, 화면 19200)

#### 4.3.1 화면 구성 블록

| 순서 | 블록 | 내용 |
|------|------|------|
| 1 | Header | 제목, `EB_EVENT` Outbox·EP 발행 배치 설명, 이동 링크 |
| 2 | Meta Bar | 연결 대상 / 배치 조회 `EB.Batch.inquiry`(EB-BAT-0001) / 이벤트 조회 `EB.Event.inquiry`(EB-EVT-0001) / 마지막 갱신 시각 |
| 3 | 배치 상태 패널 | 상태 집계 카드(`eb-stat-grid`) + 배치 정보 그리드(`eb-batch-grid`) + 자동갱신 체크박스·전체 새로고침 버튼 |
| 4 | 이벤트 필터 바 | 4개 조건 + 이벤트 조회/필터 초기화 버튼 |
| 5 | 이벤트 목록 그리드 | 7컬럼 테이블 + 페이지네이션(페이지당 15건) |

#### 4.3.2 배치 상태 표시 항목 (`EB.Batch.inquiry` 응답)

| 표시 항목 | 응답 필드 | 설명 |
|-----------|-----------|------|
| 배치 활성 | `enabled` | `nsight.eb.event-publish.enabled` |
| 실행 주기 | `fixedDelayMs` | 기본 60,000ms 표시 포맷 변환 |
| 1회 처리 건수 | `batchSize` | 기본 50건 |
| 스케줄러 | `schedulerClass`.`schedulerMethod` | `EbEventPublishScheduler.publishUserEvents` |
| EP URL | `epOnlineUrl` | 기본 `http://127.0.0.1:8090/ep/online` |
| 대기(READY) | `readyCount` | READY 상태 건수 |
| 상태 집계 카드 | `statusSummary` | READY / SENT / FAIL / TOTAL 건수 |

#### 4.3.3 이벤트 조회 필터

| 필드 ID | 라벨 | 타입 | 매칭 방식 | 매핑 body 필드 |
|---------|------|------|-----------|----------------|
| `filterEventId` | 이벤트 ID | text | LIKE 부분 일치 | `eventId` |
| `filterUserId` | 사용자 ID | text | LIKE 부분 일치 | `userId` |
| `filterEventType` | 이벤트 유형 | text (예: USER_CREATED) | = 일치 | `eventType` |
| `filterEventStatus` | 상태 | select (전체/READY/SENT/FAIL) | = 일치 | `eventStatus` |

#### 4.3.4 그리드 컬럼 — 이벤트 목록 (7컬럼)

| 순번 | 컬럼명 | 응답 필드 | DB 컬럼(EB_EVENT) |
|------|--------|-----------|--------------------|
| 1 | 이벤트 ID | `eventId` | EVENT_ID |
| 2 | 사용자 ID | `userId` | USER_ID |
| 3 | 유형 | `eventType` | EVENT_TYPE |
| 4 | 상태 | `eventStatus` | EVENT_STATUS (상태 칩) |
| 5 | 재시도 | `retryCount` | RETRY_COUNT |
| 6 | 생성일시 | `createdAt` | CREATED_AT |
| 7 | 전송일시 | `sentAt` | SENT_AT (SENT 시각) |

#### 4.3.5 이벤트(액션) 정의

| 트리거 | 액션 | 호출 거래 | 처리 내용 |
|--------|------|-----------|-----------|
| 화면 로드 / `btnRefreshAll` | 전체 새로고침 | `EB.Batch.inquiry` + `EB.Event.inquiry` | 배치 상태·집계·이벤트 목록 동시 갱신, 마지막 갱신 시각 표시 |
| `autoRefresh` 체크 | 자동 갱신 | 동일 | 30초 주기 타이머로 전체 새로고침 |
| `btnSearch` 클릭 | 이벤트 조회 | `EB.Event.inquiry` | 필터+페이징으로 조회 |
| `btnClearFilter` 클릭 | 필터 초기화 | `EB.Event.inquiry` | 필터 비우고 1페이지 재조회 |
| 페이지네이션 클릭 | 페이지 이동 | `EB.Event.inquiry` | `pageNo` 변경 후 재조회 |

## 5. 화면-거래 매핑 요약

| 화면 | serviceId | transactionCode | processingType | 페이지 크기 |
|------|-----------|-----------------|----------------|-------------|
| 사용자 관리(19101) | `EB.User.create` | EB-USR-0001 | CREATE | - |
| 사용자 관리(19101) | `EB.User.inquiry` | EB-USR-0002 | INQUIRY | 15 |
| 시스템 거래 현황(19410) | `EB.SystemTx.inquiry` | EB-STX-0001 | INQUIRY | 20 |
| 이벤트·배치 모니터(19200) | `EB.Event.inquiry` | EB-EVT-0001 | INQUIRY | 15 |
| 이벤트·배치 모니터(19200) | `EB.Batch.inquiry` | EB-BAT-0001 | INQUIRY | - |
| 거래 테스트 | `EB.Sample.inquiry` | EB-INQ-0001 | INQUIRY | - |

## 6. 공통 UI 규칙

| 구분 | 규칙 |
|------|------|
| 거래 호출 | `EbAdmin.inquiry()`/`EbAdmin.mutate()`가 표준전문 header(businessCode=EB, serviceId, transactionCode, processingType, channelId)를 구성하여 `POST /eb/online` 호출 |
| 상태 칩 | 이벤트 상태(READY=대기, SENT=성공, FAIL=실패)를 색상 칩으로 표시 (`EbAdmin.chipForEventStatus`) |
| 페이지네이션 | 응답의 `pageNo`/`pageSize`/`totalCount`로 `EbAdmin.renderPagination` 공통 렌더링 |
| 빈 결과 | `eb-empty` 셀로 "조회 중..." / "등록된 사용자가 없습니다." 등 상태 문구 표시 |
| Enter 키 | 필터·등록 폼 입력 필드에서 Enter 입력 시 즉시 조회/등록 실행 |
| 오류 처리 | 거래 실패 시 alert 영역 또는 그리드 내 오류 문구, 초기화 실패는 `NsightErrorPopup` 공통 팝업 |
| 반응형 | 19410 필터 그리드는 980px 이하에서 1열로 전환 |
