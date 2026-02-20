package com.example.untitled.artist.dto;

import com.example.untitled.artist.Artist;
import com.example.untitled.common.dto.AuditInfo;
import lombok.Builder;
import lombok.Getter;

/**
 * アーティストマスタAPIレスポンス
 */
@Getter
@Builder
public class ArtistResponse {

    /** ID **/
    private final Long id;

    /** アーティスト名 **/
    private final String artistName;

    /** ユニット名 **/
    private final String unitName;

    /** コンテンツ名 **/
    private final String content;

    /** 監査情報 **/
    private final AuditInfo auditInfo;

    public static ArtistResponse from(Artist artist) {
        return ArtistResponse.builder()
                .id(artist.getId())
                .artistName(artist.getArtistName())
                .content(artist.getContent())
                .auditInfo(AuditInfo.from(artist))
                .build();
    }
}
