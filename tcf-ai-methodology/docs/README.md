# NSIGHT Model Studio v0.2.0

NSIGHT-TCF-FRAMEWORK 업무모델을 화면에서 정의하고  
Java·Mapper XML·SQL·설계 산출물을 생성하는 **로컬 Spring Boot** 도구입니다.

> 런타임: Spring Boot 3.3 / JDK 21 / H2  
> 원본 Python MVP는 `../ref/nsight_model_studio/`에 보관합니다.  
> Gradle 모듈: `:tcf-ai-methodology` · 포트 **8787**

## 핵심 기능

- 프로젝트·업무코드·도메인·패키지 프로파일 정의
- 화면 ID·이벤트 ID·성공/실패 처리 정의
- ServiceId·거래코드·처리유형·Timeout·권한·감사 정의
- 테이블·컬럼·Java/DB 타입·PK·요청·조건·응답·민감정보 정의
- 모델·Workspace 자동검증
- 동일 도메인의 여러 ServiceId를 **하나의 Handler에 병합**
- **저장된 모델 조회** (키워드·업무코드·도메인·처리유형)
- 코드·SQL·문서 미리보기
- 단일 / 전체 Workspace ZIP 다운로드
- H2 DB 영속화 · classpath 시드(41건) · `POST /api/models/reseed`

## 식별자 계층

```text
businessCode   예: SV, OM          → 업무 WAR
  domainCode   예: Customer, User  → Handler 단위
    ServiceId  예: SV.Customer.selectSummary
```

- `businessCode` 1개 아래에 `domainCode`가 여러 개 존재합니다.
- `domainName`은 한글 표시용이며 코드 생성 키는 `domainCode`입니다.
- ServiceId 가운데 구간은 `domainCode`와 일치해야 합니다.

## 생성 산출물

| 구분 | 파일 |
|------|------|
| 계층 코드 | Handler, Facade, Service, Rule, DAO, Mapper |
| DTO | Request, Response, Criteria, Row (조회 시) |
| 영속 | MyBatis Mapper XML, DDL 초안 |
| 운영 | OM Service Catalog 등록 SQL 초안 |
| 연동 | 표준 HTTP 거래 요청 예시 (`.http`) |
| 설계 | 화면·이벤트 정의서, 거래설계서 |
| 품질 | End-to-End 추적성 CSV, Quality Gate, manifest |
| 테스트 | Rule 단위테스트 골격 |

## 실행

저장소 루트:

```bash
./gradlew :tcf-ai-methodology:bootRun
```

모듈 디렉터리:

```bat
run.bat
```

```bash
./run.sh
```

브라우저:

```text
http://127.0.0.1:8787
```

H2 Console: http://127.0.0.1:8787/h2-console  
JDBC URL 예: `jdbc:h2:file:C:/Users/<user>/nsight-model-studio/models-db`

### IDE 실행 주의

소스의 초록 Run이 `jdt.ls-java-project` classpath를 쓰면  
`SpringApplication` / `JpaRepository` ClassNotFound가 납니다.  
**Gradle bootRun**을 사용하거나 Java LS Workspace를 Clean 한 뒤  
`projectName=tcf-ai-methodology` launch 설정을 쓰십시오.

## 사용 절차

1. (선택) `POST /api/models/reseed` 로 프레임워크 시드 41건을 DB에 적재합니다.
2. **저장된 모델 조회**에서 목록·필터를 확인합니다.
3. 모델을 열거나 `신규 업무모델` / `복제`로 작성합니다.
4. 6단계(프로젝트 → 화면 → 서비스 → 필드 → 검증 → 생성)를 입력합니다.
5. `검증`에서 오류 0건을 확인합니다.
6. `코드 미리보기`로 산출물을 확인합니다.
7. 단건은 상단 `ZIP 생성`, 전체는 좌측 `전체 Workspace 생성`을 사용합니다.
8. ZIP을 대상 업무 모듈에 적용하고 Diff·Compile·Test·리뷰를 수행합니다.

## 테스트

```bash
./gradlew :tcf-ai-methodology:test
```

확인 항목:

- 샘플 모델 검증·생성
- 시드 모델 ERROR 검증 0건
- DB 저장소 로드

## 패키지 프로파일

### CURRENT_SOURCE (기본, 현재 소스 호환)

```text
com.nh.nsight.marketing.sv.entry.handler
com.nh.nsight.marketing.sv.entry.facade
com.nh.nsight.marketing.sv.application.service
com.nh.nsight.marketing.sv.application.rule
com.nh.nsight.marketing.sv.persistence.dao
com.nh.nsight.marketing.sv.persistence.mapper
```

### DOMAIN_FIRST (도메인 우선 목표형)

```text
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
...
```

## 시드·인벤토리

```bash
node tcf-ai-methodology/generate-domain-models.js
```

- 시드: `src/main/resources/data/models-seed.json`
- 목록: [DOMAIN_MODEL_INVENTORY.md](DOMAIN_MODEL_INVENTORY.md)
- 기준 소스 정합성: [SOURCE_ALIGNMENT.md](SOURCE_ALIGNMENT.md)

## 주의사항

- 로컬 개발용입니다. 운영 비밀정보를 입력하지 마십시오.
- 생성 SQL은 DA/DBA 실행계획·인덱스 검토가 필요합니다.
- 생성 Rule은 필수값·길이 골격이며 실제 업무 규칙을 보완해야 합니다.
- 생성 코드를 그대로 운영 반영하지 말고 Diff·리뷰·Compile·Test를 수행하십시오.
- 현재 버전은 DB 역공학, Git 자동병합, OM 직접등록, SSO/RBAC를 포함하지 않습니다.

상세 방법론: [NSIGHT_Automated_Development_Methodology.md](NSIGHT_Automated_Development_Methodology.md)  
요약: [ai-methlogy.md](ai-methlogy.md)
