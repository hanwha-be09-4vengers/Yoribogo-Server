package com.avengers.yoribogo.user.domain.vo.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/* 설명. 리프레시 토큰요청 받는 DTO*/
@Data
public class TokenRefreshRequest {
    @JsonProperty("refresh_token")
    private String refreshToken;
}