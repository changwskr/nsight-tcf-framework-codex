# C09 — SQL-Mapper

**선행:** [C08-패키지-프로그램구조.md](./C08-패키지-프로그램구조.md)  
**다음:** [C10-트랜잭션-오류-Timeout.md](./C10-트랜잭션-오류-Timeout.md)  
**결과 파일:** [`결과/C09-SQL-Mapper.md`](./결과/C09-SQL-Mapper.md)

## 이 단계에서 얻는 것

SQL·Mapper XML

## 지금 이 질문을 붙여 넣으세요

```text
C-MASTER를 적용한다. 결과/_확정정보원장.md 와 이전 결과/C*.md 를 인용한다.
코드는 C14 Gate 전 쓰지 마라. 질문 1개씩.
단계가 끝나면 결과/C09-SQL-Mapper.md 와 원장을 파일로 갱신한 뒤
「다음 단계로 갈까요?」만 물어라.

목록 조회에서 필요한 검색조건과 정렬 순서를 알려 주세요.

조회만이면 상세 조건만, CUD면 이어 등록·변경·삭제 SQL 요건을 하나씩.
```

## 추가 확인 (질문 1개씩)

목록: 동적WHERE, 페이징, count, 논리삭제제외, 최대건수.
안전: SELECT*금지, UPDATE/DELETE에 PK조건, 동적ORDER BY 주입금지.
Mapper Interface↔XML namespace↔id↔DAO↔Service↔ServiceId 정합.
SQL 성능·인덱스는 사람 승인(방법론).

## Gate

- Service↔SQL 연결
- 테이블 정의 부합
- UPDATE/DELETE 안전

## 결과 저장

`결과/C09-SQL-Mapper.md` 상단 확정표 + 산출물 본문. 원장 `_확정정보원장.md` 동기화.
