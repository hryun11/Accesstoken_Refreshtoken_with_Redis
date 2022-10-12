package com.prac.accesstoken_refreshtoken_with_redis.exception;

public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode){
        // RuntimeException -> Exception -> Throwable에서 message를 매개변수로 받아
        // detailMessage에 저장한 뒤, getMessage()를 통해 detailMessage를 반환한다.
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode(){
        return this.errorCode;
    }

}
