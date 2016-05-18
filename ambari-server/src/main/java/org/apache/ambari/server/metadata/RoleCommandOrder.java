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
package org.apache.ambari.server.metadata;

import java.io.*;
import java.util.*;

import com.google.inject.Inject;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.ambari.server.AmbariException;

/**
 * This class is used to establish the order between two roles. This class
 * should not be used to determine the dependencies.
 */
public class RoleCommandOrder {

  @Inject Configuration configs;
  @Inject AmbariMetaInfo ambariMetaInfo;

  private final static Logger LOG =
			LoggerFactory.getLogger(RoleCommandOrder.class);

  private final static String GENERAL_DEPS_KEY = "general_deps";
  private final static String GLUSTERFS_DEPS_KEY = "optional_glusterfs";
  private final static String NO_GLUSTERFS_DEPS_KEY = "optional_no_glusterfs";
  private final static String NAMENODE_HA_DEPS_KEY = "namenode_optional_ha";
  private final static String RESOURCEMANAGER_HA_DEPS_KEY = "resourcemanager_optional_ha";
  private final static String COMMENT_STR = "_comment";

  /**
   * Commands that are independent, role order matters
   */
  private static final Set<RoleCommand> independentCommands =
          new HashSet<RoleCommand>() {{
            add(RoleCommand.START);
            add(RoleCommand.EXECUTE);
            add(RoleCommand.SERVICE_CHECK);
          }};

  static class RoleCommandPair {
    Role role;
    RoleCommand cmd;

    public RoleCommandPair(Role _role, RoleCommand _cmd) {
      if (_role == null || _cmd == null) {
        throw new IllegalArgumentException("role = "+_role+", cmd = "+_cmd);
      }
      this.role = _role;
      this.cmd = _cmd;
    }

    @Override
    public int hashCode() {
      return (role.toString() + cmd.toString()).hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other != null && (other instanceof RoleCommandPair)
          && ((RoleCommandPair) other).role.equals(role)
          && ((RoleCommandPair) other).cmd.equals(cmd)) {
        return true;
      }
      return false;
    }

    Role getRole() {
      return role;
    }

    RoleCommand getCmd() {
      return cmd;
    }

