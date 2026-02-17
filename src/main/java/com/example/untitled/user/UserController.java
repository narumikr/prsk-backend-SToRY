package com.example.untitled.user;

import com.example.untitled.user.dto.UserListResponse;
import com.example.untitled.user.dto.UserRequest;
import com.example.untitled.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    // GET /users : ユーザー取得一覧 - Get users list
    @GetMapping
    public ResponseEntity<UserListResponse> getUsersList(
            @RequestParam(required = false, defaultValue = "1")
            @Min(value = 1, message = "Page must be 1 or greater") Integer page,
            @RequestParam(required = false, defaultValue = "20")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 100, message = "Limit must not exceed 100") Integer limit
    ) {
        Page<User> userPage = userService.getAllUsers(page - 1, limit, "userName", "ASC");
        UserListResponse response = UserListResponse.from(userPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // POST /users : ユーザー情報の登録 - Register user information
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRequest request
    ) {
        User user = userService.createUser(request);
        UserResponse response = UserResponse.from(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /users/{id} : ユーザー情報の更新 - Update user information
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable @Min(value = 1, message = "ID must be 1 or greater.") Long id,
            @Valid @RequestBody UserRequest request
    ) {
        User user = userService.updateUser(id, request);
        UserResponse response = UserResponse.from(user);
        return ResponseEntity.ok(response);
    }

    // DELETE /users/{id} : ユーザー情報の削除 - Delete user information
    @DeleteMapping("/{id}")
    public ResponseEntity deleteUser(
            @PathVariable @Min(value = 1, message = "ID must be 1 or greater.") Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
