# NSIGHT TCF Framework 개요

## 목적

NSIGHT TCF(Transaction Control Framework)는 업무 시스템의 HTTP/JSON 온라인 거래를 동일한 처리 규칙으로 실행하는 Java 프레임워크다. 업무 개발자는 공통 전처리와 후처리를 반복 구현하지 않고 `serviceId`에 대응하는 업무 Handler와 하위 계층에 집중한다.

## 주요 사용자

- 업무 서비스 개발자: 업무 거래, 규칙과 데이터 접근 구현
- 프레임워크 개발자: 표준 전문, 거래 엔진과 공통 모듈 관리
- 운영 담당자: 거래 통제, 타임아웃, 로그, 세션과 배포 관리
- 테스트 담당자: 표준 응답, 오류 경로와 통합 배포 검증
- 업무 모델 설계자: Model Studio를 통한 업무 모델 정의와 코드 초안 생성

## 핵심 기능

### 표준 거래 처리

- `StandardHeader`, `StandardRequest`, `StandardResponse` 기반 표준 전문
- `serviceId` 기반 Handler 탐색과 실행
- 성공, 업무 오류, 시스템 오류의 표준 응답 변환
- GUID와 Trace ID를 이용한 거래 추적

### 공통 정책

- 헤더 검증, 세션, 인증과 권한 확인
- 거래 통제 정책 적용
- 온라인·트랜잭션·DB 쿼리 타임아웃 적용
- 멱등성 검사
- 거래 로그, 감사 로그와 메트릭 기록

### 운영 및 배포

- 업무별 Spring Boot 독립 실행
- 외부 Tomcat용 WAR 생성과 통합 배포
- OM, Batch, Gateway, JWT, Cache, UI 지원
- Local, Dev, Prod Spring Profile 분리

### 개발 자동화

- `tcf-ai-methodology`: 업무 모델 정의, 검증과 코드 초안 생성
- `tcf-ai-crud-meoy`: 단계별 CRUD 개발 절차와 품질 Gate 지원

## 기술 기준

| 항목 | 기준 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.3.5 |
| 빌드 | Gradle 멀티모듈 |
| 웹 | Spring MVC |
| 데이터 접근 | Spring JDBC, MyBatis 3 |
| 로컬 DB | H2 |
| 캐시 | EhCache, Spring Cache |
| 배포 | 실행형 애플리케이션 및 WAR/Tomcat |
| 테스트 | JUnit 5, Spring Boot Test |

## 서비스 식별 규칙

`serviceId`는 `{BusinessCode}.{Domain}.{action}` 형식을 사용한다.

```text
SV.Customer.selectSummary
IC.Customer.inquiry
OM.User.update
```

- Business Code는 업무 WAR 경계를 나타낸다.
- Domain은 Handler와 업무 모델의 기준이다.
- action은 하나의 거래 행위를 나타낸다.
- 같은 업무와 도메인의 여러 거래는 하나의 Handler가 `serviceIds()`로 등록한다.

## 범위 밖의 사항

- 업무별 상세 정책과 데이터 의미는 각 업무 모듈이 소유한다.
- 코드 생성 결과는 초안이며 자동으로 운영 배포 가능한 완성 코드로 간주하지 않는다.
- 운영 DB 제품과 인프라 구성은 환경별 배포 설정과 운영 문서를 따른다.

