package org.unifor;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 validation (PRD): Concurrency Handling.
 * Validation criteria:
 * - Load test: N students enroll in class with N-1 seats; exactly N-1 succeed
 * - No duplicate enrollments under concurrency
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase5Test {

    private static Long matrixId;
    private static Long matrixClassId;

    private static final String[] STUDENT_EMAILS = {
            "lucas.ferreira@unifor.br",
            "beatriz.rodrigues@unifor.br",
            "rafael.pereira@unifor.br",
            "juliana.martins@unifor.br",
            "gabriel.costa@unifor.br"
    };

    @Order(1)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void setup_createMatrixAndClassWithFourSeats() {
        var matrixResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Matriz Fase 5 Concurrent\"}")
                .when()
                .post("/api/coordinator/matrices")
                .then()
                .statusCode(201)
                .extract().body().path("id");
        matrixId = Long.valueOf(matrixResponse.toString());

        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .body("{\"subjectId\":15,\"professorId\":1,\"timeSlotId\":8,\"authorizedCourseIds\":[1,2,4,6],\"maxStudents\":4}")
                .when()
                .post("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(201);

        Object firstId = given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .extract().body().path("items[0].id");
        matrixClassId = firstId instanceof Number ? ((Number) firstId).longValue() : Long.valueOf(firstId.toString());

        given()
                .pathParam("matrixId", matrixId)
                .when()
                .put("/api/coordinator/matrices/{matrixId}/activate")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    /**
     * PRD Phase 5: Load test — 5 students, 4 seats; exactly 4 succeed, 1 fails.
     * Validates CC-01, CC-02: transactional seat allocation, no overbooking.
     */
    @Order(2)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void concurrentEnroll_fiveStudentsFourSeats_exactlyFourSucceed() throws Exception {
        int n = 5;
        var barrier = new CyclicBarrier(n);
        var executor = Executors.newFixedThreadPool(n);
        var body = "{\"matrixClassId\":" + matrixClassId + "}";

        try {
            List<Future<Integer>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                String email = STUDENT_EMAILS[i];
                futures.add(executor.submit(() -> {
                    barrier.await();
                    return given()
                            .header("X-Test-User-Email", email)
                            .contentType(ContentType.JSON)
                            .body(body)
                            .when()
                            .post("/api/student/enrollments")
                            .getStatusCode();
                }));
            }

            int successCount = 0;
            int conflictCount = 0;
            for (Future<Integer> f : futures) {
                int status = f.get(10, TimeUnit.SECONDS);
                if (status == 201) successCount++;
                else if (status == 409) conflictCount++;
                else fail("Expected 201 or 409, got: " + status);
            }

            assertEquals(4, successCount, "Exactly 4 enrollments must succeed (N-1 seats)");
            assertEquals(1, conflictCount, "Exactly 1 must fail with 409 (no seats)");
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }
}
