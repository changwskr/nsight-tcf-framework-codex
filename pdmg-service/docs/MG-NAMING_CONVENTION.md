---
name: MG-NAMING_CONVENTION
description: 농협 상호금융 PDMG 프로젝트 소스 코드 명명 규칙. nhnis.mg 6계층 패키지, Controller/Service/DAO/DTO, MyBatis Mapper/SQL ID, 변수·주석 규칙을 정의한다.
---

# PDMG Naming Convention Guide

## 1. 목적

이 문서는 **현재 `pdmg-service` 소스**(`src/main/java/nhnis/mg`)를 기준으로 한 명명 규칙이다.  
새 프로그램은 임의 변형보다 본 규칙과 기준 샘플을 따른다.

기준 프로그램 샘플: **mgcoa8888**  
- 조회 `POST /mgcoa8888S0`  
- 삭제 `POST /mgcoa8888D0`  
- 동일 식별번호 통합 Controller: `mgcoa8888Controller`

공통 FW: [`pdmk-fw`](../../pdmk-fw/README.md) (legacy-web, `DefaultFilter`, `RequestBody` resolver)

호출 흐름:

```text
ServicePreventionInterceptor (pdmk-fw)
  → BizPrePostAspect (nhnis.mg.entry.aspect)
    → Controller (nhnis.mg.entry.controller)
      → Service (nhnis.mg.application.service)
        → DAO (nhnis.mg.persistence.dao)
          → Mapper XML (rdw.mg.co.a/*.xml)
```

---

## 2. 애플리케이션 분류 체계

서비스 ID·클래스명·Mapper **리소스 폴더**에 쓰인다.  
Java 패키지 경로에는 업무/세부업무 세그먼트를 넣지 않는다.

### 2.1 대구분

- 애플리케이션 그룹 코드: `MG`
- Boot / 루트 패키지: `nhnis.mg`
- 메인 클래스: `nhnis.mg.PdmgApplication`

### 2.2 업무구분

| 코드 | 업무구분 |
| ---- | -------- |
| CO | 공통 |
| IC | 통합고객 |
| MS | 미니 상품부 |
| SA | 사업관리 |

### 2.3 세부업무구분

| 코드 | 세부구분 |
| ---- | -------- |
| A | 상담 |
| B | 고객 |

현재 샘플은 모두 `CO` + `A` → 서비스 ID 접두 `mgcoa`.

---

## 3. 서비스 ID 규칙

### 3.1 기본 구조

```text
[대구분 2자][업무구분 2자][세부업무구분 1자][식별번호 4자][구분자 1자][순번 1자]
```

예시:

```text
mgcoa8888S0
```

### 3.2 구성요소

| 구분 | 길이 | 예시 | 설명 |
| ---- | ---: | ---- | ---- |
| 대구분 | 2 | mg | 애플리케이션 그룹 |
| 업무구분 | 2 | co | CO, IC, MS, SA |
| 세부업무구분 | 1 | a | A, B |
| 식별번호 | 4 | 8888 | 화면번호 또는 일반번호 |
| 구분자 | 1 | S/C/U/D/A/R | 조회/등록/수정/삭제/혼합/리포트 |
| 순번 | 1 | 0~9, A~Z | 동일 기능 내 순번 |

### 3.3 구분자

| 구분자 | 의미 |
| ------ | ---- |
| S | 조회 |
| C | 등록 |
| U | 수정 |
| D | 삭제 |
| A | 혼합 |
| R | 리포트 |

### 3.4 예시

```text
조회   : mgcoa8888S0
삭제   : mgcoa8888D0
등록   : mgcoa0000C0
수정   : mgcoa0000U0
혼합   : mgcoa0000A0
리포트 : mgcoa0000R0
```

현재 레포 샘플:

| 프로그램 | API | 설명 |
| -------- | --- | ---- |
| `mgcoa8888` | `POST /mgcoa8888S0`, `POST /mgcoa8888D0` | 이미지로그 조회/삭제 |
| `mgcoa5530` | `POST /mgcoa5530S0` | 마케팅희망고객 목록 |
| `mgcoa9999` | `POST /mgcoa9999S0` | 영업팁 실적 목록 |

---

