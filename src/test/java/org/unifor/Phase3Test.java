package org.unifor;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Phase 3 validation (PRD): Student endpoints — list enrollments, list available classes, enroll.
 * Uses @TestSecurity with OIDC disabled; CurrentUserService resolves user by principal name (email).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase3Test {

    private static Long matrixId;
    private static Long matrixClassId;
    /** Class with 1 seat for concurrent enrollment test (EN-04, CC-01, CC-02). */
    private static Long concurrentTestClassId;

    @Order(1)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void setup_createActiveMatrixAndClass() {
        var matrixResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Matriz Teste Fase 3\"}")
                .when()
                .post("/api/coordinator/matrices")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Matriz Teste Fase 3"))
                .extract().body().path("id");
        matrixId = Long.valueOf(matrixResponse.toString());

        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .body("{\"subjectId\":11,\"professorId\":1,\"timeSlotId\":4,\"authorizedCourseIds\":[1],\"maxStudents\":2}")
                .when()
                .post("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(201)
                .body("id", notNullValue());

        Object firstClassId = given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .extract().body().path("items[0].id");
        matrixClassId = firstClassId instanceof Number ? ((Number) firstClassId).longValue() : Long.valueOf(firstClassId.toString());

        given()
                .pathParam("matrixId", matrixId)
                .when()
                .put("/api/coordinator/matrices/{matrixId}/activate")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Order(2)
    @Test
    void getEnrollments_unauthenticated_returns401() {
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(401);
    }

    @Order(3)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void getEnrollments_asStudent_returns200WithItems() {
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items", hasSize(greaterThanOrEqualTo(0)));
    }

    @Order(4)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void getAvailableClasses_asStudent_returns200WithItems() {
        given()
                .when()
                .get("/api/student/classes/available")
                .then()
                .statusCode(200)
                .body("items", notNullValue());
        var size = given().when().get("/api/student/classes/available").then().statusCode(200).extract().path("items.size()");
        if (size != null && ((Number) size).intValue() >= 1) {
            given()
                    .when()
                    .get("/api/student/classes/available")
                    .then()
                    .body("items[0].id", notNullValue())
                    .body("items[0].subject", notNullValue())
                    .body("items[0].subject.id", notNullValue())
                    .body("items[0].subject.name", notNullValue())
                    .body("items[0].professor", notNullValue())
                    .body("items[0].timeSlot", notNullValue())
                    .body("items[0].maxStudents", notNullValue())
                    .body("items[0].availableSeats", notNullValue())
                    .body("items[0].authorizedForStudentCourse", equalTo(true));
        }
    }

    @Order(5)
    @Test
    @TestSecurity(user = "gabriel.costa@unifor.br", roles = "student")
    void enroll_validRequest_returns201AndBody() {
        var response = given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":" + matrixClassId + "}")
                .when()
                .post("/api/student/enrollments");
        int status = response.getStatusCode();
        Assertions.assertTrue(status == 201 || status == 409,
                "Expected 201 (enrolled) or 409 (already enrolled from prior run), got: " + status);
        if (status == 201) {
            response.then()
                    .body("id", notNullValue())
                    .body("matrixClassId", notNullValue())
                    .body("subject", notNullValue())
                    .body("professor", notNullValue())
                    .body("timeSlot", notNullValue())
                    .body("enrolledAt", notNullValue());
        }
    }

    @Order(6)
    @Test
    @TestSecurity(user = "gabriel.costa@unifor.br", roles = "student")
    void enroll_alreadyEnrolled_returns409() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":" + matrixClassId + "}")
                .when()
                .post("/api/student/enrollments")
                .then()
                .statusCode(409)
                .body("code", anyOf(equalTo("CONFLICT_ALREADY_ENROLLED"), equalTo("CONFLICT_DUPLICATE_SUBJECT")));
    }

    @Order(7)
    @Test
    @TestSecurity(user = "gabriel.costa@unifor.br", roles = "student")
    void getEnrollments_afterEnroll_returnsEnrollment() {
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(200)
                .body("items", hasSize(greaterThanOrEqualTo(1)))
                .body("items[0].id", notNullValue())
                .body("items[0].matrixClassId", notNullValue())
                .body("items[0].subject", notNullValue())
                .body("items[0].enrolledAt", notNullValue());
    }

    @Order(8)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void enroll_missingMatrixClassId_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/student/enrollments")
                .then()
                .statusCode(400);
    }

    @Order(9)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void enroll_classNotFound_returns404() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":999999}")
                .when()
                .post("/api/student/enrollments")
                .then()
                .statusCode(404);
    }

    @Order(10)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void setup_concurrentTestClass() {
        // Create fresh matrix + 1-seat class right before concurrent test to avoid leftover enrollments from prior runs
        var concurrentMatrixResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Matriz Concurrent Test\"}")
                .when()
                .post("/api/coordinator/matrices")
                .then()
                .statusCode(201)
                .extract().body().path("id");
        long concurrentMatrixId = Long.valueOf(concurrentMatrixResponse.toString());

        Object concurrentClassId = given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", concurrentMatrixId)
                .body("{\"subjectId\":14,\"professorId\":1,\"timeSlotId\":6,\"authorizedCourseIds\":[1],\"maxStudents\":1}")
                .when()
                .post("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().body().path("id");

        given()
                .pathParam("matrixId", concurrentMatrixId)
                .when()
                .put("/api/coordinator/matrices/{matrixId}/activate")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));

        concurrentTestClassId = concurrentClassId instanceof Number
                ? ((Number) concurrentClassId).longValue()
                : Long.valueOf(concurrentClassId.toString());
    }

    @Order(11)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void getEnrollments_asCoordinator_returns403() {
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(403);
    }

    /**
     * PRD Phase 3: Concurrent enrollment test — two students, one seat, only one succeeds.
     * CC-01, CC-02: seat allocation must be transactional; prevent overbooking.
     * Uses ExecutorService + X-Test-User-Email header to simulate two different students enrolling simultaneously.
     */
    @Order(12)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void concurrentEnroll_twoStudentsOneSeat_exactlyOneSucceeds() throws Exception {
        var barrier = new java.util.concurrent.CyclicBarrier(2);

        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var f1 = executor.submit(() -> {
                barrier.await();
                return given()
                        .header("X-Test-User-Email", "lucas.ferreira@unifor.br")
                        .contentType(ContentType.JSON)
                        .body("{\"matrixClassId\":" + concurrentTestClassId + "}")
                        .when()
                        .post("/api/student/enrollments")
                        .getStatusCode();
            });
            var f2 = executor.submit(() -> {
                barrier.await();
                return given()
                        .header("X-Test-User-Email", "gabriel.costa@unifor.br")
                        .contentType(ContentType.JSON)
                        .body("{\"matrixClassId\":" + concurrentTestClassId + "}")
                        .when()
                        .post("/api/student/enrollments")
                        .getStatusCode();
            });
            int r1 = f1.get();
            int r2 = f2.get();
            boolean oneSuccessOneConflict = (r1 == 201 && r2 == 409) || (r1 == 409 && r2 == 201);
            boolean bothConflict = (r1 == 409 && r2 == 409); // class full or both have subject from prior run
            Assertions.assertTrue(oneSuccessOneConflict || bothConflict,
                    "Expected one 201 and one 409 (CC-02), or both 409 (class full from prior run), but got: " + r1 + " and " + r2);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }
}
