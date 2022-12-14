package com.prac.accesstoken_refreshtoken_with_redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prac.accesstoken_refreshtoken_with_redis.dto.Response;
import com.prac.accesstoken_refreshtoken_with_redis.dto.SocialLoginInfoDto;
import com.prac.accesstoken_refreshtoken_with_redis.dto.UserResponseDto;
import com.prac.accesstoken_refreshtoken_with_redis.auth.jwt.JwtTokenProvider;
import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsImpl;
import com.prac.accesstoken_refreshtoken_with_redis.model.NaverProfile;
import com.prac.accesstoken_refreshtoken_with_redis.model.OAuthToken;
import com.prac.accesstoken_refreshtoken_with_redis.model.User;
import com.prac.accesstoken_refreshtoken_with_redis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NaverService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate redisTemplate;
    private final Response response;
    private static final String BEARER_TYPE = "Bearer";


    public SocialLoginInfoDto requestNaver(String code, String state, HttpServletResponse response){

        RestTemplate rt = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type","authorization_code");
        params.add("client_id","k7spc3MRJ9Ut2UUxudqp");
        params.add("client_secret","1feqER4xKL");
        params.add("code",code);
        params.add("client_secret","1feqER4xKL");
        params.add("state", "dontbeweak");

        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = //????????? ???????????? ????????????
                new HttpEntity<>(params, headers); //????????? exchange??? HttpEntity ??????????????? ?????? ????????????.

        //Http???????????? - Post???????????? - ????????? responseEntity ????????? ?????? ??????.
        ResponseEntity<String> responseEntity = rt.exchange(
                "https://nid.naver.com/oauth2.0/token",
                HttpMethod.POST,
                naverTokenRequest,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        OAuthToken oauthToken = null;
        try {
            oauthToken = objectMapper.readValue(responseEntity.getBody(), OAuthToken.class);
            System.out.println(oauthToken); //oauthToken ??? ????????? ??????
        } catch (
                JsonProcessingException e) {
            e.printStackTrace();
        }
        //????????? ????????? ????????? ??????
        System.out.println("????????? ????????? ?????? : " + oauthToken.getAccess_token());


        RestTemplate rt2 = new RestTemplate();

        HttpHeaders headers2 = new HttpHeaders();
        headers2.add("Authorization","Bearer "+oauthToken.getAccess_token());
        headers2.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> naverProfileRequest2 = //????????? ???????????? ????????????
                new HttpEntity<>(headers2); //????????? exchange??? HttpEntity ??????????????? ?????? ????????????.

        //Http???????????? - Post???????????? - ????????? responseEntity ????????? ?????? ??????.
        ResponseEntity<String> response2 = rt2.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.POST,
                naverProfileRequest2,
                String.class
        );

        ObjectMapper objectMapper2 = new ObjectMapper();
        NaverProfile naverProfile = null;
        try {
            naverProfile = objectMapper2.readValue(response2.getBody(), NaverProfile.class);
        } catch (
                JsonProcessingException e) {
            e.printStackTrace();
        }

        System.out.println("????????? ?????????: "+naverProfile.getResponse().getId());
        System.out.println("????????? ?????????: "+naverProfile.getResponse().getName());
        System.out.println("??????????????? ?????? ???????????? : " + "Naver_" + naverProfile.getResponse().getId());

        User naverUser = User.builder()
                .nickname(naverProfile.getResponse().getName())
                .username("Naver_"+naverProfile.getResponse().getId())
                .password(naverProfile.getResponse().getId())
                .oauth("naver")
                .build();

        User originUser = findByUser(naverUser.getUsername());

        if(originUser.getUsername() == null){
            System.out.println("?????? ???????????????.");
            SignupNaverUser(naverUser);
        }

        // naver ????????? ??????
        System.out.println("naver ????????? ?????????");
        if (naverUser.getUsername() != null) {
            User userEntity = userRepository.findByUsername(naverUser.getUsername()).orElseThrow(
                    () -> new IllegalArgumentException("naver username??? ????????????.")
            );
            UserDetailsImpl userDetails = new UserDetailsImpl(userEntity);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            //????????? ????????? ????????? ?????? ??? ????????????. -> ?????? controller ?????? @AuthenticationPrincipal UserDetailsImpl userDetails ??? ????????? ?????? ??? ??????.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // ?????? ????????? ???????????? JWT ?????? ??????
            UserResponseDto.TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

            System.out.println("access token : " + tokenInfo.getAccessToken());
            System.out.println("refresh token : " + tokenInfo.getRefreshToken());
            System.out.println("access token, refresh token ?????? ??????");

            // RefreshToken Redis ?????? (expirationTime ????????? ?????? ?????? ?????? ??????)
            redisTemplate.opsForValue()
                    .set("RT:" + authentication.getName(), tokenInfo.getRefreshToken(), tokenInfo.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);
            System.out.println("refresh token redis ?????? ??????");

            response.addHeader("Authorization", BEARER_TYPE + " " + tokenInfo.getAccessToken());
            System.out.println("JWT?????? : " + BEARER_TYPE + " " + tokenInfo.getAccessToken());
        }

        String username = naverUser.getUsername();
        String nickname = naverUser.getNickname();

        SocialLoginInfoDto socialLoginInfoDto = new SocialLoginInfoDto(username, nickname);
        return socialLoginInfoDto;

    }


    //?????? ????????? ?????? ?????? ??????
    public String SignupNaverUser(User naverUser) {
        String error = "";
        String username = naverUser.getUsername();
        String password = naverUser.getPassword();
        String nickname = naverUser.getNickname();
        String oauth = naverUser.getOauth();

        // ???????????? ?????????
        password = passwordEncoder.encode(password);
//        naverUser.setPassword(password);

        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .password(password)
                .oauth(oauth)
                .role("ROLE_USER")
                .build();

        userRepository.save(user);

        return error;
    }

    @Transactional(readOnly = true)
    public User findByUser(String username) {
        User user = userRepository.findByUsername(username).orElseGet(
                ()-> {return new User();}
        );
        return user;
    }

}
