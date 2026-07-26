# 18. 파일 업·다운로드 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 18 |
| 제목 | File Upload / Download Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [04-messaging.md](04-messaging.md), [05-exception.md](05-exception.md), [10-session.md](10-session.md), [11-login.md](11-login.md), [16-deploy.md](16-deploy.md) |
| 대상 | 업무·UI·운영 개발자 |

---

## 1. 개요

NSIGHT TCF의 파일 업·다운로드(이하 **UD**)는 **하이브리드 REST** 방식으로 구현된다.

| 구분 | 표준 온라인 거래 | UD 파일 API |
|------|------------------|-------------|
| 진입 | `POST /{code}/online` (JSON) | `POST/GET /ud/files/*` (multipart·REST) |
| 파이프라인 | STF → Dispatcher → Handler → ETF | **TCF 미경유** |
| 응답 형태 | `StandardResponse` | `OmUpdownloadResponseSupport` (유사 Map 구조) |
| 거래 로그 | `TCF_TX_LOG` 기록 | 미기록 (다운로드 감사는 별도 테이블) |
| 구현 위치 | 업무 WAR | **`tcf-om` 내장** (`updownload` 패키지) |

핵심 설계 이유:

1. **multipart·바이너리**는 JSON 전문 파이프라인에 맞지 않음
2. **공통 파일 저장소**를 OM(`tcf-om`)에 두어 업무 WAR(현재 9개)이 각각 파일 API를 중복 구현하지 않음
3. 브라우저는 **`tcf-ui` Relay**로 CORS·배포 모드(bootRun/Tomcat) 차이를 흡수

비즈니스 코드 `UD`는 `BusinessModuleDefinitions`에서 `tcf-om:8097`에 매핑된다.  
실제 파일 API는 별도 업무 WAR가 아니라 **OM WAR의 `/ud/files`** 에서 제공한다.

---

## 2. 전체 아키텍처

```text
┌─────────────────────────────────────────────────────────────────────┐
│  Browser                                                             │
│  - /ui/ud/updownload.html          (UD 전용 화면)                    │
│  - /ui/om/admin/file-management.html (OM 통합 파일 관리)             │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ fetch / window.open
┌───────────────────────────────▼─────────────────────────────────────┐
│  tcf-ui                                                              │
│  UpdownloadApiController   /api/updownload/*                         │
│  UpdownloadRelayService    RestClient 프록시                           │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
          bootRun:  http://127.0.0.1:8097/ud/files/*
          tomcat:  http://localhost:8080/om/ud/files/*
┌───────────────────────────────▼─────────────────────────────────────┐
│  tcf-om                                                              │
│  OmUpdownloadFileController  /ud/files/*                             │
│  OmUpdownloadService         메타 DB + 스토리지 조율                  │
│  OmFileStorageService        로컬 디스크 ({fileId}.bin)               │
│  OmFileDownloadAuditListener 다운로드 감사 (OM_FILE_DOWNLOAD_LOG)    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
              ┌─────────────────┴─────────────────┐
              ▼                                   ▼
     UD_FILE_META (H2)                  ./data/updownload/*.bin
     파일 메타정보                       실제 바이너리
```

### 2.1 TCF 표준 경로와의 관계

표준 온라인 거래는 `TcfGateway`·`OnlineTransactionController` → `TCF.process()`를 탄다.  
UD는 이 경로를 **의도적으로 우회**한다.

다만 OM Admin의 **다운로드 이력 조회**(`OM.FileDownload.inquiry`, `OM-FIL-0001`)는 표준 TCF 경로이며, UD REST가 남긴 `OM_FILE_DOWNLOAD_LOG`를 조회한다.  
즉 “파일 전송”과 “감사 이력 조회”는 서로 다른 진입점이다.

---

## 3. 계층별 책임

