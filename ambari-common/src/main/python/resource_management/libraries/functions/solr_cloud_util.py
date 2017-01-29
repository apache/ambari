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
import random
from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_jinja2 import Environment as JinjaEnvironment
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import StaticFile

__all__ = ["upload_configuration_to_zk", "create_collection", "setup_kerberos", "set_cluster_prop",
           "setup_kerberos_plugin", "create_znode", "check_znode", "secure_solr_znode", "secure_znode"]

def __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, separated_znode=False):
  sudo = AMBARI_SUDO_BINARY
  solr_cli_prefix = format('{sudo} JAVA_HOME={java64_home} /usr/lib/ambari-infra-solr-client/solrCloudCli.sh ' \
                           '--zookeeper-connect-string {zookeeper_quorum}')
  if separated_znode:
    solr_cli_prefix+=format(' --znode {solr_znode}')
  else:
    solr_cli_prefix+=format('{solr_znode}')
  return solr_cli_prefix

def __append_flags_if_exists(command, flagsDict):
  for key, value in flagsDict.iteritems():
    if value is not None:
        command+= " %s %s" % (key, value)
  return command

def upload_configuration_to_zk(zookeeper_quorum, solr_znode, config_set, config_set_dir, tmp_dir,
                         java64_home, retry = 5, interval = 10, solrconfig_content = None, jaas_file=None):
  """
  Upload configuration set to zookeeper with solrCloudCli.sh
  At first, it tries to download configuration set if exists into a temporary location, then upload that one to
  zookeeper. If the configuration set does not exist in zookeeper then upload it based on the config_set_dir parameter.
  """
  random_num = random.random()
  tmp_config_set_dir = format('{tmp_dir}/solr_config_{config_set}_{random_num}')
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home)
  Execute(format('{solr_cli_prefix} --download-config --config-dir {tmp_config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}'),
          only_if=format("{solr_cli_prefix} --check-config --config-set {config_set} --retry {retry} --interval {interval}"))
  appendableDict = {}
  appendableDict["--jaas-file"] = jaas_file

  if solrconfig_content is not None:
      File(format("{tmp_config_set_dir}/solrconfig.xml"),
       content=solrconfig_content,
       only_if=format("test -d {tmp_config_set_dir}")
      )
      upload_tmp_config_cmd = format('{solr_cli_prefix} --upload-config --config-dir {tmp_config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}')
      upload_tmp_config_cmd = __append_flags_if_exists(upload_tmp_config_cmd, appendableDict)
      Execute(upload_tmp_config_cmd,
        only_if=format("test -d {tmp_config_set_dir}")
      )
  upload_config_cmd = format('{solr_cli_prefix} --upload-config --config-dir {config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}')
  upload_config_cmd = __append_flags_if_exists(upload_config_cmd, appendableDict)
  Execute(upload_config_cmd,
    not_if=format("test -d {tmp_config_set_dir}")
  )

  Directory(tmp_config_set_dir,
              action="delete",
              create_parents=True
            )

def create_collection(zookeeper_quorum, solr_znode, collection, config_set, java64_home,
                      shards = 1, replication_factor = 1, max_shards = 1, retry = 5, interval = 10,
                      router_name = None, router_field = None, jaas_file = None, key_store_location = None,
                      key_store_password = None, key_store_type = None, trust_store_location = None,
                      trust_store_password = None, trust_store_type = None):
  """
  Create Solr collection based on a configuration set in zookeeper.
  If this method called again the with higher shard number (or max_shard number), then it will indicate
  the cli tool to add new shards to the Solr collection. This can be useful after added a new Solr Cloud
  instance to the cluster.

  If you would like to add shards later to a collection, then use implicit routing, e.g.:
  router_name = "implicit", router_field = "_router_field_"
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home)

  if max_shards == 1: # if max shards is not specified use this strategy
    max_shards = replication_factor * shards

  create_collection_cmd = format('{solr_cli_prefix} --create-collection --collection {collection} --config-set {config_set} '\
                                 '--shards {shards} --replication {replication_factor} --max-shards {max_shards} --retry {retry} '\
                                 '--interval {interval} --no-sharding')
  appendableDict = {}
  appendableDict["--router-name"] = router_name
  appendableDict["--router-field"] = router_field
  appendableDict["--jaas-file"] = jaas_file
  appendableDict["--key-store-location"] = key_store_location
  appendableDict["--key-store-password"] = None if key_store_password is None else '{key_store_password_param!p}'
  appendableDict["--key-store-type"] = key_store_type
  appendableDict["--trust-store-location"] = trust_store_location
  appendableDict["--trust-store-password"] = None if trust_store_password is None else '{trust_store_password_param!p}'
  appendableDict["--trust-store-type"] = trust_store_type
  create_collection_cmd = __append_flags_if_exists(create_collection_cmd, appendableDict)
  create_collection_cmd = format(create_collection_cmd, key_store_password_param=key_store_password, trust_store_password_param=trust_store_password)

  Execute(create_collection_cmd)

def setup_kerberos(zookeeper_quorum, solr_znode, copy_from_znode, java64_home, secure=False, jaas_file=None):
  """
  Copy all unsecured (or secured) Znode content to a secured (or unsecured) Znode,
  and restrict the world permissions there.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, True)
  setup_kerberos_cmd = format('{solr_cli_prefix} --setup-kerberos --copy-from-znode {copy_from_znode}')
  if secure and jaas_file is not None:
    setup_kerberos_cmd+=format(' --secure --jaas-file {jaas_file}')
  Execute(setup_kerberos_cmd)

