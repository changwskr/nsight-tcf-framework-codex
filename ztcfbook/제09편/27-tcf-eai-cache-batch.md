# 제27장. tcf-eai · tcf-cache · tcf-batch

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 · 모듈별 레퍼런스 (Quick Start) |
| **장** | 제27장 |
| **파일** | `제09편/27-tcf-eai-cache-batch.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 27.1 tcf-eai — WAR 간 연동

| ❌ 금지 | ✅ 권장 |
| --- | --- |
| ic-service가 sv-service Java import | `TcfServiceClient.call(...)` HTTP |
| WAR 간 Bean 참조 | `POST /{code}/online` 표준 전문 |

### 의존성

```gradle
implementation project(':tcf-eai')
```

### 설정 (application.yml)

```yaml
nsight:
  integration:
    default-timeout-ms: 3000
    services:
      SV:
        base-url: http://127.0.0.1:8086
        context-path: /sv
        online-path: /online
      IC:
        base-url: http://127.0.0.1:8082
        context-path: /ic
        online-path: /online
```

Gateway 경유 시 `base-url`을 `http://127.0.0.1:8100`으로 변경 가능.

### 호출 코드

```java
@Service
@RequiredArgsConstructor
public class SvIntegrationDemoService {
    private final TcfServiceClient client;

    public Map<String, Object> callIcSample(Map<String, Object> body, TransactionContext ctx) {
        return client.callForBody("IC", "IC.Sample.inquiry", "IC-INQ-0001", body, ctx);
    }
}
```

- **Header 전파:** GUID, TraceId, user, channel — `HeaderPropagationHelper`
- **데모:** `SV.Integration.icSample` → `IC.Sample.inquiry`

---

## 27.2 tcf-cache — 공통 캐시

Spring Cache + **EhCache 3 (JCache)** 공통 환경.

### 의존성·설정

```gradle
implementation project(':tcf-cache')
```

```yaml
spring:
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml
nsight:
  tcf:
    cache:
      enabled: true
```

### 캐시 영역

| alias | 용도 | TTL |
| --- | --- | --- |
| `commonCode` | 공통코드 | 30분 |
| `serviceCatalog` | ServiceId Catalog | 60분 |
| `sessionRegion` | 세션/리전 | 10분 |

상수: `TcfCacheNames`

### 사용 예

```java
@Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
public List<Map<String, Object>> findByGroup(String codeGroup) { ... }

@CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, allEntries = true)
public void save(...) { ... }
```

OM Cache 관리 UI: `OM.Cache.*` serviceId — [제25장](./25-tcf-om-ui-uj.md).

---

## 27.3 tcf-batch — 배치·모니터링 수집

| 항목 | 값 |
| --- | --- |
| 포트 | 8098 |
| Context | `/batch` |

### 역할

- AP/DB/세션/배포 **모니터링 데이터 수집**
- Batch Job 실행 이력·Scheduler 연동
- OM 대시보드·헬스체크 데이터 공급

### Quick Start

```bash
gradle :tcf-batch:bootRun

curl http://localhost:8098/actuator/health
```

### 업무 Batch와의 관계

| 구분 | 모듈 |
| --- | --- |
| 플랫폼 수집·모니터링 | tcf-batch |
| 업무 Job (@Scheduled) | 각 `*-service` WAR |
| OM Batch 관리 | tcf-om `OmBatchHandler` |

업무 Scheduler 예: [제23장 §23.5](../제08편/23-목록-페이징-등록-변경.md)

---

## 27.4 연동 시나리오 요약

```text
[SV WAR] --tcf-eai--> [IC WAR]
[tcf-om] --tcf-cache--> 공통코드·Catalog
[tcf-batch] --수집--> [OM Dashboard]
[EB WAR] --HTTP--> [EP WAR]  (이벤트, tcf-eai 대체 가능)
```

---

## 장 요약

**tcf-eai**는 WAR 간 HTTP/JSON + serviceId 연동의 유일한 표준 경로입니다. **tcf-cache**는 OM 기준정보 캐시를 중앙화하고, **tcf-batch**는 운영 모니터링·배치 이력을 수집합니다. 세 모듈 모두 JAR(또는 tcf-batch WAR)로 의존성 추가 후 yml 설정만으로 Quick Start 가능합니다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제26장 tcf-gateway · tcf-jwt](./26-tcf-gateway-jwt.md) |
| → 다음 | [제28장 tcf-cicd · tcf-scripts](./28-tcf-cicd-scripts.md) |

---

## 출처 색인

| 절 | 출처 |
| --- | --- |
| 27.1 | [zguide/tcf-eai-개발가이드.md](../../zguide/tcf-eai-개발가이드.md), [docs/architecture/46-service-integration-contract.md](../../docs/architecture/46-service-integration-contract.md) |
| 27.2 | [zguide/tcf-cache-개발가이드.md](../../zguide/tcf-cache-개발가이드.md), [docs/architecture/12-cache.md](../../docs/architecture/12-cache.md) |
| 27.3 | [zguide/tcf-batch-개발가이드.md](../../zguide/tcf-batch-개발가이드.md), [zarchitecture/12-배치-모니터링-아키텍처.md](../../zarchitecture/12-배치-모니터링-아키텍처.md) |