## 4. Java 패키지 구조 (6계층)

루트는 `nhnis.mg` 이다. 그 아래를 **계층(layer)** 으로 나눈다.

```text
src/main/java/nhnis/mg
├── PdmgApplication.java
├── ServletInitializer.java
├── entry/
│   ├── aspect/              # BizPrePostAspect
│   └── controller/          # mgcoa*Controller
├── application/
│   ├── service/             # mgcoa*Service
│   └── dto/                 # mgcoa*DTOin/out/Sub(/MsgJson)
├── persistence/
│   └── dao/                 # mgcoa*DAO (@RDWMapper)
├── client/                  # 외부 연동 (현재 package-info만)
├── config/                  # Security, MyBatis, CORS, WebMvc …
└── support/                 # MappingUtil 등 유틸
```

| 계층 | 패키지 | 역할 | 현재 소스 예 |
| ---- | ------ | ---- | ------------ |
| entry | `nhnis.mg.entry.controller` | HTTP 진입 | `mgcoa8888Controller` |
| entry | `nhnis.mg.entry.aspect` | 진입 선/후처리 Aspect | `BizPrePostAspect` |
| application | `nhnis.mg.application.service` | 업무 절차 | `mgcoa8888Service` |
| application | `nhnis.mg.application.dto` | 입출력 DTO | `mgcoa8888S0DTOin` |
| persistence | `nhnis.mg.persistence.dao` | MyBatis DAO | `mgcoa8888DAO` |
| client | `nhnis.mg.client` | 외부 WAS/API 호출 | `package-info.java` |
| config | `nhnis.mg.config` | Spring 설정 | `RdwDataSourceConfig`, `SecurityConfig` |
| support | `nhnis.mg.support` | 유틸 | `MappingUtil` |

원칙:

- Java 패키지에 `co.a` 같은 업무 세그먼트를 **넣지 않는다**.
- 업무/세부업무는 **서비스 ID·클래스명·`rdw.mg.co.a` 리소스 경로**에 둔다.
- REST 스타일(`/api/.../list`, `DtoIn`/`Dao`)은 사용하지 않는다.
- TCF `Handler` / `Facade` / `MG.Xxx.yyy` serviceId 체계는 사용하지 않는다.

---

## 5. Controller (`entry.controller`)

### 5.1 파일명 / 패키지

```text
패키지 : nhnis.mg.entry.controller
파일명 : [대구분][업무구분][세부업무구분][식별번호4자리]Controller.java
```

예: `mgcoa8888Controller.java`

클래스명에는 구분자(S/C/U/D…)를 넣지 않는다.  
동일 식별번호의 여러 서비스는 **하나의 Controller**로 통합한다.

```text
mgcoa8888S0 + mgcoa8888D0  →  mgcoa8888Controller
```

### 5.2 애노테이션

```java
@Slf4j
@RestController
```

- 클래스 레벨 `@RequestMapping` **금지**
- 메서드: `@PostMapping("/서비스ID")`
- 입력: `nhnis.fw.commons.resolver.RequestBody`  
  (요청 JSON의 `dto` 노드 바인딩)

### 5.3 메서드 시그니처

```java
@PostMapping("/mgcoa8888S0")
public mgcoa8888S0DTOout mgcoa8888S0(
        @RequestBody mgcoa8888S0DTOin input
) throws Throwable
```

요청 Body 예: `{"hdr_nhnis":{...},"dto":{...}}`  
local 프로파일에서는 `{"dto":{...}}` 만으로도 가능하다.

---

## 6. Service (`application.service`)

### 6.1 파일명 / 패키지

```text
패키지 : nhnis.mg.application.service
파일명 : [대구분][업무구분][세부업무구분][식별번호4자리]Service.java
```

예: `mgcoa8888Service.java`

### 6.2 애노테이션 / 메서드

```java
@Service
public class mgcoa8888Service {
    public mgcoa8888S0DTOout mgcoa8888S0(mgcoa8888S0DTOin input) throws Exception { … }
    public mgcoa8888D0DTOout mgcoa8888D0(mgcoa8888D0DTOin input) throws Exception { … }
}
```

- 메서드명 = 서비스 ID
- DAO·DTO는 각각 `persistence.dao`, `application.dto` 를 import

