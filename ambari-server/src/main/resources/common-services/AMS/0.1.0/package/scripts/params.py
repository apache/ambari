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

from functions import calc_xmn_from_xms
from resource_management import *
import status_params
from ambari_commons import OSCheck

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *
# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()

#AMS data
ams_pid_dir = status_params.ams_collector_pid_dir

ams_collector_script = "/usr/sbin/ambari-metrics-collector"
ams_collector_pid_dir = status_params.ams_collector_pid_dir
ams_collector_hosts = default("/clusterHostInfo/metric_collector_hosts", [])
ams_collector_host_single = ams_collector_hosts[0] #TODO cardinality is 1+ so we can have more than one host
metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
if metric_collector_port and metric_collector_port.find(':') != -1:
  metric_collector_port = metric_collector_port.split(':')[1]
pass

ams_collector_log_dir = config['configurations']['ams-env']['ams_collector_log_dir']
ams_monitor_log_dir = config['configurations']['ams-env']['ams_monitor_log_dir']

ams_monitor_dir = "/usr/lib/python2.6/site-packages/resource_monitoring"
ams_monitor_pid_dir = status_params.ams_monitor_pid_dir
ams_monitor_script = "/usr/sbin/ambari-metrics-monitor"



#hadoop params

hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = config['commandParams']['mark_draining_only']
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
smokeuser = config['configurations']['cluster-env']['smokeuser']
hbase_root_dir = config['configurations']['ams-hbase-site']['hbase.rootdir']

is_hbase_distributed = hbase_root_dir.startswith('hdfs://')

# security is disabled for embedded mode, when HBase is backed by file
security_enabled = False if not is_hbase_distributed else config['configurations']['cluster-env']['security_enabled'] 

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']

hbase_log_dir = config['configurations']['ams-hbase-env']['hbase_log_dir']
master_heapsize = config['configurations']['ams-hbase-env']['hbase_master_heapsize']

regionserver_heapsize = config['configurations']['ams-hbase-env']['hbase_regionserver_heapsize']
regionserver_xmn_max = config['configurations']['ams-hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = config['configurations']['ams-hbase-env']['hbase_regionserver_xmn_ratio']
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)
# For embedded mode
hbase_heapsize = master_heapsize

ams_checkpoint_dir = config['configurations']['ams-site']['timeline.metrics.aggregator.checkpoint.dir']
hbase_pid_dir = status_params.hbase_pid_dir
hbase_tmp_dir = config['configurations']['ams-hbase-site']['hbase.tmp.dir']
# TODO UPGRADE default, update site during upgrade
_local_dir_conf = default('/configurations/ams-hbase-site/hbase.local.dir', "${hbase.tmp.dir}/local")
local_dir = substitute_vars(_local_dir_conf, config['configurations']['ams-hbase-site'])

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")

rs_hosts = ["localhost"]

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_permissions = "RWXCA"
service_check_data = functions.get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]
hadoop_user = "hadoop"

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  master_jaas_princ = default('/configurations/ams-hbase-site/hbase.master.kerberos.principal', 'hbase/_HOST@EXAMPLE.COM').replace('_HOST',_hostname_lowercase)
  regionserver_jaas_princ = default('/configurations/ams-hbase-site/hbase.regionserver.kerberos.principal', 'hbase/_HOST@EXAMPLE.COM').replace('_HOST',_hostname_lowercase)

  master_keytab_path = config['configurations']['ams-hbase-site']['hbase.master.keytab.file']
  regionserver_keytab_path = config['configurations']['ams-hbase-site']['hbase.regionserver.keytab.file']
  smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
  hbase_user_keytab = config['configurations']['ams-hbase-env']['hbase_user_keytab']
  kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

if security_enabled:
   kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_user};")
else:
   kinit_cmd = ""

#log4j.properties
if (('ams-hbase-log4j' in config['configurations']) and ('content' in config['configurations']['ams-hbase-log4j'])):
  hbase_log4j_props = config['configurations']['ams-hbase-log4j']['content']
else:
  hbase_log4j_props = None

if (('ams-log4j' in config['configurations']) and ('content' in config['configurations']['ams-log4j'])):
  log4j_props = config['configurations']['ams-log4j']['content']
else:
  log4j_props = None

hbase_env_sh_template = config['configurations']['ams-hbase-env']['content']
ams_env_sh_template = config['configurations']['ams-env']['content']


hbase_staging_dir = "/apps/hbase/staging"
#for create_hdfs_directory
hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

import functools
# create partial functions with common arguments for every HdfsDirectory call
# to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  bin_dir = hadoop_bin_dir
)



