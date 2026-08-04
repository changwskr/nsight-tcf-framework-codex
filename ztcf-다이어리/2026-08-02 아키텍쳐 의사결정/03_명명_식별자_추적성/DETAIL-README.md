# 명명·식별자·추적성 TASK 상세 설명서

이 문서는 STD 영역의 상세 설명서 색인이다. 요약본은 빠른 판단에, **-detail.md** 문서는 배경·절차·예시·검증 기준을 이해하는 데 사용한다.

| ID | 상세 설명서 | 우선순위 | 주관 |
|---|---|---:|---|
| [STD-01](./STD-01-화면-ID-detail.md) | 화면 ID | P0 | UI |
| [STD-02](./STD-02-화면-이벤트-ID-detail.md) | 화면 이벤트 ID | P0 | UI |
| [STD-03](./STD-03-ServiceId-detail.md) | ServiceId | P0 | FW |
| [STD-04](./STD-04-거래코드-detail.md) | 거래코드 | P0 | FW |
| [STD-05](./STD-05-Java-BASE-패키지-detail.md) | Java BASE 패키지 | P0 | AA |
| [STD-06](./STD-06-업무-패키지-detail.md) | 업무 패키지 | P0 | AA |
| [STD-07](./STD-07-클래스-명명-detail.md) | 클래스 명명 | P0 | AA |
| [STD-08](./STD-08-메서드-명명-detail.md) | 메서드 명명 | P0 | AA |
| [STD-09](./STD-09-DTO-명명-detail.md) | DTO 명명 | P0 | AA |
| [STD-10](./STD-10-Mapper·SQL-ID-detail.md) | Mapper·SQL ID | P0 | DA |
| [STD-11](./STD-11-DB-객체-명명-detail.md) | DB 객체 명명 | P0 | DA |
| [STD-12](./STD-12-환경설정-Key-detail.md) | 환경설정 Key | P1 | FW |
| [STD-13](./STD-13-배치·파일-ID-detail.md) | 배치·파일 ID | P1 | AA |
| [STD-14](./STD-14-GUID·TraceId-detail.md) | GUID·TraceId | P0 | FW |
| [STD-15](./STD-15-명명-자동검증-detail.md) | 명명 자동검증 | P1 | DVO |

## 읽는 순서

1. 상세본의 결정 카드와 용어를 읽는다.
2. 현재 NSIGHT 확인 기준과 참조 문서를 실제 코드·설정과 대조한다.
3. 결정 질문에 답하고 대안을 비교한다.
4. ADR 승인 후 구현·테스트·자동검증·운영 증적을 연결한다.
