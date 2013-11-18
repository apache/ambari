#!/usr/bin/env python2.6
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

from resource_management import *
import functions

# server configurations
config = Script.get_config()

conf_dir = "/etc/hbase/conf"
daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"

hbase_user = config['configurations']['global']['hbase_user']
smokeuser = config['configurations']['global']['smokeuser']
security_enabled = config['configurations']['global']['security_enabled']
user_group = config['configurations']['global']['user_group']

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['configurations']['global']['java64_home']

log_dir = config['configurations']['global']['hbase_log_dir']
master_heapsize = config['configurations']['global']['hbase_master_heapsize']

regionserver_heapsize = config['configurations']['global']['hbase_regionserver_heapsize']
regionserver_xmn_size = functions.calc_xmn_from_xms(regionserver_heapsize, 0.2, 512)

pid_dir = config['configurations']['global']['hbase_pid_dir']
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']

client_jaas_config_file = default('hbase_client_jaas_config_file', format("{conf_dir}/hbase_client_jaas.conf"))
master_jaas_config_file = default('hbase_master_jaas_config_file', format("{conf_dir}/hbase_master_jaas.conf"))
regionserver_jaas_config_file = default('hbase_regionserver_jaas_config_file', format("{conf_dir}/hbase_regionserver_jaas.conf"))
ganglia_server_host = default('ganglia_server_host', "") # is not passed when ganglia is not present
rs_hosts = default('hbase_rs_hosts', config['clusterHostInfo']['slave_hosts']) #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves

smoke_test_user = config['configurations']['global']['smokeuser']
smokeuser_permissions = default('smokeuser_permissions', "RWXCA")
service_check_data = functions.get_unique_id_and_date()

if security_enabled:
  
  _use_hostname_in_principal = default('instance_name', True)
  _master_primary_name = config['configurations']['global']['hbase_master_primary_name']
  _hostname = default('/hostname')
  _kerberos_domain = config['configurations']['global']['kerberos_domain']
  _master_principal_name = config['configurations']['global']['hbase_master_principal_name']
  _regionserver_primary_name = config['configurations']['global']['hbase_regionserver_primary_name']
  
  if _use_hostname_in_principal:
    master_jaas_princ = format("{_master_primary_name}/{_hostname}@{_kerberos_domain}")
    regionserver_jaas_princ = format("{_regionserver_primary_name}/{_hostname}@{_kerberos_domain}")
  else:
    master_jaas_princ = format("{_master_principal_name}@{_kerberos_domain}")
    regionserver_jaas_princ = format("{_regionserver_primary_name}@{_kerberos_domain}")
    
master_keytab_path = default('configurations/hbase-site/hbase.master.keytab.file')
regionserver_keytab_path = default('/configurations/hbase-site/hbase.regionserver.keytab.file')
smoke_user_keytab = default('smokeuser_keytab')
hbase_user_keytab = default('hbase_user_keytab')
kinit_path_local = functions.get_kinit_path([default('kinit_path_local'),"/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

# fix exeuteHadoop calls for secured cluster
# to string template...
