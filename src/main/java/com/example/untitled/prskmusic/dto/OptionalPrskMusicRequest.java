package com.example.untitled.prskmusic.dto;

import com.example.untitled.prskmusic.enums.MusicType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * プロセカ楽曲マスタAPIリクエスト for PUT
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OptionalPrskMusicRequest {

    /** 楽曲タイトル **/
    @Size(max = 30, message = "楽曲タイトルは30文字以内で入力してください。 - Please enter the music title within 30 characters.")
    private String title;

    /** アーティストID **/
    private Long artistId;

    /** 楽曲タイプ **/
    private MusicType musicType;

    /** 書き下ろし楽曲かどうか **/
    private Boolean specially;

    /** 作詞者名 **/
    @Size(max = 50, message = "作詞者名は50文字以内で入力してください。 - Please enter the lyrics name within 50 characters.")
    private String lyricsName;

    /** 作曲者名 **/
    @Size(max = 50, message = "作曲者名は50文字以内で入力してください。 - Please enter the music name within 50 characters.")
    private String musicName;

    /** ゲストメンバー **/
    @Size(max = 10, message = "ゲストメンバー名は10文字以内で入力してください。 - Please enter the featuring within 10 characters.")
    private String featuring;

    /** YouTube URL **/
    @Size(max = 100, message = "YouTubeのURLは100文字以内で入力してください。 - Please enter the YouTube link within 100 characters.")
    private String youtubeLink;
}
