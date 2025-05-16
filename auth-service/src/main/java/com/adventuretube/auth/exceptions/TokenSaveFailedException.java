package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class TokenSaveFailedException extends BaseServiceException {
    public TokenSaveFailedException(AuthErrorCode errorCode) {
        super(errorCode);
    }



}
