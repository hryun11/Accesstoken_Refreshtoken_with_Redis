package com.prac.accesstoken_refreshtoken_with_redis.auth.jwt;


import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsImpl;
import com.prac.accesstoken_refreshtoken_with_redis.dto.Response;
import com.prac.accesstoken_refreshtoken_with_redis.dto.UserResponseDto;
import com.prac.accesstoken_refreshtoken_with_redis.exception.ErrorCode;
import com.prac.accesstoken_refreshtoken_with_redis.model.User;
import com.prac.accesstoken_refreshtoken_with_redis.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// JWT 토큰 생성, 토큰 복호화 및 정보 추출, 토큰 유효성 검증의 기능이 구현된 클래스
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final Long ACCESS_TOKEN_EXPIRE_TIME = 20 * 60 * 1000L;   // 20분
    private static final Long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L; // 7일
    private static final Long EXPIRED_ACCESSTOKEN_REDIS_SAVETIME = 3 * 24 * 60 * 60 * 1000L; // 3일

    private final Key key;
    private final UserRepository userRepository;
    private final RedisTemplate redisTemplate;


    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, UserRepository userRepository, RedisTemplate redisTemplate) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    // 사용자 정보를 가지고 AccessToken, RefreshToken을 생성하는 메서드
    public UserResponseDto.TokenInfo generateToken(Authentication authentication) {

        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken();

        return UserResponseDto.TokenInfo.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenExpirationTime(REFRESH_TOKEN_EXPIRE_TIME)
                .build();
    }

    // 액세스 토큰 생성
    public String generateAccessToken(Authentication authentication) {
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 발급되는 현재 시간
        Long now = (new Date().getTime());
        // AccessToken 생성
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();

        return accessToken;
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken() {
        // 발급되는 현재 시간
        Long now = (new Date().getTime());

        // RefreshToken 생성
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();

        return refreshToken;
    }


    // AccessToken 재발급
    public UserResponseDto.TokenInfo regenerateAccessToken(Authentication authentication) {
        String newAccessToken = generateAccessToken(authentication);

        return UserResponseDto.TokenInfo.builder()
                .grantType(BEARER_TYPE)
                .accessToken(newAccessToken)
                .accessTokenExpirationTime(ACCESS_TOKEN_EXPIRE_TIME)
                .build();
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        String username = claims.getSubject();

        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            UserDetailsImpl userDetails = new UserDetailsImpl(user);

            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        }
        return null;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {   // 만료된 토큰이라도 클레임 꺼냄
            return e.getClaims();
        }
    }

    public boolean validateAccessToken(String AccessToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(AccessToken);
            return true;
        } catch (
                io.jsonwebtoken.security.SecurityException |
                MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token");
            return true;
        }catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    // 토큰 만료 확인 메서드
    public boolean checkExpiredToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            return false;
        }
    }


    // 토큰 정보를 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (
                io.jsonwebtoken.security.SecurityException |
                MalformedJwtException e) {                  // jwt가 올바르게 구성되지 않았을 때
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {                   // jwt가 만료되었을 때
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {               // 예상하는 형식과 일치하지 않는 특정 형식이나 구성의 jwt일 때
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }


    // accessToken 남은 유효시간
    public Long getExpiration(String accessToken) {
        Date expiration = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getExpiration();
        // 현재 시간
        Long now = new Date().getTime();
        return (expiration.getTime() - now);
    }

    // 만료 토큰 유효 기간
    // 재발급 시 사용된 만료된 얙세스 토큰은 바로 레디스로 저장된다 (blacklist)
    // 3일 간 레디스에 저장된 후 삭제되는데
    // 3일이 지난 후 블랙리스트에 없는 것이 확인되면 재발급에 이용될 수 있는 것을 막기 위해
    // 토큰 만료 시간에 3일을 더하여 현재 시간과 비교한다. (만료 토큰의 수명은 3일로 지정)
    // 현재 시간이 '만료시간 + 3일'보다 이전인 것이 확인되면 다음 절차로 넘어간다.
    public boolean getExpiredAccessTokenlifeSpan(String accessToken) {
        Claims claims = parseClaims(accessToken);
        Long expiration = claims.getExpiration().getTime();

        // 현재 시간
        Date now = new Date();

        // 만료 토큰 폐기 시간
        Date expiredATValidTime = new Date(expiration + EXPIRED_ACCESSTOKEN_REDIS_SAVETIME);

        if (now.before(expiredATValidTime)) {
            return true;
        } else {
            return false;
        }
    }

    // 액세스 토큰 재발급
    public ResponseEntity<?> regenerateAccessTokenProcess(HttpServletResponse httpServletResponse, String accessToken, Response response) {

        // Access Token 복호화로 추출한 username으로 authentication 객체 만들기
        Authentication authentication = getAuthentication(accessToken);

        // Redis 에서 Username을 기반으로 저장된 Refresh Token 값을 가져오기
        String refreshToken = (String)redisTemplate.opsForValue().get("RT:" + authentication.getName());

        // 로그아웃되어 Redis 에 RefreshToken이 존재하지 않는 경우 처리
        if(ObjectUtils.isEmpty(refreshToken)) {
            System.out.println("==== 리프레시 토큰이 존재하지 않음 ====");
            log.error(ErrorCode.NOT_FOUND_REFRESH_TOKEN.getMessage(), ErrorCode.NOT_FOUND_REFRESH_TOKEN.getStatus());
            return response.fail("잘못된 요청입니다.", HttpStatus.BAD_REQUEST);
        }

        // Refresh Token 검증
        if (!validateToken(refreshToken)) {
            return response.fail("Refresh Token 정보가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 새로운 Access Token 생성
        UserResponseDto.TokenInfo tokenInfo = regenerateAccessToken(authentication);

        // Response Header에 새 Access Token 세팅
        httpServletResponse.setHeader("Authorization", BEARER_TYPE + " " + tokenInfo.getAccessToken());

        System.out.println("==== NEW ACCESSTOKEN : " + BEARER_TYPE + " " + tokenInfo.getAccessToken() + " ====");

        Authentication newAuthentication = getAuthentication(tokenInfo.getAccessToken());
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);
        System.out.println("==== 3-3. [PASS] SecurityContext 저장 ====");

        // 만료된 AT를 Redis에 저장
        saveAceessTokenBlackList(accessToken);
        System.out.println("==== Redis에 만료된 Access Token 저장 완료 ====");

        return response.success(tokenInfo, "Token 정보가 갱신되었습니다.", HttpStatus.OK);
    }


    // 재발급에 사용된 만료 Access Token을 Redis에 저장하는 method
    public void saveAceessTokenBlackList(String accessToken) {
        Long savetime = EXPIRED_ACCESSTOKEN_REDIS_SAVETIME;

        redisTemplate.opsForValue().set(accessToken, "expired", savetime, TimeUnit.MILLISECONDS);
    }
}
