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
package org.apache.ambari.server.serveraction.upgrades;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

/**
* Computes Ranger properties when upgrading to HDP-2.5
*/

public class RangerKerberosConfigCalculation extends AbstractServerAction {
  private static final String RANGER_ADMIN_SITE_CONFIG_TYPE = "ranger-admin-site";
  private static final String HADOOP_ENV_CONFIG_TYPE = "hadoop-env";
  private static final String HIVE_ENV_CONFIG_TYPE = "hive-env";
  private static final String YARN_ENV_CONFIG_TYPE = "yarn-env";
  private static final String HBASE_ENV_CONFIG_TYPE = "hbase-env";
  private static final String KNOX_ENV_CONFIG_TYPE = "knox-env";
  private static final String STORM_ENV_CONFIG_TYPE = "storm-env";
  private static final String KAFKA_ENV_CONFIG_TYPE = "kafka-env";
  private static final String RANGER_KMS_ENV_CONFIG_TYPE = "kms-env";
  private static final String HDFS_SITE_CONFIG_TYPE = "hdfs-site";
  private static final String RANGER_SPNEGO_KEYTAB = "ranger.spnego.kerberos.keytab";
  private static final String RANGER_PLUGINS_HDFS_SERVICE_USER = "ranger.plugins.hdfs.serviceuser";
  private static final String RANGER_PLUGINS_HIVE_SERVICE_USER = "ranger.plugins.hive.serviceuser";
  private static final String RANGER_PLUGINS_YARN_SERVICE_USER = "ranger.plugins.yarn.serviceuser";
  private static final String RANGER_PLUGINS_HBASE_SERVICE_USER = "ranger.plugins.hbase.serviceuser";
  private static final String RANGER_PLUGINS_KNOX_SERVICE_USER = "ranger.plugins.knox.serviceuser";
  private static final String RANGER_PLUGINS_STORM_SERVICE_USER = "ranger.plugins.storm.serviceuser";
  private static final String RANGER_PLUGINS_KAFKA_SERVICE_USER = "ranger.plugins.kafka.serviceuser";
  private static final String RANGER_PLUGINS_KMS_SERVICE_USER = "ranger.plugins.kms.serviceuser";

  @Inject
  private Clusters m_clusters;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = m_clusters.getCluster(clusterName);
    String errMsg = "";
    String sucessMsg = "";

    Config rangerAdminconfig = cluster.getDesiredConfigByType(RANGER_ADMIN_SITE_CONFIG_TYPE);

