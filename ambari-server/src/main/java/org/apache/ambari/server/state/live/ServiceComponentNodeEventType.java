package org.apache.ambari.server.state.live;

public enum ServiceComponentNodeEventType {
  /**
   * Operation in progress
   */
  NODE_SVCCOMP_OP_IN_PROGRESS,
  /**
   * Operation succeeded
   */
  NODE_SVCCOMP_OP_SUCCEEDED,
  /**
   * Operation failed.
   */
  NODE_SVCCOMP_OP_FAILED,
  /**
   * Re-starting a failed operation.
   */
  NODE_SVCCOMP_OP_RESTART,
  /**
   * Triggering an install.
   */
  NODE_SVCCOMP_INSTALL,
  /**
   * Triggering a start.
   */
  NODE_SVCCOMP_START,
  /**
   * Triggering a stop.
   */
  NODE_SVCCOMP_STOP,
  /**
   * Triggering an uninstall.
   */
  NODE_SVCCOMP_UNINSTALL,
  /**
   * Triggering a wipe-out ( restore to clean state ).
   */
  NODE_SVCCOMP_WIPEOUT
}
