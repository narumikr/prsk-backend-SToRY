---
name: springboot-best-practices
description: Spring Boot 3.x アプリケーションの設計・実装、または「Spring Boot アプリを設計して」「Spring Boot コードをレビューして」「Spring Boot で REST API を実装して」「JPA リポジトリを設定して」と依頼された時、もしくはレイヤードアーキテクチャ、DTO、例外処理、Spring Data JPA の実装時に使用する。
---

# Spring Boot 3.x ベストプラクティス

## 概要

Spring Boot 3.x アプリケーションの設計・実装において、以下を実現するためのガイドライン:

- **保守性の高いアーキテクチャ**: 責務が明確に分離されたレイヤー構成
- **安全で堅牢な REST API**: 適切なバリデーション・エラーハンドリング
- **効率的なデータアクセス**: N+1問題を避けた JPA 設計

### 対象範囲

- Spring Boot 3.x（Java 17+ / Jakarta EE ネームスペース）
- REST API バックエンド
- Spring Data JPA を使用したデータアクセス層

## 基本原則

### 1. レイヤー分離（Separation of Concerns）

責務をレイヤーで分割し、各層は隣接する層にのみ依存する:

```
Controller（HTTP） → Service（ビジネスロジック） → Repository（データアクセス）
```

### 2. 依存性の注入（DI）はコンストラクタ注入を使う

`@Autowired` フィールド注入は避け、コンストラクタ注入（または Lombok `@RequiredArgsConstructor`）を使用する。

### 3. エンティティを API レスポンスとして直接返さない

エンティティとDTOを分離する。エンティティの変更が API 仕様に影響しないようにする。

### 4. トランザクション境界は Service 層で管理する

`@Transactional` は Service クラスのメソッドに付与する。Controller や Repository には付与しない。

## コアベストプラクティス（優先順位順）

### 1. コンストラクタインジェクション

```java
// ❌ 避ける: フィールド注入
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

// ✅ 推奨: コンストラクタ注入（Lombokで簡略化）
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
```

### 2. DTO で API と Entity を分離

```java
// ✅ Record クラスで不変DTOを定義（Java 17+）
public record UserResponse(Long id, String name, String email) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
```

### 3. `@ControllerAdvice` でエラーハンドリングを一元化

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }
}
```

### 4. `@Transactional` の正しい使用

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)   // 参照系は readOnly = true
    public UserResponse findById(Long id) { ... }

    @Transactional                    // 更新系はデフォルト
    public UserResponse update(Long id, UpdateUserRequest request) { ... }
}
```

## クイックチェックリスト

実装時に確認する項目:

- [ ] `@Autowired` フィールド注入を使っていないか（コンストラクタ注入に変更）
- [ ] エンティティを直接 Controller から返していないか（DTO に変換）
- [ ] ビジネスロジックが Controller に書かれていないか（Service 層に移動）
- [ ] `@Transactional` が Service 層のメソッドに適切に付いているか
- [ ] 参照系メソッドに `readOnly = true` が付いているか
- [ ] `@ControllerAdvice` で例外を一元ハンドリングしているか
- [ ] Bean Validation（`@Valid`, `@NotNull` など）を使っているか
- [ ] N+1問題が発生していないか（`@EntityGraph` または Fetch Join で対処）
- [ ] パッケージ構成がレイヤーまたは機能で整理されているか

## 詳細リファレンス

より詳細な情報は以下のファイルを参照:

- **アーキテクチャ・DI設計**: `references/architecture.md`
  - レイヤードアーキテクチャの詳細
  - パッケージ構成パターン
  - `@Component` / `@Service` / `@Repository` の使い分け
  - 設定クラスの設計

- **REST API 実装**: `references/rest-api.md`
  - Controller 設計の詳細
  - リクエスト/レスポンス DTO パターン
  - Bean Validation の活用
  - 例外ハンドリングの詳細

- **データアクセス層**: `references/data-access.md`
  - Spring Data JPA リポジトリ設計
  - カスタムクエリの書き方
  - トランザクション管理の詳細
  - N+1問題と対処法

## 実装例（Before/After）

実際の改善例は `examples/before-after.md` を参照。
