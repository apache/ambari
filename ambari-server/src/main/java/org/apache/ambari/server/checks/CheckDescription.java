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

import java.util.Map;

import org.apache.ambari.server.state.stack.PrereqCheckType;

import com.google.common.collect.ImmutableMap;

/**
 * Enum that wraps the various type, text and failure messages for the checks
 * done for Stack Upgrades.
 */
public class CheckDescription {
  public static CheckDescription CLIENT_RETRY = new CheckDescription("CLIENT_RETRY",
    PrereqCheckType.SERVICE,
    "Client Retry Properties",
    new ImmutableMap.Builder<String, String>()
      .put(ClientRetryPropertyCheck.HDFS_CLIENT_RETRY_DISABLED_KEY,
          "The hdfs-site.xml property dfs.client.retry.policy.enabled should be set to \"false\" to failover quickly.")
      .put(ClientRetryPropertyCheck.HIVE_CLIENT_RETRY_MISSING_KEY,
          "The hive-site.xml property hive.metastore.failure.retries should be set to a positive value.")
      .put(ClientRetryPropertyCheck.OOZIE_CLIENT_RETRY_MISSING_KEY,
          "The oozie-env.sh script must contain a retry count such as export OOZIE_CLIENT_OPTS=\"${OOZIE_CLIENT_OPTS} -Doozie.connection.retry.count=5\"").build());

