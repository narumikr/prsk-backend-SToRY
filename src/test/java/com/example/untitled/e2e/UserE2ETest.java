package com.example.untitled.e2e;

import com.example.untitled.common.dto.ErrorResponse;
import com.example.untitled.user.dto.UserListResponse;
import com.example.untitled.user.dto.UserRequest;
import com.example.untitled.user.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User E2E Tests")
class UserE2ETest extends E2ETestBase {

    private static final String USERS_PATH = "/users";

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String uniqueUserName() {
        return "User-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private UserResponse createUser(String userName, String password) {
        UserRequest request = new UserRequest();
        request.setUserName(userName);
        request.setPassword(password);

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                getBaseUrl() + USERS_PATH,
                request,
                UserResponse.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private UserResponse createUser(String userName) {
        return createUser(userName, "testpassword");
    }

    // ========================================================================
    // GET /users - List Users
    // ========================================================================

    @Nested
    @DisplayName("GET /users")
    class GetUsers {

        @Test
        @DisplayName("Success - returns list with created users")
        void getUsersSuccess() {
            // Arrange: Create test users
            createUser(uniqueUserName());
            createUser(uniqueUserName());

            // Act
            ResponseEntity<UserListResponse> response = restTemplate.getForEntity(
                    getBaseUrl() + USERS_PATH,
                    UserListResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getItems());
            assertNotNull(response.getBody().getMetaInfo());
            assertTrue(response.getBody().getItems().size() >= 2);
        }

        @Test
        @DisplayName("Success - returns paginated results with page and limit params")
        void getUsersSuccess_withPagination() {
            // Arrange: Create 3 users
            for (int i = 0; i < 3; i++) {
                createUser(uniqueUserName());
            }

            // Act: Get page 1 with limit 2
            ResponseEntity<UserListResponse> response = restTemplate.getForEntity(
                    getBaseUrl() + USERS_PATH + "?page=1&limit=2",
                    UserListResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getMetaInfo());
            assertEquals(2, response.getBody().getMetaInfo().getLimit());
            assertTrue(response.getBody().getItems().size() <= 2);
        }
    }

    // ========================================================================
    // POST /users - Create User
    // ========================================================================

    @Nested
    @DisplayName("POST /users")
    class CreateUser {

        @Test
        @DisplayName("Success - creates user with all fields")
        void createUserSuccess() {
            // Arrange
            String userName = uniqueUserName();
            UserRequest request = new UserRequest();
            request.setUserName(userName);
            request.setPassword("testpassword");

            // Act
            ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + USERS_PATH,
                    request,
                    UserResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getId());
            assertEquals(userName, response.getBody().getUserName());
            assertNotNull(response.getBody().getAuditInfo());
        }

        @Test
        @DisplayName("Error - 409 Conflict when userName already exists")
        void createUserError_withConflict() {
            // Arrange: Create a user first
            String existingName = uniqueUserName();
            createUser(existingName);

            // Try to create another with the same userName
            UserRequest duplicateRequest = new UserRequest();
            duplicateRequest.setUserName(existingName);
            duplicateRequest.setPassword("anotherpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + USERS_PATH,
                    duplicateRequest,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when userName is blank")
        void createUserError_withBadRequest_blankUserName() {
            // Arrange
            UserRequest request = new UserRequest();
            request.setUserName("");
            request.setPassword("testpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + USERS_PATH,
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when userName is null")
        void createUserError_withBadRequest_nullUserName() {
            // Arrange
            UserRequest request = new UserRequest();
            request.setUserName(null);
            request.setPassword("testpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + USERS_PATH,
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when userName exceeds max length")
        void createUserError_withBadRequest_userNameTooLong() {
            // Arrange: userName max is 20 characters
            UserRequest request = new UserRequest();
            request.setUserName("U".repeat(21));
            request.setPassword("testpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + USERS_PATH,
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ========================================================================
    // PUT /users/{id} - Update User
    // ========================================================================

    @Nested
    @DisplayName("PUT /users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("Success - updates userName")
        void updateUserSuccess() {
            // Arrange: Create a user
            String password = "testpassword";
            UserResponse created = createUser(uniqueUserName(), password);

            UserRequest updateRequest = new UserRequest();
            updateRequest.setUserName(uniqueUserName());
            updateRequest.setPassword(password);

            // Act
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + created.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    UserResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(created.getId(), response.getBody().getId());
            assertEquals(updateRequest.getUserName(), response.getBody().getUserName());
        }

        @Test
        @DisplayName("Error - 404 Not Found when ID does not exist")
        void updateUserError_withNotFound() {
            // Arrange
            UserRequest updateRequest = new UserRequest();
            updateRequest.setUserName(uniqueUserName());
            updateRequest.setPassword("testpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + Long.MAX_VALUE,
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 401 Unauthorized when password is wrong")
        void updateUserError_withUnauthorized_wrongPassword() {
            // Arrange: Create a user with known password
            UserResponse created = createUser(uniqueUserName(), "correctpassword");

            UserRequest updateRequest = new UserRequest();
            updateRequest.setUserName(uniqueUserName());
            updateRequest.setPassword("wrongpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + created.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(401, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 409 Conflict when userName already exists for another user")
        void updateUserError_withConflict() {
            // Arrange: Create two users
            String existingName = uniqueUserName();
            createUser(existingName);
            UserResponse toUpdate = createUser(uniqueUserName(), "testpassword");

            // Try to update second user with first user's userName
            UserRequest updateRequest = new UserRequest();
            updateRequest.setUserName(existingName);
            updateRequest.setPassword("testpassword");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + toUpdate.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatusCode());
        }
    }

    // ========================================================================
    // DELETE /users/{id} - Delete User
    // ========================================================================

    @Nested
    @DisplayName("DELETE /users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("Success - deletes user and returns 204")
        void deleteUserSuccess() {
            // Arrange: Create a user
            UserResponse created = createUser(uniqueUserName());

            // Act
            ResponseEntity<Void> response = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + created.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            // Assert
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            // Verify: Try to delete again should return 404 (soft-deleted)
            ResponseEntity<ErrorResponse> verifyResponse = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + created.getId(),
                    HttpMethod.DELETE,
                    null,
                    ErrorResponse.class
            );
            assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
        }

        @Test
        @DisplayName("Error - 404 Not Found when ID does not exist")
        void deleteUserError_withNotFound() {
            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + Long.MAX_VALUE,
                    HttpMethod.DELETE,
                    null,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Success - deleted user does not appear in list")
        void deleteUserSuccess_notInList() {
            // Arrange: Create and delete a user
            UserResponse created = createUser(uniqueUserName());

            restTemplate.exchange(
                    getBaseUrl() + USERS_PATH + "/" + created.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            // Act: Get list
            ResponseEntity<UserListResponse> listResponse = restTemplate.getForEntity(
                    getBaseUrl() + USERS_PATH,
                    UserListResponse.class
            );

            // Assert: Deleted user should not be in the list
            assertEquals(HttpStatus.OK, listResponse.getStatusCode());
            assertNotNull(listResponse.getBody());

            boolean userFound = listResponse.getBody().getItems().stream()
                    .anyMatch(u -> u.getId().equals(created.getId()));
            assertFalse(userFound, "Deleted user should not appear in list");
        }
    }
}
