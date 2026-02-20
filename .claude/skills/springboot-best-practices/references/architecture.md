# アーキテクチャ設計 — Spring Boot 3.x

## レイヤードアーキテクチャ

### 各レイヤーの責務

```
┌─────────────────────────────────────────┐
│  Controller（@RestController）           │  HTTPリクエスト/レスポンスの処理のみ
│  ↓ DTO                                  │
│  Service（@Service）                     │  ビジネスロジック・トランザクション管理
│  ↓ Entity/DTO                           │
│  Repository（@Repository）               │  データアクセスのみ
│  ↓                                      │
│  Database                               │
└─────────────────────────────────────────┘
```

**Controller が持つべきでないもの:**
- ビジネスロジック（Service に移す）
- DBアクセスコード（Repository に移す）
- `@Transactional`（Service に付与する）

**Service が持つべきでないもの:**
- HTTP 固有のオブジェクト（`HttpServletRequest`, `HttpServletResponse`）
- `@RequestMapping` などの Web 層アノテーション

## パッケージ構成

### 機能別（Feature-based）パッケージ — 推奨

中〜大規模プロジェクトに適している。機能単位で変更が閉じる。

```
com.example.app/
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── User.java                  # Entity
│   ├── UserResponse.java          # DTO
│   └── CreateUserRequest.java     # DTO
├── product/
│   ├── ProductController.java
│   ├── ProductService.java
│   └── ...
└── common/
    ├── exception/
    │   └── GlobalExceptionHandler.java
    └── dto/
        └── ErrorResponse.java
```

### レイヤー別パッケージ — 小規模向け

```
com.example.app/
├── controller/
├── service/
├── repository/
├── entity/
└── dto/
```

## ステレオタイプアノテーションの使い分け

| アノテーション | 対象 | 追加効果 |
|--------------|------|---------|
| `@Component` | 汎用コンポーネント | なし |
| `@Service` | ビジネスロジック層 | なし（意味的なマーキング） |
| `@Repository` | データアクセス層 | SQLException を DataAccessException に変換 |
| `@Controller` | Web MVC コントローラー | ビュー解決 |
| `@RestController` | REST API コントローラー | `@Controller` + `@ResponseBody` |

## コンストラクタインジェクション

### なぜフィールド注入を避けるか

```java
// ❌ フィールド注入の問題点
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepository;  // テストで差し替えが困難
    @Autowired
    private ProductRepository productRepository;  // final にできない
}

// ✅ コンストラクタ注入のメリット
@Service
@RequiredArgsConstructor  // Lombok: final フィールドのコンストラクタを自動生成
public class OrderService {
    private final UserRepository userRepository;      // final で不変性を保証
    private final ProductRepository productRepository; // テストでモック注入が容易
}
```

**コンストラクタ注入のメリット:**
1. `final` で不変性を保証できる
2. テストでコンストラクタ経由でモックを注入できる
3. 依存が多くなると気づきやすい（設計の問題に早期に気づける）
4. Spring なしでインスタンス化できる

## 設定クラス

### `@Configuration` + `@Bean`

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### `@ConfigurationProperties`（型安全な設定値取得）

```java
// application.yml
// app:
//   jwt:
//     secret: mySecretKey
//     expiration-ms: 86400000

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {}

// 使用側
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;
}
```

`@Value` よりも型安全でテストしやすい。
