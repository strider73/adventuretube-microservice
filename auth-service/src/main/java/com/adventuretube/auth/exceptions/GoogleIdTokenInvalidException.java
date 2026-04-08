package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class GoogleIdTokenInvalidException extends BaseServiceException {
    public GoogleIdTokenInvalidException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
