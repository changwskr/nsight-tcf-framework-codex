-- =============================================================================
-- pdmg_infra : 은행 정보계 차세대 서버군 조사 / Architecture Repository
-- Source doc : zdocs/은행_정보계_차세대_서버군_분류_및_조사기준.md
-- Target    : MySQL 8.0+
-- =============================================================================

CREATE DATABASE IF NOT EXISTS pdmg_infra
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE pdmg_infra;

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 0. 공통: 감사 컬럼 관례
--   created_at, created_by, updated_at, updated_by
-- -----------------------------------------------------------------------------

-- =============================================================================
-- 1. 코드 / 조사기준 마스터
-- =============================================================================

CREATE TABLE code_set (
  code_set_id     VARCHAR(40)  NOT NULL COMMENT '예: SERVICE_MODEL, DEPLOY_MODEL',
  name_ko         VARCHAR(100) NOT NULL,
  description     VARCHAR(500) NULL,
  PRIMARY KEY (code_set_id)
) COMMENT='분류 코드 집합';

CREATE TABLE code_value (
  code_set_id     VARCHAR(40)  NOT NULL,
  code_value      VARCHAR(40)  NOT NULL,
  name_ko         VARCHAR(100) NOT NULL,
  sort_order      INT          NOT NULL DEFAULT 0,
  active_yn       CHAR(1)      NOT NULL DEFAULT 'Y',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (code_set_id, code_value),
  CONSTRAINT fk_code_value_set FOREIGN KEY (code_set_id) REFERENCES code_set (code_set_id)
) COMMENT='분류 코드 값';

CREATE TABLE survey_template (
  template_id     VARCHAR(40)  NOT NULL COMMENT '예: TMPL_WAS, TMPL_RDBMS',
  name_ko         VARCHAR(100) NOT NULL,
  tech_role_cd    VARCHAR(40)  NULL COMMENT 'TECH_ROLE 코드',
  doc_section     VARCHAR(40)  NULL COMMENT '문서 절 번호 예: 6.3',
  description     VARCHAR(500) NULL,
  PRIMARY KEY (template_id)
) COMMENT='서버군 유형별 조사 템플릿';

CREATE TABLE survey_item (
  item_id         VARCHAR(40)  NOT NULL,
  template_id     VARCHAR(40)  NOT NULL,
  category_ko     VARCHAR(80)  NOT NULL COMMENT '식별/Compute/보안 등',
  item_name_ko    VARCHAR(120) NOT NULL,
  data_type       VARCHAR(20)  NOT NULL DEFAULT 'STRING' COMMENT 'STRING|NUMBER|BOOL|DATE|JSON',
  required_yn     CHAR(1)      NOT NULL DEFAULT 'N',
  sort_order      INT          NOT NULL DEFAULT 0,
  description     VARCHAR(500) NULL,
  PRIMARY KEY (item_id),
  CONSTRAINT fk_survey_item_tmpl FOREIGN KEY (template_id) REFERENCES survey_template (template_id)
) COMMENT='조사항목 정의(문서 §5~§14)';

CREATE TABLE checklist_item (
  checklist_id    VARCHAR(40)  NOT NULL,
  category_ko     VARCHAR(40)  NOT NULL COMMENT '서버/Middleware/Application/운영/전환',
  item_text_ko    VARCHAR(300) NOT NULL,
  sort_order      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (checklist_id)
) COMMENT='실사 체크리스트(§32)';

CREATE TABLE quality_gate_def (
  gate_id         VARCHAR(20)  NOT NULL COMMENT 'GATE1..GATE7',
  name_ko         VARCHAR(100) NOT NULL,
  description     VARCHAR(500) NULL,
  sort_order      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (gate_id)
) COMMENT='품질 Gate 정의(§30)';

-- =============================================================================
-- 2. 조직 / 담당
-- =============================================================================

CREATE TABLE org_unit (
  org_id          VARCHAR(40)  NOT NULL,
  org_name        VARCHAR(100) NOT NULL,
  org_type        VARCHAR(20)  NULL COMMENT 'OWNER|OPS|BOTH',
  parent_org_id   VARCHAR(40)  NULL,
  PRIMARY KEY (org_id),
  CONSTRAINT fk_org_parent FOREIGN KEY (parent_org_id) REFERENCES org_unit (org_id)
) COMMENT='소유/운영 부서';

