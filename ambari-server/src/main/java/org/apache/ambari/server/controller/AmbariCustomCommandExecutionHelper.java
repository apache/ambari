package org.apache.ambari.server.controller;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;

import java.util.Map;

public interface AmbariCustomCommandExecutionHelper {
  void validateCustomCommand(ExecuteActionRequest actionRequest) throws AmbariException;

  void addAction(ExecuteActionRequest actionRequest, Stage stage,
                 HostsMap hostsMap, Map<String, String> hostLevelParams)
      throws AmbariException;

  void addServiceCheckActionImpl(Stage stage,
                                 String hostname, String smokeTestRole,
                                 long nowTimestamp,
                                 String serviceName,
                                 String componentName,
                                 Map<String, String> roleParameters,
                                 HostsMap hostsMap,
                                 Map<String, String> hostLevelParams)
              throws AmbariException;

  void createHostAction(Cluster cluster,
                        Stage stage, ServiceComponentHost scHost,
                        Map<String, Map<String, String>> configurations,
                        Map<String, Map<String, String>> configTags,
                        RoleCommand roleCommand,
                        Map<String, String> commandParams,
                        ServiceComponentHostEvent event)
                      throws AmbariException;
}
