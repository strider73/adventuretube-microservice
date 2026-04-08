package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class InternalServerException extends BaseServiceException {
    public InternalServerException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public InternalServerException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }


}
