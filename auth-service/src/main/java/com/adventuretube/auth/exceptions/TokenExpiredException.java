package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class TokenExpiredException extends BaseServiceException {
    public TokenExpiredException(AuthErrorCode errorCode) {
        super(errorCode);
    }

}
