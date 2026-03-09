package com.adventuretube.geospatial.exceptions;

import com.adventuretube.geospatial.exceptions.base.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class JobNotFoundException extends BaseServiceException {

    public JobNotFoundException(GeoErrorCode geoErrorCode) {
        super(geoErrorCode);
    }
}
