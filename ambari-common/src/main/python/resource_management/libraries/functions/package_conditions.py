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

Ambari Agent

"""
__all__ = ["is_lzo_enabled", "should_install_phoenix", "should_install_ams_collector", "should_install_ams_grafana",
           "should_install_mysql", "should_install_mysl_connector", "should_install_ranger_tagsync"]

import os
from resource_management.libraries.script import Script
from resource_management.libraries.functions.default import default

def should_install_lzo():
  config = Script.get_config()
  io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
  lzo_enabled = io_compression_codecs is not None and "com.hadoop.compression.lzo" in io_compression_codecs.lower()
  return lzo_enabled

def should_install_phoenix():
  phoenix_hosts = default('/clusterHostInfo/phoenix_query_server_hosts', [])
  phoenix_enabled = default('/configurations/hbase-env/phoenix_sql_enabled', False)
  has_phoenix = len(phoenix_hosts) > 0
  return phoenix_enabled or has_phoenix

def should_install_ams_collector():
  config = Script.get_config()
  return 'role' in config and config['role'] == "METRICS_COLLECTOR"

def should_install_ams_grafana():
  config = Script.get_config()
  return 'role' in config and config['role'] == "METRICS_GRAFANA"

def should_install_logsearch_solr():
  config = Script.get_config()
  return 'role' in config and config['role'] == "LOGSEARCH_SOLR"

def should_install_logsearch_solr_client():
  config = Script.get_config()
  return 'role' in config and config['role'] == "LOGSEARCH_SOLR_CLIENT"

def should_install_logsearch_portal():
  config = Script.get_config()
  return 'role' in config and config['role'] == "LOGSEARCH_SERVER"

def should_install_mysql():
  config = Script.get_config()
  hive_database = config['configurations']['hive-env']['hive_database']
  hive_use_existing_db = hive_database.startswith('Existing')

  if hive_use_existing_db or 'role' in config and config['role'] != "MYSQL_SERVER":
    return False
  return True

def should_install_mysl_connector():
  mysql_jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
  return not os.path.exists(mysql_jdbc_driver_jar)

def should_install_hive_atlas():
  atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
  has_atlas = len(atlas_hosts) > 0
  return has_atlas

def should_install_kerberos_server():
  config = Script.get_config()
  return 'role' in config and config['role'] != "KERBEROS_CLIENT"

def should_install_ranger_tagsync():
  config = Script.get_config()
  ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
  ranger_tagsync_enabled = default('/configurations/ranger-tagsync-site/ranger.tagsync.enabled', False)
  has_ranger_tagsync = len(ranger_tagsync_hosts) > 0

  return has_ranger_tagsync or ranger_tagsync_enabled
