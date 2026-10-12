package com.freightfox.transporter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void apiDocs_describeBothEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Transporter Assignment on Lanes API"))
                .andExpect(jsonPath("$.paths['/api/v1/transporters/input']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transporters/assignment']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transporters/input'].post.requestBody.content"
                        + "['application/json'].examples['Test Case 1']").exists())
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.servers[1].url")
                        .value("https://freightfox-transporter-assignment.onrender.com"))
                .andExpect(jsonPath("$.info.contact.email").value("yadavchandan6103@gmail.com"));
    }

    @Test
    void swaggerUi_isServed() throws Exception {
        int code = mvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertThat(code).satisfiesAnyOf(
                c -> assertThat(c).isEqualTo(200),
                c -> assertThat(c).isBetween(300, 399));
    }
}
