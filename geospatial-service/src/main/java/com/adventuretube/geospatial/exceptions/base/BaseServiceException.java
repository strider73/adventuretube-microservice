package com.adventuretube.geospatial.exceptions.base;

import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public abstract class BaseServiceException extends RuntimeException {
    private final GeoErrorCode errorCode;
    private final String origin;

    public BaseServiceException(GeoErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;

        // Automatically capture className.methodName
        StackTraceElement[] stackTrace = this.getStackTrace();
        this.origin = stackTrace.length > 0
                ? stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName()
                : "UnknownOrigin";
    }

    public GeoErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOrigin() {
        return origin;
    }
}
