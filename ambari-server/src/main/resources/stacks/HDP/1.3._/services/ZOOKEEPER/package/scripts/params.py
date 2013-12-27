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
import status_params

# server configurations
config = Script.get_config()

config_dir = "/etc/zookeeper/conf"
zk_user =  config['configurations']['global']['zk_user']
hostname = config['hostname']
zk_bin = '/usr/lib/zookeeper/bin'
user_group = config['configurations']['global']['user_group']

smoke_script = "/usr/lib/zookeeper/bin/zkCli.sh"

zk_log_dir = config['configurations']['global']['zk_log_dir']
zk_data_dir = config['configurations']['global']['zk_data_dir']
zk_pid_dir = status_params.zk_pid_dir
zk_pid_file = status_params.zk_pid_file
zk_server_heapsize = "-Xmx1024m"

tickTime = config['configurations']['global']['tickTime']
initLimit = config['configurations']['global']['initLimit']
syncLimit = config['configurations']['global']['syncLimit']
clientPort = config['configurations']['global']['clientPort']

zk_primary_name = "zookeeper"
zk_principal_name = "zookeeper/_HOST@EXAMPLE.COM"
zk_principal = zk_principal_name.replace('_HOST',hostname)

java64_home = config['configurations']['global']['java64_home']

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']

keytab_path = "/etc/security/keytabs"
zk_keytab_path = format("{keytab_path}/zk.service.keytab")
zk_server_jaas_file = format("{config_dir}/zookeeper_jaas.conf")
zk_client_jaas_file = format("{config_dir}/zookeeper_client_jaas.conf")
security_enabled = config['configurations']['global']['security_enabled']

smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
smokeuser = config['configurations']['global']['smokeuser']
kinit_path_local = get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
