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
from resource_management.libraries.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.resources import HdfsResource
import status_params

# server configurations
config = Script.get_config()
stack_name = default("/hostLevelParams/stack_name", None)
security_enabled = config['configurations']['cluster-env']['security_enabled']

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts.sort()
zookeeper_hosts_list=','.join(zookeeper_hosts)

java64_home = config['hostLevelParams']['java_home']

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade.
# Version being upgraded/downgraded to
# It cannot be used during the initial Cluser Install because the version is not yet known.
version = default("/commandParams/version", None)

# current host stack version
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
iop_stack_version = format_stack_version(stack_version_unformatted)

# Upgrade direction
upgrade_direction = default("/commandParams/upgrade_direction", None)

solr_user=config['configurations']['solr-env']['solr_user']
user_group=config['configurations']['cluster-env']['user_group']
hostname = config['hostname']
solr_server_hosts = config['clusterHostInfo']['solr_hosts'] 
solr_server_host = solr_server_hosts[0]

fs_root = config['configurations']['core-site']['fs.defaultFS']

solr_home = '/usr/iop/current/solr-server'
solr_conf_dir='/usr/iop/current/solr-server/conf'
solr_conf = solr_conf_dir
cloud_scripts=solr_home+'/server/scripts/cloud-scripts'

solr_piddir = status_params.solr_pid_dir
solr_pidfile = status_params.solr_pid_file

if "solr-env" in config['configurations']:
  #solr_hosts = config['clusterHostInfo']['solr_hosts']
  solr_znode = default('/configurations/solr-env/solr_znode', '/solr')
  solr_min_mem = default('/configurations/solr-env/solr_minmem', 1024)
  solr_max_mem = default('/configurations/solr-env/solr_maxmem', 2048)
  #solr_instance_count = len(config['clusterHostInfo']['solr_hosts'])
  solr_datadir = default('/configurations/solr-env/solr_datadir', '/opt/solr/data')
  #solr_data_resources_dir = os.path.join(solr_datadir, 'resources')
  solr_jmx_port = default('/configurations/solr-env/solr_jmx_port', 18983)  
  solr_ssl_enabled = default('configurations/solr-env/solr_ssl_enabled', False)
  solr_keystore_location = default('/configurations/solr-env/solr_keystore_location', '/etc/security/serverKeys/solr.keyStore.jks')
  solr_keystore_password = default('/configurations/solr-env/solr_keystore_password', 'bigdata')
  solr_keystore_type = default('/configurations/solr-env/solr_keystore_type', 'jks')
  #solr_truststore_location = config['configurations']['solr-env']['solr_truststore_location']
  #solr_truststore_password = config['configurations']['solr-env']['solr_truststore_password']
  #solr_truststore_type = config['configurations']['solr-env']['solr_truststore_type']
  #solr_user = config['configurations']['solr-env']['solr_user']
  solr_log_dir = config['configurations']['solr-env']['solr_log_dir']
  solr_log = format("{solr_log_dir}/solr-install.log")
  #solr_env_content = config['configurations']['solr-env']['content']
  solr_hdfs_home_dir = config['configurations']['solr-env']['solr_hdfs_home_dir']

zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
# get comma separated list of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
for host in config['clusterHostInfo']['zookeeper_hosts']:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(config['clusterHostInfo']['zookeeper_hosts']):
    zookeeper_quorum += ","

if (version is not None and compare_versions(format_stack_version(version), '4.2.0.0') >=0 ) or  compare_versions(iop_stack_version, '4.2.0.0')>= 0:
  if upgrade_direction is not None and upgrade_direction == Direction.DOWNGRADE and version is not None and compare_versions(format_stack_version(version), '4.2.0.0') < 0:
    solr_data_dir=default("/configurations/solr-env/solr_lib_dir", None)
  else:
    solr_data_dir=default("/configurations/solr-env/solr_data_dir", None)
