package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class MemberServiceException extends BaseServiceException {
    public MemberServiceException(AuthErrorCode errorCode) {
        super(errorCode);
    }

}
