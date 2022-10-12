package com.prac.accesstoken_refreshtoken_with_redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginIdCheckDto {
    private String username;
    private String nickname;

    @Builder
    public LoginIdCheckDto(String username, String nickname) {
        this.username = username;
        this.nickname = nickname;
    }
}