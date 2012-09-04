package org.apache.ambari.server.state.live;

public enum ServiceComponentNodeState {
  /**
   * Initial/Clean state
   */
  INIT,
  /**
   * In the process of installing.
   */
  INSTALLING,
  /**
   * Install failed
   */
  INSTALL_FAILED,
  /**
   * State when install completed successfully
   */
  INSTALLED,
  /**
   * In the process of starting.
   */
  STARTING,
  /**
   * Start failed.
   */
  START_FAILED,
  /**
   * State when start completed successfully.
   */
  STARTED,
  /**
   * In the process of stopping.
   */
  STOPPING,
  /**
   * Stop failed
   */
  STOP_FAILED,
  /**
   * In the process of uninstalling.
   */
  UNINSTALLING,
  /**
   * Uninstall failed.
   */
  UNINSTALL_FAILED,
  /**
   * State when uninstall completed successfully.
   */
  UNINSTALLED
}
