# REST API 実装 — Spring Boot 3.x

## Controller 設計

### 基本構成

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        URI location = URI.create("/api/v1/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

**ポイント:**
- `@RequestMapping` でベースパスを設定する（`/api/v1/`でバージョン管理）
- 戻り値は `ResponseEntity<T>` でステータスコードを明示する
- Controller はルーティングと DTO ↔ Service の橋渡しのみ行う

## DTO 設計（Java 17+ Record クラス）

### リクエスト DTO

```java
public record CreateUserRequest(
    @NotBlank(message = "名前は必須です")
    String name,

    @Email(message = "有効なメールアドレスを入力してください")
    @NotBlank
    String email,

    @Size(min = 8, message = "パスワードは8文字以上です")
    String password
) {}
```

### レスポンス DTO

```java
public record UserResponse(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt
) {
    // Entity から変換するファクトリメソッド
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }
}
```

**なぜ Record を使うか:**
- 不変オブジェクト（フィールドが `final`）
- `equals()`, `hashCode()`, `toString()` が自動生成される
- コンパクトで読みやすい

## Bean Validation

### 主要なアノテーション

| アノテーション | 対象 | 説明 |
|--------------|------|------|
| `@NotNull` | 任意の型 | null を許容しない |
| `@NotBlank` | String | null・空文字・空白のみを許容しない |
| `@Size(min, max)` | String/Collection | サイズ制約 |
| `@Min` / `@Max` | 数値 | 最小/最大値 |
| `@Email` | String | メール形式 |
| `@Pattern(regexp)` | String | 正規表現 |
| `@Valid` | オブジェクト | ネストした検証を有効化 |

### Controller での有効化

```java
// @Valid をリクエストボディに付与
@PostMapping
public ResponseEntity<UserResponse> create(
        @Valid @RequestBody CreateUserRequest request) {
    ...
}
```

### カスタムバリデーター

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {
    String message() default "このメールアドレスは既に使用されています";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {
    private final UserRepository userRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return !userRepository.existsByEmail(email);
    }
}
```

## 例外ハンドリング

### `@RestControllerAdvice` で一元管理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // バリデーションエラー（400）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return new ErrorResponse("VALIDATION_ERROR", "入力値が正しくありません", errors);
    }

    // リソース未発見（404）
    // jakarta.persistence.EntityNotFoundException または独自例外を使用すること
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage(), null);
    }

    // 予期しないエラー（500）
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        return new ErrorResponse("INTERNAL_ERROR", "サーバーエラーが発生しました", null);
    }
}
```

### エラーレスポンス DTO

```java
public record ErrorResponse(
    String code,
    String message,
    List<String> details
) {}
```

## HTTP ステータスコードの使い方

| 操作 | 成功時 | メソッド |
|------|--------|---------|
| 取得 | 200 OK | GET |
| 作成 | 201 Created（Location ヘッダー付き） | POST |
| 更新 | 200 OK | PUT / PATCH |
| 削除 | 204 No Content | DELETE |
| バリデーションエラー | 400 Bad Request | — |
| 認証エラー | 401 Unauthorized | — |
| 権限エラー | 403 Forbidden | — |
| 未発見 | 404 Not Found | — |
| 競合 | 409 Conflict | — |
