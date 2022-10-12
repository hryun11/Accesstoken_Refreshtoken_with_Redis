package com.prac.accesstoken_refreshtoken_with_redis.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResponse {

    private String message;
    private String code;
    private int status;
    public ErrorResponse(ErrorCode code){
        this.status = code.getStatus();
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    // 정적 메서드 -> 클래스가 메모리에 올라갈때 자동적으로 생성
    public static ErrorResponse of(ErrorCode code){
        // 인스턴스를 생성하지 않아도 호출 가능
        return new ErrorResponse(code);
    }

}