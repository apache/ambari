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

from ambari_commons.constants import AMBARI_SUDO_BINARY
from functions import calc_xmn_from_xms, ensure_unit_for_memory
from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.expect import expect
import status_params
from hbase import *

def treat_value_as_mb(value1):
  value = str(value1)
  try:
    part = int(value.strip()[:-1]) if value.lower().strip()[-1:] == 'm' else int(value)
    return str(part) + 'm'
  except:
    return None

# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)

component_directory = status_params.component_directory

#hadoop params
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
daemon_script = format('/usr/iop/current/{component_directory}/bin/hbase-daemon.sh')
region_mover = format('/usr/iop/current/{component_directory}/bin/region_mover.rb')
region_drainer = format('/usr/iop/current/{component_directory}/bin/draining_servers.rb')
hbase_cmd = format('/usr/iop/current/{component_directory}/bin/hbase')

limits_conf_dir = "/etc/security/limits.d"
hbase_conf_dir = status_params.hbase_conf_dir
hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = default("/commandParams/mark_draining_only",False)
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
hbase_principal_name = config['configurations']['hbase-env']['hbase_principal_name']
smokeuser = config['configurations']['cluster-env']['smokeuser']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = config['configurations']['cluster-env']['security_enabled']

hbase_hdfs_user_dir = format("/user/{hbase_user}")
hbase_hdfs_user_mode = 0755

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']

log_dir = config['configurations']['hbase-env']['hbase_log_dir']
master_heapsize_cfg = config['configurations']['hbase-env']['hbase_master_heapsize']
master_heapsize = treat_value_as_mb(master_heapsize_cfg)

hbase_javaopts_properties = config['configurations']['hbase-javaopts-properties']['content']

iop_full_version = get_iop_version()

hbase_javaopts_properties = str(hbase_javaopts_properties)
if hbase_javaopts_properties.find('-Diop.version') == -1:
  hbase_javaopts_properties = hbase_javaopts_properties+ ' -Diop.version=' + str(iop_full_version)

regionserver_heapsize = ensure_unit_for_memory(config['configurations']['hbase-env']['hbase_regionserver_heapsize'])
regionserver_xmn_max = config['configurations']['hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = expect("/configurations/hbase-env/hbase_regionserver_xmn_ratio", float) #AMBARI-15614
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)

pid_dir = status_params.pid_dir
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
local_dir = config['configurations']['hbase-site']['hbase.local.dir']

# TODO UPGRADE default, update site during upgrade
#_local_dir_conf = default('/configurations/hbase-site/hbase.local.dir', "${hbase.tmp.dir}/local")
#local_dir = substitute_vars(_local_dir_conf, config['configurations']['hbase-site'])

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', []) # is not passed when ganglia is not present
ganglia_server_host = '' if len(ganglia_server_hosts) == 0 else ganglia_server_hosts[0]

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_port and metric_collector_port.find(':') != -1:
    metric_collector_port = metric_collector_port.split(':')[1]
  pass

# if hbase is selected the hbase_rs_hosts, should not be empty, but still default just in case
if 'slave_hosts' in config['clusterHostInfo']:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/slave_hosts') #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
else:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/all_hosts')

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_permissions = "RWXCA"
service_check_data = functions.get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  master_jaas_princ = config['configurations']['hbase-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  regionserver_jaas_princ = config['configurations']['hbase-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  rest_server_jaas_princ = config['configurations']['hbase-site']['hbase.rest.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  rest_server_spnego_jaas_princ = config['configurations']['hbase-site']['hbase.rest.authentication.kerberos.principal'].replace('_HOST',_hostname_lowercase)

master_keytab_path = config['configurations']['hbase-site']['hbase.master.keytab.file']
regionserver_keytab_path = config['configurations']['hbase-site']['hbase.regionserver.keytab.file']
rest_server_keytab_path = config['configurations']['hbase-site']['hbase.rest.keytab.file']
rest_server_spnego_keytab_path = config['configurations']['hbase-site']['hbase.rest.authentication.kerberos.keytab']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hbase_user_keytab = config['configurations']['hbase-env']['hbase_user_keytab']
kinit_path_local = functions.get_kinit_path()
if security_enabled:
  kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_principal_name};")
else:
  kinit_cmd = ""

#log4j.properties
if (('hbase-log4j' in config['configurations']) and ('content' in config['configurations']['hbase-log4j'])):
  log4j_props = config['configurations']['hbase-log4j']['content']
else:
  log4j_props = None

hbase_env_sh_template = config['configurations']['hbase-env']['content']

hbase_hdfs_root_dir = config['configurations']['hbase-site']['hbase.rootdir']
hbase_staging_dir = "/apps/hbase/staging"
#for create_hdfs_directory
hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path()
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
)

if stack_version != "" and compare_versions(stack_version, '4.0') >= 0:
  command_role = default("/role", "")
  if command_role == "HBASE_MASTER" or command_role == "HBASE_REGIONSERVER":
    role_root = "master" if command_role == "HBASE_MASTER" else "regionserver"

    daemon_script=format("/usr/iop/current/hbase-{role_root}/bin/hbase-daemon.sh")
    region_mover = format("/usr/iop/current/hbase-{role_root}/bin/region_mover.rb")
    region_drainer = format("/usr/iop/current/hbase-{role_root}/bin/draining_servers.rb")
    hbase_cmd = format("/usr/iop/current/hbase-{role_root}/bin/hbase")
