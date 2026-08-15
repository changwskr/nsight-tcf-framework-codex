-- =============================================================================
-- Seed: 분류 코드 / Gate / 체크리스트 / 조사 템플릿 샘플
-- =============================================================================
USE pdmg_infra;
SET NAMES utf8mb4;

-- Code sets
INSERT INTO code_set (code_set_id, name_ko, description) VALUES
('SERVICE_MODEL', '서비스 제공모델', 'IaaS/PaaS/SaaS/BareMetal'),
('DEPLOY_MODEL', '배포모델', 'On-Prem/Private/Public/Hybrid/Multi'),
('RUNTIME_BASE', '실행 기반', 'Bare Metal/VM/Container/K8s/Serverless'),
('TECH_ROLE', '기술 서버군', '문서 §4·§6~§12'),
('TIER', '운영 중요도', 'Tier 0~4'),
('ENV', '환경', 'PROD/STG/DEV/EDU'),
('ASSET_KIND', '자산 종류', '실행 단위 유형'),
('LIFECYCLE', '생명주기 상태', '문서 §26'),
('STRATEGY_7R', '전환 7R', '문서 §14 전환'),
('NETWORK_ZONE', '보안 Zone', '문서 §28'),
('SCALE_MODE', '확장 방식', 'Scale-Up/Out'),
('MW_CATEGORY', '미들웨어 분류', NULL),
('RELATION_TYPE', '의존관계 유형', '문서 §25');

INSERT INTO code_value (code_set_id, code_value, name_ko, sort_order) VALUES
('SERVICE_MODEL','IAAS','IaaS',10),
('SERVICE_MODEL','PAAS','PaaS',20),
('SERVICE_MODEL','SAAS','SaaS',30),
('SERVICE_MODEL','BARE_METAL','Bare Metal',40),

('DEPLOY_MODEL','ON_PREMISE','On-Premise',10),
('DEPLOY_MODEL','PRIVATE_CLOUD','Private Cloud',20),
('DEPLOY_MODEL','PUBLIC_CLOUD','Public Cloud',30),
('DEPLOY_MODEL','HYBRID_CLOUD','Hybrid Cloud',40),
('DEPLOY_MODEL','MULTI_CLOUD','Multi Cloud',50),

('RUNTIME_BASE','BARE_METAL','Bare Metal',10),
('RUNTIME_BASE','VM','Virtual Machine',20),
('RUNTIME_BASE','CONTAINER','Container',30),
('RUNTIME_BASE','K8S','Kubernetes',40),
('RUNTIME_BASE','SERVERLESS','Serverless',50),

('TECH_ROLE','NETWORK_EDGE','Network/Edge',10),
('TECH_ROLE','WEB','Web',20),
('TECH_ROLE','WAS','WAS/Application',30),
('TECH_ROLE','API_GW','API/Gateway',40),
('TECH_ROLE','MW_INTEGRATION','Middleware/Integration',50),
('TECH_ROLE','DATABASE','Database',60),
('TECH_ROLE','CACHE','Cache',70),
('TECH_ROLE','MQ_EVENT','MQ/Event',80),
('TECH_ROLE','BATCH','Batch',90),
('TECH_ROLE','FILE','File',100),
('TECH_ROLE','SEARCH','Search',110),
('TECH_ROLE','DATA_ANALYTICS','Data/Analytics',120),
('TECH_ROLE','AI_ML','AI/ML',130),
('TECH_ROLE','MONITORING','Monitoring/Log',140),
('TECH_ROLE','SECURITY','Security',150),
('TECH_ROLE','DEVOPS','DevOps',160),

('TIER','TIER0','Tier 0 금융 핵심·중앙 공통',10),
('TIER','TIER1','Tier 1 24×365 중요 업무',20),
('TIER','TIER2','Tier 2 주요 정보계 업무',30),
('TIER','TIER3','Tier 3 일반 업무',40),
('TIER','TIER4','Tier 4 개발·검증·교육',50),

('ENV','PROD','운영',10),
('ENV','STG','검증',20),
('ENV','DEV','개발',30),
('ENV','EDU','교육',40),

('ASSET_KIND','BARE_METAL','Bare Metal',10),
('ASSET_KIND','VM','VM',20),
('ASSET_KIND','CONTAINER','Container',30),
('ASSET_KIND','K8S_POD','Kubernetes Pod',40),
('ASSET_KIND','CLOUD_SVC','Cloud Managed Service',50),
('ASSET_KIND','SAAS','SaaS',60),

