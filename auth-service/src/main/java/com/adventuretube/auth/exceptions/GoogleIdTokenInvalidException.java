package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class GoogleIdTokenInvalidException extends BaseServiceException {
    public GoogleIdTokenInvalidException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
