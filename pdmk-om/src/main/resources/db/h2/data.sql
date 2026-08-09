-- Local H2 seed data (ASCII-only via STRINGDECODE to avoid Windows CP949 mojibake).

INSERT INTO TB_CR_AH_SALES_TIP_RACT
    (TRT_BRC, TRTMN_ENO, SALZ_TIP_KDC, BAS_DT, PRTO_CN, INQ_CN, INP_CN)
VALUES
    ('10001', 'E0000001', '001', '20260801',
     STRINGDECODE('\uC608\uAE08\u0020\uD3EC\uD2B8\uD3F4\uB9AC\uC624\u0020\uC810\uAC80\u0020\uB300\uC0C1'),
     STRINGDECODE('\uB9CC\uAE30\u0020\uB3C4\uB798\u0020\u0033\uAC74\u0020\uC870\uD68C'),
     STRINGDECODE('\uC0C1\uB2F4\u0020\uC608\uC57D\u0020\uC644\uB8CC')),
    ('10001', 'E0000001', '002', '20260801',
     STRINGDECODE('\uB300\uCD9C\u0020\uD3EC\uD2B8\uD3F4\uB9AC\uC624\u0020\uC810\uAC80\u0020\uB300\uC0C1'),
     STRINGDECODE('\uC5F0\uCCB4\u0020\uC774\uB825\u0020\uC5C6\uC74C'),
     STRINGDECODE('\uAE08\uB9AC\u0020\uC778\uD558\u0020\uC548\uB0B4')),
    ('10001', 'E0000002', '001', '20260801',
     STRINGDECODE('\uC2E0\uADDC\u0020\uACE0\uAC1D\u0020\uC720\uCE58\u0020\uB300\uC0C1'),
     STRINGDECODE('\uAC70\uB798\u0020\uC2E4\uC801\u0020\uBBF8\uB2EC'),
     STRINGDECODE('\uBC29\uBB38\u0020\uC0C1\uB2F4\u0020\uC694\uCCAD')),
    ('10002', 'E0000003', '003', '20260731',
     STRINGDECODE('\uCE74\uB4DC\u0020\uC774\uC6A9\u0020\uC2E4\uC801\u0020\uD558\uC704'),
     STRINGDECODE('\uC804\uC6D4\u0020\uB300\uBE44\u0020\uAC10\uC18C'),
     STRINGDECODE('\uD61C\uD0DD\u0020\uC548\uB0B4\u0020\uBC1C\uC1A1')),
    ('10002', 'E0000003', '003', '20260801',
     STRINGDECODE('\uCE74\uB4DC\u0020\uC774\uC6A9\u0020\uC2E4\uC801\u0020\uD558\uC704'),
     STRINGDECODE('\uC804\uC6D4\u0020\uB300\uBE44\u0020\uC99D\uAC00'),
     STRINGDECODE('\uCD94\uAC00\u0020\uC548\uB0B4\u0020\uC5C6\uC74C'));

