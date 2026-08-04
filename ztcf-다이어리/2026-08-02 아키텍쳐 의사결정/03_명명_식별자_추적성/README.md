# 명명·식별자·추적성 아키텍처 의사결정

이 디렉터리는 STD 영역의 TASK 설명서 모음이다. 각 문서는 **결정 전 초안**이며, 실제 구현·설정·테스트를 확인하고 ARB에서 승인한 뒤 ADR 기준선으로 전환한다.

## 사용 방법

1. 우선순위와 의존 TASK를 확인한다.
2. 개별 문서의 현행 확인사항과 참조 문서를 코드·설정 기준으로 검증한다.
3. 대안 비교, PoC, 영향분석 후 결정안을 승인한다.
4. 개발표준·공통 구현·샘플·자동검증·운영 증적까지 연결한다.

## TASK 목록

| ID | 의사결정 사항 | 우선순위 | 주관 | 승인 |
|---|---|---:|---|---|
| [STD-01](./STD-01-화면-ID.md) | 화면 ID | P0 | UI | AA |
| [STD-02](./STD-02-화면-이벤트-ID.md) | 화면 이벤트 ID | P0 | UI | AA |
| [STD-03](./STD-03-ServiceId.md) | ServiceId | P0 | FW | AA |
| [STD-04](./STD-04-거래코드.md) | 거래코드 | P0 | FW | AA |
| [STD-05](./STD-05-Java-BASE-패키지.md) | Java BASE 패키지 | P0 | AA | SA |
| [STD-06](./STD-06-업무-패키지.md) | 업무 패키지 | P0 | AA | SA |
| [STD-07](./STD-07-클래스-명명.md) | 클래스 명명 | P0 | AA | AA |
| [STD-08](./STD-08-메서드-명명.md) | 메서드 명명 | P0 | AA | AA |
| [STD-09](./STD-09-DTO-명명.md) | DTO 명명 | P0 | AA | AA |
| [STD-10](./STD-10-Mapper·SQL-ID.md) | Mapper·SQL ID | P0 | DA | DA |
| [STD-11](./STD-11-DB-객체-명명.md) | DB 객체 명명 | P0 | DA | SA |
| [STD-12](./STD-12-환경설정-Key.md) | 환경설정 Key | P1 | FW | AA |
| [STD-13](./STD-13-배치·파일-ID.md) | 배치·파일 ID | P1 | AA | SA |
| [STD-14](./STD-14-GUID·TraceId.md) | GUID·TraceId | P0 | FW | AA |
| [STD-15](./STD-15-명명-자동검증.md) | 명명 자동검증 | P1 | DVO | AA |

## 공통 완료 기준

```text
ADR 승인
+ 개발표준 반영
+ 공통 샘플 또는 모듈 제공
+ 테스트 기준 및 결과
+ CI/CD 자동검증
+ 업무팀·운영 적용 확인
= TASK 완료
```

## 기준 문서

- [아키텍처 의사결정 사항 목록](../농협%20상호금융%20NSIGHT%20아키텍처%20의사결정%20사항%20목록.md)
- [TASK별 통합 방안서](../2026-08-02-아키테처-의사결정-TASK-상세.md)
