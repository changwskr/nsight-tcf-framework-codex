# 범용 CRUD Service 프롬프트

이 문서는 `pdmg-service` CRUD를 가장 간단하게 요청하는 단일 프롬프트다.
세부 항목은 [CRUD서비스프롬프트가이드.md](CRUD서비스프롬프트가이드.md), 이름 규칙은 [네이밍원칙.md](네이밍원칙.md) · [MG-NAMING_CONVENTION.md](MG-NAMING_CONVENTION.md)를 참고한다.

## 1. 사용법

1. 아래 최소 입력 예시를 내 업무 정보로 바꾼다.
2. 모르는 값은 삭제하지 말고 `[미정-질문 필요]`로 둔다.
3. 작성한 입력과 3장의 범용 프롬프트를 함께 AI에게 전달한다.

AI가 테이블, PK, serviceId, 오류코드, timeout을 추측하게 하지 않는 것이 핵심이다.

## 2. 최소 입력 예시

```text
프로그램 ID: mgcoa8888
업무명: 영업 팁 관리
패키지: nhnis.mg.co.a
serviceId: mgcoa8888S0 / mgcoa8888D0 (구분자별)
Mapper: rdw.mg.co.a/

테이블: TB_CR_AH_SALES_TIP_RACT
PK: TRT_BRC, TRTMN_ENO, SALZ_TIP_KDC, BAS_DT

컬럼:
- TRT_BRC / trtBrc / String / 필수 / 취급점 코드
- TRTMN_ENO / trtmnEno / String / 필수 / 취급자 사번
- SALZ_TIP_KDC / salzTipKdc / String / 필수 / 영업 팁 종류
- BAS_DT / basDt / String / 필수 / 기준일자 yyyyMMdd
- PRTO_CN / prtoCn / String / 선택 / 우선 처리 내용
- INQ_CN / inqCn / String / 선택 / 조회 내용
- INP_CN / inpCn / String / 선택 / 입력 내용

목록 검색: salzTipKdc 정확 일치
정렬: BAS_DT DESC, TRT_BRC, TRTMN_ENO
페이징: pageNo 기본 1, pageSize 기본 20, 최대 100
삭제 방식: 물리 삭제

참조 구조: mgcoa8888 (Handler/Facade/Service/DAO/DTO)
등록/수정 serviceId·오류 코드: [미정-질문 필요]
쓰기 트랜잭션 timeout: [미정-질문 필요]
권한: [미정-질문 필요]
```

## 3. 복사용 범용 프롬프트

```text
pdmg-service에 아래 정보를 기준으로 목록·상세·등록·수정·삭제 CRUD를 만들어줘.

[입력]
프로그램 ID: [프로그램ID, 예 mgcoaXXXX]
업무명: [업무명]
패키지: nhnis.mg.co.a (계층: entry.handler / application.facade·service·controller / dto / persistence.dao)
serviceId: [mgcoaXXXXS0, C0, U0, D0 …]
Mapper: rdw.mg.co.a/
테이블: [테이블명]
PK: [PK 컬럼]
컬럼: [DB 컬럼 / Java 필드 / Java 타입 / 필수 여부 / 설명]
목록 검색: [검색 필드와 비교 방식]
정렬: [정렬 순서]
페이징: [기본값과 최대값]
삭제 방식: [물리/논리]
오류 코드: [필수값 / 중복 / 미존재 / 충돌 / 삭제불가]
트랜잭션 timeout: [초 또는 미정-질문 필요]
권한: [권한 또는 미정-질문 필요]

[필수 작업 원칙]
1. 작업 범위는 pdmg-service만이다. pdmg-ui와 다른 프로젝트는 수정하지 마라. (UI까지 필요하면 명시)
2. docs/네이밍원칙.md, docs/MG-NAMING_CONVENTION.md, 현재 mgcoa8888 구조를 먼저 읽어라.
3. 사용자 기존 변경을 보존하고 관련 없는 리팩터링을 하지 마라.
4. 빠진 DB·업무·serviceId·오류·timeout·권한 정보는 추측하지 말고 질문해라.
5. 먼저 설계, 테스트 시나리오, 생성·수정 파일을 제시하고 승인 후 구현해라.
6. TCF ON: Handler → Facade(@Transactional) → Service → DAO/MyBatis. TCF OFF 호환 시 Controller는 application.controller.
7. 요청/응답은 { hdr_nhnis, dto } 봉투. DTO는 nhnis.mg.co.a.dto 의 *DTOin/*DTOout.
8. @TcfTransaction / StandardRequestDto / ETF 를 쓰지 마라. (PDMG 현행이 아님)
9. 조회는 Facade에 @Transactional(rdwTransactionManager, readOnly=true), 쓰기는 rollbackFor=Exception.class.
10. 공통 온라인 timeout은 OnlineTimeoutExecutor가 담당한다. Facade timeout은 입력값이 있을 때만 두고 미정이면 임의로 설정하지 마라.
11. 업무 오류는 BizException, 메시지는 exceptionCode.yml, 실패 응답은 NH_NIS_ERR_DTO / GlobalExceptionHandler.
12. DAO 메서드명과 MyBatis statement id, namespace, DTO 필드와 SQL alias를 일치시켜라.
13. 바인딩 변수 #{...}를 사용하고 PK 조건 없는 UPDATE/DELETE를 금지해라.
14. Oracle 운영 SQL과 H2 로컬 실행을 모두 고려해라.
15. 목록의 count SQL과 조회 SQL 검색조건을 일치시켜라.
16. 필수값, 중복, 상세 0건, 수정 0건, 삭제, rollback 테스트를 먼저 작성해라.
17. gradlew.bat test를 실행하고 NO-SOURCE이면 테스트가 없다고 명시해라.
18. 완료 후 변경 파일, serviceId, 트랜잭션, 오류 코드, 테스트 명령과 실제 결과를 보고해라.

지금 바로 구현하지 말고 먼저 저장소 조사 결과와 빠진 요구사항 질문부터 시작해줘.
```

## 4. 결과 확인 체크리스트

- [ ] `pdmg-service`만 변경했는가?
- [ ] 이름이 `docs/네이밍원칙.md` / `MG-NAMING_CONVENTION.md`와 맞는가?
- [ ] Handler + Facade + Service + DAO + dto 패키지가 맞는가?
- [ ] `{ hdr_nhnis, dto }` 전문과 serviceId URL을 사용했는가?
- [ ] TX는 Facade, 조회 readOnly / 쓰기 rollback인가?
- [ ] timeout과 업무 규칙을 임의로 만들지 않았는가?
- [ ] `BizException`, `exceptionCode.yml`, `NH_NIS_ERR_DTO` 흐름을 지켰는가?
- [ ] DAO와 MyBatis ID·namespace·alias가 일치하는가?
- [ ] UPDATE/DELETE에 PK 조건이 있는가?
- [ ] Oracle과 H2에서 동작 가능한가?
- [ ] 정상·실패·rollback 테스트가 있는가?
- [ ] 비밀값·개인정보·관련 없는 변경이 없는가?
- [ ] `gradlew.bat test`의 실제 결과를 보고했는가?
