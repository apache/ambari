package org.apache.ambari.server;

/**
 * Ambari unchecked exception.
 */
public class AmbariRuntimeException extends RuntimeException {
  public AmbariRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
