package com.partradar.shared.web;

/** Thrown when a requested resource does not exist or is not visible to the caller. */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String resource, Object id) {
    super(resource + " " + id + " not found");
  }
}
