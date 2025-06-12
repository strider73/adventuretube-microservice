package com.adventuretube.geospatial.exceptions;

import com.adventuretube.geospatial.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class EnvFileException extends BaseServiceException {

    public EnvFileException(ErrorCode errorCode) {
        super(errorCode);
    }
}
