class hdp-hbase::master-conn($hbase_master_host)
{
  Hdp-Hbase::Configfile<||>{hbase_master_host => $hbase_master_host}
}