CREATE TABLE person (
  person_id       VARCHAR(40)  NOT NULL,
  name_ko         VARCHAR(80)  NOT NULL,
  email           VARCHAR(120) NULL,
  phone           VARCHAR(40)  NULL,
  org_id          VARCHAR(40)  NULL,
  active_yn       CHAR(1)      NOT NULL DEFAULT 'Y',
  PRIMARY KEY (person_id),
  CONSTRAINT fk_person_org FOREIGN KEY (org_id) REFERENCES org_unit (org_id)
) COMMENT='담당자';

-- =============================================================================
-- 3. 업무 / Application
-- =============================================================================

CREATE TABLE biz_system (
  system_id       VARCHAR(40)  NOT NULL COMMENT '예: NSIGHT',
  system_name     VARCHAR(120) NOT NULL,
  biz_domain_cd   VARCHAR(40)  NULL COMMENT '업무영역 코드',
  description     VARCHAR(500) NULL,
  owner_org_id    VARCHAR(40)  NULL,
  ops_org_id      VARCHAR(40)  NULL,
  owner_person_id VARCHAR(40)  NULL,
  status_cd       VARCHAR(40)  NOT NULL DEFAULT 'CONFIRMED',
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (system_id),
  CONSTRAINT fk_biz_owner_org FOREIGN KEY (owner_org_id) REFERENCES org_unit (org_id),
  CONSTRAINT fk_biz_ops_org FOREIGN KEY (ops_org_id) REFERENCES org_unit (org_id),
  CONSTRAINT fk_biz_owner_person FOREIGN KEY (owner_person_id) REFERENCES person (person_id)
) COMMENT='업무 시스템';

CREATE TABLE application (
  app_id          VARCHAR(40)  NOT NULL,
  system_id       VARCHAR(40)  NOT NULL,
  app_name        VARCHAR(120) NOT NULL,
  app_type_cd     VARCHAR(40)  NULL COMMENT 'ONLINE|BATCH|API|PORTAL|...',
  deploy_unit     VARCHAR(40)  NULL COMMENT 'WAR|JAR|EAR|CONTAINER|...',
  context_path    VARCHAR(200) NULL,
  description     VARCHAR(500) NULL,
  status_cd       VARCHAR(40)  NOT NULL DEFAULT 'CONFIRMED',
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (app_id),
  CONSTRAINT fk_app_system FOREIGN KEY (system_id) REFERENCES biz_system (system_id)
) COMMENT='Application 배포 단위';

-- =============================================================================
-- 4. 서버군 (논리 산정 단위, §16)
-- =============================================================================

CREATE TABLE server_group (
  group_id            VARCHAR(40)  NOT NULL COMMENT '예: SG-WAS-APP-A',
  group_name          VARCHAR(120) NOT NULL,
  system_id           VARCHAR(40)  NULL,
  tech_role_cd        VARCHAR(40)  NOT NULL COMMENT 'TECH_ROLE: WAS, WEB, DB...',
  service_model_cd    VARCHAR(40)  NULL COMMENT 'IaaS|PaaS|SaaS|BareMetal 목표/현행',
  deploy_model_cd     VARCHAR(40)  NULL,
  runtime_base_cd     VARCHAR(40)  NULL COMMENT 'BareMetal|VM|Container|K8s|Serverless',
  env_cd              VARCHAR(40)  NOT NULL COMMENT 'PROD|STG|DEV|EDU',
  tier_cd             VARCHAR(40)  NULL COMMENT 'TIER0..TIER4',
  scale_mode_cd       VARCHAR(40)  NULL COMMENT 'SCALE_OUT|SCALE_UP|BOTH',
  node_active_cnt     INT          NULL,
  node_standby_cnt    INT          NULL,
  node_dr_cnt         INT          NULL,
  total_core          DECIMAL(10,2) NULL,
  total_memory_gb     DECIMAL(12,2) NULL,
  total_storage_gb    DECIMAL(14,2) NULL,
  peak_tps            DECIMAL(14,2) NULL,
  target_tps          DECIMAL(14,2) NULL,
  concurrent_users    INT          NULL,
  status_cd           VARCHAR(40)  NOT NULL DEFAULT 'DISCOVERED',
  survey_template_id  VARCHAR(40)  NULL,
  remark              VARCHAR(1000) NULL,
  created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (group_id),
  KEY ix_sg_system (system_id),
  KEY ix_sg_tech (tech_role_cd),
  KEY ix_sg_status (status_cd),
  CONSTRAINT fk_sg_system FOREIGN KEY (system_id) REFERENCES biz_system (system_id),
  CONSTRAINT fk_sg_template FOREIGN KEY (survey_template_id) REFERENCES survey_template (template_id)
) COMMENT='논리 서버군';

