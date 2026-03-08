package com.adventuretube.geospatial.exceptions;


import com.adventuretube.geospatial.exceptions.base.BaseServiceException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;

public class OwnershipMismatchException extends BaseServiceException {

     public OwnershipMismatchException(GeoErrorCode geoErrorCode) {
         super(geoErrorCode);
     }
}
