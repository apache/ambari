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




include_once '../../../php/util/Logger.php';
include_once '../../../php/conf/Config.inc';
include_once "../../../php/util/lock.php";
include_once '../../../php/db/HMCDBAccessor.php';

include_once "../../../php/util/SuggestProperties.php";

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";

system("rm -rf ./test.db");
system("rm -rf ./hmc.log");

assert_options(ASSERT_BAIL, 1);

system("sqlite3 ./test.db < ../../../db/schema.dump");

$db = new HMCDBAccessor("./test.db");

print "Creating Cluster - DataCluster\n";
$clusterName = "DataCluster";
$version = "1.0.0";
$state = "";
$result = $db->createCluster($clusterName, $version, $state);
assert(is_array($result) && isset($result["clusterName"]));
assert($result["clusterName"] == $clusterName);
assert($result["result"] == 0);
assert($result["error"] == "");

print "Add Hosts To Cluster\n";

$hosts = array (
array (
        "hostName" => "hrt9n22.cc1.ygridcore.net",
        "ip" => "127.0.0.1",
        "totalMem" => 65536,
        "cpuCount" => 16,
        "osArch" => "x86_64",
        "osType" => "RHEL5",
        "os" => "RHEL5 32-bit",
        "disksInfo" => "foo",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error"
),
array (
        "hostName" => "hrt9n34.cc1.ygridcore.net",
        "ip" => "127.0.0.2",
        "totalMem" => 32834,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error"
),
array (
        "hostName" => "hrt9n35.cc1.ygridcore.net",
        "ip" => "127.0.0.3",
        "totalMem" => 16536,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error"
),
array (
        "hostName" => "hrt9n36.cc1.ygridcore.net",
        "ip" => "127.0.0.4",
        "totalMem" => 16536,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "SUCCESS",
        "badHealthReason" => "no error"
),
array (
        "hostName" => "hrt9n37.cc1.ygridcore.net",
        "ip" => "127.0.0.5",
        "totalMem" => 16356,
        "cpuCount" => 8,
        "osArch" => "x86_64",
        "osType" => "RHEL6",
        "os" => "RHEL6 64-bit",
        "disksInfo" => "bar",
        "discoveryStatus" => "SUCCESS",
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
     "isEnabled" => TRUE
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
),
   "HBASE" => array (
       "HBASE_MASTER" => array( "state" => "UNINSTALLED", "desiredState" => ""),
       "HBASE_REGIONSERVER" => array( "state" => "UNINSTALLED", "desiredState" => "")
)

);
$result = $db->addServiceComponentsToCluster($clusterName, $comps);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Map Hosts to Roles\n";

$mapping = array (
   "NAMENODE" => array ("hrt9n22.cc1.ygridcore.net"),
   "DATANODE" => array ("hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net"),
   "JOBTRACKER" => array ("hrt9n34.cc1.ygridcore.net"),
   "TASKTRACKER" => array ("hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net"),
   "HBASE_MASTER" => array ("hrt9n37.cc1.ygridcore.net"),
   "HBASE_REGIONSERVER" => array ("hrt9n35.cc1.ygridcore.net",
                        "hrt9n36.cc1.ygridcore.net")
);

foreach ($mapping as $comp => $hosts) {
  $db->addHostsToComponent($clusterName, $comp, $hosts, "", "");
  assert(is_array($result));
  assert($result["result"] == 0);
  assert($result["error"] == "");
}

print "Suggesting Properties\n";
$suggestProperties = new SuggestProperties();
$result = $suggestProperties->suggestProperties("DataCluster", $db, TRUE);

// assert checks
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert(is_array($result["configs"]));

// check num maps/num reduces
// minCpu = 8
// maps should be 8 * 2/3 * 2 => 11
// reduces should be (8 * 2) - #maps => 5
assert($result["configs"]["mapred_map_tasks_max"] == 11);
assert($result["configs"]["mapred_red_tasks_max"] == 5);

// nn on 64GB box
assert($result["configs"]["namenode_heapsize"] > 45000
    && $result["configs"]["namenode_heapsize"] < 65000);

// jt on 32 GB box
assert($result["configs"]["jtnode_heapsize"] > 25000
    && $result["configs"]["jtnode_heapsize"] < 32000);

// hbm on 16GB box
assert($result["configs"]["hbase_master_heapsize"] > 14000
    && $result["configs"]["hbase_master_heapsize"] < 16000);


// tt and dns on same box as hrs
// tt/dn capped to 4G as 32-bit
assert($result["configs"]["dtnode_heapsize"] > 3000
    && $result["configs"]["dtnode_heapsize"] < 4096);
assert($result["configs"]["dtnode_heapsize"]
    == $result["configs"]["hadoop_heapsize"]);
assert($result["configs"]["hbase_regionserver_heapsize"] > 4096
    && $result["configs"]["hbase_regionserver_heapsize"] < 5000);

$result = $db->getServiceConfig($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
assert($result["properties"]["mapred_map_tasks_max"] == 11);
assert($result["properties"]["mapred_red_tasks_max"] == 5);
assert($result["properties"]["namenode_heapsize"] > 45000
    && $result["properties"]["namenode_heapsize"] < 65000);
assert($result["properties"]["jtnode_heapsize"] > 25000
    && $result["properties"]["jtnode_heapsize"] < 32000);

// update configs to invalid values to test verification
$updatedConfigs = array (
    "dtnode_heapsize" => 5120,
    "hbase_regionserver_heapsize" => 8196,
    "mapred_red_tasks_max" => 8,
    "jtnode_heapsize" => 35000,
    "hadoop_heapsize" => 50);

print "Updating Properties to invalid values\n";
$result = $db->updateServiceConfigs($clusterName, $updatedConfigs);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");

print "Verifying Properties\n";
$result = $db->getServiceConfig($clusterName);
assert(is_array($result));
assert($result["result"] == 0);
assert($result["error"] == "");
$configs = $result["properties"];

$result = $suggestProperties->verifyProperties($clusterName, $db, $configs);
assert(is_array($result));
assert($result["result"] != 0);
assert($result["error"] != "");
assert(is_array($result["cfgErrors"]));
assert(is_array($result["cfgWarnings"]));

// dtnode heap, jt heap and hadoop heap shld be errors
assert(array_key_exists("dtnode_heapsize", $result["cfgErrors"]));
assert(array_key_exists("hadoop_heapsize", $result["cfgErrors"]));
assert(array_key_exists("jtnode_heapsize", $result["cfgErrors"]));
assert(array_key_exists("hbase_regionserver_heapsize", $result["cfgWarnings"]));
assert(array_key_exists("mapred_red_tasks_max", $result["cfgWarnings"]));

?>
