# プロセカ楽曲リストテーブル定義書

## プロセカ楽曲マスタ(m_prsk_music)

|  No.  | 論理名         | 物理名       | データ型     | Nullable |  PK   |  UK   |  FK   | デフォルト値 | 説明                     |
| :---: | -------------- | ------------ | ------------ | :------: | :---: | :---: | :---: | ------------ | ------------------------ |
|   1   | 楽曲ID         | id           | BIGSERIAL    | NOT NULL |   ○   |   -   |   -   | -            | 内部識別用               |
|   2   | タイトル       | title        | VARCHAR(30)  | NOT NULL |   -   |   ○   |   -   | -            | 楽曲タイトル             |
|   3   | アーティストID | artist_id    | BIGINT       | NOT NULL |   -   |   -   |   ○   | -            | m_prsk_artist.id         |
|   4   | 楽曲タイプ     | music_type   | ENUM         | NOT NULL |   -   |   ○   |   -   | -            | 楽曲の種類(music_types)  |
|   5   | 書き下ろし楽曲 | specially    | BOOLEAN      |   YES    |   -   |   -   |   -   | -            | プロセカ書き下ろし楽曲か |
|   6   | 作詞           | lyrics_name  | VARCHAR(50)  |   YES    |   -   |   -   |   -   | -            | 作詞者名                 |
|   7   | 作曲           | music_name   | VARCHAR(50)  |   YES    |   -   |   -   |   -   | -            | 作曲者名                 |
|   8   | ゲスト出演     | featuring    | VARCHAR(10)  |   YES    |   -   |   -   |   -   | -            | feat. キャラクター名     |
|   9   | YouTube URL    | youtube_link | VARCHAR(100) | NOT NULL |   -   |   -   |   -   | -            | YouTubeの動画URL         |

### music_typesの値
|  値   | 名称       | 説明             |
| :---: | ---------- | ---------------- |
|   0   | オリジナル | オリジナル楽曲   |
|   1   | 3DMV       | プロセカ3DMV楽曲 |
|   2   | 2DMV       | プロセカ2DMV楽曲 |

## アーティストマスタ(m_artists)

|  No.  | 論理名         | 物理名      | データ型    | Nullable |  PK   |  UK   |  FK   | デフォルト値 | 説明           |
| :---: | -------------- | ----------- | ----------- | :------: | :---: | :---: | :---: | ------------ | -------------- |
|   1   | アーティストID | id          | BIGSERIAL   | NOT NULL |   ○   |   -   |   -   | -            | 内部識別用     |
|   2   | アーティスト名 | artist_name | VARCHAR(50) | NOT NULL |   -   |   ○   |   -   | -            | アーティスト名 |
|   3   | ユニット名     | unit_name   | VARCHAR(25) |   YES    |   -   |   -   |   -   | -            | ユニット名     |
|   4   | コンテンツ     | content     | VARCHAR(20) |   YES    |   -   |   -   |   -   | -            | コンテンツ名   |

## ユーザーマスタ(m_users)

|  No.  | 論理名     | 物理名    | データ型    | Nullable |  PK   |  UK   |  FK   | デフォルト値 | 説明       |
| :---: | ---------- | --------- | ----------- | :------: | :---: | :---: | :---: | ------------ | ---------- |
|   1   | ユーザーID | id        | BIGSERIAL   | NOT NULL |   ○   |   -   |   -   | -            | 内部識別用 |
|   2   | ユーザー名 | user_name | VARCHAR(20) | NOT NULL |   -   |   ○   |   -   | -            | ユーザー名 |
|   3   | パスワード | password  | VARCHAR(20) | NOT NULL |   -   |   -   |   -   | -            | パスワード |

## プロセカプレイリストテーブル(t_prsk_playlist)
|  No.  | 論理名         | 物理名        | データ型     | Nullable |  PK   |  UK   |  FK   | デフォルト値 | 説明               |
| :---: | -------------- | ------------- | ------------ | :------: | :---: | :---: | :---: | ------------ | ------------------ |
|   1   | プレイリストID | id            | BIGSERIAL    | NOT NULL |   ○   |   -   |   -   | -            | 内部識別用         |
|   2   | プレイリスト名 | playlist_name | VARCHAR(100) | NOT NULL |   -   |   -   |   -   | -            | プレイリスト名称   |
|   3   | ユーザーID     | user_id       | BIGINT       | NOT NULL |   -   |   -   |   ○   | -            | m_user.id          |
|   4   | 説明           | description   | VARCHAR(250) |   YES    |   -   |   -   |   -   | -            | プレイリストの説明 |

## プレイリスト楽曲テーブル(t_playlist_music)

| No. | 論理名         | 物理名      | データ型  | Nullable |  PK   |  UK   |  FK   | デフォルト値 | 説明                 |
| --- | -------------- | ----------- | --------- | :------: | :---: | :---: | :---: | ------------ | -------------------- |
| 1   | ID             | id          | BIGSERIAL | NOT NULL |   ○   |   -   |   -   | -            | 内部識別用           |
| 2   | プレイリストID | playlist_id | BIGINT    | NOT NULL |   -   |   -   |   ○   | -            | t_prsk_playlist.id   |
| 3   | 楽曲ID         | music_id    | BIGINT    | NOT NULL |   -   |   -   |   ○   | -            | m_prsk_music.id      |
| 4   | 表示順序       | sort_order  | INTEGER   | NOT NULL |   -   |   -   |   -   | -            | プレイリスト内の順序 |

### 複合ユニーク制約
- (playlist_id, music_id) - 同じ楽曲の重複登録防止
- (playlist_id, sort_order) - 同じ順序番号の重複防止

## 共通項目

|  No.  | 論理名     | 物理名     | データ型      | Nullable |  PK   |  UK   |  FK   | デフォルト値      | 説明           |
| :---: | ---------- | ---------- | ------------ | :------: | :---: | :---: | :---: | ----------------- | -------------- |
|   1   | 作成日     | created_at | TIMESTAMPTZ  | NOT NULL |   -   |   -   |   -   | CURRENT_TIMESTAMP | レコード作成日 |
|   2   | 作成者     | created_by | VARCHAR(20)  | NOT NULL |   -   |   -   |   -   | guest             | レコード作成者 |
|   3   | 更新日     | updated_at | TIMESTAMPTZ  | NOT NULL |   -   |   -   |   -   | CURRENT_TIMESTAMP | レコード更新日 |
|   4   | 更新者     | updated_by | VARCHAR(20)             | NOT NULL |   -   |   -   |   -   | guest             | レコード更新者 |
|   5   | 削除フラグ | is_deleted | BOOLEAN                 | NOT NULL |   -   |   -   |   -   | FALSE             | 削除フラグ     |