package com.avengers.yoribogo.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /* 설명. 시큐리티를 위한 RestTemplate 빈 추가 */
    @Bean(name = "securityRestTemplate")  // 시큐리티용 RestTemplate 이름 설정
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /* 설명. Security 자체에서 사용할 암호화 방식용 bean 추가 */
    @Bean
    public BCryptPasswordEncoder getBCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
