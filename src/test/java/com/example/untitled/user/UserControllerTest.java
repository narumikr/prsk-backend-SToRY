package com.example.untitled.user;

import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.exception.DuplicationResourceException;
import com.example.untitled.common.exception.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.example.untitled.common.util.UtilsFunction.generateRandomString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mvcMock;

    @MockitoBean
    private UserService userService;

    private User createMockUser(Long id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        user.setPassword("testpassword");
        return user;
    }

    /**
     * POST /users : Response success
     * ユーザー登録の正常系
     */
    @Test
    public void registerUserSuccess() throws Exception {
        User mockUser = createMockUser(1L, "testuser");

        when(userService.createUser(any())).thenReturn(mockUser);

        String reqBody = """
                {
                    "userName": "testuser",
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"));

        verify(userService, times(1)).createUser(any());
    }

    /**
     * POST /users : Response BadRequest
     * userNameのNull不可チェック
     */
    @Test
    public void registerUserError_withBadRequest_UserNameNull() throws Exception {
        String reqBody = """
                {
                    "userName": null,
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("userName"));
    }

    /**
     * POST /users : Response BadRequest
     * バリデーションエラー（全フィールド文字数超過）
     */
    @Test
    public void registerUserError_withBadRequest_LengthOver() throws Exception {
        String ngUserName = generateRandomString(21);
        String ngPassword = generateRandomString(21);

        String reqBody = """
                {
                    "userName": "%s",
                    "password": "%s"
                }
                """.formatted(ngUserName, ngPassword);

        mvcMock.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[*].field", containsInAnyOrder("userName", "password")));
    }

    /**
     * POST /users : Conflict
     * ユーザー名の重複エラー
     */
    @Test
    public void registerUserError_withConflict_AlreadyExist() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new DuplicationResourceException(
                        "Conflict detected",
                        List.of(new ErrorDetails("userName", "User name already exist: testuser"))
                ));

        String reqBody = """
                {
                    "userName": "testuser",
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("userName"));
    }

    /**
     * GET /users : Response success
     * ユーザー一覧取得の正常系
     */
    @Test
    public void getUsersListSuccess() throws Exception {
        User user1 = createMockUser(1L, "User 1");
        User user2 = createMockUser(2L, "User 2");

        Page<User> userPage = new PageImpl<>(
                Arrays.asList(user1, user2),
                PageRequest.of(0, 20),
                2
        );

        when(userService.getAllUsers(0, 20, "userName", "ASC")).thenReturn(userPage);

        mvcMock.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].userName").value("User 1"))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[1].userName").value("User 2"))
                .andExpect(jsonPath("$.meta.pageIndex").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.limit").value(20));

        verify(userService, times(1)).getAllUsers(0, 20, "userName", "ASC");
    }

    /**
     * GET /users : Response success with pagination parameters
     * ページネーションパラメータを指定した一覧取得
     */
    @Test
    public void getUsersListSuccess_WithPaginationParams() throws Exception {
        User user1 = createMockUser(11L, "User 11");
        User user2 = createMockUser(12L, "User 12");
        User user3 = createMockUser(13L, "User 13");
        User user4 = createMockUser(14L, "User 14");
        User user5 = createMockUser(15L, "User 15");

        Page<User> userPage = new PageImpl<>(
                List.of(user1, user2, user3, user4, user5),
                PageRequest.of(1, 10),
                15
        );

        when(userService.getAllUsers(1, 10, "userName", "ASC")).thenReturn(userPage);

        mvcMock.perform(get("/users")
                        .param("page", "2")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.meta.pageIndex").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.meta.totalItems").value(15))
                .andExpect(jsonPath("$.meta.limit").value(10));

        verify(userService, times(1)).getAllUsers(1, 10, "userName", "ASC");
    }

    /**
     * GET /users : BadRequest
     * 不正なページネーションパラメータ（pageが0以下）
     */
    @Test
    public void getUsersListError_withBadRequest_InvalidPage() throws Exception {
        mvcMock.perform(get("/users")
                        .param("page", "0")
                        .param("limit", "20"))
                .andExpect(status().isBadRequest());
    }

    /**
     * GET /users : BadRequest
     * 不正なページネーションパラメータ（limitが範囲外）
     */
    @Test
    public void getUsersListError_withBadRequest_InvalidLimit() throws Exception {
        mvcMock.perform(get("/users")
                        .param("page", "1")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());

        mvcMock.perform(get("/users")
                        .param("page", "1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    /**
     * PUT /users/{id} : Response success
     * ユーザー情報更新の正常系
     */
    @Test
    public void updateUserSuccess() throws Exception {
        User updatedUser = createMockUser(1L, "updateduser");

        when(userService.updateUser(eq(1L), any())).thenReturn(updatedUser);

        String reqBody = """
                {
                    "userName": "updateduser",
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("updateduser"));

        verify(userService, times(1)).updateUser(eq(1L), any());
    }

    /**
     * PUT /users/{id} : NotFound
     * 存在しないユーザーIDを指定した場合
     */
    @Test
    public void updateUserError_withNotFound() throws Exception {
        when(userService.updateUser(eq(999L), any()))
                .thenThrow(new EntityNotFoundException("User not found with id: 999"));

        String reqBody = """
                {
                    "userName": "updateduser",
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(put("/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq(999L), any());
    }

    /**
     * PUT /users/{id} : Unauthorized
     * パスワードが一致しない場合
     */
    @Test
    public void updateUserError_withUnauthorized_WrongPassword() throws Exception {
        when(userService.updateUser(eq(1L), any()))
                .thenThrow(new UnauthorizedException(
                        "Authentication failed",
                        List.of(new ErrorDetails("password", "Invalid password"))
                ));

        String reqBody = """
                {
                    "userName": "testuser",
                    "password": "wrongpassword"
                }
                """;

        mvcMock.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("password"));

        verify(userService, times(1)).updateUser(eq(1L), any());
    }

    /**
     * PUT /users/{id} : BadRequest
     * バリデーションエラー（文字数超過）
     */
    @Test
    public void updateUserError_withBadRequest_LengthOver() throws Exception {
        String ngUserName = generateRandomString(21);

        String reqBody = """
                {
                    "userName": "%s",
                    "password": "testpassword"
                }
                """.formatted(ngUserName);

        mvcMock.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("userName"));

        verify(userService, never()).updateUser(anyLong(), any());
    }

    /**
     * PUT /users/{id} : Conflict
     * ユーザー名の重複エラー
     */
    @Test
    public void updateUserError_withConflict_AlreadyExist() throws Exception {
        when(userService.updateUser(eq(1L), any()))
                .thenThrow(new DuplicationResourceException(
                        "Conflict detected",
                        List.of(new ErrorDetails("userName", "User name already exist: duplicateuser"))
                ));

        String reqBody = """
                {
                    "userName": "duplicateuser",
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("userName"));

        verify(userService, times(1)).updateUser(eq(1L), any());
    }

    /**
     * PUT /users/{id} : BadRequest
     * 不正なIDパラメータ（0以下）
     */
    @Test
    public void updateUserError_withBadRequest_InvalidId() throws Exception {
        String reqBody = """
                {
                    "userName": "testuser",
                    "password": "testpassword"
                }
                """;

        mvcMock.perform(put("/users/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(anyLong(), any());
    }

    /**
     * DELETE /users/{id} : Response success
     * ユーザー削除の正常系
     */
    @Test
    public void deleteUserSuccess() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mvcMock.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    /**
     * DELETE /users/{id} : NotFound
     * 存在しないユーザーIDを指定した場合
     */
    @Test
    public void deleteUserError_withNotFound() throws Exception {
        doThrow(new EntityNotFoundException("User not found for id: 999"))
                .when(userService).deleteUser(999L);

        mvcMock.perform(delete("/users/999"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser(999L);
    }
}
