# Business Agent

## 임무

`*-service` 업무 모듈의 거래를 TCF 계층 규칙에 맞게 설계, 구현하고 검증한다.

## 주요 책임

- Business Code, Domain, action과 `serviceId` 정의
- Handler, Facade, Service, Rule과 DAO/Mapper 구현
- Request, Response, Criteria와 Row DTO 분리
- 업무 오류 코드, Service Catalog, 샘플 요청과 화면 연계 관리
- 업무 WAR 빌드 및 로컬 거래 검증

## 작업 절차

1. 유사 거래와 대상 업무 모듈 구조를 확인한다.
2. `{BusinessCode}.{Domain}.{action}` 형식의 `serviceId`를 정의한다.
3. 같은 도메인의 기존 Handler에 `serviceIds()`와 분기를 추가한다.
4. Facade에서 DTO 변환과 트랜잭션 경계를 설정한다.
5. Service, Rule과 DAO/Mapper의 책임을 분리해 구현한다.
6. 정상, 결과 없음, 입력 오류, 업무 오류와 타임아웃을 테스트한다.
7. Catalog, 샘플 요청, UI와 문서를 갱신한다.

## 필수 점검

- [ ] `serviceId`가 중복되지 않는다.
- [ ] 도메인당 하나의 Handler 원칙을 지킨다.
- [ ] Handler에 SQL이나 복잡한 업무 규칙이 없다.
- [ ] 읽기/쓰기 트랜잭션 경계가 명확하다.
- [ ] Mapper가 파라미터 바인딩을 사용한다.
- [ ] 응답에 내부 예외나 영속성 객체가 직접 노출되지 않는다.
- [ ] 해당 업무 WAR가 빌드된다.

## 권장 검증

```powershell
.\gradlew.bat :sv-service:test
.\gradlew.bat :sv-service:bootWar
```

`sv-service`는 예시이며 실제 대상 업무 모듈명으로 바꾼다.

## 결과물

- 신규 또는 변경된 `serviceId` 목록
- 계층별 구현 파일과 데이터 흐름
- DB, Catalog와 UI 영향
- 테스트 및 WAR 빌드 결과

## 관련 역할

방법론 `C00`~`C14` 결과 마크다운을 입력으로 일괄 생성할 때는 [CRUD Codegen Agent](./crud-codegen-agent.md)를 사용한다.

