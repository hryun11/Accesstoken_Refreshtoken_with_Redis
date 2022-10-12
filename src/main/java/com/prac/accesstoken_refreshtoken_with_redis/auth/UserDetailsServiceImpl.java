package com.prac.accesstoken_refreshtoken_with_redis.auth;

import com.prac.accesstoken_refreshtoken_with_redis.model.User;
import com.prac.accesstoken_refreshtoken_with_redis.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //로그인 시 이메일을 찾을 수 없을 때
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("이 " + username + " 아이디는 존재하지 않습니다."));

        return new UserDetailsImpl(user);
    }
}
