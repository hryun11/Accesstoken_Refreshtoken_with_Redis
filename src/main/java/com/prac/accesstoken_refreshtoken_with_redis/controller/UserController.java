package com.prac.accesstoken_refreshtoken_with_redis.controller;

import com.prac.accesstoken_refreshtoken_with_redis.dto.LoginIdCheckDto;
import com.prac.accesstoken_refreshtoken_with_redis.dto.Response;
import com.prac.accesstoken_refreshtoken_with_redis.dto.SignupRequestDto;
import com.prac.accesstoken_refreshtoken_with_redis.dto.SocialLoginInfoDto;
import com.prac.accesstoken_refreshtoken_with_redis.exception.Helper;
import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsImpl;
import com.prac.accesstoken_refreshtoken_with_redis.service.KakaoService;
import com.prac.accesstoken_refreshtoken_with_redis.service.NaverService;
import com.prac.accesstoken_refreshtoken_with_redis.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final KakaoService kakaoService;
    private final NaverService naverService;

    //회원가입 요청 처리
    @PostMapping("/user/signup")
    @ApiOperation(value = "회원가입 요청 처리")
    public String registerUser(@Valid @RequestBody SignupRequestDto requestDto){
        String res = userService.registerUser(requestDto);
        if(res.equals("")){
            return "회원가입 성공";
        }else{
            return res;
        }
    }

//    @PostMapping("/login")
//    public ResponseEntity<?> login(@Validated UserRequestDto.Login login, Errors errors) {
//        // validation check
//        if (errors.hasErrors()) {
//            return response.invalidFields(Helper.refineErrors(errors));
//        }
//        return userService.login(login);
//    }

    //카카오 소셜 로그인
    @GetMapping("/auth/kakao/callback")
    @ApiOperation(value = "카카오 소셜 로그인")
    public @ResponseBody SocialLoginInfoDto kakaoCallback(String code, HttpServletResponse response) {      //ResponseBody -> Data를 리턴해주는 컨트롤러 함수
        return kakaoService.requestKakao(code, response);
    }

    //네이버 소셜 로그인
    @GetMapping("/auth/naver/callback")
    @ApiOperation(value = "네이버 소셜 로그인")
    public @ResponseBody SocialLoginInfoDto naverCallback(String code, String state, HttpServletResponse response){
        return naverService.requestNaver(code, state, response);
    }

    //로그인 유저 정보
    @GetMapping("/user")
    @ApiOperation(value = "로그인 유저 정보", notes = "로그인 한 사용자 정보를 조회한다.")
    public LoginIdCheckDto userDetails(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return userService.userInfo(userDetails);
    }

    // Access Token 재발급
//    @PostMapping("/user/reissue")
//    @ApiOperation(value = "Access Token 재발급", notes = "")
//    public ResponseEntity<?> reissue(HttpServletRequest httpServletRequest, Response response, Errors errors) {
//        // validation check
//        if (errors.hasErrors()) {
//            return response.invalidFields(Helper.refineErrors(errors));
//        }
//        return userService.reissue(httpServletRequest);
//    }


    // 로그아웃
    @PostMapping("/user/logout")
    @ApiOperation(value = "로그아웃")
    public ResponseEntity<?> logout(HttpServletRequest httpServletRequest, Response response, Errors errors) {
        // validation check
        if (errors.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(errors));
        }
        return userService.logout(httpServletRequest);
    }
}