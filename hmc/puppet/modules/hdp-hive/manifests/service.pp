class hdp-hive::service(
  $ensure
)
{
  include $hdp-hive::params
  
  $user = $hdp-hive::params::hive_user
  $hadoop_home = $hdp::params::hadoop_home
  $hive_log_dir = $hdp-hive::params::hive_log_dir
  $cmd = "env HADOOP_HOME=${hadoop_home} nohup hive --service metastore > ${hive_log_dir}/hive.out 2> ${hive_log_dir}/hive.log &"
  $pid_file = "${hdp-hive::params::hive_pid_dir}/hive.pid" 

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} '"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    #TODO: this needs to be fixed
    $daemon_cmd = "ps aux | awk '{print $1,$2}' | grep ${user} | awk '{print $2}' | xargs kill >/dev/null 2>&1"
    $no_op_test = "ps aux | grep -i [h]ive"
  } else {
    $daemon_cmd = undef
  }

  hdp-hive::service::directory { $hdp-hive::params::hive_pid_dir : }
  hdp-hive::service::directory { $hdp-hive::params::hive_log_dir : }

  anchor{'hdp-hive::service::begin':} -> Hdp-hive::Service::Directory<||> -> anchor{'hdp-hive::service::end':}
  
  if ($daemon_cmd != undef) {
    if ($ensure == 'running') {
      hdp::exec { $daemon_cmd:
        command => $daemon_cmd,
        unless  => $no_op_test
      }
    } elsif ($ensure == 'stopped') {
      hdp::exec { $daemon_cmd:
        command => $daemon_cmd,
        onlyif  => $no_op_test
      }
    }
    Hdp-hive::Service::Directory<||> -> Hdp::Exec[$daemon_cmd] -> Anchor['hdp-hive::service::end']
  }
}

define hdp-hive::service::directory()
{
  hdp::directory_recursive_create { $name: 
    owner => $hdp-hive::params::hive_user,
    mode => '0755',
    service_state => $ensure,
    force => true
  }
}

