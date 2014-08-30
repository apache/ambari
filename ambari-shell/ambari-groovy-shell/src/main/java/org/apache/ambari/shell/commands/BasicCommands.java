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
import org.apache.ambari.shell.model.AmbariContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Basic commands used in the shell. Delegating the commands
 * to the Ambari Server via a Groovy based client.
 *
 * @see org.apache.ambari.groovy.client.AmbariClient
 */
@Component
public class BasicCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;

  @Autowired
  public BasicCommands(AmbariClient client, AmbariContext context) {
    this.client = client;
    this.context = context;
  }

  /**
   * Checks whether the tasks command is available or not.
   *
   * @return true if its available false otherwise
   */
  @CliAvailabilityIndicator("tasks")
  public boolean isTasksCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Prints the tasks of the Ambari Server.
   *
   * @param id id of the request
   * @return task list
   */
  @CliCommand(value = "tasks", help = "Lists the Ambari tasks")
  public String tasks(
    @CliOption(key = "id", mandatory = false, help = "Id of the request; default is: 1", unspecifiedDefaultValue = "1") String id) {
    return renderSingleMap(client.getTaskMap(id), "TASK", "STATUS");
  }

  /**
   * Checks whether the debug on command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("debug on")
  public boolean isDebugOnCommandAvailable() {
    return !client.isDebugEnabled();
  }

  /**
   * Turns the debug on. From now on users will see the URLs of the API calls.
   *
   * @return status message
   */
  @CliCommand(value = "debug on", help = "Shows the URL of the API calls")
  public String debugOn() {
    client.setDebugEnabled(true);
    return "debug enabled";
  }

  /**
   * Checks whether the debug off command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("debug off")
  public boolean isDebugOffCommandAvailable() {
    return client.isDebugEnabled();
  }

  /**
   * Turns the debug off. URLs are not visible anymore.
   *
   * @return status message
   */
  @CliCommand(value = "debug off", help = "Stops showing the URL of the API calls")
  public String debugOff() {
    client.setDebugEnabled(false);
    return "debug disabled";
  }

  /**
   * Checks whether the hint command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("hint")
  public boolean isHintCommandAvailable() {
    return true;
  }

  /**
   * Provides some hints what you can do in the current context.
   *
   * @return hint message
   */
  @CliCommand(value = "hint", help = "Shows some hints")
  public String hint() {
    return context.getHint();
  }

}
