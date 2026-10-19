package com.wealthos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wealthos.data.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End to end through the HTTP layer: JSON shapes, translated errors, export/import, debug.
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    DataService data;
    @Autowired
    ObjectMapper json;

    @BeforeEach
    void clean() {
        data.deleteAll();
    }

    @Test
    void appInfoAndTranslations() throws Exception {
        mvc.perform(get("/api/app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languages", hasSize(2)))
                .andExpect(jsonPath("$.priceSource.name").value("none"))
                .andExpect(jsonPath("$.debugEnabled").value(true));
        mvc.perform(get("/api/i18n/es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['nav.investments']").value("Inversiones"));
        mvc.perform(get("/api/i18n/fr")).andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsComeBackTranslatedWithFieldDetails() throws Exception {
        String body = "{\"name\":\"\",\"type\":\"BANK\",\"currency\":\"EUR\",\"balance\":0}";
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation"))
                .andExpect(jsonPath("$.fields[0].field").value("name"))
                .andExpect(jsonPath("$.fields[0].message").value("The account needs a name"));
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Accept-Language", "es-ES"))
                .andExpect(jsonPath("$.message").value("Hay campos que corregir"))
                .andExpect(jsonPath("$.fields[0].message").value("La cuenta necesita un nombre"));
    }

    @Test
    void businessErrorsCarryTheRequestId() throws Exception {
        mvc.perform(post("/api/assets/999/sell").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ALL\"}").header("X-Request-Id", "abc123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "abc123"))
                .andExpect(jsonPath("$.code").value("error.positionNotFound"))
                .andExpect(jsonPath("$.requestId").value("abc123"));
    }

    @Test
    void createAccountAndPositionThenSeeThemInTheOverview() throws Exception {
        String account = mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Broker\",\"type\":\"BROKERAGE\",\"currency\":\"EUR\",\"balance\":500}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long accountId = json.readTree(account).get("id").asLong();

        String buy = """
                {"accountId": %d, "ticker": "acme", "assetType": "STOCK", "amountMode": "MONEY",
                 "amount": 1000, "price": 20, "autoPrice": true}
                """.formatted(accountId);
        mvc.perform(post("/api/assets/buy").contentType(MediaType.APPLICATION_JSON).content(buy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.asset.ticker").value("ACME"))
                .andExpect(jsonPath("$.asset.quantity").value(50))
                .andExpect(jsonPath("$.merged").value(false))
                .andExpect(jsonPath("$.warnings[0]").value("warning.noPriceSource"));
        // the same purchase again goes into the same asset
        mvc.perform(post("/api/assets/buy").contentType(MediaType.APPLICATION_JSON).content(buy))
                .andExpect(jsonPath("$.merged").value(true))
                .andExpect(jsonPath("$.asset.quantity").value(100));

        mvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.netWorth").value(2500.0))
                .andExpect(jsonPath("$.summary.totalCash").value(500.0))
                .andExpect(jsonPath("$.byAccount[0].label").value("Broker"))
                .andExpect(jsonPath("$.assets", hasSize(1)))
                .andExpect(jsonPath("$.assets[0].operationsCount").value(2))
                .andExpect(jsonPath("$.assets[0].autoPrice").value(true));
    }

    @Test
    void sampleDataFeedsEveryPage() throws Exception {
        mvc.perform(post("/api/data/sample")).andExpect(status().isNoContent());
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                // tax widgets are hidden until the user turns them on
                .andExpect(jsonPath("$.showTax").value(false))
                .andExpect(jsonPath("$.deadlines", hasSize(0)));
        mvc.perform(get("/api/networth").param("range", "1Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("week"))
                .andExpect(jsonPath("$.estimated").value(true))
                .andExpect(jsonPath("$.points[-1:].value", hasItem(94635.67)));
        mvc.perform(get("/api/networth").param("range", "CUSTOM").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/portfolio")).andExpect(jsonPath("$.assets", hasSize(11)));
        mvc.perform(get("/api/tax").param("year", "2026").param("quarter", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.ivaToPay", notNullValue()));
    }

    @Test
    void showingTaxOnTheDashboard() throws Exception {
        mvc.perform(put("/api/tax/settings/show-on-dashboard").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"show\":true}"))
                .andExpect(jsonPath("$.showOnDashboard").value(true));
        mvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.showTax").value(true))
                .andExpect(jsonPath("$.deadlines", hasSize(3)));
    }

    @Test
    void exportThenImportGivesBackTheSameInvestments() throws Exception {
        mvc.perform(post("/api/data/sample"));
        String exported = mvc.perform(get("/api/data/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("wealthos-investments-")))
                .andReturn().getResponse().getContentAsString();
        JsonNode file = json.readTree(exported);
        assertThat(file.get("format").asText()).isEqualTo("wealthos-investments");
        assertThat(file.get("version").asInt()).isEqualTo(2);
        assertThat(file.get("assets")).hasSize(11);
        String before = mvc.perform(get("/api/portfolio")).andReturn().getResponse().getContentAsString();

        mvc.perform(delete("/api/data")).andExpect(status().isNoContent());
        mvc.perform(post("/api/data/import").contentType(MediaType.APPLICATION_JSON).content(exported))
                .andExpect(status().isNoContent());

        JsonNode after = json.readTree(mvc.perform(get("/api/portfolio")).andReturn().getResponse().getContentAsString());
        assertThat(after.get("summary").get("netWorth")).isEqualTo(json.readTree(before).get("summary").get("netWorth"));
        assertThat(after.get("assets")).hasSize(11);
    }

    @Test
    void importRejectsOtherFiles() throws Exception {
        mvc.perform(post("/api/data/import").contentType(MediaType.APPLICATION_JSON).content("{\"format\":\"something\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("error.importFormat"));
    }

    @Test
    void debugEndpoints() throws Exception {
        mvc.perform(get("/api/portfolio").header("X-Request-Id", "trace01"));
        mvc.perform(get("/api/debug/info")).andExpect(status().isOk()).andExpect(jsonPath("$.priceSource.name").value("none"));
        mvc.perform(get("/api/debug/requests")).andExpect(jsonPath("$[0].id", notNullValue()));
        mvc.perform(get("/api/debug/loggers")).andExpect(jsonPath("$[0].name").value("ROOT"));
        mvc.perform(put("/api/debug/loggers/com.wealthos").contentType(MediaType.APPLICATION_JSON).content("{\"level\":\"DEBUG\"}"))
                .andExpect(jsonPath("$.level").value("DEBUG"));
        mvc.perform(get("/api/debug/logs").param("level", "INFO")).andExpect(status().isOk());
        mvc.perform(post("/api/debug/prices/refresh")).andExpect(jsonPath("$.provider").value("none"));
        mvc.perform(put("/api/debug/loggers/com.wealthos").contentType(MediaType.APPLICATION_JSON).content("{\"level\":\"WARN\"}"));
    }
}
