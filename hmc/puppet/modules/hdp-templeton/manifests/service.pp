class hdp-templeton::service(
  $ensure,
  $initial_wait = undef
)
{
  include $hdp-templeton::params
  
  $user = "$hdp-templeton::params::templeton_user"
  $hadoop_home = $hdp-templeton::params::hadoop_prefix
  $cmd = "env HADOOP_HOME=${hadoop_home} /usr/sbin/templeton_server.sh"
  $pid_file = "${hdp-templeton::params::templeton_pid_dir}/templeton.pid" 

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} start'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  '${cmd} stop'"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  hdp-templeton::service::directory { $hdp-templeton::params::templeton_pid_dir : }
  hdp-templeton::service::directory { $hdp-templeton::params::templeton_log_dir : }

  anchor{'hdp-templeton::service::begin':} -> Hdp-templeton::Service::Directory<||> -> anchor{'hdp-templeton::service::end':}
  
  if ($daemon_cmd != undef) {
    hdp::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }
    Hdp-templeton::Service::Directory<||> -> Hdp::Exec[$daemon_cmd] -> Anchor['hdp-templeton::service::end']
  }
}

define hdp-templeton::service::directory()
{
  hdp::directory_recursive_create { $name: 
    owner => $hdp-templeton::params::templeton_user,
    mode => '0755',
    service_state => $ensure,
    force => true
  }
}

