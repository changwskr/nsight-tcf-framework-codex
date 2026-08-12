package nhnis.mg.co.a.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nhnis.mg.co.a.persistence.dao.mgcoa5530DAO;
import nhnis.mg.co.a.dto.mgcoa5530S0DTOin;
import nhnis.mg.co.a.dto.mgcoa5530S0DTOout;

@ExtendWith(MockitoExtension.class)
class mgcoa5530ServiceTest {

    @Mock
    private mgcoa5530DAO dao;

    @InjectMocks
    private mgcoa5530Service service;

    @Test
    void mgcoa5530S0_mapsRowsAndPaging() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put("L5101", "A01");
        row.put("L5102", "항목");
        when(dao.mgcoa5530S0_S0_count(any())).thenReturn(3);
        when(dao.mgcoa5530S0_S0(any())).thenReturn(List.of(row, new HashMap<>(), new HashMap<>()));

        mgcoa5530S0DTOout result = service.mgcoa5530S0(new mgcoa5530S0DTOin());

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.sizemgcoa5530S0DTOSub0()).isEqualTo(3);
        assertThat(result.getmgcoa5530S0DTOSub0(0).getL5101()).isEqualTo("A01");
    }

    @Test
    void mgcoa5530S0_appliesOffset() throws Exception {
        mgcoa5530S0DTOin in = new mgcoa5530S0DTOin();
        in.setPageNo(2);
        in.setPageSize(10);
        when(dao.mgcoa5530S0_S0_count(any())).thenReturn(3);
        when(dao.mgcoa5530S0_S0(any())).thenReturn(List.of());

        mgcoa5530S0DTOout result = service.mgcoa5530S0(in);

        assertThat(result.getPageNo()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        verify(dao).mgcoa5530S0_S0(argThat(param ->
                Integer.valueOf(10).equals(param.get("offset"))
                        && Integer.valueOf(10).equals(param.get("pageSize"))));
    }
}
