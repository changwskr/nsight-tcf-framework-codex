package nhnis.mk.co.a.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class mkcoa8888D0DTOinBindTest {

    @Test
    void bindGuidListAliases() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mkcoa8888D0DTOin a = om.readValue("{\"GUID_LIST\":[\"aaa\",\"bbb\"]}", mkcoa8888D0DTOin.class);
        assertEquals(2, a.getGuidList().size());

        mkcoa8888D0DTOin b = om.readValue("{\"guidList\":[\"ccc\"]}", mkcoa8888D0DTOin.class);
        assertEquals(1, b.getGuidList().size());
        assertEquals("ccc", b.getGuidList().get(0));
    }
}