  public static CheckDescription HOSTS_HEARTBEAT = new CheckDescription("HOSTS_HEARTBEAT",
    PrereqCheckType.HOST,
    "All hosts must be communicating with Ambari. Hosts which are not reachable should be placed in Maintenance Mode.",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "There are hosts which are not communicating with Ambari.").build());

  public static CheckDescription HEALTH = new CheckDescription("HEALTH",
    PrereqCheckType.CLUSTER,
    "Cluster Health",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following issues have been detected on this cluster and should be addressed before upgrading: %s").build());

  public static CheckDescription SERVICE_CHECK = new CheckDescription("SERVICE_CHECK",
    PrereqCheckType.SERVICE,
    "Last Service Check should be more recent than the last configuration change for the given service",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following service configurations have been updated and their Service Checks should be run again: %s").build());

  public static CheckDescription HOSTS_MAINTENANCE_MODE = new CheckDescription("HOSTS_MAINTENANCE_MODE",
    PrereqCheckType.HOST,
    "Hosts in Maintenance Mode will be excluded from the upgrade.",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "There are hosts in Maintenance Mode which excludes them from being upgraded.")
      .put(HostMaintenanceModeCheck.KEY_CANNOT_START_HOST_ORDERED,
          "The following hosts cannot be in Maintenance Mode: {{fails}}.").build());

  public static CheckDescription HOSTS_MASTER_MAINTENANCE = new CheckDescription("HOSTS_MASTER_MAINTENANCE",
    PrereqCheckType.HOST,
    "Hosts in Maintenance Mode must not have any master components",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following hosts must not be in in Maintenance Mode since they host Master components: {{fails}}.")
      .put(HostsMasterMaintenanceCheck.KEY_NO_UPGRADE_NAME,
          "Could not find suitable upgrade pack for %s %s to version {{version}}.")
      .put(HostsMasterMaintenanceCheck.KEY_NO_UPGRADE_PACK,
          "Could not find upgrade pack named %s.").build());

  public static CheckDescription HOSTS_REPOSITORY_VERSION = new CheckDescription("HOSTS_REPOSITORY_VERSION",
    PrereqCheckType.HOST,
    "All hosts should have target version installed",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following hosts must have version {{version}} installed: {{fails}}.").build());

  public static CheckDescription SECONDARY_NAMENODE_MUST_BE_DELETED = new CheckDescription("SECONDARY_NAMENODE_MUST_BE_DELETED",
    PrereqCheckType.HOST,
    "The SNameNode component must be deleted from all hosts",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT, "The SNameNode component must be deleted from host: %s.").build());

  public static CheckDescription SERVICES_HIVE_MULTIPLE_METASTORES = new CheckDescription("SERVICES_HIVE_MULTIPLE_METASTORES",
    PrereqCheckType.SERVICE,
    "Hive Metastore Availability",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "Multiple Hive Metastore instances are recommended for Rolling Upgrade. This ensures that there is at least one Metastore running during the upgrade process.").build());

  public static CheckDescription SERVICES_MAINTENANCE_MODE = new CheckDescription("SERVICES_MAINTENANCE_MODE",
    PrereqCheckType.SERVICE,
    "No services can be in Maintenance Mode",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following Services must not be in Maintenance Mode: {{fails}}.").build());

  public static CheckDescription SERVICES_MR_DISTRIBUTED_CACHE = new CheckDescription("SERVICES_MR_DISTRIBUTED_CACHE",
    PrereqCheckType.SERVICE,
    "MapReduce should reference Hadoop libraries from the distributed cache in HDFS",
    new ImmutableMap.Builder<String, String>()
      .put(ServicesMapReduceDistributedCacheCheck.KEY_APP_CLASSPATH,
          "The mapred-site.xml property mapreduce.application.classpath should be set.")
      .put(ServicesMapReduceDistributedCacheCheck.KEY_FRAMEWORK_PATH,
          "The mapred-site.xml property mapreduce.application.framework.path should be set.")
      .put(ServicesMapReduceDistributedCacheCheck.KEY_NOT_DFS,
          "The mapred-site.xml property mapreduce.application.framework.path or the core-site.xml property fs.defaultFS should point to *dfs:/ url.").build());

  public static CheckDescription SERVICES_NAMENODE_HA = new CheckDescription("SERVICES_NAMENODE_HA",
    PrereqCheckType.SERVICE,
    "NameNode High Availability must be enabled",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "NameNode High Availability is not enabled. Verify that dfs.internal.nameservices property is present in hdfs-site.xml.").build());

  public static CheckDescription SERVICES_NAMENODE_TRUNCATE = new CheckDescription("SERVICES_NAMENODE_TRUNCATE",
    PrereqCheckType.SERVICE,
    "NameNode Truncate must not be allowed",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "NameNode Truncate is allowed. Verify that dfs.allow.truncate is set to 'false' in hdfs-site.xml.").build());

  public static CheckDescription SERVICES_TEZ_DISTRIBUTED_CACHE = new CheckDescription("SERVICES_TEZ_DISTRIBUTED_CACHE",
    PrereqCheckType.SERVICE,
    "Tez should reference Hadoop libraries from the distributed cache in HDFS",
    new ImmutableMap.Builder<String, String>()
      .put(ServicesTezDistributedCacheCheck.KEY_LIB_URI_MISSING,
          "The tez-site.xml property tez.lib.uris should be set.")
      .put(ServicesTezDistributedCacheCheck.KEY_USE_HADOOP_LIBS,
          "The tez-site.xml property tez.use.cluster-hadoop-libs should be set.")
      .put(ServicesTezDistributedCacheCheck.KEY_LIB_NOT_DFS,
          "The tez-site.xml property tez.lib.uris or the core-site.xml property fs.defaultFS should point to *dfs:/ url.")
      .put(ServicesTezDistributedCacheCheck.KEY_LIB_NOT_TARGZ,
          "The tez-site.xml property tez.lib.uris should point to tar.gz file.")
      .put(ServicesTezDistributedCacheCheck.KEY_USE_HADOOP_LIBS_FALSE,
          "The tez-site.xml property tez.use.cluster.hadoop-libs should be set to false.").build());

  public static CheckDescription SERVICES_UP = new CheckDescription("SERVICES_UP",
    PrereqCheckType.SERVICE,
    "All services must be started",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following Services must be started: {{fails}}. Try to do a Stop & Start in case they were started outside of Ambari.").build());

  public static CheckDescription COMPONENTS_INSTALLATION = new CheckDescription("COMPONENTS_INSTALLATION",
    PrereqCheckType.SERVICE,
    "All service components must be installed",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following Services must be reinstalled: {{fails}}. Try to reinstall the service components in INSTALL_FAILED state.").build());

  public static CheckDescription PREVIOUS_UPGRADE_COMPLETED = new CheckDescription("PREVIOUS_UPGRADE_COMPLETED",
    PrereqCheckType.CLUSTER,
    "A previous upgrade did not complete.",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The last upgrade attempt did not complete. {{fails}}").build());

  public static CheckDescription INSTALL_PACKAGES_CHECK = new CheckDescription("INSTALL_PACKAGES_CHECK",
    PrereqCheckType.CLUSTER,
    "Install packages must be re-run",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "Re-run Install Packages before starting upgrade").build());

  public static CheckDescription SERVICES_YARN_WP = new CheckDescription("SERVICES_YARN_WP",
    PrereqCheckType.SERVICE,
    "YARN work preserving restart should be enabled",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "YARN should have work preserving restart enabled. The yarn-site.xml property yarn.resourcemanager.work-preserving-recovery.enabled property should be set to true.").build());

  public static CheckDescription SERVICES_YARN_RM_HA = new CheckDescription("SERVICES_YARN_RM_HA",
    PrereqCheckType.SERVICE,
    "YARN ResourceManager High Availability is not enabled.",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "YARN ResourceManager HA should be enabled to prevent a disruption in service during the upgrade").build());

  public static CheckDescription SERVICES_YARN_TIMELINE_ST = new CheckDescription("SERVICES_YARN_TIMELINE_ST",
    PrereqCheckType.SERVICE,
    "YARN Timeline state preserving restart should be enabled",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "YARN should have state preserving restart enabled for the Timeline server. The yarn-site.xml property yarn.timeline-service.recovery.enabled should be set to true.").build());

  public static CheckDescription SERVICES_MR2_JOBHISTORY_ST = new CheckDescription("SERVICES_MR2_JOBHISTORY_ST",
    PrereqCheckType.SERVICE,
    "MapReduce2 JobHistory recovery should be enabled",
    new ImmutableMap.Builder<String, String>()
      .put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_ENABLE_KEY,
          "MapReduce2 should have recovery enabled for the JobHistory server. The mapred-site.xml property mapreduce.jobhistory.recovery.enable should be set to true.")
      .put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_KEY,
          "MapReduce2 should have recovery enabled for the JobHistory server. The mapred-site.xml property mapreduce.jobhistory.recovery.store.class should be set to org.apache.hadoop.mapreduce.v2.hs.HistoryServerLeveldbStateStoreService.")
      .put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_LEVELDB_PATH_KEY,
          "MapReduce2 should have recovery enabled for the JobHistory server. The mapred-site.xml property mapreduce.jobhistory.recovery.store.leveldb.path should be set. Please note that \"mapreduce.jobhistory.recovery.store.leveldb.path\" should be on a mount with ~3 GB of free space.").build());

  public static CheckDescription SERVICES_HIVE_DYNAMIC_SERVICE_DISCOVERY = new CheckDescription("SERVICES_HIVE_DYNAMIC_SERVICE_DISCOVERY",
    PrereqCheckType.SERVICE,
    "Hive Dynamic Service Discovery",
    new ImmutableMap.Builder<String, String>()
      .put(HiveDynamicServiceDiscoveryCheck.HIVE_DYNAMIC_SERVICE_DISCOVERY_ENABLED_KEY,
          "The hive-site.xml property hive.server2.support.dynamic.service.discovery should be set to true.")
      .put(HiveDynamicServiceDiscoveryCheck.HIVE_DYNAMIC_SERVICE_ZK_QUORUM_KEY,
          "The hive-site.xml property hive.zookeeper.quorum should be set to a comma-separate list of ZooKeeper hosts:port pairs.")
      .put(HiveDynamicServiceDiscoveryCheck.HIVE_DYNAMIC_SERVICE_ZK_NAMESPACE_KEY,
          "The hive-site.xml property hive.server2.zookeeper.namespace should be set to the value for the root namespace on ZooKeeper.").build());

  public static CheckDescription AMS_HADOOP_SINK_VERSION_COMPATIBILITY = new CheckDescription("AMS_HADOOP_SINK_VERSION_COMPATIBILITY",
    PrereqCheckType.HOST,
    "Ambari Metrics Hadoop Sinks need to be compatible with the stack version. This check ensures that compatibility.",
    new ImmutableMap.Builder<String, String>().put(AbstractCheckDescriptor.DEFAULT,"Hadoop Sink version check failed. " +
      "To fix this, please upgrade 'ambari-metrics-hadoop-sink' package to %s on all the failed hosts")
      .put(AmbariMetricsHadoopSinkVersionCompatibilityCheck.HADOOP_SINK_VERSION_NOT_SPECIFIED, "Hadoop Sink version for pre-check not specified. " +
        "Please use 'min-hadoop-sink-version' property in upgrade pack to specify min hadoop sink version").build());

  public static CheckDescription CONFIG_MERGE = new CheckDescription("CONFIG_MERGE",
    PrereqCheckType.CLUSTER,
    "Configuration Merge Check",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The following config types will have values overwritten: %s").build());

  public static CheckDescription HARDCODED_STACK_VERSION_PROPERTIES_CHECK = new CheckDescription("HARDCODED_STACK_VERSION_PROPERTIES_CHECK",
    PrereqCheckType.CLUSTER,
    "Found hardcoded stack version in property value.",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "Some properties seem to contain hardcoded stack version string \"%s\"." +
          " That is a potential problem when doing stack update.").build());

  public static CheckDescription VERSION_MISMATCH = new CheckDescription("VERSION_MISMATCH",
    PrereqCheckType.HOST,
    "All components must be reporting the expected version",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "There are components which are not reporting the expected stack version: \n%s").build());

  public static CheckDescription SERVICES_RANGER_PASSWORD_VERIFY = new CheckDescription("SERVICES_RANGER_PASSWORD_VERIFY",
    PrereqCheckType.SERVICE,
    "Verify Ambari and Ranger Password Synchronization",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "There was a problem verifying Ranger and Ambari users")
      .put(RangerPasswordCheck.KEY_RANGER_PASSWORD_MISMATCH,
          "Credentials for user '%s' in Ambari do not match Ranger.")
      .put(RangerPasswordCheck.KEY_RANGER_UNKNOWN_RESPONSE,
          "Could not verify credentials for user '%s'.  Response code %s received from %s")
      .put(RangerPasswordCheck.KEY_RANGER_COULD_NOT_ACCESS,
          "Could not access Ranger to verify user '%s' against %s. %s")
      .put(RangerPasswordCheck.KEY_RANGER_USERS_ELEMENT_MISSING,
          "The response from Ranger received, but there is no users element.  Request: %s")
      .put(RangerPasswordCheck.KEY_RANGER_OTHER_ISSUE,
          "The response from Ranger was malformed. %s. Request: %s")
      .put(RangerPasswordCheck.KEY_RANGER_CONFIG_MISSING,
          "Could not check credentials.  Missing property %s/%s").build());

  public static CheckDescription ATLAS_SERVICE_PRESENCE_CHECK = new CheckDescription("ATLAS_SERVICE_PRESENCE_CHECK",
    PrereqCheckType.SERVICE,
    "Atlas Is Not Supported For Upgrades",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The Atlas service is currently installed on the cluster. " +
          "This service does not support upgrades and must be removed before the upgrade can continue. " +
          "After upgrading, Atlas can be reinstalled").build());

  public static CheckDescription SERVICE_PRESENCE_CHECK = new CheckDescription("SERVICE_PRESENCE_CHECK",
    PrereqCheckType.SERVICE,
    "Service Is Not Supported For Upgrades",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "The %s service is currently installed on the cluster. " +
          "This service does not support upgrades and must be removed before the upgrade can continue. " +
          "After upgrading, %s can be reinstalled")
      .put(ServicePresenceCheck.KEY_SERVICE_REMOVED,
          "The %s service is currently installed on the cluster. " +
          "This service is removed from the new release and must be removed before the upgrade can continue.").build());

  public static CheckDescription RANGER_SERVICE_AUDIT_DB_CHECK = new CheckDescription("RANGER_SERVICE_AUDIT_DB_CHECK",
    PrereqCheckType.SERVICE,
    "Remove the Ranger Audit to Database Capability",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "After upgrading, Ranger will no longer support the Audit to Database feature. Instead, Ranger will audit to Solr. " +
          "To migrate the existing audit logs to Solr, follow the steps in Apache Ranger documention for 0.6 release.").build());

  public static CheckDescription KAFKA_KERBEROS_CHECK = new CheckDescription("KAFKA_KERBEROS_CHECK",
    PrereqCheckType.SERVICE,
    "Kafka upgrade on Kerberized cluster",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "Kafka is currently not Kerberized, but your cluster is. After upgrading, Kafka will automatically be Kerberized for you.").build());

  public static CheckDescription SERVICES_HIVE_ROLLING_WARNING = new CheckDescription("SERVICES_HIVE_ROLLING_WARNING",
    PrereqCheckType.SERVICE,
    "HiveServer2 Downtime",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "HiveServer2 does not currently support rolling upgrades. HiveServer2 will be upgraded, however existing queries which have not completed will fail and need to be resubmitted after HiveServer2 has been upgraded.").build());

  public static CheckDescription SERVICES_STORM_ROLLING_WARNING = new CheckDescription("SERVICES_STORM_ROLLING_WARNING",
    PrereqCheckType.SERVICE,
    "Storm Downtime During Upgrade",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
          "Storm does not support rolling upgrades on this version of the stack. If you proceed, you will be required to stop all running topologies before Storm is restarted.").build());

  public static CheckDescription AUTO_START_DISABLED = new CheckDescription("AUTO_START_DISABLED",
    PrereqCheckType.CLUSTER,
    "Auto-Start Disabled Check",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
        "Auto Start must be disabled before performing an Upgrade. To disable Auto Start, navigate to " +
          "Admin > Service Auto Start. Turn the toggle switch off to Disabled and hit Save.").build());

  public static CheckDescription RANGER_SSL_CONFIG_CHECK = new CheckDescription("RANGER_SSL_CONFIG_CHECK",
    PrereqCheckType.SERVICE,
    "Change Ranger SSL configuration path for Keystore and Truststore.",
    new ImmutableMap.Builder<String, String>()
      .put(AbstractCheckDescriptor.DEFAULT,
        "As Ranger is SSL enabled, Ranger SSL configurations will need to be changed from default value of /etc/ranger/*/conf folder to /etc/ranger/security. " +
        "Since the certificates/keystores/truststores in this path may affect the upgrade/downgrade process, it is recommended to manually move the certificates/keystores/truststores out of the conf folders and change the appropriate config values before proceeding.").build());

  public static CheckDescription LZO_CONFIG_CHECK = new CheckDescription("LZO_CONFIG_CHECK",
      PrereqCheckType.CLUSTER,
      "LZO Codec Check",
      new ImmutableMap.Builder<String, String>()
          .put(AbstractCheckDescriptor.DEFAULT,
              "You have LZO codec enabled in the core-site config of your cluster. LZO is no longer installed automatically. " +
                  "If any hosts require LZO, it should be installed before starting the upgrade. " +
                  "Consult Ambari documentation for instructions on how to do this.").build());

  public static CheckDescription JAVA_VERSION = new CheckDescription("JAVA_VERSION",
      PrereqCheckType.CLUSTER,
      "Verify Java version requirement",
      new ImmutableMap.Builder<String, String>()
        .put(AbstractCheckDescriptor.DEFAULT, "Ambari requires JDK with minimum version %s. Reconfigure Ambari with a JDK that meets the version requirement.")
          .build());

  public static CheckDescription COMPONENTS_EXIST_IN_TARGET_REPO = new CheckDescription("COMPONENTS_EXIST_IN_TARGET_REPO",
    PrereqCheckType.CLUSTER,
    "Check installed services which are not supported in the installed stack",
    new ImmutableMap.Builder<String, String>()
      .put(ComponentsExistInRepoCheck.AUTO_REMOVE, "The following services and/or components do not exist in the target stack and will be automatically removed during the upgrade.")
      .put(ComponentsExistInRepoCheck.MANUAL_REMOVE, "The following components do not exist in the target repository's stack. They must be removed from the cluster before upgrading.")
      .build()
    );

  public static CheckDescription DRUID_HA_WARNING = new CheckDescription(
      "DRUID_HA",
      PrereqCheckType.SERVICE,
      "Druid Downtime During Upgrade",
      new ImmutableMap.Builder<String, String>()
          .put(
              AbstractCheckDescriptor.DEFAULT,
              "High Availability is not enabled for Druid. Druid Service may have some downtime during upgrade. Deploy multiple instances of %s in the Cluster to avoid any downtime."
          )
          .build()
  );

  public static CheckDescription VALID_SERVICES_INCLUDED_IN_REPOSITORY = new CheckDescription("VALID_SERVICES_INCLUDED_IN_REPOSITORY",
      PrereqCheckType.CLUSTER,
      "The repository is missing services which are required",
      new ImmutableMap.Builder<String, String>()
        .put(AbstractCheckDescriptor.DEFAULT,
            "The following services are included in the upgrade but the repository is missing their dependencies:\n%s").build());


  public static CheckDescription ATLAS_MIGRATION_PROPERTY_CHECK = new CheckDescription("ATLAS_MIGRATION_PROPERTY_CHECK",
    PrereqCheckType.SERVICE, "Check for Atlas Migration Property before upgrade.",
      new ImmutableMap.Builder<String,String>().put(AbstractCheckDescriptor.DEFAULT,
        "The property atlas.migration.data.filename is missing from application-properties. Do not use atlas conf path ie /etc/atlas/conf as the value." +
        "After upgrading Atlas will no longer support TitanDB, instead it will support JanusGraph." +
        "Hence need to migrate existing data to newer formats post upgrade. " +
        "To migrate existing data, Kindly refer and follow Apache Atlas documentation for 1.0 release.").build());

  public static CheckDescription KERBEROS_ADMIN_CREDENTIAL_CHECK = new CheckDescription("KERBEROS_ADMIN_CREDENTIAL_CHECK",
      PrereqCheckType.CLUSTER,
      "The KDC administrator credentials need to be stored in Ambari persisted credential store.",
      new ImmutableMap.Builder<String, String>()
          .put(KerberosAdminPersistedCredentialCheck.KEY_PERSISTED_STORE_NOT_CONFIGURED,
              "Ambari's credential store has not been configured.  " +
                  "This is needed so the KDC administrator credential may be stored long enough to ensure it will be around if needed during the upgrade process.")
          .put(KerberosAdminPersistedCredentialCheck.KEY_CREDENTIAL_NOT_STORED,
              "The KDC administrator credential has not been stored in the persisted credential store. " +
                  "Visit the Kerberos administrator page to set the credential. " +
                  "This is needed so the KDC administrator credential may be stored long enough to ensure it will be around if needed during the upgrade process.")
          .build());


  public static final CheckDescription MISSING_OS_IN_REPO_VERSION = new CheckDescription("MISSING_OS_IN_REPO_VERSION",
    PrereqCheckType.CLUSTER,
    "Missing OS in repository version.",
    new ImmutableMap.Builder<String, String>()
      .put(MissingOsInRepoVersionCheck.SOURCE_OS, "The source version must have an entry for each OS type in the cluster")
      .put(MissingOsInRepoVersionCheck.TARGET_OS, "The target version must have an entry for each OS type in the cluster")
      .build());


  private String m_name;
  private PrereqCheckType m_type;
  private String m_description;
  private Map<String, String> m_fails;
  public CheckDescription(String name, PrereqCheckType type, String description, Map<String, String> fails) {
    m_name = name;
    m_type = type;
    m_description = description;
    m_fails = fails;
  }

  /**
   * @return the name of check
   */
  public String name() {
    return m_name;
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
