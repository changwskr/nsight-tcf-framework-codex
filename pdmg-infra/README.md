# PDMG Infra (`pdmg-infra`)

인프라/운영 보조용 Spring Boot 애플리케이션입니다.  
Gradle·Boot 버전과 `pdmg-fw` 연결 방식은 [`pdmg-service`](../pdmg-service/README.md)를 따릅니다.

| 항목 | 값 |
|------|-----|
| Boot | 3.5.14 |
| Java | 21 |
| Gradle | 8.10.1 (wrapper) |
| Port | `8081` |
| Main | `nhnis.infra.PdmgInfraApplication` |
| FW | `../pdmg-fw` (`implementation project(':pdmg-fw')`) |
| UI | `src/main/resources/static/` (내장) |

## 내장 화면 (UI)

앱 기동 후 브라우저:

| URL | 화면 |
|-----|------|
| http://localhost:8081/index.html | 홈 / 메뉴 |
| http://localhost:8081/infra/dashboard/index.html | INF-010 통합 대시보드 (ifina0100S0) |
| http://localhost:8081/infra/risks/index.html | INF-020 리스크·Gate 워크리스트 (ifina0200S0) |
| http://localhost:8081/infra/survey-gaps/index.html | INF-930 조사 미완료 (ifina9300S0) |
| http://localhost:8081/infra/codes/index.html | INF-110 분류코드 (ifina1100) |
| http://localhost:8081/infra/surveys/index.html | INF-120 조사 템플릿 (ifina1200) |
| http://localhost:8081/infra/orgs/index.html | INF-150 조직·담당자 (ifina1500) |
| http://localhost:8081/infra/audit/index.html | INF-160 변경이력·증빙 실파일 업로드 (ifina1600S0/C0) |
| http://localhost:8081/infra/systems/index.html | INF-210 업무 시스템 CRUD (ifina2100) |
| http://localhost:8081/infra/apps/index.html | INF-220 Application CRUD (ifina2200) |
| http://localhost:8081/infra/app-maps/index.html | INF-230 App↔Server/Group/DB (ifina2300) |
| http://localhost:8081/infra/groups/index.html | INF-310 서버군 CRUD (ifina3110) |
| http://localhost:8081/infra/assets/index.html | INF-320 서버 자산 파일럿 (ifina1999) |
| http://localhost:8081/infra/server-assets/index.html | INF-320 정규 자산 (ifina3100) + 이관 |
| http://localhost:8081/infra/bulk/index.html | INF-340 일괄등록 (ifina3400V0/C0) |
| http://localhost:8081/infra/middleware/index.html | INF-410 Middleware (ifina4100) |
| http://localhost:8081/infra/db/index.html | INF-420 DB Instance (ifina4200) |
| http://localhost:8081/infra/eol/index.html | INF-430 EOL/EOS (ifina4300S0 · V_IF_EOL_RISK) |
| http://localhost:8081/infra/network/index.html | INF-510 Network Endpoint (ifina5100) |
| http://localhost:8081/infra/interfaces/index.html | INF-520 Application Interface (ifina5200) |
| http://localhost:8081/infra/deps/index.html | INF-530 의존맵 (ifina5300) |
| http://localhost:8081/infra/ha/index.html | INF-610 HA·DR (ifina6100) |
| http://localhost:8081/infra/capacity/index.html | INF-620 용량 Snapshot (ifina6200) |
| http://localhost:8081/infra/security/index.html | INF-630 보안 프로파일 (ifina6300 · V0) |
| http://localhost:8081/infra/capacity-compare/index.html | INF-640 용량 비교 (ifina6400S0) |
| http://localhost:8081/infra/migration/index.html | INF-810 7R 전환계획 (ifina8100) |
| http://localhost:8081/infra/waves/index.html | INF-820 Migration Wave (ifina8200) |
| http://localhost:8081/infra/asis-tobe/index.html | INF-830 AS-IS→TO-BE (ifina8300S0) |
| http://localhost:8081/infra/proposal/index.html | INF-940 제안서 현황표·CSV/XLSX/PDF·일괄 Export (ifina9400S0/E0) |
| http://localhost:8081/infra/licenses/index.html | INF-710 라이선스 (ifina7100) |
| http://localhost:8081/infra/license-alloc/index.html | INF-720 라이선스 할당 (ifina7200) |
| http://localhost:8081/infra/tco/index.html | INF-730 비용·TCO (ifina7300) |
| http://localhost:8081/infra/checklist/index.html | INF-910 Checklist (ifina9100) |
| http://localhost:8081/infra/checklist-master/index.html | INF-130 Checklist 마스터 (ifina1300) |
| http://localhost:8081/infra/gate-defs/index.html | INF-140 Gate 정의 (ifina1400) |
| http://localhost:8081/infra/gates/index.html | INF-920 Architecture Gate (ifina9200 · Evidence 파일 업로드) |
| http://localhost:8081/health | 헬스 |

정적 GET은 `pdmg-fw` `DefaultFilter` 패스스루입니다. 거래는 동일 오리진 `POST /{serviceId}` 입니다.

## RACI Soft/Hard (`infra.auth.raci`)