    if (null == rangerAdminconfig) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
        MessageFormat.format("The {0} configuration was not found; unable to set Ranger configuration properties", RANGER_ADMIN_SITE_CONFIG_TYPE), "");
    }

    Map<String, String> targetValues = rangerAdminconfig.getProperties();

    // For Hdfs
    Config hadoopConfig = cluster.getDesiredConfigByType(HADOOP_ENV_CONFIG_TYPE);

    if (null != hadoopConfig) {
      String hadoopUser = hadoopConfig.getProperties().get("hdfs_user");
      if (null != hadoopUser) {
        targetValues.put(RANGER_PLUGINS_HDFS_SERVICE_USER, hadoopUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_HDFS_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "hdfs_user", HADOOP_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", HADOOP_ENV_CONFIG_TYPE);
    }

    // For Hive
    Config hiveConfig = cluster.getDesiredConfigByType(HIVE_ENV_CONFIG_TYPE);

    if (null != hiveConfig) {
      String hiveUser = hiveConfig.getProperties().get("hive_user");
      if (null != hiveUser) {
        targetValues.put(RANGER_PLUGINS_HIVE_SERVICE_USER, hiveUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_HIVE_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "hive_user", HIVE_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", HIVE_ENV_CONFIG_TYPE);
    }

    // For Yarn
    Config yarnConfig = cluster.getDesiredConfigByType(YARN_ENV_CONFIG_TYPE);

    if (null != yarnConfig) {
      String yarnUser = yarnConfig.getProperties().get("yarn_user");
      if (null != yarnUser) {
        targetValues.put(RANGER_PLUGINS_YARN_SERVICE_USER, yarnUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_YARN_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "yarn_user", YARN_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", YARN_ENV_CONFIG_TYPE);
    }

    // For Hbase
    Config hbaseConfig = cluster.getDesiredConfigByType(HBASE_ENV_CONFIG_TYPE);

    if (null != hbaseConfig) {
      String hbaseUser = hbaseConfig.getProperties().get("hbase_user");
      if (null != hbaseUser) {
        targetValues.put(RANGER_PLUGINS_HBASE_SERVICE_USER, hbaseUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_HBASE_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "hbase_user", HBASE_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", HBASE_ENV_CONFIG_TYPE);
    }

    // For Knox
    Config knoxConfig = cluster.getDesiredConfigByType(KNOX_ENV_CONFIG_TYPE);

    if (null != knoxConfig) {
      String knoxUser = knoxConfig.getProperties().get("knox_user");
      if (null != knoxUser) {
        targetValues.put(RANGER_PLUGINS_KNOX_SERVICE_USER, knoxUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_KNOX_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "knox_user", KNOX_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", KNOX_ENV_CONFIG_TYPE);
    }

    // For Storm
    Config stormConfig = cluster.getDesiredConfigByType(STORM_ENV_CONFIG_TYPE);

    if (null != stormConfig) {
      String stormValue = null;
      String stormUser = stormConfig.getProperties().get("storm_user");

      if (cluster.getSecurityType() == SecurityType.KERBEROS) {
        String stormPrincipal = stormConfig.getProperties().get("storm_principal_name");
        if (null != stormPrincipal) {
          String[] stormPrincipalParts = stormPrincipal.split("@");
          if(null != stormPrincipalParts && stormPrincipalParts.length > 1) {
            String stormPrincipalBareName = stormPrincipalParts[0];
            stormValue = stormPrincipalBareName;
          }
        }
      }

      if (null != stormUser) {
        if(!StringUtils.isBlank(stormValue)) {
          stormValue = stormValue + "," + stormUser;
        } else {
          stormValue = stormUser;
        }
        targetValues.put(RANGER_PLUGINS_STORM_SERVICE_USER, stormValue);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_STORM_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "storm_user", STORM_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", STORM_ENV_CONFIG_TYPE);
    }

    // For Kafka
    Config kafkaConfig = cluster.getDesiredConfigByType(KAFKA_ENV_CONFIG_TYPE);

    if (null != kafkaConfig) {
      String kafkaUser = kafkaConfig.getProperties().get("kafka_user");
      if (null != kafkaUser) {
        targetValues.put(RANGER_PLUGINS_KAFKA_SERVICE_USER, kafkaUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_KAFKA_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "kafka_user", KAFKA_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", KAFKA_ENV_CONFIG_TYPE);
    }

    // For Ranger Kms
    Config rangerKmsConfig = cluster.getDesiredConfigByType(RANGER_KMS_ENV_CONFIG_TYPE);

    if (null != rangerKmsConfig) {
      String rangerKmsUser = rangerKmsConfig.getProperties().get("kms_user");
      if (null != rangerKmsUser) {
        targetValues.put(RANGER_PLUGINS_KMS_SERVICE_USER, rangerKmsUser);
        rangerAdminconfig.setProperties(targetValues);
        rangerAdminconfig.save();
        sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_PLUGINS_KMS_SERVICE_USER);
      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "kms_user", RANGER_KMS_ENV_CONFIG_TYPE);
      }
    } else {
      errMsg = errMsg + MessageFormat.format("{0} not found\n", RANGER_KMS_ENV_CONFIG_TYPE);
    }

    // Set spnego principal
    if (cluster.getSecurityType() == SecurityType.KERBEROS) {
      Config hdfsSiteConfig = cluster.getDesiredConfigByType(HDFS_SITE_CONFIG_TYPE);

      if (null != hdfsSiteConfig) {
        String spnegoKeytab = hdfsSiteConfig.getProperties().get("dfs.web.authentication.kerberos.keytab");

        if (null != spnegoKeytab) {
          targetValues.put(RANGER_SPNEGO_KEYTAB, spnegoKeytab);
          rangerAdminconfig.setProperties(targetValues);
          rangerAdminconfig.save();
          sucessMsg = sucessMsg + MessageFormat.format("{0}\n", RANGER_SPNEGO_KEYTAB);
        } else {
          errMsg = errMsg + MessageFormat.format("{0} not found in {1}\n", "dfs.web.authentication.kerberos.keytab", HDFS_SITE_CONFIG_TYPE);
        }

      } else {
        errMsg = errMsg + MessageFormat.format("{0} not found \n", HDFS_SITE_CONFIG_TYPE);
      }

    }

    String outputMsg = MessageFormat.format("Successfully set {0} properties in {1}", sucessMsg, RANGER_ADMIN_SITE_CONFIG_TYPE);

    if(!errMsg.equalsIgnoreCase("")) {
      outputMsg = outputMsg + MessageFormat.format("\n {0}", errMsg, RANGER_ADMIN_SITE_CONFIG_TYPE);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outputMsg, "");
  }
}