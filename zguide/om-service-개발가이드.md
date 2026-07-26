# om-service 개발자 가이드 (레거시)

> ⚠ **신규 개발·배포는 [tcf-om](./tcf-om-개발가이드.md)을 사용하세요.**

---

## 1. 이 모듈의 위치

| 항목 | om-service (레거시) | tcf-om (권장) |
|------|---------------------|---------------|
| Gradle/CI | ❌ 파이프라인 미포함 | ✅ `buildBusinessWars` |
| Handler | `OmSampleHandler`만 | 24 Handler, 80+ serviceId |
| 패키지 | flat (`handler/`, `service/`) | 6계층 |
| OM Admin·UD | ❌ | ✅ tcf-ui/tcf-uj 연동 |
| 포트 | 8097 (충돌) | 8097 |

---

## 2. 왜 남아 있는가

TCF 마이그레이션 이전 구조 참고용. **WAR 빌드·ztomcat 배포는 tcf-om만** 대상입니다.

---

## 3. 실행 (호환)

```bash
# om-service/scripts/run-local.bat → 실제로 tcf-om 기동
gradle :tcf-om:bootRun
tcf-scripts/run-local.bat tcf-om
```

**om-service와 tcf-om 동시 기동 금지** (포트 8097 충돌).

---

## 4. 마이그레이션 안내

1. [tcf-om-개발가이드.md](./tcf-om-개발가이드.md) 읽기  
2. Handler를 6계층 `entry/handler` + `serviceIds()` 패턴으로 이전  
3. Catalog·거래통제는 `tcf-om/data.sql` 반영  
4. OM Admin은 tcf-ui `/om/admin/*` 사용  

---

## 5. 참고

| | |
|---|---|
| [om-service/README.md](../om-service/README.md) | 레거시 설명 |
| [tcf-om-개발가이드.md](./tcf-om-개발가이드.md) | **본체** |
| [zman/12-OM운영관리.md](../zman/12-OM운영관리.md) | OM 설계 |
