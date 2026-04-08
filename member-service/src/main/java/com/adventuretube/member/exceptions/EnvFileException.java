package com.adventuretube.member.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.member.exceptions.code.MemberErrorCode;

public class EnvFileException  extends BaseServiceException {

    public EnvFileException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
