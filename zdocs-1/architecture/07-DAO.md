# 07. DAO 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 07 |
| 제목 | DAO Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [01-application-layer.md](01-application-layer.md), [06-naming.md](06-naming.md), [05-exception.md](05-exception.md), [26-mybatis.md](26-mybatis.md), [27-paging.md](27-paging.md) |
| 구현 모듈 | `*-service`, `tcf-om` |
| 대상 | Service/DAO/Mapper 개발자 |

---

## 1. 개요

DAO(Data Access Object)는 업무 계층에서 DB 접근을 캡슐화하는 레이어다.  
TCF 프레임워크 관점에서 DAO는 **Service 아래, Mapper 위**에 위치하며 다음 역할에 집중한다.

| 핵심 책임 | 설명 |
|-----------|------|
| 영속 접근 캡슐화 | SQL 호출 상세를 Service에서 분리 |
| Mapper 위임 | MyBatis Mapper 호출과 파라미터 전달 |
| 결과 반환 | `Map`, `List<Map<String,Object>>`, `int` 등 단순 타입 전달 |
| 기술 독립성 | Service가 MyBatis 세부사항에 직접 의존하지 않게 유지 |

---

## 2. DAO 위치와 호출 구조

```text
Handler
  → Facade (@Transactional)
    → Service
      → Rule (검증)
      → DAO
        → Mapper Interface
          → Mapper XML(SQL)
            → DB
```

DAO는 트랜잭션 경계를 갖지 않고, **Facade의 트랜잭션 컨텍스트 안에서 실행**된다.

---

## 3. 계층 경계 규칙

### 3.1 허용 호출

| From | To | 허용 여부 |
|------|----|-----------|
| Service | DAO | O |
| DAO | Mapper | O |
| DAO | JDBC Template / MyBatis Session (예외적) | O |

### 3.2 금지 호출

| 금지 | 이유 |
|------|------|
| Handler → DAO 직접 호출 | 계층 우회, 트랜잭션/검증 경계 붕괴 |
| Rule → DAO 호출 | 검증과 조회/저장 책임 혼합 |
| DAO → Service 호출 | 역방향 의존, 순환 참조 위험 |
| DAO에서 BusinessException 직접 판단 남발 | 도메인 규칙은 Service/Rule 책임 |

---

## 4. DAO 상세 책임

### 4.1 DAO가 해야 하는 일

- Service에서 전달받은 조건을 DB 파라미터 구조로 정리
- Mapper 메서드 호출 (`select*`, `search*`, `insert*`, `update*`, `merge*`)
- DB 결과를 그대로 반환하거나 최소한의 변환만 수행
- 저장/수정 건수(`int`)를 반환해 Service가 성공/실패를 판단하도록 지원

### 4.2 DAO가 하면 안 되는 일

- 화면/채널별 응답 Map 조립
- 업무 정책(예: 권한, 상태전이) 결정
- 예외 메시지 사용자 친화 가공
- 트랜잭션 시작/종료 직접 제어

---

## 5. 구현 패턴 (MyBatis 기준)

MyBatis 자동설정·DataSource·SQL/XML 패턴 상세: [26-mybatis.md](26-mybatis.md)

### 5.1 클래스/패키지 표준

| 항목 | 규칙 | 예 |
|------|------|----|
| DAO 클래스명 | `{Business}{Domain}Dao` | `OmOperationDao`, `SvSampleDao` |
| DAO 패키지 | `{root}.dao` | `com.nh.nsight.marketing.om.dao` |
| Mapper 인터페이스명 | `{Business}{Domain}Mapper` | `OmOperationMapper` |
| Mapper XML | `{Business}{Domain}Mapper.xml` | `mapper/om/OmOperationMapper.xml` |

### 5.2 코드 스켈레톤

```java
@Repository
public class OmOperationDao {
    private final OmOperationMapper mapper;

    public OmOperationDao(OmOperationMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> searchErrorCodes(Map<String, Object> criteria) {
        return mapper.searchErrorCodes(criteria);
    }

    public int updateErrorCode(Map<String, Object> row) {
        return mapper.updateErrorCode(row);
    }
}
```

---

## 6. Mapper와 SQL ID 설계

DAO 설계 품질은 Mapper/SQL ID 일관성에서 크게 좌우된다.

| 규칙 | 설명 |
|------|------|
| `DAO method == Mapper method == SQL id` | 3자 이름을 동일하게 유지 |
| 액션 접두어 고정 | `select`, `search`, `count`, `insert`, `update`, `merge`, `delete`, `disable` |
| 단건/목록 구분 명확 | `selectXxx`(단건), `searchXxxs`(목록) |
| count 분리 | 페이징 목록은 `search...` + `count...` 세트 구성 |

예:

```text
DAO            : searchErrorCodes(criteria)
Mapper method  : searchErrorCodes(params)
Mapper SQL id  : searchErrorCodes
```

