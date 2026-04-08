package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class UnKnownException  extends BaseServiceException {
    public UnKnownException(AuthErrorCode errorCode) {
        super(errorCode);
    }



}
