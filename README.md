# Transporter Assignment on Lanes

A Spring Boot 3 / Java 21 service that picks at most *N* transporters to cover every freight lane at the lowest total cost, using an exact branch-and-bound solver.

| | |
|---|---|
| Live deployed URL | https://freightfox-transporter-assignment.onrender.com |
| Swagger UI | https://freightfox-transporter-assignment.onrender.com/swagger-ui.html (the root URL `/` redirects here) |
| OpenAPI JSON | https://freightfox-transporter-assignment.onrender.com/v3/api-docs |
| GitHub | https://github.com/yadavchandan84/freightfox-transporter-assignment |
| Loom video | https://www.loom.com/share/038622adb3984b1695271be0db683e01 |

> **Live demo notes.** The service runs on Render's free tier. After about 15 minutes idle it sleeps, and the first request can take around 50 seconds while it wakes up. Data is kept in memory, so it resets on every restart or wake-up. If `POST /assignment` returns `404`, submit the input again with `POST /input` first.

## 1. Problem

The input is a set of lanes (origin → destination) and a set of transporters. Each transporter quotes a price for some of the lanes. Given `maxTransporters`, the service must:

1. cover every lane,
2. use at most `maxTransporters` distinct transporters,
3. minimize the total cost,
4. use as many transporters as possible, up to the limit.

## 2. Interpretation and assumptions

- **Objective.** Choose a set `S` of transporters with `|S| ≤ maxTransporters` such that every lane is quoted by at least one member of `S`. Each lane goes to the cheapest transporter in `S`; on equal quotes, the smaller transporter id wins. Minimize the sum of the assigned quotes.
- **Tie-breaking.** Several sets can have the same minimum cost. The service then prefers the one that uses **more distinct transporters**, which is how it applies the "maximize transporter usage" goal without paying more for it. Any remaining tie goes to the **lexicographically smallest** sorted id list, so results are deterministic.
- **Cost comes first.** The service never pays more just to use another transporter. A transporter that would not win any lane at minimum cost is not selected.
- **`selectedTransporters`** lists the transporters that actually received at least one lane, sorted ascending. `assignments` is sorted by `laneId`.
- **Extra `optimal` field.** The response adds `"optimal": true|false`. It is `true` when the solver proved the result is optimal. It is `false` only if the time budget (`solver.time-budget`, default 5s) ran out first, in which case the best solution found is returned.
- **Data lifecycle.** Each `POST /input` **replaces** the stored dataset atomically. Data lives in an in-memory H2 database and is lost on restart.
- **Quotes** are positive integers (up to 1,000,000,000). Fractional values are rejected.
- **Lanes with no quote** are allowed at input time. The assignment call then returns `422` and names the uncovered lanes.
- **Infeasible limits.** If all lanes cannot be covered with `maxTransporters` transporters, the call returns `422` with the minimum number required.

### The example responses in the PDF are inconsistent

The PDF calls its sample outputs "Response (Example)", and they do not match the input data. This service returns the true optimum:

| Dataset | maxTransporters | PDF example | Actual result |
|---|---|---|---|
| Main example (10 lanes) | 3 (any value < 5) | 29000, selected [1,2,3] | **Infeasible (422).** Lanes 7–10 are quoted only by T4 and T5, and lanes 1–6 need T1/T6, T2/T7 and T3, so at least 5 transporters are required. The PDF example also assigns lanes to transporters that never quoted them (lane 7 → T2, lane 9 → T1, ...). |
| Main example | 5 | – | **48000**, selected [1,2,3,4,5] (5000+6000+7000+4000+3000+2000+8000+5000+4500+3500) |
| Test Case 1 | 3 | 75000, selected [4,6,3] | **60139**, selected [1,4,5]: lane1→T5 (11601), lane2→T1 (10512), lane3→T4 (14316), lane4→T4 (10678), lane5→T4 (13032). The PDF's own assignment adds up to 69001, not 75000, and gives lane 3 to T2, which is not in its selected list. |
| Test Case 2 | 2 | – | **Infeasible (422).** At least 3 transporters are required. |
| Test Case 2 | 3 | – | **7500**, selected [1,2,3] (lane 4 → Y at 1800) |
| Test Case 2 | 4 | – | **7450**, selected [1,2,3,4] (lane 4 → W at 1750) |

Test Case 1 for every limit: k=1 → 96940 [4], k=2 → 62938 [1,4], k=3 → 60139 [1,4,5], k≥4 → 59896 [1,3,4,5]. The automated tests check each of these against a brute-force solver.

## 3. Algorithm

The problem is a cardinality-constrained uncapacitated facility location problem, with a set cover feasibility check. It is NP-hard in general. The solver (`solver/` package, pure Java, no Spring dependencies) runs these steps in order:

