package com.example.untitled.prskmusic.dto;

import com.example.untitled.common.dto.MetaInfo;
import com.example.untitled.prskmusic.PrskMusic;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * プロセカ楽曲マスタAPIレスポンス for GET
 */
@Getter
@Builder
public class PrskMusicListResponse {

    /** プロセカ楽曲リスト **/
    private final List<PrskMusicResponse> items;

    /** メタ情報 **/
    private final MetaInfo meta;

    public static PrskMusicListResponse from(Page<PrskMusic> prskMusicPage) {
        List<PrskMusicResponse> items = prskMusicPage.getContent().stream()
                .map(PrskMusicResponse::from)
                .toList();

        MetaInfo meta = MetaInfo.from(prskMusicPage);

        return PrskMusicListResponse.builder()
                .items(items)
                .meta(meta)
                .build();
    }
}
