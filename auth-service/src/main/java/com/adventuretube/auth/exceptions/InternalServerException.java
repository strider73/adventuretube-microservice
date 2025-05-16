package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class InternalServerException extends BaseServiceException {
    public InternalServerException(AuthErrorCode errorCode) {
        super(errorCode);
    }


}
