package basic.sprinng.pulsepoint.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserSecurityAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private basic.sprinng.pulsepoint.pulsepoint.PulsePointRpcFilter pulsePointRpcFilter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(pulsePointRpcFilter)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "demo", roles = { "USER" })
    void accessUsersPage_AsUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void accessUsersPage_AsAdmin_ShouldBeOk() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "demo", roles = { "USER" })
    void rpcListUsers_AsUser_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(post("/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("X-PP-RPC", "true")
                .header("X-PP-Function", "listUsers"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Permission denied"));
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void rpcListUsers_AsAdmin_ShouldReturn200Success() throws Exception {
        mockMvc.perform(post("/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("X-PP-RPC", "true")
                .header("X-PP-Function", "listUsers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("demo"));
    }
}
