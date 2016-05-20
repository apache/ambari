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
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute

__all__ = ["upload_configuration_to_zk", "create_collection"]

def __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home):
  solr_cli_prefix = format('export JAVA_HOME={java64_home} ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh ' \
                           '-z {zookeeper_quorum}{solr_znode}')
  return solr_cli_prefix

def upload_configuration_to_zk(zookeeper_quorum, solr_znode, config_set, config_set_dir, tmp_config_set_dir,
                         java64_home, user, group, retry = 5, interval = 10):
  """
  Upload configuration set to zookeeper with solrCloudCli.sh
  At first, it tries to download configuration set if exists into a temporary location, then upload that one to
  zookeeper. (if the configuration changed there, in that case the user wont redefine it)
  If the configuration set does not exits in zookeeper then upload it based on the config_set_dir parameter.
  """
  solr_cli_prefix = __create_solr_cloud_cli_prefix(zookeeper_quorum, solr_znode, java64_home)
  Execute(format('{solr_cli_prefix} --download-config -d {tmp_config_set_dir} -cs {config_set} -rt {retry} -i {interval}'),
          only_if=format("{solr_cli_prefix} --check-config -cs {config_set} -rt {retry} -i {interval}"),
          user=user,
          group=group
          )

  Execute(format(
    '{solr_cli_prefix} --upload-config -d {config_set_dir} -cs {config_set} -rt {retry} -i {interval}'),
    not_if=format("test -d {tmp_config_set_dir}"),
    user=user,
    group=group
  )

def create_collection(zookeeper_quorum, solr_znode, collection, config_set, java64_home, user, group,
                      shards = 1, replication_factor = 1, max_shards = 1, retry = 5, interval = 10,
                      router_name = None, router_field = None):
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

  create_collection_cmd = format('{solr_cli_prefix} --create-collection -c {collection} -cs {config_set} -s {shards} -r {replication_factor} '\
    '-m {max_shards} -rt {retry} -i {interval} -ns')

  if router_name is not None and router_field is not None:
    create_collection_cmd += format(' -rn {router_name} -rf {router_field}')

  Execute(create_collection_cmd, user=user, group=group
  )
