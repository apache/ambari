/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.shell.commands;

import static org.apache.ambari.shell.support.TableRenderer.renderMapValueMap;
import static org.apache.ambari.shell.support.TableRenderer.renderSingleMap;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.Service;
import org.apache.ambari.shell.model.AmbariContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Service related commands used in the shell.
 *
 * @see org.apache.ambari.groovy.client.AmbariClient
 */
@Component
public class ServiceCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;

  @Autowired
  public ServiceCommands(AmbariClient client, AmbariContext context) {
    this.client = client;
    this.context = context;
  }

  /**
   * Checks whether the services list command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("services list")
  public boolean isServiceListCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Prints the available services list of the Ambari Server.
   *
   * @return service list
   */
  @CliCommand(value = "services list", help = "Lists the available services")
  public String servicesList() {
    return renderSingleMap(client.getServicesMap(), "SERVICE", "STATE");
  }

  /**
   * Checks whether the services components command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("services components")
  public boolean isServiceComponentsCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Prints the services components of the Ambari Server.
   *
   * @return service component list
   */
  @CliCommand(value = "services components", help = "Lists all services with their components")
  public String serviceComponents() {
    return renderMapValueMap(client.getServiceComponentsMap(), "SERVICE", "COMPONENT", "STATE");
  }

  /**
   * Checks whether the services stop command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("services stop")
  public boolean isServiceStopCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Stops a service or all services if no service name is provided.
   *
   * @return service list
   */
  @CliCommand(value = "services stop", help = "Stops a service/all the running services")
  public String stopServices(@CliOption(key = "service", mandatory = false, help = "Name of the service to stop") Service service) {
    String message;
    try {
      if (service != null) {
        String serviceName = service.getName();
        message = "Stopping " + serviceName;
        client.stopService(serviceName);
      } else {
        message = "Stopping all services..";
        client.stopAllServices();
      }
    } catch (Exception e) {
      message = "Cannot stop services";
    }
    return String.format("%s\n\n%s", message, servicesList());
  }

  /**
   * Checks whether the services start command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("services start")
  public boolean isServiceStartCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Starts a service or all services if no service name is provided.
   *
   * @return service list
   */
  @CliCommand(value = "services start", help = "Starts a service/all the services")
  public String startServices(@CliOption(key = "service", mandatory = false, help = "Name of the service to start") Service service) {
    String message;
    try {
      if (service != null) {
        String serviceName = service.getName();
        message = "Starting " + serviceName;
        client.startService(serviceName);
      } else {
        client.startAllServices();
        message = "Starting all services..";
      }
    } catch (Exception e) {
      message = "Cannot start services";
    }
    return String.format("%s\n\n%s", message, servicesList());
  }
}
