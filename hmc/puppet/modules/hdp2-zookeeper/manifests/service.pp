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
class hdp2-zookeeper::service(
  $ensure = $hdp2::params::cluster_service_state,
  $myid
)
{
  include $hdp2-zookeeper::params
  $user = $hdp2-zookeeper::params::zk_user
  $conf_dir = $hdp2-zookeeper::params::conf_dir
  $zk_bin = $hdp2::params::zk_bin
  $cmd = "/bin/env ZOOCFGDIR=${conf_dir} ZOOCFG=zoo.cfg ${zk_bin}/zkServer.sh"

  $pid_file = $hdp2-zookeeper::params::zk_pid_file  

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  'source ${conf_dir}/zookeeper-env.sh ; ${cmd} start'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
    #not using $no_op_test = "su - ${user} -c  '${cmd} status'" because checks more than whether there is a service started up
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  'source ${conf_dir}/zookeeper-env.sh ; ${cmd} stop'"
    #TODO: put in no_op_test for stopped
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }
  hdp2::directory_recursive_create { $hdp2-zookeeper::params::zk_pid_dir: 
    owner        => $user,
    context_tag => 'zk_service',
    service_state => $ensure,
    force => true
  }
  hdp2::directory_recursive_create { $hdp2-zookeeper::params::zk_log_dir: 
    owner        => $user,
    context_tag => 'zk_service',
    service_state => $ensure,
    force => true
  }
   hdp2::directory_recursive_create { $hdp2-zookeeper::params::zk_data_dir: 
    owner        => $user,
    context_tag => 'zk_service',
    service_state => $ensure,
    force => true
  }
  
  if ($daemon_cmd != undef) {
    hdp2::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }
  }

  if ($ensure == 'uninstalled') {
    anchor{'hdp2-zookeeper::service::begin':} -> Hdp2::Directory_recursive_create<|context_tag == 'zk_service'|> ->  anchor{'hdp2-zookeeper::service::end':}
  } else {
    class { 'hdp2-zookeeper::set_myid': myid => $myid}

    anchor{'hdp2-zookeeper::service::begin':} -> Hdp2::Directory_recursive_create<|context_tag == 'zk_service'|> -> 
    Class['hdp2-zookeeper::set_myid'] -> anchor{'hdp2-zookeeper::service::end':}

    if ($daemon_cmd != undef) {
      Class['hdp2-zookeeper::set_myid'] -> Hdp2::Exec[$daemon_cmd] -> Anchor['hdp2-zookeeper::service::end']
    }
  }
}

class hdp2-zookeeper::set_myid($myid)
{
  $create_file = "${hdp2-zookeeper::params::zk_data_dir}/myid"
  $cmd = "echo '${myid}' > ${create_file}"
  hdp2::exec{ $cmd:
    command => $cmd,
    creates  => $create_file
  }
}



