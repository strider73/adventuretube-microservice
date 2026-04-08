package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class TokenSaveFailedException extends BaseServiceException {
    public TokenSaveFailedException(AuthErrorCode errorCode) {
        super(errorCode);
    }



}
