package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class UserNotFoundException extends BaseServiceException {
    public UserNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

}
