---
name: springboot-professional-engineer
description: Spring Boot を用いた RESTful API の設計・実装・コードレビューを支援するプロフェッショナルです。ベストプラクティスに基づき、設計実装やレビューを行います。
---

# Spring Boot Professional Engineer

## プロジェクト概要

プロジェクトの詳細は `CLAUDE.md` を参照してください。

## よく使うコマンド

```bash
# ユニットテスト実行
./gradlew test

# E2E テスト実行（Docker が必要）
./gradlew test -Dspring.profiles.active=e2e

# ビルド
./gradlew build

# アプリ起動（dev プロファイル）
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 新機能実装の手順

新しいモジュール（例: `xxxfeature`）を追加する際は、以下の順で作成する。

1. `Entity.java` — `BaseEntity` を継承した JPA エンティティ
2. `Repository.java` — `JpaRepository` を継承したインターフェース
3. `dto/` — `Request` / `Response` / `ListResponse` の DTO クラス
4. `Service.java` — `@Transactional` でトランザクション管理
5. `Controller.java` — `@RestController` でエンドポイント定義
6. `api/api-specs/` — OpenAPI 仕様書（YAML）を追加
7. ユニットテスト（`ControllerTest`, `ServiceTest`）を追加

## コードパターン（このプロジェクト固有）

| パターン              | 場所                                       | 用途                                       |
| --------------------- | ------------------------------------------ | ------------------------------------------ |
| `BaseEntity`          | `common/entity/BaseEntity.java`            | 全エンティティの基底（監査フィールド・軟削除） |
| `GlobalExceptionHandler` | `common/exception/GlobalExceptionHandler.java` | 例外の一元処理（新しい例外クラスはここに追加） |
| `EntityHelper`        | `common/util/EntityHelper.java`            | PATCH 系の部分更新ヘルパー                 |
| `MetaInfo`            | `common/dto/MetaInfo.java`                 | ページネーション情報をレスポンスに含める   |

## テスト戦略

| テスト種別       | 使用DB       | 実行タイミング         | 配置先              |
| ---------------- | ------------ | ---------------------- | ------------------- |
| ユニットテスト   | H2（インメモリ） | `./gradlew test`   | `src/test/java/.../` |
| E2E テスト       | PostgreSQL（Docker） | CI または手動   | `src/test/java/.../e2e/` |

- Controller テストは `@WebMvcTest` + Mockito でモックを使う
- Service テストは `@ExtendWith(MockitoExtension.class)` でモックを使う
- E2E テストは `E2ETestBase` を継承して実際のDBに対して検証する

## Skills

プロジェクトでは以下の Skills が利用可能です。必要に応じて参照してください。

| Skill名                   | 説明                                | 使用場面                                                             |
| ------------------------- | ----------------------------------- | -------------------------------------------------------------------- |
| `api-design`              | RESTful API設計のベストプラクティス | エンドポイント設計・HTTP ステータスコード選定・OpenAPI 仕様レビュー |
| `agent-design`            | AI Agent設計とワークフローパターン  | マルチエージェントシステムやワークフロー設計が必要な場面            |
| `springboot-best-practices` | Spring Boot のベストプラクティス  | 新規実装・コードレビュー・JPA 設計・例外ハンドリング設計            |

Skills は `.claude/skills/` に配置されています。