-- mkcoa6666 Service Catalog + 02화면 POLICY_JSON 샘플
INSERT INTO TB_OM_SVC_CATALOG (
    SERVICE_CODE, SERVICE_NAME, BUSINESS_CODE, SCID, ENABLED, STATUS,
    ALLOWED_SYSTEM_IDS, ALLOWED_TERMINAL_TYPES, ALLOWED_BRANCHES, REQUIRED_AUTHORITIES,
    SYNC_TYPE, ALLOWED_START_TIME, ALLOWED_END_TIME, TIMEOUT_MS, MAX_TPS, MAX_CONCURRENT,
    DUPLICATE_WINDOW_SEC, AUDIT_LEVEL, REASON, ONLINE_FORCE_YN, POLICY_JSON, REG_DTM, CHG_DTM
) VALUES
    ('mkcoa5530S0', STRINGDECODE('\uC548\uB0B4\uD56D\uBAA9\u0020\uC870\uD68C'), 'mk', 'mkcoa5530', 'Y', 'NORMAL',
     'PDMK,PDMK-UI,PDMK-OM', '01,02,03', '*', NULL,
     'S', '0000', '2400', 3000, 100, 50,
     0, 'NORMAL', STRINGDECODE('\uC548\uB0B4\uD56D\uBAA9\u0020\uAE30\uBCF8\u0020\uD5C8\uC6A9'), 'N',
     '{"serviceCtrlUse":"Y","branchMode":"ALL","allowBranches":[],"denyBranches":[],"terminalTypes":{"01":"Y","02":"Y","03":"Y","04":"N","05":"N"},"unknownTerminalAction":"BLOCK","ipCtrlUse":"N","allowIps":[],"denyIps":[],"unknownIpAction":"ALLOW","loopbackPolicy":"DEV_ONLY","timeWindows":{"weekday":{"start":"0000","end":"2400","allow":"Y"},"saturday":{"start":"0000","end":"2400","allow":"Y"},"sunday":{"start":"0000","end":"2400","allow":"Y"},"holiday":{"start":"0000","end":"2400","allow":"Y"}},"blockWindows":[],"outOfHoursAction":"BLOCK","userCtrlUse":"N","userDefaultPolicy":"ALLOW_AUTH","allowUsers":[],"denyUsers":[],"authMatchRequired":"N","onlineForce":{"active":"N","scope":"SERVICE","mode":"IMMEDIATE","reason":"","startDtm":"","endDtm":""}}',
     '20260808000000', '20260808000000'),
    ('mkcoa9999S0', STRINGDECODE('\uC601\uC5C5\uD301\u0020\uC2E4\uC801\u0020\uC870\uD68C'), 'mk', 'mkcoa9999', 'Y', 'NORMAL',
     'PDMK,PDMK-UI', '01', '10001', NULL,
     'S', '0900', '1800', 5000, 50, 20,
     0, 'HIGH', STRINGDECODE('\uC601\uC5C5\uD301\u0020\u0031\u0030\u0030\u0030\u0031\uC810\u0020\uD5C8\uC6A9'), 'N',
     '{"serviceCtrlUse":"Y","branchMode":"LIST","allowBranches":[{"code":"10001","name":"HQ","enabled":"Y"}],"denyBranches":[{"code":"20001","name":"JEJU","enabled":"N"}],"terminalTypes":{"01":"Y","02":"N","03":"N","04":"N","05":"N"},"unknownTerminalAction":"BLOCK","ipCtrlUse":"N","allowIps":[],"denyIps":[],"unknownIpAction":"BLOCK","loopbackPolicy":"DEV_ONLY","timeWindows":{"weekday":{"start":"0900","end":"1800","allow":"Y"},"saturday":{"start":"0900","end":"1800","allow":"Y"},"sunday":{"start":"0000","end":"0000","allow":"N"},"holiday":{"start":"0000","end":"0000","allow":"N"}},"blockWindows":[{"start":"1200","end":"1300","reason":"lunch"}],"outOfHoursAction":"BLOCK","userCtrlUse":"N","userDefaultPolicy":"ALLOW_AUTH","allowUsers":[],"denyUsers":[],"authMatchRequired":"N","onlineForce":{"active":"N","scope":"SERVICE","mode":"IMMEDIATE","reason":"","startDtm":"","endDtm":""}}',
     '20260808000000', '20260808000000'),
    ('mkcoa8888S0', STRINGDECODE('\uC774\uBBF8\uC9C0\uB85C\uADF8\u0020\uC870\uD68C\u0028service\u0029'), 'mk', 'mkcoa8888', 'N', 'STOP',
     '*', '*', '*', NULL,
     'S', '0000', '2400', 3000, 20, 10,
     0, 'NORMAL', STRINGDECODE('\uC77C\uC2DC\u0020\uC0AC\uC6A9\uC911\uC9C0\u0020\uC608\uC2DC'), 'N', NULL,
     '20260808000000', '20260808000000'),
    ('mkcoa7777S0', STRINGDECODE('\uC774\uBBF8\uC9C0\uB85C\uADF8\u0020\uC870\uD68C\u0028OM\u0029'), 'om', 'mkcoa7777', 'Y', 'NORMAL',
     'PDMK-OM,PDMK-UI', '*', '*', NULL,
     'S', '0000', '2400', 3000, 50, 20,
     0, 'NORMAL', 'OM imagelog', 'N', NULL,
     '20260808000000', '20260808000000'),
    ('mkcoa6666S0', STRINGDECODE('\uAC70\uB798\uD1B5\uC81C\u0020\uCE74\uD0C8\uB85C\uADF8\u0020\uC870\uD68C'), 'om', 'mkcoa6666', 'Y', 'NORMAL',
     'PDMK-OM,PDMK-UI', '01,02,03', '10001,10002,10003', NULL,
     'S', '0800', '2200', 3000, 100, 50,
     0, 'HIGH', STRINGDECODE('\uAC70\uB798\uD1B5\uC81C\u0020\uC6D0\uC7A5\u0020\uAD00\uB9AC'), 'N',
     '{"serviceCtrlUse":"Y","branchMode":"OWN_AND_ALLOW","allowBranches":[{"code":"10001","name":"HQ","enabled":"Y"},{"code":"10002","name":"Gangnam","enabled":"Y"},{"code":"10003","name":"Seocho","enabled":"Y"}],"denyBranches":[{"code":"20001","name":"Jeju","enabled":"N"},{"code":"30001","name":"Test","enabled":"N"}],"terminalTypes":{"01":"Y","02":"Y","03":"Y","04":"N","05":"N"},"unknownTerminalAction":"BLOCK","ipCtrlUse":"Y","allowIps":["10.10.10.0/24","10.20.30.15","172.16.100.0/24"],"denyIps":["192.168.99.100"],"unknownIpAction":"BLOCK","loopbackPolicy":"DEV_ONLY","timeWindows":{"weekday":{"start":"0800","end":"2200","allow":"Y"},"saturday":{"start":"0900","end":"1800","allow":"Y"},"sunday":{"start":"0000","end":"0000","allow":"N"},"holiday":{"start":"0000","end":"0000","allow":"N"}},"blockWindows":[{"start":"1200","end":"1300","reason":"maint"},{"start":"2350","end":"0020","reason":"eod"}],"outOfHoursAction":"BLOCK","userCtrlUse":"Y","userDefaultPolicy":"ALLOW_AUTH","allowUsers":[{"userId":"E0000001","userName":"Hong","branch":"10001","status":"NORMAL","allow":"Y"},{"userId":"E0000002","userName":"Kim","branch":"10001","status":"NORMAL","allow":"Y"}],"denyUsers":[{"userId":"E0000100","userName":"Test","branch":"99999","status":"BLOCK","allow":"N"}],"authMatchRequired":"Y","onlineForce":{"active":"N","scope":"SERVICE","mode":"IMMEDIATE","reason":"","startDtm":"","endDtm":""}}',
     '20260808000000', '20260808000000'),
    ('mkcoa6666E0', STRINGDECODE('\uAC70\uB798\uD1B5\uC81C\u0020\uD3C9\uAC00'), 'om', 'mkcoa6666', 'Y', 'NORMAL',
     'PDMK-OM,PDMK-UI', '*', '*', NULL,
     'S', '0000', '2400', 3000, 200, 100,
     0, 'HIGH', STRINGDECODE('sys_comm \uD3C9\uAC00'), 'N', NULL,
     '20260808000000', '20260808000000'),
    ('demo.maint.S0', STRINGDECODE('\uC810\uAC80\uC911\u0020\uC0D8\uD50C'), 'mk', 'demo', 'Y', 'MAINTENANCE',
     '*', '*', '*', NULL,
     'S', '0000', '2400', 3000, 10, 5,
     0, 'NORMAL', STRINGDECODE('\uC810\uAC80\uC911\u0020\uCC28\uB2E8\u0020\uC608\uC2DC'), 'N', NULL,
     '20260808000000', '20260808000000');

