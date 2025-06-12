package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class DuplicateException extends BaseServiceException {

    public DuplicateException(ErrorCode errorCode) {
        super(errorCode);
    }

}
