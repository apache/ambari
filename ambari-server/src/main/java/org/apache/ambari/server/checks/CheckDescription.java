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
package org.apache.ambari.server.checks;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.state.stack.PrereqCheckType;

/**
 * Enum that wraps the various type, text and failure messages for the checks
 * done for Rolling Upgrades.
 */
public enum CheckDescription {

  HOSTS_HEARTBEAT(PrereqCheckType.HOST,
      "All hosts must be heartbeating with the Ambari Server unless they are in Maintenance Mode",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
            "The following hosts must be heartbeating to the Ambari Server: {{fails}}.");
      }}),

  HOSTS_MASTER_MAINTENANCE(PrereqCheckType.HOST,
      "Hosts in Maintenance Mode must not have any master components",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "The following hosts must not be in in Maintenance Mode since they host Master components: {{fails}}.");
        put(HostsMasterMaintenanceCheck.KEY_NO_UPGRADE_NAME,
          "Could not find suitable upgrade pack for %s %s to version {{version}}.");
        put(HostsMasterMaintenanceCheck.KEY_NO_UPGRADE_PACK,
          "Could not find upgrade pack named %s.");
      }}),

  HOSTS_REPOSITORY_VERSION(PrereqCheckType.HOST,
      "All hosts should have target version installed",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "The following hosts must have version {{version}} installed: {{fails}}.");
        put(HostsRepositoryVersionCheck.KEY_NO_REPO_VERSION,
          "Repository version {{version}} does not exist.");
      }}),

  SECONDARY_NAMENODE_MUST_BE_DELETED(PrereqCheckType.SERVICE,
      "The SNameNode component must be deleted from all hosts",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT, "The SNameNode component must be deleted from host: {{fails}}.");
      }}),

  SERVICES_DECOMMISSION(PrereqCheckType.SERVICE,
      "Services should not have components in decommission state",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "The following Services must not have components in decommissioned or decommissioning state: {{fails}}.");
      }}),

  SERVICES_MAINTENANCE_MODE(PrereqCheckType.SERVICE,
      "No services can be in Maintenance Mode",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "The following Services must not be in Maintenance Mode: {{fails}}.");
      }}),

  SERVICES_MR_DISTRIBUTED_CACHE(PrereqCheckType.SERVICE,
      "MapReduce should reference Hadoop libraries from the distributed cache in HDFS",
      new HashMap<String, String>() {{
        put(ServicesMapReduceDistributedCacheCheck.KEY_APP_CLASSPATH,
          "The mapred-site.xml property mapreduce.application.classpath should be set.");
        put(ServicesMapReduceDistributedCacheCheck.KEY_FRAMEWORK_PATH,
          "The mapred-site.xml property mapreduce.application.framework.path should be set.");
        put(ServicesMapReduceDistributedCacheCheck.KEY_NOT_DFS,
          "The mapred-site.xml property mapreduce.application.framework.path or the core-site.xml property fs.defaultFS should point to *dfs:/ url.");
      }}),

  SERVICES_NAMENODE_HA(PrereqCheckType.SERVICE,
      "NameNode High Availability must  be enabled",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "NameNode High Availability is not enabled. Verify that dfs.nameservices property is present in hdfs-site.xml.");
      }}),


  SERVICES_TEZ_DISTRIBUTED_CACHE(PrereqCheckType.SERVICE,
      "Tez should reference Hadoop libraries from the distributed cache in HDFS",
      new HashMap<String, String>() {{
        put(ServicesTezDistributedCacheCheck.KEY_LIB_URI_MISSING,
          "The tez-site.xml property tez.lib.uris should be set.");
        put(ServicesTezDistributedCacheCheck.KEY_USE_HADOOP_LIBS,
          "The tez-site.xml property tez.use.cluster-hadoop-libs should be set.");
        put(ServicesTezDistributedCacheCheck.KEY_LIB_NOT_DFS,
          "The tez-site.xml property tez.lib.uris or the core-site.xml property fs.defaultFS should point to *dfs:/ url.");
        put(ServicesTezDistributedCacheCheck.KEY_LIB_NOT_TARGZ,
          "The tez-site.xml property tez.lib.uris should point to tar.gz file.");
        put(ServicesTezDistributedCacheCheck.KEY_USE_HADOOP_LIBS_FALSE,
          "The tez-site.xml property tez.use.cluster.hadoop-libs should be set to false.");
      }}),

  SERVICES_UP(PrereqCheckType.SERVICE,
      "All services must be started",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "The following Services must be started: {{fails}}");
      }}),

  SERVICES_YARN_WP(PrereqCheckType.SERVICE,
      "YARN work preserving restart should be enabled",
      new HashMap<String, String>() {{
        put(AbstractCheckDescriptor.DEFAULT,
          "YARN should have work preserving restart enabled. The yarn-site.xml property yarn.resourcemanager.work-preserving-recovery.enabled property should be set to true.");
      }});

  private PrereqCheckType m_type;
  private String m_description;
  private Map<String, String> m_fails;
  private CheckDescription(PrereqCheckType type, String description,
      Map<String, String> fails) {
    m_type = type;
    m_description = description;
    m_fails = fails;
  }

  /**
   * @return the type of check
   */
  public PrereqCheckType getType() {
    return m_type;
  }

  /**
   * @return the text associated with the description
   */
  public String getText() {
    return m_description;
  }

  /**
   * @param key the failure text key
   * @return the fail text template.  Never {@code null}
   */
  public String getFail(String key) {
    return m_fails.containsKey(key) ? m_fails.get(key) : "";
  }
}
