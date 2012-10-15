class hdp2-hadoop::slave::resourcemanager-conn($resourcemanager_host)
{
  Hdp2-Hadoop::Configfile<||>{yarn_rm_host => $resourcemanager_host}
}
