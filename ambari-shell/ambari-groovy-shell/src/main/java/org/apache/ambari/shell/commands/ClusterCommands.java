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

import static org.apache.ambari.shell.support.TableRenderer.renderMultiValueMap;
import static org.apache.ambari.shell.support.TableRenderer.renderSingleMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.groovy.client.InvalidHostGroupHostAssociation;
import org.apache.ambari.shell.completion.Blueprint;
import org.apache.ambari.shell.completion.Host;
import org.apache.ambari.shell.flash.FlashService;
import org.apache.ambari.shell.model.AmbariContext;
import org.apache.ambari.shell.model.FocusType;
import org.apache.ambari.shell.model.Hints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import groovyx.net.http.HttpResponseException;

/**
 * Cluster related commands used in the shell.
 *
 * @see org.apache.ambari.groovy.client.AmbariClient
 */
@Component
public class ClusterCommands implements CommandMarker {

  private AmbariClient client;
  private AmbariContext context;
  private FlashService flashService;
  private Map<String, List<String>> hostGroups;

  @Autowired
  public ClusterCommands(AmbariClient client, AmbariContext context, FlashService flashService) {
    this.client = client;
    this.context = context;
    this.flashService = flashService;
  }

  /**
   * Checks whether the cluster build command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster build")
  public boolean isClusterBuildCommandAvailable() {
    return !context.isConnectedToCluster() && !context.isFocusOnClusterBuild() && context.areBlueprintsAvailable();
  }

  /**
   * Sets the focus on cluster building. Takes a blueprint id, if it does not exists it wont focus.
   * After focus the users are able to assign hosts to host groups.
   *
   * @param id id of the blueprint
   * @return prints the blueprint as formatted table if exists, otherwise error message
   */
  @CliCommand(value = "cluster build", help = "Starts to build a cluster")
  public String buildCluster(
    @CliOption(key = "blueprint", mandatory = true, help = "Id of the blueprint, use 'blueprints' command to see the list") Blueprint id) {
    String message;
    String blueprint = id.getName();
    if (client.doesBlueprintExist(blueprint)) {
      context.setFocus(blueprint, FocusType.CLUSTER_BUILD);
      context.setHint(Hints.ASSIGN_HOSTS);
      message = String.format("%s\n%s",
        renderSingleMap(client.getHostNames(), "HOSTNAME", "STATE"),
        renderMultiValueMap(client.getBlueprintMap(blueprint), "HOSTGROUP", "COMPONENT"));
      createNewHostGroups();
    } else {
      message = "Not a valid blueprint id";
    }
    return message;
  }

  /**
   * Checks whether the cluster assign command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster assign")
  public boolean isAssignCommandAvailable() {
    return context.isFocusOnClusterBuild();
  }

  /**
   * Assign hosts to host groups provided in the blueprint.
   *
   * @param host  host to assign
   * @param group which host group to
   * @return status message
   */
  @CliCommand(value = "cluster assign", help = "Assign host to host group")
  public String assign(
    @CliOption(key = "host", mandatory = true, help = "Fully qualified host name") Host host,
    @CliOption(key = "hostGroup", mandatory = true, help = "Host group which to assign the host") String group) {
    String message;
    String hostName = host.getName();
    if (client.getHostNames().keySet().contains(hostName)) {
      if (addHostToGroup(hostName, group)) {
        context.setHint(Hints.CREATE_CLUSTER);
        message = String.format("%s has been added to %s", hostName, group);
      } else {
        message = String.format("%s is not a valid host group", group);
      }
    } else {
      message = String.format("%s is not a valid hostname", hostName);
    }
    return message;
  }

  /**
   * Checks whether the cluster auto command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator(value = "cluster autoAssign")
  public boolean isClusterAutoAssignAvailable() {
    return context.isFocusOnClusterBuild() && !isHostAssigned();
  }

  /**
   * Tries to auto associate hosts to host groups.
   *
   * @return prints the auto assignments
   */
  @CliCommand(value = "cluster autoAssign", help = "Automatically assigns hosts to different host groups base on the provided strategy")
  public String autoAssign() throws InvalidHostGroupHostAssociation {
    Map<String, List<String>> assignments = client.recommendAssignments(context.getFocusValue());
    if (!assignments.isEmpty()) {
      hostGroups = assignments;
      context.setHint(Hints.CREATE_CLUSTER);
    }
    return showAssignments();
  }

