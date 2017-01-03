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
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.script.script import Script
import os
import status_params

def get_port_from_url(address):
  if not is_empty(address):
    return address.split(':')[-1]
  else:
    return address

# config object that holds the configurations declared in the -site.xml file
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_version = default("/commandParams/version", None)
sudo = AMBARI_SUDO_BINARY
security_enabled = status_params.security_enabled

infra_solr_conf = "/etc/ambari-infra-solr/conf"

infra_solr_port = status_params.infra_solr_port
infra_solr_piddir = status_params.infra_solr_piddir
infra_solr_pidfile = status_params.infra_solr_pidfile

user_group = config['configurations']['cluster-env']['user_group']
fetch_nonlocal_groups = config['configurations']['cluster-env']["fetch_nonlocal_groups"]

# shared configs
java64_home = config['hostLevelParams']['java_home']
zookeeper_hosts_list = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts_list.sort()
# get comma separated list of zookeeper hosts from clusterHostInfo
zookeeper_hosts = ",".join(zookeeper_hosts_list)

#####################################
# Solr configs
#####################################

# Only supporting SolrCloud mode - so hardcode those options
solr_cloudmode = 'true'
solr_dir = '/usr/lib/ambari-infra-solr'
solr_client_dir = '/usr/lib/ambari-infra-solr-client'
solr_bindir = solr_dir + '/bin'
cloud_scripts = solr_dir + '/server/scripts/cloud-scripts'

if "infra-solr-env" in config['configurations']:
  infra_solr_hosts = config['clusterHostInfo']['infra_solr_hosts']
  infra_solr_znode = config['configurations']['infra-solr-env']['infra_solr_znode']
  infra_solr_min_mem = format(config['configurations']['infra-solr-env']['infra_solr_minmem'])
  infra_solr_max_mem = format(config['configurations']['infra-solr-env']['infra_solr_maxmem'])
  infra_solr_instance_count = len(config['clusterHostInfo']['infra_solr_hosts'])
  infra_solr_datadir = format(config['configurations']['infra-solr-env']['infra_solr_datadir'])
  infra_solr_data_resources_dir = os.path.join(infra_solr_datadir, 'resources')
  infra_solr_jmx_port = config['configurations']['infra-solr-env']['infra_solr_jmx_port']
  infra_solr_ssl_enabled = default('configurations/infra-solr-env/infra_solr_ssl_enabled', False)
  infra_solr_keystore_location = config['configurations']['infra-solr-env']['infra_solr_keystore_location']
  infra_solr_keystore_password = config['configurations']['infra-solr-env']['infra_solr_keystore_password']
  infra_solr_keystore_type = config['configurations']['infra-solr-env']['infra_solr_keystore_type']
  infra_solr_truststore_location = config['configurations']['infra-solr-env']['infra_solr_truststore_location']
  infra_solr_truststore_password = config['configurations']['infra-solr-env']['infra_solr_truststore_password']
  infra_solr_truststore_type = config['configurations']['infra-solr-env']['infra_solr_truststore_type']
  infra_solr_user = config['configurations']['infra-solr-env']['infra_solr_user']
  infra_solr_log_dir = config['configurations']['infra-solr-env']['infra_solr_log_dir']
  infra_solr_log = format("{infra_solr_log_dir}/solr-install.log")
  solr_env_content = config['configurations']['infra-solr-env']['content']

zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
# get comma separated list of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
for host in config['clusterHostInfo']['zookeeper_hosts']:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(config['clusterHostInfo']['zookeeper_hosts']):
    zookeeper_quorum += ","


if security_enabled:
  kinit_path_local = status_params.kinit_path_local
  _hostname_lowercase = config['hostname'].lower()
  infra_solr_jaas_file = infra_solr_conf + '/infra_solr_jaas.conf'
  infra_solr_kerberos_keytab = config['configurations']['infra-solr-env']['infra_solr_kerberos_keytab']
  infra_solr_kerberos_principal = config['configurations']['infra-solr-env']['infra_solr_kerberos_principal'].replace('_HOST',_hostname_lowercase)
  infra_solr_web_kerberos_keytab = config['configurations']['infra-solr-env']['infra_solr_web_kerberos_keytab']
  infra_solr_web_kerberos_principal = config['configurations']['infra-solr-env']['infra_solr_web_kerberos_principal'].replace('_HOST',_hostname_lowercase)
  infra_solr_kerberos_name_rules = config['configurations']['infra-solr-env']['infra_solr_kerberos_name_rules'].replace('$', '\$')

#Solr log4j
infra_log_maxfilesize = default('configurations/infra-solr-log4j/infra_log_maxfilesize',10)
infra_log_maxbackupindex = default('configurations/infra-solr-log4j/infra_log_maxbackupindex',9)

solr_xml_content = default('configurations/infra-solr-xml/content', None)
solr_log4j_content = default('configurations/infra-solr-log4j/content', None)

smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

