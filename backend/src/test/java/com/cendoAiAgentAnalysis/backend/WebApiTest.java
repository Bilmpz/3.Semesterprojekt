package com.cendoAiAgentAnalysis.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.cendoAiAgentAnalysis.backend.analyse.config.TestData;
import com.cendoAiAgentAnalysis.backend.dataadgang.SamtaleRepository;
import com.cendoAiAgentAnalysis.backend.model.Samtale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class WebApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SamtaleRepository samtaleRepository;

    @MockitoBean
    private TestData testData;

    // #########################################################
    // ### Test af health-endpointet - Der er 4 tests af det ###

    // Kontrollerer, at health-endpointet returnerer HTTP 200 og status "ok" som JSON.
    @Test
    void healthEndpointReturnererOk() throws Exception {
        mockMvc.perform(get("/api/health")) //Sender en simuleret GET-request til /api/health
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // Kontrollerer den nuværende fejlhåndterings respons for en ukendt URL.
    @Test
    void ukendtEndpointReturnererInternalServerError() throws Exception {
        mockMvc.perform(get("/api/health/ukendt"))
                .andExpect(status().isInternalServerError());
    }

    // Kontrollerer den nuværende fejlhåndterings respons på en ikke-understøttet metode.
    @Test
    void healthEndpointPostReturnererInternalServerError() throws Exception {
        mockMvc.perform(post("/api/health"))
                .andExpect(status().isInternalServerError());
    }

    // Kontrollerer, at en ukendt query-parameter ikke ændrer health-svaret.
    @Test
    void healthEndpointIgnorererUkendtQueryParameter() throws Exception {
        mockMvc.perform(get("/api/health?test="))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("ok"));
    }


    // #########################################################
    // ### Test af kunder-endpointet - Der er 3 tests af det ###
    // Kontrollerer HTTP-status, JSON-listeformat og felter for en kunde.
    @Test
    void kunderEndpointReturnererOk() throws Exception {
        mockMvc.perform(get("/api/kunder"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isString())
                .andExpect(jsonPath("$[0].name").isString());
    }

    // Kontrollerer, at alle kunders id- og name-felter findes og er strenge.
    @Test
    void kunderEndpointReturnererForventedeFelter() throws Exception {
        mockMvc.perform(get("/api/kunder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").isArray())
                .andExpect(jsonPath("$[*].id", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$[*].name").isArray())
                .andExpect(jsonPath("$[*].name", everyItem(instanceOf(String.class))));
    }

    // Kontrollerer den nuværende fejlhåndtering for POST på et GET-endpoint.
    @Test
    void kunderEndpointAfviserPost() throws Exception {
        mockMvc.perform(post("/api/kunder"))
                .andExpect(status().isInternalServerError());
    }

    // ###############################################################
    // ### Test af virksomheder-endpointet - Der er 3 tests af det ###
    // Kontrollerer status, JSON-content-type og array-strukturen for virksomheder.
    @Test
    void virksomhederEndpointReturnererOk() throws Exception {
        mockMvc.perform(get("/api/virksomheder"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    // Kontrollerer, at hver virksomheds id og name er strenge.
    @Test
    void virksomhederEndpointReturnererForventedeFelter() throws Exception {
        mockMvc.perform(get("/api/virksomheder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").isArray())
                .andExpect(jsonPath("$[*].id", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$[*].name").isArray())
                .andExpect(jsonPath("$[*].name", everyItem(instanceOf(String.class))));
    }

    // Kontrollerer den nuværende fejlhåndtering for POST på et GET-endpoint.
    @Test
    void virksomhederEndpointAfviserPost() throws Exception {
        mockMvc.perform(post("/api/virksomheder"))
                .andExpect(status().isInternalServerError());
    }


    // #########################################################
    // ### Test af overblik-endpointet - Der er 8 tests af det ###
    // Kontrollerer status, JSON-format og dashboardets overordnede struktur.
    @Test
    void overblikEndpointReturnererOk() throws Exception {
        mockMvc.perform(get("/api/overblik"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.totalConversations").isNumber())
                .andExpect(jsonPath("$.causesFound").isNumber())
                .andExpect(jsonPath("$.lowData").isBoolean())
                .andExpect(jsonPath("$.topCauses").isArray())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    // Kontrollerer datatyper og felter for metrics, årsager og tidslinje.
    @Test
    void overblikEndpointReturnererForventedeFelter() throws Exception {
        mockMvc.perform(get("/api/overblik"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConversations").isNumber())
                .andExpect(jsonPath("$.causesFound").isNumber())
                .andExpect(jsonPath("$.lowData").isBoolean())
                .andExpect(jsonPath("$.topCauses").isArray())
                .andExpect(jsonPath("$.topCauses.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.topCauses[*].id", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$.topCauses[*].title", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$.topCauses[*].count", everyItem(instanceOf(Integer.class))))
                .andExpect(jsonPath("$.timeline").isArray())
                .andExpect(jsonPath("$.timeline.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.timeline[*].date", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$.timeline[*].conversations", everyItem(instanceOf(Integer.class))));
    }

    // Kontrollerer, at de seedede virksomheder kun får deres egne samtaler med i dashboardet.
    // Kontrollerer at companyId filtrerer dashboard-data korrekt.
    // Kontrollerer, at virksomhedsfilteret adskiller data mellem virksomheder.
    @Test
    void overblikEndpointFiltrererMedCompanyId() throws Exception {

        var virksomheder = objectMapper.readTree(
                mockMvc.perform(get("/api/virksomheder"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        var virksomhed1 = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("companyId", "virksomhed-1"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var virksomhed2 = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("companyId", "virksomhed-2"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(virksomheder.findValuesAsText("id")).contains("virksomhed-1", "virksomhed-2");
        assertThat(virksomhed1.path("totalConversations").asInt()).isGreaterThan(0);
        assertThat(virksomhed2.path("totalConversations").asInt()).isZero();
        assertThat(virksomhed2.path("topCauses")).isEmpty();
        assertThat(virksomhed2.path("timeline")).isEmpty();

        // Ukendt virksomheds-ID skal give tomme resultater.
        mockMvc.perform(get("/api/overblik")
                        .param("companyId", "ukendt-test-virksomhed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConversations").value(0))
                .andExpect(jsonPath("$.causesFound").value(0))
                .andExpect(jsonPath("$.topCauses").isEmpty())
                .andExpect(jsonPath("$.timeline").isEmpty());
    }

    // Kontrollerer filtreringen med testoprettede samtaler i og uden for et-dags perioden.
    @Test
    void overblikEndpointFiltrererMedDays() throws Exception {
        String recentId = "overblik-days-recent-" + UUID.randomUUID();
        String olderId = "overblik-days-older-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        int recentBaseline = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("days", "1"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .path("totalConversations").asInt();
        int thirtyDayBaseline = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("days", "30"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .path("totalConversations").asInt();

        assertThat(samtaleRepository.existsById(recentId)).isFalse();
        assertThat(samtaleRepository.existsById(olderId)).isFalse();

        try {
            samtaleRepository.save(new Samtale(
                    recentId, "Testårsag", "Ny test-samtale", now.minusMinutes(5)));
            samtaleRepository.save(new Samtale(
                    olderId, "Testårsag", "Ældre test-samtale", now.minusDays(2)));

            mockMvc.perform(get("/api/overblik").param("days", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.totalConversations").value(recentBaseline + 1));

            mockMvc.perform(get("/api/overblik").param("days", "30"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.totalConversations").value(thirtyDayBaseline + 2));
        } finally {
            samtaleRepository.deleteById(recentId);
            samtaleRepository.deleteById(olderId);
        }
    }

    // Kontrollerer, at søgning filtrerer metrics, topårsager og tidslinje.
    @Test
    void overblikEndpointFiltrererMedSearch() throws Exception {
        int udenSearch = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("totalConversations")
                .asInt();

        var filtreret = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("search", "gArAnTi"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int filtreretAntal = filtreret.path("totalConversations").asInt();
        assertThat(filtreretAntal).isGreaterThan(0).isLessThan(udenSearch);
        assertThat(filtreret.path("topCauses").isArray()).isTrue();
        assertThat(filtreret.path("topCauses").isEmpty()).isFalse();
        for (var cause : filtreret.path("topCauses")) {
            assertThat(cause.path("title").asText()).containsIgnoringCase("garanti");
        }
        assertThat(filtreret.path("timeline").isArray()).isTrue();
        int timelineAntal = 0;
        for (var dag : filtreret.path("timeline")) {
            timelineAntal += dag.path("conversations").asInt();
        }
        assertThat(timelineAntal).isEqualTo(filtreretAntal);
    }

    // Kontrollerer, at en tom søgning giver samme dashboarddata som ingen search-parameter.
    @Test
    void overblikEndpointHaandtererTomSearch() throws Exception {
        var udenSearch = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var tomSearch = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("search", ""))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertThat(tomSearch).isEqualTo(udenSearch);
    }

    // Kontrollerer minimum, ugyldig og stor gyldig days-værdi.
    @Test
    void overblikEndpointHaandtererDaysGraensevaerdier() throws Exception {
        mockMvc.perform(get("/api/overblik").param("days", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/overblik").param("days", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/overblik").param("days", "36500"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/overblik").param("days", "abc"))
                .andExpect(status().isBadRequest());
    }

    // Kontrollerer den nuværende fejlhåndtering for POST på et GET-endpoint.
    @Test
    void overblikEndpointAfviserPost() throws Exception {
        mockMvc.perform(post("/api/overblik"))
                .andExpect(status().isInternalServerError());
    }

    // Kontrollerer JSON-strukturen og datatyperne for cluster-listen.
    @Test
    void clustersEndpointReturnererOk() throws Exception {
        mockMvc.perform(get("/api/clusters").param("days", "36500"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[*].id", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$[*].title", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$[*].description", everyItem(instanceOf(String.class))))
                .andExpect(jsonPath("$[*].count", everyItem(instanceOf(Number.class))))
                .andExpect(jsonPath("$[*].share", everyItem(instanceOf(Number.class))))
                .andExpect(jsonPath("$[*].trend", everyItem(instanceOf(String.class))));
    }

    // Sammenholder de seedede demo-clusters og deres andel med data fra API'et.
    @Test
    void clustersEndpointReturnererForventedeFelterOgData() throws Exception {
        var clusters = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters").param("days", "36500"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        long totalConversations = objectMapper.readTree(
                mockMvc.perform(get("/api/overblik").param("days", "36500"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .path("totalConversations").asLong();

        assertThat(totalConversations).isPositive();
        String[] demoTitles = {
                "Demo: Status på reparation",
                "Demo: Garanti",
                "Demo: Faktura",
                "Demo: Ombooking",
                "Demo: Lånebil"
        };
        long[] expectedCounts = {5, 4, 3, 2, 1};

        for (int i = 0; i < demoTitles.length; i++) {
            com.fasterxml.jackson.databind.JsonNode demoCluster = null;
            for (var cluster : clusters) {
                if (demoTitles[i].equals(cluster.path("title").asText())) {
                    demoCluster = cluster;
                    break;
                }
            }

            assertThat(demoCluster)
                    .as("demo-clusteret %s findes", demoTitles[i])
                    .isNotNull();
            assertThat(demoCluster.path("count").asLong()).isEqualTo(expectedCounts[i]);
            assertThat(demoCluster.path("share").asLong())
                    .isEqualTo(Math.round(expectedCounts[i] * 100.0 / totalConversations));
            assertThat(demoCluster.path("trend").asText()).isIn("up", "down", "flat");
        }
    }

    // Kontrollerer at companyId afgrænser til virksomhedens demo-samtaler.
    @Test
    void clustersEndpointFiltrererMedCompanyId() throws Exception {
        var virksomhed1 = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters")
                                .param("companyId", "virksomhed-1")
                                .param("days", "36500"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        var virksomhed2 = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters")
                                .param("companyId", "virksomhed-2")
                                .param("days", "36500"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(virksomhed1.findValuesAsText("title"))
                .contains("Demo: Garanti", "Demo: Status på reparation");
        assertThat(virksomhed2).isEmpty();
    }

    // Kontrollerer case-insensitive søgning, tom søgning og intet match.
    @Test
    void clustersEndpointFiltrererMedSearch() throws Exception {
        var alleClusters = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters").param("days", "36500"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        var tomSearch = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters")
                                .param("days", "36500")
                                .param("search", ""))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        var soegeresultat = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters")
                                .param("days", "36500")
                                .param("search", "gArAnTi"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        var intetMatch = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters")
                                .param("days", "36500")
                                .param("search", "findes-ikke-123"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(tomSearch).isEqualTo(alleClusters);
        assertThat(soegeresultat.findValuesAsText("title"))
                .contains("Demo: Garanti");
        for (var cluster : soegeresultat) {
            assertThat(cluster.path("title").asText()).containsIgnoringCase("garanti");
        }
        assertThat(intetMatch).isEmpty();
    }

    // Kontrollerer validering af days og en stor, gyldig periode.
    @Test
    void clustersEndpointHaandtererUgyldigeOgGraensevaerdier() throws Exception {
        mockMvc.perform(get("/api/clusters").param("days", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/clusters").param("days", "36500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/clusters").param("days", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fejl").value("days skal være mindst 1"));

        mockMvc.perform(get("/api/clusters").param("days", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fejl").value("Ugyldig værdi for days"));
    }

    // Kontrollerer demo-samtaler, JSON-felter, relation og nyeste-først sortering.
    @Test
    void clusterSamtalerEndpointReturnererForventedeSamtaler() throws Exception {
        String clusterId = findClusterId("Demo: Status på reparation");
        var samtaler = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters/{id}/samtaler", clusterId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith("application/json"))
                        .andExpect(jsonPath("$").isArray())
                        .andReturn().getResponse().getContentAsString());

        assertThat(samtaler.size()).isEqualTo(5);
        Set<String> samtaleIds = new HashSet<>();
        LocalDateTime forrigeTidspunkt = null;
        for (JsonNode samtale : samtaler) {
            assertThat(samtale.has("id")).isTrue();
            assertThat(samtale.path("id").isTextual()).isTrue();
            assertThat(samtale.has("timestamp")).isTrue();
            assertThat(samtale.path("timestamp").isTextual()).isTrue();
            assertThat(samtale.has("company")).isTrue();
            assertThat(samtale.path("company").isTextual()).isTrue();
            assertThat(samtale.has("transcript")).isTrue();
            assertThat(samtale.has("reason")).isTrue();
            assertThat(samtale.path("reason").asText())
                    .isEqualTo("Demo: Status på reparation");
            assertThat(samtale.has("explanation")).isTrue();
            assertThat(samtale.path("explanation").asText())
                    .isEqualTo("Demo-samtale til test af overblikket");

            samtaleIds.add(samtale.path("id").asText());
            LocalDateTime tidspunkt = LocalDateTime.parse(samtale.path("timestamp").asText());
            if (forrigeTidspunkt != null) {
                assertThat(tidspunkt).isBeforeOrEqualTo(forrigeTidspunkt);
            }
            forrigeTidspunkt = tidspunkt;
        }

        assertThat(samtaleIds).hasSize(5).containsExactlyInAnyOrder(
                "demo-overblik-1-1",
                "demo-overblik-1-2",
                "demo-overblik-1-3",
                "demo-overblik-1-4",
                "demo-overblik-1-5");
    }

    // Ukendte ID'er bliver håndteret som 404; controlleren validerer ikke ID-formatet.
    @Test
    void clusterSamtalerEndpointHaandtererUkendtId() throws Exception {
        mockMvc.perform(get("/api/clusters/{id}/samtaler", "ukendt-cluster"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fejl")
                        .value("Clusteret med id ukendt-cluster findes ikke"));

        mockMvc.perform(get("/api/clusters/{id}/samtaler",
                        "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fejl")
                        .value("Clusteret med id 00000000-0000-0000-0000-000000000000 findes ikke"));
    }

    // Kontrollerer projektets nuværende fejlhåndtering for en ikke-understøttet metode.
    @Test
    void clusterSamtalerEndpointAfviserPost() throws Exception {
        mockMvc.perform(post("/api/clusters/ukendt-cluster/samtaler"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.fejl").value("Der skete en uventet fejl"));
    }

    // Kontrollerer API-felterne og værdierne for en eksisterende demo-samtale.
    @Test
    void samtaleEndpointReturnererForventetSamtale() throws Exception {
        String samtaleId = "api-anonymizer-test-" + UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        String transcript = "Mit navn er Peter Hansen, e-mail peter.hansen@example.com, "
                + "telefon 12345678. Jeg ønsker hjælp med bilen.";
        String anonymizedTranscript = "Mit navn er [NAVN], e-mail [EMAIL], "
                + "telefon [TLF]. Jeg ønsker hjælp med bilen.";

        assertThat(samtaleRepository.existsById(samtaleId)).isFalse();
        Samtale testSamtale = new Samtale(
                samtaleId, "Reparation", "Kunden spørger om bilen.", timestamp);
        testSamtale.setTranscript(transcript);

        try {
            samtaleRepository.save(testSamtale);

            String response = mockMvc.perform(get("/api/samtaler/{id}", samtaleId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.id").value(samtaleId))
                    .andExpect(jsonPath("$.timestamp").value(timestamp.toString()))
                    .andExpect(jsonPath("$.company").value(""))
                    .andExpect(jsonPath("$.reason").value("Reparation"))
                    .andExpect(jsonPath("$.explanation").value("Kunden spørger om bilen."))
                    .andExpect(jsonPath("$.transcript").value(anonymizedTranscript))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode json = objectMapper.readTree(response);
            assertThat(json.size()).isEqualTo(6);
            assertFalse(json.path("transcript").asText().contains("Peter Hansen"));
            assertFalse(json.path("transcript").asText().contains("peter.hansen@example.com"));
            assertFalse(json.path("transcript").asText().contains("12345678"));
            assertThat(json.path("transcript").asText()).contains("Jeg ønsker hjælp med bilen.");
        } finally {
            samtaleRepository.deleteById(samtaleId);
        }
    }

    // Endpointet returnerer 404 for et ukendt ID; ID-formatet valideres ikke separat.
    @Test
    void samtaleEndpointHaandtererUkendtId() throws Exception {
        mockMvc.perform(get("/api/samtaler/{id}", "ukendt-samtale"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fejl")
                        .value("Samtalen med id ukendt-samtale findes ikke"));

        mockMvc.perform(get("/api/samtaler/{id}",
                        "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fejl")
                        .value("Samtalen med id 00000000-0000-0000-0000-000000000000 findes ikke"));
    }

    // Kontrollerer den nuværende fælles fejlhåndtering for en ikke-understøttet HTTP-metode.
    @Test
    void samtaleEndpointAfviserPost() throws Exception {
        mockMvc.perform(post("/api/samtaler/{id}", "demo-overblik-1-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.fejl").value("Der skete en uventet fejl"));
    }

    private String findClusterId(String title) throws Exception {
        var clusters = objectMapper.readTree(
                mockMvc.perform(get("/api/clusters").param("days", "36500"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        for (JsonNode cluster : clusters) {
            if (title.equals(cluster.path("title").asText())) {
                return cluster.path("id").asText();
            }
        }
        throw new AssertionError("Fandt ikke demo-clusteret: " + title);
    }
}
