package nhnis.mp.co.a.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nhnis.fw.tcf.TcfTransaction;
import nhnis.fw.tcf.dto.ProcessingType;
import nhnis.fw.tcf.dto.StandardRequestDto;
import nhnis.fw.tcf.dto.StandardResponseDto;
import nhnis.mp.co.a.dto.mpcoa8888DtoIn;
import nhnis.mp.co.a.dto.mpcoa8888ListResponseDto;
import nhnis.mp.co.a.service.mpcoa8888Service;

@ExtendWith(MockitoExtension.class)
class mpcoa8888ControllerTest {

    @Mock
    private mpcoa8888Service service;

    @InjectMocks
    private mpcoa8888Controller controller;

    @Test
    void listWrapsServiceResultInStandardResponse() {
        mpcoa8888DtoIn body = new mpcoa8888DtoIn();
        StandardRequestDto<mpcoa8888DtoIn> request = new StandardRequestDto<>(null, body);
        mpcoa8888ListResponseDto serviceResult = new mpcoa8888ListResponseDto();
        when(service.selectSalesTipList(body)).thenReturn(serviceResult);

        StandardResponseDto<mpcoa8888ListResponseDto> response = controller.selectSalesTipList(request);

        assertThat(response.getBody()).isSameAs(serviceResult);
    }

    @Test
    void createDelegatesRequestBody() {
        mpcoa8888DtoIn body = new mpcoa8888DtoIn();
        StandardRequestDto<mpcoa8888DtoIn> request = new StandardRequestDto<>(null, body);

        StandardResponseDto<Void> response = controller.createSalesTip(request);

        verify(service).createSalesTip(body);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void allTcfMetadataMatchesContract() throws Exception {
        assertMetadata("selectSalesTipList", "MP.SalesTip8888.list", "MP-INQ-8881", ProcessingType.INQUIRY);
        assertMetadata("selectSalesTipDetail", "MP.SalesTip8888.detail", "MP-INQ-8882", ProcessingType.INQUIRY);
        assertMetadata("createSalesTip", "MP.SalesTip8888.create", "MP-CRT-8883", ProcessingType.CREATE);
        assertMetadata("updateSalesTip", "MP.SalesTip8888.update", "MP-UPD-8884", ProcessingType.UPDATE);
        assertMetadata("deleteSalesTip", "MP.SalesTip8888.delete", "MP-DEL-8885", ProcessingType.DELETE);
    }

    private void assertMetadata(String methodName, String serviceId, String transactionCode,
            ProcessingType processingType) throws Exception {
        Method method = mpcoa8888Controller.class.getMethod(methodName, StandardRequestDto.class);
        TcfTransaction transaction = method.getAnnotation(TcfTransaction.class);
        assertThat(transaction).isNotNull();
        assertThat(transaction.serviceId()).isEqualTo(serviceId);
        assertThat(transaction.transactionCode()).isEqualTo(transactionCode);
        assertThat(transaction.processingType()).isEqualTo(processingType);
    }
}
