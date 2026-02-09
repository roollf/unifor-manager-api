package org.unifor;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Phase 4 validation (PRD): Security and Access Control.
 * Validation criteria:
 * - Unauthenticated requests return 401
 * - Coordinator cannot access another coordinator's matrix
 * - Student cannot see other students' enrollments
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase4Test {

    private static Long matrixId;
    private static Long matrixClassId;

    @Order(1)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void setup_createMatrixAndClass() {
        var matrixResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Matriz Fase 4\"}")
                .when()
                .post("/api/coordinator/matrices")
                .then()
                .statusCode(201)
                .extract().body().path("id");
        matrixId = Long.valueOf(matrixResponse.toString());

        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .body("{\"subjectId\":13,\"professorId\":1,\"timeSlotId\":5,\"authorizedCourseIds\":[1],\"maxStudents\":10}")
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

    @Order(2)
    @Test
    @TestSecurity(user = "gabriel.costa@unifor.br", roles = "student")
    void setup_gabrielEnrolls() {
        int status = given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":" + matrixClassId + "}")
                .when()
                .post("/api/student/enrollments")
                .getStatusCode();
        Assertions.assertTrue(status == 201 || status == 409,
                "Expected 201 (enrolled) or 409 (already enrolled from prior run), got: " + status);
    }

    @Order(3)
    @Test
    void unauthenticated_getCoordinatorMatrices_returns401() {
        given()
                .when()
                .get("/api/coordinator/matrices")
                .then()
                .statusCode(401);
    }

    @Order(4)
    @Test
    void unauthenticated_getStudentEnrollments_returns401() {
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(401);
    }

    @Order(5)
    @Test
    @TestSecurity(user = "roberto.alves@unifor.br", roles = "coordinator")
    void coordinator_anotherCoordinatorMatrix_returns403() {
        given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(403);
    }

    @Order(6)
    @Test
    @TestSecurity(user = "beatriz.rodrigues@unifor.br", roles = "student")
    void student_cannotSeeOtherStudentsEnrollments() {
        // Beatriz (course 2) cannot enroll in classes authorized for course 1; has no enrollments.
        // Must not see Gabriel's enrollment (AC-03, AC-04).
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }
}
