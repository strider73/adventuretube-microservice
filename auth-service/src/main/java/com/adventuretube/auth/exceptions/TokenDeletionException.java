package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class TokenDeletionException extends BaseServiceException {
    public TokenDeletionException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
