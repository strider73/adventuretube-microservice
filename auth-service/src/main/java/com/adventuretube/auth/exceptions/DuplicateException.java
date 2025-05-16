package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class DuplicateException extends BaseServiceException {

    public DuplicateException(AuthErrorCode errorCode) {
        super(errorCode);
    }

}