else: #IOP 4.1
  if upgrade_direction is not None and upgrade_direction == Direction.UPGRADE:
    solr_data_dir=default("/configurations/solr-env/solr_data_dir", None)
    lib_dir=default("/configurations/solr-env/solr_data_dir", None)
    old_lib_dir=default("/configurations/solr-env/solr_lib_dir", None)
  else:
    solr_data_dir=default("/configurations/solr-env/solr_lib_dir", None)
    lib_dir=default("/configurations/solr-env/solr_lib_dir", None)
log_dir=config['configurations']['solr-env']['solr_log_dir']
pid_dir=config['configurations']['solr-env']['solr_pid_dir']
solr_port=config['configurations']['solr-env']['solr_port']

zookeeper_chroot=config['configurations']['solr-env']['ZOOKEEPER_CHROOT']

solr_xms_minmem = config['configurations']['solr-env']['solr_xms_minmem']
solr_xmx_maxmem = config['configurations']['solr-env']['solr_xmx_maxmem']

solr_site = dict(config['configurations']['solr-site'])
solr_principal = solr_site['solr.hdfs.security.kerberos.principal']

if security_enabled:
  solr_principal = solr_principal.replace('_HOST',hostname)
  solr_site['solr.hdfs.security.kerberos.principal']=solr_principal

#kerberos
sole_kerberos_enabled=config['configurations']['solr-site']['solr.hdfs.security.kerberos.enabled']
solr_keytab=config['configurations']['solr-site']['solr.hdfs.security.kerberos.keytabfile']

#log4j.properties
log4j_props = config['configurations']['solr-log4j']['content']

solr_in_sh_template = config['configurations']['solr-env']['content']

solr_pid_file = status_params.solr_pid_file

solr_hdfs_home_dir = config['configurations']['solr-env']['solr_hdfs_home_dir']
solr_hdfs_user_mode = 0755

smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = get_kinit_path()

# parameters for intgeration with Titan
configuration_tags = config['configurationTags']

# Intgerate with Titan
# parse the value for property 'index.search.solr.configset' in titan-hbase-solr
titan_solr_configset = 'titan'
if ('titan-hbase-solr' in configuration_tags):
    titan_hbase_solr_props = config['configurations']['titan-hbase-solr']['content']
    prop_list = titan_hbase_solr_props.split('\n')
    for prop in prop_list:
      if (prop.find('index.search.solr.configset') > -1):
         titan_solr_configset_prop = prop.split('=')
         titan_solr_configset = titan_solr_configset_prop[1]

titan_solr_conf_dir = format('/usr/iop/current/titan-client/conf.dist/solr')
solr_conf_trg_dir = format('/usr/iop/current/solr-server/server/solr/configsets')
solr_solr_conf_dir = format('/usr/iop/current/solr-server/server/solr/configsets/solr')
solr_titan_conf_dir = format('/usr/iop/current/solr-server/server/solr/configsets/{titan_solr_configset}')
titan_solr_jar_file = format('/usr/iop/current/titan-client/lib/jts-1.13.jar')
solr_jar_trg_file =  format('/usr/iop/current/solr-server/server/solr-webapp/webapp/WEB-INF/lib/jts-1.13.jar')
solr_conf_trg_file = format('/usr/iop/current/solr-server/server/solr/configsets/{titan_solr_configset}/solrconfig.xml')

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  solr_jaas_file = solr_conf + '/solr_jaas.conf'
  solr_kerberos_keytab = solr_keytab
  solr_kerberos_principal = solr_principal #Cannot use the one from solr-env, otherwise default @EXAMPLE.COM is in the real value
  solr_web_kerberos_keytab = default('/configurations/solr-env/solr_web_kerberos_keytab', None)
  solr_web_kerberos_principal = default('/configurations/solr-env/solr_web_kerberos_principal', None)
  if solr_web_kerberos_principal:
    solr_web_kerberos_principal = solr_web_kerberos_principal.replace('_HOST',_hostname_lowercase)
  solr_kerberos_name_rules = default('/configurations/solr-env/solr_kerberos_name_rules', 'DEFAULT')

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

