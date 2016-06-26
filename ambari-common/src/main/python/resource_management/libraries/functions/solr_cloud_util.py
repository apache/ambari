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
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.shell import as_user
from resource_management.core.source import StaticFile, InlineTemplate

__all__ = ["upload_configuration_to_zk", "create_collection"]

def __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home):
  solr_cli_prefix = format('export JAVA_HOME={java64_home} ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh ' \
                           '--zookeeper-connect-string {zookeeper_quorum}{solr_znode}')
  return solr_cli_prefix

def __append_flags_if_exists(command, flagsDict):
  for key, value in flagsDict.iteritems():
    if value is not None:
        command+= " %s %s" % (key, value)
  return command


def upload_configuration_to_zk(zookeeper_quorum, solr_znode, config_set, config_set_dir, tmp_config_set_dir,
                         java64_home, user, retry = 5, interval = 10):
  """
  Upload configuration set to zookeeper with solrCloudCli.sh
  At first, it tries to download configuration set if exists into a temporary location, then upload that one to
  zookeeper. (if the configuration changed there, in that case the user wont redefine it)
  If the configuration set does not exits in zookeeper then upload it based on the config_set_dir parameter.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home)
  Execute(format('{solr_cli_prefix} --download-config --config-dir {tmp_config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}'),
          only_if=as_user(format("{solr_cli_prefix} --check-config --config-set{config_set} --retry {retry} --interval {interval}"), user),
          user=user
          )

  Execute(format(
    '{solr_cli_prefix} --upload-config --config-dir {config_set_dir} --config-set {config_set} --retry {retry} --interval {interval}'),
    not_if=format("test -d {tmp_config_set_dir}"),
    user=user
  )

def create_collection(zookeeper_quorum, solr_znode, collection, config_set, java64_home, user,
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


  Execute(create_collection_cmd, user=user)

def setup_solr_client(config, user = None, group = None, custom_log4j = True, custom_log_location = None, log4jcontent = None):
    solr_user = config['configurations']['logsearch-solr-env']['logsearch_solr_user'] if user is None else user
    solr_group = config['configurations']['cluster-env']['user_group'] if group is None else group
    solr_client_dir = '/usr/lib/ambari-logsearch-solr-client'
    solr_client_log_dir = default('/configurations/logsearch-solr-env/logsearch_solr_client_log_dir', '/var/log/ambari-logsearch-solr-client') if custom_log_location is None else custom_log_location
    solr_client_log = format("{solr_client_log_dir}/solr-client.log")

    Directory(solr_client_log_dir,
                mode=0755,
                cd_access='a',
                owner=solr_user,
                group=solr_group,
                create_parents=True
                )
    Directory(solr_client_dir,
                mode=0755,
                cd_access='a',
                owner=solr_user,
                group=solr_group,
                create_parents=True,
                recursive_ownership=True
                )
    solrCliFilename = format("{solr_client_dir}/solrCloudCli.sh")
    File(solrCliFilename,
         mode=0755,
         owner=solr_user,
         group=solr_group,
         content=StaticFile(solrCliFilename)
         )
    if custom_log4j:
      # use custom log4j content only, when logsearch is not installed on the cluster
      solr_client_log4j_content = config['configurations']['logsearch-solr-client-log4j']['content'] if log4jcontent is None else log4jcontent
      File(format("{solr_client_dir}/log4j.properties"),
             content=InlineTemplate(solr_client_log4j_content),
             owner=solr_user,
             group=solr_group,
             mode=0644
             )
    else:
        File(format("{solr_client_dir}/log4j.properties"),
             owner=solr_user,
             group=solr_group,
             mode=0644
             )

    File(solr_client_log,
         mode=0644,
         owner=solr_user,
         group=solr_group,
         content=''
         )
