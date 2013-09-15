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

  public ActionMetadata() {
    fillServiceActions();
    fillServiceClients();
    fillServiceCheckActions();
  }

  private void fillServiceClients() {
    serviceClients.put("hdfs"       , Role.HDFS_CLIENT.toString());
    serviceClients.put("hcfs"       , Role.HCFS_CLIENT.toString());
    serviceClients.put("hbase"      , Role.HBASE_CLIENT.toString());
    serviceClients.put("mapreduce"  , Role.MAPREDUCE_CLIENT.toString());
    serviceClients.put("zookeeper"  , Role.ZOOKEEPER_CLIENT.toString());
    serviceClients.put("hive"       , Role.HIVE_CLIENT.toString());
    serviceClients.put("hcat"       , Role.HCAT.toString());
    serviceClients.put("oozie"      , Role.OOZIE_CLIENT.toString());
    serviceClients.put("pig"        , Role.PIG.toString());
    serviceClients.put("sqoop"      , Role.SQOOP.toString());
  }

  private void fillServiceActions() {
    serviceActions.put("hdfs"       , Arrays.asList(Role.HDFS_SERVICE_CHECK.toString(),
                                                    Role.DECOMMISSION_DATANODE.toString()));
    serviceActions.put("hcfs"       , Arrays.asList(Role.HCFS_SERVICE_CHECK.toString()));
    serviceActions.put("hbase"      , Arrays.asList(Role.HBASE_SERVICE_CHECK.toString()));
    serviceActions.put("mapreduce"  , Arrays.asList(Role.MAPREDUCE_SERVICE_CHECK.toString()));
    serviceActions.put("mapreduce2" , Arrays.asList(Role.MAPREDUCE2_SERVICE_CHECK.toString()));
    serviceActions.put("yarn"       , Arrays.asList(Role.YARN_SERVICE_CHECK.toString()));
    serviceActions.put("zookeeper"  , Arrays.asList(Role.ZOOKEEPER_QUORUM_SERVICE_CHECK.toString()));
    serviceActions.put("hive"       , Arrays.asList(Role.HIVE_SERVICE_CHECK.toString()));
    serviceActions.put("hcat"       , Arrays.asList(Role.HCAT_SERVICE_CHECK.toString()));
    serviceActions.put("oozie"      , Arrays.asList(Role.OOZIE_SERVICE_CHECK.toString()));
    serviceActions.put("pig"        , Arrays.asList(Role.PIG_SERVICE_CHECK.toString()));
    serviceActions.put("sqoop"      , Arrays.asList(Role.SQOOP_SERVICE_CHECK.toString()));
    serviceActions.put("webhcat"  , Arrays.asList(Role.WEBHCAT_SERVICE_CHECK.toString()));
  }

  private void fillServiceCheckActions() {
    serviceCheckActions.put("hdfs", Role.HDFS_SERVICE_CHECK.toString());
    serviceCheckActions.put("hcfs", Role.HCFS_SERVICE_CHECK.toString());
    serviceCheckActions.put("hbase", Role.HBASE_SERVICE_CHECK.toString());
    serviceCheckActions.put("mapreduce",
        Role.MAPREDUCE_SERVICE_CHECK.toString());
    serviceCheckActions.put("mapreduce2",
        Role.MAPREDUCE2_SERVICE_CHECK.toString());
    serviceCheckActions.put("yarn",
        Role.YARN_SERVICE_CHECK.toString());
    serviceCheckActions.put("zookeeper",
        Role.ZOOKEEPER_QUORUM_SERVICE_CHECK.toString());
    serviceCheckActions.put("hive", Role.HIVE_SERVICE_CHECK.toString());
    serviceCheckActions.put("hcat", Role.HCAT_SERVICE_CHECK.toString());
    serviceCheckActions.put("oozie", Role.OOZIE_SERVICE_CHECK.toString());
    serviceCheckActions.put("pig", Role.PIG_SERVICE_CHECK.toString());
    serviceCheckActions.put("sqoop", Role.SQOOP_SERVICE_CHECK.toString());
    serviceCheckActions.put("webhcat",
        Role.WEBHCAT_SERVICE_CHECK.toString());
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
}
