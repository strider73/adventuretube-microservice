package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class TokenNotFoundException extends BaseServiceException {
    public TokenNotFoundException(AuthErrorCode errorCode) {
        super(errorCode);
    }


}
