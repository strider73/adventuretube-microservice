package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class UserNotFoundException extends BaseServiceException {
    public UserNotFoundException(AuthErrorCode errorCode) {
        super(errorCode);
    }

}
