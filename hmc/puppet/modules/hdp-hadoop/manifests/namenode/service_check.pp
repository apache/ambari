class hdp-hadoop::namenode::service_check()
{
  hdp-hadoop::exec-hadoop { 'namenode::service_check':
    command   => 'dfs -ls /',
    tries     => 3,
    try_sleep => 5
  }
}
