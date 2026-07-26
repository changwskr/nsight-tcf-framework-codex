-- TCF_GATEWAY_ROUTE 기본 구성 데이터 (Oracle)
-- DDL: TCF_GATEWAY_ROUTE.sql 실행 후 본 스크립트 적용
-- TARGET_URL = TARGET_BASE_URL || CONTEXT_PATH || ONLINE_PATH

-- LOCAL: 업무별 bootRun 포트 (application-local.yml 기준)
INSERT INTO TCF_GATEWAY_ROUTE
(ROUTE_ID, ENV_CODE, ROUTE_GROUP_CODE, ROUTE_GROUP_NAME, BUSINESS_CODE, BUSINESS_NAME,
 TARGET_BASE_URL, CONTEXT_PATH, ONLINE_PATH, HEALTH_CHECK_PATH, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS,
 USE_YN, SORT_ORDER, DESCRIPTION)
VALUES
('LOCAL-GW-CC', 'LOCAL', 'MSA-A', '공통/고객 기본 그룹', 'CC', 'Common',
 'http://127.0.0.1:8081', '/cc', '/online', '/cc/actuator/health', 3000, 5000, 'Y', 10, 'LOCAL CC bootRun :8081');

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-IC', 'LOCAL', 'MSA-A', '공통/고객 기본 그룹', 'IC', 'Integration Customer',
 'http://127.0.0.1:8082', '/ic', '/online', '/ic/actuator/health', 3000, 5000, 'Y', 20, 'LOCAL IC bootRun :8082', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-PC', 'LOCAL', 'MSA-A', '공통/고객 기본 그룹', 'PC', 'Private Customer',
 'http://127.0.0.1:8083', '/pc', '/online', '/pc/actuator/health', 3000, 5000, 'Y', 30, 'LOCAL PC bootRun :8083', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-BC', 'LOCAL', 'MSA-B', '고객/마케팅 조회 그룹', 'BC', 'Business Customer',
 'http://127.0.0.1:8084', '/bc', '/online', '/bc/actuator/health', 3000, 5000, 'Y', 40, 'LOCAL BC bootRun :8084', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-MS', 'LOCAL', 'MSA-B', '고객/마케팅 조회 그룹', 'MS', 'Mini Single View',
 'http://127.0.0.1:8085', '/ms', '/online', '/ms/actuator/health', 3000, 5000, 'Y', 50, 'LOCAL MS bootRun :8085', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-SV', 'LOCAL', 'MSA-B', '고객/마케팅 조회 그룹', 'SV', 'Single View',
 'http://127.0.0.1:8086', '/sv', '/online', '/sv/actuator/health', 3000, 5000, 'Y', 60, 'LOCAL SV bootRun :8086', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-PD', 'LOCAL', 'MSA-B', '고객/마케팅 조회 그룹', 'PD', 'Product',
 'http://127.0.0.1:8087', '/pd', '/online', '/pd/actuator/health', 3000, 5000, 'Y', 70, 'LOCAL PD bootRun :8087', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-OM', 'LOCAL', 'GATEWAY', 'Gateway/운영관리', 'OM', 'Operation Management',
 'http://127.0.0.1:8097', '/om', '/online', '/om/actuator/health', 3000, 5000, 'Y', 80, 'LOCAL OM bootRun', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-EB', 'LOCAL', 'GATEWAY', 'Gateway/운영관리', 'EB', 'Enterprise Banking',
 'http://127.0.0.1:8089', '/eb', '/online', '/eb/actuator/health', 3000, 5000, 'Y', 90, 'LOCAL EB bootRun', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-EP', 'LOCAL', 'GATEWAY', 'Gateway/운영관리', 'EP', 'Enterprise Product',
 'http://127.0.0.1:8090', '/ep', '/online', '/ep/actuator/health', 3000, 5000, 'Y', 100, 'LOCAL EP bootRun', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-MG', 'LOCAL', 'GATEWAY', 'Gateway/운영관리', 'MG', 'Marketing',
 'http://127.0.0.1:8096', '/mg', '/online', '/mg/actuator/health', 3000, 5000, 'Y', 110, 'LOCAL MG bootRun', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-SS', 'LOCAL', 'GATEWAY', 'Gateway/운영관리', 'SS', 'Self Service',
 'http://127.0.0.1:8093', '/ss', '/online', '/ss/actuator/health', 3000, 5000, 'Y', 120, 'LOCAL SS bootRun', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('LOCAL-GW-JWT', 'LOCAL', 'GATEWAY', 'Gateway/운영관리', 'JWT', 'JWT Auth',
 'http://127.0.0.1:8110', '', '/online', '/actuator/health', 3000, 5000, 'Y', 130, 'LOCAL JWT bootRun', SYSDATE, NULL);

