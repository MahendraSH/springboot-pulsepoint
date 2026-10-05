package basic.sprinng.pulsepoint.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ComponentDocsIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void testComponentsIndexPage() throws Exception {
        mockMvc.perform(get("/components"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "button", "input", "select", "checkbox", "switch", "accordion",
        "card", "table", "tabs", "separator", "badge", "avatar",
        "progress", "spinner", "skeleton", "toast", "dialog", "tooltip",
        "typography", "kbd", "label", "alert"
    })
    void testAllComponentPages(String componentName) throws Exception {
        mockMvc.perform(get("/components/" + componentName))
                .andExpect(status().isOk());
    }
}
