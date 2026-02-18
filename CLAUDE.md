# Claude Code プロジェクトのガイドライン

## プロジェクト概要

本プロジェクトでは、プロジェクトセカイの楽曲情報およびアーティスト情報を管理するための RESTful API を提供します。

## 基本ルール

### 使用言語

ユーザーとの会話は日本語で行ってください。

## ドキュメント記述規約

### 技術用語の扱い

技術用語を不用意に日本語変換しないこと。

* 技術用語は原則として英語のまま使用すること
* 日本語訳が分かりにくい場合は、英語表記を優先すること

### 図の記述

* 図を記述する場合は、mermaidを使用すること

### 句読点の使用

日本語文章には`.`（ピリオド）ではなく`。`（句点）を使うこと。

* 日本語の文末には必ず`。`を使用すること
* コード中のコメントに`。`や`.`は不要
* 英語の文章では`.`を使用すること

## 技術スタック

### コア技術

| カテゴリ           | 技術                         | バージョン | 用途                     |
| ------------------ | ---------------------------- | ---------- | ------------------------ |
| **言語**           | Java                         | 21         | ビルドシステム           |
| **フレームワーク** | Spring Boot                  | 3.5.6      | REST API フレームワーク  |
| **依存管理**       | Spring Dependency Management | 1.1.7      | ライブラリバージョン管理 |
| **ビルドツール**   | Gradle                       | 8.x+       | プロジェクトビルド       |

### 主要ライブラリ

| ライブラリ        | 用途                                       |
| ----------------- | ------------------------------------------ |
| Spring Data JPA   | ORM・データベースアクセス層                |
| Spring Web        | RESTful API 開発                           |
| Spring Validation | Jakarta Bean Validation（入力検証）        |
| PostgreSQL Driver | PostgreSQL データベース接続                |
| Lombok            | ボイラープレート削減（@Data, @Builder 等） |
| Spring DevTools   | 開発時ホットリロード                       |

### テスト関連

| ライブラリ               | 用途                                   |
| ------------------------ | -------------------------------------- |
| JUnit 5 / JUnit Platform | ユニットテスト・E2E テスト実行         |
| Mockito                  | モック・スタブ生成                     |
| Spring Boot Test         | Spring 統合テスト                      |
| H2 Database              | ユニットテスト用インメモリデータベース |
| PostgreSQL               | E2E テスト用データベース               |

### インフラストラクチャ

| サービス             | 用途                         | 設定                                             |
| -------------------- | ---------------------------- | ------------------------------------------------ |
| **PostgreSQL**       | 本番・E2E テストデータベース | ローカル: localhost:5432、E2E: localhost:5433    |
| **Docker & Compose** | E2E テスト環境の自動構築     | `docker-compose.e2e.yml` で管理                  |
| **GitHub Actions**   | CI/CD 自動化                 | unit-test.yml, e2e-test.yml, api-docs-deploy.yml |

### API 設定

| 項目             | 値                              |
| ---------------- | ------------------------------- |
| ベースパス       | `/btw-api/v1`                   |
| ポート           | `8080`                          |
| プロファイル     | dev（開発）、e2e（テスト）      |
| Database Dialect | PostgreSQL Dialect（Hibernate） |

## ディレクトリ構成