-- DEV
INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-CC', 'DEV', 'MSA-A', '공통/고객 기본 그룹', 'CC', 'Common',
 'http://dev-msa-a-service:8080', '/cc', '/online', '/cc/actuator/health', 3000, 5000, 'Y', 10, 'DEV CC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-IC', 'DEV', 'MSA-A', '공통/고객 기본 그룹', 'IC', 'Integration Customer',
 'http://dev-msa-a-service:8080', '/ic', '/online', '/ic/actuator/health', 3000, 5000, 'Y', 20, 'DEV IC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-PC', 'DEV', 'MSA-A', '공통/고객 기본 그룹', 'PC', 'Private Customer',
 'http://dev-msa-a-service:8080', '/pc', '/online', '/pc/actuator/health', 3000, 5000, 'Y', 30, 'DEV PC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-BC', 'DEV', 'MSA-B', '고객/마케팅 조회 그룹', 'BC', 'Business Customer',
 'http://dev-msa-b-service:9090', '/bc', '/online', '/bc/actuator/health', 3000, 5000, 'Y', 40, 'DEV BC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-MS', 'DEV', 'MSA-B', '고객/마케팅 조회 그룹', 'MS', 'Mini Single View',
 'http://dev-msa-b-service:9090', '/ms', '/online', '/ms/actuator/health', 3000, 5000, 'Y', 50, 'DEV MS 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-SV', 'DEV', 'MSA-B', '고객/마케팅 조회 그룹', 'SV', 'Single View',
 'http://dev-msa-b-service:9090', '/sv', '/online', '/sv/actuator/health', 3000, 5000, 'Y', 60, 'DEV SV 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-PD', 'DEV', 'MSA-B', '고객/마케팅 조회 그룹', 'PD', 'Product',
 'http://dev-msa-b-service:9090', '/pd', '/online', '/pd/actuator/health', 3000, 5000, 'Y', 70, 'DEV PD 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-OM', 'DEV', 'GATEWAY', 'Gateway/운영관리', 'OM', 'Operation Management',
 'http://localhost:8080', '/om', '/online', '/om/actuator/health', 3000, 5000, 'Y', 80, 'DEV OM tomcat', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-EB', 'DEV', 'GATEWAY', 'Gateway/운영관리', 'EB', 'Enterprise Banking',
 'http://localhost:8080', '/eb', '/online', '/eb/actuator/health', 3000, 5000, 'Y', 90, 'DEV EB tomcat', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-EP', 'DEV', 'GATEWAY', 'Gateway/운영관리', 'EP', 'Enterprise Product',
 'http://localhost:8080', '/ep', '/online', '/ep/actuator/health', 3000, 5000, 'Y', 100, 'DEV EP tomcat', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-MG', 'DEV', 'GATEWAY', 'Gateway/운영관리', 'MG', 'Marketing',
 'http://localhost:8080', '/mg', '/online', '/mg/actuator/health', 3000, 5000, 'Y', 110, 'DEV MG tomcat', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-SS', 'DEV', 'GATEWAY', 'Gateway/운영관리', 'SS', 'Self Service',
 'http://localhost:8080', '/ss', '/online', '/ss/actuator/health', 3000, 5000, 'Y', 120, 'DEV SS tomcat', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('DEV-GW-JWT', 'DEV', 'GATEWAY', 'Gateway/운영관리', 'JWT', 'JWT Auth',
 'http://localhost:8080', '/jwt', '/online', '/jwt/actuator/health', 3000, 5000, 'Y', 130, 'DEV JWT tomcat', SYSDATE, NULL);

