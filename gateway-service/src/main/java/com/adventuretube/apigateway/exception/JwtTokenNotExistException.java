package com.adventuretube.apigateway.exception;

import io.jsonwebtoken.JwtException;

public class JwtTokenNotExistException extends JwtException {
    public JwtTokenNotExistException(String message) {
        super(message);
    }

}