| 계층 | 컴포넌트 | 책임 |
|------|----------|------|
| **UI** | `updownload.html`, `om-admin.js` | 목록·업로드·다운로드·삭제 UI, Relay 쿼리 조립 |
| **UI API** | `UpdownloadApiController` | `/api/updownload/*` HTTP 노출, `RelayOptions` 수신 |
| **Relay** | `UpdownloadRelayService` | 대상 URL 결정, multipart/JSON/바이너리 프록시 |
| **OM REST** | `OmUpdownloadFileController` | `/ud/files/*` REST 엔드포인트, 예외 → Map 응답 |
| **Service** | `OmUpdownloadService` | 업로드 검증, CRUD, 다운로드 스트림 조립 |
| **Storage** | `OmFileStorageService` | `{storagePath}/{fileId}.bin` 파일 I/O |
| **Audit** | `OmFileDownloadAuditListener` | 다운로드 성공/실패 감사 로그 INSERT |
| **Config** | `OmUpdownloadProperties` | 저장 경로, 최대 파일 크기 |

---

## 4. API 명세

### 4.1 tcf-om — `/ud/files`

| 메서드 | 경로 | Content-Type | 설명 |
|--------|------|--------------|------|
| `POST` | `/ud/files/upload` | `multipart/form-data` | 파일 업로드 |
| `GET` | `/ud/files` | — | 목록(페이징·필터) |
| `GET` | `/ud/files/{fileId}` | — | 상세 |
| `PUT` | `/ud/files/{fileId}` | `application/json` 또는 query | 설명 수정 |
| `DELETE` | `/ud/files/{fileId}` | — | 논리 삭제 + 물리 파일 삭제 |
| `GET` | `/ud/files/{fileId}/download` | — | 바이너리 다운로드 (`Content-Disposition: attachment`) |

**업로드 파라미터**

| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| `file` | ● | — | Multipart 파일 |
| `userId` | | `GUEST` | 업로드 사용자 |
| `description` | | — | 설명 |
| `businessCode` | | `UD` | 업무 구분 코드 |

**목록 쿼리**

| 파라미터 | 설명 |
|----------|------|
| `originalName` | 원본 파일명 LIKE 검색 |
| `uploadUser` | 업로드 사용자 |
| `fromDate`, `toDate` | 업로드 일자 범위 |
| `pageNo`, `pageSize` | 페이징 (pageSize 최대 100) |
| `businessCode` | 업무 코드 필터 |

### 4.2 tcf-ui — `/api/updownload`

OM API와 1:1 대응하며, 추가로 **배포 모드 오버라이드** 쿼리를 받는다.

| UI API | Relay 대상 (bootRun) | Relay 대상 (Tomcat) |
|--------|----------------------|---------------------|
| `GET /api/updownload/base-url` | — | 현재 해석된 base URL 반환 |
| `POST /api/updownload/upload` | `8097/ud/files/upload` | `{gateway}/om/ud/files/upload` |
| `GET /api/updownload/files` | `8097/ud/files` | `{gateway}/om/ud/files` |
| `GET /api/updownload/files/{id}` | 상세 | 상세 |
| `PUT /api/updownload/files/{id}` | 수정 | 수정 |
| `DELETE /api/updownload/files/{id}` | 삭제 | 삭제 |
| `GET /api/updownload/files/{id}/download` | 바이너리 | 바이너리 |

**Relay 공통 쿼리 (배포)**

| 파라미터 | 설명 |
|----------|------|
| `deploymentMode` | `bootrun` \| `tomcat` (미지정 시 `application.yml`) |
| `bootrunHost` | bootRun 호스트 (예: `http://127.0.0.1`) |
| `tomcatGatewayUrl` | Tomcat 게이트웨이 (예: `http://localhost:8080`) |

`UpdownloadRelayService.resolveBaseUrl()`:

- **tomcat**: `trim(tomcatGatewayUrl) + "/om"`
- **bootrun**: `trim(bootrunHost) + ":8097"` (`BusinessModuleCatalog`의 UD 포트)

