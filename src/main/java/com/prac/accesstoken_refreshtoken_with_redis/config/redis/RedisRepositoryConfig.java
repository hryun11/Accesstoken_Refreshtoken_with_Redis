package com.prac.accesstoken_refreshtoken_with_redis.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;


// Spring Data Redis를 통해 Lettuce, Jedis라는 두 가지 오픈소스 라이브러리를 사용할 수 있다.
// 의존성을 추가할 때 별도의 설정을 하지 않으면 Lettuce가 기본으로 적용된다.
// Spring Data Redis는 Redis에 두 가지 접근 방식 제공.
// 하나는 RedisTemplate을 이용한 방식, 다른 하나는 RedisRepository를 이용한 방식.
// 두 방식 모두 Redis에 접근하기 위해서는 Redis 저장소와 연결하는 과정이 필요.
// RedisConnectionFactory 인터페이스를 통해 lettuceConnectionFactory를 생성하여 반환한다.
@Configuration
@EnableRedisRepositories
public class RedisRepositoryConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    // setKeySerializer, setValueSerializer 설정을 함으로써 redis-cli를 통해 직접 테이터를 볼 수 있다.
    // RedisTemplate를 사용할 때 Spring - Redis 간 데이터 직렬화, 역직렬화 시 사용하는 방식이 jdk 직렬화 방식.
    // redis-cli을 통해 직접 데이터를 보려고 할 때 알아볼 수 없는 형태로 출력되기 때문에 적용한 설정.
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
