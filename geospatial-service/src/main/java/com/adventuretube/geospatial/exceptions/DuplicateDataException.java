package com.adventuretube.geospatial.exceptions;

import com.adventuretube.geospatial.exceptions.base.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class DuplicateDataException extends BaseServiceException {

    public DuplicateDataException(GeoErrorCode geoErrorCode) {
        super(geoErrorCode);
    }
}