-- =============================================================================
-- 5. 서버 자산 (Bare Metal / VM / Container / Cloud / SaaS)
-- =============================================================================

CREATE TABLE server_asset (
  asset_id            VARCHAR(40)  NOT NULL COMMENT 'Server ID 예: INF-APP-001',
  asset_name          VARCHAR(120) NOT NULL,
  group_id            VARCHAR(40)  NULL,
  system_id           VARCHAR(40)  NULL,
  asset_kind_cd       VARCHAR(40)  NOT NULL COMMENT 'BARE_METAL|VM|CONTAINER|K8S_POD|CLOUD_SVC|SAAS',
  env_cd              VARCHAR(40)  NOT NULL,
  tech_role_cd        VARCHAR(40)  NULL,
  service_model_cd    VARCHAR(40)  NULL,
  deploy_model_cd     VARCHAR(40)  NULL,
  runtime_base_cd     VARCHAR(40)  NULL,
  tier_cd             VARCHAR(40)  NULL,
  -- 위치
  datacenter_cd       VARCHAR(40)  NULL COMMENT 'PRIMARY|DR|...',
  cloud_provider_cd   VARCHAR(40)  NULL,
  region_cd           VARCHAR(40)  NULL,
  az_cd               VARCHAR(40)  NULL,
  rack_id             VARCHAR(80)  NULL,
  host_cluster        VARCHAR(120) NULL,
  vm_id               VARCHAR(120) NULL,
  -- Compute
  cpu_socket          INT          NULL,
  physical_core       INT          NULL,
  vcpu                INT          NULL,
  memory_gb           DECIMAL(12,2) NULL,
  swap_gb             DECIMAL(12,2) NULL,
  -- Storage (요약)
  local_disk_gb       DECIMAL(14,2) NULL,
  san_gb              DECIMAL(14,2) NULL,
  nas_gb              DECIMAL(14,2) NULL,
  object_storage_gb   DECIMAL(14,2) NULL,
  -- OS
  os_product          VARCHAR(80)  NULL,
  os_version          VARCHAR(80)  NULL,
  os_kernel           VARCHAR(120) NULL,
  os_patch_level      VARCHAR(80)  NULL,
  os_eol_date         DATE         NULL,
  timezone            VARCHAR(40)  NULL,
  locale              VARCHAR(40)  NULL,
  -- Runtime 요약
  runtime_java        VARCHAR(40)  NULL,
  runtime_other       VARCHAR(120) NULL,
  -- 조직
  owner_org_id        VARCHAR(40)  NULL,
  ops_org_id          VARCHAR(40)  NULL,
  owner_person_id     VARCHAR(40)  NULL,
  -- 상태
  status_cd           VARCHAR(40)  NOT NULL DEFAULT 'DISCOVERED',
  survey_template_id  VARCHAR(40)  NULL,
  remark              VARCHAR(1000) NULL,
  created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (asset_id),
  KEY ix_asset_group (group_id),
  KEY ix_asset_system (system_id),
  KEY ix_asset_kind (asset_kind_cd),
  KEY ix_asset_status (status_cd),
  CONSTRAINT fk_asset_group FOREIGN KEY (group_id) REFERENCES server_group (group_id),
  CONSTRAINT fk_asset_system FOREIGN KEY (system_id) REFERENCES biz_system (system_id),
  CONSTRAINT fk_asset_owner_org FOREIGN KEY (owner_org_id) REFERENCES org_unit (org_id),
  CONSTRAINT fk_asset_ops_org FOREIGN KEY (ops_org_id) REFERENCES org_unit (org_id),
  CONSTRAINT fk_asset_owner_person FOREIGN KEY (owner_person_id) REFERENCES person (person_id),
  CONSTRAINT fk_asset_template FOREIGN KEY (survey_template_id) REFERENCES survey_template (template_id)
) COMMENT='서버/VM/Container 등 실행 단위';

