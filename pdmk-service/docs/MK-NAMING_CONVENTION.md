---
name: MK-NAMING_CONVENTION
description: 농협 상호금융 PDMK 프로젝트 소스 코드 명명 규칙. Controller, Service, DAO, DTO, SubDTO, MyBatis Mapper/SQL ID, 변수명, 주석 및 파일 생성 규칙을 정의한다.
---

# PDMK Naming Convention Guide

## 1. 목적

이 문서는 PDMK 프로젝트의 소스 코드 명명 규칙을 정의한다.  
모든 개발자는 기준 소스를 임의로 참고하여 변형하기보다 본 규칙에 따라 Controller, Service, DAO, DTO, SubDTO, resources 등을 생성한다.

기준 프로그램 샘플: **mkcoa8888** (조회 `mkcoa8888S0` + 삭제 `mkcoa8888D0` 통합 Controller)

---

## 2. 애플리케이션 분류 체계

### 2.1 대구분

- 애플리케이션 그룹 코드: `MK`

### 2.2 업무구분

| 코드 | 업무구분    |
| ---- | ----------- |
| CO   | 공통        |
| IC   | 통합고객    |
| MS   | 미니 상품부 |
| SA   | 사업관리    |

### 2.3 세부업무구분

| 코드 | 세부구분 |
| ---- | -------- |
| A    | 상담     |
| B    | 고객     |

---

# 3. 서비스 ID 규칙

## 3.1 기본 구조

```text
[대구분 2자][업무구분 2자][세부업무구분 1자][식별번호 4자][구분자 1자][순번 1자]
```

예시:

```text
mkcoa8888S0
```

> 구분자+순번을 합쳐 “식별자 2자”로 부르기도 하나, 명명·검증 시에는 **구분자·순번을 각각 1자**로 본다 (§3.2).

## 3.2 구성요소

| 구분         | 길이 | 예시        | 설명                            |
| ------------ | ---: | ----------- | ------------------------------- |
| 대구분       |    2 | mk          | 애플리케이션 그룹               |
| 업무구분     |    2 | co          | CO, IC, MS, SA                  |
| 세부업무구분 |    1 | a           | A, B                            |
| 식별번호     |    4 | 8888        | 화면번호 또는 일반번호          |
| 구분자       |    1 | S/C/U/D/A/R | 조회/등록/수정/삭제/혼합/리포트 |
| 순번         |    1 | 0~9, A~Z    | 동일 기능 내 순번               |

## 3.3 구분자

| 구분자 | 의미   |
| ------ | ------ |
| S      | 조회   |
| C      | 등록   |
| U      | 수정   |
| D      | 삭제   |
| A      | 혼합   |
| R      | 리포트 |

## 3.4 예시

```text
조회   : mkcoa8888S0
등록   : mkcoa0000C0
수정   : mkcoa0000U0
삭제   : mkcoa8888D0
혼합   : mkcoa0000A0
리포트 : mkcoa0000R0
화면예 : mkcoaZ0000S0
```

---

# 4. Java 패키지 및 파일명 규칙

## 4.1 기본 패키지

```text
src/main/java
nhnis.[대구분소문자].[업무구분소문자].[세부업무구분소문자]
```

하위 패키지:

```text
controller
service
dao
dto
```

예시:

```text
nhnis.mk.co.a.controller
nhnis.mk.co.a.service
nhnis.mk.co.a.dao
nhnis.mk.co.a.dto
```

## 4.2 Controller / Service / DAO

Controller, Service, DAO 파일명에는 서비스 구분자(S/C/U/D/A/R)를 포함하지 않고 식별번호까지 사용한다.

```text
[대구분][업무구분][세부업무구분][식별번호4자리]Controller.java
[대구분][업무구분][세부업무구분][식별번호4자리]Service.java
[대구분][업무구분][세부업무구분][식별번호4자리]DAO.java
```

예시:

```text
mkcoa8888Controller.java
mkcoa8888Service.java
mkcoa8888DAO.java
```

## 4.3 DTO

DTO는 서비스 ID 전체를 포함한다.

```text
[서비스ID]DTOin.java
[서비스ID]DTOout.java
```

예시:

```text
mkcoa8888S0DTOin.java
mkcoa8888S0DTOout.java
mkcoa8888D0DTOin.java
mkcoa8888D0DTOout.java
```

## 4.4 디렉토리 예시

