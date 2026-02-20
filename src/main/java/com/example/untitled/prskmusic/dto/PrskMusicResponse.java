package com.example.untitled.prskmusic.dto;

import com.example.untitled.artist.Artist;
import com.example.untitled.common.dto.AuditInfo;
import com.example.untitled.prskmusic.PrskMusic;
import com.example.untitled.prskmusic.enums.MusicType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrskMusicResponse {

    /** ID **/
    private final Long id;

    /** 楽曲タイトル **/
    private final String title;

    /** アーティスト名 **/
    private final String artistName;

    /** ユニット名 **/
    private final String unitName;

    /** コンテンツ名 **/
    private final String content;

    /** 楽曲タイプ **/
    private final MusicType musicType;

    /** 書き下ろし曲かどうか **/
    private final Boolean specially;

    /** 作詞者名 **/
    private final String lyricsName;

    /** 作曲者名 **/
    private final String musicName;

    /** ゲストメンバー名 **/
    private final String featuring;

    /** YouTubeリンク **/
    private final String youtubeLink;

    /** 監査情報 **/
    private final AuditInfo auditInfo;

    public static PrskMusicResponse from(PrskMusic prskMusic) {
        Artist artist = prskMusic.getArtist();
        boolean isArtistDeleted = artist.isDeleted();

        return PrskMusicResponse.builder()
                .id(prskMusic.getId())
                .title(prskMusic.getTitle())
                .artistName(isArtistDeleted ? "Unknown" : artist.getArtistName())
                .unitName(isArtistDeleted ? null : artist.getUnitName())
                .content(isArtistDeleted ? null : artist.getContent())
                .musicType(prskMusic.getMusicType())
                .specially(prskMusic.getSpecially())
                .lyricsName(prskMusic.getLyricsName())
                .musicName(prskMusic.getMusicName())
                .featuring(prskMusic.getFeaturing())
                .youtubeLink(prskMusic.getYoutubeLink())
                .auditInfo(AuditInfo.from(prskMusic))
                .build();
    }
}
