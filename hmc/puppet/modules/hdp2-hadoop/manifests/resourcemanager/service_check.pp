class hdp2-hadoop::resourcemanager::service_check()
{
  hdp2-hadoop::exec-hadoop { 'resourcemanager::service_check':
    command   => 'job -list',
    tries     => 3,
    try_sleep => 5
  }
}
