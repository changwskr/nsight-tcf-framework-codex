-- =============================================================================
-- LN OM Catalog / Timeout 시드 초안 [설계 예시]
-- AA 승인 전 운영·OM DB 반영 금지.
-- 실제 OM 테이블 컬럼명은 프로젝트 OM 스키마에 맞게 조정할 것.
-- =============================================================================

-- ServiceId: LN.CustomerContact.selectList / 거래코드: LN-INQ-0001 / Timeout: 5
-- ServiceId: LN.CustomerContact.selectDetail / 거래코드: LN-INQ-0002 / Timeout: 5
-- Handler: LnCustomerContactHandler / 모듈: ln-service (ln.war)
-- processingType: INQUIRY / 감사: Y / 거래통제: 시범 기본(없음)

-- TODO(AA): OM Catalog INSERT 2건
-- TODO(AA): OM Timeout INSERT 2건 (5초)
-- TODO(AA): 권한코드 값 확정