('LIFECYCLE','DISCOVERED','발견',10),
('LIFECYCLE','VALIDATING','검증중',20),
('LIFECYCLE','CONFIRMED','현행확정',30),
('LIFECYCLE','TARGET_DEFINED','목표정의',40),
('LIFECYCLE','MIGRATION_PLANNED','전환계획',50),
('LIFECYCLE','MIGRATED','전환완료',60),
('LIFECYCLE','RETIRED','폐기',70),

('STRATEGY_7R','REHOST','Rehost',10),
('STRATEGY_7R','REPLATFORM','Replatform',20),
('STRATEGY_7R','REFACTOR','Refactor',30),
('STRATEGY_7R','REPURCHASE','Repurchase/SaaS',40),
('STRATEGY_7R','RETAIN','Retain',50),
('STRATEGY_7R','RETIRE','Retire',60),
('STRATEGY_7R','RELOCATE','Relocate',70),

('NETWORK_ZONE','INTERNET','Internet',10),
('NETWORK_ZONE','DMZ','DMZ',20),
('NETWORK_ZONE','WEB','WEB Zone',30),
('NETWORK_ZONE','APP','APP Zone',40),
('NETWORK_ZONE','DB','DB Zone',50),
('NETWORK_ZONE','MGMT','Management Zone',60),
('NETWORK_ZONE','BACKUP','Backup Zone',70),
('NETWORK_ZONE','DR','DR Zone',80),

('SCALE_MODE','SCALE_OUT','Scale-Out',10),
('SCALE_MODE','SCALE_UP','Scale-Up',20),
('SCALE_MODE','BOTH','Scale-Up/Out',30),

('MW_CATEGORY','WEB','Web Server',10),
('MW_CATEGORY','WAS','WAS',20),
('MW_CATEGORY','MQ','MQ',30),
('MW_CATEGORY','CACHE','Cache',40),
('MW_CATEGORY','SCHEDULER','Scheduler',50),
('MW_CATEGORY','ETL','ETL',60),
('MW_CATEGORY','APIGW','API Gateway',70),
('MW_CATEGORY','OTHER','Other',99),

('RELATION_TYPE','CALLS','호출',10),
('RELATION_TYPE','USES_DB','DB 사용',20),
('RELATION_TYPE','USES_CACHE','Cache 사용',30),
('RELATION_TYPE','USES_MQ','MQ 사용',40),
('RELATION_TYPE','FILE_XFER','파일 전송',50),
('RELATION_TYPE','REPLICATES','복제',60);

-- Quality gates (§30)
INSERT INTO quality_gate_def (gate_id, name_ko, description, sort_order) VALUES
('GATE1','Inventory Complete','필수 컬럼·인벤토리 완성',10),
('GATE2','Dependency Complete','의존관계 완성',20),
('GATE3','Capacity Validated','용량 검증',30),
('GATE4','Target Platform Approved','목표 플랫폼 승인',40),
('GATE5','DR/Security Approved','DR·보안 승인',50),
('GATE6','Cost Approved','비용 승인',60),
('GATE7','Migration Wave Approved','전환 Wave 승인',70);

-- Checklist (§32) 일부
INSERT INTO checklist_item (checklist_id, category_ko, item_text_ko, sort_order) VALUES
('CHK-S01','서버','서버 ID가 존재하는가',10),
('CHK-S02','서버','업무 시스템이 확인되었는가',20),
('CHK-S03','서버','서버 역할이 확인되었는가',30),
('CHK-S04','서버','물리/VM/Container가 구분되었는가',40),
('CHK-S05','서버','CPU·Memory·Storage가 확인되었는가',50),
('CHK-S06','서버','평균 및 Peak 사용량이 있는가',60),
('CHK-S07','서버','OS와 EOL이 확인되었는가',70),
('CHK-M01','Middleware','제품과 버전이 확인되었는가',10),
('CHK-M02','Middleware','License가 확인되었는가',20),
('CHK-M03','Middleware','Cluster/HA가 확인되었는가',30),
('CHK-A01','Application','배포 Application이 확인되었는가',10),
('CHK-A02','Application','외부 연계가 확인되었는가',20),
('CHK-A03','Application','DB 연결이 확인되었는가',30),
('CHK-A04','Application','Peak TPS가 확인되었는가',40),
('CHK-O01','운영','24×365 여부가 확인되었는가',10),
('CHK-O02','운영','중요도가 정의되었는가',20),
('CHK-O03','운영','RTO/RPO가 정의되었는가',30),
('CHK-O04','운영','Backup이 정의되었는가',40),
('CHK-O05','운영','DR이 확인되었는가',50),
('CHK-T01','전환','7R 전략이 결정되었는가',10),
('CHK-T02','전환','IaaS/PaaS/SaaS/Bare Metal 목표가 결정되었는가',20),
('CHK-T03','전환','목표 서버군이 정의되었는가',30),
('CHK-T04','전환','전환 Wave가 결정되었는가',40);

