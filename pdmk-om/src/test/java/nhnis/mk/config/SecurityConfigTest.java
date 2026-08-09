package nhnis.mk.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String EMPTY_REQUEST = "{\"dto\":{}}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mkcoa7777S0IsPublic() throws Exception {
        mockMvc.perform(post("/mkcoa7777S0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void mkcoa5530S0IsPublic() throws Exception {
        mockMvc.perform(post("/mkcoa5530S0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void mkcoa9999S0IsPublic() throws Exception {
        mockMvc.perform(post("/mkcoa9999S0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }
}