1. **Uncovered lanes.** If some lane has no quote at all, return infeasible and list those lanes.
2. **Exact-safe preprocessing.**
   - Drop transporters that quote nothing.
   - Drop transporters that are *strictly dominated*: another transporter quotes every lane they quote, and each of its quotes is strictly cheaper. Swapping the dominated transporter for its dominator always lowers the cost, so a dominated transporter can never appear in an optimal solution. Equal quotes are not treated as dominance, so the tie-break rules stay intact.
3. **Minimum cover check.** An exact DFS set-cover search finds the smallest number of transporters that covers all lanes. It branches on the uncovered lane with the fewest candidates and prunes with `depth + ⌈uncovered / maxNewCoverage⌉ ≥ best`. If that minimum exceeds `maxTransporters`, the solver returns `422` with the minimum required.
4. **Heuristic incumbent.** A fast heuristic produces a first good solution, which gives the search an early bound:
   - greedy cover (most newly covered lanes, then cheapest),
   - greedy cost improvement while capacity remains,
   - first-improvement local search over drop, swap and add moves, ranked by the full objective.
5. **Branch and bound.** A depth-first include/exclude search over the candidates. Candidates are ordered by the number of lanes on which they hold the cheapest quote, then by total quote. Pruning rules:
   - **Cost bound.** For each lane, take the cheaper of the current best quote and the cheapest quote among the remaining candidates (a precomputed suffix minimum). If the sum of these exceeds the incumbent cost, prune. Equal cost is not pruned, because a tie can still win on the secondary keys.
   - **Coverage bound.** Prune if `⌈uncovered / maxCoverageOfRemainingCandidate⌉ > slotsLeft`.
   - **Useless inclusion.** Never branch on including a transporter that would not win any lane, since it cannot change the assignment.
   - Every complete cover is ranked by (cost ↑, distinct transporters ↓, id list lexicographic ↑). The same comparator (`SolutionRanking`) is used by the heuristic, the B&B search and the test-only brute force.
6. **Time budget.** The deadline is checked every 1024 nodes. If it expires, the best solution found so far is returned with `optimal: false`, so the API always responds within a bounded time.

**Complexity.** The worst case is `O(Σ_{j≤k} C(n, j) · L)`, roughly `O(2ⁿ · L)` time, for `n` transporters and `L` lanes. Memory is `O(n · L)`: the cost matrix, the suffix minima, and one per-lane array per search depth. In practice dominance filtering, the heuristic incumbent and the two bounds cut the search sharply. A random instance with 25 transporters and 200 lanes (k=6) solves in well under a second in the test suite. Instances with a few dozen transporters are solved exactly. Larger instances degrade gracefully to the time-boxed best solution.

## 4. Architecture

```
com.freightfox.transporter
├── controller   TransporterController (REST), RootRedirectController (/ → Swagger UI)
├── service      TransporterInputService (replace/read dataset), AssignmentService (solve + map), InputValidator
├── solver       BranchAndBoundSolver, HeuristicSolver, MinimumCoverCalculator, SolutionRanking, ProblemInstance, ...
├── entity       LaneEntity, TransporterEntity, LaneQuoteEntity (JPA)
├── repository   Spring Data JPA repositories
├── dto          request/response records with Bean Validation + OpenAPI annotations
├── exception    GlobalExceptionHandler + domain exceptions
└── config       OpenApiConfig, ApiExamples (Swagger examples), SolverProperties
```

Data model (H2): `lanes(id, origin, destination)`, `transporters(id, name)`, and `lane_quotes(id, transporter_id → transporters, lane_id → lanes, quote)` with a unique `(transporter_id, lane_id)`.

## 5. API reference

Base path: `/api/v1/transporters`. All bodies are JSON. Every error has the shape `{"status":"error","message":"...","errors":[...]}`.

### POST `/input`: submit (replace) lanes and quotes

Request: `{"lanes":[{"id","origin","destination"}], "transporters":[{"id","name","laneQuotes":[{"laneId","quote"}]}]}`

Response `200`:
```json
{"status": "success", "message": "Input data saved successfully."}
```

### GET `/input`: read back the stored dataset (sorted by ids)

### POST `/assignment`: compute the optimized assignment

Request: `{"maxTransporters": 3}`. Response `200` (Test Case 1):
```json
{
  "status": "success",
  "totalCost": 60139,
  "assignments": [
    {"laneId": 1, "transporterId": 5}, {"laneId": 2, "transporterId": 1},
    {"laneId": 3, "transporterId": 4}, {"laneId": 4, "transporterId": 4},
    {"laneId": 5, "transporterId": 4}
  ],
  "selectedTransporters": [1, 4, 5],
  "optimal": true
}
```

