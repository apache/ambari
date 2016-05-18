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
from resource_management import *
import os
from urlparse import urlparse

config = Script.get_config()
hadoop_conf_dir = None
hadoop_common_dir = os.path.join("share", "hadoop", "common", "lib")
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
hdfs_user = hadoop_user
hadoop_common_bin = "bin"

if os.environ.has_key("HADOOP_CONF_DIR"):
  hadoop_conf_dir = os.environ["HADOOP_CONF_DIR"]
if os.environ.has_key("HADOOP_COMMON_HOME"):
  hadoop_common_dir = os.path.join(os.environ["HADOOP_COMMON_HOME"], "share", "hadoop", "common", "lib")
  hadoop_common_bin = os.path.join(os.environ["HADOOP_COMMON_HOME"], "bin")
if os.environ.has_key("HBASE_HOME"):
  hbase_lib_dir = os.path.join(os.environ["HBASE_HOME"], "lib")

if os.environ.has_key("HADOOP_NODE_INSTALL_ROOT"):
  hadoop_install_root = os.environ["HADOOP_NODE_INSTALL_ROOT"]


stack_log_dir = "c:\\hadoop\\logs"
stack_data_dir = "c:\\hadoop"
db_flavor = "MSSQL"
db_hostname = "localhost"
db_port = "1433"

hive_db_flavor = default("/configurations/hive-env/hive_database_type", None)
hive_jdbc_url = default("/configurations/hive-site/javax.jdo.option.ConnectionURL", None)
hive_db_hostname = None
hive_db_port = None
if hive_jdbc_url:
  hive_db_hostname = urlparse(hive_jdbc_url.split(";")[0].replace('jdbc:', '')).hostname
  hive_db_port = urlparse(hive_jdbc_url.split(";")[0].replace('jdbc:', '')).port

oozie_db_flavor = default("/configurations/oozie-env/oozie_ambari_database", None)
oozie_jdbc_url = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.url", None)
oozie_db_hostname = None
oozie_db_port = None
if oozie_jdbc_url:
  oozie_db_hostname = urlparse(oozie_jdbc_url.split(";")[0].replace('jdbc:', '')).hostname
  oozie_db_port = urlparse(oozie_jdbc_url.split(";")[0].replace('jdbc:', '')).port

if hive_db_hostname:
  db_hostname = hive_db_hostname
  if hive_db_port:
    db_port = hive_db_port
  if hive_db_flavor:
    db_flavor = hive_db_flavor
elif oozie_db_hostname:
  db_hostname = oozie_db_hostname
  if oozie_db_port:
    db_port = oozie_db_port
  if oozie_db_flavor:
    db_flavor = oozie_db_flavor

hive_db_name = default("/configurations/hive-site/ambari.hive.db.schema.name", "hive")
hive_db_username = default("/configurations/hive-site/javax.jdo.option.ConnectionUserName", None)
hive_db_password = default("/configurations/hive-site/javax.jdo.option.ConnectionPassword", None)

oozie_db_name = default("/configurations/oozie-site/oozie.db.schema.name", "oozie")
oozie_db_username = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.username", None)
oozie_db_password = default("/configurations/oozie-site/oozie.service.JPAService.jdbc.password", None)

delimiter = ','
namenode_host = default_string("/clusterHostInfo/namenode_host", [], delimiter)
secondary_namenode_host = default_string("/clusterHostInfo/snamenode_host", [], delimiter)
resourcemanager_host = default_string("/clusterHostInfo/rm_host", [], delimiter)
hive_server_host = default_string("/clusterHostInfo/hive_server_host", [], delimiter)
oozie_server_host = default_string("/clusterHostInfo/oozie_server", [], delimiter)
webhcat_host = default_string("/clusterHostInfo/webhcat_server_host", [], delimiter)
slave_hosts = default_string("/clusterHostInfo/slave_hosts", [], delimiter)
zookeeper_hosts = default_string("/clusterHostInfo/zookeeper_hosts", [], delimiter)
client_hosts = default_string("/clusterHostInfo/client_hosts", [], delimiter)
hbase_master = default_string("/clusterHostInfo/hbase_master_hosts", [], delimiter)
hbase_regionservers = default_string("/clusterHostInfo/hbase_rs_hosts", [], delimiter)
flume_hosts = default_string("/clusterHostInfo/flume_hosts", [], delimiter)
falcon_host = default_string("/clusterHostInfo/falcon_server_hosts", [], delimiter)
knox_host = default_string("/clusterHostInfo/knox_gateway_hosts", [], delimiter)
storm_nimbus = default_string("/clusterHostInfo/nimbus_hosts", [], delimiter)
storm_supervisors = default_string("/clusterHostInfo/supervisor_hosts", [], delimiter)
ambari_server_host = default_string("/clusterHostInfo/ambari_server_host", [], delimiter)
