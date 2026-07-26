# NSIGHT TCF — 프로젝트별 개발자 가이드 (zguide)

업무 개발자·플랫폼 개발자가 **모듈별로 바로 시작**할 수 있도록 정리한 가이드 모음입니다.

> 설계·아키텍처 상세: [`docs/`](../zdocs-1/) · [`zdoc/`](../zdocs-2/) · [`zman/`](../zman/)
> 모듈 요약: 각 프로젝트 `README.md`

---

## 읽는 순서 (처음 오신 분)

1. [tcf-core-개발가이드.md](./tcf-core-개발가이드.md) — TCF 처리 흐름 이해
2. 담당 **업무 WAR** 가이드 (예: sv-service)
3. 연동 필요 시 [tcf-eai-개발가이드.md](./tcf-eai-개발가이드.md)
4. 로컬 테스트: [tcf-ui-개발가이드.md](./tcf-ui-개발가이드.md) 또는 [tcf-scripts-개발가이드.md](./tcf-scripts-개발가이드.md)

---

## 업무 서비스 (9 WAR)

| 업무 | 가이드 | 포트 | Context |
|------|--------|------|---------|
| IC | [ic-service-개발가이드.md](./ic-service-개발가이드.md) | 8082 | /ic |
| PC | [pc-service-개발가이드.md](./pc-service-개발가이드.md) | 8083 | /pc |
| MS | [ms-service-개발가이드.md](./ms-service-개발가이드.md) | 8085 | /ms |
| SV | [sv-service-개발가이드.md](./sv-service-개발가이드.md) | 8086 | /sv |
| PD | [pd-service-개발가이드.md](./pd-service-개발가이드.md) | 8087 | /pd |
| EB | [eb-service-개발가이드.md](./eb-service-개발가이드.md) | 8089 | /eb |
| EP | [ep-service-개발가이드.md](./ep-service-개발가이드.md) | 8090 | /ep |
| SS | [ss-service-개발가이드.md](./ss-service-개발가이드.md) | 8093 | /ss |
| MG | [mg-service-개발가이드.md](./mg-service-개발가이드.md) | 8096 | /mg |

## 운영·플랫폼

| 모듈 | 가이드 | 포트 | 비고 |
|------|--------|------|------|
| tcf-om | [tcf-om-개발가이드.md](./tcf-om-개발가이드.md) | 8097 | **OM 본체** (권장) |
| om-service | [om-service-개발가이드.md](./om-service-개발가이드.md) | 8097 | 레거시 참고 |
| tcf-batch | [tcf-batch-개발가이드.md](./tcf-batch-개발가이드.md) | 8098 | |
| tcf-ui | [tcf-ui-개발가이드.md](./tcf-ui-개발가이드.md) | 8099 | 거래·OM UI |
| tcf-gateway | [tcf-gateway-개발가이드.md](./tcf-gateway-개발가이드.md) | 8100 | |
| tcf-uj | [tcf-uj-개발가이드.md](./tcf-uj-개발가이드.md) | 8102 | Gateway 경유 UI |
| tcf-jwt | [tcf-jwt-개발가이드.md](./tcf-jwt-개발가이드.md) | 8110 | |

## 공통 라이브러리·도구

| 모듈 | 가이드 | 유형 |
|------|--------|------|
| tcf-core | [tcf-core-개발가이드.md](./tcf-core-개발가이드.md) | JAR (TCF 엔진) |
| tcf-cache | [tcf-cache-개발가이드.md](./tcf-cache-개발가이드.md) | JAR |
| tcf-eai | [tcf-eai-개발가이드.md](./tcf-eai-개발가이드.md) | JAR (서비스 연동) |
| tcf-cicd | [tcf-cicd-개발가이드.md](./tcf-cicd-개발가이드.md) | 설정·배포 |
| tcf-scripts | [tcf-scripts-개발가이드.md](./tcf-scripts-개발가이드.md) | 로컬 스크립트 |

---

## 공통 개발 규칙 (한 페이지 요약)

```
POST /{업무코드}/online
  → TCF.process() [tcf-core + tcf-web]
     → STF (Header·세션·거래통제·Timeout·로그)
     → TransactionDispatcher (header.serviceId)
     → {Domain}Handler  ← 여기서 개발 시작
     → Facade → Service → Rule → DAO/Mapper
     → ETF (StandardResponse)
```

- **Controller 만들지 않음** — `/online`은 tcf-web 공통
- **Handler = 도메인(application Service)당 1개**, `serviceIds()` + `switch`
- **WAR 간 호출** = tcf-eai (Java 직접 참조 금지)
- **serviceId 등록** = tcf-om Catalog + 거래통제

자세히: [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md) · [docs/TCF_FRAMEWORK_GUIDE.md](../zdocs-1/TCF_FRAMEWORK_GUIDE.md)

---

## 로컬 포트 맵

```
8082 ic  8083 pc  8085 ms  8086 sv  8087 pd
8089 eb  8090 ep  8093 ss  8096 mg
8097 tcf-om  8098 batch  8099 ui  8100 gw  8102 uj  8110 jwt
8080 ztomcat (통합)
```
