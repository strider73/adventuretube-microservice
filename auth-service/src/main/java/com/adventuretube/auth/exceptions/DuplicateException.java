package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class DuplicateException extends BaseServiceException {

    public DuplicateException(AuthErrorCode errorCode) {
        super(errorCode);
    }

}
