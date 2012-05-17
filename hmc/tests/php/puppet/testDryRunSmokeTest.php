<?php

include "PuppetInvoker.php";
include_once "PuppetConfigs.php";
include_once "../db/Transaction.php";

$nodeSmoke = array (
         "HDFS" => "hrt9n35.cc1.ygridcore.net",
         "MAPREDUCE" => "hrt9n35.cc1.ygridcore.net",
         "HBASE" => "hrt9n34.cc1.ygridcore.net",
         "ZOOKEEPER" => "hrt9n29999.cc1.ygridcore.net",
         "HCATALOG" => "hrt9n29999.cc1.ygridcore.net",
         "HIVE" => "hrt9n29999.cc1.ygridcore.net",
         "SQOOP" => "hrt9n29999.cc1.ygridcore.net",
         "PIG" => "hrt9n288.cc1.ygridcore.net",
         "OOZIE" => "hrt9n29999.cc1.ygridcore.net",
         "TEMPLETON" => "hrt9n277.cc1.ygridcore.net",
         "DASHBOARD" => "hrt9n388.cc1.ygridcore.net",
         "GANGLIA" => "hrt9n399.cc1.ygridcore.net",
         "NAGIOS" => "hrt9n499.cc1.ygridcore.net",
         "HDFS_CLIENT" => "hrt11n1111.cc1.ygridcore.net"
       );

$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";
$GLOBALS["puppetManifestDir"] = "/tmp";
$GLOBALS["puppetKickVersionFile"] = "/tmp/kickversion.txt";
$GLOBALS["puppetReportsDir"] = "/tmp/reports";
$GLOBALS["DRYRUN"] = TRUE;
$invoker = new PuppetInvoker("./test.db");
$response = $invoker->kickServiceCheck($nodeSmoke, new Transaction("txn1", "subtxn2", "parentTxn3"), "DataCluster");

print_r($response);

?>
