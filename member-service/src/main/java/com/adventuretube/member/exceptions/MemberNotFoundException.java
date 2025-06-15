package com.adventuretube.member.exceptions;

import com.adventuretube.member.exceptions.base.BaseServiceException;
import com.adventuretube.member.exceptions.code.MemberErrorCode;

public class MemberNotFoundException extends BaseServiceException {
    public MemberNotFoundException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
