package com.adventuretube.web.exceptions;

import com.adventuretube.web.exceptions.base.BaseServiceException;
import com.adventuretube.web.exceptions.code.WebErrorCode;

public class GeoServiceException extends BaseServiceException {
    public GeoServiceException(WebErrorCode errorCode) {
        super(errorCode);
    }
}
