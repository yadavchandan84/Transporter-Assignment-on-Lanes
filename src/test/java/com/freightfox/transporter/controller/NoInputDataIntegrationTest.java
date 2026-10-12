package com.freightfox.transporter.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freightfox.transporter.repository.LaneQuoteRepository;
import com.freightfox.transporter.repository.LaneRepository;
import com.freightfox.transporter.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NoInputDataIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private LaneQuoteRepository laneQuoteRepository;
    @Autowired
    private TransporterRepository transporterRepository;
    @Autowired
    private LaneRepository laneRepository;

    @BeforeEach
    void clearData() {
        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();
    }

    @Test
    void assignment_withoutData_returns404() throws Exception {
        mvc.perform(post("/api/v1/transporters/assignment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxTransporters\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(
                        "No input data found. Submit data via POST /api/v1/transporters/input first."))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getInput_withoutData_returns404() throws Exception {
        mvc.perform(get("/api/v1/transporters/input"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