INSERT INTO TB_OM_TX_CTRL_RESULT (
    RESULT_ID, STD_GBL_ID, SERVICE_CODE, OPTR_ENO, TR_BRC, TRM_KDC, TR_TRM_IPADR,
    CONTROL_RESULT, ERROR_CODE, REASON, CHECK_STEP, REG_DTM
) VALUES
    ('R0001', '992674f81e9d4762b0d56a7fb38a1cc0', 'mkcoa6666S0', 'E0000001', '10001', '01', '10.10.10.11',
     'ALLOW', NULL, NULL, 21, '20260808222951'),
    ('R0002', '882811aa210aa210aa210aa210aa210a', 'mkcoa6666S0', 'E0000002', '20001', '01', '10.10.10.12',
     'BLOCK', 'TCF-CTL-007', 'BRANCH', 12, '20260808222943'),
    ('R0003', '271122bb910bd910bd910bd910bd910b', 'mkcoa6666S0', 'E0000100', '10001', '01', '10.10.10.13',
     'BLOCK', 'TCF-CTL-008', 'USER', 8, '20260808222930'),
    ('R0004', '663301ccabc12abc12abc12abc12abc1', 'mkcoa6666S0', 'E0000001', '10001', '04', '10.10.10.14',
     'BLOCK', 'TCF-CTL-006', 'TERMINAL', 13, '20260808222918'),
    ('R0005', '733921ddff021ff021ff021ff021ff02', 'mkcoa6666S0', 'E0000001', '10001', '01', '192.168.99.100',
     'BLOCK', 'TCF-CTL-015', 'IP', 13, '20260808222854');

-- mkcoa5530S0 sample rows (Total: 3)
INSERT INTO TB_MK_CO_A_5530 (L5101, L5102, L5103, L5104)
VALUES
    ('18', STRINGDECODE('\uC608\uAE08\uB9CC\uAE30\uC548\uB0B4'), '20260801', '10001'),
    ('19', STRINGDECODE('\uB300\uCD9C\uAE08\uB9AC\uC548\uB0B4'), '20260801', '10001'),
    ('20', STRINGDECODE('\uCE74\uB4DC\uD61C\uD0DD\uC548\uB0B4'), '20260801', '10002');