CREATE TABLE asset_attr (
  asset_id        VARCHAR(40)  NOT NULL,
  item_id         VARCHAR(40)  NOT NULL,
  attr_value      TEXT         NULL,
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (asset_id, item_id),
  CONSTRAINT fk_attr_asset FOREIGN KEY (asset_id) REFERENCES server_asset (asset_id),
  CONSTRAINT fk_attr_item FOREIGN KEY (item_id) REFERENCES survey_item (item_id)
) COMMENT='유형별 확장 조사값(EAV)';

CREATE TABLE app_server_map (
  app_id          VARCHAR(40)  NOT NULL,
  asset_id        VARCHAR(40)  NOT NULL,
  role_cd         VARCHAR(40)  NULL COMMENT 'PRIMARY|SECONDARY|BATCH_NODE',
  PRIMARY KEY (app_id, asset_id),
  CONSTRAINT fk_asm_app FOREIGN KEY (app_id) REFERENCES application (app_id),
  CONSTRAINT fk_asm_asset FOREIGN KEY (asset_id) REFERENCES server_asset (asset_id)
) COMMENT='Application-Server 매핑';

CREATE TABLE app_group_map (
  app_id          VARCHAR(40)  NOT NULL,
  group_id        VARCHAR(40)  NOT NULL,
  PRIMARY KEY (app_id, group_id),
  CONSTRAINT fk_agm_app FOREIGN KEY (app_id) REFERENCES application (app_id),
  CONSTRAINT fk_agm_group FOREIGN KEY (group_id) REFERENCES server_group (group_id)
) COMMENT='Application-ServerGroup 매핑';

-- =============================================================================
-- 6. Middleware / DB
-- =============================================================================

CREATE TABLE middleware_install (
  mw_id           VARCHAR(40)  NOT NULL,
  asset_id        VARCHAR(40)  NOT NULL,
  mw_category_cd  VARCHAR(40)  NOT NULL COMMENT 'WEB|WAS|MQ|CACHE|SCHEDULER|ETL|APIGW|OTHER',
  product_name    VARCHAR(120) NOT NULL,
  product_version VARCHAR(80)  NULL,
  eol_date        DATE         NULL,
  cluster_yn      CHAR(1)      NOT NULL DEFAULT 'N',
  config_summary  VARCHAR(1000) NULL,
  license_id      VARCHAR(40)  NULL,
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (mw_id),
  KEY ix_mw_asset (asset_id),
  CONSTRAINT fk_mw_asset FOREIGN KEY (asset_id) REFERENCES server_asset (asset_id)
) COMMENT='미들웨어 설치 현황';

CREATE TABLE db_instance (
  db_id           VARCHAR(40)  NOT NULL,
  asset_id        VARCHAR(40)  NULL COMMENT '호스트 서버(공유 가능)',
  group_id        VARCHAR(40)  NULL,
  db_engine_cd    VARCHAR(40)  NOT NULL COMMENT 'ORACLE|MYSQL|POSTGRES|TIBERO|...',
  db_role_cd      VARCHAR(40)  NULL COMMENT 'OLTP|DW|ODS|STANDBY',
  product_version VARCHAR(80)  NULL,
  instance_name   VARCHAR(120) NULL,
  charset         VARCHAR(40)  NULL,
  ha_mode_cd      VARCHAR(40)  NULL COMMENT 'RAC|DATAGUARD|REPLICATION|NONE',
  size_gb         DECIMAL(14,2) NULL,
  connection_max  INT          NULL,
  eol_date        DATE         NULL,
  personal_info_yn CHAR(1)     NOT NULL DEFAULT 'N',
  credit_info_yn  CHAR(1)      NOT NULL DEFAULT 'N',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (db_id),
  CONSTRAINT fk_db_asset FOREIGN KEY (asset_id) REFERENCES server_asset (asset_id),
  CONSTRAINT fk_db_group FOREIGN KEY (group_id) REFERENCES server_group (group_id)
) COMMENT='DB 인스턴스';

