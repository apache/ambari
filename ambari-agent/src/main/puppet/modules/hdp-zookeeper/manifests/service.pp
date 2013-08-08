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
class hdp-zookeeper::service(
  $ensure = $hdp::params::cluster_service_state,
  $myid
)
{
  include $hdp-zookeeper::params
  $user = $hdp-zookeeper::params::zk_user
  $conf_dir = $hdp-zookeeper::params::conf_dir
  $zk_bin = $hdp::params::zk_bin
  $cmd = "env ZOOCFGDIR=${conf_dir} ZOOCFG=zoo.cfg ${zk_bin}/zkServer.sh"

  $pid_file = $hdp-zookeeper::params::zk_pid_file  

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  'source ${conf_dir}/zookeeper-env.sh ; ${cmd} start'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
    #not using $no_op_test = "su - ${user} -c  '${cmd} status'" because checks more than whether there is a service started up
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  'source ${conf_dir}/zookeeper-env.sh ; ${cmd} stop' && rm -f ${pid_file}"
    #TODO: put in no_op_test for stopped
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }
  hdp::directory_recursive_create { $hdp-zookeeper::params::zk_pid_dir: 
    owner        => $user,
    context_tag => 'zk_service',
    service_state => $ensure,
    force => true
  }
  hdp::directory_recursive_create { $hdp-zookeeper::params::zk_log_dir: 
    owner        => $user,
    context_tag => 'zk_service',
    service_state => $ensure,
    force => true
  }
   hdp::directory_recursive_create { $hdp-zookeeper::params::zk_data_dir: 
    owner        => $user,
    context_tag => 'zk_service',
    service_state => $ensure,
    force => true
  }
  
  if ($daemon_cmd != undef) {
    hdp::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }
  }

  if ($ensure == 'uninstalled') {
    anchor{'hdp-zookeeper::service::begin':} -> Hdp::Directory_recursive_create<|context_tag == 'zk_service'|> ->  anchor{'hdp-zookeeper::service::end':}
  } else {
    class { 'hdp-zookeeper::set_myid': myid => $myid}

    anchor{'hdp-zookeeper::service::begin':} -> Hdp::Directory_recursive_create<|context_tag == 'zk_service'|> -> 
    Class['hdp-zookeeper::set_myid'] -> anchor{'hdp-zookeeper::service::end':}

    if ($daemon_cmd != undef) {
      Class['hdp-zookeeper::set_myid'] -> Hdp::Exec[$daemon_cmd] -> Anchor['hdp-zookeeper::service::end']
    }
  }
}

class hdp-zookeeper::set_myid($myid)
{
  file {"${hdp-zookeeper::params::zk_data_dir}/myid":
    ensure  => file,
    content => $myid,
    mode    => 0644,
  }
}



