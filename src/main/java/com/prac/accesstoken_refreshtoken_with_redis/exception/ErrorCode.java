package com.prac.accesstoken_refreshtoken_with_redis.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

    USERNAME_DUPLICATION_CODE(400, "C001", "중복된 아이디가 있습니다"),
    USERNAME_FORM_CODE(400, "C002", "아이디를 정확히 입력해 주세요"),
    PASSWORD_CHECK_CODE(400, "C003", "패스워드와 패스워드 확인이 일치하지 않습니다"),
    PASSWORD_LENGTH_CODE(400, "C004", "패스워드는 4글자 이상 입력해 주세요"),
    LOGIN_CHECK_CODE(400,"C005", "로그인을 해 주세요."),
    LOGIN_ERROR_CODE(400,"C006","잘못된 로그인 정보입니다."),
    NOT_ENOUGH_MONEY(400,"C007", "금액이 부족합니다."),
    FRIEND_ADD_CODE(400, "C008", "자기 자신과 친구할 수 없습니다"),
    FRIEND_CHECK_CODE(400, "C009", "중복된 친구입니다"),
    NO_ITEM(404,"C010", "아이템이 존재하지 않습니다."),
    NOT_FOUND_USER(404, "C011", "유저를 찾을 수 없습니다. 로그인을 다시 해주세요."),
    NOT_FOUND_CAT(404,"C012","고양이를 찾을 수 없습니다."),
    NOT_FOUND_CATIMAGE(500,"C013","고양이의 모습을 찾을 수 없습니다."),
    USED_EXPIRED_TOKEN(400, "C014", "이미 재발급에 사용된 만료 토큰입니다."),
    LOGGED_OUT_TOKEN(400, "C015", "로그아웃 된 토큰입니다."),
    INVALIED_EXPIRED_TOKEN(400, "C017", "만료된 지 너무 오래된 토큰입니다."),
    NOT_FOUND_REFRESH_TOKEN(400, "C018", "리프레시 토큰을 찾을 수 없습니다."),
    PILL_DUPLICATION_CODE(400, "C019", "이미 등록된 영양제입니다.");




    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) { //enum 은 생성자가 존재하지만 Default 생성자는 private 로 되어 있으며 public 으로 변경하는 경우 컴파일 에러가 발생
        //다른 클래스나 인터페이스에서의 상수선언이 클래스 로드 시점에서 생성되는 것 처럼 Enum 또한 생성자가 존재하지만
        // 클래스가 로드되는 시점에서 생성되기 때문에 임의로 생성하여 사용 할 수 없다
        this.status = status;
        this.code = code;
        this.message = message;
    }
}