# 제25장. OM·UI·uj 모듈

| 항목 | 내용 |
| --- | --- |
| **편** | 제9편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제25장](../ztcfbook/제09편/25-tcf-om-ui-uj.md) |

---

## 그림으로 보기

```mermaid
flowchart LR
  OM[tcf-om :8097/om] --> UI[tcf-ui :8099 Relay]
  UI --> UJ[tcf-uj :8102 Gateway UI]
```

---

## 25.1 tcf-om — 운영 센터 WAR

| | |
| --- | --- |
| 포트 | **8097** |
| URL | `POST /om/online` |
| serviceId | `OM.*` |

Catalog·통제·사용자·거래로그 **원장**.  
15장 내용 + **모듈 관점** 정리.

```bash
gradle :tcf-om:bootRun
```

⚠ **om-service(레거시)와 8097 충돌** — 동시 기동 금지.

---

## 25.2 tcf-ui — 브라우저 테스트 UI

| | |
| --- | --- |
| 포트 | **8099** |
| 역할 | HTML + **Relay** (쿠키 전달) |

| 화면 | URL |
| --- | --- |
| SV 테스트 | http://localhost:8099/sv/index.html |
| OM Admin | http://localhost:8099/om/admin/login.html |

22장 curl 대신 **버튼**으로 JSON 보낼 때 사용.

---

## 25.3 tcf-uj — Gateway 경유 UI

| | |
| --- | --- |
| 포트 | **8102** |
| 경로 | 브라우저 → **Gateway 8100** → 업무 WAR |

**운영과 같은 길**로 테스트할 때 씁니다.  
Gateway + tcf-om **먼저** 띄워야 합니다.

---

## 25.4 ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| tcf-ui 없이 CORS만 해결 | Relay가 **표준** |
| uj만 켜고 Gateway 안 켬 | **8100 필수** |
| Catalog seed 안 넣고 운영 배포 | **data.sql** 확인 |

---

## 요약

- **tcf-om** = 운영 DB·Catalog
- **tcf-ui** = 로컬 직결 테스트
- **tcf-uj** = Gateway 동형 테스트

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [24장 플랫폼 JAR](./24-플랫폼-JAR-3종.md) |
| → 다음 | [26장 Gateway·JWT](./26-Gateway-JWT-모듈.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/제09편/25-tcf-om-ui-uj.md](../ztcfbook/제09편/25-tcf-om-ui-uj.md)