CREATE TABLE app_db_map (
  app_id          VARCHAR(40)  NOT NULL,
  db_id           VARCHAR(40)  NOT NULL,
  access_mode_cd  VARCHAR(40)  NULL COMMENT 'R|W|RW',
  schema_name     VARCHAR(80)  NULL,
  PRIMARY KEY (app_id, db_id),
  CONSTRAINT fk_adm_app FOREIGN KEY (app_id) REFERENCES application (app_id),
  CONSTRAINT fk_adm_db FOREIGN KEY (db_id) REFERENCES db_instance (db_id)
) COMMENT='Application-DB 매핑';

-- =============================================================================
-- 7. 네트워크 / 의존관계 / Interface
-- =============================================================================

CREATE TABLE network_endpoint (
  endpoint_id     VARCHAR(40)  NOT NULL,
  asset_id        VARCHAR(40)  NOT NULL,
  endpoint_type_cd VARCHAR(40) NOT NULL COMMENT 'IP|VIP|DNS|LB',
  address         VARCHAR(200) NOT NULL,
  port_no         INT          NULL,
  protocol_cd     VARCHAR(20)  NULL COMMENT 'TCP|UDP|HTTP|HTTPS',
  network_zone_cd VARCHAR(40)  NULL COMMENT 'DMZ|WEB|APP|DB|MGMT|DR',
  vlan_id         VARCHAR(40)  NULL,
  subnet_cidr     VARCHAR(40)  NULL,
  primary_yn      CHAR(1)      NOT NULL DEFAULT 'N',
  PRIMARY KEY (endpoint_id),
  KEY ix_ep_asset (asset_id),
  KEY ix_ep_address (address),
  CONSTRAINT fk_ep_asset FOREIGN KEY (asset_id) REFERENCES server_asset (asset_id)
) COMMENT='네트워크 엔드포인트';

CREATE TABLE asset_relation (
  relation_id     VARCHAR(40)  NOT NULL,
  from_asset_id   VARCHAR(40)  NULL,
  to_asset_id     VARCHAR(40)  NULL,
  from_group_id   VARCHAR(40)  NULL,
  to_group_id     VARCHAR(40)  NULL,
  from_app_id     VARCHAR(40)  NULL,
  to_db_id        VARCHAR(40)  NULL,
  relation_type_cd VARCHAR(40) NOT NULL COMMENT 'CALLS|USES_DB|USES_CACHE|USES_MQ|FILE_XFER|REPLICATES',
  critical_yn     CHAR(1)      NOT NULL DEFAULT 'N',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (relation_id),
  KEY ix_rel_from_asset (from_asset_id),
  KEY ix_rel_to_asset (to_asset_id),
  CONSTRAINT fk_rel_from_asset FOREIGN KEY (from_asset_id) REFERENCES server_asset (asset_id),
  CONSTRAINT fk_rel_to_asset FOREIGN KEY (to_asset_id) REFERENCES server_asset (asset_id),
  CONSTRAINT fk_rel_from_group FOREIGN KEY (from_group_id) REFERENCES server_group (group_id),
  CONSTRAINT fk_rel_to_group FOREIGN KEY (to_group_id) REFERENCES server_group (group_id),
  CONSTRAINT fk_rel_from_app FOREIGN KEY (from_app_id) REFERENCES application (app_id),
  CONSTRAINT fk_rel_to_db FOREIGN KEY (to_db_id) REFERENCES db_instance (db_id)
) COMMENT='의존관계(§25)';

CREATE TABLE app_interface (
  interface_id    VARCHAR(40)  NOT NULL,
  from_app_id     VARCHAR(40)  NULL,
  to_app_id       VARCHAR(40)  NULL,
  to_external_name VARCHAR(200) NULL,
  protocol_cd     VARCHAR(40)  NULL COMMENT 'REST|SOAP|EAI|MCA|FILE|MQ',
  direction_cd    VARCHAR(20)  NULL COMMENT 'IN|OUT|BOTH',
  critical_yn     CHAR(1)      NOT NULL DEFAULT 'N',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (interface_id),
  CONSTRAINT fk_if_from_app FOREIGN KEY (from_app_id) REFERENCES application (app_id),
  CONSTRAINT fk_if_to_app FOREIGN KEY (to_app_id) REFERENCES application (app_id)
) COMMENT='업무 Interface';

