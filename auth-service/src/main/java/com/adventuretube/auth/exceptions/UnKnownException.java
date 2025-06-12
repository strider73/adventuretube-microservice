package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class UnKnownException  extends BaseServiceException {
    public UnKnownException(ErrorCode errorCode) {
        super(errorCode);
    }



}