    @Override
    public String toString() {
      return "RoleCommandPair{" +
              "role=" + role +
              ", cmd=" + cmd +
              '}';
    }
  }

  /**
   * key -> blocked role command value -> set of blocker role commands.
   */
  private Map<RoleCommandPair, Set<RoleCommandPair>> dependencies =
          new HashMap<RoleCommandPair, Set<RoleCommandPair>>();



  /**
   * Add a pair of tuples where the tuple defined by the first two parameters are blocked on
   * the tuple defined by the last two pair.
   * @param blockedRole Role that is blocked
   * @param blockedCommand The command on the role that is blocked
   * @param blockerRole The role that is blocking
   * @param blockerCommand The command on the blocking role
   */
  private void addDependency(Role blockedRole,
       RoleCommand blockedCommand, Role blockerRole, RoleCommand blockerCommand) {
    RoleCommandPair rcp1 = new RoleCommandPair(blockedRole, blockedCommand);
    RoleCommandPair rcp2 = new RoleCommandPair(blockerRole, blockerCommand);
    if (this.dependencies.get(rcp1) == null) {
      this.dependencies.put(rcp1, new HashSet<RoleCommandPair>());
    }
    this.dependencies.get(rcp1).add(rcp2);
  }

  void addDependencies(Map<String, Object> jsonSection) {
    if(jsonSection == null) // in case we don't have a certain section or role_command_order.json at all.
      return;
    
    for (Object blockedObj : jsonSection.keySet()) {
      String blocked = (String) blockedObj;
      if (COMMENT_STR.equals(blocked)) {
        continue; // Skip comments
      }
      ArrayList<String> blockers = (ArrayList<String>) jsonSection.get(blocked);
      for (String blocker : blockers) {
        String [] blockedTuple = blocked.split("-");
        String blockedRole = blockedTuple[0];
        String blockedCommand = blockedTuple[1];

        String [] blockerTuple = blocker.split("-");
        String blockerRole = blockerTuple[0];
        String blockerCommand = blockerTuple[1];

        addDependency(
                Role.valueOf(blockedRole), RoleCommand.valueOf(blockedCommand),
                Role.valueOf(blockerRole), RoleCommand.valueOf(blockerCommand));
      }
    }
  }

  public void initialize(Cluster cluster) {
    Boolean hasGLUSTERFS = false;
    Boolean isNameNodeHAEnabled = false;
    Boolean isResourceManagerHAEnabled = false;

    try {
      if (cluster != null && cluster.getService("GLUSTERFS") != null) {
    	  hasGLUSTERFS = true;
      } 
    } catch (AmbariException e) {
    }

    try {
      if (cluster != null &&
              cluster.getService("HDFS") != null &&
              cluster.getService("HDFS").getServiceComponent("JOURNALNODE") != null) {
        isNameNodeHAEnabled = true;
      }
    } catch (AmbariException e) {
    }

    try {
      if (cluster != null &&
              cluster.getService("YARN") != null &&
              cluster.getService("YARN").getServiceComponent("RESOURCEMANAGER").getServiceComponentHosts().size() > 1) {
        isResourceManagerHAEnabled = true;
      }
    } catch (AmbariException e) {
    }

    StackId stackId = cluster.getCurrentStackVersion();
    StackInfo stack = null;
    try {
      stack = ambariMetaInfo.getStack(stackId.getStackName(),
            stackId.getStackVersion());
    } catch (AmbariException e) {
    }

    Map<String,Object> userData = stack.getRoleCommandOrder().getContent();
    Map<String,Object> generalSection =
      (Map<String, Object>) userData.get(GENERAL_DEPS_KEY);
    addDependencies(generalSection);
    if (hasGLUSTERFS) {
      Map<String,Object> glusterfsSection =
        (Map<String, Object>) userData.get(GLUSTERFS_DEPS_KEY);
      addDependencies(glusterfsSection);
    } else {
      Map<String,Object> noGlusterFSSection =
        (Map<String, Object>) userData.get(NO_GLUSTERFS_DEPS_KEY);
      addDependencies(noGlusterFSSection);
    }
    if (isNameNodeHAEnabled) {
      Map<String,Object> NAMENODEHASection =
        (Map<String, Object>) userData.get(NAMENODE_HA_DEPS_KEY);
      addDependencies(NAMENODEHASection);
    }
    if (isResourceManagerHAEnabled) {
      Map<String,Object> ResourceManagerHASection =
        (Map<String, Object>) userData.get(RESOURCEMANAGER_HA_DEPS_KEY);
      addDependencies(ResourceManagerHASection);
    }
    extendTransitiveDependency();
  }

  /**
   * Returns the dependency order. -1 => rgn1 before rgn2, 0 => they can be
   * parallel 1 => rgn2 before rgn1
   * 
   * @param rgn1 roleGraphNode1
   * @param rgn2 roleGraphNode2
   */
  public int order(RoleGraphNode rgn1, RoleGraphNode rgn2) {
    RoleCommandPair rcp1 = new RoleCommandPair(rgn1.getRole(),
        rgn1.getCommand());
    RoleCommandPair rcp2 = new RoleCommandPair(rgn2.getRole(),
        rgn2.getCommand());
    if ((this.dependencies.get(rcp1) != null)
        && (this.dependencies.get(rcp1).contains(rcp2))) {
      return 1;
    } else if ((this.dependencies.get(rcp2) != null)
        && (this.dependencies.get(rcp2).contains(rcp1))) {
      return -1;
    } else if (!rgn2.getCommand().equals(rgn1.getCommand())) {
      return compareCommands(rgn1, rgn2);
    }
    return 0;
  }

  /**
   * Returns transitive dependencies as a services list
   * @param service to check if it depends on another services
   * @return tramsitive services
   */
  public Set<Service> getTransitiveServices(Service service, RoleCommand cmd)
    throws AmbariException {

    Set<Service> transitiveServices = new HashSet<Service>();
    Cluster cluster = service.getCluster();

    Set<RoleCommandPair> allDeps = new HashSet<RoleCommandPair>();
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      RoleCommandPair rcp = new RoleCommandPair(Role.valueOf(sc.getName()), cmd);
      Set<RoleCommandPair> deps = this.dependencies.get(rcp);
      if (deps != null) {
        allDeps.addAll(deps);
      }
    }

    for (Service s : cluster.getServices().values()) {
      for (RoleCommandPair rcp : allDeps) {
        ServiceComponent sc = s.getServiceComponents().get(rcp.getRole().toString());
        if (sc != null) {
          transitiveServices.add(s);
          break;
        }
      }
    }

    return transitiveServices;
  }

  /**
   * Adds transitive dependencies to each node.
   * A => B and B => C implies A => B,C and B => C
   */
  private void extendTransitiveDependency() {
    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> roleCommandPairSetEntry : this.dependencies.entrySet()) {
      HashSet<RoleCommandPair> visited = new HashSet<RoleCommandPair>();
      HashSet<RoleCommandPair> transitiveDependencies = new HashSet<RoleCommandPair>();
      for (RoleCommandPair directlyBlockedOn : this.dependencies.get(roleCommandPairSetEntry.getKey())) {
        visited.add(directlyBlockedOn);
        identifyTransitiveDependencies(directlyBlockedOn, visited, transitiveDependencies);
      }
      if (transitiveDependencies.size() > 0) {
        this.dependencies.get(roleCommandPairSetEntry.getKey()).addAll(transitiveDependencies);
      }
    }
  }

  private void identifyTransitiveDependencies(RoleCommandPair rcp, HashSet<RoleCommandPair> visited,
                                                     HashSet<RoleCommandPair> transitiveDependencies) {
    if (this.dependencies.get(rcp) != null) {
      for (RoleCommandPair blockedOn : this.dependencies.get(rcp)) {
        if (!visited.contains(blockedOn)) {
          visited.add(blockedOn);
          transitiveDependencies.add(blockedOn);
          identifyTransitiveDependencies(blockedOn, visited, transitiveDependencies);
        }
      }
    }
  }

  private int compareCommands(RoleGraphNode rgn1, RoleGraphNode rgn2) {
    // TODO: add proper order comparison support for RoleCommand.ACTIONEXECUTE

    RoleCommand rc1 = rgn1.getCommand();
    RoleCommand rc2 = rgn2.getCommand();
    if (rc1.equals(rc2)) {
      //If its coming here means roles have no dependencies.
      return 0;
    }

    if (independentCommands.contains(rc1) && independentCommands.contains(rc2)) {
      return 0;
    }
    
    if (rc1.equals(RoleCommand.INSTALL)) {
      return -1;
    } else if (rc2.equals(RoleCommand.INSTALL)) {
      return 1;
    } else if (rc1.equals(RoleCommand.START) || rc1.equals(RoleCommand.EXECUTE)
            || rc1.equals(RoleCommand.SERVICE_CHECK)) {
      return -1;
    } else if (rc2.equals(RoleCommand.START) || rc2.equals(RoleCommand.EXECUTE)
            || rc2.equals(RoleCommand.SERVICE_CHECK)) {
      return 1;
    } else if (rc1.equals(RoleCommand.STOP)) {
      return -1;
    } else if (rc2.equals(RoleCommand.STOP)) {
      return 1;
    }
    return 0;
  }

  public int compareDeps(RoleCommandOrder rco) {
    Set<RoleCommandPair> v1;
    Set<RoleCommandPair> v2;
    if (this == rco) {
      return 0;
    }

    // Check for key set match
    if (!this.dependencies.keySet().equals(rco.dependencies.keySet())){
      LOG.debug("dependency keysets differ");
      return 1;
    }
    LOG.debug("dependency keysets match");

    // So far so good.  Since the keysets match, let's check the
    // actual entries against each other
    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> roleCommandPairSetEntry : this.dependencies.entrySet()) {
      v1 = this.dependencies.get(roleCommandPairSetEntry.getKey());
      v2 = rco.dependencies.get(roleCommandPairSetEntry.getKey());
      if (!v1.equals(v2)) {
        LOG.debug("different entry found for key ("
          + roleCommandPairSetEntry.getKey().role.toString() + ", "
          + roleCommandPairSetEntry.getKey().cmd.toString() + ")" );
        return 1;
      }
    }
    LOG.debug("dependency entries match");
    return 0;
  }


  /**
   * For test purposes
   */
  Map<RoleCommandPair, Set<RoleCommandPair>> getDependencies() {
    return dependencies;
  }
}
