#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

__all__ = ["Direction", "SafeMode", "StackFeature"]

class Direction:
  """
  Stack Upgrade direction
  """
  UPGRADE = "upgrade"
  DOWNGRADE = "downgrade"

class SafeMode:
  """
  Namenode Safe Mode state
  """
  ON = "ON"
  OFF = "OFF"
  UNKNOWN = "UNKNOWN"
  
class StackFeature:
  """
  Stack Feature supported
  """
  SNAPPY = "snappy"
  LZO = "lzo"
  EXPRESS_UPGRADE = "express_upgrade"
  ROLLING_UPGRADE = "rolling_upgrade"
  CONFIG_VERSIONING = "config_versioning"
  DATANODE_NON_ROOT = "datanode_non_root"
  REMOVE_RANGER_HDFS_PLUGIN_ENV = "remove_ranger_hdfs_plugin_env"
  RANGER = "ranger"
  RANGER_TAGSYNC_COMPONENT = "ranger_tagsync_component"
  PHOENIX = "phoenix"
  NFS = "nfs"
  TEZ_FOR_SPARK = "tez_for_spark"
  TIMELINE_STATE_STORE = "timeline_state_store"
  COPY_TARBALL_TO_HDFS = "copy_tarball_to_hdfs"
  SPARK_16PLUS = "spark_16plus"
  SPARK_THRIFTSERVER = "spark_thriftserver"
  SPARK_LIVY = "spark_livy"
  STORM_KERBEROS = "storm_kerberos"
  STORM_AMS = "storm_ams"
  CREATE_KAFKA_BROKER_ID = "create_kafka_broker_id"
  KAFKA_LISTENERS = "kafka_listeners"
  KAFKA_KERBEROS = "kafka_kerberos"
  PIG_ON_TEZ = "pig_on_tez"
  RANGER_USERSYNC_NON_ROOT = "ranger_usersync_non_root"
  RANGER_AUDIT_DB_SUPPORT = "ranger_audit_db_support"
  ACCUMULO_KERBEROS_USER_AUTH = "accumulo_kerberos_user_auth"
  KNOX_VERSIONED_DATA_DIR = "knox_versioned_data_dir"
  KNOX_SSO_TOPOLOGY = "knox_sso_topology"
  ATLAS_ROLLING_UPGRADE = "atlas_rolling_upgrade"
  OOZIE_ADMIN_USER = "oozie_admin_user"
  OOZIE_CREATE_HIVE_TEZ_CONFIGS = "oozie_create_hive_tez_configs"
  OOZIE_SETUP_SHARED_LIB = "oozie_setup_shared_lib"
  OOZIE_HOST_KERBEROS = "oozie_host_kerberos"
  HIVE_METASTORE_UPGRADE_SCHEMA = "hive_metastore_upgrade_schema"
  HIVE_SERVER_INTERACTIVE = "hive_server_interactive"
  HIVE_WEBHCAT_SPECIFIC_CONFIGS = "hive_webhcat_specific_configs"
  HIVE_PURGE_TABLE = "hive_purge_table"
  HIVE_SERVER2_KERBERIZED_ENV = "hive_server2_kerberized_env"
  HIVE_ENV_HEAPSIZE = "hive_env_heapsize"
  RANGER_KMS_HSM_SUPPORT = "ranger_kms_hsm_support"
  RANGER_LOG4J_SUPPORT = "ranger_log4j_support"
  RANGER_KERBEROS_SUPPORT = "ranger_kerberos_support"
  HIVE_METASTORE_SITE_SUPPORT = "hive_metastore_site_support"
  RANGER_USERSYNC_PASSWORD_JCEKS = "ranger_usersync_password_jceks"
  LOGSEARCH_SUPPORT = "logsearch_support"
  HBASE_HOME_DIRECTORY = "hbase_home_directory"