-- PRD (운용)
INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-CC', 'PRD', 'MSA-A', '공통/고객 기본 그룹', 'CC', 'Common',
 'http://msa-a-service:8080', '/cc', '/online', '/cc/actuator/health', 3000, 5000, 'Y', 10, 'PRD CC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-IC', 'PRD', 'MSA-A', '공통/고객 기본 그룹', 'IC', 'Integration Customer',
 'http://msa-a-service:8080', '/ic', '/online', '/ic/actuator/health', 3000, 5000, 'Y', 20, 'PRD IC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-PC', 'PRD', 'MSA-A', '공통/고객 기본 그룹', 'PC', 'Private Customer',
 'http://msa-a-service:8080', '/pc', '/online', '/pc/actuator/health', 3000, 5000, 'Y', 30, 'PRD PC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-BC', 'PRD', 'MSA-B', '고객/마케팅 조회 그룹', 'BC', 'Business Customer',
 'http://msa-b-service:9090', '/bc', '/online', '/bc/actuator/health', 3000, 5000, 'Y', 40, 'PRD BC 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-MS', 'PRD', 'MSA-B', '고객/마케팅 조회 그룹', 'MS', 'Mini Single View',
 'http://msa-b-service:9090', '/ms', '/online', '/ms/actuator/health', 3000, 5000, 'Y', 50, 'PRD MS 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-SV', 'PRD', 'MSA-B', '고객/마케팅 조회 그룹', 'SV', 'Single View',
 'http://msa-b-service:9090', '/sv', '/online', '/sv/actuator/health', 3000, 5000, 'Y', 60, 'PRD SV 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-PD', 'PRD', 'MSA-B', '고객/마케팅 조회 그룹', 'PD', 'Product',
 'http://msa-b-service:9090', '/pd', '/online', '/pd/actuator/health', 3000, 5000, 'Y', 70, 'PRD PD 라우팅', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-OM', 'PRD', 'GATEWAY', 'Gateway/운영관리', 'OM', 'Operation Management',
 'http://msa-a-service:8080', '/om', '/online', '/om/actuator/health', 3000, 5000, 'Y', 80, 'PRD OM', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-EB', 'PRD', 'GATEWAY', 'Gateway/운영관리', 'EB', 'Enterprise Banking',
 'http://msa-a-service:8080', '/eb', '/online', '/eb/actuator/health', 3000, 5000, 'Y', 90, 'PRD EB', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-EP', 'PRD', 'GATEWAY', 'Gateway/운영관리', 'EP', 'Enterprise Product',
 'http://msa-a-service:8080', '/ep', '/online', '/ep/actuator/health', 3000, 5000, 'Y', 100, 'PRD EP', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-MG', 'PRD', 'GATEWAY', 'Gateway/운영관리', 'MG', 'Marketing',
 'http://msa-a-service:8080', '/mg', '/online', '/mg/actuator/health', 3000, 5000, 'Y', 110, 'PRD MG', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-SS', 'PRD', 'GATEWAY', 'Gateway/운영관리', 'SS', 'Self Service',
 'http://msa-a-service:8080', '/ss', '/online', '/ss/actuator/health', 3000, 5000, 'Y', 120, 'PRD SS', SYSDATE, NULL);

INSERT INTO TCF_GATEWAY_ROUTE VALUES
('PRD-GW-JWT', 'PRD', 'GATEWAY', 'Gateway/운영관리', 'JWT', 'JWT Auth',
 'http://msa-a-service:8080', '/jwt', '/online', '/jwt/actuator/health', 3000, 5000, 'Y', 130, 'PRD JWT', SYSDATE, NULL);

COMMIT;
