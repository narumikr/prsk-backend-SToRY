package com.example.untitled.user.dto;

import com.example.untitled.common.dto.MetaInfo;
import com.example.untitled.user.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * ユーザーマスタAPIレスポンス for GET
 */
@Getter
@Builder
public class UserListResponse {

    /** ユーザーリスト **/
    private final List<UserResponse> items;

    /** メタ情報 **/
    private final MetaInfo meta;

    public static UserListResponse from(Page<User> userPage) {
        List<UserResponse> items = userPage.getContent().stream()
                .map(UserResponse::from)
                .toList();

        MetaInfo meta = MetaInfo.from(userPage);

        return UserListResponse.builder()
                .items(items)
                .meta(meta)
                .build();
    }
}