-- Survey templates (대표)
INSERT INTO survey_template (template_id, name_ko, tech_role_cd, doc_section, description) VALUES
('TMPL_COMMON','공통 서버 인벤토리',NULL,'14.1','§14 공통 컬럼'),
('TMPL_BARE_METAL','Bare Metal 조사','DATABASE','5.1','물리 서버 특화'),
('TMPL_WAS','WAS·Application 조사','WAS','6.3','JVM/Thread/Pool'),
('TMPL_WEB','WEB 조사','WEB','6.2','Proxy/SSL'),
('TMPL_RDBMS','RDBMS 조사','DATABASE','7.1','DB 특화'),
('TMPL_BATCH','Batch 조사','BATCH','8.1','배치 특화');

INSERT INTO survey_item (item_id, template_id, category_ko, item_name_ko, data_type, required_yn, sort_order) VALUES
('SI-COM-001','TMPL_COMMON','식별','서버 ID','STRING','Y',10),
('SI-COM-002','TMPL_COMMON','식별','서버명','STRING','Y',20),
('SI-COM-003','TMPL_COMMON','업무','시스템명','STRING','Y',30),
('SI-COM-004','TMPL_COMMON','분류','서버군','STRING','Y',40),
('SI-COM-005','TMPL_COMMON','성능','Peak TPS','NUMBER','N',50),
('SI-COM-006','TMPL_COMMON','운영','RTO','STRING','N',60),
('SI-COM-007','TMPL_COMMON','운영','RPO','STRING','N',70),
('SI-COM-008','TMPL_COMMON','보안','개인정보 여부','BOOL','Y',80),
('SI-COM-009','TMPL_COMMON','전환','7R 전략','STRING','N',90),

('SI-BM-001','TMPL_BARE_METAL','인프라','제조사/모델','STRING','Y',10),
('SI-BM-002','TMPL_BARE_METAL','Compute','CPU Socket','NUMBER','Y',20),
('SI-BM-003','TMPL_BARE_METAL','Compute','Physical Core','NUMBER','Y',30),
('SI-BM-004','TMPL_BARE_METAL','클러스터','Cluster 방식','STRING','N',40),

('SI-WAS-001','TMPL_WAS','Runtime','Java 버전','STRING','Y',10),
('SI-WAS-002','TMPL_WAS','WAS','제품명','STRING','Y',20),
('SI-WAS-003','TMPL_WAS','WAS','버전','STRING','Y',30),
('SI-WAS-004','TMPL_WAS','JVM','Heap','STRING','N',40),
('SI-WAS-005','TMPL_WAS','DB Pool','Max Connection','NUMBER','N',50),
('SI-WAS-006','TMPL_WAS','성능','p95 응답','NUMBER','N',60),

('SI-WEB-001','TMPL_WEB','제품','Web Server 제품','STRING','Y',10),
('SI-WEB-002','TMPL_WEB','설정','Max Connection','NUMBER','N',20),
('SI-WEB-003','TMPL_WEB','보안','SSL/TLS','STRING','N',30),

('SI-DB-001','TMPL_RDBMS','엔진','DB Engine','STRING','Y',10),
('SI-DB-002','TMPL_RDBMS','HA','HA 모드','STRING','Y',20),
('SI-DB-003','TMPL_RDBMS','용량','Size GB','NUMBER','N',30),
('SI-DB-004','TMPL_RDBMS','보안','개인정보 여부','BOOL','Y',40),

('SI-BAT-001','TMPL_BATCH','배치','일 Batch량','NUMBER','N',10),
('SI-BAT-002','TMPL_BATCH','배치','배치 Window','STRING','N',20),
('SI-BAT-003','TMPL_BATCH','스케줄','Scheduler 제품','STRING','N',30);
