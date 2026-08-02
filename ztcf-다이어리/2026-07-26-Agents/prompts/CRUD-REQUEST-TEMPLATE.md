# CRUD 요청 템플릿

선택 입력 양식이다. [MASTER-CRUD-DEVELOPER.md](./MASTER-CRUD-DEVELOPER.md)를 붙인 뒤,  
아래를 채워 함께 보내거나 `CRUD 개발 시작:` 다음에 붙여 넣는다.

모르는 칸은 `미정` 또는 비워 둔다. 에이전트가 DISCOVERY 후 필요한 것만 질문한다.

---

## 템플릿

```text
[CRUD 요청]

## 대상
- 대상 모듈:          (예: av-service / 신규)
- 기준(복제) 모듈:    (예: ln-service)
- 업무코드(BC):       (예: AV)
- 도메인:             (예: CustomerContact)
- Handler명:          (예: AvCustomerContactHandler / 미정)

## CRUD 동작 (필요한 것만 Y)
- selectList:
- selectDetail:
- create:
- update:
- delete:

## ServiceId (비우면 BC.Domain.action으로 제안)
- selectList:
- selectDetail:
- create:
- update:
- delete:

## 데이터
- 테이블/View:
- PK:
- 주요 컬럼:
- 검색조건:
- 정렬:
- 페이징:             (예: pageNo/pageSize, 기본 15, 최대 100)
- 삭제방식:           (논리 USE_YN / 물리 / 해당없음)
- 동시성:             (없음 / 낙관적 / 비관적)

## 규칙·오류
- 필수 검증:
- 중복 기준:
- 업무 오류코드 접두:
- 트랜잭션:           (Facade 단일 / 기타)
- Timeout(초):        (예: 5)

## 보안·운영
- 개인정보 컬럼:
- 목록 마스킹:
- 필요 권한:
- 감사 대상:          (Y/N)
- 멱등성:             (Y/N / 조회는 N)

## 선택 산출물 (기본 N — Y만 생성)
- UI 화면:
- 샘플 요청 JSON:
- Service Catalog:
- 도움말/색인:
- schema.sql / 신규 모듈 설정:

## 기존 산출물
- 결과폴더:           (있으면 경로)
- 보호경로:           (덮지 말 파일, 쉼표 구분)

## 메모
- 
```

---

## 최소 예시 (조회만)

```text
[CRUD 요청]
- 대상 모듈: av-service
- 기준 모듈: ln-service
- 업무코드: AV
- 도메인: CustomerContact
- selectList: Y
- selectDetail: Y
- create/update/delete: N
- 테이블: AV_CUSTOMER_CONTACT
- PK: CONTACT_ID
- 검색조건: customerNo(필수), contactType(선택), USE_YN=Y
- 페이징: pageNo/pageSize
- Timeout: 5
- 개인정보 컬럼: CONTACT_VALUE
- 목록 마스킹: Y
- UI 화면: Y
- 샘플 요청 JSON: Y
- Service Catalog: N
```
