package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class GoogleIdTokenInvalidException extends BaseServiceException {
    public GoogleIdTokenInvalidException(ErrorCode errorCode) {
        super(errorCode);
    }
}
