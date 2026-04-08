package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class AccessDeniedException extends BaseServiceException {
    public AccessDeniedException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
