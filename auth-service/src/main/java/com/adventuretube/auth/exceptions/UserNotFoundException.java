package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class UserNotFoundException extends BaseServiceException {
    public UserNotFoundException(AuthErrorCode errorCode) {
        super(errorCode);
    }

}
