package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class JobNotFoundException extends BaseServiceException {

    public JobNotFoundException(GeoErrorCode geoErrorCode) {
        super(geoErrorCode);
    }

    public JobNotFoundException(GeoErrorCode geoErrorCode, Throwable cause) {
        super(geoErrorCode, cause);
    }
}
