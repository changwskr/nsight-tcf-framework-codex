package com.nh.nsight.tcf.core.support.timeout;

/** serviceId + transactionCode + businessCode 기준 Timeout 정책 */
public class TimeoutPolicy {
    private String serviceId;
    private String transactionCode;
    private String businessCode;
    private String serviceName;
    private int onlineTimeoutSec = TcfServiceTimeoutConstants.DEFAULT_ONLINE_TIMEOUT_SEC;
    private int txTimeoutSec = TcfServiceTimeoutConstants.DEFAULT_TX_TIMEOUT_SEC;
    private int dbQueryTimeoutSec = TcfServiceTimeoutConstants.DEFAULT_DB_QUERY_TIMEOUT_SEC;
    private int externalConnectTimeoutMs = TcfServiceTimeoutConstants.DEFAULT_EXTERNAL_CONNECT_TIMEOUT_MS;
    private int externalReadTimeoutMs = TcfServiceTimeoutConstants.DEFAULT_EXTERNAL_READ_TIMEOUT_MS;
    private String timeoutAction = TcfServiceTimeoutConstants.TIMEOUT_ACTION_FAIL;
    private String useYn = "Y";
    private String description;

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public String getBusinessCode() { return businessCode; }
    public void setBusinessCode(String businessCode) { this.businessCode = businessCode; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public int getOnlineTimeoutSec() { return onlineTimeoutSec; }
    public void setOnlineTimeoutSec(int onlineTimeoutSec) { this.onlineTimeoutSec = onlineTimeoutSec; }
    public int getTxTimeoutSec() { return txTimeoutSec; }
    public void setTxTimeoutSec(int txTimeoutSec) { this.txTimeoutSec = txTimeoutSec; }
    public int getDbQueryTimeoutSec() { return dbQueryTimeoutSec; }
    public void setDbQueryTimeoutSec(int dbQueryTimeoutSec) { this.dbQueryTimeoutSec = dbQueryTimeoutSec; }
    public int getExternalConnectTimeoutMs() { return externalConnectTimeoutMs; }
    public void setExternalConnectTimeoutMs(int externalConnectTimeoutMs) {
        this.externalConnectTimeoutMs = externalConnectTimeoutMs;
    }
    public int getExternalReadTimeoutMs() { return externalReadTimeoutMs; }
    public void setExternalReadTimeoutMs(int externalReadTimeoutMs) {
        this.externalReadTimeoutMs = externalReadTimeoutMs;
    }
    public String getTimeoutAction() { return timeoutAction; }
    public void setTimeoutAction(String timeoutAction) { this.timeoutAction = timeoutAction; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
