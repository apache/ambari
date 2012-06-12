class hdp-hbase::zk-conn(
 $zookeeper_hosts
)
{
  Hdp::Configfile<||>{zookeeper_hosts => $zookeeper_hosts}
}