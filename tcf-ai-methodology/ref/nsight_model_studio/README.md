# NSIGHT Model Studio v0.1.0

NSIGHT-TCF-FRAMEWORK 업무모델을 화면에서 정의하고 Java·Mapper XML·SQL·설계 산출물을 생성하는 로컬 MVP입니다.

## 핵심 기능

- 프로젝트·업무코드·도메인·패키지 프로파일 정의
- 화면 ID·이벤트 ID·성공/실패 처리 정의
- ServiceId·거래코드·처리유형·Timeout·권한·감사 정의
- 테이블·컬럼·Java/DB 타입·PK·요청·조건·응답·민감정보 정의
- 모델·Workspace 자동검증
- 동일 도메인의 여러 ServiceId를 하나의 Handler에 병합
- 코드·SQL·문서 미리보기
- 전체 생성물 ZIP 다운로드

## 생성 산출물

- Handler / Facade / Service / Rule / DAO / Mapper Interface
- Request / Criteria / Response / Row DTO
- MyBatis Mapper XML
- DDL 초안
- OM Service Catalog 등록 SQL 초안
- 표준 HTTP 거래 요청 예시
- 화면·이벤트 정의서
- 거래설계서
- End-to-End 추적성 CSV
- 품질 Gate 체크리스트
- 생성 manifest
- Rule 단위테스트 골격

## 실행

### Windows

```bat
run.bat
```

### Linux/macOS

```bash
chmod +x run.sh
./run.sh
```

브라우저에서 다음 주소를 엽니다.

```text
http://127.0.0.1:8787
```

Python 3.10 이상만 필요하며 외부 패키지를 설치하지 않습니다.

## 사용 절차

1. 샘플 모델을 열어 구조를 확인합니다.
2. `신규 업무모델` 또는 `복제`로 모델을 작성합니다.
3. 프로젝트·화면·ServiceId·테이블·필드를 입력합니다.
4. `검증`을 실행합니다.
5. 오류 0건을 확인한 후 `코드 미리보기`를 실행합니다.
6. 단일 모델은 상단 `ZIP 생성`, 저장 모델 전체는 좌측 `전체 Workspace 생성`을 사용합니다.
7. 생성 ZIP을 대상 NSIGHT 업무 모듈에 적용하고 Diff·Compile·Test·리뷰를 수행합니다.

## 테스트

```bash
python -m unittest discover -s tests -v
```

테스트는 다음을 확인합니다.

- 샘플 모델 검증
- 필수 생성파일 존재
- 동일 도메인 Handler 병합
- ServiceId 중복 차단
- 생성 Java를 TCF/Spring/MyBatis 계약 Stub과 함께 JDK 21로 컴파일

## 패키지 프로파일

### 현재 소스 호환형

```text
com.nh.nsight.marketing.sv.entry.handler
com.nh.nsight.marketing.sv.entry.facade
com.nh.nsight.marketing.sv.application.service
com.nh.nsight.marketing.sv.application.rule
com.nh.nsight.marketing.sv.persistence.dao
com.nh.nsight.marketing.sv.persistence.mapper
```

### 도메인 우선 목표형

```text
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
...
```

## 주의사항

이 버전은 로컬 개발용 MVP입니다.

- 운영 데이터·비밀번호·Token·Private Key를 입력하지 마십시오.
- 생성 SQL은 초안이며 DA/DBA 실행계획·인덱스 검토가 필요합니다.
- 생성 Rule은 필수값·길이 검증 골격이며 실제 업무 규칙을 보완해야 합니다.
- 생성 코드를 그대로 운영 반영하지 말고 Git Diff·코드리뷰·Compile·Test를 수행해야 합니다.
- 현재 버전은 DB 역공학, Git 자동병합, OM 직접등록, SSO/RBAC를 포함하지 않습니다.

상세 방법론은 `docs/NSIGHT_Automated_Development_Methodology.md`를 참조하십시오.
