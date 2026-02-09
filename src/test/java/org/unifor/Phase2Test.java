package org.unifor;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Phase 2 validation (PRD): Coordinator endpoints — create/list matrices, create/list/update/delete classes, activate matrix.
 * Uses @TestSecurity with OIDC disabled; CurrentUserService resolves user by principal name (email).
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase2Test {

    private static Long matrixId;
    private static Long matrixClassId;

    @Order(1)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void createMatrix_returns201AndBody() {
        var matrixResponse = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Matriz Fase 2\"}")
                .when()
                .post("/api/coordinator/matrices")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Matriz Fase 2"))
                .body("coordinatorId", notNullValue())
                .body("active", notNullValue())
                .body("createdAt", notNullValue())
                .extract().body().path("id");
        matrixId = Long.valueOf(String.valueOf(matrixResponse));
    }

    @Order(2)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void listMatrices_returns200WithItems() {
        given()
                .when()
                .get("/api/coordinator/matrices")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items", hasSize(greaterThanOrEqualTo(1)))
                .body("items[0].id", notNullValue())
                .body("items[0].name", notNullValue())
                .body("items[0].active", notNullValue())
                .body("items[0].classCount", notNullValue())
                .body("items[0].createdAt", notNullValue());
    }

    @Order(3)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void createMatrixClass_returns201AndBody() {
        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .body("{\"subjectId\":1,\"professorId\":1,\"timeSlotId\":1,\"authorizedCourseIds\":[1,2],\"maxStudents\":30}")
                .when()
                .post("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("matrixId", equalTo(matrixId != null ? matrixId.intValue() : null))
                .body("maxStudents", equalTo(30))
                .body("currentEnrollments", equalTo(0));

        Object firstId = given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .extract().body().path("items[0].id");
        matrixClassId = firstId instanceof Number ? ((Number) firstId).longValue() : Long.valueOf(firstId.toString());
    }

    @Order(4)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void listMatrixClasses_returns200WithItemsAndTotal() {
        given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("total", notNullValue())
                .body("items[0].id", notNullValue())
                .body("items[0].subject", notNullValue())
                .body("items[0].professor", notNullValue())
                .body("items[0].timeSlot", notNullValue())
                .body("items[0].authorizedCourses", notNullValue())
                .body("items[0].maxStudents", notNullValue())
                .body("items[0].currentEnrollments", notNullValue());
    }

    @Order(5)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void getMatrixClassById_returns200() {
        given()
                .pathParam("matrixId", matrixId)
                .pathParam("classId", matrixClassId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes/{classId}")
                .then()
                .statusCode(200)
                .body("id", equalTo(matrixClassId != null ? matrixClassId.intValue() : null))
                .body("matrixId", equalTo(matrixId != null ? matrixId.intValue() : null))
                .body("subject.id", equalTo(1))
                .body("maxStudents", equalTo(30));
    }

    @Order(6)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void updateMatrixClass_returns200() {
        given()
                .contentType(ContentType.JSON)
                .pathParam("matrixId", matrixId)
                .pathParam("classId", matrixClassId)
                .body("{\"timeSlotId\":2,\"professorId\":2,\"authorizedCourseIds\":[1]}")
                .when()
                .put("/api/coordinator/matrices/{matrixId}/classes/{classId}")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("timeSlot.id", equalTo(2))
                .body("professor.id", equalTo(2));
    }

    @Order(7)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void activateMatrix_returns204() {
        given()
                .pathParam("matrixId", matrixId)
                .when()
                .put("/api/coordinator/matrices/{matrixId}/activate")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Order(8)
    @Test
    void listMatrices_unauthenticated_returns401() {
        given()
                .when()
                .get("/api/coordinator/matrices")
                .then()
                .statusCode(401);
    }

    @Order(9)
    @Test
    @TestSecurity(user = "lucas.ferreira@unifor.br", roles = "student")
    void listMatrices_asStudent_returns403() {
        given()
                .when()
                .get("/api/coordinator/matrices")
                .then()
                .statusCode(403);
    }

    @Order(10)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void deleteMatrixClass_noEnrollments_returns204() {
        given()
                .pathParam("matrixId", matrixId)
                .pathParam("classId", matrixClassId)
                .when()
                .delete("/api/coordinator/matrices/{matrixId}/classes/{classId}")
                .then()
                .statusCode(204);
    }

    @Order(11)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void listMatrixClasses_afterDelete_excludesSoftDeletedByDefault() {
        given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(200)
                .body("items", hasSize(0))
                .body("total", equalTo(0));
    }

    @Order(12)
    @Test
    @TestSecurity(user = "carmen.lima@unifor.br", roles = "coordinator")
    void getMatrixClass_notFound_returns404() {
        given()
                .pathParam("matrixId", matrixId)
                .pathParam("classId", 999999L)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes/{classId}")
                .then()
                .statusCode(404);
    }

    @Order(13)
    @Test
    @TestSecurity(user = "roberto.alves@unifor.br", roles = "coordinator")
    void listClasses_anotherCoordinatorMatrix_returns403() {
        given()
                .pathParam("matrixId", matrixId)
                .when()
                .get("/api/coordinator/matrices/{matrixId}/classes")
                .then()
                .statusCode(403);
    }
}
