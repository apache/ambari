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


include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once "HMC.php";
include_once "Service.php";
include_once "ServiceComponent.php";
include_once "../db/Transaction.php";
include_once "../db/OrchestratorDB.php";
include_once "../puppet/DBReader.php";
include_once "../puppet/PuppetInvoker.php";

$dbPath = "/tmp/orchestrator-test.db";

system("rm -rf ".$dbPath);
system("rm -rf ".$GLOBALS["HMC_LOG_FILE"]);
system("rm -rf ".$GLOBALS["puppetReportsDir"]);
system("mkdir -p ".$GLOBALS["puppetManifestDir"]);
system("mkdir -p ".$GLOBALS["puppetReportsDir"]);

assert_options(ASSERT_BAIL, 1);

system("sqlite3 /tmp/orchestrator-test.db < ../../../db/schema.dump");

$db = new HMCDBAccessor($dbPath);

print "Creating Cluster - DataCluster\n";
$clusterName = "orchestratortestcluster";
$version = "1.0.0";
$state = "";
$result = $db->createCluster($clusterName, $version, $state);
assert(is_array($result) && isset($result["clusterName"]));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");

print "Add Hosts To Cluster\n";
print "Adding hrt9n22.cc1.ygridcore.net\n";
print "Adding hrt9n34.cc1.ygridcore.net\n";
print "Adding hrt9n35.cc1.ygridcore.net\n";
print "Adding hrt9n36.cc1.ygridcore.net\n";
print "Adding hrt9n37.cc1.ygridcore.net\n";

$hosts = array (
  array (
        "hostName" => "hrt9n22.cc1.ygridcore.net",
        "ip" => "127.0.0.1",
        "totalMem" => 64,
        "cpuCount" => 4,
        "osArch" => "x86_64",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n34.cc1.ygridcore.net",
        "ip" => "127.0.0.2",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n35.cc1.ygridcore.net",
        "ip" => "127.0.0.3",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n36.cc1.ygridcore.net",
        "ip" => "127.0.0.4",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n37.cc1.ygridcore.net",
        "ip" => "127.0.0.5",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  )
);

$result = $db->addHostsToCluster($clusterName, $hosts);
assert(is_array($result));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(count($result["hosts"]) == 5);

print "Add Services To Cluster\n";
$services = array(
   array (
     "serviceName" => "HDFS",
     "state" => "UNINSTALLED",
     "isEnabled" => TRUE
  ),
  array (
     "serviceName" => "MAPREDUCE",
     "state" => "UNINSTALLED",
     "isEnabled" => TRUE
  ),
  array (
     "serviceName" => "HBASE",
     "state" => "UNKNOWN",
     "isEnabled" => FALSE
  )
);

$result = $db->addServicesToCluster($clusterName, $services);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Add Service Components To Cluster\n";
$comps = array (
   "HDFS" => array (
       "NAMENODE" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "DATANODE" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "SNAMENODE" => array( "state" => "UNINSTALLED", "desiredState" => "")
      ),
   "MAPREDUCE" => array (
       "JOBTRACKER" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "TASKTRACKER" => array( "state" => "UNINSTALLED", "desiredState" => "")
      )
);
$result = $db->addServiceComponentsToCluster($clusterName, $comps);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Map Hosts to Roles\n";

$mapping = array (
   "NAMENODE" => array ("hrt9n22.cc1.ygridcore.net"),
   "SNAMENODE" => array ("hrt9n22.cc1.ygridcore.net"),
   "DATANODE" => array ("hrt9n34.cc1.ygridcore.net",
                        "hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net"),
   "JOBTRACKER" => array ("hrt9n37.cc1.ygridcore.net"),
   "TASKTRACKER" => array ("hrt9n34.cc1.ygridcore.net",
                        "hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net")
);

foreach ($mapping as $comp => $hosts) {
  $db->addHostsToComponent($clusterName, $comp, $hosts, "", "");
  assert(is_array($result));
  assert($result["result"] == 0);
  assert($result["error"] == "");
}

print "Add Service Configs\n";
$query = "INSERT OR REPLACE INTO ServiceConfig ( cluster_name, key, value ) "
    . " SELECT \"" . $clusterName . "\" , key, default_value "
    . " FROM ConfigProperties ";

$command = "sqlite3 $dbPath '" . $query . "'";
system($command);

print "Verify Service Config Defaults Added to DB\n";
$result = $db->getServiceConfig($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["properties"])
       && count($result["properties"]) > 0);


print "HMC testing\n";
$startTime = time();
$hmc = new HMC($dbPath, $clusterName);

