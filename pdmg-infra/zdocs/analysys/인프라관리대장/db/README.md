# 은행 정보계 서버군 조사 — 데이터베이스 설계

원천 문서: [`../은행_정보계_차세대_서버군_분류_및_조사기준.md`](../은행_정보계_차세대_서버군_분류_및_조사기준.md)

목적: 일회성 Excel 인벤토리가 아니라 **CMDB / Architecture Repository** 형태로  
현행 조사 → 의존관계 → 용량 → 목표 아키텍처 → HA/DR → 비용 → 전환 Wave 를 관리한다.

대상 DBMS: **MySQL 8** (`pdmg_infra`)

---

## 1. 설계 원칙

| 원칙 | 내용 |
|---|---|
| 추적성 | 업무 → Application → Server Group → Server → Middleware → DB → Interface |
| 이중 단위 | **서버(Node)** 와 **서버군(Group)** 을 분리. 용량·HA·DR은 군 단위 산정 |
| 코드 표준화 | IaaS/PaaS/SaaS, Bare Metal/VM/Container, Tier, 7R, Lifecycle Status 는 코드 테이블 |
| 확장 속성 | 서버군 유형별 조사항목(§5~§12)은 `survey_item` + `asset_attr` 로 확장 |
| 상태관리 | DISCOVERED → … → RETIRED (§26) |
| 품질 Gate | Gate 1~7 (§30) 결과를 서버/군/Wave 단위로 기록 |

문서 §34 폴더 구조와 테이블 영역 매핑:

```text
01_현행인벤토리  → biz_system, application, server_group, server_asset,
                   middleware_install, db_instance, cloud_service
02_의존관계      → asset_relation, app_interface, network_endpoint
03_성능용량      → capacity_snapshot (서버/군)
04_목표아키텍처  → target_platform, migration_plan
05_HA_DR         → availability_profile
06_비용          → license_contract, cost_snapshot
07_전환          → migration_wave, migration_plan
```

---

## 2. 논리 ER (핵심)

```text
org_unit ──┐
person ────┤
           ▼
      biz_system ──< application >── server_group >── server_asset
           │              │                │              │
           │              │                │              ├─ middleware_install
           │              │                │              ├─ network_endpoint
           │              │                │              └─ asset_attr
           │              │                │
           │              │                ├─ capacity_snapshot
           │              │                └─ availability_profile
           │              │
           │              └─ app_interface ──> (external / other app / db)
           │
           └─ migration_wave ──< migration_plan >── server_group / server_asset

code_set / code_value          (분류·상태·7R 등 마스터)
survey_template / survey_item  (조사기준 문서의 항목 템플릿)
quality_gate / gate_result     (품질 Gate)
```

---

## 3. 테이블 목록 (요약)

### 3.1 코드·기준 (문서 자체를 데이터로 관리)

| 테이블 | 역할 |
|---|---|
| `code_set` / `code_value` | 서비스모델·배포모델·실행기반·기술서버군·Tier·환경·상태·7R |
| `survey_template` | 서버군 유형별 조사 템플릿 (Bare Metal, WAS, DB …) |
| `survey_item` | 템플릿 내 조사항목 (필수여부, 설명) |
| `checklist_item` | §32 실사 체크리스트 |
| `quality_gate_def` | §30 Gate 1~7 정의 |

### 3.2 조직·업무

| 테이블 | 역할 |
|---|---|
| `org_unit` | 소유/운영 부서 |
| `person` | 담당자 |
| `biz_system` | 업무 시스템 (NSIGHT 등) |
| `application` | 배포 단위 Application |

### 3.3 서버군·서버

| 테이블 | 역할 |
|---|---|
| `server_group` | 논리 서버군 (WEB, APP-A, DB …) — §16 산정 단위 |
| `server_asset` | Bare Metal / VM / Container / K8s Pod / SaaS 인스턴스 |
| `asset_attr` | 유형별 확장 속성 (EAV, survey_item 연결) |

### 3.4 미들웨어·DB·네트워크

| 테이블 | 역할 |
|---|---|
| `middleware_install` | Web/WAS/MQ/Cache/Scheduler 등 설치 제품 |
| `db_instance` | RDBMS/DW 인스턴스 |
| `network_endpoint` | IP, VIP, Port, Zone |
| `asset_relation` | 의존관계 (APP→DB, APP→Cache …) |
| `app_interface` | 대외/내부 Interface |

### 3.5 운영·전환·비용

| 테이블 | 역할 |
|---|---|
| `availability_profile` | HA/DR/RTO/RPO/Backup |
| `capacity_snapshot` | 시점별 CPU/Mem/TPS/Batch 실측·목표 |
| `license_contract` | 라이선스·유지보수 |
| `cost_snapshot` | H/W·Cloud·운영비 |
| `migration_wave` | 전환 Wave |
| `migration_plan` | 7R + 목표 플랫폼 매핑 |
| `gate_result` | 품질 Gate 통과 여부 |

---

## 4. 서버 vs 서버군

| 구분 | PK | 다루는 정보 |
|---|---|---|
| `server_asset` | `asset_id` | Spec, OS, IP, Host, 개별 사용률 |
| `server_group` | `group_id` | Node 수, Active/Standby/DR, 총 Core/Mem, Peak TPS, Scale 방식 |

문서 §16 예시는 `server_group` + 집계 뷰(`v_server_group_capacity`)로 표현한다.

---

## 5. Lifecycle (§26)

`server_asset.status_cd` / `server_group.status_cd` / `migration_plan.status_cd`:

| 코드 | 의미 |
|---|---|
| DISCOVERED | 발견·수집 |
| VALIDATING | 실사·검증 중 |
| CONFIRMED | 현행 확정 |
| TARGET_DEFINED | 목표 플랫폼 정의 |
| MIGRATION_PLANNED | Wave 편성 |
| MIGRATED | 전환 완료 |
| RETIRED | 폐기 |

---

## 6. 적용 방법

```powershell
# Docker MySQL (예: pdmg-mysql)
docker exec -i pdmg-mysql mysql -uroot -proot < pdmg-infra/zdocs/db/01_schema_mysql.sql
docker exec -i pdmg-mysql mysql -uroot -proot pdmg_infra < pdmg-infra/zdocs/db/02_seed_codes.sql
```

또는:

```sql
SOURCE /path/to/01_schema_mysql.sql;
SOURCE /path/to/02_seed_codes.sql;
```

---

## 7. 후속 (pdmg-infra 앱)

1. Spring Data / MyBatis 로 CRUD API  
2. 서버 관리대장 Excel Import (§15 컬럼 매핑)  
3. Gate 자동검증 Job (§30)  
4. 제안서용 10대 표 View (§33)
