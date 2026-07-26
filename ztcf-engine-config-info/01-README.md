# NSIGHT TCF — Apache + Tomcat DeltaManager 환경설정

> **읽는 순서:** `01` → `12` (본 파일부터)  
> **셋팅 수치 SoT:** [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md) → `04`~`10`  
> **정합:** [11-정합-점검.md](./11-정합-점검.md) · **논리도:** [02-ARCHITECTURE.md](./02-ARCHITECTURE.md) · **체크섬:** [12-CHECKSUMS.sha256](./12-CHECKSUMS.sha256)

## 1. 목적

`nh.marketing.com`에서 Apache Reverse Proxy가 ARC05 WAR context를 Tomcat Cluster로 라우팅하고, DeltaManager로 센터 내부 세션을 복제한다.

```text
Client → Apache (sticky) → Tomcat×3 → ic/pc/ms/sv/…/ui/jwt.war → DB
```

| 항목 | 기준 |
|------|------|
| WAR | core 12 + jwt (extended) |
| Context | `/ic` … `/ui` `/jwt` |
| JDK | **21** |
| 빌드 | Gradle `bootWar` |
| Sticky | `JSESSIONID` ↔ `jvmRoute` |
| `/` | → `/ui/` |

## 2. 문서 읽는 순서

| 순번 | 파일 | 내용 |
|-----:|------|------|
| 01 | [01-README.md](./01-README.md) | 개요·인덱스 (본 문서) |
| 02 | [02-ARCHITECTURE.md](./02-ARCHITECTURE.md) | 논리 구조·WAR 표 |
| 03 | [03-아키텍처-셋팅정보.md](./03-아키텍처-셋팅정보.md) | 셋팅 인덱스 |
| 04 | [04-inventory-셋팅정보.md](./04-inventory-셋팅정보.md) | 노드·WAR CSV |
| 05 | [05-L4-GSLB-셋팅정보.md](./05-L4-GSLB-셋팅정보.md) | VIP·Health·Sticky |
| 06 | [06-apache-셋팅정보.md](./06-apache-셋팅정보.md) | Proxy·SSL·MPM |
| 07 | [07-tomcat-셋팅정보.md](./07-tomcat-셋팅정보.md) | JVM·Connector·Cluster |
| 08 | [08-spring-boot-셋팅정보.md](./08-spring-boot-셋팅정보.md) | yml·Hikari·세션 |
| 09 | [09-deploy-셋팅정보.md](./09-deploy-셋팅정보.md) | 배포·검증 |
| 10 | [10-operations-셋팅정보.md](./10-operations-셋팅정보.md) | Timeout·보안·모니터 |
| 11 | [11-정합-점검.md](./11-정합-점검.md) | 프로젝트 정합 |
| 12 | [12-CHECKSUMS.sha256](./12-CHECKSUMS.sha256) | 파일 무결성 |

### 디렉터리 안내 (얇은 설명)

| 경로 | 안내 |
|------|------|
| [00-inventory/인벤토리-설정.md](./00-inventory/인벤토리-설정.md) | CSV 위치 |
| [01-apache/아파치-설정.md](./01-apache/아파치-설정.md) | conf 구조 |
| [02-tomcat/톰캣-설정.md](./02-tomcat/톰캣-설정.md) | Tomcat 파일 |
| [03-spring-boot/스프링부트-설정.md](./03-spring-boot/스프링부트-설정.md) | Boot 샘플 |
| [04-l4-gslb/L4-GSLB-설정.md](./04-l4-gslb/L4-GSLB-설정.md) | L4 안내 |
| [05-deploy/배포-설정.md](./05-deploy/배포-설정.md) | 스크립트 사용 |
| [06-operations/운영-설정.md](./06-operations/운영-설정.md) | 체크리스트 인덱스 |

## 3. 적용 순서

1. [04-inventory-셋팅정보.md](./04-inventory-셋팅정보.md) — IP·`jvmRoute`·WAR 확정  
2. [06-apache-셋팅정보.md](./06-apache-셋팅정보.md) — `BalancerMember` 반영  
3. [07-tomcat-셋팅정보.md](./07-tomcat-셋팅정보.md) — `setenv`·`server.xml`  
4. WAR에 `<distributable/>` ([08-spring-boot-셋팅정보.md](./08-spring-boot-셋팅정보.md))  
5. [09-deploy-셋팅정보.md](./09-deploy-셋팅정보.md) — 배포·`validate-endpoints.sh`  
6. [10-operations-셋팅정보.md](./10-operations-셋팅정보.md) — sticky·timeout 검증  

## 4. 운영 원칙 (요약)

- 전 노드 동일 WAR 세트  
- `jvmRoute` 유일 · Apache `route`와 일치  
- 세션 객체 `Serializable`, 대량 DTO 세션 금지  
- Timeout: DB < TX < Apache < Client < L4 Idle  
- 센터 간 세션 복제 없음 → 재로그인  
- Gateway 사용 시 Apache 직행과 경로 중복 금지  
