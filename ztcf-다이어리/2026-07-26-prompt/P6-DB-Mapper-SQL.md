# P6 — DB·Mapper·SQL 설계 및 검토 프롬프트

사용: `P0` + `P6` + ServiceId·테이블·성능 조건

```text
[P6 DB·SQL 설계 작업]

다음 거래의 DB·Mapper·SQL을 설계하거나 검토하라.

ServiceId: {ServiceId}
처리유형: {조회/등록/변경/삭제}
테이블·View: {객체}
컬럼 정의: {컬럼}
PK·UK: {키}
조회조건: {조건}
예상 건수: {건수}
호출 빈도: {빈도}
목표 응답시간: {시간}
정렬·페이징: {방식}
동시성 요구: {내용}
보관·파티션 요구: {내용}

다음 항목을 검토하라.

1. DB 객체 명명규칙
2. 데이터 타입과 Java 타입 매핑
3. PK·UK·FK·Index
4. 조회조건 선택도
5. 대량조회 위험
6. Full Scan 가능성
7. 정렬·페이징 비용
8. UPDATE·DELETE 영향 행
9. 동시성·Lock·Deadlock
10. Transaction과 Rollback
11. MyBatis Namespace·Statement ID
12. Parameter Binding
13. SQL Timeout과 거래 Timeout
14. 개인정보·마스킹·감사
15. SQL Injection과 동적 SQL 안전성
16. 변경 영향 ServiceId와 화면

결과물:

- 논리 SQL
- Mapper Interface
- Mapper XML
- 필요한 Index 후보
- 정상·경계·오류 데이터
- 실행계획 확인 항목
- 성능시험 시나리오
- DBA 검토 체크리스트
- 영향 거래 목록

통계정보와 실행계획이 제공되지 않았다면
성능을 확정적으로 단정하지 말고 위험 후보로 표시하라.
```
