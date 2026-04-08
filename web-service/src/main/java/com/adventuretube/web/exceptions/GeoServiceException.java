package com.adventuretube.web.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.web.exceptions.code.WebErrorCode;

public class GeoServiceException extends BaseServiceException {
    public GeoServiceException(WebErrorCode errorCode) {
        super(errorCode);
    }
}
