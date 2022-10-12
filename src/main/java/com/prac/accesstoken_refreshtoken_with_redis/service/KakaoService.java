package com.prac.accesstoken_refreshtoken_with_redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prac.accesstoken_refreshtoken_with_redis.dto.SocialLoginInfoDto;
import com.prac.accesstoken_refreshtoken_with_redis.dto.UserResponseDto;
import com.prac.accesstoken_refreshtoken_with_redis.auth.jwt.JwtTokenProvider;
import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsImpl;
import com.prac.accesstoken_refreshtoken_with_redis.model.KakaoProfile;
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
public class KakaoService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String BEARER_TYPE = "Bearer";



//    @Value("${secret.key}")
//    private String secretKey;

    public SocialLoginInfoDto requestKakao(String code, HttpServletResponse response) { //Data를 리턴해주는 컨트롤러 함수
        //POST방식으로 key=value 데이터를 요청(카카오쪽으로)
        RestTemplate rt = new RestTemplate();

        //HttpHeader 오브젝트 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        //HttpBody 오브젝트 생성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", "599bca646044fc4147f7f8f4c461f9ca");
        params.add("redirect_uri", "http://dontbeweak.kr/auth/kakao/callback");
//        params.add("redirect_uri", "http://localhost:8080/auth/kakao/callback");
        params.add("code", code);

        //HttpHeader와 HttpBody를 하나의 오브젝트에 담기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
                new HttpEntity<>(params, headers);

        //Http 요청하기 - POST방식으로 - 그리고 response 변수의 응답 받음.
        ResponseEntity<String> responseEntity = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );
        //Gson, Json Simple, ObjectMapper 중 하나로 json 데이터를 담는다.
        ObjectMapper objectMapper = new ObjectMapper();
        OAuthToken oauthToken = null;

        try {
            oauthToken = objectMapper.readValue(responseEntity.getBody(), OAuthToken.class);
            System.out.println(oauthToken); //oauthToken 값 확인해 보기
        } catch (
                JsonProcessingException e) {
            e.printStackTrace();
        }

        //엑세스 토큰만 뽑아서 확인
        System.out.println("카카오 엑세스 토큰:" + oauthToken.getAccess_token());

        RestTemplate rt2 = new RestTemplate();

        //HttpHeader 오브젝트 생성
        HttpHeaders headers2 = new HttpHeaders();
        headers2.add("Authorization", BEARER_TYPE + " " + oauthToken.getAccess_token());
        headers2.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");


        //HttpHeader와 HttpBody를 하나의 오브젝트에 담기
        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest2 =
                new HttpEntity<>(headers2);

        //Http 요청하기 - POST방식으로 - 그리고 response 변수의 응답 받음.
        ResponseEntity<String> response2 = rt2.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoProfileRequest2,
                String.class
        );

        //KakaoProfile오브젝트를 ObjectMapper로 담는다.
        ObjectMapper objectMapper2 = new ObjectMapper();
        KakaoProfile kakaoProfile = null;

        try {
            kakaoProfile = objectMapper2.readValue(response2.getBody(), KakaoProfile.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //User 오브젝트: username, password, nickname
        System.out.println("카카오 아이디(번호): " + kakaoProfile.getId());
        System.out.println("카카오 닉네임: " + kakaoProfile.getKakao_account().getProfile().getNickname());
        System.out.println("클라이언트 서버 유저네임 : " + "Kakao_" + kakaoProfile.getId());

        User kakaoUser = User.builder()
                .nickname(kakaoProfile.getKakao_account().getProfile().getNickname())
                .username("Kakao_" + kakaoProfile.getId())
                .password(kakaoProfile.getId().toString()) //임시 비밀번호
                .oauth("kakao")
                .build();

        //가입자 혹은 비가입자 체크해서 처리
        User originUser = findByUser(kakaoUser.getUsername());

        if (originUser.getUsername() == null) {
            System.out.println("신규 회원입니다.");
            SignupKakaoUser(kakaoUser); // <-- 이 로직이 자동 로그인 입니다. 지우시면 회원가입 따로 하시면 됩니다.
//            return "회원가입 축하합니다. 유저네임: "+ kakaoUser.getUsername()+", 닉네임: "+kakaoUser.getNickname();
        }

        // kakao 로그인 처리
        System.out.println("kakao 로그인 진행중");
        if (kakaoUser.getUsername() != null) {
            User userEntity = userRepository.findByUsername(kakaoUser.getUsername()).orElseThrow(
                    () -> new IllegalArgumentException("kakao username이 없습니다.")
            );
            UserDetailsImpl userDetails = new UserDetailsImpl(userEntity);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            //홀더에 검증이 완료된 정보 값 넣어준다. -> 이제 controller 에서 @AuthenticationPrincipal UserDetailsImpl userDetails 로 정보를 꺼낼 수 있다.
            SecurityContextHolder.getContext().setAuthentication(authentication);


            // 3. 인증 정보를 기반으로 JWT 토큰 생성
            UserResponseDto.TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

            System.out.println("access token : " + tokenInfo.getAccessToken());
            System.out.println("refresh token : " + tokenInfo.getRefreshToken());
            System.out.println("access token, refresh token 생성 완료");


            // 4. RefreshToken Redis 저장 (expirationTime 설정을 통해 자동 삭제 처리)
            redisTemplate.opsForValue()
                    .set("RT:" + authentication.getName(), tokenInfo.getRefreshToken(), tokenInfo.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);
            System.out.println("refresh token redis 저장 완료");

            response.addHeader("Authorization", BEARER_TYPE + " " + tokenInfo.getAccessToken());
            System.out.println("JWT토큰 : " + BEARER_TYPE + " " + tokenInfo.getAccessToken());
        }

        String username = kakaoUser.getUsername();
        String nickname = kakaoUser.getNickname();

        SocialLoginInfoDto socialLoginInfoDto = new SocialLoginInfoDto(username, nickname);
        return socialLoginInfoDto;
//        return "로그인 한 회원의 유저네임: "+kakaoUser.getUsername()+", 닉네임: "+kakaoUser.getNickname();
    }

    //신규 카카오 회원 강제 가입
    public String SignupKakaoUser(User kakaoUser) {
        String error = "";
        String username = kakaoUser.getUsername();
        String password = passwordEncoder.encode(kakaoUser.getPassword());
        String nickname = kakaoUser.getNickname();
        String oauth = kakaoUser.getOauth();

        // 패스워드 인코딩
//        password = passwordEncoder.encode(password);
//        kakaoUser.setPassword(password);

        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .oauth(oauth)
                .password(password)
                .role("ROLE_USER")
                .build();

        userRepository.save(user);

        return error;
    }

    //회원찾기
    @Transactional(readOnly = true)
    public User findByUser(String username) {
        User user = userRepository.findByUsername(username).orElseGet(
                ()-> {return new User();}
        );
        return user;
    }
}

