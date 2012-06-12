class hdp-hadoop::slave::master-conn($master_host)
{
  Hdp-Hadoop::Configfile<||>{
    namenode_host => $master_host,
    jtnode_host   => $master_host
  }
}