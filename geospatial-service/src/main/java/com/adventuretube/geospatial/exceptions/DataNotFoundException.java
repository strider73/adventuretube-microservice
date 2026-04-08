package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class DataNotFoundException extends BaseServiceException {

    public DataNotFoundException(GeoErrorCode geoErrorCode) {
        super(geoErrorCode);
    }

    public DataNotFoundException(GeoErrorCode geoErrorCode, Throwable cause) {
        super(geoErrorCode, cause);
    }
}
