package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class TokenExpiredException extends BaseServiceException {
    public TokenExpiredException(ErrorCode errorCode) {
        super(errorCode);
    }

}