-- =============================================================================
-- 8. 가용성 / 용량 / 보안 요약
-- =============================================================================

CREATE TABLE availability_profile (
  profile_id      VARCHAR(40)  NOT NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP|SYSTEM',
  target_id       VARCHAR(40)  NOT NULL,
  ops_hours_cd    VARCHAR(40)  NULL COMMENT '24X365|BUSINESS_HOURS|BATCH_WINDOW',
  ha_yn           CHAR(1)      NOT NULL DEFAULT 'N',
  ha_mode_cd      VARCHAR(40)  NULL,
  cluster_yn      CHAR(1)      NOT NULL DEFAULT 'N',
  dr_yn           CHAR(1)      NOT NULL DEFAULT 'N',
  dr_mode_cd      VARCHAR(40)  NULL,
  rto_minutes     INT          NULL,
  rpo_minutes     INT          NULL,
  backup_yn       CHAR(1)      NOT NULL DEFAULT 'N',
  backup_policy   VARCHAR(200) NULL,
  monitoring_yn   CHAR(1)      NOT NULL DEFAULT 'N',
  alert_yn        CHAR(1)      NOT NULL DEFAULT 'N',
  patch_cycle_cd  VARCHAR(40)  NULL,
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (profile_id),
  UNIQUE KEY uk_avail_target (target_type_cd, target_id)
) COMMENT='HA/DR/RTO/RPO(§29)';

CREATE TABLE capacity_snapshot (
  snapshot_id     VARCHAR(40)  NOT NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP',
  target_id       VARCHAR(40)  NOT NULL,
  captured_at     DATETIME(3)  NOT NULL,
  metric_scope_cd VARCHAR(40)  NOT NULL DEFAULT 'CURRENT' COMMENT 'CURRENT|PEAK|TARGET|N1|DR',
  cpu_avg_pct     DECIMAL(6,2) NULL,
  cpu_peak_pct    DECIMAL(6,2) NULL,
  mem_avg_pct     DECIMAL(6,2) NULL,
  mem_peak_pct    DECIMAL(6,2) NULL,
  storage_used_gb DECIMAL(14,2) NULL,
  storage_growth_pct DECIMAL(8,2) NULL,
  iops            DECIMAL(14,2) NULL,
  throughput_mbps DECIMAL(14,2) NULL,
  users_total     INT          NULL,
  users_concurrent INT         NULL,
  tps_avg         DECIMAL(14,2) NULL,
  tps_peak        DECIMAL(14,2) NULL,
  batch_volume    DECIMAL(18,2) NULL,
  resp_avg_ms     DECIMAL(12,2) NULL,
  resp_p95_ms     DECIMAL(12,2) NULL,
  db_conn_peak    INT          NULL,
  thread_peak     INT          NULL,
  session_peak    INT          NULL,
  growth_rate_pct DECIMAL(8,2) NULL,
  safety_factor   DECIMAL(6,2) NULL,
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (snapshot_id),
  KEY ix_cap_target (target_type_cd, target_id, captured_at)
) COMMENT='성능·용량 스냅샷(§27)';

CREATE TABLE security_profile (
  profile_id      VARCHAR(40)  NOT NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP|SYSTEM|DB',
  target_id       VARCHAR(40)  NOT NULL,
  security_grade_cd VARCHAR(40) NULL,
  personal_info_yn CHAR(1)     NOT NULL DEFAULT 'N',
  credit_info_yn  CHAR(1)      NOT NULL DEFAULT 'N',
  financial_txn_yn CHAR(1)     NOT NULL DEFAULT 'N',
  admin_info_yn   CHAR(1)      NOT NULL DEFAULT 'N',
  external_conn_yn CHAR(1)     NOT NULL DEFAULT 'N',
  internet_conn_yn CHAR(1)     NOT NULL DEFAULT 'N',
  encryption_yn   CHAR(1)      NOT NULL DEFAULT 'N',
  kms_hsm_yn      CHAR(1)      NOT NULL DEFAULT 'N',
  pam_yn          CHAR(1)      NOT NULL DEFAULT 'N',
  edr_yn          CHAR(1)      NOT NULL DEFAULT 'N',
  audit_log_yn    CHAR(1)      NOT NULL DEFAULT 'N',
  auth_method_cd  VARCHAR(40)  NULL,
  network_zone_cd VARCHAR(40)  NULL,
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (profile_id),
  UNIQUE KEY uk_sec_target (target_type_cd, target_id)
) COMMENT='보안·개인정보 분류(§28)';

