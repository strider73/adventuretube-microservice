package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class TokenNotFoundException extends BaseServiceException {
    public TokenNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }


}
