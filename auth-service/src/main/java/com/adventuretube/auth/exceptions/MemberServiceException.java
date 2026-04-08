package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.exception.BaseServiceException;

public class MemberServiceException extends BaseServiceException {
    public MemberServiceException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public MemberServiceException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

}