-- =============================================================================
-- 9. 계약 / 비용 / 전환
-- =============================================================================

CREATE TABLE license_contract (
  license_id      VARCHAR(40)  NOT NULL,
  product_name    VARCHAR(120) NOT NULL,
  vendor_name     VARCHAR(120) NULL,
  license_model_cd VARCHAR(40) NULL COMMENT 'CORE|PROCESSOR|SUBSCRIPTION|NAMED_USER|...',
  qty             DECIMAL(12,2) NULL,
  annual_maint_amt DECIMAL(18,2) NULL,
  currency_cd     VARCHAR(10)  NULL DEFAULT 'KRW',
  contract_end_dt DATE         NULL,
  mobility_yn     CHAR(1)      NOT NULL DEFAULT 'N',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (license_id)
) COMMENT='라이선스·유지보수';

CREATE TABLE asset_license_map (
  asset_id        VARCHAR(40)  NOT NULL,
  license_id      VARCHAR(40)  NOT NULL,
  allocated_qty   DECIMAL(12,2) NULL,
  PRIMARY KEY (asset_id, license_id),
  CONSTRAINT fk_alm_asset FOREIGN KEY (asset_id) REFERENCES server_asset (asset_id),
  CONSTRAINT fk_alm_lic FOREIGN KEY (license_id) REFERENCES license_contract (license_id)
) COMMENT='자산-라이선스 할당';

CREATE TABLE cost_snapshot (
  cost_id         VARCHAR(40)  NOT NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP|SYSTEM|WAVE',
  target_id       VARCHAR(40)  NOT NULL,
  period_ym       CHAR(6)      NOT NULL COMMENT 'YYYYMM',
  cost_type_cd    VARCHAR(40)  NOT NULL COMMENT 'HW|CLOUD|MW|DB|OPS|OTHER',
  amount          DECIMAL(18,2) NOT NULL,
  currency_cd     VARCHAR(10)  NOT NULL DEFAULT 'KRW',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (cost_id),
  KEY ix_cost_target (target_type_cd, target_id, period_ym)
) COMMENT='비용 스냅샷(§06)';

CREATE TABLE migration_wave (
  wave_id         VARCHAR(40)  NOT NULL,
  wave_name       VARCHAR(120) NOT NULL,
  sequence_no     INT          NOT NULL,
  planned_start   DATE         NULL,
  planned_end     DATE         NULL,
  status_cd       VARCHAR(40)  NOT NULL DEFAULT 'PLANNED',
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (wave_id)
) COMMENT='전환 Wave';

CREATE TABLE migration_plan (
  plan_id         VARCHAR(40)  NOT NULL,
  wave_id         VARCHAR(40)  NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP|SYSTEM',
  target_id       VARCHAR(40)  NOT NULL,
  strategy_7r_cd  VARCHAR(40)  NOT NULL COMMENT 'REHOST|REPLATFORM|REFACTOR|REPURCHASE|RETAIN|RETIRE|RELOCATE',
  current_platform_cd VARCHAR(40) NULL,
  target_platform_cd  VARCHAR(40) NULL COMMENT 'IAAS|PAAS|SAAS|BARE_METAL|K8S',
  difficulty_cd   VARCHAR(20)  NULL COMMENT 'L|M|H',
  status_cd       VARCHAR(40)  NOT NULL DEFAULT 'TARGET_DEFINED',
  remark          VARCHAR(1000) NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (plan_id),
  KEY ix_mig_target (target_type_cd, target_id),
  CONSTRAINT fk_mig_wave FOREIGN KEY (wave_id) REFERENCES migration_wave (wave_id)
) COMMENT='전환 계획(7R)';

