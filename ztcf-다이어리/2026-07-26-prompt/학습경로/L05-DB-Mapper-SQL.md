# L05 — DB·Mapper·SQL (무엇이 저장·조회되는가)

**선행:** [L04](./L04-ServiceId-와-OM.md)  
**다음:** [L06-화면-추적성.md](./L06-화면-추적성.md)  
**관련 프롬프트:** [`../P6-DB-Mapper-SQL.md`](../P6-DB-Mapper-SQL.md)

## 이 단계에서 얻는 것

persistence 계층에서 **소유권 · XML Statement · 페이징 · 영향 행**을 본다.  
“SQL은 Mapper에만”이라는 규칙을 실행 관점으로 체화한다.

## 왜 이 순서인가

거래 키(L04)가 정해진 뒤, 그 거래가 만지는 데이터 경계를 본다.  
화면 추적(L06) 전에 데이터 축을 고정해야 매트릭스가 흔들리지 않는다.

## 지금 이 질문을 붙여 넣으세요

```text
P0 + P6을 적용한다. 운영 SQL을 추측해 확정하지 마라.

대상 거래: eb-service의 대표 조회 1건 + (가능하면) AV.Sample.inquiry

1. Mapper Interface ↔ XML namespace/statement ID 일치 여부를 실제 파일로 검증하라.
2. DAO가 Mapper를 감싸는 이유, Service가 Mapper를 직접 부르면 안 되는 이유를
   이 코드 구조로 설명하라.
3. 동적 WHERE / OFFSET·FETCH 또는 페이징 패턴이 있으면 동작과 위험을 설명해라.
4. count + search 이중 쿼리 패턴이 있으면 왜 필요한지, 없으면 대안을 사실/예시로 구분해라.
5. 데이터 소유권: 이 테이블을 다른 업무 WAR가 직접 UPDATE하면 왜 금지인지
   프레임워크 원칙과 연결하라.
6. 통계/실행계획이 없을 때 성능을 "확정"하면 안 되는 이유를 P6 기준으로 상기시켜라.

결과: SQL 추적표 (ServiceId | Mapper | Statement | Table | 비고)
스키마가 local 전용이면 '설계 예시/local'로 표시하라.
```

## 읽을 자료 / 열 파일

- `*/persistence/mapper/*.java`, `resources/mapper/**/*.xml`
- `schema.sql` (local)
- `av-service` Sample Mapper (우리가 만든 것)

## 자기 점검

1. Interface method명과 XML id가 다르면 언제 터지는가?  
2. UPDATE 영향 행 0건을 무시하면 무엇이 위험한가?  
3. 타 업무 테이블 직접 조인의 대안은? (EAI 등 — 확인된 사실만)  

## 통과 기준

- 한 ServiceId에 대해 Mapper·XML·Table을 한 줄로 연결할 수 있다.  
