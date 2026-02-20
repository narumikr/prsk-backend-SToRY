package com.example.untitled.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザーマスタAPIリクエスト
 */
@Getter
@Setter
@NoArgsConstructor
public class UserRequest {

    /** ユーザー名 **/
    @NotBlank(message = "ユーザー名は必須です。 - The user name is required.")
    @Size(max = 20, message = "ユーザー名は20文字以内で入力してください。 - Please enter the user name within 20 characters.")
    private String userName;

    /** パスワード **/
    @NotBlank(message = "パスワードは必須です。 - The password is required.")
    @Size(max = 20, message = "パスワードは20文字以内で入力してください。 - Please enter the password with 20 characters.")
    private String password;
}