$result = $hmc->deployHDP();
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];


$counter = 0;
do {
  sleep(3);
  $curTime = time();
  $progress = $hmc->getProgress($txnId);

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  //print_r($progress);


  $counter++;

  if (($counter%10) == 4) {
     file_put_contents("/tmp/hmc.foo.dump.".$counter, json_encode($progress));
  }

  if (!is_array($progress)
      || $progress["result"] != 0
      || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

/*
print "LOGS *******";
$logs = $hmc->getLogs($txnId);
print_r($logs);
*/

$startTime = time();
$result = $hmc->stopAllServices();
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  sleep(3);
  $curTime = time();

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  //print_r($progress);

  $counter++;

  if (!is_array($progress)
      || $progress["result"] != 0
      || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

$startTime = time();
$result = $hmc->startAllServices();
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  sleep(3);
  $curTime = time();
  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  //print_r($progress);

  $counter++;

  if (!is_array($progress)
      || $progress["result"] != 0
      || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

$startTime = time();
$result = $hmc->reconfigureServices(array ( "HDFS" ));
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  sleep(3);
  $curTime = time();

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  //print_r($progress);

  $counter++;

  if (!is_array($progress)
  || $progress["result"] != 0
  || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

$startTime = time();
$result = $hmc->stopServices(array ( "HDFS", "MAPREDUCE" ));
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  sleep(3);
  $curTime = time();

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  //print_r($progress);

  $counter++;

  if (!is_array($progress)
  || $progress["result"] != 0
  || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

$startTime = time();
$result = $hmc->startServices(array ( "MAPREDUCE" ));
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  sleep(3);
  $curTime = time();

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  //print_r($progress);

  $counter++;

  if (!is_array($progress)
  || $progress["result"] != 0
  || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

$startTime = time();
$result = $hmc->stopServices(array ( "MAPREDUCE" ));
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  sleep(3);
  $curTime = time();

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  //print_r($progress);

  $counter++;

  if (!is_array($progress)
  || $progress["result"] != 0
  || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );

print "Add More Hosts To Cluster\n";
print "Adding hrt9n39.cc1.ygridcore.net\n";
print "Adding hrt9n38.cc1.ygridcore.net\n";

$hosts = array (
  array (
        "hostName" => "hrt9n38.cc1.ygridcore.net",
        "ip" => "127.0.0.1",
        "totalMem" => 64,
        "cpuCount" => 4,
        "osArch" => "x86_64",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  ),
  array (
        "hostName" => "hrt9n39.cc1.ygridcore.net",
        "ip" => "127.0.0.2",
        "totalMem" => 32,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "success",
        "badHealthReason" => "no error"
  )
);

$result = $db->addHostsToCluster($clusterName, $hosts);
assert(is_array($result));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");
assert(count($result["hosts"]) == 2);

print "Map new Hosts to Roles\n";

$mapping = array (
   "DATANODE" => array ("hrt9n38.cc1.ygridcore.net",
                        "hrt9n39.cc1.ygridcore.net"),
   "TASKTRACKER" => array ("hrt9n38.cc1.ygridcore.net",
                        "hrt9n39.cc1.ygridcore.net")
);

foreach ($mapping as $comp => $hosts) {
  $db->addHostsToComponent($clusterName, $comp, $hosts, "UNKNOWN", "UNKNOWN");
  assert(is_array($result));
  assert($result["result"] == 0);
  assert($result["error"] == "");
}


$startTime = time();
$result = $hmc->deployNodes(array ( "hrt9n38.cc1.ygridcore.net" , "hrt9n39.cc1.ygridcore.net" ));
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));

$txnId = $result["txnId"];

$counter = 0;
do {
  $curTime = time();

  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  print_r($progress);

  $counter++;

  if (!is_array($progress)
  || $progress["result"] != 0
  || !$progress["processRunning"]) {
    print "Breaking loop now. Action completed \n";
    break;
  }
} while ( 1 );


$startTime = time();
print("Uninstall HDP\n");
$result = $hmc->uninstallHDP();
print_r($result);

assert($result["result"] == 0);
assert(isset($result["txnId"]));
$txnId = $result["txnId"];

$counter = 0;

do {
  $curTime = time();
  print "Elapsed: " . ($curTime - $startTime) . " seconds \n";
  print "Progress: \n";
  $progress = $hmc->getProgress($txnId);
  print_r($progress);
  $counter++;

  if (!is_array($progress) || $progress["result"] !=0
      || !$progress["processRunning"]) {
    print "Breaking loop now. Action Completed \n";
    break;
  }
} while (1);


?>
