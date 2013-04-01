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
class hdp-oozie::service(
  $ensure,
  $setup,
  $initial_wait = undef
)
{
  include $hdp-oozie::params
  
  $user = "$hdp-oozie::params::oozie_user"
  $hadoop_home = $hdp-oozie::params::hadoop_prefix
  $oozie_tmp = $hdp-oozie::params::oozie_tmp_dir
  $oozie_hdfs_user_dir = $hdp::params::oozie_hdfs_user_dir
  $cmd = "env HADOOP_HOME=${hadoop_home} /usr/sbin/oozie_server.sh"
  $pid_file = "${hdp-oozie::params::oozie_pid_dir}/oozie.pid" 
  $jar_location = $hdp::params::hadoop_jar_location
  $ext_js_path = "/usr/share/HDP-oozie/ext.zip"

  $security = $hdp::params::security_enabled
  $oozie_keytab = $hdp-oozie::params::oozie_service_keytab
  $oozie_principal = $configuration['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal']
  
  $jdbc_driver_name = $configuration['oozie-site']['oozie.service.JPAService.jdbc.driver']
  if ($jdbc_driver_name == "com.mysql.jdbc.Driver"){
    $jdbc_driver_jar = "/usr/share/java/mysql-connector-java.jar"
  } elsif($jdbc_driver_name == "oracle.jdbc.driver.OracleDriver") {
      $jdbc_driver_jar = "/usr/share/java/ojdbc6.jar"
  }
  
  

  if ($security == true) {
    $kinit_if_needed = "${hdp::params::kinit_path_local} -kt ${oozie_keytab} ${oozie_principal}"
  } else {
    $kinit_if_needed = "echo 0"
  }
  
  if ($lzo_enabled == true) {
    $lzo_jar_suffix = "-jars /usr/lib/hadoop/lib/hadoop-lzo-0.5.0.jar"
  } else {
    $lzo_jar_suffix = ""
  }

     
        
  $cmd1 = "cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz"
  $cmd2 =  "cd /usr/lib/oozie && mkdir -p ${oozie_tmp}"
  $cmd3 =  "cd /usr/lib/oozie && chown ${user}:${hdp::params::user_group} ${oozie_tmp}" 
     
  $cmd4 = $jdbc_driver_name ? {
        /(com.mysql.jdbc.Driver|oracle.jdbc.driver.OracleDriver)/ => "cd ${oozie_tmp} && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 $jar_location -jars $jdbc_driver_jar -extjs $ext_js_path $lzo_jar_suffix",
        default            => "cd ${oozie_tmp} && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 $jar_location -extjs $ext_js_path $lzo_jar_suffix",
  }
  $cmd5 =  "cd ${oozie_tmp} && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run ; echo 0"
  $cmd6 =  "su - ${user} -c '${kinit_if_needed}; hadoop dfs -put /usr/lib/oozie/share ${oozie_hdfs_user_dir} ; hadoop dfs -chmod -R 755 ${oozie_hdfs_user_dir}/share'"
  #$cmd7 = "/usr/lib/oozie/bin/oozie-start.sh"

  if ($ensure == 'installed_and_configured') {
    $sh_cmds = [$cmd1, $cmd2, $cmd3]
    $user_cmds = [$cmd4, $cmd5]
  } elsif ($ensure == 'running') {   
    $start_cmd = "su - ${user} -c  'cd ${oozie_tmp} && /usr/lib/oozie/bin/oozie-start.sh'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $stop_cmd  = "su - ${user} -c  'cd ${oozie_tmp} && /usr/lib/oozie/bin/oozie-stop.sh'"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  hdp-oozie::service::directory { $hdp-oozie::params::oozie_pid_dir : }
  hdp-oozie::service::directory { $hdp-oozie::params::oozie_log_dir : }
  hdp-oozie::service::directory { $hdp-oozie::params::oozie_tmp_dir : }
  hdp-oozie::service::directory { $hdp-oozie::params::oozie_data_dir : }
  hdp-oozie::service::directory { $hdp-oozie::params::oozie_lib_dir : }
  hdp-oozie::service::directory { $hdp-oozie::params::oozie_webapps_dir : }

  anchor{'hdp-oozie::service::begin':} -> Hdp-oozie::Service::Directory<||> -> anchor{'hdp-oozie::service::end':}
  
  if ($ensure == 'installed_and_configured') {
    hdp-oozie::service::exec_sh{$sh_cmds:}
    hdp-oozie::service::exec_user{$user_cmds:}
    Hdp-oozie::Service::Directory<||> -> Hdp-oozie::Service::Exec_sh[$cmd1] -> Hdp-oozie::Service::Exec_sh[$cmd2] ->Hdp-oozie::Service::Exec_sh[$cmd3] -> Hdp-oozie::Service::Exec_user[$cmd4] ->Hdp-oozie::Service::Exec_user[$cmd5] -> Anchor['hdp-oozie::service::end']
  } elsif ($ensure == 'running') {
    hdp::exec { "exec $cmd6" :
      command => $cmd6,
      unless => "${kinit_if_needed}; hadoop dfs -ls /user/oozie/share | awk 'BEGIN {count=0;} /share/ {count++} END {if (count > 0) {exit 0} else {exit 1}}'"
    }
    hdp::exec { "exec $start_cmd":
      command => $start_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait,
      require => Exec["exec $cmd6"]
    }
  } elsif ($ensure == 'stopped') {
    hdp::exec { "exec $stop_cmd":
      command => $stop_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
   }
  }
}

define hdp-oozie::service::directory()
{
  hdp::directory_recursive_create { $name: 
    owner => $hdp-oozie::params::oozie_user,
    mode => '0755',
    service_state => $ensure,
    force => true
  }
}
define hdp-oozie::service::createsymlinks()
{
  hdp::exec { '/usr/lib/oozie/oozie-server/lib/mapred-site.xml':
    command => "ln -sf /etc/hadoop/conf/mapred-site.xml /usr/lib/oozie/oozie-server/lib/mapred-site.xml",
    unless => "test -e /usr/lib/oozie/oozie-server/lib/mapred-site.xml"
  }
}

define hdp-oozie::service::exec_sh()
{
  $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  hdp::exec { "exec $name":
    command => "/bin/sh -c '$name'",
    unless  => $no_op_test,
    initial_wait => $initial_wait
  }
}

define hdp-oozie::service::exec_user()
{
  $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  hdp::exec { "exec $name":
    command => "su - ${user} -c '$name'",
    unless  => $no_op_test,
    initial_wait => $initial_wait
  }
}