---

## 7. DAO (`persistence.dao`)

### 7.1 파일명 / 패키지

```text
패키지 : nhnis.mg.persistence.dao
파일명 : [대구분][업무구분][세부업무구분][식별번호4자리]DAO.java
```

예: `mgcoa8888DAO.java`

### 7.2 규칙

- `@RDWMapper` (`nhnis.mg.config.RDWMapper`) 사용
- **DAO 메서드명 = MyBatis SQL ID** (반드시 동일)
- 입력: `Map<String, Object>`
- 출력: `List<Map<String, Object>>` 또는 `int`
- `@MapperScan(basePackages = "nhnis.mg.persistence.dao")`

### 7.3 메서드명 / SQL ID

```text
[서비스ID]_[DML구분][순번]
[서비스ID]_[DML구분][순번]_count
```

예 (`mgcoa8888DAO`):

```text
mgcoa8888S0_S0
mgcoa8888S0_S0_count
mgcoa8888D0_D0
```

DML 구분: `S` 조회 / `C` 등록 / `U` 수정 / `D` 삭제 / `A` 혼합  
순번: `0~9`, 초과 시 `A~Z`

---

## 8. DTO (`application.dto`)

### 8.1 파일명

```text
[서비스ID]DTOin.java
[서비스ID]DTOout.java
[서비스ID]DTOSub[순번].java
[서비스ID]DTO*MsgJson.java   (필요 시)
```

패키지: `nhnis.mg.application.dto`

예:

```text
mgcoa8888S0DTOin.java
mgcoa8888S0DTOout.java
mgcoa8888S0DTOSub0.java
mgcoa8888D0DTOin.java
mgcoa8888D0DTOout.java
```

### 8.2 기본 요건

- `com.ims.superspring.dto.DataObject` 상속
- Getter/Setter, `clone`, `toString`, `getFieldPropertyMap` 구현
- 필드명: camelCase (`guid`, `pageNo`, `totalCount` …)

### 8.3 Sub DTO

GRID/목록 행용. 메인 out DTO가 Sub 목록을 보유한다.

| 세그먼트 | 예시 |
| -------- | ---- |
| 서비스 ID | `mgcoa8888S0` |
| 고정 | `DTOSub` |
| 순번 | `0` |

예: `mgcoa8888S0DTOSub0`, `mgcoa5530S0DTOSub0`, `mgcoa9999S0DTOSub0`

---

## 9. entry.aspect / config / support / client

### 9.1 entry.aspect (`nhnis.mg.entry.aspect`)

| 클래스 | 역할 |
| ------ | ---- |
| `BizPrePostAspect` | Controller 선/후처리 로그. pointcut: `nhnis.mg.entry.controller..*` |

진입 계층에 두어 Controller와 동일 `entry` 경계에서 선/후처리를 수행한다.

### 9.2 config (`nhnis.mg.config`)

| 클래스 | 역할 |
| ------ | ---- |
| `RdwDataSourceConfig` | RDW DataSource, MyBatis, `@MapperScan` |
| `RDWMapper` | DAO용 Mapper 애노테이션 |
| `MybatisLogInterceptor` | SQL 로깅 |
| `SecurityConfig` | 무상태 보안 |
| `WebMvcConfig` / `CorsProperties` | MVC·CORS |

### 9.3 support (`nhnis.mg.support`)

| 클래스 | 역할 |
| ------ | ---- |
| `MappingUtil` | Map ↔ 객체 매핑 유틸 |

### 9.4 client (`nhnis.mg.client`)

외부 시스템 호출용. 현재 샘플 연동 없음 → `package-info.java`만 존재.  
연동 추가 시 이 패키지에 클라이언트를 둔다.

---

## 10. MyBatis Mapper 리소스

### 10.1 위치

리소스 폴더는 **서비스 ID의 대구분·업무·세부업무**를 점(`.`)으로 연결한다.  
Java DAO 패키지와 물리 폴더 경로는 **일치하지 않아도 된다**.

```text
src/main/resources/rdw.[대구분].[업무구분].[세부업무구분]/
```

현재 샘플:

