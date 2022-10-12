package com.prac.accesstoken_refreshtoken_with_redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AccesstokenRefreshtokenWithRedisApplication {

    public static final String APPLICATION_LOCATIONS = "spring.config.location="
            + "classpath:application-jwt.yml,"
            + "classpath:application.yml";

    public static void main(String[] args) {
        new SpringApplicationBuilder(AccesstokenRefreshtokenWithRedisApplication.class)
                .properties(APPLICATION_LOCATIONS)
                .run(args);
    }

}
