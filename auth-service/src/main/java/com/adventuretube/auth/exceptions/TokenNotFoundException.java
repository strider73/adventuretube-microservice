package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class TokenNotFoundException extends BaseServiceException {
    public TokenNotFoundException(AuthErrorCode errorCode) {
        super(errorCode);
    }


}
