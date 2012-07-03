<?php
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


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
