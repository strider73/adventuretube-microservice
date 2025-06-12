package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class TokenDeletionException extends BaseServiceException {
    public TokenDeletionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
