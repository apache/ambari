class hdp-hadoop::slave::jobtracker-conn($jobtracker_host)
{
  Hdp-Hadoop::Configfile<||>{jtnode_host => $jobtracker_host}
}