  /**
   * Checks whether the cluster preview command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster preview")
  public boolean isClusterPreviewCommandAvailable() {
    return context.isFocusOnClusterBuild() && isHostAssigned();
  }

  /**
   * Shows the currently assigned hosts.
   *
   * @return formatted host - host group table
   */
  @CliCommand(value = "cluster preview", help = "Shows the currently assigned hosts")
  public String showAssignments() {
    return renderMultiValueMap(hostGroups, "HOSTGROUP", "HOST");
  }

  /**
   * Checks whether the cluster create command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster create")
  public boolean isCreateClusterCommandAvailable() {
    return context.isFocusOnClusterBuild() && isHostAssigned();
  }

  /**
   * Creates a new cluster based on the provided host - host group associations and the selected blueprint.
   * If the cluster creation fails, deletes the cluster.
   *
   * @return status message
   */
  @CliCommand(value = "cluster create", help = "Create a cluster based on current blueprint and assigned hosts")
  public String createCluster(
    @CliOption(key = "exitOnFinish", mandatory = false, help = "Quits the shell when the cluster creation finishes") Boolean exit) {
    String message = "Successfully created the cluster";
    String blueprint = context.getFocusValue();
    try {
      client.createCluster(blueprint, blueprint, hostGroups);
      context.setCluster(blueprint);
      context.resetFocus();
      context.setHint(Hints.PROGRESS);
      flashService.showInstallProgress(exit == null ? false : exit);
    } catch (HttpResponseException e) {
      createNewHostGroups();
      message = "Failed to create the cluster: " + e.getMessage();
      try {
        deleteCluster(blueprint);
      } catch (HttpResponseException e1) {
        message += ". Failed to cleanup cluster creation: " + e1.getMessage();
      }
    }
    return message;
  }

  /**
   * Checks whether the cluster delete command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator("cluster delete")
  public boolean isDeleteClusterCommandAvailable() {
    return context.isConnectedToCluster();
  }

  /**
   * Deletes the cluster.
   *
   * @return status message
   */
  @CliCommand(value = "cluster delete", help = "Delete the cluster")
  public String deleteCluster() {
    String message = "Successfully deleted the cluster";
    try {
      deleteCluster(context.getCluster());
    } catch (HttpResponseException e) {
      message = "Could not delete the cluster: " + e.getMessage();
    }
    return message;
  }

  /**
   * Checks whether the cluster reset command is available or not.
   *
   * @return true if available false otherwise
   */
  @CliAvailabilityIndicator(value = "cluster reset")
  public boolean isClusterResetCommandAvailable() {
    return context.isFocusOnClusterBuild() && isHostAssigned();
  }

  @CliCommand(value = "cluster reset", help = "Clears the host - host group assignments")
  public void reset() {
    context.setHint(Hints.ASSIGN_HOSTS);
    createNewHostGroups();
  }

  private void deleteCluster(String id) throws HttpResponseException {
    client.deleteCluster(id);
  }

  private void createNewHostGroups() {
    Map<String, List<String>> groups = new HashMap<String, List<String>>();
    for (String hostGroup : client.getHostGroups(context.getFocusValue())) {
      groups.put(hostGroup, new ArrayList<String>());
    }
    this.hostGroups = groups;
  }

  private boolean addHostToGroup(String host, String group) {
    boolean result = true;
    List<String> hosts = hostGroups.get(group);
    if (hosts == null) {
      result = false;
    } else {
      hosts.add(host);
    }
    return result;
  }

  private boolean isHostAssigned() {
    boolean result = false;
    for (String group : hostGroups.keySet()) {
      if (!hostGroups.get(group).isEmpty()) {
        result = true;
        break;
      }
    }
    return result;
  }
}
