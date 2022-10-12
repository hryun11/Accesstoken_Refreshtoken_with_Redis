package com.prac.accesstoken_refreshtoken_with_redis.auth.jwt;


import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsImpl;
import com.prac.accesstoken_refreshtoken_with_redis.auth.UserDetailsServiceImpl;
import com.prac.accesstoken_refreshtoken_with_redis.exception.CustomException;
import com.prac.accesstoken_refreshtoken_with_redis.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FormLoginProvider implements AuthenticationProvider {

    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("provider를 거침");
        String username = (String)authentication.getPrincipal(); //화면에서 입력한 유저네임을 username에 담는다.
        String password = (String)authentication.getCredentials(); //화면에서 입력한 비밀번호를 password에 담는다.

        //화면에서 입력한 유저네임으로 DB에 있는 사용자의 정보를 UserDetailsImpl 형으로 가져와 userDetails에 담는다.
        UserDetailsImpl userDetails = userDetailsServiceImpl.loadUserByUsername(username);

        //화면에서 입력한 비밀번호와 DB에서 가져온 비밀번호를 비교하는 로직이다. 비밀번호가 맞지 않다면 예외를 던진다.
        if(passwordEncoder.matches(password, userDetails.getPassword())) {
            //계정이 인증됐다면 UsernamePasswordAuthenticationToken 객체에 화면에서 입력한 정보와 DB에서 가져온 권한을 담아서 리턴한다.
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } else {
            throw new CustomException(ErrorCode.LOGIN_ERROR_CODE);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {                                       //token 타입에 따라서 언제 provider를 사용할지 조건을 지정할 수 있다.
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication); //provider의 supports 값이 false를 리턴하면, provider의 authenticate 메소드가 호출되지 않는다.
    }
}
