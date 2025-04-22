package com.adventuretube.auth.exceptions;

public class GoogleIdTokenInvalidException extends  RuntimeException{

      private final AuthErrorCode errorCode;

      public GoogleIdTokenInvalidException(AuthErrorCode errorCode) {
          super(errorCode.getMessage());
          this.errorCode = errorCode;
      }

      public AuthErrorCode getErrorCode() {
          return errorCode;
      }
}
