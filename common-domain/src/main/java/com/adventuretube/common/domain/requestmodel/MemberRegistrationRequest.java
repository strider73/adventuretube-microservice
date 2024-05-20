package com.adventuretube.common.domain.requestmodel;

public record MemberRegistrationRequest (
        String name,
        String email ,
        String channeld
){}