package org.apache.ambari.server.fsm;

public enum NodeServiceComponentState {
  INIT,
  INSTALLING,
  INSTALL_FAILED,
  INSTALLED,
  STARTING,
  START_FAILED,
  STARTED,
  STOPPING,
  STOP_FAILED,
  UNINSTALLING,
  UNINSTALL_FAILED,
  UNINSTALLED
}