---

## 5. 처리 흐름

### 5.1 업로드

```text
Browser                tcf-ui                  tcf-om
   │                      │                       │
   │ POST multipart       │                       │
   ├─────────────────────►│ relayUpload()         │
   │                      ├──────────────────────►│ OmUpdownloadFileController.upload
   │                      │                       ├─ 크기 검증 (maxFileSizeBytes)
   │                      │                       ├─ UUID fileId 발급
   │                      │                       ├─ OmFileStorageService.save → .bin
   │                      │                       ├─ INSERT UD_FILE_META
   │                      │                       └─ OmUpdownloadResponseSupport.success
   │◄─────────────────────┴───────────────────────┤ JSON (header/result/body)
```

검증 실패 시 `IllegalArgumentException` → `E-UD-VAL-0001`.  
시스템 오류 → `E-UD-SYS-0001`.

### 5.2 다운로드

```text
Browser                tcf-ui                  tcf-om
   │                      │                       │
   │ GET .../download     │ relayDownload()       │
   ├─────────────────────►├──────────────────────►│ OmUpdownloadService.download
   │                      │                       ├─ UD_FILE_META 조회
   │                      │                       ├─ OmFileStorageService.load
   │                      │                       ├─ OmFileDownloadAuditListener.recordDownload
   │                      │                       └─ ResponseEntity<byte[]> + Content-Disposition
   │◄─────────────────────┴───────────────────────┤ 바이너리 (헤더 전달)
```

다운로드 API만 **JSON Map이 아닌 `ResponseEntity<byte[]>`** 를 반환한다.  
감사 로그는 성공·실패 모두 `OM_FILE_DOWNLOAD_LOG`에 남긴다.

### 5.3 삭제

1. `UD_FILE_META.USE_YN = 'N'` (논리 삭제)
2. `OmFileStorageService.delete(fileId)` (물리 파일 삭제)

메타가 없으면 `E-UD-BIZ-0001`.

---

## 6. 데이터 모델

### 6.1 `UD_FILE_META` — 파일 메타

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `FILE_ID` | VARCHAR(36) PK | UUID |
| `ORIGINAL_NAME` | VARCHAR(255) | 원본 파일명 |
| `CONTENT_TYPE` | VARCHAR(100) | MIME |
| `FILE_SIZE` | BIGINT | 바이트 크기 |
| `DESCRIPTION` | VARCHAR(500) | 설명 |
| `UPLOAD_USER` | VARCHAR(50) | 업로드 사용자 |
| `UPLOAD_TIME` | VARCHAR(40) | KST ISO 문자열 |
| `BUSINESS_CODE` | VARCHAR(10) | 기본 `UD` |
| `USE_YN` | CHAR(1) | `Y`/`N` |

인덱스: `UPLOAD_TIME`, `UPLOAD_USER`, `ORIGINAL_NAME`.

스키마: `tcf-om/src/main/resources/schema.sql`, 기동 시 `OmDatabaseMigration`에서도 생성.

### 6.2 `OM_FILE_DOWNLOAD_LOG` — 다운로드 감사

| 컬럼 | 설명 |
|------|------|
| `LOG_ID` | `FDL-{UUID}` |
| `DOWNLOAD_TIME` | 다운로드 시각 |
| `USER_ID` | 요청 사용자 |
| `FILE_NAME` | 원본 파일명 |
| `FILE_SIZE` | 파일 크기 |
| `BUSINESS_CODE` | 메타의 업무 코드 |
| `RESULT_STATUS` | `SUCCESS` / `FAIL` |
| `CLIENT_IP` | `HttpServletRequest.getRemoteAddr()` |

OM Admin **파일 관리** 화면에서 UD 메타와 감사 이력을 함께 다룬다.  
이력 **조회만** 표준 TCF (`OM.FileDownload.inquiry`)로 수행한다.

### 6.3 물리 저장

