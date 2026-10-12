package com.freightfox.transporter.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ValidationIntegrationTest {

    private static final String LANES = "[{\"id\":1,\"origin\":\"Mumbai\",\"destination\":\"Delhi\"}]";

    @Autowired
    private MockMvc mvc;

    private ResultActions postInput(String json) throws Exception {
        return mvc.perform(post("/api/v1/transporters/input").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions postAssignment(String json) throws Exception {
        return mvc.perform(post("/api/v1/transporters/assignment").contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String withQuote(String quote) {
        return "{\"lanes\":" + LANES + ",\"transporters\":[{\"id\":1,\"name\":\"T1\",\"laneQuotes\":"
                + "[{\"laneId\":1,\"quote\":" + quote + "}]}]}";
    }

    private static ResultActions expectError(ResultActions r, String message) throws Exception {
        return r.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void emptyLanes_returns400() throws Exception {
        expectError(postInput("{\"lanes\":[],\"transporters\":[{\"id\":1,\"name\":\"T1\",\"laneQuotes\":[]}]}"),
                "Validation failed")
                .andExpect(jsonPath("$.errors", hasItem(startsWith("lanes:"))));
    }

    @Test
    void emptyTransporters_returns400() throws Exception {
        expectError(postInput("{\"lanes\":" + LANES + ",\"transporters\":[]}"), "Validation failed")
                .andExpect(jsonPath("$.errors", hasItem(startsWith("transporters:"))));
    }

    @Test
    void blankOrigin_returns400_withFieldPath() throws Exception {
        expectError(postInput("{\"lanes\":[{\"id\":1,\"origin\":\" \",\"destination\":\"Delhi\"}],"
                        + "\"transporters\":[{\"id\":1,\"name\":\"T1\",\"laneQuotes\":[]}]}"),
                "Validation failed")
                .andExpect(jsonPath("$.errors", hasItem("lanes[0].origin: must not be blank")));
    }

    @Test
    void zeroQuote_returns400() throws Exception {
        expectError(postInput(withQuote("0")), "Validation failed")
                .andExpect(jsonPath("$.errors", hasItem(startsWith("transporters[0].laneQuotes[0].quote:"))));
    }

    @Test
    void negativeQuote_returns400() throws Exception {
        expectError(postInput(withQuote("-5")), "Validation failed");
    }

    @Test
    void fractionalQuote_returns400_malformed() throws Exception {
        expectError(postInput(withQuote("5000.5")), "Malformed JSON request")
                .andExpect(jsonPath("$.errors[0]").value("transporters[0].laneQuotes[0].quote: expected an integer"));
    }

    @Test
    void typeMismatch_returns400_withoutInternalDetails() throws Exception {
        expectError(postInput(withQuote("\"abc\"")), "Malformed JSON request")
                .andExpect(jsonPath("$.errors[0]").value("transporters[0].laneQuotes[0].quote: expected an integer"))
                .andExpect(jsonPath("$.errors[0]").value(not(containsString("com.freightfox"))));
    }

    @Test
    void wrongStructure_returns400_withFieldPath() throws Exception {
        expectError(postInput("{\"lanes\":{\"id\":1},\"transporters\":[]}"), "Malformed JSON request")
                .andExpect(jsonPath("$.errors[0]").value("lanes: expected an array"));
    }

    @Test
    void duplicateLaneIds_returns400() throws Exception {
        expectError(postInput("{\"lanes\":[{\"id\":1,\"origin\":\"A\",\"destination\":\"B\"},"
                        + "{\"id\":1,\"origin\":\"C\",\"destination\":\"D\"}],"
                        + "\"transporters\":[{\"id\":1,\"name\":\"T1\",\"laneQuotes\":[]}]}"),
                "Invalid input data")
                .andExpect(jsonPath("$.errors", hasItem("Duplicate lane id: 1")));
    }

    @Test
    void unknownLaneId_returns400() throws Exception {
        expectError(postInput("{\"lanes\":" + LANES + ",\"transporters\":[{\"id\":1,\"name\":\"T1\","
                        + "\"laneQuotes\":[{\"laneId\":7,\"quote\":10}]}]}"),
                "Invalid input data")
                .andExpect(jsonPath("$.errors", hasItem("Transporter 1 references unknown lane id 7")));
    }

    @Test
    void duplicateLaneQuote_returns400() throws Exception {
        expectError(postInput("{\"lanes\":" + LANES + ",\"transporters\":[{\"id\":1,\"name\":\"T1\","
                        + "\"laneQuotes\":[{\"laneId\":1,\"quote\":10},{\"laneId\":1,\"quote\":12}]}]}"),
                "Invalid input data")
                .andExpect(jsonPath("$.errors", hasItem("Transporter 1 has duplicate quotes for lane id 1")));
    }

    @Test
    void maxTransportersZero_returns400() throws Exception {
        expectError(postAssignment("{\"maxTransporters\":0}"), "Validation failed")
                .andExpect(jsonPath("$.errors", hasItem(startsWith("maxTransporters:"))));
    }

    @Test
    void maxTransportersMissing_returns400() throws Exception {
        expectError(postAssignment("{}"), "Validation failed")
                .andExpect(jsonPath("$.errors", hasItem(startsWith("maxTransporters:"))));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        expectError(postInput("{\"lanes\": [ oops"), "Malformed JSON request")
                .andExpect(jsonPath("$.errors[0]").value(startsWith("Invalid JSON syntax at line 1")))
                .andExpect(jsonPath("$.errors[0]").value(not(containsString("jackson"))));
    }

    @Test
    void emptyBody_returns400() throws Exception {
        expectError(postInput(""), "Malformed JSON request")
                .andExpect(jsonPath("$.errors[0]").value("Request body is missing or unreadable"));
    }

    @Test
    void wrongContentType_returns415() throws Exception {
        mvc.perform(post("/api/v1/transporters/input").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getOnAssignment_returns405() throws Exception {
        mvc.perform(get("/api/v1/transporters/assignment"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
