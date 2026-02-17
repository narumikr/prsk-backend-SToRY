package com.example.untitled.prskmusic.dto;

import com.example.untitled.common.dto.MetaInfo;
import com.example.untitled.prskmusic.PrskMusic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * プロセカ楽曲マスタAPIレスポンス for GET
 */
@Getter
@AllArgsConstructor
public class PrskMusicListResponse {

    /** プロセカ楽曲リスト **/
    private List<PrskMusicResponse> items;

    /** メタ情報 **/
    private MetaInfo meta;

    public static PrskMusicListResponse from(Page<PrskMusic> prskMusicPage) {
        List<PrskMusicResponse> items = prskMusicPage.getContent().stream()
                .map(prskMusic -> PrskMusicResponse.from(prskMusic))
                .toList();

        MetaInfo meta = MetaInfo.from(prskMusicPage);

        return new PrskMusicListResponse(items, meta);
    }
}
