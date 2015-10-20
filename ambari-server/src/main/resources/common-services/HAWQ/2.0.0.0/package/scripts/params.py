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

import functools
from resource_management import Script
from resource_management.libraries.functions.default import default
from resource_management.libraries.resources.hdfs_resource import HdfsResource

config = Script.get_config()


def __get_component_host(component):
  """
  Returns the first host where the given component is deployed, None if the component is not deployed
  """
  component_host = None
  if component in config['clusterHostInfo'] and len(config['clusterHostInfo'][component]) > 0:
    component_host = config['clusterHostInfo'][component][0]
  return component_host


def __get_namenode_host():
  """
  Gets the namenode host; active namenode in case of HA
  """
  namenode_host = __get_component_host('namenode_host')
  
  # hostname of the active HDFS HA Namenode (only used when HA is enabled)
  dfs_ha_namenode_active = default('/configurations/hadoop-env/dfs_ha_initial_namenode_active', None)
  if dfs_ha_namenode_active is not None:
    namenode_host = dfs_ha_namenode_active
  return namenode_host


hostname = config['hostname']

# Users and Groups
hdfs_superuser = config['configurations']['hadoop-env']['hdfs_user']
user_group = config['configurations']['cluster-env']['user_group']

# HAWQ Hostnames
hawqmaster_host = __get_component_host('hawqmaster_hosts')
hawqstandby_host = __get_component_host('hawqstandby_hosts')
hawqsegment_hosts = default('/clusterHostInfo/hawqsegment_hosts', [])

# HDFS
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

# HDFSResource partial function
HdfsResource = functools.partial(HdfsResource, user=hdfs_superuser, hdfs_site=hdfs_site, default_fs=default_fs)

namenode_host= __get_namenode_host()

# YARN
# Note: YARN is not mandatory for HAWQ. It is required only when the users set HAWQ to use YARN as resource manager
rm_host = __get_component_host('rm_host')

# Config files
gpcheck_content = config['configurations']['gpcheck-env']['content']

hawq_site = config['configurations']['hawq-site']
hawq_master_dir = hawq_site.get('hawq_master_directory')
hawq_segment_dir = hawq_site.get('hawq_segment_directory')
hawq_master_temp_dir = hawq_site.get('hawq_master_temp_directory')
hawq_segment_temp_dir = hawq_site.get('hawq_segment_temp_directory')
# Extract hawq hdfs directory from hdfs url. Ex: /hawq/hawq_default from
# host:8080/hawq/hawq_default
hawq_hdfs_data_dir = "/{0}".format(hawq_site.get('hawq_dfs_url').split('/', 1)[1])
hawq_master_address_port = hawq_site.get('hawq_master_address_port')
hawq_segment_address_port = hawq_site.get('hawq_segment_address_port')


