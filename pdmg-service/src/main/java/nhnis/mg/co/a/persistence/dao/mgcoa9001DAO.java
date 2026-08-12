package nhnis.mg.co.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.co.a.config.RDWMapper;

/**
 * 거래통제 DAO (OM TransactionControl 대응).
 * 복합키: serviceId + transactionCode + businessCode + serviceName + userId + channelId + branchId
 *
 * @logicalName mgcoa9001DAO
 */
@RDWMapper
public interface mgcoa9001DAO {

    List<Map<String, Object>> mgcoa9001S0_S0(Map<String, Object> input) throws Exception;

    int mgcoa9001S0_S0_count(Map<String, Object> input) throws Exception;

    int mgcoa9001S0_S0_exists(Map<String, Object> input) throws Exception;

    int mgcoa9001C0_C0(Map<String, Object> input) throws Exception;

    int mgcoa9001U0_U0(Map<String, Object> input) throws Exception;

    int mgcoa9001D0_D0(Map<String, Object> input) throws Exception;
}
