package com.adventuretube.member.exceptions;

import com.adventuretube.member.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class EnvFileException  extends BaseServiceException {

    public EnvFileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
