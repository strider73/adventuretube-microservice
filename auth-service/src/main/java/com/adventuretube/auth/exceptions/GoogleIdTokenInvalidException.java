package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;

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