```text
src/main/java/nhnis/mk/co/a/controller/
src/main/java/nhnis/mk/co/a/service/
src/main/java/nhnis/mk/co/a/dao/
src/main/java/nhnis/mk/co/a/dto/
```

---

# 5. Controller 규칙

## 5.1 파일명

```text
[대구분][업무구분][세부업무구분][식별번호4자리]Controller.java
```

클래스명 예시:

```java
mkcoa8888Controller
```

## 5.2 서비스 구분 처리

Controller 클래스명에는 조회/등록/수정 등의 구분자를 넣지 않는다.  
기능 구분은 메서드와 `@PostMapping` 단위로 처리한다.  
**클래스 레벨 `@RequestMapping` 은 사용하지 않는다.**

예시:

```text
mkcoa8888S0  조회
mkcoa8888D0  삭제
mkcoa5530S0  조회
mkcoa0000C0  등록
mkcoa0000U0  수정
mkcoa0000A0  혼합
mkcoa0000R0  출력물
```

## 5.3 애노테이션

클래스:

```java
@Slf4j
@RestController
```

메서드:

```java
@PostMapping("/서비스ID")
```

입력 (프레임워크 전용):

```java
@RequestBody [서비스ID]DTOin input
```

> `@RequestBody` 는 `nhnis.fw.commons.resolver.RequestBody` 를 사용한다.  
> 요청 JSON의 `dto` 노드를 DTO로 바인딩한다.

## 5.4 메서드 시그니처

```java
public [서비스ID]DTOout [서비스ID](
        @RequestBody [서비스ID]DTOin input
) throws Throwable
```

## 5.5 통합 Controller 적용

동일 식별번호 아래 서비스가 2개 이상 존재하면 하나의 Controller로 통합한다.

예:

```text
mkcoa8888S0
mkcoa8888D0
→ mkcoa8888Controller
```

식별번호가 다르면 별도 Controller를 사용한다.

---

# 6. Service 규칙

## 6.1 파일명

```text
[대구분][업무구분][세부업무구분][식별번호4자리]Service.java
```

예시:

```text
mkcoa8888Service.java
mkcoa5530Service.java
```

## 6.2 애노테이션

```java
@Slf4j
@Service
```

## 6.3 메서드

```java
public [서비스ID]DTOout [서비스ID]([서비스ID]DTOin input)
        throws Exception
```

예시:

```java
public mkcoa8888S0DTOout mkcoa8888S0(mkcoa8888S0DTOin input)
        throws Exception
```

---

# 7. DAO 규칙

## 7.1 파일명

식별번호 단위 DAO를 사용한다.

```text
[대구분][업무구분][세부업무구분][식별번호4자리]DAO.java
```

예시:

```text
mkcoa8888DAO.java
mkcoa5530DAO.java
```

**DAO 메서드명과 SQL ID는 반드시 동일하게 유지한다.**

## 7.2 메서드명

```text
[서비스ID]_[DML구분][순번]
[서비스ID]_[DML구분][순번]_count   (건수 조회 시)
```

예시:

```text
mkcoa8888S0_S0
mkcoa8888S0_S0_count
mkcoa8888D0_D0
mkcoa5530S0_S0
```

## 7.3 파라미터 / 반환형

```text
입력  : Map<String, Object>
출력  : List<Map<String, Object>> 또는 int
예외  : throws Exception
```

## 7.4 Mapper 애노테이션

```java
@RDWMapper
```

---

# 8. DTO 규칙

## 8.1 파일명

```text
[서비스ID]DTO[입출력구분].java
```

입출력 구분:

| 구분 | 의미    |
| ---- | ------- |
| in   | 입력    |
| out  | 출력    |
| io   | 입/출력 |

예시:

```text
mkcoa8888S0DTOin.java
mkcoa8888S0DTOout.java
mkcoa8888D0DTOin.java
mkcoa8888D0DTOout.java
mkcoa5530S0DTOin.java
mkcoa5530S0DTOout.java
```

## 8.2 기본 요건

- `com.ims.superspring.dto.DataObject` 상속
- Getter/Setter, clone, toString, getFieldPropertyMap 구현

---

# 9. Sub DTO 규칙

## 9.1 목적

- GRID 처리를 위한 하위 DTO 클래스
- 메인 DTO에서 참조하는 상세/서식 데이터

## 9.2 파일명

```text
[서비스ID]DTOSub[순번].java
```

예시:

```text
mkcoa8888S0DTOSub0.java
mkcoa5530S0DTOSub0.java
mkcoa9999S0DTOSub0.java
```