-- =============================================================================
-- 10. 품질 Gate / 체크리스트 결과
-- =============================================================================

CREATE TABLE gate_result (
  result_id       VARCHAR(40)  NOT NULL,
  gate_id         VARCHAR(20)  NOT NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP|SYSTEM|WAVE',
  target_id       VARCHAR(40)  NOT NULL,
  pass_yn         CHAR(1)      NOT NULL,
  checked_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  checked_by      VARCHAR(40)  NULL,
  evidence        VARCHAR(1000) NULL,
  PRIMARY KEY (result_id),
  KEY ix_gate_target (gate_id, target_type_cd, target_id),
  CONSTRAINT fk_gate_def FOREIGN KEY (gate_id) REFERENCES quality_gate_def (gate_id)
) COMMENT='품질 Gate 결과';

CREATE TABLE checklist_result (
  result_id       VARCHAR(40)  NOT NULL,
  checklist_id    VARCHAR(40)  NOT NULL,
  target_type_cd  VARCHAR(20)  NOT NULL COMMENT 'ASSET|GROUP',
  target_id       VARCHAR(40)  NOT NULL,
  checked_yn      CHAR(1)      NOT NULL DEFAULT 'N',
  checked_at      DATETIME(3)  NULL,
  checked_by      VARCHAR(40)  NULL,
  remark          VARCHAR(500) NULL,
  PRIMARY KEY (result_id),
  UNIQUE KEY uk_chk_target (checklist_id, target_type_cd, target_id),
  CONSTRAINT fk_chk_item FOREIGN KEY (checklist_id) REFERENCES checklist_item (checklist_id)
) COMMENT='실사 체크리스트 결과';

-- =============================================================================
-- 11. 제안서용 뷰 (§33 핵심 표 일부)
-- =============================================================================

CREATE OR REPLACE VIEW v_server_inventory AS
SELECT
  a.asset_id,
  a.asset_name,
  a.system_id,
  s.system_name,
  a.group_id,
  g.group_name,
  a.tech_role_cd,
  a.service_model_cd,
  a.deploy_model_cd,
  a.runtime_base_cd,
  a.env_cd,
  a.datacenter_cd,
  a.region_cd,
  a.az_cd,
  a.vcpu,
  a.memory_gb,
  a.local_disk_gb,
  a.os_product,
  a.os_version,
  a.runtime_java,
  a.status_cd,
  ep.address AS primary_ip
FROM server_asset a
LEFT JOIN biz_system s ON s.system_id = a.system_id
LEFT JOIN server_group g ON g.group_id = a.group_id
LEFT JOIN network_endpoint ep
  ON ep.asset_id = a.asset_id AND ep.endpoint_type_cd = 'IP' AND ep.primary_yn = 'Y';

CREATE OR REPLACE VIEW v_server_group_capacity AS
SELECT
  g.group_id,
  g.group_name,
  g.tech_role_cd,
  g.env_cd,
  g.node_active_cnt,
  g.node_standby_cnt,
  g.node_dr_cnt,
  g.total_core,
  g.total_memory_gb,
  g.total_storage_gb,
  g.peak_tps,
  g.target_tps,
  g.scale_mode_cd,
  g.status_cd,
  COUNT(a.asset_id) AS registered_asset_cnt
FROM server_group g
LEFT JOIN server_asset a ON a.group_id = g.group_id
GROUP BY
  g.group_id, g.group_name, g.tech_role_cd, g.env_cd,
  g.node_active_cnt, g.node_standby_cnt, g.node_dr_cnt,
  g.total_core, g.total_memory_gb, g.total_storage_gb,
  g.peak_tps, g.target_tps, g.scale_mode_cd, g.status_cd;

CREATE OR REPLACE VIEW v_migration_mapping AS
SELECT
  p.plan_id,
  p.wave_id,
  w.wave_name,
  p.target_type_cd,
  p.target_id,
  p.strategy_7r_cd,
  p.current_platform_cd,
  p.target_platform_cd,
  p.difficulty_cd,
  p.status_cd
FROM migration_plan p
LEFT JOIN migration_wave w ON w.wave_id = p.wave_id;
