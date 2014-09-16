/*
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

import com.google.inject.Singleton;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.state.Service;

import java.util.*;

/**
 * Contains metadata about actions supported by services
 */
@Singleton
public class ActionMetadata {
  private final Map<String, List<String>> serviceActions = new HashMap<String, List<String>>();
  private final Map<String, String> serviceClients = new HashMap<String, String>();
  private final Map<String, String> serviceCheckActions =
      new HashMap<String, String>();
  private final List<String> defaultHostComponentCommands = new ArrayList<String>();
  public final static String SERVICE_CHECK_POSTFIX = "_SERVICE_CHECK";
  
  private static final Map<String, String> SERVICE_CHECKS;
  static {
      Map<String, String> serviceChecks = new HashMap<String, String>();
      
      serviceChecks.put(Service.Type.ZOOKEEPER.toString(), "ZOOKEEPER_QUORUM_SERVICE_CHECK");
      
      SERVICE_CHECKS = Collections.unmodifiableMap(serviceChecks);
  }

  public ActionMetadata() {
    fillServiceClients();
    fillHostComponentCommands();
  }

  private void fillHostComponentCommands() {
    //Standart commands for any host component
    // TODO: Add START/STOP/INSTALL commands
    defaultHostComponentCommands.add("RESTART");
    defaultHostComponentCommands.add("CONFIGURE");
  }

  private void fillServiceClients() {
    serviceClients.put("hdfs"       , Role.HDFS_CLIENT.toString());
    serviceClients.put("glusterfs"  , Role.GLUSTERFS_CLIENT.toString());
    serviceClients.put("hbase"      , Role.HBASE_CLIENT.toString());
    serviceClients.put("mapreduce"  , Role.MAPREDUCE_CLIENT.toString());
    serviceClients.put("zookeeper"  , Role.ZOOKEEPER_CLIENT.toString());
    serviceClients.put("hive"       , Role.HIVE_CLIENT.toString());
    serviceClients.put("hcat"       , Role.HCAT.toString());
    serviceClients.put("oozie"      , Role.OOZIE_CLIENT.toString());
    serviceClients.put("pig"        , Role.PIG.toString());
    serviceClients.put("sqoop"      , Role.SQOOP.toString());
    serviceClients.put("yarn"       , Role.YARN_CLIENT.toString());
  }

  public List<String> getActions(String serviceName) {
    List<String> result = serviceActions.get(serviceName.toLowerCase());
    if (result != null) {
      return result;
    } else {
      return Collections.emptyList();
    }
  }

  public String getClient(String serviceName) {
    return serviceClients.get(serviceName.toLowerCase());
  }

  public String getServiceCheckAction(String serviceName) {
    return serviceCheckActions.get(serviceName.toLowerCase());
  }
  
  public void addServiceCheckAction(String serviceName) {
    String actionName = serviceName + SERVICE_CHECK_POSTFIX;
    
    if(SERVICE_CHECKS.containsKey(serviceName)) {
      actionName = SERVICE_CHECKS.get(serviceName);
    }
    
    serviceCheckActions.put(serviceName.toLowerCase(), actionName);
    serviceActions.put(serviceName.toLowerCase(), Arrays.asList(actionName));
  }

  public boolean isDefaultHostComponentCommand(String command) {
    if (command != null && defaultHostComponentCommands.contains(command)) {
      return true;
    }
    return false;
  }
}
