<!-- source: ztcf-집필본/NSIGHT TCF Chapter 30- 파일·대용량·Batch·Scheduler 설계.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제30장. 파일·대용량·Batch·Scheduler

## 도입 전 안내말

제29장에서는 반복 조회되는 소규모 기준정보를 Cache에 저장해 DB 부하를 줄이는 방법을 살펴보았다.

이번 장에서는 한 번의 온라인 거래로 처리하기 어려운 다음 업무를 다룬다.

\`\`\`text id=“bat30001” 대용량 파일 업로드

대용량 파일 다운로드

수십만·수백만 건 데이터 처리

일별·월별 집계

대상 고객 추출

대량 메시지 발송

파일 송수신

로그·이력 보관기간 정리

운영 상태 주기 수집

실패 거래 재처리

예약 시각 업무 실행



초보 개발자는 대량 데이터를 다음과 같이 처리하기 쉽다.

\`\`\`java id="bat30002"
List<Customer> customers =
customerMapper.selectAllCustomers();

for (Customer customer : customers) {
process(customer);
}

파일도 다음처럼 처리하기 쉽다.

\`\`\`java id=“bat30003” byte\[\] content = multipartFile.getBytes();

Files.write(path, content);



다운로드:

\`\`\`java id="bat30004"
byte\[\] content =
Files.readAllBytes(path);

return ResponseEntity.ok(content);

소량 데이터에서는 동작할 수 있다.

그러나 데이터가 커지면 다음 문제가 발생한다.

\`\`\`text id=“bat30005” 전체 데이터가 JVM Heap에 올라간다.

GC가 증가한다.

OutOfMemoryError가 발생한다.

Tomcat Thread가 장시간 점유된다.

DB Connection과 Row Lock이 오래 유지된다.

요청 Timeout이 발생한다.

중간 실패 후 처음부터 다시 처리한다.

같은 Job이 여러 서버에서 동시에 실행된다.

일부 데이터만 반영됐지만 성공처럼 보인다.

입력과 출력 건수가 맞지 않는다.



대용량 처리는 단순히 “더 큰 데이터를 처리하는 온라인 거래”가 아니다.

\`\`\`text id="bat30006"
온라인 거래
→ 사용자가 결과를 기다린다.

Batch
→ 일정 단위로 처리하고
상태·진행위치·재처리를 관리한다.

Scheduler
→ Batch를 언제 시작할지를 결정한다.

파일 처리
→ Binary 전송·저장·무결성·보안을 관리한다.

Scheduler와 Batch는 같은 개념이 아니다.

\`\`\`text id=“bat30007” Scheduler = 언제 실행할 것인가

Batch = 무엇을 어떤 단위와 상태로 처리할 것인가



\`@Scheduled\` 메서드가 예외 없이 종료됐다고 Batch 업무가 성공한 것은 아니다.

\`\`\`text id="bat30008"
Scheduler Trigger 성공

Batch Job 시작

1,000,000건 중
850,000건 성공
100,000건 실패
50,000건 미처리

최종 업무 결과
PARTIAL 또는 FAIL

따라서 Batch 완료는 다음과 같이 판단해야 한다.

\`\`\`text id=“bat30009” Scheduler 실행 여부

-   Job 실행상태
-   Chunk 처리결과
-   실패 건수
-   재처리 상태
-   입력·출력 대사
-   데이터 정합성



원본 가이드도 대용량 처리는 전체 데이터를 메모리에 적재하지 않고 일정 단위로 처리하며, Chunk 크기·Checkpoint·실패 레코드·중복 실행 Lock·입출력 대사를 완료조건으로 둘 것을 요구한다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 NSIGHT TCF에서 파일·대용량 처리·Batch·Scheduler를 안전하게 설계·개발·운영하기 위한 표준을 정의하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="bat30010"
온라인과 Batch 처리경계 구분

파일 업·다운로드 진입점 분리

대형 파일의 Streaming 처리

메모리·Thread·DB Pool 사용량 통제

Chunk 기반 대량 데이터 처리

Checkpoint·재시작·재처리 구조 정의

실패 레코드 격리와 PARTIAL 상태관리

입력·출력·금액 대사

Scheduler와 Job 책임 분리

다중 인스턴스 중복 실행 방지

수동 실행·재실행의 감사통제

파일명·경로·MIME·확장자 검증

악성코드·무결성·암호화 통제

보관·삭제·파기·복구 관리

Batch Metric·알림·Runbook 정의

자동검증과 품질 Gate 적용

## 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 파일 업로드 | Multipart·Streaming·분할 업로드 |
| 파일 다운로드 | Streaming·Range·대용량 |
| 파일 저장 | NAS·File System·Object Storage |
| 파일 메타 | 파일 ID·크기·Hash·상태 |
| 온라인 대량조회 | 페이징·Cursor·Streaming |
| 업무 Batch | 추출·집계·변환·발송 |
| 운영 Batch | AP·DB·배포·세션 상태수집 |
| 정리 Batch | 보존기간·삭제·Archive |
| 재처리 Batch | 실패·UNKNOWN·DLQ |
| Scheduler | Cron·Fixed Delay·수동 실행 |
| 실행 통제 | Lock·Leader·동시성 |
| 상태관리 | Job·Step·Chunk·Item |
| 운영관리 | OM 등록·실행·중지·이력 |
| 감사 | 파일 다운로드·수동 Batch·재처리 |
| 관측성 | 처리량·실패·지연·적체 |

## 대상 독자

\`\`\`text id=“bat30011” 업무 개발자

Batch 개발자

파일 연계 개발자

프레임워크 개발자

애플리케이션 아키텍트

데이터 아키텍트·DBA

인프라·스토리지 담당자

보안 담당자

운영·관제 담당자

QA·성능 테스트 담당자

DevOps·배포 담당자

PMO·업무 책임자


\## 선행조건

\`\`\`text id="bat30012"
업무 데이터 규모

파일 최대 크기

일일 증가량

Batch 완료시간

온라인 Peak 시간

입력·출력 건수

재처리 허용범위

업무 키·멱등성

파일 보안등급

보관·파기기간

WAS·Batch 서버 구성

DB Pool·Thread·Heap 용량

RTO·RPO

# 핵심 관점

\`\`\`text id=“bat30013” 대용량 처리의 핵심은 한 번에 많이 처리하는 것이 아니다.

제한된 메모리와 Connection 안에서 작은 단위로 반복 처리하고,

중간 실패가 발생해도 정확한 위치에서 안전하게 재시작하며,

최종 건수와 금액의 정합성을 증명할 수 있어야 한다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 온라인 거래와 Batch를 구분한다. |
| 2 | Scheduler와 Batch Job을 구분한다. |
| 3 | 대용량 데이터를 전체 List로 조회하지 않는다. |
| 4 | 파일을 \`byte\[\]\` 전체 적재하지 않고 Streaming한다. |
| 5 | Heap·Thread·DB Pool 기반 처리량을 산정한다. |
| 6 | 파일 크기 등급별 처리방식을 선택한다. |
| 7 | WAS Relay와 직접 Storage 전송을 비교한다. |
| 8 | Chunk·Page·Fetch Size의 차이를 설명한다. |
| 9 | Chunk 단위 Transaction을 설계한다. |
| 10 | Checkpoint와 Job 상태를 저장한다. |
| 11 | 실패 이후 정확한 위치에서 재시작한다. |
| 12 | Retry·Skip·Fail 정책을 구분한다. |
| 13 | 입력·성공·실패·미처리 건수를 대사한다. |
| 14 | 동일 Item의 재처리를 멱등하게 구현한다. |
| 15 | 다중 서버에서 Scheduler 중복 실행을 방지한다. |
| 16 | DB Lock·Leader·분산 Lock을 비교한다. |
| 17 | 수동 실행과 자동 실행의 충돌을 방지한다. |
| 18 | Cron·Fixed Rate·Fixed Delay를 구분한다. |
| 19 | Misfire와 장기 실행 Job을 처리한다. |
| 20 | 파일명과 Storage Key를 분리한다. |
| 21 | 파일 크기·확장자·MIME·Magic Byte를 검증한다. |
| 22 | 악성코드 검사와 격리구역을 설계한다. |
| 23 | SHA-256 Hash로 파일 무결성을 확인한다. |
| 24 | 파일 메타와 물리 파일의 정합성을 관리한다. |
| 25 | 다운로드 권한과 감사로그를 적용한다. |
| 26 | 파일 보관·논리삭제·물리파기를 관리한다. |
| 27 | Batch Metric·Dashboard·Alert를 정의한다. |
| 28 | Batch 장애·재시작·중복·부분성공을 테스트한다. |
| 29 | 현재 구현과 목표 구조의 차이를 설명한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| Batch | 대량 데이터를 일정 단위로 처리하는 작업 |
| Scheduler | 정해진 조건과 시각에 Job을 시작하는 구성요소 |
| Job | 하나의 Batch 업무 실행 정의 |
| Job Instance | 업무일자·Parameter로 구분되는 논리 실행 |
| Job Execution | Job Instance의 실제 실행 한 번 |
| Step | Job 내부의 처리 단계 |
| Chunk | 한 Transaction으로 처리하는 Item 묶음 |
| Item | Batch에서 처리하는 최소 업무 단위 |
| Page Size | DB에서 한 번에 조회하는 건수 |
| Fetch Size | JDBC가 DB에서 가져오는 행 묶음 |
| Commit Interval | 한 번 Commit하는 Item 수 |
| Checkpoint | 재시작을 위한 마지막 완료 위치 |
| Retry | 같은 Item을 다시 처리 |
| Skip | 실패 Item을 격리하고 다음 Item을 처리 |
| Restart | 중단된 Job을 Checkpoint에서 다시 시작 |
| Rerun | 동일하거나 새로운 Parameter로 Job 전체를 다시 실행 |
| Reconciliation | 입력·출력·건수·금액 대조 |
| PARTIAL | 일부 성공·일부 실패 상태 |
| Job Lock | 같은 Job의 중복 실행을 막는 잠금 |
| Lease | 일정 시간 동안 유효한 분산 Lock |
| Misfire | 예정 시각에 Scheduler가 실행되지 못한 상태 |
| Streaming | 전체 데이터를 메모리에 적재하지 않고 순차 처리 |
| Multipart | HTTP에서 파일과 Form 데이터를 함께 전송하는 방식 |
| Range Request | 파일 일부 구간을 요청하는 HTTP 방식 |
| Resumable Upload | 중단 위치부터 이어서 업로드하는 방식 |
| Quarantine | 검사 완료 전 파일을 격리 보관하는 영역 |
| Magic Byte | 파일 형식을 나타내는 실제 Binary Header |
| Checksum | 파일 무결성을 검증하는 Hash |
| Storage Key | 물리 저장소에서 파일을 식별하는 내부 이름 |
| Original Filename | 사용자가 업로드한 표시용 파일명 |

\---

\# 전체 목표 아키텍처

\`\`\`text id="bat30014"
┌───────────────────────────────────────────────────┐
│ 온라인 영역 │
│ │
│ 사용자 │
│ ↓ │
│ Gateway·인증·권한 │
│ ↓ │
│ 파일 접수 API │
│ ├─ 파일 크기·이름·MIME 검증 │
│ ├─ Upload Session 생성 │
│ ├─ Streaming Storage │
│ └─ 접수상태 반환 │
└───────────────────────┬───────────────────────────┘
▼
┌───────────────────────────────────────────────────┐
│ 파일·Batch 상태 저장 │
│ │
│ FILE\_META │
│ BATCH\_JOB │
│ BATCH\_EXECUTION │
│ BATCH\_STEP │
│ BATCH\_CHECKPOINT │
│ BATCH\_ERROR\_ITEM │
│ BATCH\_LOCK │
└───────────────────────┬───────────────────────────┘
▼
┌───────────────────────────────────────────────────┐
│ Batch 실행영역 │
│ │
│ Scheduler·수동 실행 │
│ ↓ │
│ Lock 획득 │
│ ↓ │
│ Reader → Processor → Writer │
│ ↓ │
│ Chunk Commit·Checkpoint │
│ ↓ │
│ 대사·완료판정 │
└───────────────────────┬───────────────────────────┘
▼
┌───────────────────────────────────────────────────┐
│ 운영·감사 │
│ │
│ OM Batch 관리 │
│ 파일 관리 │
│ 실패 Item·재처리 │
│ Metric·Alert │
│ 다운로드·수동 실행 감사 │
└───────────────────────────────────────────────────┘

# 현재 구현과 목표 구조

## 현재 tcf-batch에서 확인되는 구조

현재 tcf-batch는 OM 대시보드에 표시할 운영 상태를 주기적으로 수집하는 **수집 전용 Batch 애플리케이션**이다.

현재 확인되는 Job:

| Job ID | 기능 | 저장 대상 |
| --- | --- | --- |
| BAT-BATCH-001 | AP 상태 수집 | OM\_AP\_STATUS |
| BAT-BATCH-002 | DB 상태 수집 | OM\_DB\_STATUS |
| BAT-BATCH-004 | 배포 상태 수집 | OM\_DEPLOY\_STATUS |

설계문서에는 세션 상태 수집 BAT-BATCH-003도 정의돼 있으나 현재 분석한 기준 소스 트리에서는 관련 Scheduler·Service가 확인되지 않아 현재 Branch 구현 여부를 재확인해야 한다.

현재 실행 방식:

\`\`\`text id=“bat30015” @Scheduled

-   기동 시 초기 수집
-   수동 REST API
-   OM 원격 실행



현재 결과 상태:

\`\`\`text id="bat30016"
SUCCESS

PARTIAL

FAIL

현재 저장 방식:

\`\`\`text id=“bat30017” 대상별 상태 → MERGE Upsert

실행 결과 → OM\_BATCH\_HISTORY INSERT



좋은 점:

\`\`\`text id="bat30018"
대상별 실패를 격리한다.

일부 실패 시 PARTIAL을 사용한다.

실패 대상도 FAIL Snapshot으로 저장한다.

수동 실행 경로가 있다.

수집 결과와 실행 이력을 분리한다.

기동 Warm-up 동안 Scheduler 실행을 지연한다.

## 현재 Batch 구현의 한계

현재 tcf-batch는 대량 업무 데이터를 Chunk로 처리하는 범용 Batch Framework라기보다 소수 대상의 상태를 순회하여 Upsert하는 수집 Batch다.

현재 코드에서 명시적으로 확인되지 않는 기능:

\`\`\`text id=“bat30019” Chunk Reader·Processor·Writer

Item별 Checkpoint

Job Instance Parameter

실패 Item Table

Retry·Skip 정책

중단·재시작

DB Job Lock

다중 Instance 중복실행 방지

Job 의존관계

대량 입력·출력 대사

Job 중지 요청

Misfire 처리



따라서 향후 고객추출·집계·파일처리·대량발송 같은 업무 Batch에는 별도의 표준 Batch 실행모델이 필요하다.

현재 아키텍처 점검기준도 Scheduler 중복실행 방지, Batch 시간창, 자원분리, Checkpoint·재처리, 대용량 파일 처리방식을 독립 검토항목으로 요구한다.

\---

\## 현재 파일 업·다운로드 구현

현재 파일 기능은 \`tcf-om\`의 \`/ud/files\` REST API와 \`tcf-ui\` Relay로 구성돼 있다.

\`\`\`text id="bat30020"
Browser

→ tcf-ui Relay

→ tcf-om /ud/files

→ UD\_FILE\_META

→ Local Disk

현재 확인 기능:

\`\`\`text id=“bat30021” Multipart Upload

파일 최대 크기 50MB

파일 메타 저장

파일 목록·상세

설명 수정

논리삭제

물리 파일 삭제

Binary Download

다운로드 감사로그



현재 물리 파일명:

\`\`\`text id="bat30022"
{UUID}.bin

사용자 원본 파일명과 물리 경로를 분리하는 방향은 적절하다.

## 현재 파일 구현의 주요 위험

### 전체 메모리 적재

현재 업로드:

java id="bat30023" file.getBytes()

현재 저장:

java id="bat30024" Files.write(path, byteArray)

현재 다운로드:

java id="bat30025" Files.readAllBytes(path)

현재 응답:

java id="bat30026" ResponseEntity<byte\[\]>

UI Relay도 Binary를 byte\[\]로 받아 다시 브라우저에 전달한다.

따라서 파일 50MB 한 건이 다음과 같이 여러 메모리 복사본을 만들 수 있다.

\`\`\`text id=“bat30027” 브라우저 요청 수신 Buffer

Multipart 임시 메모리·디스크

tcf-ui byte\[\]

HTTP Client byte\[\]

tcf-om byte\[\]

Storage byte\[\]

응답 byte\[\]



동시 20건이면 수 GB 수준의 순간 메모리 사용이 발생할 수 있다.

\### 인증 사용자 신뢰

현재 Upload·Download API가 \`userId\`를 Request Parameter로 받는다.

\`\`\`text id="bat30028"
?userId=U123456

사용자가 Parameter를 바꿔 다른 사용자로 감사로그를 남길 수 있으므로, 인증된 AuthenticationContext에서 사용자 ID를 가져와야 한다.

### 파일 보안

현재 코드에서 별도 확인되지 않는 기능:

\`\`\`text id=“bat30029” 확장자 Allow List

Magic Byte 검사

악성코드 검사

SHA-256

암호화

Quarantine

다운로드 업무권한

Range Download

재개 업로드

Storage 용량 Quota

보관·파기 Scheduler


\### 메타·파일 정합성

업로드:

\`\`\`text id="bat30030"
물리 파일 저장 성공

DB INSERT 실패
→ Orphan File 가능

삭제:

\`\`\`text id=“bat30031” DB USE\_YN=‘N’

물리 파일 삭제 실패 → 메타는 삭제 → 파일은 잔존



DB와 File System은 하나의 ACID Transaction으로 묶이지 않으므로 상태머신과 보상·정리 Batch가 필요하다.

\---

\## 목표 개선방향

| 현재 | 목표 |
|---|---|
| \`byte\[\]\` 전체 적재 | InputStream·OutputStream Streaming |
| 50MB 단순 Upload | 크기 등급별 전송전략 |
| tcf-ui Binary Relay | 직접 Storage·Nginx·전용 File Gateway |
| Local Disk | 공유 NAS·Object Storage 검토 |
| 파일 저장 후 DB INSERT | \`UPLOADING→AVAILABLE\` 상태 |
| DB 삭제 후 파일 삭제 | \`DELETE\_PENDING→DELETED\` 상태 |
| userId Parameter | 인증문맥 |
| MIME Header 신뢰 | 확장자·Magic Byte 검사 |
| Hash 없음 | SHA-256 |
| 악성코드 검사 없음 | Quarantine·Scan |
| 단순 \`@Scheduled\` | DB Lock·Job 상태 |
| 실행 이력 중심 | Checkpoint·Error Item·대사 |
| 수동 API | 권한·사유·감사·중복통제 |

\---

\# 설계 원칙

\## 원칙 1. 온라인은 접수하고 Batch가 완료한다

수초 이상 걸리거나 대량 데이터를 처리하는 업무는 다음 구조를 권장한다.

\`\`\`text id="bat30032"
온라인 요청
↓
요청 검증
↓
Job 접수
↓
접수번호 반환
↓
Batch 처리
↓
상태조회·통보

사용자 HTTP Thread가 Batch 완료까지 기다리지 않는다.

## 원칙 2. 전체 데이터를 메모리에 올리지 않는다

\`\`\`text id=“bat30033” 전체 List 조회 금지

전체 파일 byte\[\] 금지

전체 결과 Excel Workbook 메모리 생성 금지


\## 원칙 3. 재실행보다 재시작을 우선한다

\`\`\`text id="bat30034"
재실행
→ 처음부터 전체 처리

재시작
→ 마지막 완료 Checkpoint부터 처리

대규모 Job은 재시작 가능하게 설계한다.

## 원칙 4. Item 처리는 멱등해야 한다

같은 Item이 다시 실행돼도 중복 결과가 생기지 않아야 한다.

## 원칙 5. Scheduler 성공과 업무 성공을 분리한다

Scheduler는 Job을 시작했을 뿐이다.

최종 결과는 Job·Step·대사 상태로 판단한다.

## 원칙 6. 파일의 원본 이름을 물리 경로에 사용하지 않는다

\`\`\`text id=“bat30035” Original Filename 보고서.xlsx

Storage Key 01JX…UUID.bin


\## 원칙 7. 파일은 검사가 끝나기 전 사용할 수 없다

\`\`\`text id="bat30036"
UPLOADING

→ UPLOADED

→ SCANNING

→ AVAILABLE

# 대안 비교 및 의사결정

## 대용량 처리방식

| 방식 | 적합 | 장점 | 위험 | 판단 |
| --- | --- | --- | --- | --- |
| 온라인 동기 | 소량·즉시 결과 | 단순 | Timeout·자원점유 | 제한 |
| 비동기 Job | 중·대량 | 장애격리 | 상태관리 | 기본 |
| Batch DB 처리 | 대량 Table | 효율적 | DB 부하 | 기본 |
| 파일 Batch | 대량 송수신 | 매우 큰 데이터 | 대사 필요 | 권장 |
| Message Item | 독립 Item | 확장성 | 순서·중복 | 조건부 |
| DB Procedure | DB 중심 집계 | 빠름 | Logic 종속 | 승인 필요 |

## 파일 전송방식

| 방식 | 적용 크기 | 장점 | 위험 | 판단 |
| --- | --- | --- | --- | --- |
| byte\[\] | 매우 소량 | 단순 | Heap 사용 | 소형 한정 |
| WAS Streaming | 중형 | 표준 HTTP | Thread 점유 | 기본 |
| Range Download | 대형 | 재개 가능 | 구현 필요 | 권장 |
| Resumable Upload | 대형 | 중단 복구 | 상태 복잡 | 대형 권장 |
| Direct Storage | 매우 대형 | WAS 부하 감소 | 권한·URL 관리 | 중장기 |
| SFTP·전용 연계 | 기관 파일 | 운영통제 | 실시간성 낮음 | 기관연계 |

## Scheduler 실행통제

| 방식 | 다중 서버 | 장점 | 문제 | 판단 |
| --- | --- | --- | --- | --- |
| 단순 @Scheduled | 중복 실행 | 간단 | 중복 | 단일 Instance만 |
| 지정 Node 실행 | 제한 | 단순 | 장애 시 미실행 | 제한 |
| DB Lock | 가능 | DB 기반 | DB 의존 | 기본 권장 |
| 분산 Lock | 가능 | 확장 | 별도 인프라 | 조건부 |
| 외부 Scheduler | 가능 | 중앙통제 | 도구 의존 | 기업 표준 시 권장 |
| Leader Election | 가능 | 자동 | 복잡 | 플랫폼형 |

# 30.1 메모리와 처리량 기준

## 30.1.1 메모리 사용량은 파일 크기와 같지 않다

파일 50MB를 byte\[\]로 처리한다고 Heap 50MB만 사용하는 것은 아니다.

\`\`\`text id=“bat30037” 원본 byte\[\]

HTTP Buffer

Framework Buffer

복사본

응답 Buffer

객체 Overhead



보수적으로 동시 처리량을 계산해야 한다.

\`\`\`text id="bat30038"
예상 Heap
\=
파일 크기
× 동시 처리 수
× 메모리 복사 계수

예:

\`\`\`text id=“bat30039” 파일 50MB

동시 업로드 20건

복사 계수 3

예상 3GB



JVM Heap 4GB 환경에서는 매우 위험하다.

\---

\## 30.1.2 파일 크기 등급

예시 기준:

| 등급 | 크기 | 권장 처리 |
|---|---:|---|
| XS | 1MB 이하 | 일반 Multipart 가능 |
| S | 1~20MB | Streaming |
| M | 20~200MB | Streaming·동시성 제한 |
| L | 200MB~2GB | 분할·재개·비동기 |
| XL | 2GB 이상 | 직접 Storage·전용 전송 |
| 초대형 | 수십·수백 GB | WAS 경유 금지·파일 연계 |

값은 인프라·네트워크·업무요구로 확정한다.

\---

\## 30.1.3 Streaming Upload

\`\`\`java id="bat30040"
public FileStorageResult store(
String storageKey,
InputStream inputStream,
long declaredSize) {

try (InputStream in =
new BufferedInputStream(inputStream);
OutputStream out =
Files.newOutputStream(
resolvePath(storageKey),
StandardOpenOption.CREATE\_NEW)) {

MessageDigest digest =
MessageDigest.getInstance("SHA-256");

byte\[\] buffer = new byte\[64 \* 1024\];
long written = 0;
int read;

while ((read = in.read(buffer)) != -1) {
written += read;

if (written > maxFileSizeBytes) {
throw new FileSizeExceededException();
}

digest.update(buffer, 0, read);
out.write(buffer, 0, read);
}

return new FileStorageResult(
written,
HexFormat.of()
.formatHex(digest.digest())
);
}
}

파일 전체를 Heap에 적재하지 않는다.

## 30.1.4 Streaming Download

\`\`\`java id=“bat30041” @GetMapping(“/{fileId}/download”) public ResponseEntity download( @PathVariable String fileId) {

FileDownloadDescriptor descriptor =
downloadService.authorize(fileId);

StreamingResponseBody body =
outputStream ->
storageService.copyTo(
descriptor.storageKey(),
outputStream
);

return ResponseEntity.ok()
.contentType(
descriptor.mediaType()
)
.contentLength(
descriptor.fileSize()
)
.header(
HttpHeaders.CONTENT\_DISPOSITION,
descriptor.contentDisposition()
)
.body(body);

}



주의:

\`\`\`text id="bat30042"
Streaming도 Tomcat Thread·Connection과
Network Bandwidth를 사용한다.

동시 다운로드 상한과 별도 Executor를 관리한다.

## 30.1.5 Relay의 메모리 증폭

현재 구조:

\`\`\`text id=“bat30043” Browser

→ tcf-ui byte\[\]

→ tcf-om byte\[\]

→ File



Relay를 거치면 Upload·Download 데이터가 두 애플리케이션을 통과한다.

대안:

\`\`\`text id="bat30044"
Browser
→ File Gateway·Storage

업무 WAR
→ 권한·Metadata·Download Token만 발급

또는 Apache·Nginx의 효율적인 File Transfer 기능을 사용한다.

## 30.1.6 대형 다운로드 Range

HTTP 요청:

http id="bat30045" Range: bytes=1048576-2097151

응답:

http id="bat30046" 206 Partial Content Content-Range: bytes 1048576-2097151/10485760

장점:

\`\`\`text id=“bat30047” 중단 후 재개

사용자 일부 구간 요청

대형 파일 안정성


\---

\## 30.1.7 분할 업로드

\`\`\`text id="bat30048"
Upload Session 생성

Chunk 1 업로드

Chunk 2 업로드

...

Chunk N 업로드

전체 Hash 검증

파일 조립

악성코드 검사

AVAILABLE

필수 식별자:

\`\`\`text id=“bat30049” uploadId

chunkNo

totalChunks

chunkSize

chunkHash

fullFileHash


\---

\## 30.1.8 온라인 대량조회

금지:

\`\`\`sql id="bat30050"
SELECT \*
FROM SV\_CUSTOMER
ORDER BY CUSTOMER\_NO

그리고 Java에서 전체 List 적재.

대안:

\`\`\`text id=“bat30051” Keyset Paging

Cursor

Streaming Result

Batch Page Reader

DB Export

파일 생성 Job


\---

\## 30.1.9 Offset Paging과 Keyset Paging

Offset:

\`\`\`sql id="bat30052"
OFFSET 900000 ROWS
FETCH NEXT 1000 ROWS ONLY

깊은 페이지에서 비용이 증가할 수 있다.

Keyset:

sql id="bat30053" WHERE CUSTOMER\_NO > :lastCustomerNo ORDER BY CUSTOMER\_NO FETCH FIRST 1000 ROWS ONLY

Checkpoint에도 마지막 업무 키를 사용할 수 있다.

## 30.1.10 Page·Fetch·Chunk

| 항목 | 의미 | 예 |
| --- | --- | --- |
| Page Size | SQL 한 번의 조회 건수 | 1,000 |
| Fetch Size | JDBC 전송 묶음 | 500 |
| Chunk Size | 한 Transaction 처리 건수 | 500 |
| Batch Size | JDBC Batch DML 묶음 | 200 |

모두 같은 숫자일 필요는 없다.

## 30.1.11 Chunk 크기 결정

Chunk가 너무 작으면:

\`\`\`text id=“bat30054” Commit 횟수 증가

DB Round Trip 증가

이력·Checkpoint 증가



Chunk가 너무 크면:

\`\`\`text id="bat30055"
Transaction 장기화

Lock 증가

Rollback 비용 증가

Heap 증가

재처리 범위 증가

측정항목:

\`\`\`text id=“bat30056” Chunk 처리시간

메모리

SQL 시간

Commit 시간

Lock

Rollback 시간

DB Redo·Undo


\---

\## 30.1.12 처리량 계산

\`\`\`text id="bat30057"
필요 TPS
\=
전체 건수
÷ 완료 허용시간

예:

\`\`\`text id=“bat30058” 1,000,000건

2시간 7,200초

필요 평균 약 139건/초



안전여유와 실패·재처리를 포함해 목표를 높게 잡는다.

\---

\## 30.1.13 병렬 처리량

\`\`\`text id="bat30059"
총 처리량
≈ Worker 수
× Worker당 처리량

그러나 Worker를 늘린 만큼 무조건 빨라지지 않는다.

병목:

\`\`\`text id=“bat30060” DB CPU

DB Connection

Index 경합

Network

외부 Rate Limit

Storage IOPS

Row Lock


\---

\## 30.1.14 Parallel Partition

안전한 분할 기준:

\`\`\`text id="bat30061"
지점코드

고객번호 Hash

업무일자

Partition Key

ID 범위

금지:

text id="bat30062" 같은 고객·같은 계좌를 여러 Worker가 동시에 변경

## 30.1.15 Batch 자원분리

온라인과 같은 JVM·Pool을 사용할 경우:

\`\`\`text id=“bat30063” Batch 대량 SQL

→ Pool 점유

→ 온라인 Connection 대기

→ p95 증가



대안:

\`\`\`text id="bat30064"
Batch 전용 WAR

Batch 전용 JVM

Batch 전용 DataSource·Pool

DB Resource Manager

Batch 시간창

동시 Worker 상한

## 30.1.16 파일 저장소 용량

text id="bat30065" 필요 용량 = 일 업로드량 × 보존일 × 복제본 × 여유율

예:

\`\`\`text id=“bat30066” 일 100GB

30일

2중 복제

20% 여유

약 7.2TB



임시파일·검사본·Backup도 포함한다.

\---

\## 30.1.17 온라인 파일 Timeout

파일 전송시간:

\`\`\`text id="bat30067"
전송시간
≈ 파일크기 ÷ 실효대역폭

1GB 파일을 20Mbps로 전송하면 수분이 걸릴 수 있다.

일반 온라인 TCF Timeout 3~5초로 처리할 수 없다.

파일 API는 별도 Timeout·Streaming·재개 정책을 사용한다.

# 30.2 Chunk·체크포인트·재처리

## 30.2.1 Job 구조

\`\`\`text id=“bat30068” Job

├─ Step 1 입력 검증

├─ Step 2 데이터 처리

├─ Step 3 파일 생성·전송

└─ Step 4 대사·완료


\---

\## 30.2.2 Job Instance

동일 Job이라도 업무일자·파일 ID가 다르면 별도 Instance다.

\`\`\`text id="bat30069"
jobName
CM\_TARGET\_EXTRACT

businessDate
2026-07-18

campaignId
CMP-001

논리 키:

text id="bat30070" CM\_TARGET\_EXTRACT + 2026-07-18 + CMP-001

## 30.2.3 Job 상태

\`\`\`text id=“bat30071” REQUESTED

READY

RUNNING

STOP\_REQUESTED

STOPPED

SUCCESS

PARTIAL

FAIL

RETRY\_WAIT

RESTARTING

CANCELLED

UNKNOWN


\---

\## 30.2.4 Step 상태

\`\`\`text id="bat30072"
NOT\_STARTED

RUNNING

SUCCESS

FAIL

SKIPPED

STOPPED

## 30.2.5 Chunk Transaction

\`\`\`text id=“bat30073” Reader 500건 조회

Processor 업무 변환

Writer 500건 저장

Checkpoint 저장

Commit



다음 Chunk는 별도 Transaction이다.

\---

\## 30.2.6 Checkpoint 항목

\`\`\`text id="bat30074"
Job Execution ID

Step ID

Partition ID

마지막 완료 Key

Page No

Read Count

Write Count

Skip Count

Chunk No

업무일자

Updated At

## 30.2.7 Checkpoint 기준

좋은 기준:

\`\`\`text id=“bat30075” 정렬이 안정적이다.

중간 데이터 추가에도 중복·누락이 없다.

업무 키가 유일하다.

재시작 후 같은 범위를 찾을 수 있다.



금지:

\`\`\`text id="bat30076"
OFFSET Page 번호만 저장

정렬조건 없음

현재시간을 기준으로 다음 범위 결정

## 30.2.8 Checkpoint Table

sql id="bat30077" CREATE TABLE TCF\_BATCH\_CHECKPOINT ( EXECUTION\_ID VARCHAR2(64) NOT NULL, STEP\_ID VARCHAR2(100) NOT NULL, PARTITION\_ID VARCHAR2(100) NOT NULL, LAST\_PROCESSED\_KEY VARCHAR2(500), CHUNK\_NO NUMBER(18) NOT NULL, READ\_COUNT NUMBER(18) NOT NULL, WRITE\_COUNT NUMBER(18) NOT NULL, SKIP\_COUNT NUMBER(18) NOT NULL, CHECKPOINT\_DATA CLOB, UPDATED\_AT TIMESTAMP NOT NULL, CONSTRAINT PK\_TCF\_BATCH\_CHECKPOINT PRIMARY KEY ( EXECUTION\_ID, STEP\_ID, PARTITION\_ID ) );

## 30.2.9 Checkpoint 저장시점

\`\`\`text id=“bat30078” 업무 데이터 Write

-   Checkpoint Update

→ 같은 Transaction



업무 데이터는 Commit됐는데 Checkpoint가 저장되지 않으면 재시작 시 중복 처리될 수 있다.

\---

\## 30.2.10 Item 멱등성

대안:

\`\`\`text id="bat30079"
업무 Unique Constraint

MERGE Upsert

처리이력 Unique

Idempotency Key

Version·상태조건

이미 처리됨 상태확인

현재 운영 상태수집 Batch의 MERGE Upsert는 같은 대상을 다시 수집해도 최신 Snapshot으로 갱신된다는 점에서 멱등한 저장 패턴이다.

## 30.2.11 Retry

Retry 대상:

\`\`\`text id=“bat30080” 일시 DB Deadlock

순간 Network 오류

일시 외부 503

짧은 Lock 충돌



Retry 금지:

\`\`\`text id="bat30081"
입력 형식 오류

필수값 누락

업무상 처리불가

SQL 문법 오류

파일 손상

권한 오류

## 30.2.12 Retry 정책

\`\`\`text id=“bat30082” 최대 횟수

Backoff

Jitter

Retryable 오류

Item 멱등성

전체 Job 종료시간


\---

\## 30.2.13 Skip

Skip은 실패를 숨기는 기능이 아니다.

\`\`\`text id="bat30083"
잘못된 Item 격리

Error Table 저장

실패사유 기록

재처리 가능 상태

Skip 상한

Skip 상한을 넘으면 Job을 FAIL 처리한다.

## 30.2.14 실패 Item Table

sql id="bat30084" CREATE TABLE TCF\_BATCH\_ERROR\_ITEM ( ERROR\_ITEM\_ID VARCHAR2(64) NOT NULL, EXECUTION\_ID VARCHAR2(64) NOT NULL, STEP\_ID VARCHAR2(100) NOT NULL, ITEM\_KEY VARCHAR2(500) NOT NULL, ERROR\_CODE VARCHAR2(50) NOT NULL, ERROR\_MESSAGE VARCHAR2(1000), RETRYABLE\_YN CHAR(1) NOT NULL, RETRY\_COUNT NUMBER(5) NOT NULL, ITEM\_PAYLOAD\_REF VARCHAR2(500), ERROR\_STATUS VARCHAR2(30) NOT NULL, CREATED\_AT TIMESTAMP NOT NULL, UPDATED\_AT TIMESTAMP NOT NULL, CONSTRAINT PK\_TCF\_BATCH\_ERROR\_ITEM PRIMARY KEY (ERROR\_ITEM\_ID) );

민감한 Item Payload 전체를 Error Table에 저장하지 않는다.

## 30.2.15 PARTIAL

\`\`\`text id=“bat30085” 전체 10,000건

성공 9,980건

실패 20건

정책 PARTIAL



PARTIAL 허용 여부는 업무마다 다르다.

| 업무 | PARTIAL |
|---|:---:|
| 운영상태 수집 | 허용 |
| 광고 발송 | 허용 가능 |
| 고객 원장 이관 | 원칙적 금지 |
| 금액 정산 | 원칙적 금지 |
| 단순 파일 변환 | 조건부 |
| 통계 집계 | 대사 후 판단 |

\---

\## 30.2.16 재시작

\`\`\`text id="bat30086"
Execution 1

Chunk 1~100 Commit

Chunk 101 실패

Checkpoint
100

재시작
→ Chunk 101부터

## 30.2.17 재실행

다음 경우 전체 재실행할 수 있다.

\`\`\`text id=“bat30087” 출력 Table을 업무일자 단위로 삭제 가능

모든 Item이 멱등함

새 Job Instance로 재생성

기존 결과와 명확히 구분


\---

\## 30.2.18 재처리

실패 Item만 다시 처리한다.

\`\`\`text id="bat30088"
ERROR\_ITEM

→ 승인

→ RETRY\_WAIT

→ 재처리

→ RESOLVED

수동 재처리는 사유·실행자·결과를 감사한다.

## 30.2.19 재처리 중 원본 변경

실패 후 원본 데이터가 변경될 수 있다.

정책을 선택한다.

\`\`\`text id=“bat30089” 실패 당시 Snapshot 재처리

또는

현재 원본 기준 재처리



금액·계약 업무에서는 기준시점이 중요하다.

\---

\## 30.2.20 입력·출력 대사

건수:

\`\`\`text id="bat30090"
입력
\=
성공
\+ 실패
\+ Skip
\+ 미처리

금액:

text id="bat30091" 입력 금액 = 출력 성공 금액 + 실패 금액 + 미처리 금액

파일:

\`\`\`text id=“bat30092” Header 건수

Detail 실제 건수

Trailer 건수

DB 반영 건수


\---

\## 30.2.21 대사 실패

\`\`\`text id="bat30093"
Job 처리 자체
SUCCESS

대사
FAIL

최종 Job
FAIL 또는 PARTIAL

대사 성공 전에는 최종 업무완료로 보지 않는다.

## 30.2.22 파일 처리 Checkpoint

\`\`\`text id=“bat30094” 파일 ID

Byte Offset

Line No

Record No

Chunk Hash

마지막 업무 키



CSV는 줄바꿈·인코딩·따옴표를 고려해 안전한 Parser 상태가 필요하다.

\---

\## 30.2.23 파일 중간 실패

\`\`\`text id="bat30095"
1,000,000번째 Line까지 Commit

1,000,001번째 Line 오류

대안:

\`\`\`text id=“bat30096” 파일 전체 Reject

오류 Line Skip

오류파일 생성

수정파일 재업로드

실패 Line만 재처리



업무 기준으로 확정한다.

\---

\## 30.2.24 Batch History

필수 항목:

\`\`\`text id="bat30097"
Job ID

Job Instance ID

Execution ID

Parameter

시작·종료시각

상태

Read·Write·Skip·Fail

입력·출력 금액

Checkpoint

실행 Node

실행자

재시작 원 Execution

오류코드

배포 Version

현재 OM\_BATCH\_HISTORY는 Job ID·실행시각·상태·처리시간·메시지를 저장하는 기본 구조다.

업무 Batch 적용 시 건수·금액·Checkpoint·실행 Node 등을 확장해야 한다.

# 30.3 중복 실행과 동시 실행 방지

## 30.3.1 단순 @Scheduled의 문제

업무 WAR가 4대이면:

\`\`\`text id=“bat30098” AP01 @Scheduled 실행

AP02 @Scheduled 실행

AP03 @Scheduled 실행

AP04 @Scheduled 실행



같은 Job이 4번 실행될 수 있다.

현재 \`tcf-batch\`를 한 Instance로만 운영한다면 문제가 줄지만, HA 구성 시 중복 실행방지 기능이 필요하다.

\---

\## 30.3.2 중복 실행 유형

\`\`\`text id="bat30099"
같은 Scheduler가 여러 Node에서 실행

이전 실행이 끝나기 전 다음 Cron 실행

자동 실행 중 운영자가 수동 실행

실패 후 Retry와 수동 재실행 충돌

동일 파일이 두 번 접수

동일 업무일자 Job이 중복 등록

## 30.3.3 Job Instance Unique

sql id="bat30100" CREATE UNIQUE INDEX UX\_BATCH\_INSTANCE ON TCF\_BATCH\_JOB\_INSTANCE ( JOB\_NAME, BUSINESS\_DATE, PARAMETER\_HASH );

같은 업무 Parameter로 중복 Instance 생성을 막는다.

## 30.3.4 DB Job Lock

sql id="bat30101" CREATE TABLE TCF\_BATCH\_LOCK ( LOCK\_NAME VARCHAR2(200) NOT NULL, LOCK\_OWNER VARCHAR2(200), LOCKED\_AT TIMESTAMP, LOCK\_UNTIL TIMESTAMP, HEARTBEAT\_AT TIMESTAMP, VERSION\_NO NUMBER(18) NOT NULL, CONSTRAINT PK\_TCF\_BATCH\_LOCK PRIMARY KEY (LOCK\_NAME) );

## 30.3.5 Lock 획득

개념:

sql id="bat30102" UPDATE TCF\_BATCH\_LOCK SET LOCK\_OWNER = :owner, LOCKED\_AT = SYSTIMESTAMP, LOCK\_UNTIL = :leaseUntil, VERSION\_NO = VERSION\_NO + 1 WHERE LOCK\_NAME = :lockName AND ( LOCK\_UNTIL IS NULL OR LOCK\_UNTIL < SYSTIMESTAMP );

영향 행 수:

\`\`\`text id=“bat30103” 1 → Lock 획득

0 → 다른 실행 중


\---

\## 30.3.6 Lease

Process가 비정상 종료되면 Lock을 해제하지 못할 수 있다.

\`\`\`text id="bat30104"
영구 Lock
→ Job 영구 미실행

따라서 LOCK\_UNTIL을 둔다.

장기 Job은 Heartbeat로 Lease를 연장한다.

## 30.3.7 Lease 위험

Lease가 Job 시간보다 짧고 Heartbeat가 실패하면:

\`\`\`text id=“bat30105” Job A 실행 중

Lease 만료

Job B Lock 획득

동시 실행



대안:

\`\`\`text id="bat30106"
충분한 Lease

Heartbeat

Fencing Token

DB 상태 재검증

## 30.3.8 Fencing Token

Lock 획득 때마다 증가하는 Version을 Writer 조건에 포함한다.

\`\`\`text id=“bat30107” Job A Token 10

Job B Token 11

Job A의 늦은 Write → Token 10 → 거절



강한 중복 방지가 필요한 업무에서 검토한다.

\---

\## 30.3.9 Lock 범위

\`\`\`text id="bat30108"
Job 전체

업무일자

Partition

파일 ID

캠페인 ID

모든 실행을 하나의 Global Lock으로 막으면 병렬처리가 불가능하다.

업무 충돌범위에 맞게 Lock Key를 설계한다.

## 30.3.10 동시 실행 정책

| 정책 | 의미 |
| --- | --- |
| FORBID | 실행 중이면 신규 실행 거절 |
| QUEUE | 기존 완료 후 실행 |
| REPLACE | 기존 중지 후 신규 실행 |
| ALLOW | Parameter·Partition이 다르면 허용 |
| MERGE | 같은 요청을 기존 실행에 연결 |

기본은 FORBID다.

## 30.3.11 자동·수동 실행

수동 실행 시 확인:

\`\`\`text id=“bat30109” 같은 Job 실행 중인가?

같은 업무일자·파일인가?

기존 실행은 어떤 상태인가?

재시작인가 신규 실행인가?

실행사유가 있는가?

승인권한이 있는가?


\---

\## 30.3.12 Scheduler 종류

\### Cron

\`\`\`text id="bat30110"
매일 02:00

업무시각 기준 실행.

### Fixed Rate

text id="bat30111" 시작시각 기준 5분마다

이전 실행이 길면 겹칠 수 있다.

### Fixed Delay

text id="bat30112" 이전 실행 종료 후 5분

겹침은 줄지만 실제 실행시각이 밀린다.

## 30.3.13 Cron Timezone

java id="bat30113" @Scheduled( cron = "${batch.cron}", zone = "Asia/Seoul" )

서버 OS Timezone에 암묵적으로 의존하지 않는다.

## 30.3.14 공휴일·영업일

Cron만으로 금융 영업일을 완전히 표현하기 어렵다.

\`\`\`text id=“bat30114” Scheduler → 매일 Trigger

Business Calendar → 영업일 여부 판단

휴일 → SKIPPED 이력


\---

\## 30.3.15 Misfire

예정시각에 서버가 중단됐다가 복구된 경우:

\`\`\`text id="bat30115"
즉시 실행

다음 일정까지 대기

누락 이력만 생성

운영 승인 후 실행

중 무엇을 선택할지 Job별로 정의한다.

## 30.3.16 장기 실행과 다음 일정

\`\`\`text id=“bat30116” 02:00 Job 시작

04:00 다음 실행시각

기존 Job 아직 실행 중



정책:

\`\`\`text id="bat30117"
중복 금지

신규 실행 SKIPPED

기존 실행 지연 Alert

## 30.3.17 Job 의존관계

\`\`\`text id=“bat30118” 고객 추출

→ 캠페인 대상 생성

→ 파일 생성

→ 외부 전송



앞 단계가 성공하기 전 다음 Job을 실행하지 않는다.

Cron 시각만 다르게 설정해 의존성을 표현하지 않는다.

\---

\## 30.3.18 Scheduler와 OM

권장 역할:

\`\`\`text id="bat30119"
OM
→ Job Catalog
→ Schedule
→ 사용여부
→ 수동 실행
→ 중지
→ 이력
→ 재처리 승인

tcf-batch
→ 실제 실행
→ Lock
→ Checkpoint
→ 상태 저장

## 30.3.19 Scheduler 설정 변경

\`\`\`text id=“bat30120” Cron

Enabled

Timezone

Concurrency Policy

Misfire Policy

Maximum Duration



변경은 승인·이력·다중 Node 반영 확인이 필요하다.

\---

\## 30.3.20 Job 강제종료

강제종료 전에:

\`\`\`text id="bat30121"
STOP\_REQUESTED 기록

현재 Chunk 종료 대기

Checkpoint 저장

외부 호출 상태 확인

Lock 해제

STOPPED

Thread Interrupt만으로 종료하면 데이터 상태가 불명확해질 수 있다.

# 30.4 파일 보안과 정합성

## 30.4.1 인증 사용자

금지:

\`\`\`text id=“bat30122” 업로드 사용자 = Request Parameter userId

다운로드 사용자 = Query String userId



권장:

\`\`\`text id="bat30123"
AuthenticationContext.getUserId()

화면이 전송한 사용자 ID는 인증 근거로 사용하지 않는다.

## 30.4.2 파일 권한

\`\`\`text id=“bat30124” 업로드 권한

목록 조회 권한

상세 조회 권한

다운로드 권한

삭제 권한

복구 권한

원문 다운로드 권한

대량 다운로드 권한



을 분리한다.

파일 ID를 안다고 누구나 다운로드할 수 있어서는 안 된다.

\---

\## 30.4.3 파일명

원본 파일명에서 제거·제한할 값:

\`\`\`text id="bat30125"
../

..\\

슬래시

역슬래시

제어문자

CR·LF

Null Byte

지나치게 긴 이름

예약어

원본 파일명은 표시·감사용 Metadata로만 사용한다.

## 30.4.4 Path Traversal

물리 경로:

text id="bat30126" storageRoot.resolve(storageKey) .normalize()

검증:

text id="bat30127" resolvedPath.startsWith(storageRoot)

현재처럼 서버가 생성한 UUID만 Storage Key로 사용하는 것은 Path Traversal 위험을 줄인다.

## 30.4.5 확장자 Allow List

예:

\`\`\`text id=“bat30128” csv

txt

xlsx

pdf

zip



업무별 허용 목록을 관리한다.

금지 또는 별도 승인:

\`\`\`text id="bat30129"
exe

dll

bat

cmd

ps1

js

jsp

class

war

jar

파일명 확장자만으로 판단하지 않는다.

## 30.4.6 MIME와 Magic Byte

\`\`\`text id=“bat30130” Content-Type application/pdf

실제 Binary MZ 실행파일



검증:

\`\`\`text id="bat30131"
확장자

Client MIME

서버 MIME 감지

Magic Byte

업무 Parser

불일치하면 격리·거절한다.

## 30.4.7 압축파일

검증:

\`\`\`text id=“bat30132” 압축 해제 후 총 크기

파일 개수

중첩 깊이

압축률

경로 Traversal

암호화 ZIP

허용 확장자



Zip Bomb을 방지한다.

\---

\## 30.4.8 악성코드 검사

\`\`\`text id="bat30133"
UPLOADING

→ QUARANTINED

→ SCANNING

→ CLEAN

→ AVAILABLE

검사 실패:

\`\`\`text id=“bat30134” INFECTED

SCAN\_FAIL

MANUAL\_REVIEW



검사 완료 전 다운로드·Batch 처리를 금지한다.

\---

\## 30.4.9 Hash

권장:

\`\`\`text id="bat30135"
SHA-256

활용:

\`\`\`text id=“bat30136” 업로드 무결성

중복 파일 탐지

전송 전후 대사

Storage 손상 확인

다운로드 증적



보안 목적의 전자서명과 단순 Hash는 구분한다.

\---

\## 30.4.10 파일 상태

\`\`\`text id="bat30137"
UPLOADING

UPLOADED

SCANNING

AVAILABLE

PROCESSING

PROCESSED

PROCESS\_FAIL

DELETE\_PENDING

DELETED

QUARANTINED

EXPIRED

## 30.4.11 파일 메타 테이블

sql id="bat30138" CREATE TABLE TCF\_FILE\_META ( FILE\_ID VARCHAR2(64) NOT NULL, ORIGINAL\_FILE\_NAME VARCHAR2(255) NOT NULL, STORAGE\_KEY VARCHAR2(300) NOT NULL, CONTENT\_TYPE VARCHAR2(100), FILE\_EXTENSION VARCHAR2(20), FILE\_SIZE\_BYTES NUMBER(20) NOT NULL, SHA256\_HASH VARCHAR2(64), FILE\_STATUS VARCHAR2(30) NOT NULL, BUSINESS\_CODE VARCHAR2(20) NOT NULL, OWNER\_USER\_ID VARCHAR2(100) NOT NULL, SECURITY\_LEVEL VARCHAR2(30) NOT NULL, SCAN\_STATUS VARCHAR2(30), RETENTION\_UNTIL TIMESTAMP, CREATED\_AT TIMESTAMP NOT NULL, AVAILABLE\_AT TIMESTAMP, DELETED\_AT TIMESTAMP, VERSION\_NO NUMBER(18) NOT NULL, CONSTRAINT PK\_TCF\_FILE\_META PRIMARY KEY (FILE\_ID), CONSTRAINT UX\_TCF\_FILE\_STORAGE UNIQUE (STORAGE\_KEY) );

## 30.4.12 업로드 정합성

권장 흐름:

text id="bat30139" 1. FILE\_META UPLOADING INSERT 2. 임시 Storage Streaming 3. 크기·Hash 검증 4. 악성코드 검사 5. 최종 Storage Rename 6. FILE\_META AVAILABLE UPDATE

중간 실패 시 상태로 복구한다.

## 30.4.13 Orphan File

\`\`\`text id=“bat30140” Storage 파일 존재

Metadata 없음



정리 Batch:

\`\`\`text id="bat30141"
Storage 목록

↔ Metadata 목록

차이 탐지

격리

승인 후 삭제

## 30.4.14 Missing File

\`\`\`text id=“bat30142” Metadata AVAILABLE

Storage 파일 없음



즉시 장애·정합성 경보 대상이다.

\---

\## 30.4.15 삭제 흐름

\`\`\`text id="bat30143"
삭제 요청

→ 권한·보존·Legal Hold 확인

→ DELETE\_PENDING

→ 물리파일 삭제

→ DELETED

→ 감사

물리 삭제 실패 시 DELETE\_PENDING으로 재처리한다.

## 30.4.16 논리삭제와 물리삭제

\`\`\`text id=“bat30144” 논리삭제 → 일반 조회 제외

보존기간 종료 → 물리 파기



즉시 물리 삭제가 필요한 개인정보·보안 요구는 별도 정책을 적용한다.

\---

\## 30.4.17 파일 암호화

\`\`\`text id="bat30145"
전송
→ TLS

저장
→ Disk·Object Storage 암호화

고보안 파일
→ 개별 Envelope Encryption 검토

암호키는 파일 저장소와 분리한다.

## 30.4.18 다운로드 Header

\`\`\`text id=“bat30146” Content-Type

Content-Length

Content-Disposition

Content-Security-Policy

X-Content-Type-Options: nosniff

Cache-Control



원본 파일명을 Header에 넣을 때 Encoding과 CRLF를 안전하게 처리한다.

\---

\## 30.4.19 브라우저 실행 방지

업로드 파일을 같은 Web Root 아래 저장하지 않는다.

\`\`\`text id="bat30147"
webapps/uploads/test.jsp

같은 구조는 원격코드 실행 위험을 만들 수 있다.

파일은 애플리케이션 실행경로 밖에 저장한다.

## 30.4.20 다운로드 감사

기록:

\`\`\`text id=“bat30148” 사용자

지점

파일 ID

파일명 마스킹

파일 크기

다운로드 시각

Source IP

ServiceId

결과

GUID

사유



현재 구현은 성공·실패 다운로드 감사로그를 기록하는 기반을 제공한다.

보완:

\`\`\`text id="bat30149"
Query Parameter userId 제거

인증 사용자 적용

GUID·권한·사유

Proxy 환경의 신뢰 IP Header

## 30.4.21 파일 Batch 대사

\`\`\`text id=“bat30150” 파일 Header 건수

Detail 실제 건수

Trailer 건수

DB 성공 건수

DB 실패 건수

중복 건수

금액 합계

Hash


\---

\## 30.4.22 인코딩

지원:

\`\`\`text id="bat30151"
UTF-8

승인된 MS949·EUC-KR

검증:

\`\`\`text id=“bat30152” BOM

잘못된 Byte

대체문자

한글 깨짐

Line Ending



인코딩을 추측하지 말고 Interface 계약에 명시한다.

\---

\# 표준 Batch Catalog

| 항목 | 설명 |
|---|---|
| Job ID | 고유 식별자 |
| Job Name | 업무명 |
| Business Code | 소유 업무 |
| Schedule | Cron·Timezone |
| Enabled | 실행 여부 |
| Concurrency | FORBID·ALLOW |
| Misfire | 즉시·Skip·수동 |
| Max Duration | 최대 실행시간 |
| Chunk Size | Commit 단위 |
| Retry Limit | Item 재시도 |
| Skip Limit | 실패 허용 |
| Restartable | 재시작 여부 |
| Idempotent | 중복 처리 안전성 |
| Input | Table·File·Queue |
| Output | Table·File·외부 |
| Reconciliation | 건수·금액 |
| Owner | 업무·운영 |
| Runbook | 장애·재처리 |
| Retention | 이력 보존 |

\---

\# 표준 Job 테이블

\`\`\`sql id="bat30153"
CREATE TABLE TCF\_BATCH\_JOB\_EXECUTION (
EXECUTION\_ID VARCHAR2(64) NOT NULL,
JOB\_NAME VARCHAR2(100) NOT NULL,
JOB\_INSTANCE\_KEY VARCHAR2(500) NOT NULL,
BUSINESS\_DATE DATE,
PARAMETER\_HASH VARCHAR2(64) NOT NULL,
RUN\_STATUS VARCHAR2(30) NOT NULL,
REQUEST\_TYPE VARCHAR2(20) NOT NULL,
REQUEST\_USER VARCHAR2(100),
REQUEST\_REASON VARCHAR2(1000),
EXECUTION\_NODE VARCHAR2(100),
STARTED\_AT TIMESTAMP,
ENDED\_AT TIMESTAMP,
READ\_COUNT NUMBER(18) DEFAULT 0,
WRITE\_COUNT NUMBER(18) DEFAULT 0,
SKIP\_COUNT NUMBER(18) DEFAULT 0,
FAIL\_COUNT NUMBER(18) DEFAULT 0,
INPUT\_AMOUNT NUMBER(30,2),
OUTPUT\_AMOUNT NUMBER(30,2),
ERROR\_CODE VARCHAR2(50),
RESTART\_OF\_EXECUTION VARCHAR2(64),
ARTIFACT\_VERSION VARCHAR2(50),
CREATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_TCF\_BATCH\_EXECUTION
PRIMARY KEY (EXECUTION\_ID)
);

# 표준 Scheduler 설정

yaml id="bat30154" nsight: batch: jobs: campaign-target-extract: enabled: true cron: "0 0 2 \* \* \*" zone: "Asia/Seoul" concurrency-policy: FORBID misfire-policy: MANUAL maximum-duration-minutes: 120 chunk-size: 500 page-size: 1000 retry-limit: 2 skip-limit: 100 lock: lease-minutes: 150 heartbeat-seconds: 30

# 정상 Batch 흐름

text id="bat30155" 1. Scheduler가 실행시각을 판단한다. 2. Job 사용여부와 업무일자를 확인한다. 3. Job Instance 중복을 검사한다. 4. DB Lock을 획득한다. 5. Job Execution을 RUNNING으로 저장한다. 6. 이전 Checkpoint를 조회한다. 7. Reader가 다음 Page를 조회한다. 8. Processor가 Item을 검증·변환한다. 9. Writer가 Chunk 단위로 저장한다. 10. 업무 데이터와 Checkpoint를 Commit한다. 11. 다음 Chunk를 반복한다. 12. 실패 Item은 정책에 따라 Retry·Skip한다. 13. 입력·출력 건수와 금액을 대사한다. 14. 최종 상태를 SUCCESS·PARTIAL·FAIL로 저장한다. 15. Lock을 해제한다. 16. Metric·알림·OM 이력을 갱신한다.

# 정상 파일 업로드 흐름

text id="bat30156" 1. 인증 사용자와 업로드 권한을 확인한다. 2. 파일명·확장자·요청 크기를 사전검증한다. 3. FILE\_META를 UPLOADING으로 생성한다. 4. 파일을 임시 영역에 Streaming 저장한다. 5. 실제 크기와 SHA-256을 계산한다. 6. MIME·Magic Byte를 검증한다. 7. 악성코드 검사를 수행한다. 8. 최종 Storage로 이동한다. 9. FILE\_META를 AVAILABLE로 변경한다. 10. 감사로그와 Metric을 기록한다. 11. File ID를 사용자에게 반환한다.

# 정상 다운로드 흐름

text id="bat30157" 1. File ID로 Metadata를 조회한다. 2. AVAILABLE 상태를 확인한다. 3. 사용자·지점·업무권한을 확인한다. 4. 보존·Legal Hold·삭제상태를 확인한다. 5. Range Header를 검증한다. 6. Storage에서 Streaming 전송한다. 7. 성공·실패 감사로그를 기록한다. 8. 전송량과 처리시간 Metric을 기록한다.

# Batch 실패 흐름

\`\`\`text id=“bat30158” Chunk 처리

→ Item 오류

→ Retryable 판단

→ 최대 2회 Retry

→ 지속 실패

→ Error Item 저장

→ Skip Limit 확인

→ 계속 또는 Job FAIL

→ 대사

→ PARTIAL·FAIL


\---

\# Scheduler 중복 흐름

\`\`\`text id="bat30159"
AP01 Trigger

AP02 Trigger

AP01 Lock 획득

AP02 Lock 실패

AP02
→ SKIPPED\_ALREADY\_RUNNING 이력

AP01
→ Job 실행

# 파일 정합성 오류 흐름

\`\`\`text id=“bat30160” FILE\_META AVAILABLE

Storage 파일 없음

→ 다운로드 차단

→ FILE\_MISSING 오류

→ Critical Alert

→ Backup·복제본 복구

→ Hash 검증

→ 상태 정상화


\---

\# 정상 예시

\`\`\`text id="bat30161"
Job
CM\_TARGET\_EXTRACT

업무일자
2026-07-18

입력
1,000,000건

Page Size
1,000

Chunk Size
500

Worker
4

중복정책
FORBID

Checkpoint
고객번호 Keyset

결과
성공 999,980
실패 20

정책
PARTIAL

오류 Item
20건 격리

대사
1,000,000 =
999,980 + 20

재처리
20건 승인 후 실행

# 금지 예시

\`\`\`text id=“bat30162” 전체 고객을 List로 조회한다.

전체 파일을 byte\[\]로 읽는다.

다운로드 파일을 byte\[\]로 응답한다.

300GB 파일을 일반 WAS Multipart로 처리한다.

파일 크기만 설정하고 동시 업로드 수를 제한하지 않는다.

원본 파일명을 물리 경로로 사용한다.

업로드 userId를 Request Parameter로 신뢰한다.

Content-Type Header만 보고 파일종류를 판단한다.

악성코드 검사 전에 파일을 다운로드 가능하게 한다.

파일을 webapps 하위에 저장한다.

파일 Hash를 검증하지 않는다.

파일 저장 후 DB INSERT 실패를 복구하지 않는다.

DB 논리삭제 후 물리삭제 실패를 무시한다.

Metadata와 Storage 대사를 수행하지 않는다.

Scheduler가 시작됐으므로 Job 성공으로 처리한다.

모든 WAS에서 같은 @Scheduled Job을 실행한다.

중복 Lock 없이 수동·자동 실행을 허용한다.

영구 Lock을 사용하고 Lease를 두지 않는다.

Lease가 만료됐는데 기존 Job Write를 허용한다.

전체 데이터를 한 Transaction으로 처리한다.

Checkpoint를 Commit과 별도로 저장한다.

실패 후 항상 처음부터 재실행한다.

Item이 멱등하지 않은데 Retry한다.

Skip된 Item을 기록하지 않는다.

Skip 상한 없이 계속 처리한다.

입력·출력 건수와 금액을 대사하지 않는다.

PARTIAL을 SUCCESS로 표시한다.

Batch가 온라인 업무와 같은 Pool을 무제한 사용한다.

Cron Timezone을 서버 기본값에 맡긴다.

장기 실행 중 다음 Schedule이 겹쳐도 실행한다.

수동 재처리에 사유·승인·감사기록이 없다.


\---

\# 연계 규칙

\## 온라인과 Batch

\`\`\`text id="bat30163"
온라인
→ Job 요청 등록

Batch
→ 요청 조회·처리

온라인
→ 상태조회

온라인 Service가 Batch 내부 클래스를 직접 호출하지 않는다.

## 파일과 Batch

\`\`\`text id=“bat30164” 파일 AVAILABLE

→ Batch Job 생성

→ PROCESSING

→ PROCESSED·PROCESS\_FAIL


\## OM 연계

\`\`\`text id="bat30165"
Job Catalog

Schedule

수동 실행

중지

재시작

Error Item

대사

이력

## 로그·추적

\`\`\`text id=“bat30166” 온라인 접수 GUID

Job Instance ID

Execution ID

File ID

Item Key



를 연결한다.

\---

\# 책임 경계와 RACI

| 활동 | 업무개발 | Batch팀 | AA | DBA | TA·Storage | 보안 | 운영·OM | QA |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Batch 요구사항 | A/R | C | C | C | I | C | C | C |
| Job 설계 | R/C | A/R | A/C | C | C | C | C | C |
| Chunk·Checkpoint | C | R | A | C | I | I | C | C |
| Reader·Writer SQL | R/C | R | C | A/R | I | I | C | C |
| Lock·Scheduler | I | R | A | C | C | I | R/C | C |
| 파일 API | R | C | A/C | I | R/C | C | C | C |
| 파일 Storage | I | C | C | I | A/R | C | R/C | C |
| 파일 보안 | C | C | C | I | C | A/R | C | C |
| 악성코드 | I | I | C | I | R | A/R | C | C |
| 재처리 | A/C | R | C | C | I | C | A/R | C |
| 대사 | A/R | R | C | A/C | I | I | C | C |
| Metric·Alert | C | R | A/C | C | C | C | A/R | C |
| 장애 복구 | R/C | R/C | C | C | R/C | C | A/R | I |
| 테스트 | R | R | C | C | C | C | C | A/R |

\---

\# 데이터 및 상태관리

\## 파일 상태관리

\`\`\`text id="bat30167"
UPLOADING

UPLOADED

SCANNING

AVAILABLE

PROCESSING

PROCESSED

PROCESS\_FAIL

DELETE\_PENDING

DELETED

QUARANTINED

EXPIRED

## Batch 상태관리

\`\`\`text id=“bat30168” REQUESTED

READY

RUNNING

STOP\_REQUESTED

STOPPED

SUCCESS

PARTIAL

FAIL

RETRY\_WAIT

RESTARTING

CANCELLED

UNKNOWN


\## 오류 Item 상태

\`\`\`text id="bat30169"
FAILED

RETRY\_WAIT

RETRYING

RESOLVED

IGNORED

MANUAL\_REVIEW

# 성능·용량·확장성

## Batch 시간창

\`\`\`text id=“bat30170” 온라인 Peak 09:00~18:00

대량 Batch 00:00~06:00



업무상 실시간 Batch는 별도 자원으로 분리한다.

\---

\## DB Pool

예:

\`\`\`text id="bat30171"
온라인 Pool
120

Batch Pool
20

Batch Worker
최대 10

Batch Worker 수가 Pool 크기보다 크면 Connection 대기가 발생한다.

## Thread

\`\`\`text id=“bat30172” Scheduler Thread

Job Launcher Thread

Worker Thread

File Streaming Thread

Virus Scan Thread



를 구분하고 무제한 Executor를 금지한다.

\---

\## Storage IOPS

동시 Upload·Download·Batch Scan이 같은 Disk를 사용하면 병목이 발생할 수 있다.

\`\`\`text id="bat30173"
업로드 Write

다운로드 Read

악성코드 Scan Read

Backup Read

정리 Delete

Storage 성능시험이 필요하다.

## Network

text id="bat30174" 동시 다운로드 수 × 평균 전송속도

가 NIC·L4·Apache 대역폭을 초과하지 않아야 한다.

## Batch Partition 확장

\`\`\`text id=“bat30175” Worker 1 지점 0000~1999

Worker 2 2000~3999



Partition 간 데이터 충돌과 최종 대사를 검증한다.

\---

\## Back Pressure

처리속도보다 입력속도가 빠르면 적체가 증가한다.

\`\`\`text id="bat30176"
입력 TPS

처리 TPS

Queue Lag

예상 소진시간

을 표시한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 사용자 | 인증문맥 사용 |
| 파일명 | 정규화·길이 제한 |
| 저장경로 | 서버 생성 Key |
| 파일종류 | 확장자·MIME·Magic |
| 악성코드 | 격리·검사 |
| Hash | SHA-256 |
| 전송 | TLS |
| 저장 | 암호화 |
| 권한 | 업로드·다운로드·삭제 분리 |
| 대량다운로드 | 별도 권한·사유 |
| 개인정보 | 최소화·보존·파기 |
| 수동 Batch | 사유·승인·감사 |
| 재처리 | 실행자·대상·결과 |
| 파일 삭제 | Legal Hold 확인 |
| 로그 | 파일 내용·민감정보 금지 |

# 운영·모니터링·장애 대응

## Batch Metric

\`\`\`text id=“bat30177” batch.job.started

batch.job.completed

batch.job.failed

batch.job.partial

batch.job.duration

batch.item.read

batch.item.written

batch.item.failed

batch.item.skipped

batch.item.retried

batch.checkpoint.lag

batch.lock.acquire.failed

batch.execution.overdue

batch.reconciliation.difference


\## 파일 Metric

\`\`\`text id="bat30178"
file.upload.count

file.upload.bytes

file.upload.duration

file.download.count

file.download.bytes

file.download.duration

file.scan.fail.count

file.hash.mismatch.count

file.orphan.count

file.missing.count

file.storage.usage

file.delete.pending.count

## Dashboard

\`\`\`text id=“bat30179” 실행 중 Job

예정·지연 Job

성공·PARTIAL·FAIL

처리 건수·처리속도

예상 완료시간

Checkpoint

오류 Item

재처리 대기

Lock Owner

파일 상태·용량

Storage 사용률

대사 차이


\## Alert

| 조건 | 등급 |
|---|---|
| Job 미실행 | Major |
| 예상시간 초과 | Major |
| 같은 Job 중복실행 | Critical |
| Checkpoint 장시간 정지 | Major |
| PARTIAL 발생 | Major |
| 대사 불일치 | Critical |
| Lock 장기점유 | Major |
| Storage 80% | Warning |
| Storage 95% | Critical |
| Malware 발견 | Security Critical |
| Hash 불일치 | Critical |
| Orphan·Missing File | Major·Critical |
| 실패 Item 급증 | Major |

\---

\## Batch 장애 점검 순서

\`\`\`text id="bat30180"
1\. Job ID·Execution ID 확인
2\. 업무일자·Parameter 확인
3\. Lock Owner와 중복실행 확인
4\. 현재 Step·Chunk·Checkpoint 확인
5\. Read·Write·Skip·Fail 건수 확인
6\. 마지막 오류코드·Item 확인
7\. DB Pool·Lock·Slow SQL 확인
8\. 외부 연계·Storage 상태 확인
9\. 입력·출력 대사 확인
10\. Restart·Retry·수동조치 결정

## 파일 장애 점검 순서

text id="bat30181" 1. File ID 확인 2. Metadata 상태 확인 3. Storage Key와 실제 파일 확인 4. 파일 크기·Hash 비교 5. 악성코드 검사상태 확인 6. 사용자 권한·감사로그 확인 7. 업로드·다운로드 Node 확인 8. Storage 용량·IO 확인 9. 복제본·Backup 복구 10. Metadata 상태 보정

# 자동검증 및 품질 Gate

## 1\. 대용량 코드 Gate

검출:

\`\`\`text id=“bat30182” MultipartFile.getBytes()

Files.readAllBytes()

InputStream.readAllBytes()

ResponseEntity<byte\[\]>

SELECT 전체 List

무제한 queryForList

메모리 Workbook



파일 크기 상한이 매우 작은 승인 사례만 예외로 둔다.

\---

\## 2. Batch Catalog Gate

\`\`\`text id="bat30183"
Job ID

Owner

Schedule

Timezone

Lock

Chunk

Retry·Skip

Restart

대사

Runbook

## 3\. Scheduler Gate

\`\`\`text id=“bat30184” 다중 Instance Lock

Concurrency Policy

Misfire Policy

Maximum Duration

Enabled

Cron Validation

영업일


\---

\## 4. Checkpoint Gate

\`\`\`text id="bat30185"
안정적 정렬

마지막 Key

동일 Transaction 저장

재시작 Test

중복방지

Partition별 상태

## 5\. File Security Gate

\`\`\`text id=“bat30186” 인증문맥

파일명 정규화

확장자 Allow List

MIME·Magic Byte

최대 크기

악성코드

Hash

Storage 경로

다운로드 권한

감사


\---

\## 6. File Integrity Gate

\`\`\`text id="bat30187"
Metadata 상태

Storage Key Unique

파일 크기

Hash

Orphan·Missing 대사

삭제 재처리

Backup·복구

## 7\. 성능 Gate

\`\`\`text id=“bat30188” 목표 완료시간

처리 TPS

Heap

Worker·Pool

Chunk 시간

Storage IOPS

Network

온라인 영향


\---

\## 8. 운영 Gate

\`\`\`text id="bat30189"
Metric

Alert

OM 조회

중지·재시작

오류 Item

대사

수동 실행 감사

Runbook

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| BATCH-001 | 정상 Scheduler 실행 | Job 시작 |
| BATCH-002 | Job Disabled | SKIPPED |
| BATCH-003 | 휴일 실행 | 정책상 Skip |
| BATCH-004 | Cron Timezone | KST 실행 |
| BATCH-005 | Server 미기동 Misfire | 정책 적용 |
| BATCH-006 | 정상 Lock 획득 | 1개 실행 |
| BATCH-007 | 두 Node 동시 Trigger | 한 Node만 실행 |
| BATCH-008 | 자동 중 수동 실행 | 중복 거절 |
| BATCH-009 | Lock Lease 만료 전 | 신규 실행 차단 |
| BATCH-010 | Process 비정상 종료 | Lease 후 복구 |
| BATCH-011 | Heartbeat 실패 | Alert·안전 중지 |
| BATCH-012 | Fencing Token 구버전 | Write 차단 |
| BATCH-013 | 이전 Job 장기 실행 | 다음 실행 Skip |
| BATCH-014 | 다른 업무일자 Job | 정책상 병렬 |
| BATCH-015 | 정상 Chunk | Commit·Checkpoint |
| BATCH-016 | Chunk 중 Item 실패 | Chunk Rollback |
| BATCH-017 | Chunk 재시작 | 마지막 Commit 이후 |
| BATCH-018 | Checkpoint 저장 실패 | 업무 Write Rollback |
| BATCH-019 | 안정적 Keyset | 중복·누락 없음 |
| BATCH-020 | Offset 중 원본 추가 | 결함 탐지 |
| BATCH-021 | Retryable Deadlock | 제한 Retry |
| BATCH-022 | 입력 오류 Item | Retry 없음 |
| BATCH-023 | Skip 허용범위 | PARTIAL |
| BATCH-024 | Skip Limit 초과 | FAIL |
| BATCH-025 | 실패 Item 저장 | 원인·Key 기록 |
| BATCH-026 | 실패 Item 재처리 | RESOLVED |
| BATCH-027 | 동일 Item 재처리 | 중복 없음 |
| BATCH-028 | MERGE Upsert 반복 | 최신 1건 |
| BATCH-029 | 전체 재실행 | 멱등 결과 |
| BATCH-030 | Input 0건 | SUCCESS\_EMPTY |
| BATCH-031 | 입력·출력 건수 일치 | SUCCESS |
| BATCH-032 | 건수 불일치 | FAIL |
| BATCH-033 | 금액 불일치 | Critical |
| BATCH-034 | 대상 일부 실패 | PARTIAL |
| BATCH-035 | 전체 대상 실패 | FAIL |
| BATCH-036 | 예상시간 초과 | Overdue Alert |
| BATCH-037 | STOP 요청 | Chunk 후 중지 |
| BATCH-038 | 강제 Process Kill | Checkpoint 복구 |
| BATCH-039 | 온라인 Peak 중 Batch | 자원 통제 |
| BATCH-040 | Worker 증가 | DB 상한 이내 |
| FILE-001 | 정상 소형 업로드 | AVAILABLE |
| FILE-002 | 0 Byte 파일 | 정책상 거절 |
| FILE-003 | 최대 크기 경계 | 정상 |
| FILE-004 | 최대 크기 초과 | 즉시 거절 |
| FILE-005 | 실제 전송크기 초과 | Streaming 중단 |
| FILE-006 | 파일명 없음 | 안전한 대체명 |
| FILE-007 | ../a.jsp 파일명 | 정규화·거절 |
| FILE-008 | CRLF 파일명 | 거절 |
| FILE-009 | 확장자 금지 | 거절 |
| FILE-010 | MIME 불일치 | 격리 |
| FILE-011 | Magic Byte 불일치 | 격리 |
| FILE-012 | 악성코드 발견 | QUARANTINED |
| FILE-013 | Scan Engine 장애 | SCAN\_FAIL |
| FILE-014 | SHA-256 정상 | 저장 |
| FILE-015 | Hash 불일치 | 사용 차단 |
| FILE-016 | 업로드 중 Network 단절 | UPLOAD\_FAIL |
| FILE-017 | 분할 업로드 재개 | 남은 Chunk |
| FILE-018 | 같은 Chunk 재전송 | 중복 저장 없음 |
| FILE-019 | 전체 파일 조립 | Hash 일치 |
| FILE-020 | Storage 저장 후 DB 실패 | Orphan 정리 |
| FILE-021 | DB 생성 후 Storage 실패 | 실패상태 |
| FILE-022 | 정상 다운로드 | Streaming |
| FILE-023 | Range Download | 206 |
| FILE-024 | 다운로드 중단 | 재개 가능 |
| FILE-025 | 권한 없는 다운로드 | 차단 |
| FILE-026 | 삭제 파일 다운로드 | 차단 |
| FILE-027 | Scan 전 다운로드 | 차단 |
| FILE-028 | 다른 업무 파일 접근 | 차단 |
| FILE-029 | Query userId 위조 | 인증 사용자 적용 |
| FILE-030 | 다운로드 성공 감사 | SUCCESS |
| FILE-031 | 다운로드 실패 감사 | FAIL |
| FILE-032 | Storage 파일 없음 | Critical Alert |
| FILE-033 | Metadata 없는 파일 | Orphan 탐지 |
| FILE-034 | 논리삭제 | 목록 제외 |
| FILE-035 | 물리삭제 성공 | DELETED |
| FILE-036 | 물리삭제 실패 | DELETE\_PENDING |
| FILE-037 | 보존기간 미도래 삭제 | 차단 |
| FILE-038 | Legal Hold 파일 삭제 | 차단 |
| FILE-039 | Storage 80% | Warning |
| FILE-040 | Storage 95% | 업로드 제한 |
| FILE-041 | 동시 100건 업로드 | Heap 안정 |
| FILE-042 | 동시 대용량 다운로드 | Thread·대역폭 안정 |
| FILE-043 | byte\[\] 코드 | CI Gate 실패 |
| FILE-044 | Web Root 저장 | Security Gate 실패 |
| FILE-045 | 압축 Bomb | 차단 |
| FILE-046 | ZIP Path Traversal | 차단 |
| FILE-047 | 잘못된 인코딩 CSV | 오류파일 |
| FILE-048 | CSV 중간 오류 | 정책상 Skip·Fail |
| FILE-049 | Header·Trailer 대사 | 일치 |
| FILE-050 | 파일 Batch 재시작 | Line Checkpoint |
| OPS-001 | 수동 Batch 사유 없음 | 거절 |
| OPS-002 | 미인가 수동 실행 | 권한 오류 |
| OPS-003 | Job 중지 | 감사로그 |
| OPS-004 | 실패 Item 재처리 | 승인·감사 |
| OPS-005 | Batch 이력 삭제 | 이중승인 |
| OPS-006 | 파일 삭제 | 감사 |
| OPS-007 | Job Metric | Count·Duration |
| OPS-008 | 대사 차이 Alert | 발생 |
| OPS-009 | 배포 후 Scheduler 중복 | Lock 방지 |
| OPS-010 | DR 전환 후 Job | 단일 실행 |

# 따라 하는 실무 절차

## 1단계. 데이터·파일 최대 규모를 확정한다

기록:

\`\`\`text id=“bat30190” 최대 건수

일평균 건수

최대 파일크기

일 파일량

완료시간

보존기간


\## 2단계. 온라인과 Batch를 분리한다

\`\`\`text id="bat30191"
온라인
→ 접수·상태조회

Batch
→ 실제 대량 처리

## 3단계. Job Instance Key를 정의한다

\`\`\`text id=“bat30192” Job Name

업무일자

업무 키

파일 ID

Parameter Hash


\## 4단계. Chunk·Checkpoint를 설계한다

\`\`\`text id="bat30193"
Reader

Page

Chunk

Writer

Commit

마지막 Key

## 5단계. Retry·Skip·재처리를 설계한다

오류유형별 정책과 상한을 작성한다.

## 6단계. 중복 실행 Lock을 구현한다

다중 Node·수동 실행을 포함해 시험한다.

## 7단계. 파일 Streaming과 보안을 구현한다

\`\`\`text id=“bat30194” 크기

파일명

MIME

Magic

Hash

Scan

권한


\## 8단계. 상태·이력·대사를 구현한다

\`\`\`text id="bat30195"
Job

Step

Item

File

Reconciliation

## 9단계. Metric·OM·Runbook을 등록한다

## 10단계. 정상·경계·장애·재시작을 시험한다

# 완료 체크리스트

## 규모·자원

| 확인 항목 | 완료 |
| --- | --- |
| 최대 건수와 파일 크기가 있다. | □ |
| 목표 완료시간이 있다. | □ |
| 전체 메모리 적재가 없다. | □ |
| Chunk·Page·Fetch 크기가 있다. | □ |
| Worker·DB Pool 상한이 있다. | □ |
| Storage·Network 용량을 산정했다. | □ |
| 온라인과 Batch 자원을 분리했다. | □ |
| 성능·장시간 시험을 수행했다. | □ |

## Batch·Checkpoint

| 확인 항목 | 완료 |
| --- | --- |
| Job Instance Key가 있다. | □ |
| Job·Step 상태가 있다. | □ |
| Chunk Transaction이 있다. | □ |
| 안정적인 Checkpoint가 있다. | □ |
| Checkpoint가 같은 Transaction에 저장된다. | □ |
| Item 처리가 멱등하다. | □ |
| Retryable 오류가 정의됐다. | □ |
| Skip Limit이 있다. | □ |
| 실패 Item이 격리된다. | □ |
| 재시작·재처리를 시험했다. | □ |
| 건수·금액 대사를 수행한다. | □ |

## Scheduler·중복

| 확인 항목 | 완료 |
| --- | --- |
| Scheduler와 Job이 분리됐다. | □ |
| 다중 Node Lock이 있다. | □ |
| Lease·Heartbeat가 있다. | □ |
| 동시 실행정책이 있다. | □ |
| 자동·수동 충돌을 방지한다. | □ |
| Cron Timezone이 명시됐다. | □ |
| 영업일 정책이 있다. | □ |
| Misfire 정책이 있다. | □ |
| 최대 실행시간이 있다. | □ |
| 강제중지 절차가 있다. | □ |

## 파일

| 확인 항목 | 완료 |
| --- | --- |
| 파일을 Streaming 처리한다. | □ |
| 파일명과 Storage Key를 분리한다. | □ |
| 업로드 크기를 이중 검증한다. | □ |
| 확장자 Allow List가 있다. | □ |
| MIME·Magic Byte를 검사한다. | □ |
| 악성코드 검사가 있다. | □ |
| SHA-256을 저장한다. | □ |
| Quarantine 상태가 있다. | □ |
| 다운로드 권한을 확인한다. | □ |
| 다운로드 감사가 있다. | □ |
| Metadata·Storage 대사가 있다. | □ |
| 삭제 재처리가 있다. | □ |
| 보존·Legal Hold가 있다. | □ |

## 운영·품질

| 확인 항목 | 완료 |
| --- | --- |
| Job Catalog가 있다. | □ |
| 실행·중지·재처리 권한이 있다. | □ |
| 수동 실행사유가 필수다. | □ |
| PARTIAL을 구분한다. | □ |
| Batch Metric이 있다. | □ |
| 파일 Metric이 있다. | □ |
| 중복·지연·대사 Alert가 있다. | □ |
| 장애 Runbook이 있다. | □ |
| byte\[\] 대용량 코드 Gate가 있다. | □ |
| 테스트 증적이 보존된다. | □ |

# 변경·호환성·폐기 관리

## Chunk 크기 변경

증가:

\`\`\`text id=“bat30196” 처리량 증가 가능

Transaction·Rollback·Heap 증가



감소:

\`\`\`text id="bat30197"
안정성 증가

Commit·Round Trip 증가

변경 전후 처리량·DB Lock·Rollback 시간을 비교한다.

## Job Parameter 변경

기존 Job Instance와 새로운 Instance가 충돌하지 않도록 Parameter Version을 사용한다.

text id="bat30198" CM\_TARGET\_EXTRACT:V2

## Checkpoint Schema 변경

구 실행이 중단된 상태에서 Checkpoint 구조를 바꾸면 재시작할 수 없을 수 있다.

\`\`\`text id=“bat30199” 구 Execution 종료·폐기

Migration

V1·V2 Reader

수동 재처리계획


\---

\## Scheduler 변경

Cron 변경 전:

\`\`\`text id="bat30200"
온라인 Peak

선행·후행 Job

업무일자

외부 시스템 운영시간

DR·Backup 시간

을 확인한다.

## 파일 저장소 전환

\`\`\`text id=“bat30201” Local Disk

→ NAS·Object Storage



절차:

\`\`\`text id="bat30202"
신규 Storage 병행

기존 파일 복사

건수·크기·Hash 대사

Dual Read

신규 Write 전환

기존 Read 종료

구 Storage 파기

## 파일 Contract 변경

\`\`\`text id=“bat30203” CSV Column 추가

인코딩 변경

날짜 형식 변경

압축·암호화 변경



Version과 병행기간이 필요하다.

\---

\## Job 폐기

\`\`\`text id="bat30204"
Scheduler Disable

실행 중 Job 종료

Retry·Error Item 정리

호출량 0 확인

OM Catalog Deprecated

이력 보존

Lock·설정 제거

코드 제거

## 파일 API 폐기

\`\`\`text id=“bat30205” 신규 업로드 중지

기존 다운로드 유지기간

파일 보존·파기

UI·Relay 제거

Route 제거

감사이력 보존


\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 대용량 처리는 데이터를 한 번에 많이 처리하는 기술이 아니라 제한된 자원으로 작은 단위를 반복 처리하는 기술이다.

둘째, Scheduler는 실행시각만 결정하며 Batch의 업무 성공을 보장하지 않는다.

셋째, 현재 \`tcf-batch\`의 상태수집 Job은 \`SUCCESS·PARTIAL·FAIL\`과 멱등 Upsert라는 좋은 기반을 제공하지만, 업무 대량처리에 필요한 Chunk·Checkpoint·Lock·재처리 기능은 추가돼야 한다.

넷째, 현재 파일기능의 \`file.getBytes()\`, \`Files.readAllBytes()\`, \`ResponseEntity<byte\[\]>\` 방식은 50MB 이하 시범기능에는 사용할 수 있지만 동시성이나 대용량 운영에는 적합하지 않다.

다섯째, 다중 인스턴스에서 단순 \`@Scheduled\`를 사용하면 같은 Job이 여러 번 실행될 수 있으므로 DB Lock·Leader·외부 Scheduler 중 하나가 필요하다.

여섯째, Checkpoint는 단순 Page 번호가 아니라 마지막으로 Commit된 안정적인 업무 위치여야 한다.

일곱째, Batch 완료는 실행 예외가 없다는 사실이 아니라 입력·성공·실패·미처리 건수와 금액 대사가 일치한다는 것으로 증명해야 한다.

여덟째, 파일 보안은 확장자 검사만이 아니라 인증·권한·경로·실제 형식·악성코드·Hash·암호화·보존·삭제·감사를 포함한다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| 전체 List 적재 | OOM·GC |
| 파일 \`byte\[\]\` 처리 | Heap 폭증 |
| WAS Relay 중복 | 메모리·대역폭 증가 |
| Chunk 없음 | 장기 Transaction |
| Checkpoint 없음 | 처음부터 재처리 |
| Checkpoint 별도 Commit | 중복·누락 |
| 멱등성 없음 | 재처리 중복 |
| Skip 무제한 | 오류 은폐 |
| 대사 없음 | 완료 증명 불가 |
| 단순 \`@Scheduled\` | 다중 실행 |
| Lock Lease 없음 | 영구 정지 |
| Lease 안전장치 없음 | 동시 Write |
| Cron Timezone 없음 | 실행시각 오류 |
| 온라인과 Pool 공유 | 사용자 지연 |
| userId Parameter 신뢰 | 감사 위조 |
| MIME Header만 검사 | 악성파일 |
| Scan 전 다운로드 | 보안사고 |
| Hash 없음 | 손상 탐지 불가 |
| 메타·파일 불일치 | Orphan·Missing |
| 즉시 전체 삭제 | 복구 불가 |
| 수동 재처리 감사 없음 | 내부통제 위반 |

\---

\## 우선 보완 과제

1\. 현재 파일 업·다운로드의 \`byte\[\]\` 방식을 Streaming으로 전환한다.
2\. \`tcf-ui\` Binary Relay를 제거하거나 Streaming Relay로 개선한다.
3\. 대형 파일은 직접 Storage·Range·Resumable 방식을 적용한다.
4\. Upload·Download 사용자 ID를 인증문맥에서 취득한다.
5\. 파일 상태모델과 SHA-256·악성코드 검사·Quarantine을 구현한다.
6\. Metadata·Storage Orphan·Missing 대사 Batch를 구현한다.
7\. \`tcf-batch\`에 DB 기반 Job Lock과 Lease·Heartbeat를 추가한다.
8\. 업무 Batch용 Job·Execution·Step·Checkpoint·Error Item 테이블을 구축한다.
9\. Chunk Reader·Processor·Writer 표준 Template을 제공한다.
10\. 자동·수동·재시작 실행의 중복정책을 통합한다.
11\. 입력·출력 건수·금액 대사를 Job 완료조건으로 적용한다.
12\. Batch 전용 DataSource·Pool·Worker 상한을 설계한다.
13\. OM Batch 화면에 현재 Step·Checkpoint·오류 Item·Lock Owner를 표시한다.
14\. 수동 실행·중지·재처리에 권한·사유·감사를 적용한다.
15\. \`getBytes()\`, \`readAllBytes()\`, 무제한 List 조회를 CI Gate로 차단한다.

\---

\## 중장기 발전 방향

\`\`\`text id="bat30206"
전체 메모리 처리
↓
Streaming·Chunk

단순 @Scheduled
↓
Job Catalog·DB Lock

재실행
↓
Checkpoint 재시작

예외 로그
↓
Error Item·재처리

실행 성공
↓
건수·금액 대사

Local 파일
↓
공유 Storage·Object Storage

단순 업로드
↓
Quarantine·Scan·Hash

수동 운영
↓
OM 승인형 실행·중지·재처리

개별 Batch
↓
표준 Reader·Processor·Writer Framework

# 마무리말

파일·대용량·Batch·Scheduler를 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“bat30207” 최대 파일 크기와 최대 처리 건수는 얼마인가?

전체 데이터를 메모리에 적재하고 있지 않은가?

파일 한 건이 몇 개의 byte\[\] 복사본을 만드는가?

온라인 요청이 Batch 완료까지 기다려야 하는가?

한 Chunk에서 몇 건을 처리하고 Commit하는가?

Checkpoint는 어느 업무 키를 저장하는가?

업무 데이터와 Checkpoint가 같은 Transaction인가?

실패 후 처음부터 재실행하는가, 중간부터 재시작하는가?

같은 Item이 다시 처리돼도 중복되지 않는가?

실패 Item을 어디에 저장하고 누가 재처리하는가?

입력 건수와 출력·실패·미처리 건수가 일치하는가?

금액과 파일 Hash가 일치하는가?

WAS가 여러 대일 때 Scheduler가 몇 번 실행되는가?

자동 실행 중 수동 실행을 요청하면 어떻게 되는가?

Lock을 보유한 Process가 죽으면 언제 복구되는가?

Cron의 Timezone과 영업일 기준은 무엇인가?

Batch가 온라인 DB Pool을 고갈시키지 않는가?

파일 원본 이름이 물리 경로에 사용되지 않는가?

파일의 확장자·MIME·Magic Byte가 모두 일치하는가?

악성코드 검사 전 파일을 사용할 수 없는가?

Metadata와 실제 파일이 일치하는가?

다운로드 사용자와 권한이 인증문맥으로 검증되는가?

수동 실행·재처리·다운로드를 감사할 수 있는가?

운영자는 Job ID·Execution ID·File ID로 전체 처리를 추적할 수 있는가?



제30장의 핵심 흐름은 다음과 같다.

\`\`\`text id="bat30208"
규모 산정
↓
온라인·Batch 분리
↓
Streaming·Chunk
↓
Checkpoint·멱등성
↓
Lock·Scheduler
↓
파일 보안·무결성
↓
대사·재처리
↓
Metric·OM 운영

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“bat30209” 대량 데이터를 처리했다는 사실보다 중간 실패 이후에도 정확하게 이어서 처리하고,

중복과 누락 없이 입력과 출력의 정합성을 증명하며,

온라인 서비스와 시스템 자원을 보호할 수 있는지가 중요하다.

파일과 Batch는 크기가 큰 온라인 거래가 아니라 별도의 상태·복구·운영체계를 가진 독립 처리영역으로 설계해야 한다. \`\`\`
