package com.adventuretube.apigateway.exception;

public class DuplicateException extends RuntimeException {

  public DuplicateException(String message) {
    super(message);
  }
}
