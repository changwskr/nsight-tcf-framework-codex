# PDMG 애플리케이션 아키텍처와 개발 가이드 제3부 집필 설계

## 1. 목적

목차의 제3부를 현재 코드에 근거한 개발자·아키텍트용 상세 본문으로 작성한다. HTTP 전문에서 TCF ON/OFF까지 요청 생명주기를 연속적으로 설명하며, 원 목차의 장·절 번호와 제목을 그대로 유지한다.

## 2. 결과물

```text
pdmg-service/docs/PDMG 애플리케이션 아키텍처와 개발 가이드/
├─ 08장.HTTP 요청과 표준 전문.md
├─ 09장.Filter와 Spring MVC.md
├─ 10장.Interceptor와 시스템 선처리.md
├─ 11장.ServiceContext와 GUID.md
├─ 12장.TCF 온라인 거래 프레임워크.md
└─ 13장.TCF OFF 호환 구조.md
```

## 3. 목차 보존

- 8장 `8.1~8.13`, 9장 `9.1~9.13`, 10장 `10.1~10.10`을 유지한다.
- 11장 `11.1~11.14`, 12장 `12.1~12.13`, 13장 `13.1~13.11`을 유지한다.
- 원 목차의 총 74개 절 제목과 순서를 자동 대조한다.
- 도입, 체크리스트, 요약과 근거 문서는 번호 없는 보조 절로 둔다.

## 4. 집필 내용

- 8장: URL, 표준 Header·DTO, GUID·Service ID, JSON 변환, 응답, 상태 코드, CORS와 UI 호출 경계를 기술한다.
- 9장: Filter 생성 조건, Body Cache, JWT, Context·MDC, MVC 변환과 종료 정리를 기술한다.
- 10장: Interceptor 시점, 시스템 선처리, 거래 통제 6개 기준, 이미지로그와 트랜잭션 외부 경계를 기술한다.
- 11장: ServiceContext의 데이터·ThreadLocal 생명주기, GUID, Worker 전파와 보안을 기술한다.
- 12장: Controller, Context, Facade, STF·ETF, Dispatcher·Registry와 정상·실패 흐름을 기술한다.
- 13장: TCF OFF의 조건부 Bean, 직접 Controller, 트랜잭션·Aspect·타임아웃·예외 차이를 기술한다.

## 5. 사실 확인 기준

1. `pdmg-fw`와 `pdmg-service` 실행 코드
2. Spring YAML과 조건부 Bean 설정
3. 테스트 코드
4. 기본 문서, 재분석 문서, 다이어그램 문서

특히 다음을 코드로 확정한다.

- `DefaultFilter`, `ServicePreventionInterceptor`, `ResponseBodyArgumentResolver`의 실제 순서와 책임
- `OnlineTransactionController`의 URL과 Service ID 우선순위
- `ServiceContextHolder` 저장·정리 방식
- `DefaultOnlineTimeoutExecutor`의 Worker Context 전파
- STF·ETF가 실제 호출되는 위치
- TCF OFF Controller가 Facade 또는 Service 중 무엇을 호출하는지
- TCF ON/OFF에 따른 `GlobalExceptionHandler`와 Handler Bean 생성 여부

## 6. 표현 원칙

- AS-IS만 기술하고 구현되지 않은 이상적 TCF 흐름을 현재 동작처럼 쓰지 않는다.
- 시스템 선후처리와 업무 트랜잭션의 범위를 구분한다.
- JWT local 생략, 비-local HMAC 검증과 RS256 발급 구조의 차이를 명시한다.
- Worker Thread 사용과 비동기 HTTP 응답을 구분한다.
- 거래 통제의 실제 지원 필드와 판정 순서를 코드에 맞춘다.
- 개인 경로, 계정, Secret, Token과 개인정보를 포함하지 않는다.

## 7. 검증 기준

- 6개 파일과 74개 절이 존재하고 원 목차와 제목·순서 차이가 없어야 한다.
- 모든 상대 Markdown 링크가 존재해야 한다.
- 클래스명, 설정 키, URL과 조건부 Bean 설명이 코드와 일치해야 한다.
- Markdown 공백 오류와 미완성·민감 문자열이 없어야 한다.

## 7.1 상세 품질 기준

- 각 장은 제1부와 같은 수준의 도입, 학습 목표, 전체 흐름도, AS-IS 코드 분석, 예제, 경계·주의사항, 체크리스트와 요약을 갖춘다.
- 각 번호 절은 한두 문장 요약으로 끝내지 않고 실제 요청에서 해당 단계가 언제·어디서·무엇을 변경하는지 설명한다.
- Filter, Interceptor, Advice, TCF, Worker와 트랜잭션의 실행 순서를 코드 호출부로 증명한다.
- 존재하지만 호출되지 않는 STF·ETF는 `Bean 존재`와 `실행 미연결`을 분리해 표시한다.
- 장당 최소 250줄 상당의 설명 밀도를 확보하되 중복 설명은 상호 링크로 줄인다.

## 8. 범위 밖

- 제4부 이후 집필
- 코드·설정 수정
- TO-BE 인증 통합 또는 TCF 리팩터링
- Word·PDF 변환
