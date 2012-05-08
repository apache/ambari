class hdp-hadoop::mapred::service_check() 
{
  $smoke_test_user = $hdp::params::smokeuser
  $jar_location = $hdp::params::hadoop_jar_location
  $input_file = 'mapredsmokeinput'
  $output_file = "mapredsmokeoutput"

  $cleanup_cmd = "dfs -rmr ${output_file} ${input_file}"
  #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
  $create_file_cmd = "$cleanup_cmd ; hadoop dfs -put /etc/passwd ${input_file} " #TODO: inconsistent that second comamnd needs hadoop
  $test_cmd = "fs -test -e ${output_file}" 
  $run_wordcount_job = "jar ${jar_location}/hadoop-examples.jar  wordcount ${input_file} ${output_file}"
  
  anchor { 'hdp-hadoop::mapred::service_check::begin':}

  hdp-hadoop::exec-hadoop { 'mapred::service_check::create_file':
    command   => $create_file_cmd,
    tries     => 1,
    try_sleep => 5,
    require   => Anchor['hdp-hadoop::mapred::service_check::begin'],
  #  notify    => Hdp-hadoop::Exec-hadoop['mapred::service_check::run_wordcount'],
    user      => $smoke_test_user
  }

  hdp-hadoop::exec-hadoop { 'mapred::service_check::run_wordcount':
    command   => $run_wordcount_job,
    tries     => 1,
    try_sleep => 5,
    require   => Hdp-hadoop::Exec-hadoop['mapred::service_check::create_file'],
    notify    => Hdp-hadoop::Exec-hadoop['mapred::service_check::test'],
    user      => $smoke_test_user,
    logoutput => "true"
  }

#  exec { 'runjob':
#    command   => "hadoop jar ${jar_location}/hadoop-examples.jar  wordcount ${input_file} ${output_file}",
#    tries     => 1,
#    try_sleep => 5,
#    require   => Hdp-hadoop::Exec-hadoop['mapred::service_check::create_file'],
#    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
#    notify    => Hdp-hadoop::Exec-hadoop['mapred::service_check::test'],
#    logoutput => "true",
#    user      => $smoke_test_user
#  }

  hdp-hadoop::exec-hadoop { 'mapred::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Hdp-hadoop::Exec-hadoop['mapred::service_check::run_wordcount'],
    before      => Anchor['hdp-hadoop::mapred::service_check::end'], #TODO: remove after testing
    user        => $smoke_test_user
  }
  
  anchor{ 'hdp-hadoop::mapred::service_check::end':}
}
