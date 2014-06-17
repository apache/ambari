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

import static org.apache.ambari.shell.support.TableRenderer.renderSingleMap;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.Host;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.FocusType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Host related commands used in the shell.
 *
 * @see org.apache.ambari.groovy.client.AmbariClient
 */
@Component
public class HostCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;

  @Autowired
  public HostCommands(AmbariClient client, AmbariContext context) {
    this.client = client;
    this.context = context;
  }

  /**
   * Checks whether the host list command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("host list")
  public boolean isHostsCommandAvailable() {
    return true;
  }

  /**
   * Prints the available hosts of the Ambari Server.
   *
   * @return host list
   */
  @CliCommand(value = "host list", help = "Lists the available hosts")
  public String hosts() {
    return client.showHostList();
  }

  /**
   * Checks whether the host focus command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("host focus")
  public boolean isFocusHostCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Sets the focus to the specified host.
   *
   * @param host the host to set the focus to
   * @return status message
   */
  @CliCommand(value = "host focus", help = "Sets the useHost to the specified host")
  public String focusHost(
    @CliOption(key = "host", mandatory = true, help = "hostname") Host host) {
    String message;
    String hostName = host.getName();
    if (client.getHostNames().keySet().contains(hostName)) {
      context.setFocus(hostName, FocusType.HOST);
      message = "Focus set to: " + hostName;
    } else {
      message = hostName + " is not a valid host name";
    }
    return message;
  }

  /**
   * Checks whether the host components command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("host components")
  public boolean isHostComponentsCommandAvailable() {
    return context.isFocusOnHost();
  }

  /**
   * Prints the components which belongs to the host being focused on.
   *
   * @return list of host components
   */
  @CliCommand(value = "host components", help = "Lists the components assigned to the selected host")
  public String hostComponents() {
    return renderSingleMap(client.getHostComponentsMap(context.getFocusValue()), "COMPONENT", "STATE");
  }
}
