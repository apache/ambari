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
define hdp::configfile(
  $component,
  $conf_dir = undef, #if this is undef then name is of form conf_dir/file_name
  $owner = undef, 
  $group = $hdp::params::user_group,
  $mode = undef,
  $size = 64, #32 or 64 bit (used to pick appropriate java_home)
  $template_tag = undef,
  $namenode_host = $hdp::params::namenode_host,
  $jtnode_host = $hdp::params::jtnode_host,
  $snamenode_host = $hdp::params::snamenode_host,
  $rm_host = $hdp::params::rm_host,
  $nm_hosts = $hdp::params::nm_hosts,
  $hs_host = $hdp::params::hs_host,
  $slave_hosts = $hdp::params::slave_hosts,
  $journalnode_hosts = $hdp::params::journalnode_hosts,
  $zkfc_hosts = $hdp::params::zkfc_hosts,
  $mapred_tt_hosts = $hdp::params::mapred_tt_hosts,
  $all_hosts = $hdp::params::all_hosts,
  $hbase_rs_hosts = $hdp::params::hbase_rs_hosts,
  $zookeeper_hosts = $hdp::params::zookeeper_hosts,
  $flume_hosts = $hdp::params::flume_hosts,
  $hbase_master_hosts = $hdp::params::hbase_master_hosts,
  $hcat_server_host = $hdp::params::hcat_server_host,
  $hive_server_host = $hdp::params::hive_server_host,
  $oozie_server = $hdp::params::oozie_server,
  $webhcat_server_host = $hdp::params::webhcat_server_host,
  $hcat_mysql_host = $hdp::params::hcat_mysql_host,
  $nagios_server_host = $hdp::params::nagios_server_host,
  $ganglia_server_host = $hdp::params::ganglia_server_host,
  $dashboard_host = $hdp::params::dashboard_host,
  $gateway_host = $hdp::params::gateway_host,
  $public_namenode_host = $hdp::params::public_namenode_host,
  $public_snamenode_host = $hdp::params::public_snamenode_host,
  $public_rm_host = $hdp::params::public_rm_host,
  $public_nm_hosts = $hdp::params::public_nm_hosts,
  $public_hs_host = $hdp::params::public_hs_host,
  $public_journalnode_hosts = $hdp::params::public_journalnode_hosts,
  $public_zkfc_hosts = $hdp::params::public_zkfc_hosts,
  $public_jtnode_host = $hdp::params::public_jtnode_host,
  $public_hbase_master_hosts = $hdp::params::public_hbase_master_hosts,
  $public_zookeeper_hosts = $hdp::params::public_zookeeper_hosts,
  $public_ganglia_server_host = $hdp::params::public_ganglia_server_host,
  $public_nagios_server_host = $hdp::params::public_nagios_server_host,
  $public_dashboard_host = $hdp::params::public_dashboard_host,
  $public_hive_server_host = $hdp::params::public_hive_server_host,
  $public_oozie_server = $hdp::params::public_oozie_server,
  $public_webhcat_server_host = $hdp::params::public_webhcat_server_host
) 
{

   if ($conf_dir == undef) {
     $qualified_file_name = $name
     $file_name = regsubst($name,'^.+/([^/]+$)','\1')
   } else {
     $qualified_file_name = "${conf_dir}/${name}"
     $file_name = $name
   }
   if ($component == 'base') {
     $module = 'hdp'
   } else {
      $module = "hdp-${component}"   
   }

   if ($template_tag == undef) {  
     $template_name = "${module}/${file_name}.erb"
   } else {
     $template_name = "${module}/${file_name}-${template_tag}.erb"
   }

   file{ $qualified_file_name:
     ensure  => present,
     owner   => $owner,
     group   => $group,
     mode    => $mode,
     content => template($template_name)
  }
}
