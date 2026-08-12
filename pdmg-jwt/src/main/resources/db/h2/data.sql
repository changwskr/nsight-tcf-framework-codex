-- 초기 비밀번호: nsight01! (JwtUserPasswordInitializer 가 BCrypt 해시 설정)
INSERT INTO OM_AUTH_GROUP (AUTH_GROUP_ID, AUTH_GROUP_NAME, DESCRIPTION, USE_YN) VALUES
('ROLE_ADMIN', '시스템관리자', 'JWT 로컬 테스트', 'Y'),
('ROLE_OPERATOR', '운영담당자', 'JWT 로컬 테스트', 'Y'),
('ROLE_VIEWER', '조회자', 'JWT 로컬 테스트', 'Y');

INSERT INTO OM_USER (USER_ID, USER_NAME, PASSWORD_HASH, BRANCH_ID, AUTH_GROUP_ID, USE_YN, LAST_LOGIN_TIME) VALUES
('admin01', 'JWT테스트관리자', NULL, '000001', 'ROLE_ADMIN', 'Y', NULL),
('op01', '김운영', NULL, '001234', 'ROLE_OPERATOR', 'Y', NULL),
('view01', '이조회', NULL, '001234', 'ROLE_VIEWER', 'Y', NULL);
