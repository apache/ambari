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
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to establish the order between two roles. This class
 * should not be used to determine the dependencies.
 */
public class RoleCommandOrder {

  private final static Logger LOG =
			LoggerFactory.getLogger(RoleCommandOrder.class);
	
  private static class RoleCommandPair {
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
  }

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  /**
   * key -> blocked role command value -> set of blocker role commands.
   */
  private Map<RoleCommandPair, Set<RoleCommandPair>> dependencies = new HashMap<RoleCommandPair, Set<RoleCommandPair>>();

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

  public void initialize(StackId stackId) {
    Boolean hasHCFS = false;
    // Installs
    if (hasHCFS) {
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.HIVE_CLIENT,
        RoleCommand.INSTALL);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.HCAT,
        RoleCommand.INSTALL);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.MAPREDUCE_CLIENT,
        RoleCommand.INSTALL);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.OOZIE_CLIENT,
        RoleCommand.INSTALL);

      // Starts
      addDependency(Role.HBASE_MASTER, RoleCommand.START, Role.ZOOKEEPER_SERVER,
          RoleCommand.START);
      addDependency(Role.HBASE_MASTER, RoleCommand.START, Role.PEERSTATUS,
          RoleCommand.START);
      addDependency(Role.HBASE_REGIONSERVER, RoleCommand.START,
          Role.HBASE_MASTER, RoleCommand.START);
      addDependency(Role.JOBTRACKER, RoleCommand.START, Role.PEERSTATUS,
          RoleCommand.START);
      addDependency(Role.TASKTRACKER, RoleCommand.START, Role.PEERSTATUS,
          RoleCommand.START);
      addDependency(Role.OOZIE_SERVER, RoleCommand.START, Role.JOBTRACKER,
          RoleCommand.START);
      addDependency(Role.OOZIE_SERVER, RoleCommand.START, Role.TASKTRACKER,
          RoleCommand.START);
      addDependency(Role.HIVE_SERVER, RoleCommand.START, Role.TASKTRACKER,
          RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.START, Role.TASKTRACKER,
          RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.START, Role.HIVE_SERVER,
          RoleCommand.START);
      addDependency(Role.HIVE_METASTORE, RoleCommand.START, Role.MYSQL_SERVER,
          RoleCommand.START);
      addDependency(Role.HIVE_SERVER, RoleCommand.START, Role.MYSQL_SERVER,
          RoleCommand.START);
      addDependency(Role.HUE_SERVER, RoleCommand.START, Role.HIVE_SERVER,
          RoleCommand.START);
      addDependency(Role.HUE_SERVER, RoleCommand.START, Role.HCAT,
          RoleCommand.START);
      addDependency(Role.HUE_SERVER, RoleCommand.START, Role.OOZIE_SERVER,
          RoleCommand.START);

      // Service checks
      addDependency(Role.HCFS_SERVICE_CHECK, RoleCommand.EXECUTE, Role.PEERSTATUS,
          RoleCommand.START);
      addDependency(Role.MAPREDUCE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.JOBTRACKER, RoleCommand.START);
      addDependency(Role.MAPREDUCE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.TASKTRACKER, RoleCommand.START);
      addDependency(Role.OOZIE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.OOZIE_SERVER, RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.WEBHCAT_SERVER, RoleCommand.START);
      addDependency(Role.HBASE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HBASE_MASTER, RoleCommand.START);
      addDependency(Role.HBASE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HBASE_REGIONSERVER, RoleCommand.START);
      addDependency(Role.HIVE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HIVE_SERVER, RoleCommand.START);
      addDependency(Role.HIVE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HIVE_METASTORE, RoleCommand.START);
      addDependency(Role.HCAT_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HIVE_SERVER, RoleCommand.START);
      addDependency(Role.PIG_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.JOBTRACKER, RoleCommand.START);
      addDependency(Role.PIG_SERVICE_CHECK, RoleCommand.EXECUTE,
         Role.TASKTRACKER, RoleCommand.START);
      addDependency(Role.SQOOP_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.JOBTRACKER, RoleCommand.START);
      addDependency(Role.SQOOP_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.TASKTRACKER, RoleCommand.START);
      addDependency(Role.ZOOKEEPER_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.ZOOKEEPER_SERVER, RoleCommand.START);
      addDependency(Role.ZOOKEEPER_QUORUM_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.ZOOKEEPER_SERVER, RoleCommand.START);

      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.STOP,
          Role.HBASE_MASTER, RoleCommand.STOP);
      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.STOP,
          Role.HBASE_REGIONSERVER, RoleCommand.STOP);
      addDependency(Role.HBASE_MASTER, RoleCommand.STOP,
          Role.HBASE_REGIONSERVER, RoleCommand.STOP);

      addDependency(Role.JOBTRACKER, RoleCommand.UPGRADE,
          Role.HCFS_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.TASKTRACKER, RoleCommand.UPGRADE,
          Role.JOBTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE,
          Role.TASKTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE,
          Role.JOBTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.UPGRADE,
          Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.ZOOKEEPER_CLIENT, RoleCommand.UPGRADE,
          Role.ZOOKEEPER_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_MASTER, RoleCommand.UPGRADE,
          Role.ZOOKEEPER_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_REGIONSERVER, RoleCommand.UPGRADE,
          Role.HBASE_MASTER, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_CLIENT, RoleCommand.UPGRADE,
          Role.HBASE_REGIONSERVER, RoleCommand.UPGRADE);

      addDependency(Role.JOBTRACKER, RoleCommand.UPGRADE,
          Role.HCFS_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.TASKTRACKER, RoleCommand.UPGRADE,
          Role.JOBTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE,
          Role.TASKTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE,
          Role.JOBTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.UPGRADE,
          Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.ZOOKEEPER_CLIENT, RoleCommand.UPGRADE,
          Role.ZOOKEEPER_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_MASTER, RoleCommand.UPGRADE,
          Role.ZOOKEEPER_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_REGIONSERVER, RoleCommand.UPGRADE,
          Role.HBASE_MASTER, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_CLIENT, RoleCommand.UPGRADE,
          Role.HBASE_REGIONSERVER, RoleCommand.UPGRADE);
      addDependency(Role.HIVE_SERVER, RoleCommand.UPGRADE,
          Role.HBASE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.HIVE_METASTORE, RoleCommand.UPGRADE,
          Role.HIVE_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.MYSQL_SERVER, RoleCommand.UPGRADE,
          Role.HIVE_METASTORE, RoleCommand.UPGRADE);
      addDependency(Role.HIVE_CLIENT, RoleCommand.UPGRADE,
          Role.MYSQL_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.HCAT, RoleCommand.UPGRADE,
          Role.HIVE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.OOZIE_SERVER, RoleCommand.UPGRADE,
          Role.HCAT, RoleCommand.UPGRADE);
      addDependency(Role.OOZIE_CLIENT, RoleCommand.UPGRADE,
          Role.OOZIE_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.UPGRADE,
          Role.OOZIE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.PIG, RoleCommand.UPGRADE,
          Role.WEBHCAT_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.SQOOP, RoleCommand.UPGRADE,
          Role.PIG, RoleCommand.UPGRADE);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.UPGRADE,
          Role.SQOOP, RoleCommand.UPGRADE);
      addDependency(Role.GANGLIA_SERVER, RoleCommand.UPGRADE,
          Role.NAGIOS_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.GANGLIA_MONITOR, RoleCommand.UPGRADE,
          Role.GANGLIA_SERVER, RoleCommand.UPGRADE);
    } else {
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.HIVE_CLIENT,
        RoleCommand.INSTALL);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.HCAT,
        RoleCommand.INSTALL);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.MAPREDUCE_CLIENT,
        RoleCommand.INSTALL);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.INSTALL, Role.OOZIE_CLIENT,
        RoleCommand.INSTALL);

      // Starts
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
      addDependency(Role.TASKTRACKER, RoleCommand.START, Role.NAMENODE,
          RoleCommand.START);
      addDependency(Role.TASKTRACKER, RoleCommand.START, Role.DATANODE,
          RoleCommand.START);
      addDependency(Role.OOZIE_SERVER, RoleCommand.START, Role.JOBTRACKER,
          RoleCommand.START);
      addDependency(Role.OOZIE_SERVER, RoleCommand.START, Role.TASKTRACKER,
          RoleCommand.START);
      addDependency(Role.HIVE_SERVER, RoleCommand.START, Role.TASKTRACKER,
          RoleCommand.START);
      addDependency(Role.HIVE_SERVER, RoleCommand.START, Role.DATANODE,
          RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.START, Role.TASKTRACKER,
          RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.START, Role.DATANODE,
          RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.START, Role.HIVE_SERVER,
          RoleCommand.START);
      addDependency(Role.HIVE_METASTORE, RoleCommand.START, Role.MYSQL_SERVER,
          RoleCommand.START);
      addDependency(Role.HIVE_SERVER, RoleCommand.START, Role.MYSQL_SERVER,
          RoleCommand.START);
      addDependency(Role.HUE_SERVER, RoleCommand.START, Role.HIVE_SERVER,
          RoleCommand.START);
      addDependency(Role.HUE_SERVER, RoleCommand.START, Role.HCAT,
          RoleCommand.START);
      addDependency(Role.HUE_SERVER, RoleCommand.START, Role.OOZIE_SERVER,
          RoleCommand.START);

      // Service checks
      addDependency(Role.HDFS_SERVICE_CHECK, RoleCommand.EXECUTE, Role.NAMENODE,
          RoleCommand.START);
      addDependency(Role.HDFS_SERVICE_CHECK, RoleCommand.EXECUTE, Role.DATANODE,
          RoleCommand.START);
      addDependency(Role.MAPREDUCE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.JOBTRACKER, RoleCommand.START);
      addDependency(Role.MAPREDUCE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.TASKTRACKER, RoleCommand.START);
      addDependency(Role.OOZIE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.OOZIE_SERVER, RoleCommand.START);
      addDependency(Role.WEBHCAT_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.WEBHCAT_SERVER, RoleCommand.START);
      addDependency(Role.HBASE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HBASE_MASTER, RoleCommand.START);
      addDependency(Role.HBASE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HBASE_REGIONSERVER, RoleCommand.START);
      addDependency(Role.HIVE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HIVE_SERVER, RoleCommand.START);
      addDependency(Role.HIVE_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HIVE_METASTORE, RoleCommand.START);
      addDependency(Role.HCAT_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.HIVE_SERVER, RoleCommand.START);
      addDependency(Role.PIG_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.JOBTRACKER, RoleCommand.START);
      addDependency(Role.PIG_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.TASKTRACKER, RoleCommand.START);
      addDependency(Role.SQOOP_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.JOBTRACKER, RoleCommand.START);
      addDependency(Role.SQOOP_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.TASKTRACKER, RoleCommand.START);
      addDependency(Role.ZOOKEEPER_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.ZOOKEEPER_SERVER, RoleCommand.START);
      addDependency(Role.ZOOKEEPER_QUORUM_SERVICE_CHECK, RoleCommand.EXECUTE,
          Role.ZOOKEEPER_SERVER, RoleCommand.START);

      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.STOP,
          Role.HBASE_MASTER, RoleCommand.STOP);
      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.STOP,
          Role.HBASE_REGIONSERVER, RoleCommand.STOP);
      addDependency(Role.NAMENODE, RoleCommand.STOP,
          Role.HBASE_MASTER, RoleCommand.STOP);
      addDependency(Role.DATANODE, RoleCommand.STOP,
          Role.HBASE_MASTER, RoleCommand.STOP);
      addDependency(Role.HBASE_MASTER, RoleCommand.STOP,
          Role.HBASE_REGIONSERVER, RoleCommand.STOP);
      addDependency(Role.NAMENODE, RoleCommand.STOP,
          Role.JOBTRACKER, RoleCommand.STOP);
      addDependency(Role.NAMENODE, RoleCommand.STOP,
          Role.TASKTRACKER, RoleCommand.STOP);
      addDependency(Role.DATANODE, RoleCommand.STOP,
          Role.JOBTRACKER, RoleCommand.STOP);
      addDependency(Role.DATANODE, RoleCommand.STOP,
          Role.TASKTRACKER, RoleCommand.STOP);

      addDependency(Role.SECONDARY_NAMENODE, RoleCommand.UPGRADE,
          Role.NAMENODE, RoleCommand.UPGRADE);
      addDependency(Role.DATANODE, RoleCommand.UPGRADE,
          Role.SECONDARY_NAMENODE, RoleCommand.UPGRADE);
      addDependency(Role.HDFS_CLIENT, RoleCommand.UPGRADE,
          Role.DATANODE, RoleCommand.UPGRADE);
      addDependency(Role.JOBTRACKER, RoleCommand.UPGRADE,
          Role.HDFS_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.TASKTRACKER, RoleCommand.UPGRADE,
          Role.JOBTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE,
          Role.TASKTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE,
          Role.TASKTRACKER, RoleCommand.UPGRADE);
      addDependency(Role.ZOOKEEPER_SERVER, RoleCommand.UPGRADE,
          Role.MAPREDUCE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.ZOOKEEPER_CLIENT, RoleCommand.UPGRADE,
          Role.ZOOKEEPER_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_MASTER, RoleCommand.UPGRADE,
          Role.ZOOKEEPER_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_REGIONSERVER, RoleCommand.UPGRADE,
          Role.HBASE_MASTER, RoleCommand.UPGRADE);
      addDependency(Role.HBASE_CLIENT, RoleCommand.UPGRADE,
          Role.HBASE_REGIONSERVER, RoleCommand.UPGRADE);
      addDependency(Role.HIVE_SERVER, RoleCommand.UPGRADE,
          Role.HBASE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.HIVE_METASTORE, RoleCommand.UPGRADE,
          Role.HIVE_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.MYSQL_SERVER, RoleCommand.UPGRADE,
          Role.HIVE_METASTORE, RoleCommand.UPGRADE);
      addDependency(Role.HIVE_CLIENT, RoleCommand.UPGRADE,
          Role.MYSQL_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.HCAT, RoleCommand.UPGRADE,
          Role.HIVE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.OOZIE_SERVER, RoleCommand.UPGRADE,
          Role.HCAT, RoleCommand.UPGRADE);
      addDependency(Role.OOZIE_CLIENT, RoleCommand.UPGRADE,
          Role.OOZIE_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.WEBHCAT_SERVER, RoleCommand.UPGRADE,
          Role.OOZIE_CLIENT, RoleCommand.UPGRADE);
      addDependency(Role.PIG, RoleCommand.UPGRADE,
          Role.WEBHCAT_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.SQOOP, RoleCommand.UPGRADE,
          Role.PIG, RoleCommand.UPGRADE);
      addDependency(Role.NAGIOS_SERVER, RoleCommand.UPGRADE,
          Role.SQOOP, RoleCommand.UPGRADE);
      addDependency(Role.GANGLIA_SERVER, RoleCommand.UPGRADE,
          Role.NAGIOS_SERVER, RoleCommand.UPGRADE);
      addDependency(Role.GANGLIA_MONITOR, RoleCommand.UPGRADE,
          Role.GANGLIA_SERVER, RoleCommand.UPGRADE);
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
   * Adds transitive dependencies to each node.
   * A => B and B => C implies A => B,C and B => C
   */
  private void extendTransitiveDependency() {
    for (RoleCommandPair rcp : this.dependencies.keySet()) {
      HashSet<RoleCommandPair> visited = new HashSet<RoleCommandPair>();
      HashSet<RoleCommandPair> transitiveDependencies = new HashSet<RoleCommandPair>();
      for (RoleCommandPair directlyBlockedOn : this.dependencies.get(rcp)) {
        visited.add(directlyBlockedOn);
        identifyTransitiveDependencies(directlyBlockedOn, visited, transitiveDependencies);
      }
      if (transitiveDependencies.size() > 0) {
        this.dependencies.get(rcp).addAll(transitiveDependencies);
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
    RoleCommand rc1 = rgn1.getCommand();
    RoleCommand rc2 = rgn2.getCommand();
    if (rc1.equals(rc2)) {
      //If its coming here means roles have no dependencies.
      return 0;
    }
   
    if ((rc1.equals(RoleCommand.START) && rc2.equals(RoleCommand.EXECUTE)) ||
        (rc2.equals(RoleCommand.START) && rc1.equals(RoleCommand.EXECUTE))) {
      //START and execute are independent, role order matters
      return 0;
    }
    
    if (rc1.equals(RoleCommand.INSTALL)) {
      return -1;
    } else if (rc2.equals(RoleCommand.INSTALL)) {
      return 1;
    } else if (rc1.equals(RoleCommand.START) || rc1.equals(RoleCommand.EXECUTE)) {
      return -1;
    } else if (rc2.equals(RoleCommand.START) || rc2.equals(RoleCommand.EXECUTE)) {
      return 1;
    } else if (rc1.equals(RoleCommand.STOP)) {
      return -1;
    } else if (rc2.equals(RoleCommand.STOP)) {
      return 1;
    }
    return 0;
  }
}
