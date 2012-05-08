class hdp-hadoop::hdfs::service_check()
{
  $unique = hdp_unique_id_and_date()
  $dir = '/tmp'
  $tmp_file = "${dir}/${unique}"

  $create_dir_cmd = "fs -mkdir ${dir} ; hadoop fs -chmod -R 777 ${dir}"
  $test_dir_exists = "hadoop fs -test -e ${dir}" #TODO: may fix up fact that test needs explicit hadoop while omamnd does not
  $cleanup_cmd = "fs -rm ${tmp_file}"
  #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
  $create_file_cmd = "${cleanup_cmd}; hadoop fs -put /etc/passwd ${tmp_file}" #TODO: inconsistent that second comamnd needs hadoop
  $test_cmd = "fs -test -e ${tmp_file}"

  anchor { 'hdp-hadoop::hdfs::service_check::begin':}

  hdp-hadoop::exec-hadoop { 'hdfs::service_check::create_dir':
    command   => $create_dir_cmd,
    unless    => $test_dir_exists,
    tries     => 3,
    try_sleep => 5,
    require   => Anchor['hdp-hadoop::hdfs::service_check::begin']
  }

  hdp-hadoop::exec-hadoop { 'hdfs::service_check::create_file':
    command   => $create_file_cmd,
    tries     => 3,
    try_sleep => 5,
    require   => Hdp-hadoop::Exec-hadoop['hdfs::service_check::create_dir'],
    notify    => Hdp-hadoop::Exec-hadoop['hdfs::service_check::test']
  }

  hdp-hadoop::exec-hadoop { 'hdfs::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Hdp-hadoop::Exec-hadoop['hdfs::service_check::create_file'],
    #notify      => Hdp-hadoop::Exec-hadoop['hdfs::service_check::cleanup']  #TODO: put in after testing
    before      => Anchor['hdp-hadoop::hdfs::service_check::end'] #TODO: remove after testing
  }

   #TODO: put in after testing
 #  hdp-hadoop::exec-hadoop { 'hdfs::service_check::cleanup':
 #   command     => $cleanup_cmd,
 #   refreshonly => true,
 #   require     => Hdp-hadoop::Exec-hadoop['hdfs::service_check::test'],
 #   before      => Anchor['hdp-hadoop::hdfs::service_check::end']
  #}
  anchor{ 'hdp-hadoop::hdfs::service_check::end':}

  class { 'hdp-hadoop::hdfs-directories' :
    service_state => running  }
}

class hdp-hadoop::hdfs-directories($service_state)
{
  $smoke_test_user = $hdp::params::smokeuser
  hdp-hadoop::hdfs::directory{ "/user/${smoke_test_user}":
    service_state => $service_state,
    owner => $smoke_test_user,
    mode  => '770',
    recursive_chmod => true
  }
}
