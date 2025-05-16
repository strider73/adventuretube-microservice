package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class UnKnownException  extends BaseServiceException {
    public UnKnownException(AuthErrorCode errorCode) {
        super(errorCode);
    }



}
