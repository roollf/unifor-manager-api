package org.unifor;

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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase3Test {

    private static Long matrixId;
    private static Long matrixClassId;

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
                .body("{\"subjectId\":2,\"professorId\":1,\"timeSlotId\":2,\"authorizedCourseIds\":[1],\"maxStudents\":2}")
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
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void enroll_validRequest_returns201AndBody() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"matrixClassId\":" + matrixClassId + "}")
                .when()
                .post("/api/student/enrollments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("matrixClassId", notNullValue())
                .body("subject", notNullValue())
                .body("professor", notNullValue())
                .body("timeSlot", notNullValue())
                .body("enrolledAt", notNullValue());
    }

    @Order(6)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
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
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
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
    void getEnrollments_asCoordinator_returns403() {
        given()
                .when()
                .get("/api/student/enrollments")
                .then()
                .statusCode(403);
    }
}