| 설정 | 기본 | 설명 |
|------|------|------|
| `infra.auth.raci.mode` | `soft` | `off` / `soft`(경고만) / `hard`(RSLT_CD=`0006`) |
| `infra.auth.raci.default-role` | `ARCH` | optr 미지정·미등록 시 폴백 |

역할은 `hdr.sys_comm.optr_eno` → `TB_IF_PERSON_PILOT.ROLE_CD` 조회, 없으면 정적 맵(`E0000001`=ARCH … `E0000007`=MW). UI 우하단 역할 선택기는 `localStorage`에 optr를 넣습니다.

**IdP 역할 동기화 (OPEN-02 MVP)** — `ifina1500E0`

| 설정 | 기본 | 설명 |
|------|------|------|
| `infra.auth.idp.enabled` | `true` | 동기화 API on/off |
| `infra.auth.idp.create-missing` | `true` | 미존재 person 자동 생성 |
| `infra.auth.idp.default-org-id` | `ORG-INFRA` | 신규 person 기본 조직 |
| `infra.auth.idp.role-map` | infra-ops→OPS … | IdP 그룹명 → RACI ROLE_CD |

샘플: `sample-requests/ifina1500-idp-sync.json`. HARD 모드에서는 Admin(`E0000005`)만 호출 가능(기준정보와 동일).

**HARD가 Service에서 `AuthGuard.denyIfHard`로 적용되는 쓰기 경로**

| 영역 | serviceId | 허용 역할 |
|------|-----------|-----------|
| Gate 판정 | `ifina9200U0` | ARCH / ADMIN / **SEC는 GATE5만** |
| 기준정보 | `ifina1100`/`1200`/`1300`/`1400`/`1500` C·U(·D) | **ADMIN만** (ARCH는 soft 경고 / hard 거절) |
| 보안 프로파일 | `ifina6300U0` | ARCH / ADMIN / SEC |
| 비용·라이선스·전환 | `ifina7100` C·U·D, `7200U`, `7300C`, `8100` C·U·D, `8200U` | ARCH / PMO / ADMIN |
| 인벤토리·기술영역 | `1999`/`2100`/`2200`/`2300`/`3100`/`3110`/`3400`/`4100`/`4200`/`5100`/`5200`/`5300`/`6100`/`6200`/`9100` C·U·D | PMO **제외**; **DBA**=`4200`+RDBMS `3100`; **MW**=`4100`+WAS/WEB `3100`; Checklist(`9100`) 양쪽 허용 |
| Lifecycle 점프 | `ifina3100U0` | 인접 전이 전원 허용, 점프는 ARCH/ADMIN (`RL-AU-002`), 역행 금지 (`RL-LF-002`) |

HARD 거절 시 `change_log`에 `TARGET_TYPE_CD=RACI`, `ACTION_CD=DENY`가 기록되고 `CHANGED_BY`는 `optr_eno`입니다.

설계 추적: 화면설계 §34.13~34.16, 서비스설계 §5.7.4·§5.9, OPEN-02/04/05.

### HARD 운영 체크리스트

1. 로컬 검증은 기본 `soft` 유지. 데모/인수만 `application.yml`에서 `infra.auth.raci.mode: hard`.
2. UI 우하단에서 역할(optr) 선택 후 쓰기 시도 — OPS는 Gate/마스터/비용 거절, PMO는 인벤토리 거절, DBA/MW는 타 영역 거절.
3. 샘플: `sample-requests/ifina9200-gate-ops-deny.json` (OPS → 0006), `ifina1999-create.json`의 `optr_eno`를 `E0000004`로 바꿔 PMO 인벤토리 거절 확인. DBA=`E0000006`, MW=`E0000007`.
4. Audit 화면(` /infra/audit/ `)에서 `RACI` / `DENY` 필터로 거절 이력 확인.
5. 운영 전환 전 Soft 모드로 역할 경고 로그(`BizPrePostAspect` RACI Soft)를 한동안 관찰.

## 파일럿 거래: `ifina1999` (서버 인벤토리 CRUD)

`pdmg-service`의 `mgcoa9000`과 동일한 서비스 사상입니다.

```text
DefaultFilter / ServicePreventionInterceptor (pdmg-fw)
  → OnlineTransactionController (TCF ON)
    → ifina1999Handler
      → ifina1999Facade (@Transactional rdwTransactionManager)
        → BizPrePostAspect → ifina1999Service → ifina1999DAO
```

| serviceId | 기능 |
|-----------|------|
| `ifina1999S0` | 목록 조회 (페이징) |
| `ifina1999C0` | 등록 |
| `ifina1999U0` | 수정 |
| `ifina1999D0` | 삭제 |

패키지: `nhnis.infra.in.a.*`  
테이블: `TB_IF_SERVER_PILOT`  
Mapper: `rdw.infra.in.a/ifina1999-ORA.xml`  
샘플: `src/main/resources/sample-requests/ifina1999-*.json`

```powershell
# 조회 예
curl -X POST http://localhost:8081/ifina1999S0 `
  -H "Content-Type: application/json" `
  --data-binary "@src/main/resources/sample-requests/ifina1999-list.json"
```

## 빌드 / 실행

```powershell
cd pdmg-infra
.\gradlew.bat test
.\gradlew.bat bootRun
```

또는 `RUN.bat`.

헬스: http://localhost:8081/health  
UI: http://localhost:8081/index.html
