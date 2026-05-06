package com.adventuretube.web.exceptions;

import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.web.exceptions.code.WebErrorCode;

public class ScreenShotJobStatusNotFoundException extends BaseServiceException {

    public ScreenShotJobStatusNotFoundException(WebErrorCode errorCode) { super(errorCode);}
    public ScreenShotJobStatusNotFoundException(WebErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
