package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class EnvFileException extends BaseServiceException {

    public EnvFileException(GeoErrorCode errorCode) {
        super(errorCode);
    }

    public EnvFileException(GeoErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
