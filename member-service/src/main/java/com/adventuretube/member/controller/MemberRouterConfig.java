package com.adventuretube.member.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class MemberRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> memberRoutes(MemberHandler handler) {
        return route(POST("/member/registerMember"), handler::registerMember)
                .andRoute(POST("/member/emailDuplicationCheck"), handler::emailDuplicationCheck)
                .andRoute(POST("/member/findMemberByEmail"), handler::findMemberByEmail)
                .andRoute(POST("/member/storeTokens"), handler::storeToken)
                .andRoute(POST("/member/findToken"), handler::findToken)
                .andRoute(POST("/member/deleteAllToken"), handler::deleteAllToken)
                .andRoute(POST("/member/deleteUser"), handler::deleteUser);
    }
}
