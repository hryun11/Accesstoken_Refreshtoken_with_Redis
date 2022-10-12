package com.prac.accesstoken_refreshtoken_with_redis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
//@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginInfoDto {

    private String username;
    private String nickname;
}