| 항목 | 값 |
|------|-----|
| 설정 키 | `nsight.updownload.storage-path` |
| 기본 경로 | `./data/updownload` |
| 파일명 규칙 | `{fileId}.bin` |
| 최대 크기 | `nsight.updownload.max-file-size-bytes` (기본 52,428,800 = 50MB) |

`OmFileStorageService`는 기동 시 저장 루트 디렉터리를 자동 생성한다.

---

## 7. 응답 형식

### 7.1 JSON API (업로드·목록·상세·수정·삭제)

`OmUpdownloadResponseSupport`가 `StandardResponse`와 **유사한** 구조를 수동 조립한다.

**성공 예**

```json
{
  "header": {
    "businessCode": "UD",
    "requestTime": "2026-06-21T10:00:00+09:00"
  },
  "result": {
    "resultCode": "S0000",
    "resultMessage": "정상 처리되었습니다."
  },
  "body": {
    "file": { "fileId": "...", "originalName": "...", ... }
  }
}
```

**실패 예**

```json
{
  "result": {
    "resultCode": "E-UD-BIZ-0001",
    "resultMessage": "파일을 찾을 수 없습니다.",
    "status": "ERROR"
  },
  "body": { "error": "파일을 찾을 수 없습니다." }
}
```

TCF `Result.fail()`과의 차이:

- 실패 시 `header` 없음
- `resultCode`에 **UD 전용 errorCode 문자열** 직접 사용 (`S0000`이 아님)
- STF/ETF·`TCF_TX_LOG`·멱등성 검사 미적용

상세: [05-exception.md §6.3](05-exception.md).

### 7.2 다운로드 API

- `Content-Type`: 메타의 `CONTENT_TYPE` (없으면 `application/octet-stream`)
- `Content-Disposition`: `attachment; filename="..."`
- 본문: 파일 바이트 배열

### 7.3 Relay 연결 실패

`UpdownloadRelayService`는 대상 OM에 연결하지 못하면 HTTP 502와 함께 UD 형태의 JSON 오류 문자열을 반환한다 (`hint`: tcf-om 8097 기동 확인).

---

## 8. UI 화면

| 화면 | URL (Tomcat `/ui` 컨텍스트) | 용도 |
|------|----------------------------|------|
| UD 파일 관리 | `/ui/ud/updownload.html` | UD 전용 업·다운로드 데모/운영 |
| OM 파일 관리 | `/ui/om/admin/file-management.html` | 메타 + 감사 이력 통합 관리 |

공통 JS (`om-admin.js`):

- `updownloadList`, `updownloadUpload`, `updownloadDownloadUrl` 등
- `buildRelayQuery()` / `updownloadQuery()`로 `deploymentMode` 등 Relay 파라미터 전달

다운로드는 `window.open(.../download?...)`으로 새 탭에서 수행한다.

---

## 9. 배포 모드별 URL

### 9.1 bootRun (로컬 개발)

```text
tcf-ui  : http://127.0.0.1:8099/ui
tcf-om  : http://127.0.0.1:8097
Relay   : http://127.0.0.1:8097/ud/files/*
```

` tcf-scripts/run-local om ui` 로 OM·UI를 함께 기동한다.

### 9.2 ztomcat (통합 배포)

```text
Gateway : http://localhost:8080
OM WAR  : /om  →  http://localhost:8080/om/ud/files/*
UI WAR  : /ui  →  http://localhost:8080/ui/api/updownload/*
```

Relay는 `tomcatGatewayUrl + "/om"`을 prefix로 사용한다.  
상세 배포: [16-deploy.md](16-deploy.md).

---

## 10. 설정

`tcf-om/src/main/resources/application.yml`:

```yaml
nsight:
  updownload:
    storage-path: ./data/updownload
    max-file-size-bytes: 52428800
```

`tcf-ui` 배포 모드 (`nsight.tcf-ui.deployment-mode`, `bootrun-host`, `tomcat-gateway-url`)는 UD Relay에도 동일하게 적용된다.

