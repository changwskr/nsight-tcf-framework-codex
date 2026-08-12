package nhnis.fw.commons.txcontrol;

/**
 * 거래통제 평가에 쓰는 요청 컨텍스트 (OM TransactionControlHeader 대응).
 */
public final class MgTxControlRequest {

    private final String serviceId;
    private final String transactionCode;
    private final String businessCode;
    private final String serviceName;
    private final String userId;
    private final String channelId;
    private final String branchId;
    private final String clientIp;

    public MgTxControlRequest(
            String serviceId,
            String transactionCode,
            String businessCode,
            String serviceName,
            String userId,
            String channelId,
            String branchId,
            String clientIp) {
        this.serviceId = serviceId;
        this.transactionCode = transactionCode;
        this.businessCode = businessCode;
        this.serviceName = serviceName;
        this.userId = userId;
        this.channelId = channelId;
        this.branchId = branchId;
        this.clientIp = clientIp;
    }

    public String getServiceId() { return serviceId; }
    public String getTransactionCode() { return transactionCode; }
    public String getBusinessCode() { return businessCode; }
    public String getServiceName() { return serviceName; }
    public String getUserId() { return userId; }
    public String getChannelId() { return channelId; }
    public String getBranchId() { return branchId; }
    public String getClientIp() { return clientIp; }
}
