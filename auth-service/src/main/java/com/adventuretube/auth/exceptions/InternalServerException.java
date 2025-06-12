package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class InternalServerException extends BaseServiceException {
    public InternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }


}
