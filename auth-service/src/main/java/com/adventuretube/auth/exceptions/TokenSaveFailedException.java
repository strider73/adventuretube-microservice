package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class TokenSaveFailedException extends BaseServiceException {
    public TokenSaveFailedException(ErrorCode errorCode) {
        super(errorCode);
    }



}
