package com.adventuretube.auth.exceptions;

public class AccessDeniedException extends RuntimeException {

  private final AuthErrorCode errorCode;

  public AccessDeniedException(AuthErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public AuthErrorCode getErrorCode() {
    return errorCode;
  }
}
