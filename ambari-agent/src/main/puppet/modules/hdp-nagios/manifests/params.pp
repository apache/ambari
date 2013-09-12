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
class hdp-nagios::params() inherits hdp::params
{   
  $conf_dir = hdp_default("nagios_conf_dir","/etc/nagios")

  if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
    $nn_metrics_property = "FSNamesystem"
  } else {
    $nn_metrics_property = "FSNamesystemMetrics"
  }

  if hdp_is_empty($hdp::params::services_names[httpd]) {
    hdp_fail("There is no service name for service httpd")
  } else {
    $service_name_by_os = $hdp::params::services_names[httpd]
  }

  if hdp_is_empty($service_name_by_os[$hdp::params::hdp_os_type]) {
    if hdp_is_empty($service_name_by_os['ALL']) {
      hdp_fail("There is no service name for service httpd")
    } else {
      $service_name = $service_name_by_os['ALL']
    }
  } else {
    $service_name = $service_name_by_os[$hdp::params::hdp_os_type]
  }

  $httpd_conf_file = "/etc/${service_name}/conf.d/nagios.conf"

  $plugins_dir = "/usr/lib64/nagios/plugins"
  $eventhandlers_dir = "/usr/lib/nagios/eventhandlers"  # Does not exist yet
  $nagios_pid_dir = "/var/run/nagios"
  $nagios_pid_file = "${nagios_pid_dir}/nagios.pid"
  $nagios_log_dir = '/var/log/nagios'
  $nagios_log_archives_dir = "${nagios_log_dir}/archives"
  

  $nagios_obj_dir = hdp_default("nagios_obj_dir","/etc/nagios/objects")
  $nagios_var_dir = hdp_default("nagios_var_dir","/var/nagios")
  $nagios_rw_dir = hdp_default("nagios_var_dir","/var/nagios/rw")
  $nagios_host_cfg = hdp_default("nagios_host_cfg","${nagios_obj_dir}/hadoop-hosts.cfg")
  $nagios_hostgroup_cfg = hdp_default("nagios_hostgroup_cfg","${nagios_obj_dir}/hadoop-hostgroups.cfg")
  $nagios_servicegroup_cfg = hdp_default("nagios_servicegroup_cfg","${nagios_obj_dir}/hadoop-servicegroups.cfg")
  $nagios_service_cfg = hdp_default("nagios_service_cfg","${nagios_obj_dir}/hadoop-services.cfg")
  $nagios_command_cfg = hdp_default("nagios_command_cfg","${nagios_obj_dir}/hadoop-commands.cfg")
  $nagios_resource_cfg = hdp_default("nagios_resource_cfg","${conf_dir}/resource.cfg")

  $nagios_web_login = hdp_default("nagios_web_login","nagiosadmin")
  $nagios_web_password = hdp_default("nagios_web_password","admin")
  
  $dfs_data_dir = $hdp::params::dfs_data_dir

  $check_result_path = hdp_default("nagios_check_result_path","/var/nagios/spool/checkresults")
   
  $nagios_contact = hdp_default("nagios/nagios-contacts/nagios_contact","monitor\\@monitor.com")

  $hostgroup_defs = {
    namenode => {host_member_info => 'namenode_host'},
    snamenode => {host_member_info => 'snamenode_host'},
    slaves => {host_member_info => 'slave_hosts'},
    tasktracker-servers => {host_member_info => 'mapred_tt_hosts'},
    agent-servers => {host_member_info => 'all_hosts'},
    nagios-server => {host_member_info => 'nagios_server_host'},
    jobtracker  => {host_member_info => 'jtnode_host'},
    ganglia-server => {host_member_info => 'ganglia_server_host'},
    flume-servers => {host_member_info => 'flume_hosts'},
    zookeeper-servers => {host_member_info => 'zookeeper_hosts'},
    hbasemasters => {host_member_info => 'hbase_master_hosts'},
    hiveserver => {host_member_info => 'hive_server_host'},
    region-servers => {host_member_info => 'hbase_rs_hosts'},
    oozie-server => {host_member_info => 'oozie_server'},
    webhcat-server => {host_member_info => 'webhcat_server_host'},
    hue-server => {host_member_info => 'hue_server_host'},
    resourcemanager => {host_member_info => 'rm_host'},
    nodemanagers => {host_member_info => 'nm_hosts'},
    historyserver2 => {host_member_info => 'hs_host'},
    journalnodes => {host_member_info => 'journalnode_hosts'}
  }
}
