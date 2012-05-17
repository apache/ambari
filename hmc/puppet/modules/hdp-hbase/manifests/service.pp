define hdp-hbase::service(
  $ensure = 'running',
  $create_pid_dir = true,
  $create_log_dir = true,
  $initial_wait = undef)
{
  include hdp-hbase::params

  $role = $name
  $user = $hdp-hbase::params::hbase_user

  $conf_dir = $hdp::params::hbase_conf_dir
  $hbase_daemon = $hdp::params::hbase_daemon_script
  $cmd = "$hbase_daemon --config ${conf_dir}"
  $pid_dir = $hdp-hbase::params::hbase_pid_dir
  $pid_file = "${pid_dir}/hbase-hbase-${role}.pid"

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} start ${role}'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  '${cmd} stop ${role}'"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  $tag = "hbase_service-${name}"
  
  if ($create_pid_dir == true) {
    hdp::directory_recursive_create { $pid_dir: 
      owner => $user,
      tag   => $tag,
      service_state => $ensure,
      force => true
    }
  }
  if ($create_log_dir == true) {
    hdp::directory_recursive_create { $hdp-hbase::params::hbase_log_dir: 
      owner => $user,
      tag   => $tag,
      service_state => $ensure,
      force => true
    }
  }

  anchor{"hdp-hbase::service::${name}::begin":} -> Hdp::Directory_recursive_create<|tag == $tag|> -> anchor{"hdp-hbase::service::${name}::end":}
  if ($daemon_cmd != undef) { 
    hdp::exec { $daemon_cmd:
      command      => $daemon_cmd,
      unless       => $no_op_test,
      initial_wait => $initial_wait
    }
    Hdp::Directory_recursive_create<|context_tag == 'hbase_service'|> -> Hdp::Exec[$daemon_cmd] -> Anchor["hdp-hbase::service::${name}::end"]
  }
}
