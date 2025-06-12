package com.adventuretube.member.exceptions;

import com.adventuretube.member.exceptions.base.BaseServiceException;
import com.adventuretube.common.api.code.ErrorCode;

public class DuplicateException extends BaseServiceException {

  public DuplicateException(ErrorCode errorCode) {
    super(errorCode);
  }
}
