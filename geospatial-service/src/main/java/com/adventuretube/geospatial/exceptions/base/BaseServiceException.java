package com.adventuretube.geospatial.exceptions.base;

import com.adventuretube.common.api.code.ErrorCode;

public abstract class BaseServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String origin;

    public BaseServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;

        // Automatically capture className.methodName
        StackTraceElement[] stackTrace = this.getStackTrace();
        this.origin = stackTrace.length > 0
                ? stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName()
                : "UnknownOrigin";
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOrigin() {
        return origin;
    }
}
