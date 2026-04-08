package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class OwnershipMismatchException extends BaseServiceException {

     public OwnershipMismatchException(GeoErrorCode geoErrorCode) {
         super(geoErrorCode);
     }
}
