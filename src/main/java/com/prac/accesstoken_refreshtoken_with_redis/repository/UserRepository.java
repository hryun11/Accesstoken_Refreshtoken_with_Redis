package com.prac.accesstoken_refreshtoken_with_redis.repository;

import com.prac.accesstoken_refreshtoken_with_redis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    User findAllById(long id);

    Optional<User> findUserByUsername(String friendname);
}