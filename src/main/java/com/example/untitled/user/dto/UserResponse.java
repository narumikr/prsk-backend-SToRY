package com.example.untitled.user.dto;

import com.example.untitled.common.dto.AuditInfo;
import com.example.untitled.user.User;
import lombok.Builder;
import lombok.Getter;

/**
 * ユーザーマスターAPIレスポンス
 */
@Getter
@Builder
public class UserResponse {

    /** ユーザーID **/
    private final Long id;

    /** ユーザー名 **/
    private final String userName;

    /** 監査情報 **/
    private final AuditInfo auditInfo;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .auditInfo(AuditInfo.from(user))
                .build();
    }
}
