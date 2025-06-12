package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class AccessDeniedException extends BaseServiceException {
    public AccessDeniedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
