package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class DuplicateDataException extends BaseServiceException {

    public DuplicateDataException(GeoErrorCode geoErrorCode) {
        super(geoErrorCode);
    }

    public DuplicateDataException(GeoErrorCode geoErrorCode, Throwable cause) {
        super(geoErrorCode, cause);
    }
}
