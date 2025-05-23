package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class TokenDeletionException extends BaseServiceException {
    public TokenDeletionException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
