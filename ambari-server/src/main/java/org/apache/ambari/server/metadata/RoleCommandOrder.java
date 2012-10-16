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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.stageplanner.RoleGraphNode;

/**
 * This class is used to establish the order between two roles. This class
 * should not be used to determine the dependencies.
 */
public class RoleCommandOrder {

  private static class RoleCommandPair {
    Role role;
    RoleCommand cmd;

    public RoleCommandPair(Role _role, RoleCommand _cmd) {
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
  }

  /**
   * key -> blocked role command value -> set of blocker role commands.
   */
  private static Map<RoleCommandPair, Set<RoleCommandPair>> dependencies = new HashMap<RoleCommandPair, Set<RoleCommandPair>>();

  private static void addDependency(Role blockedRole,
      RoleCommand blockedCommand, Role blockerRole, RoleCommand blockerCommand) {
    RoleCommandPair rcp1 = new RoleCommandPair(blockedRole, blockedCommand);
    RoleCommandPair rcp2 = new RoleCommandPair(blockerRole, blockerCommand);
    if (dependencies.get(rcp1) == null) {
      dependencies.put(rcp1, new HashSet<RoleCommandPair>());
    }
    dependencies.get(rcp1).add(rcp2);
  }

  public static void initialize() {
    addDependency(Role.SECONDARY_NAMENODE, RoleCommand.START, Role.NAMENODE,
        RoleCommand.START);
    addDependency(Role.HBASE_MASTER, RoleCommand.START, Role.ZOOKEEPER_SERVER,
        RoleCommand.START);
    addDependency(Role.HBASE_MASTER, RoleCommand.START, Role.NAMENODE,
        RoleCommand.START);
    addDependency(Role.HBASE_MASTER, RoleCommand.START, Role.DATANODE,
        RoleCommand.START);
    addDependency(Role.HBASE_REGIONSERVER, RoleCommand.START,
        Role.HBASE_MASTER, RoleCommand.START);
    addDependency(Role.JOBTRACKER, RoleCommand.START, Role.NAMENODE,
        RoleCommand.START);
    addDependency(Role.JOBTRACKER, RoleCommand.START, Role.DATANODE,
        RoleCommand.START);
    addDependency(Role.TASKTRACKER, RoleCommand.START, Role.JOBTRACKER,
        RoleCommand.START);
    addDependency(Role.OOZIE_SERVER, RoleCommand.START, Role.JOBTRACKER,
        RoleCommand.START);
    addDependency(Role.OOZIE_SERVER, RoleCommand.START, Role.TASKTRACKER,
        RoleCommand.START);
    addDependency(Role.TEMPLETON_SERVER, RoleCommand.START, Role.TASKTRACKER,
        RoleCommand.START);
  }

  /**
   * Returns the dependency order. -1 => rgn1 before rgn2, 0 => they can be
   * parallel 1 => rgn2 before rgn1
   * 
   * @param roleGraphNode
   * @param roleGraphNode2
   */
  public int order(RoleGraphNode rgn1, RoleGraphNode rgn2) {
    RoleCommandPair rcp1 = new RoleCommandPair(rgn1.getRole(),
        rgn1.getCommand());
    RoleCommandPair rcp2 = new RoleCommandPair(rgn2.getRole(),
        rgn2.getCommand());
    if ((dependencies.get(rcp1) != null)
        && (dependencies.get(rcp1).contains(rcp2))) {
      return 1;
    } else if ((dependencies.get(rcp2) != null)
        && (dependencies.get(rcp2).contains(rcp1))) {
      return -1;
    } else if (!rgn2.getCommand().equals(rgn1.getCommand())) {
      return compareCommands(rgn1.getCommand(), rgn2.getCommand());
    }
    return 0;
  }

  private int compareCommands(RoleCommand rc1, RoleCommand rc2) {
    if (rc1.equals(rc2)) {
      throw new IllegalArgumentException("rc1 and rc2 are same");
    } else if (rc1.equals(RoleCommand.INSTALL)) {
      return -1;
    } else if (rc2.equals(RoleCommand.INSTALL)) {
      return 1;
    } else if (rc1.equals(RoleCommand.START)) {
      return -1;
    } else if (rc2.equals(RoleCommand.START)) {
      return 1;
    } else if (rc1.equals(RoleCommand.EXECUTE)) {
      return -1;
    } else if (rc2.equals(RoleCommand.EXECUTE)) {
      return 1;
    } else if (rc1.equals(RoleCommand.STOP)) {
      return -1;
    } else if (rc2.equals(RoleCommand.STOP)) {
      return 1;
    }
    return 0;
  }
}