```text
src/main/resources/rdw.mg.co.a/
  mgcoa8888-ORA.xml
  mgcoa5530-ORA.xml
  mgcoa9999-ORA.xml
```

스캔 패턴: `classpath*:rdw.*/*.xml`

### 10.2 파일명

```text
[대구분][업무구분][세부업무구분][식별번호4자리]-[DB구분].xml
```

| 코드 | DB |
| ---- | -- |
| ORA | Oracle (로컬 H2 Oracle 모드 포함) |
| MYS | MySQL |
| MSS | MS SQL Server |

### 10.3 namespace

Java DAO FQCN과 일치:

```xml
<mapper namespace="nhnis.mg.persistence.dao.mgcoa8888DAO">
```

### 10.4 parameterType / resultType

조회·동적 처리 시 `java.util.HashMap` 사용.  
SQL 본문에 SQL ID 주석 포함 (`/* mgcoa8888S0_S0 */`).

---

## 11. SQL 작성 표준

- Oracle SQL 문법 기준 (로컬은 H2 `MODE=Oracle`)
- DB 키워드·Object: 대문자 / 바인딩 변수: 소문자 camelCase
- 테이블 Alias: `T1`, `T2`, `T3` / 컬럼 Alias: `AS` 사용
- Static SQL 원칙, 필요 시 Dynamic SQL
- 부정 비교: `<>`
- 성능: 긍정 조건 우선, `OR` 제한, `UNION ALL` 검토, `LIKE 'AB%'` 등

---

## 12. 변수·주석

### 12.1 Java 변수

- camelCase, 의미 있는 이름 (`guid`, `serviceId`, `totalCount`, `pageNo`)

### 12.2 주석

클래스:

```java
/**
 * 이미지로그 조회/삭제 Controller.
 *
 * @since YYYY.MM.DD
 */
```

날짜 형식: `YYYY.MM.DD`

---

## 13. 코드 생성 워크플로우

1. 대구분 / 업무구분 / 세부업무구분 / 기능(S·C·U·D·A·R) / 식별번호 결정  
2. 서비스 ID 생성 → 예: `mgcoa8888S0`  
3. 파일 생성:

```text
entry/controller/mgcoa8888Controller.java
application/service/mgcoa8888Service.java
application/dto/mgcoa8888S0DTOin.java
application/dto/mgcoa8888S0DTOout.java
application/dto/mgcoa8888S0DTOSub0.java   (목록 시)
persistence/dao/mgcoa8888DAO.java
resources/rdw.mg.co.a/mgcoa8888-ORA.xml
```

4. 검증:

- 계층 패키지 (`entry` / `application` / `persistence` …)
- `@PostMapping("/서비스ID")`
- DAO 메서드명 = SQL ID
- Mapper `namespace` = `nhnis.mg.persistence.dao.*DAO`
- 리소스 경로 `rdw.mg.co.a/` (업무 세그먼트 기준)

---

## 14. 핵심 체크리스트

| 점검항목 | 기준 |
| -------- | ---- |
| 패키지 | `nhnis.mg` + 6계층 |
| 서비스 ID | `mg` + 업무 + 세부 + 식별번호 + 구분자 + 순번 |
| Controller | `entry.controller`, 식별번호 단위, 클래스 `@RequestMapping` 금지 |
| Service | `application.service`, 메서드명 = 서비스 ID |
| DTO | `application.dto`, `DataObject`, `DTOin`/`DTOout`/`DTOSub` |
| DAO | `persistence.dao`, `@RDWMapper`, 메서드명 = SQL ID |
| Mapper XML | `rdw.mg.co.a/[식별]-ORA.xml`, namespace = persistence DAO |
| URL | `POST /[서비스ID]` |
| Aspect | `entry.aspect.BizPrePostAspect` → `entry.controller` |
| client | 외부 연동 시에만 추가 |
| REST/TCF Handler | 사용하지 않음 |

---

## 참고

- 본 문서는 `src/main/java/nhnis/mg` 실제 구조에 맞춰 작성한다.
- 공통 FW 클래스명 `PdmkTxLog`, 모듈명 `pdmk-fw` 는 FW 공유 식별자로 유지한다.
- meta `.dto` 파일은 현재 사용하지 않는다.
