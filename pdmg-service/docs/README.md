# PDMG AS-IS 문서 안내

기준일: 2026-08-09  
정본: 현재 `pdmg-service`, `pdmg-fw`, `pdmg-ui` 실행 소스와 설정

## 먼저 읽을 문서

1. [00.BigPicture Tx 흐름.md](./00.BigPicture%20Tx%20흐름.md)
2. [01.트랜잭션처리 변경.md](./01.트랜잭션처리%20변경.md)
3. [02.어플리케이션 컴포넌트 구조.md](./02.어플리케이션%20컴포넌트%20구조.md)
4. [03.어플리케이션 레이어드 아키텍처.md](./03.어플리케이션%20레이어드%20아키텍처.md)
5. [04.패키지구조.md](./04.패키지구조.md)
6. [05.전체 빅픽처 흐름.md](./05.전체%20빅픽처%20흐름.md)
7. [09.서비스ID.md](./09.서비스ID.md)
8. [10.전문.md](./10.전문.md)
9. [11.Http CORS적용.md](./11.Http%20CORS적용.md)
10. [11.예외처리.md](./11.예외처리.md)
11. [12.http요청.md](./12.http요청.md)
12. [20.타임아웃.md](./20.타임아웃.md)

## 현재 실행 계약

| 항목 | AS-IS |
|---|---|
| UI 호출 | 브라우저가 `pdmg-service`를 직접 호출하며 CORS를 사용한다. `/api/relay`는 하위 호환이다. |
| 요청 | `POST /{serviceId}` + `{ hdr_nhnis, dto }` |
| 서비스 ID | `ServiceContext.rms_svc_c` → 요청 JSON → path 순서로 선택한다. 불일치 강제 검증은 없다. |
| 성공 응답 | `{ hdr_nhnis, dto }` |
| 알려진 실패 | `{ hdr_nhnis, result: NH_NIS_ERR_DTO }` |
| Filter 실패 | `sendError(400/401)`이며 표준 `result`와 CORS 헤더가 보장되지 않는다. |
| Timeout ON TX | Worker `TransactionTemplate`이 외곽 TX를 시작하고 Facade `REQUIRED`가 참여한다. |
| Timeout OFF TX | Facade `@Transactional`이 TX를 시작한다. |
| 업무 패키지 | `nhnis.mg.co.a.*` 단일 구조 |
| 등록 거래 | `mgcoa5530S0`, `mgcoa8888S0/D0`, `mgcoa9999S0`, `mgcoa9000S0/C0/U0/D0` |

## 현재 확인된 구현 주의사항

- URL path와 `rms_svc_c`가 다르면 헤더 값이 우선될 수 있다.
- UI 직접 호출 코드는 `Authorization`을 보내지 않아 non-local JWT 환경에서 401이 될 수 있다.
- `GlobalExceptionHandler`에 일반 `Exception` Handler가 없어 모든 오류가 `result`로 통일되지는 않는다.
- `BizException` 메시지 사전이 중앙 Handler에 연결되지 않았다.
- `mgcoa5530Service`의 타임아웃 시험 코드는 인터럽트 후 업무를 계속해 작업 취소를 보장하지 않는다.
- `DefaultFilter` 조기 오류 응답에는 CORS 헤더가 누락될 수 있다.
- 공통 설정에 `spring.profiles.active: local`이 있고 prod 전용 Profile은 없다.

## `-1.md` 파일

파일명이 `-1.md`로 끝나는 문서는 특정 시점의 분석 결과를 보관한 참고 기록이다. 코드 변경 전 상태, 개인 PC 절대 경로 또는 당시 판단을 포함할 수 있으므로 **AS-IS 정본으로 사용하지 않는다**. 동일 번호의 `-1` 없는 문서와 본 README를 우선한다.

## 문서 작성 규칙

- 실행 코드와 설정을 문서보다 우선한다.
- 구현되지 않은 개선안은 `현행`으로 표현하지 않는다.
- 개인 PC 절대 경로 대신 저장소 상대 경로를 사용한다.
- 성공 `dto`와 실패 `result`를 구분한다.
- Timeout 응답과 실제 Worker 중단을 같은 의미로 표현하지 않는다.
