package com.example.poc.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http) {

        http
                .authorizeExchange((exchanges) ->
                        exchanges.anyExchange().authenticated()
                )
                .oauth2Login(withDefaults())
                .exceptionHandling()
                .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/okta"));
        return http.build();

    }
}