---

## 11. 오류 코드

| 코드 | 발생 조건 |
|------|-----------|
| `E-UD-VAL-0001` | 파일 없음, 크기 초과 등 입력 검증 |
| `E-UD-BIZ-0001` | 파일 미존재, USE_YN=N |
| `E-UD-SYS-0001` | 저장/삭제/읽기 IO 실패 등 시스템 오류 |

Controller는 `try/catch`로 예외를 삼키고 Map을 반환한다.  
다운로드 실패는 `IllegalStateException`으로 전파될 수 있어 HTTP 500이 될 수 있다.

---

## 12. 확장·연동 가이드

### 12.1 업무 WAR에서 UD 사용

업무 서비스는 파일 바이트를 직접 보관하지 않고:

1. 클라이언트 → `tcf-ui /api/updownload/upload` 또는 OM `/ud/files/upload` 호출
2. 응답 `body.file.fileId`를 업무 테이블에 FK로 저장
3. 다운로드 시 `fileId`로 `/ud/files/{fileId}/download` 호출

`businessCode` 파라미터로 업무별 파일을 구분할 수 있다.

### 12.2 TCF 파이프라인이 필요한 경우

파일 **메타 조회·이력 통계** 등 JSON 전문이 적합한 기능은 Handler + `TcfGateway`로 구현한다.  
현재 UD REST는 Controller → Service **직접 호출** 패턴이다 ([01-application-layer.md](01-application-layer.md) 참고).

multipart를 TCF에 넣으려면 별도 어댑터에서 `TcfGateway.invoke()`로 메타만 처리하고, 바이너리는 UD REST에 위임하는 **하이브리드** 구성을 권장한다.

### 12.3 신규 저장소 도입

`OmFileStorageService`만 교체하면 S3·NAS 등 외부 스토리지로 전환 가능하다.  
`OmUpdownloadService`는 `JdbcTemplate`으로 메타를 관리하므로 스토리지 구현과 분리되어 있다.

---

## 13. 체크리스트

**로컬 개발**

- [ ] `tcf-om`(8097), `tcf-ui`(8099) 기동
- [ ] `./data/updownload` 쓰기 권한 확인
- [ ] `/ui/ud/updownload.html`에서 업로드·목록·다운로드 확인

**Tomcat 통합**

- [ ] `om.war`, `ui.war` 배포 후 `verify-deploy` 통과
- [ ] `GET /ui/api/updownload/base-url?deploymentMode=tomcat` → `.../om` 포함 URL
- [ ] 다운로드 URL이 `/ui/api/updownload/files/{id}/download` 형태인지 확인

**운영**

- [ ] `max-file-size-bytes` 정책 반영
- [ ] `storage-path` 디스크 용량·백업 정책
- [ ] `OM_FILE_DOWNLOAD_LOG` 보존 기간·OM Admin 조회 권한

---

## 14. 참고 소스

| # | 경로 |
|---|------|
| 1 | `tcf-om/.../updownload/controller/OmUpdownloadFileController.java` |
| 2 | `tcf-om/.../updownload/service/OmUpdownloadService.java` |
| 3 | `tcf-om/.../updownload/service/OmFileStorageService.java` |
| 4 | `tcf-om/.../updownload/support/OmUpdownloadResponseSupport.java` |
| 5 | `tcf-om/.../support/OmFileDownloadAuditListener.java` |
| 6 | `tcf-ui/.../controller/UpdownloadApiController.java` |
| 7 | `tcf-ui/.../service/UpdownloadRelayService.java` |
| 8 | `tcf-ui/src/main/resources/static/ud/updownload.html` |
| 9 | `tcf-ui/src/main/resources/static/_shared/om-admin.js` |
| 10 | `tcf-om/src/main/resources/schema.sql` (`UD_FILE_META`, `OM_FILE_DOWNLOAD_LOG`) |