```
prsk-backend-SToRY/
├── .github/
│   └── workflows/              # CI/CD 自動化
│       ├── unit-test.yml
│       ├── e2e-test.yml
│       └── api-docs-deploy.yml
├── api/                        # OpenAPI 仕様書
│   ├── openapi.yaml
│   ├── api-specs/
│   │   ├── common.yaml
│   │   ├── hello-sekai.yaml
│   │   ├── mgt-artist.yaml
│   │   ├── mgt-prsk-music.yaml
│   │   └── mgt-user.yaml
│   └── scripts/
│       └── generate-api-docs.sh
├── src/main/java/com/example/untitled/
│   ├── UntitledApplication.java  # Spring Boot エントリーポイント
│   ├── artist/                   # アーティスト機能モジュール
│   │   ├── Artist.java
│   │   ├── ArtistController.java
│   │   ├── ArtistService.java
│   │   ├── ArtistRepository.java
│   │   └── dto/
│   │       ├── ArtistRequest.java
│   │       ├── ArtistResponse.java
│   │       ├── ArtistListResponse.java
│   │       └── OptionalArtistRequest.java
│   ├── user/                     # ユーザー機能モジュール
│   │   ├── User.java
│   │   ├── UserController.java
│   │   ├── UserService.java
│   │   ├── UserRepository.java
│   │   └── dto/
│   │       ├── UserRequest.java
│   │       ├── UserResponse.java
│   │       └── UserListResponse.java
│   ├── prskmusic/                # プロセカ楽曲機能モジュール
│   │   ├── PrskMusic.java
│   │   ├── PrskMusicController.java
│   │   ├── PrskMusicService.java
│   │   ├── PrskMusicRepository.java
│   │   ├── dto/
│   │   │   ├── PrskMusicRequest.java
│   │   │   ├── PrskMusicResponse.java
│   │   │   ├── PrskMusicListResponse.java
│   │   │   └── OptionalPrskMusicRequest.java
│   │   ├── converter/
│   │   │   └── MusicTypeConverter.java
│   │   └── enums/
│   │       └── MusicType.java
│   ├── system/                   # システム関連機能
│   │   ├── SystemController.java
│   │   └── HealthController.java
│   ├── common/                   # 共通コンポーネント
│   │   ├── entity/
│   │   │   └── BaseEntity.java   # JPA 基底エンティティ（監査フィールド、軟削除対応）
│   │   ├── exception/            # 例外ハンドリング
│   │   │   ├── GlobalExceptionHandler.java  # 一元化された例外処理
│   │   │   ├── BadRequestException.java
│   │   │   ├── DuplicationResourceException.java
│   │   │   └── UnauthorizedException.java
│   │   ├── dto/                  # 共通 DTO
│   │   │   ├── ErrorResponse.java
│   │   │   ├── ErrorResponseWithDetails.java
│   │   │   ├── ErrorDetails.java
│   │   │   ├── MetaInfo.java     # ページネーション情報
│   │   │   └── AuditInfo.java
│   │   └── util/                 # ユーティリティ
│   │       ├── EntityHelper.java   # 部分更新ヘルパー
│   │       └── UtilsFunction.java
│   └── config/
│       └── JpaConfig.java        # JPA 監査設定
├── src/main/resources/
│   ├── application.properties       # 基本設定
│   └── application-dev.properties   # 開発環境設定
├── src/test/java/com/example/untitled/
│   ├── UntitledApplicationTests.java
│   ├── artist/                   # アーティストテスト
│   │   ├── ArtistControllerTest.java
│   │   └── ArtistServiceTest.java
│   ├── user/                     # ユーザーテスト
│   │   └── UserServiceTest.java
│   ├── e2e/                      # E2E テスト
│   │   ├── ArtistE2ETest.java
│   │   └── E2ETestBase.java
│   └── system/
│       └── SystemControllerTest.java
├── src/test/resources/
│   ├── application.properties       # テスト基本設定
│   └── application-e2e.properties   # E2E テスト設定
├── build.gradle                     # Gradle ビルド設定
├── settings.gradle                  # Gradle プロジェクト設定
├── gradle/
│   └── wrapper/                     # Gradle Wrapper
├── docker-compose.e2e.yml           # E2E テスト用 PostgreSQL コンテナ
├── gradlew / gradlew.bat            # Gradle ラッパースクリプト
└── doc/                             # ドキュメント
    ├── spec/
    │   ├── database-specification.md
    │   └── entity-relationship-diagram.md
    └── ...
```

### 主要モジュール説明

| モジュール    | 説明                         | 主要ファイル                                             |
| ------------- | ---------------------------- | -------------------------------------------------------- |
| **artist**    | アーティスト情報管理         | Controller, Service, Repository, Entity                  |
| **user**      | ユーザー管理                 | Controller, Service, Repository, Entity                  |
| **prskmusic** | プロセカ楽曲情報管理         | Controller, Service, Repository, Entity, Enum, Converter |
| **system**    | ヘルスチェック等システム機能 | HealthController, SystemController                       |
| **common**    | 全モジュール共通部品         | BaseEntity, GlobalExceptionHandler, DTOs, Utilities      |
| **config**    | アプリケーション設定         | JpaConfig（JPA監査設定）                                 |

### レイヤーアーキテクチャ

```
Controller → Service → Repository → Entity → Database
   ↓
   ↓ Request/Response DTOs
   ↓
Client
```

- **Controller**: REST エンドポイント、リクエスト検証
- **Service**: ビジネスロジック、トランザクション管理
- **Repository**: Spring Data JPA インターフェース
- **Entity**: JPA エンティティ、ビジネスルール
- **DTO**: API リクエスト/レスポンス用オブジェクト
