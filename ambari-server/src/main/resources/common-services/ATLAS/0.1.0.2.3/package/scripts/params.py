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
import os
import sys
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

import status_params

# server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()

cluster_name = config['clusterName']

# security enabled
security_enabled = status_params.security_enabled

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  _atlas_principal_name = config['configurations']['application-properties']['atlas.authentication.principal']
  atlas_jaas_principal = _atlas_principal_name.replace('_HOST',_hostname_lowercase)
  atlas_keytab_path = config['configurations']['application-properties']['atlas.authentication.keytab']

stack_name = status_params.stack_name

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)

# stack version
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

metadata_home = os.environ['METADATA_HOME_DIR'] if 'METADATA_HOME_DIR' in os.environ else format('{stack_root}/current/atlas-server')
metadata_bin = format("{metadata_home}/bin")

python_binary = os.environ['PYTHON_EXE'] if 'PYTHON_EXE' in os.environ else sys.executable
metadata_start_script = format("{metadata_bin}/atlas_start.py")
metadata_stop_script = format("{metadata_bin}/atlas_stop.py")

# metadata local directory structure
log_dir = config['configurations']['atlas-env']['metadata_log_dir']
conf_dir = status_params.conf_dir # "/etc/metadata/conf"
conf_file = status_params.conf_file

atlas_login_credentials_file = os.path.join(conf_dir, "users-credentials.properties")
atlas_policy_store_file = os.path.join(conf_dir, "policy-store.txt")

atlas_hbase_conf_dir = os.path.join(metadata_home, "hbase", "conf")
atlas_hbase_log_dir = os.path.join(metadata_home, "hbase", "logs")
atlas_hbase_data_dir = os.path.join(metadata_home, "data")
atlas_hbase_zk_port = default("/configurations/atlas-hbase-site/hbase.zookeeper.property.clientPort", None)

atlas_has_embedded_hbase = default("/configurations/atlas-env/has_embedded_hbase", False)

# service locations
hadoop_conf_dir = os.path.join(os.environ["HADOOP_HOME"], "conf") if 'HADOOP_HOME' in os.environ else '/etc/hadoop/conf'

# some commands may need to supply the JAAS location when running as atlas
atlas_jaas_file = format("{conf_dir}/atlas_jaas.conf")

# user and status
metadata_user = status_params.metadata_user
user_group = config['configurations']['cluster-env']['user_group']
pid_dir = status_params.pid_dir
pid_file = format("{pid_dir}/atlas.pid")

# metadata env
java64_home = config['hostLevelParams']['java_home']
env_sh_template = config['configurations']['atlas-env']['content']

# credential provider
credential_provider = format( "jceks://file@{conf_dir}/atlas-site.jceks")

# command line args
ssl_enabled = default("/configurations/application-properties/atlas.enableTLS", False)
http_port = default("/configurations/application-properties/atlas.server.http.port", 21000)
https_port = default("/configurations/application-properties/atlas.server.https.port", 21443)
if ssl_enabled:
  metadata_port = https_port
  metadata_protocol = 'https'
else:
  metadata_port = http_port
  metadata_protocol = 'http'

metadata_host = config['hostname']

# application properties
application_properties = dict(config['configurations']['application-properties'])
application_properties['atlas.server.bind.address'] = metadata_host

metadata_env_content = config['configurations']['atlas-env']['content']

metadata_opts = config['configurations']['atlas-env']['metadata_opts']
metadata_classpath = config['configurations']['atlas-env']['metadata_classpath']
data_dir = format("{stack_root}/current/atlas-server/data")
expanded_war_dir = os.environ['METADATA_EXPANDED_WEBAPP_DIR'] if 'METADATA_EXPANDED_WEBAPP_DIR' in os.environ else format("{stack_root}/current/atlas-server/server/webapp")

metadata_log4j_content = config['configurations']['atlas-log4j']['content']

atlas_log_level = config['configurations']['atlas-log4j']['atlas_log_level']
audit_log_level = config['configurations']['atlas-log4j']['audit_log_level']

# smoke test
smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smoke_test_password = 'smoke'
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

kinit_path_local = status_params.kinit_path_local

security_check_status_file = format('{log_dir}/security_check.status')
if security_enabled:
    smoke_cmd = format('curl --negotiate -u : -b ~/cookiejar.txt -c ~/cookiejar.txt -s -o /dev/null -w "%{{http_code}}" {metadata_protocol}://{metadata_host}:{metadata_port}/')
else:
    smoke_cmd = format('curl -s -o /dev/null -w "%{{http_code}}" {metadata_protocol}://{metadata_host}:{metadata_port}/')

# kafka
kafka_bootstrap_servers = ""
kafka_broker_hosts = default('/clusterHostInfo/kafka_broker_hosts', [])

if not len(kafka_broker_hosts) == 0:
  kafka_broker_port = default("/configurations/kafka-broker/port", 6667)
  kafka_bootstrap_servers = kafka_broker_hosts[0] + ":" + str(kafka_broker_port)

kafka_zookeeper_connect = default("/configurations/kafka-broker/zookeeper.connect", None)

# atlas HA
atlas_hosts = sorted(default('/clusterHostInfo/atlas_server_hosts', []))

id = 1
server_ids = ""
server_hosts = ""
first_id = True
for host in atlas_hosts:
  server_id = "id" + str(id)
  server_host = host + ":" + metadata_port
  if first_id:
    server_ids = server_id
    server_hosts = server_host
  else:
    server_ids += "," + server_id
    server_hosts += "\n" + "atlas.server.address." + server_id + "=" + server_host

  id += 1
  first_id = False

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
logsearch_solr_znode = default("/configurations/logsearch-solr-env/logsearch_solr_znode", None)

# get comma separated lists of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
solr_zookeeper_url = ""

for host in zookeeper_hosts:
  zookeeper_host = host + ":" + str(zookeeper_port)

  if logsearch_solr_znode is not None:
    solr_zookeeper_url += zookeeper_host + logsearch_solr_znode

  zookeeper_quorum += zookeeper_host
  index += 1
  if index < len(zookeeper_hosts):
    zookeeper_quorum += ","
    solr_zookeeper_url += ","
