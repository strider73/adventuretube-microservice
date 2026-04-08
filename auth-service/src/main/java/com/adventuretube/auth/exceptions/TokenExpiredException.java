package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class TokenExpiredException extends BaseServiceException {
    public TokenExpiredException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public TokenExpiredException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
