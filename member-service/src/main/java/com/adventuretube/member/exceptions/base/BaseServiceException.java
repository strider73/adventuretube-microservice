package com.adventuretube.member.exceptions.base;

import com.adventuretube.member.exceptions.code.MemberErrorCode;

public abstract class BaseServiceException extends RuntimeException {
    private final MemberErrorCode errorCode;
    private final String origin;

    public BaseServiceException(MemberErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;

        // Automatically capture className.methodName
        StackTraceElement[] stackTrace = this.getStackTrace();
        this.origin = stackTrace.length > 0
                ? stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName()
                : "UnknownOrigin";
    }

    public MemberErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOrigin() {
        return origin;
    }
}
