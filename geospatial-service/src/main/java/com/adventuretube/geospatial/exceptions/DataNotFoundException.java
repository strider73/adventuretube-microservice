package com.adventuretube.geospatial.exceptions;

import com.adventuretube.geospatial.exceptions.base.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class DataNotFoundException extends BaseServiceException {

    public DataNotFoundException(GeoErrorCode geoErrorCode) {
        super(geoErrorCode);
    }
}
