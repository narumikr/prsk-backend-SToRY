# E2Eテスト設計仕様書

## 概要

このドキュメントは、プロジェクトのE2E（End-to-End）テストを作成する際の設計方針、テストケースの粒度、命名規則、実装パターンを定義したものです。
新しいリソースのE2Eテストを実装する際は、この仕様書に従って同じ品質・粒度でテストを作成してください。

### E2Eテストの目的

- **統合検証**: Controller → Service → Repository → Database の全レイヤーを通した動作確認
- **API仕様の検証**: OpenAPI仕様書に定義されたエンドポイントの動作検証
- **実際の環境に近いテスト**: 実際のデータベース（PostgreSQL）を使用した動作確認

## 目次

1. [テスト方針](#テスト方針)
2. [テスト構成](#テスト構成)
3. [基底クラス](#基底クラス)
4. [テストケースの粒度](#テストケースの粒度)
5. [テストケース命名規則](#テストケース命名規則)
6. [テストデータの作成方針](#テストデータの作成方針)
7. [実装パターン](#実装パターン)
8. [カバレッジ目標](#カバレッジ目標)
9. [参考実装例](#参考実装例)
10. [チェックリスト](#チェックリスト)

---

## テスト方針

### 基本方針

- **実環境に近い検証**: モックを使用せず、実際のDBに接続してテスト
- **APIレベルでの検証**: HTTPリクエスト/レスポンスを通して動作確認
- **OpenAPI仕様書との整合性**: 定義された全てのステータスコードをカバー
- **テストデータの独立性**: UUIDを使用して各テストで一意のデータを生成
- **論理削除の検証**: 削除後のデータが一覧から除外されることを確認

### UnitTestとの違い

| 観点           | UnitTest                     | E2Eテスト                                  |
| -------------- | ---------------------------- | ------------------------------------------ |
| 目的           | 単一レイヤーの動作検証       | 全レイヤーを通した統合検証                 |
| DB接続         | H2（インメモリ）             | PostgreSQL（Testcontainersまたは専用環境） |
| モック         | Service/Repositoryをモック化 | モックなし（実際の実装を使用）             |
| 実行速度       | 高速                         | 比較的低速                                 |
| 実行タイミング | ビルド時毎回                 | CI/CDパイプラインまたは任意のタイミング    |

### テストプロファイル

E2Eテストは `e2e` プロファイルで実行されます：

```properties
# application-e2e.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/testdb
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## テスト構成

### ディレクトリ構造

```
src/test/java/com/example/untitled/
├── e2e/
│   ├── E2ETestBase.java           # 全E2Eテストの基底クラス
│   ├── {Resource}E2ETest.java     # 各リソースのE2Eテスト
│   └── ...
├── {resource}/
│   ├── {Resource}ControllerTest.java
│   └── {Resource}ServiceTest.java
├── TEST_DESIGN_SPECIFICATION_FOR_UNITTEST.md
└── TEST_DESIGN_SPECIFICATION_FOR_E2E.md (このファイル)
```

### 使用フレームワーク・ライブラリ

- **JUnit 5**: テスティングフレームワーク
- **Spring Boot Test**: `@SpringBootTest` による統合テスト
- **TestRestTemplate**: HTTPクライアント
- **AssertJ/JUnit Assertions**: アサーション

---

## 基底クラス

### E2ETestBase

全てのE2Eテストクラスは `E2ETestBase` を継承します。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Tag("e2e")
public abstract class E2ETestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String getBaseUrl() {
        // TestRestTemplateはcontext-path（/api/v1）を自動で付加するため空文字を返す
        return "";
    }
}
```

### アノテーション説明

| アノテーション                                  | 説明                                    |
| ----------------------------------------------- | --------------------------------------- |
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | 実際のサーバーをランダムポートで起動    |
| `@ActiveProfiles("e2e")`                        | e2eプロファイルを有効化                 |
| `@Tag("e2e")`                                   | JUnit 5のタグ（テストフィルタリング用） |

---

## テストケースの粒度

### OpenAPI仕様書との対応

E2EテストはOpenAPI仕様書に定義された全てのレスポンスステータスコードをカバーします。

### 1. GET（一覧取得）エンドポイント `/resources`

| #   | テストケース                           | 期待する結果                | OpenAPI対応 |
| --- | -------------------------------------- | --------------------------- | ----------- |
| 1   | 正常系: データが存在する場合の一覧取得 | 200 OK、items配列とmeta情報 | 200         |
| 2   | 正常系: ページネーションパラメータ指定 | 200 OK、指定ページのデータ  | 200         |

### 2. POST（作成系）エンドポイント `/resources`

| #   | テストケース                     | 期待する結果                            | OpenAPI対応 |
| --- | -------------------------------- | --------------------------------------- | ----------- |
| 1   | 正常系: 全フィールド指定で作成   | 201 Created、リソース情報               | 201         |
| 2   | 正常系: 必須フィールドのみで作成 | 201 Created、オプションフィールドはnull | 201         |
| 3   | 異常系: ユニーク制約違反         | 409 Conflict、エラー情報                | 409         |
| 4   | 異常系: 必須フィールドがblank    | 400 Bad Request                         | 400         |
| 5   | 異常系: 必須フィールドがnull     | 400 Bad Request                         | 400         |
| 6   | 異常系: 文字数制限超過           | 400 Bad Request                         | 400         |

### 3. PUT（更新系）エンドポイント `/resources/{id}`

| #   | テストケース                                 | 期待する結果                       | OpenAPI対応 |
| --- | -------------------------------------------- | ---------------------------------- | ----------- |
| 1   | 正常系: 全フィールド更新                     | 200 OK、更新後のリソース情報       | 200         |
| 2   | 正常系: 一部フィールドのみ更新（部分更新）   | 200 OK、他フィールドは元の値を維持 | 200         |
| 3   | 異常系: 存在しないID                         | 404 Not Found                      | 404         |
| 4   | 異常系: ユニーク制約違反（他リソースと重複） | 409 Conflict                       | 409         |
| 5   | 異常系: 文字数制限超過                       | 400 Bad Request                    | 400         |

### 4. DELETE（削除系）エンドポイント `/resources/{id}`

| #   | テストケース                                 | 期待する結果                       | OpenAPI対応  |
| --- | -------------------------------------------- | ---------------------------------- | ------------ |
| 1   | 正常系: 削除成功                             | 204 No Content                     | 204          |
| 2   | 異常系: 存在しないID                         | 404 Not Found                      | 404          |
| 3   | 正常系: 削除後に一覧から除外されることを確認 | 削除したリソースが一覧に含まれない | 論理削除検証 |

---

## テストケース命名規則

### 基本フォーマット

```
{メソッド名}{Success|Error}[_{条件}]
```

### @DisplayName のフォーマット

```
{Success|Error} - {説明（英語）}
```

### 命名パターン一覧

| パターン                                 | 説明               | 例                                                 |
| ---------------------------------------- | ------------------ | -------------------------------------------------- |
| `{method}Success`                        | 正常系の基本ケース | `createArtistSuccess`                              |
| `{method}Success_{detail}`               | 正常系の特定条件   | `createArtistSuccess_withRequiredFieldsOnly`       |
| `{method}Error_with{ErrorType}`          | 異常系の基本ケース | `createArtistError_withConflict`                   |
| `{method}Error_with{ErrorType}_{detail}` | 異常系の詳細ケース | `createArtistError_withBadRequest_blankArtistName` |

### DisplayName例

```java
@Test
@DisplayName("Success - creates artist with all fields")
void createArtistSuccess() { ... }

@Test
@DisplayName("Error - 409 Conflict when artistName already exists")
void createArtistError_withConflict() { ... }

@Test
@DisplayName("Error - 400 Bad Request when artistName is blank")
void createArtistError_withBadRequest_blankArtistName() { ... }
```

---

## テストデータの作成方針

### 1. 一意性の確保

テストデータの一意性を確保するためにUUIDを使用します。

```java
private String uniqueArtistName() {
    return "Artist-" + UUID.randomUUID().toString().substring(0, 8);
}
```

### 2. ヘルパーメソッドの作成

テストデータ作成の共通処理はヘルパーメソッドとして定義します。

```java
/**
 * テスト用のアーティストを作成するヘルパーメソッド
 */
private ArtistResponse createArtist(String artistName, String unitName, String content) {
    ArtistRequest request = new ArtistRequest();
    request.setArtistName(artistName);
    request.setUnitName(unitName);
    request.setContent(content);

    ResponseEntity<ArtistResponse> response = restTemplate.postForEntity(
            getBaseUrl() + ARTISTS_PATH,
            request,
            ArtistResponse.class
    );
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    return response.getBody();
}

/**
 * 必須フィールドのみでアーティストを作成
 */
private ArtistResponse createArtist(String artistName) {
    return createArtist(artistName, null, null);
}
```

### 3. バリデーションエラー用データ

```java
// 文字数超過（50文字制限の場合）
String tooLongName = "A".repeat(51);

// 空文字
String blankValue = "";

// null値
String nullValue = null;
```

### 4. 存在しないIDの指定

存在しないIDをテストする際は `Long.MAX_VALUE` を使用します。

```java
// 存在しないIDでの操作
ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        getBaseUrl() + ARTISTS_PATH + "/" + Long.MAX_VALUE,
        HttpMethod.PUT,
        new HttpEntity<>(updateRequest),
        ErrorResponse.class
);
assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
```

---

## 実装パターン

### テストクラスの構造

`@Nested` クラスを使用してエンドポイントごとにテストをグループ化します。

```java
@DisplayName("{Resource} E2E Tests")
class {Resource}E2ETest extends E2ETestBase {

    private static final String {RESOURCES}_PATH = "/{resources}";

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String unique{Resource}Name() { ... }

    private {Resource}Response create{Resource}(...) { ... }

    // ========================================================================
    // GET /{resources} - List {Resources}
    // ========================================================================

    @Nested
    @DisplayName("GET /{resources}")
    class Get{Resources} {
        @Test
        @DisplayName("Success - returns list with created resources")
        void get{Resources}Success() { ... }
    }

    // ========================================================================
    // POST /{resources} - Create {Resource}
    // ========================================================================

    @Nested
    @DisplayName("POST /{resources}")
    class Create{Resource} {
        @Test
        @DisplayName("Success - creates resource with all fields")
        void create{Resource}Success() { ... }
    }

    // ========================================================================
    // PUT /{resources}/{id} - Update {Resource}
    // ========================================================================

    @Nested
    @DisplayName("PUT /{resources}/{id}")
    class Update{Resource} {
        @Test
        @DisplayName("Success - updates all fields")
        void update{Resource}Success() { ... }
    }

    // ========================================================================
    // DELETE /{resources}/{id} - Delete {Resource}
    // ========================================================================

    @Nested
    @DisplayName("DELETE /{resources}/{id}")
    class Delete{Resource} {
        @Test
        @DisplayName("Success - deletes resource and returns 204")
        void delete{Resource}Success() { ... }
    }
}
```

### HTTPメソッドごとの実装パターン

#### GET（一覧取得）

```java
@Test
@DisplayName("Success - returns list with created resources")
void get{Resources}Success() {
    // Arrange: テストデータ作成
    String name1 = unique{Resource}Name();
    String name2 = unique{Resource}Name();
    create{Resource}(name1, "Unit1", "Content1");
    create{Resource}(name2, "Unit2", "Content2");

    // Act
    ResponseEntity<{Resource}ListResponse> response = restTemplate.getForEntity(
            getBaseUrl() + {RESOURCES}_PATH,
            {Resource}ListResponse.class
    );

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getItems());
    assertNotNull(response.getBody().getMeta());
    assertTrue(response.getBody().getItems().size() >= 2);
}
```

#### POST（作成）

```java
@Test
@DisplayName("Success - creates resource with all fields")
void create{Resource}Success() {
    // Arrange
    {Resource}Request request = new {Resource}Request();
    request.setName(unique{Resource}Name());
    request.setOptionalField("value");

    // Act
    ResponseEntity<{Resource}Response> response = restTemplate.postForEntity(
            getBaseUrl() + {RESOURCES}_PATH,
            request,
            {Resource}Response.class
    );

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getId());
    assertEquals(request.getName(), response.getBody().getName());
    assertNotNull(response.getBody().getAuditInfo());
}

@Test
@DisplayName("Error - 409 Conflict when name already exists")
void create{Resource}Error_withConflict() {
    // Arrange: 先にリソースを作成
    String existingName = unique{Resource}Name();
    create{Resource}(existingName);

    // 同じ名前で再度作成を試みる
    {Resource}Request duplicateRequest = new {Resource}Request();
    duplicateRequest.setName(existingName);

    // Act
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            getBaseUrl() + {RESOURCES}_PATH,
            duplicateRequest,
            ErrorResponse.class
    );

    // Assert
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(409, response.getBody().getStatusCode());
}
```

#### PUT（更新）

```java
@Test
@DisplayName("Success - updates all fields")
void update{Resource}Success() {
    // Arrange: リソースを作成
    {Resource}Response created = create{Resource}(unique{Resource}Name(), "OldValue", "OldContent");

    Optional{Resource}Request updateRequest = new Optional{Resource}Request();
    updateRequest.setName(unique{Resource}Name());
    updateRequest.setOptionalField("NewValue");

    // Act
    ResponseEntity<{Resource}Response> response = restTemplate.exchange(
            getBaseUrl() + {RESOURCES}_PATH + "/" + created.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            {Resource}Response.class
    );

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(created.getId(), response.getBody().getId());
    assertEquals(updateRequest.getName(), response.getBody().getName());
}

@Test
@DisplayName("Error - 404 Not Found when ID does not exist")
void update{Resource}Error_withNotFound() {
    // Arrange
    Optional{Resource}Request updateRequest = new Optional{Resource}Request();
    updateRequest.setName(unique{Resource}Name());

    // Act
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            getBaseUrl() + {RESOURCES}_PATH + "/" + Long.MAX_VALUE,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            ErrorResponse.class
    );

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(404, response.getBody().getStatusCode());
}
```

#### DELETE（削除）

```java
@Test
@DisplayName("Success - deletes resource and returns 204")
void delete{Resource}Success() {
    // Arrange: リソースを作成
    {Resource}Response created = create{Resource}(unique{Resource}Name());

    // Act
    ResponseEntity<Void> response = restTemplate.exchange(
            getBaseUrl() + {RESOURCES}_PATH + "/" + created.getId(),
            HttpMethod.DELETE,
            null,
            Void.class
    );

    // Assert
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    // Verify: 再度削除を試みると404が返る（論理削除済み）
    ResponseEntity<ErrorResponse> verifyResponse = restTemplate.exchange(
            getBaseUrl() + {RESOURCES}_PATH + "/" + created.getId(),
            HttpMethod.DELETE,
            null,
            ErrorResponse.class
    );
    assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
}

@Test
@DisplayName("Success - deleted resource does not appear in list")
void delete{Resource}Success_notInList() {
    // Arrange: 作成して削除
    String resourceName = unique{Resource}Name();
    {Resource}Response created = create{Resource}(resourceName);

    restTemplate.exchange(
            getBaseUrl() + {RESOURCES}_PATH + "/" + created.getId(),
            HttpMethod.DELETE,
            null,
            Void.class
    );

    // Act: 一覧取得
    ResponseEntity<{Resource}ListResponse> listResponse = restTemplate.getForEntity(
            getBaseUrl() + {RESOURCES}_PATH,
            {Resource}ListResponse.class
    );

    // Assert: 削除したリソースが含まれていないこと
    assertEquals(HttpStatus.OK, listResponse.getStatusCode());
    boolean resourceFound = listResponse.getBody().getItems().stream()
            .anyMatch(r -> r.getId().equals(created.getId()));
    assertFalse(resourceFound, "Deleted resource should not appear in list");
}
```

---

## カバレッジ目標

### 目標値

E2Eテストでは、OpenAPI仕様書に定義された全てのエンドポイントとステータスコードをカバーすることを目標とします。

### 必須カバー項目

1. **全エンドポイント**: GET, POST, PUT, DELETE
2. **全レスポンスステータス**: 200, 201, 204, 400, 404, 409
3. **バリデーションルール**: 必須フィールド、文字数制限、ユニーク制約
4. **論理削除の動作**: 削除後に一覧から除外されること

---

## 参考実装例

### 完全な実装例

- [E2ETestBase.java](./java/com/example/untitled/e2e/E2ETestBase.java)
- [ArtistE2ETest.java](./java/com/example/untitled/e2e/ArtistE2ETest.java)

### 実装統計（Artistの例）

- **ArtistE2ETest**: 15テストケース
  - GET: 2ケース（一覧取得、ページネーション）
  - POST: 6ケース（正常系2、異常系4）
  - PUT: 5ケース（正常系2、異常系3）
  - DELETE: 3ケース（正常系2、異常系1）

---

## チェックリスト

新しいリソースのE2Eテストを実装する際は、以下のチェックリストを使用してください：

### セットアップ

- [ ] `E2ETestBase` を継承したテストクラスを作成
- [ ] `@DisplayName("{Resource} E2E Tests")` を付与
- [ ] リソースパスの定数を定義
- [ ] ヘルパーメソッドを作成（unique名生成、リソース作成）

### GET（一覧取得）

- [ ] 正常系: データが存在する場合の一覧取得
- [ ] 正常系: ページネーションパラメータ指定

### POST（作成）

- [ ] 正常系: 全フィールド指定で作成
- [ ] 正常系: 必須フィールドのみで作成
- [ ] 異常系: ユニーク制約違反（409 Conflict）
- [ ] 異常系: 必須フィールドがblank（400 Bad Request）
- [ ] 異常系: 必須フィールドがnull（400 Bad Request）
- [ ] 異常系: 文字数制限超過（400 Bad Request）

### PUT（更新）

- [ ] 正常系: 全フィールド更新
- [ ] 正常系: 一部フィールドのみ更新（部分更新）
- [ ] 異常系: 存在しないID（404 Not Found）
- [ ] 異常系: ユニーク制約違反（409 Conflict）
- [ ] 異常系: 文字数制限超過（400 Bad Request）

### DELETE（削除）

- [ ] 正常系: 削除成功（204 No Content）
- [ ] 異常系: 存在しないID（404 Not Found）
- [ ] 正常系: 削除後に一覧から除外されることを確認

---

## テスト実行コマンド

```bash
# E2Eテストのみ実行
./gradlew test --tests "com.example.untitled.e2e.*"

# 特定リソースのE2Eテストのみ実行
./gradlew test --tests "com.example.untitled.e2e.{Resource}E2ETest"

# 特定のテストメソッドのみ実行
./gradlew test --tests "com.example.untitled.e2e.ArtistE2ETest.Create*.create*"
```

### タグによる実行（CI/CD向け）

```bash
# e2eタグのついたテストのみ実行
./gradlew test -PincludeTags=e2e

# e2eタグのついたテストを除外
./gradlew test -PexcludeTags=e2e
```

---

## トラブルシューティング

### データベース接続エラー

- `application-e2e.properties` のDB接続設定を確認
- E2E用のデータベースが起動していることを確認

### テストデータの重複

- `unique{Resource}Name()` メソッドでUUIDを使用していることを確認
- テスト間で共有されるデータがないことを確認

### ポート競合

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` によりランダムポートが使用されるため、通常は発生しない
- 他のアプリケーションがポートを占有している場合は停止する

### タイムアウト

- DBアクセスが遅い場合はコネクションプールの設定を見直す
- Testcontainersを使用している場合は起動時間を考慮する

---

## 更新履歴

| 日付       | バージョン | 変更内容                                   | 作成者 |
| ---------- | ---------- | ------------------------------------------ | ------ |
| 2025-12-25 | 1.0.0      | 初版作成（Artistリソースの実装を基に作成） | -      |

---

このドキュメントに従うことで、プロジェクト全体で統一された品質のE2Eテストコードを維持できます。
