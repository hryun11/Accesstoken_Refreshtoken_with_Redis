package com.prac.accesstoken_refreshtoken_with_redis.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsImpl;
import com.prac.accesstoken_refreshtoken_with_redis.dto.UserResponseDto;
import com.prac.accesstoken_refreshtoken_with_redis.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class FormLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate redisTemplate;

    public FormLoginFilter(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, RedisTemplate redisTemplate) {
        super(authenticationManager);

        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }


    //login 요청하면 로그인 시도를 위해 실행되는 함수
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("JwtAuthenticationFilter: 로그인 시도 중");

        try {
            ObjectMapper om = new ObjectMapper();
            User user = om.readValue(request.getInputStream(), User.class);
            System.out.println("사용자 : " + user.getUsername());
            System.out.println("=============================================");

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

            Authentication authentication =
                    getAuthenticationManager().authenticate(authenticationToken);

            return authentication;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //attemptAuthentication실행 후 인증이 정상적으로 되었으면 successfulAuthentication 함수가 실행됨.
    //JWT 토큰을 만들어서 request요청한 사용자에게 JWT토큰을 response해주면 됨.
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        System.out.println("successfulAuthentication 실행됨: 인증이 완료되었다는 뜻.");
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();


        // 3. 인증 정보를 기반으로 JWT AccessToken, RefreshToken 생성
        UserResponseDto.TokenInfo tokenInfo = jwtTokenProvider.generateToken(authResult);

        System.out.println("ACCESS TOKEN : " + tokenInfo.getAccessToken());
        System.out.println("REFRESH TOKEN : " + tokenInfo.getRefreshToken());
        System.out.println("==== access token, refresh token 생성 완료 ====");


        // 4. RefreshToken Redis 저장 (expirationTime 설정을 통해 자동 삭제 처리)
        redisTemplate.opsForValue()
                .set("RT:" + authResult.getName(), tokenInfo.getRefreshToken(), tokenInfo.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);
        System.out.println("==== refresh token redis 저장 완료 ====");

        response.addHeader("Authorization", "Bearer "+tokenInfo.getAccessToken());
    }


    //로그인 예외처리
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        System.out.println(failed.getMessage());
        response.setStatus(400);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(failed.getMessage());

    }
}