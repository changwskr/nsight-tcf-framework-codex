# Phase 2 — Import / Validation

## 목적

pdmg-service / pdmg-ui / pdmg-fw 소스를 스캔해 인벤토리를 만들고, ontology shapes·mappings와 대조한다.

## 스크립트

```bat
scripts\import\scan-pdmg.bat
scripts\validation\validate-pdmg.bat
```

또는:

```bat
gradlew.bat bootRun --args="--nhnis.ontology.job=import"
gradlew.bat bootRun --args="--nhnis.ontology.job=validate"
```

## API (서버 기동 중)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/ontology/inventory/pdmg` | 스캔 결과(JSON) |
| POST | `/api/ontology/import/pdmg` | 스캔 + YAML 저장 |
| POST | `/api/ontology/validate/pdmg` | 스캔+검증 (FAIL 시 422) |

## 산출물

| 파일 | 내용 |
|------|------|
| `test-data/ontology/inventory-pdmg.yml` | 소스 인벤토리 |
| `test-data/queries/last-validation-report.json` | 검증 리포트 |

## 검증 코드 (요약)

| code | severity | 의미 |
|------|----------|------|
| SHAPE-SERVICEID | error | 서비스ID 정규식 위반 |
| PKG-SERVICEID-AXIS | error | 패키지축 ≠ 서비스ID 축 |
| SHAPE-MAPPER | error | Mapper 폴더 축 위반 |
| DEV-*-DRIFT | error | ontology 매핑 vs 소스 FQCN 불일치 |
| MAPPING-MISSING | warning | 소스 프로그램에 시드 매핑 없음 |
| UI-ROUTE-MISSING | warning | `/programId/index.html` 없음 |
| SOURCE-MISSING | warning | ontology만 있고 소스 없음 |

## 설정

`application.yml`:

```yaml
nhnis.ontology.scan:
  pdmg-service: ../pdmg-service
  pdmg-ui: ../pdmg-ui
  pdmg-fw: ../pdmg-fw
```