## 9.3 구성

| 세그먼트  | 항목                 | 예시          |
| --------- | -------------------- | ------------- |
| 서비스 ID | 전체 서비스 ID       | `mkcoa8888S0` |
| DTO       | 고정문자             | `DTO`         |
| Sub       | 고정문자             | `Sub`         |
| 순번      | `0~9`, 초과 시 `A~Z` | `0`           |
| 확장자    | `.java`              |               |

## 9.4 주요 생성 기준

- `DataObject` 상속
- GRID 컬럼 필드 포함
- Getter/Setter, clone, toString, getFieldPropertyMap 구현
- SubDTO 목록 필드명은 생성 규칙과 일치하도록 관리 (예: `mkcoa8888S0DTOSub0`)

---

# 10. resources / MyBatis Mapper 파일 규칙

## 10.1 위치

PDMK는 패키지 경로를 점(`.`)으로 연결한 리소스 디렉터리를 사용한다.

```text
src/main/resources/rdw.[대구분소문자].[업무구분소문자].[세부업무구분소문자]/
```

예시:

```text
src/main/resources/rdw.mk.co.a/
```

매퍼 스캔 패턴: `classpath*:rdw.*/*.xml`

## 10.2 SQL Mapper 파일명

```text
[대구분][업무구분][세부업무구분][식별번호4자리]-[DB구분].xml
```

DB 구분:

| 코드 | DB            |
| ---- | ------------- |
| ORA  | Oracle        |
| MYS  | MySQL         |
| MSS  | MS SQL Server |

예시:

```text
mkcoa8888-ORA.xml
mkcoa5530-ORA.xml
mkcoa9999-ORA.xml
```

## 10.3 Mapper namespace

```xml
<mapper namespace="nhnis.mk.co.a.dao.[식별번호단위]DAO">
```

예시:

```xml
<mapper namespace="nhnis.mk.co.a.dao.mkcoa8888DAO">
```

---

# 11. SQL ID 규칙

## 11.1 구조

```text
[서비스ID]_[DML구분][순번]
[서비스ID]_[DML구분][순번]_count
```

DML 구분:

| 코드 | 의미                     |
| ---- | ------------------------ |
| S    | 조회                     |
| C    | 등록                     |
| U    | 수정                     |
| D    | 삭제                     |
| A    | 혼합(Update & Insert 등) |

순번:

```text
0~9
초과 시 A~Z
```

예시:

```text
mkcoa8888S0_S0
mkcoa8888S0_S0_count
mkcoa8888D0_D0
mkcoa5530S0_S0
mkcoa0000C0_C0
mkcoa0000U0_U0
mkcoa0000A0_A0
mkcoa0000R0_S0
```

**DAO 메서드명과 SQL ID는 반드시 일치해야 한다.**

---

# 12. MyBatis parameterType / resultType

간단한 조회와 동적 처리 시:

```text
java.util.HashMap
```

주의:

- `parameterType`, `resultType`을 명시
- SQL ID 주석을 SQL 본문에 포함
- `#{...}` 파라미터 바인딩 사용

---

# 13. SQL 작성 표준

## 13.1 기본

- Oracle SQL 문법을 기준으로 작성
- FULL OUTER JOIN은 ANSI SQL 문법 사용
- 부정 논리 연산자는 `<>` 사용
- Static SQL 원칙, 필요 시 Dynamic SQL 사용

## 13.2 대소문자

- DB 키워드 및 Object: 대문자
- 변수 및 사용자 정의 부분: 소문자
- 테이블 Alias: `T1`, `T2`, `T3`
- 컬럼 Alias: 반드시 `AS` 사용

## 13.3 포맷팅

- 줄 바꾸기는 키워드 또는 컬럼 위치 기준
- 정렬 시 동일 레벨 키워드 맞춤
- 콤마 사용 규칙 일관성 유지
- 괄호 시작/끝 줄 정렬
- 단어 사이 및 콤마/괄호 뒤 공백
- 연산자 전후 한 칸 공백

## 13.4 주석

모든 SQL에 SQL ID 주석을 포함한다.

```sql
SELECT /*+ HINT */ /* SQL ID */
```

## 13.5 성능 권장

- 부정형 조건보다 긍정형 조건 사용
- OR 사용 제한, `UNION ALL`로 전개 고려
- 중복 제거가 불필요하면 `UNION`보다 `UNION ALL`
- `SUBSTR(COL,1,2) = 'AB'`보다 `COL LIKE 'AB%'`
- 연속 값은 `IN`보다 `BETWEEN` 검토
- `NOT` 사용 최소화
- 범위 조건은 `BETWEEN` 우선 검토

