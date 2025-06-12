package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class MemberServiceException extends BaseServiceException {
    public MemberServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

}
