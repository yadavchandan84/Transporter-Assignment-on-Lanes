package com.freightfox.transporter.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freightfox.transporter.config.ApiExamples;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class TransporterControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postInput(String json) throws Exception {
        return mvc.perform(post("/api/v1/transporters/input").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions assign(int max) throws Exception {
        return mvc.perform(post("/api/v1/transporters/assignment").contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxTransporters\":" + max + "}"));
    }

    @Test
    void postInput_returnsSuccessBody() throws Exception {
        postInput(ApiExamples.TEST_CASE_1_INPUT)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\",\"message\":\"Input data saved successfully.\"}", true));
    }

    @Test
    void getInput_returnsStoredData_sorted() throws Exception {
        String unsorted = """
                {"lanes":[{"id":2,"origin":"B","destination":"C"},{"id":1,"origin":"A","destination":"B"}],
                 "transporters":[{"id":9,"name":"Nine","laneQuotes":[{"laneId":2,"quote":5},{"laneId":1,"quote":7}]},
                                 {"id":3,"name":"Three","laneQuotes":[]}]}
                """;
        postInput(unsorted).andExpect(status().isOk());
        mvc.perform(get("/api/v1/transporters/input"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.lanes[*].id", contains(1, 2)))
                .andExpect(jsonPath("$.transporters[*].id", contains(3, 9)))
                .andExpect(jsonPath("$.transporters[1].laneQuotes[*].laneId", contains(1, 2)))
                .andExpect(jsonPath("$.transporters[1].laneQuotes[*].quote", contains(7, 5)));
    }

    @Test
    void postInput_replacesPreviousDataset() throws Exception {
        postInput(ApiExamples.TEST_CASE_1_INPUT).andExpect(status().isOk());
        postInput(ApiExamples.TEST_CASE_2_INPUT).andExpect(status().isOk());
        mvc.perform(get("/api/v1/transporters/input"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lanes", hasSize(5)))
                .andExpect(jsonPath("$.lanes[0].origin").value("Chandigarh"))
                .andExpect(jsonPath("$.transporters", hasSize(4)))
                .andExpect(jsonPath("$.transporters[0].name").value("Transporter X"));
    }

    @Test
    void assignment_testCase2_max3() throws Exception {
        postInput(ApiExamples.TEST_CASE_2_INPUT).andExpect(status().isOk());
        assign(3).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status":"success","totalCost":7500,
                         "assignments":[{"laneId":1,"transporterId":1},{"laneId":2,"transporterId":1},
                                        {"laneId":3,"transporterId":2},{"laneId":4,"transporterId":2},
                                        {"laneId":5,"transporterId":3}],
                         "selectedTransporters":[1,2,3],"optimal":true}
                        """, true));
    }

    @Test
    void assignment_testCase2_max4() throws Exception {
        postInput(ApiExamples.TEST_CASE_2_INPUT).andExpect(status().isOk());
        assign(4).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(7450))
                .andExpect(jsonPath("$.assignments[3].laneId").value(4))
                .andExpect(jsonPath("$.assignments[3].transporterId").value(4))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 2, 3, 4)));
    }

    @Test
    void assignment_testCase2_max2_returns422() throws Exception {
        postInput(ApiExamples.TEST_CASE_2_INPUT).andExpect(status().isOk());
        assign(2).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Cannot cover all 5 lanes with at most 2 transporter(s)."))
                .andExpect(jsonPath("$.errors", hasItem(startsWith("At least 3 transporters"))));
    }

    @Test
    void assignment_testCase1_max3() throws Exception {
        postInput(ApiExamples.TEST_CASE_1_INPUT).andExpect(status().isOk());
        assign(3).andExpect(status().isOk())
                .andExpect(content().json(ApiExamples.ASSIGNMENT_SUCCESS_TC1, true));
    }

    @Test
    void assignment_mainExample_max3_returns422_min5() throws Exception {
        postInput(ApiExamples.MAIN_EXAMPLE_INPUT).andExpect(status().isOk());
        assign(3).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors[0]").value(
                        "At least 5 transporters are required to cover all lanes, but maxTransporters is 3."));
    }

    @Test
    void assignment_mainExample_max5_returns48000() throws Exception {
        postInput(ApiExamples.MAIN_EXAMPLE_INPUT).andExpect(status().isOk());
        assign(5).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(48000))
                .andExpect(jsonPath("$.assignments", hasSize(10)))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$.optimal").value(true));
    }

    @Test
    void assignment_laneWithoutQuotes_returns422_listsLane() throws Exception {
        postInput("""
                {"lanes":[{"id":1,"origin":"Mumbai","destination":"Delhi"},{"id":2,"origin":"Pune","destination":"Goa"}],
                 "transporters":[{"id":1,"name":"T1","laneQuotes":[{"laneId":1,"quote":100}]}]}
                """).andExpect(status().isOk());
        assign(2).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors", contains("Lane 2 (Pune -> Goa) has no transporter quotes.")));
    }

    @Test
    void root_redirectsToSwaggerUi() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/swagger-ui.html")));
    }

    @Test
    void actuatorHealth_isUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
