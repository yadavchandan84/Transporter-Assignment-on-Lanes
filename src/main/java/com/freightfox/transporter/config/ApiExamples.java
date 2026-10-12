package com.freightfox.transporter.config;

/** Example payloads shown in Swagger UI. Test Case 1 is the default so "Try it out" works immediately. */
public final class ApiExamples {

    private ApiExamples() {
    }

    public static final String TEST_CASE_1_INPUT = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 20835}, {"laneId": 2, "quote": 10512}, {"laneId": 3, "quote": 22105}, {"laneId": 4, "quote": 42481}, {"laneId": 5, "quote": 19862}]},
                {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 1, "quote": 48844}, {"laneId": 2, "quote": 31326}, {"laneId": 3, "quote": 18640}, {"laneId": 4, "quote": 45828}, {"laneId": 5, "quote": 18297}]},
                {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 1, "quote": 39020}, {"laneId": 2, "quote": 20648}, {"laneId": 3, "quote": 31438}, {"laneId": 4, "quote": 36447}, {"laneId": 5, "quote": 12789}]},
                {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 1, "quote": 14400}, {"laneId": 2, "quote": 44514}, {"laneId": 3, "quote": 14316}, {"laneId": 4, "quote": 10678}, {"laneId": 5, "quote": 13032}]},
                {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 1, "quote": 11601}, {"laneId": 2, "quote": 19760}, {"laneId": 3, "quote": 40870}, {"laneId": 4, "quote": 20635}, {"laneId": 5, "quote": 26421}]},
                {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 35095}, {"laneId": 2, "quote": 12494}, {"laneId": 3, "quote": 17808}, {"laneId": 4, "quote": 36210}, {"laneId": 5, "quote": 39444}]},
                {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 1, "quote": 26070}, {"laneId": 2, "quote": 41098}, {"laneId": 3, "quote": 20932}, {"laneId": 4, "quote": 16897}, {"laneId": 5, "quote": 27938}]}
              ]
            }
            """;

    public static final String TEST_CASE_2_INPUT = """
            {
              "lanes": [
                {"id": 1, "origin": "Chandigarh", "destination": "Shimla"},
                {"id": 2, "origin": "Agra", "destination": "Kanpur"},
                {"id": 3, "origin": "Varanasi", "destination": "Gorakhpur"},
                {"id": 4, "origin": "Amritsar", "destination": "Ludhiana"},
                {"id": 5, "origin": "Coimbatore", "destination": "Madurai"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter X", "laneQuotes": [{"laneId": 1, "quote": 1000}, {"laneId": 2, "quote": 1500}]},
                {"id": 2, "name": "Transporter Y", "laneQuotes": [{"laneId": 3, "quote": 2000}, {"laneId": 4, "quote": 1800}]},
                {"id": 3, "name": "Transporter Z", "laneQuotes": [{"laneId": 5, "quote": 1200}]},
                {"id": 4, "name": "Transporter W", "laneQuotes": [{"laneId": 1, "quote": 1100}, {"laneId": 4, "quote": 1750}]}
              ]
            }
            """;

    public static final String MAIN_EXAMPLE_INPUT = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"},
                {"id": 6, "origin": "Guwahati", "destination": "Shillong"},
                {"id": 7, "origin": "Lucknow", "destination": "Patna"},
                {"id": 8, "origin": "Ranchi", "destination": "Bhubaneswar"},
                {"id": 9, "origin": "Surat", "destination": "Indore"},
                {"id": 10, "origin": "Nagpur", "destination": "Raipur"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 5000}, {"laneId": 2, "quote": 6000}]},
                {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 3, "quote": 7000}, {"laneId": 4, "quote": 4000}]},
                {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 5, "quote": 3000}, {"laneId": 6, "quote": 2000}]},
                {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 7, "quote": 8000}, {"laneId": 8, "quote": 5000}]},
                {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 9, "quote": 4500}, {"laneId": 10, "quote": 3500}]},
                {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 5500}, {"laneId": 2, "quote": 6200}]},
                {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 3, "quote": 7500}, {"laneId": 4, "quote": 4500}]}
              ]
            }
            """;

    public static final String ASSIGNMENT_REQUEST = """
            {
              "maxTransporters": 3
            }
            """;

    public static final String INPUT_SUCCESS = """
            {
              "status": "success",
              "message": "Input data saved successfully."
            }
            """;

    /** True optimum for Test Case 1 with maxTransporters = 3. */
    public static final String ASSIGNMENT_SUCCESS_TC1 = """
            {
              "status": "success",
              "totalCost": 60139,
              "assignments": [
                {"laneId": 1, "transporterId": 5},
                {"laneId": 2, "transporterId": 1},
                {"laneId": 3, "transporterId": 4},
                {"laneId": 4, "transporterId": 4},
                {"laneId": 5, "transporterId": 4}
              ],
              "selectedTransporters": [1, 4, 5],
              "optimal": true
            }
            """;

    public static final String ERROR_VALIDATION = """
            {
              "status": "error",
              "message": "Validation failed",
              "errors": ["lanes[0].origin: must not be blank"]
            }
            """;

    public static final String ERROR_INVALID_INPUT = """
            {
              "status": "error",
              "message": "Invalid input data",
              "errors": ["Transporter 2 references unknown lane id 9"]
            }
            """;

    public static final String ERROR_NO_DATA = """
            {
              "status": "error",
              "message": "No input data found. Submit data via POST /api/v1/transporters/input first.",
              "errors": []
            }
            """;

    public static final String ERROR_INFEASIBLE = """
            {
              "status": "error",
              "message": "Cannot cover all 5 lanes with at most 2 transporter(s).",
              "errors": ["At least 3 transporters are required to cover all lanes, but maxTransporters is 2."]
            }
            """;
}
