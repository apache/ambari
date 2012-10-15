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
class hdp2-oozie::service(
  $ensure,
  $setup,
  $initial_wait = undef
)
{
  include $hdp2-oozie::params
  
  $user = "$hdp2-oozie::params::oozie_user"
  $hadoop_home = $hdp2-oozie::params::hadoop_prefix
  $oozie_tmp = $hdp2-oozie::params::oozie_tmp_dir
  $cmd = "env HADOOP_HOME=${hadoop_home} /usr/sbin/oozie_server.sh"
  $pid_file = "${hdp2-oozie::params::oozie_pid_dir}/oozie.pid" 
  $jar_location = $hdp2::params::hadoop_client_jar_location
  $ext_js_path = "${hdp2-oozie::params::ext_zip_dir}/${hdp2-oozie::params::ext_zip_name}"

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  'cd ${oozie_tmp} ; /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.23.1 $jar_location -extjs $ext_js_path; /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run Validate DB Connection; /usr/lib/oozie/bin/oozie-start.sh'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd  = "su - ${user} -c  'cd ${oozie_tmp} ; /usr/lib/oozie/bin/oozie-stop.sh'"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  hdp2-oozie::service::directory { $hdp2-oozie::params::oozie_pid_dir : }
  hdp2-oozie::service::directory { $hdp2-oozie::params::oozie_log_dir : }
  hdp2-oozie::service::directory { $hdp2-oozie::params::oozie_tmp_dir : }
  hdp2-oozie::service::directory { $hdp2-oozie::params::oozie_data_dir : }
  hdp2-oozie::service::directory { $hdp2-oozie::params::oozie_lib_dir : }
  if ($ensure == 'running') {
    hdp2-oozie::service::createsymlinks { 'yarn-mapred-site.xml' : }
  }

  anchor{'hdp2-oozie::service::begin':} -> Hdp2-oozie::Service::Directory<||> -> anchor{'hdp2-oozie::service::end':}
  
  if ($daemon_cmd != undef) {
    hdp2::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }

    if ( $ensure == 'running' ) {
      Hdp2-oozie::Service::Directory<||> -> Hdp2-oozie::Service::Createsymlinks<||>  -> Hdp2::Exec[$daemon_cmd] -> Anchor['hdp2-oozie::service::end'] } 
    else {
      Hdp2-oozie::Service::Directory<||> -> Hdp2::Exec[$daemon_cmd] -> Anchor['hdp2-oozie::service::end']
    }
  }
}

define hdp2-oozie::service::directory()
{
  hdp2::directory_recursive_create { $name: 
    owner => $hdp2-oozie::params::oozie_user,
    mode => '0755',
    service_state => $ensure,
    force => true
  }
}

define hdp2-oozie::service::createsymlinks()
{
  hdp2::exec { '/usr/lib/oozie/conf':
    command => "ln -sf /etc/oozie/conf /usr/lib/oozie/conf",
    unless => "test -e /usr/lib/oozie/conf"
  }

  hdp2::exec { '/etc/oozie/conf/hadoop-conf':
    command => "ln -sf /etc/hadoop/conf /etc/oozie/conf/hadoop-conf",
    unless => "test -e /etc/oozie/conf/hadoop-conf"
  }
}
