# 개념 사전 v0.1 (PDMG 파일럿)

근거: 분류표, MG-NAMING_CONVENTION, BigPicture Tx 처리.

| ID | 개념 | 설명 | 축 |
|----|------|------|----|
| Module | 모듈 | `pdmg-fw` / `pdmg-service` / `pdmg-ui` / `tcf-ontology-service` | 아키 |
| MajorGroup | 대그룹 | 분류표 대그룹 코드. PDMG=`MG` | 업무 |
| BusinessCode | 업무코드 | CO/IC/PC/…/MM | 업무 |
| FunctionCode | 세부기능 | 업무별 A~D | 업무 |
| ProgramId | 프로그램 | `mgcoa9001` (대그룹+업무+기능+식별4) | 개발 |
| ServiceId | 서비스 ID | `mgcoa9001S0` (+구분자+순번) | 개발 |
| ComponentType | 컴포넌트 타입 | Handler/Facade/Controller/Service/DAO/DTO/Mapper | 개발 |
| JavaClass | Java 클래스 | FQCN | 개발 |
| MapperXml | MyBatis XML | `rdw.mg.co.a/mgcoa9001-ORA.xml` | 데이터 |
| SqlId | SQL ID | `mgcoa9001S0_S0` | 데이터 |
| Table | DB 테이블 | `TB_MG_TX_CONTROL` | 데이터 |
| UiRoute | UI 경로 | `/mgcoa9001/index.html` | 운영/채널 |
| SampleRequest | 샘플 전문 | `sample-requests/mgcoa9001-*.json` | 운영 |
| ConfigKey | 설정 키 | `nhnis.fw.tcf.enabled` 등 | 운영 |
| TxBoundary | TX 경계 | RequestThread(밖) / Worker TransactionTemplate(안) | 아키 |
| RuntimeStep | 런타임 단계 | Filter→…→Commit→후처리 | 아키 |
| ExceptionCode | 오류 코드 | `MP0401` 등 | 운영 |
| Shape | 형태 제약 | 서비스ID 정규식, 패키지 축 | 규칙 |
| Rule | 설계 규칙 | Handler→DAO 금지 등 | 규칙 |
| Mapping | 연결 | ServiceId↔Class↔Table↔UI | 매핑 |

## 관계(엣지) v0.1

| predicate | from → to |
|-----------|-----------|
| belongsTo | ServiceId → ProgramId / BusinessCode |
| implementedBy | ServiceId → JavaClass |
| mappedBy | ServiceId → SqlId / MapperXml |
| persistsTo | ServiceId → Table |
| exposedAt | ProgramId → UiRoute |
| configuredBy | RuntimeStep → ConfigKey |
| runsInside | ComponentType → TxBoundary |
| constrainedBy | ServiceId → Shape |
| governedBy | ComponentType → Rule |
