# 자동검증 및 품질 Gate

## 생성 전 Gate

- [ ] 화면 ID·이벤트 ID·ServiceId·거래코드 형식 검증
- [ ] ServiceId·거래코드 중복 검증
- [ ] 업무코드·도메인·패키지 정합성 검증
- [ ] 조회/변경 유형과 거래코드 유형 정합성 검증
- [ ] 필수 요청·조건·응답 필드 검증
- [ ] 변경 거래 감사대상 검증
- [ ] 민감정보 마스킹 규칙 검증

## 코드 Gate

- [ ] Handler는 ServiceId 분기와 Facade 호출만 수행
- [ ] Transaction 경계는 Facade에 위치
- [ ] Service는 Mapper를 직접 호출하지 않음
- [ ] Rule은 DB/외부 시스템을 호출하지 않음
- [ ] DAO Method와 Mapper Statement ID는 1:1
- [ ] Mapper namespace와 Java Interface FQCN 일치
- [ ] SQL에 안전한 WHERE 조건 존재
- [ ] TCF Timeout ≥ DB/외부 호출 Timeout 합계 검토

## CI/CD Gate 예시

```text
1. model-validate
2. code-generate
3. compileJava
4. unitTest
5. ArchUnit 계층검사
6. ServiceId 중복검사
7. Mapper namespace/SQL ID 검사
8. DDL/SQL 정적검사
9. OM Catalog 등록정보 비교
10. 산출물 추적성 누락검사
```
