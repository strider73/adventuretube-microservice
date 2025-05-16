package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class AccessDeniedException extends BaseServiceException {
    public AccessDeniedException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