---

# 14. 변수 명명 규칙

- 첫 문자는 소문자
- 형식은 카멜케이스(camelCase)

예시:

```text
guid
serviceId
totalCount
pageNo
```

---

# 15. Java 주석 규칙

## 15.1 파일 주석

필수 항목:

```text
파일명
프로그램명
설명
작성자
작성일
수정일자 / 수정자 / 수정내용
```

## 15.2 클래스 주석

```java
/**
 * @Class : 프로그램 명
 * @author : ${name}
 * @date : ${date}
 * @description : 프로그램 설명
 */
```

또는 간략형:

```java
/**
 * 이미지로그 조회/삭제 Controller.
 *
 * @since YYYY.MM.DD
 */
```

## 15.3 메서드 주석

```java
/**
 * @description : 메서드 설명
 * @method : 메서드명
 * @author : ${name}
 * @param : 파라미터
 * @return : 리턴타입
 * @throws : exception 타입
 * @date : ${date}
 */
```

## 15.4 날짜

생성/수정 당일 날짜로 자동 설정:

```text
YYYY.MM.DD
```

---

# 16. 코드 생성 워크플로우

## 16.1 1단계: 정보 입력

요청 항목:

1. 대구분 코드
2. 업무구분 코드
3. 세부업무구분 코드
4. 기능 구분(S/C/U/D/A/R)
5. 화면번호 또는 일반번호

## 16.2 2단계: 서비스 ID 생성

예:

```text
대구분       : MK
업무구분     : CO
세부업무구분 : A
기능         : 조회(S)
기본번호     : 8888
순번         : 0

서비스 ID → mkcoa8888S0
```

## 16.3 3단계: 파일 생성

생성 대상:

```text
Controller.java
Service.java
DAO.java
DTOin.java
DTOout.java
DTOSub0.java   (목록/GRID 시)
MyBatis Mapper XML
```

## 16.4 4단계: 검증

- 패키지 경로 확인 (`nhnis.mk.co.a`)
- 클래스명 / 메서드명 규칙 확인
- `@PostMapping("/서비스ID")` 확인
- DAO 메서드명과 SQL ID 일치 확인
- Mapper namespace / `rdw.mk.co.a` 경로 확인

---

# 17. meta dto 파일 규칙

## 17.1 원칙

원본 PDMP 규칙상 `.dto` 파일은 Java DTO 클래스와 동일한 쌍으로 생성한다.  
**현재 PDMK 레포에서는 meta dto를 사용하지 않는다.** (필요 시 아래 규칙을 따른다.)

## 17.2 위치

```text
[project home]/meta/nhnis/
    [대구분소문자]/
    [업무구분소문자]/
    [세부업무구분소문자]/
    dto/
```

## 17.3 파일명

```text
[서비스ID]DTO[입출력구분].dto
```

예시:

```text
mkcoa8888S0DTOin.dto
mkcoa8888S0DTOout.dto
```

---

# 18. 핵심 체크리스트

| 점검항목   | 기준                                                               |
| ---------- | ------------------------------------------------------------------ |
| 서비스 ID  | mk + 업무 + 세부 + 식별번호 + 구분자 + 순번                        |
| Controller | 식별번호 기준, 서비스 구분자 미포함, 클래스 `@RequestMapping` 금지 |
| Service    | 식별번호 기준                                                      |
| DAO        | 식별번호 단위 `…DAO`, 메서드명과 SQL ID 일치                       |
| DTO        | 서비스 ID 전체 + `DTOin`/`DTOout`, `DataObject`                    |
| Sub DTO    | `DTOSub[순번]`                                                     |
| Mapper XML | `rdw.mk.co.a/[식별번호]-ORA.xml`                                   |
| SQL ID     | `[서비스ID]_[DML][순번]` (+ `_count`)                              |
| URL        | `POST /[서비스ID]`                                                 |
| Java 변수  | camelCase                                                          |
| DB Object  | 대문자                                                             |
| SQL Alias  | 테이블 T1/T2/T3, 컬럼 AS                                           |

---

## 참고

본 파일은 PDMP Naming Convention을 PDMK(`MK`/`mk`) 기준으로 재구성한 문서이다.  
REST 스타일(`/api/mk/co/a/{id}/list`, `DtoIn`/`Dao`)은 사용하지 않는다.
