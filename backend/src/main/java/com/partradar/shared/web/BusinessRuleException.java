package com.partradar.shared.web;

/** Thrown when a request is well-formed but violates a domain rule. */
public class BusinessRuleException extends RuntimeException {

  private final String code;

  public BusinessRuleException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
