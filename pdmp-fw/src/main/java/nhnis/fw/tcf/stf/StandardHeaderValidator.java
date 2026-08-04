package nhnis.fw.tcf.stf;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import nhnis.fw.exception.BizException;
import nhnis.fw.tcf.dto.StandardHeaderDto;

/**
 * 표준 Header 필수값 검증.
 */
@Component
public class StandardHeaderValidator {

    /** exceptionCode.yml FW0001 - "{0} 오류가 발생하였습니다." */
    private static final String CODE_REQUIRED = "FW0001";

    /**
     * @param clientSuppliedHeader 클라이언트가 전문 Header를 실어 보냈는지 여부.
     *                             채널 정보는 전문으로 들어온 거래에만 요구한다.
     */
    public void validate(StandardHeaderDto header, boolean clientSuppliedHeader) {
        System.out.println("=========[StandardHeaderValidator][validate][START] "
                + "serviceId=" + header.getServiceId()
                + " transactionCode=" + header.getTransactionCode()
                + " processingType=" + header.getProcessingType()
                + " clientSuppliedHeader=" + clientSuppliedHeader);
        required(header.getBusinessCode(), "필수 Header businessCode");
        required(header.getServiceId(), "필수 Header serviceId");
        required(header.getTransactionCode(), "필수 Header transactionCode");
        required(header.getProcessingType(), "필수 Header processingType");
        if (clientSuppliedHeader) {
            required(header.getChannelId(), "필수 Header channelId");
        }
        System.out.println("=========[StandardHeaderValidator][validate][END] "
                + "serviceId=" + header.getServiceId());
    }

    private void required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(CODE_REQUIRED, fieldName);
        }
    }
}
