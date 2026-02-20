package com.example.untitled.artist.dto;

import com.example.untitled.artist.Artist;
import com.example.untitled.common.dto.MetaInfo;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * アーティストマスタAPIレスポンス for GET
 */
@Getter
@Builder
public class ArtistListResponse {

    /** アーティストリスト **/
    private final List<ArtistResponse> items;

    /** メタ情報 **/
    private final MetaInfo meta;

    public static ArtistListResponse from(Page<Artist> artistPage) {
        List<ArtistResponse> items = artistPage.getContent().stream()
                .map(ArtistResponse::from)
                .toList();

        MetaInfo meta = MetaInfo.from(artistPage);

        return ArtistListResponse.builder()
                .items(items)
                .meta(meta)
                .build();
    }
}
