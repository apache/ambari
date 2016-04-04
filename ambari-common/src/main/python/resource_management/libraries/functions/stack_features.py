#!/usr/bin/env python
"""
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

"""

# simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import ambari_simplejson as json
from resource_management.libraries.script import Script
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.version import compare_versions

_DEFAULT_STACK_FEATURES = {
  "stack_features": [
    {
      "name": "snappy",
      "description": "Snappy compressor/decompressor support",
      "min_version": "2.0.0.0",
      "max_version": "2.2.0.0"
    },
    {
      "name": "lzo",
      "description": "LZO libraries support",
      "min_version": "2.2.1.0"
    },
    {
      "name": "express_upgrade",
      "description": "Express upgrade support",
      "min_version": "2.1.0.0"
    },
    {
      "name": "rolling_upgrade",
      "description": "Rolling upgrade support",
      "min_version": "2.2.0.0"
    },
    {
      "name": "config_versioning",
      "description": "Configurable versions support",
      "min_version": "2.3.0.0"
    },
    {
      "name": "datanode_non_root",
      "description": "DataNode running as non-root support (AMBARI-7615)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "remove_ranger_hdfs_plugin_env",
      "description": "HDFS removes Ranger env files (AMBARI-14299)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "ranger",
      "description": "Ranger Service support",
      "min_version": "2.2.0.0"
    },
    {
      "name": "phoenix",
      "description": "Phoenix Service support",
      "min_version": "2.3.0.0"
    },
    {
      "name": "nfs",
      "description": "NFS support",
      "min_version": "2.3.0.0"
    },
    {
      "name": "tez_for_spark",
      "description": "Tez dependency for Spark",
      "min_version": "2.2.0.0",
      "max_version": "2.3.0.0"
    },
    {
      "name": "timeline_state_store",
      "description": "Yarn application timeline-service supports state store property (AMBARI-11442)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "copy_tarball_to_hdfs",
      "description": "Copy tarball to HDFS support (AMBARI-12113)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "spark_16plus",
      "description": "Spark 1.6+",
      "min_version": "2.4.0.0"
    },
    {
      "name": "spark_thriftserver",
      "description": "Spark Thrift Server",
      "min_version": "2.3.2.0"
    },
    {
      "name": "storm_kerberos",
      "description": "Storm Kerberos support (AMBARI-7570)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "storm_ams",
      "description": "Storm AMS integration (AMBARI-10710)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "create_kafka_broker_id",
      "description": "Ambari should create Kafka Broker Id (AMBARI-12678)",
      "min_version": "2.2.0.0",
      "max_version": "2.3.0.0"
    },
    {
      "name": "kafka_listeners",
      "description": "Kafka listeners (AMBARI-10984)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "kafka_kerberos",
      "description": "Kafka Kerberos support (AMBARI-10984)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "pig_on_tez",
      "description": "Pig on Tez support (AMBARI-7863)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "ranger_usersync_non_root",
      "description": "Ranger Usersync as non-root user (AMBARI-10416)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "accumulo_kerberos_user_auth",
      "description": "Accumulo Kerberos User Auth (AMBARI-10163)",
      "min_version": "2.3.0.0"
    },
    {
      "name": "knox_versioned_data_dir",
      "description": "Use versioned data dir for Knox (AMBARI-13164)",
      "min_version": "2.3.2.0"
    },
    {
      "name": "knox_sso_topology",
      "description": "Knox SSO Topology support (AMBARI-13975)",
      "min_version": "2.3.8.0"
    },
    {
      "name": "atlas_rolling_upgrade",
      "description": "Rolling upgrade support for Atlas",
      "min_version": "2.3.0.0"
    },
    {
      "name": "oozie_admin_user",
      "description": "Oozie install user as an Oozie admin user (AMBARI-7976)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "oozie_create_hive_tez_configs",
      "description": "Oozie create configs for Ambari Hive and Tez deployments (AMBARI-8074)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "oozie_setup_shared_lib",
      "description": "Oozie setup tools used to shared Oozie lib to HDFS (AMBARI-7240)",
      "min_version": "2.2.0.0"
    },
    {
      "name": "oozie_host_kerberos",
      "description": "Oozie in secured clusters uses _HOST in Kerberos principal (AMBARI-9775)",
      "min_version": "2.0.0.0",
      "max_version": "2.2.0.0"
    }
  ]
}

def check_stack_feature(stack_feature, stack_version):
  """
  Given a stack_feature and a specific stack_version, it validates that the feature is supported by the stack_version.
  :param stack_feature: Feature name to check if it is supported by the stack. For example: "rolling_upgrade"
  :param stack_version: Version of the stack
  :return: Will return True if successful, otherwise, False. 
  """
  stack_features_config = default("/configurations/cluster-env/stack_features", None)
  data = _DEFAULT_STACK_FEATURES

  if not stack_version:
    return False

  if stack_features_config:
    data = json.loads(stack_features_config)
  
  for feature in data["stack_features"]:
    if feature["name"] == stack_feature:
      if "min_version" in feature:
        min_version = feature["min_version"]
        if compare_versions(stack_version, min_version, format = True) < 0:
          return False
      if "max_version" in feature:
        max_version = feature["max_version"]
        if compare_versions(stack_version, max_version, format = True) >= 0:
          return False
      return True
        
  return False
