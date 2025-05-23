package com.adventuretube.geospatial.exceptions;

import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.exceptions.base.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class EnvFileException extends BaseServiceException {

    public EnvFileException(GeoErrorCode errorCode) {
        super(errorCode);
    }
}
