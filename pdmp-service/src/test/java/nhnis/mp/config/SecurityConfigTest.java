package nhnis.mp.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    private static final String EMPTY_REQUEST = "{\"body\":{}}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mpcoa8888RequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/mp/co/a/8888/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCallMpcoa8888() throws Exception {
        mockMvc.perform(post("/api/mp/co/a/8888/list")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void existingMpcoa9999RouteKeepsCurrentPublicAccess() throws Exception {
        mockMvc.perform(post("/api/mp/co/a/9999/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST))
                .andExpect(status().isOk());
    }
}
