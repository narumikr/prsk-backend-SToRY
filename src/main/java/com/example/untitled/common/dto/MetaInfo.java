package com.example.untitled.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * Meta 情報
 */
@Getter
@AllArgsConstructor
public class MetaInfo {

    /** 総アイテム数 **/
    private Long totalItems;

    /** 総ページ数 **/
    private int totalPages;

    /** 現在のページ番号 **/
    private int pageIndex;

    /** ページあたりのアイテム数 **/
    private int limit;

    public static <T>MetaInfo from(Page<T> page) {
        return new MetaInfo(
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
