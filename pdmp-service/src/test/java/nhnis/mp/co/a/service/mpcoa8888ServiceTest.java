package nhnis.mp.co.a.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import nhnis.fw.exception.BizException;
import nhnis.mp.co.a.dao.mpcoa8888Dao;
import nhnis.mp.co.a.dto.mpcoa8888DtoIn;
import nhnis.mp.co.a.dto.mpcoa8888DtoOut;
import nhnis.mp.co.a.dto.mpcoa8888ListResponseDto;

@ExtendWith(MockitoExtension.class)
class mpcoa8888ServiceTest {

    @Mock
    private mpcoa8888Dao dao;

    @InjectMocks
    private mpcoa8888Service service;

    @Test
    void listAppliesDefaultsMaximumPageSizeAndTrimmedFilter() {
        mpcoa8888DtoIn in = new mpcoa8888DtoIn();
        in.setSalzTipKdc(" 001 ");
        in.setPageNo(0);
        in.setPageSize(500);
        when(dao.mpcoa8888S0_S0_count(any())).thenReturn(101);
        when(dao.mpcoa8888S0_S0(any())).thenReturn(List.of(new mpcoa8888DtoOut()));

        mpcoa8888ListResponseDto result = service.selectSalesTipList(in);

        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotalCount()).isEqualTo(101);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(dao).mpcoa8888S0_S0(argThat(param -> param.getOffset() == 0
                && param.getPageSize() == 100
                && "001".equals(param.getSalzTipKdc())));
    }

    @Test
    void detailRejectsMissingRow() {
        mpcoa8888DtoIn in = completeInput();
        when(dao.mpcoa8888S0_S1(any())).thenReturn(null);

        BizException error = assertThrows(BizException.class, () -> service.selectSalesTipDetail(in));

        assertThat(error.getCode()).isEqualTo("MP0404");
    }

    @Test
    void detailRequiresEveryPrimaryKeyField() {
        mpcoa8888DtoIn in = completeInput();
        in.setBasDt(" ");

        BizException error = assertThrows(BizException.class, () -> service.selectSalesTipDetail(in));

        assertThat(error.getCode()).isEqualTo("FW0001");
        verify(dao, never()).mpcoa8888S0_S1(any());
    }

    @Test
    void createRejectsDuplicateKey() {
        mpcoa8888DtoIn in = completeInput();
        when(dao.mpcoa8888S0_S1(any())).thenReturn(new mpcoa8888DtoOut());

        BizException error = assertThrows(BizException.class, () -> service.createSalesTip(in));

        assertThat(error.getCode()).isEqualTo("MP0409");
        verify(dao, never()).mpcoa8888I0_I0(any());
    }

    @Test
    void createNormalizesAndInsertsAllValues() {
        mpcoa8888DtoIn in = completeInput();
        in.setPrtoCn("  등록 내용  ");
        when(dao.mpcoa8888S0_S1(any())).thenReturn(null);
        when(dao.mpcoa8888I0_I0(any())).thenReturn(1);

        service.createSalesTip(in);

        verify(dao).mpcoa8888I0_I0(argThat(param -> "10001".equals(param.getTrtBrc())
                && "등록 내용".equals(param.getPrtoCn())));
    }

    @Test
    void updateAndDeleteRejectMissingRows() {
        mpcoa8888DtoIn in = completeInput();
        when(dao.mpcoa8888U0_U0(any())).thenReturn(0);
        assertThat(assertThrows(BizException.class, () -> service.updateSalesTip(in)).getCode())
                .isEqualTo("MP0404");

        when(dao.mpcoa8888D0_D0(any())).thenReturn(0);
        assertThat(assertThrows(BizException.class, () -> service.deleteSalesTip(in)).getCode())
                .isEqualTo("MP0404");
    }

    @Test
    void writeMethodsHaveFourSecondTransactions() throws Exception {
        assertWriteTransaction("createSalesTip");
        assertWriteTransaction("updateSalesTip");
        assertWriteTransaction("deleteSalesTip");
        Transactional classTransaction = mpcoa8888Service.class.getAnnotation(Transactional.class);
        assertThat(classTransaction.readOnly()).isTrue();
    }

    private void assertWriteTransaction(String methodName) throws Exception {
        Method method = mpcoa8888Service.class.getMethod(methodName, mpcoa8888DtoIn.class);
        Transactional transaction = method.getAnnotation(Transactional.class);
        assertThat(transaction).isNotNull();
        assertThat(transaction.timeout()).isEqualTo(4);
        assertThat(transaction.readOnly()).isFalse();
    }

    private mpcoa8888DtoIn completeInput() {
        mpcoa8888DtoIn in = new mpcoa8888DtoIn();
        in.setTrtBrc(" 10001 ");
        in.setTrtmnEno(" E0000001 ");
        in.setSalzTipKdc(" 001 ");
        in.setBasDt(" 20260801 ");
        in.setPrtoCn("등록");
        in.setInqCn("조회");
        in.setInpCn("입력");
        return in;
    }
}
