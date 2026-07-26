# tcf-ai-methodology — NSIGHT Model Studio

Python MVP(`ref/nsight_model_studio`)를 **Spring Boot 3.3 / JDK 21** 모듈로 이식한  
NSIGHT-TCF 업무모델 정의·검증·코드생성 도구입니다.

| 항목 | 값 |
|------|-----|
| 버전 | 0.2.0 |
| 포트 | `8787` |
| 저장소 | H2 파일 DB (`business_model`) |
| 시드 | 프레임워크 Handler·schema 분석 **41건** |
| 관련 | CRUD 절차 위저드 [`tcf-ai-crud-meoy`](../tcf-ai-crud-meoy/) → http://127.0.0.1:8788 |

## 무엇을 하는가

화면에서 업무모델을 정의하면 다음을 생성합니다.

- Handler / Facade / Service / Rule / DAO / Mapper / Mapper XML
- Request·Response·Criteria·Row DTO
- DDL · OM Service Catalog SQL · `.http` 요청 샘플
- 화면·거래 정의서 · 추적성 CSV · Quality Gate · manifest
- 동일 `businessCode + domainCode`의 여러 ServiceId → **Handler 1개로 병합**

생성물은 **초안**입니다. Diff·Compile·Test·리뷰 후 업무 WAR에 반영하십시오.

## 핵심 개념

```text
businessCode (SV, OM …)     ← 업무 WAR / contextPath
  └── domainCode (Customer) ← Handler 단위 (도메인당 1 Handler)
        └── ServiceId       ← 거래 단위 (SV.Customer.selectSummary)
```

| 필드 | 의미 |
|------|------|
| `businessCode` | 업무코드 2~3자 (SV, IC, EB, OM …). 모듈·WAR 경계 |
| `domainCode` | 도메인 영문 ID (UpperCamelCase). Handler/패키지 기준 |
| `domainName` | 도메인 한글명 (문서·화면용) |
| `ServiceId` | `{BC}.{Domain}.{action}` — TCF 디스패치 키 |

한 `businessCode` 아래에 여러 `domainCode`가 있습니다.  
같은 이름 `Customer`라도 `SV.Customer`와 `IC.Customer`는 **다른 도메인**입니다.

## 실행

```bat
run.bat
```

또는 저장소 루트:

```bash
./gradlew :tcf-ai-methodology:bootRun
```

- UI: http://127.0.0.1:8787  
- H2 Console: http://127.0.0.1:8787/h2-console  

> IDE에서 `AiMethodologyApplication`을 직접 Run 하면 JPA classpath가 빠질 수 있습니다.  
> **Gradle `bootRun`** 또는 Run and Debug의 `tcf-ai-methodology (Gradle project)`를 사용하세요.

## UI 사용

1. **저장된 모델 조회** — DB 모델 검색(업무코드·도메인·처리유형·키워드)
2. **신규 / 사이드바 선택** — 6단계 편집(프로젝트→화면→서비스→필드→검증→생성)
3. **개발 절차서** — `ai-방법론.md` 기반 20단계(0–19) 표준 절차·목차·품질 게이트
4. **검증** / **코드 미리보기**
5. **ZIP 생성** — 현재 모델 1건  
6. **전체 Workspace 생성** — 저장 모델 전체 → `nsight-saved-models.zip`

## 저장·시드

| 항목 | 경로/방법 |
|------|-----------|
| DB 파일 | `%USERPROFILE%/nsight-model-studio/models-db` |
| JDBC URL | `NSIGHT_MODEL_STUDIO_DB_URL` (기본 H2 file) |
| 시드 JSON | `src/main/resources/data/models-seed.json` |
| 시드 재생성 | `node tcf-ai-methodology/generate-domain-models.js` |
| DB 재적재 | `POST /api/models/reseed` |
| 인벤토리 | [docs/DOMAIN_MODEL_INVENTORY.md](docs/DOMAIN_MODEL_INVENTORY.md) |

최초 기동 시 DB가 비어 있으면 레거시 `models.json` 또는 classpath seed를 자동 이관합니다.

## API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/health` | 헬스 (`storage`, `modelCount`) |
| GET | `/api/models?q=` | 목록·검색 |
| GET/POST/PUT/DELETE | `/api/models/{id}` | CRUD |
| POST | `/api/models/{id}/duplicate` | 복제 |
| POST | `/api/models/reseed` | 시드로 DB 교체 |
| GET | `/api/sample` | 샘플 모델 |
| POST | `/api/validate` | 단건 검증 |
| POST | `/api/validate-workspace` | Workspace 검증 |
| POST | `/api/preview` | 산출물 미리보기 |
| POST | `/api/generate` | 현재 모델 ZIP |
| POST | `/api/generate-saved` | 저장 모델 전체 ZIP |

## 테스트

```bash
./gradlew :tcf-ai-methodology:test
```

## 모듈 구조

```text
tcf-ai-methodology/
├── src/main/java/.../aimethodology/
│   ├── AiMethodologyApplication.java
│   ├── config / store / validation / generator / web / model
├── src/main/resources/
│   ├── application.yml
│   ├── static/                 # UI
│   └── data/                   # sample · seed
├── docs/                       # 방법론·정합성·인벤토리
├── generate-domain-models.js   # 시드 생성기
├── ref/nsight_model_studio/    # Python 원본 참조
├── run.bat / run.sh
└── build.gradle
```

## 문서

| 문서 | 내용 |
|------|------|
| [docs/README.md](docs/README.md) | 도구 사용 안내 |
| [docs/DOMAIN_MODEL_INVENTORY.md](docs/DOMAIN_MODEL_INVENTORY.md) | 시드 41건 ServiceId 목록 |
| [docs/SOURCE_ALIGNMENT.md](docs/SOURCE_ALIGNMENT.md) | sv-service 등 기준 소스 정합성 |
| [docs/NSIGHT_Automated_Development_Methodology.md](docs/NSIGHT_Automated_Development_Methodology.md) | 자동화 방법론 본문 |
| [docs/ai-methlogy.md](docs/ai-methlogy.md) | 방법론 요약 |

## 주의

- 운영 비밀번호·Token·Private Key를 입력하지 마십시오.
- 생성 SQL은 DA/DBA 검토가 필요합니다.
- 생성 Rule은 골격이며 실제 업무규칙을 보완해야 합니다.
- 운영 반영 전 Git Diff·Compile·Test·코드리뷰를 수행하십시오.
