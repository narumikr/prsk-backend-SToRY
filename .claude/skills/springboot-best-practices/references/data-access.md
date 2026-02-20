# データアクセス層 — Spring Boot 3.x / Spring Data JPA

## エンティティ設計

### 基本的なエンティティ

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA要件: 引数なしコンストラクタ
@EntityListeners(AuditingEntityListener.class)       // @CreatedDate/@LastModifiedDate を有効化
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB の AUTO_INCREMENT
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ファクトリメソッドで生成
    public static User create(String name, String email) {
        User user = new User();
        user.name = name;
        user.email = email;
        return user;
    }

    // 状態変更メソッド（ビジネスロジック）
    public void updateName(String name) {
        this.name = name;
    }
}
```

**ポイント:**
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`: JPA は引数なしコンストラクタが必要だが、外部からの直接生成を防ぐ
- セッターを公開せず、意図が明確なメソッドで状態変更する
- `@CreatedDate` / `@LastModifiedDate` は `@EnableJpaAuditing` が必要

### ID 戦略の選択

| 戦略 | 説明 | 適用場面 |
|------|------|---------|
| `IDENTITY` | DBの AUTO_INCREMENT | MySQL, PostgreSQL（推奨） |
| `SEQUENCE` | DBシーケンス | Oracle, PostgreSQL |
| `UUID` | UUID型 | 分散システム、公開ID |

```java
// UUID の例
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

## Repository 設計

### `JpaRepository` を継承する

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // メソッド名からクエリ自動生成
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByNameContaining(String keyword);

    // JPQL カスタムクエリ
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    // ネイティブSQL（必要な場合のみ）
    @Query(value = "SELECT * FROM users WHERE LOWER(name) = LOWER(:name)",
           nativeQuery = true)
    List<User> findByNameIgnoreCaseNative(@Param("name") String name);
}
```

**`JpaRepository<T, ID>` が提供するメソッド:**
- `findById()`, `findAll()`, `save()`, `delete()`, `count()` など
- `existsById()`, `findAllById()` など

### Specification（動的クエリ）

フィルター条件が動的に変わる場合:

```java
public class UserSpecifications {
    public static Specification<User> hasName(String name) {
        return (root, query, cb) ->
            name == null ? null : cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) ->
            email == null ? null : cb.equal(root.get("email"), email);
    }
}

// 使用側
List<User> users = userRepository.findAll(
    Specification.where(hasName(nameFilter))
                 .and(hasEmail(emailFilter))
);
```

Repository は `JpaRepository` に加えて `JpaSpecificationExecutor<T>` も継承する。

## トランザクション管理

### `@Transactional` の基本

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 参照系: readOnly = true でパフォーマンス向上（ダーティチェック無効化）
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }

    // 更新系: デフォルト（readOnly = false）
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.updateName(request.name());  // ダーティチェックで自動更新される
        return UserResponse.from(user);
    }
}
```

### 伝播レベル（主要なもの）

| 伝播レベル | 説明 | 使用場面 |
|-----------|------|---------|
| `REQUIRED`（デフォルト） | 既存のトランザクションに参加、なければ新規作成 | 通常の更新処理 |
| `REQUIRES_NEW` | 常に新規トランザクションを開始 | 独立したログ記録など |
| `SUPPORTS` | トランザクションがあれば参加、なければなしで実行 | 参照系で柔軟に対応 |

## N+1 問題と対処法

### N+1 問題とは

```java
// ❌ N+1 問題が発生する例
List<Order> orders = orderRepository.findAll();  // 1クエリ
for (Order order : orders) {
    order.getUser().getName();  // N クエリ（orders の数だけ実行）
}
```

### 対処法 1: `@EntityGraph`（推奨）

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "orderItems"})
    List<Order> findAll();

    @EntityGraph(attributePaths = {"user"})
    Optional<Order> findById(Long id);
}
```

### 対処法 2: JPQL Fetch Join

```java
@Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.orderItems WHERE o.id = :id")
Optional<Order> findByIdWithDetails(@Param("id") Long id);
```

### 対処法 3: `@BatchSize`（コレクション用）

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user")
    @BatchSize(size = 100)  // IN句で一括取得
    private List<Order> orders;
}
```

### どれを選ぶか

- 単一エンティティの取得 → `@EntityGraph`
- 複雑な条件の JOIN → JPQL Fetch Join
- コレクションの遅延読み込みを許容 → `@BatchSize`

## リレーションシップ設計

```java
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多対一: EAGER はデフォルトなので明示的に LAZY に変更
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 一対多: LAZY がデフォルト（変更不要）
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
}
```

**重要:** `@ManyToOne` のデフォルトは `EAGER`（即時ロード）なので、必ず `FetchType.LAZY` を指定する。
