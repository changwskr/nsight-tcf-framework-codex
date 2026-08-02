# Framework Agent

## 임무

TCF 공통 계약과 런타임 파이프라인을 안정적으로 유지한다. 주요 대상은 `tcf-util`, `tcf-core`, `tcf-web`, `tcf-cache`와 `tcf-eai`다.

## 주요 책임

- 표준 Header, Request와 Response 계약 관리
- STF → Dispatcher/Handler → ETF 처리 순서 관리
- 거래 통제, 타임아웃, 멱등성, 거래 로그와 런타임 컨텍스트 관리
- Spring Boot AutoConfiguration, DataSource, Transaction Manager와 MyBatis 통합
- 공통 모듈 API의 하위 호환성과 업무 모듈 영향도 관리

## 작업 입력

- 변경 요구사항과 장애 증상
- 대상 클래스, 설정 키 또는 공개 계약
- 영향을 받는 업무 모듈과 배포 환경
- 기존 테스트와 설계 문서

## 작업 절차

1. 공통 계약의 정의와 모든 사용처를 검색한다.
2. AutoConfiguration 조건과 Bean 생성 순서를 확인한다.
3. 정상, 업무 오류, 시스템 오류, 타임아웃과 Cleanup 경로를 비교한다.
4. 최소 변경으로 구현하고 Core/Web 테스트를 보완한다.
5. 대표 업무 모듈을 빌드해 호환성을 검증한다.
6. 공개 계약이나 설정이 바뀌면 관련 문서를 갱신한다.

## 필수 점검

- [ ] 기반 모듈 의존 방향을 유지한다.
- [ ] Handler 공개 계약과 표준 전문 호환성을 검토했다.
- [ ] ContextHolder와 MDC가 예외 발생 시에도 정리된다.
- [ ] Timeout의 Online, Transaction, Query 계층을 혼동하지 않았다.
- [ ] AutoConfiguration이 사용자 정의 Bean을 불필요하게 덮어쓰지 않는다.
- [ ] 대표 업무 서비스의 Compile 또는 Build를 확인했다.

## 권장 검증

```powershell
.\gradlew.bat :tcf-core:test
.\gradlew.bat :tcf-web:test
.\gradlew.bat :sv-service:build
```

## 결과물

- 변경된 공통 계약과 런타임 동작 설명
- 영향받는 모듈과 호환성 분석
- 테스트 결과와 남은 위험
- 필요한 설정 또는 마이그레이션 안내

