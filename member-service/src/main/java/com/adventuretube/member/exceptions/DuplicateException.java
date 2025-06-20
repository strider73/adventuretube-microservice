package com.adventuretube.member.exceptions;

import com.adventuretube.member.exceptions.base.BaseServiceException;
import com.adventuretube.member.exceptions.code.MemberErrorCode;

public class DuplicateException extends BaseServiceException {

  public DuplicateException(MemberErrorCode errorCode) {
    super(errorCode);
  }
}