### Status codes

| Status | When | `message` |
|---|---|---|
| 200 | success | – |
| 400 | Bean Validation failure (`errors` like `lanes[0].origin: must not be blank`) | `Validation failed` |
| 400 | malformed JSON, fractional/wrong-type values (`errors` like `transporters[0].laneQuotes[0].quote: expected an integer`) | `Malformed JSON request` |
| 400 | duplicate lane/transporter ids, unknown lane reference, duplicate quote for a lane | `Invalid input data` |
| 404 | no dataset submitted yet | `No input data found. Submit data via POST /api/v1/transporters/input first.` |
| 405 | wrong HTTP method | Spring's message |
| 415 | Content-Type is not `application/json` | `Content-Type must be application/json` |
| 422 | lanes without quotes / too few transporters allowed / no solution within the time budget | explanation + details in `errors` |
| 500 | unexpected error | `Internal server error` |

### curl (bash)

```bash
BASE=http://localhost:8080   # or https://freightfox-transporter-assignment.onrender.com
curl -s -X POST $BASE/api/v1/transporters/input -H 'Content-Type: application/json' -d @sample-data/test-case-1-input.json
curl -s -X POST $BASE/api/v1/transporters/assignment -H 'Content-Type: application/json' -d '{"maxTransporters":3}'
```

### PowerShell

```powershell
$base = 'http://localhost:8080'
Invoke-RestMethod -Method Post -ContentType 'application/json' -InFile sample-data/test-case-1-input.json "$base/api/v1/transporters/input"
Invoke-RestMethod -Method Post -ContentType 'application/json' -Body '{"maxTransporters":3}' "$base/api/v1/transporters/assignment" | ConvertTo-Json -Depth 5
```

## 6. Running locally

With Docker only (no JDK needed):

```bash
docker build -t transporter-app .
docker run --rm -p 8080:8080 transporter-app
# open http://localhost:8080  ->  Swagger UI
```

With a local JDK 21:

```bash
chmod +x mvnw        # Unix, first time only
./mvnw spring-boot:run
```

Configuration: `PORT` (default 8080), `solver.time-budget` (default `5s`, e.g. `-Dsolver.time-budget=10s`), and `JAVA_OPTS` (container).

## 7. Tests

Run the full build and test suite inside Docker:

```powershell
docker run --rm -v "${PWD}:/app" -v "$env:USERPROFILE\.m2:/root/.m2" -w /app maven:3.9-eclipse-temurin-21 mvn -B clean verify
```

On Linux/macOS, `./mvnw clean verify` also works.

The suite has 462 tests:
- **Solver unit tests.** Every expected value in the table above, the tie-break rules, dominance, a zero time budget (heuristic fallback), and a performance case.
- **Property tests.** 400 seeded random instances, including 100 with heavy ties, where every field must equal a brute-force reference solver. The heuristic and the minimum-cover calculator are also checked against brute force.
- **Integration tests (MockMvc).** Exact API responses, dataset replacement, every validation and error path, Swagger and OpenAPI availability, the root redirect and actuator health.

## 8. Deployment (Render)

The repo has a multi-stage, non-root `Dockerfile` and a `render.yaml` blueprint.

1. Push the repository to GitHub.
2. In Render, choose **New → Blueprint** and select the repository. Render reads `render.yaml`, which defines a Docker web service on the free plan with health check `/actuator/health`.
3. Render injects `PORT`, and the app binds to it (`server.port=${PORT:8080}`).
4. Once the service is live, open https://freightfox-transporter-assignment.onrender.com/ and you will be redirected to Swagger UI. The OpenAPI document lists a relative `/` server first, so "Try it out" calls whichever host serves the UI.

Free-tier services sleep when idle. The first request after a sleep can take about 50 s while the container starts, and the in-memory dataset starts empty again, so re-submit the input before calling `/assignment`. The endpoints are unauthenticated and share one global dataset, which is fine for this demo but means anyone using the live URL can replace the stored data.

## 9. Sample data

The `sample-data/` folder contains:
- `test-case-1-input.json`, `test-case-2-input.json`, `main-example-input.json`: the datasets from the brief.
- `assignment-max2.json` … `assignment-max5.json`: request bodies for the assignment call.
- `requests.http`: ready-to-run requests for the VS Code REST Client and IntelliJ.
- `transporter-assignment.postman_collection.json`: a Postman v2.1 collection. Set the `baseUrl` variable to the live URL to run it against the deployment.

In Swagger UI, the input endpoint offers **Test Case 1** (the default), **Test Case 2** and **Main example** in the examples dropdown.