def check_znode(zookeeper_quorum, solr_znode, java64_home, retry = 5, interval = 10):
  """
  Check znode exists or not, throws exception if does not accessible.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, True)
  check_znode_cmd = format('{solr_cli_prefix} --check-znode --retry {retry} --interval {interval}')
  Execute(check_znode_cmd)

def create_znode(zookeeper_quorum, solr_znode, java64_home, retry = 5 , interval = 10):
  """
  Create znode if does not exists, throws exception if zookeeper is not accessible.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, True)
  create_znode_cmd = format('{solr_cli_prefix} --create-znode --retry {retry} --interval {interval}')
  Execute(create_znode_cmd)

def setup_kerberos_plugin(zookeeper_quorum, solr_znode, java64_home, secure=False, security_json_location = None, jaas_file = None):
  """
  Set Kerberos plugin on the Solr znode in security.json, if secure is False, then clear the security.json
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, True)
  setup_kerberos_plugin_cmd = format('{solr_cli_prefix} --setup-kerberos-plugin')
  if secure and jaas_file is not None and security_json_location is not None:
    setup_kerberos_plugin_cmd+=format(' --jaas-file {jaas_file} --secure --security-json-location {security_json_location}')
  Execute(setup_kerberos_plugin_cmd)

def set_cluster_prop(zookeeper_quorum, solr_znode, prop_name, prop_value, java64_home, jaas_file = None):
  """
  Set a cluster property on the Solr znode in clusterprops.json
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home)
  set_cluster_prop_cmd = format('{solr_cli_prefix} --cluster-prop --property-name {prop_name} --property-value {prop_value}')
  if jaas_file is not None:
    set_cluster_prop_cmd+=format(' --jaas-file {jaas_file}')
  Execute(set_cluster_prop_cmd)

def secure_znode(zookeeper_quorum, solr_znode, jaas_file, java64_home, sasl_users=[]):
  """
  Secure znode, set a list of sasl users acl to 'cdrwa', and set acl to 'r' only for the world. 
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, True)
  sasl_users_str = ",".join(str(x) for x in sasl_users)
  secure_znode_cmd = format('{solr_cli_prefix} --secure-znode --jaas-file {jaas_file} --sasl-users {sasl_users_str}')
  Execute(secure_znode_cmd)


def secure_solr_znode(zookeeper_quorum, solr_znode, jaas_file, java64_home, sasl_users_str=''):
  """
  Secure solr znode - setup acls to 'cdrwa' for solr user, set 'r' only for the world, skipping /znode/configs and znode/collections (set those to 'cr' for the world)
  sasl_users_str: comma separated sasl users
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home, True)
  secure_solr_znode_cmd = format('{solr_cli_prefix} --secure-solr-znode --jaas-file {jaas_file} --sasl-users {sasl_users_str}')
  Execute(secure_solr_znode_cmd)

def default_config(config, name, default_value):
  subdicts = filter(None, name.split('/'))
  if not config:
    return default_value
  for x in subdicts:
    if x in config:
      config = config[x]
    else:
      return default_value
  return config

def setup_solr_client(config, custom_log4j = True, custom_log_location = None, log4jcontent = None):
    solr_client_dir = '/usr/lib/ambari-infra-solr-client'
    solr_client_log_dir = default_config(config, '/configurations/infra-solr-client-log4j/infra_solr_client_log_dir', '/var/log/ambari-infra-solr-client') if custom_log_location is None else custom_log_location
    solr_client_log = format("{solr_client_log_dir}/solr-client.log")
    solr_client_log_maxfilesize =  default_config(config, 'configurations/infra-solr-client-log4j/infra_client_log_maxfilesize', 80)
    solr_client_log_maxbackupindex =  default_config(config, 'configurations/infra-solr-client-log4j/infra_client_log_maxbackupindex', 60)

    Directory(solr_client_log_dir,
                mode=0755,
                cd_access='a',
                create_parents=True
                )
    Directory(solr_client_dir,
                mode=0755,
                cd_access='a',
                create_parents=True,
                recursive_ownership=True
                )
    solrCliFilename = format("{solr_client_dir}/solrCloudCli.sh")
    File(solrCliFilename,
         mode=0755,
         content=StaticFile(solrCliFilename)
         )
    if custom_log4j:
      # use custom log4j content only, when infra is not installed on the cluster
      solr_client_log4j_content = config['configurations']['infra-solr-client-log4j']['content'] if log4jcontent is None else log4jcontent
      context = {
        'solr_client_log': solr_client_log,
        'solr_client_log_maxfilesize': solr_client_log_maxfilesize,
        'solr_client_log_maxbackupindex': solr_client_log_maxbackupindex
      }
      template = JinjaEnvironment(
        line_statement_prefix='%',
        variable_start_string="{{",
        variable_end_string="}}")\
        .from_string(solr_client_log4j_content)

      File(format("{solr_client_dir}/log4j.properties"),
             content=template.render(context),
             mode=0644
             )
    else:
        File(format("{solr_client_dir}/log4j.properties"),
             mode=0644
             )

    File(solr_client_log,
         mode=0664,
         content=''
         )
