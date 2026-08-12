package nhnis.mg.co.a.config;

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
    void mgcoa8888S0IsPublic() throws Exception {
        mockMvc.perform(post("/mgcoa8888S0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void mgcoa5530S0IsPublic() throws Exception {
        mockMvc.perform(post("/mgcoa5530S0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void mgcoa9999S0IsPublic() throws Exception {
        mockMvc.perform(post("/mgcoa9999S0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }
}
