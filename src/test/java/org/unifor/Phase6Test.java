package org.unifor;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Phase 6 validation (PRD): Comprehensive testing.
 * - Filter integration tests (periodOfDay, authorizedCourseId, maxStudentsMin/Max)
 * - Duplicate-subject enrollment conflict test
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase6Test {

    private static Long matrixId;
    private static Long morningClassId;
    private static Long afternoonClassId;

    @Order(1)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void setup_createMatrixWithClassesForFilterTests() {
        var matrixResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Matriz Fase 6 Filters\"}")
                .when()
                .post("/api/coordinator/matrices")
                .then()
                .statusCode(201)
                .extract().body().path("id");
        matrixId = Long.valueOf(matrixResponse.toString());

        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .body("{\"subjectId\":15,\"professorId\":1,\"timeSlotId\":1,\"authorizedCourseIds\":[1,2],\"maxStudents\":20}")
                .when()
                .post("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(201);
        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .body("{\"subjectId\":15,\"professorId\":2,\"timeSlotId\":4,\"authorizedCourseIds\":[1],\"maxStudents\":50}")
                .when()
                .post("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(201);

        var items = given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .extract().body().path("items");
        morningClassId = ((Number) ((java.util.Map<?, ?>) ((java.util.List<?>) items).get(0)).get("id")).longValue();
        afternoonClassId = ((Number) ((java.util.Map<?, ?>) ((java.util.List<?>) items).get(1)).get("id")).longValue();

        given()
                .pathParam("matrixId", matrixId)
                .when()
                .put("/api/coordinator/matrices/{matrixId}/activate")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Order(2)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void listClasses_filterByPeriodOfDayMorning_returnsMatchingClasses() {
        given()
                .pathParam("matrixId", matrixId)
                .queryParam("periodOfDay", "MORNING")
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .body("items", hasSize(greaterThanOrEqualTo(1)))
                .body("items.subject.id", hasItems(15));
    }

    @Order(3)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void listClasses_filterByAuthorizedCourseId_returnsMatchingClasses() {
        given()
                .pathParam("matrixId", matrixId)
                .queryParam("authorizedCourseId", 1)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .body("items", hasSize(greaterThanOrEqualTo(2)));
    }

    @Order(4)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void listClasses_filterByMaxStudentsRange_returnsMatchingClasses() {
        given()
                .pathParam("matrixId", matrixId)
                .queryParam("maxStudentsMin", 25)
                .queryParam("maxStudentsMax", 60)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .body("items", hasSize(greaterThanOrEqualTo(1)))
                .body("items[0].maxStudents", greaterThanOrEqualTo(25))
                .body("items[0].maxStudents", lessThanOrEqualTo(60));
    }

    /**
     * PRD EN-08: Student cannot enroll in same subject more than once.
     * Enroll in class A (subject 1), then try class B (subject 1, different slot) -> 409 CONFLICT_DUPLICATE_SUBJECT.
     */
    @Order(5)
    @Test
    @TestSecurity(user = "gabriel.costa@unifor.br", roles = "student")
    void enroll_duplicateSubjectDifferentClass_returns409() {
        var firstStatus = given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":" + morningClassId + "}")
                .when()
                .post("/api/student/enrollments")
                .getStatusCode();
        Assertions.assertTrue(firstStatus == 201 || firstStatus == 409, "First enroll: 201 or 409 (already enrolled)");

        given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":" + afternoonClassId + "}")
                .when()
                .post("/api/student/enrollments")
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT_DUPLICATE_SUBJECT"));
    }
}
