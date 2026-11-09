package com.cendoAiAgentAnalysis.backend;

import com.cendoAiAgentAnalysis.backend.web.AdgangsFilter;
import com.cendoAiAgentAnalysis.backend.web.HealthController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({AdgangsFilter.class, AdgangsFilterTest.TestController.class})
@TestPropertySource(properties = {
        "cendo.security.username=testbruger",
        "cendo.security.password=testkodeord"
})
class AdgangsFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthSkalVaereOffentlig() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void apiUdenLoginSkalGive401() throws Exception {
        mockMvc.perform(get("/api/kunder"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forkertLoginSkalGive401() throws Exception {
        mockMvc.perform(get("/api/kunder")
                        .with(httpBasic("testbruger", "forkertkodeord")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void korrektLoginSkalGiveAdgang() throws Exception {
        mockMvc.perform(get("/api/health")
                        .with(httpBasic("testbruger", "testkodeord")))
                .andExpect(status().isOk());
    }

    @Test
    void korrektLoginSkalGiveAdgangTilBeskyttetApi() throws Exception {
        mockMvc.perform(get("/api/test-beskyttet")
                        .with(httpBasic("testbruger", "testkodeord")))
                .andExpect(status().isOk());
    }


    @RestController //Klassen håndterer HTTP-requests
    static class TestController {

        @GetMapping("/api/test-beskyttet") //bestemmer hvor requesten skal hen
        public String test() {
            return "OK";
        }
    }

}