---

## 7. DAO 반환 타입 가이드

| 시나리오 | 권장 반환 타입 | 비고 |
|----------|----------------|------|
| 단건 조회 | `Map<String,Object>` 또는 DTO | null 가능성 처리 필수 |
| 목록 조회 | `List<Map<String,Object>>` | 빈 리스트 허용 |
| 건수 조회 | `int` | 페이징/존재여부 판단 |
| CUD 결과 | `int` | 영향 행 수 (0이면 Service에서 BusinessException 판단) |

권장 원칙:

- null/빈값에 대한 **비즈니스 판단은 Service에서 수행**
- DAO는 데이터 사실(fact)만 전달

---

## 8. 예외 처리 원칙 (DAO 관점)

### 8.1 기본 원칙

- SQL/DB 예외(`DataAccessException` 등)는 DAO에서 과도하게 감싸지 않고 상위로 전파
- 상위(`Service` 또는 TCF `ETF.systemError`)에서 표준 오류로 변환
- 도메인 예외(`BusinessException`)는 원칙적으로 Service/Rule에서 발생

### 8.2 DAO에서 예외 변환이 필요한 경우

아래와 같은 기술 의존 예외를 의미 있는 시스템 예외로 바꾸고 싶을 때만 제한적으로 변환한다.

| 상황 | 처리 |
|------|------|
| 유니크 제약 위반을 도메인 오류로 전환 필요 | Service에서 영향행/중복조회로 처리 우선 |
| 외부 DB 장애 재시도 정책 필요 | DAO보다 인프라 계층/공통 모듈에서 처리 |

---

## 9. 트랜잭션과 DAO

DAO는 트랜잭션 경계를 만들지 않는다. 트랜잭션은 Facade에서 선언한다.

```text
Facade @Transactional
   ├─ Service A
   │   └─ DAO A (Mapper A)
   └─ Service B
       └─ DAO B (Mapper B)
```

원칙:

- 다중 DAO 호출을 하나의 유스케이스로 묶을 때 Facade 단일 트랜잭션 유지
- DAO 단위 개별 트랜잭션 분리는 지양

---

## 10. DAO 설계 체크리스트

1. DAO 클래스명이 도메인 기준으로 명확한가 (`OmOperationDao`)
2. Service가 SQL 세부를 모르고 DAO 메서드만 호출하는가
3. DAO/Mapper/SQL ID 명칭이 일치하는가
4. 목록 조회는 `search + count` 쌍을 제공하는가
5. DAO가 BusinessException 판단을 대신하지 않는가
6. 트랜잭션 선언이 Facade에만 있는가
7. Mapper XML 경로가 `mapper/{bc}/...` 규칙을 따르는가

---

## 11. 좋은 예 / 나쁜 예

### 11.1 좋은 예

```text
Service: OmErrorCodeService.update(...)
  └─ DAO: OmOperationDao.updateErrorCode(...)
      └─ Mapper: OmOperationMapper.updateErrorCode(...)
          └─ SQL id: updateErrorCode
```

특징: 이름 일치, 책임 분리, 추적 용이.

### 11.2 나쁜 예

```text
Service: CommonService.process()
  └─ DAO: OmDao.exec01(...)
      ├─ 내부에서 권한검증/메시지조립
      └─ Mapper: runSql
```

문제: 역할 불명확, 검색성 낮음, 테스트 어려움.

---

## 12. 향후 확장 포인트

| 영역 | 현재 | 확장 방향 |
|------|------|-----------|
| 반환 타입 | `Map` 중심 | 도메인 DTO 점진 도입 |
| 동적 SQL | XML 위주 | SQL Builder/공통 쿼리 컴포넌트 |
| 다중 저장소 | 단일 RDB 중심 | 읽기/쓰기 분리, 캐시 계층 연계 |
| 모니터링 | 로그 중심 | DAO 호출 메트릭·슬로우쿼리 추적 표준화 |

---

## 13. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-om/src/main/java/com/nh/nsight/marketing/om/dao/OmOperationDao.java` | DAO 대표 구현 |
| `tcf-om/src/main/java/com/nh/nsight/marketing/om/dao/OmSampleDao.java` | 샘플 DAO |
| `tcf-om/src/main/java/com/nh/nsight/marketing/om/mapper/OmOperationMapper.java` | Mapper 인터페이스 |
| `tcf-om/src/main/java/com/nh/nsight/marketing/om/mapper/OmSampleMapper.java` | 샘플 Mapper |
| `docs/architecture/01-application-layer.md` | 계층 구조 기준 |
| `docs/architecture/06-naming.md` | DAO/Mapper/SQL ID 구현 네이밍 |
| `docs/architecture/53-naming-conventions.md` | 명명규칙 통합 정의서 |

---

## 14. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — DAO 계층 책임·경계·패턴 정의 |
