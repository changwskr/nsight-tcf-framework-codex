package nhnis.fw.commons.apigw;

public enum ApiGatewayHeaderType {

    INTERFACE_ID("interface-id"),
    CONTENT_TYPE("content-type"),
    RECV_TRX_NAME("recv-trx-name"),
    RECV_TYPE("recv-type"),
    SEND_TYPE("send-type"),
    REPLY_TYPE("reply_type");

    private final String value;

    ApiGatewayHeaderType(String value) {
        this.value = value;
    }

    public String get() {
        return value;
    }
}
