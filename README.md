<img src="https://capsule-render.vercel.app/api?type=waving&height=250&color=0:bb6688,100:bb88ee&text=Coming%20of%20age%20SToRY&fontAlign=45&fontAlignY=45&fontSize=50&animation=fadeIn&desc=Journey%20to%20Professional&descAlign=75&descAlignY=60&fontColor=f5f5f7&descSize=-1&reversal=true&section=header&textBg=false" />

# **_prsk music list api_**

![welcome comment](https://readme-typing-svg.herokuapp.com?color=%23884499&width=500&lines=認められたい。+成長のため。+わたしは作り続ける。;――――わたしの、ただのエゴだよ――――;)


#### **_Tech Stack_**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=fff)](#)
[![Java](https://img.shields.io/badge/Java-%23ED8B00.svg?logo=openjdk&logoColor=white)](#)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=fff)](#)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-6BA539?logo=openapiinitiative&logoColor=white)](#)

### 💫 **_API Document_** 💫![Leo/need-divider](https://capsule-render.vercel.app/api?type=rect&height=2&color=0:3367cc,100:f5f5f7)

API仕様書は**GithubPages**にデプロイしてある

[Jump to Github Pages ➣](https://narumikr.github.io/prsk-backend-SToRY/)

### 🎀 **_Command List_** 🎀![Nightcord-at25-divider](https://capsule-render.vercel.app/api?type=rect&height=2&color=0:884499,100:f5f5f7)

#### **Core Commands**

| コマンド                  | 説明                                     |
| ------------------------- | ---------------------------------------- |
| `./gradlew build -x test` | プロジェクトをビルド（テストをスキップ） |
| `./gradlew bootRun`       | 開発サーバーを起動（localhost:8080）     |
| `./gradlew clean`         | ビルド成果物をクリア                     |

#### **Test Commands**

| コマンド             | 説明                           | データベース        |
| -------------------- | ------------------------------ | ------------------- |
| `./gradlew testUnit` | ユニットテストのみ実行         | H2 (in-memory)      |
| `./gradlew testE2e`  | E2Eテストのみ実行              | PostgreSQL (Docker) |
| `./gradlew test`     | 全テスト実行（ユニット + E2E） | 両方                |

#### **Docker Commands**

| コマンド                                        | 説明                        |
| ----------------------------------------------- | --------------------------- |
| `./gradlew dockerComposeUp`                     | E2E用PostgreSQLコンテナ起動 |
| `./gradlew dockerComposeDown`                   | E2E用PostgreSQLコンテナ停止 |
| `docker compose -f docker-compose.e2e.yml down` | Docker composeで手動停止    |

