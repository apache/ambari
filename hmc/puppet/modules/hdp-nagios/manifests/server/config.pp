#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
class hdp-nagios::server::config()
{

  $host_cfg = $hdp-nagios::params::nagios_host_cfg
  
  hdp-nagios::server::configfile { 'nagios.cfg': conf_dir => $hdp-nagios::params::conf_dir }
  hdp-nagios::server::configfile { 'hadoop-hosts.cfg': }
  hdp-nagios::server::configfile { 'hadoop-hostgroups.cfg': }
  hdp-nagios::server::configfile { 'hadoop-servicegroups.cfg': }
  hdp-nagios::server::configfile { 'hadoop-services.cfg': }
  hdp-nagios::server::configfile { 'hadoop-commands.cfg': }
  hdp-nagios::server::configfile { 'contacts.cfg': }

  hdp-nagios::server::check { 'check_cpu.pl': }
  hdp-nagios::server::check { 'check_datanode_storage.php': }
  hdp-nagios::server::check { 'check_aggregate.php': }
  hdp-nagios::server::check { 'check_hdfs_blocks.php': }
  hdp-nagios::server::check { 'check_hdfs_capacity.php': }
  hdp-nagios::server::check { 'check_rpcq_latency.php': }
  hdp-nagios::server::check { 'check_webui.sh': }
  hdp-nagios::server::check { 'check_name_dir_status.php': }
  hdp-nagios::server::check { 'check_puppet_agent_status.php': }
  hdp-nagios::server::check { 'check_oozie_status.sh': }
  hdp-nagios::server::check { 'check_templeton_status.sh': }
  hdp-nagios::server::check { 'check_hive_metastore_status.sh': }

  anchor{'hdp-nagios::server::config::begin':} -> Hdp-nagios::Server::Configfile<||> -> anchor{'hdp-nagios::server::config::end':}
  Anchor['hdp-nagios::server::config::begin'] -> Hdp-nagios::Server::Check<||> -> Anchor['hdp-nagios::server::config::end']
}


###config file helper
define hdp-nagios::server::configfile(
  $owner = $hdp-nagios::params::nagios_user,
  $conf_dir = $hdp-nagios::params::nagios_obj_dir,
  $mode = undef
) 
{
  
  hdp::configfile { "${conf_dir}/${name}":
    component      => 'nagios',
    owner          => $owner,
    mode           => $mode
  }

  
}

define hdp-nagios::server::check()
{
  file { "${hdp-nagios::params::plugins_dir}/${name}":
    source => "puppet:///modules/hdp-nagios/${name}", 
    mode => '0755'
  }
}
