#TODO: this might be replaced by just using hdp::namenode-conn
class hdp-hadoop::slave::namenode-conn($namenode_host)
{
  #TODO: check if can get rido of both
  Hdp-Hadoop::Configfile<||>{namenode_host => $namenode_host}
  Hdp::Configfile<||>{namenode_host => $namenode_host} #for components other than hadoop (e.g., hbase) 
}
