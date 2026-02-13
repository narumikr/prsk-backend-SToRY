package com.example.untitled.prskmusic.dto;

import com.example.untitled.artist.Artist;
import com.example.untitled.common.dto.AuditInfo;
import com.example.untitled.prskmusic.PrskMusic;
import com.example.untitled.prskmusic.enums.MusicType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PrskMusicResponse {

    /** ID **/
    private Long id;

    /** 楽曲タイトル **/
    private String title;

    /** アーティスト名 **/
    private String artistName;

    /** ユニット名 **/
    private String unitName;

    /** コンテンツ名 **/
    private String content;

    /** 楽曲タイプ **/
    private MusicType musicType;

    /** 書き下ろし曲かどうか **/
    private Boolean specially;

    /** 作詞者名 **/
    private String lyricsName;

    /** 作曲者名 **/
    private String musicName;

    /** ゲストメンバー名 **/
    private String featuring;

    /** YouTubeリンク **/
    private String youtubeLink;

    /** 監査情報 **/
    private AuditInfo auditInfo;

    public static PrskMusicResponse from(PrskMusic prskMusic) {
        Artist artist = prskMusic.getArtist();
        boolean isArtistDeleted = artist.isDeleted();

        return new PrskMusicResponse(
                prskMusic.getId(),
                prskMusic.getTitle(),
                isArtistDeleted ? "Unknown" : artist.getArtistName(),
                isArtistDeleted ? null : artist.getUnitName(),
                isArtistDeleted ? null : artist.getContent(),
                prskMusic.getMusicType(),
                prskMusic.getSpecially(),
                prskMusic.getLyricsName(),
                prskMusic.getMusicName(),
                prskMusic.getFeaturing(),
                prskMusic.getYoutubeLink(),
                AuditInfo.from(prskMusic)
        );
    }
}
