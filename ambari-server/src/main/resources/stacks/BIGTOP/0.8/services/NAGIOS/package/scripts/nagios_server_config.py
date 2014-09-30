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

Ambari Agent

"""

from resource_management import *

def nagios_server_config():
  import params
  
  nagios_server_configfile( 'nagios.cfg', 
                            config_dir = params.conf_dir, 
                            group = params.nagios_group
  )
  nagios_server_configfile( 'resource.cfg', 
                            config_dir = params.conf_dir, 
                            group = params.nagios_group
  )
  nagios_server_configfile( 'hadoop-hosts.cfg')
  nagios_server_configfile( 'hadoop-hostgroups.cfg')
  nagios_server_configfile( 'hadoop-servicegroups.cfg')
  nagios_server_configfile( 'hadoop-services.cfg')
  nagios_server_configfile( 'hadoop-commands.cfg')
  nagios_server_configfile( 'contacts.cfg')
  
  if System.get_instance().os_family != "suse":
    nagios_server_configfile( 'nagios',
                              config_dir = '/etc/init.d',
                              mode = 0755, 
                              owner = 'root', 
                              group = 'root'
    )

  nagios_server_check( 'check_cpu.pl')
  nagios_server_check( 'check_cpu.php')
  nagios_server_check( 'check_cpu_ha.php')
  nagios_server_check( 'check_datanode_storage.php')
  nagios_server_check( 'check_aggregate.php')
  nagios_server_check( 'check_hdfs_blocks.php')
  nagios_server_check( 'check_hdfs_capacity.php')
  nagios_server_check( 'check_rpcq_latency.php')
  nagios_server_check( 'check_rpcq_latency_ha.php')
  nagios_server_check( 'check_webui.sh')
  nagios_server_check( 'check_webui_ha.sh')
  nagios_server_check( 'check_name_dir_status.php')
  nagios_server_check( 'check_oozie_status.sh')
  nagios_server_check( 'check_templeton_status.sh')
  nagios_server_check( 'check_hive_metastore_status.sh')
  nagios_server_check( 'check_hue_status.sh')
  nagios_server_check( 'check_mapred_local_dir_used.sh')
  nagios_server_check( 'check_nodemanager_health.sh')
  nagios_server_check( 'check_namenodes_ha.sh')
  nagios_server_check( 'check_wrapper.sh')
  nagios_server_check( 'hdp_nagios_init.php')
  nagios_server_check( 'check_checkpoint_time.py' )
  nagios_server_check( 'sys_logger.py' )
  nagios_server_check( 'check_ambari_alerts.py' )

def nagios_server_configfile(
  name,
  owner = None,
  group = None,
  config_dir = None,
  mode = None
):
  import params
  owner = params.nagios_user if not owner else owner
  group = params.user_group if not group else group
  config_dir = params.nagios_obj_dir if not config_dir else config_dir
  
  TemplateConfig( format("{config_dir}/{name}"),
    owner          = owner,
    group          = group,
    mode           = mode
  )

def nagios_server_check(name):
  File( format("{plugins_dir}/{name}"),
    content = StaticFile(name), 
    mode = 0755
  )
