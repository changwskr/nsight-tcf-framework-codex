# Mapping Seed Automation

소스 스캔 인벤토리로 `ontology/mappings` 초안을 생성한다.

## 동작

1. pdmg-service/ui 스캔 (`InventorySnapshot`)
2. Mapper XML에서 table, schema.sql에서 PK, DTO 존재 여부, sample-requests 보강
3. 항상 `ontology/mappings/_generated/{programId}.yml` 갱신
4. `ontology/mappings/{programId}.yml`
   - 기본: **없을 때만** 생성 (기존 시드 보존)
   - `overwrite=true`: 기존 파일도 덮어씀 (주의)

## 실행

```bat
scripts\import\seed-mappings.bat
scripts\import\seed-mappings.bat overwrite
```

또는:

```bat
gradlew.bat seedPdmg
gradlew.bat seedPdmg -Poverwrite=true
```

API:

```text
POST /api/ontology/seed/pdmg
POST /api/ontology/seed/pdmg?overwrite=true
```

## 산출물

| 경로 | 설명 |
|------|------|
| `ontology/mappings/_generated/*.yml` | 항상 갱신되는 초안 |
| `ontology/mappings/*.yml` | 정본(기본은 missing only) |
| `test-data/queries/last-seed-report.json` | created/skipped/overwritten |

## 권장 흐름

```text
seedPdmg → 사람이 title/exceptionCodes 보정 → validatePdmg → reload API
```

`_generated` 는 레지스트리 인덱싱에서 제외된다.
