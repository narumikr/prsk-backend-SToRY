# Before/After 実装例 — Spring Boot 3.x

## 例 1: フィールド注入 → コンストラクタ注入

**Before（問題あり）:**
```java
@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // テスト時にモックを注入できない
    // final にできない
}
```

**After（推奨）:**
```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    // Lombok が自動生成するコンストラクタで注入される
}
```

---

## 例 2: エンティティを直接返す → DTO で分離

**Before（問題あり）:**
```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
    // エンティティをそのまま返すと:
    // - パスワードなど機密情報が漏れる
    // - Entity の変更が API 仕様に直結する
    // - 循環参照で JSON シリアライズが失敗しやすい
}
```

**After（推奨）:**
```java
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
}

// Service 層
@Transactional(readOnly = true)
public UserResponse findById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    return UserResponse.from(user);  // DTOに変換して返す
}

// DTO（Record クラス）
public record UserResponse(Long id, String name, String email, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
```

---

## 例 3: ビジネスロジックが Controller に → Service 層に分離

**Before（問題あり）:**
```java
@PostMapping("/orders")
public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
    // Controller にビジネスロジックが混在
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new RuntimeException("User not found"));

    Product product = productRepository.findById(request.productId())
        .orElseThrow(() -> new RuntimeException("Product not found"));

    if (product.getStock() < request.quantity()) {
        throw new RuntimeException("Stock insufficient");
    }

    Order order = new Order();
    order.setUser(user);
    order.setProduct(product);
    order.setQuantity(request.quantity());
    order.setTotalPrice(product.getPrice() * request.quantity());
    product.setStock(product.getStock() - request.quantity());

    orderRepository.save(order);
    productRepository.save(product);

    return ResponseEntity.ok(order);
}
```

**After（推奨）:**
```java
// Controller: ルーティングと DTO の橋渡しのみ
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = orderService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// Service: ビジネスロジックとトランザクション管理
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        product.decreaseStock(request.quantity());  // エンティティのメソッドでビジネスルール
        Order order = Order.create(user, product, request.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }
}
```

---

## 例 4: 例外ハンドリングを一元化する

**Before（問題あり）:**
```java
// 各 Controller で個別にハンドリング（重複が多い）
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(userService.findById(id));
    } catch (EntityNotFoundException e) {
        return ResponseEntity.notFound().build();
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
```

**After（推奨）:**
```java
// Controller: 正常系のみ書く
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
}

// 例外ハンドリングは @RestControllerAdvice に集約
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return new ErrorResponse("VALIDATION_ERROR", "入力値が正しくありません", details);
    }
}
```

---

## 例 5: N+1 問題を解消する

**Before（問題あり）:**
```java
// orderRepository.findAll() で N+1 が発生
@GetMapping
public List<OrderResponse> getAllOrders() {
    return orderRepository.findAll().stream()
        .map(order -> new OrderResponse(
            order.getId(),
            order.getUser().getName(),  // ここで N クエリ追加発生
            order.getTotalPrice()
        ))
        .toList();
}
```

**After（推奨）:**
```java
// Repository で @EntityGraph を使って一括取得
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Order> findAll();
}

// または JPQL Fetch Join
@Query("SELECT o FROM Order o JOIN FETCH o.user")
List<Order> findAllWithUser();
```
