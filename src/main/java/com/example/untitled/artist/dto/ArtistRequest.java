package com.example.untitled.artist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * アーティストマスタAPIリクエスト for POST
 */
@Getter
@Setter
@NoArgsConstructor
public class ArtistRequest {

    /** アーティスト名 **/
    @NotBlank(message = "アーティスト名は必須です。- The artist name is required.")
    @Size(max = 50, message = "アーティスト名は50文字以内で入力してください。 - Please enter the artist name within 50 characters.")
    private String artistName;

    /** ユニット名 **/
    @Size(max = 25, message = "ユニット名は25文字以内で入力してください。 - Please enter the artist name within 25 characters.")
    private String unitName;

    /** コンテンツ名 **/
    @Size(max = 20, message = "コンテンツ名は20文字以内で入力してください。 - Please enter the artist name within 20 characters.")
    private String content;
}
