class hdp-hadoop::jobtracker::service_check()
{
  hdp-hadoop::exec-hadoop { 'jobtracker::service_check':
    command   => 'job -list',
    tries     => 3,
    try_sleep => 5
  }
}
