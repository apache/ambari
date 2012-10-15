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
define hdp2::configfile(
  $component,
  $conf_dir = undef, #if this is undef then name is of form conf_dir/file_name
  $owner = undef, 
  $group = $hdp2::params::hadoop_user_group,
  $mode = undef,
  $size = 64, #32 or 64 bit (used to pick appropriate java_home)
  $template_tag = undef,
  $namenode_host = $hdp2::params::namenode_host,
  $yarn_rm_host = $hdp2::params::yarn_rm_host,
  $snamenode_host = $hdp2::params::snamenode_host,
  $slave_hosts = $hdp2::params::slave_hosts,
  $hbase_rs_hosts = $hdp2::params::hbase_rs_hosts,
  $zookeeper_hosts = $hdp2::params::zookeeper_hosts,
  $hbase_master_host = $hdp2::params::hbase_master_host,
  $hcat_server_host = $hdp2::params::hcat_server_host,
  $hive_server_host = $hdp2::params::hive_server_host,
  $oozie_server = $hdp2::params::oozie_server,
  $templeton_server_host = $hdp2::params::templeton_server_host,
  $hcat_mysql_host = $hdp2::params::hcat_mysql_host,
  $nagios_server_host = $hdp2::params::nagios_server_host,
  $ganglia_server_host = $hdp2::params::ganglia_server_host,
  $dashboard_host = $hdp2::params::dashboard_host,
  $gateway_host = $hdp2::params::gateway_host,
  $public_namenode_host = $hdp2::params::public_namenode_host,
  $public_snamenode_host = $hdp2::params::public_snamenode_host,
  $public_yarn_rm_host = $hdp2::params::public_yarn_rm_host,
  $public_hbase_master_host = $hdp2::params::public_hbase_master_host,
  $public_zookeeper_hosts = $hdp2::params::public_zookeeper_hosts,
  $public_ganglia_server_host = $hdp2::params::public_ganglia_server_host,
  $public_nagios_server_host = $hdp2::params::public_nagios_server_host,
  $public_dashboard_host = $hdp2::params::public_dashboard_host,
  $public_hive_server_host = $hdp2::params::public_hive_server_host,
  $public_oozie_server = $hdp2::params::public_oozie_server,
  $public_templeton_server_host = $hdp2::params::public_templeton_server_host
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
     $module = 'hdp2'
   } else {
      $module = "hdp2-${component}"   
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
