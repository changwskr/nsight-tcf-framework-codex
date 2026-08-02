package nhnis.mp.co.a.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import nhnis.mp.co.a.dto.mpcoa8888DtoIn;
import nhnis.mp.co.a.dto.mpcoa8888DtoOut;

@SpringBootTest
@Transactional
class mpcoa8888DaoIntegrationTest {

    @Autowired
    private mpcoa8888Dao dao;

    @Test
    void insertUpdateAndPhysicalDeleteRoundTrip() {
        mpcoa8888DtoIn row = newRow("88888", "E0008888", "888", "20260801");

        assertThat(dao.mpcoa8888I0_I0(row)).isEqualTo(1);
        assertThat(dao.mpcoa8888S0_S1(row).getPrtoCn()).isEqualTo("등록");

        row.setPrtoCn("수정");
        row.setInqCn("수정 조회");
        row.setInpCn("수정 입력");
        assertThat(dao.mpcoa8888U0_U0(row)).isEqualTo(1);
        mpcoa8888DtoOut updated = dao.mpcoa8888S0_S1(row);
        assertThat(updated.getPrtoCn()).isEqualTo("수정");
        assertThat(updated.getTrtBrc()).isEqualTo("88888");

        assertThat(dao.mpcoa8888D0_D0(row)).isEqualTo(1);
        assertThat(dao.mpcoa8888S0_S1(row)).isNull();
    }

    @Test
    void primaryKeyConstraintRejectsDuplicateInsert() {
        mpcoa8888DtoIn row = newRow("88887", "E0008887", "887", "20260801");
        dao.mpcoa8888I0_I0(row);

        assertThrows(RuntimeException.class, () -> dao.mpcoa8888I0_I0(row));
    }

    @Test
    void listFiltersByExactCodeAndUsesFixedSort() {
        dao.mpcoa8888I0_I0(newRow("88886", "E0008886", "886", "20260731"));
        dao.mpcoa8888I0_I0(newRow("88885", "E0008885", "886", "20260801"));
        dao.mpcoa8888I0_I0(newRow("88884", "E0008884", "884", "20260802"));
        mpcoa8888DtoIn search = new mpcoa8888DtoIn();
        search.setSalzTipKdc("886");
        search.setOffset(0);
        search.setPageSize(20);

        List<mpcoa8888DtoOut> result = dao.mpcoa8888S0_S0(search);

        assertThat(dao.mpcoa8888S0_S0_count(search)).isEqualTo(2);
        assertThat(result).extracting(mpcoa8888DtoOut::getBasDt)
                .containsExactly("20260801", "20260731");
        assertThat(result).extracting(mpcoa8888DtoOut::getSalzTipKdc)
                .containsOnly("886");
    }

    private mpcoa8888DtoIn newRow(String trtBrc, String trtmnEno, String salzTipKdc, String basDt) {
        mpcoa8888DtoIn row = new mpcoa8888DtoIn();
        row.setTrtBrc(trtBrc);
        row.setTrtmnEno(trtmnEno);
        row.setSalzTipKdc(salzTipKdc);
        row.setBasDt(basDt);
        row.setPrtoCn("등록");
        row.setInqCn("조회");
        row.setInpCn("입력");
        return row;
    }
}
